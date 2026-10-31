package com.morim.netloglens.core

import org.junit.Assert.assertEquals
import org.junit.Test

class AiTargetTest {
    @Test
    fun defaultIsClaude() {
        assertEquals(AiTarget.CLAUDE, AiTarget.DEFAULT)
        assertEquals(AiTarget.CLAUDE, AiTarget.fromName(null))
        assertEquals(AiTarget.CLAUDE, AiTarget.fromName("UNKNOWN"))
    }

    @Test
    fun restoresSavedChoice() {
        AiTarget.entries.forEach { assertEquals(it, AiTarget.fromName(it.name)) }
    }

    @Test
    fun packageNamesAreDistinct() {
        assertEquals(AiTarget.entries.size, AiTarget.entries.map { it.packageName }.toSet().size)
    }
}
