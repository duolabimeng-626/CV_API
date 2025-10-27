package com.duola.grpc_java.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 摄像头推理响应DTO
 */
@Getter
@Setter
public class CameraInferenceResponse {
    
    @JsonProperty("success")
    private Boolean success;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("event_type")
    private String eventType;
    
    @JsonProperty("frame_index")
    private Integer frameIndex;
    
    @JsonProperty("detections")
    private List<DetectionResult> detections;
    
    @JsonProperty("overlay_image")
    private String overlayImage;
    
    @JsonProperty("runtime_ms")
    private Double runtimeMs;
    
    @JsonProperty("frame_count")
    private Integer frameCount;
    
    @JsonProperty("session_id")
    private String sessionId;
    
    @JsonProperty("timestamp")
    private Long timestamp;
    
    @JsonProperty("progress")
    private Double progress;
    
    // 构造函数
    public CameraInferenceResponse() {}
    
    public CameraInferenceResponse(Boolean success, String message) {
        this.success = success;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }
    
    public CameraInferenceResponse(Boolean success, String message, String eventType) {
        this.success = success;
        this.message = message;
        this.eventType = eventType;
        this.timestamp = System.currentTimeMillis();
    }
    

}



