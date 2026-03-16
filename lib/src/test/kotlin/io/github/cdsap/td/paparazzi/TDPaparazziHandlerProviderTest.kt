package io.github.cdsap.td.paparazzi

import app.cash.paparazzi.SnapshotVerifier
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class TDPaparazziHandlerProviderTest {

    @TempDir
    lateinit var tempDir: File

    @AfterEach
    fun cleanup() {
        System.clearProperty("paparazzi.test.verify")
        System.clearProperty("paparazzi.build.dir")
    }

    @Test
    fun `returns TDHtmlReportWriter when verify property is not set`() {
        System.clearProperty("paparazzi.test.verify")
        System.setProperty("paparazzi.build.dir", tempDir.absolutePath)
        val provider = TDPaparazziHandlerProvider()
        val handler = provider.determineHandler(0.0)
        assertTrue(handler is TDHtmlReportWriter)
    }

    @Test
    fun `returns TDHtmlReportWriter when verify property is false`() {
        System.setProperty("paparazzi.test.verify", "false")
        System.setProperty("paparazzi.build.dir", tempDir.absolutePath)
        val provider = TDPaparazziHandlerProvider()
        val handler = provider.determineHandler(0.0)
        assertTrue(handler is TDHtmlReportWriter)
    }

    @Test
    fun `returns SnapshotVerifier when verify property is true`() {
        System.setProperty("paparazzi.test.verify", "true")
        System.setProperty("paparazzi.snapshot.dir", tempDir.absolutePath)
        try {
            val provider = TDPaparazziHandlerProvider()
            val handler = provider.determineHandler(0.1)
            assertTrue(handler is SnapshotVerifier)
        } finally {
            System.clearProperty("paparazzi.snapshot.dir")
        }
    }
}
