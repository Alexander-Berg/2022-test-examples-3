package ru.yandex.search.mail.kamaji;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.dbfields.FilterSearchFields;
import ru.yandex.http.test.ChainedHttpResource;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.test.StaticHttpItem;
import ru.yandex.parser.uri.QueryConstructor;
import ru.yandex.search.prefix.LongPrefix;
import ru.yandex.test.search.backend.TestMailSearchBackend;
import ru.yandex.test.search.backend.TestSearchBackend;
import ru.yandex.test.util.JsonChecker;

public class KamajiSubscriptionsTest extends KamajiTestBase {
    private static final long UID = 23623462;
    private static final String NOTIFY =
        "/notify?mdb=pg&pgshard=2093&operation-id=3740069460"
            + "&operation-date=1522644953.387902&uid=" + UID;
    private static final String UID_N = "&uid=";
    private static final String SUBS_YA_RU = "subs@yandex.ru";
    private static final String FILTER_SEARCH =
        "/filter_search?order=default&full_folders_and_labels=1&uid=";
    private static final String COMMA = ",";
    private static final String INBOX = "1";
    private static final String OUTBOX = "4";
    private static final String SPAM = "2";
    private static final String PENDING = "10";
    private static final String TRASH = "3";
    private static final String DRAFT = "6";

    // CSOFF: ParameterNumber
    private HttpPost store(
        final KamajiCluster cluster,
        final String mid,
        final String fid,
        final String lcn)
        throws IOException
    {
        return store(cluster, mid, fid, lcn, UID);
    }

    private HttpPost store(
        final KamajiCluster cluster,
        final String mid,
        final String fid,
        final String lcn,
        final long uid)
        throws IOException
    {
        String data =
            "{\"change_type\" : \"store\",\"useful_new_messages\":\"7526\","
                + "\"uid\" : \"" + uid + "\",\"lcn\": \"" + lcn + "\",\n"
                + "\"select_date\" : \"1522644706.594\",\n"
                + "\"operation_id\" : \"3740069460\",\n"
                + "\"pgshard\" : \"2278\",\n"
                + "\"changed\" : [\n"
                + "{\"deleted\" : false,\"tid\":" + mid + ",\"mid\":"
                + mid + ",\"seen\" : false,\n"
                + "\"fid\" : " + fid + ",\n"
                + "\"hdr_message_id\" : \"\",\n\"lids\" : [101,114,120,46,52],"
                + "\"recent\" : true}],\"fresh_count\" : \"9\",\n"
                + "\"operation_date\" : \"1522644706.386214\"}";

        HttpPost post =
            new HttpPost(cluster.kamaji().host().toString() + NOTIFY);
        post.setEntity(new StringEntity(data, ContentType.APPLICATION_JSON));
        return post;
    }
    // CSON: ParameterNumber

    private HttpPost fieldsUpdate(
        final KamajiCluster cluster,
        final String mid)
        throws IOException
    {
        String data =
            "{\"change_type\":\"fields_update\",\"uid\":" + UID
                + ",\"operation_date\":1522819576,\"changed\":[{\"mid\":\""
                + mid + "\",\"clicks_total_count\":{\"function\":\"inc\"},"
                + "\"senders\":{\"senders_from_read_count\":"
                + "{\"function\":\"inc\"}}}]}";
        HttpPost post =
            new HttpPost(cluster.kamaji().host().toString() + NOTIFY);
        post.setEntity(new StringEntity(data, ContentType.APPLICATION_JSON));
        return post;
    }

    private HttpPost move(
        final KamajiCluster cluster,
        final String lcn,
        final Map<String, Map.Entry<String, String>> mids)
        throws IOException
    {
        String data =
            "{\"change_type\" : \"move\",\n"
                + "   \"useful_new_messages\" : \"22\",\n"
                + "   \"uid\" : \"" + UID + "\",\"lcn\" : \"" + lcn
                + "\",\"select_date\" : \"1522644953.436\",\n"
                + "   \"operation_id\" : \"3740069460\",\n"
                + "   \"pgshard\" : \"2278\",\n"
                + "   \"changed\" : [\n";
        for (Map.Entry<String, Map.Entry<String, String>> mid
            : mids.entrySet())
        {
            data += "{\"src_fid\" : " + mid.getValue().getKey()
                + ",\"fid\" : " + mid.getValue().getValue()
                + ",\"mid\" : " + mid.getKey()
                + ",\"tid\" : null,\"seen\" : false,\"lids\" : [],\n"
                + "\"recent\" : true,\"deleted\" : false}";
        }
        data +=
            "],\"fresh_count\" : \"0\",\"arguments\" : {\n"
                + "\"fid\" : 3},\n"
                + "   \"operation_date\" : \"1522644953.387902\"}";

        HttpPost post =
            new HttpPost(cluster.kamaji().host().toString() + NOTIFY);
        post.setEntity(new StringEntity(data, ContentType.APPLICATION_JSON));
        return post;
    }

    private String folder(final String type, final String name) {
        return "\"folder\":{\"type\":{\"title\":\"" + type + "\"},"
            + "\"symbolicName\": {\"title\": \"" + name + "\"},\"name\":\""
            + name + '\"' + '}';
    }

    //CSOFF: ParameterNumber

