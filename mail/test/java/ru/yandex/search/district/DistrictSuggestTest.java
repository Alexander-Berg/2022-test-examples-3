package ru.yandex.search.district;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Test;

import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.search.prefix.LongPrefix;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

public class DistrictSuggestTest extends TestBase {
    // CSOFF: MultipleStringLiterals
    // CSOFF: MagicNumber
    private static void add(
        final DistrictSearchCluster cluster,
        final String request,
        final int count)
        throws Exception
    {
        cluster.searchBackend().add(
            new LongPrefix(DistrictPopularRequestsFields.prefix()),
            "\"id\":\"reqs_" + request + "\",\"request_text\":\""
                + request + "\",\"request_count\":" + count);
    }

    @Test
    public void testPopular() throws Exception {
        try (DistrictSearchCluster cluster =
                 new DistrictSearchCluster(this);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            add(cluster, "привет мало", 9);
            add(cluster, "привет уже норм", 11);
            add(cluster, "привет очень норм", 21);
            String base = cluster.proxy().host()
                + "/api/district/suggest/popular?request=";
            HttpGet get = new HttpGet(base + "при");
            cluster.addStatus("30");
            try (CloseableHttpResponse response = client.execute(get)) {
                String responseStr =
                    CharsetUtils.toString(response.getEntity());
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "[{\"highlight\": [[0, 3]], "
                            + "\"text\": \"привет очень норм\"},"
                            + "{\"highlight\": [[0, 3]], "
                            + "\"text\":\"привет уже норм\"}]"),
                    responseStr);
            }

            get = new HttpGet(base + "привет+нор");
            try (CloseableHttpResponse response = client.execute(get)) {
                String responseStr =
                    CharsetUtils.toString(response.getEntity());
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "[{\"highlight\": [[0, 6], [13, 16]], "
                            + "\"text\":\"привет очень норм\"}, "
                            + "{\"highlight\": [[0, 6], [11, 14]], "
                            + "\"text\":\"привет уже норм\"}]"),
                    responseStr);
            }

            add(cluster, "привет e;t норм", 10);
            get = new HttpGet(base + "e;");
            try (CloseableHttpResponse response = client.execute(get)) {
                String responseStr =
                    CharsetUtils.toString(response.getEntity());
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "[{\"highlight\": [[7, 9]], "
                            + "\"text\":\"привет уже норм\"}, "
                            + "{\"highlight\": [[7, 9]], "
                            + "\"text\": \"привет e;t норм\"}]"),
                    responseStr);
            }
        }
    }
    // CSON: MultipleStringLiterals
    // CSON: MagicNumber
}

