package com.duola.grpc_java.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class StreamInferenceRequest {
    
    @JsonProperty("image")
    private String image; // Base64 encoded image
    
    @JsonProperty("video")
    private String video; // Base64 encoded video
    
    @JsonProperty("images")
    private List<String> images; // List of Base64 encoded images for batch processing
    
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
    
    @JsonProperty("stream_mode")
    private String streamMode = "sse"; // sse, reactive, websocket
    
    @JsonProperty("max_frames")
    private Integer maxFrames = 100; // Maximum frames to process for video
    
    @JsonProperty("frame_interval")
    private Integer frameInterval = 1; // Process every Nth frame
    
    // Constructors
    public StreamInferenceRequest() {}
    
    public StreamInferenceRequest(String image) {
        this.image = image;
    }
    
    public StreamInferenceRequest(List<String> images) {
        this.images = images;
    }

}
