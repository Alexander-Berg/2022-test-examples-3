package ru.yandex.kitsune;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.http.test.Configs;
import ru.yandex.http.test.ExpectingHttpItem;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.StringChecker;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

public class KitsuneServerTest extends TestBase {
    @Test
    public void test() throws Exception {
        try (KitsuneCluster cluster = new KitsuneCluster(this);
             CloseableHttpClient client = Configs.createDefaultClient()) {

            final String uri = "/scoring/some/path?key1=value1";

            cluster.headServer().add(uri,
                    new ExpectingHttpItem(
                            new StringChecker("some data"),
                            "{\"status\": \"ok\"}"));

            cluster.tailServer().add(uri,
                    new ExpectingHttpItem(
                            new StringChecker("some data"),
                            "{\"status\": \"ok\"}"));

            cluster.start();
            {
                HttpPost post = new HttpPost(cluster.server().host() + uri);
                post.setEntity(new StringEntity("some data",
                        ContentType.APPLICATION_JSON));

                try (CloseableHttpResponse response = client.execute(post)) {
                    HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                    YandexAssert.check(
                            new JsonChecker("{\"status\": \"ok\"}"),
                            CharsetUtils.toString(response.getEntity()));
                }
            }
            Assert.assertEquals(
                    1,
                    cluster.headServer().accessCount(uri));
            Assert.assertEquals(
                    1,
                    cluster.tailServer().accessCount(uri));
        }
    }
}

