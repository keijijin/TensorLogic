# 🔄 質問からTensor Logicへの変換フロー

## 📊 **概要**

TensorLogicEngineに質問を投げる際、**2つのアプローチ**があります：

1. **アプローチ1**: LLMを使わずに**事前定義ルール**を使用
2. **アプローチ2**: **LLMが推論を生成**し、それをTensor Logic化

---

## 🎯 **重要な原則**

### **TensorLogicEngineは自然言語を理解しない**

```java
// TensorLogicEngineが受け取れるもの
engine.addFact("socrates_is_human", Nd4j.create(new double[]{0.98}));
engine.addRule(new Rule("human_is_mortal", ...));

// TensorLogicEngineが受け取れないもの ❌
engine.query("ソクラテスは死にますか？"); // こんなメソッドは存在しない
```

**TensorLogicEngineは純粋な数値演算エンジン**であり、以下のみを扱います：
- **INDArray**: テンソル（多次元配列）
- **Rule**: 論理ルール（演算の定義）

---

## 📋 **アプローチ1: 事前定義ルールを使用（LLM不使用）**

### **フロー図**

```
┌─────────────────┐
│ ユーザーの質問   │
│ "18歳で年収300万円" │
└────────┬────────┘
         │
         ▼
┌─────────────────────────────┐
│ 1. YAMLルールファイル        │
│    rules/loan-approval.yaml  │
│    - 事実: applicant_age     │
│    - ルール: is_adult        │
└────────┬────────────────────┘
         │
         ▼
┌─────────────────────────────┐
│ 2. RuleParser               │
│    YAMLを読み込み            │
└────────┬────────────────────┘
         │
         ▼
┌─────────────────────────────┐
│ 3. TensorConverter          │
│    FactsとRulesをINDArrayに変換 │
│    - applicant_age → [1.0]  │
│    - ルールを演算に変換       │
└────────┬────────────────────┘
         │
         ▼
┌─────────────────────────────┐
│ 4. TensorLogicEngine        │
│    - addFact()でテンソル登録  │
│    - addRule()でルール登録    │
│    - forwardChain()で推論実行 │
└────────┬────────────────────┘
         │
         ▼
┌─────────────────────────────┐
│ 5. 推論結果                  │
│    loan_approved: 0.9       │
└─────────────────────────────┘
```

### **具体例**

#### **ステップ1: YAMLルール定義**

```yaml
# rules/loan-approval.yaml
facts:
  - name: applicant_age
    tensor:
      type: scalar
      values: [1.0]  # 18歳（成人）
      confidence: 1.0
  
  - name: applicant_income
    tensor:
      values: [0.95]  # 年収300万円以上
      confidence: 0.95

rules:
  - name: determine_adult_status
    inputs: [applicant_age, age_implies_adult]
    output: is_adult
    operation: MODUS_PONENS
```

#### **ステップ2-3: 変換**

```java
// RuleParserとTensorConverterが自動的に実行
RuleDefinition definition = parser.parseFile("rules/loan-approval.yaml");
Map<String, INDArray> tensors = converter.convertAllFacts(definition);

// 結果:
// tensors.get("applicant_age") → INDArray[1.0]
// tensors.get("applicant_income") → INDArray[0.95]
```

#### **ステップ4: TensorLogicEngineに登録**

```java
// 事実を登録
for (Map.Entry<String, INDArray> entry : tensors.entrySet()) {
    engine.addFact(entry.getKey(), entry.getValue());
}

// ルールを登録
List<Rule> rules = parser.convertAllRules(definition);
for (Rule rule : rules) {
    engine.addRule(rule);
}

// 推論実行
Map<String, INDArray> results = engine.forwardChain();
// 結果: loan_approved → 0.9
```

### **このアプローチの特徴**

- ✅ **LLM不要**: OpenAI APIキー不要
- ✅ **高速**: ネットワーク通信なし
- ✅ **決定論的**: 同じ入力で常に同じ結果
- ✅ **コスト0**: API課金なし
- ❌ **柔軟性低**: 事前にルールを定義する必要がある

---

## 📋 **アプローチ2: LLMが推論を生成してTensor Logic化**

### **フロー図**

