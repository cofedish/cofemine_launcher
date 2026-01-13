/*
 * Hello Minecraft! Launcher
 * Copyright (C) 2021  huangyuhui <huanghongxun2008@126.com> and contributors
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
package org.jackhuang.hmcl;

import org.jackhuang.hmcl.util.StringUtils;
import org.jackhuang.hmcl.util.io.JarUtils;
import org.jackhuang.hmcl.util.platform.Architecture;
import org.jackhuang.hmcl.util.platform.OperatingSystem;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;

/**
 * Stores metadata about this application.
 */
public final class Metadata {
    private Metadata() {
    }

    public static final String NAME = "CofeMine";
    public static final String FULL_NAME = "CofeMine Launcher";
    public static final String VERSION = System.getProperty("hmcl.version.override", JarUtils.getAttribute("hmcl.version", "@develop@"));

    public static final String TITLE = NAME + " " + VERSION;
    public static final String FULL_TITLE = FULL_NAME + " v" + VERSION;

    public static final int MINIMUM_REQUIRED_JAVA_VERSION = 17;
    public static final int MINIMUM_SUPPORTED_JAVA_VERSION = 17;
    public static final int RECOMMENDED_JAVA_VERSION = 21;

    public static final String SITE_URL = "https://cofemine.ru";
    public static final String TELEGRAM_URL = "https://t.me/+rfvGU6sDSvEwZDgy";
    public static final String DISCORD_URL = "https://discord.gg/zyKM2XAnXW";
    public static final String GITHUB_URL = "https://github.com/cofedish/cofemine_launcher";
    public static final String GITHUB_RELEASES_URL = GITHUB_URL + "/releases";
    public static final String GITHUB_ISSUES_URL = GITHUB_URL + "/issues/new/choose";

    public static final String PUBLISH_URL = SITE_URL;
    public static final String ABOUT_URL = SITE_URL;
    public static final String DOWNLOAD_URL = GITHUB_RELEASES_URL;
    public static final String HMCL_UPDATE_URL = System.getProperty("hmcl.update_source.override", GITHUB_RELEASES_URL + "/latest/download/update.json");

    public static final String DOCS_URL = SITE_URL;
    public static final String CONTACT_URL = TELEGRAM_URL;
    public static final String CHANGELOG_URL = GITHUB_RELEASES_URL;
    public static final String EULA_URL = GITHUB_URL + "/blob/main/LICENSE";
    public static final String GROUPS_URL = TELEGRAM_URL;

    public static final String BUILD_CHANNEL = JarUtils.getAttribute("hmcl.version.type", "nightly");
    public static final String GITHUB_SHA = JarUtils.getAttribute("hmcl.version.hash", null);

    public static final Path CURRENT_DIRECTORY = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
    public static final Path MINECRAFT_DIRECTORY = OperatingSystem.getWorkingDirectory("minecraft");
    public static final Path HMCL_GLOBAL_DIRECTORY;
    public static final Path HMCL_CURRENT_DIRECTORY;
    public static final Path DEPENDENCIES_DIRECTORY;

    static {
        String hmclHome = System.getProperty("hmcl.home");
        if (hmclHome == null) {
            if (OperatingSystem.CURRENT_OS.isLinuxOrBSD()) {
                String xdgData = System.getenv("XDG_DATA_HOME");
                if (StringUtils.isNotBlank(xdgData)) {
                    HMCL_GLOBAL_DIRECTORY = Paths.get(xdgData, "hmcl").toAbsolutePath().normalize();
                } else {
                    HMCL_GLOBAL_DIRECTORY = Paths.get(System.getProperty("user.home"), ".local", "share", "hmcl").toAbsolutePath().normalize();
                }
            } else {
                HMCL_GLOBAL_DIRECTORY = OperatingSystem.getWorkingDirectory("hmcl");
            }
        } else {
            HMCL_GLOBAL_DIRECTORY = Paths.get(hmclHome).toAbsolutePath().normalize();
        }

        String hmclCurrentDir = System.getProperty("hmcl.dir");
        HMCL_CURRENT_DIRECTORY = hmclCurrentDir != null
                ? Paths.get(hmclCurrentDir).toAbsolutePath().normalize()
                : CURRENT_DIRECTORY.resolve(".hmcl");
        DEPENDENCIES_DIRECTORY = HMCL_CURRENT_DIRECTORY.resolve("dependencies");
    }

    public static boolean isStable() {
        return "stable".equals(BUILD_CHANNEL);
    }

    public static boolean isDev() {
        return "dev".equals(BUILD_CHANNEL);
    }

    public static boolean isNightly() {
        return !isStable() && !isDev();
    }

    public static @Nullable String getSuggestedJavaDownloadLink() {
        if (OperatingSystem.CURRENT_OS == OperatingSystem.LINUX && Architecture.SYSTEM_ARCH == Architecture.LOONGARCH64_OW)
            return "https://www.loongnix.cn/zh/api/java/downloads-jdk21/index.html";
        else {
            EnumSet<Architecture> supportedArchitectures;
            if (OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS)
                supportedArchitectures = EnumSet.of(Architecture.X86_64, Architecture.X86, Architecture.ARM64);
            else if (OperatingSystem.CURRENT_OS == OperatingSystem.LINUX)
                supportedArchitectures = EnumSet.of(
                        Architecture.X86_64, Architecture.X86,
                        Architecture.ARM64, Architecture.ARM32,
                        Architecture.RISCV64, Architecture.LOONGARCH64
                );
            else if (OperatingSystem.CURRENT_OS == OperatingSystem.MACOS)
                supportedArchitectures = EnumSet.of(Architecture.X86_64, Architecture.ARM64);
            else
                supportedArchitectures = EnumSet.noneOf(Architecture.class);
            if (supportedArchitectures.contains(Architecture.SYSTEM_ARCH))
                return SITE_URL + "/java";
            else
                return null;
        }
    }
}
