package ru.yandex.http.test;

import java.util.List;

import org.apache.http.protocol.HttpRequestHandler;

public class SlowpokeHttpResource implements HttpResource {
    private final HttpResource resource;
    private final long delay;

    public SlowpokeHttpResource(
        final HttpRequestHandler handler,
        final long delay)
    {
        this(new StaticHttpResource(handler), delay);
    }

    public SlowpokeHttpResource(
        final HttpResource resource,
        final long delay)
    {
        this.resource = resource;
        this.delay = delay;
    }

    @Override
    public HttpRequestHandler next() {
        HttpRequestHandler handler = resource.next();
        if (handler != null) {
            handler = new SlowpokeHttpItem(handler, delay);
        }
        return handler;
    }

    @Override
    public int accessCount() {
        return resource.accessCount();
    }

    @Override
    public List<Throwable> exceptions() {
        return resource.exceptions();
    }

    @Override
    public void exception(final Throwable t) {
        resource.exception(t);
    }
}

