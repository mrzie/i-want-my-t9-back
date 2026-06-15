package com.t9launcher.engine

import net.sourceforge.pinyin4j.PinyinHelper
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType

private val CHAR_TO_T9 = intArrayOf(
    2, 2, 2, 3, 3, 3,
    4, 4, 4, 5, 5, 5,
    6, 6, 6, 7, 7, 7, 7,
    8, 8, 8, 9, 9, 9, 9
)

private val PINYIN_FORMAT = HanyuPinyinOutputFormat().apply {
    caseType = HanyuPinyinCaseType.LOWERCASE
    toneType = HanyuPinyinToneType.WITHOUT_TONE
    vCharType = HanyuPinyinVCharType.WITH_V
}

private fun charToT9(c: Char): Int {
    val lower = c.lowercaseChar()
    return if (lower in 'a'..'z') CHAR_TO_T9[lower - 'a'] else -1
}

private fun isChinese(c: Char): Boolean {
    return c.code in 0x4E00..0x9FFF || c.code in 0x3400..0x4DBF
}

private fun getInitial(c: Char): Char? {
    if (c in 'a'..'z' || c in 'A'..'Z') return c.lowercaseChar()
    if (isChinese(c)) {
        val arr = PinyinHelper.toHanyuPinyinStringArray(c, PINYIN_FORMAT)
        if (!arr.isNullOrEmpty()) return arr[0][0]
    }
    return null
}

private fun getFullPinyin(c: Char): String? {
    if (c in 'a'..'z' || c in 'A'..'Z') return c.lowercaseChar().toString()
    if (isChinese(c)) {
        val arr = PinyinHelper.toHanyuPinyinStringArray(c, PINYIN_FORMAT)
        if (!arr.isNullOrEmpty()) return arr[0]
    }
    return null
}

private fun buildInitials(name: String): String {
    val sb = StringBuilder()
    for (c in name) {
        getInitial(c)?.let { sb.append(it) }
    }
    return sb.toString()
}

private fun buildFullPinyin(name: String): String {
    val sb = StringBuilder()
    for (c in name) {
        getFullPinyin(c)?.let { sb.append(it) }
    }
    return sb.toString()
}

data class ScoredApp(
    val app: com.t9launcher.model.AppInfo,
    val score: Int
)

private const val SCORE_INITIAL_EXACT = 400
private const val SCORE_INITIAL_PREFIX = 350
private const val SCORE_INITIAL_SUBSEQ = 300
private const val SCORE_PINYIN_SUBSEQUENCE = 200
private const val SCORE_LABEL_SUBSEQUENCE = 100

fun matchApps(pattern: String, apps: List<com.t9launcher.model.AppInfo>): List<com.t9launcher.model.AppInfo> {
    if (pattern.isEmpty()) return apps.sortedWith(
        compareByDescending<com.t9launcher.model.AppInfo> { it.launchCount }
            .thenBy { it.labelLower }
    )
    val digits = pattern.map { it - '0' }.filter { it in 0..9 }
    if (digits.isEmpty()) return apps

    val scored = apps.map { app ->
        ScoredApp(app, scoreApp(digits, app))
    }.filter { it.score > 0 }
     .sortedWith(compareByDescending<ScoredApp> { it.score }
         .thenByDescending { it.app.launchCount }
         .thenBy { it.app.labelLower })

    return scored.map { it.app }
}

private fun scoreApp(digits: List<Int>, app: com.t9launcher.model.AppInfo): Int {
    val initials = buildInitials(app.label)
    val fullPinyin = buildFullPinyin(app.label)
    val labelLower = app.labelLower

    var bestScore = 0

    val prefix = digitsToPrefix(digits, initials)
    if (initials.length == digits.size && matchSubsequence(digits, initials)) {
        bestScore = maxOf(bestScore, SCORE_INITIAL_EXACT)
    } else if (prefix.isNotEmpty() && prefix.length == digits.size && initials.startsWith(prefix)) {
        bestScore = maxOf(bestScore, SCORE_INITIAL_PREFIX)
    } else if (matchSubsequence(digits, initials)) {
        bestScore = maxOf(bestScore, SCORE_INITIAL_SUBSEQ)
    }

    if (matchSubsequence(digits, fullPinyin)) {
        bestScore = maxOf(bestScore, SCORE_PINYIN_SUBSEQUENCE)
    }
    if (matchSubsequence(digits, labelLower)) {
        bestScore = maxOf(bestScore, SCORE_LABEL_SUBSEQUENCE)
    }

    return bestScore
}

private fun matchSubsequence(digits: List<Int>, text: String): Boolean {
    return findMatchPositions(digits, text).isNotEmpty()
}

fun findMatchPositions(digits: List<Int>, text: String): List<Int> {
    val positions = mutableListOf<Int>()
    var idx = 0
    for (digit in digits) {
        if (digit == 1) {
            if (idx < text.length) { positions.add(idx); idx++ }
            else return emptyList()
            continue
        }
        var found = false
        while (idx < text.length) {
            val t9 = charToT9(text[idx])
            idx++
            if (t9 == digit) {
                positions.add(idx - 1)
                found = true
                break
            }
        }
        if (!found) return emptyList()
    }
    return positions
}

fun findHighlightPositions(digits: List<Int>, label: String): List<Int> {
    val directPositions = findMatchPositions(digits, label.lowercase())
    if (directPositions.isNotEmpty()) return directPositions

    val initials = buildInitials(label)
    val initialPositions = findMatchPositions(digits, initials)
    if (initialPositions.isEmpty()) return emptyList()

    val labelPositions = mutableListOf<Int>()
    var initialIdx = 0
    for (i in label.indices) {
        if (getInitial(label[i]) != null) {
            if (initialIdx in initialPositions) {
                labelPositions.add(i)
            }
            initialIdx++
        }
    }
    return labelPositions
}

private fun digitsToPrefix(digits: List<Int>, text: String): String {
    val sb = StringBuilder()
    var idx = 0
    for (digit in digits) {
        if (digit == 1) {
            if (idx < text.length) { sb.append(text[idx]); idx++ }
            continue
        }
        while (idx < text.length) {
            val t9 = charToT9(text[idx])
            if (t9 == digit) { sb.append(text[idx]); idx++; break }
            idx++
        }
    }
    return sb.toString()
}
