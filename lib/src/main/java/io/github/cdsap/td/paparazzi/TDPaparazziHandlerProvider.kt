package io.github.cdsap.td.paparazzi

import app.cash.paparazzi.SnapshotHandler
import app.cash.paparazzi.SnapshotVerifier
import java.io.File

class TDPaparazziHandlerProvider {
    fun determineHandler(
        maxPercentDifference: Double,
        fileNameProvider: SnapshotFileNameProvider = DefaultSnapshotFileNameProvider
    ): SnapshotHandler =
        if (System.getProperty("paparazzi.test.verify")?.toBoolean() == true) {
            createSnapshotVerifier(maxPercentDifference)
        } else {
            TDHtmlReportWriter(fileNameProvider = fileNameProvider)
        }

    private fun createSnapshotVerifier(maxPercentDifference: Double): SnapshotVerifier {
        // The SnapshotVerifier constructor signature changed across Paparazzi versions.
        // Try the single-arg constructor first (alpha04+), then fall back to the two-arg
        // variant (alpha02/alpha03) that requires an explicit snapshot directory.
        return try {
            SnapshotVerifier(maxPercentDifference)
        } catch (e: NoSuchMethodError) {
            val snapshotDir = File(System.getProperty("paparazzi.snapshot.dir", "src/test/snapshots"))
            val constructor = SnapshotVerifier::class.java.constructors.first {
                it.parameterCount == 2
                    && it.parameterTypes[0] == Double::class.javaPrimitiveType
                    && it.parameterTypes[1] == File::class.java
            }
            constructor.newInstance(maxPercentDifference, snapshotDir) as SnapshotVerifier
        }
    }
}
