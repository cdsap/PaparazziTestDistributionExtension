package com.example.integration

import app.cash.paparazzi.Paparazzi
import io.github.cdsap.td.paparazzi.tdSnapshotHandler

/**
 * Builds the Paparazzi rule used by every test class.
 *
 * When the `td.enabled` system property is `true` (set from the Gradle property
 * of the same name), the rule uses the TD Paparazzi extension's snapshot handler.
 * Otherwise it falls back to Paparazzi's default writer — i.e. vanilla Paparazzi,
 * no TD extension at all.
 */
internal fun paparazzi(): Paparazzi =
    if (System.getProperty("td.enabled", "false").toBoolean()) {
        Paparazzi(snapshotHandler = tdSnapshotHandler())
    } else {
        Paparazzi()
    }
