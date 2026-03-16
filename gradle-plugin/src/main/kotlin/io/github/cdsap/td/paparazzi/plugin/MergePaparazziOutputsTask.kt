package io.github.cdsap.td.paparazzi.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Merges individual TD Paparazzi report directories (td-*) into a single consolidated report.
 *
 * Each test execution via Test Distribution produces its own report folder. This task
 * combines all runs, images, and videos into a unified report with a single index.
 */
@CacheableTask
abstract class MergePaparazziOutputsTask : DefaultTask() {

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val inputDirectory: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Internal
    val runList = mutableListOf<String>()

    @TaskAction
    fun merge() {
        val inputDir = inputDirectory.get().asFile
        if (inputDir.walkTopDown().count() <= 1) return

        val outputDir = outputDirectory.get().asFile
        cleanOutputDirectories(outputDir)
        copyStaticFiles(inputDir, outputDir)

        val foldersToCopy = listOf("runs", "images", "videos")
        inputDir.walkTopDown()
            .filter { it.isDirectory && it.name.startsWith("td-") }
            .forEach { tdDir ->
                tdDir.walkTopDown().forEach { file ->
                    if (file.isDirectory && foldersToCopy.contains(file.name)) {
                        file.copyRecursively(File(outputDir, file.name), overwrite = true)
                        if (file.name == "runs") {
                            extractRunNames(file)
                        }
                    }
                }
            }

        writeIndexJs(outputDir)
    }

    private fun extractRunNames(runsDir: File) {
        runsDir.walkTopDown()
            .filter { it.isFile && it.name.endsWith(".js") }
            .forEach { runList.add(it.nameWithoutExtension) }
    }

    private fun writeIndexJs(outputDir: File) {
        val formatted = runList.joinToString(",\n  ") { "\"$it\"" }
        File(outputDir, "index.js").writeText(
            "window.all_runs = [\n  $formatted\n];\n"
        )
    }

    private fun copyStaticFiles(inputDir: File, outputDir: File) {
        for (fileName in listOf("index.html", "paparazzi.js")) {
            inputDir.walkTopDown()
                .firstOrNull { it.isFile && it.name == fileName }
                ?.copyTo(File(outputDir, fileName), overwrite = true)
        }
    }

    private fun cleanOutputDirectories(outputDir: File) {
        for (name in listOf("images", "videos", "runs")) {
            File(outputDir, name).apply {
                if (isDirectory) deleteRecursively()
                mkdirs()
            }
        }
        for (name in listOf("index.html", "index.js", "paparazzi.js")) {
            File(outputDir, name).delete()
        }
    }
}
