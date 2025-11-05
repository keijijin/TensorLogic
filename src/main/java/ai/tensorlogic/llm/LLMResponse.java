package ai.tensorlogic.llm;

import java.util.List;

/**
 * LLMからの応答
 */
public record LLMResponse(
    String answer,
    double confidence,
    List<String> reasoningSteps
) {}

