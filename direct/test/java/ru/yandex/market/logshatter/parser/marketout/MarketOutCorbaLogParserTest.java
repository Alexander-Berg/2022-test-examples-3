package ru.yandex.market.logshatter.parser.marketout;

import org.junit.Test;
import ru.yandex.market.logshatter.parser.LogParserChecker;

import java.util.Date;

public class MarketOutCorbaLogParserTest {

    @Test
    public void testParse() throws Exception {
        LogParserChecker checker = new LogParserChecker(new MarketOutCorbaLogParser());


        checker.check(
            "PROFILE [2015-02-24 14:17:38 +0300] PutBestModelListBySettings 65",
            new Date(1424776658000L), checker.getHost(), "PutBestModelListBySettings", 200, 65, true
        );
        checker.check(
            "PROFILE [2015-02-24 14:28:32 +0300] GetCards users-info 135256998,126246443 0",
            new Date(1424777312000L), checker.getHost(), "GetCards", 200, 0, true
        );
        //Http
        checker.checkEmpty("PROFILE[2015 - 02 - 24 14:17:38 + 0300]/gurudaemon / GetCards 0");

        checker.checkEmpty("NOT_PROFILE[2015 - 02 - 24 14:17:38 + 0300]/gurudaemon / GetCards 0");


    }
}