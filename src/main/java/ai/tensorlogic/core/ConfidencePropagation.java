package ai.tensorlogic.core;

/**
 * 確信度の伝播結果
 */
public record ConfidencePropagation(
    double finalConfidence,
    double uncertainty,
    double[] intermediateConfidences
) {}

