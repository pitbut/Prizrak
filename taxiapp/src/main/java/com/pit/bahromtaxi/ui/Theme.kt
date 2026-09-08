package com.pit.bahromtaxi.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BahromTaxiColors = darkColorScheme(
    primary = Color(0xFFFFC400),
    onPrimary = Color(0xFF1A1A1A),
    secondary = Color(0xFF2E7D32),
    error = Color(0xFFFF6B6B),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E)
)

@Composable
fun BahromTaxiTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = BahromTaxiColors, content = content)
}
