package ru.yandex.market.logshatter.parser.front;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.logshatter.parser.LogParserChecker;

import java.util.Date;

import static ru.yandex.market.logshatter.parser.trace.Environment.DEVELOPMENT;

public class TimersTskvLogParserTest {
    private LogParserChecker checker;

    @Before
    public void setUp() {
        checker = new LogParserChecker(new TimersTskvLogParser());
        checker.setOrigin("market-health-dev");
        checker.setParam("logbroker://market-health-dev", DEVELOPMENT.toString());
    }

    @Test
    public void parseTimer() throws Exception {
        String line = "tskv\ttimestamp=1502450699257\tlabel=render\ttype=preamble\tduration_ms=104\t" +
                "request_id=1502450698875/9cb883dbedf7622f5ef9c2e72e44acb5\tpage_id=test";
        checker.setFile("/var/log/yandex/market-skubi/market_front_desktop-timers.log");
        checker.check(
            line,
            new Date(1502450699257L), "market_front_desktop", checker.getHost(), "render", "preamble",
                104, "1502450698875/9cb883dbedf7622f5ef9c2e72e44acb5", DEVELOPMENT, "test"
        );
    }

    @Test
    public void emptyServiceIfNotCorrectSuffix() throws Exception {
        String line = "tskv\ttimestamp=1502450699257\tlabel=render\ttype=preamble\tduration_ms=104\t" +
                "request_id=1502450698875/9cb883dbedf7622f5ef9c2e72e44acb5\tpage_id=test";
        checker.setFile("/var/log/yandex/market-skubi/market_front_desktop.log");
        checker.check(
            line,
            new Date(1502450699257L), "", checker.getHost(), "render", "preamble", 104,
                "1502450698875/9cb883dbedf7622f5ef9c2e72e44acb5", DEVELOPMENT, "test"
        );
    }

    @Test
    public void absentPageId() throws Exception {
        String line = "tskv\ttimestamp=1502450699257\tlabel=render\ttype=preamble\tduration_ms=104\t" +
                "request_id=1502450698875/9cb883dbedf7622f5ef9c2e72e44acb5";
        checker.setFile("/var/log/yandex/market-skubi/market_front_desktop-timers.log");
        checker.check(
                line,
                new Date(1502450699257L), "market_front_desktop", checker.getHost(), "render", "preamble",
                104, "1502450698875/9cb883dbedf7622f5ef9c2e72e44acb5", DEVELOPMENT, ""
        );
    }
}
