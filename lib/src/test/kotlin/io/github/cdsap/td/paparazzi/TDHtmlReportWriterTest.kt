package io.github.cdsap.td.paparazzi

import app.cash.paparazzi.Snapshot
import app.cash.paparazzi.TestName
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
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
    }

    @Test
    fun `constructor creates required directories`() {
        val rootDir = File(tempDir, "reports")
        val writer = TDHtmlReportWriter(
            runName = "testrun",
            rootDirectory = rootDir,
            snapshotRootDirectory = File(tempDir, "snapshots")
        )

        assertTrue(File(rootDir, "runs").exists())
        assertTrue(File(rootDir, "images").exists())
        assertTrue(File(rootDir, "videos").exists())
    }

    @Test
    fun `constructor writes initial run js file`() {
        val rootDir = File(tempDir, "reports")
        val writer = TDHtmlReportWriter(
            runName = "testrun",
            rootDirectory = rootDir,
            snapshotRootDirectory = File(tempDir, "snapshots")
        )

        val runJs = File(rootDir, "runs/testrun.js")
        assertTrue(runJs.exists())
        val content = runJs.readText()
        assertTrue(content.startsWith("window.runs[\"testrun\"] = "))
        assertTrue(content.endsWith(";"))
    }

    @Test
    fun `constructor writes index js file`() {
        val rootDir = File(tempDir, "reports")
        val writer = TDHtmlReportWriter(
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
    fun `newFrameHandler writes image and records shot`() {
        val rootDir = File(tempDir, "reports")
        val writer = TDHtmlReportWriter(
            runName = "testrun",
            rootDirectory = rootDir,
            snapshotRootDirectory = File(tempDir, "snapshots")
        )

        val snapshot = Snapshot(
            name = "test-shot",
            testName = TestName("com.example", "MyTest", "testMethod"),
            timestamp = Date(),
            file = null
        )

        val frameHandler = writer.newFrameHandler(snapshot, 1, 1)
        val image = BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB)
        frameHandler.handle(image)
        frameHandler.close()

        val images = File(rootDir, "images").listFiles()
        assertNotNull(images)
        assertTrue(images!!.any { it.name.endsWith(".png") })
    }

    @Test
    fun `close writes final run js with shots`() {
        val rootDir = File(tempDir, "reports")
        val writer = TDHtmlReportWriter(
            runName = "testrun",
            rootDirectory = rootDir,
            snapshotRootDirectory = File(tempDir, "snapshots")
        )

        val snapshot = Snapshot(
            name = "test-shot",
            testName = TestName("com.example", "MyTest", "testMethod"),
            timestamp = Date(),
            file = null
        )

        val frameHandler = writer.newFrameHandler(snapshot, 1, 1)
        frameHandler.handle(BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB))
        frameHandler.close()

        writer.close()

        val runJs = File(rootDir, "runs/testrun.js")
        val content = runJs.readText()
        assertTrue(content.contains("test-shot"))
        assertTrue(content.contains("com.example.MyTest#testMethod"))
    }

    @Test
    fun `multiple frames create a video`() {
        val rootDir = File(tempDir, "reports")
        val writer = TDHtmlReportWriter(
            runName = "testrun",
            rootDirectory = rootDir,
            snapshotRootDirectory = File(tempDir, "snapshots")
        )

        val snapshot = Snapshot(
            name = "animation",
            testName = TestName("com.example", "MyTest", "testAnimation"),
            timestamp = Date(),
            file = null
        )

        val frameHandler = writer.newFrameHandler(snapshot, 3, 30)
        // Create distinct frames
        for (i in 0 until 3) {
            val image = BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB)
            val g = image.createGraphics()
            g.fillRect(i * 10, 0, 10, 10)
            g.dispose()
            frameHandler.handle(image)
        }
        frameHandler.close()

        val videos = File(rootDir, "videos").listFiles()
        assertNotNull(videos)
        assertTrue(videos!!.any { it.name.endsWith(".mov") })
    }

    @Test
    fun `duplicate images are deduplicated by hash`() {
        val rootDir = File(tempDir, "reports")
        val writer = TDHtmlReportWriter(
            runName = "testrun",
            rootDirectory = rootDir,
            snapshotRootDirectory = File(tempDir, "snapshots")
        )

        val image = BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB)

        val snapshot1 = Snapshot(
            name = "shot1",
            testName = TestName("com.example", "MyTest", "test1"),
            timestamp = Date(),
            file = null
        )
        val snapshot2 = Snapshot(
            name = "shot2",
            testName = TestName("com.example", "MyTest", "test2"),
            timestamp = Date(),
            file = null
        )

        val handler1 = writer.newFrameHandler(snapshot1, 1, 1)
        handler1.handle(image)
        handler1.close()

        val handler2 = writer.newFrameHandler(snapshot2, 1, 1)
        handler2.handle(image)
        handler2.close()

        // Same image content should produce only one file
        val images = File(rootDir, "images").listFiles()!!.filter { it.name.endsWith(".png") }
        assertEquals(1, images.size)
    }

    @Test
    fun `recording mode copies images to golden directory`() {
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

        val frameHandler = writer.newFrameHandler(snapshot, 1, 1)
        frameHandler.handle(BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB))
        frameHandler.close()

        val goldenImages = File(snapshotDir, "images")
        assertTrue(goldenImages.exists())
        assertTrue(goldenImages.listFiles()!!.any { it.name.endsWith(".png") })
    }

    @Test
    fun `index js lists all runs`() {
        val rootDir = File(tempDir, "reports")

        // Create first writer/run
        val writer1 = TDHtmlReportWriter(
            runName = "run1",
            rootDirectory = rootDir,
            snapshotRootDirectory = File(tempDir, "snapshots")
        )
        writer1.close()

        // Create second writer/run (reuses same rootDirectory, so it sees run1's files)
        val writer2 = TDHtmlReportWriter(
            runName = "run2",
            rootDirectory = rootDir,
            snapshotRootDirectory = File(tempDir, "snapshots")
        )
        writer2.close()

        val indexJs = File(rootDir, "index.js")
        val content = indexJs.readText()
        assertTrue(content.contains("run1"))
        assertTrue(content.contains("run2"))
    }
}
