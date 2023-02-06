package ru.yandex.market.sre.tools;

import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;


public class LogParserTest {
    @Test
    public void convertFullRequest1() throws Exception {
        JSONAssert.assertEquals(
                "{" +
                        "\"attempts\": 1," +
                        "\"client_addr\": \"2a02:6b8:c0f:1310:10c:b3a5:0:1fe4\"," +
                        "\"client_port\": 55460," +
                        "\"debug\": \"\"," +
                        "\"elapsed_time\": 8.139," +
                        "\"host\": \"api.content.market.yandex.ru\"," +
                        "\"method\": \"GET\"," +
                        "\"protocol\": \"HTTP/1.1\"," +
                        "\"referer\": \"https://yandex.ru\"," +
                        "\"request_size\": 0," +
                        "\"request_time\": 8.204," +
                        "\"request_timestamp\": 1577281015976," +
                        "\"request_timestamp_iso\": \"2019-12-25T16:36:55.976156+03:00\"," +
                        "\"response_size\": 876," +
                        "\"response_time\": 7.691," +
                        "\"response_timestamp\": 1577281015984," +
                        "\"response_timestamp_iso\": \"2019-12-25T16:36:55.984360+03:00\"," +
                        "\"server_name\": \"[fdee:fdee::1:216]:9080\"," +
                        "\"service_name\": \"content_api_haproxy\"," +
                        "\"status\": 200, " +
                        "\"status_text\": \"succ 200\"," +
                        "\"timestamp\": 1577281015," +
                        "\"url\":\"/v2.1.5/user/info?client=sovetnik&bigb-uid=7568015531459525968&glue=0\"," +
                        "\"user_agent\": \"Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) " +
                        "Chrome/74.0.3729.157 Safari/537.36\"," +
                        "\"x-antirobot-is-crawler\":\"GoogleBot\"," +
                        "\"x-balancer-ip\":\"2a02:6b8:0:3400::3c5\"," +
                        "\"x-balancer-port\":443," +
                        "\"x-forwarded-for\":\"82.208.113.179\"," +
                        "\"x-market-req-id\":\"1577281015976/ffa4e87eac33aa47dc10ab5a879a0500\"," +
                        "\"x-real-ip\": \"2a02:6b8:c0f:1310:10c:b3a5:0:1fe4\"," +
                        "\"x-yandex-ja3\": \"771,4865-4866-4867-49195-49199-49196-49200-52393-52392-49171-49172-156" +
                        "-157-47-53-10,0-23-65281-10-11-35-16-5-13-18-51-45-43-21,29-23-24,0\"," +
                        "\"x-yandex-ja4\": \"1027-1283-1539-2052-2053-2054-2057-2058-2059-1025-1281-1537-1026-771-" +
                        "769-770-515-513-514,1027-1283-1539-2052-2053-2054-2057-2058-2059-1025-1281-1537-1026-771-" +
                        "769-770-515-513-514,772-771-770-769,,23,1\"" +
                        "}",
                LogParser.convert(
                        String.join("\t", new String[]{
                                "[2a02:6b8:c0f:1310:10c:b3a5:0:1fe4]:55460",
                                "2019-12-25T16:36:55.976156+0300",
                                "\"GET /v2.1.5/user/info?client=sovetnik&bigb-uid=7568015531459525968&glue=0 HTTP/1" +
                                        ".1\"",
                                "0.008204s",
                                "\"https://yandex.ru\"",
                                "\"api.content.market.yandex.ru\"",
                                " [regexp content_api_haproxy [regexp default [log_headers <::X-Forwarded-For:82.208" +
                                        ".113.179::> <::X-Balancer-IP:2a02:6b8:0:3400::3c5::> " +
                                        "<::User-Agent:Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, " +
                                        "like Gecko) Chrome/74.0.3729.157 Safari/537.36::> " +
                                        "<::X-Yandex-Ja3:771,4865-4866-4867-49195-49199-49196-49200-52393-52392-49171" +
                                        "-49172-156-157-47-53-10,0-23-65281-10-11-35-16-5-13-18-51-45-43-21,29-23-24," +
                                        "0::> " +
                                        "<::X-Yandex-Ja4:1027-1283-1539-2052-2053-2054-2057-2058-2059-1025-1281-1537-" +
                                        "1026-771-769-770-515-513-514,1027-1283-1539-2052-2053-2054-2057-2058-2059-" +
                                        "1025-1281-1537-1026-771-769-770-515-513-514,772-771-770-769,,23,1::> " +
                                        "<::x-antirobot-is-crawler:GoogleBot::> " +
                                        "<::X-Balancer-Port:443::> <::X-Req-Id:1577281015976156-5163996738690262271" +
                                        "::> <::X-Forwarded-For-Y:2a02:6b8:c0f:1310:10c:b3a5:0:1fe4::> " +
                                        "<::X-Market-Req-ID:1577281015976/ffa4e87eac33aa47dc10ab5a879a0500::> " +
                                        "<::X-Real-Ip:2a02:6b8:c0f:1310:10c:b3a5:0:1fe4::> [proxy " +
                                        "fdee:fdee::1:216:9080 0.007691s/0.008139s 0/876 succ 200]]]]\n"
                        })),
                true);
    }

