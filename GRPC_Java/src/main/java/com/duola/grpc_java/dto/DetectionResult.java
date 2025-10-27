package com.duola.grpc_java.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class DetectionResult {
    
    @JsonProperty("class_name")
    private String className;
    
    @JsonProperty("class_id")
    private Integer classId;
    
    @JsonProperty("score")
    private Double score;
    
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
    
    // Constructors
    public DetectionResult() {}
    
    public DetectionResult(String className, Integer classId, Double score, 
                          Double x, Double y, Double width, Double height) {
        this.className = className;
        this.classId = classId;
        this.score = score;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
}
