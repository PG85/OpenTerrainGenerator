import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

plugins {
    id("parent-logic")
    id ("architectury-plugin") version "3.5.169" apply false
    id ("dev.architectury.loom") version "1.17.491" apply false
    id ("maven-publish")
    // Java-24 bytecode — hence the JDK-24 daemon pin in gradle/gradle-daemon-jvm.properties
    id ("io.github.pacifistmc.forgix") version "2.0.0-SNAPSHOT.5.1"
}

defaultTasks = arrayListOf("build", "publishToMavenLocal")

val ignored = listOf("common", "platforms")

subprojects {
    if (!ignored.contains(project.name)) {
        apply(plugin = "base-conventions")
    }
}

version = project.property("otg_version").toString()
group = project.property("otg_group").toString()

listOf(
    //project(":platforms:paper"),
    project(":platforms:forge"),
    project(":platforms:fabric"),
).forEach { proj ->
    proj.afterEvaluate {
        // Show more errors in intellij
        proj.tasks.withType<JavaCompile>() {
            options.compilerArgs.add("-Xmaxerrs")
            options.compilerArgs.add("5000")
        }
    }
}

// ---------------------------------------------------------------------------
// Toolchain: Gradle 9.4.1 + architectury-loom 1.17 + JDK-24 daemon. Loom 1.17
// statically remaps mixin bytecode per platform (intermediary/SRG inlined, no
// refmaps emitted) — earlier loom 1.8-1.11 left @Shadow members unmapped with no
// refmap entry, crashing production fabric at boot; 1.17 restores loom-1.7
// behavior on a modern Gradle.
//
//   ./gradlew build mergeJars   ->   build/forgix/otg-<ver>-fabric-forge.jar
// ---------------------------------------------------------------------------
forgix {
    archiveVersion.set(
        project.property("otg_version").toString() +
            project.property("otg_build").toString().let { if (it.isEmpty()) "" else "-$it" }
    )
    archiveClassifier.set("fabric-forge")
}

// remapJar tasks only exist after the platform projects are evaluated.
// Forgix mutates its input jars in place during the merge; the platform jar
// manifests carry an always-changing timestamp, so remapJar regenerates them
// on every build and build/distributions copies stay pristine.
gradle.projectsEvaluated {
    forgix {
        fabric {
            inputJar.set(
                project(":platforms:fabric").tasks
                    .named<org.gradle.jvm.tasks.Jar>("remapJar")
                    .flatMap { it.archiveFile }
            )
        }
        forge {
            inputJar.set(
                project(":platforms:forge").tasks
                    .named<org.gradle.jvm.tasks.Jar>("remapJar")
                    .flatMap { it.archiveFile }
            )
        }
    }
    tasks.named("mergeJars") {
        dependsOn(":platforms:fabric:remapJar", ":platforms:forge:remapJar")
        finalizedBy("fixMergedMixinConfigs")
    }
}

// Forgix renames every shared class that differs between loaders
// (fabric=intermediary, forge=SRG bytecode) to <Name>_fabric / <Name>_forge and
// rewrites references — EXCEPT the mixin config's own bare `mixins`/`client`
// entries (name + separate `package`, which its resource rewriter can't match).
// Left unfixed they point at the now-nonexistent unsuffixed class and every
// shared mixin fails to apply at boot. Suffix them to the renamed classes
// actually present in the jar. (The configs themselves are split per loader by
// Forgix because each platform's shadowJar stamps a loader marker into its copy
// — see the platform build scripts.)
tasks.register("fixMergedMixinConfigs") {
    doLast {
        val mergedJar = layout.buildDirectory.dir("forgix").get().asFile
            .listFiles { f -> f.name.endsWith("-fabric-forge.jar") }
            ?.maxByOrNull { it.lastModified() }
            ?: error("No merged -fabric-forge.jar found in build/forgix")

        ZipFile(mergedJar).use { zin ->
            val names = zin.entries().asSequence().map { it.name }.toSet()
            val patched = mutableMapOf<String, ByteArray>()
            listOf("fabric", "forge").forEach { loader ->
                val cfgName = "otg-shared.mixins_$loader.json"
                val entry = zin.getEntry(cfgName)
                    ?: error("$cfgName missing from merged jar — the per-loader config split did not happen")
                @Suppress("UNCHECKED_CAST")
                val cfg = JsonSlurper().parse(zin.getInputStream(entry).readBytes()) as MutableMap<String, Any?>
                val pkgDir = (cfg["package"] as String).trimEnd('.').replace('.', '/')

                // Forgix appends its loader suffix AFTER the inner-class separator
                // (Foo$1 -> Foo$1_fabric), which breaks Mixin's Outer$Inner companion
                // detection: the class is never conformed and direct classloading of it
                // is blocked at runtime (IllegalClassLoadError on world creation).
                // Mixins must compile to a single top-level class — no anonymous/local
                // classes and no enum switches (javac emits a $SwitchMap inner class).
                val brokenInner = names.filter {
                    it.startsWith("$pkgDir/") && it.contains('$') &&
                        (it.endsWith("_fabric.class") || it.endsWith("_forge.class"))
                }
                if (brokenInner.isNotEmpty()) {
                    error("Forgix-renamed mixin inner classes found (mixins must not compile to nested classes): $brokenInner")
                }
                var missing = 0
                for (key in listOf("mixins", "client")) {
                    val list = (cfg[key] as? List<*>)?.map { it as String } ?: continue
                    cfg[key] = list.map { n ->
                        val suffixed = "${n}_$loader"
                        when {
                            n.endsWith("_$loader") -> n
                            names.contains("$pkgDir/$suffixed.class") -> suffixed
                            names.contains("$pkgDir/$n.class") -> n
                            else -> { missing++; n }
                        }
                    }
                }
                if (missing > 0) error("$cfgName: $missing mixin entries resolve to no class in the merged jar")
                patched[cfgName] = JsonOutput.prettyPrint(JsonOutput.toJson(cfg)).toByteArray()
            }
            val tmp = File(mergedJar.parentFile, mergedJar.name + ".tmp")
            ZipOutputStream(tmp.outputStream()).use { zout ->
                for (e in zin.entries()) {
                    zout.putNextEntry(ZipEntry(e.name))
                    zout.write(patched[e.name] ?: zin.getInputStream(e).readBytes())
                    zout.closeEntry()
                }
            }
            tmp.copyTo(mergedJar, overwrite = true); tmp.delete()
            logger.lifecycle("fixMergedMixinConfigs: suffixed + verified ${patched.keys} in $mergedJar")
        }
    }
}
