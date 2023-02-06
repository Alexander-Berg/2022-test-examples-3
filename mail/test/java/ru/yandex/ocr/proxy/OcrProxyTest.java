package ru.yandex.ocr.proxy;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HttpRequestHandler;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.disk.search.DiskParams;
import ru.yandex.http.test.ExpectingHeaderHttpItem;
import ru.yandex.http.test.ExpectingHttpItem;
import ru.yandex.http.test.HeaderValidator;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.test.StaticHttpItem;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.http.test.ValidatingHttpItem;
import ru.yandex.http.util.YandexHeaders;
import ru.yandex.http.util.YandexHttpStatus;
import ru.yandex.search.prefix.LongPrefix;
import ru.yandex.test.search.backend.TestSearchBackend;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.TestBase;

public class OcrProxyTest extends TestBase {
    private static final String HANDLE_URI_BASE = "/process/handler?stid=";
    private static final String CALLBACK = "&callback=";
    private static final String STAT = "/stat?hr";
    private static final LongPrefix PREFIX = new LongPrefix(9000);

    // CSOFF: MultipleStringLiterals
    @Test
    public void testOcr() throws Exception {
        try (OcrProxyCluster cluster = new OcrProxyCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            String ocrText = "txto recognized";
            String uri = "/process/handler?stid=1.2.8&passcache=1";
            cluster.ocraas().add(
                uri,
                new StaticHttpResource(
                    new ValidatingHttpItem(
                        new StaticHttpItem(ocrText),
                        new HeaderValidator(DiskParams.DISK_X_SRW_KEY_TYPE)
                            .andThen(
                                new HeaderValidator(
                                    DiskParams.DISK_X_SRW_NAMESPACE))
                            .andThen(
                                new HeaderValidator(
                                    OcrProxyCluster.UNISTORAGE_TVM2_HEADER))
                            .andThen(
                                new HeaderValidator(
                                    OcrProxyCluster.APE_TVM2_HEADER)))));

            ExpectingHttpItem item =
                new ExpectingHttpItem(
                    new JsonChecker(
                        "{\"prefix\":9000, \"query\":\"id:ocrtest "
                            + "AND stid:1.2.8\", \"docs\""
                            + ":[{\"ocr_text\":\"txto recognized\"}]}"));
            String callbackUri1 = "/ocr-first?timestamp=1234567891";
            cluster.ocrCallbacksBackend().add(
                callbackUri1,
                new StaticHttpResource(
                    new ExpectingHeaderHttpItem(
                        item,
                        YandexHeaders.SERVICE,
                        OcrProxyCluster.OCR_CALLBACKS_QUEUE)));
            String callbackUri2 = "/ocr-second?para&ms&timestamp=1234567891";
            cluster.ocrCallbacksBackend().add(
                callbackUri2,
                new StaticHttpResource(
                    new ExpectingHeaderHttpItem(
                        item,
                        YandexHeaders.SERVICE,
                        OcrProxyCluster.OCR_CALLBACKS_QUEUE)));
            cluster.start();
            String baseUri = "/ocr-only?id=ocrtest&stid=1.2.8&prefix=9000";
            String ocrProxyUri1 =
                baseUri + CALLBACK + cluster.ocrCallbacksBackend().host()
                    + "/ocr-first&callback="
                    + cluster.ocrCallbacksBackend().host()
                    + "/ocr-second?para%26ms&timestamp=1234567891";
            HttpAssert.assertStatusCode(
                YandexHttpStatus.SC_OK,
                cluster.proxy().port(),
                ocrProxyUri1);

            Assert.assertEquals(
                1,
                cluster.ocrCallbacksBackend().accessCount(callbackUri1));
            Assert.assertEquals(
                1,
                cluster.ocrCallbacksBackend().accessCount(callbackUri2));
            try (CloseableHttpResponse response = client.execute(
                new HttpGet(cluster.proxy().host() + STAT)))
            {
                HttpAssert.assertStatusCode(YandexHttpStatus.SC_OK, response);
                String body = HttpAssert.body(response);
                HttpAssert.assertStat("ocraas-non-empty_ammm", "" + 1, body);
            }

            String luceneSearch = "/search?prefix=9000&get=ocr_text&text=id:*";
            // we should not update anything, stid not in index
            cluster.lucene().checkSearch(
                luceneSearch,
                TestSearchBackend.prepareResult());

            cluster.lucene().add(
                PREFIX,
                "\"id\":\"ocrtest\", \"stid\": \"1.2.8\"");
            HttpAssert.assertStatusCode(
                YandexHttpStatus.SC_OK,
                cluster.proxy().port(),
                ocrProxyUri1);
            cluster.lucene().checkSearch(
                luceneSearch,
                TestSearchBackend.prepareResult(
                    "\"ocr_text\":\"txto recognized\""));

            //Assert.assertEquals(1, cluster.backend().accessCount(backendUri));
            Assert.assertEquals(
                2,
                cluster.ocrCallbacksBackend().accessCount(callbackUri1));
            Assert.assertEquals(
                2,
                cluster.ocrCallbacksBackend().accessCount(callbackUri2));

            HttpAssert.assertStatusCode(
                YandexHttpStatus.SC_OK,
                cluster.proxy().port(),
                baseUri);
            //Assert.assertEquals(2, cluster.backend().accessCount(backendUri));

            cluster.ocraas().add(uri, YandexHttpStatus.SC_BUSY);
            HttpAssert.assertStatusCode(
                YandexHttpStatus.SC_BUSY,
                client,
                new HttpGet(cluster.proxy().host() + baseUri));

            cluster.ocraas().add(uri, YandexHttpStatus.SC_TOO_MANY_REQUESTS);
            HttpAssert.assertStatusCode(
                YandexHttpStatus.SC_TOO_MANY_REQUESTS,
                client,
                new HttpGet(cluster.proxy().host() + baseUri));
        }
    }

