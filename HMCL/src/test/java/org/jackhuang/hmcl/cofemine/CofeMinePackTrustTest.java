package org.jackhuang.hmcl.cofemine;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jackhuang.hmcl.mod.ModpackConfiguration;
import org.jackhuang.hmcl.mod.modrinth.ModrinthManifest;
import org.jackhuang.hmcl.util.gson.JsonUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Signed pack metadata as published by StoryForge's tools/pack/sfpublish.sh. release.sig.json is a real envelope from
 * https://panel.cofemine.ru/api/p/ (2026-11-01), signed with the pinned production key.
 */
public class CofeMinePackTrustTest {

    private static String fixture() throws IOException {
        try (InputStream in = CofeMinePackTrustTest.class.getResourceAsStream("/cofemine/release.sig.json")) {
            assertNotNull(in);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String withField(String envelope, String field, String value) {
        JsonObject o = JsonParser.parseString(envelope).getAsJsonObject();
        o.addProperty(field, value);
        return o.toString();
    }

    private static String b64(byte[] b) {
        return Base64.getEncoder().encodeToString(b);
    }

    @Test
    public void publishedReleaseVerifiesWithThePinnedKey() throws Exception {
        CofeMinePanelPack release = CofeMinePanelClient.parseSignedRelease(fixture(), "cofemine", CofeMinePackTrust.PINNED_KEYS);
        assertEquals("cofemine", release.getId());
        assertEquals("0.1.0-alpha.1+20261101.0748.a34ae536", release.getVersionId());
        assertTrue(release.getMrpackUrl().startsWith("https://panel.cofemine.ru/api/p/v/"));
        assertEquals(128, release.getSha512().length());
    }

    @Test
    public void tamperedPayloadIsRejected() throws Exception {
        String envelope = fixture();
        String payload = new String(Base64.getDecoder().decode(
                JsonParser.parseString(envelope).getAsJsonObject().get("payload").getAsString()), StandardCharsets.UTF_8);
        String evil = payload.replace("https://panel.cofemine.ru/api/p/v/", "https://evil.example/");
        assertNotEquals(payload, evil);
        String forged = withField(envelope, "payload", b64(evil.getBytes(StandardCharsets.UTF_8)));
        assertThrows(GeneralSecurityException.class, () -> CofeMinePackTrust.verifyEnvelope(forged));
        assertThrows(IOException.class, () -> CofeMinePanelClient.parseSignedRelease(forged, "cofemine", CofeMinePackTrust.PINNED_KEYS));
    }

    @Test
    public void otherKeysAndFormatsAreRejected() throws Exception {
        String envelope = fixture();
        assertThrows(GeneralSecurityException.class, () -> CofeMinePackTrust.verifyEnvelope(withField(envelope, "keyId", "0000000000000000")));
        assertThrows(GeneralSecurityException.class, () -> CofeMinePackTrust.verifyEnvelope(withField(envelope, "format", "other/1")));
        assertThrows(GeneralSecurityException.class, () -> CofeMinePackTrust.verifyEnvelope("[]"));
        assertThrows(GeneralSecurityException.class, () -> CofeMinePackTrust.verifyEnvelope("not json"));

        // A valid signature by a key that is not pinned: an attacker who controls the server and signs with their own key.
        KeyPair attacker = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        byte[] payload = "{\"id\":\"cofemine\"}".getBytes(StandardCharsets.UTF_8);
        Signature s = Signature.getInstance("Ed25519");
        s.initSign(attacker.getPrivate());
        s.update(payload);
        String keyId = CofeMinePackTrust.keyIdOf(attacker.getPublic().getEncoded());
        String own = "{\"format\":\"" + CofeMinePackTrust.ENVELOPE_FORMAT + "\",\"keyId\":\"" + keyId
                + "\",\"payload\":\"" + b64(payload) + "\",\"signature\":\"" + b64(s.sign()) + "\"}";
        assertThrows(GeneralSecurityException.class, () -> CofeMinePackTrust.verifyEnvelope(own));
        assertArrayEquals(payload, CofeMinePackTrust.verifyEnvelope(own, Map.of(keyId, b64(attacker.getPublic().getEncoded()))));
    }

    @Test
    public void releaseOfAnotherChannelIsRejected() {
        assertThrows(IOException.class, () -> CofeMinePanelClient.parseSignedRelease(fixture(), "cofemine-test", CofeMinePackTrust.PINNED_KEYS));
    }

    @Test
    public void officialSourceAndHttps() {
        assertTrue(CofeMinePackTrust.isOfficialSource("https://panel.cofemine.ru/api/p/x.mrpack"));
        assertTrue(CofeMinePackTrust.isOfficialSource("https://PANEL.cofemine.ru/api/p/x.mrpack"));
        assertFalse(CofeMinePackTrust.isOfficialSource("https://panel.cofemine.ru.evil.example/x.mrpack"));
        assertFalse(CofeMinePackTrust.isOfficialSource(null));
        assertTrue(CofeMinePackTrust.isHttps("https://example.org/x.mrpack"));
        assertFalse(CofeMinePackTrust.isHttps("http://panel.cofemine.ru/x.mrpack"));
        assertFalse(CofeMinePackTrust.isHttps("file:///etc/passwd"));
    }

    @Test
    public void updateRemovesOnlyFilesTheOldReleaseListedAndStaysInsideTheInstance(@TempDir Path dir) throws Exception {
        Path run = dir.resolve("versions/CofeMine");
        Files.createDirectories(run.resolve("mods"));
        for (String f : List.of("mods/a-1.0.jar", "mods/b-1.0.jar", "mods/mine.jar")) Files.writeString(run.resolve(f), "x");
        Path outside = Files.writeString(dir.resolve("secret.txt"), "x");

        List<Path> stale = CofeMineMrpackInstaller.stalePaths(run,
                List.of("mods/a-1.0.jar", "mods/b-1.0.jar", "../../secret.txt", "mods/missing.jar"),
                List.of("mods/a-1.0.jar", "mods/b-2.0.jar"));

        assertEquals(List.of(run.resolve("mods/b-1.0.jar").toAbsolutePath().normalize()), stale);
        assertTrue(Files.exists(outside));
    }

    @Test
    public void previousFilesComeFromTheRecordedModpackConfiguration(@TempDir Path dir) throws Exception {
        ModrinthManifest manifest = JsonUtils.GSON.fromJson("{\"formatVersion\":1,\"game\":\"minecraft\",\"versionId\":\"1\","
                + "\"name\":\"CofeMine\",\"dependencies\":{\"minecraft\":\"1.21.1\"},\"files\":["
                + "{\"path\":\"mods/a-1.0.jar\",\"hashes\":{\"sha1\":\"00\"},\"downloads\":[\"https://cdn.modrinth.com/a\"],\"fileSize\":1},"
                + "{\"path\":\"mods/b-1.0.jar\",\"hashes\":{\"sha1\":\"00\"},\"downloads\":[\"https://cdn.modrinth.com/b\"],\"fileSize\":1}]}",
                ModrinthManifest.class);
        Path json = dir.resolve("modpack.json");
        Files.writeString(json, JsonUtils.GSON.toJson(new ModpackConfiguration<>(manifest, "Modrinth", "CofeMine", "1", List.of())));

        assertEquals(List.of("mods/a-1.0.jar", "mods/b-1.0.jar"), CofeMineMrpackInstaller.manifestFiles(json));
        assertEquals(List.of(), CofeMineMrpackInstaller.manifestFiles(dir.resolve("missing.json")));
    }
}
