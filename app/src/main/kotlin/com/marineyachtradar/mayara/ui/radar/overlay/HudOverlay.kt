package com.marineyachtradar.mayara.ui.radar.overlay

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.marineyachtradar.mayara.data.model.NavigationData

/**
 * HUD overlay displaying connection label, heading, SOG, and COG.
 *
 * The navigation rows are absent when [navigationData] is null.
 * The connection label is shown when non-blank regardless of navigation data.
 *
 * Monospace font is used for the values to prevent layout jitter as numbers change.
 */
@Composable
fun HudOverlay(
    navigationData: NavigationData?,
    connectionLabel: String = "",
    modifier: Modifier = Modifier,
) {
    if (connectionLabel.isBlank() && navigationData == null) return

    Column(modifier = modifier.padding(12.dp)) {
        if (connectionLabel.isNotBlank()) {
            Text(
                text = connectionLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
        }
        navigationData?.headingDeg?.let { heading ->
            HudRow(label = "HDG", value = "%.1f°".format(heading))
        }
        navigationData?.sogKnots?.let { sog ->
            HudRow(label = "SOG", value = "%.1f kt".format(sog))
        }
        navigationData?.cogDeg?.let { cog ->
            HudRow(label = "COG", value = "%.1f°".format(cog))
        }
    }
}

@Composable
private fun HudRow(label: String, value: String) {
    Text(
        text = "$label  $value",
        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
        color = MaterialTheme.colorScheme.onSurface,
    )
}
