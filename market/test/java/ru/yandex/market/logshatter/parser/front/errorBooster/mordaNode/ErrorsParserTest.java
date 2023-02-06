package ru.yandex.market.logshatter.parser.front.errorBooster.mordaNode;


import java.util.Arrays;
import java.util.Date;

import com.google.common.primitives.UnsignedLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.health.configs.logshatter.sanitizer.FakeSanitizer;
import ru.yandex.market.logshatter.parser.LogParserChecker;
import ru.yandex.market.logshatter.parser.front.errorBooster.Environment;
import ru.yandex.market.logshatter.parser.front.errorBooster.LogLevel;
import ru.yandex.market.logshatter.parser.front.errorBooster.Parser;
import ru.yandex.market.logshatter.parser.front.errorBooster.Platform;
import ru.yandex.market.logshatter.parser.front.errorBooster.Runtime;

public class ErrorsParserTest {
    private LogParserChecker checker;

    @BeforeEach
    public void setUp() {
        checker = new LogParserChecker(new ErrorsParser(new FakeSanitizer()));
        checker.setLogBrokerTopic("morda-node-topic");
    }

    @Test
    public void errorParser() throws Exception {
        String line = "tskv\ttskv_format=error-log-morda-node\tctype=unstable\tprj=portal-morda\tunixtime=1559232325" +
            "\ttimestamp=2019-05-30 19:05:25\ttimezone=+0300\tpid=1652\tlevel=error\treqid=1559232325.5826.156486" +
            ".3331\tdc=vla\tstack=streamSettings (common/pages/views/commonViews.view.js:15573:21) -> execView " +
            "(js_libs/core.js:1405:47) -> streamSettings (common/pages/views/commonTouchViews.view.js:2711:20) -> " +
            "execView (js_libs/core.js:1405:47) -> streamSettings (stream/pages/views/streamTouchViews.view" +
            ".js:124:20) -> execView (js_libs/core.js:1405:47) -> streamSettings " +
            "(js_touch_exp/pages-touch/views/touchStreamViews.view.js:27:20) -> execView (js_libs/core.js:1405:47) ->" +
            " stream/pages/views/commonStreamTouchViews.view.js:182:20\tmessage=[stream-settings] Failed to parse " +
            "resourceOverride, Unexpected token , in JSON at position 431";

        String message = "[stream-settings] Failed to parse resourceOverride, Unexpected token , in JSON at position " +
            "431";
        String stacktrace = "streamSettings (common/pages/views/commonViews.view.js:15573:21) -> execView " +
            "(js_libs/core.js:1405:47) -> streamSettings (common/pages/views/commonTouchViews.view.js:2711:20) -> " +
            "execView (js_libs/core.js:1405:47) -> streamSettings (stream/pages/views/streamTouchViews.view" +
            ".js:124:20) -> execView (js_libs/core.js:1405:47) -> streamSettings " +
            "(js_touch_exp/pages-touch/views/touchStreamViews.view.js:27:20) -> execView (js_libs/core.js:1405:47) ->" +
            " stream/pages/views/commonStreamTouchViews.view.js:182:20";

        UnsignedLong hashOfEmptyString = UnsignedLong.valueOf("2202906307356721367");

        checker.check(
            line,
            new Date(1559232325000L),
            "morda", // PROJECT
            "", // SERVICE
            "", // PAGE
            Platform.UNKNOWN, // PLATFORM
            "", // URL
            hashOfEmptyString, // URL_ID
            "", // VHOST
            Environment.PRE_PRODUCTION, // ENVIRONMENT
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
            "1559232325.5826.156486.3331", // REQUEST_ID
            UnsignedLong.valueOf("13474577984309340048"), // REQUEST_ID_HASH
            UnsignedLong.valueOf(0), // YANDEXUID
            Arrays.asList("prj", "ctype"), // KV_KEYS
            Arrays.asList("portal-morda", "unstable"), // KV_VALUES
            false, // IS_INTERNAL
            "", // CDN
            "", // IPv6
            false, // LOGGED_IN
            message, // MESSAGE
            UnsignedLong.valueOf("551033731316614079"), // MESSAGE_ID
            message, // ORIGINAL_MESSAGE
            Arrays.asList(), // ERROR_URLS_KEYS
            Arrays.asList(), // ERROR_URLS_VALUES
            Runtime.NODEJS, // RUNTIME
            LogLevel.ERROR, // LEVEL
            "", // FILE
            hashOfEmptyString, // FILE_ID
            "", // BLOCK
            "", // METHOD
            0, // LINE
            0, // COL
            stacktrace, // STACK_TRACE
            UnsignedLong.valueOf("17355148826090327171"), // STACK_TRACE_ID
            stacktrace, // ORIGINAL_STACK_TRACE
            Arrays.asList("streamSettings", "execView", "streamSettings", "execView", "streamSettings", "execView",
                "streamSettings", "execView", "(anonymous)"), // STACK_TRACE_NAMES
            Arrays.asList("common/pages/views/commonViews.view.js", "js_libs/core.js", "common/pages/views" +
                "/commonTouchViews.view.js", "js_libs/core.js", "stream/pages/views/streamTouchViews.view.js",
                "js_libs/core.js", "js_touch_exp/pages-touch/views/touchStreamViews.view.js", "js_libs/core.js",
                "stream/pages/views/commonStreamTouchViews.view.js"), // STACK_TRACE_URLS
            Arrays.asList(15573, 1405, 2711, 1405, 124, 1405, 27, 1405, 182), // STACK_TRACE_LINES
            Arrays.asList(21, 47, 20, 47, 20, 47, 20, 47, 20), // STACK_TRACE_COLS
            "", // USER_AGENT
            hashOfEmptyString, // USER_AGENT_ID
            "", // SOURCE
            "", // SOURCE_METHOD
            "", // SOURCE_TYPE
            UnsignedLong.valueOf(0), // CLIENT_TIMESTAMP
            UnsignedLong.valueOf(0), // CLIENT_INIT_TIMESTAMP
            Arrays.asList(), // REPLACED_URLS_KEYS
            Arrays.asList(), // REPLACED_URLS_VALUES
            Parser.MORDA_NODE, // PARSER
            UnsignedLong.valueOf("0"), // ICOOKIE
            false, // SILENT
            "vla", // DC
            0d, // COORDINATES_LATITUDE
            0d, // COORDINATES_LONGITUDE
            0, // COORDINATES_PRECISION
            0L, // COORDINATES_TIMESTAMP
            checker.getHost(), // HOST
            checker.getLogBrokerTopic() // TOPIC
        );
    }

