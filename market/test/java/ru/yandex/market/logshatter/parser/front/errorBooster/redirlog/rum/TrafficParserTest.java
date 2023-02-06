package ru.yandex.market.logshatter.parser.front.errorBooster.redirlog.rum;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.UnsignedLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.health.configs.logshatter.useragent.FakeUserAgentDetector;
import ru.yandex.market.health.configs.logshatter.useragent.UserAgentDetector;
import ru.yandex.market.logshatter.parser.LogParserChecker;
import ru.yandex.market.logshatter.parser.front.errorBooster.Environment;
import ru.yandex.market.logshatter.parser.front.errorBooster.Platform;

public class TrafficParserTest {
    private static final Map<String, String> FAKE_YABRO_TRAITS = ImmutableMap.<String, String>builder()
        .put(UserAgentDetector.BROWSER_BASE, "Chromium")
        .put(UserAgentDetector.BROWSER_BASE_VERSION, "68.0.3440.106")
        .put(UserAgentDetector.BROWSER_ENGINE, "WebKit")
        .put(UserAgentDetector.BROWSER_ENGINE_VERSION, "537.36")
        .put(UserAgentDetector.BROWSER_NAME, "YandexBrowser")
        .put(UserAgentDetector.BROWSER_VERSION, "18.9.0.3363")
        .put("CSP1Support", "true")
        .put("CSP2Support", "true")
        .put(UserAgentDetector.OS_FAMILY, "MacOS")
        .put(UserAgentDetector.OS_NAME, "Mac OS X Sierra")
        .put(UserAgentDetector.OS_VERSION, "10.12.6")
        .put("SVGSupport", "true")
        .put("WebPSupport", "true")
        .put("YaGUI", "2.5")
        .put("historySupport", "true")
        .put("isBrowser", "true")
        .put(UserAgentDetector.IS_MOBILE, "false")
        .put("isTouch", "false")
        .put("localStorageSupport", "true")
        .put("postMessageSupport", "true")
        .build();
    private LogParserChecker checker;

    @BeforeEach
    public void setUp() {
        FakeUserAgentDetector detector = new FakeUserAgentDetector();
        detector.setDetectionResult(FAKE_YABRO_TRAITS);
        checker = new LogParserChecker(new TrafficParser());
    }

