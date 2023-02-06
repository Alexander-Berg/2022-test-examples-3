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

public class IexProxyEdaTest extends TestBase {
    //CSOFF: MultipleStringLiterals
    private static final String ADDA = "/add*";
    private static final String AXIS_URI =
        "/v1/facts/store_batch?client_id=extractors";
    private static final String EDA = "eda";
    private static final String UID_PARAM = "&uid=";
    private static final String PG = "pg";
    private static final String HTTP_LOCALHOST = "http://localhost:";
    private static final String NOTIFY = "/notify?mdb=";
    private static final String MAIL = "mail";
    private static final String STORE_PARAMS_EDA_FISCAL =
                                     "storefs_eda_fiscal.json";
    //private static final String STORE_PARAMS_ESHOP_BK =
    //                                         "storefs_eshop_bk_axis.json";
    private static final String MID_123 = "123";
    private static final String STID_123 = "1.2.3";

    @Test
    public void testAxisStoreDeliveryFiscalDeliveryCost() throws Exception {
        final String cfg = "extrasettings.axis-facts = eda, eda_extended\n"
            + "entities_domain.corp$mail$ru = eda\n"
            + "postprocess_domain.corp$mail$ru ="
                + "eda:http://localhost:" + IexProxyCluster.IPORT + "/eda, "
                + "eda_extended:http://localhost:" + IexProxyCluster.IPORT + "/eda_extended\n";
        try (IexProxyCluster cluster = new IexProxyCluster(this, null, cfg);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.iexproxy().start();
            final long uid = 1;
            final String mid = MID_123;
            //final String stid = STID_123;
            String pgPost = IexProxyTestMocks.pgNotifyPost(uid, mid);
            HttpPost post = new HttpPost(
                HTTP_LOCALHOST + cluster.iexproxy().port() + NOTIFY + PG);
            post.setEntity(
                new StringEntity(pgPost, ContentType.APPLICATION_JSON));

            String blackboxUri = IexProxyCluster.blackboxUri(UID_PARAM + uid);
            cluster.blackbox().add(
                blackboxUri,
                IexProxyCluster.blackboxResponse(uid, "a@b.c1"));

            String filterSearchUri = IexProxyTestMocks.
                                            filterSearchUri(uid, mid);
            FileEntity entity = new FileEntity(
                new File(getClass().
                    getResource(STORE_PARAMS_EDA_FISCAL).toURI()),
                ContentType.APPLICATION_JSON);
            cluster.filterSearch().add(
                filterSearchUri,
                entity);

            FileEntity cokeJson = new FileEntity(
                new File(getClass().getResource("coke_eda_delivery_fiscal.json").
                    toURI()), ContentType.APPLICATION_JSON);

            cluster.cokemulatorIexlib().add(
                "/process?stid=*",
                new StaticHttpItem(HttpStatus.SC_OK, cokeJson));

            cluster.producer().add(
                "/_status*",
                new StaticHttpResource(
                    HttpStatus.SC_OK,
                    new StringEntity("[{\"localhost\":-1}]")));

            cluster.producerAsyncClient().add(
                ADDA,
                new ProxyHandler(cluster.testLucene().indexerPort()));

            LinkedHashMap<String, Object> eda = new LinkedHashMap<>();
            eda.put("entity", EDA);

            cluster.axis().add(
                AXIS_URI,
                new AxisVerifier(1, MAIL)
                    .add(EDA, eda));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);
        }
    }

    @Test
    public void testAxisStoreFiscalOrderAndDelivery() throws Exception {
        final String cfg = "extrasettings.axis-facts = eda, eda_extended\n"
            + "entities_domain.corp$mail$ru = eda\n"
            + "postprocess_domain.corp$mail$ru ="
                + "eda:http://localhost:" + IexProxyCluster.IPORT + "/eda, "
                + "eda_extended:http://localhost:" + IexProxyCluster.IPORT + "/eda_extended\n";
        try (IexProxyCluster cluster = new IexProxyCluster(this, null, cfg);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.iexproxy().start();
            final long uid = 1;
            final String mid = MID_123;
            //final String stid = STID_123;
            String pgPost = IexProxyTestMocks.pgNotifyPost(uid, mid);
            HttpPost post = new HttpPost(
                HTTP_LOCALHOST + cluster.iexproxy().port() + NOTIFY + PG);
            post.setEntity(
                new StringEntity(pgPost, ContentType.APPLICATION_JSON));

            String blackboxUri = IexProxyCluster.blackboxUri(UID_PARAM + uid);
            cluster.blackbox().add(
                blackboxUri,
                IexProxyCluster.blackboxResponse(uid, "a@b.c1"));

            String filterSearchUri = IexProxyTestMocks.
                                            filterSearchUri(uid, mid);
            FileEntity entity = new FileEntity(
                new File(getClass().
                    getResource(STORE_PARAMS_EDA_FISCAL).toURI()),
                ContentType.APPLICATION_JSON);
            cluster.filterSearch().add(
                filterSearchUri,
                entity);

            FileEntity cokeJson = new FileEntity(
                new File(getClass().getResource("coke_eda_cart_order_and_delivery.json").
                    toURI()), ContentType.APPLICATION_JSON);

            cluster.cokemulatorIexlib().add(
                "/process?stid=*",
                new StaticHttpItem(HttpStatus.SC_OK, cokeJson));

            cluster.producer().add(
                "/_status*",
                new StaticHttpResource(
                    HttpStatus.SC_OK,
                    new StringEntity("[{\"localhost\":-1}]")));

            cluster.producerAsyncClient().add(
                ADDA,
                new ProxyHandler(cluster.testLucene().indexerPort()));

            LinkedHashMap<String, Object> eda = new LinkedHashMap<>();
            eda.put("entity", EDA);

            cluster.axis().add(
                AXIS_URI,
                new AxisVerifier(1, MAIL)
                    .add(EDA, eda));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);
        }
    }

    @Test
    public void testAxisStoreFiscalDeliveryAndTax() throws Exception {
        final String cfg = "extrasettings.axis-facts = eda, eda_extended\n"
            + "entities_domain.corp$mail$ru = eda\n"
            + "postprocess_domain.corp$mail$ru ="
                + "eda:http://localhost:" + IexProxyCluster.IPORT + "/eda, "
                + "eda_extended:http://localhost:" + IexProxyCluster.IPORT + "/eda_extended\n";
        try (IexProxyCluster cluster = new IexProxyCluster(this, null, cfg);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.iexproxy().start();
            final long uid = 1;
            final String mid = MID_123;
            //final String stid = STID_123;
            String pgPost = IexProxyTestMocks.pgNotifyPost(uid, mid);
            HttpPost post = new HttpPost(
                HTTP_LOCALHOST + cluster.iexproxy().port() + NOTIFY + PG);
            post.setEntity(
                new StringEntity(pgPost, ContentType.APPLICATION_JSON));

            String blackboxUri = IexProxyCluster.blackboxUri(UID_PARAM + uid);
            cluster.blackbox().add(
                blackboxUri,
                IexProxyCluster.blackboxResponse(uid, "a@b.c1"));

            String filterSearchUri = IexProxyTestMocks.
                                            filterSearchUri(uid, mid);
            FileEntity entity = new FileEntity(
                new File(getClass().
                    getResource(STORE_PARAMS_EDA_FISCAL).toURI()),
                ContentType.APPLICATION_JSON);
            cluster.filterSearch().add(
                filterSearchUri,
                entity);

            FileEntity cokeJson = new FileEntity(
                new File(getClass().getResource("coke_eda_cart_delivery_and_tax.json").
                    toURI()), ContentType.APPLICATION_JSON);

            cluster.cokemulatorIexlib().add(
                "/process?stid=*",
                new StaticHttpItem(HttpStatus.SC_OK, cokeJson));

            cluster.producer().add(
                "/_status*",
                new StaticHttpResource(
                    HttpStatus.SC_OK,
                    new StringEntity("[{\"localhost\":-1}]")));

            cluster.producerAsyncClient().add(
                ADDA,
                new ProxyHandler(cluster.testLucene().indexerPort()));

            LinkedHashMap<String, Object> eda = new LinkedHashMap<>();
            eda.put("entity", EDA);

            cluster.axis().add(
                AXIS_URI,
                new AxisVerifier(1, MAIL)
                    .add(EDA, eda));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);
        }
    }

    @Test
    public void testAxisStoreEda() throws Exception {
        final String cfg = "extrasettings.axis-facts = eda, eda_extended\n"
            + "entities_domain.d$c = eda\n"
            + "postprocess_domain.d$c ="
            //+ "entities_domain.corp$mail$ru = eda\n"
            //+ "postprocess_domain.corp$mail$ru ="
                + "eda:http://localhost:" + IexProxyCluster.IPORT + "/eda, "
                + "eda_extended:http://localhost:" + IexProxyCluster.IPORT + "/eda_extended\n";
        try (IexProxyCluster cluster = new IexProxyCluster(this, null, cfg);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.iexproxy().start();
            final long uid = 1;
            final String mid = MID_123;
            final String stid = STID_123;
            String pgPost = IexProxyTestMocks.pgNotifyPost(uid, mid);
            HttpPost post = new HttpPost(
                HTTP_LOCALHOST + cluster.iexproxy().port() + NOTIFY + PG);
            post.setEntity(
                new StringEntity(pgPost, ContentType.APPLICATION_JSON));

            String blackboxUri = IexProxyCluster.blackboxUri(UID_PARAM + uid);
            cluster.blackbox().add(
                blackboxUri,
                IexProxyCluster.blackboxResponse(uid, "a@b.c1"));

            String filterSearchUri = IexProxyTestMocks.
                                            filterSearchUri(uid, mid);
            cluster.filterSearch().add(
                filterSearchUri,
                IexProxyTestMocks.filterSearchPgResponse(mid, stid, ""));

            FileEntity cokeJson = new FileEntity(
                new File(getClass().getResource("coke_eda_list.json").
                    toURI()), ContentType.APPLICATION_JSON);

            cluster.cokemulatorIexlib().add(
                "/process?stid=1.2.3*",
                new StaticHttpItem(HttpStatus.SC_OK, cokeJson));

            cluster.producer().add(
                "/_status*",
                new StaticHttpResource(
                    HttpStatus.SC_OK,
                    new StringEntity("[{\"localhost\":-1}]")));

            cluster.producerAsyncClient().add(
                ADDA,
                new ProxyHandler(cluster.testLucene().indexerPort()));

            LinkedHashMap<String, Object> eda = new LinkedHashMap<>();
            eda.put("entity", EDA);

            cluster.axis().add(
                AXIS_URI,
                new AxisVerifier(1, MAIL)
                    .add(EDA, eda));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);
        }
    }
}
