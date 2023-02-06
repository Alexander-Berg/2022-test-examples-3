package ru.yandex.market.logshatter.parser.mbi;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;

public class TeleponyLogParserTest {

    LogParserChecker checker = new LogParserChecker(new TeleponyLogParser());

    @Test
    public void parse() throws Exception {

        String line1 =
            "tskv\tdate=2019-09-16T13:30:54.969Z\taction=GET\tobject_id=123\tobject_type=DBS_ORDER"
                + "\tsource=+7912382252\ttarget=+79853852251";

        checker.check(
            line1,
            1568629854,
            checker.getHost(),
            "GET",
            "123",
            "DBS_ORDER",
            "+7912382252",
            "+79853852251"
        );
    }
}