    @Test
    public void parseFullFields() throws Exception {
        String line = "HTTP_REFERER=https://yandex.ru/search/?text=%D0%B4%D0%B4%D1%82+%D1%81%D0%BA%D0%B0%D1%87%D0%B0" +
            "%D1%82%D1%8C&lr=20137&clid=2256160-321&win=379@@reqid=1581684194807728-94772253687030509800067-vla1-2547" +
            "@@ver=15342@@slots=188118,0,37;186144,0,54;35428,0,2;212820,0,32;209816,0,26;78592,0,46;78597,0,56;" +
            "209908,0,53;206523,0,42;210865,0,63;212278,0,67;211699,0,20;210826,0,75;212533,0,43;210319,0,12;209844," +
            "0,27;205690,0,2;90501,0,63;210471,0,2;212725,0,89;211551,0,2;38813,0,1;66295,0,64;61917,0,56;61913,0,70;" +
            "61909,0,56;211835,0,56;171123,0,86;202835,0,49;1283,0,83;56262,0,14;58229,0,86;60,0,90;209871,0,11;" +
            "208414,0,97;211372,0,82;85056,0,34;210277,0,72;210900,0,92;151171,0,31;210985,0,85;203222,0,49;83077,0," +
            "78;208761,0,92;135686,0,50;206196,0,11;205883,0,11;206120,0,11;205886,0,11;213070,0,51;210913,0,77;" +
            "212098,0,69;209938,0,77@@ruip=95.55.234" +
            ".147@@u=512101101516553622@@reg=20137@@dtype=iweb@@ref" +
            "=orjY4mGPRjk5boDnW0uvlrrd71vZw9kpVBUyA8nmgRH_oAUI76yuO" +
            "-TYE7U4WF62wRErppVlFf9rp5SUGhgTDrK0A29ZC9cW3gm2yJuAH3XqzndgyBKpcy05mNc39onv9iQjmbevYVUjV" +
            "-f0ble4gPGKOw_FWNmTTfbz4O3fZOQ,@@reqid=1581684194807728-94772253687030509800067-vla1-2547@@path=690.2096" +
            ".361@@vars=143=28.1034.153,287=-1,-project=web4,-page=serp,-platform=desktop,-env=production," +
            "-version=0x52f24c78209,1042=Mozilla%2F5.0%20(Windows%20NT%2010.0%3B%20WOW64)%20AppleWebKit%2F537.36%20" +
            "(KHTML%2C%20like%20Gecko)%20Chrome%2F79.0.3945.136%20YaBrowser%2F20.2.1.234%20Yowser%2F2" +
            ".5%20Safari%2F537.36,d=yandex.ru-!3!41;yastatic.net-js!16!0;avatars.mds.yandex.net-!13!173153;yastatic" +
            ".net-gif!1!0;favicon.yandex.net-ru!1!0;im0-tub-ru.yandex.net-!6!141540;yastatic.net-svg!7!1705;mc.yandex" +
            ".ru-js!1!0;yastatic.net-css!1!0;yastatic.net-png!2!0;mc.yandex.ru-!1!0;an.yandex.ru-js!1!0;im0-tub-com" +
            ".yandex.net-!10!100000;fonts.gstatic.com-!100000!5000000000;,t=6771909.835,-cdn=spb,-mc=1," +
            "-js-ready=1@@cts=1581690967874@@at=3@@uah=4168145558@@icookie=512101101516553622@@x-req-id" +
            "=1581690966489749-8997657823069814990@@robotness=0.0@@user_agent=Mozilla/5.0 (Windows NT 10.0; WOW64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.136 YaBrowser/20.2.1.234 Yowser/2.5 Safari/537" +
            ".36@@url=//yandex.ru/@@1581690966@@95.55.234.147,95.55.234.147@@512101101516553622";
        checker.setParam("allowedTypes", "js,svg,css,png,txt,gif,mp4,jsx,cur,html,jpeg,jpg,ico,pdf,ts,m4s,mpd,m3u8," +
            "webm,ogv");

        checker.check(
            line,
            new Date(1581690966000L),
            "web4", // PROJECT
            "", // SERVICE
            "serp", // PAGE
            Platform.DESKTOP, // PLATFORM
            "", // URL
            UnsignedLong.valueOf("2202906307356721367"), // URL_ID
            "yandex.ru", //VHOST
            Environment.PRODUCTION, // ENVIRONMENT
            Arrays.asList(60, 1283, 35428, 38813, 56262, 58229, 61909, 61913, 61917, 66295, 78592, 78597, 83077,
                85056, 90501, 135686, 151171, 171123, 186144, 188118, 202835, 203222, 205690, 205883, 205886, 206120,
                206196, 206523, 208414, 208761, 209816, 209844, 209871, 209908, 209938, 210277, 210319, 210471,
                210826, 210865, 210900, 210913, 210985, 211372, 211551, 211699, 211835, 212098, 212278, 212533,
                212725, 212820, 213070), // TEST_IDS
            Arrays.asList(), // EXP_FLAGS
            "WebKit", // BROWSER_ENGINE
            "537.36", // BROWSER_ENGINE_VERSION
            "YandexBrowser", // BROWSER_NAME
            "18.9.0.3363", // BROWSER_VERSION
            "18.9", // BROWSER_VERSION_MAJOR
            "Chromium", // BROWSER_BASE
            "MacOS", // OS_FAMILY
            "Mac OS X Sierra", // OS_NAME
            "10.12.6", // OS_VERSION
            "10.12", // OS_VERSION_MAJOR
            "", // DEVICE_NAME
            "", // DEVICE_VENDOR
            false, // IN_APP_BROWSER
            false, // IS_ROBOT
            false, // IS_TV
            false, // IS_TABLET
            false, // IS_TOUCH
            false, // IS_MOBILE
            false, // ADBLOCK
            "0x52f24c78209", // VERSION
            0, // REGION
            "1581684194807728-94772253687030509800067-vla1-2547", // REQUEST_ID
            UnsignedLong.valueOf("1605842161392668274"), // REQUEST_ID_HASH
            UnsignedLong.valueOf("512101101516553622"), // YANDEXUID
            Arrays.asList(), // KV_KEYS
            Arrays.asList(), // KV_VALUES,
            false, // IS_INTERNAL
            "spb", // CDN
            "::ffff:95.55.234.147", // IPv6
            false, // LOGGED_IN
            6771909L, // TIME
            Arrays.asList("favicon.yandex.net", "yastatic.net", "_im-tub.yandex.net_", "an.yandex.ru",
                "yastatic.net", "mc.yandex.ru", "avatars.mds.yandex.net", "yastatic.net", "yastatic.net",
                "mc.yandex.ru", "yastatic.net", "yandex.ru", "fonts.gstatic.com"), // COLUMN_RESOURCES_HOST
            Arrays.asList("png", "png", "unknown", "js", "js", "unknown", "unknown", "gif", "svg", "js", "css",
                "unknown", "unknown"), // COLUMN_TYPE
            Arrays.asList(1, 2, 16, 1, 16, 1, 13, 1, 7, 1, 1, 3, 65535), // COLUMN_RESOURCES_COUNT
            Arrays.asList(0L, 0L, 241540L, 0L, 0L, 0L, 173153L, 0L, 1705L, 0L, 0L, 41L, 4294967295L) //
            // COLUMN_RESOURCES_SIZE
        );
    }
}
