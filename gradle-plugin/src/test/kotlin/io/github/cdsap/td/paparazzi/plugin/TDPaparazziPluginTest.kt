package io.github.cdsap.td.paparazzi.plugin

import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TDPaparazziPluginTest {

    @Test
    fun `plugin registers extension`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("io.github.cdsap.td.paparazzi")
        assertNotNull(project.extensions.findByName("tdPaparazzi"))
    }

    @Test
    fun `plugin applies without errors when paparazzi is not present`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("io.github.cdsap.td.paparazzi")
        // Should not throw - plugin waits for paparazzi to be applied
    }

    @Test
    fun `extension has configurable libraryVersion`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("io.github.cdsap.td.paparazzi")
        val extension = project.extensions.getByType(TDPaparazziExtension::class.java)
        extension.libraryVersion.set("0.2.1")
        assertTrue(extension.libraryVersion.get() == "0.2.1")
    }

    @Test
    fun `default library version matches plugin version`() {
        assertTrue(TDPaparazziPlugin.DEFAULT_LIBRARY_VERSION == "0.3.1")
    }
}
