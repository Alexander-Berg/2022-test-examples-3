package ru.yandex.market.logshatter.logs.parser.frontend;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;
import ru.yandex.market.logshatter.parser.logshatter_for_logs.Level;

import static ru.yandex.market.logshatter.logs.parser.frontend.MarketFrontendLogParser.DATE_PATTERN;


class MarketFrontendLogParserTest {

    LogParserChecker checker = new LogParserChecker(new MarketFrontendLogParser());
    SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_PATTERN);

    @BeforeEach
    void configureCommonParams() {
        checker.setFile("market-frontend.log");
        checker.setHost("production-market-frontend-sas-1.sas.yp-c.yandex.net");
        checker.setLogBrokerTopic("rt3.kafka-bs--market-devexp@production@market-front--market-front-raw");
        checker.setParam("market-devexp@production@market-front--market-front-raw",
            "{\"project\": \"market-frontend-touch\", \"service\": \"market-frontend-touch\", \"env\": \"prod\"}");
    }

    @Test
    void thenAllFieldsSpecifiedShouldParseCorrectly() throws Exception {


        String line = "2022/05/29 10:17:34 +0300\t[worker:id=1,pid=452]\t" +
            "error\tAPP\tFailedViewError\t1653808652901/494b914d4e27caa0a25d485521e00500/1\t" +
            "Failed \"@MarketNode/TopOffers\" view";


        Date date = dateFormat.parse("2022/05/29 10:17:34 +0300");
        checker.check(
            line,
            date,
            LocalDateTime.parse("2022-05-29T10:17:34.000"), // time
            "market-frontend-touch", // project
            "market-frontend-touch", // service
            "error\tAPP\tFailedViewError\t1653808652901/494b914d4e27caa0a25d485521e00500/1\tFailed " +
                "\"@MarketNode/TopOffers\" view", //
            // message
            "prod", // env
            "", // cluster
            Level.ERROR, // level
            checker.getHost(), // hostname
            "", // version
            "SAS", // dc
            "1653808652901/494b914d4e27caa0a25d485521e00500/1", // request_id
            "", // trace_id
            "", // span_id
            "market-frontend.log", // component
            UUID.nameUUIDFromBytes(line.getBytes()), // record_id
            "", // validation_err
            "{\"worker_id\":1,\"pid\":452}" // rest
        );
    }

    @Test
    void thenMissingRequestIdShouldParseCorrectly() throws Exception {

        String line = "2022/05/29 10:17:34 +0300\t[worker:id=1,pid=452]\t" +
            "FailedViewError: Failed \"@MarketNode/TopOffers\" view";


        Date date = dateFormat.parse("2022/05/29 10:17:34 +0300");
        checker.check(
            line,
            date,
            LocalDateTime.parse("2022-05-29T10:17:34.000"), // time
            "market-frontend-touch", // project
            "market-frontend-touch", // service
            "FailedViewError: Failed \"@MarketNode/TopOffers\" view", //
            // message
            "prod", // env
            "", // cluster
            Level.ERROR, // level
            checker.getHost(), // hostname
            "", // version
            "SAS", // dc
            "", // request_id
            "", // trace_id
            "", // span_id
            "market-frontend.log", // component
            UUID.nameUUIDFromBytes(line.getBytes()), // record_id
            "", // validation_err
            "{\"worker_id\":1,\"pid\":452}" // rest
        );
    }

    @Test
    void thenMissingWorkerInfoShouldParseCorrectly() throws Exception {


        String line = "2022/05/29 10:17:34 +0300\t[pid=452]\t" +
            "error\tAPP\tFailedViewError\t1653808652901/494b914d4e27caa0a25d485521e00500/1\t" +
            "Failed \"@MarketNode/TopOffers\" view";


        Date date = dateFormat.parse("2022/05/29 10:17:34 +0300");
        checker.check(
            line,
            date,
            LocalDateTime.parse("2022-05-29T10:17:34.000"), // time
            "market-frontend-touch", // project
            "market-frontend-touch", // service
            "error\tAPP\tFailedViewError\t1653808652901/494b914d4e27caa0a25d485521e00500/1\tFailed " +
                "\"@MarketNode/TopOffers\" view", //
            // message
            "prod", // env
            "", // cluster
            Level.ERROR, // level
            checker.getHost(), // hostname
            "", // version
            "SAS", // dc
            "1653808652901/494b914d4e27caa0a25d485521e00500/1", // request_id
            "", // trace_id
            "", // span_id
            "market-frontend.log", // component
            UUID.nameUUIDFromBytes(line.getBytes()), // record_id
            "", // validation_err
            "{\"pid\":452}" // rest
        );
    }

    @Test
    void thenParsingTraceLineShouldParseCorrectly() throws Exception {

        String line = "2022/05/29 10:17:34 +0300\t[worker:id=1,pid=452]\tat Processor._writeWidget " +
            "(/ssd/db/iss3/instances/be6ztvg6nkt6sotd_production_market_front_desktop_sas_QfdNRpEqedN/application/app" +
            "/node_modules/@yandex-market/apiary/server/processor.js:396:19)";


        Date date = dateFormat.parse("2022/05/29 10:17:34 +0300");
        checker.check(
            line,
            date,
            LocalDateTime.parse("2022-05-29T10:17:34.000"), // time
            "market-frontend-touch", // project
            "market-frontend-touch", // service
            "at Processor._writeWidget (/ssd/db/iss3/instances/" +
                "be6ztvg6nkt6sotd_production_market_front_desktop_sas_QfdNRpEqedN/application/app" +
                "/node_modules/@yandex-market/apiary/server/processor.js:396:19)", //
            // message
            "prod", // env
            "", // cluster
            Level.ERROR, // level
            checker.getHost(), // hostname
            "", // version
            "SAS", // dc
            "", // request_id
            "", // trace_id
            "", // span_id
            "market-frontend.log", // component
            UUID.nameUUIDFromBytes(line.getBytes()), // record_id
            "", // validation_err
            "{\"worker_id\":1,\"pid\":452}" // rest
        );
    }

    @Test
    void thenParamsNotSpecifiedShouldUseDefaults() throws Exception {
        checker.removeParam("market-devexp@production@market-front--market-front-raw");

        String line = "2022/05/29 10:17:34 +0300\t[worker:id=1,pid=452]\t" +
            "error\tAPP\tFailedViewError\t1653808652901/494b914d4e27caa0a25d485521e00500/1\t" +
            "Failed \"@MarketNode/TopOffers\" view";


        Date date = dateFormat.parse("2022/05/29 10:17:34 +0300");
        checker.check(
            line,
            date,
            LocalDateTime.parse("2022-05-29T10:17:34.000"), // time
            "market-frontend", // project
            "market-frontend", // service
            "error\tAPP\tFailedViewError\t1653808652901/494b914d4e27caa0a25d485521e00500/1\tFailed " +
                "\"@MarketNode/TopOffers\" view", //
            // message
            "unknown_env", // env
            "", // cluster
            Level.ERROR, // level
            checker.getHost(), // hostname
            "", // version
            "SAS", // dc
            "1653808652901/494b914d4e27caa0a25d485521e00500/1", // request_id
            "", // trace_id
            "", // span_id
            "market-frontend.log", // component
            UUID.nameUUIDFromBytes(line.getBytes()), // record_id
            "", // validation_err
            "{\"worker_id\":1,\"pid\":452}" // rest
        );
    }

    @Test
    void thenParamsSetIncorrectlyUseDefaults() throws Exception {
        checker.setParam("market-devexp@production@market-front--market-front-raw", "");

        String line = "2022/05/29 10:17:34 +0300\t[worker:id=1,pid=452]\t" +
            "error\tAPP\tFailedViewError\t1653808652901/494b914d4e27caa0a25d485521e00500/1\t" +
            "Failed \"@MarketNode/TopOffers\" view";


        Date date = dateFormat.parse("2022/05/29 10:17:34 +0300");
        checker.check(
            line,
            date,
            LocalDateTime.parse("2022-05-29T10:17:34.000"), // time
            "market-frontend", // project
            "market-frontend", // service
            "error\tAPP\tFailedViewError\t1653808652901/494b914d4e27caa0a25d485521e00500/1\tFailed " +
                "\"@MarketNode/TopOffers\" view", //
            // message
            "unknown_env", // env
            "", // cluster
            Level.ERROR, // level
            checker.getHost(), // hostname
            "", // version
            "SAS", // dc
            "1653808652901/494b914d4e27caa0a25d485521e00500/1", // request_id
            "", // trace_id
            "", // span_id
            "market-frontend.log", // component
            UUID.nameUUIDFromBytes(line.getBytes()), // record_id
            "", // validation_err
            "{\"worker_id\":1,\"pid\":452}" // rest
        );
    }
}
