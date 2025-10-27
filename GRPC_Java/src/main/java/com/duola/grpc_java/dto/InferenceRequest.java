package com.duola.grpc_java.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InferenceRequest {
    
    @JsonProperty("image")
    private String image; // Base64 encoded image
    
    @JsonProperty("model")
    private String model = "yolov8n";
    
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
    
    // Constructors
    public InferenceRequest() {}
    
    public InferenceRequest(String image) {
        this.image = image;
    }

}
