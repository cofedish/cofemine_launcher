/*
 * Hello Minecraft! Launcher
 * Copyright (C) 2025 huangyuhui <huanghongxun2008@126.com> and contributors
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
package org.jackhuang.hmcl.download;

import org.jackhuang.hmcl.task.Task;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/**
 * @author Glavo
 */
public final class DownloadProviderWrapper implements DownloadProvider {

    private static final String DEFAULT_BMCLAPI_ROOT = "https://bmclapi2.bangbang93.com";
    private static final DownloadProvider FALLBACK_PROVIDER =
            new BMCLAPIDownloadProvider(System.getProperty("hmcl.bmclapi.override", DEFAULT_BMCLAPI_ROOT));
    private static final List<String> FORGE_MAVEN_PREFIXES = List.of(
            "https://maven.minecraftforge.net/",
            "https://files.minecraftforge.net/maven",
            "http://files.minecraftforge.net/maven",
            "https://maven.neoforged.net/releases/",
            "https://maven.neoforged.net/releases"
    );

    private volatile DownloadProvider provider;

    public DownloadProviderWrapper(DownloadProvider provider) {
        this.provider = provider;
    }

    public DownloadProvider getProvider() {
        return this.provider;
    }

    public void setProvider(DownloadProvider provider) {
        this.provider = Objects.requireNonNull(provider);
    }

    @Override
    public List<URI> getAssetObjectCandidates(String assetObjectLocation) {
        return getProvider().getAssetObjectCandidates(assetObjectLocation);
    }

    @Override
    public List<URI> getVersionListURLs() {
        return getProvider().getVersionListURLs();
    }

    @Override
    public String injectURL(String baseURL) {
        return getProvider().injectURL(baseURL);
    }

    @Override
    public List<URI> injectURLWithCandidates(String baseURL) {
        List<URI> primary = getProvider().injectURLWithCandidates(baseURL);
        if (!shouldAddForgeFallback(baseURL)) {
            return primary;
        }
        LinkedHashSet<URI> result = new LinkedHashSet<>(primary);
        result.addAll(FALLBACK_PROVIDER.injectURLWithCandidates(baseURL));
        return List.copyOf(result);
    }

    @Override
    public List<URI> injectURLsWithCandidates(List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<URI> result = new LinkedHashSet<>();
        for (String url : urls) {
            result.addAll(injectURLWithCandidates(url));
        }
        return List.copyOf(result);
    }

    @Override
    public VersionList<?> getVersionListById(String id) {

        return new VersionList<>() {
            @Override
            public boolean hasType() {
                return getProvider().getVersionListById(id).hasType();
            }

            @Override
            public Task<?> refreshAsync() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Task<?> refreshAsync(String gameVersion) {
                return getProvider().getVersionListById(id).refreshAsync(gameVersion)
                        .thenComposeAsync(() -> {
                            lock.writeLock().lock();
                            try {
                                versions.putAll(gameVersion, getProvider().getVersionListById(id).getVersions(gameVersion));
                            } finally {
                                lock.writeLock().unlock();
                            }
                            return null;
                        });
            }
        };
    }

    @Override
    public int getConcurrency() {
        return getProvider().getConcurrency();
    }

    private static boolean shouldAddForgeFallback(String baseURL) {
        if (baseURL == null) {
            return false;
        }
        for (String prefix : FORGE_MAVEN_PREFIXES) {
            if (baseURL.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "DownloadProviderWrapper[provider=%s]".formatted(provider);
    }
}
