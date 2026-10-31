package com.morim.netloglens.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MaskerTest {
    // 機種に依存しないことを確かめるため、Ciscoのログも「指定なし」で伏せる
    private fun mask(s: String, cats: Set<MaskCategory> = MaskCategory.entries.toSet()) =
        Masker(GenericProfile, cats).mask(s)

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
    fun generalModeMasksNothing() {
        // 汎用モード＝伏せ字カテゴリが空
        val log = "hostname R1\nenable secret 5 abc\nip address 10.1.1.1 255.255.255.0"
        val r = Masker(CiscoIosProfile, emptySet()).mask(log)
        assertEquals(log, r.text)
        assertTrue(r.mapping.isEmpty())
        val prompt = PromptBuilder.build(GenericProfile, Purpose.TROUBLE, "", r.text, masked = false)
        assertFalse(prompt.contains("伏せ字"))
        assertTrue(prompt.contains("10.1.1.1"))
        assertTrue(PromptBuilder.build(CiscoIosProfile, Purpose.TROUBLE, "", "x", masked = true).contains("伏せ字"))
    }

    @Test
    fun genericProfileProtectedMode() {
        val log = """
            admin@web01:~${'$'} cat /etc/app.conf
            db_password = S3cret!
            Password:
            next line stays
            set hostname FW-01
            FW-01 # get system status
            Serial-Number: FGT60F1234567890
            server 172.16.5.10
        """.trimIndent()
        val t = Masker(GenericProfile, MaskCategory.entries.toSet()).mask(log).text
        assertFalse(t, t.contains("S3cret"))
        assertFalse(t, t.contains("web01"))
        assertFalse(t, t.contains("admin@"))
        assertFalse(t, t.contains("FW-01"))
        assertFalse(t, t.contains("FGT60F1234567890"))
        assertFalse(t, t.contains("172.16.5.10"))
        assertTrue(t, t.contains("next line stays")) // 「Password:」の次の行は巻き込まない
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

    @Test
    fun sameResultWithOrWithoutDeviceProfile() {
        val log = """
            hostname CORE-SW01
            enable secret 9 ${'$'}9${'$'}abcdEFGH${'$'}xyz
            username admin privilege 15 secret 5 ${'$'}1${'$'}mERr${'$'}hx5rVt7rPNoS4wqbXKX7m0
            snmp-server community MyComm RO
            interface GigabitEthernet0/1
             ip address 10.1.2.3 255.255.255.0
             standby 1 authentication Hsrp1
            Processor board ID FOC1234X5YZ
            CORE-SW01#show version
        """.trimIndent()
        val all = MaskCategory.entries.toSet()
        val generic = Masker(GenericProfile, all).mask(log)
        val cisco = Masker(CiscoIosProfile, all).mask(log)
        assertEquals(generic.text, cisco.text)
        assertEquals(generic.mapping, cisco.mapping)
        assertFalse(generic.text, generic.text.contains("Hsrp1"))
    }

    @Test
    fun otherVendorsWithoutProfile() {
        val log = """
            [edit]
            admin@SRX1# show system
            host-name SRX1;
            root-authentication {
                encrypted-password "${'$'}6${'$'}abc${'$'}DEFghi"; ## SECRET-DATA
            }
            secret "${'$'}9${'$'}xYz123"; ## SECRET-DATA
            <HUAWEI-CORE>display current-configuration
            sysname HUAWEI-CORE
             snmp-agent community read cipher %^%#AbCdEf%^%#
             local-user netops password irreversible-cipher ${'$'}1a${'$'}Qw${'$'}
            config system admin
                set password ENC SH2Abcdef0123
            login user yamaha-op Yp4ss
            pre-shared-key text Ipsec!Key
            ESN: 2102351931P0ABCD1234
            ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAAAgQC7 user@pc
        """.trimIndent()
        val t = mask(log).text
        listOf(
            "SRX1", "abc", "xYz123", "HUAWEI-CORE", "AbCdEf", "netops", "Qw", "SH2Abcdef0123",
            "yamaha-op", "Yp4ss", "Ipsec!Key", "2102351931P0ABCD1234", "AAAAB3NzaC1yc2E",
        ).forEach { assertFalse("$it が残っている:\n$t", t.contains(it)) }
        assertTrue(t, t.contains("[edit]"))
    }

    @Test
    fun serverLogsAndConfigFiles() {
        val log = """
            Oct 31 10:00:01 web01 sshd[123]: Failed password for invalid user oracle from 10.0.0.5 port 2222 ssh2
            Oct 31 10:00:02 web01 sshd[124]: Accepted password for deploy from 10.0.0.6 port 2223 ssh2
            {"db_user": "app", "password": "p@ss w0rd", "api_key": "sk-123456"}
            <password>XmlPass1</password>
            -----BEGIN RSA PRIVATE KEY-----
            MIIEowIBAAKCAQEA1234
            abcdEFGH5678
            -----END RSA PRIVATE KEY-----
        """.trimIndent()
        val r = mask(log)
        val t = r.text
        listOf("oracle", "deploy", "p@ss w0rd", "sk-123456", "XmlPass1", "MIIEowIBAAKCAQEA1234", "abcdEFGH5678")
            .forEach { assertFalse("$it が残っている:\n$t", t.contains(it)) }
        assertTrue(t, t.contains("<CERT_DATA>"))
        assertTrue(t, t.contains("Failed password for invalid user"))
        assertTrue(t, t.contains("Accepted password for"))
    }

    @Test
    fun wordsAfterKeywordsThatAreNotSecrets() {
        val log = """
            service password-encryption
            key chain KC1
             key 1
              key-string 7 070C285F4D06
            %SYS-5-PRIV_AUTH_PASS: Password changed for user admin
            User      Interface   Idle
            Password:
            next line stays
        """.trimIndent()
        val r = mask(log)
        val t = r.text
        assertTrue(t, t.contains("service password-encryption"))
        assertTrue(t, t.contains("key chain KC1"))
        assertTrue(t, t.contains(" key 1"))
        assertTrue(t, t.contains("Password changed for user"))
        assertTrue(t, t.contains("User      Interface   Idle"))
        assertTrue(t, t.contains("next line stays"))
        assertFalse(t, t.contains("070C285F4D06"))
        assertFalse(t, t.contains("admin"))
        assertEquals(1, r.secretCount)
    }
}
