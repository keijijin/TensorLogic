package ai.tensorlogic.integration;

import ai.tensorlogic.core.ValidationResult;
import java.util.List;

/**
 * 検証済み推論結果
 */
public record VerifiedReasoningResult(
    String answer,
    double llmConfidence,
    List<String> reasoningSteps,
    boolean isLogicallySound,
    double validationConfidence,
    ValidationResult validationDetails
) {}

