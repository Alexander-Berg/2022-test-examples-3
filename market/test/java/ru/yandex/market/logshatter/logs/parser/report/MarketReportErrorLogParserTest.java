package ru.yandex.market.logshatter.logs.parser.report;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;
import ru.yandex.market.logshatter.parser.logshatter_for_logs.Level;


class MarketReportErrorLogParserTest {

    LogParserChecker checker = new LogParserChecker(new MarketReportErrorLogParser());
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

    @Test
    void thenAllFieldsSpecifiedShouldParseCorrectly() throws Exception {
        checker.setFile("market-report-error.log");
        checker.setHost("production-market-report-sas-1.sas.yp-c.yandex.net");
        checker.setLogBrokerTopic("market-search@market-report-error-log");

        String line = "tskv\ttskv_format=market-report-error-log\tpid=21754\tenv=production\t" +
            "location=sas\tx_market_req_id=1652364947076/6bf0d0191421219f2eb4b531d1de0500/7/2/1\t" +
            "sub_role=market\tcluster=0\thost=0\ttimestamp=2022-05-26T00:21:23+0300\tcategory=REPORT\t" +
            "code=3776\tseverity=undefined\tcontext=undefined\t" +
            "url_hash=e467f57b506d8afdf04991dd74d9cbfc\tplace=recipes_contain_glfilters\trgb=GREEN\t" +
            "resource_meta={\"client\":\"market.touch\",\"pageId\":\"touch_list\"," +
            "\"scenario\":\"fetchRecipesContainingGlfilters\"," +
            "\"chain\":\"@marketfront/PopularRecipes[/content/popularRecipes]->fetchRecipesContainingGlfilters\"}\t" +
            "client=market.touch\tclient_page_id=touch_list\t" +
            "client_scenario=fetchRecipesContainingGlfilters\tsource_role=market\tmessage=Error on get data from " +
            "memcached; market/library/cacher/MemCacher.cpp:173: Memcache - connection errors: CONNECTION FAILURE\t" +
            "cloud_service=test_report_market_sas\tsearch_type=META_AND_BASE";


        Date date = dateFormat.parse("2022-05-26T00:21:23+0300");
        checker.check(
            line,
            date,
            LocalDateTime.parse("2022-05-26T00:21:23.000"), // time
            "market-report", // project
            "market-report-error", // service
            "Error on get data from memcached; market/library/cacher/MemCacher.cpp:173: " +
                "Memcache - connection errors: CONNECTION FAILURE", //
            // message
            "prod", // env
            "", // cluster
            Level.ERROR, // level
            checker.getHost(), // hostname
            "", // version
            "SAS", // dc
            "1652364947076/6bf0d0191421219f2eb4b531d1de0500/7/2/1", // request_id
            "", // trace_id
            "", // span_id
            "market-report-error.log", // component
            UUID.nameUUIDFromBytes(line.getBytes()), // record_id
            "", // validation_err
            "{\"place\":\"recipes_contain_glfilters\",\"urlHash\":\"e467f57b506d8afdf04991dd74d9cbfc\"," +
                "\"code\":\"3776\",\"context\":\"undefined\",\"cluster\":\"0\",\"host\":\"0\"," +
                "\"cloudService\":\"test_report_market_sas\",\"category\":\"REPORT\",\"client\":\"market.touch\"," +
                "\"clientPageId\":\"touch_list\",\"clientScenario\":\"fetchRecipesContainingGlfilters\"," +
                "\"source_role\":\"market\",\"subRole\":\"market\",\"search_type\":\"META_AND_BASE\"," +
                "\"rgb\":\"GREEN\",\"pid\":\"21754\",\"severity\":\"undefined\"}" // rest
        );
    }

