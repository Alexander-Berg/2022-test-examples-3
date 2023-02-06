package ru.yandex.search.proxy.universal;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.collection.Pattern;
import ru.yandex.http.config.HttpTargetConfigBuilder;
import ru.yandex.http.proxy.AbstractProxySessionCallback;
import ru.yandex.http.proxy.ProxyRequestHandler;
import ru.yandex.http.proxy.ProxySession;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.ExpectingHttpItem;
import ru.yandex.http.test.HeaderHttpItem;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.test.NotImplementedHttpItem;
import ru.yandex.http.test.SlowpokeHttpItem;
import ru.yandex.http.test.StaticHttpItem;
import ru.yandex.http.test.StaticServer;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.http.util.EmptyFutureCallback;
import ru.yandex.http.util.YandexHeaders;
import ru.yandex.http.util.nio.BasicAsyncRequestProducerGenerator;
import ru.yandex.http.util.nio.client.AsyncClient;
import ru.yandex.http.util.nio.client.SharedConnectingIOReactor;
import ru.yandex.json.dom.JsonObject;
import ru.yandex.parser.searchmap.User;
import ru.yandex.search.prefix.LongPrefix;
import ru.yandex.search.proxy.SearchProxyParams;
import ru.yandex.search.request.PostfilterOperator;
import ru.yandex.search.request.SearchRequestBuilder;
import ru.yandex.search.request.SearchRequestQueryBuilder;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

public class UniversalSearchProxyTest extends TestBase {
    private static final String SEQUENTIAL = UniversalSearchProxy.SEQUENTIAL;
    private static final String PARALLEL = UniversalSearchProxy.PARALLEL;
    private static final String SERVICE = UniversalSearchProxyCluster.SERVICE;
    private static final String URI = "/uri?param=value";
    private static final String BODY = "Hello, world";
    private static final String SHARD = "123";
    private static final String PRODUCER_URI =
        "/_status?service=" + UniversalSearchProxyCluster.SERVICE
        + "&prefix=" + SHARD + "&all&json-type=dollar";
    private static final String PRODUCER_LOCALHOST_BEST =
        "[{\"localhost\":6},{\""
        + UniversalSearchProxyCluster.FAKE_HOST1.getHostName()
        + "\":4},{\""
        + UniversalSearchProxyCluster.FAKE_HOST2.getHostName() + "\":2}]";
    private static final String PRODUCER_LOCALHOST_EQUALS =
        "[{\"loc1.al.host.localdomain\":7},{\"localhost.localdomain\":7},"
        + "{\"localhost\":7},{\""
        + UniversalSearchProxyCluster.FAKE_HOST1.getHostName()
        + "\":7},{\""
        + UniversalSearchProxyCluster.FAKE_HOST2.getHostName() + "\":7}]";
    private static final String PRODUCER_LOCALHOST_MIDDLE =
        "[{\"" + UniversalSearchProxyCluster.FAKE_HOST1.getHostName()
        + "\":20},{\"localhost\":6},{\""
        + UniversalSearchProxyCluster.FAKE_HOST2.getHostName() + "\":1}]";
    private static final String PRODUCER_EMPTY_HOSTS = "[]";
    private static final int TIMEOUT = UniversalSearchProxyCluster.TIMEOUT;

    @Test
    public void testSequential() throws Exception {
        try (UniversalSearchProxyCluster cluster =
                new UniversalSearchProxyCluster();
            SharedConnectingIOReactor reactor = new SharedConnectingIOReactor(
                Configs.baseConfig(),
                Configs.dnsConfig());
            AsyncClient client =
                new AsyncClient(reactor, Configs.targetConfig()))
        {
            cluster.producer().add(
                PRODUCER_URI,
                new SlowpokeHttpItem(
                    new StaticHttpItem(PRODUCER_LOCALHOST_BEST),
                    TIMEOUT >> 1),
                NotImplementedHttpItem.INSTANCE);
            String uri =
                URI + '&' + SearchProxyParams.SERVICE + '=' + SERVICE
                + '&' + SearchProxyParams.SHARD + '=' + SHARD;
            cluster.backend().add(
                uri,
                new SlowpokeHttpItem(
                    new StaticHttpItem(BODY),
                    TIMEOUT >> 1),
                NotImplementedHttpItem.INSTANCE);
            cluster.start();
            reactor.start();
            client.start();

            Future<HttpResponse> future = client.execute(
                cluster.proxy().host(),
                new BasicAsyncRequestProducerGenerator(SEQUENTIAL + uri),
                EmptyFutureCallback.INSTANCE);

            // Request should be somewhere in producer now
            // All access counters are 0
            Thread.sleep(TIMEOUT >> 2);
            Assert.assertEquals(
                0,
                cluster.producer().accessCount(PRODUCER_URI));
            Assert.assertEquals(0, cluster.backend().accessCount(uri));

            // Request was processed by producer, localhost selected as best
            // backend, and request should be somewhere inside backend
            Thread.sleep(TIMEOUT >> 1);
            Assert.assertEquals(
                1,
                cluster.producer().accessCount(PRODUCER_URI));
            Assert.assertEquals(0, cluster.backend().accessCount(uri));

            // All requests completed
            Thread.sleep(TIMEOUT >> 1);
            Assert.assertEquals(1, cluster.backend().accessCount(uri));
            HttpResponse response = future.get();
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            Assert.assertEquals(
                BODY,
                CharsetUtils.toString(response.getEntity()));
        }
    }

