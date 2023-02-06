package ru.yandex.msearch;

import java.io.File;
import java.nio.charset.StandardCharsets;
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

import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

public class AutoFixIndexTest extends TestBase {

    private void prepareBrokenIndex(final File root) throws Exception {
        boolean removeDir = true;
        final String config = "field.version.store = true\n"
            + "field.version.index = false\n";
        try (Daemon daemon =
            new Daemon(SearchBackendTestBase.config(root, config, 1));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                + "\"keyword\":\"some keyword\",\"property\":\"1\","
                + "\"attribute\":\"2\", \"version\":1}"
                +",{\"keyword\":\"some keyword\","
                + "\"property\":\"3\",\"attribute\":\"4\",\"version\":2}]}",
                StandardCharsets.UTF_8));
            HttpResponse response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort()
                + "/search?prefix=1&text=keyword:some%5C+keyword&sort=property"
                + "&get=keyword,property,attribute,version"));
            String text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker("{\"hitsCount\":2,\"hitsArray\":[{"
                    + "\"keyword\":\"some keyword\",\"property\":\"3\","
                    + "\"attribute\":\"4\",\"version\":\"2\"},"
                    + "{\"keyword\":\"some keyword\","
                    + "\"property\":\"1\",\"attribute\":\"2\","
                    + "\"version\":\"1\"}]}"),
                text);
            removeDir = false;
        } finally {
            if (removeDir) {
                SearchBackendTestBase.removeDirectory(root);
            }
        }
    }

    @Test
    public void testAdd() throws Exception {
        File root = Files.createTempDirectory("testAddAutoFixIndex").toFile();
        prepareBrokenIndex(root);
        try (Daemon daemon = new Daemon(SearchBackendTestBase.config(
                root, "primary_key = keyword\nauto_fix_index = true", 1));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                + "\"keyword\":\"some keyword\",\"property\":\"4\"}]}",
                StandardCharsets.UTF_8));
            HttpResponse response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()).trim(),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort()
                + "/search?prefix=1&text=keyword:some%5C+keyword"
                + "&get=keyword,property,attribute"));
            String text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker("{\"hitsCount\":1,\"hitsArray\":[{"
                    + "\"keyword\":\"some keyword\",\"property\":\"4\","
                    + "\"attribute\":null}]}"),
                text);
        } finally {
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testModify() throws Exception {
        File root =
            Files.createTempDirectory("testModifyAutoFixIndex").toFile();
        prepareBrokenIndex(root);
        try (Daemon daemon = new Daemon(SearchBackendTestBase.config(
                root, "primary_key = keyword\nauto_fix_index = true", 1));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/modify");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                + "\"keyword\":\"some keyword\",\"property\":\"4\"}]}",
                StandardCharsets.UTF_8));
            HttpResponse response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()).trim(),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort()
                + "/search?prefix=1&text=keyword:some%5C+keyword"
                + "&get=keyword,property,attribute"));
            String text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":1,\"hitsArray\":[{"
                    + "\"keyword\":\"some keyword\",\"property\":\"4\","
                    + "\"attribute\":null}]}"),
                text);
        } finally {
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testDelete() throws Exception {
        File root =
            Files.createTempDirectory("testDeleteAutoFixIndex").toFile();
        prepareBrokenIndex(root);
        try (Daemon daemon = new Daemon(SearchBackendTestBase.config(
                root, "primary_key = keyword\nauto_fix_index = true", 1));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/delete");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                + "\"keyword\":\"some keyword\"}]}",
                StandardCharsets.UTF_8));
            HttpResponse response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()).trim(),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort()
                + "/search?prefix=1&text=keyword:some%5C+keyword"
                + "&get=keyword,property,attribute"));
            String text = EntityUtils.toString(response.getEntity()).trim();
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
    public void testDeleteByQuery() throws Exception {
        File root = Files.createTempDirectory("testDeleteByQueryAutoFixIndex")
            .toFile();
        prepareBrokenIndex(root);
        try (Daemon daemon = new Daemon(SearchBackendTestBase.config(
                root, "primary_key = keyword\nauto_fix_index = true", 1));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpResponse response = client.execute(new HttpGet(
                "http://localhost:" + daemon.jsonServerPort()
                + "/delete?prefix=1&text=keyword:*"));
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()).trim(),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort()
                + "/search?prefix=1&text=keyword:some%5C+keyword"
                + "&get=keyword,property,attribute"));
            String text = EntityUtils.toString(response.getEntity()).trim();
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
    public void testUpdate() throws Exception {
        System.err.println("testUpdate started");
        File root =
            Files.createTempDirectory("testUpdateAutoFixIndex").toFile();
        prepareBrokenIndex(root);
        try (Daemon daemon = new Daemon(SearchBackendTestBase.config(
                root, "primary_key = keyword\nauto_fix_index = true", 1));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/update");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                + "\"property\":\"5\",\"keyword\":\"some keyword\"}]}",
                StandardCharsets.UTF_8));
            HttpResponse response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()).trim(),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort()
                + "/search?prefix=1&text=keyword:some%5C+keyword"
                + "&get=keyword,property,attribute"));
            String text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            if (!text.equals("{\"hitsCount\":1,\"hitsArray\":[{"
                + "\"keyword\":\"some keyword\",\"property\":\"5\","
                + "\"attribute\":\"2\"}]}"))
            {
                YandexAssert.check(
                    new JsonChecker(
                        "{\"hitsCount\":1,\"hitsArray\":[{"
                        + "\"keyword\":\"some keyword\",\"property\":\"5\","
                        + "\"attribute\":\"4\"}]}"),
                    text);
            }
        } finally {
            System.err.println("testUpdate ended");
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testUpdateByQuery() throws Exception {
        System.err.println("testUpdateByQuery started");
        File root = Files.createTempDirectory("testUpdateByQueryAutoFixIndex")
            .toFile();
        prepareBrokenIndex(root);
        try (Daemon daemon = new Daemon(SearchBackendTestBase.config(
                root, "primary_key = keyword\nauto_fix_index = true", 1));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/update");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                + "\"property\":\"5\"}],\"query\":\"keyword:*\"}",
                StandardCharsets.UTF_8));
            HttpResponse response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()).trim(),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort()
                + "/search?prefix=1&text=keyword:some%5C+keyword"
                + "&get=keyword,property,attribute"));
            String text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            if (!text.equals("{\"hitsCount\":1,\"hitsArray\":[{"
                + "\"keyword\":\"some keyword\",\"property\":\"5\","
                + "\"attribute\":\"2\"}]}"))
            {
                YandexAssert.check(
                    new JsonChecker(
                        "{\"hitsCount\":1,\"hitsArray\":[{"
                        + "\"keyword\":\"some keyword\",\"property\":\"5\","
                        + "\"attribute\":\"4\"}]}"),
                    text);
            }
            SearchBackendTestBase.removeDirectory(root);
        } finally {
            System.err.println("testUpdateByQuery ended");
        }
    }
}

