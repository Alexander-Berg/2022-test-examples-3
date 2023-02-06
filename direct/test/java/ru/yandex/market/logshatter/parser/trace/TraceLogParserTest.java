package ru.yandex.market.logshatter.parser.trace;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.logshatter.parser.CustomAssertion;
import ru.yandex.market.logshatter.parser.EnvironmentMapper;
import ru.yandex.market.logshatter.parser.LogParserChecker;
import ru.yandex.market.logshatter.url.Page;
import ru.yandex.market.logshatter.url.PageMatcher;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import static ru.yandex.market.logshatter.parser.LogParserChecker.arraysToMap;
import static ru.yandex.market.logshatter.parser.trace.TraceLogParserTest.Indices.DURATION_MS;
import static ru.yandex.market.logshatter.parser.trace.TraceLogParserTest.Indices.END_TIME_MS;
import static ru.yandex.market.logshatter.parser.trace.TraceLogParserTest.Indices.ENVIRONMENT;
import static ru.yandex.market.logshatter.parser.trace.TraceLogParserTest.Indices.ERROR_CODE;
import static ru.yandex.market.logshatter.parser.trace.TraceLogParserTest.Indices.EVENTS_NAMES;
import static ru.yandex.market.logshatter.parser.trace.TraceLogParserTest.Indices.EVENTS_TIMESTAMPS;
import static ru.yandex.market.logshatter.parser.trace.TraceLogParserTest.Indices.HOST;
import static ru.yandex.market.logshatter.parser.trace.TraceLogParserTest.Indices.HTTP_CODE;
import static ru.yandex.market.logshatter.parser.trace.TraceLogParserTest.Indices.HTTP_METHOD;
import static ru.yandex.market.logshatter.parser.trace.TraceLogParserTest.Indices.ID_HASH;
import static ru.yandex.market.logshatter.parser.trace.TraceLogParserTest.Indices.ID_MS;
import static ru.yandex.market.logshatter.parser.trace.TraceLogParserTest.Indices.ID_SUBREQUESTS;
import static ru.yandex.market.logshatter.parser.trace.TraceLogParserTest.Indices.KV_KEYS;
import static ru.yandex.market.logshatter.parser.trace.TraceLogParserTest.Indices.KV_VALUES;
import static ru.yandex.market.logshatter.parser.trace.TraceLogParserTest.Indices.MODULE;
import static ru.yandex.market.logshatter.parser.trace.TraceLogParserTest.Indices.PAGE_ID;
import static ru.yandex.market.logshatter.parser.trace.TraceLogParserTest.Indices.PAGE_TYPE;
import static ru.yandex.market.logshatter.parser.trace.TraceLogParserTest.Indices.PROTOCOL;
import static ru.yandex.market.logshatter.parser.trace.TraceLogParserTest.Indices.QUERY_PARAMS;
import static ru.yandex.market.logshatter.parser.trace.TraceLogParserTest.Indices.REQUEST_METHOD;
import static ru.yandex.market.logshatter.parser.trace.TraceLogParserTest.Indices.RESPONSE_SIZE_BYTES;
import static ru.yandex.market.logshatter.parser.trace.TraceLogParserTest.Indices.RETRY_NUM;
import static ru.yandex.market.logshatter.parser.trace.TraceLogParserTest.Indices.SOURCE_HOST;
import static ru.yandex.market.logshatter.parser.trace.TraceLogParserTest.Indices.SOURCE_MODULE;
import static ru.yandex.market.logshatter.parser.trace.TraceLogParserTest.Indices.START_TIME_MS;
import static ru.yandex.market.logshatter.parser.trace.TraceLogParserTest.Indices.TARGET_HOST;
import static ru.yandex.market.logshatter.parser.trace.TraceLogParserTest.Indices.TARGET_MODULE;
import static ru.yandex.market.logshatter.parser.trace.TraceLogParserTest.Indices.TEST_IDS;
import static ru.yandex.market.logshatter.parser.trace.TraceLogParserTest.Indices.TYPE;
import static ru.yandex.market.logshatter.parser.trace.TraceLogParserTest.Indices.YANDEX_LOGIN;
import static ru.yandex.market.logshatter.parser.trace.TraceLogParserTest.Indices.YANDEX_UID;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 19.08.16
 */
