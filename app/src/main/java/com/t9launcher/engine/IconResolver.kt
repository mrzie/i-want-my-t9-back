package com.t9launcher.engine

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.LruCache

data class InstalledIconPack(
    val packageName: String,
    val label: String,
    val icon: Drawable?
)

object IconResolver {
    private val drawableCache = LruCache<String, Drawable?>(200)
    private var packsCache: List<InstalledIconPack>? = null

    fun resolve(context: Context, pkg: String, systemIcon: Drawable?): Drawable? {
        val packId = SettingsManager.getIconPackId(context) ?: return systemIcon
        val cacheKey = "$packId:$pkg"
        drawableCache.get(cacheKey)?.let { return it }
        val result = loadFromIconPack(context, packId, pkg) ?: systemIcon
        drawableCache.put(cacheKey, result)
        return result
    }

    fun getInstalledPacks(context: Context): List<InstalledIconPack> {
        packsCache?.let { return it }
        val pm = context.packageManager
        val packs = mutableListOf<InstalledIconPack>()

        val intents = listOf(
            "org.adw.launcher.THEMES",
            "com.novalauncher.THEME",
            "com.teslacoilsw.launcher.THEME"
        )
        val pkgNames = mutableSetOf<String>()

        for (action in intents) {
            val intent = Intent(action)
            for (ri in pm.queryIntentActivities(intent, PackageManager.GET_META_DATA)) {
                val pkgName = ri.activityInfo.packageName
                if (pkgNames.add(pkgName)) {
                    val appInfo = pm.getApplicationInfo(pkgName, 0)
                    packs.add(
                        InstalledIconPack(
                            packageName = pkgName,
                            label = appInfo.loadLabel(pm).toString(),
                            icon = appInfo.loadIcon(pm)
                        )
                    )
                }
            }
        }

        packsCache = packs
        return packs
    }

    fun clearCache() {
        drawableCache.evictAll()
        packsCache = null
    }

    private fun loadFromIconPack(context: Context, iconPackPkg: String, targetPkg: String): Drawable? {
        return try {
            val resources = context.packageManager.getResourcesForApplication(iconPackPkg)
            val drawableName = targetPkg.replace('.', '_')
            val resId = resources.getIdentifier(drawableName, "drawable", iconPackPkg)
            if (resId != 0) {
                val packContext = context.createPackageContext(iconPackPkg, 0)
                androidx.core.content.ContextCompat.getDrawable(packContext, resId)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}
