# NetLog Lens

ネットワーク機器の画面（コンソール・ターミナル）をスマホで撮影し、**スマホ内で文字認識（OCR）→（必要なら伏せ字）→ AIアプリ（Claude／ChatGPT／Gemini／Copilot）で解析**する、個人用のAndroidアプリ。

- アプリ自体はAIを持たない。解析はスマホに入っているAIアプリ（既定はClaude）に任せる
- 撮影した画像は保存も送信もしない。文字認識はスマホ内（オフライン）で行う
- 個人用で、ストア公開は前提にしない

## 使い方
1. ホーム画面で **モード**（汎用／保護）、**機種**、**やりたいこと**、**送り先AI**を選び、必要なら状況メモを書く
2. 「画面を撮影する」で撮る。長いログは何枚かに分けて撮ると自動でつながる（暗い場所はライトを使う）
3. 確認画面で読み間違いを直す。保護モードなら伏せ字の項目と、AIに送る全文を確認する
4. 「Claudeで解析」（送り先AIの名前が入る）を押すと、そのアプリに文章が渡る（同時にクリップボードにもコピーされる）
5. AIの回答をコピーしてアプリに戻り、「回答を貼り付ける」を押す。履歴に保存され、保護モードなら伏せ字を元の値に戻して表示する

## モード
| モード | 動き |
|---|---|
| **汎用モード（既定）** | 読み取った文字を伏せ字なしでそのまま送る |
| **保護モード** | IP・ホスト名・パスワードなどを伏せ字にしてから送り、回答は元の値に戻して表示 |

- 最初は汎用モード。最後に選んだモードは次回も引き継ぐ
- 社外に出せないログは保護モードで使うこと。伏せ字は機種を選ばなくても同じように効く（下の「伏せ字の対象」参照）

## 送り先AI
| 送り先 | パッケージ名 |
|---|---|
| **Claude**（既定） | `com.anthropic.claude` |
| ChatGPT | `com.openai.chatgpt` |
| Gemini | `com.google.android.apps.bard` |
| Copilot | `com.microsoft.copilot` |

- 最後に選んだ送り先は次回も引き継ぐ
- 入っていないアプリは「（未インストール）」と表示し、送るときは共有先の選択画面を出す
- どの送り先でも文章はクリップボードにも入るので、共有で本文が入らなければ貼り付ければよい
- 社外に出せないログを送る前に、送り先サービスの利用規約・社内ルールを確認すること

## 機種
| 機種 | 内容 |
|---|---|
| **指定なし（自動判定）**（既定） | AIがログから機種を推定する |
| **Cisco IOS / IOS-XE** | Cisco向けの読み方のヒント（syslogの重大度、インターフェース名の略称など）をプロンプトに入れる |

機種の選択は **プロンプトのヒントだけ** に効く。伏せ字は機種に依存しない共通ルール（`core/MaskRules.kt`）で行うので、どの機種を選んでも同じ結果になる。
共通ルールは「キーワード＋値」（`password xxx` / `secret: xxx` / `key=xxx` / `"token": "xxx"`）とプロンプト（`R1#` / `<HUAWEI>` / `user@host:~$`）で見つけるため、Cisco・Juniper・Huawei・FortiGate・YAMAHA・Linux・設定ファイル（JSON/XML）などをまとめて扱える。

## やりたいこと
- 障害の原因を調べる
- 状態を読み解く（showコマンドの出力など）
- 設定をレビューする

AIには、次の見出しで回答するよう指示している：
判定（正常／注意／異常）・読み取った要点・原因候補・次に確認するコマンド・対処案・OCRの読み取りが怪しい箇所

## 伏せ字の対象（保護モード）
画面で項目ごとにON/OFFできる。

| 項目 | 例 | 置き換え後 |
|---|---|---|
| IP・MAC | 10.1.2.3 / fe80::1 / 0011.2233.4455 | `<IP_1>` `<IPV6_1>` `<MAC_1>` |
| ホスト名・ユーザー名 | hostname、プロンプト、CDP/LLDP、`by admin` | `<HOST_1>` `<USER_1>` |
| パスワード・鍵 | enable secret、snmp community、key-string、証明書 | `<SECRET>` `<CERT_DATA>` |
| シリアル番号 | Processor board ID、SN: | `<SN_1>` |

- 同じ値には同じ記号を付ける。AIは中身は分からないが、「どれとどれが同じか」は分かる
- サブネットマスク・ワイルドカード（255.255.255.0、0.0.0.255）と 0.0.0.0 は解析に必要なので残す
- 記号と元の値の対応表はスマホ内（履歴）にだけ保存する。パスワード類は対応表にも残さない

## インストール
GitHub の [Releases → latest](../../releases/tag/latest) から `netlog-lens.apk` をスマホで開いてインストールする。
署名の鍵を固定（`app/debug.keystore`）しているので、新しいビルドはアンインストールせずに上書きインストールできる。

## 機種を増やすには
1. `app/src/main/java/com/morim/netloglens/core/` に `DeviceProfile` を実装したクラスを作る（`CiscoIosProfile.kt` を手本に、プロンプトのヒントを書く）
2. `DeviceProfile.kt` の `DeviceProfiles.all` に1行追加する
3. `app/src/test/java/com/morim/netloglens/core/MaskerTest.kt` に、その機種の実ログを「指定なし」で伏せるテストを足し、漏れがないか確かめる
4. 漏れがあれば、まず `MaskRules.kt`（共通ルール）を直す。その機種にしかない書き方だけは、プロファイルの `secretPatterns` などに追加ルールとして書ける

## 構成
| フォルダ | 内容 |
|---|---|
| `core/` | 伏せ字（Masker）、プロンプト作成、モード・機種の定義、OCR結果の行・列の復元。Androidに依存しないので単体テストできる |
| `ocr/` | ML Kit の文字認識（Latin、モデル同梱・オフライン） |
| `share/` | 選んだAIアプリへの共有（パッケージ名を直接指定、無ければ共有先の選択画面）とクリップボード |
| `data/` | 履歴（アプリ内のJSON、最大200件） |
| `ui/` | Jetpack Compose の画面（ホーム・撮影・確認・解析結果・履歴） |

## 開発環境
- Kotlin 2.1.0 + Jetpack Compose、Android Gradle Plugin 8.7.3
- minSdk 26（Android 8.0）／ targetSdk 35
- CameraX 1.4.1、ML Kit Text Recognition 16.0.1
- Gradle 8.11.1（Gradle Wrapper は同梱していない。手元でビルドするときは Android Studio で開くか、Gradle 8.11.1 を使う）

## ビルドとリリース
`main` に push すると GitHub Actions（`.github/workflows/build.yml`）が次を行う：
1. 単体テスト（`gradle testDebugUnitTest`）
2. APKを作る（`gradle assembleDebug`）
3. Release「latest」を作り直し、`netlog-lens.apk` を置く

## 今後の予定・未確認事項
- 実機での動作確認（特に、各AIアプリへ共有したときに本文が入力欄に入るか）
- 機種の追加（候補：YAMAHA RTX）
- APKのサイズ削減（今は約55MB。OCRモデルを同梱しているため）
