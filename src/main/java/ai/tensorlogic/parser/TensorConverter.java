package ai.tensorlogic.parser;

import jakarta.enterprise.context.ApplicationScoped;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * テンソル変換の汎用化クラス
 * 
 * ルール定義ファイルのテンソル仕様を
 * INDArrayに変換します。
 */
@ApplicationScoped
public class TensorConverter {
    
    private static final Logger LOG = LoggerFactory.getLogger(TensorConverter.class);
    
    /**
     * テンソル仕様からINDArrayに変換
     */
    public INDArray convert(RuleDefinition.TensorSpec spec) {
        if (spec == null) {
            throw new IllegalArgumentException("テンソル仕様がnullです");
        }
        
        String type = spec.type().toLowerCase();
        
        return switch (type) {
            case "vector" -> convertVector(spec);
            case "matrix" -> convertMatrix(spec);
            case "tensor" -> convertTensor(spec);
            case "scalar" -> convertScalar(spec);
            default -> throw new IllegalArgumentException("未対応のテンソルタイプ: " + type);
        };
    }
    
    /**
     * ベクトルに変換
     */
    @SuppressWarnings("unchecked")
    private INDArray convertVector(RuleDefinition.TensorSpec spec) {
        LOG.debug("ベクトルに変換: shape={}", spec.shape());
        
        if (spec.values() instanceof List<?> list) {
            double[] array = new double[list.size()];
            for (int i = 0; i < list.size(); i++) {
                array[i] = ((Number) list.get(i)).doubleValue();
            }
            return Nd4j.create(array);
        }
        
        throw new IllegalArgumentException("ベクトルのvaluesはList<Number>である必要があります");
    }
    
    /**
     * 行列に変換
     */
    @SuppressWarnings("unchecked")
    private INDArray convertMatrix(RuleDefinition.TensorSpec spec) {
        LOG.debug("行列に変換: shape={}", spec.shape());
        
        if (spec.values() instanceof List<?> list) {
            int rows = list.size();
            int cols = ((List<?>) list.get(0)).size();
            
            double[][] array = new double[rows][cols];
            for (int i = 0; i < rows; i++) {
                List<? extends Number> row = (List<? extends Number>) list.get(i);
                for (int j = 0; j < cols; j++) {
                    array[i][j] = row.get(j).doubleValue();
                }
            }
            return Nd4j.create(array);
        }
        
        throw new IllegalArgumentException("行列のvaluesはList<List<Number>>である必要があります");
    }
    
    /**
     * 多次元テンソルに変換
     */
    private INDArray convertTensor(RuleDefinition.TensorSpec spec) {
        LOG.debug("テンソルに変換: shape={}", spec.shape());
        
        // 再帰的に変換（今後実装）
        throw new UnsupportedOperationException("3次元以上のテンソルは今後実装予定");
    }
    
    /**
     * スカラーに変換
     */
    private INDArray convertScalar(RuleDefinition.TensorSpec spec) {
        LOG.debug("スカラーに変換");
        
        if (spec.values() instanceof Number number) {
            return Nd4j.create(new double[]{number.doubleValue()});
        }
        
        if (spec.confidence() != null) {
            return Nd4j.create(new double[]{spec.confidence()});
        }
        
        throw new IllegalArgumentException("スカラーのvaluesはNumberである必要があります");
    }
    
    /**
     * 全ての事実をテンソルに変換
     */
    public Map<String, INDArray> convertAllFacts(RuleDefinition definition) {
        LOG.info("事実をテンソルに変換中... (事実数: {})", definition.facts().size());
        
        Map<String, INDArray> tensors = new HashMap<>();
        
        for (RuleDefinition.Fact fact : definition.facts()) {
            try {
                INDArray tensor = convert(fact.tensor());
                tensors.put(fact.name(), tensor);
                
                LOG.debug("事実 '{}' を変換: shape={}, 記法={}", 
                    fact.name(), 
                    tensor.shape(), 
                    fact.notation());
                
            } catch (Exception e) {
                LOG.error("事実 '{}' の変換に失敗: {}", fact.name(), e.getMessage());
                throw new RuntimeException("テンソル変換エラー: " + fact.name(), e);
            }
        }
        
        LOG.info("{}個の事実を変換しました", tensors.size());
        return tensors;
    }
    
    /**
     * テンソル情報の表示
     */
    public String tensorInfo(INDArray tensor) {
        return String.format("shape=%s, dtype=%s, min=%.3f, max=%.3f", 
            java.util.Arrays.toString(tensor.shape()),
            tensor.dataType(),
            tensor.minNumber().doubleValue(),
            tensor.maxNumber().doubleValue()
        );
    }
}

