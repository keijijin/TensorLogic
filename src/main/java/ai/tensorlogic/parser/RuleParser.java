package ai.tensorlogic.parser;

import ai.tensorlogic.core.Rule;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ルール定義ファイルのパーサー
 * 
 * YAMLファイルからルール定義を読み込み、
 * Ruleオブジェクトに変換します。
 */
@ApplicationScoped
public class RuleParser {
    
    private static final Logger LOG = LoggerFactory.getLogger(RuleParser.class);
    
    private final ObjectMapper yamlMapper;
    
    public RuleParser() {
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
    }
    
    /**
     * ファイルパスからルール定義を読み込み
     */
    public RuleDefinition parseFile(String filePath) throws Exception {
        LOG.info("ルールファイルを読み込み: {}", filePath);
        
        File file = new File(filePath);
        RuleDefinition definition = yamlMapper.readValue(file, RuleDefinition.class);
        
        LOG.info("ルール定義を読み込みました: {} (ルール数: {})", 
            definition.metadata().name(), 
            definition.rules().size());
        
        return definition;
    }
    
    /**
     * InputStreamからルール定義を読み込み
     */
    public RuleDefinition parseStream(InputStream inputStream) throws Exception {
        LOG.info("ルール定義を読み込み中...");
        
        RuleDefinition definition = yamlMapper.readValue(inputStream, RuleDefinition.class);
        
        LOG.info("ルール定義を読み込みました: {} (ルール数: {})", 
            definition.metadata().name(), 
            definition.rules().size());
        
        return definition;
    }
    
    /**
     * リソースファイルから読み込み
     */
    public RuleDefinition parseResource(String resourcePath) throws Exception {
        LOG.info("リソースからルール定義を読み込み: {}", resourcePath);
        
        InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath);
        if (is == null) {
            throw new IllegalArgumentException("リソースが見つかりません: " + resourcePath);
        }
        
        return parseStream(is);
    }
    
    /**
     * RuleSpecからRuleオブジェクトに変換
     * 
     * @param spec ルール仕様
     * @param namespace ネームスペース（nullの場合は"default"）
     */
    public Rule convertToRule(RuleDefinition.RuleSpec spec, String namespace) {
        // operation文字列をenumに変換
        Rule.Operation operation = Rule.Operation.valueOf(spec.operation());
        
        // ネームスペースがnullまたは空の場合は"default"を使用
        String effectiveNamespace = (namespace != null && !namespace.isBlank()) ? namespace : "default";
        
        return Rule.builder()
            .namespace(effectiveNamespace)
            .inputs(spec.inputs().toArray(new String[0]))
            .output(spec.output())
            .operation(operation)
            .build();
    }
    
    /**
     * 全てのルール仕様をRuleオブジェクトに変換
     */
    public List<Rule> convertAllRules(RuleDefinition definition) {
        LOG.info("ルール変換開始: 定義数={}", definition.rules().size());
        
        // メタデータからネームスペースを取得
        String namespace = definition.metadata() != null ? definition.metadata().namespace() : null;
        LOG.info("ネームスペース: {}", namespace != null ? namespace : "default");
        
        List<Rule> rules = definition.rules().stream()
            .filter(spec -> {
                // enabledがnullの場合はtrueとして扱う
                boolean enabled = spec.enabled() == null || spec.enabled();
                LOG.debug("ルール '{}': enabled={}", spec.name(), enabled);
                return enabled;
            })
            .sorted((a, b) -> {
                int priorityA = a.priority() != null ? a.priority() : Integer.MAX_VALUE;
                int priorityB = b.priority() != null ? b.priority() : Integer.MAX_VALUE;
                return Integer.compare(priorityA, priorityB);
            })
            .map(spec -> {
                LOG.debug("ルール '{}' を変換: {} -> {} (namespace: {})", 
                    spec.name(), spec.inputs(), spec.output(), namespace);
                return this.convertToRule(spec, namespace);
            })
            .collect(Collectors.toList());
        
        LOG.info("ルール変換完了: {}個のルールを生成", rules.size());
        return rules;
    }
    
    /**
     * ルール定義の検証
     */
    public ValidationResult validate(RuleDefinition definition) {
        ValidationResult result = new ValidationResult(true, null);
        
        // メタデータの検証
        if (definition.metadata() == null) {
            return new ValidationResult(false, "メタデータが必要です");
        }
        
        // ルールの検証
        for (RuleDefinition.RuleSpec rule : definition.rules()) {
            if (rule.inputs() == null || rule.inputs().isEmpty()) {
                return new ValidationResult(false, 
                    "ルール '" + rule.name() + "' に入力が必要です");
            }
            
            if (rule.output() == null || rule.output().isEmpty()) {
                return new ValidationResult(false, 
                    "ルール '" + rule.name() + "' に出力が必要です");
            }
            
            // 演算タイプの検証
            try {
                Rule.Operation.valueOf(rule.operation());
            } catch (IllegalArgumentException e) {
                return new ValidationResult(false, 
                    "ルール '" + rule.name() + "' の演算タイプが不正です: " + rule.operation());
            }
        }
        
        LOG.info("ルール定義の検証が成功しました");
        return result;
    }
    
    /**
     * 検証結果
     */
    public record ValidationResult(boolean isValid, String errorMessage) {}
}