public class TraceLogParserTest {
    private static final String LOG_LINE = "tskv" +
        "\tdate=2016-08-19T15:54:35.476+03:00" +
        "\trequest_id=1471611275470/6b1f4847187863f0cae3a44b48fd1b3f/4/5/3" +
        "\ttime_millis=2";

    private final Date LOG_LINE_DATE = new Date(1471611275470L);
    private final Date LOG_LINE_DATE_ALTERNATIVE = new Date(1542621750230L);

    private final LogParserChecker checker = new LogParserChecker(new TraceLogParser());

    private final Object[] LOG_LINE_FIELDS = {
        1471611275470L,
        "6b1f4847187863f0cae3a44b48fd1b3f",
        new Integer[]{4, 5, 3},
        1471611275474L, // start_time = end_time - duration
        1471611275476L, // end_time = ZonedDateTime.parse("2016-08-19T15:54:35.476+03:00").toInstant().toEpochMilli()
        2, // duration
        RequestType.OUT,
        "",
        checker.getHost(),
        "",
        checker.getHost(),
        "",
        "",
        Environment.DEVELOPMENT,
        "",
        -1,
        1,
        "",
        "",
        "",
        "",
        "",
        "",
        new String[]{},
        new String[]{},
        new String[]{},
        new Long[]{},
        new Integer[]{},
        "",
        "",
        -1
    };

    public TraceLogParserTest() throws ParseException {
        checker.setParam(EnvironmentMapper.LOGBROKER_PROTOCOL_PREFIX + checker.getOrigin(), "DEVELOPMENT");
    }

    @Test
    public void testParseOptional() throws Exception {
        checker.check(LOG_LINE, LOG_LINE_DATE, LOG_LINE_FIELDS);
    }

    @Test
    public void testParse() throws Exception {
        String line = LOG_LINE +
            "\ttype=OUT" +
            "\ttarget_module=market_kgb_buker" +
            "\ttarget_host=marketbuker.yandex.ru" +
            "\tprotocol=http" +
            "\thttp_method=GET" +
            "\tquery_params=/buker/GetCards?collection=facts&ids=12518941" +
            "\tretry_num=1" +
            "\thttp_code=404" +
            "\tkv.clientId=2532" +
            "\tkv.contentApiRequestId=01e08229ee825b6" +
            "\tkv.clientApp=Internal client" +
            "\tevent.parsing=1470064678042" +
            "\tevent.extraction=1470064678043" +
            "\trequest_method=getOffers" +
            "\terror_code=CONNECTION_TIMEOUT" +
            "\tyandex_uid=fnldjcfrhn723o8bfgv623bvr23" +
            "\tyandex_login=algebraic" +
            "\ttest_ids=4324,532523,53252352" +
            "\tresponse_size_bytes=42";

        CustomAssertion keyValueAssertion = new CustomAssertion() {
            @Override
            public void doAssertion(Object[] expected, Object[] actual) {
                Map<Object, Object> expectedMap = arraysToMap((Object[]) expected[0], (Object[]) expected[1]);
                Map<Object, Object> actualMap = arraysToMap((Object[]) actual[0], (Object[]) actual[1]);
                Assert.assertEquals(expectedMap.entrySet(), actualMap.entrySet());
            }
        };

        checker.setCustomAssertion(new int[]{KV_KEYS, KV_VALUES}, keyValueAssertion);
        checker.setCustomAssertion(new int[]{EVENTS_NAMES, EVENTS_TIMESTAMPS}, keyValueAssertion);

        Object[] expectedFields = Arrays.copyOf(LOG_LINE_FIELDS, LOG_LINE_FIELDS.length);
        expectedFields[SOURCE_HOST] = checker.getHost();
        expectedFields[TARGET_MODULE] = "market_kgb_buker";
        expectedFields[TARGET_HOST] = "marketbuker.yandex.ru";
        expectedFields[HTTP_CODE] = 404;
        expectedFields[RETRY_NUM] = 1;
        expectedFields[PROTOCOL] = "http";
        expectedFields[HTTP_METHOD] = "GET";
        expectedFields[QUERY_PARAMS] = "/buker/GetCards?collection=facts&ids=12518941";
        expectedFields[KV_KEYS] = new String[]{"clientId", "contentApiRequestId", "clientApp"};
        expectedFields[KV_VALUES] = new String[]{"2532", "01e08229ee825b6", "Internal client"};
        expectedFields[EVENTS_NAMES] = new String[]{"parsing", "extraction"};
        expectedFields[EVENTS_TIMESTAMPS] = new Long[]{1470064678042L, 1470064678043L};
        expectedFields[REQUEST_METHOD] = "getOffers";
        expectedFields[ERROR_CODE] = "CONNECTION_TIMEOUT";
        expectedFields[YANDEX_UID] = "fnldjcfrhn723o8bfgv623bvr23";
        expectedFields[YANDEX_LOGIN] = "algebraic";
        expectedFields[TEST_IDS] = new Integer[]{4324, 532523, 53252352};
        expectedFields[RESPONSE_SIZE_BYTES] = 42;

        checker.check(line, LOG_LINE_DATE, expectedFields);
    }

