# ルール自動ロード機能ガイド

**作成日:** 2025年11月6日  
**バージョン:** 1.0

---

## 📋 概要

Tensor Logicアプリケーションは、起動時に`src/main/resources/rules/`ディレクトリ内の**全ての`.yaml`ルールファイルを自動的にロード**します。

### **主な特徴**

- ✅ **自動検出**: `rules/`ディレクトリ内の全`.yaml`ファイルを検出
- ✅ **自動ロード**: 検出されたファイルを起動時に自動ロード
- ✅ **設定可能**: `application.yaml`で有効/無効を切り替え可能
- ✅ **エラーハンドリング**: 一部のファイルが失敗しても他のファイルはロード
- ✅ **詳細ログ**: ロード状況を詳細にログ出力

---

## 🚀 使い方

### **1. ルールファイルを配置**

`src/main/resources/rules/`ディレクトリに`.yaml`ファイルを配置するだけです。

```
src/main/resources/
└── rules/
    ├── simple-verification-rules.yaml
    ├── age-qualification-rules.yaml
    ├── loan-approval-from-drd.yaml
    ├── bird-contradiction-rules.yaml
    └── your-custom-rules.yaml  ← 新しいルールファイル
```

### **2. アプリケーションを起動**

```bash
mvn quarkus:dev
```

または

```bash
java -jar target/quarkus-app/quarkus-run.jar
```

### **3. 起動ログで確認**

```
========================================
🚀 ルールの自動ロードを開始します
========================================
📁 5個のルールファイルが見つかりました
📥 ロード中: rules/age-qualification-rules.yaml
  ✅ 年齢と資格の推論: 成功 (事実: 3, ルール: 2)
📥 ロード中: rules/bird-contradiction-rules.yaml
  ✅ 鳥の飛行矛盾検出: 成功 (事実: 3, ルール: 2)
📥 ロード中: rules/loan-approval-from-drd.yaml
  ✅ 融資審査ルール（DRD由来）: 成功 (事実: 4, ルール: 3)
📥 ロード中: rules/simple-verification-rules.yaml
  ✅ シンプル検証ルール: 成功 (事実: 2, ルール: 1)
📥 ロード中: rules/weather-activity-rules.yaml
  ✅ 天気と活動のルール: 成功 (事実: 2, ルール: 1)
========================================
✨ ルールの自動ロード完了
  成功: 5
  失敗: 0
  合計: 5
========================================
```

---

## ⚙️ 設定

### **自動ロードの有効/無効**

`src/main/resources/application.yaml`:

```yaml
tensor:
  logic:
    rules:
      auto-load:
        enabled: true  # true: 有効, false: 無効
```

### **無効化する場合**

```yaml
tensor:
  logic:
    rules:
      auto-load:
        enabled: false
```

または環境変数で:

```bash
export TENSOR_LOGIC_RULES_AUTO_LOAD_ENABLED=false
mvn quarkus:dev
```

---

## 📝 ルールファイルの作成

### **基本構造**

```yaml
metadata:
  name: "サンプルルール"
  description: "これはサンプルです"
  version: "1.0"
  author: "あなたの名前"
  namespace: "sample"  # ネームスペース（オプション）

facts:
  fact_a:
    values: [0.9]
    description: "事実A"
  
  fact_b:
    values: [0.8]
    description: "事実B"

rules:
  - name: "rule_1"
    inputs: ["fact_a", "fact_b"]
    output: "fact_c"
    operation: "CONJUNCTION"  # MODUS_PONENS, CONJUNCTION, DISJUNCTION, CHAIN
    description: "AとBからCを導出"
    enabled: true

expected_results:
  - output: "fact_c"
    expected_value: 0.8
```

### **サポートされる演算**

| 演算 | 説明 | 例 |
|------|------|-----|
| `MODUS_PONENS` | 三段論法 (A, A→B ⇒ B) | `min(A, A→B)` |
| `CONJUNCTION` | 論理積 (A ∧ B) | `min(A, B)` |
| `DISJUNCTION` | 論理和 (A ∨ B) | `max(A, B)` |
| `CHAIN` | 推論の連鎖 | 複数ステップの推論 |

---

## 🔍 トラブルシューティング

### **問題1: ルールファイルが見つからない**

**症状:**
```
⚠️  rules/ ディレクトリにルールファイルが見つかりませんでした
```

**原因:**
- `src/main/resources/rules/`ディレクトリが存在しない
- `.yaml`拡張子のファイルがない

**解決策:**
```bash
# ディレクトリを作成
mkdir -p src/main/resources/rules

# サンプルルールをコピー
cp src/main/resources/rules/simple-verification-rules.yaml \
   src/main/resources/rules/my-rules.yaml
```

