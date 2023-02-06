package ru.yandex.market.logshatter.parser.operator_window;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;
import ru.yandex.market.logshatter.parser.trace.Environment;

public class OperatorWindowNginxTskvLogParserTest {

    LogParserChecker checker = new LogParserChecker(new OperatorWindowNginxTskvLogParser());

    @Test
    public void parse() throws Exception {
        String line = "tskv    \ttskv_format=tskv-market-default    \ttimestamp=2021-10-18T00:30:20   " +
            "\ttimezone=+0300    \tstatus=204    \tprotocol=HTTP/1.1    \tmethod=POST    " +
            "\trequest=/api/jmf/employee/unsubscribe/webpush/topic    \treferer=https://ow.market.yandex-team" +
            ".ru/entity/test_entity/edit    \tcookies=yandexuid=738296621618824840;    \tgdpr=0;    " +
            "\t_ym_uid=1618824842899898241;    \t_ym_d=1618824842;    \tyandex_login=qwerty;    \tgdpr=0;    " +
            "\tXSRF-TOKEN=85c55b82-28b9-41ee-bb87-2ce3414b91d5;    \t_ym_isad=2;    \typ=1949852941.udn" +
            ".cDprYWR5a292YS1h;    \tys=udn.cDprYWR5a292YS1h;    " +
            "\tL=UlF2fQAJZgRjd1oHWGFfW3pPd2sNZE8JBS0rHiA7ORQdFg==.1634492941.5870.350356" +
            ".704489ab560fbae37bdbc142351e61ff;    \t_ym_visorc=w;    \tfileDownload=true;    " +
            "\tSession_id=3:1634500173.5.0.1618831947397:xVisVQ:38.1.100:jV2sVQ.101:1634492941" +
            ".102:1634500173|1120000000347828.15660994.4002.2:15660994|5:180408.268779" +
            ".XXXXXXXXXXXXXXXXXXXXXXXXXXX;    \tsessionid2=3:1634500173.5.0.1618831947397:xVisVQ:38.1.100:jV2sVQ" +
            ".101:1634492941.102:1634500173|1120000000347828.15660994.4002.2:15660994|5:180408.268779" +
            ".XXXXXXXXXXXXXXXXXXXXXXXXXXX    \tuser_agent=Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36" +
            " (KHTML, like Gecko) Chrome/93.0.4577.82 YaBrowser/21.9.1.686 Yowser/2.5 Safari/537.36    \tvhost=ow" +
            ".market.yandex-team.ru    \tip=2a02:6b8:c02:45a:0:577:e299:ec6c    \tx_forwarded_for=85.172.93.141  " +
            "  \tx_real_ip=85.172.93.141    \tbytes_sent=1999    \tpage_id=-    \tpage_type=-    " +
            "\treq_id=1634506199082/193a61b7d3a169e1a289aa4cba724e84    \treq_id_seq=-    \tupstream_resp_time=0" +
            ".016    \treq_time=0.017    \tscheme=http    \tdevice_type=-    \tx_sub_req_id=-    " +
            "\tyandexuid=738296621618824840    \tssl_handshake_time=-   \tmarket_buckets=-    " +
            "\tupstream_addr=[::1]:13151    \tupstream_header_time=0.016    \tupstream_status=204    " +
            "\tmarket_req_id=1634506199082/193a61b7d3a169e1a289aa4cba724e84    \tmsec=1634506220.051    " +
            "\ticookie=-    \trequest_tags=-    \tx_return_code=-    \tx_passport_uid=-    \tsuspiciousness=-    " +
            "\tcrawler=-    \tdegradation=-    \tnanny_service_id=- ";

        checker.setOrigin("market-health-dev");
        checker.setParam("logbroker://market-health-dev", "DEVELOPMENT");

        checker.check(
            line,
            1634506220,
            checker.getHost(),
            "ow.market.yandex-team.ru",
            "/api/jmf/employee/unsubscribe/webpush/topic",
            "POST",
            204,
            16,
            true,
            "",
            "",
            "",
            "85.172.93.141",
            "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.82 " +
                "YaBrowser/21.9.1.686 Yowser/2.5 Safari/537.36",
            new String[]{},
            "738296621618824840",
            "",
            "",
            17,
            0,
            1999,
            "1634506199082/193a61b7d3a169e1a289aa4cba724e84",
            "https://ow.market.yandex-team.ru/entity/test_entity/edit",
            "UNKNOWN",
            "",
            -2,
            "",
            "",
            -2L,
            new String[]{},
            new String[]{},
            Environment.DEVELOPMENT,
            new String[0],
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
