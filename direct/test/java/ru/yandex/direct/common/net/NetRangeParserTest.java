package ru.yandex.direct.common.net;

import org.junit.Test;


public class NetRangeParserTest {
    @Test
    public void parseSingleNetworkTest() {
        NetRangeParser.parseSingleNetwork("127.0.0.1");
        NetRangeParser.parseSingleNetwork("192.168.0.1");
        NetRangeParser.parseSingleNetwork("192.168.0.1/24");
        NetRangeParser.parseSingleNetwork("192.168.0.1 - 192.168.0.50");

        NetRangeParser.parseSingleNetwork("::1");
        NetRangeParser.parseSingleNetwork("1122:3344:5566:7788:99aa:bbcc:ddee:ff00");
        NetRangeParser.parseSingleNetwork("1122:3344:5566:7788:99aa:bbcc:ddee:ff00/120");
        NetRangeParser
                .parseSingleNetwork("1122:3344:5566:7788:99aa:bbcc:ddee:ff00-1122:3344:5566:7788:99aa:bbcc:ddee:ffff");

        NetRangeParser.parseSingleNetwork("::ffff:198.11.11.11");
        NetRangeParser.parseSingleNetwork("::ffff:198.11.11.11/120");
        NetRangeParser.parseSingleNetwork("::ffff:198.11.11.11-::ffff:198.11.11.30");
        NetRangeParser.parseSingleNetwork("::ffff:127.0.0.1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyNetworkStringTest() {
        NetRangeParser.parseSingleNetwork("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void tooManyBytesTestOne() {
        NetRangeParser.parseSingleNetwork("0000:0000:0000:0000:0000:ffff:127.0.0.0.1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void tooManyBytesTestTwo() {
        NetRangeParser.parseSingleNetwork("0000:0000:0000:0000:0000:0000:ffff:127.0.0.1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void hybridRangeExceptionTest() {
        NetRangeParser.parseSingleNetwork("0000:0000:0000:0000:0000:0000:ffff:192.168.1.1-192.168.1.2");
    }


}