---

### **問題2: 特定のファイルのロードが失敗**

**症状:**
```
❌ rules/my-rules.yaml: 失敗 - 検証エラー: ...
```

**原因:**
- YAMLの文法エラー
- 必須フィールドの欠落
- 不正な演算タイプ

**解決策:**
1. YAMLの文法を確認
2. [既存のルールファイル](src/main/resources/rules/)を参考にする
3. ログの詳細エラーメッセージを確認

---

### **問題3: 自動ロードが動作しない**

**症状:**
ルールが全くロードされない

**原因:**
- 自動ロードが無効化されている
- 設定ファイルの問題

**解決策:**
```yaml
# application.yaml を確認
tensor:
  logic:
    rules:
      auto-load:
        enabled: true  # これが false になっていないか確認
```

---

## 💡 ベストプラクティス

### **1. ファイル名の命名規則**

```
✅ 良い例:
- user-authentication-rules.yaml
- product-validation-rules.yaml
- loan-approval-rules.yaml

❌ 悪い例:
- rules.yaml              (具体性がない)
- temp.yaml               (一時的なものに見える)
- test123.yaml            (意味不明)
```

### **2. ネームスペースの使用**

関連するルールを同じネームスペースでグループ化:

```yaml
metadata:
  namespace: "loan-approval"  # 融資関連のルール

# 他のファイル
metadata:
  namespace: "user-management"  # ユーザー管理関連のルール
```

### **3. バージョン管理**

```yaml
metadata:
  version: "1.2.0"
  description: "v1.2.0: 新しい条件を追加"
```

### **4. テストルールの分離**

```
rules/
├── production/      # 本番用ルール
│   ├── rule1.yaml
│   └── rule2.yaml
└── test/           # テスト用ルール（自動ロード対象外）
    ├── test1.yaml
    └── test2.yaml
```

自動ロードは`rules/`直下のみを対象とするため、サブディレクトリは無視されます。

---

## 🎯 高度な使用例

### **例1: 開発環境とプロダクション環境で異なるルールセット**

**開発環境** (`application-dev.yaml`):
```yaml
tensor:
  logic:
    rules:
      auto-load:
        enabled: true  # 全ルールをロード
```

**本番環境** (`application-prod.yaml`):
```yaml
tensor:
  logic:
    rules:
      auto-load:
        enabled: false  # 手動で必要なルールのみロード
```

実行:
```bash
# 開発環境
mvn quarkus:dev -Dquarkus.profile=dev

# 本番環境
java -jar target/quarkus-app/quarkus-run.jar -Dquarkus.profile=prod
```

---

### **例2: 特定のルールのみを起動後にロード**

自動ロードを無効にして、APIで個別にロード:

```yaml
tensor:
  logic:
    rules:
      auto-load:
        enabled: false
```

```bash
# アプリケーション起動後、必要なルールのみロード
curl -X POST http://localhost:8080/api/rules/load-resource \
  -H 'Content-Type: application/json' \
  -d '{"resourcePath": "rules/specific-rule.yaml"}'
```

---

### **例3: カスタムロードロジック**

自動ロードを無効にして、独自のロードロジックを実装:

```java
@ApplicationScoped
public class CustomRuleLoader {
    
    @Inject
    RuleLoader ruleLoader;
    
    void onStart(@Observes StartupEvent ev) {
        // 条件に応じてロード
        if (isProductionEnvironment()) {
            ruleLoader.loadFromResource("rules/production-rules.yaml");
        } else {
            ruleLoader.loadFromResource("rules/dev-rules.yaml");
        }
    }
}
```

---

## 📚 関連ドキュメント

- [ルール定義ガイド](RULE_DEFINITION_GUIDE.md)
- [ネームスペースガイド](NAMESPACE_GUIDE.md)
- [Tensor Logic Engineガイド](TENSOR_LOGIC_ENGINE_GUIDE.md)
- [REST APIリファレンス](http://localhost:8080/swagger-ui)

---

## 🎉 まとめ

### **自動ロード機能の利点**

✅ **簡単**: ファイルを配置するだけ  
✅ **自動**: 起動時に自動的にロード  
✅ **柔軟**: 設定で有効/無効を切り替え可能  
✅ **堅牢**: エラーハンドリングとログ出力  
✅ **拡張性**: カスタムロジックも実装可能  

### **推奨されるワークフロー**

1. **開発時**: 自動ロードを有効にして、全ルールをロード
2. **テスト時**: 自動ロードで全ルールをテスト
3. **本番時**: 必要に応じて自動ロードまたは手動ロードを選択

---

**Tensor Logicで、ルールベースの推論を簡単に！** 🚀

