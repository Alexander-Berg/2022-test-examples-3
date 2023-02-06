package ru.yandex.market.logshatter.logs.parser.checkouter;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;
import ru.yandex.market.logshatter.parser.logshatter_for_logs.Level;

import static ru.yandex.market.logshatter.logs.parser.checkouter.MarketCheckouterLogParser.DATE_PATTERN;


class MarketCheckouterLogParserTest {

    LogParserChecker checker = new LogParserChecker(new MarketCheckouterLogParser());
    SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_PATTERN);

    @BeforeEach
    void configureCommonParams() {
        checker.setFile("market-checkouter.log");
        checker.setHost("production-market-checkouter-sas-1.sas.yp-c.yandex.net");
        checker.setLogBrokerTopic("rt3.kafka-bs--market-checkout@checkouter-main-log");
        checker.setParam("market-checkout@checkouter-main-log",
            "{\"project\": \"market-checkouter-project\", \"service\": \"market-checkouter-service\", \"env\": " +
                "\"prod\"}");
    }

    @Test
    void thenAllFieldsSpecifiedShouldParseCorrectly() throws Exception {

        String line = "[2022-05-12 17:15:47,192] DEBUG [1652364947076/6bf0d0191421219f2eb4b531d1de0500/7/2/1]" +
            "[RequestsExecutor-2215][MarketReportSearchServiceImpl]" +
            " Get stocks for offers. Report [OFFER_INFO] response: {SSItem{shopSku='1060', warehouseId=193879}=1}~";


        Date date = dateFormat.parse("2022-05-12 17:15:47,192");
        checker.check(
            line,
            date,
            LocalDateTime.parse("2022-05-12T17:15:47.192"), // time
            "market-checkouter-project", // project
            "market-checkouter-service", // service
            "Get stocks for offers. Report [OFFER_INFO] response: {SSItem{shopSku='1060', warehouseId=193879}=1}", //
            // message
            "prod", // env
            "", // cluster
            Level.DEBUG, // level
            checker.getHost(), // hostname
            "", // version
            "SAS", // dc
            "1652364947076/6bf0d0191421219f2eb4b531d1de0500/7/2/1", // request_id
            "", // trace_id
            "", // span_id
            "market-checkouter.log", // component
            UUID.nameUUIDFromBytes(line.getBytes()), // record_id
            "", // validation_err
            "{\"loggerName\":\"MarketReportSearchServiceImpl\",\"threadName\":\"RequestsExecutor-2215\"}" // rest
        );
    }

    @Test
    void thenMissingRequestIdShouldParseCorrectly() throws Exception {

        String lineWithoutRequestId = "[2022-05-12 17:15:47,192] DEBUG [][RequestsExecutor-2200]" +
            "[OrderItemsOfferDataMutation] >>Applying OrderItemsOfferDataMutation~";

        Date date = dateFormat.parse("2022-05-12 17:15:47,192");
        checker.check(
            lineWithoutRequestId,
            date,
            LocalDateTime.parse("2022-05-12T17:15:47.192"), // time
            "market-checkouter-project", // project
            "market-checkouter-service", // service
            ">>Applying OrderItemsOfferDataMutation", // message
            "prod", // env
            "", // cluster
            Level.DEBUG, // level
            checker.getHost(), // hostname
            "", // version
            "SAS", // dc
            "", // request_id
            "", // trace_id
            "", // span_id
            "market-checkouter.log", // component
            UUID.nameUUIDFromBytes(lineWithoutRequestId.getBytes()), // record_id
            "", // validation_err
            "{\"loggerName\":\"OrderItemsOfferDataMutation\",\"threadName\":\"RequestsExecutor-2200\"}" // rest
        );
    }

    @Test
    void thenMissingAdditionalInfoShouldParseCorrectly() throws Exception {

        String lineWithoutRequestId = "[2022-05-12 17:15:47,192] " +
            "DEBUG [1652364947076/6bf0d0191421219f2eb4b531d1de0500/7/2][][] Set marketplace fields: sku=101619154415~";

        Date date = dateFormat.parse("2022-05-12 17:15:47,192");
        checker.check(
            lineWithoutRequestId,
            date,
            LocalDateTime.parse("2022-05-12T17:15:47.192"), // time
            "market-checkouter-project", // project
            "market-checkouter-service", // service
            "Set marketplace fields: sku=101619154415", // message
            "prod", // env
            "", // cluster
            Level.DEBUG, // level
            checker.getHost(), // hostname
            "", // version
            "SAS", // dc
            "1652364947076/6bf0d0191421219f2eb4b531d1de0500/7/2", // request_id
            "", // trace_id
            "", // span_id
            "market-checkouter.log", // component
            UUID.nameUUIDFromBytes(lineWithoutRequestId.getBytes()), // record_id
            "", // validation_err
            "{\"loggerName\":\"\",\"threadName\":\"\"}" // rest
        );
    }

    @Test
    void thenStackTraceSpecifiedShouldParseCorrectly() throws Exception {

        String line =
            "[2022-05-12 17:15:47,192] DEBUG [1652364947076/6bf0d0191421219f2eb4b531d1de0500/7/2/1]" +
                "[RequestsExecutor-2215][MarketReportSearchServiceImpl] " +
                "Get stocks for offers. Report [OFFER_INFO] response: " +
                "{SSItem{shopSku='1060', warehouseId=193879}=1}~ " +
                "org.springframework.web.client.HttpServerErrorException$GatewayTimeout: " +
                "504 Gateway Timeout: [{\"status\":\"error\",\"code\":504,\"data\":{\"error\":" +
                "\"invalid response from billing-wallet, status_code=500, received " +
                "{\\\"code\\\":\\\"500\\\",\\\"message\\\":\\\"Internal Server Error\\\"}\"}}]|       " +
                "at org.springframework.web.client.HttpServerErrorException.create(HttpServerErrorException.java:116)" +
                "|   " +
                "at org.springframework.web.client.DefaultResponseErrorHandler.handleError(" +
                "DefaultResponseErrorHandler.java:186)";


        Date date = dateFormat.parse("2022-05-12 17:15:47,192");
        checker.check(
            line,
            date,
            LocalDateTime.parse("2022-05-12T17:15:47.192"), // time
            "market-checkouter-project", // project
            "market-checkouter-service", // service
            "Get stocks for offers. Report [OFFER_INFO] response: {SSItem{shopSku='1060', warehouseId=193879}=1} org" +
                ".springframework.web.client.HttpServerErrorException$GatewayTimeout: 504 Gateway Timeout: " +
                "[{\"status\":\"error\",\"code\":504,\"data\":{\"error\":\"invalid response from billing-wallet, " +
                "status_code=500, received {\\\"code\\\":\\\"500\\\",\\\"message\\\":\\\"Internal Server " +
                "Error\\\"}\"}}]|       at org.springframework.web.client.HttpServerErrorException.create" +
                "(HttpServerErrorException.java:116)|   at org.springframework.web.client.DefaultResponseErrorHandler" +
                ".handleError(DefaultResponseErrorHandler.java:186)", // message
            "prod", // env
            "", // cluster
            Level.DEBUG, // level
            checker.getHost(), // hostname
            "", // version
            "SAS", // dc
            "1652364947076/6bf0d0191421219f2eb4b531d1de0500/7/2/1", // request_id
            "", // trace_id
            "", // span_id
            "market-checkouter.log", // component
            UUID.nameUUIDFromBytes(line.getBytes()), // record_id
            "", // validation_err
            "{\"loggerName\":\"MarketReportSearchServiceImpl\",\"threadName\":\"RequestsExecutor-2215\"}" // rest
        );
    }

    @Test
    void thenProjectParametersNotSpecifiedShouldUseDefaults() throws Exception {

        checker.removeParam("market-checkout@checkouter-main-log");

        String line = "[2022-05-12 17:15:47,192] DEBUG [1652364947076/6bf0d0191421219f2eb4b531d1de0500/7/2/1]" +
            "[RequestsExecutor-2215][MarketReportSearchServiceImpl]" +
            " Get stocks for offers. Report [OFFER_INFO] response: {SSItem{shopSku='1060', warehouseId=193879}=1}~";


        Date date = dateFormat.parse("2022-05-12 17:15:47,192");
        checker.check(
            line,
            date,
            LocalDateTime.parse("2022-05-12T17:15:47.192"), // time
            "market-checkouter", // project
            "market-checkouter", // service
            "Get stocks for offers. Report [OFFER_INFO] response: {SSItem{shopSku='1060', warehouseId=193879}=1}", //
            // message
            "unknown_env", // env
            "", // cluster
            Level.DEBUG, // level
            checker.getHost(), // hostname
            "", // version
            "SAS", // dc
            "1652364947076/6bf0d0191421219f2eb4b531d1de0500/7/2/1", // request_id
            "", // trace_id
            "", // span_id
            "market-checkouter.log", // component
            UUID.nameUUIDFromBytes(line.getBytes()), // record_id
            "", // validation_err
            "{\"loggerName\":\"MarketReportSearchServiceImpl\",\"threadName\":\"RequestsExecutor-2215\"}" // rest
        );
    }

    @Test
    void thenProjectParametersIncorrectShouldUseDefaults() throws Exception {

        checker.setParam("market-checkout@checkouter-main-log", "");

        String line = "[2022-05-12 17:15:47,192] DEBUG [1652364947076/6bf0d0191421219f2eb4b531d1de0500/7/2/1]" +
            "[RequestsExecutor-2215][MarketReportSearchServiceImpl]" +
            " Get stocks for offers. Report [OFFER_INFO] response: {SSItem{shopSku='1060', warehouseId=193879}=1}~";


        Date date = dateFormat.parse("2022-05-12 17:15:47,192");
        checker.check(
            line,
            date,
            LocalDateTime.parse("2022-05-12T17:15:47.192"), // time
            "market-checkouter", // project
            "market-checkouter", // service
            "Get stocks for offers. Report [OFFER_INFO] response: {SSItem{shopSku='1060', warehouseId=193879}=1}", //
            // message
            "unknown_env", // env
            "", // cluster
            Level.DEBUG, // level
            checker.getHost(), // hostname
            "", // version
            "SAS", // dc
            "1652364947076/6bf0d0191421219f2eb4b531d1de0500/7/2/1", // request_id
            "", // trace_id
            "", // span_id
            "market-checkouter.log", // component
            UUID.nameUUIDFromBytes(line.getBytes()), // record_id
            "", // validation_err
            "{\"loggerName\":\"MarketReportSearchServiceImpl\",\"threadName\":\"RequestsExecutor-2215\"}" // rest
        );
    }
}
