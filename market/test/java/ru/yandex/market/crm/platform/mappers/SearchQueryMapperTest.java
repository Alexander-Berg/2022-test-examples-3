package ru.yandex.market.crm.platform.mappers;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.crm.platform.commons.RGBType;
import ru.yandex.market.crm.platform.models.SearchQuery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author apershukov
 */
public class SearchQueryMapperTest {

    private SearchQueryMapper mapper;

    @Before
    public void setUp() {
        mapper = new SearchQueryMapper();
    }

    @Test
    public void testIgnoreFromWithoutUserId() {
        String line = "tskv\ttskv_format=market-report-access-log\tunixtime=1542058702\tevent_time=[Tue Nov 13 " +
                "00:38:22 2018]\turl=http://iva1-5111-iva-market-prep-report--b06-17050.gencfg-c.yandex" +
                ".net:17051/yandsearch?place=prime\tcookies=\tsearch_elapsed=0\tuser_agent" +
                "=\tuser_agent_hash=517762881\tremote_addr=2a02:6b8:c0e:2c:0:577:4a3b:9de7\tfull_elapsed=0" +
                "\ttotal_documents_processed=0\ttotal_documents_accepted=0\ttotal_rendered=0\treq_wiz_time=-1" +
                "\twizards=\treqid=\tredirect_info=\terror_info=\tbase_search_elapsed=0\tmeta_search_elapsed=0" +
                "\thave_trimmed_field=0\tfetch_time=0\tsnippet_requests_made=0\tsnippets_fetched=0\turl_hash" +
                "=92a60e50d75871e90d3fbd9ec3288c7e\ttest_ids=\tquery_corrected_by_speller=\tfuzzy_search_used=0" +
                "\tx_market_req_id=\tunixtime_ms=1542058702750\treq_wiz_count=0\tproduct_type=NONE" +
                "\texternal_requests_time=0\ttotal_renderable=\twizard_elements=\ticookie=\trgb=GREEN\tpp" +
                "=\trequest_body=NONE\tpartial_answer=0\testimated_max_memory_usage=482608\tenv=prestable\tlocation" +
                "=iva\tsub_role=market\tcluster=0\thost=0";

        List<SearchQuery> results = mapper.apply(line.getBytes());
        assertNotNull(results);
        assertEquals(0, results.size());
    }

