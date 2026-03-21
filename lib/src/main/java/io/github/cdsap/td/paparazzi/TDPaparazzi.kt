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
 */
fun tdSnapshotHandler(maxPercentDifference: Double = 0.0): SnapshotHandler =
    TDPaparazziHandlerProvider().determineHandler(maxPercentDifference)