    @Test
    public void convertFullRequest2() throws Exception {
        JSONAssert.assertEquals(
                "{" +
                        "\"attempts\": 1," +
                        "\"client_addr\": \"94.25.233.145\"," +
                        "\"client_port\": 63192," +
                        "\"debug\": \"\"," +
                        "\"elapsed_time\": 2.009," +
                        "\"host\": \"m.market.yandex.ru\"," +
                        "\"is_robot\": \"false\"," +
                        "\"method\": \"GET\"," +
                        "\"protocol\": \"HTTP/1.1\"," +
                        "\"referer\": \"https://yandex.ru/\"," +
                        "\"request_size\": 0," +
                        "\"request_time\": 4.725," +
                        "\"request_timestamp\": 1588414781960," +
                        "\"request_timestamp_iso\": \"2020-05-02T13:19:41.960470+03:00\"," +
                        "\"response_size\": 650," +
                        "\"response_time\": 1.934," +
                        "\"response_timestamp\": 1588414781965," +
                        "\"response_timestamp_iso\": \"2020-05-02T13:19:41.965195+03:00\"," +
                        "\"server_name\": \"[fdee:fdee::2:22]:80\"," +
                        "\"service_name\": \"touch_market\"," +
                        "\"status\": 301, " +
                        "\"status_text\": \"succ 301\"," +
                        "\"timestamp\": 1588414781," +
                        "\"url\": \"/search.xml?cvredirect=2&text=%D1%81%D0%BE%D0%B1%D0%BB%D0%B0%D0%B7%D0%BD%D1%8F%D0" +
                        "%B5%D1%82%20%D0%BF%D0%B0%D1%80%D0%BD%D1%8F&source=tabbar\"," +
                        "\"x-balancer-ip\":\"87.250.250.22\"," +
                        "\"x-balancer-port\":443," +
                        "\"x-forwarded-for\":\"94.25.233.145\"," +
                        "\"x-market-req-id\":\"1588414781960/951bc5c741ab1d4316b1d5a2a7a40500\"," +
                        "\"x-real-ip\": \"94.25.233.145\"" +
                        "}",
                LogParser.convert(
                        String.join("\t", new String[]{
                                "94.25.233.145:63192",
                                "2020-05-02T13:19:41.960470+0300",
                                "\"GET /search.xml?cvredirect=2&text=%D1%81%D0%BE%D0%B1%D0%BB%D0%B0%D0%B7%D0%BD%D1%8F" +
                                        "%D0%B5%D1%82%20%D0%BF%D0%B0%D1%80%D0%BD%D1%8F&source=tabbar HTTP/1.1\"",
                                "0.004725s",
                                "\"https://yandex.ru/\"",
                                "\"m.market.yandex.ru\"",
                                "[regexp touch_market [regexp default [h100 [cutter [antirobot [sub_antirobot [proxy " +
                                        "fdee:fdee::4c:80 0.000895s/0.000905s 2598/187 succ 200] not_robot] " +
                                        "[sub_search [exp_getter [uaas [proxy fdee:fdee:0:3400:0:3c9:0:12d:80 0" +
                                        ".001598s/0.001617s 0/4346 succ 200] uaas_answered] [log_headers " +
                                        "<::X-Balancer-Port:443::> " +
                                        "<::X-Market-Req-ID:1588414781960/951bc5c741ab1d4316b1d5a2a7a40500::> " +
                                        "<::X-Req-Id:1588414781960470-4836209873878588309::> <::X-Balancer-IP:87.250" +
                                        ".250.22::> <::X-Forwarded-For:94.25.233.145::> <::X-Real-Ip:94.25.233.145::>" +
                                        " <::X-Forwarded-For-Y:94.25.233.145::> [proxy fdee:fdee::2:22:80 0.001934s/0" +
                                        ".002009s 0/650 succ 301]]]]]]]]]"
                        })),
                true);
    }

