package ai.tensorlogic.core;

import jakarta.enterprise.context.ApplicationScoped;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.ops.transforms.Transforms;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Tensor Logic エンジンの実装
 * 
 * テンソル演算を用いて論理推論を実行します。
 * Python版のTensorLogicBaseに相当します。
 */
@ApplicationScoped
public class TensorLogicEngine {
    
    private static final Logger LOG = LoggerFactory.getLogger(TensorLogicEngine.class);
    
    private final Map<String, INDArray> facts = new HashMap<>();
    private final Map<String, Rule> rules = new HashMap<>();
    
    /**
     * 事実（ファクト）を追加
     */
    public void addFact(String name, INDArray tensor) {
        facts.put(name, tensor);
        LOG.info("事実 '{}' を追加: shape={}", name, java.util.Arrays.toString(tensor.shape()));
    }
    
    /**
     * 推論ルールを追加
     */
    public void addRule(String name, Rule rule) {
        rules.put(name, rule);
        LOG.info("ルール '{}' を追加: {} -> {}", name, rule.inputs(), rule.output());
    }
    
    /**
     * 全てのルールを取得（読み取り専用）
     */
    public Map<String, Rule> getAllRules() {
        return Map.copyOf(rules);
    }
    
    /**
     * 全ての事実を取得（読み取り専用）
     */
    public Map<String, INDArray> getAllFacts() {
        return Map.copyOf(facts);
    }
    
    /**
     * 前向き推論を実行
     */
    public Map<String, INDArray> forwardChain() {
        LOG.info("=== 前向き推論を開始 ===");
        Map<String, INDArray> newFacts = new HashMap<>();
        
        for (Map.Entry<String, Rule> entry : rules.entrySet()) {
            Rule rule = entry.getValue();
            
            // すべての入力が揃っているかチェック
            boolean allInputsAvailable = rule.inputs().stream()
                    .allMatch(facts::containsKey);
            
            if (allInputsAvailable) {
                INDArray result = applyRule(rule);
                newFacts.put(rule.output(), result);
                LOG.info("推論: {} -> {}", rule.inputs(), rule.output());
            }
        }
        
        // 新しい事実を追加
        facts.putAll(newFacts);
        return newFacts;
    }
    
    /**
     * ルールを適用
     */
    private INDArray applyRule(Rule rule) {
        return switch (rule.operation()) {
            case MODUS_PONENS -> {
                // A かつ (A→B) から B を導出
                INDArray premise = facts.get(rule.inputs().get(0));
                INDArray implication = facts.get(rule.inputs().get(1));
                yield premise.mmul(implication);
            }
            case CONJUNCTION -> {
                // A と B の論理積
                INDArray a = facts.get(rule.inputs().get(0));
                INDArray b = facts.get(rule.inputs().get(1));
                yield Transforms.min(a, b);
            }
            case CHAIN -> {
                // 関係の合成（行列の積）
                INDArray a = facts.get(rule.inputs().get(0));
                INDArray b = facts.get(rule.inputs().get(1));
                yield a.mmul(b);
            }
            default -> throw new IllegalArgumentException("Unknown operation: " + rule.operation());
        };
    }
    
    /**
     * 事実を取得
     */
    public INDArray getFact(String name) {
        return facts.get(name);
    }
    
    /**
     * 推論の妥当性を検証
     */
    public ValidationResult validateReasoning(INDArray premise1, 
                                               INDArray premise2, 
                                               INDArray conclusion,
                                               double threshold) {
        // 期待される結論を計算
        INDArray expected = premise1.mmul(premise2);
        
        // 誤差を計算
        INDArray error = Transforms.abs(expected.sub(conclusion));
        double meanError = error.meanNumber().doubleValue();
        
        boolean isValid = meanError < threshold;
        double confidence = 1.0 - meanError;
        
        return new ValidationResult(
            isValid,
            confidence,
            expected,
            conclusion,
            error
        );
    }
    
    /**
     * 矛盾を検出
     */
    public ContradictionResult detectContradiction(INDArray claim1,
                                                    INDArray claim2,
                                                    INDArray claim3) {
        // 推移律のチェック: A>B かつ B>C => A>C
        double a_gt_b = claim1.getDouble(0);
        double b_gt_c = claim2.getDouble(0);
        double c_gt_a = claim3.getDouble(0);
        
        double expected_a_gt_c = Math.min(a_gt_b, b_gt_c);
        double contradiction_score = Math.min(expected_a_gt_c, c_gt_a);
        
        boolean hasContradiction = contradiction_score > 0.3;
        
        return new ContradictionResult(
            hasContradiction,
            contradiction_score,
            expected_a_gt_c,
            c_gt_a
        );
    }
    
    /**
     * 確信度の伝播
     */
    public ConfidencePropagation propagateConfidence(double... confidences) {
        double result = 1.0;
        for (double conf : confidences) {
            result *= conf;
        }
        
        double uncertainty = 1.0 - result;
        
        return new ConfidencePropagation(result, uncertainty, confidences);
    }
}

