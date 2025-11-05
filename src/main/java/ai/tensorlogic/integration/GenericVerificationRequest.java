package ai.tensorlogic.integration;

import java.util.List;
import java.util.Map;

/**
 * 汎用検証リクエスト
 */
public record GenericVerificationRequest(
    String query,                           // LLMへの質問
    String ruleFile,                        // ルールファイルのパス（オプション）
    Map<String, List<Double>> customFacts,  // カスタム事実（オプション）
    Map<String, Double> expectedOutputs,    // 期待される出力（オプション）
    Double tolerance,                       // 許容誤差（デフォルト: 0.05）
    Boolean extractFactsFromLLM            // LLMから事実を抽出するか（デフォルト: false）
) {
    public GenericVerificationRequest {
        if (tolerance == null) {
            tolerance = 0.05;
        }
        if (extractFactsFromLLM == null) {
            extractFactsFromLLM = false;
        }
    }
}

