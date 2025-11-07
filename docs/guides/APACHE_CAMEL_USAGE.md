# 🐪 Apache Camel 使用状況ガイド

## 📊 **概要**

このプロジェクトでは、**Apache Camel**を使用してエンタープライズ統合パターン（EIP）を実装しています。

---

## ✅ **Camelの使用状況**

### **依存関係（pom.xml）**

```xml
<!-- Camel Quarkus -->
<dependency>
    <groupId>org.apache.camel.quarkus</groupId>
    <artifactId>camel-quarkus-core</artifactId>
</dependency>
<dependency>
    <groupId>org.apache.camel.quarkus</groupId>
    <artifactId>camel-quarkus-direct</artifactId>
</dependency>
<dependency>
    <groupId>org.apache.camel.quarkus</groupId>
    <artifactId>camel-quarkus-http</artifactId>
</dependency>
<dependency>
    <groupId>org.apache.camel.quarkus</groupId>
    <artifactId>camel-quarkus-jackson</artifactId>
</dependency>
<dependency>
    <groupId>org.apache.camel.quarkus</groupId>
    <artifactId>camel-quarkus-bean</artifactId>
</dependency>
<dependency>
    <groupId>org.apache.camel.quarkus</groupId>
    <artifactId>camel-quarkus-seda</artifactId>
</dependency>
```

**バージョン**: Camel Quarkus 3.6.0

---

## 🛤️ **定義されているCamelルート**

### **1. LLM推論検証ルート**

```java
from("direct:verify-llm-reasoning")
    .routeId("verify-llm-reasoning-route")
    .log("LLM推論検証を開始: ${body}")
    .bean(integration, "verifyLLMReasoning")
    .log("検証完了: 妥当性=${body.logicallySound}")
    .marshal().json();
```

**用途**: LLMの推論をTensor Logicで検証

**エンドポイント**: `POST /api/camel/verify`

---

### **2. バッチ検証ルート**

```java
from("direct:batch-verify")
    .routeId("batch-verify-route")
    .log("バッチ検証を開始")
    .split(body())
        .log("クエリ処理: ${body}")
        .to("direct:verify-llm-reasoning")
    .end()
    .log("バッチ検証完了");
```

**用途**: 複数のクエリを順次処理

**パターン**: **Splitter** (メッセージ分割)

**エンドポイント**: `POST /api/camel/batch-verify`

---

### **3. 矛盾検出ルート**

```java
from("direct:detect-contradiction")
    .routeId("detect-contradiction-route")
    .log("矛盾検出を開始")
    .bean(integration, "detectContradictions")
    .choice()
        .when(simple("${body.hasContradiction} == true"))
            .log("⚠️ 矛盾を検出: スコア=${body.score}")
        .otherwise()
            .log("✓ 論理的に一貫")
    .end()
    .marshal().json();
```

**用途**: 論理的な矛盾を検出

**パターン**: **Content-Based Router** (条件分岐)

---

### **4. ストリーミング検証ルート**

```java
from("direct:stream-verify")
    .routeId("stream-verify-route")
    .log("ストリーミング検証を開始")
    .bean(integration, "verifyStreaming")
    .marshal().json();
```

**用途**: LLMのストリーム出力をリアルタイムで検証

---

### **5. キャッシュ検証ルート**

```java
from("direct:cached-verify")
    .routeId("cached-verify-route")
    .log("キャッシュ検証を開始");
```

**用途**: 検証結果のキャッシング

---

### **6. 非同期検証ルート**

```java
from("direct:async-verify")
    .routeId("async-verify-route")
    .log("非同期検証をキューに追加")
    .to("seda:verification-queue");

from("seda:verification-queue")
    .routeId("verification-worker-route")
    .log("ワーカースレッドで検証実行")
    .to("direct:verify-llm-reasoning");
```

**用途**: 非同期処理

**パターン**: 
- **Message Channel** (SEDA)
- **Competing Consumers** (並列処理)

**エンドポイント**: `POST /api/camel/async-verify`

---

### **7. メトリクス収集ルート**

```java
from("direct:collect-metrics")
    .routeId("metrics-route")
    .log("メトリクス収集");
```

**用途**: システムメトリクスの収集

---

### **8. 汎用LLM検証ルート** 🆕

```java
from("direct:generic-verify")
    .routeId("generic-verify-route")
    .log("汎用LLM検証を開始")
    .bean(genericVerifier, "verify")
    .marshal().json();
```

