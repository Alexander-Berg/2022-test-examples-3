package ru.yandex.tikaite.server;

import java.util.Map;

import org.junit.Test;

import ru.yandex.test.util.TestBase;
import ru.yandex.tikaite.util.CommonFields;
import ru.yandex.tikaite.util.Json;

public class DiskVideoTest extends TestBase {
    private static final String CONTENT_TYPE_META = "Content-Type:";

    @Test
    public void testGpsMov() throws Exception {
        Json json = new Json(
            "video/quicktime",
            "",
            true,
            null,
            "xmpDM:audioSampleRate:600\nContent-Type:video/quicktime");
        Map<String, Object> root = json.root();
        root.put(CommonFields.CREATED, 1582963956L);
        root.put(CommonFields.MODIFIED, 1582963958L);
        root.put(CommonFields.HEIGHT, 1080L);
        root.put(CommonFields.WIDTH, 1920L);
        root.put(CommonFields.RATIO, "16:9");
        root.put(CommonFields.DURATION, 2L);
        root.put(CommonFields.MODEL, "iPhone 11 Pro");
        root.put(CommonFields.LATITUDE, 55.9138);
        root.put(CommonFields.LONGITUDE, 37.4901);
        root.put(CommonFields.ORIENTATION, "landscape");
        DiskHandlerTest.testJson("iphone.11.pro.mov", json, "&mimetype=video/quicktime");
    }

    @Test
    public void testQuicktime() throws Exception {
        Json json = createQuicktimeJson(true);
        Map<String, Object> root = json.root();
        root.put(CommonFields.CREATED, 1468078063L);
        root.put(CommonFields.MODIFIED, 1468078112L);
        root.put(CommonFields.HEIGHT, 1080L);
        root.put(CommonFields.WIDTH, 1920L);
        root.put(CommonFields.LATITUDE, 55.7874);
        root.put(CommonFields.LONGITUDE, 37.5852);
        root.put(CommonFields.MODEL, "iPhone 6");
        root.put(CommonFields.RATIO, "16:9");
        root.put(CommonFields.ORIENTATION, CommonFields.LANDSCAPE);
        root.put(CommonFields.DURATION, 49L);
        String file = "quicktime.mov";
        DiskHandlerTest.testJson(
            file,
            json,
            "",
            "extractor.tmp-file-limit = 50M");
        DiskHandlerTest.testJson(
            file,
            createQuicktimeJson(false),
            "",
            "extractor.tmp-file-limit = 10M");
    }

    private Json createQuicktimeJson(final boolean parsed) {
        String bodyText;
        Object meta;
        if (parsed) {
            bodyText = "";
            meta = new Json.Headers(
                CONTENT_TYPE_META
                    + "video/quicktime\nxmpDM:audioSampleRate:600");
        } else {
            bodyText = null;
            meta = null;
        }
        return new Json(
            "video/quicktime",
            bodyText,
            parsed,
            null,
            meta);
    }
}
