package ai.tensorlogic.api;

import ai.tensorlogic.core.BackwardChainingResult;
import ai.tensorlogic.core.TensorLogicEngine;
import ai.tensorlogic.llm.*;
import ai.tensorlogic.parser.RuleDefinition;
import ai.tensorlogic.parser.RuleLoader;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LLM推論のTensor Logic化 REST API
 */
@Path("/api/llm/reasoning-to-tensor")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "LLM Reasoning Analysis", description = "LLM推論をTensor Logicで分析・検証")
public class LLMReasoningResource {
    
    private static final Logger LOG = LoggerFactory.getLogger(LLMReasoningResource.class);
    
    @Inject
    LLMService llmService;
    
    @Inject
    LLMReasoningParser parser;
    
    @Inject
    TensorLogicEngine engine;
    
    @Inject
    RuleLoader ruleLoader;
    
    /**
     * LLMの推論を分析してTensor Logicで検証
     */
    @POST
    @Path("/analyze")
    @Operation(
        summary = "LLM推論を分析",
        description = "LLMの推論プロセスをTensor Logicで形式化し、論理的整合性を検証します"
    )
    public AnalysisResult analyzeReasoning(AnalysisRequest request) {
        LOG.info("========================================");
        LOG.info("LLM推論分析開始: {}", request.query());
        LOG.info("========================================");
        
        try {
            // ステップ1: LLMに詳細な推論をさせる
            LOG.info("ステップ1: LLMに質問");
            LLMReasoningResult llmResult = llmService.queryWithDetailedReasoning(request.query());
            
            LOG.info("LLM回答: {}", llmResult.answer());
            LOG.info("推論ステップ数: {}", llmResult.reasoningSteps().size());
            for (int i = 0; i < llmResult.reasoningSteps().size(); i++) {
                LOG.info("  ステップ{}: {}", i + 1, llmResult.reasoningSteps().get(i));
            }
            
            // ステップ2: Tensor Logic化
            LOG.info("ステップ2: Tensor Logic化");
            RuleDefinition tensorLogicDef = parser.parseToTensorLogic(llmResult);
            
            LOG.info("Facts: {}", tensorLogicDef.facts().size());
            LOG.info("Rules: {}", tensorLogicDef.rules().size());
            
            // ステップ3: Tensor Logicエンジンにロード
            LOG.info("ステップ3: Tensor Logicエンジンにロード");
            engine.clear();
            
            // Factsを登録
            for (RuleDefinition.Fact fact : tensorLogicDef.facts()) {
                // valuesをList<Double>にキャスト
                @SuppressWarnings("unchecked")
                java.util.List<Double> values = (java.util.List<Double>) fact.tensor().values();
                
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
            for (RuleDefinition.RuleSpec ruledef : tensorLogicDef.rules()) {
                ai.tensorlogic.core.Rule rule = new ai.tensorlogic.core.Rule(
                    tensorLogicDef.metadata().namespace(),
                    ruledef.inputs(),
                    ruledef.output(),
                    ai.tensorlogic.core.Rule.Operation.valueOf(ruledef.operation())
                );
                engine.addRule(rule);
            }
            
            // ステップ4: 後向き推論で検証
            LOG.info("ステップ4: 後向き推論で検証");
            String goalName = tensorLogicDef.expectedResults().isEmpty() 
                ? "final_conclusion" 
                : tensorLogicDef.expectedResults().get(0).name();
            
            BackwardChainingResult verification = engine.backwardChain(
                goalName,
                tensorLogicDef.metadata().namespace()
            );
            
            LOG.info("検証結果: success={}, confidence={}", 
                verification.success(), 
                verification.getGoalConfidence());
            
            // ステップ5: LLMとTensor Logicの結果を比較
            LOG.info("ステップ5: 結果を比較");
            ComparisonResult comparison = compareResults(llmResult, verification);
            
            LOG.info("比較結果: consistent={}, difference={}", 
                comparison.consistent(), 
                comparison.difference());
            LOG.info("========================================");
            
            return new AnalysisResult(
                llmResult,
                tensorLogicDef,
                verification,
                comparison
            );
            
        } catch (Exception e) {
            LOG.error("LLM推論分析中にエラー", e);
            throw new WebApplicationException(
                "分析中にエラーが発生しました: " + e.getMessage(),
                500
            );
        }
    }
    
    /**
     * LLMとTensor Logicの結果を比較
     */
    private ComparisonResult compareResults(
        LLMReasoningResult llm,
        BackwardChainingResult tensorLogic
    ) {
        double llmConfidence = llm.confidence();
        double tensorLogicConfidence = tensorLogic.getGoalConfidence();
        double difference = Math.abs(llmConfidence - tensorLogicConfidence);
        
        // 10%以内なら一致とみなす
        boolean consistent = difference < 0.1;
        
        String message;
        if (consistent) {
            message = "✅ 推論は論理的に一貫しています";
        } else if (!tensorLogic.success()) {
            message = "⚠️ Tensor Logicで検証できませんでした";
        } else {
            message = String.format(
                "⚠️ LLMの確信度(%.2f)とTensor Logicの確信度(%.2f)に差異があります",
                llmConfidence,
                tensorLogicConfidence
            );
        }
        
        return new ComparisonResult(
            consistent,
            difference,
            llmConfidence,
            tensorLogicConfidence,
            message
        );
    }
}

/**
 * 分析リクエスト
 */
record AnalysisRequest(String query) {}

