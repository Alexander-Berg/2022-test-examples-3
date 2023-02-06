package ru.yandex.market.logshatter.logs.parser.report;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;
import ru.yandex.market.logshatter.parser.logshatter_for_logs.Level;


class MarketReportLogParserTest {

    LogParserChecker checker = new LogParserChecker(new MarketReportMainLogParser());
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

    @Test
    void thenAllFieldsSpecifiedShouldParseCorrectly() throws Exception {
        checker.setFile("market-report-main.log");
        checker.setHost("production-market-report-sas-1.sas.yp-c.yandex.net");
        checker.setLogBrokerTopic("market-search@market-report-main-log");

        String line = "tskv\ttskv_format=market-report-access-log\tunixtime=1653950601\tevent_time=[Tue May 31 " +
            "01:43:21 2022]\turl=http://sas2-4926-749-sas-market-test--b33-17050.gencfg-c.yandex" +
            ".net:17051/yandsearch?ip=127.0.0.1&place=offerinfo&feed_shoffer_id=200836573-*&fesh=10781189&rids=213" +
            "&regset=1&pp=18&show-booking-outlets=0&adult=0&numdoc=100&show-model-card-params=1&showdiscounts=1&cpa" +
            "-category-filter=0&strip_query_language=0&show-promoted=1&show-min-quantity=1&show-urls=decrypted&client" +
            "=checkout&co-from=checkouter&show-filter-mark=specifiedForOffer&show-preorder=0&rgb=BLUE&rearr-factors" +
            "=combinator%3D1&use-virt-shop=0\tcookies=\tsearch_elapsed=18\tuser_agent=okhttp/3.14" +
            ".9\tuser_agent_hash=2541706998\tremote_addr=2a02:6b8:c04:1e7:0:522:579c:3605\tfull_elapsed=18" +
            "\ttotal_documents_processed=6\ttotal_documents_cpc=0\ttotal_documents_cpa=44\ttotal_documents_accepted=6" +
            "\ttotal_rendered=0\treq_wiz_time=-1\twizards=\treqid=\tredirect_info=\terror_info=\tbase_search_elapsed" +
            "=10\tmeta_search_elapsed=6\thave_trimmed_field=0\tfetch_time=0\tsnippet_requests_made=0" +
            "\tsnippets_fetched=0\turl_hash=0df2f07a56515e2fb1c33e411a55bc0f\ttest_ids=\tquery_corrected_by_speller" +
            "=\tfuzzy_search_used=0\tx_market_req_id=1653950601036/873618b2f89612483293e756d0fc5ddc\tunixtime_ms" +
            "=1653950601048\treq_wiz_count=0\tproduct_type=NONE\texternal_requests_time=0\ttotal_renderable=2" +
            "\twizard_elements=\ticookie=\trgb=GREEN\tpp=18\trequest_body=\tpartial_answer=0" +
            "\testimated_max_memory_usage=0\tenv=production\tlocation=sas\tsub_role=market\tcluster=0\thost=0" +
            "\tresponse_size_bytes=28205\tcpu_time_us=92977\tcpu_time_us_meta=13698\tproduct_request" +
            "=\texternal_snippet_stall_time_ms=0\tapproximate_network_usage=230482\twait_time_us=90\tresource_meta" +
            "=\tclient=\tclient_page_id=\tclient_scenario=\tis_suspicious=0\tdocs_before_accept=44\texp_info" +
            "=\tsource_role=market\tcloud_service=test_report_market_sas\tmajor_faults=0\tis_antirobot_degradation=0" +
            "\tsearch_type=META_AND_BASE\tsmm=1\tbasesearch_called=2";


        Date date = dateFormat.parse("2022-05-31T01:43:21+0300");
        checker.check(
            line,
            date,
            LocalDateTime.parse("2022-05-31T01:43:21.066"), // time
            "market-report", // project
            "market-report", // service
            "{\"search_elapsed\":18,\"total_documents_processed\":6," +
                "\"url_hash\":\"0df2f07a56515e2fb1c33e411a55bc0f\",\"partial_answer\":false," +
                "\"response_size_bytes\":28205,\"major_faults\":0}", //
            // message
            "prod", // env
            "", // cluster
            Level.INFO, // level
            checker.getHost(), // hostname
            "", // version
            "SAS", // dc
            "1653950601036/873618b2f89612483293e756d0fc5ddc", // request_id
            "", // trace_id
            "", // span_id
            "market-report-main.log", // component
            UUID.nameUUIDFromBytes(line.getBytes()), // record_id
            "", // validation_err
            "{\"place\":\"offerinfo\",\"url_hash\":\"0df2f07a56515e2fb1c33e411a55bc0f\"," +
                "\"url\":\"http://sas2-4926-749-sas-market-test--b33-17050.gencfg-c.yandex" +
                ".net:17051/yandsearch?ip=127.0.0.1&place=offerinfo&feed_shoffer_id=200836573-*&fesh=10781189&rids" +
                "=213&regset=1&pp=18&show-booking-outlets=0&adult=0&numdoc=100&show-model-card-params=1&showdiscounts" +
                "=1&cpa-category-filter=0&strip_query_language=0&show-promoted=1&show-min-quantity=1&show-urls" +
                "=decrypted&client=checkout&co-from=checkouter&show-filter-mark=specifiedForOffer&show-preorder=0&rgb" +
                "=BLUE&rearr-factors=combinator%3D1&use-virt-shop=0\",\"user_agent\":\"okhttp/3.14.9\"," +
                "\"remote_addr\":\"2a02:6b8:c04:1e7:0:522:579c:3605\",\"full_elapsed\":18,\"total_documents_cpc\":0," +
                "\"total_documents_cpa\":44,\"total_documents_accepted\":6,\"total_rendered\":0,\"req_wiz_time\":-1," +
                "\"is_redirect\":false,\"base_search_elapsed\":10,\"meta_search_elapsed\":6,\"fetch_time\":0," +
                "\"external_requests_time\":0,\"total_renderable\":2,\"rgb\":\"blue\",\"pp\":18," +
                "\"estimated_max_memory_usage\":0,\"source_role\":\"market\",\"cluster\":0,\"cpu_time_us\":92977," +
                "\"cpu_time_us_meta\":13698,\"external_snippet_stall_time_ms\":0," +
                "\"approximate_network_usage\":230482,\"wait_time_us\":90,\"client\":\"checkout\"," +
                "\"is_suspicious\":false,\"is_antirobot_degradation\":false,\"pruncount\":0,\"smm\":1.0," +
                "\"basesearch_called\":2}" // rest
        );
    }

