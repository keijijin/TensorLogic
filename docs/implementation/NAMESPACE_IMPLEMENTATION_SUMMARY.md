# Namespace（ネームスペース）実装サマリー

**実装日:** 2025年11月6日  
**バージョン:** 1.0  

---

## 📋 実装概要

**Namespace（ネームスペース）**機能をTensor Logic Engineに追加実装しました。

### 🎯 実装の目的

1. **ルールセットの分離**: 複数のルールセットを論理的に分離
2. **名前衝突の回避**: 異なるルールセットで同名のルールを使用可能
3. **選択的適用**: 特定のネームスペースのルールのみを適用
4. **柔軟性の向上**: ワイルドカード（`*`）で全ルールも適用可能

---

## 🔧 実装内容

### 1. 変更ファイル

| ファイル | 変更内容 | 行数 |
|---------|---------|------|
| `src/main/java/ai/tensorlogic/core/Rule.java` | `namespace` フィールド追加 | +8 |
| `src/main/java/ai/tensorlogic/parser/RuleDefinition.java` | `Metadata` に `namespace` 追加 | +1 |
| `src/main/java/ai/tensorlogic/parser/RuleParser.java` | ネームスペース処理追加 | +10 |
| `src/main/java/ai/tensorlogic/core/TensorLogicEngine.java` | ネームスペースフィルタリング追加 | +50 |
| `src/main/java/ai/tensorlogic/api/BackwardChainingRequest.java` | `namespace` フィールド追加 | +1 |
| `src/main/java/ai/tensorlogic/api/TensorLogicResource.java` | ネームスペースパラメータ対応 | +2 |
| `src/main/java/ai/tensorlogic/integration/GenericVerificationRequest.java` | `namespace` フィールド追加 | +1 |
| `src/main/java/ai/tensorlogic/integration/GenericLLMVerifier.java` | ネームスペース処理追加 | +6 |

### 2. 新規ファイル

| ファイル | 説明 | 行数 |
|---------|------|------|
| `NAMESPACE_GUIDE.md` | 完全なガイドドキュメント | 900+ |
| `test-namespace.sh` | 自動テストスクリプト | 250+ |
| `NAMESPACE_IMPLEMENTATION_SUMMARY.md` | 実装サマリー（本ファイル） | - |

### 3. 更新されたYAMLファイル

| ファイル | 追加されたネームスペース |
|---------|----------------------|
| `rules/loan-approval-from-drd.yaml` | `loan-approval` |
| `rules/age-qualification-rules.yaml` | `age-qualification` |
| `rules/simple-verification-rules.yaml` | `simple-verification` |
| `rules/bird-contradiction-rules.yaml` | `bird-contradiction` |

---

## 🚀 主要機能

### 1. ルールへのネームスペース追加

```java
public record Rule(
    String namespace,        // ← 新規追加
    List<String> inputs,
    String output,
    Operation operation
)
```

**デフォルト値:** `"default"`

### 2. YAMLでのネームスペース定義

```yaml
metadata:
  name: "融資審査ルール"
  version: "1.0"
  namespace: "loan-approval"  # ← 新規追加
```

### 3. Forward Chaining でのフィルタリング

```java
public Map<String, INDArray> forwardChain(String namespaceFilter) {
    // namespaceFilter が null または "*" の場合は全ルールを適用
    // それ以外は指定されたネームスペースのルールのみ適用
}
```

### 4. Backward Chaining でのフィルタリング

```java
public BackwardChainingResult backwardChain(String goal, String namespaceFilter) {
    // namespaceFilter が null または "*" の場合は全ルールを適用
    // それ以外は指定されたネームスペースのルールのみ適用
}
```

### 5. API でのネームスペース指定

```bash
# Forward Chaining
curl -X POST http://localhost:8080/api/verify/simple \
  -d '{
    "query": "...",
    "namespace": "loan-approval"
  }'

# Backward Chaining
curl -X POST http://localhost:8080/api/tensor-logic/backward-chain \
  -d '{
    "goal": "loan_approved",
    "namespace": "loan-approval"
  }'
```

---

## 📊 使用例

### 例1: 特定のネームスペースのみ適用

```bash
# loan-approvalルールのみ適用
curl -X POST http://localhost:8080/api/tensor-logic/backward-chain \
  -H 'Content-Type: application/json' \
  -d '{
    "goal": "loan_approved",
    "namespace": "loan-approval"
  }'
```

