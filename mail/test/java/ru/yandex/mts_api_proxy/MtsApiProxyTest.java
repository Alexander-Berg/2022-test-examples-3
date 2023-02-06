package ru.yandex.mts_api_proxy;

import java.nio.file.Files;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.junit.Test;

import ru.yandex.http.test.Configs;
import ru.yandex.http.test.ExpectingHeaderHttpItem;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.test.StaticHttpItem;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.http.util.YandexHeaders;
import ru.yandex.test.util.StringChecker;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

public class MtsApiProxyTest extends TestBase {
    @Test
    public void test() throws Exception {
        try (Cluster cluster = new Cluster(this);
             CloseableHttpClient client = Configs.createDefaultClient()) {

            //grant_type=client_credentials
            cluster.gozora().add(
                    "/",
                    new ExpectingHeaderHttpItem(
                            new StaticHttpItem(Files.readString(resource("token.json"))),
                            new BasicHeader(YandexHeaders.X_YA_SERVICE_TICKET, Cluster.GOZORA_TVM_TICKET),
                            new BasicHeader("Authorization", "Basic " +
                                    "TVRTX0FQSV9DT05TVU1FUl9LRVk6TVRTX0FQSV9DT05TVU1FUl9TRUNSRVQ="),
                            new BasicHeader("x-ya-dest-url", "https://api.mts.ru:443/token"),
                            new BasicHeader("x-ya-client-id", "mts_api_proxy")),
                    new ExpectingHeaderHttpItem(
                            new StaticHttpItem(Files.readString(resource("score.json"))),
                            new BasicHeader(YandexHeaders.X_YA_SERVICE_TICKET, Cluster.GOZORA_TVM_TICKET),
                            new BasicHeader("Authorization", "Bearer " +
                                    "6a6734f0e-065e-342339-93bb-8d12342e1164"),
                            new BasicHeader("x-ya-dest-url", "https://api.mts.ru:443/bnkscor/1.1.0/score"),
                            new BasicHeader("x-ya-client-id", "mts_api_proxy")));
            cluster.start();


            {
                final HttpGet get = new HttpGet(cluster.server().host() + "/score?msisdn=79257197019");

                try (CloseableHttpResponse response = client.execute(get)) {
                    HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                    YandexAssert.check(
                            new StringChecker("0.66"),
                            CharsetUtils.toString(response.getEntity()));
                }
            }
        }
    }
}

