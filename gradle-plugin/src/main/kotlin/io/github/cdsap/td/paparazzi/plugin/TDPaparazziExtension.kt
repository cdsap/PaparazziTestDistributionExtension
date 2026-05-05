package io.github.cdsap.td.paparazzi.plugin

import org.gradle.api.provider.Property

abstract class TDPaparazziExtension {
    /**
     * Directory containing the individual `td-*` report folders. Defaults to
     * `build/reports/paparazzi`.
     *
     * The plugin sets this as the `paparazzi.td.report.dir` system property on the
     * variant's unit-test task, so `TDHtmlReportWriter` writes the per-execution
     * `td-<timestamp>` folders here. The merge task also reads from this directory,
     * keeping the writer and the merger in sync.
     */
    abstract val inputReportDir: Property<String>

    /** Directory where the merged report is written. Defaults to "build/reports/paparazzi-td". */
    abstract val outputReportDir: Property<String>

    /** Whether to delete the temporary td-* directories after merging. Defaults to false. */
    abstract val cleanupTdDirectories: Property<Boolean>

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