    @Test
    public void testSequentialCancelled() throws Exception {
        try (UniversalSearchProxyCluster cluster =
                new UniversalSearchProxyCluster();
            SharedConnectingIOReactor reactor = new SharedConnectingIOReactor(
                Configs.baseConfig(),
                Configs.dnsConfig());
            AsyncClient client =
                new AsyncClient(
                    reactor,
                    new HttpTargetConfigBuilder(Configs.targetConfig())
                        .timeout(TIMEOUT >> 2)
                        .build()))
        {
            cluster.producer().add(
                PRODUCER_URI,
                new SlowpokeHttpItem(
                    new StaticHttpItem(PRODUCER_LOCALHOST_BEST),
                    TIMEOUT >> 1),
                NotImplementedHttpItem.INSTANCE);
            String uri =
                URI + '&' + SearchProxyParams.SERVICE + '=' + SERVICE
                + '&' + SearchProxyParams.SHARD + '=' + SHARD;
            cluster.backend().add(
                uri,
                new StaticHttpItem(BODY),
                NotImplementedHttpItem.INSTANCE);
            cluster.start();
            reactor.start();
            client.start();

            try {
                client.execute(
                    cluster.proxy().host(),
                    new BasicAsyncRequestProducerGenerator(SEQUENTIAL + uri),
                    EmptyFutureCallback.INSTANCE)
                    .get();
                Assert.fail();
            } catch (ExecutionException e) {
                YandexAssert.assertInstanceOf(
                    SocketTimeoutException.class,
                    e.getCause());
            }

            // No requests should reach backend
            Thread.sleep(TIMEOUT);
            Assert.assertEquals(0, cluster.backend().accessCount(uri));
        }
    }

    @Test
    public void testSequentialLocalityShuffle() throws Exception {
        try (UniversalSearchProxyCluster cluster =
                new UniversalSearchProxyCluster(
                    true,
                    UniversalSearchProxyCluster.searchMapRule(
                        new HttpHost("loc1.al.host.localdomain", 1 + 1))
                        + UniversalSearchProxyCluster.searchMapRule(
                            new HttpHost("localhost.localdomain", 2 + 2)),
                    true);
            SharedConnectingIOReactor reactor = new SharedConnectingIOReactor(
                Configs.baseConfig(),
                Configs.dnsConfig());
            AsyncClient client =
                new AsyncClient(reactor, Configs.targetConfig()))
        {
            cluster.producer().add(
                PRODUCER_URI,
                new SlowpokeHttpItem(
                    new StaticHttpItem(PRODUCER_LOCALHOST_EQUALS),
                    TIMEOUT >> 1),
                NotImplementedHttpItem.INSTANCE);
            String uri =
                URI + '&' + SearchProxyParams.SERVICE + '=' + SERVICE
                + '&' + SearchProxyParams.SHARD + '=' + SHARD;
            cluster.backend().add(
                uri,
                new SlowpokeHttpItem(
                    new StaticHttpItem(BODY),
                    TIMEOUT >> 1),
                NotImplementedHttpItem.INSTANCE);
            cluster.start();
            reactor.start();
            client.start();

            Future<HttpResponse> future = client.execute(
                cluster.proxy().host(),
                new BasicAsyncRequestProducerGenerator(SEQUENTIAL + uri),
                EmptyFutureCallback.INSTANCE);

            // Request should be somewhere in producer now
            // All access counters are 0
            Thread.sleep(TIMEOUT >> 2);
            Assert.assertEquals(
                0,
                cluster.producer().accessCount(PRODUCER_URI));
            Assert.assertEquals(0, cluster.backend().accessCount(uri));

            // Request was processed by producer, localhost selected as best
            // backend, and request should be somewhere inside backend
            Thread.sleep(TIMEOUT >> 1);
            Assert.assertEquals(
                1,
                cluster.producer().accessCount(PRODUCER_URI));
            Assert.assertEquals(0, cluster.backend().accessCount(uri));

            // All requests completed
            Thread.sleep(TIMEOUT >> 1);
            Assert.assertEquals(1, cluster.backend().accessCount(uri));
            HttpResponse response = future.get();
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            Assert.assertEquals(
                BODY,
                CharsetUtils.toString(response.getEntity()));
        }
    }

