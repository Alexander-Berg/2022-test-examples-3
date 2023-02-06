package ru.yandex.http.test;

import java.io.IOException;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import ru.yandex.function.GenericConsumer;
import ru.yandex.http.util.NotImplementedException;

public class ValidatingHttpItem implements HttpRequestHandler {
    private final HttpRequestHandler handler;
    private final GenericConsumer<? super HttpRequest, ? extends Exception>
        validator;

    public ValidatingHttpItem(
        final HttpRequestHandler handler,
        final GenericConsumer<? super HttpRequest, ? extends Exception>
        validator)
    {
        this.handler = handler;
        this.validator = validator;
    }

    @Override
    public void handle(
        final HttpRequest request,
        final HttpResponse response,
        final HttpContext context)
        throws HttpException, IOException
    {
        try {
            validator.accept(request);
        } catch (Throwable t) {
            throw new NotImplementedException("Validation failed", t);
        }
        handler.handle(request, response, context);
    }
}

