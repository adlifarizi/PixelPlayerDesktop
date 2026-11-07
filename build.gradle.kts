import java.net.URI

plugins {
    kotlin("jvm") version "2.4.20"
    kotlin("plugin.serialization") version "2.4.20"
    id("org.jetbrains.compose") version "1.12.1"
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.20"
}

val appVersionName: String = providers.gradleProperty("APP_VERSION_NAME").getOrElse("0.1.0")
val appVersionCode: Int = providers.gradleProperty("APP_VERSION_CODE").map { it.toIntOrNull() ?: 1 }.getOrElse(1)

// Compose Desktop strictly enforces "MAJOR.MINOR.BUILD" numeric format (max 3 parts)
val windowsVersion: String = run {
    val base = appVersionName.substringBefore("-").trim()
    val parts = base.split(".").mapNotNull { it.toIntOrNull() }
    val major = parts.getOrElse(0) { 1 }
    val minor = parts.getOrElse(1) { 0 }
    val patch = parts.getOrElse(2) { 0 }
    "$major.$minor.$patch"
}

// Debian: Pre-releases use tilde notation (e.g. 0.1.0~alpha.1) per Debian Policy Manual §5.6.12
val debianPackageVersion: String = appVersionName.replace("-", "~")

// RPM: Base version is strictly numeric; pre-release tags are placed in the Release field
val rpmVersion: String = windowsVersion
val rpmRelease: String = run {
    if (appVersionName.contains("-")) {
        val suffix = appVersionName.substringAfter("-").replace("-", ".")
        "0.$appVersionCode.$suffix"
    } else {
        "1"
    }
}

group = "com.kayoi.pixelplayerdesktop"
version = appVersionName

repositories {
    google()
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.components.resources)
    implementation("androidx.navigationevent:navigationevent-compose:1.1.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.1")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
    implementation("org.yaml:snakeyaml:2.4")

    // HTTP Client (OkHttp)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // DI (Koin)
    implementation("io.insert-koin:koin-core:4.0.2")
    implementation("io.insert-koin:koin-compose:4.0.2")

    // Database (SQLite JDBC)
    implementation("org.xerial:sqlite-jdbc:3.47.2.0")

    // Audio Tag Reader & libmpv Audio Engine
    implementation("net.jthink:jaudiotagger:3.0.1")
    implementation("org.gagravarr:vorbis-java-core:0.8")
    implementation("net.java.dev.jna:jna:5.16.0")
    implementation("net.java.dev.jna:jna-platform:5.16.0")
    implementation("io.methvin:directory-watcher:0.19.1")

    // Reorderable LazyList
    implementation("sh.calvin.reorderable:reorderable:2.4.3")

    // MaterialKolor (M3 Dynamic Colors & Quantizer)
    implementation("com.materialkolor:material-kolor:2.0.2")

    // Lyrics Romanization
    implementation("com.atilika.kuromoji:kuromoji-ipadic:0.9.0")
    implementation("com.belerweb:pinyin4j:2.5.1")
    implementation("com.ibm.icu:icu4j:76.1")

    // FileKit (Native File & Directory Picker)
    implementation("io.github.vinceglb:filekit-dialogs:0.16.0")

    // Linux D-Bus & MPRIS
    implementation("com.github.hypfvieh:dbus-java-core:5.1.0")
    implementation("com.github.hypfvieh:dbus-java-transport-native-unixsocket:5.1.0")

    // Testing
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.1")
}

configurations.all {
    resolutionStrategy.dependencySubstitution {
        substitute(module("net.java.dev.jna:jna-jpms"))
            .using(module("net.java.dev.jna:jna:5.19.1"))
        substitute(module("net.java.dev.jna:jna-platform-jpms"))
            .using(module("net.java.dev.jna:jna-platform:5.19.1"))
        substitute(module("org.jetbrains.androidx.navigationevent:navigationevent-compose"))
            .using(module("androidx.navigationevent:navigationevent-compose:1.1.1"))
        substitute(module("org.jetbrains.androidx.navigationevent:navigationevent-compose-desktop"))
            .using(module("androidx.navigationevent:navigationevent-compose-desktop:1.1.1"))
    }
}