```
┌─────────────────┐
│ ユーザーの質問   │
│ "ソクラテスは死んでいますか？" │
└────────┬────────┘
         │
         ▼
┌─────────────────────────────┐
│ 1. LLMService               │
│    OpenAI GPTに質問を送信    │
│    "段階的に推論してください" │
└────────┬────────────────────┘
         │
         ▼
┌─────────────────────────────┐
│ 2. LLMの推論結果            │
│    ステップ1: ソクラテスは    │
│    古代ギリシャの哲学者(100%) │
│    ステップ2: 人間の寿命は    │
│    80-100年(95%)            │
│    ステップ3: 2000年以上     │
│    経過(100%)               │
│    結論: 死んでいる(100%)    │
└────────┬────────────────────┘
         │
         ▼
┌─────────────────────────────┐
│ 3. LLMReasoningParser       │
│    自然言語 → Tensor Logic   │
│    - "ソクラテスは哲学者"     │
│      → fact_step_1: [1.0]   │
│    - "人間の寿命は80-100年"  │
│      → fact_step_2: [0.95]  │
│    - 推論ルールを生成        │
└────────┬────────────────────┘
         │
         ▼
┌─────────────────────────────┐
│ 4. TensorLogicEngine        │
│    - Facts登録: fact_step_1,2,3 │
│    - Rule登録: final_conclusion │
│    - backwardChain()で検証   │
└────────┬────────────────────┘
         │
         ▼
┌─────────────────────────────┐
│ 5. 検証結果                  │
│    LLM確信度: 0.80          │
│    Tensor確信度: 0.95       │
│    差異: 0.15               │
└─────────────────────────────┘
```

### **具体例**

#### **ステップ1-2: LLMに質問**

```java
// LLMServiceが処理
LLMReasoningResult llmResult = llmService.queryWithDetailedReasoning(
    "ソクラテスは死んでいますか？"
);

// 結果:
// llmResult.reasoningSteps = [
//   "ステップ1: ソクラテスは古代ギリシャの哲学者で、紀元前469年に生まれました。 (確信度: 100%)",
//   "ステップ2: 人間は平均的に80年から100年程度しか生きられないといわれています。 (確信度: 95%)",
//   "ステップ3: ソクラテスの生から現代までの時間は、2000年以上経過しています。 (確信度: 100%)",
//   ...
// ]
```

#### **ステップ3: LLMReasoningParserが変換**

```java
// LLMの自然言語をTensor Logicに変換
RuleDefinition tensorLogicDef = parser.parseToTensorLogic(llmResult);

// 生成されるFacts:
// fact_step_1: 
//   description: "ソクラテスは古代ギリシャの哲学者..."
//   confidence: 1.0
//   tensor: [1.0]
//
// fact_step_2:
//   description: "人間は平均的に80年から100年程度..."
//   confidence: 0.95
//   tensor: [0.95]
//
// fact_step_3:
//   description: "ソクラテスの生から現代までの時間..."
//   confidence: 1.0
//   tensor: [1.0]

// 生成されるRules:
// final_conclusion_rule:
//   inputs: [fact_step_2, fact_step_1]  // 最も不確実な2つ
//   output: final_conclusion
//   operation: CONJUNCTION  // min操作
```

**変換ロジックの詳細**:

```java
// LLMReasoningParser.java の主要ロジック

// 1. 確信度を抽出
double confidence = extractConfidence(step);
// "確信度: 95%" → 0.95

// 2. Factを生成
RuleDefinition.Fact fact = new RuleDefinition.Fact(
    "fact_step_" + i,
    cleanStatement(step),  // "(確信度: 95%)"を除去
    "LLM推論ステップ" + i,
    new RuleDefinition.Tensor(
        "vector",
        List.of(1),
        List.of(confidence),  // 確信度をテンソル値に
        confidence,
        null
    )
);

// 3. 最終結論のルールを生成
// 最も不確実な2つのFactsを入力として使用
Rule rule = new Rule(
    namespace,
    List.of("fact_step_2", "fact_step_1"),  // 最小確信度の2つ
    "final_conclusion",
    Rule.Operation.CONJUNCTION  // min(0.95, 1.0) = 0.95
);
```

#### **ステップ4: TensorLogicEngineで検証**

```java
// Factsを登録
engine.clear();
for (RuleDefinition.Fact fact : tensorLogicDef.facts()) {
    List<Double> values = (List<Double>) fact.tensor().values();
    engine.addFact(
        fact.name(),
        Nd4j.create(values.stream().mapToDouble(Double::doubleValue).toArray())
    );
}

// Rulesを登録
for (RuleDefinition.RuleSpec ruleSpec : tensorLogicDef.rules()) {
    Rule rule = new Rule(
        tensorLogicDef.metadata().namespace(),
        ruleSpec.inputs(),
        ruleSpec.output(),
        Rule.Operation.valueOf(ruleSpec.operation())
    );
    engine.addRule(rule);
}

// 後向き推論で検証
BackwardChainingResult verification = engine.backwardChain(
    "final_conclusion",
    tensorLogicDef.metadata().namespace()
);

// 結果:
// verification.success() → true
// verification.getGoalConfidence() → 0.95
```

