package com.example.integration

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun SampleComposable() {
    Box(
        Modifier
            .background(Color.White)
            .fillMaxSize()
    ) {
        Text("Integration Test")
    }
}
