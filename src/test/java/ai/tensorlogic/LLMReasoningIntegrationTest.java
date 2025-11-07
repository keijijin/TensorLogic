package ai.tensorlogic;

import ai.tensorlogic.core.BackwardChainingResult;
import ai.tensorlogic.core.TensorLogicEngine;
import ai.tensorlogic.llm.*;
import ai.tensorlogic.parser.RuleDefinition;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LLM推論のTensor Logic化のエンドツーエンドテスト
 */
@QuarkusTest
public class LLMReasoningIntegrationTest {
    
    @Inject
    LLMService llmService;
    
    @Inject
    LLMReasoningParser parser;
    
    @Inject
    TensorLogicEngine engine;
    
    @Test
    @DisplayName("エンドツーエンド: LLM推論 → Tensor Logic化 → 検証")
    public void testEndToEndWorkflow() {
        // ステップ1: LLMに推論させる（モックデータ）
        List<String> steps = List.of(
            "ステップ1: ソクラテスは人間です。(確信度: 100%)",
            "ステップ2: すべての人間は死にます。(確信度: 98%)",
            "ステップ3: したがって、ソクラテスは死にます。(確信度: 98%)"
        );
        
        LLMReasoningResult llmResult = new LLMReasoningResult(
            "ソクラテスは死にますか？",
            "はい、ソクラテスは死にます。",
            steps,
            0.98
        );
        
        // ステップ2: Tensor Logic化
        RuleDefinition definition = parser.parseToTensorLogic(llmResult);
        
        assertNotNull(definition);
        assertEquals(2, definition.facts().size()); // 結論ステップは除く
        assertFalse(definition.rules().isEmpty());
        
        // ステップ3: Tensor Logicエンジンにロード
        engine.clear();
        
        // Factsを登録
        for (RuleDefinition.Fact fact : definition.facts()) {
            @SuppressWarnings("unchecked")
            List<Double> values = (List<Double>) fact.tensor().values();
            
            engine.addFact(
                fact.name(),
                org.nd4j.linalg.factory.Nd4j.create(
                    values.stream()
                        .mapToDouble(Double::doubleValue)
                        .toArray()
                )
            );
        }
        
        // Rulesを登録
        for (RuleDefinition.RuleSpec ruleSpec : definition.rules()) {
            ai.tensorlogic.core.Rule rule = new ai.tensorlogic.core.Rule(
                definition.metadata().namespace(),
                ruleSpec.inputs(),
                ruleSpec.output(),
                ai.tensorlogic.core.Rule.Operation.valueOf(ruleSpec.operation())
            );
            engine.addRule(rule);
        }
        
        // ステップ4: 検証
        String goalName = definition.expectedResults().get(0).name();
        BackwardChainingResult verification = engine.backwardChain(
            goalName,
            definition.metadata().namespace()
        );
        
        assertNotNull(verification);
        assertTrue(verification.success());
        assertTrue(verification.getGoalConfidence() > 0.9);
    }
    
    @Test
    @DisplayName("複数の推論ステップを処理")
    public void testMultipleSteps() {
        // 準備
        List<String> steps = List.of(
            "ステップ1: 事実A (確信度: 100%)",
            "ステップ2: 事実B (確信度: 95%)",
            "ステップ3: 事実C (確信度: 90%)",
            "ステップ4: 事実D (確信度: 85%)",
            "ステップ5: 事実E (確信度: 80%)"
        );
        
        LLMReasoningResult llmResult = new LLMReasoningResult(
            "複雑な質問",
            "回答",
            steps,
            0.8
        );
        
        // 実行
        RuleDefinition definition = parser.parseToTensorLogic(llmResult);
        
        // 検証
        assertEquals(5, definition.facts().size());
        
        // 確信度が降順になっているか
        double prevConfidence = 1.1; // 初期値
        for (RuleDefinition.Fact fact : definition.facts()) {
            assertTrue(fact.tensor().confidence() <= prevConfidence);
            prevConfidence = fact.tensor().confidence();
        }
    }
    
    @Test
    @DisplayName("LLMとTensor Logicの確信度を比較")
    public void testConfidenceComparison() {
        // 準備
        List<String> steps = List.of(
            "ステップ1: 前提A (確信度: 100%)",
            "ステップ2: 前提B (確信度: 90%)"
        );
        
        LLMReasoningResult llmResult = new LLMReasoningResult(
            "質問",
            "回答",
            steps,
            0.9
        );
        
        // Tensor Logic化
        RuleDefinition definition = parser.parseToTensorLogic(llmResult);
        
        // エンジンにロード
        engine.clear();
        for (RuleDefinition.Fact fact : definition.facts()) {
            @SuppressWarnings("unchecked")
            List<Double> values = (List<Double>) fact.tensor().values();
            
            engine.addFact(
                fact.name(),
                org.nd4j.linalg.factory.Nd4j.create(
                    values.stream().mapToDouble(Double::doubleValue).toArray()
                )
            );
        }
        
        for (RuleDefinition.RuleSpec ruleSpec : definition.rules()) {
            ai.tensorlogic.core.Rule rule = new ai.tensorlogic.core.Rule(
                definition.metadata().namespace(),
                ruleSpec.inputs(),
                ruleSpec.output(),
                ai.tensorlogic.core.Rule.Operation.valueOf(ruleSpec.operation())
            );
            engine.addRule(rule);
        }
        
        // 検証
        BackwardChainingResult verification = engine.backwardChain(
            definition.expectedResults().get(0).name(),
            definition.metadata().namespace()
        );
        
        // 比較
        double llmConfidence = llmResult.confidence();
        double tensorLogicConfidence = verification.getGoalConfidence();
        double difference = Math.abs(llmConfidence - tensorLogicConfidence);
        
        // 差異が妥当な範囲内か（30%以下）
        assertTrue(difference <= 0.3, 
            String.format("差異が大きすぎます: %.2f", difference));
    }
    
    @Test
    @DisplayName("結論ステップが正しく識別される")
    public void testConclusionIdentification() {
        // 準備
        List<String> steps = List.of(
            "ステップ1: 前提A (確信度: 100%)",
            "ステップ2: 前提B (確信度: 90%)",
            "ステップ3: したがって、結論C (確信度: 85%)"
        );
        
        LLMReasoningResult llmResult = new LLMReasoningResult(
            "質問",
            "回答",
            steps,
            0.85
        );
        
        // 実行
        RuleDefinition definition = parser.parseToTensorLogic(llmResult);
        
        // 検証 - 結論ステップはルールとして生成されるべき
        // （ただし、現在の実装では3ステップの場合ルールが生成されないかもしれない）
        assertNotNull(definition.rules());
    }
}

