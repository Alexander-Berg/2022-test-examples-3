package ru.yandex.msearch;

import java.io.File;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

public class ClusterizeTest extends TestBase {
    private static final String CLUSTER_CONFIG =
        "[field.mtime]\n"
        + "tokenizer = keyword\n"
        + "filters = padding:10\n"
        + "store = true\n"
        + "prefixed = true\n"
        + "analyze = true\n"
        + "attribute = true\n"
        + "type = integer\n"
        + "[field.created]\n"
        + "tokenizer = keyword\n"
        + "filters = padding:10\n"
        + "prefixed = true\n"
        + "store = true\n"
        + "analyze = true\n"
        + "attribute = true\n"
        + "[field.latitude]\n"
        + "tokenizer = keyword\n"
        + "filters = aaz:90|maf:1000000|padding:10\n"
        + "prefixed = true\n"
        + "store = true\n"
        + "analyze = true\n"
        + "attribute = true\n"
        + "[field.longitude]\n"
        + "tokenizer = keyword\n"
        + "filters = aaz:180|maf:1000000|padding:10\n"
        + "prefixed = true\n"
        + "store = true\n"
        + "analyze = true\n"
        + "attribute = true\n";

    @Test
    public void test() throws Exception {
        File root = Files.createTempDirectory("test").toFile();
        try (Daemon daemon =
                new Daemon(SearchBackendTestBase.config(root, CLUSTER_CONFIG));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":["
                + "{\"keyword\":\"1\",\"mtime\":1000,\"created\":2000,"
                + "\"latitude\":60,\"longitude\":\"30\"},"
                + "{\"keyword\":\"2\",\"mtime\":1900},"
                + "{\"keyword\":\"3\",\"created\":2800},"
                + "{\"keyword\":\"4\",\"created\":4600,"
                + "\"latitude\":60,\"longitude\":\"30.016\"},"
                + "{\"keyword\":\"5\",\"created\":8200,"
                + "\"latitude\":60,\"longitude\":\"30.032\"},"
                + "{\"keyword\":\"6\",\"created\":11800,"
                + "\"latitude\":60,\"longitude\":\"30.064\"},"
                + "{\"keyword\":7}]}",
                StandardCharsets.UTF_8));
            HttpResponse response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort()
                + "/clusterize?prefix=1&text=keyword:*&get=keyword,date"
                + "&min-cluster=4&date-field=date&dp=const(0+zero)"
                + "&dp=fallback(created,mtime,zero+date)"));
            String text = EntityUtils.toString(response.getEntity());
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":3,\"hitsArray\":["
                    + "{\"size\":1,\"max\":11800,\"min\":11800,"
                    + "\"merged_docs\":[{\"keyword\":\"6\",\"date\":\"11800\"}]},"
                    + "{\"size\":5,\"max\":8200,\"min\":1900,\"merged_docs\":["
                    + "{\"keyword\":\"5\",\"date\":\"8200\"},"
                    + "{\"keyword\":\"4\",\"date\":\"4600\"},"
                    + "{\"keyword\":\"3\",\"date\":\"2800\"},"
                    + "{\"keyword\":\"1\",\"date\":\"2000\"},"
                    + "{\"keyword\":\"2\",\"date\":\"1900\"}]},"
                    + "{\"size\":1,\"max\":0,\"min\":0,\"merged_docs\":["
                    + "{\"keyword\":\"7\",\"date\":\"0\"}]}]}"),
                text);
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort()
                + "/clusterize?prefix=1&text=keyword:*&get=keyword"
                + "&min-cluster=1&offset=1&length=2"
                + "&asc&merged-length=2&distance=500"));
            text = EntityUtils.toString(response.getEntity());
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":4,\"hitsArray\":["
                    + "{\"size\":3,\"max\":4600,\"min\":2000,\"merged_docs\":["
                    + "{\"keyword\":\"1\"},{\"keyword\":\"3\"}]},"
                    + "{\"size\":1,\"max\":8200,\"min\":8200,\"merged_docs\":["
                    + "{\"keyword\":\"5\"}]}]}"),
                text);
        } finally {
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testMinCluster() throws Exception {
        File root = Files.createTempDirectory("testMinCluster").toFile();
        try (Daemon daemon =
                new Daemon(SearchBackendTestBase.config(root, CLUSTER_CONFIG));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":["
                + "{\"keyword\":1,\"mtime\":2000},"
                + "{\"keyword\":2,\"mtime\":2200},"
                + "{\"keyword\":3,\"mtime\":2300},"
                + "{\"keyword\":4,\"mtime\":2400},"
                + "{\"keyword\":5,\"mtime\":2500},"
                + "{\"keyword\":6,\"mtime\":5000},"
                + "{\"keyword\":7,\"mtime\":6000},"
                + "{\"keyword\":8}]}",
                StandardCharsets.UTF_8));
            HttpResponse response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort()
                + "/clusterize?prefix=1&text=keyword:*&get=keyword,date"
                + "&min-cluster=4&date-field=date&dp=const(0+zero)"
                + "&dp=fallback(created,mtime,zero+date)"));
            String text = EntityUtils.toString(response.getEntity());
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":3,\"hitsArray\":["
                    + "{\"size\":2,\"max\":6000,\"min\":5000,"
                    + "\"merged_docs\":[{\"keyword\":\"7\",\"date\":\"6000\"},"
                    + "{\"keyword\":\"6\",\"date\":\"5000\"}]},"
                    + "{\"size\":5,\"max\":2500,\"min\":2000,\"merged_docs\":["
                    + "{\"keyword\":\"5\",\"date\":\"2500\"},"
                    + "{\"keyword\":\"4\",\"date\":\"2400\"},"
                    + "{\"keyword\":\"3\",\"date\":\"2300\"},"
                    + "{\"keyword\":\"2\",\"date\":\"2200\"},"
                    + "{\"keyword\":\"1\",\"date\":\"2000\"}]},"
                    + "{\"size\":1,\"max\":0,\"min\":0,\"merged_docs\":["
                    + "{\"keyword\":\"8\",\"date\":\"0\"}]}]}"),
                text);
        } finally {
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testGroup() throws Exception {
        File root =
            Files.createTempDirectory(testName.getMethodName()).toFile();
        try (Daemon daemon =
                new Daemon(SearchBackendTestBase.config(root));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":["
                + "{\"keyword\":\"0\",\"attribute\":0},"
                + "{\"keyword\":\"1\",\"attribute\":1,\"property\":1},"
                + "{\"keyword\":\"2\",\"attribute\":2,\"property\":2},"
                + "{\"keyword\":\"3\",\"attribute\":3,\"property\":1},"
                + "{\"keyword\":\"4\",\"attribute\":4,\"property\":2},"
                + "{\"keyword\":\"5\",\"attribute\":5,\"property\":1},"
                + "{\"keyword\":\"6\",\"attribute\":6,\"property\":2}]}",
                StandardCharsets.UTF_8));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        "http://localhost:" + daemon.searchServerPort()
                        + "/clusterize?prefix=1&text=keyword:*"
                        + "&get=keyword&min-cluster=1"
                        + "&date-field=attribute&dp=increment(property+group)"
                        + "&group=group&distance=0&skip-nulls=true")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    "{\"hitsCount\":3,\"hitsArray\":["
                    + "{\"size\":3,\"group\":\"3\",\"max\":6,\"min\":2,"
                    + "\"merged_docs\":[{\"keyword\":\"6\"},"
                    + "{\"keyword\":\"4\"},{\"keyword\":\"2\"}]},"
                    + "{\"size\":3,\"group\":\"2\",\"max\":5,\"min\":1,"
                    + "\"merged_docs\":[{\"keyword\":\"5\"},"
                    + "{\"keyword\":\"3\"},{\"keyword\":\"1\"}]},"
                    + "{\"size\":1,\"max\":0,\"min\":0,"
                    + "\"merged_docs\":[{\"keyword\":\"0\"}]}]}",
                    EntityUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        "http://localhost:" + daemon.searchServerPort()
                        + "/clusterize?prefix=1&text=keyword:*"
                        + "&get=keyword&min-cluster=1"
                        + "&date-field=attribute&dp=increment(property+group)"
                        + "&group=group&distance=0&skip-nulls=true"
                        + "&postfilter=keyword+%3C%3D+4")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    "{\"hitsCount\":3,\"hitsArray\":["
                    + "{\"size\":2,\"group\":\"3\",\"max\":4,\"min\":2,"
                    + "\"merged_docs\":["
                    + "{\"keyword\":\"4\"},{\"keyword\":\"2\"}]},"
                    + "{\"size\":2,\"group\":\"2\",\"max\":3,\"min\":1,"
                    + "\"merged_docs\":["
                    + "{\"keyword\":\"3\"},{\"keyword\":\"1\"}]},"
                    + "{\"size\":1,\"max\":0,\"min\":0,"
                    + "\"merged_docs\":[{\"keyword\":\"0\"}]}]}",
                    EntityUtils.toString(response.getEntity()));
            }
        } finally {
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    private void testAllFields(final String charset) throws Exception {
        File root =
            Files.createTempDirectory(testName.getMethodName()).toFile();
        try (Daemon daemon =
                new Daemon(SearchBackendTestBase.config(root));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":["
                + "{\"keyword\":\"0\",\"attribute\":0},"
                + "{\"keyword\":\"1\",\"attribute\":2},"
                + "{\"keyword\":\"2\",\"attribute\":4},"
                + "{\"keyword\":\"3\",\"attribute\":6},"
                + "{\"keyword\":\"4\",\"attribute\":7},"
                + "{\"keyword\":\"5\",\"attribute\":8}]}",
                StandardCharsets.UTF_8));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);
            HttpGet get = new HttpGet(
                "http://localhost:" + daemon.searchServerPort()
                + "/clusterize?prefix=1&text=keyword:*"
                + "&get=*&min-cluster=3&interval=1"
                + "&date-field=attribute&distance=0");
            get.addHeader(HttpHeaders.ACCEPT_CHARSET, charset);
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    Charset.forName(charset),
                    CharsetUtils.contentType(response.getEntity())
                        .getCharset());
                Assert.assertEquals(
                    "{\"hitsCount\":2,\"hitsArray\":["
                    + "{\"size\":3,\"max\":8,\"min\":6,"
                    + "\"merged_docs\":["
                    + "{\"keyword\":\"5\",\"attribute\":\"8\"},"
                    + "{\"keyword\":\"4\",\"attribute\":\"7\"},"
                    + "{\"keyword\":\"3\",\"attribute\":\"6\"}]},"
                    + "{\"size\":3,\"max\":4,\"min\":0,"
                    + "\"merged_docs\":["
                    + "{\"keyword\":\"2\",\"attribute\":\"4\"},"
                    + "{\"keyword\":\"1\",\"attribute\":\"2\"},"
                    + "{\"keyword\":\"0\",\"attribute\":\"0\"}]}]}",
                    CharsetUtils.toString(response.getEntity()).trim());
            }
        } finally {
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testAllFields() throws Exception {
        testAllFields("UTF-8");
        testAllFields("UTF-16BE");
    }
}