    @Test
    public void testParseGreenSimple() {
        String line = "tskv\ttskv_format=market-report-access-log\tunixtime=1542058703\tevent_time=[Tue Nov 13 " +
                "00:38:23 2018]\turl=http://iva1-5111-iva-market-prep-report--b06-17050.gencfg-c.yandex" +
                ".net:17051/yandsearch?local-offers-first=0&onstock=1&touch=1&base=app.market.yandex.ru&ip=212.175.0" +
                ".193%2C212.175.0.193%2C212.175.0.193&ip-rids=213&currency=RUR&yandexuid=6405666071542042108&puid" +
                "=&family=&pof=&cpa-pof=&reqid=456f2bdf7533298349f273e53fa41896&x-yandex-icookie=6405666071542042108" +
                "&rearr-factors=&is-global-shop=0&show-min-quantity=1&regional-delivery=1&show-preorder=1&skip=0" +
                "&numdoc=10&page=1&mcpricefrom=1&how=&grhow=shop&rids=213&show-urls=external%2Cgeo%2Ccpa" +
                "%2CcallPhone&require-geo-coords=0&cpa=any&regset=2&new-delivery-mode=1&place=prime&hyperid" +
                "=7011289&pp=46&text=%D0%BA%D1%80%D0%BE%D1%81%D1%81%D0%BE%D0%B2%D0%BA%D0%B8&showVendors=top&cvredirect=1" +
                "&show-shops=top&bsformat=2&subreqid=1&showdiscounts=1" +
                "&show_explicit_content=medicine\tcookies=parent_reqid_seq=af8a69f54abb43f79ef9a2103175fae1; " +
                "reviews-merge=true; visits=1542042109-1542042109-1542042109; " +
                "i=X4MhVOXOdFTdSBZ+SBO3vxp3cNFCNiCqR9R1Q96ctZzY/0jGjdkkLcKY8v5ymHvytYbULL8T+Z36hKZXG4nWKXu+3M0=; " +
                "yandexuid=6405666071542042108; uid=AABSilvpsf2zlgByBisQAg==; " +
                "currentRegionId=54\tsearch_elapsed=29\tuser_agent=Mozilla/5.0 (Linux; Android 4.4.3; ZTE T620 " +
                "Build/LMY47D) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2878.31 Mobile Safari/537" +
                ".36\tuser_agent_hash=653160418\tremote_addr=2a02:6b8:c0c:190d:10b:604f:0:528a\tfull_elapsed=29" +
                "\ttotal_documents_processed=164\ttotal_documents_accepted=12\ttotal_rendered=0\treq_wiz_time=-1" +
                "\twizards=\treqid=456f2bdf7533298349f273e53fa41896\tredirect_info=\terror_info=\tbase_search_elapsed" +
                "=11\tmeta_search_elapsed=17\thave_trimmed_field=0\tfetch_time=1\tsnippet_requests_made=3" +
                "\tsnippets_fetched=8\turl_hash=1fcd70c80d12e0898028535b26fb3dc9\ttest_ids" +
                "=\tquery_corrected_by_speller=\tfuzzy_search_used=0\tx_market_req_id=1542058703039" +
                "/456f2bdf7533298349f273e53fa41896/1\tunixtime_ms=1542058703048\treq_wiz_count=0\tproduct_type=MODEL" +
                "\texternal_requests_time=0\ttotal_renderable=6\twizard_elements=\ticookie=6405666071542042108\trgb" +
                "=GREEN\tpp=46\trequest_body=NONE\tpartial_answer=0\testimated_max_memory_usage=44534000\tenv" +
                "=prestable\tlocation=iva\tsub_role=market\tcluster=0\thost=0";

        List<SearchQuery> results = mapper.apply(line.getBytes());
        assertNotNull(results);
        assertEquals(1, results.size());

        SearchQuery query = results.get(0);
        assertEquals("6405666071542042108", query.getUid(0).getStringValue());
        assertEquals(1542058703048L, query.getTimestamp());
        assertEquals(RGBType.GREEN, query.getRgb());
        assertEquals("кроссовки", query.getText());
    }

    @Test
    public void testParseWithPuid() {
        String line = "tskv\ttskv_format=market-report-access-log\tunixtime=1542058703\tevent_time=[Tue Nov 13 " +
                "00:38:23 2018]\turl=http://iva1-5111-iva-market-prep-report--b06-17050.gencfg-c.yandex" +
                ".net:17051/yandsearch?local-offers-first=0&onstock=1&touch=1&base=app.market.yandex.ru&ip=212.175.0" +
                ".193%2C212.175.0.193%2C212.175.0.193&ip-rids=213&currency=RUR&yandexuid=6405666071542042108&puid" +
                "=129817202&family=&pof=&cpa-pof=&reqid=456f2bdf7533298349f273e53fa41896" +
                "&x-yandex-icookie=6405666071542042108" +
                "&rearr-factors=&is-global-shop=0&show-min-quantity=1&regional-delivery=1&show-preorder=1&skip=0" +
                "&numdoc=10&page=1&mcpricefrom=1&how=&grhow=shop&rids=213&show-urls=external%2Cgeo%2Ccpa" +
                "%2CcallPhone&require-geo-coords=0&cpa=any&regset=2&new-delivery-mode=1&place=prime&hyperid" +
                "=7011289&pp=46&text=%D0%BA%D1%80%D0%BE%D1%81%D1%81%D0%BE%D0%B2%D0%BA%D0%B8&showVendors=top&cvredirect=1" +
                "&show-shops=top&bsformat=2&subreqid=1&showdiscounts=1" +
                "&show_explicit_content=medicine\tcookies=parent_reqid_seq=af8a69f54abb43f79ef9a2103175fae1; " +
                "reviews-merge=true; visits=1542042109-1542042109-1542042109; " +
                "i=X4MhVOXOdFTdSBZ+SBO3vxp3cNFCNiCqR9R1Q96ctZzY/0jGjdkkLcKY8v5ymHvytYbULL8T+Z36hKZXG4nWKXu+3M0=; " +
                "yandexuid=6405666071542042108; uid=AABSilvpsf2zlgByBisQAg==; " +
                "currentRegionId=54\tsearch_elapsed=29\tuser_agent=Mozilla/5.0 (Linux; Android 4.4.3; ZTE T620 " +
                "Build/LMY47D) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2878.31 Mobile Safari/537" +
                ".36\tuser_agent_hash=653160418\tremote_addr=2a02:6b8:c0c:190d:10b:604f:0:528a\tfull_elapsed=29" +
                "\ttotal_documents_processed=164\ttotal_documents_accepted=12\ttotal_rendered=0\treq_wiz_time=-1" +
                "\twizards=\treqid=456f2bdf7533298349f273e53fa41896\tredirect_info=\terror_info=\tbase_search_elapsed" +
                "=11\tmeta_search_elapsed=17\thave_trimmed_field=0\tfetch_time=1\tsnippet_requests_made=3" +
                "\tsnippets_fetched=8\turl_hash=1fcd70c80d12e0898028535b26fb3dc9\ttest_ids" +
                "=\tquery_corrected_by_speller=\tfuzzy_search_used=0\tx_market_req_id=1542058703039" +
                "/456f2bdf7533298349f273e53fa41896/1\tunixtime_ms=1542058703048\treq_wiz_count=0\tproduct_type=MODEL" +
                "\texternal_requests_time=0\ttotal_renderable=6\twizard_elements=\ticookie=6405666071542042108\trgb" +
                "=GREEN\tpp=46\trequest_body=NONE\tpartial_answer=0\testimated_max_memory_usage=44534000\tenv" +
                "=prestable\tlocation=iva\tsub_role=market\tcluster=0\thost=0";

        List<SearchQuery> results = mapper.apply(line.getBytes());
        assertNotNull(results);
        assertEquals(1, results.size());

        SearchQuery query = results.get(0);
        assertEquals(129817202, query.getUid(0).getIntValue());
        assertEquals("6405666071542042108", query.getUid(1).getStringValue());
        assertEquals(RGBType.GREEN, query.getRgb());
        assertEquals("кроссовки", query.getText());
    }

