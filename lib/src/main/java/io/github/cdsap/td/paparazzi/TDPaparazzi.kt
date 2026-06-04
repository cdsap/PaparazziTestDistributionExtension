@file:Suppress("DEPRECATION")

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
 * @param fileNameProvider ignored; retained for source compatibility. Golden
 *   filenames are now produced by Paparazzi itself.
 */
@JvmOverloads
fun tdSnapshotHandler(
    maxPercentDifference: Double = 0.0,
    @Suppress("UNUSED_PARAMETER")
    fileNameProvider: SnapshotFileNameProvider = DefaultSnapshotFileNameProvider
): SnapshotHandler =
    TDPaparazziHandlerProvider().determineHandler(maxPercentDifference)
