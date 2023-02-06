package ru.yandex.http.util.nio.client;

import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import com.google.common.collect.ImmutableMap;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.entity.ContentProducer;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.EntityTemplate;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.nio.protocol.HttpAsyncRequestProducer;
import org.apache.http.nio.reactor.IOReactorStatus;
import org.apache.http.protocol.HttpCoreContext;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.function.CountingSupplier;
import ru.yandex.function.GenericAutoCloseable;
import ru.yandex.http.config.HttpTargetConfigBuilder;
import ru.yandex.http.config.ImmutableDnsConfig;
import ru.yandex.http.config.ImmutableHttpTargetConfig;
import ru.yandex.http.config.RetriesConfigBuilder;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.ExpectingHeaderHttpItem;
import ru.yandex.http.test.ExpectingHttpItem;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.test.HttpClientTest;
import ru.yandex.http.test.SlowpokeHttpItem;
import ru.yandex.http.test.SlowpokeHttpResource;
import ru.yandex.http.test.StaticHttpItem;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.http.test.StaticServer;
import ru.yandex.http.util.BadResponseException;
import ru.yandex.http.util.EmptyFutureCallback;
import ru.yandex.http.util.HeaderUtils;
import ru.yandex.http.util.YandexHeaders;
import ru.yandex.http.util.nio.BasicAsyncRequestProducerGenerator;
import ru.yandex.http.util.nio.BasicAsyncResponseConsumerFactory;
import ru.yandex.http.util.server.ImmutableBaseServerConfig;
import ru.yandex.test.util.YandexAssert;

class AsyncClientWithReactor implements GenericAutoCloseable<IOException> {
    private final SharedConnectingIOReactor reactor;
    private final AsyncClient client;

    AsyncClientWithReactor(
        final ImmutableBaseServerConfig serverConfig,
        final ImmutableHttpTargetConfig backendConfig,
        final ImmutableDnsConfig dnsConfig)
        throws Exception
    {
        reactor = new SharedConnectingIOReactor(serverConfig, dnsConfig);
        client = new AsyncClient(reactor, backendConfig);
    }

    public void start() {
        reactor.start();
        client.start();
    }

    public SharedConnectingIOReactor reactor() {
        return reactor;
    }

    public AsyncClient client() {
        return client;
    }

    @Override
    @SuppressWarnings("try")
    public void close() throws IOException {
        try (SharedConnectingIOReactor reactor = this.reactor) {
            client.close();
        }
    }
}

