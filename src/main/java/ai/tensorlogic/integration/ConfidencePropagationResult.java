package ai.tensorlogic.integration;

/**
 * 確信度伝播の結果
 */
public record ConfidencePropagationResult(
    double finalConfidence,
    double uncertainty,
    double[] steps
) {}

