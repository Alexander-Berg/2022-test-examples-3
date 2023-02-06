package ru.yandex.market.logshatter.parser.checkout;

import com.google.common.collect.Lists;
import com.google.gson.internal.LinkedTreeMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.logshatter.parser.LogParserChecker;
import ru.yandex.market.logshatter.url.Page;
import ru.yandex.market.logshatter.url.PageMatcher;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

/**
 * User: akolotnina
 * Date: 2015-03-11
 * Time: 16:50
 */
public class CheckoutLogParserTest {
    LogParserChecker checker;

    @Before
    public void before() {
        checker = new LogParserChecker(new CheckoutLogParser());
    }

    @Test
    public void testParse() throws Exception {
        checker.check("[11/Mar/2015:15:41:16 +0300]\t1426077676\t127.0.0.1\tGET\t/orders/by-uid/51438745/\t200\t169\t0\t1",
                1426077676, checker.getHost(), "/orders/by-uid", "GET", 200, 169, "", 0, 0, false, "/orders/by-uid/51438745/", Collections.emptyList(), Collections.emptyList(), "", false, "", "");
        checker.check("[11/Mar/2015:16:12:48 +0300]\t1426079568\t127.0.0.1\tGET\t/orders/10301/refunds/12345\t404\t13\t0\t1",
                new Date(1426079568000L), checker.getHost(), "/orders/refunds", "GET", 404, 13, "", 0, 0, false, "/orders/10301/refunds/12345", Collections.emptyList(), Collections.emptyList(), "", false, "", "");
        checker.check("[11/Mar/2015:16:12:48 +0300]\t1426079568\t127.0.0.1\tGET\t/orders/10301/refunds/12345\t404\t13\t0\t1\t1477369754057/263eeed8cd36739dd7acf898f7a683b4",
                new Date(1426079568000L), checker.getHost(), "/orders/refunds", "GET", 404, 13, "1477369754057/263eeed8cd36739dd7acf898f7a683b4", 0, 0, false, "/orders/10301/refunds/12345", Collections.emptyList(), Collections.emptyList(), "", false, "", "");
    }

    @Test
    public void testServiceUrl() throws Exception {
        checker.check("[11/Mar/2015:15:41:16 +0300]\t1426077676\t127.0.0.1\tGET\t/ping\t200\t169\t0\t1",
                1426077676, checker.getHost(), "/ping", "GET", 200, 169, "", 0, 0, true, "/ping", Collections.emptyList(), Collections.emptyList(), "", false, "", "");

        checker.check("[11/Mar/2015:15:41:16 +0300]\t1426077676\t127.0.0.1\tGET\t/monitor/dev/by-group/unable-to-expire\t200\t169\t0\t1",
                1426077676, checker.getHost(), "/monitor/dev/by-group/unable-to-expire", "GET", 200, 169, "", 0, 0, true, "/monitor/dev/by-group/unable-to-expire", Collections.emptyList(), Collections.emptyList(), "", false, "", "");
    }

    @Test
    public void testCheckKeyValueParser() throws Exception {
        // empty case
        checker.check(
                "[11/Aug/2017:15:56:47 +0300]\t1502456207\t2a02:6b8:c0e:106:0:604:dbc:a2bf\tPOST\t/cart\t200\t193\t0\t1\t1502456206385/bd15ad03ec96467a9ac4d8cb33fb2732/5\t73\t120\t{}\n",
                1502456207,
                checker.getHost(), "/cart", "POST", 200, 193, "1502456206385/bd15ad03ec96467a9ac4d8cb33fb2732/5", 73, 120, false, "/cart", new LinkedTreeMap<>().keySet(), Collections.emptyList(), "", false, "", "");
        //single case
        LinkedTreeMap<Object, Object> result = new LinkedTreeMap<>();
        result.put("context", "");
        checker.check(
                "[11/Aug/2017:15:56:47 +0300]\t1502456207\t2a02:6b8:c0e:106:0:604:dbc:a2bf\tPOST\t/cart\t200\t193\t0\t1\t1502456206385/bd15ad03ec96467a9ac4d8cb33fb2732/5\t73\t120\t{\"context\":\"MARKET\"}\n",
                1502456207,
                checker.getHost(), "/cart", "POST", 200, 193, "1502456206385/bd15ad03ec96467a9ac4d8cb33fb2732/5", 73, 120, false, "/cart", result.keySet(), Collections.singletonList("MARKET"),
            "",
            false, "", "");

        LinkedTreeMap<Object, Object> result2 = new LinkedTreeMap<>();
        result2.put("context", "");
        result2.put("global", "");
        //multiple case
        checker.check(
                "[11/Aug/2017:15:56:47 +0300]\t1502456207\t2a02:6b8:c0e:106:0:604:dbc:a2bf\tPOST\t/cart\t200\t193\t0\t1\t1502456206385/bd15ad03ec96467a9ac4d8cb33fb2732/5\t73\t120\t{\"context\":\"MARKET\",\"global\":false}\n",
                1502456207,
                checker.getHost(), "/cart", "POST", 200, 193, "1502456206385/bd15ad03ec96467a9ac4d8cb33fb2732/5", 73, 120, false, "/cart", result2.keySet(), Arrays.asList("MARKET", "false"),
            "",
            false, "", "");

    }

