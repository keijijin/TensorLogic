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
     * 
     * 登録されているルールを使用する。
     * ルールが登録されていない場合は、デフォルトのソクラテスルールを使用。
     */
    public VerifiedReasoningResult verifyLLMReasoning(String query) {
        LOG.info("質問を処理: {}", query);
        
        // 1. LLMから回答を取得
        LLMResponse llmResponse = llmService.queryWithReasoning(query);
        LOG.info("LLMの回答: {} (確信度: {})", llmResponse.answer(), llmResponse.confidence());
        
        // 2. Tensor Logicで検証
        // 登録されているルールを使用（存在する場合）
        INDArray socratesIsHuman;
        INDArray humanIsMortal;
        INDArray expectedConclusion;
        
        // TensorLogicEngineに登録されているルールを確認
        INDArray registeredHuman = tensorLogic.getFact("socrates_is_human");
        INDArray registeredMortal = tensorLogic.getFact("human_is_mortal");
        
        if (registeredHuman != null && registeredMortal != null) {
            // 登録されているルールを使用
            LOG.info("✓ 登録されているルールを使用します");
            socratesIsHuman = registeredHuman;
            humanIsMortal = registeredMortal;
            
            // 期待される結論を取得（または計算）
            INDArray registeredConclusion = tensorLogic.getFact("socrates_is_mortal");
            if (registeredConclusion != null) {
                expectedConclusion = registeredConclusion;
                LOG.info("  登録された結論を使用: {}", expectedConclusion.getDouble(0));
            } else {
                // 前向き推論を実行して結論を得る
                tensorLogic.forwardChain();
                registeredConclusion = tensorLogic.getFact("socrates_is_mortal");
                if (registeredConclusion != null) {
                    expectedConclusion = registeredConclusion;
                    LOG.info("  前向き推論で計算された結論: {}", expectedConclusion.getDouble(0));
                } else {
                    // フォールバック: LLMの確信度を使用
                    expectedConclusion = Nd4j.create(new double[]{llmResponse.confidence()});
                    LOG.warn("  結論が計算できませんでした。LLM確信度を使用: {}", llmResponse.confidence());
                }
            }
        } else {
            // デフォルトのソクラテスルールを使用（下位互換性のため）
            LOG.warn("⚠️ ルールが登録されていません。デフォルト値を使用します");
            LOG.warn("   /api/rules/load-example を実行してルールを登録してください");
            socratesIsHuman = Nd4j.create(new double[]{llmResponse.confidence()});
            humanIsMortal = Nd4j.create(new double[][]{{0.98}});
            expectedConclusion = Nd4j.create(new double[]{llmResponse.confidence()});
        }
        
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

