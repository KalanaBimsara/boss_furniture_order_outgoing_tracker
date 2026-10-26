package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DispatchIndigo

/** Square reticle with rounded corner brackets that gently breathe while a scan line sweeps. */
@Composable
fun ScannerOverlay(
    modifier: Modifier = Modifier,
    accentColor: Color = DispatchIndigo
) {
    val transition = rememberInfiniteTransition(label = "scannerOverlay")
    val breathe by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.035f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breathe"
    )
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "sweep"
    )

    Box(
        modifier = modifier
            .fillMaxHeight(0.74f)
            .aspectRatio(1f)
            .graphicsLayer {
                scaleX = breathe
                scaleY = breathe
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 4.dp.toPx()
            inset(strokeWidth / 2f) {
                drawPath(
                    path = cornerBrackets(size, arm = size.minDimension * 0.18f, radius = 18.dp.toPx()),
                    color = accentColor,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }

            val margin = 16.dp.toPx()
            val glow = 22.dp.toPx()
            val y = margin + (size.height - margin * 2) * sweep
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, accentColor.copy(alpha = 0.18f), Color.Transparent),
                    startY = y - glow,
                    endY = y + glow
                ),
                topLeft = Offset(margin, y - glow),
                size = Size(size.width - margin * 2, glow * 2)
            )
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, accentColor, Color.Transparent),
                    startX = margin,
                    endX = size.width - margin
                ),
                start = Offset(margin, y),
                end = Offset(size.width - margin, y),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

/** Four L-shaped brackets with rounded outer corners. */
private fun cornerBrackets(size: Size, arm: Float, radius: Float): Path {
    val w = size.width
    val h = size.height
    val c = radius * 0.448f // cubic control offset approximating a quarter circle
    return Path().apply {
        moveTo(0f, arm)
        lineTo(0f, radius)
        cubicTo(0f, c, c, 0f, radius, 0f)
        lineTo(arm, 0f)

        moveTo(w - arm, 0f)
        lineTo(w - radius, 0f)
        cubicTo(w - c, 0f, w, c, w, radius)
        lineTo(w, arm)

        moveTo(w, h - arm)
        lineTo(w, h - radius)
        cubicTo(w, h - c, w - c, h, w - radius, h)
        lineTo(w - arm, h)

        moveTo(arm, h)
        lineTo(radius, h)
        cubicTo(c, h, 0f, h - c, 0f, h - radius)
        lineTo(0f, h - arm)
    }
}