    @Test
    public void testParseAlternative() throws Exception {
        String line =
            "tskv" +
            "\ttskv_format=trace-log" +
            "\tdate=2018-11-19T13:02:31.635186+0300" +
            "\trequest_id=1542621750230/83dcb2bddf6b24b5750074346e3b7d3f/13/10/1" +
            "\tsource_module=market-report" +
            "\tsource_host=iva1-0986-afa-iva-market-prod--0ec-17050.gencfg-c.yandex.net" +
            "\ttarget_module=delivery-calc" +
            "\ttarget_host=delicalc.report.mbi.vs.market.yandex.net:30012" +
            "\trequest_method=/feedOffers" +
            "\thttp_code=501" +
            "\tretry_num=3" +
            "\tduration_ms=17" +
            "\terror_code=DeliveryCalc response error (ext. type=PROTOCOL_ERROR code=501): market/report/src/external_requester.cpp:688: HTTP request to full://localhost:37716/feedOffers failed. Error:request failed(HTTP/1.1 501 Not Implemented) 17 ms elapsed" +
            "\tprotocol=http" +
            "\thttp_method=GET" +
            "\tkv.in.weight=4.5" +
            "\tkv.in.feed_id=-1" +
            "\tkv.in.program_type=4" +
            "\tkv.in.raw_message=U05BUEMAAABPJERDT1JHAAAAEP8RASgBGO2vDiIzCgAhAAUBCBJAKQUIDAA8QDEJCQg+QDkFCUiAQUBIBGj///////////8BKJEB" +
            "\tkv.in.height=30" +
            "\tkv.in.length=35" +
            "\tkv.saved_response=false" +
            "\tkv.in.generation_id=235501" +
            "\tkv.in.width=28" +
            "\tkv.in.warehouse_id=145" +
            "\tkv.out.post_bucket_ids=[243,244]" +
            "\tkv.out.delivery_opt_bucket_ids=[17736,17737]" +
            "\tkv.in.cargo_types=[-1]" +
            "\tkv.out.pickup_bucket_ids=[314,315]" +
            "\ttype=OUT" +
            "\tquery_params=";

        CustomAssertion keyValueAssertion = new CustomAssertion() {
            @Override
            public void doAssertion(Object[] expected, Object[] actual) {
                Map<Object, Object> expectedMap = arraysToMap((Object[]) expected[0], (Object[]) expected[1]);
                Map<Object, Object> actualMap = arraysToMap((Object[]) actual[0], (Object[]) actual[1]);
                Assert.assertEquals(expectedMap.entrySet(), actualMap.entrySet());
            }
        };

        checker.setCustomAssertion(new int[]{KV_KEYS, KV_VALUES}, keyValueAssertion);
        checker.setCustomAssertion(new int[]{EVENTS_NAMES, EVENTS_TIMESTAMPS}, keyValueAssertion);

        Object[] expectedFields = Arrays.copyOf(LOG_LINE_FIELDS, LOG_LINE_FIELDS.length);

        expectedFields[ID_MS] = 1542621750230L;
        expectedFields[ID_HASH] = "83dcb2bddf6b24b5750074346e3b7d3f";
        expectedFields[ID_SUBREQUESTS] = new Integer[]{13, 10, 1};
        expectedFields[START_TIME_MS] = 1542621751618L;
        expectedFields[END_TIME_MS] = 1542621751635L;
        expectedFields[DURATION_MS] = 17;
        expectedFields[TYPE] = RequestType.OUT;
        expectedFields[SOURCE_MODULE] = "market-report";
        expectedFields[SOURCE_HOST] = "iva1-0986-afa-iva-market-prod--0ec-17050.gencfg-c.yandex.net";
        expectedFields[TARGET_MODULE] = "delivery-calc";
        expectedFields[TARGET_HOST] = "delicalc.report.mbi.vs.market.yandex.net:30012";
        expectedFields[REQUEST_METHOD] = "/feedOffers";
        expectedFields[HTTP_CODE] = 501;
        expectedFields[RETRY_NUM] = 3;
        expectedFields[ERROR_CODE] = "DeliveryCalc response error (ext. type=PROTOCOL_ERROR code=501): market/report/src/external_requester.cpp:688: HTTP request to full://localhost:37716/feedOffers failed. Error:request failed(HTTP/1.1 501 Not Implemented) 17 ms elapsed";
        expectedFields[PROTOCOL] = "http";
        expectedFields[HTTP_METHOD] = "GET";
        expectedFields[QUERY_PARAMS] = "";
        expectedFields[YANDEX_UID] = "";
        expectedFields[YANDEX_LOGIN] = "";
        expectedFields[KV_KEYS] = new String[]{"in.weight", "in.feed_id", "in.program_type", "in.raw_message", "in.height", "in.length", "saved_response", "in.generation_id",
                                               "in.width", "in.warehouse_id", "out.post_bucket_ids", "out.delivery_opt_bucket_ids", "in.cargo_types", "out.pickup_bucket_ids"};
        expectedFields[KV_VALUES] = new String[]{"4.5", "-1", "4", "U05BUEMAAABPJERDT1JHAAAAEP8RASgBGO2vDiIzCgAhAAUBCBJAKQUIDAA8QDEJCQg+QDkFCUiAQUBIBGj///////////8BKJEB",
                                                 "30", "35", "false", "235501", "28", "145", "[243,244]", "[17736,17737]", "[-1]", "[314,315]"};
        expectedFields[EVENTS_NAMES] = new String[]{};
        expectedFields[EVENTS_TIMESTAMPS] = new Long[]{};
        expectedFields[TEST_IDS] = new Integer[]{};
        expectedFields[PAGE_ID] = "";
        expectedFields[PAGE_TYPE] = "";

        checker.check(line, LOG_LINE_DATE_ALTERNATIVE, expectedFields);
    }

