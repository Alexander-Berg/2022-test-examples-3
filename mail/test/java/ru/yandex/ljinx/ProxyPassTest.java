package ru.yandex.ljinx;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.protocol.HTTP;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.charset.Decoder;
import ru.yandex.http.config.HttpTargetConfigBuilder;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.ExpectingHeaderHttpItem;
import ru.yandex.http.test.ExpectingHttpItem;
import ru.yandex.http.test.HeaderHttpItem;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.test.NotImplementedHttpItem;
import ru.yandex.http.test.SlowpokeHttpItem;
import ru.yandex.http.test.StaticHttpItem;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.http.test.StaticServer;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.http.util.EmptyFutureCallback;
import ru.yandex.http.util.YandexHeaders;
import ru.yandex.http.util.YandexHttpStatus;
import ru.yandex.http.util.client.ClientBuilder;
import ru.yandex.http.util.nio.BasicAsyncRequestProducerGenerator;
import ru.yandex.http.util.nio.BasicAsyncResponseConsumerFactory;
import ru.yandex.http.util.nio.client.AsyncClient;
import ru.yandex.http.util.nio.client.SharedConnectingIOReactor;
import ru.yandex.io.DecodableByteArrayOutputStream;
import ru.yandex.io.IOStreamUtils;
import ru.yandex.test.search.backend.TestSearchBackend;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

public class ProxyPassTest extends TestBase {
    private static final String HIT = "HIT";
    private static final String MISS = "MISS";
    private static final String TEST_OK1 = "TEST_OK1";
    private static final String TEST_OK2 = "TEST_OK2";
    private static final String TEST_OK4 = "TEST_OK4";
    private static final String PARAM1 = "?param1=test";
    private static final String PARAM2 = "?param2=test";
    private static final String BODY1 = "First request body";
    private static final String BODY2 = "Second request body";
    private static final int BINARY_DATA_SIZE = 256;
    private static final byte[] TEST_LUCENE_DATA = genBinaryData();
    private static final int LUCENE_INDEX_WAIT_TIMEOUT = 30000;
    private static final int LUCENE_INDEX_WAIT_SLEEP = 500;
    private static final String NUMDOCS = "/numdocs";
    private static final String STRING_MISMATCH = "string mismatch:";
    private static final String MISSES = "lucene-test-cache-misses_ammm";
    private static final String MEMORY_HITS = "lucene-test-memory-hits_ammm";
    private static final String DISK_HITS = "lucene-test-disk-hits_ammm";
    private static final String MISSES2 = "lucene-test2-cache-misses_ammm";
    private static final String MEMORY_HITS2 = "lucene-test2-memory-hits_ammm";
    private static final String DISK_HITS2 = "lucene-test2-disk-hits_ammm";
    private static final String GET = "GET ";
    private static final String HTTP_1_1 = " HTTP/1.1\r\n\r\n";
    private static final String HTTP_200_OK = "HTTP/1.1 200 OK";
    private static final String PING = "/ping";
    private static final String LOCALHOST = "localhost";
    private static final String ASTERISK = "*";
    private static final String YES = "Yes";
    private static final String GZIP = "gzip";

    private static byte[] genBinaryData() {
        byte[] data = new byte[BINARY_DATA_SIZE];
        for (int i = 0; i < BINARY_DATA_SIZE; i++) {
            data[i] = (byte) i;
        }
        return data;
    }

    @Test
    public void testPatternMapper() throws Exception {
        try (LjinxCluster cluster = new LjinxCluster(this, false);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.upstream1().add(LjinxCluster.PP_URL1, TEST_OK1);
            cluster.upstream2().add(LjinxCluster.PP_URL2, TEST_OK2);
            cluster.start();

            //first upstream with no cache
            HttpResponse response = client.execute(
                new HttpGet(
                    cluster.ljinx().host() + LjinxCluster.PP_URL1));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            HttpAssert.assertHeader(
                YandexHeaders.X_CACHE_STATUS,
                MISS,
                response);
            Assert.assertEquals(
                TEST_OK1,
                CharsetUtils.toString(response.getEntity()));

            //first upstream with no cache, again
            response = client.execute(
                new HttpGet(
                    cluster.ljinx().host() + LjinxCluster.PP_URL1));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            HttpAssert.assertHeader(
                YandexHeaders.X_CACHE_STATUS,
                MISS,
                response);
            Assert.assertEquals(
                TEST_OK1,
                CharsetUtils.toString(response.getEntity()));

            //second upstream with memcache
            String stat = "memcache-cache-element-count_ammv";
            HttpAssert.assertStat(stat, "0", cluster.ljinx().port());
            response = client.execute(
                new HttpGet(
                    cluster.ljinx().host() + LjinxCluster.PP_URL2));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            HttpAssert.assertHeader(
                YandexHeaders.X_CACHE_STATUS,
                MISS,
                response);
            Assert.assertEquals(
                TEST_OK2,
                CharsetUtils.toString(response.getEntity()));
            HttpAssert.assertStat(stat, "1", cluster.ljinx().port());

            //second upstream with memcache, should hit
            response = client.execute(
                new HttpGet(
                    cluster.ljinx().host() + LjinxCluster.PP_URL2));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            HttpAssert.assertHeader(
                YandexHeaders.X_CACHE_STATUS,
                HIT,
                response);
            Assert.assertEquals(
                TEST_OK2,
                CharsetUtils.toString(response.getEntity()));
        }
    }

    @Test
    public void testHttpProxy() throws Exception {
        try (LjinxCluster cluster = new LjinxCluster(this, false);
            CloseableHttpClient client =
                ClientBuilder.createClient(
                    new HttpTargetConfigBuilder(Configs.targetConfig())
                        .proxy(cluster.ljinx().host())
                        .build(),
                    Configs.dnsConfig()))
        {
            cluster.upstream2().add(LjinxCluster.PP_URL4, TEST_OK4);
            cluster.start();

            //first upstream with no cache
            HttpGet get =
                new HttpGet(cluster.upstream2().host() + LjinxCluster.PP_URL4);
            HttpResponse response = client.execute(get);
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            HttpAssert.assertHeader(
                YandexHeaders.X_CACHE_STATUS,
                MISS,
                response);
            Assert.assertEquals(
                TEST_OK4,
                CharsetUtils.toString(response.getEntity()));

            get =
                new HttpGet(cluster.upstream2().host() + LjinxCluster.PP_URL4);
            response = client.execute(get);
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            HttpAssert.assertHeader(
                YandexHeaders.X_CACHE_STATUS,
                HIT,
                response);
            Assert.assertEquals(
                TEST_OK4,
                CharsetUtils.toString(response.getEntity()));
        }
    }

    @Test
    public void testTTL() throws Exception {
        try (LjinxCluster cluster = new LjinxCluster(this, false);
            CloseableHttpClient client =
                ClientBuilder.createClient(
                    new HttpTargetConfigBuilder(Configs.targetConfig())
                        .proxy(cluster.ljinx().host())
                        .build(),
                    Configs.dnsConfig()))
        {
            cluster.upstream2().add(
                LjinxCluster.PP_URL4,
                HttpStatus.SC_CONFLICT,
                TEST_OK4);
            cluster.start();

            //first upstream with no cache
            HttpGet get =
                new HttpGet(cluster.upstream2().host() + LjinxCluster.PP_URL4);
            HttpResponse response = client.execute(get);
            HttpAssert.assertStatusCode(HttpStatus.SC_CONFLICT, response);
            HttpAssert.assertHeader(
                YandexHeaders.X_CACHE_STATUS,
                MISS,
                response);
            Assert.assertEquals(
                TEST_OK4,
                CharsetUtils.toString(response.getEntity()));

            get =
                new HttpGet(cluster.upstream2().host() + LjinxCluster.PP_URL4);
            response = client.execute(get);
            HttpAssert.assertStatusCode(HttpStatus.SC_CONFLICT, response);
            HttpAssert.assertHeader(
                YandexHeaders.X_CACHE_STATUS,
                HIT,
                response);
            Assert.assertEquals(
                TEST_OK4,
                CharsetUtils.toString(response.getEntity()));
            //sleep for expire
            Thread.sleep(TimeUnit.SECONDS.toMillis(2 + 2));

            get =
                new HttpGet(cluster.upstream2().host() + LjinxCluster.PP_URL4);
            response = client.execute(get);
            HttpAssert.assertStatusCode(HttpStatus.SC_CONFLICT, response);
            HttpAssert.assertHeader(
                YandexHeaders.X_CACHE_STATUS,
                MISS,
                response);
            Assert.assertEquals(
                TEST_OK4,
                CharsetUtils.toString(response.getEntity()));
        }
    }