    @Test
    void thenMissingRequestIdShouldParseCorrectly() throws Exception {
        checker.setFile("market-report-main.log");
        checker.setHost("production-market-report-sas-1.sas.yp-c.yandex.net");
        checker.setLogBrokerTopic("market-search@market-report-main-log");

        String line = "tskv\ttskv_format=market-report-access-log\tunixtime=1653950601\tevent_time=[Tue May 31 " +
            "01:43:21 2022]\turl=http://sas2-4926-749-sas-market-test--b33-17050.gencfg-c.yandex" +
            ".net:17051/yandsearch?ip=127.0.0.1&place=offerinfo&feed_shoffer_id=200836573-*&fesh=10781189&rids=213" +
            "&regset=1&pp=18&show-booking-outlets=0&adult=0&numdoc=100&show-model-card-params=1&showdiscounts=1&cpa" +
            "-category-filter=0&strip_query_language=0&show-promoted=1&show-min-quantity=1&show-urls=decrypted&client" +
            "=checkout&co-from=checkouter&show-filter-mark=specifiedForOffer&show-preorder=0&rgb=BLUE&rearr-factors" +
            "=combinator%3D1&use-virt-shop=0\tcookies=\tsearch_elapsed=18\tuser_agent=okhttp/3.14" +
            ".9\tuser_agent_hash=2541706998\tremote_addr=2a02:6b8:c04:1e7:0:522:579c:3605\tfull_elapsed=18" +
            "\ttotal_documents_processed=6\ttotal_documents_cpc=0\ttotal_documents_cpa=44\ttotal_documents_accepted=6" +
            "\ttotal_rendered=0\treq_wiz_time=-1\twizards=\treqid=\tredirect_info=\terror_info=\tbase_search_elapsed" +
            "=10\tmeta_search_elapsed=6\thave_trimmed_field=0\tfetch_time=0\tsnippet_requests_made=0" +
            "\tsnippets_fetched=0\turl_hash=0df2f07a56515e2fb1c33e411a55bc0f\ttest_ids=\tquery_corrected_by_speller" +
            "=\tfuzzy_search_used=0\tx_market_req_id=\tunixtime_ms" +
            "=1653950601048\treq_wiz_count=0\tproduct_type=NONE\texternal_requests_time=0\ttotal_renderable=2" +
            "\twizard_elements=\ticookie=\trgb=GREEN\tpp=18\trequest_body=\tpartial_answer=0" +
            "\testimated_max_memory_usage=0\tenv=production\tlocation=sas\tsub_role=market\tcluster=0\thost=0" +
            "\tresponse_size_bytes=28205\tcpu_time_us=92977\tcpu_time_us_meta=13698\tproduct_request" +
            "=\texternal_snippet_stall_time_ms=0\tapproximate_network_usage=230482\twait_time_us=90\tresource_meta" +
            "=\tclient=\tclient_page_id=\tclient_scenario=\tis_suspicious=0\tdocs_before_accept=44\texp_info" +
            "=\tsource_role=market\tcloud_service=test_report_market_sas\tmajor_faults=0\tis_antirobot_degradation=0" +
            "\tsearch_type=META_AND_BASE\tsmm=1\tbasesearch_called=2";


        Date date = dateFormat.parse("2022-05-31T01:43:21+0300");
        checker.check(
            line,
            date,
            LocalDateTime.parse("2022-05-31T01:43:21.066"), // time
            "market-report", // project
            "market-report", // service
            "{\"search_elapsed\":18,\"total_documents_processed\":6," +
                "\"url_hash\":\"0df2f07a56515e2fb1c33e411a55bc0f\",\"partial_answer\":false," +
                "\"response_size_bytes\":28205,\"major_faults\":0}", //
            // message
            "prod", // env
            "", // cluster
            Level.INFO, // level
            checker.getHost(), // hostname
            "", // version
            "SAS", // dc
            "", // request_id
            "", // trace_id
            "", // span_id
            "market-report-main.log", // component
            UUID.nameUUIDFromBytes(line.getBytes()), // record_id
            "", // validation_err
            "{\"place\":\"offerinfo\",\"url_hash\":\"0df2f07a56515e2fb1c33e411a55bc0f\"," +
                "\"url\":\"http://sas2-4926-749-sas-market-test--b33-17050.gencfg-c.yandex" +
                ".net:17051/yandsearch?ip=127.0.0.1&place=offerinfo&feed_shoffer_id=200836573-*&fesh=10781189&rids" +
                "=213&regset=1&pp=18&show-booking-outlets=0&adult=0&numdoc=100&show-model-card-params=1&showdiscounts" +
                "=1&cpa-category-filter=0&strip_query_language=0&show-promoted=1&show-min-quantity=1&show-urls" +
                "=decrypted&client=checkout&co-from=checkouter&show-filter-mark=specifiedForOffer&show-preorder=0&rgb" +
                "=BLUE&rearr-factors=combinator%3D1&use-virt-shop=0\",\"user_agent\":\"okhttp/3.14.9\"," +
                "\"remote_addr\":\"2a02:6b8:c04:1e7:0:522:579c:3605\",\"full_elapsed\":18,\"total_documents_cpc\":0," +
                "\"total_documents_cpa\":44,\"total_documents_accepted\":6,\"total_rendered\":0,\"req_wiz_time\":-1," +
                "\"is_redirect\":false,\"base_search_elapsed\":10,\"meta_search_elapsed\":6,\"fetch_time\":0," +
                "\"external_requests_time\":0,\"total_renderable\":2,\"rgb\":\"blue\",\"pp\":18," +
                "\"estimated_max_memory_usage\":0,\"source_role\":\"market\",\"cluster\":0,\"cpu_time_us\":92977," +
                "\"cpu_time_us_meta\":13698,\"external_snippet_stall_time_ms\":0," +
                "\"approximate_network_usage\":230482,\"wait_time_us\":90,\"client\":\"checkout\"," +
                "\"is_suspicious\":false,\"is_antirobot_degradation\":false,\"pruncount\":0,\"smm\":1.0," +
                "\"basesearch_called\":2}" // rest
        );
    }

