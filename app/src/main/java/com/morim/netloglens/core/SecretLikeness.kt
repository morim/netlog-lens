package com.morim.netloglens.core

import kotlin.math.ln

/**
 * キーワードが無くても、値の「見た目」からパスワード・鍵らしさを判定する。
 *
 * - [isDefinite]：ハッシュ形式やAPIキーなど、ほぼ確実に秘密の値 → 自動で <SECRET> にする
 * - [isSuspect]：英大文字・小文字・数字が混ざったランダムっぽい語 → 伏せずに、確認画面で色を付けて知らせるだけ
 */
object SecretLikeness {
    /** 1語として切り出す範囲（空白・括弧・引用符・カンマ・= で区切る。末尾の = は Base64 の詰め物として含める） */
    val WORD_RE = Regex("""[^\s()\[\]{}<>,;"'|=]+=*""")

    private val DEFINITE = listOf(
        // crypt 形式のハッシュ：$1$ / $5$ / $6$ / $8$ / $9$（Cisco type 5/8/9、Juniper $9$、Linux）/ $2b$ / $y$ / $apr1$
        Regex("""\$(?:1|2[aby]?|5|6|7|8|9|y|apr1|sha1|md5)\$\S{4,}"""),
        // FortiOS の管理者パスワードハッシュ
        Regex("""SH2[A-Za-z0-9+/=]{20,}"""),
        // よく使われるAPIキー・トークン（AWS、GitHub、Slack、OpenAI系、JWT）
        Regex("""AKIA[0-9A-Z]{16}"""),
        Regex("""gh[pousr]_[A-Za-z0-9]{30,}"""),
        Regex("""xox[abprs]-[A-Za-z0-9-]{10,}"""),
        Regex("""sk-[A-Za-z0-9_-]{16,}"""),
        Regex("""eyJ[A-Za-z0-9_-]{5,}\.[A-Za-z0-9_-]{5,}\.[A-Za-z0-9_-]{5,}"""),
        // 32文字以上の16進（鍵・ハッシュ）
        Regex("""[0-9A-Fa-f]{32,}"""),
    )

    /** 32文字以上のBase64らしい文字列 */
    private val BASE64_RE = Regex("""[A-Za-z0-9+/]{32,}={0,2}""")

    /** インターフェース名・機種名など、英字＋数字でも秘密ではないもの */
    private val INTERFACE_LIKE = Regex("""[A-Za-z][A-Za-z-]*\d+(?:[/.:]\d+)*[A-Za-z]?""")
    private val FILE_LIKE = Regex("""(?i).+\.(?:bin|pkg|spa|cfg|conf|txt|log|xml|json|tar|gz|tgz|zip|img|iso|exe|py|sh|pem|crt|key)""")
    private val EMAIL_LIKE = Regex("""[\w.+-]+@[\w-]+(?:\.[\w-]+)+""")
    private const val SYMBOLS = "!@#$%^&*+?~"

    /**
     * 文章の中から「確実な秘密」を探す正規表現（前後が英数字の途中でないこと）。
     * crypt 形式は /etc/shadow のように「:」で区切られていても拾えるよう、値を「:」の手前までとする。
     */
    val DEFINITE_IN_TEXT: List<Regex> = listOf(
        Regex("""(?<![\w$])\$(?:1|2[aby]?|5|6|7|8|9|y|apr1|sha1|md5)\$[^\s:"',;]{4,}"""),
    ) + DEFINITE.drop(1).map { Regex("""(?<![\w+/=-])(?:${it.pattern})(?![\w+/=-])""") }

    fun isDefinite(word: String): Boolean {
        if (DEFINITE.any { it.matches(word) }) return true
        if (BASE64_RE.matches(word) && hasUpper(word) && hasLower(word) && word.any { it.isDigit() }) {
            // 長いパス（/flash/Images2023/...）を除くため、'/' を含むなら '+' か末尾 '=' があるものだけ
            return '/' !in word || '+' in word || word.endsWith("=")
        }
        return false
    }

    fun isSuspect(word: String): Boolean {
        if (word.length !in 8..64) return false
        if (word.startsWith("<") || isDefinite(word)) return false
        if (INTERFACE_LIKE.matches(word) || FILE_LIKE.matches(word)) return false
        if (word.startsWith("http", ignoreCase = true) || EMAIL_LIKE.matches(word)) return false
        // ファイルのパス（flash:/dir/File2023 など）
        if ('/' in word && word.all { it.isLetterOrDigit() || it in "/._-:" }) return false
        val upper = hasUpper(word)
        val lower = hasLower(word)
        val digit = word.any { it.isDigit() }
        val symbol = word.any { it in SYMBOLS }
        // 記号入りは小文字も必須（%LINK-3-UPDOWN のような syslog の見出しを除く）
        val mixed = (upper && lower && digit) || (symbol && lower && (upper || digit))
        return mixed && entropy(word) >= 2.75
    }

    /** 文章中の「要確認」の語の位置（伏せ字済みの記号は除く） */
    fun findSuspects(text: String): List<IntRange> =
        words(text).filter { (w, _) -> isSuspect(w) }.map { it.second }.toList()

    /** 文章から語を切り出す（前後の . : # > ! ? は外す） */
    fun words(text: String): Sequence<Pair<String, IntRange>> = WORD_RE.findAll(text).mapNotNull { m ->
        var s = m.range.first
        var e = m.range.last
        while (s <= e && text[s] in ".:#>") s++
        while (e >= s && text[e] in ".:#>!?") e--
        if (s > e) null else text.substring(s, e + 1) to (s..e)
    }

    private fun hasUpper(s: String) = s.any { it in 'A'..'Z' }
    private fun hasLower(s: String) = s.any { it in 'a'..'z' }

    /** 1文字あたりのシャノン・エントロピー（ビット） */
    fun entropy(s: String): Double {
        if (s.isEmpty()) return 0.0
        return s.groupingBy { it }.eachCount().values.sumOf { n ->
            val p = n.toDouble() / s.length
            -p * ln(p) / ln(2.0)
        }
    }
}
