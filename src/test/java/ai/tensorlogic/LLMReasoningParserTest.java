package ai.tensorlogic;

import ai.tensorlogic.llm.LLMReasoningParser;
import ai.tensorlogic.llm.LLMReasoningResult;
import ai.tensorlogic.parser.RuleDefinition;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LLMReasoningParserのテスト
 */
@QuarkusTest
public class LLMReasoningParserTest {
    
    @Inject
    LLMReasoningParser parser;
    
    @Test
    @DisplayName("シンプルな推論をTensor Logic化")
    public void testParseSimpleReasoning() {
        // 準備
        String query = "ソクラテスは死にますか？";
        String answer = "はい、ソクラテスは死にます。";
        List<String> steps = List.of(
            "ステップ1: ソクラテスは人間です。(確信度: 100%)",
            "ステップ2: すべての人間は死にます。(確信度: 98%)",
            "ステップ3: したがって、ソクラテスは死にます。(確信度: 98%)"
        );
        
        LLMReasoningResult llmResult = new LLMReasoningResult(
            query, answer, steps, 0.98
        );
        
        // 実行
        RuleDefinition definition = parser.parseToTensorLogic(llmResult);
        
        // 検証
        assertNotNull(definition);
        assertNotNull(definition.metadata());
        assertEquals("LLM Parser", definition.metadata().author());
        assertTrue(definition.metadata().name().contains("ソクラテス"));
        
        // Factsが抽出されているか（結論ステップは除く = 2つ）
        assertNotNull(definition.facts());
        assertFalse(definition.facts().isEmpty());
        assertEquals(2, definition.facts().size());
        
        // 最初のFactの確信度が1.0か
        assertEquals(1.0, definition.facts().get(0).tensor().confidence());
        
        // 2番目のFactの確信度が0.98か
        assertEquals(0.98, definition.facts().get(1).tensor().confidence());
        
        // Rulesが生成されているか
        assertNotNull(definition.rules());
        assertFalse(definition.rules().isEmpty());
    }
    
    @Test
    @DisplayName("複数ステップの推論をTensor Logic化")
    public void testParseMultiStepReasoning() {
        // 準備
        String query = "融資は受けられますか？";
        List<String> steps = List.of(
            "ステップ1: 申請者は18歳です。(確信度: 100%)",
            "ステップ2: 年収は300万円以上です。(確信度: 100%)",
            "ステップ3: 信用スコアは良好です。(確信度: 100%)",
            "ステップ4: 18歳以上で所得があるとローン資格があります。(確信度: 90%)",
            "ステップ5: 信用スコアが良好ならローン可能性が高いです。(確信度: 90%)",
            "ステップ6: 年収は返済能力の指標です。(確信度: 85%)"
        );
        
        LLMReasoningResult llmResult = new LLMReasoningResult(
            query, "融資を受けられる可能性が高いです。", steps, 0.85
        );
        
        // 実行
        RuleDefinition definition = parser.parseToTensorLogic(llmResult);
        
        // 検証
        assertNotNull(definition);
        assertEquals(6, definition.facts().size());
        
        // 確信度の順序を確認
        assertEquals(1.0, definition.facts().get(0).tensor().confidence());
        assertEquals(1.0, definition.facts().get(1).tensor().confidence());
        assertEquals(1.0, definition.facts().get(2).tensor().confidence());
        assertEquals(0.9, definition.facts().get(3).tensor().confidence());
        assertEquals(0.9, definition.facts().get(4).tensor().confidence());
        assertEquals(0.85, definition.facts().get(5).tensor().confidence());
        
        // Rulesが生成されているか
        assertNotNull(definition.rules());
        assertFalse(definition.rules().isEmpty());
        
        // デフォルトルールが最小確信度を使用しているか
        RuleDefinition.RuleSpec rule = definition.rules().get(0);
        assertTrue(rule.inputs().contains("fact_step_6") || 
                   rule.inputs().contains("fact_step_4"));
    }
    
