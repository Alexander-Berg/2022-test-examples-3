package ru.yandex.market.logshatter.parser.mbi;

import org.junit.Test;
import ru.yandex.market.logshatter.parser.LogParserChecker;

/**
 * @author stani on 13.03.18.
 */
public class MbiPartnerNginxTskvLogParserTest {

    LogParserChecker checker = new LogParserChecker(new MbiPartnerNginxTskvLogParser());

    @Test
    public void parse() throws Exception {

        String line1 = "tskv    \ttskv_format=tskv-market-default \ttimestamp=2018-03-13T00:42:36   \ttimezone=+0300  \tstatus=200      \tprotocol=HTTP/1.1       \tmethod=GET      \trequest=/getDatasource?_user_id=595264012&_remote_ip=109.252.221.243&id=21454711        \treferer=-       \tcookies=-       \tuser_agent=-    \tvhost=mbi-partner.vs.market.yandex.net  \tip=2a02:6b8:b010:5026:2181:daec:2a38:58fd       \tx_forwarded_for=2a02:6b8:0:888:225:90ff:fec8:c900       \tx_real_ip=109.252.221.243       \tbytes_sent=641  \tpage_id=-       \tpage_type=-     \treq_id=1520890956736/ce376224aac6e1565a0009d59d17c124/8 \treq_id_seq=-    \tupstream_resp_time=0.007        \treq_time=0.007  \tscheme=http     \tdevice_type=-   \tx_sub_req_id=-  \tyandexuid=-     \tssl_handshake_time=-    \tmarket_buckets=-        \tupstream_addr=[::1]:8211        \tupstream_header_time=0.007      \tupstream_status=200     \tmarket_req_id=1520890956736/ce376224aac6e1565a0009d59d17c124/8  \tmsec=1520890956.926     \ttvm=DISABLED";
        String line2 = "tskv    \ttskv_format=tskv-market-default \ttimestamp=2018-03-13T13:36:20   \ttimezone=+0300  \tstatus=500      \tprotocol=HTTP/1.1       \tmethod=POST     \trequest=/suppliers/21460129/application/edits?_user_id=607695260&_remote_ip=2a02%3A6b8%3A0%3A107%3A3067%3A6dc0%3A2c42%3Aaad9&euid=&format=json  \treferer=-       \tcookies=-       \tuser_agent=-    \tvhost=mbi-partner.vs.market.yandex.net  \tip=2a02:6b8:b010:5026:5220:4711:ae06:50cc       \tx_forwarded_for=2a02:6b8:0:888:225:90ff:fec8:c900       \tx_real_ip=2a02:6b8:0:888:225:90ff:fec8:c900     \tbytes_sent=5610 \tpage_id=-       \tpage_type=-     \treq_id=1520937380216/0950ebae9af605e65c2636c04f5e5fa6/4 \treq_id_seq=-    \tupstream_resp_time=0.281        \treq_time=0.281  \tscheme=http     \tdevice_type=-   \tx_sub_req_id=-  \tyandexuid=-     \tssl_handshake_time=-    \tmarket_buckets=-        \tupstream_addr=[::1]:8211        \tupstream_header_time=0.281      \tupstream_status=500     \tmarket_req_id=1520937380216/0950ebae9af605e65c2636c04f5e5fa6/4  \tmsec=1520937380.557     \ttvm=DISABLED";

        checker.check(
            line1,
            1520890956,
            checker.getHost(),
            "mbi-partner.vs.market.yandex.net",
            "/getDatasource?_user_id=595264012&_remote_ip=109.252.221.243&id=21454711",
            "GET",
            "",
            200,
            7,
            "2a02:6b8:b010:5026:2181:daec:2a38:58fd",
            new String[]{},
            7,
            641,
            "1520890956736/ce376224aac6e1565a0009d59d17c124/8",
            595264012L,
            -1L,
            21454711L
        );

        checker.check(
            line2,
            1520937380,
            checker.getHost(),
            "mbi-partner.vs.market.yandex.net",
            "/suppliers/21460129/application/edits?_user_id=607695260&_remote_ip=2a02%3A6b8%3A0%3A107%3A3067%3A6dc0%3A2c42%3Aaad9&euid=&format=json",
            "POST",
            "",
            500,
            281,
            "2a02:6b8:b010:5026:5220:4711:ae06:50cc",
            new String[]{},
            281,
            5610,
            "1520937380216/0950ebae9af605e65c2636c04f5e5fa6/4",
            607695260L,
            -1L,
            -1L
        );
    }
}
