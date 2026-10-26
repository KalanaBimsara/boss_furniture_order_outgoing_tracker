package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.HistoryToggleOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.ScannedRecord
import com.example.ui.components.AnimatedCounter
import com.example.ui.components.EmptyState
import com.example.ui.components.IconBadge
import com.example.ui.components.LocalModeAccent
import com.example.ui.components.SoftButton
import com.example.ui.components.StatusPill
import com.example.ui.components.appTextFieldColors
import com.example.ui.components.icon
import com.example.ui.components.pressScale
import com.example.ui.components.toLabel
import com.example.ui.theme.FactoryTheme
import com.example.ui.theme.Motion
import com.example.viewmodel.DispatchViewModel
import com.example.viewmodel.OperationMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(viewModel: DispatchViewModel) {
    val history by viewModel.scannedHistory.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val historyModeFilter by viewModel.historyModeFilter.collectAsStateWithLifecycle()
    var selectedRecordDetail by remember { mutableStateOf<ScannedRecord?>(null) }
    val accent = LocalModeAccent.current

    val context = LocalContext.current
    var isClearing by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                AnimatedCounter(count = history.size)
                Text(
                    text = "logs today",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SoftButton(
                text = "Clear & sync",
                icon = Icons.Default.DeleteSweep,
                onClick = {
                    isClearing = true
                    viewModel.clearTodayLogAndSync(
                        context = context,
                        onSuccess = { isClearing = false },
                        onError = { isClearing = false }
                    )
                },
                enabled = !isClearing && history.isNotEmpty(),
                loading = isClearing,
                color = MaterialTheme.colorScheme.error
            )
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.onSearchQueryChanged(it) },
            placeholder = { Text("Search order, customer or district") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search bar") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear click")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_history_input"),
            shape = CircleShape,
            colors = appTextFieldColors(accent)
        )

        // Mode filter pills (ALL, DISPATCH, RETURN)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ModeFilterChip(
                label = "All",
                selected = historyModeFilter == "ALL",
                color = MaterialTheme.colorScheme.onSurface,
                onClick = { viewModel.setHistoryModeFilter("ALL") }
            )
            ModeFilterChip(
                label = "Dispatches",
                icon = OperationMode.DISPATCH.icon,
                selected = historyModeFilter == "DISPATCH",
                color = FactoryTheme.colors.dispatch,
                onClick = { viewModel.setHistoryModeFilter("DISPATCH") }
            )
            ModeFilterChip(
                label = "Returns",
                icon = OperationMode.RETURN.icon,
                selected = historyModeFilter == "RETURN",
                color = FactoryTheme.colors.returns,
                onClick = { viewModel.setHistoryModeFilter("RETURN") }
            )
        }

        AnimatedContent(
            targetState = history.isEmpty(),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            transitionSpec = { fadeIn(Motion.enter()) togetherWith fadeOut(Motion.exit()) },
            label = "historyContent"
        ) { isEmpty ->
            if (isEmpty) {
                EmptyState(
                    icon = Icons.Default.HistoryToggleOff,
                    iconContentDescription = "Empty sign",
                    title = "No scanned logs found",
                    body = "Scanned dispatches and returns appear in this audit log.",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(history, key = { it.id }) { record ->
                        HistoryItemCard(
                            record = record,
                            onClick = { selectedRecordDetail = record },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }
    }

    selectedRecordDetail?.let { record ->
        RecordDetailDialog(record = record, onDismiss = { selectedRecordDetail = null })
    }
}

@Composable
private fun ModeFilterChip(
    label: String,
    selected: Boolean,
    color: Color,
    onClick: () -> Unit,
    icon: ImageVector? = null
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelLarge) },
        leadingIcon = icon?.let {
            { Icon(it, contentDescription = null, modifier = Modifier.size(16.dp)) }
        },
        shape = CircleShape,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surface,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedContainerColor = color.copy(alpha = 0.12f),
            selectedLabelColor = color,
            selectedLeadingIconColor = color
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outlineVariant,
            selectedBorderColor = Color.Transparent
        )
    )
}

