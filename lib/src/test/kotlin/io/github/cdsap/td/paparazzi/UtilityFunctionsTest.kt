package io.github.cdsap.td.paparazzi

import app.cash.paparazzi.Snapshot
import app.cash.paparazzi.TestName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Date

class UtilityFunctionsTest {

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
    fun `sanitizeForFilename replaces uppercase with lowercase`() {
        assertEquals("hello", "HELLO".sanitizeForFilename())
    }

    @Test
    fun `sanitizeForFilename replaces spaces with underscores`() {
        assertEquals("hello_world", "hello world".sanitizeForFilename())
    }

    @Test
    fun `sanitizeForFilename preserves safe characters`() {
        assertEquals("abc-123_test.file", "abc-123_test.file".sanitizeForFilename())
    }

    @Test
    fun `sanitizeForFilename replaces special characters`() {
        assertEquals("test_file", "test!file".sanitizeForFilename())
    }

    @Test
    fun `filenameSafeChars allows expected characters`() {
        assertTrue(filenameSafeChars.matches('a'))
        assertTrue(filenameSafeChars.matches('z'))
        assertTrue(filenameSafeChars.matches('0'))
        assertTrue(filenameSafeChars.matches('9'))
        assertTrue(filenameSafeChars.matches('_'))
        assertTrue(filenameSafeChars.matches('-'))
        assertTrue(filenameSafeChars.matches('.'))
    }

    @Test
    fun `filenameSafeChars rejects unsafe characters`() {
        assertTrue(!filenameSafeChars.matches('A'))
        assertTrue(!filenameSafeChars.matches(' '))
        assertTrue(!filenameSafeChars.matches('!'))
        assertTrue(!filenameSafeChars.matches('/'))
    }

    @Test
    fun `toFileName formats single image snapshot correctly`() {
        val snapshot = Snapshot(
            name = "loading",
            testName = TestName("com.example", "MyTest", "testMethod"),
            timestamp = Date(),
            file = null
        )
        val fileName = snapshot.toFileName("_", "png")
        assertEquals("com.example_MyTest_testMethod_loading.png", fileName)
    }

    @Test
    fun `toFileName with null name omits label`() {
        val snapshot = Snapshot(
            name = null,
            testName = TestName("com.example", "MyTest", "testMethod"),
            timestamp = Date(),
            file = null
        )
        val fileName = snapshot.toFileName("_", "png")
        assertEquals("com.example_MyTest_testMethod.png", fileName)
    }

    @Test
    fun `toFileName with custom delimiter`() {
        val snapshot = Snapshot(
            name = "my snapshot",
            testName = TestName("com.example", "MyTest", "testMethod"),
            timestamp = Date(),
            file = null
        )
        val fileName = snapshot.toFileName("-", "png")
        assertEquals("com.example-MyTest-testMethod-my-snapshot.png", fileName)
    }

    @Test
    fun `toFileName with video extension`() {
        val snapshot = Snapshot(
            name = "animation",
            testName = TestName("com.example", "MyTest", "testMethod"),
            timestamp = Date(),
            file = null
        )
        val fileName = snapshot.toFileName("_", "mov")
        assertEquals("com.example_MyTest_testMethod_animation.mov", fileName)
    }
}
