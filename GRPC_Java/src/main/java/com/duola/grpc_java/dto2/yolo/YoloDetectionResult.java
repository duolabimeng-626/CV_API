package com.duola.grpc_java.dto2.yolo;

import com.duola.grpc_java.dto2.base.InferenceResult;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * YOLO检测结果
 */
@Getter
@Setter
public class YoloDetectionResult extends InferenceResult {
    
    @JsonProperty("class_name")
    private String className;
    
    @JsonProperty("class_id")
    private Integer classId;
    
    @JsonProperty("confidence")
    private Double confidence;
    
    @JsonProperty("x")
    private Double x;
    
    @JsonProperty("y")
    private Double y;
    
    @JsonProperty("width")
    private Double width;
    
    @JsonProperty("height")
    private Double height;
    
    @JsonProperty("track_id")
    private Integer trackId;
    
    @JsonProperty("mask")
    private String mask; // Base64 encoded mask for segmentation
    
    @JsonProperty("keypoints")
    private Object keypoints; // For pose estimation
    
    // 构造函数
    public YoloDetectionResult() {
        super("detection", "application/json");
    }
    
    public YoloDetectionResult(String className, Integer classId, Double confidence, 
                              Double x, Double y, Double width, Double height) {
        super("detection", "application/json");
        this.className = className;
        this.classId = classId;
        this.confidence = confidence;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
    
    @Override
    public String getResultType() {
        return "yolo_detection";
    }
    
    // 便捷方法
    public Double getCenterX() {
        return x + width / 2.0;
    }
    
    public Double getCenterY() {
        return y + height / 2.0;
    }
    
    public Double getArea() {
        return width * height;
    }
    
    public Boolean isHighConfidence(Double threshold) {
        return confidence >= threshold;
    }
}
