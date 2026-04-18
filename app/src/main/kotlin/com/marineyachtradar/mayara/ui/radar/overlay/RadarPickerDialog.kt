package com.marineyachtradar.mayara.ui.radar.overlay

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.marineyachtradar.mayara.data.model.RadarInfo

/**
 * Simple dialog listing available radars. Selecting one calls [onSelect]
 * with the radar's ID and dismisses the dialog.
 */
@Composable
fun RadarPickerDialog(
    radars: List<RadarInfo>,
    currentRadarId: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Radar") },
        text = {
            LazyColumn {
                items(radars) { radar ->
                    val isCurrent = radar.id == currentRadarId
                    Text(
                        text = radar.name.ifBlank { radar.id },
                        style = if (isCurrent) {
                            MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.primary,
                            )
                        } else {
                            MaterialTheme.typography.bodyLarge
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(radar.id) }
                            .padding(vertical = 12.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