    @Test
    public void testTTLNoExpire() throws Exception {
        try (LjinxCluster cluster = new LjinxCluster(this, false);
            CloseableHttpClient client =
                ClientBuilder.createClient(
                    new HttpTargetConfigBuilder(Configs.targetConfig())
                        .proxy(cluster.ljinx().host())
                        .build(),
                    Configs.dnsConfig()))
        {
            //test no expire
            cluster.upstream2().add(
                LjinxCluster.PP_URL4,
                HttpStatus.SC_OK,
                TEST_OK4);
            cluster.start();

            //first upstream with no cache
            HttpGet get =
                new HttpGet(cluster.upstream2().host() + LjinxCluster.PP_URL4);
            HttpResponse response = client.execute(get);
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            HttpAssert.assertHeader(
                YandexHeaders.X_CACHE_STATUS,
                MISS,
                response);
            Assert.assertEquals(
                TEST_OK4,
                CharsetUtils.toString(response.getEntity()));

            get =
                new HttpGet(cluster.upstream2().host() + LjinxCluster.PP_URL4);
            response = client.execute(get);
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            HttpAssert.assertHeader(
                YandexHeaders.X_CACHE_STATUS,
                HIT,
                response);
            Assert.assertEquals(
                TEST_OK4,
                CharsetUtils.toString(response.getEntity()));
            //sleep for expire
            Thread.sleep(TimeUnit.SECONDS.toMillis(2 + 2));

            get =
                new HttpGet(cluster.upstream2().host() + LjinxCluster.PP_URL4);
            response = client.execute(get);
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            HttpAssert.assertHeader(
                YandexHeaders.X_CACHE_STATUS,
                HIT,
                response);
            Assert.assertEquals(
                TEST_OK4,
                CharsetUtils.toString(response.getEntity()));
        }
    }

    @Test
    public void testNoCache() throws Exception {
        try (LjinxCluster cluster = new LjinxCluster(this, false);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.upstream2().add(
                LjinxCluster.PP_URL2,
                new StaticHttpResource(
                    new StaticHttpItem(HttpStatus.SC_ACCEPTED, TEST_OK1)));
            cluster.start();

            HttpResponse response = client.execute(
                new HttpGet(
                    cluster.ljinx().host() + LjinxCluster.PP_URL2));
            HttpAssert.assertStatusCode(HttpStatus.SC_ACCEPTED, response);
            HttpAssert.assertHeader(
                YandexHeaders.X_CACHE_STATUS,
                MISS,
                response);
            Assert.assertEquals(
                TEST_OK1,
                CharsetUtils.toString(response.getEntity()));
            //second request
            response = client.execute(
                new HttpGet(
                    cluster.ljinx().host() + LjinxCluster.PP_URL2));
            HttpAssert.assertStatusCode(HttpStatus.SC_ACCEPTED, response);
            HttpAssert.assertHeader(
                YandexHeaders.X_CACHE_STATUS,
                MISS,
                response);
            Assert.assertEquals(
                TEST_OK1,
                CharsetUtils.toString(response.getEntity()));

            //test header
            cluster.upstream2().add(
                LjinxCluster.PP_URL2,
                new StaticHttpResource(
                    new StaticHttpItem(HttpStatus.SC_OK, TEST_OK1)
                        .addHeader(LjinxCluster.NO_CACHE_PLEASE, YES)));
            Thread.sleep(100L);

            response = client.execute(
                new HttpGet(
                    cluster.ljinx().host() + LjinxCluster.PP_URL2));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            HttpAssert.assertHeader(
                YandexHeaders.X_CACHE_STATUS,
                MISS,
                response);
            Assert.assertEquals(
                TEST_OK1,
                CharsetUtils.toString(response.getEntity()));
            //second request
            response = client.execute(
                new HttpGet(
                    cluster.ljinx().host() + LjinxCluster.PP_URL2));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            HttpAssert.assertHeader(
                YandexHeaders.X_CACHE_STATUS,
                MISS,
                response);
            Assert.assertEquals(
                TEST_OK1,
                CharsetUtils.toString(response.getEntity()));
        }
    }

