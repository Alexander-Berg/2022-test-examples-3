package ru.yandex.market.logshatter.parser.mds;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MDSAccessLogParserTest {
    @Test
    @SuppressWarnings("MethodName")
    public void testParse_realGet() throws Exception {
        LogParserChecker checker = new LogParserChecker(new MDSAccessLogParser());

        checker.check(
            "tskv\t" +
                "tskv_format=mds-access-log\t" +
                "timestamp=2021-06-01T20:35:24\t" +
                "timezone=+0300\t" +
                "status=302\t" +
                "protocol=HTTP/1.1\t" +
                "method=GET\t" +
                "request=/get-video-disk/150865/b969c8c0-3fdb-4ab3-a564-7254ef1ed9ae?sign" +
                "=21ea02fcf33ce8ae0822c075e570a020bf245cfa3539e934dd0028096a85650f&ts=60b6a81c&redirect=yes" +
                "&expiration-time=600\t" +
                "referer=-\t" +
                "cookies=-\t" +
                "user_agent=man5-69f3f6fe58ea/videostreaming/100.2647.2\t" +
                "vhost=storage.mds.yandex.net\t" +
                "ip=2a02:6b8:c0a:1103:0:564:69f3:f6fe\t" +
                "x_forwarded_for=-\t" +
                "x_real_ip=-\t" +
                "request_id=6e925df288d4a6ee\t" +
                "args=sign=21ea02fcf33ce8ae0822cfdce4f5a020bf245cfad4d9e934dd0028096a85650f&ts=60b6a81c&redirect=yes" +
                "&expiration-time=600\t" +
                "namespace=video-disk\t" +
                "scheme=http\t" +
                "port=80\t" +
                "ssl_handshake_time=-\t" +
                "ssl_protocol=-\t" +
                "upstream_cache_status=-\t" +
                "upstream_addr=unix:/var/run/mediastorage/mediastorage-proxy.sock\t" +
                "upstream_status=302\t" +
                "http_y_service=-\t" +
                "tcpinfo_rtt=8766\t" +
                "tcpinfo_rttvar=13735\t" +
                "tcpinfo_snd_cwnd=154\t" +
                "tcpinfo_rcv_space=13900\t" +
                "tcpinfo_lost=0\t" +
                "tcpinfo_retrans=0\t" +
                "tcpinfo_retransmits=0\t" +
                "tcpinfo_total_retrans=0\t" +
                "origin=-\t" +
                "x_yandex_yarl_limit=-\t" +
                "range=-\t" +
                "bytes_received=543\t" +
                "bytes_sent=776\t" +
                "content_length=-\t" +
                "upstream_content_length=0\t" +
                "upstream_response_time=0.060\t" +
                "request_time=0.060\t" +
                "request_completion=OK\n",
            new Date(1622568924000L),
            "video-disk",                                   // 00: namespace
            "6e925df288d4a6ee",                             // 01: request_id
            "hostname.test",                                // 02: host
            "2a02:6b8:c0a:1103:0:564:69f3:f6fe",            // 03: ipv6
            "2a02:6b8:c0a:1103:0:564:69f3:f6fe",            // 04: client_ipv6
            Collections.emptyList(),                        // 05: x_forwarded_for
            "",                                             // 06: x_real_ip
            "man5-69f3f6fe58ea/videostreaming/100.2647.2",  // 07: user_agent
            "",                                             // 08: referer
            "HTTP/1.1",                                     // 09: protocol
            "http",                                         // 10: scheme
            "GET",                                          // 11: method
            "storage.mds.yandex.net",                       // 12: vhost
            80,                                             // 13: port
            HandlerType.GET,                                // 14: handler_type
            "/get-video-disk/150865/b969c8c0-3fdb-4ab3-a564-7254ef1ed9ae", // 15: request
            "",                                             // 16: origin
            0,                                              // 17: ssl_handshake_time
            "",                                             // 18: ssl_protocol
            60,                                             // 19: request_time
            "",                                             // 20: range
            0L,                                             // 21: content_length
            543L,                                           // 22: bytes_received
            Collections.emptyList(),                        // 23: cookies
            302,                                            // 24: status
            776L,                                           // 25: bytes_sent
            8766,                                           // 26: tcpinfo_rtt
            13735,                                          // 27: tcpinfo_rttvar
            154,                                            // 28: tcpinfo_snd_cwnd
            13900,                                          // 29: tcpinfo_rcv_space
            0L,                                             // 30: tcpinfo_lost
            0L,                                             // 31: tcpinfo_retrans
            0L,                                             // 32: tcpinfo_retransmits
            0L,                                             // 33: tcpinfo_total_retrans
            "unix:/var/run/mediastorage/mediastorage-proxy.sock", // 34: upstream_addr
            302,                                            // 35: upstream_status
            0L,                                             // 36: upstream_content_length
            CacheStatus.UNKNOWN,                            // 37: upstream_cache_status
            60,                                             // 38: upstream_response_time
            "",                                             // 39: http_y_service
            Collections.emptyList(),                        // 40: x_yandex_yarl_limit
            0L,                                             // 41: couple_id
            Arrays.asList(
                "redirect",
                "expiration-time",
                "sign",
                "ts"
            ),                                              // 42: kv_keys
            Arrays.asList(
                "yes",
                "600",
                "21ea******650f",
                "60b6a81c"
            ),                                              // 43: kv_values
            "OK"                                            // 44: request_completion
        );
    }

    @Test
    @SuppressWarnings("MethodName")
    public void testParse_realPost() throws Exception {
        LogParserChecker checker = new LogParserChecker(new MDSAccessLogParser());

        checker.check(
            "t          skv\t" +
                "tskv_format=mds-int-access-log\t" +
                "timestamp=2021-06-01T20:05:33\t" +
                "timezone=+0300\t" +
                "status=200\t" +
                "protocol=HTTP/1.1\t" +
                "method=POST\t" +
                "request=/upload-ps-cache/?expire=172800s\t" +
                "referer=/notify?mdb=pg&pgshard=2673&operation-id=467608067&operation-date=1622567133" +
                ".151449&uid=953540578&change-type=update&changed-size=1&batch-size=1&salo-worker=pg2673:5&transfer" +
                "-timestamp=1622567133216&zoo-queue-id=18704134&deadline=1622567733334&slow&task-seq=13420&deadline" +
                "=1622567799360\t" +
                "cookies=-\t" +
                "user_agent=-\t" +
                "vhost=storage-int.mds.yandex.net:1111\t" +
                "ip=2a02:6b8:c0a:39a3:0:4c98:2ecc:0\t" +
                "x_forwarded_for=-\t" +
                "x_real_ip=-\t" +
                "request_id=202e68019d3fd5fe\t" +
                "args=expire=172800s\t" +
                "namespace=ps-cache\t" +
                "scheme=http\t" +
                "port=1111\t" +
                "ssl_handshake_time=-\t" +
                "ssl_protocol=-\t" +
                "upstream_cache_status=-\t" +
                "upstream_addr=unix:/var/run/mediastorage/mediastorage-proxy.sock\t" +
                "upstream_status=200\t" +
                "http_y_service=-\t" +
                "tcpinfo_rtt=15317\t" +
                "tcpinfo_rttvar=11491\t" +
                "tcpinfo_snd_cwnd=144\t" +
                "tcpinfo_rcv_space=322167\t" +
                "tcpinfo_lost=0\t" +
                "tcpinfo_retrans=0\t" +
                "tcpinfo_retransmits=0\t" +
                "tcpinfo_total_retrans=0\t" +
                "origin=-\t" +
                "x_yandex_yarl_limit=-\t" +
                "range=-\t" +
                "bytes_received=45302\t" +
                "bytes_sent=821\t" +
                "content_length=44395\t" +
                "upstream_content_length=599\t" +
                "upstream_response_time=0.020\t" +
                "request_time=0.017\t" +
                "couple_id=3804278\t" +
                "request_completion=OK\n",
            new Date(1622567133000L),
            "ps-cache",                             // 00: namespace
            "202e68019d3fd5fe",                     // 01: request_id
            "hostname.test",                        // 02: host
            "2a02:6b8:c0a:39a3:0:4c98:2ecc:0",      // 03: ipv6
            "2a02:6b8:c0a:39a3:0:4c98:2ecc:0",      // 04: client_ipv6
            Collections.emptyList(),                // 05: x_forwarded_for
            "",                                     // 06: x_real_ip
            "",                                     // 07: user_agent
            "/notify?mdb=pg&pgshard=2673&operation-id=467608067&operation-date=1622567133" +
                ".151449&uid=953540578&change-type=update&changed-size=1&batch-size=1&salo-worker=pg2673:5&transfer" +
                "-timestamp=1622567133216&zoo-queue-id=18704134&deadline=1622567733334&slow&task-seq=13420&deadline" +
                "=1622567799360", // 08: referer
            "HTTP/1.1",                             // 09: protocol
            "http",                                 // 10: scheme
            "POST",                                 // 11: method
            "storage-int.mds.yandex.net:1111",      // 12: vhost
            1111,                                   // 13: port
            HandlerType.PUT,                        // 14: handler_type
            "/upload-ps-cache",                     // 15: request
            "",                                     // 16: origin
            0,                                      // 17: ssl_handshake_time
            "",                                     // 18: ssl_protocol
            17,                                     // 19: request_time
            "",                                     // 20: range
            44395L,                                 // 21: content_length
            45302L,                                 // 22: bytes_received
            Collections.emptyList(),                // 23: cookies
            200,                                    // 24: status
            821L,                                   // 25: bytes_sent
            15317,                                  // 26: tcpinfo_rtt
            11491,                                  // 27: tcpinfo_rttvar
            144,                                    // 28: tcpinfo_snd_cwnd
            322167,                                 // 29: tcpinfo_rcv_space
            0L,                                     // 30: tcpinfo_lost
            0L,                                     // 31: tcpinfo_retrans
            0L,                                     // 32: tcpinfo_retransmits
            0L,                                     // 33: tcpinfo_total_retrans
            "unix:/var/run/mediastorage/mediastorage-proxy.sock", // 34: upstream_addr
            200,                                    // 35: upstream_status
            599L,                                   // 36: upstream_content_length
            CacheStatus.UNKNOWN,                    // 37: upstream_cache_status
            20,                                     // 38: upstream_response_time
            "",                                     // 39: http_y_service
            Collections.emptyList(),                // 40: x_yandex_yarl_limit
            3804278L,                               // 41: couple_id
            Collections.singletonList("expire"),    // 42: kv_keys
            Collections.singletonList("172800s"),   // 43: kv_values
            "OK"                                    // 44: request_completion
        );
    }

    @Test
    @SuppressWarnings("MethodName")
    public void testParse_allFilled() throws Exception {
        LogParserChecker checker = new LogParserChecker(new MDSAccessLogParser());

        checker.check(
            "tskv\t" +
                "tskv_format=mds-access-log\t" +
                "timestamp=2021-04-20T12:26:19\t" +
                "timezone=+0300\t" +
                "status=200\t" +
                "protocol=HTTP/2.0\t" +
                "method=GET\t" +
                "request=/get-entity_search/68218/106499026\t" +
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
                "ssl_protocol=SSL_PROTO_NAME\t" +
                "upstream_cache_status=MISS\t" +
                "upstream_addr=1.1.1.1:80\t" +
                "upstream_status=200\t" +
                "range=11-22\t" +
                "http_y_service=y-service-name\t" +
                "tcpinfo_rtt=111722\t" +
                "tcpinfo_rttvar=15982\t" +
                "tcpinfo_snd_cwnd=10\t" +
                "tcpinfo_rcv_space=14100\t" +
                "tcpinfo_lost=4000000000\t" +
                "tcpinfo_retrans=4000000001\t" +
                "tcpinfo_retransmits=4000000002\t" +
                "tcpinfo_total_retrans=4000000003\t" +
                "origin=some-origin-value\t" +
                "x_yandex_yarl_limit=some-quota-name\t" +
                "bytes_received=52\t" +
                "bytes_sent=19397\t" +
                "content_length=111222\t" +
                "upstream_content_length=3000000000\t" +
                "upstream_response_time=0.222\t" +
                "couple_id=1234\t" +
                "request_time=0.333\t" +
                "request_completion=OK\n",
            new Date(1618910779000L),
            "entity_search",                                    // 00: namespace
            "a3bdfc88976055d9",                                 // 01: request_id
            "hostname.test",                                    // 02: host
            "94.100.181.200",                            // 03: ipv6
            "188.170.81.252",                            // 04: client_ipv6
            Collections.singletonList("188.170.81.252"), // 05: x_forwarded_for
            "188.170.81.252",                            // 06: x_real_ip
            "UserAgent value",                                  // 07: user_agent
            "https://referer/address",                          // 08: referer
            "HTTP/2.0",                                         // 09: protocol
            "https",                                            // 10: scheme
            "GET",                                              // 11: method
            "avatars.mds.yandex.net",                           // 12: vhost
            443,                                                // 13: port
            HandlerType.GET,                                    // 14: handler_type
            "/get-entity_search/68218/106499026",               // 15: request
            "some-origin-value",                                // 16: origin
            111,                                                // 17: ssl_handshake_time
            "SSL_PROTO_NAME",                                   // 18: ssl_protocol
            333,                                                // 19: request_time
            "11-22",                                            // 20: range
            111_222L,                                           // 21: content_length
            52L,                                                // 22: bytes_received
            Arrays.asList("cookie1=123", "cookie2=321"),        // 23: cookies
            200,                                                // 24: status
            19397L,                                             // 25: bytes_sent
            111722,                                             // 26: tcpinfo_rtt
            15982,                                              // 27: tcpinfo_rttvar
            10,                                                 // 28: tcpinfo_snd_cwnd
            14100,                                              // 29: tcpinfo_rcv_space
            4000000000L,                                        // 30: tcpinfo_lost
            4000000001L,                                        // 31: tcpinfo_retrans
            4000000002L,                                        // 32: tcpinfo_retransmits
            4000000003L,                                        // 33: tcpinfo_total_retrans
            "1.1.1.1:80",                                       // 34: upstream_addr
            200,                                                // 35: upstream_status
            3000000000L,                                        // 36: upstream_content_length
            CacheStatus.MISS,                                   // 37: upstream_cache_status
            222,                                                // 38: upstream_response_time
            "y-service-name",                                   // 39: http_y_service
            Collections.singletonList("some-quota-name"),       // 40: x_yandex_yarl_limit
            1234L,                                              // 41: couple_id
            Collections.singletonList("webp"),                  // 42: kv_keys
            Collections.singletonList("true"),                  // 43: kv_values
            "OK"                                                // 44: request_completion
        );
    }

    @Test
    @SuppressWarnings("MethodName")
    public void testParse_allEmpty() throws Exception {
        LogParserChecker checker = new LogParserChecker(new MDSAccessLogParser());

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
                "range=-\t" +
                "http_y_service=-\t" +
                "tcpinfo_rtt=-\t" +
                "tcpinfo_rttvar=-\t" +
                "tcpinfo_snd_cwnd=-\t" +
                "tcpinfo_rcv_space=-\t" +
                "tcpinfo_lost=-\t" +
                "tcpinfo_retrans=-\t" +
                "tcpinfo_retransmits=-\t" +
                "tcpinfo_total_retrans=-\t" +
                "origin=-\t" +
                "x_yandex_yarl_limit=-\t" +
                "bytes_received=-\t" +
                "bytes_sent=-\t" +
                "content_length=-\t" +
                "upstream_content_length=-\t" +
                "upstream_response_time=-\t" +
                "couple_id=-\t" +
                "request_time=-\t" +
                "request_completion=\n",
            new Date(1619011514000L),
            "unknown",                      // 00: namespace
            "",                             // 01: request_id
            "hostname.test",                // 02: host
            "",                             // 03: ipv6
            "",                             // 04: client_ipv6
            Collections.emptyList(),        // 05: x_forwarded_for
            "",                             // 06: x_real_ip
            "",                             // 07: user_agent
            "",                             // 08: referer
            "",                             // 09: protocol
            "",                             // 10: scheme
            "",                             // 11: method
            "",                             // 12: vhost
            0,                              // 13: port
            HandlerType.GET,                // 14: handler_type
            "/get-asdf",                    // 15: request
            "",                             // 16: origin
            0,                              // 17: ssl_handshake_time
            "",                             // 18: ssl_protocol
            0,                              // 19: request_time
            "",                             // 20: range
            0L,                             // 21: content_length
            0L,                             // 22: bytes_received
            Collections.emptyList(),        // 23: cookies
            0,                              // 24: status
            0L,                             // 25: bytes_sent
            0,                              // 26: tcpinfo_rtt
            0,                              // 27: tcpinfo_rttvar
            0,                              // 28: tcpinfo_snd_cwnd
            0,                              // 29: tcpinfo_rcv_space
            0L,                             // 30: tcpinfo_lost
            0L,                             // 31: tcpinfo_retrans
            0L,                             // 32: tcpinfo_retransmits
            0L,                             // 33: tcpinfo_total_retrans
            "",                             // 34: upstream_addr
            0,                              // 35: upstream_status
            0L,                             // 36: upstream_content_length
            CacheStatus.UNKNOWN,            // 37: upstream_cache_status
            0,                              // 38: upstream_response_time
            "",                             // 39: http_y_service
            Collections.emptyList(),        // 40: x_yandex_yarl_limit
            0L,                             // 41: couple_id
            Collections.emptyList(),        // 42: kv_keys
            Collections.emptyList(),        // 43: kv_values
            ""                              // 44: request_completion
        );
    }

    @Test
    @SuppressWarnings("MethodName")
    public void testParse_filtered() throws Exception {
        LogParserChecker checker = new LogParserChecker(new MDSAccessLogParser());

        // disallowed request
        checker.checkEmpty(
            "tskv\ttskv_format=mds-access-log\ttimestamp=2021-04-20T12:26:31\ttimezone=+0300\tstatus=200\tprotocol" +
                "=HTTP/1.1\tmethod=GET\trequest=/ping\treferer=-\tcookies=-\tuser_agent=curl/7.35" +
                ".0\tvhost=localhost\tip=::1\tx_forwarded_for=-\tx_real_ip=-\trequest_id=8515a3ab625ea4a8\targs" +
                "=-\tscheme=http\tport=80\tssl_handshake_time=-\tupstream_cache_status=-\tupstream_addr=unix:/var/run" +
                "/avatars-mds/avatars-mds.sock\tupstream_status=200\thttp_y_service=-\ttcpinfo_rtt=26\ttcpinfo_rttvar" +
                "=11\ttcpinfo_snd_cwnd=11\ttcpinfo_rcv_space=43690\tx_yandex_yarl_limit=-\tbytes_received=77" +
                "\tbytes_sent=415\tcontent_length=-\tupstream_response_time=0.000\trequest_time=0" +
                ".000\trequest_completion=OK"
        );
    }

    @Test
    public void testIsAllowedRequest() {
        assertFalse(MDSAccessLogParser.isAllowedRequest(""));
        assertFalse(MDSAccessLogParser.isAllowedRequest("/ping"));
        assertFalse(MDSAccessLogParser.isAllowedRequest("/unistat"));
        assertFalse(MDSAccessLogParser.isAllowedRequest("/favicon.ico"));
        assertFalse(MDSAccessLogParser.isAllowedRequest("/robots.txt"));

        assertTrue(MDSAccessLogParser.isAllowedRequest("/"));
        assertTrue(MDSAccessLogParser.isAllowedRequest("/timetail"));
        assertTrue(MDSAccessLogParser.isAllowedRequest("/abrakadabra!"));
        assertTrue(MDSAccessLogParser.isAllowedRequest("/i"));
        assertTrue(MDSAccessLogParser.isAllowedRequest("/get-znatoki-cover/1220555" +
            "/2a0000017816dc0ec2c986fbf2f2f9bc0c75/720x240"));
        assertTrue(MDSAccessLogParser.isAllowedRequest("/delete-vthumb/904110/38842623d9543f487146b7058a6a6d0a"));
        assertTrue(MDSAccessLogParser.isAllowedRequest("/genurlsign-zen-personal/4303171/secure_3b56a5ee-75d3-4368" +
            "-a586-3031749aeb71/optimize?dynamic-watermark=1618974573623-6907689621618933296-607f976d2862bf186e187ee2" +
            "&expire-time=1800"));
        assertTrue(MDSAccessLogParser.isAllowedRequest("/getimageinfo-images-cbir/4488757/2lPmSlC9X7QXUSmeD5OwjA9621" +
            "?"));
        assertTrue(MDSAccessLogParser.isAllowedRequest("/getinfo-direct/223179/tIXvAVQLI-vbDj95J6bwpg/meta"));
        assertTrue(MDSAccessLogParser.isAllowedRequest("/put-main-images/?expire=336h"));
        assertTrue(MDSAccessLogParser.isAllowedRequest("/statistics-kino-vod-persons-gallery"));
    }
}
