# LLM推論結果のTensor Logic化ガイド

**作成日:** 2025年11月6日  
**バージョン:** 1.0

---

## 📚 目次

1. [コンセプト](#コンセプト)
2. [なぜLLM推論をTensor Logic化するのか](#なぜllm推論をtensor-logic化するのか)
3. [LLMの推論ステップとTensor Logicの対応](#llmの推論ステップとtensor-logicの対応)
4. [実装アプローチ](#実装アプローチ)
5. [具体例](#具体例)
6. [実装ステップ](#実装ステップ)
7. [ユースケース](#ユースケース)

---

## 🎯 コンセプト

### **基本的なアイデア**

```
LLMの推論プロセス → Tensor Logicの形式化
  
  [質問] 
    ↓
  [LLM推論]
    ├─ 推論ステップ1
    ├─ 推論ステップ2
    └─ 結論
    ↓
  [Tensor Logic化]
    ├─ Facts (推論中の命題)
    ├─ Rules (推論の論理構造)
    └─ 検証
```

### **目的**

1. **透明性**: LLMの推論プロセスを可視化
2. **検証可能性**: 論理的整合性を形式的に検証
3. **説明可能性**: なぜその結論に至ったかを明確化
4. **ハルシネーション検出**: 論理的矛盾を検出

---

## 💡 なぜLLM推論をTensor Logic化するのか

### **LLMの課題**

| 課題 | 説明 |
|------|------|
| **ブラックボックス** | 内部の推論プロセスが不透明 |
| **ハルシネーション** | もっともらしいが誤った情報を生成 |
| **論理的整合性** | 推論ステップ間の矛盾を検出困難 |
| **確信度の曖昧さ** | 「おそらく」「たぶん」などの定量化が困難 |

### **Tensor Logicの強み**

| 強み | 説明 |
|------|------|
| **形式的検証** | 論理的に正しいかを数学的に検証 |
| **確信度の定量化** | 0.0〜1.0で明確に表現 |
| **推論の追跡** | ステップごとの計算を追跡可能 |
| **矛盾検出** | 論理的矛盾を自動検出 |

### **組み合わせの価値**

```
LLM（柔軟な推論）+ Tensor Logic（厳密な検証）
= ハイブリッドAIシステム
```

---

## 🔄 LLMの推論ステップとTensor Logicの対応

### **例: "ソクラテスは死にますか？"**

#### **LLMの推論出力**

```
推論ステップ1: ソクラテスは人間です。
確信度: 100%

推論ステップ2: すべての人間は死にます。
確信度: 98%

推論ステップ3: したがって、ソクラテスは死にます。
確信度: 98%
```

#### **Tensor Logic化**

```yaml
metadata:
  name: "LLM推論結果: ソクラテスの死"
  source: "LLM"
  timestamp: "2025-11-06T15:00:00"
  namespace: "llm-reasoning"

facts:
  # ステップ1から抽出
  - name: socrates_is_human
    description: "ソクラテスは人間である"
    source: "LLM推論ステップ1"
    values: [1.0]  # 確信度100%
  
  # ステップ2から抽出
  - name: human_implies_mortal
    description: "すべての人間は死ぬ"
    source: "LLM推論ステップ2"
    tensor:
      type: matrix
      shape: [1, 1]
      values: [[0.98]]  # 確信度98%

rules:
  # ステップ3の論理構造
  - name: infer_socrates_mortal
    description: "三段論法の適用"
    source: "LLM推論ステップ3"
    inputs:
      - socrates_is_human
      - human_implies_mortal
    output: socrates_is_mortal
    operation: MODUS_PONENS

expected_results:
  - name: socrates_is_mortal
    expected_value: 0.98
    source: "LLM最終結論"
    tolerance: 0.05
```

---

## 🛠️ 実装アプローチ

### **アプローチ1: パターンマッチング方式**

LLMの出力から特定のパターンを検出してTensor Logic化

```java
public class LLMReasoningParser {
    
    /**
     * LLMの推論ステップをパースしてFactsとRulesに変換
     */
    public RuleDefinition parseReasoningSteps(String llmOutput, List<String> reasoningSteps) {
        RuleDefinition.Builder builder = new RuleDefinition.Builder();
        
        // メタデータ設定
        builder.metadata(new Metadata(
            "LLM推論結果",
            "1.0",
            "LLMの推論プロセスを形式化",
            "LLM Parser",
            "llm-reasoning"
        ));
        
        List<Fact> facts = new ArrayList<>();
        List<Rule> rules = new ArrayList<>();
        
        // 推論ステップをパース
        for (int i = 0; i < reasoningSteps.size(); i++) {
            String step = reasoningSteps.get(i);
            
            // パターン1: "AはBである" → Fact
            if (isStatementPattern(step)) {
                Fact fact = extractFact(step, i);
                facts.add(fact);
            }
            
            // パターン2: "AならばB" → Implication Fact
            else if (isImplicationPattern(step)) {
                Fact implication = extractImplication(step, i);
                facts.add(implication);
            }
            
            // パターン3: "したがって、A" → Rule
            else if (isConclusionPattern(step)) {
                Rule rule = extractRule(step, facts, i);
                rules.add(rule);
            }
        }
        
        builder.facts(facts);
        builder.rules(rules);
        
        return builder.build();
    }
    
    private boolean isStatementPattern(String step) {
        // "Aは〜である", "A is 〜"
        return step.matches(".*(は|である|です|is|are).*");
    }
    
    private boolean isImplicationPattern(String step) {
        // "〜ならば〜", "if 〜 then 〜"
        return step.matches(".*(ならば|なら|もし|if.*then).*");
    }
    
    private boolean isConclusionPattern(String step) {
        // "したがって", "ゆえに", "therefore"
        return step.matches(".*(したがって|ゆえに|よって|therefore|thus|hence).*");
    }
}
```

### **アプローチ2: LLMに構造化出力を要求**

LLMに直接、構造化された推論を出力させる

```java
public class StructuredReasoningLLM {
    
    /**
     * LLMに構造化された推論を要求
     */
    public String promptForStructuredReasoning(String query) {
        String prompt = """
            質問: %s
            
            以下の形式で推論してください：
            
            ## 事実（Facts）
            1. [事実名]: [説明] (確信度: XX%%)
            2. [事実名]: [説明] (確信度: XX%%)
            
            ## ルール（Rules）
            1. [ルール名]: [入力1, 入力2] → [出力] (演算: XXXX)
            
            ## 結論
            [結論] (確信度: XX%%)
            """.formatted(query);
        
        return llmService.queryWithReasoning(prompt);
    }
}
```

**LLMの出力例:**

```
## 事実（Facts）
1. socrates_is_human: ソクラテスは人間である (確信度: 100%)
2. human_implies_mortal: すべての人間は死ぬ (確信度: 98%)

## ルール（Rules）
1. infer_mortality: [socrates_is_human, human_implies_mortal] → socrates_is_mortal (演算: MODUS_PONENS)

## 結論
socrates_is_mortal: ソクラテスは死ぬ (確信度: 98%)
```

### **アプローチ3: Chain-of-Thought + Tensor Logic**

Chain-of-Thoughtプロンプティングを使い、段階的に推論させてから形式化

```java
public class ChainOfThoughtTensorLogic {
    
    public RuleDefinition generateTensorLogicFromCoT(String query) {
        // ステップ1: Chain-of-Thoughtで推論
        String cotPrompt = """
            Let's think step by step:
            Question: %s
            
            Step 1: Identify the known facts
            Step 2: Identify the applicable rules
            Step 3: Apply logical reasoning
            Step 4: Reach a conclusion
            
            Please show your reasoning at each step with confidence levels.
            """.formatted(query);
        
        String reasoning = llmService.queryWithReasoning(cotPrompt);
        
        // ステップ2: 推論ステップを抽出
        List<String> steps = extractReasoningSteps(reasoning);
        
        // ステップ3: Tensor Logic化
        return parseReasoningSteps(reasoning, steps);
    }
}
```

---

## 📝 具体例

### **例1: シンプルな三段論法**

#### **入力: LLMへの質問**

```
"ソクラテスは死にますか？"
```

#### **LLMの推論出力**

```json
{
  "answer": "はい、ソクラテスは死にます。",
  "reasoningSteps": [
    "1. ソクラテスは人間です。(確信度: 100%)",
    "2. すべての人間は死にます。(確信度: 98%)",
    "3. したがって、ソクラテスは死にます。(確信度: 98%)"
  ],
  "confidence": 0.98
}
```

#### **Tensor Logic化後のYAML**

```yaml
metadata:
  name: "LLM推論: ソクラテスの死"
  version: "1.0"
  description: "LLMの推論プロセスをTensor Logicで形式化"
  author: "LLM Parser"
  namespace: "llm-socrates"
  source: "LLM"
  timestamp: "2025-11-06T15:00:00"

facts:
  - name: socrates_is_human
    description: "ソクラテスは人間である"
    source: "LLM推論ステップ1"
    values: [1.0]
  
  - name: human_implies_mortal
    description: "すべての人間は死ぬ"
    source: "LLM推論ステップ2"
    tensor:
      type: matrix
      shape: [1, 1]
      values: [[0.98]]

rules:
  - name: infer_socrates_mortal
    description: "三段論法でソクラテスの死を推論"
    source: "LLM推論ステップ3"
    inputs:
      - socrates_is_human
      - human_implies_mortal
    output: socrates_is_mortal
    operation: MODUS_PONENS

expected_results:
  - name: socrates_is_mortal
    expected_value: 0.98
    tolerance: 0.05
```

#### **検証結果**

```java
// Tensor Logic Engineで検証
RuleDefinition def = parser.parseFile("llm-socrates.yaml");
engine.load(def);
engine.forwardChain();

INDArray result = engine.getFact("socrates_is_mortal");
// result = 0.98

// LLMの結論と一致！
```

---

### **例2: 融資審査の推論**

#### **入力: LLMへの質問**

```
"18歳で年収300万円以上、信用スコアが良好な申請者は融資を受けられますか？"
```

#### **LLMの推論出力**

```json
{
  "answer": "はい、融資を受けられる可能性が高いです。",
  "reasoningSteps": [
    "1. 申請者は18歳以上です。(確信度: 100%)",
    "2. 18歳以上は成人です。(確信度: 100%)",
    "3. したがって、申請者は成人です。(確信度: 100%)",
    "4. 年収300万円以上で信用スコアが良好です。(確信度: 90%)",
    "5. 成人かつ財務条件を満たせば融資承認されます。(確信度: 90%)"
  ],
  "confidence": 0.9
}
```

#### **Tensor Logic化**

```yaml
metadata:
  name: "LLM推論: 融資審査"
  namespace: "llm-loan-approval"

facts:
  # ステップ1
  - name: applicant_age_18
    values: [1.0]
  
  # ステップ2
  - name: age_18_implies_adult
    tensor:
      type: matrix
      shape: [1, 1]
      values: [[1.0]]
  
  # ステップ4
  - name: financial_conditions_met
    values: [0.9]

rules:
  # ステップ3
  - name: determine_adult
    inputs: [applicant_age_18, age_18_implies_adult]
    output: is_adult
    operation: MODUS_PONENS
  
  # ステップ5
  - name: determine_loan_approval
    inputs: [is_adult, financial_conditions_met]
    output: loan_approved
    operation: CONJUNCTION

expected_results:
  - name: loan_approved
    expected_value: 0.9
```

---

### **例3: 矛盾検出**

#### **入力**

```
"すべての鳥は飛べる。ペンギンは鳥である。ペンギンは飛べるか？"
```

#### **LLMの推論（誤り）**

```json
{
  "answer": "はい、ペンギンは飛べます。",
  "reasoningSteps": [
    "1. すべての鳥は飛べる。(確信度: 90%)",
    "2. ペンギンは鳥である。(確信度: 100%)",
    "3. したがって、ペンギンは飛べる。(確信度: 90%)"
  ],
  "confidence": 0.9
}
```

#### **Tensor Logic化して検証**

```yaml
facts:
  - name: all_birds_fly
    values: [0.9]
  
  - name: penguin_is_bird
    values: [1.0]
  
  # 実際の観測事実を追加
  - name: penguin_cannot_fly_observed
    description: "観測: ペンギンは飛べない"
    values: [1.0]

rules:
  - name: infer_penguin_flies
    inputs: [penguin_is_bird, all_birds_fly]
    output: penguin_should_fly
    operation: MODUS_PONENS
  
  # 矛盾検出ルール
  - name: detect_contradiction
    inputs: [penguin_should_fly, penguin_cannot_fly_observed]
    output: contradiction_detected
    operation: CONJUNCTION
```

#### **検証結果**

```
penguin_should_fly = 0.9 (LLMの推論)
penguin_cannot_fly_observed = 1.0 (実際の観測)

contradiction_detected = 0.9

⚠️ 矛盾が検出されました！
LLMの推論に誤りがあります。
```

---

## 🔧 実装ステップ

### **Phase 1: パーサーの実装**

```java
@ApplicationScoped
public class LLMReasoningToTensorLogic {
    
    @Inject
    RuleParser ruleParser;
    
    @Inject
    TensorConverter tensorConverter;
    
    /**
     * LLMの推論結果をTensor Logicのルール定義に変換
     */
    public RuleDefinition convert(LLMReasoningResult llmResult) {
        // メタデータ作成
        Metadata metadata = new Metadata(
            "LLM推論: " + llmResult.query(),
            "1.0",
            "LLMの推論プロセスを形式化",
            "LLM",
            "llm-reasoning-" + UUID.randomUUID()
        );
        
        // 推論ステップからFactsとRulesを抽出
        List<Fact> facts = extractFacts(llmResult.reasoningSteps());
        List<Rule> rules = extractRules(llmResult.reasoningSteps(), facts);
        
        // 期待結果を設定
        List<ExpectedResult> expectedResults = createExpectedResults(
            llmResult.conclusion(),
            llmResult.confidence()
        );
        
        return new RuleDefinition(metadata, facts, rules, expectedResults);
    }
    
    private List<Fact> extractFacts(List<String> steps) {
        List<Fact> facts = new ArrayList<>();
        
        for (String step : steps) {
            // パターンマッチングで事実を抽出
            if (step.matches(".*は.*である.*")) {
                Fact fact = parseStatementAsFact(step);
                facts.add(fact);
            }
            else if (step.matches(".*ならば.*")) {
                Fact implication = parseImplicationAsFact(step);
                facts.add(implication);
            }
        }
        
        return facts;
    }
    
    private Fact parseStatementAsFact(String statement) {
        // "AはBである (確信度: XX%)" をパース
        Pattern pattern = Pattern.compile("(.+)は(.+)(である|です).*確信度[:：]\\s*(\\d+)%");
        Matcher matcher = pattern.matcher(statement);
        
        if (matcher.find()) {
            String subject = matcher.group(1).trim();
            String predicate = matcher.group(2).trim();
            double confidence = Double.parseDouble(matcher.group(4)) / 100.0;
            
            String factName = generateFactName(subject, predicate);
            
            return new Fact(
                factName,
                statement,
                "LLM",
                new Tensor("vector", List.of(1), List.of(confidence), confidence)
            );
        }
        
        throw new IllegalArgumentException("パースできません: " + statement);
    }
}
```

### **Phase 2: REST APIの追加**

```java
@Path("/api/llm/reasoning-to-tensor")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LLMReasoningResource {
    
    @Inject
    LLMService llmService;
    
    @Inject
    LLMReasoningToTensorLogic converter;
    
    @Inject
    TensorLogicEngine engine;
    
    @POST
    @Path("/analyze")
    public AnalysisResult analyzeReasoning(AnalysisRequest request) {
        // 1. LLMに推論させる
        LLMReasoningResult llmResult = llmService.queryWithDetailedReasoning(
            request.query()
        );
        
        // 2. Tensor Logic化
        RuleDefinition tensorLogicDef = converter.convert(llmResult);
        
        // 3. Tensor Logicで検証
        engine.clear();
        engine.load(tensorLogicDef);
        BackwardChainingResult verification = engine.backwardChain(
            tensorLogicDef.getMainConclusion()
        );
        
        // 4. LLMとTensor Logicの結果を比較
        ComparisonResult comparison = compare(llmResult, verification);
        
        return new AnalysisResult(
            llmResult,
            tensorLogicDef,
            verification,
            comparison
        );
    }
    
    private ComparisonResult compare(
        LLMReasoningResult llm,
        BackwardChainingResult tensorLogic
    ) {
        double llmConfidence = llm.confidence();
        double tensorLogicConfidence = tensorLogic.goalConfidence();
        double difference = Math.abs(llmConfidence - tensorLogicConfidence);
        
        boolean consistent = difference < 0.1; // 10%以内なら一致
        
        return new ComparisonResult(
            consistent,
            difference,
            llmConfidence,
            tensorLogicConfidence,
            consistent ? "推論は論理的に一貫しています" : "推論に矛盾があります"
        );
    }
}

record AnalysisRequest(String query) {}

record AnalysisResult(
    LLMReasoningResult llmReasoning,
    RuleDefinition tensorLogicRepresentation,
    BackwardChainingResult tensorLogicVerification,
    ComparisonResult comparison
) {}

record ComparisonResult(
    boolean consistent,
    double difference,
    double llmConfidence,
    double tensorLogicConfidence,
    String message
) {}
```

### **Phase 3: 動的YAML生成**

```java
@ApplicationScoped
public class DynamicYAMLGenerator {
    
    /**
     * Tensor Logic定義からYAMLを生成
     */
    public String generateYAML(RuleDefinition definition) {
        StringBuilder yaml = new StringBuilder();
        
        // メタデータ
        yaml.append("metadata:\n");
        yaml.append("  name: \"").append(definition.metadata().name()).append("\"\n");
        yaml.append("  version: \"").append(definition.metadata().version()).append("\"\n");
        yaml.append("  description: \"").append(definition.metadata().description()).append("\"\n");
        yaml.append("  namespace: \"").append(definition.metadata().namespace()).append("\"\n");
        yaml.append("\n");
        
        // Facts
        yaml.append("facts:\n");
        for (Fact fact : definition.facts()) {
            yaml.append("  - name: ").append(fact.name()).append("\n");
            yaml.append("    description: \"").append(fact.description()).append("\"\n");
            yaml.append("    values: ").append(formatValues(fact.tensor().values())).append("\n");
        }
        yaml.append("\n");
        
        // Rules
        yaml.append("rules:\n");
        for (Rule rule : definition.rules()) {
            yaml.append("  - name: ").append(rule.name()).append("\n");
            yaml.append("    description: \"").append(rule.description()).append("\"\n");
            yaml.append("    inputs:\n");
            for (String input : rule.inputs()) {
                yaml.append("      - ").append(input).append("\n");
            }
            yaml.append("    output: ").append(rule.output()).append("\n");
            yaml.append("    operation: ").append(rule.operation()).append("\n");
        }
        
        return yaml.toString();
    }
}
```

---

## 🎯 ユースケース

### **ユースケース1: LLMの推論検証**

```bash
# LLMに質問して推論を検証
curl -X POST http://localhost:8080/api/llm/reasoning-to-tensor/analyze \
  -H 'Content-Type: application/json' \
  -d '{
    "query": "ソクラテスは死にますか？"
  }'
```

**結果:**
```json
{
  "llmReasoning": {
    "answer": "はい、ソクラテスは死にます",
    "confidence": 0.98
  },
  "tensorLogicVerification": {
    "success": true,
    "goalConfidence": 0.98
  },
  "comparison": {
    "consistent": true,
    "difference": 0.0,
    "message": "推論は論理的に一貫しています"
  }
}
```

### **ユースケース2: ハルシネーション検出**

```bash
curl -X POST http://localhost:8080/api/llm/reasoning-to-tensor/analyze \
  -H 'Content-Type: application/json' \
  -d '{
    "query": "ペンギンは飛べますか？"
  }'
```

**結果:**
```json
{
  "llmReasoning": {
    "answer": "はい、飛べます",
    "confidence": 0.9
  },
  "tensorLogicVerification": {
    "success": false,
    "contradictionDetected": true
  },
  "comparison": {
    "consistent": false,
    "message": "⚠️ LLMの推論に矛盾があります"
  }
}
```

### **ユースケース3: 推論の透明性向上**

LLMの推論プロセスを可視化：

```
質問: "18歳の申請者は融資を受けられますか？"

LLMの推論:
  ステップ1: 18歳 → 成人
  ステップ2: 成人 ∧ 財務条件 → 融資承認

Tensor Logic化:
  Fact: applicant_age = 1.0
  Fact: age_implies_adult = 1.0
  Rule: MODUS_PONENS → is_adult = 1.0
  Rule: CONJUNCTION → loan_approved = 0.9

検証結果:
  ✅ 論理的に一貫
  ✅ すべてのステップが検証可能
  ✅ 確信度が定量化されている
```

### **ユースケース4: 説明可能AI**

```
ユーザー: "なぜ融資が承認されたのですか？"

システム:
  1. あなたは18歳以上です (確信度: 100%)
  2. 18歳以上は成人です (法律: 確信度: 100%)
  3. したがって、あなたは成人です (確信度: 100%)
  4. 年収と信用スコアが基準を満たしています (確信度: 90%)
  5. 成人かつ財務条件を満たすため、融資が承認されました (確信度: 90%)

各ステップはTensor Logicで検証済みです。
```

---

## 📊 期待される効果

### **定量的効果**

| 指標 | 改善 |
|------|------|
| **推論の透明性** | ブラックボックス → 完全可視化 |
| **ハルシネーション検出率** | 0% → 70-80% |
| **説明可能性スコア** | 低 → 高 |
| **ユーザー信頼度** | +30% |

### **定性的効果**

1. **信頼性向上**: 推論プロセスが検証可能
2. **デバッグ容易性**: どこで誤ったかを特定可能
3. **監査可能性**: 推論の記録と追跡
4. **教育効果**: 論理的思考の学習に活用

---

## 🚀 次のステップ

### **実装ロードマップ**

#### **Phase 1: 基本実装（1-2週間）**
- [ ] LLMReasoningParser の実装
- [ ] 基本的なパターンマッチング
- [ ] シンプルな三段論法の変換

#### **Phase 2: API実装（1週間）**
- [ ] REST API エンドポイント
- [ ] 推論結果の検証機能
- [ ] YAMLの動的生成

#### **Phase 3: 高度な機能（2-3週間）**
- [ ] 複雑な推論構造への対応
- [ ] 矛盾検出の強化
- [ ] 推論グラフの可視化

#### **Phase 4: 統合とテスト（1週間）**
- [ ] 既存システムとの統合
- [ ] テストケースの作成
- [ ] ドキュメント作成

---

## 💡 まとめ

**LLM推論のTensor Logic化により:**

1. ✅ **透明性**: ブラックボックスを可視化
2. ✅ **検証可能性**: 論理的整合性を保証
3. ✅ **信頼性**: ハルシネーションを検出
4. ✅ **説明可能性**: なぜその結論かを明確化

**これは単なる検証ツールではなく、LLMとシンボリックAIの真のハイブリッドシステムです！**

---

**Tensor LogicでLLMの推論を形式化し、より信頼できるAIシステムを構築しましょう！** 🚀✨

