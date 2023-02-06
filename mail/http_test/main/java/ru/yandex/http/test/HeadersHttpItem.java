package ru.yandex.http.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

public class HeadersHttpItem implements HttpRequestHandler {
    private final HttpRequestHandler requestHandler;
    private final List<Header> headers;

    public HeadersHttpItem(
        final HttpRequestHandler requestHandler,
        final List<Header> headers)
    {
        this.requestHandler = requestHandler;
        this.headers = headers;
    }

    public HeadersHttpItem(
        final HttpRequestHandler handler,
        final String... headers)
    {
        assert headers.length % 2 == 0;

        this.requestHandler = handler;
        this.headers = new ArrayList<>();
        for (int i = 0; i < headers.length; i += 2) {
            this.headers.add(new BasicHeader(headers[i], headers[i + 1]));
        }
    }

    @Override
    public void handle(
        final HttpRequest request,
        final HttpResponse response,
        final HttpContext context)
        throws HttpException, IOException
    {
        if (requestHandler != null) {
            requestHandler.handle(request, response, context);
        }

        for (Header header: headers) {
            response.setHeader(header);
        }
    }
}
