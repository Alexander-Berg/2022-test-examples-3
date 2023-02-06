package ru.yandex.market.logshatter.parser.strm;

import java.util.HashMap;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.strm.extractors.StrmRtmpErrorLogExtractor;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StrmRtmpErrorLogExtractorTest {
    @Test
    public void testParseEventSimple() {
        String line = "2021/02/11 15:59:07.066 [info] 38#38: *110230 notify: publish_done 'vybrator.tst.strm.yandex" +
            ".net/on_publish_done', client: 2a02:6b8:c08:d912:0:4620:7afd:0, server: [::]:1935";
        assertEquals(new HashMap<String, String>() {{
            put("timestamp", "2021/02/11 15:59:07.066");
            put("level", "info");
            put("pid", "38");
            put("tid", "38");
            put("cid", "110230");
            put("message", "notify: publish_done 'vybrator.tst.strm.yandex.net/on_publish_done', client: " +
                "2a02:6b8:c08:d912:0:4620:7afd:0, server: [::]:1935");
            put("remote_addr", "2a02:6b8:c08:d912:0:4620:7afd:0");
            put("event", "notify");
        }}, new StrmRtmpErrorLogExtractor(line).getValues());
    }

    @Test
    public void testParseWithEventData() {
        String line = "2021/02/11 15:57:07.544 [info] 38#38: *110234 play: name='4967a750099c7d9b81069ccfa336e384' " +
            "args='' start=-1000 duration=0 reset=0 silent=0, client: 2a02:6b8:c11:ca0:0:434f:47ef:0, server: " +
            "[::]:1935";
        assertEquals(new HashMap<String, String>() {{
            put("timestamp", "2021/02/11 15:57:07.544");
            put("level", "info");
            put("pid", "38");
            put("tid", "38");
            put("cid", "110234");
            put("message", "play: name='4967a750099c7d9b81069ccfa336e384' args='' start=-1000 duration=0 reset=0 " +
                "silent=0, client: 2a02:6b8:c11:ca0:0:434f:47ef:0, server: [::]:1935");
            put("remote_addr", "2a02:6b8:c11:ca0:0:434f:47ef:0");
            put("event", "play");
            put("name", "4967a750099c7d9b81069ccfa336e384");
            put("args", "");
        }}, new StrmRtmpErrorLogExtractor(line).getValues());
    }

    @Test
    public void testParseClientConnected() {
        String line = "2021/02/11 15:57:07.495 [info] 38#38: *110234 client connected '2a02:6b8:c11:ca0:0:434f:47ef:0'";
        assertEquals(new HashMap<String, String>() {{
            put("timestamp", "2021/02/11 15:57:07.495");
            put("level", "info");
            put("pid", "38");
            put("tid", "38");
            put("cid", "110234");
            put("message", "client connected '2a02:6b8:c11:ca0:0:434f:47ef:0'");
            put("remote_addr", "2a02:6b8:c11:ca0:0:434f:47ef:0");
        }}, new StrmRtmpErrorLogExtractor(line).getValues());
    }

    @Test
    public void testParseError() {
        String line = "2021/02/11 15:57:09.420 [error] 38#38: *110140 recv: recv returned n=0 (11: Resource " +
            "temporarily unavailable), client: 2a02:6b8:c0b:159b:0:434f:f4aa:0, server: [::]:1935";
        assertEquals(new HashMap<String, String>() {{
            put("timestamp", "2021/02/11 15:57:09.420");
            put("level", "error");
            put("pid", "38");
            put("tid", "38");
            put("cid", "110140");
            put("message", "recv: recv returned n=0 (11: Resource temporarily unavailable), client: " +
                "2a02:6b8:c0b:159b:0:434f:f4aa:0, server: [::]:1935");
            put("remote_addr", "2a02:6b8:c0b:159b:0:434f:f4aa:0");
            put("event", "recv");
        }}, new StrmRtmpErrorLogExtractor(line).getValues());
    }

    @Test
    public void testParseKeepalive() {
        String line = "2021/02/11 15:59:07.955 [info] 38#38: *110311 client 2a02:6b8:c08:e02f:0:434f:c1e2:0 closed " +
            "keepalive connection";
        assertEquals(new HashMap<String, String>() {{
            put("timestamp", "2021/02/11 15:59:07.955");
            put("level", "info");
            put("pid", "38");
            put("tid", "38");
            put("cid", "110311");
            put("message", "client 2a02:6b8:c08:e02f:0:434f:c1e2:0 closed keepalive connection");
            put("remote_addr", "2a02:6b8:c08:e02f:0:434f:c1e2:0");
        }}, new StrmRtmpErrorLogExtractor(line).getValues());
    }
}
