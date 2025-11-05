package ai.tensorlogic.api;

import ai.tensorlogic.integration.*;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * Tensor Logic REST API エンドポイント
 */
@Path("/api/tensor-logic")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Tensor Logic", description = "LLM と Tensor Logic 統合 API")
public class TensorLogicResource {
    
    @Inject
    LLMTensorLogicIntegration integration;
    
    /**
     * LLMの推論を検証
     */
    @POST
    @Path("/verify-reasoning")
    @Operation(summary = "LLMの推論を検証", 
               description = "LLMの出力をTensor Logicで論理的に検証します")
    public VerifiedReasoningResult verifyReasoning(ReasoningRequest request) {
        return integration.verifyLLMReasoning(request.query());
    }
    
    /**
     * 矛盾を検出
     */
    @POST
    @Path("/detect-contradiction")
    @Operation(summary = "論理的矛盾を検出",
               description = "複数の主張から論理的矛盾を検出します")
    public ContradictionAnalysis detectContradiction(ContradictionRequest request) {
        return integration.detectContradictions(
            request.claim1(),
            request.claim2(),
            request.claim3()
        );
    }
    
    /**
     * 確信度の伝播を計算
     */
    @POST
    @Path("/propagate-confidence")
    @Operation(summary = "確信度の伝播",
               description = "推論チェーンにおける確信度の伝播を計算します")
    public ConfidencePropagationResult propagateConfidence(
            ConfidencePropagationRequest request) {
        return integration.propagateConfidence(request.confidences());
    }
    
    /**
     * ヘルスチェック
     */
    @GET
    @Path("/health")
    @Operation(summary = "ヘルスチェック")
    public HealthStatus health() {
        return new HealthStatus("OK", "Tensor Logic Engine is running");
    }
}

