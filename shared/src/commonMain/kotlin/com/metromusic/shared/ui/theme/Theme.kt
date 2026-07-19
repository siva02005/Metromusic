package com.metromusic.shared.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val MetroPurple = Color(0xFF6C63FF)
val MetroPink = Color(0xFFFF6B9D)
val MetroDark = Color(0xFF1A1A2E)
val MetroSurface = Color(0xFF16213E)
val MetroCard = Color(0xFF0F3460)

private val DarkScheme = darkColorScheme(
    primary = MetroPurple,
    secondary = MetroPink,
    tertiary = Color(0xFFCF6679),
    background = MetroDark,
    surface = MetroSurface,
    surfaceVariant = MetroCard,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFB3B3B3),
    outline = Color(0xFF404040)
)

@Composable
fun MetromusicTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkScheme,
        content = content
    )
}