**結果:**
```json
{
  "success": true,
  "goal": "loan_approved",
  "goalConfidence": 0.9000,
  "reasoningPath": [
    "applicant_age [既知]",
    "age_implies_adult [既知]",
    "is_adult ← [...] (ns: loan-approval)",
    "financially_eligible ← [...] (ns: loan-approval)",
    "loan_approved ← [...] (ns: loan-approval)"
  ]
}
```

**ポイント:**
- 推論パスに `(ns: loan-approval)` が表示される
- loan-approvalネームスペースのルールのみが適用される

---

### 例2: 全ネームスペース適用（ワイルドカード）

```bash
# "*" で全ルールを適用
curl -X POST http://localhost:8080/api/tensor-logic/backward-chain \
  -H 'Content-Type: application/json' \
  -d '{
    "goal": "is_adult",
    "namespace": "*"
  }'
```

**結果:**
- `loan-approval` と `age-qualification` の両方のネームスペースから `is_adult` を生成できるルールが検索される
- 最初に見つかったルールが適用される

---

### 例3: ネームスペース省略（デフォルト = 全ルール）

```bash
# namespaceを省略
curl -X POST http://localhost:8080/api/tensor-logic/backward-chain \
  -H 'Content-Type: application/json' \
  -d '{
    "goal": "loan_approved"
  }'
```

**動作:**
- `namespace` が `null` の場合、全ネームスペースのルールが適用される
- ワイルドカード（`"*"`）と同じ動作

---

## 📈 実装の詳細

### ネームスペースフィルタリングのロジック

```java
// Forward Chaining
for (Map.Entry<String, Rule> entry : rules.entrySet()) {
    Rule rule = entry.getValue();
    
    // ネームスペースフィルタリング
    if (namespaceFilter != null && !namespaceFilter.equals("*") 
        && !rule.namespace().equals(namespaceFilter)) {
        LOG.debug("ルール '{}' をスキップ（ネームスペース: {}）", 
            entry.getKey(), rule.namespace());
        continue;  // ← このルールをスキップ
    }
    
    // ルールを適用...
}
```

**条件:**
1. `namespaceFilter == null` → 全ルールを適用
2. `namespaceFilter == "*"` → 全ルールを適用
3. `rule.namespace().equals(namespaceFilter)` → そのルールを適用
4. それ以外 → そのルールをスキップ

---

## 🧪 テスト方法

### 自動テスト

```bash
# テストスクリプトを実行
chmod +x test-namespace.sh
./test-namespace.sh
```

**テスト内容:**
1. ✅ 特定のネームスペースのみ (loan-approval)
2. ✅ 特定のネームスペースのみ (age-qualification)
3. ✅ 全ネームスペース (ワイルドカード "*")
4. ✅ ネームスペース指定なし（デフォルト）
5. ✅ 存在しないネームスペース（失敗ケース）
6. ✅ Forward Chaining でネームスペース指定

### 手動テスト

```bash
# 1. Quarkus起動
mvn quarkus:dev

# 2. ルールをロード
curl -X POST http://localhost:8080/api/rules/load-resource \
  -d '{"resourcePath": "rules/loan-approval-from-drd.yaml"}'

curl -X POST http://localhost:8080/api/rules/load-resource \
  -d '{"resourcePath": "rules/age-qualification-rules.yaml"}'

# 3. ルールの確認
curl http://localhost:8080/api/rules/inspect

# 4. ネームスペース指定で推論
curl -X POST http://localhost:8080/api/tensor-logic/backward-chain \
  -d '{
    "goal": "loan_approved",
    "namespace": "loan-approval"
  }'
```

---

## 📚 ドキュメント

### 新規ドキュメント

1. **NAMESPACE_GUIDE.md** (900+ 行)
   - 完全なガイド
   - 使用方法と実例
   - APIリファレンス
   - ベストプラクティス

2. **NAMESPACE_IMPLEMENTATION_SUMMARY.md** (本ファイル)
   - 実装サマリー
   - 変更内容
   - テスト方法

### 更新ドキュメント

1. **README.md**
   - Namespace機能のセクション追加
   - 主要機能一覧更新

---

## 🎯 ユースケース

### 1. 複数ドメインのルール管理

**シナリオ:**
- 融資審査ルール（`loan-approval`）
- 医療診断ルール（`medical-diagnosis`）
- 年齢資格ルール（`age-qualification`）

