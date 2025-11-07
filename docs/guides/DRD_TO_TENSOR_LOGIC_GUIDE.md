# DRD → Tensor Logic 変換ガイド

## 概要

Decision Requirement Diagram (DRD) で定義されたビジネスルールを、Tensor Logic の YAML ルールファイルに変換する方法を説明します。

---

## DRD と Tensor Logic の対応関係

### 基本的なマッピング

| DRD要素 | DMN XML | Tensor Logic YAML | 説明 |
|---------|---------|------------------|------|
| **Input Data** | `<inputData>` | `facts:` | 入力となる事実・データ |
| **Decision** | `<decision>` | `rules:` | 推論ルール |
| **Knowledge Source** | `<knowledgeSource>` | `metadata:`, `business_rules:` | ルールの根拠・出典 |
| **Information Requirement** | `<informationRequirement>` | `inputs:` | ルールへの入力 |
| **Knowledge Requirement** | `<knowledgeRequirement>` | `operation:` | 使用する演算タイプ |
| **Decision Table** | `<decisionTable>` | `tensor.values:` | 確率値・信頼度 |

---

## 変換手順

### ステップ1: DRD構造の分析

```xml
<!-- DMN XML の例 -->
<definitions>
  <inputData id="age" name="年齢"/>
  <inputData id="income" name="収入"/>
  
  <decision id="adult" name="成人判定">
    <informationRequirement>
      <requiredInput href="#age"/>
    </informationRequirement>
  </decision>
  
  <decision id="approval" name="融資可否">
    <informationRequirement>
      <requiredDecision href="#adult"/>
    </informationRequirement>
  </decision>
</definitions>
```

### ステップ2: 依存関係の抽出

```
依存グラフ:
age → adult → approval
income → approval
```

### ステップ3: Tensor Logic YAML生成

```yaml
facts:
  - name: age_input
    description: "年齢データ"
    tensor:
      values: [1.0]

rules:
  - name: adult_decision
    inputs:
      - age_input
    output: is_adult
    operation: MODUS_PONENS
    priority: 1
  
  - name: approval_decision
    inputs:
      - is_adult
      - income_input
    output: loan_approved
    operation: CONJUNCTION
    priority: 2
```

---

## Decision Table の変換

### DMN Decision Table

```xml
<decisionTable>
  <input label="年齢">
    <inputExpression typeRef="number"/>
  </input>
  <output label="成人">
    <outputValues>
      <text>"はい","いいえ"</text>
    </outputValues>
  </output>
  <rule>
    <inputEntry>
      <text>>= 20</text>
    </inputEntry>
    <outputEntry>
      <text>"はい"</text>
    </outputEntry>
  </rule>
</decisionTable>
```

### Tensor Logic 変換

```yaml
rules:
  - name: age_to_adult
    description: "年齢から成人判定"
    inputs:
      - age_input
    output: is_adult
    operation: MODUS_PONENS
    decision_table:
      # ルール1: age >= 20 → adult = true (probability: 1.0)
      - condition: ">= 20"
        input_range: [0.9, 1.0]
        output: 1.0
        confidence: 1.0
      # ルール2: age < 20 → adult = false (probability: 0.0)
      - condition: "< 20"
        input_range: [0.0, 0.2]
        output: 0.0
        confidence: 1.0
```

---

## 確率値の設定ガイドライン

### Input Data の確率

| データソース | 推奨確率 | 理由 |
|------------|---------|------|
| **システムデータ** | 0.95-1.0 | DBから取得、高信頼性 |
| **ユーザー入力** | 0.80-0.95 | 入力ミスの可能性 |
| **推定値** | 0.60-0.80 | 計算・推定による |
| **外部API** | 0.70-0.90 | 外部依存、可用性 |

### Decision の確率

| Decision タイプ | 推奨確率 | 理由 |
|----------------|---------|------|
| **法的要件** | 0.95-1.0 | 法律で明確に規定 |
| **ビジネスルール** | 0.85-0.95 | 社内規定、例外あり |
| **経験則** | 0.70-0.85 | 過去データに基づく |
| **推測** | 0.50-0.70 | 不確実性が高い |

---

## 演算タイプの選択

### DRD パターン → Tensor Logic 演算

| DRD パターン | 説明 | Tensor Logic 演算 |
|-------------|------|-----------------|
| **A → B** | AならばB | `MODUS_PONENS` |
| **A ∧ B → C** | AかつB | `CONJUNCTION` |
| **A ∨ B → C** | AまたはB | `DISJUNCTION` |
| **A → B → C** | 推論の連鎖 | `CHAIN` |

---

## 実践例: 完全な変換プロセス

### 元のDRD

```
[顧客年齢] ────┐
             ├──> [成人判定] ────┐
[顧客収入] ────┤                ├──> [クレジットカード発行]
             └──> [収入十分] ────┘
[信用履歴] ──────────────────────┘
```

### 変換後のTensor Logic YAML

