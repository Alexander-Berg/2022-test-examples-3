package ru.yandex.msearch.proxy;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.http.test.ExpectingHttpItem;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.http.util.YandexHeaders;
import ru.yandex.msearch.proxy.config.ImmutableMsearchProxyConfig;
import ru.yandex.msearch.proxy.config.MsearchProxyConfigBuilder;
import ru.yandex.parser.config.IniConfig;
import ru.yandex.passport.tvmauth.BlackboxEnv;
import ru.yandex.search.prefix.LongPrefix;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.YandexAssert;

public class SoTest extends MsearchProxyTestBase {
    @Test
    public void testQuery() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this, true);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            cluster.producer().add(
                "/_status?service=change_log&prefix=26657"
                    + "&allow_cached&all&json-type=dollar",
                "[{\"localhost\":100500}]");

            cluster.backend().add(new LongPrefix(991949281),
                "\"url\": \"so_user_weight_991949281_40_-32421_in\",\n"
                    + "\"uw_shingle\": -32421,\n"
                    + "\"uw_type\": 40,\n"
                    + "\"uw_so_type\": \"in\",\n"
                    + "\"uw_timeset\": 1589923000,\n"
                    + "\"uw_lastset\": 1589923010,\n"
                    + "\"uw_weight\": 0.5,\n"
                    + "\"uw_setwghtcnt\": 1,\n"
                    + "\"uw_duration\": 120,\n"
                    + "\"uw_user\": \"usr\",\n"
                    + "\"uw_should_be_malic\": 1,\n"
                    + "\"uw_tags\": \"tag1\\ntag2\",\n"
                    + "\"uw_valid_until\": 1589923130\n",
                "\"url\": \"so_user_weight_991949281_40_-32421_out\",\n"
                    + "\"uw_shingle\": -32421,\n"
                    + "\"uw_type\": 40,\n"
                    + "\"uw_so_type\": \"out\",\n"
                    + "\"uw_timeset\": 1589923222,\n"
                    + "\"uw_lastset\": 1589923223,\n"
                    + "\"uw_weight\": 0.9,\n"
                    + "\"uw_setwghtcnt\": 4,\n"
                    + "\"uw_duration\": 221,\n"
                    + "\"uw_user\": \"usr\",\n"
                    + "\"uw_should_be_malic\": 1,\n"
                    + "\"uw_valid_until\": 1589923444\n",
                "\"url\": \"so_user_weight_991949281_15_123_out\",\n"
                    + "\"uw_shingle\": 123,\n"
                    + "\"uw_type\": 15,\n"
                    + "\"uw_so_type\": \"out\",\n"
                    + "\"uw_timeset\": 1589923111,\n"
                    + "\"uw_lastset\": 1589923111,\n"
                    + "\"uw_weight\": 0.9,\n"
                    + "\"uw_setwghtcnt\": 1,\n"
                    + "\"uw_duration\": 221,\n"
                    + "\"uw_user\": \"usr\",\n"
                    + "\"uw_should_be_malic\": 1,\n"
                    + "\"uw_valid_until\": 1589923332\n");

            HttpGet request = new HttpGet(
                cluster.proxy().host() + "/api/async/so/get-user-weights");
            String expected =
                " [{"
                + "    \"duration\": \"221\","
                + "    \"user\": \"usr\","
                + "    \"valid_until\": \"1589923444\","
                + "    \"shingle\": \"-32421\","
                + "    \"weight\": \"0.9\","
                + "    \"so_type\": \"out\","
                + "    \"timeset\": \"1589923222\","
                + "    \"should_be_malic\": \"1\","
                + "    \"setwghtcnt\": \"4\","
                + "    \"type\": \"40\","
                + "    \"lastset\": \"1589923223\""
                + "  },"
                + "  {"
                + "    \"duration\": \"120\","
                + "    \"user\": \"usr\","
                + "    \"valid_until\": \"1589923130\","
                + "    \"shingle\": \"-32421\","
                + "    \"weight\": \"0.5\","
                + "    \"so_type\": \"in\","
                + "    \"timeset\": \"1589923000\","
                + "    \"should_be_malic\": \"1\","
                + "    \"tags\": \"tag1\\ntag2\","
                + "    \"setwghtcnt\": \"1\","
                + "    \"type\": \"40\","
                + "    \"lastset\": \"1589923010\""
                + "  },"
                + "  {"
                + "    \"duration\": \"221\","
                + "    \"user\": \"usr\","
                + "    \"valid_until\": \"1589923332\","
                + "    \"shingle\": \"123\","
                + "    \"weight\": \"0.9\","
                + "    \"so_type\": \"out\","
                + "    \"timeset\": \"1589923111\","
                + "    \"should_be_malic\": \"1\","
                + "    \"setwghtcnt\": \"1\","
                + "    \"type\": \"15\","
                + "    \"lastset\": \"1589923111\""
                + "  }]";
            try (CloseableHttpResponse response = client.execute(request)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(expected),
                    CharsetUtils.toString(response.getEntity()));
            }

            request = new HttpGet(
                cluster.proxy().host()
                    + "/api/async/so/get-user-weights?valid_until=1589923300");
            expected =
                "  [{"
                + "    \"duration\": \"221\","
                + "    \"user\": \"usr\","
                + "    \"valid_until\": \"1589923444\","
                + "    \"shingle\": \"-32421\","
                + "    \"weight\": \"0.9\","
                + "    \"so_type\": \"out\","
                + "    \"timeset\": \"1589923222\","
                + "    \"should_be_malic\": \"1\","
                + "    \"setwghtcnt\": \"4\","
                + "    \"type\": \"40\","
                + "    \"lastset\": \"1589923223\""
                + "  },"
                + "  {"
                + "    \"duration\": \"221\","
                + "    \"user\": \"usr\","
                + "    \"valid_until\": \"1589923332\","
                + "    \"shingle\": \"123\","
                + "    \"weight\": \"0.9\","
                + "    \"so_type\": \"out\","
                + "    \"timeset\": \"1589923111\","
                + "    \"should_be_malic\": \"1\","
                + "    \"setwghtcnt\": \"1\","
                + "    \"type\": \"15\","
                + "    \"lastset\": \"1589923111\""
                + "  }]";
            try (CloseableHttpResponse response = client.execute(request)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(expected),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }


    @Test
    public void testUpdate() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this, true);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            cluster.producer().add(
                "/_status?service=change_log&prefix=991949281"
                + "&allow_cached&all&json-type=dollar",
                "[{\"localhost\":100500}]");

            cluster.producer().add(
                "/update?prefix=991949281&service=change_log",
                // first request
                new ExpectingHttpItem(new JsonChecker(
                    "{\"prefix\":991949281,\"AddIfNotExists\":true,"
                    + "\"docs\":[{\"uw_weight\":\"0.1\","
                    + "\"uw_setwghtcnt\":{\"function\":\"inc\"},"
                    + "\"uw_lastset\":\"1589923400\",\"uw_shingle\":\"123\","
                    + "\"uw_type\":\"15\",\"uw_so_type\":\"out\","
                    + "\"uw_valid_until\":{\"function\":\"sum\","
                    + "\"args\":[1589923400,{\"function\":\"get\", "
                    + "\"args\":[\"uw_duration\"]}]},"
                    + "\"url\":\"so_user_weight_991949281_15_123_out\"}]}")),
                // second request
                new ExpectingHttpItem(new JsonChecker(
                    "{\"prefix\":991949281,\"AddIfNotExists\":true,"
                    + "\"docs\":[{\"uw_timeset\":\"1589923300\","
                    + "\"uw_weight\":\"0.32\",\"uw_user\":\"inuse\","
                    + "\"uw_duration\":\"30\",\"uw_setwghtcnt\":5,"
                    + "\"uw_lastset\":\"1589923320\",\"uw_shingle\":\"365\","
                    + "\"uw_type\":\"4\",\"uw_so_type\":\"in\","
                    + "\"uw_tags\": \"first_tag\\nsecond_tag\","
                    + "\"uw_valid_until\":{\"function\":\"sum\","
                    + "\"args\":[1589923320,30]},"
                    + "\"uw_should_be_malic\":\"1\","
                    + "\"url\":\"so_user_weight_991949281_4_365_in\"}]}")));

            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                client,
                new HttpGet(
                    cluster.proxy().host()
                    + "/api/async/so/update-user-weight?shingle=123&type=15"
                    + "&weight=0.1&setwghtcnt=inc&so_type=out"
                    + "&lastset=1589923400"));

            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                client,
                new HttpGet(
                    cluster.proxy().host()
                    + "/api/async/so/update-user-weight?shingle=365&type=4"
                    + "&timeset=1589923300&lastset=1589923320&so_type=in"
                    + "&weight=0.32&setwghtcnt=5"
                    + "&duration=30&user=inuse"
                    + "&should_be_malic=1" 
                    + "&tags=first_tag%0Asecond_tag"));

            Assert.assertEquals(
                "Not all requests to backend generated",
                cluster.producer()
                    .accessCount(
                        "/update?prefix=991949281&service=change_log"), 2);
        }
    }


    @Test
    public void testSobbGetScore() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this, true)
            {
                @Override
                public ImmutableMsearchProxyConfig config(
                        final MproxyClusterContext clusterContext,
                        final IniConfig iniConfig) throws Exception
                {
                    MsearchProxyConfigBuilder config =
                            new MsearchProxyConfigBuilder(
                                    super.config(clusterContext, iniConfig));
                    config.tvm2ServiceConfig().blackboxEnv(BlackboxEnv.TEST);
                    return config.build();
                }
            };
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            cluster.producer().add(
                "/_status?service=change_log&prefix=28211"
                    + "&allow_cached&all&json-type=dollar",
                "[{\"localhost\":100500}]");

            cluster.backend().add(
                new LongPrefix(5598601),
                "\"url\":\"sobb_hunter_5598601\",\n"
                + "\"sobb_inbox\":1,\n"
                + "\"sobb_reported_time\":0\n",
                "\"url\":\"sobb_record_5598601_123\",\n"
                + "\"sobb_inbox\":0\n",
                "\"url\":\"sobb_record_5598601_124\",\n"
                + "\"sobb_inbox\":1\n",
                "\"url\":\"sobb_record_5598601_125\",\n"
                + "\"sobb_inbox\":4\n");

            cluster.producer().add(
                "/update?prefix=5598601&service=change_log",
                new ExpectingHttpItem(new JsonChecker(
                    "{\n"
                    + "  \"prefix\": 5598601,\n"
                    + "  \"AddIfNotExists\": true,\n"
                    + "  \"docs\": [{\n"
                    + "      \"sobb_report_time\": \"<any value>\",\n"
                    + "      \"sobb_inbox\": 2,\n"
                    + "      \"url\": \"sobb_hunter_5598601\"\n"
                    + "}]}\n")));

            String expectedReport = "{\n"
                + "  \"inboxed_targets\": 2,\n"
                + "  \"report_time\": \"<any value>\"\n}\n";

            HttpPost request = new HttpPost(cluster.proxy().host()
                + "/api/async/so/sobb-get-score");
            request.addHeader(YandexHeaders.X_YA_USER_TICKET,
                "3:user:CA0Q__________9_GhQKBQiJ29UCEInb1QIg0oXYzAQoAQ:Gio3ai"
                + "xJPSSzMmGM_MtaDkD3OGLIwPTEH9A8c8g8xaSs7XbdtqwQNCWduWaPvUEg"
                + "1WYQpT1MYT_rpdx1kp265E4T8daAWbxz-YJX6N1KPbj11yhx7piTiOt3BD"
                + "HB39wqF5zU8QewV8B9IKU_CwQwHcHERSYUW1UVYgTU-NiYGRE");

            HttpAssert.assertJsonResponse(
                client,
                request,
                expectedReport);

            cluster.backend().update(new LongPrefix(5598601),
                "\"url\":\"sobb_hunter_5598601\",\n"
                    + "\"sobb_inbox\":2,\n"
                    + "\"sobb_report_time\":"
                    + (System.currentTimeMillis() / 1000)
            );

            HttpAssert.assertJsonResponse(
                client,
                request,
                expectedReport);

            Assert.assertEquals(
                "Not all requests to backend generated",
                1,
                cluster.producer().accessCount(
                    "/update?prefix=5598601&service=change_log"));
        }
    }

    @Test
    public void testResolveStid() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this, true);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.producer().add(
                "/_status?service=change_log&prefix=28211"
                    + "&allow_cached&all&json-type=dollar",
                "[{\"localhost\":100500}]");
            cluster.start();

            cluster.backend().add(
                new LongPrefix(5598601),
                "\"url\": \"5598601_100500/0\",\"stid\":\"st_id\","
                + "\"mid\":\"100500\",\"headers\":\"x-yandex-spam: 1\","
                + "\"received_date\":\"1234567890\"",
                "\"url\": \"5598601_100502/0\",\"stid\":\"st_id2\","
                + "\"mid\":\"100502\",\"headers\":\"x-yandex-spam: 4\","
                + "\"received_date\":\"1234567891\"");
            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.proxy().host()
                            + "/api/async/so/resolve-stid?"
                            + "uid=5598601&mid=100500")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    "{\"stid\":\"st_id\",\"received_date\":\"1234567890\"}",
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.proxy().host()
                            + "/api/async/so/resolve-stid?"
                            + "uid=5598601&mid=100501")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_NOT_FOUND, response);
            }
            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.proxy().host()
                            + "/api/async/so/resolve-stid?"
                            + "uid=5598601&mid=100502")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_NOT_FOUND, response);
            }
            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.proxy().host()
                            + "/api/async/so/resolve-stid?"
                            + "uid=28211&mid=100500")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_NOT_FOUND, response);
            }
        }
    }
}
