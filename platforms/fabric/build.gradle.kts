plugins {
    id("platform-conventions")
    id("dev.architectury.loom") version "0.10.0-SNAPSHOT"
}

loom {
    silentMojangMappingsLicense()
    accessWidener = file("src/main/resources/otg-fabric.accesswidener")
}

dependencies {
    minecraft("com.mojang:minecraft:1.18.2")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:0.13.3")

    // Fabric API. This is technically optional, but you probably want it anyway.
    modImplementation("net.fabricmc.fabric-api:fabric-api:0.48.0+1.18.2")

    implementation(project(":common:common-core"))
}

tasks {
    processResources {
        val replacements = mapOf(
            "version" to project.version
        )
        inputs.properties(replacements)

        filesMatching("fabric.mod.json") {
            expand(replacements)
        }
    }

    jar {
        archiveClassifier.set("deobf")
    }

    shadowJar {
        archiveClassifier.set("deobf-all")
    }

    remapJar {
        input.set(shadowJar.flatMap { it.archiveFile })
    }

    remapSourcesJar {
        fixRemapSourcesDependencies()
    }
}

otgPlatform {
    productionJar.set(tasks.remapJar.flatMap { it.archiveFile })
}
