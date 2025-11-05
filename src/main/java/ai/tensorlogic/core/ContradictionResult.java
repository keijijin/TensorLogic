package ai.tensorlogic.core;

/**
 * 矛盾検出の結果
 */
public record ContradictionResult(
    boolean hasContradiction,
    double contradictionScore,
    double expectedValue,
    double actualValue
) {}

