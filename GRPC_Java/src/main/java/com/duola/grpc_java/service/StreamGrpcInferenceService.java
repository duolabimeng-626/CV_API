package com.duola.grpc_java.service;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.duola.grpc_java.dto.DetectionResult;
import com.duola.grpc_java.dto.StreamInferenceRequest;
import com.duola.grpc_java.dto.StreamInferenceResponse;
import com.duola.grpc_java.util.HeaderUtils;
import com.duola.grpc_java.util.NacosUtils;
import com.google.protobuf.ByteString;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.google.protobuf.util.Timestamps;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.ai.*;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class StreamGrpcInferenceService {
    
    @org.springframework.beans.factory.annotation.Value("${nacos.server.addr:127.0.0.1:8848}")
    private String nacosServerAddr;
    
    @org.springframework.beans.factory.annotation.Value("${nacos.namespace:}")
    private String nacosNamespace;
    
    @org.springframework.beans.factory.annotation.Value("${nacos.username:nacos}")
    private String nacosUsername;
    
    @org.springframework.beans.factory.annotation.Value("${nacos.password:nacos}")
    private String nacosPassword;
    
    @org.springframework.beans.factory.annotation.Value("${nacos.group:DEFAULT_GROUP}")
    private String nacosGroup;
    
    @org.springframework.beans.factory.annotation.Value("${grpc.service.name:yolo-detection-nano}")
    private String grpcServiceName;
    
    private static final String OUTPUT_DIR = "output";
    
    /**
     * 使用SSE进行流式推理
     */
    public void performStreamInference(StreamInferenceRequest request, SseEmitter emitter) throws Exception {
        Instance instance = discoverInstance();
        if (instance == null) {
            emitter.send(SseEmitter.event()
                    .name("error")
                    .data(new StreamInferenceResponse(false, "No healthy gRPC service instance found")));
            emitter.complete();
            return;
        }
        
        ManagedChannel channel = createChannel(instance);
        try {
            InferenceStreamServiceGrpc.InferenceStreamServiceStub stub = 
                    InferenceStreamServiceGrpc.newStub(channel);
            
            CountDownLatch finishLatch = new CountDownLatch(1);
            AtomicInteger frameCount = new AtomicInteger(0);
            
            StreamObserver<StreamRequest> requestObserver = createSSEObserver(stub, emitter, finishLatch, frameCount);
            
            // 发送推理请求
            sendStreamRequest(requestObserver, request);
            
            // 等待完成
            if (!finishLatch.await(300, TimeUnit.SECONDS)) {
                emitter.send(SseEmitter.event()
                        .name("timeout")
                        .data(new StreamInferenceResponse(false, "Request timeout")));
            }
            
            emitter.complete();
            
        } finally {
            channel.shutdownNow();
        }
    }
    
    /**
     * 使用Reactive Streams进行流式推理
     */
    public Flux<StreamInferenceResponse> performStreamInferenceReactive(StreamInferenceRequest request) {
        return Flux.create(sink -> {
            try {
                Instance instance = discoverInstance();
                if (instance == null) {
                    sink.next(new StreamInferenceResponse(false, "No healthy gRPC service instance found"));
                    sink.complete();
                    return;
                }
                
                ManagedChannel channel = createChannel(instance);
                InferenceStreamServiceGrpc.InferenceStreamServiceStub stub = 
                        InferenceStreamServiceGrpc.newStub(channel);
                
                CountDownLatch finishLatch = new CountDownLatch(1);
                AtomicInteger frameCount = new AtomicInteger(0);
                
                StreamObserver<StreamRequest> requestObserver = createReactiveObserver(stub, sink, finishLatch, frameCount);
                
                // 发送推理请求
                sendStreamRequest(requestObserver, request);
                
                // 等待完成
                if (!finishLatch.await(300, TimeUnit.SECONDS)) {
                    sink.next(new StreamInferenceResponse(false, "Request timeout"));
                }
                
                sink.complete();
                channel.shutdownNow();
                
            } catch (Exception e) {
                sink.next(new StreamInferenceResponse(false, "Stream error: " + e.getMessage()));
                sink.complete();
            }
        });
    }
    
    /**
     * 单次推理（非流式）
     */
    public StreamInferenceResponse performSingleInference(StreamInferenceRequest request) {
        try {
            Instance instance = discoverInstance();
            if (instance == null) {
                return new StreamInferenceResponse(false, "No healthy gRPC service instance found");
            }
            
            ManagedChannel channel = createChannel(instance);
            try {
                InferenceStreamServiceGrpc.InferenceStreamServiceStub stub = 
                        InferenceStreamServiceGrpc.newStub(channel);
                
                CountDownLatch finishLatch = new CountDownLatch(1);
                AtomicInteger frameCount = new AtomicInteger(0);
                StreamInferenceResponse[] response = new StreamInferenceResponse[1];
                
                StreamObserver<StreamRequest> requestObserver = stub.streamInference(new StreamObserver<StreamResponse>() {
                    @Override
                    public void onNext(StreamResponse streamResponse) {
                        try {
                            response[0] = processStreamResponse(streamResponse, frameCount.getAndIncrement());
                        } catch (Exception e) {
                            response[0] = new StreamInferenceResponse(false, "Processing error: " + e.getMessage());
                        }
                    }
                    
                    @Override
                    public void onError(Throwable t) {
                        response[0] = new StreamInferenceResponse(false, "gRPC error: " + t.getMessage());
                        finishLatch.countDown();
                    }
                    
                    @Override
                    public void onCompleted() {
                        finishLatch.countDown();
                    }
                });
                
                // 发送推理请求
                sendStreamRequest(requestObserver, request);
                
                // 等待完成
                if (!finishLatch.await(30, TimeUnit.SECONDS)) {
                    return new StreamInferenceResponse(false, "Request timeout");
                }
                
                return response[0] != null ? response[0] : new StreamInferenceResponse(false, "No response received");
                
            } finally {
                channel.shutdownNow();
            }
            
        } catch (Exception e) {
            return new StreamInferenceResponse(false, "Single inference error: " + e.getMessage());
        }
    }
    
    /**
     * 视频流推理
     */
    public void performVideoStreamInference(StreamInferenceRequest request, SseEmitter emitter) throws Exception {
        Instance instance = discoverInstance();
        if (instance == null) {
            emitter.send(SseEmitter.event()
                    .name("error")
                    .data(new StreamInferenceResponse(false, "No healthy gRPC service instance found")));
            emitter.complete();
            return;
        }
        
        ManagedChannel channel = createChannel(instance);
        try {
            InferenceStreamServiceGrpc.InferenceStreamServiceStub stub = 
                    InferenceStreamServiceGrpc.newStub(channel);
            
            CountDownLatch finishLatch = new CountDownLatch(1);
            AtomicInteger frameCount = new AtomicInteger(0);
            
            StreamObserver<StreamRequest> requestObserver = createSSEObserver(stub, emitter, finishLatch, frameCount);
            
            // 发送视频推理请求
            sendVideoStreamRequest(requestObserver, request);
            
            // 等待完成
            if (!finishLatch.await(600, TimeUnit.SECONDS)) {
                emitter.send(SseEmitter.event()
                        .name("timeout")
                        .data(new StreamInferenceResponse(false, "Video request timeout")));
            }
            
            emitter.complete();
            
        } finally {
            channel.shutdownNow();
        }
    }
    
    /**
     * 批量推理
     */
    public void performBatchInference(StreamInferenceRequest request, SseEmitter emitter) throws Exception {
        Instance instance = discoverInstance();
        if (instance == null) {
            emitter.send(SseEmitter.event()
                    .name("error")
                    .data(new StreamInferenceResponse(false, "No healthy gRPC service instance found")));
            emitter.complete();
            return;
        }
        
        ManagedChannel channel = createChannel(instance);
        try {
            InferenceStreamServiceGrpc.InferenceStreamServiceStub stub = 
                    InferenceStreamServiceGrpc.newStub(channel);
            
            CountDownLatch finishLatch = new CountDownLatch(1);
            AtomicInteger frameCount = new AtomicInteger(0);
            
            StreamObserver<StreamRequest> requestObserver = createSSEObserver(stub, emitter, finishLatch, frameCount);
            
            // 发送批量推理请求
            sendBatchStreamRequest(requestObserver, request);
            
            // 等待完成
            if (!finishLatch.await(600, TimeUnit.SECONDS)) {
                emitter.send(SseEmitter.event()
                        .name("timeout")
                        .data(new StreamInferenceResponse(false, "Batch request timeout")));
            }
            
            emitter.complete();
            
        } finally {
            channel.shutdownNow();
        }
    }
    
    private Instance discoverInstance() {
        try {
            return NacosUtils.selectOneHealthyInstance(
                    nacosServerAddr,
                    nacosNamespace,
                    nacosUsername,
                    nacosPassword,
                    grpcServiceName,
                    nacosGroup
            );
        } catch (NacosException e) {
            System.err.println("Nacos discovery failed: " + e.getMessage());
            return null;
        }
    }
    
    private ManagedChannel createChannel(Instance instance) {
        return NacosUtils.createPlainChannel(instance, 64);
    }
    
    private StreamObserver<StreamRequest> createSSEObserver(
            InferenceStreamServiceGrpc.InferenceStreamServiceStub stub,
            SseEmitter emitter,
            CountDownLatch finishLatch,
            AtomicInteger frameCount) {
        
        return stub.stream(new StreamObserver<StreamResponse>() {
            @Override
            public void onNext(StreamResponse response) {
                try {
                    if (response.hasAck()) {
                        StreamAck ack = response.getAck();
                        StreamInferenceResponse sseResponse = new StreamInferenceResponse();
                        sseResponse.setSuccess(ack.getStatus().getCode() == 0);
                        sseResponse.setMessage("ACK: " + ack.getStatus().getMessage());
                        sseResponse.setFrameIndex(0);
                        
                        emitter.send(SseEmitter.event()
                                .name("ack")
                                .data(sseResponse));
                    } else if (response.hasFrame()) {
                        FrameResult result = response.getFrame();
                        StreamInferenceResponse sseResponse = processFrameResult(result);
                        
                        emitter.send(SseEmitter.event()
                                .name("frame")
                                .data(sseResponse));
                    }
                } catch (Exception e) {
                    try {
                        emitter.send(SseEmitter.event()
                                .name("error")
                                .data(new StreamInferenceResponse(false, "SSE send error: " + e.getMessage())));
                    } catch (Exception sendError) {
                        // 忽略发送错误
                    }
                }
            }
            
            @Override
            public void onError(Throwable t) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data(new StreamInferenceResponse(false, "gRPC stream error: " + t.getMessage())));
                } catch (Exception e) {
                    // 忽略发送错误
                }
                finishLatch.countDown();
            }
            
            @Override
            public void onCompleted() {
                try {
                    emitter.send(SseEmitter.event()
                            .name("completed")
                            .data(new StreamInferenceResponse(true, "Stream completed")));
                } catch (Exception e) {
                    // 忽略发送错误
                }
                finishLatch.countDown();
            }
        });
    }
    
    private StreamObserver<StreamRequest> createReactiveObserver(
            InferenceStreamServiceGrpc.InferenceStreamServiceStub stub,
            FluxSink<StreamInferenceResponse> sink,
            CountDownLatch finishLatch,
            AtomicInteger frameCount) {
        
        return stub.stream(new StreamObserver<StreamResponse>() {
            @Override
            public void onNext(StreamResponse response) {
                if (response.hasAck()) {
                    StreamAck ack = response.getAck();
                    StreamInferenceResponse reactiveResponse = new StreamInferenceResponse();
                    reactiveResponse.setSuccess(ack.getStatus().getCode() == 0);
                    reactiveResponse.setMessage("ACK: " + ack.getStatus().getMessage());
                    reactiveResponse.setFrameIndex(0);
                    
                    sink.next(reactiveResponse);
                } else if (response.hasFrame()) {
                    StreamInferenceResponse reactiveResponse = processFrameResult(response.getFrame());
                    sink.next(reactiveResponse);
                }
            }
            
            @Override
            public void onError(Throwable t) {
                sink.next(new StreamInferenceResponse(false, "gRPC stream error: " + t.getMessage()));
                finishLatch.countDown();
            }
            
            @Override
            public void onCompleted() {
                sink.next(new StreamInferenceResponse(true, "Stream completed"));
                finishLatch.countDown();
            }
        });
    }
    
    private void sendStreamRequest(StreamObserver<StreamRequest> requestObserver, StreamInferenceRequest request) throws Exception {
        // 构建推理头部
        InferenceHeader header = buildInferenceHeader(request);
        
        // 发送流打开请求
        requestObserver.onNext(StreamRequest.newBuilder()
                .setOpen(StreamOpen.newBuilder().setHeader(header).build())
                .build());
        
        // 发送图像帧
        if (request.getImage() != null && !request.getImage().trim().isEmpty()) {
            byte[] imageBytes = Base64.getDecoder().decode(request.getImage());
            
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
            
            requestObserver.onNext(StreamRequest.newBuilder().setFrame(frame).build());
        }
        
        // 等待处理
        Thread.sleep(1000);
        
        // 发送流关闭请求
        requestObserver.onNext(StreamRequest.newBuilder()
                .setClose(StreamClose.newBuilder().build())
                .build());
        requestObserver.onCompleted();
    }
    
    private void sendVideoStreamRequest(StreamObserver<StreamRequest> requestObserver, StreamInferenceRequest request) throws Exception {
        // 构建推理头部
        InferenceHeader header = buildInferenceHeader(request);
        
        // 发送流打开请求
        requestObserver.onNext(StreamRequest.newBuilder()
                .setOpen(StreamOpen.newBuilder().setHeader(header).build())
                .build());
        
        // 发送视频帧
        if (request.getVideo() != null && !request.getVideo().trim().isEmpty()) {
            byte[] videoBytes = Base64.getDecoder().decode(request.getVideo());
            
            InputEnvelope input = InputEnvelope.newBuilder()
                    .setKind("video")
                    .setContentType("video/mp4")
                    .setBinary(ByteString.copyFrom(videoBytes))
                    .build();
            
            StreamFrame frame = StreamFrame.newBuilder()
                    .addInputs(input)
                    .setFrameIndex(0)
                    .setTs(Timestamps.fromMillis(System.currentTimeMillis()))
                    .build();
            
            requestObserver.onNext(StreamRequest.newBuilder().setFrame(frame).build());
        }
        
        // 等待处理
        Thread.sleep(2000);
        
        // 发送流关闭请求
        requestObserver.onNext(StreamRequest.newBuilder()
                .setClose(StreamClose.newBuilder().build())
                .build());
        requestObserver.onCompleted();
    }
    
    private void sendBatchStreamRequest(StreamObserver<StreamRequest> requestObserver, StreamInferenceRequest request) throws Exception {
        // 构建推理头部
        InferenceHeader header = buildInferenceHeader(request);
        
        // 发送流打开请求
        requestObserver.onNext(StreamRequest.newBuilder()
                .setOpen(StreamOpen.newBuilder().setHeader(header).build())
                .build());
        
        // 发送批量图像帧
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            for (int i = 0; i < request.getImages().size(); i++) {
                String imageBase64 = request.getImages().get(i);
                if (imageBase64 != null && !imageBase64.trim().isEmpty()) {
                    byte[] imageBytes = Base64.getDecoder().decode(imageBase64);
                    
                    InputEnvelope input = InputEnvelope.newBuilder()
                            .setKind("image")
                            .setContentType("image/jpeg")
                            .setBinary(ByteString.copyFrom(imageBytes))
                            .build();
                    
                    StreamFrame frame = StreamFrame.newBuilder()
                            .addInputs(input)
                            .setFrameIndex(i)
                            .setTs(Timestamps.fromMillis(System.currentTimeMillis()))
                            .build();
                    
                    requestObserver.onNext(StreamRequest.newBuilder().setFrame(frame).build());
                    
                    // 每帧之间稍作延迟
                    Thread.sleep(500);
                }
            }
        }
        
        // 等待处理
        Thread.sleep(2000);
        
        // 发送流关闭请求
        requestObserver.onNext(StreamRequest.newBuilder()
                .setClose(StreamClose.newBuilder().build())
                .build());
        requestObserver.onCompleted();
    }
    
    private InferenceHeader buildInferenceHeader(StreamInferenceRequest request) {
        return HeaderUtils.buildHeaderFrom(request);
    }
    
    private StreamInferenceResponse processFrameResult(FrameResult frameResult) {
        StreamInferenceResponse response = new StreamInferenceResponse();
        List<DetectionResult> detections = new ArrayList<>();
        
        // 处理检测结果
        for (ResultEnvelope envelope : frameResult.getResultsList()) {
            if ("detections".equals(envelope.getKind()) && envelope.hasMessage()) {
                // 解析检测结果
                try {
                    Struct struct = envelope.getMessage().unpack(Struct.class);
                    if (struct.containsFields("items")) {
                        Value itemsValue = struct.getFieldsOrThrow("items");
                        if (itemsValue.hasListValue()) {
                            for (Value item : itemsValue.getListValue().getValuesList()) {
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
        response.setMessage("Frame processed successfully");
        response.setDetections(detections);
        response.setFrameIndex(Math.toIntExact(frameResult.getFrameIndex()));

        return response;
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
