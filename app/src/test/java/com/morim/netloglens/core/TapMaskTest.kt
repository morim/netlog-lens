package com.morim.netloglens.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TapMaskTest {
    private val all = MaskCategory.entries.toSet()

    @Test
    fun userWordsAreMaskedEverywhere() {
        val log = """
            interface Gi0/1
             description HQ-3F-Rack2 to TOKYO-DC
            interface Gi0/2
             description HQ-3F-Rack2 uplink
        """.trimIndent()
        val r = Masker(GenericProfile, all, userWords = setOf("HQ-3F-Rack2", "TOKYO-DC")).mask(log)
        assertFalse(r.text, r.text.contains("HQ-3F-Rack2"))
        assertFalse(r.text, r.text.contains("TOKYO-DC"))
        assertEquals(2, Regex("<WORD_1>").findAll(r.text).count())
        assertEquals("HQ-3F-Rack2", r.mapping["<WORD_1>"])
        assertEquals(2, r.counts["WORD"])
    }

    @Test
    fun userWordsWorkEvenWhenAllCategoriesAreOff() {
        val r = Masker(GenericProfile, emptySet(), userWords = setOf("acme")).mask("ip domain-name acme.local\nacme")
        // 社名などはドメイン名の一部でも伏せる（ホスト名と同じ扱い）。英数字の途中（acmex）は伏せない
        assertEquals("ip domain-name <WORD_1>.local\n<WORD_1>", r.text)
        assertEquals("acmex", Masker(GenericProfile, emptySet(), userWords = setOf("acme")).mask("acmex").text)
    }

    @Test
    fun userWordCanBeAnIpAddress() {
        val r = Masker(GenericProfile, setOf(MaskCategory.SECRET), userWords = setOf("10.1.1.1")).mask("ping 10.1.1.1")
        assertEquals("ping <WORD_1>", r.text)
    }

    @Test
    fun keepValuesAreNotMasked() {
        val log = "hostname Router\nRouter#ping 10.1.1.1\nRouter#ping 10.2.2.2\naddress is 0011.2233.4455"
        val r = Masker(GenericProfile, all, keepValues = setOf("Router", "10.1.1.1", "0011.2233.4455")).mask(log)
        assertTrue(r.text, r.text.contains("hostname Router"))
        assertTrue(r.text, r.text.contains("Router#ping 10.1.1.1"))
        assertTrue(r.text, r.text.contains("Router#ping <IP_1>"))
        assertTrue(r.text, r.text.contains("0011.2233.4455"))
    }

    @Test
    fun keepValueWinsOverUserWord() {
        val r = Masker(GenericProfile, all, userWords = setOf("acme"), keepValues = setOf("acme")).mask("acme")
        assertEquals("acme", r.text)
    }

    @Test
    fun secretsCannotBeKept() {
        val r = Masker(GenericProfile, all, keepValues = setOf("cisco123")).mask("username admin password 0 cisco123")
        assertFalse(r.text, r.text.contains("cisco123"))
    }

    @Test
    fun tapOnToken() {
        val text = "<HOST_1>#ping <IP_1>"
        val t = TapTarget.at(text, 3) as TapTarget.Token
        assertEquals("<HOST_1>", t.token)
        assertTrue(t.restorable)
        assertEquals("<IP_1>", (TapTarget.at(text, text.length - 1) as TapTarget.Token).token)
        assertFalse((TapTarget.at("secret <SECRET>", 9) as TapTarget.Token).restorable)
    }

    @Test
    fun tapOnWord() {
        val text = "<HOST_1>#show cdp neighbors\n description \"HQ-3F-Rack2\", uplink."
        assertEquals("show", (TapTarget.at(text, 10) as TapTarget.Word).word)
        val i = text.indexOf("Rack2")
        assertEquals("HQ-3F-Rack2", (TapTarget.at(text, i) as TapTarget.Word).word)
        assertEquals("uplink", (TapTarget.at(text, text.length - 2) as TapTarget.Word).word)
        assertNull(TapTarget.at(text, text.indexOf(' ')))
        assertNull(TapTarget.at(text, 999))
    }
}
