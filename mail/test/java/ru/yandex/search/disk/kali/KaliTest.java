package ru.yandex.search.disk.kali;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.http.Header;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.collection.Pattern;
import ru.yandex.devtools.test.Paths;
import ru.yandex.function.GenericConsumer;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.HeaderValidator;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.test.NotImplementedHttpItem;
import ru.yandex.http.test.SlowpokeHttpItem;
import ru.yandex.http.test.StaticHttpItem;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.http.test.ValidatingHttpItem;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.http.util.EmptyFutureCallback;
import ru.yandex.http.util.YandexHeaders;
import ru.yandex.http.util.nio.BasicAsyncRequestProducerGenerator;
import ru.yandex.http.util.nio.client.AsyncClient;
import ru.yandex.http.util.nio.client.SharedConnectingIOReactor;
import ru.yandex.parser.searchmap.User;
import ru.yandex.search.prefix.LongPrefix;
import ru.yandex.test.search.backend.TestSearchBackend;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

// CSOFF: MultipleStringLiterals
public class KaliTest extends TestBase {
    private static final long SHARDS = 65534L;
    private static final String RESOURCE =
        "/api/v1/indexer/resources?uid=";
    private static final String DISK_QUEUE = "disk_queue";
    private static final String PHOTOSLICE = "photoslice";
    private static final String MKFILE = "&action=mkfile&service=";
    private static final String REINDEX = "&action=reindex&service=";
    private static final String RM = "&action=rm&service=";
    private static final String GET = "/get/";
    private static final String RAW = "?raw";
    private static final String ID = "/?zoo-queue-id=42&id=";
    private static final String PREFIX = "&prefix=";
    private static final String SEARCH = "/search?get=id,key&prefix=";
    private static final String ID_FIELD = "\"id\":\"";
    private static final String KEY_FIELD = "\",\"key\":\"";
    private static final String CALLBACK = "&callback=";
    private static final String VERSION = "&version=";
    private static final String TEXT_ID = "&text=id:";
    private static final String MIMETYPES = "&get=mimetype,tikaite_mimetype";
    private static final String RESOURCE_ID = "&resourceId=";
    private static final String REQUEST_RESOURCE_ID = "&resource_id=";
    private static final String STID = "\",\"stid\":\"";
    private static final String GET_STID = "&get=stid";
    private static final String INDEXED_PREFIX = "indexed_";
    private static final String INDEXED_VERSION_FIELD =
        "\",\"indexed_version\":\"";
    private static final String GET_INDEXED_VERSION =
        "&get=indexed_version,-key";
    private static final long TIMEOUT = 2000L;
    private static final String EXCEL_FILE = "excel.xls";
    private static final String EXCEL = "excel.djfs.json";
    private static final String EXCEL2 = "excel2.djfs.json";
    private static final String EXCEL_ID =
        "568acebb9a7bbdae6d37d677329eb1f93b5f7c61ea8190a900ed723bbda68da7";
    private static final String EXCEL_STID =
        "320.yadisk:22520228.E1336333:2394660087104244723015318716817";
    private static final String EXCEL_SEARCH_RESULT =
        TestSearchBackend.prepareResult(
            ID_FIELD + EXCEL_ID + KEY_FIELD
            + "/disk/Файлы/Папка с таблицами/Какая-то таблица.xls\"");
    private static final long EXCEL_VERSION = 1524580053145871L;
    private static final long EXCEL_UID = 92713204L;
    private static final long EXCEL_OWNER_UID = 22520228;

    // CSOFF: MethodLength
    @Test
    public void testIndexFile() throws Exception {
        try (KaliCluster cluster = new KaliCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            TestSearchBackend lucene = cluster.lucene();
            final long position = 123223;
            User user = new User(DISK_QUEUE, new LongPrefix(EXCEL_UID));
            cluster.djfs().add(
                RESOURCE + EXCEL_UID + RESOURCE_ID + EXCEL_OWNER_UID
                + ':' + EXCEL_ID,
                new ValidatingHttpItem(
                    new StaticHttpItem(
                        HttpStatus.SC_OK,
                        new FileEntity(
                            new File(getClass().getResource(EXCEL).toURI()),
                            ContentType.APPLICATION_JSON)),
                    new HeaderValidator(
                        new BasicHeader(
                            YandexHeaders.X_YANDEX_QUEUE_MESSAGE_ID,
                            DISK_QUEUE + '@' + (EXCEL_UID % SHARDS)
                            + '@' + position))
                        .andThen(
                            new HeaderValidator(
                                KaliCluster.DJFS_TVM2_HEADER))),
                new ValidatingHttpItem(
                    new StaticHttpItem(
                        HttpStatus.SC_OK,
                        new FileEntity(
                            new File(getClass().getResource(EXCEL).toURI()),
                            ContentType.APPLICATION_JSON)),
                    new HeaderValidator(
                        new BasicHeader(
                            YandexHeaders.X_YANDEX_QUEUE_MESSAGE_ID,
                            DISK_QUEUE + '@' + (EXCEL_UID % SHARDS)
                            + '@' + (position + 2 + 1)))
                        .andThen(
                            new HeaderValidator(
                                KaliCluster.DJFS_TVM2_HEADER))),
                new ValidatingHttpItem(
                    new StaticHttpItem(
                        HttpStatus.SC_OK,
                        new FileEntity(
                            new File(getClass().getResource(EXCEL2).toURI()),
                            ContentType.APPLICATION_JSON)),
                    new HeaderValidator(
                        KaliCluster.DJFS_TVM2_HEADER)));
            cluster.lenulca().add(
                GET + EXCEL_STID + RAW,
                new File(getClass().getResource(EXCEL_FILE).toURI()));
            cluster.start();

            List<Header> headers = new ArrayList<>();
            headers.add(
                new BasicHeader(
                    YandexHeaders.ZOO_SHARD_ID,
                    Long.toString(EXCEL_UID % SHARDS)));
            headers.add(
                new BasicHeader(
                    YandexHeaders.ZOO_QUEUE,
                    DISK_QUEUE));
            headers.add(
                new BasicHeader(
                    YandexHeaders.ZOO_QUEUE_ID,
                    Long.toString(position)));

            String suffix =
                VERSION + EXCEL_VERSION + REQUEST_RESOURCE_ID
                + EXCEL_OWNER_UID + ':' + EXCEL_ID;
            String mkfile =
                cluster.kali().host() + ID + EXCEL_ID + PREFIX + EXCEL_UID
                + MKFILE + DISK_QUEUE + suffix;
            String rm =
                cluster.kali().host() + ID + EXCEL_ID + PREFIX + EXCEL_UID
                + RM + DISK_QUEUE + suffix;
            String request1 = "&text=ext:xls";
            String request2 = "&text=body_text:Колонки";

            Assert.assertEquals(-1L, lucene.getQueueId(user));

            // Add document
            HttpGet get = new HttpGet(mkfile);
            for (Header header: headers) {
                get.addHeader(header);
            }
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, get);
            lucene.checkSearch(
                SEARCH + EXCEL_UID + request1,
                EXCEL_SEARCH_RESULT);
            lucene.checkSearch(
                SEARCH + EXCEL_UID + request2,
                EXCEL_SEARCH_RESULT);
            Assert.assertEquals(position, lucene.getQueueId(user));

            // Remove document
            get = new HttpGet(rm);
            headers.set(
                2,
                new BasicHeader(
                    YandexHeaders.ZOO_QUEUE_ID,
                    Long.toString(position + 2)));
            for (Header header: headers) {
                get.addHeader(header);
            }
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, get);
            lucene.checkSearch(
                SEARCH + EXCEL_UID + request1,
                TestSearchBackend.prepareResult());
            Assert.assertEquals(position + 2, lucene.getQueueId(user));
            headers.add(
                new BasicHeader(
                    YandexHeaders.ZOO_QUEUE_ID,
                    Integer.toString(Integer.MIN_VALUE)));

