# ルール確認ガイド

## 📊 読み込んだルールの内容を確認する方法

登録されたルールと事実（ファクト）の内容を確認するための様々な方法を提供します。

---

## 🌐 REST API で確認

### 1. 全ルールの一覧表示

```bash
curl http://localhost:8080/api/rules/inspect/rules
```

**レスポンス例:**
```json
{
  "success": true,
  "count": 1,
  "rules": [
    {
      "name": "modus_ponens_1",
      "inputs": ["socrates_is_human", "human_is_mortal"],
      "output": "socrates_is_mortal",
      "operation": "MODUS_PONENS",
      "notation": "socrates_is_human ∧ human_is_mortal ⟹ socrates_is_mortal"
    }
  ]
}
```

### 2. 全事実（ファクト）の一覧表示

```bash
curl http://localhost:8080/api/rules/inspect/facts
```

**レスポンス例:**
```json
{
  "success": true,
  "count": 2,
  "facts": [
    {
      "name": "socrates_is_human",
      "shape": "[1]",
      "dtype": "DOUBLE",
      "preview": "[1.00]",
      "stats": {
        "min": 1.0,
        "max": 1.0,
        "mean": 1.0,
        "std": 0.0
      }
    },
    {
      "name": "human_is_mortal",
      "shape": "[1, 1]",
      "dtype": "DOUBLE",
      "preview": "[[0.98]]",
      "stats": {
        "min": 0.98,
        "max": 0.98,
        "mean": 0.98,
        "std": 0.0
      }
    }
  ]
}
```

### 3. 特定のルールの詳細表示

```bash
curl http://localhost:8080/api/rules/inspect/rules/modus_ponens_1
```

**レスポンス例:**
```json
{
  "success": true,
  "message": "成功",
  "rule": {
    "name": "modus_ponens_1",
    "inputs": ["socrates_is_human", "human_is_mortal"],
    "output": "socrates_is_mortal",
    "operation": "MODUS_PONENS",
    "notation": "socrates_is_human ∧ human_is_mortal ⟹ socrates_is_mortal",
    "inputTensors": [
      {
        "name": "socrates_is_human",
        "shape": "[1]",
        "preview": "[1.00]"
      },
      {
        "name": "human_is_mortal",
        "shape": "[1, 1]",
        "preview": "[[0.98]]"
      }
    ]
  }
}
```

### 4. 特定の事実の詳細表示

```bash
curl http://localhost:8080/api/rules/inspect/facts/socrates_is_human
```

**レスポンス例:**
```json
{
  "success": true,
  "message": "成功",
  "fact": {
    "name": "socrates_is_human",
    "shape": "[1]",
    "dtype": "DOUBLE",
    "fullContent": "[1.00]",
    "stats": {
      "min": 1.0,
      "max": 1.0,
      "mean": 1.0,
      "std": 0.0
    },
    "elementCount": 1
  }
}
```

### 5. システムの状態確認

```bash
curl http://localhost:8080/api/rules/inspect/status
```

**レスポンス例:**
```json
{
  "totalRules": 1,
  "totalFacts": 2,
  "operationCounts": {
    "MODUS_PONENS": 1
  },
  "totalTensorElements": 2,
  "status": "稼働中"
}
```

---

## 💻 Javaコードで確認

### 方法1: Engineから直接取得

```java
@Inject
TensorLogicEngine engine;

// 特定の事実を取得
INDArray fact = engine.getFact("socrates_is_human");
System.out.println("事実: " + fact);

// テンソルの形状
System.out.println("形状: " + Arrays.toString(fact.shape()));

// テンソルの値
System.out.println("値: " + fact.toString());

// 統計情報
System.out.println("最小値: " + fact.minNumber());
System.out.println("最大値: " + fact.maxNumber());
System.out.println("平均値: " + fact.meanNumber());
```

### 方法2: Reflectionでルール一覧を取得

