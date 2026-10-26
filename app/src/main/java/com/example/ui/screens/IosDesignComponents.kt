package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ScannedRecord
import com.example.data.model.QrPayload
import com.example.data.repository.DispatchRepository
import com.example.ui.theme.*
import com.example.viewmodel.DispatchViewModel
import com.example.viewmodel.OperationMode
import com.example.viewmodel.ToastNotice
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil

@Composable
fun HairlineDivider(modifier: Modifier = Modifier, startIndent: Dp = 0.dp) {
    HorizontalDivider(
        modifier = modifier.padding(start = startIndent),
        thickness = 0.5.dp,
        color = SeparatorHairline.copy(alpha = 0.65f)
    )
}

@Composable
fun SectionHeaderLabel(
    text: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text.uppercase(),
            fontFamily = InterFontFamily,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = SecondaryLabel,
            letterSpacing = 0.6.sp
        )
        trailing?.invoke()
    }
}

@Composable
fun IosSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    activeColor: Color = DispatchIndigo,
    modifier: Modifier = Modifier
) {
    val trackColor by animateColorAsState(
        targetValue = if (checked) activeColor else Color(0xFFE5E5EA),
        animationSpec = tween(200),
        label = "switchTrack"
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 22.dp else 2.dp,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 700f),
        label = "switchThumb"
    )

    Box(
        modifier = modifier
            .width(51.dp)
            .height(31.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(trackColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onCheckedChange(!checked) },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(27.dp)
                .shadow(2.dp, CircleShape)
                .background(Color.White, CircleShape)
        )
    }
}

