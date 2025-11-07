# ルール確認機能のテスト手順

## 🚀 クイックスタート

### 1. アプリケーションを起動

```bash
mvn quarkus:dev
```

### 2. サンプルルールを読み込み

```bash
# 例1: Socratesの例
curl -X POST http://localhost:8080/api/rules/load-example

# 例2: カスタムルール（YAML形式）
curl -X POST "http://localhost:8080/api/rules/load?filePath=src/main/resources/rules/example-rules.yaml"
```

### 3. 読み込んだルールを確認

#### 方法A: シェルスクリプトを使う（推奨）

```bash
./inspect-rules.sh
```

**出力例:**
```
╔════════════════════════════════════════════════╗
║       TensorLogic ルール確認ツール            ║
╚════════════════════════════════════════════════╝

===== システム状態 =====
{
  "totalRules": 1,
  "totalFacts": 2,
  "operationCounts": {
    "MODUS_PONENS": 1
  },
  "totalTensorElements": 2,
  "status": "稼働中"
}

===== ルール一覧 =====
[MODUS_PONENS] modus_ponens_1
  記法: socrates_is_human ∧ human_is_mortal ⟹ socrates_is_mortal

===== 事実（ファクト）一覧 =====
socrates_is_human [1]
  値: [1.00]
  統計: min=1.0, max=1.0, mean=1.0

human_is_mortal [1, 1]
  値: [[0.98]]
  統計: min=0.98, max=0.98, mean=0.98

完了！
```

#### 方法B: 個別にAPIを叩く

**全ルール一覧:**
```bash
curl http://localhost:8080/api/rules/inspect/rules | jq
```

**全事実一覧:**
```bash
curl http://localhost:8080/api/rules/inspect/facts | jq
```

**特定のルール詳細:**
```bash
curl http://localhost:8080/api/rules/inspect/rules/modus_ponens_1 | jq
```

**特定の事実詳細:**
```bash
curl http://localhost:8080/api/rules/inspect/facts/socrates_is_human | jq
```

**システム状態:**
```bash
curl http://localhost:8080/api/rules/inspect/status | jq
```

#### 方法C: Swagger UIを使う

1. ブラウザで http://localhost:8080/swagger-ui を開く
2. **Rule Inspector** セクションを展開
3. 各エンドポイントを試す

---

## 📊 確認できる情報

### 1. ルール情報

- **名前**: ルールのユニークな識別子
- **入力**: 入力となる事実のリスト
- **出力**: 出力として生成される事実
- **演算タイプ**: MODUS_PONENS, CHAIN, CONJUNCTION など
- **論理記法**: 数学的な表現（∧, ⟹, ○ など）

**例:**
```json
{
  "name": "modus_ponens_1",
  "inputs": ["socrates_is_human", "human_is_mortal"],
  "output": "socrates_is_mortal",
  "operation": "MODUS_PONENS",
  "notation": "socrates_is_human ∧ human_is_mortal ⟹ socrates_is_mortal"
}
```

### 2. 事実（テンソル）情報

- **名前**: 事実のユニークな識別子
- **形状**: テンソルの次元（例: [1], [3, 3]）
- **データ型**: DOUBLE, FLOAT など
- **プレビュー**: 値の簡略表示
- **統計情報**: min, max, mean, std
- **要素数**: テンソルの全要素数

**例:**
```json
{
  "name": "socrates_is_human",
  "shape": "[1]",
  "dtype": "DOUBLE",
  "fullContent": "[1.00]",
  "stats": {
    "min": 1.0,
    "max": 1.0,
    "mean": 1.0,
    "std": 0.0
  },
  "elementCount": 1
}
```

### 3. システム状態

- **totalRules**: 登録されているルールの総数
- **totalFacts**: 登録されている事実の総数
- **operationCounts**: 演算タイプごとのルール数
- **totalTensorElements**: 全テンソルの要素数の合計
- **status**: システムの状態（稼働中、エラーなど）

**例:**
```json
{
  "totalRules": 3,
  "totalFacts": 5,
  "operationCounts": {
    "MODUS_PONENS": 1,
    "CHAIN": 2
  },
  "totalTensorElements": 102,
  "status": "稼働中"
}
```

---

## 🔍 高度な使い方

### 1. ルールの依存関係を確認

```bash
# 全ルールの入力を確認して依存関係を把握
curl -s http://localhost:8080/api/rules/inspect/rules | \
  jq -r '.rules[] | "\(.name) depends on: \(.inputs | join(", "))"'
```

**出力例:**
```
modus_ponens_1 depends on: socrates_is_human, human_is_mortal
friend_of_friend depends on: is_friend, is_friend
```

### 2. テンソルの統計情報を比較

```bash
# 全事実の統計を一覧表示
curl -s http://localhost:8080/api/rules/inspect/facts | \
  jq -r '.facts[] | "\(.name):\n  min=\(.stats.min), max=\(.stats.max), mean=\(.stats.mean)\n"'
```

