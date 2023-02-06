package ru.yandex.market.logshatter.parser.front.errorBooster.qloudNode;


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
        checker.setLogBrokerTopic("qloud-node-topic");
    }

    @Test
    public void parseError1() throws Exception {
        String line = "{\"pushclient_row_id\":137040,\"message\":\"error_booster\",\"stackTrace\":\"\"," +
            "\"loggerName\":\"stdout\",\"@fields\":{\"additional\":{\"config\":\"testing\"," +
            "\"reqid\":\"5b484949a0f05c78cd2b8bfe65ea73f9\"},\"env\":\"testing\",\"ip\":\"192.168.1.1\",\"msg\":\"GET" +
            " /404 404\",\"page\":\"yateam-server\",\"sourceMethod\":\"source-method\",\"yandexuid\":412123213321," +
            "\"block\":\"block\",\"dc\":\"vla\",\"method\":\"method\",\"level\":\"warn\",\"region\":\"100\"," +
            "\"sourceType\":\"source-type\",\"project\":\"messenger\",\"source\":\"node_js\",\"stack\":\"Error: Not " +
            "found\\n    at error404Controller (/usr/src/app/src/controller/controller404.ts:9:23)\\n    at Layer" +
            ".handle [as handle_request] (/usr/src/app/node_modules/express/lib/router/layer.js:95:5)\\n    at " +
            "trim_prefix (/usr/src/app/node_modules/express/lib/router/index.js:317:13)\\n    at " +
            "/usr/src/app/node_modules/express/lib/router/index.js:284:7\\n    at Function.process_params " +
            "(/usr/src/app/node_modules/express/lib/router/index.js:335:12)\\n    at next " +
            "(/usr/src/app/node_modules/express/lib/router/index.js:275:10)\\n    at Immediate" +
            ".\\u003canonymous\\u003e (/usr/src/app/node_modules/express/lib/application.js:233:9)\\n    at Immediate" +
            "._onImmediate (/usr/src/app/node_modules/express/lib/router/index.js:635:15)\\n    at runCallback " +
            "(timers.js:812:20)\\n    at tryOnImmediate (timers.js:768:5)\",\"ts\":1561666585081,\"ua\":\"Mozilla/5.0" +
            " (Macintosh; Intel Mac OS X 10_14_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3826.0 " +
            "Safari/537.36\",\"version\":\"657240b5c2d0a84f0843d4374781204b0ba04ea9\"},\"threadName\":\"qloud-init\"," +
            "\"@timestamp\":\"2019-06-27T20:16:25.195Z\",\"level\":40000,\"levelStr\":\"ERROR\",\"@version\":1," +
            "\"version\":1,\"host\":\"iva8-ef1b180d4b34.qloud-c.yandex.net\",\"HOSTNAME\":\"iva8-ef1b180d4b34.qloud-c" +
            ".yandex.net\",\"qloud_project\":\"mssngr\",\"qloud_application\":\"yamb-web\"," +
            "\"qloud_environment\":\"test-corp\",\"qloud_component\":\"frontend\",\"qloud_instance\":\"frontend-1\"}";

        String message = "GET /404 404";
        String stacktrace = "Error: Not found\n" +
            "    at error404Controller (/usr/src/app/src/controller/controller404.ts:9:23)\n" +
            "    at Layer.handle [as handle_request] (/usr/src/app/node_modules/express/lib/router/layer.js:95:5)\n" +
            "    at trim_prefix (/usr/src/app/node_modules/express/lib/router/index.js:317:13)\n" +
            "    at /usr/src/app/node_modules/express/lib/router/index.js:284:7\n" +
            "    at Function.process_params (/usr/src/app/node_modules/express/lib/router/index.js:335:12)\n" +
            "    at next (/usr/src/app/node_modules/express/lib/router/index.js:275:10)\n" +
            "    at Immediate.<anonymous> (/usr/src/app/node_modules/express/lib/application.js:233:9)\n" +
            "    at Immediate._onImmediate (/usr/src/app/node_modules/express/lib/router/index.js:635:15)\n" +
            "    at runCallback (timers.js:812:20)\n" +
            "    at tryOnImmediate (timers.js:768:5)";

        UnsignedLong hashOfEmptyString = UnsignedLong.valueOf("2202906307356721367");

        checker.check(
            line,
            new Date(1561666585000L),
            "messenger", // PROJECT
            "", // SERVICE
            "yateam-server", // PAGE
            Platform.UNKNOWN, // PLATFORM
            "", // URL
            hashOfEmptyString, // URL_ID
            "", // VHOST
            Environment.TESTING, // ENVIRONMENT
            Arrays.asList(), // TEST_IDS
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
            "657240b5c2d0a84f0843d4374781204b0ba04ea9", // VERSION
            100, // REGION
            "", // REQUEST_ID
            UnsignedLong.valueOf("2202906307356721367"), // REQUEST_ID_HASH
            UnsignedLong.valueOf("412123213321"), // YANDEXUID
            Arrays.asList("qloud_instance", "qloud_environment", "qloud_component", "qloud_application", "config",
                "reqid"), // KV_KEYS
            Arrays.asList("frontend-1", "test-corp", "frontend", "yamb-web", "testing",
                "5b484949a0f05c78cd2b8bfe65ea73f9"), // KV_VALUES
            false, // IS_INTERNAL
            "", // CDN
            "::ffff:192.168.1.1", // IPv6
            false, // LOGGED_IN
            message, // MESSAGE
            UnsignedLong.valueOf("2961294820622506002"), // MESSAGE_ID
            message, // ORIGINAL_MESSAGE
            Arrays.asList(), // ERROR_URLS_KEYS
            Arrays.asList(), // ERROR_URLS_VALUES
            Runtime.NODEJS, // RUNTIME
            LogLevel.WARNING, // LEVEL
            "", // FILE
            hashOfEmptyString, // FILE_ID
            "block", // BLOCK
            "method", // METHOD
            0, // LINE
            0, // COL
            stacktrace, // STACK_TRACE
            UnsignedLong.valueOf("521733004378856325"), // STACK_TRACE_ID
            stacktrace, // ORIGINAL_STACK_TRACE
            Arrays.asList("error404Controller", "Layer.handle [as handle_request]", "trim_prefix", "(anonymous)",
                "Function.process_params", "next", "Immediate.<anonymous>", "Immediate._onImmediate", "runCallback",
                "tryOnImmediate"), // STACK_TRACE_NAMES
            Arrays.asList("/usr/src/app/src/controller/controller404.ts", "/usr/src/app/node_modules/express/lib" +
                "/router/layer.js", "/usr/src/app/node_modules/express/lib/router/index.js", "/usr/src/app" +
                "/node_modules/express/lib/router/index.js", "/usr/src/app/node_modules/express/lib/router/index.js",
                "/usr/src/app/node_modules/express/lib/router/index.js", "/usr/src/app/node_modules/express/lib" +
                    "/application.js", "/usr/src/app/node_modules/express/lib/router/index.js", "timers.js", "timers" +
                    ".js"), // STACK_TRACE_URLS
            Arrays.asList(9, 95, 317, 284, 335, 275, 233, 635, 812, 768), // STACK_TRACE_LINES
            Arrays.asList(23, 5, 13, 7, 12, 10, 9, 15, 20, 5), // STACK_TRACE_COLS
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3826" +
                ".0 Safari/537.36", // USER_AGENT
            UnsignedLong.valueOf("14227271854817069307"), // USER_AGENT_ID
            "node_js", // SOURCE
            "source-method", // SOURCE_METHOD
            "source-type", // SOURCE_TYPE
            UnsignedLong.valueOf(1561666585081L), // CLIENT_TIMESTAMP
            UnsignedLong.valueOf(0), // CLIENT_INIT_TIMESTAMP
            Arrays.asList(), // REPLACED_URLS_KEYS
            Arrays.asList(), // REPLACED_URLS_VALUES
            Parser.QLOUD_NODE, // PARSER
            UnsignedLong.valueOf("0"), // ICOOKIE
            false, // SILENT
            "vla", // DC
            0d, // COORDINATES_LATITUDE
            0d, // COORDINATES_LONGITUDE
            0, // COORDINATES_PRECISION
            0L, // COORDINATES_TIMESTAMP
            "iva8-ef1b180d4b34.qloud-c.yandex.net", // HOST
            checker.getLogBrokerTopic() // TOPIC
        );
    }

    @Test
    public void parseError2() throws Exception {
        String line = "{\"pushclient_row_id\":98550,\"message\":\"error_booster\",\"stackTrace\":\"Error: Not " +
            "found\\n    at error404Controller (/usr/src/app/src/controller/controller404.ts:9:23)\\n    at Layer" +
            ".handle [as handle_request] (/usr/src/app/node_modules/express/lib/router/layer.js:95:5)\\n    at " +
            "trim_prefix (/usr/src/app/node_modules/express/lib/router/index.js:317:13)\\n    at " +
            "/usr/src/app/node_modules/express/lib/router/index.js:284:7\\n    at Function.process_params " +
            "(/usr/src/app/node_modules/express/lib/router/index.js:335:12)\\n    at next " +
            "(/usr/src/app/node_modules/express/lib/router/index.js:275:10)\\n    at Immediate" +
            ".\\u003canonymous\\u003e (/usr/src/app/node_modules/express/lib/application.js:233:9)\\n    at Immediate" +
            "._onImmediate (/usr/src/app/node_modules/express/lib/router/index.js:635:15)\\n    at runCallback " +
            "(timers.js:812:20)\\n    at tryOnImmediate (timers.js:768:5)\",\"loggerName\":\"stdout\"," +
            "\"@fields\":{\"additional\":{\"config\":\"testing\"},\"env\":\"testing\",\"msg\":\"GET /404 404\"," +
            "\"page\":\"yateam-server\",\"project\":\"messenger\",\"reqid\":\"91a24f634b2c3c0a14ba911add5c9169\"," +
            "\"ts\":1561672322331,\"ua\":\"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_5) AppleWebKit/537.36 (KHTML," +
            " like Gecko) Chrome/77.0.3826.0 Safari/537.36\",\"url\":\"https://2950.pr.messenger.test.yandex-team" +
            ".ru/404\",\"version\":\"v1.31.0-pull-2950\",\"yandexuid\":\"999167301560545095\"}," +
            "\"threadName\":\"qloud-init\",\"@timestamp\":\"2019-06-27T21:52:02.435Z\",\"level\":40000," +
            "\"levelStr\":\"ERROR\",\"@version\":1,\"version\":1,\"host\":\"iva8-ef1b180d4b34.qloud-c.yandex.net\"," +
            "\"HOSTNAME\":\"iva8-ef1b180d4b34.qloud-c.yandex.net\",\"qloud_project\":\"mssngr\"," +
            "\"qloud_application\":\"yamb-web\",\"qloud_environment\":\"test-corp\",\"qloud_component\":\"frontend\"," +
            "\"qloud_instance\":\"frontend-1\"}";

        String message = "GET /404 404";
        String stacktrace = "Error: Not found\n" +
            "    at error404Controller (/usr/src/app/src/controller/controller404.ts:9:23)\n" +
            "    at Layer.handle [as handle_request] (/usr/src/app/node_modules/express/lib/router/layer.js:95:5)\n" +
            "    at trim_prefix (/usr/src/app/node_modules/express/lib/router/index.js:317:13)\n" +
            "    at /usr/src/app/node_modules/express/lib/router/index.js:284:7\n" +
            "    at Function.process_params (/usr/src/app/node_modules/express/lib/router/index.js:335:12)\n" +
            "    at next (/usr/src/app/node_modules/express/lib/router/index.js:275:10)\n" +
            "    at Immediate.<anonymous> (/usr/src/app/node_modules/express/lib/application.js:233:9)\n" +
            "    at Immediate._onImmediate (/usr/src/app/node_modules/express/lib/router/index.js:635:15)\n" +
            "    at runCallback (timers.js:812:20)\n" +
            "    at tryOnImmediate (timers.js:768:5)";

        UnsignedLong hashOfEmptyString = UnsignedLong.valueOf("2202906307356721367");

        checker.check(
            line,
            new Date(1561672322000L),
            "messenger", // PROJECT
            "", // SERVICE
            "yateam-server", // PAGE
            Platform.UNKNOWN, // PLATFORM
            "https://2950.pr.messenger.test.yandex-team.ru/404", // URL
            UnsignedLong.valueOf("4827522659096773621"), // URL_ID
            "2950.pr.messenger.test.yandex-team.ru", // VHOST
            Environment.TESTING, // ENVIRONMENT
            Arrays.asList(), // TEST_IDS
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
            "v1.31.0-pull-2950", // VERSION
            0, // REGION
            "91a24f634b2c3c0a14ba911add5c9169", // REQUEST_ID
            UnsignedLong.valueOf("1196198431164317738"), // REQUEST_ID_HASH
            UnsignedLong.valueOf("999167301560545095"), // YANDEXUID
            Arrays.asList("qloud_instance", "qloud_environment", "qloud_component", "qloud_application", "config"),
            // KV_KEYS
            Arrays.asList("frontend-1", "test-corp", "frontend", "yamb-web", "testing"), // KV_VALUES
            false, // IS_INTERNAL
            "", // CDN
            "", // IPv6
            false, // LOGGED_IN
            message, // MESSAGE
            UnsignedLong.valueOf("2961294820622506002"), // MESSAGE_ID
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
            UnsignedLong.valueOf("521733004378856325"), // STACK_TRACE_ID
            stacktrace, // ORIGINAL_STACK_TRACE
            Arrays.asList("error404Controller", "Layer.handle [as handle_request]", "trim_prefix", "(anonymous)",
                "Function.process_params", "next", "Immediate.<anonymous>", "Immediate._onImmediate", "runCallback",
                "tryOnImmediate"), // STACK_TRACE_NAMES
            Arrays.asList("/usr/src/app/src/controller/controller404.ts", "/usr/src/app/node_modules/express/lib" +
                "/router/layer.js", "/usr/src/app/node_modules/express/lib/router/index.js", "/usr/src/app" +
                "/node_modules/express/lib/router/index.js", "/usr/src/app/node_modules/express/lib/router/index.js",
                "/usr/src/app/node_modules/express/lib/router/index.js", "/usr/src/app/node_modules/express/lib" +
                    "/application.js", "/usr/src/app/node_modules/express/lib/router/index.js", "timers.js", "timers" +
                    ".js"), // STACK_TRACE_URLS
            Arrays.asList(9, 95, 317, 284, 335, 275, 233, 635, 812, 768), // STACK_TRACE_LINES
            Arrays.asList(23, 5, 13, 7, 12, 10, 9, 15, 20, 5), // STACK_TRACE_COLS
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3826" +
                ".0 Safari/537.36", // USER_AGENT
            UnsignedLong.valueOf("14227271854817069307"), // USER_AGENT_ID
            "", // SOURCE
            "", // SOURCE_METHOD
            "", // SOURCE_TYPE
            UnsignedLong.valueOf(1561672322331L), // CLIENT_TIMESTAMP
            UnsignedLong.valueOf(0), // CLIENT_INIT_TIMESTAMP
            Arrays.asList(), // REPLACED_URLS_KEYS
            Arrays.asList(), // REPLACED_URLS_VALUES
            Parser.QLOUD_NODE, // PARSER
            UnsignedLong.valueOf("0"), // ICOOKIE
            false, // SILENT
            "", // DC
            0d, // COORDINATES_LATITUDE
            0d, // COORDINATES_LONGITUDE
            0, // COORDINATES_PRECISION
            0L, // COORDINATES_TIMESTAMP
            "iva8-ef1b180d4b34.qloud-c.yandex.net", // HOST
            checker.getLogBrokerTopic() // TOPIC
        );
    }

    @Test
    public void skipWithoutFields() throws Exception {
        String line = "{\"pushclient_row_id\":137040,\"message\":\"error_booster\",\"stackTrace\":\"\"," +
            "\"loggerName\":\"stdout\",\"threadName\":\"qloud-init\",\"@timestamp\":\"2019-06-27T20:16:25.195Z\"," +
            "\"level\":40000,\"levelStr\":\"ERROR\",\"@version\":1,\"version\":1,\"host\":\"iva8-ef1b180d4b34.qloud-c" +
            ".yandex.net\",\"HOSTNAME\":\"iva8-ef1b180d4b34.qloud-c.yandex.net\",\"qloud_project\":\"mssngr\"," +
            "\"qloud_application\":\"yamb-web\",\"qloud_environment\":\"test-corp\",\"qloud_component\":\"frontend\"," +
            "\"qloud_instance\":\"frontend-1\"}";

        checker.checkEmpty(line);
    }

    @Test
    public void skipWithoutProject() throws Exception {
        String line = "{\"pushclient_row_id\":137040,\"message\":\"error_booster\",\"stackTrace\":\"\"," +
            "\"loggerName\":\"stdout\",\"@fields\":{},\"threadName\":\"qloud-init\"," +
            "\"@timestamp\":\"2019-06-27T20:16:25.195Z\",\"level\":40000,\"levelStr\":\"ERROR\",\"@version\":1," +
            "\"version\":1,\"host\":\"iva8-ef1b180d4b34.qloud-c.yandex.net\",\"HOSTNAME\":\"iva8-ef1b180d4b34.qloud-c" +
            ".yandex.net\",\"qloud_project\":\"mssngr\",\"qloud_application\":\"yamb-web\"," +
            "\"qloud_environment\":\"test-corp\",\"qloud_component\":\"frontend\",\"qloud_instance\":\"frontend-1\"}";

        checker.checkEmpty(line);
    }
}
