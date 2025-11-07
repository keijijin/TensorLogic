# 汎用LLM検証のテスト手順

## 🎯 テスト目的

外部ルールファイルを使用した汎用LLM検証が正しく動作することを確認します。

---

## 📋 前提条件

1. アプリケーションが起動している
```bash
mvn quarkus:dev
```

2. 必要なルールファイルが存在する
- `src/main/resources/rules/example-rules.yaml`
- `src/main/resources/rules/simple-verification-rules.yaml`

---

## ✅ テストケース

### **テスト1: 基本的な検証（ソクラテスの三段論法）**

**目的:** 既存のルールファイルで検証が動作することを確認

```bash
curl -X POST http://localhost:8080/api/camel/generic-verify \
  -H "Content-Type: application/json" \
  -d '{
    "query": "ソクラテスは死ぬか？",
    "ruleFile": "rules/example-rules.yaml",
    "expectedOutputs": {
      "socrates_is_mortal": 0.98
    },
    "tolerance": 0.05
  }' | jq
```

**期待される結果:**
```json
{
  "success": true,
  "logicallySound": true,
  "validationScore": 1.0,
  "verificationMatches": [
    "socrates_is_mortal: expected=0.980, actual=0.980 ✓"
  ],
  "verificationMismatches": [],
  "missingExpectedFacts": []
}
```

---

### **テスト2: シンプルな命題論理の検証**

**目的:** 汎用的なルールで検証が動作することを確認

```bash
curl -X POST http://localhost:8080/api/camel/generic-verify \
  -H "Content-Type: application/json" \
  -d '{
    "query": "命題Aが真で、AならばBの場合、Bは真か？",
    "ruleFile": "rules/simple-verification-rules.yaml",
    "expectedOutputs": {
      "statement_b_true": 0.855
    },
    "tolerance": 0.1
  }' | jq
```

**期待される結果:**
```json
{
  "success": true,
  "logicallySound": true,
  "validationScore": 1.0,
  "llmAnswer": "はい、Bは真です。",
  "verificationMatches": [
    "statement_b_true: expected=0.855, actual=0.855 ✓"
  ]
}
```

---

### **テスト3: カスタム事実を追加して検証**

**目的:** 動的に事実を追加できることを確認

```bash
curl -X POST http://localhost:8080/api/camel/generic-verify \
  -H "Content-Type: application/json" \
  -d '{
    "query": "カスタム事実を使った推論テスト",
    "ruleFile": "rules/simple-verification-rules.yaml",
    "customFacts": {
      "statement_a_true": [0.95],
      "a_implies_b": [0.98]
    },
    "expectedOutputs": {
      "statement_b_true": 0.931
    },
    "tolerance": 0.1
  }' | jq
```

**期待される結果:**
```json
{
  "success": true,
  "logicallySound": true,
  "validationScore": 1.0,
  "inferredFacts": {
    "statement_b_true": "[[0.931]]"
  }
}
```

---

### **テスト4: 期待値との不一致を検出**

**目的:** 検証が正しく失敗を検出することを確認

```bash
curl -X POST http://localhost:8080/api/camel/generic-verify \
  -H "Content-Type: application/json" \
  -d '{
    "query": "ソクラテスは死ぬか？",
    "ruleFile": "rules/example-rules.yaml",
    "expectedOutputs": {
      "socrates_is_mortal": 0.5
    },
    "tolerance": 0.05
  }' | jq
```

**期待される結果:**
```json
{
  "success": true,
  "logicallySound": false,
  "validationScore": 0.0,
  "verificationMismatches": [
    "socrates_is_mortal: expected=0.500, actual=0.980 (diff=0.480)"
  ]
}
```

---

### **テスト5: ルールファイルなしで検証（既存ルール使用）**

**目的:** 事前に読み込んだルールを使用できることを確認

```bash
# 1. 事前にルールを読み込み
curl -X POST http://localhost:8080/api/rules/load-example

# 2. ルールファイル指定なしで検証
curl -X POST http://localhost:8080/api/camel/generic-verify \
  -H "Content-Type: application/json" \
  -d '{
    "query": "ソクラテスは死ぬか？",
    "expectedOutputs": {
      "socrates_is_mortal": 0.98
    }
  }' | jq
```

**期待される結果:**
```json
{
  "success": true,
  "logicallySound": true,
  "validationScore": 1.0
}
```

---

### **テスト6: 期待される出力なし（推論のみ）**

**目的:** 検証なしで推論だけを実行できることを確認

```bash
curl -X POST http://localhost:8080/api/camel/generic-verify \
  -H "Content-Type: application/json" \
  -d '{
    "query": "ソクラテスは死ぬか？",
    "ruleFile": "rules/example-rules.yaml"
  }' | jq
```

**期待される結果:**
```json
{
  "success": true,
  "logicallySound": true,
  "validationScore": 1.0,
  "verificationMatches": [
    "期待される出力なし - 推論のみ実行"
  ],
  "inferredFacts": {
    "socrates_is_mortal": "[[0.98]]"
  }
}
```

---

### **テスト7: 知識グラフルールでの検証**

**目的:** 異なるルールタイプでも動作することを確認

```bash
curl -X POST http://localhost:8080/api/camel/generic-verify \
  -H "Content-Type: application/json" \
  -d '{
    "query": "太郎の友達の友達は誰？",
    "ruleFile": "rules/knowledge-graph-rules.yaml",
    "expectedOutputs": {
      "friend_of_friend": 0.5
    },
    "tolerance": 0.3
  }' | jq
```

