package com.duola.grpc_java.dto2.yolo;

import com.duola.grpc_java.dto2.base.AIInferenceResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * YOLO推理专用响应类
 * 继承自AIInferenceResponse，添加YOLO特有的响应字段
 */
@Getter
@Setter
public class YoloInferenceResponse extends AIInferenceResponse {
    
    @JsonProperty("detections")
    private List<YoloDetectionResult> detections;
    
    @JsonProperty("tracks")
    private List<YoloTrackingResult> tracks;
    
    @JsonProperty("segmentations")
    private List<YoloSegmentationResult> segmentations;
    
    @JsonProperty("overlay_image")
    private String overlayImage; // Base64 encoded overlay image
    
    @JsonProperty("detection_count")
    private Integer detectionCount;
    
    @JsonProperty("track_count")
    private Integer trackCount;
    
    @JsonProperty("segmentation_count")
    private Integer segmentationCount;
    
    @JsonProperty("fps")
    private Double fps;
    
    @JsonProperty("processing_time")
    private Double processingTime;
    
    // 构造函数
    public YoloInferenceResponse() {
        super();
    }
    
    public YoloInferenceResponse(Boolean success, String message) {
        super(success, message);
    }
    
    public YoloInferenceResponse(Boolean success, String message, 
                                List<YoloDetectionResult> detections) {
        super(success, message);
        this.detections = detections;
        this.detectionCount = detections != null ? detections.size() : 0;
    }
    
    // 便捷方法
    public void addDetection(YoloDetectionResult detection) {
        if (this.detections == null) {
            this.detections = new java.util.ArrayList<>();
        }
        this.detections.add(detection);
        this.detectionCount = this.detections.size();
    }
    
    public void addTrack(YoloTrackingResult track) {
        if (this.tracks == null) {
            this.tracks = new java.util.ArrayList<>();
        }
        this.tracks.add(track);
        this.trackCount = this.tracks.size();
    }
    
    public void addSegmentation(YoloSegmentationResult segmentation) {
        if (this.segmentations == null) {
            this.segmentations = new java.util.ArrayList<>();
        }
        this.segmentations.add(segmentation);
        this.segmentationCount = this.segmentations.size();
    }
    
    public Boolean hasDetections() {
        return detections != null && !detections.isEmpty();
    }
    
    public Boolean hasTracks() {
        return tracks != null && !tracks.isEmpty();
    }
    
    public Boolean hasSegmentations() {
        return segmentations != null && !segmentations.isEmpty();
    }
    
    public Integer getTotalObjectCount() {
        int total = 0;
        if (detectionCount != null) total += detectionCount;
        if (trackCount != null) total += trackCount;
        if (segmentationCount != null) total += segmentationCount;
        return total;
    }
    
    // 创建成功响应
    public static YoloInferenceResponse success(String message, List<YoloDetectionResult> detections) {
        return new YoloInferenceResponse(true, message, detections);
    }
    
    // 创建失败响应
    public static YoloInferenceResponse error(String message) {
        return new YoloInferenceResponse(false, message);
    }
    
    // 创建空响应
    public static YoloInferenceResponse empty() {
        return new YoloInferenceResponse(true, "No objects detected", new java.util.ArrayList<>());
    }
}
