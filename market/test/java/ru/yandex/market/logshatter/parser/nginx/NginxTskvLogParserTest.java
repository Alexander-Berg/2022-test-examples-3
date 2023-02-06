package ru.yandex.market.logshatter.parser.nginx;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;
import ru.yandex.market.logshatter.parser.trace.Environment;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 14/09/16
 */
public class NginxTskvLogParserTest {

    LogParserChecker checker = new LogParserChecker(new NginxTskvLogParser());

    @Test
    @SuppressWarnings("MethodLength")
    public void parse() throws Exception {
        String line1 = "tskv   \ttskv_format=access-log-cs-vs-tools     \ttimestamp=2016-09-14T12:38:39  " +
            "\ttimezone=+0300 \tstatus=200     \tprotocol=HTTP/1.1      \tmethod=GET     " +
            "\trequest=/catalog/54800/list?was_redir=1&srnum=22&hid=90489&text=%D1%81%D1%82%D0%BE%D0%B8%D0%BC%D0%BE" +
            "%D1%81%D1%82%D1%8C%20%D0%B6%D0%B8%D0%B4%D0%BA%D0%B8%D0%B9%20%D0%BA%D0%BB%D1%8E%D1%87%20wd-40%20%D0%B1%D1" +
            "%80%D1%8F%D0%BD%D1%81%D0%BA  \treferer=https://yandex" +
            ".ru/search/?text=%D1%81%D1%82%D0%BE%D0%B8%D0%BC%D0%BE%D1%81%D1%82%D1%8C%20%D0%B6%D0%B8%D0%B4%D0%BA%D0%B8" +
            "%D0%B9%20%D0%BA%D0%BB%D1%8E%D1%87%20wd-40%20%D0%B1%D1%80%D1%8F%D0%BD%D1%81%D0%BA&lr=191       " +
            "\tcookies=Session_id=3:1575638600.5.0.1575039371313:LkUrZA:1b.1|680700992.0.2|719237914.599229.2" +
            ".2:599229|209115.740393.XXX; yandexuid=7595716151468495019; fuid01=578774ad2333340c" +
            ".SNm7Wty0KHw6Lrh2F7scIM0CyVuUgt9G2XEJaJvqpsLX-BoSILj6JDj1qtCAIJ8ec0cBi9gz_j755EEXbyPjTTGr7kU8WPHwo1O" +
            "-QmSMa7NwS0pYNQF6pUfHNFQkkH4B; deliveryincluded=1; euid=469201241; in-stock=1; " +
            "ps_gch=6197541648932050944; yandexmarket=10,RUR,1,,,,2,0,0,0; yandex_gid=191; " +
            "_ym_uid=14725498141004332374; L=AAB3YVB2TX9feX53emIGQGJaAQBLQAMLEk4zLjc=.1473676830.12643.387992" +
            ".0fc825b48ede025fef648ad2cb1e7bfc; _ym_isad=2; zm=m-white_bender.flex.webp" +
            ".css-https%3Awww_oT6-1vCdo2dtFX_bCFunRgypsk%3Al; _ym_visorc_731962=b; ys=wprid" +
            ".1473845853093840-849122948885944480695213-man1-3551; yp=1481436683.ww.1#1489613773.szm" +
            ".0_9375%3A1920x1080%3A2048x1011#1504949856.dsws.8#1504949856.dswa.0#1504949856.dwys.4#1501220536" +
            ".st_browser_s.8#1474099417.ygu.1#1476691441.cnps.2229408971%3Amax#1503897852.dwbs.4#1789036974.multib" +
            ".1#1474018450.clh.9403; yabs-frequency=/4/00020000001W6jbN/; uid=CmvdGVfZGphAiDxjA083Ag==; " +
            "HISTORY_UNAUTH_SESSION=true; _ym_visorc_160656=b; mxp=-|0||; " +
            "pof=%7B%22clid%22%3A%5B%22521%22%5D%2C%22mclid%22%3Anull%2C%22distr_type%22%3Anull%7D; " +
            "cpa-pof=%7B%22clid%22%3A%5B%22521%22%5D%2C%22mclid%22%3Anull%2C%22distr_type%22%3Anull%7D  " +
            "\tuser_agent=Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2490" +
            ".86 Safari/537.36 OPR/33.0.1990.115   \tvhost=market.yandex.ru \tip=188.43.12.57\tx_forwarded_for=-     " +
            " \tx_real_ip=-\tbytes_sent=49066\treq_id=4b6e892f41949052fb47330014a93b7a\treq_id_seq" +
            "=4b6e892f41949052fb47330014a93b7a    \tupstream_resp_time=0.351       \treq_time=0.359 \tscheme=https   " +
            "\tdevice_type=desktop \tx_sub_req_id=1 \tyandexuid=7595716151468495019  \tssl_handshake_time=0.000      " +
            " \tmarket_buckets=-       \tupstream_addr=unix:/var/run/yandex-market-skubi/server.sock    " +
            "\tupstream_header_time=0.341  \tupstream_status=200\tmarket_req_id=42/abc\t      " +
            "request_tags=CROSSBORDER,SHOP  \tx_return_code=-  \tx_passport_uid=-  \tsuspiciousness=-  \tcrawler=-  " +
            "\tdegradation=-  \tnanny_service_id=- \ttvm_is_used_service_ticket=true \ttvm_is_used_user_ticket=true";
        String line2 =
            "tskv   \ttskv_format=access-log-cs-vs-tools     \ttimestamp=2016-09-14T12:38:39  \ttimezone=+0300 " +
                "\tstatus=200     \tprotocol=HTTP/1.1      \tmethod=GET     " +
                "\trequest=/catalog/54800/list?was_redir=1&srnum=22&hid=90489&text=%D1%81%D1%82%D0%BE%D0%B8%D0%BC%D0" +
                "%BE%D1%81%D1%82%D1%8C%20%D0%B6%D0%B8%D0%B4%D0%BA%D0%B8%D0%B9%20%D0%BA%D0%BB%D1%8E%D1%87%20wd-40%20" +
                "%D0%B1%D1%80%D1%8F%D0%BD%D1%81%D0%BA  \treferer=https://yandex" +
                ".ru/search/?text=%D1%81%D1%82%D0%BE%D0%B8%D0%BC%D0%BE%D1%81%D1%82%D1%8C%20%D0%B6%D0%B8%D0%B4%D0%BA" +
                "%D0%B8%D0%B9%20%D0%BA%D0%BB%D1%8E%D1%87%20wd-40%20%D0%B1%D1%80%D1%8F%D0%BD%D1%81%D0%BA&lr=191       " +
                "\tcookies=Session_id=3:1575638600.5.1.1575039371313:LkUrZA:1b.1|680700992.0.2|719237914.599229.2" +
                ".2:599229|209115.740393.XXX; yandexuid=7595716151468495019; fuid01=578774ad2333340c" +
                ".SNm7Wty0KHw6Lrh2F7scIM0CyVuUgt9G2XEJaJvqpsLX-BoSILj6JDj1qtCAIJ8ec0cBi9gz_j755EEXbyPjTTGr7kU8WPHwo1O" +
                "-QmSMa7NwS0pYNQF6pUfHNFQkkH4B; deliveryincluded=1; euid=469201241; in-stock=1; " +
                "ps_gch=6197541648932050944; yandexmarket=10,RUR,1,,,,2,0,0,0; yandex_gid=191; " +
                "_ym_uid=14725498141004332374; L=AAB3YVB2TX9feX53emIGQGJaAQBLQAMLEk4zLjc=.1473676830.12643.387992" +
                ".0fc825b48ede025fef648ad2cb1e7bfc; _ym_isad=2; zm=m-white_bender.flex.webp" +
                ".css-https%3Awww_oT6-1vCdo2dtFX_bCFunRgypsk%3Al; _ym_visorc_731962=b; ys=wprid" +
                ".1473845853093840-849122948885944480695213-man1-3551; yp=1481436683.ww.1#1489613773.szm" +
                ".0_9375%3A1920x1080%3A2048x1011#1504949856.dsws.8#1504949856.dswa.0#1504949856.dwys.4#1501220536" +
                ".st_browser_s.8#1474099417.ygu.1#1476691441.cnps.2229408971%3Amax#1503897852.dwbs.4#1789036974" +
                ".multib.1#1474018450.clh.9403; yabs-frequency=/4/00020000001W6jbN/; uid=CmvdGVfZGphAiDxjA083Ag==; " +
                "HISTORY_UNAUTH_SESSION=true; _ym_visorc_160656=b; mxp=-|0||; " +
                "pof=%7B%22clid%22%3A%5B%22521%22%5D%2C%22mclid%22%3Anull%2C%22distr_type%22%3Anull%7D; " +
                "cpa-pof=%7B%22clid%22%3A%5B%22521%22%5D%2C%22mclid%22%3Anull%2C%22distr_type%22%3Anull%7D  " +
                "\tuser_agent=Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0" +
                ".2490.86 Safari/537.36 OPR/33.0.1990.115   \tvhost=market.yandex.ru \tip=188.43.12" +
                ".57\tx_forwarded_for=-      \tx_real_ip=-\tbytes_sent=49066\treq_id=4b6e892f41949052fb47330014a93b7a" +
                "\treq_id_seq=4b6e892f41949052fb47330014a93b7a    \tupstream_resp_time=0.351       \treq_time=0.359 " +
                "\tscheme=https   \tdevice_type=desktop \tx_sub_req_id=1 \tyandexuid=7595716151468495019  " +
                "\tssl_handshake_time=0.000       \tmarket_buckets=-       " +
                "\tupstream_addr=unix:/var/run/yandex-market-skubi/server.sock    \tupstream_header_time=0.341  " +
                "\tupstream_status=200\tmarket_req_id=42/abc \ttvm=NO_TICKET\t      request_tags=-  " +
                "\tx_return_code=204  \tx_passport_uid=2020808  \tsuspiciousness=0.9  \tcrawler=GoogleBot  " +
                "\tdegradation=1  \tnanny_service_id=production_market_front_desktop_vla " +
                "\ttvm_is_used_service_ticket=false \ttvm_is_used_user_ticket=false";
        String line3 = "tskv   \ttskv_format=access-log-cs-vs-tools     \ttimestamp=2016-09-14T12:38:39  " +
            "\ttimezone=+0300 \tstatus=200     \tprotocol=HTTP/1.1      \tmethod=GET     " +
            "\trequest=/catalog/54800/list?was_redir=1&srnum=22&hid=90489&text=%D1%81%D1%82%D0%BE%D0%B8%D0%BC%D0%BE" +
            "%D1%81%D1%82%D1%8C%20%D0%B6%D0%B8%D0%B4%D0%BA%D0%B8%D0%B9%20%D0%BA%D0%BB%D1%8E%D1%87%20wd-40%20%D0%B1%D1" +
            "%80%D1%8F%D0%BD%D1%81%D0%BA  \treferer=https://yandex" +
            ".ru/search/?text=%D1%81%D1%82%D0%BE%D0%B8%D0%BC%D0%BE%D1%81%D1%82%D1%8C%20%D0%B6%D0%B8%D0%B4%D0%BA%D0%B8" +
            "%D0%B9%20%D0%BA%D0%BB%D1%8E%D1%87%20wd-40%20%D0%B1%D1%80%D1%8F%D0%BD%D1%81%D0%BA&lr=191       " +
            "\tcookies=yandexuid=7595716151468495019; fuid01=578774ad2333340c" +
            ".SNm7Wty0KHw6Lrh2F7scIM0CyVuUgt9G2XEJaJvqpsLX-BoSILj6JDj1qtCAIJ8ec0cBi9gz_j755EEXbyPjTTGr7kU8WPHwo1O" +
            "-QmSMa7NwS0pYNQF6pUfHNFQkkH4B; deliveryincluded=1; euid=469201241; in-stock=1; " +
            "ps_gch=6197541648932050944; yandexmarket=10,RUR,1,,,,2,0,0,0; yandex_gid=191a; " +
            "_ym_uid=14725498141004332374; L=AAB3YVB2TX9feX53emIGQGJaAQBLQAMLEk4zLjc=.1473676830.12643.387992" +
            ".0fc825b48ede025fef648ad2cb1e7bfc; _ym_isad=2; zm=m-white_bender.flex.webp" +
            ".css-https%3Awww_oT6-1vCdo2dtFX_bCFunRgypsk%3Al; _ym_visorc_731962=b; ys=wprid" +
            ".1473845853093840-849122948885944480695213-man1-3551; yp=1481436683.ww.1#1489613773.szm" +
            ".0_9375%3A1920x1080%3A2048x1011#1504949856.dsws.8#1504949856.dswa.0#1504949856.dwys.4#1501220536" +
            ".st_browser_s.8#1474099417.ygu.1#1476691441.cnps.2229408971%3Amax#1503897852.dwbs.4#1789036974.multib" +
            ".1#1474018450.clh.9403; yabs-frequency=/4/00020000001W6jbN/; uid=CmvdGVfZGphAiDxjA083Ag==; " +
            "HISTORY_UNAUTH_SESSION=true; _ym_visorc_160656=b; mxp=-|0||; " +
            "pof=%7B%22clid%22%3A%5B%22521%22%5D%2C%22mclid%22%3Anull%2C%22distr_type%22%3Anull%7D; " +
            "cpa-pof=%7B%22clid%22%3A%5B%22521%22%5D%2C%22mclid%22%3Anull%2C%22distr_type%22%3Anull%7D  " +
            "\tuser_agent=Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2490" +
            ".86 Safari/537.36 OPR/33.0.1990.115   \tvhost=market.yandex.ru \tip=188.43.12.57\tx_forwarded_for=-     " +
            " \tx_real_ip=-\tbytes_sent=49066\treq_id=4b6e892f41949052fb47330014a93b7a\treq_id_seq" +
            "=4b6e892f41949052fb47330014a93b7a    \tupstream_resp_time=0.351       \treq_time=0.359 \tscheme=https   " +
            "\tdevice_type=desktop \tx_sub_req_id=1 \tyandexuid=7595716151468495019  \tssl_handshake_time=0.000      " +
            " \tmarket_buckets=-       \tupstream_addr=unix:/var/run/yandex-market-skubi/server.sock    " +
            "\tupstream_header_time=0.341  \tupstream_status=200\tmarket_req_id=42/abc \ttvm=NO_TICKET ";

        checker.setOrigin("market-health-dev");
        checker.setParam("logbroker://market-health-dev", "DEVELOPMENT");

        checker.check(
            line1,
            1473845919,
            checker.getHost(),
            "market.yandex.ru",
            "/catalog/54800/list?was_redir=1&srnum=22&hid=90489&text=%D1%81%D1%82%D0%BE%D0%B8%D0%BC%D0%BE%D1%81%D1%82" +
                "%D1%8C%20%D0%B6%D0%B8%D0%B4%D0%BA%D0%B8%D0%B9%20%D0%BA%D0%BB%D1%8E%D1%87%20wd-40%20%D0%B1%D1%80%D1" +
                "%8F%D0%BD%D1%81%D0%BA",
            "GET",
            200,
            351,
            true,
            "",
            "",
            "desktop",
            "188.43.12.57",
            "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2490.86 " +
                "Safari/537.36 OPR/33.0.1990.115",
            new String[]{},
            "7595716151468495019",
            "",
            "680700992",
            359,
            0,
            49066,
            "42/abc",
            "https://yandex.ru/search/?text=%D1%81%D1%82%D0%BE%D0%B8%D0%BC%D0%BE%D1%81%D1%82%D1%8C%20%D0%B6%D0%B8%D0" +
                "%B4%D0%BA%D0%B8%D0%B9%20%D0%BA%D0%BB%D1%8E%D1%87%20wd-40%20%D0%B1%D1%80%D1%8F%D0%BD%D1%81%D0%BA&lr" +
                "=191",
            "UNKNOWN",
            "10,RUR,1,,,,2,0,0,0",
            191,
            "1481436683.ww.1#1489613773.szm.0_9375%3A1920x1080%3A2048x1011#1504949856.dsws.8#1504949856.dswa" +
                ".0#1504949856.dwys.4#1501220536.st_browser_s.8#1474099417.ygu.1#1476691441.cnps" +
                ".2229408971%3Amax#1503897852.dwbs.4#1789036974.multib.1#1474018450.clh.9403",
            "wprid.1473845853093840-849122948885944480695213-man1-3551",
            469201241L,
            new String[]{},
            new String[]{},
            Environment.DEVELOPMENT,
            new String[]{"CROSSBORDER", "SHOP"},
            "",
            "",
            "",
            "",
            "",
            1,
            1
        );

        checker.check(
            line2,
            1473845919,
            checker.getHost(),
            "market.yandex.ru",
            "/catalog/54800/list?was_redir=1&srnum=22&hid=90489&text=%D1%81%D1%82%D0%BE%D0%B8%D0%BC%D0%BE%D1%81%D1%82" +
                "%D1%8C%20%D0%B6%D0%B8%D0%B4%D0%BA%D0%B8%D0%B9%20%D0%BA%D0%BB%D1%8E%D1%87%20wd-40%20%D0%B1%D1%80%D1" +
                "%8F%D0%BD%D1%81%D0%BA",
            "GET",
            204,
            351,
            true,
            "",
            "",
            "desktop",
            "188.43.12.57",
            "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2490.86 " +
                "Safari/537.36 OPR/33.0.1990.115",
            new String[]{},
            "7595716151468495019",
            "",
            "719237914",
            359,
            0,
            49066,
            "42/abc",
            "https://yandex.ru/search/?text=%D1%81%D1%82%D0%BE%D0%B8%D0%BC%D0%BE%D1%81%D1%82%D1%8C%20%D0%B6%D0%B8%D0" +
                "%B4%D0%BA%D0%B8%D0%B9%20%D0%BA%D0%BB%D1%8E%D1%87%20wd-40%20%D0%B1%D1%80%D1%8F%D0%BD%D1%81%D0%BA&lr" +
                "=191",
            "NO_TICKET",
            "10,RUR,1,,,,2,0,0,0",
            191,
            "1481436683.ww.1#1489613773.szm.0_9375%3A1920x1080%3A2048x1011#1504949856.dsws.8#1504949856.dswa" +
                ".0#1504949856.dwys.4#1501220536.st_browser_s.8#1474099417.ygu.1#1476691441.cnps" +
                ".2229408971%3Amax#1503897852.dwbs.4#1789036974.multib.1#1474018450.clh.9403",
            "wprid.1473845853093840-849122948885944480695213-man1-3551",
            469201241L,
            new String[]{},
            new String[]{},
            Environment.DEVELOPMENT,
            new String[]{},
            "2020808",
            "0.9",
            "GoogleBot",
            "1",
            "production_market_front_desktop_vla",
            0,
            0
        );

        checker.check(
            line3,
            1473845919,
            checker.getHost(),
            "market.yandex.ru",
            "/catalog/54800/list?was_redir=1&srnum=22&hid=90489&text=%D1%81%D1%82%D0%BE%D0%B8%D0%BC%D0%BE%D1%81%D1%82" +
                "%D1%8C%20%D0%B6%D0%B8%D0%B4%D0%BA%D0%B8%D0%B9%20%D0%BA%D0%BB%D1%8E%D1%87%20wd-40%20%D0%B1%D1%80%D1" +
                "%8F%D0%BD%D1%81%D0%BA",
            "GET",
            200,
            351,
            true,
            "",
            "",
            "desktop",
            "188.43.12.57",
            "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2490.86 " +
                "Safari/537.36 OPR/33.0.1990.115",
            new String[]{},
            "7595716151468495019",
            "",
            "",
            359,
            0,
            49066,
            "42/abc",
            "https://yandex.ru/search/?text=%D1%81%D1%82%D0%BE%D0%B8%D0%BC%D0%BE%D1%81%D1%82%D1%8C%20%D0%B6%D0%B8%D0" +
                "%B4%D0%BA%D0%B8%D0%B9%20%D0%BA%D0%BB%D1%8E%D1%87%20wd-40%20%D0%B1%D1%80%D1%8F%D0%BD%D1%81%D0%BA&lr" +
                "=191",
            "NO_TICKET",
            "10,RUR,1,,,,2,0,0,0",
            -1,
            "1481436683.ww.1#1489613773.szm.0_9375%3A1920x1080%3A2048x1011#1504949856.dsws.8#1504949856.dswa" +
                ".0#1504949856.dwys.4#1501220536.st_browser_s.8#1474099417.ygu.1#1476691441.cnps" +
                ".2229408971%3Amax#1503897852.dwbs.4#1789036974.multib.1#1474018450.clh.9403",
            "wprid.1473845853093840-849122948885944480695213-man1-3551",
            469201241L,
            new String[]{"yandex_gid"},
            new String[]{"191a"},
            Environment.DEVELOPMENT,
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
            "tskv   \ttskv_format=access-log-cs-vs-tools     \ttimestamp=2016-09-15T14:28:01  \ttimezone=+0300 " +
                "\tstatus=499     \tprotocol=HTTP/1.1      \tmethod=GET     " +
                "\trequest=/product/12460854/spec?hid=90639&track=char    \treferer=https://market.yandex" +
                ".ru/product/12460854?nid=59601&show-uid=73938837573669780660045&glfilter=1801946%3A1871499\tcookies" +
                "=yandexuid=7082609601473853020; _ym_uid=1473853001581739195; zm=m-white_bender.flex.webp" +
                ".css-https%3Awww_oT6-1vCdo2dtFX_bCFunRgypsk%3Al; fuid01=57d9365d1178d45a" +
                ".5VyVDnL9tuSF7nJy" +
                "-CsNsw2hgjn55Gs_bF1f9dQvnfBP5pGlRonFCAcVa6XhAZC5fXe92pAzS11O6BC3rVN2mVHV6ynKQMr0WhJRN3tDe9qSja4cmuW6" +
                "NZZcADjzxVmv; yandex_gid=11000; yabs-frequency=/4/0G0006LKsLS00000/rEiW84CcF0000TaiSd109cxOB79mG2PX/" +
                "; _ym_isad=2; yp=1476452671.ygu.1#1474197445.clh.2242347#1489706359.szm.1_13:1280x720:1706x829#14741" +
                "11050.gpauto.46_045430200000006:38_1678048:2732:0:1473938250#1473944305.dsd.1#1504974910.dsws.3#1504" +
                "974910.dswa.0#1504974910.dwbs.3; cpa-pof=%7B%22clid%22%3A%5B%22505%22%5D%2C%22mclid%22%3Anull%2C%22d" +
                "istr_type%22%3Anull%7D; deliveryincluded=1; pof=%7B%22clid%22%3A%5B%22505%22%5D%2C%22mclid%22%3Anull" +
                "%2C%22distr_type%22%3Anull%7D; uid=CmvdG1fag7glGxoFA3tEAg==; HISTORY_UNAUTH_SESSION=true; TOP10=; in" +
                "-stock=1; ps_bch=2727809933831730176; mxp=-|0|31276,0,18%3B30827,0,56%3B15093,0,65%3B31239,0,55|mark" +
                "et_category_relevance_formula%253Dfull_mode_v%253Bmarket_category_redirect_treshold%253D-1; yandexma" +
                "rket=10,RUR,1,,,,2,0,0,0; _ym_visorc_160656=b; ys=homesearchextchrome.8-19-0#wprid.1473938356087863-" +
                "14615206799721316767101389-sfront3-038; parent_reqid_seq=54fb16b4cdcb8dc2305b640ec0d4626d%2C6dfad012" +
                "aecc5015262a0f56bc5f220f%2C5d20bc0fef703f2a0150489d7f896b07%2Ca6761abcfbc0bb4983c111d7d2033e1e%2Cfe" +
                "3f124244a237b4939c6ff949a58fc4  \tuser_agent=Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML," +
                " like Gecko) Chrome/52.0.2743.116 Safari/537.36      \tvhost=market.yandex.ru \tip=85.175.4.168\tx_f" +
                "orwarded_for=-      \tx_real_ip=-    \tbytes_sent=0   \treq_id=890215afc01041def6941b4731d52a02\treq" +
                "_id_seq=-   \tupstream_resp_time=0.003 : -   \treq_time=0.158 \tscheme=https   \tdevice_type=-  \tx_" +
                "sub_req_id=-\tyandexuid=-     \tssl_handshake_time=0.034       \tmarket_buckets=-       \tupstream_a" +
                "ddr=unix:/var/run/yandex-market-skubi/server.sock : unix:/tmp/xscript-market.sock    \tupstream_head" +
                "er_time=0.003 : - \tupstream_status=522 : -"
        );
    }

