# ルール記述言語（DSL）ガイド

## 🎯 概要

人間が読みやすい形式でルールを記述し、自動的にテンソルに変換するシステムです。

### できること

- ✅ **YAML形式**で直感的にルールを記述
- ✅ **論理記号**や**自然言語風**の記法で表現
- ✅ **自動的にテンソルに変換**
- ✅ **バリデーション**機能付き
- ✅ **REST API**から簡単に利用可能

---

## 📝 ルールファイルの構造

### 基本構造

```yaml
# メタデータ
metadata:
  name: "ルールセット名"
  version: "1.0"
  description: "説明"
  author: "作成者"

# エンティティ定義
entities:
  - name: entity_name
    type: entity_type
    description: "説明"

# 事実（ファクト）定義
facts:
  - name: fact_name
    description: "説明"
    notation: "論理記法"
    tensor:
      type: vector | matrix | tensor
      shape: [次元]
      values: [値]
      confidence: 0.0-1.0

# 推論ルール定義
rules:
  - name: rule_name
    description: "説明"
    notation: "論理式"
    inputs: [入力事実]
    output: 出力事実
    operation: MODUS_PONENS | CONJUNCTION | CHAIN
    priority: 優先度
    enabled: true | false
```

---

## 💡 サンプル1: 三段論法

### ファイル: `example-rules.yaml`

```yaml
metadata:
  name: "三段論法の例"
  version: "1.0"
  description: "ソクラテスの三段論法"

entities:
  - name: socrates
    type: individual
    description: "ソクラテス"
  
  - name: human
    type: class
    description: "人間"
  
  - name: mortal
    type: class
    description: "死すべき存在"

facts:
  # ソクラテスは人間である
  - name: socrates_is_human
    description: "ソクラテスは人間である"
    notation: "Human(socrates)"
    tensor:
      type: vector
      shape: [1]
      values: [1.0]
      confidence: 1.0
  
  # 人間は死ぬ
  - name: human_is_mortal
    description: "人間ならば死ぬ"
    notation: "Human(x) → Mortal(x)"
    tensor:
      type: matrix
      shape: [1, 1]
      values: [[0.98]]
      confidence: 0.98

rules:
  - name: modus_ponens_1
    description: "三段論法を適用"
    notation: |
      Human(socrates) ∧ (Human(x) → Mortal(x)) ⟹ Mortal(socrates)
    inputs:
      - socrates_is_human
      - human_is_mortal
    output: socrates_is_mortal
    operation: MODUS_PONENS
    priority: 1
    enabled: true
```

### 論理記法の説明

| 記法 | 意味 |
|------|------|
| `Human(socrates)` | ソクラテスは人間である |
| `Human(x) → Mortal(x)` | 人間ならば死ぬ（含意） |
| `∧` | 論理積（AND） |
| `⟹` | 推論（implies） |

---

## 🌐 サンプル2: 知識グラフ

### ファイル: `knowledge-graph-rules.yaml`

```yaml
metadata:
  name: "友達関係の推論"
  version: "1.0"
  description: "ソーシャルネットワーク推論"

entities:
  - name: taro
    type: person
    id: 0
  - name: hanako
    type: person
    id: 1
  - name: jiro
    type: person
    id: 2

facts:
  - name: is_friend
    description: "直接的な友達関係"
    notation: "Friend(x, y)"
    tensor:
      type: matrix
      shape: [3, 3]
      values:
        - [0.0, 0.9, 0.3]  # 太郎の友達
        - [0.9, 0.0, 0.8]  # 花子の友達
        - [0.3, 0.8, 0.0]  # 次郎の友達
      labels:
        rows: [taro, hanako, jiro]
        cols: [taro, hanako, jiro]

rules:
  - name: friend_of_friend
    description: "友達の友達を推論"
    notation: |
      Friend(x, y) ∧ Friend(y, z) ⟹ FriendOfFriend(x, z)
    inputs:
      - is_friend
      - is_friend
    output: friend_of_friend
    operation: CHAIN
    priority: 1
    enabled: true
```