@Composable
private fun RecordDetailDialog(record: ScannedRecord, onDismiss: () -> Unit) {
    val isReturn = record.mode == "RETURN"
    val modeColor = if (isReturn) FactoryTheme.colors.returns else FactoryTheme.colors.dispatch

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.extraLarge,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = modeColor)
            }
        },
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                StatusPill(text = record.mode.toLabel(), color = modeColor)
                Text("Order #${record.orderNo}", style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VerificationRow(name = "Operation Mode", value = record.mode)
                if (record.restocked) {
                    VerificationRow(name = "Restocked?", value = "YES (Factory Inventory)")
                }
                if (record.vehicleNumber.isNotBlank()) {
                    VerificationRow(name = "Vehicle Number", value = record.vehicleNumber)
                }
                VerificationRow(name = "Order Number", value = "#${record.orderNo}")
                VerificationRow(name = "Customer", value = record.customer.ifBlank { "N/A" })
                VerificationRow(name = "District", value = record.district.ifBlank { "N/A" })
                VerificationRow(name = "Delivery Type", value = record.deliveryType.ifBlank { "N/A" })
                VerificationRow(name = "Copy Format", value = record.copy.ifBlank { "N/A" })
                VerificationRow(name = "Index", value = "Table ${record.tableIndex} / ${record.tableCount}")
                VerificationRow(name = "Specs (Size)", value = record.size.ifBlank { "N/A" })
                VerificationRow(name = "Top Wood Colour", value = record.topColour.ifBlank { "N/A" })
                VerificationRow(name = "Frame Powder", value = record.frameColour.ifBlank { "N/A" })
                VerificationRow(name = "Quantity Total", value = "${record.qty} Units")

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                VerificationRow(name = "Duplicate Key", value = record.duplicateKey)
                VerificationRow(name = "Sync Status", value = record.syncStatus)
                if (record.errorMessage != null) {
                    Text(
                        "Event Log: ${record.errorMessage}",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (record.notes.isNotBlank()) {
                    VerificationRow(name = "Notes", value = record.notes)
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Raw payload: ${record.rawPayload}",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 14.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background, MaterialTheme.shapes.small)
                        .padding(10.dp)
                )
            }
        }
    )
}

@Composable
fun VerificationRow(name: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun syncStatusColor(status: String): Color = when (status.uppercase()) {
    "SYNCED" -> FactoryTheme.colors.success
    "PENDING" -> FactoryTheme.colors.warning
    else -> FactoryTheme.colors.danger
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HistoryItemCard(
    record: ScannedRecord,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formatter = remember { SimpleDateFormat("MMM d · HH:mm", Locale.getDefault()) }
    val formattedDate = remember(record.scannedAt) { formatter.format(Date(record.scannedAt)) }
    val isReturn = record.mode == "RETURN"
    val modeColor = if (isReturn) FactoryTheme.colors.returns else FactoryTheme.colors.dispatch
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .pressScale(interactionSource, pressedScale = 0.98f)
            .testTag("history_item_${record.id}"),
        interactionSource = interactionSource,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconBadge(
                icon = if (isReturn) OperationMode.RETURN.icon else OperationMode.DISPATCH.icon,
                tint = modeColor,
                size = 40.dp,
                iconSize = 20.dp,
                contentDescription = if (isReturn) "Return" else "Dispatch"
            )

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "#${record.orderNo}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (record.customer.isNotBlank()) {
                    Text(
                        text = record.customer,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                val meta = listOfNotNull(
                    record.vehicleNumber.ifBlank { null },
                    record.district.ifBlank { null },
                    if (record.tableCount > 0) "Table ${record.tableIndex}/${record.tableCount}" else null,
                    if (record.size.isNotBlank()) "Size ${record.size}" else null
                )
                if (meta.isNotEmpty()) {
                    Text(
                        text = meta.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (!record.errorMessage.isNullOrEmpty()) {
                    Text(
                        text = record.errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatusPill(text = record.syncStatus.toLabel(), color = syncStatusColor(record.syncStatus))
                    if (record.copy.isNotBlank()) {
                        StatusPill(text = record.copy.toLabel(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (record.restocked) {
                        StatusPill(text = "Restocked", color = FactoryTheme.colors.success)
                    }
                }
            }
        }
    }
}
