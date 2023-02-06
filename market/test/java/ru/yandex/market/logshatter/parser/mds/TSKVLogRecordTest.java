package ru.yandex.market.logshatter.parser.mds;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.TskvSplitter;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TSKVLogRecordTest {
    @Test
    public void parseRequest() {
        TskvSplitter splitter = new TskvSplitter("\trequest=-");

        assertEquals("", (new TSKVLogRecord(splitter)).request());

        splitter = new TskvSplitter("\trequest=/get-yabs_performance/3928662/2a0000017606fc5c4509aadc908afaacfcb9" +
            "/small");
        assertEquals("/get-yabs_performance/3928662/2a0000017606fc5c4509aadc908afaacfcb9/small",
            (new TSKVLogRecord(splitter)).request());

        splitter = new TskvSplitter("\trequest=//get-yapic/0/0-0/islands-retina-middle");
        assertEquals("/get-yapic/0/0-0/islands-retina-middle", (new TSKVLogRecord(splitter)).request());


        splitter = new TskvSplitter("\trequest=/get-altay/1348156/2a00000167a38f4e08cd50a33f4f769dbcb2/XXL" +
            "/?api_client_type=yandex");
        assertEquals("/get-altay/1348156/2a00000167a38f4e08cd50a33f4f769dbcb2/XXL",
            (new TSKVLogRecord(splitter)).request());

        splitter = new TskvSplitter("\trequest=/get-yabs_performance/3928662/2a0000017606fc5c4509aadc908afaacfcb9" +
            "/small/");
        assertEquals("/get-yabs_performance/3928662/2a0000017606fc5c4509aadc908afaacfcb9/small",
            (new TSKVLogRecord(splitter)).request());
    }

    @Test
    public void parseXForwardedFor() {
        TskvSplitter splitter = new TskvSplitter("\tx_forwarded_for=-");
        assertEquals(0, (new TSKVLogRecord(splitter)).xForwardedFor().size());

        splitter = new TskvSplitter("\tx_forwarded_for=37.194.122.7");
        assertEquals(1, (new TSKVLogRecord(splitter)).xForwardedFor().size());
        assertEquals("37.194.122.7", (new TSKVLogRecord(splitter)).xForwardedFor().get(0));

        splitter = new TskvSplitter("\tx_forwarded_for=8.8.8.8, 2001:67c:2660:425:92e2:baff:fe0b:6f64");
        assertEquals(2, (new TSKVLogRecord(splitter)).xForwardedFor().size());
        assertEquals("8.8.8.8", (new TSKVLogRecord(splitter)).xForwardedFor().get(0));
        assertEquals("2001:67c:2660:425:92e2:baff:fe0b:6f64", (new TSKVLogRecord(splitter)).xForwardedFor().get(1));
    }

    @Test
    public void parseCookies() {
        TskvSplitter splitter = new TskvSplitter("\tcookies=-");
        assertEquals(0, (new TSKVLogRecord(splitter)).cookies().size());

        splitter = new TskvSplitter("\tcookies=cookie1=1");
        assertEquals(1, (new TSKVLogRecord(splitter)).cookies().size());
        assertEquals("cookie1=1", (new TSKVLogRecord(splitter)).cookies().get(0));

        splitter = new TskvSplitter("\tcookies=cookie1=1; cookie2=asdf");
        List<String> parsedCookies = (new TSKVLogRecord(splitter)).cookies();
        assertEquals(2, parsedCookies.size());
        assertEquals("cookie1=1", parsedCookies.get(0));
        assertEquals("cookie2=asdf", parsedCookies.get(1));
    }

    @Test
    public void getClientIPV6() {
        TskvSplitter splitter = new TskvSplitter("");
        assertEquals("", (new TSKVLogRecord(splitter)).clientIPv6());

        splitter = new TskvSplitter("\tip=37.194.122.7\tx_forwarded_for=-");
        assertEquals("37.194.122.7", (new TSKVLogRecord(splitter)).clientIPv6());


        splitter = new TskvSplitter("\tip=37.194.122.7\tx_forwarded_for=192.168.1.1");
        assertEquals("192.168.1.1", (new TSKVLogRecord(splitter)).clientIPv6());

        splitter = new TskvSplitter("\tip=-\tx_forwarded_for=-");
        assertEquals("", (new TSKVLogRecord(splitter)).clientIPv6());

        splitter = new TskvSplitter("\tip=-\tx_forwarded_for=2001:67c:2660:425:92e2:baff:fe0b:6f64, " +
            "2a02:6b8:c0b:3e67:10e:fafa:0:28b1");
        assertEquals("2001:67c:2660:425:92e2:baff:fe0b:6f64", (new TSKVLogRecord(splitter)).clientIPv6());
    }

    @Test
    public void parseArgs() {
        assertEquals(0, TSKVLogRecord.parseArgs("").size());
        assertEquals(1, TSKVLogRecord.parseArgs("webp=true").size());
        assertEquals("true", TSKVLogRecord.parseArgs("webp=true").get("webp"));

        Map<String, String> parsedArgs = TSKVLogRecord.parseArgs("webp=false&ns=disk&service=disk");
        assertEquals(3, parsedArgs.size());
        assertEquals("false", parsedArgs.get("webp"));
        assertEquals("disk", parsedArgs.get("ns"));
        assertEquals("disk", parsedArgs.get("service"));

        parsedArgs = TSKVLogRecord.parseArgs("thumbnail=%7B%22command%22%3A%22gravity%22%2C%22spread%22%3A10%2C" +
            "%22gravity-type%22%3A%22center%22%2C%22quality%22%3A90%2C%22height%22%3A100%2C%22width%22%3A176%7D");
        assertEquals(1, parsedArgs.size());
        assertEquals(
            "{\"command\":\"gravity\",\"spread\":10,\"gravity-type\":\"center\",\"quality\":90,\"height\":100," +
                "\"width\":176}",
            parsedArgs.get("thumbnail")
        );
    }

    @Test
    public void hideStringValue() {
        assertEquals("AsdW******43Re", TSKVLogRecord.hideStringValue("AsdWdklajDKJwdIHSDwoiuSwJN349#$*" +
            "(#$7d#*cjJKEH43Re"));
        assertEquals("8d3f******0991", TSKVLogRecord.hideStringValue(
            "8d3fffddf79e9a232ffd19f9ccaa4d6b37a6a243dbe0f23137b108a043d9da13121a9b505c804956b22e93c7f93969f4a7ba8" +
                "ddea45bf4aab0bebc8f814e0991"));
        assertEquals("******", TSKVLogRecord.hideStringValue("78dc4512061a66928e9d6284"));
        assertEquals("78dc******2841", TSKVLogRecord.hideStringValue("78dc4512061a66928e9d62841"));

        assertEquals("******", TSKVLogRecord.hideStringValue(""));
        assertEquals("******", TSKVLogRecord.hideStringValue("1"));
        assertEquals("******", TSKVLogRecord.hideStringValue("qwe"));
    }

    @Test
    public void hideArgValues() {
        Map<String, String> args = new HashMap<>();
        args.put("sign", "6b80b472f7e7b6184326546e4586ed5593c0eee8fea4e63134c4c03c6f8aa758");

        TSKVLogRecord.hideArgValues(args, "sign");
        assertEquals("6b80******a758", args.get("sign"));

        args.put("sign", "6b80b472f7e7b6184326546e4586ed5593c0eee8fea4e63134c4c03c6f8aa758");
        TSKVLogRecord.hideArgValues(args, "Sign");
        assertEquals("6b80******a758", args.get("sign"));

        args.put("key", "6b80b472f7e7b6184326546e4586ed5593c0eee8fea4e63134c4c03c6f8aa758");
        TSKVLogRecord.hideArgValues(args, "KEY");
        assertEquals("6b80******a758", args.get("key"));

        args.put("sign", "6b80b472f7e7b6184326546e4586ed5593c0eee8fea4e63134c4c03c6f8aa758");
        args.put("key", "6b80b472f7e7b6184326546e4586ed5593c0eee8fea4e63134c4c03c6f8aa758");
        TSKVLogRecord.hideArgValues(args, "kEy", "sIGN");
        assertEquals("6b80******a758", args.get("key"));
        assertEquals("6b80******a758", args.get("sign"));
    }
}
