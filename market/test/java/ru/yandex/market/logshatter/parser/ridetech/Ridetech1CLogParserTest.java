package ru.yandex.market.logshatter.parser.ridetech;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;
import ru.yandex.market.logshatter.parser.logshatter_for_logs.Level;

@SuppressWarnings("LineLength")
public class Ridetech1CLogParserTest {

    LogParserChecker checker = new LogParserChecker(new Ridetech1CLogParser());

    @Test
    public void parse() throws Exception {
        String line1 = "{\"timestamp\":\"2022-06-22T22:57:52\",\"status\":\"Отменена\",\"user\":\"ВеселинД\",\"pc\":\"WIN-1CD1IB0F68B\",\"event_type\":\"Информация\",\"event\":\"Транзакция.Начало\",\"metadata\":\"\",\"data\":\"some data\",\"comment\":\"\",\"session\":\"7\",\"connection\":\"103488\",\"application\":\"Тонкий клиент\"}";

        checker.setHost("eda-1c-drive-test-sas.sas.yp-c.yandex");
        checker.setLogBrokerTopic("rt3.sas--taxi--drive-service-station-1c-test-log");

        checker.check(
            line1,
            1655927872,
            LocalDateTime.parse("2022-06-22T22:57:52"),
            "drive", // project
            "service-station-1c", // service
            "some data", // message
            "test", // env
            "", // cluster
            Level.INFO, // level
            "eda-1c-drive-test-sas.sas.yp-c.yandex", // hostname
            "", // version
            "sas", // dc
            "", // request_id
            "", // trace_id
            "", // span_id
            "Тонкий клиент", // component
            UUID.fromString("e0a8e334-764e-3cdf-945d-ad2d6fd4bf98"), // record_id
            "", // validation_err
            "{\"metadata\":\"\",\"pc\":\"WIN-1CD1IB0F68B\",\"session\":\"7\",\"comment\":\"\",\"connection\":\"103488\",\"event\":\"Транзакция.Начало\",\"user\":\"ВеселинД\",\"status\":\"Отменена\"}" // rest
        );

        checker.setLogBrokerTopic("rt3.sas--taxi--drive-service-station-1c-prod-log");
        String line2 = "{\"timestamp\":\"2022-06-22T22:57:52\",\"status\":\"Отменена\",\"user\":\"ВеселинД\",\"pc\":\"WIN-1CD1IB0F68B\",\"event_type\":\"Примечание\",\"event\":\"Транзакция.Начало\",\"metadata\":\"\",\"data\":\"some data 2\",\"comment\":\"\",\"session\":\"7\",\"connection\":\"103488\",\"application\":\"Фоновое задание\"}";

        checker.check(
            line2,
            1655927872,
            LocalDateTime.parse("2022-06-22T22:57:52"),
            "drive", // project
            "service-station-1c", // service
            "some data 2", // message
            "prod", // env
            "", // cluster
            Level.DEBUG, // level
            "eda-1c-drive-test-sas.sas.yp-c.yandex", // hostname
            "", // version
            "sas", // dc
            "", // request_id
            "", // trace_id
            "", // span_id
            "Фоновое задание", // component
            UUID.fromString("3fbcb345-436f-3476-8145-37ca0364ed15"), // record_id
            "", // validation_err
            "{\"metadata\":\"\",\"pc\":\"WIN-1CD1IB0F68B\",\"session\":\"7\",\"comment\":\"\",\"connection\":\"103488\",\"event\":\"Транзакция.Начало\",\"user\":\"ВеселинД\",\"status\":\"Отменена\"}" // rest
        );
    }
}
