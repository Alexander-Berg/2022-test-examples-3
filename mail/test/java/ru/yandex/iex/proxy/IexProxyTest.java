package ru.yandex.iex.proxy;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.blackbox.BlackboxUserinfo;
import ru.yandex.collection.Pattern;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.test.ProxyHandler;
import ru.yandex.http.test.ProxyMultipartHandler;
import ru.yandex.http.test.StaticHttpItem;
import ru.yandex.http.test.StaticServer;
import ru.yandex.json.writer.JsonType;
import ru.yandex.search.document.mail.MailMetaInfo;
import ru.yandex.search.prefix.LongPrefix;
import ru.yandex.test.search.backend.TestSearchBackend;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;
import ru.yandex.util.filesystem.DeletingFileVisitor;

public class IexProxyTest extends TestBase {
    //CSOFF: MultipleStringLiterals
    private static final String ADDA = "/add*";
    private static final String AXIS_URI =
        "/v1/facts/store_batch?client_id=extractors";
    //private static final String ESHOP = "eshop";
    //private static final String ESHOP_BK = "eshop_bk";
    //private static final String EDA = "eda";
    private static final String EVENTS = "events";
    private static final String EVENT1 = "event1";
    private static final String MAIL_HANDLER =
        "/tikaite?json-type=dollar&stid=";
    private static final String HELLO = "hello";
    private static final String UID_PARAM = "&uid=";
    private static final String PG = "pg";
    //private static final String FILTER_SEARCH =
    //        "/filter_search?order=default&full_folders_and_labels=1&uid=";
    private static final String HTTP_LOCALHOST = "http://localhost:";
    private static final String NOTIFY = "/notify?mdb=";
    private static final String MAIL = "mail";
    private static final String FS_URI = "/filter_search*";
    private static final String CORP_FS_URI = "/filter_search*";
    private static final String CONTENTLINE = "contentline";
    private static final String TICKET = "ticket";
    private static final String TICKET1 = "ticket1";
    private static final String UNDER_TICKET = "_ticket";
    private static final String STORE_PARAMS = "storefs.json";
    //private static final String STORE_PARAMS_TYPE = "storefs_type.json";
    //private static final String STORE_PARAMS_EDA_FISCAL =
    //                                 "storefs_eda_fiscal.json";
    //private static final String STORE_PARAMS_ESHOP_BK =
    //                                         "storefs_eshop_bk_axis.json";
    private static final String MID_123 = "123";
    private static final String STID_123 = "1.2.3";
    //private static final int FOUR = 4;
    private static final long UID_VALUE = BlackboxUserinfo.CORP_UID_BEGIN;
    //private static final long UID_VAL_ESHOP = 1130000013896717L;

    @Test
    public void testAccepted() {
        Assert.assertTrue(true);
    }

    @Test
    public void testUserNotFound() throws Exception {
        try (IexProxyCluster cluster = new IexProxyCluster(
                this,
                null,
                "entities.default = events",
                true);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.iexproxy().start();
            String mid = "100505";
            String blackboxUri = IexProxyCluster.blackboxUri(
                UID_PARAM + UID_VALUE);
            cluster.corpBlackbox().add(
                blackboxUri,
                "{\"users\":[{\"id\":\"220660041221\","
                + "\"uid\":{},\"karma\":{\"value\":0},"
                + "\"karma_status\":{\"value\":0}}]}");
            HttpPost post = new HttpPost(
                HTTP_LOCALHOST + cluster.iexproxy().port() + NOTIFY + PG);
            String pgPost = IexProxyTestMocks.pgNotifyPost(UID_VALUE, mid);
            post.setEntity(
                new StringEntity(pgPost, ContentType.APPLICATION_JSON));
            HttpAssert.assertStatusCode(HttpStatus.SC_NOT_FOUND, client, post);
        }
    }

