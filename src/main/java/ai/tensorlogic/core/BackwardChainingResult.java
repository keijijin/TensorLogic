package ai.tensorlogic.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 後向き推論の結果を保持するレコード
 * 
 * @param success 推論が成功したかどうか
 * @param goal 達成しようとした目標
 * @param reasoningPath 推論パス（目標から前提条件へのトレース）
 * @param requiredFacts 目標達成に必要な事実とその値
 */
public record BackwardChainingResult(
    boolean success,
    String goal,
    List<String> reasoningPath,
    @JsonIgnore Map<String, INDArray> requiredFacts
) {
    
    /**
     * JSON出力用に必要な事実を人間が読める形式で取得
     */
    public Map<String, String> getRequiredFactsFormatted() {
        Map<String, String> formatted = new HashMap<>();
        for (Map.Entry<String, INDArray> entry : requiredFacts.entrySet()) {
            INDArray array = entry.getValue();
            if (array.isScalar()) {
                formatted.put(entry.getKey(), String.format("%.4f", array.getDouble(0)));
            } else if (array.isVector()) {
                formatted.put(entry.getKey(), array.toString());
            } else {
                formatted.put(entry.getKey(), array.shapeInfoToString());
            }
        }
        return formatted;
    }
    
    /**
     * 目標の達成確信度を取得
     */
    public double getGoalConfidence() {
        INDArray goalValue = requiredFacts.get(goal);
        if (goalValue != null) {
            if (goalValue.isScalar()) {
                return goalValue.getDouble(0);
            } else if (goalValue.isVector() && goalValue.length() > 0) {
                return goalValue.getDouble(0);
            } else if (goalValue.isMatrix()) {
                return goalValue.getDouble(0, 0);
            }
        }
        return 0.0;
    }
    
    /**
     * JSON形式で出力
     */
    public String toJson() {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"success\": ").append(success).append(",\n");
        json.append("  \"goal\": \"").append(goal).append("\",\n");
        json.append("  \"goalConfidence\": ").append(String.format("%.4f", getGoalConfidence())).append(",\n");
        json.append("  \"reasoningPath\": [\n");
        
        for (int i = 0; i < reasoningPath.size(); i++) {
            json.append("    \"").append(reasoningPath.get(i).replace("\"", "\\\"")).append("\"");
            if (i < reasoningPath.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }
        
        json.append("  ],\n");
        json.append("  \"requiredFacts\": {\n");
        
        Map<String, String> formatted = getRequiredFactsFormatted();
        int count = 0;
        for (Map.Entry<String, String> entry : formatted.entrySet()) {
            json.append("    \"").append(entry.getKey()).append("\": \"")
                .append(entry.getValue()).append("\"");
            if (count < formatted.size() - 1) {
                json.append(",");
            }
            json.append("\n");
            count++;
        }
        
        json.append("  }\n");
        json.append("}");
        
        return json.toString();
    }
    
    @Override
    public String toString() {
        return toJson();
    }
}

