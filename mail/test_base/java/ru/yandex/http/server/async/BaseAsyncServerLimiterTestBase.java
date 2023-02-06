package ru.yandex.http.server.async;

import java.io.IOException;

import org.apache.http.HttpStatus;
import org.apache.http.nio.protocol.HttpAsyncRequestHandler;

import ru.yandex.http.test.HttpServerLimiterTest;
import ru.yandex.http.util.server.ImmutableBaseServerConfig;

public abstract class BaseAsyncServerLimiterTestBase
    extends HttpServerLimiterTest<HttpAsyncRequestHandler<?>>
{
    @Override
    protected BaseAsyncServer<ImmutableBaseServerConfig> createServer(
        final ImmutableBaseServerConfig config)
        throws IOException
    {
        return new BaseAsyncServer<>(config);
    }

    @Override
    protected HttpAsyncRequestHandler<?> createDummyHandler(final int status) {
        return new StaticHandler(status, null);
    }

    @Override
    protected HttpAsyncRequestHandler<?> createDummyHandler(
        final String response)
    {
        return new StaticHandler(HttpStatus.SC_OK, response);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected HttpAsyncRequestHandler<?> createSlowpokeHandler(
        final HttpAsyncRequestHandler<?> next,
        final long delay)
    {
        return (HttpAsyncRequestHandler<?>) new SlowpokeHandler(next, delay);
    }

    @Override
    protected HttpAsyncRequestHandler<?> createJsonNormalizingHandler() {
        return JsonNormalizingHandler.INSTANCE;
    }
}

