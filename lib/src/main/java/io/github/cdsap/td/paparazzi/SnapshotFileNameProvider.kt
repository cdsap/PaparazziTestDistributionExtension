package io.github.cdsap.td.paparazzi

import app.cash.paparazzi.Snapshot

/**
 * Strategy interface for generating snapshot file names.
 *
 * No longer used internally: golden filenames are now produced by Paparazzi's
 * own `Snapshot.toFileName`, so this interface and any implementations passed
 * to [tdSnapshotHandler] are ignored. Retained for source compatibility.
 */
@Deprecated(
    "Filename generation is handled by Paparazzi; this interface is no longer used.",
    level = DeprecationLevel.WARNING
)
fun interface SnapshotFileNameProvider {
    fun toFileName(snapshot: Snapshot, delimiter: String, extension: String): String
}
