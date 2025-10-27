package com.duola.grpc_java.dto2.base;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * AI推理配置基础接口
 * 对应ai.proto中的InferenceHeader结构
 */
@Getter
@Setter
public abstract class AIInferenceConfig {
    
    @JsonProperty("model_name")
    private String modelName;
    
    @JsonProperty("model_version")
    private String modelVersion = "latest";
    
    @JsonProperty("model_tags")
    private Map<String, String> modelTags;
    
    @JsonProperty("trace_id")
    private String traceId;
    
    @JsonProperty("span_id")
    private String spanId;
    
    @JsonProperty("parent_span_id")
    private String parentSpanId;
    
    @JsonProperty("tenant_id")
    private String tenantId;
    
    @JsonProperty("user_id")
    private String userId;
    
    @JsonProperty("tenant_attrs")
    private Map<String, String> tenantAttrs;
    
    @JsonProperty("options")
    private Map<String, Object> options;
    
    @JsonProperty("accept_types")
    private String[] acceptTypes;
    
    // 抽象方法，子类必须实现
    public abstract String getInferenceType();
    
    public abstract Map<String, Object> getSpecificOptions();
    
    // 构造函数
    public AIInferenceConfig() {}
    
    public AIInferenceConfig(String modelName) {
        this.modelName = modelName;
    }
}
