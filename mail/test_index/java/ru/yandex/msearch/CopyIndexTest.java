package ru.yandex.msearch;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.http.util.YandexHeaders;
import ru.yandex.test.search.backend.TestMailSearchBackend;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

public class CopyIndexTest extends TestBase {
    private static final String CONFIG_SUFFIX = "check-copyness = true\n"
        + "[field.mid]\n"
        + "prefixed = true\n"
        + "analyze = true\n"
        + "store=true\n"
        + "[field.body_text]\n"
        + "tokenizer = letter\n"
        + "prefixed = true\n"
        + "attribute = false\n"
        + "store = true\n"
        + "analyze = true\n";
    private static final String CONFIG_SUFFIX_NOCHECK =
        "[field.mid]\n"
        + "prefixed = true\n"
        + "analyze = true\n"
        + "store=true\n"
        + "[field.body_text]\n"
        + "tokenizer = letter\n"
        + "prefixed = true\n"
        + "attribute = false\n"
        + "store = true\n"
        + "analyze = true\n";
    private static final String CONFIG_SUFFIX_NOCHECK_NOPUSH =
        "fake-queue-ids-push = false\n"
        + "[field.mid]\n"
        + "prefixed = true\n"
        + "analyze = true\n"
        + "store=true\n"
        + "[field.body_text]\n"
        + "tokenizer = letter\n"
        + "prefixed = true\n"
        + "attribute = false\n"
        + "store = true\n"
        + "analyze = true\n";
    private static final AtomicLong queueId = new AtomicLong(0);
    private static final long MAX_COPY_WAIT_TIME = 60000L;

    public static String genQueueId() {
        return Long.toString(queueId.getAndIncrement());
    }

    public static String checkQueueId() {
        return Long.toString(queueId.get() - 1);
    }

    @Test
    public void testFake() throws Exception {
    }

    private String getStatus(final String text) throws Exception {
        int br = text.indexOf("<br>");
        Assert.assertNotEquals(br, -1);
        int nextBr = text.indexOf("<br>", br + 4);
        Assert.assertNotEquals(nextBr, -1);
        final String status = text.substring(br + 4, nextBr);
        int colon = status.indexOf(':');
        Assert.assertNotEquals(colon, -1);
        Assert.assertEquals("status", status.substring(0, colon).trim());
        return status.substring(colon + 1).trim();
    }

