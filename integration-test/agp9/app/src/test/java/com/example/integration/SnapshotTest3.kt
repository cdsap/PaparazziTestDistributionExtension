package com.example.integration

import app.cash.paparazzi.Paparazzi
import io.github.cdsap.td.paparazzi.tdSnapshotHandler
import org.junit.Rule
import org.junit.Test

class SnapshotTest3 {
    @get:Rule
    val paparazzi = Paparazzi(
        snapshotHandler = tdSnapshotHandler()
    )

    @Test
    fun snapshot() {
        paparazzi.snapshot { SampleComposable() }
    }
}
