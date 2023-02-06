package ru.yandex.http.test;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.protocol.HttpRequestHandler;

public class ChainedHttpResource extends AbstractHttpResource {
    private final AtomicInteger accessCount = new AtomicInteger();
    private final HttpRequestHandler[] handlers;
    private int position = 0;

    public ChainedHttpResource(final HttpRequestHandler... handlers) {
        this.handlers = handlers;
    }

    @Override
    public synchronized HttpRequestHandler next() {
        if (position < handlers.length) {
            return new CountingHttpRequestHandler(
                handlers[position++],
                accessCount);
        } else {
            accessCount.set(Integer.MIN_VALUE);
            return null;
        }
    }

    @Override
    public int accessCount() {
        return accessCount.get();
    }
}

