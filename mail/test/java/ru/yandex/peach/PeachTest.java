package ru.yandex.peach;

import java.nio.charset.StandardCharsets;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.http.test.Configs;
import ru.yandex.http.test.ExpectingHttpItem;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.test.NotImplementedHttpItem;
import ru.yandex.http.test.SlowpokeHttpItem;
import ru.yandex.http.test.StaticHttpItem;
import ru.yandex.http.test.StaticServer;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.http.util.YandexHeaders;
import ru.yandex.json.dom.JsonObject;
import ru.yandex.json.dom.TypesafeValueContentHandler;
import ru.yandex.test.search.backend.TestSearchBackend;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

public class PeachTest extends TestBase {
    private static final String URI = "/uri?prefix=";
    private static final String TASK0 = "&task-seq=0";
    private static final String TASK1 = "&task-seq=1";
    private static final String TASK2 = "&task-seq=2";
    private static final String DEADLINE_PARAM = "deadline";
    private static final String DEADLINE = '&' + DEADLINE_PARAM + '=' + '*';
    private static final String RO = "/flush?ro";
    private static final String RW = "/flush?rw";
    private static final long WAIT_INTERVAL = 3000L;
    private static final long SHARDS = 65534L;

    @Test
    public void test() throws Exception {
        try (PeachCluster cluster = new PeachCluster(this);
            CloseableHttpClient client = HttpClients.custom()
                .setProxy(cluster.peachHost())
                .build())
        {
            cluster.backend().add(URI + 0 + TASK0, HttpStatus.SC_OK);
            cluster.start();
            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                client,
                new HttpGet(cluster.backend().host() + URI + 0));
            Thread.sleep(WAIT_INTERVAL);
            Assert.assertEquals(
                1,
                cluster.backend().accessCount(URI + 0 + TASK0));

            // Test that client status presents
            try (CloseableHttpResponse response = client.execute(
                new HttpGet(cluster.peachHost() + "/status")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                JsonObject json = TypesafeValueContentHandler.parse(
                    CharsetUtils.toString(response.getEntity()));
                JsonObject queues = json.get("queues");
                JsonObject queue = queues.get("null");
                JsonObject clientJson = queue.get("client");
                JsonObject status = clientJson.get("status");
                Assert.assertEquals("ACTIVE", status.asString());
            }
        }
    }

