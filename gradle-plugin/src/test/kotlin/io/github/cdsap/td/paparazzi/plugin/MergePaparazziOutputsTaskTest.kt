package io.github.cdsap.td.paparazzi.plugin

import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class MergePaparazziOutputsTaskTest {

    @TempDir
    lateinit var tempDir: File

    private fun createTask(): MergePaparazziOutputsTask {
        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        return project.tasks.create("mergeOutputs", MergePaparazziOutputsTask::class.java)
    }

    @Test
    fun `merge combines multiple td directories`() {
        val inputDir = File(tempDir, "input")
        val outputDir = File(tempDir, "output")

        // Create two td- report directories
        createTdReport(inputDir, "td-1000", "run_abc123", listOf("img1.png"), "shot1 content")
        createTdReport(inputDir, "td-2000", "run_def456", listOf("img2.png"), "shot2 content")

        val task = createTask()
        task.inputDirectory.set(task.project.layout.projectDirectory.dir(inputDir.absolutePath))
        task.outputDirectory.set(task.project.layout.projectDirectory.dir(outputDir.absolutePath))

        task.merge()

        // Verify merged output
        assertTrue(File(outputDir, "runs/run_abc123.js").exists())
        assertTrue(File(outputDir, "runs/run_def456.js").exists())
        assertTrue(File(outputDir, "images/img1.png").exists())
        assertTrue(File(outputDir, "images/img2.png").exists())

        val indexJs = File(outputDir, "index.js").readText()
        assertTrue(indexJs.contains("run_abc123"))
        assertTrue(indexJs.contains("run_def456"))
    }

    @Test
    fun `merge copies static files`() {
        val inputDir = File(tempDir, "input")
        val outputDir = File(tempDir, "output")

        val tdDir = File(inputDir, "td-1000")
        tdDir.mkdirs()
        File(inputDir, "td-1000/index.html").writeText("<html>test</html>")
        File(inputDir, "td-1000/paparazzi.js").writeText("// paparazzi js")
        File(inputDir, "td-1000/runs").mkdirs()
        File(inputDir, "td-1000/runs/run1.js").writeText("window.runs[\"run1\"] = [];")

        val task = createTask()
        task.inputDirectory.set(task.project.layout.projectDirectory.dir(inputDir.absolutePath))
        task.outputDirectory.set(task.project.layout.projectDirectory.dir(outputDir.absolutePath))

        task.merge()

        assertTrue(File(outputDir, "index.html").exists())
        assertEquals("<html>test</html>", File(outputDir, "index.html").readText())
        assertTrue(File(outputDir, "paparazzi.js").exists())
    }

    @Test
    fun `merge cleans output before writing`() {
        val inputDir = File(tempDir, "input")
        val outputDir = File(tempDir, "output")

        // Create stale output
        File(outputDir, "runs").mkdirs()
        File(outputDir, "runs/old_run.js").writeText("stale")
        File(outputDir, "images").mkdirs()
        File(outputDir, "images/old.png").writeText("stale")

        createTdReport(inputDir, "td-1000", "new_run", listOf("new.png"), "new content")

        val task = createTask()
        task.inputDirectory.set(task.project.layout.projectDirectory.dir(inputDir.absolutePath))
        task.outputDirectory.set(task.project.layout.projectDirectory.dir(outputDir.absolutePath))

        task.merge()

        // Old files should be gone
        assertTrue(!File(outputDir, "runs/old_run.js").exists())
        assertTrue(!File(outputDir, "images/old.png").exists())
        // New files should exist
        assertTrue(File(outputDir, "runs/new_run.js").exists())
        assertTrue(File(outputDir, "images/new.png").exists())
    }

    @Test
    fun `merge does nothing for empty input`() {
        val inputDir = File(tempDir, "input")
        inputDir.mkdirs()
        val outputDir = File(tempDir, "output")

        val task = createTask()
        task.inputDirectory.set(task.project.layout.projectDirectory.dir(inputDir.absolutePath))
        task.outputDirectory.set(task.project.layout.projectDirectory.dir(outputDir.absolutePath))

        task.merge()

        assertTrue(!File(outputDir, "index.js").exists())
    }

    @Test
    fun `merge handles video files`() {
        val inputDir = File(tempDir, "input")
        val outputDir = File(tempDir, "output")

        val tdDir = File(inputDir, "td-1000")
        File(tdDir, "videos").mkdirs()
        File(tdDir, "videos/abc.mov").writeText("fake video")
        File(tdDir, "runs").mkdirs()
        File(tdDir, "runs/run1.js").writeText("window.runs[\"run1\"] = [];")
        File(tdDir, "index.html").writeText("<html></html>")

        val task = createTask()
        task.inputDirectory.set(task.project.layout.projectDirectory.dir(inputDir.absolutePath))
        task.outputDirectory.set(task.project.layout.projectDirectory.dir(outputDir.absolutePath))

        task.merge()

        assertTrue(File(outputDir, "videos/abc.mov").exists())
        assertEquals("fake video", File(outputDir, "videos/abc.mov").readText())
    }

    @Test
    fun `index js format is valid`() {
        val inputDir = File(tempDir, "input")
        val outputDir = File(tempDir, "output")

        createTdReport(inputDir, "td-1000", "20240101120000_abc123", emptyList(), "")
        createTdReport(inputDir, "td-2000", "20240101120001_def456", emptyList(), "")

        val task = createTask()
        task.inputDirectory.set(task.project.layout.projectDirectory.dir(inputDir.absolutePath))
        task.outputDirectory.set(task.project.layout.projectDirectory.dir(outputDir.absolutePath))

        task.merge()

        val indexJs = File(outputDir, "index.js").readText()
        assertTrue(indexJs.startsWith("window.all_runs = ["))
        assertTrue(indexJs.trimEnd().endsWith("];"))
    }

    private fun createTdReport(
        inputDir: File,
        tdName: String,
        runName: String,
        imageNames: List<String>,
        runContent: String
    ) {
        val tdDir = File(inputDir, tdName)
        File(tdDir, "runs").mkdirs()
        File(tdDir, "images").mkdirs()
        File(tdDir, "videos").mkdirs()
        File(tdDir, "runs/$runName.js").writeText(
            "window.runs[\"$runName\"] = [$runContent];"
        )
        File(tdDir, "index.html").writeText("<html></html>")
        File(tdDir, "paparazzi.js").writeText("// js")
        for (imageName in imageNames) {
            File(tdDir, "images/$imageName").writeText("fake image data")
        }
    }
}
