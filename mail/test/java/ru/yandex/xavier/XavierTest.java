package ru.yandex.xavier;

import java.util.Locale;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.dbfields.ChangeType;
import ru.yandex.http.test.ExpectingHttpItem;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.test.StaticHttpItem;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.TestBase;

public class XavierTest extends TestBase {
    private static final double OPERATIOND_DATE =
        System.currentTimeMillis() / 1000.0 - 20;
    private static final long DEADLINE =
        System.currentTimeMillis() + 2000;

    private static final String DEFAULT_OP_ID = "171707289";
    private static final String DEFAULT_UID = "1130000024905389";
    private static final String DEFAULT_QUEUE_ID = "4180692";
    private static final int DEFAULT_INBOX_FID = 1;
    private static final String XIVA_UPDATE_EVENT = "msearch_status_change";
    private static final String XIVA_INSERT_EVENT = "msearch_insert";

    private static final String WEB_XIVA_CLIENT = "morda";
    private static final String PEOPLE = "people";
    private static final String SOCIAL = "social";

    // CSOFF: ParameterNumber
    // CSOFF: MultipleStringLiterals
    protected static String notifyUri(
        final String opId,
        final double opDate,
        final String uid,
        final ChangeType changeType,
        final int changedSize,
        final String zooQueueId,
        final long deadline)
    {
        return "/notify?mdb=pg&pgshard=2354&operation-id=" + opId
            + "&operation-date=" + opDate
            + "&uid=" + uid + "&change-type="
            + changeType.toString().toLowerCase(Locale.ROOT)
            + "&changed-size="
            + changedSize + "&salo-worker=pg2354:15&transfer-timestamp="
            + opDate + "&zoo-queue-id=" + zooQueueId + "&deadline="
            + deadline;
    }

    protected static String notifyUri(
        final ChangeType changeType,
        final int changedSize,
        final String queueId)
    {
        return notifyUri(
            DEFAULT_OP_ID,
            OPERATIOND_DATE,
            DEFAULT_UID,
            changeType,
            changedSize,
            queueId,
            DEADLINE);
    }

    protected static String notifyUri(final ChangeType changeType) {
        return notifyUri(changeType, 1, DEFAULT_QUEUE_ID);
    }

    protected static String notifyBody(
        final String opId,
        final double opDate,
        final String uid,
        final ChangeType changeType,
        final String lcn,
        final String... changed)
    {
        StringBuilder result =
            new StringBuilder(
                "{\"uid\":\"" + uid + "\",\"lcn\":\"" + lcn + "\",\"useful_new_"
                    + "messages\":\"" + changed.length + "\",\"select_date\":\""
                    + opDate + "\",\"fresh_count\":\"23\",\"pgshard\":\"2354\","
                    + "\"operation_id\":\"" + opId
                    + "\",\"operation_date\":\"" + opDate
                    + "\",\"change_type\":\""
                    + changeType.toString().toLowerCase(Locale.ROOT)
                    + "\",\"changed\":[");

        for (String item: changed) {
            result.append(item);
            result.append(',');
        }

        result.setLength(result.length() - 1);
        result.append("]}");
        return result.toString();
    }

    protected static String notifyBody(
        final ChangeType changeType,
        final String lcn,
        final String... changed)
    {
        return notifyBody(
            DEFAULT_OP_ID,
            OPERATIOND_DATE,
            DEFAULT_UID,
            changeType,
            lcn,
            changed);
    }

    protected static String notifyItem(
        final String mid,
        final boolean seen,
        final int fid,
        final String lids)
    {
        return "{\"deleted\":false,\"mid\":" + mid + ",\"seen\":" + seen
            + ",\"fid\":" + fid + ",\"recent\":false,"
            + "\"lids\":[" + lids + "],\"tid\":null}";
    }

    protected static String statusUri(final String uid) {
        return "/_status?service=change_log&prefix="
            + uid
            + "&all&json-type=dollar";
    }

