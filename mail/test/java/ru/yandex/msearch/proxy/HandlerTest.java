package ru.yandex.msearch.proxy;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Test;

import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.test.search.backend.TestSearchBackend;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

public class HandlerTest extends TestBase {
    @Test
    public void test() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            cluster.backend().add(
                AsyncMailSearchTest.doc(
                    "100500",
                    "\"hdr_from\":\"dpotapov@yandex-team.ru\""
                    + ",\"received_date\":\"1234567890\"",
                    ""),
                AsyncMailSearchTest.doc(
                    "100501",
                    "\"hdr_from\":\"team@yandex.ru\""
                    + ",\"received_date\":\"1234567891\"",
                    ""),
                AsyncMailSearchTest.doc(
                    "100502",
                    "\"hdr_from\":\"yandex@team.ru\""
                    + ",\"received_date\":\"1234567892\"",
                    ""));
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/?user=0&db=pg&how=tm&format=json&getfields=mid"
                        + "&text=yandex+team&length=10")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        TestSearchBackend.prepareResult(
                            "\"mid\":\"100502\"",
                            "\"mid\":\"100501\"",
                            "\"mid\":\"100500\"")),
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/?user=0&db=pg&how=tm&format=json&getfields=mid"
                        + "&text=%22yandex-team%22&length=10")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        TestSearchBackend.prepareResult(
                            "\"mid\":\"100502\"",
                            "\"mid\":\"100500\"")),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }
}

