package ru.yandex.market.logshatter.parser.mbi;

import java.text.DateFormat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;


/**
 * @author dimkarp93
 */
public class ContentApiNginxTskvLogParserTest {
    private LogParserChecker checker;
    private DateFormat dateFormat;

    @BeforeEach
    public void setUp() {
        checker = new LogParserChecker(new ContentApiNginxTskvLogParser());
        dateFormat = ContentApiHelper.dateFormat();
    }

    @Test
    public void testParse() throws Exception {
        String line = "tskv\ttskv_format=tskv-market-default\ttimestamp=2019-10-14T16:16:05\ttimezone=+0300\tstatus" +
            "=200\tprotocol=HTTP/1.1\tmethod=GET\trequest=/v2" +
            ".1/models/539573069/offers?clid=2348348&pp=918&local_offers_first=0&-3=1&-7=0&report_hide-offers-without" +
            "-cpc-link=1&report_allow-collapsing=1&report_show-preorder=1&filter_warnings=medicine_recipe&geo_id=213" +
            "&count=6&fields=OFFER_CATEGORY%2COFFER_DELIVERY%2COFFER_DISCOUNT%2COFFER_SHOP%2COFFER_PHOTO" +
            "%2COFFER_VENDOR%2COFFER_SHOP%2COFFER_ACTIVE_FILTERS%2CSHOP_RATING&groupBy=SHOP&how=DESC&sections=ADULT" +
            "%2CMEDICINE&sort=POPULARITY\treferer=-\tcookies=\tuser_agent=-\tvhost=api.content.market.yandex" +
            ".ru\tip=2a02:6b8:c04:1d4:0:577:be2f:4087\tx_forwarded_for=85.26.241" +
            ".175\tx_real_ip=2a02:6b8:c0c:1e8d:10b:4010:0:3552\tbytes_sent=18731\tpage_id=-\tpage_type=-\treq_id" +
            "=1571058965387/1ac7deca48227f7f7c91a4a3862daa59/4\treq_id_seq=-\tupstream_resp_time=0.220\treq_time=0" +
            ".220\tscheme=http\tdevice_type=-\tx_sub_req_id=-\tyandexuid=\tssl_handshake_time=-\tmarket_buckets" +
            "=-\tupstream_addr=[::1]:8837\tupstream_header_time=0" +
            ".220\tupstream_status=200\tmarket_req_id=1571058965387/1ac7deca48227f7f7c91a4a3862daa59/4\tmsec" +
            "=1571058965.816\ttvm=DISABLED\ticookie=6286556771571058965\trequest_tags=-\tx_return_code=-";
        checker.check(
            line,
            dateFormat.parse("2019-10-14 16:16:05,000"),
            "hostname.test",
            "1571058965387/1ac7deca48227f7f7c91a4a3862daa59/4",
            "85.26.241.175",
            "2a02:6b8:c04:1d4:0:577:be2f:4087",
            200,
            "/v2.1/models/539573069/offers?clid=2348348&pp=918&local_offers_first=0&-3=1&-7=0" +
                "&report_hide-offers-without-cpc-link=1&report_allow-collapsing=1&report_show-preorder=1&" +
                "filter_warnings=medicine_recipe&geo_id=213&count=6" +
                "&fields=OFFER_CATEGORY%2COFFER_DELIVERY%2COFFER_DISCOUNT%2COFFER_SHOP%2COFFER_PHOTO" +
                "%2COFFER_VENDOR%2COFFER_SHOP%2COFFER_ACTIVE_FILTERS%2CSHOP_RATING" +
                "&groupBy=SHOP&how=DESC&sections=ADULT%2CMEDICINE&sort=POPULARITY",
            18731,
            220,
            220
        );
    }

