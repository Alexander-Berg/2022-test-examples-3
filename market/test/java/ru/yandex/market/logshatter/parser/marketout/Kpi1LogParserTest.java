package ru.yandex.market.logshatter.parser.marketout;

import java.util.Date;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;

/**
 * @author kukabara
 */
public class Kpi1LogParserTest {
    @Test
    public void testParse() throws Exception {
        LogParserChecker checker = new LogParserChecker(new Kpi1LogParser());

        checker.setFile("/var/log/marketindexer/health/feedage.log");
        checker.check("[2016-01-28 12:06:01 +0300]\t20160128_1003\t393216\t227\t[2016-01-28 04:16:39 " +
                "+0300]\t[2016-01-28 04:16:39 +0300]\tok\t300",
            new Date(1453971961 * 1000L), checker.getHost(), 227L, 300L, "feedage");

        checker.setFile("/var/log/marketindexer/diff/health/diff_feedage.log");
        checker.check("[2016-01-28 12:06:01 +0300]\t20160128_1003\t393216\t227\t[2016-01-28 04:16:39 " +
                "+0300]\t[2016-01-28 04:16:39 +0300]\tok\t300",
            new Date(1453971961 * 1000L), checker.getHost(), 227L, 300L, "diff_feedage");

        checker.setFile("/var/log/marketindexer/health/scage.log");
        checker.check("[2016-03-01 13:44:39 +0300]\t200301805\t3\t7150",
            new Date(1456829079 * 1000L), checker.getHost(), 3L, 7150L, "scage");

        checker.setFile("/var/log/auction/mbi-bidding-kpi.log");
        checker.check("[2016-01-28 06:26:04 +0300]\t26683392\t21098679\t5320492\t0\t0\t0\t10039\t0\tFEED_OFFER_ID" +
                "\tSEARCH",
            new Date(1453951564 * 1000L), checker.getHost(), 0L, 0L, "bid_id");

        checker.setFile("/var/log/marketindexer/health/qindex_feedage.log");
        checker.check("[2016-02-12 13:15:00 +0300]\t399759\t9702\t1989",
            new Date(1455272100 * 1000L), checker.getHost(), 9702L, 1989L, "qindex");

        checker.setFile("/var/log/marketindexer/health/qindex_feedage_no_or.log");
        checker.check("[2016-02-12 13:15:00 +0300]\t399759\t9702\t1989",
            new Date(1455272100 * 1000L), checker.getHost(), 9702L, 1989L, "qindex_no_or");
    }
}
