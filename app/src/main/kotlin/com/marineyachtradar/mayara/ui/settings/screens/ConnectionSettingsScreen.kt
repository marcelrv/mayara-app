package com.marineyachtradar.mayara.ui.settings.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.marineyachtradar.mayara.ui.settings.ConnectionSettingsState
import kotlinx.coroutines.launch

/**
 * Connection Manager settings screen (spec §3.5).
 *
 * Displays the active connection mode and allows the user to:
 * - Clear the remembered choice ("Switch Connection")
 * - Save a manual network override (IP + port)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionSettingsScreen(
    uiState: ConnectionSettingsState,
    onSwitchConnection: () -> Unit,
    onSaveManualConnection: (host: String, port: Int) -> Unit,
    onBack: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Connection Manager") },
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {

            // ── Active connection status card ──────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Active Connection",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = uiState.displayLabel,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            // ── Switch Connection ──────────────────────────────────────
            OutlinedButton(
                onClick = {
                    onSwitchConnection()
                    scope.launch {
                        snackbarHostState.showSnackbar("Connection preference cleared")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text("Switch Connection")
            }

            HorizontalDivider()

            // ── Manual Override section ────────────────────────────────
            Text(
                text = "Manual Network Override",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Connect directly to a specific server by IP address and port.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            var host by rememberSaveable { mutableStateOf(uiState.rememberedHost) }
            var portText by rememberSaveable { mutableStateOf(uiState.rememberedPort) }
            val portValid = isValidPort(portText)

            OutlinedTextField(
                value = host,
                onValueChange = { host = it },
                label = { Text("Host / IP Address") },
                placeholder = { Text("e.g. 192.168.1.100") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = portText,
                onValueChange = { portText = it },
                label = { Text("Port") },
                placeholder = { Text("6502") },
                singleLine = true,
                isError = portText.isNotEmpty() && !portValid,
                supportingText = {
                    if (portText.isNotEmpty() && !portValid) {
                        Text("Port must be between 1 and 65535")
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        onSaveManualConnection(host, portText.toInt())
                        scope.launch {
                            snackbarHostState.showSnackbar("Connection saved")
                        }
                    },
                    enabled = host.isNotBlank() && portValid,
                ) {
                    Text("Save")
                }
            }
        }
    }
}

/**
 * Returns true if [port] is a valid TCP port number string (1–65535).
 *
 * Exposed as [internal] for JVM unit tests (mirrors the [nextPowerTarget] pattern).
 */
internal fun isValidPort(port: String): Boolean {
    val value = port.toIntOrNull() ?: return false
    return value in 1..65535
}
