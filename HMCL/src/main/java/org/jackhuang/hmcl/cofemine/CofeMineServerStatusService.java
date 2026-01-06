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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.jackhuang.hmcl.task.Schedulers;
import org.jackhuang.hmcl.util.Lang;
import org.jackhuang.hmcl.util.gson.JsonUtils;
import org.jetbrains.annotations.Nullable;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Hashtable;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.jackhuang.hmcl.util.logging.Logger.LOG;

public final class CofeMineServerStatusService implements AutoCloseable {
    private static final int DEFAULT_PORT = 25565;
    private static final int PROTOCOL_VERSION = 758;
    private static final int TIMEOUT_MS = 3000;

    private final String host;
    private final int port;
    private final Duration refreshInterval;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean refreshing = new AtomicBoolean(false);
    private final ObjectProperty<CofeMineServerStatus> status = new SimpleObjectProperty<>(CofeMineServerStatus.loading());
    private ScheduledFuture<?> refreshTask;

    public CofeMineServerStatusService(String host, int port, Duration refreshInterval) {
        this.host = Objects.requireNonNullElse(host, "");
        this.port = port > 0 ? port : DEFAULT_PORT;
        this.refreshInterval = refreshInterval == null ? Duration.ofSeconds(30) : refreshInterval;
        this.scheduler = java.util.concurrent.Executors.newSingleThreadScheduledExecutor(
                Lang.counterThreadFactory("CofeMine-ServerStatus", true));
    }

    public ReadOnlyObjectProperty<CofeMineServerStatus> statusProperty() {
        return status;
    }

    public CofeMineServerStatus getStatus() {
        return status.get();
    }

    public void start() {
        refreshNow();
        if (refreshTask == null) {
            refreshTask = scheduler.scheduleAtFixedRate(this::refreshNow,
                    refreshInterval.toSeconds(),
                    refreshInterval.toSeconds(),
                    TimeUnit.SECONDS);
        }
    }

    public void refreshNow() {
        if (!refreshing.compareAndSet(false, true)) {
            return;
        }

        CompletableFuture.supplyAsync(() -> {
            try {
                HostAndPort resolved = resolveSrv(host, port).orElse(new HostAndPort(host, port));
                return queryStatus(resolved.host(), resolved.port());
            } catch (Exception e) {
                return CofeMineServerStatus.offline(e.getClass().getSimpleName());
            }
        }, Schedulers.io()).orTimeout(TIMEOUT_MS + 1000L, TimeUnit.MILLISECONDS).whenComplete((result, error) -> {
            CofeMineServerStatus resolved = result;
            if (error != null) {
                LOG.warning("Failed to query CofeMine server status", error);
                resolved = CofeMineServerStatus.offline(error.getClass().getSimpleName());
            }
            CofeMineServerStatus finalResult = resolved == null ? CofeMineServerStatus.offline("unknown") : resolved;
            Platform.runLater(() -> status.set(finalResult));
            refreshing.set(false);
        });
    }

