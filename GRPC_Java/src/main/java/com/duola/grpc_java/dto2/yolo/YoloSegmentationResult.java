package com.duola.grpc_java.dto2.yolo;

import com.duola.grpc_java.dto2.base.InferenceResult;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * YOLO分割结果
 */
@Getter
@Setter
public class YoloSegmentationResult extends InferenceResult {
    
    @JsonProperty("class_name")
    private String className;
    
    @JsonProperty("class_id")
    private Integer classId;
    
    @JsonProperty("confidence")
    private Double confidence;
    
    @JsonProperty("mask")
    private String mask; // Base64 encoded binary mask
    
    @JsonProperty("contours")
    private Object contours; // List of contour points
    
    @JsonProperty("bbox")
    private Object bbox; // {x, y, width, height}
    
    @JsonProperty("area")
    private Double area;
    
    @JsonProperty("perimeter")
    private Double perimeter;
    
    // 构造函数
    public YoloSegmentationResult() {
        super("segmentation", "application/json");
    }
    
    public YoloSegmentationResult(String className, Integer classId, Double confidence, String mask) {
        super("segmentation", "application/json");
        this.className = className;
        this.classId = classId;
        this.confidence = confidence;
        this.mask = mask;
    }
    
    @Override
    public String getResultType() {
        return "yolo_segmentation";
    }
    
    // 便捷方法
    public Boolean hasValidMask() {
        return mask != null && !mask.isEmpty();
    }
    
    public Double getMaskDensity() {
        return area != null && area > 0 ? area : 0.0;
    }
}
