package ru.yandex.msearch;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.util.YandexHeaders;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

public abstract class PrimaryKeyTestBase extends TestBase {
    private static final Charset UTF8 = Charset.forName("utf-8");
    private static final AtomicLong queueId = new AtomicLong(0);

    public static String genQueueId() {
        return Long.toString(queueId.getAndIncrement());
    }

    public static String checkQueueId() {
        return Long.toString(queueId.get() - 1);
    }

    public static void copyJournal(
        final File source,
        final File target,
        final int shard)
        throws IOException
    {
        String path = "index/" + shard + "/journal";
        File journal = new File(target, path);
        journal.mkdirs();
        Path dst = journal.toPath();
        for (File file: new File(source, path).listFiles()) {
            Path src = file.toPath();
            Files.copy(src, dst.resolve(src.getFileName()));
        }
    }

    public abstract Config config(final File root, final String suffix)
        throws Exception;

    @Test
    public void testAdd() throws Exception {
        File root = Files.createTempDirectory("testAddPrimaryKey").toFile();
        try (Daemon daemon = new Daemon(config(root, "primary_key = keyword"));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                + "\"keyword\":\"some keyword\",\"text\":\"иди\"}]}",
                UTF8));
            post.setHeader(YandexHeaders.ZOO_QUEUE_ID, genQueueId());
            HttpResponse response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()).trim(),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            EntityUtils.consume(response.getEntity());
            post.setHeader(YandexHeaders.ZOO_QUEUE_ID, genQueueId());
            response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()).trim(),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            EntityUtils.consume(response.getEntity());
            post.setHeader(YandexHeaders.ZOO_QUEUE_ID, genQueueId());
            response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()).trim(),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            EntityUtils.consume(response.getEntity());
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&"
                + "text=text:иду&get=keyword&json-type=dollar"));
            String text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{$hitsCount\0:1,$hitsArray\0:[{"
                    + "$keyword\0:$some keyword\0}]}"),
                text);

            response = client.execute(new HttpGet(
                "http://localhost:" + daemon.jsonServerPort()
                + "/getQueueId?prefix=1"));
            text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            Assert.assertEquals(checkQueueId(), text);
            File root2 =
                Files.createTempDirectory("testAddPKJournal").toFile();
            try {
                copyJournal(root, root2, 1);
                try (Daemon daemon2 =
                        new Daemon(config(root2, "primary_key = keyword")))
                {
                    response = client.execute(new HttpGet(
                        "http://localhost:" + daemon2.searchServerPort()
                        + "/search?prefix=1&text=text:иду&get=keyword"));
                    text = EntityUtils.toString(response.getEntity()).trim();
                    Assert.assertEquals("Expected 200 OK, but received: "
                        + response.getStatusLine() + " and body: " + text,
                        HttpStatus.SC_OK,
                        response.getStatusLine().getStatusCode());
                    YandexAssert.check(
                        new JsonChecker(
                            "{\"hitsCount\":1,\"hitsArray\":[{"
                            + "\"keyword\":\"some keyword\"}]}"),
                        text);
                    response = client.execute(new HttpGet(
                        "http://localhost:" + daemon2.jsonServerPort()
                        + "/getQueueId?prefix=1"));
                    text = EntityUtils.toString(response.getEntity()).trim();
                    Assert.assertEquals("Expected 200 OK, but received: "
                        + response.getStatusLine() + " and body: " + text,
                        HttpStatus.SC_OK,
                        response.getStatusLine().getStatusCode());
                    Assert.assertEquals(checkQueueId(), text);
                }
            } finally {
                SearchBackendTestBase.removeDirectory(root2);
            }
        } finally {
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testAddWithFailOnDuplicateKey() throws Exception {
        File root = Files.createTempDirectory("testAddPrimaryKeyFailOnDuplicateKey").toFile();
        try (Daemon daemon = new Daemon(config(root, "primary_key = keyword"));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(new StringEntity("{\"prefix\":1,\"FailOnDuplicateKey\":true,\"docs\":[{"
                + "\"keyword\":\"some keyword\",\"text\":\"иди\"}]}",
                UTF8));
            post.setHeader(YandexHeaders.ZOO_QUEUE_ID, genQueueId());
            HttpResponse response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()).trim(),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            EntityUtils.consume(response.getEntity());
            post.setHeader(YandexHeaders.ZOO_QUEUE_ID, genQueueId());
            response = client.execute(post);
            Assert.assertEquals("Expected 409 CONFLICT, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()).trim(),
                HttpStatus.SC_CONFLICT,
                response.getStatusLine().getStatusCode());
            EntityUtils.consume(response.getEntity());
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&"
                + "text=text:иду&get=keyword&json-type=dollar"));
            String text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{$hitsCount\0:1,$hitsArray\0:[{"
                    + "$keyword\0:$some keyword\0}]}"),
                text);
            File root2 =
                Files.createTempDirectory("testAddPKJournal").toFile();
            try {
                copyJournal(root, root2, 1);
                try (Daemon daemon2 =
                        new Daemon(config(root2, "primary_key = keyword")))
                {
                    response = client.execute(new HttpGet(
                        "http://localhost:" + daemon2.searchServerPort()
                        + "/search?prefix=1&text=text:иду&get=keyword"));
                    text = EntityUtils.toString(response.getEntity()).trim();
                    Assert.assertEquals("Expected 200 OK, but received: "
                        + response.getStatusLine() + " and body: " + text,
                        HttpStatus.SC_OK,
                        response.getStatusLine().getStatusCode());
                    YandexAssert.check(
                        new JsonChecker(
                            "{\"hitsCount\":1,\"hitsArray\":[{"
                            + "\"keyword\":\"some keyword\"}]}"),
                        text);
                    response = client.execute(new HttpGet(
                        "http://localhost:" + daemon2.jsonServerPort()
                        + "/getQueueId?prefix=1"));
                    text = EntityUtils.toString(response.getEntity()).trim();
                    Assert.assertEquals("Expected 200 OK, but received: "
                        + response.getStatusLine() + " and body: " + text,
                        HttpStatus.SC_OK,
                        response.getStatusLine().getStatusCode());
                    Assert.assertEquals(checkQueueId(), text);
                }
            } finally {
                SearchBackendTestBase.removeDirectory(root2);
            }
        } finally {
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testModify() throws Exception {
        File root = Files.createTempDirectory("testModifyPrimaryKey").toFile();
        try (Daemon daemon = new Daemon(config(root, "primary_key = keyword"));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                + "\"keyword\":\"some keyword\",\"property\":\"1\"}]}",
                UTF8));
            post.setHeader(YandexHeaders.ZOO_QUEUE_ID, genQueueId());
            HttpResponse response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()).trim(),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            EntityUtils.consume(response.getEntity());
            post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/modify");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                + "\"keyword\":\"some keyword\",\"property\":\"2\"},"
                + "{\"keyword\":\"another keyword\",\"property\":\"3\"}]}",
                UTF8));
            post.setHeader(YandexHeaders.ZOO_QUEUE_ID, genQueueId());
            response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()).trim(),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            EntityUtils.consume(response.getEntity());
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort()
                + "/search?prefix=1&sort=property&asc&"
                + "text=keyword:*keyword&get=keyword,property"));
            String text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":2,\"hitsArray\":[{"
                    + "\"keyword\":\"some keyword\",\"property\":\"2\"},"
                    + "{\"keyword\":\"another keyword\",\"property\":\"3\"}]}"),
                text);
            File root2 =
                Files.createTempDirectory("testModifyPKJournal").toFile();
            try {
                copyJournal(root, root2, 1);
                try (Daemon daemon2 =
                        new Daemon(config(root2, "primary_key = keyword")))
                {
                    response = client.execute(new HttpGet(
                        "http://localhost:" + daemon2.searchServerPort()
                        + "/search?prefix=1&sort=property&asc=Yes&"
                        + "text=keyword:*keyword&get=keyword,property&"));
                    text = EntityUtils.toString(response.getEntity()).trim();
                    Assert.assertEquals("Expected 200 OK, but received: "
                        + response.getStatusLine() + " and body: " + text,
                        HttpStatus.SC_OK,
                        response.getStatusLine().getStatusCode());
                    YandexAssert.check(
                        new JsonChecker(
                            "{\"hitsCount\":2,\"hitsArray\":[{"
                            + "\"keyword\":\"some keyword\",\"property\":\"2\""
                            + "},{\"keyword\":\"another keyword\","
                            + "\"property\":\"3\"}]}"),
                        text);
                    response = client.execute(new HttpGet(
                        "http://localhost:" + daemon2.jsonServerPort()
                        + "/getQueueId?prefix=1"));
                    text = EntityUtils.toString(response.getEntity()).trim();
                    Assert.assertEquals("Expected 200 OK, but received: "
                        + response.getStatusLine() + " and body: " + text,
                        HttpStatus.SC_OK,
                        response.getStatusLine().getStatusCode());
                    Assert.assertEquals(checkQueueId(), text);
                }
            } finally {
                SearchBackendTestBase.removeDirectory(root2);
            }
        } finally {
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testDelete() throws Exception {
        File root = Files.createTempDirectory("testDeletePrimaryKey").toFile();
        try (Daemon daemon = new Daemon(config(root, "primary_key = keyword"));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                + "\"keyword\":\"some keyword\",\"property\":\"1\"},"
                + "{\"keyword\":\"another keyword\"}]}", UTF8));
            post.setHeader(YandexHeaders.ZOO_QUEUE_ID, genQueueId());
            HttpResponse response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()).trim(),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            EntityUtils.consume(response.getEntity());
            post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/delete");
            post.setEntity(new StringEntity(
                "{\"prefix\":1,\"docs\":[{\"keyword\":\"some keyword\"},"
                + "{\"keyword\":\"third keyword\",\"property\":\"3\"}]}",
                UTF8));
            post.setHeader(YandexHeaders.ZOO_QUEUE_ID, genQueueId());
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);
            post.setEntity(new StringEntity("{\"prefix\":2,\"docs\":[]}"));
            post.setHeader(YandexHeaders.ZOO_QUEUE_ID, genQueueId());
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);
            String secondQueueId = checkQueueId();
            post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                + "\"keyword\":\"some keyword\",\"property\":\"3\"},"
                + "{\"keyword\":\"another keyword\",\"property\":\"4\"}]}",
                UTF8));
            post.setHeader(YandexHeaders.ZOO_QUEUE_ID, genQueueId());
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);
            String firstQueueId = checkQueueId();
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort()
                + "/search?prefix=1&sort=keyword&asc=1&"
                + "text=keyword:*keyword&get=keyword,property"));
            String text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":2,\"hitsArray\":["
                    + "{\"keyword\":\"another keyword\",\"property\":null},"
                    + "{\"keyword\":\"some keyword\",\"property\":\"3\"}]}"),
                text);
            File root2 =
                Files.createTempDirectory("testDeletePKJournal").toFile();
            try {
                copyJournal(root, root2, 1);
                copyJournal(root, root2, 2);
                try (Daemon daemon2 =
                        new Daemon(config(root2, "primary_key = keyword")))
                {
                    response = client.execute(new HttpGet(
                        "http://localhost:" + daemon2.searchServerPort()
                        + "/search?prefix=1&sort=keyword&asc=1&"
                        + "text=keyword:*keyword&get=keyword,property"));
                    text = EntityUtils.toString(response.getEntity()).trim();
                    Assert.assertEquals("Expected 200 OK, but received: "
                        + response.getStatusLine() + " and body: " + text,
                        HttpStatus.SC_OK,
                        response.getStatusLine().getStatusCode());
                    YandexAssert.check(
                        new JsonChecker(
                            "{\"hitsCount\":2,\"hitsArray\":["
                            + "{\"keyword\":\"another keyword\","
                            + "\"property\":null},{\"keyword\":"
                            + "\"some keyword\",\"property\":\"3\"}]}"),
                            text);
                    response = client.execute(new HttpGet(
                        "http://localhost:" + daemon2.jsonServerPort()
                        + "/getQueueId?prefix=1"));
                    text = EntityUtils.toString(response.getEntity()).trim();
                    Assert.assertEquals("Expected 200 OK, but received: "
                        + response.getStatusLine() + " and body: " + text,
                        HttpStatus.SC_OK,
                        response.getStatusLine().getStatusCode());
                    Assert.assertEquals(firstQueueId, text);
                    response = client.execute(new HttpGet(
                        "http://localhost:" + daemon2.jsonServerPort()
                        + "/getQueueId?prefix=2"));
                    text = EntityUtils.toString(response.getEntity()).trim();
                    Assert.assertEquals("Expected 200 OK, but received: "
                        + response.getStatusLine() + " and body: " + text,
                        HttpStatus.SC_OK,
                        response.getStatusLine().getStatusCode());
                    Assert.assertEquals(secondQueueId, text);
                }
            } finally {
                SearchBackendTestBase.removeDirectory(root2);
            }
        } finally {
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testDeleteByQuery() throws Exception {
        File root = Files.createTempDirectory("testDeleteByQueryPK").toFile();
        try (Daemon daemon =
                new Daemon(config(root, "primary_key = keyword"));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                + "\"keyword\":\"some keyword\",\"property\":\"1\"},"
                + "{\"keyword\":\"another keyword\"}]}", UTF8));
            post.setHeader(YandexHeaders.ZOO_QUEUE_ID, genQueueId());
            HttpResponse response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()).trim(),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            EntityUtils.consume(response.getEntity());
            response = client.execute(new HttpGet(
                "http://localhost:" + daemon.jsonServerPort()
                + "/delete?prefix=1&text=keyword:some*"));
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()).trim(),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            EntityUtils.consume(response.getEntity());
            post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                + "\"keyword\":\"some keyword\",\"property\":\"3\"}]}",
                UTF8));
            post.setHeader(YandexHeaders.ZOO_QUEUE_ID, genQueueId());
            response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()).trim(),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            EntityUtils.consume(response.getEntity());
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort()
                + "/search?prefix=1&sort=keyword&asc=1&"
                + "text=keyword:*keyword&get=keyword,property"));
            String text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":2,\"hitsArray\":["
                    + "{\"keyword\":\"another keyword\",\"property\":null},"
                    + "{\"keyword\":\"some keyword\",\"property\":\"3\"}]}"),
                text);
            File root2 = Files.createTempDirectory(
                "testDeleteByQyeryPKJournal").toFile();
            try {
                copyJournal(root, root2, 1);
                try (Daemon daemon2 =
                        new Daemon(config(root2, "primary_key = keyword")))
                {
                    response = client.execute(new HttpGet(
                        "http://localhost:" + daemon2.searchServerPort()
                        + "/search?prefix=1&sort=keyword&asc=1&"
                        + "text=keyword:*keyword&get=keyword,property"));
                    text = EntityUtils.toString(response.getEntity()).trim();
                    Assert.assertEquals("Expected 200 OK, but received: "
                        + response.getStatusLine() + " and body: " + text,
                        HttpStatus.SC_OK,
                        response.getStatusLine().getStatusCode());
                    YandexAssert.check(
                        new JsonChecker(
                            "{\"hitsCount\":2,\"hitsArray\":["
                            + "{\"keyword\":\"another keyword\","
                            + "\"property\":null},{\"keyword\":"
                            + "\"some keyword\",\"property\":\"3\"}]}"),
                        text);
                    response = client.execute(new HttpGet(
                        "http://localhost:" + daemon2.jsonServerPort()
                        + "/getQueueId?prefix=1"));
                    text = EntityUtils.toString(response.getEntity()).trim();
                    Assert.assertEquals("Expected 200 OK, but received: "
                        + response.getStatusLine() + " and body: " + text,
                        HttpStatus.SC_OK,
                        response.getStatusLine().getStatusCode());
                    Assert.assertEquals(checkQueueId(), text);
                }
            } finally {
                SearchBackendTestBase.removeDirectory(root2);
            }
        } finally {
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testUpdate() throws Exception {
        File root = Files.createTempDirectory("testUpdatePK").toFile();
        try (Daemon daemon = new Daemon(config(root, "primary_key = keyword"));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                + "\"keyword\":\"some keyword\",\"property\":\"1\"},{"
                + "\"keyword\":\"another keyword\",\"attribute\":\"3\"}]}",
                UTF8));
            post.setHeader(YandexHeaders.ZOO_QUEUE_ID, genQueueId());
            HttpResponse response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()).trim(),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            EntityUtils.consume(response.getEntity());
            post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/update");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                + "\"property\":\"4\",\"keyword\":\"another keyword\"}]}",
                UTF8));
            post.setHeader(YandexHeaders.ZOO_QUEUE_ID, genQueueId());
            response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()).trim(),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            EntityUtils.consume(response.getEntity());
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort()
                + "/search?prefix=1&sort=keyword&asc=1&"
                + "text=keyword:*keyword&"
                + "get=__prefix,keyword,property,attribute"));
            String text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":2,\"hitsArray\":["
                    + "{\"__prefix\":\"1\",\"keyword\":\"another keyword\","
                    + "\"property\":\"4\",\"attribute\":\"3\"},"
                    + "{\"__prefix\":\"1\",\"keyword\":\"some keyword\","
                    + "\"property\":\"1\",\"attribute\":null}]}"),
                text);
            File root2 = Files.createTempDirectory(
                "testUpdatePKJournal").toFile();
            try {
                copyJournal(root, root2, 1);
                try (Daemon daemon2 =
                        new Daemon(config(root2, "primary_key = keyword")))
                {
                    response = client.execute(new HttpGet(
                        "http://localhost:" + daemon2.searchServerPort()
                        + "/search?prefix=1&get=__prefix,keyword,property"
                        + ",attribute"
                        + "&sort=keyword&asc=1&text=keyword:*keyword"));
                    text = EntityUtils.toString(response.getEntity()).trim();
                    Assert.assertEquals("Expected 200 OK, but received: "
                        + response.getStatusLine() + " and body: " + text,
                        HttpStatus.SC_OK,
                        response.getStatusLine().getStatusCode());
                    YandexAssert.check(
                        new JsonChecker(
                            "{\"hitsCount\":2,\"hitsArray\":["
                            + "{\"__prefix\":\"1\",\"keyword\":\"another keyword\""
                            + ",\"property\":\"4\",\"attribute\":\"3\"},{"
                            + "\"__prefix\":\"1\",\"keyword\":\"some keyword\","
                            + "\"property\":\"1\",\"attribute\":null}]}"),
                        text);
                    response = client.execute(new HttpGet(
                        "http://localhost:" + daemon2.jsonServerPort()
                        + "/getQueueId?prefix=1"));
                    text = EntityUtils.toString(response.getEntity()).trim();
                    Assert.assertEquals("Expected 200 OK, but received: "
                        + response.getStatusLine() + " and body: " + text,
                        HttpStatus.SC_OK,
                        response.getStatusLine().getStatusCode());
                    Assert.assertEquals(checkQueueId(), text);
                }
            } finally {
                SearchBackendTestBase.removeDirectory(root2);
            }
        } finally {
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testCondUpdate() throws Exception {
        File root = Files.createTempDirectory("testUpdatePK").toFile();
        try (Daemon daemon = new Daemon(config(root, "primary_key = keyword"));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                + "\"keyword\":\"some keyword\",\"property\":\"1\"},{"
                + "\"keyword\":\"another keyword\",\"attribute\":\"3\"}]}",
                UTF8));
            post.setHeader(YandexHeaders.ZOO_QUEUE_ID, genQueueId());
            HttpResponse response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()).trim(),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            EntityUtils.consume(response.getEntity());
            post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/update");
            post.setEntity(new StringEntity("{\"prefix\":1,\"conds\":{\"test1\":\"keyword:(some keyword1)\"},\"docs\":[{"
                + "\"property\":{\"function\":\"case\",\"args\":[100,\"test2\",-100]},\"keyword\":\"another keyword\"}]}",
                UTF8));
            post.setHeader(YandexHeaders.ZOO_QUEUE_ID, genQueueId());
            response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()).trim(),
                HttpStatus.SC_BAD_REQUEST,
                response.getStatusLine().getStatusCode());
            EntityUtils.consume(response.getEntity());
            post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/update");
            post.setEntity(new StringEntity("{\"prefix\":1,\"conds\":{\"test1\":\"keyword:(some keyword1)\"},\"docs\":[{"
                + "\"property\":{\"function\":\"case\",\"args\":[100,\"test1\",-100]},\"keyword\":\"another keyword\"}]}",
                UTF8));
            post.setHeader(YandexHeaders.ZOO_QUEUE_ID, genQueueId());
            response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()).trim(),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            EntityUtils.consume(response.getEntity());
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort()
                + "/search?prefix=1&sort=keyword&asc=1&text=keyword:*keyword"
                + "&get=__prefix,keyword,property,attribute"));
            String text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":2,\"hitsArray\":["
                    + "{\"__prefix\":\"1\",\"keyword\":\"another keyword\","
                    + "\"property\":\"100\",\"attribute\":\"3\"},"
                    + "{\"__prefix\":\"1\",\"keyword\":\"some keyword\","
                    + "\"property\":\"1\",\"attribute\":null}]}"),
                text);

            post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/update");
            post.setEntity(new StringEntity("{\"prefix\":1,\"conds\":{\"test1\":\"keyword:*keyword\"},\"docs\":[{"
                + "\"property\":{\"function\":\"case\",\"args\":[100,\"test1\",-100]},\"keyword\":\"another keyword\"}]}",
                UTF8));
            post.setHeader(YandexHeaders.ZOO_QUEUE_ID, genQueueId());
            response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()).trim(),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            EntityUtils.consume(response.getEntity());
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort()
                + "/search?prefix=1&sort=keyword&asc=1&text=keyword:*keyword"
                + "&get=__prefix,keyword,property,attribute"));
            text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":2,\"hitsArray\":["
                    + "{\"__prefix\":\"1\",\"keyword\":\"another keyword\","
                    + "\"property\":\"-100\",\"attribute\":\"3\"},"
                    + "{\"__prefix\":\"1\",\"keyword\":\"some keyword\","
                    + "\"property\":\"1\",\"attribute\":null}]}"),
                text);
            File root2 = Files.createTempDirectory(
                "testUpdatePKJournal").toFile();
            try {
                copyJournal(root, root2, 1);
                try (Daemon daemon2 =
                        new Daemon(config(root2, "primary_key = keyword")))
                {
                    response = client.execute(new HttpGet(
                        "http://localhost:" + daemon2.searchServerPort()
                        + "/search?prefix=1&get=__prefix,keyword,property"
                        + ",attribute"
                        + "&sort=keyword&asc=1&text=keyword:*keyword"));
                    text = EntityUtils.toString(response.getEntity()).trim();
                    Assert.assertEquals("Expected 200 OK, but received: "
                        + response.getStatusLine() + " and body: " + text,
                        HttpStatus.SC_OK,
                        response.getStatusLine().getStatusCode());
                    YandexAssert.check(
                        new JsonChecker(
                            "{\"hitsCount\":2,\"hitsArray\":["
                            + "{\"__prefix\":\"1\",\"keyword\":\"another keyword\""
                            + ",\"property\":\"-100\",\"attribute\":\"3\"},{"
                            + "\"__prefix\":\"1\",\"keyword\":\"some keyword\","
                            + "\"property\":\"1\",\"attribute\":null}]}"),
                        text);
                    response = client.execute(new HttpGet(
                        "http://localhost:" + daemon2.jsonServerPort()
                        + "/getQueueId?prefix=1"));
                    text = EntityUtils.toString(response.getEntity()).trim();
                    Assert.assertEquals("Expected 200 OK, but received: "
                        + response.getStatusLine() + " and body: " + text,
                        HttpStatus.SC_OK,
                        response.getStatusLine().getStatusCode());
                    Assert.assertEquals(checkQueueId(), text);
                }
            } finally {
                SearchBackendTestBase.removeDirectory(root2);
            }
        } finally {
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testCondUpdateAddIfNotExists() throws Exception {
        System.err.println("testCondUpdateAddIfNotExists started");
        File root = Files.createTempDirectory("testUpdatePK").toFile();
        try (Daemon daemon = new Daemon(config(root, "primary_key = keyword"));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                + "\"keyword\":\"some keyword\",\"property\":\"1\"}]}",
                UTF8));
            post.setHeader(YandexHeaders.ZOO_QUEUE_ID, genQueueId());
            HttpResponse response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()).trim(),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            EntityUtils.consume(response.getEntity());
            post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/update");
            post.setEntity(new StringEntity("{\"prefix\":1,\"conds\":{\"test1\":\"keyword:(some keyword1)\"},\"docs\":[{"
                + "\"property\":{\"function\":\"case\",\"args\":[100,\"test2\",-100]},\"keyword\":\"another keyword\"}]}",
                UTF8));
            post.setHeader(YandexHeaders.ZOO_QUEUE_ID, genQueueId());
            response = client.execute(post);
            Assert.assertEquals("Expected 400 Bad Request, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()).trim(),
                HttpStatus.SC_BAD_REQUEST,
                response.getStatusLine().getStatusCode());
            EntityUtils.consume(response.getEntity());
            post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/update");
            post.setEntity(new StringEntity("{\"prefix\":1,\"AddIfNotExists\":true,\"conds\":{\"test1\":\"keyword:(some keyword1)\"},\"docs\":[{"
                + "\"property\":{\"function\":\"case\",\"args\":[100,\"test1\",-100]},\"keyword\":\"another keyword\",\"attribute\":3}]}",
                UTF8));
            post.setHeader(YandexHeaders.ZOO_QUEUE_ID, genQueueId());
            response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()).trim(),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            EntityUtils.consume(response.getEntity());
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort()
                + "/search?prefix=1&sort=keyword&asc=1&text=keyword:*keyword"
                + "&get=__prefix,keyword,property,attribute"));
            String text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":2,\"hitsArray\":["
                    + "{\"__prefix\":\"1\",\"keyword\":\"another keyword\","
                    + "\"property\":\"100\",\"attribute\":\"3\"},"
                    + "{\"__prefix\":\"1\",\"keyword\":\"some keyword\","
                    + "\"property\":\"1\",\"attribute\":null}]}"),
                text);

            post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/update");
            post.setEntity(new StringEntity("{\"prefix\":1,\"conds\":{\"test1\":\"keyword:\\\"another keyword\\\"\"},\"docs\":[{"
                + "\"property\":{\"function\":\"case\",\"args\":[100,\"test1\",-100]},\"keyword\":\"another keyword\"}]}",
                UTF8));
            post.setHeader(YandexHeaders.ZOO_QUEUE_ID, genQueueId());
            response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()).trim(),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            EntityUtils.consume(response.getEntity());
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort()
                + "/search?prefix=1&sort=keyword&asc=1&text=keyword:*keyword"
                + "&get=__prefix,keyword,property,attribute"));
            text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":2,\"hitsArray\":["
                    + "{\"__prefix\":\"1\",\"keyword\":\"another keyword\","
                    + "\"property\":\"-100\",\"attribute\":\"3\"},"
                    + "{\"__prefix\":\"1\",\"keyword\":\"some keyword\","
                    + "\"property\":\"1\",\"attribute\":null}]}"),
                text);
            File root2 = Files.createTempDirectory(
                "testUpdatePKJournal").toFile();
            try {
                copyJournal(root, root2, 1);
                try (Daemon daemon2 =
                        new Daemon(config(root2, "primary_key = keyword")))
                {
                    response = client.execute(new HttpGet(
                        "http://localhost:" + daemon2.searchServerPort()
                        + "/search?prefix=1&get=__prefix,keyword,property,"
                        + "attribute"
                        + "&sort=keyword&asc=1&text=keyword:*keyword"));
                    text = EntityUtils.toString(response.getEntity()).trim();
                    Assert.assertEquals("Expected 200 OK, but received: "
                        + response.getStatusLine() + " and body: " + text,
                        HttpStatus.SC_OK,
                        response.getStatusLine().getStatusCode());
                    YandexAssert.check(
                        new JsonChecker(
                            "{\"hitsCount\":2,\"hitsArray\":["
                            + "{\"__prefix\":\"1\",\"keyword\":\"another keyword\""
                            + ",\"property\":\"-100\",\"attribute\":\"3\"},{"
                            + "\"__prefix\":\"1\",\"keyword\":\"some keyword\","
                            + "\"property\":\"1\",\"attribute\":null}]}"),
                        text);
                    response = client.execute(new HttpGet(
                        "http://localhost:" + daemon2.jsonServerPort()
                        + "/getQueueId?prefix=1"));
                    text = EntityUtils.toString(response.getEntity()).trim();
                    Assert.assertEquals("Expected 200 OK, but received: "
                        + response.getStatusLine() + " and body: " + text,
                        HttpStatus.SC_OK,
                        response.getStatusLine().getStatusCode());
                    Assert.assertEquals(checkQueueId(), text);
                }
            } finally {
                SearchBackendTestBase.removeDirectory(root2);
            }
        } finally {
            System.err.println("testCondUpdateAddIfNotExists ended");
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testCondUpdateByQuery() throws Exception {
        File root = Files.createTempDirectory("testUpdatePK").toFile();
        try (Daemon daemon = new Daemon(config(root, "primary_key = keyword"));
            CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                + "\"keyword\":\"some keyword\",\"property\":\"1\"},{"
                + "\"keyword\":\"another keyword\",\"attribute\":\"3\"}]}",
                UTF8));
            post.setHeader(YandexHeaders.ZOO_QUEUE_ID, genQueueId());
            HttpResponse response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()).trim(),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            EntityUtils.consume(response.getEntity());
            post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/update");
            post.setEntity(new StringEntity("{\"prefix\":1,\"query\":\"keyword:\\\"another keyword\\\"\",\"conds\":{\"test1\":\"keyword:(some keyword1)\"},\"docs\":[{"
                + "\"property\":{\"function\":\"case\",\"args\":[100,\"test2\",-100]}}]}",
                UTF8));
            post.setHeader(YandexHeaders.ZOO_QUEUE_ID, genQueueId());
            response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()).trim(),
                HttpStatus.SC_BAD_REQUEST,
                response.getStatusLine().getStatusCode());
            EntityUtils.consume(response.getEntity());
            post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/update");
            post.setEntity(new StringEntity("{\"prefix\":1,\"query\":\"keyword:\\\"another keyword\\\"\",\"conds\":{\"test1\":\"keyword:(some keyword1)\"},\"docs\":[{"
                + "\"property\":{\"function\":\"case\",\"args\":[100,\"test1\",-100]}}]}",
                UTF8));
            post.setHeader(YandexHeaders.ZOO_QUEUE_ID, genQueueId());
            response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()).trim(),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            EntityUtils.consume(response.getEntity());
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort()
                + "/search?prefix=1&sort=keyword&asc=1&text=keyword:*keyword"
                + "&get=__prefix,keyword,property,attribute"));
            String text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":2,\"hitsArray\":["
                    + "{\"__prefix\":\"1\",\"keyword\":\"another keyword\","
                    + "\"property\":\"100\",\"attribute\":\"3\"},"
                    + "{\"__prefix\":\"1\",\"keyword\":\"some keyword\","
                    + "\"property\":\"1\",\"attribute\":null}]}"),
                text);

            post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/update");
            post.setEntity(new StringEntity("{\"prefix\":1,\"query\":\"keyword:\\\"another keyword\\\"\",\"conds\":{\"test1\":\"keyword:\\\"some keyword\\\"\"},\"docs\":[{"
                + "\"property\":{\"function\":\"case\",\"args\":[100,\"test1\",-100]}}]}",
                UTF8));
            post.setHeader(YandexHeaders.ZOO_QUEUE_ID, genQueueId());
            response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()).trim(),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            EntityUtils.consume(response.getEntity());
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort()
                + "/search?prefix=1&sort=keyword&asc=1&text=keyword:*keyword"
                + "&get=__prefix,keyword,property,attribute"));
            text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":2,\"hitsArray\":["
                    + "{\"__prefix\":\"1\",\"keyword\":\"another keyword\","
                    + "\"property\":\"-100\",\"attribute\":\"3\"},"
                    + "{\"__prefix\":\"1\",\"keyword\":\"some keyword\","
                    + "\"property\":\"1\",\"attribute\":null}]}"),
                text);
            File root2 = Files.createTempDirectory(
                "testUpdatePKJournal").toFile();
            try {
                copyJournal(root, root2, 1);
                try (Daemon daemon2 =
                        new Daemon(config(root2, "primary_key = keyword")))
                {
                    response = client.execute(new HttpGet(
                        "http://localhost:" + daemon2.searchServerPort()
                        + "/search?prefix=1&get=__prefix,keyword,property"
                        + ",attribute"
                        + "&sort=keyword&asc=1&text=keyword:*keyword"));
                    text = EntityUtils.toString(response.getEntity()).trim();
                    Assert.assertEquals("Expected 200 OK, but received: "
                        + response.getStatusLine() + " and body: " + text,
                        HttpStatus.SC_OK,
                        response.getStatusLine().getStatusCode());
                    YandexAssert.check(
                        new JsonChecker(
                            "{\"hitsCount\":2,\"hitsArray\":["
                            + "{\"__prefix\":\"1\",\"keyword\":\"another keyword\""
                            + ",\"property\":\"-100\",\"attribute\":\"3\"},{"
                            + "\"__prefix\":\"1\",\"keyword\":\"some keyword\","
                            + "\"property\":\"1\",\"attribute\":null}]}"),
                        text);
                    response = client.execute(new HttpGet(
                        "http://localhost:" + daemon2.jsonServerPort()
                        + "/getQueueId?prefix=1"));
                    text = EntityUtils.toString(response.getEntity()).trim();
                    Assert.assertEquals("Expected 200 OK, but received: "
                        + response.getStatusLine() + " and body: " + text,
                        HttpStatus.SC_OK,
                        response.getStatusLine().getStatusCode());
                    Assert.assertEquals(checkQueueId(), text);
                }
            } finally {
                SearchBackendTestBase.removeDirectory(root2);
            }
        } finally {
            SearchBackendTestBase.removeDirectory(root);
        }
    }
    @Test
    public void testUpdateDeleteField() throws Exception {
        File root =
            Files.createTempDirectory("testUpdateDeleteFieldPK").toFile();
        try (Daemon daemon = new Daemon(config(root, "primary_key = keyword"));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                + "\"keyword\":\"some keyword\",\"property\":\"1\"},{"
                + "\"keyword\":\"another keyword\",\"attribute\":\"3\"}]}",
                UTF8));
            post.setHeader(YandexHeaders.ZOO_QUEUE_ID, genQueueId());
            HttpResponse response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()).trim(),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            EntityUtils.consume(response.getEntity());
            post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/update");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                + "\"property\":\"4\",\"keyword\":\"another keyword\","
                + "\"attribute\":null}]}",
                UTF8));
            post.setHeader(YandexHeaders.ZOO_QUEUE_ID, genQueueId());
            response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()).trim(),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            EntityUtils.consume(response.getEntity());
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort()
                + "/search?prefix=1&sort=keyword&asc=1&text=keyword:*keyword"
                + "&get=__prefix,keyword,property,attribute"));
            String text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":2,\"hitsArray\":["
                    + "{\"__prefix\":\"1\",\"keyword\":\"another keyword\","
                    + "\"property\":\"4\",\"attribute\":null},{"
                    + "\"__prefix\":\"1\",\"keyword\":\"some keyword\","
                    + "\"property\":\"1\",\"attribute\":null}]}"),
                text);
            File root2 = Files.createTempDirectory(
                "testUpdateDeleteFieldPKJournal").toFile();
            try {
                copyJournal(root, root2, 1);
                try (Daemon daemon2 =
                        new Daemon(config(root2, "primary_key = keyword")))
                {
                    response = client.execute(new HttpGet(
                        "http://localhost:" + daemon2.searchServerPort()
                        + "/search?prefix=1&get=__prefix,keyword,property"
                        + ",attribute"
                        + "&sort=keyword&asc=1&text=keyword:*keyword"));
                    text = EntityUtils.toString(response.getEntity()).trim();
                    Assert.assertEquals("Expected 200 OK, but received: "
                        + response.getStatusLine() + " and body: " + text,
                        HttpStatus.SC_OK,
                        response.getStatusLine().getStatusCode());
                    YandexAssert.check(
                        new JsonChecker(
                            "{\"hitsCount\":2,\"hitsArray\":["
                            + "{\"__prefix\":\"1\",\"keyword\":\"another keyword\""
                            + ",\"property\":\"4\",\"attribute\":null},{"
                            + "\"__prefix\":\"1\",\"keyword\":\"some keyword\","
                            + "\"property\":\"1\",\"attribute\":null}]}"),
                        text);
                    response = client.execute(new HttpGet(
                        "http://localhost:" + daemon2.jsonServerPort()
                        + "/getQueueId?prefix=1"));
                    text = EntityUtils.toString(response.getEntity()).trim();
                    Assert.assertEquals("Expected 200 OK, but received: "
                        + response.getStatusLine() + " and body: " + text,
                        HttpStatus.SC_OK,
                        response.getStatusLine().getStatusCode());
                    Assert.assertEquals(checkQueueId(), text);
                }
            } finally {
                SearchBackendTestBase.removeDirectory(root2);
            }
        } finally {
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testUpdateByQuery() throws Exception {
        File root = Files.createTempDirectory("testUpdateByQueryPK").toFile();
        try (Daemon daemon = new Daemon(config(root, "primary_key = keyword"));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                + "\"keyword\":\"some keyword\",\"property\":\"1\"},{"
                + "\"keyword\":\"another keyword\",\"attribute\":\"3\"}]}",
                UTF8));
            post.setHeader(YandexHeaders.ZOO_QUEUE_ID, genQueueId());
            HttpResponse response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()).trim(),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            EntityUtils.consume(response.getEntity());
            post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/update");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                + "\"property\":\"4\"}],"
                + "\"query\":\"keyword:another\\\\ key?ord\"}", UTF8));
            post.setHeader(YandexHeaders.ZOO_QUEUE_ID, genQueueId());
            response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()).trim(),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            EntityUtils.consume(response.getEntity());
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort()
                + "/search?prefix=1&sort=keyword&asc=1&text=keyword:*keyword"
                + "&get=__prefix,keyword,property,attribute"));
            String text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":2,\"hitsArray\":["
                    + "{\"__prefix\":\"1\",\"keyword\":\"another keyword\","
                    + "\"property\":\"4\",\"attribute\":\"3\"},"
                    + "{\"__prefix\":\"1\",\"keyword\":\"some keyword\","
                    + "\"property\":\"1\",\"attribute\":null}]}"),
                text);
            File root2 = Files.createTempDirectory(
                "testUpdateByQyeryPKJournal").toFile();
            try {
                copyJournal(root, root2, 1);
                try (Daemon daemon2 =
                        new Daemon(config(root2, "primary_key = keyword")))
                {
                    response = client.execute(new HttpGet(
                        "http://localhost:" + daemon2.searchServerPort()
                        + "/search?prefix=1&get=__prefix,keyword,property"
                        + ",attribute"
                        + "&sort=keyword&asc=1&text=keyword:*keyword"));
                    text = EntityUtils.toString(response.getEntity()).trim();
                    Assert.assertEquals("Expected 200 OK, but received: "
                        + response.getStatusLine() + " and body: " + text,
                        HttpStatus.SC_OK,
                        response.getStatusLine().getStatusCode());
                    YandexAssert.check(
                        new JsonChecker(
                            "{\"hitsCount\":2,\"hitsArray\":["
                            + "{\"__prefix\":\"1\",\"keyword\":\"another keyword\""
                            + ",\"property\":\"4\",\"attribute\":\"3\"},{"
                            + "\"__prefix\":\"1\",\"keyword\":\"some keyword\","
                            + "\"property\":\"1\",\"attribute\":null}]}"),
                        text);
                    response = client.execute(new HttpGet(
                        "http://localhost:" + daemon2.jsonServerPort()
                        + "/getQueueId?prefix=1"));
                    text = EntityUtils.toString(response.getEntity()).trim();
                    Assert.assertEquals("Expected 200 OK, but received: "
                        + response.getStatusLine() + " and body: " + text,
                        HttpStatus.SC_OK,
                        response.getStatusLine().getStatusCode());
                    Assert.assertEquals(checkQueueId(), text);
                }
            } finally {
                SearchBackendTestBase.removeDirectory(root2);
            }
        } finally {
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testUpdateByQueryDeleteField() throws Exception {
        File root = Files.createTempDirectory("testUpdateByQueryDeleteFieldPK")
            .toFile();
        try (Daemon daemon = new Daemon(config(root, "primary_key = keyword"));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                + "\"keyword\":\"some keyword\",\"property\":\"1\"},{"
                + "\"keyword\":\"another keyword\",\"attribute\":\"3\"}]}",
                UTF8));
            post.setHeader(YandexHeaders.ZOO_QUEUE_ID, genQueueId());
            HttpResponse response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()).trim(),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            EntityUtils.consume(response.getEntity());
            post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/update");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                + "\"property\":\"4\",\"attribute\":null}],"
                + "\"query\":\"keyword:another\\\\ key?ord\"}", UTF8));
            post.setHeader(YandexHeaders.ZOO_QUEUE_ID, genQueueId());
            response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()).trim(),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            EntityUtils.consume(response.getEntity());
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort()
                + "/search?prefix=1&sort=keyword&asc&"
                + "text=keyword:*keyword&get=__prefix,keyword,property"
                + ",attribute"));
            String text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":2,\"hitsArray\":["
                    + "{\"__prefix\":\"1\",\"keyword\":\"another keyword\","
                    + "\"property\":\"4\",\"attribute\":null},"
                    + "{\"__prefix\":\"1\",\"keyword\":\"some keyword\","
                    + "\"property\":\"1\",\"attribute\":null}]}"),
                text);
            File root2 = Files.createTempDirectory(
                "testUpdateByQyeryDeleteFieldPKJournal").toFile();
            try {
                copyJournal(root, root2, 1);
                try (Daemon daemon2 =
                        new Daemon(config(root2, "primary_key = keyword")))
                {
                    response = client.execute(new HttpGet(
                        "http://localhost:" + daemon2.searchServerPort()
                        + "/search?prefix=1&get=__prefix,keyword,property"
                        + ",attribute"
                        + "&sort=keyword&asc=1&text=keyword:*keyword"));
                    text = EntityUtils.toString(response.getEntity()).trim();
                    Assert.assertEquals("Expected 200 OK, but received: "
                        + response.getStatusLine() + " and body: " + text,
                        HttpStatus.SC_OK,
                        response.getStatusLine().getStatusCode());
                    YandexAssert.check(
                        new JsonChecker(
                            "{\"hitsCount\":2,\"hitsArray\":["
                            + "{\"__prefix\":\"1\",\"keyword\":\"another keyword\""
                            + ",\"property\":\"4\",\"attribute\":null},{"
                            + "\"__prefix\":\"1\",\"keyword\":\"some keyword\","
                            + "\"property\":\"1\",\"attribute\":null}]}"),
                        text);
                    response = client.execute(new HttpGet(
                        "http://localhost:" + daemon2.jsonServerPort()
                        + "/getQueueId?prefix=1"));
                    text = EntityUtils.toString(response.getEntity()).trim();
                    Assert.assertEquals("Expected 200 OK, but received: "
                        + response.getStatusLine() + " and body: " + text,
                        HttpStatus.SC_OK,
                        response.getStatusLine().getStatusCode());
                    Assert.assertEquals(checkQueueId(), text);
                }
            } finally {
                SearchBackendTestBase.removeDirectory(root2);
            }
        } finally {
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testUpdatePreserveFields() throws Exception {
        File root = Files.createTempDirectory("testUpdatePKPreserve").toFile();
        try (Daemon daemon = new Daemon(config(root, "primary_key = keyword"));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                + "\"keyword\":\"some keyword\",\"boolean\":\"some\","
                + "\"property\":\"1\"},{\"keyword\":\"another keyword\","
                + "\"boolean\":\"another\",\"attribute\":\"3\"}]}",
                UTF8));
            post.setHeader(YandexHeaders.ZOO_QUEUE_ID, genQueueId());
            HttpResponse response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()).trim(),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            EntityUtils.consume(response.getEntity());
            post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/update");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                + "\"property\":\"4\",\"keyword\":\"another keyword\"}],"
                + "\"PreserveFields\":[\"boolean\"]}",
                UTF8));
            post.setHeader(YandexHeaders.ZOO_QUEUE_ID, genQueueId());
            response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()).trim(),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            EntityUtils.consume(response.getEntity());
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort()
                + "/search?prefix=1&sort=keyword&asc=1&"
                + "text=keyword:*keyword&"
                + "get=__prefix,keyword,property,attribute,boolean"));
            String text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":2,\"hitsArray\":["
                    + "{\"__prefix\":\"1\",\"keyword\":\"another keyword\","
                    + "\"property\":\"4\",\"attribute\":null,"
                    + "\"boolean\":\"another\"},"
                    + "{\"__prefix\":\"1\",\"keyword\":\"some keyword\","
                    + "\"property\":\"1\",\"attribute\":null,"
                    + "\"boolean\":\"some\"}]}"),
                text);
        } finally {
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testReopen() throws Exception {
        File root = Files.createTempDirectory("testReopenPrimaryKey").toFile();
        try (Daemon daemon = new Daemon(config(root, "primary_key = keyword"));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                + "\"keyword\":\"some keyword\",\"text\":\"иди\"}]}",
                UTF8));
            post.setHeader(YandexHeaders.ZOO_QUEUE_ID, genQueueId());
            HttpResponse response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()).trim(),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            post.setHeader(YandexHeaders.ZOO_QUEUE_ID, genQueueId());
            response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()).trim(),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.jsonServerPort() + "/reopen"));
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()).trim(),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&"
                + "text=text:иду&get=keyword&json-type=dollar"));
            String text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{$hitsCount\0:1,$hitsArray\0:[{"
                    + "$keyword\0:$some keyword\0}]}"),
                text);
            post.setHeader(YandexHeaders.ZOO_QUEUE_ID, genQueueId());
            response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()).trim(),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&"
                + "text=text:иду&get=keyword&json-type=dollar"));
            text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{$hitsCount\0:1,$hitsArray\0:[{"
                    + "$keyword\0:$some keyword\0}]}"),
                text);


            response = client.execute(new HttpGet("http://localhost:"
                + daemon.jsonServerPort() + "/reopen?shards=1&ro=true"));
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()).trim(),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());

            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                + "\"keyword\":\"some keyword2\",\"text\":\"иди\"}]}",
                UTF8));
            post.setHeader(YandexHeaders.ZOO_QUEUE_ID, genQueueId());
            response = client.execute(post);
            Assert.assertEquals("Expected 403 Forbidden, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()).trim(),
                HttpStatus.SC_FORBIDDEN,
                response.getStatusLine().getStatusCode());

            response = client.execute(new HttpGet("http://localhost:"
                + daemon.jsonServerPort() + "/reopen?shards=1&ro=false"));
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()).trim(),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());

            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                + "\"keyword\":\"some keyword3\",\"text\":\"иди\"}]}",
                UTF8));
            post.setHeader(YandexHeaders.ZOO_QUEUE_ID, genQueueId());
            response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()).trim(),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());

            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&"
                + "text=text:иду&get=keyword&sort=keyword&asc"
                + "&json-type=dollar"));
            text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{$hitsCount\0:2,$hitsArray\0:[{"
                    + "$keyword\0:$some keyword\0},{$keyword\0:$some keyword3\0}]}"),
                text);
        } finally {
            SearchBackendTestBase.removeDirectory(root);
        }
    }
}

