package com.t9launcher.engine

import com.t9launcher.model.AppInfo

object HiddenAppsFilter {
    fun filterAndSort(
        apps: List<AppInfo>,
        search: String,
        isSystem: Boolean,
        hidden: Set<String>,
        sortMode: Int
    ): List<AppInfo> {
        return apps.filter { app ->
            val matchSearch = app.label.contains(search, ignoreCase = true) ||
                    app.packageName.contains(search, ignoreCase = true)
            val matchesTab = app.isSystem == isSystem
            matchSearch && matchesTab
        }.let { list ->
            when (sortMode) {
                1 -> list.sortedWith(compareBy<AppInfo> { hidden.contains(it.packageName) }.thenBy { it.label.lowercase() })
                2 -> list.sortedWith(compareByDescending<AppInfo> { hidden.contains(it.packageName) }.thenBy { it.label.lowercase() })
                else -> list.sortedBy { it.label.lowercase() }
            }
        }
    }
}
