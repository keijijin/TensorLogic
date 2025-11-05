package ai.tensorlogic.api;

import ai.tensorlogic.core.Rule;
import ai.tensorlogic.core.TensorLogicEngine;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ルールインスペクター REST API
 * 
 * 読み込まれたルールと事実の内容を確認するためのAPI
 */
@Path("/api/rules/inspect")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Rule Inspector", description = "読み込まれたルールの確認")
public class RuleInspectorResource {
    
    @Inject
    TensorLogicEngine engine;
    
    /**
     * 全てのルールを一覧表示
     */
    @GET
    @Path("/rules")
    @Operation(summary = "全ルールを表示", 
               description = "登録されている全てのルールの一覧を取得")
    public RuleListResponse listAllRules() {
        try {
            Map<String, Rule> rules = engine.getAllRules();
            
            List<RuleInfo> ruleInfos = rules.entrySet().stream()
                .map(entry -> new RuleInfo(
                    entry.getKey(),
                    entry.getValue().inputs(),
                    entry.getValue().output(),
                    entry.getValue().operation().name(),
                    generateNotation(entry.getValue())
                ))
                .collect(Collectors.toList());
            
            return new RuleListResponse(
                true,
                ruleInfos.size(),
                ruleInfos
            );
            
        } catch (Exception e) {
            return new RuleListResponse(
                false,
                0,
                List.of()
            );
        }
    }
    
    /**
     * 全ての事実（ファクト）を一覧表示
     */
    @GET
    @Path("/facts")
    @Operation(summary = "全事実を表示",
               description = "登録されている全ての事実とテンソルの情報を取得")
    public FactListResponse listAllFacts() {
        try {
            Map<String, INDArray> facts = engine.getAllFacts();
            
            List<FactInfo> factInfos = facts.entrySet().stream()
                .map(entry -> new FactInfo(
                    entry.getKey(),
                    Arrays.toString(entry.getValue().shape()),
                    entry.getValue().dataType().toString(),
                    formatTensor(entry.getValue()),
                    getTensorStats(entry.getValue())
                ))
                .collect(Collectors.toList());
            
            return new FactListResponse(
                true,
                factInfos.size(),
                factInfos
            );
            
        } catch (Exception e) {
            return new FactListResponse(
                false,
                0,
                List.of()
            );
        }
    }
    
    /**
     * 特定のルールの詳細を表示
     */
    @GET
    @Path("/rules/{ruleName}")
    @Operation(summary = "ルールの詳細",
               description = "特定のルールの詳細情報を取得")
    public RuleDetailResponse getRuleDetail(@PathParam("ruleName") String ruleName) {
        try {
            Map<String, Rule> rules = engine.getAllRules();
            Rule rule = rules.get(ruleName);
            
            if (rule == null) {
                return new RuleDetailResponse(
                    false,
                    "ルールが見つかりません: " + ruleName,
                    null
                );
            }
            
            RuleDetail detail = new RuleDetail(
                ruleName,
                rule.inputs(),
                rule.output(),
                rule.operation().name(),
                generateNotation(rule),
                getInputTensorInfo(rule)
            );
            
            return new RuleDetailResponse(true, "成功", detail);
            
        } catch (Exception e) {
            return new RuleDetailResponse(
                false,
                "エラー: " + e.getMessage(),
                null
            );
        }
    }
    
    /**
     * 特定の事実（テンソル）の内容を表示
     */
    @GET
    @Path("/facts/{factName}")
    @Operation(summary = "事実の詳細",
               description = "特定の事実のテンソル内容を取得")
    public FactDetailResponse getFactDetail(@PathParam("factName") String factName) {
        INDArray tensor = engine.getFact(factName);
        
        if (tensor == null) {
            return new FactDetailResponse(
                false,
                "事実が見つかりません: " + factName,
                null
            );
        }
        
        FactDetail detail = new FactDetail(
            factName,
            Arrays.toString(tensor.shape()),
            tensor.dataType().toString(),
            formatTensorFull(tensor),
            getTensorStats(tensor),
            tensor.length()
        );
        
        return new FactDetailResponse(true, "成功", detail);
    }
    
