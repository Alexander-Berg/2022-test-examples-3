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

import ru.yandex.http.test.Configs;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.test.ProxyHandler;
import ru.yandex.http.test.StaticHttpItem;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.test.util.TestBase;

public class IexProxyPeopleUrlTest extends TestBase {
    //CSOFF: MultipleStringLiterals
    private static final String ADDA = "/add*";
    private static final String AXIS_URI =
        "/v1/facts/store_batch?client_id=extractors";
    //private static final String ESHOP = "eshop";
    //private static final String ESHOP_BK = "eshop_bk";
    //private static final String EDA = "eda";
    //private static final String EVENTS = "events";
    //private static final String EVENT1 = "event1";
    private static final String MAIL_HANDLER =
        "/mail/handler?json-type=dollar&ultra-fast-mode&";
    //private static final String HELLO = "hello";
    private static final String UID_PARAM = "&uid=";
    private static final String PG = "pg";
    //private static final String FILTER_SEARCH =
    //        "/filter_search?order=default&full_folders_and_labels=1&uid=";
    private static final String HTTP_LOCALHOST = "http://localhost:";
    private static final String NOTIFY = "/notify?mdb=";
    private static final String MAIL = "mail";
    private static final String FS_URI = "/filter_search*";
    //private static final String CORP_FS_URI = "/corp_filter_search*";
    //private static final String CONTENTLINE = "contentline";
    //private static final String TICKET = "ticket";
    //private static final String TICKET1 = "ticket1";
    //private static final String UNDER_TICKET = "_ticket";
    private static final String STORE_PARAMS = "people/storefs.json";
    //private static final String STORE_PARAMS_TYPE = "people/storefs_type.json";
    //private static final String STORE_PARAMS_ESHOP_BK =
    //                                         "storefs_eshop_bk_axis.json";
    private static final long UID_VALUE = 12345;
    private static final String URLS_INFO = "urls_info";

    @Test
    public void testAxisStorePeopleUrls() throws Exception {
        final String cfg = "extrasettings.axis-facts = urls_info";
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
            cluster.blackbox().add(
                blackboxUri,
                IexProxyCluster.blackboxResponse(UID_VALUE, "a@bc.d", suid));
            FileEntity entity = new FileEntity(
                new File(getClass().getResource(STORE_PARAMS).toURI()),
                ContentType.APPLICATION_JSON);
            cluster.filterSearch().add(
                FS_URI,
                new StaticHttpItem(HttpStatus.SC_OK, entity));
            cluster.producerAsyncClient().add(
                ADDA,
                new ProxyHandler(cluster.testLucene().indexerPort())
            );

            FileEntity cokeJson = new FileEntity(
                new File(getClass().getResource("people/coke_snippets.json").
                    toURI()), ContentType.APPLICATION_JSON);

            cluster.cokemulatorIexlib().add(
                "/process?stid*",
                new StaticHttpItem(HttpStatus.SC_OK, cokeJson));

            FileEntity tikaiteJson = new FileEntity(
                new File(getClass().getResource("./people/tikaite_x_urls.json").
                    toURI()), ContentType.APPLICATION_JSON);
            cluster.tikaite().add(
                MAIL_HANDLER + '*',
                new StaticHttpItem(HttpStatus.SC_OK, tikaiteJson));

            cluster.producer().add(
                "/_status*",
                new StaticHttpResource(
                    HttpStatus.SC_OK,
                    new StringEntity("[{\"localhost\":-1}]")));

            FileEntity rcaHhJson = new FileEntity(
                new File(getClass().getResource("people/rca_hh.json").
                    toURI()), ContentType.APPLICATION_JSON);
            FileEntity rcaIgJson = new FileEntity(
                new File(getClass().getResource("people/rca_ig.json").
                    toURI()), ContentType.APPLICATION_JSON);

            cluster.rca().add(
                "/*",
                new StaticHttpItem(HttpStatus.SC_OK, rcaHhJson),
                new StaticHttpItem(HttpStatus.SC_OK, rcaHhJson),
                new StaticHttpItem(HttpStatus.SC_OK, rcaIgJson),
                new StaticHttpItem(HttpStatus.SC_OK, rcaIgJson),
                new StaticHttpItem(HttpStatus.SC_OK, rcaHhJson));

            LinkedHashMap<String, Object> urlsInfo = new LinkedHashMap<>();
            urlsInfo.put("entity", URLS_INFO);

            cluster.axis().add(
                AXIS_URI,
                new AxisVerifier(UID_VALUE, MAIL)
                    .add(URLS_INFO, urlsInfo));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);
        }
    }
}
