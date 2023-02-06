package ru.yandex.http.test;

import java.io.IOException;

import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

public class HeaderHttpItem implements HttpRequestHandler {
    private final HttpRequestHandler handler;
    private final Header header;

    public HeaderHttpItem(
        final HttpRequestHandler handler,
        final String name,
        final String value)
    {
        this(handler, new BasicHeader(name, value));
    }

    public HeaderHttpItem(
        final HttpRequestHandler handler,
        final Header header)
    {
        this.handler = handler;
        this.header = header;
    }

    @Override
    public void handle(
        final HttpRequest request,
        final HttpResponse response,
        final HttpContext context)
        throws HttpException, IOException
    {
        handler.handle(request, response, context);
        response.addHeader(header);
    }
}

