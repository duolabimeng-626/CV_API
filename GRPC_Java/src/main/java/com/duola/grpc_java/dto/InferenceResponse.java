package com.duola.grpc_java.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class InferenceResponse {
    
    @JsonProperty("success")
    private Boolean success;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("detections")
    private List<DetectionResult> detections;
    
    @JsonProperty("overlay_image")
    private String overlayImage; // Base64 encoded overlay image
    
    @JsonProperty("runtime_ms")
    private Double runtimeMs;
    
    @JsonProperty("frame_count")
    private Long frameCount;
    
    @JsonProperty("session_id")
    private String sessionId;
    
    // Constructors
    public InferenceResponse() {}
    
    public InferenceResponse(Boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    
    public InferenceResponse(Boolean success, String message, List<DetectionResult> detections) {
        this.success = success;
        this.message = message;
        this.detections = detections;
    }
    

}
