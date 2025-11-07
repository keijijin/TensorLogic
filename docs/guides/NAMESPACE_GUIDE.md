# Namespace（ネームスペース）完全ガイド

**最終更新日:** 2025年11月6日  
**バージョン:** 1.0  

---

## 📋 目次

1. [概要](#概要)
2. [なぜネームスペースが必要か](#なぜネームスペースが必要か)
3. [使用方法](#使用方法)
4. [API リファレンス](#api-リファレンス)
5. [実例](#実例)
6. [ベストプラクティス](#ベストプラクティス)

---

## 📚 概要

### Namespace（ネームスペース）とは？

**ネームスペース**は、ルールセットを論理的にグループ化し、名前の衝突を避けるための仕組みです。

```
┌─────────────────────────────────────────┐
│  Tensor Logic Engine                     │
│                                          │
│  ┌────────────────────────────────┐    │
│  │ namespace: "loan-approval"     │    │
│  │  - determine_adult_status      │    │
│  │  - determine_financial_elig..  │    │
│  │  - determine_loan_approval     │    │
│  └────────────────────────────────┘    │
│                                          │
│  ┌────────────────────────────────┐    │
│  │ namespace: "medical-diagnosis" │    │
│  │  - diagnose_flu                │    │
│  │  - check_symptoms              │    │
│  │  - recommend_treatment         │    │
│  └────────────────────────────────┘    │
│                                          │
│  ┌────────────────────────────────┐    │
│  │ namespace: "age-qualification" │    │
│  │  - infer_adult                 │    │
│  │  - infer_can_drive             │    │
│  └────────────────────────────────┘    │
└─────────────────────────────────────────┘
```

### 主な機能

| 機能 | 説明 |
|------|------|
| **分離** | ルールセットを論理的に分離 |
| **フィルタリング** | 特定のネームスペースのルールのみ適用 |
| **名前衝突回避** | 異なるルールセットで同名のルールを使用可能 |
| **ワイルドカード** | `*` または `null` で全ルールを適用 |

---

## 🎯 なぜネームスペースが必要か

### 問題点（ネームスペースなし）

```yaml
# loan-approval-rules.yaml
rules:
  - name: check_eligibility
    output: is_eligible

# medical-diagnosis-rules.yaml
rules:
  - name: check_eligibility  # ← 名前が衝突！
    output: is_eligible       # ← 出力も衝突！
```

**問題:**
- ルール名が衝突
- 意図しないルールが適用される
- 推論結果が不正確

### 解決策（ネームスペース導入）

```yaml
# loan-approval-rules.yaml
metadata:
  namespace: "loan-approval"  # ← ネームスペース指定
rules:
  - name: check_eligibility
    output: is_eligible

# medical-diagnosis-rules.yaml
metadata:
  namespace: "medical-diagnosis"  # ← 別のネームスペース
rules:
  - name: check_eligibility
    output: is_eligible
```

**メリット:**
- ✅ ルールが明確に分離
- ✅ 名前衝突なし
- ✅ 適用するルールセットを選択可能

---

## 🚀 使用方法

### 1. YAMLでネームスペースを定義

```yaml
# src/main/resources/rules/your-rules.yaml

metadata:
  name: "Your Rule Set"
  version: "1.0"
  description: "Description of your rules"
  author: "Your Name"
  namespace: "your-namespace"  # ← ここでネームスペースを指定

facts:
  # ... your facts ...

rules:
  # ... your rules ...
```

**ポイント:**
- `namespace` フィールドは `metadata` セクションに追加
- 省略すると `"default"` が使用される
- 小文字とハイフン推奨（例: `loan-approval`, `medical-diagnosis`）

---

### 2. ルールをロード

```bash
# ルールファイルをロード（ネームスペース自動認識）
curl -X POST http://localhost:8080/api/rules/load-resource \
  -H 'Content-Type: application/json' \
  -d '{"resourcePath": "rules/loan-approval-from-drd.yaml"}'
```

**ログ出力例:**
```
INFO  ネームスペース: loan-approval
INFO  ルール変換完了: 3個のルールを生成
```

---

### 3. Forward Chaining でネームスペース指定

#### **全ネームスペース（デフォルト）**

```bash
# 全ルールを適用（ネームスペース指定なし）
curl -X POST http://localhost:8080/api/verify/simple \
  -H 'Content-Type: application/json' \
  -d '{
    "query": "Your question",
    "ruleFile": "rules/your-rules.yaml"
  }'
```

#### **特定のネームスペースのみ**

```bash
# loan-approvalネームスペースのルールのみ適用
curl -X POST http://localhost:8080/api/verify/simple \
  -H 'Content-Type: application/json' \
  -d '{
    "query": "18歳で年収300万円以上、信用スコアが良好な申請者は融資を受けられますか？",
    "ruleFile": "rules/loan-approval-from-drd.yaml",
    "namespace": "loan-approval"
  }'
```

**ログ出力例:**
```
INFO  推論エンジンで検証を実行（ネームスペース: loan-approval）...
INFO  推論: [applicant_age, age_implies_adult] -> is_adult (namespace: loan-approval)
INFO  推論: [applicant_income, credit_score_good] -> financially_eligible (namespace: loan-approval)
INFO  推論: [is_adult, financially_eligible] -> loan_approved (namespace: loan-approval)
INFO  推論完了: 3個の新しい事実を導出
```

---

### 4. Backward Chaining でネームスペース指定

#### **全ネームスペース（デフォルト）**

```bash
# 全ルールで後向き推論
curl -X POST http://localhost:8080/api/tensor-logic/backward-chain \
  -H 'Content-Type: application/json' \
  -d '{
    "goal": "loan_approved"
  }'
```

#### **特定のネームスペースのみ**

```bash
# loan-approvalネームスペースのルールのみで後向き推論
curl -X POST http://localhost:8080/api/tensor-logic/backward-chain \
  -H 'Content-Type: application/json' \
  -d '{
    "goal": "loan_approved",
    "namespace": "loan-approval"
  }'
```

**結果例:**
```json
{
  "success": true,
  "goal": "loan_approved",
  "goalConfidence": 0.9000,
  "reasoningPath": [
    "applicant_age [既知]",
    "age_implies_adult [既知]",
    "is_adult ← [applicant_age, age_implies_adult] (ns: loan-approval)",
    "applicant_income [既知]",
    "credit_score_good [既知]",
    "financially_eligible ← [applicant_income, credit_score_good] (ns: loan-approval)",
    "loan_approved ← [is_adult, financially_eligible] (ns: loan-approval)"
  ]
}
```

---

### 5. ワイルドカード（全ルール適用）

```bash
# "*" で全ネームスペースのルールを適用
curl -X POST http://localhost:8080/api/verify/simple \
  -H 'Content-Type: application/json' \
  -d '{
    "query": "Your question",
    "ruleFile": "rules/your-rules.yaml",
    "namespace": "*"
  }'

# または namespace を省略
curl -X POST http://localhost:8080/api/verify/simple \
  -H 'Content-Type: application/json' \
  -d '{
    "query": "Your question",
    "ruleFile": "rules/your-rules.yaml"
  }'
```

---

## 💡 実例

### 例1: 融資審査ルール

```yaml
# rules/loan-approval-from-drd.yaml

metadata:
  name: "融資審査ルール（DRD由来）"
  version: "1.1"
  author: "DMN Converter"
  namespace: "loan-approval"  # ← ネームスペース指定

facts:
  - name: applicant_age
    tensor:
      values: [1.0]
  
  - name: age_implies_adult
    tensor:
      values: [[1.0]]

rules:
  - name: determine_adult_status
    inputs:
      - applicant_age
      - age_implies_adult
    output: is_adult
    operation: MODUS_PONENS
```

**使用例:**
```bash
# loan-approvalネームスペースのルールのみ適用
curl -X POST http://localhost:8080/api/verify/simple \
  -H 'Content-Type: application/json' \
  -d '{
    "query": "18歳の申請者は成人ですか？",
    "ruleFile": "rules/loan-approval-from-drd.yaml",
    "namespace": "loan-approval"
  }'
```

---

### 例2: 医療診断ルール

```yaml
# rules/medical-diagnosis-rules.yaml

metadata:
  name: "医療診断ルール"
  version: "1.0"
  author: "Medical Team"
  namespace: "medical-diagnosis"  # ← 別のネームスペース

facts:
  - name: has_fever
    tensor:
      values: [0.9]
  
  - name: has_cough
    tensor:
      values: [0.8]

rules:
  - name: diagnose_flu
    inputs:
      - has_fever
      - has_cough
    output: has_flu
    operation: CONJUNCTION
```

**使用例:**
```bash
# medical-diagnosisネームスペースのルールのみ適用
curl -X POST http://localhost:8080/api/verify/simple \
  -H 'Content-Type: application/json' \
  -d '{
    "query": "発熱と咳がある患者はインフルエンザですか？",
    "ruleFile": "rules/medical-diagnosis-rules.yaml",
    "namespace": "medical-diagnosis"
  }'
```

---

### 例3: 複数ルールセットのロード

```bash
# 複数のルールセットをロード
curl -X POST http://localhost:8080/api/rules/load-resource \
  -d '{"resourcePath": "rules/loan-approval-from-drd.yaml"}'

curl -X POST http://localhost:8080/api/rules/load-resource \
  -d '{"resourcePath": "rules/medical-diagnosis-rules.yaml"}'

curl -X POST http://localhost:8080/api/rules/load-resource \
  -d '{"resourcePath": "rules/age-qualification-rules.yaml"}'

# ルールの確認
curl http://localhost:8080/api/rules/inspect
```

**結果:**
```json
{
  "success": true,
  "count": 9,
  "rules": {
    "determine_adult_status": {
      "namespace": "loan-approval",
      "inputs": ["applicant_age", "age_implies_adult"],
      "output": "is_adult",
      "operation": "MODUS_PONENS"
    },
    "diagnose_flu": {
      "namespace": "medical-diagnosis",
      "inputs": ["has_fever", "has_cough"],
      "output": "has_flu",
      "operation": "CONJUNCTION"
    },
    "infer_adult": {
      "namespace": "age-qualification",
      "inputs": ["taro_is_18", "age18_is_adult"],
      "output": "taro_is_adult",
      "operation": "MODUS_PONENS"
    }
  }
}
```

---

## 📖 API リファレンス

### 1. Forward Chaining with Namespace

**エンドポイント:**
```
POST /api/verify/simple
```

**リクエスト:**
```json
{
  "query": "Your question",
  "ruleFile": "rules/your-rules.yaml",
  "namespace": "your-namespace"  // オプション（省略可）
}
```

**パラメータ:**
- `namespace` (optional): 適用するネームスペース
  - `null` または省略: 全ルールを適用
  - `"*"`: 全ルールを適用（明示的）
  - `"loan-approval"`: loan-approvalネームスペースのみ

---

### 2. Backward Chaining with Namespace

**エンドポイント:**
```
POST /api/tensor-logic/backward-chain
```

**リクエスト:**
```json
{
  "goal": "loan_approved",
  "namespace": "loan-approval"  // オプション（省略可）
}
```

**パラメータ:**
- `namespace` (optional): 適用するネームスペース
  - `null` または省略: 全ルールを適用
  - `"*"`: 全ルールを適用（明示的）
  - `"loan-approval"`: loan-approvalネームスペースのみ

---

### 3. ルールの検査

**エンドポイント:**
```
GET /api/rules/inspect
```

**レスポンス:**
```json
{
  "success": true,
  "count": 9,
  "rules": {
    "rule_name": {
      "namespace": "namespace-name",
      "inputs": [...],
      "output": "...",
      "operation": "..."
    }
  },
  "facts": {...},
  "namespaces": [
    "loan-approval",
    "medical-diagnosis",
    "age-qualification"
  ]
}
```

---

## 🏆 ベストプラクティス

### 1. ネームスペースの命名規則

**推奨:**
```
loan-approval
medical-diagnosis
age-qualification
bird-contradiction
```

**非推奨:**
```
LoanApproval  ← キャメルケースは避ける
loan_approval ← アンダースコアよりハイフン推奨
Loan-Approval ← 大文字は避ける
```

---

### 2. ネームスペースの粒度

**適切な粒度:**
- ✅ `loan-approval`: 融資審査全般
- ✅ `medical-diagnosis`: 医療診断全般
- ✅ `age-qualification`: 年齢と資格全般

**細かすぎる（非推奨）:**
- ❌ `loan-approval-adult-check`: 細かすぎる
- ❌ `loan-approval-income-check`: 細かすぎる

**粗すぎる（非推奨）:**
- ❌ `business-rules`: 粗すぎる
- ❌ `all-rules`: 意味がない

---

### 3. ドキュメントの重要性

```yaml
metadata:
  name: "融資審査ルール（DRD由来）"
  version: "1.1"
  description: "DRDで定義された融資審査プロセスをTensor Logicで実装" # ← 詳細に記載
  author: "DMN Converter"
  namespace: "loan-approval"  # ← 明確なネームスペース
```

---

### 4. テストでのネームスペース指定

```bash
# テスト1: 特定のネームスペースのみ
curl -X POST http://localhost:8080/api/verify/simple \
  -d '{
    "query": "Test query",
    "ruleFile": "rules/test-rules.yaml",
    "namespace": "test-namespace"
  }'

# テスト2: 全ネームスペース
curl -X POST http://localhost:8080/api/verify/simple \
  -d '{
    "query": "Test query",
    "ruleFile": "rules/test-rules.yaml",
    "namespace": "*"
  }'

# テスト3: ネームスペース省略（デフォルト）
curl -X POST http://localhost:8080/api/verify/simple \
  -d '{
    "query": "Test query",
    "ruleFile": "rules/test-rules.yaml"
  }'
```

---

## 📊 使用例の比較

### ケース1: 単一ルールセット

```bash
# loan-approvalルールのみロード
curl -X POST http://localhost:8080/api/rules/load-resource \
  -d '{"resourcePath": "rules/loan-approval-from-drd.yaml"}'

# ネームスペース指定不要（単一ルールセットのため）
curl -X POST http://localhost:8080/api/verify/simple \
  -d '{"query": "...", "ruleFile": "rules/loan-approval-from-drd.yaml"}'
```

---

### ケース2: 複数ルールセット（明示的指定）

```bash
# 複数ルールセットをロード
curl -X POST http://localhost:8080/api/rules/load-resource \
  -d '{"resourcePath": "rules/loan-approval-from-drd.yaml"}'

curl -X POST http://localhost:8080/api/rules/load-resource \
  -d '{"resourcePath": "rules/age-qualification-rules.yaml"}'

# loan-approvalルールのみ適用
curl -X POST http://localhost:8080/api/verify/simple \
  -d '{
    "query": "融資は承認されますか？",
    "namespace": "loan-approval"
  }'

# age-qualificationルールのみ適用
curl -X POST http://localhost:8080/api/verify/simple \
  -d '{
    "query": "運転免許を取得できますか？",
    "namespace": "age-qualification"
  }'
```

---

### ケース3: 複数ルールセット（全適用）

```bash
# 複数ルールセットをロード
curl -X POST http://localhost:8080/api/rules/load-resource \
  -d '{"resourcePath": "rules/loan-approval-from-drd.yaml"}'

curl -X POST http://localhost:8080/api/rules/load-resource \
  -d '{"resourcePath": "rules/age-qualification-rules.yaml"}'

# 全ルールを適用（ネームスペース指定なし）
curl -X POST http://localhost:8080/api/verify/simple \
  -d '{
    "query": "18歳で融資を受けられますか？"
  }'
```

**結果:**
- `loan-approval` と `age-qualification` の両方のルールが適用される
- `is_adult` が両方のネームスペースで計算される可能性

---

## 🔍 トラブルシューティング

### 問題1: ルールが適用されない

**症状:**
```
INFO  推論完了: 0個の新しい事実を導出
```

**原因:**
- ネームスペース指定が間違っている
- ルールファイルに `namespace` が定義されていない

**解決策:**
```bash
# ルールの確認
curl http://localhost:8080/api/rules/inspect

# 正しいネームスペースを指定
curl -X POST http://localhost:8080/api/verify/simple \
  -d '{
    "namespace": "loan-approval"  # ← 正しいネームスペース
  }'
```

---

### 問題2: 意図しないルールが適用される

**症状:**
```
推論: [...] -> is_adult (namespace: age-qualification)
推論: [...] -> is_adult (namespace: loan-approval)
```

**原因:**
- ネームスペース指定を省略している
- ワイルドカード（`*`）を使用している

**解決策:**
```bash
# 特定のネームスペースのみ指定
curl -X POST http://localhost:8080/api/verify/simple \
  -d '{
    "namespace": "loan-approval"  # ← loan-approvalのみ
  }'
```

---

## 🚀 まとめ

### ネームスペースの利点

| 利点 | 説明 |
|------|------|
| **分離** | ルールセットを論理的に分離 |
| **柔軟性** | 適用するルールを選択可能 |
| **保守性** | ルールの管理が容易 |
| **衝突回避** | 名前衝突を防止 |
| **テスト性** | 特定のルールセットのみテスト可能 |

### 使用方法のまとめ

1. **YAMLでネームスペースを定義**: `metadata.namespace`
2. **ルールをロード**: 自動的にネームスペースが認識される
3. **推論時に指定**: `namespace` パラメータ
4. **省略または `*`**: 全ルールを適用

---

**ネームスペース機能により、Tensor Logic Engineがさらに強力で柔軟になりました！** 🎉

**次のステップ:**
1. 既存のルールファイルに `namespace` を追加
2. 新しいルールセットを作成
3. ネームスペースを指定して推論を実行

**Happy Reasoning with Namespaces! 🚀**