    private CofeMineServerStatus queryStatus(String host, int port) throws IOException {
        long start = System.nanoTime();

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), TIMEOUT_MS);
            socket.setSoTimeout(TIMEOUT_MS);

            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            InputStream in = socket.getInputStream();

            sendPacket(out, buildHandshake(host, port));
            sendPacket(out, new byte[] { 0x00 });

            readVarInt(in); // packet length
            int packetId = readVarInt(in);
            if (packetId != 0x00) {
                throw new IOException("Unexpected packet id: " + packetId);
            }

            int jsonLength = readVarInt(in);
            if (jsonLength <= 0) {
                throw new IOException("Empty status response");
            }

            byte[] jsonBytes = in.readNBytes(jsonLength);
            String json = new String(jsonBytes, StandardCharsets.UTF_8);

            long ping = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

            JsonObject root = JsonUtils.GSON.fromJson(json, JsonObject.class);
            String motd = flattenDescription(root.get("description"));
            JsonObject players = root.has("players") && root.get("players").isJsonObject()
                    ? root.getAsJsonObject("players")
                    : null;
            int online = players != null && players.has("online") ? players.get("online").getAsInt() : -1;
            int max = players != null && players.has("max") ? players.get("max").getAsInt() : -1;

            return new CofeMineServerStatus(true, online, max, motd, ping, null);
        }
    }

    private static Optional<HostAndPort> resolveSrv(String host, int port) {
        if (host == null || host.isBlank()) {
            return Optional.empty();
        }

        String query = "_minecraft._tcp." + host;
        Hashtable<String, String> env = new Hashtable<>();
        env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");

        try {
            DirContext context = new InitialDirContext(env);
            Attributes attrs = context.getAttributes(query, new String[] { "SRV" });
            Attribute attr = attrs.get("SRV");
            if (attr == null || attr.size() == 0) {
                return Optional.empty();
            }

            String record = attr.get().toString();
            String[] parts = record.split(" ");
            if (parts.length < 4) {
                return Optional.empty();
            }

            int srvPort = Integer.parseInt(parts[2]);
            String srvHost = parts[3].endsWith(".") ? parts[3].substring(0, parts[3].length() - 1) : parts[3];
            return Optional.of(new HostAndPort(srvHost, srvPort > 0 ? srvPort : port));
        } catch (NamingException | RuntimeException e) {
            LOG.debug("Failed to resolve SRV for " + host, e);
            return Optional.empty();
        }
    }

    private static byte[] buildHandshake(String host, int port) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(buffer);
        writeVarInt(out, 0x00);
        writeVarInt(out, PROTOCOL_VERSION);
        writeString(out, host);
        out.writeShort(port);
        writeVarInt(out, 1);
        return buffer.toByteArray();
    }

    private static void sendPacket(DataOutputStream out, byte[] data) throws IOException {
        writeVarInt(out, data.length);
        out.write(data);
        out.flush();
    }

    private static void writeString(DataOutputStream out, String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        writeVarInt(out, bytes.length);
        out.write(bytes);
    }

    private static void writeVarInt(DataOutputStream out, int value) throws IOException {
        int part = value;
        while (true) {
            if ((part & 0xFFFFFF80) == 0) {
                out.writeByte(part);
                return;
            }
            out.writeByte((part & 0x7F) | 0x80);
            part >>>= 7;
        }
    }

    private static int readVarInt(InputStream in) throws IOException {
        int numRead = 0;
        int result = 0;
        int read;
        do {
            read = in.read();
            if (read == -1) {
                throw new IOException("Unexpected end of stream");
            }
            int value = (read & 0x7F);
            result |= (value << (7 * numRead));
            numRead++;
            if (numRead > 5) {
                throw new IOException("VarInt too big");
            }
        } while ((read & 0x80) != 0);
        return result;
    }

    private static @Nullable String flattenDescription(@Nullable JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        if (element.isJsonPrimitive()) {
            return element.getAsString();
        }
        if (element.isJsonArray()) {
            StringBuilder builder = new StringBuilder();
            for (JsonElement item : element.getAsJsonArray()) {
                String part = flattenDescription(item);
                if (part != null) {
                    builder.append(part);
                }
            }
            return builder.toString();
        }
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            StringBuilder builder = new StringBuilder();
            if (obj.has("text")) {
                builder.append(obj.get("text").getAsString());
            }
            if (obj.has("extra") && obj.get("extra").isJsonArray()) {
                for (JsonElement extra : obj.getAsJsonArray("extra")) {
                    String part = flattenDescription(extra);
                    if (part != null) {
                        builder.append(part);
                    }
                }
            }
            return builder.toString();
        }
        return null;
    }

    @Override
    public void close() {
        if (refreshTask != null) {
            refreshTask.cancel(true);
        }
        scheduler.shutdownNow();
    }

    private record HostAndPort(String host, int port) {
    }
}
