import org.jackhuang.hmcl.gradle.TerracottaConfigUpgradeTask
import org.jackhuang.hmcl.gradle.ci.GitHubActionUtils
import org.jackhuang.hmcl.gradle.ci.JenkinsUtils
import org.jackhuang.hmcl.gradle.l10n.CheckTranslations
import org.jackhuang.hmcl.gradle.l10n.CreateLanguageList
import org.jackhuang.hmcl.gradle.l10n.CreateLocaleNamesResourceBundle
import org.jackhuang.hmcl.gradle.l10n.UpsideDownTranslate
import org.jackhuang.hmcl.gradle.mod.ParseModDataTask
import org.jackhuang.hmcl.gradle.utils.PropertiesUtils
import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.Files
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.util.zip.ZipFile

plugins {
    alias(libs.plugins.shadow)
}

val projectConfig = PropertiesUtils.load(rootProject.file("config/project.properties").toPath())

val gitRefType = System.getenv("GITHUB_REF_TYPE")
val gitRefName = System.getenv("GITHUB_REF_NAME")
val releaseTag = when {
    gitRefType == "tag" && !gitRefName.isNullOrBlank() -> gitRefName
    else -> System.getenv("GITHUB_REF")?.takeIf { it.startsWith("refs/tags/") }?.removePrefix("refs/tags/")
}
val isReleaseTag = releaseTag != null && Regex("^v\\d+\\.\\d+\\.\\d+$").matches(releaseTag)

val isOfficial = JenkinsUtils.IS_ON_CI || GitHubActionUtils.IS_ON_OFFICIAL_REPO || isReleaseTag

val versionType = System.getenv("VERSION_TYPE")
    ?: if (isReleaseTag) "stable" else if (isOfficial) "nightly" else "unofficial"

base {
    archivesName.set("CofeMine-Launcher")
}
val executableBaseName = "CofeMine-Launcher"
val versionRoot = System.getenv("VERSION_ROOT") ?: projectConfig.getProperty("versionRoot") ?: "3"

val microsoftAuthId = System.getenv("MICROSOFT_AUTH_ID") ?: ""
val microsoftAuthSecret = System.getenv("MICROSOFT_AUTH_SECRET") ?: ""
val curseForgeApiKey = System.getenv("CURSEFORGE_API_KEY") ?: ""

// Surface the embedded-integration state up front so it shows up in the CI
// log. `createPropertiesFile` is cached, so any missing value here is the
// reason CurseForge search later 403s at runtime.
logger.lifecycle(
    "CofeMine secrets: CURSEFORGE_API_KEY={} MICROSOFT_AUTH_ID={} MICROSOFT_AUTH_SECRET={}",
    if (curseForgeApiKey.isEmpty()) "<empty>" else "<${curseForgeApiKey.length} chars>",
    if (microsoftAuthId.isEmpty()) "<empty>" else "<set>",
    if (microsoftAuthSecret.isEmpty()) "<empty>" else "<set>"
)

val launcherExe = System.getenv("HMCL_LAUNCHER_EXE")
val launcherExeFile = launcherExe?.takeIf { it.isNotBlank() }?.let { rootProject.file(it) }
if (launcherExeFile != null && !launcherExeFile.exists()) {
    throw GradleException("HMCL_LAUNCHER_EXE points to missing file: ${launcherExeFile.absolutePath}")
}

if (isReleaseTag) {
    version = releaseTag!!.removePrefix("v")
} else {
    val buildNumber = System.getenv("BUILD_NUMBER")?.toInt()
    if (buildNumber != null) {
        version = if (JenkinsUtils.IS_ON_CI && versionType == "dev") {
            "$versionRoot.0.$buildNumber"
        } else {
            "$versionRoot.$buildNumber"
        }
    } else {
        val shortCommit = System.getenv("GITHUB_SHA")?.lowercase()?.substring(0, 7)
        version = if (shortCommit.isNullOrBlank()) {
            "$versionRoot.SNAPSHOT"
        } else if (isOfficial) {
            "$versionRoot.dev-$shortCommit"
        } else {
            "$versionRoot.unofficial-$shortCommit"
        }
    }
}

val embedResources by configurations.registering