### **このアプローチの特徴**

- ✅ **柔軟性**: どんな質問にも対応可能
- ✅ **自動変換**: ルールを事前定義不要
- ✅ **検証可能**: LLMの推論を論理的に検証
- ✅ **説明可能**: 推論パスを追跡可能
- ❌ **LLM必要**: OpenAI APIキーが必要
- ❌ **コスト**: API呼び出しに課金
- ❌ **非決定論的**: LLMの出力は毎回異なる可能性

---

## 🔀 **2つのアプローチの比較**

| 特徴 | アプローチ1（事前定義） | アプローチ2（LLM変換） |
|------|----------------------|---------------------|
| **LLM使用** | ❌ 不要 | ✅ 使用 |
| **柔軟性** | ⭐⭐ 事前定義のみ | ⭐⭐⭐⭐⭐ 任意の質問 |
| **速度** | ⭐⭐⭐⭐⭐ 即座 | ⭐⭐ 10-15秒 |
| **コスト** | ⭐⭐⭐⭐⭐ 無料 | ⭐⭐ API課金 |
| **決定論性** | ⭐⭐⭐⭐⭐ 常に同じ | ⭐⭐ 毎回異なる可能性 |
| **説明可能性** | ⭐⭐⭐⭐ ルールが明確 | ⭐⭐⭐⭐⭐ 推論パス付き |
| **用途** | ビジネスルール検証 | 研究、実験、探索 |

---

## 🛣️ **実際のエンドポイント**

### **アプローチ1: 事前定義ルール**

#### **エンドポイント**: `/api/verify/simple`

```bash
curl -X POST http://localhost:8080/api/verify/simple \
  -H 'Content-Type: application/json' \
  -d '{
    "query": "18歳で年収300万円の申請者は融資を受けられますか？",
    "ruleFile": "rules/loan-approval-from-drd.yaml",
    "namespace": "loan-approval"
  }'
```

**処理フロー**:
1. YAMLルールファイルを読み込み
2. TensorConverterで変換
3. TensorLogicEngineで推論
4. 結果を返却

**LLMは使われない**: ルールファイルのみ使用

---

### **アプローチ2: LLM推論のTensor Logic化**

#### **エンドポイント**: `/api/llm/reasoning-to-tensor/analyze`

```bash
curl -X POST http://localhost:8080/api/llm/reasoning-to-tensor/analyze \
  -H 'Content-Type: application/json' \
  -d '{
    "query": "ソクラテスは死んでいますか？"
  }'
```

**処理フロー**:
1. ✅ **LLMに質問** (`LLMService`)
2. ✅ **LLMが推論ステップを生成**
3. ✅ **LLMReasoningParserが変換** (自然言語 → Tensor Logic)
4. ✅ **TensorLogicEngineで検証**
5. ✅ **LLMとTensor Logicの確信度を比較**

**LLMが使われる**: 推論の生成と変換

---

## 🔬 **LLMReasoningParserの変換ロジック**

### **主要メソッド**

```java
public class LLMReasoningParser {
    
    /**
     * LLM推論をTensor Logic化
     */
    public RuleDefinition parseToTensorLogic(LLMReasoningResult llmResult) {
        // 1. メタデータ生成
        Metadata metadata = createMetadata(llmResult);
        
        // 2. 推論ステップをパース
        List<Fact> facts = new ArrayList<>();
        List<RuleSpec> rules = new ArrayList<>();
        parseReasoningSteps(llmResult.reasoningSteps(), facts, rules);
        
        // 3. デフォルトルールを追加（必要な場合）
        if (rules.isEmpty() && facts.size() >= 2) {
            rules.add(createDefaultRule(facts));
        }
        
        // 4. 期待結果を生成
        List<ExpectedResult> expectedResults = createExpectedResults(llmResult);
        
        return new RuleDefinition(metadata, List.of(), facts, rules, expectedResults);
    }
    
    /**
     * 確信度を抽出: "確信度: 95%" → 0.95
     */
    private double extractConfidence(String text) {
        Pattern pattern = Pattern.compile("確信度[：:][\\s]*([0-9]+(?:\\.[0-9]+)?)\\s*%");
        Matcher matcher = pattern.matcher(text);
        
        if (matcher.find()) {
            return Double.parseDouble(matcher.group(1)) / 100.0;
        }
        
        // 英語パターン
        pattern = Pattern.compile("confidence[：:][\\s]*([0-9]+(?:\\.[0-9]+)?)\\s*%");
        matcher = pattern.matcher(text);
        
        if (matcher.find()) {
            return Double.parseDouble(matcher.group(1)) / 100.0;
        }
        
        return 0.9; // デフォルト
    }
    
    /**
     * デフォルトルール生成
     * 最も不確実な2つのFactsを入力として使用
     */
    private RuleSpec createDefaultRule(List<Fact> facts) {
        // 確信度でソート（昇順）
        List<Fact> sortedFacts = facts.stream()
            .sorted(Comparator.comparingDouble(f -> f.tensor().confidence()))
            .limit(2)
            .toList();
        
        return new RuleSpec(
            "final_conclusion_rule",
            "最終的な結論を導出（全Factsの最小確信度を反映）",
            "LLM推論",
            sortedFacts.stream().map(Fact::name).toList(),
            "final_conclusion",
            "CONJUNCTION",  // min操作
            999,
            true
        );
    }
}
```

