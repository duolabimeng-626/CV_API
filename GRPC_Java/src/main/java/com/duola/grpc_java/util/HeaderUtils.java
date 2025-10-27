package com.duola.grpc_java.util;

import com.duola.grpc_java.dto.StreamInferenceRequest;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import io.grpc.ai.InferenceHeader;
import io.grpc.ai.ModelSpec;

public final class HeaderUtils {

    private HeaderUtils() {}

    public static InferenceHeader buildHeaderFrom(StreamInferenceRequest request) {
        Struct.Builder optionsBuilder = Struct.newBuilder();
        optionsBuilder.putFields("target_w", Value.newBuilder().setNumberValue(request.getTargetWidth()).build());
        optionsBuilder.putFields("target_h", Value.newBuilder().setNumberValue(request.getTargetHeight()).build());
        optionsBuilder.putFields("keep_aspect_ratio", Value.newBuilder().setBoolValue(request.getKeepAspectRatio()).build());
        optionsBuilder.putFields("mode", Value.newBuilder().setStringValue(request.getMode()).build());
        optionsBuilder.putFields("persist", Value.newBuilder().setBoolValue(false).build());
        optionsBuilder.putFields("return_overlay", Value.newBuilder().setBoolValue(request.getReturnOverlay()).build());

        return InferenceHeader.newBuilder()
                .setModel(ModelSpec.newBuilder()
                        .setName(request.getModel())
                        .setVersion("v1")
                        .build())
                .setOptions(optionsBuilder.build())
                .addAccept("application/x-protobuf;type=\"google.protobuf.Struct\"")
                .addAccept("image/jpeg;kind=\"overlay\"")
                .build();
    }
}


