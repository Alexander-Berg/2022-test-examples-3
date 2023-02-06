package ru.yandex.market.logshatter.parser.marketout;

import org.junit.Test;
import ru.yandex.market.logshatter.parser.LogParserChecker;

import java.util.Date;

public class PricesSnapshotStatsParserTest {
    @Test
    public void testParse() throws Exception {
        LogParserChecker checker = new LogParserChecker(new PricesSnapshotStatsParser());

        checker.check(
            "{\"offer_deleted\": 732, \"price\": 1, \"market_color\": 0, \"old_price\": 2, \"snapshot_timestamp\": 1553773920}",
            new Date(1553773920000L), checker.getHost(), "white", 732, 1, 2);
        checker.check(
            "{\"offer_deleted\": 732, \"price\": 1, \"market_color\": 3, \"old_price\": 2, \"snapshot_timestamp\": 1553773920}",
            new Date(1553773920000L), checker.getHost(), "unknown", 732, 1, 2);
    }
}
