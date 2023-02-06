package ru.yandex.http.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import java.util.logging.Level;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.nio.protocol.HttpAsyncRequestProducer;
import org.apache.http.protocol.HTTP;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.collection.Pattern;
import ru.yandex.http.config.HttpTargetConfigBuilder;
import ru.yandex.http.util.EmptyFutureCallback;
import ru.yandex.http.util.YandexHttpStatus;
import ru.yandex.http.util.nio.BasicAsyncRequestProducer;
import ru.yandex.http.util.nio.BasicAsyncRequestProducerGenerator;
import ru.yandex.http.util.nio.BasicAsyncResponseConsumerFactory;
import ru.yandex.http.util.nio.client.AsyncClient;
import ru.yandex.http.util.nio.client.SharedConnectingIOReactor;
import ru.yandex.http.util.request.RequestHandlerMapper;
import ru.yandex.http.util.server.HttpServer;
import ru.yandex.http.util.server.ImmutableBaseServerConfig;
import ru.yandex.test.util.YandexAssert;
import ru.yandex.util.timesource.TimeSource;

public abstract class HttpServerLimiterTest<T> extends HttpServerTestBase<T> {
    @Test
    public void testLimiterConcurrency() throws Exception {
        try (HttpServer<ImmutableBaseServerConfig, T> server =
                server(
                    config(
                        "[limiter]\n"
                        + "concurrency = 0\n"
                        + "[limiter./lim1]\n"
                        + "concurrency = 1\n"
                        + "[limiter./lim2]\n"
                        + "concurrency = 2\n"
                        + "[limiter./unlim]\n"
                        + "[limiter./code202]\n"
                        + "concurrency =0\n"
                        + "error-status-code = 202\n").build());
            SharedConnectingIOReactor reactor = new SharedConnectingIOReactor(
                Configs.baseConfig(),
                Configs.dnsConfig());
            AsyncClient client =
                new AsyncClient(reactor, Configs.targetConfig(clientBc())))
        {
            String any = "/any";
            String lim1 = "/lim1";
            String lim2 = "/lim2";
            String unlim = "/unlim";
            String code202 = "/code202";
            final int sleep = 3000;
            final int lowSleep = 300;
            server.register(
                new Pattern<>(any, false),
                createSlowpokeHandler(createDummyHandler(), sleep));
            server.register(
                new Pattern<>(lim1, false),
                createSlowpokeHandler(createDummyHandler(), sleep));
            server.register(
                new Pattern<>(lim2, false),
                createSlowpokeHandler(createDummyHandler(), sleep));
            server.register(
                new Pattern<>(unlim, false),
                createSlowpokeHandler(createDummyHandler(), sleep));
            server.register(
                new Pattern<>(code202, false),
                createSlowpokeHandler(createDummyHandler(), sleep));
            server.start();
            reactor.start();
            client.start();

            //test root (default) handler; concurrency = 0
            Future<HttpResponse> first = client.execute(
                server.host(),
                new BasicAsyncRequestProducerGenerator(any),
                BasicAsyncResponseConsumerFactory.INSTANCE,
                EmptyFutureCallback.INSTANCE);
            HttpResponse response = first.get();
            HttpAssert.assertStatusCode(
                YandexHttpStatus.SC_TOO_MANY_REQUESTS,
                response);

            //test /lim1 handler; concurrency = 1
            first = client.execute(
                server.host(),
                new BasicAsyncRequestProducerGenerator(lim1),
                BasicAsyncResponseConsumerFactory.INSTANCE,
                EmptyFutureCallback.INSTANCE);
            Thread.sleep(lowSleep);
            Future<HttpResponse> second = client.execute(
                server.host(),
                new BasicAsyncRequestProducerGenerator(lim1),
                BasicAsyncResponseConsumerFactory.INSTANCE,
                EmptyFutureCallback.INSTANCE);
            HttpResponse response1 = first.get();
            HttpResponse response2 = second.get();
            HttpAssert.assertStatusCode(
                YandexHttpStatus.SC_OK,
                response1);
            HttpAssert.assertStatusCode(
                YandexHttpStatus.SC_TOO_MANY_REQUESTS,
                response2);

            //test /lim2 handler; concurrency = 2
            first = client.execute(
                server.host(),
                new BasicAsyncRequestProducerGenerator(lim2),
                BasicAsyncResponseConsumerFactory.INSTANCE,
                EmptyFutureCallback.INSTANCE);
            Thread.sleep(lowSleep);
            second = client.execute(
                server.host(),
                new BasicAsyncRequestProducerGenerator(lim2),
                BasicAsyncResponseConsumerFactory.INSTANCE,
                EmptyFutureCallback.INSTANCE);
            Thread.sleep(lowSleep);
            Future<HttpResponse> third = client.execute(
                server.host(),
                new BasicAsyncRequestProducerGenerator(lim2),
                BasicAsyncResponseConsumerFactory.INSTANCE,
                EmptyFutureCallback.INSTANCE);
            response1 = first.get();
            response2 = second.get();
            HttpResponse response3 = third.get();
            HttpAssert.assertStatusCode(
                YandexHttpStatus.SC_OK,
                response1);
            HttpAssert.assertStatusCode(
                YandexHttpStatus.SC_OK,
                response2);
            HttpAssert.assertStatusCode(
                YandexHttpStatus.SC_TOO_MANY_REQUESTS,
                response3);

            Thread.sleep(lowSleep);

            //test /lim2 handler again (regress); concurrency = 2
            first = client.execute(
                server.host(),
                new BasicAsyncRequestProducerGenerator(lim2),
                BasicAsyncResponseConsumerFactory.INSTANCE,
                EmptyFutureCallback.INSTANCE);
            Thread.sleep(lowSleep);
            second = client.execute(
                server.host(),
                new BasicAsyncRequestProducerGenerator(lim2),
                BasicAsyncResponseConsumerFactory.INSTANCE,
                EmptyFutureCallback.INSTANCE);
            Thread.sleep(lowSleep);
            third = client.execute(
                server.host(),
                new BasicAsyncRequestProducerGenerator(lim2),
                BasicAsyncResponseConsumerFactory.INSTANCE,
                EmptyFutureCallback.INSTANCE);
            response1 = first.get();
            response2 = second.get();
            response3 = third.get();
            HttpAssert.assertStatusCode(
                YandexHttpStatus.SC_OK,
                response1);
            HttpAssert.assertStatusCode(
                YandexHttpStatus.SC_OK,
                response2);
            HttpAssert.assertStatusCode(
                YandexHttpStatus.SC_TOO_MANY_REQUESTS,
                response3);

            //test /unlim handler; concurrency = -1
            first = client.execute(
                server.host(),
                new BasicAsyncRequestProducerGenerator(unlim),
                BasicAsyncResponseConsumerFactory.INSTANCE,
                EmptyFutureCallback.INSTANCE);
            second = client.execute(
                server.host(),
                new BasicAsyncRequestProducerGenerator(unlim),
                BasicAsyncResponseConsumerFactory.INSTANCE,
                EmptyFutureCallback.INSTANCE);
            third = client.execute(
                server.host(),
                new BasicAsyncRequestProducerGenerator(unlim),
                BasicAsyncResponseConsumerFactory.INSTANCE,
                EmptyFutureCallback.INSTANCE);
            response1 = first.get();
            response2 = second.get();
            response3 = third.get();
            HttpAssert.assertStatusCode(
                YandexHttpStatus.SC_OK,
                response1);
            HttpAssert.assertStatusCode(
                YandexHttpStatus.SC_OK,
                response2);
            HttpAssert.assertStatusCode(
                YandexHttpStatus.SC_OK,
                response3);

            //test /code201 handler; concurrency = 0
            first = client.execute(
                server.host(),
                new BasicAsyncRequestProducerGenerator(code202),
                BasicAsyncResponseConsumerFactory.INSTANCE,
                EmptyFutureCallback.INSTANCE);
            response1 = first.get();
            HttpAssert.assertStatusCode(
                YandexHttpStatus.SC_ACCEPTED,
                response1);
        }
    }

