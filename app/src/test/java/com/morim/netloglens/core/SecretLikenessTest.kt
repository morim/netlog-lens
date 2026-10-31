package com.morim.netloglens.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecretLikenessTest {
    @Test
    fun definiteSecretsAreMaskedWithoutKeywords() {
        val log = """
            root:${'$'}6${'$'}saltsalt${'$'}AbCdEfGhIjKlMnOpQrStUv0123456789:19000:0:99999:7:::
            export AWS_ACCESS_KEY_ID AKIAIOSFODNN7EXAMPLE
            Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0In0.dozjgNryP4J3jVmNHl0w5N
            sha256 9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08
            blob "dGhpcyBpcyBhIHNlY3JldCBrZXkgZm9yIHRlc3RpbmcgMTIz+Ab="
        """.trimIndent()
        val r = Masker(GenericProfile, setOf(MaskCategory.SECRET)).mask(log)
        val t = r.text
        listOf("saltsalt", "AKIAIOSFODNN7EXAMPLE", "eyJhbGci", "9f86d081", "dGhpcyBp").forEach {
            assertFalse("$it が残っている:\n$t", t.contains(it))
        }
        assertTrue(t, t.startsWith("root:<SECRET>")) // 「root:」は残す
        assertTrue(t, t.contains("export AWS_ACCESS_KEY_ID <SECRET>"))
    }

    @Test
    fun definiteIsOffWhenSecretCategoryIsOff() {
        val log = "AKIAIOSFODNN7EXAMPLE"
        assertEquals(log, Masker(GenericProfile, setOf(MaskCategory.ADDRESS)).mask(log).text)
    }

    @Test
    fun normalNetworkWordsAreNotSecrets() {
        val words = listOf(
            "GigabitEthernet0/1", "TenGigabitEthernet1/0/1", "WS-C2960X-48TS-L", "FOC1234X5YZ",
            "c2960-lanbasek9-mz.150-2.SE11.bin", "aes256-ctr", "hmac-sha2-256", "%LINK-3-UPDOWN",
            "00:01:02.123", "2001:db8::1", "0011.2233.4455", "Catalyst9300", "administratively",
            "/flash/Images2023/Version17/ReleaseNotesFinal", "<HOST_1>", "cisco123",
        )
        words.forEach {
            assertFalse("$it は確実な秘密ではない", SecretLikeness.isDefinite(it))
            assertFalse("$it は要確認ではない", SecretLikeness.isSuspect(it))
        }
    }

    @Test
    fun randomLookingWordsAreSuspects() {
        listOf("Sup3rKey", "hx5rVt7rPNoS4wqbXKX7m0", "P@ssw0rd!", "Xk9#mQ2z").forEach {
            assertTrue("$it は要確認", SecretLikeness.isSuspect(it))
        }
    }

    @Test
    fun findSuspectsReturnsRanges() {
        val text = "<HOST_1>#show run\n tunnel key Zq8vLm3Np\n description uplink"
        val ranges = SecretLikeness.findSuspects(text)
        assertEquals(1, ranges.size)
        assertEquals("Zq8vLm3Np", text.substring(ranges[0]))
    }
}
