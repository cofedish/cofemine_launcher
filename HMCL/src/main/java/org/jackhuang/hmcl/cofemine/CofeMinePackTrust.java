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
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Who may ship code to this launcher. A pack is a set of mods, i.e. code that runs on the player's machine, so the
 * official packs are signed (Ed25519) by a key that never leaves the publisher's machine; a compromised web server
 * can then serve old signed releases at worst, never its own jars.
 *
 * <p>The publisher uploads one self-contained envelope per channel ({@code <token>.sig.json}):
 * <pre>{"format": "cofemine-pack-signature/1", "keyId": "...", "payload": "&lt;base64&gt;", "signature": "&lt;base64&gt;"}</pre>
 * The signature covers the exact payload bytes; the payload is the release metadata ({@link CofeMinePanelPack}) with
 * the sha512 of the {@code .mrpack}, whose index in turn pins every mod file by hash. Publisher side:
 * {@code tools/pack/PackSign.java} in the StoryForge repository.
 */
public final class CofeMinePackTrust {

    public static final String ENVELOPE_FORMAT = "cofemine-pack-signature/1";

    /** keyId (first 16 hex of sha256 of the X.509 encoding) -> X.509 public key, base64. */
    static final Map<String, String> PINNED_KEYS = Map.of(
            "893c26768e348daf", "MCowBQYDK2VwAyEAPNakz34IAUXP58KkNhoJfRR75S2wojp21YQU6gIDA0Q="
    );

    /** Sources whose packs must carry a valid signature: an unsigned pack from here is an attack, not a choice. */
    static final Set<String> OFFICIAL_HOSTS = Set.of("panel.cofemine.ru");

    private CofeMinePackTrust() {
    }

    public static boolean isOfficialSource(@Nullable String url) {
        String host = hostOf(url);
        return host != null && OFFICIAL_HOSTS.contains(host);
    }

    public static boolean isHttps(@Nullable String url) {
        try {
            return url != null && "https".equals(URI.create(url).getScheme()) && hostOf(url) != null;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static @Nullable String hostOf(@Nullable String url) {
        if (url == null) return null;
        try {
            String host = URI.create(url.trim()).getHost();
            return host == null ? null : host.toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Verifies an envelope against the pinned keys and returns the signed payload bytes. */
    public static byte[] verifyEnvelope(String envelopeJson) throws GeneralSecurityException {
        return verifyEnvelope(envelopeJson, PINNED_KEYS);
    }

    static byte[] verifyEnvelope(String envelopeJson, Map<String, String> keys) throws GeneralSecurityException {
        JsonObject envelope;
        try {
            JsonElement parsed = JsonParser.parseString(envelopeJson);
            if (!parsed.isJsonObject()) throw new GeneralSecurityException("signature envelope is not a JSON object");
            envelope = parsed.getAsJsonObject();
        } catch (JsonParseException e) {
            throw new GeneralSecurityException("signature envelope is not valid JSON", e);
        }
        if (!ENVELOPE_FORMAT.equals(string(envelope, "format")))
            throw new GeneralSecurityException("unknown signature format " + string(envelope, "format"));
        String keyId = string(envelope, "keyId");
        String encodedKey = keyId == null ? null : keys.get(keyId);
        if (encodedKey == null) throw new GeneralSecurityException("signed by an unknown key " + keyId);

        byte[] keyBytes = Base64.getDecoder().decode(encodedKey);
        if (!keyId.equals(keyIdOf(keyBytes))) throw new GeneralSecurityException("pinned key does not match its id " + keyId);
        PublicKey key = KeyFactory.getInstance("Ed25519").generatePublic(new X509EncodedKeySpec(keyBytes));

        byte[] payload;
        byte[] signature;
        try {
            payload = Base64.getDecoder().decode(require(envelope, "payload"));
            signature = Base64.getDecoder().decode(require(envelope, "signature"));
        } catch (IllegalArgumentException e) {
            throw new GeneralSecurityException("signature envelope is not base64", e);
        }
        Signature verifier = Signature.getInstance("Ed25519");
        verifier.initVerify(key);
        verifier.update(payload);
        if (!verifier.verify(signature)) throw new GeneralSecurityException("signature does not match the pack metadata");
        return payload;
    }

    static String keyIdOf(byte[] x509) throws GeneralSecurityException {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(x509)).substring(0, 16);
    }

    private static @Nullable String string(JsonObject o, String name) {
        JsonElement e = o.get(name);
        return e != null && e.isJsonPrimitive() ? e.getAsString() : null;
    }

    private static String require(JsonObject o, String name) throws GeneralSecurityException {
        String v = string(o, name);
        if (v == null) throw new GeneralSecurityException("signature envelope has no " + name);
        return v;
    }
}
