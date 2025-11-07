# LLM検証アウトプット解説ガイド

## 📊 アウトプットの全体構造

LLM検証システムから返されるJSONレスポンスの詳細解説です。

---

## 🔍 出力例と各フィールドの意味

### **完全な出力例**

```json
{
  "answer": "はい、ソクラテスは死にます。",
  "llmConfidence": 0.9,
  "reasoningSteps": [
    "1. ソクラテスは人間です。",
    "2. すべての人間は死にます。",
    "3. したがって、ソクラテスは死にます。"
  ],
  "isLogicallySound": true,
  "validationConfidence": 0.982,
  "validationDetails": {
    "isValid": true,
    "confidence": 0.982,
    "expectedValue": "0.9000",
    "actualValue": "0.8820",
    "meanError": 0.018,
    "maxError": 0.018
  }
}
```

---

## 📖 フィールドの詳細解説

### **1️⃣ LLMの回答部分**

#### `answer` (String)
**意味**: LLMが生成した自然言語の回答

```json
"answer": "はい、ソクラテスは死にます。"
```

- LLMが質問に対して生成した答え
- 自然言語での説明

---

#### `llmConfidence` (Double: 0.0～1.0)
**意味**: LLMの確信度

```json
"llmConfidence": 0.9
```

| 値の範囲 | 意味 |
|---------|------|
| 0.9～1.0 | 非常に確信している |
| 0.7～0.9 | かなり確信している |
| 0.5～0.7 | やや確信している |
| 0.3～0.5 | 不確実 |
| 0.0～0.3 | ほとんど確信していない |

**この例**: LLMは90%の確信度で「ソクラテスは死ぬ」と判断

---

#### `reasoningSteps` (Array of Strings)
**意味**: LLMの推論ステップ

```json
"reasoningSteps": [
  "1. ソクラテスは人間です。",
  "2. すべての人間は死にます。",
  "3. したがって、ソクラテスは死にます。"
]
```

- LLMがどのように推論したかのステップバイステップの説明
- 論理的な思考過程を可視化
- デバッグや説明可能性（Explainability）に有用

---

### **2️⃣ Tensor Logic検証結果**

#### `isLogicallySound` (Boolean)
**意味**: LLMの推論が論理的に妥当か

```json
"isLogicallySound": true
```

| 値 | 意味 | 解釈 |
|----|------|------|
| `true` | 論理的に妥当 | ✅ LLMの回答は論理規則と一致 |
| `false` | 論理的に不適切 | ⚠️ LLMの回答に論理的な問題あり |

**この例**: `true` = ソクラテスの三段論法は論理的に正しい

---

#### `validationConfidence` (Double: 0.0～1.0)
**意味**: 検証の確信度（Tensor Logicでの計算結果）

```json
"validationConfidence": 0.982
```

**計算方法**:
```
LLMの確信度: 0.9
ルールの確実性: 0.98 (人間 → 死ぬ)
検証確信度: 0.9 × 0.98 = 0.882 → 正規化して 0.982
```

**LLM確信度 vs 検証確信度**:
- **LLM確信度**: LLMが自己評価した確信度
- **検証確信度**: Tensor Logicで論理的に計算した確信度

この例では、Tensor Logicでの検証結果の方が高い確信度を示しています。

---

### **3️⃣ 検証の詳細: `validationDetails`**

#### **改善前（問題のある出力）** ❌

```json
"validationDetails": {
  "expected": {
    "columnVector": false,
    "rowVector": false,
    "id": 19,
    "sparse": false,
    // ... 100行以上のメタデータ
  },
  "actual": {
    "columnVector": false,
    // ... 同様のメタデータ
  }
}
```

**問題点**:
- ND4JのINDArrayオブジェクトがそのままJSON化
- テンソルの実際の値が表示されない
- 内部実装の詳細が露出
- 人間が読めない

---

#### **改善後（読みやすい出力）** ✅

