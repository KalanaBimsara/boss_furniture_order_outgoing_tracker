package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Semantic colours that Material's scheme does not cover (operation modes and sync states). */
@Immutable
data class FactoryColors(
    val dispatch: Color,
    val returns: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    /** Content colour drawn on top of a solid accent fill. */
    val onAccent: Color,
    val cameraBackdrop: Color
)

private val LightFactoryColors = FactoryColors(
    dispatch = DispatchIndigo,
    returns = ReturnAmber,
    success = RestockedEmerald,
    warning = WarningOrange,
    danger = DangerRed,
    onAccent = Neutral0,
    cameraBackdrop = Neutral900
)

private val DarkFactoryColors = FactoryColors(
    dispatch = DispatchIndigoDark,
    returns = ReturnAmberDark,
    success = RestockedEmeraldDark,
    warning = WarningOrangeDark,
    danger = DangerRedDark,
    onAccent = Neutral950,
    cameraBackdrop = Neutral950
)

private val LocalFactoryColors = staticCompositionLocalOf { LightFactoryColors }

object FactoryTheme {
    val colors: FactoryColors
        @Composable
        @ReadOnlyComposable
        get() = LocalFactoryColors.current
}

private val DarkColorScheme = darkColorScheme(
    primary = DispatchIndigoDark,
    onPrimary = Neutral950,
    primaryContainer = Color(0xFF2A2B5C),
    onPrimaryContainer = Color(0xFFE0E0FF),
    secondary = Neutral400,
    onSecondary = Neutral950,
    secondaryContainer = Neutral800,
    onSecondaryContainer = Neutral100,
    tertiary = ReturnAmberDark,
    background = Neutral950,
    onBackground = Neutral50,
    surface = Neutral900,
    onSurface = Neutral50,
    surfaceVariant = Neutral850,
    onSurfaceVariant = Neutral400,
    surfaceContainerLowest = Neutral950,
    surfaceContainerLow = Neutral900,
    surfaceContainer = Neutral900,
    surfaceContainerHigh = Neutral850,
    surfaceContainerHighest = Neutral800,
    outline = Neutral700,
    outlineVariant = Neutral800,
    error = DangerRedDark,
    onError = Neutral950,
    errorContainer = Color(0xFF3B1517),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val LightColorScheme = lightColorScheme(
    primary = DispatchIndigo,
    onPrimary = Neutral0,
    primaryContainer = Color(0xFFEDEDFF),
    onPrimaryContainer = Color(0xFF1E1A78),
    secondary = Neutral500,
    onSecondary = Neutral0,
    secondaryContainer = Neutral100,
    onSecondaryContainer = Neutral700,
    tertiary = ReturnAmber,
    background = Neutral50,
    onBackground = Neutral900,
    surface = Neutral0,
    onSurface = Neutral900,
    surfaceVariant = Neutral100,
    onSurfaceVariant = Neutral500,
    surfaceContainerLowest = Neutral0,
    surfaceContainerLow = Neutral0,
    surfaceContainer = Neutral0,
    surfaceContainerHigh = Neutral0,
    surfaceContainerHighest = Neutral100,
    outline = Neutral300,
    outlineVariant = Neutral200,
    error = DangerRed,
    onError = Neutral0,
    errorContainer = Color(0xFFFEF2F2),
    onErrorContainer = Color(0xFF7F1D1D)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val factoryColors = if (darkTheme) DarkFactoryColors else LightFactoryColors

    CompositionLocalProvider(LocalFactoryColors provides factoryColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content
        )
    }
}
