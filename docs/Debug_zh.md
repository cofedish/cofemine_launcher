# Debug Options

> [!WARNING]
> These options are intended for troubleshooting and internal use. Incorrect values can cause crashes or undefined behavior.

CofeMine Launcher supports a set of environment variables and JVM properties.

| Key | Description | Default / Notes |
|---|---|---|
| `HMCL_JAVA_HOME` | Use a specific Java runtime for the launcher executable. | exe/sh only |
| `HMCL_JAVA_OPTS` | Default JVM arguments for the launcher. | exe/sh only |
| `HMCL_FORCE_GPU` | Force GPU rendering. | `false` |
| `HMCL_ANIMATION_FRAME_RATE` | Animation frame rate. | `60` |
| `HMCL_LANGUAGE` | Default UI language tag. | Uses system locale |
| `-Dhmcl.dir=<path>` | Current data directory. | `./.hmcl` |
| `-Dhmcl.home=<path>` | Global data directory. | Windows: `%APPDATA%\.hmcl`, Linux/BSD: `$XDG_DATA_HOME/hmcl`, macOS: `~/Library/Application Support/hmcl` |
| `-Dhmcl.self_integrity_check.disable=true` | Disable self integrity check when updating. |  |
| `-Dhmcl.bmclapi.override=<url>` | Override BMCLAPI root URL. | `https://bmclapi2.bangbang93.com` |
| `-Dhmcl.discoapi.override=<url>` | Override foojay Disco API root URL. | `https://api.foojay.io/disco/v3.0` |
| `HMCL_FONT` / `-Dhmcl.font.override=<font family>` | Override the default UI font. |  |
| `-Dhmcl.update_source.override=<url>` | Override update source. | `https://github.com/cofedish/cofemine_launcher/releases` |
| `-Dhmcl.authlibinjector.location=<path>` | Use a local authlib-injector JAR. | Uses bundled authlib-injector |
| `-Dhmcl.openjfx.repo=<maven repository url>` | Add a custom Maven repository for OpenJFX. |  |
| `-Dhmcl.native.encoding=<encoding>` | Override native encoding. | System default |
| `-Dhmcl.microsoft.auth.id=<App ID>` | Override Microsoft OAuth App ID. | Uses bundled ID |
| `-Dhmcl.microsoft.auth.secret=<App Secret>` | Override Microsoft OAuth App Secret. | Uses bundled secret |
| `-Dhmcl.curseforge.apikey=<Api Key>` | Override CurseForge API key. | Uses bundled key |
| `-Dhmcl.native.backend=<auto/jna/none>` | Select native backend. | `auto` |
| `-Dhmcl.hardware.fastfetch=<true/false>` | Enable fastfetch hardware detection. | `true` |
