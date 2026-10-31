package com.morim.netloglens.core

/** 伏せ字にする情報の種類。画面のON/OFFスイッチと1対1で対応する。 */
enum class MaskCategory(val label: String) {
    ADDRESS("IP・MACアドレス"),
    HOST_USER("ホスト名・ユーザー名"),
    SECRET("パスワード・鍵"),
    SERIAL("シリアル番号"),
}
