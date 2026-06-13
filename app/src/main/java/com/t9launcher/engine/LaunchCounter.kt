package com.t9launcher.engine

import android.content.Context

object LaunchCounter {
    private const val PREFS = "launch_counter"
    private const val KEY_COUNTS = "counts"

    fun increment(context: Context, packageName: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val counts = prefs.getString(KEY_COUNTS, "") ?: ""
        val map = parseCounts(counts)
        map[packageName] = (map[packageName] ?: 0) + 1
        prefs.edit().putString(KEY_COUNTS, serializeCounts(map)).apply()
    }

    fun getCount(context: Context, packageName: String): Int {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val counts = prefs.getString(KEY_COUNTS, "") ?: ""
        return parseCounts(counts)[packageName] ?: 0
    }

    fun getAll(context: Context): Map<String, Int> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val counts = prefs.getString(KEY_COUNTS, "") ?: ""
        return parseCounts(counts)
    }

    private fun parseCounts(raw: String): MutableMap<String, Int> {
        if (raw.isEmpty()) return mutableMapOf()
        return raw.split(";").mapNotNull {
            val parts = it.split(":")
            if (parts.size == 2) parts[0] to (parts[1].toIntOrNull() ?: 0) else null
        }.toMap().toMutableMap()
    }

    private fun serializeCounts(map: Map<String, Int>): String {
        return map.entries.joinToString(";") { "${it.key}:${it.value}" }
    }
}
