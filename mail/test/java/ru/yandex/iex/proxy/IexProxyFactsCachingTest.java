package ru.yandex.iex.proxy;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.UnaryOperator;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.collection.Pattern;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.ExpectingHeaderHttpItem;
import ru.yandex.http.test.ExpectingHttpItem;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.test.ProxyHandler;
import ru.yandex.http.test.StaticHttpItem;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.http.util.YandexHeaders;
import ru.yandex.http.util.request.RequestHandlerMapper;
import ru.yandex.json.dom.JsonList;
import ru.yandex.json.dom.JsonObject;
import ru.yandex.json.dom.TypesafeValueContentHandler;
import ru.yandex.test.search.backend.TestSearchBackend;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.StringChecker;
import ru.yandex.test.util.TestBase;

public class IexProxyFactsCachingTest extends TestBase {
    //private static final String MICRO = "micro";
    private static final String UID = "&uid=";
    //private static final String DOCS = "docs";
    private static final String FACTS = "/facts?mdb=pg";
    //private static final String PREFIX = "prefix";
    private static final String HTTP_LOCALHOST = "http://localhost:";
    private static final String NOTIFY = "/notify?mdb=pg";
    private static final String NOTIFY_TTEOT = "/notify-tteot?mdb=pg";
    //private static final String NOTIFY_TTEOT_PATTERN = "/notify-tteot*";
    private static final String EMPTY_ENVELOPES = "{\"envelopes\":[]}";
    //private static final String STORE_ENVELOPE = "store.json";
    private static final String STORE_PARAMS = "storefs.json";
    private static final String STORE_PARAMS_TWO_MIDS = "storefs_two_mids.json";
    private static final String POST_PARAMS = "post_params.json";
    private static final String POST_PARAMS_TWO_MIDS =
        "post_params_two_mids.json";
    private static final String FILTER_SEARCH =
            "/filter_search?order=default&full_folders_and_labels=1&uid=";
    private static final String TIMESTAMP_CONFIGS_PKPASS =
            "entities_actual_timestamps.pkpass = 147256398600\n";
    private static final int FOUR = 4;
    private static final long UID_VALUE = 1130000013896717L;
    private static final String CONFIG_EXTRA = "entities.default = events\n"
        + "postprocess.default2 = events:http://localhost:"
        + IexProxyCluster.IPORT + "/events\n";

    private UnaryOperator<String> configPostProc = config ->
        config.replace("entities.default = contentline\n", "");

    //CSOFF: MultipleStringLiterals
    @Test
    public void testEmptyFactsIndexingAndRetreiving() throws Exception {
        try (IexProxyCluster cluster =
            new IexProxyCluster(this, null, "", configPostProc, true, false);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            // Part 1: process /notify request
            // and check the facts are stored in testLucene
            String to = "work@yandex.ru";
            String bbUri = IexProxyCluster.blackboxUri(UID + UID_VALUE);
            cluster.blackbox().add(
                bbUri,
                new StaticHttpItem(
                    IexProxyCluster.blackboxResponse(UID_VALUE, "a@b")),
                new StaticHttpItem(
                    IexProxyCluster.blackboxResponse(UID_VALUE, to)));
            HttpPost post = new HttpPost(
                HTTP_LOCALHOST + cluster.iexproxy().port() + NOTIFY
                + "&update_cache=no_facts");
            post.setEntity(
                new FileEntity(
                    new File(getClass().getResource(POST_PARAMS).toURI()),
                    ContentType.APPLICATION_JSON));
            String fsUri = FILTER_SEARCH + UID_VALUE + '*';
            FileEntity entity = new FileEntity(
                new File(getClass().getResource(STORE_PARAMS).toURI()),
                ContentType.APPLICATION_JSON);
            // Put to filterSearch emulator two entities, in such case if
            // /facts will go to filterSearch from
            // /facts-extract and will not get Not Implemented Error
            cluster.filterSearch().add(
                fsUri,
                new StaticHttpItem(HttpStatus.SC_OK, entity),
                new StaticHttpItem(HttpStatus.SC_OK, entity));
            FileEntity entityFromCoke = new FileEntity(
                new File(getClass().
                    getResource("response_from_coke.json").toURI()),
                ContentType.APPLICATION_JSON);
            cluster.cokemulatorIexlib().add(
                "/process?*",
                new StaticHttpItem(HttpStatus.SC_OK, entityFromCoke));
            cluster.producerAsyncClient().add(
                "/modify*",
                new ProxyHandler(cluster.testLucene().indexerPort()));
            cluster.producerAsyncClient().add(
                "/add*",
                new ProxyHandler(cluster.testLucene().indexerPort())
            );
            cluster.iexproxy().start();
            try (CloseableHttpResponse response = client.execute(post)) {
                logger.info("/notify response = " + response);
            }
            String checkUri = "/search?prefix=1130000013896717&"
                 + "service=iex&get=fact_name,"
                 + "fact_data,fact_mid&text=fact_mid:(159314836818238392)";
            String stringToCompare = TestSearchBackend.prepareResult(
                 "\"fact_name\": \"no_facts\", \"fact_data\": null,"
                 + "\"fact_mid\": \"159314836818238392\"");
            logger.info("expecting that lucene will contain:\n"
                + stringToCompare);
            cluster.testLucene().checkSearch(checkUri, stringToCompare);
            // Part 2: process /facts request and check that result is
            // as expected
            bbUri = "/blackbox/?method=userinfo&format=json&userip=127.0.0.1"
                + "&sid=2&uid=" + UID_VALUE
                + "&dbfields=hosts.db_id.2,subscription.suid.2";
            logger.info("bbUri for /facts: " + bbUri);
            String bbResponse =
                IexProxyCluster.blackboxResponse(UID_VALUE, "A@b2");
            bbResponse = bbResponse.substring(0, bbResponse.length() - FOUR)
                + ",\"hosts.db_id.2\":\"mdb300\"" + "}}]}";
            logger.info("bbResponse for /facts: " + bbResponse);
            cluster.blackbox().add(
                bbUri,
                new StaticHttpItem(bbResponse),
                new StaticHttpItem(bbResponse));
            HttpGet get = new HttpGet(
                HTTP_LOCALHOST + cluster.iexproxy().port() + FACTS
                    + UID + UID_VALUE
                    + "&mid=159314836818238392&cokedump&extract");
            cluster.producer().add(
                   "/_status*",
                    "localhost");
            try (CloseableHttpResponse response = client.execute(get)) {
                String entityString =
                    CharsetUtils.toString(response.getEntity());
                logger.info("/facts returned:\n" + entityString);
                Path path = Paths.get(
                    getClass().getResource(
                       //"empty_facts.json").toURI());
                       "facts_empty_array.json").toURI());
                String expected = java.nio.file.Files.readString(path);
                logger.info("expected /facts result:\n" + expected);
                String result = new JsonChecker(expected)
                    .check(entityString);
                if (result != null) {
                    Assert.fail(result);
                }
            }
        }
    }