    @Test
    public void testPreviewStid() throws Exception {
        try (OcrProxyCluster cluster = new OcrProxyCluster(this)) {
            cluster.start();
            String stidDsk = "320.yadisk:9000.E69093:6646224";
            String stidAva = "ava:disk:9000:2a0000016784029097";
            String avaNmspc = "avatars-disk";
            String avaKey = "9000/2a0000016784029097";
            String ocrAvaUri =
                HANDLE_URI_BASE + avaNmspc + '/' + avaKey
                    + '/' + OcrProxyContext.AVASTID_POSTFIX + "&passcache=1";
            String ocrDiskUri = HANDLE_URI_BASE + stidDsk + "&passcache=1";

            cluster.lucene().add(
                PREFIX,
                "\"id\":\"026638cf2745e\",\"stid\": \"" + stidDsk + '\"');

            String ocrText = "Россия родина слонов";
            cluster.ocraas().add(
                ocrAvaUri,
                new ExpectingHeaderHttpItem(
                    new StaticHttpItem(ocrText),
                    DiskParams.AVATAR_X_SRW_KEY_TYPE,
                    new BasicHeader(YandexHeaders.X_SRW_KEY, avaKey),
                    DiskParams.AVATAR_X_SRW_NAMESPACE));

            HttpAssert.assertStatusCode(
                YandexHttpStatus.SC_OK,
                cluster.proxy().port(),
                "/ocr-ava?id=026638cf2745e&prefix=9000&stid="
                    + stidDsk
                    + "&preview-stid=" + stidAva);
            Assert.assertEquals(
                1,
                cluster.ocraas().accessCount(ocrAvaUri));

            cluster.lucene().checkSearch(
                "/search?prefix=9000&text=id:*&get=ocr_text",
                TestSearchBackend.prepareResult(
                    1,
                    "\"ocr_text\":\"" + ocrText + '\"'));

            ocrText = "Или все таки мамонтов";
            cluster.ocraas().add(
                ocrDiskUri,
                new ExpectingHeaderHttpItem(
                    new StaticHttpItem(ocrText),
                    DiskParams.DISK_X_SRW_KEY_TYPE,
                    new BasicHeader(YandexHeaders.X_SRW_KEY, stidDsk),
                    DiskParams.DISK_X_SRW_NAMESPACE));

            HttpAssert.assertStatusCode(
                YandexHttpStatus.SC_OK,
                cluster.proxy().port(),
                "/ocr-disk?id=026638cf2745e&prefix=9000&stid="
                    + stidDsk
                    + "&preview-stid=" + stidDsk);

            Assert.assertEquals(
                1,
                cluster.ocraas().accessCount(ocrDiskUri));

            cluster.lucene().checkSearch(
                "/search?prefix=9000&text=id:*&get=ocr_text",
                TestSearchBackend.prepareResult(
                    1,
                    "\"ocr_text\": \"" + ocrText + '\"'));

            //cv
            stidDsk = "320.yadisk:9000.E69093:ffffffff";
            stidAva = "ava:disk:9000:ac57456";
            avaKey = "9000/ac57456";
            String cvAvaUri = HANDLE_URI_BASE + avaNmspc + '/' + avaKey
                + '/' + OcrProxyContext.AVASTID_POSTFIX + "&passcache=1";
            String cvDiskUri = HANDLE_URI_BASE + stidDsk + "&passcache=1";
            cluster.lucene().add(
                PREFIX,
                "\"id\":\"4d3782293\", \"mediatype\":9, \"preview_stid\":\""
                    + stidAva
                    + "\",\"stid\": \"" + stidDsk + '\"');
            HttpRequestHandler avaStidHandler =
                new ExpectingHeaderHttpItem(
                    new StaticHttpItem("{\"i2t_hex\":\"Летающие слоны\"}"),
                    DiskParams.AVATAR_X_SRW_KEY_TYPE,
                    new BasicHeader(YandexHeaders.X_SRW_KEY, avaKey),
                    DiskParams.AVATAR_X_SRW_NAMESPACE);
            cluster.imageparser().add(cvAvaUri, avaStidHandler);

            HttpAssert.assertStatusCode(
                YandexHttpStatus.SC_OK,
                cluster.proxy().port(),
                "/cv-ava?id=4d3782293&prefix=9000");
            Assert.assertEquals(
                1,
                cluster.imageparser().accessCount(cvAvaUri));

            cluster.lucene().checkSearch(
                "/search?prefix=9000&text=id:4d3782293&get=i2t_keyword",
                TestSearchBackend.prepareResult(
                    1,
                    "\"i2t_keyword\":\"Летающие слоны\""));

            // check vs preview stid in params
            cluster.lucene().add(
                PREFIX,
                "\"id\":\"ac2453\",\"stid\": \"" + stidDsk + '\"');
            cluster.imageparser().add(cvAvaUri, avaStidHandler);

            HttpAssert.assertStatusCode(
                YandexHttpStatus.SC_OK,
                cluster.proxy().port(),
                "/cv-ava-2?id=ac2453&prefix=9000&stid=" + stidDsk
                    + "&preview-stid=" + stidAva);
            Assert.assertEquals(
                1,
                cluster.imageparser().accessCount(cvAvaUri));

            cluster.lucene().checkSearch(
                "/search?prefix=9000&text=id:ac2453&get=i2t_keyword",
                TestSearchBackend.prepareResult(
                    1,
                    "\"i2t_keyword\":\"Летающие слоны\""));
            // only disk stid
            cluster.lucene().add(
                PREFIX,
                "\"id\":\"9a32d45\", \"mediatype\":9,"
                    + "\"stid\": \"" + stidDsk + '\"');

            cluster.imageparser().add(
                cvDiskUri,
                new ExpectingHeaderHttpItem(
                    new StaticHttpItem("{\"i2t_hex\":\"Родина\"}"),
                    DiskParams.DISK_X_SRW_KEY_TYPE,
                    new BasicHeader(YandexHeaders.X_SRW_KEY, stidDsk),
                    DiskParams.DISK_X_SRW_NAMESPACE));

            HttpAssert.assertStatusCode(
                YandexHttpStatus.SC_OK,
                cluster.proxy().port(),
                "/cv-disk?id=9a32d45&prefix=9000");
            Assert.assertEquals(
                1,
                cluster.imageparser().accessCount(cvDiskUri));

            cluster.lucene().checkSearch(
                "/search?prefix=9000&text=id:9a32d45&get=i2t_keyword",
                TestSearchBackend.prepareResult(
                    1,
                    "\"i2t_keyword\":\"Родина\""));
        }
    }

