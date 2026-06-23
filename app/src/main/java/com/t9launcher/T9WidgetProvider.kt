package com.t9launcher

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.widget.RemoteViews

class T9WidgetProvider : AppWidgetProvider() {

    companion object {
        private const val PREFS = "t9_widget_prefs"
        private const val KEY_INPUT = "input"
        private const val KEY_CACHED_COLS = "cached_cols"
        private const val KEY_ROWS = "rows"
        private const val ACTION_KEY_PRESS = "com.t9launcher.KEY_PRESS"
        private const val ACTION_APP_CLICK = "com.t9launcher.APP_CLICK"
        private const val EXTRA_KEY = "key"
        private const val EXTRA_PKG = "pkg"
        private const val ROWS_THRESHOLD_DP = 280
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        for (id in ids) {
            updateWidget(context, manager, id)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        val prefs = getPrefs(context)
        val minHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
        val rows = if (minHeight >= ROWS_THRESHOLD_DP) 2 else 1
        prefs.edit().putInt(KEY_ROWS, rows).apply()
        updateWidget(context, appWidgetManager, appWidgetId)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_KEY_PRESS -> {
                val key = intent.getStringExtra(EXTRA_KEY) ?: return
                val prefs = getPrefs(context)
                var input = prefs.getString(KEY_INPUT, "") ?: ""
                input = when (key) {
                    "CLR" -> ""
                    "DEL" -> input.dropLast(1)
                    else -> input + key
                }
                prefs.edit().putString(KEY_INPUT, input).apply()
                refreshAll(context)
            }
            ACTION_APP_CLICK -> {
                val pkg = intent.getStringExtra(EXTRA_PKG) ?: return
                com.t9launcher.engine.LaunchCounter.increment(context, pkg)
                val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                }
                getPrefs(context).edit().putString(KEY_INPUT, "").apply()
                refreshAll(context)
            }
        }
    }

    private fun refreshAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, T9WidgetProvider::class.java))
        for (id in ids) {
            updateWidget(context, manager, id)
        }
    }

    private fun updateWidget(context: Context, manager: AppWidgetManager, id: Int) {
        val prefs = getPrefs(context)
        val input = prefs.getString(KEY_INPUT, "") ?: ""
        val currentCols = com.t9launcher.engine.SettingsManager.getColumns(context)
        val cachedCols = prefs.getInt(KEY_CACHED_COLS, currentCols)
        val currentRows = prefs.getInt(KEY_ROWS, 1)
        val layoutChanged = currentCols != cachedCols

        val layoutRes = when (currentRows) {
            2 -> when (currentCols) {
                3 -> R.layout.widget_t9_3col_2row
                4 -> R.layout.widget_t9_4col_2row
                else -> R.layout.widget_t9_2row
            }
            else -> when (currentCols) {
                3 -> R.layout.widget_t9_3col
                4 -> R.layout.widget_t9_4col
                else -> R.layout.widget_t9
            }
        }

        val views = RemoteViews(context.packageName, layoutRes)

        views.setTextViewText(R.id.widget_input_display, input.ifEmpty { " " })

        val apps = com.t9launcher.engine.matchApps(input, loadApps(context))

        if (input.isNotEmpty() && apps.isEmpty()) {
            views.setViewVisibility(R.id.widget_no_match, android.view.View.VISIBLE)
            views.setTextViewText(R.id.widget_no_match, "未匹配「$input」")
        } else {
            views.setViewVisibility(R.id.widget_no_match, android.view.View.GONE)
        }

        val maxSlots = getMaxSlots(currentCols, currentRows)
        for (i in 0 until maxSlots) {
            val iconId = context.resources.getIdentifier("app_icon_$i", "id", context.packageName)
            val labelId = context.resources.getIdentifier("app_label_$i", "id", context.packageName)
            val slotId = context.resources.getIdentifier("app_slot_$i", "id", context.packageName)

            if (iconId == 0 || labelId == 0 || slotId == 0) continue

            if (i < apps.size) {
                val app = apps[i]
                views.setViewVisibility(iconId, android.view.View.VISIBLE)
                views.setViewVisibility(labelId, android.view.View.VISIBLE)

                views.setTextViewText(labelId, app.label)
                val icon = app.icon
                if (icon != null) {
                    views.setImageViewBitmap(iconId, drawableToBitmap(icon, 72))
                }

                val clickIntent = Intent(context, T9WidgetProvider::class.java).apply {
                    action = ACTION_APP_CLICK
                    putExtra(EXTRA_PKG, app.packageName)
                }
                val pi = PendingIntent.getBroadcast(
                    context, 200 + i, clickIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(slotId, pi)
            } else {
                views.setViewVisibility(iconId, android.view.View.GONE)
                views.setViewVisibility(labelId, android.view.View.GONE)
            }
        }

        if (layoutChanged) {
            bindAllKeys(context, views)
            prefs.edit().putInt(KEY_CACHED_COLS, currentCols).apply()
        } else {
            bindAllKeys(context, views)
        }

        manager.updateAppWidget(id, views)
    }

    private fun getMaxSlots(cols: Int, rows: Int): Int = when (rows) {
        2 -> when (cols) {
            3 -> 6
            4 -> 8
            else -> 10
        }
        else -> when (cols) {
            3 -> 3
            4 -> 4
            else -> 5
        }
    }

    private fun bindAllKeys(context: Context, views: RemoteViews) {
        bindKeyClick(context, views, R.id.btn_settings, "SETTINGS")
        bindKeyClick(context, views, R.id.btn_clr, "CLR")
        bindKeyClick(context, views, R.id.btn_del, "DEL")

        for (i in 1..9) {
            val resId = context.resources.getIdentifier("btn_$i", "id", context.packageName)
            if (resId != 0) bindKeyClick(context, views, resId, "$i")
        }

        val settingsIntent = Intent(context, MainActivity::class.java).apply {
            putExtra("open_settings", true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val settingsPi = PendingIntent.getActivity(
            context, 300, settingsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_settings, settingsPi)
    }

    private fun bindKeyClick(context: Context, views: RemoteViews, viewId: Int, key: String) {
        val intent = Intent(context, T9WidgetProvider::class.java).apply {
            action = ACTION_KEY_PRESS
            putExtra(EXTRA_KEY, key)
        }
        val pi = PendingIntent.getBroadcast(
            context, viewId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(viewId, pi)
    }

    private fun loadApps(context: Context): List<com.t9launcher.model.AppInfo> {
        val pm = context.packageManager
        val counts = com.t9launcher.engine.LaunchCounter.getAll(context)
        val hidden = com.t9launcher.engine.HiddenAppsManager.getHidden(context)
        val apps = pm.getInstalledApplications(android.content.pm.PackageManager.GET_META_DATA)
        return apps.mapNotNull { appInfo ->
            if (hidden.contains(appInfo.packageName)) return@mapNotNull null
            val launchIntent = findLaunchIntent(pm, appInfo.packageName) ?: return@mapNotNull null
            val systemIcon = appInfo.loadIcon(pm)
            com.t9launcher.model.AppInfo(
                label = appInfo.loadLabel(pm).toString(),
                packageName = appInfo.packageName,
                icon = com.t9launcher.engine.IconResolver.resolve(context, appInfo.packageName, systemIcon),
                component = launchIntent.component
                    ?: ComponentName(appInfo.packageName, ""),
                launchCount = counts[appInfo.packageName] ?: 0
            )
        }.sortedBy { it.label.lowercase() }
    }

    private fun findLaunchIntent(pm: android.content.pm.PackageManager, packageName: String): Intent? {
        pm.getLaunchIntentForPackage(packageName)?.let { return it }
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            `package` = packageName
        }
        val resolveInfos = pm.queryIntentActivities(intent, 0)
        if (resolveInfos.isNotEmpty()) {
            val ri = resolveInfos[0]
            return Intent(Intent.ACTION_MAIN).apply {
                setClassName(packageName, ri.activityInfo.name)
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
        }
        try {
            val pi = pm.getPackageInfo(packageName, android.content.pm.PackageManager.GET_ACTIVITIES)
            val mainActivity = pi.activities?.firstOrNull {
                it.name.endsWith(".MainActivity") || it.name.endsWith(".HomeActivity")
                        || it.name.endsWith(".LauncherActivity") || it.name.endsWith(".Main")
            } ?: pi.activities?.firstOrNull()
            if (mainActivity != null) {
                return Intent(Intent.ACTION_MAIN).apply {
                    setClassName(packageName, mainActivity.name)
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
            }
        } catch (_: Exception) {}
        return null
    }

    private fun drawableToBitmap(drawable: android.graphics.drawable.Drawable, size: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(canvas)
        return bitmap
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }
}