dependencies {
    implementation(project(":HMCLCore"))
    implementation(project(":HMCLBoot"))
    implementation("libs:JFoenix")
    implementation(libs.twelvemonkeys.imageio.webp)
    implementation(libs.java.info)
    implementation(libs.monet.fx)
    implementation(libs.junrar)
    implementation("net.sf.sevenzipjbinding:sevenzipjbinding:16.02-2.01")
    implementation("net.sf.sevenzipjbinding:sevenzipjbinding-all-platforms:16.02-2.01")

    if (launcherExeFile == null) {
        implementation(libs.hmclauncher)
    }

    embedResources(libs.authlib.injector)
}

fun digest(algorithm: String, bytes: ByteArray): ByteArray = MessageDigest.getInstance(algorithm).digest(bytes)

fun createChecksum(file: File) {
    val algorithms = linkedMapOf(
        "SHA-1" to "sha1",
        "SHA-256" to "sha256",
        "SHA-512" to "sha512"
    )

    algorithms.forEach { (algorithm, ext) ->
        File(file.parentFile, "${file.name}.$ext").writeText(
            digest(algorithm, file.readBytes()).joinToString(separator = "", postfix = "\n") { "%02x".format(it) }
        )
    }
}

fun attachSignature(jar: File) {
    val keyLocation = System.getenv("HMCL_SIGNATURE_KEY")
    if (keyLocation == null) {
        logger.warn("Missing signature key")
        return
    }

    val privatekey = KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(File(keyLocation).readBytes()))
    val signer = Signature.getInstance("SHA512withRSA")
    signer.initSign(privatekey)
    ZipFile(jar).use { zip ->
        zip.stream()
            .sorted(Comparator.comparing { it.name })
            .filter { it.name != "META-INF/hmcl_signature" }
            .forEach {
                signer.update(digest("SHA-512", it.name.toByteArray()))
                signer.update(digest("SHA-512", zip.getInputStream(it).readBytes()))
            }
    }
    val signature = signer.sign()
    FileSystems.newFileSystem(URI.create("jar:" + jar.toURI()), emptyMap<String, Any>()).use { zipfs ->
        Files.newOutputStream(zipfs.getPath("META-INF/hmcl_signature")).use { it.write(signature) }
    }
}

tasks.withType<JavaCompile> {
    sourceCompatibility = "17"
    targetCompatibility = "17"
}

tasks.checkstyleMain {
    // Third-party code is not checked
    exclude("**/org/jackhuang/hmcl/ui/image/apng/**")
}

val addOpens = listOf(
    "java.base/java.lang",
    "java.base/java.lang.reflect",
    "java.base/jdk.internal.loader",
    "javafx.base/com.sun.javafx.binding",
    "javafx.base/com.sun.javafx.event",
    "javafx.base/com.sun.javafx.runtime",
    "javafx.base/javafx.beans.property",
    "javafx.graphics/javafx.css",
    "javafx.graphics/javafx.stage",
    "javafx.graphics/com.sun.glass.ui",
    "javafx.graphics/com.sun.javafx.stage",
    "javafx.graphics/com.sun.javafx.util",
    "javafx.graphics/com.sun.prism",
    "javafx.controls/com.sun.javafx.scene.control",
    "javafx.controls/com.sun.javafx.scene.control.behavior",
    "javafx.graphics/com.sun.javafx.tk.quantum",
    "javafx.controls/javafx.scene.control.skin",
    "jdk.attach/sun.tools.attach",
)

tasks.compileJava {
    options.compilerArgs.addAll(addOpens.map { "--add-exports=$it=ALL-UNNAMED" })
}

val hmclProperties = buildList {
    add("hmcl.version" to project.version.toString())
    add("hmcl.add-opens" to addOpens.joinToString(" "))
    System.getenv("GITHUB_SHA")?.let {
        add("hmcl.version.hash" to it)
    }
    add("hmcl.version.type" to versionType)
    add("hmcl.microsoft.auth.id" to microsoftAuthId)
    add("hmcl.microsoft.auth.secret" to microsoftAuthSecret)
    add("hmcl.curseforge.apikey" to curseForgeApiKey)
    add("hmcl.authlib-injector.version" to libs.authlib.injector.get().version!!)
}

