package io.github.cdsap.td.paparazzi

import app.cash.paparazzi.Snapshot
import app.cash.paparazzi.TestName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.Date

class TDPaparazziJsonTest {

    @Test
    fun `testNameToJson formats correctly`() {
        val testName = TestName("com.example", "MyClass", "myMethod")
        val json = TDPaparazziJson.testNameToJson(testName)
        assertEquals("com.example.MyClass#myMethod", json)
    }

    @Test
    fun `testNameFromJson parses correctly`() {
        val json = "com.example.MyClass#myMethod"
        val testName = TDPaparazziJson.testNameFromJson(json)
        assertEquals("com.example", testName.packageName)
        assertEquals("MyClass", testName.className)
        assertEquals("myMethod", testName.methodName)
    }

    @Test
    fun `testNameFromJson handles nested packages`() {
        val json = "com.example.deep.package.MyClass#testMethod"
        val testName = TDPaparazziJson.testNameFromJson(json)
        assertEquals("com.example.deep.package", testName.packageName)
        assertEquals("MyClass", testName.className)
        assertEquals("testMethod", testName.methodName)
    }

    @Test
    fun `testNameFromJson throws on invalid format`() {
        assertThrows(NullPointerException::class.java) {
            TDPaparazziJson.testNameFromJson("invalid-format")
        }
    }

    @Test
    fun `testName roundtrip serialization`() {
        val original = TestName("com.example.app", "ScreenTest", "testLoading")
        val json = TDPaparazziJson.testNameToJson(original)
        val parsed = TDPaparazziJson.testNameFromJson(json)
        assertEquals(original.packageName, parsed.packageName)
        assertEquals(original.className, parsed.className)
        assertEquals(original.methodName, parsed.methodName)
    }

    @Test
    fun `listOfShotsAdapter serializes snapshots`() {
        val snapshot = Snapshot(
            name = "loading",
            testName = TestName("com.example", "MyTest", "testSnapshot"),
            timestamp = Date(0),
            file = "images/abc123.png"
        )
        val json = TDPaparazziJson.listOfShotsAdapter.toJson(listOf(snapshot))
        assertNotNull(json)
        assert(json.contains("loading"))
        assert(json.contains("com.example.MyTest#testSnapshot"))
        assert(json.contains("images/abc123.png"))
    }

    @Test
    fun `listOfStringsAdapter serializes run names`() {
        val runs = listOf("20190319153912_aaab", "20190319153917_bcfe")
        val json = TDPaparazziJson.listOfStringsAdapter.toJson(runs)
        assertNotNull(json)
        assert(json.contains("20190319153912_aaab"))
        assert(json.contains("20190319153917_bcfe"))
    }

    @Test
    fun `moshi instance is not null`() {
        assertNotNull(TDPaparazziJson.moshi)
    }
}
