package com.example.ui.screens

import android.Manifest
import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AccentButton
import com.example.ui.components.BannerTone
import com.example.ui.components.SectionCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBanner
import com.example.ui.components.animatedModeAccent
import com.example.ui.components.appTextFieldColors
import com.example.ui.components.icon
import com.example.ui.theme.FactoryTheme
import com.example.ui.theme.Motion
import com.example.viewmodel.DispatchViewModel
import com.example.viewmodel.OperationMode
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ScannerScreen(viewModel: DispatchViewModel) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val currentMode by viewModel.currentMode.collectAsStateWithLifecycle()
    val scannedPayload by viewModel.scannedPayload.collectAsStateWithLifecycle()
    val scanError by viewModel.scanError.collectAsStateWithLifecycle()

    val selectedVehicleOption by viewModel.selectedVehicleOption.collectAsStateWithLifecycle()
    val pickMeVehicleNumber by viewModel.pickMeVehicleNumber.collectAsStateWithLifecycle()
    val customVehicleNumber by viewModel.customVehicleNumber.collectAsStateWithLifecycle()
    val effectiveVehicle = remember(selectedVehicleOption, pickMeVehicleNumber, customVehicleNumber) {
        viewModel.getEffectiveVehicleNumber()
    }

    val accentColor = animatedModeAccent(currentMode)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ModeHint(currentMode)

        // Vehicle selector (shared by Dispatch and Return modes)
        VehicleSelectionCard(viewModel = viewModel, accentColor = accentColor)

        CameraViewport(
            permissionGranted = cameraPermissionState.status.isGranted,
            onRequestPermission = { cameraPermissionState.launchPermissionRequest() },
            accentColor = accentColor,
            onQrScanned = { raw -> viewModel.onQrScanned(raw, context) }
        )

        StatusBanner(message = scanError, toneOf = { BannerTone.Error })

        AnimatedContent(
            targetState = currentMode,
            transitionSpec = {
                fadeIn(Motion.enter()).togetherWith(fadeOut(Motion.exit())).using(SizeTransform(clip = false))
            },
            label = "manualEntryCard"
        ) { mode ->
            if (mode == OperationMode.DISPATCH) {
                ManualDispatchCard(viewModel, effectiveVehicle, accentColor, context, focusManager)
            } else {
                ManualReturnCard(viewModel, accentColor, context, focusManager)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }

    // Confirmation popup when a QR code is scanned
    scannedPayload?.let { payload ->
        ScanConfirmDialog(
            payload = payload,
            isReturn = currentMode == OperationMode.RETURN,
            accentColor = accentColor,
            effectiveVehicle = effectiveVehicle,
            onConfirm = { viewModel.recordDispatch(context, onSuccess = {}, onDuplicate = {}) },
            onDismiss = { viewModel.clearScannedData() }
        )
    }
}

