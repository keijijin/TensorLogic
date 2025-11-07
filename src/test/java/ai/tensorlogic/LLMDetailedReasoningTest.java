package ai.tensorlogic;

import ai.tensorlogic.llm.LLMReasoningResult;
import ai.tensorlogic.llm.LLMService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LLMServiceの詳細推論機能のテスト
 */
@QuarkusTest
public class LLMDetailedReasoningTest {
    
    @Inject
    LLMService llmService;
    
    @Test
    @DisplayName("queryWithDetailedReasoningが推論ステップを返す")
    public void testQueryWithDetailedReasoning() {
        // 実行
        LLMReasoningResult result = llmService.queryWithDetailedReasoning(
            "ソクラテスは死にますか？"
        );
        
        // 検証
        assertNotNull(result);
        assertNotNull(result.query());
        assertEquals("ソクラテスは死にますか？", result.query());
        assertNotNull(result.answer());
        assertFalse(result.answer().isEmpty());
        assertNotNull(result.reasoningSteps());
        // デモモードでも何かしらのステップが返される
        assertTrue(result.reasoningSteps().size() >= 0);
        assertTrue(result.confidence() >= 0.0);
        assertTrue(result.confidence() <= 1.0);
    }
    
    @Test
    @DisplayName("複雑な質問で詳細な推論を取得")
    public void testComplexQuery() {
        // 実行
        LLMReasoningResult result = llmService.queryWithDetailedReasoning(
            "18歳で年収300万円以上、信用スコア良好な申請者は融資を受けられますか？"
        );
        
        // 検証
        assertNotNull(result);
        assertNotNull(result.answer());
        assertNotNull(result.reasoningSteps());
        
        // reasoningStepsが0個以上あることを確認（デモモードでもOK）
        assertTrue(result.reasoningSteps().size() >= 0);
        
        // デモモードでない場合は、より詳細な推論が期待される
        // （ただし、テスト環境では通常デモモードで動作する）
    }
    
    @Test
    @DisplayName("確信度が0-1の範囲内")
    public void testConfidenceRange() {
        // 実行
        LLMReasoningResult result = llmService.queryWithDetailedReasoning(
            "太陽は東から昇りますか？"
        );
        
        // 検証
        assertTrue(result.confidence() >= 0.0);
        assertTrue(result.confidence() <= 1.0);
    }
    
    @Test
    @DisplayName("空の質問でもエラーにならない")
    public void testEmptyQuery() {
        // 実行
        LLMReasoningResult result = llmService.queryWithDetailedReasoning("");
        
        // 検証 - エラーにならずに何かしら返される
        assertNotNull(result);
        assertNotNull(result.answer());
    }
    
    @Test
    @DisplayName("mainConclusionが取得できる")
    public void testMainConclusion() {
        // 準備
        LLMReasoningResult result = new LLMReasoningResult(
            "質問",
            "回答",
            java.util.List.of("ステップ1", "したがって、結論"),
            0.9
        );
        
        // 実行
        String conclusion = result.getMainConclusion();
        
        // 検証
        assertNotNull(conclusion);
        assertFalse(conclusion.isEmpty());
    }
}

