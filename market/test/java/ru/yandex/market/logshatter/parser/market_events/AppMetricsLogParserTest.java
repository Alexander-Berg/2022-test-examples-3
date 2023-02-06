package ru.yandex.market.logshatter.parser.market_events;

import java.util.Date;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;

public class AppMetricsLogParserTest {

    private LogParserChecker checker = new LogParserChecker(new AppMetricsLogParser());

    @Test
    public void testTskvParse() throws Exception {
        String line1 = "date=2016-11-07T15:02:24+0300\tprocess=market-report\tworking_time_sec=3525.0";
        checker.check(
            line1,
            new Date(1478520144000L),
            checker.getHost(),
            "market-report",
            "working_time_sec",
            "3525.0"
        );

        String line2 = "date=2016-11-07T15:02:25+0300\tprocess=report\toom_killed=0";
        checker.check(
            line2,
            new Date(1478520145000L),
            checker.getHost(),
            "report",
            "oom_killed",
            "0"
        );

        String line3 = "date=2016-11-07T15:02:26+0300\tprocess=market-report\tversion=1642101";
        checker.check(
            line3,
            new Date(1478520146000L),
            checker.getHost(),
            "market-report",
            "version",
            "1642101"
        );

        String line4 = "date=2016-11-07T15:02:26+0300\tprocess=market-report\traw_version=16.4.21.1";
        checker.check(
            line4,
            new Date(1478520146000L),
            checker.getHost(),
            "market-report",
            "raw_version",
            "16.4.21.1"
        );
    }
}
