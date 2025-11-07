package ai.tensorlogic.llm;

import ai.tensorlogic.parser.RuleDefinition;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LLMの推論結果をTensor Logicの形式に変換するパーサー
 */
@ApplicationScoped
public class LLMReasoningParser {
    
    private static final Logger LOG = LoggerFactory.getLogger(LLMReasoningParser.class);
    
    /**
     * LLMの推論結果をRuleDefinitionに変換
     */
    public RuleDefinition parseToTensorLogic(LLMReasoningResult llmResult) {
        LOG.info("LLM推論をTensor Logic化開始: {}", llmResult.query());
        
        // メタデータ作成
        RuleDefinition.Metadata metadata = createMetadata(llmResult);
        
        // 推論ステップからFactsとRulesを抽出
        List<RuleDefinition.Fact> facts = new ArrayList<>();
        List<RuleDefinition.RuleSpec> rules = new ArrayList<>();
        
        parseReasoningSteps(llmResult.reasoningSteps(), facts, rules);
        
        // 期待結果を設定
        List<RuleDefinition.ExpectedResult> expectedResults = createExpectedResults(llmResult);
        
        RuleDefinition definition = new RuleDefinition(
            metadata, 
            List.of(),  // entities (empty)
            facts, 
            rules, 
            expectedResults
        );
        
        LOG.info("Tensor Logic化完了: Facts={}, Rules={}", facts.size(), rules.size());
        
        return definition;
    }
    
