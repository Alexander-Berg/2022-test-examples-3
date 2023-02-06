package ru.yandex.search.mail.kamaji;

import java.io.File;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.dbfields.MailIndexFields;
import ru.yandex.dbfields.OracleFields;
import ru.yandex.function.FalsePredicate;
import ru.yandex.http.test.ChainedHttpResource;
import ru.yandex.http.test.ExpectingHeaderHttpItem;
import ru.yandex.http.test.ExpectingHttpItem;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.test.StaticHttpItem;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.http.util.YandexHeaders;
import ru.yandex.http.util.client.PostsRedirectStrategy;
import ru.yandex.json.dom.ValueContentHandler;
import ru.yandex.parser.email.types.MessageType;
import ru.yandex.search.document.mail.MailMetaInfo;
import ru.yandex.search.mail.kamaji.senders.StoreSendersIndexerModule;
import ru.yandex.test.search.backend.TestMailSearchBackend;
import ru.yandex.test.search.backend.TestSearchBackend;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.YandexAssert;

public class KamajiPgTest extends KamajiTestBase {
    private static final String HID = "0";
    private static final String EMPTY_LUCENE_RESPONSE =
        "{\"hitsCount\":0,\"hitsArray\":[]}";

    private static String slowBackendSearch(
        final KamajiCluster cluster,
        final long prefix,
        final String mid)
    {
        String slowSrchUri =
            "/search?IO_PRIO=3000&prefix=" + prefix
                + "&get=mid,queueId&text=url:"
                + prefix + '_' + mid + "%2F0";
        cluster.backend().add(
            slowSrchUri,
            "{\"hitsCount\": 1,\"hitsArray\":[{\"mid\": \""
                + mid + "\"} ] }");
        return slowSrchUri;
    }

