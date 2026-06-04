package io.github.cdsap.td.paparazzi

import app.cash.paparazzi.Snapshot
import app.cash.paparazzi.TestName
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.awt.image.BufferedImage
import java.io.File
import java.util.Date

class TDHtmlReportWriterTest {

    @TempDir
    lateinit var tempDir: File

    @AfterEach
    fun cleanup() {
        System.clearProperty("paparazzi.test.record")
        System.clearProperty("paparazzi.td.report.dir")
        System.clearProperty("paparazzi.build.dir")
        System.clearProperty("paparazzi.snapshot.dir")
    }

    @Test
    fun `default rootDirectory honors paparazzi td report dir system property`() {
        val configuredParent = File(tempDir, "custom-input").also { it.mkdirs() }
        System.setProperty("paparazzi.td.report.dir", configuredParent.absolutePath)

        TDHtmlReportWriter(
            runName = "testrun",
            snapshotRootDirectory = File(tempDir, "snapshots")
        )

        val tdDirs = configuredParent.listFiles()
            ?.filter { it.isDirectory && it.name.startsWith("td-") }
            ?: emptyList()
        assertEquals(1, tdDirs.size, "Expected a single td-* directory under the configured parent")
        assertTrue(File(tdDirs.single(), "runs").exists())
        assertTrue(File(tdDirs.single(), "images").exists())
        assertTrue(File(tdDirs.single(), "videos").exists())
    }

    @Test
    fun `constructor creates the standard Paparazzi report directories`() {
        val rootDir = File(tempDir, "reports")
        TDHtmlReportWriter(
            runName = "testrun",
            rootDirectory = rootDir,
            snapshotRootDirectory = File(tempDir, "snapshots")
        )

        assertTrue(File(rootDir, "runs").exists())
        assertTrue(File(rootDir, "images").exists())
        assertTrue(File(rootDir, "videos").exists())
    }

    @Test
    fun `constructor writes the run js file`() {
        val rootDir = File(tempDir, "reports")
        TDHtmlReportWriter(
            runName = "testrun",
            rootDirectory = rootDir,
            snapshotRootDirectory = File(tempDir, "snapshots")
        )

        val runJs = File(rootDir, "runs/testrun.js")
        assertTrue(runJs.exists())
        val content = runJs.readText()
        assertTrue(content.startsWith("window.runs[\"testrun\"] = "))
    }

    @Test
    fun `constructor writes the all-runs index`() {
        val rootDir = File(tempDir, "reports")
        TDHtmlReportWriter(
            runName = "testrun",
            rootDirectory = rootDir,
            snapshotRootDirectory = File(tempDir, "snapshots")
        )

        val indexJs = File(rootDir, "index.js")
        assertTrue(indexJs.exists())
        val content = indexJs.readText()
        assertTrue(content.startsWith("window.all_runs = "))
        assertTrue(content.contains("testrun"))
    }

    @Test
    fun `single-frame snapshot writes a png in images`() {
        val rootDir = File(tempDir, "reports")
        val writer = TDHtmlReportWriter(
            runName = "testrun",
            rootDirectory = rootDir,
            snapshotRootDirectory = File(tempDir, "snapshots")
        )

        val snapshot = Snapshot(
            name = "test_shot",
            testName = TestName("com.example", "MyTest", "testMethod"),
            timestamp = Date(),
            file = null
        )

        writer.newFrameHandler(snapshot, 1, -1).use { handler ->
            handler.handle(BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB))
        }

        val images = File(rootDir, "images").listFiles().orEmpty()
        assertTrue(images.any { it.name.endsWith(".png") })
    }

    @Test
    fun `multi-frame gif writes a single animated png in videos and no per-frame files`() {
        val rootDir = File(tempDir, "reports")
        val snapshotDir = File(tempDir, "snapshots")
        System.setProperty("paparazzi.test.record", "true")

        val writer = TDHtmlReportWriter(
            runName = "testrun",
            rootDirectory = rootDir,
            snapshotRootDirectory = snapshotDir
        )

        val snapshot = Snapshot(
            name = "animation",
            testName = TestName("com.example", "MyTest", "testAnimation"),
            timestamp = Date(),
            file = null
        )

        writer.newFrameHandler(snapshot, 3, 30).use { handler ->
            for (i in 0 until 3) {
                val image = BufferedImage(40, 40, BufferedImage.TYPE_INT_ARGB)
                image.createGraphics().apply {
                    fillRect(i * 10, 0, 10, 10)
                    dispose()
                }
                handler.handle(image)
            }
        }

        val videos = File(rootDir, "videos").listFiles().orEmpty()
        assertEquals(1, videos.count { it.extension == "png" }, "Expected exactly one APNG in videos/")
        assertTrue(videos.none { it.extension == "mov" }, "No legacy .mov should be written")

        val goldenVideos = File(snapshotDir, "videos").listFiles().orEmpty()
        assertEquals(1, goldenVideos.count { it.extension == "png" }, "Expected exactly one golden APNG")

        val goldenImages = File(snapshotDir, "images").listFiles().orEmpty()
        assertTrue(
            goldenImages.none { it.name.contains("animation") },
            "Per-frame goldens should not be written into images/"
        )
    }

    @Test
    fun `recording mode copies single-frame snapshot into images golden directory`() {
        System.setProperty("paparazzi.test.record", "true")
        val rootDir = File(tempDir, "reports")
        val snapshotDir = File(tempDir, "snapshots")

        val writer = TDHtmlReportWriter(
            runName = "testrun",
            rootDirectory = rootDir,
            snapshotRootDirectory = snapshotDir
        )

        val snapshot = Snapshot(
            name = "golden",
            testName = TestName("com.example", "MyTest", "testGolden"),
            timestamp = Date(),
            file = null
        )

        writer.newFrameHandler(snapshot, 1, -1).use { handler ->
            handler.handle(BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB))
        }

        val goldenImages = File(snapshotDir, "images").listFiles().orEmpty()
        assertTrue(goldenImages.any { it.name.endsWith(".png") })
    }

    @Test
    fun `close writes final run js listing the shots`() {
        val rootDir = File(tempDir, "reports")
        val writer = TDHtmlReportWriter(
            runName = "testrun",
            rootDirectory = rootDir,
            snapshotRootDirectory = File(tempDir, "snapshots")
        )

        val snapshot = Snapshot(
            name = "test_shot",
            testName = TestName("com.example", "MyTest", "testMethod"),
            timestamp = Date(),
            file = null
        )
        writer.newFrameHandler(snapshot, 1, -1).use { handler ->
            handler.handle(BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB))
        }
        writer.close()

        val runJs = File(rootDir, "runs/testrun.js").readText()
        assertTrue(runJs.contains("test_shot"))
        assertTrue(runJs.contains("com.example"))
        assertTrue(runJs.contains("MyTest"))
        assertTrue(runJs.contains("testMethod"))
    }

    @Test
    fun `index js lists every run in the same root directory`() {
        val rootDir = File(tempDir, "reports")

        TDHtmlReportWriter(
            runName = "run1",
            rootDirectory = rootDir,
            snapshotRootDirectory = File(tempDir, "snapshots")
        ).close()

        TDHtmlReportWriter(
            runName = "run2",
            rootDirectory = rootDir,
            snapshotRootDirectory = File(tempDir, "snapshots")
        ).close()

        val content = File(rootDir, "index.js").readText()
        assertTrue(content.contains("run1"))
        assertTrue(content.contains("run2"))
    }
}
