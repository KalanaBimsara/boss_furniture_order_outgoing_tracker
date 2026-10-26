package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.QrPayload
import com.example.ui.components.AccentButton
import com.example.ui.components.IconBadge
import com.example.ui.components.icon
import com.example.ui.theme.FactoryTheme
import com.example.viewmodel.OperationMode

private const val AUTO_CONFIRM_SECONDS = 5

/**
 * Confirmation shown after a QR scan. Auto-confirms after [AUTO_CONFIRM_SECONDS] seconds,
 * with a draining progress bar so the operator can see the countdown at a glance.
 */
@Composable
fun ScanConfirmDialog(
    payload: QrPayload,
    isReturn: Boolean,
    accentColor: Color,
    effectiveVehicle: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var timeLeft by remember(payload) { mutableIntStateOf(AUTO_CONFIRM_SECONDS) }
    val progress = remember(payload) { Animatable(1f) }
    val currentOnConfirm by rememberUpdatedState(onConfirm)

    LaunchedEffect(payload) {
        for (i in AUTO_CONFIRM_SECONDS downTo 1) {
            timeLeft = i
            kotlinx.coroutines.delay(1000)
        }
        currentOnConfirm()
    }
    LaunchedEffect(payload) {
        progress.animateTo(0f, tween(AUTO_CONFIRM_SECONDS * 1000, easing = LinearEasing))
    }

    val scannedCopy = payload.copy ?: "GATE PASS"
    val isGatePassCopy = scannedCopy.equals("GATE PASS", ignoreCase = true)
    val successColor = FactoryTheme.colors.success
    val actionColor = if (isReturn) successColor else accentColor

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.extraLarge,
        icon = {
            IconBadge(
                icon = if (isReturn) OperationMode.RETURN.icon else OperationMode.DISPATCH.icon,
                tint = accentColor,
                size = 52.dp,
                iconSize = 26.dp
            )
        },
        title = {
            Text(
                text = if (isReturn) "Confirm return & restock" else "Confirm outgoing dispatch",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "#${payload.orderNo ?: "N/A"}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = accentColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background, MaterialTheme.shapes.medium)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ConfirmDetail("Customer", payload.customer ?: "N/A")
                    ConfirmDetail("Total units", "${payload.totalUnits ?: payload.qty ?: 1}")
                    ConfirmDetail("Copy scanned", scannedCopy)
                    if (isGatePassCopy || isReturn) {
                        ConfirmDetail("Vehicle", effectiveVehicle)
                    }
                }

                Text(
                    text = when {
                        isReturn -> "Action: Mark returned & restock ($scannedCopy)"
                        isGatePassCopy -> "Action: Assign order to driver ($effectiveVehicle) & record GATE PASS"
                        else -> "Action: Record $scannedCopy scan only (does not assign driver)"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = actionColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(actionColor.copy(alpha = 0.1f), MaterialTheme.shapes.small)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )

                Spacer(modifier = Modifier.height(2.dp))
                LinearProgressIndicator(
                    progress = { progress.value },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = accentColor,
                    trackColor = accentColor.copy(alpha = 0.12f),
                    strokeCap = StrokeCap.Round
                )
                Text(
                    text = "Auto-confirming in ${timeLeft}s",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            AccentButton(
                text = when {
                    isReturn -> "Confirm return & restock"
                    isGatePassCopy -> "Assign driver & record"
                    else -> "Record ${payload.copy ?: "copy"} scan"
                },
                onClick = onConfirm,
                accent = accentColor,
                compact = true
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

@Composable
private fun ConfirmDetail(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}
