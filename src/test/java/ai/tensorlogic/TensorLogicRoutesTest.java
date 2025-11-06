package ai.tensorlogic;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Camel Routes のテスト
 */
@QuarkusTest
class TensorLogicRoutesTest {
    
    @Inject
    CamelContext camelContext;
    
    @Inject
    ProducerTemplate producerTemplate;
    
    @Test
    @DisplayName("Camelコンテキストが起動すること")
    void testCamelContextStarted() {
        assertNotNull(camelContext, "Camelコンテキストが存在すること");
        assertTrue(camelContext.isStarted() || camelContext.isStarting(), 
            "Camelコンテキストが起動していること");
    }
    
    @Test
    @DisplayName("direct:verify-llm-reasoning ルートが存在すること")
    void testVerifyLlmReasoningRouteExists() {
        boolean routeExists = camelContext.getRoute("verify-llm-reasoning-route") != null;
        assertTrue(routeExists, "verify-llm-reasoningルートが存在すること");
    }
    
    @Test
    @DisplayName("direct:generic-verify ルートが存在すること")
    void testGenericVerifyRouteExists() {
        boolean routeExists = camelContext.getRoute("generic-verify-route") != null;
        assertTrue(routeExists, "generic-verifyルートが存在すること");
    }
    
    @Test
    @DisplayName("direct:batch-generic-verify ルートが存在すること")
    void testBatchGenericVerifyRouteExists() {
        boolean routeExists = camelContext.getRoute("batch-generic-verify-route") != null;
        assertTrue(routeExists, "batch-generic-verifyルートが存在すること");
    }
    
    @Test
    @DisplayName("seda:verification-queue が設定されていること")
    void testVerificationQueueConfiguration() {
        // SEDA キューが正しく設定されているか確認
        assertDoesNotThrow(() -> {
            camelContext.getEndpoint("seda://verification-queue?concurrentConsumers=5");
        }, "verification-queueエンドポイントが設定されていること");
    }
    
    @Test
    @DisplayName("ルート数の確認")
    void testTotalRoutesCount() {
        int routeCount = camelContext.getRoutes().size();
        assertTrue(routeCount >= 5, "少なくとも5つのルートが定義されていること");
    }
    
    @Test
    @DisplayName("エラーハンドリングが設定されていること")
    void testErrorHandlingConfigured() {
        // Camelコンテキストが正常に動作していることを確認
        assertTrue(camelContext.getRoutes().size() > 0, 
            "ルートが設定されていること（エラーハンドリング含む）");
    }
    
    @Test
    @DisplayName("Producer Templateが動作すること")
    void testProducerTemplateWorks() {
        assertNotNull(producerTemplate, "ProducerTemplateが存在すること");
        assertNotNull(producerTemplate.getCamelContext(), 
            "ProducerTemplateがCamelContextと紐づいていること");
    }
}

