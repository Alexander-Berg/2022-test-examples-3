package ru.yandex.market.logshatter.parser.nesu;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;
import ru.yandex.market.logshatter.parser.trace.Environment;

public class NesuNginxTskvLogParserTest {
    LogParserChecker checker = new LogParserChecker(new NesuNginxTskvLogParser());

    @Test
    public void parse() throws Exception {

        String line1 = "tskv    \ttskv_format=tskv-market-default \ttimestamp=2020-11-20T00:19:04   \ttimezone=+0300 " +
            " \tstatus=200      \tprotocol=HTTP/1.1       \tmethod=GET      \trequest=/ping   \treferer=-       " +
            "\tcookies=-       \tuser_agent=-    \tvhost=nesu.vs.market.yandex.net " +
            "\tip=2a02:6b8:c0e:2c:0:577:49c5:88a9      \tx_forwarded_for=-       \tx_real_ip=-     \tbytes_sent=218  " +
            "\tpage_id=-       \tpage_type=-     \treq_id=1605820744552/303646ca5a4d9237bb1acbdedfd818d3   " +
            "\treq_id_seq=-    \tupstream_resp_time=0.000        \treq_time=0.001  \tscheme=http     \tdevice_type=- " +
            "  \tx_sub_req_id=-  \tyandexuid=      \tssl_handshake_time=-    \tmarket_buckets=-        " +
            "\tupstream_addr=[::1]:12904       \tupstream_header_time=0.000      \tupstream_status=200     " +
            "\tmarket_req_id=1605820744552/303646ca5a4d9237bb1acbdedfd818d3    \tmsec=1605820744.553     " +
            "\ttvm=DISABLED    \ticookie=-       \trequest_tags=-  \tx_return_code=- \tx_passport_uid=-        " +
            "\tsuspiciousness=-        \tcrawler=-        \tdegradation=-        \tnanny_service_id=-";
        String line2 = "tskv    \ttskv_format=tskv-market-default \ttimestamp=2020-11-20T00:19:04   \ttimezone=+0300 " +
            " \tstatus=200      \tprotocol=HTTP/1.0       \tmethod=GET      \trequest=/api/ping       \treferer=-    " +
            "   \tcookies=-       \tuser_agent=-    \tvhost=nesu.vs.market.yandex.net \tip=::1  " +
            "\tx_forwarded_for=2a02:6b8:c04:175:0:577:3bb5:38c3        \tx_real_ip=2a02:6b8:c04:175:0:577:3bb5:38c3  " +
            "    \tbytes_sent=218  \tpage_id=-       \tpage_type=-     " +
            "\treq_id=1605820744770/3c97509b10fe0e178ee1a68b86c2b279   \treq_id_seq=-    \tupstream_resp_time=0.000  " +
            "      \treq_time=0.001  \tscheme=http     \tdevice_type=-   \tx_sub_req_id=-  \tyandexuid=      " +
            "\tssl_handshake_time=-    \tmarket_buckets=-        \tupstream_addr=[::1]:12904       " +
            "\tupstream_header_time=0.000      \tupstream_status=200     " +
            "\tmarket_req_id=1605820744770/3c97509b10fe0e178ee1a68b86c2b279    \tmsec=1605820744.772     " +
            "\ttvm=DISABLED    \ticookie=-       \trequest_tags=-  \tx_return_code=- \tx_passport_uid=-        " +
            "\tsuspiciousness=-        \tcrawler=-        \tdegradation=-        \tnanny_service_id=-";

        checker.check(
            line1,
            1605820744,
            checker.getHost(),
            "nesu.vs.market.yandex.net",
            "/ping",
            "GET",
            200,
            0,
            true,
            "",
            "",
            "",
            "2a02:6b8:c0e:2c:0:577:49c5:88a9",
            "",
            new String[]{},
            "",
            "",
            "",
            1,
            0,
            218,
            "1605820744552/303646ca5a4d9237bb1acbdedfd818d3",
            "",
            "DISABLED",
            "",
            -2,
            "",
            "",
            -2L,
            new String[]{},
            new String[]{},
            Environment.UNKNOWN,
            new String[]{},
            "",
            "",
            "",
            "",
            "",
            -1,
            -1
        );

        checker.check(
            line2,
            1605820744,
            checker.getHost(),
            "nesu.vs.market.yandex.net",
            "/api/ping",
            "GET",
            200,
            0,
            true,
            "",
            "",
            "",
            "2a02:6b8:c04:175:0:577:3bb5:38c3",
            "",
            new String[]{},
            "",
            "",
            "",
            1,
            0,
            218,
            "1605820744770/3c97509b10fe0e178ee1a68b86c2b279",
            "",
            "DISABLED",
            "",
            -2,
            "",
            "",
            -2L,
            new String[]{},
            new String[]{},
            Environment.UNKNOWN,
            new String[]{},
            "",
            "",
            "",
            "",
            "",
            -1,
            -1
        );
    }
}