val hmclPropertiesFile = layout.buildDirectory.file("hmcl.properties")
val createPropertiesFile by tasks.registering {
    outputs.file(hmclPropertiesFile)
    hmclProperties.forEach { (k, v) -> inputs.property(k, v) }

    doLast {
        val targetFile = hmclPropertiesFile.get().asFile
        targetFile.parentFile.mkdir()
        targetFile.bufferedWriter().use {
            for ((k, v) in hmclProperties) {
                it.write("$k=$v\n")
            }
        }
    }
}

tasks.jar {
    enabled = false
    dependsOn(tasks["shadowJar"])
}

val jarPath = tasks.jar.get().archiveFile.get().asFile

tasks.shadowJar {
    dependsOn(createPropertiesFile)

    archiveClassifier.set(null as String?)

    exclude("**/package-info.class")
    exclude("META-INF/maven/**")

    exclude("META-INF/services/javax.imageio.spi.ImageReaderSpi")
    exclude("META-INF/services/javax.imageio.spi.ImageInputStreamSpi")

    listOf(
        "aix-*", "sunos-*", "openbsd-*", "dragonflybsd-*", "freebsd-*", "linux-*", "darwin-*",
        "*-ppc", "*-ppc64le", "*-s390x", "*-armel",
    ).forEach { exclude("com/sun/jna/$it/**") }

    minimize {
        exclude(dependency("com.google.code.gson:.*:.*"))
        exclude(dependency("net.java.dev.jna:jna:.*"))
        exclude(dependency("libs:JFoenix:.*"))
        exclude(dependency("net.sf.sevenzipjbinding:sevenzipjbinding:.*"))
        exclude(dependency("net.sf.sevenzipjbinding:sevenzipjbinding-all-platforms:.*"))
        exclude(project(":HMCLBoot"))
    }

    manifest.attributes(
        "Created-By" to "Copyright(c) 2013-2025 huangyuhui.",
        "Implementation-Version" to project.version.toString(),
        "Main-Class" to "org.jackhuang.hmcl.Main",
        "Multi-Release" to "true",
        "Add-Opens" to addOpens.joinToString(" "),
        "Enable-Native-Access" to "ALL-UNNAMED",
        "Enable-Final-Field-Mutation" to "ALL-UNNAMED",
    )

    if (launcherExeFile != null) {
        into("assets") {
            from(launcherExeFile)
        }
    }

    doLast {
        attachSignature(jarPath)
        createChecksum(jarPath)
    }
}

tasks.processResources {
    dependsOn(createPropertiesFile)
    dependsOn(upsideDownTranslate)
    dependsOn(createLocaleNamesResourceBundle)
    dependsOn(createLanguageList)

    into("assets/") {
        from(hmclPropertiesFile)
        from(embedResources)
    }

    into("assets/lang") {
        from(createLanguageList.map { it.outputFile })
        from(upsideDownTranslate.map { it.outputFile })
        from(createLocaleNamesResourceBundle.map { it.outputDirectory })
    }

    inputs.property("terracotta_version", libs.versions.terracotta)
    doLast {
        upgradeTerracottaConfig.get().checkValid()
    }
}

val makeExecutables by tasks.registering {
    val extensions = listOf("exe", "sh")

    dependsOn(tasks.jar)

    inputs.file(jarPath)
    outputs.files(extensions.map { File(jarPath.parentFile, "$executableBaseName.$it") })

    doLast {
        val jarContent = jarPath.readBytes()

        ZipFile(jarPath).use { zipFile ->
            for (extension in extensions) {
                val output = File(jarPath.parentFile, "$executableBaseName.$extension")
                val entry = zipFile.getEntry("assets/HMCLauncher.$extension")
                    ?: throw GradleException("HMCLauncher.$extension not found")

                output.outputStream().use { outputStream ->
                    zipFile.getInputStream(entry).use { it.copyTo(outputStream) }
                    outputStream.write(jarContent)
                }

                createChecksum(output)
            }
        }
    }
}

tasks.build {
    dependsOn(makeExecutables)
}

