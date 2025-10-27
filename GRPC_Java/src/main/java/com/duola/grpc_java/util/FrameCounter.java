package com.duola.grpc_java.util;

import java.util.concurrent.atomic.AtomicInteger;

public final class FrameCounter {
    private final AtomicInteger counter = new AtomicInteger(0);

    public int incrementAndGet() {
        return counter.incrementAndGet();
    }

    public int get() {
        return counter.get();
    }

    public void reset() {
        counter.set(0);
    }
}


