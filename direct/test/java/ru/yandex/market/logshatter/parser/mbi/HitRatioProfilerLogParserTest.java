package ru.yandex.market.logshatter.parser.mbi;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.logshatter.parser.LogParser;
import ru.yandex.market.logshatter.parser.LogParserChecker;

import java.text.SimpleDateFormat;

public class HitRatioProfilerLogParserTest {
    LogParserChecker checker;
    SimpleDateFormat dateFormat;

    @Before
    public void init() {
        LogParser parser = new HitRatioProfilerLogParser();
        checker = new LogParserChecker(parser);
        dateFormat = ContentApiHelper.dateFormat();
    }

    @Test
    public void shouldParseHitRatio() throws Exception {
        String string = "[2016-06-10 12:11:38,332] DEBUG [1340744662@RequestThread-0] RequestCounts /get-cpa-shops#959493880 counts: 4 misses: 2";
        checker.check(string,
            dateFormat.parse("2016-06-10 12:11:38,332"),
            checker.getHost(),
            "/get-cpa-shops",
            "/get-cpa-shops#959493880",
            4,
            2
        );
    }


    @Test
    public void shouldNotFailOnTrash() throws Exception {
        String string = "[2016-04-29 17:21:32,173] INFO  [MemoryStateReporter-Thread] Free memory 31MB (34% of total), total memory 90MB, (78% of max), max memory 114MB";
        checker.checkEmpty(string);
    }

    @Test
    public void shouldNotFailOnMarketPayment() throws Exception {
        String string = "[2016-06-10 12:09:46,087] DEBUG [758113930@qtp-1886891676-0] RequestCounts orderListNew#963056732 counts: 1 misses: 0\n";
        checker.check(string,
            dateFormat.parse("2016-06-10 12:09:46,087"),
            checker.getHost(),
            "orderListNew",
            "orderListNew#963056732",
            1,
            0);
    }
}