// --- Native installers via jpackage ---------------------------------------
//
// jpackage (bundled with JDK >= 14) produces a native installer for the host
// OS with an embedded JRE. Cross-compilation is not supported: run each
// packaging task on its target OS. Tasks are registered unconditionally so
// they appear in `./gradlew tasks`, but `onlyIf` skips them on wrong hosts.
//
// Platform notes:
//   Windows  -> .exe (or .msi with -PinstallerKind=msi); requires WiX v3 on PATH.
//   macOS    -> .dmg; requires Xcode Command Line Tools.
//   Linux    -> .deb (default) or .rpm via `packageInstallerLinuxRpm`.
//
// Icons live in HMCL/image/. At least `hmcl.png` must exist. Platform-specific
// icons (`hmcl.ico`, `hmcl.icns`) are optional — if missing, jpackage falls
// back to its generic icon. CI generates them on the fly (see release.yml).

// Installer identity. `jpackageAppName` is used for the install folder,
// shortcuts and the installer filename — kept space-free so the default
// install path stays at `%LocalAppData%\Programs\CofeMine-Launcher` without
// awkward escaping. `jpackageMenuName` is the human-friendly Start-menu /
// shortcut label shown to end users.
val jpackageAppName = "CofeMine-Launcher"
val jpackageMenuName = "CofeMine Launcher"
val jpackageVendor = "cofedish"
val jpackageCopyright = "Copyright (C) 2022 huangyuhui and contributors; (C) 2026 cofedish and contributors."
val jpackageDescription = "CofeMine Launcher - Minecraft launcher based on HMCL"

// jpackage accepts only digits/dots, 1-3 components. Release tags like
// v1.2.3 satisfy this; dev builds (e.g. "3.dev-abc1234") don't, so we
// sanitize to a safe fallback.
val jpackageAppVersion: String = run {
    val raw = project.version.toString()
    if (raw.matches(Regex("^\\d+(\\.\\d+){0,2}$"))) raw else "1.0.0"
}

val jpackageOutputDir = layout.buildDirectory.dir("installer")

val iconDir = rootProject.file("HMCL/image")
val iconPng = File(iconDir, "hmcl.png")
val iconIco = File(iconDir, "hmcl.ico")
val iconIcns = File(iconDir, "hmcl.icns")