    @Test
    public void warnParser() throws Exception {
        String line = "tskv\ttskv_format=error-log-morda-node\tctype=unstable\tprj=portal-morda\tunixtime=1561636048" +
            "\ttimestamp=2019-06-27 14:47:28\ttimezone=+0300\tpid=1580\tlevel=warn\treqid=1561636048.01006.154967" +
            ".2779\tmessage=[teaser][v14] No img in teaser, will not show {\"title1\":\"Управляйте проектами\"," +
            "\"textauthor\":\"\",\"text1\":\"в Яндекс.Трекере\",\"raw_url\":\"https://yandex" +
            ".ru/tracker/?utm_source\\=yandex&utm_medium\\=morda&utm_campaign\\=generaltracker292685&utm_content" +
            "\\=russia&utm_term\\=gen1.4\",\"alt\":\"Яндекс.Трекер мок\",\"product\":\"tracker\",\"text2\":\"\"," +
            "\"bannerid\":\"7033684928:3589664906075987525\"," +
            "\"linknext" +
            "\":\"\\=m2H6j0S1G0H846344d2m_vQ73e012P01W8_iipIu0R3_beSEs06AWiGPw06e0f03eAs0Ih030hW4_m7e1BC9Y0MYm5oG1QB0" +
            "NC05aQ0ku0K5c0Q2qApp3gW6gWFG1nBn1m0000000000-0S1W0W4q0Y0Y821m920W801-0ZJlyk31u0A0S4A00000000043O2WBP2qLU" +
            "M9Ks3T4nw0kYm5pe31-93W0000000F0_a0x0X3sW3i24FQ4F00000000y3_P3m0000000F0_W125WU8CcX094G0000000F0_o130eX2X" +
            "4G0000000F0_g170X3s84W6G4W7W4e606EaI61JKRdL3m2m0\",\"age_restriction\":\"\",\"title2\":\"любого масштаба" +
            "\",\"url\":\"https://yabs.yandex.ru/count/G-j_KAUagwq50FG1488WCIoi55q00000EDgD5404I11Wn19miF-MXmw00GcG0O" +
            "2FxBCqc060Z-opDBW1iF-MXmx00GBO0Og2n1de0QW2y0APkhVO0P03eAs0Ih031BW4_m7e1BC9-0IYm5o81QB0N905ei1Sm0MHe2xW1G" +
            "Nm1G6O1eBGhFCEe0Qg0wW6gWF91bs6TlHf5yUAqGPThk7YQXV7Yga7HLvObJODqJ6m1u20c3pG1nBu1m6020JG2828W870a820W07u2D" +
            "E_ouC7W0e1mGe000000000GDWA0geB476kg5T5NW007oGmi4oC1G302u2Z1SWBWDIJ0TaBHLvObJODqJ7e2wB0NF0B2-WC7-0D0e8E94" +
            "GsDp4uGaKpBK4mCaGjD4D1EIquD3OmBJSsDZ0qC4GrHKOmHeaE00000000y3-G3i24FPWEtOpQev_jnUEA0Q0Em8Gzi0u1eGy0000000" +
            "3mFzaF00000000y3_W3m6048M1uWoG49UwuQ7dYjI3nm6Q40aH00000000y3_84C2Y4FWG4O0H4OWH0v0H0w4H00000000y3-e4S24FV" +
            "eH6Gq000005G00000T000002K00000BG0000284W6G4W7W4e606EaI61JKRdL3m2pm4X7u4W604pA84mEG4p80\"}";

        String message = "[teaser][v14] No img in teaser, will not show {\"title1\":\"Управляйте проектами\"," +
            "\"textauthor\":\"\",\"text1\":\"в Яндекс.Трекере\",\"raw_url\":\"https://yandex" +
            ".ru/tracker/?utm_source\\=yandex&utm_medium\\=morda&utm_campaign\\=generaltracker292685&utm_content" +
            "\\=russia&utm_term\\=gen1.4\",\"alt\":\"Яндекс.Трекер мок\",\"product\":\"tracker\",\"text2\":\"\"," +
            "\"bannerid\":\"7033684928:3589664906075987525\"," +
            "\"linknext" +
            "\":\"\\=m2H6j0S1G0H846344d2m_vQ73e012P01W8_iipIu0R3_beSEs06AWiGPw06e0f03eAs0Ih030hW4_m7e1BC9Y0MYm5oG1QB0" +
            "NC05aQ0ku0K5c0Q2qApp3gW6gWFG1nBn1m0000000000-0S1W0W4q0Y0Y821m920W801-0ZJlyk31u0A0S4A00000000043O2WBP2qLU" +
            "M9Ks3T4nw0kYm5pe31-93W0000000F0_a0x0X3sW3i24FQ4F00000000y3_P3m0000000F0_W125WU8CcX094G0000000F0_o130eX2X" +
            "4G0000000F0_g170X3s84W6G4W7W4e606EaI61JKRdL3m2m0\",\"age_restriction\":\"\",\"title2\":\"любого масштаба" +
            "\",\"url\":\"https://yabs.yandex.ru/count/G-j_KAUagwq50FG1488WCIoi55q00000EDgD5404I11Wn19miF-MXmw00GcG0O" +
            "2FxBCqc060Z-opDBW1iF-MXmx00GBO0Og2n1de0QW2y0APkhVO0P03eAs0Ih031BW4_m7e1BC9-0IYm5o81QB0N905ei1Sm0MHe2xW1G" +
            "Nm1G6O1eBGhFCEe0Qg0wW6gWF91bs6TlHf5yUAqGPThk7YQXV7Yga7HLvObJODqJ6m1u20c3pG1nBu1m6020JG2828W870a820W07u2D" +
            "E_ouC7W0e1mGe000000000GDWA0geB476kg5T5NW007oGmi4oC1G302u2Z1SWBWDIJ0TaBHLvObJODqJ7e2wB0NF0B2-WC7-0D0e8E94" +
            "GsDp4uGaKpBK4mCaGjD4D1EIquD3OmBJSsDZ0qC4GrHKOmHeaE00000000y3-G3i24FPWEtOpQev_jnUEA0Q0Em8Gzi0u1eGy0000000" +
            "3mFzaF00000000y3_W3m6048M1uWoG49UwuQ7dYjI3nm6Q40aH00000000y3_84C2Y4FWG4O0H4OWH0v0H0w4H00000000y3-e4S24FV" +
            "eH6Gq000005G00000T000002K00000BG0000284W6G4W7W4e606EaI61JKRdL3m2pm4X7u4W604pA84mEG4p80\"}";

        UnsignedLong hashOfEmptyString = UnsignedLong.valueOf("2202906307356721367");

        checker.check(
            line,
            new Date(1561636048000L),
            "morda", // PROJECT
            "", // SERVICE
            "", // PAGE
            Platform.UNKNOWN, // PLATFORM
            "", // URL
            hashOfEmptyString, // URL_ID
            "", // VHOST
            Environment.PRE_PRODUCTION, // ENVIRONMENT
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
            "1561636048.01006.154967.2779", // REQUEST_ID
            UnsignedLong.valueOf("3376810537559371806"), // REQUEST_ID_HASH
            UnsignedLong.valueOf(0), // YANDEXUID
            Arrays.asList("prj", "ctype"), // KV_KEYS
            Arrays.asList("portal-morda", "unstable"), // KV_VALUES
            false, // IS_INTERNAL
            "", // CDN
            "", // IPv6
            false, // LOGGED_IN
            message, // MESSAGE
            UnsignedLong.valueOf("14914530954471144596"), // MESSAGE_ID
            message, // ORIGINAL_MESSAGE
            Arrays.asList(), // ERROR_URLS_KEYS
            Arrays.asList(), // ERROR_URLS_VALUES
            Runtime.NODEJS, // RUNTIME
            LogLevel.WARNING, // LEVEL
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
            UnsignedLong.valueOf(0L), // CLIENT_TIMESTAMP
            UnsignedLong.valueOf(0), // CLIENT_INIT_TIMESTAMP
            Arrays.asList(), // REPLACED_URLS_KEYS
            Arrays.asList(), // REPLACED_URLS_VALUES
            Parser.MORDA_NODE, // PARSER
            UnsignedLong.valueOf("0"), // ICOOKIE
            false, // SILENT
            "", // DC
            0d, // COORDINATES_LATITUDE
            0d, // COORDINATES_LONGITUDE
            0, // COORDINATES_PRECISION
            0L, // COORDINATES_TIMESTAMP
            checker.getHost(), // HOST
            checker.getLogBrokerTopic() // TOPIC
        );
    }

