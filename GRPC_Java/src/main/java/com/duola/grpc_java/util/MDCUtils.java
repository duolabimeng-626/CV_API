package com.duola.grpc_java.util;

import io.grpc.ai.InferenceHeader;
import io.grpc.ai.ModelSpec;
import io.grpc.ai.TenantContext;
import io.grpc.ai.TraceContext;
import org.slf4j.MDC;

import java.util.UUID;

public final class MDCUtils {

    private MDCUtils() {}

    public static void populateFromHeader(InferenceHeader header) {
        if (header == null) return;
        if (header.hasTrace()) {
            TraceContext trace = header.getTrace();
            putIfNotEmpty("trace_id", trace.getTraceId());
            putIfNotEmpty("span_id", trace.getSpanId());
            putIfNotEmpty("parent_span_id", trace.getParentSpanId());
        }
        if (header.hasTenant()) {
            TenantContext tenant = header.getTenant();
            putIfNotEmpty("tenant_id", tenant.getTenantId());
            putIfNotEmpty("user_id", tenant.getUserId());
        }
        if (header.hasModel()) {
            ModelSpec model = header.getModel();
            putIfNotEmpty("model_name", model.getName());
            putIfNotEmpty("model_version", model.getVersion());
        }
    }

    public static String ensureTraceId() {
        String traceId = MDC.get("trace_id");
        if (traceId == null || traceId.isEmpty()) {
            traceId = UUID.randomUUID().toString();
            MDC.put("trace_id", traceId);
        }
        return traceId;
    }

    public static void clear() {
        MDC.clear();
    }

    private static void putIfNotEmpty(String key, String value) {
        if (value != null && !value.isEmpty()) {
            MDC.put(key, value);
        }
    }
}


