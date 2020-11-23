package org.jetbrains.kotlin

import groovy.lang.Closure
import org.gradle.api.Task
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.InputDirectory
import java.io.File

/**
 * A task that copies samples and replaces direct repository URLs with ones provided by the cache-redirector service.
 * This task also adds kotlin compiler repository from the project's gradle.properties file.
 */
open class CopySamples: Copy() {
    @InputDirectory
    var samplesDir: File = project.file("samples")

    private fun configureReplacements() {
        from(samplesDir) {
            it.exclude("**/*.gradle.kts")
            it.exclude("**/*.gradle")
            it.exclude("**/gradle.properties")
        }
        from(samplesDir) {
            it.include("**/*.gradle")
            val replacements = replacementsWithWrappedKey { s -> "maven { url '$s' }" } + centralReplacements
            it.filter { line ->
                val repo = line.trim()
                replacements[repo]?.let { r -> line.replace(repo, r) } ?: line
            }
        }
        from(samplesDir) {
            it.include("**/*.gradle.kts")
            val replacements = replacementsWithWrappedKey { s -> "maven(\"$s\")"} + centralReplacements
            it.filter { line ->
                val repo = line.trim()
                replacements[repo]?.let { r -> line.replace(repo, r) } ?: line
            }
        }
        from(samplesDir) {
            it.include("**/gradle.properties")

            val kotlinVersion = project.property("kotlinVersion") as? String
                ?: throw IllegalArgumentException("Property kotlinVersion should be specified in the root project")
            val kotlinCompilerRepo = project.property("kotlinCompilerRepo") as? String
                ?: throw IllegalArgumentException("Property kotlinCompilerRepo should be specified in the root project")

            it.filter { line ->
                when {
                    line.contains("kotlin_version") ->  "kotlin_version=$kotlinVersion"
                    line.contains("kotlinCompilerRepo") -> "kotlinCompilerRepo=$kotlinCompilerRepo"
                    else -> line
                }
            }
        }
    }

    override fun configure(closure: Closure<Any>): Task {
        super.configure(closure)
        configureReplacements()
        return this
    }

    private inline fun replacementsWithWrappedKey(wrap: (String) -> String) =
        urlReplacements.map { entry ->
            Pair(wrap(entry.key), entry.value)
        }.toMap()

    private val urlReplacements = mapOf(
        "https://dl.bintray.com/kotlin/kotlin-dev" to
                "maven { setUrl(\"https://cache-redirector.jetbrains.com/dl.bintray.com/kotlin/kotlin-dev\") }",
        "https://dl.bintray.com/kotlin/kotlin-eap" to
                "maven { setUrl(\"https://cache-redirector.jetbrains.com/dl.bintray.com/kotlin/kotlin-eap\") }",
        "https://dl.bintray.com/kotlin/ktor" to
                "maven { setUrl(\"https://cache-redirector.jetbrains.com/dl.bintray.com/kotlin/ktor\") }",
        "https://plugins.gradle.org/m2" to
                "maven { setUrl(\"https://cache-redirector.jetbrains.com/plugins.gradle.org/m2\") }"
    )

    private val centralReplacements = mapOf(
        "mavenCentral()" to "maven { setUrl(\"https://cache-redirector.jetbrains.com/maven-central\") }",
        "jcenter()" to "maven { setUrl(\"https://cache-redirector.jetbrains.com/jcenter\") }",
    )
}
