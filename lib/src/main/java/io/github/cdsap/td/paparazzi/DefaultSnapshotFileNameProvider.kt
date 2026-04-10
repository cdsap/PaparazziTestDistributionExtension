package io.github.cdsap.td.paparazzi

import app.cash.paparazzi.Snapshot
import java.util.Locale

/**
 * Default file name provider that matches the naming convention used by this library.
 *
 * Format: `{packageName}{delimiter}{className}{delimiter}{methodName}[{delimiter}{label}].{extension}`
 *
 * The snapshot [Snapshot.name] (if present) is lowercased and whitespace is replaced with the delimiter.
 */
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
