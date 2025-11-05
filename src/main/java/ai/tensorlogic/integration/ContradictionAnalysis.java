package ai.tensorlogic.integration;

/**
 * 矛盾分析結果
 */
public record ContradictionAnalysis(
    boolean hasContradiction,
    double score,
    String explanation
) {}

