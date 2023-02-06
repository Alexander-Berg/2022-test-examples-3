package ru.yandex.http.server.async;

import org.apache.http.HttpRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.nio.protocol.BasicAsyncRequestConsumer;
import org.apache.http.nio.protocol.HttpAsyncExchange;
import org.apache.http.nio.protocol.HttpAsyncRequestConsumer;
import org.apache.http.nio.protocol.HttpAsyncRequestHandler;
import org.apache.http.protocol.HttpContext;

public class StaticHandler implements HttpAsyncRequestHandler<HttpRequest> {
    private final int status;
    private final String response;

    StaticHandler(
        final int status,
        final String response)
    {
        this.status = status;
        this.response = response;
    }

    @Override
    public HttpAsyncRequestConsumer<HttpRequest> processRequest(
        final HttpRequest request,
        final HttpContext context)
    {
        return new BasicAsyncRequestConsumer();
    }

    @Override
    public void handle(
        final HttpRequest request,
        final HttpAsyncExchange exchange,
        final HttpContext context)
    {
        exchange.getResponse().setStatusCode(status);
        if (response != null) {
            exchange.getResponse().setEntity(
                new NStringEntity(response, ContentType.TEXT_PLAIN));
        }
        exchange.submitResponse();
    }
}

