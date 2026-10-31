# NetLog Lens

ネットワーク機器の画面（コンソール・ターミナル）をスマホで撮影し、**スマホ内で文字認識（OCR）→（必要なら伏せ字）→ Claudeアプリで解析**する、個人用のAndroidアプリ。

- アプリ自体はAIを持たない。解析はスマホに入っているClaudeアプリに任せる
- 撮影した画像は保存も送信もしない。文字認識はスマホ内（オフライン）で行う
- 個人用で、ストア公開は前提にしない

## 使い方
1. ホーム画面で **モード**（汎用／保護）、**機種**、**やりたいこと**を選び、必要なら状況メモを書く
2. 「画面を撮影する」で撮る。長いログは何枚かに分けて撮ると自動でつながる（暗い場所はライトを使う）
3. 確認画面で読み間違いを直す。保護モードなら伏せ字の項目と、Claudeに送る全文を確認する
4. 「Claudeで解析」を押すと、Claudeアプリに文章が渡る（同時にクリップボードにもコピーされる）
5. Claudeの回答をコピーしてアプリに戻り、「回答を貼り付ける」を押す。履歴に保存され、保護モードなら伏せ字を元の値に戻して表示する

## モード
| モード | 動き |
|---|---|
| **汎用モード（既定）** | 読み取った文字を伏せ字なしでそのまま送る |
| **保護モード** | IP・ホスト名・パスワードなどを伏せ字にしてから送り、回答は元の値に戻して表示 |

- 最初は汎用モード。最後に選んだモードは次回も引き継ぐ
- 社外に出せないログは、保護モード＋機種指定（Cisco など）で使うこと

## 機種
| 機種 | 内容 |
|---|---|
| **指定なし（自動判定）**（既定） | Claudeがログから機種を推定する。保護モードでは、IP・MACに加えて `password` / `secret` / `hostname` / `user@host` / `Serial Number` など、どの機種にもありがちな書式を伏せ字にする |
| **Cisco IOS / IOS-XE** | Cisco向けの伏せ字ルールと、読み方のヒント（syslogの重大度、インターフェース名の略称など）をプロンプトに入れる |

## やりたいこと
- 障害の原因を調べる
- 状態を読み解く（showコマンドの出力など）
- 設定をレビューする

Claudeには、次の見出しで回答するよう指示している：
判定（正常／注意／異常）・読み取った要点・原因候補・次に確認するコマンド・対処案・OCRの読み取りが怪しい箇所

## 伏せ字の対象（保護モード）
画面で項目ごとにON/OFFできる。

| 項目 | 例 | 置き換え後 |
|---|---|---|
| IP・MAC | 10.1.2.3 / fe80::1 / 0011.2233.4455 | `<IP_1>` `<IPV6_1>` `<MAC_1>` |
| ホスト名・ユーザー名 | hostname、プロンプト、CDP/LLDP、`by admin` | `<HOST_1>` `<USER_1>` |
| パスワード・鍵 | enable secret、snmp community、key-string、証明書 | `<SECRET>` `<CERT_DATA>` |
| シリアル番号 | Processor board ID、SN: | `<SN_1>` |

- 同じ値には同じ記号を付ける。Claudeは中身は分からないが、「どれとどれが同じか」は分かる
- サブネットマスク・ワイルドカード（255.255.255.0、0.0.0.255）と 0.0.0.0 は解析に必要なので残す
- 記号と元の値の対応表はスマホ内（履歴）にだけ保存する。パスワード類は対応表にも残さない

## インストール
GitHub の [Releases → latest](../../releases/tag/latest) から `netlog-lens.apk` をスマホで開いてインストールする。
署名の鍵を固定（`app/debug.keystore`）しているので、新しいビルドはアンインストールせずに上書きインストールできる。

## 機種を増やすには
1. `app/src/main/java/com/morim/netloglens/core/` に `DeviceProfile` を実装したクラスを作る（`CiscoIosProfile.kt` を手本に、伏せ字の正規表現とプロンプトのヒントを書き換える）
2. `DeviceProfile.kt` の `DeviceProfiles.all` に1行追加する
3. `app/src/test/java/com/morim/netloglens/core/` に、その機種の実ログを使ったテストを追加する

## 構成
| フォルダ | 内容 |
|---|---|
| `core/` | 伏せ字（Masker）、プロンプト作成、モード・機種の定義、OCR結果の行・列の復元。Androidに依存しないので単体テストできる |
| `ocr/` | ML Kit の文字認識（Latin、モデル同梱・オフライン） |
| `share/` | Claudeアプリへの共有（`com.anthropic.claude` を直接指定、無ければ共有先の選択画面）とクリップボード |
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
- 実機での動作確認（特に、Claudeアプリへ共有したときに本文が入力欄に入るか）
- 機種の追加（候補：YAMAHA RTX）
- APKのサイズ削減（今は約55MB。OCRモデルを同梱しているため）
