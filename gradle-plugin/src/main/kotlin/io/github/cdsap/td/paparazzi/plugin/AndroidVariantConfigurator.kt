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

            val inputReportDirPath = extension.inputReportDir.getOrElse(DEFAULT_INPUT_REPORT_DIR)
            val inputReportDir = project.layout.projectDirectory.dir(inputReportDirPath)

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
                    wireTestTask(testTask, mergeTask, inputReportDirPath)
                }
            }
        }
    }

    /**
     * Wires a unit-test task to the TD report layout: sets the system property the writer
     * reads, and makes the merge task run after the test task finishes.
     *
     * [inputReportDirPath] should be a project-relative path (the same string the user
     * configures on the extension). Test Distribution does not path-remap system property
     * values, so a daemon-side absolute path would not resolve on remote agents — the
     * relative form lets each agent resolve it against its own workspace.
     */
    internal fun wireTestTask(
        testTask: Test,
        mergeTask: TaskProvider<MergePaparazziOutputsTask>,
        inputReportDirPath: String,
    ) {
        testTask.systemProperty(TD_REPORT_DIR_SYSTEM_PROPERTY, inputReportDirPath)
        testTask.finalizedBy(mergeTask)
    }

    internal const val TD_REPORT_DIR_SYSTEM_PROPERTY = "paparazzi.td.report.dir"
    internal const val DEFAULT_INPUT_REPORT_DIR = "build/reports/paparazzi"
}