@Composable
fun CopyTypeBadge(copyName: String, modifier: Modifier = Modifier) {
    val normalized = DispatchRepository.normalizeCopyType(copyName)
    val isGatePass = normalized == DispatchRepository.COPY_GATE_PASS
    if (isGatePass) {
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(6.dp),
            color = Color(0xFF18181B)
        ) {
            Text(
                text = normalized,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    } else {
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(6.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFD4D4D8))
        ) {
            Text(
                text = normalized,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF18181B),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun ModePillBadge(mode: String, modifier: Modifier = Modifier) {
    val isReturn = mode.equals("RETURN", ignoreCase = true)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = if (isReturn) ReturnAmber else DispatchIndigo
    ) {
        Text(
            text = if (isReturn) "RETURN" else "DISPATCH",
            fontFamily = InterFontFamily,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (isReturn) Color(0xFF1C1003) else Color.White,
            letterSpacing = 0.4.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun RestockedPillBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = RestockedEmerald
    ) {
        Text(
            text = "RESTOCKED",
            fontFamily = InterFontFamily,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            letterSpacing = 0.4.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun SyncStatusBadge(status: String, modifier: Modifier = Modifier) {
    when (status.uppercase()) {
        "SYNCED" -> {
            Surface(
                modifier = modifier,
                shape = RoundedCornerShape(999.dp),
                color = Color(0xFFD1FAE5)
            ) {
                Text(
                    text = "SYNCED",
                    fontFamily = InterFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = RestockedEmerald,
                    letterSpacing = 0.3.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                )
            }
        }
        "DUPLICATE" -> {
            Surface(
                modifier = modifier,
                shape = RoundedCornerShape(999.dp),
                color = DuplicateRed
            ) {
                Text(
                    text = "DUPLICATE",
                    fontFamily = InterFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 0.3.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                )
            }
        }
        "FAILED" -> {
            Surface(
                modifier = modifier,
                shape = RoundedCornerShape(999.dp),
                color = Color(0xFFFEF2F2),
                border = BorderStroke(1.dp, DuplicateRed.copy(alpha = 0.6f))
            ) {
                Text(
                    text = "FAILED",
                    fontFamily = InterFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = DuplicateRed,
                    letterSpacing = 0.3.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                )
            }
        }
        else -> {
            // PENDING dashed orange pill
            Box(
                modifier = modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xFFFFF7ED))
                    .drawWithContent {
                        drawContent()
                        val stroke = Stroke(
                            width = 1.2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f), 0f)
                        )
                        drawRoundRect(
                            color = OfflineOrange,
                            cornerRadius = CornerRadius(size.height / 2f, size.height / 2f),
                            style = stroke
                        )
                    }
                    .padding(horizontal = 10.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "PENDING",
                    fontFamily = InterFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = OfflineOrange,
                    letterSpacing = 0.3.sp
                )
            }
        }
    }
}

@Composable
fun TopFloatingToast(
    toast: ToastNotice?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = toast != null,
        enter = slideInVertically(
            initialOffsetY = { -it - 40 },
            animationSpec = spring(dampingRatio = 0.65f, stiffness = 380f)
        ) + fadeIn(tween(180)),
        exit = slideOutVertically(
            targetOffsetY = { -it - 40 },
            animationSpec = tween(200)
        ) + fadeOut(tween(150)),
        modifier = modifier
    ) {
        toast?.let { item ->
            Surface(
                onClick = onDismiss,
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                shadowElevation = 10.dp,
                border = BorderStroke(0.5.dp, SeparatorHairline.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = if (item.isOffline) OfflineOrange else RestockedEmerald,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (item.isOffline) Icons.Default.WifiOff else Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = Neutral900
                        )
                        Text(
                            text = item.subtitle,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 13.sp,
                            color = SecondaryLabel,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VehiclePickerSheet(
    viewModel: DispatchViewModel,
    onDismiss: () -> Unit
) {
    val selectedOption by viewModel.selectedVehicleOption.collectAsState()
    val savedCustomVehicles by viewModel.savedCustomVehicles.collectAsState()
    val customNumber by viewModel.customVehicleNumber.collectAsState()
    var showAddCustomField by remember {
        mutableStateOf(selectedOption == DispatchViewModel.VEHICLE_CUSTOM_OPTION)
    }

    val coreOptions = remember(savedCustomVehicles) {
        val customList = savedCustomVehicles.filter { custom ->
            DispatchViewModel.DEFAULT_VEHICLES.none { it.equals(custom, ignoreCase = true) } &&
                !custom.equals(DispatchViewModel.VEHICLE_CUSTOM_OPTION, ignoreCase = true)
        }
        DispatchViewModel.DEFAULT_VEHICLES + customList
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = GroupedBg,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {}
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .navigationBarsPadding()
            ) {
                // Drag handle
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(38.dp)
                        .height(5.dp)
                        .background(Color(0xFFC7C7CC), RoundedCornerShape(3.dp))
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Header
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Vehicle",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Neutral900,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Text(
                            text = "Done",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = DispatchIndigo
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Vehicle options list
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CellWhite,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        coreOptions.forEachIndexed { idx, option ->
                            val isSelected = selectedOption == option
                            val subtitle = when (option) {
                                DispatchViewModel.VEHICLE_DAC_4912 -> "Company truck · driver linked"
                                DispatchViewModel.VEHICLE_DAE_4217 -> "Company truck · driver linked"
                                DispatchViewModel.VEHICLE_PICK_ME -> "Third-party · plate number required"
                                else -> "Custom registered vehicle"
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.setSelectedVehicleOption(option)
                                        if (option != DispatchViewModel.VEHICLE_CUSTOM_OPTION) {
                                            onDismiss()
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 14.dp)
                                    .testTag("vehicle_option_${option.lowercase().replace(" ", "_")}"),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = option,
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 16.sp,
                                        color = Neutral900
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = subtitle,
                                        fontFamily = InterFontFamily,
                                        fontSize = 13.sp,
                                        color = SecondaryLabel
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = DispatchIndigo,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            if (idx < coreOptions.lastIndex) {
                                HairlineDivider(startIndent = 16.dp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // + Add custom vehicle button / input
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CellWhite,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showAddCustomField = !showAddCustomField
                                    viewModel.setSelectedVehicleOption(DispatchViewModel.VEHICLE_CUSTOM_OPTION)
                                }
                                .testTag("vehicle_option_custom_vehicle"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = DispatchIndigo,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Add custom vehicle",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                color = DispatchIndigo
                            )
                        }

                        if (showAddCustomField || selectedOption == DispatchViewModel.VEHICLE_CUSTOM_OPTION) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = customNumber,
                                    onValueChange = { viewModel.setCustomVehicleNumber(it) },
                                    placeholder = { Text("e.g. WP-DAG-8890 (Kamal)") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("custom_vehicle_number_input")
                                )
                                Button(
                                    onClick = {
                                        viewModel.addAndSelectCustomVehicle(customNumber)
                                        onDismiss()
                                    },
                                    enabled = customNumber.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(containerColor = DispatchIndigo),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .height(52.dp)
                                        .testTag("add_custom_vehicle_button")
                                ) {
                                    Text("Save", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun AutoConfirmScanSheet(
    payload: QrPayload,
    isReturnMode: Boolean,
    effectiveVehicle: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val scannedCopy = DispatchRepository.normalizeCopyType(payload.copy)
    val isGatePass = DispatchRepository.isGatePassCopy(scannedCopy)
    val accentColor = if (isReturnMode) ReturnAmber else DispatchIndigo

    val progressAnim = remember(payload) { Animatable(1f) }
    LaunchedEffect(payload) {
        progressAnim.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 5000, easing = LinearEasing)
        )
        onConfirm()
    }

    val secondsLeft = ceil(progressAnim.value * 5f).toInt().coerceIn(1, 5)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onCancel() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = CellWhite,
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {}
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
                    .navigationBarsPadding()
            ) {
                // Drag handle
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(38.dp)
                        .height(5.dp)
                        .background(Color(0xFFD1D1D6), RoundedCornerShape(3.dp))
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Top Countdown Ring + Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier.size(38.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeW = 3.dp.toPx()
                            drawCircle(
                                color = accentColor.copy(alpha = 0.18f),
                                style = Stroke(width = strokeW)
                            )
                            drawArc(
                                color = accentColor,
                                startAngle = -90f,
                                sweepAngle = 360f * progressAnim.value,
                                useCenter = false,
                                style = Stroke(width = strokeW, cap = StrokeCap.Round)
                            )
                        }
                        Text(
                            text = "$secondsLeft",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = accentColor
                        )
                    }

                    Column {
                        Text(
                            text = if (isReturnMode) "QR SCANNED · RETURN" else "QR SCANNED · DISPATCH",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp,
                            color = accentColor,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = when {
                                isReturnMode -> "Confirm Return & Restock"
                                isGatePass -> "Confirm Dispatch"
                                else -> "Confirm Copy Scan"
                            },
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            color = Neutral900,
                            letterSpacing = (-0.4).sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Order No + Copy Badge Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "#${payload.orderNo ?: "N/A"}",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 34.sp,
                        color = Neutral900,
                        letterSpacing = (-0.8).sp
                    )
                    CopyTypeBadge(copyName = scannedCopy)
                }

                Spacer(modifier = Modifier.height(12.dp))
                HairlineDivider()

                // Customer Row
                SheetKeyValueRow(
                    label = "Customer",
                    value = payload.customer?.ifBlank { "Factory Order" } ?: "Factory Order"
                )
                HairlineDivider()

                // Units Row
                val unitCount = payload.totalUnits ?: payload.qty ?: 1
                SheetKeyValueRow(
                    label = "Units",
                    value = "$unitCount ${if (unitCount == 1) "table" else "tables"}"
                )
                HairlineDivider()

                // Vehicle / Driver Row
                if (isReturnMode || isGatePass) {
                    SheetKeyValueRow(
                        label = "Vehicle",
                        value = effectiveVehicle
                    )
                } else {
                    SheetKeyValueRow(
                        label = "Driver",
                        value = "Unchanged",
                        valueColor = SecondaryLabel
                    )
                }

                if (isReturnMode) {
                    HairlineDivider()
                    SheetKeyValueRow(
                        label = "Restock",
                        value = "Yes · back to stock",
                        valueColor = RestockedEmerald
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Callout Box
                val calloutBg = when {
                    isReturnMode -> ReturnAmberTint
                    isGatePass -> DispatchIndigoTint
                    else -> Color(0xFFF4F4F5)
                }
                val calloutBorder = when {
                    isReturnMode -> Color(0xFFFDE68A)
                    isGatePass -> Color(0xFFC7D2FE)
                    else -> Color(0xFFE4E4E7)
                }
                val calloutTextColor = when {
                    isReturnMode -> ReturnAmberText
                    isGatePass -> DispatchIndigoText
                    else -> Neutral900
                }
                val calloutText = when {
                    isReturnMode -> "Mark returned & restock ($scannedCopy)"
                    isGatePass -> "Assign order to $effectiveVehicle &\nrecord GATE PASS"
                    else -> "Record $scannedCopy only — does not\nassign a driver"
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = calloutBg,
                    border = BorderStroke(1.dp, calloutBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = calloutText,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = calloutTextColor,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Draining Primary Confirm Button
                val baseButtonColor = if (isReturnMode) Color(0xFFB45309) else Color(0xFF3730A3)
                val fillButtonColor = if (isReturnMode) ReturnAmber else DispatchIndigo
                val buttonText = when {
                    isReturnMode -> "Confirm Return (${secondsLeft}s)"
                    isGatePass -> "Confirm Dispatch (${secondsLeft}s)"
                    else -> "Record Scan (${secondsLeft}s)"
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(baseButtonColor)
                        .drawWithContent {
                            drawRect(
                                color = fillButtonColor,
                                size = Size(size.width * progressAnim.value, size.height)
                            )
                            drawContent()
                        }
                        .clickable { onConfirm() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = buttonText,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = if (isReturnMode) ReturnAmberInk else Color.White
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Cancel Button
                Surface(
                    onClick = onCancel,
                    shape = RoundedCornerShape(14.dp),
                    color = TertiaryFill,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "Cancel",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = Neutral900
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetKeyValueRow(
    label: String,
    value: String,
    valueColor: Color = Neutral900
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 11.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontFamily = InterFontFamily,
            fontSize = 15.sp,
            color = SecondaryLabel
        )
        Text(
            text = value,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = valueColor
        )
    }
}

@Composable
fun RecordDetailScreen(
    record: ScannedRecord,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val clipboardManager = LocalClipboardManager.current
    val dateFormat = remember { SimpleDateFormat("dd MMM · HH:mm:ss", Locale.getDefault()) }
    val scannedStr = remember(record.scannedAt) { dateFormat.format(Date(record.scannedAt)) }
    val statusUpper = record.syncStatus.uppercase()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GroupedBg)
    ) {
        // iOS-style Navigation Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onBack() }
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                    contentDescription = "Back to History",
                    tint = DispatchIndigo,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "History",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 17.sp,
                    color = DispatchIndigo
                )
            }

            Text(
                text = "Record",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = Neutral900,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Status Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CellWhite,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val badgeBg = when (statusUpper) {
                        "SYNCED" -> RestockedEmerald
                        "DUPLICATE" -> DuplicateRed
                        "PENDING" -> OfflineOrange
                        else -> DuplicateRed
                    }
                    val badgeIcon = when (statusUpper) {
                        "SYNCED" -> Icons.Default.Check
                        "DUPLICATE" -> Icons.Default.Close
                        "PENDING" -> Icons.Default.Schedule
                        else -> Icons.Default.PriorityHigh
                    }

                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(badgeBg, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = badgeIcon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "#${record.orderNo}",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 34.sp,
                        color = Neutral900,
                        letterSpacing = (-0.8).sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ModePillBadge(mode = record.mode)
                        if (record.restocked) {
                            RestockedPillBadge()
                        }
                        CopyTypeBadge(copyName = record.copy)
                        SyncStatusBadge(status = record.syncStatus)
                    }

                    if (!record.errorMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = record.errorMessage.orEmpty(),
                            fontFamily = InterFontFamily,
                            fontSize = 13.sp,
                            color = DuplicateRed,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // OPERATION Group
            Column {
                SectionHeaderLabel(text = "OPERATION")
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CellWhite,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        DetailGroupRow(
                            label = "Mode",
                            value = if (record.mode.equals("RETURN", true)) "Return" else "Dispatch"
                        )
                        HairlineDivider()
                        DetailGroupRow(
                            label = "Restocked",
                            value = if (record.restocked) "Yes" else "No"
                        )
                        HairlineDivider()
                        DetailGroupRow(
                            label = "Sync status",
                            value = record.syncStatus.lowercase().replaceFirstChar { it.uppercase() },
                            valueColor = when (statusUpper) {
                                "SYNCED" -> RestockedEmerald
                                "DUPLICATE", "FAILED" -> DuplicateRed
                                else -> OfflineOrange
                            },
                            valueBold = true
                        )
                        HairlineDivider()
                        DetailGroupRow(
                            label = "Scanned",
                            value = scannedStr
                        )
                    }
                }
            }

            // ORDER & TRANSPORT Group
            Column {
                SectionHeaderLabel(text = "ORDER & TRANSPORT")
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CellWhite,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        DetailGroupRow(
                            label = "Customer",
                            value = record.customer.ifBlank { "Factory Order" }
                        )
                        HairlineDivider()
                        DetailGroupRow(
                            label = "District",
                            value = record.district.ifBlank { "Colombo" }
                        )
                        HairlineDivider()
                        DetailGroupRow(
                            label = "Delivery type",
                            value = record.deliveryType.ifBlank {
                                if (record.vehicleNumber.startsWith("Pick Me", true)) "Third-party delivery" else "Company delivery"
                            }
                        )
                        HairlineDivider()
                        DetailGroupRow(
                            label = "Vehicle",
                            value = record.vehicleNumber.ifBlank { "Unassigned" }
                        )
                    }
                }
            }

            // ITEM Group
            Column {
                SectionHeaderLabel(text = "ITEM")
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CellWhite,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        DetailGroupRow(
                            label = "Copy format",
                            value = record.copy.ifBlank { "GATE PASS" }
                        )
                        HairlineDivider()
                        DetailGroupRow(
                            label = "Table",
                            value = "${record.tableIndex} of ${record.tableCount.coerceAtLeast(1)}"
                        )
                        HairlineDivider()
                        DetailGroupRow(
                            label = "Size",
                            value = record.size.ifBlank { "Standard" }
                        )
                        HairlineDivider()
                        DetailGroupRow(
                            label = "Top wood colour",
                            value = record.topColour.ifBlank { "Teak" },
                            colorDot = Color(0xFF8B5A2B)
                        )
                        HairlineDivider()
                        DetailGroupRow(
                            label = "Frame powder",
                            value = record.frameColour.ifBlank { "Matte Black" },
                            colorDot = Color(0xFF1F2937)
                        )
                        HairlineDivider()
                        DetailGroupRow(
                            label = "Quantity",
                            value = "${record.qty}"
                        )
                    }
                }
            }

            // AUDIT Group
            Column {
                SectionHeaderLabel(text = "AUDIT")
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CellWhite,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column {
                            Text(
                                text = "Duplicate key",
                                fontFamily = InterFontFamily,
                                fontSize = 12.sp,
                                color = SecondaryLabel
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "${record.orderNo} | ${record.copy} | ${if (record.mode.equals("RETURN", true)) "returned" else "dispatched"}",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = Neutral900
                            )
                        }
                        HairlineDivider()
                        Column {
                            Text(
                                text = "Event log",
                                fontFamily = InterFontFamily,
                                fontSize = 12.sp,
                                color = SecondaryLabel
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = record.errorMessage?.ifBlank { null }
                                    ?: "Synced to public.transport (copy_name=${record.copy}, transport_status=${if (record.mode.equals("RETURN", true)) "returned" else "dispatched"})",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 12.sp,
                                color = if (record.errorMessage.isNullOrBlank()) RestockedEmerald else DuplicateRed,
                                lineHeight = 17.sp
                            )
                        }
                        HairlineDivider()
                        Column {
                            Text(
                                text = "Device",
                                fontFamily = InterFontFamily,
                                fontSize = 12.sp,
                                color = SecondaryLabel
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = formatDeviceId(record.deviceId),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                color = Neutral900
                            )
                        }
                    }
                }
            }

            // RAW QR PAYLOAD Section
            val rawJsonText = remember(record) {
                if (record.rawPayload.trim().startsWith("{")) {
                    record.rawPayload
                } else {
                    """
{
  "order_no": "${record.orderNo}",
  "copy_name": "${record.copy}",
  "customer": "${record.customer.ifBlank { "Factory Order" }}",
  "district": "${record.district.ifBlank { "Colombo" }}",
  "vehicle": "${record.vehicleNumber}",
  "table_index": "${record.tableIndex}/${record.tableCount.coerceAtLeast(1)}",
  "size": "${record.size.ifBlank { "Standard" }}",
  "transport_status": "${if (record.mode.equals("RETURN", true)) "returned" else "dispatched"}"
}
                    """.trimIndent()
                }
            }

            Column {
                SectionHeaderLabel(
                    text = "RAW QR PAYLOAD",
                    trailing = {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    clipboardManager.setText(AnnotatedString(rawJsonText))
                                }
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Payload",
                                tint = DispatchIndigo,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Copy",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = DispatchIndigo
                            )
                        }
                    }
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Neutral900,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = rawJsonText,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = Color(0xFFE4E4E7),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DetailGroupRow(
    label: String,
    value: String,
    valueColor: Color = SecondaryLabel,
    valueBold: Boolean = false,
    colorDot: Color? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 13.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontFamily = InterFontFamily,
            fontSize = 16.sp,
            color = Neutral900
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (colorDot != null) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(colorDot, RoundedCornerShape(3.dp))
                )
            }
            Text(
                text = value,
                fontFamily = InterFontFamily,
                fontWeight = if (valueBold) FontWeight.Bold else FontWeight.Normal,
                fontSize = 16.sp,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

fun formatDeviceId(raw: String): String {
    val clean = raw.replace(" ", "").take(16)
    return clean.chunked(4).joinToString(" ")
}