    @Test
    public void parseWithUuid() {
        String line = "tskv\ttskv_format=market-report-access-log\tunixtime=1542058703\tevent_time=[Tue Nov 13 " +
                "00:38:23 2018]\turl=http://iva1-5111-iva-market-prep-report--b06-17050.gencfg-c.yandex" +
                ".net:17051/yandsearch?local-offers-first=0&onstock=1&touch=1&base=app.market.yandex.ru&ip=212.175.0" +
                ".193%2C212.175.0.193%2C212.175.0.193&ip-rids=213&currency=RUR&uuid=4645d3780f8ffccc5e566d2e0e3f02ed" +
                "&family=&pof=&cpa-pof=&reqid=456f2bdf7533298349f273e53fa41896&x-yandex-icookie=6405666071542042108" +
                "&rearr-factors=&is-global-shop=0&show-min-quantity=1&regional-delivery=1&show-preorder=1&skip=0" +
                "&numdoc=10&page=1&mcpricefrom=1&how=&grhow=shop&rids=213&show-urls=external%2Cgeo%2Ccpa" +
                "%2CcallPhone&require-geo-coords=0&cpa=any&regset=2&new-delivery-mode=1&place=prime&hyperid" +
                "=7011289&pp=46&text=%D0%BA%D1%80%D0%BE%D1%81%D1%81%D0%BE%D0%B2%D0%BA%D0%B8&showVendors=top&cvredirect=1" +
                "&show-shops=top&bsformat=2&subreqid=1&showdiscounts=1" +
                "&show_explicit_content=medicine\tcookies=parent_reqid_seq=af8a69f54abb43f79ef9a2103175fae1; " +
                "reviews-merge=true; visits=1542042109-1542042109-1542042109; " +
                "i=X4MhVOXOdFTdSBZ+SBO3vxp3cNFCNiCqR9R1Q96ctZzY/0jGjdkkLcKY8v5ymHvytYbULL8T+Z36hKZXG4nWKXu+3M0=; " +
                "yandexuid=6405666071542042108; uid=AABSilvpsf2zlgByBisQAg==; " +
                "currentRegionId=54\tsearch_elapsed=29\tuser_agent=Mozilla/5.0 (Linux; Android 4.4.3; ZTE T620 " +
                "Build/LMY47D) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2878.31 Mobile Safari/537" +
                ".36\tuser_agent_hash=653160418\tremote_addr=2a02:6b8:c0c:190d:10b:604f:0:528a\tfull_elapsed=29" +
                "\ttotal_documents_processed=164\ttotal_documents_accepted=12\ttotal_rendered=0\treq_wiz_time=-1" +
                "\twizards=\treqid=456f2bdf7533298349f273e53fa41896\tredirect_info=\terror_info=\tbase_search_elapsed" +
                "=11\tmeta_search_elapsed=17\thave_trimmed_field=0\tfetch_time=1\tsnippet_requests_made=3" +
                "\tsnippets_fetched=8\turl_hash=1fcd70c80d12e0898028535b26fb3dc9\ttest_ids" +
                "=\tquery_corrected_by_speller=\tfuzzy_search_used=0\tx_market_req_id=1542058703039" +
                "/456f2bdf7533298349f273e53fa41896/1\tunixtime_ms=1542058703048\treq_wiz_count=0\tproduct_type=MODEL" +
                "\texternal_requests_time=0\ttotal_renderable=6\twizard_elements=\ticookie=6405666071542042108\trgb" +
                "=RED\tpp=46\trequest_body=NONE\tpartial_answer=0\testimated_max_memory_usage=44534000\tenv" +
                "=prestable\tlocation=iva\tsub_role=market\tcluster=0\thost=0";

        List<SearchQuery> results = mapper.apply(line.getBytes());
        assertNotNull(results);
        assertEquals(1, results.size());

        SearchQuery query = results.get(0);
        assertEquals("4645d3780f8ffccc5e566d2e0e3f02ed", query.getUid(0).getStringValue());
        assertEquals(RGBType.RED, query.getRgb());
        assertEquals("кроссовки", query.getText());
    }

