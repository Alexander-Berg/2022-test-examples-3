package ru.yandex.market.logshatter.parser.ridetech;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;
import ru.yandex.market.logshatter.parser.logshatter_for_logs.Level;

@SuppressWarnings("LineLength")
public class ExperimentalEdaLogParserTest {

    LogParserChecker checker = new LogParserChecker(new ExperimentalEdaLogParser());

    @Test
    public void parse() throws Exception {
        String line1 = "{\"_service\":\"eda_core-jobs_stable\",\"_dc\":\"vla\",\"_version\":\"\",\"_branch\":\"\",\"_canary\":false,\"_allocation_id\":\"\",\"_container_id\":\"\",\"_host\":\"eda-core-jobs-stable-36.vla.yp-c.yandex.net\",\"_layer\":\"prod\",\"_time\":\"2022-06-21T15:34:51.786226+03:00\",\"_level\":\"INFO\",\"_uuid\":\"11821343-31f3-45ab-93ce-9431e255dd76\",\"currentUserType\":null,\"meta_type\":\"\",\"span_id\":\"54194a0e653457b8\",\"parent_id\":null,\"uuid\":\"4ad54826-8637-405d-bd73-1e4d7f8206ff\",\"currentUser\":null,\"timestamp\":\"2022-06-21T15:34:51.786248\",\"trace_id\":\"cc70d17ed84a0be2\",\"duration\":0.005015134811401367,\"link\":\"46dd8aab2868464f\",\"type\":\"log\",\"durationFormatted\":\"0.005 seconds\",\"caller\":\"/var/www/vendor/monolog/monolog/src/Monolog/Logger.php:546\",\"context\":\"{\\\"data\\\":{\\\"placeId\\\":153545,\\\"originId\\\":\\\"cdeb1e3bed204674b6612a82a6074352000100010001_940f2269f8444f56b5c798f4831c9c8b000200010000\\\",\\\"imageUrl\\\":\\\"grocery-pics:\\\\/\\\\/grocery\\\\/2783132\\\\/27f80a9836754001adea0667629bcf6c\\\",\\\"vendorUpdatedAt\\\":null,\\\"vendorHash\\\":null,\\\"resetCache\\\":false},\\\"env\\\":\\\"prod\\\"}\",\"_time_nano\":\"786226000\",\"_context\":\"image_download\",\"_thread\":\"cc70d17ed84a0be2\",\"_request_id\":\"46dd8aab2868464f\",\"_message\":\"Menu image download message successfully processed.\",\"_timestamp\":\"2022-06-21T15:34:51\",\"_timezone\":\"+03:00\"}";
        String line2 = "{\"_service\":\"eda_core_pre_stable\",\"_dc\":\"vla\",\"_version\":\"\",\"_branch\":\"\",\"_canary\":false,\"_allocation_id\":\"\",\"_container_id\":\"\",\"_host\":\"eda-core-pre-stable-3.vla.yp-c.yandex.net\",\"_layer\":\"prod\",\"_time\":\"2022-06-21T18:47:00.21198+03:00\",\"_level\":\"INFO\",\"_uuid\":\"9a3d1c64-8b43-41af-8c1d-3abfaabee6e7\",\"type\":\"log\",\"parent_id\":\"f0e0e6a274c5fcae\",\"uuid\":\"ff0a804d377c8ad66312cda1510478c7\",\"duration\":0.09621882438659668,\"timestamp\":\"2022-06-21T18:47:00.212015\",\"meta_type\":\"/server/api/v1/surge/courier-activity\",\"span_id\":\"456ee8c5294519fe\",\"currentUser\":null,\"context\":\"[]\",\"trace_id\":\"27e56e2c8d9c45d0998baa5a18441347\",\"durationFormatted\":\"0.096 seconds\",\"caller\":\"/var/www/vendor/monolog/monolog/src/Monolog/Logger.php:546\",\"link\":\"5fc32cba19d854ae\",\"currentUserType\":null,\"parent_link\":\"8d96d6f9598140c88e594a7062734f55\",\"_time_nano\":\"211980000\",\"_context\":\"common_components\",\"_thread\":\"27e56e2c8d9c45d0998baa5a18441347\",\"_request_id\":\"5fc32cba19d854ae\",\"_message\":\"[TaxiConfig] load config from storage\",\"_timestamp\":\"2022-06-21T18:47:00\",\"_timezone\":\"+03:00\"}";
        String line3 = "{\"_service\":\"eda_core_testing\",\"_dc\":\"vla\",\"_version\":\"\",\"_branch\":\"\",\"_canary\":false,\"_allocation_id\":\"\",\"_container_id\":\"\",\"_host\":\"eda-core-testing-6.vla.yp-c.yandex.net\",\"_layer\":\"test\",\"_time\":\"2022-06-21T19:09:12.023252+03:00\",\"_level\":\"INFO\",\"_uuid\":\"27158af6-1798-447c-88b5-fdbf31a94b80\",\"trace_id\":\"a7c11959eefe52d9\",\"currentUser\":null,\"parent_id\":\"94592fbf85851653\",\"caller\":\"/var/www/vendor/monolog/monolog/src/Monolog/Logger.php:546\",\"timestamp\":\"2022-06-21T19:09:12.023299\",\"link\":\"19c4d580dc46e3db\",\"duration\":0.06148505210876465,\"durationFormatted\":\"0.061 seconds\",\"context\":\"[]\",\"meta_type\":\"/ping\",\"type\":\"log\",\"span_id\":\"00a0ad88c4f0a5ff\",\"uuid\":\"fd1da4d7baa19668e7bf5d74df019094\",\"currentUserType\":null,\"_time_nano\":\"023252000\",\"_context\":\"common_components\",\"_thread\":\"a7c11959eefe52d9\",\"_request_id\":\"19c4d580dc46e3db\",\"_message\":\"[TaxiConfig] load config from storage\",\"_timestamp\":\"2022-06-21T19:09:12\",\"_timezone\":\"+03:00\"}";

        checker.setParam("eda-core-jobs-stable-36.vla.yp-c.yandex.net",
            "{'namespace': 'eda', 'project': 'eda', 'service': 'core-jobs', 'env': 'stable'}");
        checker.setParam("eda-core-pre-stable-3.vla.yp-c.yandex.net",
            "{'namespace': 'eda', 'project': 'eda', 'service': 'core', 'env': 'prestable'}");

        checker.check(
            line1,
            1655814891,
            LocalDateTime.parse("2022-06-21T15:34:51.786226"),
            "eda_eda", // project
            "core-jobs", // service
            "Menu image download message successfully processed.", // message
            "stable", // env
            "", // cluster
            Level.INFO, // level
            "eda-core-jobs-stable-36.vla.yp-c.yandex.net", // hostname
            "", // version
            "vla", // dc
            "46dd8aab2868464f", // request_id
            "cc70d17ed84a0be2", // trace_id
            "54194a0e653457b8", // span_id
            "image_download", // component
            UUID.fromString("11821343-31f3-45ab-93ce-9431e255dd76"), // record_id
            "", // validation_err
            "{\"type\":\"log\",\"uuid\":\"4ad54826-8637-405d-bd73-1e4d7f8206ff\",\"duration\":\"0.005015134811401367\",\"currentUserType\":\"null\",\"context\":\"{\\\"data\\\":{\\\"placeId\\\":153545,\\\"originId\\\":\\\"cdeb1e3bed204674b6612a82a6074352000100010001_940f2269f8444f56b5c798f4831c9c8b000200010000\\\",\\\"imageUrl\\\":\\\"grocery-pics:\\\\/\\\\/grocery\\\\/2783132\\\\/27f80a9836754001adea0667629bcf6c\\\",\\\"vendorUpdatedAt\\\":null,\\\"vendorHash\\\":null,\\\"resetCache\\\":false},\\\"env\\\":\\\"prod\\\"}\",\"timestamp\":\"2022-06-21T15:34:51.786248\",\"durationFormatted\":\"0.005 seconds\",\"meta_type\":\"\",\"currentUser\":\"null\",\"caller\":\"/var/www/vendor/monolog/monolog/src/Monolog/Logger.php:546\",\"parent_id\":\"null\"}" // rest
        );

        checker.check(
            line2,
            1655826420,
            LocalDateTime.parse("2022-06-21T18:47:00.21198"),
            "eda_eda", // project
            "core", // service
            "[TaxiConfig] load config from storage", // message
            "prestable", // env
            "", // cluster
            Level.INFO, // level
            "eda-core-pre-stable-3.vla.yp-c.yandex.net", // hostname
            "", // version
            "vla", // dc
            "5fc32cba19d854ae", // request_id
            "27e56e2c8d9c45d0998baa5a18441347", // trace_id
            "456ee8c5294519fe", // span_id
            "common_components", // component
            UUID.fromString("9a3d1c64-8b43-41af-8c1d-3abfaabee6e7"), // record_id
            "", // validation_err
            "{\"type\":\"log\",\"uuid\":\"ff0a804d377c8ad66312cda1510478c7\",\"duration\":\"0.09621882438659668\",\"parent_link\":\"8d96d6f9598140c88e594a7062734f55\",\"currentUserType\":\"null\",\"context\":\"[]\",\"timestamp\":\"2022-06-21T18:47:00.212015\",\"durationFormatted\":\"0.096 seconds\",\"meta_type\":\"/server/api/v1/surge/courier-activity\",\"currentUser\":\"null\",\"caller\":\"/var/www/vendor/monolog/monolog/src/Monolog/Logger.php:546\",\"parent_id\":\"f0e0e6a274c5fcae\"}" // rest
        );

        checker.check(
            line3,
            1655827752,
            LocalDateTime.parse("2022-06-21T19:09:12.023252"),
            "eda", // project
            "core", // service
            "[TaxiConfig] load config from storage", // message
            "testing", // env
            "", // cluster
            Level.INFO, // level
            "eda-core-testing-6.vla.yp-c.yandex.net", // hostname
            "", // version
            "vla", // dc
            "19c4d580dc46e3db", // request_id
            "a7c11959eefe52d9", // trace_id
            "00a0ad88c4f0a5ff", // span_id
            "common_components", // component
            UUID.fromString("27158af6-1798-447c-88b5-fdbf31a94b80"), // record_id
            "", // validation_err
            "{\"type\":\"log\",\"uuid\":\"fd1da4d7baa19668e7bf5d74df019094\",\"duration\":\"0.06148505210876465\",\"currentUserType\":\"null\",\"context\":\"[]\",\"timestamp\":\"2022-06-21T19:09:12.023299\",\"durationFormatted\":\"0.061 seconds\",\"meta_type\":\"/ping\",\"currentUser\":\"null\",\"caller\":\"/var/www/vendor/monolog/monolog/src/Monolog/Logger.php:546\",\"parent_id\":\"94592fbf85851653\"}" // rest
        );
    }
}
