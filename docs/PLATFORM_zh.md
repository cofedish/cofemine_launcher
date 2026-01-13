# Platform Support Status

<!-- #BEGIN LANGUAGE_SWITCHER -->
[English](PLATFORM.md) | **Chinese**
<!-- #END LANGUAGE_SWITCHER -->

## Launcher Compatibility

<!-- #BEGIN BLOCK -->
<!-- #PROPERTY NAME=LAUNCHER_COMPATIBILITY -->
<table>
  <thead>
    <tr>
      <th></th>
      <th>Windows</th>
      <th>Linux</th>
      <th>macOS</th>
      <th>FreeBSD</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>x86-64</td>
      <td>
        Full (Windows 7 ~ Windows 11)
        <br>
        Full (Windows Server 2008 R2 ~ 2025)
        <br>
        Legacy (Windows Vista)
        <br>
        Legacy (Windows Server 2003 ~ 2008)
      </td>
      <td>Full</td>
      <td>Full</td>
      <td>Full</td>
    </tr>
    <tr>
      <td>x86</td>
      <td>
        Limited (Windows 7 ~ Windows 10)
        <br>
        Legacy (Windows XP/Vista)
      </td>
      <td>Limited</td>
      <td>Not supported</td>
      <td>Not supported</td>
    </tr>
    <tr>
      <td>ARM64</td>
      <td>Full</td>
      <td>Full</td>
      <td>Full</td>
      <td>Not supported</td>
    </tr>
    <tr>
      <td>ARM32</td>
      <td>Not supported</td>
      <td>Limited</td>
      <td>Not supported</td>
      <td>Not supported</td>
    </tr>
    <tr>
      <td>MIPS64el</td>
      <td>Not supported</td>
      <td>Limited</td>
      <td>Not supported</td>
      <td>Not supported</td>
    </tr>
    <tr>
      <td>RISC-V 64</td>
      <td>Not supported</td>
      <td>Full</td>
      <td>Not supported</td>
      <td>Not supported</td>
    </tr>
    <tr>
      <td>LoongArch64</td>
      <td>Not supported</td>
      <td>
        Full (New World)
        <br>
        Limited (Old World)
      </td>
      <td>Not supported</td>
      <td>Not supported</td>
    </tr>
  </tbody>
</table>
<!-- #END BLOCK -->

Legend:

- Full: fully supported by the CofeMine Launcher team.
- Limited: legacy platforms; some features may be unavailable.
- Legacy: older platforms with no active maintenance.
- Not supported: not available at this time.

## Game Compatibility

<!-- #BEGIN BLOCK -->
<!-- #PROPERTY NAME=GAME_COMPATIBILITY -->
|                             | Windows                                         | Linux                                      | macOS                                                                 | FreeBSD                           |
|-----------------------------|:------------------------------------------------|:-------------------------------------------|:----------------------------------------------------------------------|:----------------------------------|
| x86-64                      | Official                                        | Official                                   | Official                                                              | Community (Minecraft 1.13~1.21.11) |
| x86                         | Official (~1.20.4)                              | Official (~1.20.4)                         | N/A                                                                   | N/A                               |
| ARM64                       | Community (Minecraft 1.8~1.18.2)<br/>Official (Minecraft 1.19+) | Community (Minecraft 1.8~1.21.11) | Community (Minecraft 1.6~1.18.2)<br/>Official (Minecraft 1.19+)<br/>Official (Rosetta 2) | Low                               |
| ARM32                       | N/A                                             | Community (Minecraft 1.8~1.20.1)           | N/A                                                                   | N/A                               |
| MIPS64el                    | N/A                                             | Community (Minecraft 1.8~1.20.1)           | N/A                                                                   | N/A                               |
| RISC-V 64                   | N/A                                             | Community (Minecraft 1.13~1.21.5)          | N/A                                                                   | N/A                               |
| LoongArch64 (New World)     | N/A                                             | Community (Minecraft 1.6~1.21.11)          | N/A                                                                   | N/A                               |
| LoongArch64 (Old World)     | N/A                                             | Community (Minecraft 1.6~1.20.1)           | N/A                                                                   | N/A                               |
| PowerPC-64 (Little-Endian)  | N/A                                             | Low                                       | N/A                                                                   | N/A                               |
| S390x                       | N/A                                             | Low                                       | N/A                                                                   | N/A                               |
<!-- #END BLOCK -->

Legend:

- Official: supported by Mojang. Report game issues to Mojang.
- Community: supported by the CofeMine Launcher team; may have more issues.
- Low: requires manual native libraries and additional setup.
- N/A: not available.

## Terracotta Compatibility

<!-- #BEGIN BLOCK -->
<!-- #PROPERTY NAME=TERRACOTTA_COMPATIBILITY -->
<table>
  <thead>
    <tr>
      <th></th>
      <th>Windows</th>
      <th>Linux</th>
      <th>macOS</th>
      <th>FreeBSD</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>x86-64</td>
      <td>
        Full (Windows 10 ~ Windows 11)
        <br>
        Full (Windows Server 2016 ~ 2025)
      </td>
      <td>Full</td>
      <td>Full</td>
      <td>Full</td>
    </tr>
    <tr>
      <td>x86</td>
      <td>Not supported</td>
      <td>Not supported</td>
      <td>Not supported</td>
      <td>Not supported</td>
    </tr>
    <tr>
      <td>ARM64</td>
      <td>Full</td>
      <td>Full</td>
      <td>Full</td>
      <td>Not supported</td>
    </tr>
    <tr>
      <td>ARM32</td>
      <td>Not supported</td>
      <td>Not supported</td>
      <td>Not supported</td>
      <td>Not supported</td>
    </tr>
    <tr>
      <td>MIPS64el</td>
      <td>Not supported</td>
      <td>Not supported</td>
      <td>Not supported</td>
      <td>Not supported</td>
    </tr>
    <tr>
      <td>RISC-V 64</td>
      <td>Not supported</td>
      <td>Low</td>
      <td>Not supported</td>
      <td>Not supported</td>
    </tr>
    <tr>
      <td>LoongArch64</td>
      <td>Not supported</td>
      <td>
        Full (New World)
        <br>
        Low (Old World)
      </td>
      <td>Not supported</td>
      <td>Not supported</td>
    </tr>
  </tbody>
</table>
<!-- #END BLOCK -->
