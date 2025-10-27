package com.duola.grpc_java.client;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.google.protobuf.util.Timestamps;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.ai.*;
import io.grpc.stub.StreamObserver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class NacosDiscoveryGrpcClient {

    private static final String OUTPUT_DIR = "output";

    public static void main(String[] args) throws Exception {
        // 固定按用户提供的路径发送一次图片和一次视频
        String imagePath = "/Users/duola/Documents/test.png";
        String videoPath = "/Users/duola/LearningNote/63_1754729347.mp4";

        // Nacos 连接信息（可通过 JVM 参数覆盖）
        String serverAddr = System.getProperty("serverAddr", "127.0.0.1:8848");
        String namespace = System.getProperty("nacosNamespace", "");
        String username = System.getProperty("nacosUsername", "nacos");
        String password = System.getProperty("nacosPassword", "nacos");
        String group = System.getProperty("nacosGroup", "DEFAULT_GROUP");

        Instance instance = discoverInstance(serverAddr, namespace, username, password, group, "yolo-detection-nano");
        if (instance == null) {
            System.err.println("No healthy instance found for service: yolo-detection-nano");
            return;
        }

        String host = instance.getIp();
        int port = instance.getPort();
        System.out.println("Discovered yolo-detection-nano -> " + host + ":" + port);

        // 确保输出目录存在
        try {
            Files.createDirectories(new File(OUTPUT_DIR).toPath());
        } catch (IOException e) {
            System.err.println("Failed to create output directory: " + e.getMessage());
        }

        ManagedChannel channel = ManagedChannelBuilder
                .forAddress(host, port)
                .usePlaintext()
                .maxInboundMessageSize(64 * 1024 * 1024)
                .build();

        try {
            InferenceStreamServiceGrpc.InferenceStreamServiceStub stub = InferenceStreamServiceGrpc.newStub(channel);

            // 发送图片
            sendSingleImage(stub, imagePath);

            // 发送视频（二进制作为一个输入，具体解码由服务端决定）
            sendSingleVideoBinary(stub, videoPath);
        } finally {
            channel.shutdownNow();
            System.out.println("Client shutdown.");
        }
    }

    private static Instance discoverInstance(String serverAddr, String namespace, String username, String password,
                                              String group, String serviceName) {
        try {
            Properties props = new Properties();
            props.setProperty("serverAddr", serverAddr);
            if (!namespace.isEmpty()) props.setProperty("namespace", namespace);
            props.setProperty("username", username);
            props.setProperty("password", password);

            NamingService namingService = NamingFactory.createNamingService(props);
            // 选择一个健康实例（可根据需要读取权重、metadata 等）
            return namingService.selectOneHealthyInstance(serviceName, group);
        } catch (NacosException e) {
            System.err.println("Nacos discovery failed: " + e.getMessage());
            return null;
        }
    }

    private static void sendSingleImage(InferenceStreamServiceGrpc.InferenceStreamServiceStub stub, String imagePath)
            throws InterruptedException {
        File file = new File(imagePath);
        if (!file.exists()) {
            System.err.println("Image file not found: " + imagePath);
            return;
        }

        CountDownLatch finishLatch = new CountDownLatch(1);
        AtomicInteger frameCount = new AtomicInteger(0);
        StreamObserver<StreamRequest> requestObserver = createResponsePrintingObserver(stub, finishLatch, frameCount);

        try {
            byte[] imageBytes = Files.readAllBytes(file.toPath());
            String contentType = guessImageContentType(file.getName());

            // Header（模型名用服务名，版本固定 v1；按需修改）
            InferenceHeader header = buildHeader("yolo-detection-nano");

            // open
            requestObserver.onNext(StreamRequest.newBuilder()
                    .setOpen(StreamOpen.newBuilder().setHeader(header).build())
                    .build());

            // frame
            InputEnvelope input = InputEnvelope.newBuilder()
                    .setKind("image")
                    .setContentType(contentType)
                    .setBinary(ByteString.copyFrom(imageBytes))
                    .build();

            StreamFrame frame = StreamFrame.newBuilder()
                    .addInputs(input)
                    .setFrameIndex(0)
                    .setTs(Timestamps.fromMillis(System.currentTimeMillis()))
                    .build();

            requestObserver.onNext(StreamRequest.newBuilder().setFrame(frame).build());

            // close
            requestObserver.onNext(StreamRequest.newBuilder()
                    .setClose(StreamClose.newBuilder().build()).build());
            requestObserver.onCompleted();

            if (!finishLatch.await(30, TimeUnit.SECONDS)) {
                System.err.println("Image request timed out");
            }
        } catch (IOException e) {
            System.err.println("Read image error: " + e.getMessage());
        }
    }

    private static void sendSingleVideoBinary(InferenceStreamServiceGrpc.InferenceStreamServiceStub stub, String videoPath)
            throws InterruptedException {
        File file = new File(videoPath);
        if (!file.exists()) {
            System.err.println("Video file not found: " + videoPath);
            return;
        }

        // 注意：一次性发送整个视频文件可能较大，需确保 gRPC 与服务端允许的消息大小足够。
        CountDownLatch finishLatch = new CountDownLatch(1);
        AtomicInteger frameCount = new AtomicInteger(0);
        StreamObserver<StreamRequest> requestObserver = createResponsePrintingObserver(stub, finishLatch, frameCount);

        try {
            byte[] videoBytes = Files.readAllBytes(file.toPath());

            InferenceHeader header = buildHeader("yolo-detection-nano");

            // open
            requestObserver.onNext(StreamRequest.newBuilder()
                    .setOpen(StreamOpen.newBuilder().setHeader(header).build())
                    .build());

            // 将整段视频作为一个输入，由服务端自行解码
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

            // close
            requestObserver.onNext(StreamRequest.newBuilder()
                    .setClose(StreamClose.newBuilder().build()).build());
            requestObserver.onCompleted();

            if (!finishLatch.await(120, TimeUnit.SECONDS)) {
                System.err.println("Video request timed out");
            }
        } catch (IOException e) {
            System.err.println("Read video error: " + e.getMessage());
        }
    }

    private static InferenceHeader buildHeader(String modelName) {
        Struct.Builder optionsBuilder = Struct.newBuilder();
        optionsBuilder.putFields("target_w", Value.newBuilder().setNumberValue(640).build());
        optionsBuilder.putFields("target_h", Value.newBuilder().setNumberValue(640).build());
        optionsBuilder.putFields("keep_aspect_ratio", Value.newBuilder().setBoolValue(true).build());
        optionsBuilder.putFields("mode", Value.newBuilder().setStringValue("detect").build());
        optionsBuilder.putFields("persist", Value.newBuilder().setBoolValue(false).build());
        optionsBuilder.putFields("return_overlay", Value.newBuilder().setBoolValue(true).build());

        return InferenceHeader.newBuilder()
                .setModel(ModelSpec.newBuilder().setName(modelName).setVersion("v1").build())
                .setOptions(optionsBuilder.build())
                .addAccept("application/x-protobuf;type=\"google.protobuf.Struct\"")
                .addAccept("image/jpeg;kind=\"overlay\"")
                .build();
    }

    private static StreamObserver<StreamRequest> createResponsePrintingObserver(
            InferenceStreamServiceGrpc.InferenceStreamServiceStub stub,
            CountDownLatch finishLatch,
            AtomicInteger frameCount) {

        return stub.stream(new StreamObserver<StreamResponse>() {
            @Override
            public void onNext(StreamResponse response) {
                if (response.hasAck()) {
                    StreamAck ack = response.getAck();
                    System.out.println("ACK: code=" + ack.getStatus().getCode() + ", msg=" + ack.getStatus().getMessage());
                } else if (response.hasFrame()) {
                    FrameResult result = response.getFrame();
                    System.out.println("FrameResult index=" + result.getFrameIndex());
                    if (result.hasMeta()) {
                        System.out.println("Meta: " + result.getMeta());
                    }
                    for (ResultEnvelope envelope : result.getResultsList()) {
                        System.out.println("- kind=" + envelope.getKind() + ", ct=" + envelope.getContentType() + ", idx=" + envelope.getInputIndex());
                        if (envelope.hasMessage()) {
                            parseDetectionMessage(envelope.getMessage());
                        } else if (envelope.hasBinary()) {
                            System.out.println("  binary bytes=" + envelope.getBinary().size());
                            // 保存可视化结果：图片或视频
                            String ct = envelope.getContentType();
                            if (isImageContentType(ct)) {
                                String ext = imageExtensionForContentType(ct);
                                String filename = OUTPUT_DIR + "/result_frame_" + result.getFrameIndex() + ext;
                                saveBinaryToFile(envelope.getBinary(), filename);
                                System.out.println("  saved image overlay => " + filename);
                            } else if (isVideoContentType(ct)) {
                                String filename = OUTPUT_DIR + "/result_video_" + result.getFrameIndex() + ".mp4";
                                saveBinaryToFile(envelope.getBinary(), filename);
                                System.out.println("  saved video => " + filename);
                            }
                        }
                    }
                }
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("Stream error: " + t.getMessage());
                finishLatch.countDown();
            }

            @Override
            public void onCompleted() {
                System.out.println("Stream completed.");
                finishLatch.countDown();
            }
        });
    }

    private static void parseDetectionMessage(Any message) {
        try {
            Struct struct = message.unpack(Struct.class);
            if (struct.containsFields("items")) {
                Value itemsValue = struct.getFieldsOrThrow("items");
                if (itemsValue.hasListValue()) {
                    List<Value> items = itemsValue.getListValue().getValuesList();
                    System.out.println("Detections: " + items.size());
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to parse detection message: " + e.getMessage());
        }
    }

    private static String guessImageContentType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".bmp")) return "image/bmp";
        return "image/jpeg"; // 默认
    }

    private static boolean isImageContentType(String contentType) {
        if (contentType == null) return false;
        String ct = contentType.toLowerCase();
        return ct.startsWith("image/") || ct.contains("kind=\"overlay\"");
    }

    private static boolean isVideoContentType(String contentType) {
        if (contentType == null) return false;
        String ct = contentType.toLowerCase();
        return ct.startsWith("video/");
    }

    private static String imageExtensionForContentType(String contentType) {
        String ct = contentType == null ? "" : contentType.toLowerCase();
        if (ct.contains("png")) return ".png";
        if (ct.contains("bmp")) return ".bmp";
        return ".jpg";
    }

    private static void saveBinaryToFile(ByteString data, String path) {
        try {
            Files.write(new File(path).toPath(), data.toByteArray());
        } catch (IOException e) {
            System.err.println("Failed to save file: " + path + ", reason: " + e.getMessage());
        }
    }
}