    protected static String xivaListItem(
        final String client,
        final String extra,
        final String platform)
    {
        return "{\"client\" : \"" + client + "\",\"extra\":\"" + extra
            + "\",\"filter\":\"\",\"id\":\"8b4cfa894476c5ea488e18edb03c79b4\","
            + "\"platform\": \"" + platform
            + "\",\"session\": \"8b4cfa894476c5ea488e18edb03c79b4\","
            + "\"ttl\":31536000,\"uuid\":\"8b4cfa894476c5ea488e18edb03c79b4\"}";
    }

    protected static String xivaList(final String... items) {
        StringBuilder result = new StringBuilder();
        result.append('[');
        for (String item: items) {
            result.append(item);
            result.append(',');
        }

        result.setLength(result.length() - 1);
        result.append(']');
        return result.toString();
    }

    protected static String xivaListUri(final String user) {
        return "/v2/list?service=mail&user=" + user
            + "&token=" + XavierCluster.XIVA_LIST_TOKEN;
    }

    protected static String xivaSendUri(
        final String user,
        final String event,
        final String lcn)
    {
        return "/v2/send?&ttl=0&token=" + XavierCluster.XIVA_NOTIDY_TOKEN
            + "&user=" + user + "&event=" + event + "&lcn=" + lcn;
    }

    protected static String counters(final int... counters) {
        return "\"counters\":{\"people\":{\"unread\":"
            + counters[CountersChecker.CATEGORY.PEOPLE.ordinal()]
            + "},\"social\":{\"unread\":"
            + counters[CountersChecker.CATEGORY.SOCIAL.ordinal()]
            + "},\"eshops\":{\"unread\":"
            + counters[CountersChecker.CATEGORY.ESHOPS.ordinal()]
            + "},\"trips\":{\"unread\":"
            + counters[CountersChecker.CATEGORY.TRIPS.ordinal()] + "}}";
    }

    protected static String proxyCountersUri(
        final String uid,
        final String position)
    {
        return "/api/async/mail/tabs/count?unread=true&nolaf&uid=" + uid
            + "&mdb=pg&filters=people,social,eshops,trips&position=" + position;
    }

    protected static String proxyCategoryData(
        final String mid,
        final String category)
    {
        StringBuilder result = new StringBuilder();
        result.append("{\"");
        result.append(mid);
        result.append("\":[");
        if (category != null) {
            result.append('\"');
            result.append(category);
            result.append('\"');
        }

        result.append("]}");
        return result.toString();
    }

    protected static String proxyCategoryUri(
        final String uid,
        final String mid,
        final String position)
    {
        return "/api/async/mail/tabs/category?&uid=" + uid
            + "&mdb=pg&position=" + position + "&mid=" + mid;
    }

    protected static String filterSearchUri(
        final String opId,
        final String uid,
        final String lcn,
        final String... mids)
    {
        return "/filter_search?caller=msearch&order=default"
            + "&full_folders_and_labels=1&uid=" + uid + "&lcn="
            + lcn + "&operation-id="
            + opId + "&pgshard=2354&mids="
            + String.join("&mids=", mids);
    }

    protected static String envelopes(final String... envelopes) {
        return "\"envelopes\":[" + String.join(",", envelopes) + ']';
    }

    protected static String folder(final String name, final boolean system) {
        String result = "\"name\":\"" + name
            + "\",\"isUser\":" + String.valueOf(!system)
            + ",\"isSystem\":" + String.valueOf(system);
        if (system) {
            result += ",\"type\":{\"code\":3,\"title\":\"system\"}";
        } else {
            result += ",\"type\":{\"code\":3,\"title\":\"user\"}";
        }

        result += ",\"symbolicName\":{\"code\":1,\"title\":\"";
        result += name.toLowerCase(Locale.ROOT) + "\"}";
        return result;
    }

    // CSON: ParameterNumber
    // CSON: MultipleStringLiterals

    @FunctionalInterface
    private interface EnvelopeSupplier {
        String get(final String mid, final String folder, final String labels);
    }