    @Test
    public void testFactsIndexingAndRetreiving() throws Exception {
        try (IexProxyCluster cluster =
            new IexProxyCluster(this, null, CONFIG_EXTRA, true);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            // Part 1: process /notify request
            // and check the facts are stored in testLucene
            String to = "work@yandex.ru";
            String bbUri = IexProxyCluster.blackboxUri(UID + UID_VALUE);
            cluster.blackbox().add(
                bbUri,
                new StaticHttpItem(
                    IexProxyCluster.blackboxResponse(UID_VALUE, "a@b")),
                new StaticHttpItem(
                    IexProxyCluster.blackboxResponse(UID_VALUE, to)));
            HttpPost post = new HttpPost(
                HTTP_LOCALHOST + cluster.iexproxy().port() + NOTIFY_TTEOT
                + "&reindex");
            cluster.producerAsyncClient().add(
                "/modify*",
                new ExpectingHeaderHttpItem(
                    new ProxyHandler(cluster.testLucene().indexerPort()),
                    YandexHeaders.X_INDEX_OPERATION_QUEUE,
                    cluster.iexproxy().xIndexOperationQueueNameBacklog())
            );
            cluster.producerAsyncClient().add(
                "/add*",
                new ExpectingHeaderHttpItem(
                    new ProxyHandler(cluster.testLucene().indexerPort()),
                    YandexHeaders.X_INDEX_OPERATION_QUEUE,
                    cluster.iexproxy().xIndexOperationQueueNameBacklog())
            );
            post.setEntity(
                new FileEntity(
                    new File(getClass().getResource(POST_PARAMS).toURI()),
                    ContentType.APPLICATION_JSON));
            String fsUri = FILTER_SEARCH + UID_VALUE + '*';
            FileEntity entity = new FileEntity(
                new File(getClass().getResource(STORE_PARAMS).toURI()),
                ContentType.APPLICATION_JSON);
            // Put to filterSearch emulator only one entity, in such case if
            // /facts will go to filterSearch it will get Not Implemented Error
            cluster.filterSearch().add(
                fsUri,
                new StaticHttpItem(HttpStatus.SC_OK, entity));
            FileEntity entityFromCoke = new FileEntity(
                new File(getClass().
                    getResource("response_from_coke.json").toURI()),
                ContentType.APPLICATION_JSON);
            cluster.cokemulatorIexlib().add(
                "/process?*",
                new StaticHttpItem(HttpStatus.SC_OK, entityFromCoke));
            cluster.iexproxy().start();
            try (CloseableHttpResponse response = client.execute(post)) {
                logger.info("/notify response = " + response);
            }
            String checkUri = "/search?prefix=1130000013896717&"
                 + "service=iex&get=fact_name,"
                 + "fact_data,fact_mid&text=fact_mid:(159314836818238392)";
            String stringToCompare = TestSearchBackend.prepareResult(
                 "\"fact_name\": \"pdf\","
                     + "\"fact_data\":"
                     + "\"{\\\"hid_pdf\\\":\\\"1.2\\\"}\","
                     + "\"fact_mid\": \"159314836818238392\"",
                 "\"fact_name\": \"contentline\","
                     + "\"fact_data\": \"{\\\"weight\\\":1,\\\"text\\\""
                     + ":\\\"Hello! I'm just test message\\\"}\","
                     + "\"fact_mid\": \"159314836818238392\"",
                 "\"fact_name\": \"opa-opa\","
                     + "\"fact_data\":"
                     + "\"{\\\"t\\\":2,\\\"tr\\\":\\\"Bla-bla\\\"}\","
                     + "\"fact_mid\": \"159314836818238392\"",
                 "\"fact_name\": \"events\", \"fact_data\": null,"
                     + "\"fact_mid\": \"159314836818238392\"");
            logger.info("expecting that lucene will contain:\n"
                + stringToCompare);
            cluster.testLucene().checkSearch(checkUri, stringToCompare);
            // Part 2: process /facts request and check that result is
            // as expected
            bbUri = "/blackbox/?method=userinfo&format=json&userip=127.0.0.1"
                + "&sid=2&uid=" + UID_VALUE
                + "&dbfields=hosts.db_id.2,subscription.suid.2";
            logger.info("bbUri for /facts: " + bbUri);
            String bbResponse =
                IexProxyCluster.blackboxResponse(UID_VALUE, "A@b2");
            bbResponse = bbResponse.substring(0, bbResponse.length() - FOUR)
                + ",\"hosts.db_id.2\":\"mdb300\"" + "}}]}";
            logger.info("bbResponse for /facts: " + bbResponse);
            cluster.blackbox().add(
                bbUri,
                new StaticHttpItem(bbResponse),
                new StaticHttpItem(bbResponse));
            HttpGet get = new HttpGet(
                HTTP_LOCALHOST + cluster.iexproxy().port() + FACTS
                    + UID + UID_VALUE
                    + "&mid=159314836818238392&cokedump&extract");
            cluster.producer().add(
                "/_status*",
                "localhost");
            try (CloseableHttpResponse response = client.execute(get)) {
                String entityString =
                    CharsetUtils.toString(response.getEntity());
                logger.info("/facts returned:\n" + entityString);
                Path path = Paths.get(
                    getClass().getResource(
                       "facts_response_to_compare_cokedump.json").toURI());
                String expected = java.nio.file.Files.readString(path);
                logger.info("expected /facts result:\n" + expected);
                String result = new JsonChecker(expected)
                    .check(entityString);
                if (result != null) {
                    Assert.fail(result);
                }
            }
        }
    }

    @Test
    public void testSkipFactsIndexingAndRetreiving() throws Exception {
        try (IexProxyCluster cluster =
            new IexProxyCluster(
                this,
                null,
                CONFIG_EXTRA
                + "extrasettings.no-cache-facts = _nocache",
                true);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            // Part 1: process /notify request
            // and check the facts are stored in testLucene
            String to = "work@yandex.ru";
            String bbUri = IexProxyCluster.blackboxUri(UID + UID_VALUE);
            cluster.blackbox().add(
                bbUri,
                new StaticHttpItem(
                    IexProxyCluster.blackboxResponse(UID_VALUE, "a@b")),
                new StaticHttpItem(
                    IexProxyCluster.blackboxResponse(UID_VALUE, to)));
            HttpPost post = new HttpPost(
                HTTP_LOCALHOST + cluster.iexproxy().port() + NOTIFY_TTEOT
                + "&reindex");
            cluster.producerAsyncClient().add(
                "/modify*",
                new ExpectingHeaderHttpItem(
                    new ProxyHandler(cluster.testLucene().indexerPort()),
                    YandexHeaders.X_INDEX_OPERATION_QUEUE,
                    cluster.iexproxy().xIndexOperationQueueNameBacklog())
            );
            cluster.producerAsyncClient().add(
                "/add*",
                new ExpectingHeaderHttpItem(
                    new ProxyHandler(cluster.testLucene().indexerPort()),
                    YandexHeaders.X_INDEX_OPERATION_QUEUE,
                    cluster.iexproxy().xIndexOperationQueueNameBacklog())
            );
            post.setEntity(
                new FileEntity(
                    new File(getClass().getResource(POST_PARAMS).toURI()),
                    ContentType.APPLICATION_JSON));
            String fsUri = FILTER_SEARCH + UID_VALUE + '*';
            FileEntity entity = new FileEntity(
                new File(getClass().getResource(STORE_PARAMS).toURI()),
                ContentType.APPLICATION_JSON);
            // Put to filterSearch emulator only one entity, in such case if
            // /facts will go to filterSearch it will get Not Implemented Error
            cluster.filterSearch().add(
                fsUri,
                new StaticHttpItem(HttpStatus.SC_OK, entity));
            FileEntity entityFromCoke = new FileEntity(
                new File(getClass().
                    getResource("response_from_coke_nocache.json").toURI()),
                ContentType.APPLICATION_JSON);
            cluster.cokemulatorIexlib().add(
                "/process?*",
                new StaticHttpItem(HttpStatus.SC_OK, entityFromCoke));
            cluster.iexproxy().start();
            try (CloseableHttpResponse response = client.execute(post)) {
                logger.info("/notify response = " + response);
            }
            String checkUri = "/search?prefix=1130000013896717&"
                 + "service=iex&get=fact_name,"
                 + "fact_data,fact_mid&text=fact_mid:(159314836818238392)";
            String stringToCompare = TestSearchBackend.prepareResult(
                 "\"fact_name\": \"pdf\","
                     + "\"fact_data\":"
                     + "\"{\\\"hid_pdf\\\":\\\"1.2\\\"}\","
                     + "\"fact_mid\": \"159314836818238392\"",
                 "\"fact_name\": \"contentline\","
                     + "\"fact_data\": \"{\\\"weight\\\":1,\\\"text\\\""
                     + ":\\\"Hello! I'm just test message\\\"}\","
                     + "\"fact_mid\": \"159314836818238392\"",
                 "\"fact_name\": \"opa-opa\","
                     + "\"fact_data\":"
                     + "\"{\\\"t\\\":2,\\\"tr\\\":\\\"Bla-bla\\\"}\","
                     + "\"fact_mid\": \"159314836818238392\"",
                 "\"fact_name\": \"events\", \"fact_data\": null,"
                     + "\"fact_mid\": \"159314836818238392\"");
            logger.info("expecting that lucene will contain:\n"
                + stringToCompare);
            cluster.testLucene().checkSearch(checkUri, stringToCompare);
            // Part 2: process /facts request and check that result is
            // as expected
            bbUri = "/blackbox/?method=userinfo&format=json&userip=127.0.0.1"
                + "&sid=2&uid=" + UID_VALUE
                + "&dbfields=hosts.db_id.2,subscription.suid.2";
            logger.info("bbUri for /facts: " + bbUri);
            String bbResponse =
                IexProxyCluster.blackboxResponse(UID_VALUE, "A@b2");
            bbResponse = bbResponse.substring(0, bbResponse.length() - FOUR)
                + ",\"hosts.db_id.2\":\"mdb300\"" + "}}]}";
            logger.info("bbResponse for /facts: " + bbResponse);
            cluster.blackbox().add(
                bbUri,
                new StaticHttpItem(bbResponse),
                new StaticHttpItem(bbResponse));
            HttpGet get = new HttpGet(
                HTTP_LOCALHOST + cluster.iexproxy().port() + FACTS
                    + UID + UID_VALUE
                    + "&mid=159314836818238392&cokedump&extract");
            cluster.producer().add(
                "/_status*",
                "localhost");
            try (CloseableHttpResponse response = client.execute(get)) {
                String entityString =
                    CharsetUtils.toString(response.getEntity());
                logger.info("/facts returned:\n" + entityString);
                Path path = Paths.get(
                    getClass().getResource(
                       "facts_response_to_compare_cokedump.json")
                            .toURI());
                String expected = java.nio.file.Files.readString(path);
                logger.info("expected /facts result:\n" + expected);
                String result = new JsonChecker(expected)
                    .check(entityString);
                if (result != null) {
                    Assert.fail(result);
                }
            }
        }
    }

