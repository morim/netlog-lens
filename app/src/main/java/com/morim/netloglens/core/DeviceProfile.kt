package com.morim.netloglens.core

/**
 * 機種ごとの違いをまとめたもの。
 * 新しい機種に対応するときは、このインターフェースを実装したクラスを作り、
 * [DeviceProfiles.all] に1行追加するだけでよい。
 * 伏せ字は機種に依存しない共通ルール（[CommonMaskRules]）で行うので、
 * 機種ごとに書くのは基本的にプロンプトのヒントだけ。
 */
interface DeviceProfile {
    /** 履歴保存用の固定ID（変えないこと） */
    val id: String

    /** 画面に出す名前 */
    val displayName: String

    /** プロンプト冒頭の「あなたは〇〇に詳しい…」の〇〇 */
    val expertise: String

    /** 機種固有の読み方のヒント（プロンプトに追記される） */
    val promptHints: List<String>

    // ここから下は「機種固有の追加ルール」（任意）。
    // 伏せ字は機種に依存しない共通ルール（CommonMaskRules）で行うので、普通は書かなくてよい。
    // 共通ルールで拾えない、その機種だけの書き方があるときだけ足す。

    /** パスワード・鍵の値を見つける正規表現。グループ1=前置き（残す）、グループ2=値（伏せる） */
    val secretPatterns: List<Regex> get() = emptyList()

    /** ホスト名を見つける正規表現。グループ1=ホスト名 */
    val hostnamePatterns: List<Regex> get() = emptyList()

    /** ユーザー名を見つける正規表現。グループ1=ユーザー名 */
    val usernamePatterns: List<Regex> get() = emptyList()

    /** シリアル番号を見つける正規表現。グループ1=シリアル */
    val serialPatterns: List<Regex> get() = emptyList()

    /** 行全体を置き換える正規表現と置換文字列 */
    val blockPatterns: List<Pair<Regex, String>> get() = emptyList()
}

/** 機種を限定しないプロファイル。伏せ字は共通ルール（CommonMaskRules）だけで行う。 */
object GenericProfile : DeviceProfile {
    override val id = "generic"
    override val displayName = "指定なし（自動判定）"
    override val expertise = "ネットワーク機器・サーバ・PCのログや画面表示"
    override val promptHints = listOf(
        "機種・OS・ソフトウェアはログの内容から推定し、回答の最初に推定結果を書いてください。",
    )
}

object DeviceProfiles {
    val all: List<DeviceProfile> = listOf(
        GenericProfile,
        CiscoIosProfile,
        // 例：YamahaRtxProfile, FortigateProfile をここに追加
    )

    fun byId(id: String): DeviceProfile = all.firstOrNull { it.id == id } ?: all.first()
}
