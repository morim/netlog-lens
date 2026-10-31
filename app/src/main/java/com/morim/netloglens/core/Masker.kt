package com.morim.netloglens.core

/**
 * 伏せ字の結果。
 * @param text 伏せ字にした後の文字列（AIに送るもの）
 * @param mapping 記号 → 元の値。スマホの中にだけ保存し、AIの回答を元に戻すのに使う。
 *                パスワード類は復元不要なので入れない。
 */
data class MaskResult(
    val text: String,
    val mapping: Map<String, String>,
    val secretCount: Int,
) {
    val counts: Map<String, Int>
        get() = mapping.keys.groupingBy { it.substringBefore('_').removePrefix("<") }.eachCount() +
            (if (secretCount > 0) mapOf("SECRET" to secretCount) else emptyMap())
}

class Masker(
    private val profile: DeviceProfile,
    private val enabled: Set<MaskCategory>,
) {
    private val valueToToken = LinkedHashMap<String, String>()
    private val counters = HashMap<String, Int>()
    private var secretCount = 0

    private fun tokenFor(kind: String, value: String): String {
        val key = "$kind|$value"
        return valueToToken.getOrPut(key) {
            val n = (counters[kind] ?: 0) + 1
            counters[kind] = n
            "<${kind}_$n>"
        }
    }

    fun mask(input: String): MaskResult {
        var text = input.replace("\r\n", "\n")
        if (enabled.isEmpty()) return MaskResult(text, emptyMap(), 0)

        if (MaskCategory.SECRET in enabled) {
            for ((re, replacement) in CommonMaskRules.blockPatterns + profile.blockPatterns) {
                text = re.replace(text) { secretCount++; replacement }
            }
            for (re in CommonMaskRules.secretPatterns + profile.secretPatterns) {
                text = re.replace(text) { m ->
                    val value = m.groupValues[2]
                    if (isSecretValue(value)) {
                        secretCount++
                        m.groupValues[1] + "<SECRET>"
                    } else {
                        m.value
                    }
                }
            }
        }

        // 値を集めてから、同じ文字列をログ全体で置き換える
        val literals = mutableListOf<Pair<String, String>>() // kind, value
        if (MaskCategory.SERIAL in enabled) {
            (CommonMaskRules.serialPatterns + profile.serialPatterns).forEach { re ->
                re.findAll(text).forEach { literals += "SN" to it.groupValues[1].trimEnd(',', '.', ';') }
            }
        }
        if (MaskCategory.HOST_USER in enabled) {
            (CommonMaskRules.hostnamePatterns + profile.hostnamePatterns).forEach { re ->
                re.findAll(text).forEach { literals += "HOST" to it.groupValues[1].trimEnd(',', '.', ';') }
            }
            (CommonMaskRules.usernamePatterns + profile.usernamePatterns).forEach { re ->
                re.findAll(text).forEach { literals += "USER" to it.groupValues[1].trimEnd(',', '.', ';') }
            }
        }
        val seen = HashSet<String>()
        val unique = literals
            .map { (k, v) -> k to v.trim('"', '\'') }
            .filter { (_, v) -> v.length >= 2 && !v.startsWith("<") && !TOKEN_NAME_RE.matches(v) && !isIpv4(v) && seen.add(v) }
            // 長い値から置き換える（"SW01" より先に "SW01-CORE"）
            .sortedByDescending { it.second.length }
        for ((kind, value) in unique) {
            val token = tokenFor(kind, value)
            val re = Regex("""(?<![\w-])${Regex.escape(value)}(?![\w-])""")
            text = re.replace(text) { token }
        }

        if (MaskCategory.ADDRESS in enabled) {
            text = MAC_RE.replace(text) { tokenFor("MAC", it.value.lowercase()) }
            text = IPV6_CANDIDATE_RE.replace(text) { m ->
                if (isMaskableIpv6(m.value)) tokenFor("IPV6", m.value.lowercase()) else m.value
            }
            text = IPV4_RE.replace(text) { m ->
                if (isNetmaskOrWildcard(m.value)) m.value else tokenFor("IP", m.value)
            }
        }

        val mapping = valueToToken.entries.associate { (k, token) -> token to k.substringAfter('|') }
        return MaskResult(text, mapping, secretCount)
    }

    companion object {
        /** 伏せ字の記号の中身（<CERT_DATA> の CERT_DATA など）をホスト名と間違えないために使う */
        private val TOKEN_NAME_RE = Regex("""SECRET|CERT_DATA|(?:IP|IPV6|MAC|HOST|USER|SN)_\d+""")

        private const val OCTET = """(?:25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)"""
        val IPV4_RE = Regex("""(?<![\w.])$OCTET(?:\.$OCTET){3}(?![\w.]*\d)""")

        val MAC_RE = Regex(
            """(?<![\w.:-])(?:[0-9A-Fa-f]{4}\.[0-9A-Fa-f]{4}\.[0-9A-Fa-f]{4}""" +
                """|[0-9A-Fa-f]{2}(?::[0-9A-Fa-f]{2}){5}""" +
                """|[0-9A-Fa-f]{2}(?:-[0-9A-Fa-f]{2}){5})(?![\w.:-])""",
        )

        val IPV6_CANDIDATE_RE = Regex("""(?<![\w:.])[0-9A-Fa-f:]*:[0-9A-Fa-f:]*(?![\w:])""")

        /** 「password changed」の changed や、key chain の番号などは伏せない */
        private fun isSecretValue(raw: String): Boolean {
            val v = raw.trim('"', '\'')
            if (v.isEmpty() || v.startsWith("<")) return false // 空、または伏せ字済み
            if (v.length <= 2 && v.all { it.isDigit() }) return false
            return v.lowercase() !in CommonMaskRules.STOP_WORDS
        }

        fun isIpv4(s: String) = Regex("^$OCTET(?:\\.$OCTET){3}$").matches(s)

        /** 255.255.255.0 などのサブネットマスク、0.0.0.255 などのワイルドカード、0.0.0.0 は残す */
        fun isNetmaskOrWildcard(ip: String): Boolean {
            val parts = ip.split('.').map { it.toInt() }
            val bits = parts.fold(0L) { acc, p -> (acc shl 8) or p.toLong() }
            if (bits == 0L) return true
            val mask = bits.toString(2).padStart(32, '0')
            val isMask = !mask.contains("01")
            val isWildcard = !mask.contains("10")
            return (isMask && parts[0] == 255) || (isWildcard && parts[3] == 255)
        }

        /** 時刻（00:01:02）などを誤って伏せないよう、IPv6として妥当なものだけ対象にする */
        fun isMaskableIpv6(s: String): Boolean {
            val dbl = s.indexOf("::")
            if (dbl >= 0 && s.indexOf("::", dbl + 1) >= 0) return false
            if (s.contains(":::")) return false
            val groups = s.split(":").filter { it.isNotEmpty() }
            if (groups.any { it.length > 4 || !it.all { c -> c.isDigit() || c.lowercaseChar() in 'a'..'f' } }) return false
            return if (dbl >= 0) {
                // "::" と "::1" は伏せる意味がない。最低2グループ、かつ16進文字かfe80等を含む
                groups.size in 2..7 && groups.any { g -> g.length >= 3 || g.any { it.isLetter() } }
            } else {
                groups.size == 8
            }
        }

        /** AIの回答に出てくる記号を元の値に戻す */
        fun unmask(text: String, mapping: Map<String, String>): String {
            var out = text
            mapping.entries.sortedByDescending { it.key.length }.forEach { (token, value) ->
                out = out.replace(token, value)
            }
            return out
        }
    }
}
