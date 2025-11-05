package ai.tensorlogic.parser;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * ルール定義ファイルのデータ構造
 * 
 * YAMLファイルから読み込まれるルール定義
 */
public record RuleDefinition(
    Metadata metadata,
    List<Entity> entities,
    List<Fact> facts,
    List<RuleSpec> rules,
    @JsonProperty("expected_results") List<ExpectedResult> expectedResults
) {
    
    /**
     * メタデータ
     */
    public record Metadata(
        String name,
        String version,
        String description,
        String author
    ) {}
    
    /**
     * エンティティ定義
     */
    public record Entity(
        String name,
        String type,
        Integer id,
        String description
    ) {}
    
    /**
     * 事実（ファクト）定義
     */
    public record Fact(
        String name,
        String description,
        String notation,
        TensorSpec tensor
    ) {}
    
    /**
     * テンソル仕様
     */
    public record TensorSpec(
        String type,        // "vector", "matrix", "tensor"
        List<Integer> shape,
        Object values,      // List<Double> or List<List<Double>>
        Double confidence,
        Map<String, List<String>> labels
    ) {}
    
    /**
     * ルール仕様
     */
    public record RuleSpec(
        String name,
        String description,
        String notation,
        List<String> inputs,
        String output,
        String operation,   // "MODUS_PONENS", "CONJUNCTION", "CHAIN"
        Integer priority,
        Boolean enabled
    ) {}
    
    /**
     * 期待される結果
     */
    public record ExpectedResult(
        String name,
        String description,
        String notation,
        @JsonProperty("expected_value") Double expectedValue,
        Double tolerance
    ) {}
}

