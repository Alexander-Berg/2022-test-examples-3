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

public class IexProxyAdvPaymentTest extends TestBase {
    //CSOFF: MultipleStringLiterals
    private static final String ADDA = "/add*";
    private static final String AXIS_URI =
        "/v1/facts/store_batch?client_id=extractors";
    private static final String MAIL_HANDLER =
        "/mail/handler?json-type=dollar&ultra-fast-mode&";
    private static final String UID_PARAM = "&uid=";
    private static final String PG = "pg";
    private static final String HTTP_LOCALHOST = "http://localhost:";
    private static final String NOTIFY = "/notify?mdb=";
    private static final String MAIL = "mail";
    private static final String ADV_PAYMENT = "adv_payment";
    private static final String MID_123 = "123";
    private static final String STID_123 = "1.2.3";

    @Test
    public void testAxisStoreAdvPaymentGoogle() throws Exception {
        final String cfg = "extrasettings.axis-facts = adv_payment\n"
            + "entities_domain.d$c = adv_payment\n"
            + "postprocess_domain.d$c ="
                + "adv_payment:http://localhost:" + IexProxyCluster.IPORT
                + "/adv_payment\n";
        try (IexProxyCluster cluster = new IexProxyCluster(this, null, cfg);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.iexproxy().start();
            final long uid = 1;
            final String mid = MID_123;
            final String stid = STID_123;
            String pgPost = IexProxyTestMocks.pgNotifyPost(uid, mid);
            HttpPost post = new HttpPost(
                HTTP_LOCALHOST + cluster.iexproxy().port() + "=" + NOTIFY + PG);
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
                new File(getClass().getResource("coke_adv_payment_google.json").
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

            LinkedHashMap<String, Object> advPayment = new LinkedHashMap<>();
            advPayment.put("entity", ADV_PAYMENT);

            cluster.axis().add(
                AXIS_URI,
                new AxisVerifier(1, MAIL)
                    .add(ADV_PAYMENT, advPayment));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);
        }
    }

    @Test
    public void testAxisStoreAdvPaymentFacebook() throws Exception {
        final String cfg = "extrasettings.axis-facts = adv_payment\n"
            + "entities_domain.support$facebook$com = adv_payment\n"
            + "postprocess_domain.support$facebook$com ="
                + "adv_payment:http://localhost:" + IexProxyCluster.IPORT
                + "/adv_payment\n";
        try (IexProxyCluster cluster = new IexProxyCluster(this, null, cfg);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.iexproxy().start();
            final long uid = 1;
            final String mid = MID_123;
            final String stid = STID_123;
            String pgPost = IexProxyTestMocks.pgNotifyPost(uid, mid);
            HttpPost post = new HttpPost(
                HTTP_LOCALHOST + cluster.iexproxy().port() + "=" + NOTIFY + PG);
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
                IexProxyTestMocks.filterSearchPgResponse(
                    mid,
                    stid,
                    "",
                    "user",
                    "advertise-noreply",
                    "support.facebook.com"));

            FileEntity cokeJson = new FileEntity(
                new File(getClass().getResource("coke_adv_payment_facebook.json").
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

            LinkedHashMap<String, Object> advPayment = new LinkedHashMap<>();
            advPayment.put("entity", ADV_PAYMENT);

            cluster.axis().add(
                AXIS_URI,
                new AxisVerifier(1, MAIL)
                    .add(ADV_PAYMENT, advPayment));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);
        }
    }

    @Test
    public void testAxisStoreAdvPaymentFacebookOneRow() throws Exception {
        final String cfg = "extrasettings.axis-facts = adv_payment\n"
            + "entities_domain.support$facebook$com = adv_payment\n"
            + "postprocess_domain.support$facebook$com ="
                + "adv_payment:http://localhost:" + IexProxyCluster.IPORT
                + "/adv_payment\n";
        try (IexProxyCluster cluster = new IexProxyCluster(this, null, cfg);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.iexproxy().start();
            final long uid = 1;
            final String mid = MID_123;
            final String stid = STID_123;
            String pgPost = IexProxyTestMocks.pgNotifyPost(uid, mid);
            HttpPost post = new HttpPost(
                HTTP_LOCALHOST + cluster.iexproxy().port() + "=" + NOTIFY + PG);
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
                IexProxyTestMocks.filterSearchPgResponse(
                    mid,
                    stid,
                    "",
                    "user",
                    "advertise-noreply",
                    "support.facebook.com"));

            FileEntity cokeJson = new FileEntity(
                new File(getClass().getResource("coke_adv_payment_facebook_one_row.json").
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

            LinkedHashMap<String, Object> advPayment = new LinkedHashMap<>();
            advPayment.put("entity", ADV_PAYMENT);

            cluster.axis().add(
                AXIS_URI,
                new AxisVerifier(1, MAIL)
                    .add(ADV_PAYMENT, advPayment));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);
        }
    }

