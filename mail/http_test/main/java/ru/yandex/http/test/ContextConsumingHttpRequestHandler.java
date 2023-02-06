package ru.yandex.http.test;

import java.io.IOException;
import java.util.function.Consumer;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

public class ContextConsumingHttpRequestHandler implements HttpRequestHandler {
    private final HttpRequestHandler handler;
    private final Consumer<HttpContext> consumer;

    public ContextConsumingHttpRequestHandler(
        final HttpRequestHandler handler,
        final Consumer<HttpContext> consumer)
    {
        this.handler = handler;
        this.consumer = consumer;
    }

    @Override
    public void handle(
        final HttpRequest request,
        final HttpResponse response,
        final HttpContext context)
        throws HttpException, IOException
    {
        consumer.accept(context);
        handler.handle(request, response, context);
    }

    @Override
    public String toString() {
        return handler.toString();
    }
}

