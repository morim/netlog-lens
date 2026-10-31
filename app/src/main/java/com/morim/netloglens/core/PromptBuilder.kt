package com.morim.netloglens.core

enum class Purpose(val label: String, val instruction: String) {
    TROUBLE(
        "障害の原因を調べる",
        "このログから障害・異常の原因を推定し、切り分けの手順を示してください。",
    ),
    STATUS(
        "状態を読み解く",
        "このshowコマンド等の出力を読み解き、正常か、気になる点はないかを説明してください。",
    ),
    CONFIG(
        "設定をレビューする",
        "この設定の誤り・抜け・セキュリティ上の問題・ベストプラクティスとの差を指摘してください。",
    ),
}

object PromptBuilder {
    fun build(profile: DeviceProfile, purpose: Purpose, memo: String, maskedLog: String): String = buildString {
        appendLine("あなたは${profile.expertise}に詳しいネットワークエンジニアです。")
        appendLine("以下はスマホで機器の画面を撮影し、OCRで文字起こししたログです。")
        appendLine("OCRの誤認識（0とO、1とlとI、:と;、行の欠け・重複など）が含まれる可能性があるので、文脈から補って読んでください。")
        if (profile.masking) {
            appendLine("<IP_1> や <HOST_1> のような記号は、機密保護のための伏せ字です。同じ記号は同じ値を表します。中身は推測せず、回答でも記号をそのまま使ってください。<SECRET> はパスワード等で、値の良し悪しは判断不要です。")
        }
        profile.promptHints.forEach { appendLine("- $it") }
        appendLine()
        appendLine("## 依頼内容")
        appendLine(purpose.instruction)
        if (memo.isNotBlank()) {
            appendLine()
            appendLine("## 状況メモ")
            appendLine(memo.trim())
        }
        appendLine()
        appendLine("## ログ")
        appendLine("```")
        appendLine(maskedLog.trim())
        appendLine("```")
        appendLine()
        appendLine("## 回答形式")
        appendLine("日本語で、次の見出しのとおりに答えてください。")
        appendLine("### 判定")
        appendLine("正常／注意／異常 のどれかと一言")
        appendLine("### 読み取った要点")
        appendLine("### 原因候補（可能性の高い順）")
        appendLine("### 次に確認するコマンド")
        appendLine("コードブロックで、実行順に")
        appendLine("### 対処案")
        appendLine("### OCRの読み取りが怪しい箇所")
    }
}
