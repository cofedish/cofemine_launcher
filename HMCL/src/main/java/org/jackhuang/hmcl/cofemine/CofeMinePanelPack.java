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

    /** Publisher's build id ({@code <version>+<stamp>.<commit>}); tells two releases apart. */
    @SerializedName("versionId")
    private @Nullable String versionId;

    /** sha512 of the .mrpack at {@link #mrpackUrl} (hex); set in signed release metadata. */
    @SerializedName("sha512")
    private @Nullable String sha512;

    /** The channel's signed release metadata ({@link CofeMinePackTrust}); absent for unsigned sources. */
    @SerializedName("signedMetadataUrl")
    private @Nullable String signedMetadataUrl;

    public CofeMinePanelPack() {
    }

    /** An index-less entry: only what an installed pack recorded (used for unsigned updates). */
    public static CofeMinePanelPack unsigned(String id, String displayName, String mrpackUrl) {
        CofeMinePanelPack pack = new CofeMinePanelPack();
        pack.id = id;
        pack.displayName = displayName;
        pack.mrpackUrl = mrpackUrl;
        return pack;
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

    public @Nullable String getVersionId() { return versionId; }

    public @Nullable String getSha512() { return sha512; }

    public @Nullable String getSignedMetadataUrl() { return signedMetadataUrl; }
}
