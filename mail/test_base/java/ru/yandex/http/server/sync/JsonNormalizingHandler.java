package ru.yandex.http.server.sync;

import java.io.IOException;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import ru.yandex.http.util.BadRequestException;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.http.util.ServiceUnavailableException;
import ru.yandex.json.dom.JsonObject;
import ru.yandex.json.dom.TypesafeValueContentHandler;
import ru.yandex.json.parser.JsonException;
import ru.yandex.json.writer.JsonTypeExtractor;

public enum JsonNormalizingHandler implements HttpRequestHandler {
    INSTANCE;

    @Override
    public void handle(
        final HttpRequest request,
        final HttpResponse response,
        final HttpContext context)
        throws HttpException, IOException
    {
        if (!(request instanceof HttpEntityEnclosingRequest)) {
            throw new BadRequestException("Payload expected");
        }

        try {
            JsonObject object =
                TypesafeValueContentHandler.parse(
                    CharsetUtils.content(
                        ((HttpEntityEnclosingRequest) request)
                            .getEntity()));
            response.setEntity(
                new JsonEntity(object, request, JsonTypeExtractor.NORMAL));
        } catch (JsonException e) {
            throw new ServiceUnavailableException(e);
        }
    }
}

