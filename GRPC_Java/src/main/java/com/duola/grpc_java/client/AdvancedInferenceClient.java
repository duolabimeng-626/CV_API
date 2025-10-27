package com.duola.grpc_java.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.ai.*;
import io.grpc.stub.StreamObserver;

import com.google.protobuf.util.Timestamps;
import com.google.protobuf.ByteString;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class AdvancedInferenceClient {

    private static final String OUTPUT_DIR = "output";

    public static void main(String[] args) throws InterruptedException {
        // 检查命令行参数
        if (args.length < 1) {
            System.err.println("Usage: java AdvancedInferenceClient <image_path|directory> [server_host] [server_port]");
            System.err.println("Example: java AdvancedInferenceClient /path/to/image.jpg localhost 50051");
            System.err.println("Example: java AdvancedInferenceClient /path/to/images/ localhost 50051");
            return;
        }

        String inputPath = args[0];
        String serverHost = args.length > 1 ? args[1] : "localhost";
        int serverPort = args.length > 2 ? Integer.parseInt(args[2]) : 50051;

        System.out.println("=== Advanced YOLO Inference Client ===");
        System.out.println("Server: " + serverHost + ":" + serverPort);
        System.out.println("Input: " + inputPath);

        // 创建输出目录
        try {
            Files.createDirectories(Paths.get(OUTPUT_DIR));
        } catch (IOException e) {
            System.err.println("Failed to create output directory: " + e.getMessage());
            return;
        }

        ManagedChannel channel = ManagedChannelBuilder
                .forAddress(serverHost, serverPort)
                .usePlaintext()
                .maxInboundMessageSize(32 * 1024 * 1024) // 32MB
                .build();

        try {
            InferenceStreamServiceGrpc.InferenceStreamServiceStub stub =
                    InferenceStreamServiceGrpc.newStub(channel);

            File inputFile = new File(inputPath);
            if (inputFile.isFile()) {
                // 处理单个文件
                processSingleImage(stub, inputFile);
            } else if (inputFile.isDirectory()) {
                // 处理目录中的所有图像文件
                processImageDirectory(stub, inputFile);
            } else {
                System.err.println("Input path is neither a file nor a directory: " + inputPath);
            }

        } finally {
            channel.shutdownNow();
            System.out.println("Client shutdown.");
        }
    }

    private static void processSingleImage(InferenceStreamServiceGrpc.InferenceStreamServiceStub stub,
                                           File imageFile) throws InterruptedException {
        System.out.println("\n=== Processing Single Image ===");
        System.out.println("Image: " + imageFile.getName());

        CountDownLatch finishLatch = new CountDownLatch(1);
        AtomicInteger frameCount = new AtomicInteger(0);

        StreamObserver<StreamRequest> requestObserver = createRequestObserver(stub, finishLatch, frameCount);

        try {
            byte[] imageBytes = Files.readAllBytes(imageFile.toPath());
            System.out.println("Loaded image: " + imageBytes.length + " bytes");

            // 发送流
            sendInferenceRequest(requestObserver, imageBytes, 0);

            // 等待完成
            if (!finishLatch.await(30, TimeUnit.SECONDS)) {
                System.err.println("Request timed out");
            }

        } catch (IOException e) {
            System.err.println("Error reading image file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void processImageDirectory(InferenceStreamServiceGrpc.InferenceStreamServiceStub stub,
                                              File directory) throws InterruptedException {
        System.out.println("\n=== Processing Image Directory ===");

        File[] imageFiles = directory.listFiles((dir, name) -> {
            String lower = name.toLowerCase();
            return lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
                    lower.endsWith(".png") || lower.endsWith(".bmp");
        });

        if (imageFiles == null || imageFiles.length == 0) {
            System.err.println("No image files found in directory: " + directory.getPath());
            return;
        }

        System.out.println("Found " + imageFiles.length + " image files");

        for (int i = 0; i < imageFiles.length; i++) {
            File imageFile = imageFiles[i];
            System.out.println("\n--- Processing " + (i + 1) + "/" + imageFiles.length + ": " + imageFile.getName());

            CountDownLatch finishLatch = new CountDownLatch(1);
            AtomicInteger frameCount = new AtomicInteger(0);

            StreamObserver<StreamRequest> requestObserver = createRequestObserver(stub, finishLatch, frameCount);

            try {
                byte[] imageBytes = Files.readAllBytes(imageFile.toPath());
                System.out.println("Loaded image: " + imageBytes.length + " bytes");

                // 发送流
                sendInferenceRequest(requestObserver, imageBytes, i);

                // 等待完成
                if (!finishLatch.await(30, TimeUnit.SECONDS)) {
                    System.err.println("Request timed out for " + imageFile.getName());
                }

                // 短暂休息，避免服务器过载
                Thread.sleep(500);

            } catch (IOException e) {
                System.err.println("Error reading image file " + imageFile.getName() + ": " + e.getMessage());
            }
        }
    }

    private static StreamObserver<StreamRequest> createRequestObserver(
            InferenceStreamServiceGrpc.InferenceStreamServiceStub stub,
            CountDownLatch finishLatch,
            AtomicInteger frameCount) {

        return stub.stream(new StreamObserver<StreamResponse>() {
            @Override
            public void onNext(StreamResponse response) {
                if (response.hasAck()) {
                    StreamAck ack = response.getAck();
                    System.out.println("Received ACK: " + ack.getStatus().getMessage());
                    if (ack.getStatus().getCode() != 0) {
                        System.err.println("Error code: " + ack.getStatus().getCode());
                    }
                } else if (response.hasFrame()) {
                    FrameResult result = response.getFrame();
                    System.out.println("\n=== Detection Results ===");
                    System.out.println("Frame index: " + result.getFrameIndex());

                    // 打印元数据
                    if (result.hasMeta()) {
                        System.out.println("Metadata:");
                        result.getMeta().getFieldsMap().forEach((key, value) -> {
                            System.out.println("  " + key + ": " + value);
                        });
                    }

                    // 处理检测结果
                    processDetectionResults(result);
                }
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("Stream error: " + t.getMessage());
                t.printStackTrace();
                finishLatch.countDown();
            }

            @Override
            public void onCompleted() {
                System.out.println("Stream completed.");
                finishLatch.countDown();
            }
        });
    }

    private static void processDetectionResults(FrameResult result) {
        for (ResultEnvelope envelope : result.getResultsList()) {
            System.out.println("\nResult Envelope:");
            System.out.println("  Kind: " + envelope.getKind());
            System.out.println("  Content Type: " + envelope.getContentType());
            System.out.println("  Input Index: " + envelope.getInputIndex());

            if (envelope.hasMessage()) {
                // 解析检测结果
                parseDetectionMessage(envelope.getMessage());
            } else if (envelope.hasBinary()) {
                // 保存叠加图像
                saveOverlayImage(envelope.getBinary(), result.getFrameIndex());
            }
        }
    }

    private static void parseDetectionMessage(Any message) {
        try {
            Struct struct = message.unpack(Struct.class);
            System.out.println("  Detection Results:");

            if (struct.containsFields("items")) {
                com.google.protobuf.Value itemsValue = struct.getFieldsOrThrow("items");
                if (itemsValue.hasListValue()) {
                    List<com.google.protobuf.Value> items = itemsValue.getListValue().getValuesList();
                    System.out.println("    Found " + items.size() + " detections:");

                    for (int i = 0; i < items.size(); i++) {
                        com.google.protobuf.Value item = items.get(i);
                        if (item.hasStructValue()) {
                            Struct itemStruct = item.getStructValue();
                            System.out.println("    Detection " + (i + 1) + ":");

                            // 提取检测信息
                            if (itemStruct.containsFields("class_name")) {
                                String className = itemStruct.getFieldsOrThrow("class_name").getStringValue();
                                System.out.println("      Class: " + className);
                            }

                            if (itemStruct.containsFields("score")) {
                                double score = itemStruct.getFieldsOrThrow("score").getNumberValue();
                                System.out.println("      Score: " + String.format("%.3f", score));
                            }

                            if (itemStruct.containsFields("x") && itemStruct.containsFields("y") &&
                                    itemStruct.containsFields("width") && itemStruct.containsFields("height")) {
                                double x = itemStruct.getFieldsOrThrow("x").getNumberValue();
                                double y = itemStruct.getFieldsOrThrow("y").getNumberValue();
                                double width = itemStruct.getFieldsOrThrow("width").getNumberValue();
                                double height = itemStruct.getFieldsOrThrow("height").getNumberValue();
                                System.out.println("      BBox: (" + String.format("%.1f", x) + ", " +
                                        String.format("%.1f", y) + ", " +
                                        String.format("%.1f", width) + ", " +
                                        String.format("%.1f", height) + ")");
                            }

                            if (itemStruct.containsFields("track_id")) {
                                double trackId = itemStruct.getFieldsOrThrow("track_id").getNumberValue();
                                System.out.println("      Track ID: " + (int)trackId);
                            }
                        }
                    }
                }
            }
        } catch (InvalidProtocolBufferException e) {
            System.err.println("Failed to parse detection message: " + e.getMessage());
        }
    }

    private static void saveOverlayImage(ByteString binaryData, long frameIndex) {
        try {
            String filename = OUTPUT_DIR + "/overlay_frame_" + frameIndex + ".jpg";
            try (FileOutputStream fos = new FileOutputStream(filename)) {
                fos.write(binaryData.toByteArray());
                System.out.println("  Saved overlay image: " + filename + " (" + binaryData.size() + " bytes)");
            }
        } catch (IOException e) {
            System.err.println("Failed to save overlay image: " + e.getMessage());
        }
    }

    private static void sendInferenceRequest(StreamObserver<StreamRequest> requestObserver,
                                             byte[] imageBytes, int frameIndex) {
        // 构建推理头部，包含配置选项
        Struct.Builder optionsBuilder = Struct.newBuilder();
        optionsBuilder.putFields("target_w", Value.newBuilder().setNumberValue(640).build());
        optionsBuilder.putFields("target_h", Value.newBuilder().setNumberValue(640).build());
        optionsBuilder.putFields("keep_aspect_ratio", Value.newBuilder().setBoolValue(true).build());
        optionsBuilder.putFields("mode", Value.newBuilder().setStringValue("detect").build());
        optionsBuilder.putFields("persist", Value.newBuilder().setBoolValue(false).build());
        optionsBuilder.putFields("return_overlay", Value.newBuilder().setBoolValue(true).build());

        InferenceHeader header = InferenceHeader.newBuilder()
                .setModel(ModelSpec.newBuilder()
                        .setName("yolov8n")
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
        System.out.println("Sent StreamOpen");

        // 发送图像帧
        InputEnvelope input = InputEnvelope.newBuilder()
                .setKind("image")
                .setContentType("image/jpeg")
                .setBinary(ByteString.copyFrom(imageBytes))
                .build();

        StreamFrame frame = StreamFrame.newBuilder()
                .addInputs(input)
                .setFrameIndex(frameIndex)
                .setTs(Timestamps.fromMillis(System.currentTimeMillis()))
                .build();

        StreamRequest frameReq = StreamRequest.newBuilder()
                .setFrame(frame)
                .build();

        requestObserver.onNext(frameReq);
        System.out.println("Sent frame " + frameIndex + " with " + imageBytes.length + " bytes");

        // 等待一段时间让服务器处理
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 发送流关闭请求
        StreamRequest closeReq = StreamRequest.newBuilder()
                .setClose(StreamClose.newBuilder().build())
                .build();

        requestObserver.onNext(closeReq);
        System.out.println("Sent StreamClose");

        requestObserver.onCompleted();
    }
}
