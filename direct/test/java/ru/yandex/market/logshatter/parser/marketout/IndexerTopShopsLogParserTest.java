package ru.yandex.market.logshatter.parser.marketout;

import org.junit.Test;
import ru.yandex.market.logshatter.parser.LogParserChecker;

import java.util.Date;

/**
 * @author vlid
 */
public class IndexerTopShopsLogParserTest {
    @Test
    public void testParse() throws Exception {
        LogParserChecker checker = new LogParserChecker(new IndexerTopShopsLogParser());

        checker.setFile("/var/log/marketindexer/health/feedage.log");
        checker.check("[2016-01-28 12:06:01 +0300]\t20160128_1003\t393216\t227\t[2016-01-28 04:16:39 +0300]\t[2016-01-28 04:16:39 +0300]\tok\t300\t9876543\t4\t10\t50\t100\tsome\tother\tdata",
            new Date(1453971961 * 1000L), checker.getHost(), 9876543L, 4, 10, 50, 100);
    }
}