    @Test
    public void testLimiterMemoryLimit() throws Exception {
        final long limit = 100L;
        try (HttpServer<ImmutableBaseServerConfig, T> server =
                server(
                    config(
                        "[limiter]\nper-key-minimal-concurrency = 2\n"
                        + "key = get-or-null(key)\nmemory-limit = " + limit
                        + "\nstater-prefix = memory-limiter").build());
            SharedConnectingIOReactor reactor = new SharedConnectingIOReactor(
                Configs.baseConfig(),
                Configs.dnsConfig());
            AsyncClient client =
                new AsyncClient(reactor, Configs.targetConfig(clientBc()));
            CloseableHttpClient syncClient =
                Configs.createDefaultClient(clientBc()))
        {
            final long sleep = 4000L;
            long smallSleep = sleep >> 3;
            server.register(
                new Pattern<>(URI, false),
                createSlowpokeHandler(createDummyHandler(), sleep));
            byte[] half = new byte[(int) (limit >> 1)];
            byte[] big = new byte[(int) (limit << 1)];
            server.start();
            reactor.start();
            client.start();
            String maxMemorySignal = "memory-limiter-max-memory_axxx";
            String currentMemorySignal = "memory-limiter-current-memory_axxx";
            String currentRequestsSignal =
                "memory-limiter-current-requests_axxx";

            String bigRequestHeaders =
                RequestHandlerMapper.POST + ' ' + PING
                + " HTTP/1.1\r\nContent-Length: " + big.length
                + "\r\n\r\n";
            String request =
                RequestHandlerMapper.POST + ' ' + PING
                + " HTTP/1.1\r\nContent-Length: 5000\r\n\r\nHello!";
            String request9000 =
                RequestHandlerMapper.POST + ' ' + PING
                + "?9000 HTTP/1.1\r\nContent-Length: 9000\r\n\r\nHello!";
            String chunked =
                RequestHandlerMapper.POST + ' ' + PING
                + "?chunked HTTP/1.1\r\nTransfer-Encoding: chunked\r\n\r\n"
                + "4\r\naaaa\r\n\r\n";
            String key1Uri = URI + "?key=1";
            String key2Uri = URI + "?key=2";
            for (int i = 0; i <= 2; ++i) {
                server.logger().info("Big request limiter test #" + i);
                // Big requests is only one, so it will pass
                Future<HttpResponse> first = client.execute(
                    server.host(),
                    new BasicAsyncRequestProducerGenerator(
                        URI,
                        big,
                        ContentType.TEXT_PLAIN),
                    BasicAsyncResponseConsumerFactory.INSTANCE,
                    EmptyFutureCallback.INSTANCE);
                Thread.sleep(smallSleep);
                server.logger().info("Checking big request stats");
                String stats = HttpAssert.stats(syncClient, server.host());
                HttpAssert.assertStat(
                    maxMemorySignal,
                    Long.toString(limit),
                    stats);
                HttpAssert.assertStat(
                    currentMemorySignal,
                    Integer.toString(big.length),
                    stats);
                // Big request + /stat request
                HttpAssert.assertStat(
                    currentRequestsSignal,
                    Integer.toString(2),
                    stats);

                server.logger().info("Sending small request");
                // Will be rejected, because limit already exceeded
                Future<HttpResponse> second = client.execute(
                    server.host(),
                    new BasicAsyncRequestProducerGenerator(
                        URI,
                        half,
                        ContentType.TEXT_PLAIN),
                    BasicAsyncResponseConsumerFactory.INSTANCE,
                    EmptyFutureCallback.INSTANCE);
                // First two request for each key will be accepted
                // Third one will be rejected
                Future<HttpResponse> key11 = client.execute(
                    server.host(),
                    new BasicAsyncRequestProducerGenerator(
                        key1Uri,
                        half,
                        ContentType.TEXT_PLAIN),
                    BasicAsyncResponseConsumerFactory.INSTANCE,
                    EmptyFutureCallback.INSTANCE);
                Future<HttpResponse> key12 = client.execute(
                    server.host(),
                    new BasicAsyncRequestProducerGenerator(
                        key1Uri,
                        half,
                        ContentType.TEXT_PLAIN),
                    BasicAsyncResponseConsumerFactory.INSTANCE,
                    EmptyFutureCallback.INSTANCE);
                Future<HttpResponse> key21 = client.execute(
                    server.host(),
                    new BasicAsyncRequestProducerGenerator(
                        key2Uri,
                        half,
                        ContentType.TEXT_PLAIN),
                    BasicAsyncResponseConsumerFactory.INSTANCE,
                    EmptyFutureCallback.INSTANCE);
                Future<HttpResponse> key22 = client.execute(
                    server.host(),
                    new BasicAsyncRequestProducerGenerator(
                        key2Uri,
                        half,
                        ContentType.TEXT_PLAIN),
                    BasicAsyncResponseConsumerFactory.INSTANCE,
                    EmptyFutureCallback.INSTANCE);
                Thread.sleep(smallSleep);
                stats = HttpAssert.stats(syncClient, server.host());
                HttpAssert.assertStat(
                    "active-connections_axxx",
                    // Big request + 2 key1 requests + 2 key2 requests + /stat
                    Integer.toString(1 + 2 + 2 + 1),
                    stats);
                Thread.sleep(smallSleep);
                Future<HttpResponse> key13 = client.execute(
                    server.host(),
                    new BasicAsyncRequestProducerGenerator(
                        key1Uri,
                        half,
                        ContentType.TEXT_PLAIN),
                    BasicAsyncResponseConsumerFactory.INSTANCE,
                    EmptyFutureCallback.INSTANCE);
                Future<HttpResponse> key23 = client.execute(
                    server.host(),
                    new BasicAsyncRequestProducerGenerator(
                        key2Uri,
                        half,
                        ContentType.TEXT_PLAIN),
                    BasicAsyncResponseConsumerFactory.INSTANCE,
                    EmptyFutureCallback.INSTANCE);
                HttpAssert.assertStatusCode(
                    YandexHttpStatus.SC_TOO_MANY_REQUESTS,
                    second.get());
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, key11.get());
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, key12.get());
                HttpAssert.assertStatusCode(
                    YandexHttpStatus.SC_TOO_MANY_REQUESTS,
                    key13.get());
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, key21.get());
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, key22.get());
                HttpAssert.assertStatusCode(
                    YandexHttpStatus.SC_TOO_MANY_REQUESTS,
                    key23.get());
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, first.get());

                server.logger().info("Big requests finalized, making trash");
                // Send request that will be prematurely terminated
                try (Socket socket = new Socket(LOCALHOST, server.port())) {
                    socket.setTcpNoDelay(true);
                    Socket dataStreamSocket;
                    if (server.sslContext() == null) {
                        dataStreamSocket = socket;
                    } else {
                        dataStreamSocket =
                            sslConnectionSocketFactory.createLayeredSocket(
                                socket,
                                LOCALHOST,
                                server.port(),
                                null);
                    }
                    dataStreamSocket.getOutputStream().write(
                        request.getBytes(StandardCharsets.UTF_8));
                    dataStreamSocket.getOutputStream().flush();
                    socket.getOutputStream().write(0);
                    socket.getOutputStream().flush();
                }
                Thread.sleep(smallSleep);

                if (https()) {
                    // HTTPS request which will be terminated badly
                    Socket socket = new Socket(LOCALHOST, server.port());
                    try (Socket sslSocket =
                            sslConnectionSocketFactory.createLayeredSocket(
                                socket,
                                LOCALHOST,
                                server.port(),
                                null))
                    {
                        server.logger().info("Ruining SSL");
                        sslSocket.getOutputStream().write(
                            request9000.getBytes(StandardCharsets.UTF_8));
                        sslSocket.getOutputStream().flush();
                        Thread.sleep(TIMEOUT >> 2);
                        socket.getOutputStream().write(
                            request9000.getBytes(StandardCharsets.UTF_8));
                        socket.getOutputStream().flush();
                        Thread.sleep(TIMEOUT >> 2);
                        socket.close();
                        Thread.sleep(TIMEOUT >> 2);
                    } catch (IOException e) {
                    }
                }
                Thread.sleep(defaultConfig.metricsTimeFrame());
            }

            for (int i = 0; i <= 2; ++i) {
                System.gc();
                System.runFinalization();
                yield();
                server.logger().info("Half requests limiter test #" + i);
                // Two half requests will pass, third one will be rejected, big
                // request will be rejected too
                Future<HttpResponse> first = client.execute(
                    server.host(),
                    new BasicAsyncRequestProducerGenerator(
                        URI,
                        half,
                        ContentType.TEXT_PLAIN),
                    BasicAsyncResponseConsumerFactory.INSTANCE,
                    EmptyFutureCallback.INSTANCE);
                Thread.sleep(smallSleep);
                try (Socket socket = connectTo(server)) {
                    socket.setTcpNoDelay(true);
                    socket.getOutputStream().write(
                        bigRequestHeaders.getBytes(StandardCharsets.UTF_8));
                    socket.getOutputStream().flush();
                    Thread.sleep(smallSleep);
                    Future<HttpResponse> second = client.execute(
                        server.host(),
                        new BasicAsyncRequestProducerGenerator(
                            URI,
                            half,
                            ContentType.TEXT_PLAIN),
                        BasicAsyncResponseConsumerFactory.INSTANCE,
                        EmptyFutureCallback.INSTANCE);
                    Thread.sleep(smallSleep);
                    server.logger().info("Checking half requests stats");
                    String stats = HttpAssert.stats(syncClient, server.host());
                    HttpAssert.assertStat(
                        maxMemorySignal,
                        Long.toString(limit),
                        stats);
                    HttpAssert.assertStat(
                        currentMemorySignal,
                        Integer.toString(half.length << 1),
                        stats);
                    // Two half requests + /stat request
                    HttpAssert.assertStat(
                        currentRequestsSignal,
                        Integer.toString(2 + 1),
                        stats);
                    // Will be rejected, because limit already exceeded
                    Future<HttpResponse> third = client.execute(
                        server.host(),
                        new BasicAsyncRequestProducerGenerator(
                            URI,
                            half,
                            ContentType.TEXT_PLAIN),
                        BasicAsyncResponseConsumerFactory.INSTANCE,
                        EmptyFutureCallback.INSTANCE);
                    boolean passed = false;
                    try (BufferedReader reader =
                            new BufferedReader(
                                new InputStreamReader(
                                    socket.getInputStream(),
                                    StandardCharsets.UTF_8)))
                    {
                        socket.getOutputStream().write(big);
                        socket.getOutputStream().flush();
                        YandexAssert.assertStartsWith(
                            "HTTP/1.1 "
                            + YandexHttpStatus.SC_TOO_MANY_REQUESTS,
                            reader.readLine());
                        while (true) {
                            String line = reader.readLine();
                            Assert.assertNotNull(line);
                            if (line.isEmpty()) {
                                break;
                            }
                        }
                        passed = true;
                    } catch (IOException e) {
                        if (passed) {
                            logger.log(
                                Level.WARNING,
                                "Test is passed, but connection was "
                                + "terminated abnormally",
                                e);
                        } else {
                            throw e;
                        }
                    }
                    HttpAssert.assertStatusCode(
                        YandexHttpStatus.SC_TOO_MANY_REQUESTS,
                        third.get());
                    HttpAssert.assertStatusCode(HttpStatus.SC_OK, first.get());
                    HttpAssert.assertStatusCode(
                        HttpStatus.SC_OK,
                        second.get());
                }

                server.logger().info("Making bad things");
                // Send request that will be closed by server
                Socket socket = connectTo(server);
                socket.getOutputStream().write(
                    request.getBytes(StandardCharsets.UTF_8));
                socket.getOutputStream().flush();
                Thread.sleep(TIMEOUT << 1);

                server.logger().info("Making bad chunked thing");
                // And malformed chunked request
                socket = connectTo(server);
                socket.getOutputStream().write(
                    chunked.getBytes(StandardCharsets.UTF_8));
                socket.getOutputStream().flush();
                Thread.sleep(defaultConfig.metricsTimeFrame());
            }
        }
    }

    @Test
    public void testLimiterReleaseOnConnectionClose() throws Exception {
        try (HttpServer<ImmutableBaseServerConfig, T> server =
                server(
                    config(
                        "[limiter]\nconcurrency = 99\nstater-prefix = limiter")
                    .build()))
        {
            server.register(
                new Pattern<>(URI, false),
                createJsonNormalizingHandler());
            server.start();
            String currentRequestsSignal = "limiter-current-requests_axxx";
            String request =
                RequestHandlerMapper.POST + ' ' + URI
                + " HTTP/1.0\r\n\r\n\"Hello!\"";
            final long sleep = 300L;
            final int socketsCount = 5;
            final int iterations = 10;
            for (int j = 0; j < iterations; ++j) {
                server.logger().info(ITERATION + j);
                Socket[] sockets = new Socket[socketsCount];
                for (int i = 0; i < socketsCount; ++i) {
                    Socket socket = connectTo(server);
                    socket.setTcpNoDelay(true);
                    socket.getOutputStream().write(
                        request.getBytes(StandardCharsets.UTF_8));
                    socket.getOutputStream().flush();
                    socket.setSoLinger(true, 0);
                    sockets[i] = socket;
                }
                Thread.sleep(sleep);
                System.gc();
                for (Socket socket: sockets) {
                    socket.close();
                }
                Thread.sleep(sleep);
                // /stat request
                HttpAssert.assertStat(
                    currentRequestsSignal,
                    Integer.toString(1),
                    server);
            }
        }
    }

    @Test
    public void testLimiterReleaseOnCloseWait() throws Exception {
        try (HttpServer<ImmutableBaseServerConfig, T> server =
                server(
                    config(
                        "[limiter]\nconcurrency = 99\nstater-prefix = lmtr")
                    .build()))
        {
            server.register(
                new Pattern<>(URI, false),
                createJsonNormalizingHandler());
            server.start();
            String currentRequestsSignal = "lmtr-current-requests_axxx";
            final int len = 200000;
            StringBuilder sb = new StringBuilder(RequestHandlerMapper.POST);
            sb.append(' ');
            sb.append(URI);
            sb.append(" HTTP/1.1\r\nContent-Length: 10000002\r\n\r\n");
            sb.append('"');
            for (int i = 0; i < len; ++i) {
                sb.append(
                    "12345678901234567890123456789012345678901234567890");
            }
            sb.append('"');
            byte[] request = new String(sb).getBytes(StandardCharsets.UTF_8);
            final int iterations = 3;
            for (int j = 0; j < iterations; ++j) {
                server.logger().info(ITERATION + j);
                boolean ok = false;
                try (Socket socket = connectTo(server)) {
                    socket.setTcpNoDelay(true);
                    socket.getOutputStream().write(request);
                    socket.getOutputStream().flush();
                    Thread.sleep(TIMEOUT << 1);
                    HttpAssert.assertStat(
                        currentRequestsSignal,
                        Integer.toString(1),
                        server);
                    ok = true;
                } catch (IOException e) {
                    if (!ok) {
                        throw e;
                    }
                }
                Thread.sleep(TIMEOUT >> 2);
            }
        }
    }

    @Test
    public void testLimiterPerKeyConcurrency() throws Exception {
        try (HttpServer<ImmutableBaseServerConfig, T> server =
                server(
                    config(
                        "[limiter./lim1cgi]\n"
                        + "key = get(uid)\n"
                        + "per-key-concurrency = 1").build());
            SharedConnectingIOReactor reactor = new SharedConnectingIOReactor(
                Configs.baseConfig(),
                Configs.dnsConfig());
            AsyncClient client =
                new AsyncClient(reactor, Configs.targetConfig(clientBc())))
        {
            String lim1 = "/lim1cgi";
            String uid = "?uid=7397000";
            final int sleep = 3000;
            final int lowSleep = 1000;
            server.register(
                new Pattern<>(lim1, false),
                createSlowpokeHandler(createDummyHandler(), sleep));
            server.start();
            reactor.start();
            client.start();

            //loop for testing limiter leak
            for (int t = 0; t < 2 + 1; t++) {
                logger.info("Iteration #" + t);
                //test /lim1 handler; concurrency = 1
                Future<HttpResponse> first = client.execute(
                    server.host(),
                    new BasicAsyncRequestProducerGenerator(
                        lim1 + uid + "&req1"),
                    BasicAsyncResponseConsumerFactory.INSTANCE,
                    EmptyFutureCallback.INSTANCE);
                Thread.sleep(lowSleep);
                Future<HttpResponse> second = client.execute(
                    server.host(),
                    new BasicAsyncRequestProducerGenerator(
                        lim1 + uid + "&req2"),
                    BasicAsyncResponseConsumerFactory.INSTANCE,
                    EmptyFutureCallback.INSTANCE);
                HttpResponse response1 = first.get();
                HttpResponse response2 = second.get();
                HttpAssert.assertStatusCode(
                    YandexHttpStatus.SC_OK,
                    response1);
                HttpAssert.assertStatusCode(
                    YandexHttpStatus.SC_TOO_MANY_REQUESTS,
                    response2);
                Thread.sleep(sleep << 1);
            }
        }
    }

    @Test
    public void testLimiterRequestsLoadTest() throws Exception {
        if (https()) {
            // HTTPS is too slow for this test
            return;
        }
        final int workers = 20;
        final int count = 1000;
        ImmutableBaseServerConfig config =
            config(
                LOGGER_OFF
                + "[limiter]\nconcurrency = " + (workers >> 1)
                + "\nstater-prefix = requests-load")
            .connections(count << 1)
            .workers(workers)
            .build();
        try (HttpServer<ImmutableBaseServerConfig, T> server = server(config);
            SharedConnectingIOReactor reactor = new SharedConnectingIOReactor(
                config,
                Configs.dnsConfig());
            AsyncClient client = new AsyncClient(
                reactor,
                new HttpTargetConfigBuilder(Configs.targetConfig(clientBc()))
                    .connections(count << 1)
                    .build()))
        {
            server.start();
            reactor.start();
            client.start();

            HttpHost host = server.host();
            Supplier<? extends HttpAsyncRequestProducer> requestSupplier =
                () -> new BasicAsyncRequestProducer(
                    host,
                    new BasicHttpRequest("GET", PING));
            for (int j = 0; j <= 2; ++j) {
                if (j > 0) {
                    Thread.sleep(TIMEOUT + (TIMEOUT >> 2));
                }
                List<Future<HttpResponse>> requests = new ArrayList<>(count);
                for (int i = 0; i < count; ++i) {
                    requests.add(
                        client.execute(
                            requestSupplier,
                            BasicAsyncResponseConsumerFactory.INSTANCE,
                            EmptyFutureCallback.INSTANCE));
                    if ((count % workers) == 0) {
                        yield();
                    }
                }
                for (Future<HttpResponse> request: requests) {
                    request.get();
                }
                HttpAssert.assertStat(
                    "requests-load-current-requests_axxx",
                    Integer.toString(1),
                    server);
            }
        }
    }

    @Test
    public void testConnectionsLimit() throws Exception {
        try (HttpServer<ImmutableBaseServerConfig, T> server =
                server(
                    config(
                        "server.max-connections-per-host = 2")
                        .build());
            SharedConnectingIOReactor reactor = new SharedConnectingIOReactor(
                Configs.baseConfig(),
                Configs.dnsConfig());
            AsyncClient client =
                new AsyncClient(reactor, Configs.targetConfig()))
        {
            server.start();
            reactor.start();
            client.start();

            final long sleep = 2000L;
            server.register(
                new Pattern<>(URI, false),
                createSlowpokeHandler(createDummyHandler(), sleep));

            Future<HttpResponse> first = client.execute(
                server.host(),
                new BasicAsyncRequestProducerGenerator(URI),
                BasicAsyncResponseConsumerFactory.INSTANCE,
                EmptyFutureCallback.INSTANCE);
            Future<HttpResponse> second = client.execute(
                server.host(),
                new BasicAsyncRequestProducerGenerator(URI),
                BasicAsyncResponseConsumerFactory.INSTANCE,
                EmptyFutureCallback.INSTANCE);
            Thread.sleep(sleep >> 2);

            long start = TimeSource.INSTANCE.currentTimeMillis();
            Future<HttpResponse> third = client.execute(
                server.host(),
                new BasicAsyncRequestProducerGenerator(PING),
                BasicAsyncResponseConsumerFactory.INSTANCE,
                EmptyFutureCallback.INSTANCE);
            HttpResponse response = third.get();
            long end = TimeSource.INSTANCE.currentTimeMillis();

            HttpAssert.assertStatusCode(HttpStatus.SC_OK, first.get());
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, second.get());
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);

            YandexAssert.assertGreater(
                sleep >> 1,
                end - start);
        }
    }

    @Test
    public void testConnectionsRejectLimit() throws Exception {
        try (HttpServer<ImmutableBaseServerConfig, T> server =
                server(
                    config(
                        "server.connections = 2\n"
                        + "server.reject-connections-over-limit = true")
                        .build());
            SharedConnectingIOReactor reactor = new SharedConnectingIOReactor(
                Configs.baseConfig(),
                Configs.dnsConfig());
            AsyncClient client =
                new AsyncClient(reactor, Configs.targetConfig()))
        {
            server.start();
            reactor.start();
            client.start();

            final long sleep = 4000L;
            server.register(
                new Pattern<>(URI, false),
                createSlowpokeHandler(createDummyHandler(), sleep));

            BasicAsyncRequestProducerGenerator producerGenerator =
                new BasicAsyncRequestProducerGenerator(PING);
            producerGenerator.addHeader(
                HTTP.CONN_DIRECTIVE,
                HTTP.CONN_CLOSE);
            try {
                client.execute(
                    server.host(),
                    producerGenerator,
                    BasicAsyncResponseConsumerFactory.INSTANCE,
                    EmptyFutureCallback.INSTANCE)
                    .get();
            } catch (Exception e) {
                // Just a warmup
            }
            Thread.sleep(sleep >> 2);

            Future<HttpResponse> first = client.execute(
                server.host(),
                new BasicAsyncRequestProducerGenerator(URI),
                BasicAsyncResponseConsumerFactory.INSTANCE,
                EmptyFutureCallback.INSTANCE);
            logger.info("First request spawned");
            Future<HttpResponse> second = client.execute(
                server.host(),
                new BasicAsyncRequestProducerGenerator(URI),
                BasicAsyncResponseConsumerFactory.INSTANCE,
                EmptyFutureCallback.INSTANCE);
            logger.info("Second request spawned");
            Thread.sleep(sleep >> 2);

            long start = TimeSource.INSTANCE.currentTimeMillis();
            Future<HttpResponse> third = client.execute(
                server.host(),
                new BasicAsyncRequestProducerGenerator(PING),
                BasicAsyncResponseConsumerFactory.INSTANCE,
                EmptyFutureCallback.INSTANCE);
            logger.info("Third request spawned");
            try {
                third.get();
                Assert.fail();
            } catch (ExecutionException e) {
                YandexAssert.assertInstanceOf(
                    IOException.class,
                    e.getCause());
            }
            long end = TimeSource.INSTANCE.currentTimeMillis();

            HttpAssert.assertStatusCode(HttpStatus.SC_OK, first.get());
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, second.get());

            YandexAssert.assertLess(
                sleep >> 1,
                end - start);
        }
    }
}

