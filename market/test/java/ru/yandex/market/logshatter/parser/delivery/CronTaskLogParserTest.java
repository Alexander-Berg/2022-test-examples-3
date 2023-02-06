package ru.yandex.market.logshatter.parser.delivery;

import java.util.Date;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;

public class CronTaskLogParserTest {
    static LogParserChecker checker = new LogParserChecker(new DeliveryCronTaskParser());

    @Test
    public void testParser() throws Exception {
        checker.check(
            "[2016-08-24 11:56:23] Sys.Cron:INFO:  {\"id\":\"cron_57bd6137111160.82061671\",\"startTime\":1472028982" +
                ".9885," +
                "\"endTime\":1472028982.994, \"exitCode\":1, \"controller\":\"TestCommand\",\"action\":\"stat\"," +
                "\"options\":{\"someParams\":\"xxxYYY\"}," +
                "\"args\":[\"xxx\"],\"duration\":5,\"memory\":\"4194304\",\"environment\":\"local\"," +
                "\"full_hostname\":\"web\"," +
                "\"hostname\":\"web\",\"context\":{\"param\":\"xxxYYY\"}} []",
            new Date(1472028928000L),
            "cron_57bd6137111160.82061671",
            new Date(1472028928000L),
            new Date(1472028928000L),
            1,
            "TestCommand",
            "stat",
            new String[]{"someParams"},
            "{\"someParams\":\"xxxYYY\"}",
            new String[]{"xxx"},
            5,
            4194304,
            "local",
            "web",
            "web",
            new String[]{"param"},
            "{\"param\":\"xxxYYY\"}"
        );
    }
}
