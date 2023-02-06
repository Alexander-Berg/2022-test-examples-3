package ru.yandex.search.disk.proxy.ipdd;

import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.search.disk.proxy.ProxyCluster;
import ru.yandex.test.search.backend.TestSearchBackend;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

public class IpddTest extends TestBase {
    private static final long VERSION = 1441341964000L;
    private static final long MILLIS_PER_DAY = 86400000L;
    private static final long TIMESTAMP =
        VERSION / MILLIS_PER_DAY * MILLIS_PER_DAY;
    private static final long INTERVAL = 1200000L;
    private static final long INTERVAL3 = INTERVAL + INTERVAL + INTERVAL;
    private static final long MSK_TZ_OFFSET = 10800000L;

    // uri parts
    private static final String CLUSTERIZE =
        "/ipdd/clusterize?uid=0&interval=";
    private static final String GROUP_URI =
        "/ipdd/group?uid=0&get=id&group_key=";
    private static final String SEARCH_URI =
        "/ipdd/search?uid=0&text=";
    private static final String RAW_SEARCH_URI =
        "/ipdd/search/raw?uid=0&text=";
    private static final String COUNTERS_URI =
        "/ipdd/counters?uid=0&field=";
    private static final String PRODUCER_URI =
        "/_status?service=ipdd&prefix=0&all&json-type=dollar";
    private static final String PAGE_LOAD_TIMESTAMP =
        "&get=id&page_load_timestamp=";
    private static final String TZ_OFFSET = "&tz_offset=";
    private static final String PLATFORM = "&platform=";
    private static final String FROM = "&end_timestamp=";
    private static final String TO = "&start_timestamp=";
    private static final String EVENT_TYPE_PARAM = "&event_type=";
    private static final String PARENT_FOLDER = "&parent_folder=";
    // dirs
    private static final String DIR1 = "/disk/photos1";
    private static final String DIR2 = "/disk/photoslice \\\"тесты\\\"/te\\\\";
    private static final String DIR2URI = "/disk/photoslice+%22тесты%22/te%5C";
    // event types
    private static final String EVENT_TYPE = "event_type";
    private static final String FS_COPY = "fs-copy";
    private static final String FS_UPLOAD = "fs-upload";
    // platforms
    private static final String IOS = "ios";
    private static final String MAC = "mac";
    // clustered fields
    private static final String SIZE = "\"size\":";
    private static final String GROUP = ",\"group\":[";
    private static final String MAX = "],\"max\":";
    private static final String MIN = ",\"min\":";
    private static final String MERGED = ",\"merged_docs\":";
    private static final String ID = "\"id\":\"";
    // ids
    private static final String NINE = "9";
    private static final String TEN = "10";
    private static final String ELEVEN = "11";
    private static final String TWELVE = "12";
    // docs
    private static final String DOC0 = "\"id\":\"0\"";
    private static final String DOC1 = "\"id\":\"1\"";
    private static final String DOC2 = "\"id\":\"2\"";
    private static final String DOC3 = "\"id\":\"3\"";
    private static final String DOC4 = "\"id\":\"4\"";
    private static final String DOC5 = "\"id\":\"5\"";
    private static final String DOC6 = "\"id\":\"6\"";
    private static final String DOC7 = "\"id\":\"7\"";
    private static final String DOC8 = "\"id\":\"8\"";
    private static final String DOC9 = "\"id\":\"9\"";
    private static final String DOC10 = "\"id\":\"10\"";
    private static final String DOC11 = "\"id\":\"11\"";
    private static final String DOC12 = "\"id\":\"12\"";
    private static final String DOC13 = "\"id\":\"13\"";
    private static final String DOC14 = "\"id\":\"14\"";
    private static final String DOC15 = "\"id\":\"15\"";

    private int docId = 0;

    private String doc(final String name, final String folder) {
        return doc(name, folder, IOS);
    }

    private String doc(
        final String name,
        final String folder,
        final String platform)
    {
        return doc(name, folder, platform, FS_UPLOAD);
    }

    // CSOFF: ParameterNumber
    private String doc(
        final String name,
        final String folder,
        final String platform,
        final String eventType)
    {
        int docId = this.docId++;
        return "\"id\":" + docId + ",\"version\":" + (VERSION - docId)
            + ",\"event_type\":\"" + eventType
            + "\",\"group_key\":\"" + folder
            + "\",\"source_folder\":\"" + folder
            + "\",\"platform\":\"" + platform
            + "\",\"source_path\":\"" + folder + '/' + name
            + "\",\"event_timestamp\":";
    }
    // CSON: ParameterNumber

    private static String mkgroup(
        final String group,
        final long timestamp,
        final long tzOffset)
    {
        return '"' + group + "\",\"" + (timestamp + tzOffset) / MILLIS_PER_DAY
            + '"';
    }

    private void prepareIndex(final ProxyCluster cluster) throws IOException {
        docId = 0;
        cluster.backend().add(
            doc("0", DIR1)
            + (TIMESTAMP - 1L),
            doc("1", DIR1)
            + TIMESTAMP,
            doc("2", DIR1, IOS, FS_COPY) + TIMESTAMP,
            doc("3", DIR1, IOS, FS_COPY) + (TIMESTAMP + INTERVAL),
            doc("4", DIR1, IOS, FS_COPY) + (TIMESTAMP + INTERVAL3 - 1L),
            doc("5", DIR1)
            + (TIMESTAMP + INTERVAL3),
            doc("6", DIR2)
            + TIMESTAMP,
            doc("7", DIR2, MAC)
            + (TIMESTAMP + 2L),
            doc("8", DIR2, MAC, FS_COPY) + (TIMESTAMP + INTERVAL),
            doc(NINE, DIR2, MAC, FS_COPY) + (TIMESTAMP + INTERVAL3 + 1L));
    }

