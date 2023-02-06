package ru.yandex.http.test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import ru.yandex.http.util.server.AbstractHttpServer;
import ru.yandex.logger.PrefixedLogger;

public class CountingHttpRequestHandler implements HttpRequestHandler {
    private final HttpRequestHandler handler;
    private final AtomicInteger count;

    public CountingHttpRequestHandler(
        final HttpRequestHandler handler,
        final AtomicInteger count)
    {
        this.handler = handler;
        this.count = count;
    }

    @Override
    public void handle(
        final HttpRequest request,
        final HttpResponse response,
        final HttpContext context)
        throws HttpException, IOException
    {
        PrefixedLogger logger =
            (PrefixedLogger) context.getAttribute(AbstractHttpServer.LOGGER);
        try {
            handler.handle(request, response, context);
        } catch (Throwable t) {
            count.set(Integer.MIN_VALUE);
            throw t;
        }
        int count = this.count.incrementAndGet();
        logger.fine("Access count incremented to " + count);
    }

    @Override
    public String toString() {
        return handler.toString();
    }
}

