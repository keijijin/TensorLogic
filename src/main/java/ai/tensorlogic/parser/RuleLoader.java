package ai.tensorlogic.parser;

import ai.tensorlogic.core.Rule;
import ai.tensorlogic.core.TensorLogicEngine;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * ルールローダー
 * 
 * ルール定義ファイルを読み込み、TensorLogicEngineに登録します。
 */
@ApplicationScoped
public class RuleLoader {
    
    private static final Logger LOG = LoggerFactory.getLogger(RuleLoader.class);
    
    @Inject
    RuleParser parser;
    
    @Inject
    TensorConverter converter;
    
    @Inject
    TensorLogicEngine engine;
    
    /**
     * ルールファイルを読み込んでエンジンに登録
     */
    public LoadResult loadFromFile(String filePath) {
        try {
            LOG.info("ルールファイルを読み込み開始: {}", filePath);
            
            // 1. ルール定義を読み込み
            RuleDefinition definition = parser.parseFile(filePath);
            
            // 2. 検証
            RuleParser.ValidationResult validation = parser.validate(definition);
            if (!validation.isValid()) {
                throw new RuntimeException("検証エラー: " + validation.errorMessage());
            }
            
            // 3. ルールとテンソルを登録
            return load(definition);
            
        } catch (Exception e) {
            LOG.error("ルールファイルの読み込みに失敗: {}", e.getMessage(), e);
            throw new RuntimeException("ルール読み込みエラー", e);
        }
    }
    
    /**
     * リソースファイルから読み込み
     */
    public LoadResult loadFromResource(String resourcePath) {
        try {
            LOG.info("リソースからルールを読み込み開始: {}", resourcePath);
            
            // 1. ルール定義を読み込み
            RuleDefinition definition = parser.parseResource(resourcePath);
            
            // 2. 検証
            RuleParser.ValidationResult validation = parser.validate(definition);
            if (!validation.isValid()) {
                throw new RuntimeException("検証エラー: " + validation.errorMessage());
            }
            
            // 3. ルールとテンソルを登録
            return load(definition);
            
        } catch (Exception e) {
            LOG.error("リソースの読み込みに失敗: {}", e.getMessage(), e);
            throw new RuntimeException("ルール読み込みエラー", e);
        }
    }
    
    /**
     * ルール定義をエンジンに登録
     */
    private LoadResult load(RuleDefinition definition) {
        LOG.info("ルールを登録中: {}", definition.metadata().name());
        
        int factCount = 0;
        int ruleCount = 0;
        
        // 1. 事実（テンソル）を登録
        Map<String, INDArray> tensors = converter.convertAllFacts(definition);
        for (Map.Entry<String, INDArray> entry : tensors.entrySet()) {
            engine.addFact(entry.getKey(), entry.getValue());
            factCount++;
            
            LOG.debug("事実を登録: {} {}", 
                entry.getKey(), 
                converter.tensorInfo(entry.getValue()));
        }
        
        // 2. ルールを登録
        List<Rule> rules = parser.convertAllRules(definition);
        for (int i = 0; i < rules.size(); i++) {
            Rule rule = rules.get(i);
            RuleDefinition.RuleSpec spec = definition.rules().get(i);
            
            engine.addRule(spec.name(), rule);
            ruleCount++;
            
            LOG.debug("ルールを登録: {} ({} -> {})", 
                spec.name(), 
                spec.inputs(), 
                spec.output());
        }
        
        LOG.info("ルール登録完了: 事実{}個, ルール{}個", factCount, ruleCount);
        
        return new LoadResult(
            true,
            definition.metadata().name(),
            factCount,
            ruleCount,
            null
        );
    }
    
    /**
     * 読み込み結果
     */
    public record LoadResult(
        boolean success,
        String ruleName,
        int factCount,
        int ruleCount,
        String errorMessage
    ) {
        public String summary() {
            if (success) {
                return String.format("✓ '%s' を読み込みました (事実: %d, ルール: %d)", 
                    ruleName, factCount, ruleCount);
            } else {
                return String.format("✗ 読み込み失敗: %s", errorMessage);
            }
        }
    }
}

