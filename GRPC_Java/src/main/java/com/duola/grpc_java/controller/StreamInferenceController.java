package com.duola.grpc_java.controller;

import com.duola.grpc_java.dto.StreamInferenceRequest;
import com.duola.grpc_java.dto.StreamInferenceResponse;
import com.duola.grpc_java.service.StreamGrpcInferenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/stream")
@CrossOrigin(origins = "*")
public class StreamInferenceController {
    
    @Autowired
    private StreamGrpcInferenceService streamGrpcInferenceService;
    
    /**
     * 流式推理接口 - 使用SSE (Server-Sent Events)
     * 支持实时返回推理结果
     */
    @PostMapping(value = "/inference/sse", consumes = MediaType.APPLICATION_JSON_VALUE)
    public SseEmitter streamInferenceSSE(@RequestBody StreamInferenceRequest request) {
        SseEmitter emitter = new SseEmitter(300000L); // 5分钟超时
        
        // 异步处理推理请求
        CompletableFuture.runAsync(() -> {
            try {
                streamGrpcInferenceService.performStreamInference(request, emitter);
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data(new StreamInferenceResponse(false, "Stream error: " + e.getMessage())));
                    emitter.completeWithError(e);
                } catch (Exception sendError) {
                    emitter.completeWithError(sendError);
                }
            }
        });
        
        return emitter;
    }
    
    /**
     * 流式推理接口 - 使用Reactive Streams
     * 返回Flux流式数据
     */
    @PostMapping(value = "/inference/reactive", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Flux<StreamInferenceResponse> streamInferenceReactive(@RequestBody StreamInferenceRequest request) {
        return streamGrpcInferenceService.performStreamInferenceReactive(request);
    }
    
    /**
     * 视频流推理接口 - 支持视频文件上传和实时处理
     */
    @PostMapping(value = "/video/inference", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SseEmitter streamVideoInference(
            @RequestParam("video") String videoBase64,
            @RequestParam(value = "model", defaultValue = "yolo-detection-nano") String model,
            @RequestParam(value = "target_width", defaultValue = "640") Integer targetWidth,
            @RequestParam(value = "target_height", defaultValue = "640") Integer targetHeight,
            @RequestParam(value = "keep_aspect_ratio", defaultValue = "true") Boolean keepAspectRatio,
            @RequestParam(value = "mode", defaultValue = "detect") String mode,
            @RequestParam(value = "return_overlay", defaultValue = "true") Boolean returnOverlay) {
        
        StreamInferenceRequest request = new StreamInferenceRequest();
        request.setVideo(videoBase64);
        request.setModel(model);
        request.setTargetWidth(targetWidth);
        request.setTargetHeight(targetHeight);
        request.setKeepAspectRatio(keepAspectRatio);
        request.setMode(mode);
        request.setReturnOverlay(returnOverlay);
        
        SseEmitter emitter = new SseEmitter(600000L); // 10分钟超时
        
        CompletableFuture.runAsync(() -> {
            try {
                streamGrpcInferenceService.performVideoStreamInference(request, emitter);
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data(new StreamInferenceResponse(false, "Video stream error: " + e.getMessage())));
                    emitter.completeWithError(e);
                } catch (Exception sendError) {
                    emitter.completeWithError(sendError);
                }
            }
        });
        
        return emitter;
    }
    
    /**
     * 批量图片推理接口 - 支持多张图片同时处理
     */
    @PostMapping(value = "/batch/inference", consumes = MediaType.APPLICATION_JSON_VALUE)
    public SseEmitter batchInference(@RequestBody StreamInferenceRequest request) {
        SseEmitter emitter = new SseEmitter(600000L); // 10分钟超时
        
        CompletableFuture.runAsync(() -> {
            try {
                streamGrpcInferenceService.performBatchInference(request, emitter);
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data(new StreamInferenceResponse(false, "Batch inference error: " + e.getMessage())));
                    emitter.completeWithError(e);
                } catch (Exception sendError) {
                    emitter.completeWithError(sendError);
                }
            }
        });
        
        return emitter;
    }
    
    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Stream inference service is running");
    }
}
