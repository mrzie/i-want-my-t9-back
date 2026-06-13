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
    var filterMode by remember { mutableIntStateOf(0) } // 0: all, 1: hidden, 2: visible

    LaunchedEffect(Unit) {
        allApps = loadAllApps(context)
        hidden = com.t9launcher.engine.HiddenAppsManager.getHidden(context)
    }

    val filtered = allApps.filter {
        val matchesSearch = it.label.contains(search, ignoreCase = true) || it.packageName.contains(search, ignoreCase = true)
        val isHidden = hidden.contains(it.packageName)
        val matchesFilter = when (filterMode) {
            1 -> isHidden
            2 -> !isHidden
            else -> true
        }
        matchesSearch && matchesFilter
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
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

            Spacer(modifier = Modifier.width(8.dp))

            val tabs = listOf("所有", "隐藏", "显示")
            val tabColors = listOf(Color(0xFF4A90D9), Color(0xFFFF6B6B), Color(0xFF66BB6A))
            tabs.forEachIndexed { index, label ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (filterMode == index) tabColors[index] else Color(0xFF2A2A2A))
                        .clickable { filterMode = index }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }
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

private fun loadAllApps(context: android.content.Context): List<com.t9launcher.model.AppInfo> {
    val pm = context.packageManager
    val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
    val result = apps.mapNotNull { appInfo ->
        val launchIntent = findLaunchIntent(pm, appInfo.packageName) ?: return@mapNotNull null
        com.t9launcher.model.AppInfo(
            label = appInfo.loadLabel(pm).toString(),
            packageName = appInfo.packageName,
            icon = appInfo.loadIcon(pm),
            component = launchIntent.component
                ?: android.content.ComponentName(appInfo.packageName, ""),
            isSystem = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
        )
    }.sortedBy { it.label.lowercase() }
    return result
}

private fun findLaunchIntent(pm: android.content.pm.PackageManager, packageName: String): android.content.Intent? {
    pm.getLaunchIntentForPackage(packageName)?.let { return it }
    val intent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
        addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        `package` = packageName
    }
    val resolveInfos = pm.queryIntentActivities(intent, 0)
    if (resolveInfos.isNotEmpty()) {
        val ri = resolveInfos[0]
        return android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
            setClassName(packageName, ri.activityInfo.name)
            addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        }
    }
    try {
        val pi = pm.getPackageInfo(packageName, android.content.pm.PackageManager.GET_ACTIVITIES)
        val mainActivity = pi.activities?.firstOrNull {
            it.name.endsWith(".MainActivity") || it.name.endsWith(".HomeActivity")
                    || it.name.endsWith(".LauncherActivity") || it.name.endsWith(".Main")
        } ?: pi.activities?.firstOrNull()
        if (mainActivity != null) {
            return android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
                setClassName(packageName, mainActivity.name)
                addCategory(android.content.Intent.CATEGORY_LAUNCHER)
            }
        }
    } catch (_: Exception) {}
    return null
}