```java
@Inject
TensorLogicEngine engine;

public void inspectRules() throws Exception {
    // Reflectionでprivateフィールドにアクセス
    Field rulesField = TensorLogicEngine.class.getDeclaredField("rules");
    rulesField.setAccessible(true);
    
    @SuppressWarnings("unchecked")
    Map<String, Rule> rules = (Map<String, Rule>) rulesField.get(engine);
    
    // ルールを表示
    rules.forEach((name, rule) -> {
        System.out.println("ルール名: " + name);
        System.out.println("  入力: " + rule.inputs());
        System.out.println("  出力: " + rule.output());
        System.out.println("  演算: " + rule.operation());
        System.out.println();
    });
}
```

### 方法3: カスタムインスペクターメソッド

```java
public class RuleInspector {
    
    @Inject
    TensorLogicEngine engine;
    
    /**
     * ルールの詳細を表示
     */
    public void printRuleDetails(String ruleName) {
        // 実装は RuleInspectorResource を参照
    }
    
    /**
     * 全事実を表形式で表示
     */
    public void printFactsTable() {
        Map<String, INDArray> facts = getFactsFromEngine();
        
        System.out.println("┌─────────────────────┬───────────┬────────────────┐");
        System.out.println("│ 事実名              │ 形状      │ プレビュー      │");
        System.out.println("├─────────────────────┼───────────┼────────────────┤");
        
        facts.forEach((name, tensor) -> {
            System.out.printf("│ %-19s │ %-9s │ %-14s │%n",
                name,
                Arrays.toString(tensor.shape()),
                formatTensor(tensor)
            );
        });
        
        System.out.println("└─────────────────────┴───────────┴────────────────┘");
    }
    
    private String formatTensor(INDArray tensor) {
        if (tensor.length() <= 3) {
            return tensor.toString();
        } else {
            return String.format("[%d elem.]", tensor.length());
        }
    }
}
```

---

## 🎨 コマンドラインツール

### シェルスクリプトで確認

```bash
#!/bin/bash
# inspect-rules.sh

BASE_URL="http://localhost:8080/api/rules/inspect"

echo "===== システム状態 ====="
curl -s "$BASE_URL/status" | jq .

echo ""
echo "===== ルール一覧 ====="
curl -s "$BASE_URL/rules" | jq '.rules[] | {name, operation, notation}'

echo ""
echo "===== 事実一覧 ====="
curl -s "$BASE_URL/facts" | jq '.facts[] | {name, shape, stats}'
```

**使用方法:**
```bash
chmod +x inspect-rules.sh
./inspect-rules.sh
```

---

## 📊 出力フォーマット例

### 1. テーブル形式（人間が読みやすい）

```
┌─────────────────────────┬─────────────┬──────────────┐
│ ルール名                │ 演算タイプ   │ 優先度       │
├─────────────────────────┼─────────────┼──────────────┤
│ modus_ponens_1          │ MODUS_PONENS│ 1            │
│ friend_of_friend        │ CHAIN       │ 2            │
└─────────────────────────┴─────────────┴──────────────┘

┌─────────────────────┬───────────┬─────────────────────┐
│ 事実名              │ 形状      │ 統計情報            │
├─────────────────────┼───────────┼─────────────────────┤
│ socrates_is_human   │ [1]       │ min=1.0, max=1.0   │
│ human_is_mortal     │ [1,1]     │ min=0.98, max=0.98 │
└─────────────────────┴───────────┴─────────────────────┘
```

### 2. グラフ形式（依存関係を表示）

```
socrates_is_human ──┐
                    ├──> [MODUS_PONENS] ──> socrates_is_mortal
human_is_mortal ────┘

is_friend ──┐
            ├──> [CHAIN] ──> friend_of_friend
is_friend ──┘
```

---

## 🔍 高度な確認方法

### 1. ルールの依存関係グラフ

```java
public class RuleDependencyAnalyzer {
    
    public Map<String, List<String>> analyzeDependencies() {
        Map<String, List<String>> dependencies = new HashMap<>();
        
        // 各ルールについて、入力となる事実を記録
        rules.forEach((name, rule) -> {
            dependencies.put(name, rule.inputs());
        });
        
        return dependencies;
    }
    
    public void printDependencyGraph() {
        Map<String, List<String>> deps = analyzeDependencies();
        
        deps.forEach((rule, inputs) -> {
            System.out.println(rule + " depends on:");
            inputs.forEach(input -> 
                System.out.println("  - " + input)
            );
        });
    }
}
```