fun Exec.configureJpackage(
    type: String,
    iconFile: File?,
    extraArgs: List<String> = emptyList()
) {
    dependsOn(tasks.shadowJar)
    group = "distribution"

    val jarFile = tasks.shadowJar.get().archiveFile.get().asFile
    val inputDir = jarFile.parentFile
    val destDir = jpackageOutputDir.get().asFile

    inputs.file(jarFile)
    outputs.dir(destDir)

    doFirst {
        destDir.mkdirs()
        // jpackage fails if dest already contains an installer with the same
        // filename. Delete only files matching this task's output type so that
        // a sibling task's artifacts (e.g. .deb when building .rpm) survive.
        val typeExtensions = when (type) {
            "deb" -> listOf(".deb")
            "rpm" -> listOf(".rpm")
            "dmg" -> listOf(".dmg")
            "pkg" -> listOf(".pkg")
            "msi" -> listOf(".msi")
            "exe" -> listOf(".exe")
            else -> listOf(".$type")
        }
        destDir.listFiles()?.forEach { f ->
            if (f.isFile && typeExtensions.any { ext ->
                    f.name.endsWith(ext) ||
                    f.name.endsWith("$ext.sha1") ||
                    f.name.endsWith("$ext.sha256") ||
                    f.name.endsWith("$ext.sha512")
                }) {
                f.delete()
            }
        }

        val javaHome = System.getProperty("java.home")
        val jpackageExe = File(javaHome).resolve("bin").resolve(
            if (System.getProperty("os.name").lowercase().startsWith("windows")) "jpackage.exe" else "jpackage"
        )
        if (!jpackageExe.exists()) {
            throw GradleException("jpackage not found at $jpackageExe. Requires JDK >= 14.")
        }

        val args = mutableListOf(
            jpackageExe.absolutePath,
            "--type", type,
            "--input", inputDir.absolutePath,
            "--main-jar", jarFile.name,
            "--name", jpackageAppName,
            "--app-version", jpackageAppVersion,
            "--vendor", jpackageVendor,
            "--copyright", jpackageCopyright,
            "--description", jpackageDescription,
            "--dest", destDir.absolutePath
        )
        iconFile?.takeIf { it.exists() }?.let { args += listOf("--icon", it.absolutePath) }
        addOpens.forEach { args += listOf("--java-options", "--add-opens=$it=ALL-UNNAMED") }
        args += listOf("--java-options", "--enable-native-access=ALL-UNNAMED")
        args += extraArgs

        commandLine(args)
        logger.lifecycle("jpackage: {}", args.joinToString(" "))
    }

    doLast {
        // Strip the version suffix from jpackage's output file so release
        // assets keep stable, version-less names across releases.
        // jpackage produces names like:
        //   CofeMine-Launcher-1.2.9.exe        (Windows .exe/.msi/.dmg/.pkg)
        //   cofemine-launcher_1.2.9-1_amd64.deb (Linux .deb)
        //   cofemine-launcher-1.2.9-1.x86_64.rpm (Linux .rpm)
        val renamed = mutableListOf<File>()
        destDir.listFiles()?.forEach { f ->
            if (!f.isFile) return@forEach
            val ext = when {
                f.name.endsWith(".exe") -> ".exe"
                f.name.endsWith(".msi") -> ".msi"
                f.name.endsWith(".dmg") -> ".dmg"
                f.name.endsWith(".pkg") -> ".pkg"
                f.name.endsWith(".deb") -> ".deb"
                f.name.endsWith(".rpm") -> ".rpm"
                else -> return@forEach
            }
            val versionless = when (ext) {
                // Windows .exe from jpackage is an installer, but the
                // existing `makeExecutables` task also produces a
                // `CofeMine-Launcher.exe` portable wrapper — use a `-Setup`
                // suffix to disambiguate and to match the usual Windows
                // convention for installer binaries.
                ".exe" -> "$jpackageAppName-Setup$ext"
                ".msi" -> "$jpackageAppName-Setup$ext"
                ".deb" -> "cofemine-launcher$ext"
                ".rpm" -> "cofemine-launcher$ext"
                else -> "$jpackageAppName$ext"
            }
            if (f.name != versionless) {
                val target = File(destDir, versionless)
                if (target.exists()) target.delete()
                if (f.renameTo(target)) {
                    renamed += target
                    return@forEach
                }
            }
            renamed += f
        }
        renamed.forEach { createChecksum(it) }
    }
}

val packageInstallerWindows by tasks.registering(Exec::class) {
    description = "Builds a Windows .exe installer via jpackage (Windows host only)."
    val installerKind = (project.findProperty("installerKind") as? String) ?: "exe"
    configureJpackage(
        type = installerKind, // "exe" or "msi"
        iconFile = iconIco.takeIf { it.exists() } ?: iconPng,
        extraArgs = listOf(
            // Per-user install ends up under %LocalAppData%\Programs\<name>,
            // so non-admin users can install without UAC and the launcher's
            // working directory is writable.
            "--win-per-user-install",
            "--win-menu",
            "--win-menu-group", jpackageMenuName,
            "--win-shortcut",
            "--win-shortcut-prompt",
            "--win-dir-chooser",
            "--win-help-url", "https://github.com/cofedish/cofemine_launcher",
            "--win-update-url", "https://github.com/cofedish/cofemine_launcher/releases/latest",
            // Stable UpgradeCode keeps this installer compatible with future
            // versions so they replace instead of stacking. Must be a valid
            // RFC 4122 UUID (hex digits + dashes only).
            "--win-upgrade-uuid", "6f0d2b1a-c4a1-4b5d-9a8f-a11ce0fe0001"
        )
    )
    onlyIf { System.getProperty("os.name").lowercase().startsWith("windows") }
}

// --- Custom-branded Windows installer via Inno Setup ----------------------
//
// Pipeline: jpackage --type app-image produces a self-contained app directory
// (launcher exe + bundled JRE + our jar), which is then wrapped by Inno Setup
// into a branded .exe installer. We keep the jpackage msi/.exe task above as
// a fallback for environments without Inno Setup.

val appImageDir = layout.buildDirectory.dir("app-image")
val innoAssetsDir = layout.buildDirectory.dir("inno-assets")
val innoOutputDir = layout.buildDirectory.dir("installer")

