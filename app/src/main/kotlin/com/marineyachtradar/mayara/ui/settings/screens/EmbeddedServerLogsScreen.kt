package com.marineyachtradar.mayara.ui.settings.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.isActive
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Embedded Server Logs screen (spec §3.5 — Embedded Server Status).
 *
 * Auto-refreshes every 2 seconds via [LaunchedEffect]. The scroll state is maintained
 * in the composable so new lines appear at the bottom.
 *
 * @param logs      Current log output from [com.marineyachtradar.mayara.jni.RadarJni.getLogs].
 * @param onRefresh Called to fetch the latest logs (hoisted to [SettingsViewModel.refreshLogs]).
 * @param onBack    Called when the user presses the back button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmbeddedServerLogsScreen(
    logs: String,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
) {
    // Auto-refresh every 2 seconds while the screen is active
    LaunchedEffect(Unit) {
        while (isActive) {
            onRefresh()
            delay(2_000)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Server Logs") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refresh logs",
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        if (logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No logs available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            val lines = logs.lines()
            val listState = rememberLazyListState()

            // Scroll to the last line when new logs arrive
            LaunchedEffect(lines.size) {
                if (lines.isNotEmpty()) {
                    listState.animateScrollToItem(lines.lastIndex)
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                items(lines) { line ->
                    Text(
                        text = line,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = logLineColor(line),
                        modifier = Modifier.padding(vertical = 1.dp),
                    )
                }
            }
        }
    }
}

/** Colour-code log lines by severity prefix. */
@Composable
private fun logLineColor(line: String) = when {
    line.startsWith("[ERROR]") -> MaterialTheme.colorScheme.error
    line.startsWith("[WARN]") -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.onSurface
}