public abstract class AsyncClientTestBase
    extends HttpClientTest<AsyncClientWithReactor>
{
    private static final int SMOL_TIMEOUT = 300;
    private static final String STATUS = "status";
    private static final String PENDING_POOLS = "pending_pools";
    private static final String ACTIVE_CONNECTIONS = "active_connections";
    private static final String PENDING_CONNECTIONS = "pending_connections";
    private static final String AVAILABLE_CONNECTIONS =
        "available_connections";

    @Override
    protected AsyncClientWithReactor createClient(
        final ImmutableHttpTargetConfig backendConfig,
        final ImmutableDnsConfig dnsConfig)
        throws Exception
    {
        AsyncClientWithReactor client =
            new AsyncClientWithReactor(config(), backendConfig, dnsConfig);
        client.start();
        return client;
    }

    @Override
    protected void sendRequest(
        final AsyncClientWithReactor client,
        final int expectedStatus,
        final HttpUriRequest request)
        throws Exception
    {
        Supplier<HttpAsyncRequestProducer> requestSupplier;
        if (request instanceof HttpEntityEnclosingRequest) {
            requestSupplier = new AsyncPostURIRequestProducerSupplier(
                request.getURI(),
                ((HttpEntityEnclosingRequest) request).getEntity());
        } else {
            requestSupplier =
                new AsyncGetURIRequestProducerSupplier(request.getURI());
        }
        try {
            HttpResponse response =
                client.client().execute(
                    requestSupplier,
                    BasicAsyncResponseConsumerFactory.ANY_GOOD,
                    EmptyFutureCallback.INSTANCE)
                    .get();
            int status = response.getStatusLine().getStatusCode();
            if (status != expectedStatus) {
                throw new BadResponseException(request, response);
            }
        } catch (ExecutionException e) {
            throw (Exception) e.getCause();
        }
    }

    @Override
    protected void testIoRetriesPostCheck(final AsyncClientWithReactor client)
        throws Exception
    {
        Assert.assertEquals(
            new ImmutableMap.Builder<String, Object>()
                .put(STATUS, IOReactorStatus.ACTIVE)
                .put(PENDING_POOLS, 0)
                .build(),
            client.client().status(true));
        Assert.assertEquals(
            Collections.emptyMap(),
            client.reactor().dnsResolver().status(true));
    }

    @Test
    public void testPoolTimeout() throws Exception {
        try (StaticServer server = new StaticServer(config());
            SharedConnectingIOReactor reactor =
                new SharedConnectingIOReactor(config(), Configs.dnsConfig());
            AsyncClient client =
                new AsyncClient(
                    reactor,
                    new HttpTargetConfigBuilder(Configs.targetConfig())
                        .connections(1)
                        .timeout(TIMEOUT)
                        .poolTimeout(TIMEOUT >> 1)
                        .build()))
        {
            server.start();
            reactor.start();
            client.start();
            server.add(
                URI,
                new SlowpokeHttpResource(
                    new StaticHttpResource(HttpStatus.SC_OK),
                    TIMEOUT << 1));
            Future<HttpResponse> future = client.execute(
                server.host(),
                new BasicAsyncRequestProducerGenerator(URI),
                BasicAsyncResponseConsumerFactory.OK,
                EmptyFutureCallback.INSTANCE);
            Thread.sleep(SMOL_TIMEOUT);
            Future<HttpResponse> second = client.execute(
                server.host(),
                new BasicAsyncRequestProducerGenerator(URI),
                BasicAsyncResponseConsumerFactory.OK,
                EmptyFutureCallback.INSTANCE);
            try {
                future.get();
                Assert.fail();
            } catch (ExecutionException e) {
                YandexAssert.assertInstanceOf(
                    SocketTimeoutException.class,
                    e.getCause());
            }
            Thread.sleep(TIMEOUT << 1);
            Assert.assertEquals(1, server.accessCount(URI));
            try {
                second.get();
                Assert.fail();
            } catch (ExecutionException e) {
                YandexAssert.assertInstanceOf(
                    TimeoutException.class,
                    e.getCause());
            }
            Assert.assertEquals(
                new ImmutableMap.Builder<String, Object>()
                    .put(STATUS, IOReactorStatus.ACTIVE)
                    .put(PENDING_POOLS, 0)
                    .build(),
                client.status(true));
            Assert.assertEquals(
                Collections.emptyMap(),
                reactor.dnsResolver().status(true));
        }
    }

    @Test
    public void testIoRetriesOnStoppedServer() throws Exception {
        try (SharedConnectingIOReactor reactor =
                new SharedConnectingIOReactor(config(), Configs.dnsConfig());
            AsyncClient client =
                new AsyncClient(
                    reactor,
                    new HttpTargetConfigBuilder(Configs.targetConfig())
                        .timeout(TIMEOUT)
                        .ioRetries(new RetriesConfigBuilder().count(RETRIES))
                        .build()))
        {
            reactor.start();
            client.start();
            Thread.sleep(TIMEOUT);
            final int httpPort = 80;
            Future<HttpResponse> future = client.execute(
                new HttpHost("nobody.will.ever.register.this", httpPort),
                new BasicAsyncRequestProducerGenerator(URI),
                BasicAsyncResponseConsumerFactory.OK,
                EmptyFutureCallback.INSTANCE);
            try {
                future.get();
                Assert.fail();
            } catch (ExecutionException e) {
                YandexAssert.assertInstanceOf(
                    UnknownHostException.class,
                    e.getCause());
            }
            Assert.assertEquals(
                new ImmutableMap.Builder<String, Object>()
                    .put(STATUS, IOReactorStatus.ACTIVE)
                    .put(PENDING_POOLS, 0)
                    .build(),
                client.status(true));
            Assert.assertEquals(
                Collections.emptyMap(),
                reactor.dnsResolver().status(true));
        }
    }

    @Test
    public void testCancelledRetry() throws Exception {
        try (StaticServer server = new StaticServer(config());
            SharedConnectingIOReactor reactor =
                new SharedConnectingIOReactor(config(), Configs.dnsConfig());
            AsyncClient client =
                new AsyncClient(
                    reactor,
                    new HttpTargetConfigBuilder(Configs.targetConfig())
                        .httpRetries(
                            new RetriesConfigBuilder()
                                .count(RETRIES)
                                .interval(TIMEOUT))
                        .build()))
        {
            server.start();
            reactor.start();
            client.start();
            server.add(URI, HttpStatus.SC_GATEWAY_TIMEOUT, "Die!!!");
            try {
                client.execute(
                    server.host(),
                    new BasicAsyncRequestProducerGenerator(PING),
                    BasicAsyncResponseConsumerFactory.OK,
                    EmptyFutureCallback.INSTANCE)
                    .get();
            } catch (Throwable t) {
            }
            Future<HttpResponse> future = client.execute(
                server.host(),
                new BasicAsyncRequestProducerGenerator(URI),
                BasicAsyncResponseConsumerFactory.OK,
                EmptyFutureCallback.INSTANCE);
            Thread.sleep(TIMEOUT >> 1);
            Assert.assertTrue(future.cancel(true));
            Thread.sleep(TIMEOUT << 1);
            Assert.assertEquals(1, server.accessCount(URI));
        }
    }

    @Test
    public void testFailover() throws Exception {
        try (StaticServer server1 = new StaticServer(config());
            StaticServer server2 = new StaticServer(config());
            SharedConnectingIOReactor reactor =
                new SharedConnectingIOReactor(config(), Configs.dnsConfig());
            AsyncClient client =
                new AsyncClient(
                    reactor,
                    new HttpTargetConfigBuilder(Configs.targetConfig())
                        .httpRetries(
                            new RetriesConfigBuilder()
                                .count(RETRIES)
                                .interval(TIMEOUT))
                        .build()))
        {
            server1.start();
            server2.start();
            reactor.start();
            client.start();
            server1.add(URI, HttpStatus.SC_SERVICE_UNAVAILABLE, "Try again");
            server2.add(URI, HttpStatus.SC_OK, "Hi there!");
            Future<HttpResponse> future = client.execute(
                Arrays.asList(server1.host(), server2.host()),
                new BasicAsyncRequestProducerGenerator(URI),
                BasicAsyncResponseConsumerFactory.OK,
                EmptyFutureCallback.INSTANCE);
            HttpResponse response = future.get(
                TIMEOUT >> 1,
                TimeUnit.MILLISECONDS);
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            Map<Object, Object> status = new HashMap<>();
            status.put(STATUS, IOReactorStatus.ACTIVE);
            status.put(PENDING_POOLS, 0);
            Map<Object, Object> routeStatus = new HashMap<>();
            routeStatus.put(ACTIVE_CONNECTIONS, 0);
            routeStatus.put(PENDING_CONNECTIONS, 0);
            routeStatus.put(AVAILABLE_CONNECTIONS, 1);
            Assert.assertEquals(status, client.status(false));
            status.put(
                new HttpRoute(server2.host(), null, https()).toString(),
                routeStatus);
            Assert.assertEquals(status, client.status(true));
        }
    }

    @Test
    public void testFailoverInterval() throws Exception {
        try (StaticServer server1 = new StaticServer(config());
            StaticServer server2 = new StaticServer(config());
            SharedConnectingIOReactor reactor =
                new SharedConnectingIOReactor(config(), Configs.dnsConfig());
            AsyncClient client =
                new AsyncClient(
                    reactor,
                    new HttpTargetConfigBuilder(Configs.targetConfig())
                        .httpRetries(
                            new RetriesConfigBuilder()
                                .count(RETRIES)
                                .interval(TIMEOUT))
                        .build()))
        {
            server1.start();
            server2.start();
            reactor.start();
            client.start();
            server1.add(
                URI,
                new StaticHttpItem(HttpStatus.SC_GATEWAY_TIMEOUT),
                new StaticHttpItem(HttpStatus.SC_OK));
            server2.add(URI, HttpStatus.SC_BAD_GATEWAY);
            long start = System.currentTimeMillis();
            Future<HttpResponse> future = client.execute(
                Arrays.asList(server1.host(), server2.host()),
                new BasicAsyncRequestProducerGenerator(URI),
                BasicAsyncResponseConsumerFactory.OK,
                EmptyFutureCallback.INSTANCE);
            HttpResponse response = future.get();
            long timeTaken = System.currentTimeMillis() - start;
            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                response);
            YandexAssert.assertNotLess((long) TIMEOUT, timeTaken);
        }
    }

    @Test
    public void testFailedFailover() throws Exception {
        try (StaticServer server1 = new StaticServer(config());
            StaticServer server2 = new StaticServer(config());
            SharedConnectingIOReactor reactor =
                new SharedConnectingIOReactor(config(), Configs.dnsConfig());
            AsyncClient client =
                new AsyncClient(
                    reactor,
                    new HttpTargetConfigBuilder(Configs.targetConfig())
                        .httpRetries(new RetriesConfigBuilder().count(RETRIES))
                        .build()))
        {
            server1.start();
            server2.start();
            reactor.start();
            client.start();
            server1.add(URI, HttpStatus.SC_INSUFFICIENT_STORAGE);
            server2.add(URI, HttpStatus.SC_INSUFFICIENT_STORAGE);
            Future<HttpResponse> future = client.execute(
                Arrays.asList(server1.host(), server2.host()),
                new BasicAsyncRequestProducerGenerator(URI),
                BasicAsyncResponseConsumerFactory.OK,
                EmptyFutureCallback.INSTANCE);
            try {
                future.get();
                Assert.fail();
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                YandexAssert.assertInstanceOf(
                    BadResponseException.class,
                    cause);
                Assert.assertEquals(
                    HttpStatus.SC_INSUFFICIENT_STORAGE,
                    ((BadResponseException) cause).statusCode());
            }
        }
    }

    @Test
    public void testIoFailover() throws Exception {
        final int iterations = 10000;
        ContentProducer producer = new ContentProducer() {
            @Override
            public void writeTo(final OutputStream out) throws IOException {
                for (int i = 0; i < iterations; ++i) {
                    out.write(1);
                    out.flush();
                }
                throw new IOException("Time to die!");
            }
        };
        EntityTemplate entity = new EntityTemplate(producer);
        entity.setChunked(true);
        try (StaticServer server1 = new StaticServer(config("First"));
            StaticServer server2 = new StaticServer(config("Second"));
            SharedConnectingIOReactor reactor =
                new SharedConnectingIOReactor(config(), Configs.dnsConfig());
            AsyncClient client =
                new AsyncClient(
                    reactor,
                    new HttpTargetConfigBuilder(Configs.targetConfig())
                        .connections(2)
                        .timeout(TIMEOUT >> 1)
                        .poolTimeout(TIMEOUT >> 1)
                        // IO retries will occur more often, so request will be
                        // sent to second server twice, before HTTP retry to
                        // first server will take a place
                        .ioRetries(
                            new RetriesConfigBuilder()
                                .count(RETRIES)
                                .interval(TIMEOUT >> 1))
                        .httpRetries(
                            new RetriesConfigBuilder()
                                .count(RETRIES)
                                .interval(TIMEOUT << 1))
                        .build()))
        {
            server1.start();
            server2.start();
            reactor.start();
            client.start();
            server1.add(URI, HttpStatus.SC_BAD_GATEWAY);
            server2.add(
                URI,
                new StaticHttpItem(HttpStatus.SC_OK, entity),
                new StaticHttpItem(HttpStatus.SC_OK));
            Future<HttpResponse> future = client.execute(
                Arrays.asList(server1.host(), server2.host()),
                new BasicAsyncRequestProducerGenerator(URI),
                BasicAsyncResponseConsumerFactory.OK,
                EmptyFutureCallback.INSTANCE);
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, future.get());
            Assert.assertEquals(1, server1.accessCount(URI));
            Assert.assertEquals(2, server2.accessCount(URI));
        }
    }

    @Test
    public void testNonRetriableHost() throws Exception {
        try (StaticServer server = new StaticServer(config());
            SharedConnectingIOReactor reactor =
                new SharedConnectingIOReactor(config(), Configs.dnsConfig());
            AsyncClient client =
                new AsyncClient(
                    reactor,
                    new HttpTargetConfigBuilder(Configs.targetConfig())
                        .timeout(TIMEOUT >> 1)
                        .poolTimeout(TIMEOUT >> 1)
                        .ioRetries(
                            new RetriesConfigBuilder()
                                .count(RETRIES)
                                // prevent server workers clogging
                                .interval(TIMEOUT << 1))
                        .build()))
        {
            server.start();
            reactor.start();
            client.start();
            server.add(
                URI,
                new StaticHttpResource(
                    new SlowpokeHttpItem(
                        new StaticHttpItem(HttpStatus.SC_ACCEPTED),
                        TIMEOUT << 1)));
            CountingSupplier<HttpClientContext> context =
                new CountingSupplier<>(client.httpClientContextGenerator());
            Future<HttpResponse> future = client.execute(
                Arrays.asList(
                    new HttpHost(
                        "nobody.will.ever.register.this.too",
                        server.port()),
                    server.host()),
                new BasicAsyncRequestProducerGenerator(URI),
                BasicAsyncResponseConsumerFactory.OK,
                context,
                EmptyFutureCallback.INSTANCE);
            try {
                future.get();
                Assert.fail();
            } catch (ExecutionException e) {
                YandexAssert.assertInstanceOf(
                    SocketTimeoutException.class,
                    e.getCause());
            }
            Thread.sleep(TIMEOUT << 1);
            Assert.assertEquals(RETRIES + 1, server.accessCount(URI));
            Assert.assertEquals(RETRIES + 2, context.count().get());
        }
    }

    private void testReferer(final boolean passReferer) throws Exception {
        try (StaticServer server = new StaticServer(config());
            SharedConnectingIOReactor reactor =
                new SharedConnectingIOReactor(config(), Configs.dnsConfig());
            AsyncClient client =
                new AsyncClient(
                    reactor,
                    new HttpTargetConfigBuilder(Configs.targetConfig())
                        .passReferer(passReferer)
                        .build()))
        {
            server.start();
            reactor.start();
            client.start();
            String referer;
            if (passReferer) {
                referer = URI + URI;
            } else {
                referer = null;
            }
            server.add(
                URI,
                new ExpectingHeaderHttpItem(
                    new StaticHttpItem(HttpStatus.SC_OK),
                    HttpHeaders.REFERER,
                    referer));
            HttpCoreContext context = HttpCoreContext.create();
            context.setAttribute(
                HttpCoreContext.HTTP_REQUEST,
                new BasicHttpRequest("GET", URI + URI));
            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                client.adjust(context).execute(
                    new AsyncGetURIRequestProducerSupplier(
                        server.host() + URI),
                    BasicAsyncResponseConsumerFactory.INSTANCE,
                    EmptyFutureCallback.INSTANCE)
                    .get());
        }
    }

    @Test
    public void testReferer() throws Exception {
        testReferer(true);
    }

    @Test
    public void testRefererDisabled() throws Exception {
        testReferer(false);
    }

    @Test
    public void testOverrideAcceptCharset() throws Exception {
        try (StaticServer server = new StaticServer(config());
            SharedConnectingIOReactor reactor =
                new SharedConnectingIOReactor(config(), Configs.dnsConfig());
            AsyncClient client =
                new AsyncClient(
                    reactor,
                    new HttpTargetConfigBuilder(Configs.targetConfig())
                        .responseCharset(StandardCharsets.UTF_16BE)
                        .build()))
        {
            server.start();
            reactor.start();
            client.start();
            String charset = StandardCharsets.UTF_16LE.name();
            server.add(
                URI,
                new ExpectingHeaderHttpItem(
                    new StaticHttpItem(HttpStatus.SC_OK),
                    HttpHeaders.ACCEPT_CHARSET,
                    charset));
            BasicAsyncRequestProducerGenerator producerGenerator =
                new BasicAsyncRequestProducerGenerator(URI);
            producerGenerator.addHeader(
                HeaderUtils.createHeader(HttpHeaders.ACCEPT_CHARSET, charset));
            Future<HttpResponse> future = client.execute(
                server.host(),
                producerGenerator,
                BasicAsyncResponseConsumerFactory.OK,
                EmptyFutureCallback.INSTANCE);
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, future.get());
        }
    }

    @Test
    public void testTestAsyncPostURI() throws Exception {
        try (StaticServer server = new StaticServer(config());
            SharedConnectingIOReactor reactor =
                new SharedConnectingIOReactor(config(), Configs.dnsConfig());
            AsyncClient client =
                new AsyncClient(reactor, Configs.targetConfig()))
        {
            server.start();
            reactor.start();
            client.start();
            String body = "test";
            server.add(
                URI,
                new ExpectingHttpItem(body),
                new ExpectingHttpItem(body));

            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                client.execute(
                    new AsyncPostURIRequestProducerSupplier(
                        server.host() + URI,
                        body,
                        ContentType.APPLICATION_OCTET_STREAM),
                    BasicAsyncResponseConsumerFactory.OK,
                    EmptyFutureCallback.INSTANCE).get());

            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                client.execute(
                    new AsyncPostURIRequestProducerSupplier(
                        server.host() + URI,
                        new byte[]{'t', 'e', 's', 't'},
                        ContentType.APPLICATION_OCTET_STREAM),
                    BasicAsyncResponseConsumerFactory.OK,
                    EmptyFutureCallback.INSTANCE).get());
        }
    }

    @Test
    public void testMultiProxy() throws Exception {
        try (StaticServer proxy1 = new StaticServer(config("Proxy-1"));
            StaticServer proxy2 = new StaticServer(config("Proxy-2"));
            StaticServer proxy3 = new StaticServer(config("Proxy-3"));
            SharedConnectingIOReactor reactor =
                new SharedConnectingIOReactor(config(), Configs.dnsConfig());
            AsyncClient client =
                new AsyncClient(reactor, Configs.targetConfig()))
        {
            String uri = "http://localhost:8081/some/uri?here";
            proxy1.add(uri, HttpStatus.SC_SERVICE_UNAVAILABLE);
            proxy2.add(uri, HttpStatus.SC_OK);
            proxy3.add(uri, HttpStatus.SC_SERVICE_UNAVAILABLE);

            proxy1.start();
            proxy2.start();
            proxy3.start();
            reactor.start();
            client.start();

            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                client.execute(
                    Arrays.asList(proxy1.host(), proxy2.host(), proxy3.host()),
                    new AsyncGetURIRequestProducerSupplier(uri),
                    BasicAsyncResponseConsumerFactory.OK,
                    EmptyFutureCallback.INSTANCE).get());

            Assert.assertEquals(1, proxy1.accessCount(uri));
            Assert.assertEquals(1, proxy2.accessCount(uri));
            Assert.assertEquals(0, proxy3.accessCount(uri));
        }
    }

    @Test
    public void testProxyUpstreamStatus() throws Exception {
        try (StaticServer proxy = new StaticServer(config("Proxy"));
            SharedConnectingIOReactor reactor =
                new SharedConnectingIOReactor(config(), Configs.dnsConfig());
            AsyncClient client =
                new AsyncClient(
                    reactor,
                    new HttpTargetConfigBuilder(Configs.targetConfig())
                        .proxy(proxy.host())
                        .build()))
        {
            String uri = "http://localhost:8081/another/uri?here";
            proxy.add(uri, HttpStatus.SC_OK);

            proxy.start();
            reactor.start();
            client.start();

            BasicRequestsListener listener = new BasicRequestsListener();
            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                client.execute(
                    new AsyncGetURIRequestProducerSupplier(uri),
                    BasicAsyncResponseConsumerFactory.OK,
                    listener.createContextGeneratorFor(client),
                    EmptyFutureCallback.INSTANCE).get());

            final int port = 8081;
            Assert.assertEquals(
                new HttpHost("localhost", port),
                listener.get(0).route().getTargetHost());
            Assert.assertEquals(
                proxy.host(),
                listener.get(0).route().getProxyHost());
        }
    }

    @Test
    public void testCancelledRequest() throws Exception {
        try (StaticServer server = new StaticServer(config());
            SharedConnectingIOReactor reactor =
                new SharedConnectingIOReactor(config(), Configs.dnsConfig());
            AsyncClient client =
                new AsyncClient(
                    reactor,
                    new HttpTargetConfigBuilder(Configs.targetConfig())
                        .connections(1)
                        .timeout(TIMEOUT << 1)
                        .poolTimeout(TIMEOUT)
                        .build()))
        {
            server.start();
            reactor.start();
            client.start();
            server.add(
                URI,
                new SlowpokeHttpResource(
                    new StaticHttpResource(HttpStatus.SC_OK),
                    TIMEOUT));
            Future<HttpResponse> future = client.execute(
                server.host(),
                new BasicAsyncRequestProducerGenerator(URI),
                BasicAsyncResponseConsumerFactory.OK,
                EmptyFutureCallback.INSTANCE);
            Thread.sleep(SMOL_TIMEOUT);
            logger.info("Spawning second request");
            Future<HttpResponse> second = client.execute(
                server.host(),
                new BasicAsyncRequestProducerGenerator(URI),
                BasicAsyncResponseConsumerFactory.OK,
                EmptyFutureCallback.INSTANCE);
            Thread.sleep(SMOL_TIMEOUT);
            Assert.assertTrue(second.cancel(true));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, future.get());
            Thread.sleep(TIMEOUT);
            // Second request never reached server
            Assert.assertEquals(1, server.accessCount(URI));
            Assert.assertEquals(
                new ImmutableMap.Builder<String, Object>()
                    .put(STATUS, IOReactorStatus.ACTIVE)
                    .put(PENDING_POOLS, 0)
                    .put(
                        new HttpRoute(server.host(), null, https()).toString(),
                        new ImmutableMap.Builder<String, Object>()
                            .put(ACTIVE_CONNECTIONS, 0)
                            .put(PENDING_CONNECTIONS, 0)
                            .put(AVAILABLE_CONNECTIONS, 1)
                            .build())
                    .build(),
                client.status(true));
        }
    }

    @Test
    public void testRequestDetailsHiddenHeaders() throws Exception {
        try (StaticServer server = new StaticServer(config());
            SharedConnectingIOReactor reactor =
                new SharedConnectingIOReactor(config(), Configs.dnsConfig());
            AsyncClient client =
                new AsyncClient(
                    reactor,
                    new HttpTargetConfigBuilder(Configs.targetConfig())
                        .connections(1)
                        .timeout(TIMEOUT << 1)
                        .poolTimeout(TIMEOUT)
                        .build()))
        {
            server.start();
            reactor.start();
            client.start();
            server.add(
                URI,
                new StaticHttpResource(HttpStatus.SC_SERVICE_UNAVAILABLE));
            RequestsListener listener = new BasicRequestsListener();
            BasicAsyncRequestProducerGenerator producerGenerator =
                new BasicAsyncRequestProducerGenerator(URI);
            String secret = "secret-token-here";
            producerGenerator.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                secret);
            try {
                client.execute(
                    server.host(),
                    producerGenerator,
                    BasicAsyncResponseConsumerFactory.OK,
                    listener.createContextGeneratorFor(client),
                    EmptyFutureCallback.INSTANCE)
                    .get();
                Assert.fail();
            } catch (ExecutionException e) {
                YandexAssert.assertInstanceOf(
                    BadResponseException.class,
                    e.getCause());
                YandexAssert.assertNotContains(
                    secret,
                    e.getCause().getMessage());
            }
            YandexAssert.assertNotContains(secret, listener.details());
        }
    }
}

