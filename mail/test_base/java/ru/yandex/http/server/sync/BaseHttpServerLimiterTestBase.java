package ru.yandex.http.server.sync;

import java.io.IOException;

import org.apache.http.protocol.HttpRequestHandler;

import ru.yandex.http.test.HttpServerLimiterTest;
import ru.yandex.http.test.SlowpokeHttpItem;
import ru.yandex.http.test.StaticHttpItem;
import ru.yandex.http.util.server.ImmutableBaseServerConfig;

public abstract class BaseHttpServerLimiterTestBase
    extends HttpServerLimiterTest<HttpRequestHandler>
{
    @Override
    protected BaseHttpServer<ImmutableBaseServerConfig> createServer(
        final ImmutableBaseServerConfig config)
        throws IOException
    {
        return new BaseHttpServer<>(config);
    }

    @Override
    protected HttpRequestHandler createDummyHandler(final int status) {
        return new StaticHttpItem(status);
    }

    @Override
    protected HttpRequestHandler createDummyHandler(final String response) {
        return new StaticHttpItem(response);
    }

    @Override
    protected HttpRequestHandler createSlowpokeHandler(
        final HttpRequestHandler next,
        final long delay)
    {
        return new SlowpokeHttpItem(next, delay);
    }

    @Override
    protected HttpRequestHandler createJsonNormalizingHandler() {
        return JsonNormalizingHandler.INSTANCE;
    }
}

