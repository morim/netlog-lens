package com.morim.netloglens.core

/**
 * 機種に依存しない伏せ字のルール（共通の辞書）。
 *
 * 「キーワード＋値」（password xxx / secret: xxx / key=xxx / "token": "xxx"）や、
 * プロンプト（R1# / user@host:~$）など、どの機種にもありがちな書式だけで見つける。
 * 機種を選んでも選ばなくても、保護モードでは必ずこのルールが使われる。
 * 機種プロファイルの正規表現は、ここで拾えない書式を足すための「おまけ」。
 *
 * 正規表現の決まり：
 * - secretPatterns：グループ1=前置き（残す）、グループ2=値（<SECRET> にする）
 * - それ以外：グループ1=値
 * - 改行をまたがないよう、空白は基本的に [ \t] を使う
 */
object CommonMaskRules {
    /** 値の直前に入りがちな「種類」の語（password 7 xxx、set password ENC xxx、community read cipher xxx など） */
    private const val MODS =
        """(?:[ \t]+(?:\d{1,2}|md5|sha\d*|text|plain(?:text)?|irreversible-cipher|cipher|simple|encrypted|hidden|ENC|read|write|ro|rw|read-only|read-write|local|remote))"""

    /** 値：引用符で囲まれた文字列、またはスペースまでの文字列（伏せ字済みの <…> は除く） */
    private const val VALUE = """("[^"\n]*"|'[^'\n]*'|[^\s<"']\S*)"""

    /** パスワード・鍵を表すキーワード。前に英字が無いこと（encrypted-password、db_password は拾う） */
    private const val SECRET_WORDS =
        """(?:password|passwd|passphrase|secret|community|pre-shared-key|psk|api[_-]?key|access[_-]?key|private[_-]?key|token|""" +
            """key-string|authentication-key|auth-key|message-digest-key|shared-key|shared-secret|credential)"""

    val secretPatterns: List<Regex> = listOf(
        // 汎用：キーワード＋値（password 7 xxx / secret: xxx / key=xxx / "password": "xxx"）
        Regex(
            """(?im)((?<![A-Za-z])$SECRET_WORDS["']?(?:[ \t]*[:=][ \t]*|$MODS{0,3}[ \t]+))$VALUE""",
        ),
        // 行頭の key、tacacs/radius/isakmp/server の key（key chain 名などは STOP_WORDS で除外）
        Regex(
            """(?im)^([ \t]*(?:(?:set[ \t]+)?(?:tacacs|radius)(?:-server)?[ \t]+(?:host[ \t]+\S+[ \t]+)?|crypto[ \t]+isakmp[ \t]+|server[ \t]+\S+[ \t]+)?key(?:[ \t]+\d{1,2})?[ \t]+)$VALUE""",
        ),
        // snmp-server host <宛先> [version 2c] <community>
        Regex("""(?im)^([ \t]*snmp-server[ \t]+host[ \t]+\S+(?:[ \t]+(?:informs|traps|version[ \t]+\S+))*[ \t]+)(\S+)"""),
        // HSRP/VRRP/GLBP の認証文字列
        Regex("""(?im)(\b(?:standby|vrrp|glbp)[ \t]+\d+[ \t]+authentication(?:[ \t]+text)?[ \t]+)(\S+)"""),
        // XML：<password>xxx</password>（小文字のみ。伏せ字済みの <SECRET> を拾わないように）
        Regex("""(<(?:password|secret|key|community|psk)>)([^<\n]+)(?=</)"""),
        // YAMAHA RTX など：login user <名前> <パスワード>
        Regex("""(?im)^([ \t]*login[ \t]+user[ \t]+\S+[ \t]+)(\S+)"""),
        // SSH公開鍵：ssh-rsa AAAA...
        Regex("""((?:ssh-(?:rsa|ed25519|dss)|ecdsa-sha2-\S+)[ \t]+)(AAAA[0-9A-Za-z+/=]+)"""),
    )

    /**
     * キーワードの後ろに来ても「値」ではない語。
     * 例：「password changed」「Password for root」「key chain KC1」「secret is required」
     */
    val STOP_WORDS: Set<String> = setOf(
        "changed", "for", "is", "was", "of", "to", "the", "required", "expired", "prompt", "incorrect", "invalid",
        "authentication", "encryption", "enable", "enabled", "disable", "disabled", "none", "policy", "recovery",
        "aging", "length", "minimum", "complexity", "mismatch", "failed", "accepted", "change", "reset", "chain",
        "generate", "bucket", "config-key", "zeroize", "set", "unset", "no", "md5", "sha", "text", "string", "type",
    )

