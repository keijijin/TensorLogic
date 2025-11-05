package ai.tensorlogic.core;

import org.nd4j.linalg.api.ndarray.INDArray;

/**
 * 検証結果を表すレコード
 */
public record ValidationResult(
    boolean isValid,
    double confidence,
    INDArray expected,
    INDArray actual,
    INDArray error
) {
    
    /**
     * JSON用の文字列表現
     */
    public String toJson() {
        return String.format("""
            {
                "isValid": %b,
                "confidence": %.4f,
                "expected": %s,
                "actual": %s,
                "meanError": %.4f
            }
            """,
            isValid,
            confidence,
            expected.toStringFull(),
            actual.toStringFull(),
            error.meanNumber().doubleValue()
        );
    }
}

