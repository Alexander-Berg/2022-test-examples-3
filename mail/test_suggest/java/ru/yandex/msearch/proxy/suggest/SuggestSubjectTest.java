package ru.yandex.msearch.proxy.suggest;

import java.util.List;
import java.util.Map;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import ru.yandex.blackbox.BlackboxUserinfo;
import ru.yandex.msearch.proxy.MsearchProxyCluster;
import ru.yandex.msearch.proxy.MsearchProxyTestBase;
import ru.yandex.msearch.proxy.config.SubjectSuggestConfig;
import ru.yandex.search.prefix.LongPrefix;
import ru.yandex.test.util.YandexAssert;

public class SuggestSubjectTest extends MsearchProxyTestBase {
    private static final String SUBJECT_API =
        "/api/async/mail/suggest/subject?";

    public static List<String> subjectSuggest(
        final MsearchProxyCluster cluster,
        final CloseableHttpClient client,
        final String request)
        throws Exception
    {
        return AsyncSuggestTest.suggests(cluster, client, SUBJECT_API, request);
    }

    @Test
    public void testSubjectSuggest() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();

            String uri = "/_status?service=change_log&prefix=0&allow_cached"
                + "&all&json-type=dollar";
            //cluster.producer().add(uri, "localhost");
            cluster.backend().add(
                doc(
                    "100500",
                    "\"thread_id\":100500," +
                        "\"received_date\":\"1234567890\"," +
                        "\"hdr_subject\":" +
                        "\"ЗАКАЗ: Linnen Cotton Pillow Case\"," +
                        "\"hdr_subject_normalized\":" +
                        "\"ЗАКАЗ: Linnen Cotton Pillow Case\"",
                    ""),

                doc(
                    "100501",
                    "\"thread_id\":100501," +
                        "\"received_date\":\"1234567891\"," +
                        "\"hdr_subject\":\"" +
                        "Ваш заказ с сайта горизонт когда то будет\"," +
                        "\"hdr_subject_normalized\":\"" +
                        "Ваш заказ с сайта горизонт когда то будет\"",
                    ""),

                doc(
                    "100502",
                    "\"thread_id\":100502," +
                        "\"received_date\":\"1234567892\"," +
                        "\"hdr_subject\":" +
                        "\"Фото мастерская Вучетича\"," +
                        "\"hdr_subject_normalized\":" +
                        "\"Фото мастерская Вучетича\"",
                    ""),
                doc(
                    "100503",
                    "\"thread_id\":100503,\"received_date\":\"1234567893\","
                        + "\"hdr_subject\":" +
                        "\"Всем привет в этом чатике\","
                        + "\"hdr_subject_normalized\":" +
                        "\"Всем привет в этом чатике\"",
                    ""),
                doc(
                    "100504",
                    "\"thread_id\":100503,\"received_date\":\"1234567894\","
                        + "\"hdr_subject\":\"Re: Всем привет в этом чатике\","
                        + "\"hdr_subject_normalized\":" +
                        "\"Всем привет в этом чатике\"",
                    ""),
                doc(
                    "100505",
                    "\"thread_id\":100505,\"received_date\":\"1234567895\","
                        + "\"hdr_subject\":" +
                        "\"Фото тем селфи больше ада\","
                        + "\"hdr_subject_normalized\":" +
                        "\"Фото тем селфи больше ада\"",
                    ""),
                doc(
                    "100506",
                    "\"thread_id\":100506,\"received_date\":\"1234567896\","
                        + "\"hdr_subject\":" +
                        "\"Фото тем селфи больше ада\","
                        + "\"hdr_subject_normalized\":" +
                        "\"Фото тем селфи больше ада\"",
                    ""),
                doc(
                    "100507",
                    "\"thread_id\":100507,\"received_date\":\"1234567897\","
                        + "\"hdr_subject\":" +
                        "\"Marketoria Маркетория внутри\","
                        + "\"hdr_subject_normalized\":" +
                        "\"Marketoria Маркетория внутри\"",
                    ""),
                doc(
                    "100508",
                    "\"thread_id\":100508,\"received_date\":\"1234567898\","
                        + "\"hdr_subject\":" +
                        "\"Маркетория внутри\","
                        + "\"hdr_subject_normalized\":" +
                        "\"Маркетория внутри\"",
                    ""),
                doc(
                    "100509",
                    "\"thread_id\":100509,\"received_date\":\"1234567899\","
                        + "\"hdr_subject\":" +
                        "\"Marketoria внутри\","
                        + "\"hdr_subject_normalized\":" +
                        "\"Marketoria внутри\"",
                    ""),
                doc(
                    "100510",
                    "\"thread_id\":100510,\"received_date\":\"1234567900\","
                        + "\"hdr_subject\":" +
                        "\"No subject\","
                        + "\"hdr_subject_normalized\":" +
                        "\"No subject\"," +
                        "\"hdr_subject_keyword\":" +
                        "\"No subject\"",
                    ""),
                doc(
                    "100511",
                    "\"thread_id\":100511,\"received_date\":\"1234567901\","
                        + "\"message_type\":\"4 people\"," +
                        "\"hdr_subject\":" +
                        "\"Type order 2\"," +
                        "\"hdr_subject_normalized\":" +
                        "\"Type order 2\"," +
                        "\"hdr_subject_keyword\":" +
                        "\"Type order\"",
                    ""),
                doc(
                    "100512",
                    "\"thread_id\":100512,\"received_date\":\"1234567902\","
                        + "\"message_type\":\"13 news\"," +
                        "\"hdr_subject\":" +
                        "\"Type order 4\"," +
                        "\"hdr_subject_normalized\":" +
                        "\"Type order 4\"," +
                        "\"hdr_subject_keyword\":" +
                        "\"Type order\"",
                    ""),
                doc(
                    "100513",
                    "\"thread_id\":100513,\"received_date\":\"1234567903\","
                        + "\"message_type\":\"22 personalnews\"," +
                        "\"hdr_subject\":" +
                        "\"Type order 3\"," +
                        "\"hdr_subject_normalized\":" +
                        "\"Type order 3\"," +
                        "\"hdr_subject_keyword\":" +
                        "\"Type order\"",
                    ""),
                doc(
                    "100514",
                    "\"thread_id\":100514,\"received_date\":\"1234567904\","
                        + "\"message_type\":\"4 people\"," +
                        "\"hdr_subject\":" +
                        "\"Type order 1\"," +
                        "\"hdr_subject_normalized\":" +
                        "\"Type order 1\"," +
                        "\"hdr_subject_keyword\":" +
                        "\"Type order\"",
                    ""),
                doc(
                    "100515",
                    "\"thread_id\":100515,\"received_date\":\"1234567905\","
                        + "\"message_type\":\"4 people\"," +
                        "\"hdr_subject\":" +
                        "\"Фото мастерская Вучетича\"," +
                        "\"hdr_subject_normalized\":" +
                        "\"Фото мастерская Вучетича\"," +
                        "\"folder_type\":\"spam\"," +
                        "\"hdr_subject_keyword\":" +
                        "\"Фото мастерская Вучетича\"",
                    "")
            );