    val hostnamePatterns: List<Regex> = listOf(
        // hostname R1 / sysname R1（Huawei等）/ host-name R1;（Juniper）/ set hostname "FW-01"（FortiGate）
        Regex("""(?im)^[ \t]*(?:set[ \t]+)?(?:hostname|host-name|sysname|system-name)[ \t]+["']?([\w.-]+)"""),
        // プロンプト：Router# / SW01(config-if)# / R1> / FW-01 # get
        Regex("""(?m)^([A-Za-z][\w.-]{0,62})(?:\([\w/.-]+\))?(?:[#>]|[ \t][#>](?=[ \t]|$))"""),
        // プロンプト：<HUAWEI> / [HUAWEI]（XMLの <tag>…</tag> と Juniper の [edit] は除く）
        Regex("""(?m)^<([A-Za-z][\w.-]{0,62})>(?![^\n]*</)"""),
        Regex("""(?m)^\[~?\*?(?!edit])([A-Za-z][\w.-]{0,62})]"""),
        // user@host:~$ / admin@SRX1> / root@host#
        Regex("""(?m)^[\w.-]+@([\w.-]+)[:~ >#$]"""),
        // CDP / LLDP の隣接機器
        Regex("""(?im)^[ \t]*Device[ \t]*ID[ \t]*:[ \t]*(\S+)"""),
        Regex("""(?im)^[ \t]*System[ \t]+Name[ \t]*:[ \t]*(\S+)"""),
        // ドメイン名
        Regex("""(?im)^[ \t]*(?:set[ \t]+|ip[ \t]+)?domain[- ]name[ \t]+["']?([\w.-]+)"""),
    )

    val usernamePatterns: List<Regex> = listOf(
        // 設定の行頭：username admin / user admin / local-user admin / login user admin（小文字のみ。表の見出し「User」は拾わない）
        Regex("""(?m)^[ \t]*(?:set[ \t]+)?(?:username|user|local-user|login[ \t]+user)[ \t]+["']?([\w.@-]+)"""),
        // user@host
        Regex("""(?m)^([\w.-]+)@[\w.-]+[:~ >#$]"""),
        // User: admin / Username: admin / Login: admin
        Regex("""(?im)^[ \t]*(?:User(?:[ \t]*name)?|Login)[ \t]*:[ \t]*(\S+)"""),
        // syslog：Configured from console by admin / [user: admin] / for user admin / password for root from
        Regex("""(?i)\bConfigured[ \t]+from[ \t]+\S+[ \t]+by[ \t]+(\S+)"""),
        Regex("""(?i)\[user:[ \t]*([^\]\s]+)[ \t]*]"""),
        Regex("""(?i)\b(?:for|by)[ \t]+(?:invalid[ \t]+)?user[ \t]+["']?([\w.@-]+)"""),
        Regex("""(?i)\bpassword[ \t]+for[ \t]+(?:invalid[ \t]+user[ \t]+)?([\w.@-]+)[ \t]+from\b"""),
    )

    val serialPatterns: List<Regex> = listOf(
        Regex("""(?i)\bProcessor[ \t]+board[ \t]+ID[ \t]+(\S+)"""),
        Regex("""(?i)\bSystem[ \t]+serial[ \t]+number[ \t]*:[ \t]*(\S+)"""),
        // Serial Number: / Serial-Number: / Serial No. = / S/N: / SN: / ESN: / BarCode=
        Regex("""(?i)\b(?:Serial(?:[ \t_-]*(?:Number|No\.?|Num))?|S/N|SN|ESN|BarCode)[ \t]*[:=][ \t]*([A-Za-z0-9][\w-]{3,})"""),
    )

    /** 複数行・行全体を置き換えるもの */
    val blockPatterns: List<Pair<Regex, String>> = listOf(
        // PEM形式の証明書・秘密鍵
        Regex("""(?s)-----BEGIN [A-Z0-9 ]+-----.*?-----END [A-Z0-9 ]+-----""") to "<CERT_DATA>",
        // 証明書の16進ダンプ行（8桁16進が4個以上並ぶ行。Cisco の crypto pki certificate など）
        Regex("""(?m)^[ \t]*(?:[0-9A-Fa-f]{8}[ \t]+){3,}[0-9A-Fa-f]{2,8}[ \t]*$""") to "  <CERT_DATA>",
    )
}
