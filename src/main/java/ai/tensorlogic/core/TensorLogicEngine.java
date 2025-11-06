package ai.tensorlogic.core;

import jakarta.enterprise.context.ApplicationScoped;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.ops.transforms.Transforms;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Tensor Logic エンジンの実装
 * 
 * テンソル演算を用いて論理推論を実行します。
 * Python版のTensorLogicBaseに相当します。
 * 
 * 主要機能:
 * - Forward Chaining（前向き推論）: 事実からルールを適用して新しい事実を導出
 * - Backward Chaining（後向き推論）: 目標から逆向きに必要な条件を探索
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
     * 前向き推論を実行（全ネームスペース）
     */
    public Map<String, INDArray> forwardChain() {
        return forwardChain(null);
    }
    
    /**
     * 前向き推論を実行（ネームスペース指定）
     * 
     * @param namespaceFilter ネームスペース（nullまたは"*"の場合は全ルールを適用）
     */
    public Map<String, INDArray> forwardChain(String namespaceFilter) {
        if (namespaceFilter == null || namespaceFilter.equals("*")) {
            LOG.info("=== 前向き推論を開始（全ネームスペース） ===");
        } else {
            LOG.info("=== 前向き推論を開始（ネームスペース: {}） ===", namespaceFilter);
        }
        
        Map<String, INDArray> newFacts = new HashMap<>();
        
        for (Map.Entry<String, Rule> entry : rules.entrySet()) {
            Rule rule = entry.getValue();
            
            // ネームスペースフィルタリング
            if (namespaceFilter != null && !namespaceFilter.equals("*") 
                && !rule.namespace().equals(namespaceFilter)) {
                LOG.debug("ルール '{}' をスキップ（ネームスペース: {}）", 
                    entry.getKey(), rule.namespace());
                continue;
            }
            
            // すべての入力が揃っているかチェック
            boolean allInputsAvailable = rule.inputs().stream()
                    .allMatch(facts::containsKey);
            
            if (allInputsAvailable) {
                INDArray result = applyRule(rule);
                newFacts.put(rule.output(), result);
                LOG.info("推論: {} -> {} (namespace: {})", 
                    rule.inputs(), rule.output(), rule.namespace());
            }
        }
        
        // 新しい事実を追加
        facts.putAll(newFacts);
        LOG.info("前向き推論完了: {}個の新しい事実を導出", newFacts.size());
        return newFacts;
    }
    
    /**
     * 後向き推論を実行（全ネームスペース）
     * 
     * 目標から逆向きに推論し、その目標を達成するために必要な前提条件を探索します。
     * 
     * @param goal 達成したい目標（事実の名前）
     * @return 後向き推論の結果
     */
    public BackwardChainingResult backwardChain(String goal) {
        return backwardChain(goal, null);
    }
    
    /**
     * 後向き推論を実行（ネームスペース指定）
     * 
     * @param goal 達成したい目標（事実の名前）
     * @param namespaceFilter ネームスペース（nullまたは"*"の場合は全ルールを適用）
     * @return 後向き推論の結果
     */
    public BackwardChainingResult backwardChain(String goal, String namespaceFilter) {
        if (namespaceFilter == null || namespaceFilter.equals("*")) {
            LOG.info("=== 後向き推論を開始: 目標='{}' （全ネームスペース） ===", goal);
        } else {
            LOG.info("=== 後向き推論を開始: 目標='{}' （ネームスペース: {}） ===", goal, namespaceFilter);
        }
        
        List<String> path = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Map<String, INDArray> requiredFacts = new HashMap<>();
        
        boolean success = backwardChainRecursive(goal, path, visited, requiredFacts, namespaceFilter);
        
        if (success) {
            LOG.info("✓ 後向き推論成功: 目標 '{}' は達成可能", goal);
            LOG.info("  推論パス: {}", String.join(" <- ", path));
            LOG.info("  必要な事実: {}", requiredFacts.keySet());
        } else {
            LOG.warn("✗ 後向き推論失敗: 目標 '{}' は達成不可能", goal);
        }
        
        return new BackwardChainingResult(success, goal, path, requiredFacts);
    }
    
    /**
     * 後向き推論の再帰的実装
     */
    private boolean backwardChainRecursive(String goal, List<String> path, 
                                           Set<String> visited, Map<String, INDArray> requiredFacts,
                                           String namespaceFilter) {
        // 既に計算済み（requiredFactsに存在）の場合
        if (requiredFacts.containsKey(goal)) {
            LOG.debug("  ✓ '{}' は既に計算済み", goal);
            return true;
        }
        
        // 既に訪問済みの場合は無限ループを防ぐ
        if (visited.contains(goal)) {
            LOG.debug("  ⚠ '{}' は訪問済みだが計算されていない（循環参照の可能性）", goal);
            return false;
        }
        visited.add(goal);
        
        // 既に事実として存在する場合
        if (facts.containsKey(goal)) {
            path.add(goal + " [既知]");
            requiredFacts.put(goal, facts.get(goal));
            LOG.debug("  ✓ '{}' は既知の事実", goal);
            return true;
        }
        
        // 目標を生成できるルールを探す
        for (Map.Entry<String, Rule> entry : rules.entrySet()) {
            Rule rule = entry.getValue();
            
            // ネームスペースフィルタリング
            if (namespaceFilter != null && !namespaceFilter.equals("*") 
                && !rule.namespace().equals(namespaceFilter)) {
                LOG.debug("  → ルール '{}' をスキップ（ネームスペース: {}）", 
                    entry.getKey(), rule.namespace());
                continue;
            }
            
            if (rule.output().equals(goal)) {
                LOG.debug("  → ルール '{}' が目標 '{}' を生成可能: {} -> {} (namespace: {})", 
                    entry.getKey(), goal, rule.inputs(), rule.output(), rule.namespace());
                
                // このルールのすべての入力を再帰的に解決
                boolean allInputsResolved = true;
                for (String input : rule.inputs()) {
                    if (!backwardChainRecursive(input, path, visited, requiredFacts, namespaceFilter)) {
                        allInputsResolved = false;
                        break;
                    }
                }
                
                if (allInputsResolved) {
                    // すべての入力が解決できた場合、ルールを適用
                    path.add(goal + " ← [" + String.join(", ", rule.inputs()) + "] (ns: " + rule.namespace() + ")");
                    
                    // ルールを適用して目標を計算（後向き推論用: requiredFactsから取得）
                    INDArray result = applyRuleForBackwardChaining(rule, requiredFacts);
                    
                    // Nullチェック - ルール適用が失敗した場合
                    if (result == null) {
                        LOG.warn("  ✗ ルール適用失敗: {} (namespace: {})", goal, rule.namespace());
                        path.remove(path.size() - 1); // 失敗したパスを削除
                        continue; // 次のルールを試す
                    }
                    
                    requiredFacts.put(goal, result);
                    
                    LOG.debug("  ✓ ルール適用成功: {} = {} (namespace: {})", goal, result, rule.namespace());
                    return true;
                }
            }
        }
        
        LOG.debug("  ✗ '{}' を生成するルール（namespace={}）が見つかりません", goal, namespaceFilter);
        return false;
    }
    
    /**
     * ルールを適用（後向き推論用）
     * requiredFacts から値を取得し、なければ facts から取得
     */
    private INDArray applyRuleForBackwardChaining(Rule rule, Map<String, INDArray> requiredFacts) {
        return switch (rule.operation()) {
            case MODUS_PONENS -> {
                // A かつ (A→B) から B を導出
                INDArray premise = getFactValue(rule.inputs().get(0), requiredFacts);
                INDArray implication = getFactValue(rule.inputs().get(1), requiredFacts);
                
                // Nullチェック
                if (premise == null) {
                    LOG.warn("事実が見つかりません: {}", rule.inputs().get(0));
                    yield null;
                }
                if (implication == null) {
                    LOG.warn("事実が見つかりません: {}", rule.inputs().get(1));
                    yield null;
                }
                
                yield premise.mmul(implication);
            }
            case CONJUNCTION -> {
                // A と B の論理積（最小値）
                INDArray a = getFactValue(rule.inputs().get(0), requiredFacts);
                INDArray b = getFactValue(rule.inputs().get(1), requiredFacts);
                
                // Nullチェック
                if (a == null) {
                    LOG.warn("事実が見つかりません: {}", rule.inputs().get(0));
                    yield null;
                }
                if (b == null) {
                    LOG.warn("事実が見つかりません: {}", rule.inputs().get(1));
                    yield null;
                }
                
                yield Transforms.min(a, b);
            }
            case DISJUNCTION -> {
                // A または B の論理和（最大値）
                INDArray a = getFactValue(rule.inputs().get(0), requiredFacts);
                INDArray b = getFactValue(rule.inputs().get(1), requiredFacts);
                
                // Nullチェック
                if (a == null) {
                    LOG.warn("事実が見つかりません: {}", rule.inputs().get(0));
                    yield null;
                }
                if (b == null) {
                    LOG.warn("事実が見つかりません: {}", rule.inputs().get(1));
                    yield null;
                }
                
                yield Transforms.max(a, b);
            }
            case CHAIN -> {
                // 関係の合成（行列の積）
                INDArray a = getFactValue(rule.inputs().get(0), requiredFacts);
                INDArray b = getFactValue(rule.inputs().get(1), requiredFacts);
                
                // Nullチェック
                if (a == null) {
                    LOG.warn("事実が見つかりません: {}", rule.inputs().get(0));
                    yield null;
                }
                if (b == null) {
                    LOG.warn("事実が見つかりません: {}", rule.inputs().get(1));
                    yield null;
                }
                
                yield a.mmul(b);
            }
            default -> throw new IllegalArgumentException("Unknown operation: " + rule.operation());
        };
    }
    
    /**
     * 事実の値を取得（requiredFacts を優先、なければ facts から）
     */
    private INDArray getFactValue(String factName, Map<String, INDArray> requiredFacts) {
        // まず requiredFacts から探す（後向き推論で計算された中間結果）
        if (requiredFacts.containsKey(factName)) {
            return requiredFacts.get(factName);
        }
        // なければ facts から探す（既知の事実）
        return facts.get(factName);
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
                
                // Nullチェック
                if (premise == null) {
                    LOG.warn("事実が見つかりません: {}", rule.inputs().get(0));
                    yield null;
                }
                if (implication == null) {
                    LOG.warn("事実が見つかりません: {}", rule.inputs().get(1));
                    yield null;
                }
                
                yield premise.mmul(implication);
            }
            case CONJUNCTION -> {
                // A と B の論理積（最小値）
                INDArray a = facts.get(rule.inputs().get(0));
                INDArray b = facts.get(rule.inputs().get(1));
                
                // Nullチェック
                if (a == null) {
                    LOG.warn("事実が見つかりません: {}", rule.inputs().get(0));
                    yield null;
                }
                if (b == null) {
                    LOG.warn("事実が見つかりません: {}", rule.inputs().get(1));
                    yield null;
                }
                
                yield Transforms.min(a, b);
            }
            case DISJUNCTION -> {
                // A または B の論理和（最大値）
                INDArray a = facts.get(rule.inputs().get(0));
                INDArray b = facts.get(rule.inputs().get(1));
                
                // Nullチェック
                if (a == null) {
                    LOG.warn("事実が見つかりません: {}", rule.inputs().get(0));
                    yield null;
                }
                if (b == null) {
                    LOG.warn("事実が見つかりません: {}", rule.inputs().get(1));
                    yield null;
                }
                
                yield Transforms.max(a, b);
            }
            case CHAIN -> {
                // 関係の合成（行列の積）
                INDArray a = facts.get(rule.inputs().get(0));
                INDArray b = facts.get(rule.inputs().get(1));
                
                // Nullチェック
                if (a == null) {
                    LOG.warn("事実が見つかりません: {}", rule.inputs().get(0));
                    yield null;
                }
                if (b == null) {
                    LOG.warn("事実が見つかりません: {}", rule.inputs().get(1));
                    yield null;
                }
                
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

