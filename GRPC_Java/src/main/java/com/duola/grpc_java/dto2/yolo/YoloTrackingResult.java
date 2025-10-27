package com.duola.grpc_java.dto2.yolo;

import com.duola.grpc_java.dto2.base.InferenceResult;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * YOLO跟踪结果
 */
@Getter
@Setter
public class YoloTrackingResult extends InferenceResult {
    
    @JsonProperty("track_id")
    private Integer trackId;
    
    @JsonProperty("detections")
    private List<YoloDetectionResult> detections;
    
    @JsonProperty("track_age")
    private Integer trackAge;
    
    @JsonProperty("track_hits")
    private Integer trackHits;
    
    @JsonProperty("track_state")
    private String trackState; // active, lost, deleted
    
    @JsonProperty("velocity")
    private Object velocity; // {x, y} velocity vector
    
    @JsonProperty("trajectory")
    private List<Object> trajectory; // List of {x, y, timestamp}
    
    // 构造函数
    public YoloTrackingResult() {
        super("tracking", "application/json");
    }
    
    public YoloTrackingResult(Integer trackId, List<YoloDetectionResult> detections) {
        super("tracking", "application/json");
        this.trackId = trackId;
        this.detections = detections;
    }
    
    @Override
    public String getResultType() {
        return "yolo_tracking";
    }
    
    // 便捷方法
    public Boolean isActive() {
        return "active".equals(trackState);
    }
    
    public Boolean isLost() {
        return "lost".equals(trackState);
    }
    
    public Integer getDetectionCount() {
        return detections != null ? detections.size() : 0;
    }
}
