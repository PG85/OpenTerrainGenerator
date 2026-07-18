import java.nio.file.FileSystems
import java.nio.file.Files

plugins {
    id("platform-conventions")
    id("com.gradleup.shadow")
    id("architectury-plugin")
    id("dev.architectury.loom")
}

architectury {
    platformSetupLoomIde()
    forge()
}

val otg: Configuration by configurations.creating
// Shared module: compiled/run-in-dev via architectury's development configuration so that
// modlauncher sees it as part of the mod (mixin configs in shared are unreadable otherwise).
// Production jar gets the Forge-transformed shared jar merged in shadowJar below.
val common: Configuration by configurations.creating
configurations {
    implementation {
        extendsFrom(otg)
    }
    compileClasspath.get().extendsFrom(common)
    runtimeClasspath.get().extendsFrom(common)
    named("developmentForge") {
        extendsFrom(common)
    }
}

dependencies {
    "forge"("net.minecraftforge:forge:${project.property("forge_version")}")

    minecraft("com.mojang:minecraft:${project.property("minecraft_version")}")
    mappings(loom.officialMojangMappings())

    otg(project(":common:common-core"))
    common(project(path = ":platforms:shared", configuration = "namedElements")) { isTransitive = false }

    compileOnly("org.projectlombok:lombok:1.18.32")
    annotationProcessor("org.projectlombok:lombok:1.18.32")
}

// Dev runs only: the common modules split packages between each other (com.pg85.otg.config
// lives in common-util AND common-core, etc.), which modlauncher's JPMS module layer rejects
// when each jar is its own automatic module. Merge them into a single jar for the game layer.
// Production is unaffected — shadowJar merges everything into one jar anyway.
val devLibsJar = tasks.register<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("devLibsJar") {
    archiveBaseName.set("otg-common-dev-libs")
    archiveClassifier.set("dev-libs")
    // Explicit module name — the automatic one derived from the file name would clash with Forge's
    manifest {
        attributes("Automatic-Module-Name" to "com.pg85.otg.devlibs")
    }
    configurations = listOf(otg)
    // jsonSchema requires validation.api at module resolution and isn't shipped in production
    dependencyFilter.apply {
        exclude(dependency("com.fasterxml.jackson.module:jackson-module-jsonSchema"))
    }
    exclude("module-info.class")
    exclude("META-INF/versions/*/module-info.class")
}

dependencies {
    "forgeRuntimeLibrary"(files(devLibsJar.flatMap { it.archiveFile }))
}

loom {
    forge {
        mixinConfig("otg-shared.mixins.json")
    }
}

tasks {
    processResources {
        inputs.property("version", project.property("otg_version"))
        inputs.property("minecraft_version", project.property("minecraft_version"))

        filesMatching("META-INF/mods.toml") {
            val map = mapOf(
                "version" to inputs.properties["version"].toString(),
                "minecraft_version" to inputs.properties["minecraft_version"].toString(),
            )
            expand(map)
        }
    }

    shadowJar {
        dependencyFilter.apply {
            include(project(":common:common-annotation"))
            include(project(":common:common-util"))
            include(project(":common:common-customobject"))
            include(project(":common:common-generator"))
            include(project(":common:common-core"))
            // shared excluded from dep filter — included via Forge-transformed jar below
        }
        dependsOn(":platforms:shared:transformProductionForge")
        from(zipTree(project(":platforms:shared").layout.buildDirectory.file("libs/shared-${project.property("otg_version")}-SNAPSHOT-transformProductionForge.jar")))
        exclude("architectury.common.json")
        configurations = listOf(otg)
        archiveClassifier.set("deobf-all")

        // Loader marker + stale refmap removal — see the fabric shadowJar note:
        // the copies must differ so the Forgix merge splits the config per loader.
        doLast {
            FileSystems.newFileSystem(archiveFile.get().asFile.toPath()).use { fs ->
                val path = fs.getPath("otg-shared.mixins.json")
                if (Files.exists(path)) {
                    val json = Files.readString(path)
                        .replaceFirst("\"refmap\": \"shared-platforms_shared-refmap.json\",\n", "")
                        .replaceFirst("{", "{\n  \"_forgix_loader\": \"forge\",")
                    Files.writeString(path, json)
                }
            }
        }
    }

    remapJar {
        dependsOn(shadowJar)
        inputFile.set(shadowJar.get().archiveFile)
        archiveVersion = project.property("otg_version").toString()
    }
}

otgPlatform {
    productionJar.set(tasks.remapJar.flatMap { it.archiveFile })
}