    //CSOFF: MultipleStringLiterals
    @Test
    public void testFactsCachingFacts() throws Exception {
        try (IexProxyCluster cluster =
            new IexProxyCluster(this, null, CONFIG_EXTRA, true);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            // Part 1: no facts are in testLucene. Process /facts request
            // and check the facts are now stored in testLucene
            String to = "work@yandex.ru";
            String bbUri =
                "/blackbox/?method=userinfo&format=json&userip=127.0.0.1"
                + "&sid=2&uid=" + UID_VALUE
                + "&dbfields=hosts.db_id.2,subscription.suid.2";
            logger.info("bbUri for /facts: " + bbUri);
            String bbResponse =
                IexProxyCluster.blackboxResponse(UID_VALUE, "A@b2");
            bbResponse = bbResponse.substring(0, bbResponse.length() - FOUR)
                + ",\"hosts.db_id.2\":\"mdb300\"" + "}}]}";
            logger.info("bbResponse for /facts: " + bbResponse);
            cluster.blackbox().add(
                bbUri,
                new StaticHttpItem(bbResponse),
                new StaticHttpItem(bbResponse));
            HttpGet get = new HttpGet(
                HTTP_LOCALHOST + cluster.iexproxy().port() + FACTS
                    + UID + UID_VALUE
                    + "&mid=159314836818238392&cokedump&extract=true");
            String fsUri = FILTER_SEARCH + UID_VALUE + '*';
            FileEntity entity = new FileEntity(
                new File(getClass().getResource(STORE_PARAMS).toURI()),
                ContentType.APPLICATION_JSON);
            // Put to filterSearch emulator only two entities
            // (second for XivaFactsUpdateCallback), in such case if
            // /facts will go to filterSearch it will get Not Implemented Error
            cluster.filterSearch().add(
                fsUri,
                new StaticHttpItem(HttpStatus.SC_OK, entity),
                new StaticHttpItem(HttpStatus.SC_OK, entity),
                new StaticHttpItem(EMPTY_ENVELOPES));
            FileEntity entityFromCoke = new FileEntity(
                new File(getClass().
                    getResource("response_from_coke.json").toURI()),
                ContentType.APPLICATION_JSON);
            cluster.cokemulatorIexlib().add(
                "/process?*",
                new StaticHttpItem(HttpStatus.SC_OK, entityFromCoke));
            cluster.producer().add(
                   "/_status*",
                    "localhost");
            cluster.iexproxy().start();
            cluster.producerAsyncClient().add(
                "/modify*",
                new ExpectingHeaderHttpItem(
                    new ProxyHandler(cluster.testLucene().indexerPort()),
                    YandexHeaders.X_INDEX_OPERATION_QUEUE,
                    cluster.iexproxy().xIndexOperationQueueNameFacts()),
                new ExpectingHeaderHttpItem(
                    new ProxyHandler(cluster.testLucene().indexerPort()),
                    YandexHeaders.X_INDEX_OPERATION_QUEUE,
                    cluster.iexproxy().xIndexOperationQueueNameFacts())
            );
            try (CloseableHttpResponse response = client.execute(get)) {
                String entityString =
                    CharsetUtils.toString(response.getEntity());
                logger.info("/facts returned:\n" + entityString);
            }
            String checkUri = "/search?prefix=1130000013896717&"
                 + "service=iex&get=fact_name,"
                 + "fact_data,fact_mid&text=fact_mid:(159314836818238392)";
            String stringToCompare = TestSearchBackend.prepareResult(
                 "\"fact_name\": \"pdf\","
                     + "\"fact_data\":"
                     + "\"{\\\"hid_pdf\\\":\\\"1.2\\\"}\","
                     + "\"fact_mid\": \"159314836818238392\"",
                 "\"fact_name\": \"contentline\","
                     + "\"fact_data\": \"{\\\"weight\\\":1,\\\"text\\\""
                     + ":\\\"Hello! I'm just test message\\\"}\","
                     + "\"fact_mid\": \"159314836818238392\"",
                 "\"fact_name\": \"opa-opa\","
                     + "\"fact_data\":"
                     + "\"{\\\"t\\\":2,\\\"tr\\\":\\\"Bla-bla\\\"}\","
                     + "\"fact_mid\": \"159314836818238392\"",
                 "\"fact_name\": \"events\", \"fact_data\": null,"
                     + "\"fact_mid\": \"159314836818238392\"");
            logger.info("FactsCachingFacts:"
                + "expecting that lucene will contain:\n"
                + stringToCompare);
            cluster.testLucene().checkSearch(checkUri, stringToCompare);
            // Part 2: process /facts request and check that result is
            // as expected
            cluster.blackbox().add(
                bbUri,
                new StaticHttpItem(bbResponse),
                new StaticHttpItem(bbResponse));
            HttpGet getSecond = new HttpGet(
                HTTP_LOCALHOST + cluster.iexproxy().port() + FACTS
                    + UID + UID_VALUE
                    + "&mid=159314836818238392&mid=1&cokedump&extract");
            cluster.producer().add(
                "/_status*",
                "localhost");
            try (CloseableHttpResponse response = client.execute(getSecond)) {
                String entityString =
                    CharsetUtils.toString(response.getEntity());
                logger.info("/facts returned:\n" + entityString);
                Path path = Paths.get(
                    getClass().getResource(
                       "facts_response_to_compare_cokedump.json").toURI());
                String expected = java.nio.file.Files.readString(path);
                logger.info("expected /facts result:\n" + expected);
                String result = new JsonChecker(expected)
                    .check(entityString);
                if (result != null) {
                    Assert.fail(result);
                }
                try (CloseableHttpClient statClient =
                        Configs.createDefaultClient();
                    CloseableHttpResponse statResponse = statClient.execute(
                        new HttpGet(cluster.iexproxy().host() + "/stat")))
                {
                    JsonList root = TypesafeValueContentHandler.parse(
                        CharsetUtils.toString(statResponse.
                            getEntity())).asList();
                    Assert.assertTrue(
                        findMetric(
                            root,
                            "less-0.5-facts-cache-hit-ratio_ammm")
                                .asString().equals("1")
                        || findMetric(
                            root,
                            "less-0.5-facts-cache-hit-ratio_ammm")
                                .asString().equals("0"));
                    Assert.assertTrue(
                        findMetric(
                            root,
                            "less-0.75-facts-cache-hit-ratio_ammm")
                                .asString().equals("1")
                        || findMetric(
                            root,
                            "less-0.75-facts-cache-hit-ratio_ammm")
                                .asString().equals("0"));
                }
            }
        }
    }

