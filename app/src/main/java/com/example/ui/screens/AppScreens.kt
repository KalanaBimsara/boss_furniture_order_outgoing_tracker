package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.ScannedRecord
import com.example.viewmodel.DispatchViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.example.data.model.QrPayload
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HeaderBar(
    title: String,
    subtitle: String,
    onIconClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 1.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            letterSpacing = 0.5.sp
                        )
                    }
                }
                
                IconButton(
                    onClick = onIconClick,
                    modifier = Modifier
                        .size(44.dp)
                        .background(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(22.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Header Settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), thickness = 1.dp)
        }
    }
}

@Composable
fun ScannerOverlay(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .aspectRatio(1.2f)
            .background(Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(width = 2.dp, color = Color.White.copy(alpha = 0.3f), shape = RoundedCornerShape(24.dp))
        ) {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 5.dp.toPx()
                val cornerLength = 32.dp.toPx()
                val cornerRadius = 24.dp.toPx()
                val color = Color(0xFF4F46E5) // Indigo600

                // Top Left
                drawArc(
                    color = color,
                    startAngle = 180f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(-1.dp.toPx(), -1.dp.toPx()),
                    size = Size(cornerRadius * 2, cornerRadius * 2),
                    style = Stroke(width = strokeWidth)
                )
                drawLine(
                    color = color,
                    start = Offset(-1.dp.toPx(), cornerRadius),
                    end = Offset(-1.dp.toPx(), cornerLength),
                    strokeWidth = strokeWidth
                )
                drawLine(
                    color = color,
                    start = Offset(cornerRadius, -1.dp.toPx()),
                    end = Offset(cornerLength, -1.dp.toPx()),
                    strokeWidth = strokeWidth
                )

                // Top Right
                drawArc(
                    color = color,
                    startAngle = 270f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(size.width - cornerRadius * 2 + 1.dp.toPx(), -1.dp.toPx()),
                    size = Size(cornerRadius * 2, cornerRadius * 2),
                    style = Stroke(width = strokeWidth)
                )
                drawLine(
                    color = color,
                    start = Offset(size.width + 1.dp.toPx(), cornerRadius),
                    end = Offset(size.width + 1.dp.toPx(), cornerLength),
                    strokeWidth = strokeWidth
                )
                drawLine(
                    color = color,
                    start = Offset(size.width - cornerRadius, -1.dp.toPx()),
                    end = Offset(size.width - cornerLength, -1.dp.toPx()),
                    strokeWidth = strokeWidth
                )

                // Bottom Left
                drawArc(
                    color = color,
                    startAngle = 90f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(-1.dp.toPx(), size.height - cornerRadius * 2 + 1.dp.toPx()),
                    size = Size(cornerRadius * 2, cornerRadius * 2),
                    style = Stroke(width = strokeWidth)
                )
                drawLine(
                    color = color,
                    start = Offset(-1.dp.toPx(), size.height - cornerRadius),
                    end = Offset(-1.dp.toPx(), size.height - cornerLength),
                    strokeWidth = strokeWidth
                )
                drawLine(
                    color = color,
                    start = Offset(cornerRadius, size.height + 1.dp.toPx()),
                    end = Offset(cornerLength, size.height + 1.dp.toPx()),
                    strokeWidth = strokeWidth
                )

                // Bottom Right
                drawArc(
                    color = color,
                    startAngle = 0f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(size.width - cornerRadius * 2 + 1.dp.toPx(), size.height - cornerRadius * 2 + 1.dp.toPx()),
                    size = Size(cornerRadius * 2, cornerRadius * 2),
                    style = Stroke(width = strokeWidth)
                )
                drawLine(
                    color = color,
                    start = Offset(size.width + 1.dp.toPx(), size.height - cornerRadius),
                    end = Offset(size.width + 1.dp.toPx(), size.height - cornerLength),
                    strokeWidth = strokeWidth
                )
                drawLine(
                    color = color,
                    start = Offset(size.width - cornerRadius, size.height + 1.dp.toPx()),
                    end = Offset(size.width - cornerLength, size.height + 1.dp.toPx()),
                    strokeWidth = strokeWidth
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .background(Color.Transparent)
                .border(width = 1.dp, color = Color.White.copy(alpha = 0.1f), shape = RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "ALIGN QR CODE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.5f),
                letterSpacing = 2.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigator(viewModel: DispatchViewModel, modifier: Modifier = Modifier) {
    var currentTab by remember { mutableStateOf(0) }
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val syncMessage by viewModel.syncMessage.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Display sync messages in a dynamic snackbar-like dialog or toast
    LaunchedEffect(syncMessage) {
        if (syncMessage != null) {
            // Message shown in UI, can clear after short display or keep as status banner
        }
    }

    Scaffold(
        topBar = {
            HeaderBar(
                title = when (currentTab) {
                    0 -> "Factory Dispatch"
                    1 -> "Dispatch Sync Queue"
                    2 -> "Historical Logs"
                    3 -> "System Settings"
                    else -> "Factory Dispatch"
                },
                subtitle = when (currentTab) {
                    0 -> "STATION 01 • ACTIVE"
                    1 -> "STATION 01 • QUEUED"
                    2 -> "STATION 01 • COMPLETED"
                    3 -> "STATION 01 • CONFIGS"
                    else -> "STATION 01"
                },
                onIconClick = { currentTab = 3 }
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("app_navigation_bar"),
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = "Scanner") },
                    label = { Text("Scanner") },
                    modifier = Modifier.testTag("nav_item_scanner")
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = { Icon(Icons.Default.CloudSync, contentDescription = "Pending Pack list") },
                    label = { Text("Pending") },
                    modifier = Modifier.testTag("nav_item_pending")
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = { Icon(Icons.Default.History, contentDescription = "History Log") },
                    label = { Text("History") },
                    modifier = Modifier.testTag("nav_item_history")
                )
                NavigationBarItem(
                    selected = currentTab == 3,
                    onClick = { currentTab = 3 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings config") },
                    label = { Text("Settings") },
                    modifier = Modifier.testTag("nav_item_settings")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                0 -> ScannerScreen(viewModel)
                1 -> PendingScreen(viewModel)
                2 -> HistoryScreen(viewModel)
                3 -> SettingsScreen(viewModel)
            }

            // Global loader indicator overlay
            if (isSyncing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = syncMessage ?: "Broadcasting dispatch...",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ScannerScreen(viewModel: DispatchViewModel) {
    val context = LocalContext.current
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val scannedPayload by viewModel.scannedPayload.collectAsStateWithLifecycle()
    val scanError by viewModel.scanError.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Keep QR code inside camera field of view to scan automatically.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.DarkGray),
            contentAlignment = Alignment.Center
        ) {
            if (cameraPermissionState.status.isGranted) {
                CameraScannerView(
                    modifier = Modifier.fillMaxSize(),
                    onQrScanned = { raw ->
                        viewModel.onQrScanned(raw, context)
                    }
                )
                
                // Aim rectangle overlay
                ScannerOverlay(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = Color.LightGray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Camera permission required is to scan QR labels.",
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { cameraPermissionState.launchPermissionRequest() },
                        modifier = Modifier.testTag("request_permission_button")
                    ) {
                        Icon(Icons.Default.VpnKey, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Grant Permission")
                    }
                }
            }
        }

        // Show Scan parsing Error Banner
        AnimatedVisibility(
            visible = scanError != null,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically()
        ) {
            scanError?.let { err ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = "Error Logo",
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = err,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
        
        // Popup Summary when QR code is scanned successfully
        if (scannedPayload != null) {
            var timeLeft by remember(scannedPayload) { mutableIntStateOf(5) }
            
            LaunchedEffect(scannedPayload) {
                for (i in 5 downTo 1) {
                    timeLeft = i
                    kotlinx.coroutines.delay(1000)
                }
                viewModel.recordDispatch(context, onSuccess = {}, onDuplicate = { err -> 
                    // handled by viewmodel
                })
            }

            androidx.compose.material3.AlertDialog(
                onDismissRequest = { viewModel.clearScannedData() },
                title = { Text("Scan Summary (Auto-submits in ${timeLeft}s)") },
                text = {
                    Column {
                        Text("Order No: ${scannedPayload?.orderNo ?: "N/A"}", style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Sales Person: ${scannedPayload?.salesPerson ?: "N/A"}", style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Total Units: ${scannedPayload?.totalUnits ?: scannedPayload?.qty ?: 1}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Copy: ${scannedPayload?.copy ?: "N/A"}", style = MaterialTheme.typography.bodyLarge)
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        viewModel.recordDispatch(context, onSuccess = {}, onDuplicate = { err -> 
                            // viewModel handles duplicate errors internally via UI state flow or similar
                        })
                    }) {
                        Text("Confirm Now")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.clearScannedData() }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun OrderReviewCard(
    order: QrPayload,
    onRecord: () -> Unit,
    onScanAgain: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .testTag("order_review_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Ticket Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ORDER NO: #${order.orderNo ?: "N/A"}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "TRACKING TICKET",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                
                // Copy Type badge
                val badgeColor = if (order.copy?.uppercase() == "OFFICE COPY") {
                    MaterialTheme.colorScheme.tertiaryContainer
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeColor,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Text(
                        text = order.copy?.uppercase() ?: "CUSTOMER COPY",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            // Body specifications list in an elegant monospaced design resembling shipping labels
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    DetailRow(label = "Customer Client", value = order.customer ?: "N/A", icon = Icons.Default.Business)
                    DetailRow(label = "District Area", value = order.district ?: "N/A", icon = Icons.Default.Place)
                    DetailRow(label = "Delivery Date", value = order.deliveryDate ?: "N/A", icon = Icons.Default.CalendarToday)
                    DetailRow(label = "Delivery Type", value = order.deliveryType ?: "N/A", icon = Icons.Default.LocalShipping)
                    DetailRow(label = "Item Index", value = "Table ${order.tableIndex ?: 0} of ${order.tableCount ?: 0}", icon = Icons.Default.FormatListNumbered)
                    DetailRow(label = "Table Size Dimensions", value = order.size ?: "N/A", icon = Icons.Default.AspectRatio)
                    DetailRow(label = "Top Finishing Wood", value = (order.topColour ?: "N/A").uppercase(), icon = Icons.Default.Layers)
                    DetailRow(label = "Frame Surface Coat", value = (order.frameColour ?: "N/A").uppercase(), icon = Icons.Default.Palette)
                    DetailRow(label = "Leg Profile Info", value = "${order.legSize ?: "N/A"} in Height ${order.legHeight ?: "N/A"}\" (${order.legShape ?: "N/A"})", icon = Icons.Default.Analytics)
                    DetailRow(label = "Quantity Count", value = "${order.qty ?: 1} UNIT(S)", icon = Icons.Default.ShoppingBag)
                }
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            // Action Buttons (Record, Scan again, Cancel)
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onRecord,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("record_outgoing_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "RECORD AS OUTGOING",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onScanAgain,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("scan_again_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Scan Again", fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("cancel_scan_button"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cancel", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(130.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun PendingScreen(viewModel: DispatchViewModel) {
    val history by viewModel.scannedHistory.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val syncMessage by viewModel.syncMessage.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    // Filter history for only pending rows
    val pendingRecords = remember(history) {
        history.filter { it.syncStatus == "PENDING" }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "${pendingRecords.size} dispatch logs queued offline",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (pendingRecords.isNotEmpty()) {
                Button(
                    onClick = { viewModel.forceSyncPending() },
                    modifier = Modifier.testTag("sync_all_button"),
                    enabled = !isSyncing
                ) {
                    Icon(Icons.Default.Sync, contentDescription = "Sync icon")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sync All")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Feedback of sync response completed
        if (syncMessage != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = syncMessage ?: "",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                    IconButton(onClick = { viewModel.clearSyncMessage() }) {
                        Icon(Icons.Default.Close, contentDescription = "Close prompt", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

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
                    Icon(
                        Icons.Default.CloudDone,
                        contentDescription = "Sync complete sign",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "All Dispatch Rows Synced!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Any scans done offline are kept inside local storage and will list here if sync fails.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(pendingRecords) { record ->
                    HistoryItemCard(record = record, onClick = {})
                }
            }
        }
    }
}

@Composable
fun HistoryScreen(viewModel: DispatchViewModel) {
    val history by viewModel.scannedHistory.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    var selectedRecordDetail by remember { mutableStateOf<ScannedRecord?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Total localized logs: ${history.size}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Search Bar Text Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.onSearchQueryChanged(it) },
            label = { Text("Search by OrderNo, Customer, or District") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search bar") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear click")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .testTag("search_history_input"),
            shape = RoundedCornerShape(12.dp)
        )

        if (history.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.HistoryToggleOff,
                        contentDescription = "Empty sign",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Scanned Logs Found",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Scanned factory products show up in this log history.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(history) { record ->
                    HistoryItemCard(
                        record = record,
                        onClick = { selectedRecordDetail = record }
                    )
                }
            }
        }
    }

    if (selectedRecordDetail != null) {
        selectedRecordDetail?.let { record ->
            AlertDialog(
                onDismissRequest = { selectedRecordDetail = null },
                confirmButton = {
                    Button(onClick = { selectedRecordDetail = null }) {
                        Text("Done")
                    }
                },
                title = { Text("Record Verification Detail") },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        VerificationRow(name = "Order Number", value = "#${record.orderNo}")
                        VerificationRow(name = "Customer", value = record.customer)
                        VerificationRow(name = "District", value = record.district)
                        VerificationRow(name = "Delivery Type", value = record.deliveryType)
                        VerificationRow(name = "Copy Format", value = record.copy)
                        VerificationRow(name = "Index", value = "Table ${record.tableIndex} / ${record.tableCount}")
                        VerificationRow(name = "Specs (Size)", value = record.size)
                        VerificationRow(name = "Top Wood Colour", value = record.topColour)
                        VerificationRow(name = "Frame Powder", value = record.frameColour)
                        VerificationRow(name = "Leg height/Shape", value = "${record.legSize} - ${record.legHeight}\" (${record.legShape})")
                        VerificationRow(name = "Quantity Total", value = "${record.qty} Units")
                        
                        Divider(modifier = Modifier.padding(vertical = 6.dp))
                        
                        VerificationRow(name = "Duplicate Key", value = record.duplicateKey)
                        VerificationRow(name = "Google Synced?", value = record.syncStatus)
                        if (record.errorMessage != null) {
                            Text(
                                "Event Log: ${record.errorMessage}",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                            )
                        }

                        Divider(modifier = Modifier.padding(vertical = 6.dp))
                        Text(
                            text = "Raw payload: ${record.rawPayload}",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 14.sp
                        )
                    }
                }
            )
        }
    }
}

@Composable
fun VerificationRow(name: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun HistoryItemCard(record: ScannedRecord, onClick: () -> Unit) {
    val formatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val formattedDate = remember(record.scannedAt) { formatter.format(Date(record.scannedAt)) }

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("history_item_${record.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "#${record.orderNo} • ${record.customer}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                // Dispatch pills
                val statusColor = when (record.syncStatus.uppercase()) {
                    "SYNCED" -> Color(0xFF2E7D32) // Soft Green
                    "PENDING" -> Color(0xFFEF6C00) // Soft Orange
                    "DUPLICATE" -> Color(0xFFC62828) // Dark Red
                    else -> Color(0xFFD32F2F) // Normal Red
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = record.syncStatus.uppercase(),
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Salesperson: ${record.salesPerson}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${record.district} • Table ${record.tableIndex}/${record.tableCount} • Size: ${record.size}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!record.errorMessage.isNullOrEmpty()) {
                        Text(
                            text = record.errorMessage ?: "",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.error,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                Text(
                    text = formattedDate,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@SuppressLint("HardwareIds")
@Composable
fun SettingsScreen(viewModel: DispatchViewModel) {
    val context = LocalContext.current
    val appsScriptUrl by viewModel.appsScriptUrl.collectAsStateWithLifecycle()
    val scannerName by viewModel.scannerName.collectAsStateWithLifecycle()
    val authorizedCopies by viewModel.authorizedCopies.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboardManager.current

    var tempUrl by remember(appsScriptUrl) { mutableStateOf(appsScriptUrl) }
    var tempName by remember(scannerName) { mutableStateOf(scannerName) }
    var tempAuthorizedCopies by remember(authorizedCopies) { mutableStateOf(authorizedCopies) }
    var showUrlError by remember { mutableStateOf(false) }

    var showClearConfirm by remember { mutableStateOf(false) }
    var clearMessage by remember { mutableStateOf<String?>(null) }

    // Google Apps Script sample text to copy
    val sampleAppsScriptCode = """
function doPost(e) {
  try {
    var jsonString = e.postData.contents;
    var data = JSON.parse(jsonString);
    var scannerName = data.scannerName || "Unknown Scanner";
    var deviceId = data.deviceId || "Unknown Device";
    var rawPayload = data.rawPayload || "";
    var payload = data.payload || {};
    
    var orderNo = payload.orderNo || "";
    var orderId = payload.orderId || "";
    var customer = payload.customer || "";
    var district = payload.district || "";
    var deliveryDate = payload.deliveryDate || "";
    var deliveryType = payload.deliveryType || "";
    var copy = payload.copy || "";
    var tableIndex = payload.tableIndex !== undefined ? payload.tableIndex : "";
    var tableCount = payload.tableCount !== undefined ? payload.tableCount : "";
    var size = payload.size || "";
    var topColour = payload.topColour || "";
    var frameColour = payload.frameColour || "";
    var qty = payload.qty !== undefined ? payload.qty : "";
    var legSize = payload.legSize || "";
    var legShape = payload.legShape || "";
    var legHeight = payload.legHeight || "";
    var qrTimestamp = payload.ts || "";
    
    var duplicateKey = (orderId + "-" + tableIndex + "-" + copy).trim();
    var sheet = SpreadsheetApp.getActiveSpreadsheet().getActiveSheet();
    var lastRow = sheet.getLastRow();
    var isDuplicate = false;
    
    if (lastRow > 1) {
      var values = sheet.getRange(2, 22, lastRow - 1, 1).getValues();
      for (var i = 0; i < values.length; i++) {
        if (values[i][0].toString().trim() === duplicateKey) {
          isDuplicate = true;
          break;
        }
      }
    }
    
    if (isDuplicate) {
      return ContentService.createTextOutput(JSON.stringify({
        "success": false,
        "duplicate": true,
        "message": "This item has already been scanned."
      })).setMimeType(ContentService.MimeType.JSON);
    }
    
    var scanId = "SCAN-" + Utilities.getUuid();
    var scannedAt = new Date().toISOString();
    var syncStatus = "Synced";
    
    sheet.appendRow([
      scanId, scannedAt, scannerName, deviceId, orderNo, orderId, customer, 
      district, deliveryDate, deliveryType, copy, tableIndex, tableCount, 
      size, topColour, frameColour, qty, legSize, legShape, legHeight, 
      qrTimestamp, duplicateKey, rawPayload, syncStatus
    ]);
    
    return ContentService.createTextOutput(JSON.stringify({
      "success": true,
      "scanId": scanId,
      "message": "Scanned data recorded successfully"
    })).setMimeType(ContentService.MimeType.JSON);
  } catch (err) {
    return ContentService.createTextOutput(JSON.stringify({
      "success": false,
      "message": "Server Error: " + err.toString()
    })).setMimeType(ContentService.MimeType.JSON);
  }
}
""".trimIndent()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Configure dispatcher settings to update Google Sheets database.",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Connection Endpoints",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Google Apps Script API Endpoint URL",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = tempUrl,
                        onValueChange = {
                            tempUrl = it
                            showUrlError = it.isNotEmpty() && !it.startsWith("https://")
                        },
                        placeholder = { Text("https://script.google.com/macros/s/.../exec") },
                        isError = showUrlError,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_apps_script_url"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Next
                        )
                    )
                    if (showUrlError) {
                        Text(
                            text = "URL must begin with https://",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Packing Line / Station ID",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        placeholder = { Text("Packing Station 01") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_scanner_name"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Authorized Copies",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    val copyOptions = listOf("PRODUCTION COPY", "OFFICE COPY", "TRANSPORT COPY", "CUSTOMER COPY")
                    val selectedCopies = tempAuthorizedCopies.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableSet()
                    
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(start = 8.dp)
                    ) {
                        copyOptions.forEach { option ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                androidx.compose.material3.Checkbox(
                                    checked = selectedCopies.contains(option),
                                    onCheckedChange = { isChecked ->
                                        if (isChecked) {
                                            selectedCopies.add(option)
                                        } else {
                                            selectedCopies.remove(option)
                                        }
                                        tempAuthorizedCopies = selectedCopies.joinToString(", ")
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = option, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (tempUrl.isEmpty() || tempUrl.startsWith("https://")) {
                                viewModel.updateSettings(tempUrl, tempName, tempAuthorizedCopies)
                                viewModel.triggerFeedback(context)
                                showUrlError = false
                            } else {
                                showUrlError = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_settings_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Save settings")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Configuration")
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Device Signature Diagnostics",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Android Secure ID:", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            viewModel.deviceId,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Apps Script Setup Guide",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )

                        IconButton(onClick = {
                            clipboardManager.setText(AnnotatedString(sampleAppsScriptCode))
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy boilerplate")
                        }
                    }
                    
                    Text(
                        text = "1. Create a new Google Sheet.\n" +
                               "2. Go to Extensions -> Apps Script and paste code.\n" +
                               "3. Click 'Deploy' -> 'New deployment' -> select 'Web App'.\n" +
                               "4. Execute as 'Me' & set access to 'Anyone'.\n" +
                               "5. Copy resulting URL, paste into the top config slot and click 'Save'.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp),
                        lineHeight = 18.sp
                    )
                }
            }
        }

        item {
            Button(
                onClick = { showClearConfirm = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("clear_history_button")
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete cache")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Clear Scanned Records Locally")
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearHistory()
                    showClearConfirm = false
                    clearMessage = "Database logs successfully cleared."
                }) {
                    Text("Confirm Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel")
                }
            },
            title = { Text("Delete Local Databases Logs?") },
            text = { Text("This will permanently clear logs from the local device history storage. It will not affect Google Sheets rows already synced.") }
        )
    }

    if (clearMessage != null) {
        AlertDialog(
            onDismissRequest = { clearMessage = null },
            confirmButton = {
                Button(onClick = { clearMessage = null }) {
                    Text("Okay")
                }
            },
            title = { Text("Action Log") },
            text = { Text(clearMessage ?: "") }
        )
    }
}
