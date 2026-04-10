package io.github.cdsap.td.paparazzi

import app.cash.paparazzi.SnapshotHandler

/**
 * Returns a Test Distribution-compatible [SnapshotHandler] that can be passed
 * directly to the Paparazzi constructor.
 *
 * This decouples the library from the Paparazzi constructor signature, allowing
 * it to work with any Paparazzi version.
 *
 * Usage:
 * ```
 * @get:Rule
 * val paparazzi = Paparazzi(
 *     snapshotHandler = tdSnapshotHandler()
 * )
 * ```
 *
 * @param maxPercentDifference threshold for snapshot verification mode
 * @param fileNameProvider strategy for generating snapshot file names.
 *   Supply a custom implementation to match the naming convention of your
 *   Paparazzi version and avoid snapshot filename mismatches.
 */
fun tdSnapshotHandler(
    maxPercentDifference: Double = 0.0,
    fileNameProvider: SnapshotFileNameProvider = DefaultSnapshotFileNameProvider
): SnapshotHandler =
    TDPaparazziHandlerProvider().determineHandler(maxPercentDifference, fileNameProvider)
