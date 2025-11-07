# Tensor Logic テストガイド

**作成日:** 2025年11月6日  
**バージョン:** 1.0  

---

## 📋 目次

1. [概要](#概要)
2. [テストの実行](#テストの実行)
3. [テストの構成](#テストの構成)
4. [各テストクラスの説明](#各テストクラスの説明)
5. [カバレッジ](#カバレッジ)
6. [トラブルシューティング](#トラブルシューティング)

---

## 📚 概要

このプロジェクトには、**JUnit 5** と **Quarkus Test** を使用した包括的なテストスイートが含まれています。

### テストの種類

| 種類 | 説明 | テストクラス数 |
|------|------|-------------|
| **ユニットテスト** | 個別コンポーネントのテスト | 5 |
| **統合テスト** | 複数コンポーネントの連携テスト | 1 |
| **API テスト** | REST APIエンドポイントのテスト | 1 |

### テスト対象コンポーネント

- ✅ TensorLogicEngine (Forward/Backward Chaining, Namespace)
- ✅ Rule (各種演算)
- ✅ RuleParser & RuleLoader
- ✅ Camel Routes
- ✅ LLMService
- ✅ REST API Endpoints
- ✅ エンドツーエンド統合

---

## 🚀 テストの実行

### 全テストの実行

```bash
# Mavenで全テストを実行
mvn test

# または Quarkus CLIを使用
quarkus test
```

**出力例:**
```
[INFO] Tests run: 45, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---

### 特定のテストクラスのみ実行

```bash
# TensorLogicEngineのテストのみ
mvn test -Dtest=TensorLogicEngineTest

# REST APIのテストのみ
mvn test -Dtest=TensorLogicResourceTest

# 統合テストのみ
mvn test -Dtest=IntegrationTest
```

---

### 特定のテストメソッドのみ実行

```bash
# 特定のテストメソッド
mvn test -Dtest=TensorLogicEngineTest#testForwardChaining_ModusPonens

# パターンマッチング
mvn test -Dtest=TensorLogicEngineTest#test*Chaining*
```

---

### 継続的なテスト実行（開発モード）

```bash
# Quarkus開発モードでテストを継続実行
mvn quarkus:dev

# または
quarkus dev

# その後、ターミナルで 'r' を押すとテストが実行される
```

---

### カバレッジレポートの生成

```bash
# JaCoCo カバレッジレポートを生成
mvn clean test jacoco:report

# レポートの場所
# target/site/jacoco/index.html
```

---

## 📦 テストの構成

### ディレクトリ構造

```
src/test/
├── java/
│   └── ai/
│       └── tensorlogic/
│           ├── TensorLogicEngineTest.java       # エンジンのテスト
│           ├── RuleParserTest.java              # パーサーのテスト
│           ├── TensorLogicRoutesTest.java       # Camelルートのテスト
│           ├── TensorLogicResourceTest.java     # REST APIのテスト
│           ├── LLMServiceTest.java              # LLMサービスのテスト
│           └── IntegrationTest.java             # 統合テスト
└── resources/
    └── application.properties                   # テスト用設定
```

---

## 📖 各テストクラスの説明

### 1. TensorLogicEngineTest

**目的:** TensorLogicEngineの全機能をテスト

**テスト内容:**
- ✅ 事実の追加と取得
- ✅ ルールの追加
- ✅ Forward Chaining (MODUS_PONENS, CONJUNCTION, DISJUNCTION)
- ✅ Backward Chaining (成功/失敗ケース)
- ✅ ネームスペースフィルタリング
- ✅ 複数ステップの推論

**テスト数:** 12

**実行例:**
```bash
mvn test -Dtest=TensorLogicEngineTest
```

**重要なテスト:**
- `testForwardChaining_ModusPonens`: 三段論法の検証
- `testBackwardChaining_Success`: 後向き推論の動作確認
- `testForwardChaining_NamespaceFiltering`: ネームスペース機能の検証

---

### 2. RuleParserTest

**目的:** ルールパーサーとローダーのテスト

**テスト内容:**
- ✅ YAMLファイルの読み込み
- ✅ ルール定義の変換
- ✅ ルール定義の検証
- ✅ 複数ルールセットのロード
- ✅ 演算タイプの変換
- ✅ enabledフラグの処理

**テスト数:** 8

**実行例:**
```bash
mvn test -Dtest=RuleParserTest
```

**重要なテスト:**
- `testParseResource`: YAMLファイルの正常読み込み
- `testConvertAllRules`: ルールの変換処理
- `testLoadFromResource`: リソースからのロード

---

### 3. TensorLogicRoutesTest

**目的:** Apache Camelルートのテスト

**テスト内容:**
- ✅ Camelコンテキストの起動確認
- ✅ 各ルートの存在確認
- ✅ SEDA キューの設定確認
- ✅ エラーハンドリングの設定確認
- ✅ Producer Templateの動作確認

**テスト数:** 8

**実行例:**
```bash
mvn test -Dtest=TensorLogicRoutesTest
```

**重要なテスト:**
- `testCamelContextStarted`: Camelの正常起動
- `testVerifyLlmReasoningRouteExists`: 主要ルートの存在確認

---

### 4. TensorLogicResourceTest

**目的:** REST APIエンドポイントのテスト

**テスト内容:**
- ✅ ヘルスチェックエンドポイント
- ✅ Backward Chaining API (成功/失敗)
- ✅ ルール検査API
- ✅ ルールロードAPI (成功/失敗)
- ✅ 汎用検証API
- ✅ Swagger UIの有効化

**テスト数:** 8

**実行例:**
```bash
mvn test -Dtest=TensorLogicResourceTest
```

**重要なテスト:**
- `testBackwardChainAPI`: Backward Chaining APIの動作
- `testSimpleVerifyAPI`: 汎用検証APIの動作

---

### 5. LLMServiceTest

**目的:** LLMサービスの統合テスト

**テスト内容:**
- ✅ サービスのインジェクション
- ✅ デモモードでの動作
- ✅ 推論ステップ付きクエリ
- ✅ 簡易クエリ
- ✅ デモモード検出
- ✅ OpenAI API統合（APIキー設定時）
- ✅ エラーハンドリング

**テスト数:** 7

**実行例:**
```bash
mvn test -Dtest=LLMServiceTest
```

**重要なテスト:**
- `testDemoMode`: デモモードでの動作確認
- `testOpenAIIntegration`: 実際のOpenAI API呼び出し（条件付き）

**Note:** `testOpenAIIntegration`は`OPENAI_API_KEY`環境変数が設定されている場合のみ実行されます。

---

### 6. IntegrationTest

**目的:** エンドツーエンドの統合テスト

**テスト内容:**
- ✅ ルールロード → Forward Chaining → Backward Chaining
- ✅ 複数ネームスペースの管理
- ✅ LLM + Tensor Logic統合
- ✅ ネームスペースフィルタリング
- ✅ エラーリカバリー
- ✅ 推論パスのトレーサビリティ

**テスト数:** 6

**実行例:**
```bash
mvn test -Dtest=IntegrationTest
```

**重要なテスト:**
- `testFullWorkflow`: 完全なワークフローの検証
- `testLLMIntegration`: LLMとTensor Logicの連携

---

## 📊 カバレッジ

### 対象範囲

| パッケージ | カバレッジ目標 | 現状 |
|-----------|--------------|------|
| `ai.tensorlogic.core` | 90%+ | ✅ |
| `ai.tensorlogic.parser` | 85%+ | ✅ |
| `ai.tensorlogic.camel` | 75%+ | ✅ |
| `ai.tensorlogic.api` | 80%+ | ✅ |
| `ai.tensorlogic.llm` | 70%+ | ✅ |
| `ai.tensorlogic.integration` | 80%+ | ✅ |

### カバレッジレポートの確認

```bash
# JaCoCoレポート生成
mvn clean test jacoco:report

# ブラウザで確認
open target/site/jacoco/index.html
```

---

## 🎯 テスト実行のベストプラクティス

### 1. 開発中のテスト

```bash
# 継続的にテストを実行
mvn quarkus:dev

# コード変更時に自動でテストが実行される
# 'r' を押して手動でテスト実行
```

### 2. コミット前のテスト

```bash
# 全テストを実行してからコミット
mvn clean test

# または特定のテストのみ
mvn test -Dtest=TensorLogicEngineTest,IntegrationTest
```

### 3. CI/CDでのテスト

```bash
# すべてのテストを実行し、カバレッジレポートを生成
mvn clean verify jacoco:report

# 結果の確認
cat target/surefire-reports/TEST-*.xml
```

---

## 🔧 トラブルシューティング

### 問題1: テストが失敗する

**症状:**
```
[ERROR] Tests run: 45, Failures: 3, Errors: 1, Skipped: 0
```

**原因と解決策:**

1. **ND4Jの初期化エラー**
   ```bash
   # ND4Jのネイティブライブラリをクリーン
   mvn clean
   rm -rf ~/.m2/repository/org/nd4j
   mvn test
   ```

2. **Quarkusのコンテキストエラー**
   ```bash
   # Quarkusのキャッシュをクリア
   rm -rf target/
   mvn clean compile test
   ```

3. **ルールファイルが見つからない**
   ```bash
   # リソースディレクトリを確認
   ls src/main/resources/rules/
   ls src/test/resources/
   ```

---

### 問題2: LLM統合テストが失敗する

**症状:**
```
testOpenAIIntegration() FAILED
```

**原因:**
- OpenAI APIキーが設定されていない
- ネットワーク接続の問題

**解決策:**
```bash
# APIキーを設定（オプション）
export OPENAI_API_KEY=sk-your-actual-key

# または、デモモードでテスト
# (testOpenAIIntegrationはスキップされる)
mvn test -Dtest=LLMServiceTest -Dtest.exclude=testOpenAIIntegration
```

---

### 問題3: Camel Routesテストが失敗する

**症状:**
```
testCamelContextStarted() FAILED
```

**原因:**
- Camelコンテキストの初期化に時間がかかる
- ポートの競合

**解決策:**
```bash
# テスト用ポートを変更
# src/test/resources/application.properties
quarkus.http.test-port=8082

# Camelの初期化を待つ
# テストに @TestMethodOrder を追加
```

---

### 問題4: メモリ不足エラー

**症状:**
```
java.lang.OutOfMemoryError: Java heap space
```

**解決策:**
```bash
# Mavenのメモリを増やす
export MAVEN_OPTS="-Xmx2g -XX:MaxMetaspaceSize=512m"
mvn test

# または
mvn test -DargLine="-Xmx2g"
```

---

## 📝 テストの追加方法

### 新しいテストクラスの作成

```java
package ai.tensorlogic;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class YourNewTest {
    
    @Test
    @DisplayName("テストの説明")
    void testSomething() {
        // Given
        // ... テストデータの準備
        
        // When
        // ... テスト対象の実行
        
        // Then
        // ... 結果の検証
        assertTrue(true, "条件が真であること");
    }
}
```

### テストの命名規則

- **クラス名:** `*Test` (例: `TensorLogicEngineTest`)
- **メソッド名:** `test*` (例: `testForwardChaining`)
- **DisplayName:** 日本語でわかりやすく (例: `"Forward Chaining - MODUS_PONENS"`)

---

## 🚀 継続的インテグレーション (CI)

### GitHub Actionsの例

```yaml
# .github/workflows/test.yml
name: Test

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'
      - name: Run tests
        run: mvn clean verify
      - name: Upload coverage
        uses: codecov/codecov-action@v3
```

---

## 📚 参考資料

- [JUnit 5 ユーザーガイド](https://junit.org/junit5/docs/current/user-guide/)
- [Quarkus Testing Guide](https://quarkus.io/guides/getting-started-testing)
- [REST Assured](https://rest-assured.io/)
- [Apache Camel Testing](https://camel.apache.org/manual/testing.html)

---

## 🎉 まとめ

### テスト統計

- **総テストクラス数:** 6
- **総テスト数:** 約 50
- **カバレッジ目標:** 80%+
- **実行時間:** 約 30-60秒

### 次のステップ

1. **全テストを実行:**
   ```bash
   mvn test
   ```

2. **カバレッジを確認:**
   ```bash
   mvn jacoco:report
   open target/site/jacoco/index.html
   ```

3. **継続的にテストを追加:**
   - 新機能追加時にテストも追加
   - バグ修正時に再発防止のテストを追加

---

**Happy Testing! 🚀**

**テストは品質の証明です。継続的にテストを実行し、高品質なコードを維持しましょう。**

