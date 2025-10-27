package com.duola.grpc_java.dto2.yolo;

import com.duola.grpc_java.dto2.base.AIInferenceConfig;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * YOLO专属推理配置DTO
 * 包含YOLO模型的所有相关配置参数
 */
@Getter
@Setter
public class YoloInferenceConfig extends AIInferenceConfig {
    
    // ========== 基础配置 ==========
    @JsonProperty("target_width")
    private Integer targetWidth = 640;
    
    @JsonProperty("target_height")
    private Integer targetHeight = 640;
    
    @JsonProperty("keep_aspect_ratio")
    private Boolean keepAspectRatio = true;
    
    @JsonProperty("mode")
    private String mode = "detect"; // detect, track, segment
    
    // ========== YOLO特有配置 ==========
    @JsonProperty("confidence_threshold")
    private Double confidenceThreshold = 0.5;
    
    @JsonProperty("nms_threshold")
    private Double nmsThreshold = 0.45;
    
    @JsonProperty("max_detections")
    private Integer maxDetections = 100;
    
    @JsonProperty("class_filter")
    private List<Integer> classFilter;
    
    // ========== 跟踪配置 ==========
    @JsonProperty("track_threshold")
    private Double trackThreshold = 0.3;
    
    @JsonProperty("track_buffer")
    private Integer trackBuffer = 30;
    
    @JsonProperty("track_max_age")
    private Integer trackMaxAge = 30;
    
    @JsonProperty("track_min_hits")
    private Integer trackMinHits = 3;
    
    // ========== 分割配置 ==========
    @JsonProperty("mask_threshold")
    private Double maskThreshold = 0.5;
    
    @JsonProperty("contour_threshold")
    private Double contourThreshold = 0.1;
    
    // ========== 性能优化配置 ==========
    @JsonProperty("batch_size")
    private Integer batchSize = 1;
    
    @JsonProperty("device")
    private String device = "cpu"; // cpu, gpu, cuda
    
    @JsonProperty("precision")
    private String precision = "fp32"; // fp16, fp32
    
    @JsonProperty("optimization_level")
    private Integer optimizationLevel = 1; // 0-3
    
    // ========== 输出配置 ==========
    @JsonProperty("return_detections")
    private Boolean returnDetections = true;
    
    @JsonProperty("return_tracks")
    private Boolean returnTracks = false;
    
    @JsonProperty("return_masks")
    private Boolean returnMasks = false;
    
    @JsonProperty("return_overlay")
    private Boolean returnOverlay = true;
    
    @JsonProperty("coordinate_format")
    private String coordinateFormat = "normalized"; // normalized, pixel
    
    // ========== 可视化配置 ==========
    @JsonProperty("box_color")
    private String boxColor = "#FF0000";
    
    @JsonProperty("text_color")
    private String textColor = "#FFFFFF";
    
    @JsonProperty("font_size")
    private Integer fontSize = 12;
    
    @JsonProperty("line_thickness")
    private Integer lineThickness = 2;
    
    // ========== 流式处理配置 ==========
    @JsonProperty("frame_interval")
    private Integer frameInterval = 1;
    
    @JsonProperty("session_id")
    private String sessionId;
    
    // 构造函数
    public YoloInferenceConfig() {
        super("yolov8n");
    }
    
    public YoloInferenceConfig(String modelName) {
        super(modelName);
    }
    
    // 实现抽象方法
    @Override
    public String getInferenceType() {
        return "yolo";
    }
    
    @Override
    public Map<String, Object> getSpecificOptions() {
        return Map.of(
            "confidence_threshold", confidenceThreshold,
            "nms_threshold", nmsThreshold,
            "max_detections", maxDetections,
            "track_threshold", trackThreshold,
            "mask_threshold", maskThreshold,
            "device", device,
            "precision", precision
        );
    }
    
    // 便捷方法
    public YoloInferenceConfig forDetection() {
        this.mode = "detect";
        this.returnDetections = true;
        this.returnTracks = false;
        this.returnMasks = false;
        return this;
    }
    
    public YoloInferenceConfig forTracking() {
        this.mode = "track";
        this.returnDetections = true;
        this.returnTracks = true;
        this.returnMasks = false;
        return this;
    }
    
    public YoloInferenceConfig forSegmentation() {
        this.mode = "segment";
        this.returnDetections = true;
        this.returnMasks = true;
        this.returnTracks = false;
        return this;
    }
    
    public YoloInferenceConfig withHighAccuracy() {
        this.confidenceThreshold = 0.3;
        this.nmsThreshold = 0.3;
        this.precision = "fp32";
        this.optimizationLevel = 3;
        return this;
    }
    
    public YoloInferenceConfig withHighSpeed() {
        this.confidenceThreshold = 0.6;
        this.nmsThreshold = 0.6;
        this.precision = "fp16";
        this.optimizationLevel = 1;
        return this;
    }
}
