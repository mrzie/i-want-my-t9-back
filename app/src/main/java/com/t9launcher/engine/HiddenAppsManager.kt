package com.t9launcher.engine

import android.content.Context

object HiddenAppsManager {
    private const val PREFS = "hidden_apps"
    private const val KEY_HIDDEN = "hidden"

    fun getHidden(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_HIDDEN, "") ?: ""
        return if (raw.isEmpty()) emptySet() else raw.split(",").toSet()
    }

    fun setHidden(context: Context, packages: Set<String>) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_HIDDEN, packages.joinToString(",")).apply()
    }

    fun toggle(context: Context, packageName: String) {
        val current = getHidden(context).toMutableSet()
        if (current.contains(packageName)) current.remove(packageName) else current.add(packageName)
        setHidden(context, current)
    }

    fun isHidden(context: Context, packageName: String): Boolean {
        return getHidden(context).contains(packageName)
    }
}
