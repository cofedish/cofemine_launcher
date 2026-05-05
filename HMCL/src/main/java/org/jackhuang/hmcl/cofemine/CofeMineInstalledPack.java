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
 * Persisted record describing a single CofeMine modpack the user installed
 * through the panel. Stored as a JSON array under {@code config.cofeminePacks}.
 *
 * <p>{@code id} is the panel-side pack id (UUID-like string); {@code mrpackUrl}
 * is the direct .mrpack endpoint used for re-downloading on update.
 */
public final class CofeMineInstalledPack {

    @SerializedName("id")
    private String id;

    @SerializedName("displayName")
    private String displayName;

    @SerializedName("instancePath")
    private String instancePath;

    @SerializedName("versionName")
    private String versionName;

    @SerializedName("mrpackUrl")
    private String mrpackUrl;

    @SerializedName("metadataUrl")
    private @Nullable String metadataUrl;

    @SerializedName("minecraft")
    private @Nullable String minecraft;

    @SerializedName("loader")
    private @Nullable String loader;

    @SerializedName("loaderVersion")
    private @Nullable String loaderVersion;

    @SerializedName("installedAt")
    private @Nullable String installedAt;

    @SerializedName("updatedAt")
    private @Nullable String updatedAt;

    public CofeMineInstalledPack() {
    }

    public CofeMineInstalledPack(String id, String displayName, String instancePath,
                                 String versionName, String mrpackUrl,
                                 @Nullable String metadataUrl, @Nullable String minecraft,
                                 @Nullable String loader, @Nullable String loaderVersion,
                                 @Nullable String installedAt, @Nullable String updatedAt) {
        this.id = id;
        this.displayName = displayName;
        this.instancePath = instancePath;
        this.versionName = versionName;
        this.mrpackUrl = mrpackUrl;
        this.metadataUrl = metadataUrl;
        this.minecraft = minecraft;
        this.loader = loader;
        this.loaderVersion = loaderVersion;
        this.installedAt = installedAt;
        this.updatedAt = updatedAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getInstancePath() { return instancePath; }
    public void setInstancePath(String instancePath) { this.instancePath = instancePath; }

    public String getVersionName() { return versionName; }
    public void setVersionName(String versionName) { this.versionName = versionName; }

    public String getMrpackUrl() { return mrpackUrl; }
    public void setMrpackUrl(String mrpackUrl) { this.mrpackUrl = mrpackUrl; }

    public @Nullable String getMetadataUrl() { return metadataUrl; }
    public void setMetadataUrl(@Nullable String metadataUrl) { this.metadataUrl = metadataUrl; }

    public @Nullable String getMinecraft() { return minecraft; }
    public void setMinecraft(@Nullable String minecraft) { this.minecraft = minecraft; }

    public @Nullable String getLoader() { return loader; }
    public void setLoader(@Nullable String loader) { this.loader = loader; }

    public @Nullable String getLoaderVersion() { return loaderVersion; }
    public void setLoaderVersion(@Nullable String loaderVersion) { this.loaderVersion = loaderVersion; }

    public @Nullable String getInstalledAt() { return installedAt; }
    public void setInstalledAt(@Nullable String installedAt) { this.installedAt = installedAt; }

    public @Nullable String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(@Nullable String updatedAt) { this.updatedAt = updatedAt; }
}
