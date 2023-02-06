package ru.yandex.market.logshatter.parser.strm;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import com.google.common.primitives.UnsignedLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;


public class StrmTskvAccessLogParserTest {
    private LogParserChecker checker;

    @BeforeEach
    public void setUp() {
        checker = new LogParserChecker(new StrmTskvAccessLogParser());
        checker.setHost("testhost");
    }

    @Test
    public void parseRegional() throws Exception {
        // generic log from regional
        String line = StrmTestUtils.readTestLine("strm/nginx-tskv-regional.log");

        checker.check(
            line,
            new Date(1581587239167L),
            1581587239.167d,
            "",
            "",
            "",
            -1,
            0L,
            0,
            UnsignedLong.ZERO,
            0L,
            0,
            "chunk",
            "/vh-youtube-converted/vod-content/96430223837623880",
            1483902L,
            Collections.emptyList(),
            0,
            "testhost",
            false,
            0,
            "GET",
            "HTTP/1.1",
            "",
            "",
            "empty",
            -1,
            "::ffff:95.53.40.27",
            57939,
            "vsid=8083db91c1ceb4b57c695bc419274c22f0b56b790511xWEBx3278x1581587155&adsid" +
                "=b4d6681d70c771080a2211a73d59262ef588b020a6c9xWEBx3278x1581587155&gzip=1&reqid=1581587088916796" +
                "-1665565493028482137088749-vla1-1744-V-PAD&no_cache=1",
            "OK",
            "cc7e258af0104d65",
            false,
            "/vh-youtube-converted/vod-content/96430223837623880/dash/video/avc1/3/seg-11.m4s",
            1.576d,
            "https",
            0d,
            "TLSv1.2",
            "200",
            0,
            14100,
            87280,
            4901,
            72,
            0,
            "",
            "",
            "",
            Collections.singletonList("[2a02:6b8:c0e:103:0:584:7be1:7b67]:9090"),
            Collections.singletonList(1483895L),
            "MISS",
            Collections.emptyList(),
            Collections.singletonList(0.108d),
            Collections.singletonList("200"),
            "/vh-youtube-converted/vod-content/96430223837623880/dash/video/avc1/3/seg-11.m4s",
            // fake user-agent detector data
            "desktop",
            "",
            "",
            "",
            "MacOS",
            "Mac OS X Sierra",
            "10.12.6",
            "YandexBrowser",
            "18.9.0.3363",
            "ext-strm-mskrt06.strm.yandex.net",
            "8083db91c1ceb4b57c695bc419274c22f0b56b790511xWEBx3278x1581587155",
            "",
            "",
            "",
            "::ffff:95.53.40.27",
            UnsignedLong.valueOf(0),
            ""
        );
    }

    @Test
    public void parseEmpty() throws Exception {
        String line = "-- MARK --";
        checker.checkEmpty(line);
    }

    @Test
    public void parseMultipleUpstreams() throws Exception {
        // log with multiple upstreams
        String line = StrmTestUtils.readTestLine("strm/nginx-tskv-multiple-upstreams.log");

        checker.check(
            line,
            new Date(1581607725977L),
            1581607725.977d,
            "",
            "",
            "",
            -1,
            0L,
            0,
            UnsignedLong.ZERO,
            0L,
            0,
            "chunk",
            "",
            11126L,
            Collections.emptyList(),
            0,
            "testhost",
            false,
            0,
            "GET",
            "HTTP/1.1",
            "bytes=1486848-1497288",
            "",
            "empty",
            -1,
            "2a01:540:c30b:6d00:47ac:49b8:4c7c:7dab",
            60796,
            "",
            "OK",
            "82050e41f4fddfb1",
            false,
            "/vh-canvas-converted/get-canvas/video_59f1853a0d98b3da35c2ef01_169_360p.mp4",
            0.053d,
            "https",
            0.089d,
            "TLSv1.2",
            "206",
            0,
            13900,
            54955,
            9187,
            15,
            0,
            "",
            "",
            "",
            Arrays.asList("[2a02:6b8:0:3400:0:71d:0:b]:80", "[2a02:6b8:c1d:2e8c:0:584:9726:0]:9090"),
            Arrays.asList(535L, 1497840L),
            "MISS",
            Collections.emptyList(),
            Arrays.asList(0.04d, 0.008d),
            Arrays.asList("200", "200"),
            "/vh-canvas-converted/get-canvas/video_59f1853a0d98b3da35c2ef01_169_360p.mp4",
            // fake user-agent detector data
            "desktop",
            "",
            "",
            "",
            "MacOS",
            "Mac OS X Sierra",
            "10.12.6",
            "YandexBrowser",
            "18.9.0.3363",
            "strm.yandex.ru",
            "",
            "",
            "",
            "",
            "2a01:540:c30b:6d00:47ac:49b8:4c7c:7dab",
            UnsignedLong.ZERO,
            ""
        );
    }

