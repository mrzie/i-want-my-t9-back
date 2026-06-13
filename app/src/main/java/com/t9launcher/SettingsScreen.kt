package com.t9launcher

import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    var subPage by remember { mutableStateOf<String?>(null) }

    when (subPage) {
        "hidden_apps" -> HiddenAppsPage(onBack = { subPage = null })
        else -> SettingsMainPage(onBack = onBack, onNavigate = { subPage = it })
    }
}

@Composable
private fun SettingsMainPage(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var columns by remember { mutableIntStateOf(com.t9launcher.engine.SettingsManager.getColumns(context)) }
    val hiddenCount = remember {
        mutableIntStateOf(com.t9launcher.engine.HiddenAppsManager.getHidden(context).size)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "✕",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 20.sp,
                modifier = Modifier
                    .clickable { onBack() }
                    .padding(end = 12.dp)
            )
            Text("设置", color = MaterialTheme.colorScheme.onBackground, fontSize = 20.sp)
        }

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("应用列数", color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (opt in listOf(3, 4, 5)) {
                        FilterChip(
                            selected = opt == columns,
                            onClick = {
                                columns = opt
                                com.t9launcher.engine.SettingsManager.setColumns(context, opt)
                            },
                            label = { Text("${opt}列") }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigate("hidden_apps") },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("不显示应用", color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("已隐藏 ${hiddenCount.intValue} 个应用", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
                Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 20.sp)
            }
        }
    }
}

@Composable
private fun HiddenAppsPage(onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var allApps by remember { mutableStateOf<List<com.t9launcher.model.AppInfo>>(emptyList()) }
    var hidden by remember { mutableStateOf(setOf<String>()) }
    var search by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }
    var showHidden by remember { mutableStateOf(false) }
    var autoHiddenCount by remember { mutableIntStateOf(0) }
    val tabs = listOf("用户应用", "系统应用")

    LaunchedEffect(Unit) {
        val result = loadAllApps(context)
        allApps = result.first
        autoHiddenCount = result.second
        hidden = com.t9launcher.engine.HiddenAppsManager.getHidden(context)
    }

    val displayed = allApps.filter { app ->
        val matchSearch = app.label.contains(search, ignoreCase = true) ||
                app.packageName.contains(search, ignoreCase = true)
        val isHidden = hidden.contains(app.packageName)
        matchSearch && when (selectedTab) {
            0 -> !app.isSystem && (!isHidden || showHidden)
            1 -> app.isSystem && (!isHidden || showHidden)
            else -> true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "‹",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 22.sp,
                modifier = Modifier
                    .clickable { onBack() }
                    .padding(end = 12.dp)
            )
            Text("不显示应用", color = MaterialTheme.colorScheme.onBackground, fontSize = 20.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("搜索应用...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "显示已隐藏应用",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp
            )
            Switch(
                checked = showHidden,
                onCheckedChange = { showHidden = it }
            )
        }

        if (autoHiddenCount > 0) {
            Text(
                text = "已自动隐藏 $autoHiddenCount 个无图标应用",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontSize = 13.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            items(displayed) { app ->
                val isHidden = hidden.contains(app.packageName)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val icon = app.icon
                    if (icon != null) {
                        androidx.compose.foundation.Image(
                            bitmap = iconToBitmap(icon, 48).asImageBitmap(),
                            contentDescription = app.label,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = app.label,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = app.packageName,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Switch(
                        checked = isHidden,
                        onCheckedChange = {
                            com.t9launcher.engine.HiddenAppsManager.toggle(context, app.packageName)
                            hidden = com.t9launcher.engine.HiddenAppsManager.getHidden(context)
                        }
                    )
                }
            }
        }
    }
}

private fun loadAllApps(context: android.content.Context): Pair<List<com.t9launcher.model.AppInfo>, Int> {
    val pm = context.packageManager
    val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
    
    val prefs = context.getSharedPreferences("hidden_apps", android.content.Context.MODE_PRIVATE)
    val isFirstLoad = !prefs.contains("first_load_done")
    var autoHiddenCount = 0
    
    val result = apps.mapNotNull { appInfo ->
        val launchIntent = pm.getLaunchIntentForPackage(appInfo.packageName) ?: return@mapNotNull null
        val icon = appInfo.loadIcon(pm)
        
        // 无图标且首次加载时自动隐藏
        if (icon == null && isFirstLoad) {
            com.t9launcher.engine.HiddenAppsManager.toggle(context, appInfo.packageName)
            autoHiddenCount++
        }
        
        com.t9launcher.model.AppInfo(
            label = appInfo.loadLabel(pm).toString(),
            packageName = appInfo.packageName,
            icon = icon,
            component = launchIntent.component
                ?: android.content.ComponentName(appInfo.packageName, ""),
            isSystem = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
        )
    }.sortedBy { it.label.lowercase() }
    
    // 标记首次加载完成
    if (isFirstLoad) {
        prefs.edit().putBoolean("first_load_done", true).apply()
    }
    
    return Pair(result, autoHiddenCount)
}
