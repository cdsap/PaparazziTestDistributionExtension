package io.github.cdsap.td.paparazzi.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

class TDPaparazziPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val extension = project.extensions.create(
            "tdPaparazzi",
            TDPaparazziExtension::class.java
        )

        project.plugins.withId("app.cash.paparazzi") {
            addLibraryDependency(project, extension)

            project.plugins.withId("com.android.library") {
                AndroidVariantConfigurator.configure(project, extension)
            }
        }
    }

    private fun addLibraryDependency(project: Project, extension: TDPaparazziExtension) {
        project.afterEvaluate {
            val version = extension.libraryVersion.getOrElse(DEFAULT_LIBRARY_VERSION)
            project.dependencies.add(
                "testImplementation",
                "io.github.cdsap:td-paparazzi-ext:$version"
            )
        }
    }

    companion object {
        const val DEFAULT_LIBRARY_VERSION = "0.3.0"
    }
}