    @Test
    public void testByBindKeyParser() throws Exception {
        LinkedTreeMap<Object, Object> result = new LinkedTreeMap<>();
        result.put("param.userPhone", "+79099955967");
        result.put("param.uid", "363943318");

        checker.check("[15/Feb/2018:09:07:50 +0300]\t1518674870\t213.248.50.30, " +
            "2a02:6b8:0:888:225:90ff:fec8:c900\tGET\t/orders/by-bind-key/3505805.961CF4DB7787CC20CFE6D05C4727EDAF" +
            "\t200\t240\t0\t1\t1518674870234/d1a1e471f2e10a55c08d8aeb20fc5306/3\t0\t240\t{\"param" +
            ".userPhone\":\"+79099955967\",\"param.uid\":\"363943318\"}",
            1518674870,
            checker.getHost(),
            "/orders/by-bind-key",
            "GET",
            200,
            240,
            "1518674870234/d1a1e471f2e10a55c08d8aeb20fc5306/3",
            0,
            240,
            false,
            "/orders/by-bind-key/3505805.961CF4DB7787CC20CFE6D05C4727EDAF",
            result.keySet(),
            Lists.newArrayList(result.values()),
            "",
            false,
            "",
            ""
        );
    }

    @Test
    public void testPlaneshiftParser() throws Exception {
        LinkedTreeMap<Object, Object> result = new LinkedTreeMap<>();
        result.put("market.environment.profile", "planeshift");

        checker.check("[15/Feb/2018:09:07:50 +0300]\t1518674870\t213.248.50.30, " +
                "2a02:6b8:0:888:225:90ff:fec8:c900\tGET\t/orders/by-bind-key/3505805.961CF4DB7787CC20CFE6D05C4727EDAF" +
                "\t200\t240\t0\t1\t1518674870234/d1a1e471f2e10a55c08d8aeb20fc5306/3\t0\t240\t{\"market.environment" +
                ".profile\": \"planeshift\"}",
            1518674870,
            checker.getHost(),
            "/orders/by-bind-key",
            "GET",
            200,
            240,
            "1518674870234/d1a1e471f2e10a55c08d8aeb20fc5306/3",
            0,
            240,
            false,
            "/orders/by-bind-key/3505805.961CF4DB7787CC20CFE6D05C4727EDAF",
            result.keySet(),
            Lists.newArrayList(result.values()),
            "",
            false,
            "",
            "planeshift"
        );
    }

