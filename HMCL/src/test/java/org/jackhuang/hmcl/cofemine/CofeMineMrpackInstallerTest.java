package org.jackhuang.hmcl.cofemine;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/** Pack files beyond mods/: first-install defaults (overrides-default/) and stale shader/resource packs. */
public class CofeMineMrpackInstallerTest {

    private static Path pack(Path dir, Map<String, String> entries) throws IOException {
        Path file = dir.resolve("pack.mrpack");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(file))) {
            for (Map.Entry<String, String> e : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(e.getKey()));
                zip.write(e.getValue().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return file;
    }

    @Test
    public void defaultsAreInstalledOnlyWhereThePlayerHasNoFile(@TempDir Path dir) throws Exception {
        Path run = Files.createDirectories(dir.resolve("versions/CofeMine"));
        Files.createDirectories(run.resolve("config"));
        Files.writeString(run.resolve("config/iris.properties"), "enableShaders=false\n"); // the player turned shaders off
        Path mrpack = pack(dir, Map.of(
                "overrides-default/config/iris.properties", "enableShaders=true\nshaderPack=pack.zip\n",
                "overrides-default/config/sub/new.toml", "a=1\n",
                "overrides-default/../../escape.txt", "x",
                "overrides/config/other.toml", "not a default"));

        List<Path> installed = CofeMineMrpackInstaller.installDefaults(mrpack, run);

        assertEquals(List.of(run.resolve("config/sub/new.toml").toAbsolutePath().normalize()), installed);
        assertEquals("enableShaders=false\n", Files.readString(run.resolve("config/iris.properties")));
        assertEquals("a=1\n", Files.readString(run.resolve("config/sub/new.toml")));
        assertFalse(Files.exists(dir.resolve("escape.txt")));
        assertFalse(Files.exists(run.resolve("config/other.toml")), "overrides/ is HMCL's business, not this step's");

        // A second run (pack update) writes nothing: every default now exists.
        assertEquals(List.of(), CofeMineMrpackInstaller.installDefaults(mrpack, run));
        // A default the player deleted comes back (the documented way to reset it).
        Files.delete(run.resolve("config/iris.properties"));
        CofeMineMrpackInstaller.installDefaults(mrpack, run);
        assertEquals("enableShaders=true\nshaderPack=pack.zip\n", Files.readString(run.resolve("config/iris.properties")));
    }

    @Test
    public void shaderAndResourcePacksDroppedFromTheLockAreRemoved(@TempDir Path dir) throws Exception {
        Path run = Files.createDirectories(dir.resolve("versions/CofeMine"));
        for (String f : List.of("shaderpacks/old-1.0.zip", "shaderpacks/mine.zip", "resourcepacks/rp-1.zip", "mods/a.jar")) {
            Files.createDirectories(run.resolve(f).getParent());
            Files.writeString(run.resolve(f), "x");
        }
        List<Path> stale = CofeMineMrpackInstaller.stalePaths(run,
                List.of("shaderpacks/old-1.0.zip", "resourcepacks/rp-1.zip", "mods/a.jar"),
                List.of("shaderpacks/new-2.0.zip", "mods/a.jar"));
        assertEquals(List.of(run.resolve("shaderpacks/old-1.0.zip").toAbsolutePath().normalize(),
                run.resolve("resourcepacks/rp-1.zip").toAbsolutePath().normalize()), stale);
    }
}
