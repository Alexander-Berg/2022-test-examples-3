package ru.yandex.market.logshatter.parser.direct.bsexport;

import java.text.SimpleDateFormat;

import org.junit.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;

import java.util.Collections;
import java.util.Date;
import java.util.Arrays;

public class BsExportEssLogParserTest {

    @Test
    public void testParse() throws Exception {
        String line1 = "{'log_time':'2019-01-28 00:03:15'," +
            "'span_id':4493870985097058242," +
            "'cid':1234," +
            "'pid':3123," +
            "'bid':43242," +
            "'bs_banner_id':72057603580143848," +
            "'order_id':987654," +
            "'type':'banner_resources'," +
            "'data':{" +
            "'iterId': 112694569," +
            "'orderId': 28368595," +
            "'adgroupId': 4283900143," +
            "'bannerId': 72057603580143848," +
            "'exportId': 9542215912," +
            "'vcardDomainFilter': '88005552457.phone'," +
            "'updateTime': 1607634000" +
            "}}";

        BsExportEssLogParser parser = new BsExportEssLogParser();
        LogParserChecker checker = new LogParserChecker(parser);

        SimpleDateFormat dateTimeFormat = parser.dateTimeFormat;


        String data = "{\"iterId\":112694569,\"orderId\":28368595,\"adgroupId\":4283900143," +
            "\"bannerId\":72057603580143848,\"exportId\":9542215912,\"vcardDomainFilter\":\"88005552457" +
            ".phone\",\"updateTime\":1607634000}";
        checker.check(line1,
            dateTimeFormat.parse("2019-01-28 00:03:15"),
            1234L, // cid
            0L,
            "",
            "banner_resources", // type
            3123L, // pid
            43242L, // bid
            72057603580143848L, //bs banner id
            987654L, // orderid
            "", // host
            "", // meta
            4493870985097058242L, // span_id
            data
        );
    }

    @Test
    public void testCompressedParse() throws Exception {
        String line1 = "H4sIAAAAAAAAAEVPywrCMBD8FdlzC3k0seQmCOJJkN5ESmsWCaRtTNODlPy7jS16G" +
                "mZ2Zmd3Bjs862A6BAWMMJoTntNyR6XiQhEJGYyu6WujQTFBhCxFAloIzvdlkUF4uxTtJhuMs" +
                "wb9ktBNaEDdZmhTjGTgNnwklISyUhKZQTvWbdP36OttPni9Efp3rdtm0Ggx4BVfE44hCZfkP" +
                "h+X9p97KT/okx8m99XJwqv1wCq9GGO8xw8hxTb28wAAAA==";

        BsExportEssLogParser parser = new BsExportEssLogParser();
        LogParserChecker checker = new LogParserChecker(parser);

        checker.check(line1,
                Collections.singletonList(parser.dateTimeFormat.parse("2021-03-18 16:35:06")),
                Collections.singletonList(new Object[]{
                        60128606L, //cid
                        0L,
                        "",
                        "multiplier", // type
                        0L, // pid
                        0L, // bid
                        0L, //bs banner id
                        160128606L, // order id
                        "", // host
                        "", // meta
                        2505685250514533784L, //span_id
                        "{\"deleteRequest\":{\"OrderID\":\"160128606\",\"AdGroupID\":\"0\",\"Type\":\"Time\"}}"
                })
        );
    }

    @Test
    public void testParseNull() throws Exception {
        String line1 = "{'log_time':'2019-01-28 00:03:15'," +
            "'span_id':4493870985097058242," +
            "'cid':1234," +
            "'pid':3123," +
            "'bid':43242," +
            "'bs_banner_id':456789," +
            "'order_id':987654," +
            "'type':'banner_resources'," +
            "'data':null}";

        BsExportEssLogParser parser = new BsExportEssLogParser();
        LogParserChecker checker = new LogParserChecker(parser);

        SimpleDateFormat dateTimeFormat = parser.dateTimeFormat;

        String data = "null";
        checker.check(line1,
            dateTimeFormat.parse("2019-01-28 00:03:15"),
            1234L,
            0L,
            "",
            "banner_resources", // type
            3123L,
            43242L,
            456789L,
            987654L,
            "", // host
            "", // meta
            4493870985097058242L,
            data
        );
    }
}

