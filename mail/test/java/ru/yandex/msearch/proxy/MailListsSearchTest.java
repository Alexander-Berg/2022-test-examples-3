package ru.yandex.msearch.proxy;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Test;

import ru.yandex.http.test.HttpAssert;
import ru.yandex.search.prefix.LongPrefix;

public class MailListsSearchTest extends MsearchProxyTestBase {
    protected static String mlFieldEnvelope(
        final String name,
        final String value)
    {
        if (value != null) {
            return "\"" + name +  "\":\"" + value + "\"";
        }

        return "\"" + name +  "\":null";
    }

    protected static String mlEnvelope(
        final String mid,
        final String threadId,
        final String date,
        final String subject,
        final String from)
    {
        return envelope(
            mid,
            mlFieldEnvelope("threadId", threadId),
            mlFieldEnvelope("date", date),
            mlFieldEnvelope("threadId", threadId),
            mlFieldEnvelope("subject", subject),
            "\"from\":[{\"local\": \"" + from + "\"}]");
    }

    @Test
    public void testMl() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this, true);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();

            cluster.producer().add("/_status*", "[{$localhost\0:100500}]");
            cluster.backend().add(
                new LongPrefix(1120000000002819L),
                // basic date ordering and morphology test
                doc(
                    "100500",
                    "\"hdr_to_normalized\":\"potapovd@gmail.com\""
                        + ",\"received_date\":\"1234567890\""
                        + ",\"hdr_subject\":\"Миру мир\"",
                    "\"pure_body\":\"иди\""));

            cluster.backend().add(
                new LongPrefix(1120000000002677L),
                // basic date ordering and morphology test
                doc(
                    "100501",
                    "\"hdr_to_normalized\":\"potapovd@gmail.com\""
                        + ",\"received_date\":\"1234567890\""
                        + ",\"hdr_subject\":\"Миру search мир\"",
                    "\"pure_body\":\"иди\""),
                doc(
                    "100502",
                    "\"hdr_to_normalized\":\"self@buratino.ru\""
                        + ",\"received_date\":\"1234567891\""
                        + ",\"hdr_subject\":\"Полундра, кто то разбил наш индекс\"",
                    "\"pure_body\":\"А вот нечего было катать из интересных репозиториев\""));

            cluster.corpFilterSearch().add(
                "/filter_search?order=default&folder_set=default&uid=1120000000002819&mdb=pg&mids=100500",
                envelopes(
                    "",
                    mlEnvelope("100500", "200500", "123456", "Миру мир", "potapovd")));

            //mlEnvelope("100502", "200502", "1234567891", "Полундра, кто то разбил наш индекс", "self")
            cluster.corpFilterSearch().add(
                "/filter_search?order=default&folder_set=default&uid=1120000000002677&mdb=pg&mids=100501",
                envelopes(
                    "",
                    mlEnvelope("100501", "200501", "1234567890", "Миру мир", "potapovd")));

            cluster.corpMl().add(
                "/apiv3/lists/staff_maillists?&uid=1120000000040290",
                "{\"owner_of\":[], " +
                "  \"has_direct_read_permission\": [\n" +
                "    {\n" +
                "      \"uid\": 1120000000001272,\n" +
                "      \"name\": \"mail-dev\",\n" +
                "      \"id\": 2113,\n" +
                "      \"email\": \"mail-dev@yandex-team.ru\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"uid\": 1120000000002940,\n" +
                "      \"name\": \"staff\",\n" +
                "      \"id\": 3536,\n" +
                "      \"email\": \"staff@yandex-team.ru\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"uid\": 1120000000025397,\n" +
                "      \"name\": \"personal-services\",\n" +
                "      \"id\": 10435,\n" +
                "      \"email\": \"personal-services@yandex-team.ru\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"uid\": 1120000000175717,\n" +
                "      \"name\": \"main-staff\",\n" +
                "      \"id\": 42721,\n" +
                "      \"email\": \"main-staff@yandex-team.ru\"\n" +
                "    }\n" +
                "  ]," +
                "\"has_group_read_permission\": [\n" +
                "    {\n" +
                "      \"uid\": 1120000000002819,\n" +
                "      \"name\": \"search\",\n" +
                "      \"id\": 2396,\n" +
                "      \"email\": \"search@yandex-team.ru\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"uid\": 1120000000002777,\n" +
                "      \"name\": \"redrose-announces\",\n" +
                "      \"id\": 2541,\n" +
                "      \"email\": \"redrose-announces@yandex-team.ru\"\n" +
                "    }]," +
                "\"subscribed_to\": [\n" +
                "    {\n" +
                "      \"uid\": 1120000000002819,\n" +
                "      \"name\": \"search\",\n" +
                "      \"id\": 436291,\n" +
                "      \"email\": \"search@yandex-team.ru\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"uid\": 1120000000025397,\n" +
                "      \"name\": \"personal-services\",\n" +
                "      \"id\": 436294,\n" +
                "      \"email\": \"personal-services@yandex-team.ru\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"uid\": 1120000000002940,\n" +
                "      \"name\": \"staff\",\n" +
                "      \"id\": 436321,\n" +
                "      \"email\": \"staff@yandex-team.ru\"\n" +
                "    }]," +
                "\"open\": [\n" +
                "    {\n" +
                "      \"uid\": 1120000000002677,\n" +
                "      \"name\": \"pddadm-dev\",\n" +
                "      \"id\": 655,\n" +
                "      \"email\": \"pddadm-dev@yandex-team.ru\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"uid\": 1120000000002851,\n" +
                "      \"name\": \"semark\",\n" +
                "      \"id\": 656,\n" +
                "      \"email\": \"semark@yandex-team.ru\"\n" +
                "    }]}");

            //mlEnvelope("100500", "200500", "123456", "Миру мир", "potapovd")));
            //mlEnvelope("100501", "200501", "1234567890", "Миру мир", "potapovd")));
            HttpAssert.assertJsonResponse(
                client,
                cluster.proxy().host() + "/api/async/ml/search?uid=1120000000040290&request=мир",
                "{\"meta\":{\"page\":0,\"per_page\":200,\"next\":null,\"doc_count\":2,\"pages_count\":0," +
                    "\"maillists\":[{\"name\":\"pddadm-dev\",\"count\":1},{\"name\":\"search\",\"count\":1}]}," +
                    "\"results\":[" +
                    "{\"snippet\":{\"id\":\"100500\",\"thread_id\":\"200500\",\"title\":\"Миру мир\",\"time\":\"123456\"," +
                    "\"author\":\"potapovd\",\"maillist\":\"search\",\"text\":null}}," +
                    "{\"snippet\":{\"id\":\"100501\",\"thread_id\":\"200501\",\"title\":\"Миру мир\"," +
                    "\"time\":\"1234567890\",\"author\":\"potapovd\",\"maillist\":\"pddadm-dev\",\"text\":null}}" +
                    "]}");

            HttpAssert.assertJsonResponse(
                client,
                cluster.proxy().host() + "/api/async/ml/search?uid=1120000000040290&request=мир&maillist=pddadm-dev",
                "{\"meta\":{\"page\":0,\"per_page\":200,\"next\":null,\"doc_count\":1,\"pages_count\":0," +
                    "\"maillists\":[{\"name\":\"pddadm-dev\",\"count\":1}]}," +
                    "\"results\":[" +
                    "{\"snippet\":{\"id\":\"100501\",\"thread_id\":\"200501\",\"title\":\"Миру мир\"," +
                    "\"time\":\"1234567890\",\"author\":\"potapovd\",\"maillist\":\"pddadm-dev\",\"text\":null}}" +
                    "]}");

            HttpAssert.assertJsonResponse(
                client,
                cluster.proxy().host() + "/api/async/ml/search?uid=1120000000040290&request=search+мир",
                "{\"meta\":{\"page\":0,\"per_page\":200,\"next\":null,\"doc_count\":2,\"pages_count\":0," +
                    "\"maillists\":[{\"name\":\"search\",\"count\":1}, {\"name\":\"pddadm-dev\",\"count\":1}]}," +
                    "\"results\":[" +
                    "{\"snippet\":{\"id\":\"100500\",\"thread_id\":\"200500\",\"title\":\"Миру мир\",\"time\":\"123456\"," +
                    "\"author\":\"potapovd\",\"maillist\":\"search\",\"text\":null}}," +
                    "{\"snippet\":{\"id\":\"100501\",\"thread_id\":\"200501\",\"title\":\"Миру мир\"," +
                    "\"time\":\"1234567890\",\"author\":\"potapovd\",\"maillist\":\"pddadm-dev\",\"text\":null}}" +
                    "]}");

            HttpAssert.assertJsonResponse(
                client,
                cluster.proxy().host() + "/api/async/ml/search?uid=1120000000040290&request=pddadm-dev+мир",
                "{\"meta\":{\"page\":0,\"per_page\":200,\"next\":null,\"doc_count\":1,\"pages_count\":0," +
                    "\"maillists\":[{\"name\":\"pddadm-dev\",\"count\":1}]}," +
                    "\"results\":[" +
                    "{\"snippet\":{\"id\":\"100501\",\"thread_id\":\"200501\",\"title\":\"Миру мир\"," +
                    "\"time\":\"1234567890\",\"author\":\"potapovd\",\"maillist\":\"pddadm-dev\",\"text\":null}}" +
                    "]}");
        }
    }
}