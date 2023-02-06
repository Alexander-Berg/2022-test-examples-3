package ru.yandex.msearch;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.entity.GzipCompressingEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.lucene.util.ByteBlockPool;
import org.apache.lucene.store.BlockCompressedInputStreamBase;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.collection.Pattern;
import ru.yandex.devtools.test.Paths;
import ru.yandex.function.GenericAutoCloseableHolder;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.CountingHttpRequestHandler;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.test.ProxyHandler;
import ru.yandex.http.test.StaticServer;
import ru.yandex.http.util.YandexHeaders;
import ru.yandex.json.parser.JsonParser;
import ru.yandex.json.parser.StringCollectorFactory;
import ru.yandex.parser.config.IniConfig;
import ru.yandex.search.json.HandlersManager;
import ru.yandex.search.json.UpdateDocumentsMapCollector;
import ru.yandex.search.json.UpdateMessageRootHandler;
import ru.yandex.search.prefix.LongPrefix;
import ru.yandex.search.prefix.PrefixType;
import ru.yandex.test.search.backend.TestSearchBackend;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;
import ru.yandex.util.filesystem.DeletingFileVisitor;

public class JsonIndexTest extends TestBase {
    private static final AtomicLong queueId = new AtomicLong(0);

    public static String newQueueId() {
        return Long.toString(queueId.getAndIncrement());
    }

    public static String lastQueueId() {
        return Long.toString(queueId.get() - 1);
    }

