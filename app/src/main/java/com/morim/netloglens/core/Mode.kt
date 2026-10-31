package com.morim.netloglens.core

/** 送り方のモード。既定は汎用モード。 */
enum class Mode(val label: String, val description: String) {
    GENERAL("汎用モード", "読み取った文字をそのまま送ります（伏せ字なし）"),
    PROTECTED("保護モード", "IP・ホスト名・パスワード等を伏せ字にしてから送ります"),
}
