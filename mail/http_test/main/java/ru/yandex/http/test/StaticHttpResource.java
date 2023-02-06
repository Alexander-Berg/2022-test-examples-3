package ru.yandex.http.test;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.HttpEntity;
import org.apache.http.protocol.HttpRequestHandler;

public class StaticHttpResource extends AbstractHttpResource {
    public static final StaticHttpResource OK =
        new StaticHttpResource(StaticHttpItem.OK);

    private final AtomicInteger accessCount = new AtomicInteger();
    private final HttpRequestHandler handler;

    public StaticHttpResource(final int statusCode) {
        this(statusCode, null);
    }

    public StaticHttpResource(final int statusCode, final HttpEntity entity) {
        this(new StaticHttpItem(statusCode, entity));
    }

    public StaticHttpResource(final HttpRequestHandler handler) {
        this.handler = handler;
    }

    @Override
    public HttpRequestHandler next() {
        return new CountingHttpRequestHandler(handler, accessCount);
    }

    @Override
    public int accessCount() {
        return accessCount.get();
    }
}

