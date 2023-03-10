package ru.yandex.market.logshatter.parser.strm;

import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;

class StrmNginxErrorLogParserTest {
    private LogParserChecker checker;

    @BeforeEach
    public void setUp() {
        checker = new LogParserChecker(new StrmNginxErrorLogParser());
        checker.setHost("testhost");
    }

    @Test
    void parseInitError() throws Exception {
        String line = StrmTestUtils.readTestLine("strm/nginx-error.log", 0);

        checker.check(
            line,
            new Date(1617883227999L),
            1617883227.999d,
            "testhost", // host
            "notice", // level
            1001839L, // pid
            1001839L, // tid
            0L, // cid
            "http sdch filter init in /etc/nginx/nginx.conf:36", // message
            "", // client
            "", // server
            "", // request_method
            "", // request_uri
            "", // request_protocol
            "", // subrequest
            "", // upstream
            "", // vhost
            "" // referrer
        );
    }

    @Test
    void parseFullRequestError() throws Exception {
        String line = StrmTestUtils.readTestLine("strm/nginx-error.log", 1);

        checker.check(
            line,
            new Date(1617890010773L),
            1617890010.773d,
            "testhost", // host
            "error", // level
            1081643L, // pid
            1081643L, // tid
            388280917L, // cid
            "upstream prematurely closed connection while reading response header from upstream, client: 90.151.90" +
                ".150, server: _, request: \"GET /vh-ottenc-converted/vod-content/4883418e586ab5008f46d93d4d2e0894" +
                "/7838580x1613188466xddba57a4-c2ac-4f64-b7dc-9e8e61c16460/dash/ysign1" +
                "=a0b7c05789bbe6cdc05bdb930bec162aa2db84aa20991435f11e9700c8fabeea,lid=1520,no_cache=1,pfx," +
                "ts=607c8289/video_sdr_avc_720p_4000/seg-133" +
                ".m4s?vsid=6983b910fa88701a6cdcfc51a23e5c2ef8e068d6af5dxWEBx6465x1617889675&ottsession" +
                "=9729645576f9425097cacca5ffbe38e7&from=ott-kp&no_cache=1&t=1617890007822 HTTP/1.1\", upstream: " +
                "\"http://[2a02:6b8:c0a:3eb8:0:495f:5cc6:0]:9090/vh-ottenc-converted/vod-content" +
                "/4883418e586ab5008f46d93d4d2e0894/7838580x1613188466xddba57a4-c2ac-4f64-b7dc-9e8e61c16460/dash" +
                "/video_sdr_avc_720p_4000/seg-133" +
                ".m4s?t=1617890007822&lid=1520&vsid=6983b910fa88701a6cdcfc51a23e5c2ef8e068d6af5dxWEBx6465x1617889675" +
                "&from=ott-kp&no_cache=1&ottsession=9729645576f9425097cacca5ffbe38e7&for-regional-cache=1\", host: " +
                "\"ext-strm-marrt06.strm.yandex.net\", referrer: \"https://hd.kinopoisk.ru/\"", // message
            "::ffff:90.151.90.150", // client
            "_", // server
            "GET", // request_method
            "/vh-ottenc-converted/vod-content/4883418e586ab5008f46d93d4d2e0894/7838580x1613188466xddba57a4-c2ac" +
                "-4f64-b7dc-9e8e61c16460/dash/ysign1" +
                "=a0b7c05789bbe6cdc05bdb930bec162aa2db84aa20991435f11e9700c8fabeea,lid=1520,no_cache=1,pfx," +
                "ts=607c8289/video_sdr_avc_720p_4000/seg-133" +
                ".m4s?vsid=6983b910fa88701a6cdcfc51a23e5c2ef8e068d6af5dxWEBx6465x1617889675&ottsession" +
                "=9729645576f9425097cacca5ffbe38e7&from=ott-kp&no_cache=1&t=1617890007822", // request_uri
            "HTTP/1.1", // request_protocol
            "", // subrequest
            "http://[2a02:6b8:c0a:3eb8:0:495f:5cc6:0]:9090/vh-ottenc-converted/vod-content" +
                "/4883418e586ab5008f46d93d4d2e0894/7838580x1613188466xddba57a4-c2ac-4f64-b7dc-9e8e61c16460/dash" +
                "/video_sdr_avc_720p_4000/seg-133" +
                ".m4s?t=1617890007822&lid=1520&vsid=6983b910fa88701a6cdcfc51a23e5c2ef8e068d6af5dxWEBx6465x1617889675" +
                "&from=ott-kp&no_cache=1&ottsession=9729645576f9425097cacca5ffbe38e7&for-regional-cache=1", // upstream
            "ext-strm-marrt06.strm.yandex.net", // vhost
            "https://hd.kinopoisk.ru/" // referrer
        );
    }

