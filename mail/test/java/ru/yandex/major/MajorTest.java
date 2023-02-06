package ru.yandex.major;

import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.http.test.ExpectingHttpItem;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.test.StaticServer;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

public class MajorTest extends TestBase {
    private static final int TIMEOUT = 1000;
    private static final int DELAY = 100;
    private static final String UID1 = "65600";

    private static Function<Integer, Boolean> check(
        final StaticServer server,
        final String uri)
    {
        return (r) -> server.accessCount(uri) == r;
    }

    private static String yuidUpdate(final String uid, final String... yuids) {
        return "{\"prefix\": " + uid + ",\"AddIfNotExists\":\"true\","
            + "\"docs\":[{\"url\":\"user_gbl_" + uid + "\",\"yuids\":"
            + "{\"function\":\"make_set\",\"args\":[\""
            + String.join("\n", yuids) + "\",{\"function\":"
            + "\"get\",\"args\":[\"yuids\"]}]}}]}";
    }

    // CSOFF: MethodLength
    @Test
    public void test() throws Exception {
        try (MajorCluster cluster = new MajorCluster();
             CloseableHttpClient client = HttpClients.createDefault())
        {
            String updateUri = "/update?service=major&prefix=0";
            HttpPost post =
                new HttpPost(cluster.firstMajor().host() + updateUri);

            long ts = System.currentTimeMillis();
            String updateText = "{\"65600\": {\"ts\":" + ts
                + ",\"yuids\": [124, 235]},"
                + "\"65601\": {\"ts\":" + ts + ",\"yuids\": []},"
                + "\"61\": {\"ts\": " + ts + ",\"yuids\": [78]}}";

            post.setEntity(new StringEntity(updateText));
            String backupUri = updateUri + "&final";
            cluster.backupHead().add(
                backupUri,
                new ExpectingHttpItem(new JsonChecker(updateText)));

            String prodUri1 =
                "/update?&yuids&prefix=65600&service=change_log";
            cluster.producer().add(
                prodUri1,
                new ExpectingHttpItem(
                    new JsonChecker(
                        yuidUpdate(UID1, "124", "235"))));

            String prodUri2 =
                "/update?&yuids&prefix=65601&service=change_log";
            String prodUri3 =
                "/update?&yuids&prefix=61&service=change_log";
            cluster.producer().add(
                prodUri3,
                new ExpectingHttpItem(
                    new JsonChecker(yuidUpdate("61", "78"))));

            String proxyUri1 = "/api/async/enlarge/your?uid=65600";
            String proxyUri2 = "/api/async/enlarge/your?uid=65601";
            String proxyUri3 = "/api/async/enlarge/your?uid=61";
            cluster.proxy().add(proxyUri1, HttpStatus.SC_OK);
            cluster.proxy().add(proxyUri2, HttpStatus.SC_OK);
            cluster.proxy().add(proxyUri3, HttpStatus.SC_OK);

            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);

            waitRequests(
                check(cluster.backupHead(), backupUri), 1, TIMEOUT);
            waitRequests(check(cluster.producer(), prodUri1), 1, TIMEOUT);
            waitRequests(check(cluster.producer(), prodUri3), 1, TIMEOUT);
            waitRequests(check(cluster.proxy(), proxyUri1), 1, TIMEOUT);
            waitRequests(check(cluster.proxy(), proxyUri2), 1, TIMEOUT);
            waitRequests(check(cluster.proxy(), proxyUri3), 1, TIMEOUT);
            Assert.assertEquals(cluster.producer().accessCount(prodUri2), 0);

            HttpPost post2 =
                new HttpPost(cluster.firstMajor().host() + backupUri);
            post2.setEntity(new StringEntity(updateText));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post2);
            final long timeout = 1000;
            Thread.sleep(timeout);

            Assert.assertEquals(cluster.backupHead().accessCount(backupUri), 1);
            Assert.assertEquals(cluster.backupHead().accessCount(updateUri), 0);
            Assert.assertEquals(cluster.producer().accessCount(prodUri1), 1);
            Assert.assertEquals(cluster.producer().accessCount(prodUri2), 0);
            Assert.assertEquals(cluster.producer().accessCount(prodUri3), 1);