    /**
     * システムの状態を表示
     */
    @GET
    @Path("/status")
    @Operation(summary = "システム状態",
               description = "登録されているルールと事実の統計情報")
    public SystemStatus getSystemStatus() {
        try {
            Map<String, Rule> rules = engine.getAllRules();
            Map<String, INDArray> facts = engine.getAllFacts();
            
            Map<String, Integer> operationCounts = rules.values().stream()
                .collect(Collectors.groupingBy(
                    rule -> rule.operation().name(),
                    Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));
            
            long totalTensorElements = facts.values().stream()
                .mapToLong(INDArray::length)
                .sum();
            
            return new SystemStatus(
                rules.size(),
                facts.size(),
                operationCounts,
                totalTensorElements,
                "稼働中"
            );
            
        } catch (Exception e) {
            return new SystemStatus(
                0, 0, Map.of(), 0, "エラー: " + e.getMessage()
            );
        }
    }
    
    // ===== ヘルパーメソッド =====
    
    /**
     * ルールの論理記法を生成
     */
    private String generateNotation(Rule rule) {
        String operation = rule.operation().name();
        String inputs = String.join(", ", rule.inputs());
        
        return switch (rule.operation()) {
            case MODUS_PONENS -> String.format("%s ∧ %s ⟹ %s", 
                rule.inputs().get(0), rule.inputs().get(1), rule.output());
            case CONJUNCTION -> String.format("%s ∧ %s ⟹ %s", 
                inputs, inputs, rule.output());
            case CHAIN -> String.format("%s ○ %s ⟹ %s", 
                rule.inputs().get(0), rule.inputs().get(1), rule.output());
            default -> String.format("%s(%s) ⟹ %s", operation, inputs, rule.output());
        };
    }
    
    /**
     * テンソルをフォーマット（簡略版）
     */
    private String formatTensor(INDArray tensor) {
        if (tensor.length() <= 10) {
            return tensor.toString();
        } else {
            return String.format("[%d elements: %.3f...%.3f]", 
                tensor.length(),
                tensor.getDouble(0),
                tensor.getDouble(tensor.length() - 1));
        }
    }
    
    /**
     * テンソルをフォーマット（完全版）
     */
    private String formatTensorFull(INDArray tensor) {
        return tensor.toString();
    }
    
    /**
     * テンソルの統計情報
     */
    private TensorStats getTensorStats(INDArray tensor) {
        return new TensorStats(
            tensor.minNumber().doubleValue(),
            tensor.maxNumber().doubleValue(),
            tensor.meanNumber().doubleValue(),
            tensor.stdNumber().doubleValue()
        );
    }
    
    /**
     * 入力テンソルの情報を取得
     */
    private List<InputTensorInfo> getInputTensorInfo(Rule rule) {
        return rule.inputs().stream()
            .map(inputName -> {
                INDArray tensor = engine.getFact(inputName);
                if (tensor != null) {
                    return new InputTensorInfo(
                        inputName,
                        Arrays.toString(tensor.shape()),
                        formatTensor(tensor)
                    );
                } else {
                    return new InputTensorInfo(inputName, "不明", "見つかりません");
                }
            })
            .collect(Collectors.toList());
    }
}

// ===== レスポンスDTO =====

record RuleListResponse(
    boolean success,
    int count,
    List<RuleInfo> rules
) {}

record RuleInfo(
    String name,
    List<String> inputs,
    String output,
    String operation,
    String notation
) {}

record FactListResponse(
    boolean success,
    int count,
    List<FactInfo> facts
) {}

record FactInfo(
    String name,
    String shape,
    String dtype,
    String preview,
    TensorStats stats
) {}

record RuleDetailResponse(
    boolean success,
    String message,
    RuleDetail rule
) {}

record RuleDetail(
    String name,
    List<String> inputs,
    String output,
    String operation,
    String notation,
    List<InputTensorInfo> inputTensors
) {}

record InputTensorInfo(
    String name,
    String shape,
    String preview
) {}

record FactDetailResponse(
    boolean success,
    String message,
    FactDetail fact
) {}

record FactDetail(
    String name,
    String shape,
    String dtype,
    String fullContent,
    TensorStats stats,
    long elementCount
) {}

record TensorStats(
    double min,
    double max,
    double mean,
    double std
) {}

record SystemStatus(
    int totalRules,
    int totalFacts,
    Map<String, Integer> operationCounts,
    long totalTensorElements,
    String status
) {}

