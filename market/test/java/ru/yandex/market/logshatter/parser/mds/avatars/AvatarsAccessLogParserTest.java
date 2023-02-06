package ru.yandex.market.logshatter.parser.mds.avatars;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;
import ru.yandex.market.logshatter.parser.mds.CacheStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("LineLength")
public class AvatarsAccessLogParserTest {
    @Test
    public void testParseRealGet() throws Exception {
        LogParserChecker checker = new LogParserChecker(new AvatarsAccessLogParser());

        checker.check(
            "tskv\t" +
                "tskv_format=avatars-access-log\t" +
                "timestamp=2021-04-20T11:46:04\t" +
                "timezone=+0300\t" +
                "status=200\t" +
                "protocol=HTTP/2.0\t" +
                "method=GET\t" +
                "request=/get-turbo/3081272/rth28e9480427aa30b50339ebc908168cdc/horizontal_288\t" +
                "referer=https://yandex.ru/search/touch/?service=www.yandex&ui=webmobileapp" +
                ".yandex&appsearch_header=1&manufacturer=HUAWEI&app_id=ru.yandex" +
                ".searchplugin&cellid=250%2C20%2C140870677%2C13122%2C0&app_version=7080501&api_key=45de325a-08de-435d" +
                "-bcc3-1ebf6e0ae41b&app_build_number=50024&scalefactor=2" +
                ".00&searchlib_ver=535&did=2e4e44de13cd90d726c944e855d49c77&clid=1902851&text=%D1%87%D1%82%D0%BE%20" +
                "%D0%BB%D1%83%D1%87%D1%88%D0%B5%20%D0%BB%D0%B8%D1%84%D0%B0%D0%BD%20%D0%B8%D0%BB%D0%B8%20%D1%82%D0%B0" +
                "%D0%B9%D0%BE%D1%82%D0%B0&uuid=93782383d1d962ba14686f5f093cc4ae&model=AUM-L41&scr_h=1358&scr_w=720" +
                "&_est_resp_time=639&app_version_name=7.85&os_version=8.0" +
                ".0&mobile-connection-type=6&internal_browser_enabled=1&query_source=type&app_platform=android" +
                "&app_req_id=1618904694121-7-627d3a2e-f2f6-4e50-a0ba-593f4b62e9da-LMETA&meta_req_id=1618904694121-7" +
                "-627d3a2e-f2f6-4e50-a0ba-593f4b62e9da-LMETA\t" +
                "cookies=yp=2147483647.ygu.1; yandexuid=6004610661543935182\t" +
                "user_agent=Mozilla/5.0 (Linux; Android 8.0.0; AUM-L41 Build/HONORAUM-L41; wv) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Version/4.0 Chrome/70.0.3538.110 Mobile Safari/537.36 YandexSearch/7.85 " +
                "YandexSearchWebView/7.85\t" +
                "vhost=avatars.mds.yandex.net\t" +
                "ip=176.59.128.63\t" +
                "x_forwarded_for=-\t" +
                "x_real_ip=-\t" +
                "request_id=972f82f052a02b95\t" +
                "args=webp=true\t" +
                "namespace=turbo\t" +
                "scheme=https\t" +
                "port=443\t" +
                "ssl_handshake_time=0.000\t" +
                "ssl_protocol=TLSv2\t" +
                "upstream_cache_status=MISS\t" +
                "upstream_addr=unix:/var/run/avatars-mds/avatars-mds.sock\t" +
                "upstream_status=200\t" +
                "http_y_service=-\t" +
                "tcpinfo_rtt=94689\t" +
                "tcpinfo_rttvar=25653\t" +
                "tcpinfo_snd_cwnd=19\t" +
                "tcpinfo_rcv_space=14100\t" +
                "tcpinfo_lost=946890\t" +
                "tcpinfo_retrans=256530\t" +
                "tcpinfo_retransmits=190\t" +
                "tcpinfo_total_retrans=141000\t" +
                "x_yandex_yarl_limit=-\t" +
                "bytes_received=67\t" +
                "bytes_sent=8342\t" +
                "content_length=-\t" +
                "upstream_content_length=100500\t" +
                "upstream_response_time=0.000\t" +
                "request_time=0.040\t" +
                "request_completion=OK\t" +
                "antirobot_result=bypassed\t" +
                "antirobot_status=200\n",
            new Date(1618908364000L),
            "turbo",                                        // 00: namespace
            "horizontal_288",                               // 01: alias
            "972f82f052a02b95",                             // 02: request_id
            "hostname.test",                                // 03: host
            "176.59.128.63",                         // 04: ipv6
            "176.59.128.63",                         // 05: client_ipv6
            Collections.emptyList(),                        // 06: x_forwarded_for
            "",                                             // 07: x_real_ip
            "Mozilla/5.0 (Linux; Android 8.0.0; AUM-L41 Build/HONORAUM-L41; wv) AppleWebKit/537.36 (KHTML, like " +
                "Gecko) Version/4.0 Chrome/70.0.3538.110 Mobile Safari/537.36 YandexSearch/7.85 YandexSearchWebView/7" +
                ".85", // 08: user_agent
            "https://yandex.ru/search/touch/?service=www.yandex&ui=webmobileapp" +
                ".yandex&appsearch_header=1&manufacturer=HUAWEI&app_id=ru.yandex" +
                ".searchplugin&cellid=250%2C20%2C140870677%2C13122%2C0&app_version=7080501&api_key=45de325a-08de-435d" +
                "-bcc3-1ebf6e0ae41b&app_build_number=50024&scalefactor=2" +
                ".00&searchlib_ver=535&did=2e4e44de13cd90d726c944e855d49c77&clid=1902851&text=%D1%87%D1%82%D0%BE%20" +
                "%D0%BB%D1%83%D1%87%D1%88%D0%B5%20%D0%BB%D0%B8%D1%84%D0%B0%D0%BD%20%D0%B8%D0%BB%D0%B8%20%D1%82%D0%B0" +
                "%D0%B9%D0%BE%D1%82%D0%B0&uuid=93782383d1d962ba14686f5f093cc4ae&model=AUM-L41&scr_h=1358&scr_w=720" +
                "&_est_resp_time=639&app_version_name=7.85&os_version=8.0" +
                ".0&mobile-connection-type=6&internal_browser_enabled=1&query_source=type&app_platform=android" +
                "&app_req_id=1618904694121-7-627d3a2e-f2f6-4e50-a0ba-593f4b62e9da-LMETA&meta_req_id=1618904694121-7" +
                "-627d3a2e-f2f6-4e50-a0ba-593f4b62e9da-LMETA", // 09: referer
            "HTTP/2.0",                                     // 10: protocol
            "https",                                        // 11: scheme
            "GET",                                          // 12: method
            "avatars.mds.yandex.net",                       // 13: vhost
            443,                                            // 14: port
            HandlerType.GET,                                // 15: handler_type
            "/get-turbo/3081272/rth28e9480427aa30b50339ebc908168cdc/horizontal_288", // 16: request
            0,                                              // 17: ssl_handshake_time
            "TLSv2",                                        // 18: ssl_protocol
            40,                                             // 19: request_time
            0L,                                             // 10: content_length
            67L,                                            // 21: bytes_received
            Arrays.asList("yp=2147483647.ygu.1", "yandexuid=6004610661543935182"), // 22: cookies
            200,                                            // 23: status
            8342L,                                          // 24: bytes_sent
            94689,                                          // 25: tcpinfo_rtt
            25653,                                          // 26: tcpinfo_rttvar
            19,                                             // 27: tcpinfo_snd_cwnd
            14100,                                          // 28: tcpinfo_rcv_space
            946890L,                                        // 29: tcpinfo_lost
            256530L,                                        // 20: tcpinfo_retrans
            190L,                                           // 31: tcpinfo_retransmits
            141000L,                                        // 32: tcpinfo_total_retrans
            "unix:/var/run/avatars-mds/avatars-mds.sock",   // 33: upstream_addr
            200,                                            // 34: upstream_status
            100500L,                                            // 35: upstream_content_length
            CacheStatus.MISS,                               // 36: upstream_cache_status
            0,                                              // 37: upstream_response_time
            "",                                             // 38: http_y_service
            Collections.emptyList(),                        // 39: x_yandex_yarl_limit
            "",                                             // 40: orig_format
            0L,                                             // 41: couple_id
            Collections.singletonList("webp"),              // 42: kvKeys (query args)
            Collections.singletonList("true"),              // 43: kvValues (query args)
            "OK",                                           // 44: request_completion
            "bypassed",                                     // 45: antirobot_result
            200                                             // 46: antirobot_status
        );
    }