    @Test
    public void testNotify() throws Exception {
        try (IexProxyCluster cluster = new IexProxyCluster(this);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.iexproxy().start();
            String suid = "9006";
            String mid = "100506";
            String blackboxUri = IexProxyCluster.blackboxUri(
                UID_PARAM + UID_VALUE);
            cluster.corpBlackbox().add(
                blackboxUri,
                IexProxyCluster.blackboxResponse(UID_VALUE, "a@b.c", suid, PG));
            FileEntity entity = new FileEntity(
                new File(getClass().getResource(STORE_PARAMS).toURI()),
                ContentType.APPLICATION_JSON);
            cluster.corpFilterSearch().add(
                CORP_FS_URI,
                new StaticHttpItem(HttpStatus.SC_OK, entity));
            cluster.producerAsyncClient().add(
                ADDA,
                new ProxyHandler(cluster.testLucene().indexerPort())
            );
            cluster.cokemulatorIexlib().add(
                "/process?stid=1.2.6&*",
                "{\"events\":{}, \"contentline\":\"jopa2\"}");

            HttpPost post = new HttpPost(
                HTTP_LOCALHOST + cluster.iexproxy().port() + NOTIFY + PG);
            String pgPost = IexProxyTestMocks.pgNotifyPost(UID_VALUE, mid);
            post.setEntity(
                new StringEntity(pgPost, ContentType.APPLICATION_JSON));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);
        }
    }

    @Test
    public void testSkipSpam() throws Exception {
        final String cfg = "extrasettings.axis-facts = _ticket, events, taxi\n"
            + "headers.message-type-43 = rpop-info, message-id, "
            + "message-id-hash \n";
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
                IexProxyTestMocks.
                    filterSearchPgResponse(mid, stid, "43", "spam"));

            LinkedHashMap<String, Object> event1 = new LinkedHashMap<>();
            event1.put(EVENT1, HELLO);
            LinkedHashMap<String, Object> ticket = new LinkedHashMap<>();
            ticket.put(TICKET1, true);
            LinkedHashMap<String, Object> cokelution = new LinkedHashMap<>();
            cokelution.put(EVENTS, event1);
            cokelution.put(CONTENTLINE, "jopa6p");
            cokelution.put(TICKET, ticket);

            cluster.cokemulatorIexlib().add(
                "/process?stid=" + stid + '*',
                JsonType.NORMAL.toString(cokelution));

            cluster.tikaite().add(
                MAIL_HANDLER + stid + '*',
                "{\"prefix\":" + uid
                    + ", \"docs\":[{\"headers\":\"x-yandex-rpop-id: 123\\n"
                    + "x-yandex-rpop-info: qweqwe@imap.yandex.ru\\n"
                    + "message-id: <weqwe@qeqwe>\"}]}");

            cluster.producerAsyncClient().add(
                ADDA,
                new ProxyHandler(cluster.testLucene().indexerPort()));

            cluster.axis().add(
                AXIS_URI,
                new AxisVerifier(1, MAIL)
                    .add(EVENTS, event1)
                    .add(UNDER_TICKET, ticket));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);

            final String onlineUri = "/online?uid=" + uid;
            final String msearchUri = "/api/async/enlarge/your?uid=" + uid;
            cluster.onlineDB().add(onlineUri, new OnlineHandler(true));
            cluster.msearch().add(msearchUri, "");

            String tikaiteUrl = MAIL_HANDLER + stid + '*';
            //rpop-info: foreign
            cluster.tikaite().add(
                tikaiteUrl,
                "{\"prefix\":" + uid
                    + ", \"docs\":[{\"headers\":\"x-yandex-rpop-id: 123\\n"
                    + "x-yandex-rpop-info: qweqwe@imap.google.ru\"}]}");
            cluster.producerAsyncClient().add(
                ADDA,
                new ProxyHandler(cluster.testLucene().indexerPort()));
            cluster.axis().add(
                AXIS_URI,
                new AxisVerifier(1, MAIL)
                    .add(EVENTS, event1)
                    .add(UNDER_TICKET, ticket));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);
            Assert.assertEquals(0, cluster.tikaite().accessCount(tikaiteUrl));
            Assert.assertEquals(1, cluster.onlineDB().accessCount(onlineUri));
            Assert.assertEquals(1, cluster.msearch().accessCount(msearchUri));

            cluster.onlineDB().add(onlineUri, new OnlineHandler(false));

            //rpop-info: none
            cluster.tikaite().add(
                MAIL_HANDLER + stid + '*',
                "{\"prefix\":" + uid
                    + ", \"docs\":[{\"headers\":\"x-yandex-rpop-id: 123\\n"
                    + "x-yandex-rpop-fuflo: qweqwe@imap.google.ru\"}]}");
            cluster.producerAsyncClient().add(
                ADDA,
                new ProxyHandler(cluster.testLucene().indexerPort()));
            cluster.axis().add(
                AXIS_URI,
                new AxisVerifier(1, MAIL)
                    .add(EVENTS, event1)
                    .add(UNDER_TICKET, ticket));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);
            Assert.assertEquals(1, cluster.onlineDB().accessCount(onlineUri));
            Assert.assertEquals(1, cluster.msearch().accessCount(msearchUri));
        }
    }

    @Test
    public void testAxisStoreHeaders() throws Exception {
        final String cfg = "extrasettings.axis-facts = _ticket, events, taxi\n"
            + "headers.message-type-43 = rpop-info, message-id, "
            + "message-id-hash\n";
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
                IexProxyTestMocks.filterSearchPgResponse(mid, stid, "43"));

            LinkedHashMap<String, Object> event1 = new LinkedHashMap<>();
            event1.put(EVENT1, HELLO);
            LinkedHashMap<String, Object> ticket = new LinkedHashMap<>();
            ticket.put(TICKET1, true);
            LinkedHashMap<String, Object> cokelution = new LinkedHashMap<>();
            cokelution.put(EVENTS, event1);
            cokelution.put(CONTENTLINE, "jopa6p");
            cokelution.put(TICKET, ticket);

            cluster.cokemulatorIexlib().add(
                "/process?stid=" + stid + '*',
                JsonType.NORMAL.toString(cokelution));

            final String tikaiteUrl = "/headers?json-type=dollar&stid=" + stid;

            cluster.tikaite().add(
                tikaiteUrl,
                "{\"headers\":[{\"x-yandex-rpop-id\":\"123\"},"
                + "{\"x-yandex-rpop-info\": \"qweqwe@imap.yandex.ru\"},"
                + "{\"message-id\":\"<weqwe@qeqwe>\"}]}");

            cluster.producerAsyncClient().add(
                ADDA,
                new ProxyHandler(cluster.testLucene().indexerPort()));

            cluster.axis().add(
                AXIS_URI,
                new AxisVerifier(1, MAIL)
                    .add(EVENTS, event1)
                    .add(UNDER_TICKET, ticket));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);

            final String onlineUri = "/online?uid=" + uid;
            final String msearchUri = "/api/async/enlarge/your?uid=" + uid;
            Assert.assertEquals(1, cluster.tikaite().accessCount(tikaiteUrl));
            cluster.onlineDB().add(onlineUri, new OnlineHandler(true));
            cluster.msearch().add(msearchUri, "");

            //rpop-info: foreign
            cluster.tikaite().add(
                tikaiteUrl,
                "{\"headers\":[{\"x-yandex-rpop-id\": 123},"
                + "{\"x-yandex-rpop-info\": \"qweqwe@imap.google.ru\"}]}");
            cluster.producerAsyncClient().add(
                ADDA,
                new ProxyHandler(cluster.testLucene().indexerPort()));
            cluster.axis().add(
                AXIS_URI,
                new AxisVerifier(1, MAIL)
                    .add(EVENTS, event1)
                    .add(UNDER_TICKET, ticket));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);
            Assert.assertEquals(1, cluster.tikaite().accessCount(tikaiteUrl));
            Assert.assertEquals(1, cluster.onlineDB().accessCount(onlineUri));
            Assert.assertEquals(1, cluster.msearch().accessCount(msearchUri));

            cluster.onlineDB().add(onlineUri, new OnlineHandler(false));

            //rpop-info: none
            cluster.tikaite().add(
                tikaiteUrl,
                "{\"headers\":[{\"x-yandex-rpop-id\": 123},"
                + "{\"x-yandex-rpop-fuflo\": \"qweqwe@imap.google.ru\"}]}");
            cluster.producerAsyncClient().add(
                ADDA,
                new ProxyHandler(cluster.testLucene().indexerPort()));
            cluster.axis().add(
                AXIS_URI,
                new AxisVerifier(1, MAIL)
                    .add(EVENTS, event1)
                    .add(UNDER_TICKET, ticket));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);
            Assert.assertEquals(1, cluster.onlineDB().accessCount(onlineUri));
            Assert.assertEquals(1, cluster.msearch().accessCount(msearchUri));
        }
    }

    @Test
    public void testAxisStoreHeadersByDomain() throws Exception {
        final String cfg = "extrasettings.axis-facts = _ticket, events, taxi\n"
            + "headers_domain.d$c = rpop-info, message-id, message-id-hash\n"
            + "entities_domain.d$c = taxi\n"
            + "postprocess_domain.d$c ="
                + "taxi:http://localhost:" + IexProxyCluster.IPORT + "/taxi\n";
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

            LinkedHashMap<String, Object> event1 = new LinkedHashMap<>();
            event1.put(EVENT1, HELLO);
            LinkedHashMap<String, Object> ticket = new LinkedHashMap<>();
            ticket.put(TICKET1, true);
            LinkedHashMap<String, Object> cokelution = new LinkedHashMap<>();
            cokelution.put(EVENTS, event1);
            cokelution.put(CONTENTLINE, "jopa6p");
            cokelution.put(TICKET, ticket);

            cluster.cokemulatorIexlib().add(
                "/process?stid=" + stid + '*',
                JsonType.NORMAL.toString(cokelution));

            cluster.tikaite().add(
                "/headers?json-type=dollar&stid=" + stid,
                "{\"headers\":[{\"x-yandex-rpop-id\":123},"
                + "{\"x-yandex-rpop-info\": \"qweqwe@imap.yandex.ru\"}]}");

            cluster.producerAsyncClient().add(
                ADDA,
                new ProxyHandler(cluster.testLucene().indexerPort()));

            cluster.axis().add(
                AXIS_URI,
                new AxisVerifier(1, MAIL)
                    .add(EVENTS, event1)
                    .add(UNDER_TICKET, ticket));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);
        }
    }

    @Test
    public void testAxisStore() throws Exception {
        final String cfg = "extrasettings.axis-facts = _ticket, events";
        try (IexProxyCluster cluster = new IexProxyCluster(this, null, cfg);
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
                new File(getClass().getResource(STORE_PARAMS).toURI()),
                ContentType.APPLICATION_JSON);
            cluster.corpFilterSearch().add(
                CORP_FS_URI,
                new StaticHttpItem(HttpStatus.SC_OK, entity));
            cluster.producerAsyncClient().add(
                ADDA,
                new ProxyHandler(cluster.testLucene().indexerPort())
            );
            LinkedHashMap<String, Object> event1 = new LinkedHashMap<>();
            event1.put(EVENT1, HELLO);
            LinkedHashMap<String, Object> ticket = new LinkedHashMap<>();
            ticket.put(TICKET1, true);
            LinkedHashMap<String, Object> cokelution = new LinkedHashMap<>();
            cokelution.put(EVENTS, event1);
            cokelution.put(CONTENTLINE, "jopa5");
            cokelution.put(TICKET, ticket);

            cluster.cokemulatorIexlib().add(
                "/process?stid=1.2.7&*",
                JsonType.NORMAL.toString(cokelution));

            cluster.axis().add(
                AXIS_URI,
                new AxisVerifier(UID_VALUE, MAIL)
                    .add(EVENTS, event1)
                    .add(UNDER_TICKET, ticket));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);
        }
    }

    @Test
    public void testAxisStoreReindex() throws Exception {
        final String cfg = "extrasettings.axis-facts = _ticket,"
            + " events";
        try (IexProxyCluster cluster = new IexProxyCluster(this, null, cfg);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.iexproxy().start();
            String suid = "90072";
            String mid = "1005072";
            HttpPost post = new HttpPost(
                HTTP_LOCALHOST + cluster.iexproxy().port() + NOTIFY + PG);
            String pgPost = IexProxyTestMocks.pgNotifyPost(UID_VALUE, mid);
            post.setEntity(
                new StringEntity(pgPost, ContentType.APPLICATION_JSON));
            String blackboxUri = IexProxyCluster.blackboxUri(
                UID_PARAM + UID_VALUE);
            cluster.corpBlackbox().add(
                blackboxUri,
                IexProxyCluster.blackboxResponse(UID_VALUE, "ab@b.d", suid));
            FileEntity entity = new FileEntity(
                new File(getClass().getResource(STORE_PARAMS).toURI()),
                ContentType.APPLICATION_JSON);
            cluster.corpFilterSearch().add(
                CORP_FS_URI,
                new StaticHttpItem(HttpStatus.SC_OK, entity));
            cluster.producerAsyncClient().add(
                ADDA,
                new ProxyHandler(cluster.testLucene().indexerPort())
            );
            String threadId = "7788778877";
            LinkedHashMap<String, Object> event1 = new LinkedHashMap<>();
            event1.put(EVENT1, HELLO);
            LinkedHashMap<String, Object> ticket = new LinkedHashMap<>();
            ticket.put(TICKET1, true);
            LinkedHashMap<String, Object> tid = new LinkedHashMap<>();
            tid.put(MailMetaInfo.THREAD_ID_FIELD, threadId);
            LinkedHashMap<String, Object> cokelution = new LinkedHashMap<>();
            cokelution.put(EVENTS, event1);
            cokelution.put(CONTENTLINE, "jopa333");
            cokelution.put(TICKET, ticket);

            cluster.cokemulatorIexlib().add(
                "/process?stid=1.2.8&*",
                JsonType.NORMAL.toString(cokelution));

            cluster.axis().add(
                AXIS_URI,
                new AxisVerifier(UID_VALUE, MAIL, true)
                    .add(EVENTS, event1)
                    .add(MailMetaInfo.THREAD_ID_FIELD, tid)
                    .add(UNDER_TICKET, ticket));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);
        }
    }

    @Test
    public void testAxisStoreReindexUpdateCache() throws Exception {
        final String cfg = "extrasettings.axis-facts = "
            + "ticket, events";
        try (IexProxyCluster cluster = new IexProxyCluster(this, null, cfg);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.iexproxy().start();
            String suid = "90071";
            String mid = "1005071";
            HttpPost post = new HttpPost(
                HTTP_LOCALHOST + cluster.iexproxy().port() + NOTIFY + PG);
            String pgPost = IexProxyTestMocks.pgNotifyPost(UID_VALUE, mid);
            post.setEntity(
                new StringEntity(pgPost, ContentType.APPLICATION_JSON));
            String blackboxUri = IexProxyCluster.blackboxUri(
                UID_PARAM + UID_VALUE);
            cluster.corpBlackbox().add(
                blackboxUri,
                IexProxyCluster.blackboxResponse(UID_VALUE, "a@b.d", suid));
            FileEntity entity = new FileEntity(
                new File(getClass().getResource(STORE_PARAMS).toURI()),
                ContentType.APPLICATION_JSON);
            cluster.corpFilterSearch().add(
                CORP_FS_URI,
                new StaticHttpItem(HttpStatus.SC_OK, entity));
            cluster.producerAsyncClient().add(
                ADDA,
                new ProxyHandler(cluster.testLucene().indexerPort())
            );
            LinkedHashMap<String, Object> event1 = new LinkedHashMap<>();
            event1.put(EVENT1, HELLO);
            LinkedHashMap<String, Object> ticket = new LinkedHashMap<>();
            ticket.put(TICKET1, true);
            LinkedHashMap<String, Object> cokelution = new LinkedHashMap<>();
            cokelution.put(EVENTS, event1);
            cokelution.put(CONTENTLINE, "jopa2");
            cokelution.put(TICKET, ticket);

            cluster.cokemulatorIexlib().add(
                "/process?stid=1.2.9&*",
                JsonType.NORMAL.toString(cokelution));

            cluster.axis().add(
                AXIS_URI,
                new AxisVerifier(UID_VALUE, MAIL, true)
                    .add(EVENTS, event1)
                    .add(UNDER_TICKET, ticket));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);
        }
    }

    @Test
    public void testNoJournaling() throws Exception {
        final String cfg = "extrasettings.no-journaling-facts = _contentline";
        File root = Files.createTempDirectory("testNoJournaling").toFile();
        try (IexProxyCluster cluster = new IexProxyCluster(this, root, cfg);
            CloseableHttpClient client = Configs.createDefaultClient();
            BufferedReader logReader =
                new BufferedReader(
                    new InputStreamReader(
                        new FileInputStream(
                            cluster.iexproxy().config()
                                .reqresLog().single().file()),
                        StandardCharsets.UTF_8)))
        {
            cluster.iexproxy().start();
            String suid = "9008";
            String mid = "100508";
            HttpPost post = new HttpPost(
                HTTP_LOCALHOST + cluster.iexproxy().port() + NOTIFY + PG);
            String pgPost = IexProxyTestMocks.pgNotifyPost(UID_VALUE, mid);
            post.setEntity(
                new StringEntity(pgPost, ContentType.APPLICATION_JSON));
            String blackboxUri = IexProxyCluster.blackboxUri(
                UID_PARAM + UID_VALUE);
            cluster.corpBlackbox().add(
                blackboxUri,
                IexProxyCluster.blackboxResponse(UID_VALUE, "a@b.e", suid));
            FileEntity entity = new FileEntity(
                new File(getClass().getResource(STORE_PARAMS).toURI()),
                ContentType.APPLICATION_JSON);
            cluster.corpFilterSearch().add(
                CORP_FS_URI,
                new StaticHttpItem(HttpStatus.SC_OK, entity));
            cluster.producerAsyncClient().add(
                ADDA,
                new ProxyHandler(cluster.testLucene().indexerPort())
            );
            LinkedHashMap<String, Object> event1 = new LinkedHashMap<>();
            event1.put("event2", "hello2");
            LinkedHashMap<String, Object> ticket = new LinkedHashMap<>();
            ticket.put("ticket2", true);
            LinkedHashMap<String, Object> cokelution = new LinkedHashMap<>();
            cokelution.put(EVENTS, event1);
            cokelution.put(CONTENTLINE, "jopa3");
            cokelution.put(TICKET, ticket);

            cluster.cokemulatorIexlib().add(
                "/process?stid=1.2.10&*",
                JsonType.NORMAL.toString(cokelution));

            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);

            for (
                String logLine = logReader.readLine();
                logLine != null;
                logLine = logReader.readLine())
            {
                YandexAssert.assertNotContains("e=_contentline", logLine);
            }
        } finally {
            removeDirectory(root);
        }
    }

    private static void removeDirectory(final File directory) throws Exception {
        Files.walkFileTree(directory.toPath(), DeletingFileVisitor.INSTANCE);
    }

    @Test
    public void testTickerReminder() throws Exception {
        try (IexProxyCluster cluster = new IexProxyCluster(this);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.iexproxy().start();
            HttpPost post = new HttpPost(
                HTTP_LOCALHOST + cluster.iexproxy().port()
                + "/ticket?email=orders@ozon.ru"
                + "&user_email=iex.proxy1@yandex.ru"
                + "&received_date=1470076399&uid=396972830"
                + "&mid=159596311794943389");
            cluster.reminder().add(
                "/mail/create-or-update-flight-reminder",
                "{\"reminder\":{}, \"anwser\":\"opa\"}");
            post.setEntity(
                new FileEntity(
                    new File(
                        getClass().getResource(
                            "ticketReminder1.json").toURI())));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);
        }
    }

    private void testMessageStorageChange(final long uid) throws Exception {
        try (IexProxyCluster cluster = new IexProxyCluster(this, true)) {
            cluster.iexproxy().start();
            cluster.testLucene().add(
                new LongPrefix(uid),
                "\"url\":\"first_e1\",\"fact_mid\":\"1\","
                + "\"fact_stid\":\"1.2.3\",\"fact_data\":\"9\"",
                "\"url\":\"first_e2\",\"fact_mid\":\"1\","
                + "\"fact_stid\":\"1.2.3\",\"fact_data\":\"8\"",
                "\"url\":\"second_e1\",\"fact_mid\":\"2\","
                + "\"fact_stid\":\"2.2.3\",\"fact_data\":\"7\"",
                "\"url\":\"second_e3\",\"fact_mid\":\"2\","
                + "\"fact_stid\":\"2.2.3\",\"fact_data\":\"6\"");
            StaticServer blackbox;
            StaticServer filterSearch;
            if (uid == 1L) {
                blackbox = cluster.blackbox();
                filterSearch = cluster.filterSearch();
            } else {
                blackbox = cluster.corpBlackbox();
                filterSearch = cluster.corpFilterSearch();
            }
            String blackboxUri = IexProxyCluster.blackboxUri(UID_PARAM + uid);

            blackbox.add(
                blackboxUri,
                IexProxyCluster.blackboxResponse(uid, "a@b.c.d", "100500"));
            String commonFields =
                ",\"fid\":11,\"receiveDate\":1234567890,\"threadId\":12,"
                + "\"rfcId\":13,\"folder\":{\"type\":{\"title\":\"user\"},"
                + "\"name\":\"folder\"}";
            filterSearch.add(
                FS_URI,
                "{\"envelopes\":[{\"mid\":1,\"stid\":\"1.2.4\""
                + commonFields + "},{\"mid\":2,\"stid\":\"2.2.4\""
                + commonFields + "},{\"mid\":3,\"stid\":\"3.2.4\""
                + commonFields + "}]}");
            cluster.producerAsyncClient().register(
                new Pattern<>("", true),
                new ProxyMultipartHandler(cluster.testLucene().indexerPort()));
            HttpPost post = new HttpPost(
                HTTP_LOCALHOST + cluster.iexproxy().port() + NOTIFY + PG);
            post.setEntity(
                new StringEntity(
                    "{\"uid\": \"" + uid
                    + "\",\"lcn\": \"15\","
                    + "\"change_type\": \"message-storage-change\","
                    + "\"operation_date\": \"1234567890\","
                    + "\"operation_id\": \"1230\","
                    + "\"changed\": [{\"mid\":1},{\"mid\":2},{\"mid\":3}],"
                    + "\"fresh_count\": \"0\","
                    + "\"useful_new_messages\": \"1\","
                    + "\"pgshard\": \"2\"}",
                    ContentType.APPLICATION_JSON));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);
            cluster.testLucene().checkSearch(
                "/search?sort=fact_data&prefix=" + uid
                + "&get=url,fact_mid,fact_stid,fact_data&text=fact_mid:*",
                TestSearchBackend.prepareResult(
                    "\"url\":\"first_e1\",\"fact_mid\":\"1\""
                    + ",\"fact_stid\":\"1.2.4\",\"fact_data\":\"9\"",
                    "\"url\":\"first_e2\",\"fact_mid\":\"1\""
                    + ",\"fact_stid\":\"1.2.4\",\"fact_data\":\"8\"",
                    "\"url\":\"second_e1\",\"fact_mid\":\"2\""
                    + ",\"fact_stid\":\"2.2.4\",\"fact_data\":\"7\"",
                    "\"url\":\"second_e3\",\"fact_mid\":\"2\""
                    + ",\"fact_stid\":\"2.2.4\",\"fact_data\":\"6\""));
        }
    }

    @Test
    public void testMessageStorageChange() throws Exception {
        testMessageStorageChange(1L);
    }

    @Test
    public void testMessageStorageChangeCorp() throws Exception {
        final long uid = 1120000000004695L;
        testMessageStorageChange(uid);
    }
}
