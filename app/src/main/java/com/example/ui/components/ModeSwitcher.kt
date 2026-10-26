package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.ui.theme.FactoryTheme
import com.example.ui.theme.Motion
import com.example.viewmodel.OperationMode

/** Pill-shaped Dispatch / Return toggle whose highlight slides between the two options. */
@Composable
fun SegmentedModeSwitcher(
    currentMode: OperationMode,
    onModeSelected: (OperationMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDispatch = currentMode == OperationMode.DISPATCH
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(48.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            .padding(4.dp)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val segmentWidth = maxWidth / 2
            val indicatorOffset by animateDpAsState(
                targetValue = if (isDispatch) 0.dp else segmentWidth,
                animationSpec = Motion.gentleSpring(),
                label = "modeIndicatorOffset"
            )
            val indicatorColor by animateColorAsState(
                targetValue = modeAccentColor(currentMode),
                animationSpec = Motion.emphasized(),
                label = "modeIndicatorColor"
            )

            Box(
                modifier = Modifier
                    .offset { IntOffset(indicatorOffset.roundToPx(), 0) }
                    .width(segmentWidth)
                    .fillMaxHeight()
                    .background(indicatorColor, CircleShape)
            )

            Row(modifier = Modifier.fillMaxSize()) {
                ModeSegment(
                    label = "Dispatch",
                    icon = OperationMode.DISPATCH.icon,
                    contentDescription = "Dispatch Mode",
                    selected = isDispatch,
                    onClick = { onModeSelected(OperationMode.DISPATCH) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("mode_switch_dispatch")
                )
                ModeSegment(
                    label = "Return",
                    icon = OperationMode.RETURN.icon,
                    contentDescription = "Return Mode",
                    selected = !isDispatch,
                    onClick = { onModeSelected(OperationMode.RETURN) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("mode_switch_return")
                )
            }
        }
    }
}

@Composable
private fun ModeSegment(
    label: String,
    icon: ImageVector,
    contentDescription: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentColor by animateColorAsState(
        targetValue = if (selected) FactoryTheme.colors.onAccent else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = Motion.emphasized(),
        label = "modeSegmentContent"
    )
    Row(
        modifier = modifier
            .fillMaxHeight()
            .clip(CircleShape)
            .clickable(role = Role.Tab, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = contentDescription, tint = contentColor, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = contentColor)
    }
}
