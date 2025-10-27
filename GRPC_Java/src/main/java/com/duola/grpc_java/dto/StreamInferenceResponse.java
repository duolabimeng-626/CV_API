package com.duola.grpc_java.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class StreamInferenceResponse {
    
    @JsonProperty("success")
    private Boolean success;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("frame_index")
    private Integer frameIndex;
    
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
    
    @JsonProperty("timestamp")
    private Long timestamp;
    
    @JsonProperty("event_type")
    private String eventType; // ack, frame, error, completed
    
    @JsonProperty("progress")
    private Double progress; // 0.0 to 1.0
    
    @JsonProperty("total_frames")
    private Integer totalFrames;
    
    @JsonProperty("processed_frames")
    private Integer processedFrames;
    
    // Constructors
    public StreamInferenceResponse() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public StreamInferenceResponse(Boolean success, String message) {
        this();
        this.success = success;
        this.message = message;
    }
    
    public StreamInferenceResponse(Boolean success, String message, List<DetectionResult> detections) {
        this();
        this.success = success;
        this.message = message;
        this.detections = detections;
    }
    
    public StreamInferenceResponse(Boolean success, String message, Integer frameIndex) {
        this();
        this.success = success;
        this.message = message;
        this.frameIndex = frameIndex;
    }
}
