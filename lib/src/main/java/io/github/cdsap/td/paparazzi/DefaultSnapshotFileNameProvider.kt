@file:Suppress("DEPRECATION")

package io.github.cdsap.td.paparazzi

import app.cash.paparazzi.Snapshot
import java.util.Locale

/**
 * Default file name provider, retained for source compatibility. Paparazzi's
 * own `Snapshot.toFileName` is what actually produces golden filenames now;
 * this implementation is no longer used internally.
 */
@Deprecated(
    "Filename generation is handled by Paparazzi; this implementation is no longer used.",
    level = DeprecationLevel.WARNING
)
object DefaultSnapshotFileNameProvider : SnapshotFileNameProvider {
    override fun toFileName(snapshot: Snapshot, delimiter: String, extension: String): String {
        val formattedLabel = if (snapshot.name != null) {
            "$delimiter${snapshot.name!!.lowercase(Locale.US).replace("\\s".toRegex(), delimiter)}"
        } else {
            ""
        }
        return "${snapshot.testName.packageName}${delimiter}${snapshot.testName.className}${delimiter}${snapshot.testName.methodName.replace("\\s".toRegex(), delimiter)}$formattedLabel.$extension"
    }
}
