@file:Suppress("DEPRECATION")

package io.github.cdsap.td.paparazzi

import app.cash.paparazzi.HtmlReportWriter
import app.cash.paparazzi.Snapshot
import app.cash.paparazzi.SnapshotHandler
import app.cash.paparazzi.SnapshotHandler.FrameHandler
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * SnapshotHandler that writes a Paparazzi HTML report into a unique per-process
 * directory (`td-<timestamp>/`) so that many Test Distribution agents running
 * on the same disk do not overwrite each other's reports. A later merge task
 * consolidates every `td-*` folder into the final report.
 *
 * Frame encoding, golden-file format, and report JSON are delegated to the
 * Paparazzi version on the classpath, so goldens written by this class are
 * byte-compatible with whatever Paparazzi version the consumer uses.
 */
class TDHtmlReportWriter @JvmOverloads constructor(
    runName: String = defaultRunName(),
    rootDirectory: File = File(
        defaultReportParentDir(),
        "td-${System.currentTimeMillis()}"
    ),
    snapshotRootDirectory: File = File(
        System.getProperty("paparazzi.snapshot.dir", "src/test/snapshots")
    ),
    @Suppress("UNUSED_PARAMETER")
    fileNameProvider: SnapshotFileNameProvider = DefaultSnapshotFileNameProvider
) : SnapshotHandler {

    private val delegate: SnapshotHandler = createUpstreamHandler(
        runName = runName,
        rootDirectory = rootDirectory.apply { mkdirs() },
        snapshotRootDirectory = snapshotRootDirectory
    )

    override fun newFrameHandler(
        snapshot: Snapshot,
        frameCount: Int,
        fps: Int
    ): FrameHandler = delegate.newFrameHandler(snapshot, frameCount, fps)

    override fun close() {
        delegate.close()
    }
}

/**
 * Builds an upstream HtmlReportWriter while tolerating the constructor change
 * between Paparazzi alpha02 and alpha03+. We compile-link against alpha02's
 * `(String, File, File)` signature; on alpha03+ that throws NoSuchMethodError
 * and we fall back to the public `(String, File, double)` overload, temporarily
 * setting `paparazzi.snapshot.dir` so the writer's default snapshot directory
 * resolves to the value the caller asked for.
 */
private fun createUpstreamHandler(
    runName: String,
    rootDirectory: File,
    snapshotRootDirectory: File
): SnapshotHandler = try {
    HtmlReportWriter(runName, rootDirectory, snapshotRootDirectory)
} catch (e: NoSuchMethodError) {
    val constructor = HtmlReportWriter::class.java.getDeclaredConstructor(
        String::class.java,
        File::class.java,
        Double::class.javaPrimitiveType
    )
    withSnapshotDirProperty(snapshotRootDirectory.absolutePath) {
        constructor.newInstance(runName, rootDirectory, 0.0) as SnapshotHandler
    }
}

private inline fun <T> withSnapshotDirProperty(value: String, block: () -> T): T {
    val key = "paparazzi.snapshot.dir"
    val previous = System.getProperty(key)
    System.setProperty(key, value)
    return try {
        block()
    } finally {
        if (previous == null) System.clearProperty(key) else System.setProperty(key, previous)
    }
}

internal fun defaultRunName(): String {
    val timestamp = SimpleDateFormat("yyyyMMddHHmmss", Locale.US).format(Date())
    val token = UUID.randomUUID().toString().substring(0, 6)
    return "${timestamp}_$token"
}

/**
 * Resolves the parent directory under which `td-<timestamp>` report folders are written.
 *
 * Resolution order:
 *  1. `paparazzi.td.report.dir` — the Gradle plugin sets this from `tdPaparazzi.inputReportDir`,
 *     so the merge task and the writer always agree on the location.
 *  2. `${paparazzi.build.dir or "build"}/reports/paparazzi` — backwards-compatible default for
 *     setups that don't apply the plugin.
 */
internal fun defaultReportParentDir(): String {
    System.getProperty("paparazzi.td.report.dir")?.takeIf { it.isNotBlank() }?.let { return it }
    val buildDir = System.getProperty("paparazzi.build.dir", "build")
    return "$buildDir/reports/paparazzi"
}
