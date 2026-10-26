package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.theme.FactoryTheme
import com.example.ui.theme.Motion

enum class BannerTone { Success, Error, Info }

/** Keeps the last non-null message so the exit animation still has text to show. */
private class LastValue<T>(var value: T? = null)

/**
 * Inline status message that expands into place when [message] becomes non-null and
 * collapses away when it is cleared.
 */
@Composable
fun StatusBanner(
    message: String?,
    modifier: Modifier = Modifier,
    toneOf: (String) -> BannerTone = { BannerTone.Info },
    accent: Color? = null,
    innerPadding: PaddingValues = PaddingValues(0.dp),
    onDismiss: (() -> Unit)? = null
) {
    val last = remember { LastValue<String>() }
    if (message != null) last.value = message
    val shown = message ?: last.value

    AnimatedVisibility(
        visible = message != null,
        modifier = modifier,
        enter = fadeIn(Motion.enter()) + expandVertically(Motion.enter(), expandFrom = Alignment.Top),
        exit = fadeOut(Motion.exit()) + shrinkVertically(Motion.exit(), shrinkTowards = Alignment.Top)
    ) {
        if (shown == null) return@AnimatedVisibility
        val tone = toneOf(shown)
        val color = when (tone) {
            BannerTone.Success -> FactoryTheme.colors.success
            BannerTone.Error -> MaterialTheme.colorScheme.error
            BannerTone.Info -> accent ?: MaterialTheme.colorScheme.onSurfaceVariant
        }
        val icon: ImageVector = when (tone) {
            BannerTone.Success -> Icons.Default.CheckCircle
            BannerTone.Error -> Icons.Default.ErrorOutline
            BannerTone.Info -> Icons.Outlined.Info
        }
        Box(modifier = Modifier.padding(innerPadding)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color.copy(alpha = 0.09f), MaterialTheme.shapes.medium)
                    .padding(start = 14.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                Text(
                    text = shown,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = if (onDismiss == null) 10.dp else 0.dp)
                )
                if (onDismiss != null) {
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

/** Centered illustration + copy for empty lists; eases in the first time it appears. */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    body: String,
    tint: Color,
    modifier: Modifier = Modifier,
    iconContentDescription: String? = null
) {
    val visibleState = remember { MutableTransitionState(false).apply { targetState = true } }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        AnimatedVisibility(
            visibleState = visibleState,
            enter = fadeIn(Motion.enter(Motion.LONG)) +
                scaleIn(Motion.enter(Motion.LONG), initialScale = 0.92f) +
                slideInVertically(Motion.enter(Motion.LONG)) { it / 12 }
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .padding(24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .background(tint.copy(alpha = 0.08f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = iconContentDescription, tint = tint, modifier = Modifier.size(40.dp))
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
