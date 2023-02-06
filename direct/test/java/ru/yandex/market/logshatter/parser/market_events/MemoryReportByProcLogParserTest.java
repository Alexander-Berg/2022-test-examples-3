package ru.yandex.market.logshatter.parser.market_events;

import org.junit.Test;
import ru.yandex.market.logshatter.parser.LogParserChecker;

import java.util.Date;

public class MemoryReportByProcLogParserTest {

    private LogParserChecker checker = new LogParserChecker(new MemoryReportByProcLogParser());

    @Test
    public void testTskvParse() throws Exception {

        String line1 = "date=2016-11-07T15:03:29+0300\tprocess=reports_total\trss_bytes=34770006016\tpss_bytes=34635713536\tanon_bytes=24036470784\tuss_bytes=34503966720";
        checker.check(
            line1,
            new Date(1478520209000L),
            checker.getHost(),
            "reports_total",
            34770006016L,
            34635713536L,
            24036470784L,
            34503966720L,
            -1L
        );

        String line2 = "date=2016-11-07T15:04:29+0300\tprocess=system\tused_bytes=52276731904";
        checker.check(
            line2,
            new Date(1478520269000L),
            checker.getHost(),
            "system",
            -1L,
            -1L,
            -1L,
            -1L,
            52276731904L
        );

        String line3 = "date=2016-11-07T15:04:29+0300\tprocess=market-report\trss_bytes=34775396352\tpss_bytes=34641101824\tanon_bytes=24041676800\tuss_bytes=34509357056";
        checker.check(
            line3,
            new Date(1478520269000L),
            checker.getHost(),
            "market-report",
            34775396352L,
            34641101824L,
            24041676800L,
            34509357056L,
            -1L
        );
    }
}
