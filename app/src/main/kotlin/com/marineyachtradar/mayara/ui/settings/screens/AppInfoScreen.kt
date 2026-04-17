package com.marineyachtradar.mayara.ui.settings.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * App Info settings screen (spec §3.5).
 *
 * Displays static information about the app:
 * - App version (from [BuildConfig.VERSION_NAME] via [SettingsViewModel.appVersion])
 * - Radar firmware (not yet available from API — shows placeholder)
 * - License information
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppInfoScreen(
    appVersion: String,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Info") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            item {
                ListItem(
                    headlineContent = { Text("App Version") },
                    trailingContent = { Text(appVersion) },
                )
                HorizontalDivider()
            }
            item {
                ListItem(
                    headlineContent = { Text("Radar Firmware") },
                    trailingContent = { Text("N/A (not connected)") },
                )
                HorizontalDivider()
            }
            item {
                ListItem(
                    headlineContent = { Text("App License") },
                    trailingContent = { Text("Apache 2.0") },
                )
                HorizontalDivider()
            }
            item {
                ListItem(
                    headlineContent = { Text("Server License") },
                    supportingContent = { Text("mayara-server") },
                    trailingContent = { Text("GPL-2.0") },
                )
            }
        }
    }
}