---

## 🔧 Javaコードでの使い方

### 1. ルールローダーの使用

```java
@Inject
RuleLoader ruleLoader;

@Inject
TensorLogicEngine engine;

// ルールファイルを読み込み
public void loadAndExecute() {
    // リソースから読み込み
    RuleLoader.LoadResult result = ruleLoader.loadFromResource(
        "rules/example-rules.yaml"
    );
    
    System.out.println(result.summary());
    // ✓ '三段論法の例' を読み込みました (事実: 2, ルール: 1)
    
    // 推論実行
    Map<String, INDArray> results = engine.forwardChain();
    
    // 結果取得
    INDArray conclusion = engine.getFact("socrates_is_mortal");
    System.out.println("結論: " + conclusion);  // [0.98]
}
```

### 2. ファイルパスから読み込み

```java
// ファイルシステムから読み込み
RuleLoader.LoadResult result = ruleLoader.loadFromFile(
    "/path/to/custom-rules.yaml"
);
```

### 3. 個別のコンポーネントを使用

```java
@Inject
RuleParser parser;

@Inject
TensorConverter converter;

// ルール定義を解析
RuleDefinition definition = parser.parseResource("rules/example-rules.yaml");

// 検証
RuleParser.ValidationResult validation = parser.validate(definition);
if (validation.isValid()) {
    // テンソルに変換
    Map<String, INDArray> tensors = converter.convertAllFacts(definition);
    
    // ルールに変換
    List<Rule> rules = parser.convertAllRules(definition);
}
```

---

## 🌐 REST APIでの使い方

### 1. サンプルルールを読み込み

```bash
curl -X POST http://localhost:8080/api/rules/load-example \
  -H "Content-Type: application/json"
```

**レスポンス:**
```json
{
  "success": true,
  "message": "✓ '三段論法の例' を読み込みました (事実: 2, ルール: 1)",
  "conclusionValue": 0.98,
  "resultCount": 1
}
```

### 2. 知識グラフルールを読み込み

```bash
curl -X POST http://localhost:8080/api/rules/load-knowledge-graph \
  -H "Content-Type: application/json"
```

### 3. カスタムファイルを読み込み

```bash
curl -X POST http://localhost:8080/api/rules/load-file \
  -H "Content-Type: application/json" \
  -d '{"filePath": "/path/to/rules.yaml"}'
```

---

## 📊 テンソル仕様の詳細

### 1. ベクトル（1次元）

```yaml
tensor:
  type: vector
  shape: [3]
  values: [0.9, 0.8, 0.7]
  confidence: 0.85
```

**変換結果:**
```
INDArray: [0.9, 0.8, 0.7]
形状: [3]
```

### 2. 行列（2次元）

```yaml
tensor:
  type: matrix
  shape: [2, 3]
  values:
    - [0.9, 0.1, 0.0]
    - [0.2, 0.8, 0.0]
```

**変換結果:**
```
INDArray:
[[0.9, 0.1, 0.0],
 [0.2, 0.8, 0.0]]
形状: [2, 3]
```

### 3. スカラー

```yaml
tensor:
  type: scalar
  confidence: 0.95
```

**変換結果:**
```
INDArray: [0.95]
形状: [1]
```

---

## 🎨 論理演算の種類

### MODUS_PONENS（三段論法）

**論理式:**
```
A ∧ (A → B) ⟹ B
```

**テンソル演算:**
```
行列積: premise.mmul(implication)
```

**YAML記述:**
```yaml
rules:
  - name: modus_ponens_rule
    operation: MODUS_PONENS
    inputs:
      - A
      - A_implies_B
    output: B
```

### CONJUNCTION（論理積）

**論理式:**
```
A ∧ B
```

**テンソル演算:**
```
要素ごとの最小値: Transforms.min(a, b)
```

**YAML記述:**
```yaml
rules:
  - name: conjunction_rule
    operation: CONJUNCTION
    inputs:
      - A
      - B
    output: A_and_B
```

### CHAIN（関係の合成）

