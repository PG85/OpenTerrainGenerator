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
    fabric()
}

val otg: Configuration by configurations.creating
configurations {
    implementation {
        extendsFrom(otg)
    }
}

dependencies {
    modImplementation("net.fabricmc:fabric-loader:${project.property("fabric_loader_version")}")
    modApi("net.fabricmc.fabric-api:fabric-api:${project.property("fabric_api_version")}")

    minecraft("com.mojang:minecraft:${project.property("minecraft_version")}")
    mappings(loom.officialMojangMappings())

    otg(project(":common:common-core"))
    otg(project(path = ":platforms:shared", configuration = "namedElements")) { isTransitive = false }

    compileOnly("org.projectlombok:lombok:1.18.32")
    annotationProcessor("org.projectlombok:lombok:1.18.32")

}

loom {
    //accessWidenerPath = file("src/main/resources/META-INF/otg.accesswidener")
}

tasks {
    processResources {
        inputs.property("version", project.property("otg_version"))
        inputs.property("minecraft_version", project.property("minecraft_version"))
        inputs.property("fabric_loader_version", project.property("fabric_loader_version"))

        filesMatching("fabric.mod.json") {
            val map = mapOf(
                "version" to inputs.properties["version"].toString(),
                "minecraft_version" to inputs.properties["minecraft_version"].toString(),
                "fabric_loader_version" to inputs.properties["fabric_loader_version"].toString(),
            )
            expand(map)
        }
    }

    shadowJar {
        dependencyFilter.apply {
            include(project(":platforms:shared"))
        }
        exclude("architectury.common.json")
        configurations = listOf(otg)
        archiveClassifier.set("deobf-all")

        // Stamp a loader marker into the shared mixin config (and drop the stale
        // refmap key — loom 1.17 statically remaps mixin bytecode and emits no
        // refmaps). The marker makes the fabric and forge copies differ so the
        // Forgix merge splits them into otg-shared.mixins_<loader>.json and
        // rewires fabric.mod.json / the forge manifest — identical copies would
        // be deduplicated into one config that cannot serve both loaders' renamed
        // mixin classes. GSON ignores unknown keys, so the marker is inert.
        doLast {
            FileSystems.newFileSystem(archiveFile.get().asFile.toPath()).use { fs ->
                val path = fs.getPath("otg-shared.mixins.json")
                if (Files.exists(path)) {
                    val json = Files.readString(path)
                        .replaceFirst("\"refmap\": \"shared-platforms_shared-refmap.json\",\n", "")
                        .replaceFirst("{", "{\n  \"_forgix_loader\": \"fabric\",")
                    Files.writeString(path, json)
                }
            }
        }
    }

    remapJar {
        //injectAccessWidener = true
        dependsOn(shadowJar)
        inputFile.set(shadowJar.get().archiveFile)

        archiveVersion = project.property("otg_version").toString()
    }

    sourcesJar {
        //val commonSources = project(":platforms:shared").tasks.sourcesJar
        //def sharedSources = project(":platforms:shared").sourcesJar
        //dependsOn commonSources, sharedSources
        //from(commonSources.get().archiveFile.map { zipTree(it) })
        //from sharedSources.archiveFile.map { zipTree(it) }
    }
}

otgPlatform {
    productionJar.set(tasks.remapJar.flatMap { it.archiveFile })
}