    @Test
    public void testParseRealPost() throws Exception {
        LogParserChecker checker = new LogParserChecker(new AvatarsAccessLogParser());

        checker.check(
            "tskv\t" +
                "tskv_format=mds-int-access-log\t" +
                "timestamp=2021-04-20T12:26:05\t" +
                "timezone=+0300\t" +
                "status=200\t" +
                "protocol=HTTP/1.1\t" +
                "method=POST\t" +
                "request=/put-main-images/?expire=336h\t" +
                "referer=-\t" +
                "cookies=-\t" +
                "user_agent=-\t" +
                "vhost=avatars-int.mds.yandex.net\t" +
                "ip=2a02:6b8:c0e:63:0:604:db7:a2ee\t" +
                "x_forwarded_for=-\t" +
                "x_real_ip=-\t" +
                "request_id=16ec277d09688b3f\t" +
                "args=expire=336h\t" +
                "namespace=main-images\t" +
                "scheme=http\t" +
                "port=13000\t" +
                "ssl_handshake_time=-\t" +
                "ssl_protocol=TLSv2\t" +
                "upstream_cache_status=-\t" +
                "upstream_addr=unix:/var/run/avatars-mds/avatars-mds.sock\t" +
                "upstream_status=200\t" +
                "http_y_service=-\t" +
                "tcpinfo_rtt=170\t" +
                "tcpinfo_rttvar=68\t" +
                "tcpinfo_snd_cwnd=12\t" +
                "tcpinfo_rcv_space=13900\t" +
                "tcpinfo_lost=1700\t" +
                "tcpinfo_retrans=680\t" +
                "tcpinfo_retransmits=120\t" +
                "tcpinfo_total_retrans=139000\t" +
                "x_yandex_yarl_limit=-\t" +
                "bytes_received=8609\t" +
                "bytes_sent=1558\t" +
                "content_length=7982\t" +
                "upstream_response_time=0.008\t" +
                "upstream_content_length=100500\t" +
                "request_time=0.007\t" +
                "orig_format=WEBP\t" +
                "couple_id=1891246\t" +
                "request_completion=OK\t" +
                "antirobot_result=bypassed\t" +
                "antirobot_status=200\n",
            new Date(1618910765000L),
            "main-images",                          // 00: namespace
            "",                                     // 01: alias
            "16ec277d09688b3f",                     // 02: request_id
            "hostname.test",                        // 03: host
            "2a02:6b8:c0e:63:0:604:db7:a2ee",       // 04: ipv6
            "2a02:6b8:c0e:63:0:604:db7:a2ee",       // 05: client_ipv6
            Collections.emptyList(),                // 06: x_forwarded_for
            "",                                     // 07: x_real_ip
            "",                                     // 08: user_agent
            "",                                     // 09: referer
            "HTTP/1.1",                             // 10: protocol
            "http",                                 // 11: scheme
            "POST",                                 // 12: method
            "avatars-int.mds.yandex.net",           // 13: vhost
            13000,                                  // 14: port
            HandlerType.PUT,                        // 15: handler_type
            "/put-main-images",                     // 16: request
            0,                                      // 17: ssl_handshake_time
            "TLSv2",                                // 18: ssl_protocol
            7,                                      // 19: request_time
            7_982L,                                 // 10: content_length
            8_609L,                                 // 21: bytes_received
            Collections.emptyList(),                // 22: cookies
            200,                                    // 23: status
            1_558L,                                 // 24: bytes_sent
            170,                                    // 25: tcpinfo_rtt
            68,                                     // 26: tcpinfo_rttvar
            12,                                     // 27: tcpinfo_snd_cwnd
            13900,                                  // 28: tcpinfo_rcv_space
            1700L,                                  // 29: tcpinfo_lost
            680L,                                   // 20: tcpinfo_retrans
            120L,                                   // 31: tcpinfo_retransmits
            139000L,                                // 32: tcpinfo_total_retrans
            "unix:/var/run/avatars-mds/avatars-mds.sock", // 33: upstream_addr
            200,                                    // 34: upstream_status
            100500L,                                // 35: upstream_content_length
            CacheStatus.UNKNOWN,                    // 36: upstream_cache_status
            8,                                      // 37: upstream_response_time
            "",                                     // 38: http_y_service
            Collections.emptyList(),                // 39: x_yandex_yarl_limit
            "WEBP",                                 // 40: orig_format
            1891246L,                               // 41: couple_id
            Collections.singletonList("expire"),    // 42: kvKeys (query args)
            Collections.singletonList("336h"),      // 43: kvValues (query args)
            "OK",                                   // 44: request_completion
            "bypassed",                             // 45: antirobot_result
            200                                     // 46: antirobot_status
        );
    }