    @Test
    public void testCopyIndexSimple() throws Exception {
        System.err.println("testCopyIndexSimple started");
        File srcRoot = Files.createTempDirectory("testCopyIndexSimpleSource").toFile();
        File dstRoot = Files.createTempDirectory("testCopyIndexSimpleDestination").toFile();
        boolean success = false;
        long defaultQueueId = 0;
        try (Daemon source =
                new Daemon(
                    SearchBackendTestBase.config(
                        srcRoot,
                        CONFIG_SUFFIX,
                        1));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            StringBuilder ThreeMbBody = new StringBuilder(1024 * 1024 * 3);
            while (ThreeMbBody.length() < 1024 * 1024 * 3) {
                ThreeMbBody.append("abcdef ");
            }

            HttpPost post = new HttpPost("http://localhost:"
                + source.jsonServerPort() + "/add");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                + "\"mid\":\"100500\","
                + "\"body_text\":\"" + ThreeMbBody.toString() + "\"}]}",
                StandardCharsets.UTF_8));
            post.addHeader(YandexHeaders.ZOO_QUEUE_ID, genQueueId());
            HttpResponse response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            EntityUtils.consume(response.getEntity());
            response = client.execute(new HttpGet("http://localhost:"
                + source.searchServerPort()
                + "/search?prefix=1&get=mid&text=mid:*&check-copyness=false"));
            String text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":1,\"hitsArray\":[{\"mid\":\"100500\"}]}"),
                text);

            StringBuilder OneMbBody = new StringBuilder(1024 * 1024 * 1);
            while (OneMbBody.length() < 1024 * 1024 * 1) {
                OneMbBody.append("abcdef ");
            }

            //default service
            for (int i = 1; i <= 3; i++ ) {
                post.setHeader(YandexHeaders.ZOO_QUEUE_ID, genQueueId());
                post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                    + "\"mid\":\"10050" + i + "\","
                    + "\"body_text\":\"" + OneMbBody.toString() + "\"}]}",
                    StandardCharsets.UTF_8));
                response = client.execute(post);
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: "
                    + EntityUtils.toString(response.getEntity()),
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                EntityUtils.consume(response.getEntity());
            }
            defaultQueueId = queueId.get() - 1;

            //popa service
            for (int i = 1; i <= 3; i++ ) {
                post.setHeader(YandexHeaders.ZOO_QUEUE_ID, genQueueId());
                post.setHeader(YandexHeaders.ZOO_SHARD_ID, "2");
                post.setHeader(YandexHeaders.ZOO_QUEUE, "popa");
                post.setEntity(new StringEntity("{\"prefix\":2,\"docs\":[{"
                    + "\"mid\":\"20050" + i + "\","
                    + "\"body_text\":\"" + OneMbBody.toString() + "\"}]}",
                    StandardCharsets.UTF_8));
                response = client.execute(post);
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: "
                    + EntityUtils.toString(response.getEntity()),
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                EntityUtils.consume(response.getEntity());
            }

            //test check copyness
            response =
                client.execute(
                    new HttpGet(
                        "http://localhost:" + source.jsonServerPort()
                            + "/getQueueId?prefix=1"));
            HttpAssert.assertStatusCode(
                HttpStatus.SC_SERVICE_UNAVAILABLE,
                response);

            response =
                client.execute(
                    new HttpGet(
                        "http://localhost:" + source.jsonServerPort()
                            + "/getQueueId?prefix=1&service=popa"));
            Assert.assertEquals(
                "Expected 503 service unavailable, but received: "
                    + response.getStatusLine(),
                HttpStatus.SC_SERVICE_UNAVAILABLE,
                response.getStatusLine().getStatusCode());

            //search
            response = client.execute(new HttpGet("http://localhost:"
                + source.searchServerPort()
                + "/search?prefix=1&get=mid&text=mid:*&sort=mid&asc"
                + "&check-copyness=false"));
            text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":4,\"hitsArray\":["
                    + "{\"mid\":\"100500\"}"
                    + ",{\"mid\":\"100501\"}"
                    + ",{\"mid\":\"100502\"}"
                    + ",{\"mid\":\"100503\"}"
                    + "]}"),
                text);

            response = client.execute(new HttpGet("http://localhost:"
                + source.searchServerPort()
                + "/search?prefix=2&get=mid&text=mid:*&sort=mid&asc"
                + "&check-copyness=false"));
            text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":3,\"hitsArray\":["
                    + "{\"mid\":\"200501\"}"
                    + ",{\"mid\":\"200502\"}"
                    + ",{\"mid\":\"200503\"}"
                    + "]}"),
                text);

        } //second run (test journals)
        try (Daemon source =
                new Daemon(
                    SearchBackendTestBase.config(
                        srcRoot,
                        CONFIG_SUFFIX,
                        1));
            Daemon destination =
                new Daemon(
                    SearchBackendTestBase.config(
                        dstRoot,
                        "queueid-service-fallback = true\n" + CONFIG_SUFFIX,
                        1));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            final String copyJob =
                "http://localhost:" + destination.dumpPort()
                + "/?copyindex&shards=0-65534&from=localhost:"
                + source.searchPort() + "&sharding-fields=__prefix";

            HttpResponse response =
                client.execute(new HttpGet("http://localhost:" + source.dumpPort()
                    + "/fakecopy"));
            String text = EntityUtils.toString(response.getEntity());
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            Assert.assertEquals("ok", text.trim());

            response = client.execute(new HttpGet(copyJob + "&start"));
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            text = EntityUtils.toString(response.getEntity());
            Assert.assertEquals(
                "running",
                getStatus(text));
            final long start = System.currentTimeMillis();
            boolean finished = false;
            while (System.currentTimeMillis() - start < MAX_COPY_WAIT_TIME) {
                response = client.execute(new HttpGet(copyJob + "&status"));
                text = EntityUtils.toString(response.getEntity());
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: " + text,
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                if (getStatus(text).equals("finished")) {
                    finished = true;
                    break;
                }
                if (getStatus(text).equals("failed")) {
                    break;
                }
                Thread.sleep(1000);
            }
            Assert.assertEquals(true, finished);

            response = client.execute(new HttpGet("http://localhost:"
                + destination.searchServerPort()
                + "/search?prefix=1&get=mid&text=mid:*&sort=mid&asc"));
            text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":4,\"hitsArray\":["
                    + "{\"mid\":\"100500\"}"
                    + ",{\"mid\":\"100501\"}"
                    + ",{\"mid\":\"100502\"}"
                    + ",{\"mid\":\"100503\"}"
                    + "]}"),
                text);

            response = client.execute(new HttpGet("http://localhost:"
                + destination.searchServerPort()
                + "/search?prefix=2&get=mid&text=mid:*&sort=mid&asc"
                + "&service=popa"));
            text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":3,\"hitsArray\":["
                    + "{\"mid\":\"200501\"}"
                    + ",{\"mid\":\"200502\"}"
                    + ",{\"mid\":\"200503\"}"
                    + "]}"),
                text);
            Assert.assertEquals(
                response.getFirstHeader(YandexHeaders.ZOO_QUEUE_ID).getValue(),
                checkQueueId());

            response =
                client.execute(
                    new HttpGet(
                        "http://localhost:" + destination.jsonServerPort()
                            + "/getQueueId?prefix=1"));
            text = EntityUtils.toString(response.getEntity());
            Assert.assertEquals(
                "Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            Assert.assertEquals(Long.toString(defaultQueueId), text);

            //test fallback
            response =
                client.execute(
                    new HttpGet(
                        "http://localhost:" + destination.jsonServerPort()
                            + "/getQueueId?prefix=1&service=popa"));
            text = EntityUtils.toString(response.getEntity());
            Assert.assertEquals(
                "Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            Assert.assertEquals(Long.toString(defaultQueueId), text);
            //test no fallback (request on source)
            response =
                client.execute(
                    new HttpGet(
                        "http://localhost:" + source.jsonServerPort()
                            + "/getQueueId?prefix=1&service=popa"));
            text = EntityUtils.toString(response.getEntity());
            Assert.assertEquals(
                "Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            Assert.assertEquals("-1", text);

            response =
                client.execute(
                    new HttpGet(
                        "http://localhost:" + destination.jsonServerPort()
                            + "/getQueueId?prefix=2&service=popa"));
            text = EntityUtils.toString(response.getEntity());
            Assert.assertEquals(
                "Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            Assert.assertEquals(checkQueueId(), text);

            success = true;
        } finally {
            System.err.println("testCopyIndexSimple ended");
            if (success) {
                SearchBackendTestBase.removeDirectory(srcRoot);
                SearchBackendTestBase.removeDirectory(dstRoot);
            }
        }
    }

    @Test
    public void testCopyQueueIds() throws Exception {
        File srcRoot =
            Files.createTempDirectory("testCopyQueueIdsSource").toFile();
        File dstRoot =
            Files.createTempDirectory("testCopyQueueIdsDestination").toFile();
        boolean success = false;
        StringBuilder bodyShmody = new StringBuilder(1024 * 1024 * 3);
        int i = 0;
        while (bodyShmody.length() < 1024 * 1024 * 3) {
            bodyShmody.append("abcdef ");
            bodyShmody.append("qwe" + i++);
            bodyShmody.append(Integer.toHexString(i));
        }
        String bigQueueId = "2718281828";
        try (Daemon source =
                new Daemon(
                    SearchBackendTestBase.config(
                        srcRoot,
                        CONFIG_SUFFIX_NOCHECK_NOPUSH,
                        10));
            CloseableHttpClient client = HttpClients.createDefault())
        {

            //lucene shard 4, outershard: 0, position 1
            HttpPost post = new HttpPost("http://localhost:"
                + source.jsonServerPort() + "/add");
            post.setEntity(new StringEntity("{\"prefix\":65534,\"docs\":[{"
                + "\"mid\":\"100504\","
                + "\"body_text\":\"" + new String(bodyShmody) + "\"}]}",
                StandardCharsets.UTF_8));

            post.setHeader(YandexHeaders.ZOO_QUEUE_ID, "1");
            post.setHeader(YandexHeaders.ZOO_SHARD_ID, "0");
            post.setHeader(YandexHeaders.ZOO_QUEUE, "service");

            HttpResponse response = client.execute(post);

            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            EntityUtils.consume(response.getEntity());
            response = client.execute(new HttpGet("http://localhost:"
                + source.searchServerPort()
                + "/search?prefix=65534&get=mid&text=mid:*&check-copyness=false"));
            String text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":1,\"hitsArray\":[{"
                    + "\"mid\":\"100504\"}]}"),
                text);

            //lucene shard 0, outershard: 0, position 2
            post.setEntity(new StringEntity("{\"prefix\":0,\"docs\":[{"
                + "\"mid\":\"100500\","
                + "\"body_text\":\"" + new String(bodyShmody) + "\"}]}",
                StandardCharsets.UTF_8));
            post.setHeader(YandexHeaders.ZOO_QUEUE_ID, bigQueueId);
            post.setHeader(YandexHeaders.ZOO_SHARD_ID, "0");
            post.setHeader(YandexHeaders.ZOO_QUEUE, "service");

            response = client.execute(post);

            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            EntityUtils.consume(response.getEntity());
            response = client.execute(new HttpGet("http://localhost:"
                + source.searchServerPort()
                + "/search?prefix=0&get=mid&text=mid:*"));
            text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":1,\"hitsArray\":[{"
                    + "\"mid\":\"100500\"}]}"),
                text);

            //test src queueIds
            response =
                client.execute(
                    new HttpGet(
                        "http://localhost:" + source.jsonServerPort()
                            + "/getQueueId?prefix=0&service=service"));
            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                response);
            text = EntityUtils.toString(response.getEntity());
            Assert.assertEquals(bigQueueId, text);

            response =
                client.execute(
                    new HttpGet(
                        "http://localhost:" + source.jsonServerPort()
                            + "/getQueueId?prefix=65534&service=service"));
            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                response);
            text = EntityUtils.toString(response.getEntity());
            Assert.assertEquals("1", text);
        }
        try (Daemon source =
                new Daemon(
                    SearchBackendTestBase.config(
                        srcRoot,
                        CONFIG_SUFFIX_NOCHECK_NOPUSH,
                        10));
            Daemon destination =
                new Daemon(
                    SearchBackendTestBase.config(
                        dstRoot,
                        CONFIG_SUFFIX,
                        10));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            final String copyJob =
                "http://localhost:" + destination.dumpPort()
                + "/?copyindex&shards=0-65534&threads=1&from=localhost:"
                + source.searchPort() + "&sharding-fields=__prefix";

            HttpResponse response =
                client.execute(new HttpGet("http://localhost:" + source.dumpPort()
                    + "/fakecopy"));
            String text = EntityUtils.toString(response.getEntity());
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            Assert.assertEquals("ok", text.trim());

            response =
                client.execute(new HttpGet(copyJob + "&start"));
            text = EntityUtils.toString(response.getEntity());
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            Assert.assertEquals(
                "running",
                getStatus(text));
            final long start = System.currentTimeMillis();
            boolean finished = false;
            while (System.currentTimeMillis() - start < MAX_COPY_WAIT_TIME) {
                response = client.execute(new HttpGet(copyJob + "&status"));
                text = EntityUtils.toString(response.getEntity());
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: " + text,
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                if (getStatus(text).equals("finished")) {
                    finished = true;
                    break;
                }
                if (getStatus(text).equals("failed")) {
                    break;
                }
                Thread.sleep(1000);
            }
            Assert.assertEquals(true, finished);

            response = client.execute(new HttpGet("http://localhost:"
                + destination.searchServerPort()
                + "/search?prefix=0&get=mid&text=mid:*&sort=mid&asc"));
            text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":1,\"hitsArray\":["
                    + "{\"mid\":\"100500\"}"
                    + "]}"),
                text);

            response = client.execute(new HttpGet("http://localhost:"
                + destination.searchServerPort()
                + "/search?prefix=65534&get=mid&text=mid:*&sort=mid&asc"
                + "&service=service"));
            text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":1,\"hitsArray\":["
                    + "{\"mid\":\"100504\"}"
                    + "]}"),
                text);
            Assert.assertEquals(
                "1",
                response.getFirstHeader(YandexHeaders.ZOO_QUEUE_ID).getValue());

            //test dst queueIds
            response =
                client.execute(
                    new HttpGet(
                        "http://localhost:" + destination.jsonServerPort()
                            + "/getQueueId?prefix=0&service=service"));
            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                response);
            text = EntityUtils.toString(response.getEntity());
            Assert.assertEquals(bigQueueId, text);

            response =
                client.execute(
                    new HttpGet(
                        "http://localhost:" + destination.jsonServerPort()
                            + "/getQueueId?prefix=65534&service=service"));
            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                response);
            text = EntityUtils.toString(response.getEntity());
            Assert.assertEquals("1", text);

            response =
                client.execute(
                    new HttpGet(
                        "http://localhost:" + source.jsonServerPort()
                            + "/getQueueId?shard=0&service=service"));
            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                response);
            text = EntityUtils.toString(response.getEntity());
            Assert.assertEquals("1", text);

            //test dirtyfying
            HttpPost post = new HttpPost("http://localhost:"
                + source.jsonServerPort() + "/add");
            post.setEntity(new StringEntity("{\"prefix\":65534,\"docs\":[{"
                + "\"mid\":\"100505\","
                + "\"body_text\":\"" + new String(bodyShmody) + "\"}]}",
                StandardCharsets.UTF_8));

            post.setHeader(YandexHeaders.ZOO_QUEUE_ID, "10");
            post.setHeader(YandexHeaders.ZOO_SHARD_ID, "0");
            post.setHeader(YandexHeaders.ZOO_QUEUE, "service");

            response = client.execute(post);

            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            EntityUtils.consume(response.getEntity());

            response =
                client.execute(
                    new HttpGet(
                        "http://localhost:" + source.jsonServerPort()
                            + "/getQueueId?shard=0&service=service"));
            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                response);
            text = EntityUtils.toString(response.getEntity());
            Assert.assertEquals("10", text);
            success = true;
        } finally {
            System.err.println("testCopyQueueIds ended");
            if (success) {
                SearchBackendTestBase.removeDirectory(srcRoot);
                SearchBackendTestBase.removeDirectory(dstRoot);
            }
        }
    }

    /**
     * Testing case, when only updatePosition requests was in shard, no docs
     * We should copy position
     * @throws Exception
     */
    @Test
    public void testCopyQueueIdsEmptyDocs() throws Exception {
        Map<String, String> overrides =
            Collections.singletonMap(
                "update-queueid-on-empty-dumpindex",
                "true");

        try (TestMailSearchBackend source = new TestMailSearchBackend(this, overrides);
             TestMailSearchBackend dest = new TestMailSearchBackend(this, overrides);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpPost post = new HttpPost(
                source.indexerUri()
                    + "/delete?updatePosition"
                    + "&shard=5&service=change_log&position=10");
            post.addHeader(YandexHeaders.ZOO_QUEUE_ID, "10");
            post.addHeader(YandexHeaders.ZOO_SHARD_ID, "5");
            post.addHeader(YandexHeaders.ZOO_QUEUE, "change_log");

            post.setEntity(
                new StringEntity(
                    "{\"prefix\":5,\"docs\":[]}",
                    ContentType.APPLICATION_JSON));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);

            String getQidUri = "/getQueueId?shard=5&service=change_log";
            try (CloseableHttpResponse response =
                     client.execute(new HttpGet("http://localhost:"
                         + source.indexerPort() + getQidUri)))
            {
                String text = EntityUtils.toString(response.getEntity());
                Assert.assertEquals("Expected 200 OK, but received: "
                        + response.getStatusLine() + " and body: " + text,
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                Assert.assertEquals("10", text.trim());
            }

            final String copyJob =
                "http://localhost:" + dest.dumpPort()
                    + "/?copyindex&shards=5-5&threads=1&from=localhost:"
                    + source.dumpPort() + "&sharding-fields=__prefix";

            try (CloseableHttpResponse response =
                     client.execute(new HttpGet("http://localhost:"
                         + source.dumpPort()
                         + "/fakecopy?start-shard=5&end-shard=5")))
            {
                String text = EntityUtils.toString(response.getEntity());
                Assert.assertEquals("Expected 200 OK, but received: "
                        + response.getStatusLine() + " and body: " + text,
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                Assert.assertEquals("ok", text.trim());
            }

            try (CloseableHttpResponse response =
                     client.execute(new HttpGet(copyJob + "&start")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    "running",
                    getStatus(CharsetUtils.toString(response.getEntity())));
            }

            final long start = System.currentTimeMillis();
            boolean finished = false;
            while (System.currentTimeMillis() - start < MAX_COPY_WAIT_TIME) {
                try (CloseableHttpResponse response = client.execute(
                    new HttpGet(copyJob + "&status")))
                {
                    String text = EntityUtils.toString(response.getEntity());
                    Assert.assertEquals("Expected 200 OK, but received: "
                            + response.getStatusLine() + " and body: " + text,
                        HttpStatus.SC_OK,
                        response.getStatusLine().getStatusCode());
                    if (getStatus(text).equals("finished")) {
                        finished = true;
                        break;
                    }
                    if (getStatus(text).equals("failed")) {
                        break;
                    }
                }

                Thread.sleep(200);
            }

            try (CloseableHttpResponse response =
                     client.execute(new HttpGet("http://localhost:"
                         + dest.indexerPort() + getQidUri)))
            {
                String text = EntityUtils.toString(response.getEntity());
                Assert.assertEquals("Expected 200 OK, but received: "
                        + response.getStatusLine() + " and body: " + text,
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                Assert.assertEquals("10", text.trim());
            }

            Assert.assertEquals(true, finished);
        }
    }

    @Test
    public void testCopyDeletedDocs() throws Exception {
        File srcRoot =
            Files.createTempDirectory("testCopyDeletedDocsSource").toFile();
        File dstRoot =
            Files.createTempDirectory(
                "testCopyDeletedDocsDestination").toFile();
        boolean success = false;
        StringBuilder bodyShmody = new StringBuilder(1024 * 1024 * 3);
        int d = 0;
        while (bodyShmody.length() < 1024 * 1024 * 3) {
            bodyShmody.append("abcdef ");
            bodyShmody.append("qwe" + d++);
            bodyShmody.append(Integer.toHexString(d));
        }
        try (Daemon source =
                new Daemon(
                    SearchBackendTestBase.config(
                        srcRoot,
                        "use-fast-commit-codec=false\n"
                            + CONFIG_SUFFIX_NOCHECK_NOPUSH,
                        1));
            CloseableHttpClient client = HttpClients.createDefault();
            Daemon destination =
                new Daemon(
                    SearchBackendTestBase.config(
                        dstRoot,
                        CONFIG_SUFFIX,
                        1)))
        {
            HttpPost post = new HttpPost("http://localhost:"
                + source.jsonServerPort() + "/add");
            //add 2 docs
            for (int i = 0; i < 2; i++) {
                post.setEntity(new StringEntity("{\"prefix\":33,\"docs\":[{"
                    + "\"mid\":\"10050" + i + "\","
                    + "\"body_text\":\"" + new String(bodyShmody) + "\"}]}",
                    StandardCharsets.UTF_8));

                HttpResponse response = client.execute(post);
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: "
                    + EntityUtils.toString(response.getEntity()),
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                EntityUtils.consume(response.getEntity());
            }
            //flush
            HttpResponse response = client.execute(
                new HttpGet("http://localhost:"
                    + source.jsonServerPort()
                    + "/flush?wait=true"));
            String text = EntityUtils.toString(response.getEntity());
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            //add another 2 docs
            for (int i = 2; i < 4; i++) {
                post.setEntity(new StringEntity("{\"prefix\":33,\"docs\":[{"
                    + "\"mid\":\"10050" + i + "\","
                    + "\"body_text\":\"" + new String(bodyShmody) + "\"}]}",
                    StandardCharsets.UTF_8));

                response = client.execute(post);
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: "
                    + EntityUtils.toString(response.getEntity()),
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                EntityUtils.consume(response.getEntity());
            }

            response = client.execute(
                new HttpGet("http://localhost:"
                    + source.searchServerPort()
                    + "/search?prefix=33&get=mid&text=mid:*"
                    + "&check-copyness=false&sort=mid&asc"));
            text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":4,\"hitsArray\":["
                    + "{\"mid\":\"100500\"}"
                    + ",{\"mid\":\"100501\"}"
                    + ",{\"mid\":\"100502\"}"
                    + ",{\"mid\":\"100503\"}"
                    + "]}"),
                text);

            response = client.execute(
                new HttpGet("http://localhost:"
                    + source.jsonServerPort()
                    + "/delete?prefix=33&text=mid:100501"));
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());

            response = client.execute(new HttpGet("http://localhost:"
                + source.searchServerPort()
                + "/search?prefix=33&get=mid&text=mid:*"
                + "&check-copyness=false&sort=mid&asc"));
            text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":3,\"hitsArray\":["
                    + "{\"mid\":\"100500\"}"
                    + ",{\"mid\":\"100502\"}"
                    + ",{\"mid\":\"100503\"}"
                    + "]}"),
                text);

            final String copyJob =
                "http://localhost:" + destination.dumpPort()
                + "/?copyindex&shards=32-33&threads=1&from=localhost:"
                + source.searchPort() + "&sharding-fields=__prefix";

            response =
                client.execute(new HttpGet("http://localhost:"
                    + source.dumpPort()
                    + "/fakecopy"));
            text = EntityUtils.toString(response.getEntity());
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            Assert.assertEquals("ok", text.trim());

            response =
                client.execute(new HttpGet(copyJob + "&start"));
            text = EntityUtils.toString(response.getEntity());
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            Assert.assertEquals(
                "running",
                getStatus(text));
            final long start = System.currentTimeMillis();
            boolean finished = false;
            while (System.currentTimeMillis() - start < MAX_COPY_WAIT_TIME) {
                response = client.execute(new HttpGet(copyJob + "&status"));
                text = EntityUtils.toString(response.getEntity());
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: " + text,
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                if (getStatus(text).equals("finished")) {
                    finished = true;
                    break;
                }
                if (getStatus(text).equals("failed")) {
                    break;
                }
                Thread.sleep(1000);
            }
            Assert.assertEquals(true, finished);

            response = client.execute(new HttpGet("http://localhost:"
                + destination.searchServerPort()
                + "/search?prefix=33&get=mid&text=mid:*&sort=mid&asc"
                + "&check-copyness=false"));
            text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":3,\"hitsArray\":["
                    + "{\"mid\":\"100500\"}"
                    + ",{\"mid\":\"100502\"}"
                    + ",{\"mid\":\"100503\"}"
                    + "]}"),
                text);
        } finally {
            System.err.println("testCopyDeletedDocs ended");
            if (success) {
                SearchBackendTestBase.removeDirectory(srcRoot);
                SearchBackendTestBase.removeDirectory(dstRoot);
            }
        }
    }
}

