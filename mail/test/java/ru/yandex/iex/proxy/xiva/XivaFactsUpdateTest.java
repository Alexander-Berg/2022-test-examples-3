package ru.yandex.iex.proxy.xiva;

import java.io.File;
import java.io.IOException;

import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.collection.Pattern;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.ProxyHandler;
import ru.yandex.http.test.StaticHttpItem;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.http.util.BadRequestException;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.http.util.YandexHeaders;
import ru.yandex.http.util.request.RequestHandlerMapper;
import ru.yandex.iex.proxy.IexProxyCluster;
import ru.yandex.iex.proxy.OnlineHandler;
import ru.yandex.parser.uri.QueryConstructor;
import ru.yandex.search.prefix.LongPrefix;
import ru.yandex.test.util.TestBase;

public class XivaFactsUpdateTest extends TestBase {
    private static final String RESOURCES_PATH = "xiva/";

    private static final long UID_VALUE = 588355978L;
    private static final String SUID_VALUE = "9000";
    private static final String MID1 = "166070236259287377";
    private static final String MID2 = "166070236259287378";
    private static final String MID3 = "166070236259287379";
    private static final String DOMAIN = "yandex.ru";

    private static final String OK = "OK";
    private static final String UID = "uid";
    private static final String UID_PARAM = "&uid=";
    private static final String UID_PARAM_VALUE = UID_PARAM + UID_VALUE;
    private static final String MID = "mid";
    private static final String MIDS = "mids";
    private static final String MDB_PARAM = "&mdb=pg";
    private static final String HTTP_LOCALHOST = "http://localhost:";
    private static final String LOCALHOST = "localhost";
    private static final String TAKSA_HOST = "qtaksa.mail.yandex.net";
    private static final String FACTS_URI =
        "/facts?mdb=pg&cokedump" + UID_PARAM_VALUE;
    private static final String FILTER_SEARCH =
        "/filter_search?order=default&full_folders_and_labels=1"
        + UID_PARAM_VALUE + MDB_PARAM;
    private static final String TAKSA_REQUEST = "/api/list?" + UID_PARAM_VALUE
        + MDB_PARAM + "&version=hound&retry=false";
    private static final String XIVA_REQUEST = "/v2/send?&ttl=0&token=123&user="
        + UID_VALUE + "&event=iex_widgets_update";
    private static final String FACTS_RESPONSE = "/facts response: ";

    private static final long TIMEOUT = 10000;
    private static final String CONFIG_EXTRA =
        "entities.default = contentline\n";
    private static final String STOREFS1_2 = "storefs1_2.json";
    private static final String FACTS = "facts.json";
    private static final String FACTS2 = "facts2.json";
    private static final String TAKSA_RESPONSE = "taksa_response.json";
    private static final String X_REQUEST_ID = "23k34j4kw4";
    private static final String X_ENABLED_BOXES = "85232,0,82;86125,0,36";

    private IexProxyCluster cluster;

    @Before
    public void initialize() throws Exception {
        cluster = new IexProxyCluster(this, null, CONFIG_EXTRA, true);
        // blackbox
        String bbUri = IexProxyCluster.blackboxUri(UID + UID_VALUE);
        cluster.blackbox().add(bbUri, new StaticHttpItem(
            IexProxyCluster
                .blackboxResponse(UID_VALUE, DOMAIN, SUID_VALUE)));

        // online handler
        final String onlineUri = "/online?uid=" + UID_VALUE;
        cluster.onlineDB().add(
            onlineUri,
            new StaticHttpResource(new OnlineHandler(true)));

        // status response mock
        cluster.producer().add(
            "/_status*",
            new StaticHttpResource(
                HttpStatus.SC_OK,
                new StringEntity("[{\"localhost\":-1}]")));

        // axis response mock
        cluster.axis().add(
            "/v1/facts/store_batch?client_id=extractors",
            HttpStatus.SC_OK);

        // producerAsyncClient add requests handlers
        cluster.producerAsyncClient().register(
            new Pattern<>("/add", false),
            new ProxyHandler(cluster.testLucene().indexerPort()),
            RequestHandlerMapper.POST);
        cluster.producerAsyncClient().register(
            new Pattern<>("/modify", false),
            new ProxyHandler(cluster.testLucene().indexerPort()),
            RequestHandlerMapper.POST);
        cluster.producerAsyncClient().register(
            new Pattern<>("/update", false),
            new ProxyHandler(cluster.testLucene().indexerPort()),
            RequestHandlerMapper.POST);

        cluster.factsExtractor().register(
            new Pattern<>("/facts-extract", false),
            new ProxyHandler(
                new HttpHost(LOCALHOST, cluster.iexproxy().port()),
                YandexHeaders.X_REQUEST_ID,
                YandexHeaders.X_ENABLED_BOXES),
            RequestHandlerMapper.GET);
        cluster.producerAsyncClient().register(
            new Pattern<>("/facts", false),
            new ProxyHandler(cluster.iexproxy().port()),
            RequestHandlerMapper.GET);

        // cokemulator Iexlib
        FileEntity entityFromCoke = new FileEntity(
            new File(getClass().getResource("response_from_coke.json").toURI()),
            ContentType.APPLICATION_JSON);
        cluster.cokemulatorIexlib().add(
            "/process?*",
            new StaticHttpResource(HttpStatus.SC_OK, entityFromCoke));
    }