@Composable
private fun ModeHint(currentMode: OperationMode) {
    AnimatedContent(
        targetState = currentMode,
        transitionSpec = { fadeIn(Motion.enter()) togetherWith fadeOut(Motion.exit()) },
        label = "modeHint"
    ) { mode ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Outlined.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = if (mode == OperationMode.DISPATCH) {
                    "Only GATE PASS assigns the order to a driver. All 4 copy types are recorded."
                } else {
                    "Select a vehicle, then scan the QR or enter the order # to return & restock."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CameraViewport(
    permissionGranted: Boolean,
    onRequestPermission: () -> Unit,
    accentColor: Color,
    onQrScanned: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .clip(MaterialTheme.shapes.large)
            .background(FactoryTheme.colors.cameraBackdrop),
        contentAlignment = Alignment.Center
    ) {
        if (permissionGranted) {
            CameraScannerView(
                modifier = Modifier.fillMaxSize(),
                onQrScanned = onQrScanned
            )

            ScannerOverlay(
                modifier = Modifier.align(Alignment.Center),
                accentColor = accentColor
            )

            Text(
                text = "Align QR code within the frame",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
                    .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color.White.copy(alpha = 0.08f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(30.dp)
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Camera permission is required to scan barcodes & QR codes.",
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                AccentButton(
                    text = "Grant camera permission",
                    icon = Icons.Default.VpnKey,
                    onClick = onRequestPermission,
                    accent = accentColor,
                    compact = true,
                    modifier = Modifier.testTag("request_permission_button")
                )
            }
        }
    }
}

@Composable
private fun ManualDispatchCard(
    viewModel: DispatchViewModel,
    effectiveVehicle: String,
    accentColor: Color,
    context: Context,
    focusManager: FocusManager
) {
    val manualDispatchOrderInput by viewModel.manualDispatchOrderInput.collectAsStateWithLifecycle()
    val manualDispatchStatus by viewModel.manualDispatchStatus.collectAsStateWithLifecycle()
    val isProcessingDispatch by viewModel.isProcessingDispatch.collectAsStateWithLifecycle()

    SectionCard(modifier = Modifier.testTag("manual_dispatch_card")) {
        SectionHeader(
            title = "Manual dispatch",
            subtitle = "Assigns order to $effectiveVehicle & records dispatch",
            icon = Icons.Default.Keyboard,
            accent = accentColor
        )

        Spacer(modifier = Modifier.height(16.dp))

        OrderNumberField(
            value = manualDispatchOrderInput,
            onValueChange = { viewModel.setManualDispatchOrderInput(it) },
            accentColor = accentColor,
            clearDescription = "Clear dispatch order input",
            testTag = "manual_dispatch_order_input",
            onDone = {
                focusManager.clearFocus()
                viewModel.processManualDispatch(context)
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        AccentButton(
            text = "Dispatch order · $effectiveVehicle",
            icon = OperationMode.DISPATCH.icon,
            onClick = {
                focusManager.clearFocus()
                viewModel.processManualDispatch(context)
            },
            enabled = !isProcessingDispatch && manualDispatchOrderInput.isNotBlank(),
            loading = isProcessingDispatch,
            loadingText = "Updating factory database…",
            accent = accentColor,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("process_dispatch_button")
        )

        StatusBanner(
            message = manualDispatchStatus,
            toneOf = { status ->
                val isError = status.startsWith("Please") || status.startsWith("Duplicate") || status.startsWith("Alert")
                if (isError) BannerTone.Error else BannerTone.Success
            },
            innerPadding = PaddingValues(top = 12.dp),
            onDismiss = { viewModel.clearManualDispatchStatus() }
        )
    }
}

@Composable
private fun ManualReturnCard(
    viewModel: DispatchViewModel,
    accentColor: Color,
    context: Context,
    focusManager: FocusManager
) {
    val manualOrderInput by viewModel.manualOrderInput.collectAsStateWithLifecycle()
    val manualRestocked by viewModel.manualRestocked.collectAsStateWithLifecycle()
    val manualReturnStatus by viewModel.manualReturnStatus.collectAsStateWithLifecycle()
    val isProcessingReturn by viewModel.isProcessingReturn.collectAsStateWithLifecycle()
    val successColor = FactoryTheme.colors.success

    SectionCard(modifier = Modifier.testTag("manual_return_card")) {
        SectionHeader(
            title = "Manual return",
            subtitle = "Type the order number to mark it returned & restocked",
            icon = Icons.Default.EditNote,
            accent = accentColor
        )

        Spacer(modifier = Modifier.height(16.dp))

        OrderNumberField(
            value = manualOrderInput,
            onValueChange = { viewModel.setManualOrderInput(it) },
            accentColor = accentColor,
            clearDescription = "Clear input",
            testTag = "manual_order_number_input",
            onDone = {
                focusManager.clearFocus()
                viewModel.processManualReturn(context)
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .toggleable(
                    value = manualRestocked,
                    role = Role.Switch,
                    onValueChange = { viewModel.setManualRestocked(it) }
                )
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.Inventory2,
                contentDescription = null,
                tint = if (manualRestocked) successColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "Restock to factory inventory",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = manualRestocked,
                onCheckedChange = null,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = successColor,
                    checkedThumbColor = FactoryTheme.colors.onAccent,
                    uncheckedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        AccentButton(
            text = "Process return & restock",
            icon = OperationMode.RETURN.icon,
            onClick = {
                focusManager.clearFocus()
                viewModel.processManualReturn(context)
            },
            enabled = !isProcessingReturn && manualOrderInput.isNotBlank(),
            loading = isProcessingReturn,
            loadingText = "Updating factory database…",
            accent = accentColor,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("process_return_button")
        )

        StatusBanner(
            message = manualReturnStatus,
            toneOf = { status ->
                val isSuccess = status.contains("successfully", ignoreCase = true) ||
                    status.contains("marked returned", ignoreCase = true)
                if (isSuccess) BannerTone.Success else BannerTone.Info
            },
            accent = accentColor,
            innerPadding = PaddingValues(top = 12.dp),
            onDismiss = { viewModel.clearManualReturnStatus() }
        )
    }
}

@Composable
private fun OrderNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    accentColor: Color,
    clearDescription: String,
    testTag: String,
    onDone: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("Order number") },
        placeholder = { Text("e.g. 1024 or ORD-8821") },
        leadingIcon = { Icon(Icons.Default.Tag, contentDescription = null) },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = clearDescription)
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        shape = MaterialTheme.shapes.medium,
        colors = appTextFieldColors(accentColor)
    )
}
