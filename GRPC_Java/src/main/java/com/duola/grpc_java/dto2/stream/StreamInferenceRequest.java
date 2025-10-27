package com.duola.grpc_java.dto2.stream;

import com.duola.grpc_java.dto2.base.AIInferenceConfig;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * 流式推理请求类
 * 对应ai.proto中的StreamRequest结构
 */
@Getter
@Setter
public class StreamInferenceRequest {
    
    @JsonProperty("event_type")
    private String eventType; // open, frame, close
    
    @JsonProperty("header")
    private AIInferenceConfig header;
    
    @JsonProperty("frame_data")
    private FrameData frameData;
    
    @JsonProperty("close_meta")
    private Object closeMeta;
    
    // 构造函数
    public StreamInferenceRequest() {}
    
    public StreamInferenceRequest(String eventType) {
        this.eventType = eventType;
    }
    
    // 便捷方法
    public static StreamInferenceRequest open(AIInferenceConfig config) {
        StreamInferenceRequest request = new StreamInferenceRequest("open");
        request.setHeader(config);
        return request;
    }
    
    public static StreamInferenceRequest frame(FrameData frameData) {
        StreamInferenceRequest request = new StreamInferenceRequest("frame");
        request.setFrameData(frameData);
        return request;
    }
    
    public static StreamInferenceRequest close(Object meta) {
        StreamInferenceRequest request = new StreamInferenceRequest("close");
        request.setCloseMeta(meta);
        return request;
    }
    
    /**
     * 帧数据内部类
     */
    @Getter
    @Setter
    public static class FrameData {
        @JsonProperty("inputs")
        private java.util.List<InputEnvelope> inputs;
        
        @JsonProperty("frame_index")
        private Long frameIndex;
        
        @JsonProperty("timestamp")
        private Long timestamp;
        
        public FrameData() {}
        
        public FrameData(Long frameIndex, java.util.List<InputEnvelope> inputs) {
            this.frameIndex = frameIndex;
            this.inputs = inputs;
        }
    }
    
    /**
     * 输入信封类
     */
    @Getter
    @Setter
    public static class InputEnvelope {
        @JsonProperty("kind")
        private String kind; // image, text, audio
        
        @JsonProperty("content_type")
        private String contentType; // image/jpeg, text/plain
        
        @JsonProperty("payload")
        private Object payload; // Base64 encoded data or text
        
        @JsonProperty("tags")
        private java.util.Map<String, String> tags;
        
        public InputEnvelope() {}
        
        public InputEnvelope(String kind, String contentType, Object payload) {
            this.kind = kind;
            this.contentType = contentType;
            this.payload = payload;
        }
    }
}