            String expected = "{\"online\": true,\"yuids\":[\"124\", \"235\"]}";
            try (CloseableHttpResponse response =
                     client.execute(
                         new HttpGet(
                             cluster.firstMajor().host()
                                 + "/get?uid=65600&get=online,yuids")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(expected),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response =
                     client.execute(
                         new HttpGet(
                             cluster.firstMajor().host()
                                 + "/online?uid=65600")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertHeader("Status", "Online", response);
                YandexAssert.check(
                    new JsonChecker("{\"online\": true}"),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response =
                     client.execute(
                         new HttpGet(
                             cluster.firstMajor().host()
                                 + "/get?uid=65630&get=online,yuids")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "{\"online\": false, \"yuids\":[]}"),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response =
                     client.execute(
                         new HttpGet(
                             cluster.firstMajor().host()
                                 + "/get?uid=65601&get=online,yuids")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "{\"online\": true, \"yuids\":[]}"),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response =
                     client.execute(
                         new HttpGet(
                             cluster.firstMajor().host()
                                 + "/get?uid=61&get=online,yuids")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "{\"online\": true, \"yuids\":[\"78\"]}"),
                    CharsetUtils.toString(response.getEntity()));
            }

            ts += 1;
            updateText = "{\"65600\":{\"ts\":" + ts + ",\"yuids\": [900]},"
                + "\"65601\":{\"ts\":" + ts + ",\"yuids\": [800]},"
                + "\"61\":{\"ts\": " + ts + ",\"yuids\":[78]}}";

            post.setEntity(new StringEntity(updateText));
            cluster.producer().add(
                prodUri1,
                new ExpectingHttpItem(
                    new JsonChecker(
                        yuidUpdate(UID1, "124\n235\n900"))));
            cluster.producer().add(
                prodUri2,
                new ExpectingHttpItem(
                    new JsonChecker(yuidUpdate("65601", "800"))));
            cluster.producer().add(
                prodUri3,
                HttpStatus.SC_INTERNAL_SERVER_ERROR);

            cluster.proxy().add(proxyUri1, HttpStatus.SC_INTERNAL_SERVER_ERROR);
            cluster.proxy().add(proxyUri2, HttpStatus.SC_INTERNAL_SERVER_ERROR);
            cluster.proxy().add(proxyUri3, HttpStatus.SC_INTERNAL_SERVER_ERROR);
            cluster.backupHead().add(
                backupUri,
                new ExpectingHttpItem(new JsonChecker(updateText)));

            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);
            waitRequests(check(cluster.producer(), prodUri1), 1, TIMEOUT);
            // 1 new request
            waitRequests(check(cluster.producer(), prodUri2), 1, TIMEOUT);
            // no request this time to third
            Assert.assertEquals(cluster.producer().accessCount(prodUri3), 0);

            Assert.assertEquals(cluster.proxy().accessCount(proxyUri1), 0);
            Assert.assertEquals(cluster.proxy().accessCount(proxyUri2), 0);
            Assert.assertEquals(cluster.proxy().accessCount(proxyUri3), 0);

            ts = 0;
            post.setEntity(new StringEntity(
                "{\"65639\": {\"ts\": " + ts + ",\"yuids\": [\"88\"]}}"));
            String producerUri4 =
                "/update?&yuids&prefix=65639&service=change_log";
            cluster.producer().add(
                producerUri4,
                new ExpectingHttpItem(
                    new JsonChecker(yuidUpdate("65639", "88"))));

            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);
            try (CloseableHttpResponse response =
                     client.execute(
                         new HttpGet(
                             cluster.firstMajor().host()
                                 + "/get?uid=65639&get=online,yuids")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "{\"online\": false, \"yuids\":[\"88\"]}"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }
    // CSON: MethodLength

    @Test
    public void testRedirect() throws Exception {
        HttpClientBuilder nonRedirectBuilder =
            HttpClients.custom().setDefaultRequestConfig(
                RequestConfig.copy(RequestConfig.DEFAULT)
                    .setRedirectsEnabled(false).build());
        try (MajorCluster cluster = new MajorCluster();
             CloseableHttpClient client = nonRedirectBuilder.build())
        {
            String redirectHost =
                "http://" + MajorCluster.SECOND_MAJOR
                    + ':' + cluster.secondMajor().port();

            String getUri = "/get?uid=65300&get=online,yuids";
            try (CloseableHttpResponse response =
                     client.execute(
                         new HttpGet(cluster.firstMajor().host() + getUri)))
            {
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_TEMPORARY_REDIRECT,
                    response);

                HttpAssert.assertHeader(
                    HttpHeaders.LOCATION,
                    redirectHost + getUri,
                    response);
            }

            String updateURi = "/update?prefix=65301";
            HttpPost post = new HttpPost(
                cluster.firstMajor().host() + updateURi);
            post.setEntity(new StringEntity("{}"));

            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_TEMPORARY_REDIRECT,
                    response);

                HttpAssert.assertHeader(
                    HttpHeaders.LOCATION,
                    redirectHost + updateURi,
                    response);
            }
        }
    }

    @Test
    public void forceYuidsUpdate() throws Exception {
        try (MajorCluster cluster = new MajorCluster();
             CloseableHttpClient client = HttpClients.createDefault())
        {
            String updateUri = "/forceYuidsUpdate?uid=65620&yuid=374&yuid=218";
            String prodUri1 =
                "/update?&yuids&prefix=65620&service=change_log";
            cluster.producer().add(
                prodUri1,
                new ExpectingHttpItem(
                    new JsonChecker(
                        yuidUpdate("65620", "374", "218"))));
            try (CloseableHttpResponse response =
                     client.execute(
                         new HttpGet(
                             cluster.firstMajor().host() + updateUri)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            }

            try (CloseableHttpResponse response =
                     client.execute(
                         new HttpGet(
                             cluster.firstMajor().host() + updateUri)))
            {
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_BAD_GATEWAY,
                    response);
            }
        }
    }

    private void waitRequests(
        final Function<Integer, Boolean> func,
        final int reqs,
        final int timeout)
        throws Exception
    {
        int waiting = 0;
        while (!func.apply(reqs)) {
            Thread.sleep(DELAY);
            waiting += DELAY;
            if (waiting > timeout) {
                throw new TimeoutException("Timeout waiting requests");
            }
        }
    }
}