    @Test
    public void testCv() throws Exception {
        try (OcrProxyCluster cluster = new OcrProxyCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.lucene().add(
                PREFIX,
                "\"id\":\"cvtest\",\"stid\": \"1.2.9\"");

            String uri = "/process/handler?stid=1.2.9&passcache=1";
            String expected1 =
                "{\"uid\":9000,\"id\":\"cvtest\","
                    + "\"stid\": \"1.2.9\", \"height\": -1, \"width\": -1}";

            cluster.imageparser().add(
                uri,
                "{\"i2t_hex\":\"ABBA\"}");
            ExpectingHttpItem item =
                new ExpectingHttpItem(
                    new JsonChecker(expected1));

            // TODO Как то Проверять что приходим с нужными хидерами в люцену
//            String backendUri = "/update?cv&prefix=9000&id=cvtest";
//            cluster.backend().add(
//                backendUri,
//                new ExpectingHeaderHttpItem(
//                    item,
//                    YandexHeaders.ZOO_QUEUE,
//                    "imageparser"),
//                new ExpectingHeaderHttpItem(
//                    item,
//                    YandexHeaders.ZOO_QUEUE,
//                    null));
            String callbackUri1 = "/cv-first?timestamp=1234567892";
            cluster.ocrCallbacksBackend().add(
                callbackUri1,
                new StaticHttpResource(
                    new ExpectingHeaderHttpItem(
                        item,
                        YandexHeaders.SERVICE,
                        OcrProxyCluster.CV_CALLBACKS_QUEUE)));
            String callbackUri2 = "/cv-second?para&ms&timestamp=1234567892";
            cluster.ocrCallbacksBackend().add(
                callbackUri2,
                new StaticHttpResource(
                    new ExpectingHeaderHttpItem(
                        item,
                        YandexHeaders.SERVICE,
                        OcrProxyCluster.CV_CALLBACKS_QUEUE)));
            cluster.start();

            String baseUri = "/cv?id=cvtest&stid=1.2.9&prefix=9000";
            HttpAssert.assertStatusCode(
                YandexHttpStatus.SC_OK,
                cluster.proxy().port(),
                baseUri + CALLBACK + cluster.ocrCallbacksBackend().host()
                + "/cv-first&callback=" + cluster.ocrCallbacksBackend().host()
                + "/cv-second?para%26ms&timestamp=1234567892");
            //Assert.assertEquals(1, cluster.backend().accessCount(backendUri));
            Assert.assertEquals(
                1,
                cluster.ocrCallbacksBackend().accessCount(callbackUri1));
            Assert.assertEquals(
                1,
                cluster.ocrCallbacksBackend().accessCount(callbackUri2));
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(cluster.proxy().host() + STAT)))
            {
                HttpAssert.assertStatusCode(YandexHttpStatus.SC_OK, response);
                String body = HttpAssert.body(response);
                HttpAssert.assertStat("cv-i2t-non-empty_ammm", "" + 1, body);
                HttpAssert.assertStat("cv-faces-empty_ammm", "" + 1, body);
            }

