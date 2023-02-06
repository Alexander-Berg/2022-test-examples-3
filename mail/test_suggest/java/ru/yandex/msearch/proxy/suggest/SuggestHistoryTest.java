package ru.yandex.msearch.proxy.suggest;

import java.util.List;

import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.msearch.proxy.MsearchProxyCluster;
import ru.yandex.msearch.proxy.MsearchProxyTestBase;
import ru.yandex.msearch.proxy.SinxProxyHandler;
import ru.yandex.msearch.proxy.api.async.mail.rules.StoreSearchRequestRule;
import ru.yandex.msearch.proxy.api.async.suggest.BasicSuggests;
import ru.yandex.msearch.proxy.api.async.suggest.history.UpdateStoredRequest;
import ru.yandex.msearch.proxy.api.async.suggest.united.Target;
import ru.yandex.msearch.proxy.config.ImmutableMsearchProxyConfig;
import ru.yandex.msearch.proxy.config.MsearchProxyConfigBuilder;
import ru.yandex.parser.config.IniConfig;
import ru.yandex.parser.uri.QueryConstructor;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.YandexAssert;

public class SuggestHistoryTest extends MsearchProxyTestBase {
    private static final String HISTORY_API = "/api/async/suggest/history?";

    private static List<String> historySuggest(
        final MsearchProxyCluster cluster,
        final CloseableHttpClient client,
        final String request)
        throws Exception
    {
        return AsyncSuggestTest.suggests(cluster, client, HISTORY_API, request);
    }

    @Test
    @Ignore
    public void testHistoryReplaceWithSenderAndReceiver() throws Exception {

        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this, true) {
            @Override
            public ImmutableMsearchProxyConfig config(
                final MproxyClusterContext clusterContext,
                final IniConfig iniConfig) throws Exception
            {
                MsearchProxyConfigBuilder config =
                    new MsearchProxyConfigBuilder(
                        super.config(clusterContext, iniConfig));
                config.suggestConfig().historyToContacts(true);
                return config.build();
            }
        };
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            cluster.backend().add(
                "\"url\":\"reqs_0_vonidu\","
                    + "\"request_raw\":\"vonidu\","
                    + "\"request_normalized\":\"vonidu\","
                    + "\"request_spaceless\":\"vonidu\","
                    + "\"request_date\":\"1\","
                    + "\"request_count\":\"34\","
                    + "\"request_mids\":\"159877786771652706\"",
                "\"url\":\"reqs_0_vonidu@yandex.ru\","
                    + "\"request_raw\":\"vonidu@yandex.ru\","
                    + "\"request_normalized\":\"vonidu@yandex.ru\","
                    + "\"request_spaceless\":\"vonidu@yandex.ru\","
                    + "\"request_date\":\"2\",\"request_count\":\"1\"",
                "\"url\":\"reqs_0_от:vonidu@yandex.ru\","
                    + "\"request_raw\":\"от:vonidu@yandex.ru\","
                    + "\"request_normalized\":\"от vonidu@yandex.ru"
                    + ".com\",\"request_spaceless\":\"от"
                    + "vonidu@yandex.ru\",\"request_date\":\"3\","
                    + "\"request_count\":\"2\"",
                "\"url\":\"reqs_0_от von@yandex.ru\","
                    + "\"request_raw\":\"от von@yandex.ru\","
                    + "\"request_normalized\":\"от von@yandex.ru"
                    + ".com\",\"request_spaceless\":\"от"
                    + "von@yandex.ru\",\"request_date\":\"4\","
                    + "\"request_count\":\"2\"",
                "\"url\":\"reqs_0_от: voni@yandex.ru\","
                    + "\"request_raw\":\"от: voni@yandex.ru\","
                    + "\"request_normalized\":\"от voni@yandex.ru"
                    + ".com\",\"request_spaceless\":\"от"
                    + "voni@yandex.ru\",\"request_date\":\"5\","
                    + "\"request_count\":\"2\""
            );

