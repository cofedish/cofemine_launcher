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
import org.jackhuang.hmcl.util.io.FileUtils;

import kala.compress.archivers.zip.ZipArchiveReader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

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

    private CofeMineMrpackInstaller() {
    }

    public static Task<Void> createInstallTask(Profile profile, String mrpackUrl, String versionName) {
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
            return new FileDownloadTask(mrpackUrl, target);
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

        return prepare
                .thenComposeAsync(download)
                .thenComposeAsync(install)
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
}