    @Test
    public void testXivaUpdateRequest() throws Exception {
        // filter search
        FileEntity entity = new FileEntity(
            new File(getClass().getResource(STOREFS1_2).toURI()),
            ContentType.APPLICATION_JSON);
        cluster.filterSearch().add(
            filterSearchUri(MID1, MID2),
            new StaticHttpResource(HttpStatus.SC_OK, entity));

        // taksa response mock
        FileEntity entityTaksa = new FileEntity(
            new File(getClass().getResource(TAKSA_RESPONSE).toURI()),
            ContentType.APPLICATION_JSON);
        cluster.taksa().add(
            TAKSA_REQUEST,
            new StaticHttpResource(HttpStatus.SC_OK, entityTaksa));

        // xiva response mock
        cluster.xiva().add(
            XIVA_REQUEST,
            new StaticHttpResource(HttpStatus.SC_OK, new StringEntity(OK)));

        cluster.iexproxy().start();
        addFactToLucene(MID3, "1530109830");

        String query = HTTP_LOCALHOST + cluster.iexproxy().port()
            + factsUri(TAKSA_HOST, MID1, MID2, MID3);
        HttpGet getFacts = new HttpGet(query);
        getFacts.addHeader(YandexHeaders.X_REQUEST_ID, X_REQUEST_ID);
        getFacts.addHeader(YandexHeaders.X_ENABLED_BOXES, X_ENABLED_BOXES);
        try (CloseableHttpClient client = Configs.createDefaultClient()) {
            try (CloseableHttpResponse response = client.execute(getFacts)) {
                String entityString =
                    CharsetUtils.toString(response.getEntity());
                System.out.println(FACTS_RESPONSE + entityString);
            }
        }
        cluster.waitProducerRequests(
            cluster.filterSearch(),
            filterSearchUri(MID1, MID2),
            2);
        cluster.waitProducerRequests(cluster.taksa(), TAKSA_REQUEST, 1);
        cluster.waitProducerRequests(cluster.xiva(), XIVA_REQUEST, 1);
        waitAndCheckFacts(FACTS, MID1, MID2, MID3);
    }

