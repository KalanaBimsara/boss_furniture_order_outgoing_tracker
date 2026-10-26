package com.example.ui.screens

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AccentButton
import com.example.ui.components.BannerTone
import com.example.ui.components.IconBadge
import com.example.ui.components.LocalModeAccent
import com.example.ui.components.SectionCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.SoftButton
import com.example.ui.components.StatusBanner
import com.example.ui.components.StatusPill
import com.example.ui.components.appTextFieldColors
import com.example.ui.theme.FactoryTheme
import com.example.ui.theme.Motion
import com.example.viewmodel.DispatchViewModel

@SuppressLint("HardwareIds")
@Composable
fun SettingsScreen(viewModel: DispatchViewModel) {
    var isUnlocked by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf(false) }

    AnimatedContent(
        targetState = isUnlocked,
        transitionSpec = {
            (fadeIn(Motion.enter(Motion.LONG)) + scaleIn(Motion.enter(Motion.LONG), initialScale = 0.96f))
                .togetherWith(fadeOut(Motion.exit()) + scaleOut(Motion.exit(), targetScale = 1.02f))
        },
        label = "settingsLock"
    ) { unlocked ->
        if (!unlocked) {
            SettingsLock(
                passwordInput = passwordInput,
                passwordError = passwordError,
                onPasswordChange = {
                    passwordInput = it
                    passwordError = false
                },
                onUnlock = {
                    if (passwordInput == "Kalana1812@") {
                        isUnlocked = true
                    } else {
                        passwordError = true
                    }
                }
            )
        } else {
            SettingsContent(viewModel)
        }
    }
}

