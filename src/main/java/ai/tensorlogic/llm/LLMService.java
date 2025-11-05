package ai.tensorlogic.llm;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;

/**
 * LLM API統合サービス
 * 
 * OpenAI APIやその他のLLMとの通信を管理
 */
@ApplicationScoped
public class LLMService {
    
    private static final Logger LOG = LoggerFactory.getLogger(LLMService.class);
    
    @ConfigProperty(name = "llm.openai.api-key", defaultValue = "demo-mode")
    String apiKey;
    
    @ConfigProperty(name = "llm.openai.model", defaultValue = "gpt-4")
    String model;
    
    @ConfigProperty(name = "llm.openai.timeout", defaultValue = "30s")
    Duration timeout;
    
    private OpenAiService openAiService;
    
    /**
     * LLMに質問し、Chain-of-Thought推論を取得
     */
    public LLMResponse queryWithReasoning(String query) {
        LOG.info("========================================");
        LOG.info("LLMに質問: {}", query);
        LOG.info("========================================");
        
        // ⭐ APIキーの状態を確認（常に表示）
        LOG.warn("========================================");
        LOG.warn("📋 APIキーの状態を確認:");
        if (apiKey == null) {
            LOG.warn("  ❌ APIキー: NULL");
        } else if (apiKey.isBlank()) {
            LOG.warn("  ❌ APIキー: 空文字");
        } else if (apiKey.length() < 20) {
            LOG.warn("  ❌ APIキー: '{}' (短すぎる)", apiKey);
        } else {
            LOG.warn("  📝 APIキー: {}... (長さ: {}文字)", 
                apiKey.substring(0, Math.min(15, apiKey.length())), apiKey.length());
        }
        LOG.warn("========================================");
        
        // ✅ 有効なAPIキーかどうかを判定（ロジックを逆転）
        boolean isValidApiKey = apiKey != null && 
                               !apiKey.isBlank() &&
                               apiKey.length() >= 20 &&  // OpenAI APIキーは通常40文字以上
                               apiKey.startsWith("sk-") &&
                               !apiKey.equals("demo-mode") &&
                               !apiKey.contains("your-actual") &&
                               !apiKey.contains("your-api-key") &&
                               !apiKey.startsWith("sk-your-");
        
        // デモモード判定（有効なAPIキーでない場合）
        boolean isDemoMode = !isValidApiKey;
        
        LOG.warn("========================================");
        LOG.warn("🔍 有効性チェック結果:");
        if (apiKey == null) {
            LOG.warn("  ❌ NULL");
        } else {
            LOG.warn("  - 長さチェック (>= 20): {}", apiKey.length() >= 20 ? "✅ PASS" : "❌ FAIL");
            LOG.warn("  - 'sk-' で開始: {}", apiKey.startsWith("sk-") ? "✅ PASS" : "❌ FAIL");
            LOG.warn("  - 'demo-mode' でない: {}", !apiKey.equals("demo-mode") ? "✅ PASS" : "❌ FAIL");
            LOG.warn("  - プレースホルダーでない: {}", 
                !apiKey.contains("your-actual") && !apiKey.contains("your-api-key") ? "✅ PASS" : "❌ FAIL");
        }
        LOG.warn("  → 判定結果: {}", isValidApiKey ? "✅ 有効なAPIキー" : "❌ 無効");
        LOG.warn("========================================");
        
        if (isDemoMode) {
            LOG.error("🔴 動作モード: デモモード");
            LOG.error("⚠️  理由: APIキーが無効または未設定です");
            LOG.error("💡 修正方法:");
            LOG.error("   1. application.yaml を開く");
            LOG.error("   2. api-key: を実際のOpenAI APIキーに変更");
            LOG.error("   3. 形式: sk-proj-... または sk-...");
            LOG.error("   4. ファイルを保存（Quarkusが自動リロード）");
        } else {
            LOG.info("✅ 動作モード: OpenAI API実行モード");
            LOG.info("🔑 APIキー: 有効");
        }
        LOG.warn("========================================");
        
        if (!isDemoMode && openAiService == null) {
            LOG.info("🔧 OpenAiServiceを初期化します...");
            openAiService = new OpenAiService(apiKey, timeout);
            LOG.info("✅ OpenAiService初期化完了");
        }
        
        // デモモード（APIキーが設定されていない場合）
        if (isDemoMode) {
            LOG.warn("🎭 デモモードで固定レスポンスを返します");
            return simulateLLMResponse(query);
        }
        
        try {
            LOG.info("🚀 OpenAI APIを呼び出します...");
            LOG.info("   モデル: {}", model);
            
            var request = ChatCompletionRequest.builder()
                .model(model)
                .messages(List.of(
                    new ChatMessage("system", "段階的に推論し、確信度を示してください。"),
                    new ChatMessage("user", query)
                ))
                .build();
            
            var response = openAiService.createChatCompletion(request);
            String content = response.getChoices().get(0).getMessage().getContent();
            
            LOG.info("✅ OpenAI APIから応答を受信しました");
            LOG.info("   応答長: {} 文字", content.length());
            
            return parseLLMResponse(content);
            
        } catch (Exception e) {
            LOG.error("❌ LLM API呼び出しエラーが発生しました", e);
            LOG.error("   エラー詳細: {}", e.getMessage());
            LOG.warn("🎭 フォールバック: デモモードで応答します");
            return simulateLLMResponse(query);
        }
    }
    
    /**
     * LLM応答をパース
     */
    private LLMResponse parseLLMResponse(String content) {
        // 確信度を抽出（簡易実装）
        double confidence = 0.8;
        if (content.contains("確実") || content.contains("間違いなく")) {
            confidence = 0.95;
        } else if (content.contains("おそらく") || content.contains("可能性が高い")) {
            confidence = 0.7;
        }
        
        return new LLMResponse(content, confidence, extractReasoningSteps(content));
    }
    
    /**
     * 推論ステップを抽出
     */
    private List<String> extractReasoningSteps(String content) {
        // 番号付きリストを検出
        return content.lines()
            .filter(line -> line.matches("^\\d+\\..*"))
            .toList();
    }
    
    /**
     * LLM応答をシミュレート（デモ用）
     */
    private LLMResponse simulateLLMResponse(String query) {
        LOG.warn("========================================");
        LOG.warn("🎭 デモモード: LLM応答をシミュレート");
        LOG.warn("⚠️  注意: 質問の内容は無視され、固定レスポンスを返します");
        LOG.warn("========================================");
        
        if (query.contains("ソクラテス")) {
            LOG.info("📝 パターンマッチ: 'ソクラテス' を検出");
            LOG.info("   → ソクラテスの三段論法の固定レスポンスを返します");
            return new LLMResponse(
                "はい、ソクラテスは死にます。",
                0.90,
                List.of(
                    "1. ソクラテスは人間です。",
                    "2. すべての人間は死にます。",
                    "3. したがって、ソクラテスは死にます。"
                )
            );
        }
        
        LOG.info("📝 パターンマッチ: なし");
        LOG.info("   → デフォルトレスポンスを返します");
        return new LLMResponse(
            "情報が不足しています。",
            0.3,
            List.of("1. 十分な情報がありません。")
        );
    }
}