val packageWindowsAppImage by tasks.registering(Exec::class) {
    description = "Produces a jpackage app-image (directory) for Inno Setup to wrap."
    group = "distribution"
    dependsOn(tasks.shadowJar)

    val jarFile = tasks.shadowJar.get().archiveFile.get().asFile
    val inputDir = jarFile.parentFile
    val outDir = appImageDir.get().asFile

    inputs.file(jarFile)
    outputs.dir(outDir)

    doFirst {
        // jpackage --type app-image fails if the target directory already
        // contains an app-image with the same name. Wipe it each run.
        if (outDir.exists()) outDir.deleteRecursively()
        outDir.mkdirs()

        val javaHome = System.getProperty("java.home")
        val jpackageExe = File(javaHome).resolve("bin").resolve("jpackage.exe")
        if (!jpackageExe.exists()) {
            throw GradleException("jpackage not found at $jpackageExe. Requires JDK >= 14.")
        }

        val args = mutableListOf(
            jpackageExe.absolutePath,
            "--type", "app-image",
            "--input", inputDir.absolutePath,
            "--main-jar", jarFile.name,
            "--name", jpackageAppName,
            "--app-version", jpackageAppVersion,
            "--vendor", jpackageVendor,
            "--copyright", jpackageCopyright,
            "--description", jpackageDescription,
            "--dest", outDir.absolutePath
        )
        val icon = if (iconIco.exists()) iconIco else iconPng
        if (icon.exists()) args += listOf("--icon", icon.absolutePath)
        addOpens.forEach { args += listOf("--java-options", "--add-opens=$it=ALL-UNNAMED") }
        args += listOf("--java-options", "--enable-native-access=ALL-UNNAMED")

        commandLine(args)
        logger.lifecycle("jpackage app-image: {}", args.joinToString(" "))
    }
    onlyIf { System.getProperty("os.name").lowercase().startsWith("windows") }
}

val packageWindowsInnoSetup by tasks.registering(Exec::class) {
    description = "Wraps the Windows app-image in a branded Inno Setup installer."
    group = "distribution"
    dependsOn(packageWindowsAppImage)

    val issScript = rootProject.file("HMCL/packaging/windows/cofemine-launcher.iss")
    val outDir = innoOutputDir.get().asFile
    val appVersion = jpackageAppVersion

    inputs.file(issScript)
    inputs.dir(appImageDir)
    inputs.property("version", appVersion)
    outputs.file(File(outDir, "${jpackageAppName}-Setup.exe"))

    doFirst {
        outDir.mkdirs()
        // Locate iscc.exe: GitHub runners have it preinstalled at the path
        // below; local dev machines usually have it on PATH after install.
        val localAppData = System.getenv("LOCALAPPDATA")
        val candidates = listOfNotNull(
            System.getenv("INNO_SETUP")?.let { File(it) },
            File("C:/Program Files (x86)/Inno Setup 6/ISCC.exe"),
            File("C:/Program Files/Inno Setup 6/ISCC.exe"),
            localAppData?.let { File("$it/Programs/Inno Setup 6/ISCC.exe") }
        )
        val iscc = candidates.firstOrNull { it.exists() }
            ?: throw GradleException(
                "Inno Setup compiler (iscc.exe) not found. Install Inno Setup 6 " +
                "from https://jrsoftware.org/isinfo.php or set INNO_SETUP env var."
            )

        val assetsDir = innoAssetsDir.get().asFile
        val bannerWelcome = File(assetsDir, "welcome.bmp")
        val bannerHeader = File(assetsDir, "header.bmp")

        val args = mutableListOf(
            iscc.absolutePath,
            "/DAppVersion=$appVersion",
            "/DAppName=${jpackageAppName}",
            "/DAppMenuName=${jpackageMenuName}",
            "/DAppPublisher=${jpackageVendor}",
            "/DOutputDir=${outDir.absolutePath.replace('\\', '/')}",
            "/DOutputBase=${jpackageAppName}-Setup",
            "/DAppImageDir=${appImageDir.get().asFile.resolve(jpackageAppName).absolutePath.replace('\\', '/')}",
            "/DAppIcon=${iconIco.absolutePath.replace('\\', '/')}",
            "/DLicenseFile=${rootProject.file("LICENSE").absolutePath.replace('\\', '/')}"
        )
        if (bannerWelcome.exists())
            args += "/DWelcomeBanner=${bannerWelcome.absolutePath.replace('\\', '/')}"
        if (bannerHeader.exists())
            args += "/DHeaderBanner=${bannerHeader.absolutePath.replace('\\', '/')}"

        args += issScript.absolutePath

        commandLine(args)
        logger.lifecycle("iscc: {}", args.joinToString(" "))
    }

    doLast {
        // The .iss sets OutputBaseFilename to ${jpackageAppName}-Setup, so the
        // resulting file is already versionless. Just add checksums.
        val installer = File(outDir, "${jpackageAppName}-Setup.exe")
        if (installer.exists()) createChecksum(installer)
    }

    onlyIf { System.getProperty("os.name").lowercase().startsWith("windows") }
}

