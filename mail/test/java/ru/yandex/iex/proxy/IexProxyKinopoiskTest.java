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

public class IexProxyKinopoiskTest extends TestBase {
    //CSOFF: MultipleStringLiterals
    private static final long UID_FRAUD = 1120000000002254L;
    private static final String ADDA = "/add*";
    private static final String AXIS_URI =
        "/v1/facts/store_batch?client_id=extractors";
    private static final String PG = "pg";
    private static final String HTTP_LOCALHOST = "http://localhost:";
    private static final String NOTIFY = "/notify?mdb=";
    private static final String MAIL = "mail";
    private static final String REFUND_FBL = "refund_fbl";
    private static final String MID_123 = "123";
    private static final String STID_123 = "1.2.3";

    @Test
    public void testAxisStoreEventTicket() throws Exception {
        final String cfg = "extrasettings.axis-facts = event-ticket\n"
            + "entities_rcpt_uid.1120000000002254 = event-ticket\n"
            + "postprocess_rcpt_uid.1120000000002254 ="
                + "event-ticket:http://localhost:" + IexProxyCluster.IPORT
                + "/event-ticket\n";
        try (IexProxyCluster cluster = new IexProxyCluster(this, null, cfg);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.iexproxy().start();
            final long uid = UID_FRAUD;
            final String mid = MID_123;
            final String stid = STID_123;
            String pgPost = IexProxyTestMocks.pgNotifyPost(uid, mid);
            HttpPost post = new HttpPost(
                HTTP_LOCALHOST + cluster.iexproxy().port() + "=" + NOTIFY + PG);
            post.setEntity(
                new StringEntity(pgPost, ContentType.APPLICATION_JSON));

            cluster.corpBlackbox().add(
                "*",
                IexProxyCluster.blackboxResponse(uid, "a@b.c1"));

            cluster.corpFilterSearch().add(
                "*",
                IexProxyTestMocks.filterSearchPgResponse(mid, stid, ""));

            FileEntity cokeJson = new FileEntity(
                new File(
                    getClass().getResource("coke_kinopoisk.json").
                        toURI()), ContentType.APPLICATION_JSON);

            cluster.cokemulatorIexlib().add(
                "/process?stid=1.2.3*",
                new StaticHttpItem(HttpStatus.SC_OK, cokeJson));

            cluster.producer().add(
                "/_status*",
                new StaticHttpResource(
                    HttpStatus.SC_OK,
                    new StringEntity("[{\"localhost\":-1}]")));

            FileEntity kinopoiskJson = new FileEntity(
                new File(
                    getClass().getResource("kinopoisk_response_ql.json").
                        toURI()), ContentType.APPLICATION_JSON);

            FileEntity afishaAnyJson = new FileEntity(
                new File(
                    getClass().getResource("kinopoisk_response_ql.json").
                        toURI()), ContentType.APPLICATION_JSON);

            cluster.kinopoiskQl().add(
                "/*",
                new StaticHttpItem(HttpStatus.SC_OK, kinopoiskJson));

            cluster.afisha().add(
                "/*",
                new StaticHttpItem(HttpStatus.SC_OK, afishaAnyJson));

            cluster.producerAsyncClient().add(
                ADDA,
                new ProxyHandler(cluster.testLucene().indexerPort()));

            LinkedHashMap<String, Object> kinopoisk = new LinkedHashMap<>();
            kinopoisk.put("entity", "event-ticket");

            cluster.axis().add(
                AXIS_URI,
                new AxisVerifier(uid, MAIL)
                    .add("event-ticket", kinopoisk));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);
        }
    }

    @Test
    public void testAxisStoreEventTicketEmptyKinopoiskResponse() throws Exception {
        final String cfg = "extrasettings.axis-facts = event-ticket\n"
            + "entities_rcpt_uid.1120000000002254 = event-ticket\n"
            + "postprocess_rcpt_uid.1120000000002254 ="
                + "event-ticket:http://localhost:" + IexProxyCluster.IPORT
                + "/event-ticket\n";
        try (IexProxyCluster cluster = new IexProxyCluster(this, null, cfg);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.iexproxy().start();
            final long uid = UID_FRAUD;
            final String mid = MID_123;
            final String stid = STID_123;
            String pgPost = IexProxyTestMocks.pgNotifyPost(uid, mid);
            HttpPost post = new HttpPost(
                HTTP_LOCALHOST + cluster.iexproxy().port() + "=" + NOTIFY + PG);
            post.setEntity(
                new StringEntity(pgPost, ContentType.APPLICATION_JSON));

            cluster.corpBlackbox().add(
                "*",
                IexProxyCluster.blackboxResponse(uid, "a@b.c1"));

            cluster.corpFilterSearch().add(
                "*",
                IexProxyTestMocks.filterSearchPgResponse(mid, stid, ""));

            FileEntity cokeJson = new FileEntity(
                new File(
                    getClass().getResource("coke_kinopoisk.json").
                        toURI()), ContentType.APPLICATION_JSON);

            cluster.cokemulatorIexlib().add(
                "/process?stid=1.2.3*",
                new StaticHttpItem(HttpStatus.SC_OK, cokeJson));

            cluster.producer().add(
                "/_status*",
                new StaticHttpResource(
                    HttpStatus.SC_OK,
                    new StringEntity("[{\"localhost\":-1}]")));

            FileEntity kinopoiskJson = new FileEntity(
                new File(
                    getClass().getResource("kinopoisk_empty_response_ql.json").
                        toURI()), ContentType.APPLICATION_JSON);

            FileEntity afishaAnyJson = new FileEntity(
                new File(
                    getClass().getResource("kinopoisk_response_ql.json").
                        toURI()), ContentType.APPLICATION_JSON);

            cluster.kinopoiskQl().add(
                "/*",
                new StaticHttpItem(HttpStatus.SC_OK, kinopoiskJson));

            cluster.afisha().add(
                "/*",
                new StaticHttpItem(HttpStatus.SC_OK, afishaAnyJson));

            cluster.producerAsyncClient().add(
                ADDA,
                new ProxyHandler(cluster.testLucene().indexerPort()));

            LinkedHashMap<String, Object> kinopoisk = new LinkedHashMap<>();
            kinopoisk.put("entity", "event-ticket");

            cluster.axis().add(
                AXIS_URI,
                new AxisVerifier(uid, MAIL)
                    .add("event-ticket", kinopoisk));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);
        }
    }

    @Test
    public void testAxisStoreEventTicketKinopoiskNotImplemented() throws Exception {
        final String cfg = "extrasettings.axis-facts = event-ticket\n"
            + "entities_rcpt_uid.1120000000002254 = event-ticket\n"
            + "postprocess_rcpt_uid.1120000000002254 ="
                + "event-ticket:http://localhost:" + IexProxyCluster.IPORT
                + "/event-ticket\n";
        try (IexProxyCluster cluster = new IexProxyCluster(this, null, cfg);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.iexproxy().start();
            final long uid = UID_FRAUD;
            final String mid = MID_123;
            final String stid = STID_123;
            String pgPost = IexProxyTestMocks.pgNotifyPost(uid, mid);
            HttpPost post = new HttpPost(
                HTTP_LOCALHOST + cluster.iexproxy().port() + "=" + NOTIFY + PG);
            post.setEntity(
                new StringEntity(pgPost, ContentType.APPLICATION_JSON));

            cluster.corpBlackbox().add(
                "*",
                IexProxyCluster.blackboxResponse(uid, "a@b.c1"));

            cluster.corpFilterSearch().add(
                "*",
                IexProxyTestMocks.filterSearchPgResponse(mid, stid, ""));

            FileEntity cokeJson = new FileEntity(
                new File(
                    getClass().getResource("coke_kinopoisk.json").
                        toURI()), ContentType.APPLICATION_JSON);

            cluster.cokemulatorIexlib().add(
                "/process?stid=1.2.3*",
                new StaticHttpItem(HttpStatus.SC_OK, cokeJson));

            cluster.producer().add(
                "/_status*",
                new StaticHttpResource(
                    HttpStatus.SC_OK,
                    new StringEntity("[{\"localhost\":-1}]")));

            FileEntity kinopoiskJson = new FileEntity(
                new File(
                    getClass().getResource("kinopoisk_response_ql.json").
                        toURI()), ContentType.APPLICATION_JSON);

            FileEntity afishaAnyJson = new FileEntity(
                new File(
                    getClass().getResource("kinopoisk_response_ql.json").
                        toURI()), ContentType.APPLICATION_JSON);

            cluster.afisha().add(
                "/*",
                new StaticHttpItem(HttpStatus.SC_OK, afishaAnyJson));

            cluster.producerAsyncClient().add(
                ADDA,
                new ProxyHandler(cluster.testLucene().indexerPort()));

            LinkedHashMap<String, Object> kinopoisk = new LinkedHashMap<>();
            kinopoisk.put("entity", "event-ticket");

            cluster.axis().add(
                AXIS_URI,
                new AxisVerifier(uid, MAIL)
                    .add("event-ticket", kinopoisk));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);
        }
    }
}