val generateBuildConfig = tasks.register("generateBuildConfig") {
    val outputDir = layout.buildDirectory.dir("generated/sources/buildConfig/main")
    val vName = appVersionName
    val vCode = appVersionCode

    inputs.property("versionName", vName)
    inputs.property("versionCode", vCode)
    outputs.dir(outputDir)

    doLast {
        val dir = outputDir.get().asFile
        val file = File(dir, "com/kayoi/pixelplayerdesktop/BuildConfig.kt")
        file.parentFile.mkdirs()
        file.writeText(
            """
            package com.kayoi.pixelplayerdesktop

            object BuildConfig {
                const val APP_NAME: String = "PixelPlayerDesktop"
                const val APP_VERSION_NAME: String = "$vName"
                const val APP_VERSION_CODE: Int = $vCode
                const val APPLICATION_ID: String = "com.kayoi.pixelplayerdesktop"
            }
            """.trimIndent() + "\n"
        )
    }
}

kotlin {
    jvmToolchain(21)
    sourceSets.getByName("main").kotlin.srcDir(generateBuildConfig)
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<JavaExec>("runCli") {
    group = "application"
    description = "Runs the CLI debug verification runner"
    mainClass.set("com.kayoi.pixelplayerdesktop.cli.CliRunnerKt")
    classpath = sourceSets["main"].runtimeClasspath
    standardInput = System.`in`
}

compose.desktop {
    application {
        mainClass = "MainKt"

        jvmArgs(
            "-Dfile.encoding=UTF-8",
            "-Dsun.jnu.encoding=UTF-8",
            "-Djna.encoding=UTF-8",
            "-Xms48m",
            "-Xmx384m",
            "-XX:+UseG1GC",
            "-XX:G1HeapRegionSize=4m",
            "-XX:MinHeapFreeRatio=20",
            "-XX:MaxHeapFreeRatio=40",
            "-XX:G1PeriodicGCInterval=3000",
            "-XX:+UseStringDeduplication"
        )

        buildTypes.release.proguard {
            configurationFiles.from(project.file("proguard-rules.pro"))
        }

        nativeDistributions {
            modules(
                "java.sql",
                "java.naming",
                "java.desktop",
                "jdk.unsupported",
                "jdk.security.auth",
                "jdk.crypto.ec",
                "jdk.net",
                "java.instrument"
            )
            appResourcesRootDir.set(project.layout.projectDirectory.dir("src/main/resources-packaged"))

            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Rpm,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Exe,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg
            )

            packageName = "PixelPlayer"
            packageVersion = windowsVersion
            description = "PixelPlayer Desktop - Modern Material 3 Music Player"
            vendor = "PixelPlayer"
            copyright = "Copyright (C) 2026 Theo Vilardo"

            linux {
                packageName = "pixelplayer"
                iconFile.set(project.file("src/main/resources/icons/icon.png"))
                debPackageVersion = debianPackageVersion
                rpmPackageVersion = rpmVersion
                appRelease = rpmRelease
                debMaintainer = "adlifarizi <adlifarizi@users.noreply.github.com>"
                menuGroup = "AudioVideo;Player;Audio;"
                appCategory = "AudioVideo"
                rpmLicenseType = "GPL-3.0"
            }

            windows {
                iconFile.set(project.file("src/main/resources/icons/icon.ico"))
                packageVersion = windowsVersion
                exePackageVersion = windowsVersion
                menuGroup = "PixelPlayer"
                upgradeUuid = "d7bfb9d0-6243-4f9e-a81d-e4b5efc4e723"
            }

            macOS {
                iconFile.set(project.file("src/main/resources/icons/icon.icns"))
                packageVersion = windowsVersion
                dmgPackageVersion = windowsVersion
                bundleID = "com.kayoi.pixelplayerdesktop"
                packageName = "PixelPlayer"
                appCategory = "public.app-category.music"
                minimumSystemVersion = "12.0"
            }
        }
    }
}

