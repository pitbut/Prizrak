package com.pit.prizrak.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PrizrakColors = darkColorScheme(
    primary = Color(0xFF7FE7DC),
    secondary = Color(0xFF3A7CA5),
    background = Color.Black,
    surface = Color(0xFF10151B)
)

@Composable
fun PrizrakTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = PrizrakColors, content = content)
}
