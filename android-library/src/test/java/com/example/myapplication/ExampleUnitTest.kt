package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import app.cash.paparazzi.Paparazzi
import io.github.cdsap.td.paparazzi.TDPaparazziHandlerProvider
import org.junit.Test

import org.junit.Assert.*
import org.junit.Rule

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @get:Rule
    val paparazzi = Paparazzi(
        snapshotHandler = TDPaparazziHandlerProvider().determineHandler(0.1)
    )

    @Test
    fun compose() {
        paparazzi.snapshot { HelloPaparazzi() }
    }
}
@Suppress("TestFunctionName")
@Composable
fun HelloPaparazzi() {
    val text = "Hello, Paparazzi"
    Column(
        Modifier
            .background(Color.White)
            .fillMaxSize()
            .wrapContentSize()
    ) {
        Text(text)
        Text(text, style = TextStyle(fontFamily = FontFamily.Cursive))
        Text(
            text = text,
            style = TextStyle(textDecoration = TextDecoration.LineThrough)
        )
        Text(
            text = text,
            style = TextStyle(textDecoration = TextDecoration.Underline)
        )
        Text(
            text = text,
            style = TextStyle(
                textDecoration = TextDecoration.combine(
                    listOf(
                        TextDecoration.Underline,
                        TextDecoration.LineThrough
                    )
                ),
                fontWeight = FontWeight.Bold
            )
        )
    }
}