val packageInstallerMac by tasks.registering(Exec::class) {
    description = "Builds a macOS .dmg installer via jpackage (macOS host only)."
    configureJpackage(
        type = "dmg",
        iconFile = iconIcns.takeIf { it.exists() },
        extraArgs = listOf(
            "--mac-package-name", "CofeMine"
        )
    )
    onlyIf { System.getProperty("os.name").lowercase().startsWith("mac") }
}

val packageInstallerLinuxDeb by tasks.registering(Exec::class) {
    description = "Builds a Linux .deb installer via jpackage (Linux host only)."
    configureJpackage(
        type = "deb",
        iconFile = iconPng,
        extraArgs = listOf(
            "--linux-shortcut",
            "--linux-menu-group", "Game",
            "--linux-deb-maintainer", "cofedish@users.noreply.github.com",
            "--linux-package-name", "cofemine-launcher"
        )
    )
    onlyIf { System.getProperty("os.name").lowercase().startsWith("linux") }
}

val packageInstallerLinuxRpm by tasks.registering(Exec::class) {
    description = "Builds a Linux .rpm installer via jpackage (Linux host only)."
    configureJpackage(
        type = "rpm",
        iconFile = iconPng,
        extraArgs = listOf(
            "--linux-shortcut",
            "--linux-menu-group", "Game",
            "--linux-package-name", "cofemine-launcher"
        )
    )
    onlyIf { System.getProperty("os.name").lowercase().startsWith("linux") }
}

// Convenience aggregate: builds whatever installer(s) fit the current host.
// On Windows we prefer the branded Inno Setup installer; if Inno Setup
// isn't available the MSI/EXE jpackage fallback (packageInstallerWindows)
// can be invoked directly.
tasks.register("packageInstaller") {
    group = "distribution"
    description = "Builds native installer(s) for the current host OS."
    val osName = System.getProperty("os.name").lowercase()
    when {
        osName.startsWith("windows") -> dependsOn(packageWindowsInnoSetup)
        osName.startsWith("mac") -> dependsOn(packageInstallerMac)
        osName.startsWith("linux") -> dependsOn(packageInstallerLinuxDeb, packageInstallerLinuxRpm)
    }
}

fun parseToolOptions(options: String?): MutableList<String> {
    if (options == null)
        return mutableListOf()

    val builder = StringBuilder()
    val result = mutableListOf<String>()

    var offset = 0

    loop@ while (offset < options.length) {
        val ch = options[offset]
        if (Character.isWhitespace(ch)) {
            if (builder.isNotEmpty()) {
                result += builder.toString()
                builder.clear()
            }

            while (offset < options.length && Character.isWhitespace(options[offset])) {
                offset++
            }

            continue@loop
        }

        if (ch == '\'' || ch == '"') {
            offset++

            while (offset < options.length) {
                val ch2 = options[offset++]
                if (ch2 != ch) {
                    builder.append(ch2)
                } else {
                    continue@loop
                }
            }

            throw GradleException("Unmatched quote in $options")
        }

        builder.append(ch)
        offset++
    }

    if (builder.isNotEmpty()) {
        result += builder.toString()
    }

    return result
}

