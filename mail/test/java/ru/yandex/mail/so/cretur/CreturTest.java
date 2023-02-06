package ru.yandex.mail.so.cretur;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Test;

import ru.yandex.http.test.Configs;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.http.util.YandexHeaders;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

public class CreturTest extends TestBase {
    public CreturTest() {
        super(false, 0L);
    }

    @Test
    public void test() throws Exception {
        try (CreturCluster cluster = new CreturCluster(this);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.prepareDatabase();
            cluster.start();

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.cretur().httpHost()
                            + "/get-organization-ip-whitelist?org_id=123")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_NOT_FOUND, response);
            }

            HttpPost post =
                new HttpPost(
                    cluster.cretur().httpHost()
                    + "/set-organization-ip-whitelist?org_id=123");
            post.setEntity(
                new StringEntity(
                    loadResourceAsString("whitelist0.json"),
                    ContentType.APPLICATION_JSON));
            post.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                CreturCluster.SINGLE_SO_TVM_TICKET);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            }

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.cretur().httpHost()
                            + "/get-organization-ip-whitelist?org_id=123")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(loadResourceAsString("whitelist0.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            post =
                new HttpPost(
                    cluster.cretur().httpHost()
                    + "/set-organization-ip-whitelist?org_id=123");
            post.setEntity(
                new StringEntity(
                    loadResourceAsString("whitelist1.json"),
                    ContentType.APPLICATION_JSON));
            post.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                CreturCluster.SINGLE_SO_TVM_TICKET);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            }

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.cretur().httpHost()
                            + "/get-organization-ip-whitelist?org_id=123")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(loadResourceAsString("whitelist1.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            post =
                new HttpPost(
                    cluster.cretur().httpHost()
                    + "/set-organization-ip-whitelist?org_id=456");
            post.setEntity(
                new StringEntity(
                    loadResourceAsString("whitelist2.json"),
                    ContentType.APPLICATION_JSON));
            post.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                CreturCluster.SINGLE_SO_TVM_TICKET);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            }

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.cretur().httpHost()
                            + "/set-organization-daily-limit"
                            + "?org_id=456&daily_limit=100")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            }

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.cretur().httpHost()
                            + "/get-all-organizations-settings")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(loadResourceAsString("settings1.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            post =
                new HttpPost(
                    cluster.cretur().httpHost()
                    + "/set-organization-ip-whitelist?org_id=456");
            post.setEntity(
                new StringEntity(
                    loadResourceAsString("empty-whitelist.json"),
                    ContentType.APPLICATION_JSON));
            post.addHeader(
                YandexHeaders.X_YA_SERVICE_TICKET,
                CreturCluster.SINGLE_SO_TVM_TICKET);
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            }

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.cretur().httpHost()
                            + "/get-all-organizations-settings")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(loadResourceAsString("settings2.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.cretur().httpHost()
                            + "/delete-organization-ip-whitelist?org_id=123")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            }

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.cretur().httpHost()
                            + "/get-all-organizations-settings")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(loadResourceAsString("settings3.json")),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testIpWhitelistMaxSize() throws Exception {
        try (CreturCluster cluster = new CreturCluster(this);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.prepareDatabase();
            cluster.start();

            int ipWhitelistMaxSize =
                cluster.cretur().config().ipWhitelistMaxSize();
            StringBuilder sb = new StringBuilder("{\"ip_whitelist\":[");
            for (int i = 0; i < ipWhitelistMaxSize; ++i) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append("\"192.168.0.");
                sb.append(i);
                sb.append('"');
            }
            sb.append(']');
            sb.append('}');
            String whitelist = new String(sb);

            HttpPost post =
                new HttpPost(
                    cluster.cretur().httpHost()
                    + "/set-organization-ip-whitelist?org_id=123");
            post.setEntity(
                new StringEntity(whitelist, ContentType.APPLICATION_JSON));
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            }

            post =
                new HttpPost(
                    cluster.cretur().httpHost()
                    + "/set-organization-ip-whitelist?org_id=123");
            post.setEntity(
                new StringEntity(
                    whitelist.replace(
                        "192.168.0.0",
                        "192.168.0.0\",\"192.168.0.255"),
                    ContentType.APPLICATION_JSON));
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_REQUEST_TOO_LONG,
                    response);
            }
        }
    }
}