    @Test
    public void testParseRealI() throws Exception {
        LogParserChecker checker = new LogParserChecker(new AvatarsAccessLogParser());

        checker.check(
            "tskv\t" +
                "tskv_format=avatars-access-log\t" +
                "timestamp=2021-04-21T16:25:14\t" +
                "timezone=+0300\t" +
                "status=200\t" +
                "protocol=HTTP/2.0\t" +
                "method=GET\t" +
                "request=/i?id=28bf21af0b6b2f0fcd5dff7ab9d398ae-1003139-vthumb&shower=3&n=1040&w=176&h=100\t" +
                "referer=https://yandex.ru/search/?text=%D0%BF%D0%BB%D1%8F%D0%B6+%D1%81%D0%B5%D0%BA%D1%81+%D1%81%D0" +
                "%BA%D1%80%D1%8B%D1%82%D0%B0%D1%8F+%D0%BA%D0%B0%D0%BC%D0%B5%D1%80%D0%B0&lr=213&suggest_reqid" +
                "=878405436158489778114951804163568&src=suggest_Pb\t" +
                "cookies=-\t" +
                "user_agent=Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0" +
                ".4324.150 Atom/10.1.0.42 Safari/537.36\t" +
                "vhost=avatars.mds.yandex.net\t" +
                "ip=77.50.136.190\t" +
                "x_forwarded_for=-\t" +
                "x_real_ip=-\t" +
                "request_id=da97d5ff4e1f7320\t" +
                "args=thumbnail=%7B%22command%22%3A%22gravity%22%2C%22spread%22%3A3%2C%22gravity-type%22%3A%22center" +
                "%22%2C%22quality%22%3A90%2C%22height%22%3A100%2C%22width%22%3A176%7D\t" +
                "namespace=vthumb\t" +
                "scheme=https\t" +
                "port=443\t" +
                "ssl_handshake_time=0.000\t" +
                "ssl_protocol=TLSv2\t" +
                "upstream_cache_status=-\t" +
                "upstream_addr=unix:/var/run/avatars-mds/avatars-mds.sock\t" +
                "upstream_status=200\t" +
                "http_y_service=-\t" +
                "tcpinfo_rtt=14204\t" +
                "tcpinfo_rttvar=11777\t" +
                "tcpinfo_snd_cwnd=58\t" +
                "tcpinfo_rcv_space=28200\t" +
                "tcpinfo_lost=142040\t" +
                "tcpinfo_retrans=117770\t" +
                "tcpinfo_retransmits=580\t" +
                "tcpinfo_total_retrans=282000\t" +
                "x_yandex_yarl_limit=-\t" +
                "bytes_received=73\t" +
                "bytes_sent=12636\t" +
                "content_length=-\t" +
                "upstream_content_length=100500\t" +
                "upstream_response_time=0.004\t" +
                "request_time=0.001\t" +
                "request_completion=OK\t" +
                "antirobot_result=bypassed\t" +
                "antirobot_status=200\n",
            new Date(1619011514000L),
            "vthumb",                                           // 00: namespace
            "",                                                 // 01: alias
            "da97d5ff4e1f7320",                                 // 02: request_id
            "hostname.test",                                    // 03: host
            "77.50.136.190",                             // 04: ipv6
            "77.50.136.190",                             // 05: client_ipv6
            Collections.emptyList(),                            // 06: x_forwarded_for
            "",                                                 // 07: x_real_ip
            "Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.150 Atom/10" +
                ".1.0.42 Safari/537.36", // 08: user_agent
            "https://yandex.ru/search/?text=%D0%BF%D0%BB%D1%8F%D0%B6+%D1%81%D0%B5%D0%BA%D1%81+%D1%81%D0%BA%D1%80%D1" +
                "%8B%D1%82%D0%B0%D1%8F+%D0%BA%D0%B0%D0%BC%D0%B5%D1%80%D0%B0&lr=213&suggest_reqid" +
                "=878405436158489778114951804163568&src=suggest_Pb", // 09: referer
            "HTTP/2.0",                                         // 10: protocol
            "https",                                            // 11: scheme
            "GET",                                              // 12: method
            "avatars.mds.yandex.net",                           // 13: vhost
            443,                                                // 14: port
            HandlerType.I,                                      // 15: handler_type
            "/i",                                               // 16: request
            0,                                                  // 17: ssl_handshake_time
            "TLSv2",                                            // 18: ssl_protocol
            1,                                                  // 19: request_time
            0L,                                                 // 10: content_length
            73L,                                                // 21: bytes_received
            Collections.emptyList(),                            // 22: cookies
            200,                                                // 23: status
            12636L,                                             // 24: bytes_sent
            14204,                                              // 25: tcpinfo_rtt
            11777,                                              // 26: tcpinfo_rttvar
            58,                                                 // 27: tcpinfo_snd_cwnd
            28200,                                              // 28: tcpinfo_rcv_space
            142040L,                                            // 29: tcpinfo_lost
            117770L,                                            // 30: tcpinfo_retrans
            580L,                                               // 31: tcpinfo_retransmits
            282000L,                                            // 32: tcpinfo_total_retrans
            "unix:/var/run/avatars-mds/avatars-mds.sock",       // 33: upstream_addr
            200,                                                // 34: upstream_status
            100500L,                                            // 35: upstream_content_length
            CacheStatus.UNKNOWN,                                // 36: upstream_cache_status
            4,                                                  // 37: upstream_response_time
            "",                                                 // 38: http_y_service
            Collections.emptyList(),                            // 39: x_yandex_yarl_limit
            "",                                                 // 40: orig_format
            0L,                                                 // 41: couple_id
            Arrays.asList("shower", "thumbnail", "w", "h", "id", "n"), // 42: kv_keys
            Arrays.asList(
                "3",
                "{\"command\":\"gravity\",\"spread\":3,\"gravity-type\":\"center\",\"quality\":90,\"height\":100," +
                    "\"width\":176}",
                "176",
                "100",
                "28bf21af0b6b2f0fcd5dff7ab9d398ae-1003139-vthumb",
                "1040"
            ), // 43: kv_values
            "OK",                                   // 44: request_completion
            "bypassed",                             // 45: antirobot_result
            200                                     // 46: antirobot_status
        );
    }