            //cluster.producer().add(
            //   "/_status?service=opqueue&prefix=0&allow_cached", "localhost");

            List<String> suggests = subjectSuggest(cluster, client, "фото вуч");

            Assert.assertEquals("Expecting 1", 1, suggests.size());
            Assert.assertEquals("Фото мастерская Вучетича", suggests.get(0));

            suggests = subjectSuggest(cluster, client, "привет");
            Assert.assertEquals("Expecting 1", 1, suggests.size());
            Assert.assertEquals("Всем привет в этом чатике", suggests.get(0));

            suggests = subjectSuggest(cluster, client, "зака");
            Assert.assertEquals("Expecting 2", 2, suggests.size());
            Assert.assertEquals("Ваш заказ с сайта горизонт когда то будет",
                                suggests.get(0));
            Assert.assertEquals(
                "ЗАКАЗ: Linnen Cotton Pillow Case",
                suggests.get(1));

            // morpho test
            suggests = subjectSuggest(cluster, client, "мастерская вуч");
            Assert.assertEquals("Expecting 1", 1, suggests.size());
            Assert.assertEquals("Фото мастерская Вучетича", suggests.get(0));

            // non word symbols test
            suggests = subjectSuggest(cluster, client, " # мастерская ^ * вуч");
            Assert.assertEquals("Expecting 1", 1, suggests.size());
            Assert.assertEquals("Фото мастерская Вучетича", suggests.get(0));

            // not searching for small amounts of letters test
            SubjectSuggestConfig sConfig =
                cluster.proxy().config().suggestConfig().subjectConfig();

            int minWordSize = sConfig.subjectMinimumWordLength();

            String request = "мастерская";
            for (int i = 0; i < request.length(); i++) {
                suggests = subjectSuggest(
                    cluster,
                    client,
                    request.substring(0, i + 1));

                if (i + 1 < minWordSize) {
                    Assert.assertEquals(0, suggests.size());
                } else {
                    if (i >= 3 ){
                        Assert.assertEquals("Expecting 1", 1, suggests.size());
                        Assert.assertEquals(
                            "Фото мастерская Вучетича",
                            suggests.get(0));
                    } else {
                        YandexAssert.assertGreater(0, suggests.size());
                    }
                }
            }

