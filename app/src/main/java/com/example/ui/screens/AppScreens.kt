package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AssignmentReturn
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.ScannedRecord
import com.example.ui.theme.*
import com.example.viewmodel.DispatchViewModel
import com.example.viewmodel.OperationMode
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun SegmentedModeSwitcher(
    currentMode: OperationMode,
    onModeSelected: (OperationMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDispatch = currentMode == OperationMode.DISPATCH
    val dispatchBg by animateColorAsState(
        targetValue = if (isDispatch) DispatchIndigo else Color.Transparent,
        animationSpec = tween(250),
        label = "dispatchSegmentBg"
    )
    val returnBg by animateColorAsState(
        targetValue = if (!isDispatch) ReturnAmber else Color.Transparent,
        animationSpec = tween(250),
        label = "returnSegmentBg"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = TertiaryFill
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Dispatch Segment
            Surface(
                onClick = { onModeSelected(OperationMode.DISPATCH) },
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .testTag("mode_switch_dispatch"),
                shape = RoundedCornerShape(8.dp),
                color = dispatchBg,
                shadowElevation = if (isDispatch) 2.dp else 0.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalShipping,
                        contentDescription = "Dispatch Mode",
                        tint = if (isDispatch) Color.White else Neutral900,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Dispatch",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (isDispatch) Color.White else Neutral900
                    )
                }
            }

            // Return Segment
            Surface(
                onClick = { onModeSelected(OperationMode.RETURN) },
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .testTag("mode_switch_return"),
                shape = RoundedCornerShape(8.dp),
                color = returnBg,
                shadowElevation = if (!isDispatch) 2.dp else 0.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Return Mode",
                        tint = if (!isDispatch) ReturnAmberInk else Neutral900,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Return",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (!isDispatch) ReturnAmberInk else Neutral900
                    )
                }
            }
        }
    }
}

@Composable
fun ScannerOverlay(
    modifier: Modifier = Modifier,
    accentColor: Color = DispatchIndigo
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanLaser")
    val laserProgress by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laserLineY"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 36.dp, vertical = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 3.5.dp.toPx()
            val cornerLen = 22.dp.toPx()
            val radius = 12.dp.toPx()

            // Top-Left corner
            drawArc(
                color = accentColor,
                startAngle = 180f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(0f, 0f),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth)
            )
            drawLine(accentColor, Offset(0f, radius), Offset(0f, cornerLen), strokeWidth)
            drawLine(accentColor, Offset(radius, 0f), Offset(cornerLen, 0f), strokeWidth)

            // Top-Right corner
            drawArc(
                color = accentColor,
                startAngle = 270f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(size.width - radius * 2, 0f),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth)
            )
            drawLine(accentColor, Offset(size.width, radius), Offset(size.width, cornerLen), strokeWidth)
            drawLine(accentColor, Offset(size.width - radius, 0f), Offset(size.width - cornerLen, 0f), strokeWidth)

            // Bottom-Left corner
            drawArc(
                color = accentColor,
                startAngle = 90f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(0f, size.height - radius * 2),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth)
            )
            drawLine(accentColor, Offset(0f, size.height - radius), Offset(0f, size.height - cornerLen), strokeWidth)
            drawLine(accentColor, Offset(radius, size.height), Offset(cornerLen, size.height), strokeWidth)

            // Bottom-Right corner
            drawArc(
                color = accentColor,
                startAngle = 0f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(size.width - radius * 2, size.height - radius * 2),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth)
            )
            drawLine(accentColor, Offset(size.width, size.height - radius), Offset(size.width, size.height - cornerLen), strokeWidth)
            drawLine(accentColor, Offset(size.width - radius, size.height), Offset(size.width - cornerLen, size.height), strokeWidth)

            // Glowing Horizontal Scan Line
            val yPos = size.height * laserProgress
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        accentColor.copy(alpha = 0.95f),
                        Color.White,
                        accentColor.copy(alpha = 0.95f),
                        Color.Transparent
                    )
                ),
                start = Offset(8.dp.toPx(), yPos),
                end = Offset(size.width - 8.dp.toPx(), yPos),
                strokeWidth = 2.5.dp.toPx()
            )
        }
    }
}

