package com.example.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/** Shared motion tokens so every transition in the app feels like the same family. */
object Motion {
    const val SHORT = 180
    const val MEDIUM = 280
    const val LONG = 420

    val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    fun <T> emphasized(durationMillis: Int = MEDIUM): FiniteAnimationSpec<T> =
        tween(durationMillis, easing = Emphasized)

    fun <T> enter(durationMillis: Int = MEDIUM): FiniteAnimationSpec<T> =
        tween(durationMillis, easing = EmphasizedDecelerate)

    fun <T> exit(durationMillis: Int = SHORT): FiniteAnimationSpec<T> =
        tween(durationMillis, easing = EmphasizedAccelerate)

    /** Soft spring for sliding indicators and press feedback. */
    fun <T> gentleSpring(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
}
