package ru.yandex.iex.proxy;

import java.io.File;
import java.util.LinkedHashMap;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Test;

import ru.yandex.blackbox.BlackboxUserinfo;
import ru.yandex.collection.Pattern;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.test.ProxyHandler;
import ru.yandex.http.test.StaticHttpItem;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.http.util.request.RequestHandlerMapper;
import ru.yandex.search.prefix.LongPrefix;
import ru.yandex.test.util.TestBase;

public class IexProxyEshopTest extends TestBase {
    //CSOFF: MultipleStringLiterals
    private static final String ADDA = "/add*";
    private static final String AXIS_URI =
        "/v1/facts/store_batch?client_id=extractors";
    private static final String ESHOP = "eshop";
    private static final String ESHOP_BK = "eshop_bk";
    //private static final String EVENTS = "events";
    //private static final String EVENT1 = "event1";
    //private static final String MAIL_HANDLER =
    //    "/tikaite?json-type=dollar&stid=";
    //private static final String HELLO = "hello";
    private static final String UID_PARAM = "&uid=";
    private static final String PG = "pg";
    private static final String HTTP_LOCALHOST = "http://localhost:";
    private static final String NOTIFY = "/notify?mdb=";
    private static final String MAIL = "mail";
    //private static final String FS_URI = "/filter_search*";
    private static final String CORP_FS_URI = "/filter_search*";
    //private static final String CONTENTLINE = "contentline";
    //private static final String TICKET = "ticket";
    //private static final String TICKET1 = "ticket1";
    //private static final String UNDER_TICKET = "_ticket";
    //private static final String STORE_PARAMS = "storefs.json";
    private static final String STORE_PARAMS_TYPE = "storefs_type.json";
    private static final String STORE_PARAMS_ESHOP_BK =
                                             "storefs_eshop_bk_axis.json";
    //private static final String MID_123 = "123";
    //private static final String STID_123 = "1.2.3";
    private static final int FOUR = 4;
    private static final long UID_VALUE = BlackboxUserinfo.CORP_UID_BEGIN;
    private static final long UID_VAL_ESHOP = 1130000013896717L;

    @Test
    public void testEshopIsNewOrder() throws Exception {
        String path = "./cacheupdate/eshop/";
        String eshopMid = "777";
        String additionalMid = "11";
        String storefsSecond = "storefs_update.json";
        String postParams = "post_params.json";
        String configExtra = "";
        try (IexProxyCluster cluster =
             new IexProxyCluster(this, null, configExtra, true);
             CloseableHttpClient client = Configs.createDefaultClient())
        {
            String bbUri =
                IexProxyCluster.blackboxUri(UID_PARAM + UID_VAL_ESHOP);
            System.err.println("bbUri for /facts: " + bbUri);
            String bbResponse =
                IexProxyCluster.blackboxResponse(UID_VAL_ESHOP, "A@b2");
            bbResponse = bbResponse.substring(0, bbResponse.length() - FOUR)
                + ",\"hosts.db_id.2\":\"mdb300\"" + "}}]}";
            System.err.println("bbResponse for /facts: " + bbResponse);
            cluster.blackbox().add(
                bbUri,
                new StaticHttpResource(
                    HttpStatus.SC_OK,
                    new StringEntity(bbResponse)));

            cluster.producer().add(
                "/_status*",
                new StaticHttpResource(
                    HttpStatus.SC_OK,
                    new StringEntity("[{\"localhost\":-1}]")));

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

            LinkedHashMap<String, Object> eshopEntity = new LinkedHashMap<>();
            eshopEntity.put("entity", ESHOP);
            cluster.axis().add(
                AXIS_URI,
                new AxisVerifier(UID_VAL_ESHOP, MAIL)
                    .add(ESHOP, eshopEntity));

            IexProxyTestMocks.cokemulatorIexlibMock(
                cluster,
                path + "response_from_coke.json");
            IexProxyTestMocks.filterSearchMock(
                cluster,
                path + STORE_PARAMS_TYPE,
                UID_VAL_ESHOP,
                eshopMid);
            IexProxyTestMocks.filterSearchMock(
                cluster,
                path + storefsSecond,
                UID_VAL_ESHOP,
                additionalMid);
            cluster.iexproxy().start();

            HttpPost post = new HttpPost(
                HTTP_LOCALHOST + cluster.iexproxy().port() + NOTIFY + PG);
            post.setEntity(
                new FileEntity(
                    new File(getClass().getResource(path + postParams)
                        .toURI()),
                    ContentType.APPLICATION_JSON));
            //CloseableHttpResponse response = client.execute(post);
            String postParamsSecond = "post_params_update.json";
            IexProxyTestMocks.cokemulatorIexlibMock(
                cluster,
                path + "response_from_coke_update.json");
            HttpPost postCancellation = new HttpPost(
                HTTP_LOCALHOST + cluster.iexproxy().port() + NOTIFY + PG);
            postCancellation.setEntity(
                new FileEntity(
                    new File(getClass().getResource(
                        path + postParamsSecond).toURI()),
                    ContentType.APPLICATION_JSON));
            cluster.axis().add(
                AXIS_URI,
                new AxisVerifier(UID_VAL_ESHOP, MAIL)
                    .add(ESHOP, eshopEntity));

            HttpAssert.assertStatusCode(
                    HttpStatus.SC_OK,
                    client,
                    postCancellation);
        }
    }