    @Test
    public void testFactsDoNotUpdateCache() throws Exception {
        try (IexProxyCluster cluster =
            new IexProxyCluster(this, null, CONFIG_EXTRA, true);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            String to = "work@yandex.ru";
            String bbUri =
                "/blackbox/?method=userinfo&format=json&userip=127.0.0.1"
                + "&sid=2&uid=" + UID_VALUE
                + "&dbfields=hosts.db_id.2,subscription.suid.2";
            logger.info("bbUri for /facts: " + bbUri);
            String bbResponse =
                IexProxyCluster.blackboxResponse(UID_VALUE, "A@b2");
            bbResponse = bbResponse.substring(0, bbResponse.length() - FOUR)
                + ",\"hosts.db_id.2\":\"mdb300\"" + "}}]}";
            logger.info("bbResponse for /facts: " + bbResponse);
            cluster.blackbox().add(
                bbUri,
                new StaticHttpItem(bbResponse));
            // this is to check data in lucene in case of updating requests
            cluster.producerAsyncClient().add(
                "/modify*",
                new ExpectingHeaderHttpItem(
                    new ProxyHandler(cluster.testLucene().indexerPort()),
                    YandexHeaders.X_INDEX_OPERATION_QUEUE,
                    cluster.iexproxy().xIndexOperationQueueNameFacts())
            );
            HttpGet get = new HttpGet(
                HTTP_LOCALHOST + cluster.iexproxy().port() + FACTS
                    + UID + UID_VALUE
                    + "&mid=159314836818238392&mid=1&cokedump"
                    + "&extract&update_cache=false");
            String fsUri = FILTER_SEARCH + UID_VALUE + '*';
            FileEntity entity = new FileEntity(
                new File(getClass().getResource(STORE_PARAMS).toURI()),
                ContentType.APPLICATION_JSON);
            cluster.filterSearch().add(
                fsUri,
                new StaticHttpItem(HttpStatus.SC_OK, entity));
            FileEntity entityFromCoke = new FileEntity(
                new File(getClass().
                    getResource("response_from_coke.json").toURI()),
                ContentType.APPLICATION_JSON);
            cluster.cokemulatorIexlib().add(
                "/process?*",
                new StaticHttpItem(HttpStatus.SC_OK, entityFromCoke));
            cluster.producer().add(
                   "/_status*",
                    "localhost");
            cluster.iexproxy().start();
            String entityString = "";
            try (CloseableHttpResponse response = client.execute(get)) {
                entityString = CharsetUtils.toString(response.getEntity());
                logger.info("/facts returned:\n" + entityString);
            }
            String checkUri = "/search?prefix=1130000013896717&"
                 + "service=iex&get=fact_name,"
                 + "fact_data,fact_mid&text=fact_mid:(159314836818238392)";
            String stringToCompare = TestSearchBackend.prepareResult();
            logger.info("FactsDoNotUpdateCache:"
                + "expecting that lucene will contain:\n"
                + stringToCompare);
            cluster.testLucene().checkSearch(checkUri, stringToCompare);
        }
    }

    @Test
    public void testFactsWithoutCacheWithExtractParam() throws Exception {
        try (IexProxyCluster cluster =
            new IexProxyCluster(this, null, CONFIG_EXTRA, true);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            String to = "work2@yandex.ru";
            String bbUri = IexProxyCluster.blackboxUri(UID + UID_VALUE);
            cluster.blackbox().add(
                bbUri,
                new StaticHttpItem(
                    IexProxyCluster.blackboxResponse(UID_VALUE, "a2@b")),
                new StaticHttpItem(
                    IexProxyCluster.blackboxResponse(UID_VALUE, to)));
            bbUri = "/blackbox/?method=userinfo&format=json&userip=127.0.0.1"
                + "&sid=2&uid=" + UID_VALUE
                + "&dbfields=hosts.db_id.2,subscription.suid.2";
            logger.info("bbUri for /facts: " + bbUri);
            String bbResponse =
                IexProxyCluster.blackboxResponse(UID_VALUE, "A@b2");
            bbResponse = bbResponse.substring(0, bbResponse.length() - FOUR)
                + ",\"hosts.db_id.2\":\"mdb300\"" + "}}]}";
            logger.info("bbResponse for /facts: " + bbResponse);
            cluster.blackbox().add(
                bbUri,
                new StaticHttpItem(bbResponse),
                new StaticHttpItem(bbResponse));
            String fsUri = FILTER_SEARCH + UID_VALUE + '*';
            FileEntity entity = new FileEntity(
                new File(getClass().getResource(STORE_PARAMS).toURI()),
                ContentType.APPLICATION_JSON);
            // Put to filterSearch emulator only one entity, in such case if
            // /facts will go to filterSearch it will get Not Implemented Error
            cluster.filterSearch().add(
                fsUri,
                new StaticHttpItem(HttpStatus.SC_OK, entity));
            FileEntity entityFromCoke = new FileEntity(
                new File(getClass().
                    getResource("response_from_coke.json").toURI()),
                ContentType.APPLICATION_JSON);
            cluster.cokemulatorIexlib().add(
                "/process?*",
                new StaticHttpItem(HttpStatus.SC_OK, entityFromCoke));
            cluster.producerAsyncClient().add(
                "/modify*",
                new ExpectingHeaderHttpItem(
                    new ProxyHandler(cluster.testLucene().indexerPort()),
                    YandexHeaders.X_INDEX_OPERATION_QUEUE,
                    cluster.iexproxy().xIndexOperationQueueNameFacts())
            );
            cluster.iexproxy().start();
            HttpGet get = new HttpGet(
                HTTP_LOCALHOST + cluster.iexproxy().port() + FACTS
                    + UID + UID_VALUE
                    + "&mid=159314836818238392&cokedump&extract");
            cluster.producer().add(
                   "/_status*",
                    "localhost");
            try (CloseableHttpResponse response = client.execute(get)) {
                String entityString =
                    CharsetUtils.toString(response.getEntity());
                logger.info("/facts returned:\n" + entityString);
                Path path = Paths.get(
                    getClass().getResource(
                        //currenlty /facts don't go to FS and coke, just return
                        //response from lucene
                        //"empty_facts.json").toURI());
                       "facts_response_to_compare_cokedump.json").toURI());
                String expected = java.nio.file.Files.readString(path);
                logger.info("expected /facts result:\n" + expected);
                String result = new JsonChecker(expected)
                    .check(entityString);
                if (result != null) {
                    Assert.fail(result);
                }
            }
        }
    }

    @Test
    public void testFactsWithoutCache() throws Exception {
        try (IexProxyCluster cluster =
            new IexProxyCluster(this, null, CONFIG_EXTRA, true);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            String to = "work2@yandex.ru";
            String bbUri = IexProxyCluster.blackboxUri(UID + UID_VALUE);
            cluster.blackbox().add(
                bbUri,
                new StaticHttpItem(
                    IexProxyCluster.blackboxResponse(UID_VALUE, "a2@b")),
                new StaticHttpItem(
                    IexProxyCluster.blackboxResponse(UID_VALUE, to)));
            bbUri = "/blackbox/?method=userinfo&format=json&userip=127.0.0.1"
                + "&sid=2&uid=" + UID_VALUE
                + "&dbfields=hosts.db_id.2,subscription.suid.2";
            logger.info("bbUri for /facts: " + bbUri);
            String bbResponse =
                IexProxyCluster.blackboxResponse(UID_VALUE, "A@b2");
            bbResponse = bbResponse.substring(0, bbResponse.length() - FOUR)
                + ",\"hosts.db_id.2\":\"mdb300\"" + "}}]}";
            logger.info("bbResponse for /facts: " + bbResponse);
            cluster.blackbox().add(
                bbUri,
                new StaticHttpItem(bbResponse),
                new StaticHttpItem(bbResponse));
            String fsUri = FILTER_SEARCH + UID_VALUE + '*';
            FileEntity entity = new FileEntity(
                new File(getClass().getResource(STORE_PARAMS).toURI()),
                ContentType.APPLICATION_JSON);
            // Put to filterSearch emulator only one entity, in such case if
            // /facts will go to filterSearch it will get Not Implemented Error
            cluster.filterSearch().add(
                fsUri,
                new StaticHttpItem(HttpStatus.SC_OK, entity));
            FileEntity entityFromCoke = new FileEntity(
                new File(getClass().
                    getResource("response_from_coke.json").toURI()),
                ContentType.APPLICATION_JSON);
            cluster.cokemulatorIexlib().add(
                "/process?*",
                new StaticHttpItem(HttpStatus.SC_OK, entityFromCoke));
            cluster.producerAsyncClient().add(
                "/modify*",
                new ExpectingHeaderHttpItem(
                    new ProxyHandler(cluster.testLucene().indexerPort()),
                    YandexHeaders.X_INDEX_OPERATION_QUEUE,
                    cluster.iexproxy().xIndexOperationQueueNameFacts())
            );
            cluster.iexproxy().start();
            String uri = HTTP_LOCALHOST + cluster.iexproxy().port() + FACTS
                    + UID + UID_VALUE
                    + "&mid=159314836818238392&mid=1&mid=2&mid=3&cokedump";
            HttpGet get = new HttpGet(uri);
            cluster.producer().add(
                   "/_status*",
                    "localhost");
            String newUri = "/facts?&uid=1130000013896717&cokedump=true"
                + "&mid=159314836818238392&mdb=pg&extract=true";
            cluster.producerAsyncClient().add(
                    newUri,
                    new ExpectingHttpItem((StringChecker) null));
            try (CloseableHttpResponse response = client.execute(get)) {
                String entityString =
                    CharsetUtils.toString(response.getEntity());
                logger.info("/facts returned:\n" + entityString);
                Path path = Paths.get(
                    getClass().getResource(
                       "empty_facts.json").toURI());
                String expected = java.nio.file.Files.readString(path);
                logger.info("expected /facts result:\n" + expected);
                String result = new JsonChecker(expected)
                    .check(entityString);
                if (result != null) {
                    Assert.fail(result);
                }
            }
        }
    }

