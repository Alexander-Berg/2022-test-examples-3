package ru.yandex.market.logshatter.parser.mbi;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;

public class ApiQueryProfilerLogParserTest {
    LogParserChecker checker;
    ApiQueryProfilerLogParser parser;
    SimpleDateFormat dateFormat;

    @BeforeEach
    public void init() {
        parser = new ApiQueryProfilerLogParser();
        checker = new LogParserChecker(parser);
        dateFormat = ContentApiHelper.dateFormat();
    }

    @Test
    public void shouldNotFailOnTrash() throws Exception {
        String string = "[2016-04-29 17:21:32,173] INFO  [MemoryStateReporter-Thread] " +
            "Free memory 31MB (34% of total), total memory 90MB, (78% of max), max memory 114MB";
        checker.checkEmpty(string);
    }

    @Test
    public void shouldParseQuerySum() throws Exception {
        String string = "[2016-04-29 15:37:01,632] DEBUG [328046478@RequestThread-0] " +
            "QuerySum /get-cpa-shops#5 JDBC : 705 ms.";
        checker.check(string,
            dateFormat.parse("2016-04-29 15:37:01,632"),
            checker.getHost(),
            "/get-cpa-shops",
            "/get-cpa-shops#5",
            "QuerySum",
            "JDBC",
            705L
        );
    }

    @Test
    public void shouldParseRequest() throws Exception {
        String string = "[2016-04-29 15:37:01,632] DEBUG [328046478@RequestThread-0] " +
            "Request /get-cpa-shops#5 took 731 ms.";
        checker.check(string,
            dateFormat.parse("2016-04-29 15:37:01,632"),
            checker.getHost(),
            "/get-cpa-shops",
            "/get-cpa-shops#5",
            "Request",
            "",
            731L
        );
    }

    @Test
    public void shouldNotFailOnMarketPayment() throws Exception {
        String string = "[2016-05-13 14:01:16,414] DEBUG [2113026120@qtp-464094472-0] " +
            "Request orderListNew#7 took 113 ms.";
        checker.check(string,
            dateFormat.parse("2016-05-13 14:01:16,414"),
            checker.getHost(),
            "orderListNew",
            "orderListNew#7",
            "Request",
            "",
            113L);
    }

    @Test
    public void shouldReturnValidData() throws Exception {
        String string = "[2016-08-08 12:55:29,984] DEBUG [862403353@qtp-1541912393-6678] " +
            "QuerySum getDatasourceIndexState#1360425384 MEMCACHE : 7 ms.";
        checker.check(string,
            // dateFormat.parse("2016-08-08 12:55:29,984"),
            new Date(1470650129 * 1000L + 984L),
            checker.getHost(),
            "getDatasourceIndexState",
            "getDatasourceIndexState#1360425384",
            "QuerySum",
            "MEMCACHE",
            7L);
    }

    @Test
    public void shouldParseQuerySumWithMarketRequestId() throws Exception {
        String string = "[2020-10-05 13:04:54,035] DEBUG [374500:136546407:getCampaign] " +
            "1601892293882/44ea43ce998f37f5f130259de9b00500/6 QuerySum getCampaign#-373693185 MEMCACHE : 3 ms.";
        checker.check(string,
            dateFormat.parse("2020-10-05 13:04:54,035"),
            checker.getHost(),
            "getCampaign",
            "getCampaign#-373693185",
            "QuerySum",
            "MEMCACHE",
            3L
        );
    }

    @Test
    public void shouldParseRequestWithMarketRequestId() throws Exception {
        String string = "[2020-10-05 14:00:54,645] DEBUG [393761:343182025:getAlerts] " +
            "1601895654393/0cc250c343293af20a877265eab00500/14 Request getAlerts#-373673924 took 4 ms.";
        checker.check(string,
            dateFormat.parse("2020-10-05 14:00:54,645"),
            checker.getHost(),
            "getAlerts",
            "getAlerts#-373673924",
            "Request",
            "",
            4L
        );
    }

    @Test
    public void shouldNotFailOnMarketPaymentWithMarketRequestId() throws Exception {
        String string = "[2020-10-05 14:00:54,645] DEBUG [393761:343182025:getAlerts] " +
            "1601895654393/0cc250c343293af20a877265eab00500/14 Request getAlerts#-373673924 took 4 ms.";
        checker.check(string,
            dateFormat.parse("2020-10-05 14:00:54,645"),
            checker.getHost(),
            "getAlerts",
            "getAlerts#-373673924",
            "Request",
            "",
            4L);
    }

    @Test
    public void shouldReturnValidDataWithMarketRequestId() throws Exception {
        String string = "[2020-10-05 13:04:54,035] DEBUG [374500:136546407:getCampaign] " +
            "1601892293882/44ea43ce998f37f5f130259de9b00500/6 QuerySum getCampaign#-373693185 MEMCACHE : 3 ms.";
        checker.check(string,
            new Date(1601892294 * 1000L + 35L),
            checker.getHost(),
            "getCampaign",
            "getCampaign#-373693185",
            "QuerySum",
            "MEMCACHE",
            3L);
    }
}