// For IntelliJ IDEA
tasks.withType<JavaExec> {
    if (name != "run") {
        jvmArgs(addOpens.map { "--add-opens=$it=ALL-UNNAMED" })
//        if (javaVersion >= JavaVersion.VERSION_24) {
//            jvmArgs("--enable-native-access=ALL-UNNAMED")
//        }
    }
}

tasks.register<JavaExec>("run") {
    dependsOn(tasks.jar)

    group = "application"

    classpath = files(jarPath)
    workingDir = rootProject.rootDir

    val vmOptions = parseToolOptions(System.getenv("HMCL_JAVA_OPTS") ?: "-Xmx1g")
    if (vmOptions.none { it.startsWith("-Dhmcl.offline.auth.restricted=") })
        vmOptions += "-Dhmcl.offline.auth.restricted=false"

    jvmArgs(vmOptions)

    val hmclJavaHome = System.getenv("HMCL_JAVA_HOME")
    if (hmclJavaHome != null) {
        this.executable(
            file(hmclJavaHome).resolve("bin")
                .resolve(if (System.getProperty("os.name").lowercase().startsWith("windows")) "java.exe" else "java")
        )
    }

    doFirst {
        logger.quiet("HMCL_JAVA_OPTS: {}", vmOptions)
        logger.quiet("HMCL_JAVA_HOME: {}", hmclJavaHome ?: System.getProperty("java.home"))
    }
}

// terracotta

val upgradeTerracottaConfig = tasks.register<TerracottaConfigUpgradeTask>("upgradeTerracottaConfig") {
    val destination = layout.projectDirectory.file("src/main/resources/assets/terracotta.json")
    val source = layout.projectDirectory.file("terracotta-template.json");

    classifiers.set(listOf(
        "windows-x86_64", "windows-arm64",
        "macos-x86_64", "macos-arm64",
        "linux-x86_64", "linux-arm64", "linux-loongarch64", "linux-riscv64",
        "freebsd-x86_64"
    ))

    version.set(libs.versions.terracotta)
    downloadURL.set($$"https://github.com/burningtnt/Terracotta/releases/download/v${version}/terracotta-${version}-${classifier}-pkg.tar.gz")

    templateFile.set(source)
    outputFile.set(destination)
}

// Check Translations

tasks.register<CheckTranslations>("checkTranslations") {
    val dir = layout.projectDirectory.dir("src/main/resources/assets/lang")

    englishFile.set(dir.file("I18N.properties"))
    simplifiedChineseFile.set(dir.file("I18N_zh_CN.properties"))
    traditionalChineseFile.set(dir.file("I18N_zh.properties"))
    classicalChineseFile.set(dir.file("I18N_lzh.properties"))
}

// l10n

val generatedDir = layout.buildDirectory.dir("generated")

val upsideDownTranslate by tasks.registering(UpsideDownTranslate::class) {
    inputFile.set(layout.projectDirectory.file("src/main/resources/assets/lang/I18N.properties"))
    outputFile.set(generatedDir.map { it.file("generated/i18n/I18N_en_Qabs.properties") })
}

val createLanguageList by tasks.registering(CreateLanguageList::class) {
    resourceBundleDir.set(layout.projectDirectory.dir("src/main/resources/assets/lang"))
    resourceBundleBaseName.set("I18N")
    additionalLanguages.set(listOf("en-Qabs"))
    outputFile.set(generatedDir.map { it.file("languages.json") })
}

val createLocaleNamesResourceBundle by tasks.registering(CreateLocaleNamesResourceBundle::class) {
    dependsOn(createLanguageList)

    languagesFile.set(createLanguageList.flatMap { it.outputFile })
    outputDirectory.set(generatedDir.map { it.dir("generated/LocaleNames") })
}

// mcmod data

tasks.register<ParseModDataTask>("parseModData") {
    inputFile.set(layout.projectDirectory.file("mod.json"))
    outputFile.set(layout.projectDirectory.file("src/main/resources/assets/mod_data.txt"))
}

tasks.register<ParseModDataTask>("parseModPackData") {
    inputFile.set(layout.projectDirectory.file("modpack.json"))
    outputFile.set(layout.projectDirectory.file("src/main/resources/assets/modpack_data.txt"))
}
