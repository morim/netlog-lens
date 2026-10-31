package com.morim.netloglens.core

/** Cisco IOS / IOS-XE（Catalyst、ISRなど） */
object CiscoIosProfile : DeviceProfile {
    override val id = "cisco_ios"
    override val displayName = "Cisco IOS / IOS-XE"
    override val expertise = "Cisco IOS / IOS-XE（Catalyst・ISR・ASR）"

    override val promptHints = listOf(
        "syslogは「%FACILITY-SEVERITY-MNEMONIC: メッセージ」形式です。SEVERITYの数字（0=emergencies〜7=debugging）も判定に使ってください。",
        "インターフェース名は Gi=GigabitEthernet、Te=TenGigabitEthernet、Fa=FastEthernet、Po=Port-channel、Vl=Vlan の略です。",
        "show interfaces のエラーカウンタ（CRC、input errors、output drops など）が出ていれば、物理層・デュプレックス・輻輳のどれが疑わしいかも述べてください。",
    )

    private const val OPT_TYPE = """(?:\s+\d{1,2})?""" // "secret 5 xxx" の 5 など

    override val secretPatterns = listOf(
        // enable secret / enable password
        Regex("""(?im)^(\s*enable\s+(?:secret|password)$OPT_TYPE\s+)(\S+)"""),
        // username X ... secret/password
        Regex("""(?im)^(\s*username\s+\S+.*?\b(?:secret|password)$OPT_TYPE\s+)(\S+)"""),
        // line vty の password など、行頭の password / secret
        Regex("""(?im)^(\s*(?:password|secret)$OPT_TYPE\s+)(\S+)"""),
        // snmp-server community / host ... community
        Regex("""(?im)^(\s*snmp-server\s+community\s+)(\S+)"""),
        Regex("""(?im)^(\s*snmp-server\s+host\s+\S+(?:\s+(?:informs|traps|version\s+\S+))*\s+)(\S+)"""),
        // tacacs / radius / isakmp / 行頭の key
        Regex("""(?im)^(\s*(?:tacacs-server\s+|radius-server\s+|crypto\s+isakmp\s+)?key$OPT_TYPE\s+)(\S+)"""),
        Regex("""(?im)(\bkey-string$OPT_TYPE\s+)(\S+)"""),
        Regex("""(?im)(\bpre-shared-key(?:\s+(?:local|remote))?$OPT_TYPE\s+)(\S+)"""),
        Regex("""(?im)(\b(?:authentication-key|message-digest-key\s+\d+\s+md5|ntp\s+authentication-key\s+\d+\s+md5)$OPT_TYPE\s+)(\S+)"""),
        Regex("""(?im)(\bppp\s+(?:chap|pap\s+sent-username\s+\S+)\s+password$OPT_TYPE\s+)(\S+)"""),
        Regex("""(?im)(\bstandby\s+\d+\s+authentication(?:\s+md5\s+key-string|\s+text)?$OPT_TYPE\s+)(\S+)"""),
    )

    override val hostnamePatterns = listOf(
        Regex("""(?im)^\s*hostname\s+(\S+)"""),
        // プロンプト: Router#, SW01(config-if)#, R1>
        Regex("""(?m)^([A-Za-z][\w.-]{0,62})(?:\([\w-]+\))?[#>]"""),
        // CDP / LLDP
        Regex("""(?im)^\s*Device ID\s*:\s*(\S+)"""),
        Regex("""(?im)^\s*System Name\s*:\s*(\S+)"""),
        Regex("""(?im)^\s*ip\s+domain[- ]name\s+(\S+)"""),
    )

    override val usernamePatterns = listOf(
        Regex("""(?im)^\s*username\s+(\S+)"""),
        Regex("""(?i)\bConfigured from \S+ by (\S+)"""),
        Regex("""(?i)\[user:\s*([^\]\s]+)\s*\]"""),
        Regex("""(?im)^\s*(?:User(?:name)?)\s*:\s*(\S+)"""),
    )

    override val serialPatterns = listOf(
        Regex("""(?i)\bProcessor board ID\s+(\S+)"""),
        Regex("""(?i)\bSystem serial number\s*:\s*(\S+)"""),
        Regex("""(?i)\bSN:\s*([A-Za-z0-9]{6,})"""),
        Regex("""(?i)\bSerial Number\s*:\s*(\S+)"""),
    )

    override val blockPatterns = listOf(
        // 証明書の16進ダンプ行（8桁16進が4個以上並ぶ行）
        Regex("""(?m)^[ \t]*(?:[0-9A-Fa-f]{8}[ \t]+){3,}[0-9A-Fa-f]{2,8}[ \t]*$""") to "  <CERT_DATA>",
    )
}
