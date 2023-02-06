package ru.yandex.msearch;

import java.io.File;
import java.io.StringReader;
import java.nio.charset.Charset;
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

public class MergeFuncTest extends TestBase {
    private static final Charset UTF8 = Charset.forName("utf-8");

    @Test
    public void testMergeFuncSearchNoPrimaryKey() throws Exception {
        File root =
            Files.createTempDirectory("testMergeValuesSearchNoPrimaryKey")
                .toFile();
        try (Daemon daemon = new Daemon(SearchBackendTestBase.config(root));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                + "\"keyword\":\"first\",\"attribute\":1},{"
                + "\"keyword\":\"second\",\"attribute\":1},{"
                + "\"keyword\":\"third\",\"attribute\":2},{"
                + "\"keyword\":\"forth\",\"attribute\":2}]}", UTF8));
            HttpResponse response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());

            //test default merge_func which is "values"
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&text="
                + "attribute:[1%20TO%202]&get=keyword,attribute&sort=keyword&asc&group=attribute"));
            String text = EntityUtils.toString(response.getEntity());
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":2,\"hitsArray\":[{"
                    + "\"keyword\":\"first\",\"attribute\":\"1\",\"merged_docs\":[{\"keyword\":\"second\",\"attribute\":\"1\"}]},"
                    + "{\"keyword\":\"forth\",\"attribute\":\"2\",\"merged_docs\":[{\"keyword\":\"third\",\"attribute\":\"2\"}]}]}"),
                text);

            //test "values" merge_func
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&text="
                + "attribute:[1%20TO%202]&get=keyword,attribute&sort=keyword&asc&group=attribute&merge_func=values"));
            text = EntityUtils.toString(response.getEntity());
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":2,\"hitsArray\":[{"
                    + "\"keyword\":\"first\",\"attribute\":\"1\",\"merged_docs\":[{\"keyword\":\"second\",\"attribute\":\"1\"}]},"
                    + "{\"keyword\":\"forth\",\"attribute\":\"2\",\"merged_docs\":[{\"keyword\":\"third\",\"attribute\":\"2\"}]}]}"),
                text);

            //test "count" merge_func
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&text="
                + "attribute:[1%20TO%202]&get=keyword,attribute&sort=keyword&asc&group=attribute&merge_func=count"));
            text = EntityUtils.toString(response.getEntity());
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":2,\"hitsArray\":[{"
                    + "\"keyword\":\"first\",\"attribute\":\"1\",\"merged_docs_count\":1},"
                    + "{\"keyword\":\"forth\",\"attribute\":\"2\",\"merged_docs_count\":1}]}"),
                text);

            //test "all" merge_func
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&text="
                + "attribute:[1%20TO%202]&get=keyword,attribute&sort=keyword&asc&group=attribute&merge_func=all"));
            text = EntityUtils.toString(response.getEntity());
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":2,\"hitsArray\":[{"
                    + "\"keyword\":\"first\",\"attribute\":\"1\",\"merged_docs\":[{\"keyword\":\"second\",\"attribute\":\"1\"}],\"merged_docs_count\":1},"
                    + "{\"keyword\":\"forth\",\"attribute\":\"2\",\"merged_docs\":[{\"keyword\":\"third\",\"attribute\":\"2\"}],\"merged_docs_count\":1}]}"),
                text);
        } finally {
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testMergeFuncSearchYesPrimaryKey() throws Exception {
        File root = Files.createTempDirectory("testMergeValuesSearchNoPrimaryKey").toFile();
        try (Daemon daemon = new Daemon(
                SearchBackendTestBase.config(root, "primary_key = keyword"));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                + "\"keyword\":\"first\",\"attribute\":1},{"
                + "\"keyword\":\"second\",\"attribute\":1},{"
                + "\"keyword\":\"third\",\"attribute\":2},{"
                + "\"keyword\":\"forth\",\"attribute\":2}]}", UTF8));
            HttpResponse response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());

            //test default merge_func which is "values"
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&text="
                + "attribute:[1%20TO%202]&get=keyword,attribute&sort=keyword&asc&group=attribute"));
            String text = EntityUtils.toString(response.getEntity());
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":2,\"hitsArray\":[{"
                    + "\"keyword\":\"first\",\"attribute\":\"1\",\"merged_docs\":[{\"keyword\":\"second\",\"attribute\":\"1\"}]},"
                    + "{\"keyword\":\"forth\",\"attribute\":\"2\",\"merged_docs\":[{\"keyword\":\"third\",\"attribute\":\"2\"}]}]}"),
                text);

            //test "values" merge_func
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&text="
                + "attribute:[1%20TO%202]&get=keyword,attribute&sort=keyword&asc&group=attribute&merge_func=values"));
            text = EntityUtils.toString(response.getEntity());
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":2,\"hitsArray\":[{"
                    + "\"keyword\":\"first\",\"attribute\":\"1\",\"merged_docs\":[{\"keyword\":\"second\",\"attribute\":\"1\"}]},"
                    + "{\"keyword\":\"forth\",\"attribute\":\"2\",\"merged_docs\":[{\"keyword\":\"third\",\"attribute\":\"2\"}]}]}"),
                text);

            //test "count" merge_func
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&text="
                + "attribute:[1%20TO%202]&get=keyword,attribute&sort=keyword&asc&group=attribute&merge_func=count"));
            text = EntityUtils.toString(response.getEntity());
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":2,\"hitsArray\":[{"
                    + "\"keyword\":\"first\",\"attribute\":\"1\",\"merged_docs_count\":1},"
                    + "{\"keyword\":\"forth\",\"attribute\":\"2\",\"merged_docs_count\":1}]}"),
                text);

            //test "all" merge_func
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&text="
                + "attribute:[1%20TO%202]&get=keyword,attribute&sort=keyword&asc&group=attribute&merge_func=all"));
            text = EntityUtils.toString(response.getEntity());
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":2,\"hitsArray\":[{"
                    + "\"keyword\":\"first\",\"attribute\":\"1\",\"merged_docs\":[{\"keyword\":\"second\",\"attribute\":\"1\"}],\"merged_docs_count\":1},"
                    + "{\"keyword\":\"forth\",\"attribute\":\"2\",\"merged_docs\":[{\"keyword\":\"third\",\"attribute\":\"2\"}],\"merged_docs_count\":1}]}"),
                text);
        } finally {
            SearchBackendTestBase.removeDirectory(root);
        }
    }
}