    @Test
    public void parseTumblerLog() throws Exception {
        // log from tumbler
        String line = StrmTestUtils.readTestLine("strm/nginx-tskv-tumbler.log");

        checker.check(
            line,
            new Date(1581673920000L),
            1581673920.0d,
            "",
            "",
            "",
            -1,
            0L,
            0,
            UnsignedLong.ZERO,
            0L,
            0,
            "",
            "",
            4878L,
            Collections.emptyList(),
            0,
            "testhost",
            false,
            0,
            "GET",
            "HTTP/1.1",
            "",
            "",
            "empty",
            -1,
            "::1",
            0,
            "",
            "",
            "d558fee82edb6c4d83dfa7bd3a056090",
            false,
            "/unistat",
            0.002d,
            "http",
            0d,
            "",
            "200",
            0,
            43690,
            29,
            15,
            10,
            0,
            "",
            "",
            "",
            Collections.emptyList(),
            Collections.emptyList(),
            "",
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            "/unistat",
            // fake user-agent detector data
            "desktop",
            "",
            "",
            "",
            "MacOS",
            "Mac OS X Sierra",
            "10.12.6",
            "YandexBrowser",
            "18.9.0.3363",
            "localhost:4444",
            "",
            "",
            "",
            "",
            "",
            UnsignedLong.valueOf(0),
            ""
        );

    }

    @Test
    public void parseAbsentUpstream() throws Exception {
        // sometimes last upstream is absent, but delimiter is in place
        String line = StrmTestUtils.readTestLine("strm/nginx-tskv-absent-upstream.log");

        checker.check(
            line,
            new Date(1581829089101L),
            1581829089.101d,
            "",
            "",
            "",
            -1,
            0L,
            0,
            UnsignedLong.ZERO,
            0L,
            0,
            "chunk",
            "",
            494L,
            Collections.emptyList(),
            0,
            "testhost",
            false,
            0,
            "GET",
            "HTTP/1.1",
            "",
            "",
            "empty",
            -1,
            "2a02:6b8:0:2b09::132",
            41124,
            "vsid=s4gadud7tw4bzls&orig_uri=/kal/vkusnoetv_supres/vkusnoetv_supres0_169_240p.json/seg-316365813-v1-a1" +
                ".ts&for-regional-cache=1&for-regional-cache=1&hide_stub=1",
            "OK",
            "dc22e5f6bfb042f6",
            false,
            "/kalproxy/kal/api/vkusnoetv_supres/vkusnoetv_supres0_169_240p-1581829070000.mp4",
            1.003d,
            "http",
            0d,
            "",
            "502",
            0,
            26514,
            5597,
            2798,
            10,
            0,
            "",
            "",
            "",
            Arrays.asList("[2a02:6b8:0:3400::3:147]:80", "[2a02:6b8:0:3400:0:534:0:6]:80"),
            Arrays.asList(560L, 553L),
            "MISS",
            Collections.emptyList(),
            Arrays.asList(0.004d, 0d),
            Arrays.asList("404", "200"),
            "/vkusnoetv_supres/vkusnoetv_supres0_169_240p-1581829070000.mp4",
            // fake user-agent detector data
            "desktop",
            "",
            "",
            "",
            "MacOS",
            "Mac OS X Sierra",
            "10.12.6",
            "YandexBrowser",
            "18.9.0.3363",
            "src_common_9090",
            "s4gadud7tw4bzls",
            "",
            "",
            "",
            "2a02:6b8:0:2b09::132",
            UnsignedLong.valueOf(0),
            ""
        );
    }