            cluster.producer().add(
                "/_status?service=opqueue&prefix=0&allow_cached"
                    + "&all&json-type=dollar",
                "[{\"localhost\":100500}]");
            cluster.producer().add(
                "/_status?service=change_log&prefix=0&allow_cached"
                    + "&all&json-type=dollar",
                "[{\"localhost\":100500}]");

            QueryConstructor qc =
                new QueryConstructor("/api/async/mail/suggest?");
            qc.append("mdb", "mdb200");
            qc.append("suid", "0");
            qc.append("request", "von");

            String expected =
                "[" +
                    "{\"target\":\"history\"," +
                    "\"show_text\":\"от von@yandex.ru\"," +
                    "\"search_params\":{}," +
                    "\"search_text\":\"от von@yandex.ru\"}," +
                    "{\"target\":\"history\"," +
                    "\"show_text\":\"vonidu@yandex.ru\"," +
                    "\"search_params\":{}," +
                    "\"search_text\":\"vonidu@yandex.ru\"}," +
                    "{\"target\":\"history\"," +
                    "\"show_text\":\"vonidu\"," +
                    "\"search_params\":{}," +
                    "\"search_text\":\"vonidu\"}," +
                    "{\"target\":\"contact\"," +
                    "\"show_text\":\"\\\"voni\\\" voni@yandex.ru\"," +
                    "\"display_name\":\"voni\"," +
                    "\"search_params\":{}," +
                    "\"search_text\":\"от:voni@yandex.ru\"," +
                    "\"email\": \"voni@yandex.ru\"," +
                    "\"unread_cnt\": 0}," +
                    "{\"target\":\"contact\"," +
                    "\"show_text\":\"\\\"vonidu\\\" vonidu@yandex.ru\"," +
                    "\"display_name\":\"vonidu\"," +
                    "\"search_params\":{}," +
                    "\"email\": \"vonidu@yandex.ru\"," +
                    "\"search_text\":\"от:vonidu@yandex.ru\"," +
                    "\"unread_cnt\": 0}" +
                    "]";