            // Remove document once again, check that position updated
            get =
                new HttpGet(
                    rm.replace(
                        Long.toString(EXCEL_VERSION),
                        Long.toString(EXCEL_VERSION + 1)));
            headers.set(
                2,
                new BasicHeader(
                    YandexHeaders.ZOO_QUEUE_ID,
                    Long.toString(position + 2 + 1)));
            for (Header header: headers) {
                get.addHeader(header);
            }
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, get);
            lucene.checkSearch(
                SEARCH + EXCEL_UID + request1,
                TestSearchBackend.prepareResult());
            Assert.assertEquals(position + 2 + 1, lucene.getQueueId(user));
            Assert.assertEquals(
                -1L,
                lucene.getQueueId(
                    new User(PHOTOSLICE, new LongPrefix(EXCEL_UID))));

            String malformedSignal = "malformed-action-order_ammm";
            String skippedSignal = "version-skipped-requests_ammm";

            // Will silently fail, reporting with skipped signal
            get = new HttpGet(mkfile);
            for (Header header: headers) {
                get.addHeader(header);
            }
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, get);
            // Nothing added because of indexed action type check
            lucene.checkSearch(
                SEARCH + EXCEL_UID + request1,
                TestSearchBackend.prepareResult());
            HttpAssert.assertStat(
                malformedSignal,
                Integer.toString(0),
                cluster.kali().port());
            HttpAssert.assertStat(
                skippedSignal,
                Integer.toString(1),
                cluster.kali().port());

            // Will silently fail, reporting with malformed order signal
            get =
                new HttpGet(
                    mkfile.replace(
                        Long.toString(EXCEL_VERSION),
                        Long.toString(EXCEL_VERSION + 2)));
            for (Header header: headers) {
                get.addHeader(header);
            }
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, get);
            // Nothing added because of indexed action type check
            lucene.checkSearch(
                SEARCH + EXCEL_UID + request1,
                TestSearchBackend.prepareResult());
            HttpAssert.assertStat(
                malformedSignal,
                Integer.toString(1),
                cluster.kali().port());
            HttpAssert.assertStat(
                skippedSignal,
                Integer.toString(1),
                cluster.kali().port());
        }
    }
    // CSON: MethodLength

    @Test
    public void testWatchdog() throws Exception {
        try (KaliCluster cluster = new KaliCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.djfs().add(
                RESOURCE + EXCEL_UID + RESOURCE_ID + EXCEL_OWNER_UID
                + ':' + EXCEL_ID,
                new StaticHttpResource(
                    new StaticHttpItem(
                        HttpStatus.SC_OK,
                        new FileEntity(
                            new File(getClass().getResource(EXCEL).toURI()),
                            ContentType.APPLICATION_JSON))));
            cluster.lenulca().add(
                GET + EXCEL_STID + RAW,
                new File(getClass().getResource(EXCEL_FILE).toURI()));
            cluster.start();
            List<Header> headers = new ArrayList<>();
            headers.add(
                new BasicHeader(
                    YandexHeaders.ZOO_SHARD_ID,
                    Long.toString(EXCEL_UID % SHARDS)));
            headers.add(
                new BasicHeader(
                    YandexHeaders.ZOO_QUEUE,
                    DISK_QUEUE));
            String suffix =
                REQUEST_RESOURCE_ID + EXCEL_OWNER_UID + ':' + EXCEL_ID;
            String mkfile =
                cluster.kali().host() + ID + EXCEL_ID + PREFIX + EXCEL_UID
                + MKFILE + DISK_QUEUE + suffix + "&watchdog";

            // Add document for the first time
            HttpPost post = new HttpPost(mkfile + VERSION + EXCEL_VERSION);
            post.setEntity(new ByteArrayEntity(new byte[0]));
            for (Header header: headers) {
                post.addHeader(header);
            }
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);
            Assert.assertEquals(
                1,
                cluster.lenulca().accessCount(GET + EXCEL_STID + RAW));

            // Add document again
            HttpGet get = new HttpGet(mkfile + VERSION + (EXCEL_VERSION + 1));
            for (Header header: headers) {
                get.addHeader(header);
            }
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, get);
            Assert.assertEquals(
                2,
                cluster.lenulca().accessCount(GET + EXCEL_STID + RAW));
        }
    }

    private void testIndexImage(final String action) throws Exception {
        try (KaliCluster cluster = new KaliCluster(this)) {
            TestSearchBackend lucene = cluster.lucene();
            final String id =
                "a51c65feac7f8fe0a114b0d9a8d10a0a7"
                + "a0ce0d630db5736d7619e2a0357705c";
            String stid =
                "320.yadisk:5598601.E1329136:142985201065327062225295744981";
            final long uid = 203889311L;
            User user = new User(PHOTOSLICE, new LongPrefix(uid));
            cluster.djfs().add(
                RESOURCE + uid + "&resourceId=203889311:" + id,
                new File(
                    getClass()
                        .getResource("lepestrichestvo.djfs.json").toURI()));
            cluster.lenulca().add(
                GET + stid + RAW,
                new File(
                    Paths.getSandboxResourcesRoot() + "/lepestrichestvo.JPG"));
            String callbackUri = "/callback";
            GenericConsumer<Object, Exception> noQueueId =
                x -> Assert.assertEquals(-1, lucene.getQueueId(user));
            String key = "/disk/Всё подряд/lepestrichestvo.JPG\"";
            GenericConsumer<Object, Exception> payloadCheck = x -> {
                if (!(x instanceof HttpEntityEnclosingRequest)) {
                    throw new Exception(
                        "Expecting post request");
                }

                HttpEntityEnclosingRequest req =
                    (HttpEntityEnclosingRequest) x;

                YandexAssert.check(
                    new JsonChecker(
                        "[{\"height\": 3264," +
                            "\"latitude\": 55.664822," +
                            "\"longitude\": 37.489869," +
                            "\"altitude\": 193,"
                            + "\"id\":\"" + id
                            + "\",\"orientation\":\"portrait\","
                            + "\"width\": 2448}]"),
                    CharsetUtils.toString(req.getEntity()));
            };

            GenericConsumer<Object, Exception> luceneCheck =
                x -> lucene.checkSearch(
                    SEARCH + uid
                        + "&text=ocr_text:%28куча+текста%29+AND"
                        + "+model:iphone&get=ocr_text,manufacturer,name",
                    TestSearchBackend.prepareResult(
                        ID_FIELD + id + "\",\"ocr_text\":\""
                            + KaliCluster.OCR_TEXT
                            + "\",\"manufacturer\":\"Apple\","
                            + "\"name\":\"lepestrichestvo.JPG"
                            + KEY_FIELD + key));

            cluster.callbacks().add(
                callbackUri,
                new ValidatingHttpItem(
                    StaticHttpItem.OK,
                    noQueueId.andThen(payloadCheck).andThen(luceneCheck)),
                NotImplementedHttpItem.INSTANCE);
            String cvCallbackUri = "/cv-callback?params";
            cluster.callbacks().add(
                cvCallbackUri,
                StaticHttpItem.OK,
                NotImplementedHttpItem.INSTANCE);
            String ocrCallbackUri = "/ocr/callback?a=b&c=d";
            cluster.callbacks().add(
                ocrCallbackUri,
                StaticHttpItem.OK,
                NotImplementedHttpItem.INSTANCE);
            cluster.start();

            final long position = 3423421L;

            // Add document
            HttpGet get = new HttpGet(
                cluster.kali().host() + ID + id + PREFIX + uid
                + action + PHOTOSLICE
                + "&version=1524580049581668&resource_id=203889311:" + id
                + CALLBACK + cluster.callbacks().host()
                + "/callback&cv_callback=" + cluster.callbacks().host()
                + "/cv-callback?params&ocr_callback="
                + cluster.callbacks().host()
                + "/ocr/callback%3Fa%3Db%26c=d");
            get.addHeader(
                YandexHeaders.ZOO_SHARD_ID,
                Long.toString(uid % SHARDS));
            get.addHeader(YandexHeaders.ZOO_QUEUE, PHOTOSLICE);
            get.addHeader(YandexHeaders.ZOO_QUEUE_ID, Long.toString(position));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, get);
            lucene.checkSearch(
                SEARCH + uid + "&text=faces:1",
                TestSearchBackend.prepareResult(ID_FIELD + id + KEY_FIELD + key));
            Assert.assertEquals(
                1,
                cluster.callbacks().accessCount(callbackUri));
            Assert.assertEquals(
                1,
                cluster.callbacks().accessCount(cvCallbackUri));
            Assert.assertEquals(
                1,
                cluster.callbacks().accessCount(ocrCallbackUri));
            Assert.assertEquals(position, lucene.getQueueId(user));
        }
    }

    @Test
    public void testIndexImage() throws Exception {
        testIndexImage(MKFILE);
        testIndexImage(REINDEX);
    }

    @Test
    public void testIndexDir() throws Exception {
        try (KaliCluster cluster = new KaliCluster(this)) {
            String id =
                "3c3d09dfd82558cbd7e1d4b67e7d7f447"
                + "f7d11b830e9b5163dc7398887057b3b";
            final long uid = 5598601;
            cluster.djfs().add(
                RESOURCE + uid + "&resourceId=5598601:" + id,
                new ValidatingHttpItem(
                    new StaticHttpItem(
                        HttpStatus.SC_OK,
                        new FileEntity(
                            new File(
                                getClass().getResource("ping-pong.djfs.json")
                                    .toURI()),
                            ContentType.APPLICATION_JSON)),
                    new HeaderValidator(
                        new BasicHeader(
                            YandexHeaders.X_YANDEX_QUEUE_MESSAGE_ID,
                            DISK_QUEUE + '@' + (uid % SHARDS) + "@42"))
                        .andThen(
                            new HeaderValidator(
                                KaliCluster.DJFS_TVM2_HEADER))));
            String zooHashPrefix =
                "6469736b5f7175657565f000000000000002af0000000";
            String callback1 = "/callback1?timestamp=1234567890";
            cluster.callbacks().add(
                callback1,
                new StaticHttpResource(
                    new ValidatingHttpItem(
                        new StaticHttpItem(HttpStatus.SC_OK),
                        new HeaderValidator(
                            new BasicHeader(
                                YandexHeaders.ZOO_HASH,
                                zooHashPrefix + 0)))));
            String callback2 = "/callback2?param&timestamp=1234567890";
            cluster.callbacks().add(
                callback2,
                new StaticHttpResource(
                    new ValidatingHttpItem(
                        new StaticHttpItem(HttpStatus.SC_OK),
                        new HeaderValidator(
                            new BasicHeader(
                                YandexHeaders.ZOO_HASH,
                                zooHashPrefix + 1))
                                .andThen(
                                    new HeaderValidator(
                                        new BasicHeader(
                                            YandexHeaders.CHECK_DUPLICATE,
                                            "true"))))));
            cluster.start();

            // Add document
            HttpGet get = new HttpGet(
                cluster.kali().host() + ID + id + PREFIX + uid
                + MKFILE + PHOTOSLICE
                + "&version=1410580049581668&resource_id=5598601:" + id
                + CALLBACK + cluster.callbacks().host()
                + "/callback1&callback=" + cluster.callbacks().host()
                + callback2);
            get.addHeader(
                YandexHeaders.ZOO_SHARD_ID,
                Long.toString(uid % SHARDS));
            get.addHeader(YandexHeaders.ZOO_QUEUE, DISK_QUEUE);
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, get);
            cluster.lucene().checkSearch(
                SEARCH + uid
                + "&text=visible:0+AND+type:dir&get=timestamp,version,folder",
                TestSearchBackend.prepareResult(
                    ID_FIELD + id + "\",\"timestamp\":\"1234567890\","
                    // version got from DJFS response
                    + "\"version\":\"1411399421404672"
                    + KEY_FIELD + "/disk/Настольный теннис 21.09.2014\","
                    + "\"folder\":\"Настольный теннис 21.09.2014\""));
            Assert.assertEquals(1, cluster.callbacks().accessCount(callback1));
            Assert.assertEquals(1, cluster.callbacks().accessCount(callback2));
        }
    }

    @Test
    public void testTikaiteFailure() throws Exception {
        try (KaliCluster cluster = new KaliCluster(this)) {
            String id =
                "3272440bcee235826ee00e6aee245d00d"
                + "126859efca46328c7d8bc9c9f360917";
            String stid =
                "320.yadisk:28148644.E200163:73498011068821467828280455014";
            final long uid = 203889312L;
            cluster.djfs().add(
                RESOURCE + uid + "&resourceId=203889312:" + id,
                new File(
                    getClass().getResource("zlib.djfs.json").toURI()));
            cluster.lenulca().add(
                GET + stid + RAW,
                HttpStatus.SC_SERVICE_UNAVAILABLE);
            cluster.start();

            HttpGet get = new HttpGet(
                cluster.kali().host() + ID + id + PREFIX + uid
                + MKFILE + DISK_QUEUE
                + "&version=1524580049581668&resource_id=203889312:" + id);
            get.addHeader(
                YandexHeaders.ZOO_SHARD_ID,
                Long.toString(uid % SHARDS));
            get.addHeader(YandexHeaders.ZOO_QUEUE, DISK_QUEUE);
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, get);
            cluster.lucene().checkSearch(
                SEARCH + uid
                + "&text=mediatype:6+AND+tikaite_error:1"
                + "&get=mimetype,fotki_tags,folder",
                TestSearchBackend.prepareResult(
                    ID_FIELD + id
                    + "\",\"mimetype\":\"application/pdf\""
                    + ",\"fotki_tags\":\""
                    + KEY_FIELD + "/disk/Всё подряд/zlib.3.pdf\","
                    + "\"folder\":\"Всё подряд\""));
        }
    }

    @Test
    public void testMalformedDjfsJson() throws Exception {
        try (KaliCluster cluster = new KaliCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            String id =
                "3272440bcee235826ee00e6aee245d00"
                + "d126859efca46328c7d8bc9c9f360917";
            String stid =
                "320.yadisk:28148644.E200163:73498011068821467828280455015";
            final long uid = 203889313L;
            final long version = 1524743150151937L;
            File djfsJson = new File(
                getClass().getResource("malformed.djfs.json").toURI());
            cluster.djfs().add(
                RESOURCE + uid + RESOURCE_ID + uid + ':' + id,
                djfsJson);
            cluster.lenulca().add(
                GET + stid + RAW,
                HttpStatus.SC_SERVICE_UNAVAILABLE);
            cluster.start();

            String resourceId = "&resource_id=203889313:";
            HttpGet get = new HttpGet(
                cluster.kali().host() + ID + id + PREFIX + uid
                + MKFILE + DISK_QUEUE + VERSION + version
                + resourceId + id);
            get.addHeader(
                YandexHeaders.ZOO_SHARD_ID,
                Long.toString(uid % SHARDS));
            get.addHeader(YandexHeaders.ZOO_QUEUE, DISK_QUEUE);
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, get);
            String stats = HttpAssert.stats(client, cluster.kali().port());
            HttpAssert.assertStat(
                "document-serialization-failures_ammm",
                "2",
                // missing "key" field is not mandatory now
                // +  malformed "visible" field
                stats);
            cluster.lucene().checkSearch(
                SEARCH + uid
                + "&text=mediatype:6&get=visible",
                TestSearchBackend.prepareResult(
                    ID_FIELD + id
                    + "\",\"key\":null,\"visible\":null"));

            cluster.djfs().add(
                RESOURCE + uid + RESOURCE_ID + uid + ':' + id,
                "{}");
            get = new HttpGet(
                cluster.kali().host() + ID + id + PREFIX + uid
                + MKFILE + DISK_QUEUE + VERSION + (version + 1)
                + resourceId + id);
            get.addHeader(
                YandexHeaders.ZOO_SHARD_ID,
                Long.toString(uid % SHARDS));
            get.addHeader(YandexHeaders.ZOO_QUEUE, DISK_QUEUE);
            HttpAssert.assertStatusCode(
                HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE,
                client,
                get);
        }
    }

    @Test
    public void testMimetypeHint() throws Exception {
        try (KaliCluster cluster = new KaliCluster(this)) {
            String id =
                "3d86a04352cbe5e56c3bcc92a72fda89a"
                + "426f7f86558ab182445f5586f9d6425";
            String stid =
                "4221.yadisk:14594669.2754523535209715925457579134902";
            final long uid = 14594669L;
            cluster.djfs().add(
                RESOURCE + uid + "&resourceId=14594669:" + id,
                new File(
                    getClass()
                        .getResource("x-canon-cr2.djfs.json").toURI()));
            cluster.lenulca().add(
                GET + stid + RAW,
                new File(
                    Paths.getSandboxResourcesRoot() + "/x-canon-cr2.cr2"));
            String key =
                "/disk/BackUp/Photos/Aperture Library - iMac.aplibrary/Masters"
                + "/2012/08/17/20120817-164608/_MG_0215.CR2";
            cluster.start();

            // Add document
            HttpGet get = new HttpGet(
                cluster.kali().host() + ID + id + PREFIX + uid
                + MKFILE + PHOTOSLICE
                + "&version=1375708450794231&resource_id=14594669:" + id);
            get.addHeader(
                YandexHeaders.ZOO_SHARD_ID,
                Long.toString(uid % SHARDS));
            get.addHeader(YandexHeaders.ZOO_QUEUE, PHOTOSLICE);
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, get);
            cluster.lucene().checkSearch(
                SEARCH + uid + TEXT_ID + id + MIMETYPES,
                TestSearchBackend.prepareResult(
                    ID_FIELD + id + KEY_FIELD + key
                    + "\",\"mimetype\":\"image/x-canon-cr2\""
                    + ",\"tikaite_mimetype\":\"image/x-canon-cr2\""));
        }
    }

    @Test
    public void testWrongDjfsMimetype() throws Exception {
        try (KaliCluster cluster = new KaliCluster(this)) {
            String id =
                "6d86a04352cbe5e56c3bcc92a72fda89a"
                + "426f7f86558ab182445f5586f9d6426";
            String stid =
                "4221.yadisk:14594670.2754523535209715925457579134906";
            final long uid = 14594670L;
            cluster.djfs().add(
                RESOURCE + uid + "&resourceId=14594670:" + id,
                new File(getClass().getResource("tux.djfs.json").toURI()));
            cluster.lenulca().add(
                GET + stid + RAW,
                new File(
                    getClass().getResource("tux.gif").toURI()));
            String key = "/disk/gallery/tux.jpg";
            cluster.start();

            // Add document
            HttpGet get = new HttpGet(
                cluster.kali().host() + ID + id + PREFIX + uid
                + MKFILE + PHOTOSLICE
                + "&version=1375708450794236&resource_id=14594670:" + id);
            get.addHeader(
                YandexHeaders.ZOO_SHARD_ID,
                Long.toString(uid % SHARDS));
            get.addHeader(YandexHeaders.ZOO_QUEUE, PHOTOSLICE);
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, get);
            cluster.lucene().checkSearch(
                SEARCH + uid + TEXT_ID + id + MIMETYPES,
                TestSearchBackend.prepareResult(
                    ID_FIELD + id + KEY_FIELD + key
                    + "\",\"mimetype\":\"image/jpeg\""
                    + ",\"tikaite_mimetype\":\"image/gif\""));
        }
    }

    @Test
    public void testKeepOcrAndCv() throws Exception {
        String common =
            "\"aux_folder\": \"disk\",\n"
                + "\"ext\": \"JPG\",\n"
                + "\"id\":\"a51c65feac7f8fe0a114b0d9a8d10a0a7a"
                + "0ce0d630db5736d7619e2a0357705c\",\n"
                + "\"folder\": \"Всё подряд\",\n"
                + "\"height\": \"3264\",\n"
                + "\"latitude\": \"55.66482\",\n"
                + "\"altitude\": \"193.0\",\n"
                + "\"longitude\": \"37.48987\",\n"
                + "\"manufacturer\": \"Apple\",\n"
                + "\"md5\": \"a8179224fc6be0ba661c239f91c057fe\",\n"
                + "\"mediatype\": \"9\","
                + "\"key\": \"/disk/Всё подряд/lepestrichestvo.JPG\","
                + "\"mimetype\": \"image/jpeg\",\n"
                + "\"model\": \"iPhone 6 Plus\",\n"
                + "\"name\": \"lepestrichestvo.JPG\",\n"
                + "\"preview_stid\": "
                + "\"ava:disk:203889311:2a0000016784029097a0d58265a839c80626\","
                + "\"ratio\": \"4:3\",\n"
                + "\"resource_id\":\"203889311:a51c65feac7f8fe0a114b0d9a8d10"
                + "a0a7a0ce0d630db5736d7619e2a0357705c\",\n"
                + "\"size\": \"2545015\",\n"
                + "\"stid\": \"320.yadisk:5598601.E1329136:"
                + "142985201065327062225295744981\",\n"
                + "\"tikaite_mimetype\": \"image/jpeg\",\n"
                + "\"type\": \"file\",\n"
                + "\"version\": \"1524653686127963\",\n"
                + "\"visible\": \"1\",\n"
                + "\"orientation\": \"portrait\",\n"
                + "\"owner\": \"203889311\",\n"
                + "\"parent_fid\": \"275f1b5cab867099a882c7781f0df56b\",\n"
                + "\"parsed\": \"true\",\n"
                + "\"albums_exclusions\": \"beautiful\nawful\",\n"
                + "\"width\": \"2448\"";

        String expected1 = common + ",\"beautiful\": \"0.25\",\n"
            + "\"faces\": \"[{\\\"x\\\":\\\"0.5\\\","
            + "\\\"y\\\":\\\"0.5\\\",\\\"width\\\":\\\"0.5\\\","
            + "\\\"height\\\":\\\"0.5\\\"},{\\\"x\\\":\\\"0.25\\\","
            + "\\\"y\\\":\\\"0.25\\\",\\\"width\\\":\\\"0.75\\\","
            + "\\\"height\\\":\\\"0.125\\\"}]\","
            + "\"faces_heights\": \"0.5\\n0.125\\n\","
            + "\"faces_widths\": \"0.5\\n0.75\\n\","
            + "\"i2t_keyword\": [-70,-70],"
            + "\"ocr_text\": \"Куча осмысленного "
            + "текста\\nразделённого переводом строк\",\n"
            + "\"wallpaper\": \"0.5\"\n";
        String expected2 = common + ",\"beautiful\": \"0.55\",\n"
            + "\"faces\": \"[{\\\"x\\\":\\\"0.5\\\","
            + "\\\"y\\\":\\\"0.5\\\",\\\"width\\\":\\\"0.5\\\","
            + "\\\"height\\\":\\\"0.5\\\"},{\\\"x\\\":\\\"0.25\\\","
            + "\\\"y\\\":\\\"0.25\\\",\\\"width\\\":\\\"0.75\\\","
            + "\\\"height\\\":\\\"0.125\\\"}]\","
            + "\"faces_heights\": \"0.5\\n0.125\\n\","
            + "\"faces_widths\": \"0.5\\n0.75\\n\","
            + "\"i2t_keyword\": [-70,-70],"
            + "\"bad_quality\":\"0.0457669\","
            + "\"ocr_text\": \"Куча осмысленного "
            + "текста\\nразделённого переводом строк\"";
        try (KaliCluster cluster = new KaliCluster(this)) {
            String id =
                "a51c65feac7f8fe0a114b0d9a8d10a"
                    + "0a7a0ce0d630db5736d7619e2a0357705c";
            String stid = "320.yadisk:5598601.E1329136:"
                + "142985201065327062225295744981";
            final long uid = 203889311;

            cluster.djfs().add(
                RESOURCE + uid + RESOURCE_ID + uid + ':' + id,
                new StaticHttpResource(
                    HttpStatus.SC_OK,
                    new FileEntity(
                        new File(
                            getClass().getResource(
                                "lepestrichestvo.djfs.json")
                                .toURI()),
                        ContentType.APPLICATION_JSON)));
            //cluster.
            // Add document
            cluster.lenulca().add(
                GET + stid + RAW,
                new File(
                    Paths.getSandboxResourcesRoot() + "/lepestrichestvo.JPG"));

            HttpGet get = new HttpGet(
                cluster.kali().host() + ID + id + PREFIX + uid
                    + MKFILE + PHOTOSLICE
                    + "&version=1524653686127963&resource_id="
                    + uid + ':' + id);
            get.addHeader(
                YandexHeaders.ZOO_SHARD_ID,
                Long.toString(uid % SHARDS));
            get.addHeader(YandexHeaders.ZOO_QUEUE, PHOTOSLICE);
            cluster.start();
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, get);

            String checkRequest = SEARCH + uid + TEXT_ID + id
                + "&get=*,-built_date,-modified,-photoslice_time,"
                + "-mtime,-created,-etime,-ctime";

            cluster.lucene().checkSearch(
                checkRequest,
                TestSearchBackend.prepareResult(expected1));

            cluster.imageparser().register(
                new Pattern<>("/process/handler", false),
                new StaticHttpItem(
                    "{\"i2t_hex\":\"BABA\",\"classes\":{"
                        + "\"beautiful\":0.55, \"bad_quality\":\"0.0457669\"},"
                        + "\"faces\":["
                        + "{\"x\":0.5,\"y\":0.5,\"width\":0.5,\"height\":0.5},"
                        + "{\"x\":0.25,\"y\":0.25,\"width\":0.75,"
                        + "\"height\":0.125}]}"));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, get);
            cluster.lucene().checkSearch(
                checkRequest,
                TestSearchBackend.prepareResult(expected2));
        }
    }

    // CSOFF: MethodLength
    @Test
    public void testParallelRequestsQueueing() throws Exception {
        try (SharedConnectingIOReactor reactor = new SharedConnectingIOReactor(
                Configs.baseConfig(),
                Configs.dnsConfig());
            AsyncClient asyncClient =
                new AsyncClient(reactor, Configs.targetConfig());
            KaliCluster cluster = new KaliCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            String id =
                "41bb4e971bac3f48d8c474322228423ae"
                + "da843c554429df83075ed38ba30bd68";
            String stid1 =
                "320.yadisk:478914153.E1372940:207504547820116390374698914424";
            String stid2 =
                "320.yadisk:478914153.E1372940:207504547820116390374698914425";
            String stid3 =
                "320.yadisk:478914153.E1372940:207504547820116390374698914426";
            final long uid = 478914153L;
            String queue2 = "queue-2.djfs.json";
            User user = new User(PHOTOSLICE, new LongPrefix(uid));
            cluster.djfs().add(
                RESOURCE + uid + RESOURCE_ID + uid + ':' + id,
                new StaticHttpItem(
                    HttpStatus.SC_OK,
                    new FileEntity(
                        new File(
                            getClass().getResource("queue-1.djfs.json")
                                .toURI()),
                        ContentType.APPLICATION_JSON)),
                new StaticHttpItem(
                    HttpStatus.SC_OK,
                    new FileEntity(
                        new File(getClass().getResource(queue2).toURI()),
                        ContentType.APPLICATION_JSON)),
                new StaticHttpItem(
                    HttpStatus.SC_OK,
                    new FileEntity(
                        new File(
                            getClass().getResource("queue-3.djfs.json")
                                .toURI()),
                        ContentType.APPLICATION_JSON)),
                new StaticHttpItem(
                    HttpStatus.SC_OK,
                    new FileEntity(
                        new File(getClass().getResource(queue2).toURI()),
                        ContentType.APPLICATION_JSON)),
                NotImplementedHttpItem.INSTANCE);
            cluster.lenulca().add(
                GET + stid1 + RAW,
                new SlowpokeHttpItem(new StaticHttpItem("hello"), TIMEOUT));
            cluster.lenulca().add(
                GET + stid2 + RAW,
                new StaticHttpItem("world"));
            cluster.lenulca().add(
                GET + stid3 + RAW,
                new StaticHttpItem("again"));
            String key = "/disk/data/statistics/8_06_08_2018_0-00-04_inf";
            reactor.start();
            asyncClient.start();
            cluster.start();

            // First request
            // Should take long time, then ovewritten by second request
            BasicAsyncRequestProducerGenerator generator =
                new BasicAsyncRequestProducerGenerator(
                    ID + id + PREFIX + uid + MKFILE + DISK_QUEUE
                    + "&version=1533567936913995&resource_id=478914153:" + id);
            generator.addHeader(
                YandexHeaders.ZOO_SHARD_ID,
                Long.toString(uid % SHARDS));
            generator.addHeader(YandexHeaders.ZOO_QUEUE, DISK_QUEUE);

            Future<HttpResponse> slowRequest = asyncClient.execute(
                cluster.kali().host(),
                generator,
                EmptyFutureCallback.INSTANCE);
            Thread.sleep(TIMEOUT >> 2);
            String lockStorageSize = "lock-storage-size_axxx";
            String lockQueueSize = "lock-queue-size_axxx";
            String stats = HttpAssert.stats(client, cluster.kali().port());
            // Request is in process, should create entry in lock storage
            HttpAssert.assertStat(
                lockStorageSize,
                Integer.toString(1),
                stats);
            // Since this is the only request processing, there is not other
            // tasks in async lock queue
            HttpAssert.assertStat(
                lockQueueSize,
                Integer.toString(0),
                stats);
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(cluster.kali().host() + "/locks-status")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "[{\"key\":\"" + uid + '#' + uid + ':' + id
                        + "\",\"refs\":1,\"locks\":{\"active-task\":"
                        + "\"<any value>\",\"queued-tasks-count\":0,"
                        + "\"queued-tasks\":[]}}]"),
                    CharsetUtils.toString(response.getEntity()));
            }

            // Second request, ovewrites first one
            // Should wait until first request completion
            String version2 =
                "&version=1533567936913996&resource_id=478914153:";
            HttpGet get = new HttpGet(
                cluster.kali().host() + ID + id + PREFIX + uid
                + MKFILE + PHOTOSLICE + version2 + id);
            get.addHeader(
                YandexHeaders.ZOO_SHARD_ID,
                Long.toString(uid % SHARDS));
            get.addHeader(YandexHeaders.ZOO_QUEUE, PHOTOSLICE);
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, get);
            cluster.lucene().checkSearch(
                SEARCH + uid + TEXT_ID + id + GET_STID,
                TestSearchBackend.prepareResult(
                    ID_FIELD + id + KEY_FIELD + key + 1
                    + STID + stid2 + '"'));
            // First request should be already completed, because second
            // request will be locked by it
            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                slowRequest.get(0, TimeUnit.MILLISECONDS));
            stats = HttpAssert.stats(client, cluster.kali().port());
            // All requests completed, lock storage should be emptied by now
            HttpAssert.assertStat(
                lockStorageSize,
                Integer.toString(0),
                stats);
            // Second request was blocked, so queue size was 1
            HttpAssert.assertStat(
                lockQueueSize,
                Integer.toString(1),
                stats);

            // Third request
            final long version3 = 1533567936913997L;
            final long position = 42;
            get = new HttpGet(
                cluster.kali().host() + ID + id + PREFIX + uid
                + MKFILE + PHOTOSLICE
                + VERSION + version3 + "&resource_id=478914153:" + id);
            get.addHeader(
                YandexHeaders.ZOO_SHARD_ID,
                Long.toString(uid % SHARDS));
            get.addHeader(YandexHeaders.ZOO_QUEUE, PHOTOSLICE);
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, get);
            cluster.lucene().checkSearch(
                SEARCH + uid + TEXT_ID + id + GET_STID,
                TestSearchBackend.prepareResult(
                    ID_FIELD + id + KEY_FIELD + key + 2
                    + STID + stid3 + '"'));

            // Second request once again, should be skipped, but callback will
            // be executed
            String callbackUri = "/queued-callback";
            get = new HttpGet(
                cluster.kali().host() + ID + id + PREFIX + uid
                + MKFILE + PHOTOSLICE + version2 + id
                + CALLBACK + cluster.callbacks().host() + callbackUri);
            cluster.callbacks().add(callbackUri, HttpStatus.SC_OK);
            get.addHeader(
                YandexHeaders.ZOO_SHARD_ID,
                Long.toString(uid % SHARDS));
            get.addHeader(YandexHeaders.ZOO_QUEUE, PHOTOSLICE);
            get.addHeader(YandexHeaders.ZOO_QUEUE_ID, Long.toString(position));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, get);
            // Nothing changed
            cluster.lucene().checkSearch(
                SEARCH + uid + TEXT_ID + id + GET_STID,
                TestSearchBackend.prepareResult(
                    ID_FIELD + id + KEY_FIELD + key + 2
                    + STID + stid3 + '"'));
            Assert.assertEquals(
                1,
                cluster.callbacks().accessCount(callbackUri));
            cluster.lucene().checkSearch(
                SEARCH + uid + TEXT_ID + INDEXED_PREFIX + id
                + GET_INDEXED_VERSION,
                TestSearchBackend.prepareResult(
                    ID_FIELD + INDEXED_PREFIX + id
                    + INDEXED_VERSION_FIELD + version3 + '"'));
            Assert.assertEquals(position, cluster.lucene().getQueueId(user));

            // Repeat callback, but do not pass ZooQueue, so
            // BadRequestException will be thrown
            // Check that there is no IllegalStateException in response
            get = new HttpGet(
                cluster.kali().host() + ID + id + PREFIX + uid
                + MKFILE + PHOTOSLICE + version2 + id
                + CALLBACK + cluster.callbacks().host() + callbackUri);
            cluster.callbacks().add(callbackUri, HttpStatus.SC_OK);
            get.addHeader(
                YandexHeaders.ZOO_SHARD_ID,
                Long.toString(uid % SHARDS));
            get.addHeader(YandexHeaders.ZOO_QUEUE_ID, Long.toString(position));
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_BAD_REQUEST,
                    response);
                YandexAssert.assertNotContains(
                    "IllegalStateException",
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }
    // CSON: MultipleStringLiterals
    // CSON: MethodLength

    private static String doc(
        final String id,
        final long version,
        final boolean hasResourceId)
    {
        StringBuilder sb = new StringBuilder(ID_FIELD);
        sb.append(id);
        if (hasResourceId) {
            sb.append("\",\"resource_id\":\"0:");
            sb.append(id);
        }
        sb.append("\",\"type\":\"dir\",\"name\":\"");
        sb.append(id);
        sb.append("\",\"key\":\"/disk/");
        sb.append(id);
        sb.append("\",\"version\":\"");
        sb.append(version);
        sb.append('"');
        return new String(sb);
    }

    private static String indexedVersion(
        final String id,
        final long version,
        final KaliActionType actionType)
    {
        StringBuilder sb = new StringBuilder("\"id\":\"indexed_");
        sb.append(id);
        sb.append("\",\"resource_id\":\"indexed_0:");
        sb.append(id);
        sb.append("\",\"indexed_action_type\":\"");
        sb.append(actionType);
        sb.append(INDEXED_VERSION_FIELD);
        sb.append(version);
        sb.append('"');
        return new String(sb);
    }

    // CSOFF: MethodLength
    @Test
    public void testCleanup() throws Exception {
        try (KaliCluster cluster = new KaliCluster(this);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            long version = 2L;
            long oldVersion = version - 1L;
            long newVersion = version + 1L;
            List<String> in = new ArrayList<>();
            List<String> out = new ArrayList<>();
            List<String> updateDocs = new ArrayList<>();
            String doc; // temporary doc

            // Docs below minimal doc
            // ----------------------

            // Old doc without resource_id or indexed version
            String lowOld0 = "01a0";
            in.add(doc(lowOld0, oldVersion, false));

            // Old doc with resource_id, but without indexed version
            // Probably from transition period
            String lowOld1 = "01a1";
            in.add(doc(lowOld1, oldVersion, true));
            out.add(
                indexedVersion(lowOld1, oldVersion, KaliActionType.REMOVE));

            // Old doc with resource_id and indexed version
            String lowOld2 = "01a2";
            in.add(doc(lowOld2, oldVersion, true));
            in.add(indexedVersion(lowOld2, oldVersion, KaliActionType.UPDATE));
            out.add(
                indexedVersion(lowOld2, oldVersion, KaliActionType.REMOVE));

            // Deleted old doc
            String lowOldDel = "01a3";
            doc = indexedVersion(lowOldDel, oldVersion, KaliActionType.REMOVE);
            in.add(doc);
            out.add(doc);

            // Doc which is newer than indexation request
            String lowNew = "01b0";
            doc = doc(lowNew, newVersion, true);
            in.add(doc);
            out.add(doc);
            doc = indexedVersion(lowNew, newVersion, KaliActionType.UPDATE);
            in.add(doc);
            out.add(doc);

            // Deleted newer doc
            String lowNewDel = "01b1";
            doc = indexedVersion(lowNewDel, newVersion, KaliActionType.REMOVE);
            in.add(doc);
            out.add(doc);

            // Some trash that shouldn't be there
            String lowTrash = "01c0";
            in.add(doc(lowTrash, newVersion, false));

            // Docs above max doc
            // ------------------

            // Old doc without resource_id or indexed version
            String upOld0 = "0fa0";
            in.add(doc(upOld0, oldVersion, false));

            // Old doc with resource_id, but without indexed version
            // Probably from transition period
            String upOld1 = "0fa1";
            in.add(doc(upOld1, oldVersion, true));
            out.add(indexedVersion(upOld1, oldVersion, KaliActionType.REMOVE));

            // Old doc with resource_id and indexed version
            String upOld2 = "0fa2";
            in.add(doc(upOld2, oldVersion, true));
            in.add(indexedVersion(upOld2, oldVersion, KaliActionType.UPDATE));
            out.add(indexedVersion(upOld2, oldVersion, KaliActionType.REMOVE));

            // Doc which is newer than indexation request
            String upNew = "0fb0";
            doc = doc(upNew, newVersion, true);
            in.add(doc);
            out.add(doc);
            doc = indexedVersion(upNew, newVersion, KaliActionType.REINDEX);
            in.add(doc);
            out.add(doc);

            // Deleted newer doc
            String upNewDel = "0fb1";
            doc = indexedVersion(upNewDel, newVersion, KaliActionType.REMOVE);
            in.add(doc);
            out.add(doc);

            // Some thash that shouldn't be there
            String upTrash = "0fc0";
            in.add(doc(upTrash, newVersion, false));

            // Inner docs
            // ----------
            // Updated docs are:
            // * 08a0 (very old)
            // * 08b2 (just old)
            // * 08c4 (old)
            // * 08d6 (newer)
            // * 08e8 (removed newer)
            // * 08ea (older, gone in DJFS)
            // * 08ec (missing)
            // * 08ee (missing)

            // Old doc without resource_id
            String inDoc0 = "08a4";
            in.add(doc(inDoc0, oldVersion, false));

            // Old doc with resource_id, but without indexed version
            // Probably from transition period
            String inDoc1 = "08a6";
            in.add(doc(inDoc1, oldVersion, true));
            out.add(indexedVersion(inDoc1, oldVersion, KaliActionType.REMOVE));

            // Old doc with resource_id and indexed version
            String inDoc2 = "08a8";
            in.add(doc(inDoc2, oldVersion, true));
            in.add(indexedVersion(inDoc2, oldVersion, KaliActionType.REINDEX));
            out.add(indexedVersion(inDoc2, oldVersion, KaliActionType.REMOVE));

            // Deleted old doc
            String inOldDel = "08aa";
            doc = indexedVersion(inOldDel, oldVersion, KaliActionType.REMOVE);
            in.add(doc);
            out.add(doc);

            // Doc which is newer than indexation request
            String inNew = "08b4";
            doc = doc(inNew, newVersion, true);
            in.add(doc);
            out.add(doc);
            doc = indexedVersion(inNew, newVersion, KaliActionType.UPDATE);
            in.add(doc);
            out.add(doc);

            // Deleted newer doc
            String inNewDel = "08b6";
            doc = indexedVersion(inNewDel, newVersion, KaliActionType.REMOVE);
            in.add(doc);
            out.add(doc);

            // Some trash that shouldn't be there
            String inTrash = "08c2";
            in.add(doc(inTrash, newVersion, false));

            // Updated docs
            // -----------------------

            // Old doc without resource_id
            String upd0 = "08a0";
            in.add(doc(upd0, oldVersion, false));
            out.add(doc(upd0, version, true));
            out.add(indexedVersion(upd0, version, KaliActionType.REINDEX));
            updateDocs.add(upd0);

            // Old doc with resource_id, but without indexed version
            // Probably from transition period
            String upd1 = "08b2";
            in.add(doc(upd1, oldVersion, true));
            out.add(doc(upd1, version, true));
            out.add(indexedVersion(upd1, version, KaliActionType.REINDEX));
            updateDocs.add(upd1);

            // Old doc with resource_id and indexed version
            String upd2 = "08c4";
            in.add(doc(upd2, oldVersion, true));
            in.add(indexedVersion(upd2, oldVersion, KaliActionType.UPDATE));
            out.add(doc(upd2, version, true));
            out.add(indexedVersion(upd2, version, KaliActionType.REINDEX));
            updateDocs.add(upd2);

            // Doc which is newer than indexation request
            String updNew = "08d6";
            doc = doc(updNew, newVersion, true);
            in.add(doc);
            out.add(doc);
            doc = indexedVersion(updNew, newVersion, KaliActionType.UPDATE);
            in.add(doc);
            out.add(doc);
            updateDocs.add(updNew);

            // Deleted newer doc
            String updNewDel = "08e8";
            doc = indexedVersion(updNewDel, newVersion, KaliActionType.REMOVE);
            in.add(doc);
            out.add(doc);
            updateDocs.add(updNewDel);

            // Old doc, which is missing in DJFS
            // We won't touch it, leaving it removal to appropriate request
            String updOldGone = "08ea";
            doc = doc(updOldGone, oldVersion, true);
            in.add(doc);
            out.add(doc);
            doc =
                indexedVersion(updOldGone, oldVersion, KaliActionType.UPDATE);
            in.add(doc);
            out.add(doc);
            updateDocs.add(updOldGone);

            // Missing docs, will add them
            String updMiss0 = "08ec";
            out.add(doc(updMiss0, version, true));
            out.add(indexedVersion(updMiss0, version, KaliActionType.REINDEX));
            updateDocs.add(updMiss0);

            String updMiss1 = "08ee";
            out.add(doc(updMiss1, version, true));
            out.add(indexedVersion(updMiss1, version, KaliActionType.REINDEX));
            updateDocs.add(updMiss1);

            StringBuilder djfsUri = new StringBuilder(RESOURCE);
            djfsUri.append(0L);
            StringBuilder kaliRequest = new StringBuilder("{\"docs\":[");
            StringBuilder djfsResponse = new StringBuilder("{\"items\":[");
            for (String updateDoc: updateDocs) {
                djfsUri.append(RESOURCE_ID);
                djfsUri.append(0L);
                djfsUri.append(':');
                djfsUri.append(updateDoc);
                doc = doc(updateDoc, version, true);
                kaliRequest.append('{');
                kaliRequest.append(doc);
                kaliRequest.append('}');
                kaliRequest.append(',');
                if (!updateDoc.equals(updOldGone)) {
                    djfsResponse.append('{');
                    djfsResponse.append(doc);
                    djfsResponse.append(
                        ",\"uid\":0,\"visible\":\"true\",\"mtime\":");
                    djfsResponse.append(version);
                    djfsResponse.append(",\"ctime\":");
                    djfsResponse.append(version);
                    djfsResponse.append('}');
                    djfsResponse.append(',');
                }
            }
            kaliRequest.setCharAt(kaliRequest.length() - 1, ']');
            kaliRequest.append('}');
            djfsResponse.setCharAt(djfsResponse.length() - 1, ']');
            djfsResponse.append('}');
            cluster.djfs().add(
                new String(djfsUri),
                new StaticHttpItem(new String(djfsResponse)));
            cluster.start();
            cluster.lucene().add(in.toArray(new String[in.size()]));
            out.sort(Comparator.<String>naturalOrder().reversed());

            HttpPost post = new HttpPost(
                cluster.kali().host()
                + "/?zoo-queue-id=100500&prefix=0&action=reindex"
                + "&cleanup-type=inner&version=" + version);
            post.setEntity(
                new StringEntity(
                    new String(kaliRequest),
                    ContentType.APPLICATION_JSON));
            post.addHeader(
                YandexHeaders.ZOO_SHARD_ID,
                Long.toString(0L));
            post.addHeader(YandexHeaders.ZOO_QUEUE, DISK_QUEUE);
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);

            post = new HttpPost(
                cluster.kali().host()
                + "/?zoo-queue-id=100501&prefix=0&action=reindex"
                + "&cleanup-type=outer&version=" + version);
            post.setEntity(
                new StringEntity(
                    "{\"docs\":[{" + doc(upd0, version, true)
                    + "},{" + doc(updMiss1, version, true) + '}' + ']' + '}',
                    ContentType.APPLICATION_JSON));
            post.addHeader(
                YandexHeaders.ZOO_SHARD_ID,
                Long.toString(0L));
            post.addHeader(YandexHeaders.ZOO_QUEUE, DISK_QUEUE);
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);

            String[] outDocs = new String[out.size()];
            for (int i = 0; i < outDocs.length; ++i) {
                doc = out.get(i);
                if (doc.indexOf(INDEXED_PREFIX) == -1
                    && doc.indexOf("\"version\":\"3\"") == -1
                    && !doc.contains('"' + updOldGone))
                {
                    outDocs[i] =
                        doc + ",\"aux_folder\":\"disk\",\"ctime\":\"2\""
                        + ",\"mtime\":\"2\",\"version\":\"2\",\"owner\":\"0\""
                        + ",\"visible\":\"1\"";
                } else {
                    outDocs[i] = doc;
                }
            }
            cluster.lucene().checkSearch(
                SEARCH + "0&text=__prefix:0&get=*,-folder&skip-nulls&sort=id",
                TestSearchBackend.prepareResult(outDocs));
        }
    }
    // CSON: MethodLength
}

