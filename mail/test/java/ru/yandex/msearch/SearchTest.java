package ru.yandex.msearch;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.http.HttpHeaders;
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
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.devtools.test.Paths;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.http.util.YandexHeaders;
import ru.yandex.json.dom.JsonList;
import ru.yandex.json.dom.JsonMap;
import ru.yandex.json.dom.JsonObject;
import ru.yandex.json.dom.TypesafeValueContentHandler;
import ru.yandex.search.prefix.LongPrefix;
import ru.yandex.test.search.backend.TestSearchBackend;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.StringChecker;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;
import ru.yandex.util.string.StringUtils;

public class SearchTest extends TestBase {
    private static final int DISK_UNISTAT_MAX_SIGNALS = 2000;
    private static final int BIG_FIELD_SIZE = 1000;
    private static final String NUMBER_CONFIG =
        "[field.number]\n"
        + "tokenizer = keyword\n"
        + "filters = padding:3\n"
        + "analyze = true\n"
        + "attribute = true\n"
        + "store = true\n"
        + "[field.float-array]\n"
        + "tokenizer = boolean\n"
        + "analyze = true\n"
        + "attribute = true\n"
        + "store = true\n"
        + "type = float-array\n"
        + "[field.byte-array]\n"
        + "tokenizer = boolean\n"
        + "analyze = true\n"
        + "attribute = true\n"
        + "store = true\n"
        + "type = byte-array\n"
        + "[field.lat]\n"
        + "tokenizer = keyword\n"
        + "analyze = true\n"
        + "attribute = true\n"
        + "store = true\n"
        + "filters = aaz:90|maf:1000000|padding:10\n"
        + "type = float\n"
        + "[field.lon]\n"
        + "tokenizer = keyword\n"
        + "analyze = true\n"
        + "attribute = true\n"
        + "store = true\n"
        + "filters = aaz:180|maf:1000000|padding:10\n"
        + "type = float\n";

    private static final String NUMBER_CONFIG_CACHE =
        "[field.number]\n"
        + "tokenizer = keyword\n"
        + "filters = padding:3\n"
        + "analyze = true\n"
        + "attribute = true\n"
        + "store = true\n"
        + "[field.float-array]\n"
        + "tokenizer = boolean\n"
        + "analyze = true\n"
        + "attribute = true\n"
        + "store = true\n"
        + "type = float-array\n"
        + "[field.byte-array]\n"
        + "tokenizer = boolean\n"
        + "analyze = true\n"
        + "attribute = true\n"
        + "store = true\n"
        + "cache = raw\n"
        + "type = byte-array\n";

    static {
        System.setProperty(
            "LUCENE_DISK_CONFIG_INCLUDE",
            "search_backend_thin.conf");
    }

    private static String mkFunc(final String name, final String... args) {
        return "{\"function\":\"" + name + "\", \"args\":[" +
            StringUtils.join(args, ',') + "]}";
    }