            // test duplicates
            suggests = subjectSuggest(cluster, client, "селфи");
            Assert.assertEquals(1, suggests.size());
            Assert.assertEquals("Фото тем селфи больше ада", suggests.get(0));
            // test mail count
            List<Map<String, String>> mappedSuggests =
                AsyncSuggestTest.suggestsParamMap(
                    cluster,
                    client,
                    SUBJECT_API,
                    "mdb=mdb200&suid=0&limit=10&request=привет&new-format");

            Assert.assertEquals(1, mappedSuggests.size());
            Assert.assertEquals(
                "Всем привет в этом чатике",
                mappedSuggests.get(0).get("show_text"));
            Assert.assertEquals(
                2,
                Integer.parseInt(mappedSuggests.get(0).get("mail-count")));

            // testing result order, original first
            suggests = subjectSuggest(cluster, client, "Маркетори");

            Assert.assertEquals(3, suggests.size());
            Assert.assertEquals("Маркетория внутри", suggests.get(0));
            Assert.assertEquals("Marketoria Маркетория внутри", suggests.get(1));
            Assert.assertEquals("Marketoria внутри", suggests.get(2));

            suggests = subjectSuggest(cluster, client, "в  ");
            Assert.assertEquals(1, suggests.size());
            Assert.assertEquals("Всем привет в этом чатике", suggests.get(0));

            // test no subject
            suggests = subjectSuggest(cluster, client, "без темы");
            Assert.assertEquals(1, suggests.size());
            Assert.assertEquals("No subject", suggests.get(0));

            // remove morpho for small words
            suggests = subjectSuggest(cluster, client, "тем");
            Assert.assertEquals(1, suggests.size());
            Assert.assertEquals("Фото тем селфи больше ада",
                                suggests.get(0));

            //testing message_type_order priority
            suggests = subjectSuggest(cluster, client, "order");
            Assert.assertEquals(4, suggests.size());
            Assert.assertEquals("Type order 1", suggests.get(0));
            Assert.assertEquals("Type order 2", suggests.get(1));
            Assert.assertEquals("Type order 3", suggests.get(2));
            Assert.assertEquals("Type order 4", suggests.get(3));

            //testing not suggesting from trash
            cluster.backend().add(
                doc(
                    "100515",
                    "\"thread_id\":\"\"," +
                        "\"received_date\":\"1234567905\"," +
                        "\"folder_type\":\"trash\"," +
                        "\"hdr_subject\":" +
                        "\"Удаленное не очень важное письмо\"," +
                        "\"hdr_subject_normalized\":" +
                        "\"Удаленное не очень важное письмо\"",
                    ""),
                doc(
                    "100516",
                    "\"thread_id\":100516," +
                        "\"received_date\":\"1234567906\"," +
                        "\"folder_type\":\"inbox\"," +
                        "\"hdr_subject\":" +
                        "\"Очень Важное письмо\"," +
                        "\"hdr_subject_normalized\":" +
                        "\"Очень Важное письмо\"",
                    "")
            );

            suggests = subjectSuggest(cluster, client, "важное");
            Assert.assertEquals(1, suggests.size());
            Assert.assertEquals("Очень Важное письмо", suggests.get(0));
        }
    }

    @Test
    public void testCorpOnlyInbox() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            LongPrefix prefix =
                new LongPrefix(BlackboxUserinfo.CORP_UID_BEGIN + 1);
            cluster.backend().add(
                prefix,
                doc(
                    "100500",
                    "\"thread_id\":100500"
                        + ",\"hdr_subject\":\"Hello sweetie\""
                        + ",\"received_date\":\"1234567890\""
                        + ",\"fid\":\"10\""
                        + ",\"folder_type\":\"inbox\"",
                    ""),
                doc(
                    "100501",
                    "\"thread_id\":100501,"
                        + "\"hdr_subject\":\"Hello bad one\","
                        + "\"received_date\":\"1234567890\""
                        + ",\"fid\":\"15\""
                        + ",\"folder_type\":\"sent\"",
                    ""));

            String request = "mdb=mdb200&suid="
                + (BlackboxUserinfo.CORP_UID_BEGIN + 1)
                + "&limit=10&request=hell";

            List<String> suggests = AsyncSuggestTest.suggestsParam(
                cluster,
                client,
                SUBJECT_API,
                request + "&folder=inbox");

            Assert.assertEquals(1, suggests.size());
            Assert.assertEquals("Hello sweetie", suggests.get(0));


            suggests = AsyncSuggestTest.suggestsParam(
                cluster,
                client,
                SUBJECT_API,
                request + "&fid=10");

            Assert.assertEquals(1, suggests.size());
            Assert.assertEquals("Hello sweetie", suggests.get(0));
        }
    }
}
