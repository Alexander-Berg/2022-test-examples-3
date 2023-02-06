package ru.yandex.market.logshatter.parser.mbi;

import java.time.Instant;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;
import ru.yandex.market.logshatter.parser.trace.Environment;
import ru.yandex.market.logshatter.url.Page;
import ru.yandex.market.logshatter.url.PageMatcher;

/**
 * Тесты для {@link MbiPartnerStatLogParser}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
public class MbiPartnerStatLogParserTest {

    private static final LogParserChecker CHECKER = getChecker();

    private static LogParserChecker getChecker() {
        final MbiPartnerStatLogParser parser = new MbiPartnerStatLogParser();
        final PageMatcher pageMatcher = (host, method, url) -> {
            if (!"/fmcg/123/report/shows".equals(url)) {
                return null;
            }

            return new Page("fmcg_campaignId_report_shows", "/fmcg/<campaignId>/report/shows", "mbi-partner-stat", Instant.MIN);
        };
        return new LogParserChecker(parser, pageMatcher);
    }

    @Test
    public void parse() throws Exception {

        final String line1 = "tskv    \ttskv_format=tskv-market-default \ttimestamp=2019-07-30T07:57:48   \ttimezone=+0300  \tstatus=200      \tprotocol=HTTP/1.1       \tmethod=GET      \trequest=/testResponse/false     \treferer=-       \tcookies=-       \tuser_agent=curl/7.54.0  \tvhost=mbi-partner-stat.tst.vs.market.yandex.net \tip=2a02:6b8:c04:1a7:0:633:2d19:3434     \tx_forwarded_for=2a02:6b8:b080:8801::1:1 \tx_real_ip=2a02:6b8:b080:8801::1:1       \tbytes_sent=456  \tpage_id=-       \tpage_type=-     \treq_id=1564462668981/1eb49539c76257ed96d3444b0a80921d   \treq_id_seq=-    \tupstream_resp_time=0.001        \treq_time=0.001  \tscheme=http     \tdevice_type=-   \tx_sub_req_id=-  \tyandexuid=123      \tssl_handshake_time=-    \tmarket_buckets=-        \tupstream_addr=[::1]:12484       \tupstream_header_time=0.001      \tupstream_status=200     \tmarket_req_id=1564462668981/1eb49539c76257ed96d3444b0a80921d    \tmsec=1564462668.994     \ttvm=DISABLED    \ticookie=-       \trequest_tags=-";
        final String line2 = "tskv    \ttskv_format=tskv-market-default \ttimestamp=2019-07-30T07:57:48   \ttimezone=+0300  \tstatus=500      \tprotocol=HTTP/1.1       \tmethod=POST      \trequest=/fmcg/123/report/shows     \treferer=-       \tcookies=-       \tuser_agent=curl/7.54.0  \tvhost=mbi-partner-stat.tst.vs.market.yandex.net \tip=2a02:6b8:c04:1a7:0:633:2d19:3434     \tx_forwarded_for=2a02:6b8:b080:8801::1:1 \tx_real_ip=2a02:6b8:b080:8801::1:1       \tbytes_sent=456  \tpage_id=-       \tpage_type=-     \treq_id=1564462668981/1eb49539c76257ed96d3444b0a80921d   \treq_id_seq=-    \tupstream_resp_time=0.001        \treq_time=0.001  \tscheme=http     \tdevice_type=-   \tx_sub_req_id=-  \tyandexuid=      \tssl_handshake_time=-    \tmarket_buckets=-        \tupstream_addr=[::1]:12484       \tupstream_header_time=0.001      \tupstream_status=200     \tmarket_req_id=1564462668981/1eb49539c76257ed96d3444b0a80921d    \tmsec=1564462668.994     \ttvm=DISABLED    \ticookie=-       \trequest_tags=-";

        CHECKER.check(
            line1,
            1564462668,
            CHECKER.getHost(),
            "mbi-partner-stat.tst.vs.market.yandex.net",
            "/testResponse/false",
            "GET",
            "",
            200,
            1,
            "2a02:6b8:c04:1a7:0:633:2d19:3434",
            new String[]{},
            "123",
            "",
            1,
            456,
            "1564462668981/1eb49539c76257ed96d3444b0a80921d",
            "DISABLED",
            -1L,
            -1L,
            -1L,
            Environment.UNKNOWN
        );

        CHECKER.check(
            line2,
            1564462668,
            CHECKER.getHost(),
            "mbi-partner-stat.tst.vs.market.yandex.net",
            "/fmcg/123/report/shows",
            "POST",
            "fmcg_campaignId_report_shows",
            500,
            1,
            "2a02:6b8:c04:1a7:0:633:2d19:3434",
            new String[]{},
            "",
            "",
            1,
            456,
            "1564462668981/1eb49539c76257ed96d3444b0a80921d",
            "DISABLED",
            -1L,
            -1L,
            123L,
            Environment.UNKNOWN
        );
    }
}
