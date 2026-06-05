package com.example.integration

import android.widget.ProgressBar
import org.junit.Rule
import org.junit.Test

class GifAnimationTest {
    @get:Rule
    val paparazzi = paparazzi()

    @Test
    fun shortAnimation() {
        val view = ProgressBar(paparazzi.context).apply { isIndeterminate = true }
        paparazzi.gif(view, "short", start = 0L, end = 200L, fps = 30)
    }

    @Test
    fun longerAnimation() {
        val view = ProgressBar(paparazzi.context).apply { isIndeterminate = true }
        paparazzi.gif(view, "longer", start = 0L, end = 800L, fps = 30)
    }

    @Test
    fun highFpsAnimation() {
        val view = ProgressBar(paparazzi.context).apply { isIndeterminate = true }
        paparazzi.gif(view, "high_fps", start = 0L, end = 300L, fps = 60)
    }

    @Test
    fun lowFpsAnimation() {
        val view = ProgressBar(paparazzi.context).apply { isIndeterminate = true }
        paparazzi.gif(view, "low_fps", start = 0L, end = 500L, fps = 10)
    }
}
