# NetLog Lens

ネットワーク機器の画面（コンソール・ターミナル）をスマホで撮影し、**スマホ内でOCR → 機密情報を伏せ字 → Claudeアプリで解析**する個人用Androidアプリ。
アプリ自体はAIを持たず、外部に送るのは伏せ字にした後のテキストだけ。

## 使い方
1. 機種・やりたいこと（障害解析／状態の読み解き／設定レビュー）を選び、状況メモを書く
2. 画面を撮影（長いログは複数枚に分けて撮ると自動でつながる）
3. 読み取り結果を直し、伏せ字の内容を確認して「Claudeで解析」
4. Claudeの回答をコピーしてアプリに戻り「回答を貼り付ける」→ 伏せ字を元の値に戻して表示・履歴に保存

## 汎用モード
機種で「汎用（伏せ字なし）」を選ぶと、機種を限定しないプロンプトで、OCR結果を伏せ字なしでそのまま送る。
機種はClaudeがログから推定する。社外に出せないログには使わないこと。

## 伏せ字の対象（画面で個別にON/OFF）
| 項目 | 例 | 置換後 |
|---|---|---|
| IP・MAC | 10.1.2.3 / fe80::1 / 0011.2233.4455 | `<IP_1>` `<IPV6_1>` `<MAC_1>` |
| ホスト名・ユーザー名 | hostname, プロンプト, CDP/LLDP, `by admin` | `<HOST_1>` `<USER_1>` |
| パスワード・鍵 | enable secret, snmp community, key-string, 証明書 | `<SECRET>` `<CERT_DATA>` |
| シリアル | Processor board ID, SN: | `<SN_1>` |

- 同じ値には同じ記号が付くので、Claudeは「どれとどれが同じか」は分かる
- サブネットマスク・ワイルドカード（255.255.255.0, 0.0.0.255）、0.0.0.0 は解析に必要なので残す
- 記号と元の値の対応表はスマホ内（履歴）にだけ保存。パスワード類は対応表にも残さない

## インストール
GitHub の [Releases → latest](../../releases/tag/latest) から `netlog-lens.apk` をスマホで開いてインストール。
鍵が固定（`app/debug.keystore`）なので、新しいビルドは上書きインストールできる。

## 機種を増やすには
`app/src/main/java/com/morim/netloglens/core/` に `DeviceProfile` を実装したクラスを作り（`CiscoIosProfile.kt` をコピーして正規表現とヒントを書き換える）、`DeviceProfiles.all` に追加する。テストは `app/src/test/` に。

## 構成
- `core/` … 伏せ字・プロンプト生成・OCR行の組み立て（Android非依存、単体テスト済み）
- `ocr/` … ML Kit 文字認識（モデル同梱・オフライン）
- `share/` … Claudeアプリへの共有とクリップボード
- `data/` … 履歴（アプリ内JSON）
- `ui/` … Jetpack Compose の画面

ビルド：GitHub Actions（`.github/workflows/build.yml`）で単体テスト → APK作成 → latestリリース更新。