    @Test
    public void testParseAllFilled() throws Exception {
        LogParserChecker checker = new LogParserChecker(new AvatarsAccessLogParser());

        checker.check(
            "tskv\t" +
                "tskv_format=avatars-access-log\t" +
                "timestamp=2021-04-20T12:26:19\t" +
                "timezone=+0300\t" +
                "status=200\t" +
                "protocol=HTTP/2.0\t" +
                "method=GET\t" +
                "request=/get-entity_search/68218/106499026/S114x114_2x\t" +
                "referer=https://referer/address\t" +
                "cookies=cookie1=123; cookie2=321\t" +
                "user_agent=UserAgent value\t" +
                "vhost=avatars.mds.yandex.net\t" +
                "ip=94.100.181.200\t" +
                "x_forwarded_for=188.170.81.252\t" +
                "x_real_ip=188.170.81.252\t" +
                "request_id=a3bdfc88976055d9\t" +
                "args=webp=true\t" +
                "namespace=entity_search\t" +
                "scheme=https\t" +
                "port=443\t" +
                "ssl_handshake_time=0.111\t" +
                "ssl_protocol=SomeSSLProto\t" +
                "upstream_cache_status=MISS\t" +
                "upstream_addr=1.1.1.1:80\t" +
                "upstream_status=200\t" +
                "http_y_service=y-service-name\t" +
                "tcpinfo_rtt=111722\t" +
                "tcpinfo_rttvar=15982\t" +
                "tcpinfo_snd_cwnd=10\t" +
                "tcpinfo_rcv_space=14100\t" +
                "tcpinfo_lost=1117220\t" +
                "tcpinfo_retrans=159820\t" +
                "tcpinfo_retransmits=100\t" +
                "tcpinfo_total_retrans=141000\t" +
                "x_yandex_yarl_limit=some-quota-name\t" +
                "bytes_received=52\t" +
                "bytes_sent=19397\t" +
                "content_length=111222\t" +
                "upstream_content_length=100500\t" +
                "upstream_response_time=0.222\t" +
                "orig_format=JPG\t" +
                "couple_id=1234\t" +
                "request_time=0.333\t" +
                "request_completion=OK\t" +
                "antirobot_result=bypassed\t" +
                "antirobot_status=200\n",
            new Date(1618910779000L),
            "entity_search",                                    // 00: namespace
            "S114x114_2x",                                      // 01: alias
            "a3bdfc88976055d9",                                 // 02: request_id
            "hostname.test",                                    // 03: host
            "94.100.181.200",                            // 04: ipv6
            "188.170.81.252",                            // 05: client_ipv6
            Collections.singletonList("188.170.81.252"), // 06: x_forwarded_for
            "188.170.81.252",                            // 07: x_real_ip
            "UserAgent value",                                  // 08: user_agent
            "https://referer/address",                          // 09: referer
            "HTTP/2.0",                                         // 10: protocol
            "https",                                            // 11: scheme
            "GET",                                              // 12: method
            "avatars.mds.yandex.net",                           // 13: vhost
            443,                                                // 14: port
            HandlerType.GET,                                    // 15: handler_type
            "/get-entity_search/68218/106499026/S114x114_2x",   // 16: request
            111,                                                // 17: ssl_handshake_time
            "SomeSSLProto",                                     // 18: ssl_protocol
            333,                                                // 19: request_time
            111_222L,                                           // 10: content_length
            52L,                                                // 21: bytes_received
            Arrays.asList("cookie1=123", "cookie2=321"),        // 22: cookies
            200,                                                // 23: status
            19397L,                                             // 24: bytes_sent
            111722,                                             // 25: tcpinfo_rtt
            15982,                                              // 26: tcpinfo_rttvar
            10,                                                 // 27: tcpinfo_snd_cwnd
            14100,                                              // 28: tcpinfo_rcv_space
            1117220L,                                           // 29: tcpinfo_lost
            159820L,                                            // 20: tcpinfo_retrans
            100L,                                               // 31: tcpinfo_retransmits
            141000L,                                            // 32: tcpinfo_total_retrans
            "1.1.1.1:80",                                       // 33: upstream_addr
            200,                                                // 34: upstream_status
            100500L,                                            // 35: upstream_content_length
            CacheStatus.MISS,                                   // 36: upstream_cache_status
            222,                                                // 37: upstream_response_time
            "y-service-name",                                   // 38: http_y_service
            Collections.singletonList("some-quota-name"),       // 39: x_yandex_yarl_limit
            "JPG",                                              // 40: orig_format
            1234L,                                              // 41: couple_id
            Collections.singletonList("webp"),                  // 42: kv_keys
            Collections.singletonList("true"),                  // 43: kv_values
            "OK",                                               // 44: request_completion
            "bypassed",                                         // 45: antirobot_result
            200                                                 // 46: antirobot_status
        );
    }

