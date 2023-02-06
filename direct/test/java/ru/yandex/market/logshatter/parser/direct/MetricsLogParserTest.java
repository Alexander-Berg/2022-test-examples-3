package ru.yandex.market.logshatter.parser.direct;

import java.util.Arrays;
import java.util.Date;

import org.junit.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;

public class MetricsLogParserTest {

    @Test
    public void testParse() throws Exception {
        String line = "{\"log_time\":\"2019-11-29 13:55:43\",\"method\":\"metrics.add\"," +
            "\"service\":\"direct.intapi\",\"ip\":\"2a02:6b8:c02:747:0:627:e5d0:6380\",\"reqid\":1974442920196711455," +
            "\"log_hostname\":\"141.8.150.219-red.dhcp.yndx.net\",\"log_type\":\"metrics\"," +
            "\"data\":[{\"name\":\"YA_IDEA\",\"value\":13.0,\"context\":{\"login\":\"dlyange\"," +
            "\"path\":\"arc/arcadia/direct\"}}]}";

        LogParserChecker checker = new LogParserChecker(new MetricsLogParser());

        checker.check(line,
            new Date(1575024943000L),
            "direct.intapi",
            "metrics.add",
            "141.8.150.219-red.dhcp.yndx.net",
            1974442920196711455L,
            "2a02:6b8:c02:747:0:627:e5d0:6380",
            "YA_IDEA",
            13.0,
            Arrays.asList("login", "path"),
            Arrays.asList("dlyange", "arc/arcadia/direct")
        );
    }
}
