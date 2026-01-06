/*
 * Hello Minecraft! Launcher
 * Copyright (C) 2025  huangyuhui <huanghongxun2008@126.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.jackhuang.hmcl.cofemine;

import com.google.gson.JsonParseException;
import org.jackhuang.hmcl.setting.Profile;
import org.jackhuang.hmcl.setting.Profiles;
import org.jackhuang.hmcl.task.FileDownloadTask;
import org.jackhuang.hmcl.task.Schedulers;
import org.jackhuang.hmcl.task.Task;
import org.jackhuang.hmcl.util.StringUtils;
import org.jackhuang.hmcl.util.gson.JsonUtils;
import org.jackhuang.hmcl.util.io.FileUtils;
import org.jackhuang.hmcl.util.io.HttpRequest;
import org.jackhuang.hmcl.util.io.Unzipper;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.jackhuang.hmcl.util.logging.Logger.LOG;

public final class CofeMineModpackService {
    public static final String PROFILE_NAME = "CofeMine";
    public static final String MARKER_DIR = ".cofemine";
    public static final String MARKER_FILE = "installed.json";

    private static final List<String> DEFAULT_UPDATE_DIRS = List.of(
            "mods",
            "config",
            "kubejs",
            "defaultconfigs",
            "resourcepacks",
            "shaderpacks",
            "datapacks",
            "scripts"
    );

    private static final Set<String> PROTECTED_TOP_LEVEL = Set.of(
            "saves",
            "screenshots",
            "logs",
            "crash-reports",
            "options.txt"
    );

    public CompletableFuture<CofeMineModpackManifest> loadManifestAsync(@Nullable String manifestUrl) {
        if (StringUtils.isBlank(manifestUrl)) {
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.supplyAsync(() -> {
            try {
                String json = HttpRequest.GET(manifestUrl).getString();
                return JsonUtils.fromNonNullJson(json, CofeMineModpackManifest.class);
            } catch (IOException | JsonParseException e) {
                LOG.warning("Unable to load CofeMine manifest", e);
                return null;
            }
        }, Schedulers.io());
    }

    public Task<Void> createInstallTask(Path targetDir, String zipUrl, @Nullable CofeMineModpackManifest manifest, String manifestUrl) throws IOException {
        return createTask(targetDir, zipUrl, manifest, manifestUrl, Mode.INSTALL);
    }

    public Task<Void> createUpdateTask(Path targetDir, String zipUrl, @Nullable CofeMineModpackManifest manifest, String manifestUrl) throws IOException {
        return createTask(targetDir, zipUrl, manifest, manifestUrl, Mode.UPDATE);
    }

    public static boolean isInstalled(@Nullable Path targetDir) {
        if (targetDir == null) {
            return false;
        }
        return Files.isRegularFile(getMarkerPath(targetDir));
    }

    public static Path getMarkerPath(Path targetDir) {
        return targetDir.resolve(MARKER_DIR).resolve(MARKER_FILE);
    }

    public static void ensureProfile(Path targetDir) {
        Profile existing = Profiles.getProfiles().stream()
                .filter(profile -> PROFILE_NAME.equals(profile.getName()))
                .findFirst()
                .orElse(null);

        if (existing == null) {
            Profile profile = new Profile(PROFILE_NAME, targetDir);
            profile.setUseRelativePath(false);
            Profiles.getProfiles().add(profile);
            Profiles.setSelectedProfile(profile);
        } else {
            existing.setGameDir(targetDir);
            Profiles.setSelectedProfile(existing);
        }
    }

    private Task<Void> createTask(Path targetDir, String zipUrl, @Nullable CofeMineModpackManifest manifest, String manifestUrl, Mode mode) throws IOException {
        Objects.requireNonNull(targetDir, "targetDir");

        Path workDir = Files.createTempDirectory("cofemine-modpack");
        Path zipPath = workDir.resolve("cofemine-pack.zip");
        Path extractDir = workDir.resolve("extract");

        FileDownloadTask.IntegrityCheck integrityCheck = manifest != null && StringUtils.isNotBlank(manifest.sha256())
                ? new FileDownloadTask.IntegrityCheck("SHA-256", manifest.sha256())
                : null;

        FileDownloadTask downloadTask = new FileDownloadTask(zipUrl, zipPath, integrityCheck);
        downloadTask.addIntegrityCheckHandler(FileDownloadTask.ZIP_INTEGRITY_CHECK_HANDLER);
        downloadTask.setName("cofemine-pack.zip");

        Task<Void> unzipTask = Task.runAsync("CofeMine Unpack", Schedulers.io(), () -> {
            new Unzipper(zipPath, extractDir)
                    .setReplaceExistentFile(true)
                    .unzip();
        });

        Task<Void> syncTask = new Task<Void>() {
            @Override
            public void execute() throws Exception {
                Path contentRoot = resolveContentRoot(extractDir);
                if (mode == Mode.INSTALL) {
                    FileUtils.copyDirectory(contentRoot, targetDir);
                } else {
                    syncUpdateWithProgress(contentRoot, targetDir, manifest);
                }
            }

            private void syncUpdateWithProgress(Path sourceRoot, Path targetDir, @Nullable CofeMineModpackManifest manifest) throws IOException {
                Set<String> allowedTopLevel = new HashSet<>();
                if (manifest != null && manifest.directories() != null && !manifest.directories().isEmpty()) {
                    for (String entry : manifest.directories()) {
                        if (StringUtils.isBlank(entry)) {
                            continue;
                        }
                        String normalized = entry.replace('\\', '/');
                        if (normalized.startsWith("/")) {
                            normalized = normalized.substring(1);
                        }
                        if (normalized.endsWith("/")) {
                            normalized = normalized.substring(0, normalized.length() - 1);
                        }
                        if (!normalized.isBlank()) {
                            String top = normalized.split("/")[0];
                            allowedTopLevel.add(top.toLowerCase(Locale.ROOT));
                        }
                    }
                } else {
                    for (String entry : DEFAULT_UPDATE_DIRS) {
                        allowedTopLevel.add(entry.toLowerCase(Locale.ROOT));
                    }
                }

                List<Path> files = new ArrayList<>();
                try (var stream = Files.walk(sourceRoot)) {
                    stream.filter(Files::isRegularFile).forEach(files::add);
                }

                long total = files.size();
                long current = 0;

                for (Path file : files) {
                    current++;
                    updateProgress(current, total);

                    Path relative = sourceRoot.relativize(file);
                    if (relative.getNameCount() == 0) {
                        continue;
                    }
                    String topLevel = relative.getName(0).toString().toLowerCase(Locale.ROOT);
                    if (PROTECTED_TOP_LEVEL.contains(topLevel)) {
                        continue;
                    }
                    if (!allowedTopLevel.contains(topLevel)) {
                        continue;
                    }
                    if ("options.txt".equalsIgnoreCase(relative.toString().replace('\\', '/'))) {
                        continue;
                    }

                    Path target = targetDir.resolve(relative.toString());
                    Files.createDirectories(target.getParent());
                    Files.copy(file, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }

                updateProgress(total == 0 ? 1 : total, total == 0 ? 1 : total);
            }
        }.setExecutor(Schedulers.io()).setName("CofeMine Sync");

        Task<Void> markerTask = Task.runAsync("CofeMine Marker", Schedulers.io(), () -> {
            writeMarker(targetDir, manifest, zipUrl, manifestUrl);
        });

        Task<Void> sequence = new CofeMineModpackTask(List.of(downloadTask, unzipTask, syncTask, markerTask));
        return sequence.whenComplete(Schedulers.io(), exception -> FileUtils.deleteDirectoryQuietly(workDir));
    }

    private static Path resolveContentRoot(Path extractDir) throws IOException {
        try (var stream = Files.list(extractDir)) {
            List<Path> children = stream.toList();
            if (children.size() == 1 && Files.isDirectory(children.get(0))) {
                Path candidate = children.get(0);
                if (looksLikeModpackRoot(candidate)) {
                    return candidate;
                }
            }
        }
        return extractDir;
    }

    private static boolean looksLikeModpackRoot(Path candidate) {
        return Files.isDirectory(candidate.resolve("mods"))
                || Files.isDirectory(candidate.resolve("config"))
                || Files.isDirectory(candidate.resolve("versions"))
                || Files.isDirectory(candidate.resolve("minecraft"));
    }

    private static void writeMarker(Path targetDir, @Nullable CofeMineModpackManifest manifest, String zipUrl, String manifestUrl) throws IOException {
        Path markerDir = targetDir.resolve(MARKER_DIR);
        Files.createDirectories(markerDir);
        CofeMineModpackMarker marker = new CofeMineModpackMarker(
                manifest != null ? manifest.version() : null,
                manifest != null ? manifest.updatedAt() : null,
                zipUrl,
                manifestUrl,
                Instant.now().toString()
        );
        FileUtils.saveSafely(markerDir.resolve(MARKER_FILE),
                out -> out.write(JsonUtils.GSON.toJson(marker).getBytes(StandardCharsets.UTF_8)));
    }

    private enum Mode {
        INSTALL,
        UPDATE
    }

    private static final class CofeMineModpackTask extends Task<Void> {
        private final List<Task<?>> dependents;

        private CofeMineModpackTask(List<Task<?>> dependents) {
            this.dependents = dependents;
        }

        @Override
        public void execute() {
            setResult(null);
        }

        @Override
        public List<Task<?>> getDependents() {
            return dependents;
        }
    }

    private record CofeMineModpackMarker(
            @Nullable String version,
            @Nullable String updatedAt,
            String zipUrl,
            String manifestUrl,
            String installedAt
    ) {
    }
}