### 3. 演算タイプ別のルール数を表示

```bash
# システム状態から演算タイプを抽出
curl -s http://localhost:8080/api/rules/inspect/status | \
  jq '.operationCounts'
```

**出力例:**
```json
{
  "MODUS_PONENS": 1,
  "CHAIN": 2,
  "CONJUNCTION": 1
}
```

### 4. 監視スクリプト（定期的にチェック）

```bash
# watch-rules.sh
#!/bin/bash

while true; do
  clear
  echo "=== 現在時刻: $(date) ==="
  curl -s http://localhost:8080/api/rules/inspect/status | jq
  sleep 5
done
```

**使用方法:**
```bash
chmod +x watch-rules.sh
./watch-rules.sh
```

---

## 🧪 テストシナリオ

### シナリオ1: 基本的な確認

```bash
# 1. システム状態を確認
curl http://localhost:8080/api/rules/inspect/status

# 2. ルールが0件であることを確認
# → totalRules: 0

# 3. ルールを読み込み
curl -X POST http://localhost:8080/api/rules/load-example

# 4. 再度システム状態を確認
curl http://localhost:8080/api/rules/inspect/status

# 5. ルールが追加されたことを確認
# → totalRules: 1 以上
```

### シナリオ2: ルールの詳細確認

```bash
# 1. 全ルールを表示
curl http://localhost:8080/api/rules/inspect/rules | jq '.rules[].name'

# 2. 最初のルール名を取得
RULE_NAME=$(curl -s http://localhost:8080/api/rules/inspect/rules | jq -r '.rules[0].name')

# 3. そのルールの詳細を表示
curl "http://localhost:8080/api/rules/inspect/rules/$RULE_NAME" | jq
```

### シナリオ3: テンソルの内容確認

```bash
# 1. 全事実を表示
curl http://localhost:8080/api/rules/inspect/facts | jq '.facts[].name'

# 2. 最初の事実名を取得
FACT_NAME=$(curl -s http://localhost:8080/api/rules/inspect/facts | jq -r '.facts[0].name')

# 3. そのテンソルの完全な内容を表示
curl "http://localhost:8080/api/rules/inspect/facts/$FACT_NAME" | jq '.fact.fullContent'
```

---

## 🐛 トラブルシューティング

### 問題1: `jq: command not found`

**解決方法:**
```bash
# macOS
brew install jq

# Ubuntu/Debian
sudo apt-get install jq

# または jq 無しでも動作します（整形されない出力）
curl http://localhost:8080/api/rules/inspect/status
```

### 問題2: `Connection refused`

**原因:** アプリケーションが起動していない

**解決方法:**
```bash
# 別のターミナルでアプリケーションを起動
mvn quarkus:dev

# 起動を確認
curl http://localhost:8080/q/health
```

### 問題3: `404 Not Found`

**原因:** エンドポイントのパスが間違っている

**解決方法:**
```bash
# 正しいパス:
curl http://localhost:8080/api/rules/inspect/status

# 間違ったパス:
curl http://localhost:8080/rules/inspect/status  # ❌ /api/ が抜けている
```

### 問題4: ルールや事実が表示されない

**原因:** ルールをまだ読み込んでいない

**解決方法:**
```bash
# サンプルルールを読み込み
curl -X POST http://localhost:8080/api/rules/load-example

# または外部ファイルから読み込み
curl -X POST "http://localhost:8080/api/rules/load?filePath=src/main/resources/rules/example-rules.yaml"
```

---

## 📚 関連ドキュメント

- [RULE_INSPECTION_GUIDE.md](./RULE_INSPECTION_GUIDE.md) - 詳細なガイド
- [RULE_DSL_GUIDE.md](./RULE_DSL_GUIDE.md) - ルールDSLの説明
- [RULE_AND_TENSOR_GUIDE.md](./RULE_AND_TENSOR_GUIDE.md) - ルールとテンソル変換
- [QUICKSTART.md](./QUICKSTART.md) - 基本的な使い方

---

## 🎯 まとめ

### 確認方法の選び方

| 方法 | 推奨ケース | 特徴 |
|------|-----------|------|
| **シェルスクリプト** | 手軽に確認したい | 全情報を一度に表示 |
| **個別API** | 特定の情報だけ必要 | 柔軟にカスタマイズ可能 |
| **Swagger UI** | GUIで操作したい | インタラクティブ、初心者向け |
| **Javaコード** | プログラムから確認 | 自動化、カスタム処理 |

### よく使うコマンド

```bash
# クイック確認
./inspect-rules.sh

# システム状態のみ
curl -s http://localhost:8080/api/rules/inspect/status | jq

# ルール一覧のみ
curl -s http://localhost:8080/api/rules/inspect/rules | jq '.rules[].name'

# 事実一覧のみ
curl -s http://localhost:8080/api/rules/inspect/facts | jq '.facts[].name'
```

---

**作成日**: 2025年11月5日  
**対応バージョン**: Java 21 + Quarkus 3.6

