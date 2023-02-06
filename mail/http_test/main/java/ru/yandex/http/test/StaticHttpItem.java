package ru.yandex.http.test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

public class StaticHttpItem implements HttpRequestHandler {
    public static final StaticHttpItem OK =
        new StaticHttpItem(HttpStatus.SC_OK);
    public static final StaticHttpItem BAD_REQUEST =
        new StaticHttpItem(HttpStatus.SC_BAD_REQUEST);
    public static final StaticHttpItem NOT_FOUND =
        new StaticHttpItem(HttpStatus.SC_NOT_FOUND);

    private final int statusCode;
    private final HttpEntity entity;
    private List<Header> headers = null;

    public StaticHttpItem(final int statusCode) {
        this(statusCode, (HttpEntity) null);
    }

    public StaticHttpItem(final String body) {
        this(HttpStatus.SC_OK, body);
    }

    public StaticHttpItem(final int statusCode, final String body) {
        this(
            statusCode,
            new StringEntity(
                body,
                ContentType.TEXT_PLAIN.withCharset(StandardCharsets.UTF_8)));
    }

    public StaticHttpItem(final int statusCode, final byte[] body) {
        this(statusCode, new ByteArrayEntity(body));
    }

    public StaticHttpItem(final int statusCode, final HttpEntity entity) {
        this.statusCode = statusCode;
        this.entity = entity;
    }

    public StaticHttpItem addHeader(final String name, final String value) {
        if (headers == null) {
            headers = new ArrayList<>();
        }
        headers.add(new BasicHeader(name, value));
        return this;
    }

    @Override
    public void handle(
        final HttpRequest request,
        final HttpResponse response,
        final HttpContext context)
        throws HttpException, IOException
    {
        response.setStatusCode(statusCode);
        response.setEntity(entity);
        if (headers != null) {
            for (Header header : headers) {
                response.addHeader(header);
            }
        }
    }
}