    @Test
    public void testAdd() throws Exception {
        File root = Files.createTempDirectory("testAdd").toFile();
        try {
            try (Daemon daemon = new Daemon(SearchBackendTestBase.config(root));
                CloseableHttpClient client = HttpClients.createDefault())
            {
                HttpPost post = new HttpPost("http://localhost:"
                    + daemon.jsonServerPort() + "/add");
                post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                    + "\"keyword\":\"some keyword\",\"text\":\"ОченьДлинный"
                    + "ТокенЧтобыПроверитьКакПоведётСебяЛеммерСТакимиДлинными"
                    + "Словами\",\"boolean\":true}]}",
                    StandardCharsets.UTF_8));
                HttpResponse response = client.execute(post);
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: "
                    + EntityUtils.toString(response.getEntity()),
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                response = client.execute(new HttpGet("http://localhost:"
                    + daemon.searchServerPort() + "/search?prefix=1&text=text:"
                    + "ОченьДлинныйТокенЧтобыПроверитьКакПоведетСебяЛеммерС"
                    + "ТакимиДлиннымиСловами&get=keyword,boolean"));
                String text = EntityUtils.toString(response.getEntity()).trim();
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: " + text,
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                YandexAssert.check(
                    new JsonChecker(
                        "{\"hitsCount\":1,\"hitsArray\":[{\"keyword\":\""
                        + "some keyword\",\"boolean\":\"true\"}]}"),
                    text);
            }
        } finally {
            System.gc();
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testDeleteByQuery() throws Exception {
        System.err.println( "testDeleteByQuery testcase started" );
        File root = Files.createTempDirectory("testDeleteByQuery").toFile();
        try {
            try (Daemon daemon = new Daemon(SearchBackendTestBase.config(root));
                CloseableHttpClient client = HttpClients.createDefault())
            {
                HttpPost post = new HttpPost("http://localhost:"
                    + daemon.jsonServerPort() + "/add");
                post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                    + "\"keyword\":\"some keyword\",\"text\":\"иди\"},"
                    + "{\"keyword\":\"some keyword2\",\"text\":\"иду\"}]}",
                    StandardCharsets.UTF_16));
                HttpResponse response = client.execute(post);
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: "
                    + EntityUtils.toString(response.getEntity()),
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                InputStreamEntity entity = new InputStreamEntity(
                    new ByteArrayInputStream(
                        ("{\"prefix\":11,\"docs\":[{"
                        + "\"keyword\":\"some keyword3\",\"text\":\"иди\"}]}")
                            .getBytes(Charset.forName("utf-32"))),
                    ContentType.APPLICATION_JSON.withCharset(
                        Charset.forName("utf-32")));
                entity.setChunked(true);
                post.setEntity(entity);
                response = client.execute(post);
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: "
                    + EntityUtils.toString(response.getEntity()),
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                response = client.execute(new HttpGet("http://localhost:"
                    + daemon.searchServerPort() + "/search?prefix=1&"
                    + "text=text:иди&get=keyword&sort=keyword&prefix=11"));
                String text = EntityUtils.toString(response.getEntity()).trim();
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: " + text,
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                YandexAssert.check(
                    new JsonChecker(
                        "{\"hitsCount\":3,\"hitsArray\":[{"
                        + "\"keyword\":\"some keyword3\"},"
                        + "{\"keyword\":\"some keyword2\"},"
                        + "{\"keyword\":\"some keyword\"}]}"),
                    text);
                response = client.execute(new HttpGet("http://localhost:"
                    + daemon.jsonServerPort()
                    + "/delete?text=text:иди&prefix=1"));
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: "
                    + EntityUtils.toString(response.getEntity()),
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                response = client.execute(new HttpGet("http://localhost:"
                    + daemon.searchServerPort() + "/search?prefix=1&"
                    + "text=text:иди&get=keyword&sort=keyword&prefix=11"));
                text = EntityUtils.toString(response.getEntity()).trim();
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: " + text,
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                YandexAssert.check(
                    new JsonChecker(
                        "{\"hitsCount\":1,\"hitsArray\":[{"
                        + "\"keyword\":\"some keyword3\"}]}"),
                    text);
                File root2 = Files.createTempDirectory(
                    "testDeleteByQueryJournal").toFile();
                try {
                    File journal = new File(root2, "index/1/journal");
                    journal.mkdirs();
                    Path dst = journal.toPath();
                    for (File file
                        : new File(root, "index/1/journal").listFiles())
                    {
                        Path src = file.toPath();
                        Files.copy(src, dst.resolve(src.getFileName()));
                    }
                    try (Daemon daemon2 = new Daemon(SearchBackendTestBase.config(root2))) {
                        response = client.execute(new HttpGet(
                            "http://localhost:" + daemon2.searchServerPort()
                            + "/search?prefix=1&text=text:иди&get=keyword&"
                            + "sort=keyword&prefix=11"));
                        text = EntityUtils.toString(response.getEntity()).trim();
                        Assert.assertEquals("Expected 200 OK, but received: "
                            + response.getStatusLine() + " and body: " + text,
                            HttpStatus.SC_OK,
                            response.getStatusLine().getStatusCode());
                        YandexAssert.check(
                            new JsonChecker(
                                "{\"hitsCount\":1,\"hitsArray\":[{"
                                + "\"keyword\":\"some keyword3\"}]}"),
                            text);
                    }
                } finally {
                    SearchBackendTestBase.removeDirectory(root2);
                }
            }
        } finally {
            System.err.println( "testDeleteByQuery testcase stopped" );
            System.gc();
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testDeleteViaSearch() throws Exception {
        System.err.println( "testDeleteViaSearch testcase started" );
        File root = Files.createTempDirectory("testDeleteViaSearch").toFile();
        try {
            try (Daemon daemon = new Daemon(SearchBackendTestBase.config(root));
                CloseableHttpClient client = HttpClients.createDefault())
            {
                HttpPost post = new HttpPost("http://localhost:"
                    + daemon.jsonServerPort() + "/add");
                post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                    + "\"keyword\":\"some keyword\",\"text\":\"идут\"},"
                    + "{\"keyword\":\"new keyword\",\"text\":\"абырвалг\"}]}",
                    StandardCharsets.UTF_8));
                HttpResponse response = client.execute(post);
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: "
                    + EntityUtils.toString(response.getEntity()),
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                response = client.execute(new HttpGet("http://localhost:"
                    + daemon.searchPort() + "/?user=1&text=text:иду&"
                    + "op=delete&yes_i_want_this=ugu"));
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: "
                    + EntityUtils.toString(response.getEntity()).trim(),
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                response = client.execute(new HttpGet("http://localhost:"
                    + daemon.searchServerPort() + "/search?prefix=1&"
                    + "text=keyword:*keyword&get=keyword&get=attribute"));
                String text = EntityUtils.toString(response.getEntity()).trim();
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: " + text,
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                YandexAssert.check(
                    new JsonChecker(
                        "{\"hitsCount\":1,\"hitsArray\":[{"
                        + "\"keyword\":\"new keyword\",\"attribute\":null}]}"),
                    text);
            }
        } finally {
            System.err.println( "testDeleteViaSearch testcase stopped" );
            System.gc();
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testUpdateByQuery() throws Exception {
        System.err.println( "testUpdateByQuery testcase started" );
        File root = Files.createTempDirectory("testUpdateByQuery").toFile();
        try {
            try (Daemon daemon =
                    new Daemon(SearchBackendTestBase.config(root, "prefix_type = string\n"));
                CloseableHttpClient client = HttpClients.createDefault())
            {
                HttpPost post = new HttpPost("http://localhost:"
                    + daemon.jsonServerPort() + "/add");
                HttpEntity entity = new GzipCompressingEntity(
                    new InputStreamEntity(new ByteArrayInputStream(
                        ("{\"prefix\":\"hello, world\",\"docs\":[{"
                        + "\"keyword\":\"some keyword\",\"attribute\":\"1\","
                        + "\"property\":\"2\"},{\"keyword\":\"some keyword\","
                        + "\"property\":\"5\"},{\"keyword\":\"some keyword\","
                        + "\"property\":\"6\",\"attribute\":null},"
                        + "{\"keyword\":\"some keyword2\",\"attribute\":\"3\","
                        + "\"property\":3},"
                        + "{\"keyword\":\"some keyword3\",\"attribute\":\"4\","
                        + "\"property\":1}]}")
                            .getBytes(StandardCharsets.UTF_8))));
                post.setEntity(entity);
                HttpResponse response = client.execute(post);
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: "
                    + EntityUtils.toString(response.getEntity()),
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());

                post = new HttpPost("http://localhost:"
                    + daemon.jsonServerPort() + "/update");
                post.setEntity(new StringEntity("{$prefix\0:\"hello, world\","
                    + "$docs\0:[{\"attribute\":$4\0}],"
                    + "\"query\":\"keyword:some\\\\ keyword\"}",
                    StandardCharsets.UTF_8));
                response = client.execute(post);
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: "
                    + EntityUtils.toString(response.getEntity()),
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());

                post = new HttpPost("http://localhost:"
                    + daemon.jsonServerPort() + "/update");
                post.setEntity(new StringEntity("{$prefix\0:\"hello, world\","
                    + "$docs\0:[{"
                    + "\"property\":0,"
                    + "\"attribute\":{\"function\":\"inc\","
                    + "\"args\":["
                        + "1,"
                        + "{\"function\":\"lt\",\"args\":["
                            + "{\"function\":\"get\",\"args\":[\"property\"]},"
                            + "1"
                        + "]}"
                    + "]}}],"
                    + "\"query\":\"keyword:some\\\\ keyword3\"}",
                    StandardCharsets.UTF_8));
                response = client.execute(post);
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: "
                    + EntityUtils.toString(response.getEntity()),
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());

                response = client.execute(new HttpGet("http://localhost:"
                    + daemon.searchServerPort()
                    + "/search?prefix=hello,+world&text=keyword:*&"
                    + "sort=property&"
                    + "get=keyword,attribute,property&json-type=dollar"));
                String text = EntityUtils.toString(response.getEntity()).trim();
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: " + text,
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                YandexAssert.check(
                    new JsonChecker(
                        "{$hitsCount\0:5,$hitsArray\0:[{"
                        + "$keyword\0:$some keyword\0,$attribute\0:$4\0,"
                        + "$property\0:$6\0},{$keyword\0:$some keyword\0,"
                        + "$attribute\0:$4\0,$property\0:$5\0},"
                        + "{$keyword\0:$some keyword2\0,$attribute\0:$3\0,"
                        + "$property\0:$3\0},{$keyword\0:$some keyword\0,"
                        + "$attribute\0:$4\0,$property\0:$2\0},"
                        + "{$keyword\0:$some keyword3\0,$attribute\0:$5\0,"
                        + "$property\0:$0\0}]}"),
                    text);

                post = new HttpPost("http://localhost:"
                    + daemon.jsonServerPort() + "/update");
                post.setEntity(new StringEntity("{$prefix\0:\"hello, world\","
                    + "$docs\0:[{\"attribute\":{\"function\":\"inc\","
                    + "\"args\":["
                        + "1,"
                        + "{\"function\":\"lt\",\"args\":["
                            + "{\"function\":\"get\",\"args\":[\"property\"]},"
                            + "1"
                        + "]}"
                    + "]},\"property\":0}],"
                    + "\"query\":\"keyword:some\\\\ keyword3\"}",
                    StandardCharsets.UTF_8));
                response = client.execute(post);
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: "
                    + EntityUtils.toString(response.getEntity()),
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());

                response = client.execute(new HttpGet("http://localhost:"
                    + daemon.searchServerPort()
                    + "/search?prefix=hello,+world&text=keyword:*&"
                    + "sort=property&"
                    + "get=keyword,attribute,property&json-type=dollar"));
                text = EntityUtils.toString(response.getEntity()).trim();
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: " + text,
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                YandexAssert.check(
                    new JsonChecker(
                        "{$hitsCount\0:5,$hitsArray\0:[{"
                        + "$keyword\0:$some keyword\0,$attribute\0:$4\0,"
                        + "$property\0:$6\0},{$keyword\0:$some keyword\0,"
                        + "$attribute\0:$4\0,$property\0:$5\0},"
                        + "{$keyword\0:$some keyword2\0,$attribute\0:$3\0,"
                        + "$property\0:$3\0},{$keyword\0:$some keyword\0,"
                        + "$attribute\0:$4\0,$property\0:$2\0},"
                        + "{$keyword\0:$some keyword3\0,$attribute\0:$6\0,"
                        + "$property\0:$0\0}]}"),
                    text);

                post = new HttpPost("http://localhost:"
                    + daemon.jsonServerPort() + "/update");
                post.setEntity(new StringEntity("{$prefix\0:\"hello, world\","
                    + "$docs\0:[{\"attribute\":"
                        + "{\"inc\":["
                            + "{\"lt\":[{\"get\":[\"property\"]},0]}"
                    + "]},\"property\":0}],"
                    + "\"query\":\"keyword:some\\\\ keyword3\"}",
                    StandardCharsets.UTF_8));
                response = client.execute(post);
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: "
                    + EntityUtils.toString(response.getEntity()),
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());

                response = client.execute(new HttpGet("http://localhost:"
                    + daemon.searchServerPort()
                    + "/search?prefix=hello,+world&text=keyword:*&"
                    + "sort=property&"
                    + "get=keyword,attribute,property&json-type=dollar"));
                text = EntityUtils.toString(response.getEntity()).trim();
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: " + text,
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                YandexAssert.check(
                    new JsonChecker(
                        "{$hitsCount\0:5,$hitsArray\0:[{"
                        + "$keyword\0:$some keyword\0,$attribute\0:$4\0,"
                        + "$property\0:$6\0},{$keyword\0:$some keyword\0,"
                        + "$attribute\0:$4\0,$property\0:$5\0},"
                        + "{$keyword\0:$some keyword2\0,$attribute\0:$3\0,"
                        + "$property\0:$3\0},{$keyword\0:$some keyword\0,"
                        + "$attribute\0:$4\0,$property\0:$2\0},"
                        + "{$keyword\0:$some keyword3\0,$attribute\0:$6\0,"
                        + "$property\0:$0\0}]}"),
                    text);

                post = new HttpPost("http://localhost:"
                    + daemon.jsonServerPort() + "/update");
                post.setEntity(new StringEntity("{$prefix\0:\"hello, world\","
                    + "$docs\0:[{\"attribute\":{\"function\":\"inc\","
                    + "\"args\":["
                        + "{\"function\":\"lt\",\"args\":["
                            + "{\"function\":\"get\",\"args\":[\"property\"]},"
                            + "1"
                        + "]}"
                    + "]},\"property\":0}],"
                    + "\"query\":\"keyword:some\\\\ keyword3\"}",
                    StandardCharsets.UTF_8));
                response = client.execute(post);
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: "
                    + EntityUtils.toString(response.getEntity()),
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());

                response = client.execute(new HttpGet("http://localhost:"
                    + daemon.searchServerPort()
                    + "/search?prefix=hello,+world&text=keyword:*&"
                    + "sort=property&"
                    + "get=keyword,attribute,property&json-type=dollar"));
                text = EntityUtils.toString(response.getEntity()).trim();
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: " + text,
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                YandexAssert.check(
                    new JsonChecker(
                        "{$hitsCount\0:5,$hitsArray\0:[{"
                        + "$keyword\0:$some keyword\0,$attribute\0:$4\0,"
                        + "$property\0:$6\0},{$keyword\0:$some keyword\0,"
                        + "$attribute\0:$4\0,$property\0:$5\0},"
                        + "{$keyword\0:$some keyword2\0,$attribute\0:$3\0,"
                        + "$property\0:$3\0},{$keyword\0:$some keyword\0,"
                        + "$attribute\0:$4\0,$property\0:$2\0},"
                        + "{$keyword\0:$some keyword3\0,$attribute\0:$7\0,"
                        + "$property\0:$0\0}]}"),
                    text);
                File root2 = Files.createTempDirectory(
                    "testUpdateByQueryJournal").toFile();
                try {
                    for (int i = 0; i < SearchBackendTestBase.SHARDS; ++i) {
                        File journal =
                            new File(root2, "index/" + i + "/journal");
                        journal.mkdirs();
                        Path dst = journal.toPath();
                        for (File file: new File(root,
                                "index/" + i + "/journal").listFiles())
                        {
                            Path src = file.toPath();
                            Files.copy(src, dst.resolve(src.getFileName()));
                        }
                    }
                    try (Daemon daemon2 = new Daemon(
                            SearchBackendTestBase.config(root2, "prefix_type = string\n")))
                    {
                        response = client.execute(new HttpGet(
                            "http://localhost:" + daemon2.searchServerPort()
                            + "/search?prefix=hello,%20world&text=keyword:*&"
                            + "sort=property&get=keyword,attribute,property"
                            + "&json-type=dollar"));
                        text = EntityUtils.toString(response.getEntity()).trim();
                        Assert.assertEquals("Expected 200 OK, but received: "
                            + response.getStatusLine() + " and body: " + text,
                            HttpStatus.SC_OK,
                            response.getStatusLine().getStatusCode());
                        YandexAssert.check(
                            new JsonChecker(
                                "{$hitsCount\0:5,$hitsArray\0:[{"
                                + "$keyword\0:$some keyword\0,$attribute\0:$4\0,"
                                + "$property\0:$6\0},{$keyword\0:$some keyword\0,"
                                + "$attribute\0:$4\0,$property\0:$5\0},"
                                + "{$keyword\0:$some keyword2\0,$attribute\0:$3\0,"
                                + "$property\0:$3\0},{$keyword\0:$some keyword\0,"
                                + "$attribute\0:$4\0,$property\0:$2\0},"
                                + "{$keyword\0:$some keyword3\0,$attribute\0:$7\0,"
                                + "$property\0:$0\0}]}"),
                            text);
                    }
                } finally {
                    SearchBackendTestBase.removeDirectory(root2);
                }
            }
        } finally {
            System.err.println( "testUpdateByQuery testcase stopped" );
            System.gc();
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testUpdateIfNotMatches() throws Exception {
        System.err.println( "testUpdateByQuery testcase started" );
        File root = Files.createTempDirectory("testUpdateByQuery").toFile();
        try {
            try (Daemon daemon = new Daemon(SearchBackendTestBase.config(
                    root,
                    "prefix_type = string\nprimary_key = keyword\n"));
                CloseableHttpClient client = HttpClients.createDefault())
            {
                HttpPost post = new HttpPost("http://localhost:"
                    + daemon.jsonServerPort() + "/add");
                InputStreamEntity entity = new InputStreamEntity(
                    new ByteArrayInputStream(
                        ("{\"prefix\":\"hello, world\",\"docs\":[{"
                        + "\"keyword\":\"some keyword\",\"attribute\":\"1\","
                        + "\"property\":\"2\"},{\"keyword\":\"some keyword\","
                        + "\"property\":\"5\"},{\"keyword\":\"some keyword\","
                        + "\"property\":\"6\",\"attribute\":null},"
                        + "{\"keyword\":\"some keyword2\",\"attribute\":\"3\","
                        + "\"property\":3}]}")
                            .getBytes(StandardCharsets.UTF_8)));
                post.setEntity(entity);
                HttpResponse response = client.execute(post);
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: "
                    + EntityUtils.toString(response.getEntity()),
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                //Matches - no update
                post = new HttpPost("http://localhost:"
                    + daemon.jsonServerPort() + "/update");
                post.setEntity(new StringEntity("{$prefix\0:\"hello, world\","
                    + "$docs\0:[{\"keyword\":\"counter\",\"attribute\":$4\0}],"
                    + "\"query\":\"keyword:some\\\\ keyword\","
                    + "\"UpdateIfNotMatches\":true}",
                    StandardCharsets.UTF_8));
                response = client.execute(post);
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: "
                    + EntityUtils.toString(response.getEntity()),
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                response = client.execute(new HttpGet("http://localhost:"
                    + daemon.searchServerPort()
                    + "/search?prefix=hello,+world&text=keyword:counter&"
                    + "sort=keyword&"
                    + "get=keyword,attribute&json-type=dollar"));
                String text = EntityUtils.toString(response.getEntity()).trim();
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: " + text,
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                YandexAssert.check(
                    new JsonChecker(
                        "{$hitsCount\0:0,$hitsArray\0:[]}"),
                    text);

                //Not matches - no update because of updatable document is not exists
                post = new HttpPost("http://localhost:"
                    + daemon.jsonServerPort() + "/update");
                post.setEntity(new StringEntity("{$prefix\0:\"hello, world\","
                    + "$docs\0:[{\"keyword\":\"counter\",\"attribute\":$4\0}],"
                    + "\"query\":\"keyword:luck\\\\ keyword\","
                    + "\"UpdateIfNotMatches\":true}",
                    StandardCharsets.UTF_8));
                response = client.execute(post);
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: "
                    + EntityUtils.toString(response.getEntity()),
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                response = client.execute(new HttpGet("http://localhost:"
                    + daemon.searchServerPort()
                    + "/search?prefix=hello,+world&text=keyword:counter&"
                    + "sort=keyword&"
                    + "get=keyword,attribute&hr"));
                text = EntityUtils.toString(response.getEntity()).trim();
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: " + text,
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                YandexAssert.check(
                    new JsonChecker(
                        "{\n    \"hitsCount\": 0,\n"
                        + "    \"hitsArray\": [\n\n    ]\n}"),
                    text);

                //Not matches - should update and add
                post = new HttpPost("http://localhost:"
                    + daemon.jsonServerPort() + "/update");
                post.setEntity(new StringEntity("{$prefix\0:\"hello, world\","
                    + "$docs\0:[{\"keyword\":\"counter\",\"attribute\":{\"function\":\"inc\"}}],"
                    + "\"query\":\"keyword:luck\\\\ keyword\","
                    + "\"UpdateIfNotMatches\":true, \"AddIfNotExists\":true}",
                    StandardCharsets.UTF_8));
                response = client.execute(post);
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: "
                    + EntityUtils.toString(response.getEntity()),
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                response = client.execute(new HttpGet("http://localhost:"
                    + daemon.searchServerPort()
                    + "/search?prefix=hello,+world&text=keyword:counter&"
                    + "sort=keyword&"
                    + "get=keyword,attribute&json-type=dollar"));
                text = EntityUtils.toString(response.getEntity()).trim();
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: " + text,
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                YandexAssert.check(
                    new JsonChecker(
                        "{$hitsCount\0:1,$hitsArray\0:[{$keyword\0:$counter\0,"
                        + "$attribute\0:$1\0}]}"),
                    text);

                //Not matches - should update
                post = new HttpPost("http://localhost:"
                    + daemon.jsonServerPort() + "/update");
                post.setEntity(new StringEntity("{$prefix\0:\"hello, world\","
                    + "$docs\0:[{\"keyword\":\"counter\",\"attribute\":{\"function\":\"inc\",\"args\":[2]}}],"
                    + "\"query\":\"keyword:luck\\\\ keyword\","
                    + "\"UpdateIfNotMatches\":true, \"AddIfNotExists\":false}",
                    StandardCharsets.UTF_8));
                response = client.execute(post);
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: "
                    + EntityUtils.toString(response.getEntity()),
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                response = client.execute(new HttpGet("http://localhost:"
                    + daemon.searchServerPort()
                    + "/search?prefix=hello,+world&text=keyword:counter&"
                    + "sort=keyword&"
                    + "get=keyword,attribute&json-type=dollar"));
                text = EntityUtils.toString(response.getEntity()).trim();
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: " + text,
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                YandexAssert.check(
                    new JsonChecker(
                        "{$hitsCount\0:1,$hitsArray\0:[{$keyword\0:$counter\0,"
                        + "$attribute\0:$3\0}]}"),
                    text);

                File root2 = Files.createTempDirectory(
                    "testUpdateByQueryJournal").toFile();
                try {
                    for (int i = 0; i < SearchBackendTestBase.SHARDS; ++i) {
                        File journal =
                            new File(root2, "index/" + i + "/journal");
                        journal.mkdirs();
                        Path dst = journal.toPath();
                        for (File file: new File(root,
                                "index/" + i + "/journal").listFiles())
                        {
                            Path src = file.toPath();
                            Files.copy(src, dst.resolve(src.getFileName()));
                        }
                    }
                    try (Daemon daemon2 = new Daemon(
                            SearchBackendTestBase.config(root2, "prefix_type = string\n")))
                    {
                        response = client.execute(new HttpGet("http://localhost:"
                            + daemon.searchServerPort()
                            + "/search?prefix=hello,+world&text=keyword:counter&"
                            + "sort=keyword&"
                            + "get=keyword,attribute&json-type=dollar"));
                        text = EntityUtils.toString(response.getEntity()).trim();
                        Assert.assertEquals("Expected 200 OK, but received: "
                            + response.getStatusLine() + " and body: " + text,
                            HttpStatus.SC_OK,
                            response.getStatusLine().getStatusCode());
                        YandexAssert.check(
                            new JsonChecker(
                                "{$hitsCount\0:1,$hitsArray\0:[{$keyword\0:"
                                + "$counter\0,$attribute\0:$3\0}]}"),
                            text);
                    }
                } finally {
                    SearchBackendTestBase.removeDirectory(root2);
                }
            }
        } finally {
            System.err.println( "testUpdateByQuery testcase stopped" );
            System.gc();
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testUpdateByQueryDeleteField() throws Exception {
        System.err.println( "testUpdateByQueryDeleteField testcase started" );
        File root =
            Files.createTempDirectory("testUpdateByQueryDeleteField").toFile();
        try {
            try (Daemon daemon = new Daemon(SearchBackendTestBase.config(root));
                CloseableHttpClient client = HttpClients.createDefault())
            {
                HttpPost post = new HttpPost("http://localhost:"
                    + daemon.jsonServerPort() + "/add");
                post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                    + "\"keyword\":\"some keyword\",\"attribute\":\"1\","
                    + "\"property\":\"2\"},{\"keyword\":\"some keyword\","
                    + "\"property\":\"5\"},{\"keyword\":\"some keyword\","
                    + "\"property\":\"6\",\"attribute\":null},"
                    + "{\"keyword\":\"some keyword2\",\"attribute\":\"3\","
                    + "\"property\":3}]}",
                    StandardCharsets.UTF_8));
                HttpResponse response = client.execute(post);
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: "
                    + EntityUtils.toString(response.getEntity()),
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                post = new HttpPost("http://localhost:"
                    + daemon.jsonServerPort() + "/update");
                post.setEntity(new StringEntity("{$prefix\0:1,$docs\0:[{"
                    + "\"attribute\":$4\0,$property\0:null}],"
                    + "\"query\":\"keyword:some\\\\ keyword\"}",
                    StandardCharsets.UTF_8));
                response = client.execute(post);
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: "
                    + EntityUtils.toString(response.getEntity()),
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                response = client.execute(new HttpGet("http://localhost:"
                    + daemon.searchServerPort()
                    + "/search?prefix=1&text=keyword:*&sort=attribute&"
                    + "get=keyword,attribute,property&json-type=dollar"));
                String text = EntityUtils.toString(response.getEntity()).trim();
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: " + text,
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                YandexAssert.check(
                    new JsonChecker(
                        "{$hitsCount\0:4,$hitsArray\0:[{"
                        + "$keyword\0:$some keyword\0,$attribute\0:$4\0,"
                        + "$property\0:null},{"
                        + "$keyword\0:$some keyword\0,$attribute\0:$4\0,"
                        + "$property\0:null},{"
                        + "$keyword\0:$some keyword\0,$attribute\0:$4\0,"
                        + "$property\0:null},{$keyword\0:$some keyword2\0,"
                        + "$attribute\0:$3\0,$property\0:$3\0}]}"),
                    text);
                File root2 = Files.createTempDirectory(
                    "testUpdateByQueryDeleteFieldJournal").toFile();
                try {
                    File journal = new File(root2, "index/1/journal");
                    journal.mkdirs();
                    Path dst = journal.toPath();
                    for (File file
                        : new File(root, "index/1/journal").listFiles())
                    {
                        Path src = file.toPath();
                        Files.copy(src, dst.resolve(src.getFileName()));
                    }
                    try (Daemon daemon2 = new Daemon(SearchBackendTestBase.config(root2))) {
                        response = client.execute(new HttpGet(
                            "http://localhost:" + daemon2.searchServerPort()
                            + "/search?prefix=1&text=keyword:*&sort=attribute&"
                            + "get=keyword,attribute,property"
                            + "&json-type=dollar"));
                        text = EntityUtils.toString(response.getEntity()).trim();
                        Assert.assertEquals("Expected 200 OK, but received: "
                            + response.getStatusLine() + " and body: " + text,
                            HttpStatus.SC_OK,
                            response.getStatusLine().getStatusCode());
                        YandexAssert.check(
                            new JsonChecker(
                                "{$hitsCount\0:4,$hitsArray\0:[{"
                                + "$keyword\0:$some keyword\0,$attribute\0:$4\0,"
                                + "$property\0:null},{"
                                + "$keyword\0:$some keyword\0,$attribute\0:$4\0,"
                                + "$property\0:null},{"
                                + "$keyword\0:$some keyword\0,$attribute\0:$4\0,"
                                + "$property\0:null},{$keyword\0:$some keyword2\0,"
                                + "$attribute\0:$3\0,$property\0:$3\0}]}"),
                            text);
                    }
                } finally {
                    SearchBackendTestBase.removeDirectory(root2);
                }
            }
        } finally {
            System.err.println( "testUpdateByQueryDeleteField testcase stopped" );
            System.gc();
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testUpdateByQueryBadQuery() throws Exception {
        System.err.println( "testUpdateByQueryBadQuery testcase started" );
        File root =
            Files.createTempDirectory("testUpdateByQueryBadQuery").toFile();
        try {
            try (Daemon daemon = new Daemon(SearchBackendTestBase.config(root));
                CloseableHttpClient client = HttpClients.createDefault())
            {
                HttpPost post = new HttpPost("http://localhost:"
                    + daemon.jsonServerPort() + "/update");
                post.setEntity(new StringEntity("{$prefix\0:1,$docs\0:[{"
                    + "\"attribute\":$4\0,$property\0:null}],"
                    + "\"query\":\"keyword:some *\"}",
                    StandardCharsets.UTF_8));
                HttpResponse response = client.execute(post);
                Assert.assertEquals("Expected 400 Bad Request, but received: "
                    + response.getStatusLine() + " and body: "
                    + EntityUtils.toString(response.getEntity()),
                    HttpStatus.SC_BAD_REQUEST,
                    response.getStatusLine().getStatusCode());
                post.setEntity(new StringEntity("{$prefix\0:1,$docs\0:[{"
                    + "\"attribute\":$4\0,$property\0:null}],"
                    + "\"query\":null}",
                    StandardCharsets.UTF_8));
                response = client.execute(post);
                Assert.assertEquals("Expected 400 Bad Request, but received: "
                    + response.getStatusLine() + " and body: "
                    + EntityUtils.toString(response.getEntity()),
                    HttpStatus.SC_BAD_REQUEST,
                    response.getStatusLine().getStatusCode());
            }
        } finally {
            System.err.println( "testUpdateByQueryBadQuery testcase stopped" );
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testUpdateBadFunction() throws Exception {
        System.err.println( "testUpdateBadFunction testcase started" );
        File root =
            Files.createTempDirectory("testUpdateBadFunction").toFile();
        try {
            try (Daemon daemon = new Daemon(SearchBackendTestBase.config(root));
                CloseableHttpClient client = HttpClients.createDefault())
            {
                HttpPost post = new HttpPost("http://localhost:"
                    + daemon.jsonServerPort() + "/update");
                post.setEntity(new StringEntity("{$prefix\0:1,$docs\0:[{"
                    + "\"attribute\":{$inc\0:[],$dec\0:[]},$property\0:null}]}",
                    StandardCharsets.UTF_8));
                HttpResponse response = client.execute(post);
                Assert.assertEquals("Expected 400 Bad Request, but received: "
                    + response.getStatusLine() + " and body: "
                    + EntityUtils.toString(response.getEntity()),
                    HttpStatus.SC_BAD_REQUEST,
                    response.getStatusLine().getStatusCode());
            }
        } finally {
            System.err.println( "testUpdateBadFunction testcase stopped" );
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testUpdatePreserveFields() throws Exception {
        System.err.println("testUpdatePreserveFields testcase started");
        File root = Files.createTempDirectory("testUpdatePreserveFields").toFile();
        String suffix =
            "prefix_type = string\n"
            + "primary_key = keyword\n"
            + "field.counter1.store = true\n"
            + "field.counter2.store = true\n";

        try {
            try (Daemon daemon = new Daemon(
                SearchBackendTestBase.config(root, suffix));
                 CloseableHttpClient client = HttpClients.createDefault())
            {
                HttpPost post = new HttpPost(
                    "http://localhost:" + daemon.jsonServerPort() + "/add");

                StringBuilder textBuilder =
                    new StringBuilder("{\"prefix\":\"prsrv\",\"docs\":[{");
                textBuilder.append("\"keyword\":\"doc1\","
                                       + "\"attribute\":\"1\","
                                       + "\"property\":\"2\","
                                       + "\"counter1\":0,"
                                       + "\"counter2\":4}");
                textBuilder.append(",");
                textBuilder.append("{\"keyword\":\"doc2\",\"property\":\"5\"}");
                textBuilder.append("]}");
                HttpEntity entity = new GzipCompressingEntity(
                    new InputStreamEntity(
                        new ByteArrayInputStream(
                            textBuilder.toString().getBytes(StandardCharsets.UTF_8))));

                post.setEntity(entity);
                HttpResponse response = client.execute(post);
                Assert.assertEquals(
                    "Expected 200 OK, but received: "
                        + response.getStatusLine() + " and "
                        + "body: " +
                        EntityUtils.toString(
                            response.getEntity()),
                            HttpStatus.SC_OK,
                            response.getStatusLine().getStatusCode());

                post = new HttpPost(
                    "http://localhost:" + daemon.jsonServerPort() + "/update");

                post.setEntity(
                    new StringEntity(
                        "{\"prefix\":\"prsrv\","
                            + "\"docs\":[{\"attribute\":4, \"keyword\":\"doc1\"}],"
                            + "\"PreserveFields\":[\"counter1\", \"counter2\"]}",
                        StandardCharsets.UTF_8));

                response = client.execute(post);
                Assert.assertEquals(
                    "Expected 200 OK, but received: "
                        + response.getStatusLine() + " and body: "
                        + EntityUtils.toString(response.getEntity()),
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());

                response = client.execute(
                    new HttpGet(
                        "http://localhost:" + daemon.searchServerPort()
                            + "/search?prefix=prsrv&text=keyword:doc1&"
                            + "get=*"));
                String text = EntityUtils.toString(response.getEntity()).trim();
                Assert.assertEquals(
                    "Expected 200 OK, but received: "
                        + response.getStatusLine() + " and body: " + text,
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());

                YandexAssert.check(
                    new JsonChecker(
                        "{\"hitsCount\":1,\"hitsArray\":[{"
                        + "\"keyword\":\"doc1\",\"attribute\":\"4\","
                        + "\"counter1\":\"0\",\"counter2\":\"4\"}]}"),
                    text);

                post.setEntity(
                    new StringEntity(
                        "{\"prefix\":\"prsrv\","
                            + "\"docs\":[{\"counter1\":20, \"keyword\":\"doc3\"}],"
                            + "\"PreserveFields\":[\"counter1\", \"counter2\"],"
                            + "\"AddIfNotExists\":true}",
                        StandardCharsets.UTF_8));

                response = client.execute(post);
                Assert.assertEquals(
                    "Expected 200 OK, but received: "
                        + response.getStatusLine() + " and body: "
                        + EntityUtils.toString(response.getEntity()),
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());

                response = client.execute(
                    new HttpGet(
                        "http://localhost:" + daemon.searchServerPort()
                            + "/search?prefix=prsrv&text=keyword:doc3&"
                            + "get=*"));
                text = EntityUtils.toString(response.getEntity()).trim();
                YandexAssert.check(
                    new JsonChecker(
                        "{\"hitsCount\":1,\"hitsArray\":[{"
                        + "\"keyword\":\"doc3\",\"counter1\":\"20\"}]}"),
                    text);

                post.setEntity(
                    new StringEntity(
                        "{\"prefix\":\"prsrv\","
                            + "\"docs\":[{\"keyword\":\"doc3\", \"counter1\":{\"function\":\"inc\"}}],"
                            + "\"PreserveFields\":[\"counter1\", \"counter2\"],"
                            + "\"AddIfNotExists\":true}",
                        StandardCharsets.UTF_8));

                response = client.execute(post);
                Assert.assertEquals(
                    "Expected 200 OK, but received: "
                        + response.getStatusLine() + " and body: "
                        + EntityUtils.toString(response.getEntity()),
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());

                response = client.execute(
                    new HttpGet(
                        "http://localhost:" + daemon.searchServerPort()
                            + "/search?prefix=prsrv&text=keyword:doc3&"
                            + "get=*"));
                text = EntityUtils.toString(response.getEntity()).trim();
                YandexAssert.check(
                    new JsonChecker(
                        "{\"hitsCount\":1,\"hitsArray\":[{"
                        + "\"keyword\":\"doc3\",\"counter1\":\"21\"}]}"),
                    text);

                post.setEntity(
                    new StringEntity(
                        "{\"prefix\":\"prsrv\","
                            + "\"docs\":[{\"keyword\":\"doc3\", \"attribute\":\"28\"}],"
                            + "\"PreserveFields\":[],"
                            + "\"AddIfNotExists\":true}",
                        StandardCharsets.UTF_8));

                response = client.execute(post);
                Assert.assertEquals(
                    "Expected 200 OK, but received: "
                        + response.getStatusLine() + " and body: "
                        + EntityUtils.toString(response.getEntity()),
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());

                response = client.execute(
                    new HttpGet(
                        "http://localhost:" + daemon.searchServerPort()
                            + "/search?prefix=prsrv&text=keyword:doc3&"
                            + "get=*"));
                text = EntityUtils.toString(response.getEntity()).trim();
                YandexAssert.check(
                    new JsonChecker(
                        "{\"hitsCount\":1,\"hitsArray\":[{"
                        + "\"keyword\":\"doc3\",\"attribute\":\"28\"}]}"),
                    text);
            }
        } finally {
            System.err.println("testUpdatePreserveFields testcase stopped");
            System.gc();
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testReopen() throws Exception {
        System.err.println( "testReopen testcase started" );
        File root = Files.createTempDirectory("testReopen").toFile();
        try {
            try (Daemon daemon = new Daemon(SearchBackendTestBase.config(root));
                CloseableHttpClient client = HttpClients.createDefault())
            {
                HttpPost post = new HttpPost("http://localhost:"
                    + daemon.jsonServerPort() + "/add");
                post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                    + "\"keyword\":\"some keyword\",\"text\":"
                    + "\"я пошёл гулять с другом\"}]}",
                    StandardCharsets.UTF_8));
                HttpResponse response = client.execute(post);
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: "
                    + EntityUtils.toString(response.getEntity()),
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                response = client.execute(new HttpGet("http://localhost:"
                    + daemon.jsonServerPort() + "/reopen"));
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: "
                    + EntityUtils.toString(response.getEntity()),
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                response = client.execute(new HttpGet("http://localhost:"
                    + daemon.searchServerPort() + "/search?prefix=1&"
                    + "text=text:друзья&get=keyword"));
                String text = EntityUtils.toString(response.getEntity()).trim();
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: " + text,
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                YandexAssert.check(
                    new JsonChecker(
                        "{\"hitsCount\":1,\"hitsArray\":[{"
                        + "\"keyword\":\"some keyword\"}]}"),
                    text);
                response = client.execute(post);
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: "
                    + EntityUtils.toString(response.getEntity()),
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                response = client.execute(new HttpGet("http://localhost:"
                    + daemon.searchServerPort() + "/search?prefix=1&"
                    + "text=text:другие&get=keyword"));
                text = EntityUtils.toString(response.getEntity()).trim();
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: " + text,
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                YandexAssert.check(
                    new JsonChecker(
                        "{\"hitsCount\":2,\"hitsArray\":[{"
                        + "\"keyword\":\"some keyword\"},{"
                        + "\"keyword\":\"some keyword\"}]}"),
                    text);
            }
        } finally {
            System.err.println( "testReopen testcase stopped" );
            System.gc();
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testFlush() throws Exception {
        System.err.println( "testFlush testcase started" );
        File root = Files.createTempDirectory("testFlush").toFile();
        try {
            try (Daemon daemon = new Daemon(SearchBackendTestBase.config(root, "maxmemdocs = 1"));
                CloseableHttpClient client = HttpClients.createDefault())
            {
                HttpPost post = new HttpPost("http://localhost:"
                    + daemon.jsonServerPort() + "/add");
                post.setEntity(new StringEntity(
                    "{\"prefix\":1,\"docs\":[{\"keyword\":\"some keyword\","
                    + "\"text\":\"больше текста\"}]}",
                    StandardCharsets.UTF_8));
                StringBuilder result = new StringBuilder();
                HttpResponse response;
                for (int i = 0; i < 20; ++i) {
                    response = client.execute(post);
                    Assert.assertEquals("Expected 200 OK, but received: "
                        + response.getStatusLine() + " and body: "
                        + EntityUtils.toString(response.getEntity()),
                        HttpStatus.SC_OK,
                        response.getStatusLine().getStatusCode());
                    if (i > 0) {
                        result.append(',');
                    }
                    result.append("{\"keyword\":\"some keyword\"}");
                }
                Thread.sleep(500);
                response = client.execute(new HttpGet("http://localhost:"
                    + daemon.searchServerPort()
                    + "/search?prefix=1&text=text:тексты&get=keyword"));
                String text = EntityUtils.toString(response.getEntity()).trim();
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: " + text,
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                YandexAssert.check(
                    new JsonChecker(
                        "{\"hitsCount\":20,\"hitsArray\":["
                        + result + "]}"),
                    text);
                for (int i = 0; i < 20; ++i) {
                    response = client.execute(post);
                    Assert.assertEquals("Expected 200 OK, but received: "
                        + response.getStatusLine() + " and body: "
                        + EntityUtils.toString(response.getEntity()),
                        HttpStatus.SC_OK,
                        response.getStatusLine().getStatusCode());
                }
                Thread.sleep(500);
                response = client.execute(new HttpGet("http://localhost:"
                    + daemon.searchServerPort()
                    + "/search?prefix=1&text=text:текст&get=keyword"));
                text = EntityUtils.toString(response.getEntity()).trim();
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: " + text,
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                YandexAssert.check(
                    new JsonChecker(
                        "{\"hitsCount\":40,\"hitsArray\":["
                        + result + ',' + result + "]}"),
                    text);
            }
        } finally {
            System.err.println( "testFlush testcase stopped" );
            System.gc();
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testNumDocs() throws Exception {
        System.err.println( "testNumDocs testcase started" );
        File root = Files.createTempDirectory("testNumDocs").toFile();
        try {
            try (Daemon daemon = new Daemon(SearchBackendTestBase.config(root));
                CloseableHttpClient client = HttpClients.createDefault())
            {
                HttpPost post = new HttpPost("http://localhost:"
                    + daemon.jsonServerPort() + "/add");
                post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                    + "\"keyword\":\"some keyword\",\"text\":\"тексты\"},{"
                    + "\"keyword\":\"another keyword\",\"text\":\"текст\"}]}",
                    StandardCharsets.UTF_8));
                HttpResponse response = client.execute(post);
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: "
                    + EntityUtils.toString(response.getEntity()),
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                post.setEntity(new StringEntity("{\"prefix\":2,\"docs\":[{"
                    + "\"keyword\":\"some keyword\",\"text\":\"текста\"}]}",
                    StandardCharsets.UTF_8));
                response = client.execute(post);
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: "
                    + EntityUtils.toString(response.getEntity()),
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                response = client.execute(new HttpGet("http://localhost:"
                    + daemon.searchServerPort() + "/numdocs"));
                String text = EntityUtils.toString(response.getEntity());
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: " + text,
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                Assert.assertEquals("{\"docs\":3,\"shards\":{"
                    + "\"0\":{\"docs\":0,\"parts\":[0]},"
                    + "\"1\":{\"docs\":2,\"parts\":[2]},"
                    + "\"2\":{\"docs\":1,\"parts\":[1]},"
                    + "\"3\":{\"docs\":0,\"parts\":[0]},"
                    + "\"4\":{\"docs\":0,\"parts\":[0]},"
                    + "\"5\":{\"docs\":0,\"parts\":[0]},"
                    + "\"6\":{\"docs\":0,\"parts\":[0]},"
                    + "\"7\":{\"docs\":0,\"parts\":[0]},"
                    + "\"8\":{\"docs\":0,\"parts\":[0]},"
                    + "\"9\":{\"docs\":0,\"parts\":[0]}}}", text);
            }
        } finally {
            System.err.println( "testNumDocs testcase stopped" );
            System.gc();
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testAlias() throws Exception {
        System.err.println( "testAlias testcase started" );
        File root = Files.createTempDirectory("testAlias").toFile();
        try {
            try (Daemon daemon = new Daemon(SearchBackendTestBase.config(root,
                    "field.keyword.index_alias = globalkw\n"
                    + "field.globalkw.tokenizer = keyword\n"
                    + "field.globalkw.attribute = true\n"
                    + "field.globalkw.analyze = true\n"));
                CloseableHttpClient client = HttpClients.createDefault())
            {
                HttpPost post = new HttpPost("http://localhost:"
                    + daemon.jsonServerPort() + "/add");
                post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                    + "\"keyword\":\"some keyword\",\"text\":\"миры\"}]}",
                    StandardCharsets.UTF_8));
                HttpResponse response = client.execute(post);
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: "
                    + EntityUtils.toString(response.getEntity()),
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                response = client.execute(new HttpGet("http://localhost:"
                    + daemon.searchServerPort()
                    + "/search?text=globalkw:some*&get=keyword,attribute"));
                String text = EntityUtils.toString(response.getEntity()).trim();
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: " + text,
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                YandexAssert.check(
                    new JsonChecker(
                        "{\"hitsCount\":1,\"hitsArray\":[{"
                        + "\"keyword\":\"some keyword\",\"attribute\":null}]}"),
                    text);
                response = client.execute(new HttpGet("http://localhost:"
                    + daemon.searchServerPort() + "/search?text=globalkw:%22"
                    + "some+keyword%22&get=keyword,attribute"));
                text = EntityUtils.toString(response.getEntity()).trim();
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: " + text,
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                YandexAssert.check(
                    new JsonChecker(
                        "{\"hitsCount\":1,\"hitsArray\":[{"
                        + "\"keyword\":\"some keyword\",\"attribute\":null}]}"),
                    text);
                post = new HttpPost("http://localhost:"
                    + daemon.jsonServerPort() + "/update");
                post.setEntity(new StringEntity("{\"prefix\":1,"
                    + "\"query\":\"keyword:*\",\"docs\":[{"
                    + "\"attribute\":\"2\"}]}",
                    StandardCharsets.UTF_8));
                response = client.execute(post);
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: "
                    + EntityUtils.toString(response.getEntity()),
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                response = client.execute(new HttpGet("http://localhost:"
                    + daemon.searchServerPort()
                    + "/search?text=globalkw:some*&get=keyword,attribute"));
                text = EntityUtils.toString(response.getEntity()).trim();
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: " + text,
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                YandexAssert.check(
                    new JsonChecker(
                        "{\"hitsCount\":1,\"hitsArray\":[{"
                        + "\"keyword\":\"some keyword\",\"attribute\":\"2\"}]}"),
                    text);
            }
        } finally {
            System.err.println( "testAlias testcase stopped" );
            System.gc();
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testMailSearch() throws Exception {
        System.err.println( "testMailSearch testcase started" );
        File root = Files.createTempDirectory("testMailSearch").toFile();
        try {
            try (Daemon daemon = new Daemon(SearchBackendTestBase.config(root,
                    "field.mid.tokenizer = keyword\n"
                    + "field.mid.filters = lowercase|yo\n"
                    + "field.mid.prefixed = true\n"
                    + "field.mid.store = true\n"
                    + "field.mid.analyze = true\n"
                    + "field.mid.attribute = true\n"));
                CloseableHttpClient client = HttpClients.createDefault())
            {
                HttpPost post = new HttpPost("http://localhost:"
                    + daemon.jsonServerPort() + "/add");
                post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                    + "\"keyword\":\"first\",\"mid\":\"1\"},{"
                    + "\"keyword\":\"second\",\"mid\":\"2\"},{"
                    + "\"keyword\":\"third\",\"mid\":\"3\"}]}",
                    StandardCharsets.UTF_8));
                HttpResponse response = client.execute(post);
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: "
                    + EntityUtils.toString(response.getEntity()),
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                response = client.execute(new HttpGet("http://localhost:"
                    + daemon.searchPort() + "/?text=1&user=1&"
                    + "getfields=keyword,mid&format=json"));
                String text = EntityUtils.toString(response.getEntity()).trim();
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: " + text,
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                YandexAssert.check(
                    new JsonChecker(
                        "{\"hitsCount\":1,\"hitsArray\":[{"
                        + "\"keyword\":\"first\",\"mid\":\"1\"}]}"),
                    text);
            }
        } finally {
            System.err.println( "testMailSearch testcase stopped" );
            System.gc();
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testAttachType() throws Exception {
        System.err.println( "testAttachType testcase started" );
        File root = Files.createTempDirectory("testAttachType").toFile();
        try {
            try (Daemon daemon = new Daemon(SearchBackendTestBase.config(root,
                    "prefix_type = string\n"
                    + "field.mid.tokenizer = keyword\n"
                    + "field.mid.filters = lowercase|yo\n"
                    + "field.mid.prefixed = true\n"
                    + "field.mid.store = true\n"
                    + "field.mid.analyze = true\n"
                    + "field.mid.attribute = true\n"
                    + "field.attachtype.tokenizer = letter\n"
                    + "field.attachtype.filters = lowercase|yo\n"
                    + "field.attachtype.prefixed = true\n"
                    + "field.attachtype.store = true\n"
                    + "field.attachtype.analyze = true\n"
                    + "field.attachtype.attribute = true\n"));
                CloseableHttpClient client = HttpClients.createDefault())
            {
                HttpPost post = new HttpPost("http://localhost:"
                    + daemon.jsonServerPort() + "/add");
                post.setEntity(new StringEntity("{\"prefix\":\"hello\","
                    + "\"docs\":[{"
                    + "\"mid\":\"1\",\"attachtype\":\"html\"},{"
                    + "\"mid\":\"2\",\"attachtype\":\"xml\"},{"
                    + "\"mid\":\"3\",\"attachtype\":\"fb2\"}]}",
                    StandardCharsets.UTF_8));
                HttpResponse response = client.execute(post);
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: "
                    + EntityUtils.toString(response.getEntity()),
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                response = client.execute(new HttpGet("http://localhost:"
                    + daemon.searchPort() + "/?text=pdf+html+xml&user=hello&"
                    + "getfields=mid&format=json&orderby=mid"));
                String text = EntityUtils.toString(response.getEntity()).trim();
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: " + text,
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                YandexAssert.check(
                    new JsonChecker(
                        "{\"hitsCount\":2,\"hitsArray\":[{"
                        + "\"mid\":\"2\"},{\"mid\":\"1\"}]}"),
                    text);
                response = client.execute(new HttpGet("http://localhost:"
                    + daemon.searchPort() + "/?text=txt%2C+fb2%2C+xml&"
                    + "user=hello&getfields=mid&format=json&orderby=mid"));
                text = EntityUtils.toString(response.getEntity()).trim();
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: " + text,
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                YandexAssert.check(
                    new JsonChecker(
                        "{\"hitsCount\":2,\"hitsArray\":[{"
                        + "\"mid\":\"3\"},{\"mid\":\"2\"}]}"),
                    text);
            }
        } finally {
            System.err.println( "testAttachType testcase stopped" );
            System.gc();
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testYo() throws Exception {
        System.err.println("testYo testcase started");
        File root = Files.createTempDirectory("testYo").toFile();
        try {
            try (Daemon daemon = new Daemon(SearchBackendTestBase.config(root,
                    "field.mid.tokenizer = keyword\n"
                    + "field.mid.filters = lowercase|yo\n"
                    + "field.mid.prefixed = true\n"
                    + "field.mid.store = true\n"
                    + "field.mid.analyze = true\n"
                    + "field.mid.attribute = true\n"
                    + "field.body_text.tokenizer = letter\n"
                    + "field.body_text.filters = "
                    + "lowercase|replace:ё:е|lemmer\n"
                    + "field.body_text.prefixed = true\n"
                    + "field.body_text.analyze = true\n"));
                CloseableHttpClient client = HttpClients.createDefault())
            {
                HttpPost post = new HttpPost("http://localhost:"
                    + daemon.jsonServerPort() + "/add");
                post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                    + "\"mid\":\"1\",\"body_text\":\"плёс ёлка "
                    + "абвгдё евразия нобиле тренд "
                    + "уууеёууу fotoğrafları\"}]}", StandardCharsets.UTF_8));
                HttpResponse response = client.execute(post);
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: "
                    + EntityUtils.toString(response.getEntity()),
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                response = client.execute(new HttpGet("http://localhost:"
                    + daemon.searchPort() + "/?text=плес&user=1&"
                    + "getfields=mid&format=json&orderby=mid"));
                String text = EntityUtils.toString(response.getEntity()).trim();
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: " + text,
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                Assert.assertEquals(
                    "{\"hitsCount\":1,\"hitsArray\":[{\"mid\":\"1\"}]}", text);
                response = client.execute(new HttpGet("http://localhost:"
                    + daemon.searchPort() + "/?text=абвгде&user=1&"
                    + "getfields=mid&format=json&orderby=mid"));
                text = EntityUtils.toString(response.getEntity()).trim();
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: " + text,
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                Assert.assertEquals(
                    "{\"hitsCount\":1,\"hitsArray\":[{\"mid\":\"1\"}]}", text);
                response = client.execute(new HttpGet("http://localhost:"
                    + daemon.searchPort() + "/?text=уууёеууу&user=1&"
                    + "getfields=mid&format=json&orderby=mid"));
                text = EntityUtils.toString(response.getEntity()).trim();
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: " + text,
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                Assert.assertEquals(
                    "{\"hitsCount\":1,\"hitsArray\":[{\"mid\":\"1\"}]}", text);
                response = client.execute(new HttpGet("http://localhost:"
                    + daemon.searchPort() + "/?text=fotoğraf&user=1&"
                    + "getfields=mid&format=json&orderby=mid"));
                text = EntityUtils.toString(response.getEntity()).trim();
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: " + text,
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                Assert.assertEquals(
                    "{\"hitsCount\":1,\"hitsArray\":[{\"mid\":\"1\"}]}", text);
            }
        } finally {
            System.err.println("testYo testcase stopped");
            System.gc();
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testPrefixesFilter() throws Exception {
        System.err.println("testSearchIndexFilter testcase started");
        File root = Files.createTempDirectory("testReplace").toFile();
        try (Daemon daemon = new Daemon(SearchBackendTestBase.config(root,
                "field.mid.tokenizer = keyword\n"
                        + "field.mid.prefixed = true\n"
                        + "field.mid.store = true\n"
                        + "field.mid.analyze = true\n"
                        + "field.mid.attribute = true\n"
                        + "field.body_text.tokenizer = letter\n"
                        + "field.body_text.index-filters = prefixes:2\n"
                        + "field.body_text.index_alias = body_text_p\n"
                        + "field.body_text.analyze = true\n"
                        + "field.body_text_p.tokenizer = letter\n"
                        + "field.body_text_p.index-filters = prefixes:2\n"
                        + "field.body_text_p.prefixed = true\n"
                        + "field.body_text_p.analyze = true\n"));
             CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost("http://localhost:"
                    + daemon.jsonServerPort() + "/add");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{\"mi"
                    + "d\":\"1\",\"body_text\":\"ворота вёр украл у, саши. Aber Sa sind nicht über\"}]}",
                    StandardCharsets.UTF_8));

            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);
            post.setEntity(new StringEntity("{\"prefix\":2,\"docs\":[{\"mi"
                    + "d\":\"2\",\"body_text\":\"Витя васю вёл за шиворот\"}]}",
                    StandardCharsets.UTF_8));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);
            String baseSearchUri = daemon.searchServerHost().toString() + "/search?prefix=1&get=mid&text=body_text:";

            String expectedFound = "{\"hitsCount\":1,\"hitsArray\":[{\"mid\":\"1\"}]}";
            String expectedEmpty = "{\"hitsCount\":0,\"hitsArray\":[]}";
            String keys = "1#A\n" +
                    "1#Ab\n" +
                    "1#Aber\n" +
                    "1#S\n" +
                    "1#Sa\n" +
                    "1#n\n" +
                    "1#ni\n" +
                    "1#nicht\n" +
                    "1#s\n" +
                    "1#si\n" +
                    "1#sind\n" +
                    "1#ü\n" +
                    "1#üb\n" +
                    "1#über\n" +
                    "1#в\n" +
                    "1#во\n" +
                    "1#ворота\n" +
                    "1#вё\n" +
                    "1#вёр\n" +
                    "1#с\n" +
                    "1#са\n" +
                    "1#саши\n" +
                    "1#у\n" +
                    "1#ук\n" +
                    "1#украл\n";
            HttpAssert.assertStringResponse(client, daemon.searchServerHost() + "/printkeys?field=body_text_p&prefix=1", keys);
            HttpAssert.assertJsonResponse(
                    client,
                    baseSearchUri + "в",
                    expectedFound);
            HttpAssert.assertJsonResponse(
                    client,
                    baseSearchUri + "вё",
                    expectedFound);
            HttpAssert.assertJsonResponse(
                    client,
                    baseSearchUri + "воро",
                    expectedEmpty);
            HttpAssert.assertJsonResponse(
                    client,
                    baseSearchUri + "nich",
                    expectedEmpty);
            HttpAssert.assertJsonResponse(
                    client,
                    baseSearchUri + "ворота",
                    expectedFound);

            String expectedSearchFound = "{\"hitsCount\":2,\"hitsArray\":[{\"mid\":\"2\"}, {\"mid\":\"1\"}]}";
            HttpAssert.assertJsonResponse(
                    client,
                    daemon.searchServerHost().toString() + "/search?&get=mid&text=body_text:в*",
                    expectedSearchFound);
            expectedSearchFound = "{\"hitsCount\":1,\"hitsArray\":[{\"mid\":\"1\"}]}";
            HttpAssert.assertJsonResponse(
                    client,
                    daemon.searchServerHost().toString() + "/search?prefix=1&get=mid&text=body_text_p:в*",
                    expectedSearchFound);
            HttpAssert.assertJsonResponse(
                    client,
                    daemon.searchServerHost().toString() + "/search?prefix=1&get=mid&text=body_text_p:ворот*",
                    expectedSearchFound);
            expectedSearchFound = "{\"hitsCount\":1,\"hitsArray\":[{\"mid\":\"2\"}]}";
            HttpAssert.assertJsonResponse(
                    client,
                    daemon.searchServerHost().toString() + "/search?prefix=2&get=mid&text=body_text_p:в*",
                    expectedSearchFound);
        } finally {
            System.err.println("testSearchIndexFilter testcase stopped");
            System.gc();
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testReplace() throws Exception {
        System.err.println("testReplace testcase started");
        File root = Files.createTempDirectory("testReplace").toFile();
        try (Daemon daemon = new Daemon(SearchBackendTestBase.config(root,
                "field.mid.tokenizer = keyword\n"
                + "field.mid.filters = lowercase|yo\n"
                + "field.mid.prefixed = true\n"
                + "field.mid.store = true\n"
                + "field.mid.analyze = true\n"
                + "field.mid.attribute = true\n"
                + "field.body_text.tokenizer = letter\n"
                + "field.body_text.filters = replace:aa:mmm|replace:bb:b"
                + "|replace:ccc:\n"
                + "field.body_text.prefixed = true\n"
                + "field.body_text.analyze = true\n"));
            CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{\"mi"
                + "d\":\"1\",\"body_text\":\"aacdbbcbbebb ccc acccb\"}]}",
                StandardCharsets.UTF_8));
            HttpResponse response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchPort() + "/?text=mmmcdbbcbeb&user=1&"
                + "getfields=mid&format=json&orderby=mid"));
            String text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            Assert.assertEquals(
                "{\"hitsCount\":1,\"hitsArray\":[{\"mid\":\"1\"}]}",
                text);
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&"
                + "text=body_text:%22cccccc+ab%22&get=mid"));
            text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            Assert.assertEquals(
                "{\"hitsCount\":1,\"hitsArray\":[{\"mid\":\"1\"}]}",
                text);
        } finally {
            System.err.println("testReplace testcase stopped");
            System.gc();
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testUrl() throws Exception {
        System.err.println("testUrl testcase started");
        File root = Files.createTempDirectory("testUrl").toFile();
        try (Daemon daemon = new Daemon(SearchBackendTestBase.config(root,
                "field.mid.tokenizer = keyword\n"
                + "field.mid.filters = lowercase|yo\n"
                + "field.mid.prefixed = true\n"
                + "field.mid.store = true\n"
                + "field.mid.analyze = true\n"
                + "field.mid.attribute = true\n"
                + "field.body_text.tokenizer = letter\n"
                + "field.body_text.filters = lowercase|yo|lemmer\n"
                + "field.body_text.prefixed = true\n"
                + "field.body_text.analyze = true\n"));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                + "\"mid\":\"1\",\"body_text\":\"http://www.example.com "
                + "http://www.example.com/path/to/name "
                + "HTTP://EN.EXAMPLE1.ORG/ "
                + "http://en.example.org/ "
                + "www.example.com "
                + "example.com "
                + "http://www.лдаоырпываю.рф "
                + "squ[are]s aga(i)n\"}]}",
                StandardCharsets.UTF_8));
            HttpResponse response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchPort() + "/?text=HTTP://EN.EXAMPLE.ORG/"
                + "&user=1&getfields=mid&format=json&orderby=mid"));
            String text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            Assert.assertEquals(
                "{\"hitsCount\":1,\"hitsArray\":[{\"mid\":\"1\"}]}",
                text);
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchPort() + "/?text=http://en.example1.org/"
                + "&user=1&getfields=mid&format=json&orderby=mid"));
            text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            Assert.assertEquals(
                "{\"hitsCount\":1,\"hitsArray\":[{\"mid\":\"1\"}]}",
                text);
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchPort() + "/?text=squ[are]s"
                + "&user=1&getfields=mid&format=json&orderby=mid"));
            text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            Assert.assertEquals(
                "{\"hitsCount\":1,\"hitsArray\":[{\"mid\":\"1\"}]}",
                text);
        } finally {
            System.err.println("testUrl testcase stopped");
            System.gc();
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testMultiSearch() throws Exception {
        System.err.println( "testMultiSearch testcase started" );
        File root = Files.createTempDirectory("testMultiSearch").toFile();
        try (Daemon daemon = new Daemon(SearchBackendTestBase.config(
                root,
                "field.mid.tokenizer = keyword\n"
                + "field.mid.filters = lowercase|yo\n"
                + "field.mid.prefixed = true\n"
                + "field.mid.store = true\n"
                + "field.mid.analyze = true\n"
                + "field.mid.attribute = true\n"
                + "field.body_text.tokenizer = letter\n"
                + "field.body_text.filters = lowercase|lemmer\n"
                + "field.body_text.prefixed = true\n"
                + "field.body_text.analyze = true\n"
                + "field.hdr_from.tokenizer = letter\n"
                + "field.hdr_from.filters = lowercase|replace:ё:е\n"
                + "field.hdr_from.prefixed = true\n"
                + "field.hdr_from.analyze = true\n"));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                + "\"mid\":\"1\",\"body_text\":\"дивный мир\","
                + "\"hdr_from\":\"vasya@pupkin.com\"},{"
                + "\"mid\":\"2\",\"body_text\":\"дивный новый мир\","
                + "\"hdr_from\":\"putin@voffka.com\"},{"
                + "\"mid\":\"3\",\"body_text\":\"новый мира\","
                + "\"hdr_from\":\"analizer@ya.ru\"},{"
                + "\"mid\":\"4\",\"body_text\":\"дивный мир\","
                + "\"hdr_from\":\"potapov.d@gmail.com\"}]}",
                StandardCharsets.UTF_8));
            HttpResponse response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchPort() + "/?text=дивный+мира&user=1&"
                + "getfields=mid&format=json&orderby=mid"));
            String text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":3,\"hitsArray\":[{"
                    + "\"mid\":\"4\"},{\"mid\":\"2\"},{\"mid\":\"1\"}]}"),
                text);
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchPort() + "/?text=новый+мир+ya&user=1&"
                + "getfields=mid&format=json&orderby=mid"));
            text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":1,\"hitsArray\":[{"
                    + "\"mid\":\"3\"}]}"),
                text);
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchPort() + "/?text=%22дивный+мир%22+com"
                + "&user=1&getfields=mid&format=json&orderby=mid"));
            text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":2,\"hitsArray\":[{"
                    + "\"mid\":\"4\"},{\"mid\":\"1\"}]}"),
                text);
        } finally {
            System.err.println( "testMultiSearch testcase stopped" );
            System.gc();
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testUnicode() throws Exception {
        System.err.println( "testUnicode testcase started" );
        File root = Files.createTempDirectory("testUnicode").toFile();
        try (Daemon daemon = new Daemon(SearchBackendTestBase.config(root));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                + "\"keyword\":\"\\u00a9\",\"text\":\"Hi\\u00ae\"}]}",
                StandardCharsets.UTF_8));
            HttpResponse response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort()
                + "/search?prefix=1&text=text:hi&get=keyword"));
            String text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":1,\"hitsArray\":[{"
                    + "\"keyword\":\"©\"}]}"),
                text);
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort()
                + "/search?prefix=1&text=keyword:©&get=keyword"));
            text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":1,\"hitsArray\":[{"
                    + "\"keyword\":\"©\"}]}"),
                text);
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort()
                + "/search?prefix=1&text=text:u*&get=keyword"));
            text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker("{\"hitsCount\":0,\"hitsArray\":[]}"),
                text);
        } finally {
            System.err.println( "testUnicode testcase stopped" );
            System.gc();
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testBadUnicode() throws Exception {
        System.err.println( "testBadUnicode testcase started" );
        File root = Files.createTempDirectory("testBadUnicode").toFile();
        try (Daemon daemon = new Daemon(SearchBackendTestBase.config(root));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                + "\"keyword\":\"some keyword\",\"text\":\"Hi\\ufdef\"}]}",
                StandardCharsets.UTF_8));
            HttpResponse response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort()
                + "/search?prefix=1&text=text:hi&get=keyword"));
            String text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":1,\"hitsArray\":[{"
                    + "\"keyword\":\"some keyword\"}]}"),
                text);
        } finally {
            System.err.println( "testBadUnicode testcase stopped" );
            System.gc();
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testMalformedUnicode() throws Exception {
        System.err.println( "testMalformedUnicode testcase started" );
        File root = Files.createTempDirectory("testMalformedUnicode").toFile();
        try (Daemon daemon = new Daemon(SearchBackendTestBase.config(root));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            byte[] strbuf = "{\"prefix\":1,\"docs\":[{\"keyword\":\"hello"
                .getBytes(StandardCharsets.UTF_8);
            byte[] array = Arrays.copyOf(strbuf, strbuf.length + 5);
            array[strbuf.length + 0] = (byte) 0x80; // invalid UTF-8 byte
            array[strbuf.length + 1] = (byte) 0x22; // "
            array[strbuf.length + 2] = (byte) 0x7D; // }
            array[strbuf.length + 3] = (byte) 0x5D; // ]
            array[strbuf.length + 4] = (byte) 0x7D; // }
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(new InputStreamEntity(
                new ByteArrayInputStream(array), array.length));
            HttpResponse response = client.execute(post);
            Assert.assertEquals("Expected 400 Bad Request, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()),
                HttpStatus.SC_BAD_REQUEST,
                response.getStatusLine().getStatusCode());
        } finally {
            System.err.println( "testMalformedUnicode testcase stopped" );
            System.gc();
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testDatedSearch() throws Exception {
        System.err.println("testDatedSearch testcase started");
        File root = Files.createTempDirectory("testDatedSearch").toFile();
        try (Daemon daemon = new Daemon(SearchBackendTestBase.config(
                root,
                "ignored_fields = unknown_field\n"
                + "field.mid.tokenizer = keyword\n"
                + "field.mid.filters = lowercase|yo\n"
                + "field.mid.prefixed = true\n"
                + "field.mid.store = true\n"
                + "field.mid.analyze = true\n"
                + "field.mid.attribute = true\n"
                + "field.body_text.tokenizer = letter\n"
                + "field.body_text.filters = lowercase|lemmer\n"
                + "field.body_text.prefixed = true\n"
                + "field.body_text.analyze = true\n"
                + "[field.suid]\n"
                + "tokenizer = keyword\n"
                + "store = true\n"
                + "attribute = true\n"));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                + "\"mid\":\"1\",\"suid\":1,\"body_text\":"
                + "\"Лошади карандаши\","
                + "\"received_date\":\"1368359690\"},{"
                + "\"mid\":\"2\",\"suid\":1,\"body_text\":\""
                + "Кони тетради\",\"unknown_field\":\"nothing\","
                + "\"received_date\":\"1368446082\"},{"
                + "\"mid\":\"3\",\"suid\":\"1\",\"body_text\":\""
                + "ЛоШадь КоНь карандаш тетрадь"
                + "\",\"received_date\":\"1368532475\"}]}",
                StandardCharsets.UTF_8));
            HttpResponse response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchPort() + "/?text=тетрадь&user=1&"
                + "getfields=mid&format=json&dated=1&"
                + "from_year=2013&from_month=4&from_day=13"));
            String text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":2,\"hitsArray\":[{"
                    + "\"mid\":\"3\"},{\"mid\":\"2\"}]}"),
                text);
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchPort() + "/?text=&user=1&"
                + "getfields=mid&format=json&dated=1&"
                + "from_year=2013&from_month=4&from_day=13&orderby=mid"));
            text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":2,\"hitsArray\":[{"
                    + "\"mid\":\"3\"},{\"mid\":\"2\"}]}"),
                text);
        } finally {
            System.err.println("testDatedSearch testcase stopped");
            System.gc();
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testDeletesWeight() throws Exception {
        System.err.println( "testDeletesWeight testcase started" );
        File root = Files.createTempDirectory("testDeletesWeight").toFile();
        try (Daemon daemon =
                new Daemon(SearchBackendTestBase.config(root, "maxmemdocs = 100000"));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            StringBuilder sb = new StringBuilder("http://localhost:");
            sb.append(daemon.jsonServerPort());
            sb.append("/delete?text=text:");
            for (int i = 0; i < 100; ++i) {
                sb.append(
                    "very_long_line_to_be_remove_from_index_forever");
            }
            sb.append("&prefix=");
            String request = sb.toString();
            for (int i = 0; i < 5000; ++i) {
                HttpResponse response =
                    client.execute(new HttpGet(request + i));
                Assert.assertEquals("At iteration " + i
                    + "Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: "
                    + EntityUtils.toString(response.getEntity()),
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
            }
        } finally {
            System.err.println( "testDeletesWeight testcase stopped" );
            System.gc();
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testLongTerms() throws Exception {
        System.err.println( "testLongTerms testcase started" );
        File root = Files.createTempDirectory("testDeletesWeight").toFile();
        try (Daemon daemon =
                new Daemon(SearchBackendTestBase.config(root, "maxmemdocs = 100000"));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            final int PREFIX_SIZE = "1#".length();
            final int HEADER_SIZE = 2;
            final int MEDIUM_TERM_SIZE =
                ByteBlockPool.BYTE_BLOCK_SIZE - PREFIX_SIZE - HEADER_SIZE;
            final int LARGE_TERM_SIZE =
                (ByteBlockPool.LARGE_BLOCK_SIZE) - PREFIX_SIZE;
            StringBuilder sb = new StringBuilder(LARGE_TERM_SIZE);
            while (sb.length() < MEDIUM_TERM_SIZE) {
                sb.append("AbyrvalgAbyrValgQweQweAsdAsd");
            }
            sb.setLength(MEDIUM_TERM_SIZE - 10);
            final String mediumTerm = sb.toString();
            //index medium term
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
//            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
//                + "\"keyword\":\"qwe\"}]}",
//                StandardCharsets.UTF_8));
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                + "\"keyword\":\"" + mediumTerm + "\"}]}",
                StandardCharsets.UTF_8));
            HttpResponse response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());

            while (sb.length() < LARGE_TERM_SIZE) {
                sb.append("AbyrvalgAbyrValgQweQweAsdAsd");
            }
            sb.setLength(LARGE_TERM_SIZE);
            final String largeTerm = sb.toString();
            //index large term
            post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                + "\"keyword\":\"" + largeTerm + "\"}]}",
                StandardCharsets.UTF_8));
            response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());

            //wildcard search
            response = client.execute(
                new HttpGet(
                    "http://localhost:" + daemon.searchServerPort()
                    + "/search?text=keyword:*&prefix=1&get=keyword"));
            String text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            Assert.assertEquals(
                "{\"hitsCount\":2,\"hitsArray\":[{"
                + "\"keyword\":\"" + mediumTerm + "\"}"
                + ",{\"keyword\":\"" + largeTerm + "\"}]}",
                text);

            response = client.execute(
                new HttpGet(
                    "http://localhost:" + daemon.searchServerPort()
                    + "/search?text=keyword:*Byrvalg*&prefix=1&get=keyword"));
            text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker("{\"hitsCount\":0,\"hitsArray\":[]}"),
                text);

            response = client.execute(
                new HttpGet(
                    "http://localhost:" + daemon.searchServerPort()
                    + "/search?text=keyword:*Byrvalg*&prefix=1&get=keyword"
                    + "&lowercase-expanded-terms"));
            text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            Assert.assertEquals(
                "{\"hitsCount\":2,\"hitsArray\":[{"
                + "\"keyword\":\"" + mediumTerm + "\"}"
                + ",{\"keyword\":\"" + largeTerm + "\"}]}",
                text);
        } finally {
            System.err.println( "testLongTerms testcase stopped" );
            System.gc();
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testMinQueueId() throws Exception {
        System.err.println( "testMinQueueId testcase started" );
        File root = Files.createTempDirectory("testMinQueueId").toFile();
        try (Daemon daemon =
                new Daemon(SearchBackendTestBase.config(root, "maxmemdocs = 100000"));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            final String prefix1 = "0";
            final String prefix2 = Long.toString(QueueShard.SHARDS_MAGIC);
            final String shard = "0";
            int keyword = 0;
            //1: index one document to lucene shard 1
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(
                new StringEntity(
                    "{\"prefix\":" + prefix1 + ",\"docs\":[{"
                    + "\"keyword\":" + keyword + "}]}",
                    StandardCharsets.UTF_8));
            post.addHeader(YandexHeaders.ZOO_QUEUE_ID, newQueueId());
            post.addHeader(YandexHeaders.ZOO_SHARD_ID, shard);
            post.addHeader(YandexHeaders.ZOO_QUEUE, "popa");

            HttpResponse response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());

            response = client.execute(
                new HttpGet(
                    "http://localhost:"
                    + daemon.jsonServerPort() + "/getQueueId?service=popa"
                    + "&shard=" + shard));
            String text = EntityUtils.toString(response.getEntity());
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            Assert.assertEquals(lastQueueId(), text);
            response = client.execute(
                new HttpGet(
                    "http://localhost:"
                    + daemon.jsonServerPort() + "/getQueueId?service=popa"
                    + "&prefix=1130000014487050"));
            text = EntityUtils.toString(response.getEntity());
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            Assert.assertEquals(lastQueueId(), text);

            final long firstQueueId = queueId.get() - 1;

            keyword++;
            //2: index one document to different lucene shard
            post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(
                new StringEntity(
                    "{\"prefix\":" + prefix2 + ",\"docs\":[{"
                    + "\"keyword\":" + keyword + "}]}",
                    StandardCharsets.UTF_8));
            post.addHeader(YandexHeaders.ZOO_QUEUE_ID, newQueueId());
            post.addHeader(YandexHeaders.ZOO_SHARD_ID, shard);
            post.addHeader(YandexHeaders.ZOO_QUEUE, "popa");

            response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            //2.1: index weak queueid check document
            post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(
                new StringEntity(
                    "{\"prefix\":" + prefix2 + ",\"docs\":[{"
                    + "\"keyword\":" + keyword + "}]}",
                    StandardCharsets.UTF_8));
            post.addHeader(
                YandexHeaders.ZOO_QUEUE_ID_TO_CHECK,
                Long.toString(queueId.get() - 1));
            post.addHeader(YandexHeaders.ZOO_SHARD_ID, shard);
            post.addHeader(YandexHeaders.ZOO_QUEUE, "popa");

            response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()),
                HttpStatus.SC_ACCEPTED,
                response.getStatusLine().getStatusCode());

            //2.2: index another bigger weak queueid check document
            //further queueId checking should not be afftected by weak docs
            post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(
                new StringEntity(
                    "{\"prefix\":" + prefix2 + ",\"docs\":[{"
                    + "\"keyword\":" + keyword + "}]}",
                    StandardCharsets.UTF_8));
            post.addHeader(
                YandexHeaders.ZOO_QUEUE_ID_TO_CHECK,
                Long.toString(queueId.get() + 10));
            post.addHeader(YandexHeaders.ZOO_SHARD_ID, shard);
            post.addHeader(YandexHeaders.ZOO_QUEUE, "popa");

            response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());


            //3: getQueueId must return maximum queueId from the two shards
            response = client.execute(
                new HttpGet(
                    "http://localhost:"
                    + daemon.jsonServerPort() + "/getQueueId?service=popa"
                    + "&shard=" + shard));
            text = EntityUtils.toString(response.getEntity());
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            Assert.assertEquals(Long.toString(queueId.get() - 1), text);


            //4: replay journal
            File root2 =
                Files.createTempDirectory("testMinQueueIdJournal1_").toFile();
            try {
                File journal = new File(root2, "index/0/journal");
                journal.mkdirs();
                Path dst = journal.toPath();
                for (File file
                    : new File(root, "index/0/journal").listFiles())
                {
                    Path src = file.toPath();
                    Files.copy(src, dst.resolve(src.getFileName()));
                }
                journal = new File(root2, "index/4/journal");
                journal.mkdirs();
                dst = journal.toPath();
                for (File file
                    : new File(root, "index/4/journal").listFiles())
                {
                    Path src = file.toPath();
                    Files.copy(src, dst.resolve(src.getFileName()));
                }
                try (Daemon daemon2 = new Daemon(SearchBackendTestBase.config(root2))) {
                    //5: getQueueId must return MINinum queueId from the two shards
                    response = client.execute(
                    new HttpGet(
                        "http://localhost:"
                        + daemon2.jsonServerPort() + "/getQueueId?service=popa"
                        + "&shard=" + shard));
                    text = EntityUtils.toString(response.getEntity());
                    Assert.assertEquals("Expected 200 OK, but received: "
                        + response.getStatusLine() + " and body: " + text,
                        HttpStatus.SC_OK,
                        response.getStatusLine().getStatusCode());
                    Assert.assertEquals(Long.toString(firstQueueId), text);
                }
            } finally {
                SearchBackendTestBase.removeDirectory(root2);
            }

            //6: flush second shard, new queueid should be pushed to the first
            //shard journal
            response = client.execute(
                new HttpGet(
                    "http://localhost:"
                    + daemon.jsonServerPort() + "/flush?wait=true&shards=4"));
            text = EntityUtils.toString(response.getEntity());
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            root2 =
                Files.createTempDirectory("testMinQueueIdJournal2_").toFile();
            try {
                //copy only first shard journal
                File journal = new File(root2, "index/0/journal");
                journal.mkdirs();
                Path dst = journal.toPath();
                for (File file
                    : new File(root, "index/0/journal").listFiles())
                {
                    Path src = file.toPath();
                    Files.copy(src, dst.resolve(src.getFileName()));
                }
                try (Daemon daemon2 = new Daemon(SearchBackendTestBase.config(root2))) {
                    //7: getQueueId must return maxinum queueId from the two shards
                    response = client.execute(
                    new HttpGet(
                        "http://localhost:"
                        + daemon2.jsonServerPort() + "/getQueueId?service=popa"
                        + "&shard=" + shard));
                    text = EntityUtils.toString(response.getEntity());
                    Assert.assertEquals("Expected 200 OK, but received: "
                        + response.getStatusLine() + " and body: " + text,
                        HttpStatus.SC_OK,
                        response.getStatusLine().getStatusCode());
                    Assert.assertEquals(Long.toString(queueId.get() - 1), text);
                }
            } finally {
                SearchBackendTestBase.removeDirectory(root2);
            }

        } finally {
            System.err.println("testMinQueueId testcase stopped");
            System.gc();
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testDeleteViaProxy() throws Exception {
        try (TestSearchBackend lucene = new TestSearchBackend(
                this,
                new File(
                    Paths.getSourcePath(
                        "mail/search/mail/search_backend_mail_config/files"
                        + "/search_backend_test.conf_mail")));
            StaticServer proxy = new StaticServer(Configs.baseConfig()))
        {
            AtomicInteger counter = new AtomicInteger();
            proxy.register(
                new Pattern<>("/delete", false),
                new CountingHttpRequestHandler(
                    new ProxyHandler(lucene.indexerPort()),
                    counter));
            proxy.start();

            lucene.add("\"url\":\"1\"", "\"url\":\"2\"", "\"url\":\"3\"");
            lucene.checkSearch(
                "/search?prefix=0&get=url&sort=url&text=url:*",
                TestSearchBackend.prepareResult(
                    "\"url\":\"3\"",
                    "\"url\":\"2\"",
                    "\"url\":\"1\""));
            Assert.assertEquals(0, counter.get());

            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                proxy.port(),
                "/delete?prefix=0&text=url:3");
            lucene.checkSearch(
                "/search?prefix=0&get=url&sort=url&text=url:*",
                TestSearchBackend.prepareResult("\"url\":\"2\"", "\"url\":\"1\""));
            Assert.assertEquals(1, counter.get());

            HttpPost post =
                new HttpPost("http://localhost:" + proxy.port() + "/delete");
            post.setEntity(
                new StringEntity("{\"prefix\":0,\"docs\":[{\"url\":\"1\"}]}"));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);
            lucene.checkSearch(
                "/search?prefix=0&get=url&sort=url&text=url:*",
                TestSearchBackend.prepareResult("\"url\":\"2\""));
            Assert.assertEquals(2, counter.get());
        }
    }

    @Test
    public void testSumQueue() throws Exception {
        try (TestSearchBackend lucene = new TestSearchBackend(
            this,
            new File(
                Paths.getSourcePath(
                    "mail/search/mail/search_backend_mail_config/files"
                    + "/search_backend_test.conf_mail"))))
        {
            String start = "{\n"
                + "  \"prefix\": 9000,\n"
                + "  \"AddIfNotExists\":true,\n"
                + "  \"docs\": [\n"
                + "    {\n"
                + "      \"url\": \"reqs_9000_request\",\n"
                + "       \"request_mids\": {\"function\": \"sum_queue\", "
                + "\"args\": [\"";
            String end = "\", {\"function\": \"get\", \"args\": "
                + "[\"request_mids\"]}, 5]}\n"
                + "    }]\n"
                + "}";

            HttpPost post = new HttpPost(lucene.indexerUri() + "/update");
            System.out.println(start + "2370000003264406640" + end);
            post.setEntity(
                new StringEntity(start + "2370000003264406640" + end));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);

            post.setEntity(
                new StringEntity(
                    start + "" + end, StandardCharsets.UTF_8));

            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);

            lucene.checkSearch(
                "/search?prefix=9000&get=request_mids&text=url:*",
                TestSearchBackend.prepareResult(
                    "\"request_mids\":"
                        + "\"2370000003264406640\t1\""));

            post.setEntity(
                new StringEntity(start + "2370000003264406641" + end));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);

            post.setEntity(
                new StringEntity(start + "2370000003264406640" + end));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);

            lucene.checkSearch(
                "/search?prefix=9000&get=request_mids&text=url:*",
                TestSearchBackend.prepareResult(
                    "\"request_mids\":\""
                        + "2370000003264406640\t2\n"
                        + "2370000003264406641\t1\""));

            post.setEntity(
                new StringEntity(start + "2370000003264406639" + end));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);

            post.setEntity(
                new StringEntity(start + "2370000003264406639" + end));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);

            post.setEntity(
                new StringEntity(start + "2370000003264406642" + end));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);

            lucene.checkSearch(
                "/search?prefix=9000&get=request_mids&text=url:*",
                TestSearchBackend.prepareResult(
                    "\"request_mids\":\""
                        + "2370000003264406642\t1\n"
                        + "2370000003264406639\t2\n"
                        + "2370000003264406640\t2\n"
                        + "2370000003264406641\t1\""));
            post.setEntity(
                new StringEntity(start + "2370000003264406643" + end));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);

            post.setEntity(
                new StringEntity(start + "2370000003264406644" + end));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);

            lucene.checkSearch(
                "/search?prefix=9000&get=request_mids&text=url:*",
                TestSearchBackend.prepareResult(
                    "\"request_mids\":\""
                        + "2370000003264406644\t1\n"
                        + "2370000003264406643\t1\n"
                        + "2370000003264406642\t1\n"
                        + "2370000003264406639\t2\n"
                        + "2370000003264406640\t2\""));
        }
    }

    @Test
    public void testSumMap() throws Exception {
        try (TestSearchBackend lucene = new TestSearchBackend(
            this,
            new File(
                Paths.getSourcePath(
                    "mail/search/mail/search_backend_mail_config/files"
                    + "/search_backend_test.conf_mail"))))
        {
            String start = "{\n"
                + "  \"prefix\": 9000,\n"
                + "  \"AddIfNotExists\":true,\n"
                + "  \"docs\": [\n"
                + "    {\n"
                + "      \"url\": \"umtype_9000\",\n"
                + "       \"mtype_show_count\": {\"function\": \"sum_map\", "
                + "\"args\": [\"";
            String end = "\", {\"function\": \"get\", \"args\": "
                + "[\"mtype_show_count\"]}]}\n"
                + "    }]\n"
                + "}";

            HttpPost post = new HttpPost(lucene.indexerUri() + "/update");
            post.setEntity(
                new StringEntity(start + "4,5\t1\n1,2,3\t1" + end));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);

            post.setEntity(
                new StringEntity(
                    start + "1,2\t1\n1,2,3\t1" + end, StandardCharsets.UTF_8));

            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);

            String searchUri =
                lucene.searchUri()
                    + "/search?prefix=9000&service=pg"
                    + "&text=url:umtype_9000&get=mtype_show_count";

            lucene.checkSearch(
                "/search?prefix=9000&get=mtype_show_count&text=url:*",
                TestSearchBackend.prepareResult(
                    "\"mtype_show_count\":"
                        + "\"1,2\t1\n1,2,3\t2\n4,5\t1\""));
        }
    }

    @Test
    public void testDefaultFunction() throws Exception {
        try (TestSearchBackend lucene = new TestSearchBackend(
            this,
            new File(
                Paths.getSourcePath(
                    "mail/search/mail/search_backend_mail_config/files"
                    + "/search_backend_test.conf_mail"))))
        {
            HttpPost post = new HttpPost(lucene.indexerUri() + "/update");
            String update = "{\n "
                + "  \"prefix\": 9001,\n "
                + "  \"AddIfNotExists\":true,\n "
                + "  \"docs\": [\n"
                + "    {\n"
                + "      \"url\": \"umtype_9001\",\n"
                + "       \"mtype_show_count\": {\"function\": \"default\", "
                + "\"args\": [\"100500\"]}}]\n"
                + '}';
            post.setEntity(new StringEntity(update));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);

            lucene.checkSearch(
                "/search?prefix=9001&get=mtype_show_count&text=url:*",
                TestSearchBackend.prepareResult(
                    "\"mtype_show_count\":\"100500\""));

            update = "{\n "
                + "  \"prefix\": 9001,\n "
                + "  \"AddIfNotExists\":true,\n "
                + "  \"docs\": [\n"
                + "    {\n"
                + "      \"url\": \"umtype_9001\",\n"
                + "       \"mtype_show_count\": {\"function\": \"default\", "
                + "\"args\": [\"100501\"]}}]\n"
                + '}';
            post.setEntity(new StringEntity(update));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);

            lucene.checkSearch(
                "/search?prefix=9001&get=mtype_show_count&text=url:*",
                TestSearchBackend.prepareResult(
                    "\"mtype_show_count\":\"100500\""));
        }
    }

    @Test
    public void testFunctionIterationConsistency() throws Exception {
        File root =
            Files.createTempDirectory("testFunctionIterationConsistency")
                .toFile();
        try (TestSearchBackend lucene =
            new TestSearchBackend(
                this,
                SearchBackendTestBase.config(
                    root,
                    "primary_key = keyword\n"
                    + "field.cnt01.store=true\n"
                    + "field.cnt02.store=true\n"
                    + "field.cnt03.store=true\n"
                    + "field.cnt04.store=true\n"
                    + "field.cnt05.store=true\n"
                    + "field.cnt06.store=true\n"
                    + "field.cnt07.store=true\n"
                    + "field.cnt08.store=true\n"
                    + "field.cnt09.store=true\n"
                    + "field.cnt10.store=true\n"
                    + "field.cnt11.store=true\n"
                    + "field.cnt12.store=true\n"
                    + "field.cnt13.store=true\n"
                    + "field.cnt14.store=true\n"
                    + "field.cnt15.store=true\n")))
        {
            HttpPost post = new HttpPost(lucene.indexerUri() + "/update");
            String update = "{\n "
                + "  \"prefix\": 9001,\n "
                + "  \"AddIfNotExists\":true,\n "
                + "  \"docs\": [\n"
                + "    {\n"
                + "      \"keyword\":\"url\",\n"
                + "      \"cnt01\":{\"inc\":[]},\n"
                + "      \"cnt02\": {\"sum\":[\"1\",{\"get\":[\"cnt01\"]}]}\n"
                + "    }\n"
                + "  ]\n"
                + '}';
            System.err.println("JSON: " + update);
            post.setEntity(new StringEntity(update));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);

            lucene.checkSearch(
                "/search?prefix=9001&get=cnt01,cnt02&text=keyword:*",
                TestSearchBackend.prepareResult(
                    "\"cnt01\":\"1\","
                    + "\"cnt02\":\"2\""));

            update = "{\n "
                + "  \"prefix\": 9001,\n "
                + "  \"docs\": [\n"
                + "    {\n"
                + "      \"keyword\":\"url\",\n"
                + "      \"cnt01\": {\"inc\":[]},\n"
                + "      \"cnt02\": {\"sum\":[\"1\",{\"get\":[\"cnt01\"]}]},\n"
                + "      \"cnt03\": {\"sum\":[\"1\",{\"get\":[\"cnt02\"]}]},\n"
                + "      \"cnt04\": {\"sum\":[\"1\",{\"get\":[\"cnt03\"]}]},\n"
                + "      \"cnt05\": {\"sum\":[\"1\",{\"get\":[\"cnt04\"]}]},\n"
                + "      \"cnt06\": {\"sum\":[\"1\",{\"get\":[\"cnt05\"]}]},\n"
                + "      \"cnt07\": {\"sum\":[\"1\",{\"get\":[\"cnt06\"]}]},\n"
                + "      \"cnt08\": {\"sum\":[\"1\",{\"get\":[\"cnt07\"]}]},\n"
                + "      \"cnt09\": {\"sum\":[\"1\",{\"get\":[\"cnt08\"]}]},\n"
                + "      \"cnt10\": {\"sum\":[\"1\",{\"get\":[\"cnt09\"]}]},\n"
                + "      \"cnt11\": {\"sum\":[\"1\",{\"get\":[\"cnt10\"]}]},\n"
                + "      \"cnt12\": {\"sum\":[\"1\",{\"get\":[\"cnt11\"]}]},\n"
                + "      \"cnt13\": {\"sum\":[\"1\",{\"get\":[\"cnt12\"]}]},\n"
                + "      \"cnt14\": {\"sum\":[\"1\",{\"get\":[\"cnt13\"]}]},\n"
                + "      \"cnt15\": {\"sum\":[\"1\",{\"get\":[\"cnt14\"]}]}\n"
                + "    }\n"
                + "  ]\n"
                + '}';
            System.err.println("JSON: " + update);
            post.setEntity(new StringEntity(update));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);

            lucene.checkSearch(
                "/search?prefix=9001&get=cnt01,cnt02,cnt03,cnt04,cnt05,cnt06"
                    + ",cnt07,cnt08,cnt09"
                    + ",cnt10,cnt11,cnt12,cnt13,cnt14,cnt15&text=keyword:*",
                TestSearchBackend.prepareResult(
                    "\"cnt01\":\"2\","
                    + "\"cnt02\":\"3\","
                    + "\"cnt03\":\"4\","
                    + "\"cnt04\":\"5\","
                    + "\"cnt05\":\"6\","
                    + "\"cnt06\":\"7\","
                    + "\"cnt07\":\"8\","
                    + "\"cnt08\":\"9\","
                    + "\"cnt09\":\"10\","
                    + "\"cnt10\":\"11\","
                    + "\"cnt11\":\"12\","
                    + "\"cnt12\":\"13\","
                    + "\"cnt13\":\"14\","
                    + "\"cnt14\":\"15\","
                    + "\"cnt15\":\"16\""));
        } finally {
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testFunctionIterationConsistencyOrderIndependent() throws Exception {
        File root =
                Files.createTempDirectory("testFunctionIterationConsistency")
                        .toFile();
        try (TestSearchBackend lucene =
                     new TestSearchBackend(
                             this,
                             SearchBackendTestBase.config(
                                     root,
                                     "primary_key = keyword\n"
                                             + "field.cnt01.store=true\n"
                                             + "field.cnt02.store=true\n"
                                             + "field.cnt03.store=true\n"
                                             + "field.cnt04.store=true\n"
                                             + "field.cnt05.store=true\n"
                                             + "field.cnt06.store=true\n"
                                             + "field.cnt07.store=true\n"
                                             + "field.cnt08.store=true\n"
                                             + "field.cnt09.store=true\n"
                                             + "field.cnt10.store=true\n"
                                             + "field.cnt11.store=true\n"
                                             + "field.cnt12.store=true\n"
                                             + "field.cnt13.store=true\n"
                                             + "field.cnt14.store=true\n"
                                             + "field.cnt15.store=true\n")))
        {
            HttpPost post = new HttpPost(lucene.indexerUri() + "/update?order-independent-update=true");
            String update = "{\n "
                    + "  \"prefix\": 9001,\n "
                    + "  \"AddIfNotExists\":true,\n "
                    + "  \"docs\": [\n"
                    + "    {\n"
                    + "      \"keyword\":\"url\",\n"
                    + "      \"cnt01\":{\"inc\":[]},\n"
                    + "      \"cnt02\": {\"sum\":[\"1\",{\"get\":[\"cnt01\"]}]}\n"
                    + "    }\n"
                    + "  ]\n"
                    + '}';
            System.err.println("JSON: " + update);
            post.setEntity(new StringEntity(update));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);

            lucene.checkSearch(
                    "/search?prefix=9001&get=cnt01,cnt02&text=keyword:*",
                    TestSearchBackend.prepareResult(
                            "\"cnt01\":\"1\","
                                    + "\"cnt02\":\"1\""));

            update = "{\n "
                    + "  \"prefix\": 9001,\n "
                    + "  \"docs\": [\n"
                    + "    {\n"
                    + "      \"keyword\":\"url\",\n"
                    + "      \"cnt01\": {\"inc\":[]},\n"
                    + "      \"cnt02\": {\"sum\":[\"1\",{\"get\":[\"cnt01\"]}]},\n"
                    + "      \"cnt03\": {\"sum\":[\"1\",{\"get\":[\"cnt02\"]}]},\n"
                    + "      \"cnt04\": {\"sum\":[\"1\",{\"get\":[\"cnt03\"]}]},\n"
                    + "      \"cnt05\": {\"sum\":[\"1\",{\"get\":[\"cnt04\"]}]},\n"
                    + "      \"cnt06\": {\"sum\":[\"1\",{\"get\":[\"cnt05\"]}]},\n"
                    + "      \"cnt07\": {\"sum\":[\"1\",{\"get\":[\"cnt06\"]}]},\n"
                    + "      \"cnt08\": {\"sum\":[\"1\",{\"get\":[\"cnt07\"]}]},\n"
                    + "      \"cnt09\": {\"sum\":[\"1\",{\"get\":[\"cnt08\"]}]},\n"
                    + "      \"cnt10\": {\"sum\":[\"1\",{\"get\":[\"cnt09\"]}]},\n"
                    + "      \"cnt11\": {\"sum\":[\"1\",{\"get\":[\"cnt10\"]}]},\n"
                    + "      \"cnt12\": {\"sum\":[\"1\",{\"get\":[\"cnt11\"]}]},\n"
                    + "      \"cnt13\": {\"sum\":[\"1\",{\"get\":[\"cnt12\"]}]},\n"
                    + "      \"cnt14\": {\"sum\":[\"1\",{\"get\":[\"cnt13\"]}]},\n"
                    + "      \"cnt15\": {\"sum\":[\"1\",{\"get\":[\"cnt14\"]}]}\n"
                    + "    }\n"
                    + "  ]\n"
                    + '}';
            System.err.println("JSON: " + update);
            post.setEntity(new StringEntity(update));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);

            lucene.checkSearch(
                    "/search?prefix=9001&get=cnt01,cnt02,cnt03,cnt04,cnt05,cnt06"
                            + ",cnt07,cnt08,cnt09"
                            + ",cnt10,cnt11,cnt12,cnt13,cnt14,cnt15&text=keyword:*",
                    TestSearchBackend.prepareResult(
                            "\"cnt01\":\"2\","
                                    + "\"cnt02\":\"2\","
                                    + "\"cnt03\":\"2\","
                                    + "\"cnt04\":\"1\","
                                    + "\"cnt05\":\"1\","
                                    + "\"cnt06\":\"1\","
                                    + "\"cnt07\":\"1\","
                                    + "\"cnt08\":\"1\","
                                    + "\"cnt09\":\"1\","
                                    + "\"cnt10\":\"1\","
                                    + "\"cnt11\":\"1\","
                                    + "\"cnt12\":\"1\","
                                    + "\"cnt13\":\"1\","
                                    + "\"cnt14\":\"1\","
                                    + "\"cnt15\":\"1\""));
        } finally {
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testMapSetGet() throws Exception {
        File root =
            Files.createTempDirectory("testMapSetGet")
                .toFile();
        try (TestSearchBackend lucene =
            new TestSearchBackend(
                this,
                SearchBackendTestBase.config(
                    root,
                    "primary_key = keyword\n"
                    + "field.map.store=true\n")))
        {
            HttpPost post = new HttpPost(lucene.indexerUri() + "/update");
            String update = "{\n "
                + "  \"prefix\": 9001,\n "
                + "  \"AddIfNotExists\":true,\n "
                + "  \"docs\": [\n"
                + "    {\n"
                + "      \"keyword\":\"url\",\n"
                + "      \"map\":{\"map_set\":[{\"get\":[\"map\"]}"
                + "                            ,\"key1\",\"15\"]}\n"
                + "    }\n"
                + "  ]\n"
                + '}';
            System.err.println("JSON: " + update);
            post.setEntity(new StringEntity(update));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);

            lucene.checkSearch(
                "/search?prefix=9001&get=map&text=keyword:*",
                TestSearchBackend.prepareResult(
                    "\"map\":\"key1\t15\""));

            update = "{\n "
                + "  \"prefix\": 9001,\n "
                + "  \"AddIfNotExists\":true,\n "
                + "  \"docs\": [\n"
                + "    {\n"
                + "      \"keyword\":\"url\",\n"
                + "      \"map\":{"
                + "          \"map_set\":["
                + "              {\"get\":[\"map\"]},"
                + "              \"key2\","
                + "              {\"map_get\":[{\"get\":[\"map\"]},\"key1\"]}"
                + "          ]}\n"
                + "    }\n"
                + "  ]\n"
                + '}';
            System.err.println("JSON: " + update);
            post.setEntity(new StringEntity(update));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);

            lucene.checkSearch(
                "/search?prefix=9001&get=map&text=keyword:*",
                TestSearchBackend.prepareResult(
                    "\"map\":\"key1\t15\nkey2\t15\""));
        } finally {
            SearchBackendTestBase.removeDirectory(root);
        }
    }
    @Test
    public void testMakeSet() throws Exception {
        try (TestSearchBackend lucene = new TestSearchBackend(
                this,
                new File(
                    Paths.getSourcePath(
                        "mail/search/mail/search_backend_mail_config/files"
                        + "/search_backend_test.conf_mail"))))
        {
            HttpPost post = new HttpPost(lucene.indexerUri() + "/update");
            post.setEntity(
                new StringEntity(
                    "{\"prefix\":0,\"AddIfNotExists\":true,\"docs\":["
                    + "{\"url\":\"1\",\"senders_names\":"
                    + "{\"function\":\"make_set\",\"args\":[{\"function\":"
                    + "\"get\",\"args\":[\"senders_names\"]},"
                    + "\"Аркадий Голиков\"]}}]}",
                    StandardCharsets.UTF_8));
            // Add twice, result should stay same
            for (int i = 0; i <= 1; ++i) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);
                lucene.checkSearch(
                    "/search?prefix=0&get=senders_names&text=url:*",
                    TestSearchBackend.prepareResult(
                        "\"senders_names\":\"Аркадий Голиков\""));
            }

            post.setEntity(
                new StringEntity(
                    "{\"prefix\":0,\"AddIfNotExists\":true,\"docs\":["
                    + "{\"url\":\"1\",\"senders_names\":"
                    + "{\"function\":\"make_set\",\"args\":[{\"function\":"
                    + "\"get\",\"args\":[\"senders_names\"]},"
                    + "\"Аркадий Гайдар\"]}}]}",
                    StandardCharsets.UTF_8));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);
            // Same string shouldn't be added twice
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);

            lucene.checkSearch(
                "/search?prefix=0&get=senders_names&text=url:*",
                TestSearchBackend.prepareResult(
                    "\"senders_names\":"
                    + "\"Аркадий Гайдар\nАркадий Голиков\""));

            // Test proper speedhack behaviour on exact substrings
            post.setEntity(
                new StringEntity(
                    "{\"prefix\":0,\"AddIfNotExists\":true,\"docs\":["
                    + "{\"url\":\"1\",\"senders_names\":"
                    + "{\"function\":\"make_set\",\"args\":[{\"function\":"
                    + "\"get\",\"args\":[\"senders_names\"]},"
                    + "\"Гайдар\"]}}]}",
                    StandardCharsets.UTF_8));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);

            lucene.checkSearch(
                "/search?prefix=0&get=senders_names&text=url:*",
                TestSearchBackend.prepareResult(
                    "\"senders_names\":"
                    + "\"Гайдар\nАркадий Гайдар\nАркадий Голиков\""));

            // Test multi add and empty line skip
            post.setEntity(
                new StringEntity(
                    "{\"prefix\":0,\"AddIfNotExists\":true,\"docs\":["
                    + "{\"url\":\"1\",\"senders_names\":"
                    + "{\"function\":\"make_set\",\"args\":[{\"function\":"
                    + "\"get\",\"args\":[\"senders_names\"]},"
                    + "\"Гайдар\nГоликов\n\nАркадий Петрович Гайдар\"]}}]}",
                    StandardCharsets.UTF_8));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);

            lucene.checkSearch(
                "/search?prefix=0&get=senders_names&text=url:*",
                TestSearchBackend.prepareResult(
                    "\"senders_names\":"
                    + "\"Гайдар\nГоликов\nАркадий Петрович Гайдар\n"
                    + "Аркадий Гайдар\nАркадий Голиков\""));

            // Test adding of line which already present somewhere in the
            // middle of set
            post.setEntity(
                new StringEntity(
                    "{\"prefix\":0,\"AddIfNotExists\":true,\"docs\":["
                    + "{\"url\":\"1\",\"senders_names\":"
                    + "{\"function\":\"make_set\",\"args\":[{\"function\":"
                    + "\"get\",\"args\":[\"senders_names\"]},"
                    + "\"Гайдар\"]}}]}",
                    StandardCharsets.UTF_8));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);

            lucene.checkSearch(
                "/search?prefix=0&get=senders_names&text=url:*",
                TestSearchBackend.prepareResult(
                    "\"senders_names\":"
                    + "\"Гайдар\nГоликов\nАркадий Петрович Гайдар\n"
                    + "Аркадий Гайдар\nАркадий Голиков\""));

            // Test boundary condition for adding to the end of names list
            post.setEntity(
                new StringEntity(
                    "{\"prefix\":0,\"AddIfNotExists\":true,\"docs\":["
                    + "{\"url\":\"1\",\"senders_names\":"
                    + "{\"function\":\"make_set\",\"args\":[{\"function\":"
                    + "\"get\",\"args\":[\"senders_names\"]},"
                    + "\"Аркадий Петрович Гайда\"]}}]}",
                    StandardCharsets.UTF_8));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);

            lucene.checkSearch(
                "/search?prefix=0&get=senders_names&text=url:*",
                TestSearchBackend.prepareResult(
                    "\"senders_names\":"
                    + "\"Аркадий Петрович Гайда\nГайдар\nГоликов\n"
                    + "Аркадий Петрович Гайдар\n"
                    + "Аркадий Гайдар\nАркадий Голиков\""));

            // Test truncation by maxNames
            post.setEntity(
                new StringEntity(
                    "{\"prefix\":0,\"AddIfNotExists\":true,\"docs\":["
                    + "{\"url\":\"1\",\"senders_names\":"
                    + "{\"function\":\"make_set\",\"args\":[{\"function\":"
                    + "\"get\",\"args\":[\"senders_names\"]},"
                    + "\"Писатель\", \"3\"]}}]}",
                    StandardCharsets.UTF_8));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);

            lucene.checkSearch(
                "/search?prefix=0&get=senders_names&text=url:*",
                TestSearchBackend.prepareResult(
                    "\"senders_names\":"
                    + "\"Писатель\nАркадий Петрович Гайда\nГайдар\""));

            // Test input truncation by maxNames
            post.setEntity(
                new StringEntity(
                    "{\"prefix\":0,\"AddIfNotExists\":true,\"docs\":["
                    + "{\"url\":\"1\",\"senders_names\":"
                    + "{\"function\":\"make_set\",\"args\":[{\"function\":"
                    + "\"get\",\"args\":[\"senders_names\"]},"
                    + "\"Афтар\nЖжош\nПеши\nИсчо\", \"3\"]}}]}",
                    StandardCharsets.UTF_8));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);

            lucene.checkSearch(
                "/search?prefix=0&get=senders_names&text=url:*",
                TestSearchBackend.prepareResult(
                    "\"senders_names\":\"Афтар\nЖжош\nПеши\""));

            // try add empty lines to empty senders
            post.setEntity(
                new StringEntity(
                    "{\"prefix\":1,\"AddIfNotExists\":true,\"docs\":["
                    + "{\"url\":\"2\",\"senders_names\":"
                    + "{\"function\":\"make_set\",\"args\":[{\"function\":"
                    + "\"get\",\"args\":[\"senders_names\"]},"
                    + "\"\n\n\"]}}]}",
                    StandardCharsets.UTF_8));

            // Add twice, result should stay same
            for (int i = 0; i <= 1; ++i) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);
                lucene.checkSearch(
                    "/search?prefix=1&get=senders_names&text=__prefix:1",
                    TestSearchBackend.prepareResult("\"senders_names\":\"\""));
            }
        }
    }

    @Test
    public void testBooleanFunctions() throws Exception {
        try (TestSearchBackend lucene = new TestSearchBackend(
            this,
            new File(
                Paths.getSourcePath(
                    "mail/search/mail/search_backend_mail_config/files"
                    + "/search_backend_test.conf_mail"))))
        {
            HttpPost post = new HttpPost(lucene.indexerUri() + "/update");
            //test &lt
            String update = "{\n "
                + "  \"prefix\": 9001,\n "
                + "  \"AddIfNotExists\":true,\n "
                + "  \"docs\": [\n"
                + "    {\n"
                + "      \"url\": \"umtype_9001\",\n"
                + "      \"request_count\":{\"lt\":[1,2]},\n"
                + "      \"request_docs_count\":{\"lt\":[1,1]}}]\n"
                + '}';
            post.setEntity(new StringEntity(update));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);

            lucene.checkSearch(
                "/search?prefix=9001&get=request_count,request_docs_count"
                    + "&text=url:*",
                TestSearchBackend.prepareResult(
                    "\"request_count\":\"1\","
                        + "\"request_docs_count\":\"0\""));

            //test &le
            update = "{\n "
                + "  \"prefix\": 9001,\n "
                + "  \"AddIfNotExists\":true,\n "
                + "  \"docs\": [\n"
                + "    {\n"
                + "      \"url\": \"umtype_9001\",\n"
                + "      \"request_count\":{\"le\":[1,1]},\n"
                + "      \"request_docs_count\":{\"le\":[2,1]}}]\n"
                + '}';
            post.setEntity(new StringEntity(update));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);

            lucene.checkSearch(
                "/search?prefix=9001&get=request_count,request_docs_count"
                    + "&text=url:*",
                TestSearchBackend.prepareResult(
                    "\"request_count\":\"1\","
                        + "\"request_docs_count\":\"0\""));

            //test &gt
            update = "{\n "
                + "  \"prefix\": 9001,\n "
                + "  \"AddIfNotExists\":true,\n "
                + "  \"docs\": [\n"
                + "    {\n"
                + "      \"url\": \"umtype_9001\",\n"
                + "      \"request_count\":{\"gt\":[2,1]},\n"
                + "      \"request_docs_count\":{\"gt\":[1,1]}}]\n"
                + '}';
            post.setEntity(new StringEntity(update));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);

            lucene.checkSearch(
                "/search?prefix=9001&get=request_count,request_docs_count"
                    + "&text=url:*",
                TestSearchBackend.prepareResult(
                    "\"request_count\":\"1\","
                        + "\"request_docs_count\":\"0\""));

            //test &ge
            update = "{\n "
                + "  \"prefix\": 9001,\n "
                + "  \"AddIfNotExists\":true,\n "
                + "  \"docs\": [\n"
                + "    {\n"
                + "      \"url\": \"umtype_9001\",\n"
                + "      \"request_count\":{\"ge\":[1,1]},\n"
                + "      \"request_docs_count\":{\"ge\":[1,2]}}]\n"
                + '}';
            post.setEntity(new StringEntity(update));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);

            lucene.checkSearch(
                "/search?prefix=9001&get=request_count,request_docs_count"
                    + "&text=url:*",
                TestSearchBackend.prepareResult(
                    "\"request_count\":\"1\","
                        + "\"request_docs_count\":\"0\""));

            //test &eq
            update = "{\n "
                + "  \"prefix\": 9001,\n "
                + "  \"AddIfNotExists\":true,\n "
                + "  \"docs\": [\n"
                + "    {\n"
                + "      \"url\": \"umtype_9001\",\n"
                + "      \"request_count\":{\"eq\":[\"qwe\",\"qwe\"]},\n"
                + "      \"request_docs_count\":{\"eq\":[1,2]}}]\n"
                + '}';
            post.setEntity(new StringEntity(update));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);

            lucene.checkSearch(
                "/search?prefix=9001&get=request_count,request_docs_count"
                    + "&text=url:*",
                TestSearchBackend.prepareResult(
                    "\"request_count\":\"1\","
                        + "\"request_docs_count\":\"0\""));

            //test &and
            update = "{\n "
                + "  \"prefix\": 9001,\n "
                + "  \"AddIfNotExists\":true,\n "
                + "  \"docs\": [\n"
                + "    {\n"
                + "      \"url\": \"umtype_9001\",\n"
                + "      \"request_count\":{\"and\":[\"true\",\"true\",1]},\n"
                + "      \"request_docs_count\":{\"and\":[0,\"true\"]}}]\n"
                + '}';
            post.setEntity(new StringEntity(update));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);

            lucene.checkSearch(
                "/search?prefix=9001&get=request_count,request_docs_count"
                    + "&text=url:*",
                TestSearchBackend.prepareResult(
                    "\"request_count\":\"1\","
                        + "\"request_docs_count\":\"0\""));

            //test &or
            update = "{\n "
                + "  \"prefix\": 9001,\n "
                + "  \"AddIfNotExists\":true,\n "
                + "  \"docs\": [\n"
                + "    {\n"
                + "      \"url\": \"umtype_9001\",\n"
                + "      \"request_count\":{\"or\":[\"true\",\"false\",1]},\n"
                + "      \"request_docs_count\":{\"or\":[0,\"false\"]}}]\n"
                + '}';
            post.setEntity(new StringEntity(update));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);

            lucene.checkSearch(
                "/search?prefix=9001&get=request_count,request_docs_count"
                    + "&text=url:*",
                TestSearchBackend.prepareResult(
                    "\"request_count\":\"1\","
                        + "\"request_docs_count\":\"0\""));

            //test &xor
            update = "{\n "
                + "  \"prefix\": 9001,\n "
                + "  \"AddIfNotExists\":true,\n "
                + "  \"docs\": [\n"
                + "    {\n"
                + "      \"url\": \"umtype_9001\",\n"
                + "      \"request_count\":{\"xor\":[\"true\",\"false\",0]},\n"
                + "      \"request_docs_count\":{\"xor\":[0,\"false\"]}}]\n"
                + '}';
            post.setEntity(new StringEntity(update));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);

            lucene.checkSearch(
                "/search?prefix=9001&get=request_count,request_docs_count"
                    + "&text=url:*",
                TestSearchBackend.prepareResult(
                    "\"request_count\":\"1\","
                        + "\"request_docs_count\":\"0\""));

            //test &not
            update = "{\n "
                + "  \"prefix\": 9001,\n "
                + "  \"AddIfNotExists\":true,\n "
                + "  \"docs\": [\n"
                + "    {\n"
                + "      \"url\": \"umtype_9001\",\n"
                + "      \"request_count\":{\"not\":[\"false\"]},\n"
                + "      \"request_docs_count\":{\"not\":[1]}}]\n"
                + '}';
            post.setEntity(new StringEntity(update));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);

            lucene.checkSearch(
                "/search?prefix=9001&get=request_count,request_docs_count"
                    + "&text=url:*",
                TestSearchBackend.prepareResult(
                    "\"request_count\":\"1\","
                        + "\"request_docs_count\":\"0\""));
        }
    }

    @Test
    public void testConditionalIncrementFunction() throws Exception {
        try (TestSearchBackend lucene = new TestSearchBackend(
            this,
            new File(
                Paths.getSourcePath(
                    "mail/search/mail/search_backend_mail_config/files"
                    + "/search_backend_test.conf_mail"))))
        {
            HttpPost post = new HttpPost(lucene.indexerUri() + "/update");
            //test don't increment
            String update = "{\n "
                + "  \"prefix\": 9001,\n "
                + "  \"AddIfNotExists\":true,\n "
                + "  \"docs\": [\n"
                + "    {\n"
                + "      \"url\": \"umtype_9001\",\n"
                + "      \"request_count\":2,\n"
                + "      \"request_docs_count\":{\"inc\":[1,{\"lt\":[1,1]}]}}]\n"
                + '}';
            post.setEntity(new StringEntity(update));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);
            lucene.checkSearch(
                "/search?prefix=9001&get=request_count,request_docs_count"
                    + "&text=url:*",
                TestSearchBackend.prepareResult(
                    "\"request_count\":\"2\","
                        + "\"request_docs_count\":\"0\""));

            //test increment
            update = "{\n "
                + "  \"prefix\": 9001,\n "
                + "  \"AddIfNotExists\":true,\n "
                + "  \"docs\": [\n"
                + "    {\n"
                + "      \"url\": \"umtype_9001\",\n"
                + "      \"request_count\":2,\n"
                + "      \"request_docs_count\":{\"inc\":[1,{\"lt\":[1,2]}]}}]\n"
                + '}';
            post.setEntity(new StringEntity(update));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);
            lucene.checkSearch(
                "/search?prefix=9001&get=request_count,request_docs_count"
                    + "&text=url:*",
                TestSearchBackend.prepareResult(
                    "\"request_count\":\"2\","
                        + "\"request_docs_count\":\"1\""));

            //test complex don't increment
            update = "{\n "
                + "  \"prefix\": 9001,\n "
                + "  \"AddIfNotExists\":true,\n "
                + "  \"docs\": [\n"
                + "    {\n"
                + "      \"url\": \"umtype_9001\",\n"
                + "      \"request_count\":2,\n"
                + "      \"request_docs_count\":{\"inc\":["
                + "             1,"
                + "             {\"and\":[{\"lt\":["
                + "             4,"
                + "             {\"get\":[\"request_count\"]}]}]}"
                + "     ]}}]\n"
                + '}';
            post.setEntity(new StringEntity(update));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);
            lucene.checkSearch(
                "/search?prefix=9001&get=request_count,request_docs_count"
                    + "&text=url:*",
                TestSearchBackend.prepareResult(
                    "\"request_count\":\"2\","
                        + "\"request_docs_count\":\"1\""));
        }
    }

    @Test
    public void testUpdateOnFieldConfigChange() throws Exception {
        Path root = Files.createTempDirectory(testName.getMethodName());
        try (GenericAutoCloseableHolder indexDirHolder =
            new GenericAutoCloseableHolder<>())
        {
            final String noAnalyzedField =
                "primary_key = keyword\n"
                + "field.lf.tokenizer = lf\n"
                + "field.lf.store = true\n"
                + "field.lf.attribute = true\n";
            final String analyzedField =
                noAnalyzedField
                + "field.lf.analyze = true\n";
            try (TestSearchBackend lucene =
                    new TestSearchBackend(
                        root,
                        SearchBackendTestBase.config("", noAnalyzedField, 1)))
            {
                indexDirHolder.reset(lucene.releaseIndex());
                HttpPost post = new HttpPost(lucene.indexerUri() + "/update");
                //test don't increment
                String update = "{\n "
                    + "  \"prefix\": 9002,\n "
                    + "  \"AddIfNotExists\":true,\n "
                    + "  \"docs\": [\n"
                    + "    {\n"
                    + "      \"keyword\": \"lftest_9002\",\n"
                    + "      \"lf\":\"token1\\ntoken2\"\n"
                    + "    }]\n"
                    + '}';
                post.setEntity(new StringEntity(update));
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);
                lucene.checkSearch(
                    "/search?prefix=9002&get=keyword,lf"
                        + "&text=lf:token1",
                    TestSearchBackend.prepareResult());

                lucene.checkSearch(
                    "/search?prefix=9002&get=keyword,lf"
                        + "&text=lf:token1*",
                    TestSearchBackend.prepareResult(
                        "\"keyword\":\"lftest_9002\","
                        + "\"lf\":\"token1\\ntoken2\""));
            }

            try (TestSearchBackend lucene =
                    new TestSearchBackend(
                        root,
                        SearchBackendTestBase.config("", analyzedField, 1)))
            {
                indexDirHolder.reset(lucene.releaseIndex());
                HttpPost post = new HttpPost(lucene.indexerUri() + "/update");
                //test don't increment
                String update = "{\n "
                    + "  \"prefix\": 9002,\n "
                    + "  \"docs\": [\n"
                    + "    {\n"
                    + "      \"keyword\": \"lftest_9002\"\n"
                    + "    }]\n"
                    + '}';
                post.setEntity(new StringEntity(update));
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);
                lucene.checkSearch(
                    "/search?prefix=9002&get=keyword,lf"
                        + "&text=lf:token2",
                    TestSearchBackend.prepareResult(
                        "\"keyword\":\"lftest_9002\","
                        + "\"lf\":\"token1\\ntoken2\""));

                lucene.checkSearch(
                    "/search?prefix=9002&get=keyword,lf"
                        + "&text=lf:token1",
                    TestSearchBackend.prepareResult(
                        "\"keyword\":\"lftest_9002\","
                        + "\"lf\":\"token1\\ntoken2\""));
            }
        }
    }

    @Test
    public void testNormalizeIpQueue() throws Exception {
        try (TestSearchBackend lucene = new TestSearchBackend(
            this,
            new File(
                Paths.getSourcePath(
                    "mail/search/mail/search_backend_mail_config/files"
                    + "/search_backend_test.conf_mail"))))
        {
            lucene.add(
                "\"url\":1,\"mimetype\":{\"function\":\"normalize_ip\","
                + "\"args\":[\"8.8.8.8\"]}",
                "\"url\":2,\"mimetype\":{\"function\":\"normalize_ip\","
                + "\"args\":[\"4.8\"]}",
                "\"url\":3,\"mimetype\":{\"function\":\"normalize_ip\","
                + "\"args\":[\"::ffff:84.76.24.205\"]}",
                "\"url\":4,\"mimetype\":{\"function\":\"normalize_ip\","
                + "\"args\":[\"2a02:6b8:0:3400::812\"]}");

            lucene.checkSearch(
                "/search?prefix=0&get=mimetype&text=mimetype:8.8.8.8",
                TestSearchBackend.prepareResult("\"mimetype\":\"8.8.8.8\""));

            lucene.checkSearch(
                "/search?prefix=0&get=mimetype&text=mimetype:4.0.0.8",
                TestSearchBackend.prepareResult("\"mimetype\":\"4.0.0.8\""));

            lucene.checkSearch(
                "/search?prefix=0&get=mimetype&text=mimetype:84.76.24.205",
                TestSearchBackend.prepareResult("\"mimetype\":\"84.76.24.205\""));

            lucene.checkSearch(
                "/search?prefix=0&get=mimetype"
                + "&text=mimetype:%222a02:6b8:0:3400:0:0:0:812%22",
                TestSearchBackend.prepareResult(
                    "\"mimetype\":\"2a02:6b8:0:3400:0:0:0:812\""));
        }
    }

    @Test
    public void testIgnoreBinaryData() throws Exception {
        String binaryWithText = "needle�����─����������������������������������"
            + "single_pulse������������������������������������������������";
        try (TestSearchBackend lucene = new TestSearchBackend(
            this,
            new File(
                Paths.getSourcePath(
                    "mail/search/mail/search_backend_mail_config/files"
                    + "/search_backend_test.conf_mail"))))
        {
            lucene.add(
                "\"url\":1,\"body_text\":\"" + binaryWithText+ "\"");

            lucene.checkSearch(
                "/search?prefix=0&get=url&text=body_text:needle",
                TestSearchBackend.prepareResult());
        }
    }

    @Test
    public void testIndexDocProcessor() throws Exception {
        try (TestSearchBackend lucene = new TestSearchBackend(
            this,
            new File(
                Paths.getSourcePath(
                    "mail/search/mail/search_backend_mail_config/files"
                    + "/search_backend_test.conf_mail"))))
        {
            lucene.add("\"url\":\"doc_type1_1\",\"mimetype\":\"image\"");
            HttpPost post = new HttpPost(lucene.indexerUri() + "/add");

            StringBuilder sb = new StringBuilder("{\"prefix\":\"");
            sb.append(new LongPrefix(0L));
            //sb.append("\",\"AddIfNotExists\":\"true");
            sb.append(
                "\",\"processor\":{\"pkjoin\":{\"query\":{\"function\":\"get\","
                    + "\"args\": [\"hdr_subject\"]},\"get\":[\"mimetype\"]}},");
            sb.append("\"docs\":");
            sb.append(TestSearchBackend.concatDocs(
                "\"url\":\"doc_type2_2\", \"hdr_subject\": \"doc_type1_1\""));
            sb.append('}');

            post.setEntity(
                new StringEntity(
                    new String(sb),
                    ContentType.APPLICATION_JSON));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);

            lucene.checkSearch(
                "/search?prefix=0&text=url:doc_type2_2&get=*",
                new JsonChecker(
                    "{\"hitsCount\":1,"
                        + "\"hitsArray\":[{\"url\":\"doc_type2_2\", "
                        + "\"hdr_subject\": \"doc_type1_1\","
                        + " \"mimetype\": \"image\"}]}"));

            lucene.add("\"url\":\"doc_type3_3\",\"mimetype\":\"video\"");
            sb = new StringBuilder("{\"prefix\":\"");
            sb.append(new LongPrefix(0L));
            //sb.append("\",\"AddIfNotExists\":\"true");
            lucene.flush();
            Thread.sleep(500L);
            sb.append(
                "\",\"processor\":{\"pkjoin\":{\"query\":{\"function\":\"get\","
                    + "\"args\": [\"hdr_subject\"]},\"get\":[\"mimetype\"]}},");
            sb.append("\"docs\":");
            sb.append(TestSearchBackend.concatDocs(
                "\"url\":\"doc_type2_2\", \"hdr_subject\": \"doc_type3_3\""));
            sb.append('}');

            post = new HttpPost(lucene.indexerUri() + "/update");
            post.setEntity(
                new StringEntity(
                    new String(sb),
                    ContentType.APPLICATION_JSON));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);

            lucene.checkSearch(
                "/search?prefix=0&text=url:doc_type2_2&get=*",
                new JsonChecker(
                    "{\"hitsCount\":1,"
                        + "\"hitsArray\":[{\"url\":\"doc_type2_2\", "
                        + "\"hdr_subject\": \"doc_type3_3\","
                        + " \"mimetype\": \"video\"}]}"));
        }
    }

    @Test
    public void testDynamicConfig() throws Exception {
        Path root = Files.createTempDirectory(testName.getMethodName());
        File dynamicConfigFile = new File(root.toFile(), "dynamic_config.conf");

        String dynamicField = "\n[field.dynamic_field]\n"
            + "tokenizer = keyword\n"
            + "filters = padding:10\n"
            + "prefixed = true\n"
            + "store = true\n"
            + "analyze = true\n"
            + "attribute = true\n";
        Files.writeString(dynamicConfigFile.toPath(), dynamicField, StandardOpenOption.CREATE_NEW);

        String suffix = "prefix_type = long\n" +
            "primary_key = id\n" +
            "dynamic_fields_dir = " + root.toAbsolutePath() +
            "\n\n[field.id]\n"
            + "tokenizer = keyword\n"
            + "store = true\n"
            + "analyze = true\n"
            + "attribute = true\n";
        try (TestSearchBackend lucene =
                 new TestSearchBackend(
                     root,
                     SearchBackendTestBase.config("", suffix, 1)))
        {
            lucene.add("\"id\":\"doc_0_1\",\"keyword\":\"value\", \"dynamic_field\": \"ya dynamic!\"");
            lucene.checkSearch(
                "/search?prefix=0&req=1&text=id:doc_0_1&get=*",
                new JsonChecker(
                    "{\"hitsCount\":1,"
                        + "\"hitsArray\":[{\"id\":\"doc_0_1\", "
                        + "\"keyword\": \"value\","
                        + " \"dynamic_field\": \"ya dynamic!\"}]}"));

            String update =
                "\n[field.new_dynamic_field]\n"
                    + "tokenizer = keyword\n"
                    + "filters = padding:10\n"
                    + "prefixed = true\n"
                    + "store = true\n"
                    + "analyze = true\n"
                    + "attribute = true\n";
            HttpPost post = new HttpPost(lucene.indexerUri() + "/schema_update");
            post.setEntity(new StringEntity(update, StandardCharsets.UTF_8));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);
            lucene.add("\"id\":\"doc_0_2\",\"keyword\":\"value2\", \"new_dynamic_field\": \"ya super dynamic!\"");
            String expected = "{\"hitsCount\":1,"
                + "\"hitsArray\":[{\"id\":\"doc_0_2\", "
                + "\"keyword\": \"value2\","
                + " \"new_dynamic_field\": \"ya super dynamic!\"}]}";

            lucene.checkSearch(
                "/search?prefix=0&req=2&text=id:doc_0_2&get=*",
                new JsonChecker(expected));
            lucene.flush();

            lucene.checkSearch(
                "/search?prefix=0&req=3&text=id:doc_0_2&get=*",
                new JsonChecker(expected));

            // now remove

            String updateSchema2 =
                "\n[field.new_dynamic_field_2]\n"
                    + "tokenizer = keyword\n"
                    + "filters = padding:10\n"
                    + "prefixed = true\n"
                    + "store = true\n"
                    + "analyze = true\n"
                    + "attribute = true\n";
            post = new HttpPost(lucene.indexerUri() + "/schema_update");
            post.setEntity(new StringEntity(updateSchema2, StandardCharsets.UTF_8));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);

            expected = "{\"hitsCount\":1,"
                + "\"hitsArray\":[{\"id\":\"doc_0_2\", "
                + "\"keyword\": \"value2\"}]}";
            lucene.checkSearch(
                "/search?prefix=0&req=4&text=id:doc_0_2&get=*",
                new JsonChecker(expected));
            lucene.flush();
            lucene.checkSearch(
                "/search?prefix=0&req=5&text=id:doc_0_2&get=*",
                new JsonChecker(expected));

            lucene.update("\"id\":\"doc_0_2\",\"keyword\":\"value2\", \"new_dynamic_field_2\": \"ya2\", \"new_dynamic_field\": \"ya\"");
            lucene.flush();
            BlockCompressedInputStreamBase.freeCache();
            expected = "{\"hitsCount\":1,"
                + "\"hitsArray\":[{\"id\":\"doc_0_2\", "
                + "\"keyword\": \"value2\","
                + " \"new_dynamic_field_2\": \"ya2\"}]}";

            lucene.checkSearch(
                "/search?prefix=0&req=6&text=id:doc_0_2&get=*",
                new JsonChecker(expected));
        }
    }

    @Test
    public void testMultipleDatabases() throws Exception {
        System.err.println( "testMailSearch testcase started" );
        File root = Files.createTempDirectory("testMultipleDatabases").toFile();
        File firstDbPath = new File(root, "first");
        firstDbPath.deleteOnExit();
        Files.createDirectory(firstDbPath.toPath());
        File secondDbPath = new File(root, "second");
        secondDbPath.deleteOnExit();
        Files.createDirectory(secondDbPath.toPath());

        String config = "http.port = 0\n"
            + "http.timeout = 10000\n"
            + "http.connections = 1000\n"
            + "http.workers.min = 20\n"
            + "search.port = 0\n"
            + "search.timeout = 10000\n"
            + "search.connections = 1000\n"
            + "search.workers.min = 20\n"
            + "indexer.port = 0\n"
            + "indexer.timeout = 10000\n"
            + "indexer.connections = 1000\n"
            + "indexer.workers.min = 20\n"
            + "drop-password= ugu\n"
            + "index_threads = 20\n"
            + "xurls_regex_file = "
            + Paths.getSourcePath(
            "mail/search/mail/search_backend_mail_config/files"
                + "/search_backend_xurls_patterns")
            + "\nfull_log.level.min = all\n"
            + "database.first.shards = 1"
            + "\ndatabase.first.use_journal = 1\n"
            + "database.first.yandex_codec.terms_writer_block_size = 8192\n"
            + "database.first.yandex_codec.group_field = __prefix\n"
            + "database.first.yandex_codec.fields_writer_buffer_size = 6144\n"
            + "database.first.index_path = " + firstDbPath.toString() + '\n'
            + "\ndatabase.first.primary_key = keyword\n"
            + "database.first.field.keyword.tokenizer = keyword\n"
            + "database.first.field.keyword.store = true\n"
            + "database.first.field.keyword.prefixed = true\n"
            + "database.first.field.keyword.analyze = true\n"
            + "database.first.field.attribute.tokenizer = whitespace\n"
            + "database.first.field.attribute.filters = lowercase\n"
            + "database.first.field.attribute.prefixed = false\n"
            + "database.first.field.attribute.attribute = true\n"
            + "database.first.field.attribute.analyze = true\n"
            + "database.first.field.attribute.store = true\n"

            + "database.second.shards = 1"
            + "\ndatabase.second.use_journal = 1\n"
            + "database.second.yandex_codec.terms_writer_block_size = 8192\n"
            + "database.second.yandex_codec.group_field = __prefix\n"
            + "database.second.yandex_codec.fields_writer_buffer_size = 6144\n"
            + "database.second.index_path = " + secondDbPath.toString() + '\n'
            + "\ndatabase.second.primary_key = pksecond\n"
            + "database.second.field.keyword.tokenizer = keyword\n"
            + "database.second.field.keyword.store = false\n"
            + "database.second.field.keyword.prefixed = false\n"
            + "database.second.field.keyword.analyze = true\n"
            + "database.second.field.pksecond.tokenizer = keyword\n"
            + "database.second.field.pksecond.prefixed = false\n"
            + "database.second.field.pksecond.attribute = true\n"
            + "database.second.field.pksecond.analyze = true\n"
            + "database.second.field.pksecond.store = true\n";

        String add1 = "{\"prefix\":1,\"docs\":[{"
            + "\"keyword\":\"keyword1\",\"attribute\":\"attribute1\", \"pksecond\": \"pksecond1\"},{"
            + "\"keyword\":\"keyword2\",\"attribute\":\"attribute2\", \"pksecond\": \"pksecond2\"},{"
            + "\"keyword\":\"keyword3\",\"attribute\":\"attribute3\", \"pksecond\": \"pksecond3\"}]}";
        try {
            try (Daemon daemon = new Daemon(new Config(new IniConfig(new StringReader(config))));
                 CloseableHttpClient client = HttpClients.createDefault())
            {
                HttpPost post = new HttpPost("http://localhost:"
                    + daemon.jsonServerPort() + "/add?db=first");
                post.setEntity(new StringEntity(add1, StandardCharsets.UTF_8));
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);

                post = new HttpPost("http://localhost:"
                    + daemon.jsonServerPort() + "/add?db=second");
                post.setEntity(new StringEntity(add1, StandardCharsets.UTF_8));
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);

                HttpAssert.assertJsonResponse(client, "http://localhost:"
                    + daemon.searchServerPort() + "/search?text=keyword:keyword1&prefix=1&"
                    + "get=*&db=first", "{\"hitsCount\":1, \"hitsArray\":[" +
                    "{\"attribute\": \"attribute1\",\n" +
                    "\"keyword\": \"keyword1\"}]}");
                HttpAssert.assertJsonResponse(client, "http://localhost:"
                    + daemon.searchServerPort() + "/search?text=keyword:keyword1&"
                    + "get=*&db=second", "{\"hitsCount\":1, \"hitsArray\":[" +
                    "{\"pksecond\": \"pksecond1\"}]}");

                HttpAssert.assertJsonResponse(client, "http://localhost:"
                    + daemon.searchServerPort() + "/printkeys?print-freqs&"
                    + "field=keyword&db=second&hr", "{" +
                    "    \"keyword1\": {\n" +
                    "        \"freq\": 1\n" +
                    "    },\n" +
                    "    \"keyword2\": {\n" +
                    "        \"freq\": 1\n" +
                    "    },\n" +
                    "    \"keyword3\": {\n" +
                    "        \"freq\": 1\n" +
                    "    }}");

                HttpAssert.assertJsonResponse(client, "http://localhost:"
                    + daemon.searchServerPort() + "/printkeys?print-freqs&"
                    + "field=keyword&db=first&hr", "{" +
                    "    \"1#keyword1\": {\n" +
                    "        \"freq\": 1\n" +
                    "    },\n" +
                    "    \"1#keyword2\": {\n" +
                    "        \"freq\": 1\n" +
                    "    },\n" +
                    "    \"1#keyword3\": {\n" +
                    "        \"freq\": 1\n" +
                    "    }}");
            }
        } finally {
            System.err.println( "testMultipleDatabases testcase stopped" );
            System.gc();
            SearchBackendTestBase.removeDirectory(root);
        }
    }
}

