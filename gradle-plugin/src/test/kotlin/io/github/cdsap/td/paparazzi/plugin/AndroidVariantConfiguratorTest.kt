package io.github.cdsap.td.paparazzi.plugin

import org.gradle.api.tasks.testing.Test
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test as JTest

class AndroidVariantConfiguratorTest {

    @JTest
    fun `wireTestTask sets td report dir system property`() {
        val project = ProjectBuilder.builder().build()
        val testTask = project.tasks.create("testDebugUnitTest", Test::class.java)
        val mergeTask = project.tasks.register(
            "mergePaparazziDebugOutputs",
            MergePaparazziOutputsTask::class.java
        )

        AndroidVariantConfigurator.wireTestTask(testTask, mergeTask, "/abs/path/to/input")

        assertEquals(
            "/abs/path/to/input",
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

        AndroidVariantConfigurator.wireTestTask(testTask, mergeTask, "/abs/path/to/input")

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
}