    @Test
    public void testFactsNoSearchServers() throws Exception {
        try (IexProxyCluster cluster =
            new IexProxyCluster(this, null, CONFIG_EXTRA, true);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            String to = "work2@yandex.ru";
            String bbUri = IexProxyCluster.blackboxUri(UID + UID_VALUE);
            cluster.blackbox().add(
                bbUri,
                new StaticHttpItem(
                    IexProxyCluster.blackboxResponse(UID_VALUE, "a2@b")),
                new StaticHttpItem(
                    IexProxyCluster.blackboxResponse(UID_VALUE, to)));
            bbUri = "/blackbox/?method=userinfo&format=json&userip=127.0.0.1"
                + "&sid=2&uid=" + UID_VALUE
                + "&dbfields=hosts.db_id.2,subscription.suid.2";
            logger.info("bbUri for /facts: " + bbUri);
            String bbResponse =
                IexProxyCluster.blackboxResponse(UID_VALUE, "A@b2");
            bbResponse = bbResponse.substring(0, bbResponse.length() - FOUR)
                + ",\"hosts.db_id.2\":\"mdb300\"" + "}}]}";
            logger.info("bbResponse for /facts: " + bbResponse);
            cluster.blackbox().add(
                bbUri,
                new StaticHttpItem(bbResponse),
                new StaticHttpItem(bbResponse));
            String fsUri = FILTER_SEARCH + UID_VALUE + '*';
            FileEntity entity = new FileEntity(
                new File(getClass().getResource(STORE_PARAMS).toURI()),
                ContentType.APPLICATION_JSON);
            // Put to filterSearch emulator only one entity, in such case if
            // /facts will go to filterSearch it will get Not Implemented Error
            cluster.filterSearch().add(
                fsUri,
                new StaticHttpItem(HttpStatus.SC_OK, entity));
            FileEntity entityFromCoke = new FileEntity(
                new File(getClass().
                    getResource("response_from_coke.json").toURI()),
                ContentType.APPLICATION_JSON);
            cluster.cokemulatorIexlib().add(
                "/process?*",
                new StaticHttpItem(HttpStatus.SC_OK, entityFromCoke));
            cluster.producerAsyncClient().add(
                "/modify*",
                new ExpectingHeaderHttpItem(
                    new ProxyHandler(cluster.testLucene().indexerPort()),
                    YandexHeaders.X_INDEX_OPERATION_QUEUE,
                    cluster.iexproxy().xIndexOperationQueueNameFacts())
            );
            cluster.iexproxy().start();
            HttpGet get = new HttpGet(
                HTTP_LOCALHOST + cluster.iexproxy().port() + FACTS
                    + UID + UID_VALUE
                    + "&mid=159314836818238392&cokedump&extract");
            cluster.producer().add(
                   "/_status*",
                    "localhost");
            try (CloseableHttpResponse response = client.execute(get)) {
                String entityString =
                    CharsetUtils.toString(response.getEntity());
                logger.info("/facts returned:\n" + entityString);
                Path path = Paths.get(
                    getClass().getResource(
                        //currenlty /facts don't go to FS and coke, just return
                        //response from lucene
                        //"empty_facts.json").toURI());
                       "facts_response_to_compare_cokedump.json").toURI());
                String expected = java.nio.file.Files.readString(path);
                logger.info("expected /facts result:\n" + expected);
                String result = new JsonChecker(expected)
                    .check(entityString);
                if (result != null) {
                    Assert.fail(result);
                }
            }
        }
    }

    @Test
    public void testFactsUpdating() throws Exception {
        try (IexProxyCluster cluster =
            new IexProxyCluster(
                this,
                null,
                TIMESTAMP_CONFIGS_PKPASS + CONFIG_EXTRA,
                true);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            // Part 1: process /notify request
            // and check the facts are stored in testLucene
            String to = "work@yandex.ru";
            String bbUri = IexProxyCluster.blackboxUri(UID + UID_VALUE);
            cluster.blackbox().add(
                bbUri,
                new StaticHttpItem(
                    IexProxyCluster.blackboxResponse(UID_VALUE, "a@b")),
                new StaticHttpItem(
                    IexProxyCluster.blackboxResponse(UID_VALUE, to)));
            HttpPost post = new HttpPost(
                HTTP_LOCALHOST + cluster.iexproxy().port() + NOTIFY);
            post.setEntity(
                new FileEntity(
                    new File(getClass().getResource(POST_PARAMS).toURI()),
                    ContentType.APPLICATION_JSON));
            String fsUri = FILTER_SEARCH + UID_VALUE + '*';
            FileEntity entity = new FileEntity(
                new File(getClass().getResource(STORE_PARAMS).toURI()),
                ContentType.APPLICATION_JSON);
            // Put to filterSearch emulator only one entity, in such case if
            // /facts will go to filterSearch it will get Not Implemented Error
            cluster.filterSearch().add(
                fsUri,
                new StaticHttpItem(HttpStatus.SC_OK, entity),
                new StaticHttpItem(HttpStatus.SC_OK, entity));
            FileEntity entityFromCoke = new FileEntity(
                new File(getClass().
                    getResource("response_from_coke.json").toURI()),
                ContentType.APPLICATION_JSON);
            cluster.producerAsyncClient().add(
                "/modify*",
                new ProxyHandler(cluster.testLucene().indexerPort()));
            cluster.producerAsyncClient().add(
                "/add*",
                new ProxyHandler(cluster.testLucene().indexerPort()));
            cluster.cokemulatorIexlib().add(
                "/process?*",
                new StaticHttpItem(HttpStatus.SC_OK, entityFromCoke));
            cluster.iexproxy().start();
            try (CloseableHttpResponse response = client.execute(post)) {
                logger.info("/notify response = " + response);
            }
            String checkUri = "/search?prefix=1130000013896717&"
                 + "service=iex&get=fact_name,"
                 + "fact_data,fact_mid&text=fact_mid:(159314836818238392)";
            String stringToCompare = TestSearchBackend.prepareResult(
                 "\"fact_name\": \"pdf\","
                     + "\"fact_data\":"
                     + "\"{\\\"hid_pdf\\\":\\\"1.2\\\"}\","
                     + "\"fact_mid\": \"159314836818238392\"",
                 "\"fact_name\": \"contentline\","
                     + "\"fact_data\": \"{\\\"weight\\\":1,\\\"text\\\""
                     + ":\\\"Hello! I'm just test message\\\"}\","
                     + "\"fact_mid\": \"159314836818238392\"",
                 "\"fact_name\": \"opa-opa\","
                     + "\"fact_data\":"
                     + "\"{\\\"t\\\":2,\\\"tr\\\":\\\"Bla-bla\\\"}\","
                     + "\"fact_mid\": \"159314836818238392\"",
                 "\"fact_name\": \"events\", \"fact_data\": null,"
                     + "\"fact_mid\": \"159314836818238392\"");
            logger.info("expecting that lucene will contain:\n"
                + stringToCompare);
            cluster.testLucene().checkSearch(checkUri, stringToCompare);
            // Part 2: process /facts request and check that result is
            // as expected
            FileEntity updatedEntityFromCoke = new FileEntity(
                new File(getClass().
                    getResource("response_from_coke_updated.json").toURI()),
                ContentType.APPLICATION_JSON);
            cluster.cokemulatorIexlib().add(
                "/process?*",
                new StaticHttpItem(HttpStatus.SC_OK, updatedEntityFromCoke));
            bbUri = "/blackbox/?method=userinfo&format=json&userip=127.0.0.1"
                + "&sid=2&uid=" + UID_VALUE
                + "&dbfields=hosts.db_id.2,subscription.suid.2";
            logger.info("bbUri for /facts: " + bbUri);
            String bbResponse =
                IexProxyCluster.blackboxResponse(UID_VALUE, "A@b2");
            bbResponse = bbResponse.substring(0, bbResponse.length() - FOUR)
                + ",\"hosts.db_id.2\":\"mdb300\"" + "}}]}";
            logger.info("bbResponse for /facts: " + bbResponse);
            cluster.blackbox().add(
                bbUri,
                new StaticHttpItem(bbResponse),
                new StaticHttpItem(bbResponse));
            HttpGet get = new HttpGet(
                HTTP_LOCALHOST + cluster.iexproxy().port() + FACTS
                    + UID + UID_VALUE
                    + "&mid=159314836818238392");
            cluster.producer().add(
                "/_status*",
                "localhost");
            try (CloseableHttpResponse response = client.execute(get)) {
                String entityString =
                    CharsetUtils.toString(response.getEntity());
                logger.info("/facts returned:\n" + entityString);
                Path path = Paths.get(
                    getClass().getResource(
                       "facts_updated_response_to_compare.json").toURI());
                String expected = java.nio.file.Files.readString(path);
                logger.info("expected /facts result:\n" + expected);
                String result = new JsonChecker(expected)
                    .check(entityString);
                if (result != null) {
                    Assert.fail(result);
                }
            }
        }
    }

