/*
 * Hello Minecraft! Launcher
 * Copyright (C) 2025  huangyuhui <huanghongxun2008@126.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package org.jackhuang.hmcl.cofemine;

import org.jackhuang.hmcl.download.DefaultDependencyManager;
import org.jackhuang.hmcl.game.GameDirectoryType;
import org.jackhuang.hmcl.game.HMCLGameRepository;
import org.jackhuang.hmcl.mod.Modpack;
import org.jackhuang.hmcl.mod.ModpackConfiguration;
import org.jackhuang.hmcl.mod.modrinth.ModrinthInstallTask;
import org.jackhuang.hmcl.mod.modrinth.ModrinthManifest;
import org.jackhuang.hmcl.mod.modrinth.ModrinthModpackProvider;
import org.jackhuang.hmcl.setting.DownloadProviders;
import org.jackhuang.hmcl.setting.Profile;
import org.jackhuang.hmcl.setting.VersionSetting;
import org.jackhuang.hmcl.task.FileDownloadTask;
import org.jackhuang.hmcl.task.Schedulers;
import org.jackhuang.hmcl.task.Task;
import org.jackhuang.hmcl.util.io.CompressingUtils;
import org.jackhuang.hmcl.util.gson.JsonUtils;
import org.jackhuang.hmcl.util.io.FileUtils;
import org.jetbrains.annotations.Nullable;

import kala.compress.archivers.zip.ZipArchiveReader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.jackhuang.hmcl.util.logging.Logger.LOG;

/**
 * Installs / updates a CofeMine pack delivered as a Modrinth-format
 * {@code .mrpack} archive served by the panel.
 *
 * <p>The flow mirrors {@code ModpackHelper.getInstallTask} from upstream
 * HMCL: mark the version as a modpack <em>before</em> the install runs so
 * {@link HMCLGameRepository#getRunDirectory(String)} routes overrides into
 * {@code versions/<name>/} (VERSION_FOLDER mode), then materialise the
 * VERSION_FOLDER setting and drop the in-memory mark on success.
 */
public final class CofeMineMrpackInstaller {

    /**
     * Pack folder whose files are installed only where the instance has no such file: defaults the player may change
     * (shader on/off and choice in config/iris.properties, ...). A pack update never overwrites or deletes them.
     * Format: docs/production/pack-publishing.md in the StoryForge repository.
     */
    public static final String DEFAULTS_DIR = "overrides-default/";

    private CofeMineMrpackInstaller() {
    }

    public static Task<Void> createInstallTask(Profile profile, String mrpackUrl, String versionName) {
        return createInstallTask(profile, mrpackUrl, versionName, null);
    }

