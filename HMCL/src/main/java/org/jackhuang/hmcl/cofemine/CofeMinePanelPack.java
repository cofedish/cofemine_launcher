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

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.Nullable;

/**
 * One entry from {@code GET /api/p/index.json} or {@code GET /api/p/<token>.json}
 * served by the CofeMine Panel. Mirrors the JSON contract documented in
 * {@code cofemine-panel/docs/pack-integration.md} §2.2 / §2.3.
 */
public final class CofeMinePanelPack {

    @SerializedName("id")
    private String id;

    @SerializedName("displayName")
    private String displayName;

    @SerializedName("versionName")
    private @Nullable String versionName;

    @SerializedName("minecraft")
    private @Nullable String minecraft;

    @SerializedName("loader")
    private @Nullable String loader;

    @SerializedName("loaderVersion")
    private @Nullable String loaderVersion;

    @SerializedName("mrpackUrl")
    private String mrpackUrl;

    @SerializedName("metadataUrl")
    private @Nullable String metadataUrl;

    @SerializedName("updatedAt")
    private @Nullable String updatedAt;

    public CofeMinePanelPack() {
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public @Nullable String getVersionName() { return versionName; }
    public @Nullable String getMinecraft() { return minecraft; }
    public @Nullable String getLoader() { return loader; }
    public @Nullable String getLoaderVersion() { return loaderVersion; }
    public String getMrpackUrl() { return mrpackUrl; }
    public @Nullable String getMetadataUrl() { return metadataUrl; }
    public @Nullable String getUpdatedAt() { return updatedAt; }
}