tasks.register("setupMpvWindows") {
    group = "mpv"
    description = "Downloads and bundles libmpv for Windows x64"
    val targetDir = layout.projectDirectory.dir("src/main/resources-packaged/windows/mpv").asFile
    val mpvUrl = "https://github.com/shinchiro/mpv-winbuild-cmake/releases/download/20261021/mpv-dev-x86_64-20261021-git-e471496c56.7z"
    val downloadDir = layout.buildDirectory.dir("downloads").get().asFile
    val archiveFile = downloadDir.resolve("mpv-dev-x86_64.7z")

    outputs.dir(targetDir)
    onlyIf {
        !targetDir.resolve("libmpv-2.dll").exists()
    }

    doLast {
        downloadDir.mkdirs()
        targetDir.mkdirs()
        if (!archiveFile.exists()) {
            println("Downloading libmpv Windows x64 from $mpvUrl...")
            val tempFile = downloadDir.resolve("mpv-dev-x86_64.7z.part")
            tempFile.delete()
            URI(mpvUrl).toURL().openStream().use { input ->
                tempFile.outputStream().use { output -> input.copyTo(output) }
            }
            if (!tempFile.renameTo(archiveFile)) {
                tempFile.copyTo(archiveFile, overwrite = true)
                tempFile.delete()
            }
        }

        val candidates = listOf(
            "7z",
            "C:\\Program Files\\7-Zip\\7z.exe",
            "C:\\Program Files (x86)\\7-Zip\\7z.exe"
        )
        val sevenZip = candidates.firstOrNull { bin ->
            try {
                ProcessBuilder(bin).redirectErrorStream(true).start().waitFor()
                true
            } catch (_: Exception) {
                false
            }
        } ?: throw GradleException("7-Zip (7z) not found. Please install 7-Zip (e.g. 'winget install 7zip.7zip' or https://www.7-zip.org/)")

        println("Extracting libmpv to $targetDir...")
        val cmd = listOf(sevenZip, "e", "-y", archiveFile.absolutePath, "-o${targetDir.absolutePath}", "libmpv-2.dll")
        val process = ProcessBuilder(cmd).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            archiveFile.delete()
            throw GradleException("Failed to extract libmpv archive with 7z (exit code $exitCode):\n$output")
        }

        println("libmpv Windows bundled successfully.")
    }
}

tasks.register("setupMpvLinux") {
    group = "mpv"
    description = "Bundles libmpv for Linux from host system"
    val targetDir = layout.projectDirectory.dir("src/main/resources-packaged/linux/mpv").asFile
    outputs.dir(targetDir)
    onlyIf {
        !targetDir.resolve("libmpv.so.2").exists() && !targetDir.resolve("libmpv.so").exists()
    }

    doLast {
        val searchDirs = listOf(
            file("/usr/lib/x86_64-linux-gnu"),
            file("/usr/lib64"),
            file("/usr/lib")
        )
        val mpvLibDir = searchDirs.firstOrNull { dir ->
            dir.resolve("libmpv.so.2").exists() || dir.resolve("libmpv.so").exists()
        }

        if (mpvLibDir == null) {
            println("WARNING: libmpv not found on host Linux system. Install with: sudo apt install -y libmpv-dev mpv")
            return@doLast
        }

        println("Bundling libmpv from $mpvLibDir to $targetDir...")
        targetDir.mkdirs()

        copy {
            from(mpvLibDir) {
                include("libmpv.so*")
            }
            into(targetDir)
        }

        println("libmpv Linux bundled successfully.")
    }
}