    @Test
    public void testXreturnCode() throws Exception {
        checker.check(
            "tskv   \ttskv_format=access-log-cs-vs-tools     \tstatus=200\ttimestamp=2016-09-14T12:38:39  " +
                "\ttimezone=+0300 \tstatus=200     \tprotocol=HTTP/1.1      \tmethod=GET     " +
                "\trequest=/catalog/54800/list?was_redir=1&srnum=22&hid=90489&text=%D1%81%D1%82%D0%BE%D0%B8%D0%BC%D0" +
                "%BE%D1%81%D1%82%D1%8C%20%D0%B6%D0%B8%D0%B4%D0%BA%D0%B8%D0%B9%20%D0%BA%D0%BB%D1%8E%D1%87%20wd-40%20" +
                "%D0%B1%D1%80%D1%8F%D0%BD%D1%81%D0%BA  \treferer=https://yandex" +
                ".ru/search/?text=%D1%81%D1%82%D0%BE%D0%B8%D0%BC%D0%BE%D1%81%D1%82%D1%8C%20%D0%B6%D0%B8%D0%B4%D0%BA" +
                "%D0%B8%D0%B9%20%D0%BA%D0%BB%D1%8E%D1%87%20wd-40%20%D0%B1%D1%80%D1%8F%D0%BD%D1%81%D0%BA&lr=191       " +
                "\tcookies=yandexuid=7595716151468495019; fuid01=578774ad2333340c" +
                ".SNm7Wty0KHw6Lrh2F7scIM0CyVuUgt9G2XEJaJvqpsLX-BoSILj6JDj1qtCAIJ8ec0cBi9gz_j755EEXbyPjTTGr7kU8WPHwo1O" +
                "-QmSMa7NwS0pYNQF6pUfHNFQkkH4B; deliveryincluded=1; euid=469201241; in-stock=1; " +
                "ps_gch=6197541648932050944; yandexmarket=10,RUR,1,,,,2,0,0,0; yandex_gid=191; " +
                "_ym_uid=14725498141004332374; L=AAB3YVB2TX9feX53emIGQGJaAQBLQAMLEk4zLjc=.1473676830.12643.387992" +
                ".0fc825b48ede025fef648ad2cb1e7bfc; _ym_isad=2; zm=m-white_bender.flex.webp" +
                ".css-https%3Awww_oT6-1vCdo2dtFX_bCFunRgypsk%3Al; _ym_visorc_731962=b; ys=wprid" +
                ".1473845853093840-849122948885944480695213-man1-3551; yp=1481436683.ww.1#1489613773.szm" +
                ".0_9375%3A1920x1080%3A2048x1011#1504949856.dsws.8#1504949856.dswa.0#1504949856.dwys.4#1501220536" +
                ".st_browser_s.8#1474099417.ygu.1#1476691441.cnps.2229408971%3Amax#1503897852.dwbs.4#1789036974" +
                ".multib.1#1474018450.clh.9403; yabs-frequency=/4/00020000001W6jbN/; uid=CmvdGVfZGphAiDxjA083Ag==; " +
                "HISTORY_UNAUTH_SESSION=true; _ym_visorc_160656=b; mxp=-|0||; " +
                "pof=%7B%22clid%22%3A%5B%22521%22%5D%2C%22mclid%22%3Anull%2C%22distr_type%22%3Anull%7D; " +
                "cpa-pof=%7B%22clid%22%3A%5B%22521%22%5D%2C%22mclid%22%3Anull%2C%22distr_type%22%3Anull%7D  " +
                "\tuser_agent=Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0" +
                ".2490.86 Safari/537.36 OPR/33.0.1990.115   \tvhost=market.yandex.ru \tip=188.43.12" +
                ".57\tx_forwarded_for=-      \tx_real_ip=-\tbytes_sent=49066\treq_id=4b6e892f41949052fb47330014a93b7a" +
                "\treq_id_seq=4b6e892f41949052fb47330014a93b7a    \tupstream_resp_time=0.351       \treq_time=0.359 " +
                "\tscheme=https   \tdevice_type=desktop \tx_sub_req_id=1 \tyandexuid=7595716151468495019  " +
                "\tssl_handshake_time=0.000       \tmarket_buckets=-       " +
                "\tupstream_addr=unix:/var/run/yandex-market-skubi/server.sock    \tupstream_header_time=0.341  " +
                "\tupstream_status=200\tmarket_req_id=42/abc\t      request_tags=CROSSBORDER,SHOP"
        );
        assertEquals(200, checker.getFields()[4]);

        checker.check(
            "tskv   \ttskv_format=access-log-cs-vs-tools      \tstatus=200\ttimestamp=2016-09-14T12:38:39  " +
                "\ttimezone=+0300 \tstatus=200     \tprotocol=HTTP/1.1      \tmethod=GET     " +
                "\trequest=/catalog/54800/list?was_redir=1&srnum=22&hid=90489&text=%D1%81%D1%82%D0%BE%D0%B8%D0%BC%D0" +
                "%BE%D1%81%D1%82%D1%8C%20%D0%B6%D0%B8%D0%B4%D0%BA%D0%B8%D0%B9%20%D0%BA%D0%BB%D1%8E%D1%87%20wd-40%20" +
                "%D0%B1%D1%80%D1%8F%D0%BD%D1%81%D0%BA  \treferer=https://yandex" +
                ".ru/search/?text=%D1%81%D1%82%D0%BE%D0%B8%D0%BC%D0%BE%D1%81%D1%82%D1%8C%20%D0%B6%D0%B8%D0%B4%D0%BA" +
                "%D0%B8%D0%B9%20%D0%BA%D0%BB%D1%8E%D1%87%20wd-40%20%D0%B1%D1%80%D1%8F%D0%BD%D1%81%D0%BA&lr=191       " +
                "\tcookies=yandexuid=7595716151468495019; fuid01=578774ad2333340c" +
                ".SNm7Wty0KHw6Lrh2F7scIM0CyVuUgt9G2XEJaJvqpsLX-BoSILj6JDj1qtCAIJ8ec0cBi9gz_j755EEXbyPjTTGr7kU8WPHwo1O" +
                "-QmSMa7NwS0pYNQF6pUfHNFQkkH4B; deliveryincluded=1; euid=469201241; in-stock=1; " +
                "ps_gch=6197541648932050944; yandexmarket=10,RUR,1,,,,2,0,0,0; yandex_gid=191; " +
                "_ym_uid=14725498141004332374; L=AAB3YVB2TX9feX53emIGQGJaAQBLQAMLEk4zLjc=.1473676830.12643.387992" +
                ".0fc825b48ede025fef648ad2cb1e7bfc; _ym_isad=2; zm=m-white_bender.flex.webp" +
                ".css-https%3Awww_oT6-1vCdo2dtFX_bCFunRgypsk%3Al; _ym_visorc_731962=b; ys=wprid" +
                ".1473845853093840-849122948885944480695213-man1-3551; yp=1481436683.ww.1#1489613773.szm" +
                ".0_9375%3A1920x1080%3A2048x1011#1504949856.dsws.8#1504949856.dswa.0#1504949856.dwys.4#1501220536" +
                ".st_browser_s.8#1474099417.ygu.1#1476691441.cnps.2229408971%3Amax#1503897852.dwbs.4#1789036974" +
                ".multib.1#1474018450.clh.9403; yabs-frequency=/4/00020000001W6jbN/; uid=CmvdGVfZGphAiDxjA083Ag==; " +
                "HISTORY_UNAUTH_SESSION=true; _ym_visorc_160656=b; mxp=-|0||; " +
                "pof=%7B%22clid%22%3A%5B%22521%22%5D%2C%22mclid%22%3Anull%2C%22distr_type%22%3Anull%7D; " +
                "cpa-pof=%7B%22clid%22%3A%5B%22521%22%5D%2C%22mclid%22%3Anull%2C%22distr_type%22%3Anull%7D  " +
                "\tuser_agent=Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0" +
                ".2490.86 Safari/537.36 OPR/33.0.1990.115   \tvhost=market.yandex.ru \tip=188.43.12" +
                ".57\tx_forwarded_for=-      \tx_real_ip=-\tbytes_sent=49066\treq_id=4b6e892f41949052fb47330014a93b7a" +
                "\treq_id_seq=4b6e892f41949052fb47330014a93b7a    \tupstream_resp_time=0.351       \treq_time=0.359 " +
                "\tscheme=https   \tdevice_type=desktop \tx_sub_req_id=1 \tyandexuid=7595716151468495019  " +
                "\tssl_handshake_time=0.000       \tmarket_buckets=-       " +
                "\tupstream_addr=unix:/var/run/yandex-market-skubi/server.sock    \tupstream_header_time=0.341  " +
                "\tupstream_status=200\tmarket_req_id=42/abc\t      request_tags=CROSSBORDER,SHOP\tx_return_code=400"
        );
        assertEquals(400, checker.getFields()[4]);
    }
}