    @Test
    public void testIntervalSearch() throws Exception {
        File root = Files.createTempDirectory("testIntervalSearch").toFile();
        try (Daemon daemon =
                new Daemon(SearchBackendTestBase.config(root, NUMBER_CONFIG));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                + "\"keyword\":\"first\",\"number\":9},{"
                + "\"keyword\":\"second\",\"number\":10},{"
                + "\"keyword\":\"third\",\"number\":\"000090\"},{"
                + "\"keyword\":\"forth\",\"number\":\"0102\"}]}",
                StandardCharsets.UTF_8));
            HttpResponse response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()).trim(),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&text="
                + "number:[10%20TO%2092]&get=keyword&sort=keyword&asc"));
            String text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":2,\"hitsArray\":[{"
                    + "\"keyword\":\"second\"},{\"keyword\":\"third\"}]}"),
                text);
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&text="
                + "number:[0%20TO%20101]&get=keyword&sort=keyword&asc"));
            text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":3,\"hitsArray\":[{"
                    + "\"keyword\":\"first\"},{"
                    + "\"keyword\":\"second\"},{\"keyword\":\"third\"}]}"),
                text);
        } finally {
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testIntervalSearchPrefixed() throws Exception {
        File root =
            Files.createTempDirectory("testIntervalSearchPrefixed").toFile();
        try (Daemon daemon = new Daemon(SearchBackendTestBase.config(
                root, NUMBER_CONFIG + "prefixed = true\n"));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                + "\"keyword\":\"first\",\"number\":\"0000\"},{"
                + "\"keyword\":\"second\",\"number\":5},{"
                + "\"keyword\":\"third\",\"number\":12},{"
                + "\"keyword\":\"forth\",\"number\":102}]}",
                StandardCharsets.UTF_8));
            HttpResponse response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()).trim(),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&text="
                + "number:[3%20TO%2020]&get=keyword&sort=keyword&asc"));
            String text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":2,\"hitsArray\":[{"
                    + "\"keyword\":\"second\"},{\"keyword\":\"third\"}]}"),
                text);
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&text="
                + "number:[9%20TO%2010000]&get=keyword&sort=keyword&asc"));
            text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":2,\"hitsArray\":[{"
                    + "\"keyword\":\"forth\"},{\"keyword\":\"third\"}]}"),
                text);
        } finally {
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testIntervalSignedSearch() throws Exception {
        File root =
            Files.createTempDirectory("testIntervalSignedSearch").toFile();
        try (Daemon daemon = new Daemon(SearchBackendTestBase.config(
                root,
                NUMBER_CONFIG.replace("padding", "aaz:100|daf:1|padding")
                + "prefixed = true\n"));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                + "\"keyword\":\"first\",\"number\":\"-4\"},{"
                + "\"keyword\":\"second\",\"number\":1},{"
                + "\"keyword\":\"third\",\"number\":-2},{"
                + "\"keyword\":\"forth\",\"number\":10}]}",
                StandardCharsets.UTF_8));
            HttpResponse response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()).trim(),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&text="
                + "number:[-3%20TO%202]&get=keyword&sort=keyword&asc"));
            String text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":2,\"hitsArray\":[{"
                    + "\"keyword\":\"second\"},{\"keyword\":\"third\"}]}"),
                text);
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&text="
                + "number:[-999999%20TO%200]&get=keyword&sort=keyword"));
            text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":2,\"hitsArray\":[{"
                    + "\"keyword\":\"third\"},{\"keyword\":\"first\"}]}"),
                text);
        } finally {
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testExactMatch() throws Exception {
        File root = Files.createTempDirectory("testExactMatch").toFile();
        try (Daemon daemon = new Daemon(SearchBackendTestBase.config(root));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                + "\"keyword\":\"1\",\"text\":\"мир\"},{"
                + "\"keyword\":\"2\",\"text\":\"мира\"},{"
                + "\"keyword\":\"3\",\"text\":\"миры\"}]}",
                StandardCharsets.UTF_8));
            HttpResponse response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()).trim(),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&text="
                + "text:мир&get=keyword&sort=keyword&asc"));
            String text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":3,\"hitsArray\":[{"
                    + "\"keyword\":\"1\"},{\"keyword\":\"2\"},{"
                    + "\"keyword\":\"3\"}]}"),
                text);
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&text="
                + "text%3A%22мир%22&get=keyword&sort=keyword&asc"));
            text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":1,\"hitsArray\":[{"
                    + "\"keyword\":\"1\"}]}"),
                text);
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&text="
                + "text%3A%22мира%22&get=keyword&sort=keyword&asc"));
            text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":1,\"hitsArray\":[{"
                    + "\"keyword\":\"2\"}]}"),
                text);
        } finally {
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testPhraseMatch() throws Exception {
        File root = Files.createTempDirectory("testPhraseMatch").toFile();
        try (Daemon daemon = new Daemon(SearchBackendTestBase.config(root));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                + "\"keyword\":\"1\",\"text\":\"дивный мир\"},{"
                + "\"keyword\":\"2\",\"text\":\"дивный новый мир\"},{"
                + "\"keyword\":\"3\",\"text\":\"новый мира\"},{"
                + "\"keyword\":\"4\",\"text\":\"дивный новый что ли мир\"}"
                + ",{\"keyword\":\"5\",\"text\":\"123 456\"}"
                + ",{\"keyword\":\"6\",\"text\":\"456 123\"}"
                + "]}", StandardCharsets.UTF_8));
            HttpResponse response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()).trim(),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&text="
                + "text:%22дивный+мир%22&get=keyword&sort=keyword&asc"));
            String text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":1,\"hitsArray\":[{"
                    + "\"keyword\":\"1\"}]}"),
                text);
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&text="
                + "text:%22дивный+мир%22%7E2&get=keyword&sort=keyword"));
            text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":2,\"hitsArray\":[{"
                    + "\"keyword\":\"2\"},{\"keyword\":\"1\"}]}"),
                text);
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&text="
                + "text%3A%22новый+мир%22&get=keyword&sort=keyword&asc"));
            text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":1,\"hitsArray\":[{"
                    + "\"keyword\":\"2\"}]}"),
                text);
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&text="
                + "text%3A123.456&get=keyword&sort=keyword&asc"));
            text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":1,\"hitsArray\":[{"
                    + "\"keyword\":\"5\"}]}"),
                text);
        } finally {
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testPhraseMatchExtended() throws Exception {
        File root = Files.createTempDirectory("testPhraseMatchExtended").toFile();
        final Config config = SearchBackendTestBase.config(root,
                "primary_key = keyword");
        try (Daemon daemon = new Daemon(config);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/modify");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                + "\"keyword\":\"1\",\"text\":\"дивный 1 мир\"},{"
                + "\"keyword\":\"2\",\"text\":\"дивный 1 новый 1 мир\"},{"
                + "\"keyword\":\"3\",\"text\":\"новый 1 мира\"},{"
                + "\"keyword\":\"4\",\"text\":\"дивный 1 новый 1 что ли мир\"}"
                + ",{\"keyword\":\"5\",\"text\":\"123 456\"}"
                + ",{\"keyword\":\"6\",\"text\":\"456 123\"}"
                + "]}", StandardCharsets.UTF_8));

            HttpResponse response;
            //populate all indexer threadLocals with /add/delete/add documents
            //with +1 positions
            for (int i = 0; i < config.indexThreads() * 4; i++) {
                response = client.execute(post);
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: "
                    + EntityUtils.toString(response.getEntity()).trim(),
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
            }
            //then index normal positions
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                + "\"keyword\":\"1\",\"text\":\"дивный мир\"},{"
                + "\"keyword\":\"2\",\"text\":\"дивный новый мир\"},{"
                + "\"keyword\":\"3\",\"text\":\"новый мира\"},{"
                + "\"keyword\":\"4\",\"text\":\"дивный новый что ли мир\"}"
                + ",{\"keyword\":\"5\",\"text\":\"123 456\"}"
                + ",{\"keyword\":\"6\",\"text\":\"456 123\"}"
                + "]}", StandardCharsets.UTF_8));
            response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()).trim(),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());

            //test as usual
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&text="
                + "text:%22дивный+мир%22&get=keyword&sort=keyword&asc"));
            String text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":1,\"hitsArray\":[{"
                    + "\"keyword\":\"1\"}]}"),
                text);
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&text="
                + "text:%22дивный+мир%22%7E2&get=keyword&sort=keyword"));
            text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":2,\"hitsArray\":[{"
                    + "\"keyword\":\"2\"},{\"keyword\":\"1\"}]}"),
                text);
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&text="
                + "text%3A%22новый+мир%22&get=keyword&sort=keyword&asc"));
            text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":1,\"hitsArray\":[{"
                    + "\"keyword\":\"2\"}]}"),
                text);
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&text="
                + "text%3A123.456&get=keyword&sort=keyword&asc"));
            text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":1,\"hitsArray\":[{"
                    + "\"keyword\":\"5\"}]}"),
                text);
        } finally {
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testSloppyMatch() throws Exception {
        File root = Files.createTempDirectory("testSloppyMatch").toFile();
        try (Daemon daemon = new Daemon(SearchBackendTestBase.config(root));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                + "\"keyword\":\"1\",\"text\":\"дивный мир\"},{"
                + "\"keyword\":\"2\",\"text\":\"дивный новый мир\"},{"
                + "\"keyword\":\"3\",\"text\":\"новый мира\"},{"
                + "\"keyword\":\"4\",\"text\":\"дивный новый что ли мир\"}"
                + "]}", StandardCharsets.UTF_8));
            HttpResponse response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()).trim(),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&text="
                + "text:дивный%5C%20мир&get=keyword&sort=keyword&asc"));
            String text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":1,\"hitsArray\":[{"
                    + "\"keyword\":\"1\"}]}"),
                text);
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&text="
                + "text%3Aновый%5C%20мир&get=keyword&sort=keyword&asc"));
            text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":2,\"hitsArray\":[{"
                    + "\"keyword\":\"2\"},{\"keyword\":\"3\"}]}"),
                text);
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&text="
                + "text:дивный%5C%20мир%7E2&get=keyword&sort=keyword"));
            text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":2,\"hitsArray\":[{"
                    + "\"keyword\":\"2\"},{\"keyword\":\"1\"}]}"),
                text);
        } finally {
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testPrefixCaseSensitiveSearch() throws Exception {
        File root = Files.createTempDirectory("testIntervalSearch").toFile();
        try (Daemon daemon = new Daemon(SearchBackendTestBase.config(root));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                + "\"keyword\":\"first\",\"attribute\":1},{"
                + "\"keyword\":\"FIRST\",\"attribute\":2}]}",
                StandardCharsets.UTF_8));
            HttpResponse response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()).trim(),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&text="
                + "keyword:FIR*&get=keyword"));
            String text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":1,\"hitsArray\":[{"
                    + "\"keyword\":\"FIRST\"}]}"),
                text);
        } finally {
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testBareStar() throws Exception {
        File root = Files.createTempDirectory("testBareStar").toFile();
        try (Daemon daemon = new Daemon(SearchBackendTestBase.config(root));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                + "\"keyword\":\"first\",\"attribute\":1},{"
                + "\"keyword\":\"FIRST\",\"attribute\":2}]}",
                StandardCharsets.UTF_8));
            HttpResponse response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()).trim(),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&text=*"
                + "&get=keyword"));
            String text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 400 Bad Request, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_BAD_REQUEST,
                response.getStatusLine().getStatusCode());
        } finally {
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testAcceptCharset() throws Exception {
        File root = Files.createTempDirectory("testAcceptCharset").toFile();
        try (Daemon daemon = new Daemon(
                SearchBackendTestBase.config(root, "lowercase_expanded_terms = true"));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                + "\"keyword\":\"привет\"}]}",
                StandardCharsets.UTF_8));
            HttpResponse response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()).trim(),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            HttpGet get = new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&"
                + "text=keyword:Прив*&get=keyword");
            get.addHeader(HttpHeaders.ACCEPT_CHARSET, "cp1251");
            response = client.execute(get);
            Assert.assertEquals("application/json; charset=windows-1251",
                response.getEntity().getContentType().getValue());
            String text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":1,\"hitsArray\":[{"
                    + "\"keyword\":\"привет\"}]}"),
                text);
        } finally {
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testLfTokenizer() throws Exception {
        File root = Files.createTempDirectory("testLfTokenizer").toFile();
        try (Daemon daemon = new Daemon(SearchBackendTestBase.config(
                root,
                "[field.lf]\n"
                + "tokenizer = lf\n"
                + "filters = headers\n"
                + "attribute = true\n"
                + "analyze = true\n"));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                + "\"keyword\":\"some keyword\", \"lf\":\""
                + "Header1: value1 \\nheader2:value2\\r\\nvalue3\\n"
                + "header4:  value4\\n\"}]}",
                StandardCharsets.UTF_8));
            HttpResponse response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()).trim(),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&"
                + "text=lf%3aheader1%5c%3avalue1&get=keyword"));
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
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&"
                + "text=lf%3aHeader2%5c%3a%5c+value2&get=keyword"));
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
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&"
                + "text=lf%3aUnknown%5c%3a%5c+value3&get=keyword"));
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
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&"
                + "text=lf%3aheader4%5c%3a%5c+value4&get=keyword"));
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
        } finally {
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testBooleanTokenizer() throws Exception {
        File root = Files.createTempDirectory("testBooleanTokenizer").toFile();
        try (Daemon daemon = new Daemon(SearchBackendTestBase.config(root));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                + "\"keyword\":\"some keyword\", \"boolean\":\"hi\"},{"
                + "\"keyword\":\"another keyword\"},{\"keyword\":\"third\""
                + ",\"boolean\":\"\"}]}",
                StandardCharsets.UTF_8));
            HttpResponse response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()).trim(),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&"
                + "text=boolean:bye&get=keyword"));
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
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&"
                + "text=keyword:*+AND+NOT+boolean:me&get=keyword"
                + "&sort=keyword"));
            text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":2,\"hitsArray\":[{\"keyword\":\"third\"},{"
                    + "\"keyword\":\"another keyword\"}]}"),
                text);
        } finally {
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testTruncateFilter() throws Exception {
        File root = Files.createTempDirectory("testTruncateFilter").toFile();
        try (Daemon daemon = new Daemon(SearchBackendTestBase.config(
                root,
                "[field.long]\n"
                + "tokenizer = keyword\n"
                + "filters = truncate:3\n"
                + "attribute = true\n"
                + "analyze = true\n"));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                + "\"keyword\":\"some keyword\", \"long\":\"hi\"},{"
                + "\"keyword\":\"another keyword\", \"long\":\"hello\"}]}",
                StandardCharsets.UTF_8));
            HttpResponse response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()).trim(),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&"
                + "text=long:hi&get=keyword"));
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
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&"
                + "text=long:hellawes&get=keyword"));
            text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":1,\"hitsArray\":[{"
                    + "\"keyword\":\"another keyword\"}]}"),
                text);
        } finally {
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testMafFilter() throws Exception {
        File root = Files.createTempDirectory("testMafFilter").toFile();
        try (Daemon daemon = new Daemon(SearchBackendTestBase.config(
                root,
                "[field.maf]\n"
                + "tokenizer = keyword\n"
                + "filters = maf:100|padding:6\n"
                + "attribute = true\n"
                + "analyze = true\n"));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                + "\"keyword\":\"first\", \"maf\":\"03.33333\"},{"
                + "\"keyword\":\"second\", \"maf\":\"2.004\"},{"
                + "\"keyword\":\"third\", \"maf\":\"12.5\"},{"
                + "\"keyword\":\"forth\", \"maf\":\"0.031e2\"},{"
                + "\"keyword\":\"fifth\", \"maf\":\"1.5e1\"}]}",
                StandardCharsets.UTF_8));
            HttpResponse response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()).trim(),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&"
                + "text=maf:15&get=keyword"));
            String text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":1,\"hitsArray\":[{"
                    + "\"keyword\":\"fifth\"}]}"),
                text);
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&"
                + "text=maf:[0.32e1+TO+13]&get=keyword&sort=keyword&asc"));
            text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":2,\"hitsArray\":[{"
                    + "\"keyword\":\"first\"},{\"keyword\":\"third\"}]}"),
                text);
        } finally {
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testIgnorePrefixFilter() throws Exception {
        File root =
            Files.createTempDirectory("testIgnorePrefixFilter").toFile();
        try (Daemon daemon = new Daemon(SearchBackendTestBase.config(
                root,
                "[field.headers]\n"
                + "tokenizer = lf\n"
                + "filters = headers|ignoreprefix:x-yandex-meta-|"
                + "ignoreprefix:received\\\\:\n"
                + "attribute = true\n"
                + "analyze = true\n"
                + "prefixed = true\n"));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                + "\"keyword\":\"first\", \"headers\":\""
                + "X-Spam: 1\\n"
                + "Received: from mxfront.yandex.ru\\n"
                + "X-Yandex-Meta-HdrFrom: test@ya.ru\\n"
                + "X-Yandex-NotifyMsg: msg\\n\"}]}",
                StandardCharsets.UTF_8));
            HttpResponse response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()).trim(),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&"
                + "text=headers:x-spam%5c:%5c%201&get=keyword"));
            String text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":1,\"hitsArray\":[{"
                    + "\"keyword\":\"first\"}]}"),
                text);
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&"
                + "text=headers:x-yandex-meta-hdrfrom%5c:%5c%20test@ya.ru"
                + "&get=keyword"));
            text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker("{\"hitsCount\":0,\"hitsArray\":[]}"),
                text);
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&"
                + "text=headers:received*&get=keyword"));
            text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker("{\"hitsCount\":0,\"hitsArray\":[]}"),
                text);
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&"
                + "text=headers:x-yandex-notifymsg%5c:%5c%20msg"
                + "&get=keyword"));
            text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":1,\"hitsArray\":[{"
                    + "\"keyword\":\"first\"}]}"),
                text);
        } finally {
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testEmptyGet() throws Exception {
        File root = Files.createTempDirectory("testEmptyGet").toFile();
        try (Daemon daemon =
                new Daemon(SearchBackendTestBase.config(root, NUMBER_CONFIG));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                + "\"keyword\":\"first\"}]}", StandardCharsets.UTF_8));
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            }
            try (CloseableHttpResponse response = client.execute(
                new HttpGet("http://localhost:"
                    + daemon.searchServerPort() + "/search?prefix=1&text="
                    + "keyword:*&get=")))
            {
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_BAD_REQUEST,
                    response);
            }
            try (CloseableHttpResponse response = client.execute(
                new HttpGet("http://localhost:"
                    + daemon.searchServerPort() + "/search?prefix=1&text="
                    + "keyword:*&length=0")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker("{\"hitsCount\":1,\"hitsArray\":[]}"),
                    CharsetUtils.toString(response.getEntity()));
            }
        } finally {
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testSearchScope() throws Exception {
        File root = Files.createTempDirectory("testSearchScope").toFile();
        try (Daemon daemon =
                new Daemon(SearchBackendTestBase.config(root, NUMBER_CONFIG));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                + "\"keyword\":\"first\",\"number\":8}]}",
                StandardCharsets.UTF_8));
            HttpResponse response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()).trim(),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&text="
                + "first&get=keyword&scope=number"));
            String text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker("{\"hitsCount\":0,\"hitsArray\":[]}"),
                text);
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&text="
                + "first&get=keyword&scope=keyword,number"));
            text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":1,\"hitsArray\":[{"
                    + "\"keyword\":\"first\"}]}"),
                text);
        } finally {
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testMultiFieldSearch() throws Exception {
        File root = Files.createTempDirectory("testMultiFieldSearch").toFile();
        try (Daemon daemon = new Daemon(SearchBackendTestBase.config(
                root,
                "\ndefault_operator = or\n"
                + "[field.first]\n"
                + "tokenizer = letter\n"
                + "analyze = true\n"
                + "[field.second]\n"
                + "tokenizer = letter\n"
                + "analyze = true\n"
                + "[field.third]\n"
                + "tokenizer = letter\n"
                + "analyze = true\n"));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                + "\"keyword\":\"first\",\"first\":\"some_thing\","
                + "\"second\":\"field value\",\"third\":\"test this\"},"
                + "{\"keyword\":\"second\",\"first\":\"another thing\","
                + "\"second\":\"a value\",\"third\":\"here i am\"},"
                + "{\"keyword\":\"-\",\"first\":\"third first\"},"
                + "{\"keyword\":\"third\",\"first\":\"abc-def\","
                + "\"second\":\"123-456\"}]}",
                StandardCharsets.UTF_8));
            HttpResponse response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()).trim(),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&text="
                + "first,second:%28some+value%29&get=keyword"));
            String text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":1,\"hitsArray\":[{"
                    + "\"keyword\":\"first\"}]}"),
                text);
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&text="
                + "first,second,third:%28here%5c+i+thing%29&get=keyword"));
            text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":1,\"hitsArray\":[{"
                    + "\"keyword\":\"second\"}]}"),
                text);
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&text="
                + "keyword,first:%5c-&get=keyword"));
            text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":1,\"hitsArray\":[{"
                    + "\"keyword\":\"-\"}]}"),
                text);
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&text="
                + "first,second,third:(test+field)+AND+third:(test+field)"
                + "&get=keyword"));
            text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":1,\"hitsArray\":[{"
                    + "\"keyword\":\"first\"}]}"),
                text);
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&text="
                + "first,second:(abc%5c-def)+AND+(first:(abc%5c-def)+OR+"
                + "second:(abc%5c-def))&get=keyword"));
            text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":1,\"hitsArray\":[{"
                    + "\"keyword\":\"third\"}]}"),
                text);
        } finally {
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testSedFilter() throws Exception {
        File root = Files.createTempDirectory("testSedFilter").toFile();
        try (Daemon daemon = new Daemon(SearchBackendTestBase.config(
                root,
                "[field.sed]\n"
                + "tokenizer = keyword\n"
                + "filters = lowercase|sed:^/[^/]+/((.*)/\\\\|)[^/]+$:$2\n"
                + "attribute = true\n"
                + "analyze = true\n"
                + "index_alias = aux_folder\n"
                + "[field.aux_folder]\n"
                + "tokenizer = keyword\n"
                + "filters = sed:^/([^/]+)/.*:$1\n"
                + "attribute = true\n"
                + "analyze = true\n"));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                + "\"keyword\":\"first\", \"sed\":\"hello\"},{"
                + "\"keyword\":\"second\", "
                + "\"sed\":\"/trash/disk/path/to/file.txt/html\"},{"
                + "\"keyword\":\"third\", "
                + "\"sed\":\"/disk/path/to/file.txt\"},{"
                + "\"keyword\":\"forth\", \"sed\":\"/disk/root.txt\"}]}",
                StandardCharsets.UTF_8));
            HttpResponse response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()).trim(),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&"
                + "text=sed:hello&get=keyword"));
            String text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":1,\"hitsArray\":[{"
                    + "\"keyword\":\"first\"}]}"),
                text);
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&"
                + "text=sed:disk/path/to/file.txt&get=keyword"));
            text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&"
                + "text=aux_folder:/trash/ugar.avi&get=keyword"));
            text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":1,\"hitsArray\":[{"
                    + "\"keyword\":\"second\"}]}"),
                text);
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&"
                + "text=sed:path/to&get=keyword"));
            text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":1,\"hitsArray\":[{"
                    + "\"keyword\":\"third\"}]}"),
                text);
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&"
                + "text=sed:*&get=keyword&sort=keyword&asc"));
            text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":3,\"hitsArray\":[{"
                    + "\"keyword\":\"first\"},{\"keyword\":\"second\"},"
                    + "{\"keyword\":\"third\"}]}"),
                text);
        } finally {
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testIntegerSort() throws Exception {
        System.err.println( "testIntegerSort testcase started" );
        File root = Files.createTempDirectory("testIntegerSort").toFile();
        try (Daemon daemon = new Daemon(SearchBackendTestBase.config(
                root,
                "[field.int]\n"
                + "tokenizer = letter\n"
                + "index = false\n"
                + "store = true\n"
                + "type = string\n"));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":["
                + "{\"keyword\":\"bad\",\"text\":\"c d\",\"int\":\"g\"}]}",
                StandardCharsets.UTF_8));
            HttpResponse response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()).trim(),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
        }
        try (Daemon daemon = new Daemon(SearchBackendTestBase.config(
                root,
                "[field.int]\n"
                + "tokenizer = letter\n"
                + "index = false\n"
                + "store = true\n"
                + "type = integer\n"));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":["
                + "{\"keyword\":\"first\",\"text\":\"a b\",\"int\":40},"
                + "{\"keyword\":\"second\",\"text\":\"b c\",\"int\":5},"
                + "{\"keyword\":\"third\",\"text\":\"a c\",\"int\":70},"
                + "{\"keyword\":\"forth\",\"text\":\"d\",\"int\":50}]}",
                StandardCharsets.UTF_8));
            HttpResponse response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()).trim(),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&text=text:"
                + "a+OR+text:b&get=keyword&sort=int"));
            String text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":3,\"hitsArray\":["
                    + "{\"keyword\":\"third\"},"
                    + "{\"keyword\":\"first\"},"
                    + "{\"keyword\":\"second\"}]}"),
                text);
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&text=text:"
                + "c&get=keyword&sort=int&asc"));
            text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":3,\"hitsArray\":["
                    + "{\"keyword\":\"bad\"},"
                    + "{\"keyword\":\"second\"},"
                    + "{\"keyword\":\"third\"}]}"),
                text);
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&text=text:"
                + "d&get=keyword&sort=int"));
            text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":2,\"hitsArray\":["
                    + "{\"keyword\":\"forth\"},"
                    + "{\"keyword\":\"bad\"}]}"),
                text);
        } finally {
            System.err.println( "testIntegerSort testcase stopped" );
            System.gc();
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testAggregate() throws Exception {
        File root = Files.createTempDirectory("testAggregate").toFile();
        try (Daemon daemon = new Daemon(SearchBackendTestBase.config(root));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                + "\"keyword\":\"1\",\"attribute\":\"1\"},{"
                + "\"keyword\":\"2\",\"attribute\":\"1\"},{"
                + "\"keyword\":\"3\",\"attribute\":\"2\"},{"
                + "\"keyword\":\"4\",\"attribute\":\"2\"}]}",
                StandardCharsets.UTF_8));
            HttpResponse response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()).trim(),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());

            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&text="
                + "keyword:*&group=attribute&get=keyword,summa,count&"
                + "sort=keyword&"
                + "aggregate=sum(keyword)+summa,avg(keyword)+average,count(keyword)+count"));
            String text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":2,\"hitsArray\":[{"
                    + "\"keyword\":\"4\",\"summa\":\"7\",\"average\":\"3\",\"count\":\"2\","
                    + "\"merged_docs\":[{\"keyword\":\"3\",\"summa\":\"7\","
                    + "\"average\":\"3\",\"count\":\"2\"}]},{\"keyword\":\"2\",\"summa\":\"3\""
                    + ",\"average\":\"1\",\"count\":\"2\",\"merged_docs\":[{\"keyword\":\"1\","
                    + "\"summa\":\"3\",\"average\":\"1\",\"count\":\"2\"}]}]}"),
                text);

            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&text="
                + "keyword:*&group=attribute&get=keyword,attribute,count&"
                + "sort=keyword&aggregate=count(attribute;1)+count"));
            text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":2,\"hitsArray\":[{"
                    + "\"keyword\":\"4\",\"attribute\":\"2\",\"count\":\"0\","
                    + "\"merged_docs\":[{\"keyword\":\"3\",\"attribute\":\"2\",\"count\":\"0\"}]},"
                    + "{\"keyword\":\"2\",\"attribute\":\"1\",\"count\":\"2\""
                    + ",\"merged_docs\":[{\"keyword\":\"1\",\"attribute\":\"1\",\"count\":\"2\"}]}]}"),
                text);

            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                + "\"keyword\":\"5\",\"attribute\":\"1\"},{"
                + "\"keyword\":\"6\",\"attribute\":\"1\"},{"
                + "\"keyword\":\"7\",\"attribute\":\"2\"},{"
                + "\"keyword\":\"8\",\"attribute\":\"2\"}]}",
                StandardCharsets.UTF_8));

            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&text="
                + "keyword:*&group=attribute&get=keyword,attribute,lset&"
                + "sort=keyword&merge_func=count&aggregate=set(keyword;2)+lset"));
            text = EntityUtils.toString(response.getEntity()).trim();
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":2,\"hitsArray\":[{"
                    + "\"keyword\":\"8\",\"attribute\":\"2\""
                    + ",\"lset\":\"3\\n4\",\"merged_docs_count\":3},"
                    + "{\"keyword\":\"6\",\"attribute\":\"1\""
                    + ",\"lset\":\"1\\n2\",\"merged_docs_count\":3}]}"),
                text);
        } finally {
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testGetAllFields() throws Exception {
        File root = Files.createTempDirectory("testGetAllFields").toFile();
        try (Daemon daemon = new Daemon(SearchBackendTestBase.config(root));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                + "\"keyword\":\"1\",\"attribute\":\"1\"},{"
                + "\"keyword\":\"2\",\"attribute\":\"1\"},{"
                + "\"keyword\":\"3\",\"attribute\":\"2\","
                + "\"property\":\"a\"},{\"text\":\"hello\","
                + "\"keyword\":\"4\",\"attribute\":\"2\"}]}",
                StandardCharsets.UTF_8));
            HttpResponse response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()).trim(),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&text="
                + "keyword:*&get=*,-attribute&sort=keyword"));
            String text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":4,\"hitsArray\":["
                    + "{\"keyword\":\"4\"},"
                    + "{\"keyword\":\"3\",\"property\":\"a\"},"
                    + "{\"keyword\":\"2\"},"
                    + "{\"keyword\":\"1\"}]}"),
                text);
        } finally {
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testEscape() throws Exception {
        File root = Files.createTempDirectory("testEscape").toFile();
        try (Daemon daemon = new Daemon(SearchBackendTestBase.config(root));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":["
                + "{\"keyword\":\"11\"},{\"keyword\":\"1?\"},"
                + "{\"keyword\":\"22\"},{\"keyword\":\"2*\"}]}",
                StandardCharsets.UTF_8));
            HttpResponse response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()).trim(),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&text="
                + "keyword:1%3F&get=*&sort=keyword&dp=const(0+const)"));
            String text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":2,\"hitsArray\":["
                    + "{\"keyword\":\"1?\"},{\"keyword\":\"11\"}]}"),
                text);
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&text="
                + "keyword:1%5C%3F&get=**&sort=keyword&dp=const(0+const)"));
            text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":1,\"hitsArray\":["
                    + "{\"keyword\":\"1?\",\"const\":\"0\"}]}"),
                text);
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&text="
                + "keyword:2*&get=*&sort=keyword"));
            text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":2,\"hitsArray\":["
                    + "{\"keyword\":\"22\"},{\"keyword\":\"2*\"}]}"),
                text);
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&text="
                + "keyword:2%5C*&get=*&sort=keyword"));
            text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":1,\"hitsArray\":["
                    + "{\"keyword\":\"2*\"}]}"),
                text);
        } finally {
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testMultiSort() throws Exception {
        File root = Files.createTempDirectory("testMultiSort").toFile();
        try (Daemon daemon =
                new Daemon(SearchBackendTestBase.config(root, NUMBER_CONFIG));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                + "\"keyword\":\"first\",\"number\":5},{"
                + "\"keyword\":\"second\",\"property\":0,\"number\":3},{"
                + "\"keyword\":\"third\",\"number\":\"7\"},{"
                + "\"keyword\":\"forth\",\"property\":8,\"number\":1},{"
                + "\"keyword\":\"fifth\",\"number\":\"9\"},{"
                + "\"keyword\":\"sixth\"},{"
                + "\"keyword\":\"eighth\"},{"
                + "\"keyword\":\"seventh\",\"property\":\"4\"}]}",
                StandardCharsets.UTF_8));
            HttpResponse response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()).trim(),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&text="
                + "keyword:*&get=keyword&sort="
                + "multi(number+asc,property,keyword+desc)"));
            String text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":8,\"hitsArray\":["
                    + "{\"keyword\":\"fifth\"},"
                    + "{\"keyword\":\"third\"},"
                    + "{\"keyword\":\"first\"},"
                    + "{\"keyword\":\"second\"},"
                    + "{\"keyword\":\"forth\"},"
                    + "{\"keyword\":\"seventh\"},"
                    + "{\"keyword\":\"eighth\"},"
                    + "{\"keyword\":\"sixth\"}]}"),
                text);
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&text="
                + "keyword:s*&get=keyword&sort=property"));
            text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":3,\"hitsArray\":["
                    + "{\"keyword\":\"seventh\"},"
                    + "{\"keyword\":\"second\"},"
                    + "{\"keyword\":\"sixth\"}]}"),
                text);

            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&text="
                + "keyword:*+AND+NOT+keyword:sixth&get=keyword&sort=np"
                + "&dp=fallback(number,property+np)"));
            text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":7,\"hitsArray\":["
                    + "{\"keyword\":\"fifth\"},"
                    + "{\"keyword\":\"third\"},"
                    + "{\"keyword\":\"first\"},"
                    + "{\"keyword\":\"seventh\"},"
                    + "{\"keyword\":\"second\"},"
                    + "{\"keyword\":\"forth\"},"
                    + "{\"keyword\":\"eighth\"}]}"),
                text);

            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&text="
                + "keyword:*+AND+NOT+keyword:sixth&get=keyword&sort=min"
                + "&dp=min(number,property+min)"));
            text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":7,\"hitsArray\":["
                    + "{\"keyword\":\"fifth\"},"
                    + "{\"keyword\":\"third\"},"
                    + "{\"keyword\":\"first\"},"
                    + "{\"keyword\":\"seventh\"},"
                    + "{\"keyword\":\"forth\"},"
                    + "{\"keyword\":\"second\"},"
                    + "{\"keyword\":\"eighth\"}]}"),
                text);
        } finally {
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testGroupLeader() throws Exception {
        File root = Files.createTempDirectory("testGroupLeader").toFile();
        try (Daemon daemon =
                new Daemon(SearchBackendTestBase.config(root, NUMBER_CONFIG));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":["
                + "{\"keyword\":\"first\",\"number\":5},"
                + "{\"keyword\":\"second\",\"number\":4},"
                + "{\"keyword\":\"third\",\"number\":3},"
                + "{\"keyword\":\"forth\",\"number\":5},"
                + "{\"keyword\":\"fifth\",\"number\":4},"
                + "{\"keyword\":\"sixth\",\"number\":3},"
                + "{\"keyword\":\"seventh\",\"number\":5},"
                + "{\"keyword\":\"eighth\",\"number\":\"1\"}]}",
                StandardCharsets.UTF_8));
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            }
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet("http://localhost:"
                        + daemon.searchServerPort()
                        + "/search?prefix=1&text="
                        + "keyword:*&get=keyword,number"
                        + "&sort=multi(number,keyword)&group=number"
                        + "&merge_func=count")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "{\"hitsCount\":4,\"hitsArray\":["
                        + "{\"keyword\":\"seventh\",\"number\":\"5\","
                        + "\"merged_docs_count\":2},"
                        + "{\"keyword\":\"second\",\"number\":\"4\","
                        + "\"merged_docs_count\":1},"
                        + "{\"keyword\":\"third\",\"number\":\"3\","
                        + "\"merged_docs_count\":1},"
                        + "{\"keyword\":\"eighth\",\"number\":\"1\"}]}"),
                    CharsetUtils.toString(response.getEntity()));
            }
        } finally {
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testDocProcessor() throws Exception {
        File root = Files.createTempDirectory("testDocProcessor").toFile();
        try (Daemon daemon =
                new Daemon(SearchBackendTestBase.config(root, NUMBER_CONFIG));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                + "\"keyword\":\"first\",\"number\":0},{"
                + "\"keyword\":\"second\",\"property\":1234567890},{"
                + "\"keyword\":\"third\",\"property\":1134567890},{"
                + "\"keyword\":\"forth\"}]}",
                StandardCharsets.UTF_8));
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            }
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet("http://localhost:"
                    + daemon.searchServerPort() + "/search?prefix=1&text="
                    + "keyword:*&get=keyword,number&dp=const(0+const)"
                    + "&dp=fallback(number+month)"
                    + "&dp=extractdate(property+year,month)"
                    + "&dp=increment(const+cnst)"
                    + "&dp=fallback(month,cnst+number)"
                    + "&dp=fallback(property,cnst+outer)"
                    + "&dp=to-long(number+nmbr)"
                    + "&sort=nmbr")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "{\"hitsCount\":4,\"hitsArray\":["
                        + "{\"keyword\":\"third\",\"number\":\"12\"},"
                        + "{\"keyword\":\"second\",\"number\":\"2\"},"
                        + "{\"keyword\":\"forth\",\"number\":\"1\"},"
                        + "{\"keyword\":\"first\",\"number\":\"0\"}]}"),
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet("http://localhost:"
                    + daemon.searchServerPort() + "/search?prefix=1&text="
                    + "keyword:*&get=keyword,number&dp=const(0+const)"
                    + "&dp=fallback(number+month)"
                    + "&dp=extractdate(property+year,month)"
                    + "&dp=increment(const)"
                    + "&dp=fallback(month,const+number)"
                    + "&dp=fallback(property,const+outer)"
                    + "&dp=to-string(number+number)"
                    + "&sort=number"
                    + "&outer=outer")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "{\"hitsCount\":3,\"hitsArray\":["
                        + "{\"keyword\":\"second\",\"number\":\"2\"},"
                        + "{\"keyword\":\"third\",\"number\":\"12\"},"
                        + "{\"keyword\":\"first\",\"number\":\"0\"}]}"),
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet("http://localhost:"
                    + daemon.searchServerPort() + "/search?prefix=1&text="
                    + "keyword:*&get=keyword,property,number,K1,K2,A,B,C,D,E,F,"
                    + "G,H,I,J,K,C1,C2,C3,R1,R2,R3,R4,KDOI,poly,L2,LE,L10"
                    + "&dp=const(0+const)"
                    + "&dp=fallback(number+month)"
                    + "&dp=extractdate(property+year,month)"
                    + "&dp=increment(const+cnst)"
                    + "&dp=fallback(month,cnst+number)"
                    + "&dp=const(10+K1)"
                    + "&dp=const(0.13+K2)"
                    + "&dp=const(1+K3)"
                    + "&dp=fallback(property,number+property)"
                    + "&dp=cdiv(number,2+A)"
                    + "&dp=cmul(A,2+B)"
                    + "&dp=cfdiv(number,2.2f+C)"
                    + "&dp=fmul(B,C+D)"
                    + "&dp=fdiv(K3,K2+E)"
                    + "&dp=mul(D,E+F)"
                    + "&dp=div(number,A+G)"
                    + "&dp=sum(F,G+H)"
                    + "&dp=sub(H,G+I)"
                    + "&dp=csub(I,1+J)"
                    + "&dp=csum(J,1+K)"
                    + "&dp=contains(keyword,first+C1)"
                    + "&dp=contains(keyword,se,cond+C2)"
                    + "&dp=contains_any(keyword,se,cond+C3)"
                    + "&dp=regex_contains(keyword,[a-z]%2B+R1)"
                    + "&dp=regex_contains(keyword,f[a-z]%2B,s[a-z]%2B+R2)"
                    + "&dp=regex_contains(keyword,[a-z]+R3)"
                    + "&dp=regex_contains_any(keyword,[a-z]+R4)"
                    + "&dp=k_days_old(property,100+KDO)"
                    + "&dp=to-long(KDO+KDOI)"
                    + "&dp=poly(K1,2,K2,3,property,4,A,2.2f,B,3.3f+poly)"
                    + "&dp=clog(number,2+L2)"
                    + "&dp=clog(number,E+LE)"
                    + "&dp=clog(number,10+L10)"
                    + "&sort=keyword&asc")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                final long kdoi1 =
                    (System.currentTimeMillis() / 1000) / 86400 / 100;
                final long kdoi2 =
                    ((System.currentTimeMillis() / 1000) - 1234567890)
                        / 86400 / 100;
                final long kdoi3 =
                    ((System.currentTimeMillis() / 1000) - 1134567890)
                        / 86400 / 100;
                YandexAssert.check(
                    new JsonChecker(
                        "{\"hitsCount\":4,\"hitsArray\":["
                        + "{\"keyword\":\"first\",\"property\":\"0\","
                            + "\"number\":\"0\""
                            + ",\"K1\":\"10\",\"K2\":\"0.13\",\"A\":\"0\""
                            + ",\"B\":\"0\",\"C\":\"0.0\",\"D\":\"0.0\""
                            + ",\"E\":\"7.692308\",\"F\":\"0\",\"G\":\"0\""
                            + ",\"H\":\"0\",\"I\":\"0\",\"J\":\"-1\""
                            + ",\"K\":\"0\",\"C1\":\"1\",\"C2\":\"0\""
                            + ",\"C3\":\"0\",\"R1\":\"1\",\"R2\":\"2\""
                            + ",\"R3\":\"5\",\"R4\":\"1\",\"KDOI\":\""
                                + kdoi1 + "\""
                            + ",\"poly\":\"20.39\""
                            + ",\"L2\":\"0.0\",\"LE\":\"0.0\",\"L10\":\"0.0\"}"
                        + ",{\"keyword\":\"forth\",\"property\":\"1\","
                            + "\"number\":\"1\""
                            + ",\"K1\":\"10\",\"K2\":\"0.13\",\"A\":\"0\""
                            + ",\"B\":\"0\",\"C\":\"0.45454544\""
                                + ",\"D\":\"0.0\""
                            + ",\"E\":\"7.692308\",\"F\":\"0\",\"G\":\"0\""
                            + ",\"H\":\"0\",\"I\":\"0\",\"J\":\"-1\""
                            + ",\"K\":\"0\",\"C1\":\"0\",\"C2\":\"0\""
                            + ",\"C3\":\"0\",\"R1\":\"1\",\"R2\":\"1\""
                            + ",\"R3\":\"5\",\"R4\":\"1\",\"KDOI\":\""
                                + kdoi1 + "\""
                            + ",\"poly\":\"24.39\""
                            + ",\"L2\":\"0.0\",\"LE\":\"0.0\",\"L10\":\"0.0\"}"
                        + ",{\"keyword\":\"second\",\"property\":\"1234567890\","
                            + "\"number\":\"2\""
                            + ",\"K1\":\"10\",\"K2\":\"0.13\",\"A\":\"1\""
                            + ",\"B\":\"2\",\"C\":\"0.9090909\""
                                + ",\"D\":\"1.8181818\""
                            + ",\"E\":\"7.692308\",\"F\":\"7\",\"G\":\"2\""
                            + ",\"H\":\"9\",\"I\":\"7\",\"J\":\"6\""
                            + ",\"K\":\"7\",\"C1\":\"0\",\"C2\":\"2\""
                            + ",\"C3\":\"1\",\"R1\":\"1\",\"R2\":\"1\""
                            + ",\"R3\":\"6\",\"R4\":\"1\",\"KDOI\":\""
                                + kdoi2 + "\""
                            + ",\"poly\":\"4.9382717E9\""
                            + ",\"L2\":\"1.0\",\"LE\":\"0.6931472\""
                                + ",\"L10\":\"0.30103\"}"
                        + ",{\"keyword\":\"third\",\"property\":\"1134567890\","
                            + "\"number\":\"12\""
                            + ",\"K1\":\"10\",\"K2\":\"0.13\",\"A\":\"6\""
                            + ",\"B\":\"12\",\"C\":\"5.4545455\""
                                + ",\"D\":\"65.454544\""
                            + ",\"E\":\"7.692308\",\"F\":\"455\",\"G\":\"2\""
                            + ",\"H\":\"457\",\"I\":\"455\",\"J\":\"454\""
                            + ",\"K\":\"455\",\"C1\":\"0\",\"C2\":\"0\""
                            + ",\"C3\":\"0\",\"R1\":\"1\",\"R2\":\"0\""
                            + ",\"R3\":\"5\",\"R4\":\"1\",\"KDOI\":\""
                                + kdoi3 + "\""
                            + ",\"poly\":\"4.5382717E9\""
                            + ",\"L2\":\"3.5849626\",\"LE\":\"2.4849067\""
                                + ",\"L10\":\"1.0791812\"}"
                        + "]}"),
                    CharsetUtils.toString(response.getEntity()));
            }
        } finally {
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testFilterByContactsDocProcessor() throws Exception {
        File root = Files
            .createTempDirectory("testFilterByContactsDocProcessor")
            .toFile();
        try (TestSearchBackend lucene = new TestSearchBackend(
            SearchBackendTestBase.config(
                root.getCanonicalPath(),
                "[field.url]\n"
                + "tokenizer = keyword\n"
                + "store = true\n"
                + "prefixed = true\n"
                + "analyze = true\n"
                + "attribute = true\n"
                + "bloom = true\n"
                + "[field.hdr_to]\n"
                + "tokenizer = letter\n"
                + "filters = lowercase|replace:ё:е\n"
                + "prefixed = true\n"
                + "store = true\n"
                + "analyze = true\n"
                + "normalize-utf = true\n"
                + "[field.hdr_from]\n"
                + "tokenizer = letter\n"
                + "filters = lowercase|replace:ё:е\n"
                + "prefixed = true\n"
                + "store = true\n"
                + "analyze = true\n"
                + "normalize-utf = true\n"
                + "[field.hdr_cc]\n"
                + "tokenizer = letter\n"
                + "filters = lowercase|replace:ё:е\n"
                + "prefixed = true\n"
                + "store = true\n"
                + "analyze = true\n"
                + "normalize-utf = true\n",
                10)))
        {
            lucene.add(
                new LongPrefix(1L),
                "\"url\": \"0\","
                +"\"hdr_to\": \"\\\"marija.smolina@yandex.ru\\\" "
                + "<marija.smolina@yandex.ru>\\n\","
                + "\"hdr_from\": \"\\\"Мария Смолина\\\" "
                + "<marija-smolina28@gmail.com>\\n\","
                + "\"hdr_cc\": null",
                "\"url\": \"1\","
                + "\"hdr_to\": \"\\\"Lamoda.ru\\\" "
                + "<support@lamoda.ru>\\n\","
                + "\"hdr_from\": \"\\\"Мария Смолина\\\" "
                + "<marija_smolina28@yandex.ru>\\n\","
                + "\"hdr_cc\": null",
                "\"url\": \"2\","
                + "\"hdr_to\": \"\\\"Anna 123\\\" "
                + "<anna.russkih@yandex.ru>\\n\","
                + "\"hdr_from\": \"\\\"Мария Смолина\\\" "
                + "<marija.smolina28@yandex.ru>\\n\","
                + "\"hdr_cc\": null",
                "\"url\": \"3\","
                + "\"hdr_to\": \"\\\"Mariia\\\" "
                + "<mariia.gracheva12345@gmail.com>\\n\","
                + "\"hdr_from\": \"\\\"https://www.gmail.com/\\\" "
                + "<google@gmail.com>\\n\","
                + "\"hdr_cc\": \"\\\"Мария Грачева\\\" "
                + "<marijagracheva@yandex.ru>\\n\""
            );

            lucene.checkSearch(
                "/search?prefix=1&text=hdr_to:*&scorer=perfield("
                + "hdr_to,hdr_from,hdr_cc)&get=url,%23hdr_to_exact_hits,"
                + "%23hdr_to_non_exact_hits,%23hdr_from_exact_hits,"
                + "%23hdr_from_non_exact_hits,%23hdr_cc_exact_hits,"
                + "%23hdr_cc_non_exact_hits"
                + "&dp=filter_by_contacts(hdr_to,hdr_from,hdr_cc+none+smolina)",
                new JsonChecker(
                    "{\"hitsCount\":3,\"hitsArray\":["
                    + "{\"url\": \"0\","
                    + "\"#hdr_to_exact_hits\":\"1\","
                    + "\"#hdr_to_non_exact_hits\":\"1\","
                    + "\"#hdr_from_exact_hits\":\"1\","
                    + "\"#hdr_from_non_exact_hits\":\"1\","
                    + "\"#hdr_cc_exact_hits\":null,"
                    + "\"#hdr_cc_non_exact_hits\":null"
                    + "},"
                    + "{\"url\": \"1\","
                    + "\"#hdr_to_exact_hits\":\"0\","
                    + "\"#hdr_to_non_exact_hits\":\"0\","
                    + "\"#hdr_from_exact_hits\":\"1\","
                    + "\"#hdr_from_non_exact_hits\":\"1\","
                    + "\"#hdr_cc_exact_hits\":null,"
                    + "\"#hdr_cc_non_exact_hits\":null"
                    + "},"
                    + "{\"url\": \"2\","
                    + "\"#hdr_to_exact_hits\":\"0\","
                    + "\"#hdr_to_non_exact_hits\":\"0\","
                    + "\"#hdr_from_exact_hits\":\"1\","
                    + "\"#hdr_from_non_exact_hits\":\"1\","
                    + "\"#hdr_cc_exact_hits\":null,"
                    + "\"#hdr_cc_non_exact_hits\":null"
                    + "}"
                    + "]}"));

            String uri = "/search?prefix=1&text=hdr_to:*&scorer=perfield("
                + "hdr_to,hdr_from,hdr_cc)&get=url";

            lucene.checkSearch(
                uri + "&dp=filter_by_contacts(hdr_to,hdr_from,hdr_cc+all+грачева)",
                new JsonChecker(
                    "{\"hitsCount\":1,\"hitsArray\":[{\"url\": \"3\"}]}"));

            lucene.checkSearch(
                uri + "&dp=filter_by_contacts(hdr_to,hdr_from,hdr_cc+all+molina)",
                new JsonChecker("{\"hitsCount\":0,\"hitsArray\":[]}"));

            lucene.checkSearch(
                uri + "&dp=filter_by_contacts(hdr_to,hdr_from+all+ru)",
                new JsonChecker(
                    "{\"hitsCount\":1,\"hitsArray\":[{\"url\": \"2\"}]}"));

            lucene.checkSearch(
                uri + "&dp=filter_by_contacts(hdr_to,hdr_from+all+com)",
                new JsonChecker("{\"hitsCount\":0,\"hitsArray\":[]}"));

            lucene.checkSearch(
                uri + "&dp=filter_by_contacts(hdr_to,hdr_from+all+gmail,mariia)",
                new JsonChecker("{\"hitsCount\":0,\"hitsArray\":[]}"));

            lucene.checkSearch(
                uri + "&dp=filter_by_contacts(hdr_to,hdr_from,hdr_cc"
                + "+all+lam+an,rus+ан,рус+фт,кгы+gmail)",
                new JsonChecker(
                    "{\"hitsCount\":3,\"hitsArray\":["
                    + "{\"url\": \"1\"},"
                    + "{\"url\": \"2\"},"
                    + "{\"url\": \"3\"}]}"));

            lucene.checkSearch(
                uri + "&dp=filter_by_contacts(hdr_to,hdr_from+all+yandex)",
                new JsonChecker("{\"hitsCount\":0,\"hitsArray\":[]}"));

            lucene.checkSearch(
                uri + "&dp=filter_by_contacts(hdr_to,hdr_from+none+yandex)",
                new JsonChecker(
                    "{\"hitsCount\":3,\"hitsArray\":["
                    + "{\"url\": \"0\"},"
                    + "{\"url\": \"1\"},"
                    + "{\"url\": \"2\"}]}"));

            lucene.checkSearch(
                uri + "&dp=filter_by_contacts(hdr_to,hdr_from,hdr_cc+yandex+yandex)",
                new JsonChecker("{\"hitsCount\":0,\"hitsArray\":[]}"));

            lucene.checkSearch(
                uri + "&dp=filter_by_contacts(hdr_to,hdr_from+none+gmail)",
                new JsonChecker(
                    "{\"hitsCount\":2,\"hitsArray\":["
                    + "{\"url\": \"0\"},"
                    + "{\"url\": \"3\"}]}"));

            lucene.checkSearch(
                uri + "&dp=filter_by_contacts(hdr_to,hdr_from,hdr_cc+gmail+gmail)",
                new JsonChecker(
                    "{\"hitsCount\":1,\"hitsArray\":[{\"url\": \"3\"}]}"));
        }
    }

    @Test
    public void testReplaceAllDocProcessor() throws Exception {
        File root =
            Files.createTempDirectory("testReplaceAllDocProcessor").toFile();
        try (Daemon daemon =
             new Daemon(SearchBackendTestBase.config(
                 root,
                 "[field.hdr_to]\n"
                 + "tokenizer = letter\n"
                 + "filters = lowercase|replace:ё:е\n"
                 + "prefixed = true\n"
                 + "store = true\n"
                 + "analyze = true\n"
                 + "normalize-utf = true\n"));
             CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpPost post = new HttpPost(
                "http://localhost:" + daemon.jsonServerPort() + "/add");
            post.setEntity(
                new StringEntity(
                    "{\"prefix\":1,\"docs\":["
                    + "{\"hdr_to\":\"\\\"marija.smolina@yandex.ru\\\" "
                    + "<marija.smolina@yandex.ru>\\n\\\"Мария Смолина\\\" "
                    + "<marija.smolina28@yandex.ru>\\n\""
                    + "}]}",
                    StandardCharsets.UTF_8));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);

            String uri = "http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&text="
                + "hdr_to:*&get=hdr_to_new";
            try (CloseableHttpResponse response = client.execute(
                new HttpGet(uri + "&dp=replace_all(hdr_to,,b+hdr_to_new)")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_BAD_REQUEST, response);
            }

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(uri + "&dp=replace_all(hdr_to,a+hdr_to_new)")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_BAD_REQUEST, response);
            }

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(uri + "&dp=replace_all(hdr_to,ar,+hdr_to_new)")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals("",
                    "{\"hitsCount\":1,\"hitsArray\":["
                    + "{\"hdr_to_new\":\"\\\"mija.smolina@yandex.ru\\\" "
                    + "<mija.smolina@yandex.ru>\\n\\\"Мария Смолина\\\" "
                    + "<mija.smolina28@yandex.ru>\\n\""
                    + "}]}",
                    CharsetUtils.toString(response.getEntity()).trim());
            }

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(uri + "&dp=replace_all(hdr_to,a,b+hdr_to_new)")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals("",
                    "{\"hitsCount\":1,\"hitsArray\":["
                    + "{\"hdr_to_new\":\"\\\"mbrijb.smolinb@ybndex.ru\\\" "
                    + "<mbrijb.smolinb@ybndex.ru>\\n\\\"Мария Смолина\\\" "
                    + "<mbrijb.smolinb28@ybndex.ru>\\n\""
                    + "}]}",
                    CharsetUtils.toString(response.getEntity()).trim());
            }

            String domainRegexp = "@[a-zA-Z0-9-._]*[.][a-zA-Z0-9]*";

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(uri + "&dp=replace_all(hdr_to,"
                    + domainRegexp + ",+hdr_to_new)")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals("",
                    "{\"hitsCount\":1,\"hitsArray\":["
                    + "{\"hdr_to_new\":\"\\\"marija.smolina\\\" "
                    + "<marija.smolina>\\n\\\"Мария Смолина\\\" "
                    + "<marija.smolina28>\\n\""
                    + "}]}",
                    CharsetUtils.toString(response.getEntity()).trim());
            }

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(
                    uri + "&dp=replace_all(hdr_to,%5Cn,+hdr_to_new)")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals("",
                    "{\"hitsCount\":1,\"hitsArray\":["
                    + "{\"hdr_to_new\":\"\\\"marija.smolina@yandex.ru\\\" "
                    + "<marija.smolina@yandex.ru>\\\"Мария Смолина\\\" "
                    + "<marija.smolina28@yandex.ru>\""
                    + "}]}",
                    CharsetUtils.toString(response.getEntity()).trim());
            }
        }
    }

    @Test
    public void testCDotProduct() throws Exception {
        File root = Files.createTempDirectory("testCDotProduct").toFile();
        try (Daemon daemon =
                new Daemon(SearchBackendTestBase.config(root, NUMBER_CONFIG));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(
                new StringEntity(
                    "{\"prefix\":1,\"docs\":["
                    + "{\"keyword\":\"first\","
                    + "\"float-array\":\"0.5\n0.25\n0.125\"},"
                    + "{\"keyword\":\"second\","
                    + "\"float-array\":\"0.5\n0.25\n0.5\"},"
                    + "{\"keyword\":\"third\","
                    + "\"float-array\":\"0.5\n0.5\n0\"},"
                    + "{\"keyword\":\"forth\"}]}",
                StandardCharsets.UTF_8));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet("http://localhost:"
                    + daemon.searchServerPort() + "/search?prefix=1&text="
                    + "keyword:*&get=keyword,product"
                    + "&dp=cdot-product(float-array+0.5%0a2%0a-0.5+product)"
                    + "&sort=product")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    "{\"hitsCount\":4,\"hitsArray\":["
                    + "{\"keyword\":\"third\",\"product\":\"1.25\"},"
                    + "{\"keyword\":\"first\",\"product\":\"0.6875\"},"
                    + "{\"keyword\":\"second\",\"product\":\"0.5\"},"
                    + "{\"keyword\":\"forth\",\"product\":null}]}",
                    CharsetUtils.toString(response.getEntity()).trim());
            }
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet("http://localhost:"
                    + daemon.searchServerPort() + "/search?prefix=1&text="
                    + "keyword:*&get=keyword,product"
                    + "&dp=cdot-product(float-array+0.5%0a2%0a-0.5+product)"
                    + "&postfilter=product+%3e%3d+0.6"
                    + "&postfilter=product+%3c%3d+1e0"
                    + "&sort=product")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    "{\"hitsCount\":1,\"hitsArray\":["
                    + "{\"keyword\":\"first\",\"product\":\"0.6875\"}]}",
                    CharsetUtils.toString(response.getEntity()).trim());
            }
            // And check comparator
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet("http://localhost:"
                    + daemon.searchServerPort() + "/search?prefix=1&text="
                    + "keyword:*&get=keyword,float-array"
                    + "&sort=float-array")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    "{\"hitsCount\":4,\"hitsArray\":["
                    + "{\"keyword\":\"third\",\"float-array\":[0.5,0.5,0.0]},"
                    + "{\"keyword\":\"second\","
                    + "\"float-array\":[0.5,0.25,0.5]},"
                    + "{\"keyword\":\"first\","
                    + "\"float-array\":[0.5,0.25,0.125]},"
                    + "{\"keyword\":\"forth\",\"float-array\":null}]}",
                    CharsetUtils.toString(response.getEntity()).trim());
            }
        }
    }

    @Test
    public void testCHexDotProduct() throws Exception {
        File root = Files.createTempDirectory("testCHexDotProduct").toFile();
        try (Daemon daemon =
                new Daemon(SearchBackendTestBase.config(root, NUMBER_CONFIG));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(
                new StringEntity(
                    "{\"prefix\":1,\"docs\":["
                    + "{\"keyword\":\"first\",\"byte-array\":\"010203\"},"
                    + "{\"keyword\":\"second\",\"byte-array\":\"7F7E7D\"},"
                    + "{\"keyword\":\"third\",\"byte-array\":\"8081FF\"},"
                    + "{\"keyword\":\"forth\"}]}",
                StandardCharsets.UTF_8));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet("http://localhost:"
                    + daemon.searchServerPort() + "/search?prefix=1&text="
                    + "keyword:*&get=keyword,product"
                    + "&dp=chex-dot-product(byte-array+010002+product)"
                    + "&sort=product")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    "{\"hitsCount\":4,\"hitsArray\":["
                    + "{\"keyword\":\"second\",\"product\":\"377\"},"
                    + "{\"keyword\":\"first\",\"product\":\"7\"},"
                    + "{\"keyword\":\"third\",\"product\":\"-130\"},"
                    + "{\"keyword\":\"forth\",\"product\":null}]}",
                    CharsetUtils.toString(response.getEntity()).trim().trim());
            }
            // Check comparator
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet("http://localhost:"
                    + daemon.searchServerPort() + "/search?prefix=1&text="
                    + "keyword:*&get=keyword,byte-array,hex"
                    + "&sort=byte-array&dp=to-string(byte-array+hex)")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    "{\"hitsCount\":4,\"hitsArray\":["
                    + "{\"keyword\":\"second\",\"byte-array\":[127,126,125],"
                    + "\"hex\":\"7F7E7D\"},"
                    + "{\"keyword\":\"first\",\"byte-array\":[1,2,3],"
                    + "\"hex\":\"010203\"},"
                    + "{\"keyword\":\"third\",\"byte-array\":[-128,-127,-1],"
                    + "\"hex\":\"8081FF\"},"
                    + "{\"keyword\":\"forth\",\"byte-array\":null,"
                    + "\"hex\":null}]}",
                    CharsetUtils.toString(response.getEntity()).trim());
            }
        }
    }

    @Test
    public void testGeoDistanceDocProcessor() throws Exception {
        File root = Files.createTempDirectory("testGeoDistanceDocProcessor")
            .toFile();
        try (Daemon daemon =
                new Daemon(SearchBackendTestBase.config(root, NUMBER_CONFIG));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(
                new StringEntity(
                    "{\"prefix\":1,\"docs\":["
                    + "{\"keyword\":\"first\","
                    + "\"lat\":54.9486,\"lon\":35.3525},"
                    + "{\"keyword\":\"second\","
                    + "\"lat\":54.9616,\"lon\":35.6024}]}",
                StandardCharsets.UTF_8));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet("http://localhost:"
                    + daemon.searchServerPort() + "/search?prefix=1&text="
                    + "keyword:*&get=keyword,distance"
                    + "&dp=fconst(54.5616+clat)"
                    + "&dp=fconst(33.6024+clon)"
                    + "&dp=geo_distance(lat,lon,clat,clon+distance)"
                    + "&sort=distance")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    "{\"hitsCount\":2,\"hitsArray\":["
                    + "{\"keyword\":\"second\",\"distance\":\"135798.77\"},"
                    + "{\"keyword\":\"first\",\"distance\":\"120258.08\"}]}",
                    CharsetUtils.toString(response.getEntity()).trim());
            }
        }
    }

    @Test
    public void testMultiDocProcessor() throws Exception {
        File root =
            Files.createTempDirectory("testMultiDocProcessor").toFile();
        try (Daemon daemon =
                new Daemon(SearchBackendTestBase.config(root, NUMBER_CONFIG));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":["
                + "{\"keyword\":\"1\",\"number\":1,\"property\":1},"
                + "{\"keyword\":\"2\",\"number\":1,\"property\":2},"
                + "{\"keyword\":\"3\",\"number\":1,\"property\":3},"
                + "{\"keyword\":\"4\",\"number\":2},"
                + "{\"keyword\":\"5\",\"number\":2,\"property\":2},"
                + "{\"keyword\":\"6\",\"number\":2}]}",
                StandardCharsets.UTF_8));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet("http://localhost:"
                    + daemon.searchServerPort() + "/search?prefix=1&text="
                    + "__prefix:1&get=keyword,sort,concat,cdiv,cmul,sum"
                    + "&dp=to-long(number)"
                    + "&dp=to-long(property)"
                    + "&dp=descending(property)"
                    + "&dp=multi(number,property,keyword+sort)"
                    + "&dp=concat(number,property+concat)"
                    + "&dp=cdiv(keyword,3+cdiv)"
                    + "&dp=cmul(property,2+cmul)"
                    + "&dp=sum(number,property+sum)"
                    + "&sort=sort&skip-nulls")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    "{\"hitsCount\":6,\"hitsArray\":["
                    + "{\"keyword\":\"5\",\"sort\":[\"2\",\"2\",\"5\"],"
                    + "\"concat\":\"22\",\"cdiv\":\"1\",\"cmul\":\"4\","
                    + "\"sum\":\"4\"},"
                    + "{\"keyword\":\"6\",\"sort\":[\"2\",null,\"6\"],"
                    + "\"cdiv\":\"2\"},"
                    + "{\"keyword\":\"4\",\"sort\":[\"2\",null,\"4\"],"
                    + "\"cdiv\":\"1\"},"
                    + "{\"keyword\":\"1\",\"sort\":[\"1\",\"1\",\"1\"],"
                    + "\"concat\":\"11\",\"cdiv\":\"0\",\"cmul\":\"2\","
                    + "\"sum\":\"2\"},"
                    + "{\"keyword\":\"2\",\"sort\":[\"1\",\"2\",\"2\"],"
                    + "\"concat\":\"12\",\"cdiv\":\"0\",\"cmul\":\"4\","
                    + "\"sum\":\"3\"},"
                    + "{\"keyword\":\"3\",\"sort\":[\"1\",\"3\",\"3\"],"
                    + "\"concat\":\"13\",\"cdiv\":\"1\",\"cmul\":\"6\","
                    + "\"sum\":\"4\"}]}",
                    CharsetUtils.toString(response.getEntity()).trim());
            }
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet("http://localhost:"
                    + daemon.searchServerPort() + "/search?prefix=1&text="
                    + "__prefix:1&get=group"
                    + "&dp=multi(property,property+pair)"
                    + "&dp=multi(number,pair+group)"
                    + "&group=group"
                    + "&merge_func=none")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    "{\"hitsCount\":5,\"hitsArray\":["
                    + "{\"group\":[\"1\",[\"1\",\"1\"]]},"
                    + "{\"group\":[\"1\",[\"2\",\"2\"]]},"
                    + "{\"group\":[\"1\",[\"3\",\"3\"]]},"
                    + "{\"group\":[\"2\",[null,null]]},"
                    + "{\"group\":[\"2\",[\"2\",\"2\"]]}]}",
                    CharsetUtils.toString(response.getEntity()).trim());
            }
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet("http://localhost:"
                    + daemon.searchServerPort() + "/search?prefix=1&text="
                    + "__prefix:1&get=keyword"
                    + "&dp=const(3+three)"
                    + "&dp=fallback(property,three+prop)"
                    + "&postfilter=number+%3c%3d+1"
                    + "&postfilter=prop+%3e%3d+3")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    "{\"hitsCount\":1,\"hitsArray\":[{\"keyword\":\"3\"}]}",
                    CharsetUtils.toString(response.getEntity()).trim());
            }
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet("http://localhost:"
                    + daemon.searchServerPort() + "/search?prefix=1&text="
                    + "__prefix:1&get=keyword&postfilter=property+%3e%3d+2"
                    + "&sort=keyword")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    "{\"hitsCount\":3,\"hitsArray\":[{\"keyword\":\"5\"},"
                    + "{\"keyword\":\"3\"},{\"keyword\":\"2\"}]}",
                    CharsetUtils.toString(response.getEntity()).trim());
            }
        } finally {
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testCollectorFactory() throws Exception {
        File root = Files.createTempDirectory("testCollectorFactory").toFile();
        try (Daemon daemon =
                new Daemon(SearchBackendTestBase.config(root, NUMBER_CONFIG));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                + "\"keyword\":\"first\",\"number\":0},{"
                + "\"keyword\":\"second\",\"property\":1234567890},{"
                + "\"keyword\":\"third\",\"property\":1134567890},{"
                + "\"keyword\":\"forth\"}]}",
                StandardCharsets.UTF_8));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);
            //test default collector
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet("http://localhost:"
                    + daemon.searchServerPort() + "/search?prefix=1&text="
                    + "keyword:*&get=keyword,number&dp=const(0+const)"
                    + "&dp=fallback(number+month)"
                    + "&dp=extractdate(property+year,month)"
                    + "&dp=increment(const,1+cnst)"
                    + "&dp=fallback(month,cnst+number)"
                    + "&dp=to-long(number)"
                    + "&sort=number")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    "{\"hitsCount\":4,\"hitsArray\":["
                    + "{\"keyword\":\"third\",\"number\":\"12\"},"
                    + "{\"keyword\":\"second\",\"number\":\"2\"},"
                    + "{\"keyword\":\"forth\",\"number\":\"1\"},"
                    + "{\"keyword\":\"first\",\"number\":\"0\"}]}",
                    CharsetUtils.toString(response.getEntity()).trim());
            }

            //test "collector" argument checking
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet("http://localhost:"
                    + daemon.searchServerPort() + "/search?prefix=1&text="
                    + "keyword:*&get=keyword,number&dp=const(0+const)"
                    + "&dp=fallback(number+month)"
                    + "&dp=extract-date(property+year,month)"
                    + "&dp=increment(const,-2)"
                    + "&dp=fallback(month,const+number)"
                    + "&dp=to-long(number)"
                    + "&sort=number"
                    + "&collector=sorted")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    "{\"hitsCount\":4,\"hitsArray\":["
                    + "{\"keyword\":\"third\",\"number\":\"12\"},"
                    + "{\"keyword\":\"second\",\"number\":\"2\"},"
                    + "{\"keyword\":\"first\",\"number\":\"0\"},"
                    + "{\"keyword\":\"forth\",\"number\":\"-2\"}]}",
                    CharsetUtils.toString(response.getEntity()).trim());
            }

            //test "collector" argument checking
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet("http://localhost:"
                    + daemon.searchServerPort() + "/search?prefix=1&text="
                    + "keyword:*&get=keyword,number&dp=const(0+const)"
                    + "&dp=fallback(number+month)"
                    + "&dp=extractdate(property+year,month)"
                    + "&dp=increment(const)"
                    + "&dp=fallback(month,const+number)"
                    + "&sort=number"
                    + "&collector=unknown")))
            {
                Assert.assertEquals("Expected 400 Bad Request, but received: "
                    + response.getStatusLine() + " and body: "
                    + CharsetUtils.toString(response.getEntity()).trim(),
                    HttpStatus.SC_BAD_REQUEST,
                    response.getStatusLine().getStatusCode());
            }

            //test CollectorFactory request parameters validating
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet("http://localhost:"
                    + daemon.searchServerPort() + "/search?prefix=1&text="
                    + "keyword:*&get=keyword,number&dp=const(0+const)"
                    + "&dp=fallback(number+month)"
                    + "&dp=extractdate(property+year,month)"
                    + "&dp=increment(const)"
                    + "&dp=fallback(month,const+number)"
                    + "&sort=number"
                    + "&collector=passthru")))
            {
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_BAD_REQUEST,
                    response);
            }

            //test PassThru collector with DocProcessor
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet("http://localhost:"
                    + daemon.searchServerPort() + "/search?prefix=1&text="
                    + "keyword:*&get=keyword,number&dp=const(0+const)"
                    + "&dp=fallback(number+month)"
                    + "&dp=extractdate(property+year,month)"
                    + "&dp=increment(const+const)"
                    + "&dp=fallback(month,const+number)"
                    + "&dp=to-long(number)"
                    + "&merge_func=none"
                    + "&collector=passthru")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    "{\"hitsCount\":4,\"hitsArray\":["
                    + "{\"keyword\":\"first\",\"number\":\"0\"},"
                    + "{\"keyword\":\"second\",\"number\":\"2\"},"
                    + "{\"keyword\":\"third\",\"number\":\"12\"},"
                    + "{\"keyword\":\"forth\",\"number\":\"1\"}"
                    + "]}",
                    CharsetUtils.toString(response.getEntity()).trim());
            }

            //test PassThru collector with early termination
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet("http://localhost:"
                    + daemon.searchServerPort() + "/search?prefix=1&text="
                    + "keyword:*&get=keyword,number&dp=const(0+const)"
                    + "&dp=fallback(number+month)"
                    + "&dp=extractdate(property+year,month)"
                    + "&dp=increment(const+const)"
                    + "&dp=fallback(month,const+number)"
                    + "&dp=to-long(number)"
                    + "&merge_func=none"
                    + "&length=2"
                    + "&early-interrupt"
                    + "&collector=passthru")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    "{\"hitsArray\":["
                    + "{\"keyword\":\"first\",\"number\":\"0\"},"
                    + "{\"keyword\":\"second\",\"number\":\"2\"}"
                    + "],\"hitsCount\":2}",
                    CharsetUtils.toString(response.getEntity()).trim());
            }

            try (CloseableHttpResponse response = client.execute(
                    new HttpGet("http://localhost:"
                    + daemon.searchServerPort() + "/search?prefix=1&text="
                    + "keyword:*&get=keyword,number&dp=const(0+const)"
                    + "&dp=fallback(number+month)"
                    + "&dp=extractdate(property+year,month)"
                    + "&dp=increment(const+const)"
                    + "&dp=fallback(month,const+number)"
                    + "&dp=to-long(number)"
                    + "&merge_func=none"
                    + "&offset=1"
                    + "&collector=passthru")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    "{\"hitsCount\":4,\"hitsArray\":["
                    + "{\"keyword\":\"second\",\"number\":\"2\"},"
                    + "{\"keyword\":\"third\",\"number\":\"12\"},"
                    + "{\"keyword\":\"forth\",\"number\":\"1\"}"
                    + "]}",
                    CharsetUtils.toString(response.getEntity()).trim());
            }

            try (CloseableHttpResponse response = client.execute(
                    new HttpGet("http://localhost:"
                    + daemon.searchServerPort() + "/search?prefix=1&text="
                    + "keyword:*&get=keyword,number&dp=const(0+const)"
                    + "&dp=fallback(number+month)"
                    + "&dp=extractdate(property+year,month)"
                    + "&dp=increment(const+const)"
                    + "&dp=fallback(month,const+number)"
                    + "&dp=to-long(number)"
                    + "&merge_func=none"
                    + "&offset=1"
                    + "&length=1"
                    + "&collector=passthru")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    "{\"hitsArray\":["
                    + "{\"keyword\":\"second\",\"number\":\"2\"}"
                    + "],\"hitsCount\":4}",
                    CharsetUtils.toString(response.getEntity()).trim());
            }

            try (CloseableHttpResponse response = client.execute(
                    new HttpGet("http://localhost:"
                    + daemon.searchServerPort() + "/search?prefix=1&text="
                    + "keyword:*&get=keyword,number&dp=const(0+const)"
                    + "&dp=fallback(number+month)"
                    + "&dp=extractdate(property+year,month)"
                    + "&dp=increment(const+const)"
                    + "&dp=fallback(month,const+number)"
                    + "&dp=to-long(number)"
                    + "&merge_func=none"
                    + "&offset=1"
                    + "&length=1"
                    + "&collector=passthru(1)")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    "{\"hitsArray\":["
                    + "{\"keyword\":\"second\",\"number\":\"2\"}"
                    + "],\"hitsCount\":4}",
                    CharsetUtils.toString(response.getEntity()).trim());
            }

            //test PassThru collector without DocProcessor
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet("http://localhost:"
                    + daemon.searchServerPort() + "/search?prefix=1&text="
                    + "keyword:*&get=keyword,number"
                    + "&merge_func=none"
                    + "&collector=passthru(0)")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    "{\"hitsArray\":["
                    + "{\"keyword\":\"first\",\"number\":\"0\"},"
                    + "{\"keyword\":\"second\",\"number\":null},"
                    + "{\"keyword\":\"third\",\"number\":null},"
                    + "{\"keyword\":\"forth\",\"number\":null}"
                    + "],\"hitsCount\":4}",
                    CharsetUtils.toString(response.getEntity()).trim());
            }

            //test PassThru collector automated selection
            //PassThruCollector will return documents in the order they
            //was added while SortedCollector will reorder whem
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet("http://localhost:"
                    + daemon.searchServerPort() + "/search?prefix=1&text="
                    + "keyword:*&get=keyword,number"
                    + "&merge_func=none&offset=1")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    "{\"hitsCount\":4,\"hitsArray\":["
                    + "{\"keyword\":\"second\",\"number\":null},"
                    + "{\"keyword\":\"third\",\"number\":null},"
                    + "{\"keyword\":\"forth\",\"number\":null}"
                    + "]}",
                    CharsetUtils.toString(response.getEntity()).trim());
            }
        } finally {
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testWildcard() throws Exception {
        try (TestSearchBackend lucene = new TestSearchBackend(
                this,
                new File(
                    Paths.getSourcePath(
                        "mail/search/mail/search_backend_mail_config/files"
                        + "/search_backend_test.conf_mail"))))
        {
            lucene.add(
                "\"url\":\"1\",\"stid\":\"20040\"",
                "\"url\":\"2\",\"stid\":\"020040\"",
                "\"url\":\"3\",\"stid\":\"20*40\"",
                "\"url\":\"4\",\"stid\":\"20*400\"");
            lucene.checkSearch(
                "/search?prefix=0&get=url&sort=url&text=stid:20%5C*40",
                TestSearchBackend.prepareResult("\"url\":\"3\""));
            lucene.checkSearch(
                "/search?prefix=0&get=url&sort=url&text=stid:20%5C*40*",
                TestSearchBackend.prepareResult("\"url\":\"4\"", "\"url\":\"3\""));
            // Shitty query language makes me sad
            lucene.checkSearch(
                "/search?prefix=0&get=url&sort=url&text=stid:*20%5C*40*",
                TestSearchBackend.prepareResult(
                    "\"url\":\"4\"",
                    "\"url\":\"3\"",
                    "\"url\":\"2\"",
                    "\"url\":\"1\""));
        }
    }

    @Test
    public void testLongOverflow() throws Exception {
        File root = Files.createTempDirectory("testDocProcessor").toFile();
        try (Daemon daemon =
                new Daemon(
                    SearchBackendTestBase.config(
                        root,
                        NUMBER_CONFIG
                        + "[field.long]\nstore = true\ntype = long\n"));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                + "\"keyword\":\"1\",\"long\":\"9254360000374336477\"}]}",
                StandardCharsets.UTF_8));
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            }
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet("http://localhost:"
                    + daemon.searchServerPort()
                    + "/search?prefix=1&text=keyword:*&get=long")))
            {
                // Here we see JSON producer by lucene, which cannot be parsed
                // by conventionals JSON parsers
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    "{\"hitsCount\":1,\"hitsArray\":["
                    + "{\"long\":\"9254360000374336477\"}]}",
                    CharsetUtils.toString(response.getEntity()).trim());
            }
        } finally {
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testNormalization() throws Exception {
        try (TestSearchBackend lucene = new TestSearchBackend(
                this,
                new File(
                    Paths.getSourcePath(
                        "mail/search/mail/search_backend_mail_config/files"
                        + "/search_backend_test.conf_mail"))))
        {
            lucene.add(
                "\"url\":\"1\",\"body_text\":\"йод\"",
                "\"url\":\"2\",\"body_text\":\"пьяны\u0438\u0306\"",
                "\"url\":\"3\",\"body_text\":\"Pendeltåg\"");
            lucene.checkSearch(
                "/search?prefix=0&get=url&sort=url&text="
                + "body_text:(пьяный+и%cc%86од+pendelta%cc%8ag)",
                TestSearchBackend.prepareResult(
                    "\"url\":\"3\"",
                    "\"url\":\"2\"",
                    "\"url\":\"1\""));
            lucene.checkSearch(
                "/search?prefix=0&get=url&sort=url&text=url:(1+2)&offset=1",
                TestSearchBackend.prepareResult(2, "\"url\":\"1\""));
        }
    }

    @Test
    public void testStringEqualsPredicate() throws Exception {
        try (TestSearchBackend lucene = new TestSearchBackend(
                this,
                new File(
                    Paths.getSourcePath(
                        "mail/search/mail/search_backend_mail_config/files"
                        + "/search_backend_test.conf_mail"))))
        {
            lucene.add(
                "\"url\":\"1\",\"body_text\":\"мир\",\"hdr_subject\":\"ром\"",
                "\"url\":\"2\",\"body_text\":\"миру мир\"",
                "\"url\":\"3\",\"body_text\":\"мир дверь мяч\"");
            lucene.checkSearch(
                "/search?prefix=0&get=url&sort=url&text=body_text:миру"
                + "&postfilter=url+%21=+2",
                TestSearchBackend.prepareResult("\"url\":\"3\"", "\"url\":\"1\""));
            lucene.checkSearch(
                "/search?prefix=0&get=url&sort=url&text=body_text:миру"
                + "&postfilter=hdr_subject+%21=+ром",
                TestSearchBackend.prepareResult("\"url\":\"3\"", "\"url\":\"2\""));
            lucene.checkSearch(
                "/search?prefix=0&get=url&sort=url&text=body_text:миру"
                + "&postfilter=url+==+2",
                TestSearchBackend.prepareResult("\"url\":\"2\""));
        }
    }

    @Test
    public void testNullScore() throws Exception {
        try (TestSearchBackend lucene = new TestSearchBackend(
                this,
                new File(
                    Paths.getSourcePath(
                        "mail/search/mail/search_backend_mail_config/files"
                        + "/search_backend_test.conf_mail"))))
        {
            lucene.add(
                "\"url\":\"1\",\"body_text\":\"мир\"",
                "\"url\":\"2\",\"body_text\":\"миру мир\"",
                "\"url\":\"3\",\"body_text\":\"мир дверь мяч\"");
            lucene.checkSearch(
                "/search?prefix=0&get=**&sort=url&text=body_text:миру",
                TestSearchBackend.prepareResult(
                    "\"url\":\"3\"",
                    "\"url\":\"2\"",
                    "\"url\":\"1\""));
            lucene.checkSearch(
                "/search?prefix=0&get=**&sort=url&text=body_text:миру"
                + "&scorer=null",
                TestSearchBackend.prepareResult(
                    "\"url\":\"3\"",
                    "\"url\":\"2\"",
                    "\"url\":\"1\""));
            try (
                CloseableHttpClient client = HttpClients.createDefault();
                CloseableHttpResponse response = client.execute(
                new HttpGet("http://localhost:"
                    + lucene.searchPort()
                    + "/search?prefix=1&text=body_text:миру&get=url"
                    + "&scorer=invalid")))
            {
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_BAD_REQUEST,
                    response);
            }
        }
    }

    @Test
    public void testLuceneScore() throws Exception {
        try (TestSearchBackend lucene = new TestSearchBackend(
                this,
                new File(
                    Paths.getSourcePath(
                        "mail/search/mail/search_backend_mail_config/files"
                        + "/search_backend_test.conf_mail"))))
        {
            lucene.add(
                "\"url\":\"1\",\"body_text\":\"мир\", \"fid\":1",
                "\"url\":\"2\",\"body_text\":\"миру мир\",\"fid\":2",
                "\"url\":\"3\",\"body_text\":\"мир дверь мяч\", \"fid\":3");
            //using to_long here to prevent float comparison
            lucene.checkSearch(
                "/search?prefix=0&get=url,score&sort=url&text=body_text:%22мир%22"
                    + "&scorer=lucene&dp=to_long(%23score+score)",
                TestSearchBackend.prepareResult(
                    "\"url\":\"3\",\"score\":\"0\"",
                    "\"url\":\"2\",\"score\":\"0\"",
                    "\"url\":\"1\",\"score\":\"0\""));
            //check score is normalized against user doc count
            //(not total index doc count)
            //adding doc to another prefix should not affect score
            //otherwise a rounded score will be == 1
            lucene.add(
                new LongPrefix(1L),
                "\"url\":\"4\",\"body_text\":\"мир\"",
                "\"url\":\"5\",\"body_text\":\"миру мир\"",
                "\"url\":\"6\",\"body_text\":\"мир дверь мяч\"");
            //ensure all docs are in the same shard
            lucene.checkSearch(
                "/search?get=url&sort=url&text=url:*",
                TestSearchBackend.prepareResult(
                    "\"url\":\"6\"",
                    "\"url\":\"5\"",
                    "\"url\":\"4\"",
                    "\"url\":\"3\"",
                    "\"url\":\"2\"",
                    "\"url\":\"1\""));
            lucene.checkSearch(
                "/search?prefix=0&get=url,score&sort=url&text=body_text:%22мир%22"
                    + "&scorer=lucene&dp=to_long(%23score+score)",
                TestSearchBackend.prepareResult(
                    "\"url\":\"3\",\"score\":\"0\"",
                    "\"url\":\"2\",\"score\":\"0\"",
                    "\"url\":\"1\",\"score\":\"0\""));

            lucene.checkSearch(
                "/search?prefix=0&get=url,score&sort=url&text=body_text:%22мир%22"
                    + "&scorer=lucene&dp=to_long(%23score+score)",
                TestSearchBackend.prepareResult(
                    "\"url\":\"3\",\"score\":\"0\"",
                    "\"url\":\"2\",\"score\":\"0\"",
                    "\"url\":\"1\",\"score\":\"0\""));
            // now check that lucene score not 0
            JsonList result = TypesafeValueContentHandler.parse(
                lucene.getSearchOutput(
                    "/search?prefix=0&get=url,%23score&sort=%23score&text=(body_text:мяч%5E10+OR+body_text:м*)" +
                        "+AND+NOT+fid:1&scorer=lucene")).asMap().getList("hitsArray");
            Assert.assertEquals(result.size(), 2);
            Assert.assertEquals(result.get(0).asMap().getString("url"), "3");
            Assert.assertTrue(result.get(0).asMap().getDouble("#score") > 0.0);
            Assert.assertEquals(result.get(1).asMap().getString("url"), "2");
            Assert.assertTrue(result.get(1).asMap().getDouble("#score") > 0.0);
        }
    }

    @Test
    public void testPerFieldScore() throws Exception {
        try (TestSearchBackend lucene = new TestSearchBackend(
                this,
                new File(
                    Paths.getSourcePath(
                        "mail/search/mail/search_backend_mail_config/files"
                        + "/search_backend_test.conf_mail"))))
        {
            lucene.add("\"url\":\"1\",\"body_text\":\"мир\"");
            lucene.flush();
            lucene.add(
                "\"url\":\"2\",\"body_text\":\"миру мир\"",
                "\"url\":\"3\",\"body_text\":\"мир дверь мяч\"");
            lucene.checkSearch(
                "/search?prefix=0&get=**&sort=url&text=body_text:%22мир%22"
                    + "&scorer=perfield(body_text)"
                    + "&dp=round(%23body_text_exact_score)"
                    + "&dp=round(%23body_text_non_exact_score)",
                TestSearchBackend.prepareResult(
                    "\"url\":\"3\",\"#body_text_exact_freq\":\"1.0\","
                        + "\"#body_text_exact_hits\":\"1\","
                        + "\"#body_text_exact_score\":\"1\","
                        + "\"#body_text_non_exact_freq\":\"0.0\","
                        + "\"#body_text_non_exact_hits\":\"0\","
                        + "\"#body_text_non_exact_score\":\"0\"",
                    "\"url\":\"2\",\"#body_text_exact_freq\":\"1.0\","
                        + "\"#body_text_exact_hits\":\"1\","
                        + "\"#body_text_exact_score\":\"1\","
                        + "\"#body_text_non_exact_freq\":\"0.0\","
                        + "\"#body_text_non_exact_hits\":\"0\","
                        + "\"#body_text_non_exact_score\":\"0\"",
                    "\"url\":\"1\",\"#body_text_exact_freq\":\"1.0\","
                        + "\"#body_text_exact_hits\":\"1\","
                        + "\"#body_text_exact_score\":\"1\","
                        + "\"#body_text_non_exact_freq\":\"0.0\","
                        + "\"#body_text_non_exact_hits\":\"0\","
                        + "\"#body_text_non_exact_score\":\"0\""));

            lucene.checkSearch(
                "/search?prefix=0&get=**&sort=url&text=body_text:мир"
                    + "&scorer=perfield(body_text)"
                    + "&dp=round(%23body_text_exact_score)"
                    + "&dp=round(%23body_text_non_exact_score)",
                TestSearchBackend.prepareResult(
                    "\"url\":\"3\",\"#body_text_exact_freq\":\"1.0\","
                        + "\"#body_text_exact_hits\":\"1\","
                        + "\"#body_text_exact_score\":\"1\","
                        + "\"#body_text_non_exact_freq\":\"1.0\","
                        + "\"#body_text_non_exact_hits\":\"1\","
                        + "\"#body_text_non_exact_score\":\"1\"",
                    "\"url\":\"2\",\"#body_text_exact_freq\":\"1.0\","
                        + "\"#body_text_exact_hits\":\"1\","
                        + "\"#body_text_exact_score\":\"1\","
                        + "\"#body_text_non_exact_freq\":\"4.0\","
                        + "\"#body_text_non_exact_hits\":\"1\","
                        + "\"#body_text_non_exact_score\":\"1\"",
                    "\"url\":\"1\",\"#body_text_exact_freq\":\"1.0\","
                        + "\"#body_text_exact_hits\":\"1\","
                        + "\"#body_text_exact_score\":\"1\","
                        + "\"#body_text_non_exact_freq\":\"1.0\","
                        + "\"#body_text_non_exact_hits\":\"1\","
                        + "\"#body_text_non_exact_score\":\"1\""));

            //test wildcard query
            lucene.checkSearch(
                "/search?prefix=0&get=**&sort=url&text=body_text:ми*р"
                    + "&scorer=perfield(body_text)"
                    + "&dp=round(%23body_text_exact_score)"
                    + "&dp=round(%23body_text_non_exact_score)",
                TestSearchBackend.prepareResult(
                    "\"url\":\"3\",\"#body_text_exact_freq\":\"0.0\","
                        + "\"#body_text_exact_hits\":\"0\","
                        + "\"#body_text_exact_score\":\"0\","
                        + "\"#body_text_non_exact_freq\":\"1.0\","
                        + "\"#body_text_non_exact_hits\":\"1\","
                        + "\"#body_text_non_exact_score\":\"1\"",
                    "\"url\":\"2\",\"#body_text_exact_freq\":\"0.0\","
                        + "\"#body_text_exact_hits\":\"0\","
                        + "\"#body_text_exact_score\":\"0\","
                        + "\"#body_text_non_exact_freq\":\"1.0\","
                        + "\"#body_text_non_exact_hits\":\"1\","
                        + "\"#body_text_non_exact_score\":\"1\"",
                    "\"url\":\"1\",\"#body_text_exact_freq\":\"0.0\","
                        + "\"#body_text_exact_hits\":\"0\","
                        + "\"#body_text_exact_score\":\"0\","
                        + "\"#body_text_non_exact_freq\":\"1.0\","
                        + "\"#body_text_non_exact_hits\":\"1\","
                        + "\"#body_text_non_exact_score\":\"1\""));
            //test wildcard query, pruning
            lucene.checkSearch(
                "/search?prefix=0&get=**&sort=url&text=body_text:ми*р"
                    + "&scorer=perfield(body_text)&collector=pruning"
                    + "&dp=round(%23body_text_exact_score)"
                    + "&dp=round(%23body_text_non_exact_score)",
                TestSearchBackend.prepareResult(
                    "\"url\":\"3\",\"#body_text_exact_freq\":\"0.0\","
                        + "\"#body_text_exact_hits\":\"0\","
                        + "\"#body_text_exact_score\":\"0\","
                        + "\"#body_text_non_exact_freq\":\"1.0\","
                        + "\"#body_text_non_exact_hits\":\"1\","
                        + "\"#body_text_non_exact_score\":\"1\"",
                    "\"url\":\"2\",\"#body_text_exact_freq\":\"0.0\","
                        + "\"#body_text_exact_hits\":\"0\","
                        + "\"#body_text_exact_score\":\"0\","
                        + "\"#body_text_non_exact_freq\":\"1.0\","
                        + "\"#body_text_non_exact_hits\":\"1\","
                        + "\"#body_text_non_exact_score\":\"1\"",
                    "\"url\":\"1\",\"#body_text_exact_freq\":\"0.0\","
                        + "\"#body_text_exact_hits\":\"0\","
                        + "\"#body_text_exact_score\":\"0\","
                        + "\"#body_text_non_exact_freq\":\"1.0\","
                        + "\"#body_text_non_exact_hits\":\"1\","
                        + "\"#body_text_non_exact_score\":\"1\""));

            lucene.checkSearch(
                "/search?prefix=0&get=**&sort=url&text=мир+дверь+мяч"
                    + "&scorer=perfield(body_text)"
                    + "&scope=body_text"
                    + "&dp=round(%23body_text_exact_score)"
                    + "&dp=round(%23body_text_non_exact_score)",
                TestSearchBackend.prepareResult(
                    "\"url\":\"3\",\"#body_text_exact_freq\":\"3.0\","
                        + "\"#body_text_exact_hits\":\"3\","
                        + "\"#body_text_exact_score\":\"1\","
                        + "\"#body_text_non_exact_freq\":\"3.0\","
                        + "\"#body_text_non_exact_hits\":\"3\","
                        + "\"#body_text_non_exact_score\":\"1\"",
                    "\"url\":\"2\",\"#body_text_exact_freq\":\"1.0\","
                        + "\"#body_text_exact_hits\":\"1\","
                        + "\"#body_text_exact_score\":\"0\","
                        + "\"#body_text_non_exact_freq\":\"4.0\","
                        + "\"#body_text_non_exact_hits\":\"1\","
                        + "\"#body_text_non_exact_score\":\"0\"",
                    "\"url\":\"1\",\"#body_text_exact_freq\":\"1.0\","
                        + "\"#body_text_exact_hits\":\"1\","
                        + "\"#body_text_exact_score\":\"0\","
                        + "\"#body_text_non_exact_freq\":\"1.0\","
                        + "\"#body_text_non_exact_hits\":\"1\","
                        + "\"#body_text_non_exact_score\":\"0\""));
            //check phrase search normalization
            lucene.checkSearch(
                "/search?prefix=0&get=**&sort=url&text=body_text:мир.мяч"
                    + "&scorer=perfield(body_text)"
                    + "&scope=body_text"
                    + "&dp=round(%23body_text_non_exact_freq)"
                    + "&dp=round(%23body_text_non_exact_score)",
                TestSearchBackend.prepareResult(
                    "\"url\":\"3\",\"#body_text_exact_freq\":\"0.0\","
                        + "\"#body_text_exact_hits\":\"0\","
                        + "\"#body_text_exact_score\":\"0.0\","
                        + "\"#body_text_non_exact_freq\":\"0\","
                        + "\"#body_text_non_exact_hits\":\"1\","
                        + "\"#body_text_non_exact_score\":\"1\""));

            lucene.checkSearch(
                "/search?prefix=0&get=**&sort=url&text=body_text:мир.дверь"
                    + "&scorer=perfield(body_text)"
                    + "&scope=body_text"
                    + "&dp=round(%23body_text_exact_freq)"
                    + "&dp=round(%23body_text_exact_score)"
                    + "&dp=round(%23body_text_non_exact_freq)"
                    + "&dp=round(%23body_text_non_exact_score)",
                TestSearchBackend.prepareResult(
                    "\"url\":\"3\",\"#body_text_exact_freq\":\"1\","
                        + "\"#body_text_exact_hits\":\"1\","
                        + "\"#body_text_exact_score\":\"2\","
                        + "\"#body_text_non_exact_freq\":\"1\","
                        + "\"#body_text_non_exact_hits\":\"1\","
                        + "\"#body_text_non_exact_score\":\"2\""));

            lucene.checkSearch(
                "/search?prefix=0&get=**&sort=url"
                    + "&text=body_text:%22мир+дверь%22"
                    + "&scorer=perfield(body_text)"
                    + "&scope=body_text"
                    + "&dp=round(%23body_text_exact_freq)"
                    + "&dp=round(%23body_text_exact_score)"
                    + "&dp=round(%23body_text_non_exact_freq)"
                    + "&dp=round(%23body_text_non_exact_score)",
                TestSearchBackend.prepareResult(
                    "\"url\":\"3\",\"#body_text_exact_freq\":\"1\","
                        + "\"#body_text_exact_hits\":\"1\","
                        + "\"#body_text_exact_score\":\"1\","
                        + "\"#body_text_non_exact_freq\":\"0\","
                        + "\"#body_text_non_exact_hits\":\"0\","
                        + "\"#body_text_non_exact_score\":\"0\""));

            //test PassThru collector
            lucene.checkSearch(
                "/search?prefix=0&get=**"
                    + "&text=body_text:%22мир+дверь%22"
                    + "&scorer=perfield(body_text)"
                    + "&scope=body_text"
                    + "&collector=passthru"
                    + "&dp=round(%23body_text_exact_freq)"
                    + "&dp=round(%23body_text_exact_score)"
                    + "&dp=round(%23body_text_non_exact_freq)"
                    + "&dp=round(%23body_text_non_exact_score)",
                TestSearchBackend.prepareResult(
                    "\"url\":\"3\",\"#body_text_exact_freq\":\"1\","
                        + "\"#body_text_exact_hits\":\"1\","
                        + "\"#body_text_exact_score\":\"1\","
                        + "\"#body_text_non_exact_freq\":\"0\","
                        + "\"#body_text_non_exact_hits\":\"0\","
                        + "\"#body_text_non_exact_score\":\"0\""));

            //test Pruning collector's NPE:
            // proper scorer.preprocess, process sequence
            lucene.checkSearch(
                "/search?prefix=0&get=**&sort=url"
                    + "&text=(мир+OR+дверь)+AND+NOT+мяч"
                    + "&scorer=perfield(pure_body)"
                    + "&scope=body_text"
                    + "&collector=pruning"
                    + "&dp=round(%23body_text_exact_score)"
                    + "&dp=round(%23body_text_non_exact_score)",
                TestSearchBackend.prepareResult("\"url\":\"2\"", "\"url\":\"1\""));

            //test Pruning collector
            lucene.checkSearch(
                "/search?prefix=0&get=**"
                    + "&text=body_text:%22мир+дверь%22"
                    + "&scorer=perfield(body_text)"
                    + "&scope=body_text"
                    + "&collector=pruning"
                    + "&sort=url"
                    + "&dp=round(%23body_text_exact_freq)"
                    + "&dp=round(%23body_text_exact_score)"
                    + "&dp=round(%23body_text_non_exact_freq)"
                    + "&dp=round(%23body_text_non_exact_score)",
                TestSearchBackend.prepareResult(
                    "\"url\":\"3\",\"#body_text_exact_freq\":\"1\","
                        + "\"#body_text_exact_hits\":\"1\","
                        + "\"#body_text_exact_score\":\"1\","
                        + "\"#body_text_non_exact_freq\":\"0\","
                        + "\"#body_text_non_exact_hits\":\"0\","
                        + "\"#body_text_non_exact_score\":\"0\""));
        }
    }

    @Test
    public void testAttachnamePhraseQuery() throws Exception {
        try (TestSearchBackend lucene = new TestSearchBackend(
                this,
                new File(
                    Paths.getSourcePath(
                        "mail/search/mail/search_backend_mail_config/files"
                        + "/search_backend_test.conf_mail"))))
        {
            lucene.add(
                "\"url\":\"1\",\"attachname\":\"ЛР-01\"",
                "\"url\":\"2\",\"attachname\":\"ЛР-02\"",
                "\"url\":\"3\",\"attachname\":\"01-ЛР\"");
            lucene.checkSearch(
                "/search?prefix=0&get=url&sort=attachname"
                + "&text=attachname:%22ЛР-01%22",
                TestSearchBackend.prepareResult("\"url\":\"1\""));
        }
    }

    @Test
    public void testCollectorMemoryLimit() throws Exception {
        try (TestSearchBackend lucene = new TestSearchBackend(
                this,
                new File(
                    Paths.getSourcePath(
                        "mail/search/mail/search_backend_mail_config/files"
                        + "/search_backend_test.conf_mail"))))
        {
            final StringBuilder bigField = new StringBuilder(BIG_FIELD_SIZE);
            for (int i = 0; i < BIG_FIELD_SIZE; i++) {
                bigField.append('0');
            }
            lucene.add(
                "\"url\":\"1\",\"attachname\":\"" + bigField.toString() + "\"",
                "\"url\":\"2\",\"attachname\":\"" + bigField.toString() + "\"",
                "\"url\":\"3\",\"attachname\":\"" + bigField.toString() + "\"");
            //no limit
            lucene.checkSearch(
                "/search?prefix=0&get=url,attachname&sort=url"
                + "&text=url:*&asc",
                TestSearchBackend.prepareResult(
                    "\"url\":\"1\",\"attachname\":\"" + bigField.toString()
                        + "\"",
                    "\"url\":\"2\",\"attachname\":\"" + bigField.toString()
                        + "\"",
                    "\"url\":\"3\",\"attachname\":\"" + bigField.toString()
                        + "\""));
            //limit of 1 bytes should leave at least 1 hit
            lucene.checkSearch(
                "/search?prefix=0&get=url,attachname&sort=url"
                + "&text=url:*&asc&memory-limit=1",
                TestSearchBackend.prepareResult(
                    3,
                    "\"url\":\"1\",\"attachname\":\"" + bigField.toString()
                        + "\""));

            //limit 5k
            lucene.checkSearch(
                "/search?prefix=0&get=url,attachname,mid"
                + "&sort=multi(url,attachname)"
                + "&text=url:*&asc&memory-limit=5k",
                TestSearchBackend.prepareResult(
                    3,
                    "\"url\":\"1\",\"attachname\":\"" + bigField.toString()
                        + "\",\"mid\":null",
                    "\"url\":\"2\",\"attachname\":\"" + bigField.toString()
                        + "\",\"mid\":null"));
            //limit 5k no step2 protection
            lucene.checkSearch(
                "/search?prefix=0&get=url,attachname,mid"
                + "&sort=url"
                + "&text=url:*&asc&memory-limit=5k",
                TestSearchBackend.prepareResult(
                    3,
                    "\"url\":\"1\",\"attachname\":\"" + bigField.toString()
                        + "\",\"mid\":null",
                    "\"url\":\"2\",\"attachname\":\"" + bigField.toString()
                        + "\",\"mid\":null"));
        }
    }

    @Test
    public void testCollectorMemoryLimitSorterRemoval() throws Exception {
        try (TestSearchBackend lucene = new TestSearchBackend(
                this,
                new File(
                    Paths.getSourcePath(
                        "mail/search/mail/search_backend_mail_config/files"
                        + "/search_backend_test.conf_mail"))))
        {
            StringBuilder sb = new StringBuilder(BIG_FIELD_SIZE);
            for (int i = 0; i < BIG_FIELD_SIZE; i++) {
                sb.append('0');
            }
            String bigField = new String(sb);
            lucene.add(
                "\"url\":\"1\",\"attachname\":\"a\"",
                "\"url\":\"2\",\"attachname\":\"b\"",
                "\"url\":\"3\",\"attachname\":\"" + bigField + '"');
            // no limit
            lucene.checkSearch(
                "/search?prefix=0&get=url,attachname&sort=url"
                + "&text=url:*&asc",
                TestSearchBackend.prepareResult(
                    "\"url\":\"1\",\"attachname\":\"a\"",
                    "\"url\":\"2\",\"attachname\":\"b\"",
                    "\"url\":\"3\",\"attachname\":\"" + bigField + '"'));
            // should leave only last hit with removal of first two
            lucene.checkSearch(
                "/search?prefix=0&get=url,attachname&sort=url"
                + "&text=url:*&memory-limit=" + (BIG_FIELD_SIZE >> 1),
                TestSearchBackend.prepareResult(
                    3,
                    "\"url\":\"3\",\"attachname\":\"" + bigField + '"'));
            lucene.add(
                "\"url\":\"4\",\"attachname\":\"" + bigField + '"',
                "\"url\":\"5\",\"attachname\":\"a\"",
                "\"url\":\"6\",\"attachname\":\"b\"",
                "\"url\":\"7\",\"attachname\":\"c\"",
                "\"url\":\"8\",\"attachname\":\"d\"",
                "\"url\":\"9\",\"attachname\":\"e\"");
            lucene.checkSearch(
                "/search?prefix=0&get=url,attachname&sort=url&length=4"
                + "&text=url:*&memory-limit=" + (BIG_FIELD_SIZE >> 1),
                TestSearchBackend.prepareResult(
                    9,
                    "\"url\":\"9\",\"attachname\":\"e\"",
                    "\"url\":\"8\",\"attachname\":\"d\"",
                    "\"url\":\"7\",\"attachname\":\"c\"",
                    "\"url\":\"6\",\"attachname\":\"b\""));
        }
    }

    @Test
    public void testPrefixlessPrefixFieldSearch() throws Exception {
        try (TestSearchBackend lucene = new TestSearchBackend(
                this,
                new File(
                    Paths.getSourcePath(
                        "mail/search/disk/search_backend_disk_config/files"
                        + "/search_backend.conf"))))
        {
            lucene.add(
                "\"version\":1,\"id\":\"1\",\"peach_url\":\"a\","
                + "\"peach_queue\":\"q1\"",
                "\"version\":1,\"id\":\"2\",\"peach_url\":\"b\","
                + "\"peach_queue\":\"q1\"",
                "\"version\":1,\"id\":\"3\",\"peach_url\":\"c\","
                + "\"peach_queue\":\"q1\"",
                "\"version\":1,\"id\":\"4\",\"peach_url\":\"d\","
                + "\"peach_queue\":\"q2\"",
                "\"version\":1,\"id\":\"5\",\"peach_url\":\"e\"");
            lucene.add(
                new LongPrefix(1L),
                "\"version\":1,\"id\":\"6\",\"peach_url\":\"a\","
                + "\"peach_queue\":\"q1\"",
                "\"version\":1,\"id\":\"7\",\"peach_url\":\"a\","
                + "\"peach_queue\":\"q2\"",
                "\"version\":1,\"id\":\"8\",\"peach_url\":\"a\","
                + "\"peach_queue\":\"q2\"",
                "\"version\":1,\"id\":\"9\",\"peach_url\":\"a\"");
            lucene.flush();
            // no limit
            lucene.checkSearch(
                "/search-peach?get=__prefix,peach_queue"
                + "&dp=multi(__prefix,peach_queue+peach_group)"
                + "&sort=peach_group&group=peach_group&merge_func=count"
                + "&text=peach_url:*&prefixless-field=peach_url",
                TestSearchBackend.prepareResult(
                    "\"__prefix\":\"1\",\"peach_queue\":\"q2\","
                    + "\"merged_docs_count\":1",
                    "\"__prefix\":\"1\",\"peach_queue\":\"q1\"",
                    "\"__prefix\":\"1\",\"peach_queue\":null",
                    "\"__prefix\":\"0\",\"peach_queue\":\"q2\"",
                    "\"__prefix\":\"0\",\"peach_queue\":\"q1\","
                    + "\"merged_docs_count\":2",
                    "\"__prefix\":\"0\",\"peach_queue\":null"));
            HttpAssert.assertStat(
                "peach-total_ammm",
                "1",
                lucene.searchPort(),
                DISK_UNISTAT_MAX_SIGNALS);
            HttpAssert.assertStat(
                "unit-test-peach-total_ammm",
                "1",
                lucene.searchPort(),
                DISK_UNISTAT_MAX_SIGNALS);
        }
    }

    @Test
    public void testFieldsCache() throws Exception {
        File root = Files.createTempDirectory("testFieldsCache").toFile();
        try (Daemon daemon =
                new Daemon(SearchBackendTestBase.config(root, NUMBER_CONFIG_CACHE));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(
                new StringEntity(
                    "{\"prefix\":1,\"docs\":["
                    + "{\"keyword\":\"first\",\"byte-array\":\"010203\"},"
                    + "{\"keyword\":\"second\",\"byte-array\":\"7F7E7D\"},"
                    + "{\"keyword\":\"third\",\"byte-array\":\"8081FF\"},"
                    + "{\"keyword\":\"forth\"}]}",
                StandardCharsets.UTF_8));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);
            //flush
            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                client,
                new HttpGet("http://localhost:" + daemon.jsonServerPort()
                    + "/flush?wait=true"));
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet("http://localhost:"
                    + daemon.searchServerPort() + "/search?prefix=1&text="
                    + "keyword:*&get=keyword,product"
                    + "&dp=chex-dot-product(byte-array+010002+product)"
                    + "&sort=product")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    "{\"hitsCount\":4,\"hitsArray\":["
                    + "{\"keyword\":\"second\",\"product\":\"377\"},"
                    + "{\"keyword\":\"first\",\"product\":\"7\"},"
                    + "{\"keyword\":\"third\",\"product\":\"-130\"},"
                    + "{\"keyword\":\"forth\",\"product\":null}]}",
                    CharsetUtils.toString(response.getEntity()).trim());
            }
            // Check comparator
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet("http://localhost:"
                    + daemon.searchServerPort() + "/search?prefix=1&text="
                    + "keyword:*&get=keyword,byte-array"
                    + "&sort=byte-array")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    "{\"hitsCount\":4,\"hitsArray\":["
                    + "{\"keyword\":\"second\",\"byte-array\":[127,126,125]},"
                    + "{\"keyword\":\"first\",\"byte-array\":[1,2,3]},"
                    + "{\"keyword\":\"third\",\"byte-array\":[-128,-127,-1]},"
                    + "{\"keyword\":\"forth\",\"byte-array\":null}]}",
                    CharsetUtils.toString(response.getEntity()).trim());
            }
        }
    }

    @Test
    public void testLeftJoinDp() throws Exception {
        File root = Files.createTempDirectory("testLeftJoinDp").toFile();
        try (TestSearchBackend lucene = new TestSearchBackend(SearchBackendTestBase.config(root.getCanonicalPath(),
            "prefix_type = long\nprimary_key = id\n"
            + "[field.id]\n"
                + "tokenizer = keyword\n"
                + "store = true\n"
                + "prefixed = true\n"
                + "analyze = true\n"
                + "attribute = true\n"
                + "bloom = true\n"
                + "[field.key]\n"
                + "tokenizer = keyword\n"
                + "filters = lowercase|replace:ё:е\n"
                + "prefixed = true\n"
                + "store = true\n"
                + "analyze = true\n"
                + "attribute = true\n"
                + "[field.type]\n"
                + "tokenizer = keyword\n"
                + "store = true\n"
                + "prefixed = true\n"
                + "analyze = true\n"
                + "attribute = true\n"
                + "[field.name]\n"
                + "tokenizer = keyword\n"
                + "store = true\n"
                + "prefixed = true\n"
                + "analyze = true\n"
                + "attribute = true\n"
                + "[field.parent_fid]\n"
                + "tokenizer = keyword\n"
                + "store = true\n"
                + "prefixed = true\n"
                + "analyze = true\n"
                + "attribute = true\n"
                + "[field.version]\n"
                + "tokenizer = keyword\n"
                + "store = true\n"
                + "prefixed = true\n"
                + "analyze = true\n"
                + "attribute = true\n"
                + "\n"
                + "[field.fid]\n"
                + "tokenizer = keyword\n"
                + "store = true\n"
                + "prefixed = true\n"
                + "analyze = true\n"
                + "attribute = true",
            10));
             CloseableHttpClient client = HttpClients.createDefault())
        {
            lucene.add(
                new LongPrefix(1L),
                "\"id\":1,\"parent_fid\":\"275f1b5cab867099a882c7781f0df56b"
                    + "\",\"version\":\"10\",\"type\":\"file\"",
                "\"id\":2,\"type\":\"dir\", \"key\":\"/disk/Mega Folder\","
                    + "\"fid\":\"275f1b5cab867099a882c7781f0df56b\"");

            lucene.checkSearch(
                "/search?prefix=1&text=type:file&get=**,-version"
                    + "&dp=left_join(parent_fid,fid,type:dir,key)",
                new JsonChecker(
                    "{\"hitsCount\":1,"
                        + "\"hitsArray\":["
                        + "{\"id\":\"1\","
                        + "\"key\":\"/disk/Mega Folder\","
                        + "\"type\":\"file\","
                        + "\"parent_fid\":\"275f1b5cab867099a882c7781f0df56b\"}]}"));

            lucene.add(
                new LongPrefix(1L),
                "\"id\":3,\"parent_fid\":\"qergqerg\",\"version\":\"11\","
                    + "\"type\":\"file\"",
                "\"id\":4,\"type\":\"dir\", \"key\":\"/disk/Other/Folder\","
                    + "\"fid\":\"qergqerg\"");

            lucene.checkSearch(
                "/search?prefix=1&text=type:file&get=**,-version&dp=left_join"
                    + "(parent_fid,fid,type:dir,key+parent_key)",
                new JsonChecker(
                    "{\"hitsCount\":2,"
                        + "\"hitsArray\":[{\"id\":\"1\",\"type\":\"file\","
                        + "\"parent_fid\":\"275f1b5cab867099a882c7781f0df56b\","
                        + "\"parent_key\":\"/disk/Mega Folder\"},{\"id\":\"3\","
                        + "\"type\":\"file\",\"parent_fid\":\"qergqerg\","
                        + "\"parent_key\":\"/disk/Other/Folder\"}]}\n"));

            lucene.add(
                new LongPrefix(2L),
                "\"id\":0,\"parent_fid\":\"abc1\",\"version\":\"16\","
                    + "\"type\":\"file\"",
                "\"id\":1,\"parent_fid\":\"abc3\",\"version\":\"14\","
                    + "\"type\":\"file\"",
                "\"id\":2,\"type\":\"dir\", \"name\": \"DirName1\", "
                    + "\"key\":\"/disk/Other/Folder1\",\"fid\":\"abc1\"",
                "\"id\":3,\"type\":\"dir\", \"name\": \"DirName2\", "
                    + "\"key\":\"/disk/Other/Folder2\",\"fid\":\"abc2\"",
                "\"id\":4,\"type\":\"dir\", \"name\": \"DirName3\", "
                    + "\"key\":\"/disk/Other/Folder3\",\"fid\":\"abc3\"",
                "\"id\":5,\"parent_fid\":\"abc1\",\"version\":\"13\","
                    + "\"type\":\"file\"");

            lucene.checkSearch(
                "/search?prefix=2&text=type:file&get=**"
                    + "&dp=left_join(parent_fid,fid,type:dir,key+parent_key,"
                    + "name+parent_name)&sort=version",
                new JsonChecker(
                    "{\n"
                        + "    \"hitsCount\": 3,\n"
                        + "    \"hitsArray\": [\n"
                        + "        {\n"
                        + "            \"id\": \"0\",\n"
                        + "            \"type\": \"file\",\n"
                        + "            \"parent_fid\": \"abc1\",\n"
                        + "            \"version\": \"16\",\n"
                        + "            \"parent_key\": "
                        + "\"/disk/Other/Folder1\",\n"
                        + "            \"parent_name\": \"DirName1\"\n"
                        + "        },\n"
                        + "        {\n"
                        + "            \"id\": \"1\",\n"
                        + "            \"type\": \"file\",\n"
                        + "            \"parent_fid\": \"abc3\",\n"
                        + "            \"version\": \"14\",\n"
                        + "            \"parent_key\": "
                        + "\"/disk/Other/Folder3\",\n"
                        + "            \"parent_name\": \"DirName3\"\n"
                        + "        },\n"
                        + "        {\n"
                        + "            \"id\": \"5\",\n"
                        + "            \"type\": \"file\",\n"
                        + "            \"parent_fid\": \"abc1\",\n"
                        + "            \"version\": \"13\",\n"
                        + "            \"parent_key\": "
                        + "\"/disk/Other/Folder1\",\n"
                        + "            \"parent_name\": \"DirName1\"\n"
                        + "        }\n"
                        + "    ]\n"
                        + "}"));

            lucene.add(
                new LongPrefix(3L),
                "\"id\":0,\"parent_fid\":\"abc1\",\"version\":\"16\","
                    + "\"type\":\"file\"",
                "\"id\":5,\"parent_fid\":\"abc3\",\"version\":\"13\","
                    + "\"type\":\"file\"");

            lucene.checkSearch(
                "/search?prefix=3&text=type:file&get=**"
                    + "&dp=left_join(parent_fid,fid,type:dir,"
                    + "key+parent_key,"
                    + "name+parent_name)&sort=version&hr",
                new JsonChecker("{\n"
                    + "    \"hitsCount\": 2,\n"
                    + "    \"hitsArray\": [\n"
                    + "        {\n"
                    + "            \"id\": \"0\",\n"
                    + "            \"type\": \"file\",\n"
                    + "            \"parent_fid\": \"abc1\",\n"
                    + "            \"version\": \"16\"\n"
                    + "        },\n"
                    + "        {\n"
                    + "            \"id\": \"5\",\n"
                    + "            \"type\": \"file\",\n"
                    + "            \"parent_fid\": \"abc3\",\n"
                    + "            \"version\": \"13\"\n"
                    + "        }\n"
                    + "    ]\n"
                    + "}"));

            lucene.checkSearch(
                "/search?prefix=3&text=type:file&get=**"
                    + "&dp=left_join(parent_fid,fid,,"
                    + "key+parent_key,"
                    + "name+parent_name)&sort=version&hr",
                new JsonChecker("{\n"
                    + "    \"hitsCount\": 2,\n"
                    + "    \"hitsArray\": [\n"
                    + "        {\n"
                    + "            \"id\": \"0\",\n"
                    + "            \"type\": \"file\",\n"
                    + "            \"parent_fid\": \"abc1\",\n"
                    + "            \"version\": \"16\"\n"
                    + "        },\n"
                    + "        {\n"
                    + "            \"id\": \"5\",\n"
                    + "            \"type\": \"file\",\n"
                    + "            \"parent_fid\": \"abc3\",\n"
                    + "            \"version\": \"13\"\n"
                    + "        }\n"
                    + "    ]\n"
                    + "}"));
        }
    }

    @Test
    public void testFilterDp() throws Exception {
        File root = Files.createTempDirectory("testFilderDp").toFile();
        try (TestSearchBackend lucene = new TestSearchBackend(
            SearchBackendTestBase.config(
                root.getCanonicalPath(),
                "prefix_type = long\nprimary_key = id\n"
                    + "[field.id]\n"
                    + "tokenizer = keyword\n"
                    + "store = true\n"
                    + "prefixed = true\n"
                    + "analyze = true\n"
                    + "attribute = true\n"
                    + "bloom = true\n"
                    + "[field.counter]\n"
                    + "tokenizer = keyword\n"
                    + "store = true\n"
                    + "prefixed = true\n"
                    + "analyze = true\n"
                    + "attribute = true\n"
                    + "type = integer\n"
                    + "[field.type]\n"
                    + "tokenizer = keyword\n"
                    + "store = true\n"
                    + "prefixed = true\n"
                    + "analyze = true\n"
                    + "attribute = true\n",
                10)))
        {
            lucene.add(
                new LongPrefix(0L),
                "\"id\":1,\"counter\":1,\"type\":\"file\"",
                "\"id\":2,\"counter\":4,\"type\":\"file\"",
                "\"id\":3,\"counter\":6,\"type\":\"file\"",
                "\"id\":4,\"counter\":3,\"type\":\"file\"");

            lucene.checkSearch(
                "/search?prefix=0&text=type:file&get=id&debug"
                    + "&dp=filter_cmp(counter,%3E%3D,4)",
                new JsonChecker("{\"hitsCount\":2,"
                    + "\"hitsArray\":["
                    + "{\"id\":\"2\"},"
                    + "{\"id\":\"3\"}]}"));

            // test sorted
            lucene.checkSearch(
                "/search?prefix=0&text=type:file&get=id&debug"
                    + "&dp=filter_cmp(counter,%3E%3D,4)&sort=id",
                new JsonChecker("{\"hitsCount\":2,"
                    + "\"hitsArray\":["
                    + "{\"id\":\"3\"},"
                    + "{\"id\":\"2\"}]}"));

            // with combination of dps
            lucene.checkSearch(
                "/search?prefix=0&text=type:file&get=id,cnt&debug"
                    + "&dp=csum(counter,5+cnt)"
                    + "&dp=filter_cmp(cnt,%3E%3D,10)&sort=id",
                new JsonChecker("{\"hitsCount\":1,"
                    + "\"hitsArray\":["
                    + "{\"id\":\"3\", \"cnt\": \"11\"}]}"));
            // with combination of dps
            lucene.checkSearch(
                "/search?prefix=0&text=type:file&get=id&debug"
                    + "&dp=csum(counter,5+cnt)"
                    + "&dp=csum(cnt,5+cnt2)"
                    + "&dp=filter_cmp(cnt2,%3E%3D,12)&sort=cnt2",
                new JsonChecker("{\"hitsCount\":3,"
                    + "\"hitsArray\":["
                    + "{\"id\":\"3\"},"
                    + "{\"id\":\"2\"},"
                    + "{\"id\":\"4\"}]}"));

            // with combination of dps
            lucene.checkSearch(
                "/search?prefix=0&text=type:file&get=id,cnt3&debug"
                    + "&dp=csum(counter,5+cnt)"
                    + "&dp=csum(cnt,5+cnt2)"
                    + "&dp=csum(cnt2,5+cnt3)"
                    + "&dp=filter_cmp(cnt3,%3E%3D,19)&sort=cnt3",
                new JsonChecker("{\"hitsCount\":2,"
                    + "\"hitsArray\":["
                    + "{\"id\":\"3\", \"cnt3\": \"21\"},"
                    + "{\"id\":\"2\", \"cnt3\": \"19\"}]}"));
        }
    }

    @Test
    public void testTreeRootDp() throws Exception {
        File root = Files.createTempDirectory("testTreeRootDp1").toFile();
        try (TestSearchBackend lucene = new TestSearchBackend(SearchBackendTestBase.config(root.getCanonicalPath(),
            "prefix_type = long\nprimary_key = id\n"
                + "[field.id]\n"
                + "tokenizer = keyword\n"
                + "store = true\n"
                + "prefixed = true\n"
                + "analyze = true\n"
                + "attribute = true\n"
                + "bloom = true\n"
                + "[field.key]\n"
                + "tokenizer = keyword\n"
                + "store = true\n"
                + "prefixed = true\n"
                + "analyze = true\n"
                + "attribute = true\n"
                + "bloom = true\n"
                + "[field.type]\n"
                + "tokenizer = keyword\n"
                + "store = true\n"
                + "prefixed = true\n"
                + "analyze = true\n"
                + "attribute = true\n"
                + "[field.name]\n"
                + "tokenizer = keyword\n"
                + "store = true\n"
                + "prefixed = true\n"
                + "analyze = true\n"
                + "attribute = true\n"
                + "[field.parent_fid]\n"
                + "tokenizer = keyword\n"
                + "store = true\n"
                + "prefixed = true\n"
                + "analyze = true\n"
                + "attribute = true\n"
                + "[field.rev]\n"
                + "tokenizer = keyword\n"
                + "store = true\n"
                + "prefixed = true\n"
                + "analyze = true\n"
                + "type = long\n"
                + "attribute = true\n"
                + "[field.fid]\n"
                + "tokenizer = keyword\n"
                + "store = true\n"
                + "prefixed = true\n"
                + "analyze = true\n"
                + "attribute = true",
            10));
             CloseableHttpClient client = HttpClients.createDefault())
        {
            lucene.add(
                new LongPrefix(0L),
                "\"id\":1,\"parent_fid\":\"fid_100\",\"type\":\"file\","
                    + "\"name\":\"lucenejoin.java\",\"key\": \"none\",\"rev\":2",
                "\"id\":2,\"type\":\"dir\", \"parent_fid\":\"fid_300\","
                    + "\"fid\":\"fid_100\",\"name\": \"Bidlocod\",\"rev\":4",
                "\"id\":3,\"type\":\"dir\", \"parent_fid\":\"fid_400\","
                    + "\"fid\":\"fid_300\",\"name\": \"Code\",\"rev\": 3",
                "\"id\":4,\"type\":\"dir\","
                    + "\"fid\":\"fid_400\",\"name\": \"disk\",\"rev\":1",
                "\"id\":5,\"type\":\"dir\", \"parent_fid\":\"fid_400\","
                    + "\"fid\":\"fid_500\",\"name\": \"Other\",\"rev\":5",
                "\"id\":6,\"parent_fid\":\"fid_500\",\"type\":\"file\","
                    + "\"name\":\"other.java\",\"key\":\"none\",\"rev\":1",
                "\"id\":7,\"parent_fid\":\"fid_400\",\"type\":\"file\","
                    + "\"name\":\"under_root.java\",\"key\":\"none\",\"rev\":6"
                );

            lucene.checkSearch(
                "/search?prefix=0&text=fid:(fid_400+OR+fid_500)"
                    + "&get=fid,sum,cnt&debug"
                    + "&dp=tree_calc(parent_fid,fid,sum_count(id,type,file+sum,cnt))",
                new JsonChecker("{\"hitsCount\":2,"
                    + "\"hitsArray\":["
                    + "{\"sum\":\"14\",\"cnt\":\"3\",\"fid\": \"fid_400\"},"
                    + "{\"sum\":\"6\",\"fid\": \"fid_500\",\"cnt\":\"1\"}]}"));

            lucene.checkSearch(
                "/search?prefix=0&text=fid:(fid_400+OR+fid_500)"
                    + "&get=files&debug"
                    + "&dp=tree_calc(parent_fid,fid,print())",
                new JsonChecker("{\"hitsCount\":2,"
                    + "\"hitsArray\":["
                    + "{\"files\":\"/under_root.java\\n/Other/other.java\\n"
                    + "/Code/Bidlocod/lucenejoin.java\\n\"},"
                    + "{\"files\": \"/other.java\\n\"}]}"));

            lucene.checkSearch(
                "/search?prefix=0&text=type:file&get=key&debug"
                    + "&dp=tree_root(parent_fid,fid,type:dir,concat(/),"
                    + "name+key)",
                new JsonChecker("{\"hitsCount\":3,"
                    + "\"hitsArray\":["
                    + "{\"key\":\"/disk/Code/Bidlocod/lucenejoin.java\"},"
                    + "{\"key\":\"/disk/Other/other.java\"},"
                    + "{\"key\": \"/disk/under_root.java\"}]}"));
            lucene.checkSearch(
                "/search?prefix=0&text=type:file&get=key,max_rev&debug"
                    + "&dp=tree_root_rev(parent_fid,fid,type:dir,disk_concat(/),"
                    + "name,rev+key,max_rev)"
                    + "&sort=id&disk-allowed-roots=disk,attach",
                new JsonChecker("{\"hitsCount\":3,"
                    + "\"hitsArray\":["
                    + "{\"key\": \"/disk/under_root.java\",\"max_rev\":\"6\"},"
                    + "{\"key\":\"/disk/Other/other.java\",\"max_rev\":\"5\"},"
                    + "{\"key\":\"/disk/Code/Bidlocod/lucenejoin.java\","
                    + "\"max_rev\": \"4\"}]}"));

             // check sorted collector
            lucene.checkSearch(
                "/search?prefix=0&text=type:file&get=key"
                    + "&dp=tree_root(parent_fid,fid,type:dir,concat(/),"
                    + "name+key)&sort=type",
                new JsonChecker("{\"hitsCount\":3,"
                    + "\"hitsArray\":["
                    + "{\"key\":\"/disk/Code/Bidlocod/lucenejoin.java\"},"
                    + "{\"key\":\"/disk/Other/other.java\"},"
                    + "{\"key\": \"/disk/under_root.java\"}]}"));

            // more complex structure
            lucene.add(
                new LongPrefix(0L),
                "\"id\":8,\"parent_fid\":\"fid_100\",\"type\":\"file\","
                    + "\"name\":\"lucenejoin2.java\",\"key\":\"none\","
                    + "\"rev\":7",
                "\"id\":9,\"parent_fid\":\"fid_300\",\"type\":\"file\","
                    + "\"name\": \"lucenejoin3.java\",\"key\": \"none\","
                    + "\"rev\":1");

            lucene.checkSearch(
                "/search?prefix=0&text=type:file&get=key,max_rev&debug"
                    + "&dp=tree_root_rev(parent_fid,fid,type:dir,disk_concat(/),"
                    + "name,rev+key,max_rev)"
                    + "&sort=id&disk-allowed-roots=disk,attach",
                new JsonChecker("{\"hitsCount\":5,"
                    + "\"hitsArray\":["
                    + "{\"key\":\"/disk/Code/lucenejoin3.java\","
                    + "\"max_rev\": \"3\"},"
                    + "{\"key\":\"/disk/Code/Bidlocod/lucenejoin2.java\","
                    + "\"max_rev\": \"7\"},"
                    + "{\"key\":\"/disk/under_root.java\",\"max_rev\": \"6\"},"
                    + "{\"key\":\"/disk/Other/other.java\",\"max_rev\": \"5\"},"
                    + "{\"key\":\"/disk/Code/Bidlocod/lucenejoin.java\","
                    + " \"max_rev\": \"4\"}]}"));


            lucene.checkSearch(
                "/search?prefix=0&text=type:file&get=key&debug"
                    + "&dp=tree_root(parent_fid,fid,type:dir,concat(/),"
                    + "name+key)&sort=id",
                new JsonChecker("{\"hitsCount\":5,"
                    + "\"hitsArray\":["
                    + "{\"key\":\"/disk/Code/lucenejoin3.java\"},"
                    + "{\"key\":\"/disk/Code/Bidlocod/lucenejoin2.java\"},"
                    + "{\"key\":\"/disk/under_root.java\"},"
                    + "{\"key\": \"/disk/Other/other.java\"},"
                    + "{\"key\": \"/disk/Code/Bidlocod/lucenejoin.java\"}]}"));

            // add leafs without roots, heer parent_if for fid_700 do not exist
            lucene.add(
                new LongPrefix(0L),
                "\"id\":90,\"parent_fid\":\"fid_700\",\"type\":\"file\","
                    + "\"name\": \"brokenfile1.java\",\"key\": \"none\"",
                "\"id\":91,\"parent_fid\":\"fid_800\",\"type\":\"file\","
                    + "\"name\": \"brokenfile2.java\",\"key\": \"none\"",
                "\"id\":92,\"parent_fid\":\"fid_800\",\"type\":\"file\","
                    + "\"name\": \"brokenfile3.java\",\"key\": \"none\"",
                "\"id\":93,\"type\":\"dir\",\"parent_fid\":\"fid_700\","
                    + "\"fid\":\"fid_800\",\"name\": \"brokenfolder\"",
                "\"id\":94,\"type\":\"dir\",\"parent_fid\":\"fid_750\","
                    + "\"fid\":\"fid_700\",\"name\": \"brokenroot\"");

            lucene.checkSearch(
                "/search?prefix=0&text=type:file&get=key&debug"
                    + "&dp=tree_root(parent_fid,fid,type:dir,disk_concat(/),"
                    + "name+key)&sort=id&disk-allowed-roots=disk,attach",
                new JsonChecker("{\"hitsCount\":5,"
                    + "\"hitsArray\":["
                    + "{\"key\":\"/disk/Code/lucenejoin3.java\"},"
                    + "{\"key\":\"/disk/Code/Bidlocod/lucenejoin2.java\"},"
                    + "{\"key\":\"/disk/under_root.java\"},"
                    + "{\"key\": \"/disk/Other/other.java\"},"
                    + "{\"key\": \"/disk/Code/Bidlocod/lucenejoin.java\"}]}"));

            lucene.checkSearch(
                "/search?prefix=0&text=type:file&get=key&debug"
                    + "&dp=tree_root(parent_fid,fid,type:dir,disk_concat(/),"
                    + "name+key)&sort=id"
                    + "&disk-allowed-roots=disk,attach,brokenroot",
                new JsonChecker("{\"hitsCount\":8,"
                    + "\"hitsArray\":["
                    + "{\"key\":\"/brokenroot/brokenfolder/brokenfile3.java\"},"
                    + "{\"key\":\"/brokenroot/brokenfolder/brokenfile2.java\"},"
                    + "{\"key\":\"/brokenroot/brokenfile1.java\"},"
                    + "{\"key\":\"/disk/Code/lucenejoin3.java\"},"
                    + "{\"key\":\"/disk/Code/Bidlocod/lucenejoin2.java\"},"
                    + "{\"key\":\"/disk/under_root.java\"},"
                    + "{\"key\": \"/disk/Other/other.java\"},"
                    + "{\"key\": \"/disk/Code/Bidlocod/lucenejoin.java\"}]}"));


            lucene.checkSearch(
                "/search?prefix=0&text=type:file&get=key,max_rev&debug"
                    + "&dp=tree_root_rev(parent_fid,fid,type:dir,disk_concat(/),"
                    + "name,rev+key,max_rev)"
                    + "&sort=id&disk-allowed-roots=disk,attach",
                new JsonChecker("{\"hitsCount\":5,"
                    + "\"hitsArray\":["
                    + "{\"key\":\"/disk/Code/lucenejoin3.java\","
                    + "\"max_rev\": \"3\"},"
                    + "{\"key\":\"/disk/Code/Bidlocod/lucenejoin2.java\","
                    + "\"max_rev\": \"7\"},"
                    + "{\"key\":\"/disk/under_root.java\",\"max_rev\": \"6\"},"
                    + "{\"key\": \"/disk/Other/other.java\",\"max_rev\": \"5\"},"
                    + "{\"key\": \"/disk/Code/Bidlocod/lucenejoin.java\","
                    + " \"max_rev\": \"4\"}]}"));


            lucene.checkSearch(
                "/search?prefix=0&text=type:file&get=key,max_rev&debug"
                    + "&dp=tree_root_rev(parent_fid,fid,type:dir,disk_concat(/),"
                    + "name,rev+key,max_rev)"
                    + "&sort=id&disk-allowed-roots=disk,attach,brokenroot",
                new JsonChecker("{\"hitsCount\":5,"
                    + "\"hitsArray\":["
                    + "{\"key\":\"/disk/Code/lucenejoin3.java\","
                    + "\"max_rev\": \"3\"},"
                    + "{\"key\":\"/disk/Code/Bidlocod/lucenejoin2.java\","
                    + "\"max_rev\": \"7\"},"
                    + "{\"key\":\"/disk/under_root.java\",\"max_rev\": \"6\"},"
                    + "{\"key\": \"/disk/Other/other.java\",\"max_rev\": \"5\"},"
                    + "{\"key\": \"/disk/Code/Bidlocod/lucenejoin.java\","
                    + " \"max_rev\": \"4\"}]}"));
            // test with revision
        }
    }

    @Test
    public void testIfDocProcessor() throws Exception {
        try (TestSearchBackend lucene = new TestSearchBackend(
            this,
            new File(
                Paths.getSourcePath(
                    "mail/search/mail/search_backend_mail_config/files"
                    + "/search_backend_test.conf_mail"))))
        {
            lucene.add(
                "\"url\":\"first\",\"mid\":1",
                "\"url\":\"second\",\"msg_id\":1,\"mid\":1",
                "\"url\":\"third\",\"msg_id\":0,\"mid\":1",
                "\"url\":\"forth\",\"mid\":1");
            lucene.checkSearch(
                "/search?prefix=0&text=url:*&get=url,result,result2"
                + "&sort=mid&asc"
                + "&dp=const(0+success)"
                + "&dp=const(+unsuccess)"
                + "&dp=if(msg_id,success,unsuccess+result)"
                + "&dp=contains(url,ir+conditional)"
                + "&dp=if(conditional,url,unsuccess+result2)",
                TestSearchBackend.prepareResult(
                    "\"url\":\"first\",\"result\":null,\"result2\":\"first\"",
                    "\"url\":\"second\",\"result\":\"0\",\"result2\":\"\"",
                    "\"url\":\"third\",\"result\":\"\",\"result2\":\"third\"",
                    "\"url\":\"forth\",\"result\":null,\"result2\":\"\""));
        }
    }

    @Test
    public void testEqualsDocProcessor() throws Exception {
        try (TestSearchBackend lucene = new TestSearchBackend(
            this,
            new File(
                Paths.getSourcePath(
                    "mail/search/mail/search_backend_mail_config/files"
                    + "/search_backend_test.conf_mail"))))
        {
            lucene.add(
                "\"url\":0,\"hdr_from\":null",
                "\"url\":1,\"hdr_from\":\"first\"",
                "\"url\":2,\"hdr_from\":\"ya@yandex.ru\"",
                "\"url\":3,\"hdr_from\":32760",
                "\"url\":4,\"hdr_from\":1234.560900",
                "\"url\":5,\"hdr_from\":\"\"");
            lucene.checkSearch(
                "/search?prefix=0&text=url:*&get=url,result&sort=url&asc"
                + "&dp=equals(hdr_from,,first,1234.5609,,yandex.ru+result)",
                TestSearchBackend.prepareResult(
                    "\"url\":\"0\",\"result\":\"0\"",
                    "\"url\":\"1\",\"result\":\"1\"",
                    "\"url\":\"2\",\"result\":\"0\"",
                    "\"url\":\"3\",\"result\":\"0\"",
                    "\"url\":\"4\",\"result\":\"1\"",
                    "\"url\":\"5\",\"result\":\"1\""));
        }
    }

    @Test
    public void testEqualsPostFilter() throws Exception {
        try (TestSearchBackend lucene = new TestSearchBackend(
            this,
            new File(
                Paths.getSourcePath(
                    "mail/search/mail/search_backend_mail_config/files"
                        + "/search_backend_test.conf_mail"))))
        {
            lucene.add(
                "\"url\":0,\"mid\":0",
                "\"url\":1,\"mid\":1,\"thread_id\":0",
                "\"url\":2,\"mid\":2,\"thread_id\":2",
                "\"url\":3,\"mid\":null,\"thread_id\":3",
                "\"url\":4",
                "\"url\":5,\"mid\":2,\"thread_id\":2",
                "\"url\":6,\"mid\":6,\"thread_id\":6");
            lucene.checkSearch(
                "/search?prefix=0&text=url:*&get=url&sort=url&asc"
                    + "&dp=to-long(thread_id+tid)&postfilter=mid+equals+tid",
                TestSearchBackend.prepareResult(
                    "\"url\":\"2\"",
                    "\"url\":\"5\"",
                    "\"url\":\"6\""));
            lucene.checkSearch(
                "/search?prefix=0&text=url:*&get=url&sort=url&asc"
                    + "&dp=to-long(thread_id+tid)"
                    + "&postfilter=mid+notequals+tid",
                TestSearchBackend.prepareResult(
                    "\"url\":\"0\"",
                    "\"url\":\"1\"",
                    "\"url\":\"3\""));
            lucene.checkSearch(
                "/printkeys?field=url&dp=to-long(thread_id+tid)"
                    + "&postfilter=mid+equals+tid&text=url:*",
                new StringChecker("2\n5\n6\n"));
        }
    }

    @Test
    public void testConditionalUpdates() throws Exception {
        try (TestSearchBackend lucene = new TestSearchBackend(
            this,
            new File(
                Paths.getSourcePath(
                    "mail/search/mail/search_backend_mail_config/files"
                        + "/search_backend_test.conf_mail"))))
        {
            lucene.add("\"url\":0,\"mid\":0");
            lucene.update("\"url\":0,\"mid\":"
                + mkFunc("set", "1"));

            lucene.update("\"url\":0,\"mid\":"
                + mkFunc("inc", "1",
                    mkFunc("eq", "1",
                        mkFunc("get", "\"mid\""))));

            lucene.checkSearch(
                "/search?prefix=0&text=url:*&get=url,mid",
                TestSearchBackend.prepareResult(
                    "\"url\":\"0\",\"mid\":\"2\""));

            lucene.update("\"url\":0,\"mid\":"
                + mkFunc("set", "3",
                    mkFunc("eq", "2",
                        mkFunc("get", "\"mid\""))));
            lucene.update("\"url\":0,\"mid\":"
                + mkFunc("set", "255",
                    mkFunc("eq", "2",
                        mkFunc("get", "\"mid\""))));

            lucene.update("\"url\":0,\"mid\":"
                + mkFunc("inc", "255",
                    mkFunc("eq", "2",
                        mkFunc("get", "\"mid\""))));

            lucene.update("\"url\":0,\"mid\":"
                + mkFunc("inc"));

            lucene.checkSearch(
                "/search?prefix=0&text=url:*&get=url,mid,lcn",
                TestSearchBackend.prepareResult(
                    "\"url\":\"0\",\"mid\":\"4\",\"lcn\":null"));

            lucene.update(
                "\"url\":0,\"mid\":" + mkFunc("inc", "2")
                + ",\"lcn\":" + mkFunc("inc", "5"));

            lucene.checkSearch(
                "/search?prefix=0&text=url:*&get=url,mid,lcn",
                TestSearchBackend.prepareResult(
                    "\"url\":\"0\",\"mid\":\"6\",\"lcn\":\"5\""));
        }
    }

    @Test
    public void testGreaterThanPredicate() throws Exception {
        try (TestSearchBackend lucene = new TestSearchBackend(
                this,
                new File(
                    Paths.getSourcePath(
                        "mail/search/mail/search_backend_mail_config/files"
                        + "/search_backend_test.conf_mail"))))
        {
            lucene.add(
                "\"url\":\"1\",\"mid\":\"1\",\"lcn\":\"0\"",
                "\"url\":\"2\",\"mid\":\"2\",\"lcn\":\"2\"",
                "\"url\":\"3\",\"mid\":\"3\",\"lcn\":\"3\"",
                "\"url\":\"4\",\"mid\":\"4\"",
                "\"url\":\"5\",\"lcn\":\"1\"",
                "\"url\":\"6\",\"mid\":\"6\",\"lcn\":\"2\"",
                "\"url\":\"7\",\"mid\":\"7\",\"lcn\":\"8\"");

            lucene.checkSearch(
                "/search?prefix=0&get=url&sort=url&asc&text=url:*"
                + "&postfilter=mid+greaterthan+lcn",
                TestSearchBackend.prepareResult(
                    "\"url\":\"1\"",
                    "\"url\":\"6\""));

            lucene.checkSearch(
                "/search?prefix=0&get=url&sort=url&asc&text=url:*"
                + "&postfilter=lcn+greaterthan+mid",
                TestSearchBackend.prepareResult("\"url\":\"7\""));

            lucene.checkSearch(
                "/search?prefix=0&get=url&sort=url&asc&text=url:*"
                + "&dp=const(2+lcnconst)&dp=to-long(lcnconst+lcnlong)"
                + "&dp=const(5+midconst)&dp=to-long(midconst+midlong)"
                + "&dp=multi(lcnlong,midlong+threshold)"
                + "&dp=multi(lcn,mid+key)"
                + "&postfilter=key+greaterthan+threshold",
                TestSearchBackend.prepareResult(
                    "\"url\":\"3\"",
                    "\"url\":\"6\"",
                    "\"url\":\"7\""));
        }
    }

    private void testIndexBadPrefix(
        final TestSearchBackend lucene,
        final String uri)
        throws Exception
    {
        // Explicitly null prefix
        HttpPost post = new HttpPost(lucene.indexerUri() + uri);
        post.setEntity(
            new StringEntity(
                "{\"prefix\":null,\"docs\":[{\"url\":1}]}",
                ContentType.APPLICATION_JSON));
        HttpAssert.assertStatusCode(HttpStatus.SC_BAD_REQUEST, post);

        // Bad prefix
        post = new HttpPost(lucene.indexerUri() + uri);
        post.setEntity(
            new StringEntity(
                "{\"prefix\":\"hello\",\"docs\":[{\"url\":1}]}",
                ContentType.APPLICATION_JSON));
        HttpAssert.assertStatusCode(HttpStatus.SC_BAD_REQUEST, post);

        // Incomplete json
        post = new HttpPost(lucene.indexerUri() + uri);
        post.setEntity(
            new StringEntity(
                "{\"prefix\":0,\"docs\":[{\"url\":1}]",
                ContentType.APPLICATION_JSON));
        HttpAssert.assertStatusCode(HttpStatus.SC_BAD_REQUEST, post);

        // No url
        post = new HttpPost(lucene.indexerUri() + uri);
        post.setEntity(
            new StringEntity(
                "{\"prefix\":0,\"docs\":[{\"mid\":1}]}",
                ContentType.APPLICATION_JSON));
        HttpAssert.assertStatusCode(HttpStatus.SC_BAD_REQUEST, post);
    }

    @Test
    public void testIndexBadPrefix() throws Exception {
        try (TestSearchBackend lucene = new TestSearchBackend(
                this,
                new File(
                    Paths.getSourcePath(
                        "mail/search/mail/search_backend_mail_config/files"
                        + "/search_backend_test.conf_mail"))))
        {
            testIndexBadPrefix(lucene, "/add");
            testIndexBadPrefix(lucene, "/modify");
            testIndexBadPrefix(lucene, "/update");
            lucene.add("\"url\":1");
            testIndexBadPrefix(lucene, "/update");
        }
    }

    private void testIndexBadFieldType(
        final TestSearchBackend lucene,
        final String uri) throws Exception
    {
        // lcn field type mismatch
        HttpPost post = new HttpPost(lucene.indexerUri() + uri);
        post.setEntity(
            new StringEntity(
                "{\"prefix\":0,\"docs\":[{\"url\":1,\"lcn\":\"hi\"}]}",
                ContentType.APPLICATION_JSON));
        HttpAssert.assertStatusCode(HttpStatus.SC_BAD_REQUEST, post);
    }

    @Test
    public void testIndexBadFieldType() throws Exception {
        try (TestSearchBackend lucene = new TestSearchBackend(
                this,
                new File(
                    Paths.getSourcePath(
                        "mail/search/mail/search_backend_mail_config/files"
                        + "/search_backend_test.conf_mail"))))
        {
            testIndexBadFieldType(lucene, "/add");
            testIndexBadFieldType(lucene, "/modify");
            // Without document, update will be rejected with 200 OK before
            // fields validation
            lucene.add("\"url\":1");
            testIndexBadFieldType(lucene, "/update");
        }
    }

    @Test
    public void testUpdateBadFieldType() throws Exception {
        try (TestSearchBackend lucene = new TestSearchBackend(
                this,
                new File(
                    Paths.getSourcePath(
                        "mail/search/mail/search_backend_mail_config/files"
                        + "/search_backend_test.conf_mail"))))
        {
            lucene.add(
                "\"url\":1,\"hdr_from_email\":\"analizer@yandex.ru\","
                + "\"lcn\":1");

            // Bad field type for increment
            HttpPost post = new HttpPost(lucene.indexerUri() + "/update");
            post.setEntity(
                new StringEntity(
                    "{\"prefix\":0,\"docs\":[{\"url\":1,"
                    + "\"hdr_from_normalized\":\""
                    + mkFunc("inc", "1", mkFunc("get", "\"hdr_from_email\""))
                    + "}]}",
                    ContentType.APPLICATION_JSON));
            HttpAssert.assertStatusCode(HttpStatus.SC_BAD_REQUEST, post);

            // Bad increment operand
            post = new HttpPost(lucene.indexerUri() + "/update");
            post.setEntity(
                new StringEntity(
                    "{\"prefix\":0,\"docs\":[{\"url\":1,"
                    + "\"hdr_from_normalized\":\""
                    + mkFunc("inc", "hi", mkFunc("get", "\"lcn\""))
                    + "}]}",
                    ContentType.APPLICATION_JSON));
            HttpAssert.assertStatusCode(HttpStatus.SC_BAD_REQUEST, post);
        }
    }

    @Test
    public void testNullifyOnUpdate() throws Exception {
        try (TestSearchBackend lucene =
                new TestSearchBackend(
                    this,
                    new File(
                        Paths.getSourcePath(
                            "mail/search/mail/search_backend_mail_config/files"
                                + "/search_backend_test.conf_mail"))))
        {
            lucene.add("\"url\":0,\"mid\":0,\"hid\":\"hello\"");

            String[] expectedValues = new String[] {
                "\"hello\"",
                "null",
                "\"world\"",
                "\"world\""
            };

            for (String expectedValue: expectedValues) {
                lucene.checkSearch(
                    "/search?prefix=0&text=url:*&get=url,mid,hid",
                    TestSearchBackend.prepareResult(
                        "\"url\":\"0\",\"mid\":\"0\",\"hid\":"
                        + expectedValue));

                lucene.update(
                    "\"url\":0,\"hid\":{\"function\":\"if\",\"args\":["
                    + "{\"function\":\"eq\",\"args\":["
                    + "\"hello\",{\"function\":\"get\",\"args\":[\"hid\"]}]}"
                    + ", null, \"world\"]}");
            }
        }
    }
    @Test
    public void testQueueId() throws Exception {
        try (TestSearchBackend lucene =
                new TestSearchBackend(
                    this,
                    new File(
                        Paths.getSourcePath(
                            "mail/search/mail/search_backend_mail_config/files"
                                + "/search_backend_test.conf_mail"))))
        {
            HttpPost post = new HttpPost(lucene.indexerUri() + "/add");
            post.setEntity(
                new StringEntity(
                    "{\"prefix\":0,\"docs\":[{\"url\":1}]}",
                    ContentType.APPLICATION_JSON));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);
            lucene.checkSearch(
                "/search?prefix=0&text=url:1&get=url,__queue_id,__queue_name",
                new JsonChecker(
                    "{\"hitsCount\":1,\"hitsArray\":[{\"url\":\"1\","
                    + "\"__queue_id\":null,\"__queue_name\":null}]}"));

            post = new HttpPost(lucene.indexerUri() + "/add?zoo-queue-id=5");
            post.addHeader(YandexHeaders.ZOO_SHARD_ID, "0");
            post.addHeader(YandexHeaders.ZOO_QUEUE, "myqueue");
            post.addHeader(
                YandexHeaders.ZOO_QUEUE_ID,
                Integer.toString(Integer.MIN_VALUE));
            post.setEntity(
                new StringEntity(
                    "{\"prefix\":0,\"docs\":[{\"url\":2}]}",
                    ContentType.APPLICATION_JSON));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);
            lucene.checkSearch(
                "/search?prefix=0&text=url:2&get=url,__queue_id,__queue_name",
                new JsonChecker(
                    "{\"hitsCount\":1,\"hitsArray\":[{\"url\":\"2\","
                    + "\"__queue_id\":\"5\",\"__queue_name\":\"myqueue\"}]}"));

            post = new HttpPost(lucene.indexerUri() + "/add");
            post.addHeader(YandexHeaders.ZOO_SHARD_ID, "0");
            post.addHeader(YandexHeaders.ZOO_QUEUE, "myqueue");
            post.addHeader(YandexHeaders.ZOO_QUEUE_ID, "3");
            post.setEntity(
                new StringEntity(
                    "{\"prefix\":0,\"docs\":[{\"url\":3}]}",
                    ContentType.APPLICATION_JSON));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);
            lucene.checkSearch(
                "/search?prefix=0&text=url:3&get=url,__queue_id,__queue_name",
                new JsonChecker(
                    "{\"hitsCount\":1,\"hitsArray\":[{\"url\":\"3\","
                    + "\"__queue_id\":\"3\",\"__queue_name\":\"myqueue\"}]}"));

            post = new HttpPost(lucene.indexerUri() + "/update");
            post.addHeader(YandexHeaders.ZOO_SHARD_ID, "0");
            post.addHeader(YandexHeaders.ZOO_QUEUE, "myqueue2");
            post.addHeader(YandexHeaders.ZOO_QUEUE_ID, "4");
            post.setEntity(
                new StringEntity(
                    "{\"prefix\":0,\"docs\":[{\"url\":3,\"mid\":\"2\","
                    + "\"received_date\":\"1234567890\"}]}",
                    ContentType.APPLICATION_JSON));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);
            lucene.checkSearch(
                "/search?prefix=0&text=url:3&get=url,__queue_id,__queue_name,"
                + "mid,received_date",
                new JsonChecker(
                    "{\"hitsCount\":1,\"hitsArray\":[{\"url\":\"3\","
                    + "\"__queue_id\":\"4\",\"__queue_name\":\"myqueue2\","
                    + "\"mid\":\"2\",\"received_date\":\"1234567890\"}]}"));

            post = new HttpPost(lucene.indexerUri() + "/update");
            post.addHeader(YandexHeaders.ZOO_SHARD_ID, "0");
            post.addHeader(YandexHeaders.ZOO_QUEUE, "myqueue3");
            post.addHeader(YandexHeaders.ZOO_QUEUE_ID, "5");
            post.setEntity(
                new StringEntity(
                    "{\"prefix\":0,\"query\":\"url:3\",\"docs\":[{\"mid\":1}]}",
                    ContentType.APPLICATION_JSON));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);
            lucene.checkSearch(
                "/search?prefix=0&text=url:3&get=url,__queue_id,__queue_name,"
                + "mid,received_date",
                new JsonChecker(
                    "{\"hitsCount\":1,\"hitsArray\":[{\"url\":\"3\","
                    + "\"__queue_id\":\"5\",\"__queue_name\":\"myqueue3\","
                    + "\"mid\":\"1\",\"received_date\":\"1234567890\"}]}"));

            post = new HttpPost(lucene.indexerUri() + "/update");
            post.setEntity(
                new StringEntity(
                    "{\"prefix\":0,\"PreserveFields\":[\"mid\"],\"docs\":["
                    + "{\"url\":3,\"folder_name\":\"test\"}]}",
                    ContentType.APPLICATION_JSON));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);
            lucene.checkSearch(
                "/search?prefix=0&text=url:3&get=url,__queue_id,__queue_name,"
                + "mid,received_date,folder_name",
                new JsonChecker(
                    "{\"hitsCount\":1,\"hitsArray\":[{\"url\":\"3\","
                    + "\"__queue_id\":\"5\",\"__queue_name\":\"myqueue3\","
                    + "\"mid\":\"1\",\"received_date\":null,\"folder_name\":"
                    + "\"test\"}]}"));

            post = new HttpPost(lucene.indexerUri() + "/update");
            post.addHeader(YandexHeaders.ZOO_SHARD_ID, "0");
            post.addHeader(YandexHeaders.ZOO_QUEUE, "myqueue4");
            post.addHeader(YandexHeaders.ZOO_QUEUE_ID, "6");
            post.setEntity(
                new StringEntity(
                    "{\"prefix\":0,\"PreserveFields\":[\"mid\"],\"docs\":["
                    + "{\"url\":3,\"received_date\":\"11\"}]}",
                    ContentType.APPLICATION_JSON));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);
            lucene.checkSearch(
                "/search?prefix=0&text=url:3&get=url,__queue_id,__queue_name,"
                + "mid,received_date,folder_name",
                new JsonChecker(
                    "{\"hitsCount\":1,\"hitsArray\":[{\"url\":\"3\","
                    + "\"__queue_id\":\"6\",\"__queue_name\":\"myqueue4\","
                    + "\"mid\":\"1\",\"received_date\":\"11\",\"folder_name\":"
                    + "null}]}"));
        }
    }
}

