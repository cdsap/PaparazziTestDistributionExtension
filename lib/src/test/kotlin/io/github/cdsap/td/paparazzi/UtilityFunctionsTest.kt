@file:Suppress("DEPRECATION")

package io.github.cdsap.td.paparazzi

import app.cash.paparazzi.Snapshot
import app.cash.paparazzi.TestName
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Date

class UtilityFunctionsTest {

    @AfterEach
    fun clearReportDirProperties() {
        System.clearProperty("paparazzi.td.report.dir")
        System.clearProperty("paparazzi.build.dir")
    }

    @Test
    fun `defaultReportParentDir uses paparazzi td report dir when set`() {
        System.setProperty("paparazzi.td.report.dir", "/tmp/my-td-reports")
        assertEquals("/tmp/my-td-reports", defaultReportParentDir())
    }

    @Test
    fun `defaultReportParentDir prefers paparazzi td report dir over paparazzi build dir`() {
        System.setProperty("paparazzi.td.report.dir", "/tmp/my-td-reports")
        System.setProperty("paparazzi.build.dir", "/tmp/some-build")
        assertEquals("/tmp/my-td-reports", defaultReportParentDir())
    }

    @Test
    fun `defaultReportParentDir falls back to paparazzi build dir`() {
        System.setProperty("paparazzi.build.dir", "custom-build")
        assertEquals("custom-build/reports/paparazzi", defaultReportParentDir())
    }

    @Test
    fun `defaultReportParentDir defaults to build reports paparazzi`() {
        assertEquals("build/reports/paparazzi", defaultReportParentDir())
    }

    @Test
    fun `defaultReportParentDir treats blank paparazzi td report dir as unset`() {
        System.setProperty("paparazzi.td.report.dir", "   ")
        assertEquals("build/reports/paparazzi", defaultReportParentDir())
    }

    @Test
    fun `defaultRunName has expected format`() {
        val name = defaultRunName()
        assertNotNull(name)
        // Format: yyyyMMddHHmmss_xxxxxx (14 digits + underscore + 6 char UUID)
        assertTrue(name.matches(Regex("\\d{14}_[a-f0-9\\-]{6}")))
    }

    @Test
    fun `defaultRunName generates unique values`() {
        val name1 = defaultRunName()
        val name2 = defaultRunName()
        // UUID portion should differ
        assertTrue(name1 != name2 || name1.substring(15) != name2.substring(15))
    }

    @Test
    fun `DefaultSnapshotFileNameProvider formats single image snapshot correctly`() {
        val snapshot = Snapshot(
            name = "loading",
            testName = TestName("com.example", "MyTest", "testMethod"),
            timestamp = Date(),
            file = null
        )
        val fileName = DefaultSnapshotFileNameProvider.toFileName(snapshot, "_", "png")
        assertEquals("com.example_MyTest_testMethod_loading.png", fileName)
    }

    @Test
    fun `DefaultSnapshotFileNameProvider with null name omits label`() {
        val snapshot = Snapshot(
            name = null,
            testName = TestName("com.example", "MyTest", "testMethod"),
            timestamp = Date(),
            file = null
        )
        val fileName = DefaultSnapshotFileNameProvider.toFileName(snapshot, "_", "png")
        assertEquals("com.example_MyTest_testMethod.png", fileName)
    }

    @Test
    fun `custom SnapshotFileNameProvider can still be implemented`() {
        val customProvider = SnapshotFileNameProvider { snapshot, _, extension ->
            "${snapshot.testName.className}.${snapshot.testName.methodName}.$extension"
        }
        val snapshot = Snapshot(
            name = "loading",
            testName = TestName("com.example", "MyTest", "testMethod"),
            timestamp = Date(),
            file = null
        )
        val fileName = customProvider.toFileName(snapshot, "_", "png")
        assertEquals("MyTest.testMethod.png", fileName)
    }
}