    @Test
    public void testFactsUpToDate() throws Exception {
        try (IexProxyCluster cluster =
            new IexProxyCluster(this, null, CONFIG_EXTRA, true);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            // Part 1: process /notify request
            // and check the facts are stored in testLucene
            String to = "work@yandex.ru";
            String bbUri = IexProxyCluster.blackboxUri(UID + UID_VALUE);
            cluster.blackbox().add(
                bbUri,
                new StaticHttpItem(
                    IexProxyCluster.blackboxResponse(UID_VALUE, "a@b")),
                new StaticHttpItem(
                    IexProxyCluster.blackboxResponse(UID_VALUE, to)));
            cluster.producerAsyncClient().add(
                "/modify*",
                new ProxyHandler(cluster.testLucene().indexerPort())
            );
            cluster.producerAsyncClient().add(
                "/add*",
                new ProxyHandler(cluster.testLucene().indexerPort())
            );
            HttpPost post = new HttpPost(
                HTTP_LOCALHOST + cluster.iexproxy().port() + NOTIFY);
            post.setEntity(
                new FileEntity(
                    new File(getClass().getResource(POST_PARAMS).toURI()),
                    ContentType.APPLICATION_JSON));
            String fsUri = FILTER_SEARCH + UID_VALUE + '*';
            FileEntity entity = new FileEntity(
                new File(getClass().getResource(STORE_PARAMS).toURI()),
                ContentType.APPLICATION_JSON);
            // Put to filterSearch emulator only one entity, in such case if
            // /facts will go to filterSearch it will get Not Implemented Error
            cluster.filterSearch().add(
                fsUri,
                new StaticHttpItem(HttpStatus.SC_OK, entity),
                new StaticHttpItem(HttpStatus.SC_OK, entity));
            FileEntity entityFromCoke = new FileEntity(
                new File(getClass().
                    getResource("response_from_coke.json").toURI()),
                ContentType.APPLICATION_JSON);
            cluster.cokemulatorIexlib().add(
                "/process?*",
                new StaticHttpItem(HttpStatus.SC_OK, entityFromCoke));
            cluster.iexproxy().start();
            try (CloseableHttpResponse response = client.execute(post)) {
                logger.info("/notify response = " + response);
            }
            String checkUri = "/search?prefix=1130000013896717&"
                 + "service=iex&get=fact_name,"
                 + "fact_data,fact_mid&text=fact_mid:(159314836818238392)";
            String stringToCompare = TestSearchBackend.prepareResult(
                 "\"fact_name\": \"pdf\","
                     + "\"fact_data\":"
                     + "\"{\\\"hid_pdf\\\":\\\"1.2\\\"}\","
                     + "\"fact_mid\": \"159314836818238392\"",
                 "\"fact_name\": \"contentline\","
                     + "\"fact_data\": \"{\\\"weight\\\":1,\\\"text\\\""
                     + ":\\\"Hello! I'm just test message\\\"}\","
                     + "\"fact_mid\": \"159314836818238392\"",
                 "\"fact_name\": \"opa-opa\","
                     + "\"fact_data\":"
                     + "\"{\\\"t\\\":2,\\\"tr\\\":\\\"Bla-bla\\\"}\","
                     + "\"fact_mid\": \"159314836818238392\"",
                 "\"fact_name\": \"events\", \"fact_data\": null,"
                     + "\"fact_mid\": \"159314836818238392\"");
            logger.info("expecting that lucene will contain:\n"
                + stringToCompare);
            cluster.testLucene().checkSearch(checkUri, stringToCompare);
            // Part 2: process /facts request and check that result is
            // as expected
            FileEntity updatedEntityFromCoke = new FileEntity(
                new File(getClass().
                    getResource("response_from_coke_updated.json").toURI()),
                ContentType.APPLICATION_JSON);
            cluster.cokemulatorIexlib().add(
                "/process?*",
                new StaticHttpItem(HttpStatus.SC_OK, updatedEntityFromCoke));
            bbUri = "/blackbox/?method=userinfo&format=json&userip=127.0.0.1"
                + "&sid=2&uid=" + UID_VALUE
                + "&dbfields=hosts.db_id.2,subscription.suid.2";
            logger.info("bbUri for /facts: " + bbUri);
            String bbResponse =
                IexProxyCluster.blackboxResponse(UID_VALUE, "A@b2");
            bbResponse = bbResponse.substring(0, bbResponse.length() - FOUR)
                + ",\"hosts.db_id.2\":\"mdb300\"" + "}}]}";
            logger.info("bbResponse for /facts: " + bbResponse);
            cluster.blackbox().add(
                bbUri,
                new StaticHttpItem(bbResponse),
                new StaticHttpItem(bbResponse));
            HttpGet get = new HttpGet(
                HTTP_LOCALHOST + cluster.iexproxy().port() + FACTS
                    + UID + UID_VALUE
                    + "&mid=159314836818238392&cokedump&extract");
            cluster.producer().add(
                "/_status*",
                "localhost");
            try (CloseableHttpResponse response = client.execute(get)) {
                String entityString =
                    CharsetUtils.toString(response.getEntity());
                logger.info("/facts returned:\n" + entityString);
                Path path = Paths.get(
                    getClass().getResource(
                       "facts_response_to_compare_cokedump.json").toURI());
                String expected = java.nio.file.Files.readString(path);
                logger.info("expected /facts result:\n" + expected);
                String result = new JsonChecker(expected)
                    .check(entityString);
                if (result != null) {
                    Assert.fail(result);
                }
            }
        }
    }

    @Test
    public void testIgnoreCacheParam() throws Exception {
        try (IexProxyCluster cluster =
            new IexProxyCluster(this, null, "", configPostProc, false, false);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            String to = "work@yandex.ru";
            String bbUri = IexProxyCluster.blackboxUri(UID + UID_VALUE);
            cluster.blackbox().add(
                bbUri,
                new StaticHttpItem(
                    IexProxyCluster.blackboxResponse(UID_VALUE, "a@b")),
                new StaticHttpItem(
                    IexProxyCluster.blackboxResponse(UID_VALUE, to)));
            String fsUri = FILTER_SEARCH + UID_VALUE + '*';
            FileEntity entity = new FileEntity(
                new File(getClass().getResource(STORE_PARAMS).toURI()),
                ContentType.APPLICATION_JSON);
            cluster.filterSearch().add(
                fsUri,
                new StaticHttpResource(
                    new StaticHttpItem(HttpStatus.SC_OK, entity)));
            cluster.producerAsyncClient().add(
                "/modify*",
                new ExpectingHeaderHttpItem(
                    new ProxyHandler(cluster.testLucene().indexerPort()),
                    YandexHeaders.X_INDEX_OPERATION_QUEUE,
                    cluster.iexproxy().xIndexOperationQueueNameFacts())
            );
            FileEntity entityFromCoke = new FileEntity(
                new File(getClass().
                    getResource("response_from_coke.json").toURI()),
                    ContentType.APPLICATION_JSON);
            cluster.cokemulatorIexlib().add(
                "/process?*",
                new StaticHttpItem(HttpStatus.SC_OK, entityFromCoke));
            cluster.iexproxy().start();
            bbUri = "/blackbox/?method=userinfo&format=json&userip=127.0.0.1"
                + "&sid=2&uid=" + UID_VALUE
                + "&dbfields=hosts.db_id.2,subscription.suid.2";
            logger.info("bbUri for /facts: " + bbUri);
            String bbResponse =
                IexProxyCluster.blackboxResponse(UID_VALUE, "A@b2");
            bbResponse = bbResponse.substring(0, bbResponse.length() - FOUR)
                + ",\"hosts.db_id.2\":\"mdb300\"" + "}}]}";
            logger.info("bbResponse for /facts: " + bbResponse);
            cluster.blackbox().add(
                bbUri,
                new StaticHttpItem(bbResponse),
                new StaticHttpItem(bbResponse));
            HttpGet get = new HttpGet(
                HTTP_LOCALHOST + cluster.iexproxy().port() + FACTS
                + UID + UID_VALUE
                + "&mid=159314836818238392&cokedump&extract");
            cluster.producer().add(
                "/_status*",
                "localhost");
            try (CloseableHttpResponse response = client.execute(get)) {
                String entityString =
                     CharsetUtils.toString(response.getEntity());
                logger.info("/facts returned:\n" + entityString);
                Path path = Paths.get(
                     getClass().getResource(
                        "empty_facts.json").toURI());
                String expected = java.nio.file.Files.readString(path);
                logger.info("expected /facts result:\n" + expected);
                String result = new JsonChecker(expected)
                    .check(entityString);
                if (result != null) {
                    Assert.fail(result);
                }
                Assert.assertEquals(2, cluster.filterSearch().
                    accessCount(fsUri));
            }
        }
    }

