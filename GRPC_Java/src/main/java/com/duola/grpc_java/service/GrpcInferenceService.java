package com.duola.grpc_java.service;

import com.duola.grpc_java.dto.DetectionResult;
import com.duola.grpc_java.dto.InferenceRequest;
import com.duola.grpc_java.dto.InferenceResponse;
import com.google.protobuf.ByteString;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.google.protobuf.util.Timestamps;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.ai.*;
import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class GrpcInferenceService {
    
    private final String grpcServerHost;
    private final int grpcServerPort;
    
    public GrpcInferenceService(String grpcServerHost, int grpcServerPort) {
        this.grpcServerHost = grpcServerHost;
        this.grpcServerPort = grpcServerPort;
    }
    
    public InferenceResponse performInference(InferenceRequest request) {
        ManagedChannel channel = null;
        try {
            // 创建 gRPC 连接
            channel = ManagedChannelBuilder
                    .forAddress(grpcServerHost, grpcServerPort)
                    .usePlaintext()
                    .maxInboundMessageSize(32 * 1024 * 1024) // 32MB
                    .build();
            
            InferenceStreamServiceGrpc.InferenceStreamServiceStub stub = 
                    InferenceStreamServiceGrpc.newStub(channel);
            
            // 解码 Base64 图像
            byte[] imageBytes = Base64.getDecoder().decode(request.getImage());
            
            // 创建响应对象
            InferenceResponse response = new InferenceResponse();
            CountDownLatch finishLatch = new CountDownLatch(1);
            
            // 创建请求观察者
            StreamObserver<StreamRequest> requestObserver = stub.stream(
                    new StreamObserver<StreamResponse>() {
                        @Override
                        public void onNext(StreamResponse grpcResponse) {
                            if (grpcResponse.hasAck()) {
                                StreamAck ack = grpcResponse.getAck();
                                if (ack.getStatus().getCode() != 0) {
                                    response.setSuccess(false);
                                    response.setMessage("gRPC error: " + ack.getStatus().getMessage());
                                }
                            } else if (grpcResponse.hasFrame()) {
                                FrameResult frameResult = grpcResponse.getFrame();
                                processFrameResult(frameResult, response);
                            }
                        }
                        
                        @Override
                        public void onError(Throwable t) {
                            response.setSuccess(false);
                            response.setMessage("gRPC stream error: " + t.getMessage());
                            finishLatch.countDown();
                        }
                        
                        @Override
                        public void onCompleted() {
                            finishLatch.countDown();
                        }
                    }
            );
            
            // 发送推理请求
            sendInferenceRequest(requestObserver, request, imageBytes);
            
            // 等待响应完成
            if (!finishLatch.await(30, TimeUnit.SECONDS)) {
                response.setSuccess(false);
                response.setMessage("Request timeout");
            }
            
            return response;
            
        } catch (Exception e) {
            return new InferenceResponse(false, "Service error: " + e.getMessage());
        } finally {
            if (channel != null) {
                channel.shutdownNow();
            }
        }
    }
    
    private void sendInferenceRequest(StreamObserver<StreamRequest> requestObserver, 
                                   InferenceRequest request, byte[] imageBytes) {
        try {
            // 构建推理头部
            Struct.Builder optionsBuilder = Struct.newBuilder();
            optionsBuilder.putFields("target_w", Value.newBuilder().setNumberValue(request.getTargetWidth()).build());
            optionsBuilder.putFields("target_h", Value.newBuilder().setNumberValue(request.getTargetHeight()).build());
            optionsBuilder.putFields("keep_aspect_ratio", Value.newBuilder().setBoolValue(request.getKeepAspectRatio()).build());
            optionsBuilder.putFields("mode", Value.newBuilder().setStringValue(request.getMode()).build());
            optionsBuilder.putFields("persist", Value.newBuilder().setBoolValue(false).build());
            optionsBuilder.putFields("return_overlay", Value.newBuilder().setBoolValue(request.getReturnOverlay()).build());
            
            InferenceHeader header = InferenceHeader.newBuilder()
                    .setModel(ModelSpec.newBuilder()
                            .setName(request.getModel())
                            .setVersion("v1")
                            .build())
                    .setOptions(optionsBuilder.build())
                    .addAccept("application/x-protobuf;type=\"google.protobuf.Struct\"")
                    .addAccept("image/jpeg;kind=\"overlay\"")
                    .build();
            
            // 发送流打开请求
            StreamRequest openReq = StreamRequest.newBuilder()
                    .setOpen(StreamOpen.newBuilder().setHeader(header).build())
                    .build();
            
            requestObserver.onNext(openReq);
            
            // 发送图像帧
            InputEnvelope input = InputEnvelope.newBuilder()
                    .setKind("image")
                    .setContentType("image/jpeg")
                    .setBinary(ByteString.copyFrom(imageBytes))
                    .build();
            
            StreamFrame frame = StreamFrame.newBuilder()
                    .addInputs(input)
                    .setFrameIndex(0)
                    .setTs(Timestamps.fromMillis(System.currentTimeMillis()))
                    .build();
            
            StreamRequest frameReq = StreamRequest.newBuilder()
                    .setFrame(frame)
                    .build();
            
            requestObserver.onNext(frameReq);
            
            // 等待处理
            Thread.sleep(1000);
            
            // 发送流关闭请求
            StreamRequest closeReq = StreamRequest.newBuilder()
                    .setClose(StreamClose.newBuilder().build())
                    .build();
            
            requestObserver.onNext(closeReq);
            requestObserver.onCompleted();
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to send inference request", e);
        }
    }
    
    private void processFrameResult(FrameResult frameResult, InferenceResponse response) {
        List<DetectionResult> detections = new ArrayList<>();
        
        // 处理检测结果
        for (ResultEnvelope envelope : frameResult.getResultsList()) {
            if ("detections".equals(envelope.getKind()) && envelope.hasMessage()) {
                // 解析检测结果
                try {
                    Struct struct = envelope.getMessage().unpack(Struct.class);
                    if (struct.containsFields("items")) {
                        com.google.protobuf.Value itemsValue = struct.getFieldsOrThrow("items");
                        if (itemsValue.hasListValue()) {
                            for (com.google.protobuf.Value item : itemsValue.getListValue().getValuesList()) {
                                if (item.hasStructValue()) {
                                    Struct itemStruct = item.getStructValue();
                                    DetectionResult detection = parseDetectionResult(itemStruct);
                                    if (detection != null) {
                                        detections.add(detection);
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    // 忽略解析错误
                }
            } else if ("overlay".equals(envelope.getKind()) && envelope.hasBinary()) {
                // 处理叠加图像
                String overlayBase64 = Base64.getEncoder().encodeToString(
                        envelope.getBinary().toByteArray());
                response.setOverlayImage(overlayBase64);
            }
        }
        
        // 处理元数据
        if (frameResult.hasMeta()) {
            Struct meta = frameResult.getMeta();
            if (meta.containsFields("runtime_ms")) {
                response.setRuntimeMs(meta.getFieldsOrThrow("runtime_ms").getNumberValue());
            }
            if (meta.containsFields("frame_count")) {
                response.setFrameCount((long) meta.getFieldsOrThrow("frame_count").getNumberValue());
            }
            if (meta.containsFields("session_id")) {
                response.setSessionId(meta.getFieldsOrThrow("session_id").getStringValue());
            }
        }
        
        response.setSuccess(true);
        response.setMessage("Inference completed successfully");
        response.setDetections(detections);
    }
    
    private DetectionResult parseDetectionResult(Struct itemStruct) {
        try {
            DetectionResult detection = new DetectionResult();
            
            if (itemStruct.containsFields("class_name")) {
                detection.setClassName(itemStruct.getFieldsOrThrow("class_name").getStringValue());
            }
            if (itemStruct.containsFields("class_id")) {
                detection.setClassId((int) itemStruct.getFieldsOrThrow("class_id").getNumberValue());
            }
            if (itemStruct.containsFields("score")) {
                detection.setScore(itemStruct.getFieldsOrThrow("score").getNumberValue());
            }
            if (itemStruct.containsFields("x")) {
                detection.setX(itemStruct.getFieldsOrThrow("x").getNumberValue());
            }
            if (itemStruct.containsFields("y")) {
                detection.setY(itemStruct.getFieldsOrThrow("y").getNumberValue());
            }
            if (itemStruct.containsFields("width")) {
                detection.setWidth(itemStruct.getFieldsOrThrow("width").getNumberValue());
            }
            if (itemStruct.containsFields("height")) {
                detection.setHeight(itemStruct.getFieldsOrThrow("height").getNumberValue());
            }
            if (itemStruct.containsFields("track_id")) {
                detection.setTrackId((int) itemStruct.getFieldsOrThrow("track_id").getNumberValue());
            }
            
            return detection;
        } catch (Exception e) {
            return null;
        }
    }
}
