package io.github.cdsap.td.paparazzi.plugin

import org.gradle.api.tasks.testing.Test
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test as JTest

class AndroidVariantConfiguratorTest {

    @JTest
    fun `wireTestTask sets td report dir system property to the relative path`() {
        val project = ProjectBuilder.builder().build()
        val testTask = project.tasks.create("testDebugUnitTest", Test::class.java)
        val mergeTask = project.tasks.register(
            "mergePaparazziDebugOutputs",
            MergePaparazziOutputsTask::class.java
        )

        AndroidVariantConfigurator.wireTestTask(testTask, mergeTask, "build/reports/paparazzi")

        // Must be the relative path, not absolutized. Test Distribution does not remap
        // system-property values, so daemon-side absolute paths would not resolve on agents.
        assertEquals(
            "build/reports/paparazzi",
            testTask.systemProperties[AndroidVariantConfigurator.TD_REPORT_DIR_SYSTEM_PROPERTY]
        )
    }

    @JTest
    fun `wireTestTask makes merge task a finalizer of the test task`() {
        val project = ProjectBuilder.builder().build()
        val testTask = project.tasks.create("testDebugUnitTest", Test::class.java)
        val mergeTask = project.tasks.register(
            "mergePaparazziDebugOutputs",
            MergePaparazziOutputsTask::class.java
        )

        AndroidVariantConfigurator.wireTestTask(testTask, mergeTask, "build/reports/paparazzi")

        val finalizers = testTask.finalizedBy.getDependencies(testTask).map { it.name }
        assertTrue(
            finalizers.contains("mergePaparazziDebugOutputs"),
            "Expected mergePaparazziDebugOutputs to be a finalizer; got: $finalizers"
        )
    }

    @JTest
    fun `td report dir system property name is paparazzi td report dir`() {
        // Locks the property name — TDHtmlReportWriter reads the same string,
        // so renaming on either side without updating the other would silently break the wiring.
        assertEquals("paparazzi.td.report.dir", AndroidVariantConfigurator.TD_REPORT_DIR_SYSTEM_PROPERTY)
    }

    @JTest
    fun `registerRemoteExecutionInputs declares intermediates under the exact variant slug`() {
        val project = ProjectBuilder.builder().build()
        val testTask = project.tasks.create("testFreeDebugUnitTest", Test::class.java)
        project.configurations.create("layoutlibResources")
        val runtimeClasspath = project.configurations.create("freeDebugRuntimeClasspath")

        // The directory Paparazzi actually writes to uses the camelCase variant slug verbatim.
        // The bug this guards against: re-deriving the name from the task name lowercases it to
        // "freedebug", so the declared input would point at a different, empty directory.
        val intermediates = project.layout.buildDirectory
            .dir("intermediates/paparazzi/freeDebug").get().asFile
        intermediates.mkdirs()
        val marker = intermediates.resolve("resources.json").apply { writeText("{}") }

        AndroidVariantConfigurator.registerRemoteExecutionInputs(
            project,
            testTask,
            variantName = "freeDebug",
            runtimeConfiguration = runtimeClasspath,
        )

        // Public API only: resolve the task's declared inputs and confirm the intermediates file
        // is among them. Property names / path sensitivity are asserted in the functional test,
        // where a real build can observe them without reaching into Gradle internals.
        assertTrue(
            testTask.inputs.files.files.contains(marker),
            "Expected the freeDebug paparazzi intermediates to be a declared input; " +
                "got: ${testTask.inputs.files.files}",
        )
    }

    @JTest
    fun `registerRemoteExecutionInputs does not fail when layoutlibResources is absent`() {
        val project = ProjectBuilder.builder().build()
        val testTask = project.tasks.create("testDebugUnitTest", Test::class.java)
        val runtimeClasspath = project.configurations.create("debugRuntimeClasspath")

        // No layoutlibResources configuration created — the guarded lookup must simply skip it
        // rather than throw.
        assertDoesNotThrow {
            AndroidVariantConfigurator.registerRemoteExecutionInputs(
                project,
                testTask,
                variantName = "debug",
                runtimeConfiguration = runtimeClasspath,
            )
        }
    }
}