    @Test
    public void testParseSourceHostAndModuleDefaultValues() throws Exception {
        String moduleName = "market-api";
        String host = "market.yandex.ru";
        String fileName = String.format("%s-request-trace.log", moduleName);

        checker.setFile(fileName);
        checker.setHost(host);

        Object[] expectedFields = Arrays.copyOf(LOG_LINE_FIELDS, LOG_LINE_FIELDS.length);
        expectedFields[MODULE] = moduleName;
        expectedFields[HOST] = host;
        expectedFields[SOURCE_MODULE] = moduleName;
        expectedFields[SOURCE_HOST] = host;

        checker.check(LOG_LINE, LOG_LINE_DATE, expectedFields);
    }

    @Test
    public void testParseModuleName() throws Exception {
        String moduleName = "market-api";
        String fileName = String.format("%s-trace.log", moduleName);
        checker.setFile(fileName);

        Object[] expectedFields = Arrays.copyOf(LOG_LINE_FIELDS, LOG_LINE_FIELDS.length);
        expectedFields[MODULE] = moduleName;
        expectedFields[SOURCE_MODULE] = moduleName;

        checker.check(LOG_LINE, LOG_LINE_DATE, expectedFields);
    }

    @Test
    public void testParseTargetHostAndModuleDefaultValues() throws Exception {
        String moduleName = "market-api";
        String host = "market.yandex.ru";
        String fileName = String.format("%s-request-trace.log", moduleName);

        checker.setFile(fileName);
        checker.setHost(host);

        Object[] expectedFields = Arrays.copyOf(LOG_LINE_FIELDS, LOG_LINE_FIELDS.length);
        expectedFields[TYPE] = RequestType.IN;
        expectedFields[HOST] = host;
        expectedFields[MODULE] = moduleName;
        expectedFields[SOURCE_HOST] = "";
        expectedFields[TARGET_MODULE] = moduleName;
        expectedFields[TARGET_HOST] = host;

        checker.check(LOG_LINE + "\ttype=IN", LOG_LINE_DATE, expectedFields);
    }