    @Test
    public void parsePlgoAccessLog() throws Exception {
        String line = StrmTestUtils.readTestLine("strm/nginx-tskv-plgo.log");

        checker.check(
            line,
            new Date(1599818993169L),
            1599818993.169d,
            "",
            "",
            "",
            -1,
            0L,
            0,
            UnsignedLong.ZERO,
            0L,
            0,
            "playlist",
            "",
            1293L,
            Collections.emptyList(),
            0,
            "testhost",
            false,
            0,
            "GET",
            "HTTP/1.1",
            "",
            "",
            "empty",
            -1,
            "2a02:6b8:c1d:22a2:0:495f:6cb2:0",
            46806,
            "from=zen%3Azen_lib%3Apublisher_api%3Azen_lib_publisher&chunks=1&vsid" +
                "=6e140232d62eb17daa53a74ffa05b3ceeab5726944c1xWEBx5302x1599818989&t=1599818991125",
            "",
            "632cf2022b84f653",
            false,
            "/chunks/vod/zen-vod/vod-content/10711363880086486385_169_240p.m3u8",
            0.001d,
            "http",
            0d,
            "",
            "200",
            0,
            35352,
            3234,
            26,
            10,
            0,
            "",
            "",
            "",
            Collections.singletonList("[::1]:81"),
            Collections.singletonList(31867L),
            "",
            Collections.emptyList(),
            Collections.singletonList(0.000d),
            Collections.singletonList("200"),
            "/chunks/vod/zen-vod/vod-content/10711363880086486385_169_240p.m3u8",
            // fake user-agent detector data
            "desktop",
            "",
            "",
            "",
            "MacOS",
            "Mac OS X Sierra",
            "10.12.6",
            "YandexBrowser",
            "18.9.0.3363",
            "users-playlist",
            "6e140232d62eb17daa53a74ffa05b3ceeab5726944c1xWEBx5302x1599818989",
            "",
            "host=strm-plgo-production-10.sas.yp-c.yandex.net; version=7321011",
            "link_id=1519; host=ext-strm-m9mts24.strm.yandex.net; contentID=/zen-vod/vod-content/10711363880086486385",
            "::ffff:80.70.109.9",
            UnsignedLong.valueOf(0),
            ""
        );
    }

