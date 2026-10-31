package com.morim.netloglens.core

/** 確認画面で、伏せ字にした文章のどこをタップしたか */
sealed interface TapTarget {
    val range: IntRange

    /** 伏せ字の記号（<HOST_1> など）。タップすると元に戻せる */
    data class Token(val token: String, override val range: IntRange) : TapTarget {
        /** パスワード類と証明書は対応表に元の値が無いので戻せない */
        val restorable: Boolean get() = token != "<SECRET>" && token != "<CERT_DATA>"
    }

    /** 伏せ字になっていない語。タップすると伏せ字にできる */
    data class Word(val word: String, override val range: IntRange) : TapTarget

    companion object {
        val TOKEN_RE = Regex("""<[A-Z][A-Z0-9]*(?:_[A-Z0-9]+)*>""")

        /** 語の区切りとみなす文字（空白のほか、括弧・引用符・カンマなど） */
        private const val SEPARATORS = "()[]{}<>,;\"'=|"

        /** 語の前後から取り除く記号（"SW01." や "Gi0/1:" の末尾など） */
        private const val TRIM = ".:#>!?"

        /**
         * 文章の offset 文字目をタップしたときの対象を返す。何も無い位置なら null。
         * 記号の上なら [Token]、それ以外は前後の区切り文字までを1語とした [Word]。
         */
        fun at(text: String, offset: Int): TapTarget? {
            if (offset !in text.indices) return null
            TOKEN_RE.findAll(text).firstOrNull { offset in it.range }?.let { return Token(it.value, it.range) }

            fun isPart(c: Char) = !c.isWhitespace() && c !in SEPARATORS
            if (!isPart(text[offset])) return null
            var start = offset
            while (start > 0 && isPart(text[start - 1])) start--
            var end = offset
            while (end < text.length - 1 && isPart(text[end + 1])) end++
            while (start <= end && text[start] in TRIM) start++
            while (end >= start && text[end] in TRIM) end--
            if (end - start + 1 < 2) return null
            return Word(text.substring(start, end + 1), start..end)
        }
    }
}
