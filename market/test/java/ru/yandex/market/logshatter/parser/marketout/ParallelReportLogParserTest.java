package ru.yandex.market.logshatter.parser.marketout;

import java.util.Date;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;

public class ParallelReportLogParserTest {

    private LogParserChecker checker = new LogParserChecker(new ParallelReportLogParser());


    @Test
    @SuppressWarnings("MethodLength")
    public void testTskvParse() throws Exception {
        String line1 = "tskv\ttskv_format=market-report-access-log\tunixtime=1468339259\tevent_time=[Tue Jul 12 " +
            "19:00:59 2016]\turl=http://inferno.yandex" +
            ".ru:59157/yandsearch?pp=18&text=kijanka&bsformat=1&place=parallel&ip=127.0.0" +
            ".1&stat-block-id=test&type=card_hybrid&clid=788\tcookies=\tsearch_elapsed=10\tuser_agent" +
            "=\tuser_agent_hash=517762881\tremote_addr=::1\tfull_elapsed=10\ttotal_documents_processed=5" +
            "\ttotal_documents_accepted=5\ttotal_rendered=1\treq_wiz_time=-1\twizards=wiz_offers\treqid=test" +
            "\tredirect_info=0\terror_info=\tbase_search_elapsed=4\tmeta_search_elapsed=0\thave_trimmed_field=0" +
            "\tfetch_time=30\tsnippet_requests_made=0\tsnippets_fetched=631\turl_hash" +
            "=dd5baa7487c74a98c679777f3a073f1f\ttest_ids=\tquery_corrected_by_speller=\tfuzzy_search_used=0" +
            "\tx_market_req_id=market/42/21\tunixtime_ms=1468339259000\texternal_requests_time=42\tpartial_answer=1" +
            "\testimated_max_memory_usage=10000000000\tenv=production\tlocation=sas\tsub_role=blue-shadow\tcluster=1" +
            "\thost=2\tcpu_time_us=123456\tcpu_time_us_meta=654321\texternal_snippet_stall_time_ms=1" +
            "\tapproximate_network_usage=5200969\twait_time_us=789\tclient=pokupki" +
            ".touch\tclient_page_id=blue-market_product\tclient_scenario=fetchSkus\tis_suspicious=1\tsource_role=blue" +
            "-shadow\tcloud_service=int_man\tmajor_faults=123\tresponse_size_bytes=1337\tdifferent_doc_used_gta" +
            "=\tdifferent_snippet_used_gta=\tdifferent_metadoc_used_gta=\trequest_gta_count" +
            "=\tdifferent_doc_attrs_sum_size=\tdifferent_snippet_attrs_sum_size=\tdifferent_metadoc_attrs_sum_size" +
            "=\textra_data_size_sum=\tbasesearch_called=3";
        checker.check(
            line1,
            new Date(1468339259010L),
            checker.getHost(),
            10, 10, 4, 0,
            1, 5, 5,
            -1, false, false, 1,
            "parallel", 18, 0, "test",
            new String[]{"wiz_offers"},
            30, 631, 42, "card_hybrid", false, true, 10000000000L,
            new Integer[0],
            "production", 1, 788, "NONE" /* bot */,
            123456L /* cpu_time_us */,
            1 /* external_snippet_stall_time_ms */,
            5200969 /* approximate_network_usage */,
            false /* batch-bids-recommendations */,
            789L /* wait_time_us */,
            "pokupki.touch" /* client */,
            "white" /* rgb */,
            "blue-market_product" /* client_page_id */,
            "fetchSkus" /* client_scenario */,
            true /* is_suspicious */,
            "blue-shadow" /* source_role */,
            0 /* numdoc */,
            "int_man" /* cloud_service */,
            123 /* major_faults */,
            1337 /* response_size_bytes */,
            new String[]{}, /* different_doc_used_gta */
            new String[]{}, /* different_snippet_used_gta */
            new String[]{}, /* different_metadoc_used_gta */
            0, /* request_gta_count */
            0, /* different_doc_attrs_sum_size */
            0, /* different_snippet_attrs_sum_size */
            0, /* different_metadoc_attrs_sum_size */
            0, /* extra_data_size_sum */
            3 /* basesearch_count */,
            654321L /* cpu_time_us_meta */
        );

        String lineWithoutClid = "tskv\ttskv_format=market-report-access-log\tunixtime=1468339259\tevent_time=[Tue " +
            "Jul 12 19:00:59 2016]\turl=http://inferno.yandex" +
            ".ru:59157/yandsearch?pp=18&text=kijanka&bsformat=1&place=parallel&ip=127.0.0" +
            ".1&stat-block-id=test&type=card_hybrid&batch-bids-recommendations=1\tcookies=\tsearch_elapsed=10" +
            "\tuser_agent=\tuser_agent_hash=517762881\tremote_addr=::1\tfull_elapsed=10\ttotal_documents_processed=5" +
            "\ttotal_documents_accepted=5\ttotal_rendered=1\treq_wiz_time=-1\twizards=wiz_offers\treqid=test" +
            "\tredirect_info=0\terror_info=\tbase_search_elapsed=4\tmeta_search_elapsed=0\thave_trimmed_field=0" +
            "\tfetch_time=30\tsnippet_requests_made=0\tsnippets_fetched=631\turl_hash" +
            "=dd5baa7487c74a98c679777f3a073f1f\ttest_ids=\tquery_corrected_by_speller=\tfuzzy_search_used=0" +
            "\tx_market_req_id=market/42/21\tunixtime_ms=1468339259000\texternal_requests_time=42\tpartial_answer=1" +
            "\testimated_max_memory_usage=10000000000\tenv=production\tlocation=sas\tsub_role=blue-shadow\tcluster=1" +
            "\thost=2\tcpu_time_us=123456\tcpu_time_us_meta=654321\texternal_snippet_stall_time_ms=1" +
            "\tapproximate_network_usage=5200969\twait_time_us=789\tsource_role=parallel";
        checker.check(
            lineWithoutClid,
            new Date(1468339259010L),
            checker.getHost(),
            10, 10, 4, 0,
            1, 5, 5,
            -1, false, false, 1,
            "parallel", 18, 0, "test",
            new String[]{"wiz_offers"},
            30, 631, 42, "card_hybrid", false, true, 10000000000L,
            new Integer[0],
            "production", 1,
            0 /* clid */, "NONE" /* bot */,
            123456L /* cpu_time_us */,
            1 /* external_snippet_stall_time_ms */,
            5200969 /* approximate_network_usage */,
            true /* batch-bids-recommendations */,
            789L /* wait_time_us */,
            "OTHER" /* client */,
            "white" /* rgb */,
            "" /* client_page_id */,
            "" /* client_scenario */,
            false /* is_suspicious */,
            "parallel" /* source_role */,
            0 /* numdoc */,
            "" /* cloud_service */,
            0 /* major_faults */,
            0 /* response_size_bytes */,
            new String[]{}, /* different_doc_used_gta */
            new String[]{}, /* different_snippet_used_gta */
            new String[]{}, /* different_metadoc_used_gta */
            0, /* request_gta_count */
            0, /* different_doc_attrs_sum_size */
            0, /* different_snippet_attrs_sum_size */
            0, /* different_metadoc_attrs_sum_size */
            0, /* extra_data_size_sum */
            0,
            654321L /* cpu_time_us_meta */
        );

        String lineWithClidVid = "tskv\ttskv_format=market-report-access-log\tunixtime=1468339259\tevent_time=[Tue " +
            "Jul 12 19:00:59 2016]\turl=http://inferno.yandex" +
            ".ru:59157/yandsearch?pp=18&text=kijanka&bsformat=1&place=parallel&ip=127.0.0" +
            ".1&stat-block-id=test&type=card_hybrid&clid=766-89&content-api-client=18932\tcookies=\tsearch_elapsed=10" +
            "\tuser_agent=\tuser_agent_hash=517762881\tremote_addr=::1\tfull_elapsed=10\ttotal_documents_processed=5" +
            "\ttotal_documents_accepted=5\ttotal_rendered=1\treq_wiz_time=-1\twizards=wiz_offers\treqid=test" +
            "\tredirect_info=0\terror_info=\tbase_search_elapsed=4\tmeta_search_elapsed=0\thave_trimmed_field=0" +
            "\tfetch_time=30\tsnippet_requests_made=0\tsnippets_fetched=631\turl_hash" +
            "=dd5baa7487c74a98c679777f3a073f1f\ttest_ids=\tquery_corrected_by_speller=\tfuzzy_search_used=0" +
            "\tx_market_req_id=market/42/21\tunixtime_ms=1468339259000\texternal_requests_time=42\tpartial_answer=1" +
            "\testimated_max_memory_usage=10000000000\tenv=production\tlocation=sas\tsub_role=blue-shadow\tcluster=1" +
            "\thost=2\tcpu_time_us=123456\tcpu_time_us_meta=654321\texternal_snippet_stall_time_ms=1" +
            "\tapproximate_network_usage=5200969\twait_time_us=789\tsource_role=bk";
        checker.check(
            lineWithClidVid,
            new Date(1468339259010L),
            checker.getHost(),
            10, 10, 4, 0,
            1, 5, 5,
            -1, false, false, 1,
            "parallel", 18, 0, "test",
            new String[]{"wiz_offers"},
            30, 631, 42, "card_hybrid", false, true, 10000000000L,
            new Integer[0],
            "production", 1,
            766 /* clid */, "NONE" /* bot */,
            123456L /* cpu_time_us */,
            1 /* external_snippet_stall_time_ms */,
            5200969 /* approximate_network_usage */,
            false /* batch-bids-recommendations */,
            789L /* wait_time_us */,
            "OTHER" /* client */,
            "white" /* rgb */,
            "" /* client_page_id */,
            "" /* client_scenario */,
            false /* is_suspicious */,
            "bk" /* source_role */,
            0 /* numdoc */,
            ""/* cloud_service */,
            0 /* major_faults */,
            0 /* response_size_bytes */,
            new String[]{}, /* different_doc_used_gta */
            new String[]{}, /* different_snippet_used_gta */
            new String[]{}, /* different_metadoc_used_gta */
            0, /* request_gta_count */
            0, /* different_doc_attrs_sum_size */
            0, /* different_snippet_attrs_sum_size */
            0, /* different_metadoc_attrs_sum_size */
            0, /* extra_data_size_sum */
            0,
            654321L /* cpu_time_us_meta */
        );

        String lineWithBot = "tskv\ttskv_format=market-report-access-log\tunixtime=1468339259\tevent_time=[Tue Jul 12" +
            " 19:00:59 2016]\turl=http://inferno.yandex" +
            ".ru:59157/yandsearch?pp=18&text=kijanka&bsformat=1&place=parallel&ip=127.0.0" +
            ".1&stat-block-id=test&type=card_hybrid&clid=788&client=widget\tcookies=\tsearch_elapsed=10\tuser_agent" +
            "=Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)" +
            "\tuser_agent_hash=3812692112\tremote_addr=::1\tfull_elapsed=10\ttotal_documents_processed=5" +
            "\ttotal_documents_accepted=5\ttotal_rendered=1\treq_wiz_time=-1\twizards=wiz_offers\treqid=test" +
            "\tredirect_info=0\terror_info=\tbase_search_elapsed=4\tmeta_search_elapsed=0\thave_trimmed_field=0" +
            "\tfetch_time=30\tsnippet_requests_made=0\tsnippets_fetched=631\turl_hash" +
            "=dd5baa7487c74a98c679777f3a073f1f\ttest_ids=\tquery_corrected_by_speller=\tfuzzy_search_used=0" +
            "\tx_market_req_id=market/42/21\tunixtime_ms=1468339259000\texternal_requests_time=42\tpartial_answer=1" +
            "\testimated_max_memory_usage=10000000000\tenv=production\tlocation=sas\tsub_role=blue-shadow\tcluster=1" +
            "\thost=2\tcpu_time_us=123456\tcpu_time_us_meta=654321\texternal_snippet_stall_time_ms=1" +
            "\tapproximate_network_usage=5200969\twait_time_us=789";
        checker.check(
            lineWithBot,
            new Date(1468339259010L),
            checker.getHost(),
            10, 10, 4, 0,
            1, 5, 5,
            -1, false, false, 1,
            "parallel", 18, 0, "test",
            new String[]{"wiz_offers"},
            30, 631, 42, "card_hybrid", false, true, 10000000000L,
            new Integer[0],
            "production", 1, 788, "GOOGLE" /* bot */,
            123456L /* cpu_time_us */,
            1 /* external_snippet_stall_time_ms */,
            5200969 /* approximate_network_usage */,
            false /* batch-bids-recommendations */,
            789L /* wait_time_us */,
            "widget" /* client */,
            "white" /* rgb */,
            "" /* client_page_id */,
            "" /* client_scenario */,
            false /* is_suspicious */,
            "" /* source_role */,
            0 /* numdoc */,
            "" /* cloud_service */,
            0 /* major_faults */,
            0 /* response_size_bytes */,
            new String[]{}, /* different_doc_used_gta */
            new String[]{}, /* different_snippet_used_gta */
            new String[]{}, /* different_metadoc_used_gta */
            0, /* request_gta_count */
            0, /* different_doc_attrs_sum_size */
            0, /* different_snippet_attrs_sum_size */
            0, /* different_metadoc_attrs_sum_size */
            0, /* extra_data_size_sum */
            0,
            654321L /* cpu_time_us_meta */
        );

        String lineWithOptimizationInfo = "tskv\ttskv_format=market-report-access-log\tunixtime=1468339259" +
            "\tevent_time=[Tue Jul 12 19:00:59 2016]\turl=http://inferno.yandex" +
            ".ru:59157/yandsearch?pp=18&text=kijanka&bsformat=1&place=parallel&ip=127.0.0" +
            ".1&stat-block-id=test&type=card_hybrid&clid=788\tcookies=\tsearch_elapsed=10\tuser_agent" +
            "=\tuser_agent_hash=517762881\tremote_addr=::1\tfull_elapsed=10\ttotal_documents_processed=5" +
            "\ttotal_documents_accepted=5\ttotal_rendered=1\treq_wiz_time=-1\twizards=wiz_offers\treqid=test" +
            "\tredirect_info=0\terror_info=\tbase_search_elapsed=4\tmeta_search_elapsed=0\thave_trimmed_field=0" +
            "\tfetch_time=30\tsnippet_requests_made=0\tsnippets_fetched=631\turl_hash" +
            "=dd5baa7487c74a98c679777f3a073f1f\ttest_ids=\tquery_corrected_by_speller=\tfuzzy_search_used=0" +
            "\tx_market_req_id=market/42/21\tunixtime_ms=1468339259000\texternal_requests_time=42\tpartial_answer=1" +
            "\testimated_max_memory_usage=10000000000\tenv=production\tlocation=sas\tsub_role=blue-shadow\tcluster=1" +
            "\thost=2\tcpu_time_us=123456\tcpu_time_us_meta=654321\texternal_snippet_stall_time_ms=1" +
            "\tapproximate_network_usage=5200969\twait_time_us=789\tclient=pokupki" +
            ".touch\tclient_page_id=blue-market_product\tclient_scenario=fetchSkus\tis_suspicious=1\tsource_role=blue" +
            "-shadow\tcloud_service=int_man\tmajor_faults=123\tresponse_size_bytes=1337\tdifferent_doc_used_gta=a1," +
            "b1\tdifferent_snippet_used_gta=a2,b2\tdifferent_metadoc_used_gta=a3," +
            "b3\trequest_gta_count=44\tdifferent_doc_attrs_sum_size=55\tdifferent_snippet_attrs_sum_size=66" +
            "\tdifferent_metadoc_attrs_sum_size=77\textra_data_size_sum=88\tbasesearch_called=111";
        checker.check(
            lineWithOptimizationInfo,
            new Date(1468339259010L),
            checker.getHost(),
            10, 10, 4, 0,
            1, 5, 5,
            -1, false, false, 1,
            "parallel", 18, 0, "test",
            new String[]{"wiz_offers"},
            30, 631, 42, "card_hybrid", false, true, 10000000000L,
            new Integer[0],
            "production", 1, 788, "NONE" /* bot */,
            123456L /* cpu_time_us */,
            1 /* external_snippet_stall_time_ms */,
            5200969 /* approximate_network_usage */,
            false /* batch-bids-recommendations */,
            789L /* wait_time_us */,
            "pokupki.touch" /* client */,
            "white" /* rgb */,
            "blue-market_product" /* client_page_id */,
            "fetchSkus" /* client_scenario */,
            true /* is_suspicious */,
            "blue-shadow" /* source_role */,
            0 /* numdoc */,
            "int_man" /* cloud_service */,
            123 /* major_faults */,
            1337 /* response_size_bytes */,
            new String[]{"a1", "b1"}, /* different_doc_used_gta */
            new String[]{"a2", "b2"}, /* different_snippet_used_gta */
            new String[]{"a3", "b3"}, /* different_metadoc_used_gta */
            44, /* request_gta_count */
            55, /* different_doc_attrs_sum_size */
            66, /* different_snippet_attrs_sum_size */
            77, /* different_metadoc_attrs_sum_size */
            88, /* extra_data_size_sum */
            111,
            654321L /* cpu_time_us_meta */
        );

        checker.check(
            "tskv\ttskv_format=market-report-access-log\tunixtime=1474029401\tevent_time=[Fri Sep 16 15:36:41 " +
                "2016]\turl=http://msh-par10h.market.yandex" +
                ".net:17053/yandsearch?place=consistency_check\tcookies=\tsearch_elapsed=6\tuser_agent" +
                "=\tuser_agent_hash=517762881\tremote_addr=::1\tfull_elapsed=6\ttotal_documents_processed=0" +
                "\ttotal_documents_accepted=0\ttotal_rendered=0\treq_wiz_time=-1\twizards=\treqid=\tredirect_info=0" +
                "\terror_info=\tbase_search_elapsed=0\tmeta_search_elapsed=0\thave_trimmed_field=0\tfetch_time=2" +
                "\tsnippet_requests_made=1\tsnippets_fetched=34\turl_hash=67ae705fb08dccfe6443a276e3ab0196\ttest_ids" +
                "=\tquery_corrected_by_speller=\tfuzzy_search_used=0\tx_market_req_id=\tunixtime_ms=1474029401539" +
                "\texternal_requests_time=42\tpartial_answer=1\testimated_max_memory_usage=10000000000\tenv" +
                "=production\tlocation=sas\tsub_role=blue-shadow\tcluster=1\thost=2\tcpu_time_us=123456" +
                "\tcpu_time_us_meta=654321\texternal_snippet_stall_time_ms=1\tapproximate_network_usage=5200969" +
                "\twait_time_us=789"
        );
    }