    /**
     * @param sha512 expected sha512 of the .mrpack (hex), from signed release metadata; the download fails on a
     *               mismatch. Null only for unsigned sources the user explicitly accepted.
     */
    public static Task<Void> createInstallTask(Profile profile, String mrpackUrl, String versionName, @Nullable String sha512) {
        HMCLGameRepository repository = profile.getRepository();

        // CRITICAL: mark the version as a modpack *before* the install
        // task is constructed. ModrinthInstallTask captures
        // repository.getRunDirectory(name) at construction time; without
        // the mark it returns the profile root (ROOT_FOLDER mode) and the
        // .mrpack overrides get dumped there instead of into
        // versions/<name>/.
        repository.markVersionAsModpack(versionName);

        java.util.concurrent.atomic.AtomicReference<Path> workDirRef = new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.atomic.AtomicReference<Path> mrpackFileRef = new java.util.concurrent.atomic.AtomicReference<>();

        Task<Void> prepare = Task.runAsync("CofeMine .mrpack prepare", Schedulers.io(), () -> {
            Path workDir = Files.createTempDirectory("cofemine-mrpack");
            workDirRef.set(workDir);
            mrpackFileRef.set(workDir.resolve("pack.mrpack"));
        });

        Task<Void> download = Task.composeAsync("CofeMine .mrpack download", () -> {
            Path target = mrpackFileRef.get();
            return new FileDownloadTask(mrpackUrl, target, FileDownloadTask.IntegrityCheck.of("SHA-512", sha512));
        });

        Task<Void> install = Task.composeAsync("CofeMine .mrpack install", () -> {
            Path file = mrpackFileRef.get();
            DefaultDependencyManager dm = profile.getDependency(DownloadProviders.getDownloadProvider());

            Modpack modpack;
            try (ZipArchiveReader zip = CompressingUtils.openZipFile(file, StandardCharsets.UTF_8)) {
                modpack = ModrinthModpackProvider.INSTANCE.readManifest(zip, file, StandardCharsets.UTF_8);
            }

            if (!(modpack.getManifest() instanceof ModrinthManifest manifest)) {
                throw new IllegalStateException("Pack manifest is not a Modrinth manifest");
            }

            return new ModrinthInstallTask(dm, file, modpack, manifest, versionName, null);
        });

        Task<Void> finalize = Task.runAsync("CofeMine .mrpack finalize", Schedulers.io(), () -> {
            // Persist the VERSION_FOLDER setting so the modpack stays
            // isolated even after we drop the in-memory mark, then drop
            // the mark so the in-memory bookkeeping mirrors disk state.
            try {
                repository.refreshVersions();
                VersionSetting vs = repository.specializeVersionSetting(versionName);
                if (vs != null) {
                    vs.setGameDirType(GameDirectoryType.VERSION_FOLDER);
                }
            } finally {
                repository.undoMark(versionName);
            }
        });

        Task<Void> defaults = Task.runAsync("CofeMine first-install defaults", Schedulers.io(), () -> {
            for (Path installed : installDefaults(mrpackFileRef.get(), repository.getRunDirectory(versionName))) {
                LOG.info("CofeMine pack: installed default " + installed);
            }
        });

        return prepare
                .thenComposeAsync(download)
                .thenComposeAsync(install)
                .thenComposeAsync(defaults)
                .thenComposeAsync(finalize)
                .whenComplete(Schedulers.io(), ex -> {
                    Path workDir = workDirRef.get();
                    if (workDir != null) {
                        FileUtils.deleteDirectoryQuietly(workDir);
                    }
                    if (ex != null) {
                        // Install failed — drop the mark so a retry starts
                        // from a clean slate.
                        try {
                            repository.undoMark(versionName);
                        } catch (Exception cleanupEx) {
                            LOG.warning("Failed to undo modpack mark for " + versionName, cleanupEx);
                        }
                    }
                });
    }

    /**
     * Updates an installed pack in place. On top of the install: the version folder is backed up first and put back
     * if anything fails (a failed Modrinth install otherwise deletes the version, worlds and options included), and
     * mod files the previous release listed but the new one no longer does are removed (otherwise a mod that changed
     * version stays twice in mods/ and the game refuses to start). Files the player added are never touched.
     */
    public static Task<Void> createUpdateTask(Profile profile, String mrpackUrl, String versionName, @Nullable String sha512) {
        HMCLGameRepository repository = profile.getRepository();
        Path versionRoot = repository.getVersionRoot(versionName);
        Path backup = repository.getBaseDirectory().resolve("backup").resolve(versionName + "-cofemine-update");
        List<String> previousFiles = new ArrayList<>();
        // Set by a step that only runs after the install succeeded: on cancellation HMCL may report no exception.
        java.util.concurrent.atomic.AtomicBoolean installed = new java.util.concurrent.atomic.AtomicBoolean();

        Task<Void> prepare = Task.runAsync("CofeMine update backup", Schedulers.io(), () -> {
            previousFiles.addAll(manifestFiles(repository.getModpackConfiguration(versionName)));
            if (Files.exists(backup)) FileUtils.deleteDirectory(backup);
            FileUtils.copyDirectory(versionRoot, backup);
        });

        return prepare
                .thenComposeAsync(() -> createInstallTask(profile, mrpackUrl, versionName, sha512))
                .thenRunAsync(() -> installed.set(true))
                .whenComplete(Schedulers.io(), ex -> {
                    if (installed.get()) {
                        Path runDir = repository.getRunDirectory(versionName);
                        List<String> current = manifestFiles(repository.getModpackConfiguration(versionName));
                        for (Path stale : stalePaths(runDir, previousFiles, current)) {
                            LOG.info("CofeMine update: removing " + stale + " (no longer in the pack)");
                            Files.deleteIfExists(stale);
                        }
                        FileUtils.deleteDirectoryQuietly(backup);
                    } else if (Files.isDirectory(backup)) {
                        LOG.warning("CofeMine update failed, restoring " + versionRoot + " from " + backup, ex);
                        if (Files.exists(versionRoot)) FileUtils.deleteDirectory(versionRoot);
                        FileUtils.copyDirectory(backup, versionRoot);
                        FileUtils.deleteDirectoryQuietly(backup);
                        repository.refreshVersions();
                    }
                });
    }

