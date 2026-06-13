package com.t9launcher.engine

import android.content.ComponentName
import com.t9launcher.model.AppInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class HiddenAppsFilterTest {

    private fun app(label: String, packageName: String, isSystem: Boolean = false): AppInfo {
        return AppInfo(
            label = label,
            packageName = packageName,
            icon = null,
            component = ComponentName(packageName, ""),
            isSystem = isSystem
        )
    }

    @Test
    fun `alphabetical sort - apps sorted by label`() {
        val apps = listOf(
            app("Zebra", "com.zebra"),
            app("Apple", "com.apple"),
            app("Banana", "com.banana")
        )
        val result = HiddenAppsFilter.filterAndSort(
            apps = apps,
            search = "",
            isSystem = false,
            hidden = emptySet(),
            sortMode = 0
        )
        assertEquals("Apple", result[0].label)
        assertEquals("Banana", result[1].label)
        assertEquals("Zebra", result[2].label)
    }

    @Test
    fun `alphabetical sort - case insensitive`() {
        val apps = listOf(
            app("banana", "com.banana"),
            app("Apple", "com.apple"),
            app("CHERRY", "com.cherry")
        )
        val result = HiddenAppsFilter.filterAndSort(
            apps = apps,
            search = "",
            isSystem = false,
            hidden = emptySet(),
            sortMode = 0
        )
        assertEquals("Apple", result[0].label)
        assertEquals("banana", result[1].label)
        assertEquals("CHERRY", result[2].label)
    }

    @Test
    fun `visible first sort - hidden apps at bottom`() {
        val apps = listOf(
            app("Zebra", "com.zebra"),
            app("Apple", "com.apple"),
            app("Banana", "com.banana")
        )
        val hidden = setOf("com.apple")
        val result = HiddenAppsFilter.filterAndSort(
            apps = apps,
            search = "",
            isSystem = false,
            hidden = hidden,
            sortMode = 1
        )
        assertEquals("Banana", result[0].label)
        assertEquals("Zebra", result[1].label)
        assertEquals("Apple", result[2].label)
    }

    @Test
    fun `hidden first sort - visible apps at bottom`() {
        val apps = listOf(
            app("Zebra", "com.zebra"),
            app("Apple", "com.apple"),
            app("Banana", "com.banana")
        )
        val hidden = setOf("com.apple")
        val result = HiddenAppsFilter.filterAndSort(
            apps = apps,
            search = "",
            isSystem = false,
            hidden = hidden,
            sortMode = 2
        )
        assertEquals("Apple", result[0].label)
        assertEquals("Banana", result[1].label)
        assertEquals("Zebra", result[2].label)
    }

    @Test
    fun `filter by search - matches label`() {
        val apps = listOf(
            app("WeChat", "com.tencent.mm"),
            app("WhatsApp", "com.whatsapp"),
            app("Telegram", "org.telegram")
        )
        val result = HiddenAppsFilter.filterAndSort(
            apps = apps,
            search = "What",
            isSystem = false,
            hidden = emptySet(),
            sortMode = 0
        )
        assertEquals(1, result.size)
        assertEquals("WhatsApp", result[0].label)
    }

    @Test
    fun `filter by search - matches package name`() {
        val apps = listOf(
            app("WeChat", "com.tencent.mm"),
            app("WhatsApp", "com.whatsapp"),
            app("Telegram", "org.telegram")
        )
        val result = HiddenAppsFilter.filterAndSort(
            apps = apps,
            search = "tencent",
            isSystem = false,
            hidden = emptySet(),
            sortMode = 0
        )
        assertEquals(1, result.size)
        assertEquals("WeChat", result[0].label)
    }

    @Test
    fun `filter by tab - user apps only`() {
        val apps = listOf(
            app("User App", "com.user", isSystem = false),
            app("System App", "com.system", isSystem = true)
        )
        val result = HiddenAppsFilter.filterAndSort(
            apps = apps,
            search = "",
            isSystem = false,
            hidden = emptySet(),
            sortMode = 0
        )
        assertEquals(1, result.size)
        assertEquals("User App", result[0].label)
    }

    @Test
    fun `filter by tab - system apps only`() {
        val apps = listOf(
            app("User App", "com.user", isSystem = false),
            app("System App", "com.system", isSystem = true)
        )
        val result = HiddenAppsFilter.filterAndSort(
            apps = apps,
            search = "",
            isSystem = true,
            hidden = emptySet(),
            sortMode = 0
        )
        assertEquals(1, result.size)
        assertEquals("System App", result[0].label)
    }

    @Test
    fun `combined filter and sort`() {
        val apps = listOf(
            app("Zebra", "com.zebra", isSystem = false),
            app("Apple", "com.apple", isSystem = true),
            app("Banana", "com.banana", isSystem = false),
            app("Cherry", "com.cherry", isSystem = false)
        )
        val hidden = setOf("com.cherry")
        val result = HiddenAppsFilter.filterAndSort(
            apps = apps,
            search = "",
            isSystem = false,
            hidden = hidden,
            sortMode = 1
        )
        assertEquals(3, result.size)
        assertEquals("Banana", result[0].label)
        assertEquals("Zebra", result[1].label)
        assertEquals("Cherry", result[2].label)
    }

    @Test
    fun `empty result when no match`() {
        val apps = listOf(
            app("Apple", "com.apple"),
            app("Banana", "com.banana")
        )
        val result = HiddenAppsFilter.filterAndSort(
            apps = apps,
            search = "xyz",
            isSystem = false,
            hidden = emptySet(),
            sortMode = 0
        )
        assertEquals(0, result.size)
    }
}
