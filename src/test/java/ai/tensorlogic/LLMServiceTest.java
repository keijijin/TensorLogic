package ai.tensorlogic;

import ai.tensorlogic.llm.LLMResponse;
import ai.tensorlogic.llm.LLMService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LLMService のテスト
 */
@QuarkusTest
class LLMServiceTest {
    
    @Inject
    LLMService llmService;
    
    @Test
    @DisplayName("LLMServiceがインジェクトされること")
    void testLLMServiceInjected() {
        assertNotNull(llmService, "LLMServiceがインジェクトされること");
    }
    
    @Test
    @DisplayName("デモモードでの動作")
    void testDemoMode() {
        // Given: APIキーが設定されていない場合、デモモードで動作
        
        // When
        LLMResponse response = llmService.queryWithReasoning("テスト質問");
        
        // Then
        assertNotNull(response, "レスポンスが返ること");
        assertNotNull(response.answer(), "回答が存在すること");
        assertTrue(response.confidence() > 0, "確信度が設定されること");
    }
    
    @Test
    @DisplayName("推論ステップ付きクエリ")
    void testQueryWithReasoning() {
        // When
        LLMResponse response = llmService.queryWithReasoning("ソクラテスは死にますか？");
        
        // Then
        assertNotNull(response, "レスポンスが返ること");
        assertNotNull(response.answer(), "回答が存在すること");
        assertNotNull(response.reasoningSteps(), "推論ステップが存在すること");
        assertTrue(response.confidence() >= 0.0 && response.confidence() <= 1.0, 
            "確信度が0-1の範囲であること");
    }
    
    @Test
    @DisplayName("簡易クエリ")
    void testSimpleQuery() {
        // When
        LLMResponse response = llmService.queryWithReasoning("こんにちは");
        
        // Then
        assertNotNull(response, "レスポンスが返ること");
        assertNotNull(response.answer(), "回答が存在すること");
        assertFalse(response.answer().isEmpty(), "回答が空でないこと");
    }
    
    @Test
    @DisplayName("デモモード検出")
    void testDemoModeDetection() {
        // デモモードかどうかはAPIキーの設定による
        // このテストはデモモードでの動作を確認
        
        // When: デモモードでも正常に動作すること
        assertDoesNotThrow(() -> {
            LLMResponse response = llmService.queryWithReasoning("テスト");
            assertNotNull(response);
        }, "デモモードでも動作すること");
    }
    
    @Test
    @EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = "sk-.*")
    @DisplayName("OpenAI APIとの統合（APIキー設定時のみ）")
    void testOpenAIIntegration() {
        // Given: 有効なAPIキーが設定されている場合のみ実行
        
        // When
        LLMResponse response = llmService.queryWithReasoning("1 + 1は？");
        
        // Then
        assertNotNull(response, "レスポンスが返ること");
        assertNotNull(response.answer(), "回答が存在すること");
        assertTrue(response.answer().contains("2"), "正しい回答が含まれること");
    }
    
    @Test
    @DisplayName("エラーハンドリング - 空の質問")
    void testErrorHandling_EmptyQuery() {
        // When & Then
        assertDoesNotThrow(() -> {
            LLMResponse response = llmService.queryWithReasoning("");
            assertNotNull(response, "空の質問でもレスポンスが返ること");
        }, "空の質問でもエラーにならないこと");
    }
}

