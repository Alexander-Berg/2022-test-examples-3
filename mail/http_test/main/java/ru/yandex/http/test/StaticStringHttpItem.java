package ru.yandex.http.test;

import java.io.IOException;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import ru.yandex.http.util.CharsetUtils;

public class StaticStringHttpItem implements HttpRequestHandler {
    private final int statusCode;
    private final String body;

    public StaticStringHttpItem(final String body) {
        this(HttpStatus.SC_OK, body);
    }

    public StaticStringHttpItem(final int statusCode, final String body) {
        this.statusCode = statusCode;
        this.body = body;
    }

    @Override
    public void handle(
        final HttpRequest request,
        final HttpResponse response,
        final HttpContext context)
        throws HttpException, IOException
    {
        response.setStatusCode(statusCode);
        response.setEntity(
            new StringEntity(
                body,
                ContentType.TEXT_PLAIN.withCharset(
                    CharsetUtils.acceptedCharset(request))));
    }
}

