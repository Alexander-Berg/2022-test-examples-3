package ru.yandex.market.logshatter.logs.parser.combinator_team;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;
import ru.yandex.market.logshatter.parser.logshatter_for_logs.Level;

import static ru.yandex.market.logshatter.logs.parser.combinator_team.CombinatorLogParser.DATE_PATTERN;


public class CombinatorLogParserTest {
    LogParserChecker checker = new LogParserChecker(new CombinatorLogParser());
    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DATE_PATTERN);

    @Test
    void parse() throws Exception {
        checker.setFile("combinator.log");
        checker.setHost("production-market-combinator-man-1.man.yp-c.yandex.net");
        checker.setLogBrokerTopic("rt3.kafka-bs--market-devexp@production@combinator-team--combinator");
        String line = "2022-06-20T10:41:03.981+0300\tERROR\troutes/postpone_delivery.go:487\trouting time not found " +
            "for warehouse partnerId = 101366";

        LocalDateTime dateTime = LocalDateTime.from(dateTimeFormatter.parse("2022-06-20T10:41:03.981+0300"));
        Date date = Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
        checker.check(
            line,
            date,
            dateTime, // time
            "combinator-team", // project
            "combinator", // service
            "routes/postpone_delivery.go:487\trouting time not found for warehouse partnerId = 101366", // message
            "production", // env
            "", // cluster
            Level.ERROR, // level
            checker.getHost(), // hostname
            "", // version
            "MAN", // dc
            "", // request_id
            "", // trace_id
            "", // span_id
            "combinator.log", // component
            UUID.nameUUIDFromBytes(line.getBytes()), // record_id
            "", // validation_err
            "{}" // rest
        );
    }
}