    @Test
    public void testDashXForwarderFor() throws Exception {
        String line = "tskv\ttskv_format=tskv-market-default\ttimestamp=2019-10-14T16:16:05\ttimezone=+0300\tstatus" +
            "=200\tprotocol=HTTP/1.1\tmethod=GET\trequest=/v2" +
            ".1/models/539573069/offers?clid=2348348&pp=918&local_offers_first=0&-3=1&-7=0&report_hide-offers-without" +
            "-cpc-link=1&report_allow-collapsing=1&report_show-preorder=1&filter_warnings=medicine_recipe&geo_id=213" +
            "&count=6&fields=OFFER_CATEGORY%2COFFER_DELIVERY%2COFFER_DISCOUNT%2COFFER_SHOP%2COFFER_PHOTO" +
            "%2COFFER_VENDOR%2COFFER_SHOP%2COFFER_ACTIVE_FILTERS%2CSHOP_RATING&groupBy=SHOP&how=DESC&sections=ADULT" +
            "%2CMEDICINE&sort=POPULARITY\treferer=-\tcookies=\tuser_agent=-\tvhost=api.content.market.yandex" +
            ".ru\tip=2a02:6b8:c04:1d4:0:577:be2f:4087\tx_forwarded_for=-" +
            "\tx_real_ip=2a02:6b8:c0c:1e8d:10b:4010:0:3552\tbytes_sent=18731\tpage_id=-\tpage_type=-\treq_id" +
            "=1571058965387/1ac7deca48227f7f7c91a4a3862daa59/4\treq_id_seq=-\tupstream_resp_time=0.220\treq_time=0" +
            ".220\tscheme=http\tdevice_type=-\tx_sub_req_id=-\tyandexuid=\tssl_handshake_time=-\tmarket_buckets" +
            "=-\tupstream_addr=[::1]:8837\tupstream_header_time=0" +
            ".220\tupstream_status=200\tmarket_req_id=1571058965387/1ac7deca48227f7f7c91a4a3862daa59/4\tmsec" +
            "=1571058965.816\ttvm=DISABLED\ticookie=6286556771571058965\trequest_tags=-\tx_return_code=-";
        checker.check(
            line,
            dateFormat.parse("2019-10-14 16:16:05,000"),
            "hostname.test",
            "1571058965387/1ac7deca48227f7f7c91a4a3862daa59/4",
            "-",
            "2a02:6b8:c04:1d4:0:577:be2f:4087",
            200,
            "/v2.1/models/539573069/offers?clid=2348348&pp=918&local_offers_first=0&-3=1&-7=0" +
                "&report_hide-offers-without-cpc-link=1&report_allow-collapsing=1&report_show-preorder=1&" +
                "filter_warnings=medicine_recipe&geo_id=213&count=6" +
                "&fields=OFFER_CATEGORY%2COFFER_DELIVERY%2COFFER_DISCOUNT%2COFFER_SHOP%2COFFER_PHOTO" +
                "%2COFFER_VENDOR%2COFFER_SHOP%2COFFER_ACTIVE_FILTERS%2CSHOP_RATING" +
                "&groupBy=SHOP&how=DESC&sections=ADULT%2CMEDICINE&sort=POPULARITY",
            18731,
            220,
            220
        );
    }

    @Test
    public void testEmptyXForwarderFor() throws Exception {
        String line = "tskv\ttskv_format=tskv-market-default\ttimestamp=2019-10-14T16:16:05\ttimezone=+0300\tstatus" +
            "=200\tprotocol=HTTP/1.1\tmethod=GET\trequest=/v2" +
            ".1/models/539573069/offers?clid=2348348&pp=918&local_offers_first=0&-3=1&-7=0&report_hide-offers-without" +
            "-cpc-link=1&report_allow-collapsing=1&report_show-preorder=1&filter_warnings=medicine_recipe&geo_id=213" +
            "&count=6&fields=OFFER_CATEGORY%2COFFER_DELIVERY%2COFFER_DISCOUNT%2COFFER_SHOP%2COFFER_PHOTO" +
            "%2COFFER_VENDOR%2COFFER_SHOP%2COFFER_ACTIVE_FILTERS%2CSHOP_RATING&groupBy=SHOP&how=DESC&sections=ADULT" +
            "%2CMEDICINE&sort=POPULARITY\treferer=-\tcookies=\tuser_agent=-\tvhost=api.content.market.yandex" +
            ".ru\tip=2a02:6b8:c04:1d4:0:577:be2f:4087\tx_real_ip=2a02:6b8:c0c:1e8d:10b:4010:0:3552\tbytes_sent=18731" +
            "\tpage_id=-\tpage_type=-\treq_id" +
            "=1571058965387/1ac7deca48227f7f7c91a4a3862daa59/4\treq_id_seq=-\tupstream_resp_time=0.220\treq_time=0" +
            ".220\tscheme=http\tdevice_type=-\tx_sub_req_id=-\tyandexuid=\tssl_handshake_time=-\tmarket_buckets" +
            "=-\tupstream_addr=[::1]:8837\tupstream_header_time=0" +
            ".220\tupstream_status=200\tmarket_req_id=1571058965387/1ac7deca48227f7f7c91a4a3862daa59/4\tmsec" +
            "=1571058965.816\ttvm=DISABLED\ticookie=6286556771571058965\trequest_tags=-\tx_return_code=-";
        checker.check(
            line,
            dateFormat.parse("2019-10-14 16:16:05,000"),
            "hostname.test",
            "1571058965387/1ac7deca48227f7f7c91a4a3862daa59/4",
            "-",
            "2a02:6b8:c04:1d4:0:577:be2f:4087",
            200,
            "/v2.1/models/539573069/offers?clid=2348348&pp=918&local_offers_first=0&-3=1&-7=0" +
                "&report_hide-offers-without-cpc-link=1&report_allow-collapsing=1&report_show-preorder=1&" +
                "filter_warnings=medicine_recipe&geo_id=213&count=6" +
                "&fields=OFFER_CATEGORY%2COFFER_DELIVERY%2COFFER_DISCOUNT%2COFFER_SHOP%2COFFER_PHOTO" +
                "%2COFFER_VENDOR%2COFFER_SHOP%2COFFER_ACTIVE_FILTERS%2CSHOP_RATING" +
                "&groupBy=SHOP&how=DESC&sections=ADULT%2CMEDICINE&sort=POPULARITY",
            18731,
            220,
            220
        );
    }
}
