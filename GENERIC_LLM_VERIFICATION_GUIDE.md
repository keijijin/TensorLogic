# 汎用LLM検証ガイド

## 🎯 概要

**汎用LLM検証システム**は、外部ルールファイルを使用して、LLMの回答を論理的に検証する機能です。
どのようなルールでも適用可能で、柔軟にカスタマイズできます。

---

## 📍 実装場所

### 1. **Camelルート** (`TensorLogicRoutes.java`)

```java
// Route 8: 汎用LLM検証パイプライン
from("direct:generic-verify")
    .routeId("generic-verify-route")
    .bean(genericVerifier, "verify")
    .marshal().json();
```

**フロー:**
```
HTTP Request
    ↓
Camel Route: direct:generic-verify
    ↓
GenericLLMVerifier.verify()
    ↓
  1. ルールファイルを読み込み（オプション）
  2. LLMに問い合わせ
  3. 事実を抽出・登録
  4. 推論エンジンで前向き推論を実行
  5. 期待される結果と比較
  6. 検証結果を返却
    ↓
JSON Response
```

### 2. **検証ロジック** (`GenericLLMVerifier.java`)

汎用的な検証を行う新しいサービスクラス：

```java
@ApplicationScoped
public class GenericLLMVerifier {
    public GenericVerificationResult verify(GenericVerificationRequest request) {
        // 1. ルールファイルを読み込み
        // 2. LLMに問い合わせ
        // 3. 事実を抽出・登録
        // 4. 推論エンジンで前向き推論
        // 5. 期待値と比較
        // 6. 検証結果を返却
    }
}
```

### 3. **REST API** (`CamelIntegrationResource.java`)

```java
@POST
@Path("/api/camel/generic-verify")
public String genericVerify(GenericVerifyRequest request)
```

---

## 🚀 使い方

### **基本的な使い方**

#### 1. **シンプルな検証（ルールファイル指定）**

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
  }'
```

**レスポンス:**
```json
{
  "success": true,
  "query": "ソクラテスは死ぬか？",
  "llmAnswer": "はい、ソクラテスは人間であり、全ての人間は死ぬため、ソクラテスも死にます。",
  "llmConfidence": 0.95,
  "logicallySound": true,
  "validationScore": 1.0,
  "inferredFacts": {
    "socrates_is_mortal": "[[0.98]]"
  },
  "verificationMatches": [
    "socrates_is_mortal: expected=0.980, actual=0.980 ✓"
  ],
  "verificationMismatches": [],
  "missingExpectedFacts": []
}
```

---

### **高度な使い方**

#### 2. **カスタム事実を指定**

外部ルールファイルに加えて、リクエスト時に事実を動的に追加：

```bash
curl -X POST http://localhost:8080/api/camel/generic-verify \
  -H "Content-Type: application/json" \
  -d '{
    "query": "太郎の友達の友達は誰？",
    "ruleFile": "rules/knowledge-graph-rules.yaml",
    "customFacts": {
      "taro_friend_confidence": [0.9],
      "friend_transitivity": [0.85]
    },
    "expectedOutputs": {
      "friend_of_friend": 0.765
    },
    "tolerance": 0.1
  }'
```

#### 3. **ルールファイルなし（既に登録済みのルールを使用）**

```bash
# 事前にルールを読み込み
curl -X POST http://localhost:8080/api/rules/load-example

# ルールファイル指定なしで検証
curl -X POST http://localhost:8080/api/camel/generic-verify \
  -H "Content-Type: application/json" \
  -d '{
    "query": "ソクラテスは死ぬか？",
    "expectedOutputs": {
      "socrates_is_mortal": 0.98
    }
  }'
```

#### 4. **LLMの確信度を事実として抽出**

```bash
curl -X POST http://localhost:8080/api/camel/generic-verify \
  -H "Content-Type: application/json" \
  -d '{
    "query": "Aは正しいか？",
    "ruleFile": "rules/my-rules.yaml",
    "extractFactsFromLLM": true,
    "expectedOutputs": {
      "conclusion": 0.9
    }
  }'
