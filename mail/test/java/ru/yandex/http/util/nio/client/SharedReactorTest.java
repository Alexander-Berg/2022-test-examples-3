package ru.yandex.http.util.nio.client;

import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.http.config.HttpTargetConfigBuilder;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.test.SlowpokeHttpResource;
import ru.yandex.http.test.StaticHttpItem;
import ru.yandex.http.test.StaticServer;
import ru.yandex.http.util.EmptyFutureCallback;
import ru.yandex.http.util.nio.BasicAsyncRequestProducerGenerator;
import ru.yandex.http.util.nio.BasicAsyncResponseConsumerFactory;
import ru.yandex.http.util.server.BaseServerConfigBuilder;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

public class SharedReactorTest extends TestBase {
    private static final long TIMEOUT1 = 2000L;
    private static final long TIMEOUT2 = TIMEOUT1 << 1;
    private static final String URI0 = "/uri0";
    private static final String URI1 = "/uri1";
    private static final String URI2 = "/uri2";

    @Test
    public void testTimeout() throws Exception {
        try (StaticServer server =
                new StaticServer(
                    new BaseServerConfigBuilder(Configs.baseConfig())
                        .workers(2 + 2)
                        .build());
            SharedConnectingIOReactor reactor = new SharedConnectingIOReactor(
                Configs.baseConfig(),
                Configs.dnsConfig());
            AsyncClient client1 =
                new AsyncClient(
                    reactor,
                    new HttpTargetConfigBuilder(Configs.targetConfig())
                        .timeout((int) TIMEOUT1)
                        .build()))
        {
            server.add(URI0, HttpStatus.SC_ACCEPTED);
            server.add(
                URI1,
                new SlowpokeHttpResource(
                    new StaticHttpItem(HttpStatus.SC_FORBIDDEN),
                    TIMEOUT1 + (TIMEOUT1 >> 2)));
            server.add(
                URI2,
                new SlowpokeHttpResource(
                    new StaticHttpItem(HttpStatus.SC_NOT_FOUND),
                    TIMEOUT2 << 1));
            server.start();
            reactor.start();
            client1.start();
            try (AsyncClient client2 =
                    new AsyncClient(
                        reactor,
                        new HttpTargetConfigBuilder(Configs.targetConfig())
                            .timeout((int) TIMEOUT2)
                            .build()))
            {
                client2.start();

                // Check simplest case, just send two requests
                long start = System.currentTimeMillis();
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_ACCEPTED,
                    client1.execute(
                        server.host(),
                        new BasicAsyncRequestProducerGenerator(URI0),
                        BasicAsyncResponseConsumerFactory.INSTANCE,
                        EmptyFutureCallback.INSTANCE).get());
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_ACCEPTED,
                    client2.execute(
                        server.host(),
                        new BasicAsyncRequestProducerGenerator(URI0),
                        BasicAsyncResponseConsumerFactory.INSTANCE,
                        EmptyFutureCallback.INSTANCE).get());
                YandexAssert.assertLess(
                    TIMEOUT1,
                    System.currentTimeMillis() - start);

                // Check that first client will fail, while second succeed
                start = System.currentTimeMillis();
                Future<HttpResponse> future1 = client1.execute(
                    server.host(),
                    new BasicAsyncRequestProducerGenerator(URI1),
                    BasicAsyncResponseConsumerFactory.INSTANCE,
                    EmptyFutureCallback.INSTANCE);
                Future<HttpResponse> future2 = client2.execute(
                    server.host(),
                    new BasicAsyncRequestProducerGenerator(URI1),
                    BasicAsyncResponseConsumerFactory.INSTANCE,
                    EmptyFutureCallback.INSTANCE);
                try {
                    future1.get();
                    Assert.fail();
                } catch (ExecutionException e) {
                    YandexAssert.assertInstanceOf(
                        SocketTimeoutException.class,
                        e.getCause());
                }
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_FORBIDDEN,
                    future2.get());
                long timeSpent = System.currentTimeMillis() - start;
                YandexAssert.assertGreater(TIMEOUT1, timeSpent);
                YandexAssert.assertLess(TIMEOUT2, timeSpent);

                // Check that both clients fail on long response
                start = System.currentTimeMillis();
                future1 = client1.execute(
                    server.host(),
                    new BasicAsyncRequestProducerGenerator(URI2),
                    BasicAsyncResponseConsumerFactory.INSTANCE,
                    EmptyFutureCallback.INSTANCE);
                future2 = client2.execute(
                    server.host(),
                    new BasicAsyncRequestProducerGenerator(URI2),
                    BasicAsyncResponseConsumerFactory.INSTANCE,
                    EmptyFutureCallback.INSTANCE);
                try {
                    future1.get();
                    Assert.fail();
                } catch (ExecutionException e) {
                    YandexAssert.assertInstanceOf(
                        SocketTimeoutException.class,
                        e.getCause());
                }
                try {
                    future2.get();
                    Assert.fail();
                } catch (ExecutionException e) {
                    YandexAssert.assertInstanceOf(
                        SocketTimeoutException.class,
                        e.getCause());
                }
                YandexAssert.assertGreater(
                    TIMEOUT2,
                    System.currentTimeMillis() - start);
            }
            // Check that reactor is not shutted down by second client
            HttpAssert.assertStatusCode(
                HttpStatus.SC_ACCEPTED,
                client1.execute(
                    server.host(),
                    new BasicAsyncRequestProducerGenerator(URI0),
                    BasicAsyncResponseConsumerFactory.INSTANCE,
                    EmptyFutureCallback.INSTANCE).get());
        }
    }
}

