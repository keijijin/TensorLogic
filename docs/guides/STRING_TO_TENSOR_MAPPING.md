# 🔤 文字列からテンソルへのマッピング

## 🎯 **核心的な質問**

> **日本語を理解しないTensor Logic エンジンが、「ソクラテス」という日本語をルールのファクトとして解釈する仕組みは何ですか？**

---

## 💡 **答え: 文字列は単なるラベル（キー）**

**TensorLogicEngineは「ソクラテス」という文字列の意味を理解していません。**

エンジンがやっていることは：
1. 文字列を**ただのキー**として扱う
2. キーに対応する**数値（テンソル）**を取り出す
3. 数値に対して演算を実行

---

## 📊 **内部データ構造**

### **TensorLogicEngine.javaの実装**

```java
public class TensorLogicEngine {
    // 事実を保存するMap
    private final Map<String, INDArray> facts = new HashMap<>();
    
    // ルールを保存するMap
    private final Map<String, Rule> rules = new HashMap<>();
    
    /**
     * 事実を追加
     */
    public void addFact(String name, INDArray tensor) {
        facts.put(name, tensor);  // ← 文字列をキーとして保存
    }
    
    /**
     * 事実を取得
     */
    public INDArray getFact(String name) {
        return facts.get(name);  // ← キーで数値を取り出す
    }
}
```

**重要なポイント**:
- `Map<String, INDArray>` → **辞書（キー→値のマッピング）**
- キー（String）: `"socrates_is_human"`, `"ソクラテスは人間"` など
- 値（INDArray）: `[0.98]` などの数値（テンソル）

---

## 🔍 **具体例で理解する**

### **例1: 「ソクラテス」という文字列**

```java
// ファクトを登録
engine.addFact("ソクラテスは人間", Nd4j.create(new double[]{0.98}));

// 内部的にはこうなる:
facts.put("ソクラテスは人間", [0.98]);

// エンジンにとって、これは単なる辞書のエントリ:
{
  "ソクラテスは人間": [0.98]
}
```

### **例2: 意味のない文字列でも同じ**

```java
// こんな文字列でも全く同じように動作する
engine.addFact("abc123xyz", Nd4j.create(new double[]{0.98}));
engine.addFact("🍕🍔🍟", Nd4j.create(new double[]{0.98}));
engine.addFact("socrates_is_human", Nd4j.create(new double[]{0.98}));

// エンジンにとっては全部同じ:
{
  "abc123xyz": [0.98],
  "🍕🍔🍟": [0.98],
  "socrates_is_human": [0.98]
}
```

**エンジンは文字列の意味を理解していない！**

---

## 🧮 **推論時の動作**

### **ルールの適用**

```java
// ルール定義
Rule rule = new Rule(
    "default",
    List.of("ソクラテスは人間", "人間は動物"),  // ← 入力のキー
    "ソクラテスは動物",                        // ← 出力のキー
    Rule.Operation.MODUS_PONENS
);

// エンジンの動作:
// 1. "ソクラテスは人間" というキーで facts から値を取得
INDArray input1 = facts.get("ソクラテスは人間");  // → [0.98]

// 2. "人間は動物" というキーで facts から値を取得
INDArray input2 = facts.get("人間は動物");  // → [0.95]

// 3. 数値に対して演算を実行（MODUS_PONENS = 行列演算）
INDArray result = input1.mmul(input2);  // → [0.93]

// 4. 結果を新しいキーで保存
facts.put("ソクラテスは動物", result);
```

**エンジンがやっていること**:
1. 文字列キーで**辞書検索**
2. 取得した**数値に演算**を適用
3. 結果を別の文字列キーで**保存**

**エンジンがやっていないこと**:
- ❌ 「ソクラテス」が人名だと理解
- ❌ 「人間」と「動物」の関係を理解
- ❌ 日本語の意味を解析

---

## 🗺️ **完全なマッピング例**

### **入力（人間が理解するレベル）**

```
事実:
  - ソクラテスは人間である
  - 人間は動物である

ルール:
  - もしAがBで、BがCなら、AはCである

結論:
  - ソクラテスは動物である
```

### **TensorLogicEngineの内部表現**

