package ru.yandex.market.logshatter.parser.checkout;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.logshatter.parser.CustomAssertion;
import ru.yandex.market.logshatter.parser.EnvironmentMapper;
import ru.yandex.market.logshatter.parser.LogParserChecker;
import ru.yandex.market.logshatter.parser.trace.Environment;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import static ru.yandex.market.logshatter.parser.LogParserChecker.arraysToMap;
import static ru.yandex.market.logshatter.parser.checkout.CheckouterOutgoingLogParserTest.Indices.ERROR_CODE;
import static ru.yandex.market.logshatter.parser.checkout.CheckouterOutgoingLogParserTest.Indices.HOST;
import static ru.yandex.market.logshatter.parser.checkout.CheckouterOutgoingLogParserTest.Indices.HTTP_CODE;
import static ru.yandex.market.logshatter.parser.checkout.CheckouterOutgoingLogParserTest.Indices.HTTP_METHOD;
import static ru.yandex.market.logshatter.parser.checkout.CheckouterOutgoingLogParserTest.Indices.KV_KEYS;
import static ru.yandex.market.logshatter.parser.checkout.CheckouterOutgoingLogParserTest.Indices.KV_VALUES;
import static ru.yandex.market.logshatter.parser.checkout.CheckouterOutgoingLogParserTest.Indices.PROTOCOL;
import static ru.yandex.market.logshatter.parser.checkout.CheckouterOutgoingLogParserTest.Indices.QUERY_PARAMS;
import static ru.yandex.market.logshatter.parser.checkout.CheckouterOutgoingLogParserTest.Indices.REQUEST_METHOD;
import static ru.yandex.market.logshatter.parser.checkout.CheckouterOutgoingLogParserTest.Indices.RESPONSE_SIZE_BYTES;
import static ru.yandex.market.logshatter.parser.checkout.CheckouterOutgoingLogParserTest.Indices.RETRY_NUM;
import static ru.yandex.market.logshatter.parser.checkout.CheckouterOutgoingLogParserTest.Indices.TARGET_HOST;
import static ru.yandex.market.logshatter.parser.checkout.CheckouterOutgoingLogParserTest.Indices.TARGET_MODULE;
import static ru.yandex.market.logshatter.parser.checkout.CheckouterOutgoingLogParserTest.Indices.TEST_IDS;
import static ru.yandex.market.logshatter.parser.checkout.CheckouterOutgoingLogParserTest.Indices.YANDEX_LOGIN;
import static ru.yandex.market.logshatter.parser.checkout.CheckouterOutgoingLogParserTest.Indices.YANDEX_UID;

public class CheckouterOutgoingLogParserTest {
    private LogParserChecker checker;
    private static final String LOG_LINE_BEGINNING = "tskv" +
        "\tdate=2016-08-19T15:54:35.476+03:00" +
        "\trequest_id=1471611275470/6b1f4847187863f0cae3a44b48fd1b3f/4/5/3" +
        "\ttime_millis=2";

    private Object[] logLineFieldsTemplate;
    private final Date LOG_LINE_DATE = new Date(1471611275470L);

    @Before
    public void before() {
        checker = new LogParserChecker(new CheckouterOutgoingLogParser());
        checker.setParam(EnvironmentMapper.LOGBROKER_PROTOCOL_PREFIX + checker.getOrigin(), "DEVELOPMENT");

        logLineFieldsTemplate = new Object[]{
            "1471611275470/6b1f4847187863f0cae3a44b48fd1b3f/4/5/3",
            1471611275474L, // start_time = end_time - duration
            1471611275476L, // end_time = ZonedDateTime.parse("2016-08-19T15:54:35.476+03:00").toInstant().toEpochMilli()
            2, // duration
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
            new Integer[]{},
            -1
        };
    }

    @Test
    public void testSkipLine() throws Exception {
        String line = LOG_LINE_BEGINNING +
            "\ttype=OUT" +
            "\ttarget_module=pgaas" +
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

        checker.checkEmpty(line);
    }

    @Test
    public void testParse() throws Exception {
        String line = LOG_LINE_BEGINNING +
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

        CustomAssertion keyValueAssertion = (expected, actual) -> {
            Map<Object, Object> expectedMap = arraysToMap((Object[]) expected[0], (Object[]) expected[1]);
            Map<Object, Object> actualMap = arraysToMap((Object[]) actual[0], (Object[]) actual[1]);
            Assert.assertEquals(expectedMap.entrySet(), actualMap.entrySet());
        };

        checker.setCustomAssertion(new int[]{KV_KEYS, KV_VALUES}, keyValueAssertion);

        Object[] expectedFields = Arrays.copyOf(logLineFieldsTemplate, logLineFieldsTemplate.length);
        expectedFields[HOST] = checker.getHost();
        expectedFields[TARGET_MODULE] = "market_kgb_buker";
        expectedFields[TARGET_HOST] = "marketbuker.yandex.ru";
        expectedFields[HTTP_CODE] = 404;
        expectedFields[RETRY_NUM] = 1;
        expectedFields[PROTOCOL] = "http";
        expectedFields[HTTP_METHOD] = "GET";
        expectedFields[QUERY_PARAMS] = "/buker/GetCards?collection=facts&ids=12518941";
        expectedFields[KV_KEYS] = new String[]{"clientId", "contentApiRequestId", "clientApp"};
        expectedFields[KV_VALUES] = new String[]{"2532", "01e08229ee825b6", "Internal client"};
        expectedFields[REQUEST_METHOD] = "getOffers";
        expectedFields[ERROR_CODE] = "CONNECTION_TIMEOUT";
        expectedFields[YANDEX_UID] = "fnldjcfrhn723o8bfgv623bvr23";
        expectedFields[YANDEX_LOGIN] = "algebraic";
        expectedFields[TEST_IDS] = new Integer[]{4324, 532523, 53252352};
        expectedFields[RESPONSE_SIZE_BYTES] = 42;

        checker.check(line, LOG_LINE_DATE, expectedFields);
    }

    class Indices {
        public static final int REQUEST_ID = 0;
        public static final int START_TIME_MS = 1;
        public static final int END_TIME_MS = 2;
        public static final int DURATION_MS = 3;
        public static final int HOST = 4;
        public static final int TARGET_MODULE = 5;
        public static final int TARGET_HOST = 6;
        public static final int ENVIRONMENT = 7;
        public static final int REQUEST_METHOD = 8;
        public static final int HTTP_CODE = 9;
        public static final int RETRY_NUM = 10;
        public static final int ERROR_CODE = 11;
        public static final int PROTOCOL = 12;
        public static final int HTTP_METHOD = 13;
        public static final int QUERY_PARAMS = 14;
        public static final int YANDEX_UID = 15;
        public static final int YANDEX_LOGIN = 16;
        public static final int KV_KEYS = 17;
        public static final int KV_VALUES = 18;
        public static final int TEST_IDS = 19;
        public static final int RESPONSE_SIZE_BYTES = 20;
    }

}
