package com.t9launcher.model

import android.content.ComponentName
import android.graphics.drawable.Drawable

data class AppInfo(
    val label: String,
    val packageName: String,
    val icon: Drawable?,
    val component: ComponentName,
    val launchCount: Int = 0,
    val isSystem: Boolean = false
) {
    val labelLower: String = label.lowercase()
}