    @Test
    public void testDropCache() throws Exception {
        try (LjinxCluster cluster = new LjinxCluster(this, false);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.upstream2().add(
                LjinxCluster.PP_URL2,
                new StaticHttpResource(
                    new StaticHttpItem(HttpStatus.SC_OK, TEST_OK1)));
            cluster.start();

            HttpResponse response = client.execute(
                new HttpGet(
                    cluster.ljinx().host() + LjinxCluster.PP_URL2));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            HttpAssert.assertHeader(
                YandexHeaders.X_CACHE_STATUS,
                MISS,
                response);
            Assert.assertEquals(
                TEST_OK1,
                CharsetUtils.toString(response.getEntity()));
            //second request
            response = client.execute(
                new HttpGet(
                    cluster.ljinx().host() + LjinxCluster.PP_URL2));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            HttpAssert.assertHeader(
                YandexHeaders.X_CACHE_STATUS,
                HIT,
                response);
            Assert.assertEquals(
                TEST_OK1,
                CharsetUtils.toString(response.getEntity()));

            HttpGet invalidate =
                new HttpGet(cluster.ljinx().host() + LjinxCluster.PP_URL2);
            invalidate.addHeader(YandexHeaders.X_CACHE_INVALIDATE, "");
            response = client.execute(invalidate);
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);

            //should MISS
            response = client.execute(
                new HttpGet(
                    cluster.ljinx().host() + LjinxCluster.PP_URL2));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            HttpAssert.assertHeader(
                YandexHeaders.X_CACHE_STATUS,
                MISS,
                response);
            Assert.assertEquals(
                TEST_OK1,
                CharsetUtils.toString(response.getEntity()));
        }
    }

    @Test
    public void testLuceneProxiedCache() throws Exception {
        try (LjinxCluster cluster = new LjinxCluster(this, true);
            CloseableHttpClient client =
                ClientBuilder.createClient(
                    new HttpTargetConfigBuilder(Configs.targetConfig())
                        .proxy(cluster.ljinx().host())
                        .build(),
                    Configs.dnsConfig()))
        {
            final String url = "/test3/q:w:e/";
            cluster.upstream2().add(
                url,
                new StaticHttpResource(
                    new StaticHttpItem(
                        HttpStatus.SC_OK,
                        new ByteArrayEntity(TEST_LUCENE_DATA))));
            cluster.start();
            final long ts = TimeUnit.MILLISECONDS.toSeconds(
                System.currentTimeMillis());
            String keepAliveTimeout = "timeout=9";

            HttpResponse response = client.execute(
                new HttpGet(
                    cluster.upstream2().host() + url));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            HttpAssert.assertHeader(
                YandexHeaders.X_CACHE_STATUS,
                MISS,
                response);
            // Assert that ljinx provides persistent connections
            HttpAssert.assertHeader(
                HTTP.CONN_KEEP_ALIVE,
                keepAliveTimeout,
                response);
            Assert.assertArrayEquals(
                TEST_LUCENE_DATA,
                CharsetUtils.toByteArray(response.getEntity()));

            //wait for lucene to index
            long deadline =
                System.currentTimeMillis() + LUCENE_INDEX_WAIT_TIMEOUT;
            while (System.currentTimeMillis() < deadline) {
                try {
                    cluster.lucene().checkSearch(
                        NUMDOCS,
                        "{\"docs\":1,\"shards\":\"<any value>\"" + '}');
                    break;
                } catch (java.lang.AssertionError e) {
                    if (e.getMessage().startsWith(STRING_MISMATCH)) {
                        Thread.sleep(LUCENE_INDEX_WAIT_SLEEP);
                    } else {
                        throw e;
                    }
                }
            }

            //second request
            response = client.execute(
                new HttpGet(
                    cluster.upstream2().host() + url));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            HttpAssert.assertHeader(
                YandexHeaders.X_CACHE_STATUS,
                HIT,
                response);
            // Assert that ljinx provides persistent connections
            HttpAssert.assertHeader(
                HTTP.CONN_KEEP_ALIVE,
                keepAliveTimeout,
                response);
            Assert.assertArrayEquals(
                TEST_LUCENE_DATA,
                CharsetUtils.toByteArray(response.getEntity()));
            String stats = HttpAssert.stats(cluster.ljinx().port());
            HttpAssert.assertStat(
                MISSES,
                Integer.valueOf(1).toString(),
                stats);
            HttpAssert.assertStat(
                MEMORY_HITS,
                Integer.valueOf(1).toString(),
                stats);
            HttpAssert.assertStat(
                DISK_HITS,
                Integer.valueOf(0).toString(),
                stats);

            HttpGet invalidate =
                new HttpGet(cluster.upstream2().host() + url);
            invalidate.addHeader(YandexHeaders.X_CACHE_INVALIDATE, "");
            response = client.execute(invalidate);
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);

            //should HIT
            response = client.execute(
                new HttpGet(
                        "http://127.0.0.2:" + cluster.upstream2().port()
                            + url));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            HttpAssert.assertHeader(
                YandexHeaders.X_CACHE_STATUS,
                HIT,
                response);
            // Assert that ljinx provides persistent connections
            HttpAssert.assertHeader(
                HTTP.CONN_KEEP_ALIVE,
                keepAliveTimeout,
                response);
            Assert.assertArrayEquals(
                TEST_LUCENE_DATA,
                CharsetUtils.toByteArray(response.getEntity()));
            HttpAssert.assertStat(
                DISK_HITS,
                Integer.valueOf(1).toString(),
                cluster.ljinx().port());

            //should HIT disk cache again, because loadHitsToMemory is not set
            response = client.execute(
                new HttpGet(
                    cluster.upstream2().host() + url));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            HttpAssert.assertHeader(
                YandexHeaders.X_CACHE_STATUS,
                HIT,
                response);
            // Assert that ljinx provides persistent connections
            HttpAssert.assertHeader(
                HTTP.CONN_KEEP_ALIVE,
                keepAliveTimeout,
                response);
            Assert.assertArrayEquals(
                TEST_LUCENE_DATA,
                CharsetUtils.toByteArray(response.getEntity()));
            HttpAssert.assertStat(
                DISK_HITS,
                Integer.valueOf(2).toString(),
                cluster.ljinx().port());

            //default TTL is 1 day
            //lucene TTL resolution = 1 second
            //(http_expire_timestamp - startTS) / 30 = 2880
            cluster.lucene().checkSearch(
                "/search?text=url:*&get=http_expire_timestamp"
                    + "&dp=csub(http_expire_timestamp," + ts + ')'
                    + "&dp=cdiv(http_expire_timestamp,"
                    + TimeUnit.MILLISECONDS.toSeconds(LUCENE_INDEX_WAIT_TIMEOUT)
                    + ')',
                TestSearchBackend.prepareResult(
                    "\"http_expire_timestamp\":\"2880\""));
        }
    }

    @Test
    public void testLuceneMiminalTTL() throws Exception {
        try (LjinxCluster cluster = new LjinxCluster(this, true);
            CloseableHttpClient client =
                ClientBuilder.createClient(
                    new HttpTargetConfigBuilder(Configs.targetConfig())
                        .proxy(cluster.ljinx().host())
                        .build(),
                    Configs.dnsConfig()))
        {
            cluster.upstream2().add(
                LjinxCluster.PP_URL3,
                new StaticHttpResource(
                    new StaticHttpItem(
                        HttpStatus.SC_CONFLICT,
                        new ByteArrayEntity(TEST_LUCENE_DATA))));
            cluster.start();

            HttpResponse response = client.execute(
                new HttpGet(
                    cluster.upstream2().host() + LjinxCluster.PP_URL3));
            HttpAssert.assertStatusCode(HttpStatus.SC_CONFLICT, response);
            HttpAssert.assertHeader(
                YandexHeaders.X_CACHE_STATUS,
                MISS,
                response);
            Assert.assertArrayEquals(
                TEST_LUCENE_DATA,
                CharsetUtils.toByteArray(response.getEntity()));

            //second request
            response = client.execute(
                new HttpGet(
                    cluster.upstream2().host() + LjinxCluster.PP_URL3));
            HttpAssert.assertStatusCode(HttpStatus.SC_CONFLICT, response);
            HttpAssert.assertHeader(
                YandexHeaders.X_CACHE_STATUS,
                HIT,
                response);
            Assert.assertArrayEquals(
                TEST_LUCENE_DATA,
                CharsetUtils.toByteArray(response.getEntity()));

            HttpGet invalidate =
                new HttpGet(cluster.upstream2().host() + LjinxCluster.PP_URL3);
            invalidate.addHeader(YandexHeaders.X_CACHE_INVALIDATE, "");
            response = client.execute(invalidate);
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);

            //should MISS
            response = client.execute(
                new HttpGet(
                    cluster.upstream2().host() + LjinxCluster.PP_URL3));
            HttpAssert.assertStatusCode(HttpStatus.SC_CONFLICT, response);
            HttpAssert.assertHeader(
                YandexHeaders.X_CACHE_STATUS,
                MISS,
                response);
            Assert.assertArrayEquals(
                TEST_LUCENE_DATA,
                CharsetUtils.toByteArray(response.getEntity()));
        }
    }

    @Test
    public void testGetCacheKeyFunction() throws Exception {
        try (LjinxCluster cluster = new LjinxCluster(this, false);
            CloseableHttpClient client =
                ClientBuilder.createClient(
                    new HttpTargetConfigBuilder(Configs.targetConfig())
                        .proxy(cluster.ljinx().host())
                        .build(),
                    Configs.dnsConfig()))
        {
            cluster.upstream2().add(
                LjinxCluster.PP_URL5 + PARAM1,
                HttpStatus.SC_OK,
                TEST_OK4);
            cluster.start();

            //first upstream with no cache
            HttpGet get =
                new HttpGet(
                    cluster.upstream2().host()
                    + LjinxCluster.PP_URL5 + PARAM1);
            HttpResponse response = client.execute(get);
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            HttpAssert.assertHeader(
                YandexHeaders.X_CACHE_STATUS,
                MISS,
                response);
            Assert.assertEquals(
                TEST_OK4,
                CharsetUtils.toString(response.getEntity()));

            get =
                new HttpGet(
                    cluster.upstream2().host()
                    + LjinxCluster.PP_URL5
                    + PARAM1);
            response = client.execute(get);
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            HttpAssert.assertHeader(
                YandexHeaders.X_CACHE_STATUS,
                HIT,
                response);
            Assert.assertEquals(
                TEST_OK4,
                CharsetUtils.toString(response.getEntity()));

            get =
                new HttpGet(
                    cluster.upstream2().host()
                    + LjinxCluster.PP_URL5 + PARAM2);
            response = client.execute(get);
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            HttpAssert.assertHeader(
                YandexHeaders.X_CACHE_STATUS,
                HIT,
                response);
            Assert.assertEquals(
                TEST_OK4,
                CharsetUtils.toString(response.getEntity()));
        }
    }

    @Test
    public void testPassHeaders() throws Exception {
        try (LjinxCluster cluster = new LjinxCluster(this, false);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            String authToken = "QWxhZGRpbjpvcGVuIHNlc2FtZQ==";
            cluster.upstream2().add(
                LjinxCluster.PP_PASS_HEADERS_URL,
                new StaticHttpResource(
                    new ExpectingHeaderHttpItem(
                        new StaticHttpItem(HttpStatus.SC_OK, TEST_OK1),
                        HttpHeaders.AUTHORIZATION,
                        authToken)));
            cluster.start();

            HttpGet get = new HttpGet(
                cluster.ljinx().host() + LjinxCluster.PP_PASS_HEADERS_URL);
            get.addHeader(HttpHeaders.AUTHORIZATION, authToken);
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertHeader(
                    YandexHeaders.X_CACHE_STATUS,
                    MISS,
                    response);
                Assert.assertEquals(
                    TEST_OK1,
                    CharsetUtils.toString(response.getEntity()));
            }

            //second request
            get = new HttpGet(
                cluster.ljinx().host() + LjinxCluster.PP_PASS_HEADERS_URL);
            get.addHeader(HttpHeaders.AUTHORIZATION, "TG9naW46cEBzc3cwcmQ=");
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertHeader(
                    YandexHeaders.X_CACHE_STATUS,
                    HIT,
                    response);
                Assert.assertEquals(
                    TEST_OK1,
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testSiblings() throws Exception {
        try (LjinxCluster cluster = new LjinxCluster(this, false);
            CloseableHttpClient client1 =
                ClientBuilder.createClient(
                    new HttpTargetConfigBuilder(Configs.targetConfig())
                        .proxy(cluster.ljinx(0).host())
                        .build(),
                    Configs.dnsConfig());
            CloseableHttpClient client2 =
                ClientBuilder.createClient(
                    new HttpTargetConfigBuilder(Configs.targetConfig())
                        .proxy(cluster.ljinx(1).host())
                        .build(),
                    Configs.dnsConfig()))
        {
            String uri = LjinxCluster.PP_SIBLINGS + "?uid=2";
            cluster.upstream2().add(uri, TEST_OK4);
            cluster.start();

            // first upstream with no cache
            try (CloseableHttpResponse response = client1.execute(
                    new HttpGet(cluster.upstream2().host() + uri)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertHeader(
                    YandexHeaders.X_CACHE_STATUS,
                    MISS,
                    response);
                Assert.assertEquals(
                    TEST_OK4,
                    CharsetUtils.toString(response.getEntity()));
            }

            // second request should hit cache
            try (CloseableHttpResponse response = client2.execute(
                    new HttpGet(cluster.upstream2().host() + uri)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertHeader(
                    YandexHeaders.X_CACHE_STATUS,
                    HIT,
                    response);
                Assert.assertEquals(
                    TEST_OK4,
                    CharsetUtils.toString(response.getEntity()));
                // Check that Content-TYPE presents only once
                YandexAssert.assertSize(
                    1,
                    Arrays.asList(
                        response.getHeaders(HttpHeaders.CONTENT_TYPE)));
            }

            cluster.upstream2().add(
                uri,
                new StaticHttpResource(
                    new ExpectingHeaderHttpItem(
                        new StaticHttpItem(TEST_OK4),
                        YandexHeaders.X_YA_SERVICE_TICKET,
                        null)));
            Thread.sleep(100L);

            String headerValue = "Some ticket here";
            HttpGet get = new HttpGet(cluster.upstream2().host() + uri);
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                headerValue);
            // Header added, cache miss expected
            try (CloseableHttpResponse response = client2.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertHeader(
                    YandexHeaders.X_CACHE_STATUS,
                    MISS,
                    response);
                Assert.assertEquals(
                    TEST_OK4,
                    CharsetUtils.toString(response.getEntity()));
            }

            get = new HttpGet(cluster.upstream2().host() + uri);
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET.toUpperCase(Locale.ROOT),
                headerValue);
            // Only header case changed, should hid
            try (CloseableHttpResponse response = client2.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertHeader(
                    YandexHeaders.X_CACHE_STATUS,
                    HIT,
                    response);
                Assert.assertEquals(
                    TEST_OK4,
                    CharsetUtils.toString(response.getEntity()));
            }

            // Test that no fallback to next sibling occurs on 429
            // Avoid DDoSing
            uri = LjinxCluster.PP_SIBLINGS + "?uid=429";
            cluster.upstream2().add(
                uri,
                YandexHttpStatus.SC_TOO_MANY_REQUESTS);
            Thread.sleep(100L);
            try (CloseableHttpResponse response = client1.execute(
                    new HttpGet(cluster.upstream2().host() + uri)))
            {
                HttpAssert.assertStatusCode(
                    YandexHttpStatus.SC_TOO_MANY_REQUESTS,
                    response);
                HttpAssert.assertHeader(
                    YandexHeaders.X_CACHE_STATUS,
                    MISS,
                    response);
                Assert.assertEquals(1, cluster.upstream2().accessCount(uri));
            }
        }
    }

    // CSOFF: MethodLength
    @Test
    public void testSiblingsFailures() throws Exception {
        try (LjinxCluster cluster = new LjinxCluster(this, false);
            CloseableHttpClient client =
                ClientBuilder.createClient(
                    new HttpTargetConfigBuilder(Configs.targetConfig())
                        .proxy(cluster.ljinx(0).host())
                        .build(),
                    Configs.dnsConfig()))
        {
            String uriOk = LjinxCluster.PP_SIBLINGS + "?uid=1&ok";
            String uriBad = LjinxCluster.PP_SIBLINGS + "?uid=1&bad";
            cluster.upstream2().add(uriOk, TEST_OK4);
            cluster.upstream2().add(uriBad, HttpStatus.SC_BAD_REQUEST);
            cluster.start();

            String totalShuffles = "memcache-total-siblings-shuffles_ammm";
            String siblingsRemovals = "memcache-siblings-removals_ammm";
            // Will go through the last sibling (shuffle rotates left)
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(cluster.upstream2().host() + uriOk)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    0,
                    cluster.ljinxProxy(0).accessCount(ASTERISK));
                Assert.assertEquals(
                    0,
                    cluster.ljinxProxy(1).accessCount(ASTERISK));
                Assert.assertEquals(
                    0,
                    cluster.ljinxProxy(2).accessCount(ASTERISK));
                Assert.assertEquals(
                    1,
                    cluster.ljinxProxy(2 + 1).accessCount(ASTERISK));
                String stats = HttpAssert.stats(cluster.ljinx(0).port());
                HttpAssert.assertStat(
                    totalShuffles,
                    Integer.toString(1),
                    stats);
                HttpAssert.assertStat(
                    siblingsRemovals,
                    Integer.toString(0),
                    stats);
            }

            // Last request was OK, so next request will go through the same
            // sibling
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(cluster.upstream2().host() + uriBad)))
            {
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_BAD_REQUEST,
                    response);
                Assert.assertEquals(
                    0,
                    cluster.ljinxProxy(0).accessCount(ASTERISK));
                Assert.assertEquals(
                    0,
                    cluster.ljinxProxy(1).accessCount(ASTERISK));
                Assert.assertEquals(
                    0,
                    cluster.ljinxProxy(2).accessCount(ASTERISK));
                Assert.assertEquals(
                    2,
                    cluster.ljinxProxy(2 + 1).accessCount(ASTERISK));
                String stats = HttpAssert.stats(cluster.ljinx(0).port());
                HttpAssert.assertStat(
                    totalShuffles,
                    Integer.toString(2),
                    stats);
                HttpAssert.assertStat(
                    siblingsRemovals,
                    Integer.toString(0),
                    stats);
            }

            // Last request wasn't OK, so next request will go through the next
            // sibling
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(cluster.upstream2().host() + uriBad)))
            {
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_BAD_REQUEST,
                    response);
                Assert.assertEquals(
                    0,
                    cluster.ljinxProxy(0).accessCount(ASTERISK));
                Assert.assertEquals(
                    0,
                    cluster.ljinxProxy(1).accessCount(ASTERISK));
                Assert.assertEquals(
                    1,
                    cluster.ljinxProxy(2).accessCount(ASTERISK));
                Assert.assertEquals(
                    2,
                    cluster.ljinxProxy(2 + 1).accessCount(ASTERISK));
                String stats = HttpAssert.stats(cluster.ljinx(0).port());
                HttpAssert.assertStat(
                    totalShuffles,
                    Integer.toString(2 + 1),
                    stats);
                HttpAssert.assertStat(
                    siblingsRemovals,
                    Integer.toString(1),
                    stats);
            }

            // Last request wasn't OK, so next request will go through the next
            // sibling
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(cluster.upstream2().host() + uriBad)))
            {
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_BAD_REQUEST,
                    response);
                Assert.assertEquals(
                    0,
                    cluster.ljinxProxy(0).accessCount(ASTERISK));
                Assert.assertEquals(
                    1,
                    cluster.ljinxProxy(1).accessCount(ASTERISK));
                Assert.assertEquals(
                    1,
                    cluster.ljinxProxy(2).accessCount(ASTERISK));
                Assert.assertEquals(
                    2,
                    cluster.ljinxProxy(2 + 1).accessCount(ASTERISK));
                String stats = HttpAssert.stats(cluster.ljinx(0).port());
                HttpAssert.assertStat(
                    totalShuffles,
                    Integer.toString(2 + 2),
                    stats);
                HttpAssert.assertStat(
                    siblingsRemovals,
                    Integer.toString(2),
                    stats);
            }

            // Last request wasn't OK, so next request will go through the next
            // sibling
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(cluster.upstream2().host() + uriBad)))
            {
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_BAD_REQUEST,
                    response);
                Assert.assertEquals(
                    1,
                    cluster.ljinxProxy(0).accessCount(ASTERISK));
                Assert.assertEquals(
                    1,
                    cluster.ljinxProxy(1).accessCount(ASTERISK));
                Assert.assertEquals(
                    1,
                    cluster.ljinxProxy(2).accessCount(ASTERISK));
                Assert.assertEquals(
                    2,
                    cluster.ljinxProxy(2 + 1).accessCount(ASTERISK));
            }
        }
    }
    // CSON: MethodLength

    @Test
    public void testSiblingsHash() throws Exception {
        try (LjinxCluster cluster = new LjinxCluster(this, false);
            CloseableHttpClient client1 =
                ClientBuilder.createClient(
                    new HttpTargetConfigBuilder(Configs.targetConfig())
                        .proxy(cluster.ljinx(0).host())
                        .build(),
                    Configs.dnsConfig());
            CloseableHttpClient client2 =
                ClientBuilder.createClient(
                    new HttpTargetConfigBuilder(Configs.targetConfig())
                        .proxy(cluster.ljinx(1).host())
                        .build(),
                    Configs.dnsConfig()))
        {
            String uri = LjinxCluster.PP_SIBLINGS_HASH + "?stid=a";
            cluster.upstream2().add(uri, TEST_OK4);
            cluster.start();

            // first upstream with no cache
            try (CloseableHttpResponse response = client1.execute(
                    new HttpGet(cluster.upstream2().host() + uri)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertHeader(
                    YandexHeaders.X_CACHE_STATUS,
                    MISS,
                    response);
                Assert.assertEquals(
                    TEST_OK4,
                    CharsetUtils.toString(response.getEntity()));
            }

            // second request should hit cache
            try (CloseableHttpResponse response = client2.execute(
                    new HttpGet(cluster.upstream2().host() + uri)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertHeader(
                    YandexHeaders.X_CACHE_STATUS,
                    HIT,
                    response);
                Assert.assertEquals(
                    TEST_OK4,
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testSiblingsTimeout() throws Exception {
        try (LjinxCluster cluster = new LjinxCluster(this, false);
            CloseableHttpClient client =
                ClientBuilder.createClient(
                    new HttpTargetConfigBuilder(Configs.targetConfig())
                        .proxy(cluster.ljinx(0).host())
                        .build(),
                    Configs.dnsConfig()))
        {
            String uri = LjinxCluster.PP_SIBLINGS + "?uid=3";
            cluster.upstream2().add(
                uri,
                new StaticHttpResource(
                    new SlowpokeHttpItem(
                        StaticHttpItem.OK,
                        LjinxCluster.SIBLINGS_TIMEOUT << 1)));
            cluster.start();

            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(cluster.upstream2().host() + uri)))
            {
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_GATEWAY_TIMEOUT,
                    response);
            }
        }
    }

    @Test
    public void testSiblingsSearchMap() throws Exception {
        try (LjinxCluster cluster = new LjinxCluster(this, false);
            CloseableHttpClient client1 =
                ClientBuilder.createClient(
                    new HttpTargetConfigBuilder(Configs.targetConfig())
                        .proxy(cluster.ljinx(0).host())
                        .build(),
                    Configs.dnsConfig());
            CloseableHttpClient client2 =
                ClientBuilder.createClient(
                    new HttpTargetConfigBuilder(Configs.targetConfig())
                        .proxy(cluster.ljinx(1).host())
                        .build(),
                    Configs.dnsConfig()))
        {
            String uri = LjinxCluster.PP_SIBLINGS_SEARCHMAP + "?stid=b";
            cluster.upstream2().add(uri, TEST_OK4);
            cluster.start();

            // first upstream with no cache
            try (CloseableHttpResponse response = client1.execute(
                    new HttpGet(cluster.upstream2().host() + uri)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertHeader(
                    YandexHeaders.X_CACHE_STATUS,
                    MISS,
                    response);
                Assert.assertEquals(
                    TEST_OK4,
                    CharsetUtils.toString(response.getEntity()));
            }

            // second request should hit cache
            try (CloseableHttpResponse response = client2.execute(
                    new HttpGet(cluster.upstream2().host() + uri)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertHeader(
                    YandexHeaders.X_CACHE_STATUS,
                    HIT,
                    response);
                Assert.assertEquals(
                    TEST_OK4,
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testSiblingsSetPort() throws Exception {
        try (LjinxCluster cluster = new LjinxCluster(this, false);
            CloseableHttpClient client1 =
                ClientBuilder.createClient(
                    new HttpTargetConfigBuilder(Configs.targetConfig())
                        .proxy(cluster.ljinx(0).host())
                        .build(),
                    Configs.dnsConfig());
            CloseableHttpClient client2 =
                ClientBuilder.createClient(
                    new HttpTargetConfigBuilder(Configs.targetConfig())
                        .proxy(cluster.ljinx(1).host())
                        .build(),
                    Configs.dnsConfig()))
        {
            String uri = LjinxCluster.PP_SIBLINGS_SET_PORT + "?stid=c";
            cluster.upstream2().add(uri, TEST_OK4);
            cluster.start();

            // first upstream with no cache
            try (CloseableHttpResponse response = client1.execute(
                    new HttpGet(cluster.upstream2().host() + uri)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertHeader(
                    YandexHeaders.X_CACHE_STATUS,
                    MISS,
                    response);
                Assert.assertEquals(
                    TEST_OK4,
                    CharsetUtils.toString(response.getEntity()));
            }

            // second request should hit cache
            try (CloseableHttpResponse response = client2.execute(
                    new HttpGet(cluster.upstream2().host() + uri)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertHeader(
                    YandexHeaders.X_CACHE_STATUS,
                    HIT,
                    response);
                Assert.assertEquals(
                    TEST_OK4,
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testSiblingsDirectRequest() throws Exception {
        try (LjinxCluster cluster = new LjinxCluster(this, false);
             CloseableHttpClient client = Configs.createDefaultClient())
        {
            String uri = LjinxCluster.PP_SIBLINGS_SEARCHMAP + "?stid=e";
            cluster.upstream2().add(uri, TEST_OK4);
            cluster.start();

            try (CloseableHttpResponse response = client.execute(
                cluster.ljinx().host(),
                new HttpGet(uri)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertHeader(
                    YandexHeaders.X_CACHE_STATUS,
                    MISS,
                    response);
                Assert.assertEquals(
                    TEST_OK4,
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response = client.execute(
                cluster.ljinx().host(),
                new HttpGet(uri)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertHeader(
                    YandexHeaders.X_CACHE_STATUS,
                    HIT,
                    response);
                Assert.assertEquals(
                    TEST_OK4,
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testSiblingsRandomShuffle() throws Exception {
        try (LjinxCluster cluster = new LjinxCluster(this, false);
            CloseableHttpClient client1 =
                ClientBuilder.createClient(
                    new HttpTargetConfigBuilder(Configs.targetConfig())
                        .proxy(cluster.ljinx(0).host())
                        .build(),
                    Configs.dnsConfig());
            CloseableHttpClient client2 =
                ClientBuilder.createClient(
                    new HttpTargetConfigBuilder(Configs.targetConfig())
                        .proxy(cluster.ljinx(1).host())
                        .build(),
                    Configs.dnsConfig()))
        {
            String uri = LjinxCluster.PP_SIBLINGS_RANDOM;
            cluster.upstream2().add(uri, TEST_OK4);
            cluster.start();

            // first upstream with no cache
            try (CloseableHttpResponse response = client1.execute(
                    new HttpGet(cluster.upstream2().host() + uri)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertHeader(
                    YandexHeaders.X_CACHE_STATUS,
                    MISS,
                    response);
                Assert.assertEquals(
                    TEST_OK4,
                    CharsetUtils.toString(response.getEntity()));
            }

            // second request should hit cache
            try (CloseableHttpResponse response = client2.execute(
                    new HttpGet(cluster.upstream2().host() + uri)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertHeader(
                    YandexHeaders.X_CACHE_STATUS,
                    HIT,
                    response);
                Assert.assertEquals(
                    TEST_OK4,
                    CharsetUtils.toString(response.getEntity()));
                // Check that Content-TYPE presents only once
                YandexAssert.assertSize(
                    1,
                    Arrays.asList(
                        response.getHeaders(HttpHeaders.CONTENT_TYPE)));
            }
        }
    }

    @Test
    public void testSiblingsTruncate() throws Exception {
        try (LjinxCluster cluster = new LjinxCluster(this, false);
            CloseableHttpClient client1 =
                ClientBuilder.createClient(
                    new HttpTargetConfigBuilder(Configs.targetConfig())
                        .proxy(cluster.ljinx(0).host())
                        .build(),
                    Configs.dnsConfig());
            CloseableHttpClient client2 =
                ClientBuilder.createClient(
                    new HttpTargetConfigBuilder(Configs.targetConfig())
                        .proxy(cluster.ljinx(1).host())
                        .build(),
                    Configs.dnsConfig()))
        {
            String uri = LjinxCluster.PP_SIBLINGS_TRUNCATE;
            cluster.upstream2().add(uri, TEST_OK4);
            cluster.start();

            HttpGet get = new HttpGet(cluster.upstream2().host() + uri);
            // manually add this headers, because second request will have them
            get.addHeader(HttpHeaders.ACCEPT_CHARSET, "UTF-8");
            get.addHeader(HttpHeaders.ACCEPT_ENCODING, "gzip,deflate");
            // first upstream with no cache
            try (CloseableHttpResponse response = client2.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertHeader(
                    YandexHeaders.X_CACHE_STATUS,
                    MISS,
                    response);
                Assert.assertEquals(
                    TEST_OK4,
                    CharsetUtils.toString(response.getEntity()));
            }

            get = new HttpGet(cluster.upstream2().host() + uri);
            get.addHeader(YandexHeaders.X_LJINX_IGNORE_SIBLINGS, YES);

            // second direct request should hit cache
            try (CloseableHttpResponse response = client1.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertHeader(
                    YandexHeaders.X_CACHE_STATUS,
                    HIT,
                    response);
                Assert.assertEquals(
                    TEST_OK4,
                    CharsetUtils.toString(response.getEntity()));
                // Check that Content-TYPE presents only once
                YandexAssert.assertSize(
                    1,
                    Arrays.asList(
                        response.getHeaders(HttpHeaders.CONTENT_TYPE)));
            }
        }
    }

    @Test
    public void testLoadHitsToMemory() throws Exception {
        try (LjinxCluster cluster = new LjinxCluster(this, true);
            CloseableHttpClient client =
                ClientBuilder.createClient(
                    new HttpTargetConfigBuilder(Configs.targetConfig())
                        .proxy(cluster.ljinx().host())
                        .build(),
                    Configs.dnsConfig()))
        {
            cluster.upstream2().add(
                LjinxCluster.PP_HITS_LOADING,
                new StaticHttpResource(
                    new StaticHttpItem(
                        HttpStatus.SC_OK,
                        new ByteArrayEntity(TEST_LUCENE_DATA))));
            cluster.start();

            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.upstream2().host()
                        + LjinxCluster.PP_HITS_LOADING)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertHeader(
                    YandexHeaders.X_CACHE_STATUS,
                    MISS,
                    response);
                Assert.assertArrayEquals(
                    TEST_LUCENE_DATA,
                    CharsetUtils.toByteArray(response.getEntity()));
            }

            //wait for lucene to index
            long deadline =
                System.currentTimeMillis() + LUCENE_INDEX_WAIT_TIMEOUT;
            while (System.currentTimeMillis() < deadline) {
                try {
                    cluster.lucene().checkSearch(
                        NUMDOCS,
                        "{\"docs\":1,\"shards\":\"<any value>\"}");
                    break;
                } catch (java.lang.AssertionError e) {
                    if (e.getMessage().startsWith(STRING_MISMATCH)) {
                        Thread.sleep(LUCENE_INDEX_WAIT_SLEEP);
                    } else {
                        throw e;
                    }
                }
            }

            //drop memory cache
            HttpGet invalidate = new HttpGet(
                cluster.upstream2().host() + LjinxCluster.PP_HITS_LOADING);
            invalidate.addHeader(YandexHeaders.X_CACHE_INVALIDATE, "");
            try (CloseableHttpResponse response = client.execute(invalidate)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            }

            String stats = HttpAssert.stats(cluster.ljinx().port());
            HttpAssert.assertStat(
                MISSES2,
                Integer.valueOf(1).toString(),
                stats);
            HttpAssert.assertStat(
                MEMORY_HITS2,
                Integer.valueOf(0).toString(),
                stats);
            HttpAssert.assertStat(
                DISK_HITS2,
                Integer.valueOf(0).toString(),
                stats);

            //should HIT
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.upstream2().host()
                        + LjinxCluster.PP_HITS_LOADING)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertHeader(
                    YandexHeaders.X_CACHE_STATUS,
                    HIT,
                    response);
                Assert.assertArrayEquals(
                    TEST_LUCENE_DATA,
                    CharsetUtils.toByteArray(response.getEntity()));
                stats = HttpAssert.stats(cluster.ljinx().port());
                HttpAssert.assertStat(
                    MISSES2,
                    Integer.valueOf(1).toString(),
                    stats);
                HttpAssert.assertStat(
                    MEMORY_HITS2,
                    Integer.valueOf(0).toString(),
                    stats);
                HttpAssert.assertStat(
                    DISK_HITS2,
                    Integer.valueOf(1).toString(),
                    stats);
            }

            //should HIT memory
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.upstream2().host()
                        + LjinxCluster.PP_HITS_LOADING)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertHeader(
                    YandexHeaders.X_CACHE_STATUS,
                    HIT,
                    response);
                Assert.assertArrayEquals(
                    TEST_LUCENE_DATA,
                    CharsetUtils.toByteArray(response.getEntity()));
                stats = HttpAssert.stats(cluster.ljinx().port());
                HttpAssert.assertStat(
                    MISSES2,
                    Integer.valueOf(1).toString(),
                    stats);
                HttpAssert.assertStat(
                    MEMORY_HITS2,
                    Integer.valueOf(1).toString(),
                    stats);
                HttpAssert.assertStat(
                    DISK_HITS2,
                    Integer.valueOf(1).toString(),
                    stats);
            }
        }
    }

    @Test
    public void testSiblingsConcat() throws Exception {
        try (LjinxCluster cluster = new LjinxCluster(this, false);
            CloseableHttpClient client =
                ClientBuilder.createClient(
                    new HttpTargetConfigBuilder(Configs.targetConfig())
                        .proxy(cluster.ljinx(0).host())
                        .build(),
                    Configs.dnsConfig());
            StaticServer backend1 =
                new StaticServer(Configs.baseConfig("Backend1"));
            StaticServer backend2 =
                new StaticServer(Configs.baseConfig("Backend2")))
        {
            String uri =
                LjinxCluster.PP_SIBLINGS_CONCAT + "?lhs=" + backend1.host()
                + "&rhs=" + backend2.host();
            String fullUri = cluster.upstream2().host() + uri;
            backend2.add(fullUri, TEST_OK1);

            cluster.start();
            backend2.start();

            try (CloseableHttpResponse response =
                    client.execute(new HttpGet(fullUri)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    TEST_OK1,
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    // CSOFF: MethodLength
    private void testPost(final String uri) throws Exception {
        try (LjinxCluster cluster = new LjinxCluster(this, true);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            String encoding = "secret encoding";
            cluster.upstream2().add(
                uri + PARAM1,
                new HeaderHttpItem(
                    new ExpectingHttpItem(BODY1, TEST_OK1),
                    HTTP.CONTENT_ENCODING,
                    encoding),
                new ExpectingHttpItem(BODY2, TEST_OK2),
                NotImplementedHttpItem.INSTANCE);
            cluster.upstream2().add(
                uri + PARAM2,
                new ExpectingHttpItem(BODY1, TEST_OK2),
                new ExpectingHttpItem(BODY2, TEST_OK1),
                NotImplementedHttpItem.INSTANCE);
            cluster.start();

            HttpPost post =
                new HttpPost(cluster.ljinx().host() + uri + PARAM1);
            post.setEntity(new StringEntity(BODY1));
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertHeader(
                    YandexHeaders.X_CACHE_STATUS,
                    MISS,
                    response);
                HttpAssert.assertHeader(
                    HTTP.CONTENT_ENCODING,
                    encoding,
                    response);
                Assert.assertEquals(
                    TEST_OK1,
                    CharsetUtils.toString(response.getEntity()));
                // Check that Content-Type presents only once
                YandexAssert.assertSize(
                    1,
                    Arrays.asList(
                        response.getHeaders(HttpHeaders.CONTENT_TYPE)));
            }
            // Test memory hit
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertHeader(
                    YandexHeaders.X_CACHE_HIT_TYPE,
                    CacheResponse.CacheType.MEMORY.toString(),
                    response);
                HttpAssert.assertHeader(
                    HTTP.CONTENT_ENCODING,
                    encoding,
                    response);
                Assert.assertEquals(
                    TEST_OK1,
                    CharsetUtils.toString(response.getEntity()));
                // Check that Content-Type presents only once
                YandexAssert.assertSize(
                    1,
                    Arrays.asList(
                        response.getHeaders(HttpHeaders.CONTENT_TYPE)));
            }
            HttpPost invalidate =
                new HttpPost(cluster.ljinx().host() + uri + PARAM1);
            invalidate.setEntity(new StringEntity(BODY1));
            invalidate.addHeader(YandexHeaders.X_CACHE_INVALIDATE, "");
            try (CloseableHttpResponse response = client.execute(invalidate)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            }
            // Wait while first request document will be indexed, so entry will
            // be evicted from tempStoreMap
            Thread.sleep(LUCENE_INDEX_WAIT_SLEEP);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertHeader(
                    YandexHeaders.X_CACHE_HIT_TYPE,
                    CacheResponse.CacheType.DISK.toString(),
                    response);
                HttpAssert.assertHeader(
                    HTTP.CONTENT_ENCODING,
                    encoding,
                    response);
                Assert.assertEquals(
                    TEST_OK1,
                    CharsetUtils.toString(response.getEntity()));
            }
            post.setEntity(new StringEntity(BODY2));
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertHeader(
                    YandexHeaders.X_CACHE_STATUS,
                    MISS,
                    response);
                Assert.assertEquals(
                    TEST_OK2,
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertHeader(
                    YandexHeaders.X_CACHE_STATUS,
                    HIT,
                    response);
                Assert.assertEquals(
                    TEST_OK2,
                    CharsetUtils.toString(response.getEntity()));
            }

            post = new HttpPost(cluster.ljinx().host() + uri + PARAM2);
            post.setEntity(new StringEntity(BODY1));
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertHeader(
                    YandexHeaders.X_CACHE_STATUS,
                    MISS,
                    response);
                Assert.assertEquals(
                    TEST_OK2,
                    CharsetUtils.toString(response.getEntity()));
            }
            // Test memory hit
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertHeader(
                    YandexHeaders.X_CACHE_HIT_TYPE,
                    CacheResponse.CacheType.MEMORY.toString(),
                    response);
                Assert.assertEquals(
                    TEST_OK2,
                    CharsetUtils.toString(response.getEntity()));
            }
            invalidate = new HttpPost(cluster.ljinx().host() + uri + PARAM2);
            invalidate.setEntity(new StringEntity(BODY1));
            invalidate.addHeader(YandexHeaders.X_CACHE_INVALIDATE, "");
            try (CloseableHttpResponse response = client.execute(invalidate)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            }
            // Wait while first request document will be indexed, so entry will
            // be evicted from tempStoreMap
            Thread.sleep(LUCENE_INDEX_WAIT_SLEEP);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertHeader(
                    YandexHeaders.X_CACHE_HIT_TYPE,
                    CacheResponse.CacheType.DISK.toString(),
                    response);
                Assert.assertEquals(
                    TEST_OK2,
                    CharsetUtils.toString(response.getEntity()));
            }
            post.setEntity(new StringEntity(BODY2));
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertHeader(
                    YandexHeaders.X_CACHE_STATUS,
                    MISS,
                    response);
                Assert.assertEquals(
                    TEST_OK1,
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertHeader(
                    YandexHeaders.X_CACHE_STATUS,
                    HIT,
                    response);
                Assert.assertEquals(
                    TEST_OK1,
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }
    // CSON: MethodLength

    @Test
    public void testPost() throws Exception {
        testPost(LjinxCluster.PP_URL6);
    }

    @Test
    public void testPostMd5() throws Exception {
        testPost(LjinxCluster.PP_URL7);
    }

    @Test
    public void testUpstreamClosed() throws Exception {
        try (LjinxCluster cluster = new LjinxCluster(this, false);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.start();
            cluster.upstream2().close();
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.ljinx().host() + LjinxCluster.PP_URL2)))
            {
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_BAD_GATEWAY,
                    response);
                HttpAssert.assertHeader(
                    YandexHeaders.X_CACHE_STATUS,
                    null,
                    response);
            }
        }
    }

    private void testHeadersFilter(final String uri) throws Exception {
        try (LjinxCluster cluster = new LjinxCluster(this, true);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.upstream2().add(
                uri,
                new ExpectingHeaderHttpItem(
                    new StaticHttpItem(TEST_OK1),
                    LjinxCluster.X_MY_HEADER,
                    Boolean.TRUE.toString()),
                new ExpectingHeaderHttpItem(
                    new StaticHttpItem(TEST_OK2),
                    LjinxCluster.X_MY_HEADER,
                    Boolean.FALSE.toString()),
                NotImplementedHttpItem.INSTANCE);
            cluster.start();

            HttpGet get = new HttpGet(cluster.ljinx().host() + uri);
            get.addHeader(LjinxCluster.X_MY_HEADER, Boolean.TRUE.toString());
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertHeader(
                    YandexHeaders.X_CACHE_STATUS,
                    MISS,
                    response);
                Assert.assertEquals(
                    TEST_OK1,
                    CharsetUtils.toString(response.getEntity()));
            }
            // Test memory hit
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertHeader(
                    YandexHeaders.X_CACHE_HIT_TYPE,
                    CacheResponse.CacheType.MEMORY.toString(),
                    response);
                Assert.assertEquals(
                    TEST_OK1,
                    CharsetUtils.toString(response.getEntity()));
            }
            HttpGet invalidate = new HttpGet(cluster.ljinx().host() + uri);
            invalidate.addHeader(
                LjinxCluster.X_MY_HEADER,
                Boolean.TRUE.toString());
            invalidate.addHeader(YandexHeaders.X_CACHE_INVALIDATE, "");
            try (CloseableHttpResponse response = client.execute(invalidate)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            }
            // Wait while first request document will be indexed, so entry will
            // be evicted from tempStoreMap
            Thread.sleep(LUCENE_INDEX_WAIT_SLEEP);
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertHeader(
                    YandexHeaders.X_CACHE_HIT_TYPE,
                    CacheResponse.CacheType.DISK.toString(),
                    response);
                Assert.assertEquals(
                    TEST_OK1,
                    CharsetUtils.toString(response.getEntity()));
                // Check that Content-Type presents only once
                YandexAssert.assertSize(
                    1,
                    Arrays.asList(
                        response.getHeaders(HttpHeaders.CONTENT_TYPE)));
            }

            get = new HttpGet(cluster.ljinx().host() + uri);
            get.addHeader(LjinxCluster.X_MY_HEADER, Boolean.FALSE.toString());
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertHeader(
                    YandexHeaders.X_CACHE_STATUS,
                    MISS,
                    response);
                Assert.assertEquals(
                    TEST_OK2,
                    CharsetUtils.toString(response.getEntity()));
            }
            // Test memory hit
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertHeader(
                    YandexHeaders.X_CACHE_HIT_TYPE,
                    CacheResponse.CacheType.MEMORY.toString(),
                    response);
                Assert.assertEquals(
                    TEST_OK2,
                    CharsetUtils.toString(response.getEntity()));
            }
            invalidate = new HttpGet(cluster.ljinx().host() + uri);
            invalidate.addHeader(
                LjinxCluster.X_MY_HEADER,
                Boolean.FALSE.toString());
            invalidate.addHeader(YandexHeaders.X_CACHE_INVALIDATE, "");
            try (CloseableHttpResponse response = client.execute(invalidate)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            }
            // Wait while first request document will be indexed, so entry will
            // be evicted from tempStoreMap
            Thread.sleep(LUCENE_INDEX_WAIT_SLEEP);
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertHeader(
                    YandexHeaders.X_CACHE_HIT_TYPE,
                    CacheResponse.CacheType.DISK.toString(),
                    response);
                Assert.assertEquals(
                    TEST_OK2,
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testHeadersFilter() throws Exception {
        testHeadersFilter(LjinxCluster.PP_URL6);
    }

    @Test
    public void testHeadersFilterMd5() throws Exception {
        testHeadersFilter(LjinxCluster.PP_URL7);
    }

    @Test
    public void testKeepAlive() throws Exception {
        try (LjinxCluster cluster = new LjinxCluster(this, false);
            CloseableHttpClient client =
                ClientBuilder.createClient(
                    new HttpTargetConfigBuilder(Configs.targetConfig())
                        .contentCompression(false)
                        .briefHeaders(true)
                        .build(),
                    Configs.dnsConfig()))
        {
            cluster.upstream2().add(LjinxCluster.PP_URL2, TEST_OK2 + '\n');
            cluster.start();

            // Init cache
            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.ljinx().host() + LjinxCluster.PP_URL2)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertHeader(
                    YandexHeaders.X_CACHE_STATUS,
                    MISS,
                    response);
                Assert.assertEquals(
                    TEST_OK2 + '\n',
                    CharsetUtils.toString(response.getEntity()));
            }

            try (Socket socket =
                    new Socket(LOCALHOST, cluster.ljinx().port()))
            {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(
                            socket.getInputStream(),
                            StandardCharsets.UTF_8)))
                {
                    // Send request
                    socket.getOutputStream().write(
                        (GET + LjinxCluster.PP_URL2 + HTTP_1_1).getBytes(
                            StandardCharsets.UTF_8));
                    socket.getOutputStream().flush();
                    Assert.assertEquals(HTTP_200_OK, reader.readLine());
                    List<String> headers = new ArrayList<>();
                    while (true) {
                        String line = reader.readLine();
                        if (line.isEmpty()) {
                            break;
                        }
                        headers.add(line);
                    }
                    // Should hit cache
                    YandexAssert.assertContains(
                        YandexHeaders.X_CACHE_STATUS + ':' + ' ' + HIT,
                        headers);
                    Assert.assertEquals(TEST_OK2, reader.readLine());

                    // Send some other request on the same connection
                    socket.getOutputStream().write(
                        (GET + PING + HTTP_1_1)
                        .getBytes(StandardCharsets.UTF_8));
                    socket.getOutputStream().flush();
                    Assert.assertEquals(HTTP_200_OK, reader.readLine());
                }
            }
        }
    }

    @Test
    public void testSiblingsKeepAlive() throws Exception {
        try (LjinxCluster cluster = new LjinxCluster(this, false);
            CloseableHttpClient client =
                ClientBuilder.createClient(
                    new HttpTargetConfigBuilder(Configs.targetConfig())
                        .proxy(cluster.ljinx(0).host())
                        .build(),
                    Configs.dnsConfig()))
        {
            String uri = LjinxCluster.PP_SIBLINGS + "?uid=5";
            cluster.upstream2().add(uri, TEST_OK2 + '\n');
            cluster.start();

            String url = cluster.upstream2().host() + uri;
            // Init cache
            try (CloseableHttpResponse response =
                    client.execute(new HttpGet(url)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertHeader(
                    YandexHeaders.X_CACHE_STATUS,
                    MISS,
                    response);
                Assert.assertEquals(
                    TEST_OK2 + '\n',
                    CharsetUtils.toString(response.getEntity()));
            }

            try (Socket socket =
                    new Socket(LOCALHOST, cluster.ljinx().port()))
            {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(
                            socket.getInputStream(),
                            StandardCharsets.UTF_8)))
                {
                    // Send request
                    socket.getOutputStream().write(
                        (GET + url + HTTP_1_1)
                        .getBytes(StandardCharsets.UTF_8));
                    socket.getOutputStream().flush();
                    Assert.assertEquals(HTTP_200_OK, reader.readLine());
                    List<String> headers = new ArrayList<>();
                    while (true) {
                        String line = reader.readLine();
                        if (line.isEmpty()) {
                            break;
                        }
                        headers.add(line);
                    }
                    // Should hit cache
                    YandexAssert.assertContains(
                        YandexHeaders.X_CACHE_STATUS + ':' + ' ' + HIT,
                        headers);
                    Assert.assertEquals(TEST_OK2, reader.readLine());

                    // Send some other request on the same connection
                    socket.getOutputStream().write(
                        (GET + PING + HTTP_1_1)
                        .getBytes(StandardCharsets.UTF_8));
                    socket.getOutputStream().flush();
                    Assert.assertEquals(HTTP_200_OK, reader.readLine());
                }
            }
        }
    }

    @Test
    public void testUniqueId() throws Exception {
        final long delay = 2000L;
        try (LjinxCluster cluster = new LjinxCluster(this, false);
            SharedConnectingIOReactor reactor = new SharedConnectingIOReactor(
                Configs.baseConfig(),
                Configs.dnsConfig());
            AsyncClient client =
                new AsyncClient(reactor, Configs.targetConfig()))
        {
            String hello = "hello";
            String world = "world";
            cluster.upstream2().add(
                LjinxCluster.PP_UNIQUE_ID,
                new SlowpokeHttpItem(new StaticHttpItem(hello), delay),
                new SlowpokeHttpItem(new StaticHttpItem(world), delay));
            cluster.start();
            reactor.start();
            client.start();

            long start = System.currentTimeMillis();
            Future<HttpResponse> first = client.execute(
                cluster.ljinx().host(),
                new BasicAsyncRequestProducerGenerator(
                    LjinxCluster.PP_UNIQUE_ID),
                BasicAsyncResponseConsumerFactory.INSTANCE,
                EmptyFutureCallback.INSTANCE);
            Thread.sleep(delay >> 2);
            Future<HttpResponse> second = client.execute(
                cluster.ljinx().host(),
                new BasicAsyncRequestProducerGenerator(
                    LjinxCluster.PP_UNIQUE_ID),
                BasicAsyncResponseConsumerFactory.INSTANCE,
                EmptyFutureCallback.INSTANCE);
            HttpResponse response = first.get();
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            Assert.assertEquals(
                hello,
                CharsetUtils.toString(response.getEntity()));
            response = second.get();
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            Assert.assertEquals(
                world,
                CharsetUtils.toString(response.getEntity()));
            YandexAssert.assertLess(
                delay + (delay >> 1),
                System.currentTimeMillis() - start);
        }
    }

    @Test
    public void testGzipBackend() throws Exception {
        try (LjinxCluster cluster = new LjinxCluster(this, false);
            CloseableHttpClient client =
                ClientBuilder.createClient(
                    new HttpTargetConfigBuilder(Configs.targetConfig())
                        .proxy(cluster.ljinx().host())
                        .contentCompression(false)
                        .build(),
                    Configs.dnsConfig()))
        {
            String body = LjinxCluster.genStringData();
            cluster.gzipUpstream().add(LjinxCluster.PP_GZIP, body);
            cluster.start();

            HttpGet get = new HttpGet(
                cluster.gzipUpstream().host() + LjinxCluster.PP_GZIP);
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertHeader(
                    YandexHeaders.X_CACHE_STATUS,
                    MISS,
                    response);
                Assert.assertEquals(
                    body.length(),
                    response.getEntity().getContentLength());
                Assert.assertEquals(
                    body,
                    CharsetUtils.toString(response.getEntity()));
            }

            get = new HttpGet(
                cluster.gzipUpstream().host() + LjinxCluster.PP_GZIP);
            get.addHeader(HttpHeaders.ACCEPT_ENCODING, GZIP);
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                // Accept-Encoding changed, so not cache hit expected
                HttpAssert.assertHeader(
                    YandexHeaders.X_CACHE_STATUS,
                    MISS,
                    response);
                HttpAssert.assertHeader(
                    HttpHeaders.CONTENT_ENCODING,
                    GZIP,
                    response);
                long contentLength = response.getEntity().getContentLength();
                Assert.assertNotEquals(-1L, contentLength);
                YandexAssert.assertLess((long) body.length(), contentLength);
                DecodableByteArrayOutputStream data = IOStreamUtils.consume(
                    new GZIPInputStream(response.getEntity().getContent()));
                Decoder decoder = new Decoder(StandardCharsets.UTF_8);
                data.processWith(decoder);
                Assert.assertEquals(body, decoder.toString());
            }
        }
    }

    @Test
    public void testGzipLjinx() throws Exception {
        try (LjinxCluster cluster = new LjinxCluster(this, false, true);
            CloseableHttpClient client =
                ClientBuilder.createClient(
                    new HttpTargetConfigBuilder(Configs.targetConfig())
                        .proxy(cluster.ljinx().host())
                        .contentCompression(false)
                        .build(),
                    Configs.dnsConfig()))
        {
            String body = LjinxCluster.genStringData();
            cluster.upstream2().add(LjinxCluster.PP_URL4, body);
            cluster.gzipUpstream().add(LjinxCluster.PP_GZIP, body);
            cluster.start();

            HttpGet get = new HttpGet(
                cluster.gzipUpstream().host() + LjinxCluster.PP_GZIP);
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertHeader(
                    YandexHeaders.X_CACHE_STATUS,
                    MISS,
                    response);
                Assert.assertEquals(
                    body.length(),
                    response.getEntity().getContentLength());
                Assert.assertEquals(
                    body,
                    CharsetUtils.toString(response.getEntity()));
            }

            get =
                new HttpGet(cluster.upstream2().host() + LjinxCluster.PP_URL4);
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertHeader(
                    YandexHeaders.X_CACHE_STATUS,
                    MISS,
                    response);
                Assert.assertEquals(
                    body.length(),
                    response.getEntity().getContentLength());
                Assert.assertEquals(
                    body,
                    CharsetUtils.toString(response.getEntity()));
            }

            get = new HttpGet(
                cluster.gzipUpstream().host() + LjinxCluster.PP_GZIP);
            get.addHeader(HttpHeaders.ACCEPT_ENCODING, GZIP);
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                // Accept-Encoding changed, so not cache hit expected
                HttpAssert.assertHeader(
                    YandexHeaders.X_CACHE_STATUS,
                    MISS,
                    response);
                HttpAssert.assertHeader(
                    HttpHeaders.CONTENT_ENCODING,
                    GZIP,
                    response);
                long contentLength = response.getEntity().getContentLength();
                Assert.assertNotEquals(-1L, contentLength);
                YandexAssert.assertLess((long) body.length(), contentLength);
                DecodableByteArrayOutputStream data = IOStreamUtils.consume(
                    new GZIPInputStream(response.getEntity().getContent()));
                Decoder decoder = new Decoder(StandardCharsets.UTF_8);
                data.processWith(decoder);
                Assert.assertEquals(body, decoder.toString());
            }

            get =
                new HttpGet(cluster.upstream2().host() + LjinxCluster.PP_URL4);
            get.addHeader(HttpHeaders.ACCEPT_ENCODING, GZIP);
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                // Accept-Encoding changed, so not cache hit expected
                HttpAssert.assertHeader(
                    YandexHeaders.X_CACHE_STATUS,
                    MISS,
                    response);
                HttpAssert.assertHeader(
                    HttpHeaders.CONTENT_ENCODING,
                    GZIP,
                    response);
                long contentLength = response.getEntity().getContentLength();
                Assert.assertNotEquals(-1L, contentLength);
                YandexAssert.assertLess((long) body.length(), contentLength);
                DecodableByteArrayOutputStream data = IOStreamUtils.consume(
                    new GZIPInputStream(response.getEntity().getContent()));
                Decoder decoder = new Decoder(StandardCharsets.UTF_8);
                data.processWith(decoder);
                Assert.assertEquals(body, decoder.toString());
            }
        }
    }

    @Test
    public void testMdsLjinx() throws Exception {
        try (LjinxCluster cluster = new LjinxCluster(this, true);
            CloseableHttpClient client =
                ClientBuilder.createClient(
                    new HttpTargetConfigBuilder(Configs.targetConfig())
                        .proxy(cluster.ljinx().host())
                        .build(),
                    Configs.dnsConfig()))
        {
            String body = LjinxCluster.genStringData();
            cluster.upstream2().add(LjinxCluster.PP_MDS_URL, body);
            cluster.start();

            // first time upstream with no cache
            HttpGet get =
                new HttpGet(
                    cluster.upstream2().host() + LjinxCluster.PP_MDS_URL);
            String headerValue = "Some ticket here";
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                headerValue);
            get.setHeader(
                HttpHeaders.CONTENT_LENGTH,
                Long.toString(LjinxCluster.LONG_STRING_SIZE));
            HttpResponse response = client.execute(get);
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            HttpAssert.assertHeader(
                YandexHeaders.X_CACHE_STATUS,
                MISS,
                response);
            Assert.assertEquals(
                body,
                CharsetUtils.toString(response.getEntity()));
            // second time upstream with cache
            get =
                new HttpGet(
                    cluster.upstream2().host() + LjinxCluster.PP_MDS_URL);
            get.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                headerValue);
            response = client.execute(get);
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            HttpAssert.assertHeader(
                YandexHeaders.X_CACHE_STATUS,
                HIT,
                response);
            Assert.assertEquals(
                body,
                CharsetUtils.toString(response.getEntity()));
        }
    }

    @Test
    public void testReplacement() throws Exception {
        try (LjinxCluster cluster = new LjinxCluster(this, false);
             CloseableHttpClient client = Configs.createDefaultClient())
        {
            String query = "/123/345";
            cluster.upstream1()
                .add(LjinxCluster.PP_REPLACEMENT_NEW_PATH, TEST_OK1);
            cluster.upstream1()
                .add(LjinxCluster.PP_REPLACEMENT_NEW_PATH2, TEST_OK2);
            cluster.upstream1().add(query, TEST_OK4);
            cluster.start();

            HttpResponse response = client.execute(
                new HttpGet(
                    cluster.ljinx().host() + LjinxCluster.PP_REPLACEMENT));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            HttpAssert.assertHeader(
                YandexHeaders.X_CACHE_STATUS,
                MISS,
                response);
            Assert.assertEquals(
                TEST_OK1,
                CharsetUtils.toString(response.getEntity()));

            response = client.execute(
                new HttpGet(
                    cluster.ljinx().host() + LjinxCluster.PP_REPLACEMENT2));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            HttpAssert.assertHeader(
                YandexHeaders.X_CACHE_STATUS,
                MISS,
                response);
            Assert.assertEquals(
                TEST_OK2,
                CharsetUtils.toString(response.getEntity()));

            response = client.execute(
                new HttpGet(cluster.ljinx().host()
                    + LjinxCluster.PP_REPLACEMENT3 + query));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            HttpAssert.assertHeader(
                YandexHeaders.X_CACHE_STATUS,
                MISS,
                response);
            Assert.assertEquals(
                TEST_OK4,
                CharsetUtils.toString(response.getEntity()));
        }
    }
}