---

## 📊 **実例: "ソクラテスは死んでいますか？"**

### **入力（LLMの出力）**

```json
{
  "reasoningSteps": [
    "ステップ1: ソクラテスは古代ギリシャの哲学者で、紀元前469年に生まれました。 (確信度: 100%)",
    "ステップ2: 人間は平均的に80年から100年程度しか生きられないといわれています。 (確信度: 95%)",
    "ステップ3: ソクラテスの生から現代までの時間は、2000年以上経過しています。 (確信度: 100%)"
  ],
  "confidence": 0.8
}
```

### **変換後（Tensor Logic形式）**

```yaml
facts:
  - name: fact_step_1
    description: "ソクラテスは古代ギリシャの哲学者で、紀元前469年に生まれました。"
    confidence: 1.0
    tensor: [1.0]
  
  - name: fact_step_2
    description: "人間は平均的に80年から100年程度しか生きられない"
    confidence: 0.95
    tensor: [0.95]
  
  - name: fact_step_3
    description: "ソクラテスの生から現代までの時間は、2000年以上経過"
    confidence: 1.0
    tensor: [1.0]

rules:
  - name: final_conclusion_rule
    inputs: [fact_step_2, fact_step_1]  # 最も不確実な2つ
    output: final_conclusion
    operation: CONJUNCTION  # min(0.95, 1.0) = 0.95
```

### **TensorLogicEngineでの計算**

```java
// 登録されたFacts
facts.get("fact_step_1") → [1.0]
facts.get("fact_step_2") → [0.95]
facts.get("fact_step_3") → [1.0]

// ルール適用
final_conclusion = CONJUNCTION(fact_step_2, fact_step_1)
                 = min(0.95, 1.0)
                 = 0.95

// 比較
LLM確信度: 0.80
Tensor Logic確信度: 0.95
差異: 0.15 (15%)
```

---

## 🎯 **まとめ**

### **質問に答えると**

> **TensorLogicEngineに質問を投げる場合、そこで使われる概念はLLMを使って変換されているのですか？**

**回答**:

1. **TensorLogicEngineは自然言語を理解しない**
   - 受け取るのは`INDArray`（テンソル）と`Rule`（ルール）のみ

2. **2つのアプローチがある**:
   - **アプローチ1**: LLMを使わず、YAMLで事前定義されたルールを使用
   - **アプローチ2**: LLMに推論させて、その結果を`LLMReasoningParser`がTensor Logic形式に変換

3. **アプローチ2の場合**:
   - ✅ LLMが自然言語で推論を生成
   - ✅ `LLMReasoningParser`が推論ステップを解析
   - ✅ 確信度を抽出（"確信度: 95%" → 0.95）
   - ✅ Facts（事実）とテンソル値に変換
   - ✅ Rules（推論ルール）を生成
   - ✅ TensorLogicEngineで数値演算として検証

4. **変換の本質**:
   ```
   自然言語の推論 → 形式的な論理 → テンソル演算
   "ソクラテスは人間"  → fact_step_1  → [0.98]
   ```

---

## 📚 **関連ドキュメント**

- [LLM推論のTensor Logic化ガイド](./LLM_TO_TENSOR_LOGIC_GUIDE.md)
- [Tensor Logicエンジンガイド](./TENSOR_LOGIC_ENGINE_GUIDE.md)
- [LLM推論検証レポート](../reports/LLM_REASONING_VERIFICATION_REPORT.md)
- [実装詳細: LLM統合](../implementation/LLM_INTEGRATION.md)

---

**作成日**: 2025年11月7日  
**プロジェクト**: TensorLogic 1.0.0  
**キーコンポーネント**: LLMReasoningParser, TensorLogicEngine, LLMService

