package io.github.cdsap.td.paparazzi.plugin

import com.android.build.api.variant.AndroidComponentsExtension
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.Test

/**
 * Separated from the plugin class so that AGP classes are only loaded
 * when the Android plugin is actually present on the classpath.
 */
internal object AndroidVariantConfigurator {

    fun configure(project: Project, extension: TDPaparazziExtension) {
        val androidComponents = project.extensions.getByType(AndroidComponentsExtension::class.java)
        androidComponents.onVariants(androidComponents.selector().all()) { variant ->
            val variantName = variant.name.replaceFirstChar { it.uppercaseChar() }

            val testTaskName = "test${variantName}UnitTest"

            val inputReportDir = project.layout.projectDirectory.dir(
                extension.inputReportDir.getOrElse(DEFAULT_INPUT_REPORT_DIR)
            )

            val mergeTask = project.tasks.register(
                "mergePaparazzi${variantName}Outputs",
                MergePaparazziOutputsTask::class.java
            ) { task ->
                task.dependsOn(testTaskName)
                task.inputDirectory.set(inputReportDir)
                task.outputDirectory.set(
                    project.layout.projectDirectory.dir(
                        extension.outputReportDir.getOrElse("build/reports/paparazzi-td")
                    )
                )
                task.cleanupTdDirectories.set(extension.cleanupTdDirectories.orElse(false))
            }

            project.tasks.withType(Test::class.java).configureEach { testTask ->
                if (testTask.name == testTaskName) {
                    wireTestTask(testTask, mergeTask, inputReportDir.asFile.absolutePath)
                }
            }
        }
    }

    /**
     * Wires a unit-test task to the TD report layout: sets the system property the writer
     * reads, and makes the merge task run after the test task finishes.
     */
    internal fun wireTestTask(
        testTask: Test,
        mergeTask: TaskProvider<MergePaparazziOutputsTask>,
        inputReportDirAbsolutePath: String,
    ) {
        testTask.systemProperty(TD_REPORT_DIR_SYSTEM_PROPERTY, inputReportDirAbsolutePath)
        testTask.finalizedBy(mergeTask)
    }

    internal const val TD_REPORT_DIR_SYSTEM_PROPERTY = "paparazzi.td.report.dir"
    internal const val DEFAULT_INPUT_REPORT_DIR = "build/reports/paparazzi"
}
