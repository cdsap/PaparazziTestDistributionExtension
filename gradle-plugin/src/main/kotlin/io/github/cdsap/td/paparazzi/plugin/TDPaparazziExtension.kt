package io.github.cdsap.td.paparazzi.plugin

import org.gradle.api.provider.Property

abstract class TDPaparazziExtension {
    /** Directory containing the individual td-* report folders. Defaults to "build/reports/paparazzi". */
    abstract val inputReportDir: Property<String>

    /** Directory where the merged report is written. Defaults to "build/reports/paparazzi-td". */
    abstract val outputReportDir: Property<String>

    /**
     * Version of the `io.github.cdsap:td-paparazzi-ext` library to add as testImplementation.
     * Defaults to the plugin version.
     *
     * Note: this does NOT control the Paparazzi version. The Paparazzi version is determined
     * by the `app.cash.paparazzi` plugin applied in your project. The td-paparazzi-ext library
     * uses `compileOnly` for its Paparazzi dependency, so it will use whatever version your
     * project provides.
     */
    abstract val libraryVersion: Property<String>
}
