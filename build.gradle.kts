import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import java.util.Properties
import java.util.zip.ZipFile

plugins {
    java
}

group = "com.tinyfishing"
version = "1.0"

val hytaleServerJar = file(
    System.getProperty("hytale.server.jar")
        ?: "${System.getProperty("user.home")}/Library/Application Support/Hytale/install/release/package/game/latest/Server/HytaleServer.jar"
)

val hytaleSaveModsDir = file(
    System.getProperty("hytale.save.mods.dir")
        ?: "${System.getProperty("user.home")}/Library/Application Support/Hytale/UserData/Saves/TestWorld/mods"
)

fun readHytaleServerVersion(serverJar: File): String {
    require(serverJar.exists()) {
        "Missing Hytale server jar: ${serverJar.absolutePath}"
    }

    ZipFile(serverJar).use { zipFile ->
        val versionEntry = zipFile.getEntry("META-INF/maven/com.hypixel.hytale/Server/pom.properties")
            ?: error("Could not locate Hytale server version metadata in ${serverJar.absolutePath}")
        zipFile.getInputStream(versionEntry).use { inputStream ->
            val properties = Properties()
            properties.load(inputStream)
            return properties.getProperty("version")
                ?: error("Missing Hytale server version in ${serverJar.absolutePath}")
        }
    }
}

val hytaleServerVersion = readHytaleServerVersion(hytaleServerJar)

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.mongodb:bson:4.11.1")
    compileOnly(files(hytaleServerJar))
    testImplementation(files(hytaleServerJar))
    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.processResources {
    val releaseVersion = project.version.toString()
    filesMatching("manifest.json") {
        expand(
            "version" to releaseVersion,
            "serverVersion" to hytaleServerVersion
        )
    }
}

tasks.test {
    useJUnitPlatform()
    systemProperty("java.util.logging.manager", "com.hypixel.hytale.logger.backend.HytaleLogManager")
    testLogging {
        exceptionFormat = TestExceptionFormat.FULL
    }
}

tasks.jar {
    archiveBaseName.set("tiny-fishing")
    archiveVersion.set(project.version.toString())
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.register<Copy>("deployToHytaleSaveMods") {
    dependsOn(tasks.jar)
    val jarTask = tasks.jar.get()
    val targetArchiveName = jarTask.archiveFileName.get()
    val existingTinyFishingJars = hytaleSaveModsDir.listFiles { _, name ->
        name.startsWith("tiny-fishing-") && name.endsWith(".jar") && name != targetArchiveName
    } ?: emptyArray()
    doFirst {
        require(hytaleServerJar.exists()) {
            "Missing Hytale server jar: ${hytaleServerJar.absolutePath}"
        }
        if (!hytaleSaveModsDir.exists()) {
            hytaleSaveModsDir.mkdirs()
        }
        existingTinyFishingJars.forEach { existingJar ->
            if (existingJar.exists()) {
                existingJar.delete()
            }
        }
    }
    from(jarTask.archiveFile)
    into(hytaleSaveModsDir)
}