    @Test
    public void testServiceRequests() throws Exception {
        final int serviceRequestFieldIndex = 21;
        checker.check("tskv\ttskv_format=market-report-access-log\tunixtime=1504927759\tevent_time=[Sat Sep  9 " +
            "06:29:19 2017]\turl=http://msh01e.market.yandex" +
            ".net:17051/yandsearch?place=consistency_check\tcookies=\tsearch_elapsed=0\tuser_agent=\tuser_agent_hash" +
            "=517762881\tremote_addr=2a02:6b8:c0e:2c:0:577:4a3b:9de7\tfull_elapsed=0\ttotal_documents_processed=0" +
            "\ttotal_documents_accepted=0\ttotal_rendered=0\treq_wiz_time=-1\twizards=\treqid=\tredirect_info" +
            "=\terror_info=\tbase_search_elapsed=0\tmeta_search_elapsed=0\thave_trimmed_field=0\tfetch_time=0" +
            "\tsnippet_requests_made=0\tsnippets_fetched=0\turl_hash=02c49f2486bfa1d395d6c30299e3071b\ttest_ids" +
            "=\tquery_corrected_by_speller=\tfuzzy_search_used=0\tx_market_req_id=\tunixtime_ms=1504927759388" +
            "\treq_wiz_count=0\tproduct_type=NONE\texternal_requests_time=0\ttotal_renderable=\twizard_elements" +
            "=\tpartial_answer=\testimated_max_memory_usage=10000000000\tenv=production\tlocation=sas\tsub_role=blue" +
            "-shadow\tcluster=1\thost=2\tcpu_time_us=123456\tcpu_time_us_meta=654321\texternal_snippet_stall_time_ms" +
            "=1\tapproximate_network_usage=5200969\twait_time_us=789");
        Assertions.assertTrue(checker.getFields()[serviceRequestFieldIndex].equals(true));
        checker.check("tskv\ttskv_format=market-report-access-log\tunixtime=1504927748\tevent_time=[Sat Sep  9 " +
            "06:29:08 2017]\turl=http://msh01e.market.yandex" +
            ".net:17051/yandsearch?place=report_status\tcookies=\tsearch_elapsed=0\tuser_agent=\tuser_agent_hash" +
            "=517762881\tremote_addr=2a02:6b8:c0e:29:0:577:9ecf:3858\tfull_elapsed=0\ttotal_documents_processed=0" +
            "\ttotal_documents_accepted=0\ttotal_rendered=0\treq_wiz_time=-1\twizards=\treqid=\tredirect_info" +
            "=\terror_info=\tbase_search_elapsed=0\tmeta_search_elapsed=0\thave_trimmed_field=0\tfetch_time=0" +
            "\tsnippet_requests_made=0\tsnippets_fetched=0\turl_hash=caff26ae896a5c4bf37b4dbef1aab16a\ttest_ids" +
            "=\tquery_corrected_by_speller=\tfuzzy_search_used=0\tx_market_req_id=1504927748336" +
            "/0f5e6d97ab5e868ede9f803bea70c74d\tunixtime_ms=1504927748337\treq_wiz_count=0\tproduct_type=NONE" +
            "\texternal_requests_time=0\ttotal_renderable=\twizard_elements=\tpartial_answer" +
            "=\testimated_max_memory_usage=10000000000\tenv=production\tlocation=sas\tsub_role=blue-shadow\tcluster=1" +
            "\thost=2\tcpu_time_us=123456\tcpu_time_us_meta=654321\texternal_snippet_stall_time_ms=1" +
            "\tapproximate_network_usage=5200969\twait_time_us=789");
        Assertions.assertTrue(checker.getFields()[serviceRequestFieldIndex].equals(true));
        checker.check("tskv\ttskv_format=market-report-access-log\tunixtime=1504927921\tevent_time=[Sat Sep  9 " +
            "06:32:01 2017]\turl=http://msh01e.market.yandex.net:17051/yandsearch?place=mainreport&ip=127.0.0" +
            ".1&pp=18&text=nokia&numdoc=3&rids=213&nocache=1&timeout=1000000000&local-sources-only=1\tcookies" +
            "=\tsearch_elapsed=135\tuser_agent=Python-urllib/2" +
            ".7\tuser_agent_hash=4148826853\tremote_addr=::1\tfull_elapsed=136\ttotal_documents_processed=14769" +
            "\ttotal_documents_accepted=1912\ttotal_rendered=0\treq_wiz_time=15\twizards=\treqid=\tredirect_info=0" +
            "\terror_info=\tbase_search_elapsed=77\tmeta_search_elapsed=0\thave_trimmed_field=0\tfetch_time=11" +
            "\tsnippet_requests_made=5\tsnippets_fetched=15\turl_hash=cf9a96a87a6568e1886e7b68ab01ea9b\ttest_ids" +
            "=\tquery_corrected_by_speller=\tfuzzy_search_used=0\tx_market_req_id=\tunixtime_ms=1504927921733" +
            "\treq_wiz_count=1\tproduct_type=NONE\texternal_requests_time=15\ttotal_renderable=1912\twizard_elements" +
            "=\tpartial_answer=\testimated_max_memory_usage=10000000000\tenv=production\tlocation=sas\tsub_role=blue" +
            "-shadow\tcluster=1\thost=2\tcpu_time_us=123456\tcpu_time_us_meta=654321\texternal_snippet_stall_time_ms" +
            "=1\tapproximate_network_usage=5200969\twait_time_us=789");
        Assertions.assertTrue(checker.getFields()[serviceRequestFieldIndex].equals(true));
        checker.check("tskv\ttskv_format=market-report-access-log\tunixtime=1504927921\tevent_time=[Sat Sep  9 " +
            "06:32:01 2017]\turl=http://msh01e.market.yandex.net:17051/yandsearch?place=mainreport&ip=127.0.0" +
            ".1&pp=18&text=nokia&numdoc=3&rids=213&nocache=1&timeout=1000000000&mini-tank=1\tcookies=\tsearch_elapsed" +
            "=135\tuser_agent=Python-urllib/2.7\tuser_agent_hash=4148826853\tremote_addr=::1\tfull_elapsed=136" +
            "\ttotal_documents_processed=14769\ttotal_documents_accepted=1912\ttotal_rendered=0\treq_wiz_time=15" +
            "\twizards=\treqid=\tredirect_info=0\terror_info=\tbase_search_elapsed=77\tmeta_search_elapsed=0" +
            "\thave_trimmed_field=0\tfetch_time=11\tsnippet_requests_made=5\tsnippets_fetched=15\turl_hash" +
            "=cf9a96a87a6568e1886e7b68ab01ea9b\ttest_ids=\tquery_corrected_by_speller=\tfuzzy_search_used=0" +
            "\tx_market_req_id=\tunixtime_ms=1504927921733\treq_wiz_count=1\tproduct_type=NONE" +
            "\texternal_requests_time=15\ttotal_renderable=1912\twizard_elements=\tpartial_answer" +
            "=\testimated_max_memory_usage=10000000000\tenv=production\tlocation=sas\tsub_role=blue-shadow\tcluster=1" +
            "\thost=2\tcpu_time_us=123456\tcpu_time_us_meta=654321\texternal_snippet_stall_time_ms=1" +
            "\tapproximate_network_usage=5200969\twait_time_us=789");
        Assertions.assertTrue(checker.getFields()[serviceRequestFieldIndex].equals(true));
    }

