package io.github.cdsap.td.paparazzi

import app.cash.paparazzi.Snapshot

/**
 * Strategy interface for generating snapshot file names.
 *
 * Consumers can implement this to match the file naming convention used by
 * their version of Paparazzi, avoiding mismatches when comparing baseline images.
 */
fun interface SnapshotFileNameProvider {
    fun toFileName(snapshot: Snapshot, delimiter: String, extension: String): String
}