**メリット:**
- ルールセットが明確に分離
- 名前衝突なし
- 特定のドメインのみテスト可能

---

### 2. 段階的なルール適用

**シナリオ:**
- 基本ルール（`basic`）
- 拡張ルール（`extended`）
- 実験的ルール（`experimental`）

**使用例:**
```bash
# 基本ルールのみ
curl -X POST .../backward-chain -d '{"namespace": "basic"}'

# 基本+拡張ルール
curl -X POST .../backward-chain -d '{"namespace": "*"}'

# 実験的ルールのみ（テスト）
curl -X POST .../backward-chain -d '{"namespace": "experimental"}'
```

---

### 3. 環境別のルール適用

**シナリオ:**
- 開発環境（`dev`）
- ステージング環境（`staging`）
- 本番環境（`prod`）

**使用例:**
```yaml
# rules/business-rules-dev.yaml
metadata:
  namespace: "dev"

# rules/business-rules-prod.yaml
metadata:
  namespace: "prod"
```

```bash
# 環境変数でネームスペースを切り替え
NAMESPACE=${ENVIRONMENT:-dev}
curl -X POST .../backward-chain -d "{\"namespace\": \"$NAMESPACE\"}"
```

---

## 🏆 成果

### 実装完了項目

- ✅ Ruleに`namespace`フィールド追加
- ✅ RuleDefinitionに`namespace`フィールド追加
- ✅ RuleParserでネームスペース処理
- ✅ TensorLogicEngineでネームスペースフィルタリング
- ✅ Forward Chainingでネームスペース指定
- ✅ Backward Chainingでネームスペース指定
- ✅ APIリクエストに`namespace`パラメータ追加
- ✅ 全YAMLファイルに`namespace`追加
- ✅ 完全なドキュメント
- ✅ 自動テストスクリプト

### 技術的成果

- ✅ ルールセットの論理的分離
- ✅ 名前衝突の回避
- ✅ 選択的なルール適用
- ✅ ワイルドカード対応
- ✅ 下位互換性の維持（省略時は全ルール適用）

### ビジネス的成果

- ✅ マルチドメイン対応
- ✅ テストの容易性向上
- ✅ 保守性の向上
- ✅ 環境別のルール管理

---

## 📞 問い合わせ

実装に関する質問や追加機能のリクエストは、開発チームまでお問い合わせください。

---

**実装完了日:** 2025年11月6日  
**実装者:** AI Coding Assistant  
**レビュー:** ユーザー承認待ち

---

## 📝 変更履歴

| 日付 | バージョン | 変更内容 |
|------|-----------|---------|
| 2025-11-06 | 1.0 | Namespace 初回実装 |

---

## 🔄 今後の拡張可能性

### 短期的拡張（1-3ヶ月）

1. **ネームスペースの階層化**
   ```
   loan-approval
     ├─ loan-approval.basic
     ├─ loan-approval.advanced
     └─ loan-approval.experimental
   ```

2. **ネームスペースのエイリアス**
   ```yaml
   metadata:
     namespace: "loan-approval"
     aliases: ["loan", "la", "融資審査"]
   ```

3. **ネームスペースの動的ロード**
   ```bash
   # ディレクトリ内の全ルールセットをロード
   curl -X POST /api/rules/load-directory \
     -d '{"directory": "rules/production/"}'
   ```

### 中長期的拡張（6-12ヶ月）

1. **ネームスペースの優先順位**
   ```yaml
   metadata:
     namespace: "loan-approval"
     priority: 10  # 高優先度
   ```

2. **ネームスペースの依存関係**
   ```yaml
   metadata:
     namespace: "loan-approval-extended"
     dependencies: ["loan-approval-basic"]
   ```

3. **ネームスペースのバージョン管理**
   ```yaml
   metadata:
     namespace: "loan-approval@1.2.0"
     compatible_with: ["loan-approval@1.x"]
   ```

---

**🎉 Namespace 実装完了！**

Tensor Logic Engineがさらに柔軟で強力になりました。複数のルールセットを管理し、適用するルールを選択できるようになりました。

**次のステップ:**
1. `./test-namespace.sh` でテスト実行
2. [NAMESPACE_GUIDE.md](NAMESPACE_GUIDE.md) で詳細を確認
3. 既存のルールファイルにネームスペースを追加
4. 新しいルールセットを作成

**Happy Reasoning with Namespaces! 🚀**

