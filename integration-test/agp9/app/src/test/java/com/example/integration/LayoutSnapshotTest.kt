package com.example.integration

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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

class LayoutSnapshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        snapshotHandler = tdSnapshotHandler()
    )

    @Test
    fun columnLayout() {
        paparazzi.snapshot {
            Container {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Row 1")
                    Text("Row 2")
                    Text("Row 3")
                }
            }
        }
    }

    @Test
    fun rowLayout() {
        paparazzi.snapshot {
            Container {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("A")
                    Text("B")
                    Text("C")
                }
            }
        }
    }

    @Composable
    private fun Container(content: @Composable () -> Unit) {
        Column(
            Modifier
                .background(Color.White)
                .fillMaxSize()
                .padding(16.dp)
        ) { content() }
    }
}
