package com.duola.grpc_java.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.ai.*;
import io.grpc.stub.StreamObserver;

import com.google.protobuf.util.Timestamps;
import com.google.protobuf.ByteString;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class InferenceClient {

    public static void main(String[] args) throws InterruptedException {
        // 检查命令行参数
        if (args.length < 1) {
            System.err.println("Usage: java InferenceClient <image_path> [server_host] [server_port]");
            System.err.println("Example: java InferenceClient /path/to/image.jpg localhost 50051");
            return;
        }

        String imagePath = args[0];
        String serverHost = args.length > 1 ? args[1] : "localhost";
        int serverPort = args.length > 2 ? Integer.parseInt(args[2]) : 50051;

        System.out.println("Connecting to gRPC server at " + serverHost + ":" + serverPort);
        System.out.println("Image path: " + imagePath);

        ManagedChannel channel = ManagedChannelBuilder
                .forAddress(serverHost, serverPort)
                .usePlaintext()
                .maxInboundMessageSize(32 * 1024 * 1024) // 32MB
                .build();

        InferenceStreamServiceGrpc.InferenceStreamServiceStub stub = InferenceStreamServiceGrpc.newStub(channel);

        CountDownLatch finishLatch = new CountDownLatch(1);

        StreamObserver<StreamRequest> requestObserver = stub.stream(new StreamObserver<StreamResponse>() {
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
                    System.out.println("\n=== Frame Result ===");
                    System.out.println("Frame index: " + result.getFrameIndex());

                    // 打印元数据
                    if (result.hasMeta()) {
                        System.out.println("Metadata:");
                        result.getMeta().getFieldsMap().forEach((key, value) -> {
                            System.out.println("  " + key + ": " + value);
                        });
                    }

                    // 处理检测结果
                    for (ResultEnvelope envelope : result.getResultsList()) {
                        System.out.println("\nResult Envelope:");
                        System.out.println("  Kind: " + envelope.getKind());
                        System.out.println("  Content Type: " + envelope.getContentType());
                        System.out.println("  Input Index: " + envelope.getInputIndex());

                        if (envelope.hasMessage()) {
                            System.out.println("  Has detection message");
                            // 这里可以进一步解析检测结果
                        } else if (envelope.hasBinary()) {
                            System.out.println("  Has binary data (overlay image)");
                            System.out.println("  Binary size: " + envelope.getBinary().size() + " bytes");
                        }
                    }
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

        try {
            // 读取图像文件
            File imageFile = new File(imagePath);
            if (!imageFile.exists()) {
                System.err.println("Image file not found: " + imagePath);
                return;
            }

            byte[] imageBytes = Files.readAllBytes(imageFile.toPath());
            System.out.println("Loaded image: " + imageBytes.length + " bytes");

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
                    .setFrameIndex(0)
                    .setTs(Timestamps.fromMillis(System.currentTimeMillis()))
                    .build();

            StreamRequest frameReq = StreamRequest.newBuilder()
                    .setFrame(frame)
                    .build();

            requestObserver.onNext(frameReq);
            System.out.println("Sent frame with " + imageBytes.length + " bytes");

            // 等待一段时间让服务器处理
            Thread.sleep(2000);

            // 发送流关闭请求
            StreamRequest closeReq = StreamRequest.newBuilder()
                    .setClose(StreamClose.newBuilder().build())
                    .build();

            requestObserver.onNext(closeReq);
            System.out.println("Sent StreamClose");

            requestObserver.onCompleted();

            // 等待响应完成
            if (!finishLatch.await(10, TimeUnit.SECONDS)) {
                System.err.println("Request timed out");
            }

        } catch (IOException e) {
            System.err.println("Error reading image file: " + e.getMessage());
            e.printStackTrace();
        } finally {
            channel.shutdownNow();
            System.out.println("Client shutdown.");
        }
    }
}