```java
// 事実のマッピング（String → INDArray）
facts = {
  "socrates_is_human": [0.98],    // ← 文字列はただのキー
  "human_is_animal": [0.95]       // ← 意味は人間が付与
}

// ルールのマッピング
rules = {
  "rule1": Rule(
    inputs: ["socrates_is_human", "human_is_animal"],  // ← キーを指定
    output: "socrates_is_animal",
    operation: MODUS_PONENS
  )
}

// 推論実行
result = applyRule(
  facts.get("socrates_is_human"),    // [0.98]
  facts.get("human_is_animal")       // [0.95]
)

// 結果を保存
facts.put("socrates_is_animal", result);  // [0.93]
```

---

## 🎭 **比喩で理解する**

### **エンジンは「スーパーのレジ」のようなもの**

```
レジのバーコードリーダー:
  バーコード "4901234567890" を読み取る
  ↓
  データベースで検索: "4901234567890" → 価格: 298円
  ↓
  価格を表示

レジは商品の中身を理解していない！
- チョコレートか
- 日本製か
- 賞味期限は何日か

レジはバーコード（キー）と価格（値）を対応させているだけ。
```

**TensorLogicEngineも同じ**:
```
TensorLogicEngine:
  文字列 "socrates_is_human" を受け取る
  ↓
  辞書で検索: "socrates_is_human" → テンソル: [0.98]
  ↓
  テンソルを使って演算

エンジンは文字列の意味を理解していない！
- ソクラテスが誰か
- 人間とは何か
- なぜこの値なのか

エンジンは文字列（キー）とテンソル（値）を対応させているだけ。
```

---

## 🔑 **キーの命名規則**

### **実際のプロジェクトでの例**

```yaml
# rules/loan-approval-from-drd.yaml
facts:
  - name: applicant_age          # ← 英語のキー
    tensor:
      values: [1.0]
  
  - name: applicant_income       # ← 英語のキー
    tensor:
      values: [0.95]
  
  - name: 申請者の年齢             # ← 日本語のキーも可能
    tensor:
      values: [1.0]
```

**重要**: キーは何でもいい
- 英語: `"applicant_age"`
- 日本語: `"申請者の年齢"`
- 数字: `"fact_1"`
- 絵文字: `"👤_age"`

**エンジンは全て同じように扱う** → 単なる文字列のキー

---

## 🧠 **では、意味はどこで付与されるのか？**

### **答え: 人間が付与する**

```java
// 人間がコードを書くとき、意味のある名前を付ける
engine.addFact("socrates_is_human", Nd4j.create(new double[]{0.98}));
                  ↑
            これは人間のための名前
            エンジンは意味を理解していない

// こう書いても動作は全く同じ
engine.addFact("x1", Nd4j.create(new double[]{0.98}));
```

**意味を理解しているのは**:
1. **開発者**: コードを書く人
2. **YAMLファイル作成者**: ルールを定義する人
3. **ドキュメント**: 説明を書く人

**意味を理解していないのは**:
- ❌ TensorLogicEngine
- ❌ コンピュータ

---

## 📊 **図解: データフロー**

```
┌──────────────────────────────────────────────────┐
│ 人間の理解レベル                                    │
│                                                  │
│ 「ソクラテスは人間である」                          │
│ 「人間は死ぬ」                                     │
│ 「したがって、ソクラテスは死ぬ」                      │
└─────────────┬────────────────────────────────────┘
              │
              │ 人間が変換（YAMLファイルやコード）
              ▼
┌──────────────────────────────────────────────────┐
│ TensorLogicEngineのデータ構造                      │
│                                                  │
│ Map<String, INDArray> facts = {                 │
│   "socrates_is_human": [0.98],  ← 文字列=キー    │
│   "human_is_mortal": [0.95],                    │
│   "socrates_is_mortal": [0.93]                  │
│ }                                               │
│                                                  │
│ ↑ エンジンは文字列の意味を知らない                   │
│ ↑ 単なる辞書のキーとして扱う                         │
└──────────────────────────────────────────────────┘
              │
              │ エンジンが実行する演算
              ▼
┌──────────────────────────────────────────────────┐
│ 純粋な数値演算                                      │
│                                                  │
│ result = mmul([0.98], [0.95])                   │
│        = [0.93]                                 │
│                                                  │
│ ↑ テンソル演算（行列の掛け算など）                    │
│ ↑ 意味は一切関係ない                                │
└──────────────────────────────────────────────────┘
```

---

## 🔬 **実験: 証明してみる**

### **実験1: 意味のない文字列を使う**