    @Test
    public void testAxisStoreAdvPaymentDirectCampaign() throws Exception {
        final String cfg = "extrasettings.axis-facts = adv_payment\n"
            + "entities_email.direct_noreply@yandex-team$ru = adv_payment\n"
            + "postprocess_email.direct_noreply@yandex-team$ru ="
                + "adv_payment:http://localhost:" + IexProxyCluster.IPORT
                + "/adv_payment_direct\n";
        try (IexProxyCluster cluster = new IexProxyCluster(this, null, cfg);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.iexproxy().start();
            final long uid = 1;
            final String mid = MID_123;
            final String stid = STID_123;
            String pgPost = IexProxyTestMocks.pgNotifyPost(uid, mid);
            HttpPost post = new HttpPost(
                HTTP_LOCALHOST + cluster.iexproxy().port() + "=" + NOTIFY + PG);
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
                IexProxyTestMocks.filterSearchPgResponse(
                    mid,
                    stid,
                    "",
                    "user",
                    "direct_noreply",
                    "yandex-team.ru"));

            FileEntity cokeJson = new FileEntity(
                new File(getClass().getResource("coke_adv_payment_facebook_one_row.json").
                    toURI()), ContentType.APPLICATION_JSON);

            cluster.cokemulatorIexlib().add(
                "/process?stid=1.2.3*",
                new StaticHttpItem(HttpStatus.SC_OK, cokeJson));

            FileEntity tikaiteJson = new FileEntity(
                new File(getClass().getResource("./adv_payment/tikaite_response_direct_campaign.json").
                    toURI()), ContentType.APPLICATION_JSON);
            cluster.tikaite().add(
                MAIL_HANDLER + '*',
                new StaticHttpItem(HttpStatus.SC_OK, tikaiteJson),
                new StaticHttpItem(HttpStatus.SC_OK, tikaiteJson),
                new StaticHttpItem(HttpStatus.SC_OK, tikaiteJson));


            cluster.producer().add(
                "/_status*",
                new StaticHttpResource(
                    HttpStatus.SC_OK,
                    new StringEntity("[{\"localhost\":-1}]")));

            cluster.producerAsyncClient().add(
                ADDA,
                new ProxyHandler(cluster.testLucene().indexerPort()));

            LinkedHashMap<String, Object> advPayment = new LinkedHashMap<>();
            advPayment.put("entity", ADV_PAYMENT);

            cluster.axis().add(
                AXIS_URI,
                new AxisVerifier(1, MAIL)
                    .add(ADV_PAYMENT, advPayment));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);
        }
    }

    @Test
    public void testAxisStoreAdvPaymentDirectLogin() throws Exception {
        final String cfg = "extrasettings.axis-facts = adv_payment\n"
            + "entities_email.direct_noreply@yandex-team$ru = adv_payment\n"
            + "postprocess_email.direct_noreply@yandex-team$ru ="
                + "adv_payment:http://localhost:" + IexProxyCluster.IPORT
                + "/adv_payment_direct\n";
        try (IexProxyCluster cluster = new IexProxyCluster(this, null, cfg);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.iexproxy().start();
            final long uid = 1;
            final String mid = MID_123;
            final String stid = STID_123;
            String pgPost = IexProxyTestMocks.pgNotifyPost(uid, mid);
            HttpPost post = new HttpPost(
                HTTP_LOCALHOST + cluster.iexproxy().port() + "=" + NOTIFY + PG);
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
                IexProxyTestMocks.filterSearchPgResponse(
                    mid,
                    stid,
                    "",
                    "user",
                    "direct_noreply",
                    "yandex-team.ru"));

            FileEntity cokeJson = new FileEntity(
                new File(getClass().getResource("coke_adv_payment_facebook_one_row.json").
                    toURI()), ContentType.APPLICATION_JSON);

            cluster.cokemulatorIexlib().add(
                "/process?stid=1.2.3*",
                new StaticHttpItem(HttpStatus.SC_OK, cokeJson));

            FileEntity tikaiteJson = new FileEntity(
                new File(getClass().getResource("./adv_payment/tikaite_response_direct_login.json").
                    toURI()), ContentType.APPLICATION_JSON);
            cluster.tikaite().add(
                MAIL_HANDLER + '*',
                new StaticHttpItem(HttpStatus.SC_OK, tikaiteJson),
                new StaticHttpItem(HttpStatus.SC_OK, tikaiteJson),
                new StaticHttpItem(HttpStatus.SC_OK, tikaiteJson));


            cluster.producer().add(
                "/_status*",
                new StaticHttpResource(
                    HttpStatus.SC_OK,
                    new StringEntity("[{\"localhost\":-1}]")));

            cluster.producerAsyncClient().add(
                ADDA,
                new ProxyHandler(cluster.testLucene().indexerPort()));

            LinkedHashMap<String, Object> advPayment = new LinkedHashMap<>();
            advPayment.put("entity", ADV_PAYMENT);

            cluster.axis().add(
                AXIS_URI,
                new AxisVerifier(1, MAIL)
                    .add(ADV_PAYMENT, advPayment));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);
        }
    }

}
