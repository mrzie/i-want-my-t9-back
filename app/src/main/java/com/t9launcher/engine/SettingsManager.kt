package com.t9launcher.engine

import android.content.Context

object SettingsManager {
    private const val PREFS = "app_settings"
    private const val KEY_COLUMNS = "columns"

    fun getColumns(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_COLUMNS, 4)
    }

    fun setColumns(context: Context, columns: Int) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_COLUMNS, columns).apply()
    }
}
