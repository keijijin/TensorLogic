package ai.tensorlogic.llm;

/**
 * LLMとTensor Logicの比較結果
 * 
 * @param consistent 推論が一貫しているか
 * @param difference 確信度の差異
 * @param llmConfidence LLMの確信度
 * @param tensorLogicConfidence Tensor Logicの確信度
 * @param message 説明メッセージ
 */
public record ComparisonResult(
    boolean consistent,
    double difference,
    double llmConfidence,
    double tensorLogicConfidence,
    String message
) {
}