```json
"validationDetails": {
  "isValid": true,
  "confidence": 0.982,
  "expectedValue": "0.9000",
  "actualValue": "0.8820",
  "meanError": 0.018,
  "maxError": 0.018
}
```

**各フィールドの意味**:

| フィールド | 型 | 意味 | この例 |
|-----------|-----|------|--------|
| `isValid` | Boolean | 検証が成功したか | `true` |
| `confidence` | Double | 検証の確信度 | 0.982 (98.2%) |
| `expectedValue` | String | 期待される値 | "0.9000" |
| `actualValue` | String | 実際の計算値 | "0.8820" |
| `meanError` | Double | 平均誤差 | 0.018 |
| `maxError` | Double | 最大誤差 | 0.018 |

---

## 📈 実際の計算過程

### **ソクラテスの三段論法の例**

```
前提1: socrates_is_human = [1.0]
       「ソクラテスは人間である」（100%確実）

前提2: human_is_mortal = [[0.98]]
       「人間ならば死ぬ」（98%確実）

計算: socrates_is_mortal = socrates_is_human ⊗ human_is_mortal
                          = [1.0] × [[0.98]]
                          = [0.98]

期待値: 0.9000 (LLMの確信度)
実際値: 0.8820 (テンソル計算の結果)
誤差:   0.018  (許容範囲内)
```

### **検証の流れ**

```
Step 1: LLMに質問
  └→ 「ソクラテスは死にますか？」

Step 2: LLMの回答を取得
  ├→ 回答: "はい、死にます"
  └→ 確信度: 0.9

Step 3: Tensor Logicで検証
  ├→ ルール読み込み: socrates_is_human, human_is_mortal
  ├→ 推論実行: forwardChain()
  └→ 結果: 0.882

Step 4: 期待値と比較
  ├→ 期待値: 0.9
  ├→ 実際値: 0.882
  ├→ 誤差: 0.018 < 許容誤差(0.2)
  └→ 検証: ✓ 合格

Step 5: 結果を返却
  └→ isLogicallySound: true
```

---

## 🎨 異なるシナリオでの出力例

### **シナリオ1: 検証成功（高確信度）**

```json
{
  "answer": "はい、正しいです。",
  "llmConfidence": 0.95,
  "isLogicallySound": true,
  "validationConfidence": 0.99,
  "validationDetails": {
    "isValid": true,
    "expectedValue": "0.9500",
    "actualValue": "0.9450",
    "meanError": 0.005
  }
}
```

**解釈**: 
- ✅ LLMとTensor Logicの結果がほぼ一致
- ✅ 誤差が非常に小さい (0.005)
- ✅ 高い信頼性

---

### **シナリオ2: 検証失敗（論理的矛盾）**

```json
{
  "answer": "いいえ、ソクラテスは死にません。",
  "llmConfidence": 0.6,
  "isLogicallySound": false,
  "validationConfidence": 0.1,
  "validationDetails": {
    "isValid": false,
    "expectedValue": "0.6000",
    "actualValue": "0.9800",
    "meanError": 0.380
  }
}
```

**解釈**:
- ⚠️ LLMの回答が論理規則と矛盾
- ⚠️ 誤差が大きい (0.380 > 許容誤差)
- ⚠️ LLMがハルシネーション（幻覚）を起こしている可能性

---

### **シナリオ3: 不確実な回答**

```json
{
  "answer": "おそらく正しいと思います。",
  "llmConfidence": 0.5,
  "isLogicallySound": true,
  "validationConfidence": 0.75,
  "validationDetails": {
    "isValid": true,
    "expectedValue": "0.5000",
    "actualValue": "0.4800",
    "meanError": 0.020
  }
}
```

**解釈**:
- 🤔 LLMの確信度が低い (0.5)
- ✅ しかし論理的には整合性がある
- 💡 Tensor Logicの方が高い確信度 (0.75)

---

## 🔧 `@JsonIgnore` による改善

### **実装の変更点**

#### **変更前**

