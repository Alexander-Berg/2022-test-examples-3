package ru.yandex.http.server.async;

import java.io.IOException;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.nio.protocol.HttpAsyncExchange;
import org.apache.http.nio.protocol.HttpAsyncRequestConsumer;
import org.apache.http.nio.protocol.HttpAsyncRequestHandler;
import org.apache.http.protocol.HttpContext;

import ru.yandex.http.util.BadRequestException;
import ru.yandex.json.async.consumer.JsonAsyncTypesafeDomConsumer;
import ru.yandex.json.dom.BasicContainerFactory;
import ru.yandex.json.dom.JsonObject;
import ru.yandex.json.parser.StringCollectorsFactory;
import ru.yandex.json.writer.JsonType;

public enum JsonNormalizingHandler
    implements HttpAsyncRequestHandler<JsonObject>
{
    INSTANCE;

    @Override
    public HttpAsyncRequestConsumer<JsonObject> processRequest(
        final HttpRequest request,
        final HttpContext context)
        throws HttpException
    {
        if (!(request instanceof HttpEntityEnclosingRequest)) {
            throw new BadRequestException("Payload expected");
        }
        return new JsonAsyncTypesafeDomConsumer(
            ((HttpEntityEnclosingRequest) request).getEntity(),
            StringCollectorsFactory.INSTANCE,
            BasicContainerFactory.INSTANCE);
    }

    @Override
    public void handle(
        final JsonObject request,
        final HttpAsyncExchange exchange,
        final HttpContext context)
        throws IOException
    {
        exchange.getResponse().setEntity(
            new NStringEntity(
                JsonType.NORMAL.toString(request),
                ContentType.APPLICATION_JSON));
        exchange.submitResponse();
    }
}