    /**
     * メタデータを作成
     */
    private RuleDefinition.Metadata createMetadata(LLMReasoningResult llmResult) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        
        return new RuleDefinition.Metadata(
            "LLM推論: " + truncate(llmResult.query(), 50),
            "1.0",
            "LLMの推論プロセスをTensor Logicで形式化",
            "LLM Parser",
            "llm-reasoning-" + timestamp.replace(":", "-")
        );
    }
    
    /**
     * 推論ステップをパースしてFactsとRulesに変換
     */
    private void parseReasoningSteps(
        List<String> steps,
        List<RuleDefinition.Fact> facts,
        List<RuleDefinition.RuleSpec> rules
    ) {
        Map<String, String> factNames = new HashMap<>();
        
        for (int i = 0; i < steps.size(); i++) {
            String step = steps.get(i);
            LOG.debug("ステップ{}をパース: {}", i + 1, step);
            
            // 確信度を抽出
            double confidence = extractConfidence(step);
            
            // ステップの内容を解析
            if (isConclusionStep(step) && i > 0) {
                // 結論ステップ → Rule
                RuleDefinition.RuleSpec rule = createRuleFromConclusion(
                    step, 
                    factNames, 
                    confidence, 
                    i
                );
                if (rule != null) {
                    rules.add(rule);
                }
            } else {
                // 前提ステップ → Fact
                RuleDefinition.Fact fact = createFactFromStatement(
                    step, 
                    confidence, 
                    i
                );
                facts.add(fact);
                
                // Fact名を記録
                factNames.put("step_" + (i + 1), fact.name());
            }
        }
        
        // 少なくとも1つのルールを作成（最後のステップから）
        if (rules.isEmpty() && facts.size() >= 2) {
            RuleDefinition.RuleSpec finalRule = createDefaultRule(facts);
            rules.add(finalRule);
        }
    }
    
    /**
     * 確信度を抽出（0.0〜1.0）
     */
    private double extractConfidence(String text) {
        // "確信度: 90%" や "(確信度: 90%)" のパターンを探す
        Pattern pattern = Pattern.compile("確信度[:：]?\\s*(\\d+)%");
        Matcher matcher = pattern.matcher(text);
        
        if (matcher.find()) {
            return Double.parseDouble(matcher.group(1)) / 100.0;
        }
        
        // 英語版も対応
        pattern = Pattern.compile("confidence[:：]?\\s*(\\d+)%");
        matcher = pattern.matcher(text);
        
        if (matcher.find()) {
            return Double.parseDouble(matcher.group(1)) / 100.0;
        }
        
        // デフォルト
        return 0.9;
    }
    
    /**
     * 結論ステップかどうかを判定
     */
    private boolean isConclusionStep(String step) {
        String lower = step.toLowerCase();
        return lower.contains("したがって") ||
               lower.contains("ゆえに") ||
               lower.contains("よって") ||
               lower.contains("therefore") ||
               lower.contains("thus") ||
               lower.contains("hence") ||
               lower.contains("結論");
    }
    
    /**
     * 文からFactを作成
     */
    private RuleDefinition.Fact createFactFromStatement(
        String statement,
        double confidence,
        int stepNumber
    ) {
        String factName = "fact_step_" + (stepNumber + 1);
        String description = cleanStatement(statement);
        
        // 確信度に応じてテンソルの値を設定
        List<Double> values = List.of(confidence);
        
        RuleDefinition.TensorSpec tensor = new RuleDefinition.TensorSpec(
            "vector",
            List.of(1),
            values,
            confidence,
            null
        );
        
        return new RuleDefinition.Fact(
            factName,
            description,
            "LLM推論ステップ" + (stepNumber + 1),
            tensor
        );
    }
    
    /**
     * 結論からRuleを作成
     */
    private RuleDefinition.RuleSpec createRuleFromConclusion(
        String conclusion,
        Map<String, String> factNames,
        double confidence,
        int stepNumber
    ) {
        String ruleName = "rule_step_" + (stepNumber + 1);
        String description = cleanStatement(conclusion);
        
        // 前のステップのFactを入力として使用
        List<String> inputs = new ArrayList<>();
        for (int i = 1; i < stepNumber; i++) {
            String factName = factNames.get("step_" + i);
            if (factName != null) {
                inputs.add(factName);
            }
        }
        
        // 入力が2つ未満の場合はnullを返す
        if (inputs.size() < 2) {
            LOG.warn("入力が不足: {}", inputs.size());
            return null;
        }
        
        String output = "conclusion_step_" + (stepNumber + 1);
        
        // 演算タイプを推定
        String operation = estimateOperation(conclusion, inputs.size());
        
        return new RuleDefinition.RuleSpec(
            ruleName,
            description,
            "LLM推論",
            inputs,
            output,
            operation,
            stepNumber,
            true
        );
    }
    
    /**
     * デフォルトのルールを作成（最後のステップから）
     */
    private RuleDefinition.RuleSpec createDefaultRule(List<RuleDefinition.Fact> facts) {
        // 全てのFactを入力とする（2つ以上の場合）
        List<String> inputs;
        
        if (facts.size() >= 2) {
            // 全てのFactを使用
            inputs = facts.stream()
                .map(RuleDefinition.Fact::name)
                .toList();
        } else {
            // 1つしかない場合はダミーを追加
            inputs = List.of(
                facts.get(0).name(),
                facts.get(0).name()
            );
        }
        
        // CONJUNCTIONは2入力しか取れないので、
        // 複数のFactがある場合は最初の2つだけを使うか、
        // または新しい演算を定義する必要がある
        // 今回は簡易的に最小確信度のFactを選択
        if (facts.size() > 2) {
            // 確信度が最も低い2つのFactを選択
            inputs = facts.stream()
                .sorted((f1, f2) -> Double.compare(
                    f1.tensor().confidence(),
                    f2.tensor().confidence()
                ))
                .limit(2)
                .map(RuleDefinition.Fact::name)
                .toList();
            
            LOG.info("{}個のFactから確信度が最も低い2つを選択: {}", facts.size(), inputs);
        }
        
        return new RuleDefinition.RuleSpec(
            "final_conclusion_rule",
            "最終的な結論を導出（全Factsの最小確信度を反映）",
            "LLM推論",
            inputs,
            "final_conclusion",
            "CONJUNCTION",
            999,
            true
        );
    }
    
    /**
     * 演算タイプを推定
     */
    private String estimateOperation(String text, int inputCount) {
        String lower = text.toLowerCase();
        
        // "かつ"、"and" → CONJUNCTION
        if (lower.contains("かつ") || lower.contains(" and ")) {
            return "CONJUNCTION";
        }
        
        // "または"、"or" → DISJUNCTION
        if (lower.contains("または") || lower.contains(" or ")) {
            return "DISJUNCTION";
        }
        
        // "ならば"、"if...then" → MODUS_PONENS
        if (lower.contains("ならば") || 
            (lower.contains("if") && lower.contains("then"))) {
            return "MODUS_PONENS";
        }
        
        // デフォルトはCONJUNCTION
        return "CONJUNCTION";
    }
    
    /**
     * 期待結果を作成
     */
    private List<RuleDefinition.ExpectedResult> createExpectedResults(LLMReasoningResult llmResult) {
        String conclusionName = "final_conclusion";
        
        RuleDefinition.ExpectedResult expected = new RuleDefinition.ExpectedResult(
            conclusionName,
            "LLMの最終結論",
            null,  // notation
            llmResult.confidence(),
            0.1
        );
        
        return List.of(expected);
    }
    
    /**
     * 文をクリーンアップ
     */
    private String cleanStatement(String text) {
        // "ステップX:" を削除
        text = text.replaceAll("^[\\d]+\\.\\s*", "");
        text = text.replaceAll("^ステップ\\d+[:：]\\s*", "");
        text = text.replaceAll("^Step \\d+[:：]\\s*", "");
        
        // 確信度の部分を削除
        text = text.replaceAll("\\s*\\(確信度[:：]?\\s*\\d+%\\)", "");
        text = text.replaceAll("\\s*\\(confidence[:：]?\\s*\\d+%\\)", "");
        
        return text.trim();
    }
    
    /**
     * 文字列を切り詰め
     */
    private String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}

