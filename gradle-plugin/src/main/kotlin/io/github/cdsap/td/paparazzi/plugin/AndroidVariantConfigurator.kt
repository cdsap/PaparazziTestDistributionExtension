package io.github.cdsap.td.paparazzi.plugin

import com.android.build.api.variant.AndroidComponentsExtension
import org.gradle.api.Project
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

            val mergeTask = project.tasks.register(
                "mergePaparazzi${variantName}Outputs",
                MergePaparazziOutputsTask::class.java
            ) { task ->
                task.dependsOn(testTaskName)
                task.inputDirectory.set(
                    project.layout.projectDirectory.dir(
                        extension.inputReportDir.getOrElse("build/reports/paparazzi")
                    )
                )
                task.outputDirectory.set(
                    project.layout.projectDirectory.dir(
                        extension.outputReportDir.getOrElse("build/reports/paparazzi-td")
                    )
                )
            }

            project.tasks.withType(Test::class.java).configureEach { testTask ->
                if (testTask.name == "test${variantName}UnitTest") {
                    testTask.finalizedBy(mergeTask)
                }
            }
        }
    }
}
