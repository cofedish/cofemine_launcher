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
import org.jackhuang.hmcl.mod.Modpack;
import org.jackhuang.hmcl.mod.modrinth.ModrinthInstallTask;
import org.jackhuang.hmcl.mod.modrinth.ModrinthManifest;
import org.jackhuang.hmcl.mod.modrinth.ModrinthModpackProvider;
import org.jackhuang.hmcl.setting.DownloadProviders;
import org.jackhuang.hmcl.setting.Profile;
import org.jackhuang.hmcl.task.FileDownloadTask;
import org.jackhuang.hmcl.task.Schedulers;
import org.jackhuang.hmcl.task.Task;
import org.jackhuang.hmcl.util.io.CompressingUtils;
import org.jackhuang.hmcl.util.io.FileUtils;

import kala.compress.archivers.zip.ZipArchiveReader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Installs / updates a CofeMine pack delivered as a Modrinth-format
 * {@code .mrpack} archive served by the panel. The Modrinth flow already
 * understands {@code modrinth.index.json}, applies overrides, and chains
 * the Minecraft + loader install — we just download the file from the
 * panel URL and hand it off.
 */
public final class CofeMineMrpackInstaller {

    private CofeMineMrpackInstaller() {
    }

    /**
     * Build a task that downloads the {@code .mrpack} from the panel and
     * installs it as a fresh version inside {@code profile}.
     *
     * @param profile      target HMCL profile (game directory + version repo)
     * @param mrpackUrl    direct {@code /api/p/<token>.mrpack} URL
     * @param versionName  version id under which the pack is registered
     */
    public static Task<Void> createInstallTask(Profile profile, String mrpackUrl, String versionName) {
        // Allocate a per-invocation working dir lazily so IOException flows
        // through the task chain instead of being thrown at configuration.
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

        return prepare
                .thenComposeAsync(download)
                .thenComposeAsync(install)
                .whenComplete(Schedulers.io(), ex -> {
                    Path workDir = workDirRef.get();
                    if (workDir != null) {
                        FileUtils.deleteDirectoryQuietly(workDir);
                    }
                });
    }
}
