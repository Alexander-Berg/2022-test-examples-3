package ru.yandex.msearch.proxy;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.http.util.YandexHeaders;
import ru.yandex.search.prefix.LongPrefix;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.YandexAssert;

public class KeyboardHandlerTest extends MsearchProxyTestBase {
    private static final String URI = "/api/async/mail/keyboard";
    private static final String OAUTH_TOKEN =
        "AQAAAAAAVW2JAAEyK7awWn-2JkwNlKHnrg9ITaI";
    private static final String OAUTH_HEADER = "OAuth " + OAUTH_TOKEN;

    private static String prepareBlackbox(
        final MsearchProxyCluster cluster,
        final String ip)
    {
        String blackboxUri =
            "/blackbox/?format=json&method=oauth&userip=" + ip
            + "&dbfields=hosts.db_id.2,subscription.suid.2&oauth_token="
            + OAUTH_TOKEN + "&scopes=mail:search";
        cluster.blackbox().add(
            blackboxUri,
            "{\"error\": \"OK\",\"uid\": {\"hosted\": false,\"lite\": false,"
            + "\"value\": \"0\"},\"login\": \"Analizer\","
            + "\"have_password\": true,\"have_hint\": true,\"karma\": {"
            + "\"value\": 0},\"karma_status\": {\"value\": 6000},"
            + "\"status\": {\"id\": 0,\"value\": \"VALID\"},"
            + "\"dbfields\":{\"hosts.db_id.2\":\"pg\","
            + "\"subscription.suid.2\":\"2\"}}");
        return blackboxUri;
    }

    private static void prepareIndex(final MsearchProxyCluster cluster)
        throws Exception
    {
        cluster.backend().add(
            // Document without folder type, dunno how it is possible
            doc(
                "100500",
                "\"received_date\":\"1234567890\"",
                "\"pure_body\":\"Миру мир\""),
            // Document in inbox
            doc(
                "100501",
                "\"received_date\":\"1234567891\","
                + "\"folder_type\":\"inbox\"",
                "\"pure_body\":\"Мир дверь мяч\""),
            // Documents in sent folder
            doc(
                "100502",
                "\"received_date\":\"1234567892\","
                + "\"folder_type\":\"sent\",\"thread_id\":\"100500\"",
                "\"pure_body\":\"Миру полудня\"",
                "\"body_text\":\"Текст книги\""),
            doc(
                "100503",
                "\"received_date\":\"1234567893\","
                + "\"folder_type\":\"sent\",\"thread_id\":100503",
                "\"pure_body\":\"Дивный-дивный Босх\"",
                "\"body_text\":\"Текст картины\""),
            // Document in drafts folder
            doc(
                "100504",
                "\"received_date\":\"1234567894\","
                + "\"folder_type\":\"draft\",\"thread_id\":\"100504\","
                + "\"hdr_subject_normalized\":\"Тестовый тест\"",
                "\"pure_body\":\"Дивный новый мир passw0rd\"",
                "\"body_text\":\"attachment contents\""));
        cluster.backend().add(
            "\"url\": \"senders_uid_0_juliyas@yandex-team.ru\","
            + "\"senders_uid\": \"0\","
            + "\"senders_last_contacted\": \"1486638203\","
            + "\"senders_sent_count\": \"7\","
            + "\"senders_names\": \"0\\njuliyas\","
            + "\"senders_lcn\": \"2\"",
            "\"url\":\"senders_uid_0_nanton@mera.ru\","
            + "\"senders_uid\": \"0\","
            + "\"senders_last_contacted\": \"1243329531\","
            + "\"senders_received_count\": \"6\","
            + "\"senders_sent_count\": \"6\","
            + "\"senders_names\": \"0\\nAntonova, Natalya"
            + "\\nnanton@mera.ru\\nНаташа Антонова\","
            + "\"senders_lcn\": \"1\"",
            "\"url\": \"senders_uid_0_dserikov@yandex-team.ru\","
            + "\"senders_uid\": \"0\","
            + "\"senders_last_contacted\": \"1274360847\","
            + "\"senders_received_count\": \"5\","
            + "\"senders_sent_count\": \"8\","
            + "\"senders_names\": \"0\\nDenis Serikov\"");
        cluster.backend().add(
            new LongPrefix(1L),
            "\"url\": \"senders_uid_1_potapov@ihps.nnov.ru\","
            + "\"senders_uid\": \"1\","
            + "\"senders_last_contacted\": \"1448007658\","
            + "\"senders_received_count\": \"4\","
            + "\"senders_sent_count\": \"6\","
            + "\"senders_names\": \"0\\nPotapov Alexander M.\","
            + "\"senders_lcn\": \"1\"");
    }

