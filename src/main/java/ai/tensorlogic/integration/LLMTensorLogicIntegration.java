package ai.tensorlogic.integration;

import ai.tensorlogic.core.*;
import ai.tensorlogic.llm.LLMResponse;
import ai.tensorlogic.llm.LLMService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LLMとTensor Logicの統合サービス
 * 
 * Python版のHybridReasoningSystemに相当
 */
@ApplicationScoped
public class LLMTensorLogicIntegration {
    
    private static final Logger LOG = LoggerFactory.getLogger(LLMTensorLogicIntegration.class);
    
    @Inject
    TensorLogicEngine tensorLogic;
    
    @Inject
    LLMService llmService;
    
    /**
     * LLMの推論をTensor Logicで検証
     */
    public VerifiedReasoningResult verifyLLMReasoning(String query) {
        LOG.info("質問を処理: {}", query);
        
        // 1. LLMから回答を取得
        LLMResponse llmResponse = llmService.queryWithReasoning(query);
        LOG.info("LLMの回答: {} (確信度: {})", llmResponse.answer(), llmResponse.confidence());
        
        // 2. Tensor Logicで検証
        // 例: ソクラテスの三段論法
        INDArray socratesIsHuman = Nd4j.create(new double[]{llmResponse.confidence()});
        INDArray humanIsMortal = Nd4j.create(new double[][]{{0.98}});
        INDArray expectedConclusion = Nd4j.create(new double[]{llmResponse.confidence()});
        
        ValidationResult validation = tensorLogic.validateReasoning(
            socratesIsHuman,
            humanIsMortal,
            expectedConclusion,
            0.2
        );
        
        LOG.info("検証結果: {} (信頼度: {})", 
            validation.isValid() ? "✓ 妥当" : "✗ 不適切", 
            validation.confidence());
        
        return new VerifiedReasoningResult(
            llmResponse.answer(),
            llmResponse.confidence(),
            llmResponse.reasoningSteps(),
            validation.isValid(),
            validation.confidence(),
            validation
        );
    }
    
    /**
     * 矛盾を検出
     */
    public ContradictionAnalysis detectContradictions(
            double claim1, double claim2, double claim3) {
        
        INDArray c1 = Nd4j.create(new double[]{claim1});
        INDArray c2 = Nd4j.create(new double[]{claim2});
        INDArray c3 = Nd4j.create(new double[]{claim3});
        
        ContradictionResult result = tensorLogic.detectContradiction(c1, c2, c3);
        
        if (result.hasContradiction()) {
            LOG.warn("⚠️ 論理的矛盾を検出: スコア={}", result.contradictionScore());
        }
        
        return new ContradictionAnalysis(
            result.hasContradiction(),
            result.contradictionScore(),
            result.hasContradiction() 
                ? "論理的矛盾を検出しました。推移律に違反しています。"
                : "論理的に一貫しています。"
        );
    }
    
    /**
     * 確信度の伝播を計算
     */
    public ConfidencePropagationResult propagateConfidence(double... confidences) {
        ConfidencePropagation propagation = tensorLogic.propagateConfidence(confidences);
        
        LOG.info("確信度の伝播: {} -> {} (不確実性: {})",
            confidences, 
            propagation.finalConfidence(),
            propagation.uncertainty());
        
        return new ConfidencePropagationResult(
            propagation.finalConfidence(),
            propagation.uncertainty(),
            confidences
        );
    }
}

