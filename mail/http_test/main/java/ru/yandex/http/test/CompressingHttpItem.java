package ru.yandex.http.test;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.entity.GzipCompressingEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

public class CompressingHttpItem extends ExpectingHeaderHttpItem {
    public CompressingHttpItem(final HttpRequestHandler handler) {
        super(
            handler,
            new BasicHeader(
                HttpHeaders.ACCEPT_ENCODING,
                "gzip,deflate"));
    }

    @Override
    public void handle(
        final HttpRequest request,
        final HttpResponse response,
        final HttpContext context)
        throws HttpException, IOException
    {
        super.handle(request, response, context);
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            response.setEntity(new GzipCompressingEntity(entity));
        }
    }
}