            HttpAssert.assertStatusCode(
                YandexHttpStatus.SC_OK,
                cluster.proxy().port(),
                baseUri);
            //Assert.assertEquals(2, cluster.backend().accessCount(backendUri));
            Assert.assertEquals(
                1,
                cluster.ocrCallbacksBackend().accessCount(callbackUri1));
            Assert.assertEquals(
                1,
                cluster.ocrCallbacksBackend().accessCount(callbackUri2));

            cluster.imageparser().add(uri, YandexHttpStatus.SC_BUSY);
            HttpAssert.assertStatusCode(
                YandexHttpStatus.SC_BUSY,
                client,
                new HttpGet(cluster.proxy().host() + baseUri));

            cluster.imageparser().add(
                uri,
                YandexHttpStatus.SC_TOO_MANY_REQUESTS);
            HttpAssert.assertStatusCode(
                YandexHttpStatus.SC_TOO_MANY_REQUESTS,
                client,
                new HttpGet(cluster.proxy().host() + baseUri));

            String withSizes = "/cv?id=cvtest&stid=1.3.0&prefix=9000"
                + "&width=5&height=10";
            String expected2 =
                "{\"uid\":9000,\"id\":\"cvtest\",\"beautiful\":\"10.0\","
                    + "\"stid\": \"1.3.0\", \"height\": 10, \"width\": 5}";

            item = new ExpectingHttpItem(new JsonChecker(expected2));
            cluster.ocrCallbacksBackend().add(
                callbackUri1,
                new StaticHttpResource(
                    new ExpectingHeaderHttpItem(
                        item,
                        YandexHeaders.SERVICE,
                        OcrProxyCluster.CV_CALLBACKS_QUEUE)));
            cluster.ocrCallbacksBackend().add(
                callbackUri2,
                new StaticHttpResource(
                    new ExpectingHeaderHttpItem(
                        item,
                        YandexHeaders.SERVICE,
                        OcrProxyCluster.CV_CALLBACKS_QUEUE)));

            uri = "/process/handler?stid=1.3.0&passcache=1";
            cluster.imageparser().add(
                uri,
                "{\"classes\":{\"beautiful\":\"10.0\"}}");
            Assert.assertEquals(
                0,
                cluster.ocrCallbacksBackend().accessCount(callbackUri1));
            Assert.assertEquals(
                0,
                cluster.ocrCallbacksBackend().accessCount(callbackUri2));
            HttpAssert.assertStatusCode(
                YandexHttpStatus.SC_OK,
                cluster.proxy().port(),
                withSizes + CALLBACK + cluster.ocrCallbacksBackend().host()
                    + "/cv-first&callback="
                    + cluster.ocrCallbacksBackend().host()
                    + "/cv-second?para%26ms&timestamp=1234567892");

            Assert.assertEquals(
                1,
                cluster.ocrCallbacksBackend().accessCount(callbackUri1));
            Assert.assertEquals(
                1,
                cluster.ocrCallbacksBackend().accessCount(callbackUri2));
        }
    }
    // CSON: MultipleStringLiterals
}