@Composable
private fun SettingsLock(
    passwordInput: String,
    passwordError: Boolean,
    onPasswordChange: (String) -> Unit,
    onUnlock: () -> Unit
) {
    val accent = LocalModeAccent.current
    val shake = remember { Animatable(0f) }
    var failedAttempts by remember { mutableIntStateOf(0) }
    if (passwordError) {
        LaunchedEffect(failedAttempts) {
            for (offset in listOf(14f, -12f, 9f, -6f, 3f, 0f)) {
                shake.animateTo(offset, tween(durationMillis = 45))
            }
        }
    }

    val attemptUnlock = {
        onUnlock()
        failedAttempts++
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconBadge(
            icon = Icons.Default.Lock,
            tint = if (passwordError) MaterialTheme.colorScheme.error else accent,
            size = 72.dp,
            iconSize = 32.dp,
            contentDescription = "Locked"
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text("Settings are locked", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Enter the settings password to continue",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedTextField(
            value = passwordInput,
            onValueChange = onPasswordChange,
            label = { Text("Password") },
            isError = passwordError,
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { attemptUnlock() }),
            shape = MaterialTheme.shapes.medium,
            colors = appTextFieldColors(accent),
            modifier = Modifier
                .widthIn(max = 360.dp)
                .fillMaxWidth()
                .offset { IntOffset(shake.value.dp.roundToPx(), 0) }
        )
        AnimatedVisibility(
            visible = passwordError,
            enter = fadeIn(Motion.enter()) + expandVertically(Motion.enter()),
            exit = fadeOut(Motion.exit()) + shrinkVertically(Motion.exit())
        ) {
            Text(
                "Incorrect password",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        AccentButton(
            text = "Unlock",
            onClick = { attemptUnlock() },
            accent = accent,
            modifier = Modifier
                .widthIn(max = 360.dp)
                .fillMaxWidth()
        )
    }
}

@Composable
private fun SettingsContent(viewModel: DispatchViewModel) {
    val context = LocalContext.current
    val scannerName by viewModel.scannerName.collectAsStateWithLifecycle()
    val authorizedCopies by viewModel.authorizedCopies.collectAsStateWithLifecycle()
    val supabaseUrl by viewModel.supabaseUrl.collectAsStateWithLifecycle()
    val supabaseKey by viewModel.supabaseKey.collectAsStateWithLifecycle()
    val testConnectionStatus by viewModel.testConnectionStatus.collectAsStateWithLifecycle()
    val accent = LocalModeAccent.current
    val successColor = FactoryTheme.colors.success
    val fieldColors = appTextFieldColors(accent)

    val clipboardManager = LocalClipboardManager.current

    var tempName by remember(scannerName) { mutableStateOf(scannerName) }
    var tempAuthorizedCopies by remember(authorizedCopies) { mutableStateOf(authorizedCopies) }
    var tempSupabaseUrl by remember(supabaseUrl) { mutableStateOf(supabaseUrl) }
    var tempSupabaseKey by remember(supabaseKey) { mutableStateOf(supabaseKey) }
    var isKeyVisible by remember { mutableStateOf(false) }

    var saveSuccessMessage by remember { mutableStateOf<String?>(null) }
    var sqlCopiedMessage by remember { mutableStateOf(false) }

    val recommendedSqlScript = remember {
        """
-- 1. Ensure copy_name and transport_status columns exist on public.transport
ALTER TABLE public.transport
  ADD COLUMN IF NOT EXISTS copy_name text,
  ADD COLUMN IF NOT EXISTS transport_status text;

-- 2. Optional Dedicated Audit Table for Dispatch & Return Scans
CREATE TABLE IF NOT EXISTS public.dispatch_return_logs (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  order_id uuid REFERENCES public.orders(id) ON DELETE SET NULL,
  order_form_number text NOT NULL,
  operation_mode text NOT NULL CHECK (operation_mode IN ('DISPATCH', 'RETURN')),
  vehicle_number text NOT NULL DEFAULT '',
  scanner_name text NOT NULL DEFAULT '',
  device_id text NOT NULL DEFAULT '',
  restocked boolean NOT NULL DEFAULT false,
  copy_type text DEFAULT '',
  customer_name text DEFAULT '',
  customer_district text DEFAULT '',
  table_size text DEFAULT '',
  colour text DEFAULT '',
  quantity integer NOT NULL DEFAULT 1,
  notes text DEFAULT '',
  created_at timestamp with time zone NOT NULL DEFAULT timezone('utc'::text, now()),
  CONSTRAINT dispatch_return_logs_pkey PRIMARY KEY (id)
);
        """.trimIndent()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
                Text(
                    text = "Factory system & Supabase",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Preconfigured to your central Supabase Factory Database (orders, profiles, transport).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Preconfigured Supabase database
        item {
            SectionCard {
                SectionHeader(
                    title = "Supabase database",
                    subtitle = "baxnczbtheknzpoerpfn.supabase.co",
                    icon = Icons.Default.Storage,
                    accent = accent,
                    trailing = { StatusPill(text = "Preconfigured", color = successColor) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = tempSupabaseUrl,
                    onValueChange = { tempSupabaseUrl = it },
                    label = { Text("Project URL") },
                    placeholder = { Text("https://baxnczbtheknzpoerpfn.supabase.co") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_supabase_url"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    shape = MaterialTheme.shapes.medium,
                    colors = fieldColors
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = tempSupabaseKey,
                    onValueChange = { tempSupabaseKey = it },
                    label = { Text("Publishable key") },
                    placeholder = { Text("sb_publishable_...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_supabase_key"),
                    singleLine = true,
                    visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                            AnimatedContent(
                                targetState = isKeyVisible,
                                transitionSpec = {
                                    (fadeIn(Motion.enter()) + scaleIn(Motion.enter(), initialScale = 0.7f))
                                        .togetherWith(fadeOut(Motion.exit()) + scaleOut(Motion.exit(), targetScale = 0.7f))
                                },
                                label = "keyVisibilityIcon"
                            ) { visible ->
                                Icon(
                                    imageVector = if (visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle key visibility"
                                )
                            }
                        }
                    },
                    shape = MaterialTheme.shapes.medium,
                    colors = fieldColors
                )

                Spacer(modifier = Modifier.height(12.dp))

                SoftButton(
                    text = "Test Supabase connection",
                    icon = Icons.Default.NetworkCheck,
                    onClick = { viewModel.testSupabaseConnection() },
                    color = accent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("test_supabase_connection_button")
                )

                StatusBanner(
                    message = testConnectionStatus,
                    toneOf = { if (it.startsWith("Success")) BannerTone.Success else BannerTone.Error },
                    innerPadding = PaddingValues(top = 12.dp)
                )
            }
        }

        // Station & dispatch filter configuration
        item {
            SectionCard {
                SectionHeader(
                    title = "Station & copy authorization",
                    subtitle = "Which copy types this station records",
                    icon = Icons.Default.Tune,
                    accent = accent
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    label = { Text("Scanner name / station ID") },
                    placeholder = { Text("Packing Station 01") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_scanner_name"),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    colors = fieldColors
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Authorized copies (dispatch filter)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))

                val copyOptions = listOf("ACCOUNT COPY", "GATE PASS", "TRANSPORT COPY", "PRODUCTION COPY")
                val selectedCopies = tempAuthorizedCopies.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableSet()

                copyOptions.forEach { option ->
                    val checked = selectedCopies.contains(option)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.small)
                            .toggleable(
                                value = checked,
                                role = Role.Checkbox,
                                onValueChange = { isChecked ->
                                    if (isChecked) {
                                        selectedCopies.add(option)
                                    } else {
                                        selectedCopies.remove(option)
                                    }
                                    tempAuthorizedCopies = selectedCopies.joinToString(", ")
                                }
                            )
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = null,
                            colors = CheckboxDefaults.colors(
                                checkedColor = accent,
                                uncheckedColor = MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier.padding(12.dp)
                        )
                        Text(text = option, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                AccentButton(
                    text = "Save all configurations",
                    icon = Icons.Default.Save,
                    onClick = {
                        viewModel.updateSettings(
                            name = tempName,
                            authorizedCopies = tempAuthorizedCopies,
                            supabaseUrl = tempSupabaseUrl,
                            supabaseKey = tempSupabaseKey
                        )
                        viewModel.triggerFeedback(context)
                        saveSuccessMessage = "Settings saved successfully!"
                    },
                    accent = accent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_settings_button")
                )

                StatusBanner(
                    message = saveSuccessMessage,
                    toneOf = { BannerTone.Success },
                    innerPadding = PaddingValues(top = 12.dp)
                )
            }
        }

        // Supabase schema & audit table SQL instructions
        item {
            SectionCard {
                SectionHeader(
                    title = "SQL audit table setup",
                    subtitle = "Optional: also maintain dispatch_return_logs",
                    icon = Icons.Default.Terminal,
                    accent = accent,
                    trailing = {
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(recommendedSqlScript))
                                sqlCopiedMessage = true
                            }
                        ) {
                            AnimatedContent(
                                targetState = sqlCopiedMessage,
                                transitionSpec = {
                                    (fadeIn(Motion.enter()) + scaleIn(Motion.enter(), initialScale = 0.6f))
                                        .togetherWith(fadeOut(Motion.exit()) + scaleOut(Motion.exit(), targetScale = 0.6f))
                                },
                                label = "copySqlIcon"
                            ) { copied ->
                                Icon(
                                    imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                                    contentDescription = "Copy SQL Script",
                                    tint = if (copied) successColor else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "The app works immediately with `orders`, `profiles`, and `transport`. Run this SQL in Supabase SQL Editor to also maintain `dispatch_return_logs`.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                AnimatedVisibility(
                    visible = sqlCopiedMessage,
                    enter = fadeIn(Motion.enter()) + expandVertically(Motion.enter()),
                    exit = fadeOut(Motion.exit()) + shrinkVertically(Motion.exit())
                ) {
                    Text(
                        text = "SQL script copied to clipboard!",
                        color = successColor,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = recommendedSqlScript,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background, MaterialTheme.shapes.small)
                        .padding(12.dp)
                )
            }
        }

        // Device ID
        item {
            SectionCard {
                SectionHeader(
                    title = "Device signature",
                    subtitle = "Android secure ID",
                    icon = Icons.Default.Fingerprint,
                    accent = accent
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = viewModel.deviceId,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background, MaterialTheme.shapes.small)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                )
            }
        }
    }
}