    @Test
    public void testParseAllEmpty() throws Exception {
        LogParserChecker checker = new LogParserChecker(new AvatarsAccessLogParser());

        checker.check(
            "tskv\t" +
                "tskv_format=avatars-access-log\t" +
                "timestamp=2021-04-21T16:25:14\t" +
                "timezone=+0300\t" +
                "status=-\t" +
                "protocol=-\t" +
                "method=-\t" +
                "request=/get-asdf\t" +
                "referer=-\t" +
                "cookies=-\t" +
                "user_agent=-\t" +
                "vhost=-\t" +
                "ip=-\t" +
                "x_forwarded_for=-\t" +
                "x_real_ip=-\t" +
                "request_id=-\t" +
                "args=-\t" +
                "namespace=-\t" +
                "scheme=-\t" +
                "port=-\t" +
                "ssl_handshake_time=-\t" +
                "ssl_protocol=-\t" +
                "upstream_cache_status=-\t" +
                "upstream_addr=-\t" +
                "upstream_status=-\t" +
                "http_y_service=-\t" +
                "tcpinfo_rtt=-\t" +
                "tcpinfo_rttvar=-\t" +
                "tcpinfo_snd_cwnd=-\t" +
                "tcpinfo_rcv_space=-\t" +
                "tcpinfo_lost=-\t" +
                "tcpinfo_retrans=-\t" +
                "tcpinfo_retransmits=-\t" +
                "tcpinfo_total_retrans=-\t" +
                "x_yandex_yarl_limit=-\t" +
                "bytes_received=-\t" +
                "bytes_sent=-\t" +
                "content_length=-\t" +
                "upstream_content_length=-\t" +
                "upstream_response_time=-\t" +
                "request_time=-\t" +
                "request_completion=\t" +
                "antirobot_result=\t" +
                "antirobot_status=\n",
            new Date(1619011514000L),
            "unknown",                      // 00: namespace
            "",                             // 01: alias
            "",                             // 02: request_id
            "hostname.test",                // 03: host
            "",                             // 04: ipv6
            "",                             // 05: client_ipv6
            Collections.emptyList(),        // 06: x_forwarded_for
            "",                             // 07: x_real_ip
            "",                             // 08: user_agent
            "",                             // 09: referer
            "",                             // 10: protocol
            "",                             // 11: scheme
            "",                             // 12: method
            "",                             // 13: vhost
            0,                              // 14: port
            HandlerType.GET,                // 15: handler_type
            "/get-asdf",                    // 16: request
            0,                              // 17: ssl_handshake_time
            "",                             // 18: ssl_protocol
            0,                              // 19: request_time
            0L,                             // 10: content_length
            0L,                             // 21: bytes_received
            Collections.emptyList(),        // 22: cookies
            0,                              // 23: status
            0L,                             // 24: bytes_sent
            0,                              // 25: tcpinfo_rtt
            0,                              // 26: tcpinfo_rttvar
            0,                              // 27: tcpinfo_snd_cwnd
            0,                              // 28: tcpinfo_rcv_space
            0L,                             // 29: tcpinfo_lost
            0L,                             // 20: tcpinfo_retrans
            0L,                             // 31: tcpinfo_retransmits
            0L,                             // 32: tcpinfo_total_retrans
            "",                             // 33: upstream_addr
            0,                              // 34: upstream_status
            0L,                             // 35: upstream_content_length
            CacheStatus.UNKNOWN,            // 36: upstream_cache_status
            0,                              // 37: upstream_response_time
            "",                             // 38: http_y_service
            Collections.emptyList(),        // 39: x_yandex_yarl_limit
            "",                             // 40: orig_format
            0L,                             // 41: couple_id
            Collections.emptyList(),        // 42: kv_keys
            Collections.emptyList(),        // 43: kv_values
            "",                             // 44: request_completion
            "",                             // 45: antirobot_result
            0                               // 46: antirobot_status
        );
    }

