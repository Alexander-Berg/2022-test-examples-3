package ru.yandex.iex.proxy.cacheupdate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.http.HttpException;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Test;

import ru.yandex.collection.Pattern;
import ru.yandex.dbfields.PgFields;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.ProxyHandler;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.http.util.request.RequestHandlerMapper;
import ru.yandex.iex.proxy.IexProxyCluster;
import ru.yandex.iex.proxy.IexProxyTestMocks;
import ru.yandex.iex.proxy.MailStorageCluster;
import ru.yandex.iex.proxy.OnlineHandler;
import ru.yandex.json.parser.JsonException;
import ru.yandex.mail.search.MailSearchDefaults;
import ru.yandex.parser.uri.QueryConstructor;
import ru.yandex.test.util.TestBase;

public class IexProxyCacheUpdateTest extends TestBase {
    private static final String BOOKING_PATH = "booking/";
    private static final String ESHOP_PATH = "eshop/";
    private static final String CALENDAR_PATH = "calendar/";
    private static final String CACHE_UPDATE_PATH = "cacheupdate/";
    private static final String UID = "&uid=";
    private static final String MID = "mid";
    private static final String HTTP_LOCALHOST = "http://localhost:";
    private static final String STORE_PARAMS = "storefs_type.json";
    private static final String STOREFS_UPDATE = "storefs_update.json";
    private static final String STOREFS_CANCEL = "storefs_cancellation.json";
    private static final String POST_PARAMS = "post_params.json";
    private static final String POST_PARAMS_UPDATE = "post_params_update.json";
    private static final String POST_PARAMS_CANCELLATION =
        "post_params_cancellation.json";
    private static final String FIRST_LUCENE = "first_lucene.json";
    private static final String RESPONSE_FROM_COKE = "response_from_coke.json";
    private static final String NOTIFY = "/notify?mdb=pg";
    //private static final String FILTER_SEARCH =
    //        "/filter_search?order=default&full_folders_and_labels=1&uid=";
    private static final String SEARCH = "/search?prefix=1130000013896717"
        + "&sort=fact_name&asc=true&service=" + IexProxyCluster.QUEUE_NAME
        + "&get=fact_name,fact_data,fact_mid,fact_is_coke_solution"
        + "&text=fact_mid:";
    private static final int FOUR = 4;
    private static final long UID_VALUE = 1130000013896717L;
    private static final String BOOKING_MID = "159314836818238392";
    private static final String CALENDAR_MID = "123";
    private static final String CALENDAR_MID2 = "124";
    private static final String CALENDAR_MID3 = "125";
    private static final String ESHOP_MID = "777";
    private static final String ADDITIONAL_MID = "11";
    private static final String CONFIG_EXTRA =
        "postprocess.default2 = events:http://localhost:"
        + IexProxyCluster.IPORT + "/events\n";

    private void init(final IexProxyCluster cluster)
        throws IOException
    {
        cluster.iexproxy().start();
        String bbUri = IexProxyCluster.blackboxUri(UID + UID_VALUE);
        logger.info("bbUri for /facts: " + bbUri);
        String bbResponse = IexProxyCluster.blackboxResponse(UID_VALUE, "A@b2");
        bbResponse = bbResponse.substring(0, bbResponse.length() - FOUR)
            + ",\"hosts.db_id.2\":\"mdb300\"" + "}}]}";
        logger.info("bbResponse for /facts: " + bbResponse);
        cluster.blackbox().add(bbUri, new StaticHttpResource(HttpStatus.SC_OK, new StringEntity(bbResponse)));

        cluster.axis().add("/v1*", new StaticHttpResource(HttpStatus.SC_OK));
        // online handler
        String onlineUri = "/online?uid=" + UID_VALUE;
        cluster.onlineDB().add(onlineUri, new StaticHttpResource(new OnlineHandler(true)));
        // enlarge response mock
        String msearchUri = "/api/async/enlarge/your?uid=" + UID_VALUE;
        cluster.msearch().add(msearchUri, new StaticHttpResource(HttpStatus.SC_OK, new StringEntity("")));
        // producer's mock
        cluster.producer().add(
            "/_status*",
            new StaticHttpResource(HttpStatus.SC_OK, new StringEntity("[{\"localhost\":-1}]")));

        cluster.producerAsyncClient().register(
            new Pattern<>("/modify", false),
            new ProxyHandler(cluster.testLucene().indexerPort()),
            RequestHandlerMapper.POST);
        cluster.producerAsyncClient().register(
            new Pattern<>("/add", false),
            new ProxyHandler(cluster.testLucene().indexerPort()),
            RequestHandlerMapper.POST);
        cluster.producerAsyncClient().register(
            new Pattern<>("/update", false),
            new ProxyHandler(cluster.testLucene().indexerPort()),
            RequestHandlerMapper.POST);
    }

