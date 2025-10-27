package com.duola.grpc_java.server.imp;

import io.grpc.ai.*;
import io.grpc.stub.StreamObserver;


import com.google.protobuf.Struct;
import com.google.protobuf.Timestamp;
import com.google.protobuf.Value;
import com.google.protobuf.util.Timestamps;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import com.duola.grpc_java.util.MDCUtils;

import java.util.concurrent.atomic.AtomicBoolean;

public class InferenceStreamServiceImpl extends InferenceStreamServiceGrpc.InferenceStreamServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(InferenceStreamServiceImpl.class);

    @Override
    public StreamObserver<StreamRequest> stream(StreamObserver<StreamResponse> responseObserver) {
        return new StreamObserver<StreamRequest>() {
            final AtomicBoolean opened = new AtomicBoolean(false);
            InferenceHeader header = null;
            long recvFrames = 0L;

            @Override
            public void onNext(StreamRequest request) {
                try {
                    // Populate MDC from header for tracing/tenant
                    if (request.hasOpen() && request.getOpen().hasHeader()) {
                        MDCUtils.populateFromHeader(request.getOpen().getHeader());
                    } else if (header != null) {
                        MDCUtils.populateFromHeader(header);
                    }

                    if (request.hasOpen()) {
                        handleOpen(request.getOpen(), responseObserver);
                    } else if (request.hasFrame()) {
                        handleFrame(request.getFrame(), responseObserver);
                    } else if (request.hasClose()) {
                        handleClose(request.getClose(), responseObserver);
                    } else {
                        sendAck(responseObserver, 3, "Unknown event in StreamRequest"); // INVALID_ARGUMENT
                    }
                } catch (Exception e) {
                    logger.error("Server error while handling stream request", e);
                    sendAck(responseObserver, 13, "Server error: " + e.getMessage()); // INTERNAL
                    responseObserver.onCompleted();
                } finally {
                    MDCUtils.clear();
                }
            }

            @Override
            public void onError(Throwable t) {
                try {
                    logger.error("Client stream error: {}", t.getMessage(), t);
                } finally {
                    MDCUtils.clear();
                    responseObserver.onCompleted();
                }
            }

            @Override
            public void onCompleted() {
                try {
                    logger.info("Client completed sending. Total frames received: {}", recvFrames);
                } finally {
                    MDCUtils.clear();
                    responseObserver.onCompleted();
                }
            }

            private void handleOpen(StreamOpen open, StreamObserver<StreamResponse> out) {
                if (opened.get()) {
                    sendAck(out, 9, "Stream already opened"); // FAILED_PRECONDITION
                    return;
                }
                header = open.getHeader();
                opened.set(true);

                String modelName = header.hasModel() ? header.getModel().getName() : "(unknown)";
                String modelVer  = header.hasModel() ? header.getModel().getVersion() : "(unknown)";
                logger.info("StreamOpen received. Model = {} / {}", modelName, modelVer);

                sendAck(out, 0, "Stream opened"); // OK
            }

            private void handleFrame(StreamFrame frame, StreamObserver<StreamResponse> out) {
                if (!opened.get()) {
                    sendAck(out, 9, "Stream not opened yet"); // FAILED_PRECONDITION
                    return;
                }
                recvFrames++;

                long frameIndex = frame.getFrameIndex();
                Timestamp ts = frame.hasTs() ? frame.getTs() : Timestamps.fromMillis(System.currentTimeMillis());
                logger.info("Frame received: index={}, inputs={}, ts={}",
                        frameIndex, frame.getInputsCount(), Timestamps.toString(ts));

                FrameResult.Builder frameResult = FrameResult.newBuilder()
                        .setFrameIndex(frameIndex)
                        .setMeta(Struct.newBuilder()
                                .putFields("runtime_ms", Value.newBuilder().setNumberValue(12.3).build())
                                .putFields("notes", Value.newBuilder().setStringValue("demo inference").build())
                                .build());

                for (int i = 0; i < frame.getInputsCount(); i++) {
                    InputEnvelope in = frame.getInputs(i);

                    Struct.Builder json = Struct.newBuilder()
                            .putFields("input_kind", Value.newBuilder().setStringValue(in.getKind()).build())
                            .putFields("content_type", Value.newBuilder().setStringValue(in.getContentType()).build())
                            .putFields("index_in_frame", Value.newBuilder().setNumberValue(i).build());

                    if (in.hasText()) {
                        json.putFields("text_len", Value.newBuilder().setNumberValue(in.getText().length()).build());
                        json.putFields("preview", Value.newBuilder().setStringValue(
                                in.getText().length() > 32 ? in.getText().substring(0, 32) + "..." : in.getText()
                        ).build());
                    } else if (in.hasBinary()) {
                        json.putFields("binary_size", Value.newBuilder().setNumberValue(in.getBinary().size()).build());
                    } else if (in.hasJson()) {
                        json.putFields("json_keys", Value.newBuilder().setNumberValue(in.getJson().getFieldsCount()).build());
                    } else if (in.hasMessage()) {
                        json.putFields("message_any", Value.newBuilder().setStringValue(in.getMessage().getTypeUrl()).build());
                    }

                    Struct.Builder meta = Struct.newBuilder()
                            .putFields("echo_tags", Value.newBuilder().setStringValue(in.getTagsMap().toString()).build());

                    ResultEnvelope result = ResultEnvelope.newBuilder()
                            .setKind("echo")
                            .setContentType("application/json")
                            .setJson(json.build())
                            .setMeta(meta.build())
                            .setInputIndex(i)
                            .build();

                    frameResult.addResults(result);
                }

                StreamResponse resp = StreamResponse.newBuilder()
                        .setFrame(frameResult.build())
                        .build();

                out.onNext(resp);
            }

            private void handleClose(StreamClose close, StreamObserver<StreamResponse> out) {
                logger.info("StreamClose received. meta={}", (close.hasMeta() ? close.getMeta() : "{}"));
                sendAck(out, 0, "Stream closed"); // OK
            }

            private void sendAck(StreamObserver<StreamResponse> out, int code, String message) {
                CustomStatus status = CustomStatus.newBuilder()
                        .setCode(code)
                        .setMessage(message)
                        .build();

                StreamAck ack = StreamAck.newBuilder()
                        .setStatus(status)
                        .build();

                StreamResponse resp = StreamResponse.newBuilder()
                        .setAck(ack)
                        .build();

                out.onNext(resp);
            }
        };
    }

    // MDC population moved to MDCUtils
}

