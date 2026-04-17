package com.marineyachtradar.mayara.ui.connection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.marineyachtradar.mayara.data.model.ConnectionMode
import com.marineyachtradar.mayara.data.model.DiscoveredServer

/**
 * Modal dialog allowing the user to choose a connection mode (spec §2 / §3.5).
 *
 * Shown on first launch or when the user taps "Switch Connection" in Settings.
 *
 * - **Embedded**: the built-in JNI radar server running in-process on this device.
 * - **Network**: a remote mayara-server or SignalK node.
 *   - If [discoveredServers] is non-empty, the user picks from a list.
 *   - Otherwise the user enters a host/IP and port manually.
 *
 * "Remember my choice" persists the selection via [ConnectionManager] (Phase 5).
 *
 * @param discoveredServers mDNS-discovered servers to present as network options.
 * @param onConnect Called when the user confirms. Provides the chosen [ConnectionMode]
 *                  and whether to persist the choice.
 * @param onDismiss Called when the user dismisses without making a selection.
 */
@Composable
fun ConnectionPickerDialog(
    discoveredServers: List<DiscoveredServer>,
    onConnect: (mode: ConnectionMode, remember: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedOption by rememberSaveable { mutableStateOf(PickerOption.EMBEDDED) }
    var rememberChoice by rememberSaveable { mutableStateOf(false) }
    var selectedServerIndex by rememberSaveable { mutableIntStateOf(0) }
    var manualHost by rememberSaveable { mutableStateOf("") }
    var manualPort by rememberSaveable { mutableStateOf("6502") }

    val manualHostValid = manualHost.isNotBlank() && manualHost.length <= 255
    val manualPortValid = manualPort.toIntOrNull()?.let { it in 1..65535 } == true

    val connectEnabled = when (selectedOption) {
        PickerOption.EMBEDDED -> true
        PickerOption.NETWORK -> {
            if (discoveredServers.isEmpty()) manualHostValid && manualPortValid
            else true
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Connect to Radar", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // ---- Embedded option ------------------------------------
                ConnectionOptionCard(
                    selected = selectedOption == PickerOption.EMBEDDED,
                    onSelect = { selectedOption = PickerOption.EMBEDDED },
                    title = "Built-in Radar",
                    subtitle = "Uses the embedded radar server running on this device",
                    icon = { Icon(Icons.Filled.Computer, contentDescription = null) },
                )

                // ---- Network option -------------------------------------
                ConnectionOptionCard(
                    selected = selectedOption == PickerOption.NETWORK,
                    onSelect = { selectedOption = PickerOption.NETWORK },
                    title = "Network Server",
                    subtitle = when {
                        discoveredServers.isEmpty() -> "Enter the IP address of a remote server"
                        discoveredServers.size == 1 -> "1 server found on your network"
                        else -> "${discoveredServers.size} servers found on your network"
                    },
                    icon = { Icon(Icons.Filled.Wifi, contentDescription = null) },
                )

                // ---- Network sub-options (visible when Network selected) -
                if (selectedOption == PickerOption.NETWORK) {
                    if (discoveredServers.isNotEmpty()) {
                        // Discovered server list
                        Column(
                            modifier = Modifier.padding(start = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            discoveredServers.forEachIndexed { index, server ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .selectable(
                                            selected = selectedServerIndex == index,
                                            onClick = { selectedServerIndex = index },
                                            role = Role.RadioButton,
                                        )
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    RadioButton(
                                        selected = selectedServerIndex == index,
                                        onClick = { selectedServerIndex = index },
                                    )
                                    Column {
                                        Text(
                                            text = server.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                        Text(
                                            text = "${server.host}:${server.port}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Manual IP entry
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = manualHost,
                                onValueChange = { manualHost = it.trim() },
                                label = { Text("Host / IP Address") },
                                placeholder = { Text("e.g. 192.168.1.100") },
                                singleLine = true,
                                isError = manualHost.isNotEmpty() && !manualHostValid,
                                supportingText = if (manualHost.isNotEmpty() && !manualHostValid) {
                                    { Text("Enter a valid hostname or IP address") }
                                } else null,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                value = manualPort,
                                onValueChange = { manualPort = it.trim() },
                                label = { Text("Port") },
                                placeholder = { Text("6502") },
                                singleLine = true,
                                isError = manualPort.isNotEmpty() && !manualPortValid,
                                supportingText = if (manualPort.isNotEmpty() && !manualPortValid) {
                                    { Text("Port must be 1–65535") }
                                } else null,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // ---- Remember my choice --------------------------------
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = rememberChoice,
                            onClick = { rememberChoice = !rememberChoice },
                            role = Role.Checkbox,
                        )
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Checkbox(
                        checked = rememberChoice,
                        onCheckedChange = { rememberChoice = it },
                    )
                    Text(
                        text = "Remember my choice",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = connectEnabled,
                onClick = {
                    val mode: ConnectionMode = when (selectedOption) {
                        PickerOption.EMBEDDED -> ConnectionMode.Embedded()
                        PickerOption.NETWORK -> {
                            val baseUrl = if (discoveredServers.isNotEmpty()) {
                                discoveredServers[selectedServerIndex].baseUrl
                            } else {
                                val port = manualPort.toIntOrNull() ?: 6502
                                "http://$manualHost:$port"
                            }
                            ConnectionMode.Network(baseUrl)
                        }
                    }
                    onConnect(mode, rememberChoice)
                },
            ) {
                Text("Connect")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

// ---------------------------------------------------------------------------
// Private composable helpers
// ---------------------------------------------------------------------------

@Composable
private fun ConnectionOptionCard(
    selected: Boolean,
    onSelect: () -> Unit,
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
) {
    val containerColor = if (selected)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surfaceVariant

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            icon()
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            RadioButton(selected = selected, onClick = onSelect)
        }
    }
}

// ---------------------------------------------------------------------------
// Internal state enum — exposed for unit testing
// ---------------------------------------------------------------------------

/** The two top-level choices in the connection picker. */
internal enum class PickerOption { EMBEDDED, NETWORK }