    @Test
    public void testPaymentMarkupParser() throws Exception {
        LinkedTreeMap<Object, Object> result = new LinkedTreeMap<>();
        result.put("header.Accept", "*/*");
        result.put("header.Content-Type", "application/json");
        result.put("header.User-Agent", "PycURL/7.19.5.3 libcurl/7.35.0 OpenSSL/1.0.1f zlib/1.2.8 libidn/1.28 librtmp/2.3");
        result.put("header.X-Forwarded-For", "2a02:6b8:c04:191:0:627:8619:5947, 2a02:6b8:c04:173:0:577:5220:4711");
        result.put("header.X-Real-IP", "2a02:6b8:c04:191:0:627:8619:5947");
        result.put("header.X-Market-Req-ID", "1559129477285/c3ecb6324b6ad1100c6389f9f3ef7ff4");
        result.put("header.Host", "checkouter.market.http.yandex.net");
        result.put("header.Content-Length", "226");
        result.put("header.Accept-Encoding", "gzip");
        result.put("header.Connection", "close");

        checker.check("[29/May/2019:14:31:18 +0300]\t1559129478\t2a02:6b8:c04:191:0:627:8619:5947, " +
            "2a02:6b8:c04:173:0:577:5220:4711\tPOST\t/payments/bc6be0a5d33b5adb1444e392c0c514a3/markup\t200\t1058\t0" +
            "\t1\t1559129477285/c3ecb6324b6ad1100c6389f9f3ef7ff4\t0\t1058\t{\"header.Accept\":\"*/*\",\"header" +
            ".Content-Type\":\"application/json\",\"header.User-Agent\":\"PycURL/7.19.5.3 libcurl/7.35.0 OpenSSL/1.0" +
            ".1f zlib/1.2.8 libidn/1.28 librtmp/2.3\",\"header.X-Forwarded-For\":\"2a02:6b8:c04:191:0:627:8619:5947, " +
            "2a02:6b8:c04:173:0:577:5220:4711\",\"header.X-Real-IP\":\"2a02:6b8:c04:191:0:627:8619:5947\",\"header" +
            ".X-Market-Req-ID\":\"1559129477285/c3ecb6324b6ad1100c6389f9f3ef7ff4\",\"header.Host\":\"checkouter" +
            ".market.http.yandex.net\",\"header.Content-Length\":\"226\",\"header.Accept-Encoding\":\"gzip\",\"header" +
            ".Connection\":\"close\"}",
            1559129478,
            checker.getHost(),
            "/payments/markup",
            "POST",
            200,
            1058,
            "1559129477285/c3ecb6324b6ad1100c6389f9f3ef7ff4",
            0,
            1058,
            false,
            "/payments/bc6be0a5d33b5adb1444e392c0c514a3/markup",
            result.keySet(),
            Lists.newArrayList(result.values()),
            "",
            false,
            "",
            ""
        );

    }

    @Test
    public void testVipCheckout() throws Exception {
        String log = "[15/Feb/2018:09:07:50 +0300]\t1518674870\t213.248.50.30, " +
            "2a02:6b8:0:888:225:90ff:fec8:c900\tGET\t/orders/by-bind-key/3505805.961CF4DB7787CC20CFE6D05C4727EDAF" +
            "\t200\t240\t0\t1\t1518674870234/d1a1e471f2e10a55c08d8aeb20fc5306/3\t0\t240\t{\"param" +
            ".userPhone\":\"+79099955967\",\"param.uid\":\"41\"}";

        LinkedTreeMap<Object, Object> result = new LinkedTreeMap<>();
        result.put("param.userPhone", "+79099955967");
        result.put("param.uid", "41");

        checker.check(log,
            1518674870,
            checker.getHost(),
            "/orders/by-bind-key",
            "GET",
            200,
            240,
            "1518674870234/d1a1e471f2e10a55c08d8aeb20fc5306/3",
            0,
            240,
            false,
            "/orders/by-bind-key/3505805.961CF4DB7787CC20CFE6D05C4727EDAF",
            result.keySet(),
            Lists.newArrayList(result.values()),
            "volozh",
            true,
            "",
            ""
        );
    }

    @Test
    public void testPageId() throws Exception {
        PageMatcher pageMatcher = Mockito.mock(PageMatcher.class);
        Mockito.when(pageMatcher.matchUrl("checkouter.market.http.yandex.net", "POST", "/checkout"))
            .thenReturn(new Page("checkout", "POST"));

        String log = "[15/May/2019:07:03:48 +0300]\t1557893028\t2a02:6b8:c0c:3da7:0:1406:c66a:2f5e, " +
            "2a02:6b8:c04:1a7:0:633:2d19:3434\tPOST\t/checkout\t200\t2137\t0\t1\t1557893026042" +
            "/4c81d3c48601b07bd061133443b42380\t687\t1450\t{}";

        LogParserChecker localChecker = new LogParserChecker(new CheckoutLogParser(), pageMatcher);

        localChecker.check(log,
            1557893028,
            checker.getHost(),
            "/checkout",
            "POST",
            200,
            2137,
            "1557893026042/4c81d3c48601b07bd061133443b42380",
            687,
            1450,
            false,
            "/checkout",
            Collections.emptySet(),
            Collections.emptyList(),
            "",
            false,
            "checkout",
            ""
        );
    }
}
