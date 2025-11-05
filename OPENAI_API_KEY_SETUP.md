# OpenAI APIキー設定ガイド

## 🔧 環境変数が読み込まれない問題のトラブルシューティング

---

## 🎯 問題: 環境変数 `OPENAI_API_KEY` を設定してもデモモードのまま

### **症状**
```bash
curl -X POST http://localhost:8080/api/camel/verify \
  -H 'Content-Type: application/json' \
  -d '"ソクラテスは生きている。"'
```

**期待**: 実際のLLMが質問内容を理解して回答

**実際**: デモモードの固定レスポンスが返る
```json
{
  "answer": "はい、ソクラテスは死にます。"  // ← 質問を無視
}
```

---

## 🔍 原因の特定

### **ステップ1: ログを確認**

アプリケーションのログに以下が表示されているか確認：

```
⚠️ デモモードで動作します（OpenAI APIキーが未設定または'demo-mode'）
   実際のLLMを使用するには、環境変数 OPENAI_API_KEY を設定してください
```

デバッグログも確認：
```
APIキーの状態:
  - 設定値: demo-mode
  - null?: false
  - blank?: false
  - equals('demo-mode')?: true
動作モード: デモモード
```

---

## ✅ 解決方法

### **方法1: アプリケーションを再起動する** ⭐ **最も多い原因**

環境変数を設定した**後**にアプリケーションを起動する必要があります。

#### **間違った手順** ❌
```bash
# 1. アプリケーション起動（先）
mvn quarkus:dev

# 2. 別のターミナルで環境変数設定（後）
export OPENAI_API_KEY="sk-..."  # ← 起動中のアプリには反映されない！
```

#### **正しい手順** ✅
```bash
# 1. 環境変数を設定（先）
export OPENAI_API_KEY="sk-..."

# 2. 確認
echo $OPENAI_API_KEY

# 3. アプリケーション起動（後）
mvn quarkus:dev
```

または、**既存のアプリケーションを停止して再起動**：
```bash
# Ctrl+C でアプリを停止

# 環境変数を設定
export OPENAI_API_KEY="sk-..."

# 再起動
mvn quarkus:dev
```

---

### **方法2: `.env` ファイルを使用（推奨）** ⭐

Quarkusは自動的にプロジェクトルートの `.env` ファイルを読み込みます。

#### **1. `.env` ファイルを作成**

```bash
cd /Users/kjin/ai/TensorLogic

# .env.example をコピー
cp .env.example .env

# エディタで編集
nano .env
```

#### **2. APIキーを記入**

```bash
# .env ファイルの内容
OPENAI_API_KEY=sk-proj-xxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

⚠️ **注意**: 
- `OPENAI_API_KEY=` の後にスペースを入れない
- クォートは不要
- 実際のAPIキーに置き換える

#### **3. ファイルを保存して再起動**

```bash
# Ctrl+C でアプリを停止（起動中の場合）

# 再起動
mvn quarkus:dev
```

#### **利点**
- ✅ 永続的（ターミナルを閉じても有効）
- ✅ プロジェクト固有の設定
- ✅ `.gitignore` に含まれているので安全

---

### **方法3: `application.yaml` に直接記述（非推奨）**

⚠️ **セキュリティリスク**: APIキーがGitにコミットされる可能性

```yaml
# src/main/resources/application.yaml
llm:
  openai:
    api-key: sk-proj-xxxxxxxxxxxxxxxxxxxxxxxxxxxxx  # ⚠️ 危険
```

**この方法は本番環境では絶対に使わないでください！**

---

## 🔍 動作確認

### **ステップ1: ログを確認**

アプリケーション起動時に以下のログが表示されるはずです：

```
動作モード: OpenAI API実行モード
OpenAiServiceを初期化します...
```

### **ステップ2: APIをテスト**

```bash
curl -X POST http://localhost:8080/api/camel/verify \
  -H 'Content-Type: application/json' \
  -d '"ソクラテスは生きている。"'