**用途**: 外部ルールファイルを使用した汎用検証

**エンドポイント**: `POST /api/camel/generic-verify`

---

### **9. バッチ汎用検証ルート** 🆕

```java
from("direct:batch-generic-verify")
    .routeId("batch-generic-verify-route")
    .log("バッチ汎用検証を開始")
    .split(body())
        .log("リクエスト処理: ${body}")
        .to("direct:generic-verify")
    .end()
    .log("バッチ汎用検証完了");
```

**用途**: 複数の汎用検証を一括処理

**パターン**: **Splitter** (メッセージ分割)

---

### **10. 複数ルール検証ルート** 🆕

```java
from("direct:multi-rule-verify")
    .routeId("multi-rule-verify-route")
    .log("複数ルール検証を開始")
    .bean(genericVerifier, "verifyWithMultipleRules")
    .marshal().json();
```

**用途**: 複数のルールセットで検証

---

## 🔌 **Camel統合エンドポイント**

### **REST API (`CamelIntegrationResource.java`)**

| メソッド | パス | 説明 | Camelルート |
|---------|------|------|------------|
| POST | `/api/camel/verify` | LLM推論検証 | `direct:verify-llm-reasoning` |
| POST | `/api/camel/batch-verify` | バッチ検証 | `direct:batch-verify` |
| POST | `/api/camel/async-verify` | 非同期検証 | `direct:async-verify` |
| POST | `/api/camel/generic-verify` | 汎用検証 | `direct:generic-verify` |
| POST | `/api/camel/batch-generic-verify` | バッチ汎用検証 | `direct:batch-generic-verify` |

---

## 🎯 **使用しているCamelコンポーネント**

### **1. Direct**
- 同期的なルート呼び出し
- メモリ内での高速な処理
- 例: `direct:verify-llm-reasoning`

### **2. SEDA** (Staged Event-Driven Architecture)
- 非同期処理キュー
- 並列処理のサポート
- 例: `seda:verification-queue`

### **3. Bean**
- Javaビジネスロジックの呼び出し
- 例: `.bean(integration, "verifyLLMReasoning")`

### **4. Jackson**
- JSON マーシャリング/アンマーシャリング
- 例: `.marshal().json()`

---

## 🔄 **実装されているEIPパターン**

### **1. Splitter (分割)**
バッチ検証で使用：複数のクエリを個別に処理

```java
.split(body())
    .to("direct:verify-llm-reasoning")
.end()
```

### **2. Content-Based Router (条件分岐)**
矛盾検出で使用：結果に応じて処理を分岐

```java
.choice()
    .when(simple("${body.hasContradiction} == true"))
        .log("⚠️ 矛盾を検出")
    .otherwise()
        .log("✓ 論理的に一貫")
.end()
```

### **3. Message Channel (メッセージチャネル)**
非同期処理でSEDAキューを使用

```java
.to("seda:verification-queue")
```

### **4. Exception Handling (例外処理)**
グローバルエラーハンドリング

```java
onException(Exception.class)
    .handled(true)
    .log(LoggingLevel.ERROR, "エラー: ${exception.message}")
    .setBody(constant("{ \"error\": \"${exception.message}\" }"));
```

---

## 📊 **Camelの利点**

### **1. エンタープライズ統合パターン**
- ✅ 標準的なEIPパターンを使用
- ✅ 保守性の高いコード
- ✅ チーム全体での理解が容易

### **2. 非同期処理**
- ✅ SEDA キューによる非同期処理
- ✅ 並列処理のサポート
- ✅ 高スループット

### **3. ルーティングの柔軟性**
- ✅ 動的なメッセージルーティング
- ✅ 条件分岐
- ✅ メッセージ変換

### **4. モニタリング**
- ✅ ログ出力
- ✅ メトリクス収集
- ✅ トレーシング

### **5. テスト容易性**
- ✅ ルートごとに独立したテスト
- ✅ モックの使用が容易
- ✅ 統合テストのサポート

---

## 🧪 **Camelルートのテスト**

### **テストファイル**

```java
src/test/java/ai/tensorlogic/TensorLogicRoutesTest.java
```

### **テスト内容**

1. ✅ ルートの存在確認
2. ✅ SEDAキューの設定確認
3. ✅ エラーハンドリングの確認

詳細は [docs/testing/TEST_GUIDE.md](../testing/TEST_GUIDE.md) を参照。

---

## 📈 **Camelルート一覧**