    @Test
    public void testNumdoc() throws Exception {
        final int numdocFieldIndex = 40;

        final String withNumdoc = "tskv\ttskv_format=market-report-access-log\tunixtime=1504927921\tevent_time=[Sat " +
            "Sep  9 06:32:01 2017]\turl=http://msh01e.market.yandex.net:17051/yandsearch?place=mainreport&ip=127.0.0" +
            ".1&pp=18&text=nokia&numdoc=3&rids=213&nocache=1&timeout=1000000000&local-sources-only=1\tcookies" +
            "=\tsearch_elapsed=135\tuser_agent=Python-urllib/2" +
            ".7\tuser_agent_hash=4148826853\tremote_addr=::1\tfull_elapsed=136\ttotal_documents_processed=14769" +
            "\ttotal_documents_accepted=1912\ttotal_rendered=0\treq_wiz_time=15\twizards=\treqid=\tredirect_info=0" +
            "\terror_info=\tbase_search_elapsed=77\tmeta_search_elapsed=0\thave_trimmed_field=0\tfetch_time=11" +
            "\tsnippet_requests_made=5\tsnippets_fetched=15\turl_hash=cf9a96a87a6568e1886e7b68ab01ea9b\ttest_ids" +
            "=\tquery_corrected_by_speller=\tfuzzy_search_used=0\tx_market_req_id=\tunixtime_ms=1504927921733" +
            "\treq_wiz_count=1\tproduct_type=NONE\texternal_requests_time=15\ttotal_renderable=1912\twizard_elements" +
            "=\tpartial_answer=\testimated_max_memory_usage=10000000000\tenv=production\tlocation=sas\tsub_role=blue" +
            "-shadow\tcluster=1\thost=2\tcpu_time_us=123456\tcpu_time_us_meta=654321\texternal_snippet_stall_time_ms" +
            "=1\tapproximate_network_usage=5200969\twait_time_us=789";
        checker.check(withNumdoc);
        Assertions.assertTrue(checker.getFields()[numdocFieldIndex].equals(3));

        final String withoutNumdoc = "tskv\ttskv_format=market-report-access-log\tunixtime=1468339259\tevent_time" +
            "=[Tue Jul 12 19:00:59 2016]\turl=http://inferno.yandex" +
            ".ru:59157/yandsearch?pp=18&text=kijanka&bsformat=1&place=parallel&ip=127.0.0" +
            ".1&stat-block-id=test&type=card_hybrid&clid=788&client=widget\tcookies=\tsearch_elapsed=10\tuser_agent" +
            "=Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)" +
            "\tuser_agent_hash=3812692112\tremote_addr=::1\tfull_elapsed=10\ttotal_documents_processed=5" +
            "\ttotal_documents_accepted=5\ttotal_rendered=1\treq_wiz_time=-1\twizards=wiz_offers\treqid=test" +
            "\tredirect_info=0\terror_info=\tbase_search_elapsed=4\tmeta_search_elapsed=0\thave_trimmed_field=0" +
            "\tfetch_time=30\tsnippet_requests_made=0\tsnippets_fetched=631\turl_hash" +
            "=dd5baa7487c74a98c679777f3a073f1f\ttest_ids=\tquery_corrected_by_speller=\tfuzzy_search_used=0" +
            "\tx_market_req_id=market/42/21\tunixtime_ms=1468339259000\texternal_requests_time=42\tpartial_answer=1" +
            "\testimated_max_memory_usage=10000000000\tenv=production\tlocation=sas\tsub_role=blue-shadow\tcluster=1" +
            "\thost=2\tcpu_time_us=123456\tcpu_time_us_meta=654321\texternal_snippet_stall_time_ms=1" +
            "\tapproximate_network_usage=5200969\twait_time_us=789";
        checker.check(withoutNumdoc);
        Assertions.assertTrue(checker.getFields()[numdocFieldIndex].equals(0));
    }

