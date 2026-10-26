package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AnimatedCounter
import com.example.ui.components.EmptyState
import com.example.ui.components.LocalModeAccent
import com.example.ui.components.SoftButton
import com.example.ui.components.StatusBanner
import com.example.ui.theme.FactoryTheme
import com.example.ui.theme.Motion
import com.example.viewmodel.DispatchViewModel

@Composable
fun PendingScreen(viewModel: DispatchViewModel) {
    val history by viewModel.scannedHistory.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val syncMessage by viewModel.syncMessage.collectAsStateWithLifecycle()
    val accent = LocalModeAccent.current

    val pendingRecords = remember(history) {
        history.filter { it.syncStatus == "PENDING" }
    }

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
                AnimatedCounter(count = pendingRecords.size)
                Text(
                    text = "queue records waiting for sync",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(
                visible = pendingRecords.isNotEmpty(),
                enter = fadeIn(Motion.enter()) + scaleIn(Motion.enter(), initialScale = 0.9f),
                exit = fadeOut(Motion.exit()) + scaleOut(Motion.exit(), targetScale = 0.9f)
            ) {
                SoftButton(
                    text = "Sync all",
                    icon = Icons.Default.Sync,
                    onClick = { viewModel.forceSyncPending() },
                    enabled = !isSyncing,
                    color = accent,
                    modifier = Modifier.testTag("sync_all_button")
                )
            }
        }

        StatusBanner(
            message = syncMessage,
            accent = accent,
            innerPadding = PaddingValues(bottom = 12.dp),
            onDismiss = { viewModel.clearSyncMessage() }
        )

        AnimatedContent(
            targetState = pendingRecords.isEmpty(),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            transitionSpec = { fadeIn(Motion.enter()) togetherWith fadeOut(Motion.exit()) },
            label = "pendingContent"
        ) { isEmpty ->
            if (isEmpty) {
                EmptyState(
                    icon = Icons.Default.CloudDone,
                    iconContentDescription = "Sync complete sign",
                    title = "All dispatch & return records synced",
                    body = "Scans done offline are kept inside local SQLite and synced to the preconfigured Supabase factory database automatically.",
                    tint = FactoryTheme.colors.success,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(pendingRecords, key = { it.id }) { record ->
                        HistoryItemCard(
                            record = record,
                            onClick = {},
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }
    }
}
