package ai.tensorlogic.camel;

import ai.tensorlogic.integration.LLMTensorLogicIntegration;
import ai.tensorlogic.integration.GenericLLMVerifier;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.LoggingLevel;

/**
 * Apache Camel Routes for Tensor Logic
 * 
 * エンタープライズ統合パターンを使用したLLM処理パイプライン
 */
@ApplicationScoped
public class TensorLogicRoutes extends RouteBuilder {
    
    @Inject
    LLMTensorLogicIntegration integration;
    
    @Inject
    GenericLLMVerifier genericVerifier;
    
    @Override
    public void configure() {
        
        // エラーハンドリング
        onException(Exception.class)
            .handled(true)
            .log(LoggingLevel.ERROR, "エラーが発生しました: ${exception.message}")
            .setBody(constant("{ \"error\": \"${exception.message}\" }"));
        
        /**
         * Route 1: LLM推論検証パイプライン
         * 
         * direct:verify-llm-reasoning
         *   → LLMに質問
         *   → Tensor Logicで検証
         *   → 結果を返却
         */
        from("direct:verify-llm-reasoning")
            .routeId("verify-llm-reasoning-route")
            .log("LLM推論検証を開始: ${body}")
            .bean(integration, "verifyLLMReasoning")
            .log("検証完了: 妥当性=${body.logicallySound}, 信頼度=${body.validationConfidence}")
            .marshal().json();
        
        /**
         * Route 2: バッチ推論検証
         * 
         * 複数のクエリを順次処理
         */
        from("direct:batch-verify")
            .routeId("batch-verify-route")
            .log("バッチ検証を開始")
            .split(body())
                .log("クエリ処理: ${body}")
                .to("direct:verify-llm-reasoning")
            .end()
            .log("バッチ検証完了");
        
        /**
         * Route 3: 矛盾検出パイプライン
         */
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
        
        /**
         * Route 4: ストリーミング検証
         * 
         * LLMのストリーム出力をリアルタイムで検証
         */
        from("direct:stream-verify")
            .routeId("stream-verify-route")
            .log("ストリーミング検証を開始")
            .split(body().tokenize("\n"))
                .filter(body().isNotNull())
                .log("トークン処理: ${body}")
                .to("direct:verify-llm-reasoning")
            .end();
        
        /**
         * Route 5: キャッシング付き検証
         * 
         * 同じクエリの結果をキャッシュ（簡易実装）
         */
        from("direct:cached-verify")
            .routeId("cached-verify-route")
            .log("キャッシュ検証: ${body}")
            .to("direct:verify-llm-reasoning");
        
        /**
         * Route 6: 非同期検証
         */
        from("direct:async-verify")
            .routeId("async-verify-route")
            .log("非同期検証をキューに追加")
            .to("seda:verification-queue?concurrentConsumers=5");
        
        from("seda:verification-queue")
            .routeId("verification-worker-route")
            .log("ワーカー処理開始: ${threadName}")
            .to("direct:verify-llm-reasoning")
            .log("ワーカー処理完了: ${threadName}");
        
        /**
         * Route 7: メトリクス収集
         */
        from("direct:collect-metrics")
            .routeId("metrics-route")
            .log("メトリクス: 処理時間=${header.CamelTimerElapsedTime}ms");
        
        /**
         * Route 8: 汎用LLM検証パイプライン ⭐ NEW
         * 
         * 外部ルールファイルを使用した汎用的な検証
         * direct:generic-verify
         *   → ルールファイルを読み込み（オプション）
         *   → LLMに質問
         *   → 登録されているルールで推論を実行
         *   → 期待される結果と比較
         *   → 詳細な検証結果を返却
         */
        from("direct:generic-verify")
            .routeId("generic-verify-route")
            .log("汎用LLM検証を開始")
            .log("  質問: ${body.query}")
            .log("  ルールファイル: ${body.ruleFile}")
            .bean(genericVerifier, "verify")
            .log("検証完了: 論理的妥当性=${body.logicallySound}, スコア=${body.validationScore}")
            .choice()
                .when(simple("${body.logicallySound} == true"))
                    .log("✓ LLMの回答は論理的に妥当です")
                .otherwise()
                    .log("⚠️ LLMの回答に論理的な問題があります: ${body.verificationMismatches}")
            .end()
            .marshal().json();
        
        /**
         * Route 9: バッチ汎用検証
         * 
         * 複数のクエリを汎用検証
         */
        from("direct:batch-generic-verify")
            .routeId("batch-generic-verify-route")
            .log("バッチ汎用検証を開始")
            .split(body())
                .log("汎用検証クエリ処理: ${body.query}")
                .to("direct:generic-verify")
            .end()
            .log("バッチ汎用検証完了");
        
        /**
         * Route 10: ルールファイル切り替え検証
         * 
         * 同じクエリを異なるルールファイルで検証して比較
         */
        from("direct:multi-rule-verify")
            .routeId("multi-rule-verify-route")
            .log("マルチルール検証を開始")
            .split(simple("${body.ruleFiles}"))
                .log("ルールファイル ${body} で検証")
                .setHeader("ruleFile", simple("${body}"))
                .to("direct:generic-verify")
            .end()
            .log("マルチルール検証完了");
    }
}

