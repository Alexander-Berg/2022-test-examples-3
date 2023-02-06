package ru.yandex.tikaite.server;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentProducer;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.devtools.test.Paths;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.StaticServer;
import ru.yandex.test.util.TestBase;
import ru.yandex.tikaite.util.CommonFields;
import ru.yandex.tikaite.util.Json;

// CSOFF: MagicNumber
public class ImageTest extends TestBase {
    private static final String CONTENT_TYPE_META = "Content-Type:";
    private static final String IMAGE_JPEG = "image/jpeg";
    private static final String IMAGE_PNG = "image/png";
    private static final String IMAGE_RAW_NIKON = "image/x-raw-nikon";
    private static final String IMAGE_TIFF = "image/tiff";
    private static final String IMAGE_HEIF = "image/heif";
    private static final String LANDSCAPE = "landscape";
    private static final String PORTRAIT = "portrait";
    private static final String APPLE = "Apple";
    private static final String IPHONE_7_PLUS = "iPhone 7 Plus";
    private static final String NIKON = "NIKON CORPORATION";
    private static final String RATIO_4X3 = "4:3";
    private static final int TIMEOUT = 20000;

    public ImageTest() {
        super(false, 0L);
    }

    @Test
    public void testIphoneJpg() throws Exception {
        Json json = new Json(
            IMAGE_JPEG,
            "",
            true,
            null,
            new Json.Contains(CONTENT_TYPE_META + IMAGE_JPEG));
        Map<String, Object> root = json.root();
        root.put(CommonFields.CREATED, 1369301916L);
        root.put(CommonFields.MODIFIED, 1369301916L);
        root.put(CommonFields.HEIGHT, 2592L);
        root.put(CommonFields.WIDTH, 1936L);
        root.put(CommonFields.ORIENTATION, PORTRAIT);
        root.put(CommonFields.RATIO, "162:121");
        root.put(CommonFields.ALTITUDE, 196L);
        root.put(CommonFields.LATITUDE, 55.547333);
        root.put(CommonFields.LONGITUDE, 37.070667);
        root.put(CommonFields.MANUFACTURER, APPLE);
        root.put(CommonFields.MODEL, "iPhone 4");
        root.put(CommonFields.EXIF_ORIENTATION, 6L);
        DiskHandlerTest.testJson("iphone.jpg", json);
    }

    @Test
    public void testNikonJpg() throws Exception {
        Json json = new Json(
            IMAGE_JPEG,
            "",
            true,
            null,
            new Json.Contains(CONTENT_TYPE_META + IMAGE_JPEG));
        Map<String, Object> root = json.root();
        root.put(CommonFields.CREATED, 1371492313L);
        root.put(CommonFields.MODIFIED, 1371492313L);
        root.put(CommonFields.HEIGHT, 3264L);
        root.put(CommonFields.WIDTH, 4928L);
        root.put(CommonFields.ORIENTATION, LANDSCAPE);
        root.put(CommonFields.RATIO, "77:51");
        root.put(CommonFields.MANUFACTURER, NIKON);
        root.put(CommonFields.MODEL, "NIKON D5100");
        root.put(CommonFields.EXIF_ORIENTATION, 1L);
        DiskHandlerTest.testJson("nikon.jpg", json);
    }

    @Test
    public void testTverJpg() throws Exception {
        Json json = new Json(
            IMAGE_JPEG,
            "",
            true,
            null,
            new Json.Contains(CONTENT_TYPE_META + IMAGE_JPEG));
        Map<String, Object> root = json.root();
        String author = "тестовый автор";
        root.put(CommonFields.ARTIST, author);
        root.put(CommonFields.AUTHOR, author);
        root.put(
            CommonFields.DESCRIPTION,
            "тестовое " + "описание");
        String keywords = "тестовые ключевые слова";
        root.put(CommonFields.KEYWORDS, keywords);
        root.put(CommonFields.SUBJECT, keywords);
        root.put(
            CommonFields.TITLE,
            new Json.Headers(
                "тестовое название\nЕщё один тестовый заголовок"));
        root.put(CommonFields.CREATED, 1373284684L);
        root.put(CommonFields.MODIFIED, 1373284684L);
        root.put(CommonFields.HEIGHT, 549L);
        root.put(CommonFields.WIDTH, 1200L);
        root.put(CommonFields.ORIENTATION, LANDSCAPE);
        root.put(CommonFields.RATIO, "400:183");
        root.put(CommonFields.EXIF_ORIENTATION, 1L);
        DiskHandlerTest.testJson("tver.jpg", json);
    }

