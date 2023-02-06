package ru.yandex.mail.so2.skeleton;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Test;

import ru.yandex.http.test.Configs;
import ru.yandex.http.test.ExpectingHeaderHttpItem;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.test.StaticHttpItem;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.http.util.YandexHeaders;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

public class So2SkeletonTest extends TestBase {
    public So2SkeletonTest() {
        super(false, 0L);
    }

    @Test
    public void test() throws Exception {
        try (So2SkeletonCluster cluster = new So2SkeletonCluster();
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.blackbox().add(
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1"
                + "&get_family_info&sid=smtp&uid=5598601",
                new StaticHttpResource(
                    new ExpectingHeaderHttpItem(
                        new StaticHttpItem(
                            loadResourceAsString(
                                "analizer-blackbox-userinfo.json")),
                        YandexHeaders.X_YA_SERVICE_TICKET,
                        So2SkeletonCluster.BLACKBOX_TVM_TICKET)));
            cluster.blackbox().add(
                "/blackbox/?format=json&method=family_info&family_id=f12984",
                new ExpectingHeaderHttpItem(
                    new StaticHttpItem(
                        loadResourceAsString("family-info.json")),
                    YandexHeaders.X_YA_SERVICE_TICKET,
                    So2SkeletonCluster.BLACKBOX_TVM_TICKET));
            cluster.start();

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.skeleton().host()
                            + "/?uid=5598601&resolve-family-members")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString("full-response.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.skeleton().host()
                            + "/?uid=5598601")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString("short-response.json")),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }
}