    @Test
    void parseTruncatedError() throws Exception {
        String line = StrmTestUtils.readTestLine("strm/nginx-error.log", 2);

        checker.check(
            line,
            new Date(1617870191886L),
            1617870191.886,
            "testhost", // host
            "error", // level
            3861262L, // pid
            3861262L, // tid
            364452836L, // cid
            "c++ exception [[ nginx/modules/strm_packager/src/common/source_mp4_file.cpp:112: subrequest non-zero " +
                "error code: 499 ]], backtrace [[  ]]  while sending to client, client: 83.149.21.162, server: _, " +
                "request: \"GET /vod/vh-ugc-converted/vod-content/0979da82d2364486684073af194bb16c/7dc6c335-e4a86a07" +
                "-ab382b91-f8a9c120/kaltura/desc_20b7075d2de22b556897b09b11e5658f/vxCgrjSJzthk/ysign1" +
                "=b69a635837d804bd937586d524aaedff16e359baa0cc5070b81265c74736bfc5,lid=1531,no_cache=1,pfx," +
                "ts=607c81de/ysign2=650c8150499b0ef984a137218a2308d77d97aad819dd4675730b6d2e6bd10748,acodec=AAC," +
                "ts=607c81de,ysign1/aid4/init-a1" +
                ".mp4?vsid=2e9b5a33203b5886d5513266e15db78e803c22354e96xWEBx6191x1617869285&adsid" +
                "=fffb010f2f2b6b1b6a300c02e9f62106fa5ce74b5bedxWEBx6191x1617869285&gzip=1&reqid=1617869277.10901" +
                ".87506.748110&from=efir&no_cache=1&packager=1&t=1617870197568 HTTP/1.1\", subrequest: " +
                "\"/vod_kalproxy/video_chunks/vh-ugc-converted/vod-content/0979da82d2364486684073af194bb16c/7dc6c335" +
                "-e4a86a07-ab382b91-f8a9c120/kaltura/audio_4_52b8b7673075776c2aadba4e68d4bfd2.mp4\", upstream: " +
                "\"http://[2a02:6b8:c1b:2a10:0:495f:5958:0]:9090/vod_kalproxy/video_chunks/vh-ugc-converted/vod" +
                "-content/0979da82d2364486684073af194bb16c/7dc6c335-e4a86a07-ab382b91-f8a9c120/kaltura" +
                "/audio_4_52b8b7673075776c2aadba4e68d4bfd2.mp4?&for-regional-cache=1\", host: \"ext-strm-mskmarmgf06" +
                ".strm.yandex.net\", referrer: \"https://yandex" +
                ".ru/efir?use_friendly_frame=true&vsid" +
                "=2e9b5a33203b5886d5513266e15db78e803c22354e96xWEBx6191x1617869285&from=efir&reqid=1617869277.10901" +
                ".87506.748110&slots=344604%2C0%2C14%3B299056%2C0%2C1%3B350102%2C0%2C98%3B351264%2C0%2C22%3B334542" +
                "%2C0%2C89%3B333338%2C0%2C29%3B350866%2C0%2C6%3B350037%2C0%2C20%3B342797%2C0%2C69%3B342859%2C0%2C99" +
                "%3B350797%2C0%2C83%3B351781%2C0%2C71%3B350670%2C0%2C41%3B351278%2C0%2C16%3B32739%2C0%2C41%3B203896" +
                "%2C0%2C35%3B339357%2C0%2C48%3B344369%2C0%2C58%3B329370%2C0%2C15%3B329375%2C0%2C31%3B329392%2C0%2C37" +
                "%3B336917%2C0%2C86%3B336930%2C0%2C10%3B336935%2C0%2C57%3B336940%2C0%2C63%3B336944%2C0%2C40%3B336952" +
                "%2C0%2C93%3B336959%2C0%2C20%3B336964%2C0%2C86%3B336968%2C0%2C97%3B336974%2C0%2C85%3B336979%2C0%2C11" +
                "%3B336983%2C0%2C42%3B336985%2C0%2C10%3B336992%2C0%2C9%3B336993%2C0%2C90%3B336997%2C0%2C94%3B337004" +
                "%2C0%2C71&stream_url=https%3A%2F%2Fstrm.yandex" +
                ".ru%2Fvod%2Fvh-ugc-converted%2Fvod-content%2F0979da82d2364486684073af194bb16c%2F7dc6c335-e4a86a07" +
                "-ab382b91-f8a9c120%2Fkaltura%2Fdesc_20b7075d2de22b556897b09b11e5658f%2FvxCgrjSJzthk%2Fysign1" +
                "%3D626a4844baffbc534d4a4b5452b3a199ae127a129f38dec04b39e20a61222909%2CabcID%3D967%2Cfrom%3Defir" +
                "%2Cpfx%2Cregion%3D10000%2Csfx%2Cts%3D607bec6d%2Fmanifest" +
                ".mpd%3Fclid%3D495%26yandexuid%3D758530301609753009%26slots%3Dnull%26from%3Defir%26stream_block" +
                "%3Dplayer-recommendation%26puid%3D105349537%26from_block%3Dvideosearch_morda%26reqid%3D1617869277" +
                ".10901.87506.748110%26locale%3Dru%26hash%3De7d6e284c92325b8e051e6c87006f419%26service%3Dya-main" +
                "%26content_id%3DvxCgrjSJzthk%26brand-safety-categories%3D%255B%255D&additional_params=%7B%22from%22" +
                "%3A%22efir%22%2C%22stream_block%22%3A%22player-recommendation%22%2C%22puid%22%3A%22105349537%22%2C" +
                "%22from_block%22%3A%22videosearch_morda%22%2C%22reqid%22%3A%221617869277.10901.87506" +
                ".748110%22%2C%22locale%22%3A%22ru%22%2C%22hash%22%3A%22e7d6e284c92325b8e051e6c87006f419%22%2C" +
                "%22slots%22%3A%22344604%2C0%2C14%3B299056%2C0%2C1%3B350102%2C0%2C98%3B351264%2C0%2C22%3B334542%2C0" +
                "%2C89%3B333338%2C0%2C29%3B350866%2C0%2C6%3B350037%2C0%2C20%3B342797%2C0%2C69%3B342859%2C0%2C99" +
                "%3B350797%2C0%2C83%3B351781%2C0%2C71%3B350670%2C0%2C41%3B351278%2C0%2C16%3B32739%2C0%2C41%3B203896" +
                "%2C0%2C35%3B339357%2C0%2C48%3B344369%2C0%2C58%3B329370%2C0%2C15%3B329375%2C0%2C31%3B329392%2C0%2C37" +
                "%3B336917%2C0%2C86%3B336930%2C0%2C10%3B336935%2C0%2C57%3B336940%2C0%2C63%3B336944%2C0%2C40%3B336952" +
                "%2C0%2C93%3B336959%2C0%2C20%3B336964%2C0%2C86%3B336968%2C0%2C97%3B336974%2C0%2C85%3B336979%2C0%2C11" +
                "%3B336983%2C0%2C42%3B336985%2C0%2C10%3B336992%2C0%2C9%3B336993%2C0%2C90%3B336997%2C0%2C94%3B337004" +
                "%2C0%2C71%22%2C%22service%22%3A%22ya-main%22%2C%22content_id%22%3A%22vxCgrjSJzthk%22%2C%22brand" +
                "-safety-categories%2", // message
            "::ffff:83.149.21.162", // client
            "_", // server
            "GET", // request_method
            "/vod/vh-ugc-converted/vod-content/0979da82d2364486684073af194bb16c/7dc6c335-e4a86a07-ab382b91-f8a9c120" +
                "/kaltura/desc_20b7075d2de22b556897b09b11e5658f/vxCgrjSJzthk/ysign1" +
                "=b69a635837d804bd937586d524aaedff16e359baa0cc5070b81265c74736bfc5,lid=1531,no_cache=1,pfx," +
                "ts=607c81de/ysign2=650c8150499b0ef984a137218a2308d77d97aad819dd4675730b6d2e6bd10748,acodec=AAC," +
                "ts=607c81de,ysign1/aid4/init-a1" +
                ".mp4?vsid=2e9b5a33203b5886d5513266e15db78e803c22354e96xWEBx6191x1617869285&adsid" +
                "=fffb010f2f2b6b1b6a300c02e9f62106fa5ce74b5bedxWEBx6191x1617869285&gzip=1&reqid=1617869277.10901" +
                ".87506.748110&from=efir&no_cache=1&packager=1&t=1617870197568", // request_uri
            "HTTP/1.1", // request_protocol
            "/vod_kalproxy/video_chunks/vh-ugc-converted/vod-content/0979da82d2364486684073af194bb16c/7dc6c335" +
                "-e4a86a07-ab382b91-f8a9c120/kaltura/audio_4_52b8b7673075776c2aadba4e68d4bfd2.mp4", // subrequest
            "http://[2a02:6b8:c1b:2a10:0:495f:5958:0]:9090/vod_kalproxy/video_chunks/vh-ugc-converted/vod-content" +
                "/0979da82d2364486684073af194bb16c/7dc6c335-e4a86a07-ab382b91-f8a9c120/kaltura" +
                "/audio_4_52b8b7673075776c2aadba4e68d4bfd2.mp4?&for-regional-cache=1", // upstream
            "ext-strm-mskmarmgf06.strm.yandex.net", // vhost
            "https://yandex.ru/efir?use_friendly_frame=true&vsid" +
                "=2e9b5a33203b5886d5513266e15db78e803c22354e96xWEBx6191x1617869285&from=efir&reqid=1617869277.10901" +
                ".87506.748110&slots=344604%2C0%2C14%3B299056%2C0%2C1%3B350102%2C0%2C98%3B351264%2C0%2C22%3B334542" +
                "%2C0%2C89%3B333338%2C0%2C29%3B350866%2C0%2C6%3B350037%2C0%2C20%3B342797%2C0%2C69%3B342859%2C0%2C99" +
                "%3B350797%2C0%2C83%3B351781%2C0%2C71%3B350670%2C0%2C41%3B351278%2C0%2C16%3B32739%2C0%2C41%3B203896" +
                "%2C0%2C35%3B339357%2C0%2C48%3B344369%2C0%2C58%3B329370%2C0%2C15%3B329375%2C0%2C31%3B329392%2C0%2C37" +
                "%3B336917%2C0%2C86%3B336930%2C0%2C10%3B336935%2C0%2C57%3B336940%2C0%2C63%3B336944%2C0%2C40%3B336952" +
                "%2C0%2C93%3B336959%2C0%2C20%3B336964%2C0%2C86%3B336968%2C0%2C97%3B336974%2C0%2C85%3B336979%2C0%2C11" +
                "%3B336983%2C0%2C42%3B336985%2C0%2C10%3B336992%2C0%2C9%3B336993%2C0%2C90%3B336997%2C0%2C94%3B337004" +
                "%2C0%2C71&stream_url=https%3A%2F%2Fstrm.yandex" +
                ".ru%2Fvod%2Fvh-ugc-converted%2Fvod-content%2F0979da82d2364486684073af194bb16c%2F7dc6c335-e4a86a07" +
                "-ab382b91-f8a9c120%2Fkaltura%2Fdesc_20b7075d2de22b556897b09b11e5658f%2FvxCgrjSJzthk%2Fysign1" +
                "%3D626a4844baffbc534d4a4b5452b3a199ae127a129f38dec04b39e20a61222909%2CabcID%3D967%2Cfrom%3Defir" +
                "%2Cpfx%2Cregion%3D10000%2Csfx%2Cts%3D607bec6d%2Fmanifest" +
                ".mpd%3Fclid%3D495%26yandexuid%3D758530301609753009%26slots%3Dnull%26from%3Defir%26stream_block" +
                "%3Dplayer-recommendation%26puid%3D105349537%26from_block%3Dvideosearch_morda%26reqid%3D1617869277" +
                ".10901.87506.748110%26locale%3Dru%26hash%3De7d6e284c92325b8e051e6c87006f419%26service%3Dya-main" +
                "%26content_id%3DvxCgrjSJzthk%26brand-safety-categories%3D%255B%255D&additional_params=%7B%22from%22" +
                "%3A%22efir%22%2C%22stream_block%22%3A%22player-recommendation%22%2C%22puid%22%3A%22105349537%22%2C" +
                "%22from_block%22%3A%22videosearch_morda%22%2C%22reqid%22%3A%221617869277.10901.87506" +
                ".748110%22%2C%22locale%22%3A%22ru%22%2C%22hash%22%3A%22e7d6e284c92325b8e051e6c87006f419%22%2C" +
                "%22slots%22%3A%22344604%2C0%2C14%3B299056%2C0%2C1%3B350102%2C0%2C98%3B351264%2C0%2C22%3B334542%2C0" +
                "%2C89%3B333338%2C0%2C29%3B350866%2C0%2C6%3B350037%2C0%2C20%3B342797%2C0%2C69%3B342859%2C0%2C99" +
                "%3B350797%2C0%2C83%3B351781%2C0%2C71%3B350670%2C0%2C41%3B351278%2C0%2C16%3B32739%2C0%2C41%3B203896" +
                "%2C0%2C35%3B339357%2C0%2C48%3B344369%2C0%2C58%3B329370%2C0%2C15%3B329375%2C0%2C31%3B329392%2C0%2C37" +
                "%3B336917%2C0%2C86%3B336930%2C0%2C10%3B336935%2C0%2C57%3B336940%2C0%2C63%3B336944%2C0%2C40%3B336952" +
                "%2C0%2C93%3B336959%2C0%2C20%3B336964%2C0%2C86%3B336968%2C0%2C97%3B336974%2C0%2C85%3B336979%2C0%2C11" +
                "%3B336983%2C0%2C42%3B336985%2C0%2C10%3B336992%2C0%2C9%3B336993%2C0%2C90%3B336997%2C0%2C94%3B337004" +
                "%2C0%2C71%22%2C%22service%22%3A%22ya-main%22%2C%22content_id%22%3A%22vxCgrjSJzthk%22%2C%22brand" +
                "-safety-categories%2" // referrer
        );
    }