    private String fsMid(
        final String mid,
        final String fid,
        final long receivedDate,
        final String from,
        final List<String> types)
    {
        String folder = "";
        switch (fid) {
            case INBOX:
                folder = folder(FilterSearchFields.TYPE_SYSTEM, "inbox");
                break;
            case SPAM:
                folder = folder(FilterSearchFields.TYPE_SYSTEM, "spam");
                break;
            case DRAFT:
                folder = folder(FilterSearchFields.TYPE_SYSTEM, "draft");
                break;
            case OUTBOX:
                folder = folder(FilterSearchFields.TYPE_SYSTEM, "sent");
                break;
            case TRASH:
                folder = folder(FilterSearchFields.TYPE_SYSTEM, "trash");
                break;
            case PENDING:
                folder = folder(FilterSearchFields.TYPE_SYSTEM, "pending");
                break;
            default:
                folder = folder(FilterSearchFields.TYPE_USER, fid);
                break;
        }

        return fsMid(mid, fid, folder, receivedDate, from, types);
    }

    private String fsMid(
        final String mid,
        final String fid,
        final String folder,
        final long receivedDate,
        final String from,
        final List<String> types)
    {
        String[] address = from.split("@");

        return
            "{\"mid\": \"" + mid + "\",\"fid\": " + fid
                + ",\"receiveDate\":" + receivedDate + ",\"threadId\":12,"
                + "\"types\" : [" + String.join(COMMA, types)
                + "],\"from\":[{\"domain\":\"" + address[1] + "\",\"local\":\""
                + address[0] + "\",\"displayName\" : \"Здесь имя\"}],"
                + "\"rfcId\":13,\"stid\": \"1.1.1\","
                + "\"mid\":\"" + mid + "\",\"to\":[{\"domain\" : \"yandex.ru\","
                + "\"local\" : \"subs\",\"displayName\" : \"subs\"}],"
                + folder + '}';
    }

    private String filterSearch(
        final KamajiCluster cluster,
        final String lcn,
        final Map<String, String> mids)
    {
        return filterSearch(cluster, lcn, mids, UID);
    }

    private String filterSearch(
        final KamajiCluster cluster,
        final String lcn,
        final Map<String, String> mids,
        final long uid)
    {
        String uri = FILTER_SEARCH + uid + "&mdb=pg&suid=90000&lcn=";
        if (lcn != null) {
            uri += lcn + "&operation-id=3740069460&pgshard=2278";
        } else {
            uri += "-1&zoo-queue-id=-1";
        }
        for (String mid: mids.keySet()) {
            uri += "&mids=" + mid;
        }

        String data =
            "{\"envelopes\":[" + String.join(COMMA, mids.values()) + "]}";

        cluster.filterSearch().add(
            uri,
            new ChainedHttpResource(
                new StaticHttpItem(data),
                new StaticHttpItem(data)));

        return uri;
    }

    private void check(
        final TestSearchBackend lucene,
        final String... expectedArr)
        throws Exception
    {
        check("url:subs_*", lucene, expectedArr);
    }

    private void checkSubsDb(
        final TestSearchBackend lucene,
        final String... expectedArr)
        throws Exception
    {
        check("url:subs_*&db=subscriptions", lucene, expectedArr);
    }

    private void check(
        final String text,
        final TestSearchBackend lucene,
        final String... expectedArr)
        throws Exception
    {
        String checkStr = "{\"hitsArray\": [";
        for (int i = 0; i < expectedArr.length; i++) {
            if (i != 0) {
                checkStr += ',';
            }
            checkStr += expectedArr[i];
        }

        checkStr += "],\"hitsCount\": " + expectedArr.length + '}';
        lucene.checkSearch(
            "/search-qwe?prefix=" + UID + "&text=" + text + "&get=*",
            new JsonChecker(checkStr));
    }

    private void check(
        final TestSearchBackend lucene,
        final long monthTs,
        final long receivedTs,
        final String email,
        final String read,
        final String received)
        throws Exception
    {
        check(lucene, expected(monthTs, receivedTs, email, read, received));
    }

    private void checkSubsDb(
        final TestSearchBackend lucene,
        final long uid,
        final long receivedTs,
        final String email,
        final String domain,
        final String read,
        final String received)
        throws Exception
    {
        checkSubsDb(lucene, expectedSubsDb(uid, receivedTs, email, domain, read, received));
    }

    private void checkSubsDb(
        final TestSearchBackend lucene,
        final long uid,
        final long receivedTs,
        final String email,
        final String domain,
        final String read,
        final String received,
        final String hiddenTypes,
        final String userActionTs)
        throws Exception
    {
        checkSubsDb(lucene, expectedSubsDb(uid, receivedTs, email, domain, read, received, hiddenTypes, userActionTs));
    }

    private String expected(
        final long monthTs,
        final long receivedTs,
        final String email,
        final String read,
        final String received)
        throws Exception
    {
        String expected =
            "{\"subs_email\": \"" + email;
        if (read != null) {
            expected += "\",\"subs_read_types\":\"" + read;
        }

        expected += "\",\"type\":\"mail_subscriptions"
                + "\",\"subs_received_types\":\"" + received
                + "\", \"subs_received_month\": \"" + monthTs
                + "\", \"subs_last_received_date\": \"" + receivedTs
                + "\", \"subs_names\":\"Здесь имя"
                + "\",\"url\": \"subs_" + UID + '_' + email + '_' + monthTs
                + "\"}";
        return expected;
    }

