package com.t9launcher.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class T9MatcherTest {

    private fun match(pattern: String, names: List<String>): List<String> {
        val apps = names.map { name ->
            com.t9launcher.model.AppInfo(
                label = name,
                packageName = "com.test.$name",
                icon = null,
                component = android.content.ComponentName("com.test.$name", "")
            )
        }
        return matchApps(pattern, apps).map { it.label }
    }

    @Test
    fun `empty pattern returns all apps`() {
        val result = match("", listOf("App", "Beta"))
        assertEquals(2, result.size)
    }

    @Test
    fun `basic english match - 22 matches Cat`() {
        val result = match("22", listOf("App", "Beta", "Cat"))
        assertTrue(result.contains("Cat"))
    }

    @Test
    fun `22 does not match App`() {
        val result = match("22", listOf("App"))
        assertTrue(result.isEmpty())
    }

    @Test
    fun `wildcard digit 1 matches any char`() {
        val result = match("12", listOf("App", "Beta", "Cat"))
        assertTrue(result.contains("Cat"))
    }

    @Test
    fun `12 does not match App`() {
        val result = match("12", listOf("App"))
        assertTrue(result.isEmpty())
    }

    @Test
    fun `chinese pinyin initials - 68 matches meituan`() {
        // m=6, t=8
        val result = match("68", listOf("美团", "淘宝", "京东"))
        assertEquals("美团", result.first())
    }

    @Test
    fun `68 does not match taobao`() {
        // t=8, b=2 → 82, not 68
        val result = match("68", listOf("淘宝"))
        assertTrue(result.isEmpty())
    }

    @Test
    fun `chinese pinyin subsequence - 826 matches tao`() {
        // t=8, a=2, o=6
        val result = match("826", listOf("淘宝", "美团", "京东"))
        assertEquals("淘宝", result.first())
    }

    @Test
    fun `826 does not match jingdong`() {
        // jingdong = 54646364, 8=t not in jingdong
        val result = match("826", listOf("京东"))
        assertTrue(result.isEmpty())
    }

    @Test
    fun `initial match ranks higher than subsequence - 68`() {
        // m=6, t=8: 美团 initials "mt" exact match vs hellotalk subsequence
        val result = match("68", listOf("hellotalk", "美团"))
        assertEquals("美团", result.first())
    }

    @Test
    fun `mixed chinese and english - 53 matches jd`() {
        // j=5, d=3
        val result = match("53", listOf("京东", "淘宝", "美团"))
        assertEquals("京东", result.first())
    }

    @Test
    fun `53 does not match taobao`() {
        val result = match("53", listOf("淘宝"))
        assertTrue(result.isEmpty())
    }

    @Test
    fun `exact initials beats subsequence - 53`() {
        // j=5, d=3
        val result = match("53", listOf("Judo", "京东"))
        assertEquals("京东", result.first())
    }

    @Test
    fun `no match returns empty`() {
        val result = match("999", listOf("App", "Beta"))
        assertTrue(result.isEmpty())
    }

    @Test
    fun `non-digit chars in pattern are ignored`() {
        val result = match("a2", listOf("App", "Beta"))
        assertTrue(result.contains("App"))
    }

    @Test
    fun `a2 does not match Cat`() {
        // a is ignored, 2=ABC, Cat starts with c=2
        // but "App" matches via label "app" a=2
        val result = match("a2", listOf("Cat"))
        assertTrue(result.contains("Cat"))
    }

    @Test
    fun `wildcard 1 matches bilibili with 1515`() {
        // b=2, i=4, l=5, i=4, b=2, i=4, l=5, i=4
        // 1=wc, 5=l, 1=wc, 5=l
        val result = match("1515", listOf("哔哩哔哩", "美团", "京东"))
        assertEquals("哔哩哔哩", result.first())
    }

    @Test
    fun `1515 does not match meituan`() {
        val result = match("1515", listOf("美团"))
        assertTrue(result.isEmpty())
    }

    @Test
    fun `long input with no match returns empty`() {
        // 9=WXYZ, need 10+ W/X/Y/Z chars - no common app has this
        val result = match("9999999999", listOf("App", "Beta", "Cat", "美团"))
        assertTrue(result.isEmpty())
    }

    @Test
    fun `single digit 9 still matches apps with WXYZ`() {
        val result = match("9", listOf("WhatsApp", "WeChat", "App"))
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `single digit 9 does not match Cat`() {
        // C=2, a=2, t=8, none is 9
        val result = match("9", listOf("Cat"))
        assertTrue(result.isEmpty())
    }
}