---

### **テスト8: バッチ検証**

**目的:** 複数のクエリを一度に検証できることを確認

```bash
curl -X POST http://localhost:8080/api/camel/batch-generic-verify \
  -H "Content-Type: application/json" \
  -d '[
    {
      "query": "ソクラテスは死ぬか？",
      "ruleFile": "rules/example-rules.yaml",
      "expectedOutputs": {"socrates_is_mortal": 0.98}
    },
    {
      "query": "命題Aが真ならBは真か？",
      "ruleFile": "rules/simple-verification-rules.yaml",
      "expectedOutputs": {"statement_b_true": 0.855}
    }
  ]' | jq
```

---

### **テスト9: 許容誤差の調整**

**目的:** 許容誤差が正しく機能することを確認

```bash
# 許容誤差: 0.01（厳しい）
curl -X POST http://localhost:8080/api/camel/generic-verify \
  -H "Content-Type: application/json" \
  -d '{
    "query": "テスト",
    "ruleFile": "rules/example-rules.yaml",
    "expectedOutputs": {"socrates_is_mortal": 0.98},
    "tolerance": 0.01
  }' | jq

# 許容誤差: 0.5（緩い）
curl -X POST http://localhost:8080/api/camel/generic-verify \
  -H "Content-Type: application/json" \
  -d '{
    "query": "テスト",
    "ruleFile": "rules/example-rules.yaml",
    "expectedOutputs": {"socrates_is_mortal": 0.5},
    "tolerance": 0.5
  }' | jq
```

---

### **テスト10: エラーハンドリング**

**目的:** エラーが適切に処理されることを確認

#### 10-1. 存在しないルールファイル

```bash
curl -X POST http://localhost:8080/api/camel/generic-verify \
  -H "Content-Type: application/json" \
  -d '{
    "query": "テスト",
    "ruleFile": "rules/non-existent.yaml"
  }' | jq
```

**期待される結果:**
```json
{
  "success": false,
  "errorMessage": "検証エラー: リソースが見つかりません: rules/non-existent.yaml"
}
```

#### 10-2. 不正なJSON

```bash
curl -X POST http://localhost:8080/api/camel/generic-verify \
  -H "Content-Type: application/json" \
  -d '{ invalid json }' | jq
```

---

## 📊 テスト結果の確認

### **成功基準**

すべてのテストケースで以下を確認：

1. ✅ `success: true` が返される
2. ✅ `logicallySound` が期待通り
3. ✅ `validationScore` が適切
4. ✅ `verificationMatches` に期待される項目がある
5. ✅ エラーケースで適切なエラーメッセージが返る

### **ログ確認**

アプリケーションのログで以下を確認：

```
INFO  [ai.ten.int.GenericLLMVerifier] (executor-thread-1) === 汎用LLM検証を開始 ===
INFO  [ai.ten.int.GenericLLMVerifier] (executor-thread-1) 質問: ソクラテスは死ぬか？
INFO  [ai.ten.int.GenericLLMVerifier] (executor-thread-1) ルールファイルを読み込み: rules/example-rules.yaml
INFO  [ai.ten.par.RuleLoader] (executor-thread-1) ルール登録完了: 事実2個, ルール1個
INFO  [ai.ten.int.GenericLLMVerifier] (executor-thread-1) 推論エンジンで検証を実行...
INFO  [ai.ten.cor.TensorLogicEngine] (executor-thread-1) === 前向き推論を開始 ===
INFO  [ai.ten.int.GenericLLMVerifier] (executor-thread-1) ✓ 検証成功: socrates_is_mortal
```

---

## 🔍 Swagger UIでのテスト

1. ブラウザで http://localhost:8080/swagger-ui を開く
2. **Camel Integration** セクションを展開
3. `POST /api/camel/generic-verify` を選択
4. **Try it out** をクリック
5. リクエストボディを入力
6. **Execute** をクリック

---

## 🐛 トラブルシューティング

### 問題1: `success: false` が返される

**確認事項:**
- ルールファイルのパスが正しいか
- ルールファイルの YAML 構文が正しいか
- アプリケーションログでエラーメッセージを確認

### 問題2: `logicallySound: false` になる

**確認事項:**
- 期待値が正しいか
- 許容誤差 (`tolerance`) が適切か
- `verificationMismatches` を確認して差分を確認

### 問題3: `inferredFacts` が空

**確認事項:**
- ルールが正しく読み込まれているか確認
  ```bash
  curl http://localhost:8080/api/rules/inspect/rules | jq
  ```
- 事実が登録されているか確認
  ```bash
  curl http://localhost:8080/api/rules/inspect/facts | jq
  ```

---

## 📈 パフォーマンステスト

### 連続100リクエスト

```bash
for i in {1..100}; do
  curl -X POST http://localhost:8080/api/camel/generic-verify \
    -H "Content-Type: application/json" \
    -d '{
      "query": "テスト'$i'",
      "ruleFile": "rules/example-rules.yaml",
      "expectedOutputs": {"socrates_is_mortal": 0.98}
    }' > /dev/null 2>&1
  echo "リクエスト $i 完了"
done
```

---

## 🎯 まとめ

### テスト完了後の確認

- [ ] 全てのテストケースが成功
- [ ] エラーハンドリングが適切
- [ ] ログが正しく出力されている
- [ ] パフォーマンスが許容範囲内
- [ ] ドキュメントと実装が一致

---

**作成日**: 2025年11月5日  
**対応バージョン**: Java 21 + Quarkus 3.6 + Camel 4.x