    @Test
    public void testSequentialNoProducerHosts() throws Exception {
        try (UniversalSearchProxyCluster cluster =
                new UniversalSearchProxyCluster();
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.producer().add(PRODUCER_URI, PRODUCER_EMPTY_HOSTS);
            String uri =
                URI + '&' + SearchProxyParams.SERVICE + '=' + SERVICE
                + '&' + SearchProxyParams.PREFIX + '=' + SHARD;
            cluster.backend().add(uri, BODY);
            cluster.start();

            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(cluster.proxy().host() + SEQUENTIAL + uri)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_NOT_FOUND, response);
                YandexAssert.assertContains(
                    PRODUCER_EMPTY_HOSTS,
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testSequentialWithPos() throws Exception {
        try (UniversalSearchProxyCluster cluster =
                new UniversalSearchProxyCluster();
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.producer().add(PRODUCER_URI, PRODUCER_LOCALHOST_BEST);
            String uri =
                URI + '&' + SearchProxyParams.SERVICE + '=' + SERVICE
                + '&' + SearchProxyParams.PREFIX + '=' + SHARD;
            String requestBody = "Request body";
            cluster.backend().add(
                uri,
                new ExpectingHttpItem(
                    requestBody,
                    BODY));
            cluster.start();

            HttpPost post =
                new HttpPost(cluster.proxy().host() + SEQUENTIAL + uri);
            post.setEntity(
                new StringEntity(requestBody, ContentType.TEXT_PLAIN));
            post.addHeader(YandexHeaders.X_MINIMAL_POSITION, "5");
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    BODY,
                    CharsetUtils.toString(response.getEntity()));
            }

            HttpGet get =
                new HttpGet(cluster.proxy().host() + SEQUENTIAL + uri);
            get.addHeader(YandexHeaders.X_MINIMAL_POSITION, "7");
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_NOT_FOUND, response);
                YandexAssert.assertContains(
                    UniversalSearchProxyCluster.FAKE_HOST1.getHostName(),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testSequentialFailover() throws Exception {
        try (UniversalSearchProxyCluster cluster =
                new UniversalSearchProxyCluster();
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.producer().add(PRODUCER_URI, PRODUCER_LOCALHOST_BEST);
            // regular shuffle based on hashcode, which is not stable against rerun
            // using locality shuffle instead
            String uri =
                URI + '&' + SearchProxyParams.SERVICE + '=' + SERVICE
                + '&' + SearchProxyParams.PREFIX + '=' + SHARD + "&locality-shuffle=true";
            cluster.backend().add(uri, HttpStatus.SC_SERVICE_UNAVAILABLE);
            cluster.start();

            // Two hosts will be returned by producer
            // Both will cause errors
            HttpGet get =
                new HttpGet(cluster.proxy().host() + SEQUENTIAL + uri);
            get.addHeader(YandexHeaders.X_MINIMAL_POSITION, "2");
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_SERVICE_UNAVAILABLE,
                    response);
                YandexAssert.assertContains(
                    UnknownHostException.class.getSimpleName(),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testSequentialFailoverDelay() throws Exception {
        try (UniversalSearchProxyCluster cluster =
                new UniversalSearchProxyCluster();
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.producer().add(PRODUCER_URI, PRODUCER_LOCALHOST_BEST);
            String uri =
                URI + '&' + SearchProxyParams.SERVICE + '=' + SERVICE
                + '&' + SearchProxyParams.PREFIX + '=' + SHARD
                + '&' + SearchProxyParams.FAILOVER_DELAY + '='
                + (TIMEOUT >> 2);
            String errorText = "Go away!";
            cluster.backend().add(
                uri,
                new SlowpokeHttpItem(
                    new StaticHttpItem(
                        HttpStatus.SC_SERVICE_UNAVAILABLE,
                        errorText),
                    TIMEOUT >> 1),
                NotImplementedHttpItem.INSTANCE);
            cluster.start();

            // Two hosts will be returned by producer
            // Will failover to second before first done this
            HttpGet get =
                new HttpGet(cluster.proxy().host() + SEQUENTIAL + uri);
            get.addHeader(YandexHeaders.X_MINIMAL_POSITION, "3");
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_BAD_GATEWAY,
                    response);
                YandexAssert.assertContains(
                    errorText,
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testSequentialFailoverDelayHeader() throws Exception {
        try (UniversalSearchProxyCluster cluster =
                new UniversalSearchProxyCluster();
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.producer().add(PRODUCER_URI, PRODUCER_LOCALHOST_BEST);
            String uri =
                URI + '&' + SearchProxyParams.SERVICE + '=' + SERVICE
                + '&' + SearchProxyParams.PREFIX + '=' + SHARD
                + '&' + SearchProxyParams.MIN_POS + "=3";
            String errorText = "Go away n00b!";
            cluster.backend().add(
                uri,
                new SlowpokeHttpItem(
                    new StaticHttpItem(
                        HttpStatus.SC_SERVICE_UNAVAILABLE,
                        errorText),
                    TIMEOUT >> 1),
                NotImplementedHttpItem.INSTANCE);
            cluster.start();

            // Two hosts will be returned by producer
            // Will failover to second before first done this
            HttpGet get =
                new HttpGet(cluster.proxy().host() + SEQUENTIAL + uri);
            get.addHeader(
                YandexHeaders.X_SEARCH_FAILOVER_DELAY,
                Integer.toString(TIMEOUT >> 2));
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_BAD_GATEWAY,
                    response);
                YandexAssert.assertContains(
                    errorText,
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testParallelProducerRespondedFirst() throws Exception {
        try (UniversalSearchProxyCluster cluster =
                new UniversalSearchProxyCluster();
            SharedConnectingIOReactor reactor = new SharedConnectingIOReactor(
                Configs.baseConfig(),
                Configs.dnsConfig());
            AsyncClient client =
                new AsyncClient(reactor, Configs.targetConfig()))
        {
            cluster.producer().add(PRODUCER_URI, PRODUCER_LOCALHOST_BEST);
            cluster.backend().add(
                URI,
                new SlowpokeHttpItem(
                    new HeaderHttpItem(
                        new StaticHttpItem(BODY),
                        YandexHeaders.ZOO_QUEUE_ID,
                        "8"),
                    TIMEOUT >> 1),
                NotImplementedHttpItem.INSTANCE);
            cluster.start();
            reactor.start();
            client.start();

            BasicAsyncRequestProducerGenerator producerGenerator =
                new BasicAsyncRequestProducerGenerator(PARALLEL + URI);
            producerGenerator.addHeader(YandexHeaders.SERVICE, SERVICE);
            producerGenerator.addHeader(YandexHeaders.ZOO_SHARD_ID, SHARD);
            Future<HttpResponse> future = client.execute(
                cluster.proxy().host(),
                producerGenerator,
                EmptyFutureCallback.INSTANCE);

            // Producer should have responded by now
            Thread.sleep(TIMEOUT >> 2);
            Assert.assertEquals(
                1,
                cluster.producer().accessCount(PRODUCER_URI));
            Assert.assertEquals(0, cluster.backend().accessCount(URI));

            // Request was processed by backend
            Thread.sleep(TIMEOUT >> 1);
            Assert.assertEquals(1, cluster.backend().accessCount(URI));

            // All requests completed
            HttpResponse response = future.get();
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            Assert.assertEquals(
                BODY,
                CharsetUtils.toString(response.getEntity()));
        }
    }

    @Test
    public void testParallelProducerRespondedLast() throws Exception {
        try (UniversalSearchProxyCluster cluster =
                new UniversalSearchProxyCluster();
            SharedConnectingIOReactor reactor = new SharedConnectingIOReactor(
                Configs.baseConfig(),
                Configs.dnsConfig());
            AsyncClient client =
                new AsyncClient(reactor, Configs.targetConfig()))
        {
            cluster.producer().add(
                PRODUCER_URI,
                new SlowpokeHttpItem(
                    new StaticHttpItem(PRODUCER_LOCALHOST_BEST),
                    TIMEOUT >> 1),
                NotImplementedHttpItem.INSTANCE);
            cluster.backend().add(
                URI,
                new HeaderHttpItem(
                    new StaticHttpItem(BODY),
                    YandexHeaders.ZOO_QUEUE_ID,
                    "9"),
                NotImplementedHttpItem.INSTANCE);
            cluster.start();
            reactor.start();
            client.start();

            BasicAsyncRequestProducerGenerator producerGenerator =
                new BasicAsyncRequestProducerGenerator(PARALLEL + URI);
            producerGenerator.addHeader(YandexHeaders.SERVICE, SERVICE);
            producerGenerator.addHeader(
                YandexHeaders.X_SEARCH_PREFIX,
                SHARD);
            Future<HttpResponse> future = client.execute(
                cluster.proxy().host(),
                producerGenerator,
                EmptyFutureCallback.INSTANCE);

            // Request should be somewhere in producer now
            Thread.sleep(TIMEOUT >> 2);
            Assert.assertEquals(
                0,
                cluster.producer().accessCount(PRODUCER_URI));
            Assert.assertEquals(1, cluster.backend().accessCount(URI));

            // Request was processed by producer
            Thread.sleep(TIMEOUT >> 1);
            Assert.assertEquals(
                1,
                cluster.producer().accessCount(PRODUCER_URI));

            HttpResponse response = future.get();
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            Assert.assertEquals(
                BODY,
                CharsetUtils.toString(response.getEntity()));
        }
    }

    @Test
    public void testParallelWithPos() throws Exception {
        try (UniversalSearchProxyCluster cluster =
                new UniversalSearchProxyCluster();
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.producer().add(
                PRODUCER_URI,
                new SlowpokeHttpItem(
                    new StaticHttpItem(PRODUCER_LOCALHOST_MIDDLE),
                    TIMEOUT >> 1),
                NotImplementedHttpItem.INSTANCE);
            cluster.backend().add(
                URI,
                new HeaderHttpItem(
                    new StaticHttpItem(BODY),
                    YandexHeaders.ZOO_QUEUE_ID,
                    "11"),
                NotImplementedHttpItem.INSTANCE);
            cluster.start();

            // localhost result is better than minimal position, so we won't
            // request producer
            HttpGet get = new HttpGet(cluster.proxy().host() + PARALLEL + URI);
            get.addHeader(YandexHeaders.SERVICE, SERVICE);
            get.addHeader(YandexHeaders.X_SEARCH_PREFIX, SHARD);
            get.addHeader(YandexHeaders.X_MINIMAL_POSITION, "10");
            long start = System.currentTimeMillis();
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    BODY,
                    CharsetUtils.toString(response.getEntity()));
                YandexAssert.assertLess(
                    (long) TIMEOUT >> 1,
                    System.currentTimeMillis() - start);
            }
        }
    }

    @Test
    public void testParallelNoWaitProducer() throws Exception {
        try (UniversalSearchProxyCluster cluster =
                new UniversalSearchProxyCluster(true, false);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.producer().add(
                PRODUCER_URI,
                new SlowpokeHttpItem(
                    new StaticHttpItem(PRODUCER_LOCALHOST_MIDDLE),
                    TIMEOUT >> 1),
                NotImplementedHttpItem.INSTANCE);
            // Even default zoo queue id will be fine
            cluster.backend().add(URI, BODY);
            cluster.start();

            // localhost result is better than minimal position, so we won't
            // request producer
            HttpGet get = new HttpGet(cluster.proxy().host() + PARALLEL + URI);
            get.addHeader(YandexHeaders.SERVICE, SERVICE);
            get.addHeader(YandexHeaders.X_SEARCH_PREFIX, SHARD);
            long start = System.currentTimeMillis();
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    BODY,
                    CharsetUtils.toString(response.getEntity()));
                YandexAssert.assertLess(
                    (long) TIMEOUT >> 1,
                    System.currentTimeMillis() - start);
            }
        }
    }

    @Test
    public void testParallelProducerDiscardBackendResult() throws Exception {
        try (UniversalSearchProxyCluster cluster =
                new UniversalSearchProxyCluster();
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.producer().add(
                PRODUCER_URI,
                new SlowpokeHttpItem(
                    new StaticHttpItem(PRODUCER_LOCALHOST_MIDDLE),
                    TIMEOUT >> 1),
                NotImplementedHttpItem.INSTANCE);
            cluster.backend().add(
                URI,
                new HeaderHttpItem(
                    new StaticHttpItem(BODY),
                    YandexHeaders.ZOO_QUEUE_ID,
                    "6"),
                NotImplementedHttpItem.INSTANCE);
            cluster.start();

            // localhost result will be discarded, so server will return 503
            HttpGet get = new HttpGet(cluster.proxy().host() + PARALLEL + URI);
            get.addHeader(YandexHeaders.SERVICE, SERVICE);
            get.addHeader(YandexHeaders.X_SEARCH_PREFIX, SHARD);
            try (CloseableHttpResponse response = client.execute(get)) {
                // Min pos is 20 while the only available backend has pos 6
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_BAD_GATEWAY,
                    response);
                Assert.assertEquals(
                    1,
                    cluster.backend().accessCount(URI));
                Assert.assertEquals(
                    1,
                    cluster.producer().accessCount(PRODUCER_URI));
            }
        }
    }

    @Test
    public void testParallelBackendResultNotAccepted() throws Exception {
        try (UniversalSearchProxyCluster cluster =
                new UniversalSearchProxyCluster();
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.backend().add(
                URI,
                new HeaderHttpItem(
                    new StaticHttpItem(BODY),
                    YandexHeaders.ZOO_QUEUE_ID,
                    "12"),
                NotImplementedHttpItem.INSTANCE);
            cluster.start();

            // localhost result will be discarded, so server will return 503
            HttpGet get = new HttpGet(cluster.proxy().host() + PARALLEL + URI);
            get.addHeader(YandexHeaders.SERVICE, SERVICE);
            get.addHeader(YandexHeaders.X_SEARCH_PREFIX, SHARD);
            get.addHeader(YandexHeaders.X_MINIMAL_POSITION, "13");
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_BAD_GATEWAY,
                    response);
                Assert.assertEquals(
                    1,
                    cluster.backend().accessCount(URI));
            }
        }
    }

    @Test
    public void testParallelBackendErrorBeforeProducer() throws Exception {
        try (UniversalSearchProxyCluster cluster =
                new UniversalSearchProxyCluster();
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.producer().add(
                PRODUCER_URI,
                new SlowpokeHttpItem(
                    new StaticHttpItem(PRODUCER_LOCALHOST_MIDDLE),
                    TIMEOUT >> 1),
                NotImplementedHttpItem.INSTANCE);
            cluster.backend().add(URI, HttpStatus.SC_BAD_GATEWAY);
            cluster.start();

            // Both backends will fail, so server won't wait for producer
            HttpGet get = new HttpGet(cluster.proxy().host() + PARALLEL + URI);
            get.addHeader(YandexHeaders.SERVICE, SERVICE);
            get.addHeader(YandexHeaders.X_SEARCH_PREFIX, SHARD);
            long start = System.currentTimeMillis();
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_BAD_GATEWAY,
                    response);
                YandexAssert.assertContains(
                    UnknownHostException.class.getSimpleName(),
                    CharsetUtils.toString(response.getEntity()));
                YandexAssert.assertLess(
                    (long) TIMEOUT >> 1,
                    System.currentTimeMillis() - start);
            }
        }
    }

    @Test
    public void testParallelBackendErrorWithPos() throws Exception {
        try (UniversalSearchProxyCluster cluster =
                new UniversalSearchProxyCluster();
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.backend().add(URI, HttpStatus.SC_BAD_GATEWAY);
            cluster.start();

            // Position is set, so no request to producer will be made
            HttpGet get = new HttpGet(cluster.proxy().host() + PARALLEL + URI);
            get.addHeader(YandexHeaders.SERVICE, SERVICE);
            get.addHeader(YandexHeaders.X_SEARCH_PREFIX, SHARD);
            get.addHeader(YandexHeaders.X_MINIMAL_POSITION, "14");
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_BAD_GATEWAY,
                    response);
                YandexAssert.assertContains(
                    UnknownHostException.class.getSimpleName(),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testProducerless() throws Exception {
        try (UniversalSearchProxyCluster cluster =
                new UniversalSearchProxyCluster(false, true);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.backend().add(
                URI,
                new HeaderHttpItem(
                    new StaticHttpItem(BODY),
                    YandexHeaders.ZOO_QUEUE_ID,
                    "16"),
                NotImplementedHttpItem.INSTANCE);
            cluster.start();

            // Sequential handler disabled in producerless mode
            HttpGet get =
                new HttpGet(cluster.proxy().host() + SEQUENTIAL + URI);
            get.addHeader(YandexHeaders.SERVICE, SERVICE);
            get.addHeader(YandexHeaders.X_SEARCH_PREFIX, SHARD);

            HttpAssert.assertStatusCode(
                HttpStatus.SC_NOT_IMPLEMENTED,
                client,
                get);

            // Positionless requests is not supported too
            get = new HttpGet(cluster.proxy().host() + PARALLEL + URI);
            get.addHeader(YandexHeaders.SERVICE, SERVICE);
            get.addHeader(YandexHeaders.X_SEARCH_PREFIX, SHARD);
            HttpAssert.assertStatusCode(
                HttpStatus.SC_BAD_REQUEST,
                client,
                get);

            get.addHeader(YandexHeaders.X_MINIMAL_POSITION, "15");
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    BODY,
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testProducerlessCgiMinPosition() throws Exception {
        try (UniversalSearchProxyCluster cluster =
                new UniversalSearchProxyCluster(false, true);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            String uri =
                URI + '&' + SearchProxyParams.SERVICE + '=' + SERVICE
                + '&' + SearchProxyParams.SHARD + '=' + SHARD
                + '&' + SearchProxyParams.MIN_POS + "=17";
            cluster.backend().add(
                uri,
                new HeaderHttpItem(
                    new StaticHttpItem(BODY),
                    YandexHeaders.ZOO_QUEUE_ID,
                    "17"),
                NotImplementedHttpItem.INSTANCE);
            cluster.start();

            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(cluster.proxy().host() + PARALLEL + uri)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    BODY,
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testProducerlessLatterIsBetter() throws Exception {
        try (StaticServer backend = new StaticServer(Configs.baseConfig());
            UniversalSearchProxyCluster cluster =
                new UniversalSearchProxyCluster(
                    false,
                    UniversalSearchProxyCluster.searchMapRule(
                        new HttpHost(
                            UniversalSearchProxyCluster.GOOD_FAKE,
                            backend.port())),
                    false);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            backend.add(
                URI,
                new HeaderHttpItem(
                    new StaticHttpItem("This is not what you want"),
                    YandexHeaders.ZOO_QUEUE_ID,
                    "18"),
                NotImplementedHttpItem.INSTANCE);
            cluster.backend().add(
                URI,
                new SlowpokeHttpItem(
                    new HeaderHttpItem(
                        new StaticHttpItem(BODY),
                        YandexHeaders.ZOO_QUEUE_ID,
                        "20"),
                    TIMEOUT >> 1),
                NotImplementedHttpItem.INSTANCE);
            backend.start();
            cluster.start();

            // First response will be discarded
            HttpGet get = new HttpGet(cluster.proxy().host() + PARALLEL + URI);
            get.addHeader(YandexHeaders.SERVICE, SERVICE);
            get.addHeader(YandexHeaders.X_SEARCH_PREFIX, SHARD);
            get.addHeader(YandexHeaders.X_MINIMAL_POSITION, "19");
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    BODY,
                    CharsetUtils.toString(response.getEntity()));
                Assert.assertEquals(1, backend.accessCount(URI));
            }
        }
    }

    private static void testProducerError(
        final long producerErrorDelay,
        final long laggingHostDelay,
        final long actualHostDelay)
        throws Exception
    {
        try (StaticServer backend = new StaticServer(Configs.baseConfig());
            UniversalSearchProxyCluster cluster =
                new UniversalSearchProxyCluster(
                    true,
                    UniversalSearchProxyCluster.searchMapRule(
                        new HttpHost(
                            UniversalSearchProxyCluster.GOOD_FAKE,
                            backend.port())),
                    false);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.producer().add(
                PRODUCER_URI,
                new SlowpokeHttpItem(
                    new StaticHttpItem(HttpStatus.SC_BAD_GATEWAY),
                    producerErrorDelay),
                NotImplementedHttpItem.INSTANCE);
            backend.add(
                URI,
                new SlowpokeHttpItem(
                    new HeaderHttpItem(
                        new StaticHttpItem("This is not what you want"),
                        YandexHeaders.ZOO_QUEUE_ID,
                        "18"),
                    laggingHostDelay),
                NotImplementedHttpItem.INSTANCE);
            cluster.backend().add(
                URI,
                new SlowpokeHttpItem(
                    new HeaderHttpItem(
                        new StaticHttpItem(BODY),
                        YandexHeaders.ZOO_QUEUE_ID,
                        "20"),
                    actualHostDelay),
                NotImplementedHttpItem.INSTANCE);
            backend.start();
            cluster.start();

            // First response will be discarded
            HttpGet get = new HttpGet(cluster.proxy().host() + PARALLEL + URI);
            get.addHeader(YandexHeaders.SERVICE, SERVICE);
            get.addHeader(YandexHeaders.X_SEARCH_PREFIX, SHARD);
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    BODY,
                    CharsetUtils.toString(response.getEntity()));
                Assert.assertEquals(1, backend.accessCount(URI));
            }
        }
    }

    @Test
    public void testProducerError1() throws Exception {
        // Producer fails instantly
        // Then lagging host responds
        // Then actual host responds
        testProducerError(0L, TIMEOUT / 3, TIMEOUT * 2 / 3);
    }

    @Test
    public void testProducerError2() throws Exception {
        // Lagging host responds instantly
        // Then producer fails
        // Then actual host responds
        testProducerError(TIMEOUT / 3, 0, TIMEOUT * 2 / 3);
    }

    @Test
    public void testProducerError3() throws Exception {
        // Lagging host responds instantly
        // Then producer fails
        // Then actual host responds
        testProducerError(TIMEOUT / 3, 0, TIMEOUT * 2 / 3);
    }

    @Test
    public void testProducerError4() throws Exception {
        // Lagging host responds instantly
        // Then actual host responds
        // Then producer fails
        testProducerError(TIMEOUT * 2 / 3, 0, TIMEOUT / 3);
    }

    @Test
    public void testProducerError5() throws Exception {
        // Actual host responds instantly
        // Then lagging host responds
        // Then producer fails
        testProducerError(TIMEOUT * 2 / 3, TIMEOUT / 3, 0L);
    }

    @Test
    public void testLaggingHost() throws Exception {
        try (StaticServer backend1 =
                new StaticServer(Configs.baseConfig("Backend1"));
            StaticServer backend2 =
                new StaticServer(Configs.baseConfig("Backend2"));
            UniversalSearchProxyCluster cluster =
                new UniversalSearchProxyCluster(
                    true,
                    UniversalSearchProxyCluster.searchMapRule(
                        new HttpHost(
                            UniversalSearchProxyCluster.GOOD_FAKE,
                            backend1.port()))
                    + UniversalSearchProxyCluster.searchMapRule(
                        new HttpHost(
                            UniversalSearchProxyCluster.GOOD_FAKE2,
                            backend2.port())),
                    false);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            String uri = URI + "&allow-lagging-hosts";
            cluster.producer().add(
                PRODUCER_URI,
                new StaticHttpItem(HttpStatus.SC_BAD_GATEWAY),
                NotImplementedHttpItem.INSTANCE);
            backend1.add(
                uri,
                new SlowpokeHttpItem(
                    new StaticHttpItem(HttpStatus.SC_BAD_GATEWAY),
                    TIMEOUT >> 2),
                NotImplementedHttpItem.INSTANCE);
            cluster.backend().add(
                uri,
                new SlowpokeHttpItem(
                    new HeaderHttpItem(
                        new StaticHttpItem(BODY),
                        YandexHeaders.ZOO_QUEUE_ID,
                        "20"),
                    TIMEOUT >> 1),
                NotImplementedHttpItem.INSTANCE);
            backend2.add(
                uri,
                new SlowpokeHttpItem(
                    new StaticHttpItem(HttpStatus.SC_BAD_GATEWAY),
                    TIMEOUT * 3 / 4),
                NotImplementedHttpItem.INSTANCE);
            backend1.start();
            backend2.start();
            cluster.start();

            // First response will be discarded
            HttpGet get = new HttpGet(cluster.proxy().host() + PARALLEL + uri);
            get.addHeader(YandexHeaders.SERVICE, SERVICE);
            get.addHeader(YandexHeaders.X_SEARCH_PREFIX, SHARD);
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    BODY,
                    CharsetUtils.toString(response.getEntity()));
                Assert.assertEquals(1, backend1.accessCount(uri));
                Assert.assertEquals(1, backend2.accessCount(uri));
            }
        }
    }

    @Test
    public void testEarlyResponseOnActualBackend() throws Exception {
        try (StaticServer backend1 =
                new StaticServer(Configs.baseConfig("Backend1"));
            StaticServer backend2 =
                new StaticServer(Configs.baseConfig("Backend2"));
            UniversalSearchProxyCluster cluster =
                new UniversalSearchProxyCluster(
                    true,
                    UniversalSearchProxyCluster.searchMapRule(
                        new HttpHost(
                            UniversalSearchProxyCluster.GOOD_FAKE,
                            backend1.port()))
                    + UniversalSearchProxyCluster.searchMapRule(
                        new HttpHost(
                            UniversalSearchProxyCluster.GOOD_FAKE2,
                            backend2.port())),
                    false);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.producer().add(
                PRODUCER_URI,
                // minPos == 6
                new StaticHttpItem(PRODUCER_LOCALHOST_BEST),
                NotImplementedHttpItem.INSTANCE);
            // Ignored because position < minPos
            backend1.add(
                URI,
                new SlowpokeHttpItem(
                    new HeaderHttpItem(
                        new StaticHttpItem(BODY),
                        YandexHeaders.ZOO_QUEUE_ID,
                        "4"),
                    TIMEOUT >> 2),
                NotImplementedHttpItem.INSTANCE);
            // Position is OK
            cluster.backend().add(
                URI,
                new SlowpokeHttpItem(
                    new HeaderHttpItem(
                        new StaticHttpItem(BODY + BODY),
                        YandexHeaders.ZOO_QUEUE_ID,
                        "8"),
                    TIMEOUT >> 1),
                NotImplementedHttpItem.INSTANCE);
            // Ignored, because reponse already sent
            backend2.add(
                URI,
                new SlowpokeHttpItem(
                    new HeaderHttpItem(
                        new StaticHttpItem(BODY + BODY + BODY),
                        YandexHeaders.ZOO_QUEUE_ID,
                        "20"),
                    TIMEOUT * 3 / 4),
                NotImplementedHttpItem.INSTANCE);
            backend1.start();
            backend2.start();
            cluster.start();

            // First response will be discarded
            HttpGet get = new HttpGet(cluster.proxy().host() + PARALLEL + URI);
            get.addHeader(YandexHeaders.SERVICE, SERVICE);
            get.addHeader(YandexHeaders.X_SEARCH_PREFIX, SHARD);
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    BODY + BODY,
                    CharsetUtils.toString(response.getEntity()));
                Assert.assertEquals(1, backend1.accessCount(URI));
            }
        }
    }

    @Test
    public void testEarlyResponseOnActualBackendWithFail() throws Exception {
        try (StaticServer backend1 =
                new StaticServer(Configs.baseConfig("Backend1"));
            StaticServer backend2 =
                new StaticServer(Configs.baseConfig("Backend2"));
            StaticServer backend3 =
                new StaticServer(Configs.baseConfig("Backend3"));
            UniversalSearchProxyCluster cluster =
                new UniversalSearchProxyCluster(
                    true,
                    UniversalSearchProxyCluster.searchMapRule(
                        new HttpHost(
                            UniversalSearchProxyCluster.GOOD_FAKE,
                            backend1.port()))
                    + UniversalSearchProxyCluster.searchMapRule(
                        new HttpHost(
                            UniversalSearchProxyCluster.GOOD_FAKE2,
                            backend2.port()))
                    + UniversalSearchProxyCluster.searchMapRule(
                        new HttpHost(
                            UniversalSearchProxyCluster.GOOD_FAKE3,
                            backend3.port())),
                    false);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.producer().add(
                PRODUCER_URI,
                // minPos == 6
                new StaticHttpItem(PRODUCER_LOCALHOST_BEST),
                NotImplementedHttpItem.INSTANCE);
            backend1.add(
                URI,
                new SlowpokeHttpItem(
                    new StaticHttpItem(HttpStatus.SC_BAD_GATEWAY),
                    TIMEOUT >> 2),
                NotImplementedHttpItem.INSTANCE);
            // Ignored because position < minPos
            cluster.backend().add(
                URI,
                new SlowpokeHttpItem(
                    new HeaderHttpItem(
                        new StaticHttpItem(BODY),
                        YandexHeaders.ZOO_QUEUE_ID,
                        "4"),
                    TIMEOUT >> 1),
                NotImplementedHttpItem.INSTANCE);
            // Position OK
            backend2.add(
                URI,
                new SlowpokeHttpItem(
                    new HeaderHttpItem(
                        new StaticHttpItem(BODY + BODY),
                        YandexHeaders.ZOO_QUEUE_ID,
                        "8"),
                    TIMEOUT * 3 / 4),
                NotImplementedHttpItem.INSTANCE);
            // Ignored, because response already sent
            backend3.add(
                URI,
                new SlowpokeHttpItem(
                    new HeaderHttpItem(
                        new StaticHttpItem(BODY + BODY + BODY),
                        YandexHeaders.ZOO_QUEUE_ID,
                        "20"),
                    TIMEOUT),
                NotImplementedHttpItem.INSTANCE);
            backend1.start();
            backend2.start();
            backend3.start();
            cluster.start();

            // First response will be discarded
            HttpGet get = new HttpGet(cluster.proxy().host() + PARALLEL + URI);
            get.addHeader(YandexHeaders.SERVICE, SERVICE);
            get.addHeader(YandexHeaders.X_SEARCH_PREFIX, SHARD);
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    BODY + BODY,
                    CharsetUtils.toString(response.getEntity()));
                Assert.assertEquals(1, backend1.accessCount(URI));
                Assert.assertEquals(1, cluster.backend().accessCount(URI));
                Assert.assertEquals(1, backend2.accessCount(URI));
            }
        }
    }


    @Test
    public void testSearchRequest() throws Exception {
        try (UniversalSearchProxyCluster cluster =
                 new UniversalSearchProxyCluster(true, true);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.producer().add("/_status*", "[{$localhost\0:100500}]");
            cluster.proxy().register(
                new Pattern<>("/api/suggest", true),
                new TaggedProxyRequestHandler(cluster.proxy()));

            cluster.backend().add(
                "/search?&prefix=0&service=my_service&collector=sorted" +
                    "&length=10&group=groupField" +
                    "&postfilter=pfField+%3D%3D+0" +
                    "&text=search_field1:i%5C+am%5C+vasya+OR+search_field2:i%5C+am%5C+petya",
                "{\"hitsCount\":0, \"hitsArray\":[]}");
            cluster.start();

            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK, client,
                new HttpGet(cluster.proxy().host() + "/api/suggest?uid=1&text=v"));
        }
    }

    private static class TaggedProxyRequestHandler implements ProxyRequestHandler {
        private final UniversalSearchProxy<?> proxy;

        public TaggedProxyRequestHandler(final UniversalSearchProxy<?> proxy) {
            this.proxy = proxy;
        }

        @Override
        public void handle(final ProxySession session) throws HttpException, IOException {
            SearchRequestBuilder request = new SearchRequestBuilder();
            User user = new User(UniversalSearchProxyCluster.SERVICE, new LongPrefix(0L));
            request
                .prefix(user.prefix())
                .service(user.service())
                .collector(SearchRequestBuilder.Collector.SORTED)
                .length(10)
                .group("groupField")
                .postfilter("pfField", PostfilterOperator.EQUAL, "0");
            SearchRequestQueryBuilder queryBuilder = request.queryBuilder();
            queryBuilder.fieldQuery("search_field1", "i am vasya")
                .or().fieldQuery("search_field2", "i am petya");
            request.query(queryBuilder);

            proxy.searchRequest(
                user,
                session,
                "suggest-names",
                new BasicAsyncRequestProducerGenerator(request.toString()),
                new AbstractProxySessionCallback<>(session) {
                    @Override
                    public void completed(final JsonObject jsonObject) {
                        session.response(HttpStatus.SC_OK);
                    }
                }
            );
        }
    }
}

