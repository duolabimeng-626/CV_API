package com.duola.grpc_java.dto2.base;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * 推理结果基础接口
 * 对应ai.proto中的ResultEnvelope结构
 */
@Getter
@Setter
public abstract class InferenceResult {
    
    @JsonProperty("kind")
    private String kind;
    
    @JsonProperty("content_type")
    private String contentType;
    
    @JsonProperty("input_index")
    private Integer inputIndex;
    
    @JsonProperty("meta")
    private Object meta;
    
    // 抽象方法，子类必须实现
    public abstract String getResultType();
    
    // 构造函数
    public InferenceResult() {}
    
    public InferenceResult(String kind, String contentType) {
        this.kind = kind;
        this.contentType = contentType;
    }
}
