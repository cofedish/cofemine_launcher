# Build Guide

<!-- #BEGIN LANGUAGE_SWITCHER -->
**English** | [Chinese](Building_zh.md)
<!-- #END LANGUAGE_SWITCHER -->

## Requirements

To build CofeMine Launcher, you need to install JDK 17 (or higher). You can download it here: [Download Liberica JDK](https://bell-sw.com/pages/downloads/#jdk-25-lts).

After installing the JDK, make sure the `JAVA_HOME` environment variable points to the required JDK directory.
You can check the JDK version that `JAVA_HOME` points to like this:

<details>
<summary>Windows</summary>

PowerShell:
```
PS > & "$env:JAVA_HOME/bin/java.exe" -version
openjdk version "25" 2025-09-16 LTS
OpenJDK Runtime Environment (build 25+37-LTS)
OpenJDK 64-Bit Server VM (build 25+37-LTS, mixed mode, sharing)
```

</details>

<details>
<summary>Linux/macOS/FreeBSD</summary>

```
> $JAVA_HOME/bin/java -version
openjdk version "25" 2025-09-16 LTS
OpenJDK Runtime Environment (build 25+37-LTS)
OpenJDK 64-Bit Server VM (build 25+37-LTS, mixed mode, sharing)
```

</details>

## Get CofeMine Launcher Source Code

- You can get the latest source code via [Git](https://git-scm.com/downloads):
  ```shell
  git clone https://github.com/cofedish/cofemine_launcher.git
  cd cofemine_launcher
  ```
- You can manually download a specific version of the source code from the [GitHub Release page](https://github.com/cofedish/cofemine_launcher/releases).

## Build CofeMine Launcher

To build CofeMine Launcher, switch to the root directory of the project and run the following command:

```shell
./gradlew clean makeExecutables
```

The built CofeMine Launcher program files are located in the `HMCL/build/libs` module directory under the project root.