    private String expectedSubsDb(
        final Long uid,
        final long receivedTs,
        final String email,
        final String domain,
        final String read,
        final String receivedTypes)
        throws Exception
    {
        return expectedSubsDb(uid, receivedTs, email, domain, read, receivedTypes, null, null);
    }

    private String expectedSubsDb(
        final Long uid,
        final long receivedTs,
        final String email,
        final String domain,
        final String read,
        final String receivedTypes,
        final String hiddenTypes,
        final String userActionTs)
        throws Exception
    {
        String expected =
            "{\"subs_email\": \"" + email;
        if (read != null) {
            expected += "\",\"subs_read_types\":\"" + read;
        }

        if (hiddenTypes != null) {
            expected += "\", \"subs_hidden_types\": \"" + hiddenTypes;
        }

        if (userActionTs != null) {
            expected += "\", \"subs_user_action_ts\": \"" + userActionTs;
        }

        expected += "\",\"subs_uid\":\"" + uid
            + "\",\"subs_received_types\":\"" + receivedTypes
            + "\", \"subs_domain\": \"" + domain
            + "\", \"subs_received_date\": \"" + receivedTs
            + "\", \"subs_names\":\"Здесь имя"
            + "\",\"url\": \"subs_" + UID + '_' + email
            + "\"}";
        return expected;
    }
    //CSON: ParameterNumber

    private static void init(
        final KamajiCluster cluster)
        throws Exception
    {
        String bbUri = blackboxUri(UID_N + UID);
        cluster.blackbox().add(
            bbUri,
            blackboxResponse(UID, SUBS_YA_RU));
        cluster.tikaite().add(
            "/mail/handler?json-type=dollar&stid=1.1.1",
            "{\"docs\":[{\"hid\":\"1\",\"body_text\":\"hello\"}]}");
    }

    @Test
    public void testTimeRangeSearch() throws Exception {
        KamajiConfigBuilder config =
            new KamajiConfigBuilder().subscriptionsIndexEnabled(true);
        try (TestSearchBackend lucene = new TestMailSearchBackend(this);
             KamajiCluster cluster = new KamajiCluster(lucene, config))
        {
            init(cluster);

            cluster.kamaji().start();

            final String kolya = "kolya@yandex.ru";
            final String mid1 = "445670";
            final String mid2 = "445671";
            final String mid3 = "445672";
            final String mid4 = "445673";
            final String mid5 = "445674";
            final String lcn = "756";
            final long ts1 = 1522734446L; // Втр Апр  3 08:47:26 MSK 2018
            final long ts2 = 1521734446L; // Чтв Мар 22 19:00:46 MSK 2018
            final long ts3 = 1521304446L; // Сбт Мар 17 19:34:06 MSK 2018
            final long ts4 = 1501734446; //  Чтв Авг  3 07:27:26 MSK 2017
            final long ts5 = 1488734446L; // Вск Мар  5 20:20:46 MSK 2017
            final long expMonth1 = 1522530000L;
            final long expMonth2 = 1519851600L;
            final String type = "26";
            List<String> types = Collections.singletonList(type);
            // first
            String envelope = fsMid(mid1, INBOX, ts1, kolya, types);
            Map<String, String> envelopes =
                Collections.singletonMap(mid1, envelope);
            filterSearch(cluster, lcn, envelopes);
            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                store(cluster, mid1, INBOX, lcn));
            // second
            envelope = fsMid(mid2, INBOX, ts2, kolya, types);
            envelopes = Collections.singletonMap(mid2, envelope);
            filterSearch(cluster, lcn, envelopes);

            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                store(cluster, mid2, INBOX, lcn));

