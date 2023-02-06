package ru.yandex.market.logshatter.parser.front.errorBooster.redirlog.errors;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.google.common.primitives.UnsignedLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import ru.yandex.market.health.configs.logshatter.sanitizer.FakeSanitizer;
import ru.yandex.market.health.configs.logshatter.useragent.UatraitsUserAgentDetector;
import ru.yandex.market.health.configs.logshatter.useragent.UserAgentDetector;
import ru.yandex.market.logshatter.parser.LogParserChecker;
import ru.yandex.market.logshatter.parser.front.errorBooster.Environment;
import ru.yandex.market.logshatter.parser.front.errorBooster.LogLevel;
import ru.yandex.market.logshatter.parser.front.errorBooster.Parser;
import ru.yandex.market.logshatter.parser.front.errorBooster.Platform;
import ru.yandex.market.logshatter.parser.front.errorBooster.Runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ErrorsParserTest {
    private static final String YABRO_UA = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_6)" +
        " AppleWebKit/537.36 (KHTML, like Gecko) Chrome/68.0.3440.106 YaBrowser/18.9.0.3363 Yowser/2.5 Safari/537.36";
    private LogParserChecker checker;

    @BeforeEach
    public void setUp() {
        checker = new LogParserChecker(new ErrorsParser(new FakeSanitizer()));
        checker.setLogBrokerTopic("redirlog-errors-topic");
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void parseAllFields() throws Exception {
        String line = "HTTP_REFERER=https://yandex.ru/search/?lr=39&clid=2242347&msid=1543764562.4732.122087.856243" +
            "&text=%D0%BF%D1%80%D0%BE%D0%BC%D0%B5%D1%82%D0%B5%D0%B9@@path=690.2361" +
            "@@dtype=iweb@@reg=39" +
            "@@robotness=1.0@@is_internal=1" +
            "@@cts=1543764674355" +
            "@@ruip=178.76.222.51@@url=//yandex.ru/" +
            "@@u=6042811641515572709" +
            "@@ref=orjY4mGPRjk5boDnW0uvlrrd71vZw9kpibIAK_uZ4bX2d0VUnsewRf_stGH-rdQtwS0S5EvsSnxDv4ypraMsYzBhHDc" +
            "_86aovcpsP_A6GOMb1PMknnHJs0KFJIRgtwG2ehrNoyvUE5-djQpgQPigXBjHh6qlVPExRayUO03E2CEEnoWT0ywmGg,," +
            "@@ver=14644@@at=3@@icookie=6042811641515572708" +
            "@@reqid=11111%2F1543764671160555-947891346418723336261425-man1-4034" +
            "@@slots=105520,0,79;103926,0,34;104582,0,35;63207,0,56;103849,0,55" +
            "@@vars=-stack=Error%3A%20Error!%0A%20%20%20%20at%20https%3A%2F%2Fwww-rapido.wdevx.yandex.ru%2Ftmpl%2Frum" +
            "%2Ferror-counter%2Fexample%2Fbuild%2Findex_over_rum.html%3A966%3A23,-env=prestable," +
            "-experiments=recommendations%3Aitem2item%3Bselection_events%3Arecommended%3Brubric_landing%3Adefault%3B" +
            "best_filter%3Aon%3Bconcert_recommended_selection%3Aoff%3Bactual_events_main%3Aon," +
            "-additional=%7B%22resource%22%3A%7B%22content_id%22%3A%22482bf91b3a2151fa8f11e763486fa0f4%22%2C%22" +
            "content_type%22%3A%22channel%22%7D%2C%22from%22%3A%22morda%22%2C%22from_block%22%3A%22" +
            "media-footer_stream_item%22%2C%22count%22%3A%2010%7D," +
            "-msg=Unhandled%20rejection%3A%20Error!," +
            "-project=appmetrica%2Didea%2Dplugin,-platform=desktop,-version=0xrelease%2Fdesktop%2Fv1.157.0," +
            "-region=121567,-level=error,-page=touch%3Aproduct," +
            "-line=3,-col=1488,-block=video-search_WITH_VERY_VERY_SECRET_,-method=getData," +
            "-url=https%3A%2F%2Fyastatic.net%2Fyandex-video-player-iframe-api" +
            "%2Fjs%2Fstream_player_video_search.min.js," +
            "-source=dzen,-sourceMethod=last_posts,-type=network,-loggedin=1," +
            "-service=zen%3Asite_desktop%3Ayandex%3Asite_desktop," +
            "-adb=1," +
            "-host=error-booster-api-1.vla.yp-c.yandex.net," +
            "-coordinates_gp=42_593994140625%3A-5_60302734375%3A500_0000000%3A1%3A1543330234243443," +
            "-dc=vla," +
            "-cdn=mskcdn," +
            "-referrer=https://yandex.com/search/text=%D0%BF%D1%80%D0%BE%D0%BC%D0%B5%D1%82%D0%B5%D0%B9," +
            "-yandexuid=8567101671562274770," +
            "-ua=Mozilla%2F5.0%20(Macintosh%3B%20Intel%2" +
            "0Mac%20OS%20X%2010_12_6)%20AppleWebKit%2F537.36%20(KHTML%2C%20like%20Gecko)%20Chrome%2F68.0.3440.106%20Y" +
            "aBrowser%2F18.9.0.3363%20Yowser%2F2.5%20Safari%2F537.36,-silent=yes,-ts=1543330234716," +
            "init-ts=1533330234716" +
            "@@1543764674@@178.76.222.51,178.76.222.51@@6042811641515572709";

        List<Integer> testIds = Arrays.asList(63207, 103849, 103926, 104582, 105520);
        List<String> expFlags = Arrays.asList(
            "recommendations:item2item",
            "selection_events:recommended",
            "rubric_landing:default",
            "best_filter:on",
            "concert_recommended_selection:off",
            "actual_events_main:on"
        );

        String stackTrace = "Error: Error!\n" +
            "    at {{REPLACED_STACKTRACE_URL_0}}";

        String originalStackTrace = "Error: Error!\n" +
            "    at https://www-rapido.wdevx.yandex.ru/tmpl/rum/error-counter/example/build/index_over_rum.html:966:23";

        List<String> kvKeys = Arrays.asList("resource", "from", "from_block", "count");
        List<String> kvValues = Arrays.asList(
            "{\"content_id\":\"482bf91b3a2151fa8f11e763486fa0f4\",\"content_type\":\"channel\"}",
            "morda",
            "media-footer_stream_item",
            "10"
        );

        checker.check(
            line,
            new Date(1543764674000L),
            "appmetrica-idea-plugin", // PROJECT
            "zen:site_desktop:yandex:site_desktop", // SERVICE
            "touch:product", // PAGE
            Platform.DESKTOP, // PLATFORM
            "https://yandex.com/search/text=прометей", // URL
            UnsignedLong.valueOf("13322248798649617483"), // URL_ID
            "yandex.com", //VHOST
            Environment.PRESTABLE, // ENVIRONMENT
            testIds, // TEST_IDS
            expFlags, // EXP_FLAGS
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
            true, // IS_ROBOT
            false, // IS_TV
            false, // IS_TABLET
            false, // IS_TOUCH
            false, // IS_MOBILE
            true, // ADBLOCK
            "0xrelease/desktop/v1.157.0", // VERSION
            121567, // REGION
            "11111/1543764671160555-947891346418723336261425-man1-4034", // REQUEST_ID
            UnsignedLong.valueOf("5560926446039861426"), // REQUEST_ID_HASH
            UnsignedLong.valueOf("8567101671562274770"), // YANDEXUID
            kvKeys, // KV_KEYS
            kvValues, // KV_VALUES
            true, // IS_INTERNAL
            "mskcdn", // CDN
            "::ffff:178.76.222.51", // IPv6
            true, // LOGGED_IN
            "Unhandled rejection: Error!", // MESSAGE
            UnsignedLong.valueOf("2061895500485984885"), // MESSAGE_ID
            "Unhandled rejection: Error!", // ORIGINAL_MESSAGE
            Arrays.asList(), // ERROR_URLS_KEYS
            Arrays.asList(), // ERROR_URLS_VALUES
            Runtime.BROWSERJS, // RUNTIME
            LogLevel.ERROR, // LEVEL
            "https://yastatic.net/yandex-video-player-iframe-api/js/stream_player_video_search.min.js", // FILE
            UnsignedLong.valueOf("5098545656749072563"), // FILE_ID
            "video-search_WITH_VERY_VERY_SECRET_", // BLOCK
            "getData", // METHOD
            3, // LINE
            1488, // COL
            stackTrace, // STACK_TRACE
            UnsignedLong.valueOf("6064961238869675173"), // STACK_TRACE_ID
            originalStackTrace, // ORIGINAL_STACK_TRACE
            Arrays.asList("(anonymous)"), // STACK_TRACE_NAMES
            Arrays.asList("https://www-rapido.wdevx.yandex.ru/tmpl/rum/error-counter/example/build/index_over_rum" +
                ".html"), // STACK_TRACE_URLS
            Arrays.asList(966), // STACK_TRACE_LINES
            Arrays.asList(23), // STACK_TRACE_COLS
            YABRO_UA, // USER_AGENT
            UnsignedLong.valueOf("601701267019334270"), // USER_AGENT_ID
            "dzen", // SOURCE
            "last_posts", // SOURCE_METHOD
            "network", // SOURCE_TYPE
            UnsignedLong.valueOf(1543330234716L), // CLIENT_TIMESTAMP
            UnsignedLong.valueOf(1533330234716L), // CLIENT_INIT_TIMESTAMP
            Arrays.asList("STACKTRACE_0"), // REPLACED_URLS_KEYS
            Arrays.asList("https://www-rapido.wdevx.yandex.ru/tmpl/rum/error-counter/example/build/index_over_rum" +
                ".html:966:23"), // REPLACED_URLS_VALUES
            Parser.REDIR_LOG, // PARSER
            UnsignedLong.valueOf("6042811641515572708"), // ICOOKIE
            true, // SILENT
            "vla", // DC
            42.593994140625d, // COORDINATES_LATITUDE
            -5.60302734375d, // COORDINATES_LONGITUDE
            500, // COORDINATES_PRECISION
            4294967295L, // COORDINATES_TIMESTAMP
            "error-booster-api-1.vla.yp-c.yandex.net", // HOST
            checker.getLogBrokerTopic() // TOPIC
        );
    }

    @Test
    public void parseAMinimalBrowserFields() throws Exception {
        String line = "HTTP_REFERER=https://market.yandex.ru/main@@path=690.2609" +
            "@@is_robot=1@@vars=-project=test,-msg=Script%20error.,-ts=-1543330234716," +
            "@@1550683651@@2a00:1fa2:46a:9a12:650b:b1cf:e907:afd0,94.29.81.173@@";

        UnsignedLong hashOfEmptyString = UnsignedLong.valueOf("2202906307356721367");

        checker.check(
            line,
            new Date(1550683651000L),
            "test", // PROJECT
            "", // SERVICE
            "", // PAGE
            Platform.UNKNOWN, // PLATFORM
            "https://market.yandex.ru/main", // URL
            UnsignedLong.valueOf("7608158466322718070"), // URL_ID
            "market.yandex.ru", // VHOST
            Environment.UNKNOWN, // ENVIRONMENT
            Arrays.asList(), // TEST_IDS
            Arrays.asList(), // EXP_FLAGS
            "", // BROWSER_ENGINE
            "", // BROWSER_ENGINE_VERSION
            "", // BROWSER_NAME
            "", // BROWSER_VERSION
            "", // BROWSER_VERSION_MAJOR
            "", // BROWSER_BASE
            "", // OS_FAMILY
            "", // OS_NAME
            "", // OS_VERSION
            "", // OS_VERSION_MAJOR
            "", // DEVICE_NAME
            "", // DEVICE_VENDOR
            false, // IN_APP_BROWSER
            true, // IS_ROBOT
            false, // IS_TV
            false, // IS_TABLET
            false, // IS_TOUCH
            false, // IS_MOBILE
            false, // ADBLOCK
            "", // VERSION
            0, // REGION
            "", // REQUEST_ID
            hashOfEmptyString, // REQUEST_ID_HASH
            UnsignedLong.valueOf(0), // YANDEXUID
            Arrays.asList(), // KV_KEYS
            Arrays.asList(), // KV_VALUES
            false, // IS_INTERNAL
            "", // CDN
            "2a00:1fa2:46a:9a12:650b:b1cf:e907:afd0", // IPv6
            false, // LOGGED_IN
            "Script error", // MESSAGE
            UnsignedLong.valueOf("750895502580084983"), // MESSAGE_ID
            "Script error.", // ORIGINAL_MESSAGE
            Arrays.asList(), // ERROR_URLS_KEYS
            Arrays.asList(), // ERROR_URLS_VALUES
            Runtime.BROWSERJS, // RUNTIME
            LogLevel.ERROR, // LEVEL
            "", // FILE
            hashOfEmptyString, // FILE_ID
            "", // BLOCK
            "", // METHOD
            0, // LINE
            0, // COL
            "", // STACK_TRACE
            hashOfEmptyString, // STACK_TRACE_ID
            "", // ORIGINAL_STACK_TRACE
            Arrays.asList(), // STACK_TRACE_NAMES
            Arrays.asList(), // STACK_TRACE_URLS
            Arrays.asList(), // STACK_TRACE_LINES
            Arrays.asList(), // STACK_TRACE_COLS
            "", // USER_AGENT
            hashOfEmptyString, // USER_AGENT_ID
            "script", // SOURCE
            "", // SOURCE_METHOD
            "", // SOURCE_TYPE
            UnsignedLong.valueOf(0), // CLIENT_TIMESTAMP
            UnsignedLong.valueOf(0), // CLIENT_INIT_TIMESTAMP
            Arrays.asList(), // REPLACED_URLS_KEYS
            Arrays.asList(), // REPLACED_URLS_VALUES
            Parser.REDIR_LOG, // PARSER
            UnsignedLong.valueOf("0"), // ICOOKIE
            false, // SILENT
            "", // DC
            0d, // COORDINATES_LATITUDE
            0d, // COORDINATES_LONGITUDE
            0, // COORDINATES_PRECISION
            0L, // COORDINATES_TIMESTAMP
            "", // HOST
            checker.getLogBrokerTopic() // TOPIC
        );
    }

    @Test
    public void parseAMinimalNodeFields() throws Exception {
        String line = "HTTP_REFERER=@@path=690.3698" +
            "@@vars=-project=test,-msg=Script%20error.," +
            "@@1550683651@@94.29.81.173@@";

        UnsignedLong hashOfEmptyString = UnsignedLong.valueOf("2202906307356721367");

        checker.check(
            line,
            new Date(1550683651000L),
            "test", // PROJECT
            "", // SERVICE
            "", // PAGE
            Platform.UNKNOWN, // PLATFORM
            "", // URL
            hashOfEmptyString, // URL_ID
            "", // VHOST
            Environment.UNKNOWN, // ENVIRONMENT
            Arrays.asList(), // TEST_IDS
            Arrays.asList(), // EXP_FLAGS
            "", // BROWSER_ENGINE
            "", // BROWSER_ENGINE_VERSION
            "", // BROWSER_NAME
            "", // BROWSER_VERSION
            "", // BROWSER_VERSION_MAJOR
            "", // BROWSER_BASE
            "", // OS_FAMILY
            "", // OS_NAME
            "", // OS_VERSION
            "", // OS_VERSION_MAJOR
            "", // DEVICE_NAME
            "", // DEVICE_VENDOR
            false, // IN_APP_BROWSER
            false, // IS_ROBOT
            false, // IS_TV
            false, // IS_TABLET
            false, // IS_TOUCH
            false, // IS_MOBILE
            false, // ADBLOCK
            "", // VERSION
            0, // REGION
            "", // REQUEST_ID
            hashOfEmptyString, // REQUEST_ID_HASH
            UnsignedLong.valueOf(0), // YANDEXUID
            Arrays.asList(), // KV_KEYS
            Arrays.asList(), // KV_VALUES
            false, // IS_INTERNAL
            "", // CDN
            "::ffff:94.29.81.173", // IPv6
            false, // LOGGED_IN
            "Script error", // MESSAGE
            UnsignedLong.valueOf("750895502580084983"), // MESSAGE_ID
            "Script error.", // ORIGINAL_MESSAGE
            Arrays.asList(), // ERROR_URLS_KEYS
            Arrays.asList(), // ERROR_URLS_VALUES
            Runtime.NODEJS, // RUNTIME
            LogLevel.UNKNOWN, // LEVEL
            "", // FILE
            hashOfEmptyString, // FILE_ID
            "", // BLOCK
            "", // METHOD
            0, // LINE
            0, // COL
            "", // STACK_TRACE
            hashOfEmptyString, // STACK_TRACE_ID
            "", // ORIGINAL_STACK_TRACE
            Arrays.asList(), // STACK_TRACE_NAMES
            Arrays.asList(), // STACK_TRACE_URLS
            Arrays.asList(), // STACK_TRACE_LINES
            Arrays.asList(), // STACK_TRACE_COLS
            "", // USER_AGENT
            hashOfEmptyString, // USER_AGENT_ID
            "", // SOURCE
            "", // SOURCE_METHOD
            "", // SOURCE_TYPE
            UnsignedLong.valueOf(0), // CLIENT_TIMESTAMP
            UnsignedLong.valueOf(0), // CLIENT_INIT_TIMESTAMP
            Arrays.asList(), // REPLACED_URLS_KEYS
            Arrays.asList(), // REPLACED_URLS_VALUES
            Parser.REDIR_LOG, // PARSER
            UnsignedLong.valueOf("0"), // ICOOKIE
            false, // SILENT
            "", // DC
            0d, // COORDINATES_LATITUDE
            0d, // COORDINATES_LONGITUDE
            0, // COORDINATES_PRECISION
            0L, // COORDINATES_TIMESTAMP
            "", // HOST
            checker.getLogBrokerTopic() // TOPIC
        );
    }

    @Test
    public void parseJavaFields() throws Exception {
        String line = "HTTP_REFERER=@@path=690.2361" +
            "@@vars=-project=test,-language=java,-msg=Script%20error.,-block=block_WITH_VERY_VERY_SECRET_," +
            "-stack=Exception%20in%20thread%20%22main%22%20java.lang.RuntimeException%3A%20java.lang.reflect" +
            ".InvocationTargetException%0A%09at%20test.a.pkg.Dog.bark(Dog.java%3A8)%0A%09at%20test.a.pkg.Cat.meow(Cat" +
            ".java%3A6)," +
            "@@1550683651@@94.29.81.173@@";

        UnsignedLong hashOfEmptyString = UnsignedLong.valueOf("2202906307356721367");

        String stacktrace = "Exception in thread \"main\" java.lang.RuntimeException: java.lang.reflect" +
            ".InvocationTargetException\n" +
            "\tat test.a.pkg.Dog.bark(Dog.java:8)\n" +
            "\tat test.a.pkg.Cat.meow(Cat.java:6)";

        checker.setParam("sanitizer", "true");
        checker.check(
            line,
            new Date(1550683651000L),
            "test", // PROJECT
            "", // SERVICE
            "", // PAGE
            Platform.UNKNOWN, // PLATFORM
            "", // URL
            hashOfEmptyString, // URL_ID
            "", // VHOST
            Environment.UNKNOWN, // ENVIRONMENT
            Arrays.asList(), // TEST_IDS
            Arrays.asList(), // EXP_FLAGS
            "", // BROWSER_ENGINE
            "", // BROWSER_ENGINE_VERSION
            "", // BROWSER_NAME
            "", // BROWSER_VERSION
            "", // BROWSER_VERSION_MAJOR
            "", // BROWSER_BASE
            "", // OS_FAMILY
            "", // OS_NAME
            "", // OS_VERSION
            "", // OS_VERSION_MAJOR
            "", // DEVICE_NAME
            "", // DEVICE_VENDOR
            false, // IN_APP_BROWSER
            false, // IS_ROBOT
            false, // IS_TV
            false, // IS_TABLET
            false, // IS_TOUCH
            false, // IS_MOBILE
            false, // ADBLOCK
            "", // VERSION
            0, // REGION
            "", // REQUEST_ID
            hashOfEmptyString, // REQUEST_ID_HASH
            UnsignedLong.valueOf(0), // YANDEXUID
            Arrays.asList(), // KV_KEYS
            Arrays.asList(), // KV_VALUES
            false, // IS_INTERNAL
            "", // CDN
            "::ffff:94.29.81.173", // IPv6
            false, // LOGGED_IN
            "Script error", // MESSAGE
            UnsignedLong.valueOf("750895502580084983"), // MESSAGE_ID
            "Script error.", // ORIGINAL_MESSAGE
            Arrays.asList(), // ERROR_URLS_KEYS
            Arrays.asList(), // ERROR_URLS_VALUES
            Runtime.JAVA, // RUNTIME
            LogLevel.ERROR, // LEVEL
            "", // FILE
            hashOfEmptyString, // FILE_ID
            "block_WITH_VERY_VERY_XXXXXX_", // BLOCK
            "", // METHOD
            0, // LINE
            0, // COL
            stacktrace, // STACK_TRACE
            UnsignedLong.valueOf("5892849190093834809"), // STACK_TRACE_ID
            stacktrace, // ORIGINAL_STACK_TRACE
            Arrays.asList("test.a.pkg.Dog.bark", "test.a.pkg.Cat.meow"), // STACK_TRACE_NAMES
            Arrays.asList("Dog.java", "Cat.java"), // STACK_TRACE_URLS
            Arrays.asList(8, 6), // STACK_TRACE_LINES
            Arrays.asList(0, 0), // STACK_TRACE_COLS
            "", // USER_AGENT
            hashOfEmptyString, // USER_AGENT_ID
            "uncaught", // SOURCE
            "", // SOURCE_METHOD
            "", // SOURCE_TYPE
            UnsignedLong.valueOf(0), // CLIENT_TIMESTAMP
            UnsignedLong.valueOf(0), // CLIENT_INIT_TIMESTAMP
            Arrays.asList(), // REPLACED_URLS_KEYS
            Arrays.asList(), // REPLACED_URLS_VALUES
            Parser.REDIR_LOG, // PARSER
            UnsignedLong.valueOf("0"), // ICOOKIE
            false, // SILENT
            "", // DC
            0d, // COORDINATES_LATITUDE
            0d, // COORDINATES_LONGITUDE
            0, // COORDINATES_PRECISION
            0L, // COORDINATES_TIMESTAMP
            "", // HOST
            checker.getLogBrokerTopic() // TOPIC
        );
    }

    @Test
    public void skipBadPathFields() throws Exception {
        String line = "HTTP_REFERER=@@path=bad" +
            "@@vars=-project=test,-msg=Script%20error.," +
            "@@1550683651@@94.29.81.173,94.29.81.173@@";

        checker.checkEmpty(line);

        String line2 = "???$y?=?ko^?E";

        checker.checkEmpty(line2);
    }

    @Test
    public void skipEmptyProjectFields() throws Exception {
        String line = "HTTP_REFERER=@@path=690.2361" +
            "@@vars=,-msg=Script%20error.," +
            "@@1550683651@@94.29.81.173,94.29.81.173@@";

        checker.checkEmpty(line);
    }

    /**
     * Тест для отладки с настоящей библиотекой uatraits без моков и фейков.
     * Заигнорен по умолчанию - можно запускать локально только при необходимости.
     * Если этот тест не запускается на MacOS, и выводит ошибку про библиотеку libboost_system-mt.dylib,
     * то надо проделать следующие действия:
     * 1. brew install boost@1.55
     * 2. сделать symlink из /usr/local/opt/boost@1.55/lib в /usr/local/lib для библиотек libboost_system-mt.*
     */
    @Test
    @Disabled
    public void detectDesktopBrowser() {
        UserAgentDetector detector = new UatraitsUserAgentDetector();
        detectDesktopBrowser(detector);

    }

    public void detectDesktopBrowser(UserAgentDetector detector) {
        String ua = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0" +
            ".3538.102 YaBrowser/18.11.1.716 Yowser/2.5 Safari/537.36";
        Map<String, String> browser = detector.detect(ua);

        assertEquals("Chromium", browser.get(UserAgentDetector.BROWSER_BASE));
        assertEquals("70.0.3538.102", browser.get(UserAgentDetector.BROWSER_BASE_VERSION));
        assertEquals("YandexBrowser", browser.get(UserAgentDetector.BROWSER_NAME));
        assertEquals("18.11.1.716", browser.get(UserAgentDetector.BROWSER_VERSION));
        assertEquals("WebKit", browser.get(UserAgentDetector.BROWSER_ENGINE));
        assertEquals("537.36", browser.get(UserAgentDetector.BROWSER_ENGINE_VERSION));
        assertEquals("MacOS", browser.get(UserAgentDetector.OS_FAMILY));
        assertEquals("Mac OS X Yosemite", browser.get(UserAgentDetector.OS_NAME));
        assertEquals("10.10", browser.get(UserAgentDetector.OS_VERSION));
        assertEquals("false", browser.get(UserAgentDetector.IS_MOBILE));
        assertEquals(null, browser.get(UserAgentDetector.IS_ROBOT));
        assertEquals(null, browser.get(UserAgentDetector.IS_TV));
        assertEquals(null, browser.get(UserAgentDetector.IS_TABLET));
        assertEquals(null, browser.get(UserAgentDetector.IS_TOUCH));
        assertEquals(null, browser.get(UserAgentDetector.IN_APP_BROWSER));
        assertEquals(null, browser.get(UserAgentDetector.DEVICE_NAME));
        assertEquals(null, browser.get(UserAgentDetector.DEVICE_VENDOR));
    }

    /**
     * Тест для отладки с настоящей библиотекой uatraits без моков и фейков.
     * Заигнорен по умолчанию - можно запускать локально только при необходимости.
     * Если этот тест не запускается на MacOS, и выводит ошибку про библиотеку libboost_system-mt.dylib,
     * то надо проделать следующие действия:
     * 1. brew install boost@1.55
     * 2. сделать symlink из /usr/local/opt/boost@1.55/lib в /usr/local/lib для библиотек libboost_system-mt.*
     */
    @Test
    @Disabled
    public void detectTouchBrowser() {
        UserAgentDetector detector = new UatraitsUserAgentDetector();
        detectTouchBrowser(detector);
    }

    public void detectTouchBrowser(UserAgentDetector detector) {
        String ua = "Mozilla/5.0 (Linux; U; Android 7.1.2; ru-ru; Redmi 4X Build/N2G47H) AppleWebKit/537.36 (KHTML, " +
            "like Gecko) Version/4.0 Chrome/61.0.3163.128 Mobile Safari/537.36 XiaoMi/MiuiBrowser/10.4.3-g";
        Map<String, String> browser = detector.detect(ua);

        assertEquals("Chromium", browser.get(UserAgentDetector.BROWSER_BASE));
        assertEquals("61.0.3163.128", browser.get(UserAgentDetector.BROWSER_BASE_VERSION));
        assertEquals("ChromeMobile", browser.get(UserAgentDetector.BROWSER_NAME));
        assertEquals("61.0.3163", browser.get(UserAgentDetector.BROWSER_VERSION));
        assertEquals("WebKit", browser.get(UserAgentDetector.BROWSER_ENGINE));
        assertEquals("537.36", browser.get(UserAgentDetector.BROWSER_ENGINE_VERSION));
        assertEquals("Android", browser.get(UserAgentDetector.OS_FAMILY));
        assertEquals(null, browser.get(UserAgentDetector.OS_NAME));
        assertEquals("7.1.2", browser.get(UserAgentDetector.OS_VERSION));
        assertEquals("true", browser.get(UserAgentDetector.IS_MOBILE));
        assertEquals(null, browser.get(UserAgentDetector.IS_ROBOT));
        assertEquals(null, browser.get(UserAgentDetector.IS_TV));
        assertEquals("false", browser.get(UserAgentDetector.IS_TABLET));
        assertEquals("true", browser.get(UserAgentDetector.IS_TOUCH));
        assertEquals(null, browser.get(UserAgentDetector.IN_APP_BROWSER));
        assertEquals(" Redmi 4X", browser.get(UserAgentDetector.DEVICE_NAME));
        assertEquals("Xiaomi", browser.get(UserAgentDetector.DEVICE_VENDOR));
    }
}
