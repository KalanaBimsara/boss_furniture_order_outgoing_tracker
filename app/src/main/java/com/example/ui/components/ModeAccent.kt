package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AssignmentReturn
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.theme.DispatchIndigo
import com.example.ui.theme.FactoryTheme
import com.example.ui.theme.Motion
import com.example.viewmodel.OperationMode

/** Accent of the active operation mode, animated by [com.example.ui.screens.AppNavigator]. */
val LocalModeAccent = compositionLocalOf { DispatchIndigo }

@Composable
@ReadOnlyComposable
fun modeAccentColor(mode: OperationMode): Color =
    if (mode == OperationMode.DISPATCH) FactoryTheme.colors.dispatch else FactoryTheme.colors.returns

/** Mode accent that cross-fades whenever the operator flips between Dispatch and Return. */
@Composable
fun animatedModeAccent(mode: OperationMode): Color {
    val accent by animateColorAsState(
        targetValue = modeAccentColor(mode),
        animationSpec = Motion.emphasized(Motion.LONG),
        label = "modeAccent"
    )
    return accent
}

val OperationMode.icon: ImageVector
    get() = if (this == OperationMode.DISPATCH) Icons.Default.LocalShipping else Icons.AutoMirrored.Filled.AssignmentReturn