    @Test
    public void test() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            prepareIndex(cluster);
            String blackboxUri = prepareBlackbox(cluster, "127.0.0.1");
            // Test request without Authorization header
            try (CloseableHttpResponse response =
                    client.execute(new HttpGet(cluster.proxy().host() + URI)))
            {
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_UNAUTHORIZED,
                    response);
            }
            Assert.assertEquals(
                0,
                cluster.blackbox().accessCount(blackboxUri));
            // Request all data
            HttpGet get = new HttpGet(cluster.proxy().host() + URI);
            get.addHeader(HttpHeaders.AUTHORIZATION, OAUTH_HEADER);
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "{\"subject-words\":{"
                        + "\"words-total\":5,"
                        + "\"frequency-total\":5,"
                        + "\"exact-words-total\":2,"
                        + "\"exact-frequency-total\":2,"
                        + "\"top-words\":["
                        + "{\"word\":\"тест\",\"frequency\":1,\"exact\":true},"
                        + "{\"word\":\"тестовый\",\"frequency\":1,"
                        + "\"exact\":true},"
                        + "{\"word\":\"тест\",\"frequency\":1,"
                        + "\"exact\":false},"
                        + "{\"word\":\"тесто\",\"frequency\":1,"
                        + "\"exact\":false},"
                        + "{\"word\":\"тестовый\",\"frequency\":1,"
                        + "\"exact\":false}]},"
                        + "\"body-words\":{"
                        + "\"words-total\":14,"
                        + "\"frequency-total\":19,"
                        + "\"exact-words-total\":6,"
                        + "\"exact-frequency-total\":8,"
                        + "\"top-words\":["
                        + "{\"word\":\"дивный\",\"frequency\":3,"
                        + "\"exact\":true},"
                        + "{\"word\":\"дивный\",\"frequency\":3,"
                        + "\"exact\":false},"
                        + "{\"word\":\"мир\",\"frequency\":2,\"exact\":false},"
                        + "{\"word\":\"босх\",\"frequency\":1,\"exact\":true},"
                        + "{\"word\":\"мир\",\"frequency\":1,\"exact\":true},"
                        + "{\"word\":\"миру\",\"frequency\":1,\"exact\":true},"
                        + "{\"word\":\"новый\",\"frequency\":1,"
                        + "\"exact\":true},"
                        + "{\"word\":\"полудня\",\"frequency\":1,"
                        + "\"exact\":true},"
                        + "{\"word\":\"босх\",\"frequency\":1,"
                        + "\"exact\":false},"
                        + "{\"word\":\"мира\",\"frequency\":1,"
                        + "\"exact\":false},"
                        + "{\"word\":\"новый\",\"frequency\":1,"
                        + "\"exact\":false},"
                        + "{\"word\":\"полдень\",\"frequency\":1,"
                        + "\"exact\":false},"
                        + "{\"word\":\"полудень\",\"frequency\":1,"
                        + "\"exact\":false},"
                        + "{\"word\":\"полудній\",\"frequency\":1,"
                        + "\"exact\":false}]},"
                        + "\"contacts\":["
                        + "{\"email\":\"dserikov@yandex-team.ru\","
                        + "\"names\":[\"Denis Serikov\"],"
                        + "\"sent-count\":8,"
                        + "\"received-count\":5,"
                        + "\"last-contacted\":1274360847},"
                        + "{\"email\":\"juliyas@yandex-team.ru\","
                        + "\"names\":[\"juliyas\"],"
                        + "\"sent-count\":7,"
                        + "\"received-count\":0,"
                        + "\"last-contacted\":1486638203},"
                        + "{\"email\":\"nanton@mera.ru\","
                        + "\"names\":"
                        + "[\"Antonova, Natalya\",\"Наташа Антонова\"],"
                        + "\"sent-count\":6,"
                        + "\"received-count\":6,"
                        + "\"last-contacted\":1243329531}]}"),
                    CharsetUtils.toString(response.getEntity()));
            }
            Assert.assertEquals(
                1,
                cluster.blackbox().accessCount(blackboxUri));
            // Request no results
            get = new HttpGet(
                cluster.proxy().host() + URI
                + "?top-subject-words=0&top-body-words=0&top-contacts=0");
            get.addHeader(HttpHeaders.AUTHORIZATION, OAUTH_HEADER);
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "{\"subject-words\":{"
                        + "\"words-total\":0,"
                        + "\"frequency-total\":0,"
                        + "\"exact-words-total\":0,"
                        + "\"exact-frequency-total\":0,"
                        + "\"top-words\":[]},"
                        + "\"body-words\":{"
                        + "\"words-total\":0,"
                        + "\"frequency-total\":0,"
                        + "\"exact-words-total\":0,"
                        + "\"exact-frequency-total\":0,"
                        + "\"top-words\":[]},"
                        + "\"contacts\":[]}"),
                    CharsetUtils.toString(response.getEntity()));
            }
            Assert.assertEquals(
                2,
                cluster.blackbox().accessCount(blackboxUri));
        }
    }

    @Test
    public void testPagingAndProducer() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this, true);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            prepareIndex(cluster);
            String producerUri =
                "/_status?service=change_log&prefix=0&allow_cached"
                + "&all&json-type=dollar";
            cluster.producer().add(producerUri, "[{\"localhost\":100500}]");
            String ip = "10.100.0.1";
            String blackboxUri = prepareBlackbox(cluster, ip);
            HttpGet get = new HttpGet(
                cluster.proxy().host() + URI
                + "?top-subject-words=1&top-body-words=3&top-contacts=1");
            get.addHeader(HttpHeaders.AUTHORIZATION, OAUTH_HEADER);
            get.addHeader(YandexHeaders.X_REAL_IP, ip);
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "{\"subject-words\":{"
                        + "\"words-total\":5,"
                        + "\"frequency-total\":5,"
                        + "\"exact-words-total\":2,"
                        + "\"exact-frequency-total\":2,"
                        + "\"top-words\":["
                        + "{\"word\":\"тест\",\"frequency\":1,"
                        + "\"exact\":true}]},"
                        + "\"body-words\":{"
                        + "\"words-total\":14,"
                        + "\"frequency-total\":19,"
                        + "\"exact-words-total\":6,"
                        + "\"exact-frequency-total\":8,"
                        + "\"top-words\":["
                        + "{\"word\":\"дивный\",\"frequency\":3,"
                        + "\"exact\":true},"
                        + "{\"word\":\"дивный\",\"frequency\":3,"
                        + "\"exact\":false},"
                        + "{\"word\":\"мир\",\"frequency\":2,"
                        + "\"exact\":false}]},"
                        + "\"contacts\":["
                        + "{\"email\":\"dserikov@yandex-team.ru\","
                        + "\"names\":[\"Denis Serikov\"],"
                        + "\"sent-count\":8,"
                        + "\"received-count\":5,"
                        + "\"last-contacted\":1274360847}]}"),
                    CharsetUtils.toString(response.getEntity()));
            }
            Assert.assertEquals(
                1,
                cluster.blackbox().accessCount(blackboxUri));
            Assert.assertEquals(
                1,
                cluster.producer().accessCount(producerUri));
        }
    }

    @Test
    public void testTvmHandlerWithFilter() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            prepareIndex(cluster);
            String uri =
                cluster.proxy().host() + URI
                + "-tvm?uid=0&top-subject-words=0&top-contacts=0";
            HttpGet get = new HttpGet(uri + "&first-in-thread");
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "{\"body-words\":{"
                        + "\"words-total\":8,"
                        + "\"frequency-total\":12,"
                        + "\"exact-words-total\":4,"
                        + "\"exact-frequency-total\":6,"
                        + "\"top-words\":["
                        + "{\"word\":\"дивный\",\"frequency\":3,"
                        + "\"exact\":true},"
                        + "{\"word\":\"дивный\",\"frequency\":3,"
                        + "\"exact\":false},"
                        + "{\"word\":\"босх\",\"frequency\":1,\"exact\":true},"
                        + "{\"word\":\"мир\",\"frequency\":1,\"exact\":true},"
                        + "{\"word\":\"новый\",\"frequency\":1,"
                        + "\"exact\":true},"
                        + "{\"word\":\"босх\",\"frequency\":1,"
                        + "\"exact\":false},"
                        + "{\"word\":\"мир\",\"frequency\":1,"
                        + "\"exact\":false},"
                        + "{\"word\":\"новый\",\"frequency\":1,"
                        + "\"exact\":false}]},"
                        + "\"subject-words\":{"
                        + "\"words-total\":0,"
                        + "\"frequency-total\":0,"
                        + "\"exact-words-total\":0,"
                        + "\"exact-frequency-total\":0,"
                        + "\"top-words\":[]},"
                        + "\"contacts\":[]}"),
                    CharsetUtils.toString(response.getEntity()));
            }

            get =
                new HttpGet(uri + "&from-date=1234567891&to-date=1234567892");
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "{\"body-words\":{"
                        + "\"words-total\":7,"
                        + "\"frequency-total\":7,"
                        + "\"exact-words-total\":2,"
                        + "\"exact-frequency-total\":2,"
                        + "\"top-words\":["
                        + "{\"word\":\"миру\",\"frequency\":1,"
                        + "\"exact\":true},"
                        + "{\"word\":\"полудня\",\"frequency\":1,"
                        + "\"exact\":true},"
                        + "{\"word\":\"мир\",\"frequency\":1,\"exact\":false},"
                        + "{\"word\":\"мира\",\"frequency\":1,"
                        + "\"exact\":false},"
                        + "{\"word\":\"полдень\",\"frequency\":1,"
                        + "\"exact\":false},"
                        + "{\"word\":\"полудень\",\"frequency\":1,"
                        + "\"exact\":false},"
                        + "{\"word\":\"полудній\",\"frequency\":1,"
                        + "\"exact\":false}]},"
                        + "\"subject-words\":{"
                        + "\"words-total\":0,"
                        + "\"frequency-total\":0,"
                        + "\"exact-words-total\":0,"
                        + "\"exact-frequency-total\":0,"
                        + "\"top-words\":[]},"
                        + "\"contacts\":[]}"),
                    CharsetUtils.toString(response.getEntity()));
            }

            get =
                new HttpGet(uri + "&from-date=1234567894&first-in-thread");
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "{\"body-words\":{"
                        + "\"words-total\":6,"
                        + "\"frequency-total\":6,"
                        + "\"exact-words-total\":3,"
                        + "\"exact-frequency-total\":3,"
                        + "\"top-words\":["
                        + "{\"word\":\"дивный\",\"frequency\":1,"
                        + "\"exact\":true},"
                        + "{\"word\":\"мир\",\"frequency\":1,"
                        + "\"exact\":true},"
                        + "{\"word\":\"новый\",\"frequency\":1,"
                        + "\"exact\":true},"
                        + "{\"word\":\"дивный\",\"frequency\":1,"
                        + "\"exact\":false},"
                        + "{\"word\":\"мир\",\"frequency\":1,"
                        + "\"exact\":false},"
                        + "{\"word\":\"новый\",\"frequency\":1,"
                        + "\"exact\":false}]},"
                        + "\"subject-words\":{"
                        + "\"words-total\":0,"
                        + "\"frequency-total\":0,"
                        + "\"exact-words-total\":0,"
                        + "\"exact-frequency-total\":0,"
                        + "\"top-words\":[]},"
                        + "\"contacts\":[]}"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }
}

