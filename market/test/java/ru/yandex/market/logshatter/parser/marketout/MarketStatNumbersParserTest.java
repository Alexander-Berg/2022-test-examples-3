package ru.yandex.market.logshatter.parser.marketout;

import java.util.Date;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;

public class MarketStatNumbersParserTest {

    private LogParserChecker checker = new LogParserChecker(new MarketStatNumbersParser());


    @Test
    public void testAllFields() throws Exception {

        String line = "tskv\ttskv_format=market-report-access-log\tunixtime=1468339259\tevent_time=[Tue Jul 12 " +
            "19:00:59 2016]\turl=http://inferno.yandex" +
            ".ru:59157/yandsearch?pp=18&bsformat=1&place=prime&warehouse-id=11&supplier-id=12&hid=1233&filter-express" +
            "-delivery=1&rids=213&ip=127.0.0.1&stat-block-id=test&type=card_hybrid&clid=788&shop-promo-id=qwe123%20," +
            "abc456,%20xyz%2C%20%23zxc\tcookies=\tsearch_elapsed=10\tuser_agent=\tuser_agent_hash=517762881" +
            "\tremote_addr=::1\tfull_elapsed=10\ttotal_documents_processed=5\ttotal_documents_accepted=1912" +
            "\ttotal_rendered=1\treq_wiz_time=-1\twizards=wiz_offers\treqid=test\tredirect_info=0\terror_info" +
            "=\tbase_search_elapsed=4\tmeta_search_elapsed=0\thave_trimmed_field=0\tfetch_time=30" +
            "\tsnippet_requests_made=0\tsnippets_fetched=631\turl_hash=dd5baa7487c74a98c679777f3a073f1f\ttest_ids" +
            "=\tquery_corrected_by_speller=\tfuzzy_search_used=0\tx_market_req_id=market/42/21\tunixtime_ms" +
            "=1468339259000\texternal_requests_time=42\tpartial_answer=1\testimated_max_memory_usage=10000000000\tenv" +
            "=production\tlocation=sas\tsub_role=blue-shadow\tcluster=1\thost=2\tcpu_time_us=123456\tcpu_time_us_meta" +
            "=654321\texternal_snippet_stall_time_ms=1\tapproximate_network_usage=5200969\twait_time_us=789\tclient" +
            "=pokupki.touch\tclient_page_id=blue-market_product\tclient_scenario=fetchSkus\tis_suspicious=1" +
            "\tsource_role=blue-shadow\tcloud_service=int_man\tmajor_faults=123\tresponse_size_bytes=1337" +
            "\tdifferent_doc_used_gta=\tdifferent_snippet_used_gta=\tdifferent_metadoc_used_gta=\trequest_gta_count" +
            "=\tdifferent_doc_attrs_sum_size=\tdifferent_snippet_attrs_sum_size=\tdifferent_metadoc_attrs_sum_size" +
            "=\textra_data_size_sum=\tbasesearch_called=3\ttotal_renderable=7";
        checker.check(
            line,
            new Date(1468339259010L),
            213,  // rids=213
            1233, // hid=1233
            11,   // warehouse-id=11
            12,   // supplier-id=12
            1,    // filter-express-delivery=1
            1912  // total_documents_accepted=1912
        );
    }
}