    @Test
    public void testHtcOneJpg() throws Exception {
        Json json = new Json(
            IMAGE_JPEG,
            "",
            true,
            null,
            new Json.Contains(CONTENT_TYPE_META + IMAGE_JPEG));
        Map<String, Object> root = json.root();
        root.put(CommonFields.CREATED, 1366743689L);
        root.put(CommonFields.MODIFIED, 1366743689L);
        root.put(CommonFields.HEIGHT, 1840L);
        root.put(CommonFields.WIDTH, 3264L);
        root.put(CommonFields.ORIENTATION, LANDSCAPE);
        root.put(CommonFields.RATIO, "204:115");
        root.put(CommonFields.MANUFACTURER, "HTC");
        root.put(CommonFields.MODEL, "HTC One X");
        root.put(CommonFields.EXIF_ORIENTATION, 0L);
        DiskHandlerTest.testJson("htc-one.jpg", json);
    }

    @Test
    public void testRoofJpg() throws Exception {
        Json json = new Json(
            IMAGE_JPEG,
            "",
            true,
            null,
            new Json.Contains(CONTENT_TYPE_META + IMAGE_JPEG));
        Map<String, Object> root = json.root();
        root.put(CommonFields.CREATED, 1342817233L);
        root.put(CommonFields.MODIFIED, 1342817233L);
        root.put(CommonFields.HEIGHT, 1944L);
        root.put(CommonFields.WIDTH, 2592L);
        root.put(CommonFields.ORIENTATION, LANDSCAPE);
        root.put(CommonFields.RATIO, RATIO_4X3);
        root.put(CommonFields.ALTITUDE, 160.9d);
        root.put(CommonFields.LATITUDE, 57.260003);
        root.put(CommonFields.LONGITUDE, 37.796349);
        root.put(CommonFields.MANUFACTURER, "SONY");
        root.put(CommonFields.MODEL, "DSC-HX9V");
        root.put(CommonFields.EXIF_ORIENTATION, 1L);
        DiskHandlerTest.testJson("roof.jpg", json);
    }

    @Test
    public void testJpgComment() throws Exception {
        Json json = new Json(
            IMAGE_JPEG,
            "",
            true,
            null,
            new Json.Contains(CONTENT_TYPE_META + IMAGE_JPEG));
        Map<String, Object> root = json.root();
        root.put(CommonFields.CREATED, 1280314920L);
        String author = "Some Tourist";
        root.put(CommonFields.ARTIST, author);
        root.put(CommonFields.AUTHOR, author);
        root.put(CommonFields.HEIGHT, 77L);
        root.put(CommonFields.WIDTH, 103L);
        root.put(CommonFields.ORIENTATION, LANDSCAPE);
        root.put(CommonFields.RATIO, "103:77");
        root.put(CommonFields.TITLE, "Tosteberga Ängar");
        Object keywords = new Json.Headers(
            "grazelands\nnature reserve\nbird watching\ncoast");
        root.put(CommonFields.KEYWORDS, keywords);
        root.put(CommonFields.SUBJECT, keywords);
        root.put(
            CommonFields.DESCRIPTION,
            new Json.Contains("Bird site in north eastern Skåne, Sweden."));
        root.put(CommonFields.EXIF_ORIENTATION, 1L);
        DiskHandlerTest.testJson("commented.jpg", json);

        root.remove(CommonFields.ARTIST);
        root.remove(CommonFields.AUTHOR);
        root.remove(CommonFields.KEYWORDS);
        root.remove(CommonFields.SUBJECT);
        root.remove(CommonFields.TITLE);
        root.put(CommonFields.CREATED, 1280314932L);
        root.put(CommonFields.HEIGHT, 76L);
        root.put(CommonFields.WIDTH, 102L);
        root.put(CommonFields.RATIO, "51:38");
        root.put(CommonFields.MANUFACTURER, "Nokia");
        root.put(CommonFields.MODEL, "N78");
        root.put(CommonFields.COMMENT, root.remove(CommonFields.DESCRIPTION));
        root.put(
            CommonFields.DESCRIPTION,
            "Licensed to the Apache Software Foundation (ASF) under one or "
            + "more contributor license agreements. See the NOTICE file "
            + "distributed with this work for additional information "
            + "regarding copyright ownership.");
        root.put(CommonFields.ALTITUDE, 37L);
        root.put(CommonFields.LONGITUDE, 14.462778);
        root.put(CommonFields.LATITUDE, 56.0125);
        root.put(CommonFields.EXIF_ORIENTATION, 1L);
        DiskHandlerTest.testJson("keywords.jpg", json);
    }