    @Test
    public void testRgb() throws Exception {
        final int rgbFieldIndex = 35;
        checker.check("tskv\ttskv_format=market-report-access-log\tunixtime=1504927759\tevent_time=[Sat Sep  9 " +
            "06:29:19 2017]\turl=http://msh01e.market.yandex" +
            ".net:17051/yandsearch?place=prime\tcookies=\tsearch_elapsed=0\tuser_agent=\tuser_agent_hash=517762881" +
            "\tremote_addr=2a02:6b8:c0e:2c:0:577:4a3b:9de7\tfull_elapsed=0\ttotal_documents_processed=0" +
            "\ttotal_documents_accepted=0\ttotal_rendered=0\treq_wiz_time=-1\twizards=\treqid=\tredirect_info" +
            "=\terror_info=\tbase_search_elapsed=0\tmeta_search_elapsed=0\thave_trimmed_field=0\tfetch_time=0" +
            "\tsnippet_requests_made=0\tsnippets_fetched=0\turl_hash=02c49f2486bfa1d395d6c30299e3071b\ttest_ids" +
            "=\tquery_corrected_by_speller=\tfuzzy_search_used=0\tx_market_req_id=\tunixtime_ms=1504927759388" +
            "\treq_wiz_count=0\tproduct_type=NONE\texternal_requests_time=0\ttotal_renderable=\twizard_elements" +
            "=\tpartial_answer=\testimated_max_memory_usage=10000000000\tenv=production\tlocation=sas\tsub_role=blue" +
            "-shadow\tcluster=1\thost=2\tcpu_time_us=123456\tcpu_time_us_meta=654321\texternal_snippet_stall_time_ms" +
            "=1\tapproximate_network_usage=5200969\twait_time_us=789");
        Assertions.assertTrue(checker.getFields()[rgbFieldIndex].equals("white"));
        checker.check("tskv\ttskv_format=market-report-access-log\tunixtime=1504927759\tevent_time=[Sat Sep  9 " +
            "06:29:19 2017]\turl=http://msh01e.market.yandex" +
            ".net:17051/yandsearch?place=prime&rgb=green\tcookies=\tsearch_elapsed=0\tuser_agent=\tuser_agent_hash" +
            "=517762881\tremote_addr=2a02:6b8:c0e:2c:0:577:4a3b:9de7\tfull_elapsed=0\ttotal_documents_processed=0" +
            "\ttotal_documents_accepted=0\ttotal_rendered=0\treq_wiz_time=-1\twizards=\treqid=\tredirect_info" +
            "=\terror_info=\tbase_search_elapsed=0\tmeta_search_elapsed=0\thave_trimmed_field=0\tfetch_time=0" +
            "\tsnippet_requests_made=0\tsnippets_fetched=0\turl_hash=02c49f2486bfa1d395d6c30299e3071b\ttest_ids" +
            "=\tquery_corrected_by_speller=\tfuzzy_search_used=0\tx_market_req_id=\tunixtime_ms=1504927759388" +
            "\treq_wiz_count=0\tproduct_type=NONE\texternal_requests_time=0\ttotal_renderable=\twizard_elements" +
            "=\tpartial_answer=\testimated_max_memory_usage=10000000000\tenv=production\tlocation=sas\tsub_role=blue" +
            "-shadow\tcluster=1\thost=2\tcpu_time_us=123456\tcpu_time_us_meta=654321\texternal_snippet_stall_time_ms" +
            "=1\tapproximate_network_usage=5200969\twait_time_us=789");
        Assertions.assertTrue(checker.getFields()[rgbFieldIndex].equals("white"));
        checker.check("tskv\ttskv_format=market-report-access-log\tunixtime=1504927759\tevent_time=[Sat Sep  9 " +
            "06:29:19 2017]\turl=http://msh01e.market.yandex" +
            ".net:17051/yandsearch?place=prime&rgb=blue\tcookies=\tsearch_elapsed=0\tuser_agent=\tuser_agent_hash" +
            "=517762881\tremote_addr=2a02:6b8:c0e:2c:0:577:4a3b:9de7\tfull_elapsed=0\ttotal_documents_processed=0" +
            "\ttotal_documents_accepted=0\ttotal_rendered=0\treq_wiz_time=-1\twizards=\treqid=\tredirect_info" +
            "=\terror_info=\tbase_search_elapsed=0\tmeta_search_elapsed=0\thave_trimmed_field=0\tfetch_time=0" +
            "\tsnippet_requests_made=0\tsnippets_fetched=0\turl_hash=02c49f2486bfa1d395d6c30299e3071b\ttest_ids" +
            "=\tquery_corrected_by_speller=\tfuzzy_search_used=0\tx_market_req_id=\tunixtime_ms=1504927759388" +
            "\treq_wiz_count=0\tproduct_type=NONE\texternal_requests_time=0\ttotal_renderable=\twizard_elements" +
            "=\tpartial_answer=\testimated_max_memory_usage=10000000000\tenv=production\tlocation=sas\tsub_role=blue" +
            "-shadow\tcluster=1\thost=2\tcpu_time_us=123456\tcpu_time_us_meta=654321\texternal_snippet_stall_time_ms" +
            "=1\tapproximate_network_usage=5200969\twait_time_us=789");
        Assertions.assertTrue(checker.getFields()[rgbFieldIndex].equals("blue"));
        checker.check("tskv\ttskv_format=market-report-access-log\tunixtime=1504927759\tevent_time=[Sat Sep  9 " +
            "06:29:19 2017]\turl=http://msh01e.market.yandex" +
            ".net:17051/yandsearch?place=prime\tcookies=\tsearch_elapsed=0\tuser_agent=\tuser_agent_hash=517762881" +
            "\tremote_addr=2a02:6b8:c0e:2c:0:577:4a3b:9de7\tfull_elapsed=0\ttotal_documents_processed=0" +
            "\ttotal_documents_accepted=0\ttotal_rendered=0\treq_wiz_time=-1\twizards=\treqid=\tredirect_info" +
            "=\terror_info=\tbase_search_elapsed=0\tmeta_search_elapsed=0\thave_trimmed_field=0\tfetch_time=0" +
            "\tsnippet_requests_made=0\tsnippets_fetched=0\turl_hash=02c49f2486bfa1d395d6c30299e3071b\ttest_ids" +
            "=\tquery_corrected_by_speller=\tfuzzy_search_used=0\tx_market_req_id=\tunixtime_ms=1504927759388" +
            "\treq_wiz_count=0\tproduct_type=NONE\texternal_requests_time=0\ttotal_renderable=\twizard_elements" +
            "=\tpartial_answer=\testimated_max_memory_usage=10000000000\tenv=production\tlocation=sas\tsub_role=blue" +
            "-shadow\tcluster=1\thost=2\tcpu_time_us=123456\tcpu_time_us_meta=654321\texternal_snippet_stall_time_ms" +
            "=1\tapproximate_network_usage=5200969\twait_time_us=789\trgb=GREEN");
        Assertions.assertTrue(checker.getFields()[rgbFieldIndex].equals("white"));
        checker.check("tskv\ttskv_format=market-report-access-log\tunixtime=1504927759\tevent_time=[Sat Sep  9 " +
            "06:29:19 2017]\turl=http://msh01e.market.yandex" +
            ".net:17051/yandsearch?place=prime\tcookies=\tsearch_elapsed=0\tuser_agent=\tuser_agent_hash=517762881" +
            "\tremote_addr=2a02:6b8:c0e:2c:0:577:4a3b:9de7\tfull_elapsed=0\ttotal_documents_processed=0" +
            "\ttotal_documents_accepted=0\ttotal_rendered=0\treq_wiz_time=-1\twizards=\treqid=\tredirect_info" +
            "=\terror_info=\tbase_search_elapsed=0\tmeta_search_elapsed=0\thave_trimmed_field=0\tfetch_time=0" +
            "\tsnippet_requests_made=0\tsnippets_fetched=0\turl_hash=02c49f2486bfa1d395d6c30299e3071b\ttest_ids" +
            "=\tquery_corrected_by_speller=\tfuzzy_search_used=0\tx_market_req_id=\tunixtime_ms=1504927759388" +
            "\treq_wiz_count=0\tproduct_type=NONE\texternal_requests_time=0\ttotal_renderable=\twizard_elements" +
            "=\tpartial_answer=\testimated_max_memory_usage=10000000000\tenv=production\tlocation=sas\tsub_role=blue" +
            "-shadow\tcluster=1\thost=2\tcpu_time_us=123456\tcpu_time_us_meta=654321\texternal_snippet_stall_time_ms" +
            "=1\tapproximate_network_usage=5200969\twait_time_us=789\trgb=BLUE");
        Assertions.assertTrue(checker.getFields()[rgbFieldIndex].equals("blue"));
    }
}
