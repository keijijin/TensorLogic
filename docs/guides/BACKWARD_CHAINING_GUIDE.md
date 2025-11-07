# Backward Chaining（後向き推論）完全ガイド

**最終更新日:** 2025年11月6日  
**バージョン:** 1.0  

---

## 📋 目次

1. [概要](#概要)
2. [Forward vs Backward Chaining](#forward-vs-backward-chaining)
3. [実装詳細](#実装詳細)
4. [使用方法](#使用方法)
5. [実例](#実例)
6. [API リファレンス](#api-リファレンス)
7. [ユースケース](#ユースケース)

---

## 📚 概要

### Backward Chaining（後向き推論）とは？

**後向き推論**は、**目標から逆向きに推論**する手法です。「この結論を導くためには何が必要か？」という問いに答えます。

```
┌──────────────────────────────────────┐
│  目標: 「融資が承認される？」          │
└──────────────┬───────────────────────┘
               │ ← 逆向きに探索
               ▼
┌──────────────────────────────────────┐
│  必要条件: 「成人である」 ∧           │
│            「財務適格である」          │
└──────────────┬───────────────────────┘
               │ ← さらに逆向き
               ▼
┌──────────────────────────────────────┐
│  必要条件: 「18歳以上である」 ∧       │
│            「年収300万円以上」 ∧      │
│            「信用スコア良好」          │
└──────────────────────────────────────┘
```

### 主な特徴

| 特徴 | 説明 |
|------|------|
| **目標駆動** | 達成したい目標から逆算 |
| **効率的** | 必要な推論のみ実行 |
| **透明性** | 推論パスが明確 |
| **診断向き** | 「なぜこの結果？」に答える |

---

## 🔄 Forward vs Backward Chaining

### Forward Chaining（前向き推論）

```
[既知の事実] → [ルール適用] → [新しい事実] → [ルール適用] → [結論]
```

**特徴:**
- ✅ すべての導出可能な事実を生成
- ✅ データ駆動
- ❌ 不要な推論も実行される可能性
- **用途:** 探索的推論、すべての結果を知りたい場合

**例:**
```
事実: applicant_age (1.0)
      age_implies_adult (1.0)
      applicant_income (0.95)
      credit_score_good (0.90)

↓ Forward Chaining

結果: is_adult (1.0)
      financially_eligible (0.90)
      loan_approved (0.90)
      ... その他すべての導出可能な事実
```

---

### Backward Chaining（後向き推論）

```
[目標] ← [必要なルール] ← [必要な前提] ← ... ← [既知の事実]
```

**特徴:**
- ✅ 目標に必要な推論のみ実行
- ✅ 目標駆動
- ✅ 推論パスが明確
- **用途:** 診断、説明、効率的な推論

**例:**
```
目標: loan_approved

↓ Backward Chaining

推論パス:
1. loan_approved を生成するには？
   → is_adult (1.0) ∧ financially_eligible が必要
   
2. is_adult を生成するには？
   → applicant_age (1.0) ∧ age_implies_adult (1.0) [既知]
   
3. financially_eligible を生成するには？
   → applicant_income (0.95) ∧ credit_score_good (0.90) [既知]

結果: loan_approved (0.90) ✓ 達成可能
```

---

## 🔧 実装詳細

### アルゴリズム

```java
public BackwardChainingResult backwardChain(String goal) {
    // 1. 目標が既知の事実か確認
    if (facts.containsKey(goal)) {
        return success(goal);
    }
    
    // 2. 目標を生成できるルールを探索
    for (Rule rule : rules) {
        if (rule.output().equals(goal)) {
            
            // 3. ルールの入力を再帰的に解決
            boolean canResolve = true;
            for (String input : rule.inputs()) {
                if (!backwardChain(input).success()) {
                    canResolve = false;
                    break;
                }
            }
            
            // 4. すべての入力が解決できれば、ルールを適用
            if (canResolve) {
                applyRule(rule);
                return success(goal);
            }
        }
    }
    
    // 5. 目標を達成できない
    return failure(goal);
}
```

### 無限ループ防止

```java
Set<String> visited = new HashSet<>();

if (visited.contains(goal)) {
    return true;  // 既に訪問済み
}
visited.add(goal);
```

### 推論パスの記録

```java
List<String> path = new ArrayList<>();

// 事実の記録
path.add(goal + " [既知]");

// ルール適用の記録
path.add(goal + " ← [" + String.join(", ", rule.inputs()) + "]");
```

---

## 🚀 使用方法

### 1. ルールを定義

```yaml
# loan-approval-rules.yaml

facts:
  - name: applicant_age
    tensor:
      type: vector
      shape: [1]
      values: [1.0]
      confidence: 1.0
  
  - name: age_implies_adult
    tensor:
      type: matrix
      shape: [1, 1]
      values: [[1.0]]
      confidence: 1.0
  
  - name: applicant_income
    tensor:
      type: vector
      shape: [1]
      values: [0.95]
      confidence: 0.95
  
  - name: credit_score_good
    tensor:
      type: vector
      shape: [1]
      values: [0.90]
      confidence: 0.90

rules:
  - name: determine_adult_status
    inputs:
      - applicant_age
      - age_implies_adult
    output: is_adult
    operation: MODUS_PONENS
    priority: 1
  
  - name: determine_financial_eligibility
    inputs:
      - applicant_income
      - credit_score_good
    output: financially_eligible
    operation: CONJUNCTION
    priority: 1
  
  - name: determine_loan_approval
    inputs:
      - is_adult
      - financially_eligible
    output: loan_approved
    operation: CONJUNCTION
    priority: 2
```

### 2. ルールをロード

```bash
curl -X POST http://localhost:8080/api/rules/load-resource \
  -H 'Content-Type: application/json' \
  -d '{"resourcePath": "rules/loan-approval-from-drd.yaml"}'
```

### 3. Backward Chainingを実行

```bash
curl -X POST http://localhost:8080/api/tensor-logic/backward-chain \
  -H 'Content-Type: application/json' \
  -d '{"goal": "loan_approved"}'
```

### 4. 結果を確認

```json
{
  "success": true,
  "goal": "loan_approved",
  "goalConfidence": 0.9000,
  "reasoningPath": [
    "applicant_age [既知]",
    "age_implies_adult [既知]",
    "is_adult ← [applicant_age, age_implies_adult]",
    "applicant_income [既知]",
    "credit_score_good [既知]",
    "financially_eligible ← [applicant_income, credit_score_good]",
    "loan_approved ← [is_adult, financially_eligible]"
  ],
  "requiredFacts": {
    "applicant_age": "1.0000",
    "age_implies_adult": "[[1.0000]]",
    "is_adult": "[[1.0000]]",
    "applicant_income": "0.9500",
    "credit_score_good": "0.9000",
    "financially_eligible": "[0.9000]",
    "loan_approved": "[[0.9000]]"
  }
}
```

---

## 💡 実例

### 例1: 融資承認の診断

**シナリオ:** 「融資が承認されるためには何が必要か？」

```bash
# ルールをロード
curl -X POST http://localhost:8080/api/rules/load-resource \
  -H 'Content-Type: application/json' \
  -d '{"resourcePath": "rules/loan-approval-from-drd.yaml"}'

# Backward Chainingを実行
curl -X POST http://localhost:8080/api/tensor-logic/backward-chain \
  -H 'Content-Type: application/json' \
  -d '{"goal": "loan_approved"}'
```

**結果の解釈:**

```
✓ 後向き推論成功: 目標 'loan_approved' は達成可能

推論パス:
  loan_approved ← [is_adult, financially_eligible]
  is_adult ← [applicant_age, age_implies_adult]
  financially_eligible ← [applicant_income, credit_score_good]

必要な事実:
  - applicant_age: 1.0 (18歳以上である)
  - age_implies_adult: 1.0 (18歳以上→成人)
  - applicant_income: 0.95 (年収300万円以上)
  - credit_score_good: 0.90 (信用スコア良好)

結論: loan_approved = 0.90 (90%の確信度で承認)
```

**ビジネス価値:**
- ✅ 融資承認の条件が明確
- ✅ 不足している条件を特定可能
- ✅ 申請者へのフィードバックに使える

---

### 例2: 医療診断の推論

**シナリオ:** 「この病気と診断されるためには何が必要か？」

```yaml
# medical-diagnosis-rules.yaml

facts:
  - name: has_fever
    tensor:
      type: vector
      shape: [1]
      values: [0.9]
      confidence: 0.9
  
  - name: has_cough
    tensor:
      type: vector
      shape: [1]
      values: [0.8]
      confidence: 0.8
  
  - name: fever_and_cough_implies_flu
    tensor:
      type: matrix
      shape: [1, 1]
      values: [[0.85]]
      confidence: 0.85

rules:
  - name: diagnose_flu
    inputs:
      - has_fever
      - has_cough
      - fever_and_cough_implies_flu
    output: has_flu
    operation: CONJUNCTION
```

```bash
# Backward Chaining
curl -X POST http://localhost:8080/api/tensor-logic/backward-chain \
  -H 'Content-Type: application/json' \
  -d '{"goal": "has_flu"}'
```

**結果:**
```json
{
  "success": true,
  "goal": "has_flu",
  "goalConfidence": 0.8000,
  "reasoningPath": [
    "has_fever [既知]",
    "has_cough [既知]",
    "fever_and_cough_implies_flu [既知]",
    "has_flu ← [has_fever, has_cough, fever_and_cough_implies_flu]"
  ],
  "requiredFacts": {
    "has_fever": "0.9000",
    "has_cough": "0.8000",
    "fever_and_cough_implies_flu": "[[0.8500]]",
    "has_flu": "0.8000"
  }
}
```

**医療的価値:**
- ✅ 診断根拠の透明性
- ✅ 必要な検査項目の特定
- ✅ 確信度の明示（80%）

---

### 例3: 資格取得の可否判定

**シナリオ:** 「運転免許を取得できるか？」

```bash
# ルールをロード
curl -X POST http://localhost:8080/api/rules/load-resource \
  -H 'Content-Type: application/json' \
  -d '{"resourcePath": "rules/age-qualification-rules.yaml"}'

# Backward Chainingを実行
curl -X POST http://localhost:8080/api/tensor-logic/backward-chain \
  -H 'Content-Type: application/json' \
  -d '{"goal": "taro_can_drive"}'
```

**結果:**
```json
{
  "success": true,
  "goal": "taro_can_drive",
  "goalConfidence": 0.9500,
  "reasoningPath": [
    "taro_is_18 [既知]",
    "age18_is_adult [既知]",
    "taro_is_adult ← [taro_is_18, age18_is_adult]",
    "adult_can_drive [既知]",
    "taro_can_drive ← [taro_is_adult, adult_can_drive]"
  ],
  "requiredFacts": {
    "taro_is_18": "1.0000",
    "age18_is_adult": "[[1.0000]]",
    "taro_is_adult": "[[1.0000]]",
    "adult_can_drive": "[[0.9500]]",
    "taro_can_drive": "[[0.9500]]"
  }
}
```

**推論の可視化:**
```
taro_can_drive (0.95)
  ↑
  ├─ taro_is_adult (1.0)
  │   ↑
  │   ├─ taro_is_18 (1.0) [既知]
  │   └─ age18_is_adult (1.0) [既知]
  │
  └─ adult_can_drive (0.95) [既知]

結論: 95%の確信度で取得可能
（健康状態等の条件により5%の不確実性）
```

---

## 📖 API リファレンス

### エンドポイント

```
POST /api/tensor-logic/backward-chain
```

### リクエスト

```json
{
  "goal": "loan_approved"
}
```

**パラメータ:**
- `goal` (required): 達成したい目標（事実の名前）

### レスポンス

```json
{
  "success": true,
  "goal": "loan_approved",
  "goalConfidence": 0.9000,
  "reasoningPath": [
    "applicant_age [既知]",
    "age_implies_adult [既知]",
    "is_adult ← [applicant_age, age_implies_adult]",
    "applicant_income [既知]",
    "credit_score_good [既知]",
    "financially_eligible ← [applicant_income, credit_score_good]",
    "loan_approved ← [is_adult, financially_eligible]"
  ],
  "requiredFacts": {
    "applicant_age": "1.0000",
    "age_implies_adult": "[[1.0000]]",
    "is_adult": "[[1.0000]]",
    "applicant_income": "0.9500",
    "credit_score_good": "0.9000",
    "financially_eligible": "[0.9000]",
    "loan_approved": "[[0.9000]]"
  }
}
```

**フィールド:**
- `success`: 推論が成功したかどうか
- `goal`: 達成しようとした目標
- `goalConfidence`: 目標の達成確信度（0.0-1.0）
- `reasoningPath`: 推論パス（目標から前提条件へのトレース）
- `requiredFacts`: 目標達成に必要な事実とその値

### エラーレスポンス

```json
{
  "success": false,
  "goal": "unknown_goal",
  "goalConfidence": 0.0,
  "reasoningPath": [],
  "requiredFacts": {}
}
```

---

## 🎯 ユースケース

### 1. **診断システム**

**問題:** 「なぜこの結論に至ったのか？」

**解決:** Backward Chainingで推論パスを明示

```
目標: システム障害
↓ Backward Chaining
必要条件:
  - サーバーダウン (0.9)
  - ネットワーク切断 (0.8)
  - データベース接続失敗 (0.95)
  
結論: システム障害 (0.8) ← 最も弱い証拠に基づく
```

**メリット:**
- ✅ 根本原因の特定
- ✅ 修復の優先順位付け
- ✅ 説明可能な診断

---

### 2. **コンプライアンスチェック**

**問題:** 「この取引は規制を満たしているか？」

**解決:** 必要な条件を逆算

```
目標: 取引承認
↓ Backward Chaining
必要条件:
  - KYC完了 (1.0)
  - AML審査通過 (0.95)
  - リスクスコア許容範囲 (0.9)
  - 取引金額制限内 (1.0)
  
結論: 取引承認 (0.9)
```

**メリット:**
- ✅ 規制準拠の証明
- ✅ 不足書類の特定
- ✅ 監査証跡

---

### 3. **推薦システム**

**問題:** 「なぜこの商品を推薦したのか？」

**解決:** 推薦理由を説明

```
目標: recommend_product_A
↓ Backward Chaining
必要条件:
  - user_likes_category_electronics (0.85)
  - user_budget_high (0.9)
  - product_A_in_stock (1.0)
  - product_A_highly_rated (0.95)
  
結論: recommend_product_A (0.85)
```

**メリット:**
- ✅ 推薦理由の透明性
- ✅ ユーザー信頼の向上
- ✅ GDPR準拠（説明可能性）

---

### 4. **知識ギャップ分析**

**問題:** 「目標達成に何が不足しているか？」

**解決:** 不足条件の特定

```
目標: project_success
↓ Backward Chaining
必要条件:
  - budget_approved (1.0) ✓
  - team_available (0.5) ✗ 不足！
  - stakeholder_approval (0.3) ✗ 不足！
  - resources_allocated (0.8) △
  
結論: project_success (0.3) ← 最も弱い条件
アクション: チーム確保(優先度高)、ステークホルダー承認(優先度高)
```

**メリット:**
- ✅ ボトルネックの可視化
- ✅ アクションプランの策定
- ✅ リスク管理

---

## 🔬 技術的詳細

### 時間計算量

- **最悪ケース:** O(b^d)
  - b: 各ノードの分岐係数（ルール数）
  - d: 推論の深さ

- **最良ケース:** O(d)
  - 直線的な推論パスの場合

### 空間計算量

- **O(d):** 推論パスの深さに比例
- 訪問済みノードの記録: O(n)
  - n: 探索したノード数

### 最適化手法

1. **メモ化（Memoization）**
   ```java
   Map<String, BackwardChainingResult> cache = new HashMap<>();
   
   if (cache.containsKey(goal)) {
       return cache.get(goal);
   }
   ```

2. **ルールの優先順位**
   ```yaml
   rules:
     - name: high_priority_rule
       priority: 1  # 先に試す
     - name: low_priority_rule
       priority: 10  # 後で試す
   ```

3. **早期終了**
   ```java
   if (goal_confidence < threshold) {
       return earlyExit();
   }
   ```

---

## 📊 Forward vs Backward の使い分け

| シナリオ | 推奨手法 | 理由 |
|---------|---------|------|
| すべての結果を知りたい | Forward | 網羅的探索 |
| 特定の目標を達成したい | Backward | 効率的 |
| 「なぜ？」に答えたい | Backward | 説明可能性 |
| 大量のデータから学習 | Forward | データ駆動 |
| 診断・デバッグ | Backward | 根本原因分析 |
| リアルタイム推論 | Backward | 必要最小限の計算 |
| ルールベースの検証 | Forward | すべてのルール適用 |

---

## 🚀 次のステップ

### 1. **ハイブリッド推論**

Forward と Backward を組み合わせる：

```java
// Forward で可能性を探索
Map<String, INDArray> possibilities = engine.forwardChain();

// Backward で目標達成パスを確認
for (String possibility : possibilities.keySet()) {
    BackwardChainingResult result = engine.backwardChain(possibility);
    if (result.success()) {
        // 達成可能な目標を特定
    }
}
```

### 2. **対話的推論**

ユーザーに不足情報を尋ねる：

```java
BackwardChainingResult result = engine.backwardChain("loan_approved");

if (!result.success()) {
    List<String> missingFacts = result.getMissingFacts();
    // ユーザーに質問: "applicant_age を入力してください"
}
```

### 3. **仮説推論（Abduction）**

観測から仮説を生成：

```
観測: loan_approved = false
↓ Backward Chaining
可能性:
  - is_adult = false （18歳未満？）
  - financially_eligible = false （収入不足？信用スコア低い？）
  
仮説: 申請者は18歳未満または財務的に不適格
```

---

## 📚 参考文献

- Russell, S., & Norvig, P. (2020). *Artificial Intelligence: A Modern Approach* (4th ed.). Pearson.
  - Chapter 9: Inference in First-Order Logic

- Nilsson, N. J. (1998). *Artificial Intelligence: A New Synthesis*. Morgan Kaufmann.
  - Chapter 15: Backward Chaining and Goal-Driven Reasoning

- Tensor Logic 論文: [2510.12269v3.pdf](/Users/kjin/ai/TensorLogic/2510.12269v3.pdf)

---

**このガイドは、Tensor Logic Engineにおける後向き推論の完全なリファレンスです。**

**質問や追加機能のリクエストは、開発チームまでお問い合わせください。** 🚀