    @Test
    public void testJpgNewGps() throws Exception {
        Json json = new Json(
            IMAGE_JPEG,
            "",
            true,
            null,
            new Json.Contains(CONTENT_TYPE_META + IMAGE_JPEG));
        Map<String, Object> root = json.root();
        root.put(CommonFields.CREATED, 1383157394L);
        root.put(CommonFields.MODIFIED, 1383157394L);
        root.put(CommonFields.HEIGHT, 2976L);
        root.put(CommonFields.WIDTH, 3968L);
        root.put(CommonFields.ORIENTATION, LANDSCAPE);
        root.put(CommonFields.RATIO, RATIO_4X3);
        root.put(CommonFields.MANUFACTURER, "OLYMPUS IMAGING CORP.");
        root.put(CommonFields.MODEL, "TG-2");
        root.put(CommonFields.DESCRIPTION, "OLYMPUS DIGITAL CAMERA");
        root.put(CommonFields.EXIF_ORIENTATION, 1L);
        DiskHandlerTest.testJson("newgps.jpg", json);
    }

    @Test
    public void testPng() throws Exception {
        Json json = new Json(
            IMAGE_PNG,
            "",
            true,
            null,
            new Json.Contains(CONTENT_TYPE_META + IMAGE_PNG));
        Map<String, Object> root = json.root();
        String comment = "Sample image file for TweakPNG.\n"
            + "Sample intėrnătiőnāl text chunk.";
        root.put(CommonFields.COMMENT, comment);
        root.put(CommonFields.MODIFIED, 1275735600L);
        root.put(CommonFields.HEIGHT, 150L);
        root.put(CommonFields.WIDTH, 300L);
        root.put(CommonFields.ORIENTATION, LANDSCAPE);
        root.put(CommonFields.RATIO, "2:1");
        DiskHandlerTest.testJson("itxt.png", json);
    }

    @Test
    public void testPlakatPng() throws Exception {
        Json json = new Json(
            IMAGE_PNG,
            "",
            true,
            null,
            new Json.Contains(CONTENT_TYPE_META + IMAGE_PNG));
        Map<String, Object> root = json.root();
        root.put(CommonFields.HEIGHT, 29292L);
        root.put(CommonFields.WIDTH, 10040L);
        root.put(CommonFields.ORIENTATION, PORTRAIT);
        root.put(CommonFields.RATIO, "7323:2510");
        DiskHandlerTest.testJson("plakat.png", json);
    }