    @Test
    public void testFilterSearchEmptyResponse() throws Exception {
        // filter search
        cluster.filterSearch().add(
            filterSearchUri(MID1, MID2),
            new StaticHttpResource(
                HttpStatus.SC_OK,
                new StringEntity("{\"envelopes\":[]}")));

        // taksa response mock
        FileEntity entityTaksa = new FileEntity(
            new File(getClass().getResource(TAKSA_RESPONSE).toURI()),
            ContentType.APPLICATION_JSON);
        cluster.taksa().add(
            TAKSA_REQUEST,
            new StaticHttpResource(HttpStatus.SC_OK, entityTaksa));

        // xiva response mock
        cluster.xiva().add(
            XIVA_REQUEST,
            new StaticHttpResource(HttpStatus.SC_OK, new StringEntity(OK)));

        cluster.iexproxy().start();
        addFactToLucene(MID3, "1530109850");

        String query = HTTP_LOCALHOST + cluster.iexproxy().port()
            + factsUri(TAKSA_HOST, MID1, MID2, MID3);
        HttpGet getFacts = new HttpGet(query);
        getFacts.addHeader(YandexHeaders.X_REQUEST_ID, X_REQUEST_ID);
        getFacts.addHeader(YandexHeaders.X_ENABLED_BOXES, X_ENABLED_BOXES);
        try (CloseableHttpClient client = Configs.createDefaultClient()) {
            try (CloseableHttpResponse response = client.execute(getFacts)) {
                String entityString =
                    CharsetUtils.toString(response.getEntity());
                System.out.println(FACTS_RESPONSE + entityString);
            }
        }
        cluster.waitProducerRequests(
            cluster.filterSearch(),
            filterSearchUri(MID1, MID2),
            2);
        cluster.waitProducerRequests(cluster.taksa(), TAKSA_REQUEST, 0);
        cluster.waitProducerRequests(cluster.xiva(), XIVA_REQUEST, 0);
    }

    @Test
    public void testEmptyTaksaResponse() throws Exception {
        // filter search
        FileEntity entity = new FileEntity(
            new File(getClass().getResource(STOREFS1_2).toURI()),
            ContentType.APPLICATION_JSON);
        cluster.filterSearch().add(
            filterSearchUri(MID1, MID2),
            new StaticHttpResource(HttpStatus.SC_OK, entity));

        // taksa response mock
        FileEntity entityTaksa = new FileEntity(new File(
            getClass().getResource("taksa_empty_response.json").toURI()),
            ContentType.APPLICATION_JSON);
        cluster.taksa().add(
            TAKSA_REQUEST,
            new StaticHttpResource(HttpStatus.SC_OK, entityTaksa));

        cluster.iexproxy().start();

        String query = HTTP_LOCALHOST + cluster.iexproxy().port()
            + factsUri(null, MID1, MID2);
        HttpGet getFacts = new HttpGet(query);
        getFacts.addHeader(YandexHeaders.X_REQUEST_ID, X_REQUEST_ID);
        try (CloseableHttpClient client = Configs.createDefaultClient()) {
            try (CloseableHttpResponse response = client.execute(getFacts)) {
                String entityString =
                    CharsetUtils.toString(response.getEntity());
                System.out.println(FACTS_RESPONSE + entityString);
            }
        }
        cluster.waitProducerRequests(
            cluster.filterSearch(),
            filterSearchUri(MID1, MID2),
            2);
        cluster.waitProducerRequests(cluster.taksa(), TAKSA_REQUEST, 1);
        Assert.assertEquals(cluster.xiva().accessCount(XIVA_REQUEST), 0);
    }

    @Test
    public void testNoXivaUpdate() throws Exception {
        cluster.iexproxy().start();

        addFactToLucene(MID1, "1530109831");
        addFactToLucene(MID2, "1530109832");

        String query = HTTP_LOCALHOST + cluster.iexproxy().port()
            + factsUri(TAKSA_HOST, MID1, MID2);
        HttpGet getFacts = new HttpGet(query);
        getFacts.addHeader(YandexHeaders.X_REQUEST_ID, X_REQUEST_ID);
        try (CloseableHttpClient client = Configs.createDefaultClient()) {
            try (CloseableHttpResponse response = client.execute(getFacts)) {
                String entityString =
                    CharsetUtils.toString(response.getEntity());
                System.out.println(FACTS_RESPONSE + entityString);
            }
        }
        waitAndCheckFacts(FACTS2, MID1, MID2);
        Assert.assertEquals(cluster.taksa().accessCount(TAKSA_REQUEST), 0);
        Assert.assertEquals(cluster.xiva().accessCount(XIVA_REQUEST), 0);
    }