```

---

## 📊 リクエストパラメータ

### `GenericVerificationRequest`

| パラメータ | 型 | 必須 | デフォルト | 説明 |
|-----------|-----|------|-----------|------|
| `query` | String | ✓ | - | LLMへの質問 |
| `ruleFile` | String | - | null | ルールファイルのパス（`rules/`以下） |
| `customFacts` | Map<String, List<Double>> | - | {} | カスタム事実（動的に追加） |
| `expectedOutputs` | Map<String, Double> | - | {} | 期待される推論結果 |
| `tolerance` | Double | - | 0.05 | 許容誤差 |
| `extractFactsFromLLM` | Boolean | - | false | LLMの確信度を事実として抽出 |

### `customFacts` の形式

```json
{
  "customFacts": {
    "fact_name_1": [1.0],              // 1次元ベクトル
    "fact_name_2": [0.9, 0.8, 0.7],    // 3次元ベクトル
    "confidence_matrix": [0.95, 0.85]  // 2要素のベクトル
  }
}
```

### `expectedOutputs` の形式

```json
{
  "expectedOutputs": {
    "socrates_is_mortal": 0.98,        // 期待値: 0.98
    "friend_of_friend": 0.765          // 期待値: 0.765
  }
}
```

---

## 📈 レスポンスフォーマット

### `GenericVerificationResult`

```json
{
  "success": true,                     // 処理が成功したか
  "query": "ソクラテスは死ぬか？",      // 元の質問
  "llmAnswer": "はい、死にます。",     // LLMの回答
  "llmConfidence": 0.95,               // LLMの確信度
  "reasoningSteps": [                  // LLMの推論ステップ
    "1. ソクラテスは人間である",
    "2. 人間は死ぬ",
    "3. 故にソクラテスは死ぬ"
  ],
  "logicallySound": true,              // 論理的に妥当か
  "validationScore": 1.0,              // 検証スコア（0.0～1.0）
  "inferredFacts": {                   // 推論された事実
    "socrates_is_mortal": "[[0.98]]"
  },
  "verificationMatches": [             // 検証成功した項目
    "socrates_is_mortal: expected=0.980, actual=0.980 ✓"
  ],
  "verificationMismatches": [],        // 検証失敗した項目
  "missingExpectedFacts": [],          // 見つからなかった事実
  "errorMessage": null                 // エラーメッセージ（エラー時のみ）
}
```

---

## 🔍 実用例

### **例1: 医療診断の検証**

```yaml
# rules/medical-diagnosis.yaml
metadata:
  name: "医療診断ルール"

facts:
  - name: has_fever
    tensor:
      type: vector
      shape: [1]
      values: [1.0]
  
  - name: fever_indicates_infection
    tensor:
      type: matrix
      shape: [1, 1]
      values: [[0.85]]

rules:
  - name: diagnose_infection
    inputs: [has_fever, fever_indicates_infection]
    output: likely_infection
    operation: MODUS_PONENS
    enabled: true
```

**検証リクエスト:**
```bash
curl -X POST http://localhost:8080/api/camel/generic-verify \
  -H "Content-Type: application/json" \
  -d '{
    "query": "患者は熱があります。感染症の可能性は？",
    "ruleFile": "rules/medical-diagnosis.yaml",
    "expectedOutputs": {
      "likely_infection": 0.85
    },
    "tolerance": 0.1
  }'
```

---

### **例2: ビジネスルールの検証**

```yaml
# rules/business-rules.yaml
metadata:
  name: "クレジット承認ルール"

facts:
  - name: credit_score_high
    tensor:
      type: vector
      shape: [1]
      values: [1.0]
  
  - name: high_score_approval
    tensor:
      type: matrix
      shape: [1, 1]
      values: [[0.95]]

rules:
  - name: approve_credit
    inputs: [credit_score_high, high_score_approval]
    output: credit_approved
    operation: MODUS_PONENS
    enabled: true
```

**検証リクエスト:**
```bash
curl -X POST http://localhost:8080/api/camel/generic-verify \
  -H "Content-Type: application/json" \
  -d '{
    "query": "このお客様のクレジットスコアは800です。承認すべきですか？",
    "ruleFile": "rules/business-rules.yaml",
    "customFacts": {
      "credit_score_high": [1.0],
      "income_sufficient": [0.9]
    },
    "expectedOutputs": {
      "credit_approved": 0.95
    }
  }'
```

---

### **例3: 法律判断の検証**

```yaml
# rules/legal-reasoning.yaml
metadata:
  name: "契約有効性判断"

facts:
  - name: signed_by_both_parties
    tensor:
      type: vector
      shape: [1]
      values: [1.0]
  
  - name: signature_makes_valid
    tensor:
      type: matrix
      shape: [1, 1]
      values: [[0.99]]

