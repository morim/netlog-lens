package com.morim.netloglens.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RowAssemblerTest {
    @Test
    fun columnsOnSameRowAreJoinedInOrder() {
        // 1文字=10px。2列目が少しずれた高さで返ってきても同じ行になる
        val lines = listOf(
            OcrLine("up", 400, 102, 420, 122),
            OcrLine("Vlan10", 0, 100, 60, 120),
            OcrLine("10.1.1.1", 200, 101, 280, 121),
            OcrLine("Gi0/1", 0, 130, 50, 150),
            OcrLine("down", 400, 131, 440, 151),
        )
        val out = RowAssembler.assemble(lines).lines()
        assertEquals(2, out.size)
        assertTrue(out[0], Regex("""^Vlan10 +10\.1\.1\.1 +up$""").matches(out[0]))
        assertTrue(out[1], Regex("""^Gi0/1 +down$""").matches(out[1]))
    }

    @Test
    fun empty() = assertEquals("", RowAssembler.assemble(emptyList()))
}
