package io.github.cdsap.td.paparazzi

import app.cash.paparazzi.SnapshotHandler
import app.cash.paparazzi.SnapshotVerifier

class TDPaparazziHandlerProvider {
    fun determineHandler(maxPercentDifference: Double): SnapshotHandler =
        if (System.getProperty("paparazzi.test.verify")?.toBoolean() == true) {
            SnapshotVerifier(maxPercentDifference)
        } else {
            TDHtmlReportWriter()
        }
}
