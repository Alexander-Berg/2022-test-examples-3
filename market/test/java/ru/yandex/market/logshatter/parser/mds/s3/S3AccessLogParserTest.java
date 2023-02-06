package ru.yandex.market.logshatter.parser.mds.s3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;
import ru.yandex.market.logshatter.parser.mds.CacheStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class S3AccessLogParserTest {
    private final List<String> keys = new ArrayList<>();
    private final List<String> vals = new ArrayList<>();

    @AfterEach
    public void tearDown() {
        keys.clear();
        vals.clear();
    }

    @Test
    @SuppressWarnings("MethodName")
    public void testParseBucketTags_emptyTags() {
        Pair<List<String>, List<String>> result = S3AccessLogParser.parseBucketTags("{}");

        assertEquals(keys, result.getLeft(), "List of keys is not empty for empty bucket tags");
        assertEquals(vals, result.getRight(), "List of values is not empty for empty bucket tags");
    }

    @Test
    @SuppressWarnings("MethodName")
    public void testParseBucketTags_emptyValue() {
        keys.add("empty_tag");
        vals.add("");

        Pair<List<String>, List<String>> result = S3AccessLogParser.parseBucketTags("{\"empty_tag\": \"\"}");

        assertEquals(keys, result.getLeft(), "Wrong bucket tags parsing result: single key is expected");
        assertEquals(vals, result.getRight(), "Wrong bucket tags parsing result: single empty value is expected");
    }

    @Test
    @SuppressWarnings("MethodName")
    public void testParseBucketTags_brokenJson() {
        keys.add("unknown_tag");
        vals.add("broken json value, ");

        Pair<List<String>, List<String>> result = S3AccessLogParser.parseBucketTags("broken json value, ");

        assertEquals(keys, result.getLeft(), "Unexpected result for broken JSON syntax value");
        assertEquals(vals, result.getRight(), "Unexpected result for broken JSON syntax value");
    }

    @Test
    @SuppressWarnings("MethodName")
    public void testParseBucketTags_string() {
        keys.add("str_tag");
        vals.add("str_value");

        Pair<List<String>, List<String>> result = S3AccessLogParser.parseBucketTags("{\"str_tag\": \"str_value\"}");

        assertEquals(keys, result.getLeft(), "Wrong bucket tags parsing result: single key is expected");
        assertEquals(vals, result.getRight(), "Wrong bucket tags parsing result: single string value is expected");
    }

    @Test
    @SuppressWarnings("MethodName")
    public void testParseBucketTags_numeric1() {
        keys.add("num_tag1");
        vals.add("1");

        Pair<List<String>, List<String>> result = S3AccessLogParser.parseBucketTags("{\"num_tag1\": \"1\"}");

        assertEquals(keys, result.getLeft(), "Wrong bucket tags parsing result: single key is expected");
        assertEquals(vals, result.getRight(), "Wrong bucket tags parsing result: single string value is expected");
    }

    @Test
    @SuppressWarnings("MethodName")
    public void testParseBucketTags_numeric2() {
        keys.add("num_tag10");
        vals.add("10");

        Pair<List<String>, List<String>> result = S3AccessLogParser.parseBucketTags("{\"num_tag10\": 10}");

        assertEquals(keys, result.getLeft(), "Wrong bucket tags parsing result: single key is expected");
        assertEquals(vals, result.getRight(), "Wrong bucket tags parsing result: single string value is expected");
    }

    @Test
    @SuppressWarnings("MethodName")
    public void testParseBucketTags_severalTags() {
        keys.add("tag1");
        vals.add("val1");

        keys.add("tag2");
        vals.add("val2");

        Pair<List<String>, List<String>> result = S3AccessLogParser.parseBucketTags("{\"tag1\": \"val1\", " +
            "\"tag2\":\"val2\"}");

        assertEquals(keys, result.getLeft(), "Wrong bucket tags parsing result: two keys are expected");
        assertEquals(vals, result.getRight(), "Wrong bucket tags parsing result: two values are expected");
    }

    @Test
    @SuppressWarnings("MethodName")
    public void testParse_real() throws Exception {
        LogParserChecker checker = new LogParserChecker(new S3AccessLogParser());

        checker.check(
            "tskv\t" +
                "tskv_format=s3-access-log\t" +
                "timestamp=2021-04-20T20:05:55\t" +
                "timezone=+0300\t" +
                "status=200\t" +
                "protocol=HTTP/2.0\t" +
                "method=GET\t" +
                "request=/get-canvas-html5/3006599/94e0babb-0cff-4a0d-928e-4398922751f7/image.jpg\t" +
                "referer=https://yastatic.net/\t" +
                "cookies=-\t" +
                "user_agent=Mozilla/5.0 (Linux; Android 9; JAT-LX1 Build/HONORJAT-LX1; wv) AppleWebKit/537.36 (KHTML," +
                " like Gecko) Version/4.0 Chrome/90.0.4430.82 Mobile Safari/537.36\t" +
                "vhost=storage.mds.yandex.net\t" +
                "ip=81.13.123.6\t" +
                "x_forwarded_for=-\t" +
                "x_real_ip=-\t" +
                "request_id=8b919a49e1116b8e\t" +
                "args=-\t" +
                "scheme=https\t" +
                "port=443\t" +
                "ssl_handshake_time=0.000\t" +
                "ssl_protocol=TLSv1.2\t" +
                "upstream_cache_status=-\t" +
                "upstream_addr=unix:/var/run/mediastorage/mediastorage-proxy.sock\t" +
                "upstream_status=200\t" +
                "http_y_service=-\t" +
                "tcpinfo_rtt=86460\t" +
                "tcpinfo_rttvar=34157\t" +
                "tcpinfo_snd_cwnd=22\t" +
                "tcpinfo_rcv_space=14100\t" +
                "tcpinfo_lost=78\t" +
                "tcpinfo_retrans=24\t" +
                "tcpinfo_retransmits=0\t" +
                "tcpinfo_total_retrans=4294956142\t" +
                "origin=https://yandex.ru\t" +
                "x_s3_storage_class=STANDARD\t" +
                "x_s3_handler=-\t" +
                "x_s3_cloud_id=b1g46nqg6k0l0ub7ntam\t" +
                "x_s3_folder_id=-\t" +
                "x_s3_requester=-\t" +
                "x_s3_bucket=-\t" +
                "x_s3_bucket_tags={}\t" +
                "x_s3_object_key=-\t" +
                "x_s3_version_id=-\t" +
                "x_yandex_yarl_limit=-\t" +
                "elliptics_cache=hit\t" +
                "range=-\t" +
                "bytes_received=66\t" +
                "bytes_sent=10737418579\t" +
                "content_length=-\t" +
                "upstream_content_length=-\t" +
                "upstream_response_time=0.000\t" +
                "request_time=0.002\t" +
                "request_completion=OK\t" +
                "ssl_session_id=cbebbbb0db2128695b2d74ad67a5dd4390225d03a619fd9563aa13b682743f57\t" +
                "x_s3_access_key=JP_BuF-uAjSwCmVOsXmI\t" +
                "x_s3_shard_id=-\n",
            new Date(1618938355000L),
            "8b919a49e1116b8e",             // 00: request_id
            "hostname.test",                // 01: host
            "81.13.123.6",           // 02: ipv6
            "81.13.123.6",           // 03: client_ipv6
            Collections.emptyList(),        // 04: x_forwarded_for
            "",                             // 05: x_real_ip
            "Mozilla/5.0 (Linux; Android 9; JAT-LX1 Build/HONORJAT-LX1; wv) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Version/4.0 Chrome/90.0.4430.82 Mobile Safari/537.36", // 06: user_agent
            "https://yastatic.net/",        // 07: referer
            "HTTP/2.0",                     // 08: protocol
            "https",                        // 09: scheme
            "GET",                          // 10: method
            "storage.mds.yandex.net",       // 11: vhost
            443,                            // 12: port
            "/get-canvas-html5/3006599/94e0babb-0cff-4a0d-928e-4398922751f7/image.jpg", // 13: request
            "https://yandex.ru",            // 14: origin
            0,                              // 15: ssl_handshake_time
            "TLSv1.2",                      // 16: ssl_protocol
            2,                              // 17: request_time
            "",                             // 18: range
            0L,                             // 19: content_length
            66L,                            // 20: bytes_received
            Collections.emptyList(),        // 21: cookies
            200,                            // 22: status
            10_737_418_579L,                // 23: bytes_sent
            86460,                          // 24: tcpinfo_rtt
            34157,                          // 25: tcpinfo_rttvar
            22,                             // 26: tcpinfo_snd_cwnd
            14100,                          // 27: tcpinfo_rcv_space
            78L,                            // 28: tcpinfo_lost
            24L,                            // 29: tcpinfo_retrans
            0L,                             // 30: tcpinfo_retransmits
            4_294_956_142L,                 // 31: tcpinfo_total_retrans
            "unix:/var/run/mediastorage/mediastorage-proxy.sock", // 32: upstream_addr
            200,                            // 33: upstream_status
            0L,                             // 34: upstream_content_length
            CacheStatus.UNKNOWN,            // 35: upstream_cache_status
            0,                              // 36: upstream_response_time
            "",                             // 37: http_y_service
            Collections.emptyList(),        // 38: x_yandex_yarl_limit
            "",                             // 39: x_s3_requester
            "",                             // 40: x_s3_handler
            "b1g46nqg6k0l0ub7ntam",         // 41: x_s3_cloud_id
            "",                             // 42: x_s3_folder_id
            "unknown",                      // 43: x_s3_bucket
            "",                             // 44: x_s3_object_key
            "",                             // 45: x_s3_version_id
            "STANDARD",                     // 46: x_s3_storage_class
            Collections.emptyList(),        // 47: x_s3_bucket_tags (keys)
            Collections.emptyList(),        // 48: x_s3_bucket_tags (vals)
            CacheStatus.HIT,                // 49: elliptics_cache
            Collections.emptyList(),        // 50: kvKeys (query args)
            Collections.emptyList(),        // 51: kvValues (query args)
            "OK",                           // 52: request_completion
            "cbebbbb0db2128695b2d74ad67a5dd4390225d03a619fd9563aa13b682743f57", // 53: ssl_session_id
            "JP_BuF-uAjSwCmVOsXmI",         // 54: x_s3_access_key
            ""                              // 55: x_s3_shard_id
        );
    }

    @Test
    @SuppressWarnings("MethodName")
    public void testParse_allEmpty() throws Exception {
        LogParserChecker checker = new LogParserChecker(new S3AccessLogParser());

        checker.check(
            "tskv\t" +
                "tskv_format=s3-access-log\t" +
                "timestamp=2021-04-20T20:05:55\t" +
                "timezone=+0300\t" +
                "status=-\t" +
                "protocol=-\t" +
                "method=-\t" +
                "request=-\t" +
                "referer=-\t" +
                "cookies=-\t" +
                "user_agent=-\t" +
                "vhost=-\t" +
                "ip=-\t" +
                "x_forwarded_for=-\t" +
                "x_real_ip=-\t" +
                "request_id=-\t" +
                "args=-\t" +
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
                "origin=-\t" +
                "x_s3_storage_class=-\t" +
                "x_s3_handler=-\t" +
                "x_s3_cloud_id=-\t" +
                "x_s3_folder_id=-\t" +
                "x_s3_requester=-\t" +
                "x_s3_bucket=-\t" +
                "x_s3_bucket_tags=-\t" +
                "x_s3_object_key=-\t" +
                "x_s3_version_id=-\t" +
                "x_yandex_yarl_limit=-\t" +
                "elliptics_cache=-\t" +
                "range=-\t" +
                "bytes_received=-\t" +
                "bytes_sent=-\t" +
                "content_length=-\t" +
                "upstream_content_length=-\t" +
                "upstream_response_time=-\t" +
                "request_time=-\t" +
                "request_completion=\t" +
                "ssl_session_id=-\t" +
                "x_s3_access_key=-\t" +
                "x_s3_shard_id=-\n",
            new Date(1618938355000L),
            "",                             // 00: request_id
            "hostname.test",                // 01: host
            "",                             // 02: ipv6
            "",                             // 03: client_ipv6
            Collections.emptyList(),        // 04: x_forwarded_for
            "",                             // 05: x_real_ip
            "",                             // 06: user_agent
            "",                             // 07: referer
            "",                             // 08: protocol
            "",                             // 09: scheme
            "",                             // 10: method
            "",                             // 11: vhost
            0,                              // 12: port
            "",                             // 13: request
            "",                             // 14: origin
            0,                              // 15: ssl_handshake_time
            "",                             // 16: ssl_protocol
            0,                              // 17: request_time
            "",                             // 18: range
            0L,                             // 19: content_length
            0L,                             // 20: bytes_received
            Collections.emptyList(),        // 21: cookies
            0,                              // 22: status
            0L,                             // 23: bytes_sent
            0,                              // 24: tcpinfo_rtt
            0,                              // 25: tcpinfo_rttvar
            0,                              // 26: tcpinfo_snd_cwnd
            0,                              // 27: tcpinfo_rcv_space
            0L,                             // 28: tcpinfo_lost
            0L,                             // 29: tcpinfo_retrans
            0L,                             // 30: tcpinfo_retransmits
            0L,                             // 31: tcpinfo_total_retrans
            "",                             // 32: upstream_addr
            0,                              // 33: upstream_status
            0L,                             // 34: upstream_content_length
            CacheStatus.UNKNOWN,            // 35: upstream_cache_status
            0,                              // 36: upstream_response_time
            "",                             // 37: http_y_service
            Collections.emptyList(),        // 38: x_yandex_yarl_limit
            "",                             // 39: x_s3_requester
            "",                             // 40: x_s3_handler
            "",                             // 41: x_s3_cloud_id
            "",                             // 42: x_s3_folder_id
            "unknown",                      // 43: x_s3_bucket
            "",                             // 44: x_s3_object_key
            "",                             // 45: x_s3_version_id
            "",                             // 46: x_s3_storage_class
            Collections.emptyList(),        // 47: x_s3_bucket_tags (keys)
            Collections.emptyList(),        // 48: x_s3_bucket_tags (vals)
            CacheStatus.UNKNOWN,            // 49: elliptics_cache
            Collections.emptyList(),        // 50: kvKeys (query args)
            Collections.emptyList(),        // 51: kvValues (query args)
            "",                             // 52: request_completion
            "",                             // 53: ssl_session_id
            "",                             // 54: x_s3_access_key
            ""                              // 55: x_s3_shard_id

        );
    }

    @Test
    @SuppressWarnings("MethodName")
    public void testParse_allFilled() throws Exception {
        LogParserChecker checker = new LogParserChecker(new S3AccessLogParser());

        checker.check(
            "tskv\t" +
                "tskv_format=s3-access-log\t" +
                "timestamp=2021-04-20T20:12:03\t" +
                "timezone=+0300\t" +
                "status=200\t" +
                "protocol=HTTP/2.0\t" +
                "method=GET\t" +
                "request=/path/to/document\t" +
                "referer=referer field value\t" +
                "cookies=cookie1=value1\t" +
                "user_agent=UserAgent field value\t" +
                "vhost=example-vhost.s3.yandex.net\t" +
                "ip=1.1.1.1\t" +
                "x_forwarded_for=2.2.2.2\t" +
                "x_real_ip=3.3.3.3\t" +
                "request_id=72b33500f35fcba1\t" +
                "args=a=1&b=2\t" +
                "scheme=https\t" +
                "port=443\t" +
                "ssl_handshake_time=0.111\t" +
                "ssl_protocol=SomeSSLProtocolName\t" +
                "upstream_cache_status=miss\t" +
                "upstream_addr=unix:/path/to/unix/socket\t" +
                "upstream_status=200\t" +
                "http_y_service=y-service-name\t" +
                "tcpinfo_rtt=4001\t" +
                "tcpinfo_rttvar=4002\t" +
                "tcpinfo_snd_cwnd=4003\t" +
                "tcpinfo_rcv_space=4004\t" +
                "tcpinfo_lost=4005\t" +
                "tcpinfo_retrans=4006\t" +
                "tcpinfo_retransmits=4007\t" +
                "tcpinfo_total_retrans=4008\t" +
                "origin=example.com\t" +
                "x_s3_storage_class=STORAGECLASSNAME\t" +
                "x_s3_handler=GET Object\t" +
                "x_s3_cloud_id=cloudID\t" +
                "x_s3_folder_id=5555\t" +
                "x_s3_requester=requester field value\t" +
                "x_s3_bucket=example-bucket\t" +
                "x_s3_bucket_tags={\"tag1\": \"val1\"}\t" +
                "x_s3_object_key=path/to/object\t" +
                "x_s3_version_id=6666\t" +
                "x_yandex_yarl_limit=some-limit-name\t" +
                "elliptics_cache=bypass\t" +
                "range=100-1000\t" +
                "bytes_received=7777\t" +
                "bytes_sent=8888\t" +
                "content_length=9999\t" +
                "upstream_response_time=0.222\t" +
                "request_time=0.333\t" +
                "request_completion=OK\t" +
                "ssl_session_id=SomeSessionId\t" +
                "x_s3_access_key=fsgfdfsdfgrs34542\t" +
                "x_s3_shard_id=17\n",
            new Date(1618938723000L),
            "72b33500f35fcba1",                             // 00: request_id
            "hostname.test",                                // 01: host
            "1.1.1.1",                               // 02: ipv6
            "2.2.2.2",                               // 03: client_ipv6
            Collections.singletonList("2.2.2.2"),    // 04: x_forwarded_for
            "3.3.3.3",                               // 05: x_real_ip
            "UserAgent field value",                        // 06: user_agent
            "referer field value",                          // 07: referer
            "HTTP/2.0",                                     // 08: protocol
            "https",                                        // 09: scheme
            "GET",                                          // 10: method
            "example-vhost.s3.yandex.net",                  // 11: vhost
            443,                                            // 12: port
            "/path/to/document",                            // 13: request
            "example.com",                                  // 14: origin
            111,                                            // 15: ssl_handshake_time
            "SomeSSLProtocolName",                          // 16: ssl_protocol
            333,                                            // 17: request_time
            "100-1000",                                     // 18: range
            9999L,                                          // 19: content_length
            7777L,                                          // 20: bytes_received
            Collections.singletonList("cookie1=value1"),    // 21: cookies
            200,                                            // 22: status
            8888L,                                          // 23: bytes_sent
            4001,                                           // 24: tcpinfo_rtt
            4002,                                           // 25: tcpinfo_rttvar
            4003,                                           // 26: tcpinfo_snd_cwnd
            4004,                                           // 27: tcpinfo_rcv_space
            4005L,                                          // 28: tcpinfo_lost
            4006L,                                          // 29: tcpinfo_retrans
            4007L,                                          // 30: tcpinfo_retransmits
            4008L,                                          // 31: tcpinfo_total_retrans
            "unix:/path/to/unix/socket",                    // 32: upstream_addr
            200,                                            // 33: upstream_status
            0L,                                             // 34: upstream_content_length
            CacheStatus.MISS,                               // 35: upstream_cache_status
            222,                                            // 36: upstream_response_time
            "y-service-name",                               // 37: http_y_service
            Collections.singletonList("some-limit-name"),   // 38: x_yandex_yarl_limit
            "requester field value",                        // 39: x_s3_requester
            "GET Object",                                   // 40: x_s3_handler
            "cloudID",                                      // 41: x_s3_cloud_id
            "5555",                                         // 42: x_s3_folder_id
            "example-bucket",                               // 43: x_s3_bucket
            "path/to/object",                               // 44: x_s3_object_key
            "6666",                                         // 45: x_s3_version_id
            "STORAGECLASSNAME",                             // 46: x_s3_storage_class
            Collections.singletonList("tag1"),              // 47: x_s3_bucket_tags (keys)
            Collections.singletonList("val1"),              // 48: x_s3_bucket_tags (vals)
            CacheStatus.BYPASS,                             // 49: elliptics_cache
            Arrays.asList("a", "b"),                        // 50: kv_keys
            Arrays.asList("1", "2"),                        // 51: kv_values
            "OK",                                           // 52: request_completion
            "SomeSessionId",                                // 53: ssl_session_id
            "fsgfdfsdfgrs34542",                            // 54: x_s3_access_key
            "17"                                            // 55: x_s3_shard_id
        );
    }
}