    @Test
    public void testDelete() throws Exception {
        try (KamajiCluster cluster = new KamajiCluster();
            CloseableHttpClient client = HttpClients.createDefault())
        {
            String zooQueue = "delete_queue";
            String queueId = "300";
            String uid = "320178675";
            String deleteUri = DELETE + uid + "&text=mid_p:(1+2+3+5)";
            cluster.backend().add(
                deleteUri,
                new StaticHttpResource(
                    new ExpectingHeaderHttpItem(
                        new StaticHttpItem(HttpStatus.SC_OK),
                        new BasicHeader(YandexHeaders.ZOO_QUEUE, zooQueue),
                        new BasicHeader(YandexHeaders.ZOO_QUEUE_ID, queueId),
                        new BasicHeader(YandexHeaders.ZOO_SHARD_ID, uid))));

            HttpPost post = new HttpPost(
                HTTP_LOCALHOST + cluster.kamaji().port() + NOTIFY);
            post.addHeader(YandexHeaders.ZOO_QUEUE, zooQueue);
            post.addHeader(YandexHeaders.ZOO_QUEUE_ID, queueId);
            post.addHeader(YandexHeaders.ZOO_SHARD_ID, uid);
            post.setEntity(
                new StringEntity(
                    "{\"operation_id\": \"160014\","
                    + "\"uid\": \"320178675\","
                    + "\"lcn\": \"245\","
                    + "\"change_type\": \"delete\","
                    + "\"operation_date\": \"1436810748.043101\","
                    + "\"changed\": ["
                    + "{\"src_fid\": 4, \"mid\": 1},"
                    + "{\"src_fid\": 4, \"mid\": 2},"
                    + "{\"src_fid\": 4, \"mid\": 3},"
                    + "{\"src_fid\": 4, \"mid\": 5}],"
                    + "\"fresh_count\": \"2558\","
                    + "\"useful_new_messages\": \"0\","
                    + "\"pgshard\": \"1\"}",
                    ContentType.APPLICATION_JSON));
            cluster.kamaji().start();
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);
            Assert.assertEquals(1, cluster.backend().accessCount(deleteUri));
        }
    }

    @Test
    public void testDeleteUser() throws Exception {
        try (KamajiCluster cluster = new KamajiCluster();
            CloseableHttpClient client = HttpClients.createDefault())
        {
            String zooQueue = "delete_user_queue";
            String queueId = "301";
            String uid = "320178676";
            String deleteUri =
                DELETE + uid + "&text=uid:" + uid
                + "+OR+fact_uid:" + uid
                + "+OR+__prefix:" + uid
                + "+OR+senders_uid:" + uid
                + "+OR+senders_domain_uid:320178676";
            cluster.backend().add(
                deleteUri,
                new StaticHttpResource(
                    new ExpectingHeaderHttpItem(
                        new StaticHttpItem(HttpStatus.SC_OK),
                        new BasicHeader(YandexHeaders.ZOO_QUEUE, zooQueue),
                        new BasicHeader(YandexHeaders.ZOO_QUEUE_ID, queueId),
                        new BasicHeader(YandexHeaders.ZOO_SHARD_ID, uid))));

            HttpPost post = new HttpPost(
                HTTP_LOCALHOST + cluster.kamaji().port() + NOTIFY);
            post.addHeader(YandexHeaders.ZOO_QUEUE, zooQueue);
            post.addHeader(YandexHeaders.ZOO_QUEUE_ID, queueId);
            post.addHeader(YandexHeaders.ZOO_SHARD_ID, uid);
            post.setEntity(
                new StringEntity(
                    "{\"operation_id\": \"160013\","
                    + "\"uid\": \"320178676\","
                    + "\"lcn\": \"246\","
                    + "\"change_type\": \"user-delete\","
                    + "\"operation_date\": \"1436810740.043101\","
                    + "\"fresh_count\": \"2550\","
                    + "\"useful_new_messages\": \"5\","
                    + "\"pgshard\": \"15\"}",
                    ContentType.APPLICATION_JSON));
            cluster.kamaji().start();
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);
            Assert.assertEquals(1, cluster.backend().accessCount(deleteUri));
        }
    }

    @Test
    public void testMidsCleanup() throws Exception {
        try (KamajiCluster cluster = new KamajiCluster();
            CloseableHttpClient client = HttpClients.createDefault())
        {
            String deleteUri =
                DELETE + "9000&text=((mid_padded:[0+TO+3]+OR+"
                + "mid_padded:[9+TO+99999999999999999999])+AND+lcn:[0+TO+22])"
                + "+OR+(uid:9000+AND+NOT+mid_padded:*)";
            cluster.backend().add(
                deleteUri,
                new StaticHttpResource(HttpStatus.SC_OK));
            HttpPost post = new HttpPost(
                HTTP_LOCALHOST + cluster.kamaji().port() + NOTIFY);
            post.setEntity(
                new StringEntity(
                    "{\"operation_id\": \"160015\","
                    + "\"uid\": \"9000\",\"lcn\": \"22\","
                    + "\"change_type\": \"search-mids-cleanup\","
                    + "\"operation_date\": \"1436810748.043102\","
                    + "\"changed\": [{\"mid\":4},{\"mid\":8}]}",
                    ContentType.APPLICATION_JSON));
            cluster.kamaji().start();
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);
            Assert.assertEquals(1, cluster.backend().accessCount(deleteUri));
        }
    }

    // CSOFF: MethodLength
    @Test
    public void testStore() throws Exception {
        try (KamajiCluster cluster = new KamajiCluster();
            CloseableHttpClient client = HttpClients.createDefault())
        {
            final String fsJsonFile = "store-fs.json";
            final long uid = 203889311L;
            String queueId = "6789";
            String mid = "156218612074414099";
            String to = "hirthwork@yandex.ru";
            String from = "potapov.d@gmail.com\n";
            String lcn = "33";
            String stid = "1.632123143.7594801846142779115218810981";
            String subject = "unseen email";
            String receivedDate = "14387010855";
            String bbUri = blackboxUri(UID_PARAM + uid);
            cluster.blackbox().add(
                bbUri,
                new StaticHttpItem(blackboxResponse(uid, to)),
                new StaticHttpItem(blackboxResponse(uid, to)),
                new StaticHttpItem(blackboxResponse(uid, to, from.trim())),
                new StaticHttpItem(blackboxResponse(uid, to, from.trim())),
                new StaticHttpItem(blackboxResponse(uid, to)),
                new StaticHttpItem(blackboxResponse(uid, to)));

            cluster.backend().add(
                "/search?" + "IO_PRIO=3000&prefix=" + uid + "&text=url:usrtype_"
                    + uid + "_potapovd@gmail.com&get=user_types",
                EMPTY_LUCENE_RESPONSE);

            List<String> preservedFastFields = new ArrayList<>();
            preservedFastFields.add(MailIndexFields.SHERLOCK_TEMPLATES);
            List<String> preservedSlowFields = new ArrayList<>();
            preservedSlowFields.addAll(
                StoreSendersIndexerModule.STORE_PRESERVE_FIELDS);

            HttpPost post = new HttpPost(
                HTTP_LOCALHOST + cluster.kamaji().port() + NOTIFY
                + QUEUE_ID_P + queueId);
            post.addHeader(YandexHeaders.ZOO_QUEUE_ID, queueId);
            post.setEntity(
                new FileEntity(
                    new File(getClass().getResource(STORE_ENVELOPE).toURI()),
                    ContentType.APPLICATION_JSON));
            String fsUri =
                FILTER_SEARCH + uid
                + "&mdb=pg&suid=90000&lcn=33&operation-id=166697"
                + "&pgshard=1&mids=" + mid;
            FileEntity entity = new FileEntity(
                new File(getClass().getResource(fsJsonFile).toURI()),
                ContentType.APPLICATION_JSON);
            cluster.filterSearch().add(
                fsUri,
                new StaticHttpItem(HttpStatus.SC_OK, entity),
                new StaticHttpItem(HttpStatus.SC_OK, entity),
                new StaticHttpItem(HttpStatus.SC_OK, entity),
                new StaticHttpItem(HttpStatus.SC_OK, entity),
                new StaticHttpItem(EMPTY_ENVELOPES),
                new StaticHttpItem(EMPTY_ENVELOPES));

            Map<String, Object> fastDoc = new HashMap<>();
            fastDoc.put(MailIndexFields.UID, Long.toString(uid));
            fastDoc.put(MailIndexFields.MID, mid);
            fastDoc.put(MailIndexFields.HID, HID);
            fastDoc.put(MailIndexFields.QUEUE_ID, queueId);
            fastDoc.put(
                MailIndexFields.URL,
                Long.toString(uid) + '_' + mid + '/' + HID);
            fastDoc.put(MailIndexFields.STID, stid);
            fastDoc.put(
                MailIndexFields.HDR + MailIndexFields.FROM,
                "\"Dmitry Potapov\" <potapov.d@gmail.com>\n");
            fastDoc.put(
                MailIndexFields.HDR + MailIndexFields.FROM
                + MailIndexFields.EMAIL,
                from);
            fastDoc.put(
                MailIndexFields.HDR + MailIndexFields.FROM
                + MailIndexFields.NORMALIZED,
                "potapovd@gmail.com\n");
            fastDoc.put(
                MailIndexFields.HDR + MailIndexFields.FROM
                + MailIndexFields.DISPLAY_NAME,
                "Dmitry Potapov\n");
            fastDoc.put(
                MailIndexFields.REPLY_TO,
                "\"Potapov, Dmitry\" <potapovd@googlemail.com>\n");
            fastDoc.put(
                MailIndexFields.REPLY_TO + MailIndexFields.EMAIL,
                "potapovd@googlemail.com\n");
            fastDoc.put(
                MailIndexFields.REPLY_TO + MailIndexFields.NORMALIZED,
                "potapovd@gmail.com\n");
            fastDoc.put(
                MailIndexFields.REPLY_TO + MailIndexFields.DISPLAY_NAME,
                "Potapov, Dmitry\n");
            String hdrTo = MailIndexFields.HDR + MailIndexFields.TO;
            fastDoc.put(MailIndexFields.QUEUE_ID, queueId);
            fastDoc.put(hdrTo, "\"hirthwork@yandex.ru\" <" + to + '>' + '\n');
            fastDoc.put(hdrTo + MailIndexFields.EMAIL, to + '\n');
            fastDoc.put(hdrTo + MailIndexFields.NORMALIZED, to + '\n');
            fastDoc.put(hdrTo + MailIndexFields.DISPLAY_NAME, to + '\n');
            fastDoc.put(MailIndexFields.LCN, lcn);
            fastDoc.put(
                MailIndexFields.ATTACHMENTS,
                "MoreOcaml.pdf\nbenchmark.csv");
            fastDoc.put(MailIndexFields.LIDS, "7\nFAKE_ATTACHED_LBL\n");
            fastDoc.put(MailMetaInfo.HAS_ATTACHMENTS, Boolean.TRUE.toString());
            fastDoc.put(MailIndexFields.UNREAD, Boolean.TRUE.toString());
            fastDoc.put(MailIndexFields.PURE_BODY, "subj");
            fastDoc.put(MailIndexFields.FID, Integer.toString(1));
            fastDoc.put(MailIndexFields.FOLDER_NAME, "Inbox");
            fastDoc.put(MailIndexFields.FOLDER_TYPE, "inbox");
            fastDoc.put(MailMetaInfo.MESSAGE_TYPE, "4 people");
            String hdrSubject = MailMetaInfo.HDR + MailMetaInfo.SUBJECT;
            fastDoc.put(hdrSubject, subject);
            fastDoc.put(hdrSubject + MailMetaInfo.NORMALIZED, subject);
            fastDoc.put(MailMetaInfo.RECEIVED_DATE, receivedDate);
            fastDoc.put(
                MailIndexFields.MSG_ID,
                "<CALZx8J2i9X7+fZkMTda12OJS1z5Pp8eiRq0gg6xRE7ABwsSAKg@mail."
                + "gmail.com>");
            fastDoc.put(MailMetaInfo.THREAD_ID_FIELD, mid);

            Map<String, Object> sherlockDoc = new HashMap<>();
            sherlockDoc.put(MailIndexFields.URL, "shrlck_" + uid + '_' + stid);
            sherlockDoc.put(MailIndexFields.SHERLOCK_MID, mid);

            MailMetaInfo meta =
                new MailMetaInfo(-1, -1, FalsePredicate.INSTANCE);
            meta.set(MailMetaInfo.LCN, lcn);
            meta.set(MailMetaInfo.MID, mid);
            meta.set(MailMetaInfo.UID, Long.toString(uid));
            meta.set(MailMetaInfo.STID, stid);
            meta.set(MailMetaInfo.HDR + MailMetaInfo.TO, to);
            meta.set(
                MailMetaInfo.HDR + MailMetaInfo.FROM,
                from);
            meta.set(MailMetaInfo.RECEIVED_DATE, receivedDate);
            meta.setMessageTypes(
                Collections.singleton(MessageType.PEOPLE.typeNumber()));

            Map<String, Object> fastDocs =
                docComplete(cluster.kamaji().config(), preservedFastFields);
            fastDocs.put(PREFIX, uid);
            fastDocs.put(DOCS, Arrays.asList(fastDoc, sherlockDoc));
            String fastUri = MODIFY + uid + "&fast&mid=" + mid;
            cluster.backend().add(
                fastUri,
                new ChainedHttpResource(
                    new ExpectingHeaderHttpItem(
                        new ExpectingHttpItem(
                            new JsonChecker(fastDocs)),
                        new BasicHeader(
                            YandexHeaders.ZOO_QUEUE_ID,
                            queueId)),
                    new ExpectingHeaderHttpItem(
                        new ExpectingHttpItem(
                            new JsonChecker(fastDocs)),
                        new BasicHeader(
                            YandexHeaders.ZOO_QUEUE_ID,
                            queueId)),
                    new ExpectingHeaderHttpItem(
                        new ExpectingHttpItem(
                            new JsonChecker(fastDocs)),
                        new BasicHeader(
                            YandexHeaders.ZOO_QUEUE_ID,
                            queueId))));

            String tikaiteUri = "/mail/handler?json-type=dollar&stid=" + stid;
            cluster.tikaite().add(
                tikaiteUri,
                "{\"docs\":[{\"hid\":\"1\",\"pure_body\":\"hello\"}]}");

            String hello = "hello";
            Map<String, Object> doc = new HashMap<>(fastDoc);
            doc.remove(MailIndexFields.PURE_BODY);
            doc.put(MailIndexFields.PURE_BODY, hello);
            doc.put(MailIndexFields.HID, Integer.toString(1));
            doc.put(MailIndexFields.QUEUE_ID, queueId);
            doc.put(
                MailIndexFields.URL,
                Long.toString(uid) + '_' + mid + '/' + '1');

            Map<String, Object> docComplete =
                docComplete(
                    cluster.kamaji().config(),
                    preservedSlowFields);

            docComplete.put(PREFIX, uid);
            List<Map<String, Object>> sendDocs = sendersDocs(
                cluster,
                post.getURI().toString(),
                CharsetUtils.toString(post.getEntity()),
                uid,
                lcn,
                new String(
                    Files.readAllBytes(new File(getClass().getResource(
                        fsJsonFile).toURI()).toPath()),
                    StandardCharsets.UTF_8),
                to);

            List<Object> docs = new ArrayList<>();
            docs.add(fastDoc);
            docs.addAll(sendDocs);
            docs.add(doc);
            docComplete.put(DOCS, docs);

            Map<String, Object> docWithoutSenders =
                docComplete(
                    cluster.kamaji().config(),
                    preservedSlowFields);

            docWithoutSenders.put(PREFIX, uid);
            docWithoutSenders.put(DOCS, Arrays.asList(fastDoc, doc));

            String slowIndexUri = MODIFY + uid + MID + mid;
            // second notify do not generate senders because from presents in
            // addresses in blackbox
            cluster.backend().add(
                slowIndexUri,
                new ChainedHttpResource(
                    new ExpectingHeaderHttpItem(
                        new ExpectingHttpItem(new JsonChecker(docComplete)),
                        new BasicHeader(YandexHeaders.ZOO_QUEUE_ID, null),
                        new BasicHeader(
                            YandexHeaders.ZOO_QUEUE_ID_TO_CHECK,
                            null)),
                    new ExpectingHeaderHttpItem(
                        new ExpectingHttpItem(
                            new JsonChecker(docWithoutSenders)),
                        new BasicHeader(YandexHeaders.ZOO_QUEUE_ID, null),
                        new BasicHeader(
                            YandexHeaders.ZOO_QUEUE_ID_TO_CHECK,
                            null)),
                    new ExpectingHeaderHttpItem(
                        new ExpectingHttpItem(new JsonChecker(docComplete)),
                        new BasicHeader(YandexHeaders.ZOO_QUEUE_ID, null),
                        new BasicHeader(
                            YandexHeaders.ZOO_QUEUE_ID_TO_CHECK,
                            null))));

            String deleteUri =
                "/delete?prefix=203889311&text=mid_p:156218612074414099"
                + "+AND+NOT+url:(203889311_156218612074414099/0"
                + "+OR+203889311_156218612074414099/1)";
            cluster.backend().add(deleteUri, HttpStatus.SC_OK);

            String slowSrchUri = slowBackendSearch(cluster, uid, mid);

            cluster.kamaji().start();
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);
            Assert.assertEquals(2, cluster.filterSearch().accessCount(fsUri));
            Assert.assertEquals(2, cluster.blackbox().accessCount(bbUri));
            Assert.assertEquals(1, cluster.backend().accessCount(fastUri));
            Assert.assertEquals(1, cluster.backend().accessCount(slowIndexUri));
            Assert.assertEquals(1, cluster.backend().accessCount(deleteUri));
            Assert.assertEquals(2, cluster.backend().accessCount(slowSrchUri));
            String stats = HttpAssert.stats(cluster.kamaji().port());
            HttpAssert.assertStat(
                "tikaite-requests_ammm",
                Integer.toString(1),
                stats);
            HttpAssert.assertStat(
                "tikaite-errors_ammm",
                Integer.toString(0),
                stats);
            HttpAssert.assertStat(
                "tikaite-total-requests_ammm",
                Integer.toString(1),
                stats);
            HttpAssert.assertStat(
                "tikaite-failed-requests_ammm",
                Integer.toString(0),
                stats);
            HttpAssert.assertStat(
                "filter-search-requests_ammm",
                Integer.toString(2),
                stats);
            HttpAssert.assertStat(
                "filter-search-non-retriable-errors_ammm",
                Integer.toString(0),
                stats);
            HttpAssert.assertStat(
                "inbox-web-text-length_ammm",
                Integer.toString(hello.length()),
                stats);

            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);
            Assert.assertEquals(
                2 + 2,
                cluster.filterSearch().accessCount(fsUri));
            Assert.assertEquals(2 + 2, cluster.blackbox().accessCount(bbUri));
            Assert.assertEquals(2, cluster.backend().accessCount(fastUri));
            Assert.assertEquals(2, cluster.backend().accessCount(slowIndexUri));
            Assert.assertEquals(2, cluster.backend().accessCount(deleteUri));
            Assert.assertEquals(
                2 + 2,
                cluster.backend().accessCount(slowSrchUri));

            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);
            Assert.assertEquals(
                2 + 2 + 2,
                cluster.filterSearch().accessCount(fsUri));
            Assert.assertEquals(
                2 + 2 + 2,
                cluster.blackbox().accessCount(bbUri));
            Assert.assertEquals(2, cluster.backend().accessCount(fastUri));
            Assert.assertEquals(2, cluster.backend().accessCount(slowIndexUri));
            Assert.assertEquals(2, cluster.backend().accessCount(deleteUri));
            // empty envelopes, only 1 request
            Assert.assertEquals(
                2 + 2 + 1,
                cluster.backend().accessCount(slowSrchUri));
        }
    }

    @Test
    public void testBadFilterSearch() throws Exception {
        try (KamajiCluster cluster = new KamajiCluster();
            CloseableHttpClient client = HttpClients.createDefault())
        {
            final long uid = 203889311L;
            String to = "hirthwork2@yandex.ru";
            String bbUri = blackboxUri(UID_PARAM + uid);
            cluster.blackbox().add(
                bbUri,
                blackboxResponse(uid, to));
            String fsUri = FILTER_SEARCH + uid + '*';
            cluster.filterSearch().add(
                fsUri,
                "{\"error\":{\"code\":1000,\"message\":\"Unknown DB error\","
                + "\"reason\":\"can't retrieve labels: RowCollector: "
                + "sharpei_client error: bad http code\"}}");
            cluster.kamaji().start();
            HttpPost post = new HttpPost(
                HTTP_LOCALHOST + cluster.kamaji().port() + NOTIFY);
            post.setEntity(
                new FileEntity(
                    new File(getClass().getResource(STORE_ENVELOPE).toURI()),
                    ContentType.APPLICATION_JSON));
            HttpAssert.assertStatusCode(
                HttpStatus.SC_BAD_GATEWAY,
                client,
                post);
            Assert.assertEquals(1, cluster.filterSearch().accessCount(fsUri));
        }
    }

    // CSOFF: ParameterNumber
    private static Map<String, Object> fastDoc(
        final long uid,
        final String n,
        final String lcn,
        final String queueId)
    {
        Map<String, Object> doc = new HashMap<>();
        doc.put(MailIndexFields.UID, Long.toString(uid));
        doc.put(MailIndexFields.MID, n);
        doc.put(MailIndexFields.HID, HID);
        doc.put(MailIndexFields.URL, Long.toString(uid) + '_' + n + '/' + 0);
        doc.put(MailIndexFields.STID, "1." + n + '.' + n);
        doc.put(MailIndexFields.LCN, lcn);
        doc.put(MailIndexFields.FID, "11");
        doc.put(MailIndexFields.RECEIVED_DATE, "1234567890");
        doc.put(MailIndexFields.THREAD_ID, "12");
        doc.put(MailIndexFields.MSG_ID, "13");
        doc.put(MailIndexFields.FOLDER_NAME, "folder");
        doc.put(MailIndexFields.FOLDER_TYPE, "user");
        doc.put(MailIndexFields.UNREAD, Boolean.TRUE.toString());
        doc.put(MailIndexFields.QUEUE_ID, queueId);
        return doc;
    }

    private static Map<String, Object> fastDocComplete(
        final long uid,
        final String n,
        final String lcn,
        final String queueId)
    {
        Map<String, Object> doc = new HashMap<>();
        doc.put(ADD_IF_NOT_EXISTS, true);
        doc.put(PREFIX, uid);
        doc.put(DOCS, Collections.singletonList(fastDoc(uid, n, lcn, queueId)));
        return doc;
    }
    // CSON: ParameterNumber

    @Test
    public void testUpdate() throws Exception {
        try (KamajiCluster cluster = new KamajiCluster();
            CloseableHttpClient client =
                HttpClients.custom().setRedirectStrategy(
                    new PostsRedirectStrategy()).build())
        {
            final long uid = 1120000000004695L;
            String lcn = "15";
            String mid1 = "1";
            String mid2 = "5";
            String mid3 = "7";
            String mid4 = "9";
            String queueId = "100";
            HttpPost post = new HttpPost(
                HTTP_LOCALHOST + cluster.kamaji().port()
                    + NOTIFY + QUEUE_ID_P + queueId);
            String zooQueue = CHANGE_LOG_QUEUE;

            post.addHeader(YandexHeaders.ZOO_QUEUE, zooQueue);
            post.addHeader(YandexHeaders.ZOO_QUEUE_ID, queueId);
            post.addHeader(YandexHeaders.ZOO_SHARD_ID, Long.toString(uid));
            post.setEntity(
                new StringEntity(
                    "{\"uid\": \"1120000000004695\","
                    + "\"lcn\": \"15\","
                    + "\"change_type\": \"search-update\","
                    + "\"operation_date\": \"1234567890\","
                    + "\"operation_id\": \"1230\","
                    + "\"changed\": [{\"mid\":1},{\"mid\":2},{\"mid\":3},"
                    + "{\"mid\":4},{\"mid\":5},{\"mid\":7},{\"mid\":9}],"
                    + "\"fresh_count\": \"0\","
                    + "\"useful_new_messages\": \"1\","
                    + "\"pgshard\": \"2\"}",
                    ContentType.APPLICATION_JSON));
            String bbUri = blackboxUri(UID_PARAM + uid);
            cluster.blackbox().add(
                bbUri,
                blackboxResponse(uid, "up@dt"));
            String common =
                uid + "&mdb=pg&suid=90000&lcn=15&folder_set=default"
                + "&operation-id=1230&pgshard=2";
            String fsUri1 = FILTER_SEARCH + common + "&mids=1&mids=2";
            String fsUri2 = FILTER_SEARCH + common + "&mids=3&mids=4";
            String fsUri3 = FILTER_SEARCH + common + "&mids=5&mids=7";
            String fsUri4 = FILTER_SEARCH + common + "&mids=9";
            String commonFields =
                ",\"fid\":11,\"receiveDate\":1234567890,\"threadId\":12,"
                + "\"rfcId\":13,\"folder\":{\"type\":{\"title\":\"user\"},"
                + "\"name\":\"folder\"}";
            cluster.filterSearch().add(
                fsUri1,
                "{\"envelopes\":[{\"mid\":1,\"stid\":\"1.1.1\""
                + commonFields + JSON_END);
            cluster.filterSearch().add(fsUri2, EMPTY_ENVELOPES);
            cluster.filterSearch().add(
                fsUri3,
                "{\"envelopes\":[{\"mid\":5,\"stid\":\"1.5.5\""
                + commonFields + "},{\"mid\":7,\"stid\":\"1.7.7\""
                + commonFields + JSON_END);
            cluster.filterSearch().add(
                fsUri4,
                "{\"envelopes\":[{\"mid\":9,\"stid\":\"1.9.9\""
                + commonFields + JSON_END);

            String deletePrefix =
                DELETE + uid + "&text=lcn:[0+TO+15]+AND+mid_padded:[";
            String deleteUri1 =
                deletePrefix + "1+TO+2]+AND+NOT+mid_padded:(1+2)";
            cluster.backend().add(deleteUri1, HttpStatus.SC_OK);
            String deleteUri2 =
                deletePrefix + "2+TO+4]+AND+NOT+mid_padded:(2+3+4)";
            cluster.backend().add(deleteUri2, HttpStatus.SC_OK);
            String deleteUri3 =
                deletePrefix + "4+TO+7]+AND+NOT+mid_padded:(4+5+7)";
            cluster.backend().add(deleteUri3, HttpStatus.SC_OK);
            String deleteUri4 =
                deletePrefix + "7+TO+9]+AND+NOT+mid_padded:(7+9)";
            cluster.backend().add(deleteUri4, HttpStatus.SC_OK);
            String fastUri1 = MODIFY + uid + "&fast&mid=1";
            List<String> preserveFields
                = new ArrayList<>(cluster.kamaji().config().preserveFields());

            Map<String, Object> fastDocComplete =
                fastDocComplete(uid, mid1, lcn, queueId);
            fastDocComplete.put(PRESERVE_FIELDS, preserveFields);

            cluster.backend().add(
                fastUri1,
                new ExpectingHeaderHttpItem(
                    new ExpectingHttpItem(new JsonChecker(fastDocComplete)),
                    new BasicHeader(
                        YandexHeaders.ZOO_QUEUE,
                        zooQueue),
                    new BasicHeader(
                        YandexHeaders.ZOO_QUEUE_ID,
                        null),
                    new BasicHeader(
                        YandexHeaders.ZOO_QUEUE_ID_TO_CHECK,
                        queueId),
                    new BasicHeader(
                        YandexHeaders.ZOO_SHARD_ID,
                        Long.toString(uid))));

            String fastUri2 = MODIFY + uid + "&fast&mid=5";
            fastDocComplete = fastDocComplete(uid, mid2, lcn, queueId);
            fastDocComplete.put(PRESERVE_FIELDS, preserveFields);
            cluster.backend().add(
                fastUri2,
                new ExpectingHeaderHttpItem(
                    new ExpectingHttpItem(new JsonChecker(fastDocComplete)),
                    new BasicHeader(
                        YandexHeaders.ZOO_QUEUE,
                        zooQueue),
                    new BasicHeader(
                        YandexHeaders.ZOO_QUEUE_ID,
                        null),
                    new BasicHeader(
                        YandexHeaders.ZOO_QUEUE_ID_TO_CHECK,
                        queueId),
                    new BasicHeader(
                        YandexHeaders.ZOO_SHARD_ID,
                        Long.toString(uid))));

            String fastUri3 = MODIFY + uid + "&fast&mid=7";
            fastDocComplete = fastDocComplete(uid, mid3, lcn, queueId);
            fastDocComplete.put(PRESERVE_FIELDS, preserveFields);
            cluster.backend().add(
                fastUri3,
                new ExpectingHeaderHttpItem(
                    new ExpectingHttpItem(new JsonChecker(fastDocComplete)),
                    new BasicHeader(
                        YandexHeaders.ZOO_QUEUE,
                        zooQueue),
                    new BasicHeader(
                        YandexHeaders.ZOO_QUEUE_ID,
                        null),
                    new BasicHeader(
                        YandexHeaders.ZOO_QUEUE_ID_TO_CHECK,
                        queueId),
                    new BasicHeader(
                        YandexHeaders.ZOO_SHARD_ID,
                        Long.toString(uid))));

            String fastUri4 = MODIFY + uid + "&fast&mid=9";
            fastDocComplete = fastDocComplete(uid, mid4, lcn, queueId);
            fastDocComplete.put(PRESERVE_FIELDS, preserveFields);
            cluster.backend().add(
                fastUri4,
                new ExpectingHeaderHttpItem(
                    new ExpectingHttpItem(new JsonChecker(fastDocComplete)),
                    new BasicHeader(
                        YandexHeaders.ZOO_QUEUE,
                        zooQueue),
                    new BasicHeader(
                        YandexHeaders.ZOO_QUEUE_ID_TO_CHECK,
                        null),
                    new BasicHeader(
                        YandexHeaders.ZOO_QUEUE_ID,
                        queueId),
                    new BasicHeader(
                        YandexHeaders.ZOO_SHARD_ID,
                        Long.toString(uid))));

            String tikaiteUri1 = "/mail/handler?json-type=dollar&stid=1.1.1";
            cluster.tikaite().add(
                tikaiteUri1,
                "{\"docs\":[{\"hid\":\"1\",\"body_text\":\"hello1\"}]}");
            String tikaiteUri2 = "/mail/handler?json-type=dollar&stid=1.5.5";
            cluster.tikaite().add(
                tikaiteUri2,
                "{\"docs\":[{\"hid\":\"1\",\"body_text\":\"hello5\"}]}");
            String tikaiteUri3 = "/mail/handler?json-type=dollar&stid=1.7.7";
            cluster.tikaite().add(
                tikaiteUri3,
                "{\"docs\":[{\"hid\":\"1\",\"body_text\":\"hello7\"}]}");
            String tikaiteUri4 = "/mail/handler?json-type=dollar&stid=1.9.9";
            cluster.tikaite().add(
                tikaiteUri4,
                "{\"docs\":[{\"hid\":\"1\",\"body_text\":\"hello9\"}]}");

            Map<String, Object> doc =
                new HashMap<>(fastDoc(uid, mid1, lcn, queueId));
            doc.put(MailIndexFields.BODY_TEXT, "hello1");
            doc.put(MailIndexFields.HID, mid1);
            doc.put(MailIndexFields.MID, mid1);
            doc.put(
                MailIndexFields.URL,
                Long.toString(uid) + '_' + mid1 + '/' + 1);
            Map<String, Object> docComplete =
                docComplete(cluster.kamaji().config());
            docComplete.put(PREFIX, uid);
            docComplete.put(
                DOCS,
                Arrays.asList(fastDoc(uid, mid1, lcn, queueId), doc));

            String modifyUri1 = MODIFY + uid + MID + mid1;
            cluster.backend().add(
                modifyUri1,
                new StaticHttpResource(
                    new ExpectingHttpItem(new JsonChecker(docComplete))));

            doc = new HashMap<>(fastDoc(uid, mid2, lcn, queueId));
            doc.put(MailIndexFields.BODY_TEXT, "hello5");
            doc.put(MailIndexFields.HID, mid1);
            doc.put(MailIndexFields.MID, mid2);
            doc.put(
                MailIndexFields.URL,
                Long.toString(uid) + '_' + mid2 + '/' + 1);
            docComplete = docComplete(cluster.kamaji().config());
            docComplete.put(PREFIX, uid);
            docComplete.put(
                DOCS,
                Arrays.asList(fastDoc(uid, mid2, lcn, queueId), doc));

            String modifyUri2 = MODIFY + uid + MID + mid2;
            cluster.backend().add(
                modifyUri2,
                new StaticHttpResource(
                    new ExpectingHttpItem(new JsonChecker(docComplete))));

            doc = new HashMap<>(fastDoc(uid, mid3, lcn, queueId));
            doc.put(MailIndexFields.BODY_TEXT, "hello7");
            doc.put(MailIndexFields.HID, mid1);
            doc.put(MailIndexFields.MID, mid3);
            doc.put(
                MailIndexFields.URL,
                Long.toString(uid) + '_' + mid3 + '/' + 1);
            docComplete = docComplete(cluster.kamaji().config());
            docComplete.put(PREFIX, uid);
            docComplete.put(
                DOCS,
                Arrays.asList(fastDoc(uid, mid3, lcn, queueId), doc));
            String modifyUri3 = MODIFY + uid + MID + mid3;
            cluster.backend().add(
                modifyUri3,
                new StaticHttpResource(
                    new ExpectingHttpItem(new JsonChecker(docComplete))));

            doc = new HashMap<>(fastDoc(uid, mid4, lcn, queueId));
            doc.put(MailIndexFields.BODY_TEXT, "hello9");
            doc.put(MailIndexFields.HID, mid1);
            doc.put(MailIndexFields.MID, mid4);
            doc.put(
                MailIndexFields.URL,
                Long.toString(uid) + '_' + mid4 + '/' + 1);
            docComplete = docComplete(cluster.kamaji().config());
            docComplete.put(PREFIX, uid);
            docComplete.put(
                DOCS,
                Arrays.asList(fastDoc(uid, mid4, lcn, queueId), doc));

            String modifyUri4 = MODIFY + uid + MID + mid4;
            cluster.backend().add(
                modifyUri4,
                new StaticHttpResource(
                    new ExpectingHttpItem(new JsonChecker(docComplete))));
            String cleanupUri1 =
                "/delete?prefix=1120000000004695&text=mid_p:1+AND+NOT+url:("
                + "1120000000004695_1/0+OR+1120000000004695_1/1)";
            cluster.backend().add(cleanupUri1, HttpStatus.SC_OK);
            String cleanupUri2 =
                "/delete?prefix=1120000000004695&text=mid_p:5+AND+NOT+url:("
                + "1120000000004695_5/0+OR+1120000000004695_5/1)";
            cluster.backend().add(cleanupUri2, HttpStatus.SC_OK);
            String cleanupUri3 =
                "/delete?prefix=1120000000004695&text=mid_p:7+AND+NOT+url:("
                + "1120000000004695_7/0+OR+1120000000004695_7/1)";
            cluster.backend().add(cleanupUri3, HttpStatus.SC_OK);
            String cleanupUri4 =
                "/delete?prefix=1120000000004695&text=mid_p:9+AND+NOT+url:("
                + "1120000000004695_9/0+OR+1120000000004695_9/1)";
            cluster.backend().add(cleanupUri4, StaticHttpResource.OK);
            //no commit position reqie, we handled last mid alone

            String slowSrchUri1 = slowBackendSearch(cluster, uid, mid1);
            String slowSrchUri2 = slowBackendSearch(cluster, uid, "2");
            String slowSrchUri3 = slowBackendSearch(cluster, uid, "3");
            String slowSrchUri4 = slowBackendSearch(cluster, uid, "4");
            String slowSrchUri5 = slowBackendSearch(cluster, uid, mid2);
            String slowSrchUri7 = slowBackendSearch(cluster, uid, mid3);
            String slowSrchUri9 = slowBackendSearch(cluster, uid, mid4);

            cluster.kamaji().start();
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);
            Assert.assertEquals(2 << 2, cluster.blackbox().accessCount(bbUri));
            Assert.assertEquals(2, cluster.filterSearch().accessCount(fsUri1));
            Assert.assertEquals(2, cluster.filterSearch().accessCount(fsUri2));
            Assert.assertEquals(2, cluster.filterSearch().accessCount(fsUri3));
            Assert.assertEquals(2, cluster.filterSearch().accessCount(fsUri4));
            Assert.assertEquals(1, cluster.backend().accessCount(fastUri1));
            Assert.assertEquals(1, cluster.backend().accessCount(fastUri2));
            Assert.assertEquals(1, cluster.backend().accessCount(fastUri3));
            Assert.assertEquals(1, cluster.backend().accessCount(fastUri4));
            Assert.assertEquals(1, cluster.backend().accessCount(modifyUri1));
            Assert.assertEquals(1, cluster.backend().accessCount(modifyUri2));
            Assert.assertEquals(1, cluster.backend().accessCount(modifyUri3));
            Assert.assertEquals(1, cluster.backend().accessCount(modifyUri4));
            Assert.assertEquals(1, cluster.backend().accessCount(deleteUri1));
            Assert.assertEquals(1, cluster.backend().accessCount(deleteUri2));
            Assert.assertEquals(1, cluster.backend().accessCount(deleteUri3));
            Assert.assertEquals(1, cluster.backend().accessCount(deleteUri4));
            Assert.assertEquals(1, cluster.backend().accessCount(cleanupUri1));
            Assert.assertEquals(1, cluster.backend().accessCount(cleanupUri2));
            Assert.assertEquals(1, cluster.backend().accessCount(cleanupUri3));
            Assert.assertEquals(1, cluster.backend().accessCount(cleanupUri4));
            Assert.assertEquals(2, cluster.backend().accessCount(slowSrchUri1));
            Assert.assertEquals(1, cluster.backend().accessCount(slowSrchUri2));
            Assert.assertEquals(1, cluster.backend().accessCount(slowSrchUri3));
            Assert.assertEquals(1, cluster.backend().accessCount(slowSrchUri4));
            Assert.assertEquals(2, cluster.backend().accessCount(slowSrchUri5));
            Assert.assertEquals(2, cluster.backend().accessCount(slowSrchUri7));
            Assert.assertEquals(2, cluster.backend().accessCount(slowSrchUri9));
        }
    }

    @Test
    public void testUpdateAttach() throws Exception {
        try (KamajiCluster cluster = new KamajiCluster();
            CloseableHttpClient client =
                HttpClients.custom().setRedirectStrategy(
                    new PostsRedirectStrategy()).build())
        {
            final long uid = 9001L;
            String lcn = "25";
            String mid1 = "10";
            String mid2 = "21";
            String mid3 = "22";
            String zooQueue = "queue_name";
            String queueId = "200";
            HttpPost post = new HttpPost(
                HTTP_LOCALHOST + cluster.kamaji().port()
                    + NOTIFY + QUEUE_ID_P + queueId);
            post.addHeader(YandexHeaders.ZOO_QUEUE, zooQueue);
            post.addHeader(YandexHeaders.ZOO_QUEUE_ID, queueId);
            post.addHeader(YandexHeaders.ZOO_SHARD_ID, Long.toString(uid));
            post.setEntity(
                new StringEntity(
                    "{\"operation_id\": 100,"
                    + "\"uid\": \"9001\","
                    + "\"lcn\": \"25\","
                    + "\"change_type\": \"update-attach\","
                    + "\"operation_date\": \"1234567899\","
                    + "\"changed\": [{\"mid\":10},{\"mid\":215},"
                    + "{\"mid\":117},{\"mid\":319},{\"mid\":21},{\"mid\":22}],"
                    + "\"fresh_count\": \"10\","
                    + "\"useful_new_messages\": \"11\"}",
                    ContentType.APPLICATION_JSON));
            String bbUri = blackboxUri(UID_PARAM + uid);
            cluster.blackbox().add(
                bbUri,
                blackboxResponse(uid, "up@at"));
            String prefix =
                FILTER_SEARCH
                + "9001&mdb=pg&suid=90000&lcn=25&operation-id=100";
            String fsUri1 = prefix + "&mids=10&mids=215";
            String fsUri2 = prefix + "&mids=117&mids=319";
            String fsUri3 = prefix + "&mids=21&mids=22";
            String commonFields =
                ",\"fid\":11,\"receiveDate\":1234567890, \"threadId\":12,"
                + "\"rfcId\":13, \"folder\":{\"type\":{\"title\":\"user\"},"
                + " \"name\":\"folder\"}";
            cluster.filterSearch().add(
                fsUri1,
                "{\"envelopes\":[{\"mid\":10,\"stid\":\"1.10.10\""
                + commonFields + JSON_END);
            cluster.filterSearch().add(fsUri2, EMPTY_ENVELOPES);
            cluster.filterSearch().add(
                fsUri3,
                "{\"envelopes\":[{\"mid\":21,\"stid\":\"1.21.21\""
                + commonFields + "},{\"mid\":22,\"stid\":\"1.22.22\""
                + commonFields + JSON_END);

            String fastUri1 = MODIFY + uid + "&fast&mid=10";
            List<String> preserveFields
                = new ArrayList<>(cluster.kamaji().config().preserveFields());

            Map<String, Object> fastDocComplete =
                fastDocComplete(uid, mid1, lcn, queueId);

            fastDocComplete.put(PRESERVE_FIELDS, preserveFields);
            cluster.backend().add(
                fastUri1,
                new ExpectingHeaderHttpItem(
                    new ExpectingHttpItem(new JsonChecker(fastDocComplete)),
                    new BasicHeader(
                        YandexHeaders.ZOO_QUEUE,
                        zooQueue),
                    new BasicHeader(
                        YandexHeaders.ZOO_QUEUE_ID,
                        null),
                    new BasicHeader(
                        YandexHeaders.ZOO_QUEUE_ID_TO_CHECK,
                        queueId),
                    new BasicHeader(
                        YandexHeaders.ZOO_SHARD_ID,
                        Long.toString(uid))));

            String fastUri2 = MODIFY + uid + "&fast&mid=21";
            fastDocComplete = fastDocComplete(uid, mid2, lcn, queueId);
            fastDocComplete.put(PRESERVE_FIELDS, preserveFields);

            cluster.backend().add(
                fastUri2,
                new ExpectingHeaderHttpItem(
                    new ExpectingHttpItem(new JsonChecker(fastDocComplete)),
                    new BasicHeader(
                        YandexHeaders.ZOO_QUEUE,
                        zooQueue),
                    new BasicHeader(
                        YandexHeaders.ZOO_QUEUE_ID,
                        null),
                    new BasicHeader(
                        YandexHeaders.ZOO_QUEUE_ID_TO_CHECK,
                        queueId),
                    new BasicHeader(
                        YandexHeaders.ZOO_SHARD_ID,
                        Long.toString(uid))));

            String fastUri3 = MODIFY + uid + "&fast&mid=22";
            fastDocComplete = fastDocComplete(uid, mid3, lcn, queueId);
            fastDocComplete.put(PRESERVE_FIELDS, preserveFields);

            cluster.backend().add(
                fastUri3,
                new ExpectingHeaderHttpItem(
                    new ExpectingHttpItem(new JsonChecker(fastDocComplete)),
                    new BasicHeader(
                        YandexHeaders.ZOO_QUEUE,
                        zooQueue),
                    new BasicHeader(
                        YandexHeaders.ZOO_QUEUE_ID,
                        null),
                    new BasicHeader(
                        YandexHeaders.ZOO_QUEUE_ID_TO_CHECK,
                        queueId),
                    new BasicHeader(
                        YandexHeaders.ZOO_SHARD_ID,
                        Long.toString(uid))));

            String tikaiteUri1 = "/mail/handler?json-type=dollar&stid=1.10.10";
            cluster.tikaite().add(tikaiteUri1, HttpStatus.SC_NOT_FOUND);
            String tikaiteUri2 = "/mail/handler?json-type=dollar&stid=1.21.21";
            cluster.tikaite().add(
                tikaiteUri2,
                "{\"docs\":[{\"hid\":\"1\",\"body_text\":\"hello21\"}]}");
            String tikaiteUri3 = "/mail/handler?json-type=dollar&stid=1.22.22";
            cluster.tikaite().add(
                tikaiteUri3,
                "{\"docs\":[{\"hid\":\"1\",\"body_text\":\"hello22\"}]}");

            Map<String, Object> doc =
                new HashMap<>(fastDoc(uid, mid1, lcn, queueId));

            doc.put(MailIndexFields.TIKAITE_ERROR, JsonChecker.ANY_VALUE);
            doc.put(MailIndexFields.HID, Integer.toString(0));
            doc.put(MailIndexFields.MID, mid1);
            doc.put(
                MailIndexFields.URL,
                Long.toString(uid) + '_' + mid1 + '/' + 0);
            Map<String, Object> docComplete =
                docComplete(cluster.kamaji().config());
            docComplete.put(PREFIX, uid);
            docComplete.put(DOCS, Collections.singletonList(doc));
            String modifyUri1 = MODIFY + uid + MID + mid1;

            cluster.backend().add(
                modifyUri1,
                new StaticHttpResource(
                    new ExpectingHttpItem(new JsonChecker(docComplete))));

            doc = new HashMap<>(fastDoc(uid, mid2, lcn, queueId));
            doc.put(MailIndexFields.BODY_TEXT, "hello21");
            doc.put(MailIndexFields.HID, Integer.toString(1));
            doc.put(MailIndexFields.MID, mid2);
            doc.put(
                MailIndexFields.URL,
                Long.toString(uid) + '_' + mid2 + '/' + 1);
            docComplete = docComplete(cluster.kamaji().config());
            docComplete.put(PREFIX, uid);
            docComplete.put(
                DOCS,
                Arrays.asList(fastDoc(uid, mid2, lcn, queueId), doc));
            String modifyUri2 = MODIFY + uid + MID + mid2;
            cluster.backend().add(
                modifyUri2,
                new StaticHttpResource(
                    new ExpectingHttpItem(new JsonChecker(docComplete))));

            doc = new HashMap<>(fastDoc(uid, mid3, lcn, queueId));
            doc.put(MailIndexFields.BODY_TEXT, "hello22");
            doc.put(MailIndexFields.HID, Integer.toString(1));
            doc.put(MailIndexFields.MID, mid3);
            doc.put(
                MailIndexFields.URL,
                Long.toString(uid) + '_' + mid3 + '/' + 1);
            docComplete = docComplete(cluster.kamaji().config());
            docComplete.put(PREFIX, uid);
            docComplete.put(
                DOCS,
                Arrays.asList(fastDoc(uid, mid3, lcn, queueId), doc));
            String modifyUri3 = MODIFY + uid + MID + mid3;
            cluster.backend().add(
                modifyUri3,
                new StaticHttpResource(
                    new ExpectingHttpItem(new JsonChecker(docComplete))));

            String slowSrchUri1 = slowBackendSearch(cluster, uid, mid1);
            String slowSrchUri2 = slowBackendSearch(cluster, uid, mid2);
            String slowSrchUri3 = slowBackendSearch(cluster, uid, mid3);
            String slowSrchUri4 = slowBackendSearch(cluster, uid, "215");
            String slowSrchUri5 = slowBackendSearch(cluster, uid, "117");
            String slowSrchUri6 = slowBackendSearch(cluster, uid, "319");

            String commitUri = "/delete?commit&uid=9001&operation-id=100";
            cluster.backend().add(
                commitUri,
                new StaticHttpResource(
                    new ExpectingHeaderHttpItem(
                        new ExpectingHttpItem(
                            new JsonChecker("{\"prefix\":9001,\"docs\":[]}")),
                        new BasicHeader(YandexHeaders.ZOO_QUEUE, zooQueue),
                        new BasicHeader(YandexHeaders.ZOO_QUEUE_ID, queueId),
                        new BasicHeader(
                            YandexHeaders.ZOO_SHARD_ID,
                            Long.toString(uid)))));

            String deleteUri1 =
                "/delete?prefix=9001&text=mid_p:10+AND+NOT+url:(9001_10/0)";
            cluster.backend().add(deleteUri1, HttpStatus.SC_OK);
            String deleteUri2 =
                "/delete?prefix=9001&text=mid_p:21"
                + "+AND+NOT+url:(9001_21/0+OR+9001_21/1)";
            cluster.backend().add(deleteUri2, HttpStatus.SC_OK);
            String deleteUri3 =
                "/delete?prefix=9001&text=mid_p:22"
                + "+AND+NOT+url:(9001_22/0+OR+9001_22/1)";
            cluster.backend().add(deleteUri3, HttpStatus.SC_OK);

            cluster.kamaji().start();
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);
            Assert.assertEquals(
                2 + 2 + 2,
                cluster.blackbox().accessCount(bbUri));
            Assert.assertEquals(2, cluster.filterSearch().accessCount(fsUri1));
            Assert.assertEquals(2, cluster.filterSearch().accessCount(fsUri2));
            Assert.assertEquals(2, cluster.filterSearch().accessCount(fsUri3));
            Assert.assertEquals(1, cluster.backend().accessCount(fastUri1));
            Assert.assertEquals(1, cluster.backend().accessCount(fastUri2));
            Assert.assertEquals(1, cluster.backend().accessCount(fastUri3));
            Assert.assertEquals(1, cluster.backend().accessCount(modifyUri1));
            Assert.assertEquals(1, cluster.backend().accessCount(modifyUri2));
            Assert.assertEquals(1, cluster.backend().accessCount(modifyUri3));
            Assert.assertEquals(1, cluster.backend().accessCount(deleteUri1));
            Assert.assertEquals(1, cluster.backend().accessCount(deleteUri2));
            Assert.assertEquals(1, cluster.backend().accessCount(deleteUri3));
            Assert.assertEquals(1, cluster.backend().accessCount(commitUri));
            Assert.assertEquals(2, cluster.backend().accessCount(slowSrchUri1));
            Assert.assertEquals(2, cluster.backend().accessCount(slowSrchUri2));
            Assert.assertEquals(2, cluster.backend().accessCount(slowSrchUri3));
            Assert.assertEquals(1, cluster.backend().accessCount(slowSrchUri4));
            Assert.assertEquals(1, cluster.backend().accessCount(slowSrchUri5));
            Assert.assertEquals(1, cluster.backend().accessCount(slowSrchUri6));
        }
    }
    // CSON: MethodLength

    @Test
    public void testGone() throws Exception {
        try (KamajiCluster cluster = new KamajiCluster();
            CloseableHttpClient client = HttpClients.createDefault())
        {
            List<String> lines = Files.readAllLines(
                new File(getClass().getResource(STORE_ENVELOPE).toURI())
                    .toPath(),
                StandardCharsets.UTF_8);
            StringBuilder sb = new StringBuilder();
            for (String line: lines) {
                sb.append(line);
                sb.append('\n');
            }
            final long uid = 203889311L;
            String to = "analizer@yandex.ru";
            String bbUri = blackboxUri(UID_PARAM + uid);
            cluster.blackbox().add(
                bbUri,
                blackboxResponse(uid, 0L, to));
            cluster.kamaji().start();
            HttpPost post = new HttpPost(
                HTTP_LOCALHOST + cluster.kamaji().port() + NOTIFY);
            post.setEntity(
                new StringEntity(
                    new String(sb).replace("\"store\"", "\"update\""),
                    ContentType.APPLICATION_JSON));
            HttpAssert.assertStatusCode(HttpStatus.SC_GONE, client, post);
        }
    }

    // CSOFF: MethodLength
    @Test
    public void testUpdateFields() throws Exception {
        try (KamajiCluster cluster = new KamajiCluster();
             CloseableHttpClient client =
                 HttpClients.custom().setRedirectStrategy(
                     new PostsRedirectStrategy()).build())
        {
            final long uid = 1220000000004695L;
            String lcn = "150";
            String mid1 = "41";
            String mid2 = "20";
            String queueIdFast = "111";
            String queueIdSlow = "555";
            HttpPost post = new HttpPost(
                HTTP_LOCALHOST + cluster.kamaji().port()
                    + NOTIFY + QUEUE_ID_P + queueIdSlow);
            post.addHeader(YandexHeaders.ZOO_QUEUE, CHANGE_LOG_QUEUE);
            post.addHeader(YandexHeaders.ZOO_QUEUE_ID, queueIdFast);
            post.addHeader(YandexHeaders.ZOO_SHARD_ID, Long.toString(uid));
            post.setEntity(
                new StringEntity(
                    "{\"uid\": \"1220000000004695\","
                        + "\"change_type\": \"fields_update\""
                        + ",\"operation_date\": \"1234567890\","
                        + "\"operation_id\": \"1221\","
                        + "\"changed\": [{\"mid\":41, \"clicks_total_count\": "
                        + "{\"function\": \"inc\"}},"
                        + "{\"mid\":20, \"clicks_total_count\":"
                        + "{\"function\": \"sum\", "
                        + "\"args\":[1,{\"function\":\"ident\"}]}},"
                        + "{\"mid\":50}]"
                        + ",\"fresh_count\": \"0\""
                        + ",\"useful_new_messages\": \"1\""
                        + ",\"pgshard\": \"2\"}",
                    ContentType.APPLICATION_JSON));

            String bbUri = blackboxUri(UID_PARAM + uid);
            cluster.blackbox().add(
                bbUri,
                blackboxResponse(uid, "down@dt"));
            String common =
                uid + "&mdb=pg&suid=90000&lcn=-1&zoo-queue-id=11"
                + "1&operation-id=1221&pgshard=2";
            String commonSlow =
                uid + "&mdb=pg&suid=90000&lcn=-1&zoo-queue-id=55"
                + "5&operation-id=1221&pgshard=2";
            String fsUri1 = FILTER_SEARCH + common + "&mids=41&mids=20";
            String fsUri1Slow =
                FILTER_SEARCH + commonSlow + "&mids=41" + "&mids=20";
            String fsUri2 = FILTER_SEARCH + common + "&mids=50";
            String fsUri2Slow = FILTER_SEARCH + commonSlow + "&mids=5" + 0;
            String commonFields =
                ",\"fid\":11,\"receiveDate\":1234567890,\"threadId\":12"
                    + ",\"rfcId\":13,\"folder\":{\"type\":{\"title\":\"user\"}"
                    + ",\"name\":\"folder\"}";
            String fs1Body =
                "{\"envelopes\":[{\"mid\":41,\"stid\":\"1.41.41\""
                + commonFields + "},{\"mid\":20,\"stid\":\"1.20.20\""
                + commonFields + ",\"labels\":[]}]}";
            cluster.filterSearch().add(fsUri1, fs1Body);
            cluster.filterSearch().add(fsUri1Slow, fs1Body);
            cluster.filterSearch().add(fsUri2, EMPTY_ENVELOPES);
            cluster.filterSearch().add(fsUri2Slow, EMPTY_ENVELOPES);

            String fastUri1 = MODIFY + uid + "&fast&mid=41";
            Map<String, Object> fst1 = new HashMap<>();

            fst1.put(ADD_IF_NOT_EXISTS, true);
            List<String> preserveFields =
                new ArrayList<>(cluster.kamaji().config().preserveFields());
            preserveFields.add(OracleFields.LCN);
            fst1.put(PRESERVE_FIELDS, preserveFields);
            fst1.put(PREFIX, uid);
            Map<String, Object> fstDoc1 =
                new HashMap<>(fastDoc(uid, mid1, lcn, queueIdFast));
            fstDoc1.remove(OracleFields.LCN);

            final String function = "function";
            final String counterName = "clicks_total_count";
            Map<String, Object> counter = new HashMap<>();
            counter.put(function, "inc");
            fstDoc1.put(counterName, counter);
            fst1.put(DOCS, Collections.singletonList(fstDoc1));

            cluster.backend().add(
                fastUri1,
                new ExpectingHeaderHttpItem(
                    new ExpectingHttpItem(new JsonChecker(fst1)),
                    new BasicHeader(
                        YandexHeaders.ZOO_QUEUE,
                        CHANGE_LOG_QUEUE),
                    new BasicHeader(
                        YandexHeaders.ZOO_QUEUE_ID,
                        null),
                    new BasicHeader(
                        YandexHeaders.ZOO_QUEUE_ID_TO_CHECK,
                        queueIdFast),
                    new BasicHeader(
                        YandexHeaders.ZOO_SHARD_ID,
                        Long.toString(uid))));

            String fastUri2 = MODIFY + uid + "&fast&mid=20";
            Map<String, Object> fst2 = new HashMap<>();

            fst2.put(ADD_IF_NOT_EXISTS, true);
            fst2.put(PRESERVE_FIELDS, preserveFields);
            fst2.put(PREFIX, uid);
            Map<String, Object> fstDoc2 =
                new HashMap<>(fastDoc(uid, mid2, lcn, queueIdFast));
            fstDoc2.remove(OracleFields.LCN);

            counter = new HashMap<>();
            counter.put(function, "sum");
            List<Object> args = new ArrayList<>();
            args.add(1L);
            args.add(Collections.singletonMap(function, "ident"));
            counter.put("args", args);
            fstDoc2.put(counterName, counter);
            fst2.put(DOCS, Collections.singletonList(fstDoc2));
            cluster.backend().add(
                fastUri2,
                new ExpectingHeaderHttpItem(
                    new ExpectingHttpItem(new JsonChecker(fst2)),
                    new BasicHeader(
                        YandexHeaders.ZOO_QUEUE,
                        CHANGE_LOG_QUEUE),
                    new BasicHeader(
                        YandexHeaders.ZOO_QUEUE_ID,
                        null),
                    new BasicHeader(
                        YandexHeaders.ZOO_QUEUE_ID_TO_CHECK,
                        queueIdFast),
                    new BasicHeader(
                        YandexHeaders.ZOO_SHARD_ID,
                        Long.toString(uid))));

            String tikaiteUri1 = "/mail/handler?json-type=dollar&stid=1.41.41";
            cluster.tikaite().add(
                tikaiteUri1,
                "{\"docs\":[{\"hid\":\"1\",\"body_text\":\"hello10\"}]}");
            String tikaiteUri2 = "/mail/handler?json-type=dollar&stid=1.20.20";
            cluster.tikaite().add(
                tikaiteUri2,
                "{\"docs\":[{\"hid\":\"1\",\"body_text\":\"hello20\"}]}");

            Map<String, Object> doc =
                new HashMap<>(fastDoc(uid, mid1, lcn, queueIdSlow));
            doc.remove(OracleFields.LCN);
            doc.put(MailIndexFields.BODY_TEXT, "hello10");
            doc.put(MailIndexFields.HID, Integer.toString(1));
            doc.put(MailIndexFields.MID, mid1);
            doc.put(
                MailIndexFields.URL,
                Long.toString(uid) + '_' + mid1 + '/' + 1);

            Map<String, Object> docComplete = new HashMap<>();
            docComplete.put(PRESERVE_FIELDS, preserveFields);
            docComplete.put(ADD_IF_NOT_EXISTS, true);
            Map<?, ?> fstDoc = new HashMap<>(
                fastDoc(uid, mid1, lcn, queueIdSlow));
            fstDoc.remove(OracleFields.LCN);
            docComplete.put(PREFIX, uid);
            docComplete.put(
                DOCS,
                Arrays.asList(fstDoc, doc));
            String modifyUri1 = MODIFY + uid + MID + mid1;

            cluster.backend().add(
                modifyUri1,
                new StaticHttpResource(
                    new ExpectingHeaderHttpItem(
                        new ExpectingHttpItem(new JsonChecker(docComplete)),
                        new BasicHeader(YandexHeaders.ZOO_QUEUE_ID, null),
                        new BasicHeader(
                            YandexHeaders.ZOO_QUEUE_ID_TO_CHECK,
                            null))));

            doc = new HashMap<>(fastDoc(uid, mid2, lcn, queueIdSlow));
            doc.put(MailIndexFields.BODY_TEXT, "hello20");
            doc.remove(OracleFields.LCN);

            doc.put(MailIndexFields.HID, Integer.toString(1));
            doc.put(MailIndexFields.MID, mid2);
            doc.put(
                MailIndexFields.URL,
                Long.toString(uid) + '_' + mid2 + '/' + 1);
            docComplete = new HashMap<>();
            docComplete.put(PRESERVE_FIELDS, preserveFields);
            docComplete.put(ADD_IF_NOT_EXISTS, true);
            docComplete.put(PREFIX, uid);

            fstDoc = new HashMap<>(fastDoc(uid, mid2, lcn, queueIdSlow));
            fstDoc.remove(OracleFields.LCN);

            docComplete.put(
                DOCS,
                Arrays.asList(fstDoc, doc));
            String modifyUri2 = MODIFY + uid + MID + mid2;
            cluster.backend().add(
                modifyUri2,
                new StaticHttpResource(
                    new ExpectingHeaderHttpItem(
                        new ExpectingHttpItem(new JsonChecker(docComplete)),
                        new BasicHeader(YandexHeaders.ZOO_QUEUE_ID, null),
                        new BasicHeader(
                            YandexHeaders.ZOO_QUEUE_ID_TO_CHECK,
                            null))));

            String deleteUri1 =
                "/delete?prefix=1220000000004695&text=mid_p:41+AND+NOT+url:("
                + "1220000000004695_41/0+OR+1220000000004695_41/1)";
            cluster.backend().add(deleteUri1, HttpStatus.SC_OK);
            String deleteUri2 =
                "/delete?prefix=1220000000004695&text=mid_p:20+AND+NOT+url:("
                + "1220000000004695_20/0+OR+1220000000004695_20/1)";
            cluster.backend().add(deleteUri2, HttpStatus.SC_OK);

            String commitUri =
                "/delete?commit&uid=1220000000004695&operation-id=1221";
            cluster.backend().add(
                commitUri,
                new StaticHttpResource(
                    new ExpectingHttpItem(
                        new JsonChecker(
                            "{\"prefix\":1220000000004695,\"docs\":[]"
                                + '}'))));

            String slowSrchUri1 = slowBackendSearch(cluster, uid, mid1);
            String slowSrchUri2 = slowBackendSearch(cluster, uid, mid2);
            String slowSrchUri3 = slowBackendSearch(cluster, uid, "50");

            cluster.kamaji().start();
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);
            Assert.assertEquals(1, cluster.filterSearch().accessCount(fsUri1));
            Assert.assertEquals(1, cluster.filterSearch().accessCount(fsUri2));
            Assert.assertEquals(
                1,
                cluster.filterSearch().accessCount(fsUri1Slow));
            Assert.assertEquals(
                1,
                cluster.filterSearch().accessCount(fsUri2Slow));
            Assert.assertEquals(1, cluster.backend().accessCount(deleteUri1));
            Assert.assertEquals(1, cluster.backend().accessCount(deleteUri2));
            Assert.assertEquals(2, cluster.backend().accessCount(slowSrchUri1));
            Assert.assertEquals(2, cluster.backend().accessCount(slowSrchUri2));
            Assert.assertEquals(1, cluster.backend().accessCount(slowSrchUri3));
        }
    }
    // CSON: MethodLength

    @Test
    public void testLids() throws Exception {
        try (TestSearchBackend lucene = new TestMailSearchBackend(this);
            KamajiCluster cluster = new KamajiCluster(lucene);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.kamaji().start();
            final long uid = 9000L;
            final String queueId = "124124";
            cluster.blackbox().add(
                blackboxUri(UID_PARAM + uid),
                blackboxResponse(uid, "domainless"));
            cluster.filterSearch().add(
                FILTER_SEARCH + uid
                + "&mdb=pg&suid=90000&lcn=1&operation-id=2&mids=3",
                "{\"envelopes\":[{\"mid\":3,\"stid\":\"1.stid\","
                + "\"labels\":[\"14\",\"16\",\"6\",\"2\",\"FAKE_SEEN_LBL\"],"
                + "\"types\":[],\"subject\":\"\",\"fid\":1,\"threadId\":\"\","
                + "\"folder\":{\"type\":{\"title\":\"user\"},\"name\":\"fd\"},"
                + "\"receiveDate\":1234567890,\"rfcId\":10,\"to\":["
                + "{\"displayName\":\"\",\"domain\":\"\",\"local\":\"local1\"}"
                + ",{\"local\":\"local2\"}],\"labelsInfo\":{"
                + "\"FAKE_SEEN_LBL\":{\"type\":{\"title\":\"system\"},"
                + "\"symbolicName\":{\"title\":\"seen_label\"}},"
                + "\"14\":{\"type\":{\"title\":\"user\"},\"name\":\"Метко\"},"
                + "\"16\":{\"type\":{\"title\":\"social\"},\"name\":\"lbl\"},"
                + "\"6\":{\"type\":{\"title\":\"system\"},"
                + "\"name\":\"priority_high\","
                + "\"symbolicName\":{\"title\":\"important_label\"}},"
                + "\"2\":{\"type\":{\"title\":\"system\"},\"name\":\"draft\","
                + "\"symbolicName\":{\"title\":\"draft_label\"}}}}]}");
            HttpPost post = new HttpPost(
                HTTP_LOCALHOST + cluster.kamaji().port()
                    + NOTIFY + QUEUE_ID_P + queueId);
            post.addHeader(YandexHeaders.ZOO_QUEUE_ID, queueId);

            post.setEntity(
                new StringEntity(
                    "{\"lcn\":1,\"operation_id\":2,\"uid\":9000,"
                    + "\"operation_date\": \"1436810748.043103\","
                    + "\"change_type\":\"store\",\"changed\":[{\"mid\":3}]}"));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);
            Map<Object, Object> doc =
                new HashMap<>(
                    (Map<?, ?>) ValueContentHandler.parse(
                        "{\"url\":\"9000_3/0\",\"mid\":\"3\",\"fid\":\"1\","
                        + "\"folder_name\":\"fd\","
                        + "\"received_date\":\"1234567890\","
                        + "\"lids\":\"14\\n16\\n6\\n2\\nFAKE_SEEN_LBL\\n\","
                        + "\"labels_names\":\"Метко\\n\",\"msg_id\":\"10\","
                        + "\"hid\":\"0\",\"hdr_to\":\"<local1>\\n<local2>\\n\""
                        + ",\"hdr_to_normalized\":\"local1\\nlocal2\\n\","
                        + "\"folder_type\":\"user\",\"lcn\":\"1\","
                        + "\"stid\":\"1.stid\",\"thread_id\":\"\","
                        + "\"queueId\":\"" + queueId
                        + "\", \"draft\":\"true\",\"uid\":\"9000\"}"));
            doc.put(MailIndexFields.TIKAITE_ERROR, JsonChecker.ANY_VALUE);
            Map<String, Object> expected = new HashMap<>();
            expected.put("hitsCount", 1L);
            expected.put("hitsArray", Collections.singletonList(doc));
            lucene.checkSearch(
                "/search?prefix=9000&text=mid_p:3&get=*&hr",
                new JsonChecker(expected));
        }
    }

    @Test
    public void testBadMid() throws Exception {
        try (KamajiCluster cluster = new KamajiCluster()) {
            final long uid = 9003L;
            String bbUri = blackboxUri(UID_PARAM + uid);
            cluster.blackbox().add(
                bbUri,
                blackboxResponse(uid, "home@alone"));
            cluster.kamaji().start();
            HttpPost post = new HttpPost(
                HTTP_LOCALHOST + cluster.kamaji().port() + NOTIFY);
            post.addHeader(YandexHeaders.ZOO_QUEUE_ID, "65782");
            post.setEntity(
                new StringEntity(
                    "{\"operation_id\": 102,"
                    + "\"uid\": \"9003\","
                    + "\"lcn\": \"26\","
                    + "\"change_type\":\"update-attach\","
                    + "\"operation_date\": \"1234567896\","
                    + "\"changed\": [{\"mid\":\"t159033361841521587\"}],"
                    + "\"fresh_count\": \"12\","
                    + "\"useful_new_messages\": \"13\"}",
                    ContentType.APPLICATION_JSON));
            HttpAssert.assertStatusCode(HttpStatus.SC_BAD_REQUEST, post);
        }
    }

    @Test
    public void testBadFilterSearch2() throws Exception {
        try (KamajiCluster cluster = new KamajiCluster()) {
            final long uid = 9004L;
            String bbUri = blackboxUri(UID_PARAM + uid);
            cluster.blackbox().add(
                bbUri,
                blackboxResponse(uid, "home@alone2"));
            cluster.kamaji().start();
            HttpPost post = new HttpPost(
                HTTP_LOCALHOST + cluster.kamaji().port() + NOTIFY);
            post.addHeader(YandexHeaders.ZOO_QUEUE_ID, "65783");
            post.setEntity(
                new StringEntity(
                    "{\"operation_id\": 103,"
                    + "\"uid\": \"9004\","
                    + "\"lcn\": \"27\","
                    + "\"change_type\":\"store\","
                    + "\"operation_date\": \"1234567897\","
                    + "\"changed\": [{\"mid\":\"3\"}],"
                    + "\"fresh_count\": \"13\","
                    + "\"useful_new_messages\": \"14\"}",
                    ContentType.APPLICATION_JSON));
            cluster.filterSearch().add(
                FILTER_SEARCH + uid
                + "&mdb=pg&suid=90000&lcn=27&operation-id=103&mids=3",
                "{\"error\":{\"code\":1,\"message\":\"unknown error\""
                    + ",\"reason\":\"could not utfize string. charset = "
                    + "iso-8859-1; content = �I�ny�n\u001A�nz�n\u001B�\"}}");
            cluster.backend().add(
                "/search?IO_PRIO=3000&prefix=9004&text=url:usr"
                    + "type_9004_noreply@private.com&get=user_types",
                EMPTY_LUCENE_RESPONSE);
            cluster.backend().add(
                "/search?IO_PRIO=3000&prefix=9004&get=mid,queueId"
                + "&text=url:9004_3/0",
                EMPTY_LUCENE_RESPONSE);
            String commitUri =
                "/delete?commit&uid=9004&operation-id=103";
            cluster.backend().add(
                commitUri,
                new StaticHttpResource(
                    new ExpectingHttpItem(
                        new JsonChecker(
                            "{\"prefix\":9004,\"docs\":[]}"))));

            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);
        }
    }

    @Test
    public void testTikaiteProxy() throws Exception {
        String tikaDoc =
            "[{$hdr_from\000:$ВКонтакте <admin@notify.vk.com>\000,"
            + "$hdr_to_email\000:$tikaite-test@yandex.ru\000,"
            + "$hid\000:$1.1\000,$x_urls\000:$https://vk.com\n"
            + "https://vk.com/tikaite1?hash=tikaite1\n"
            + "https://vk.com/im?msgid=100500&sel=100500&mhash=4d9\n"
            + "https://vk.com/im?msgid=0&sel=0\n"
            + "https://vk.com/settings?act=notify&f=email_block&uhash"
            + "=bc66d00&utype=pm&mhash=87f\n\000"
            + ",$parsed\000:true,$mimetype\000:$text/html\000}]";
        String tikaData =
            "{$docs\000:" + tikaDoc + '}';

        try (KamajiCluster cluster =
                 new KamajiCluster(new TestMailSearchBackend(this));
             CloseableHttpClient client = HttpClients.createDefault())
        {
            String stid =
                "320.mail:191672484.E1447090:299270767025648430464675007682";

            String tikaiteUri =
                "/mail/handler?json-type=dollar&stid=320.mail:191672484."
                    + "E1447090:299270767025648430464675007682";

            cluster.tikaite().add(tikaiteUri, tikaData);

            String uriBase = cluster.kamaji().host() + "/tikaite?stid=" + stid;

            cluster.start();
            try (CloseableHttpResponse response =
                     client.execute(new HttpGet(uriBase)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                String responseStr =
                    CharsetUtils.toString(response.getEntity());
                YandexAssert.check(new JsonChecker(tikaDoc), responseStr);
            }

            uriBase += "&get=";
            try (CloseableHttpResponse response =
                     client.execute(new HttpGet(uriBase)))
            {
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_BAD_REQUEST,
                    response);
            }

            try (CloseableHttpResponse response =
                     client.execute(new HttpGet(uriBase + '*')))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                String responseStr =
                    CharsetUtils.toString(response.getEntity());
                YandexAssert.check(new JsonChecker(tikaDoc), responseStr);
            }

            try (CloseableHttpResponse response =
                     client.execute(new HttpGet(uriBase + "*&hid=1.1")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                String responseStr =
                    CharsetUtils.toString(response.getEntity());
                YandexAssert.check(new JsonChecker(tikaDoc), responseStr);
            }

            try (CloseableHttpResponse response =
                     client.execute(new HttpGet(uriBase + "hdr_to_email")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                String responseStr =
                    CharsetUtils.toString(response.getEntity());
                YandexAssert.check(
                    new JsonChecker(
                        "[{\"hdr_to_email\": \"tikaite-test@yandex.ru\"}]"),
                    responseStr);
            }

            try (CloseableHttpResponse response =
                     client.execute(new HttpGet(uriBase + "hdr_no_email")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                String responseStr =
                    CharsetUtils.toString(response.getEntity());
                YandexAssert.check(
                    new JsonChecker(
                        "[{\"hdr_no_email\": null}]"),
                    responseStr);
            }

            try (CloseableHttpResponse response =
                     client.execute(
                         new HttpGet(uriBase + "hdr_to_email,invalid")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                String responseStr =
                    CharsetUtils.toString(response.getEntity());
                YandexAssert.check(
                    new JsonChecker(
                        "[{\"hdr_to_email\": \"tikaite-test@yandex.ru\","
                            + " \"invalid\":null}]"),
                    responseStr);
            }
        }
    }

    // CSOFF: MultipleStringLiterals
    // CSOFF: MagicNumber
    @Test
    public void testMoveToTab() throws Exception {
        String tikaiteUri =
                "/mail/handler?json-type=dollar&stid=320.mail:227356512.E1063312"
                        + ":2723667442102777291933486789070";
        String fsResponse =
                "{\"envelopes\":[{\"rfcId"
                        + "\":\"7a29ea2e4a5c4d03a8cf1a1b892dad7a\",\"newCount\":0,"
                        + "\"size\":30317,\"extraData\":\"ivan.dudinov@yandex.ru\","
                        + "\"stid\":\"320.mail:227356512.E1063312"
                        + ":2723667442102777291933486789070\","
                        + "\"attachmentsCount\":0,\"attachmentsFullSize\":0,"
                        + "\"specialLabels\":[],\"date\":1568665450,"
                        + "\"replyTo\":[{\"domain\":\"yango.yandex.com\","
                        + "\"local\":\"no-reply\",\"displayName\":\"no-reply@yango"
                        + ".yandex.com\"}],\"fid\":\"1\",\"firstline\":\""
                        + "Mon tajet a la maison\",\"mid\":\"170292360909969419\","
                        + "\"threadId\":\"170292360909969419\",\"ImapModSeq\":\"\","
                        + "\"uidl\":\"\",\"specialLabelsInfo\":{},"
                        + "\"tab\":\"relevant\",\"bcc\":[],\"messagesCount\":18203,"
                        + "\"to\":[{\"domain\":\"yandex.ru\",\"local\":\"ivan"
                        + ".dudinov\",\"displayName\":\"ivan.dudinov@yandex.ru\"}],"
                        + "\"imapId\":\"19285\",\"types\":[7,56,65,103],"
                        + "\"labelsInfo\":{\"80\":{\"color\":\"\",\"isUser\":false,"
                        + "\"creationTime\":\"1468568152\",\"messagesCount\":2267,"
                        + "\"name\":\"7\",\"isSystem\":false,\"type\":{\"code\":9,"
                        + "\"title\":\"so\"},\"symbolicName\":{\"code\":0,"
                        + "\"title\":\"\"}},\"97\":{\"color\":\"\",\"isUser\":false,"
                        + "\"creationTime\":\"1481964840\",\"messagesCount\":540,"
                        + "\"name\":\"56\",\"isSystem\":false,\"type\":{\"code\":9,"
                        + "\"title\":\"so\"},\"symbolicName\":{\"code\":0,"
                        + "\"title\":\"\"}},\"121\":{\"color\":\"\",\"isUser\":false,"
                        + "\"creationTime\":\"1506607913\",\"messagesCount\":3601,"
                        + "\"name\":\"65\",\"isSystem\":false,\"type\":{\"code\":9,"
                        + "\"title\":\"so\"},\"symbolicName\":{\"code\":0,"
                        + "\"title\":\"\"}},\"165\":{\"color\":\"\",\"isUser\":false,"
                        + "\"creationTime\":\"1538488492\",\"messagesCount\":647,"
                        + "\"name\":\"103\",\"isSystem\":false,\"type\":{\"code\":9,"
                        + "\"title\":\"so\"},\"symbolicName\":{\"code\":0,"
                        + "\"title\":\"\"}},\"FAKE_SEEN_LBL\":{\"color\":\"\","
                        + "\"isUser\":false,\"creationTime\":\"\","
                        + "\"messagesCount\":0,\"name\":\"FAKE_SEEN_LBL\","
                        + "\"isSystem\":true,\"type\":{\"code\":3,"
                        + "\"title\":\"system\"},\"symbolicName\":{\"code\":23,"
                        + "\"title\":\"seen_label\"}},"
                        + "\"FAKE_RECENT_LBL\":{\"color\":\"\",\"isUser\":false,"
                        + "\"creationTime\":\"\",\"messagesCount\":0,"
                        + "\"name\":\"FAKE_RECENT_LBL\",\"isSystem\":true,"
                        + "\"type\":{\"code\":3,\"title\":\"system\"},"
                        + "\"symbolicName\":{\"code\":13,\"title\":\"recent_label\"}}},"
                        + "\"folder\":{\"isThreadable\":true,\"subscribed\":\"\","
                        + "\"parentId\":\"0\",\"position\":0,\"bytes\":2082173617,"
                        + "\"pop3On\":\"0\",\"subscribedForSharedFolder\":false,"
                        + "\"isUser\":false,\"creationTime\":\"1379749025\","
                        + "\"folderOptions\":{\"getPosition\":0},"
                        + "\"newMessagesCount\":6078,\"name\":\"Inbox\","
                        + "\"isSystem\":true,\"recentMessagesCount\":17801,"
                        + "\"unvisited\":true,\"type\":{\"code\":3,"
                        + "\"title\":\"system\"},\"scn\":\"31858\",\"shared\":\"0\","
                        + "\"revision\":31858,\"symbolicName\":{\"code\":1,"
                        + "\"title\":\"inbox\"}},\"inReplyTo\":\"\","
                        + "\"attachments\":[],\"hdrLastStatus\":\"\","
                        + "\"hdrStatus\":\"\",\"receiveDate\":\"1568665455000\","
                        + "\"from\":[{\"domain\":\"yango.yandex.com\","
                        + "\"local\":\"no-reply\",\"displayName\":\"Yango\"}],"
                        + "\"cc\":[],\"threadCount\":0,"
                        + "\"subjectInfo\":{\"isSplitted\":true,\"prefix\":\"\","
                        + "\"postfix\":\"\",\"subject\":\"Yandex.Taxi – rapport "
                        + "sur le trajet 16 septembre 2019\",\"type\":\"\"},"
                        + "\"subject\":\"Yandex.Taxi – rapport sur le trajet 16 "
                        + "septembre 2019\",\"references\":\"\",\"revision\":31849,"
                        + "\"labels\":[\"121\",\"165\",\"80\",\"97\","
                        + "\"FAKE_RECENT_LBL\",\"FAKE_SEEN_LBL\"]}]}";
        String tikaResp =
                "{\"docs\":[{\"hdr_from_display_name\": \"Yango\\n\",\n"
                        + "\"hid\": \"1.1\",\"pure_body\": \"Service d’assistance\","
                        + "\"disposition_type\": \"inline\",\"body_text\": \"\",\n"
                        + "\"gateway_received_date\": \"1568665455\",\n"
                        + "\"smtp_id\": \"zfvXBAiflG-OFs8vaP5\","
                        + "\"hdr_to_normalized\": \"ivan-dudinov@yandex.ru\\n\",\n"
                        + "\"stid\":\"320.mail:227356512.E1063312"
                        + ":2723667442102777291933486789070\",\"received_date\":\""
                        + "1568665455.216\",\"hdr_to\": \"ivan.dudinov@yandex.ru\","
                        + "\"hdr_from_normalized\": \"no-reply@yango.yandex.com\\n\","
                        + "\"hdr_from\": \"Yango <no-reply@yango.yandex.com>\","
                        + "\"hdr_subject\": \"Yandex.Taxi – rapport sur le trajet\",\n"
                        + "\"built_date\": \"2019-09-16 15:21:13\","
                        + "\"hdr_to_email\": \"ivan.dudinov@yandex.ru\\n\","
                        + "\"meta\": \"Content-Type:text/plain; charset=UTF-8\","
                        + "\"hdr_from_email\": \"no-reply@yango.yandex.com\\n\","
                        + "\"parsed\": true,\"mimetype\": \"text/plain\"}]}";
        String changed = "{\"pgshard\":\"2083\",\"uid\":\"227356512\","
                + "\"operation_id\":\"2706868187\","
                + "\"change_type\":\"move-to-tab\",\"operation_date\":\""
                + "1568869182.080775\",\"lcn\":\"191675\",\"arguments\":"
                + "{\"tab\":null},\"changed\":[{\"recent\":true,\"lids\":[],"
                + "\"fid\":3,\"mid\":170292360909969419,\"src_fid\":3,\"src_tab\":"
                + "\"relevant\",\"deleted\":false,\"tab\":null,\"seen\":true,"
                + "\"tid\":null}],\"useful_new_messages\":\"24\","
                + "\"fresh_count\":\"1\",\"select_date\":\"1568869182.145\"}";
        final long uid = 227356512L;
        String fsUri = FILTER_SEARCH + uid + "&mdb=pg&suid=90000&lcn=191675"
                + "&operation-id=2706868187&pgshard=2083&mids=170292360909969419";

        try (TestMailSearchBackend searchBackend = new TestMailSearchBackend(this);
             KamajiCluster cluster = new KamajiCluster(searchBackend)) {
            String bbUri = blackboxUri(UID_PARAM + uid);
            cluster.blackbox().add(
                    bbUri,
                    blackboxResponse(uid, "home@alone.not"));
            cluster.kamaji().start();
            HttpPost post = new HttpPost(
                    HTTP_LOCALHOST + cluster.kamaji().port() + NOTIFY);
            post.addHeader(YandexHeaders.ZOO_QUEUE_ID, "10643281");
            post.setEntity(
                    new StringEntity(changed, ContentType.APPLICATION_JSON));
            cluster.filterSearch().add(fsUri, fsResponse);

            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);
            searchBackend.checkSearch(
                    "/search?prefix=227356512&text="
                            + "mid_p:170292360909969419&get=url,tab",
                    new JsonChecker(
                            "{\"hitsCount\": 1,\"hitsArray\": [{\"tab\": "
                                    + "\"relevant\", "
                                    + "\"url\": \"227356512_170292360909969419/0\"}]}"));
            post.setURI(URI.create(
                    HTTP_LOCALHOST + cluster.kamaji().port() + NOTIFY + "&slow"));

            cluster.tikaite().add(tikaiteUri, tikaResp);
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);
            searchBackend.checkSearch(
                    "/search?prefix=227356512&text=mid_p:170292360909969419"
                            + "&get=url,tab&sort=hid",
                    new JsonChecker(
                            "{\"hitsCount\": 2,\"hitsArray\": ["
                                    + "{\"tab\": \"relevant\", \"url\":"
                                    + "\"227356512_170292360909969419/1.1\"},"
                                    + "{\"tab\": \"relevant\", \"url\":"
                                    + "\"227356512_170292360909969419/0\"}]}"));
        }
    }
    // CSON: MultipleStringLiterals
    // CSON: MagicNumber

    @Test
    public void testStickers() throws Exception {
        testStickersBase("sticker-create");
        testStickersBase("sticker-remove");
    }

    private void testStickersBase(String changeType) throws Exception {
        try (KamajiCluster cluster = new KamajiCluster();
             CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpPost post = new HttpPost(
                    HTTP_LOCALHOST + cluster.kamaji().port() + NOTIFY);
            post.setEntity(
                    new StringEntity(
                            "{\"operation_id\": \"160015\","
                                    + "\"uid\": \"9000\",\"lcn\": \"22\","
                                    + "\"change_type\": \"" + changeType + "\","
                                    + "\"operation_date\": \"1436810748.043102\","
                                    + "\"changed\": [{\"mid\":4},{\"mid\":8}]}",
                            ContentType.APPLICATION_JSON));
            cluster.kamaji().start();
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);
        }
    }
}