    @Test
    public void testBookingCancelledUpdateCache() throws Exception {
        String path = CACHE_UPDATE_PATH + BOOKING_PATH;
        try (IexProxyCluster cluster = new IexProxyCluster(this, null, CONFIG_EXTRA, true);
             CloseableHttpClient client = Configs.createDefaultClient())
        {
            init(cluster);
            IexProxyTestMocks.filterSearchMock(
                cluster,
                path + STORE_PARAMS,
                UID_VALUE,
                BOOKING_MID);
            IexProxyTestMocks.filterSearchMock(
                cluster,
                path + STOREFS_CANCEL,
                UID_VALUE,
                ADDITIONAL_MID);
            IexProxyTestMocks.cokemulatorIexlibMock(cluster, path + RESPONSE_FROM_COKE);

            HttpPost post = new HttpPost(HTTP_LOCALHOST + cluster.iexproxy().port() + NOTIFY);
            post.setEntity(
                new FileEntity(
                    new File(IexProxyTestMocks.class.getResource(path + POST_PARAMS).toURI()),
                    ContentType.APPLICATION_JSON));
            CloseableHttpResponse response = client.execute(post);
            String checkUri = SEARCH + BOOKING_MID;
            String fileName = path + FIRST_LUCENE;
            String stringToCompare = stringToLucene(fileName);
            logger.info("Expecting that lucene will contain:\n" + stringToCompare);
            hookUpdateExcepted(checkUri, fileName, cluster);
            cluster.testLucene().checkSearch(checkUri, stringToCompare);

            IexProxyTestMocks.cokemulatorIexlibMock(cluster, path + "response_from_coke_cancelling.json");
            HttpPost postCancellation = new HttpPost(HTTP_LOCALHOST + cluster.iexproxy().port() + NOTIFY);
            postCancellation.setEntity(
                new FileEntity(
                    new File(IexProxyTestMocks.class.getResource(
                        path + POST_PARAMS_CANCELLATION).toURI()),
                    ContentType.APPLICATION_JSON));
            CloseableHttpResponse secondResponse = client.execute(postCancellation);
            fileName = path + "cancelled_lucene.json";
            String cancelledStringCompare = stringToLucene(fileName);
            logger.info("Cancelled, expecting that lucene will contain:\n" + cancelledStringCompare);
            hookUpdateExcepted(checkUri, fileName, cluster);
            cluster.testLucene().checkSearch(checkUri, cancelledStringCompare);
        }
    }

