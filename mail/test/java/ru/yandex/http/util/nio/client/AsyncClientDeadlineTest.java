package ru.yandex.http.util.nio.client;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.http.config.HttpTargetConfigBuilder;
import ru.yandex.http.config.RetriesConfigBuilder;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.HeaderHttpItem;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.test.SlowpokeHttpResource;
import ru.yandex.http.test.StaticHttpItem;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.http.test.StaticServer;
import ru.yandex.http.util.BadResponseException;
import ru.yandex.http.util.EmptyFutureCallback;
import ru.yandex.http.util.nio.BasicAsyncRequestProducerGenerator;
import ru.yandex.http.util.nio.BasicAsyncResponseConsumerFactory;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

public class AsyncClientDeadlineTest extends TestBase {
    private static final int TIMEOUT = 4000;
    private static final int RETRIES = 3;
    private static final String URI = "/uri";
    private static final String LOCALHOST = "localhost";

    @Test
    public void testDeadline() throws Exception {
        try (StaticServer server1 =
                new StaticServer(Configs.baseConfig("Deadline1"));
            StaticServer server2 =
                new StaticServer(Configs.baseConfig("Deadline2"));
            SharedConnectingIOReactor reactor = new SharedConnectingIOReactor(
                Configs.baseConfig(),
                Configs.dnsConfig());
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
            server1.add(
                URI,
                new StaticHttpItem(HttpStatus.SC_GATEWAY_TIMEOUT),
                new StaticHttpItem(HttpStatus.SC_OK));
            server2.add(
                URI,
                new SlowpokeHttpResource(
                    new StaticHttpResource(HttpStatus.SC_BAD_GATEWAY),
                    TIMEOUT >> 2));
            server1.start();
            server2.start();
            reactor.start();
            client.start();
            Future<HttpResponse> future = client.execute(
                Arrays.asList(
                    new HttpHost(LOCALHOST, server1.port()),
                    new HttpHost(LOCALHOST, server2.port())),
                new BasicAsyncRequestProducerGenerator(URI),
                System.currentTimeMillis() + ((TIMEOUT >> 1) + TIMEOUT),
                BasicAsyncResponseConsumerFactory.OK,
                EmptyFutureCallback.INSTANCE);
            Thread.sleep(TIMEOUT >> 1);
            Assert.assertEquals(1, server1.accessCount(URI));
            Assert.assertEquals(1, server2.accessCount(URI));
            Assert.assertFalse(future.isDone());
            Thread.sleep(TIMEOUT);
            Assert.assertEquals(2, server1.accessCount(URI));
            Assert.assertEquals(1, server2.accessCount(URI));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, future.get());
        }
    }

    @Test
    public void testDeadlineBadRequest() throws Exception {
        try (StaticServer server1 =
                new StaticServer(Configs.baseConfig("DeadlineBad1"));
            StaticServer server2 =
                new StaticServer(Configs.baseConfig("DeadlineBad2"));
            SharedConnectingIOReactor reactor = new SharedConnectingIOReactor(
                Configs.baseConfig(),
                Configs.dnsConfig());
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
            server1.add(URI, HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE);
            server2.add(URI, HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE);
            Future<HttpResponse> future = client.execute(
                Arrays.asList(
                    new HttpHost(LOCALHOST, server1.port()),
                    new HttpHost(LOCALHOST, server2.port())),
                new BasicAsyncRequestProducerGenerator(URI),
                System.currentTimeMillis() + ((TIMEOUT >> 1) + TIMEOUT),
                BasicAsyncResponseConsumerFactory.OK,
                EmptyFutureCallback.INSTANCE);
            Thread.sleep(TIMEOUT >> 1);
            Assert.assertTrue(future.isDone());
            Assert.assertEquals(1, server1.accessCount(URI));
            Assert.assertEquals(0, server2.accessCount(URI));
            try {
                future.get();
                Assert.fail();
            } catch (ExecutionException e) {
                YandexAssert.assertInstanceOf(
                    BadResponseException.class,
                    e.getCause());
            }
        }
    }

    @Test
    public void testDeadlineDeadBackend() throws Exception {
        try (StaticServer server1 =
                new StaticServer(Configs.baseConfig("DeadlineDead1"));
            StaticServer server2 =
                new StaticServer(Configs.baseConfig("DeadlineDead2"));
            StaticServer server3 =
                new StaticServer(Configs.baseConfig("DeadlineDead3"));
            SharedConnectingIOReactor reactor = new SharedConnectingIOReactor(
                Configs.baseConfig(),
                Configs.dnsConfig());
            AsyncClient client =
                new AsyncClient(reactor, Configs.targetConfig()))
        {
            server1.add(URI, HttpStatus.SC_INTERNAL_SERVER_ERROR);
            server2.add(URI, HttpStatus.SC_INTERNAL_SERVER_ERROR);
            server3.add(URI, HttpStatus.SC_INTERNAL_SERVER_ERROR);
            server1.start();
            server2.start();
            server3.start();
            reactor.start();
            client.start();
            Future<HttpResponse> future = client.execute(
                Arrays.asList(
                    new HttpHost(LOCALHOST, server1.port()),
                    new HttpHost(LOCALHOST, server2.port()),
                    new HttpHost(LOCALHOST, server3.port())),
                new BasicAsyncRequestProducerGenerator(URI),
                System.currentTimeMillis() + TIMEOUT + TIMEOUT + TIMEOUT,
                BasicAsyncResponseConsumerFactory.OK,
                EmptyFutureCallback.INSTANCE);
            Thread.sleep(TIMEOUT >> 1);
            Assert.assertTrue(future.isDone());
            Assert.assertEquals(1, server1.accessCount(URI));
            Assert.assertEquals(1, server2.accessCount(URI));
            Assert.assertEquals(1, server3.accessCount(URI));
            try {
                future.get();
                Assert.fail();
            } catch (ExecutionException e) {
                YandexAssert.assertInstanceOf(
                    BadResponseException.class,
                    e.getCause());
            }
        }
    }

    @Test
    public void testRedirects() throws Exception {
        try (StaticServer server = new StaticServer(Configs.baseConfig());
            SharedConnectingIOReactor reactor = new SharedConnectingIOReactor(
                Configs.baseConfig(),
                Configs.dnsConfig());
            AsyncClient client =
                new AsyncClient(
                    reactor,
                    new HttpTargetConfigBuilder(Configs.targetConfig())
                        .redirects(false)
                        .build()))
        {
            server.start();
            reactor.start();
            client.start();
            String redirectUri = "/second-uri";
            server.add(redirectUri, HttpStatus.SC_OK);
            server.add(
                URI,
                new HeaderHttpItem(
                    new StaticHttpItem(HttpStatus.SC_TEMPORARY_REDIRECT),
                    HttpHeaders.LOCATION,
                    redirectUri));
            Future<HttpResponse> future = client.execute(
                Collections.singletonList(server.host()),
                new BasicAsyncRequestProducerGenerator(URI),
                System.currentTimeMillis() + TIMEOUT,
                BasicAsyncResponseConsumerFactory.INSTANCE,
                EmptyFutureCallback.INSTANCE);
            HttpAssert.assertStatusCode(
                HttpStatus.SC_TEMPORARY_REDIRECT,
                future.get());
            Assert.assertEquals(1, server.accessCount(URI));
            Assert.assertEquals(0, server.accessCount(redirectUri));
        }
    }
}