    @Test
    public void testBadPngInput() throws Exception {
        ContentProducer producer = new ContentProducer() {
            @Override
            public void writeTo(final OutputStream out) throws IOException {
                byte[] buf = new byte[TIMEOUT];
                try (InputStream in = new FileInputStream(
                        Paths.getSandboxResourcesRoot() + "/wallpaper.png"))
                {
                    final int toRead = 2000000;
                    int left = toRead;
                    while (left > 0) {
                        int read = in.read(buf, 0, Math.min(left, buf.length));
                        out.write(buf, 0, read);
                        out.flush();
                        left -= read;
                    }
                    try {
                        Thread.sleep(TIMEOUT);
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }
        };
        try (CloseableHttpClient client = HttpClients.createDefault();
            StaticServer backend = new StaticServer(Configs.baseConfig());
            Server server = new Server(
                ServerTest.getConfig(
                    backend.port(),
                    "\nserver.fragment-size-hint = 0")))
        {
            backend.add("/get/wallpaper.png?raw", producer);
            backend.start();
            server.start();
            try (CloseableHttpResponse response = client.execute(new HttpGet(
                    "http://localhost:" + server.port()
                    + "/get/wallpaper.png?name=disk")))
            {
                Assert.assertEquals(
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                DiskHandlerTest.assertInvalidJson(response);
            }
        }
    }

    @Test
    public void testThumbnailJpg() throws Exception {
        Json json = new Json(
            IMAGE_JPEG,
            "",
            true,
            null,
            new Json.AllOf(
                new Json.Contains(CONTENT_TYPE_META + IMAGE_JPEG),
                new Json.Contains("X Resolution:300 dots per inch")));
        Map<String, Object> root = json.root();
        root.put(CommonFields.CREATED, 1400960575L);
        root.put(CommonFields.MODIFIED, 1402875370L);
        root.put(CommonFields.HEIGHT, 3008L);
        root.put(CommonFields.WIDTH, 2000L);
        root.put(CommonFields.ORIENTATION, PORTRAIT);
        root.put(CommonFields.RATIO, "188:125");
        root.put(CommonFields.MANUFACTURER, NIKON);
        root.put(CommonFields.MODEL, "NIKON D40");
        root.put(CommonFields.ARTIST, "Picasa");
        DiskHandlerTest.testJson("thumbnail.jpg", json);
    }

    @Test
    public void testNef() throws Exception {
        Json json = new Json(
            IMAGE_RAW_NIKON,
            "",
            true,
            null,
            new Json.Contains(CONTENT_TYPE_META + "image/x-nikon-nef"));
        Map<String, Object> root = json.root();
        root.put(CommonFields.CREATED, 1224845868L);
        root.put(CommonFields.MODIFIED, 1224845868L);
        root.put(CommonFields.HEIGHT, 2844L);
        root.put(CommonFields.WIDTH, 4288L);
        root.put(CommonFields.ORIENTATION, LANDSCAPE);
        root.put(CommonFields.RATIO, "1072:711");
        root.put(CommonFields.MANUFACTURER, NIKON);
        root.put(CommonFields.MODEL, "NIKON D700");
        root.put(CommonFields.EXIF_ORIENTATION, 1L);
        String file = "image.nef";
        DiskHandlerTest.testJson(file, json, "&mimetype=image/x-nikon-nef");
        root.put(
            CommonFields.META,
            new Json.Contains(CONTENT_TYPE_META + IMAGE_RAW_NIKON));
        DiskHandlerTest.testJson(file, json, "&mimetype=image/x-raw-nikon");
    }

    @Test
    public void testTiff() throws Exception {
        Json json = new Json(
            IMAGE_TIFF,
            "",
            true,
            null,
            new Json.Contains(CONTENT_TYPE_META + IMAGE_TIFF));
        Map<String, Object> root = json.root();
        root.put(CommonFields.HEIGHT, 281L);
        root.put(CommonFields.WIDTH, 900L);
        root.put(CommonFields.ORIENTATION, LANDSCAPE);
        root.put(CommonFields.RATIO, "900:281");
        root.put(CommonFields.EXIF_ORIENTATION, 1L);
        String file = "dilbert.tiff";
        DiskHandlerTest.testJson(file, json);
        DiskHandlerTest.testJson(
            file,
            new Json(
                IMAGE_TIFF,
                null,
                false,
                null,
                null),
            "",
            "extractor.tmp-file-limit = 100K");
    }

    @Test
    public void testAutoRotate() throws Exception {
        Json json = new Json(
            IMAGE_JPEG,
            "",
            true,
            null,
            new Json.Contains(CONTENT_TYPE_META + IMAGE_JPEG));
        Map<String, Object> root = json.root();
        root.put(CommonFields.CREATED, 1456676723L);
        root.put(CommonFields.MODIFIED, 1456676723L);
        root.put(CommonFields.HEIGHT, 3264L);
        root.put(CommonFields.WIDTH, 2448L);
        root.put(CommonFields.ORIENTATION, PORTRAIT);
        root.put(CommonFields.RATIO, RATIO_4X3);
        root.put(CommonFields.ALTITUDE, 90.24d);
        root.put(CommonFields.LATITUDE, 59.567642);
        root.put(CommonFields.LONGITUDE, 30.123681);
        root.put(CommonFields.MANUFACTURER, APPLE);
        root.put(CommonFields.MODEL, "iPhone 6 Plus");
        root.put(CommonFields.EXIF_ORIENTATION, 6L);
        DiskHandlerTest.testJson("gatchina.jpg", json);
    }

    @Test
    public void testHeic() throws Exception {
        Json json = new Json(
            IMAGE_HEIF,
            "",
            true,
            null,
            new Json.Contains(CONTENT_TYPE_META + IMAGE_HEIF));
        Map<String, Object> root = json.root();
        root.put(CommonFields.HEIGHT, 3024L);
        root.put(CommonFields.WIDTH, 4032L);
        root.put(CommonFields.ORIENTATION, LANDSCAPE);
        root.put(CommonFields.RATIO, RATIO_4X3);
        root.put(CommonFields.ALTITUDE, 137.16d);
        root.put(CommonFields.LATITUDE, 55.735028);
        root.put(CommonFields.LONGITUDE, 37.585592);
        root.put(CommonFields.MANUFACTURER, APPLE);
        root.put(CommonFields.MODEL, IPHONE_7_PLUS);
        root.put(CommonFields.EXIF_ORIENTATION, 1L);
        root.put(CommonFields.CREATED, 1505481188L);
        root.put(CommonFields.MODIFIED, 1505481188L);
        DiskHandlerTest.testJson("test.heic", json);
    }

    @Test
    public void testRotatedHeic() throws Exception {
        Json json = new Json(
            IMAGE_HEIF,
            "",
            true,
            null,
            new Json.Contains(CONTENT_TYPE_META + IMAGE_HEIF));
        Map<String, Object> root = json.root();
        root.put(CommonFields.HEIGHT, 4032L);
        root.put(CommonFields.WIDTH, 3024L);
        root.put(CommonFields.ORIENTATION, PORTRAIT);
        root.put(CommonFields.RATIO, RATIO_4X3);
        root.put(CommonFields.ALTITUDE, 136.92d);
        root.put(CommonFields.LATITUDE, 55.734989);
        root.put(CommonFields.LONGITUDE, 37.585664);
        root.put(CommonFields.MANUFACTURER, APPLE);
        root.put(CommonFields.MODEL, IPHONE_7_PLUS);
        root.put(CommonFields.EXIF_ORIENTATION, 5L);
        root.put(CommonFields.CREATED, 1505508457L);
        root.put(CommonFields.MODIFIED, 1505508457L);
        DiskHandlerTest.testJson("rotated.heic", json);
    }

    // This files contains iinf block with mime info, which have content type,
    // but not space for content encoding, which caused excessive read, leading
    // to wrong file position
    @Test
    public void testEofHeic() throws Exception {
        Json json = new Json(
            IMAGE_HEIF,
            "",
            true,
            null,
            new Json.Contains(CONTENT_TYPE_META + IMAGE_HEIF));
        Map<String, Object> root = json.root();
        root.put(CommonFields.HEIGHT, 3024L);
        root.put(CommonFields.WIDTH, 4032L);
        root.put(CommonFields.ORIENTATION, LANDSCAPE);
        root.put(CommonFields.RATIO, RATIO_4X3);
        root.put(CommonFields.ALTITUDE, 164.24);
        root.put(CommonFields.LATITUDE, 55.771156);
        root.put(CommonFields.LONGITUDE, 37.675336);
        root.put(CommonFields.MANUFACTURER, APPLE);
        root.put(CommonFields.MODEL, "iPhone XS Max");
        root.put(CommonFields.EXIF_ORIENTATION, 1L);
        root.put(CommonFields.CREATED, 1547116759L);
        root.put(CommonFields.MODIFIED, 1547116759L);
        DiskHandlerTest.testJson("eof.heic", json);
    }

    @Test
    public void testStrippedLandscapeHeic() throws Exception {
        Json json = new Json(
            IMAGE_HEIF,
            "",
            true,
            null,
            new Json.Contains(CONTENT_TYPE_META + IMAGE_HEIF));
        Map<String, Object> root = json.root();
        root.put(CommonFields.HEIGHT, 3024L);
        root.put(CommonFields.WIDTH, 4032L);
        root.put(CommonFields.ORIENTATION, LANDSCAPE);
        root.put(CommonFields.RATIO, RATIO_4X3);
        DiskHandlerTest.testJson("eof-stripped.heic", json);
    }

    @Test
    public void testGeoHeic() throws Exception {
        Json json = new Json(
            IMAGE_HEIF,
            "",
            true,
            null,
            new Json.Contains(CONTENT_TYPE_META + IMAGE_HEIF));
        Map<String, Object> root = json.root();
        root.put(CommonFields.HEIGHT, 4032L);
        root.put(CommonFields.WIDTH, 3024L);
        root.put(CommonFields.ORIENTATION, PORTRAIT);
        root.put(CommonFields.RATIO, RATIO_4X3);
        root.put(CommonFields.ALTITUDE, 134.07d);
        root.put(CommonFields.LATITUDE, 55.734039);
        root.put(CommonFields.LONGITUDE, 37.586675);
        root.put(CommonFields.MANUFACTURER, APPLE);
        root.put(CommonFields.MODEL, "iPhone 7");
        root.put(CommonFields.EXIF_ORIENTATION, 5L);
        root.put(CommonFields.CREATED, 1539734674L);
        root.put(CommonFields.MODIFIED, 1539734674L);
        DiskHandlerTest.testJson("geo.heic", json);
    }

    @Test
    public void testStrippedPortraitHeic() throws Exception {
        Json json = new Json(
            IMAGE_HEIF,
            "",
            true,
            null,
            new Json.Contains(CONTENT_TYPE_META + IMAGE_HEIF));
        Map<String, Object> root = json.root();
        root.put(CommonFields.HEIGHT, 4032L);
        root.put(CommonFields.WIDTH, 3024L);
        root.put(CommonFields.ORIENTATION, PORTRAIT);
        root.put(CommonFields.RATIO, RATIO_4X3);
        DiskHandlerTest.testJson("geo-stripped.heic", json);
    }

    @Test
    public void testGpsFlowers() throws Exception {
        Json json = new Json(
            IMAGE_PNG,
            "",
            true,
            null,
            new Json.Contains(CONTENT_TYPE_META + IMAGE_PNG));
        Map<String, Object> root = json.root();
        root.put(CommonFields.ALTITUDE, 0L);
        root.put(CommonFields.LATITUDE, 30.034782166666666d);
        root.put(CommonFields.LONGITUDE, 31.229186666666667d);
        root.put(CommonFields.HEIGHT, 1136L);
        root.put(CommonFields.WIDTH, 640L);
        root.put(CommonFields.ORIENTATION, PORTRAIT);
        root.put(CommonFields.RATIO, "71:40");
        DiskHandlerTest.testJson("gps-flowers.png", json);
    }
}
// CSON: MagicNumber

