package ai.tensorlogic.integration;

import java.util.List;
import java.util.Map;

/**
 * 汎用検証結果
 */
public record GenericVerificationResult(
    boolean success,
    String query,
    String llmAnswer,
    double llmConfidence,
    List<String> reasoningSteps,
    boolean logicallySound,
    double validationScore,
    Map<String, String> inferredFacts,
    List<String> verificationMatches,
    List<String> verificationMismatches,
    List<String> missingExpectedFacts,
    String errorMessage
) {
    public static GenericVerificationResult error(String query, String errorMessage) {
        return new GenericVerificationResult(
            false, query, null, 0.0, List.of(), 
            false, 0.0, Map.of(), List.of(), List.of(), List.of(), 
            errorMessage
        );
    }
}

