package com.t9launcher

import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HiddenAppsScreen(onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var allApps by remember { mutableStateOf<List<com.t9launcher.model.AppInfo>>(emptyList()) }
    var hidden by remember { mutableStateOf(setOf<String>()) }
    var search by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val result = loadAllApps(context)
        allApps = result.first
        hidden = com.t9launcher.engine.HiddenAppsManager.getHidden(context)
    }

    val filtered = allApps.filter {
        it.label.contains(search, ignoreCase = true) || it.packageName.contains(search, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "✕",
                color = Color.White,
                fontSize = 20.sp,
                modifier = Modifier
                    .clickable { onBack() }
                    .padding(end = 12.dp)
            )
            Text("不显示应用", color = Color.White, fontSize = 20.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF2A2A2A))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            if (search.isEmpty()) {
                Text("搜索应用...", color = Color(0xFF666666), fontSize = 14.sp)
            }
            androidx.compose.foundation.text.BasicTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.fillMaxWidth(),
                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 14.sp),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            items(filtered) { app ->
                val isHidden = hidden.contains(app.packageName)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isHidden) Color(0xFF333333) else Color.Transparent)
                        .clickable {
                            com.t9launcher.engine.HiddenAppsManager.toggle(context, app.packageName)
                            hidden = com.t9launcher.engine.HiddenAppsManager.getHidden(context)
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val icon = app.icon
                    if (icon != null) {
                        androidx.compose.foundation.Image(
                            bitmap = iconToBitmap(icon, 48).asImageBitmap(),
                            contentDescription = app.label,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = app.label,
                            color = Color.White,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = app.packageName,
                            color = Color(0xFF666666),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = if (isHidden) "已隐藏" else "显示",
                        color = if (isHidden) Color(0xFFFF6B6B) else Color(0xFF66BB6A),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

private fun loadAllApps(context: android.content.Context): Pair<List<com.t9launcher.model.AppInfo>, Int> {
    val pm = context.packageManager
    val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
    val result = apps.mapNotNull { appInfo ->
        val launchIntent = pm.getLaunchIntentForPackage(appInfo.packageName) ?: return@mapNotNull null
        com.t9launcher.model.AppInfo(
            label = appInfo.loadLabel(pm).toString(),
            packageName = appInfo.packageName,
            icon = appInfo.loadIcon(pm),
            component = launchIntent.component
                ?: android.content.ComponentName(appInfo.packageName, ""),
            isSystem = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
        )
    }.sortedBy { it.label.lowercase() }
    return Pair(result, 0)
}