    @Test
    public void convertFullRequest3() throws Exception {
        JSONAssert.assertEquals(
                "{" +
                        "\"attempts\": 4," +
                        "\"client_addr\": \"94.25.233.145\"," +
                        "\"client_port\": 63192," +
                        "\"debug\": \"\"," +
                        "\"elapsed_time\": 1.804," +
                        "\"host\": \"m.market.yandex.ru\"," +
                        "\"is_robot\": \"false\"," +
                        "\"method\": \"GET\"," +
                        "\"protocol\": \"HTTP/1.1\"," +
                        "\"referer\": \"https://yandex.ru/\"," +
                        "\"request_size\": 0," +
                        "\"request_time\": 4.725," +
                        "\"request_timestamp\": 1588414781960," +
                        "\"request_timestamp_iso\": \"2020-05-02T13:19:41.960470+03:00\"," +
                        "\"response_size\": 5473," +
                        "\"response_time\": 1.727," +
                        "\"response_timestamp\": 1588414781965," +
                        "\"response_timestamp_iso\": \"2020-05-02T13:19:41.965195+03:00\"," +
                        "\"server_name\": \"lite12i.search.yandex.net:80\"," +
                        "\"service_name\": \"yandex\"," +
                        "\"status\": 404, " +
                        "\"status_text\": \"succ 404\"," +
                        "\"timestamp\": 1588414781," +
                        "\"url\": \"/search.xml?cvredirect=2&text=%D1%81%D0%BE%D0%B1%D0%BB%D0%B0%D0%B7%D0%BD%D1%8F%D0" +
                        "%B5%D1%82%20%D0%BF%D0%B0%D1%80%D0%BD%D1%8F&source=tabbar\"" +
                        "}",
                LogParser.convert(
                        String.join("\t", new String[]{
                                "94.25.233.145:63192",
                                "2020-05-02T13:19:41.960470+0300",
                                "\"GET /search.xml?cvredirect=2&text=%D1%81%D0%BE%D0%B1%D0%BB%D0%B0%D0%B7%D0%BD%D1%8F" +
                                        "%D0%B5%D1%82%20%D0%BF%D0%B0%D1%80%D0%BD%D1%8F&source=tabbar HTTP/1.1\"",
                                "0.004725s",
                                "\"https://yandex.ru/\"",
                                "\"m.market.yandex.ru\"",
                                "[log_headers <::X-Req-Id:1534324664018377-2159731762284062726::> [regexp yandex " +
                                        "[h100 [regexp_path morda [h100 [cutter [antirobot [sub_antirobot [proxy " +
                                        "man1-0510.search.yandex.net:13512 0.000500s/0.000514s 0/166 succ 200] " +
                                        "not_robot] [sub_search [threshold [geobase [geo [proxy man1-0234.search" +
                                        ".yandex.net:35253 0.000821s/0.000845s 0/519 succ 200] laas_answered] " +
                                        "[exp_getter [uaas [proxy 127.0.0.1:15481 0.000693s/0.000704s 0/580 succ 200]" +
                                        " uaas_answered] [proxy vla2-0379-94f-vla-portal-morda-pre-fb0-9235.gencfg-c" +
                                        ".yandex.net:9235 0.000000s/0.150252s system_error ETIMEDOUT] [proxy sas" +
                                        ".wfront.yandex.net:80 0.000000s/0.150998s system_error ETIMEDOUT] [proxy iva" +
                                        ".wfront.yandex.net:80 0.000000s/0.150101s system_error ETIMEDOUT] [on_error " +
                                        "[proxy lite12i.search.yandex.net:80 0.001727s/0.001804s 0/5473 succ " +
                                        "404]]]]]]]]]]]]]"
                        })),
                true);
    }

