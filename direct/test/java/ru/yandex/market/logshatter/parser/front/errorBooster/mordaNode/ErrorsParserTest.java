package ru.yandex.market.logshatter.parser.front.errorBooster.mordaNode;


import com.google.common.primitives.UnsignedLong;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.logshatter.parser.LogParserChecker;
import ru.yandex.market.logshatter.parser.front.errorBooster.*;
import ru.yandex.market.logshatter.parser.front.errorBooster.Runtime;

import java.util.Arrays;
import java.util.Date;

public class ErrorsParserTest {
    private LogParserChecker checker;

    @Before
    public void setUp() {
        checker = new LogParserChecker(new ErrorsParser());
    }

    @Test
    public void errorParser() throws Exception {
        String line = "tskv\ttskv_format=error-log-morda-node\tctype=unstable\tproject=portal-morda\tunixtime=1559232325\ttimestamp=2019-05-30 19:05:25\ttimezone=+0300\tpid=1652\tlevel=error\treqid=1559232325.5826.156486.3331\tstack=streamSettings (common/pages/views/commonViews.view.js:15573:21) -> execView (js_libs/core.js:1405:47) -> streamSettings (common/pages/views/commonTouchViews.view.js:2711:20) -> execView (js_libs/core.js:1405:47) -> streamSettings (stream/pages/views/streamTouchViews.view.js:124:20) -> execView (js_libs/core.js:1405:47) -> streamSettings (js_touch_exp/pages-touch/views/touchStreamViews.view.js:27:20) -> execView (js_libs/core.js:1405:47) -> stream/pages/views/commonStreamTouchViews.view.js:182:20\tmessage=[stream-settings] Failed to parse resourceOverride, Unexpected token , in JSON at position 431";

        String message = "[stream-settings] Failed to parse resourceOverride, Unexpected token , in JSON at position 431";
        String stacktrace = "streamSettings (common/pages/views/commonViews.view.js:15573:21) -> execView (js_libs/core.js:1405:47) -> streamSettings (common/pages/views/commonTouchViews.view.js:2711:20) -> execView (js_libs/core.js:1405:47) -> streamSettings (stream/pages/views/streamTouchViews.view.js:124:20) -> execView (js_libs/core.js:1405:47) -> streamSettings (js_touch_exp/pages-touch/views/touchStreamViews.view.js:27:20) -> execView (js_libs/core.js:1405:47) -> stream/pages/views/commonStreamTouchViews.view.js:182:20";

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
            Arrays.asList("project", "ctype"), // KV_KEYS
            Arrays.asList("portal-morda", "unstable"), // KV_VALUES
            false, // IS_INTERNAL
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
            UnsignedLong.valueOf("8065024488036245163"), // STACK_TRACE_ID
            "", // ORIGINAL_STACK_TRACE
            "", // USER_AGENT
            hashOfEmptyString, // USER_AGENT_ID
            "", // SOURCE
            "", // SOURCE_METHOD
            "", // SOURCE_TYPE
            0, // CLIENT_TIMESTAMP
            Arrays.asList(), // REPLACED_URLS_KEYS
            Arrays.asList(), // REPLACED_URLS_VALUES
            0L, // IP
            Parser.MORDA_NODE // PARSER
        );
    }

    @Test
    public void warnParser() throws Exception {
        String line = "tskv\ttskv_format=error-log-morda-node\tctype=unstable\tproject=portal-morda\tunixtime=1561636048\ttimestamp=2019-06-27 14:47:28\ttimezone=+0300\tpid=1580\tlevel=warn\treqid=1561636048.01006.154967.2779\tmessage=[teaser][v14] No img in teaser, will not show {\"title1\":\"Управляйте проектами\",\"textauthor\":\"\",\"text1\":\"в Яндекс.Трекере\",\"raw_url\":\"https://yandex.ru/tracker/?utm_source\\=yandex&utm_medium\\=morda&utm_campaign\\=generaltracker292685&utm_content\\=russia&utm_term\\=gen1.4\",\"alt\":\"Яндекс.Трекер мок\",\"product\":\"tracker\",\"text2\":\"\",\"bannerid\":\"7033684928:3589664906075987525\",\"linknext\":\"\\=m2H6j0S1G0H846344d2m_vQ73e012P01W8_iipIu0R3_beSEs06AWiGPw06e0f03eAs0Ih030hW4_m7e1BC9Y0MYm5oG1QB0NC05aQ0ku0K5c0Q2qApp3gW6gWFG1nBn1m0000000000-0S1W0W4q0Y0Y821m920W801-0ZJlyk31u0A0S4A00000000043O2WBP2qLUM9Ks3T4nw0kYm5pe31-93W0000000F0_a0x0X3sW3i24FQ4F00000000y3_P3m0000000F0_W125WU8CcX094G0000000F0_o130eX2X4G0000000F0_g170X3s84W6G4W7W4e606EaI61JKRdL3m2m0\",\"age_restriction\":\"\",\"title2\":\"любого масштаба\",\"url\":\"https://yabs.yandex.ru/count/G-j_KAUagwq50FG1488WCIoi55q00000EDgD5404I11Wn19miF-MXmw00GcG0O2FxBCqc060Z-opDBW1iF-MXmx00GBO0Og2n1de0QW2y0APkhVO0P03eAs0Ih031BW4_m7e1BC9-0IYm5o81QB0N905ei1Sm0MHe2xW1GNm1G6O1eBGhFCEe0Qg0wW6gWF91bs6TlHf5yUAqGPThk7YQXV7Yga7HLvObJODqJ6m1u20c3pG1nBu1m6020JG2828W870a820W07u2DE_ouC7W0e1mGe000000000GDWA0geB476kg5T5NW007oGmi4oC1G302u2Z1SWBWDIJ0TaBHLvObJODqJ7e2wB0NF0B2-WC7-0D0e8E94GsDp4uGaKpBK4mCaGjD4D1EIquD3OmBJSsDZ0qC4GrHKOmHeaE00000000y3-G3i24FPWEtOpQev_jnUEA0Q0Em8Gzi0u1eGy00000003mFzaF00000000y3_W3m6048M1uWoG49UwuQ7dYjI3nm6Q40aH00000000y3_84C2Y4FWG4O0H4OWH0v0H0w4H00000000y3-e4S24FVeH6Gq000005G00000T000002K00000BG0000284W6G4W7W4e606EaI61JKRdL3m2pm4X7u4W604pA84mEG4p80\"}";

        String message = "[teaser][v14] No img in teaser, will not show {\"title1\":\"Управляйте проектами\",\"textauthor\":\"\",\"text1\":\"в Яндекс.Трекере\",\"raw_url\":\"https://yandex.ru/tracker/?utm_source\\=yandex&utm_medium\\=morda&utm_campaign\\=generaltracker292685&utm_content\\=russia&utm_term\\=gen1.4\",\"alt\":\"Яндекс.Трекер мок\",\"product\":\"tracker\",\"text2\":\"\",\"bannerid\":\"7033684928:3589664906075987525\",\"linknext\":\"\\=m2H6j0S1G0H846344d2m_vQ73e012P01W8_iipIu0R3_beSEs06AWiGPw06e0f03eAs0Ih030hW4_m7e1BC9Y0MYm5oG1QB0NC05aQ0ku0K5c0Q2qApp3gW6gWFG1nBn1m0000000000-0S1W0W4q0Y0Y821m920W801-0ZJlyk31u0A0S4A00000000043O2WBP2qLUM9Ks3T4nw0kYm5pe31-93W0000000F0_a0x0X3sW3i24FQ4F00000000y3_P3m0000000F0_W125WU8CcX094G0000000F0_o130eX2X4G0000000F0_g170X3s84W6G4W7W4e606EaI61JKRdL3m2m0\",\"age_restriction\":\"\",\"title2\":\"любого масштаба\",\"url\":\"https://yabs.yandex.ru/count/G-j_KAUagwq50FG1488WCIoi55q00000EDgD5404I11Wn19miF-MXmw00GcG0O2FxBCqc060Z-opDBW1iF-MXmx00GBO0Og2n1de0QW2y0APkhVO0P03eAs0Ih031BW4_m7e1BC9-0IYm5o81QB0N905ei1Sm0MHe2xW1GNm1G6O1eBGhFCEe0Qg0wW6gWF91bs6TlHf5yUAqGPThk7YQXV7Yga7HLvObJODqJ6m1u20c3pG1nBu1m6020JG2828W870a820W07u2DE_ouC7W0e1mGe000000000GDWA0geB476kg5T5NW007oGmi4oC1G302u2Z1SWBWDIJ0TaBHLvObJODqJ7e2wB0NF0B2-WC7-0D0e8E94GsDp4uGaKpBK4mCaGjD4D1EIquD3OmBJSsDZ0qC4GrHKOmHeaE00000000y3-G3i24FPWEtOpQev_jnUEA0Q0Em8Gzi0u1eGy00000003mFzaF00000000y3_W3m6048M1uWoG49UwuQ7dYjI3nm6Q40aH00000000y3_84C2Y4FWG4O0H4OWH0v0H0w4H00000000y3-e4S24FVeH6Gq000005G00000T000002K00000BG0000284W6G4W7W4e606EaI61JKRdL3m2pm4X7u4W604pA84mEG4p80\"}";

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
            Arrays.asList("project", "ctype"), // KV_KEYS
            Arrays.asList("portal-morda", "unstable"), // KV_VALUES
            false, // IS_INTERNAL
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
            "", // USER_AGENT
            hashOfEmptyString, // USER_AGENT_ID
            "", // SOURCE
            "", // SOURCE_METHOD
            "", // SOURCE_TYPE
            0, // CLIENT_TIMESTAMP
            Arrays.asList(), // REPLACED_URLS_KEYS
            Arrays.asList(), // REPLACED_URLS_VALUES
            0L, // IP
            Parser.MORDA_NODE // PARSER
        );
    }

    @Test
    public void skipWithLevelDebug() throws Exception {
        String line = "tskv\ttskv_format=error-log-morda-node\tctype=assessor\tproject=portal-morda-assessors-rc\tunixtime=1561574840\ttimestamp=2019-06-26 21:47:20\ttimezone=+0300\tpid=1705\tlevel=info\tmessage=worker 1705 started";

        checker.checkEmpty(line);
    }
}
