package com.marineyachtradar.mayara.ui.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.marineyachtradar.mayara.ui.theme.MayaraTheme

/**
 * Full-screen Settings Activity (spec §3.5).
 *
 * Kept separate from [com.marineyachtradar.mayara.MainActivity] so it has its own back stack
 * and the radar view remains alive in the background while settings are open.
 *
 * Navigation is handled by [SettingsNavHost]; state is owned by [SettingsViewModel].
 */
class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MayaraTheme {
                val viewModel: SettingsViewModel = viewModel()
                SettingsNavHost(
                    viewModel = viewModel,
                    onFinish = { finish() },
                )
            }
        }
    }
}
