package ru.yandex.market.logshatter.parser.marketout;

import java.util.Date;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;

public class PromoTskvLogParserTest {

    private LogParserChecker checker = new LogParserChecker(new PromoTskvLogParser());


    @Test
    public void testTskvParse() throws Exception {
        String line1 = "tskv\ttskv_format=market-report-promo-log\tunixtime=1647029718413073\trequest_id" +
            "=16991771953240733328/123\tpuid=123456789\tfeed_id=777\toffer_id=tl2iU-PouOyu0lAI4-hHyQ\t" +
            "shop_promo_id" +
            "=jwsx1gRFpREwdLrS2p6xWw\tpromo_key=DLiC-hzjdwP2sq3Vy1T9iw\tsource_type=loyalty\tpromo_type=blue-cashback" +
            "\treason=DeclinedByLoyaltyProgramStatus\tdebug=\tpipeline=0";
        checker.check(
            line1,
            new Date(1647029718413073L / 1000L),
            "16991771953240733328/123",
            "123456789",
            777,
            "tl2iU-PouOyu0lAI4-hHyQ",
            "jwsx1gRFpREwdLrS2p6xWw",
            "DLiC-hzjdwP2sq3Vy1T9iw",
            "loyalty",
            "blue-cashback",
            "DeclinedByLoyaltyProgramStatus",
            "",
            0
        );
    }
}