    @Test
    public void testEshopUpdateCache() throws Exception {
        String path = CACHE_UPDATE_PATH + ESHOP_PATH;
        try (IexProxyCluster cluster = new IexProxyCluster(this, null, CONFIG_EXTRA, true);
             CloseableHttpClient client = Configs.createDefaultClient())
        {
            init(cluster);
            IexProxyTestMocks.cokemulatorIexlibMock(cluster, path + RESPONSE_FROM_COKE);
            IexProxyTestMocks.filterSearchMock(
                cluster,
                path + STORE_PARAMS,
                UID_VALUE,
                ESHOP_MID);
            IexProxyTestMocks.filterSearchMock(
                cluster,
                path + STOREFS_UPDATE,
                UID_VALUE,
                ADDITIONAL_MID);

            HttpPost post = new HttpPost(HTTP_LOCALHOST + cluster.iexproxy().port() + NOTIFY);
            post.setEntity(
                new FileEntity(
                    new File(IexProxyTestMocks.class.getResource(path + POST_PARAMS).toURI()),
                    ContentType.APPLICATION_JSON));
            CloseableHttpResponse response = client.execute(post);
            String checkUri = SEARCH + ESHOP_MID;
            String fileName = path + FIRST_LUCENE;
            String stringToCompare = stringToLucene(fileName);
            logger.info("Expecting that lucene will contain first time:\n" + stringToCompare);
            hookUpdateExcepted(checkUri, fileName, cluster);
            cluster.testLucene().checkSearch(checkUri, stringToCompare);

            IexProxyTestMocks.cokemulatorIexlibMock(cluster, path + "response_from_coke_update.json");
            HttpPost postCancellation = new HttpPost(HTTP_LOCALHOST + cluster.iexproxy().port() + NOTIFY);
            postCancellation.setEntity(
                new FileEntity(
                    new File(IexProxyTestMocks.class.getResource(path + POST_PARAMS_UPDATE).toURI()),
                    ContentType.APPLICATION_JSON));
            CloseableHttpResponse secondResponse = client.execute(postCancellation);
            fileName = path + "updated_lucene.json";
            String cancelledStringCompare = stringToLucene(fileName);
            logger.info("Eshop updated, expecting that lucene will contain second_time:\n"
                + cancelledStringCompare);
            hookUpdateExcepted(checkUri, fileName, cluster);
            cluster.testLucene().checkSearch(checkUri, cancelledStringCompare);
        }
    }

    @Test
    public void testEshopUpdateCacheWithDash() throws Exception {
        String path = CACHE_UPDATE_PATH + ESHOP_PATH;
        try (IexProxyCluster cluster = new IexProxyCluster(this, null, CONFIG_EXTRA, true);
             CloseableHttpClient client = Configs.createDefaultClient())
        {
            init(cluster);
            IexProxyTestMocks.filterSearchMock(
                cluster,
                path + STORE_PARAMS,
                UID_VALUE,
                ESHOP_MID);
            IexProxyTestMocks.filterSearchMock(
                cluster,
                path + STOREFS_UPDATE,
                UID_VALUE,
                ADDITIONAL_MID);
            IexProxyTestMocks.cokemulatorIexlibMock(cluster, path + "response_from_coke_dash.json");

            HttpPost post = new HttpPost( HTTP_LOCALHOST + cluster.iexproxy().port() + NOTIFY);
            post.setEntity(
                new FileEntity(
                    new File(IexProxyTestMocks.class.getResource(path + POST_PARAMS).toURI()),
                    ContentType.APPLICATION_JSON));
            CloseableHttpResponse response = client.execute(post);
            String checkUri = SEARCH + ESHOP_MID;
            String fileName = path + "first_lucene_dash.json";
            String stringToCompare = stringToLucene(fileName);
            logger.info("Expecting that lucene will contain first time:\n" + stringToCompare);
            hookUpdateExcepted(checkUri, fileName, cluster);
            cluster.testLucene().checkSearch(checkUri, stringToCompare);

            IexProxyTestMocks.cokemulatorIexlibMock(cluster, path + "response_from_coke_update_dash.json");
            HttpPost postCancellation = new HttpPost(
                HTTP_LOCALHOST + cluster.iexproxy().port() + NOTIFY);
            postCancellation.setEntity(
                new FileEntity(
                    new File(IexProxyTestMocks.class.getResource(path + POST_PARAMS_UPDATE).toURI()),
                    ContentType.APPLICATION_JSON));
            CloseableHttpResponse secondResponse = client.execute(postCancellation);
            fileName = path + "updated_lucene_dash.json";
            String cancelledStringCompare = stringToLucene(fileName);
            logger.info("Eshop updated, expecting that lucene will contain second_time:\n"
                + cancelledStringCompare);
            hookUpdateExcepted(checkUri, fileName, cluster);
            cluster.testLucene().checkSearch(checkUri, cancelledStringCompare);
        }
    }