```

**成功時の出力例**:
```json
{
  "answer": "いいえ、ソクラテスは紀元前399年に亡くなりました。",
  "llmConfidence": 0.95,
  "reasoningSteps": [
    "1. ソクラテスは古代ギリシャの哲学者です。",
    "2. 彼は紀元前399年に処刑されました。",
    "3. したがって、現在は生きていません。"
  ],
  "isLogicallySound": true
}
```

✅ **質問の内容を理解した回答が返ってきます！**

---

## 🐛 よくある問題

### **問題1: 環境変数が空**

```bash
echo $OPENAI_API_KEY
# (何も表示されない)
```

**解決**:
```bash
# 正しく設定
export OPENAI_API_KEY="sk-..."

# 確認
echo $OPENAI_API_KEY
# → sk-proj-... と表示されるはず
```

---

### **問題2: 別のターミナルで設定した**

環境変数は**設定したターミナルセッションでのみ有効**です。

**シナリオ**:
```bash
# ターミナル1
export OPENAI_API_KEY="sk-..."

# ターミナル2 (別のウィンドウ)
mvn quarkus:dev  # ← APIキーは設定されていない！
```

**解決**:
- **同じターミナル**で環境変数を設定してから起動
- または `.env` ファイルを使用

---

### **問題3: Quarkusの開発モードで環境変数が更新されない**

Quarkusの開発モード（`mvn quarkus:dev`）は、起動時の環境変数を使用します。

**解決**:
1. アプリケーションを停止（Ctrl+C）
2. 環境変数を設定
3. 再起動

**または、`.env` ファイルを編集後、Quarkusが自動リロードします**

---

### **問題4: APIキーの形式が間違っている**

OpenAI APIキーの正しい形式：
```
sk-proj-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

または
```
sk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

**確認**:
```bash
echo $OPENAI_API_KEY | grep -E '^sk-'
# → APIキーが表示されればOK
```

---

### **問題5: APIキーが無効**

```
LLM API呼び出しエラー
→ デモモードで動作します
```

**原因**:
- APIキーが期限切れ
- APIキーが無効化されている
- クォータ（使用制限）を超えている

**解決**:
1. OpenAIダッシュボードで確認: https://platform.openai.com/api-keys
2. 新しいAPIキーを作成
3. 課金設定を確認

---

## 📊 デバッグログの見方

### **デモモードの場合**

```log
APIキーの状態:
  - 設定値: demo-mode
  - null?: false
  - blank?: false
  - equals('demo-mode')?: true
動作モード: デモモード
⚠️ デモモードで動作します（OpenAI APIキーが未設定または'demo-mode'）
```

### **APIキーが正しく設定されている場合**

```log
APIキーの状態:
  - 設定値: sk-proj...
  - null?: false
  - blank?: false
  - equals('demo-mode')?: false
動作モード: OpenAI API実行モード
OpenAiServiceを初期化します...
```

---

## 🎯 チェックリスト

環境変数が効かない場合、以下を確認：

- [ ] 環境変数を設定した**後**にアプリケーションを起動したか
- [ ] `echo $OPENAI_API_KEY` でAPIキーが表示されるか
- [ ] APIキーの形式が `sk-` で始まっているか
- [ ] 同じターミナルセッションで設定と起動を行ったか
- [ ] アプリケーションのログを確認したか
- [ ] ログに「デモモード」と表示されていないか
- [ ] `.env` ファイルを使う場合、ファイルがプロジェクトルートにあるか
- [ ] `.env` ファイルに余計なスペースや改行がないか

---

## 🔒 セキュリティのベストプラクティス

### **✅ 推奨**
- `.env` ファイルを使用（`.gitignore` に含まれている）
- 環境変数で設定
- システムの環境変数管理ツールを使用

### **❌ 非推奨**
- `application.yaml` に直接記述
- ソースコードにハードコード
- パブリックリポジトリにコミット

---

## 📚 関連情報

### **OpenAI APIキーの取得**
https://platform.openai.com/api-keys

### **Quarkus設定ガイド**
https://quarkus.io/guides/config-reference

### **料金確認**
https://platform.openai.com/usage

---

**作成日**: 2025年11月5日  
**対応バージョン**: Java 21 + Quarkus 3.6

