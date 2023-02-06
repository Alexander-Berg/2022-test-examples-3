package ru.yandex.http.server.async;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.concurrent.Cancellable;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.nio.protocol.HttpAsyncRequestHandler;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.collection.Pattern;
import ru.yandex.http.proxy.ProxyRequestHandler;
import ru.yandex.http.proxy.ProxyRequestHandlerAdapter;
import ru.yandex.http.proxy.ProxySession;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.test.HttpServerTest;
import ru.yandex.http.util.server.ImmutableBaseServerConfig;

public abstract class BaseAsyncServerTestBase
    extends HttpServerTest<HttpAsyncRequestHandler<?>>
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

    @Test
    public void testSetCallbackAfterResponse() throws Exception {
        try (CloseableHttpClient client =
                Configs.createDefaultClient(clientBc());
            BaseAsyncServer<ImmutableBaseServerConfig> server =
                createServer(defaultConfig))
        {
            AtomicBoolean failedFlag = new AtomicBoolean();
            server.register(
                new Pattern<>(URI, false),
                new ProxyRequestHandlerAdapter(
                    new SetCallbackAfterResponseHandler(failedFlag),
                    server));
            server.start();
            for (int i = 0; i < 100 && !failedFlag.get(); ++i) {
                try (CloseableHttpResponse response =
                        client.execute(new HttpGet(server.host() + URI)))
                {
                    HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                }
            }
            Assert.assertFalse(failedFlag.get());
        }
    }

    private static class SetCallbackAfterResponseHandler
        implements Cancellable,
            ProxyRequestHandler,
            Thread.UncaughtExceptionHandler
    {
        private final AtomicBoolean failedFlag;

        public SetCallbackAfterResponseHandler(
            final AtomicBoolean failedFlag)
        {
            this.failedFlag = failedFlag;
        }

        @Override
        public void handle(final ProxySession session) {
            final Object lock = new Object();
            final AtomicBoolean flag = new AtomicBoolean();
            Thread t1 = new Thread() {
                @Override
                public void run() {
                    synchronized (lock) {
                        while (!flag.get()) {
                            try {
                                lock.wait();
                            } catch (InterruptedException e) {
                            }
                        }
                    }
                    session.response(HttpStatus.SC_OK);
                }
            };
            t1.setDaemon(true);
            t1.setUncaughtExceptionHandler(this);
            Thread t2 = new Thread() {
                @Override
                public void run() {
                    synchronized (lock) {
                        while (!flag.get()) {
                            try {
                                lock.wait();
                            } catch (InterruptedException e) {
                            }
                        }
                    }
                    session.subscribeForCancellation(
                        SetCallbackAfterResponseHandler.this);
                }
            };
            t2.setUncaughtExceptionHandler(this);
            t2.setDaemon(true);
            t1.start();
            t2.start();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
            synchronized (lock) {
                flag.set(true);
                lock.notifyAll();
            }
        }

        @Override
        public boolean cancel() {
            return true;
        }

        @Override
        public void uncaughtException(final Thread t, final Throwable e) {
            System.err.println("Thread " + t + " thrown unhandled exception");
            e.printStackTrace();
            failedFlag.set(true);
        }
    }
}

