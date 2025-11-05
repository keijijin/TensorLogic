package ai.tensorlogic.core;

import java.util.List;

/**
 * 推論ルールを表すレコード
 * 
 * Java 21のRecordを使用して不変なデータクラスを定義
 */
public record Rule(
    List<String> inputs,
    String output,
    Operation operation
) {
    
    public enum Operation {
        MODUS_PONENS,   // 三段論法: A かつ (A→B) から B
        CONJUNCTION,    // 論理積: A ∧ B
        CHAIN,         // 関係の合成: R1 ○ R2
        DISJUNCTION    // 論理和: A ∨ B
    }
    
    /**
     * ビルダーパターンでルールを構築
     */
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private List<String> inputs;
        private String output;
        private Operation operation = Operation.MODUS_PONENS;
        
        public Builder inputs(String... inputs) {
            this.inputs = List.of(inputs);
            return this;
        }
        
        public Builder output(String output) {
            this.output = output;
            return this;
        }
        
        public Builder operation(Operation operation) {
            this.operation = operation;
            return this;
        }
        
        public Rule build() {
            return new Rule(inputs, output, operation);
        }
    }
}