    //CSOFF: MultipleStringLiterals
    @Test
    public void testNotifyReindexUpdateCache() throws Exception {
        try (IexProxyCluster cluster =
            new IexProxyCluster(this, null, CONFIG_EXTRA, true);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            // Part 1: no facts are in testLucene. Process /facts request
            // and check the facts are now stored in testLucene
            String to = "work@yandex.ru";
            String bbUri = IexProxyCluster.blackboxUri(UID + UID_VALUE);
            cluster.blackbox().add(
                bbUri,
                new StaticHttpItem(
                    IexProxyCluster.blackboxResponse(UID_VALUE, "a@b")),
                new StaticHttpItem(
                    IexProxyCluster.blackboxResponse(UID_VALUE, to)));
            logger.info("bbUri for /facts: " + bbUri);
            String bbResponse =
                IexProxyCluster.blackboxResponse(UID_VALUE, "A@b2");
            bbResponse = bbResponse.substring(0, bbResponse.length() - FOUR)
                + ",\"hosts.db_id.2\":\"mdb300\"" + "}}]}";
            logger.info("bbResponse for /facts: " + bbResponse);
            cluster.blackbox().add(
                bbUri,
                new StaticHttpItem(bbResponse),
                new StaticHttpItem(bbResponse));
            HttpPost post = new HttpPost(
                HTTP_LOCALHOST + cluster.iexproxy().port() + NOTIFY
                    + "&reindex&update_cache=events");
            post.setEntity(
                new FileEntity(
                    new File(getClass().getResource(POST_PARAMS).toURI()),
                    ContentType.APPLICATION_JSON));
            String fsUri = FILTER_SEARCH + UID_VALUE + '*';
            FileEntity entity = new FileEntity(
                new File(getClass().getResource(STORE_PARAMS).toURI()),
                ContentType.APPLICATION_JSON);
            // Put to filterSearch emulator only one entity, in such case if
            // /facts will go to filterSearch it will get Not Implemented Error
            cluster.filterSearch().add(
                fsUri,
                new StaticHttpItem(HttpStatus.SC_OK, entity),
                new StaticHttpItem(HttpStatus.SC_OK, entity),
                new StaticHttpItem(EMPTY_ENVELOPES));
            FileEntity entityFromCoke = new FileEntity(
                new File(getClass().
                    getResource("response_from_coke.json").toURI()),
                ContentType.APPLICATION_JSON);
            cluster.cokemulatorIexlib().add(
                "/process?*",
                new StaticHttpItem(HttpStatus.SC_OK, entityFromCoke));
            cluster.producer().add(
                   "/_status*",
                    "localhost");
            cluster.iexproxy().start();
            cluster.producerAsyncClient().register(
                new Pattern<>("/modify", false),
                new ProxyHandler(cluster.testLucene().indexerPort()),
                RequestHandlerMapper.POST);
            cluster.producerAsyncClient().register(
                new Pattern<>("/add", false),
                new ProxyHandler(cluster.testLucene().indexerPort()),
                RequestHandlerMapper.POST);
            CloseableHttpResponse response = client.execute(post);
            String checkUri = "/search?prefix=1130000013896717&sort=fact_name"
                 + "&asc=True&service=iex&get=fact_name,"
                 + "fact_data,fact_mid&text=fact_mid:(159314836818238392)";
            String stringToCompare = TestSearchBackend.prepareResult(
                 "\"fact_name\": \"contentline\","
                     + "\"fact_data\": \"{\\\"weight\\\":1,\\\"text\\\""
                     + ":\\\"Hello! I'm just test message\\\"}\","
                     + "\"fact_mid\": \"159314836818238392\"",
                 "\"fact_name\": \"events\", \"fact_data\": null,"
                     + "\"fact_mid\": \"159314836818238392\"",
                 "\"fact_name\": \"opa-opa\","
                     + "\"fact_data\":"
                     + "\"{\\\"t\\\":2,\\\"tr\\\":\\\"Bla-bla\\\"}\","
                     + "\"fact_mid\": \"159314836818238392\"",
                 "\"fact_name\": \"pdf\","
                     + "\"fact_data\":"
                     + "\"{\\\"hid_pdf\\\":\\\"1.2\\\"}\","
                     + "\"fact_mid\": \"159314836818238392\"");
            logger.info("FactsCachingFacts:"
                + "expecting that lucene will contain:\n"
                + stringToCompare);
            cluster.testLucene().checkSearch(checkUri, stringToCompare);
            // Part 2: process /facts request and check that result is
            // as expected
            cluster.blackbox().add(
                bbUri,
                new StaticHttpItem(bbResponse),
                new StaticHttpItem(bbResponse));
            HttpPost postUpdateCache = new HttpPost(
                HTTP_LOCALHOST + cluster.iexproxy().port() + NOTIFY
                    + "&reindex&update_cache=opa-opa,"
                    + "pkpass&update_cache=events");
            postUpdateCache.setEntity(
                new FileEntity(
                    new File(getClass().getResource(POST_PARAMS).toURI()),
                    ContentType.APPLICATION_JSON));
            entityFromCoke = new FileEntity(
                new File(getClass().
                    getResource("response_from_coke_updated.json").toURI()),
                ContentType.APPLICATION_JSON);
            cluster.cokemulatorIexlib().add(
                "/process?*",
                new StaticHttpItem(HttpStatus.SC_OK, entityFromCoke));
            cluster.producer().add(
                "/_status*",
                "localhost");
            try (CloseableHttpResponse responseSecond =
                    client.execute(postUpdateCache))
            {
                String entityString =
                    CharsetUtils.toString(responseSecond.getEntity());
                logger.info("response:\n" + entityString);
                stringToCompare = TestSearchBackend.prepareResult(
                    "\"fact_name\": \"contentline\","
                        + "\"fact_data\": \"{\\\"weight\\\":1,\\\"text\\\""
                        + ":\\\"Hello! I'm just test message\\\"}\","
                        + "\"fact_mid\": \"159314836818238392\"",
                    "\"fact_name\": \"events\", \"fact_data\": null,"
                        + "\"fact_mid\": \"159314836818238392\"",
                    "\"fact_name\": \"opa-opa\","
                        + "\"fact_data\":"
                        + "\"{\\\"t\\\":2,\\\"tr\\\":\\\"Bla-bla-second\\\"}\","
                        + "\"fact_mid\": \"159314836818238392\"",
                    "\"fact_name\": \"pdf\","
                        + "\"fact_data\":"
                        + "\"{\\\"hid_pdf\\\":\\\"1.2\\\"}\","
                        + "\"fact_mid\": \"159314836818238392\"");
                logger.info("FactsCachingFacts:"
                    + "expecting that after second request lucene"
                    + "will contain:\n"
                    + stringToCompare);
                cluster.testLucene().checkSearch(checkUri, stringToCompare);
            }
        }
    }