    @Test
    public void fatalParser() throws Exception {
        String line = "tskv\ttskv_format=error-log-morda-node\tctype=prestable\tprj=portal-morda\tunixtime=1569338958" +
            "\ttimestamp=2019-09-24 18:29:18\ttimezone=+0300\tpid=3409\tlevel=fatal\tmessage=uncaughtException Error:" +
            " ENOSPC: no space left on device, write";

        String message = "uncaughtException Error: ENOSPC: no space left on device, write";

        UnsignedLong hashOfEmptyString = UnsignedLong.valueOf("2202906307356721367");

        checker.check(
            line,
            new Date(1569338958000L),
            "morda", // PROJECT
            "", // SERVICE
            "", // PAGE
            Platform.UNKNOWN, // PLATFORM
            "", // URL
            hashOfEmptyString, // URL_ID
            "", // VHOST
            Environment.PRODUCTION, // ENVIRONMENT
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
            UnsignedLong.valueOf("2202906307356721367"), // REQUEST_ID_HASH
            UnsignedLong.valueOf(0), // YANDEXUID
            Arrays.asList("prj", "ctype"), // KV_KEYS
            Arrays.asList("portal-morda", "prestable"), // KV_VALUES
            false, // IS_INTERNAL
            "", // CDN
            "", // IPv6
            false, // LOGGED_IN
            message, // MESSAGE
            UnsignedLong.valueOf("12572255164577185294"), // MESSAGE_ID
            message, // ORIGINAL_MESSAGE
            Arrays.asList(), // ERROR_URLS_KEYS
            Arrays.asList(), // ERROR_URLS_VALUES
            Runtime.NODEJS, // RUNTIME
            LogLevel.CRITICAL, // LEVEL
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
            Parser.MORDA_NODE, // PARSER
            UnsignedLong.valueOf("0"), // ICOOKIE
            false, // SILENT
            "", // DC
            0d, // COORDINATES_LATITUDE
            0d, // COORDINATES_LONGITUDE
            0, // COORDINATES_PRECISION
            0L, // COORDINATES_TIMESTAMP
            checker.getHost(), // HOST
            checker.getLogBrokerTopic() // TOPIC
        );
    }

