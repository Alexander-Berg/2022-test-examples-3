package ru.yandex.market.logshatter.parser.mbi;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.logshatter.parser.LogParserChecker;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ApiQueryProfilerLogParserTest {
    LogParserChecker checker;
    ApiQueryProfilerLogParser parser;
    SimpleDateFormat dateFormat;

    @Before
    public void init() {
        parser = new ApiQueryProfilerLogParser();
        checker = new LogParserChecker(parser);
        dateFormat = ContentApiHelper.dateFormat();
    }

    @Test
    public void shouldParseQuerySum() throws Exception {
        String string = "[2016-04-29 15:37:01,632] DEBUG [328046478@RequestThread-0] QuerySum /get-cpa-shops#5 JDBC : 705 ms.";
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
        String string = "[2016-04-29 15:37:01,632] DEBUG [328046478@RequestThread-0] Request /get-cpa-shops#5 took 731 ms.";
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
    public void shouldNotFailOnTrash() throws Exception {
        String string = "[2016-04-29 17:21:32,173] INFO  [MemoryStateReporter-Thread] Free memory 31MB (34% of total), total memory 90MB, (78% of max), max memory 114MB";
        checker.checkEmpty(string);
    }

    @Test
    public void shouldNotFailOnMarketPayment() throws Exception {
        String string = "[2016-05-13 14:01:16,414] DEBUG [2113026120@qtp-464094472-0] Request orderListNew#7 took 113 ms.";
        checker.check(string,
            dateFormat.parse("2016-05-13 14:01:16,414"),
            checker.getHost(),
            "orderListNew",
            "orderListNew#7",
            "Request",
            "",
            113l);
    }

    @Test
    public void shouldReturnValidData() throws Exception {
        String string = "[2016-08-08 12:55:29,984] DEBUG [862403353@qtp-1541912393-6678] QuerySum getDatasourceIndexState#1360425384 MEMCACHE : 7 ms.";
        checker.check(string,
            // dateFormat.parse("2016-08-08 12:55:29,984"),
            new Date(1470650129 * 1000L + 984L),
            checker.getHost(),
            "getDatasourceIndexState",
            "getDatasourceIndexState#1360425384",
            "QuerySum",
            "MEMCACHE",
            7l);
    }
}
