package com.morim.netloglens.core

import kotlin.math.abs
import kotlin.math.roundToInt

/** OCRで見つかった1つの文字列と、その画像上の位置 */
data class OcrLine(val text: String, val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val centerY get() = (top + bottom) / 2.0
    val height get() = (bottom - top).coerceAtLeast(1)
    val charWidth get() = (right - left).toDouble() / text.length.coerceAtLeast(1)
}

/**
 * OCRは表（show ip interface brief など）の列を別々の塊として返すことがある。
 * 縦位置が同じものを1行にまとめ、横の間隔に応じて空白を入れて、画面の見た目に近い文字列に戻す。
 */
object RowAssembler {
    fun assemble(lines: List<OcrLine>): String {
        if (lines.isEmpty()) return ""
        val medianH = lines.map { it.height }.sorted()[lines.size / 2]
        val rows = mutableListOf<MutableList<OcrLine>>()
        for (l in lines.sortedBy { it.centerY }) {
            val row = rows.lastOrNull()
            if (row != null && abs(l.centerY - row.map { it.centerY }.average()) < medianH * 0.5) row += l
            else rows += mutableListOf(l)
        }
        val minLeft = lines.minOf { it.left }
        val charW = lines.map { it.charWidth }.sorted()[lines.size / 2].coerceAtLeast(1.0)
        return rows.joinToString("\n") { row ->
            val sb = StringBuilder()
            var cursor = minLeft.toDouble()
            row.sortedBy { it.left }.forEachIndexed { i, l ->
                val gap = ((l.left - cursor) / charW).roundToInt()
                val spaces = if (i == 0) gap.coerceIn(0, 8) else gap.coerceIn(1, 40)
                repeat(spaces) { sb.append(' ') }
                sb.append(l.text)
                cursor = maxOf(cursor + (spaces + l.text.length) * charW, l.right.toDouble())
            }
            sb.toString().trimEnd()
        }
    }
}
