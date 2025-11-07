package ai.tensorlogic.llm;

import ai.tensorlogic.core.BackwardChainingResult;
import ai.tensorlogic.parser.RuleDefinition;

/**
 * LLM推論の分析結果
 * 
 * @param llmReasoning LLMの推論結果
 * @param tensorLogicRepresentation Tensor Logicでの表現
 * @param tensorLogicVerification Tensor Logicでの検証結果
 * @param comparison LLMとTensor Logicの比較結果
 */
public record AnalysisResult(
    LLMReasoningResult llmReasoning,
    RuleDefinition tensorLogicRepresentation,
    BackwardChainingResult tensorLogicVerification,
    ComparisonResult comparison
) {
}

