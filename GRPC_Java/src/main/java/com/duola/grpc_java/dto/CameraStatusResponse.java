package com.duola.grpc_java.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 摄像头状态响应DTO
 */
@Getter
@Setter
public class CameraStatusResponse {
    
    @JsonProperty("success")
    private Boolean success;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("active_sessions")
    private Map<String, CameraSessionInfo> activeSessions;
    
    @JsonProperty("total_sessions")
    private Integer totalSessions;
    
    @JsonProperty("timestamp")
    private Long timestamp;
    
    // 构造函数
    public CameraStatusResponse() {
        this.activeSessions = new ConcurrentHashMap<>();
        this.timestamp = System.currentTimeMillis();
    }
    
    public CameraStatusResponse(Boolean success, String message) {
        this();
        this.success = success;
        this.message = message;
    }
    
    // Getter和Setter方法
    public Boolean getSuccess() {
        return success;
    }
    
    public void setSuccess(Boolean success) {
        this.success = success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public Map<String, CameraSessionInfo> getActiveSessions() {
        return activeSessions;
    }
    
    public void setActiveSessions(Map<String, CameraSessionInfo> activeSessions) {
        this.activeSessions = activeSessions;
    }
    
    public Integer getTotalSessions() {
        return totalSessions;
    }
    
    public void setTotalSessions(Integer totalSessions) {
        this.totalSessions = totalSessions;
    }
    
    public Long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
    
    /**
     * 摄像头会话信息内部类
     */
    @Getter
    @Setter
    public static class CameraSessionInfo {
        
        @JsonProperty("session_id")
        private String sessionId;
        
        @JsonProperty("frame_rate")
        private Integer frameRate;
        
        @JsonProperty("detection_threshold")
        private Double detectionThreshold;
        
        @JsonProperty("model")
        private String model;
        
        @JsonProperty("total_frames")
        private Integer totalFrames;
        
        @JsonProperty("detected_frames")
        private Integer detectedFrames;
        
        @JsonProperty("detected_objects")
        private Integer detectedObjects;
        
        @JsonProperty("avg_fps")
        private Double avgFps;
        
        @JsonProperty("start_time")
        private Long startTime;
        
        @JsonProperty("last_activity")
        private Long lastActivity;
        
        // 构造函数
        public CameraSessionInfo() {}
        
        public CameraSessionInfo(String sessionId) {
            this.sessionId = sessionId;
            this.startTime = System.currentTimeMillis();
            this.lastActivity = System.currentTimeMillis();
        }

    }
}



