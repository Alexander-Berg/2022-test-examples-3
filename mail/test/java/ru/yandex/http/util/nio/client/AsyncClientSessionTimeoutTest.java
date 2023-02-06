package ru.yandex.http.util.nio.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.http.config.HttpTargetConfigBuilder;
import ru.yandex.http.test.ChainedHttpResource;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.test.StaticServer;
import ru.yandex.http.util.EmptyFutureCallback;
import ru.yandex.http.util.nio.BasicAsyncRequestProducerGenerator;
import ru.yandex.http.util.nio.BasicAsyncResponseConsumerFactory;
import ru.yandex.http.util.server.BaseServerConfigBuilder;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

public class AsyncClientSessionTimeoutTest extends TestBase {
    @Test
    public void testSessionTimeout() throws Exception {
        BaseServerConfigBuilder serverConfig =
            new BaseServerConfigBuilder(Configs.baseConfig("TestServer"))
                .connections(2);

        final int clientTimeout = 5000;
        final long responseDelay = 3000;
        final int requestDelay = 200;
        final int requests = 3;
        final String uriBase = "/test";
        final String uriClient = uriBase + "?num=";
        final String uriServer = uriBase + '*';

        HttpTargetConfigBuilder clientConfig =
            new HttpTargetConfigBuilder(Configs.targetConfig())
                .connections(requests)
                .timeout(clientTimeout);

        // first check that without sessionTimeout things are not good
        try (StaticServer testServer = new StaticServer(serverConfig.build());
            SharedConnectingIOReactor reactor = new SharedConnectingIOReactor(
                Configs.baseConfig(),
                Configs.dnsConfig());
            AsyncClient client =
                new AsyncClient(reactor, clientConfig.build()))
        {
            client.start();
            reactor.start();
            testServer.start();

            testServer.add(
                uriServer,
                new ChainedHttpResource(
                    new DelayedHttpHandler(responseDelay),
                    new DelayedHttpHandler(responseDelay),
                    new DelayedHttpHandler(responseDelay)));

            List<Future<HttpResponse>> futures = new ArrayList<>();

            for (int i = 0; i < requests; i++) {
                futures.add(
                    client.execute(
                        testServer.host(),
                        new BasicAsyncRequestProducerGenerator(uriClient + i),
                        BasicAsyncResponseConsumerFactory.INSTANCE,
                        client.httpClientContextGenerator(),
                        EmptyFutureCallback.INSTANCE));
                Thread.sleep(requestDelay);
            }

            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                futures.get(0).get());
            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                futures.get(1).get());
            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                futures.get(2).get());
        }

        clientConfig.sessionTimeout(clientTimeout);

        try (StaticServer testServer = new StaticServer(serverConfig.build());
            SharedConnectingIOReactor reactor = new SharedConnectingIOReactor(
                Configs.baseConfig(),
                Configs.dnsConfig());
            AsyncClient client =
                new AsyncClient(reactor, clientConfig.build()))
        {
            client.start();
            reactor.start();
            testServer.start();

            testServer.add(
                uriServer,
                new ChainedHttpResource(
                    new DelayedHttpHandler(responseDelay),
                    new DelayedHttpHandler(responseDelay),
                    new DelayedHttpHandler(responseDelay)));

            List<Future<HttpResponse>> futures = new ArrayList<>();

            for (int i = 0; i < requests; i++) {
                futures.add(
                    client.execute(
                        testServer.host(),
                        new BasicAsyncRequestProducerGenerator(uriClient + i),
                        BasicAsyncResponseConsumerFactory.INSTANCE,
                        client.httpClientContextGenerator(),
                        EmptyFutureCallback.INSTANCE));
                Thread.sleep(requestDelay);
            }

            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                futures.get(0).get());
            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                futures.get(1).get());

            try {
                futures.get(2).get();
                Assert.fail();
            } catch (ExecutionException e) {
                YandexAssert.assertInstanceOf(
                    SocketTimeoutException.class,
                    e.getCause());
            }
        }
    }

    private static final class DelayedHttpHandler
        implements HttpRequestHandler
    {
        private final long delay;

        private DelayedHttpHandler(final long delay) {
            this.delay = delay;
        }

        @Override
        public void handle(
            final HttpRequest request,
            final HttpResponse response,
            final HttpContext context)
            throws HttpException, IOException
        {
            response.setStatusCode(HttpStatus.SC_OK);
            BasicHttpEntity entity = new BasicHttpEntity() {
                @Override
                public void writeTo(final OutputStream out)
                    throws IOException
                {
                    out.flush();
                    super.writeTo(out);
                }
            };
            entity.setContent(
                new SlowpokeOutputStream(
                    new ByteArrayInputStream(
                        "ResponseMessage".getBytes(StandardCharsets.UTF_8)),
                    delay));
            //entity.setChunked(true);
            response.setEntity(entity);
            response.addHeader("FirstHeader", "FirstLongHeader");
            response.addHeader("SecondHeader", "SecondLongHeader");
        }
    }

    @SuppressWarnings("InputStreamSlowMultibyteRead")
    private static final class SlowpokeOutputStream extends InputStream {
        private final long delay;
        private final InputStream wrapped;
        private volatile boolean first = true;

        private SlowpokeOutputStream(
            final InputStream wrapped,
            final long delay)
        {
            this.delay = delay;
            this.wrapped = wrapped;
        }

        @Override
        public int read() throws IOException {
            if (first) {
                try {
                    Thread.sleep(delay);
                    first = false;
                } catch (InterruptedException ie) {
                    throw new IOException(ie);
                }
            }

            return wrapped.read();
        }
    }
}