    @Test
    public void testParseSourceEnvironment() throws Exception {
        checker.setOrigin("market-health-stable");
        checker.setParam(EnvironmentMapper.LOGBROKER_PROTOCOL_PREFIX + checker.getOrigin(), "PRODUCTION");

        Object[] expectedFields = Arrays.copyOf(LOG_LINE_FIELDS, LOG_LINE_FIELDS.length);
        expectedFields[ENVIRONMENT] = Environment.PRODUCTION;

        checker.check(LOG_LINE, LOG_LINE_DATE, expectedFields);
    }

    @Test
    public void testParseRequestIdWithoutSubrequests() throws Exception {
        String line = "tskv" +
            "\tdate=2016-08-19T15:54:35.476+03:00" +
            "\trequest_id=1471611275470/6b1f4847187863f0cae3a44b48fd1b3f" +
            "\ttime_millis=2";

        Object[] expectedFields = Arrays.copyOf(LOG_LINE_FIELDS, LOG_LINE_FIELDS.length);
        expectedFields[ID_SUBREQUESTS] = new Integer[]{};

        checker.check(line, LOG_LINE_DATE, expectedFields);
    }

    @Test
    public void testParseDurationMsInsteadOfTimeMillis() throws Exception {
        String line = "tskv" +
            "\tdate=2016-08-19T15:54:35.476+03:00" +
            "\trequest_id=1471611275470/6b1f4847187863f0cae3a44b48fd1b3f" +
            "\tduration_ms=2";

        Object[] expectedFields = Arrays.copyOf(LOG_LINE_FIELDS, LOG_LINE_FIELDS.length);
        expectedFields[ID_SUBREQUESTS] = new Integer[]{};

        checker.check(line, LOG_LINE_DATE, expectedFields);
    }

    @Test
    public void testParseOk() throws Exception {
        checker.check(
            "tskv\tdate=2016-09-16T11:38:07.528+03:00\trequest_id=1471611275470/568000474bbc5fdff0c3fb5b829cbbbb/20\ttype=OUT\trequest_method=getFiltersDescription\thttp_method=GET\terror_code=RETRIES_LIMIT_EXCEEDED->HTTP_CLIENT_REQUEST_ERROR->ENOTFOUND\tprotocol=http\tquery_params=/buker/GetCards?collection=filters-description&format=json&hid=7812073&ids=glprice%2C7893318%2C7925349%2C8220902%2C7957713%2C12687116%2C12849867%2C12602446%2C8224557%2C12601845%2C7957718%2C12849913%2C12849891%2C12849876%2C13956715%2Cmanufacturer_warranty%2Constock%2Cqrfrom%2Cfree-delivery%2Coffer-shipping%2Chome_region%2Cdelivery-interval%2Cfesh%2Cprepay-enabled%2Cshow-book-now-only\ttarget_module=market_kgb_buker\ttarget_host=1mslb.tst.vs.market.yandex.net\tretry_num=2\tduration_ms=42"
        );

      }

