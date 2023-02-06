package ru.yandex.market.logshatter.parser.marketout;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SubplaceReportLogParserTest {

    private LogParserChecker checker = new LogParserChecker(new SubplaceReportLogParser());


    @Test
    public void testTskvParse() throws Exception {
        String line1 = "tskv\ttskv_format=market-report-access-log\tunixtime=1635260217\tunixtime_ms=1635260217345" +
            "\tevent_time=[Tue Oct 26 17:56:57 2021]\tsub_place=NMarketReport::NProductOffers::TProductOffersPlace" +
            "\tsearch_elapsed=0\ttotal_documents_processed=2\ttotal_documents_accepted=2\ttotal_rendered=0" +
            "\tbase_search_elapsed=30\tmeta_search_elapsed=19\turl_hash=11d92b5c62e7b9931d1335df95d55bac" +
            "\tx_market_req_id=1635260217461/51d936abea8bdc11268b60b042cf0500/1/1\ttotal_renderable=1\tenv=production" +
            "\tlocation=man\tsub_role=market\tcluster=4\thost=1\tsource_role=market\tis_antirobot_degradation=0" +
            "\tdifferent_doc_used_gta=_Name_offer_id,blue_height,blue_length,blue_market,blue_weight,blue_width," +
            "business_id,cbid,classifier_magic_id,cluster_id,datasource_id,delivery_priority,delivery_type,doc_type," +
            "downloadable_delivery_type,dsrcid,extra_data,fee,feed_id,hidd,hyper,is_book_now,is_cutprice," +
            "is_recommended,like_new,nid,offer_promo,pessimized_by_delivery_options,pickup_delivery_type," +
            "previously_used,reserve_price,shop_sku,sku,store_delivery_type,ts,ungrouped_hyper_blue,vbid," +
            "vendor_autostrategy,vendor_id,ware_md5\tdifferent_snippet_used_gta=PicturesProtoBase64,_Title,_Url," +
            "cargo_types,countries_of_origin,description,manufacturer_country_ids,min_quantity,step_quantity," +
            "vendor_code,vendor_string\trequest_gta_count=136\tdifferent_doc_attrs_sum_size=3431" +
            "\tdifferent_snippet_attrs_sum_size=956\tdifferent_metadoc_attrs_sum_size=0\textra_data_size_sum=1444" +
            "\tbasesearch_called=3";
        checker.check(
            line1
        );

        assertEquals("NMarketReport::NProductOffers::TProductOffersPlace", checker.getFields()[1]);

        String line2 = "tskv\ttskv_format=market-report-access-log\tunixtime=1635260217\tunixtime_ms=1635260217345" +
            "\tevent_time=[Tue Oct 26 17:56:57 2021]\tsub_place=NMarketReport::NRecom::TModelDataLoaderProxy" +
            "\tsearch_elapsed=0\ttotal_documents_processed=0\ttotal_documents_accepted=0\ttotal_rendered=0" +
            "\tbase_search_elapsed=0\tmeta_search_elapsed=0\turl_hash=4cbe1b71879805a91a133541e09332cb\tenv" +
            "=production\tlocation=man\tsub_role=market\tcluster=4\thost=1\tsource_role=market" +
            "\tis_antirobot_degradation=0\trequest_gta_count=0\tdifferent_doc_attrs_sum_size=0" +
            "\tdifferent_snippet_attrs_sum_size=0\tdifferent_metadoc_attrs_sum_size=0";
        checker.check(
            line2
        );
    }
}