    @Test
    public void testParseGiltered() throws Exception {
        LogParserChecker checker = new LogParserChecker(new AvatarsAccessLogParser());

        // disallowed request
        checker.checkEmpty(
            "tskv\ttskv_format=avatars-access-log\ttimestamp=2021-04-20T12:26:31\ttimezone=+0300\tstatus=200" +
                "\tprotocol=HTTP/1.1\tmethod=GET\trequest=/ping\treferer=-\tcookies=-\tuser_agent=curl/7.35" +
                ".0\tvhost=localhost\tip=::1\tx_forwarded_for=-\tx_real_ip=-\trequest_id=8515a3ab625ea4a8\targs" +
                "=-\tscheme=http\tport=80\tssl_handshake_time=-\tupstream_cache_status=-\tupstream_addr=unix:/var/run" +
                "/avatars-mds/avatars-mds.sock\tupstream_status=200\thttp_y_service=-\ttcpinfo_rtt=26\ttcpinfo_rttvar" +
                "=11\ttcpinfo_snd_cwnd=11\ttcpinfo_rcv_space=43690\tx_yandex_yarl_limit=-\tbytes_received=77" +
                "\tbytes_sent=415\tcontent_length=-\tupstream_response_time=0.000\trequest_time=0" +
                ".000\trequest_completion=OK\tantirobot_result=bypassed\tantirobot_status=200"
        );
    }

