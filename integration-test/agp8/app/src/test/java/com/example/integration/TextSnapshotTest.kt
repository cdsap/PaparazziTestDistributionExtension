package com.example.integration

import org.junit.Rule
import org.junit.Test

class TextSnapshotTest {
    @get:Rule
    val paparazzi = paparazzi()

    @Test
    fun shortText() {
        paparazzi.snapshot { LabelBox("Hi") }
    }

    @Test
    fun mediumText() {
        paparazzi.snapshot { LabelBox("Hello, world!") }
    }

    @Test
    fun longText() {
        paparazzi.snapshot { LabelBox("This is a much longer string used for the snapshot.") }
    }

    @Test
    fun emptyText() {
        paparazzi.snapshot { LabelBox("") }
    }

    @Test
    fun numericText() {
        paparazzi.snapshot { LabelBox("1234567890") }
    }
}
