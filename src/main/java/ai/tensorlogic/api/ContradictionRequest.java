package ai.tensorlogic.api;

/**
 * 矛盾検出リクエスト
 */
public record ContradictionRequest(
    double claim1,
    double claim2,
    double claim3
) {}