    @Test
    public void testPrepareRequest() {
        assertEquals("", AvatarsAccessLogParser.prepareRequest(""));
        assertEquals("/get-yabs_performance/3928662/2a0000017606fc5c4509aadc908afaacfcb9/small",
            AvatarsAccessLogParser.prepareRequest("/get-yabs_performance/3928662/2a0000017606fc5c4509aadc908afaacfcb9" +
                "/small"));
        assertEquals("/get-znatoki-cover/1220555/2a0000017816dc0ec2c986fbf2f2f9bc0c75/720x240",
            AvatarsAccessLogParser.prepareRequest("/avatars/get-znatoki-cover/1220555" +
                "/2a0000017816dc0ec2c986fbf2f2f9bc0c75/720x240"));
    }

    @Test
    public void testParseRequest() {
        Map<String, String> args = new HashMap<>();

        assertEquals("720x240", AvatarsAccessLogParser.parseRequest("/get-znatoki-cover/1220555" +
            "/2a0000017816dc0ec2c986fbf2f2f9bc0c75/720x240", args).get("alias"));
        assertEquals("smart_crop_516x290_card_white", AvatarsAccessLogParser.parseRequest("/get-zen_doc/3774499" +
            "/-3481070416654282731/smart_crop_516x290_card_white", args).get("alias"));

        assertFalse(AvatarsAccessLogParser.parseRequest("/get-znatoki", args).containsKey("alias"));
        assertFalse(AvatarsAccessLogParser.parseRequest("/get-znatoki/1220555", args).containsKey("alias"));
        assertFalse(AvatarsAccessLogParser.parseRequest("/get-znatoki/1220555/2a0000017816dc0ec2c986fbf2f2f9bc0c75",
            args).containsKey("alias"));
        assertFalse(AvatarsAccessLogParser.parseRequest("/get-znatoki/1220555/2a0000017816dc0ec2c986fbf2f2f9bc0c75/",
            args).containsKey("alias"));
        assertFalse(AvatarsAccessLogParser.parseRequest("/get-znatoki/1220555/2a0000017816dc0ec2c986fbf2f2f9bc0c75" +
            "/720x240/test", args).containsKey("alias"));
        assertFalse(AvatarsAccessLogParser.parseRequest("/get-znatoki/1220555/2a0000017816dc0ec2c986fbf2f2f9bc0c75" +
            "//test", args).containsKey("alias"));
    }

