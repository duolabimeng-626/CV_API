package com.duola.grpc_java.controller;

import com.duola.grpc_java.dto.CameraInferenceRequest;
import com.duola.grpc_java.dto.CameraInferenceResponse;
import com.duola.grpc_java.dto.CameraConfigRequest;
import com.duola.grpc_java.dto.CameraStatusResponse;
import com.duola.grpc_java.service.CameraInferenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.CompletableFuture;

/**
 * 摄像头推理控制器
 */
@RestController
@RequestMapping("/api/v1/camera")
@CrossOrigin(origins = "*")
public class CameraController {
    
    @Autowired
    private CameraInferenceService cameraInferenceService;
    
    /**
     * 摄像头单帧推理接口
     */
    @PostMapping(value = "/frame/inference", consumes = MediaType.APPLICATION_JSON_VALUE)
    public SseEmitter cameraFrameInference(@RequestBody CameraInferenceRequest request) {
        SseEmitter emitter = new SseEmitter(30000L); // 30秒超时
        
        // 异步处理推理请求
        CompletableFuture.runAsync(() -> {
            try {
                CameraInferenceResponse response = cameraInferenceService.processCameraFrame(request);
                emitter.send(SseEmitter.event()
                        .name("frame")
                        .data(response));
                emitter.complete();
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data(new CameraInferenceResponse(false, "Frame inference error: " + e.getMessage(), "error")));
                    emitter.completeWithError(e);
                } catch (Exception sendError) {
                    emitter.completeWithError(sendError);
                }
            }
        });
        
        return emitter;
    }
    
    /**
     * 摄像头流式推理接口
     */
    @PostMapping(value = "/stream/inference", consumes = MediaType.APPLICATION_JSON_VALUE)
    public SseEmitter cameraStreamInference(@RequestBody CameraInferenceRequest request) {
        SseEmitter emitter = new SseEmitter(300000L); // 5分钟超时
        
        // 异步处理流式推理请求
        CompletableFuture.runAsync(() -> {
            try {
                cameraInferenceService.processCameraStream(request, emitter);
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data(new CameraInferenceResponse(false, "Stream inference error: " + e.getMessage(), "error")));
                    emitter.completeWithError(e);
                } catch (Exception sendError) {
                    emitter.completeWithError(sendError);
                }
            }
        });
        
        return emitter;
    }
    
    /**
     * 摄像头流状态查询接口
     */
    @GetMapping("/status")
    public ResponseEntity<CameraStatusResponse> getCameraStatus() {
        try {
            CameraStatusResponse response = cameraInferenceService.getCameraStatus();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            CameraStatusResponse errorResponse = new CameraStatusResponse(false, "Status retrieval error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 摄像头流配置接口
     */
    @PostMapping(value = "/config", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CameraInferenceResponse> configureCamera(@RequestBody CameraConfigRequest request) {
        try {
            CameraInferenceResponse response = cameraInferenceService.configureCameraSession(request);
            if (response.getSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            CameraInferenceResponse errorResponse = new CameraInferenceResponse(false, "Configuration error: " + e.getMessage(), "error");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 停止摄像头流接口
     */
    @PostMapping("/stop")
    public ResponseEntity<CameraInferenceResponse> stopCameraStream(@RequestParam("sessionId") String sessionId) {
        try {
            CameraInferenceResponse response = cameraInferenceService.stopCameraSession(sessionId);
            if (response.getSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            CameraInferenceResponse errorResponse = new CameraInferenceResponse(false, "Stop session error: " + e.getMessage(), "error");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Camera service is running");
    }
}



