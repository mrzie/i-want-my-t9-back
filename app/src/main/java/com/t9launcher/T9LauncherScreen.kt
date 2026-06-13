package com.t9launcher

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.t9launcher.engine.matchApps
import com.t9launcher.model.AppInfo

@Composable
fun T9LauncherScreen(onSettings: () -> Unit = {}) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var input by remember { mutableStateOf("") }
    var columns by remember { mutableIntStateOf(com.t9launcher.engine.SettingsManager.getColumns(context)) }

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            apps = loadApps(context)
            columns = com.t9launcher.engine.SettingsManager.getColumns(context)
        }
    }

    val filtered = remember(input, apps) { matchApps(input, apps) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        if (input.isNotEmpty() && filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "未匹配「$input」",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filtered) { app ->
                    AppItem(app, input) {
                        com.t9launcher.engine.LaunchCounter.increment(context, app.packageName)
                        val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                        if (launchIntent != null) {
                            context.startActivity(launchIntent)
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = input.ifEmpty { " " },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 16.sp,
                    textAlign = TextAlign.End
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            T9Keyboard(
                onKeyPress = { key ->
                    input = when (key) {
                        "DEL" -> input.dropLast(1)
                        "CLR" -> ""
                        else -> input + key
                    }
                },
                onSettings = onSettings
            )
        }
    }
}

@Composable
private fun AppItem(app: AppInfo, input: String, onClick: () -> Unit) {
    val matchedPositions = remember(input, app) {
        if (input.isEmpty()) emptyList()
        else {
            val digits = input.map { it - '0' }.filter { it in 0..9 }
            com.t9launcher.engine.findHighlightPositions(digits, app.label)
        }
    }

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val icon = app.icon
        if (icon != null) {
            androidx.compose.foundation.Image(
                bitmap = iconToBitmap(icon, 64).asImageBitmap(),
                contentDescription = app.label,
                modifier = Modifier.size(52.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        val annotated = buildAnnotatedString {
            for (i in app.label.indices) {
                if (i in matchedPositions) {
                    withStyle(SpanStyle(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )) { append(app.label[i]) }
                } else {
                    withStyle(SpanStyle(
                        color = MaterialTheme.colorScheme.onSurface
                    )) { append(app.label[i]) }
                }
            }
        }
        Text(
            text = annotated,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun T9Keyboard(onKeyPress: (String) -> Unit, onSettings: () -> Unit = {}) {
    val rows = listOf(
        listOf("1" to "", "2" to "ABC", "3" to "DEF"),
        listOf("4" to "GHI", "5" to "JKL", "6" to "MNO"),
        listOf("7" to "PQRS", "8" to "TUV", "9" to "WXYZ"),
        listOf("*" to "SETTINGS", "0" to "CLR", "#" to "DEL")
    )

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        for (row in rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                for ((key, sub) in row) {
                    val isSettings = key == "*"
                    val isClear = key == "0"
                    val isDelete = key == "#"
                    val displayText = when {
                        isSettings -> "⚙"
                        isClear -> "✕"
                        isDelete -> "⌫"
                        else -> key
                    }
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val bgColor = if (isPressed) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.surfaceContainerHigh

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(bgColor)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) {
                                when {
                                    isSettings -> onSettings()
                                    isClear -> onKeyPress("CLR")
                                    isDelete -> onKeyPress("DEL")
                                    else -> onKeyPress(key)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(displayText, color = MaterialTheme.colorScheme.onSurface, fontSize = if (isSettings || isClear || isDelete) 20.sp else 18.sp)
                            if (sub.isNotEmpty() && !isSettings && !isClear && !isDelete) {
                                Text(sub, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun loadApps(context: android.content.Context): List<AppInfo> {
    val pm = context.packageManager
    val counts = com.t9launcher.engine.LaunchCounter.getAll(context)
    val hidden = com.t9launcher.engine.HiddenAppsManager.getHidden(context)
    val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
    return apps.mapNotNull { appInfo ->
        if (hidden.contains(appInfo.packageName)) return@mapNotNull null
        val launchIntent = findLaunchIntent(pm, appInfo.packageName) ?: return@mapNotNull null
        AppInfo(
            label = appInfo.loadLabel(pm).toString(),
            packageName = appInfo.packageName,
            icon = appInfo.loadIcon(pm),
            component = launchIntent.component
                ?: android.content.ComponentName(appInfo.packageName, ""),
            launchCount = counts[appInfo.packageName] ?: 0
        )
    }.sortedBy { it.label.lowercase() }
}

private fun findLaunchIntent(pm: android.content.pm.PackageManager, packageName: String): Intent? {
    pm.getLaunchIntentForPackage(packageName)?.let { return it }
    val intent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
        addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        `package` = packageName
    }
    val resolveInfos = pm.queryIntentActivities(intent, 0)
    if (resolveInfos.isNotEmpty()) {
        val ri = resolveInfos[0]
        return Intent(android.content.Intent.ACTION_MAIN).apply {
            setClassName(packageName, ri.activityInfo.name)
            addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        }
    }
    try {
        val pi = pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
        val mainActivity = pi.activities?.firstOrNull {
            it.name.endsWith(".MainActivity") || it.name.endsWith(".HomeActivity")
                    || it.name.endsWith(".LauncherActivity") || it.name.endsWith(".Main")
        } ?: pi.activities?.firstOrNull()
        if (mainActivity != null) {
            return Intent(android.content.Intent.ACTION_MAIN).apply {
                setClassName(packageName, mainActivity.name)
                addCategory(android.content.Intent.CATEGORY_LAUNCHER)
            }
        }
    } catch (_: Exception) {}
    return null
}

internal fun iconToBitmap(drawable: android.graphics.drawable.Drawable, size: Int): android.graphics.Bitmap {
    val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    drawable.setBounds(0, 0, size, size)
    drawable.draw(canvas)
    return bitmap
}