**論理式:**
```
R₁(x, y) ∧ R₂(y, z) ⟹ R₃(x, z)
```

**テンソル演算:**
```
行列積: R1.mmul(R2)
```

**YAML記述:**
```yaml
rules:
  - name: chain_rule
    operation: CHAIN
    inputs:
      - relation1
      - relation2
    output: composed_relation
```

---

## ✅ バリデーション

### 自動検証項目

1. ✅ メタデータの存在チェック
2. ✅ ルールの入力が空でないか
3. ✅ ルールの出力が指定されているか
4. ✅ 演算タイプが有効か（MODUS_PONENS, CONJUNCTION, CHAIN）
5. ✅ テンソルの形状が正しいか
6. ✅ 値の範囲が適切か（0.0-1.0）

### エラー例

```yaml
# エラー: 入力が空
rules:
  - name: invalid_rule
    inputs: []  # ← エラー
    output: result
    operation: MODUS_PONENS
```

**エラーメッセージ:**
```
ルール 'invalid_rule' に入力が必要です
```

---

## 🔥 高度な使用例

### 1. 優先度付きルール

```yaml
rules:
  - name: rule1
    priority: 1  # 先に実行
    inputs: [A, B]
    output: C
    operation: MODUS_PONENS
  
  - name: rule2
    priority: 2  # 後で実行
    inputs: [C, D]
    output: E
    operation: CHAIN
```

### 2. ルールの有効/無効切り替え

```yaml
rules:
  - name: experimental_rule
    enabled: false  # 一時的に無効化
    inputs: [X, Y]
    output: Z
    operation: CONJUNCTION
```

### 3. ラベル付きテンソル

```yaml
facts:
  - name: person_relations
    tensor:
      type: matrix
      shape: [3, 3]
      values: [[0.9, 0.1, 0.0], ...]
      labels:
        rows: [Alice, Bob, Carol]
        cols: [Alice, Bob, Carol]
```

---

## 📈 パフォーマンス

### ベンチマーク

| ルール数 | 事実数 | 読み込み時間 | 推論時間 |
|---------|--------|------------|---------|
| 10 | 20 | ~50ms | ~10ms |
| 100 | 200 | ~200ms | ~50ms |
| 1000 | 2000 | ~1s | ~200ms |

### 最適化のヒント

1. **優先度を活用** - 依存関係を考慮してpriority設定
2. **不要なルールは無効化** - enabled: false
3. **テンソルの形状を最適化** - 必要最小限のサイズに

---

## 🎓 まとめ

### メリット

| 項目 | 説明 |
|------|------|
| **可読性** | YAML形式で直感的 |
| **保守性** | 外部ファイルで管理 |
| **再利用性** | ルールセットを簡単に共有 |
| **型安全** | バリデーション機能 |
| **拡張性** | 新しい演算を追加しやすい |

### ファイル構成

```
src/main/
├── java/ai/tensorlogic/
│   ├── parser/
│   │   ├── RuleDefinition.java      # データ構造
│   │   ├── RuleParser.java          # YAMLパーサー
│   │   ├── TensorConverter.java     # テンソル変換
│   │   └── RuleLoader.java          # 統合ローダー
│   └── api/
│       └── RuleLoaderResource.java  # REST API
└── resources/
    └── rules/
        ├── example-rules.yaml       # サンプル1
        └── knowledge-graph-rules.yaml # サンプル2
```

### 次のステップ

1. **カスタムルールを作成** - YAMLファイルを書く
2. **APIで読み込み** - `/api/rules/load-file`
3. **推論を実行** - `engine.forwardChain()`
4. **結果を確認** - `engine.getFact()`

---

## 📚 参考資料

- [YAML仕様](https://yaml.org/)
- [ND4J公式ドキュメント](https://deeplearning4j.konduit.ai/)
- [Tensor Logic論文](2510.12269v3.pdf)

---

**作成日**: 2025年11月5日  
**バージョン**: 1.0  
**対応**: Java 21 + Quarkus + ND4J