| ルートID | エンドポイント | 用途 | 状態 |
|---------|--------------|------|------|
| `verify-llm-reasoning-route` | `direct:verify-llm-reasoning` | LLM推論検証 | ✅ 実装済み |
| `batch-verify-route` | `direct:batch-verify` | バッチ検証 | ✅ 実装済み |
| `detect-contradiction-route` | `direct:detect-contradiction` | 矛盾検出 | ✅ 実装済み |
| `stream-verify-route` | `direct:stream-verify` | ストリーミング検証 | ✅ 実装済み |
| `cached-verify-route` | `direct:cached-verify` | キャッシュ検証 | ✅ 実装済み |
| `async-verify-route` | `direct:async-verify` | 非同期検証開始 | ✅ 実装済み |
| `verification-worker-route` | `seda:verification-queue` | 非同期検証実行 | ✅ 実装済み |
| `metrics-route` | `direct:collect-metrics` | メトリクス収集 | ✅ 実装済み |
| `generic-verify-route` | `direct:generic-verify` | 汎用検証 | ✅ **NEW!** |
| `batch-generic-verify-route` | `direct:batch-generic-verify` | バッチ汎用検証 | ✅ **NEW!** |
| `multi-rule-verify-route` | `direct:multi-rule-verify` | 複数ルール検証 | ✅ **NEW!** |

**合計**: **11ルート**

---

## 🚀 **使用例**

### **例1: シンプルなLLM検証**

```bash
curl -X POST http://localhost:8080/api/camel/verify \
  -H 'Content-Type: application/json' \
  -d '"ソクラテスは死にますか？"'
```

**Camelルート**: `direct:verify-llm-reasoning`

---

### **例2: バッチ検証**

```bash
curl -X POST http://localhost:8080/api/camel/batch-verify \
  -H 'Content-Type: application/json' \
  -d '["質問1", "質問2", "質問3"]'
```

**Camelルート**: 
1. `direct:batch-verify` (分割)
2. → `direct:verify-llm-reasoning` (各質問を処理)

---

### **例3: 非同期検証**

```bash
curl -X POST http://localhost:8080/api/camel/async-verify \
  -H 'Content-Type: application/json' \
  -d '"時間のかかる質問"'
```

**Camelルート**:
1. `direct:async-verify` (キューに追加)
2. → `seda:verification-queue` (非同期実行)
3. → `direct:verify-llm-reasoning` (実際の検証)

---

### **例4: 汎用検証（外部ルール使用）** 🆕

```bash
curl -X POST http://localhost:8080/api/camel/generic-verify \
  -H 'Content-Type: application/json' \
  -d '{
    "query": "18歳で年収300万円の申請者は融資を受けられますか？",
    "ruleFile": "rules/loan-approval-from-drd.yaml",
    "namespace": "loan-approval"
  }'
```

**Camelルート**: `direct:generic-verify`

---

## 🔍 **Camelコンソール（デバッグ用）**

Quarkus Dev UIでCamelルートの状態を確認できます：

```
http://localhost:8080/q/dev/
```

---

## 📚 **参考資料**

### **公式ドキュメント**
- [Apache Camel](https://camel.apache.org/)
- [Camel Quarkus](https://camel.apache.org/camel-quarkus/latest/)
- [Enterprise Integration Patterns](https://www.enterpriseintegrationpatterns.com/)

### **プロジェクト内ドキュメント**
- [TensorLogicエンジンガイド](./TENSOR_LOGIC_ENGINE_GUIDE.md)
- [テストガイド](../testing/TEST_GUIDE.md)
- [実装詳細](../implementation/JAVA_IMPLEMENTATION.md)

---

## 🎯 **まとめ**

### **Camelの使用状況**

- ✅ **11個のルート**を定義
- ✅ **6つのCamelコンポーネント**を使用
- ✅ **4つのEIPパターン**を実装
- ✅ **5つのREST API**を提供

### **Camelを使う理由**

1. **標準化**: エンタープライズ統合の標準パターンを使用
2. **保守性**: ルートが独立しており、変更が容易
3. **スケーラビリティ**: 非同期処理とキューイングをサポート
4. **テスト**: ルート単位でのテストが容易
5. **モニタリング**: 組み込みのログとメトリクス

---

**作成日**: 2025年11月7日  
**対象バージョン**: Camel Quarkus 3.6.0  
**プロジェクト**: TensorLogic 1.0.0