    @Test
    void parseGarbage() throws Exception {
        String line = StrmTestUtils.readTestLine("strm/nginx-error.log", 3);

        checker.check(
            line,
            new Date(1617901630106L),
            1617901630.106d,
            "testhost", // host
            "error", // level
            734305L, // pid
            734305L, // tid
            795728876L, // cid
            "[lua] metrics.lua:524: Monitor status: false. Err: /etc/nginx/lua/metrics.lua:373: bad argument #1 to " +
                "'find' (string expected, got nil) while logging request, client: ::1, server: _, request: " +
                "\"M\uD9FB\uDCEDV`#jlr2Y@47??HRG#tSX~t    6N??M??dd;=r??g9|~??\"P1;1_EHa%8Yp#:y?-c\"", // message
            "::1", // client
            "_", // server
            "M\uD9FB\uDCEDV`#jlr2Y@47??HRG#tSX~t", // request_method
            "", // request_uri
            "", // request_protocol
            "", // subrequest
            "", // upstream
            "", // vhost
            "" // referrer
        );
    }

    @Test
    void parseOriginalNginx() throws Exception {
        String line = StrmTestUtils.readTestLine("strm/nginx-error.log", 4);

        checker.check(
            line,
            new Date(1606717507000L),
            1606717507.000d,
            "testhost", // host
            "notice", // level
            15496L, // pid
            15496L, // tid
            0L, // cid
            "signal process started", // message
            "", // client
            "", // server
            "", // request_method
            "", // request_uri
            "", // request_protocol
            "", // subrequest
            "", // upstream
            "", // vhost
            "" // referrer
        );
    }
}
