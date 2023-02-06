package ru.yandex.msearch;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
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

public class GroupTest extends TestBase {
    private static final Charset UTF8 = Charset.forName("utf-8");

    @Test
    public void testGroupMulti() throws Exception {
        File root = Files.createTempDirectory("testMergeValuesSearchNoPrimaryKey").toFile();
        try (Daemon daemon = new Daemon(SearchBackendTestBase.config(
                root,
                "field.attribute2.tokenizer = whitespace\n"
                + "field.attribute2.filters = lowercase\n"
                + "field.attribute2.prefixed = false\n"
                + "field.attribute2.attribute = true\n"
                + "field.attribute2.analyze = true\n"
                + "field.attribute2.store = true\n"));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                + "\"keyword\":\"first\",\"attribute\":1,\"attribute2\":1},{"
                + "\"keyword\":\"second\",\"attribute\":1,\"attribute2\":1},{"
                + "\"keyword\":\"third\",\"attribute\":2,\"attribute2\":1},{"
                + "\"keyword\":\"forth\",\"attribute\":2,\"attribute2\":2}]}", UTF8));
            HttpResponse response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());

            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort() + "/search?prefix=1&text="
                + "attribute:[1%20TO%202]&get=keyword,attribute,attribute2&sort=keyword&asc&group=multi(attribute,attribute2)"));
            String text = EntityUtils.toString(response.getEntity());
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":3,\"hitsArray\":[{"
                    + "\"keyword\":\"first\",\"attribute\":\"1\",\"attribute2\":\"1\",\"merged_docs\":[{\"keyword\":\"second\",\"attribute\":\"1\",\"attribute2\":\"1\"}]},"
                    + "{\"keyword\":\"forth\",\"attribute\":\"2\",\"attribute2\":\"2\"},"
                    + "{\"keyword\":\"third\",\"attribute\":\"2\",\"attribute2\":\"1\"}]}"),
                text);
        } finally {
            SearchBackendTestBase.removeDirectory(root);
        }
    }
}