    @Test
    public void testIgnoreWithoutText() {
        String line = "tskv\ttskv_format=market-report-access-log\tunixtime=1542058703\tevent_time=[Tue Nov 13 " +
                "00:38:23 2018]\turl=http://iva1-5111-iva-market-prep-report--b06-17050.gencfg-c.yandex" +
                ".net:17051/yandsearch?local-offers-first=0&onstock=1&touch=1&base=app.market.yandex.ru&ip=212.175.0" +
                ".193%2C212.175.0.193%2C212.175.0.193&ip-rids=213&currency=RUR&yandexuid=6405666071542042108&puid" +
                "=&family=&pof=&cpa-pof=&reqid=456f2bdf7533298349f273e53fa41896&x-yandex-icookie=6405666071542042108" +
                "&rearr-factors=&is-global-shop=0&show-min-quantity=1&regional-delivery=1&show-preorder=1&skip=0" +
                "&numdoc=10&page=1&mcpricefrom=1&how=&grhow=shop&rids=213&show-urls=external%2Cgeo%2Ccpa" +
                "%2CcallPhone&require-geo-coords=0&cpa=any&regset=2&new-delivery-mode=1&place=prime&hyperid" +
                "=7011289&pp=46&showVendors=top&cvredirect=1" +
                "&show-shops=top&bsformat=2&subreqid=1&showdiscounts=1" +
                "&show_explicit_content=medicine\tcookies=parent_reqid_seq=af8a69f54abb43f79ef9a2103175fae1; " +
                "reviews-merge=true; visits=1542042109-1542042109-1542042109; " +
                "i=X4MhVOXOdFTdSBZ+SBO3vxp3cNFCNiCqR9R1Q96ctZzY/0jGjdkkLcKY8v5ymHvytYbULL8T+Z36hKZXG4nWKXu+3M0=; " +
                "yandexuid=6405666071542042108; uid=AABSilvpsf2zlgByBisQAg==; " +
                "currentRegionId=54\tsearch_elapsed=29\tuser_agent=Mozilla/5.0 (Linux; Android 4.4.3; ZTE T620 " +
                "Build/LMY47D) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2878.31 Mobile Safari/537" +
                ".36\tuser_agent_hash=653160418\tremote_addr=2a02:6b8:c0c:190d:10b:604f:0:528a\tfull_elapsed=29" +
                "\ttotal_documents_processed=164\ttotal_documents_accepted=12\ttotal_rendered=0\treq_wiz_time=-1" +
                "\twizards=\treqid=456f2bdf7533298349f273e53fa41896\tredirect_info=\terror_info=\tbase_search_elapsed" +
                "=11\tmeta_search_elapsed=17\thave_trimmed_field=0\tfetch_time=1\tsnippet_requests_made=3" +
                "\tsnippets_fetched=8\turl_hash=1fcd70c80d12e0898028535b26fb3dc9\ttest_ids" +
                "=\tquery_corrected_by_speller=\tfuzzy_search_used=0\tx_market_req_id=1542058703039" +
                "/456f2bdf7533298349f273e53fa41896/1\tunixtime_ms=1542058703048\treq_wiz_count=0\tproduct_type=MODEL" +
                "\texternal_requests_time=0\ttotal_renderable=6\twizard_elements=\ticookie=6405666071542042108\trgb" +
                "=GREEN\tpp=46\trequest_body=NONE\tpartial_answer=0\testimated_max_memory_usage=44534000\tenv" +
                "=prestable\tlocation=iva\tsub_role=market\tcluster=0\thost=0";

        List<SearchQuery> results = mapper.apply(line.getBytes());
        assertNotNull(results);
        assertEquals(0, results.size());
    }

