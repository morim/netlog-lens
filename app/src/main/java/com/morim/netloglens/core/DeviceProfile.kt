package com.morim.netloglens.core

/**
 * 機種ごとの違いをまとめたもの。
 * 新しい機種に対応するときは、このインターフェースを実装したクラスを作り、
 * [DeviceProfiles.all] に1行追加するだけでよい。
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

    /** パスワード・鍵の値を見つける正規表現。グループ1=前置き（残す）、グループ2=値（伏せる） */
    val secretPatterns: List<Regex>

    /** ホスト名を見つける正規表現。グループ1=ホスト名 */
    val hostnamePatterns: List<Regex>

    /** ユーザー名を見つける正規表現。グループ1=ユーザー名 */
    val usernamePatterns: List<Regex>

    /** シリアル番号を見つける正規表現。グループ1=シリアル */
    val serialPatterns: List<Regex>

    /** 行全体を置き換える正規表現（証明書の16進ダンプなど）と置換文字列 */
    val blockPatterns: List<Pair<Regex, String>> get() = emptyList()

    /** false にすると伏せ字を一切行わない（汎用モード） */
    val masking: Boolean get() = true
}

/** 汎用モード：機種を限定せず、伏せ字もしない。OCR結果をそのまま送る。 */
object GenericProfile : DeviceProfile {
    override val id = "generic"
    override val displayName = "汎用（伏せ字なし）"
    override val expertise = "ネットワーク機器・サーバ・PCのログや画面表示"
    override val promptHints = listOf(
        "機種・OS・ソフトウェアはログの内容から推定し、回答の最初に推定結果を書いてください。",
    )
    override val secretPatterns = emptyList<Regex>()
    override val hostnamePatterns = emptyList<Regex>()
    override val usernamePatterns = emptyList<Regex>()
    override val serialPatterns = emptyList<Regex>()
    override val masking = false
}

object DeviceProfiles {
    val all: List<DeviceProfile> = listOf(
        CiscoIosProfile,
        GenericProfile,
        // 例：YamahaRtxProfile, FortigateProfile をここに追加
    )

    fun byId(id: String): DeviceProfile = all.firstOrNull { it.id == id } ?: all.first()
}
