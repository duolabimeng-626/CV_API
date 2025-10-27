package com.duola.grpc_java.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * 摄像头推理请求DTO
 */
@Getter
@Setter
public class CameraInferenceRequest {
    
    @JsonProperty("image")
    private String image;
    
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
    
    @JsonProperty("frame_interval")
    private Integer frameInterval = 5;
    
    @JsonProperty("detection_threshold")
    private Double detectionThreshold = 0.5;
    
    @JsonProperty("session_id")
    private String sessionId;
    
    // 构造函数
    public CameraInferenceRequest() {}

}
