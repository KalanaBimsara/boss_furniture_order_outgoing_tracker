package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
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
    val onAccent: Color,
    val cameraBackdrop: Color
)

private val LightFactoryColors = FactoryColors(
    dispatch = DispatchIndigo,
    returns = ReturnAmber,
    success = RestockedEmerald,
    warning = OfflineOrange,
    danger = DuplicateRed,
    onAccent = Neutral0,
    cameraBackdrop = Neutral900
)

private val LocalFactoryColors = staticCompositionLocalOf { LightFactoryColors }

object FactoryTheme {
    val colors: FactoryColors
        @Composable
        @ReadOnlyComposable
        get() = LocalFactoryColors.current
}

private val IosLightColorScheme = lightColorScheme(
    primary = DispatchIndigo,
    onPrimary = Neutral0,
    primaryContainer = DispatchIndigoTint,
    onPrimaryContainer = DispatchIndigoText,
    secondary = SecondaryLabel,
    onSecondary = Neutral0,
    secondaryContainer = TertiaryFill,
    onSecondaryContainer = Neutral900,
    tertiary = ReturnAmber,
    background = GroupedBg,
    onBackground = Neutral900,
    surface = CellWhite,
    onSurface = Neutral900,
    surfaceVariant = TertiaryFill,
    onSurfaceVariant = SecondaryLabel,
    surfaceContainerLowest = CellWhite,
    surfaceContainerLow = GroupedBg,
    surfaceContainer = CellWhite,
    surfaceContainerHigh = CellWhite,
    surfaceContainerHighest = TertiaryFill,
    outline = SeparatorHairline,
    outlineVariant = Neutral200,
    error = DuplicateRed,
    onError = Neutral0,
    errorContainer = DuplicateRedTint,
    onErrorContainer = DuplicateRed
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalFactoryColors provides LightFactoryColors) {
        MaterialTheme(
            colorScheme = IosLightColorScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content
        )
    }
}
