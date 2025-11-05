package ai.tensorlogic.integration;

import ai.tensorlogic.core.TensorLogicEngine;
import ai.tensorlogic.llm.LLMResponse;
import ai.tensorlogic.llm.LLMService;
import ai.tensorlogic.parser.RuleDefinition;
import ai.tensorlogic.parser.RuleLoader;
import ai.tensorlogic.parser.TensorConverter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 汎用的なLLM検証サービス
 * 
 * 外部ファイルから読み込んだルールを使用して、
 * LLMの回答を検証します。
 */
@ApplicationScoped
public class GenericLLMVerifier {
    
    private static final Logger LOG = LoggerFactory.getLogger(GenericLLMVerifier.class);
    
    @Inject
    TensorLogicEngine engine;
    
    @Inject
    LLMService llmService;
    
    @Inject
    RuleLoader ruleLoader;
    
    @Inject
    TensorConverter converter;
    
    /**
     * 汎用的なLLM推論検証
     * 
     * @param request 検証リクエスト
     * @return 検証結果
     */
    public GenericVerificationResult verify(GenericVerificationRequest request) {
        LOG.info("=== 汎用LLM検証を開始 ===");
        LOG.info("質問: {}", request.query());
        
        try {
            // 1. ルールファイルを読み込み（指定されている場合）
            if (request.ruleFile() != null && !request.ruleFile().isEmpty()) {
                LOG.info("ルールファイルを読み込み: {}", request.ruleFile());
                RuleLoader.LoadResult loadResult = ruleLoader.loadFromResource(request.ruleFile());
                LOG.info("ルール読み込み完了: {}", loadResult.summary());
            }
            
            // 2. LLMに問い合わせ
            LLMResponse llmResponse = llmService.queryWithReasoning(request.query());
            LOG.info("LLMの回答: {}", llmResponse.answer());
            LOG.info("LLM確信度: {}", llmResponse.confidence());
            
            // 3. LLMの回答から事実を抽出・登録
            extractAndRegisterFacts(llmResponse, request);
            
            // 4. 推論エンジンで前向き推論を実行
            LOG.info("推論エンジンで検証を実行...");
            Map<String, INDArray> inferredFacts = engine.forwardChain();
            LOG.info("推論完了: {}個の新しい事実を推論", inferredFacts.size());
            
            // 5. 期待される結果と比較
            VerificationStatus status = verifyAgainstExpectedResults(
                inferredFacts, 
                request.expectedOutputs(),
                request.tolerance()
            );
            
            // 6. 結果を構築
            return buildVerificationResult(
                llmResponse,
                inferredFacts,
                status,
                request
            );
            
        } catch (Exception e) {
            LOG.error("検証中にエラーが発生: {}", e.getMessage(), e);
            return GenericVerificationResult.error(
                request.query(),
                "検証エラー: " + e.getMessage()
            );
        }
    }
    
    /**
     * LLMの回答から事実を抽出してエンジンに登録
     */
    private void extractAndRegisterFacts(LLMResponse llmResponse, GenericVerificationRequest request) {
        // カスタム事実が指定されている場合
        if (request.customFacts() != null && !request.customFacts().isEmpty()) {
            LOG.info("カスタム事実を登録: {}個", request.customFacts().size());
            
            for (Map.Entry<String, List<Double>> entry : request.customFacts().entrySet()) {
                String factName = entry.getKey();
                List<Double> values = entry.getValue();
                
                // リストを配列に変換
                double[] valueArray = values.stream().mapToDouble(Double::doubleValue).toArray();
                INDArray tensor = org.nd4j.linalg.factory.Nd4j.create(valueArray);
                
                engine.addFact(factName, tensor);
                LOG.debug("事実を登録: {} = {}", factName, tensor);
            }
        }
        
        // LLMの確信度から事実を生成（デフォルトの挙動）
        if (request.extractFactsFromLLM() != null && request.extractFactsFromLLM()) {
            String llmFactName = "llm_confidence_" + System.currentTimeMillis();
            INDArray llmConfidenceTensor = org.nd4j.linalg.factory.Nd4j.create(
                new double[]{llmResponse.confidence()}
            );
            engine.addFact(llmFactName, llmConfidenceTensor);
            LOG.debug("LLM確信度を事実として登録: {} = {}", llmFactName, llmResponse.confidence());
        }
    }
    
    /**
     * 推論結果を期待される結果と比較
     */
    private VerificationStatus verifyAgainstExpectedResults(
            Map<String, INDArray> inferredFacts,
            Map<String, Double> expectedOutputs,
            double tolerance) {
        
        if (expectedOutputs == null || expectedOutputs.isEmpty()) {
            LOG.info("期待される出力が指定されていません。推論のみ実行。");
            return VerificationStatus.NO_EXPECTATIONS;
        }
        
        List<String> matches = new ArrayList<>();
        List<String> mismatches = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        
        for (Map.Entry<String, Double> expected : expectedOutputs.entrySet()) {
            String factName = expected.getKey();
            double expectedValue = expected.getValue();
            
            INDArray actualTensor = engine.getFact(factName);
            
            if (actualTensor == null) {
                missing.add(factName);
                LOG.warn("期待される事実が見つかりません: {}", factName);
                continue;
            }
            
            double actualValue = actualTensor.getDouble(0);
            double diff = Math.abs(expectedValue - actualValue);
            
            if (diff <= tolerance) {
                matches.add(String.format("%s: expected=%.3f, actual=%.3f ✓", 
                    factName, expectedValue, actualValue));
                LOG.info("✓ 検証成功: {} (期待値={}, 実際={}, 差={})", 
                    factName, expectedValue, actualValue, diff);
            } else {
                mismatches.add(String.format("%s: expected=%.3f, actual=%.3f (diff=%.3f)", 
                    factName, expectedValue, actualValue, diff));
                LOG.warn("✗ 検証失敗: {} (期待値={}, 実際={}, 差={})", 
                    factName, expectedValue, actualValue, diff);
            }
        }
        
        boolean isValid = mismatches.isEmpty() && missing.isEmpty();
        double validationScore = (double) matches.size() / expectedOutputs.size();
        
        return new VerificationStatus(
            isValid,
            validationScore,
            matches,
            mismatches,
            missing
        );
    }
    
    /**
     * 検証結果を構築
     */
    private GenericVerificationResult buildVerificationResult(
            LLMResponse llmResponse,
            Map<String, INDArray> inferredFacts,
            VerificationStatus status,
            GenericVerificationRequest request) {
        
        // 推論された事実を文字列に変換
        Map<String, String> inferredFactsStr = new HashMap<>();
        for (Map.Entry<String, INDArray> entry : inferredFacts.entrySet()) {
            inferredFactsStr.put(entry.getKey(), entry.getValue().toString());
        }
        
        return new GenericVerificationResult(
            true,
            request.query(),
            llmResponse.answer(),
            llmResponse.confidence(),
            llmResponse.reasoningSteps(),
            status.isValid(),
            status.validationScore(),
            inferredFactsStr,
            status.matches(),
            status.mismatches(),
            status.missing(),
            null
        );
    }
}

/**
 * 検証ステータス（内部使用）
 */
record VerificationStatus(
    boolean isValid,
    double validationScore,
    List<String> matches,
    List<String> mismatches,
    List<String> missing
) {
    static final VerificationStatus NO_EXPECTATIONS = new VerificationStatus(
        true, 1.0, List.of("期待される出力なし - 推論のみ実行"), List.of(), List.of()
    );
}


