package com.duola.grpc_java.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * 摄像头配置请求DTO
 */
@Getter
@Setter
public class CameraConfigRequest {
    
    @JsonProperty("session_id")
    private String sessionId;
    
    @JsonProperty("frame_rate")
    private Integer frameRate = 5;
    
    @JsonProperty("detection_threshold")
    private Double detectionThreshold = 0.5;
    
    @JsonProperty("model")
    private String model = "yolo-detection-nano";
    
    @JsonProperty("target_width")
    private Integer targetWidth = 640;
    
    @JsonProperty("target_height")
    private Integer targetHeight = 640;
    
    @JsonProperty("keep_aspect_ratio")
    private Boolean keepAspectRatio = true;
    
    @JsonProperty("mode")
    private String mode = "detect";
    
    @JsonProperty("return_overlay")
    private Boolean returnOverlay = true;
    
    @JsonProperty("max_concurrent_requests")
    private Integer maxConcurrentRequests = 3;
    
    // 构造函数
    public CameraConfigRequest() {}

}



