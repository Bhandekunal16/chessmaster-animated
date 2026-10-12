package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Amber400,
    onPrimary = Slate900,
    primaryContainer = Slate800,
    onPrimaryContainer = Amber400,
    secondary = Cyan500,
    onSecondary = Slate900,
    secondaryContainer = Slate800,
    onSecondaryContainer = Cyan500,
    tertiary = Emerald500,
    background = Color(0xFF0A0F1D),
    onBackground = Slate50,
    surface = Slate800,
    onSurface = Slate50,
    surfaceVariant = Slate700,
    onSurfaceVariant = Slate200,
    error = Rose500,
    onError = Color.White
)

private val LightColorScheme = DarkColorScheme // Modern chess app embraces rich dark theme by default

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
