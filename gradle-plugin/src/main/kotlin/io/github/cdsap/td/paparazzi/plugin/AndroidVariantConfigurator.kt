package io.github.cdsap.td.paparazzi.plugin

import com.android.build.api.variant.AndroidComponentsExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.tasks.PathSensitivity
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
                    registerRemoteExecutionInputs(
                        project,
                        testTask,
                        variant.name,
                        variant.runtimeConfiguration,
                    )
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

    /**
     * Declares the Paparazzi inputs the unit-test task reads at render time so that Test
     * Distribution ships them to remote agents and the build cache key stays correct. Without
     * these, a remote agent renders against a workspace that is missing layout resources.
     *
     * The caller passes values straight off the AGP variant model rather than re-deriving them
     * from the test task name:
     *  - [variantName] must be `Variant.name` — the exact slug Paparazzi uses when it writes
     *    `intermediates/paparazzi/<name>/`. Re-deriving it from the task name (strip `test` /
     *    `UnitTest`, lowercase) breaks for flavored variants — e.g. `freeDebug` would be looked
     *    up as `freedebug` and silently resolve to an empty directory.
     *  - [runtimeConfiguration] must be `Variant.runtimeConfiguration` — the same configuration
     *    the old code looked up by name as `<variant>RuntimeClasspath`, but obtained from AGP
     *    instead of reconstructed.
     *
     * All lookups are on the current project only (no cross-project configuration access), and
     * the artifact views resolve lazily, so this is safe under Project Isolation. The AAR view
     * excludes project components deliberately: those resources are tracked through normal task
     * dependencies, and filtering them keeps the view free of cross-project artifact resolution.
     */
    internal fun registerRemoteExecutionInputs(
        project: Project,
        testTask: Test,
        variantName: String,
        runtimeConfiguration: Configuration,
    ) {
        // 1. Paparazzi resource intermediates generated for this variant.
        testTask.inputs.dir(
            project.layout.buildDirectory.dir("intermediates/paparazzi/$variantName")
        )
            .withPropertyName("paparazzi.td.intermediates")
            .withPathSensitivity(PathSensitivity.RELATIVE)

        // 2. layoutlib resources — a single project-level configuration created by the
        //    Paparazzi plugin (not per-variant). Guarded because it only exists once Paparazzi
        //    has been configured.
        project.configurations.findByName("layoutlibResources")?.let { layoutlibResources ->
            val layoutlibDirs = layoutlibResources.incoming.artifactView { view ->
                view.attributes.attribute(
                    ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE,
                    ArtifactTypeDefinition.DIRECTORY_TYPE,
                )
            }.files
            testTask.inputs.files(layoutlibDirs)
                .withPropertyName("paparazzi.layoutlib.resources")
                .withPathSensitivity(PathSensitivity.NONE)
        }

        // 3. android-res directories contributed by external AAR dependencies on the variant
        //    runtime classpath. lenient(true) tolerates partially-resolvable graphs; the
        //    component filter drops project dependencies (tracked via task deps instead).
        val aarResourceDirs = runtimeConfiguration.incoming.artifactView { view ->
            view.attributes.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ANDROID_RES_TYPE)
            view.lenient(true)
            view.componentFilter { id -> id !is ProjectComponentIdentifier }
        }.files
        testTask.inputs.files(aarResourceDirs)
            .withPropertyName("paparazzi.aar.resource.dirs")
            .withPathSensitivity(PathSensitivity.NONE)
    }

    internal const val TD_REPORT_DIR_SYSTEM_PROPERTY = "paparazzi.td.report.dir"
    internal const val DEFAULT_INPUT_REPORT_DIR = "build/reports/paparazzi"
    private const val ANDROID_RES_TYPE = "android-res"
}