    @Test
    public void testClusterize() throws Exception {
        try (ProxyCluster cluster = new ProxyCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            prepareIndex(cluster);
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host() + CLUSTERIZE + INTERVAL
                        + PAGE_LOAD_TIMESTAMP + VERSION + TZ_OFFSET + 0)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        TestSearchBackend.prepareResult(
                            SIZE + 1 + GROUP
                            + mkgroup(DIR2, TIMESTAMP, 0L)
                            + MAX + (TIMESTAMP + INTERVAL3 + 1L)
                            + MIN + (TIMESTAMP + INTERVAL3 + 1L)
                            + MERGED + TestSearchBackend.concatDocs(DOC9),
                            SIZE + 2 + GROUP
                            + mkgroup(DIR1, TIMESTAMP, 0L)
                            + MAX + (TIMESTAMP + INTERVAL3)
                            + MIN + (TIMESTAMP + INTERVAL3 - 1L)
                            + MERGED + TestSearchBackend.concatDocs(DOC5, DOC4),
                            SIZE + (2 + 1) + GROUP
                            + mkgroup(DIR1, TIMESTAMP, 0L)
                            + MAX + (TIMESTAMP + INTERVAL)
                            + MIN + TIMESTAMP
                            + MERGED + TestSearchBackend.concatDocs(DOC3, DOC2, DOC1),
                            SIZE + (2 + 1) + GROUP
                            + mkgroup(DIR2, TIMESTAMP, 0L)
                            + MAX + (TIMESTAMP + INTERVAL)
                            + MIN + TIMESTAMP
                            + MERGED + TestSearchBackend.concatDocs(DOC8, DOC7, DOC6),
                            SIZE + 1 + GROUP
                            + mkgroup(DIR1, TIMESTAMP - 1L, 0L)
                            + MAX + (TIMESTAMP - 1L)
                            + MIN + (TIMESTAMP - 1L)
                            + MERGED + TestSearchBackend.concatDocs(DOC0))),
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host() + CLUSTERIZE + INTERVAL
                        + PAGE_LOAD_TIMESTAMP + (VERSION - 2L - 1L)
                        + TZ_OFFSET + 0
                        + "&offset=1&max_amount=2&max_events_per_group=1")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        TestSearchBackend.prepareResult(
                            2 + 2,
                            SIZE + 2 + GROUP
                            + mkgroup(DIR1, TIMESTAMP, 0L)
                            + MAX + (TIMESTAMP + INTERVAL3)
                            + MIN + (TIMESTAMP + INTERVAL3 - 1L)
                            + MERGED + TestSearchBackend.concatDocs(DOC5),
                            SIZE + 1 + GROUP
                            + mkgroup(DIR1, TIMESTAMP, 0L)
                            + MAX + (TIMESTAMP + INTERVAL)
                            + MIN + (TIMESTAMP + INTERVAL)
                            + MERGED + TestSearchBackend.concatDocs(DOC3))),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testClusterizeTzOffsetAndPlatform() throws Exception {
        try (ProxyCluster cluster = new ProxyCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            prepareIndex(cluster);
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host() + CLUSTERIZE + INTERVAL
                        + PAGE_LOAD_TIMESTAMP + VERSION
                        + TZ_OFFSET + MSK_TZ_OFFSET + PLATFORM + IOS)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        TestSearchBackend.prepareResult(
                            SIZE + 2 + GROUP
                            + mkgroup(DIR1, TIMESTAMP, MSK_TZ_OFFSET)
                            + MAX + (TIMESTAMP + INTERVAL3)
                            + MIN + (TIMESTAMP + INTERVAL3 - 1L)
                            + MERGED + TestSearchBackend.concatDocs(DOC5, DOC4),
                            SIZE + (2 + 2) + GROUP
                            + mkgroup(DIR1, TIMESTAMP, MSK_TZ_OFFSET)
                            + MAX + (TIMESTAMP + INTERVAL)
                            + MIN + (TIMESTAMP - 1L)
                            + MERGED
                            + TestSearchBackend.concatDocs(DOC3, DOC2, DOC1, DOC0),
                            SIZE + 1 + GROUP
                            + mkgroup(DIR2, TIMESTAMP, 0L)
                            + MAX + TIMESTAMP
                            + MIN + TIMESTAMP
                            + MERGED + TestSearchBackend.concatDocs(DOC6))),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testClusterizeEventTypeAndStartTimestamp() throws Exception {
        try (ProxyCluster cluster = new ProxyCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            prepareIndex(cluster);
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host() + CLUSTERIZE + INTERVAL
                        + PAGE_LOAD_TIMESTAMP + VERSION + TZ_OFFSET + 0
                        + EVENT_TYPE_PARAM + FS_COPY
                        + TO + (TIMESTAMP + INTERVAL + INTERVAL))))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        TestSearchBackend.prepareResult(
                            SIZE + 2 + GROUP
                            + mkgroup(DIR1, TIMESTAMP, 0L)
                            + MAX + (TIMESTAMP + INTERVAL)
                            + MIN + TIMESTAMP
                            + MERGED + TestSearchBackend.concatDocs(DOC3, DOC2),
                            SIZE + 1 + GROUP
                            + mkgroup(DIR2, TIMESTAMP, 0L)
                            + MAX + (TIMESTAMP + INTERVAL)
                            + MIN + (TIMESTAMP + INTERVAL)
                            + MERGED + TestSearchBackend.concatDocs(DOC8))),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testClusterizeParentFolderAndEndTimestamp() throws Exception {
        try (ProxyCluster cluster = new ProxyCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            prepareIndex(cluster);
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host() + CLUSTERIZE + INTERVAL
                        + PAGE_LOAD_TIMESTAMP + VERSION + TZ_OFFSET + 0
                        + PARENT_FOLDER + DIR1
                        + FROM + (TIMESTAMP + INTERVAL))))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        TestSearchBackend.prepareResult(
                            SIZE + 2 + GROUP
                            + mkgroup(DIR1, TIMESTAMP, 0L)
                            + MAX + (TIMESTAMP + INTERVAL3)
                            + MIN + (TIMESTAMP + INTERVAL3 - 1L)
                            + MERGED + TestSearchBackend.concatDocs(DOC5, DOC4),
                            SIZE + 1 + GROUP
                            + mkgroup(DIR1, TIMESTAMP, 0L)
                            + MAX + (TIMESTAMP + INTERVAL)
                            + MIN + (TIMESTAMP + INTERVAL)
                            + MERGED + TestSearchBackend.concatDocs(DOC3))),
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host() + CLUSTERIZE + INTERVAL
                        + PAGE_LOAD_TIMESTAMP + VERSION + TZ_OFFSET + 0
                        + PARENT_FOLDER + DIR2URI
                        + FROM + (TIMESTAMP + INTERVAL))))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        TestSearchBackend.prepareResult(
                            SIZE + 1 + GROUP
                            + mkgroup(DIR2, TIMESTAMP, 0L)
                            + MAX + (TIMESTAMP + INTERVAL3 + 1L)
                            + MIN + (TIMESTAMP + INTERVAL3 + 1L)
                            + MERGED + TestSearchBackend.concatDocs(DOC9),
                            SIZE + 1 + GROUP
                            + mkgroup(DIR2, TIMESTAMP, 0L)
                            + MAX + (TIMESTAMP + INTERVAL)
                            + MIN + (TIMESTAMP + INTERVAL)
                            + MERGED + TestSearchBackend.concatDocs(DOC8))),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testClusterizeWithCounters() throws Exception {
        try (ProxyCluster cluster = new ProxyCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            prepareIndex(cluster);
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host() + CLUSTERIZE + INTERVAL
                        + PAGE_LOAD_TIMESTAMP + VERSION + TZ_OFFSET + 0
                        + "&count_distinct=platform,event_type")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        TestSearchBackend.prepareResult(
                            SIZE + 1 + GROUP
                            + mkgroup(DIR2, TIMESTAMP, 0L)
                            + MAX + (TIMESTAMP + INTERVAL3 + 1L)
                            + MIN + (TIMESTAMP + INTERVAL3 + 1L)
                            + ",\"counters\":{\"platform\":{\"mac\":1},"
                            + "\"event_type\":{\"fs-copy\":1}}"
                            + MERGED + TestSearchBackend.concatDocs(DOC9),
                            SIZE + 2 + GROUP
                            + mkgroup(DIR1, TIMESTAMP, 0L)
                            + MAX + (TIMESTAMP + INTERVAL3)
                            + MIN + (TIMESTAMP + INTERVAL3 - 1L)
                            + ",\"counters\":{\"platform\":{\"ios\":2},"
                            + "\"event_type\":{\"fs-copy\":1,\"fs-upload\":1}}"
                            + MERGED + TestSearchBackend.concatDocs(DOC5, DOC4),
                            SIZE + (2 + 1) + GROUP
                            + mkgroup(DIR1, TIMESTAMP, 0L)
                            + MAX + (TIMESTAMP + INTERVAL)
                            + MIN + TIMESTAMP
                            + ",\"counters\":{\"platform\":{\"ios\":3},"
                            + "\"event_type\":{\"fs-copy\":2,\"fs-upload\":1}}"
                            + MERGED + TestSearchBackend.concatDocs(DOC3, DOC2, DOC1),
                            SIZE + (2 + 1) + GROUP
                            + mkgroup(DIR2, TIMESTAMP, 0L)
                            + MAX + (TIMESTAMP + INTERVAL)
                            + MIN + TIMESTAMP
                            + ",\"counters\":{\"platform\":{\"ios\":1,"
                            + "\"mac\":2},"
                            + "\"event_type\":{\"fs-copy\":1,\"fs-upload\":2}}"
                            + MERGED + TestSearchBackend.concatDocs(DOC8, DOC7, DOC6),
                            SIZE + 1 + GROUP
                            + mkgroup(DIR1, TIMESTAMP - 1L, 0L)
                            + MAX + (TIMESTAMP - 1L)
                            + MIN + (TIMESTAMP - 1L)
                            + ",\"counters\":{\"platform\":{\"ios\":1},"
                            + "\"event_type\":{\"fs-upload\":1}}"
                            + MERGED + TestSearchBackend.concatDocs(DOC0))),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    private void testClusterizeWithPartialCounters(final String charset)
        throws Exception
    {
        try (ProxyCluster cluster = new ProxyCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            prepareIndex(cluster);
            cluster.backend().add(
                doc(TEN, DIR1) + (TIMESTAMP + INTERVAL + INTERVAL + 1L)
                + ",\"resource_type\":\"video\",\"resource_media_type\":6",
                doc(ELEVEN, DIR1) + (TIMESTAMP + INTERVAL + INTERVAL - 1L)
                + ",\"resource_type\":\"video\"",
                doc(TWELVE, DIR2)
                + (TIMESTAMP + INTERVAL3 + INTERVAL + INTERVAL)
                + ",\"resource_type\":\"image\",\"resource_media_type\":8",
                doc("13", DIR2) + (TIMESTAMP + INTERVAL3 + 2L)
                + ",\"resource_type\":\"image\"",
                doc("14", DIR2) + (TIMESTAMP + INTERVAL3 + INTERVAL + 1L)
                + ",\"resource_media_type\":8",
                doc("15", DIR2) + (TIMESTAMP + INTERVAL3 + INTERVAL - 1L)
                + ",\"user_type\":\"god\"");
            HttpGet get = new HttpGet(
                cluster.proxy().host() + CLUSTERIZE + INTERVAL
                + PAGE_LOAD_TIMESTAMP + VERSION + TZ_OFFSET + MSK_TZ_OFFSET
                + "&count_distinct="
                + "resource_type,resource_media_type,user_type");
            get.addHeader(HttpHeaders.ACCEPT_CHARSET, charset);
            try (CloseableHttpResponse response = client.execute(get)) {
                final int docs = 8;
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                Assert.assertEquals(
                    Charset.forName(charset),
                    CharsetUtils.contentType(response.getEntity())
                        .getCharset());
                YandexAssert.check(
                    new JsonChecker(
                        TestSearchBackend.prepareResult(
                            SIZE + (2 + 2 + 1) + GROUP
                            + mkgroup(DIR2, TIMESTAMP, 0L)
                            + MAX
                            + (TIMESTAMP + INTERVAL3 + INTERVAL + INTERVAL)
                            + MIN + (TIMESTAMP + INTERVAL3 + 1L)
                            + ",\"counters\":{\"resource_media_type\":{\"8\":2"
                            + "},\"resource_type\":{\"image\":2},"
                            + "\"user_type\":{\"god\":1}}"
                            + MERGED
                            + TestSearchBackend.concatDocs(
                                DOC12,
                                DOC14,
                                DOC15,
                                DOC13,
                                DOC9),
                            SIZE + docs + GROUP
                            + mkgroup(DIR1, TIMESTAMP, 0L)
                            + MAX + (TIMESTAMP + INTERVAL3)
                            + MIN + (TIMESTAMP - 1L)
                            + ",\"counters\":{\"resource_media_type\":"
                            + "{\"6\":1},\"resource_type\":{\"video\":2}}"
                            + MERGED + TestSearchBackend.concatDocs(
                                DOC5,
                                DOC4,
                                DOC10,
                                DOC11,
                                DOC3,
                                DOC2,
                                DOC1,
                                DOC0),
                            SIZE + (2 + 1) + GROUP
                            + mkgroup(DIR2, TIMESTAMP, 0L)
                            + MAX + (TIMESTAMP + INTERVAL)
                            + MIN + TIMESTAMP
                            + MERGED
                            + TestSearchBackend.concatDocs(DOC8, DOC7, DOC6))),
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host() + CLUSTERIZE + INTERVAL
                        + PAGE_LOAD_TIMESTAMP + VERSION
                        + TZ_OFFSET + MSK_TZ_OFFSET
                        + "&only_resource_type=video")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        TestSearchBackend.prepareResult(
                            SIZE + 2 + GROUP
                            + mkgroup(DIR1, TIMESTAMP, 0L)
                            + MAX + (TIMESTAMP + INTERVAL + INTERVAL + 1L)
                            + MIN + (TIMESTAMP + INTERVAL + INTERVAL - 1L)
                            + MERGED + TestSearchBackend.concatDocs(DOC10, DOC11))),
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host() + CLUSTERIZE + INTERVAL
                        + PAGE_LOAD_TIMESTAMP + VERSION
                        + TZ_OFFSET + MSK_TZ_OFFSET + PLATFORM + IOS
                        + "&exclude_resource_type=image,video")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        TestSearchBackend.prepareResult(
                            SIZE + 2 + GROUP
                            + mkgroup(DIR2, TIMESTAMP, MSK_TZ_OFFSET)
                            + MAX + (TIMESTAMP + INTERVAL3 + INTERVAL + 1L)
                            + MIN + (TIMESTAMP + INTERVAL3 + INTERVAL + -1L)
                            + MERGED + TestSearchBackend.concatDocs(DOC14, DOC15),
                            SIZE + 2 + GROUP
                            + mkgroup(DIR1, TIMESTAMP, MSK_TZ_OFFSET)
                            + MAX + (TIMESTAMP + INTERVAL3)
                            + MIN + (TIMESTAMP + INTERVAL3 - 1L)
                            + MERGED + TestSearchBackend.concatDocs(DOC5, DOC4),
                            SIZE + (2 + 2) + GROUP
                            + mkgroup(DIR1, TIMESTAMP, MSK_TZ_OFFSET)
                            + MAX + (TIMESTAMP + INTERVAL)
                            + MIN + (TIMESTAMP - 1L)
                            + MERGED
                            + TestSearchBackend.concatDocs(DOC3, DOC2, DOC1, DOC0),
                            SIZE + 1 + GROUP
                            + mkgroup(DIR2, TIMESTAMP, 0L)
                            + MAX + TIMESTAMP
                            + MIN + TIMESTAMP
                            + MERGED + TestSearchBackend.concatDocs(DOC6))),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testClusterizeWithPartialCounters() throws Exception {
        testClusterizeWithPartialCounters("UTF-8");
        testClusterizeWithPartialCounters("UTF-16BE");
    }

    // ignored after http errortype in producer client
    // test should be reconsidered, because it is ambigious
    // failing just on 501 and not failing on timeout makes not much sense
    // we should either require producer response to not return empty clusterization
    // either no require it at all
    @Ignore
    @Test
    public void testClusterizeEmptyIndex() throws Exception {
        try (ProxyCluster cluster = new ProxyCluster(this, true, true);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();

            // No producer, not response
            HttpAssert.assertStatusCode(
                HttpStatus.SC_NOT_IMPLEMENTED,
                cluster.proxy().port(),
                CLUSTERIZE + INTERVAL + PAGE_LOAD_TIMESTAMP + VERSION
                + TZ_OFFSET + 0);

            cluster.producer().add(PRODUCER_URI, HttpStatus.SC_BAD_GATEWAY);
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host() + CLUSTERIZE + INTERVAL
                        + PAGE_LOAD_TIMESTAMP + VERSION + TZ_OFFSET + 0)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(TestSearchBackend.prepareResult()),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    private static String dayDoc(final String id, final long timestamp) {
        return ID + id + "\",\"group_key\":\"/disk/photos1\","
            + "\"platform\":\"mac\",\"event_type\":\"fs-upload\",\"version\":"
            + timestamp + ",\"event_timestamp\":" + timestamp;
    }

    // CSOFF: MagicNumber
    private static String[] prepareDaysIndex(
        final TestSearchBackend lucene,
        final long start)
        throws Exception
    {
        lucene.add(
            dayDoc("1 Jun 1986", start),
            dayDoc("2 Jun 1986", start + MILLIS_PER_DAY),
            dayDoc("3 Jun 1986", start + MILLIS_PER_DAY * 2),
            dayDoc("4 Jun 1986", start + MILLIS_PER_DAY * 3),
            dayDoc("5 Jun 1986", start + MILLIS_PER_DAY * 4),
            dayDoc("6 Jun 1986", start + MILLIS_PER_DAY * 5),
            dayDoc("7 Jun 1986", start + MILLIS_PER_DAY * 6),
            dayDoc("9 Jun 1986", start + MILLIS_PER_DAY * 8),
            dayDoc("10 Jun 1986", start + MILLIS_PER_DAY * 9),
            dayDoc("11 Jun 1986", start + MILLIS_PER_DAY * 10),
            dayDoc("13 Jun 1986", start + MILLIS_PER_DAY * 12),
            dayDoc("14 Jun 1986", start + MILLIS_PER_DAY * 13),
            dayDoc("15 Jun 1986", start + MILLIS_PER_DAY * 14));
        String[] docs = new String[16];
        for (int i = 0; i < docs.length; ++i) {
            docs[i] =
                SIZE + 1 + GROUP
                + mkgroup(DIR1, start + MILLIS_PER_DAY * (i - 1), 0L)
                + MAX + (start + MILLIS_PER_DAY * (i - 1))
                + MIN + (start + MILLIS_PER_DAY * (i - 1)) + MERGED
                + TestSearchBackend.concatDocs(ID + i + " Jun 1986\"");
        }
        return docs;
    }

    @Test
    public void testClusterizeDaysCount() throws Exception {
        try (ProxyCluster cluster = new ProxyCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            // noon 01 June 1986 MSK
            final long start = 517996800000L;
            cluster.start();
            String[] docs = prepareDaysIndex(cluster.backend(), start);
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host() + CLUSTERIZE + INTERVAL
                        + PAGE_LOAD_TIMESTAMP + VERSION + TZ_OFFSET + 0
                        + "&max_amount=9")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        TestSearchBackend.prepareResult(
                            13,
                            docs[15],
                            docs[14],
                            docs[13],
                            docs[11],
                            docs[10],
                            docs[9],
                            docs[7],
                            docs[6],
                            docs[5])),
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host() + CLUSTERIZE + INTERVAL
                        + PAGE_LOAD_TIMESTAMP + VERSION + TZ_OFFSET + 0
                        + "&max_amount=12")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        TestSearchBackend.prepareResult(
                            13,
                            docs[15],
                            docs[14],
                            docs[13],
                            docs[11],
                            docs[10],
                            docs[9],
                            docs[7],
                            docs[6],
                            docs[5],
                            docs[4],
                            docs[3],
                            docs[2])),
                    CharsetUtils.toString(response.getEntity()));
            }
            // check that 'since' filter won't break end_timestamp filter
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host() + CLUSTERIZE + INTERVAL
                        + PAGE_LOAD_TIMESTAMP + VERSION + TZ_OFFSET + 0
                        + "&max_amount=3&end_timestamp="
                        + (start + MILLIS_PER_DAY * 13))))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        TestSearchBackend.prepareResult(docs[15], docs[14])),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testClusterizeDaysCountFat() throws Exception {
        try (ProxyCluster cluster = new ProxyCluster(this, false, false, 3);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            // noon 01 June 1986 MSK
            final long start = 517996800000L;
            cluster.start();
            String[] docs = prepareDaysIndex(cluster.backend(), start);
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host() + CLUSTERIZE + INTERVAL
                        + PAGE_LOAD_TIMESTAMP + VERSION + TZ_OFFSET + 0)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        TestSearchBackend.prepareResult(
                            docs[15],
                            docs[14],
                            docs[13],
                            docs[11],
                            docs[10],
                            docs[9])),
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host() + CLUSTERIZE + INTERVAL
                        + PAGE_LOAD_TIMESTAMP + (start + MILLIS_PER_DAY * 12)
                        + TZ_OFFSET + 0)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        TestSearchBackend.prepareResult(
                            docs[13],
                            docs[11],
                            docs[10],
                            docs[9],
                            docs[7])),
                    CharsetUtils.toString(response.getEntity()));
            }
            // check proper fat check for non-zero offset
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host() + CLUSTERIZE + INTERVAL
                        + PAGE_LOAD_TIMESTAMP + VERSION + TZ_OFFSET + 0
                        + "&offset=1" + "&max_amount=1")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        TestSearchBackend.prepareResult(
                            9,
                            docs[14])),
                    CharsetUtils.toString(response.getEntity()));
            }
            // check fat user counters
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host() + COUNTERS_URI + EVENT_TYPE
                        + PAGE_LOAD_TIMESTAMP + VERSION)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker("{\"fs-upload\":6}"),
                    CharsetUtils.toString(response.getEntity()));
            }
            // check fat user counters override by additional filter
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host() + COUNTERS_URI + EVENT_TYPE
                        + PAGE_LOAD_TIMESTAMP + VERSION + "&platform=mac")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker("{\"fs-upload\":13}"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testClusterizeDaysCountTimezoneOffset() throws Exception {
        try (ProxyCluster cluster = new ProxyCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            // midnight 1 June 2016 UTC, elements will be paired because of MSK
            // timezone offset
            final long start = 1464739200000L;
            final long end = start + MILLIS_PER_DAY * 14;
            String ts =
                "\",\"group_key\":\"/disk/photoslice \\\"тесты\\\"/te\\\\\","
                + "\"version\":" + end + ",\"event_timestamp\"" + ':';
            cluster.start();
            cluster.backend().add(
                "\"id\":\"1 Jun 2016" + ts + (start - 1),
                "\"id\":\"2 Jun 2016" + ts + start,
                "\"id\":\"3 Jun 2016" + ts + (start + MILLIS_PER_DAY * 2 - 1),
                "\"id\":\"4 Jun 2016" + ts + (start + MILLIS_PER_DAY * 2),
                "\"id\":\"5 Jun 2016" + ts + (start + MILLIS_PER_DAY * 4 - 1),
                "\"id\":\"6 Jun 2016" + ts + (start + MILLIS_PER_DAY * 4),
                "\"id\":\"7 Jun 2016" + ts + (start + MILLIS_PER_DAY * 6 - 1),
                "\"id\":\"8 Jun 2016" + ts + (start + MILLIS_PER_DAY * 6),
                "\"id\":\"9 Jun 2016" + ts + (start + MILLIS_PER_DAY * 8 - 1),
                "\"id\":\"10 Jun 2016" + ts + (start + MILLIS_PER_DAY * 8),
                "\"id\":\"11 Jun 2016" + ts
                + (start + MILLIS_PER_DAY * 10 - 1),
                "\"id\":\"12 Jun 2016" + ts + (start + MILLIS_PER_DAY * 10),
                "\"id\":\"13 Jun 2016" + ts
                + (start + MILLIS_PER_DAY * 12 - 1),
                "\"id\":\"14 Jun 2016" + ts + (start + MILLIS_PER_DAY * 12),
                "\"id\":\"15 Jun 2016" + ts
                + (start + MILLIS_PER_DAY * 14 - 1));
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host() + CLUSTERIZE + INTERVAL
                        + PAGE_LOAD_TIMESTAMP + end + TZ_OFFSET
                        + MSK_TZ_OFFSET + "&max_amount=3")))
            {
                YandexAssert.check(
                    new JsonChecker(
                        TestSearchBackend.prepareResult(
                            // Note that this is less than total clusters
                            // count (8), because search scope was reduced by
                            // event_timestamp_day smart filter
                            6,
                            SIZE + 1 + GROUP
                            + mkgroup(
                                DIR2,
                                start + MILLIS_PER_DAY * 14 - 1,
                                MSK_TZ_OFFSET)
                            + MAX + (start + MILLIS_PER_DAY * 14 - 1)
                            + MIN + (start + MILLIS_PER_DAY * 14 - 1)
                            + MERGED
                            + TestSearchBackend.concatDocs("\"id\":\"15 Jun 2016\""),
                            SIZE + 2 + GROUP
                            + mkgroup(DIR2, start + MILLIS_PER_DAY * 12, 0L)
                            + MAX + (start + MILLIS_PER_DAY * 12)
                            + MIN + (start + MILLIS_PER_DAY * 12 - 1)
                            + MERGED
                            + TestSearchBackend.concatDocs(
                                "\"id\":\"14 Jun 2016\"",
                                "\"id\":\"13 Jun 2016\""),
                            SIZE + 2 + GROUP
                            + mkgroup(DIR2, start + MILLIS_PER_DAY * 10, 0L)
                            + MAX + (start + MILLIS_PER_DAY * 10)
                            + MIN + (start + MILLIS_PER_DAY * 10 - 1)
                            + MERGED
                            + TestSearchBackend.concatDocs(
                                "\"id\":\"12 Jun 2016\"",
                                "\"id\":\"11 Jun 2016\""))),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testClusterizeMergedLength() throws Exception {
        try (ProxyCluster cluster = new ProxyCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            // 15 June 2016 10 AM MSK
            final long start = 1465974000000L;
            cluster.start();
            // documents will be collected in order, so clusters will be merged
            String ts =
                "\",\"group_key\":\"/disk/photos1\",\"version\":2,\""
                + "event_timestamp\":";
            cluster.backend().add(
                "\"id\":\"a" + ts + start,
                "\"id\":\"b" + ts + (start + INTERVAL),
                "\"id\":\"c" + ts + (start + INTERVAL * 2),
                "\"id\":\"e" + ts + (start + INTERVAL * 4),
                "\"id\":\"f" + ts + (start + INTERVAL * 5),
                "\"id\":\"g" + ts + (start + INTERVAL * 6),
                "\"id\":\"h" + ts + (start + INTERVAL * 7),
                "\"id\":\"i" + ts + (start + INTERVAL * 8),
                "\"id\":\"d" + ts + (start + INTERVAL * 3));
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host() + CLUSTERIZE + INTERVAL
                        + PAGE_LOAD_TIMESTAMP
                            .replace("get=id", "get=id,group_id")
                        + VERSION + TZ_OFFSET + 0
                        + "&max_events_per_group=4&asc")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        TestSearchBackend.prepareResult(
                            SIZE + 9 + GROUP
                            + mkgroup(DIR1, start, 0L)
                            + MAX + (start + INTERVAL * 8)
                            + MIN + start
                            + MERGED
                            + TestSearchBackend.concatDocs(
                                "\"id\":\"a\",\"group_id\":null",
                                "\"id\":\"b\",\"group_id\":null",
                                "\"id\":\"c\",\"group_id\":null",
                                "\"id\":\"d\",\"group_id\":null"))),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }
    // CSON: MagicNumber

    @Test
    public void testGroup() throws Exception {
        try (ProxyCluster cluster = new ProxyCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            prepareIndex(cluster);
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host() + GROUP_URI + DIR1
                        + PAGE_LOAD_TIMESTAMP + VERSION
                        + FROM + (TIMESTAMP - 1L)
                        + TO + (TIMESTAMP + INTERVAL))))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        TestSearchBackend.prepareResult(DOC3, DOC2, DOC1, DOC0)),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testGroupPaging() throws Exception {
        try (ProxyCluster cluster = new ProxyCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            prepareIndex(cluster);
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host() + GROUP_URI + DIR2URI
                        + PAGE_LOAD_TIMESTAMP + (VERSION - 2L - 2L - 2L - 1L)
                        + FROM + 0
                        + TO + (TIMESTAMP + INTERVAL3 + 1L)
                        + "&offset=1&max_amount=1")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(TestSearchBackend.prepareResult(2 + 1, DOC8)),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testSearch() throws Exception {
        try (ProxyCluster cluster = new ProxyCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            prepareIndex(cluster);
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host() + SEARCH_URI + NINE
                        + PAGE_LOAD_TIMESTAMP + VERSION)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        TestSearchBackend.prepareResultWithPrefix(
                            ",\"search-options\":{\"request\":\"9\"}",
                            DOC9)),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testSearchComplexRequestAndPaging() throws Exception {
        try (ProxyCluster cluster = new ProxyCluster(this, true);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            prepareIndex(cluster);
            HttpGet get = new HttpGet(
                cluster.proxy().host() + SEARCH_URI + "photos1+-1+-3"
                + PAGE_LOAD_TIMESTAMP + VERSION + "&offset=1&max_amount=2");
            HttpAssert.assertStatusCode(
                HttpStatus.SC_NOT_IMPLEMENTED,
                client,
                get);
            cluster.producer().add(PRODUCER_URI, "[{\"localhost\":100500}]");
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        TestSearchBackend.prepareResultWithPrefix(
                            2 + 2,
                            ",\"search-options\":"
                            + "{\"request\":\"photos1 -1 -3\"}",
                            DOC4,
                            DOC2)),
                    CharsetUtils.toString(response.getEntity()));
            }
            cluster.producer().add(PRODUCER_URI, "[]");
            HttpAssert.assertStatusCode(HttpStatus.SC_NOT_FOUND, client, get);
        }
    }

    @Test
    public void testSearchFilters() throws Exception {
        try (ProxyCluster cluster = new ProxyCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            prepareIndex(cluster);
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host() + SEARCH_URI + "photo"
                        + PAGE_LOAD_TIMESTAMP + VERSION
                        + PARENT_FOLDER + DIR1)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        TestSearchBackend.prepareResultWithPrefix(
                            ",\"search-options\":{\"request\":\"photo\"}",
                            DOC5,
                            DOC4,
                            DOC3,
                            DOC2,
                            DOC1,
                            DOC0)),
                    CharsetUtils.toString(response.getEntity()));
            }
            String emptyOptions = ",\"search-options\":{\"request\":\"\"}";
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host() + "/ipdd/search?uid=0"
                        + PAGE_LOAD_TIMESTAMP + VERSION
                        + PARENT_FOLDER + DIR2URI
                        + EVENT_TYPE_PARAM + FS_COPY)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        TestSearchBackend.prepareResultWithPrefix(
                            emptyOptions,
                            DOC9,
                            DOC8)),
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host() + SEARCH_URI
                        + PAGE_LOAD_TIMESTAMP + VERSION
                        + PARENT_FOLDER + DIR2URI
                        + EVENT_TYPE_PARAM + FS_COPY)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        TestSearchBackend.prepareResultWithPrefix(
                            emptyOptions,
                            DOC9,
                            DOC8)),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testSearchMorphology() throws Exception {
        try (ProxyCluster cluster = new ProxyCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            prepareIndex(cluster);
            cluster.backend().add(
                doc(TEN, DIR1, MAC, FS_COPY) + (TIMESTAMP + INTERVAL3 + 1L)
                + ",\"product_name_ru\":\"мира\"");
            cluster.backend().add(
                doc(ELEVEN, DIR1, MAC, FS_COPY)
                    .replace(DIR1 + '/' + ELEVEN, "a\u00a0bc") + TIMESTAMP);
            String erratumUri =
                "/misspell.json/check?srv=disk-search"
                + "&options=321&text=\"мир\"";
            cluster.erratum().add(
                erratumUri,
                "{\"code\":200,\"lang\":\"ru,en\",\"rule\":\"\","
                + "\"flags\":0,\"r\":0}");
            cluster.start();
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host() + SEARCH_URI + "+миры++"
                        + PAGE_LOAD_TIMESTAMP + VERSION
                        + PARENT_FOLDER + DIR1)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        TestSearchBackend.prepareResultWithPrefix(
                            ",\"search-options\":{\"request\":\" миры  \"}",
                            DOC10)),
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host() + SEARCH_URI + "%22мир%22"
                        + PAGE_LOAD_TIMESTAMP + VERSION
                        + PARENT_FOLDER + DIR1)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        TestSearchBackend.prepareResultWithPrefix(
                            ",\"search-options\":{\"request\":"
                            + "\"\\\"мир\\\"\"}")),
                    CharsetUtils.toString(response.getEntity()));
            }
            Assert.assertEquals(1, cluster.erratum().accessCount(erratumUri));
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host() + SEARCH_URI + "%c2%a0b"
                        + PAGE_LOAD_TIMESTAMP + VERSION
                        + PARENT_FOLDER + DIR1)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        TestSearchBackend.prepareResultWithPrefix(
                            ",\"search-options\":{\"request\":\"\\u00a0b\"}",
                            DOC11)),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testSearchMisspell() throws Exception {
        try (ProxyCluster cluster = new ProxyCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            prepareIndex(cluster);
            cluster.erratum().add(
                "/misspell.json/check?srv=disk-search&options=321&text=ghbdtn",
                "{\"code\":201,\"lang\":\"ru,en\",\"rule\":\"KeyboardLayout\","
                + "\"flags\":1024,\"r\":10000,\"srcText\":\"ghbdtn\","
                + "\"text\":\"привет\"}");
            cluster.start();
            String request =
                cluster.proxy().host() + SEARCH_URI + "ghbdtn"
                + PAGE_LOAD_TIMESTAMP + VERSION
                + PARENT_FOLDER + DIR1;
            try (CloseableHttpResponse response =
                    client.execute(new HttpGet(request)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        TestSearchBackend.prepareResultWithPrefix(
                            ",\"search-options\":{\"request\":\"ghbdtn\"}")),
                    CharsetUtils.toString(response.getEntity()));
            }
            cluster.backend().add(
                doc(TEN, DIR1, MAC, FS_COPY) + (TIMESTAMP + INTERVAL3 + 1L)
                + ",\"product_name_ru\":\"привет\"");
            try (CloseableHttpResponse response =
                    client.execute(new HttpGet(request)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        TestSearchBackend.prepareResultWithPrefix(
                            ",\"search-options\":{\"request\":\"привет\","
                            + "\"rule\":\"KeyboardLayout\"}",
                            DOC10)),
                    CharsetUtils.toString(response.getEntity()));
            }
            cluster.backend().add(
                doc(ELEVEN, DIR1, MAC, FS_COPY) + (TIMESTAMP + INTERVAL3 + 1L)
                + ",\"product_name_ru\":\"ghbdtn\"");
            try (CloseableHttpResponse response =
                    client.execute(new HttpGet(request)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        TestSearchBackend.prepareResultWithPrefix(
                            ",\"search-options\":{\"request\":\"ghbdtn\","
                            + "\"suggest\":\"привет\""
                            + ",\"rule\":\"KeyboardLayout\"}",
                            DOC11)),
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response =
                    client.execute(new HttpGet(request + "&force")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        TestSearchBackend.prepareResultWithPrefix(
                            ",\"search-options\":{\"request\""
                            + ":\"ghbdtn\"}",
                            DOC11)),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testRawSearch() throws Exception {
        try (ProxyCluster cluster = new ProxyCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            prepareIndex(cluster);
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host() + RAW_SEARCH_URI
                        + "platform:mac+OR+event_timestamp:" + TIMESTAMP
                        + PAGE_LOAD_TIMESTAMP)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        TestSearchBackend.prepareResultWithPrefix(
                            ",\"search-options\":{\"request\":\""
                            + "platform:mac OR event_timestamp:" + TIMESTAMP
                            + "\"}",
                            DOC9,
                            DOC8,
                            DOC7,
                            DOC6,
                            DOC2,
                            DOC1)),
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host() + RAW_SEARCH_URI
                        + "platform:bsd" + PAGE_LOAD_TIMESTAMP)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        TestSearchBackend.prepareResultWithPrefix(
                            ",\"search-options\":{"
                            + "\"request\":\"platform:bsd\"}")),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testCounters() throws Exception {
        try (ProxyCluster cluster = new ProxyCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            cluster.backend().add(
                doc(ELEVEN, DIR1) + TIMESTAMP
                + ",\"product_name_ru\":\"суть\"");
            prepareIndex(cluster);
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host() + COUNTERS_URI + EVENT_TYPE
                        + PAGE_LOAD_TIMESTAMP + VERSION)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker("{\"fs-copy\":5,\"fs-upload\":5}"),
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host() + COUNTERS_URI
                        + "event_type&platform=mac"
                        + PAGE_LOAD_TIMESTAMP + VERSION)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker("{\"fs-copy\":2,\"fs-upload\":1}"),
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host() + COUNTERS_URI + "platform"
                        + PAGE_LOAD_TIMESTAMP + VERSION)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker("{\"ios\":7,\"mac\":3}"),
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host() + COUNTERS_URI
                        + "product_name_ru" + PAGE_LOAD_TIMESTAMP + VERSION)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker("{\"суть\":1}"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }
}