    @Test
    public void testTaksaTesting() throws Exception {
        // filter search
        FileEntity entity = new FileEntity(
            new File(getClass().getResource(STOREFS1_2).toURI()),
            ContentType.APPLICATION_JSON);
        cluster.filterSearch().add(
            filterSearchUri(MID1, MID2),
            new StaticHttpResource(HttpStatus.SC_OK, entity));

        // taksa response mock
        FileEntity entityTaksa = new FileEntity(
            new File(getClass().getResource(TAKSA_RESPONSE).toURI()),
            ContentType.APPLICATION_JSON);
        cluster.taksaTesting().add(
            TAKSA_REQUEST,
            new StaticHttpResource(HttpStatus.SC_OK, entityTaksa));

        // xiva response mock
        cluster.xiva().add(
            XIVA_REQUEST,
            new StaticHttpResource(HttpStatus.SC_OK, new StringEntity(OK)));

        cluster.iexproxy().start();
        addFactToLucene(MID3, "1530109835");

        String query = HTTP_LOCALHOST + cluster.iexproxy().port()
            + factsUri(LOCALHOST, MID1, MID2, MID3);
        HttpGet getFacts = new HttpGet(query);
        getFacts.addHeader(YandexHeaders.X_REQUEST_ID, X_REQUEST_ID);
        try (CloseableHttpClient client = Configs.createDefaultClient()) {
            try (CloseableHttpResponse response = client.execute(getFacts)) {
                String entityString =
                    CharsetUtils.toString(response.getEntity());
                System.out.println(FACTS_RESPONSE + entityString);
            }
        }
        cluster.waitProducerRequests(
            cluster.filterSearch(),
            filterSearchUri(MID1, MID2),
            2);
        cluster.waitProducerRequests(cluster.taksaTesting(), TAKSA_REQUEST, 1);
        cluster.waitProducerRequests(cluster.xiva(), XIVA_REQUEST, 1);
        waitAndCheckFacts(FACTS, MID1, MID2, MID3);
        Assert.assertEquals(cluster.taksa().accessCount(TAKSA_REQUEST), 0);
    }

    private void addFactToLucene(final String mid, final String receivedDate)
        throws IOException
    {
        String doc =
            "\"url\": \"facts_" + UID_VALUE + '_' + mid + "_contentline\","
            + "\"fact_uid\": " + UID_VALUE + ','
            + "\"fact_stid\": \"320.mail:" + UID_VALUE
            + ".E1012402:4131466600177705023448835500537\","
            + "\"fact_name\": \"_contentline\","
            + "\"fact_mid\": \"" + mid + '\"' + ','
            + "\"fact_received_date\": \"" + receivedDate + '\"' + ','
            + "\"fact_is_coke_solution\": \"false\","
            + "\"fact_from\": \"info@" + DOMAIN + '\"' + ','
            + "\"fact_domain\": \"" + DOMAIN + '\"';
        cluster.testLucene().add(new LongPrefix(UID_VALUE), doc);
    }

    private void checkFacts(
        final String fileName,
        final String... mids) throws Exception
    {
        QueryConstructor uri = new QueryConstructor(
            HTTP_LOCALHOST + cluster.iexproxy().port() + FACTS_URI);
        for (String mid: mids) {
            uri.append(MID, mid);
        }
        HttpGet getFacts = new HttpGet(uri.toString());
        String facts;
        try (CloseableHttpClient client = Configs.createDefaultClient()) {
            try (CloseableHttpResponse response = client.execute(getFacts)) {
                facts = CharsetUtils.toString(response.getEntity());
                System.out.println("/facts returned:\n" + facts);
            }
        }
        cluster.compareJson(RESOURCES_PATH + fileName, facts, false);
    }

    private void waitAndCheckFacts(
        final String fileName,
        final String... mids) throws Exception
    {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < TIMEOUT) {
            try {
                checkFacts(fileName, mids);
            } catch (AssertionError error) {
                continue;
            }
            return;
        }
        checkFacts(fileName, mids);
    }

    private String factsUri(final String taksaHost, final String... mids)
        throws BadRequestException
    {
        QueryConstructor uri = new QueryConstructor(FACTS_URI);
        if (taksaHost != null) {
            uri.append("taksa-host", taksaHost);
        }
        for (String mid: mids) {
            uri.append(MID, mid);
        }
        return uri.toString();
    }

    private String filterSearchUri(final String... mids)
        throws BadRequestException
    {
        QueryConstructor uri = new QueryConstructor(FILTER_SEARCH);
        for (String mid : mids) {
            uri.append(MIDS, mid);
        }
        return uri.toString();
    }
}