    /** {@code files[].path} of the Modrinth manifest recorded for an installed version; empty if unknown. */
    public static List<String> manifestFiles(Path modpackConfiguration) {
        List<String> paths = new ArrayList<>();
        if (!Files.isRegularFile(modpackConfiguration)) return paths;
        try {
            ModpackConfiguration<ModrinthManifest> config = JsonUtils.fromJsonFile(
                    modpackConfiguration, ModpackConfiguration.typeOf(ModrinthManifest.class));
            if (config != null && config.getManifest() != null && config.getManifest().getFiles() != null) {
                for (ModrinthManifest.File file : config.getManifest().getFiles()) {
                    if (file.getPath() != null) paths.add(file.getPath());
                }
            }
        } catch (Exception e) {
            LOG.warning("Unable to read the previous pack manifest " + modpackConfiguration, e);
        }
        return paths;
    }

    /** Files listed by the previous release but not the current one, resolved strictly inside {@code runDir}. */
    public static List<Path> stalePaths(Path runDir, List<String> previous, List<String> current) {
        Path root = runDir.toAbsolutePath().normalize();
        Set<Path> keep = new HashSet<>();
        for (String p : current) keep.add(root.resolve(p).normalize());
        List<Path> stale = new ArrayList<>();
        for (String p : previous) {
            Path resolved = root.resolve(p).normalize();
            if (!resolved.startsWith(root) || resolved.equals(root) || keep.contains(resolved)) continue;
            if (Files.isRegularFile(resolved)) stale.add(resolved);
        }
        return stale;
    }

    /**
     * Copies every {@link #DEFAULTS_DIR} entry of the pack to the same relative path in {@code runDir} if, and only
     * if, nothing exists there yet. Entries resolving outside {@code runDir} are ignored.
     *
     * @return the files written
     */
    public static List<Path> installDefaults(Path mrpack, Path runDir) throws IOException {
        Path root = runDir.toAbsolutePath().normalize();
        List<Path> installed = new ArrayList<>();
        try (ZipFile zip = new ZipFile(mrpack.toFile(), StandardCharsets.UTF_8)) {
            for (Enumeration<? extends ZipEntry> entries = zip.entries(); entries.hasMoreElements(); ) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName().replace('\\', '/');
                if (entry.isDirectory() || !name.startsWith(DEFAULTS_DIR) || name.length() == DEFAULTS_DIR.length()) continue;
                Path target = root.resolve(name.substring(DEFAULTS_DIR.length())).normalize();
                if (!target.startsWith(root) || target.equals(root)) {
                    LOG.warning("CofeMine pack: ignoring default outside the instance: " + name);
                    continue;
                }
                if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) continue; // the player's file wins
                Files.createDirectories(target.getParent());
                Path part = Files.createTempFile(target.getParent(), ".cofemine-default", ".part");
                try {
                    try (InputStream in = zip.getInputStream(entry)) {
                        Files.copy(in, part, StandardCopyOption.REPLACE_EXISTING);
                    }
                    Files.move(part, target); // no REPLACE_EXISTING: never clobbers a file that appeared meanwhile
                    installed.add(target);
                } catch (FileAlreadyExistsException e) {
                    LOG.info("CofeMine pack: " + target + " appeared meanwhile, keeping it");
                } finally {
                    Files.deleteIfExists(part);
                }
            }
        }
        return installed;
    }
}
