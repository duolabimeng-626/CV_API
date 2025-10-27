package com.duola.grpc_java.controller;

import com.duola.grpc_java.dto.InferenceRequest;
import com.duola.grpc_java.dto.InferenceResponse;
import com.duola.grpc_java.service.GrpcInferenceService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class InferenceController {
    
    private final GrpcInferenceService grpcInferenceService;
    
    public InferenceController(@Value("${grpc.server.host:localhost}") String grpcHost,
                            @Value("${grpc.server.port:50051}") int grpcPort) {
        this.grpcInferenceService = new GrpcInferenceService(grpcHost, grpcPort);
    }
    
    @PostMapping(value = "/inference", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InferenceResponse> performInference(@RequestBody InferenceRequest request) {
        try {
            // 验证请求
            if (request.getImage() == null || request.getImage().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new InferenceResponse(false, "Image is required"));
            }
            
            // 执行推理
            InferenceResponse response = grpcInferenceService.performInference(request);
            
            if (response.getSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.internalServerError().body(response);
            }
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new InferenceResponse(false, "Internal server error: " + e.getMessage()));
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Service is running");
    }
    
    @GetMapping("/")
    public ResponseEntity<String> root() {
        return ResponseEntity.ok("YOLO Inference API - Send POST requests to /api/v1/inference");
    }
}