    @Test
    public void testSkipAnotherPlace() {
        String line = "tskv\ttskv_format=market-report-access-log\tunixtime=1542058703\tevent_time=[Tue Nov 13 " +
                "00:38:23 2018]\turl=http://iva1-5111-iva-market-prep-report--b06-17050.gencfg-c.yandex" +
                ".net:17051/yandsearch?local-offers-first=0&onstock=1&touch=1&base=app.market.yandex.ru&ip=212.175.0" +
                ".193%2C212.175.0.193%2C212.175.0.193&ip-rids=213&currency=RUR&yandexuid=6405666071542042108&puid" +
                "=&family=&pof=&cpa-pof=&reqid=456f2bdf7533298349f273e53fa41896&x-yandex-icookie=6405666071542042108" +
                "&rearr-factors=&is-global-shop=0&show-min-quantity=1&regional-delivery=1&show-preorder=1&skip=0" +
                "&numdoc=10&page=1&mcpricefrom=1&how=&grhow=shop&rids=213&show-urls=external%2Cgeo%2Ccpa" +
                "%2CcallPhone&require-geo-coords=0&cpa=any&regset=2&new-delivery-mode=1&place=productoffers&hyperid" +
                "=7011289&pp=46&text=%D0%BA%D1%80%D0%BE%D1%81%D1%81%D0%BE%D0%B2%D0%BA%D0%B8&showVendors=top&cvredirect=1" +
                "&show-shops=top&bsformat=2&subreqid=1&showdiscounts=1" +
                "&show_explicit_content=medicine\tcookies=parent_reqid_seq=af8a69f54abb43f79ef9a2103175fae1; " +
                "reviews-merge=true; visits=1542042109-1542042109-1542042109; " +
                "i=X4MhVOXOdFTdSBZ+SBO3vxp3cNFCNiCqR9R1Q96ctZzY/0jGjdkkLcKY8v5ymHvytYbULL8T+Z36hKZXG4nWKXu+3M0=; " +
                "yandexuid=6405666071542042108; uid=AABSilvpsf2zlgByBisQAg==; " +
                "currentRegionId=54\tsearch_elapsed=29\tuser_agent=Mozilla/5.0 (Linux; Android 4.4.3; ZTE T620 " +
                "Build/LMY47D) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2878.31 Mobile Safari/537" +
                ".36\tuser_agent_hash=653160418\tremote_addr=2a02:6b8:c0c:190d:10b:604f:0:528a\tfull_elapsed=29" +
                "\ttotal_documents_processed=164\ttotal_documents_accepted=12\ttotal_rendered=0\treq_wiz_time=-1" +
                "\twizards=\treqid=456f2bdf7533298349f273e53fa41896\tredirect_info=\terror_info=\tbase_search_elapsed" +
                "=11\tmeta_search_elapsed=17\thave_trimmed_field=0\tfetch_time=1\tsnippet_requests_made=3" +
                "\tsnippets_fetched=8\turl_hash=1fcd70c80d12e0898028535b26fb3dc9\ttest_ids" +
                "=\tquery_corrected_by_speller=\tfuzzy_search_used=0\tx_market_req_id=1542058703039" +
                "/456f2bdf7533298349f273e53fa41896/1\tunixtime_ms=1542058703048\treq_wiz_count=0\tproduct_type=MODEL" +
                "\texternal_requests_time=0\ttotal_renderable=6\twizard_elements=\ticookie=6405666071542042108\trgb" +
                "=GREEN\tpp=46\trequest_body=NONE\tpartial_answer=0\testimated_max_memory_usage=44534000\tenv" +
                "=prestable\tlocation=iva\tsub_role=market\tcluster=0\thost=0";

        List<SearchQuery> results = mapper.apply(line.getBytes());
        assertNotNull(results);
        assertEquals(0, results.size());
    }