    @Test
    public void testParseWithSourceFields() throws Exception {
        PageMatcher pageMatcher = Mockito.mock(PageMatcher.class);
        Mockito.when(pageMatcher.matchUrl("checkouter.vs.market.yandex.net", "POST", "/checkout"))
                .thenReturn(new Page("checkout", "POST"));

        LogParserChecker mockitoChecker = new LogParserChecker(new TraceLogParser(), pageMatcher);
        mockitoChecker.setParam(EnvironmentMapper.LOGBROKER_PROTOCOL_PREFIX + checker.getOrigin(), "DEVELOPMENT");

        Object[] args = Arrays.copyOf(LOG_LINE_FIELDS, LOG_LINE_FIELDS.length);
        args[Indices.ID_MS] = 1471611275470L;
        args[Indices.ID_HASH] = "568000474bbc5fdff0c3fb5b829cbbbb";
        args[Indices.ID_SUBREQUESTS] = new Integer[] {};
        args[Indices.START_TIME_MS] = 1495030091078L;
        args[Indices.END_TIME_MS] = 1495030091346L;
        args[Indices.DURATION_MS] = 268;
        args[TYPE] = RequestType.OUT;
        args[MODULE] = "";
        args[HOST] = "hostname.test";
        args[SOURCE_MODULE] = "";
        args[SOURCE_HOST] = "hostname.test";
        args[TARGET_MODULE] = "market_report";
        args[TARGET_HOST] = "report.tst.vs.market.yandex.net";
        args[ENVIRONMENT] = Environment.DEVELOPMENT;
        args[REQUEST_METHOD] = "/yandsearch";
        args[HTTP_CODE] = 200;
        args[RETRY_NUM] = 1;
        args[ERROR_CODE] = "";
        args[PROTOCOL] = "http";
        args[HTTP_METHOD] = "GET";
        args[QUERY_PARAMS] = "ip=127.0.1.1&place=offerinfo&feed_shoffer_id=200305173-7&fesh=10207612&rids=213&regset=1&pp=18&show-booking-outlets=0&adult=1&numdoc=1&showdiscounts=1&cpa-category-filter=0&strip_query_language=0&show-promoted=1&show-min-quantity=1&show-urls=decrypted&client=checkout&co-from=checkouter";
        args[YANDEX_UID] = "25096041";
        args[YANDEX_LOGIN] = "";
        args[PAGE_ID] = "checkout";
        args[PAGE_TYPE] = "POST";

        mockitoChecker.check(
            "tskv" +
                    "\tdate=2017-05-17T17:08:11.346+03:00" +
                    "\ttype=OUT" +
                    "\trequest_id=1471611275470/568000474bbc5fdff0c3fb5b829cbbbb" +
                    "\ttarget_module=market_report" +
                    "\ttarget_host=report.tst.vs.market.yandex.net" +
                    "\tprotocol=http" +
                    "\thttp_method=GET" +
                    "\trequest_method=/yandsearch" +
                    "\tquery_params=ip=127.0.1.1&place=offerinfo&feed_shoffer_id=200305173-7&fesh=10207612&rids=213&regset=1&pp=18&show-booking-outlets=0&adult=1&numdoc=1&showdiscounts=1&cpa-category-filter=0&strip_query_language=0&show-promoted=1&show-min-quantity=1&show-urls=decrypted&client=checkout&co-from=checkouter" +
                    "\tyandex_uid=25096041" +
                    "\tretry_num=1" +
                    "\ttime_millis=268" +
                    "\thttp_code=200" +
                    "\tsource_vhost=checkouter.vs.market.yandex.net" +
                    "\tsource_http_method=POST" +
                    "\tsource_request_method=/checkout",
                new Date(1471611275470L),
                args
        );
    }

