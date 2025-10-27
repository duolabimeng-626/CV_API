package com.duola.grpc_java.service;

import com.duola.grpc_java.dto.CameraInferenceRequest;
import com.duola.grpc_java.dto.CameraInferenceResponse;
import com.duola.grpc_java.dto.CameraConfigRequest;
import com.duola.grpc_java.dto.CameraStatusResponse;
import com.duola.grpc_java.dto.StreamInferenceRequest;
import com.duola.grpc_java.dto.StreamInferenceResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 摄像头推理服务
 */
@Service
public class CameraInferenceService {
    
    @Autowired
    private StreamGrpcInferenceService streamGrpcInferenceService;
    
    // 存储活跃的摄像头会话
    private final Map<String, CameraStatusResponse.CameraSessionInfo> activeSessions = new ConcurrentHashMap<>();
    
    // 统计信息
    private final AtomicInteger totalSessions = new AtomicInteger(0);
    private final AtomicLong totalFrames = new AtomicLong(0);
    private final AtomicLong detectedFrames = new AtomicLong(0);
    private final AtomicLong detectedObjects = new AtomicLong(0);
    
    /**
     * 处理摄像头单帧推理
     */
    public CameraInferenceResponse processCameraFrame(CameraInferenceRequest request) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 验证请求
            if (request.getImage() == null || request.getImage().trim().isEmpty()) {
                return new CameraInferenceResponse(false, "Image is required", "error");
            }
            
            // 转换为StreamInferenceRequest
            StreamInferenceRequest streamRequest = convertToStreamRequest(request);
            
            // 执行推理
            StreamInferenceResponse streamResponse = streamGrpcInferenceService.performSingleInference(streamRequest);
            
            // 更新统计信息
            updateSessionStats(request.getSessionId(), streamResponse);
            
            // 转换为摄像头响应
            CameraInferenceResponse response = convertToCameraResponse(streamResponse, request.getSessionId());
            response.setRuntimeMs((double) (System.currentTimeMillis() - startTime));
            response.setEventType("frame");
            