    @Test
    public void newMordaFormatParser() throws Exception {
        String line = "tskv\ttskv_format=error-log-morda-node\tctype=unstable\tprj=portal-morda\tunixtime=1566291184" +
            "\ttimestamp=2019-08-20 11:53:04\ttimezone=+0300\tpid=1553\tlevel=warn\treqid=1566291183.66999.154967" +
            ".2333\tuid=7084056791560544764\tpage=big1\tsource=source\tsourceMethod=sourceMethod\tsourceType=type" +
            "\turl=https://rtc.yandex.ru/?usemock\\=teaser_empty\thandler=big" +
            ".v14w\thost=sas2-8836-f11-sas-portal-morda-rc-29310.gencfg-c.yandex.net\tversion=2" +
            ".2204\tip=2a02:6b8:b080:6506::1:10\tregion=213\tisInternal=1\tproject=morda\tblock=teaser\tmethod=teaser" +
            "\tadditional={\"teaser\":{\"raw_url\":\"https://plus.yandex" +
            ".ru/card?utm_source\\=yandexportal&utm_campaign\\=Drive10&utm_medium\\=teaser&utm_content\\=violet_girl" +
            "&utm_term\\=aug\",\"product\":\"plus\",\"url\":\"https://yabs.yandex" +
            ".ru/count/QR1pLcXg" +
            "-Zq50F41488WCPoXMrq00000EDgD5404I11Wn19mX8cNXmw00GcG0So4hfWrc07CXAwODRW1X8cNXmx00GBO0OQ5onde0QW2y0Aydzgi" +
            "3P03yCE0Rx031BW4_m7e1FK8-0I0YNg81O29Uf05W8bwm0MFb5NW1GNm1G6O1eBGhFCEe0Qg0wW6gWF91Z5XQVBrGkXBqGRPkohQ-fyU" +
            "6Ta60000KCu0002f1mT_-21I1xn2i0U0W9WCq0SIW0W2q0Y0Y821mP20W801-0Y7mCk31u0A0S4A00000000043O2WAg2n0QlQ5Q1ty0" +
            "0EW6jsw7a0K0m0k0emN82u3Kam7P2mT_-21I1xn2w0k0YNhm2mRe31_W3GA93W0000000F0_a0x0X3sO3h72fPFVtf3qIw0Em8Gzi0u1" +
            "eGy00000003mFzaF00000000y3_W3m604CF2vGoG4ExhhUdqs-_jy06Q40aH00000000y3_84C2Y4FWG5u0H5uWH0P0H0Q4H00000000" +
            "y3-e4S24FVeH6Gq000005G00000T000002K00000BG0000284W6G4W7W4e606EaInFkx_8MoCqVm4XU84mAO4mYe4uwHlBxUXlIz5S0J" +
            "____________0TeJ2WW0400O0200A000\",\"alt\":\"Яндекс.Плюс\",\"textauthor\":\"\",\"text1\":\"До 31 октября" +
            " — 10%\",\"bannerid\":\"7139082534:4808726551446716167\",\"title1\":\"Кэшбэк на Драйв\",\"text2\":\"\"," +
            "\"linknext\":\"\\=VgEtcny1G0H846344d24YPU73e012P01p8Ikc3MO0So4hfWrk064YPU73jW1XeNB6UW1g0AG0_33W6-m0mAu1F" +
            "y1w0Jr28W5W8bwa0M0YNh01O-KLU051PW6Wj2iymwe1ge3i0U0W9WCq0SIyGS00000000008080j08W8Y0WS6GW8200VW8Xy3BWmU02W" +
            "712W0000000010s0e2sGi7V_WWKWUyGkWBW8bww0mVYGu00000003mFv0Em8Gze0x0X3sX3m0000000F0_sGy00000003mFu0GmyBb39" +
            "eG2H400000003mFyWGmA8GeH400000003mFwWHm8GzY181a181u1A1W1Zf4iJxk_o5iZD7c1C8g1EEaRo-teRqlHK0\",\"age_restr" +
            "iction\":\"0+\",\"title2\":\"по карте Яндекс.Плюс\",\"img_retina\":{\"url\":\"https://avatars.mds.yandex" +
            ".net/get-banana/26007/x25B4ls62kw7VgqqQ_ld4Le01_banana_20161021_violet_girl_hd.png/optimize\",\"width\":" +
            "\"240\",\"height\":\"180\"}}}\tmessage=[teaser][v14] No img in teaser, will not show\tslots=204304,0,22;" +
            "221201,0,46;328285,0,54;335711,0,80;336804,0,94\tenvironment=development\tuseragent=Mozilla/5.0 (X11; Li" +
            "nux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4421.0 Safari/537.36 Edg/90.0.810.1";

        String message = "[teaser][v14] No img in teaser, will not show";

        UnsignedLong hashOfEmptyString = UnsignedLong.valueOf("2202906307356721367");

        checker.check(
            line,
            new Date(1566291184000L),
            "morda", // PROJECT
            "", // SERVICE
            "big1", // PAGE
            Platform.UNKNOWN, // PLATFORM
            "https://rtc.yandex.ru/?usemock=teaser_empty", // URL
            UnsignedLong.valueOf("12523669605988166923"), // URL_ID
            "rtc.yandex.ru", // VHOST
            Environment.DEVELOPMENT, // ENVIRONMENT
            Arrays.asList(204304, 221201, 328285, 335711, 336804), // TEST_IDS
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
            "2.2204", // VERSION
            213, // REGION
            "1566291183.66999.154967.2333", // REQUEST_ID
            UnsignedLong.valueOf("15773465697935147505"), // REQUEST_ID_HASH
            UnsignedLong.valueOf("7084056791560544764"), // YANDEXUID
            Arrays.asList("prj", "handler", "ctype", "teaser"), // KV_KEYS
            Arrays.asList("portal-morda", "big.v14w", "unstable", "{\"raw_url\":\"https://plus.yandex" +
                ".ru/card?utm_source=yandexportal&utm_campaign=Drive10&utm_medium=teaser&utm_content=violet_girl" +
                "&utm_term=aug\",\"product\":\"plus\",\"url\":\"https://yabs.yandex" +
                ".ru/count/QR1pLcXg" +
                "-Zq50F41488WCPoXMrq00000EDgD5404I11Wn19mX8cNXmw00GcG0So4hfWrc07CXAwODRW1X8cNXmx00GBO0OQ5onde0QW2y0Ay" +
                "dzgi3P03yCE0Rx031BW4_m7e1FK8-0I0YNg81O29Uf05W8bwm0MFb5NW1GNm1G6O1eBGhFCEe0Qg0wW6gWF91Z5XQVBrGkXBqGRP" +
                "kohQ-fyU6Ta60000KCu0002f1mT_-21I1xn2i0U0W9WCq0SIW0W2q0Y0Y821mP20W801-0Y7mCk31u0A0S4A00000000043O2WAg" +
                "2n0QlQ5Q1ty00EW6jsw7a0K0m0k0emN82u3Kam7P2mT_-21I1xn2w0k0YNhm2mRe31_W3GA93W0000000F0_a0x0X3sO3h72fPFV" +
                "tf3qIw0Em8Gzi0u1eGy00000003mFzaF00000000y3_W3m604CF2vGoG4ExhhUdqs-_jy06Q40aH00000000y3_84C2Y4FWG5u0H" +
                "5uWH0P0H0Q4H00000000y3-e4S24FVeH6Gq000005G00000T000002K00000BG0000284W6G4W7W4e606EaInFkx_8MoCqVm4XU8" +
                "4mAO4mYe4uwHlBxUXlIz5S0J____________0TeJ2WW0400O0200A000\",\"alt\":\"Яндекс.Плюс\",\"textauthor\":\"" +
                "\",\"text1\":\"До 31 октября — 10%\",\"bannerid\":\"7139082534:4808726551446716167\",\"title1\":\"Кэ" +
                "шбэк на Драйв\",\"text2\":\"\",\"linknext\":\"=VgEtcny1G0H846344d24YPU73e012P01p8Ikc3MO0So4hfWrk064Y" +
                "PU73jW1XeNB6UW1g0AG0_33W6-m0mAu1Fy1w0Jr28W5W8bwa0M0YNh01O-KLU051PW6Wj2iymwe1ge3i0U0W9WCq0SIyGS000000" +
                "00008080j08W8Y0WS6GW8200VW8Xy3BWmU02W712W0000000010s0e2sGi7V_WWKWUyGkWBW8bww0mVYGu00000003mFv0Em8Gze" +
                "0x0X3sX3m0000000F0_sGy00000003mFu0GmyBb39eG2H400000003mFyWGmA8GeH400000003mFwWHm8GzY181a181u1A1W1Zf4" +
                "iJxk_o5iZD7c1C8g1EEaRo-teRqlHK0\",\"age_restriction\":\"0+\",\"title2\":\"по карте Яндекс.Плюс\",\"i" +
                "mg_retina\":{\"url\":\"https://avatars.mds.yandex.net/get-banana/26007/x25B4ls62kw7VgqqQ_ld4Le01_ban" +
                "ana_20161021_violet_girl_hd.png/optimize\",\"width\":\"240\",\"height\":\"180\"}}"), // KV_VALUES
            true, // IS_INTERNAL
            "", // CDN
            "2a02:6b8:b080:6506::1:10", // IPv6
            false, // LOGGED_IN
            message, // MESSAGE
            UnsignedLong.valueOf("13840108834744856996"), // MESSAGE_ID
            message, // ORIGINAL_MESSAGE
            Arrays.asList(), // ERROR_URLS_KEYS
            Arrays.asList(), // ERROR_URLS_VALUES
            Runtime.NODEJS, // RUNTIME
            LogLevel.WARNING, // LEVEL
            "", // FILE
            hashOfEmptyString, // FILE_ID
            "teaser", // BLOCK
            "teaser", // METHOD
            0, // LINE
            0, // COL
            "", // STACK_TRACE
            UnsignedLong.valueOf("2202906307356721367"), // STACK_TRACE_ID
            "", // ORIGINAL_STACK_TRACE
            Arrays.asList(), // STACK_TRACE_NAMES
            Arrays.asList(), // STACK_TRACE_URLS
            Arrays.asList(), // STACK_TRACE_LINES
            Arrays.asList(), // STACK_TRACE_COLS
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4421.0 Safari/537.36 " +
                "Edg/90.0.810.1", // USER_AGENT
            UnsignedLong.valueOf("6274190839110891026"), // USER_AGENT_ID
            "source", // SOURCE
            "sourceMethod", // SOURCE_METHOD
            "type", // SOURCE_TYPE
            UnsignedLong.valueOf(0), // CLIENT_TIMESTAMP
            UnsignedLong.valueOf(0), // CLIENT_INIT_TIMESTAMP
            Arrays.asList(), // REPLACED_URLS_KEYS
            Arrays.asList(), // REPLACED_URLS_VALUES
            Parser.MORDA_NODE, // PARSER
            UnsignedLong.valueOf("0"), // ICOOKIE
            false, // SILENT
            "", // DC
            0d, // COORDINATES_LATITUDE
            0d, // COORDINATES_LONGITUDE
            0, // COORDINATES_PRECISION
            0L, // COORDINATES_TIMESTAMP
            "sas2-8836-f11-sas-portal-morda-rc-29310.gencfg-c.yandex.net", // HOST
            checker.getLogBrokerTopic() // TOPIC
        );
    }