```java
// 普通の命名
engine.addFact("socrates_is_human", Nd4j.create(new double[]{0.98}));
engine.addFact("human_is_mortal", Nd4j.create(new double[]{0.95}));

Rule rule1 = new Rule(
    "ns",
    List.of("socrates_is_human", "human_is_mortal"),
    "socrates_is_mortal",
    Rule.Operation.MODUS_PONENS
);

engine.addRule(rule1);
Map<String, INDArray> result1 = engine.forwardChain();
// 結果: socrates_is_mortal → [0.93]

// 意味のない文字列に置き換え
engine.clear();
engine.addFact("abc", Nd4j.create(new double[]{0.98}));
engine.addFact("def", Nd4j.create(new double[]{0.95}));

Rule rule2 = new Rule(
    "ns",
    List.of("abc", "def"),
    "xyz",
    Rule.Operation.MODUS_PONENS
);

engine.addRule(rule2);
Map<String, INDArray> result2 = engine.forwardChain();
// 結果: xyz → [0.93]  ← 全く同じ結果！
```

**結論**: 文字列の意味は計算に影響しない

---

### **実験2: 日本語、英語、数字を混ぜる**

```java
engine.clear();
engine.addFact("事実1", Nd4j.create(new double[]{0.98}));
engine.addFact("fact_2", Nd4j.create(new double[]{0.95}));
engine.addFact("123", Nd4j.create(new double[]{0.90}));

Rule rule = new Rule(
    "mix",
    List.of("事実1", "fact_2"),  // 日本語と英語のミックス
    "123",                      // 数字
    Rule.Operation.CONJUNCTION
);

engine.addRule(rule);
Map<String, INDArray> result = engine.forwardChain();
// 結果: 123 → min(0.98, 0.95) = 0.95

// ← 問題なく動作！
```

**結論**: 任意のUnicode文字列がキーとして使える

---

## 💡 **重要な洞察**

### **1. シンボリックAI vs ニューラルAI**

```
従来のシンボリックAI:
  "ソクラテス" という記号
  ↓
  知識ベースで意味を定義
  ↓
  「古代ギリシャの哲学者」という意味を持つ

TensorLogicEngine:
  "ソクラテス" という文字列
  ↓
  辞書のキー（意味なし）
  ↓
  [0.98] というテンソルに対応
  ↓
  数値演算のみ
```

**TensorLogicEngineは純粋に数値演算エンジン**

---

### **2. セマンティクス（意味）の分離**

```
┌─────────────────────┐
│ セマンティックレイヤー │  ← 人間が理解
│ （意味の層）          │
│ - ソクラテスは人名    │
│ - 人間は生物学的概念  │
└──────────┬──────────┘
           │
           │ マッピング（人間が定義）
           ▼
┌─────────────────────┐
│ シンタックスレイヤー   │  ← エンジンが処理
│ （構文の層）          │
│ - "socrates": [0.98] │
│ - "human": [0.95]    │
└─────────────────────┘
```

**設計の利点**:
- ✅ エンジンがシンプル
- ✅ 任意の言語に対応（日本語、英語、中国語...）
- ✅ 高速な数値演算
- ✅ 意味とロジックの分離

---

## 🎯 **まとめ**

### **質問への最終回答**

> **日本語を理解しないTensor Logic エンジンが、「ソクラテス」という日本語をルールのファクトとして解釈する仕組みは？**

**答え**:

1. **解釈していない**
   - エンジンは「ソクラテス」の意味を理解していない
   - 単なる文字列のキーとして扱っている

2. **辞書（Map）を使用**
   ```java
   Map<String, INDArray> facts = new HashMap<>();
   facts.put("ソクラテスは人間", [0.98]);
   ```

3. **キーで数値を取得**
   ```java
   INDArray value = facts.get("ソクラテスは人間");
   // → [0.98]
   ```

4. **数値に演算を適用**
   ```java
   INDArray result = applyOperation(value1, value2);
   ```

5. **意味は人間が付与**
   - キーの命名は人間が決める
   - エンジンはキーと値を対応させるだけ
   - 計算には意味は一切関係ない

---

## 🔑 **キーポイント**

```
文字列（キー）   →   テンソル（値）
    ↓                   ↓
  意味なし            これが重要
    ↓                   ↓
  単なるラベル        実際の計算対象
```

**TensorLogicEngineの本質**:
- 📦 **辞書**: 文字列 → 数値のマッピング
- 🧮 **演算器**: 数値に対する演算を実行
- 🏷️ **ラベル**: 文字列は人間のためのラベル

---

**作成日**: 2025年11月7日  
**プロジェクト**: TensorLogic 1.0.0  
**核心コード**: `Map<String, INDArray> facts = new HashMap<>();`

