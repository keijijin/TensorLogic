package ai.tensorlogic.startup;

import ai.tensorlogic.parser.RuleLoader;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * 起動時ルール自動ロード
 * 
 * アプリケーション起動時に rules/ ディレクトリ内の
 * 全ての .yaml ファイルを自動的にロードします。
 */
@ApplicationScoped
public class RuleAutoLoader {
    
    private static final Logger LOG = LoggerFactory.getLogger(RuleAutoLoader.class);
    
    private static final String RULES_DIRECTORY = "rules";
    private static final String YAML_EXTENSION = ".yaml";
    
    @ConfigProperty(name = "tensor.logic.rules.auto-load.enabled", defaultValue = "true")
    boolean autoLoadEnabled;
    
    @Inject
    RuleLoader ruleLoader;
    
    /**
     * アプリケーション起動時に実行
     */
    void onStart(@Observes StartupEvent ev) {
        if (!autoLoadEnabled) {
            LOG.info("ℹ️  ルールの自動ロードは無効化されています (tensor.logic.rules.auto-load.enabled=false)");
            return;
        }
        
        LOG.info("========================================");
        LOG.info("🚀 ルールの自動ロードを開始します");
        LOG.info("========================================");
        
        try {
            List<String> ruleFiles = findRuleFiles();
            
            if (ruleFiles.isEmpty()) {
                LOG.warn("⚠️  rules/ ディレクトリにルールファイルが見つかりませんでした");
                return;
            }
            
            LOG.info("📁 {}個のルールファイルが見つかりました", ruleFiles.size());
            
            int successCount = 0;
            int failureCount = 0;
            
            for (String ruleFile : ruleFiles) {
                try {
                    LOG.info("📥 ロード中: {}", ruleFile);
                    RuleLoader.LoadResult result = ruleLoader.loadFromResource(ruleFile);
                    
                    if (result.success()) {
                        successCount++;
                        LOG.info("  ✅ {}: {} (事実: {}, ルール: {})", 
                            result.ruleName(), 
                            "成功",
                            result.factCount(), 
                            result.ruleCount());
                    } else {
                        failureCount++;
                        LOG.error("  ❌ {}: 失敗 - {}", 
                            ruleFile, 
                            result.errorMessage());
                    }
                    
                } catch (Exception e) {
                    failureCount++;
                    LOG.error("  ❌ {}: 例外発生 - {}", ruleFile, e.getMessage(), e);
                }
            }
            
            LOG.info("========================================");
            LOG.info("✨ ルールの自動ロード完了");
            LOG.info("  成功: {}", successCount);
            LOG.info("  失敗: {}", failureCount);
            LOG.info("  合計: {}", ruleFiles.size());
            LOG.info("========================================");
            
        } catch (Exception e) {
            LOG.error("❌ ルールファイルの検索中にエラーが発生しました", e);
        }
    }
    
    /**
     * rules/ ディレクトリ内の全ての .yaml ファイルを検索
     * 
     * @return ルールファイルのパスリスト（例: "rules/example.yaml"）
     */
    private List<String> findRuleFiles() throws IOException, URISyntaxException {
        List<String> ruleFiles = new ArrayList<>();
        
        // クラスローダーからリソースを取得
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URI uri = classLoader.getResource(RULES_DIRECTORY).toURI();
        
        Path rulesPath;
        FileSystem fileSystem = null;
        
        try {
            if (uri.getScheme().equals("jar")) {
                // JARファイル内のリソースの場合
                fileSystem = FileSystems.newFileSystem(uri, Collections.emptyMap());
                rulesPath = fileSystem.getPath("/" + RULES_DIRECTORY);
            } else {
                // 通常のファイルシステムの場合
                rulesPath = Paths.get(uri);
            }
            
            // ディレクトリ内の .yaml ファイルを検索
            try (Stream<Path> paths = Files.walk(rulesPath, 1)) {
                paths.filter(Files::isRegularFile)
                     .filter(path -> path.toString().endsWith(YAML_EXTENSION))
                     .forEach(path -> {
                         String fileName = path.getFileName().toString();
                         String resourcePath = RULES_DIRECTORY + "/" + fileName;
                         ruleFiles.add(resourcePath);
                         LOG.debug("発見: {}", resourcePath);
                     });
            }
            
        } finally {
            if (fileSystem != null) {
                fileSystem.close();
            }
        }
        
        // アルファベット順にソート
        Collections.sort(ruleFiles);
        
        return ruleFiles;
    }
}

