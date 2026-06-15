package com.t9launcher

import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.t9launcher.engine.HiddenAppsFilter

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    var subPage by remember { mutableStateOf<String?>(null) }

    when (subPage) {
        "hidden_apps" -> HiddenAppsPage(onBack = { subPage = null })
        "about" -> AboutPage(onBack = { subPage = null })
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
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "关闭",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .clickable { onBack() }
                    .padding(end = 12.dp)
                    .size(24.dp)
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
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigate("about") },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("关于 T9 Search", color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp, modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
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
    var selectedTab by remember { mutableIntStateOf(0) } // 0: user, 1: system
    var sortMode by remember { mutableIntStateOf(0) } // 0: alpha, 1: visible first, 2: hidden first
    var showFilterDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val result = loadAllApps(context)
        allApps = result.first
        hidden = com.t9launcher.engine.HiddenAppsManager.getHidden(context)
    }

    val displayed = HiddenAppsFilter.filterAndSort(
        apps = allApps,
        search = search,
        isSystem = selectedTab == 1,
        hidden = hidden,
        sortMode = sortMode
    )

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
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .clickable { onBack() }
                    .padding(end = 12.dp)
                    .size(24.dp)
            )
            Text("隐藏应用", color = MaterialTheme.colorScheme.onBackground, fontSize = 20.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            val tabs = listOf("用户软件", "系统软件")
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontSize = 13.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.weight(1f),
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

            Spacer(modifier = Modifier.width(8.dp))

            Box {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable { showFilterDialog = true }
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "筛选",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = showFilterDialog,
                    onDismissRequest = { showFilterDialog = false }
                ) {
                    val options = listOf("字母排序" to 0, "显示优先" to 1, "隐藏优先" to 2)
                    options.forEach { (label, mode) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                sortMode = mode
                                showFilterDialog = false
                            },
                            leadingIcon = {
                                if (sortMode == mode) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

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
        val launchIntent = findLaunchIntent(pm, appInfo.packageName) ?: return@mapNotNull null
        val icon = appInfo.loadIcon(pm)
        val hasStandardLauncher = pm.getLaunchIntentForPackage(appInfo.packageName) != null
        
        // 首次加载时，无图标或无标准启动 Intent 的应用自动隐藏
        if (isFirstLoad && (icon == null || !hasStandardLauncher)) {
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

@Composable
private fun AboutPage(onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
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
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .clickable { onBack() }
                    .padding(end = 12.dp)
                    .size(24.dp)
            )
            Text("关于 T9 Search", color = MaterialTheme.colorScheme.onBackground, fontSize = 20.sp)
        }

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "为了纪念系统功能本该有的 T9 应用搜索能力，我们只能用一种迂回的办法挽留这个功能。",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "GitHub",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp,
                    modifier = Modifier.clickable {
                        val intent = android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse("https://github.com/mrzie/i-want-my-t9-back")
                        )
                        context.startActivity(intent)
                    }
                )
            }
        }
    }
}
