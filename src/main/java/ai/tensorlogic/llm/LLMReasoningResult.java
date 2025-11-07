package ai.tensorlogic.llm;

import java.util.List;

/**
 * LLMの詳細な推論結果
 * 
 * @param query 元の質問
 * @param answer LLMの回答
 * @param reasoningSteps 推論ステップのリスト
 * @param confidence 全体の確信度
 */
public record LLMReasoningResult(
    String query,
    String answer,
    List<String> reasoningSteps,
    double confidence
) {
    /**
     * 主な結論を取得
     */
    public String getMainConclusion() {
        if (reasoningSteps == null || reasoningSteps.isEmpty()) {
            return "unknown";
        }
        // 最後のステップから結論を推定
        String lastStep = reasoningSteps.get(reasoningSteps.size() - 1);
        return extractConclusionName(lastStep);
    }
    
    private String extractConclusionName(String step) {
        // "したがって、Xである" → "X"
        if (step.contains("したがって")) {
            return "final_conclusion";
        }
        return "conclusion";
    }
}

