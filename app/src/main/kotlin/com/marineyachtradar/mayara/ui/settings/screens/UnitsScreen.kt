package com.marineyachtradar.mayara.ui.settings.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.marineyachtradar.mayara.data.model.BearingMode
import com.marineyachtradar.mayara.data.model.DistanceUnit

/**
 * Units & Formats settings screen (spec §3.5).
 *
 * Presents [FilterChip] selectors for:
 * - **Distance unit**: NM / km / SM
 * - **Bearing mode**: True / Magnetic
 *
 * All state is hoisted; selection callbacks are forwarded to [SettingsViewModel].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitsScreen(
    distanceUnit: DistanceUnit,
    bearingMode: BearingMode,
    onDistanceUnitChange: (DistanceUnit) -> Unit,
    onBearingModeChange: (BearingMode) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Units & Formats") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {

            // ── Distance section ───────────────────────────────────────
            Text(
                text = "Distance",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DistanceUnit.entries.forEach { unit ->
                    FilterChip(
                        selected = distanceUnit == unit,
                        onClick = { onDistanceUnitChange(unit) },
                        label = { Text(unit.label) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))

            // ── Bearing section ────────────────────────────────────────
            Text(
                text = "Bearing Type",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BearingMode.entries.forEach { mode ->
                    FilterChip(
                        selected = bearingMode == mode,
                        onClick = { onBearingModeChange(mode) },
                        label = { Text(mode.label) },
                    )
                }
            }
        }
    }
}