rules:
  - name: contract_validity
    inputs: [signed_by_both_parties, signature_makes_valid]
    output: contract_valid
    operation: MODUS_PONENS
    enabled: true
```

---

## 🔄 従来の方法との比較

### **従来の方法（ハードコード）**

```java
// LLMTensorLogicIntegration.java
public VerifiedReasoningResult verifyLLMReasoning(String query) {
    // ❌ ソクラテスの三段論法に固定
    INDArray socratesIsHuman = Nd4j.create(new double[]{0.95});
    INDArray humanIsMortal = Nd4j.create(new double[][]{{0.98}});
    // ...
}
```

**問題点:**
- ✗ 他のドメインに適用できない
- ✗ ルール変更のたびにコード修正が必要
- ✗ 柔軟性がない

### **新しい方法（汎用化）**

```bash
# ✓ どんなルールでも適用可能
curl -X POST http://localhost:8080/api/camel/generic-verify \
  -d '{ "query": "...", "ruleFile": "rules/any-rules.yaml" }'
```

**利点:**
- ✓ どのようなドメインにも適用可能
- ✓ ルールはYAMLファイルで管理
- ✓ コード変更不要
- ✓ 動的にルールを切り替え可能

---

## 🎨 Camelルートの拡張

### **カスタムルートの追加**

```java
// TensorLogicRoutes.java に追加

/**
 * 複数のルールファイルで検証して比較
 */
from("direct:compare-rules")
    .routeId("compare-rules-route")
    .log("ルール比較検証を開始")
    .multicast()
        .parallelProcessing()
        .to("direct:verify-rule-a")
        .to("direct:verify-rule-b")
        .to("direct:verify-rule-c")
    .end()
    .aggregate(constant(true), new CompareResultsAggregator())
    .completionSize(3)
    .log("ルール比較完了");
```

---

## 🔧 トラブルシューティング

### **問題1: ルールファイルが見つからない**

```json
{
  "success": false,
  "errorMessage": "検証エラー: リソースが見つかりません: rules/my-rules.yaml"
}
```

**解決方法:**
- ルールファイルが `src/main/resources/rules/` に存在することを確認
- パスが正しいか確認（`rules/` で始まる相対パス）

### **問題2: 期待される出力が見つからない**

```json
{
  "logicallySound": false,
  "validationScore": 0.0,
  "missingExpectedFacts": ["expected_fact_name"]
}
```

**解決方法:**
- ルールの `output` が正しく設定されているか確認
- 推論エンジンが正しく動作しているか確認
- ログを確認: `LOG.info("推論完了: {}個の新しい事実を推論", inferredFacts.size())`

### **問題3: 検証が常に失敗する**

```json
{
  "verificationMismatches": [
    "fact_name: expected=0.950, actual=0.850 (diff=0.100)"
  ]
}
```

**解決方法:**
- `tolerance` パラメータを調整（デフォルト: 0.05）
- 期待値が現実的か確認
- ルールのテンソル値を確認

---

## 📚 関連ドキュメント

- [RULE_DSL_GUIDE.md](./RULE_DSL_GUIDE.md) - ルールDSLの詳細
- [RULE_AND_TENSOR_GUIDE.md](./RULE_AND_TENSOR_GUIDE.md) - テンソル変換の仕組み
- [RULE_INSPECTION_GUIDE.md](./RULE_INSPECTION_GUIDE.md) - ルールの確認方法
- [JAVA_IMPLEMENTATION.md](./JAVA_IMPLEMENTATION.md) - 全体アーキテクチャ

---

## 🎯 まとめ

### **汎用LLM検証システムの特徴**

| 特徴 | 説明 |
|------|------|
| **柔軟性** | どのようなルールでも適用可能 |
| **分離** | ルールとコードが分離 |
| **拡張性** | 新しいルールを追加しやすい |
| **検証可能** | 詳細な検証結果を提供 |
| **統合** | Camelの強力な機能を活用 |

### **使用シーン**

1. **LLMの回答を論理的に検証** - ハルシネーション防止
2. **ドメイン固有のルール適用** - 医療、法律、金融など
3. **複数のルールセットで比較** - ベストプラクティスの発見
4. **リアルタイム検証** - ストリーミングLLM出力の検証
5. **バッチ処理** - 大量のクエリを効率的に検証

---

**作成日**: 2025年11月5日  
**対応バージョン**: Java 21 + Quarkus 3.6 + Camel 4.x