    @Test
    public void newEtherFormatParser() throws Exception {
        String line = "tskv\ttskv_format=error-log-morda-node\tctype=unstable\tprj=portal-morda\tunixtime=1566299753" +
            "\ttimestamp=2019-08-20 14:15:53\ttimezone=+0300\tpid=1592\tlevel=error\treqid=1566299752.84891.154967" +
            ".2331\tuid=7084056791560544764\turl=https://rtc.yandex.ru/efir\thandler=embedstream" +
            ".*\thost=sas2-8836-f11-sas-portal-morda-rc-29310.gencfg-c.yandex.net\tversion=2" +
            ".2204\tip=2a02:6b8:b080:6510::1:2\tregion=213\tisInternal=1\tproject=ether\tstack=Object.home.error " +
            "(js_libs/core.js:183:17) -> execView (js_libs/core.js:1453:38) -> body__layout " +
            "(stream/pages/views/commonStreamDesktopViews.view.js:38:121) -> execView (js_libs/core.js:1442:47) -> " +
            "bodyLayout (stream/pages/views/streamDesktopViews.view.js:5:12) -> execView (js_libs/core.js:1442:47) ->" +
            " body__content (stream/pages/views/commonStreamDesktopViews.view.js:33:122) -> execView (js_libs/core" +
            ".js:1442:47) -> body (common/pages/views/commonViews.view.js:2238:88)" +
            "\tblock=stream\tmethod=stream__data\tmessage=Error on view -> streamData.channels.forEach is not a " +
            "function";

        String message = "Error on view -> streamData.channels.forEach is not a function";
        String stacktrace = "Object.home.error (js_libs/core.js:183:17) -> execView (js_libs/core.js:1453:38) -> " +
            "body__layout (stream/pages/views/commonStreamDesktopViews.view.js:38:121) -> execView (js_libs/core" +
            ".js:1442:47) -> bodyLayout (stream/pages/views/streamDesktopViews.view.js:5:12) -> execView " +
            "(js_libs/core.js:1442:47) -> body__content (stream/pages/views/commonStreamDesktopViews.view.js:33:122) " +
            "-> execView (js_libs/core.js:1442:47) -> body (common/pages/views/commonViews.view.js:2238:88)";

        UnsignedLong hashOfEmptyString = UnsignedLong.valueOf("2202906307356721367");

        checker.check(
            line,
            new Date(1566299753000L),
            "ether", // PROJECT
            "", // SERVICE
            "", // PAGE
            Platform.UNKNOWN, // PLATFORM
            "https://rtc.yandex.ru/efir", // URL
            UnsignedLong.valueOf("6238727100190660877"), // URL_ID
            "rtc.yandex.ru", // VHOST
            Environment.PRE_PRODUCTION, // ENVIRONMENT
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
            "2.2204", // VERSION
            213, // REGION
            "1566299752.84891.154967.2331", // REQUEST_ID
            UnsignedLong.valueOf("3099189446055999649"), // REQUEST_ID_HASH
            UnsignedLong.valueOf("7084056791560544764"), // YANDEXUID
            Arrays.asList("prj", "handler", "ctype"), // KV_KEYS
            Arrays.asList("portal-morda", "embedstream.*", "unstable"), // KV_VALUES
            true, // IS_INTERNAL
            "", // CDN
            "2a02:6b8:b080:6510::1:2", // IPv6
            false, // LOGGED_IN
            message, // MESSAGE
            UnsignedLong.valueOf("15789359098214103254"), // MESSAGE_ID
            message, // ORIGINAL_MESSAGE
            Arrays.asList(), // ERROR_URLS_KEYS
            Arrays.asList(), // ERROR_URLS_VALUES
            Runtime.NODEJS, // RUNTIME
            LogLevel.ERROR, // LEVEL
            "", // FILE
            hashOfEmptyString, // FILE_ID
            "stream", // BLOCK
            "stream__data", // METHOD
            0, // LINE
            0, // COL
            stacktrace, // STACK_TRACE
            UnsignedLong.valueOf("17799356997130527874"), // STACK_TRACE_ID
            stacktrace, // ORIGINAL_STACK_TRACE
            Arrays.asList("Object.home.error", "execView", "body__layout", "execView", "bodyLayout", "execView",
                "body__content", "execView", "body"), // STACK_TRACE_NAMES
            Arrays.asList("js_libs/core.js", "js_libs/core.js",
                "stream/pages/views/commonStreamDesktopViews.view.js",
                "js_libs/core.js", "stream/pages/views/streamDesktopViews.view.js", "js_libs/core.js", "stream" +
                    "/pages/views/commonStreamDesktopViews.view.js", "js_libs/core.js", "common/pages/views" +
                    "/commonViews.view.js"), // STACK_TRACE_URLS
            Arrays.asList(183, 1453, 38, 1442, 5, 1442, 33, 1442, 2238), // STACK_TRACE_LINES
            Arrays.asList(17, 38, 121, 47, 12, 47, 122, 47, 88), // STACK_TRACE_COLS
            "", // USER_AGENT
            hashOfEmptyString, // USER_AGENT_ID
            "", // SOURCE
            "", // SOURCE_METHOD
            "", // SOURCE_TYPE
            UnsignedLong.valueOf(0), // CLIENT_TIMESTAMP
            UnsignedLong.valueOf(0), // CLIENT_INIT_TIMESTAMP
            Arrays.asList(), // REPLACED_URLS_KEYS
            Arrays.asList(), // REPLACED_URLS_VALUES
            Parser.MORDA_NODE, // PARSER
            UnsignedLong.valueOf("0"), // ICOOKIE
            false, // SILENT
            "", // DC
            0d, // COORDINATES_LATITUDE
            0d, // COORDINATES_LONGITUDE
            0, // COORDINATES_PRECISION
            0L, // COORDINATES_TIMESTAMP
            "sas2-8836-f11-sas-portal-morda-rc-29310.gencfg-c.yandex.net", // HOST
            checker.getLogBrokerTopic() // TOPIC
        );
    }

    @Test
    public void skipWithLevelDebug() throws Exception {
        String line = "tskv\ttskv_format=error-log-morda-node\tctype=assessor\tproject=portal-morda-assessors-rc" +
            "\tunixtime=1561574840\ttimestamp=2019-06-26 " +
            "21:47:20\ttimezone=+0300\tpid=1705\tlevel=info\tmessage=worker 1705 started";

        checker.checkEmpty(line);
    }
}
