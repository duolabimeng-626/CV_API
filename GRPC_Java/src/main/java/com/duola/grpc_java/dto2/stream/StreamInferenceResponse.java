package com.duola.grpc_java.dto2.stream;

import com.duola.grpc_java.dto2.base.InferenceResult;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * 流式推理响应类
 * 对应ai.proto中的StreamResponse结构
 */
@Getter
@Setter
public class StreamInferenceResponse {
    
    @JsonProperty("event_type")
    private String eventType; // ack, frame
    
    @JsonProperty("status")
    private Status status;
    
    @JsonProperty("frame_result")
    private FrameResult frameResult;
    
    // 构造函数
    public StreamInferenceResponse() {}
    
    public StreamInferenceResponse(String eventType) {
        this.eventType = eventType;
    }
    
    // 便捷方法
    public static StreamInferenceResponse ack(Status status) {
        StreamInferenceResponse response = new StreamInferenceResponse("ack");
        response.setStatus(status);
        return response;
    }
    
    public static StreamInferenceResponse frame(FrameResult frameResult) {
        StreamInferenceResponse response = new StreamInferenceResponse("frame");
        response.setFrameResult(frameResult);
        return response;
    }
    
    /**
     * 状态类
     */
    @Getter
    @Setter
    public static class Status {
        @JsonProperty("code")
        private Integer code;
        
        @JsonProperty("message")
        private String message;
        
        @JsonProperty("details")
        private Object details;
        
        public Status() {}
        
        public Status(Integer code, String message) {
            this.code = code;
            this.message = message;
        }
        
        public static Status success() {
            return new Status(0, "Success");
        }
        
        public static Status error(String message) {
            return new Status(-1, message);
        }
    }
    
    /**
     * 帧结果类
     */
    @Getter
    @Setter
    public static class FrameResult {
        @JsonProperty("frame_index")
        private Long frameIndex;
        
        @JsonProperty("results")
        private java.util.List<InferenceResult> results;
        
        @JsonProperty("meta")
        private java.util.Map<String, Object> meta;
        
        public FrameResult() {}
        
        public FrameResult(Long frameIndex, java.util.List<InferenceResult> results) {
            this.frameIndex = frameIndex;
            this.results = results;
        }
    }
}