    @Test
    void thenMissingAdditionalInfoShouldParseCorrectly() throws Exception {
        checker.setFile("market-report-main.log");
        checker.setHost("production-market-report-sas-1.sas.yp-c.yandex.net");
        checker.setLogBrokerTopic("market-search@market-report-main-log");

        String line = "tskv\ttskv_format=market-report-access-log\tunixtime=1653950601\tevent_time=[Tue May 31 " +
            "01:43:21 2022]\turl=http://sas2-4926-749-sas-market-test--b33-17050.gencfg-c.yandex" +
            ".net:17051/yandsearch?ip=127.0.0.1&place=offerinfo&feed_shoffer_id=200836573-*&fesh=10781189&rids=213" +
            "&regset=1&pp=18&show-booking-outlets=0&adult=0&numdoc=100&show-model-card-params=1&showdiscounts=1&cpa" +
            "-category-filter=0&strip_query_language=0&show-promoted=1&show-min-quantity=1&show-urls=decrypted&client" +
            "=checkout&co-from=checkouter&show-filter-mark=specifiedForOffer&show-preorder=0&rgb=BLUE&rearr-factors" +
            "=combinator%3D1&use-virt-shop=0\tcookies=\tsearch_elapsed=18\tuser_agent=okhttp/3.14" +
            ".9\tuser_agent_hash=2541706998\tremote_addr=2a02:6b8:c04:1e7:0:522:579c:3605\tfull_elapsed=18" +
            "\ttotal_documents_processed=6\ttotal_documents_cpc=0\ttotal_documents_cpa=44\ttotal_documents_accepted=6" +
            "\ttotal_rendered=0\treq_wiz_time=-1\twizards=\treqid=\tredirect_info=\terror_info=\tbase_search_elapsed" +
            "=10\tmeta_search_elapsed=6\thave_trimmed_field=0\tfetch_time=0\tsnippet_requests_made=0" +
            "\tsnippets_fetched=0\turl_hash=0df2f07a56515e2fb1c33e411a55bc0f\ttest_ids=\tquery_corrected_by_speller" +
            "=\tfuzzy_search_used=0\tx_market_req_id=1653950601036/873618b2f89612483293e756d0fc5ddc\tunixtime_ms" +
            "=1653950601048\treq_wiz_count=0\tproduct_type=NONE\texternal_requests_time=0\ttotal_renderable=2" +
            "\twizard_elements=\ticookie=\trgb=GREEN\tpp=18\trequest_body=\tpartial_answer=0" +
            "\testimated_max_memory_usage=0\tenv=production\tlocation=sas\tsub_role=market\tcluster=0\thost=0" +
            "\tresponse_size_bytes=28205\tcpu_time_us=92977\tcpu_time_us_meta=13698\tproduct_request" +
            "=\texternal_snippet_stall_time_ms=0\tapproximate_network_usage=230482\twait_time_us=90\tresource_meta" +
            "=\tclient=\tclient_page_id=\tclient_scenario=\tis_suspicious=0\tdocs_before_accept=44\texp_info" +
            "=\tsource_role=market\tcloud_service=test_report_market_sas\tmajor_faults=0\tis_antirobot_degradation=0" +
            "\tsearch_type=META_AND_BASE";


        Date date = dateFormat.parse("2022-05-31T01:43:21+0300");
        checker.check(
            line,
            date,
            LocalDateTime.parse("2022-05-31T01:43:21.066"), // time
            "market-report", // project
            "market-report", // service
            "{\"search_elapsed\":18,\"total_documents_processed\":6," +
                "\"url_hash\":\"0df2f07a56515e2fb1c33e411a55bc0f\",\"partial_answer\":false," +
                "\"response_size_bytes\":28205,\"major_faults\":0}", //
            // message
            "prod", // env
            "", // cluster
            Level.INFO, // level
            checker.getHost(), // hostname
            "", // version
            "SAS", // dc
            "1653950601036/873618b2f89612483293e756d0fc5ddc", // request_id
            "", // trace_id
            "", // span_id
            "market-report-main.log", // component
            UUID.nameUUIDFromBytes(line.getBytes()), // record_id
            "", // validation_err
            "{\"place\":\"offerinfo\",\"url_hash\":\"0df2f07a56515e2fb1c33e411a55bc0f\"," +
                "\"url\":\"http://sas2-4926-749-sas-market-test--b33-17050.gencfg-c.yandex" +
                ".net:17051/yandsearch?ip=127.0.0.1&place=offerinfo&feed_shoffer_id=200836573-*&fesh=10781189&rids" +
                "=213&regset=1&pp=18&show-booking-outlets=0&adult=0&numdoc=100&show-model-card-params=1&showdiscounts" +
                "=1&cpa-category-filter=0&strip_query_language=0&show-promoted=1&show-min-quantity=1&show-urls" +
                "=decrypted&client=checkout&co-from=checkouter&show-filter-mark=specifiedForOffer&show-preorder=0&rgb" +
                "=BLUE&rearr-factors=combinator%3D1&use-virt-shop=0\",\"user_agent\":\"okhttp/3.14.9\"," +
                "\"remote_addr\":\"2a02:6b8:c04:1e7:0:522:579c:3605\",\"full_elapsed\":18,\"total_documents_cpc\":0," +
                "\"total_documents_cpa\":44,\"total_documents_accepted\":6,\"total_rendered\":0,\"req_wiz_time\":-1," +
                "\"is_redirect\":false,\"base_search_elapsed\":10,\"meta_search_elapsed\":6,\"fetch_time\":0," +
                "\"external_requests_time\":0,\"total_renderable\":2,\"rgb\":\"blue\",\"pp\":18," +
                "\"estimated_max_memory_usage\":0,\"source_role\":\"market\",\"cluster\":0,\"cpu_time_us\":92977," +
                "\"cpu_time_us_meta\":13698,\"external_snippet_stall_time_ms\":0," +
                "\"approximate_network_usage\":230482,\"wait_time_us\":90,\"client\":\"checkout\"," +
                "\"is_suspicious\":false,\"is_antirobot_degradation\":false,\"pruncount\":0,\"smm\":1.0," +
                "\"basesearch_called\":0}" // rest
        );
    }
}