    @Test
    public void testAxisStoreEshopBk() throws Exception {
        final String cfg = "extrasettings.axis-facts = eshop, eshop_bk";
        try (IexProxyCluster cluster =
                new IexProxyCluster(this, null, cfg, true);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.iexproxy().start();
            String mid = "100507";
            String suid = "9007";
            HttpPost post = new HttpPost(
                HTTP_LOCALHOST + cluster.iexproxy().port() + NOTIFY + PG);
            String pgPost = IexProxyTestMocks.pgNotifyPost(UID_VALUE, mid);
            post.setEntity(
                new StringEntity(pgPost, ContentType.APPLICATION_JSON));
            String blackboxUri = IexProxyCluster.blackboxUri(
                UID_PARAM + UID_VALUE);
            cluster.corpBlackbox().add(
                blackboxUri,
                IexProxyCluster.blackboxResponse(UID_VALUE, "a@bc.d", suid));
            FileEntity entity = new FileEntity(
                new File(getClass().getResource(STORE_PARAMS_ESHOP_BK).toURI()),
                ContentType.APPLICATION_JSON);
            cluster.corpFilterSearch().add(
                CORP_FS_URI,
                new StaticHttpItem(HttpStatus.SC_OK, entity));
            cluster.producerAsyncClient().add(
                ADDA,
                new ProxyHandler(cluster.testLucene().indexerPort())
            );

            FileEntity cokeJson = new FileEntity(
                new File(getClass().getResource("coke_eshop_order.json").
                    toURI()), ContentType.APPLICATION_JSON);

            cluster.cokemulatorIexlib().add(
                "/process?stid=1.6321*",
                new StaticHttpItem(HttpStatus.SC_OK, cokeJson));

            FileEntity marketEntityEmptyModels = new FileEntity(
                new File(getClass().
                    getResource("market/market_answer_empty_models.json")
                        .toURI()),
                ContentType.APPLICATION_JSON);
            cluster.market().add(
                "/v2.1/models/match?&name=*",
                new StaticHttpItem(
                    HttpStatus.SC_OK,
                    marketEntityEmptyModels));

            FileEntity bkEntity = new FileEntity(
                new File(getClass().getResource(
                        "bk/bk_answer.json").
                    toURI()), ContentType.APPLICATION_JSON);

            cluster.bk().add(
                "/",
                new StaticHttpItem(HttpStatus.SC_OK, bkEntity));

            cluster.producer().add(
                "/_status*",
                new StaticHttpResource(
                    HttpStatus.SC_OK,
                    new StringEntity("[{\"localhost\":-1}]")));
            String doc =
                "\"url\": \"user_gbl_" + UID_VALUE + "\","
                + "\"yuids\": \"3984446211505941116\n2489070261462964455\"";
            cluster.testLucene().add(
                new LongPrefix(UID_VALUE), doc);

            LinkedHashMap<String, Object> eshopBk = new LinkedHashMap<>();
            eshopBk.put("entity", ESHOP_BK);

            cluster.axis().add(
                AXIS_URI,
                new AxisVerifier(UID_VALUE, MAIL)
                    .add(ESHOP_BK, eshopBk));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);
        }
    }
}
