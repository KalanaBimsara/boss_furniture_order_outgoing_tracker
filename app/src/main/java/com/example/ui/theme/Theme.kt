package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Indigo600,
    onPrimary = Color.White,
    secondary = Slate400,
    onSecondary = Color.Black,
    tertiary = Amber700,
    background = Slate900,
    surface = Slate800,
    onBackground = Color.White,
    onSurface = Slate100,
    outline = Slate500,
    surfaceVariant = Slate800,
    onSurfaceVariant = Slate400
)

private val LightColorScheme = lightColorScheme(
    primary = Indigo600,
    onPrimary = Color.White,
    secondary = Slate500,
    onSecondary = Color.White,
    tertiary = Amber700,
    background = Slate50,
    surface = Color.White,
    onBackground = Slate900,
    onSurface = Slate800,
    outline = Slate200,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate500
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disable dynamic colors to strictly enforce the brand look and feel!
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme


  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
