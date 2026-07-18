plugins {
    // id("platform-conventions")
    id("com.gradleup.shadow")
    id("architectury-plugin")
    id("dev.architectury.loom")
}

architectury {
    common("fabric", "forge")
}

val otg: Configuration by configurations.creating

configurations {
    implementation {
        extendsFrom(otg)
    }
}

dependencies {
    modImplementation ("net.fabricmc:fabric-loader:${project.property("fabric_loader_version")}")
    //modApi "net.fabricmc.fabric-api:fabric-api:${rootProject.fabric_api_version}"
    // Remove the next line if you don"t want to depend on the API

    minecraft ("com.mojang:minecraft:${project.property("minecraft_version")}")
    mappings (loom.officialMojangMappings())

    otg(project(":common:common-core"))

    compileOnly ("org.projectlombok:lombok:1.18.32")
    annotationProcessor ("org.projectlombok:lombok:1.18.32")
}

tasks {
    shadowJar {
        dependencyFilter.apply {
            include(project(":common:common-annotation"))
            include(project(":common:common-util"))
            include(project(":common:common-customobject"))
            include(project(":common:common-generator"))
            include(project(":common:common-core"))
        }
        from(rootDir) {
            include("resources/**/*")
        }

        exclude("architectury.common.json")
        configurations = listOf(otg)
        archiveClassifier.set("deobf-all")
    }

    remapJar {
        //injectAccessWidener = true
        dependsOn(shadowJar)
        inputFile.set(shadowJar.get().archiveFile)

        archiveVersion = project.property("otg_version").toString()
    }
}