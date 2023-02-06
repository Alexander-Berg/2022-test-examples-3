package ru.yandex.http.test;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

public class NotImplementedHttpItem implements HttpRequestHandler {
    public static final NotImplementedHttpItem INSTANCE =
        new NotImplementedHttpItem();

    protected NotImplementedHttpItem() {
    }

    @Override
    public void handle(
        final HttpRequest request,
        final HttpResponse response,
        final HttpContext context)
    {
        response.setStatusCode(HttpStatus.SC_NOT_IMPLEMENTED);
    }
}

