package com.example.integration

import android.widget.ProgressBar
import app.cash.paparazzi.Paparazzi
import io.github.cdsap.td.paparazzi.tdSnapshotHandler
import org.junit.Rule
import org.junit.Test

class SnapshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        snapshotHandler = tdSnapshotHandler()
    )

    @Test
    fun snapshot() {
        paparazzi.snapshot { SampleComposable() }
    }

    @Test
    fun progressBarAnimation() {
        val view = ProgressBar(paparazzi.context).apply { isIndeterminate = true }
        paparazzi.gif(view, "progress_bar", start = 0L, end = 500L, fps = 30)
    }
}
