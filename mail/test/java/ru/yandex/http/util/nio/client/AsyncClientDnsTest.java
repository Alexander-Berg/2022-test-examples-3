package ru.yandex.http.util.nio.client;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import com.google.common.collect.ImmutableMap;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.impl.conn.SystemDefaultDnsResolver;
import org.apache.http.nio.reactor.IOReactorStatus;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.concurrent.SingleNamedThreadFactory;
import ru.yandex.http.config.HttpTargetConfigBuilder;
import ru.yandex.http.config.ImmutableHttpTargetConfig;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.test.SlowpokeHttpItem;
import ru.yandex.http.test.StaticHttpItem;
import ru.yandex.http.test.StaticServer;
import ru.yandex.http.util.EmptyFutureCallback;
import ru.yandex.http.util.nio.BasicAsyncRequestProducerGenerator;
import ru.yandex.http.util.nio.BasicAsyncResponseConsumerFactory;
import ru.yandex.http.util.nio.client.pool.DnsUpdater;
import ru.yandex.http.util.nio.client.pool.MultiDnsResolver;
import ru.yandex.http.util.nio.client.pool.SingleThreadDnsResolver;
import ru.yandex.http.util.nio.client.pool.SingleThreadDnsUpdater;
import ru.yandex.http.util.server.ImmutableBaseServerConfig;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

public class AsyncClientDnsTest extends TestBase {
    private static final long TIMEOUT = 2000L;
    private static final int DEFAULT_DNS_TTL = 600000;
    private static final String URI = "/uri";
    private static final String LOCALHOST = "localhost";
    private static final String STATUS = "status";
    private static final String PENDING_POOLS = "pending_pools";

    @Test
    public void testDnsTakenTooLong() throws Exception {
        String name = "DNS-Taken-Too-Long";
        ThreadGroup threadGroup = new ThreadGroup(name);
        ImmutableBaseServerConfig serverConfig = Configs.baseConfig();
        ImmutableHttpTargetConfig backendConfig =
            new HttpTargetConfigBuilder(Configs.targetConfig())
                .connectTimeout((int) (TIMEOUT >> 1))
                .build();
        try (StaticServer server = new StaticServer(serverConfig);
            SharedConnectingIOReactor reactor = new SharedConnectingIOReactor(
                new MultiDnsResolver(
                    1,
                    () -> new SlowpokeDnsResolver(TIMEOUT << 1)),
                new SingleThreadDnsUpdater(
                    SystemDefaultDnsResolver.INSTANCE,
                    DEFAULT_DNS_TTL,
                    new SingleNamedThreadFactory(name)),
                serverConfig,
                threadGroup);
            AsyncClient client = new AsyncClient(reactor, backendConfig))
        {
            server.add(URI, HttpStatus.SC_OK);
            server.start();
            reactor.start();
            client.start();
            Future<HttpResponse> future = client.execute(
                server.host(),
                new BasicAsyncRequestProducerGenerator(URI),
                BasicAsyncResponseConsumerFactory.OK,
                EmptyFutureCallback.INSTANCE);
            Thread.sleep(TIMEOUT >> 2);
            Assert.assertEquals(
                new ImmutableMap.Builder<String, Object>()
                    .put(STATUS, IOReactorStatus.ACTIVE)
                    .put(PENDING_POOLS, 1)
                    .build(),
                client.status(true));
            Assert.assertEquals(
                new ImmutableMap.Builder<String, Object>()
                    .put(
                        Integer.toString(0),
                        new ImmutableMap.Builder<String, Object>()
                            .put(SingleThreadDnsResolver.CACHE_SIZE, 0)
                            .put(SingleThreadDnsResolver.PENDING_COUNT, 0)
                            .put(SingleThreadDnsResolver.RESOLVING, LOCALHOST)
                            .build())
                    .build(),
                reactor.dnsResolver().status(true));
            try {
                future.get();
                Assert.fail();
            } catch (ExecutionException e) {
                e.printStackTrace();
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

            // There shouldn't be any new DNS lookup on second request
            long start = System.currentTimeMillis();
            future = client.execute(
                server.host(),
                new BasicAsyncRequestProducerGenerator(URI),
                BasicAsyncResponseConsumerFactory.OK,
                EmptyFutureCallback.INSTANCE);
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, future.get());
            YandexAssert.assertLess(
                TIMEOUT,
                System.currentTimeMillis() - start);
        }
    }

