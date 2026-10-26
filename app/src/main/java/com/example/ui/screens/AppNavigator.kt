package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.LocalModeAccent
import com.example.ui.components.SegmentedModeSwitcher
import com.example.ui.components.animatedModeAccent
import com.example.ui.components.icon
import com.example.ui.theme.FactoryTheme
import com.example.ui.theme.Motion
import com.example.viewmodel.DispatchViewModel
import com.example.viewmodel.OperationMode

private data class TabSpec(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val contentDescription: String,
    val testTag: String
)

private val Tabs = listOf(
    TabSpec("Scanner", Icons.Outlined.QrCodeScanner, Icons.Default.QrCodeScanner, "Scanner", "nav_item_scanner"),
    TabSpec("Pending", Icons.Outlined.CloudSync, Icons.Default.CloudSync, "Pending Pack list", "nav_item_pending"),
    TabSpec("History", Icons.Outlined.History, Icons.Default.History, "History Log", "nav_item_history"),
    TabSpec("Settings", Icons.Outlined.Settings, Icons.Default.Settings, "Settings config", "nav_item_settings")
)

@Composable
fun AppNavigator(viewModel: DispatchViewModel, modifier: Modifier = Modifier) {
    var currentTab by rememberSaveable { mutableIntStateOf(0) }
    val currentMode by viewModel.currentMode.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val syncMessage by viewModel.syncMessage.collectAsStateWithLifecycle()
    val accent = animatedModeAccent(currentMode)

    CompositionLocalProvider(LocalModeAccent provides accent) {
        Scaffold(
            modifier = modifier,
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                Column(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.background)
                        .statusBarsPadding()
                ) {
                    HeaderBar(
                        title = when (currentTab) {
                            0 -> if (currentMode == OperationMode.DISPATCH) "Factory Dispatch" else "Reverse Logistics"
                            1 -> "Sync Queue"
                            2 -> "History"
                            3 -> "Settings"
                            else -> "Factory System"
                        },
                        subtitle = when (currentTab) {
                            0 -> if (currentMode == OperationMode.DISPATCH) "Station 01 · Outgoing active" else "Station 01 · Return & restock"
                            1 -> "Station 01 · Queued for sync"
                            2 -> "Station 01 · Audit logs"
                            3 -> "Station 01 · Configs & backend"
                            else -> "Station 01"
                        },
                        currentMode = currentMode,
                        accent = accent,
                        onIconClick = { currentTab = 3 }
                    )

                    // Mode switcher stays visible on every tab
                    SegmentedModeSwitcher(
                        currentMode = currentMode,
                        onModeSelected = { viewModel.setMode(it) }
                    )
                }
            },
            bottomBar = {
                AppBottomBar(
                    currentTab = currentTab,
                    accent = accent,
                    onTabSelected = { currentTab = it }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = {
                        val direction = if (targetState > initialState) 1 else -1
                        (fadeIn(Motion.enter()) + slideInHorizontally(Motion.enter()) { w -> direction * w / 8 })
                            .togetherWith(fadeOut(Motion.exit()) + slideOutHorizontally(Motion.exit()) { w -> -direction * w / 8 })
                            .using(SizeTransform(clip = false))
                    },
                    label = "tabContent"
                ) { tab ->
                    when (tab) {
                        0 -> ScannerScreen(viewModel)
                        1 -> PendingScreen(viewModel)
                        2 -> HistoryScreen(viewModel)
                        3 -> SettingsScreen(viewModel)
                    }
                }

                SyncOverlay(visible = isSyncing, message = syncMessage)
            }
        }
    }
}

@Composable
fun HeaderBar(
    title: String,
    subtitle: String,
    currentMode: OperationMode,
    accent: Color,
    onIconClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(accent, MaterialTheme.shapes.small),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = currentMode,
                transitionSpec = {
                    (fadeIn(Motion.enter()) + scaleIn(Motion.enter(), initialScale = 0.6f))
                        .togetherWith(fadeOut(Motion.exit()) + scaleOut(Motion.exit(), targetScale = 0.6f))
                },
                label = "headerModeIcon"
            ) { mode ->
                Icon(
                    imageVector = mode.icon,
                    contentDescription = null,
                    tint = FactoryTheme.colors.onAccent,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            AnimatedContent(
                targetState = title,
                transitionSpec = {
                    (fadeIn(Motion.enter()) + slideInVertically(Motion.enter()) { it / 3 })
                        .togetherWith(fadeOut(Motion.exit()) + slideOutVertically(Motion.exit()) { -it / 3 })
                },
                label = "headerTitle"
            ) { text ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            AnimatedContent(
                targetState = subtitle,
                transitionSpec = { fadeIn(Motion.enter()) togetherWith fadeOut(Motion.exit()) },
                label = "headerSubtitle"
            ) { text ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        IconButton(onClick = onIconClick, modifier = Modifier.size(44.dp)) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = "Header Settings",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun AppBottomBar(
    currentTab: Int,
    accent: Color,
    onTabSelected: (Int) -> Unit
) {
    Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
        NavigationBar(
            modifier = Modifier.testTag("app_navigation_bar"),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        ) {
            Tabs.forEachIndexed { index, tab ->
                val selected = currentTab == index
                NavigationBarItem(
                    selected = selected,
                    onClick = { onTabSelected(index) },
                    icon = {
                        Icon(
                            imageVector = if (selected) tab.selectedIcon else tab.icon,
                            contentDescription = tab.contentDescription
                        )
                    },
                    label = { Text(tab.label, style = MaterialTheme.typography.labelMedium) },
                    modifier = Modifier.testTag(tab.testTag),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = accent,
                        selectedTextColor = accent,
                        indicatorColor = accent.copy(alpha = 0.12f),
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}

@Composable
private fun SyncOverlay(visible: Boolean, message: String?) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(Motion.enter()),
        exit = fadeOut(Motion.exit())
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.32f)),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .animateEnterExit(
                        enter = scaleIn(Motion.enter(Motion.LONG), initialScale = 0.9f),
                        exit = scaleOut(Motion.exit(), targetScale = 0.95f)
                    )
                    .widthIn(max = 300.dp)
                    .padding(24.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = LocalModeAccent.current,
                        strokeWidth = 3.dp,
                        strokeCap = StrokeCap.Round,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = message ?: "Syncing with Supabase Factory Database...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
