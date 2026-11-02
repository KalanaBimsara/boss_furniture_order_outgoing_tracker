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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.api.SupabaseOrderTable
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
    onCancel: () -> Unit,
    isManualConfirmation: Boolean = false,
    isFetchingManualOrder: Boolean = false,
    manualOrderFoundInDb: Boolean = true,
    manualOrderTables: List<SupabaseOrderTable> = emptyList(),
    initialNote: String = "",
    initialRestocked: Boolean = true,
    onConfirmWithEdits: ((QrPayload, String, Boolean) -> Unit)? = null
) {
    val focusManager = LocalFocusManager.current
    var isEditingDetails by remember(isManualConfirmation) { mutableStateOf(isManualConfirmation) }
    var noteText by remember(payload.orderNo) { mutableStateOf(initialNote) }
    var isTypingNote by remember { mutableStateOf(false) }
    var restockFlag by remember(initialRestocked) { mutableStateOf(initialRestocked) }

    // Editable order & table specification state (updated when DB fetch completes)
    var customerInput by remember { mutableStateOf(payload.customer.orEmpty()) }
    var districtInput by remember { mutableStateOf(payload.district.orEmpty()) }
    var copyInput by remember { mutableStateOf(DispatchRepository.normalizeCopyType(payload.copy)) }
    var sizeInput by remember { mutableStateOf(payload.resolvedSize.orEmpty()) }
    var qtyInput by remember { mutableStateOf((payload.resolvedQty ?: 1).coerceAtLeast(1).toString()) }
    var topColourInput by remember { mutableStateOf(payload.resolvedTopColour.orEmpty()) }
    var frameColourInput by remember { mutableStateOf(payload.resolvedFrameColour.orEmpty()) }
    var legSizeInput by remember { mutableStateOf(payload.resolvedLegSize.orEmpty()) }
    var legHeightInput by remember { mutableStateOf(payload.resolvedLegHeight.orEmpty()) }
    var legShapeInput by remember { mutableStateOf(payload.resolvedLegShape.orEmpty()) }
    var selectedTableIdx by remember { mutableIntStateOf(0) }

    LaunchedEffect(payload) {
        customerInput = payload.customer.orEmpty()
        districtInput = payload.district.orEmpty()
        copyInput = DispatchRepository.normalizeCopyType(payload.copy)
        sizeInput = payload.resolvedSize.orEmpty()
        qtyInput = (payload.resolvedQty ?: 1).coerceAtLeast(1).toString()
        topColourInput = payload.resolvedTopColour.orEmpty()
        frameColourInput = payload.resolvedFrameColour.orEmpty()
        legSizeInput = payload.resolvedLegSize.orEmpty()
        legHeightInput = payload.resolvedLegHeight.orEmpty()
        legShapeInput = payload.resolvedLegShape.orEmpty()
    }

    val scannedCopy = DispatchRepository.normalizeCopyType(copyInput)
    val isGatePass = DispatchRepository.isGatePassCopy(scannedCopy)
    val accentColor = if (isReturnMode) ReturnAmber else DispatchIndigo
    val scanTimeSlst = remember(payload.orderNo) { DispatchRepository.formatSriLankaDateTime() }

    val buildEditedPayload: () -> QrPayload = {
        val parsedQty = qtyInput.trim().toIntOrNull()?.coerceAtLeast(1) ?: (payload.resolvedQty ?: 1).coerceAtLeast(1)
        val totalUnitsVal = (payload.resolvedTotalUnits ?: parsedQty).coerceAtLeast(parsedQty)
        payload.copy(
            customer = customerInput.trim().ifBlank { "Factory Order" },
            district = districtInput.trim().ifBlank { "Colombo" },
            copy = scannedCopy,
            size = sizeInput.trim(),
            tableSize = sizeInput.trim(),
            qty = parsedQty,
            totalUnits = totalUnitsVal,
            tableCount = totalUnitsVal,
            tableIndex = (selectedTableIdx + 1).coerceAtLeast(1),
            topColour = topColourInput.trim(),
            frameColour = frameColourInput.trim(),
            legSize = legSizeInput.trim(),
            legHeight = legHeightInput.trim(),
            legShape = legShapeInput.trim()
        )
    }

    val triggerConfirm = {
        val updated = buildEditedPayload()
        if (onConfirmWithEdits != null) {
            onConfirmWithEdits(updated, noteText.trim(), restockFlag)
        } else {
            onConfirm()
        }
    }

    // Hold countdown when user enters note typing, edits order details, or during manual order entry
    val isCountdownHeld = isTypingNote || isEditingDetails || isManualConfirmation || isFetchingManualOrder

    val progressAnim = remember(payload.orderNo) { Animatable(1f) }
    LaunchedEffect(isCountdownHeld, payload.orderNo) {
        if (isCountdownHeld) {
            progressAnim.stop()
        } else {
            val remainingMs = (progressAnim.value * 5000f).toInt().coerceAtLeast(250)
            progressAnim.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = remainingMs, easing = LinearEasing)
            )
            if (progressAnim.value <= 0.01f && !isCountdownHeld) {
                triggerConfirm()
            }
        }
    }

    val secondsLeft = ceil(progressAnim.value * 5f).toInt().coerceIn(1, 5)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
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
                .heightIn(max = 720.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {}
                .testTag("auto_confirm_scan_sheet")
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

                Spacer(modifier = Modifier.height(14.dp))

                // Top Countdown Ring + Title + Edit Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("countdown_indicator_box"),
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
                                    sweepAngle = if (isCountdownHeld) 360f else 360f * progressAnim.value,
                                    useCenter = false,
                                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                                )
                            }
                            if (isCountdownHeld) {
                                Icon(
                                    imageVector = Icons.Default.Pause,
                                    contentDescription = "Countdown Held",
                                    tint = accentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Text(
                                    text = "$secondsLeft",
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = accentColor
                                )
                            }
                        }

                        Column {
                            Text(
                                text = when {
                                    isManualConfirmation && isReturnMode -> "MANUAL ENTRY · RETURN"
                                    isManualConfirmation -> "MANUAL ENTRY · DISPATCH"
                                    isReturnMode -> "QR SCANNED · RETURN"
                                    else -> "QR SCANNED · DISPATCH"
                                },
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
                                fontSize = 20.sp,
                                color = Neutral900,
                                letterSpacing = (-0.4).sp
                            )
                        }
                    }

                    // Edit / Done Editing pill button
                    Surface(
                        onClick = {
                            isEditingDetails = !isEditingDetails
                            if (!isEditingDetails) {
                                focusManager.clearFocus()
                            }
                        },
                        shape = RoundedCornerShape(999.dp),
                        color = if (isEditingDetails) accentColor.copy(alpha = 0.14f) else TertiaryFill,
                        modifier = Modifier.testTag("toggle_edit_order_details_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isEditingDetails) Icons.Default.Check else Icons.Default.Edit,
                                contentDescription = "Toggle Edit Details",
                                tint = if (isEditingDetails) accentColor else Neutral900,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (isEditingDetails) "Done" else "Edit",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (isEditingDetails) accentColor else Neutral900
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable middle body for order details, editable fields, and note input
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
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
                            fontSize = 30.sp,
                            color = Neutral900,
                            letterSpacing = (-0.8).sp
                        )
                        CopyTypeBadge(copyName = scannedCopy)
                    }

                    // Database Fetch Status Banner for Manual Order Entry
                    if (isManualConfirmation) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val dbStatusBg = when {
                            isFetchingManualOrder -> DispatchIndigoTint
                            manualOrderFoundInDb -> RestockedEmeraldTint
                            else -> ReturnAmberTint
                        }
                        val dbStatusColor = when {
                            isFetchingManualOrder -> DispatchIndigoText
                            manualOrderFoundInDb -> RestockedEmerald
                            else -> ReturnAmberText
                        }
                        val dbStatusText = when {
                            isFetchingManualOrder -> "Fetching order #${payload.orderNo} details from database..."
                            manualOrderFoundInDb -> "Order details fetched from database · Edit any field below before confirming."
                            else -> "Order #${payload.orderNo} not in database · Enter or edit order details manually below."
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = dbStatusBg,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("manual_order_db_status_banner")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (isFetchingManualOrder) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp,
                                        color = dbStatusColor
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (manualOrderFoundInDb) Icons.Default.CloudDone else Icons.Default.EditNote,
                                        contentDescription = null,
                                        tint = dbStatusColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = dbStatusText,
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    color = dbStatusColor
                                )
                            }
                        }
                    }

                    // Multi-table selector chips when an order has multiple tables in `order_tables`
                    if (manualOrderTables.size > 1) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "SELECT TABLE UNIT (${manualOrderTables.size} IN ORDER)",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = SecondaryLabel,
                            letterSpacing = 0.4.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            manualOrderTables.forEachIndexed { idx, tableRow ->
                                val selected = selectedTableIdx == idx
                                val chipLabel = buildString {
                                    append("Table ${idx + 1}: ")
                                    append(tableRow.table_size?.ifBlank { "Standard" } ?: "Standard")
                                    if (!tableRow.top_colour.isNullOrBlank()) {
                                        append(" (${tableRow.top_colour})")
                                    }
                                }
                                Surface(
                                    onClick = {
                                        selectedTableIdx = idx
                                        sizeInput = tableRow.table_size.orEmpty()
                                        qtyInput = (tableRow.quantity ?: 1).coerceAtLeast(1).toString()
                                        topColourInput = tableRow.top_colour.orEmpty()
                                        frameColourInput = tableRow.frame_colour.orEmpty()
                                        legSizeInput = tableRow.leg_size.orEmpty()
                                        legHeightInput = tableRow.leg_height.orEmpty()
                                        legShapeInput = tableRow.leg_shape.orEmpty()
                                    },
                                    shape = RoundedCornerShape(999.dp),
                                    color = if (selected) accentColor else TertiaryFill,
                                    modifier = Modifier.testTag("manual_table_chip_$idx")
                                ) {
                                    Text(
                                        text = chipLabel,
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (selected) (if (isReturnMode) ReturnAmberInk else Color.White) else Neutral900,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HairlineDivider()

                    if (isEditingDetails) {
                        // Editable Order & Table Specification Form
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "ORDER & TABLE DETAILS (EDITABLE)",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = SecondaryLabel,
                            letterSpacing = 0.4.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Copy Type Selector Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("GATE PASS", "TRANSPORT COPY", "ACCOUNT COPY").forEach { copyOption ->
                                val isSelectedCopy = scannedCopy.equals(copyOption, ignoreCase = true)
                                Surface(
                                    onClick = { copyInput = copyOption },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelectedCopy) Neutral900 else TertiaryFill,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = copyOption,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = if (isSelectedCopy) Color.White else Neutral900,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Row 1: Customer & District
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = customerInput,
                                onValueChange = { customerInput = it },
                                label = { Text("Customer", fontSize = 12.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("confirm_edit_customer_input")
                            )
                            OutlinedTextField(
                                value = districtInput,
                                onValueChange = { districtInput = it },
                                label = { Text("District", fontSize = 12.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("confirm_edit_district_input")
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Row 2: Table Size & Quantity
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = sizeInput,
                                onValueChange = { sizeInput = it },
                                label = { Text("Table Size (e.g. 30x60)", fontSize = 12.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .testTag("confirm_edit_size_input")
                            )
                            OutlinedTextField(
                                value = qtyInput,
                                onValueChange = { qtyInput = it.filter { ch -> ch.isDigit() } },
                                label = { Text("Qty", fontSize = 12.sp) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(0.7f)
                                    .testTag("confirm_edit_qty_input")
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Row 3: Top Colour & Leg/Frame Colour
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = topColourInput,
                                onValueChange = { topColourInput = it },
                                label = { Text("Top Colour", fontSize = 12.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("confirm_edit_top_colour_input")
                            )
                            OutlinedTextField(
                                value = frameColourInput,
                                onValueChange = { frameColourInput = it },
                                label = { Text("Leg Colour", fontSize = 12.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("confirm_edit_frame_colour_input")
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Row 4: Leg Size, Leg Height, Leg Shape
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = legSizeInput,
                                onValueChange = { legSizeInput = it },
                                label = { Text("Leg Size", fontSize = 11.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("confirm_edit_leg_size_input")
                            )
                            OutlinedTextField(
                                value = legHeightInput,
                                onValueChange = { legHeightInput = it },
                                label = { Text("Height", fontSize = 11.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(0.85f)
                                    .testTag("confirm_edit_leg_height_input")
                            )
                            OutlinedTextField(
                                value = legShapeInput,
                                onValueChange = { legShapeInput = it },
                                label = { Text("Leg Shape", fontSize = 11.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1.15f)
                                    .testTag("confirm_edit_leg_shape_input")
                            )
                        }

                        if (isReturnMode) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Restock to inventory",
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = Neutral900
                                )
                                IosSwitch(
                                    checked = restockFlag,
                                    onCheckedChange = { restockFlag = it },
                                    activeColor = RestockedEmerald
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        HairlineDivider()
                    } else {
                        // Standard Compact Summary Rows
                        SheetKeyValueRow(
                            label = "Customer",
                            value = customerInput.ifBlank { "Factory Order" }
                        )
                        HairlineDivider()

                        if (districtInput.isNotBlank()) {
                            SheetKeyValueRow(
                                label = "District",
                                value = districtInput
                            )
                            HairlineDivider()
                        }

                        val dispatchedQty = (qtyInput.toIntOrNull() ?: (payload.resolvedQty ?: 1)).coerceAtLeast(1)
                        val totalOrderUnits = (payload.resolvedTotalUnits ?: dispatchedQty).coerceAtLeast(dispatchedQty)
                        val scannedSize = sizeInput.trim()
                        if (scannedSize.isNotBlank()) {
                            SheetKeyValueRow(
                                label = "Table size",
                                value = "$scannedSize · Qty $dispatchedQty${if (totalOrderUnits > dispatchedQty) " (of $totalOrderUnits)" else ""}"
                            )
                            HairlineDivider()
                        } else {
                            SheetKeyValueRow(
                                label = "Units",
                                value = "$dispatchedQty ${if (dispatchedQty == 1) "table" else "tables"}"
                            )
                            HairlineDivider()
                        }

                        val featureDetails = listOfNotNull(
                            topColourInput.takeIf { it.isNotBlank() }?.let { "$it top" },
                            frameColourInput.takeIf { it.isNotBlank() }?.let { "$it leg" },
                            legSizeInput.takeIf { it.isNotBlank() },
                            legShapeInput.takeIf { it.isNotBlank() },
                            legHeightInput.takeIf { it.isNotBlank() }?.let { "H:$it" }
                        ).joinToString(" · ")
                        if (featureDetails.isNotBlank()) {
                            SheetKeyValueRow(
                                label = "Features",
                                value = featureDetails
                            )
                            HairlineDivider()
                        }

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
                                value = if (restockFlag) "Yes · back to stock" else "No · do not restock",
                                valueColor = if (restockFlag) RestockedEmerald else SecondaryLabel
                            )
                        }

                        HairlineDivider()
                        SheetKeyValueRow(
                            label = "Time (Sri Lanka)",
                            value = scanTimeSlst
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Order Note Input Field (Entering note typing holds countdown until Done)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("confirm_order_note_section")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "ORDER NOTE (OPTIONAL)",
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = SecondaryLabel,
                                    letterSpacing = 0.4.sp
                                )
                                if (isTypingNote) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = ReturnAmberTint
                                    ) {
                                        Text(
                                            text = "TIMER PAUSED",
                                            fontFamily = InterFontFamily,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 9.sp,
                                            color = ReturnAmberText,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            if (isTypingNote) {
                                Text(
                                    text = "Done typing",
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = accentColor,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable {
                                            isTypingNote = false
                                            focusManager.clearFocus()
                                        }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                        .testTag("confirm_note_done_button")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = noteText,
                            onValueChange = {
                                noteText = it
                                isTypingNote = true
                            },
                            placeholder = {
                                Text(
                                    text = "Add a note about this order (holds countdown)...",
                                    fontFamily = InterFontFamily,
                                    fontSize = 13.sp,
                                    color = Neutral400
                                )
                            },
                            singleLine = false,
                            maxLines = 2,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    isTypingNote = false
                                    focusManager.clearFocus()
                                }
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { focusState ->
                                    if (focusState.isFocused) {
                                        isTypingNote = true
                                    }
                                }
                                .testTag("confirm_note_input")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

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
                        isReturnMode -> "Mark returned${if (restockFlag) " & restock" else ""} ($scannedCopy)"
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
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Draining / Held Primary Confirm Button
                val baseButtonColor = if (isReturnMode) Color(0xFFB45309) else Color(0xFF3730A3)
                val fillButtonColor = if (isReturnMode) ReturnAmber else DispatchIndigo
                val buttonText = when {
                    isCountdownHeld && isReturnMode -> "Confirm Return"
                    isCountdownHeld && isGatePass -> "Confirm Dispatch"
                    isCountdownHeld -> "Record Scan"
                    isReturnMode -> "Confirm Return (${secondsLeft}s)"
                    isGatePass -> "Confirm Dispatch (${secondsLeft}s)"
                    else -> "Record Scan (${secondsLeft}s)"
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(baseButtonColor)
                        .drawWithContent {
                            drawRect(
                                color = fillButtonColor,
                                size = Size(
                                    if (isCountdownHeld) size.width else size.width * progressAnim.value,
                                    size.height
                                )
                            )
                            drawContent()
                        }
                        .clickable {
                            focusManager.clearFocus()
                            triggerConfirm()
                        }
                        .testTag("confirm_scan_sheet_button"),
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

                Spacer(modifier = Modifier.height(8.dp))

                // Cancel Button
                Surface(
                    onClick = {
                        focusManager.clearFocus()
                        onCancel()
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = TertiaryFill,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("cancel_scan_sheet_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "Cancel",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
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
    onBack: () -> Unit,
    onSaveNote: ((String) -> Unit)? = null
) {
    BackHandler { onBack() }
    val clipboardManager = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current
    val scannedStr = remember(record.scannedAt) {
        "${DispatchRepository.formatSriLankaDateTime(record.scannedAt)} (SLST)"
    }
    val statusUpper = record.syncStatus.uppercase()
    var noteDraft by remember(record.id, record.notes) { mutableStateOf(record.notes) }
    var noteSavedFeedback by remember { mutableStateOf(false) }

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

            // ORDER NOTE Group (Add or edit note in History Detail)
            Column {
                SectionHeaderLabel(text = "ORDER NOTE")
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CellWhite,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = noteDraft,
                            onValueChange = {
                                noteDraft = it
                                noteSavedFeedback = false
                            },
                            placeholder = {
                                Text(
                                    text = "Add a note about this order...",
                                    fontFamily = InterFontFamily,
                                    fontSize = 14.sp,
                                    color = Neutral400
                                )
                            },
                            maxLines = 3,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("detail_record_note_input")
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (noteSavedFeedback) "Note saved & synced" else "Saved to history & dispatch log",
                                fontFamily = InterFontFamily,
                                fontWeight = if (noteSavedFeedback) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp,
                                color = if (noteSavedFeedback) RestockedEmerald else SecondaryLabel
                            )
                            Button(
                                onClick = {
                                    focusManager.clearFocus()
                                    onSaveNote?.invoke(noteDraft.trim())
                                    noteSavedFeedback = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DispatchIndigo),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .height(38.dp)
                                    .testTag("detail_save_note_button")
                            ) {
                                Text(
                                    text = "Save Note",
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                            }
                        }
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
                            label = "Frame / leg colour",
                            value = record.frameColour.ifBlank { "Matte Black" },
                            colorDot = Color(0xFF1F2937)
                        )
                        if (record.legSize.isNotBlank()) {
                            HairlineDivider()
                            DetailGroupRow(
                                label = "Leg size",
                                value = record.legSize
                            )
                        }
                        if (record.legHeight.isNotBlank()) {
                            HairlineDivider()
                            DetailGroupRow(
                                label = "Leg height",
                                value = record.legHeight
                            )
                        }
                        if (record.legShape.isNotBlank()) {
                            HairlineDivider()
                            DetailGroupRow(
                                label = "Leg shape",
                                value = record.legShape
                            )
                        }
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
                                text = "Combined duplicate key (7 table features)",
                                fontFamily = InterFontFamily,
                                fontSize = 12.sp,
                                color = SecondaryLabel
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = record.duplicateKey.ifBlank {
                                    "${record.orderNo} | ${record.copy} | ${record.size}"
                                },
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
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
  "scanned_at_sri_lanka": "${DispatchRepository.formatSriLankaDateTime(record.scannedAt)}",
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