    @Test
    public void testBookingCancelledDoNotUpdateCache() throws Exception {
        String path = CACHE_UPDATE_PATH + BOOKING_PATH;
        try (IexProxyCluster cluster = new IexProxyCluster(this, null, CONFIG_EXTRA, true);
             CloseableHttpClient client = Configs.createDefaultClient())
        {
            init(cluster);
            IexProxyTestMocks.filterSearchMock(
                cluster,
                path + STORE_PARAMS,
                UID_VALUE,
                BOOKING_MID);
            IexProxyTestMocks.filterSearchMock(
                cluster,
                path + STOREFS_CANCEL,
                UID_VALUE,
                ADDITIONAL_MID);
            IexProxyTestMocks.cokemulatorIexlibMock(cluster, path + RESPONSE_FROM_COKE);

            HttpPost post = new HttpPost(
                HTTP_LOCALHOST + cluster.iexproxy().port() + NOTIFY);
            post.setEntity(
                new FileEntity(
                    new File(IexProxyTestMocks.class.getResource(path + POST_PARAMS).toURI()),
                    ContentType.APPLICATION_JSON));
            CloseableHttpResponse response = client.execute(post);
            String checkUri = SEARCH + BOOKING_MID;
            String fileName = path + FIRST_LUCENE;
            String stringToCompare = stringToLucene(fileName);
            logger.info("Expecting that lucene will contain: \n" + stringToCompare);
            hookUpdateExcepted(checkUri, fileName, cluster);
            cluster.testLucene().checkSearch(checkUri, stringToCompare);

            IexProxyTestMocks.cokemulatorIexlibMock(cluster, path + "response_from_coke_cancel_but_diff_reservation.json");
            HttpPost postCancellation = new HttpPost(HTTP_LOCALHOST + cluster.iexproxy().port() + NOTIFY);
            postCancellation.setEntity(
                new FileEntity(
                    new File(IexProxyTestMocks.class.getResource(path + POST_PARAMS_CANCELLATION).toURI()),
                    ContentType.APPLICATION_JSON));
            CloseableHttpResponse secondResponse = client.execute(postCancellation);
            logger.info("Not cancelled, expecting that lucene will contain the same:\n" + stringToCompare);
            cluster.testLucene().checkSearch(checkUri, stringToCompare);
        }
    }