### 2. テンソルの視覚化

```java
public void visualizeTensor(INDArray tensor) {
    if (tensor.rank() == 1) {
        // 1次元ベクトル → バーチャート風
        for (int i = 0; i < tensor.length(); i++) {
            double value = tensor.getDouble(i);
            int bars = (int) (value * 50);
            System.out.printf("[%d] %s %.3f%n", 
                i, 
                "█".repeat(bars), 
                value
            );
        }
    } else if (tensor.rank() == 2) {
        // 2次元行列 → ヒートマップ風
        System.out.println("行列ヒートマップ:");
        for (int i = 0; i < tensor.rows(); i++) {
            for (int j = 0; j < tensor.columns(); j++) {
                double value = tensor.getDouble(i, j);
                String symbol = value > 0.7 ? "█" : 
                               value > 0.3 ? "▓" : 
                               value > 0.1 ? "░" : "·";
                System.out.print(symbol + " ");
            }
            System.out.println();
        }
    }
}
```

**出力例:**
```
[0] ██████████████████████████████████████████████████ 1.000
[1] ████████████████████████████████████████████████ 0.980
[2] █████████████████████████████ 0.650

行列ヒートマップ:
█ ░ ·
█ · █
· █ ░
```

---

## 📈 統計情報の詳細

### テンソルの統計

```bash
curl http://localhost:8080/api/rules/inspect/facts/is_friend | jq '.fact.stats'
```

**出力:**
```json
{
  "min": 0.0,
  "max": 0.9,
  "mean": 0.433,
  "std": 0.378
}
```

### システム全体の統計

```bash
curl http://localhost:8080/api/rules/inspect/status | jq
```

**出力:**
```json
{
  "totalRules": 3,
  "totalFacts": 5,
  "operationCounts": {
    "MODUS_PONENS": 1,
    "CHAIN": 2
  },
  "totalTensorElements": 102,
  "status": "稼働中"
}
```

---

## 🎯 実用例

### シナリオ1: デバッグ

```bash
# 1. ルールを読み込み
curl -X POST http://localhost:8080/api/rules/load-example

# 2. 登録されたか確認
curl http://localhost:8080/api/rules/inspect/status

# 3. ルールの詳細を確認
curl http://localhost:8080/api/rules/inspect/rules/modus_ponens_1

# 4. 入力テンソルの内容を確認
curl http://localhost:8080/api/rules/inspect/facts/socrates_is_human
```

### シナリオ2: 監視ダッシュボード

```javascript
// JavaScript で定期的にステータスをチェック
setInterval(async () => {
    const response = await fetch('http://localhost:8080/api/rules/inspect/status');
    const status = await response.json();
    
    console.log(`ルール数: ${status.totalRules}`);
    console.log(`事実数: ${status.totalFacts}`);
    console.log(`ステータス: ${status.status}`);
}, 5000);  // 5秒ごと
```

---

## 🔒 セキュリティに関する注意

### Reflection使用時の注意

```java
// privateフィールドへのアクセスは慎重に
Field field = clazz.getDeclaredField("fieldName");
field.setAccessible(true);  // セキュリティマネージャーで制限可能

// 本番環境では、専用のgetterメソッドを追加することを推奨
```

---

## 📚 まとめ

### 確認方法一覧

| 方法 | 用途 | コマンド |
|------|------|---------|
| **REST API** | 外部からの確認 | `curl http://localhost:8080/api/rules/inspect/*` |
| **Javaコード** | プログラムからの確認 | `engine.getFact()` など |
| **シェルスクリプト** | バッチ処理 | `./inspect-rules.sh` |
| **Swagger UI** | インタラクティブ | `http://localhost:8080/swagger-ui` |

### エンドポイント一覧

```
GET /api/rules/inspect/rules          # 全ルール一覧
GET /api/rules/inspect/facts          # 全事実一覧
GET /api/rules/inspect/rules/{name}   # ルール詳細
GET /api/rules/inspect/facts/{name}   # 事実詳細
GET /api/rules/inspect/status         # システム状態
```

---

**作成日**: 2025年11月5日  
**対応バージョン**: Java 21 + Quarkus 3.6

