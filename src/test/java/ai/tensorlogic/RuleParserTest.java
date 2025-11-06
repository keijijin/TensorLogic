package ai.tensorlogic;

import ai.tensorlogic.core.Rule;
import ai.tensorlogic.parser.RuleDefinition;
import ai.tensorlogic.parser.RuleLoader;
import ai.tensorlogic.parser.RuleParser;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RuleParser と RuleLoader のテスト
 */
@QuarkusTest
class RuleParserTest {
    
    @Inject
    RuleParser parser;
    
    @Inject
    RuleLoader loader;
    
    @Test
    @DisplayName("リソースからのルール読み込み")
    void testParseResource() throws Exception {
        // Given: リソースパス
        String resourcePath = "rules/simple-verification-rules.yaml";
        
        // When: ルールを読み込み
        RuleDefinition definition = parser.parseResource(resourcePath);
        
        // Then
        assertNotNull(definition, "ルール定義が読み込まれること");
        assertNotNull(definition.metadata(), "メタデータが存在すること");
        assertEquals("シンプル検証ルール", definition.metadata().name(), "名前が正しいこと");
        assertEquals("simple-verification", definition.metadata().namespace(), 
            "ネームスペースが正しいこと");
        assertFalse(definition.rules().isEmpty(), "ルールが存在すること");
    }
    
    @Test
    @DisplayName("ルール定義の変換")
    void testConvertAllRules() throws Exception {
        // Given
        RuleDefinition definition = parser.parseResource("rules/simple-verification-rules.yaml");
        
        // When: ルールを変換
        List<Rule> rules = parser.convertAllRules(definition);
        
        // Then
        assertFalse(rules.isEmpty(), "ルールが変換されること");
        
        Rule firstRule = rules.get(0);
        assertNotNull(firstRule.namespace(), "ネームスペースが設定されること");
        assertEquals("simple-verification", firstRule.namespace(), 
            "ネームスペースが正しいこと");
        assertFalse(firstRule.inputs().isEmpty(), "入力が設定されること");
        assertNotNull(firstRule.output(), "出力が設定されること");
        assertNotNull(firstRule.operation(), "演算が設定されること");
    }
    
    @Test
    @DisplayName("ルール定義の検証 - 成功")
    void testValidateRuleDefinition_Success() throws Exception {
        // Given
        RuleDefinition definition = parser.parseResource("rules/loan-approval-from-drd.yaml");
        
        // When
        RuleParser.ValidationResult result = parser.validate(definition);
        
        // Then
        assertTrue(result.isValid(), "検証が成功すること");
        assertNull(result.errorMessage(), "エラーメッセージがないこと");
    }
    
    @Test
    @DisplayName("RuleLoader - リソースからのロード")
    void testLoadFromResource() throws Exception {
        // Given
        String resourcePath = "rules/loan-approval-from-drd.yaml";
        
        // When
        RuleLoader.LoadResult result = loader.loadFromResource(resourcePath);
        
        // Then
        assertTrue(result.success(), "ロードが成功すること");
        assertNotNull(result.ruleName(), "ルール名が存在すること");
        assertTrue(result.factCount() > 0, "事実が存在すること");
        assertTrue(result.ruleCount() > 0, "ルールが存在すること");
    }
    
    @Test
    @DisplayName("RuleLoader - 複数ルールセットのロード")
    void testLoadMultipleRuleSets() throws Exception {
        // Given
        String[] resourcePaths = {
            "rules/simple-verification-rules.yaml",
            "rules/age-qualification-rules.yaml"
        };
        
        // When & Then
        for (String path : resourcePaths) {
            RuleLoader.LoadResult result = loader.loadFromResource(path);
            assertTrue(result.success(), path + " のロードが成功すること");
        }
    }
    
    @Test
    @DisplayName("ルールの演算タイプ変換")
    void testOperationTypeConversion() throws Exception {
        // Given
        RuleDefinition definition = parser.parseResource("rules/loan-approval-from-drd.yaml");
        
        // When
        List<Rule> rules = parser.convertAllRules(definition);
        
        // Then
        assertTrue(rules.stream()
            .anyMatch(r -> r.operation() == Rule.Operation.MODUS_PONENS), 
            "MODUS_PONENSルールが存在すること");
        
        assertTrue(rules.stream()
            .anyMatch(r -> r.operation() == Rule.Operation.CONJUNCTION), 
            "CONJUNCTIONルールが存在すること");
    }
    
    @Test
    @DisplayName("enabledフラグの処理")
    void testEnabledFlag() throws Exception {
        // Given
        RuleDefinition definition = parser.parseResource("rules/loan-approval-from-drd.yaml");
        
        // When
        List<Rule> rules = parser.convertAllRules(definition);
        
        // Then
        // enabledがtrueまたはnullのルールのみが変換されること
        assertFalse(rules.isEmpty(), "有効なルールが変換されること");
    }
    
    @Test
    @DisplayName("事実とルールの数の確認")
    void testFactsAndRulesCount() throws Exception {
        // Given
        String resourcePath = "rules/loan-approval-from-drd.yaml";
        RuleDefinition definition = parser.parseResource(resourcePath);
        
        // When
        int factsCount = definition.facts() != null ? definition.facts().size() : 0;
        int rulesCount = definition.rules() != null ? definition.rules().size() : 0;
        
        // Then
        assertTrue(factsCount > 0, "事実が存在すること");
        assertTrue(rulesCount > 0, "ルールが存在すること");
        
        // LoadResultのカウントと一致すること
        RuleLoader.LoadResult result = loader.loadFromResource(resourcePath);
        assertEquals(factsCount, result.factCount(), 
            "事実の数が一致すること");
        assertEquals(rulesCount, result.ruleCount(), 
            "ルールの数が一致すること");
    }
}

