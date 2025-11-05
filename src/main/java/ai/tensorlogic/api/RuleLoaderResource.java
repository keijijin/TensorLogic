package ai.tensorlogic.api;

import ai.tensorlogic.core.TensorLogicEngine;
import ai.tensorlogic.parser.RuleLoader;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.util.Map;

/**
 * ルールローダー REST API
 */
@Path("/api/rules")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Rule Loader", description = "外部ファイルからルールを読み込み")
public class RuleLoaderResource {
    
    @Inject
    RuleLoader ruleLoader;
    
    @Inject
    TensorLogicEngine engine;
    
    /**
     * サンプルルールを読み込み
     */
    @POST
    @Path("/load-example")
    @Operation(summary = "サンプルルールを読み込み",
               description = "三段論法のサンプルルールを読み込んで推論を実行")
    public RuleExecutionResult loadExample() {
        // サンプルルールを読み込み
        RuleLoader.LoadResult loadResult = ruleLoader.loadFromResource(
            "rules/example-rules.yaml"
        );
        
        // 推論実行
        Map<String, INDArray> results = engine.forwardChain();
        
        // 結果取得
        INDArray conclusion = engine.getFact("socrates_is_mortal");
        
        return new RuleExecutionResult(
            loadResult.success(),
            loadResult.summary(),
            conclusion != null ? conclusion.getDouble(0) : null,
            results.size()
        );
    }
    
    /**
     * 知識グラフルールを読み込み
     */
    @POST
    @Path("/load-knowledge-graph")
    @Operation(summary = "知識グラフルールを読み込み")
    public RuleExecutionResult loadKnowledgeGraph() {
        RuleLoader.LoadResult loadResult = ruleLoader.loadFromResource(
            "rules/knowledge-graph-rules.yaml"
        );
        
        Map<String, INDArray> results = engine.forwardChain();
        
        return new RuleExecutionResult(
            loadResult.success(),
            loadResult.summary(),
            null,
            results.size()
        );
    }
    
    /**
     * カスタムルールファイルを読み込み
     */
    @POST
    @Path("/load-file")
    @Operation(summary = "カスタムルールファイルを読み込み")
    public RuleExecutionResult loadFile(FilePathRequest request) {
        RuleLoader.LoadResult loadResult = ruleLoader.loadFromFile(
            request.filePath()
        );
        
        Map<String, INDArray> results = engine.forwardChain();
        
        return new RuleExecutionResult(
            loadResult.success(),
            loadResult.summary(),
            null,
            results.size()
        );
    }
}

record FilePathRequest(String filePath) {}

record RuleExecutionResult(
    boolean success,
    String message,
    Double conclusionValue,
    int resultCount
) {}

