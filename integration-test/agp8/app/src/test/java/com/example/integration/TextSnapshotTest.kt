package com.example.integration

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.Paparazzi
import io.github.cdsap.td.paparazzi.tdSnapshotHandler
import org.junit.Rule
import org.junit.Test

class TextSnapshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        snapshotHandler = tdSnapshotHandler()
    )

    @Test
    fun shortText() {
        paparazzi.snapshot {
            Container { Text("Hello") }
        }
    }

    @Test
    fun longText() {
        paparazzi.snapshot {
            Container {
                Text("The quick brown fox jumps over the lazy dog.")
            }
        }
    }

    @Composable
    private fun Container(content: @Composable () -> Unit) {
        Box(
            Modifier
                .background(Color.White)
                .fillMaxSize()
                .padding(16.dp)
        ) { content() }
    }
}
