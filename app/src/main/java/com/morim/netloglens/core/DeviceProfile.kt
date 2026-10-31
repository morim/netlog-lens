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
}

/** 機種を限定しないプロファイル。保護モードでは、どの機種にもありがちな書式だけを伏せ字にする。 */
object GenericProfile : DeviceProfile {
    override val id = "generic"
    override val displayName = "指定なし（自動判定）"
    override val expertise = "ネットワーク機器・サーバ・PCのログや画面表示"
    override val promptHints = listOf(
        "機種・OS・ソフトウェアはログの内容から推定し、回答の最初に推定結果を書いてください。",
    )

    // 改行をまたがないよう、空白は [ \t] に限定する
    override val secretPatterns = listOf(
        Regex("""(?im)((?<![A-Za-z])(?:password|passwd|secret|community|pre-shared-key|psk|api[_-]?key|token)(?:[ \t]*[:=][ \t]*|[ \t]+(?:\d{1,2}[ \t]+)?))([^\s<]\S*)"""),
        Regex("""(?im)(\bkey-string(?:[ \t]+\d{1,2})?[ \t]+)(\S+)"""),
    )
    override val hostnamePatterns = listOf(
        Regex("""(?im)^[ \t]*(?:set[ \t]+)?hostname[ \t]+"?([\w.-]+)"""),
        Regex("""(?m)^([A-Za-z][\w.-]{0,62})(?:\([\w-]+\))?[#>]"""),
        Regex("""(?m)^[\w.-]+@([\w.-]+)[:~ ]"""), // user@host:~$
    )
    override val usernamePatterns = listOf(
        Regex("""(?im)^[ \t]*username[ \t]+(\S+)"""),
        Regex("""(?m)^([\w.-]+)@[\w.-]+[:~ ]"""),
        Regex("""(?im)^[ \t]*(?:User(?:name)?|Login)[ \t]*:[ \t]*(\S+)"""),
    )
    override val serialPatterns = listOf(
        Regex("""(?i)\bSerial(?:[ \t_-]*(?:Number|No\.?))?[ \t]*[:=][ \t]*(\S+)"""),
        Regex("""(?i)\bSN:[ \t]*([A-Za-z0-9]{6,})"""),
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