    //CSOFF: MultipleStringLiterals
    @Test
    public void testNotifyReindexUpdateCacheTwoMids() throws Exception {
        try (IexProxyCluster cluster =
            new IexProxyCluster(this, null, CONFIG_EXTRA, true);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            // no facts are in testLucene. Process /notify request
            // with 2 mids - one with 200 response from coke
            // and another - with 415 - checking that info for first mid will
            // be stored in testLucene
            String to = "work@yandex.ru";
            String bbUri = IexProxyCluster.blackboxUri(UID + UID_VALUE);
            cluster.blackbox().add(
                bbUri,
                new StaticHttpItem(
                    IexProxyCluster.blackboxResponse(UID_VALUE, "a@b")),
                new StaticHttpItem(
                    IexProxyCluster.blackboxResponse(UID_VALUE, to)));
            logger.info("bbUri for: " + bbUri);
            String bbResponse =
                IexProxyCluster.blackboxResponse(UID_VALUE, "A@b2");
            bbResponse = bbResponse.substring(0, bbResponse.length() - FOUR)
                + ",\"hosts.db_id.2\":\"mdb300\"" + "}}]}";
            logger.info("bbResponse for: " + bbResponse);
            cluster.blackbox().add(
                bbUri,
                new StaticHttpItem(bbResponse),
                new StaticHttpItem(bbResponse));
            HttpPost post = new HttpPost(
                HTTP_LOCALHOST + cluster.iexproxy().port() + NOTIFY
                    + "&reindex&update_cache=events");
            post.setEntity(
                new FileEntity(
                    new File(getClass().getResource(POST_PARAMS_TWO_MIDS)
                        .toURI()),
                    ContentType.APPLICATION_JSON));
            String fsUri = FILTER_SEARCH + UID_VALUE + '*';
            FileEntity entity = new FileEntity(
                new File(getClass().getResource(STORE_PARAMS_TWO_MIDS).toURI()),
                ContentType.APPLICATION_JSON);
            cluster.filterSearch().add(
                fsUri,
                new StaticHttpItem(HttpStatus.SC_OK, entity),
                new StaticHttpItem(HttpStatus.SC_OK, entity),
                new StaticHttpItem(EMPTY_ENVELOPES));
            cluster.axis().add(
                "/v1*",
                new StaticHttpItem(HttpStatus.SC_OK),
                new StaticHttpItem(HttpStatus.SC_OK));
            FileEntity entityFromCoke = new FileEntity(
                new File(getClass().
                    getResource("response_from_coke.json").toURI()),
                ContentType.APPLICATION_JSON);
            cluster.cokemulatorIexlib().add(
                "/process?stid=1.632123143*",
                new StaticHttpItem(HttpStatus.SC_OK, entityFromCoke));
            cluster.cokemulatorIexlib().add(
                "/process?stid=1.123*",
                HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE);
            cluster.producer().add(
                "/_status*",
                "localhost");
            cluster.iexproxy().start();
            cluster.producerAsyncClient().register(
                new Pattern<>("/modify", false),
                new ProxyHandler(cluster.testLucene().indexerPort()),
                RequestHandlerMapper.POST);
            cluster.producerAsyncClient().register(
                new Pattern<>("/add", false),
                new ProxyHandler(cluster.testLucene().indexerPort()),
                RequestHandlerMapper.POST);
            CloseableHttpResponse response = client.execute(post);
            String checkUri = "/search?prefix=1130000013896717&sort=fact_name"
                 + "&asc=True&service=iex&get=fact_name,"
                 + "fact_data,fact_mid&text=fact_mid:(159314836818238392)";
            String stringToCompare = TestSearchBackend.prepareResult(
                 "\"fact_name\": \"contentline\","
                     + "\"fact_data\": \"{\\\"weight\\\":1,\\\"text\\\""
                     + ":\\\"Hello! I'm just test message\\\"}\","
                     + "\"fact_mid\": \"159314836818238392\"",
                 "\"fact_name\": \"events\", \"fact_data\": null,"
                     + "\"fact_mid\": \"159314836818238392\"",
                 "\"fact_name\": \"opa-opa\","
                     + "\"fact_data\":"
                     + "\"{\\\"t\\\":2,\\\"tr\\\":\\\"Bla-bla\\\"}\","
                     + "\"fact_mid\": \"159314836818238392\"",
                 "\"fact_name\": \"pdf\","
                     + "\"fact_data\":"
                     + "\"{\\\"hid_pdf\\\":\\\"1.2\\\"}\","
                     + "\"fact_mid\": \"159314836818238392\"");
            logger.info("FactsCachingFacts:"
                + "expecting that lucene will contain:\n"
                + stringToCompare);
            cluster.testLucene().checkSearch(checkUri, stringToCompare);
        }
    }

    @Test
    public void testNoMids() throws Exception {
        try (IexProxyCluster cluster =
            new IexProxyCluster(this, null, CONFIG_EXTRA, true);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            HttpGet get = new HttpGet(
                HTTP_LOCALHOST + cluster.iexproxy().port() + FACTS
                    + UID + UID_VALUE);
            cluster.iexproxy().start();
            HttpAssert.assertStatusCode(HttpStatus.SC_BAD_REQUEST, client, get);
        }
    }

    @Test
    public void testFactNamesCgiParam() throws Exception {
        try (IexProxyCluster cluster =
            new IexProxyCluster(this, null, CONFIG_EXTRA, true);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            // Part 1: process /notify request
            String to = "work@yandex.ru";
            String bbUri = IexProxyCluster.blackboxUri(UID + UID_VALUE);
            cluster.blackbox().add(
                bbUri,
                new StaticHttpItem(
                    IexProxyCluster.blackboxResponse(UID_VALUE, "a@b")),
                new StaticHttpItem(
                    IexProxyCluster.blackboxResponse(UID_VALUE, to)));
            cluster.producerAsyncClient().add(
                "/modify*",
                new ExpectingHeaderHttpItem(
                    new ProxyHandler(cluster.testLucene().indexerPort()),
                    YandexHeaders.X_INDEX_OPERATION_QUEUE,
                    cluster.iexproxy().xIndexOperationQueueNameBacklog())
            );
            cluster.producerAsyncClient().add(
                "/add*",
                new ExpectingHeaderHttpItem(
                    new ProxyHandler(cluster.testLucene().indexerPort()),
                    YandexHeaders.X_INDEX_OPERATION_QUEUE,
                    cluster.iexproxy().xIndexOperationQueueNameBacklog())
            );
            HttpPost post = new HttpPost(
                HTTP_LOCALHOST + cluster.iexproxy().port() + NOTIFY_TTEOT
                + "&reindex");
            post.setEntity(
                new FileEntity(
                    new File(getClass().getResource(POST_PARAMS).toURI()),
                    ContentType.APPLICATION_JSON));
            String fsUri = FILTER_SEARCH + UID_VALUE + '*';
            FileEntity entity = new FileEntity(
                new File(getClass().getResource(STORE_PARAMS).toURI()),
                ContentType.APPLICATION_JSON);
            // Put to filterSearch emulator only one entity, in such case if
            // /facts will go to filterSearch it will get Not Implemented Error
            cluster.filterSearch().add(
                fsUri,
                new StaticHttpItem(HttpStatus.SC_OK, entity));
            FileEntity entityFromCoke = new FileEntity(
                new File(getClass().
                    getResource("response_from_coke.json").toURI()),
                ContentType.APPLICATION_JSON);
            cluster.cokemulatorIexlib().add(
                "/process?*",
                new StaticHttpItem(HttpStatus.SC_OK, entityFromCoke));
            cluster.iexproxy().start();
            try (CloseableHttpResponse response = client.execute(post)) {
                logger.info("/notify response = " + response);
            }
            // Part 2: process /facts request and check that result is
            // as expected
            bbUri = "/blackbox/?method=userinfo&format=json&userip=127.0.0.1"
                    + "&sid=2&uid=" + UID_VALUE
                    + "&dbfields=hosts.db_id.2,subscription.suid.2";
            logger.info("bbUri for /facts: " + bbUri);
            String bbResponse =
                IexProxyCluster.blackboxResponse(UID_VALUE, "A@b2");
            bbResponse = bbResponse.substring(0, bbResponse.length() - FOUR)
                         + ",\"hosts.db_id.2\":\"mdb300\"" + "}}]}";
            logger.info("bbResponse for /facts: " + bbResponse);
            cluster.blackbox().add(
                bbUri,
                new StaticHttpItem(bbResponse),
                new StaticHttpItem(bbResponse));
            HttpGet get = new HttpGet(
                HTTP_LOCALHOST + cluster.iexproxy().port() + FACTS
                + UID + UID_VALUE
                + "&mid=159314836818238392&cokedump&extract"
                + "&fact_names=pdf,contentline");
            cluster.producer().add(
                "/_status*",
                "localhost");
            try (CloseableHttpResponse response = client.execute(get)) {
                String entityString =
                    CharsetUtils.toString(response.getEntity());
                logger.info("/facts returned:\n" + entityString);
                Path path = Paths.get(
                    getClass().getResource(
                        "facts_response_fact_names_to_compare.json").toURI());
                String expected = java.nio.file.Files.readString(path);
                logger.info("expected /facts result:\n" + expected);
                String result = new JsonChecker(expected).check(entityString);
                if (result != null) {
                    Assert.fail(result);
                }
            }
        }
    }

    private JsonObject findMetric(final JsonList root, final String name)
        throws Exception
    {
        for (JsonObject pair: root) {
            if (name.equals(pair.get(0).asString())) {
                return pair.get(1);
            }
        }
        Assert.fail("Failed to find metric '" + name + "' in " + root);
        return null;
    }
}
