package com.duola.grpc_java.dto2.base;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * AI推理总返回类
 * 对应ai.proto中的FrameResult结构
 */
@Getter
@Setter
public class AIInferenceResponse {
    
    @JsonProperty("success")
    private Boolean success;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("frame_index")
    private Long frameIndex;
    
    @JsonProperty("timestamp")
    private Long timestamp;
    
    @JsonProperty("runtime_ms")
    private Double runtimeMs;
    
    @JsonProperty("session_id")
    private String sessionId;
    
    @JsonProperty("model_info")
    private ModelInfo modelInfo;
    
    @JsonProperty("results")
    private List<InferenceResult> results;
    
    @JsonProperty("meta")
    private Map<String, Object> meta;
    
    // 构造函数
    public AIInferenceResponse() {}
    
    public AIInferenceResponse(Boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    
    public AIInferenceResponse(Boolean success, String message, List<InferenceResult> results) {
        this.success = success;
        this.message = message;
        this.results = results;
    }
    
    // 便捷方法
    public void addResult(InferenceResult result) {
        if (this.results == null) {
            this.results = new java.util.ArrayList<>();
        }
        this.results.add(result);
    }
    
    public void addMeta(String key, Object value) {
        if (this.meta == null) {
            this.meta = new java.util.HashMap<>();
        }
        this.meta.put(key, value);
    }
    
    public Boolean hasResults() {
        return results != null && !results.isEmpty();
    }
    
    public Integer getResultCount() {
        return results != null ? results.size() : 0;
    }
    
    /**
     * 模型信息内部类
     */
    @Getter
    @Setter
    public static class ModelInfo {
        @JsonProperty("name")
        private String name;
        
        @JsonProperty("version")
        private String version;
        
        @JsonProperty("type")
        private String type;
        
        @JsonProperty("tags")
        private Map<String, String> tags;
        
        public ModelInfo() {}
        
        public ModelInfo(String name, String version, String type) {
            this.name = name;
            this.version = version;
            this.type = type;
        }
    }
}
