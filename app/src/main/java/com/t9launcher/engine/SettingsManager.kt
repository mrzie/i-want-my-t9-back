package com.t9launcher.engine

import android.content.Context

object SettingsManager {
    private const val PREFS = "app_settings"
    private const val KEY_COLUMNS = "columns"
    private const val KEY_ICON_PACK_ID = "icon_pack_id"

    fun getColumns(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_COLUMNS, 4)
    }

    fun setColumns(context: Context, columns: Int) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_COLUMNS, columns).apply()
    }

    fun getIconPackId(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val id = prefs.getString(KEY_ICON_PACK_ID, null)
        return if (id.isNullOrEmpty()) null else id
    }

    fun setIconPackId(context: Context, id: String?) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (id == null) prefs.edit().remove(KEY_ICON_PACK_ID).apply()
        else prefs.edit().putString(KEY_ICON_PACK_ID, id).apply()
    }
}