    @Test
    public void testParseWithSourcePageIdAndPageType() throws Exception {
        Object[] args = Arrays.copyOf(LOG_LINE_FIELDS, LOG_LINE_FIELDS.length);
        args[Indices.ID_MS] = 1471611275470L;
        args[Indices.ID_HASH] = "568000474bbc5fdff0c3fb5b829cbbbb";
        args[Indices.ID_SUBREQUESTS] = new Integer[] {};
        args[Indices.START_TIME_MS] = 1495030091078L;
        args[Indices.END_TIME_MS] = 1495030091346L;
        args[Indices.DURATION_MS] = 268;
        args[TYPE] = RequestType.OUT;
        args[MODULE] = "";
        args[HOST] = "hostname.test";
        args[SOURCE_MODULE] = "";
        args[SOURCE_HOST] = "hostname.test";
        args[TARGET_MODULE] = "market_report";
        args[TARGET_HOST] = "report.tst.vs.market.yandex.net";
        args[ENVIRONMENT] = Environment.DEVELOPMENT;
        args[REQUEST_METHOD] = "/yandsearch";
        args[HTTP_CODE] = 200;
        args[RETRY_NUM] = 1;
        args[ERROR_CODE] = "";
        args[PROTOCOL] = "http";
        args[HTTP_METHOD] = "GET";
        args[QUERY_PARAMS] = "ip=127.0.1.1&place=offerinfo&feed_shoffer_id=200305173-7&fesh=10207612&rids=213&regset=1&pp=18&show-booking-outlets=0&adult=1&numdoc=1&showdiscounts=1&cpa-category-filter=0&strip_query_language=0&show-promoted=1&show-min-quantity=1&show-urls=decrypted&client=checkout&co-from=checkouter";
        args[YANDEX_UID] = "25096041";
        args[YANDEX_LOGIN] = "";
        args[PAGE_ID] = "checkout";
        args[PAGE_TYPE] = "POST";

        checker.check(
                "tskv" +
                        "\tdate=2017-05-17T17:08:11.346+03:00" +
                        "\ttype=OUT" +
                        "\trequest_id=1471611275470/568000474bbc5fdff0c3fb5b829cbbbb" +
                        "\ttarget_module=market_report" +
                        "\ttarget_host=report.tst.vs.market.yandex.net" +
                        "\tprotocol=http" +
                        "\thttp_method=GET" +
                        "\trequest_method=/yandsearch" +
                        "\tquery_params=ip=127.0.1.1&place=offerinfo&feed_shoffer_id=200305173-7&fesh=10207612&rids=213&regset=1&pp=18&show-booking-outlets=0&adult=1&numdoc=1&showdiscounts=1&cpa-category-filter=0&strip_query_language=0&show-promoted=1&show-min-quantity=1&show-urls=decrypted&client=checkout&co-from=checkouter" +
                        "\tyandex_uid=25096041" +
                        "\tretry_num=1" +
                        "\ttime_millis=268" +
                        "\thttp_code=200" +
                        "\tsource_page_id=checkout" +
                        "\tsource_page_type=POST",
                new Date(1471611275470L),
                args
        );
    }

    class Indices {
        public static final int ID_MS = 0;
        public static final int ID_HASH = 1;
        public static final int ID_SUBREQUESTS = 2;
        public static final int START_TIME_MS = 3;
        public static final int END_TIME_MS = 4;
        public static final int DURATION_MS = 5;
        public static final int TYPE = 6;
        public static final int MODULE = 7;
        public static final int HOST = 8;
        public static final int SOURCE_MODULE = 9;
        public static final int SOURCE_HOST = 10;
        public static final int TARGET_MODULE = 11;
        public static final int TARGET_HOST = 12;
        public static final int ENVIRONMENT = 13;
        public static final int REQUEST_METHOD = 14;
        public static final int HTTP_CODE = 15;
        public static final int RETRY_NUM = 16;
        public static final int ERROR_CODE = 17;
        public static final int PROTOCOL = 18;
        public static final int HTTP_METHOD = 19;
        public static final int QUERY_PARAMS = 20;
        public static final int YANDEX_UID = 21;
        public static final int YANDEX_LOGIN = 22;
        public static final int KV_KEYS = 23;
        public static final int KV_VALUES = 24;
        public static final int EVENTS_NAMES = 25;
        public static final int EVENTS_TIMESTAMPS = 26;
        public static final int TEST_IDS = 27;
        public static final int PAGE_ID = 28;
        public static final int PAGE_TYPE = 29;
        public static final int RESPONSE_SIZE_BYTES = 30;
    }
}