    @Test
    public void convertFullRequest4() throws Exception {
        JSONAssert.assertEquals(
                "{" +
                        "\"attempts\": 2," +
                        "\"client_addr\": \"95.153.134.97\"," +
                        "\"client_port\": 36691," +
                        "\"debug\": \"\"," +
                        "\"elapsed_time\": 1075.685," +
                        "\"host\": \"m.market.yandex.ru\"," +
                        "\"is_robot\": \"false\"," +
                        "\"method\": \"GET\"," +
                        "\"protocol\": \"HTTP/1.1\"," +
                        "\"referer\": \"https://m.market.yandex" +
                        ".ru/search?rs" +
                        "=eJwzSvKS4xLLK0rJy0rN9M6qtIxI9rM0MA31KjEtl9BXYNBgAMlXROQGlOcZhDplZRqZGGQnV6QbeVUFSmjA5EMN_LNSvSyiDJzDK4PTfF08I33dDQMlokHyEQwAec8aaA%2C%2C&text=%D0%BB%D0%B5%D1%82%D1%83%D0%B0%D0%BB%D1%8C%20%D0%B4%D1%83%D1%85%D0%B8%20%D0%BB%D0%B0%D0%BD%D0%BA%D0%BE%D0%BC&lr=101414&clid=708\"," +
                        "\"request_size\": 0," +
                        "\"request_time\": 1482.097," +
                        "\"request_timestamp\": 1590504539956," +
                        "\"request_timestamp_iso\": \"2020-05-26T17:48:59.956850+03:00\"," +
                        "\"response_size\": 46275," +
                        "\"response_time\": 1070.584," +
                        "\"response_timestamp\": 1590504541438," +
                        "\"response_timestamp_iso\": \"2020-05-26T17:49:01.438947+03:00\"," +
                        "\"server_name\": \"[fdee:fdee::2:22]:9080\"," +
                        "\"service_name\": \"touch_market\"," +
                        "\"status\": 200, " +
                        "\"status_text\": \"succ 200\"," +
                        "\"timestamp\": 1590504541," +
                        "\"url\": \"/api/search/result?rs" +
                        "=eJwzSvKS4xLLK0rJy0rN9M6qtIxI9rM0MA31KjEtl9BXYNBgAMlXROQGlOcZhDplZRqZGGQnV6QbeVUFSmjA5EMN_LNSvSyiDJzDK4PTfF08I33dDQMlokHyEQwAec8aaA%2C%2C&text=%D0%BB%D0%B5%D1%82%D1%83%D0%B0%D0%BB%D1%8C%20%D0%B4%D1%83%D1%85%D0%B8%20%D0%BB%D0%B0%D0%BD%D0%BA%D0%BE%D0%BC&lr=101414&clid=708&local-offers-first=0&onstock=0&numdoc=16&isMultisearch=false&omit-render=1&page=2&viewMode=list&show-urls=external&show-urls=cpa&show-urls=offercard&show-urls=callPhone&show-urls=productVendorBid&show-urls=encryptedmodel&show-urls=chat&show-urls=encryptedTurbo&show-urls=directTurbo&show-urls=beruOrder&pickup-options=grouped\"," +
                        "\"x-balancer-ip\":\"87.250.250.22\"," +
                        "\"x-balancer-port\":443," +
                        "\"x-forwarded-for\":\"95.153.134.97\"," +
                        "\"x-market-req-id\":\"1590504539956/20c3a41a46f143cb72261e328ea60500\"," +
                        "\"x-real-ip\": \"95.153.134.97\"" +
                        "}",
                LogParser.convert(
                        String.join("\t", new String[]{
                                "95.153.134.97:36691",
                                "2020-05-26T17:48:59.956850+0300",
                                "\"GET /api/search/result?rs" +
                                        "=eJwzSvKS4xLLK0rJy0rN9M6qtIxI9rM0MA31KjEtl9BXYNBgAMlXROQGlOcZhDplZRqZGGQnV6QbeVUFSmjA5EMN_LNSvSyiDJzDK4PTfF08I33dDQMlokHyEQwAec8aaA%2C%2C&text=%D0%BB%D0%B5%D1%82%D1%83%D0%B0%D0%BB%D1%8C%20%D0%B4%D1%83%D1%85%D0%B8%20%D0%BB%D0%B0%D0%BD%D0%BA%D0%BE%D0%BC&lr=101414&clid=708&local-offers-first=0&onstock=0&numdoc=16&isMultisearch=false&omit-render=1&page=2&viewMode=list&show-urls=external&show-urls=cpa&show-urls=offercard&show-urls=callPhone&show-urls=productVendorBid&show-urls=encryptedmodel&show-urls=chat&show-urls=encryptedTurbo&show-urls=directTurbo&show-urls=beruOrder&pickup-options=grouped HTTP/1.1\"",
                                "1.482097s",
                                "\"https://m.market.yandex" +
                                        ".ru/search?rs" +
                                        "=eJwzSvKS4xLLK0rJy0rN9M6qtIxI9rM0MA31KjEtl9BXYNBgAMlXROQGlOcZhDplZRqZGGQnV6QbeVUFSmjA5EMN_LNSvSyiDJzDK4PTfF08I33dDQMlokHyEQwAec8aaA%2C%2C&text=%D0%BB%D0%B5%D1%82%D1%83%D0%B0%D0%BB%D1%8C%20%D0%B4%D1%83%D1%85%D0%B8%20%D0%BB%D0%B0%D0%BD%D0%BA%D0%BE%D0%BC&lr=101414&clid=708\"",
                                "\"m.market.yandex.ru\"",
                                "[regexp touch_market [regexp default [report u:touch_market [h100 [cutter [antirobot" +
                                        " [sub_antirobot [proxy fdee:fdee::4c:80 0.000814s/0.000836s 4076/187 succ " +
                                        "200] not_robot] [sub_search [exp_getter [uaas [report " +
                                        "u:touch_market_exp_getter_uaas [proxy fdee:fdee:0:3400:0:3c9:0:12d:80 0" +
                                        ".000759s/0.000772s 0/4071 succ 200]] uaas_answered] [log_headers " +
                                        "<::X-Balancer-Port:443::> " +
                                        "<::X-Market-Req-ID:1590504539956/20c3a41a46f143cb72261e328ea60500::> " +
                                        "<::X-Req-Id:1590504539956850-14646815696535995168::> <::X-Balancer-IP:87.250" +
                                        ".250.22::> <::X-Forwarded-For:95.153.134.97::> <::X-Real-Ip:95.153.134.97::>" +
                                        " <::X-Forwarded-For-Y:95.153.134.97::> [proxy fdee:fdee::2:22:9080 0" +
                                        ".404555s/0.404565s 0/233 backend http_error 503] [proxy fdee:fdee::2:22:9080" +
                                        " 1.070584s/1.075685s 0/46275 succ 200]]]]]]]]]]"
                        })),
                true);
    }