    @Test
    public void testIsAllowedRequest() {
        assertFalse(AvatarsAccessLogParser.isAllowedRequest(""));
        assertFalse(AvatarsAccessLogParser.isAllowedRequest("/ping"));
        assertFalse(AvatarsAccessLogParser.isAllowedRequest("/unistat"));
        assertFalse(AvatarsAccessLogParser.isAllowedRequest("/favicon.ico"));
        assertFalse(AvatarsAccessLogParser.isAllowedRequest("/robots.txt"));

        assertTrue(AvatarsAccessLogParser.isAllowedRequest("/"));
        assertTrue(AvatarsAccessLogParser.isAllowedRequest("/timetail"));
        assertTrue(AvatarsAccessLogParser.isAllowedRequest("/abrakadabra!"));
        assertTrue(AvatarsAccessLogParser.isAllowedRequest("/i"));
        assertTrue(AvatarsAccessLogParser.isAllowedRequest("/get-znatoki-cover/1220555" +
            "/2a0000017816dc0ec2c986fbf2f2f9bc0c75/720x240"));
        assertTrue(AvatarsAccessLogParser.isAllowedRequest("/delete-vthumb/904110/38842623d9543f487146b7058a6a6d0a"));
        assertTrue(AvatarsAccessLogParser.isAllowedRequest("/genurlsign-zen-personal/4303171/secure_3b56a5ee-75d3" +
            "-4368-a586-3031749aeb71/optimize?dynamic-watermark=1618974573623-6907689621618933296" +
            "-607f976d2862bf186e187ee2&expire-time=1800"));
        assertTrue(AvatarsAccessLogParser.isAllowedRequest("/getimageinfo-images-cbir/4488757" +
            "/2lPmSlC9X7QXUSmeD5OwjA9621?"));
        assertTrue(AvatarsAccessLogParser.isAllowedRequest("/getinfo-direct/223179/tIXvAVQLI-vbDj95J6bwpg/meta"));
        assertTrue(AvatarsAccessLogParser.isAllowedRequest("/put-main-images/?expire=336h"));
        assertTrue(AvatarsAccessLogParser.isAllowedRequest("/statistics-kino-vod-persons-gallery"));
    }
}