tasks.register("setupMpvMacos") {
    group = "mpv"
    description = "Bundles libmpv for macOS from host system"
    val targetDir = layout.projectDirectory.dir("src/main/resources-packaged/macos/mpv").asFile
    outputs.dir(targetDir)
    onlyIf {
        !targetDir.resolve("libmpv.2.dylib").exists() && !targetDir.resolve("libmpv.dylib").exists()
    }

    doLast {
        val searchDirs = listOf(
            file("/opt/homebrew/lib"),
            file("/usr/local/lib"),
            file("/usr/lib")
        )
        val mpvLibDir = searchDirs.firstOrNull { dir ->
            dir.resolve("libmpv.2.dylib").exists() || dir.resolve("libmpv.dylib").exists()
        }

        if (mpvLibDir == null) {
            println("WARNING: libmpv not found on host macOS system. Install with: brew install mpv")
            return@doLast
        }

        println("Bundling libmpv from $mpvLibDir to $targetDir...")
        targetDir.mkdirs()

        val dylibFiles = mpvLibDir.listFiles { _, name -> name.startsWith("libmpv") && name.endsWith(".dylib") } ?: emptyArray()
        for (file in dylibFiles) {
            val realFile = file.canonicalFile
            val dest = targetDir.resolve(file.name)
            realFile.copyTo(dest, overwrite = true)
        }

        val bundlerCandidates = listOf("dylibbundler", "/opt/homebrew/bin/dylibbundler", "/usr/local/bin/dylibbundler")
        val dylibbundler = bundlerCandidates.firstOrNull { bin ->
            try {
                ProcessBuilder(bin, "-h").redirectErrorStream(true).start().waitFor() == 0
            } catch (_: Exception) {
                false
            }
        }

        if (dylibbundler != null) {
            println("Resolving and bundling dylib dependencies with dylibbundler...")
            val copiedDylibs = targetDir.listFiles { _, name -> name.startsWith("libmpv") && name.endsWith(".dylib") } ?: emptyArray()
            val cmd = mutableListOf(dylibbundler, "-b", "-d", targetDir.absolutePath, "-p", "@loader_path/", "-cd", "-of")
            for (lib in copiedDylibs) {
                cmd.add("-x")
                cmd.add(lib.absolutePath)
            }
            val process = ProcessBuilder(cmd).redirectErrorStream(true).start()
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                println("WARNING: dylibbundler failed with exit code $exitCode:\n$output")
            } else {
                println("Dependencies bundled successfully with dylibbundler.")
            }

            val allBundledDylibs = targetDir.listFiles { _, name -> name.endsWith(".dylib") } ?: emptyArray()
            for (dylib in allBundledDylibs) {
                var attempts = 0
                while (attempts < 10) {
                    attempts++
                    val otoolProc = ProcessBuilder("otool", "-l", dylib.absolutePath).start()
                    val otoolOutput = otoolProc.inputStream.bufferedReader().readText()
                    otoolProc.waitFor()

                    val rpaths = mutableListOf<String>()
                    val lines = otoolOutput.lines()
                    for (i in lines.indices) {
                        val line = lines[i].trim()
                        if (line.endsWith("LC_RPATH")) {
                            for (j in (i + 1)..minOf(i + 4, lines.size - 1)) {
                                val cand = lines[j].trim()
                                if (cand.startsWith("path ")) {
                                    val rpath = cand.removePrefix("path ").replace(Regex("\\s*\\(offset.*"), "").trim()
                                    rpaths.add(rpath)
                                    break
                                }
                            }
                        }
                    }

                    val duplicates = rpaths.groupingBy { it }.eachCount().filter { it.value > 1 }.keys
                    if (duplicates.isEmpty()) {
                        break
                    }

                    var deletedAny = false
                    for (dup in duplicates) {
                        println("Removing duplicate LC_RPATH '$dup' from ${dylib.name}...")
                        val proc = ProcessBuilder("install_name_tool", "-delete_rpath", dup, dylib.absolutePath)
                            .redirectErrorStream(true)
                            .start()
                        proc.inputStream.bufferedReader().readText()
                        if (proc.waitFor() == 0) {
                            deletedAny = true
                        }
                    }
                    if (!deletedAny) {
                        break
                    }
                }
            }

            for (dylib in allBundledDylibs) {
                try {
                    ProcessBuilder("codesign", "-f", "-s", "-", dylib.absolutePath)
                        .redirectErrorStream(true)
                        .start()
                        .waitFor()
                } catch (_: Exception) {
                }
            }
        } else {
            println("WARNING: dylibbundler not found. Dependent dylibs may not be bundled. Install with: brew install dylibbundler")
        }

        println("libmpv macOS bundled successfully.")
    }
}

tasks.matching {
    it.name.contains("Resource", ignoreCase = true) ||
    it.name.startsWith("package") ||
    it.name == "createDistributable"
}.configureEach {
    if (name != "setupMpvWindows" && name != "setupMpvLinux" && name != "setupMpvMacos") {
        val os = org.gradle.internal.os.OperatingSystem.current()
        if (os.isWindows) {
            dependsOn("setupMpvWindows")
        } else if (os.isLinux) {
            dependsOn("setupMpvLinux")
        } else if (os.isMacOsX) {
            dependsOn("setupMpvMacos")
        }
    }
}

compose.resources {
    packageOfResClass = "com.kayoi.pixelplayerdesktop.generated.resources"
    generateResClass = always
}