    @Test
    void thenMissingRequestIdShouldParseCorrectly() throws Exception {
        checker.setFile("market-report-error.log");
        checker.setHost("production-market-report-sas-1.sas.yp-c.yandex.net");
        checker.setLogBrokerTopic("market-search@market-report-error-log");

        String line = "tskv\ttskv_format=market-report-error-log\tpid=21754\tenv=production\t" +
            "location=sas\t" +
            "sub_role=market\tcluster=0\thost=0\ttimestamp=2022-05-26T00:21:23+0300\tcategory=REPORT\t" +
            "code=3776\tseverity=undefined\tcontext=undefined\t" +
            "url_hash=e467f57b506d8afdf04991dd74d9cbfc\tplace=recipes_contain_glfilters\trgb=GREEN\t" +
            "resource_meta={\"client\":\"market.touch\",\"pageId\":\"touch_list\"," +
            "\"scenario\":\"fetchRecipesContainingGlfilters\"," +
            "\"chain\":\"@marketfront/PopularRecipes[/content/popularRecipes]->fetchRecipesContainingGlfilters\"}\t" +
            "client=market.touch\tclient_page_id=touch_list\t" +
            "client_scenario=fetchRecipesContainingGlfilters\tsource_role=market\tmessage=Error on get data from " +
            "memcached; market/library/cacher/MemCacher.cpp:173: Memcache - connection errors: CONNECTION FAILURE\t" +
            "cloud_service=test_report_market_sas\tsearch_type=META_AND_BASE";


        Date date = dateFormat.parse("2022-05-26T00:21:23+0300");
        checker.check(
            line,
            date,
            LocalDateTime.parse("2022-05-26T00:21:23.000"), // time
            "market-report", // project
            "market-report-error", // service
            "Error on get data from memcached; market/library/cacher/MemCacher.cpp:173: " +
                "Memcache - connection errors: CONNECTION FAILURE", //
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
            "market-report-error.log", // component
            UUID.nameUUIDFromBytes(line.getBytes()), // record_id
            "", // validation_err
            "{\"place\":\"recipes_contain_glfilters\",\"urlHash\":\"e467f57b506d8afdf04991dd74d9cbfc\"," +
                "\"code\":\"3776\",\"context\":\"undefined\",\"cluster\":\"0\",\"host\":\"0\"," +
                "\"cloudService\":\"test_report_market_sas\",\"category\":\"REPORT\",\"client\":\"market.touch\"," +
                "\"clientPageId\":\"touch_list\",\"clientScenario\":\"fetchRecipesContainingGlfilters\"," +
                "\"source_role\":\"market\",\"subRole\":\"market\",\"search_type\":\"META_AND_BASE\"," +
                "\"rgb\":\"GREEN\",\"pid\":\"21754\",\"severity\":\"undefined\"}" // rest
        );
    }

    @Test
    void thenMissingAdditionalInfoShouldParseCorrectly() throws Exception {
        checker.setFile("market-report-error.log");
        checker.setHost("production-market-report-sas-1.sas.yp-c.yandex.net");
        checker.setLogBrokerTopic("market-search@market-report-error-log");

        String line = "tskv\ttskv_format=market-report-error-log\tpid=21754\tenv=production\t" +
            "place=recipes_contain_glfilters\tsource_role=market\tmessage=Error on get data from " +
            "memcached; market/library/cacher/MemCacher.cpp:173: Memcache - connection errors: CONNECTION FAILURE\t" +
            "timestamp=2022-05-26T00:21:23+0300\tcloud_service=test_report_market_sas\tsearch_type=META_AND_BASE";


        Date date = dateFormat.parse("2022-05-26T00:21:23+0300");
        checker.check(
            line,
            date,
            LocalDateTime.parse("2022-05-26T00:21:23.000"), // time
            "market-report", // project
            "market-report-error", // service
            "Error on get data from memcached; market/library/cacher/MemCacher.cpp:173: " +
                "Memcache - connection errors: CONNECTION FAILURE", //
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
            "market-report-error.log", // component
            UUID.nameUUIDFromBytes(line.getBytes()), // record_id
            "", // validation_err
            "{\"place\":\"recipes_contain_glfilters\",\"cloudService\":\"test_report_market_sas\"," +
                "\"source_role\":\"market\",\"search_type\":\"META_AND_BASE\",\"pid\":\"21754\"}" // rest
        );
    }
}