    @Test
    @DisplayName("確信度の抽出テスト")
    public void testConfidenceExtraction() {
        // 準備
        List<String> steps = List.of(
            "事実A (確信度: 100%)",
            "事実B (確信度: 95%)",
            "事実C (確信度: 80%)",
            "事実D (確信度: 50%)"
        );
        
        LLMReasoningResult llmResult = new LLMReasoningResult(
            "テスト", "回答", steps, 0.5
        );
        
        // 実行
        RuleDefinition definition = parser.parseToTensorLogic(llmResult);
        
        // 検証
        assertEquals(4, definition.facts().size());
        assertEquals(1.0, definition.facts().get(0).tensor().confidence());
        assertEquals(0.95, definition.facts().get(1).tensor().confidence());
        assertEquals(0.8, definition.facts().get(2).tensor().confidence());
        assertEquals(0.5, definition.facts().get(3).tensor().confidence());
    }
    
    @Test
    @DisplayName("英語の推論ステップをパース")
    public void testParseEnglishSteps() {
        // 準備
        List<String> steps = List.of(
            "Step 1: Socrates is human. (confidence: 100%)",
            "Step 2: All humans are mortal. (confidence: 98%)",
            "Step 3: Therefore, Socrates is mortal. (confidence: 98%)"
        );
        
        LLMReasoningResult llmResult = new LLMReasoningResult(
            "Is Socrates mortal?", "Yes, Socrates is mortal.", steps, 0.98
        );
        
        // 実行
        RuleDefinition definition = parser.parseToTensorLogic(llmResult);
        
        // 検証（結論ステップは除く = 2つ）
        assertNotNull(definition);
        assertEquals(2, definition.facts().size());
        
        // 確信度が正しく抽出されているか
        assertEquals(1.0, definition.facts().get(0).tensor().confidence());
        assertEquals(0.98, definition.facts().get(1).tensor().confidence());
    }
    
    @Test
    @DisplayName("確信度が指定されていない場合のデフォルト値")
    public void testDefaultConfidence() {
        // 準備
        List<String> steps = List.of(
            "ステップ1: これは事実です。",
            "ステップ2: これも事実です。"
        );
        
        LLMReasoningResult llmResult = new LLMReasoningResult(
            "質問", "回答", steps, 0.9
        );
        
        // 実行
        RuleDefinition definition = parser.parseToTensorLogic(llmResult);
        
        // 検証
        assertNotNull(definition);
        assertEquals(2, definition.facts().size());
        
        // デフォルト確信度が0.9になっているか
        assertEquals(0.9, definition.facts().get(0).tensor().confidence());
    }
    
    @Test
    @DisplayName("期待結果が正しく生成される")
    public void testExpectedResults() {
        // 準備
        List<String> steps = List.of(
            "ステップ1: 事実A (確信度: 100%)",
            "ステップ2: 事実B (確信度: 90%)"
        );
        
        LLMReasoningResult llmResult = new LLMReasoningResult(
            "質問", "回答", steps, 0.85
        );
        
        // 実行
        RuleDefinition definition = parser.parseToTensorLogic(llmResult);
        
        // 検証
        assertNotNull(definition.expectedResults());
        assertFalse(definition.expectedResults().isEmpty());
        
        RuleDefinition.ExpectedResult expected = definition.expectedResults().get(0);
        assertEquals("final_conclusion", expected.name());
        assertEquals(0.85, expected.expectedValue());
        assertEquals(0.1, expected.tolerance());
    }
    
    @Test
    @DisplayName("空の推論ステップを処理")
    public void testEmptySteps() {
        // 準備
        List<String> steps = List.of();
        
        LLMReasoningResult llmResult = new LLMReasoningResult(
            "質問", "回答", steps, 0.5
        );
        
        // 実行
        RuleDefinition definition = parser.parseToTensorLogic(llmResult);
        
        // 検証
        assertNotNull(definition);
        assertEquals(0, definition.facts().size());
    }
}