    @Test
    public void testSkipWithSortingParameterOnDesktop() {
        String line = "tskv\ttskv_format=market-report-access-log\tunixtime=1542058703\tevent_time=[Tue Nov 13 " +
                "00:38:23 2018]\turl=http://iva1-5111-iva-market-prep-report--b06-17050.gencfg-c.yandex" +
                ".net:17051/yandsearch?local-offers-first=0&onstock=1&touch=1&base=app.market.yandex.ru&ip=212.175.0" +
                ".193%2C212.175.0.193%2C212.175.0.193&ip-rids=213&currency=RUR&yandexuid=6405666071542042108&puid" +
                "=&family=&pof=&cpa-pof=&reqid=456f2bdf7533298349f273e53fa41896&x-yandex-icookie=6405666071542042108" +
                "&rearr-factors=&is-global-shop=0&show-min-quantity=1&regional-delivery=1&show-preorder=1&skip=0" +
                "&numdoc=10&page=1&mcpricefrom=1&how=aprice&grhow=shop&rids=213&show-urls=external%2Cgeo%2Ccpa" +
                "%2CcallPhone&require-geo-coords=0&cpa=any&regset=2&new-delivery-mode=1&place=prime&hyperid" +
                "=7011289&pp=46&text=%D0%BA%D1%80%D0%BE%D1%81%D1%81%D0%BE%D0%B2%D0%BA%D0%B8&showVendors=top&cvredirect=1" +
                "&show-shops=top&bsformat=2&subreqid=1&showdiscounts=1" +
                "&show_explicit_content=medicine\tcookies=parent_reqid_seq=af8a69f54abb43f79ef9a2103175fae1; " +
                "reviews-merge=true; visits=1542042109-1542042109-1542042109; " +
                "i=X4MhVOXOdFTdSBZ+SBO3vxp3cNFCNiCqR9R1Q96ctZzY/0jGjdkkLcKY8v5ymHvytYbULL8T+Z36hKZXG4nWKXu+3M0=; " +
                "yandexuid=6405666071542042108; uid=AABSilvpsf2zlgByBisQAg==; " +
                "currentRegionId=54\tsearch_elapsed=29\tuser_agent=Mozilla/5.0 (Linux; Android 4.4.3; ZTE T620 " +
                "Build/LMY47D) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2878.31 Mobile Safari/537" +
                ".36\tuser_agent_hash=653160418\tremote_addr=2a02:6b8:c0c:190d:10b:604f:0:528a\tfull_elapsed=29" +
                "\ttotal_documents_processed=164\ttotal_documents_accepted=12\ttotal_rendered=0\treq_wiz_time=-1" +
                "\twizards=\treqid=456f2bdf7533298349f273e53fa41896\tredirect_info=\terror_info=\tbase_search_elapsed" +
                "=11\tmeta_search_elapsed=17\thave_trimmed_field=0\tfetch_time=1\tsnippet_requests_made=3" +
                "\tsnippets_fetched=8\turl_hash=1fcd70c80d12e0898028535b26fb3dc9\ttest_ids" +
                "=\tquery_corrected_by_speller=\tfuzzy_search_used=0\tx_market_req_id=1542058703039" +
                "/456f2bdf7533298349f273e53fa41896/1\tunixtime_ms=1542058703048\treq_wiz_count=0\tproduct_type=MODEL" +
                "\texternal_requests_time=0\ttotal_renderable=6\twizard_elements=\ticookie=6405666071542042108\trgb" +
                "=GREEN\tpp=46\trequest_body=NONE\tpartial_answer=0\testimated_max_memory_usage=44534000\tenv" +
                "=prestable\tlocation=iva\tsub_role=market\tcluster=0\thost=0";

        List<SearchQuery> results = mapper.apply(line.getBytes());
        assertNotNull(results);
        assertEquals(0, results.size());
    }
}