    @Test
    public void testPost() throws Exception {
        try (PeachCluster cluster = new PeachCluster(
                this,
                PeachCluster.config().payloadField("stid"));
            CloseableHttpClient client = HttpClients.custom()
                .setProxy(cluster.peachHost())
                .build())
        {
            String text = "Привет, мир!\nМиру — мир!";
            cluster.backend().add(
                URI + 0 + TASK0,
                new ExpectingHttpItem(text));
            cluster.start();
            HttpPost post = new HttpPost(cluster.backend().host() + URI + 0);
            post.setEntity(new StringEntity(text, StandardCharsets.UTF_8));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);
            Thread.sleep(WAIT_INTERVAL);
            Assert.assertEquals(
                1,
                cluster.backend().accessCount(URI + 0 + TASK0));
        }
    }

    @Test
    public void testPortsMapping() throws Exception {
        try (TestSearchBackend storage = new TestSearchBackend(this, PeachCluster.CONFIG);
            StaticServer backend = new StaticServer(Configs.baseConfig()))
        {
            backend.add(URI + 0 + TASK0, HttpStatus.SC_OK);
            backend.start();
            PeachConfigBuilder config = PeachCluster.config();
            final int port = 80;
            config.localPortsMapping().put(port, backend.port());
            try (PeachCluster cluster =
                    new PeachCluster(storage, backend, config);
                CloseableHttpClient client = HttpClients.custom()
                    .setProxy(cluster.peachHost())
                    .build())
            {
                cluster.start();
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_OK,
                    client,
                    new HttpGet("http://localhost:80" + URI + 0));
                Thread.sleep(WAIT_INTERVAL);
                Assert.assertEquals(
                    1,
                    cluster.backend().accessCount(URI + 0 + TASK0));
            }
        }
    }

    @Test
    public void testRetries() throws Exception {
        PeachConfigBuilder config = PeachCluster.config();
        config.queuesConfig().get(null).deadlineParam(DEADLINE_PARAM);
        try (PeachCluster cluster = new PeachCluster(this, config);
            CloseableHttpClient client = HttpClients.custom()
                .setProxy(cluster.peachHost())
                .build())
        {
            cluster.backend().add(
                URI + 0 + TASK0 + DEADLINE,
                new StaticHttpItem(HttpStatus.SC_FORBIDDEN),
                new StaticHttpItem(HttpStatus.SC_OK),
                NotImplementedHttpItem.INSTANCE);
            cluster.backend().add(
                URI + 1 + TASK0 + DEADLINE,
                new StaticHttpItem(HttpStatus.SC_BAD_GATEWAY),
                new StaticHttpItem(HttpStatus.SC_GATEWAY_TIMEOUT),
                new StaticHttpItem(HttpStatus.SC_OK),
                NotImplementedHttpItem.INSTANCE);
            cluster.backend().add(
                URI + 1 + TASK1 + DEADLINE,
                new StaticHttpItem(HttpStatus.SC_BAD_REQUEST),
                NotImplementedHttpItem.INSTANCE);
            cluster.start();
            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                client,
                new HttpGet(cluster.backend().host() + URI + 0));
            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                client,
                new HttpGet(cluster.backend().host() + URI + 1));
            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                client,
                new HttpGet(cluster.backend().host() + URI + 1));
            // Wait for a while, then wait for URI + 1 two retries
            Thread.sleep(
                WAIT_INTERVAL
                + config.queuesConfig().get(null).retriesIntervalMax() * 2);
            Assert.assertEquals(
                2,
                cluster.backend().accessCount(URI + 0 + TASK0 + DEADLINE));
            Assert.assertEquals(
                2 + 1,
                cluster.backend().accessCount(URI + 1 + TASK0 + DEADLINE));
            Assert.assertEquals(
                1,
                cluster.backend().accessCount(URI + 1 + TASK1 + DEADLINE));
        }
    }

    @Test
    public void testQueues() throws Exception {
        String queueName = "Queue";
        PeachConfigBuilder config = PeachCluster.config();
        config.queuesConfig().get(null).deadlineParam(DEADLINE_PARAM);
        config.queuesConfig().put(queueName, PeachCluster.queueConfig());
        try (PeachCluster cluster = new PeachCluster(this, config);
            CloseableHttpClient client = HttpClients.custom()
                .setProxy(cluster.peachHost())
                .build())
        {
            // Default queue
            cluster.backend().add(
                URI + 0 + TASK0 + DEADLINE,
                new SlowpokeHttpItem(
                    new StaticHttpItem(HttpStatus.SC_OK),
                    WAIT_INTERVAL));
            // Named queue
            cluster.backend().add(
                URI + 0 + TASK0,
                new SlowpokeHttpItem(
                    new StaticHttpItem(HttpStatus.SC_OK),
                    WAIT_INTERVAL));
            cluster.start();
            // Default queue request
            HttpGet get = new HttpGet(cluster.backend().host() + URI + 0);
            get.addHeader(YandexHeaders.X_PEACH_QUEUE, queueName);
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, get);
            // Named queue request
            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                client,
                new HttpGet(cluster.backend().host() + URI + 0));
            Thread.sleep(WAIT_INTERVAL + (WAIT_INTERVAL >> 2L));
            Assert.assertEquals(
                1,
                cluster.backend().accessCount(URI + 0 + TASK0));
            Assert.assertEquals(
                1,
                cluster.backend().accessCount(URI + 0 + TASK0 + DEADLINE));
        }
    }

    @Test
    public void testPersistency() throws Exception {
        try (TestSearchBackend storage = new TestSearchBackend(this, PeachCluster.CONFIG);
            StaticServer backend = new StaticServer(Configs.baseConfig()))
        {
            String queueName = "reindex";
            PeachConfigBuilder config = PeachCluster.config();
            config.queuesConfig().get(null).deadlineParam(DEADLINE_PARAM);
            config.queuesConfig().put(queueName, PeachCluster.queueConfig());
            try (PeachCluster cluster =
                    new PeachCluster(storage, backend, config);
                CloseableHttpClient client = HttpClients.custom()
                    .setProxy(cluster.peachHost())
                    .build())
            {
                cluster.start();
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_OK,
                    client,
                    new HttpGet(backend.host() + URI + 0));
                HttpGet get = new HttpGet(cluster.backend().host() + URI + 0);
                get.addHeader(YandexHeaders.X_PEACH_QUEUE, queueName);
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, get);
                Thread.sleep(WAIT_INTERVAL);
            }
            // Process backlog
            backend.add(URI + 0 + TASK0, HttpStatus.SC_SERVICE_UNAVAILABLE);
            backend.add(
                URI + 0 + TASK0 + DEADLINE,
                HttpStatus.SC_SERVICE_UNAVAILABLE);
            backend.start();
            Thread.sleep(WAIT_INTERVAL);
            backend.add(
                URI + 0 + TASK0,
                new SlowpokeHttpItem(
                    new StaticHttpItem(HttpStatus.SC_OK),
                    WAIT_INTERVAL));
            backend.add(
                URI + 0 + TASK0 + DEADLINE,
                new SlowpokeHttpItem(
                    new StaticHttpItem(HttpStatus.SC_OK),
                    WAIT_INTERVAL));
            try (PeachCluster cluster =
                    new PeachCluster(storage, backend, config))
            {
                cluster.start();
                Thread.sleep(WAIT_INTERVAL + (WAIT_INTERVAL >> 2L));
                Assert.assertEquals(1, backend.accessCount(URI + 0 + TASK0));
                Assert.assertEquals(
                    1,
                    backend.accessCount(URI + 0 + TASK0 + DEADLINE));
            }
        }
    }

    @Test
    public void testQueue() throws Exception {
        String suffix = "&queue-suffix";
        PeachConfigBuilder config = PeachCluster.config();
        config.queuesConfig().get(null).batchSize(2);
        try (PeachCluster cluster = new PeachCluster(this, config);
            CloseableHttpClient client = HttpClients.custom()
                .setProxy(cluster.peachHost())
                .build())
        {
            cluster.backend().add(
                URI + 0 + TASK0,
                new SlowpokeHttpItem(
                    new StaticHttpItem(HttpStatus.SC_OK),
                    WAIT_INTERVAL),
                NotImplementedHttpItem.INSTANCE);
            cluster.backend().add(
                URI + SHARDS + suffix + TASK1,
                new SlowpokeHttpItem(
                    new StaticHttpItem(HttpStatus.SC_OK),
                    WAIT_INTERVAL),
                NotImplementedHttpItem.INSTANCE);
            cluster.backend().add(
                URI + (SHARDS + SHARDS) + suffix + 0 + TASK2,
                HttpStatus.SC_OK);
            cluster.start();
            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                client,
                new HttpGet(cluster.backend().host() + URI + 0));
            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                client,
                new HttpGet(cluster.backend().host() + URI + SHARDS + suffix));
            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                client,
                new HttpGet(
                    cluster.backend().host() + URI
                    + (SHARDS + SHARDS) + suffix + 0));
            Thread.sleep(WAIT_INTERVAL >> 1);
            Assert.assertEquals(
                0,
                cluster.backend().accessCount(URI + 0 + TASK0));
            Assert.assertEquals(
                0,
                cluster.backend().accessCount(URI + SHARDS + suffix + TASK1));
            Assert.assertEquals(
                0,
                cluster.backend().accessCount(
                    URI + (SHARDS + SHARDS) + suffix + 0 + TASK2));
            Thread.sleep(WAIT_INTERVAL);
            Assert.assertEquals(
                1,
                cluster.backend().accessCount(URI + 0 + TASK0));
            Assert.assertEquals(
                0,
                cluster.backend().accessCount(URI + SHARDS + suffix + TASK1));
            Assert.assertEquals(
                0,
                cluster.backend().accessCount(
                    URI + (SHARDS + SHARDS) + suffix + 0 + TASK2));
            Thread.sleep(WAIT_INTERVAL);
            Assert.assertEquals(
                1,
                cluster.backend().accessCount(URI + 0 + TASK0));
            Assert.assertEquals(
                1,
                cluster.backend().accessCount(URI + SHARDS + suffix + TASK1));
            Assert.assertEquals(
                1,
                cluster.backend().accessCount(
                    URI + (SHARDS + SHARDS) + suffix + 0 + TASK2));
        }
    }

    @Test
    public void testStorageFailure() throws Exception {
        try (PeachCluster cluster = new PeachCluster(this);
            CloseableHttpClient client = HttpClients.custom()
                .setProxy(cluster.peachHost())
                .build())
        {
            cluster.backend().add(URI + 0 + TASK1, HttpStatus.SC_OK);
            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                new HttpGet(cluster.storage().indexerUri() + RO));
            cluster.start();
            HttpAssert.assertStatusCode(
                HttpStatus.SC_SERVICE_UNAVAILABLE,
                client,
                new HttpGet(cluster.backend().host() + URI + 0));
            Assert.assertEquals(
                0,
                cluster.backend().accessCount(URI + 0 + TASK1));

            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                new HttpGet(cluster.storage().indexerUri() + RW));
            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                client,
                new HttpGet(cluster.backend().host() + URI + 0));
            Thread.sleep(WAIT_INTERVAL);
            Assert.assertEquals(
                1,
                cluster.backend().accessCount(URI + 0 + TASK1));
        }
    }

    @Test
    public void testPersistentStorageFailure() throws Exception {
        try (TestSearchBackend storage = new TestSearchBackend(this, PeachCluster.CONFIG);
            StaticServer backend = new StaticServer(Configs.baseConfig()))
        {
            try (PeachCluster cluster = new PeachCluster(storage, backend);
                CloseableHttpClient client = HttpClients.custom()
                    .setProxy(cluster.peachHost())
                    .build())
            {
                cluster.start();
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_OK,
                    client,
                    new HttpGet(backend.host() + URI + 0));
            }
            // Process backlog
            backend.add(URI + 0 + TASK0, HttpStatus.SC_SERVICE_UNAVAILABLE);
            backend.start();
            Thread.sleep(WAIT_INTERVAL);
            backend.add(URI + 0 + TASK0, HttpStatus.SC_OK);
            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                new HttpGet(storage.indexerUri() + RO));
            try (PeachCluster cluster = new PeachCluster(storage, backend)) {
                cluster.start();
                Thread.sleep(WAIT_INTERVAL);
                YandexAssert.assertGreater(
                    1,
                    backend.accessCount(URI + 0 + TASK0));
            }
        }
    }

    @Test
    public void testParallel() throws Exception {
        try (TestSearchBackend storage = new TestSearchBackend(this, PeachCluster.CONFIG);
            StaticServer backend = new StaticServer(Configs.baseConfig()))
        {
            try (PeachCluster cluster = new PeachCluster(storage, backend);
                CloseableHttpClient client = HttpClients.custom()
                    .setProxy(cluster.peachHost())
                    .build())
            {
                cluster.start();
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_OK,
                    client,
                    new HttpGet(backend.host() + URI + 0));
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_OK,
                    client,
                    new HttpGet(backend.host() + URI + SHARDS));
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_OK,
                    client,
                    new HttpGet(backend.host() + URI + (SHARDS + SHARDS)));
            }
            // Process backlog
            backend.add(URI + 0 + TASK0, HttpStatus.SC_SERVICE_UNAVAILABLE);
            backend.add(
                URI + SHARDS + TASK1,
                HttpStatus.SC_SERVICE_UNAVAILABLE);
            backend.add(
                URI + (SHARDS + SHARDS) + TASK2,
                HttpStatus.SC_SERVICE_UNAVAILABLE);
            backend.start();
            Thread.sleep(WAIT_INTERVAL);
            backend.add(
                URI + 0 + TASK0,
                new SlowpokeHttpItem(
                    new StaticHttpItem(HttpStatus.SC_OK),
                    WAIT_INTERVAL),
                NotImplementedHttpItem.INSTANCE);
            backend.add(
                URI + SHARDS + TASK1,
                new SlowpokeHttpItem(
                    new StaticHttpItem(HttpStatus.SC_OK),
                    WAIT_INTERVAL),
                NotImplementedHttpItem.INSTANCE);
            backend.add(
                URI + SHARDS + TASK1,
                new SlowpokeHttpItem(
                    new StaticHttpItem(HttpStatus.SC_OK),
                    WAIT_INTERVAL),
                NotImplementedHttpItem.INSTANCE);
            backend.add(
                URI + (SHARDS + SHARDS) + TASK2,
                new SlowpokeHttpItem(
                    new StaticHttpItem(HttpStatus.SC_OK),
                    WAIT_INTERVAL),
                NotImplementedHttpItem.INSTANCE);
            PeachConfigBuilder config = PeachCluster.config();
            config.queuesConfig().get(null).batchSize(2).parallel(true);
            try (PeachCluster cluster =
                    new PeachCluster(storage, backend, config))
            {
                cluster.start();
                Assert.assertEquals(
                    0,
                    backend.accessCount(URI + 0 + TASK0));
                Assert.assertEquals(
                    0,
                    backend.accessCount(URI + SHARDS + TASK1));
                Assert.assertEquals(
                    0,
                    backend.accessCount(URI + (SHARDS + SHARDS) + TASK2));
                Thread.sleep(WAIT_INTERVAL + (WAIT_INTERVAL >> 2L));
                Assert.assertEquals(
                    1,
                    backend.accessCount(URI + 0 + TASK0));
                Assert.assertEquals(
                    1,
                    backend.accessCount(URI + SHARDS + TASK1));
                Assert.assertEquals(
                    0,
                    backend.accessCount(URI + (SHARDS + SHARDS) + TASK2));
                Thread.sleep(WAIT_INTERVAL + (WAIT_INTERVAL >> 2L));
                Assert.assertEquals(
                    1,
                    backend.accessCount(URI + 0 + TASK0));
                Assert.assertEquals(
                    1,
                    backend.accessCount(URI + SHARDS + TASK1));
                Assert.assertEquals(
                    1,
                    backend.accessCount(URI + (SHARDS + SHARDS) + TASK2));
            }
        }
    }
}

