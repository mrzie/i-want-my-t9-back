package com.t9launcher

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.t9launcher.ui.T9Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        window.attributes.layoutInDisplayCutoutMode =
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        super.onCreate(savedInstanceState)
        val startSettings = intent?.getBooleanExtra("open_settings", false) == true
        setContent {
            T9Theme {
                var screen by remember { mutableStateOf(if (startSettings) "settings" else "main") }
                when (screen) {
                    "main" -> T9LauncherScreen(onSettings = { screen = "settings" })
                    "settings" -> SettingsScreen(onBack = { screen = "main" })
                }
            }
        }
    }
}