    @Test
    public void testCalendarUpdateCache() throws Exception {
        org.junit.Assume.assumeTrue(MailStorageCluster.iexUrl() != null);
        String path = CACHE_UPDATE_PATH + CALENDAR_PATH;
        String post1 = path + "post1.json";
        String post2 = path + "post2.json";
        String post3 = path + "post3.json";
        String configExtra = CONFIG_EXTRA + "entities.default = contentline\n";
        try (IexProxyCluster cluster = new IexProxyCluster(this, null, configExtra, true, true))
        {
            init(cluster);
            IexProxyTestMocks.filterSearchMock(
                cluster,
                path + "storefs1.json",
                UID_VALUE,
                CALENDAR_MID);
            IexProxyTestMocks.filterSearchMock(
                cluster,
                path + "storefs2.json",
                UID_VALUE,
                CALENDAR_MID2);
            IexProxyTestMocks.filterSearchMock(
                cluster,
                path + "storefs3.json",
                UID_VALUE,
                CALENDAR_MID3);
            IexProxyTestMocks.filterSearchMock(
                cluster,
                path + "storefs2-3.json",
                UID_VALUE,
                CALENDAR_MID2,
                CALENDAR_MID3);
            cluster.producerAsyncClient().register(
                new Pattern<>("/notify", true),
                new ProxyHandler(cluster.iexproxy().port()),
                RequestHandlerMapper.POST);
            // attachSid response mock
            cluster.attachSid().add(
                "/attach_sid",
                new StaticHttpResource(HttpStatus.SC_OK, new StringEntity(
                    "{\"result\":[{\"sids\":[\"sid_mock\"]}]}")));
            // importEventByIcsUrl response mock
            URL eventEntityURL = IexProxyTestMocks.class.getResource(path + "import_event_by_ics_url.json");
            FileEntity eventEntity = new FileEntity(new File(eventEntityURL.toURI()), ContentType.APPLICATION_JSON);
            String getEventInfo = "/api/mail/importEventByIcsUrl*";
            cluster.calendar().add(getEventInfo, new StaticHttpResource(HttpStatus.SC_OK, eventEntity));

            sendNotifyAndCheck(cluster, post1, path + "facts1.json");
            sendNotifyAndCheck(cluster, post2, path + "facts2.json");
            sendNotifyAndCheck(cluster, post3, path + "facts3.json");
            sendNotifyAndCheck(cluster, post2, path + "facts4.json");
        }
    }

    private void sendNotifyAndCheck(
        final IexProxyCluster cluster,
        final String postParams,
        final String checkFile)
        throws Exception
    {
        QueryConstructor updateCache = new QueryConstructor(NOTIFY);
        updateCache.append("service", MailSearchDefaults.BP_CHANGE_LOG);
        updateCache.append("prefix", UID_VALUE);
        updateCache.append("update_cache", "event-ticket");
        updateCache.append(PgFields.CHANGE_TYPE, "iex-update");
        HttpPost post = new HttpPost(HTTP_LOCALHOST + cluster.iexproxy().port() + updateCache);
        post.setEntity(
            new FileEntity(
                new File(IexProxyTestMocks.class.getResource(postParams).toURI()),
                ContentType.APPLICATION_JSON));
        CloseableHttpClient client = Configs.createDefaultClient();
        CloseableHttpResponse response = client.execute(post);

        QueryConstructor checkUri = new QueryConstructor(
            HTTP_LOCALHOST + cluster.iexproxy().port()
            + "/facts?mdb=pg&uid=" + UID_VALUE);
        checkUri.append(MID, CALENDAR_MID);
        checkUri.append(MID, CALENDAR_MID2);
        checkUri.append(MID, CALENDAR_MID3);
        HttpGet getFacts = new HttpGet(checkUri.toString());
        String facts;
        try (CloseableHttpResponse factsResponse = client.execute(getFacts)) {
            facts = CharsetUtils.toString(factsResponse.getEntity());
            logger.info("/facts returned:\n" + facts);
        }
        cluster.compareJson(checkFile, facts, false);
    }

    private String stringToLucene(final String fileName) {
        try {
            Path path = Paths.get(IexProxyTestMocks.class.getResource(fileName).toURI());
            return java.nio.file.Files.readString(path);
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void hookUpdateExcepted(
        final String uri,
        final String fileName,
        final IexProxyCluster cluster)
        throws HttpException, IOException, JsonException
    {
        boolean updateExpectedJson = false;
        if (updateExpectedJson) {
            updateJsonTest(fileName, cluster.testLucene().getSearchOutput(uri));
        }
    }

    private void updateJsonTest(
        final String fileName,
        final String newExpected)
    {
        try {
            Path path = Paths.get(IexProxyTestMocks.class.getResource(fileName).toURI());
            String absolutePath = path.toAbsolutePath().toString();
            logger.info("Change answer for file " + absolutePath);
            try (FileOutputStream stream = new FileOutputStream(absolutePath)) {
                stream.write(newExpected.getBytes(StandardCharsets.UTF_8));
            }
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