            // third
            envelope = fsMid(mid3, INBOX, ts3, kolya, types);
            envelopes = Collections.singletonMap(mid3, envelope);
            filterSearch(cluster, lcn, envelopes);

            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                store(cluster, mid3, INBOX, lcn));

            // forth
            envelope = fsMid(mid4, INBOX, ts4, kolya, types);
            envelopes = Collections.singletonMap(mid4, envelope);
            filterSearch(cluster, lcn, envelopes);

            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                store(cluster, mid4, INBOX, lcn));

            //fifth
            envelope = fsMid(mid5, INBOX, ts5, kolya, types);
            envelopes = Collections.singletonMap(mid5, envelope);
            filterSearch(cluster, lcn, envelopes);

            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                store(cluster, mid5, INBOX, lcn));

            final String expReceived1 = "26\t1";
            // 1521300000 Сбт Мар 17 18:20:00 MSK 2018
            // 1522735446 Втр Апр  3 09:04:06 MSK 2018
            check(
                "subs_received_month_p:[1521300000+TO+1522735446]",
                lucene,
                expected(expMonth1, ts1, kolya, null, expReceived1));
            // 1522735446 Втр Апр  3 09:04:06 MSK 2018
            // 1518642000 Чтв Фев 15 00:00:00 MSK 2018
            String expReceived2 = "26\t2";
            check(
                "subs_received_month_p:[1518642000+TO+1522735446]",
                lucene,
                expected(expMonth1, ts1, kolya, null, expReceived1),
                expected(expMonth2, ts2, kolya, null, expReceived2));
        }
    }

    @Test
    public void testSearchMessageTypes() throws Exception {
        KamajiConfigBuilder config =
            new KamajiConfigBuilder().subscriptionsIndexEnabled(true);
        try (TestSearchBackend lucene = new TestMailSearchBackend(this);
             KamajiCluster cluster = new KamajiCluster(lucene, config))
        {
            init(cluster);

            cluster.kamaji().start();

            final String petya = "petya@yandex.ru";
            final String fedya = "fedya@yandex.ru";
            final String mid1 = "345670";
            final String mid2 = "345671";
            final String mid3 = "345672";
            final String mid4 = "345673";
            final String lcn = "456";
            final long ts = 1522734446L;
            final long expMonth1 = 1522530000L;
            List<String> types1 = Arrays.asList("14".split(COMMA));
            List<String> types2 = Arrays.asList("1,23,43".split(COMMA));
            List<String> types3 = Arrays.asList("14,43,44".split(COMMA));
            List<String> types4 = Arrays.asList("12,23,44".split(COMMA));
            // first petya
            String envelope = fsMid(mid1, INBOX, ts, petya, types1);
            Map<String, String> envelopes =
                Collections.singletonMap(mid1, envelope);
            filterSearch(cluster, lcn, envelopes);

            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                store(cluster, mid1, INBOX, lcn));

            // second fedya
            envelope = fsMid(mid2, INBOX, ts, fedya, types2);
            envelopes = Collections.singletonMap(mid2, envelope);
            filterSearch(cluster, lcn, envelopes);

            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                store(cluster, mid2, INBOX, lcn));

            // third petya
            envelope = fsMid(mid3, INBOX, ts, petya, types3);
            envelopes = Collections.singletonMap(mid3, envelope);
            filterSearch(cluster, lcn, envelopes);

            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                store(cluster, mid3, INBOX, lcn));

            // forth fedya
            envelope = fsMid(mid4, INBOX, ts, fedya, types4);
            envelopes = Collections.singletonMap(mid4, envelope);
            filterSearch(cluster, lcn, envelopes);

            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                store(cluster, mid4, INBOX, lcn));

            // ok now fedya types is 1,12,23,43,44
            // petya is 13,43,44
            String fedyaRcvd = "44\t1\n12\t1\n23\t2\n1\t1\n43\t1";
            String petyaRcvd = "44\t1\n14\t2\n43\t1";
            check(
                "subs_message_types:1",
                lucene,
                expected(expMonth1, ts, fedya, null, fedyaRcvd));

            check(
                "subs_message_types:(1+OR+43)",
                lucene,
                expected(expMonth1, ts, fedya, null, fedyaRcvd),
                expected(expMonth1, ts, petya, null, petyaRcvd));
        }
    }

    // CSOFF: MultipleStringLiterals
    // CSOFF: MagicNumber
    @Test
    public void testInvalidFolder() throws Exception {
        KamajiConfigBuilder config =
            new KamajiConfigBuilder().subscriptionsIndexEnabled(true);
        try (TestSearchBackend lucene = new TestMailSearchBackend(this);
             KamajiCluster cluster = new KamajiCluster(lucene, config))
        {
            init(cluster);

            final String lcn = "444";
            final String mid1 = "1000";
            final long ts1 = 1522736446L;
            final List<String> types = Collections.singletonList("13");

            String petya = "asya@yandex.ru";

            cluster.kamaji().start();

            String envelope = fsMid(
                mid1,
                "500",
                folder(
                    FilterSearchFields.TYPE_SYSTEM,
                    "Abrivalg"),
                ts1,
                petya,
                types);
            Map<String, String> envelopes =
                Collections.singletonMap(mid1, envelope);
            filterSearch(cluster, lcn, envelopes);

            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                store(cluster, mid1, "500", lcn));
        }
    }

    // CSOFF: MethodLength
    @Test
    public void testChangeTypes() throws Exception {
        KamajiConfigBuilder config =
            new KamajiConfigBuilder().subscriptionsIndexEnabled(true);
        try (TestSearchBackend lucene = new TestMailSearchBackend(this);
             KamajiCluster cluster = new KamajiCluster(lucene, config))
        {
            init(cluster);

            final String userFolder = "99";
            final String lcn = "444";
            final String mid1 = "1000";
            final String mid2 = "1001";
            final String mid3 = "1002";
            final String mid4 = "1003";
            final String mid5 = "1004";
            final String mid6 = "1005";

            final List<String> types1 =
                Arrays.asList("1,13,65".split(COMMA));
            final List<String> types2 = Collections.singletonList("13");
            final List<String> types3 = Arrays.asList("42,13".split(COMMA));

            final long ts1 = 1522736446L; // April 2018 month ts is 1522530000
            final long ts2 = 1522734446L; // --

            final long expMonth1 = 1522530000L;
            String vasya = "vasya@yandex.ru";
            String domain = "yandex.ru";

            cluster.kamaji().start();

            String envelope = fsMid(mid1, userFolder, ts1, vasya, types1);
            Map<String, String> envelopes =
                Collections.singletonMap(mid1, envelope);
            String fsUri1 = filterSearch(cluster, lcn, envelopes);

            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                store(cluster, mid1, userFolder, lcn));

            Assert.assertEquals(2, cluster.filterSearch().accessCount(fsUri1));
            String expRcv1 = "1\t1\n13\t1\n65\t1";
            check(lucene, expMonth1, ts1, vasya, null, expRcv1);
            checkSubsDb(lucene, UID, ts1, vasya, domain,  null, expRcv1);
            // second mid
            envelope = fsMid(mid2, PENDING, ts2, vasya, types2);
            envelopes = Collections.singletonMap(mid2, envelope);
            String fsUri2 = filterSearch(cluster, lcn, envelopes);

            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                store(cluster, mid2, PENDING, lcn));

            Assert.assertEquals(
                2,
                cluster.filterSearch().accessCount(fsUri2));

            String expRcv2 = "1\t1\n13\t2\n65\t1";
            check(lucene, expMonth1, ts1, vasya, null, expRcv2);
            checkSubsDb(lucene, UID, ts1, vasya, domain,  null, expRcv2);

            // read first
            envelope = fsMid(mid1, userFolder, ts1, vasya, types1);
            envelopes = Collections.singletonMap(mid1, envelope);
            String fsUri3 = filterSearch(cluster, null, envelopes);

            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                fieldsUpdate(cluster, mid1));
            Assert.assertEquals(2, cluster.filterSearch().accessCount(fsUri3));

            check(lucene, expMonth1, ts1, vasya, expRcv1, expRcv2);
            checkSubsDb(lucene, UID, ts1, vasya, domain,  expRcv1, expRcv2);

            // than move first to another folder
            envelope = fsMid(mid1, OUTBOX, ts1, vasya, types1);
            envelopes = Collections.singletonMap(mid1, envelope);
            String fsUri4 = filterSearch(cluster, lcn, envelopes);
            Map<String, Map.Entry<String, String>> changed =
                Collections.singletonMap(
                    mid1,
                    new AbstractMap.SimpleEntry<>(TRASH, OUTBOX));

            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                move(cluster, lcn, changed));

            // nothing should changed
            check(lucene, expMonth1, ts1, vasya, expRcv1, expRcv2);
            checkSubsDb(lucene, UID, ts1, vasya, domain,  expRcv1, expRcv2);

            Assert.assertEquals(2, cluster.filterSearch().accessCount(fsUri4));
            // store to other fid
            envelope = fsMid(mid3, OUTBOX, ts1, vasya, types3);
            envelopes = Collections.singletonMap(mid3, envelope);
            String fsUri5 = filterSearch(cluster, lcn, envelopes);

            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                store(cluster, mid3, OUTBOX, lcn));

            // ignoring store to outbox
            check(lucene, expMonth1, ts1, vasya, expRcv1, expRcv2);
            checkSubsDb(lucene, UID, ts1, vasya, domain,  expRcv1, expRcv2);

            Assert.assertEquals(2, cluster.filterSearch().accessCount(fsUri5));
            //read mid3
            envelope = fsMid(mid3, OUTBOX, ts1, vasya, types3);
            envelopes = Collections.singletonMap(mid3, envelope);
            filterSearch(cluster, null, envelopes);
            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                fieldsUpdate(cluster, mid3));
            check(lucene, expMonth1, ts1, vasya, expRcv1, expRcv2);
            checkSubsDb(lucene, UID, ts1, vasya, domain,  expRcv1, expRcv2);
            // move mid3 to INBOX
            envelope = fsMid(mid3, INBOX, ts1, vasya, types3);
            envelopes = Collections.singletonMap(mid3, envelope);
            filterSearch(cluster, lcn, envelopes);
            changed = Collections.singletonMap(
                mid3,
                new AbstractMap.SimpleEntry<>(OUTBOX, INBOX));
            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                move(cluster, lcn, changed));

            check(lucene, expMonth1, ts1, vasya, expRcv1, expRcv2);
            checkSubsDb(lucene, UID, ts1, vasya, domain,  expRcv1, expRcv2);

            QueryConstructor qc = new QueryConstructor("/search?");
            qc.append("prefix", UID);
            qc.append("text", "subs_message_types:13");
            qc.append("get", "*");

            //test e,pty mtypes
            envelope = fsMid(mid4, OUTBOX, ts1, vasya, Collections.emptyList());
            envelopes = Collections.singletonMap(mid4, envelope);
            filterSearch(cluster, lcn, envelopes);

            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                store(cluster, mid4, INBOX, lcn));

            // simple store to inbox
            envelope = fsMid(mid5, INBOX, ts1, vasya, types2);
            envelopes = Collections.singletonMap(mid5, envelope);
            fsUri5 = filterSearch(cluster, lcn, envelopes);

            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                store(cluster, mid5, INBOX, lcn));

            Assert.assertEquals(
                2,
                cluster.filterSearch().accessCount(fsUri5));
            String expRcv5 = "1\t1\n13\t3\n65\t1";
            String expRead5 = "1\t1\n13\t1\n65\t1";
            check(lucene, expMonth1, ts1, vasya, expRead5, expRcv5);
            checkSubsDb(lucene, UID, ts1, vasya, domain,  expRead5, expRcv5);

            // simple store to user
            String userFid = "156";
            envelope = fsMid(mid6, userFid, ts1, vasya, types2);
            envelopes = Collections.singletonMap(mid6, envelope);
            String fsUri6 = filterSearch(cluster, lcn, envelopes);

            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                store(cluster, mid6, userFid, lcn));

            Assert.assertEquals(
                2,
                cluster.filterSearch().accessCount(fsUri6));
            String expRcv6 = "1\t1\n13\t4\n65\t1";
            String expRead6 = "1\t1\n13\t1\n65\t1";
            check(lucene, expMonth1, ts1, vasya, expRead6, expRcv6);
            checkSubsDb(lucene, UID, ts1, vasya, domain,  expRead6, expRcv6);
        }
    }
    // CSON: MultipleStringLiterals
    // CSON: MagicNumber
    // CSON: MethodLength

    @Test
    public void testMailActionFastThanEverything() throws Exception {
        KamajiConfigBuilder config =
            new KamajiConfigBuilder().subscriptionsIndexEnabled(true);
        try (TestSearchBackend lucene = new TestMailSearchBackend(this);
             KamajiCluster cluster = new KamajiCluster(lucene, config))
        {
            init(cluster);

            final String lcn = "133428";
            final String mid = "165507286305912386";

            final List<String> types =
                Arrays.asList("58,4,55".split(COMMA));

            final long ts = 1525335366;

            final long expMonth1 = 1525122000L;
            String vasya = "osago@nasko.ru";

            cluster.kamaji().start();

            String envelope = fsMid(mid, INBOX, ts, vasya, types);
            Map<String, String> envelopes =
                Collections.singletonMap(mid, envelope);
            String fsUri1 = filterSearch(cluster, lcn, envelopes);

            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                store(cluster, mid, INBOX, lcn));

            Assert.assertEquals(2, cluster.filterSearch().accessCount(fsUri1));
            String expRcv1 = "55\t1\n58\t1\n4\t1";
            check(lucene, expMonth1, ts, vasya, null, expRcv1);
            // read first

            envelope = fsMid(mid, INBOX, ts, vasya, types);
            envelopes = Collections.singletonMap(mid, envelope);
            filterSearch(cluster, null, envelopes);
            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                fieldsUpdate(cluster, mid));
            check(lucene, expMonth1, ts, vasya, expRcv1, expRcv1);

            // read second time, but now click came faster than move
            // if current state has good folder type we increasing read counter
            envelope = fsMid(mid, OUTBOX, ts, vasya, types);
            envelopes = Collections.singletonMap(mid, envelope);
            filterSearch(cluster, null, envelopes);
            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                fieldsUpdate(cluster, mid));
            check(lucene, expMonth1, ts, vasya, expRcv1, expRcv1);

            String expRead2 = "55\t2\n58\t2\n4\t2";
            envelope = fsMid(mid, "100", ts, vasya, types);
            envelopes = Collections.singletonMap(mid, envelope);
            filterSearch(cluster, null, envelopes);
            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                fieldsUpdate(cluster, mid));
            check(lucene, expMonth1, ts, vasya, expRead2, expRcv1);
            envelope = fsMid(mid, TRASH, ts, vasya, types);
            envelopes = Collections.singletonMap(mid, envelope);
            filterSearch(cluster, null, envelopes);
            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                fieldsUpdate(cluster, mid));
            check(lucene, expMonth1, ts, vasya, expRead2, expRcv1);
        }
    }

    @Test
    public void testKeepUserData() throws Exception {
        KamajiConfigBuilder config =
            new KamajiConfigBuilder().subscriptionsIndexEnabled(true);
        try (TestSearchBackend lucene = new TestMailSearchBackend(this);
             KamajiCluster cluster = new KamajiCluster(lucene, config)) {
            init(cluster);

            HttpPost post = new HttpPost(lucene.indexerUri() + "/add?db=subscriptions");
            post.setEntity(
                new StringEntity(
                    "{\"prefix\": \"" + UID + "\", " +
                        "\"docs\":[{\"url\": \"subs_" + UID + "_notification@facebookmail.com\"," +
                        " \"subs_hidden_types\":\"10\", \"subs_user_action_ts\": \"100500\"}]}",
                    ContentType.APPLICATION_JSON));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);

            final String lcn = "544";
            final String mid1 = "2000";
            //final String mid2 = "2001";

            final List<String> types1 = Arrays.asList("17, 54, 100".split(COMMA));

            final long ts1 = 1522736446L; // April 2018 month ts is 1522530000
            //final long expMonth1 = 1522530000L;
            String fb1 = "notification+zj4o06c29t6y@facebookmail.com";
            String expEmail = "notification@facebookmail.com";
            String expDomain = "facebookmail.com";

            cluster.kamaji().start();

            String envelope = fsMid(mid1, INBOX, ts1, fb1, types1);
            Map<String, String> envelopes =
                Collections.singletonMap(mid1, envelope);
            String fsUri1 = filterSearch(cluster, lcn, envelopes);

            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                store(cluster, mid1, INBOX, lcn));

            Assert.assertEquals(
                2,
                cluster.filterSearch().accessCount(fsUri1));

            String expRcv1 = "100\t1\n17\t1\n54\t1";

            checkSubsDb(lucene, UID, ts1, expEmail, expDomain, null, expRcv1, "10", "100500");
        }
    }

    @Test
    public void testHashedEmails() throws Exception {
        KamajiConfigBuilder config =
            new KamajiConfigBuilder().subscriptionsIndexEnabled(true);
        try (TestSearchBackend lucene = new TestMailSearchBackend(this);
             KamajiCluster cluster = new KamajiCluster(lucene, config))
        {
            init(cluster);

            final String lcn = "544";
            final String mid1 = "2000";
            final String mid2 = "2001";

            final List<String> types1 = Arrays.asList("17, 54, 100".split(COMMA));

            final long ts1 = 1522736446L; // April 2018 month ts is 1522530000
            final long expMonth1 = 1522530000L;
            String fb1 = "notification+zj4o06c29t6y@facebookmail.com";
            String fb2 = "notification+kr4kaan2nnex@facebookmail.com";
            String expEmail = "notification@facebookmail.com";
            String expDomain = "facebookmail.com";

            cluster.kamaji().start();

            String envelope = fsMid(mid1, INBOX, ts1, fb1, types1);
            Map<String, String> envelopes =
                Collections.singletonMap(mid1, envelope);
            String fsUri1 = filterSearch(cluster, lcn, envelopes);

            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                store(cluster, mid1, INBOX, lcn));

            Assert.assertEquals(
                2,
                cluster.filterSearch().accessCount(fsUri1));

            String expRcv1 = "100\t1\n17\t1\n54\t1";

            check(lucene, expMonth1, ts1, expEmail, null, expRcv1);
            checkSubsDb(lucene, UID, ts1, expEmail, expDomain, null, expRcv1);
            // second mid
            envelope = fsMid(mid2, INBOX, ts1, fb2, types1);
            envelopes = Collections.singletonMap(mid2, envelope);
            String fsUri2 = filterSearch(cluster, lcn, envelopes);

            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                store(cluster, mid2, INBOX, lcn));

            Assert.assertEquals(
                2,
                cluster.filterSearch().accessCount(fsUri2));

            String expRcv2 = "100\t2\n17\t2\n54\t2";

            check(lucene, expMonth1, ts1, expEmail, null, expRcv2);
            checkSubsDb(lucene, UID, ts1, expEmail, expDomain, null, expRcv2);
        }
    }

    /**
     * Testing DST case, when start date-time do not exists in dst,
     * for example 01 april 1984 00:00:00 do not exists
     * @throws Exception
     */
    @Test
    public void testFirstApril() throws Exception {
        KamajiConfigBuilder config =
            new KamajiConfigBuilder().subscriptionsIndexEnabled(true);
        try (TestSearchBackend lucene = new TestMailSearchBackend(this);
             KamajiCluster cluster = new KamajiCluster(lucene, config))
        {
            init(cluster);

            final String lcn = "1344";
            final String mid1 = "10000";

            final List<String> types1 = Arrays.asList("18, 54".split(COMMA));

            final long ts1 = 451902609L; // April 1984 month ts is 449614800
            final long expMonth1 = 449614800L;
            String eml1 = "clockfell@time.com";

            cluster.kamaji().start();

            String envelope = fsMid(mid1, INBOX, ts1, eml1, types1);
            Map<String, String> envelopes =
                Collections.singletonMap(mid1, envelope);
            String fsUri1 = filterSearch(cluster, lcn, envelopes);

            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                store(cluster, mid1, INBOX, lcn));

            Assert.assertEquals(
                2,
                cluster.filterSearch().accessCount(fsUri1));

            String expRcv1 = "18\t1\n54\t1";
            check(lucene, expMonth1, ts1, eml1, null, expRcv1);
        }
    }

    // CSOFF: MethodLength
    @Ignore
    public void testFixSubscription() throws Exception {
        KamajiConfigBuilder config =
            new KamajiConfigBuilder().subscriptionsIndexEnabled(true);
        try (TestSearchBackend lucene = new TestMailSearchBackend(this);
             KamajiCluster cluster = new KamajiCluster(lucene, config))
        {
            init(cluster);

            long lcn = 1L;
            long mid = 1L;

            final List<String> types1 = Arrays.asList("1, 18, 54".split(COMMA));

            final long ts1 = 451902609L; // April 1984 month ts is 449614800
            String eml = "clockfell@time.co";

            cluster.kamaji().start();

            int count = 2 * 2 * 2 + 2;
            long uid = UID;
            final int uidStep = 10;

            for (int i = 0; i < count; i++) {
                String bbUri = blackboxUri(UID_N + uid);
                cluster.blackbox().add(
                    bbUri,
                    blackboxResponse(uid, SUBS_YA_RU));
                String envelope =
                    fsMid(Long.toString(mid), INBOX, ts1, eml + mid, types1);
                Map<String, String> envelopes =
                    Collections.singletonMap(Long.toString(mid), envelope);
                String fsUri1 =
                    filterSearch(cluster, Long.toString(lcn), envelopes, uid);
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_OK,
                    store(
                        cluster,
                        Long.toString(mid),
                        INBOX,
                        Long.toString(lcn),
                        uid));
                Assert.assertEquals(
                    2,
                    cluster.filterSearch().accessCount(fsUri1));
                mid++;
                lcn++;
                uid += uidStep;
            }
            //CSOFF: MultipleStringLiterals
            lucene.checkSearch(
                "/search?prefix=" + UID
                    + "&text=url:subs_*&sort=url&get=url,__prefix",
                TestSearchBackend.prepareResult(
                    count,
                    "\"url\":\"subs_23623552_clockfell@time.co10_449614800\","
                        + "\"__prefix\":\"23623552\"",
                    "\"url\":\"subs_23623542_clockfell@time.co9_449614800\","
                        + "\"__prefix\":\"23623542\"",
                    "\"url\":\"subs_23623532_clockfell@time.co8_449614800\","
                        + "\"__prefix\":\"23623532\"",
                    "\"url\":\"subs_23623522_clockfell@time.co7_449614800\","
                        + "\"__prefix\":\"23623522\"",
                    "\"url\":\"subs_23623512_clockfell@time.co6_449614800\","
                        + "\"__prefix\":\"23623512\"",
                    "\"url\":\"subs_23623502_clockfell@time.co5_449614800\","
                        + "\"__prefix\":\"23623502\"",
                    "\"url\":\"subs_23623492_clockfell@time.co4_449614800\","
                        + "\"__prefix\":\"23623492\"",
                    "\"url\":\"subs_23623482_clockfell@time.co3_449614800\","
                        + "\"__prefix\":\"23623482\"",
                    "\"url\":\"subs_23623472_clockfell@time.co2_449614800\","
                        + "\"__prefix\":\"23623472\"",
                    "\"url\":\"subs_23623462_clockfell@time.co1_449614800\","
                        + "\"__prefix\":\"23623462\""
                ));
            //break prefixes;
            HttpPost post =
                new HttpPost(lucene.indexerUri() + "/update");
            post.setEntity(
                new StringEntity(
                    "{\"prefix\":2,\"query\":\"url:subs_*\""
                        + ",\"docs\":[{\"type\":\"mail_subscriptions\"}]}",
                    ContentType.APPLICATION_JSON));
            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                post);
            //add broken url
            lucene.add(new LongPrefix(2), "\"url\":\"subs__1\"");
            lucene.add(new LongPrefix(2), "\"url\":\"subs_236234j2_1\"");
            lucene.checkSearch(
                "/search?prefix=" + UID
                    + "&text=url:subs_*&sort=url&get=url,__prefix",
                TestSearchBackend.prepareResult(
                    count + 2,
                    "\"url\":\"subs__1\","
                        + "\"__prefix\":\"2\"",
                    "\"url\":\"subs_23623552_clockfell@time.co10_449614800\","
                        + "\"__prefix\":\"23623552\"",
                    "\"url\":\"subs_23623542_clockfell@time.co9_449614800\","
                        + "\"__prefix\":\"23623552\"",
                    "\"url\":\"subs_23623532_clockfell@time.co8_449614800\","
                        + "\"__prefix\":\"23623552\"",
                    "\"url\":\"subs_23623522_clockfell@time.co7_449614800\","
                        + "\"__prefix\":\"23623552\"",
                    "\"url\":\"subs_23623512_clockfell@time.co6_449614800\","
                        + "\"__prefix\":\"23623552\"",
                    "\"url\":\"subs_23623502_clockfell@time.co5_449614800\","
                        + "\"__prefix\":\"23623552\"",
                    "\"url\":\"subs_236234j2_1\","
                        + "\"__prefix\":\"23623552\"",
                    "\"url\":\"subs_23623492_clockfell@time.co4_449614800\","
                        + "\"__prefix\":\"23623552\"",
                    "\"url\":\"subs_23623482_clockfell@time.co3_449614800\","
                        + "\"__prefix\":\"23623552\"",
                    "\"url\":\"subs_23623472_clockfell@time.co2_449614800\","
                        + "\"__prefix\":\"23623552\"",
                    "\"url\":\"subs_23623462_clockfell@time.co1_449614800\","
                        + "\"__prefix\":\"23623552\""
                ));
            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                new HttpGet(cluster.kamaji().host().toString()
                    + "/fix-docs-prefix?url-prefix=subs_"
                    + "&version=1&batch-size=2&shard=2"));
            lucene.checkSearch(
                "/search?prefix=" + UID
                    + "&text=url:subs_*&sort=url&get=url,__prefix",
                TestSearchBackend.prepareResult(
                    count + 2,
                    "\"url\":\"subs__1\","
                        + "\"__prefix\":\"2\"",
                    "\"url\":\"subs_23623552_clockfell@time.co10_449614800\","
                        + "\"__prefix\":\"23623552\"",
                    "\"url\":\"subs_23623542_clockfell@time.co9_449614800\","
                        + "\"__prefix\":\"23623542\"",
                    "\"url\":\"subs_23623532_clockfell@time.co8_449614800\","
                        + "\"__prefix\":\"23623532\"",
                    "\"url\":\"subs_23623522_clockfell@time.co7_449614800\","
                        + "\"__prefix\":\"23623522\"",
                    "\"url\":\"subs_23623512_clockfell@time.co6_449614800\","
                        + "\"__prefix\":\"23623512\"",
                    "\"url\":\"subs_23623502_clockfell@time.co5_449614800\","
                        + "\"__prefix\":\"23623502\"",
                    "\"url\":\"subs_236234j2_1\","
                        + "\"__prefix\":\"2\"",
                    "\"url\":\"subs_23623492_clockfell@time.co4_449614800\","
                        + "\"__prefix\":\"23623492\"",
                    "\"url\":\"subs_23623482_clockfell@time.co3_449614800\","
                        + "\"__prefix\":\"23623482\"",
                    "\"url\":\"subs_23623472_clockfell@time.co2_449614800\","
                        + "\"__prefix\":\"23623472\"",
                    "\"url\":\"subs_23623462_clockfell@time.co1_449614800\","
                        + "\"__prefix\":\"23623462\""
                ));
            //CSON: MultipleStringLiterals
        }
    }
    // CSON: MethodLength
}