    // CSOFF: MethodLength
    @Test
    public void testCacheCounters() throws Exception {
        final String uid = DEFAULT_UID;
        final String queue1 = "100";
        final String lcn1 = "1100";
        final String mid1 = "164381386399095100";
        final String queue2 = "101";
        final String lcn2 = "1101";
        final String mid2 = "164381386399095101";
        final String queue3 = "102";
        final String lcn3 = "1102";
        final String mid3 = "164381386399095102";
        final String queue4 = "103";
        final String queue5 = "104";
        final String lcn5 = "1104";
        final String mid5 = "164381386399095104";

        EnvelopeSupplier evlpProvider =
            (mid, folder, labels) -> ("{\"mid\":\"MID\",\"fid\":\"1\","
                    + "\"threadId\":\"MID\",\"size\":2,"
                    + "\"subject\":\"Палмсабжект\",\"firstline\":\"Фистлайн\","
                    + "\"from\":[{\"local\":\"fist\",\"domain\":\"fist.ru\""
                    + ",\"displayName\":\"Fist\"}],\"folder\":{" + folder
                    + "},\"labels\":[" + labels
                    + "], \"to\":[{\"local\":\"palm\",\"domain\":\"palm.ru\","
                    + "\"displayName\":\"palm@palm.ru\"}]}")
                .replaceAll("MID", mid);

        String deleteData =
            "{\"uid\": \"" + uid + "\",\"lcn\":\"1103\","
                + "\"useful_new_messages\":\"99\",\"changed\":[{"
                + "\"src_fid\":2,\"mid\":164381386399095104},"
                + "{\"src_fid\":2,\"mid\":164381386399095105}],\""
                + "change_type\":\"delete\",\"select_date\":\"1514938279.404\""
                + ",\"fresh_count\":\"31\",\"pgshard\":\"14\","
                + "\"operation_id\":\"3792588327\","
                + "\"operation_date\":\"1514938279.32585\"}";

        try (XavierCluster cluster = new XavierCluster();
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();

            HttpPost storePost1 = new HttpPost(
                cluster.xavier().host().toURI()
                    + notifyUri(ChangeType.STORE, 1, queue1));
            storePost1.setEntity(
                new StringEntity(
                    notifyBody(
                        ChangeType.STORE,
                        lcn1,
                        notifyItem(mid1, false, DEFAULT_INBOX_FID, ""))));
            HttpPost storePost2 = new HttpPost(
                cluster.xavier().host().toURI()
                    + notifyUri(ChangeType.STORE, 1, queue2));
            storePost2.setEntity(
                new StringEntity(
                    notifyBody(
                        ChangeType.STORE,
                        lcn2,
                        notifyItem(mid2, false, DEFAULT_INBOX_FID, ""))));
            HttpPost storePost3 = new HttpPost(
                cluster.xavier().host().toURI()
                    + notifyUri(ChangeType.STORE, 1, queue3));
            storePost3.setEntity(
                new StringEntity(
                    notifyBody(
                        ChangeType.STORE,
                        lcn3,
                        notifyItem(mid3, true, DEFAULT_INBOX_FID, ""))));

            cluster.producer().add(
                statusUri(uid),
                new StaticHttpItem("[{\"localhost\":100}]"),
                new StaticHttpItem("[{\"localhost\":101}]"),
                new StaticHttpItem("[{\"localhost\":102}]"),
                new StaticHttpItem("[{\"localhost\": 103}]"));

            cluster.xiva().add(
                xivaListUri(uid),
                xivaList(xivaListItem(WEB_XIVA_CLIENT, "", "")));

            String categoryUri1 = proxyCategoryUri(uid, mid1, queue1);
            cluster.proxy().add(categoryUri1, proxyCategoryData(mid1, PEOPLE));
            String categoryUri2 = proxyCategoryUri(uid, mid2, queue2);
            cluster.proxy().add(categoryUri2, proxyCategoryData(mid2, SOCIAL));
            String categoryUri3 = proxyCategoryUri(uid, mid3, queue3);
            cluster.proxy().add(categoryUri3, proxyCategoryData(mid3, PEOPLE));

            cluster.proxy().add(proxyCountersUri(uid, queue1), "[1, 0, 0, 0]");

            String inbox = folder("Inbox", true);
            String notSeen = "\"FAKE_RECENT_LBL\"";
            String seen = "\"FAKE_RECENT_LBL\",\"FAKE_SEEN_LBL\"";
            cluster.filterSearch().add(
                filterSearchUri(DEFAULT_OP_ID, uid, lcn1, mid1),
                '{' + envelopes(evlpProvider.get(mid1, inbox, notSeen)) + '}');
            cluster.filterSearch().add(
                filterSearchUri(DEFAULT_OP_ID, uid, lcn2, mid2),
                '{' + envelopes(evlpProvider.get(mid2, inbox, notSeen)) + '}');
            cluster.filterSearch().add(
                filterSearchUri(DEFAULT_OP_ID, uid, lcn3, mid3),
                '{' + envelopes(evlpProvider.get(mid1, inbox, seen)) + '}');

            String xivaUri1 = xivaSendUri(uid, XIVA_INSERT_EVENT, lcn1);
            String xivaUri2 = xivaSendUri(uid, XIVA_INSERT_EVENT, lcn2);
            String xivaUri3 = xivaSendUri(uid, XIVA_INSERT_EVENT, lcn3);
            cluster.xiva().add(
                xivaUri1,
                new ExpectingHttpItem(new CountersChecker(1, 0, 0, 0)));
            cluster.xiva().add(
                xivaUri2,
                new ExpectingHttpItem(new CountersChecker(1, 1, 0, 0)));
            cluster.xiva().add(
                xivaUri3,
                new ExpectingHttpItem(new CountersChecker(1, 1, 0, 0)));

            // Ok, on start we have 0,0,0,0 everything in read state
            // than we should cache it and increase by ourselves
            // without requests to proxy
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, storePost1);
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, storePost2);
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, storePost3);
            Assert.assertEquals(1, cluster.xiva().accessCount(xivaUri1));
            Assert.assertEquals(1, cluster.xiva().accessCount(xivaUri2));
            Assert.assertEquals(1, cluster.xiva().accessCount(xivaUri3));

            cluster.producer().add(
                statusUri(uid),
                new StaticHttpItem("[{\"localhost\":103}]"),
                new StaticHttpItem("[{\"localhost\":104}]"));

            HttpPost deletePost = new HttpPost(
                cluster.xavier().host().toURI()
                    + notifyUri(ChangeType.DELETE, 2, queue4));
            deletePost.setEntity(new StringEntity(deleteData));

            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, deletePost);

            // delete should drop cache
            HttpPost storePost5 = new HttpPost(
                cluster.xavier().host().toURI()
                    + notifyUri(ChangeType.STORE, 1, queue5));
            storePost5.setEntity(
                new StringEntity(
                    notifyBody(
                        ChangeType.STORE,
                        lcn5,
                        notifyItem(mid5, false, DEFAULT_INBOX_FID, ""))));

            cluster.filterSearch().add(
                filterSearchUri(DEFAULT_OP_ID, uid, lcn5, mid5),
                '{' + envelopes(evlpProvider.get(mid5, inbox, notSeen)) + '}');

            String counterUri5 = proxyCountersUri(uid, queue5);
            cluster.proxy().add(counterUri5, "[0, 2, 0, 0]");

            String xivaUri5 = xivaSendUri(uid, XIVA_INSERT_EVENT, lcn5);
            cluster.xiva().add(
                xivaUri5,
                new ExpectingHttpItem(new CountersChecker(0, 2, 0, 0)));

            cluster.proxy().add(
                proxyCategoryUri(uid, mid5, queue5),
                proxyCategoryData(mid5, SOCIAL));

            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, storePost5);

            Assert.assertEquals(1, cluster.xiva().accessCount(xivaUri5));
            Assert.assertEquals(1, cluster.proxy().accessCount(counterUri5));
        }
    }
    // CSON: MethodLength

    @Test
    public void testStore() throws Exception {
        try (XavierCluster cluster = new XavierCluster();
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();

            final String mid = "164381386399042000";
            final String lcn = "678";
            // i didn't find any occur of FAKE_SEEN_LABEL in notify
            final String lids =
                "10,11,12,13,14,9";

            final String queueId = "10896";
            final String uid = DEFAULT_UID;

            HttpPost post = new HttpPost(
                notifyUri(ChangeType.STORE, 1, queueId));
            post.setEntity(
                new StringEntity(
                    notifyBody(
                        ChangeType.STORE,
                        lcn,
                        notifyItem(mid, false, DEFAULT_INBOX_FID, lids))));

            cluster.producer().add(
                statusUri(uid),
                new StaticHttpItem("[{\"localhost\":10894}]"),
                new StaticHttpItem("[{\"localhost\":10896}]"));

            cluster.xiva().add(
                xivaListUri(uid),
                xivaList(xivaListItem(WEB_XIVA_CLIENT, "", "")));

            String xivaNotifyUri =
                xivaSendUri(uid, XIVA_INSERT_EVENT, lcn);

            cluster.proxy().add(
                proxyCategoryUri(uid, mid, queueId),
                proxyCategoryData(mid, PEOPLE));

            cluster.proxy().add(
                proxyCountersUri(uid, queueId),
                "[1,0,0,0]");

            String envelope = "{\"mid\":\"164381386399042000\",\"fid\":\"1\","
                + "\"threadId\":\"164381386399042000\",\"size\":1,"
                + "\"subject\":\"Подтверждение\",\"firstline\":"
                + "\"Чтобы подтвердить\","
                + "\"from\":[{\"local\":\"noreply\",\"domain\":\"blizzard.com\""
                + ",\"displayName\":\"Blizzard Entertainment\"}],"
                + "\"to\":[{\"local\":\"l.bakyleva\",\"domain\":\"yandex.ru\","
                + "\"displayName\":\"l.bakyleva@yandex.ru\"}]}";
            cluster.filterSearch().add(
                filterSearchUri(DEFAULT_OP_ID, uid, lcn, mid),
                '{' + envelopes(envelope) + '}');

            String expecting =
                "{\"payload\":{\"message\":{\"hdr_status\":\"New\",\"lcn\":"
                    + lcn + ",\"mid\":\"164381386399042000\","
                    + "\"firstline\":\"Чтобы подтвердить\",\"sz\":1,"
                    + "\"hdr_subject\":\"Подтверждение\",\"fid\":\"1\","
                    + "\"operation\":\"insert\",\"thread_id\":"
                    + "\"164381386399042000\",\"session_key\":\"\","
                    + "\"method_id\":\"\",\"received_date\":\"\",\"uid\":\""
                    + uid + "\",\"lid\":\"\","
                    + "\"hdr_from\":\"\\\\\\\"Blizzard Entertainment\\\\\\\""
                    + " noreply@blizzard.com\",\"hdr_to\":\"\\\\\\\""
                    + "l.bakyleva@yandex.ru\\\\\\\" l.bakyleva@yandex.ru\","
                    + "\"fresh_count\":\"0\",\"hdr_message_id\":\"\","
                    + "\"fid_type\":\"\",\"categories\":{\"164381386399042000\""
                    + ":[\"people\"]}," + counters(1, 0, 0, 0)
                    + "},\"operation\":\"insert\",\"service\":\"msearch\","
                    + "\"uid\":\"" + uid + "\",\"session_key\":\"\",\"lcn\":"
                    + lcn + ",\"tags\":[],\"version\":\"1\",\"raw_data\":"
                    + "{\"uid\":\"" + uid + "\",\"operation\":\"unsupported\","
                    + "\"envelopes\":[" + envelope + "]}}}";

            cluster.xiva().add(
                xivaNotifyUri,
                new ExpectingHttpItem(new JsonChecker(expecting)));

            try (CloseableHttpResponse response =
                     client.execute(cluster.xavier().host(), post))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    1,
                    cluster.xiva().accessCount(xivaNotifyUri));
            }
        }
    }

    @Test
    public void testUpdates() throws Exception {
        try (XavierCluster cluster = new XavierCluster();
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();

            final String mid1 = "164381386399042548";
            final String mid2 = "164381386399042549";
            final String lids = "44,64,74,130,138,141,148";
            final String lcn1 = "123";
            final String queueId0 = "100499";
            final String queueId1 = "100500";

            HttpPost post0 = new HttpPost(
                notifyUri(ChangeType.UPDATE, 1, queueId0));
            post0.setEntity(
                new StringEntity(
                    notifyBody(
                        ChangeType.UPDATE,
                        lcn1,
                        notifyItem(mid1, false, DEFAULT_INBOX_FID, lids))));

            HttpPost post1 = new HttpPost(
                notifyUri(ChangeType.UPDATE, 1, queueId1));
            post1.setEntity(
                new StringEntity(
                    notifyBody(
                        ChangeType.UPDATE,
                        lcn1,
                        notifyItem(mid2, false, DEFAULT_INBOX_FID, lids))));

            cluster.producer().add(
                statusUri(DEFAULT_UID),
                new StaticHttpItem("[{\"localhost\":100499}]"),
                new StaticHttpItem("[{\"localhost\":100501}]"));
            String xivaMobile =
                xivaListItem(
                    "ru_yandex_mail_3_21_43964__samsung_"
                        + "GT-I9500__Android_5_0_1_",
                    "",
                    "gcm");
            String xivaYaBro = xivaListItem(WEB_XIVA_CLIENT, "", "");

            cluster.xiva().add(
                xivaListUri(DEFAULT_UID),
                new StaticHttpItem(xivaList(xivaMobile)),
                new StaticHttpItem(xivaList(xivaMobile, xivaYaBro)));

            String xivaNotifyUri =
                xivaSendUri(DEFAULT_UID, XIVA_UPDATE_EVENT, lcn1);

            cluster.proxy().add(
                proxyCategoryUri(DEFAULT_UID, mid2, queueId1),
                proxyCategoryData(mid2, SOCIAL));

            cluster.proxy().add(
                proxyCountersUri(DEFAULT_UID, queueId1),
                "[0, 1, 0, 0]");

            cluster.filterSearch().add(
                filterSearchUri(DEFAULT_OP_ID, DEFAULT_UID, lcn1, mid2),
                "{\"envelopes\":["
                    + "{\"mid\":\"164381386399042549\",\"fid\":\"1\"}]}");

            String expecting =
                "{\"payload\":{\"message\":{\"lcn\":" + lcn1
                    + ",\"method_id\":\"\", \"mids\":[\"" + mid2
                    + "\"],\"operation\":\"status change\",\"session_key\":\"\""
                    + ",\"status\":\"RO\", \"uid\":\""
                    + DEFAULT_UID + "\",\"categories\":{\"" + mid2
                    + "\":[\"social\"]}," + counters(0, 1, 0, 0)
                    + "},\"operation\":\"unsupported\","
                    + "\"service\":\"msearch\",\"uid\":\"" + DEFAULT_UID
                    + "\",\"tags\":[],\"version\":\"1\",\"raw_data\":{"
                    + "\"lcn\":" + lcn1 + ",\"method_id\":\"\",\"mids\":[\""
                    + mid2 + "\"],\"operation\":\"status change\","
                    + "\"session_key\":\"\",\"status\":\"RO\",\"uid\":\""
                    + DEFAULT_UID + "\", \"categories\":{\"" + mid2
                    + "\": [\"social\"]}," + counters(0, 1, 0, 0) + "}}}";
            cluster.xiva().add(
                xivaNotifyUri,
                new ExpectingHttpItem(new JsonChecker(expecting)));

            try (CloseableHttpResponse response =
                     client.execute(cluster.xavier().host(), post0))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            }

            try (CloseableHttpResponse response =
                     client.execute(cluster.xavier().host(), post1))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    1,
                    cluster.xiva().accessCount(xivaNotifyUri));
            }
        }
    }
}