```yaml
metadata:
  name: "クレジットカード発行判定"
  source_drd: "credit-card-approval.drd"

facts:
  - name: customer_age
    description: "顧客年齢: 25歳"
    tensor:
      values: [1.0]  # 確実なデータ
  
  - name: customer_income
    description: "年収: 400万円"
    tensor:
      values: [0.95]  # 自己申告のため
  
  - name: credit_history
    description: "信用履歴: 良好"
    tensor:
      values: [0.90]  # 過去データ

rules:
  # Decision: 成人判定
  - name: age_check
    inputs: [customer_age]
    output: is_adult
    operation: MODUS_PONENS
    priority: 1
  
  # Decision: 収入十分
  - name: income_check
    inputs: [customer_income]
    output: sufficient_income
    operation: MODUS_PONENS
    priority: 1
  
  # Decision: カード発行
  - name: card_approval
    inputs:
      - is_adult
      - sufficient_income
      - credit_history
    output: card_issued
    operation: CONJUNCTION  # すべて満たす必要
    priority: 2

expected_results:
  - name: card_issued
    expected_value: 0.855  # min(1.0, 0.95, 0.90)
    tolerance: 0.1
```

---

## 自動変換ツールの設計

### 入力: DMN XML

```xml
<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="https://www.omg.org/spec/DMN/20191111/MODEL/">
  <inputData id="input_age" name="年齢"/>
  <decision id="decision_adult" name="成人判定">
    <informationRequirement>
      <requiredInput href="#input_age"/>
    </informationRequirement>
    <decisionTable>...</decisionTable>
  </decision>
</definitions>
```

### 出力: Tensor Logic YAML

```yaml
metadata:
  name: "Auto-generated from DMN"
  source: "model.dmn"

facts:
  - name: input_age
    tensor:
      values: [1.0]

rules:
  - name: decision_adult
    inputs: [input_age]
    output: is_adult
    operation: MODUS_PONENS
```

---

## 変換ツールの実装案

### Java実装（Quarkusに統合）

```java
@Path("/api/converter")
public class DmnToTensorLogicConverter {
    
    @POST
    @Path("/dmn-to-yaml")
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.TEXT_PLAIN)
    public String convertDmnToYaml(String dmnXml) {
        // 1. DMN XMLをパース
        DmnModel model = parseDmn(dmnXml);
        
        // 2. DRD構造を分析
        DrdGraph graph = analyzeDrd(model);
        
        // 3. Tensor Logic YAMLを生成
        String yaml = generateTensorLogicYaml(graph);
        
        return yaml;
    }
}
```

### Python実装（スタンドアロンツール）

```python
import xml.etree.ElementTree as ET
import yaml

def convert_dmn_to_tensor_logic(dmn_file, output_file):
    # DMN XMLを読み込み
    tree = ET.parse(dmn_file)
    root = tree.getroot()
    
    # Input Dataを抽出
    facts = extract_facts(root)
    
    # Decisionを抽出
    rules = extract_rules(root)
    
    # YAMLを生成
    tensor_logic = {
        'metadata': extract_metadata(root),
        'facts': facts,
        'rules': rules
    }
    
    with open(output_file, 'w') as f:
        yaml.dump(tensor_logic, f, allow_unicode=True)
```

---

## ベストプラクティス

### 1. トレーサビリティの確保

```yaml
metadata:
  source_drd: "original-model.drd"
  conversion_date: "2025-11-05"
  converter_version: "1.0"

rules:
  - name: decision_1
    source_drd_id: "Decision_12345"
    source_drd_name: "成人判定"
```

### 2. ビジネスルールの文書化

```yaml
business_rules:
  - id: "BR-001"
    description: "20歳以上を成人とする"
    source: "民法第4条"
    effective_date: "2022-04-01"
```

### 3. バージョン管理

```yaml
metadata:
  version: "2.1"
  changelog:
    - version: "2.1"
      date: "2025-11-05"
      changes: "収入基準を300万円から350万円に変更"
    - version: "2.0"
      date: "2025-10-01"
      changes: "信用スコアの計算方法を変更"
```

---

## まとめ

### DRD → Tensor Logic変換の利点

1. ✅ **視覚的設計**: DRDで直感的にルールを設計
2. ✅ **標準化**: DMN標準に準拠
3. ✅ **トレーサビリティ**: ビジネスルールとコードの紐付け
4. ✅ **自動化**: 変換ツールで効率化
5. ✅ **検証可能**: LLMと組み合わせて自動検証

### 今後の拡張

- [ ] DMN Decision Tableの完全サポート
- [ ] FEEL式のTensor Logic演算への変換
- [ ] ビジュアルエディタの統合
- [ ] リアルタイム同期（DRD ⟷ Tensor Logic）

---

## 参考資料

- [DMN Specification](https://www.omg.org/spec/DMN/)
- [Tensor Logic Paper](https://arxiv.org/abs/2510.12269)
- [Camunda DMN](https://camunda.com/dmn/)

