package ru.yandex.http.test;

import java.io.IOException;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

public class SlowpokeHttpItem implements HttpRequestHandler {
    private final HttpRequestHandler handler;
    private final long delay;

    public SlowpokeHttpItem(
        final HttpRequestHandler handler,
        final long delay)
    {
        this.handler = handler;
        this.delay = delay;
    }

    @Override
    public void handle(
        final HttpRequest request,
        final HttpResponse response,
        final HttpContext context)
        throws HttpException, IOException
    {
        handler.handle(request, response, context);
        // CSOFF: EmptyBlock
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
        }
        // CSON: EmptyBlock
    }
}

