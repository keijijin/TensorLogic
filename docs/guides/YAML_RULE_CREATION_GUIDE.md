# YAMLルールファイル作成ガイド

**作成日:** 2025年11月6日  
**バージョン:** 1.0

---

## 📚 目次

1. [ルールと事実の本質的な違い](#ルールと事実の本質的な違い)
2. [YAMLファイルの基本構造](#yamlファイルの基本構造)
3. [事実（Facts）の表現方法](#事実factsの表現方法)
4. [ルール（Rules）の表現方法](#ルールrulesの表現方法)
5. [実践例：既存ルールから学ぶ](#実践例既存ルールから学ぶ)
6. [よくある間違いと修正方法](#よくある間違いと修正方法)
7. [チェックリスト](#チェックリスト)

---

## 📖 ルールと事実の本質的な違い

### **事実（Fact）とは**

```
事実 = 「ある時点で確定している情報」
```

**判断基準:**
- ✅ 「AはBである」「Xの値はYである」と言える
- ✅ 数値や確信度で表現できる
- ✅ 観測・測定・入力された情報
- ✅ 推論の結果として得られる情報

**例:**
```
○ 良い例（これは事実）
- 「太郎は18歳である」
- 「今日は雨が降っている」
- 「申請者の年収は300万円以上」
- 「信用スコアは良好」

× 悪い例（これはルール）
- 「雨なら傘を持つ」 → これは条件と結論の関係
- 「18歳以上なら成人」 → これは含意関係
```

### **ルール（Rule）とは**

```
ルール = 「事実から新しい事実を導く変換規則」
```

**判断基準:**
- ✅ 「AならばBである」「AかつBからCが導かれる」と言える
- ✅ 入力（前提）と出力（結論）がある
- ✅ 論理演算を含む
- ✅ 一般的な原則や法則

**例:**
```
○ 良い例（これはルール）
- 「人間かつ(人間→死ぬ) ならば 死ぬ」
- 「雨かつ傘なし ならば 濡れる」
- 「成人かつ財務適格 ならば 融資承認」
- 「年齢18歳以上 かつ (18歳以上→成人) ならば 成人」

× 悪い例（これは事実）
- 「太郎は成人である」 → これは状態
- 「確信度は0.9」 → これは値
```

---

## 🏗️ YAMLファイルの基本構造

```yaml
# ====================================
# メタデータ（必須）
# ====================================
metadata:
  name: "ルールセットの名前"
  version: "1.0"
  description: "このルールセットの説明"
  author: "作成者名"
  namespace: "namespace-name"  # オプションだが推奨

# ====================================
# 事実（Facts）セクション
# ====================================
facts:
  - name: fact_name_1
    description: "事実の説明"
    notation: "数学的表記（オプション）"
    tensor:
      type: vector  # または matrix, scalar
      shape: [1]    # テンソルの形状
      values: [0.9] # 具体的な値
      confidence: 0.9  # オプション

  - name: fact_name_2
    # 簡易記法
    values: [0.8]
    description: "別の事実"

# ====================================
# ルール（Rules）セクション
# ====================================
rules:
  - name: rule_name_1
    description: "ルールの説明"
    notation: "論理記法（オプション）"
    inputs:
      - fact_name_1
      - fact_name_2
    output: derived_fact
    operation: CONJUNCTION  # または MODUS_PONENS, DISJUNCTION, CHAIN
    priority: 1  # オプション
    enabled: true  # オプション（デフォルト: true）

# ====================================
# 期待結果（オプション、テスト用）
# ====================================
expected_results:
  - name: derived_fact
    description: "期待される結果"
    expected_value: 0.8
    tolerance: 0.1
```

---

## 🎯 事実（Facts）の表現方法

### **原則1: 事実は「状態」を表す**

#### **例1: 観測された情報**

```yaml
facts:
  # ✅ 良い例: 測定可能で具体的
  - name: applicant_age
    description: "申請者は18歳以上である"
    values: [1.0]  # 確信度100%（確実）
  
  # ✅ 良い例: 確率的な情報
  - name: credit_score_good
    description: "信用スコアが良好である"
    values: [0.90]  # 確信度90%
```

**ポイント:**
- 観測された事実は確信度100%（`1.0`）
- 評価や判断は確信度を下げる（`0.9`など）

#### **例2: 含意関係（知識として表現）**

```yaml
facts:
  # ✅ 良い例: 含意関係を行列で表現
  - name: age_implies_adult
    description: "18歳以上ならば成人である（民法第4条）"
    notation: "Age >= 18 → Adult"
    tensor:
      type: matrix
      shape: [1, 1]
      values: [[1.0]]  # 法律なので確信度100%
```

**ポイント:**
- 含意関係（A→B）は行列として表現
- 法律や定理など確実なものは`1.0`
- 経験則など不確実なものは`0.9`以下

#### **例3: 複数の属性を持つ事実**

```yaml
facts:
  # ✅ 良い例: ベクトルで複数の側面を表現
  - name: weather_conditions
    description: "天気の状態（晴れ、曇り、雨）"
    tensor:
      type: vector
      shape: [3]
      values: [0.2, 0.3, 0.8]  # [晴れ, 曇り, 雨]
      confidence: 0.9
```

**ポイント:**
- 複数の属性はベクトルで表現
- 合計が1.0である必要はない（確信度の分布ではない）

---

### **事実の分類と表現パターン**

#### **パターン1: 入力事実（Input Facts）**

```yaml
# 外部から与えられる情報
facts:
  - name: user_input_age
    description: "ユーザーが入力した年齢"
    values: [1.0]  # 入力されたデータなので確実
  
  - name: sensor_temperature
    description: "センサーが測定した温度（高温）"
    values: [0.85]  # センサーの精度により確信度が下がる
```

#### **パターン2: 知識事実（Knowledge Facts）**

```yaml
# ドメイン知識や法則
facts:
  - name: all_humans_mortal
    description: "すべての人間は死ぬ（哲学的真理）"
    notation: "∀x (Human(x) → Mortal(x))"
    tensor:
      type: matrix
      shape: [1, 1]
      values: [[0.98]]  # ほぼ確実だが、100%ではない
  
  - name: legal_adult_age
    description: "民法第4条：18歳以上は成人"
    notation: "Age >= 18 → Adult"
    tensor:
      type: matrix
      shape: [1, 1]
      values: [[1.0]]  # 法律なので100%
```

#### **パターン3: 中間事実（Intermediate Facts）**

```yaml
# 推論の途中で計算される事実
# これらは facts セクションに書かず、rules の output として定義
# （自動的に計算されるため）

# ❌ 悪い例: 中間結果を facts に書いてはいけない
facts:
  - name: is_adult  # これは推論で導かれるので書かない
    values: [1.0]

# ✅ 良い例: rules の output として定義
rules:
  - name: determine_adult
    inputs: [age, age_threshold]
    output: is_adult  # ここで定義される
    operation: MODUS_PONENS
```

---

## 🎯 ルール（Rules）の表現方法

### **原則1: ルールは「変換」を表す**

#### **例1: 三段論法（Modus Ponens）**

```yaml
rules:
  # ✅ 良い例: A ∧ (A→B) ⟹ B
  - name: socrates_is_mortal
    description: "ソクラテスは人間、人間は死ぬ、故にソクラテスは死ぬ"
    notation: "Human(Socrates) ∧ (Human→Mortal) ⟹ Mortal(Socrates)"
    inputs:
      - socrates_is_human      # 前提A
      - human_implies_mortal   # 含意 A→B
    output: socrates_is_mortal  # 結論B
    operation: MODUS_PONENS
```

**必要な facts:**
```yaml
facts:
  - name: socrates_is_human
    values: [1.0]
  
  - name: human_implies_mortal
    tensor:
      type: matrix
      shape: [1, 1]
      values: [[0.98]]
```

**結果:**
```
socrates_is_mortal = socrates_is_human × human_implies_mortal
                   = 1.0 × 0.98 = 0.98
```

#### **例2: 論理積（Conjunction）**

```yaml
rules:
  # ✅ 良い例: A ∧ B
  - name: approve_loan
    description: "成人かつ財務適格ならば融資承認"
    notation: "Adult ∧ FinanciallyEligible → LoanApproved"
    inputs:
      - is_adult
      - financially_eligible
    output: loan_approved
    operation: CONJUNCTION  # min(A, B)
```

**必要な facts:**
```yaml
facts:
  - name: is_adult
    values: [1.0]
  
  - name: financially_eligible
    values: [0.9]
```

**結果:**
```
loan_approved = min(is_adult, financially_eligible)
              = min(1.0, 0.9) = 0.9
```

#### **例3: 論理和（Disjunction）**

```yaml
rules:
  # ✅ 良い例: A ∨ B
  - name: transportation_available
    description: "バスまたは電車が利用可能"
    notation: "Bus ∨ Train → Transportation"
    inputs:
      - bus_available
      - train_available
    output: can_commute
    operation: DISJUNCTION  # max(A, B)
```

**必要な facts:**
```yaml
facts:
  - name: bus_available
    values: [0.7]
  
  - name: train_available
    values: [0.9]
```

**結果:**
```
can_commute = max(bus_available, train_available)
            = max(0.7, 0.9) = 0.9
```

#### **例4: 推論の連鎖（Chain）**

```yaml
rules:
  # ✅ 良い例: 複数ステップの推論
  - name: multi_hop_reasoning
    description: "関係の合成"
    notation: "A→B ∧ B→C ⟹ A→C"
    inputs:
      - relation_ab
      - relation_bc
    output: relation_ac
    operation: CHAIN  # 行列積
```

---

## 📚 実践例：既存ルールから学ぶ

### **例1: シンプルな検証ルール**

**ファイル:** `simple-verification-rules.yaml`

```yaml
metadata:
  name: "シンプル検証ルール"
  version: "1.0"
  description: "ソクラテスの死を推論する基本的な三段論法"
  author: "Tensor Logic Team"
  namespace: "simple-verification"

facts:
  # 事実1: ソクラテスは人間である
  - name: socrates_is_human
    description: "ソクラテスは人間である"
    notation: "Human(Socrates)"
    values: [1.0]  # 歴史的事実として確実
  
  # 事実2: すべての人間は死ぬ
  - name: human_implies_mortal
    description: "すべての人間は死ぬ"
    notation: "∀x (Human(x) → Mortal(x))"
    tensor:
      type: matrix
      shape: [1, 1]
      values: [[0.98]]  # 一般的な真理として高い確信度

rules:
  # ルール: 三段論法
  - name: infer_socrates_mortal
    description: "ソクラテスは人間、人間は死ぬ、故にソクラテスは死ぬ"
    notation: "Human(Socrates) ∧ (Human→Mortal) ⟹ Mortal(Socrates)"
    inputs:
      - socrates_is_human
      - human_implies_mortal
    output: socrates_is_mortal
    operation: MODUS_PONENS
    enabled: true

expected_results:
  - name: socrates_is_mortal
    expected_value: 0.98
    tolerance: 0.05
```

**学べるポイント:**
1. ✅ 事実は具体的な命題（「ソクラテスは人間」）
2. ✅ 含意関係も事実として定義（「人間→死ぬ」）
3. ✅ ルールは推論プロセス（三段論法の適用）
4. ✅ 期待結果でテストを定義

---

### **例2: 年齢と資格の推論**

**ファイル:** `age-qualification-rules.yaml`

```yaml
metadata:
  name: "年齢と資格の推論"
  version: "1.1"
  description: "年齢から成人判定と運転資格を推論"
  author: "Tensor Logic Team"
  namespace: "age-qualification"

facts:
  # 入力事実: 太郎の年齢
  - name: taro_age
    description: "太郎は18歳である"
    notation: "Age(太郎) = 18"
    values: [1.0]  # 確定した情報
  
  # 知識事実: 成人の定義
  - name: adult_age_threshold
    description: "18歳以上は成人（民法第4条、2022年4月1日施行）"
    notation: "Age >= 18 → Adult"
    tensor:
      type: matrix
      shape: [1, 1]
      values: [[1.0]]  # 法律なので確実
  
  # 知識事実: 運転免許の条件
  - name: adult_implies_can_drive
    description: "成人ならば運転免許を取得できる"
    notation: "Adult → CanDrive"
    tensor:
      type: matrix
      shape: [1, 1]
      values: [[0.95]]  # ほぼ確実だが例外あり（免許取得には試験が必要）

rules:
  # ルール1: 年齢から成人判定
  - name: determine_adult
    description: "年齢が18歳以上なら成人"
    notation: "Age(太郎) ∧ (Age>=18→Adult) ⟹ Adult(太郎)"
    inputs:
      - taro_age
      - adult_age_threshold
    output: taro_is_adult
    operation: MODUS_PONENS
    priority: 1
  
  # ルール2: 成人から運転資格判定
  - name: determine_can_drive
    description: "成人なら運転免許を取得できる"
    notation: "Adult(太郎) ∧ (Adult→CanDrive) ⟹ CanDrive(太郎)"
    inputs:
      - taro_is_adult
      - adult_implies_can_drive
    output: taro_can_drive
    operation: MODUS_PONENS
    priority: 2

expected_results:
  - name: taro_is_adult
    expected_value: 1.0
    tolerance: 0.01
  
  - name: taro_can_drive
    expected_value: 0.95
    tolerance: 0.05
```

**学べるポイント:**
1. ✅ 多段推論（年齢→成人→運転資格）
2. ✅ 中間結果（`taro_is_adult`）は自動計算される
3. ✅ 優先度（priority）で順序を制御
4. ✅ 法律は確信度1.0、実務は0.95など使い分け

---

### **例3: 融資審査ルール（DRD由来）**

**ファイル:** `loan-approval-from-drd.yaml`

```yaml
metadata:
  name: "融資審査ルール（DRD由来）"
  version: "1.1"
  description: "Decision Requirement Diagramから変換した融資審査プロセス"
  author: "DMN Converter"
  namespace: "loan-approval"

facts:
  # 入力1: 年齢
  - name: applicant_age
    description: "申請者は18歳以上である"
    notation: "Age(applicant) >= 18"
    values: [1.0]
  
  # 知識: 年齢→成人
  - name: age_implies_adult
    description: "18歳以上ならば成人（民法第4条）"
    notation: "Age >= 18 → Adult"
    tensor:
      type: matrix
      shape: [1, 1]
      values: [[1.0]]
  
  # 入力2: 収入
  - name: applicant_income
    description: "申請者の年収は300万円以上"
    notation: "Income(applicant) >= 3000000"
    values: [0.95]  # 書類ベースなので95%
  
  # 入力3: 信用スコア
  - name: credit_score_good
    description: "信用スコアが良好"
    notation: "CreditScore(applicant) > 650"
    values: [0.90]

rules:
  # 決定1: 成人判定
  - name: determine_adult_status
    description: "年齢から成人かどうかを判定"
    notation: "Age(申請者) ∧ (Age>=18→Adult) ⟹ Adult(申請者)"
    inputs:
      - applicant_age
      - age_implies_adult
    output: is_adult
    operation: MODUS_PONENS
    priority: 1
  
  # 決定2: 財務適格性
  - name: determine_financial_eligibility
    description: "収入と信用スコアから財務適格性を判定"
    notation: "Income ∧ CreditScore → FinanciallyEligible"
    inputs:
      - applicant_income
      - credit_score_good
    output: financially_eligible
    operation: CONJUNCTION  # 両方満たす必要がある
    priority: 1
  
  # 決定3: 融資可否
  - name: determine_loan_approval
    description: "成人かつ財務適格であれば融資承認"
    notation: "Adult ∧ FinanciallyEligible → LoanApproved"
    inputs:
      - is_adult
      - financially_eligible
    output: loan_approved
    operation: CONJUNCTION
    priority: 2

expected_results:
  - name: is_adult
    expected_value: 1.0
    tolerance: 0.05
  
  - name: financially_eligible
    expected_value: 0.855  # min(0.95, 0.90)
    tolerance: 0.1
  
  - name: loan_approved
    expected_value: 0.855  # min(1.0, 0.855)
    tolerance: 0.1
```

**学べるポイント:**
1. ✅ ビジネスプロセスの表現
2. ✅ 複数の判定条件（年齢、収入、信用）
3. ✅ 並列実行可能なルール（priority: 1が2つ）
4. ✅ DRDの決定ノード → Rule、入力ノード → Fact

---

### **例4: 矛盾検出ルール**

**ファイル:** `bird-contradiction-rules.yaml`

```yaml
metadata:
  name: "鳥の飛行矛盾検出"
  version: "1.0"
  description: "論理的矛盾を検出する例（ペンギンのパラドックス）"
  author: "Tensor Logic Team"
  namespace: "bird-contradiction"

facts:
  # 前提1: すべての鳥は飛べる
  - name: all_birds_fly
    description: "すべての鳥は飛べる（一般論）"
    notation: "∀x (Bird(x) → CanFly(x))"
    values: [0.9]  # 一般論だが例外あり
  
  # 前提2: ペンギンは鳥である
  - name: penguin_is_bird
    description: "ペンギンは鳥である"
    notation: "Bird(Penguin)"
    values: [1.0]  # 生物学的事実
  
  # 前提3: ペンギンは飛べない
  - name: penguin_cannot_fly
    description: "ペンギンは飛べない"
    notation: "¬CanFly(Penguin)"
    values: [1.0]  # 観察事実

rules:
  # ルール1: 鳥→飛べる を適用
  - name: bird_implies_fly
    description: "ペンギンは鳥なので飛べるはず"
    notation: "Bird(Penguin) ∧ (Bird→Fly) ⟹ Fly(Penguin)"
    inputs:
      - penguin_is_bird
      - all_birds_fly
    output: penguin_should_fly
    operation: MODUS_PONENS
  
  # ルール2: 矛盾検出
  - name: detect_contradiction
    description: "飛べるはずだが飛べない → 矛盾"
    notation: "Fly(Penguin) ∧ ¬Fly(Penguin) → Contradiction"
    inputs:
      - penguin_should_fly
      - penguin_cannot_fly
    output: contradiction_detected
    operation: CONJUNCTION

expected_results:
  - name: penguin_should_fly
    expected_value: 0.9
    tolerance: 0.1
  
  - name: contradiction_detected
    expected_value: 0.9  # 矛盾が検出される
    tolerance: 0.1
```

**学べるポイント:**
1. ✅ 矛盾検出の表現方法
2. ✅ 一般論（0.9）と具体例（1.0）の使い分け
3. ✅ 否定の表現（`cannot_fly`）
4. ✅ 例外の扱い方

---

### **例5: 天気と活動のルール**

**ファイル:** `weather-activity-rules.yaml`

```yaml
metadata:
  name: "天気と活動のルール"
  version: "1.0"
  description: "天気に応じた活動を推論"
  author: "Tensor Logic Team"
  namespace: "weather-activity"

facts:
  # 入力: 天気
  - name: is_sunny
    description: "今日は晴れている"
    notation: "Weather = Sunny"
    values: [0.85]  # 天気予報なので85%
  
  # 知識: 晴れ→外出
  - name: sunny_implies_outdoor
    description: "晴れなら外出するべき"
    notation: "Sunny → GoOutdoor"
    tensor:
      type: matrix
      shape: [1, 1]
      values: [[0.8]]  # 個人の好みもあるので80%

rules:
  - name: decide_outdoor_activity
    description: "晴れなら外出を推奨"
    notation: "Sunny ∧ (Sunny→Outdoor) ⟹ Outdoor"
    inputs:
      - is_sunny
      - sunny_implies_outdoor
    output: should_go_outdoor
    operation: MODUS_PONENS

expected_results:
  - name: should_go_outdoor
    expected_value: 0.68  # 0.85 × 0.8
    tolerance: 0.1
```

**学べるポイント:**
1. ✅ 確率的な情報の扱い（天気予報85%）
2. ✅ 個人の嗜好の反映（80%）
3. ✅ 確信度の伝播計算

---

## ⚠️ よくある間違いと修正方法

### **間違い1: 中間結果を facts に書く**

❌ **悪い例:**
```yaml
facts:
  - name: is_adult
    values: [1.0]  # これは推論で導かれるので書いてはいけない

rules:
  - name: determine_adult
    inputs: [age]
    output: is_adult
    operation: MODUS_PONENS
```

✅ **正しい例:**
```yaml
facts:
  - name: age
    values: [1.0]
  
  - name: age_threshold
    values: [[1.0]]

rules:
  - name: determine_adult
    inputs: [age, age_threshold]
    output: is_adult  # ここで初めて定義される
    operation: MODUS_PONENS
```

---

### **間違い2: ルールを事実として書く**

❌ **悪い例:**
```yaml
facts:
  - name: if_rain_then_wet
    values: [1.0]  # これは関係性なのでルールにすべき
```

✅ **正しい例:**
```yaml
facts:
  - name: is_raining
    values: [1.0]
  
  - name: rain_implies_wet
    tensor:
      type: matrix
      shape: [1, 1]
      values: [[1.0]]

rules:
  - name: infer_wet
    inputs: [is_raining, rain_implies_wet]
    output: is_wet
    operation: MODUS_PONENS
```

---

### **間違い3: テンソルの形状が間違っている**

❌ **悪い例:**
```yaml
facts:
  # MODUS_PONENS には行列が必要
  - name: human_implies_mortal
    values: [0.98]  # ベクトルになっている
```

✅ **正しい例:**
```yaml
facts:
  - name: human_implies_mortal
    tensor:
      type: matrix
      shape: [1, 1]
      values: [[0.98]]  # 行列として定義
```

---

### **間違い4: 演算タイプが不適切**

❌ **悪い例:**
```yaml
rules:
  # A ∧ B には CONJUNCTION を使うべき
  - name: both_conditions
    inputs: [condition_a, condition_b]
    output: result
    operation: MODUS_PONENS  # 間違い！
```

✅ **正しい例:**
```yaml
rules:
  - name: both_conditions
    inputs: [condition_a, condition_b]
    output: result
    operation: CONJUNCTION  # 正しい
```

**演算の使い分け:**
- `MODUS_PONENS`: A ∧ (A→B) ⟹ B（三段論法）
- `CONJUNCTION`: A ∧ B（論理積）
- `DISJUNCTION`: A ∨ B（論理和）
- `CHAIN`: A→B ∧ B→C ⟹ A→C（推論の連鎖）

---

### **間違い5: 入力の数が演算と合わない**

❌ **悪い例:**
```yaml
rules:
  # MODUS_PONENS には2つの入力が必要
  - name: incomplete_rule
    inputs: [premise]  # 1つしかない！
    output: conclusion
    operation: MODUS_PONENS
```

✅ **正しい例:**
```yaml
rules:
  - name: complete_rule
    inputs: [premise, implication]  # 2つ必要
    output: conclusion
    operation: MODUS_PONENS
```

**入力数の要件:**
- `MODUS_PONENS`: 2つ（前提、含意）
- `CONJUNCTION`: 2つ（A、B）
- `DISJUNCTION`: 2つ（A、B）
- `CHAIN`: 2つ（関係1、関係2）

---

### **間違い6: ネームスペースを設定していない**

❌ **悪い例:**
```yaml
metadata:
  name: "マイルール"
  version: "1.0"
  # namespace がない
```

✅ **正しい例:**
```yaml
metadata:
  name: "マイルール"
  version: "1.0"
  namespace: "my-rules"  # 追加
```

**メリット:**
- 複数のルールセットを区別できる
- 特定のネームスペースだけで推論できる
- コードの整理がしやすい

---

## ✅ チェックリスト

YAMLファイルを作成したら、以下をチェック：

### **メタデータ**
- [ ] `name` が設定されている
- [ ] `version` が設定されている
- [ ] `description` がわかりやすい
- [ ] `namespace` が設定されている（推奨）

### **事実（Facts）**
- [ ] すべての事実に `name` がある
- [ ] すべての事実に `values` または `tensor` がある
- [ ] 含意関係は行列（`[[0.9]]`）で表現
- [ ] 観測事実は確信度1.0
- [ ] 中間結果を facts に書いていない

### **ルール（Rules）**
- [ ] すべてのルールに `name` がある
- [ ] `inputs` が正しく設定されている
- [ ] `output` が設定されている
- [ ] `operation` が適切（MODUS_PONENS, CONJUNCTION等）
- [ ] 入力の数が演算と合っている
- [ ] 入力として指定した事実が facts に存在する

### **期待結果（オプション）**
- [ ] `expected_value` が計算可能
- [ ] `tolerance` が適切（通常0.05〜0.1）

### **YAML文法**
- [ ] インデントが正しい（スペース2個）
- [ ] リスト項目に `-` がある
- [ ] 文字列が必要な場所で引用符を使用
- [ ] コロン `:` の後にスペースがある

---

## 📝 テンプレート

### **シンプルなテンプレート**

```yaml
metadata:
  name: "ルールセット名"
  version: "1.0"
  description: "説明"
  author: "作成者"
  namespace: "namespace-name"

facts:
  - name: input_fact_1
    description: "入力事実1"
    values: [1.0]
  
  - name: knowledge_fact
    description: "知識事実（含意）"
    tensor:
      type: matrix
      shape: [1, 1]
      values: [[0.95]]

rules:
  - name: inference_rule
    description: "推論ルール"
    inputs:
      - input_fact_1
      - knowledge_fact
    output: derived_fact
    operation: MODUS_PONENS

expected_results:
  - name: derived_fact
    expected_value: 0.95
    tolerance: 0.1
```

### **複雑なテンプレート（多段推論）**

```yaml
metadata:
  name: "複雑なルールセット"
  version: "1.0"
  description: "多段推論の例"
  author: "作成者"
  namespace: "complex-rules"

facts:
  # ステップ1の入力
  - name: input_a
    values: [1.0]
  
  - name: rule_a_to_b
    tensor:
      type: matrix
      shape: [1, 1]
      values: [[0.9]]
  
  # ステップ2の入力
  - name: input_c
    values: [0.8]
  
  - name: rule_b_and_c_to_d
    values: [0.95]

rules:
  # ステップ1: A → B
  - name: derive_b
    inputs: [input_a, rule_a_to_b]
    output: intermediate_b
    operation: MODUS_PONENS
    priority: 1
  
  # ステップ2: B ∧ C → D
  - name: derive_d
    inputs: [intermediate_b, input_c]
    output: final_d
    operation: CONJUNCTION
    priority: 2

expected_results:
  - name: intermediate_b
    expected_value: 0.9
    tolerance: 0.1
  
  - name: final_d
    expected_value: 0.8  # min(0.9, 0.8)
    tolerance: 0.1
```

---

## 🎓 まとめ

### **事実とルールの判断基準**

```
質問: 「これは事実か？ルールか？」

1. 「〜である」と言える → 事実
   「〜ならば〜である」と言える → ルール

2. 数値で表現できる → 事実
   変換プロセスである → ルール

3. ある時点の状態 → 事実
   状態間の関係 → ルール

4. 観測・測定された → 事実
   推論・計算される → ルール
```

### **設計のコツ**

1. **事実は具体的に**
   - ❌ `condition` → ✅ `age_is_18_or_above`

2. **ルールは明確に**
   - ❌ `process` → ✅ `determine_adult_from_age`

3. **確信度を適切に**
   - 法律・定理: `1.0`
   - 観測事実: `1.0`
   - 一般論: `0.9〜0.95`
   - 経験則: `0.7〜0.9`
   - 不確実: `0.5〜0.7`

4. **ネームスペースで整理**
   - ドメインごとに分ける
   - `loan-approval`, `user-management`等

5. **期待結果でテスト**
   - すべてのルールに期待値を設定
   - 許容誤差を適切に設定

---

## 🚀 次のステップ

1. **テンプレートを使って作成**
   - シンプルなテンプレートから始める
   - 既存のルールを参考にする

2. **ロードしてテスト**
   ```bash
   curl -X POST http://localhost:8080/api/rules/load-resource \
     -H 'Content-Type: application/json' \
     -d '{"resourcePath": "rules/your-rules.yaml"}'
   ```

3. **推論を実行**
   ```bash
   # 前向き推論
   curl http://localhost:8080/api/rules/inspect
   
   # 後向き推論
   curl -X POST http://localhost:8080/api/tensor-logic/backward-chain \
     -H 'Content-Type: application/json' \
     -d '{"goal": "your_goal", "namespace": "your-namespace"}'
   ```

4. **結果を検証**
   - `expected_results` と実際の結果を比較
   - 誤差が大きい場合はルールを見直す

---

**Happy Rule Creating! 🎉**

YAMLルールファイルを通じて、Tensor Logicの力を最大限に活用してください！

