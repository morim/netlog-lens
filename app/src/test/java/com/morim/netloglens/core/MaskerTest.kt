package com.morim.netloglens.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MaskerTest {
    private fun mask(s: String, cats: Set<MaskCategory> = MaskCategory.entries.toSet()) =
        Masker(CiscoIosProfile, cats).mask(s)

    @Test
    fun ipv4_sameValueSameToken_masksKept() {
        val r = mask(
            """
            ip address 10.1.2.3 255.255.255.0
            ip route 0.0.0.0 0.0.0.0 10.1.2.254
            access-list 10 permit 192.168.0.0 0.0.0.255
            ping 10.1.2.3
            """.trimIndent(),
        )
        assertTrue(r.text.contains("ip address <IP_1> 255.255.255.0"))
        assertTrue(r.text.contains("ip route 0.0.0.0 0.0.0.0 <IP_2>"))
        assertTrue(r.text.contains("permit <IP_3> 0.0.0.255"))
        assertTrue(r.text.contains("ping <IP_1>"))
        assertEquals("10.1.2.3", r.mapping["<IP_1>"])
    }

    @Test
    fun timestampsAndVersionsAreNotIpv6() {
        val log = "*Mar  1 00:01:02.123: %LINK-3-UPDOWN: Interface GigabitEthernet0/1, changed state to up"
        val r = mask(log)
        assertTrue(r.text.contains("00:01:02.123"))
        assertTrue(r.text.contains("%LINK-3-UPDOWN:"))
        assertTrue(r.text.contains("GigabitEthernet0/1"))
    }

    @Test
    fun ipv6AndMac() {
        val r = mask(
            """
            ipv6 address 2001:DB8:10::1/64
            ipv6 address FE80::1 link-local
            Hardware is iGbE, address is 0011.2233.4455 (bia 0011.2233.4455)
            aa:bb:cc:dd:ee:ff
            """.trimIndent(),
        )
        assertTrue(r.text, r.text.contains("ipv6 address <IPV6_1>/64"))
        assertTrue(r.text.contains("ipv6 address <IPV6_2> link-local"))
        assertTrue(r.text.contains("address is <MAC_1> (bia <MAC_1>)"))
        assertTrue(r.text.contains("<MAC_2>"))
    }

    @Test
    fun secrets() {
        val r = mask(
            """
            enable secret 9 ${'$'}9${'$'}abcdEFGH${'$'}xyz
            username admin privilege 15 secret 5 ${'$'}1${'$'}mERr${'$'}hx5rVt7rPNoS4wqbXKX7m0
            snmp-server community MyComm RO
            snmp-server host 10.0.0.9 version 2c TrapComm
            line vty 0 4
             password 7 0822455D0A16
            tacacs-server key 7 1234ABCD
             key-string 7 070C285F4D06
            crypto isakmp key Sup3rKey address 0.0.0.0
            """.trimIndent(),
        )
        val t = r.text
        assertFalse(t, t.contains("abcdEFGH"))
        assertFalse(t, t.contains("mERr"))
        assertFalse(t, t.contains("MyComm"))
        assertFalse(t, t.contains("TrapComm"))
        assertFalse(t, t.contains("0822455D0A16"))
        assertFalse(t, t.contains("1234ABCD"))
        assertFalse(t, t.contains("070C285F4D06"))
        assertFalse(t, t.contains("Sup3rKey"))
        assertTrue(t, t.contains("snmp-server community <SECRET> RO"))
        assertTrue(t, t.contains("enable secret 9 <SECRET>"))
        assertEquals(8, r.secretCount)
        assertFalse(r.mapping.values.any { it.contains("MyComm") })
    }

    @Test
    fun hostnamesUsersSerials() {
        val r = mask(
            """
            hostname CORE-SW01
            CORE-SW01#show version
            Processor board ID FOC1234X5YZ
            NAME: "1", DESCR: "WS-C2960X-48TS-L"
            PID: WS-C2960X-48TS-L  , VID: V05  , SN: FOC1234X5YZ
            %SYS-5-CONFIG_I: Configured from console by netadmin on vty0 (10.9.9.9)
            Device ID: DIST-RT01.example.local
            CORE-SW01(config-if)#
            """.trimIndent(),
        )
        val t = r.text
        assertFalse(t, t.contains("CORE-SW01"))
        assertFalse(t, t.contains("FOC1234X5YZ"))
        assertFalse(t, t.contains("netadmin"))
        assertFalse(t, t.contains("DIST-RT01"))
        assertTrue(t, t.contains("<HOST_1>#show version") || t.contains("#show version"))
        assertTrue(t, t.contains("WS-C2960X-48TS-L")) // 型番は残す
        assertTrue(t, t.contains("Processor board ID <SN_1>"))
    }

    @Test
    fun disabledCategoriesAreKept() {
        val r = mask("hostname R1\nip address 10.1.1.1 255.255.255.0", setOf(MaskCategory.SECRET))
        assertTrue(r.text.contains("hostname R1"))
        assertTrue(r.text.contains("10.1.1.1"))
    }

    @Test
    fun unmaskRestoresAnswer() {
        val r = mask("R1#ping 10.1.1.1\nhostname R1")
        val answer = r.text.lines().first() + " が失敗しています"
        val restored = Masker.unmask(answer, r.mapping)
        assertEquals("R1#ping 10.1.1.1 が失敗しています", restored)
    }

    @Test
    fun genericModeMasksNothing() {
        val log = "hostname R1\nenable secret 5 abc\nip address 10.1.1.1 255.255.255.0"
        val r = Masker(GenericProfile, MaskCategory.entries.toSet()).mask(log)
        assertEquals(log, r.text)
        assertTrue(r.mapping.isEmpty())
        val prompt = PromptBuilder.build(GenericProfile, Purpose.TROUBLE, "", r.text)
        assertFalse(prompt.contains("伏せ字"))
        assertTrue(prompt.contains("10.1.1.1"))
        assertTrue(PromptBuilder.build(CiscoIosProfile, Purpose.TROUBLE, "", "x").contains("伏せ字"))
    }

    @Test
    fun certificateHexIsMasked() {
        val r = mask(
            """
            crypto pki certificate chain TP-self-signed-1
             certificate self-signed 01
              3082022B 30820194 A0030201 02020101 300D0609 2A864886 F70D0101 05050030
              31312F30 2D060355 04031326 494F532D 53656C66 2D536967 6E65642D 43657274
                quit
            """.trimIndent(),
        )
        assertFalse(r.text.contains("3082022B"))
        assertTrue(r.text.contains("<CERT_DATA>"))
    }
}