    @Test
    public void testDnsTakenTooLongCancel() throws Exception {
        String name = "DNS-Taken-Too-Long-Cancel";
        ThreadGroup threadGroup = new ThreadGroup(name);
        ImmutableBaseServerConfig serverConfig = Configs.baseConfig();
        ImmutableHttpTargetConfig backendConfig = Configs.targetConfig();
        try (StaticServer server = new StaticServer(serverConfig);
            SharedConnectingIOReactor reactor = new SharedConnectingIOReactor(
                new SlowpokeDnsResolver(TIMEOUT),
                new SingleThreadDnsUpdater(
                    SystemDefaultDnsResolver.INSTANCE,
                    DEFAULT_DNS_TTL,
                    new SingleNamedThreadFactory(name)),
                serverConfig,
                threadGroup);
            AsyncClient client = new AsyncClient(reactor, backendConfig))
        {
            server.add(URI, HttpStatus.SC_OK);
            server.start();
            reactor.start();
            client.start();
            Future<HttpResponse> future = client.execute(
                server.host(),
                new BasicAsyncRequestProducerGenerator(URI),
                BasicAsyncResponseConsumerFactory.OK,
                EmptyFutureCallback.INSTANCE);
            Thread.sleep(TIMEOUT >> 2);
            Assert.assertEquals(
                new ImmutableMap.Builder<String, Object>()
                    .put(STATUS, IOReactorStatus.ACTIVE)
                    .put(PENDING_POOLS, 1)
                    .build(),
                client.status(true));
            Assert.assertEquals(
                new ImmutableMap.Builder<String, Object>()
                    .put(SingleThreadDnsResolver.CACHE_SIZE, 0)
                    .put(SingleThreadDnsResolver.PENDING_COUNT, 0)
                    .put(SingleThreadDnsResolver.RESOLVING, LOCALHOST)
                    .build(),
                reactor.dnsResolver().status(true));
            Assert.assertFalse(future.isDone());
            future.cancel(true);
            Assert.assertTrue(future.isDone());
            // pending_pools = 1 because DnsCallback can't be cancelled
            Assert.assertEquals(
                new ImmutableMap.Builder<String, Object>()
                    .put(STATUS, IOReactorStatus.ACTIVE)
                    .put(PENDING_POOLS, 1)
                    .build(),
                client.status(true));
            Thread.sleep(TIMEOUT);
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
    public void testDnsHostGone() throws Exception {
        String name = "DNS-Host-Gone";
        ThreadGroup threadGroup = new ThreadGroup(name);
        ImmutableBaseServerConfig serverConfig = Configs.baseConfig();
        ImmutableHttpTargetConfig backendConfig = Configs.targetConfig();
        FakeDnsUpdater dnsUpdater = new FakeDnsUpdater();
        try (StaticServer server = new StaticServer(serverConfig);
            SharedConnectingIOReactor reactor = new SharedConnectingIOReactor(
                new SingleThreadDnsResolver(
                    SystemDefaultDnsResolver.INSTANCE,
                    DEFAULT_DNS_TTL,
                    DEFAULT_DNS_TTL,
                    new SingleNamedThreadFactory(name)),
                dnsUpdater,
                serverConfig,
                threadGroup);
            AsyncClient client = new AsyncClient(reactor, backendConfig))
        {
            server.add(URI, HttpStatus.SC_OK);
            server.start();
            reactor.start();
            client.start();
            for (int i = 0; i <= 2; ++i) {
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_OK,
                    client.execute(
                        server.host(),
                        new BasicAsyncRequestProducerGenerator(URI),
                        BasicAsyncResponseConsumerFactory.OK,
                        EmptyFutureCallback.INSTANCE).get());
                dnsUpdater.notify(LOCALHOST, null);
            }
            Future<HttpResponse> future = client.execute(
                server.host(),
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
            dnsUpdater.notify(LOCALHOST, InetAddress.getByName(LOCALHOST));
            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                client.execute(
                    server.host(),
                    new BasicAsyncRequestProducerGenerator(URI),
                    BasicAsyncResponseConsumerFactory.OK,
                    EmptyFutureCallback.INSTANCE).get());
        }
    }

    @Test
    public void testDnsStackOverflow() throws Exception {
        // When host is gone, all pending requests will be failed at once.
        // Checks that there is no recursion callback calls which leads to
        // StackOverflowError
        String name = "DNS-StackOverflow";
        ThreadGroup threadGroup = new ThreadGroup(name);
        ImmutableBaseServerConfig serverConfig = Configs.baseConfig();
        ImmutableHttpTargetConfig backendConfig =
            new HttpTargetConfigBuilder(Configs.targetConfig())
                .connections(1)
                .poolTimeout((int) (TIMEOUT << 2))
                .build();
        FakeDnsUpdater dnsUpdater = new FakeDnsUpdater();
        try (StaticServer server = new StaticServer(serverConfig);
            SharedConnectingIOReactor reactor = new SharedConnectingIOReactor(
                new SingleThreadDnsResolver(
                    SystemDefaultDnsResolver.INSTANCE,
                    DEFAULT_DNS_TTL,
                    DEFAULT_DNS_TTL,
                    new SingleNamedThreadFactory(name)),
                dnsUpdater,
                serverConfig,
                threadGroup);
            AsyncClient client = new AsyncClient(reactor, backendConfig))
        {
            // Only first request will be slow, all other requests will get
            // 501 Not Implemented immediately
            server.add(
                URI,
                new SlowpokeHttpItem(
                    StaticHttpItem.BAD_REQUEST,
                    TIMEOUT << 2));
            server.start();
            reactor.start();
            client.start();
            // Occupy only connection allowed
            Future<HttpResponse> future = client.execute(
                server.host(),
                new BasicAsyncRequestProducerGenerator(URI),
                BasicAsyncResponseConsumerFactory.INSTANCE,
                EmptyFutureCallback.INSTANCE);
            Thread.sleep(TIMEOUT >> 1);
            // Spawn requests
            final int maxRequests = 1000;
            List<Future<HttpResponse>> futures = new ArrayList<>();
            for (int i = 0; i < maxRequests; ++i) {
                futures.add(
                    client.execute(
                        server.host(),
                        new BasicAsyncRequestProducerGenerator(URI),
                        BasicAsyncResponseConsumerFactory.INSTANCE,
                        EmptyFutureCallback.INSTANCE));
            }
            // Emulate host has gone
            for (int i = 0; i <= 2; ++i) {
                dnsUpdater.notify(LOCALHOST, null);
            }
            // Only connection available will be immediately closed, so pending
            // requests will try to connect
            HttpAssert.assertStatusCode(
                HttpStatus.SC_BAD_REQUEST,
                future.get());
            for (int i = 0; i < futures.size(); ++i) {
                try {
                    Assert.fail(
                        "Unexpected success at request #" + i
                        + ':' + ' ' + futures.get(i).get());
                } catch (ExecutionException e) {
                    YandexAssert.assertInstanceOf(
                        UnknownHostException.class,
                        e.getCause());
                }
            }
        }
    }

    private static class FakeDnsUpdater implements DnsUpdater {
        private final Map<String, List<Consumer<InetAddress>>> subscribers =
            new HashMap<>();

        @Override
        public void start() {
        }

        @Override
        public void close() {
        }

        @Override
        public synchronized void subscribe(
            final String hostname,
            final Consumer<InetAddress> subscriber)
        {
            List<Consumer<InetAddress>> subscribers =
                this.subscribers.get(hostname);
            if (subscribers == null) {
                subscribers = new ArrayList<>();
                this.subscribers.put(hostname, subscribers);
            }
            subscribers.add(subscriber);
        }

        public synchronized void notify(
            final String hostname,
            final InetAddress address)
        {
            List<Consumer<InetAddress>> subscribers =
                this.subscribers.get(hostname);
            if (subscribers != null) {
                for (Consumer<InetAddress> subscriber: subscribers) {
                    subscriber.accept(address);
                }
            }
        }
    }
}

