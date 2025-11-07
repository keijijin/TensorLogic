# ルールの記述とテンソル変換の完全ガイド

## 📚 目次

1. [ルールの定義場所](#1-ルールの定義場所)
2. [ルールの構築方法](#2-ルールの構築方法)
3. [テンソルへの変換](#3-テンソルへの変換最重要)
4. [ルールの適用とテンソル演算](#4-ルールの適用とテンソル演算)
5. [完全な実行フロー](#5-完全な実行フロー)
6. [具体的な使用例](#6-具体的な使用例)
7. [LLMとの統合での変換](#7-llmとの統合での変換)
8. [テンソル変換のパターン一覧](#8-テンソル変換のパターン一覧)
9. [まとめ](#9-まとめ)

---

## 🎯 1. ルールの定義場所

### ファイル: `Rule.java`

```java
package ai.tensorlogic.core;

public record Rule(
    List<String> inputs,
    String output,
    Operation operation
) {
    
    public enum Operation {
        MODUS_PONENS,   // 三段論法: A かつ (A→B) から B
        CONJUNCTION,    // 論理積: A ∧ B
        CHAIN,         // 関係の合成: R1 ○ R2
        DISJUNCTION    // 論理和: A ∨ B
    }
    
    // ... Builder パターン
}
```

### ポイント

| 項目 | 説明 |
|------|------|
| **Java 21 Record** | 不変データ構造（イミュータブル） |
| **inputs** | 入力となる事実名のリスト |
| **output** | 出力される事実名 |
| **operation** | 適用する論理演算の種類（列挙型） |

### 論理演算の種類

```java
Operation.MODUS_PONENS   // 三段論法: A ∧ (A→B) ⟹ B
Operation.CONJUNCTION    // 論理積: A ∧ B
Operation.CHAIN         // 関係の合成: R₁ ○ R₂
Operation.DISJUNCTION   // 論理和: A ∨ B
```

---

## 🏗️ 2. ルールの構築方法

### ビルダーパターン

```java
public static class Builder {
    private List<String> inputs;
    private String output;
    private Operation operation = Operation.MODUS_PONENS;
    
    public Builder inputs(String... inputs) {
        this.inputs = List.of(inputs);
        return this;
    }
    
    public Builder output(String output) {
        this.output = output;
        return this;
    }
    
    public Builder operation(Operation operation) {
        this.operation = operation;
        return this;
    }
    
    public Rule build() {
        return new Rule(inputs, output, operation);
    }
}
```

### 使用例

```java
// 三段論法のルールを作成
Rule rule = Rule.builder()
    .inputs("socrates_is_human", "human_is_mortal")
    .output("socrates_is_mortal")
    .operation(Rule.Operation.MODUS_PONENS)
    .build();
```

### メソッドチェーン

```java
Rule.builder()
    .inputs("A", "B")         // 入力を設定
    .output("C")              // 出力を設定
    .operation(Operation.CONJUNCTION)  // 演算を設定
    .build();                 // ルールを生成
```

---

## ⚙️ 3. テンソルへの変換（最重要！）

### ファイル: `LLMTensorLogicIntegration.java`

```java
// LLMの確信度をテンソルに変換
INDArray socratesIsHuman = Nd4j.create(new double[]{llmResponse.confidence()});
INDArray humanIsMortal = Nd4j.create(new double[][]{{0.98}});
INDArray expectedConclusion = Nd4j.create(new double[]{llmResponse.confidence()});
```

### テンソル変換の基本

| Java表記 | テンソル形状 | 意味 | 用途 |
|----------|------------|------|------|
| `new double[]{0.9}` | 1次元ベクトル `[0.9]` | スカラー値 | 確信度、真偽値 |
| `new double[][]{{0.98}}` | 2次元行列 `[[0.98]]` | 1×1行列 | 変換行列 |
| `new double[]{0.9, 0.8, 0.7}` | 1次元ベクトル `[0.9, 0.8, 0.7]` | ベクトル | 複数の確信度 |
| `new double[][]{{0.9, 0.1}, {0.2, 0.8}}` | 2×2行列 | 関係行列 | エンティティ間の関係 |

### ND4Jでのテンソル作成

```java
// 1次元ベクトル
INDArray vector = Nd4j.create(new double[]{0.9});
// 形状: [1]

// 2次元行列
INDArray matrix = Nd4j.create(new double[][]{{0.9, 0.1}, {0.2, 0.8}});
// 形状: [2, 2]

// 3次元テンソル
INDArray tensor3d = Nd4j.create(new double[][][]{
    {{0.9, 0.1}, {0.2, 0.8}},
    {{0.7, 0.3}, {0.4, 0.6}}
});
// 形状: [2, 2, 2]
```

---

## 🔄 4. ルールの適用とテンソル演算

### ファイル: `TensorLogicEngine.java`

```java
private INDArray applyRule(Rule rule) {
    return switch (rule.operation()) {
        case MODUS_PONENS -> {
            // A かつ (A→B) から B を導出
            INDArray premise = facts.get(rule.inputs().get(0));
            INDArray implication = facts.get(rule.inputs().get(1));
            yield premise.mmul(implication);
        }
        case CONJUNCTION -> {
            // A と B の論理積
            INDArray a = facts.get(rule.inputs().get(0));
            INDArray b = facts.get(rule.inputs().get(1));
            yield Transforms.min(a, b);
        }
        case CHAIN -> {
            // 関係の合成（行列の積）
            INDArray a = facts.get(rule.inputs().get(0));
            INDArray b = facts.get(rule.inputs().get(1));
            yield a.mmul(b);
        }
        default -> throw new IllegalArgumentException("Unknown operation: " + rule.operation());
    };
}
```

### テンソル演算の対応表

| 論理演算 | 数学記法 | テンソル演算 | ND4Jメソッド |
|---------|---------|------------|-------------|
| **三段論法** | A ∧ (A→B) ⟹ B | 行列積 | `premise.mmul(implication)` |
| **論理積** | A ∧ B | 要素ごとの最小値 | `Transforms.min(a, b)` |
| **関係の合成** | R₁ ○ R₂ | 行列積 | `a.mmul(b)` |
| **論理和** | A ∨ B | 要素ごとの最大値 | `Transforms.max(a, b)` |

### 計算例：三段論法

```java
// 入力
INDArray A = Nd4j.create(new double[]{0.9});           // [0.9]
INDArray A_implies_B = Nd4j.create(new double[][]{{0.98}});  // [[0.98]]

// 計算
INDArray B = A.mmul(A_implies_B);  // 行列積

// 結果
// B = [0.882]  (0.9 × 0.98)
```

---

## 📝 5. 完全な実行フロー

### フローチャート

```
┌─────────────────────────────────────────────────────────────┐
│ 1. ルールの定義（Rule.java）                                 │
│                                                              │
│    Rule rule = Rule.builder()                               │
│        .inputs("A", "A_implies_B")                          │
│        .output("B")                                         │
│        .operation(Operation.MODUS_PONENS)                   │
│        .build();                                            │
└────────────────┬────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────┐
│ 2. 事実のテンソル変換（LLMTensorLogicIntegration.java）      │
│                                                              │
│    INDArray A = Nd4j.create(new double[]{0.9});            │
│    INDArray A_implies_B = Nd4j.create(new double[][]{{0.98}});│
└────────────────┬────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────┐
│ 3. エンジンに登録（TensorLogicEngine.java）                  │
│                                                              │
│    engine.addFact("A", A);                                  │
│    engine.addFact("A_implies_B", A_implies_B);             │
│    engine.addRule("rule1", rule);                          │
└────────────────┬────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────┐
│ 4. 推論実行（TensorLogicEngine.java）                        │
│                                                              │
│    Map<String, INDArray> results = engine.forwardChain();  │
│                                                              │
│    内部処理:                                                  │
│    → applyRule() を呼び出し                                   │
│    → switch文でOperation判定                                 │
│    → 対応するテンソル演算実行                                  │
│    → premise.mmul(implication)                              │
└────────────────┬────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────┐
│ 5. 結果の取得                                                 │
│                                                              │
│    INDArray B = engine.getFact("B");                       │
│    // B = [0.882] (0.9 × 0.98)                            │
└─────────────────────────────────────────────────────────────┘
```

### コードによるフロー

```java
// ステップ1: エンジンの取得（DIコンテナから）
@Inject
TensorLogicEngine engine;

// ステップ2: 事実をテンソルに変換
INDArray socrates = Nd4j.create(new double[]{1.0});
INDArray humanToMortal = Nd4j.create(new double[][]{{0.98}});

// ステップ3: 事実を登録
engine.addFact("socrates_is_human", socrates);
engine.addFact("human_is_mortal", humanToMortal);

// ステップ4: ルールを定義・登録
Rule rule = Rule.builder()
    .inputs("socrates_is_human", "human_is_mortal")
    .output("socrates_is_mortal")
    .operation(Rule.Operation.MODUS_PONENS)
    .build();
engine.addRule("inference", rule);

// ステップ5: 推論実行
Map<String, INDArray> results = engine.forwardChain();

// ステップ6: 結果取得
INDArray conclusion = engine.getFact("socrates_is_mortal");
System.out.println("結論: " + conclusion);  // [0.98]
```

---

## 💡 6. 具体的な使用例

### 例1: 三段論法（Modus Ponens）

```java
/**
 * 三段論法の例
 * 
 * 前提1: ソクラテスは人間である (確信度: 1.0)
 * 前提2: 人間は死ぬ (確信度: 0.98)
 * 結論: ソクラテスは死ぬ (期待値: 0.98)
 */
public void example1_ModusPonens() {
    // 1. テンソルに変換
    INDArray socrates = Nd4j.create(new double[]{1.0});
    INDArray humanMortal = Nd4j.create(new double[][]{{0.98}});
    
    // 2. 事実を登録
    engine.addFact("socrates_is_human", socrates);
    engine.addFact("human_is_mortal", humanMortal);
    
    // 3. ルールを定義
    Rule rule = Rule.builder()
        .inputs("socrates_is_human", "human_is_mortal")
        .output("socrates_is_mortal")
        .operation(Rule.Operation.MODUS_PONENS)
        .build();
    engine.addRule("inference", rule);
    
    // 4. 推論実行
    engine.forwardChain();
    
    // 5. 結果確認
    INDArray result = engine.getFact("socrates_is_mortal");
    System.out.println("ソクラテスは死ぬ: " + result);  // [0.98]
}
```

### 例2: 知識グラフでの推論

```java
/**
 * 知識グラフの例
 * 
 * エンティティ: [太郎, 花子, 次郎]
 * 関係: 友達関係
 * 推論: 友達の友達を計算
 */
public void example2_KnowledgeGraph() {
    // 1. 友達関係をテンソルで表現
    INDArray friends = Nd4j.create(new double[][]{
        {0.0, 0.9, 0.3},  // 太郎 -> (太郎, 花子, 次郎)
        {0.9, 0.0, 0.8},  // 花子 -> (太郎, 花子, 次郎)
        {0.3, 0.8, 0.0}   // 次郎 -> (太郎, 花子, 次郎)
    });
    
    // 2. 事実を登録
    engine.addFact("is_friend", friends);
    
    // 3. ルール: 友達の友達を計算
    Rule chainRule = Rule.builder()
        .inputs("is_friend", "is_friend")
        .output("friend_of_friend")
        .operation(Rule.Operation.CHAIN)
        .build();
    engine.addRule("friend_chain", chainRule);
    
    // 4. 推論実行
    engine.forwardChain();
    
    // 5. 結果確認
    INDArray result = engine.getFact("friend_of_friend");
    System.out.println("友達の友達:\n" + result);
    /*
     * 太郎→次郎: 0.72 (花子を経由)
     * 花子→太郎: 0.24
     * など
     */
}
```

### 例3: ファジィ論理

```java
/**
 * ファジィ論理の例
 * 
 * 天気の状態から最適な活動を推論
 */
public void example3_FuzzyLogic() {
    // 1. 天気の確率ベクトル
    INDArray weather = Nd4j.create(new double[]{0.6, 0.3, 0.1});
    // [晴れ, 曇り, 雨]
    
    // 2. 天気→活動の関係行列
    INDArray weatherToActivity = Nd4j.create(new double[][]{
        {0.9, 0.3, 0.2},  // 晴れ → (ピクニック, 読書, 映画)
        {0.5, 0.7, 0.6},  // 曇り → (ピクニック, 読書, 映画)
        {0.1, 0.9, 0.9}   // 雨   → (ピクニック, 読書, 映画)
    });
    
    // 3. 事実を登録
    engine.addFact("weather", weather);
    engine.addFact("weather_to_activity", weatherToActivity);
    
    // 4. ルール定義
    Rule rule = Rule.builder()
        .inputs("weather", "weather_to_activity")
        .output("recommended_activity")
        .operation(Rule.Operation.MODUS_PONENS)
        .build();
    engine.addRule("activity_inference", rule);
    
    // 5. 推論実行
    engine.forwardChain();
    
    // 6. 結果確認
    INDArray result = engine.getFact("recommended_activity");
    System.out.println("推奨活動: " + result);
    // [0.70, 0.48, 0.39] → ピクニックが最適
}
```

---

## 🔍 7. LLMとの統合での変換

### ファイル: `LLMTensorLogicIntegration.java`

```java
public VerifiedReasoningResult verifyLLMReasoning(String query) {
    LOG.info("質問を処理: {}", query);
    
    // 1. LLMから回答を取得
    LLMResponse llmResponse = llmService.queryWithReasoning(query);
    LOG.info("LLMの回答: {} (確信度: {})", 
             llmResponse.answer(), 
             llmResponse.confidence());
    
    // 2. LLMの確信度をテンソルに変換 ← ここが重要！
    INDArray socratesIsHuman = Nd4j.create(
        new double[]{llmResponse.confidence()}
    );
    INDArray humanIsMortal = Nd4j.create(
        new double[][]{{0.98}}
    );
    INDArray expectedConclusion = Nd4j.create(
        new double[]{llmResponse.confidence()}
    );
    
    // 3. Tensor Logicで検証
    ValidationResult validation = tensorLogic.validateReasoning(
        socratesIsHuman,
        humanIsMortal,
        expectedConclusion,
        0.2  // 許容誤差20%
    );
    
    LOG.info("検証結果: {} (信頼度: {})", 
        validation.isValid() ? "✓ 妥当" : "✗ 不適切", 
        validation.confidence());
    
    return new VerifiedReasoningResult(
        llmResponse.answer(),
        llmResponse.confidence(),
        llmResponse.reasoningSteps(),
        validation.isValid(),
        validation.confidence(),
        validation
    );
}
```

### 変換フロー

```
┌──────────────────┐
│  LLM             │
│  "ソクラテスは    │
│   死にます"       │
└────────┬─────────┘
         │
         ▼
    確信度: 0.9
         │
         ▼
┌──────────────────┐
│ テンソル変換      │
│ Nd4j.create()    │
└────────┬─────────┘
         │
         ▼
   INDArray [0.9]
         │
         ▼
┌──────────────────┐
│ Tensor Logic     │
│ エンジン          │
└────────┬─────────┘
         │
         ▼
   検証結果
```

### REST API経由での使用

```bash
# APIリクエスト
curl -X POST http://localhost:8080/api/tensor-logic/verify-reasoning \
  -H "Content-Type: application/json" \
  -d '{"query": "ソクラテスは死ぬのか？"}'

# 内部処理
# 1. LLMに質問 → 回答＋確信度取得
# 2. 確信度をdouble値として抽出: 0.9
# 3. Nd4j.create(new double[]{0.9}) でテンソル変換
# 4. Tensor Logicエンジンで検証
# 5. 結果をJSON形式で返却
```

---

## 📊 8. テンソル変換のパターン一覧

### 基本パターン

| データ型 | Java表記 | テンソル形状 | 次元 | 用途 |
|---------|---------|------------|-----|------|
| **スカラー** | `new double[]{0.9}` | `[0.9]` | 1D | 確信度、真偽値 |
| **ベクトル** | `new double[]{0.9, 0.8, 0.7}` | `[0.9, 0.8, 0.7]` | 1D | 複数の確信度 |
| **行列** | `new double[][]{{0.9, 0.1}, {0.2, 0.8}}` | `[[0.9, 0.1], [0.2, 0.8]]` | 2D | 変換行列、関係 |
| **3次元テンソル** | `new double[][][]{...}` | `[n][m][k]` | 3D | 複雑な関係 |

### 実用パターン

#### 1. 確信度ベクトル

```java
// 複数の命題の確信度
INDArray confidences = Nd4j.create(new double[]{
    0.9,  // 命題1の確信度
    0.8,  // 命題2の確信度
    0.7   // 命題3の確信度
});
```

#### 2. 関係行列

```java
// エンティティ間の関係
//        人A   人B   人C
INDArray relations = Nd4j.create(new double[][]{
    {0.0, 0.9, 0.3},  // 人A → (A, B, C)
    {0.9, 0.0, 0.8},  // 人B → (A, B, C)
    {0.3, 0.8, 0.0}   // 人C → (A, B, C)
});
```

#### 3. 変換行列

```java
// 属性から結論への変換
//          結論1  結論2
INDArray transform = Nd4j.create(new double[][]{
    {0.9,  0.1},  // 属性1 → (結論1, 結論2)
    {0.2,  0.8}   // 属性2 → (結論1, 結論2)
});
```

#### 4. バッチ処理

```java
// 複数のサンプルをバッチ処理
//         特徴1  特徴2  特徴3
INDArray batch = Nd4j.create(new double[][]{
    {1.0,  1.0,  0.0},  // サンプル1
    {1.0,  1.0,  0.0},  // サンプル2
    {0.0,  1.0,  1.0},  // サンプル3
    {0.0,  0.0,  1.0}   // サンプル4
});
```

### NumPy vs ND4J 対応表

| 操作 | NumPy (Python) | ND4J (Java) |
|------|----------------|-------------|
| **配列作成** | `np.array([0.9])` | `Nd4j.create(new double[]{0.9})` |
| **行列積** | `np.einsum('i,ij->j', a, b)` | `a.mmul(b)` |
| **要素ごとの最小値** | `np.minimum(a, b)` | `Transforms.min(a, b)` |
| **要素ごとの最大値** | `np.maximum(a, b)` | `Transforms.max(a, b)` |
| **絶対値** | `np.abs(a)` | `Transforms.abs(a)` |
| **平均値** | `a.mean()` | `a.meanNumber().doubleValue()` |
| **形状取得** | `a.shape` | `a.shape()` |
| **転置** | `a.T` | `a.transpose()` |

---

## 🎯 9. まとめ

### 重要ポイント

| # | 場所 | ファイル | 役割 |
|---|------|---------|------|
| **1** | ルール定義 | `Rule.java` (10-21行) | 論理演算の**定義** |
| **2** | ルール構築 | `Rule.java` (26-52行) | ビルダーパターンでの**構築** |
| **3** | テンソル変換 | `LLMTensorLogicIntegration.java` (41-43行) | データの**テンソル変換** ⭐ |
| **4** | 事実登録 | `TensorLogicEngine.java` (30-33行) | テンソルの**登録** |
| **5** | ルール登録 | `TensorLogicEngine.java` (38-41行) | ルールの**登録** |
| **6** | ルール適用 | `TensorLogicEngine.java` (72-94行) | テンソル演算の**実行** |
| **7** | 推論実行 | `TensorLogicEngine.java` (46-67行) | 前向き推論の**実行** |

### 最重要ポイント ⭐

**テンソル変換は主に以下の場所で行われます:**

```java
// ファイル: LLMTensorLogicIntegration.java (41-43行目)
INDArray socratesIsHuman = Nd4j.create(new double[]{llmResponse.confidence()});
INDArray humanIsMortal = Nd4j.create(new double[][]{{0.98}});
INDArray expectedConclusion = Nd4j.create(new double[]{llmResponse.confidence()});
```

### クイックリファレンス

```java
// 1. テンソル作成
INDArray tensor = Nd4j.create(new double[]{0.9});

// 2. ルール作成
Rule rule = Rule.builder()
    .inputs("A", "B")
    .output("C")
    .operation(Rule.Operation.MODUS_PONENS)
    .build();

// 3. エンジンに登録
engine.addFact("A", tensorA);
engine.addRule("rule1", rule);

// 4. 推論実行
Map<String, INDArray> results = engine.forwardChain();

// 5. 結果取得
INDArray result = engine.getFact("C");
```

### 学習の順序

1. ✅ **ルールの定義** を理解する (`Rule.java`)
2. ✅ **テンソル変換** を理解する (`Nd4j.create()`)
3. ✅ **テンソル演算** を理解する (`mmul()`, `Transforms.min()`)
4. ✅ **推論エンジン** を理解する (`TensorLogicEngine.java`)
5. ✅ **LLM統合** を理解する (`LLMTensorLogicIntegration.java`)

---

## 📚 参考資料

- [Tensor Logic論文](2510.12269v3.pdf) - Pedro Domingos
- [ND4J公式ドキュメント](https://deeplearning4j.konduit.ai/nd4j/tutorials/quickstart)
- [Quarkus公式ドキュメント](https://quarkus.io/)
- [Java 21新機能](https://openjdk.org/projects/jdk/21/)

---

**作成日**: 2025年11月5日  
**バージョン**: 1.0  
**対象**: Java 21 + Quarkus + ND4J実装

