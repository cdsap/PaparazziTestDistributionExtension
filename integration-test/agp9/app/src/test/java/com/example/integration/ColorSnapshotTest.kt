package com.example.integration

import androidx.compose.ui.graphics.Color
import app.cash.paparazzi.Paparazzi
import io.github.cdsap.td.paparazzi.tdSnapshotHandler
import org.junit.Rule
import org.junit.Test

class ColorSnapshotTest {
    @get:Rule
    val paparazzi = Paparazzi(snapshotHandler = tdSnapshotHandler())

    @Test
    fun red() {
        paparazzi.snapshot { ColoredBox(Color.Red, "Red") }
    }

    @Test
    fun green() {
        paparazzi.snapshot { ColoredBox(Color.Green, "Green") }
    }

    @Test
    fun blue() {
        paparazzi.snapshot { ColoredBox(Color.Blue, "Blue") }
    }

    @Test
    fun yellow() {
        paparazzi.snapshot { ColoredBox(Color.Yellow, "Yellow") }
    }

    @Test
    fun magenta() {
        paparazzi.snapshot { ColoredBox(Color.Magenta, "Magenta") }
    }

    @Test
    fun cyan() {
        paparazzi.snapshot { ColoredBox(Color.Cyan, "Cyan") }
    }

    @Test
    fun gray() {
        paparazzi.snapshot { ColoredBox(Color.Gray, "Gray") }
    }

    @Test
    fun black() {
        paparazzi.snapshot { ColoredBox(Color.Black, "Black") }
    }
}