    @Test
    public void parseDuplicateCookie() throws Exception {
        String line = StrmTestUtils.readTestLine("strm/nginx-tskv-duplicate-cookie.log");

        checker.check(
            line,
            new Date(1600703255017L),
            1600703255.017d, // timestamp_ms
            "", // geo
            "", // environment
            "", // component
            -1, // abc_id
            0L, // bbr_badndwith
            0, // bbr_rtt
            UnsignedLong.ZERO, // client_timestamp
            0L, // connection
            0, // connection_requests
            "playlist", // content_file_type
            "/zen-vod/vod-content/5262582260451505137/44050a21-fb10fc42-d3679472-353813f0/kaltura", // content_id
            1354L, // bytes_sent
            Collections.emptyList(), // exp
            40, // geo_region_id
            "testhost", // host
            false, // is_subrequest
            0, // link_id
            "GET", // method
            "HTTP/1.1", // protocol
            "", // range
            "zen:zen_lib:mobile_morda:morda_zen_lib_mobile", // ref_from
            "zen", // ref_source
            -1, // ref_source_abc_id
            "::ffff:85.140.1.175", // remote_addr
            41315, // remote_port
            "from=zen%3Azen_lib%3Amobile_morda%3Amorda_zen_lib_mobile&video-content-id=vtMvL3CEz6XM&partner-stat-id" +
                "=143140&vsid=0b34596012c0e2143f75284475087c51e37e92f42a4cxWEBx5363x1600703228&adsid" +
                "=008c53ccc9010732c0886fb99e2798a5ef9fc6f7e19fxWEBx5363x1600703228", // request_args
            "OK", // request_completion
            "090b24b6214ce208", // request_id
            false, // request_is_external
            "/vod/zen-vod/vod-content/5262582260451505137/44050a21-fb10fc42-d3679472-353813f0/kaltura" +
                "/desc_fb6df24d2edb15a3510e031efac25a74/vtMvL3CEz6XM/master.m3u8", // request_path
            0.012d, // request_time
            "https", // scheme
            0d, // ssl_handshake_time
            "TLSv1.2", // ssl_protocol
            "200", // status
            0, // stub
            14100, // tcpinfo_rcv_space
            112031, // tcpinfo_rtt
            104127, // tcpinfo_rttvar
            19, // tcpinfo_snd_cwnd
            0, // tcpinfo_total_retrans
            "", // traffic_type
            "vod-kaltura-m3u8", // unistat_prj
            "zen-vod", // unistat_tier
            Collections.singletonList("[2a02:6b8:0:3400:0:71d:0:b]:80"), // upstream_addr
            Collections.singletonList(1131L), // upstream_bytes_received
            "", // upstream_cache_status
            Collections.emptyList(), // upstream_http_user_regions
            Collections.singletonList(0.012d), // upstream_response_time
            Collections.singletonList("200"), // upstream_status
            "/kalvod/vod/zen-vod/vod-content/5262582260451505137/44050a21-fb10fc42-d3679472-353813f0/kaltura" +
                "/desc_fb6df24d2edb15a3510e031efac25a74/vtMvL3CEz6XM/master.m3u8", // uri
            // fake user-agent detector data
            "desktop",
            "",
            "",
            "",
            "MacOS",
            "Mac OS X Sierra",
            "10.12.6",
            "YandexBrowser",
            "18.9.0.3363",
            "strm.yandex.ru", // vhost
            "0b34596012c0e2143f75284475087c51e37e92f42a4cxWEBx5363x1600703228", // vsid
            "", // ottsession
            "host=strm-plgo-production-12.vla.yp-c.yandex.net; version=7342092", // x_plg
            "", // x_plg_debug
            "::ffff:85.140.1.175", // x_real_ip
            UnsignedLong.ZERO, // yandex_uid
            "" // yandex_login
        );
    }

    @Test
    public void parseEscapedTskv() throws Exception {
        String line = StrmTestUtils.readTestLine("strm/nginx-tskv-escaped.log");

        checker.check(
            line,
            new Date(1585844240597L),
            1585844240.597d, // timestamp_ms
            "akamai", // geo
            "production", // environment
            "external-cdn", // component
            -1, // abc_id
            0L, // bbr_badndwith
            0, // bbr_rtt
            UnsignedLong.ZERO, // client_timestamp
            0L, // connection
            0, // connection_requests
            "chunk", // content_file_type
            "/vh-ott-converted/ott-content/484897989-418763813e99ea87b687d662da616e8f", // content_id
            455459L, // bytes_sent
            Collections.emptyList(), // exp
            0, // geo_region_id
            "testhost", // host
            false, // is_subrequest
            0, // link_id
            "GET", // method
            "", // protocol
            "", // range
            "", // ref_from
            "empty", // ref_source
            -1, // ref_source_abc_id
            "::ffff:94.25.168.24", // remote_addr
            0, // remote_port
            "no_cache=1&vsid=7e995037dc8e8bf9977688850191a4374021b482856cxWEBx3481x1585843677", // request_args
            "", // request_completion
            "28a12e0e", // request_id
            false, // request_is_external
            "/vh-ott-converted/ott-content/484897989-418763813e99ea87b687d662da616e8f/2-video_169_576p-528.ts", //
            // request_path
            0.177d, // request_time
            "https", // scheme
            0d, // ssl_handshake_time
            "D", // ssl_protocol
            "000", // status
            0, // stub
            0, // tcpinfo_rcv_space
            0, // tcpinfo_rtt
            0, // tcpinfo_rttvar
            0, // tcpinfo_snd_cwnd
            0, // tcpinfo_total_retrans
            "", // traffic_type
            "", // unistat_prj
            "", // unistat_tier
            Collections.singletonList("parent"), // upstream_addr
            Collections.singletonList(0L), // upstream_bytes_received
            "MISS", // upstream_cache_status
            Collections.emptyList(), // upstream_http_user_regions
            Collections.emptyList(), // upstream_response_time
            Collections.emptyList(), // upstream_status
            "/vh-ott-converted/ott-content/484897989-418763813e99ea87b687d662da616e8f/2-video_169_576p-528.ts", // uri
            // fake user-agent detector data
            "desktop",
            "",
            "",
            "",
            "MacOS",
            "Mac OS X Sierra",
            "10.12.6",
            "YandexBrowser",
            "18.9.0.3363",
            "akamai.strm.yandex.net", // vhost
            "7e995037dc8e8bf9977688850191a4374021b482856cxWEBx3481x1585843677", // vsid
            "", // ottsession
            "", // x_plg
            "", // x_plg_debug
            "", // x_real_ip
            UnsignedLong.valueOf(0L), // yandex_uid
            "" // yandex_login
        );
    }

