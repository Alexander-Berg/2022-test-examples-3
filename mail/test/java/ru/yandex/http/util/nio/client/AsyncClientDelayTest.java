package ru.yandex.http.util.nio.client;

import java.util.Arrays;
import java.util.concurrent.Future;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.http.config.HttpTargetConfigBuilder;
import ru.yandex.http.config.RetriesConfigBuilder;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.test.SlowpokeHttpResource;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.http.test.StaticServer;
import ru.yandex.http.util.EmptyFutureCallback;
import ru.yandex.http.util.nio.BasicAsyncRequestProducerGenerator;
import ru.yandex.http.util.nio.BasicAsyncResponseConsumerFactory;
import ru.yandex.test.util.TestBase;

public class AsyncClientDelayTest extends TestBase {
    private static final int TIMEOUT = 4000;
    private static final int RETRIES = 3;
    private static final String PING = "/ping";
    private static final String URI = "/uri";
    private static final String LOCALHOST = "localhost";

    public AsyncClientDelayTest() {
        super(true, 1000L);
    }

    @Test
    public void testDelay() throws Exception {
        try (StaticServer server1 =
                new StaticServer(Configs.baseConfig("Delay1"));
            StaticServer server2 =
                new StaticServer(Configs.baseConfig("Delay2"));
            SharedConnectingIOReactor reactor = new SharedConnectingIOReactor(
                Configs.baseConfig(),
                Configs.dnsConfig());
            AsyncClient client =
                new AsyncClient(reactor, Configs.targetConfig()))
        {
            server1.add(
                URI,
                new SlowpokeHttpResource(
                    new StaticHttpResource(HttpStatus.SC_BAD_GATEWAY),
                    TIMEOUT));
            server2.add(URI, HttpStatus.SC_OK);
            server1.start();
            server2.start();
            reactor.start();
            client.start();
            Future<HttpResponse> future = client.executeWithDelay(
                Arrays.asList(
                    new HttpHost(LOCALHOST, server1.port()),
                    new HttpHost(LOCALHOST, server2.port())),
                new BasicAsyncRequestProducerGenerator(URI),
                TIMEOUT >> 1,
                BasicAsyncResponseConsumerFactory.OK,
                EmptyFutureCallback.INSTANCE);
            Thread.sleep(TIMEOUT >> 2);
            Assert.assertEquals(1, server1.accessCount(URI));
            Assert.assertEquals(0, server2.accessCount(URI));
            Assert.assertFalse(future.isDone());
            Thread.sleep(TIMEOUT >> 1);
            Assert.assertEquals(1, server1.accessCount(URI));
            Assert.assertEquals(1, server2.accessCount(URI));
            Assert.assertTrue(future.isDone());
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, future.get());

            Thread.sleep(TIMEOUT);
            server1.add(URI, HttpStatus.SC_OK);
            server2.add(URI, HttpStatus.SC_BAD_GATEWAY);
            future = client.executeWithDelay(
                Arrays.asList(
                    new HttpHost(LOCALHOST, server1.port()),
                    new HttpHost(LOCALHOST, server2.port())),
                new BasicAsyncRequestProducerGenerator(URI),
                TIMEOUT >> 1,
                BasicAsyncResponseConsumerFactory.OK,
                EmptyFutureCallback.INSTANCE);
            Thread.sleep((TIMEOUT >> 2) + (TIMEOUT >> (2 + 1)));
            Assert.assertEquals(1, server1.accessCount(URI));
            Assert.assertEquals(0, server2.accessCount(URI));
            Assert.assertTrue(future.isDone());
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, future.get());
        }
    }

    @Test
    public void testDelayDeadBackend() throws Exception {
        try (StaticServer server1 =
                new StaticServer(Configs.baseConfig("DelayDead1"));
            StaticServer server2 =
                new StaticServer(Configs.baseConfig("DelayDead2"));
            StaticServer server3 =
                new StaticServer(Configs.baseConfig("DelayDead3"));
            SharedConnectingIOReactor reactor = new SharedConnectingIOReactor(
                Configs.baseConfig(),
                Configs.dnsConfig());
            AsyncClient client =
                new AsyncClient(reactor, Configs.targetConfig()))
        {
            server1.add(URI, HttpStatus.SC_BAD_GATEWAY);
            server2.add(URI, HttpStatus.SC_BAD_GATEWAY);
            server3.add(URI, HttpStatus.SC_OK);
            server1.start();
            server2.start();
            server3.start();
            reactor.start();
            client.start();
            Future<HttpResponse> future = client.executeWithDelay(
                Arrays.asList(
                    new HttpHost(LOCALHOST, server1.port()),
                    new HttpHost(LOCALHOST, server2.port()),
                    new HttpHost(LOCALHOST, server3.port())),
                new BasicAsyncRequestProducerGenerator(URI),
                TIMEOUT,
                BasicAsyncResponseConsumerFactory.OK,
                EmptyFutureCallback.INSTANCE);
            Thread.sleep(TIMEOUT >> 1);
            Assert.assertTrue(future.isDone());
            Assert.assertEquals(1, server1.accessCount(URI));
            Assert.assertEquals(1, server2.accessCount(URI));
            Assert.assertEquals(1, server3.accessCount(URI));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, future.get());
        }
    }

    @Test
    public void testDelayFailover() throws Exception {
        try (StaticServer server1 =
                new StaticServer(Configs.baseConfig("DelayFailover1"));
            StaticServer server2 =
                new StaticServer(Configs.baseConfig("DelayFailover2"));
            SharedConnectingIOReactor reactor = new SharedConnectingIOReactor(
                Configs.baseConfig(),
                Configs.dnsConfig());
            AsyncClient client =
                new AsyncClient(
                    reactor,
                    new HttpTargetConfigBuilder(Configs.targetConfig())
                        .connections(1)
                        .timeout(TIMEOUT)
                        .poolTimeout(TIMEOUT >> 1)
                        .httpRetries(
                            new RetriesConfigBuilder()
                                .count(RETRIES)
                                .interval(TIMEOUT))
                        .build()))
        {
            server1.add(URI, HttpStatus.SC_BAD_GATEWAY);
            server2.add(URI, HttpStatus.SC_OK);
            server1.start();
            server2.start();
            reactor.start();
            client.start();

            // Warmup
            client.execute(
                server1.host(),
                new BasicAsyncRequestProducerGenerator(PING),
                BasicAsyncResponseConsumerFactory.INSTANCE,
                EmptyFutureCallback.INSTANCE)
                .get();

            client.execute(
                server2.host(),
                new BasicAsyncRequestProducerGenerator(PING),
                BasicAsyncResponseConsumerFactory.INSTANCE,
                EmptyFutureCallback.INSTANCE)
                .get();

            Future<HttpResponse> future = client.executeWithDelay(
                Arrays.asList(
                    new HttpHost(LOCALHOST, server1.port()),
                    new HttpHost(LOCALHOST, server2.port())),
                new BasicAsyncRequestProducerGenerator(URI),
                TIMEOUT >> 1,
                BasicAsyncResponseConsumerFactory.OK,
                EmptyFutureCallback.INSTANCE);
            Thread.sleep(TIMEOUT >> 2);
            Assert.assertEquals(1, server1.accessCount(URI));
            Assert.assertEquals(1, server2.accessCount(URI));
            Assert.assertTrue(future.isDone());
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, future.get());
        }
    }

    @Test
    public void testDelayCancel() throws Exception {
        try (StaticServer server1 =
                new StaticServer(Configs.baseConfig("DelayCancel1"));
            StaticServer server2 =
                new StaticServer(Configs.baseConfig("DelayCancel2"));
            SharedConnectingIOReactor reactor = new SharedConnectingIOReactor(
                Configs.baseConfig(),
                Configs.dnsConfig());
            AsyncClient client =
                new AsyncClient(
                    reactor,
                    new HttpTargetConfigBuilder(Configs.targetConfig())
                        .connections(1)
                        .timeout(TIMEOUT)
                        .poolTimeout(TIMEOUT >> 1)
                        .httpRetries(
                            new RetriesConfigBuilder()
                                .count(RETRIES)
                                .interval(TIMEOUT))
                        .build()))
        {
            server1.add(
                URI,
                new SlowpokeHttpResource(
                    new StaticHttpResource(HttpStatus.SC_BAD_GATEWAY),
                    TIMEOUT));
            server2.add(URI, HttpStatus.SC_OK);
            server1.start();
            server2.start();
            reactor.start();
            client.start();

            // Warmup
            client.execute(
                server1.host(),
                new BasicAsyncRequestProducerGenerator(PING),
                BasicAsyncResponseConsumerFactory.INSTANCE,
                EmptyFutureCallback.INSTANCE)
                .get();

            client.execute(
                server2.host(),
                new BasicAsyncRequestProducerGenerator(PING),
                BasicAsyncResponseConsumerFactory.INSTANCE,
                EmptyFutureCallback.INSTANCE)
                .get();

            Future<HttpResponse> future = client.executeWithDelay(
                Arrays.asList(
                    new HttpHost(LOCALHOST, server1.port()),
                    new HttpHost(LOCALHOST, server2.port())),
                new BasicAsyncRequestProducerGenerator(URI),
                TIMEOUT >> 1,
                BasicAsyncResponseConsumerFactory.OK,
                EmptyFutureCallback.INSTANCE);
            Thread.sleep(TIMEOUT >> 2);
            future.cancel(true);
            Assert.assertTrue(future.isDone());
            Assert.assertTrue(future.isCancelled());
            Thread.sleep(TIMEOUT);
            Assert.assertEquals(1, server1.accessCount(URI));
            Assert.assertEquals(0, server2.accessCount(URI));
        }
    }
}

