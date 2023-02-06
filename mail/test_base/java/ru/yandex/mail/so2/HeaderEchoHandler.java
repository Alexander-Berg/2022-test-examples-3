package ru.yandex.mail.so2;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import ru.yandex.http.util.BadRequestException;
import ru.yandex.http.util.ByteArrayEntityFactory;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.http.util.YandexHeaders;

public enum HeaderEchoHandler implements HttpRequestHandler {
    INSTANCE;

    @Override
    public void handle(
        final HttpRequest request,
        final HttpResponse response,
        final HttpContext context)
        throws HttpException, IOException
    {
        if (request instanceof HttpEntityEnclosingRequest) {
            response.addHeader(
                request.getFirstHeader(So2HttpServer.SO2_DATA_HEADER));
            response.addHeader(
                YandexHeaders.URI,
                request.getRequestLine().getUri());
            HttpEntity entity =
                ((HttpEntityEnclosingRequest) request).getEntity();
            ByteArrayEntity responseEntity =
                CharsetUtils.toDecodable(entity).processWith(
                    ByteArrayEntityFactory.INSTANCE);
            responseEntity.setContentType(entity.getContentType());
            response.setEntity(responseEntity);
        } else {
            throw new BadRequestException("Payload expected");
        }
    }
}

