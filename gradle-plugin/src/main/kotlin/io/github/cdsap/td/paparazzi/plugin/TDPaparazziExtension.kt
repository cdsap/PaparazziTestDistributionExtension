package io.github.cdsap.td.paparazzi.plugin

import org.gradle.api.provider.Property

abstract class TDPaparazziExtension {
    /** Directory containing the individual td-* report folders. Defaults to "build/reports/paparazzi". */
    abstract val inputReportDir: Property<String>

    /** Directory where the merged report is written. Defaults to "build/reports/paparazzi-td". */
    abstract val outputReportDir: Property<String>

    /** Version of the td-paparazzi-ext library to add as testImplementation. Defaults to the plugin version. */
    abstract val libraryVersion: Property<String>
}
