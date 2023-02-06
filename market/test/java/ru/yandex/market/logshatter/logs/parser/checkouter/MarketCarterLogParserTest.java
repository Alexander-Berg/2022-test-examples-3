package ru.yandex.market.logshatter.logs.parser.checkouter;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;
import ru.yandex.market.logshatter.parser.logshatter_for_logs.Level;

import static ru.yandex.market.logshatter.logs.parser.checkouter.MarketCarterLogParser.DATE_PATTERN;


class MarketCarterLogParserTest {

    LogParserChecker checker = new LogParserChecker(new MarketCarterLogParser());
    SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_PATTERN);

    @Test
    void thenAllFieldsSpecifiedShouldParseCorrectly() throws Exception {
        checker.setFile("market-carter.log");
        checker.setHost("production-market-checkouter-sas-1.sas.yp-c.yandex.net");
        checker.setLogBrokerTopic("rt3.kafka-bs--market-checkout@checkouter-main-log");
        checker.setParam("market-checkout@checkouter-main-log", "{\"project\": \"market-checkouter\"," +
            " \"service\": \"market-carter\", \"env\": \"prod\"}");

        String line = "[2022-07-11 11:23:00,002] DEBUG [1657527780001/873af534dfd46fffaab449ea21f81261][RequestThread" +
            "-21] GET \"/actuator/prometheus\", parameters={}\n";
        Date date = dateFormat.parse("2022-07-11 11:23:00,002");

        checker.check(
            line,
            date,
            LocalDateTime.parse("2022-07-11T11:23:00.002"), // time
            "market-checkouter", // project
            "market-carter", // service
            "GET \"/actuator/prometheus\", parameters={}\n", // message
            "prod", // env
            "", // cluster
            Level.DEBUG, // level
            checker.getHost(), // hostname
            "", // version
            "SAS", // dc
            "1657527780001/873af534dfd46fffaab449ea21f81261", // request_id
            "", // trace_id
            "", // span_id
            "market-carter.log", // component
            UUID.nameUUIDFromBytes(line.getBytes()), // record_id
            "", // validation_err
            "{\"threadName\":\"RequestThread-21\"}" // rest
        );
    }
}
