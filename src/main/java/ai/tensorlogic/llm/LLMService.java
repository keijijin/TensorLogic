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
        LOG.info("LLMに質問: {}", query);
        
        // デモモード判定
        boolean isDemoMode = apiKey == null || 
                            apiKey.equals("demo-mode") || 
                            apiKey.equals("your-api-key-here") ||
                            apiKey.isBlank();
        
        if (!isDemoMode && openAiService == null) {
            openAiService = new OpenAiService(apiKey, timeout);
        }
        
        // デモモード（APIキーが設定されていない場合）
        if (isDemoMode) {
            LOG.info("デモモードで動作します（OpenAI APIキーが未設定）");
            return simulateLLMResponse(query);
        }
        
        try {
            var request = ChatCompletionRequest.builder()
                .model(model)
                .messages(List.of(
                    new ChatMessage("system", "段階的に推論し、確信度を示してください。"),
                    new ChatMessage("user", query)
                ))
                .build();
            
            var response = openAiService.createChatCompletion(request);
            String content = response.getChoices().get(0).getMessage().getContent();
            
            return parseLLMResponse(content);
            
        } catch (Exception e) {
            LOG.error("LLM API呼び出しエラー", e);
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
        LOG.info("デモモード: LLM応答をシミュレート");
        
        if (query.contains("ソクラテス")) {
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
        
        return new LLMResponse(
            "情報が不足しています。",
            0.3,
            List.of("1. 十分な情報がありません。")
        );
    }
}

