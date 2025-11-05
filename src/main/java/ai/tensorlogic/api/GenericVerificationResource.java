package ai.tensorlogic.api;

import ai.tensorlogic.integration.GenericLLMVerifier;
import ai.tensorlogic.integration.GenericVerificationRequest;
import ai.tensorlogic.integration.GenericVerificationResult;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.Map;

/**
 * 汎用LLM検証のREST API
 * 
 * Camelを経由せず、直接GenericLLMVerifierを呼び出します。
 */
@Path("/api/verify")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Generic Verification", description = "汎用LLM検証API")
public class GenericVerificationResource {
    
    @Inject
    GenericLLMVerifier verifier;
    
    /**
     * 汎用LLM検証
     */
    @POST
    @Path("/generic")
    @Operation(
        summary = "汎用LLM検証",
        description = "外部ルールファイルを使用してLLMの回答を検証します"
    )
    public GenericVerificationResult verify(VerifyRequest request) {
        GenericVerificationRequest verificationRequest = new GenericVerificationRequest(
            request.query(),
            request.ruleFile(),
            request.customFacts(),
            request.expectedOutputs(),
            request.tolerance(),
            request.extractFactsFromLLM()
        );
        
        return verifier.verify(verificationRequest);
    }
    
    /**
     * シンプルな検証（最小限のパラメータ）
     */
    @POST
    @Path("/simple")
    @Operation(
        summary = "シンプルなLLM検証",
        description = "質問とルールファイルのみを指定して検証"
    )
    public GenericVerificationResult simpleVerify(SimpleVerifyRequest request) {
        GenericVerificationRequest verificationRequest = new GenericVerificationRequest(
            request.query(),
            request.ruleFile(),
            null,  // customFacts
            null,  // expectedOutputs
            null,  // tolerance (デフォルト値を使用)
            false  // extractFactsFromLLM
        );
        
        return verifier.verify(verificationRequest);
    }
}

/**
 * 汎用検証リクエスト
 */
record VerifyRequest(
    String query,
    String ruleFile,
    Map<String, List<Double>> customFacts,
    Map<String, Double> expectedOutputs,
    Double tolerance,
    Boolean extractFactsFromLLM
) {}

/**
 * シンプルな検証リクエスト
 */
record SimpleVerifyRequest(
    String query,
    String ruleFile
) {}

