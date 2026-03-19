package io.github.cdsap.td.paparazzi.plugin

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import java.util.stream.Stream

/**
 * Functional tests that verify the plugin works across different Gradle versions
 * and that paparazzi version decoupling works correctly.
 *
 * These tests use Gradle TestKit to run real Gradle builds in isolated project directories.
 * They don't require an Android SDK — they test the plugin configuration and merge task
 * in a plain Java/Kotlin project context.
 */
class CompatibilityTest {

    @TempDir
    lateinit var projectDir: File

    private val buildFile get() = File(projectDir, "build.gradle.kts")
    private val settingsFile get() = File(projectDir, "settings.gradle.kts")

    @BeforeEach
    fun setup() {
        settingsFile.writeText(
            """
            rootProject.name = "test-project"
            """.trimIndent()
        )
    }

    @ParameterizedTest(name = "plugin applies and creates extension on Gradle {0}")
    @MethodSource("gradleVersions")
    fun `plugin applies and creates extension`(gradleVersion: String) {
        buildFile.writeText(
            """
            plugins {
                id("io.github.cdsap.td.paparazzi")
            }

            tasks.register("verifyExtension") {
                doLast {
                    val ext = project.extensions.findByName("tdPaparazzi")
                    requireNotNull(ext) { "tdPaparazzi extension not found" }
                    println("EXTENSION_OK")
                }
            }
            """.trimIndent()
        )

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("verifyExtension", "--stacktrace")
            .withGradleVersion(gradleVersion)
            .build()

        assertTrue(result.output.contains("EXTENSION_OK"))
    }

    @ParameterizedTest(name = "extension properties are configurable on Gradle {0}")
    @MethodSource("gradleVersions")
    fun `extension properties are configurable`(gradleVersion: String) {
        buildFile.writeText(
            """
            plugins {
                id("io.github.cdsap.td.paparazzi")
            }

            tdPaparazzi {
                inputReportDir.set("custom/input")
                outputReportDir.set("custom/output")
                libraryVersion.set("1.0.0")
            }

            tasks.register("verifyConfig") {
                doLast {
                    val ext = project.extensions.findByName("tdPaparazzi")
                    requireNotNull(ext) { "tdPaparazzi extension not found" }
                    val inputMethod = ext.javaClass.getMethod("getInputReportDir")
                    val outputMethod = ext.javaClass.getMethod("getOutputReportDir")
                    val versionMethod = ext.javaClass.getMethod("getLibraryVersion")
                    val input = (inputMethod.invoke(ext) as org.gradle.api.provider.Property<*>).get()
                    val output = (outputMethod.invoke(ext) as org.gradle.api.provider.Property<*>).get()
                    val version = (versionMethod.invoke(ext) as org.gradle.api.provider.Property<*>).get()
                    require(input == "custom/input") { "inputReportDir mismatch: ${'$'}input" }
                    require(output == "custom/output") { "outputReportDir mismatch: ${'$'}output" }
                    require(version == "1.0.0") { "libraryVersion mismatch: ${'$'}version" }
                    println("CONFIG_OK")
                }
            }
            """.trimIndent()
        )

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("verifyConfig", "--stacktrace")
            .withGradleVersion(gradleVersion)
            .build()

        assertTrue(result.output.contains("CONFIG_OK"))
    }

    @ParameterizedTest(name = "merge task works on Gradle {0}")
    @MethodSource("gradleVersions")
    fun `merge task executes correctly`(gradleVersion: String) {
        val inputDir = File(projectDir, "build/reports/paparazzi")
        createTdReport(inputDir, "td-agent1", "run_abc123", listOf("snap1.png"))
        createTdReport(inputDir, "td-agent2", "run_def456", listOf("snap2.png"))

        buildFile.writeText(
            """
            plugins {
                id("io.github.cdsap.td.paparazzi")
            }

            tasks.register<io.github.cdsap.td.paparazzi.plugin.MergePaparazziOutputsTask>("testMerge") {
                inputDirectory.set(layout.projectDirectory.dir("build/reports/paparazzi"))
                outputDirectory.set(layout.projectDirectory.dir("build/reports/paparazzi-td"))
            }
            """.trimIndent()
        )

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("testMerge", "--stacktrace")
            .withGradleVersion(gradleVersion)
            .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":testMerge")?.outcome)

        val outputDir = File(projectDir, "build/reports/paparazzi-td")
        assertTrue(File(outputDir, "runs/run_abc123.js").exists())
        assertTrue(File(outputDir, "runs/run_def456.js").exists())
        assertTrue(File(outputDir, "images/snap1.png").exists())
        assertTrue(File(outputDir, "images/snap2.png").exists())
        assertTrue(File(outputDir, "index.html").exists())
        assertTrue(File(outputDir, "paparazzi.js").exists())

        val indexJs = File(outputDir, "index.js").readText()
        assertTrue(indexJs.contains("run_abc123"))
        assertTrue(indexJs.contains("run_def456"))
    }

    @ParameterizedTest(name = "merge task is UP-TO-DATE on second run with Gradle {0}")
    @MethodSource("gradleVersions")
    fun `merge task is cacheable`(gradleVersion: String) {
        val inputDir = File(projectDir, "build/reports/paparazzi")
        createTdReport(inputDir, "td-agent1", "run_abc", listOf("img.png"))

        buildFile.writeText(
            """
            plugins {
                id("io.github.cdsap.td.paparazzi")
            }

            tasks.register<io.github.cdsap.td.paparazzi.plugin.MergePaparazziOutputsTask>("testMerge") {
                inputDirectory.set(layout.projectDirectory.dir("build/reports/paparazzi"))
                outputDirectory.set(layout.projectDirectory.dir("build/reports/paparazzi-td"))
            }
            """.trimIndent()
        )

        val runner = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("testMerge", "--stacktrace")
            .withGradleVersion(gradleVersion)

        val firstResult = runner.build()
        assertEquals(TaskOutcome.SUCCESS, firstResult.task(":testMerge")?.outcome)

        val secondResult = runner.build()
        assertEquals(TaskOutcome.UP_TO_DATE, secondResult.task(":testMerge")?.outcome)
    }

    private fun createTdReport(
        inputDir: File,
        tdName: String,
        runName: String,
        imageNames: List<String>
    ) {
        val tdDir = File(inputDir, tdName)
        File(tdDir, "runs").mkdirs()
        File(tdDir, "images").mkdirs()
        File(tdDir, "videos").mkdirs()
        File(tdDir, "runs/$runName.js").writeText("window.runs[\"$runName\"] = [];")
        File(tdDir, "index.html").writeText("<html><body>Paparazzi Report</body></html>")
        File(tdDir, "paparazzi.js").writeText("// paparazzi.js")
        for (imageName in imageNames) {
            File(tdDir, "images/$imageName").writeText("fake image data")
        }
    }

    companion object {
        @JvmStatic
        fun gradleVersions(): Stream<Arguments> = Stream.of(
            Arguments.of("8.11"),
            Arguments.of("8.13"),
            Arguments.of("9.2.1")
        )
    }
}