            return response;
            
        } catch (Exception e) {
            return new CameraInferenceResponse(false, "Camera inference error: " + e.getMessage(), "error");
        }
    }
    
    /**
     * 处理摄像头流式推理
     */
    public void processCameraStream(CameraInferenceRequest request, SseEmitter emitter) {
        try {
            // 验证请求
            if (request.getImage() == null || request.getImage().trim().isEmpty()) {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data(new CameraInferenceResponse(false, "Image is required", "error")));
                emitter.completeWithError(new IllegalArgumentException("Image is required"));
                return;
            }
            
            // 发送ACK
            CameraInferenceResponse ackResponse = new CameraInferenceResponse(true, "Camera stream started", "ack");
            ackResponse.setSessionId(request.getSessionId());
            emitter.send(SseEmitter.event().name("ack").data(ackResponse));
            
            // 转换为StreamInferenceRequest
            StreamInferenceRequest streamRequest = convertToStreamRequest(request);
            
            // 执行流式推理
            streamGrpcInferenceService.performStreamInference(streamRequest, new SseEmitter() {
                @Override
                public void send(Object object) throws IOException {
                    if (object instanceof StreamInferenceResponse) {
                        StreamInferenceResponse streamResponse = (StreamInferenceResponse) object;
                        CameraInferenceResponse cameraResponse = convertToCameraResponse(streamResponse, request.getSessionId());
                        updateSessionStats(request.getSessionId(), streamResponse);
                        emitter.send(SseEmitter.event().name("frame").data(cameraResponse));
                    }
                }
                
                @Override
                public void complete() {
                    emitter.complete();
                }
                
                @Override
                public void completeWithError(Throwable ex) {
                    emitter.completeWithError(ex);
                }
            });
            
        } catch (Exception e) {
            try {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data(new CameraInferenceResponse(false, "Camera stream error: " + e.getMessage(), "error")));
                emitter.completeWithError(e);
            } catch (Exception sendError) {
                emitter.completeWithError(sendError);
            }
        }
    }
    
    /**
     * 配置摄像头会话
     */
    public CameraInferenceResponse configureCameraSession(CameraConfigRequest request) {
        try {
            String sessionId = request.getSessionId();
            if (sessionId == null || sessionId.trim().isEmpty()) {
                sessionId = "camera_session_" + System.currentTimeMillis();
            }
            final String finalSessionId = sessionId;  // 新建一个 effectively final 的变量

            CameraStatusResponse.CameraSessionInfo sessionInfo = activeSessions.computeIfAbsent(
                    finalSessionId,
                    k -> new CameraStatusResponse.CameraSessionInfo(finalSessionId)
            );


            // 更新配置
            sessionInfo.setFrameRate(request.getFrameRate());
            sessionInfo.setDetectionThreshold(request.getDetectionThreshold());
            sessionInfo.setModel(request.getModel());
            
            return new CameraInferenceResponse(true, "Camera session configured successfully", "config");
            
        } catch (Exception e) {
            return new CameraInferenceResponse(false, "Configuration error: " + e.getMessage(), "error");
        }
    }
    
    /**
     * 获取摄像头状态
     */
    public CameraStatusResponse getCameraStatus() {
        try {
            CameraStatusResponse response = new CameraStatusResponse(true, "Camera status retrieved successfully");
            response.setActiveSessions(activeSessions);
            response.setTotalSessions(activeSessions.size());
            
            return response;
            
        } catch (Exception e) {
            return new CameraStatusResponse(false, "Status retrieval error: " + e.getMessage());
        }
    }
    
    /**
     * 停止摄像头会话
     */
    public CameraInferenceResponse stopCameraSession(String sessionId) {
        try {
            if (sessionId != null && activeSessions.containsKey(sessionId)) {
                activeSessions.remove(sessionId);
                return new CameraInferenceResponse(true, "Camera session stopped successfully", "stop");
            } else {
                return new CameraInferenceResponse(false, "Session not found: " + sessionId, "error");
            }
        } catch (Exception e) {
            return new CameraInferenceResponse(false, "Stop session error: " + e.getMessage(), "error");
        }
    }
    
    /**
     * 转换请求格式
     */
    private StreamInferenceRequest convertToStreamRequest(CameraInferenceRequest request) {
        StreamInferenceRequest streamRequest = new StreamInferenceRequest();
        streamRequest.setImage(request.getImage());
        streamRequest.setModel(request.getModel());
        streamRequest.setTargetWidth(request.getTargetWidth());
        streamRequest.setTargetHeight(request.getTargetHeight());
        streamRequest.setKeepAspectRatio(request.getKeepAspectRatio());
        streamRequest.setMode(request.getMode());
        streamRequest.setReturnOverlay(request.getReturnOverlay());
        return streamRequest;
    }
    
    /**
     * 转换响应格式
     */
    private CameraInferenceResponse convertToCameraResponse(StreamInferenceResponse streamResponse, String sessionId) {
        CameraInferenceResponse response = new CameraInferenceResponse(
            streamResponse.getSuccess(), 
            streamResponse.getMessage()
        );
        
        response.setEventType("frame");
        response.setDetections(streamResponse.getDetections());
        response.setOverlayImage(streamResponse.getOverlayImage());
        response.setRuntimeMs(streamResponse.getRuntimeMs());
        response.setSessionId(sessionId);
        response.setTimestamp(System.currentTimeMillis());
        
        return response;
    }
    
    /**
     * 更新会话统计信息
     */
    private void updateSessionStats(String sessionId, StreamInferenceResponse response) {
        if (sessionId != null) {
            CameraStatusResponse.CameraSessionInfo sessionInfo = activeSessions.get(sessionId);
            if (sessionInfo != null) {
                sessionInfo.setTotalFrames(sessionInfo.getTotalFrames() + 1);
                sessionInfo.setLastActivity(System.currentTimeMillis());
                
                if (response.getDetections() != null && !response.getDetections().isEmpty()) {
                    sessionInfo.setDetectedFrames(sessionInfo.getDetectedFrames() + 1);
                    sessionInfo.setDetectedObjects(sessionInfo.getDetectedObjects() + response.getDetections().size());
                }
                
                // 计算平均FPS
                long elapsed = System.currentTimeMillis() - sessionInfo.getStartTime();
                if (elapsed > 0) {
                    double fps = (sessionInfo.getTotalFrames() * 1000.0) / elapsed;
                    sessionInfo.setAvgFps(fps);
                }
            }
        }
        
        // 更新全局统计
        totalFrames.incrementAndGet();
        if (response.getDetections() != null && !response.getDetections().isEmpty()) {
            detectedFrames.incrementAndGet();
            detectedObjects.addAndGet(response.getDetections().size());
        }
    }
}



