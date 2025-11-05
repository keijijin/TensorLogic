package ai.tensorlogic.camel;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.ProducerTemplate;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * Camel統合エンドポイント
 * 
 * Camel Routesを経由したLLM処理
 */
@Path("/api/camel")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Camel Integration", description = "Apache Camel経由のLLM統合")
public class CamelIntegrationResource {
    
    @Inject
    ProducerTemplate producerTemplate;
    
    /**
     * Camel経由でLLM推論を検証
     */
    @POST
    @Path("/verify")
    @Operation(summary = "Camel経由の検証")
    public String verifyViaCamel(String query) {
        return producerTemplate.requestBody("direct:verify-llm-reasoning", query, String.class);
    }
    
    /**
     * バッチ検証
     */
    @POST
    @Path("/batch-verify")
    @Operation(summary = "バッチ検証")
    public String batchVerify(String[] queries) {
        return producerTemplate.requestBody("direct:batch-verify", queries, String.class);
    }
    
    /**
     * 非同期検証
     */
    @POST
    @Path("/async-verify")
    @Operation(summary = "非同期検証")
    public String asyncVerify(String query) {
        producerTemplate.sendBody("direct:async-verify", query);
        return "{ \"status\": \"queued\", \"message\": \"検証リクエストをキューに追加しました\" }";
    }
    
    /**
     * 汎用LLM検証（外部ルールファイルを使用）⭐ NEW
     */
    @POST
    @Path("/generic-verify")
    @Operation(
        summary = "汎用LLM検証",
        description = "外部ルールファイルを使用してLLMの回答を検証します。どのようなルールでも適用可能。"
    )
    public String genericVerify(GenericVerifyRequest request) {
        return producerTemplate.requestBody("direct:generic-verify", request, String.class);
    }
    
    /**
     * バッチ汎用検証
     */
    @POST
    @Path("/batch-generic-verify")
    @Operation(summary = "バッチ汎用検証")
    public String batchGenericVerify(GenericVerifyRequest[] requests) {
        return producerTemplate.requestBody("direct:batch-generic-verify", requests, String.class);
    }
}

/**
 * 汎用検証リクエスト
 */
record GenericVerifyRequest(
    String query,
    String ruleFile,
    java.util.Map<String, java.util.List<Double>> customFacts,
    java.util.Map<String, Double> expectedOutputs,
    Double tolerance,
    Boolean extractFactsFromLLM
) {}


