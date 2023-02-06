package ru.yandex.market.logshatter.parser.front.errorBooster.sentry;


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
        checker.setLogBrokerTopic("sentry-topic");
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void pythonParser() throws Exception {
        String line = "{\"fingerprint\":[\"myrpc\",\"POST\",\"/foo.bar\"],\"exception\":{\"values\": " +
            "[{\"stacktrace\":{\"frames\":[{\"function\":\"<module>\",\"abs_path\":\"/home/orloffv/sentry/app.py\"," +
            "\"pre_context\":[\"capture_message(\\\"Hello World\\\")\",\"\",\"logging.error('Protocol problem: %s', " +
            "'connection reset', extra=dict(extra_key=\\\"extra_value\\\"))\",\"logging.error('Protocol problem: %s, " +
            "%s, %s', 'connection reset', 'connection bad', extra=dict(extra_key=\\\"extra_value\\\"))\",\"\"]," +
            "\"vars\":{\"logging\":\"<module 'logging' from '/usr/lib/python2.7/logging/__init__.pyc'>\"," +
            "\"sentry_logging\":\"<sentry_sdk.integrations.logging.LoggingIntegration object at 0x7f1f04b33690>\"," +
            "\"__builtins__\":\"<module '__builtin__' (built-in)>\",\"__file__\":\"'app.py'\",\"set_tag\":\"<function" +
            " set_tag at 0x7f1f04abb320>\",\"__package__\":\"None\",\"set_user\":\"<function set_user at " +
            "0x7f1f04abb488>\",\"add_breadcrumb\":\"<function add_breadcrumb at 0x7f1f04abb1b8>\"," +
            "\"__name__\":\"'__main__'\",\"__doc__\":\"None\"},\"post_context\":[],\"in_app\":true," +
            "\"module\":\"__main__\",\"filename\":\"app.py\",\"context_line\":\"raise ValueError(\\\"It really " +
            "works!\\\")\",\"lineno\":73},{\"filename\":\"__init__.py\",\"abs_path\":\"/usr/lib/python3" +
            ".6/json/__init__.py\",\"function\":\"loads\",\"module\":\"json\",\"lineno\":354,\"pre_context\":[]," +
            "\"context_line\":\"        return _default_decoder.decode(s)\",\"post_context\":[],\"vars\":{\"kw\":{}," +
            "\"s\":\"'asdasdasd'\"},\"in_app\":true},{\"filename\":\"json/decoder.py\"," +
            "\"abs_path\":\"/usr/lib/python3.6/json/decoder.py\",\"function\":\"decode\",\"module\":\"json.decoder\"," +
            "\"lineno\":339,\"pre_context\":[],\"context_line\":\"        obj, end = self.raw_decode(s, idx=_w(s, 0)" +
            ".end())\",\"post_context\":[],\"vars\":{\"s\":\"'asdasdasd'\"},\"in_app\":true}]}," +
            "\"type\":\"ValueError\",\"value\":\"It really works!\",\"module\":\"exceptions\"," +
            "\"mechanism\":{\"type\":\"excepthook\",\"handled\":false}}]},\"server_name\":\"orloffv-dev.sas.yp-c" +
            ".yandex.net\",\"level\":\"error\",\"contexts\":{\"runtime\":{\"version\":\"2.7.12\"," +
            "\"name\":\"CPython\",\"build\":\"2.7.12 (default, Mar  1 2021, 11:38:31) \\n[GCC 5.4.0 20160609]\"}," +
            "\"character\":{\"attack_type\":\"melee\",\"age\":19,\"name\":\"Mighty Fighter\"}}," +
            "\"timestamp\":\"2021-11-02T12:01:45.579702Z\",\"extra\":{\"sys.argv\":[\"app.py\"]}," +
            "\"modules\":{\"pychecker\":\"0.8.19\",\"mysqlclient\":\"1.3.7\",\"pip\":\"8.1.1\",\"virtualenv\":\"15.0" +
            ".1\",\"lxml\":\"3.5.0\",\"sentry-sdk\":\"1.4.3\",\"six\":\"1.10.0\",\"yandex-yt\":\"0.10.10\"," +
            "\"certifi\":\"2021.10.8\",\"numpy\":\"1.11.0\",\"pysqlite\":\"2.7.0\",\"bsddb3\":\"6.1.0\"," +
            "\"protobuf\":\"2.6.1\",\"python\":\"2.7.12\",\"urllib3\":\"1.26.7\",\"markupsafe\":\"0.23\"," +
            "\"scipy\":\"0.17.0\",\"jinja2\":\"2.8\",\"python-dateutil\":\"2.4.2\",\"msgpack-python\":\"0.4.6\"," +
            "\"requests\":\"2.9.1\",\"decorator\":\"4.0.6\",\"argparse\":\"1.2.1\",\"pyyaml\":\"3.11\",\"pycurl\":\"7" +
            ".43.0\",\"python-systemd\":\"231\",\"mercurial\":\"3.7.3\",\"portopy\":\"4.18.32\",\"wsgiref\":\"0.1" +
            ".2\",\"chardet\":\"2.3.0\",\"pycrypto\":\"2.6.1\",\"python-apt\":\"1.1.0b1+ubuntu0.16.4.11\"}," +
            "\"environment\":\"development\",\"event_id\":\"1e7bd78e00c94157930e479478b51c55\"," +
            "\"platform\":\"python\",\"breadcrumbs\":{\"values\":[{\"category\":\"auth\",\"level\":\"info\"," +
            "\"timestamp\":\"2021-11-02T12:01:45.579525Z\",\"type\":\"navigation\",\"message\":\"Authenticated user " +
            "test@yandex-team.ru\",\"data\":{\"to\":\"/dashboard\",\"from\":\"/login\"}}]}," +
            "\"release\":\"release_version\",\"sdk\":{\"version\":\"1.4.3\",\"name\":\"sentry.python\"," +
            "\"packages\":[{\"version\":\"1.4.3\",\"name\":\"pypi:sentry-sdk\"}],\"integrations\":[\"argv\"," +
            "\"atexit\",\"dedupe\",\"excepthook\",\"logging\",\"modules\",\"stdlib\",\"threading\"]}," +
            "\"tags\":{\"loggedin\":\"true\",\"sourceType\":\"sourceType\",\"service\":\"python_service\"," +
            "\"isRobot\":\"true\",\"url\":\"https://yandex.com/python\",\"isInternal\":\"true\",\"region\":\"199\"," +
            "\"sourceMethod\":\"sourceMethod\",\"dc\":\"vla\",\"page\":\"custom_page\",\"project\":\"python_sentry\"," +
            "\"source\":\"source\",\"reqid\":\"request-id-1234\",\"experiments\":\"aaa=1;bb=7;ccc=yes;ddd\"," +
            "\"platform\":\"desktop\",\"isAdblock\":\"true\",\"useragent\":\"Mozilla/5.0 (Macintosh; Intel Mac OS X " +
            "10_14_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3826.0 Safari/537.36\",\"slots\":\"123123," +
            "12,12;5555,3,1\",\"method\":\"method\",\"block\":\"block_WITH_VERY_VERY_SECRET_\"," +
            "\"my_tag\":\"my_tag_value\"},\"user\":{\"yandexuid\":\"12345678890\",\"ip_address\":\"192.168.1.1\"," +
            "\"puid\":\"12931231231\"}}";
        UnsignedLong hashOfEmptyString = UnsignedLong.valueOf("2202906307356721367");

        checker.check(
            line,
            new Date(1635854505000L),
            "python_sentry", // PROJECT
            "python_service", // SERVICE
            "custom_page", // PAGE
            Platform.DESKTOP, // PLATFORM
            "https://yandex.com/python", // URL
            UnsignedLong.valueOf("11051605148223342864"), // URL_ID
            "yandex.com", // VHOST
            Environment.DEVELOPMENT, // ENVIRONMENT
            Arrays.asList(5555, 123123), // TEST_IDS
            Arrays.asList("aaa=1", "bb=7", "ccc=yes", "ddd"), // EXP_FLAGS
            "WebKit", // BROWSER_ENGINE
            "537.36", // BfROWSER_ENGINE_VERSION
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
            "release_version", // VERSION
            199, // REGION
            "request-id-1234", // REQUEST_ID
            UnsignedLong.valueOf("1233527196171568231"), // REQUEST_ID_HASH
            UnsignedLong.valueOf("12345678890"), // YANDEXUID
            Arrays.asList("puid", "my_tag", "breadcrumbs", "contexts", "extra"), // KV_KEYS
            Arrays.asList("12931231231", "my_tag_value", "[{\"category\":\"auth\",\"level\":\"info\"," +
                "\"timestamp\":\"2021-11-02T12:01:45.579525Z\",\"type\":\"navigation\",\"message\":\"Authenticated " +
                "user test@yandex-team.ru\",\"data\":{\"to\":\"/dashboard\",\"from\":\"/login\"}}]", "{\"runtime" +
                "\":{\"version\":\"2.7.12\",\"name\":\"CPython\",\"build\":\"2.7.12 (default, Mar  1 2021, 11:38:31) " +
                "\\n[GCC 5.4.0 20160609]\"},\"character\":{\"attack_type\":\"melee\",\"age\":19,\"name\":\"Mighty " +
                "Fighter\"}}", "{\"sys.argv\":[\"app.py\"]}"), // KV_VALUES
            true, // IS_INTERNAL
            "", // CDN
            "::ffff:192.168.1.1", // IPv6
            true, // LOGGED_IN
            "exceptions.ValueError: It really works!", // MESSAGE
            UnsignedLong.valueOf("6789854735103128386"), // MESSAGE_ID
            "exceptions.ValueError: It really works!", // ORIGINAL_MESSAGE
            Arrays.asList(), // ERROR_URLS_KEYS
            Arrays.asList(), // ERROR_URLS_VALUES
            Runtime.PYTHON, // RUNTIME
            LogLevel.ERROR, // LEVEL
            "/home/orloffv/sentry/app.py", // FILE
            UnsignedLong.valueOf("17493307811081226566"), // FILE_ID
            "block_WITH_VERY_VERY_SECRET_", // BLOCK
            "<module>", // METHOD
            0, // LINE
            0, // COL
            "exceptions.ValueError: It really works!\n" +
                "  File \"/home/orloffv/sentry/app.py\", line 73, in <module>\n" +
                "    raise ValueError(\"It really works!\")\n" +
                "  File \"/usr/lib/python3.6/json/__init__.py\", line 354, in loads\n" +
                "    return _default_decoder.decode(s)\n" +
                "  File \"/usr/lib/python3.6/json/decoder.py\", line 339, in decode\n" +
                "    obj, end = self.raw_decode(s, idx=_w(s, 0).end())", // STACK_TRACE
            UnsignedLong.valueOf("8960601308152724625"), // STACK_TRACE_ID
            "exceptions.ValueError: It really works!\n" +
                "  File \"/home/orloffv/sentry/app.py\", line 73, in <module>\n" +
                "    raise ValueError(\"It really works!\")\n" +
                "  File \"/usr/lib/python3.6/json/__init__.py\", line 354, in loads\n" +
                "    return _default_decoder.decode(s)\n" +
                "  File \"/usr/lib/python3.6/json/decoder.py\", line 339, in decode\n" +
                "    obj, end = self.raw_decode(s, idx=_w(s, 0).end())", // ORIGINAL_STACK_TRACE
            Arrays.asList("<module>", "loads", "decode"), // STACK_TRACE_NAMES
            Arrays.asList("/home/orloffv/sentry/app.py", "/usr/lib/python3.6/json/__init__.py", "/usr/lib/python3" +
                ".6/json/decoder.py"), // STACK_TRACE_URLS
            Arrays.asList(73, 354, 339), // STACK_TRACE_LINES
            Arrays.asList(0, 0, 0), // STACK_TRACE_COLS
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3826" +
                ".0 Safari/537.36", // USER_AGENT
            UnsignedLong.valueOf("1319564247142717470"), // USER_AGENT_ID
            "source", // SOURCE
            "sourceMethod", // SOURCE_METHOD
            "sourceType", // SOURCE_TYPE
            UnsignedLong.valueOf(0), // CLIENT_TIMESTAMP
            UnsignedLong.valueOf(0), // CLIENT_INIT_TIMESTAMP
            Arrays.asList(), // REPLACED_URLS_KEYS
            Arrays.asList(), // REPLACED_URLS_VALUES
            Parser.SENTRY, // PARSER
            UnsignedLong.valueOf("0"), // ICOOKIE
            false, // SILENT
            "vla", // DC
            0d, // COORDINATES_LATITUDE
            0d, // COORDINATES_LONGITUDE
            0, // COORDINATES_PRECISION
            0L, // COORDINATES_TIMESTAMP
            "orloffv-dev.sas.yp-c.yandex.net", // HOST
            checker.getLogBrokerTopic() // TOPIC
        );
    }

    @Test
    public void pythonParserWithMessage() throws Exception {
        String line = "{\"server_name\":\"orloffv-dev.sas.yp-c.yandex.net\",\"extra\":{\"sys.argv\":[\"app.py\"]}," +
            "\"contexts\":{\"runtime\":{\"version\":\"2.7.12\",\"name\":\"CPython\",\"build\":\"2.7.12 (default, Mar " +
            " 1 2021, 11:38:31) \\n[GCC 5.4.0 20160609]\"},\"character\":{\"attack_type\":\"melee\",\"age\":19," +
            "\"name\":\"Mighty Fighter\"}},\"timestamp\":\"2021-11-02T13:18:54.200887Z\",\"level\":\"info\"," +
            "\"modules\":{\"pychecker\":\"0.8.19\",\"mysqlclient\":\"1.3.7\",\"pip\":\"8.1.1\",\"virtualenv\":\"15.0" +
            ".1\",\"lxml\":\"3.5.0\",\"sentry-sdk\":\"1.4.3\",\"six\":\"1.10.0\",\"yandex-yt\":\"0.10.10\"," +
            "\"certifi\":\"2021.10.8\",\"numpy\":\"1.11.0\",\"pysqlite\":\"2.7.0\",\"bsddb3\":\"6.1.0\"," +
            "\"protobuf\":\"2.6.1\",\"python\":\"2.7.12\",\"urllib3\":\"1.26.7\",\"markupsafe\":\"0.23\"," +
            "\"scipy\":\"0.17.0\",\"jinja2\":\"2.8\",\"python-dateutil\":\"2.4.2\",\"msgpack-python\":\"0.4.6\"," +
            "\"requests\":\"2.9.1\",\"decorator\":\"4.0.6\",\"argparse\":\"1.2.1\",\"pyyaml\":\"3.11\",\"pycurl\":\"7" +
            ".43.0\",\"python-systemd\":\"231\",\"mercurial\":\"3.7.3\",\"portopy\":\"4.18.32\",\"wsgiref\":\"0.1" +
            ".2\",\"chardet\":\"2.3.0\",\"pycrypto\":\"2.6.1\",\"python-apt\":\"1.1.0b1+ubuntu0.16.4.11\"}," +
            "\"environment\":\"development\",\"event_id\":\"a5f029b25ef44950babea7289854be50\"," +
            "\"platform\":\"python\",\"breadcrumbs\":{\"values\":[{\"category\":\"auth\",\"level\":\"info\"," +
            "\"timestamp\":\"2021-11-02T13:18:54.200819Z\",\"type\":\"navigation\",\"message\":\"Authenticated user " +
            "test@yandex-team.ru\",\"data\":{\"to\":\"/dashboard\",\"from\":\"/login\"}}]}," +
            "\"release\":\"release_version\",\"message\":\"Hello World\",\"sdk\":{\"version\":\"1.4.3\"," +
            "\"name\":\"sentry.python\",\"packages\":[{\"version\":\"1.4.3\",\"name\":\"pypi:sentry-sdk\"}]," +
            "\"integrations\":[\"argv\",\"atexit\",\"dedupe\",\"excepthook\",\"logging\",\"modules\",\"stdlib\"," +
            "\"threading\"]},\"tags\":{\"loggedin\":\"true\",\"sourceType\":\"sourceType\"," +
            "\"service\":\"python_service\",\"isRobot\":\"true\",\"url\":\"https://yandex.com/python\"," +
            "\"isInternal\":\"true\",\"region\":\"199\",\"sourceMethod\":\"sourceMethod\",\"dc\":\"vla\"," +
            "\"page\":\"custom_page\",\"project\":\"python_sentry\",\"source\":\"source\"," +
            "\"reqid\":\"request-id-1234\",\"experiments\":\"aaa=1;bb=7;ccc=yes;ddd\",\"platform\":\"desktop\"," +
            "\"isAdblock\":\"true\",\"useragent\":\"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537" +
            ".36 (KHTML, like Gecko) Chrome/77.0.3826.0 Safari/537.36\",\"slots\":\"123123,12,12;5555,3,1\"," +
            "\"method\":\"method\",\"block\":\"block\",\"my_tag\":\"my_tag_value\"}," +
            "\"user\":{\"yandexuid\":\"12345678890\",\"ip_address\":\"192.168.1.1\",\"puid\":\"12931231231\"}}";
        UnsignedLong hashOfEmptyString = UnsignedLong.valueOf("2202906307356721367");

        checker.check(
            line,
            new Date(1635859134000L),
            "python_sentry", // PROJECT
            "python_service", // SERVICE
            "custom_page", // PAGE
            Platform.DESKTOP, // PLATFORM
            "https://yandex.com/python", // URL
            UnsignedLong.valueOf("11051605148223342864"), // URL_ID
            "yandex.com", // VHOST
            Environment.DEVELOPMENT, // ENVIRONMENT
            Arrays.asList(5555, 123123), // TEST_IDS
            Arrays.asList("aaa=1", "bb=7", "ccc=yes", "ddd"), // EXP_FLAGS
            "WebKit", // BROWSER_ENGINE
            "537.36", // BfROWSER_ENGINE_VERSION
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
            "release_version", // VERSION
            199, // REGION
            "request-id-1234", // REQUEST_ID
            UnsignedLong.valueOf("1233527196171568231"), // REQUEST_ID_HASH
            UnsignedLong.valueOf("12345678890"), // YANDEXUID
            Arrays.asList("puid", "my_tag", "breadcrumbs", "contexts", "extra"), // KV_KEYS
            Arrays.asList("12931231231", "my_tag_value", "[{\"category\":\"auth\",\"level\":\"info\"," +
                "\"timestamp\":\"2021-11-02T13:18:54.200819Z\",\"type\":\"navigation\",\"message\":\"Authenticated " +
                "user test@yandex-team.ru\",\"data\":{\"to\":\"/dashboard\",\"from\":\"/login\"}}]", "{\"runtime" +
                "\":{\"version\":\"2.7.12\",\"name\":\"CPython\",\"build\":\"2.7.12 (default, Mar  1 2021, 11:38:31) " +
                "\\n[GCC 5.4.0 20160609]\"},\"character\":{\"attack_type\":\"melee\",\"age\":19,\"name\":\"Mighty " +
                "Fighter\"}}", "{\"sys.argv\":[\"app.py\"]}"), // KV_VALUES
            true, // IS_INTERNAL
            "", // CDN
            "::ffff:192.168.1.1", // IPv6
            true, // LOGGED_IN
            "Hello World", // MESSAGE
            UnsignedLong.valueOf("14021647942854966113"), // MESSAGE_ID
            "Hello World", // ORIGINAL_MESSAGE
            Arrays.asList(), // ERROR_URLS_KEYS
            Arrays.asList(), // ERROR_URLS_VALUES
            Runtime.PYTHON, // RUNTIME
            LogLevel.INFO, // LEVEL
            "", // FILE
            UnsignedLong.valueOf("2202906307356721367"), // FILE_ID
            "block", // BLOCK
            "method", // METHOD
            0, // LINE
            0, // COL
            "", // STACK_TRACE
            UnsignedLong.valueOf("2202906307356721367"), // STACK_TRACE_ID
            "", // ORIGINAL_STACK_TRACE
            Arrays.asList(), // STACK_TRACE_NAMES
            Arrays.asList(), // STACK_TRACE_URLS
            Arrays.asList(), // STACK_TRACE_LINES
            Arrays.asList(), // STACK_TRACE_COLS
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3826" +
                ".0 Safari/537.36", // USER_AGENT
            UnsignedLong.valueOf("1319564247142717470"), // USER_AGENT_ID
            "source", // SOURCE
            "sourceMethod", // SOURCE_METHOD
            "sourceType", // SOURCE_TYPE
            UnsignedLong.valueOf(0), // CLIENT_TIMESTAMP
            UnsignedLong.valueOf(0), // CLIENT_INIT_TIMESTAMP
            Arrays.asList(), // REPLACED_URLS_KEYS
            Arrays.asList(), // REPLACED_URLS_VALUES
            Parser.SENTRY, // PARSER
            UnsignedLong.valueOf("0"), // ICOOKIE
            false, // SILENT
            "vla", // DC
            0d, // COORDINATES_LATITUDE
            0d, // COORDINATES_LONGITUDE
            0, // COORDINATES_PRECISION
            0L, // COORDINATES_TIMESTAMP
            "orloffv-dev.sas.yp-c.yandex.net", // HOST
            checker.getLogBrokerTopic() // TOPIC
        );
    }

    @Test
    public void pythonParserWithLogEntry() throws Exception {
        String line = "{\"server_name\":\"orloffv-dev.sas.yp-c.yandex.net\",\"extra\":{\"sys.argv\":[\"app.py\"]," +
            "\"extra_key\":\"extra_value\"},\"contexts\":{\"runtime\":{\"version\":\"2.7.12\",\"name\":\"CPython\"," +
            "\"build\":\"2.7.12 (default, Mar  1 2021, 11:38:31) \\n[GCC 5.4.0 20160609]\"}," +
            "\"character\":{\"attack_type\":\"melee\",\"age\":19,\"name\":\"Mighty Fighter\"}}," +
            "\"timestamp\":\"2021-11-02T13:48:58.321460Z\",\"level\":\"error\",\"modules\":{\"pychecker\":\"0.8.19\"," +
            "\"mysqlclient\":\"1.3.7\",\"pip\":\"8.1.1\",\"virtualenv\":\"15.0.1\",\"lxml\":\"3.5.0\"," +
            "\"sentry-sdk\":\"1.4.3\",\"six\":\"1.10.0\",\"yandex-yt\":\"0.10.10\",\"certifi\":\"2021.10.8\"," +
            "\"numpy\":\"1.11.0\",\"pysqlite\":\"2.7.0\",\"bsddb3\":\"6.1.0\",\"protobuf\":\"2.6.1\",\"python\":\"2.7" +
            ".12\",\"urllib3\":\"1.26.7\",\"markupsafe\":\"0.23\",\"scipy\":\"0.17.0\",\"jinja2\":\"2.8\"," +
            "\"python-dateutil\":\"2.4.2\",\"msgpack-python\":\"0.4.6\",\"requests\":\"2.9.1\",\"decorator\":\"4.0" +
            ".6\",\"argparse\":\"1.2.1\",\"pyyaml\":\"3.11\",\"pycurl\":\"7.43.0\",\"python-systemd\":\"231\"," +
            "\"mercurial\":\"3.7.3\",\"portopy\":\"4.18.32\",\"wsgiref\":\"0.1.2\",\"chardet\":\"2.3.0\"," +
            "\"pycrypto\":\"2.6.1\",\"python-apt\":\"1.1.0b1+ubuntu0.16.4.11\"},\"environment\":\"development\"," +
            "\"event_id\":\"8566bbc57c0e44f9a0f72443391e1d3b\",\"platform\":\"python\"," +
            "\"logentry\":{\"message\":\"Protocol problem: %s, %s, %s, %6.2fs\",\"params\":[\"connection reset\", " +
            "-1001428145181, null, 0.824]},\"breadcrumbs\":{\"values\":[{\"category\":\"auth\",\"level\":\"info\"," +
            "\"timestamp\":\"2021-11-02T13:48:58.273890Z\",\"type\":\"navigation\",\"message\":\"Authenticated user " +
            "test@yandex-team.ru\",\"data\":{\"to\":\"/dashboard\",\"from\":\"/login\"}}]}," +
            "\"release\":\"release_version\",\"logger\":\"root\",\"sdk\":{\"version\":\"1.4.3\",\"name\":\"sentry" +
            ".python\",\"packages\":[{\"version\":\"1.4.3\",\"name\":\"pypi:sentry-sdk\"}]," +
            "\"integrations\":[\"argv\",\"atexit\",\"dedupe\",\"excepthook\",\"logging\",\"modules\",\"stdlib\"," +
            "\"threading\"]},\"tags\":{\"loggedin\":\"true\",\"sourceType\":\"sourceType\"," +
            "\"service\":\"python_service\",\"isRobot\":\"true\",\"url\":\"https://yandex.com/python\"," +
            "\"isInternal\":\"true\",\"region\":\"199\",\"sourceMethod\":\"sourceMethod\",\"dc\":\"vla\"," +
            "\"page\":\"custom_page\",\"project\":\"python_sentry\",\"source\":\"source\"," +
            "\"reqid\":\"request-id-1234\",\"experiments\":\"aaa=1;bb=7;ccc=yes;ddd\",\"platform\":\"desktop\"," +
            "\"isAdblock\":\"true\",\"useragent\":\"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537" +
            ".36 (KHTML, like Gecko) Chrome/77.0.3826.0 Safari/537.36\",\"slots\":\"123123,12,12;5555,3,1\"," +
            "\"method\":\"method\",\"block\":\"block_WITH_VERY_VERY_SECRET_\",\"my_tag\":\"my_tag_value\"}," +
            "\"user\":{\"yandexuid\":\"12345678890\",\"ip_address\":\"192.168.1.1\",\"puid\":\"12931231231\"}}";
        UnsignedLong hashOfEmptyString = UnsignedLong.valueOf("2202906307356721367");

        checker.setParam("sanitizer", "true");
        checker.check(
            line,
            new Date(1635860938000L),
            "python_sentry", // PROJECT
            "python_service", // SERVICE
            "custom_page", // PAGE
            Platform.DESKTOP, // PLATFORM
            "https://yandex.com/python", // URL
            UnsignedLong.valueOf("11051605148223342864"), // URL_ID
            "yandex.com", // VHOST
            Environment.DEVELOPMENT, // ENVIRONMENT
            Arrays.asList(5555, 123123), // TEST_IDS
            Arrays.asList("aaa=1", "bb=7", "ccc=yes", "ddd"), // EXP_FLAGS
            "WebKit", // BROWSER_ENGINE
            "537.36", // BfROWSER_ENGINE_VERSION
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
            "release_version", // VERSION
            199, // REGION
            "request-id-1234", // REQUEST_ID
            UnsignedLong.valueOf("1233527196171568231"), // REQUEST_ID_HASH
            UnsignedLong.valueOf("12345678890"), // YANDEXUID
            Arrays.asList("puid", "my_tag", "breadcrumbs", "contexts", "extra", "logger", "message_formatted"), //
            // KV_KEYS
            Arrays.asList("12931231231", "my_tag_value", "[{\"category\":\"auth\",\"level\":\"info\"," +
                "\"timestamp\":\"2021-11-02T13:48:58.273890Z\",\"type\":\"navigation\",\"message\":\"Authenticated " +
                "user test@yandex-team.ru\",\"data\":{\"to\":\"/dashboard\",\"from\":\"/login\"}}]", "{\"runtime" +
                "\":{\"version\":\"2.7.12\",\"name\":\"CPython\",\"build\":\"2.7.12 (default, Mar  1 2021, 11:38:31) " +
                "\\n[GCC 5.4.0 20160609]\"},\"character\":{\"attack_type\":\"melee\",\"age\":19,\"name\":\"Mighty " +
                "Fighter\"}}", "{\"sys.argv\":[\"app.py\"],\"extra_key\":\"extra_value\"}", "root", "Protocol " +
                "problem: connection reset, -1001428145181, null,   0.82s"), // KV_VALUES
            true, // IS_INTERNAL
            "", // CDN
            "::ffff:192.168.1.1", // IPv6
            true, // LOGGED_IN
            "Protocol problem: %s, %s, %s, %6.2fs", // MESSAGE
            UnsignedLong.valueOf("1753738061433201562"), // MESSAGE_ID
            "Protocol problem: %s, %s, %s, %6.2fs", // ORIGINAL_MESSAGE
            Arrays.asList(), // ERROR_URLS_KEYS
            Arrays.asList(), // ERROR_URLS_VALUES
            Runtime.PYTHON, // RUNTIME
            LogLevel.ERROR, // LEVEL
            "", // FILE
            UnsignedLong.valueOf("2202906307356721367"), // FILE_ID
            "block_WITH_VERY_VERY_XXXXXX_", // BLOCK
            "method", // METHOD
            0, // LINE
            0, // COL
            "", // STACK_TRACE
            UnsignedLong.valueOf("2202906307356721367"), // STACK_TRACE_ID
            "", // ORIGINAL_STACK_TRACE
            Arrays.asList(), // STACK_TRACE_NAMES
            Arrays.asList(), // STACK_TRACE_URLS
            Arrays.asList(), // STACK_TRACE_LINES
            Arrays.asList(), // STACK_TRACE_COLS
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3826" +
                ".0 Safari/537.36", // USER_AGENT
            UnsignedLong.valueOf("1319564247142717470"), // USER_AGENT_ID
            "source", // SOURCE
            "sourceMethod", // SOURCE_METHOD
            "sourceType", // SOURCE_TYPE
            UnsignedLong.valueOf(0), // CLIENT_TIMESTAMP
            UnsignedLong.valueOf(0), // CLIENT_INIT_TIMESTAMP
            Arrays.asList(), // REPLACED_URLS_KEYS
            Arrays.asList(), // REPLACED_URLS_VALUES
            Parser.SENTRY, // PARSER
            UnsignedLong.valueOf("0"), // ICOOKIE
            false, // SILENT
            "vla", // DC
            0d, // COORDINATES_LATITUDE
            0d, // COORDINATES_LONGITUDE
            0, // COORDINATES_PRECISION
            0L, // COORDINATES_TIMESTAMP
            "orloffv-dev.sas.yp-c.yandex.net", // HOST
            checker.getLogBrokerTopic() // TOPIC
        );
    }

    @Test
    public void minimal() throws Exception {
        String line = "{\"timestamp\":\"2021-11-02T12:01:45.579702Z\"}";
        UnsignedLong hashOfEmptyString = UnsignedLong.valueOf("2202906307356721367");

        checker.check(
            line,
            new Date(1635854505000L),
            "unknown-sentry-project", // PROJECT
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
            "", // BfROWSER_ENGINE_VERSION
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
            UnsignedLong.valueOf("0"), // YANDEXUID
            Arrays.asList(), // KV_KEYS
            Arrays.asList(), // KV_VALUES
            false, // IS_INTERNAL
            "", // CDN
            "", // IPv6
            false, // LOGGED_IN
            "Empty error", // MESSAGE
            UnsignedLong.valueOf("1376810649030035901"), // MESSAGE_ID
            "Empty error", // ORIGINAL_MESSAGE
            Arrays.asList(), // ERROR_URLS_KEYS
            Arrays.asList(), // ERROR_URLS_VALUES
            Runtime.UNKNOWN, // RUNTIME
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
            Parser.SENTRY, // PARSER
            UnsignedLong.valueOf("0"), // ICOOKIE
            false, // SILENT
            "", // DC
            0d, // COORDINATES_LATITUDE
            0d, // COORDINATES_LONGITUDE
            0, // COORDINATES_PRECISION
            0L, // COORDINATES_TIMESTAMP
            "hostname.test", // HOST
            checker.getLogBrokerTopic() // TOPIC
        );
    }

    @Test
    public void goParser() throws Exception {
        String line = "{\"breadcrumbs\":[{\"category\":\"auth\",\"message\":\"Authenticated user test@yandex-team" +
            ".ru\",\"level\":\"info\",\"timestamp\":\"2021-11-03T15:38:20.549432413+03:00\"}]," +
            "\"contexts\":{\"character\":{\"age\":19,\"attack_type\":\"melee\",\"name\":\"Mighty Fighter\"}," +
            "\"device\":{\"arch\":\"amd64\",\"num_cpu\":64},\"os\":{\"name\":\"linux\"}," +
            "\"runtime\":{\"go_maxprocs\":64,\"go_numcgocalls\":1,\"go_numroutines\":2,\"name\":\"go\"," +
            "\"version\":\"go1.15.2\"}},\"event_id\":\"28e38ec0f07749e6a9c8b4e264197bb6\",\"level\":\"error\"," +
            "\"platform\":\"go\",\"release\":\"my-project-name@1.0.0\",\"sdk\":{\"name\":\"sentry.go\"," +
            "\"version\":\"0.11.0\",\"integrations\":[\"ContextifyFrames\",\"Environment\",\"IgnoreErrors\"," +
            "\"Modules\"],\"packages\":[{\"name\":\"sentry-go\",\"version\":\"0.11.0\"}]}," +
            "\"server_name\":\"orloffv-dev.sas.yp-c.yandex.net\",\"tags\":{\"block\":\"block\",\"dc\":\"vla\"," +
            "\"experiments\":\"aaa=1;bb=7;ccc=yes;ddd\",\"isAdblock\":\"true\",\"isInternal\":\"true\"," +
            "\"isRobot\":\"true\",\"loggedin\":\"true\",\"method\":\"method\",\"my_tag\":\"my_tag_value\"," +
            "\"page\":\"custom_page\",\"platform\":\"desktop\",\"project\":\"python_sentry\",\"region\":\"199\"," +
            "\"reqid\":\"request-id-1234\",\"service\":\"python_service\",\"slots\":\"123123,12,12;5555,3,1\"," +
            "\"source\":\"source\",\"sourceMethod\":\"sourceMethod\",\"sourceType\":\"sourceType\"," +
            "\"url\":\"https://yandex.com/python\",\"useragent\":\"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3826.0 Safari/537.36\"}," +
            "\"user\":{\"id\":\"12345678890\",\"ip_address\":\"192.168.1.1\"},\"exception\":[{\"type\":\"syscall" +
            ".Errno\",\"value\":\"no such file or directory\"},{\"type\":\"*os.PathError\",\"value\":\"open filename" +
            ".ext: no such file or directory\",\"stacktrace\":{\"frames\":[{\"function\":\"main\"," +
            "\"module\":\"main\",\"abs_path\":\"/home/orloffv/go_projects/src/hello/hello.go\",\"lineno\":68," +
            "\"pre_context\":[\"    fmt.Printf(\\\"Hello, you have successfully installed GoLang in Linux\\\\n\\\")" +
            "\",\"\\tsentry.CaptureMessage(\\\"It works!\\\")\",\"\\t\",\"\\tfile, err := os.Open(\\\"filename" +
            ".ext\\\")\",\"\\tif err != nil {\"],\"context_line\":\"\\t\\tsentry.CaptureException(err)\"," +
            "\"post_context\":[\"\\t}\",\"\\tdefer file.Close()                      // закрываем файл\",\"    fmt" +
            ".Println(file.Name()) \",\"}\",\"\"],\"in_app\":true}]}}],\"timestamp\":\"2021-11-03T15:38:20" +
            ".550189269+03:00\"}";
        UnsignedLong hashOfEmptyString = UnsignedLong.valueOf("2202906307356721367");

        checker.check(
            line,
            new Date(1635943100000L),
            "python_sentry", // PROJECT
            "python_service", // SERVICE
            "custom_page", // PAGE
            Platform.DESKTOP, // PLATFORM
            "https://yandex.com/python", // URL
            UnsignedLong.valueOf("11051605148223342864"), // URL_ID
            "yandex.com", // VHOST
            Environment.UNKNOWN, // ENVIRONMENT
            Arrays.asList(5555, 123123), // TEST_IDS
            Arrays.asList("aaa=1", "bb=7", "ccc=yes", "ddd"), // EXP_FLAGS
            "WebKit", // BROWSER_ENGINE
            "537.36", // BfROWSER_ENGINE_VERSION
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
            "my-project-name@1.0.0", // VERSION
            199, // REGION
            "request-id-1234", // REQUEST_ID
            UnsignedLong.valueOf("1233527196171568231"), // REQUEST_ID_HASH
            UnsignedLong.valueOf("12345678890"), // YANDEXUID
            Arrays.asList("my_tag", "breadcrumbs", "contexts"), // KV_KEYS
            Arrays.asList("my_tag_value", "[{\"category\":\"auth\",\"message\":\"Authenticated user test@yandex-team" +
                ".ru\",\"level\":\"info\",\"timestamp\":\"2021-11-03T15:38:20.549432413+03:00\"}]", "{\"character" +
                "\":{\"age\":19,\"attack_type\":\"melee\",\"name\":\"Mighty Fighter\"}," +
                "\"device\":{\"arch\":\"amd64\",\"num_cpu\":64},\"os\":{\"name\":\"linux\"}," +
                "\"runtime\":{\"go_maxprocs\":64,\"go_numcgocalls\":1,\"go_numroutines\":2,\"name\":\"go\"," +
                "\"version\":\"go1.15.2\"}}"), // KV_VALUES
            true, // IS_INTERNAL
            "", // CDN
            "::ffff:192.168.1.1", // IPv6
            true, // LOGGED_IN
            "syscall.Errno: no such file or directory", // MESSAGE
            UnsignedLong.valueOf("3171165530419667501"), // MESSAGE_ID
            "syscall.Errno: no such file or directory", // ORIGINAL_MESSAGE
            Arrays.asList(), // ERROR_URLS_KEYS
            Arrays.asList(), // ERROR_URLS_VALUES
            Runtime.GO, // RUNTIME
            LogLevel.ERROR, // LEVEL
            "/home/orloffv/go_projects/src/hello/hello.go", // FILE
            UnsignedLong.valueOf("12174495796598305251"), // FILE_ID
            "block", // BLOCK
            "main", // METHOD
            0, // LINE
            0, // COL
            "syscall.Errno: no such file or directory\n" +
                "  File \"/home/orloffv/go_projects/src/hello/hello.go\", line 68, in main\n" +
                "    sentry.CaptureException(err)", // STACK_TRACE
            UnsignedLong.valueOf("17196683171257135900"), // STACK_TRACE_ID
            "syscall.Errno: no such file or directory\n" +
                "  File \"/home/orloffv/go_projects/src/hello/hello.go\", line 68, in main\n" +
                "    sentry.CaptureException(err)", // ORIGINAL_STACK_TRACE
            Arrays.asList("main"), // STACK_TRACE_NAMES
            Arrays.asList("/home/orloffv/go_projects/src/hello/hello.go"), // STACK_TRACE_URLS
            Arrays.asList(68), // STACK_TRACE_LINES
            Arrays.asList(0), // STACK_TRACE_COLS
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3826" +
                ".0 Safari/537.36", // USER_AGENT
            UnsignedLong.valueOf("1319564247142717470"), // USER_AGENT_ID
            "source", // SOURCE
            "sourceMethod", // SOURCE_METHOD
            "sourceType", // SOURCE_TYPE
            UnsignedLong.valueOf(0), // CLIENT_TIMESTAMP
            UnsignedLong.valueOf(0), // CLIENT_INIT_TIMESTAMP
            Arrays.asList(), // REPLACED_URLS_KEYS
            Arrays.asList(), // REPLACED_URLS_VALUES
            Parser.SENTRY, // PARSER
            UnsignedLong.valueOf("0"), // ICOOKIE
            false, // SILENT
            "vla", // DC
            0d, // COORDINATES_LATITUDE
            0d, // COORDINATES_LONGITUDE
            0, // COORDINATES_PRECISION
            0L, // COORDINATES_TIMESTAMP
            "orloffv-dev.sas.yp-c.yandex.net", // HOST
            checker.getLogBrokerTopic() // TOPIC
        );
    }

    @Test
    public void nodejsParser() throws Exception {
        String line = "{\"exception\":{\"values\":[{\"stacktrace\":{\"frames\":[{\"colno\":7," +
            "\"filename\":\"internal/timers.js\",\"function\":\"processTimers\",\"lineno\":492,\"in_app\":false," +
            "\"module\":\"timers\"},{\"colno\":17,\"filename\":\"internal/timers.js\",\"function\":\"listOnTimeout\"," +
            "\"lineno\":549,\"in_app\":false,\"module\":\"timers\"},{\"colno\":7," +
            "\"filename\":\"/home/orloffv/sentry/index.js\",\"function\":\"Timeout._onTimeout\",\"lineno\":55," +
            "\"in_app\":true,\"module\":\"index\",\"pre_context\":[\"const transaction = Sentry.startTransaction({\"," +
            "\"    op: \\\"test\\\",\",\"    name: \\\"My First Test Transaction\\\",\",\"  });\",\"  \",\"  " +
            "setTimeout(() => {\",\"    try {\"],\"context_line\":\"      foo();\",\"post_context\":[\"    } catch " +
            "(e) {\",\"      Sentry.captureException(e);\",\"    } finally {\",\"      transaction.finish();\",\"    " +
            "}\",\"  }, 99);\",\"\"]}]},\"type\":\"ReferenceError\",\"value\":\"foo is not defined\"," +
            "\"mechanism\":{\"type\":\"generic\",\"handled\":true}}]}," +
            "\"event_id\":\"e3ddc8cb70544c49b1c3f19f953318b8\",\"platform\":\"node\",\"timestamp\":1635952012.873," +
            "\"environment\":\"production\",\"sdk\":{\"integrations\":[\"InboundFilters\",\"FunctionToString\"," +
            "\"Console\",\"Http\",\"OnUncaughtException\",\"OnUnhandledRejection\",\"LinkedErrors\"]," +
            "\"name\":\"sentry.javascript.node\",\"version\":\"6.14.0\",\"packages\":[{\"name\":\"npm:@sentry/node\"," +
            "\"version\":\"6.14.0\"}]},\"tags\":{\"platform\":\"desktop\",\"project\":\"python_sentry\"," +
            "\"service\":\"python_service\",\"page\":\"custom_page\",\"url\":\"https://yandex.com/python\"," +
            "\"slots\":\"123123,12,12;5555,3,1\",\"dc\":\"vla\",\"reqid\":\"request-id-1234\"," +
            "\"experiments\":\"aaa=1;bb=7;ccc=yes;ddd\",\"isInternal\":\"true\",\"isRobot\":\"true\"," +
            "\"loggedin\":\"true\",\"isAdblock\":\"true\",\"region\":\"199\",\"useragent\":\"Mozilla/5.0 (Macintosh; " +
            "Intel Mac OS X 10_14_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3826.0 Safari/537.36\"," +
            "\"source\":\"source\",\"sourceMethod\":\"sourceMethod\",\"sourceType\":\"sourceType\"," +
            "\"block\":\"block\",\"method\":\"method\",\"my_tag\":\"my_tag_value\"}," +
            "\"user\":{\"yandexuid\":\"12345678890\",\"ip_address\":\"192.168.1.1\",\"puid\":\"12931231231\"}," +
            "\"contexts\":{\"character\":{\"name\":\"Mighty Fighter\",\"age\":19,\"attack_type\":\"melee\"}}," +
            "\"breadcrumbs\":[{\"timestamp\":1635952012.757,\"category\":\"auth\",\"message\":\"Authenticated user " +
            "test@yandex-team.ru\",\"level\":\"info\"}]}";
        UnsignedLong hashOfEmptyString = UnsignedLong.valueOf("2202906307356721367");

        checker.check(
            line,
            new Date(1635952012000L),
            "python_sentry", // PROJECT
            "python_service", // SERVICE
            "custom_page", // PAGE
            Platform.DESKTOP, // PLATFORM
            "https://yandex.com/python", // URL
            UnsignedLong.valueOf("11051605148223342864"), // URL_ID
            "yandex.com", // VHOST
            Environment.PRODUCTION, // ENVIRONMENT
            Arrays.asList(5555, 123123), // TEST_IDS
            Arrays.asList("aaa=1", "bb=7", "ccc=yes", "ddd"), // EXP_FLAGS
            "WebKit", // BROWSER_ENGINE
            "537.36", // BfROWSER_ENGINE_VERSION
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
            "", // VERSION
            199, // REGION
            "request-id-1234", // REQUEST_ID
            UnsignedLong.valueOf("1233527196171568231"), // REQUEST_ID_HASH
            UnsignedLong.valueOf("12345678890"), // YANDEXUID
            Arrays.asList("puid", "my_tag", "breadcrumbs", "contexts"), // KV_KEYS
            Arrays.asList("12931231231", "my_tag_value", "[{\"timestamp\":1635952012.757,\"category\":\"auth\"," +
                "\"message\":\"Authenticated user test@yandex-team.ru\",\"level\":\"info\"}]", "{\"character" +
                "\":{\"name\":\"Mighty Fighter\",\"age\":19,\"attack_type\":\"melee\"}}"), // KV_VALUES
            true, // IS_INTERNAL
            "", // CDN
            "::ffff:192.168.1.1", // IPv6
            true, // LOGGED_IN
            "ReferenceError: foo is not defined", // MESSAGE
            UnsignedLong.valueOf("15151035054703696826"), // MESSAGE_ID
            "ReferenceError: foo is not defined", // ORIGINAL_MESSAGE
            Arrays.asList(), // ERROR_URLS_KEYS
            Arrays.asList(), // ERROR_URLS_VALUES
            Runtime.NODEJS, // RUNTIME
            LogLevel.UNKNOWN, // LEVEL
            "internal/timers.js", // FILE
            UnsignedLong.valueOf("18084107859145354208"), // FILE_ID
            "block", // BLOCK
            "processTimers", // METHOD
            0, // LINE
            0, // COL
            "ReferenceError: foo is not defined\n" +
                "  File \"internal/timers.js\", line 492, in processTimers\n" +
                "  File \"internal/timers.js\", line 549, in listOnTimeout\n" +
                "  File \"/home/orloffv/sentry/index.js\", line 55, in Timeout._onTimeout\n" +
                "    foo();", // STACK_TRACE
            UnsignedLong.valueOf("8445923446443567015"), // STACK_TRACE_ID
            "ReferenceError: foo is not defined\n" +
                "  File \"internal/timers.js\", line 492, in processTimers\n" +
                "  File \"internal/timers.js\", line 549, in listOnTimeout\n" +
                "  File \"/home/orloffv/sentry/index.js\", line 55, in Timeout._onTimeout\n" +
                "    foo();", // ORIGINAL_STACK_TRACE
            Arrays.asList("processTimers", "listOnTimeout", "Timeout._onTimeout"), // STACK_TRACE_NAMES
            Arrays.asList("internal/timers.js", "internal/timers.js", "/home/orloffv/sentry/index.js"), //
            // STACK_TRACE_URLS
            Arrays.asList(492, 549, 55), // STACK_TRACE_LINES
            Arrays.asList(7, 17, 7), // STACK_TRACE_COLS
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3826" +
                ".0 Safari/537.36", // USER_AGENT
            UnsignedLong.valueOf("1319564247142717470"), // USER_AGENT_ID
            "source", // SOURCE
            "sourceMethod", // SOURCE_METHOD
            "sourceType", // SOURCE_TYPE
            UnsignedLong.valueOf(0), // CLIENT_TIMESTAMP
            UnsignedLong.valueOf(0), // CLIENT_INIT_TIMESTAMP
            Arrays.asList(), // REPLACED_URLS_KEYS
            Arrays.asList(), // REPLACED_URLS_VALUES
            Parser.SENTRY, // PARSER
            UnsignedLong.valueOf("0"), // ICOOKIE
            false, // SILENT
            "vla", // DC
            0d, // COORDINATES_LATITUDE
            0d, // COORDINATES_LONGITUDE
            0, // COORDINATES_PRECISION
            0L, // COORDINATES_TIMESTAMP
            "hostname.test", // HOST
            checker.getLogBrokerTopic() // TOPIC
        );
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void javaParser() throws Exception {
        String line = "{\"timestamp\":\"2021-11-08T09:09:25.260Z\",\"message\":{\"formatted\":\"Caught exception!\"," +
            "\"message\":\"Caught exception!\"},\"logger\":\"sentry.example.log4j2.Application\"," +
            "\"exception\":{\"values\":[{\"type\":\"ArithmeticException\",\"value\":\"/ by zero\",\"module\":\"java" +
            ".lang\",\"thread_id\":30,\"stacktrace\":{\"frames\":[{\"filename\":\"Thread.java\",\"function\":\"run\"," +
            "\"module\":\"java.lang.Thread\",\"lineno\":833,\"in_app\":false,\"native\":false}," +
            "{\"filename\":\"ExecJavaMojo.java\",\"function\":\"run\",\"module\":\"org.codehaus.mojo.exec" +
            ".ExecJavaMojo$1\",\"lineno\":293,\"in_app\":false,\"native\":false},{\"filename\":\"Method.java\"," +
            "\"function\":\"invoke\",\"module\":\"java.lang.reflect.Method\",\"lineno\":568,\"in_app\":false," +
            "\"native\":false},{\"filename\":\"DelegatingMethodAccessorImpl.java\",\"function\":\"invoke\"," +
            "\"module\":\"jdk.internal.reflect.DelegatingMethodAccessorImpl\",\"lineno\":43,\"in_app\":false," +
            "\"native\":false},{\"filename\":\"NativeMethodAccessorImpl.java\",\"function\":\"invoke\"," +
            "\"module\":\"jdk.internal.reflect.NativeMethodAccessorImpl\",\"lineno\":77,\"in_app\":false," +
            "\"native\":false},{\"filename\":\"NativeMethodAccessorImpl.java\",\"function\":\"invoke0\"," +
            "\"module\":\"jdk.internal.reflect.NativeMethodAccessorImpl\",\"in_app\":false,\"native\":true}," +
            "{\"filename\":\"Application.java\",\"function\":\"main\",\"module\":\"sentry.example.log4j2" +
            ".Application\",\"lineno\":67,\"in_app\":false,\"native\":false}]}}]},\"level\":\"error\"," +
            "\"event_id\":\"85af8b13790a46aeb2ee74231beac2db\",\"contexts\":{\"attack_type\":{\"value\":\"melee\"}," +
            "\"name\":{\"value\":\"Mighty Fighter\"},\"Context Data\":{\"extra_key\":\"extra_value\"}," +
            "\"age\":{\"value\":19}},\"sdk\":{\"name\":\"sentry.java.log4j2\",\"version\":\"5.0.1\"," +
            "\"packages\":[{\"name\":\"maven:io.sentry:sentry\",\"version\":\"5.0.1\"},{\"name\":\"maven:io" +
            ".sentry:sentry-log4j2\",\"version\":\"5.0.1\"}]},\"tags\":{\"isRobot\":\"true\",\"method\":\"method\"," +
            "\"experiments\":\"aaa=1;bb=7;ccc=yes;ddd\",\"useragent\":\"Mozilla/5.0 (Macintosh; Intel Mac OS X " +
            "10_14_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3826.0 Safari/537.36\"," +
            "\"project\":\"python_sentry\",\"sourceMethod\":\"sourceMethod\",\"isAdblock\":\"true\"," +
            "\"source\":\"source\",\"platform\":\"desktop\",\"url\":\"https://yandex.com/python\"," +
            "\"reqid\":\"request-id-1234\",\"isInternal\":\"true\",\"my_tag\":\"my_tag_value\",\"slots\":\"123123,12," +
            "12;5555,3,1\",\"sourceType\":\"sourceType\",\"service\":\"python_service\",\"loggedin\":\"true\"," +
            "\"block\":\"block\",\"page\":\"custom_page\",\"region\":\"199\",\"dc\":\"vla\"}," +
            "\"environment\":\"production\",\"platform\":\"java\",\"user\":{\"email\":\"jane.doe@example.com\"," +
            "\"id\":\"12345678890\",\"ip_address\":\"192.168.1.1\"},\"server_name\":\"orloffv-dev.sas.yp-c.yandex" +
            ".net\",\"breadcrumbs\":[{\"timestamp\":\"2021-11-08T09:09:25.240Z\",\"message\":\"Authenticated user " +
            "test@yandex-team.ru\",\"category\":\"auth\",\"level\":\"info\"},{\"timestamp\":\"2021-11-08T09:09:25" +
            ".245Z\",\"message\":\"Debug message\",\"category\":\"sentry.example.log4j2.Application\"," +
            "\"level\":\"debug\"},{\"timestamp\":\"2021-11-08T09:09:25.247Z\",\"message\":\"Info message\"," +
            "\"category\":\"sentry.example.log4j2.Application\",\"level\":\"info\"}," +
            "{\"timestamp\":\"2021-11-08T09:09:25.259Z\",\"message\":\"Warn message\",\"category\":\"sentry.example" +
            ".log4j2.Application\",\"level\":\"warning\"}],\"extra\":{\"thread_name\":\"sentry.example.log4j2" +
            ".Application.main()\"}}";
        UnsignedLong hashOfEmptyString = UnsignedLong.valueOf("2202906307356721367");

        checker.check(
            line,
            new Date(1636362565000L),
            "python_sentry", // PROJECT
            "python_service", // SERVICE
            "custom_page", // PAGE
            Platform.DESKTOP, // PLATFORM
            "https://yandex.com/python", // URL
            UnsignedLong.valueOf("11051605148223342864"), // URL_ID
            "yandex.com", // VHOST
            Environment.PRODUCTION, // ENVIRONMENT
            Arrays.asList(5555, 123123), // TEST_IDS
            Arrays.asList("aaa=1", "bb=7", "ccc=yes", "ddd"), // EXP_FLAGS
            "WebKit", // BROWSER_ENGINE
            "537.36", // BfROWSER_ENGINE_VERSION
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
            "", // VERSION
            199, // REGION
            "request-id-1234", // REQUEST_ID
            UnsignedLong.valueOf("1233527196171568231"), // REQUEST_ID_HASH
            UnsignedLong.valueOf("12345678890"), // YANDEXUID
            Arrays.asList("my_tag", "breadcrumbs", "contexts", "extra", "logger"), // KV_KEYS
            Arrays.asList("my_tag_value", "[{\"timestamp\":\"2021-11-08T09:09:25.240Z\",\"message\":\"Authenticated " +
                "user test@yandex-team.ru\",\"category\":\"auth\",\"level\":\"info\"}," +
                "{\"timestamp\":\"2021-11-08T09:09:25.245Z\",\"message\":\"Debug message\",\"category\":\"sentry" +
                ".example.log4j2.Application\",\"level\":\"debug\"},{\"timestamp\":\"2021-11-08T09:09:25.247Z\"," +
                "\"message\":\"Info message\",\"category\":\"sentry.example.log4j2.Application\",\"level\":\"info\"}," +
                "{\"timestamp\":\"2021-11-08T09:09:25.259Z\",\"message\":\"Warn message\",\"category\":\"sentry" +
                ".example.log4j2.Application\",\"level\":\"warning\"}]", "{\"attack_type\":{\"value\":\"melee\"}," +
                "\"name\":{\"value\":\"Mighty Fighter\"},\"Context Data\":{\"extra_key\":\"extra_value\"}," +
                "\"age\":{\"value\":19}}", "{\"thread_name\":\"sentry.example.log4j2.Application.main()\"}", "sentry" +
                ".example.log4j2.Application"), // KV_VALUES
            true, // IS_INTERNAL
            "", // CDN
            "::ffff:192.168.1.1", // IPv6
            true, // LOGGED_IN
            "java.lang.ArithmeticException: / by zero", // MESSAGE
            UnsignedLong.valueOf("15438152985576424412"), // MESSAGE_ID
            "java.lang.ArithmeticException: / by zero", // ORIGINAL_MESSAGE
            Arrays.asList(), // ERROR_URLS_KEYS
            Arrays.asList(), // ERROR_URLS_VALUES
            Runtime.JAVA, // RUNTIME
            LogLevel.ERROR, // LEVEL
            "Thread.java", // FILE
            UnsignedLong.valueOf("15021819951854981369"), // FILE_ID
            "block", // BLOCK
            "run", // METHOD
            0, // LINE
            0, // COL
            "java.lang.ArithmeticException: / by zero\n" +
                "  File \"Thread.java\", line 833, in run\n" +
                "  File \"ExecJavaMojo.java\", line 293, in run\n" +
                "  File \"Method.java\", line 568, in invoke\n" +
                "  File \"DelegatingMethodAccessorImpl.java\", line 43, in invoke\n" +
                "  File \"NativeMethodAccessorImpl.java\", line 77, in invoke\n" +
                "  File \"NativeMethodAccessorImpl.java\", line 0, in invoke0\n" +
                "  File \"Application.java\", line 67, in main", // STACK_TRACE
            UnsignedLong.valueOf("13868613666384361017"), // STACK_TRACE_ID
            "java.lang.ArithmeticException: / by zero\n" +
                "  File \"Thread.java\", line 833, in run\n" +
                "  File \"ExecJavaMojo.java\", line 293, in run\n" +
                "  File \"Method.java\", line 568, in invoke\n" +
                "  File \"DelegatingMethodAccessorImpl.java\", line 43, in invoke\n" +
                "  File \"NativeMethodAccessorImpl.java\", line 77, in invoke\n" +
                "  File \"NativeMethodAccessorImpl.java\", line 0, in invoke0\n" +
                "  File \"Application.java\", line 67, in main", // ORIGINAL_STACK_TRACE
            Arrays.asList("run", "run", "invoke", "invoke", "invoke", "invoke0", "main"), // STACK_TRACE_NAMES
            Arrays.asList("Thread.java", "ExecJavaMojo.java", "Method.java", "DelegatingMethodAccessorImpl.java",
                "NativeMethodAccessorImpl.java", "NativeMethodAccessorImpl.java", "Application.java"), //
            // STACK_TRACE_URLS
            Arrays.asList(833, 293, 568, 43, 77, 0, 67), // STACK_TRACE_LINES
            Arrays.asList(0, 0, 0, 0, 0, 0, 0), // STACK_TRACE_COLS
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3826" +
                ".0 Safari/537.36", // USER_AGENT
            UnsignedLong.valueOf("1319564247142717470"), // USER_AGENT_ID
            "source", // SOURCE
            "sourceMethod", // SOURCE_METHOD
            "sourceType", // SOURCE_TYPE
            UnsignedLong.valueOf(0), // CLIENT_TIMESTAMP
            UnsignedLong.valueOf(0), // CLIENT_INIT_TIMESTAMP
            Arrays.asList(), // REPLACED_URLS_KEYS
            Arrays.asList(), // REPLACED_URLS_VALUES
            Parser.SENTRY, // PARSER
            UnsignedLong.valueOf("0"), // ICOOKIE
            false, // SILENT
            "vla", // DC
            0d, // COORDINATES_LATITUDE
            0d, // COORDINATES_LONGITUDE
            0, // COORDINATES_PRECISION
            0L, // COORDINATES_TIMESTAMP
            "orloffv-dev.sas.yp-c.yandex.net", // HOST
            checker.getLogBrokerTopic() // TOPIC
        );
    }

    @Test
    public void skipWithoutProject() throws Exception {
        String line = "{}";

        checker.checkEmpty(line);
    }

    @Test
    public void skipWithoutTimestamp() throws Exception {
        String line = "{\"timestamp\":0}";

        checker.checkEmpty(line);
    }

    @Test
    public void skipWithoutTimestamp2() throws Exception {
        String line = "{\"content_type\":\"application/json\",\"type\":\"event\",\"length\":3063}";

        checker.checkEmpty(line);
    }

    @Test
    public void skipWithoutTimestamp3() throws Exception {
        String line = "{\"event_id\":\"85af8b13790a46aeb2ee74231beac2db\",\"sdk\":{\"name\":\"sentry.java.log4j2\"," +
            "\"version\":\"5.0.1\",\"packages\":[{\"name\":\"maven:io.sentry:sentry\",\"version\":\"5.0.1\"}," +
            "{\"name\":\"maven:io.sentry:sentry-log4j2\",\"version\":\"5.0.1\"}]}}";

        checker.checkEmpty(line);
    }
}