    @Test
    public void convertFullRequest5RpsLimiter() throws Exception {
        JSONAssert.assertEquals(
            "{" +
                    "\"attempts\": 1," +
                    "\"client_addr\": \"5.255.253.41\"," +
                    "\"client_port\": 42898," +
                    "\"debug\": \"\"," +
                    "\"elapsed_time\": 677.625," +
                    "\"host\": \"market.yandex.ru\"," +
                    "\"is_robot\": \"false\"," +
                    "\"method\": \"GET\"," +
                    "\"protocol\": \"HTTP/1.1\"," +
                    "\"referer\": \"https://pokupki.market.yandex.ru/product/101332993970\"," +
                    "\"request_size\": 0," +
                    "\"request_time\": 680.215," +
                    "\"request_timestamp\": 1638160983869," +
                    "\"request_timestamp_iso\": \"2021-11-29T07:43:03.869005+03:00\"," +
                    "\"response_size\": 3275," +
                    "\"response_time\": 677.142," +
                    "\"response_timestamp\": 1638160984549," +
                    "\"response_timestamp_iso\": \"2021-11-29T07:43:04.549220+03:00\"," +
                    "\"server_name\": \"[fdee:fdee::2:22]:87\"," +
                    "\"service_name\": \"market_desktop\"," +
                    "\"status\": 200, " +
                    "\"status_text\": \"87344 succ 200\"," +
                    "\"timestamp\": 1638160984," +
                    "\"url\": \"/product--zerkalo-planta-plm-0102-nastolnoe-kosmeticheskoe-s-sensornoi-regulirovkoi-iarkosti-podsvetki-zariadka-usb/968816233?pokupki=1&cpa=1&sku=101332993970\"," +
                    "\"x-balancer-ip\":\"87.250.250.22\"," +
                    "\"x-balancer-port\":443," +
                    "\"x-forwarded-for\":\"5.255.253.41\"," +
                    "\"x-market-req-id\":\"1638160983869/50d5aaba93976b2c4d468713e6d10500\"," +
                    "\"x-real-ip\": \"5.255.253.41\"," +
                    "\"user_agent\": \"python-requests/2.25.1\"," +
                    "\"x-antirobot-is-crawler\": \"YandexBot\"," +
                    "\"x-yandex-ja3\": \"771,49195-49199-49196-49200-52393-52392-49161-49171-49162-49172-156-157-47-53-49170-10-4865-4866-4867,0-5-10-11-13-65281-18-43-51,29-23-24-25,0\"," +
                    "\"x-yandex-ja4\": \"2052-1027-2055-2053-2054-1025-1281-1537-1283-1539-513-515,,772-771-770-769,,29,\"" +
                    "}",
            LogParser.convert(
                String.join("\t", new String[]{
                    "5.255.253.41:42898\t2021-11-29T07:43:03.869005+0300\t\"GET " +
                    "/product--zerkalo-planta-plm-0102-nastolnoe-kosmeticheskoe-s-sensornoi" +
                    "-regulirovkoi-iarkosti-podsvetki-zariadka-usb/968816233?pokupki=1&cpa=1&sku" +
                    "=101332993970 HTTP/1.1\"\t0.680215s\t\"https://pokupki.market.yandex" +
                    ".ru/product/101332993970\"\t\"market.yandex.ru\"\t [regexp market_desktop " +
                    "[regexp default [report u:market_desktop [h100 [cutter [antirobot [sub_antirobot" +
                    " [proxy fdee:fdee::4c:80 0.001120s/0.001128s/connect=0.000000s 2041/406 0 succ " +
                    "200] not_robot] [sub_search [exp_getter [uaas [report " +
                    "u:market_desktop_exp_getter_uaas [proxy fdee:fdee:0:3400:0:3c9:0:12d:80 0" +
                    ".000684s/0.000693s/connect=0.000000s 0/1810 0 succ 200]] uaas_answered] [regexp " +
                    "external_client [log_headers <::x-yandex-antirobot-degradation:0::> " +
                    "<::x-yandex-internal-request:0::> <::X-Balancer-Port:443::> " +
                    "<::x-antirobot-is-crawler:YandexBot::> " +
                    "<::X-Market-Req-ID:1638160983869/50d5aaba93976b2c4d468713e6d10500::> " +
                    "<::X-Req-Id:1638160983869005-3200818620924613968::> <::X-Balancer-IP:87.250.250" +
                    ".22::> <::X-Yandex-Ja4:2052-1027-2055-2053-2054-1025-1281-1537-1283-1539-513" +
                    "-515,,772-771-770-769,,29,::> <::User-Agent:python-requests/2.25.1::> " +
                    "<::X-Yandex-Ja3:771,49195-49199-49196-49200-52393-52392-49161-49171-49162-49172" +
                    "-156-157-47-53-49170-10-4865-4866-4867,0-5-10-11-13-65281-18-43-51,29-23-24-25," +
                    "0::> <::X-Forwarded-For:5.255.253.41::> <::X-Real-Ip:5.255.253.41::> " +
                    "<::X-Forwarded-For-Y:5.255.253.41::> [rps_limiter [proxy " +
                    "market-rpslimiter-balancer-2.sas.yp-c.yandex.net:80 0.000490s/0" +
                    ".000511s/connect=0.000233s 0/112 0 succ 200] quota:front-front allow [regexp " +
                    "alternative_backends_antirobot_crawler [proxy fdee:fdee::2:22:87 0.677142s/0" +
                    ".677625s/connect=0.000000s 0/3275 87344 succ 200]]]]]]]]]]]]]\n"
                })),
            true
        );
    }
}
