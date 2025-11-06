# Tensor Logic Engine 完全ガイド

## 📚 目次

1. [概要](#概要)
2. [システムアーキテクチャ](#システムアーキテクチャ)
3. [主要コンポーネント](#主要コンポーネント)
4. [技術スタック](#技術スタック)
5. [セットアップ](#セットアップ)
6. [使用方法](#使用方法)
7. [ルール定義](#ルール定義)
8. [API仕様](#api仕様)
9. [実装例](#実装例)
10. [ベストプラクティス](#ベストプラクティス)
11. [トラブルシューティング](#トラブルシューティング)
12. [今後の展望](#今後の展望)

---

## 概要

### 🎯 Tensor Logic Engineとは

**Tensor Logic Engine**は、**ニューラルAI（LLM）とシンボリックAI（Tensor Logic）を融合**させた次世代のハイブリッド推論システムです。

#### 主な特徴

- **ハイブリッド推論**: LLMの柔軟性とTensor Logicの厳密性を組み合わせ
- **透明性**: ブラックボックスなLLMに数学的検証を追加
- **拡張性**: YAMLでルールを簡単に定義・追加
- **実用性**: REST APIとエンタープライズ統合パターン

#### なぜこのシステムが必要か

| 課題 | 従来のLLM | Tensor Logic Engine |
|------|----------|-------------------|
| **信頼性** | ハルシネーション（幻覚） | 論理的検証で確認 |
| **透明性** | ブラックボックス | 推論過程を可視化 |
| **一貫性** | 応答のばらつき | 数学的に検証 |
| **拡張性** | 再学習が必要 | ルール追加で対応 |

---

## システムアーキテクチャ

### 全体構成

```
┌─────────────────────────────────────────────────────────────┐
│                     REST API Layer                           │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │  Camel       │  │  Generic     │  │  Rule        │      │
│  │  Integration │  │  Verification│  │  Management  │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                  Integration Layer                           │
│  ┌──────────────────────────────────────────────────────┐   │
│  │       GenericLLMVerifier                             │   │
│  │  - LLM応答の取得                                      │   │
│  │  - Tensor Logic検証                                  │   │
│  │  - 結果の統合                                        │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
              ↓                           ↓
┌──────────────────────┐    ┌──────────────────────────────┐
│   LLM Service        │    │   Tensor Logic Engine        │
│  ┌────────────────┐  │    │  ┌────────────────────────┐  │
│  │ OpenAI GPT-4   │  │    │  │ Facts (INDArray)       │  │
│  │ API Integration│  │    │  │ Rules (Operations)     │  │
│  │ Demo Mode      │  │    │  │ Forward Chaining       │  │
│  └────────────────┘  │    │  │ Contradiction Check    │  │
└──────────────────────┘    │  └────────────────────────┘  │
                            └──────────────────────────────┘
                                        ↓
                            ┌──────────────────────────────┐
                            │   Rule Management            │
                            │  ┌────────────────────────┐  │
                            │  │ RuleParser (YAML)      │  │
                            │  │ RuleLoader             │  │
                            │  │ TensorConverter        │  │
                            │  └────────────────────────┘  │
                            └──────────────────────────────┘
```

### 処理フロー

```
1. REST APIで質問とルールファイルを受信
   ↓
2. RuleLoaderがYAMLルールを読み込み
   ↓
3. TensorConverterが事実とルールをINDArrayに変換
   ↓
4. TensorLogicEngineに登録
   ↓
5. LLMServiceがGPT-4に質問を送信
   ↓
6. GenericLLMVerifierが統合処理
   ├─ LLM応答から事実を抽出
   ├─ Tensor Logicで前向き推論
   └─ 期待結果と比較
   ↓
7. 検証結果をJSONで返却
```

---

## 主要コンポーネント

### 1. TensorLogicEngine

**パッケージ**: `ai.tensorlogic.core`  
**役割**: テンソル演算による論理推論の実行

#### 主な機能

```java
@ApplicationScoped
public class TensorLogicEngine {
    // 事実の管理
    private final Map<String, INDArray> facts = new HashMap<>();
    
    // ルールの管理
    private final Map<String, Rule> rules = new HashMap<>();
    
    // 事実を追加
    public void addFact(String name, INDArray tensor)
    
    // ルールを追加
    public void addRule(String name, Rule rule)
    
    // 前向き推論を実行
    public Map<String, INDArray> forwardChain()
    
    // 矛盾を検出
    public double detectContradiction(String fact1, String fact2)
}
```

#### サポートされる演算

| 演算タイプ | 説明 | 数式 | 実装 |
|----------|------|------|------|
| **MODUS_PONENS** | 三段論法 | A ∧ (A→B) ⟹ B | `matmul(A, R)` |
| **CONJUNCTION** | 論理積 | A ∧ B | `min(A, B)` |
| **DISJUNCTION** | 論理和 | A ∨ B | `max(A, B)` |
| **CHAIN** | 関係の合成 | R₁ ○ R₂ | `matmul(R₁, R₂)` |

#### 前向き推論アルゴリズム

```
入力: 事実集合 F、ルール集合 R
出力: 推論された新しい事実

1. 推論カウンター = 0
2. While 推論カウンター < 最大反復回数:
   a. 各ルール r ∈ R に対して:
      - 入力事実が全て存在するか確認
      - 存在する場合、演算を適用
      - 新しい事実を導出
   b. 新しい事実が導出されなければ終了
   c. 推論カウンター++
3. 推論された事実を返却
```

---

### 2. LLMService

**パッケージ**: `ai.tensorlogic.llm`  
**役割**: OpenAI GPT-4との連携

#### API キー管理

```java
@ConfigProperty(name = "llm.openai.api-key", defaultValue = "demo-mode")
String apiKey;

// 有効なAPIキーの条件
private boolean isValidApiKey() {
    return apiKey != null 
        && !apiKey.isBlank()
        && apiKey.length() >= 20
        && apiKey.startsWith("sk-")
        && !apiKey.equals("demo-mode")
        && !apiKey.contains("your-actual")
        && !apiKey.contains("your-api-key");
}
```

#### 動作モード

| モード | 条件 | 動作 |
|-------|------|------|
| **OpenAI API実行モード** | 有効なAPIキー | GPT-4に実際に問い合わせ |
| **デモモード** | 無効/未設定のAPIキー | 固定の模擬応答を返却 |

#### LLM応答の構造

```java
public record LLMResponse(
    String answer,              // LLMの回答
    double confidence,          // 確信度 (0.0-1.0)
    List<String> reasoningSteps // 推論ステップ
)
```

---

### 3. GenericLLMVerifier

**パッケージ**: `ai.tensorlogic.integration`  
**役割**: LLMとTensor Logicの統合

#### 検証フロー

```java
public GenericVerificationResult verify(GenericVerificationRequest request) {
    // 1. ルールファイルを読み込み
    ruleLoader.loadFromResource(request.ruleFile());
    
    // 2. LLMに問い合わせ
    LLMResponse llmResponse = llmService.queryWithReasoning(request.query());
    
    // 3. LLMの回答から事実を抽出・登録
    extractAndRegisterFacts(llmResponse, request);
    
    // 4. 推論エンジンで前向き推論を実行
    Map<String, INDArray> inferredFacts = engine.forwardChain();
    
    // 5. 期待される結果と比較
    VerificationStatus status = verifyAgainstExpectedResults(
        inferredFacts, 
        request.expectedOutputs(),
        request.tolerance()
    );
    
    // 6. 結果を構築して返却
    return buildVerificationResult(llmResponse, inferredFacts, status, request);
}
```

#### 検証結果

```java
public record GenericVerificationResult(
    boolean success,                    // 検証成功/失敗
    String query,                       // 質問
    String llmAnswer,                   // LLMの回答
    double llmConfidence,               // LLM確信度
    List<String> reasoningSteps,        // 推論ステップ
    boolean logicallySound,             // 論理的妥当性
    double validationScore,             // 検証スコア
    Map<String, String> inferredFacts,  // 推論された事実
    List<String> verificationMatches,   // 検証成功項目
    List<String> verificationMismatches,// 検証失敗項目
    List<String> missingExpectedFacts,  // 欠落している期待事実
    String errorMessage                 // エラーメッセージ
)
```

---

### 4. Rule Management System

#### RuleParser

**役割**: YAMLファイルのパースと検証

```java
@ApplicationScoped
public class RuleParser {
    // YAMLファイルをパース
    public RuleDefinition parseResource(String resourcePath)
    
    // ルール定義を検証
    public ValidationResult validate(RuleDefinition definition)
    
    // RuleSpecをRuleオブジェクトに変換
    public Rule convertToRule(RuleSpec spec)
}
```

#### TensorConverter

**役割**: ルール定義をINDArrayに変換

```java
@ApplicationScoped
public class TensorConverter {
    // すべての事実をテンソルに変換
    public Map<String, INDArray> convertAllFacts(RuleDefinition definition)
    
    // 単一の事実をテンソルに変換
    public INDArray convertToTensor(FactSpec factSpec)
    
    // テンソル情報を文字列化（デバッグ用）
    public String tensorInfo(INDArray tensor)
}
```

#### RuleLoader

**役割**: ルールをエンジンに登録

```java
@ApplicationScoped
public class RuleLoader {
    // ファイルシステムから読み込み
    public LoadResult loadFromFile(String filePath)
    
    // リソースから読み込み（推奨）
    public LoadResult loadFromResource(String resourcePath)
}
```

---

## 技術スタック

### バックエンド

| 技術 | バージョン | 用途 |
|-----|----------|------|
| **Java** | 21 | Records, Pattern Matching, Text Blocks |
| **Quarkus** | 3.6.0 | アプリケーションフレームワーク |
| **Apache Camel** | 4.x | エンタープライズ統合パターン |
| **ND4J** | 1.0.0-M2.1 | テンソル演算（NumPy相当） |
| **Maven** | 3.x | ビルド管理 |

### LLM統合

| 技術 | 用途 |
|-----|------|
| **OpenAI Java Client** | GPT-4 API連携 |
| **Jackson** | JSON/YAMLシリアライゼーション |

### API & ドキュメント

| 技術 | 用途 |
|-----|------|
| **RESTEasy Reactive** | 高性能REST API |
| **SmallRye OpenAPI** | OpenAPI 3.0仕様生成 |
| **Swagger UI** | インタラクティブAPI ドキュメント |

### 依存関係

```xml
<dependencies>
    <!-- Quarkus -->
    <dependency>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-resteasy-reactive-jackson</artifactId>
    </dependency>
    
    <!-- Apache Camel -->
    <dependency>
        <groupId>org.apache.camel.quarkus</groupId>
        <artifactId>camel-quarkus-core</artifactId>
    </dependency>
    
    <!-- ND4J (テンソル演算) -->
    <dependency>
        <groupId>org.nd4j</groupId>
        <artifactId>nd4j-native-platform</artifactId>
        <version>1.0.0-M2.1</version>
    </dependency>
    
    <!-- OpenAI API -->
    <dependency>
        <groupId>com.theokanning.openai-gpt3-java</groupId>
        <artifactId>service</artifactId>
        <version>0.18.2</version>
    </dependency>
</dependencies>
```

---

## セットアップ

### 前提条件

- **Java 21** 以上
- **Maven 3.8** 以上
- **OpenAI API Key** (オプション: デモモードでも動作)

### インストール

```bash
# リポジトリをクローン
git clone <repository-url>
cd TensorLogic

# ビルド
mvn clean package

# 開発モードで起動
mvn quarkus:dev
```

### 環境変数の設定

#### OpenAI API Keyの設定（推奨）

```bash
# .envファイルを作成
echo "OPENAI_API_KEY=sk-proj-..." > .env

# または環境変数を直接設定
export OPENAI_API_KEY=sk-proj-...
```

#### application.yamlでの設定

```yaml
llm:
  openai:
    api-key: ${OPENAI_API_KEY:demo-mode}
    model: gpt-4
    timeout: 30s
```

### 起動確認

```bash
# ヘルスチェック
curl http://localhost:8080/q/health

# Swagger UI
open http://localhost:8080/swagger-ui
```

---

## 使用方法

### 基本的な使い方

#### 1. シンプルな検証

```bash
curl -X POST http://localhost:8080/api/verify/simple \
  -H 'Content-Type: application/json' \
  -d '{
    "query": "ソクラテスは死にますか？",
    "ruleFile": "rules/simple-verification-rules.yaml"
  }'
```

**応答例：**

```json
{
  "success": true,
  "query": "ソクラテスは死にますか？",
  "llmAnswer": "はい、ソクラテスは死にます...",
  "llmConfidence": 0.95,
  "reasoningSteps": [
    "1. ソクラテスは人間です。",
    "2. すべての人間は死にます。",
    "3. したがって、ソクラテスは死にます。"
  ],
  "logicallySound": true,
  "validationScore": 1.0,
  "inferredFacts": {
    "statement_b_true": "0.8550"
  }
}
```

#### 2. ルールの読み込みと確認

```bash
# ルールを読み込み
curl -X POST http://localhost:8080/api/rules/load-resource \
  -H 'Content-Type: application/json' \
  -d '{"resourcePath": "rules/bird-contradiction-rules.yaml"}'

# 読み込んだルールを確認
curl http://localhost:8080/api/rules/inspect
```

#### 3. 矛盾検出

```bash
curl -X POST http://localhost:8080/api/verify/simple \
  -H 'Content-Type: application/json' \
  -d '{
    "query": "すべての鳥は飛べるが、ペンギンは鳥である。ペンギンは飛べるか？",
    "ruleFile": "rules/bird-contradiction-rules.yaml"
  }'
```

---

## ルール定義

### YAMLルールファイルの構造

```yaml
# ====================================
# メタデータ
# ====================================
metadata:
  name: "ルールセット名"
  version: "1.0"
  description: "ルールの説明"
  author: "作成者名"

# ====================================
# エンティティ（オプション）
# ====================================
entities:
  - name: entity_name
    type: proposition
    description: "エンティティの説明"

# ====================================
# 事実（初期値）
# ====================================
facts:
  - name: fact_name
    description: "事実の説明"
    notation: "数学的記法（オプション）"
    tensor:
      type: vector          # vector, matrix, scalar
      shape: [1]            # テンソルの形状
      values: [0.9]         # 値（確率）
      confidence: 0.9       # 確信度

# ====================================
# 推論ルール
# ====================================
rules:
  - name: rule_name
    description: "ルールの説明"
    notation: "A ∧ B ⟹ C"
    inputs:
      - input_fact_1
      - input_fact_2
    output: output_fact
    operation: MODUS_PONENS  # MODUS_PONENS, CONJUNCTION, DISJUNCTION, CHAIN
    priority: 1              # 実行優先度（低い数字が先）
    enabled: true            # 有効/無効

# ====================================
# 期待される結果（検証用）
# ====================================
expected_results:
  - name: output_fact
    description: "期待される結果の説明"
    notation: "P(C)"
    expected_value: 0.85    # 期待値
    tolerance: 0.1          # 許容誤差
```

### 実例：三段論法

```yaml
metadata:
  name: "ソクラテスの三段論法"
  version: "1.0"

facts:
  # ソクラテスは人間である
  - name: socrates_is_human
    tensor:
      type: vector
      shape: [1]
      values: [0.95]
      confidence: 0.95
  
  # すべての人間は死ぬ
  - name: human_is_mortal
    tensor:
      type: matrix
      shape: [1, 1]
      values: [[0.98]]
      confidence: 0.98

rules:
  - name: deduce_mortality
    description: "ソクラテスは人間 ∧ 人間は死ぬ ⟹ ソクラテスは死ぬ"
    inputs:
      - socrates_is_human
      - human_is_mortal
    output: socrates_is_mortal
    operation: MODUS_PONENS
    enabled: true

expected_results:
  - name: socrates_is_mortal
    expected_value: 0.931  # 0.95 * 0.98 ≈ 0.931
    tolerance: 0.05
```

### 実例：矛盾検出

```yaml
metadata:
  name: "ペンギンの飛行矛盾"
  version: "1.0"

facts:
  # 一般論: すべての鳥は飛べる
  - name: all_birds_can_fly
    tensor:
      type: matrix
      shape: [1, 1]
      values: [[0.9]]
      confidence: 0.9
  
  # 事実1: ペンギンは鳥である
  - name: penguin_is_bird
    tensor:
      type: vector
      shape: [1]
      values: [1.0]
      confidence: 1.0
  
  # 事実2: ペンギンは飛べない
  - name: penguin_cannot_fly
    tensor:
      type: vector
      shape: [1]
      values: [1.0]
      confidence: 1.0

rules:
  # ルール1: 一般論からの推論
  - name: bird_implies_fly
    description: "ペンギンは鳥 ∧ 鳥は飛べる ⟹ ペンギンは飛べるはず"
    inputs:
      - penguin_is_bird
      - all_birds_can_fly
    output: penguin_should_fly
    operation: MODUS_PONENS
    priority: 1
    enabled: true
  
  # ルール2: 矛盾検出
  - name: detect_contradiction
    description: "飛べるはず ∧ 飛べない ⟹ 矛盾が存在"
    inputs:
      - penguin_should_fly
      - penguin_cannot_fly
    output: both_claims_exist
    operation: CONJUNCTION
    priority: 2
    enabled: true

expected_results:
  - name: penguin_should_fly
    expected_value: 0.9
    tolerance: 0.1
  
  - name: both_claims_exist
    expected_value: 0.9  # 高い値 = 矛盾が強く存在
    tolerance: 0.1
```

---

## API仕様

### 検証API

#### POST /api/verify/simple

**シンプルなLLM検証**

**リクエスト：**

```json
{
  "query": "質問文",
  "ruleFile": "rules/rule-file.yaml"
}
```

**レスポンス：**

```json
{
  "success": true,
  "query": "質問文",
  "llmAnswer": "LLMの回答",
  "llmConfidence": 0.9,
  "reasoningSteps": ["推論ステップ1", "推論ステップ2"],
  "logicallySound": true,
  "validationScore": 0.95,
  "inferredFacts": {
    "fact_name": "0.8500"
  },
  "verificationMatches": ["検証成功項目"],
  "verificationMismatches": [],
  "missingExpectedFacts": [],
  "errorMessage": null
}
```

#### POST /api/verify/generic

**詳細な検証（カスタム事実・期待値付き）**

**リクエスト：**

```json
{
  "query": "質問文",
  "ruleFile": "rules/rule-file.yaml",
  "customFacts": {
    "custom_fact": [0.9]
  },
  "expectedOutputs": {
    "output_fact": 0.85
  },
  "tolerance": 0.1,
  "extractFactsFromLLM": false
}
```

---

### ルール管理API

#### POST /api/rules/load-resource

**リソースからルールを読み込み**

**リクエスト：**

```json
{
  "resourcePath": "rules/bird-contradiction-rules.yaml"
}
```

**レスポンス：**

```json
{
  "success": true,
  "message": "✓ '鳥の飛行矛盾検出' を読み込みました (事実: 3, ルール: 2)",
  "conclusionValue": null,
  "resultCount": 2
}
```

#### GET /api/rules/inspect

**登録済みルールと事実を確認**

**レスポンス：**

```json
{
  "success": true,
  "count": 5,
  "rules": [
    {
      "name": "rule_name",
      "inputs": ["input1", "input2"],
      "output": "output",
      "operation": "MODUS_PONENS"
    }
  ],
  "facts": [
    {
      "name": "fact_name",
      "shape": "[1, 1]",
      "dtype": "DOUBLE",
      "fullContent": "[[0.9000]]",
      "stats": {
        "min": 0.9,
        "max": 0.9,
        "mean": 0.9,
        "std": 0.0
      }
    }
  ]
}
```

---

### Camel統合API

#### POST /api/camel/verify

**基本的な検証（Camel経由）**

**リクエスト：**

```json
"質問文"
```

**レスポンス：**

```json
{
  "answer": "LLMの回答",
  "llmConfidence": 0.9,
  "reasoningSteps": ["推論ステップ"],
  "isLogicallySound": true,
  "validationConfidence": 0.95,
  "validationDetails": {
    "isValid": true,
    "confidence": 0.95,
    "actualValue": "0.9000",
    "expectedValue": "0.8550",
    "meanError": 0.045,
    "maxError": 0.045
  }
}
```

#### POST /api/camel/generic-verify

**汎用検証（Camel経由）**

**リクエスト：**

```json
{
  "query": "質問文",
  "ruleFile": "rules/rule-file.yaml"
}
```

#### POST /api/camel/batch-verify

**バッチ検証**

**リクエスト：**

```json
[
  {
    "query": "質問1",
    "ruleFile": "rules/rule1.yaml"
  },
  {
    "query": "質問2",
    "ruleFile": "rules/rule2.yaml"
  }
]
```

---

## 実装例

### 実例1：ペンギンの矛盾検出

**目的**: 一般論と例外の矛盾を検出

**質問:**
```
すべての鳥は飛べるが、ペンギンは鳥である。ペンギンは飛べるか？
```

**処理フロー:**

```
1. ルール読み込み
   ├─ all_birds_can_fly = [[0.9]]
   ├─ penguin_is_bird = [[1.0]]
   └─ penguin_cannot_fly = [[1.0]]

2. LLMに質問
   ├─ GPT-4が段階的推論
   ├─ 「一般論では飛べるはずだが、実際には飛べない」
   └─ 確信度: 0.8

3. Tensor Logic推論
   ├─ Rule 1: penguin_is_bird ∧ all_birds_can_fly
   │         ⟹ penguin_should_fly = 0.9
   │
   └─ Rule 2: penguin_should_fly ∧ penguin_cannot_fly
             ⟹ both_claims_exist = min(0.9, 1.0) = 0.9

4. 結果統合
   ├─ LLM推論: "ペンギンは飛べない"
   ├─ Tensor Logic: 矛盾度 = 0.9（高い）
   └─ 検証スコア: 1.0（論理的に一貫）
```

**結果:**

```json
{
  "success": true,
  "llmAnswer": "ペンギンは飛べません。一般論と例外の違いです。",
  "llmConfidence": 0.8,
  "logicallySound": true,
  "validationScore": 1.0,
  "inferredFacts": {
    "penguin_should_fly": "0.9000",
    "both_claims_exist": "0.9000"
  }
}
```

**解釈:**
- LLMは矛盾を正しく認識
- Tensor Logicは矛盾度を定量化（0.9 = 高い矛盾）
- 両者が一致して信頼性の高い結論

---

### 実例2：ソクラテスの三段論法

**目的**: 古典的な演繹推論の検証

**質問:**
```
ソクラテスは死にますか？
```

**処理フロー:**

```
1. ルール読み込み
   ├─ socrates_is_human = [[0.95]]
   └─ human_is_mortal = [[0.98]]

2. LLMに質問
   ├─ 多角的な推論を展開
   │  - 史実のソクラテス: すでに死亡（確信度100%）
   │  - 一般的な人間: いつか死ぬ（確信度95%）
   └─ 総合確信度: 0.95

3. Tensor Logic推論
   └─ Rule: socrates_is_human ∧ human_is_mortal
           ⟹ socrates_is_mortal = 0.95 * 0.98 = 0.931

4. 結果統合
   ├─ LLM推論: "はい、死にます"
   ├─ Tensor Logic: 0.931（93.1%の確率で死ぬ）
   └─ 検証スコア: 0.98（ほぼ完全に一致）
```

**結果:**

```json
{
  "success": true,
  "llmConfidence": 0.95,
  "logicallySound": true,
  "validationScore": 0.98,
  "inferredFacts": {
    "socrates_is_mortal": "0.9310"
  }
}
```

---

### 実例3：カモノハシの例外

**目的**: 誤った前提の認識と修正

**質問:**
```
すべての哺乳類は卵を産まない。カモノハシは哺乳類である。
カモノハシは卵を産むか？
```

**LLMの推論:**

```
段階1: カモノハシは哺乳類（確信度100%）

段階2: 「すべての哺乳類が卵を産まない」は不正確
        → 確信度を65%に下げる ✓

結論: カモノハシは卵を産む（確信度100%）
```

**結果:**

```json
{
  "success": true,
  "llmAnswer": "カモノハシは卵を産みます。前提が誤っています。",
  "llmConfidence": 0.8,
  "logicallySound": true
}
```

**解釈:**
- LLMは前提の誤りを認識
- 確信度を適切に調整
- 正しい結論を導出

---

## ベストプラクティス

### 1. ルール設計

#### ✅ 推奨

- **原子的なルール**: 1つのルールに1つの推論
- **明確な命名**: `bird_implies_fly`のような説明的な名前
- **適切な確率**: 現実世界の不確実性を反映
- **ドキュメント**: `description`と`notation`フィールドを活用

#### ❌ 避けるべき

- **複雑すぎるルール**: 複数の推論を1つのルールに詰め込む
- **曖昧な命名**: `rule1`, `rule2`などの名前
- **極端な確率**: 0.0や1.0を安易に使う（例外を考慮）

### 2. 事実の定義

#### 確率の設定ガイド

| 確率 | 意味 | 例 |
|-----|------|-----|
| **0.95-1.0** | ほぼ確実 | "ペンギンは鳥である" |
| **0.8-0.95** | 高い確信 | "すべての鳥は飛べる"（例外あり） |
| **0.6-0.8** | 中程度の確信 | "天気予報" |
| **0.4-0.6** | 不確実 | "コイントス" |
| **0.0-0.4** | 低い確信 | "宝くじが当たる" |

### 3. テストとデバッグ

#### ステップバイステップ検証

```bash
# 1. ルールの読み込みを確認
curl -X POST http://localhost:8080/api/rules/load-resource \
  -H 'Content-Type: application/json' \
  -d '{"resourcePath": "rules/your-rules.yaml"}'

# 2. 登録された事実とルールを確認
curl http://localhost:8080/api/rules/inspect

# 3. 簡単な質問でテスト
curl -X POST http://localhost:8080/api/verify/simple \
  -H 'Content-Type: application/json' \
  -d '{"query": "簡単な質問", "ruleFile": "rules/your-rules.yaml"}'

# 4. 複雑な質問でテスト
curl -X POST http://localhost:8080/api/verify/simple \
  -H 'Content-Type: application/json' \
  -d '{"query": "複雑な質問", "ruleFile": "rules/your-rules.yaml"}'
```

### 4. パフォーマンス最適化

#### LLM呼び出しの最適化

- **キャッシュ**: 同じ質問は結果をキャッシュ
- **バッチ処理**: 複数の質問をまとめて処理
- **タイムアウト設定**: 長時間の応答を避ける

```yaml
llm:
  openai:
    timeout: 30s  # 適切なタイムアウト
    model: gpt-4  # または gpt-3.5-turbo（速度優先）
```

#### Tensor演算の最適化

- **前向き推論の反復制限**: `maxIterations`を適切に設定
- **ルールの優先度**: `priority`で実行順序を制御
- **不要なルールの無効化**: `enabled: false`で無効化

---

## トラブルシューティング

### よくある問題と解決方法

#### 1. OpenAI APIエラー

**問題:**
```
You exceeded your current quota
```

**解決:**
- OpenAI Platformで課金設定を確認
- デモモードでテスト（APIキーを`demo-mode`に設定）

#### 2. ルール読み込みエラー

**問題:**
```
FileNotFoundException: rules/rule-file.yaml
```

**解決:**
- `/api/rules/load-resource`を使用（`/load-file`ではない）
- ファイルが`src/main/resources/rules/`に存在するか確認

#### 3. コンパイルエラー

**問題:**
```
クラス GenericVerificationRequestはpublicであり、
ファイルGenericVerificationRequest.javaで宣言する必要があります
```

**解決:**
- `public record`は専用ファイルに定義する必要がある
- 既に修正済み（各recordが独立したファイル）

#### 4. 矛盾が検出されない

**問題:**
矛盾があるはずなのに`both_claims_exist`が計算されていない

**解決:**
- `expected_results`に`both_claims_exist`を追加
- ルールの`priority`を確認（依存関係のあるルールは後で実行）
- `inspect` APIで推論結果を確認

#### 5. 検証スコアが低い

**問題:**
`validationScore`が期待より低い

**解決:**
- `tolerance`を調整（デフォルト: 0.05）
- 期待値を現実的な値に修正
- LLMの確信度とTensor Logicの結果を個別に確認

---

## 今後の展望

### 短期的な改善（1-3ヶ月）

#### 1. エンジン機能の拡張

- [ ] **新しい演算タイプ**
  - `NEGATION`: 否定 (¬A)
  - `IMPLICATION`: 含意 (A → B)
  - `XOR`: 排他的論理和 (A ⊕ B)

- [ ] **確率的推論の強化**
  - ベイズ推論のサポート
  - 不確実性の伝播
  - モンテカルロシミュレーション

- [ ] **時間的推論**
  - 時系列データの処理
  - 因果関係の推論
  - 変化の追跡

#### 2. UI/UX改善

- [ ] **Webダッシュボード**
  - 推論過程の可視化
  - ルールエディタ
  - リアルタイムモニタリング

- [ ] **インタラクティブAPI**
  - WebSocketサポート
  - ストリーミング応答
  - 対話型推論

#### 3. パフォーマンス最適化

- [ ] **キャッシング**
  - LLM応答のキャッシュ
  - 推論結果のキャッシュ
  - Redis統合

- [ ] **並列処理**
  - マルチスレッド推論
  - 分散処理
  - GPUアクセラレーション

### 中期的な改善（3-6ヶ月）

#### 1. 高度な推論機能

- [ ] **説明可能AI (XAI)**
  - 推論の説明生成
  - 反事実的推論
  - 重要度分析

- [ ] **メタ推論**
  - ルールの学習
  - 自動ルール生成
  - ルールの最適化

- [ ] **マルチモーダル推論**
  - 画像 + テキスト
  - 音声 + テキスト
  - グラフ構造の推論

#### 2. エンタープライズ機能

- [ ] **セキュリティ**
  - 認証・認可
  - 監査ログ
  - データ暗号化

- [ ] **スケーラビリティ**
  - Kubernetes対応
  - マイクロサービス化
  - 負荷分散

- [ ] **統合**
  - データベース統合
  - 外部API連携
  - イベント駆動アーキテクチャ

### 長期的なビジョン（6ヶ月以上）

#### 1. 研究開発

- [ ] **新しいTensor Logic演算**
  - 高次論理
  - モーダル論理
  - 時相論理

- [ ] **ニューラルシンボリック統合**
  - ニューラルネットワークとの深い統合
  - 学習可能な論理演算
  - エンドツーエンド学習

- [ ] **理論的基盤の強化**
  - 形式的検証
  - 完全性・健全性の証明
  - 計算複雑性の分析

#### 2. 応用分野

- [ ] **医療診断支援**
  - 症状から疾患を推論
  - 治療方針の検証
  - 薬剤相互作用の検出

- [ ] **法律文書分析**
  - 契約書の論理検証
  - 判例の推論
  - コンプライアンスチェック

- [ ] **科学的発見**
  - 仮説の検証
  - 実験計画の最適化
  - 論文の論理分析

---

## 参考資料

### 論文

- **Tensor Logic**: [arXiv:2510.12269v3](https://arxiv.org/abs/2510.12269)
- **Neural-Symbolic Integration**: 関連研究の引用

### ドキュメント

- [JAVA_IMPLEMENTATION.md](./JAVA_IMPLEMENTATION.md) - Java実装の詳細
- [RULE_DSL_GUIDE.md](./RULE_DSL_GUIDE.md) - ルールDSLの完全ガイド
- [OPENAI_API_KEY_SETUP.md](./OPENAI_API_KEY_SETUP.md) - APIキー設定ガイド
- [GENERIC_LLM_VERIFICATION_GUIDE.md](./GENERIC_LLM_VERIFICATION_GUIDE.md) - 汎用検証ガイド

### 外部リソース

- [Quarkus Documentation](https://quarkus.io/guides/)
- [Apache Camel Documentation](https://camel.apache.org/manual/)
- [ND4J Documentation](https://deeplearning4j.konduit.ai/nd4j/tutorials)
- [OpenAI API Documentation](https://platform.openai.com/docs/)

---

## コントリビューション

### 開発ガイドライン

1. **コーディング規約**: Java 21のベストプラクティスに従う
2. **テスト**: 新機能には必ずテストを追加
3. **ドキュメント**: コードコメントとマークダウンを更新
4. **Pull Request**: 詳細な説明と変更理由を記載

### バグレポート

Issue作成時に以下を含める：
- 再現手順
- 期待される動作
- 実際の動作
- 環境情報（Java, OS, etc.）

---

## ライセンス

[ライセンス情報をここに記載]

---

## お問い合わせ

- **Email**: [連絡先]
- **GitHub Issues**: [GitHubリンク]
- **ディスカッション**: [ディスカッションリンク]

---

**最終更新日**: 2025年11月5日  
**バージョン**: 1.0.0

