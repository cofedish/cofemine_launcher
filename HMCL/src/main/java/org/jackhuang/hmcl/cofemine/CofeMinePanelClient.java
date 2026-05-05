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

import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import org.jackhuang.hmcl.task.Schedulers;
import org.jackhuang.hmcl.util.StringUtils;
import org.jackhuang.hmcl.util.gson.JsonUtils;
import org.jackhuang.hmcl.util.io.HttpRequest;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import static org.jackhuang.hmcl.util.logging.Logger.LOG;

/**
 * Client for the CofeMine Panel public-pack HTTP API. See
 * {@code cofemine-panel/docs/pack-integration.md} for the full contract.
 *
 * <p>Endpoints used:
 * <ul>
 *   <li>{@code GET /api/p/index.json} — list every pack with a public token.</li>
 *   <li>{@code GET /api/p/<token>.json} — metadata for a single pack.</li>
 *   <li>{@code GET /api/p/<token>.mrpack} — binary download (used elsewhere).</li>
 * </ul>
 *
 * <p>All requests are anonymous; the panel does not set CORS / cookies on
 * {@code /api/p/*}.
 */
public final class CofeMinePanelClient {

    private final String panelBaseUrl;

    public CofeMinePanelClient(String panelBaseUrl) {
        this.panelBaseUrl = normalize(panelBaseUrl);
    }

    public String getPanelBaseUrl() {
        return panelBaseUrl;
    }

    /**
     * GET {@code /api/p/index.json} — returns all packs with public tokens.
     * Empty list if the panel has none, never null.
     */
    public CompletableFuture<List<CofeMinePanelPack>> fetchIndexAsync() {
        return CompletableFuture.supplyAsync(this::fetchIndex, Schedulers.io());
    }

    public List<CofeMinePanelPack> fetchIndex() throws RuntimeException {
        String url = panelBaseUrl + "/api/p/index.json";
        try {
            String json = HttpRequest.GET(url).getString();
            IndexResponse resp = JsonUtils.fromNonNullJson(json, IndexResponse.class);
            return resp.packs == null ? Collections.emptyList() : resp.packs;
        } catch (IOException | JsonParseException e) {
            LOG.warning("Failed to load CofeMine panel index from " + url, e);
            throw new RuntimeException(e);
        }
    }

    /** GET {@code /api/p/<token>.json} — metadata for a single pack. */
    public CompletableFuture<CofeMinePanelPack> fetchMetadataAsync(String token) {
        return CompletableFuture.supplyAsync(() -> fetchMetadata(token), Schedulers.io());
    }

    public CofeMinePanelPack fetchMetadata(String token) {
        String url = panelBaseUrl + "/api/p/" + token + ".json";
        try {
            String json = HttpRequest.GET(url).getString();
            return JsonUtils.fromNonNullJson(json, CofeMinePanelPack.class);
        } catch (IOException | JsonParseException e) {
            LOG.warning("Failed to load CofeMine panel metadata from " + url, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Pull the {@code <hex32>} token out of a panel mrpack URL. Returns null
     * if the URL doesn't look like a panel pack URL.
     */
    public static @Nullable String extractTokenFromMrpackUrl(@Nullable String url) {
        if (StringUtils.isBlank(url)) return null;
        int slash = url.lastIndexOf('/');
        if (slash < 0) return null;
        String tail = url.substring(slash + 1);
        int dot = tail.indexOf('.');
        if (dot < 0) return null;
        String token = tail.substring(0, dot);
        if (token.length() != 32) return null;
        for (int i = 0; i < token.length(); i++) {
            char c = Character.toLowerCase(token.charAt(i));
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f'))) {
                return null;
            }
        }
        return token.toLowerCase(Locale.ROOT);
    }

    private static String normalize(String url) {
        if (url == null) return "";
        String trimmed = url.trim();
        while (trimmed.endsWith("/")) trimmed = trimmed.substring(0, trimmed.length() - 1);
        return trimmed;
    }

    private static final class IndexResponse {
        @SerializedName("packs")
        List<CofeMinePanelPack> packs;
        @SerializedName("generatedAt")
        String generatedAt;
    }
}