```java
public record ValidationResult(
    boolean isValid,
    double confidence,
    INDArray expected,      // ⚠️ JSON化されてしまう
    INDArray actual,        // ⚠️ JSON化されてしまう
    INDArray error          // ⚠️ JSON化されてしまう
) {}
```

#### **変更後** ✅

```java
public record ValidationResult(
    boolean isValid,
    double confidence,
    @JsonIgnore INDArray expected,      // ✅ JSON化から除外
    @JsonIgnore INDArray actual,        // ✅ JSON化から除外
    @JsonIgnore INDArray error          // ✅ JSON化から除外
) {
    // 代わりに人間が読みやすいメソッドを提供
    public String getExpectedValue() {
        return formatTensor(expected);
    }
    
    public String getActualValue() {
        return formatTensor(actual);
    }
    
    public double getMeanError() {
        return error.meanNumber().doubleValue();
    }
}
```

---

## 📊 各フィールドの活用方法

### **ダッシュボード表示**

```javascript
// フロントエンドでの表示例
function displayVerification(result) {
    // LLMの回答
    document.getElementById('answer').textContent = result.answer;
    
    // 確信度バー
    const confidenceBar = document.getElementById('confidence-bar');
    confidenceBar.style.width = (result.llmConfidence * 100) + '%';
    
    // 検証結果のバッジ
    const badge = document.getElementById('validation-badge');
    if (result.isLogicallySound) {
        badge.className = 'badge badge-success';
        badge.textContent = '✓ 論理的に妥当';
    } else {
        badge.className = 'badge badge-warning';
        badge.textContent = '⚠ 論理的な問題';
    }
    
    // 検証スコア
    document.getElementById('validation-score').textContent = 
        (result.validationConfidence * 100).toFixed(1) + '%';
}
```

### **ログ分析**

```python
# Python でのログ分析例
import json

def analyze_verification_logs(log_file):
    results = []
    with open(log_file) as f:
        for line in f:
            data = json.loads(line)
            results.append({
                'llm_confidence': data['llmConfidence'],
                'validation_confidence': data['validationConfidence'],
                'is_sound': data['isLogicallySound'],
                'error': data['validationDetails']['meanError']
            })
    
    # 統計分析
    avg_llm_conf = sum(r['llm_confidence'] for r in results) / len(results)
    avg_val_conf = sum(r['validation_confidence'] for r in results) / len(results)
    error_rate = sum(1 for r in results if not r['is_sound']) / len(results)
    
    print(f"LLM平均確信度: {avg_llm_conf:.2f}")
    print(f"検証平均確信度: {avg_val_conf:.2f}")
    print(f"論理エラー率: {error_rate:.2%}")
```

---

## 🎯 まとめ

### **重要なポイント**

1. **LLM確信度 vs 検証確信度**
   - LLM確信度: LLMの自己評価
   - 検証確信度: 論理的に計算された確実性
   - 検証確信度の方が信頼できる

2. **`isLogicallySound` の判定**
   - `true`: LLMの回答は論理規則と一致
   - `false`: 論理的な矛盾を検出

3. **`validationDetails` の活用**
   - 期待値と実際値の比較
   - 誤差の確認
   - デバッグとトラブルシューティング

4. **改善されたJSON出力**
   - `@JsonIgnore` でINDArrayを除外
   - 人間が読みやすい文字列形式に変換
   - 必要な情報のみを返却

---

## 📚 関連ドキュメント

- [GENERIC_LLM_VERIFICATION_GUIDE.md](./GENERIC_LLM_VERIFICATION_GUIDE.md) - 汎用検証システム
- [RULE_INSPECTION_GUIDE.md](./RULE_INSPECTION_GUIDE.md) - ルール確認方法
- [JAVA_IMPLEMENTATION.md](./JAVA_IMPLEMENTATION.md) - システム全体のアーキテクチャ

---

**作成日**: 2025年11月5日  
**対応バージョン**: Java 21 + Quarkus 3.6

