package ru.yandex.market.logshatter.parser.trace;

import org.junit.Test;
import ru.yandex.market.logshatter.parser.LogParserChecker;

import java.util.Date;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 23/11/2018
 */
public class ReportTraceLogParserTest {


    @Test
    public void test() throws Exception {
        String line = "tskv\ttskv_format=market-report-access-log\tunixtime=1542980085" +
            "\tevent_time=[Fri Nov 23 16:34:45 2018]" +
            "\turl=http://sas1-9877-288-sas-market-prod--cc7-17050.gencfg-c.yandex.net:17051/yandsearch?use-multi-navigation-trees=1&base=beru.ru&ip=92.255.220.52%2C92.255.220.52%2C92.255.220.52&ip-rids=46&yandexuid=2885631431540881301&puid=139518031&currency=RUR&icookie=480hQKorM30QAIGc5D1cbKhvdt7UYFrWdybsep8i4HwS7p3IZbDK4%2F7pkE5%2B%2BoT5nKbIVRJUkfJGP1xyI72z0yWr6PI%3D&x-yandex-icookie=4378565641542979865&reqid=945daa4557e027a4c2966021f1a767e5&test-buckets=106091%2C0%2C45%3B103847%2C0%2C82%3B104973%2C0%2C95%3B106271%2C0%2C31%3B88582%2C0%2C11%3B98642%2C0%2C53&rearr-factors=yamarec-coviews-mode%3Dsku_siblings1%3Brty_qpipe_for_blue%3D1%3Bfullgen_prices%3D1&numdoc=200&page=1&onstock=&rids=46&show-urls=external&trim-thumbs=1&require-geo-coords=0&regset=2&new-delivery-mode=1&place=products_by_history&pp=18&pg=18&show_explicit_content=medicine&regional-delivery=1&referer=https%253A%252F%252Fberu.ru%252Fapi%252Fsearch%253Fhid%253D191211%2526glfilter%253D4898082%25253A12109164%2526glfilter%253D4898086%25253A12833982%25252C12833983%25252C12109224%25252C13519935%25252C15005483%25252C15625548%25252C15625547%2526glfilter%253D7893318%25253A761983%2526glfilter%253D15937366%25253A15937380%2526nid%253D80143%2526cvredirect%253D0%2526sk%253Du849a4c38a1f81a6a92c7c9fc5d58fb73&subreqid=1&row_width=1&client=frontend&show-min-quantity=1&show-filter-mark=specifiedForOffer&rgb=BLUE&market-sku=&show-preorder=1&bsformat=2\tcookies=uid=AABuBVv4ARkcEACbBIxIAg==; L=X2xHc10HDnxFUlNlB1duUWxbZlJYUwFcQFpEDEQZBxYvNA==.1541440788.13675.312294.b5319acdd8c4d091a9d4bd5c86d055c8; my=YwA=; yandex_gid=46; parent_reqid_seq=e19bbc738d71241572509dc8efb4a714%2C40dfa425ae207d7e63ac2994b84592a7%2Cb97a7b273fb00c81d9b759549e8cdafc%2C729df13ec890234f15fd8d3724c7fd36%2C1cfdd4f2bdd872d82299d9942986e349; fonts-loaded=1; available-delivery=46%3D1; sessionid2=3:1542979865.5.0.1541440788944:HfK1Xg:45.1|139518031.0.2|45:5427.758873.Jwf7HgLeAf_Apx-ir2Axq7_jNKY; Session_id=3:1542979865.5.0.1541440788944:HfK1Xg:45.1|139518031.0.2|45:5427.635575._nDfmWLwlhnq1uSucFG_0lOLs_E; yp=1543066268.yu.2885631431540881301; visits=1542979865-1542979865-1542979865; _ym_isad=2; _ym_d=1542979865; yandexuid=2885631431540881301; mda=1; HISTORY_AUTH_SESSION=%7B%22isAuth%22%3Atrue%7D; _ym_visorc_47628343=b; _ym_uid=15429798651003896804; i=480hQKorM30QAIGc5D1cbKhvdt7UYFrWdybsep8i4HwS7p3IZbDK4/7pkE5++oT5nKbIVRJUkfJGP1xyI72z0yWr6PI=; _ym_wasSynced=%7B%22time%22%3A1542979866945%2C%22params%22%3A%7B%22eu%22%3A0%7D%2C%22bkParams%22%3A%7B%7D%7D; welcome_coin=%7B%22intro%22%3Atrue%2C%22entrypoint%22%3Atrue%7D; yandex_login=r.tuskenis; Cookie_check=checked\tsearch_elapsed=13\tuser_agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/64.0.3282.140 Safari/537.36 Edge/17.17134\tuser_agent_hash=4231555168\tremote_addr=2a02:6b8:c08:7a91:10d:2431:0:6e05\tfull_elapsed=13\ttotal_documents_processed=0\ttotal_documents_accepted=0\ttotal_rendered=0\treq_wiz_time=-1\twizards=\treqid=945daa4557e027a4c2966021f1a767e5\tredirect_info=\terror_info=\tbase_search_elapsed=3\tmeta_search_elapsed=2\thave_trimmed_field=0\tfetch_time=0\tsnippet_requests_made=0\tsnippets_fetched=0\turl_hash=983d90247d8f81dfaa2d55b3352569a9" +
            "\ttest_ids=\tquery_corrected_by_speller=\tfuzzy_search_used=0" +
            "\tx_market_req_id=1542980085421/945daa4557e027a4c2966021f1a767e5/10\tunixtime_ms=1542980085935" +
            "\treq_wiz_count=0\tproduct_type=NONE\texternal_requests_time=8\ttotal_renderable=0\twizard_elements=" +
            "\ticookie=4378565641542979865\trgb=BLUE\tpp=18\trequest_body=\tpartial_answer=0" +
            "\testimated_max_memory_usage=4179456\tenv=production\tlocation=sas\tsub_role=blue-market" +
            "\tcluster=4\thost=0\n";

        LogParserChecker checker = new LogParserChecker(new ReportTraceLogParser());

        checker.check(
            line,
            new Date(1542980085421L),
            1542980085421L,
            "945daa4557e027a4c2966021f1a767e5",
            new Integer[]{10},
            1542980085935L,
            1542980085948L,
            13,
            RequestType.IN,
            ReportTraceLogParser.REPORT_MODULE_ID,
            "hostname.test",
            "",
            "2a02:6b8:c08:7a91:10d:2431:0:6e05",
            ReportTraceLogParser.REPORT_MODULE_ID,
            "hostname.test",
            Environment.UNKNOWN,
            "GET",
            -1,
            1,
            "",
            "http",
            "",
            "",
            "",
            "",
            ReportTraceLogParser.KV_KEYS,
            new String[]{"products_by_history", "18", "0", "0", "0", "13", "3", "2", "-1"},
            new Object[]{},
            new Object[]{},
            new Object[]{},
            "",
            "",
            -1
        );
    }
}