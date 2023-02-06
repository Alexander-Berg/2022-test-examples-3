package ru.yandex.sp_kitsune;

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
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

public class SpKitsuneTest extends TestBase {
    @Test
    public void testIn() throws Exception {
        try (Cluster cluster = new Cluster(
                this,
                resource("mail/so/daemons/sp_kitsune/sp_kitsune_config/files/sp-kitsune-in.conf"),
                "IN");
             CloseableHttpClient client = Configs.createDefaultClient()) {

            final String url = "/antispam?session_id=aIZo3O77Ey-Os7i02gq&format=protobuf-json&testing=true";
            final String requestJson = loadResourceAsString("mail/so/daemons/so2/so2/test/resources/ru/yandex/mail" +
                    "/so2" +
                    "/protobuf-request.json");
            final String headResponseJson = loadResourceAsString("head-protobuf-response.json");
            final String tailResponseJson = loadResourceAsString("tail-protobuf-response.json");

            cluster.head().add(url,
                    new ExpectingHttpItem(
                            new JsonChecker(requestJson),
                            headResponseJson),
                    new ExpectingHttpItem(
                            new JsonChecker(requestJson),
                            headResponseJson));

            cluster.tail().add(url,
                    new ExpectingHttpItem(
                            new JsonChecker(requestJson),
                            headResponseJson),
                    new ExpectingHttpItem(
                            new JsonChecker(requestJson),
                            tailResponseJson));

            cluster.start();
            {
                final HttpPost post = new HttpPost(cluster.server().host() + url);
                post.setEntity(
                        new StringEntity(
                                requestJson,
                                ContentType.APPLICATION_JSON));
                try (CloseableHttpResponse response = client.execute(post)) {
                    HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                    YandexAssert.check(
                            new JsonChecker(headResponseJson),
                            CharsetUtils.toString(response.getEntity()));
                }

                Thread.sleep(1000);

                final TailStater.TailStat stat = cluster.server().tailsStaters().get("testing").stat();
                Assert.assertEquals(
                        1,
                        stat.total()
                );
                Assert.assertEquals(
                        0,
                        stat.failedParsing()
                );
                Assert.assertEquals(
                        0,
                        stat.failedRequest()
                );
                Assert.assertEquals(
                        0,
                        stat.mismatchedResolution()
                );
            }

            {
                final HttpPost post = new HttpPost(cluster.server().host() + url);
                post.setEntity(
                        new StringEntity(
                                requestJson,
                                ContentType.APPLICATION_JSON));
                try (CloseableHttpResponse response = client.execute(post)) {
                    HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                    YandexAssert.check(
                            new JsonChecker(headResponseJson),
                            CharsetUtils.toString(response.getEntity()));
                }

                Thread.sleep(1000);

                final TailStater.TailStat stat = cluster.server().tailsStaters().get("testing").stat();
                Assert.assertEquals(
                        2,
                        stat.total()
                );
                Assert.assertEquals(
                        0,
                        stat.failedParsing()
                );
                Assert.assertEquals(
                        0,
                        stat.failedRequest()
                );
                Assert.assertEquals(
                        1,
                        stat.mismatchedResolution()
                );
            }

            Assert.assertEquals(
                    2,
                    cluster.head().accessCount(url));
            Assert.assertEquals(
                    2,
                    cluster.tail().accessCount(url));
        }
    }

    @Test
    public void testOut() throws Exception {
        try (Cluster cluster = new Cluster(
                this,
                resource("mail/so/daemons/sp_kitsune/sp_kitsune_config/files/sp-kitsune-out.conf"),
                "OUT")) {
            cluster.start();
        }
    }

    @Test
    public void testCorp() throws Exception {
        try (Cluster cluster = new Cluster(
                this,
                resource("mail/so/daemons/sp_kitsune/sp_kitsune_config/files/sp-kitsune-corp.conf"),
                "CORP")) {
            cluster.start();
        }
    }

    @Test
    public void testTesting() throws Exception {
        try (Cluster cluster = new Cluster(
                this,
                resource("mail/so/daemons/sp_kitsune/sp_kitsune_config/files/sp-kitsune-testing.conf"),
                "IN")) {
            cluster.start();
        }
    }
}
