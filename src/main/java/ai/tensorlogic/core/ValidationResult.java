package ai.tensorlogic.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.nd4j.linalg.api.ndarray.INDArray;

/**
 * 検証結果を表すレコード
 */
public record ValidationResult(
    boolean isValid,
    double confidence,
    @JsonIgnore INDArray expected,      // JSON化から除外
    @JsonIgnore INDArray actual,        // JSON化から除外
    @JsonIgnore INDArray error          // JSON化から除外
) {
    
    /**
     * 人間が読みやすい形式で期待値を取得
     */
    public String getExpectedValue() {
        if (expected == null) return "null";
        return formatTensor(expected);
    }
    
    /**
     * 人間が読みやすい形式で実際の値を取得
     */
    public String getActualValue() {
        if (actual == null) return "null";
        return formatTensor(actual);
    }
    
    /**
     * 誤差の平均値を取得
     */
    public double getMeanError() {
        if (error == null) return 0.0;
        return error.meanNumber().doubleValue();
    }
    
    /**
     * 誤差の最大値を取得
     */
    public double getMaxError() {
        if (error == null) return 0.0;
        return error.maxNumber().doubleValue();
    }
    
    /**
     * テンソルを読みやすい文字列に変換
     */
    private String formatTensor(INDArray tensor) {
        if (tensor.isScalar()) {
            return String.format("%.4f", tensor.getDouble(0));
        } else if (tensor.isVector() && tensor.length() <= 5) {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < tensor.length(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(String.format("%.4f", tensor.getDouble(i)));
            }
            sb.append("]");
            return sb.toString();
        } else {
            return String.format("[%d elements, mean=%.4f]", 
                tensor.length(), 
                tensor.meanNumber().doubleValue());
        }
    }
    
    /**
     * JSON用の文字列表現
     */
    public String toJson() {
        return String.format("""
            {
                "isValid": %b,
                "confidence": %.4f,
                "expectedValue": "%s",
                "actualValue": "%s",
                "meanError": %.4f,
                "maxError": %.4f
            }
            """,
            isValid,
            confidence,
            getExpectedValue(),
            getActualValue(),
            getMeanError(),
            getMaxError()
        );
    }
}