    @Test
    void parseSubrequest() throws Exception {
        String line = StrmTestUtils.readTestLine("strm/nginx-subrequest-access.log");

        checker.check(
            line,
            new Date(1617815707031L),
            1617815707.031d,
            "mskm9", // geo
            "production", // environment
            "edge", // component
            -1, // abc_id
            0L, // bbr_badndwith
            0, // bbr_rtt
            UnsignedLong.ZERO, // client_timestamp
            0L, // connection
            0, // connection_requests
            "chunk", // content_file_type
            "", // content_id
            0L, // bytes_sent
            Collections.emptyList(), // exp
            0, // geo_region_id
            "testhost", // host
            true, // is_subrequest
            0, // link_id
            "GET", // method
            "", // protocol
            "bytes=0-4095", // range
            "", // ref_from
            "empty", // ref_source
            -1, // ref_source_abc_id
            "", // remote_addr
            0, // remote_port
            "adsid=6e514d2246dd3a348756488d27ccdaafac10bd584bcbxWEBx6191x1617808305&gzip=1&partner_id=278914&reqid" +
                "=1617808300.27243.78679.219902&target_ref=https%3A%2F%2Fyastatic" +
                ".net&uuid=48795d2ccb32062892540f0f7bea8f5e&from=streamhandler_other&vsid" +
                "=21a65026b2dbe09801412f51757d48b532ce00a9ba97xWEBx6191x1617808304&t=1617815702731", // request_args
            "", // request_completion
            "d4ee7585f6fc4e71", // request_id
            false, // request_is_external
            "/kal/zvezda/ysign2=893f418309f18e8710de8ec71d6397f43931e4e184e6344dd4a4358816601632,lid=1504,pfx," +
                "ts=607b542d/ysign1=44542a526d559bbd2f31fc5c470aaaee9a04af40b1e46633148f4e70e1d7136e,dar=169," +
                "res=240,start=1617807600,ts=607b542d,ysign2/fragment-323561648-a1.m4s", // request_path
            0d, // request_time
            "", // scheme
            0d, // ssl_handshake_time
            "", // ssl_protocol
            "206", // status
            0, // stub
            0, // tcpinfo_rcv_space
            0, // tcpinfo_rtt
            0, // tcpinfo_rttvar
            0, // tcpinfo_snd_cwnd
            0, // tcpinfo_total_retrans
            "subrequest", // traffic_type
            "kalproxy-mp4", // unistat_prj
            "zvezda", // unistat_tier
            Collections.emptyList(), // upstream_addr
            Collections.emptyList(), // upstream_bytes_received
            "HIT", // upstream_cache_status
            Collections.emptyList(), // upstream_http_user_regions
            Collections.emptyList(), // upstream_response_time
            Collections.emptyList(), // upstream_status
            "/kalproxy/kal/api/zvezda/zvezda0_169_240p-1617808240000.mp4", // uri
            // fake user-agent detector data
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "", // vhost
            "21a65026b2dbe09801412f51757d48b532ce00a9ba97xWEBx6191x1617808304", // vsid
            "", // ottsession
            "", // x_plg
            "", // x_plg_debug
            "", // x_real_ip
            UnsignedLong.valueOf(0L), // yandex_uid
            "" // yandex_login
        );
    }
}