@Composable
fun AppNavigator(viewModel: DispatchViewModel, modifier: Modifier = Modifier) {
    var currentTab by remember { mutableIntStateOf(0) }
    val allRecords by viewModel.allRecordsState.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val syncMessage by viewModel.syncMessage.collectAsStateWithLifecycle()
    val activeToast by viewModel.activeToast.collectAsStateWithLifecycle()

    val pendingCount = remember(allRecords) {
        allRecords.count { it.syncStatus.equals("PENDING", ignoreCase = true) }
    }

    Scaffold(
        modifier = modifier,
        containerColor = GroupedBg,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CellWhite.copy(alpha = 0.96f))
            ) {
                HairlineDivider()
                NavigationBar(
                    modifier = Modifier.testTag("app_navigation_bar"),
                    containerColor = Color.Transparent,
                    tonalElevation = 0.dp
                ) {
                    val navColors = NavigationBarItemDefaults.colors(
                        selectedIconColor = DispatchIndigo,
                        selectedTextColor = DispatchIndigo,
                        indicatorColor = Color.Transparent,
                        unselectedIconColor = SecondaryLabel,
                        unselectedTextColor = SecondaryLabel
                    )

                    NavigationBarItem(
                        selected = currentTab == 0,
                        onClick = { currentTab = 0 },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Scanner"
                            )
                        },
                        label = {
                            Text(
                                "Scanner",
                                fontFamily = InterFontFamily,
                                fontWeight = if (currentTab == 0) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.sp
                            )
                        },
                        colors = navColors,
                        modifier = Modifier.testTag("nav_item_scanner")
                    )

                    NavigationBarItem(
                        selected = currentTab == 1,
                        onClick = { currentTab = 1 },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (pendingCount > 0) {
                                        Badge(
                                            containerColor = DuplicateRed,
                                            contentColor = Color.White
                                        ) {
                                            Text(
                                                text = "$pendingCount",
                                                fontFamily = InterFontFamily,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CloudUpload,
                                    contentDescription = "Pending Pack list"
                                )
                            }
                        },
                        label = {
                            Text(
                                "Pending",
                                fontFamily = InterFontFamily,
                                fontWeight = if (currentTab == 1) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.sp
                            )
                        },
                        colors = navColors,
                        modifier = Modifier.testTag("nav_item_pending")
                    )

                    NavigationBarItem(
                        selected = currentTab == 2,
                        onClick = { currentTab = 2 },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = "History Log"
                            )
                        },
                        label = {
                            Text(
                                "History",
                                fontFamily = InterFontFamily,
                                fontWeight = if (currentTab == 2) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.sp
                            )
                        },
                        colors = navColors,
                        modifier = Modifier.testTag("nav_item_history")
                    )

                    NavigationBarItem(
                        selected = currentTab == 3,
                        onClick = { currentTab = 3 },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = "Settings config"
                            )
                        },
                        label = {
                            Text(
                                "Settings",
                                fontFamily = InterFontFamily,
                                fontWeight = if (currentTab == 3) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.sp
                            )
                        },
                        colors = navColors,
                        modifier = Modifier.testTag("nav_item_settings")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(GroupedBg)
                .padding(innerPadding)
        ) {
            when (currentTab) {
                0 -> ScannerScreen(
                    viewModel = viewModel,
                    onOpenSettings = { currentTab = 3 }
                )
                1 -> PendingScreen(viewModel = viewModel)
                2 -> HistoryScreen(viewModel = viewModel)
                3 -> SettingsScreen(viewModel = viewModel)
            }

            // Top Floating Toast (States 6 & 7 in Key States)
            TopFloatingToast(
                toast = activeToast,
                onDismiss = { viewModel.dismissToast() },
                modifier = Modifier.align(Alignment.TopCenter)
            )

            // Global syncing loader overlay
            if (isSyncing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = CellWhite,
                        shadowElevation = 12.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = DispatchIndigo)
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = syncMessage ?: "Syncing with Supabase...",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Neutral900,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VehicleSelectionCard(
    viewModel: DispatchViewModel,
    accentColor: Color,
    isReturnMode: Boolean,
    onOpenPickerSheet: () -> Unit
) {
    val selectedOption by viewModel.selectedVehicleOption.collectAsStateWithLifecycle()
    val pickMeNumber by viewModel.pickMeVehicleNumber.collectAsStateWithLifecycle()
    val customNumber by viewModel.customVehicleNumber.collectAsStateWithLifecycle()

    val effectiveVehicle = remember(selectedOption, pickMeNumber, customNumber) {
        viewModel.getEffectiveVehicleNumber()
    }
    val displayVehicle = remember(effectiveVehicle) {
        effectiveVehicle.replace(" (", " · ").replace(")", "")
    }
    val assignedSummary = remember(selectedOption, pickMeNumber, customNumber, isReturnMode) {
        if (isReturnMode) {
            "Return logged against $effectiveVehicle"
        } else {
            viewModel.getAssignedDeliverySummary()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("vehicle_selection_card")
    ) {
        SectionHeaderLabel(text = "TRANSPORT")

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = CellWhite,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                // Main Vehicle Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenPickerSheet() }
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                        .testTag("vehicle_dropdown_field"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(accentColor, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalShipping,
                                contentDescription = "Vehicle",
                                tint = if (isReturnMode) ReturnAmberInk else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = "Vehicle",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 17.sp,
                            color = Neutral900
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = displayVehicle,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 15.sp,
                            color = SecondaryLabel,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("active_vehicle_badge")
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Choose vehicle",
                            tint = Color(0xFFC7C7CC),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Conditional Row 2: Pick Me Plate Number Input (matches State 9 in Key States)
                if (selectedOption == DispatchViewModel.VEHICLE_PICK_ME) {
                    HairlineDivider(startIndent = 58.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Plate no.",
                            fontFamily = InterFontFamily,
                            fontSize = 16.sp,
                            color = Neutral900,
                            modifier = Modifier.padding(start = 44.dp)
                        )
                        BasicTextField(
                            value = pickMeNumber,
                            onValueChange = { viewModel.setPickMeVehicleNumber(it) },
                            singleLine = true,
                            textStyle = TextStyle(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Neutral900,
                                textAlign = TextAlign.End
                            ),
                            cursorBrush = SolidColor(accentColor),
                            decorationBox = { innerTextField ->
                                Box(contentAlignment = Alignment.CenterEnd) {
                                    if (pickMeNumber.isEmpty()) {
                                        Text(
                                            text = "CAA-8910",
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontSize = 15.sp,
                                            color = Neutral400
                                        )
                                    }
                                    innerTextField()
                                }
                            },
                            modifier = Modifier
                                .widthIn(min = 120.dp)
                                .testTag("pick_me_vehicle_number_input")
                        )
                    }
                }

                // Conditional Row 2: Custom Vehicle Number Input
                if (selectedOption == DispatchViewModel.VEHICLE_CUSTOM_OPTION) {
                    HairlineDivider(startIndent = 14.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
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
                            onClick = { viewModel.addAndSelectCustomVehicle(customNumber) },
                            enabled = customNumber.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("add_custom_vehicle_button")
                        ) {
                            Text("Save", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = assignedSummary,
            fontFamily = InterFontFamily,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            color = SecondaryLabel,
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .testTag("driver_assignment_badge")
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ScannerScreen(
    viewModel: DispatchViewModel,
    onOpenSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val currentMode by viewModel.currentMode.collectAsStateWithLifecycle()
    val scannedPayload by viewModel.scannedPayload.collectAsStateWithLifecycle()
    val scanError by viewModel.scanError.collectAsStateWithLifecycle()
    val rejectTriggerCount by viewModel.rejectTriggerCount.collectAsStateWithLifecycle()
    val scannerName by viewModel.scannerName.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()

    // Dispatch Mode Manual Input states
    val manualDispatchOrderInput by viewModel.manualDispatchOrderInput.collectAsStateWithLifecycle()
    val manualDispatchStatus by viewModel.manualDispatchStatus.collectAsStateWithLifecycle()
    val isProcessingDispatch by viewModel.isProcessingDispatch.collectAsStateWithLifecycle()

    // Return Mode Manual Input states
    val manualOrderInput by viewModel.manualOrderInput.collectAsStateWithLifecycle()
    val manualRestocked by viewModel.manualRestocked.collectAsStateWithLifecycle()
    val manualReturnStatus by viewModel.manualReturnStatus.collectAsStateWithLifecycle()
    val isProcessingReturn by viewModel.isProcessingReturn.collectAsStateWithLifecycle()

    val selectedVehicleOption by viewModel.selectedVehicleOption.collectAsStateWithLifecycle()
    val pickMeVehicleNumber by viewModel.pickMeVehicleNumber.collectAsStateWithLifecycle()
    val customVehicleNumber by viewModel.customVehicleNumber.collectAsStateWithLifecycle()
    val effectiveVehicle = remember(selectedVehicleOption, pickMeVehicleNumber, customVehicleNumber) {
        viewModel.getEffectiveVehicleNumber()
    }
    val shortVehicleCode = remember(effectiveVehicle) {
        effectiveVehicle.substringBefore("(").trim()
    }

    val isReturnMode = currentMode == OperationMode.RETURN
    val accentColor by animateColorAsState(
        targetValue = if (isReturnMode) ReturnAmber else DispatchIndigo,
        animationSpec = tween(300),
        label = "modeAccentColor"
    )

    var showVehicleSheet by remember { mutableStateOf(false) }
    var torchEnabled by remember { mutableStateOf(false) }

    // 420ms Horizontal Reject Shake Animation (Motion spec #5)
    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(rejectTriggerCount) {
        if (rejectTriggerCount > 0) {
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 420
                    -14f at 70
                    14f at 140
                    -10f at 210
                    10f at 280
                    -5f at 350
                    0f at 420
                }
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // Top Status Pill & Settings Gear Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = TertiaryFill
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    color = if (isOnline) Color(0xFF22C55E) else OfflineOrange,
                                    shape = CircleShape
                                )
                        )
                        Text(
                            text = "${scannerName.ifBlank { "Station 01" }} · ${if (isOnline) "Online" else "Offline"}",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = Neutral900
                        )
                    }
                }

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(36.dp)
                        .background(TertiaryFill, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "Header Settings",
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Large iOS Title & Subtitle
            Text(
                text = if (isReturnMode) "Returns" else "Dispatch",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 34.sp,
                lineHeight = 40.sp,
                color = Neutral900,
                letterSpacing = (-0.8).sp
            )
            Text(
                text = if (isReturnMode) {
                    "Incoming · return & restock to factory"
                } else {
                    "Outgoing · only GATE PASS assigns the driver"
                },
                fontFamily = InterFontFamily,
                fontSize = 13.sp,
                color = SecondaryLabel
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Segmented Mode Switcher
            SegmentedModeSwitcher(
                currentMode = currentMode,
                onModeSelected = { viewModel.setMode(it) }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // TRANSPORT Section (Vehicle Selector)
            VehicleSelectionCard(
                viewModel = viewModel,
                accentColor = accentColor,
                isReturnMode = isReturnMode,
                onOpenPickerSheet = { showVehicleSheet = true }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Reject Banner (States 4 & 5: Duplicate denied / Unauthorized copy) OR Live Camera Viewport
            AnimatedVisibility(
                visible = scanError != null,
                enter = fadeIn(tween(180)) + expandVertically(),
                exit = fadeOut(tween(150)) + shrinkVertically()
            ) {
                scanError?.let { err ->
                    val isUnauthorized = err.startsWith("Unauthorized", ignoreCase = true)
                    val lines = err.lines()
                    val primaryLine = lines.firstOrNull().orEmpty()
                        .removePrefix("Duplicate Denied:")
                        .removePrefix("Unauthorized:")
                        .trim()
                    val secondaryLine = lines.drop(1).joinToString(" ").trim()

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFFFF5F5),
                        border = BorderStroke(1.5.dp, DuplicateRed),
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
                            .padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(DuplicateRed, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isUnauthorized) Icons.Default.Block else Icons.Default.Close,
                                    contentDescription = "Alert",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isUnauthorized) "Unauthorized copy" else "Duplicate denied",
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = DuplicateRed
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = primaryLine,
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = Neutral900,
                                    lineHeight = 18.sp
                                )
                                if (secondaryLine.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = secondaryLine,
                                        fontFamily = InterFontFamily,
                                        fontSize = 12.sp,
                                        color = SecondaryLabel
                                    )
                                }
                            }
                            IconButton(
                                onClick = { viewModel.dismissScanError() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss error",
                                    tint = SecondaryLabel,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Camera QR Viewport (r22)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(205.dp)
                    .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
                    .clip(RoundedCornerShape(22.dp))
                    .background(Neutral950),
                contentAlignment = Alignment.Center
            ) {
                if (cameraPermissionState.status.isGranted) {
                    CameraScannerView(
                        modifier = Modifier.fillMaxSize(),
                        torchEnabled = torchEnabled,
                        onQrScanned = { raw ->
                            viewModel.onQrScanned(raw, context)
                        }
                    )
                    ScannerOverlay(
                        modifier = Modifier.fillMaxSize(),
                        accentColor = accentColor
                    )
                } else {
                    ScannerOverlay(
                        modifier = Modifier.fillMaxSize(),
                        accentColor = accentColor
                    )
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode2,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { cameraPermissionState.launchPermissionRequest() },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            shape = RoundedCornerShape(999.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("request_permission_button")
                        ) {
                            Text(
                                text = "Grant Camera Permission",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isReturnMode) ReturnAmberInk else Color.White
                            )
                        }
                    }
                }

                // Top-Center "Align QR code" Pill
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Color(0xFF27272A).copy(alpha = 0.82f),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 14.dp)
                ) {
                    Text(
                        text = "Align QR code",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                    )
                }

                // Bottom-Left Mode Pill inside Camera
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = accentColor,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 14.dp, bottom = 14.dp)
                ) {
                    Text(
                        text = if (isReturnMode) "RETURN" else "DISPATCH",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp,
                        color = if (isReturnMode) ReturnAmberInk else Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }

                // Bottom-Right Flashlight / Torch Toggle Button inside Camera
                IconButton(
                    onClick = { torchEnabled = !torchEnabled },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 14.dp, bottom = 12.dp)
                        .size(38.dp)
                        .background(
                            color = if (torchEnabled) Color.White else Color(0xFF27272A).copy(alpha = 0.85f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = if (torchEnabled) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                        contentDescription = "Toggle Flashlight",
                        tint = if (torchEnabled) Neutral900 else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // MANUAL ENTRY Section
            SectionHeaderLabel(text = "MANUAL ENTRY")

            if (!isReturnMode) {
                // DISPATCH MODE MANUAL ENTRY
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CellWhite,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_dispatch_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .background(Color(0xFF8E8E93), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tag,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = "Order",
                                fontFamily = InterFontFamily,
                                fontSize = 17.sp,
                                color = Neutral900
                            )
                        }

                        BasicTextField(
                            value = manualDispatchOrderInput,
                            onValueChange = { viewModel.setManualDispatchOrderInput(it) },
                            singleLine = true,
                            textStyle = TextStyle(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = Neutral900,
                                textAlign = TextAlign.End
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    viewModel.processManualDispatch(context)
                                }
                            ),
                            cursorBrush = SolidColor(DispatchIndigo),
                            decorationBox = { innerTextField ->
                                Box(contentAlignment = Alignment.CenterEnd) {
                                    if (manualDispatchOrderInput.isEmpty()) {
                                        Text(
                                            text = "1031",
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 16.sp,
                                            color = Neutral400
                                        )
                                    }
                                    innerTextField()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 16.dp)
                                .testTag("manual_dispatch_order_input")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val dispatchBtnLabel = if (manualDispatchOrderInput.isNotBlank()) {
                    "Dispatch #${manualDispatchOrderInput.trim()} · $shortVehicleCode"
                } else {
                    "Dispatch Order · $shortVehicleCode"
                }

                Button(
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.processManualDispatch(context)
                    },
                    enabled = !isProcessingDispatch && manualDispatchOrderInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DispatchIndigo,
                        disabledContainerColor = DispatchIndigo.copy(alpha = 0.55f),
                        contentColor = Color.White,
                        disabledContentColor = Color.White.copy(alpha = 0.85f)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("process_dispatch_button")
                ) {
                    if (isProcessingDispatch) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Updating Factory Database...",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    } else {
                        Text(
                            text = dispatchBtnLabel,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    }
                }

                if (manualDispatchStatus != null && scanError == null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = manualDispatchStatus.orEmpty(),
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = if (manualDispatchStatus?.startsWith("Please") == true ||
                            manualDispatchStatus?.startsWith("Alert") == true ||
                            manualDispatchStatus?.startsWith("Duplicate") == true
                        ) DuplicateRed else DispatchIndigo,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            } else {
                // RETURN MODE MANUAL ENTRY
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CellWhite,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_return_card")
                ) {
                    Column {
                        // Row 1: Order #
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .background(Color(0xFF8E8E93), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tag,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = "Order",
                                    fontFamily = InterFontFamily,
                                    fontSize = 17.sp,
                                    color = Neutral900
                                )
                            }

                            BasicTextField(
                                value = manualOrderInput,
                                onValueChange = { viewModel.setManualOrderInput(it) },
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = Neutral900,
                                    textAlign = TextAlign.End
                                ),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        focusManager.clearFocus()
                                        viewModel.processManualReturn(context)
                                    }
                                ),
                                cursorBrush = SolidColor(ReturnAmber),
                                decorationBox = { innerTextField ->
                                    Box(contentAlignment = Alignment.CenterEnd) {
                                        if (manualOrderInput.isEmpty()) {
                                            Text(
                                                text = "1031",
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 16.sp,
                                                color = Neutral400
                                            )
                                        }
                                        innerTextField()
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 16.dp)
                                    .testTag("manual_order_number_input")
                            )
                        }

                        HairlineDivider(startIndent = 56.dp)

                        // Row 2: Restock to inventory switch
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .background(RestockedEmerald, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Inventory2,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Restock to inventory",
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 16.sp,
                                        color = Neutral900
                                    )
                                    Text(
                                        text = "Units go back on the stock count",
                                        fontFamily = InterFontFamily,
                                        fontSize = 12.sp,
                                        color = SecondaryLabel
                                    )
                                }
                            }

                            IosSwitch(
                                checked = manualRestocked,
                                onCheckedChange = { viewModel.setManualRestocked(it) },
                                activeColor = RestockedEmerald
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val returnBtnLabel = if (manualOrderInput.isNotBlank()) {
                    "Return & Restock #${manualOrderInput.trim()}"
                } else {
                    "Return & Restock Order"
                }

                Button(
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.processManualReturn(context)
                    },
                    enabled = !isProcessingReturn && manualOrderInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ReturnAmber,
                        disabledContainerColor = ReturnAmber.copy(alpha = 0.55f),
                        contentColor = ReturnAmberInk,
                        disabledContentColor = ReturnAmberInk.copy(alpha = 0.7f)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("process_return_button")
                ) {
                    if (isProcessingReturn) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = ReturnAmberInk,
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Updating Factory Database...",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    } else {
                        Text(
                            text = returnBtnLabel,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    }
                }

                if (manualReturnStatus != null && scanError == null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = manualReturnStatus.orEmpty(),
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = if (manualReturnStatus?.startsWith("Please") == true ||
                            manualReturnStatus?.startsWith("Alert") == true ||
                            manualReturnStatus?.startsWith("Duplicate") == true
                        ) DuplicateRed else RestockedEmerald,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Torn or smudged label? Type the order number.\nManual entries are recorded as GATE PASS.",
                fontFamily = InterFontFamily,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = SecondaryLabel,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Vehicle Picker Bottom Sheet (State 8)
        if (showVehicleSheet) {
            VehiclePickerSheet(
                viewModel = viewModel,
                onDismiss = { showVehicleSheet = false }
            )
        }

        // 5-Second Draining Auto-Confirm QR Scan Sheet (States 1, 2, 3)
        if (scannedPayload != null) {
            AutoConfirmScanSheet(
                payload = scannedPayload!!,
                isReturnMode = isReturnMode,
                effectiveVehicle = effectiveVehicle,
                onConfirm = {
                    viewModel.recordDispatch(context, onSuccess = {}, onDuplicate = {})
                },
                onCancel = {
                    viewModel.clearScannedData()
                }
            )
        }
    }
}

@Composable
fun PendingScreen(viewModel: DispatchViewModel) {
    val context = LocalContext.current
    val allRecords by viewModel.allRecordsState.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val syncMessage by viewModel.syncMessage.collectAsStateWithLifecycle()
    val lastSyncTime by viewModel.lastSyncTime.collectAsStateWithLifecycle()
    val lastSyncSummary by viewModel.lastSyncSummary.collectAsStateWithLifecycle()

    val pendingRecords = remember(allRecords) {
        allRecords.filter { it.syncStatus.equals("PENDING", ignoreCase = true) }
    }

    val totalCount = lastSyncSummary?.total ?: allRecords.size
    val syncedCount = lastSyncSummary?.synced ?: allRecords.count { it.syncStatus.equals("SYNCED", true) }
    val dupCount = lastSyncSummary?.duplicates ?: allRecords.count { it.syncStatus.equals("DUPLICATE", true) }
    val failedCount = lastSyncSummary?.failures ?: allRecords.count { it.syncStatus.equals("FAILED", true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GroupedBg)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        // Top Action Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Button(
                onClick = { viewModel.forceSyncPending() },
                enabled = !isSyncing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DispatchIndigo,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(999.dp),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                modifier = Modifier
                    .height(38.dp)
                    .testTag("sync_all_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Sync All",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Sync All",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        // Large Title
        Text(
            text = "Pending",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 34.sp,
            color = Neutral900,
            letterSpacing = (-0.8).sp
        )
        Text(
            text = "${pendingRecords.size} ${if (pendingRecords.size == 1) "scan" else "scans"} waiting on this phone",
            fontFamily = InterFontFamily,
            fontSize = 15.sp,
            color = SecondaryLabel
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Offline Reassurance Banner
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = OfflineOrangeTint,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(OfflineOrange, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.WifiOff,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (pendingRecords.isNotEmpty()) "No connection" else "Auto-sync ready",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = Color(0xFF7C2D12)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Scans are safe on this phone. They sync on their own when the signal is back.",
                        fontFamily = InterFontFamily,
                        fontSize = 14.sp,
                        lineHeight = 19.sp,
                        color = Color(0xFF9A3412)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // LAST SYNC 4-Column Card
        SectionHeaderLabel(
            text = "LAST SYNC · ${lastSyncTime ?: SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())}"
        )
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = CellWhite,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SyncStatColumn(
                    count = "$totalCount",
                    label = "Total",
                    color = Neutral900,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .width(0.5.dp)
                        .height(34.dp)
                        .background(SeparatorHairline)
                )
                SyncStatColumn(
                    count = "$syncedCount",
                    label = "Synced",
                    color = RestockedEmerald,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .width(0.5.dp)
                        .height(34.dp)
                        .background(SeparatorHairline)
                )
                SyncStatColumn(
                    count = "$dupCount",
                    label = "Duplicate",
                    color = DuplicateRed,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .width(0.5.dp)
                        .height(34.dp)
                        .background(SeparatorHairline)
                )
                SyncStatColumn(
                    count = "$failedCount",
                    label = "Failed",
                    color = DuplicateRed,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (!syncMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = syncMessage.orEmpty(),
                fontFamily = InterFontFamily,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = SecondaryLabel,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (pendingRecords.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(RestockedEmeraldTint, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "All Synced",
                            tint = RestockedEmerald,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "All Scans Synced",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = Neutral900
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "No offline records waiting in the queue.",
                        fontFamily = InterFontFamily,
                        fontSize = 14.sp,
                        color = SecondaryLabel,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            SectionHeaderLabel(text = "WAITING · ${pendingRecords.size}")
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = CellWhite,
                modifier = Modifier.fillMaxWidth()
            ) {
                LazyColumn {
                    itemsIndexed(
                        items = pendingRecords,
                        key = { _, item -> item.id }
                    ) { index, record ->
                        SwipeablePendingRow(
                            record = record,
                            onRetry = { viewModel.retrySingleRecord(record, context) },
                            onDelete = { viewModel.deleteSingleRecord(record.id) }
                        )
                        if (index < pendingRecords.lastIndex) {
                            HairlineDivider(startIndent = 64.dp)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Swipe left on a scan to retry or delete it.",
                fontFamily = InterFontFamily,
                fontSize = 12.sp,
                color = SecondaryLabel,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
private fun SyncStatColumn(
    count: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = count,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 22.sp,
            color = color
        )
        Text(
            text = label,
            fontFamily = InterFontFamily,
            fontSize = 12.sp,
            color = color.copy(alpha = 0.85f)
        )
    }
}

@Composable
private fun SwipeablePendingRow(
    record: ScannedRecord,
    onRetry: () -> Unit,
    onDelete: () -> Unit
) {
    val isReturn = record.mode.equals("RETURN", ignoreCase = true)
    val timeStr = remember(record.scannedAt) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(record.scannedAt))
    }
    var offsetX by remember { mutableFloatStateOf(0f) }
    val animatedOffsetX by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "pendingSwipeOffset"
    )
    val maxRevealPx = 360f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        // Background Retry + Delete actions
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(CellWhite),
            horizontalArrangement = Arrangement.End
        ) {
            Box(
                modifier = Modifier
                    .width(76.dp)
                    .fillMaxHeight()
                    .background(DispatchIndigo)
                    .clickable {
                        offsetX = 0f
                        onRetry()
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Retry",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Retry",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }
            }
            Box(
                modifier = Modifier
                    .width(76.dp)
                    .fillMaxHeight()
                    .background(DuplicateRed)
                    .clickable {
                        offsetX = 0f
                        onDelete()
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Delete",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }
            }
        }

        // Foreground Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
                .background(CellWhite)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            offsetX = if (offsetX < -maxRevealPx / 2f) -maxRevealPx else 0f
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            offsetX = (offsetX + dragAmount).coerceIn(-maxRevealPx, 0f)
                        }
                    )
                }
                .clickable {
                    offsetX = if (offsetX == 0f) -maxRevealPx else 0f
                }
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(
                        color = if (isReturn) ReturnAmber else DispatchIndigo,
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isReturn) Icons.AutoMirrored.Filled.Undo else Icons.Default.LocalShipping,
                    contentDescription = null,
                    tint = if (isReturn) ReturnAmberInk else Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "#${record.orderNo}",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp,
                        color = Neutral900
                    )
                    Text(
                        text = record.copy,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = Neutral700
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = listOfNotNull(
                        record.customer.ifBlank { null },
                        record.vehicleNumber.substringBefore("(").trim().ifBlank { null },
                        "Table ${record.tableIndex}/${record.tableCount.coerceAtLeast(1)}"
                    ).joinToString(" · "),
                    fontFamily = InterFontFamily,
                    fontSize = 13.sp,
                    color = SecondaryLabel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = timeStr,
                    fontFamily = InterFontFamily,
                    fontSize = 13.sp,
                    color = SecondaryLabel
                )
                Spacer(modifier = Modifier.height(4.dp))
                SyncStatusBadge(status = "PENDING")
            }
        }
    }
}

@Composable
fun HistoryScreen(viewModel: DispatchViewModel) {
    val history by viewModel.scannedHistory.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val historyModeFilter by viewModel.historyModeFilter.collectAsStateWithLifecycle()
    val scannerName by viewModel.scannerName.collectAsStateWithLifecycle()
    var selectedRecordDetail by remember { mutableStateOf<ScannedRecord?>(null) }

    val context = LocalContext.current
    var isClearing by remember { mutableStateOf(false) }

    if (selectedRecordDetail != null) {
        RecordDetailScreen(
            record = selectedRecordDetail!!,
            onBack = { selectedRecordDetail = null }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GroupedBg)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        // Top Right Clear & Sync Pill Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Surface(
                onClick = {
                    if (!isClearing) {
                        isClearing = true
                        viewModel.clearTodayLogAndSync(
                            context = context,
                            onSuccess = { isClearing = false },
                            onError = { isClearing = false }
                        )
                    }
                },
                shape = RoundedCornerShape(999.dp),
                color = Color(0xFFFEE2E2)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isClearing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = Color(0xFFB91C1C),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Clear & Sync",
                            tint = Color(0xFFB91C1C),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "Clear & Sync",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFFB91C1C)
                    )
                }
            }
        }

        // Large Title
        Text(
            text = "History",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 34.sp,
            color = Neutral900,
            letterSpacing = (-0.8).sp
        )
        Text(
            text = "Today · ${scannerName.ifBlank { "Station 01" }} · ${history.size} shown",
            fontFamily = InterFontFamily,
            fontSize = 15.sp,
            color = SecondaryLabel
        )

        Spacer(modifier = Modifier.height(12.dp))

        // iOS Search Bar
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = TertiaryFill,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = SecondaryLabel,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = InterFontFamily,
                        fontSize = 15.sp,
                        color = Neutral900
                    ),
                    cursorBrush = SolidColor(DispatchIndigo),
                    decorationBox = { innerTextField ->
                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Order, customer, district, vehicle, copy",
                                    fontFamily = InterFontFamily,
                                    fontSize = 15.sp,
                                    color = SecondaryLabel,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            innerTextField()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("search_history_input")
                )
                if (searchQuery.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Default.Cancel,
                        contentDescription = "Clear search",
                        tint = SecondaryLabel,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { viewModel.onSearchQueryChanged("") }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3-Way iOS Segmented Filter Bar (All | Dispatches | Returns)
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = TertiaryFill,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val filters = listOf(
                    "ALL" to "All",
                    "DISPATCH" to "Dispatches",
                    "RETURN" to "Returns"
                )
                filters.forEach { (key, label) ->
                    val selected = historyModeFilter == key
                    Surface(
                        onClick = { viewModel.setHistoryModeFilter(key) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (selected) CellWhite else Color.Transparent,
                        shadowElevation = if (selected) 1.dp else 0.dp,
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = label,
                                fontFamily = InterFontFamily,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp,
                                color = Neutral900
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (history.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.History,
                        contentDescription = "Empty",
                        tint = Neutral400,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "No Scanned Logs Found",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Neutral900
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Scanned dispatches and returns appear in this audit log.",
                        fontFamily = InterFontFamily,
                        fontSize = 13.sp,
                        color = SecondaryLabel,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            SectionHeaderLabel(text = "TODAY")
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = CellWhite,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
            ) {
                LazyColumn {
                    itemsIndexed(
                        items = history,
                        key = { _, rec -> rec.id }
                    ) { index, record ->
                        HistoryItemCard(
                            record = record,
                            onClick = { selectedRecordDetail = record }
                        )
                        if (index < history.lastIndex) {
                            HairlineDivider(startIndent = 62.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryItemCard(record: ScannedRecord, onClick: () -> Unit) {
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val formattedTime = remember(record.scannedAt) { timeFormatter.format(Date(record.scannedAt)) }
    val statusUpper = record.syncStatus.uppercase()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 14.dp)
            .testTag("history_item_${record.id}"),
        verticalAlignment = Alignment.Top
    ) {
        // Left Status Circle Icon
        when (statusUpper) {
            "SYNCED" -> {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(RestockedEmerald, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Synced",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            "DUPLICATE" -> {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(DuplicateRed, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Duplicate",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            "FAILED" -> {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .border(1.5.dp, DuplicateRed.copy(alpha = 0.7f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "!",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = DuplicateRed
                    )
                }
            }
            else -> {
                // PENDING outlined orange clock
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .border(1.5.dp, OfflineOrange, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Pending",
                        tint = OfflineOrange,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Center Content
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "#${record.orderNo}",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    color = Neutral900
                )
                ModePillBadge(mode = record.mode)
                if (record.restocked) {
                    RestockedPillBadge()
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = "${record.copy.ifBlank { "GATE PASS" }} · Table ${record.tableIndex}/${record.tableCount.coerceAtLeast(1)}",
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = Neutral800
            )

            Spacer(modifier = Modifier.height(2.dp))

            val metaLine = listOfNotNull(
                record.customer.ifBlank { null },
                record.vehicleNumber.substringBefore("(").trim().ifBlank { null },
                record.district.ifBlank { null }
            ).joinToString(" · ")

            if (metaLine.isNotEmpty()) {
                Text(
                    text = metaLine,
                    fontFamily = InterFontFamily,
                    fontSize = 13.sp,
                    color = SecondaryLabel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!record.errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = record.errorMessage.orEmpty()
                        .removePrefix("Duplicate Denied:")
                        .trim(),
                    fontFamily = InterFontFamily,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = DuplicateRed
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Right Column: Time + Chevron + SyncStatusBadge
        Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formattedTime,
                    fontFamily = InterFontFamily,
                    fontSize = 13.sp,
                    color = SecondaryLabel
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color(0xFFC7C7CC),
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            SyncStatusBadge(status = record.syncStatus)
        }
    }
}

@SuppressLint("HardwareIds")
@Composable
fun SettingsScreen(viewModel: DispatchViewModel) {
    val context = LocalContext.current
    val scannerName by viewModel.scannerName.collectAsStateWithLifecycle()
    val authorizedCopies by viewModel.authorizedCopies.collectAsStateWithLifecycle()
    val supabaseUrl by viewModel.supabaseUrl.collectAsStateWithLifecycle()
    val supabaseKey by viewModel.supabaseKey.collectAsStateWithLifecycle()
    val testConnectionStatus by viewModel.testConnectionStatus.collectAsStateWithLifecycle()

    val clipboardManager = LocalClipboardManager.current

    var tempName by remember(scannerName) { mutableStateOf(scannerName) }
    var tempAuthorizedCopies by remember(authorizedCopies) { mutableStateOf(authorizedCopies) }
    var tempSupabaseUrl by remember(supabaseUrl) { mutableStateOf(supabaseUrl) }
    var tempSupabaseKey by remember(supabaseKey) { mutableStateOf(supabaseKey) }
    var isKeyVisible by remember { mutableStateOf(false) }

    var isUnlocked by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf(false) }

    val recommendedSqlScript = remember {
        """
-- one dispatch + one return per copy
alter table public.transport
  add column if not exists copy_name text,
  add column if not exists transport_status text;

create unique index if not exists
  transport_once_per_status
  on public.transport
  (order_id, copy_name, transport_status);
        """.trimIndent()
    }

    // LOCKED STATE (State 10 in Key States)
    if (!isUnlocked) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(GroupedBg)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Settings",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 34.sp,
                color = Neutral900,
                letterSpacing = (-0.8).sp
            )
            Text(
                text = "Locked · ${scannerName.ifBlank { "Station 01" }}",
                fontFamily = InterFontFamily,
                fontSize = 15.sp,
                color = SecondaryLabel
            )

            Spacer(modifier = Modifier.height(28.dp))

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = CellWhite,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background( Color(0xFF18181B), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = "Locked",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Admin access required",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = Neutral900
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Enter the admin password to change the database, the station name, or which copies this phone may scan.",
                        fontFamily = InterFontFamily,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = SecondaryLabel,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = GroupedBg,
                        border = if (passwordError) BorderStroke(1.dp, DuplicateRed) else null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BasicTextField(
                                value = passwordInput,
                                onValueChange = {
                                    passwordInput = it
                                    passwordError = false
                                },
                                singleLine = true,
                                visualTransformation = if (passwordVisible) {
                                    VisualTransformation.None
                                } else {
                                    PasswordVisualTransformation()
                                },
                                textStyle = TextStyle(
                                    fontFamily = InterFontFamily,
                                    fontSize = 16.sp,
                                    color = Neutral900
                                ),
                                cursorBrush = SolidColor(DispatchIndigo),
                                decorationBox = { innerTextField ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        if (passwordInput.isEmpty()) {
                                            Text(
                                                text = "Password",
                                                fontFamily = InterFontFamily,
                                                fontSize = 16.sp,
                                                color = Neutral400
                                            )
                                        }
                                        innerTextField()
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = "Toggle password visibility",
                                tint = SecondaryLabel,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable { passwordVisible = !passwordVisible }
                            )
                        }
                    }

                    if (passwordError) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Incorrect admin password",
                            fontFamily = InterFontFamily,
                            fontSize = 12.sp,
                            color = DuplicateRed
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            if (passwordInput == "Kalana1812@") {
                                isUnlocked = true
                                passwordInput = ""
                            } else {
                                passwordError = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF18181B),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(
                            text = "Unlock",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Forgot it? Ask the factory IT admin.",
                        fontFamily = InterFontFamily,
                        fontSize = 12.sp,
                        color = SecondaryLabel
                    )
                }
            }
        }
        return
    }

    // UNLOCKED STATE (Settings_27cb6bc3.png)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GroupedBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        // Top Right Lock Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Surface(
                onClick = { isUnlocked = false },
                shape = RoundedCornerShape(999.dp),
                color = Color(0xFFE8EAF6)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = "Lock Settings",
                        tint = DispatchIndigo,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "Lock",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = DispatchIndigo
                    )
                }
            }
        }

        Text(
            text = "Settings",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 34.sp,
            color = Neutral900,
            letterSpacing = (-0.8).sp
        )
        Text(
            text = "Unlocked · re-locks after 5 minutes",
            fontFamily = InterFontFamily,
            fontSize = 15.sp,
            color = SecondaryLabel
        )

        Spacer(modifier = Modifier.height(16.dp))

        // SUPABASE DATABASE Section
        SectionHeaderLabel(
            text = "SUPABASE DATABASE",
            trailing = {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Color(0xFFD1FAE5)
                ) {
                    Text(
                        text = "PRECONFIGURED",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 10.sp,
                        color = RestockedEmerald,
                        letterSpacing = 0.4.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        )

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = CellWhite,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                // Project URL Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 13.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Project URL",
                        fontFamily = InterFontFamily,
                        fontSize = 16.sp,
                        color = Neutral900
                    )
                    BasicTextField(
                        value = tempSupabaseUrl,
                        onValueChange = { tempSupabaseUrl = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 13.sp,
                            color = SecondaryLabel,
                            textAlign = TextAlign.End
                        ),
                        cursorBrush = SolidColor(DispatchIndigo),
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp)
                            .testTag("input_supabase_url")
                    )
                }

                HairlineDivider()

                // Publishable Key Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 13.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Publishable key",
                        fontFamily = InterFontFamily,
                        fontSize = 16.sp,
                        color = Neutral900
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp)
                    ) {
                        BasicTextField(
                            value = tempSupabaseKey,
                            onValueChange = { tempSupabaseKey = it },
                            singleLine = true,
                            visualTransformation = if (isKeyVisible) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            textStyle = TextStyle(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 13.sp,
                                color = SecondaryLabel,
                                textAlign = TextAlign.End
                            ),
                            cursorBrush = SolidColor(DispatchIndigo),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_supabase_key")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = if (isKeyVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = "Toggle key visibility",
                            tint = DispatchIndigo,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { isKeyVisible = !isKeyVisible }
                        )
                    }
                }

                HairlineDivider()

                // Test Connection Action Row
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.testSupabaseConnection() }
                        .padding(vertical = 13.dp)
                        .testTag("test_supabase_connection_button")
                ) {
                    Text(
                        text = "Test Connection",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                        color = DispatchIndigo
                    )
                    if (testConnectionStatus != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = testConnectionStatus.orEmpty(),
                            fontFamily = InterFontFamily,
                            fontSize = 12.sp,
                            color = if (testConnectionStatus?.startsWith("Success") == true) RestockedEmerald else DuplicateRed
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // STATION Section
        SectionHeaderLabel(text = "STATION")
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = CellWhite,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 13.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Station ID",
                    fontFamily = InterFontFamily,
                    fontSize = 16.sp,
                    color = Neutral900
                )
                BasicTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = InterFontFamily,
                        fontSize = 16.sp,
                        color = SecondaryLabel,
                        textAlign = TextAlign.End
                    ),
                    cursorBrush = SolidColor(DispatchIndigo),
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp)
                        .testTag("input_scanner_name")
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // COPIES THIS STATION MAY SCAN Section
        SectionHeaderLabel(text = "COPIES THIS STATION MAY SCAN")
        val copyOptions = listOf(
            "GATE PASS" to "Assigns the driver",
            "ACCOUNT COPY" to "Record only",
            "TRANSPORT COPY" to "Record only",
            "PRODUCTION COPY" to "Record only"
        )
        val selectedCopies = tempAuthorizedCopies
            .split(",")
            .map { it.trim().uppercase() }
            .filter { it.isNotEmpty() }
            .toMutableSet()

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = CellWhite,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                copyOptions.forEachIndexed { idx, (copyName, defaultSubtitle) ->
                    val isEnabled = selectedCopies.contains(copyName)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = copyName,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Neutral900
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isEnabled) defaultSubtitle else "Blocked at this station",
                                fontFamily = InterFontFamily,
                                fontSize = 12.sp,
                                color = if (isEnabled) SecondaryLabel else DuplicateRed
                            )
                        }

                        IosSwitch(
                            checked = isEnabled,
                            onCheckedChange = { checked ->
                                val nextSet = selectedCopies.toMutableSet()
                                if (checked) nextSet.add(copyName) else nextSet.remove(copyName)
                                tempAuthorizedCopies = nextSet.joinToString(", ")
                            },
                            activeColor = DispatchIndigo
                        )
                    }
                    if (idx < copyOptions.lastIndex) {
                        HairlineDivider()
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Copies switched off are rejected at this station as Unauthorized.",
            fontFamily = InterFontFamily,
            fontSize = 12.sp,
            color = SecondaryLabel,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Save All Configurations Button
        Button(
            onClick = {
                viewModel.updateSettings(
                    name = tempName,
                    authorizedCopies = tempAuthorizedCopies,
                    supabaseUrl = tempSupabaseUrl,
                    supabaseKey = tempSupabaseKey
                )
                viewModel.triggerFeedback(context)
                viewModel.showTopToast(
                    title = "Configurations saved",
                    subtitle = "${tempName.ifBlank { "Station 01" }} · Supabase connected",
                    isOffline = false
                )
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = DispatchIndigo,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("save_settings_button")
        ) {
            Text(
                text = "Save All Configurations",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // SQL SETUP Section
        SectionHeaderLabel(
            text = "SQL SETUP",
            trailing = {
                Text(
                    text = "Copy",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = DispatchIndigo,
                    modifier = Modifier
                        .clickable {
                            clipboardManager.setText(AnnotatedString(recommendedSqlScript))
                            viewModel.showTopToast(
                                title = "SQL copied",
                                subtitle = "Paste in Supabase SQL Editor",
                                isOffline = false
                            )
                        }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        )

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Neutral900,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = recommendedSqlScript,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = Color(0xFFE4E4E7),
                modifier = Modifier.padding(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // DEVICE Section
        SectionHeaderLabel(text = "DEVICE")
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = CellWhite,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Android Secure ID",
                    fontFamily = InterFontFamily,
                    fontSize = 16.sp,
                    color = Neutral900
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = formatDeviceId(viewModel.deviceId),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 13.sp,
                        color = SecondaryLabel
                    )
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy Device ID",
                        tint = DispatchIndigo,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable {
                                clipboardManager.setText(AnnotatedString(viewModel.deviceId))
                            }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Stamped on every scan in the audit log.",
            fontFamily = InterFontFamily,
            fontSize = 12.sp,
            color = SecondaryLabel,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}