            try (CloseableHttpResponse response = client.execute(
                cluster.proxy().host(), new HttpGet(qc.toString())))
            {
                String responseStr = CharsetUtils.toString(response.getEntity());
                Assert.assertEquals(
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());

                YandexAssert.check(new JsonChecker(expected), responseStr);
            }
        }
    }

    @Test
    public void testHistorySuggest() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this, true);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            String producerURI = UpdateStoredRequest.API_SAVE_ROUTE + "*";
            String fsURI = "/filter_search?order=default&mdb=mdb200&suid=0" +
                "&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash";
            String defaultParams = "/api/async/mail/search?"
                + "mdb=mdb200&suid=0&first=0"
                + "&request=";

            cluster.start();
            cluster.backend().add(fixedDoc(
                "100500",
                "Заголовок письма",
                "Мой твое не понимать"));
            cluster.backend().add(fixedDoc(
                "100501",
                "100501",
                "Понимать мне тебя не очень просто"));
            cluster.backend().add(fixedDoc(
                "100502",
                "100502",
                "Мои Невероятные интересные " +
                    "приключения буратино Ва-типа шестого"));
            cluster.producer().add(
                "/_status?service=opqueue&prefix=0&allow_cached"
                    + "&all&json-type=dollar",
                "[{\"localhost\":100500}]");
            cluster.producer().add(
                "/_status?service=change_log&prefix=0&allow_cached"
                    + "&all&json-type=dollar",
                "[{\"localhost\":100500}]");

            filterSearch(cluster, fsURI, "100500");
            filterSearch(cluster, fsURI, "100502");
            filterSearch(cluster, fsURI, "100501");

            final HttpHost backendHost = new HttpHost(
                "localhost",
                cluster.backend().indexerPort());

            SinxProxyHandler indexerProxy = new SinxProxyHandler(
                backendHost,
                HttpPost.METHOD_NAME);

            cluster.producer().add(
                producerURI,
                new StaticHttpResource(indexerProxy));

            String searchRequest = "приключения";
            searchOk(
                cluster,
                client,
                defaultParams + searchRequest);

            indexerProxy.waitForRequests(1 ,10000);

            List<String> suggests;
            StringBuilder suggestRequest = new StringBuilder("");
            for (int i = 0; i < searchRequest.length(); i++) {
                suggestRequest.append(searchRequest.charAt(i));
                suggests = historySuggest(
                    cluster,
                    client,
                    suggestRequest.toString());

                Assert.assertEquals("Expecting 1 suggest", 1, suggests.size());
                Assert.assertEquals(searchRequest, suggests.get(0));
            }

            // Testing morpho
            suggests = historySuggest(cluster, client, "приключение");
            Assert.assertEquals("Expecting only 1 suggest", 1, suggests.size());
            Assert.assertEquals(searchRequest, suggests.get(0));

            //Testing wrong case
            suggests = historySuggest(cluster, client, "ПриКлюЧ");
            Assert.assertEquals("Expecting only 1 suggest", 1, suggests.size());
            Assert.assertEquals(searchRequest, suggests.get(0));

            //Testing wrong layout
            suggests = historySuggest(cluster, client, "Ghbrk.XT");
            Assert.assertEquals("Expecting only 1 suggest", 1, suggests.size());
            Assert.assertEquals(searchRequest, suggests.get(0));

            //Testing no suggest
            suggests = historySuggest(cluster, client, "преключ");
            Assert.assertEquals("Expecting no suggest", 0, suggests.size());


            searchOk(
                cluster,
                client,
                defaultParams + "приключения буратино");

            searchOk(
                cluster,
                client,
                defaultParams + "мои интересный приключения");

            searchOk(
                cluster,
                client,
                defaultParams + "мои интересный приключения");

            searchOk(
                cluster,
                client,
                defaultParams + "мои твои не понимай");

            // making last request greater timestamp
            Thread.sleep(1100);
            searchOk(
                cluster,
                client,
                defaultParams + "буратино шестого");

            indexerProxy.waitForRequests(5 ,10000);

            String multiSortParam = "mdb=mdb200&suid=0&limit=5&" +
                "sort_fields=request_count,request_date";
            suggests = AsyncSuggestTest.suggestsParam(
                cluster,
                client,
                HISTORY_API,
                multiSortParam + "&request=мои");

            Assert.assertEquals("Wrong suggests count", 2, suggests.size());
            Assert.assertEquals("мои интересный приключения", suggests.get(0));
            Assert.assertEquals("мои твои не понимай", suggests.get(1));

            suggests = AsyncSuggestTest.suggestsParam(
                cluster,
                client,
                HISTORY_API,
                multiSortParam + "&request=буратино");
            Assert.assertEquals("Wrong suggests count", 2, suggests.size());
            Assert.assertEquals("буратино шестого", suggests.get(0));
            Assert.assertEquals("приключения буратино", suggests.get(1));

            // Testing empty request
            suggests = historySuggest(cluster, client, "");
            Assert.assertEquals("Expecting 5 requests", 5, suggests.size());

            suggests = historySuggest(cluster, client, "   ");
            Assert.assertEquals("Expecting 5 requests", 5, suggests.size());

            // Testing missing request param
            suggests = historySuggest(cluster, client, null);
            Assert.assertEquals("Expecting 5 requests", 5, suggests.size());

            // Testing empty flag works
            suggests = AsyncSuggestTest.suggestsParam(
                cluster,
                client,
                HISTORY_API,
                "mdb=mdb200&suid=0&limit=5&empty=false");

            Assert.assertEquals("Expecting no suggests", 0, suggests.size());

            // Testing limits
            suggests = AsyncSuggestTest.suggestsParam(
                cluster,
                client,
                HISTORY_API,
                "mdb=mdb200&suid=0&limit=2");

            Assert.assertEquals("Expecting 2 requests", 2, suggests.size());

            // Testing legacy
            String blackboxUri = "/blackbox/?format=json&method=userinfo&" +
                "userip=127.0.0.1&dbfields=hosts.db_id.2&sid=2&suid=0";
            cluster.blackbox().add(
                blackboxUri,
                MsearchProxyCluster.blackboxResponse(0L, 0L, "mdb200"));

            suggests = AsyncSuggestTest.suggestsParam(
                cluster,
                client,
                HISTORY_API,
                "service=mailsuggest&how=req_timestamp&desc=yes" +
                    "&numdoc=2&kps=0&format=json_request");

            Assert.assertEquals("Expecting 2 requests", 2, suggests.size());

            suggests = AsyncSuggestTest.suggestsParam(
                cluster,
                client,
                HISTORY_API,
                "service=mailsuggest&how=req_timestamp&desc=yes" +
                    "&numdoc=2&kps=0&format=json_request&text=буратино");

            Assert.assertEquals("Expecting 2 requests", 2, suggests.size());
            Assert.assertEquals("буратино шестого", suggests.get(0));
            Assert.assertEquals("приключения буратино", suggests.get(1));

            // Testing pg user and order in fields
            suggests = AsyncSuggestTest.suggestsParam(
                cluster,
                client,
                HISTORY_API,
                "uid=0&limit=5&sort_fields=request_count,request_date" +
                    "&order=asc,desc&request=мои");

            Assert.assertEquals("Wrong suggests count", 2, suggests.size());
            Assert.assertEquals("мои твои не понимай", suggests.get(0));
            Assert.assertEquals("мои интересный приключения", suggests.get(1));

            // Testing ascending order
            suggests = AsyncSuggestTest.suggestsParam(cluster,
                                     client,
                                     HISTORY_API,
                                     "service=mailsuggest&how=req_timestamp&desc=no" +
                                         "&numdoc=2&kps=0&format=json_request&text=Буратино");
            Assert.assertEquals("Expecting 2 requests", 2, suggests.size());
            Assert.assertEquals("приключения буратино", suggests.get(0));
            Assert.assertEquals("буратино шестого", suggests.get(1));

            // Testing empty result because of empty hosts returned by producer
            cluster.producer().add(
                "/_status?service=opqueue&prefix=2&allow_cached"
                    + "&all&json-type=dollar", "[]");
            suggests = AsyncSuggestTest.suggestsParam(
                cluster,
                client,
                HISTORY_API,
                "mdb=mdb200&suid=2&limit=5&request=буратино");

            Assert.assertEquals("Expecting no suggests", 0, suggests.size());

            // Testing failing on no suid/uid provided
            try (CloseableHttpResponse response = client.execute(
                new HttpGet(
                    AsyncSuggestTest.suggestRequest(
                        cluster,
                        HISTORY_API,
                        "&limit=5&request=Буратино"))))
            {
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_BAD_REQUEST,
                    response);
                String responseTxt = CharsetUtils.toString(
                    response.getEntity());

                YandexAssert.assertContains(
                    "HTTP/1.1 400 Bad Request: No user id supplied",
                    responseTxt);
            }

            // Testing duplicates
            searchRequest = "  приключения  ";
            searchOk(
                cluster,
                client,
                defaultParams + searchRequest);
            indexerProxy.waitForRequests(1, 10000);

            suggests = historySuggest(cluster, client, "приключ");
            Assert.assertEquals(3, suggests.size());
            Assert.assertTrue(suggests.contains("приключения буратино"));
            Assert.assertTrue(suggests.contains("приключения"));
            Assert.assertTrue(suggests.contains("мои интересный приключения"));

            //Test BasicSuggests container itself, for the sake of coverage
            BasicSuggests suggestsContainer =
                new BasicSuggests(Target.UNITED, 2);

            suggestsContainer.add("suggest1");
            suggestsContainer.add("suggest2");
            suggestsContainer.add("suggest3");
            Assert.assertEquals("Expecting 2", 2, suggestsContainer.size());

            // test redundant spaces
            suggests = historySuggest(cluster, client, "мои твои   ");
            Assert.assertEquals(1, suggests.size());
            Assert.assertEquals("мои твои не понимай", suggests.get(0));

            // test emails in request
            searchRequest = "приключения vonidu@yandex-team.ru";
            searchOk(
                cluster,
                client,
                defaultParams + searchRequest);
            indexerProxy.waitForRequests(1, 10000);
            suggests = historySuggest(cluster, client, "приключения vonidu");
            Assert.assertEquals(1, suggests.size());
            Assert.assertEquals("приключения vonidu@yandex-team.ru",
                                suggests.get(0));

            //testing not storing small requests
            searchRequest = "пр";
            searchOk(
                cluster,
                client,
                defaultParams + searchRequest);
            indexerProxy.waitForNoRequests(1000);

            //testing cutting down legacy format orig_text prefix
            suggests = AsyncSuggestTest.suggestsParam(
                cluster,
                client,
                HISTORY_API,
                "service=mailsuggest&how=req_timestamp&desc=yes"
                    + "&numdoc=2&kps=0&format=json_request"
                    + "&text=orig_text:\"буратино\"*");

            Assert.assertEquals("Expecting 2 requests", 2, suggests.size());
            Assert.assertEquals("буратино шестого", suggests.get(0));
            Assert.assertEquals("приключения буратино", suggests.get(1));

            suggests = AsyncSuggestTest.suggestsParam(
                cluster,
                client,
                HISTORY_API,
                "service=mailsuggest&how=req_timestamp&desc=yes"
                    + "&numdoc=2&kps=0&format=json_request"
                    + "&text=url:\"*\"");

            Assert.assertEquals("Expecting 2 requests", 2, suggests.size());

            //testing experimental mixed
            cluster.backend().add(
                doc(
                    "100510",
                    "\"thread_id\":100510," +
                        "\"received_date\":\"1234567890\"," +
                        "\"hdr_subject\":" +
                        "\"Приключения Сени\"," +
                        "\"hdr_subject_normalized\":" +
                        "\"Приключения Сени\"",
                    ""));

            suggests = AsyncSuggestTest.suggestsParam(
                cluster,
                client,
                HISTORY_API,
                "service=mailsuggest&how=req_timestamp&desc=yes"
                    + "&numdoc=5&kps=0&format=json_request"
                    + "&text=orig_text:\"приключения\"*&subject=true");

            Assert.assertEquals(5, suggests.size());
            Assert.assertTrue(
                suggests.contains("приключения vonidu@yandex-team.ru"));
            Assert.assertTrue(suggests.contains("приключения буратино"));
            Assert.assertTrue(suggests.contains("приключения"));
            Assert.assertTrue(suggests.contains("мои интересный приключения"));
            Assert.assertEquals(
                suggests.get(4),
                "Приключения Сени");
        }
    }

    @Test
    public void testCaseInsensitive() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this, true);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            String producerURI = UpdateStoredRequest.API_SAVE_ROUTE + "*";
            String fsURI = "/filter_search?order=default&mdb=mdb200&suid=0" +
                "&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash";
            String defaultParams = "/api/async/mail/search?"
                + "mdb=mdb200&suid=0&first=0"
                + "&request=";

            cluster.start();
            cluster.backend().add(fixedDoc(
                "100500",
                "Заголовок письма",
                "Тело письма"));
            cluster.producer().add(
                "/_status?service=opqueue&prefix=0&allow_cached"
                    + "&all&json-type=dollar",
                "[{\"localhost\":100500}]");
            cluster.producer().add(
                "/_status?service=change_log&prefix=0&allow_cached"
                    + "&all&json-type=dollar",
                "[{\"localhost\":100500}]");

            filterSearch(cluster, fsURI, "100500");

            final HttpHost backendHost = new HttpHost(
                "localhost",
                cluster.backend().indexerPort());

            SinxProxyHandler indexerProxy = new SinxProxyHandler(
                backendHost,
                HttpPost.METHOD_NAME);

            cluster.producer().add(
                producerURI,
                new StaticHttpResource(indexerProxy));

            searchOk(
                cluster,
                client,
                defaultParams + "Заголовок");

            searchOk(
                cluster,
                client,
                defaultParams + "заголовок");

            indexerProxy.waitForRequests(2, 10000);

            List<String> suggests =
                historySuggest(cluster, client, "заг");

            Assert.assertEquals("Expecting only 1 suggest", 1, suggests.size());
            Assert.assertTrue("заголовок".equalsIgnoreCase(suggests.get(0)));
        }
    }
}
