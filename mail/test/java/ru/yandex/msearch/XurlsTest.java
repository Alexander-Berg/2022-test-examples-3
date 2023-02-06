package ru.yandex.msearch;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

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

import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

public class XurlsTest extends TestBase {
    private static final String CONFIG_SUFFIX = "\n[field.mid]\n"
        + "store=true\n"
        + "[field.x_urls]\n"
        + "tokenizer = boolean\n"
        + "prefixed = true\n"
        + "attribute = true\n"
        + "store = true\n"
        + "analyze = true\n";

    @Test
    public void testSimple() throws Exception {
        File root = Files.createTempDirectory("testSimple").toFile();
        try (Daemon daemon =
                new Daemon(SearchBackendTestBase.config(root, CONFIG_SUFFIX));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                + "\"mid\":\"100500\",\"x_urls\":\""
                + "mailto:dpotapov@yandex-team.ru\n"
                + "http://www.w3.ort/TR/some?tr=here\n"
                + "http://yandex.ru\"}]}",
                StandardCharsets.UTF_8));
            HttpResponse response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            EntityUtils.consume(response.getEntity());
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchPort()
                + "/?user=1&format=json&getfields=x_urls&"
                + "outergroup=x_urls&imap=1&text=x_urls:*"));
            String text = EntityUtils.toString(response.getEntity());
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":1,\"hitsArray\":[{"
                    + "\"x_urls\":\"http://yandex.ru\\n\"}]}"),
                text);
        } finally {
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testSingle() throws Exception {
        File root = Files.createTempDirectory("testSingle").toFile();
        try (Daemon daemon =
                new Daemon(SearchBackendTestBase.config(root, CONFIG_SUFFIX));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                + "\"mid\":\"100500\",\"x_urls\":\"http:"
                + "//e.mail.ru/sentmsg?compose&To=ganinegor@ya.ru\n\"}]}",
                StandardCharsets.UTF_8));
            HttpResponse response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            EntityUtils.consume(response.getEntity());
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchPort()
                + "/?user=1&format=json&getfields=mid,x_urls&"
                + "outergroup=x_urls&imap=1&text=x_urls:*"));
            String text = EntityUtils.toString(response.getEntity());
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker("{\"hitsCount\":0,\"hitsArray\":[]}"),
                text);
        } finally {
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testMailRu() throws Exception {
        File root = Files.createTempDirectory("testMailRu").toFile();
        try (Daemon daemon =
                new Daemon(SearchBackendTestBase.config(root, CONFIG_SUFFIX));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                + "\"mid\":\"100500\",\"x_urls\":\""
                + "http://www.w3.ort/TR/some?tr=here\n"
                + "http://yandex.ru/Я\n"
                + "http://e.mail.ru/sentmsg?compose&To=me@ya.ru\n"
                + "e.mail.ru/sentmsg?compose&To=me@ya.ru\n"
                + "e.mail.ru%2Fsentmsg?compose&To=me@ya.ru\n"
                + "e.mail.ru/cgi-bin/sentmsg?compose&To=me@ya.ru\n"
                + "e.mail.ru/cgi-bin/sentmsg?compose&to=me@ya.ru\n"
                + "https://e.mail.ru/sentmsg?mailto=mailto%253achitamara@mail.ru\n"
                + "https://e.mail.ru/messages/inbox/sentmsg?mailto=mailto%253autis1981@mail.ru\n"
                + "\"}]}",
                StandardCharsets.UTF_8));
            HttpResponse response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            EntityUtils.consume(response.getEntity());
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchPort()
                + "/?user=1&format=json&getfields=x_urls&"
                + "outergroup=x_urls&imap=1&text=x_urls:*"));
            String text = EntityUtils.toString(response.getEntity());
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":1,\"hitsArray\":[{"
                    + "\"x_urls\":\"http://yandex.ru/%D0%AF\\n\"}]}"),
                text);
        } finally {
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testNear() throws Exception {
        File root = Files.createTempDirectory("testNear").toFile();
        try (Daemon daemon =
                new Daemon(SearchBackendTestBase.config(root, CONFIG_SUFFIX));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                + "\"mid\":\"100500\",\"x_urls\":\"http:"
                + "//e.mail.ru/sentmsg?compose&To=ganinegor@ya.ru\n\"}]}",
                StandardCharsets.UTF_8));
            HttpResponse response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            EntityUtils.consume(response.getEntity());
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchPort()
                + "/?user=1&format=json&getfields=mid,x_urls&"
                + "outergroup=x_urls&near=mid:100500&offset=-100&limit=200"
                + "&imap=1&text=x_urls:*"));
            String text = EntityUtils.toString(response.getEntity());
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":1,\"hitsArray\":["
                    + "{\"mid\":\"100500\",\"x_urls\":\"\"}]}"),
                text);
        } finally {
            SearchBackendTestBase.removeDirectory(root);
        }
    }
}

