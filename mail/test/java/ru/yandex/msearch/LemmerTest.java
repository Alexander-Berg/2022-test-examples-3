package ru.yandex.msearch;

import java.io.File;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

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

import ru.yandex.devtools.test.Paths;
import ru.yandex.test.search.backend.TestSearchBackend;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

public class LemmerTest extends TestBase {
    private static final Charset UTF8 = Charset.forName("utf-8");

    @Test
    public void testHomonyms() throws Exception {
        File root = Files.createTempDirectory("testHomonyms").toFile();
        try (Daemon daemon = new Daemon(SearchBackendTestBase.config(root));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                + "\"text\":\"Ты добрый как Санта. "
                + "Дену я куда всё это?\","
                + "\"keyword\":\"first\"},{\"text\":\"кликовой\","
                + "\"keyword\":\"second\"},"
                + "{\"text\":\"залишити\",\"keyword\":\"third\"},"
                + "{\"text\":\"şehir sokak kayak\",\"keyword\":\"forth\"}"
                + "]}", UTF8));
            HttpResponse response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            EntityUtils.consume(response.getEntity());
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&"
                + "text=text:%28как+дела%29&get=keyword"));
            String text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":1,"
                    + "\"hitsArray\":[{\"keyword\":\"first\"}]}"),
                text);
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&"
                + "text=text:%28Доброе+дело%29&get=keyword"));
            text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":1,"
                    + "\"hitsArray\":[{\"keyword\":\"first\"}]}"),
                text);

            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&"
                + "text=text:кликовая&get=keyword"));
            text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":1,"
                    + "\"hitsArray\":[{\"keyword\":\"second\"}]}"),
                text);

            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&"
                + "text=text:%22куда+все+это%22&get=keyword"));
            text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":1,"
                    + "\"hitsArray\":[{\"keyword\":\"first\"}]}"),
                text);

            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&"
                + "text=text:%22дёну+я+куда%22&get=keyword"));
            text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":1,"
                    + "\"hitsArray\":[{\"keyword\":\"first\"}]}"),
                text);
            /*response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&"
                + "text=text:залишати&get=keyword"));
            text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":1,"
                    + "\"hitsArray\":[{\"keyword\":\"third\"}]}"),
                text);
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&"
                + "text=text:kayakçılık&get=keyword"));
            text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":1,"
                    + "\"hitsArray\":[{\"keyword\":\"forth\"}]}"),
                text);*/
        } finally {
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testSurzhik() throws Exception {
        try (TestSearchBackend lucene = new TestSearchBackend(
                this,
                new File(
                    Paths.getSourcePath(
                        "mail/search/mail/search_backend_mail_config/files"
                        + "/search_backend_test.conf_mail"))))
        {
            lucene.add(
                "\"url\":\"1\",\"body_text\":\"репозитория\"",
                "\"url\":\"2\",\"body_text\":\"репозиторий\"",
                "\"url\":\"3\",\"body_text\":\"двухфакторную\"",
                "\"url\":\"4\",\"body_text\":\"двухфакторная\"");
            lucene.checkSearch(
                "/search?prefix=0&get=*&sort=url&text=body_text:репозитория",
                TestSearchBackend.prepareResult("\"url\":\"2\"", "\"url\":\"1\""));
            lucene.checkSearch(
                "/search?prefix=0&get=*&sort=url&text=body_text:репозиторий",
                TestSearchBackend.prepareResult("\"url\":\"2\"", "\"url\":\"1\""));
            lucene.checkSearch(
                "/search?prefix=0&get=*&sort=url&text=body_text:двухфакторная",
                TestSearchBackend.prepareResult("\"url\":\"4\"", "\"url\":\"3\""));
            lucene.checkSearch(
                "/search?prefix=0&get=*&sort=url&text=body_text:двухфакторную",
                TestSearchBackend.prepareResult("\"url\":\"4\"", "\"url\":\"3\""));
        }
    }
}

