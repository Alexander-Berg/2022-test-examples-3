package ru.yandex.market.logshatter.parser.front;

import junit.framework.Assert;
import org.junit.Test;
import ru.yandex.market.logshatter.parser.LogParserChecker;

import java.util.Date;

public class PepelacNginxLogParserTest {

    @Test
    public void testParse() throws Exception {
        LogParserChecker checker = new LogParserChecker(new PepelacNginxLogParser());
//Mozilla/5.0

        checker.check(
            "[10/Apr/2016:23:43:15 +0300] market.yandex.ru 2a02:6b8:0:1a31:7198:78e2:9029:af6f \"GET /common.hid/resources/product/getForumCount.xml?forumID=root-6-0-10381609 HTTP/1.1\" 200 \"https://market.yandex.ru/product--pioneer-n-30/10381609\" \"Mozilla/5.0 (Windows NT 6.1; WOW64; rv:45.0) Gecko/20100101 Firefox/45.0\" \"yandexuid=243183051454136212; yp=1469910387.szm.1:1280x1024:1280x887#1467916131.ww.1#1462732131.ygu.1#1462732142.los.1; fuid01=56ac5bf05f5e4ee7.pe-Vfs8-2T3vlMWutzDvskKJrWPtolklchblCIyI-M4pwgffjMEHNgteYQ5LKiXWjGKHw8mRypCuxl4ueDxkIUH0o8koZgVfPhQQ7wsoX2Tc2y7819Jg6peFe1y3501N; yabs-frequency=/4/0G0002TSh5O00000/4aUmSCWZ8G00/; ps_gch=7265903975381601280; yandexmarket=10,RUR,1,,,,2,0,0; deliveryincluded=false; yandex_gid=2; zm=m-white_bender.flex.css-https%3Awww_tvzhensX6C-PUhctl-TiXMV1WfQ%3Al; _ym_uid=14601401521029108454; _ym_isad=1; mxp=phones-new2|20326|23283,0,4%3B23350,0,22%3B20326,0,76%3B15093,0,94|market_redirect_black_words%253D1; uid=CmvdF1cKtvq3BH7tA4rbAg==; parent_reqid_seq=e711c6e2e732536ee7f3b16015090662%2Cf598db64a4162772e4b048ef1afec9c5; cuts=0; HISTORY_UNAUTH_SESSION=true; _ym_visorc_160656=b; ps_bch=132139746330893440\" ee3144048f010f5f4fe74c9bbd1b13d7 \"0.110\" 0.110 698 \"-\" https \"-\" \"-\" - \"-\" 0.001 \"unix:/tmp/xscript-market.sock\" \"-\" \"0.110\" \"200\"",
            new Date(1460320995000L),
            checker.getHost(),
            "market.yandex.ru",
            "/common.hid/resources/product/getForumCount.xml?forumID=root-6-0-10381609",
            "GET",
            200,
            110,
            true,
            "",
            "",
            "",
            "2a02:6b8:0:1a31:7198:78e2:9029:af6f",
            "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:45.0) Gecko/20100101 Firefox/45.0",
            new Object[]{},
            "243183051454136212",
            "",
            110,
            1
        );

        checker.check(
            "[23/Jan/2015:07:56:11 +0300] - 62.78.82.8 \"5\" 400 \"-\" \"-\" \"-\" bae3c113b026167968e39461979bba711421988971.081 \"-\" 0.057 361 \"-\"  http \"touch\"",
            null, null
        );
//        checker.check("[22/Jan/2015:14:12:47 +0300] market.yandex.ru 93.116.28.33 \"GET /prefetch.txt HTTP/1.1\" 304 \"-\" \"Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/38.0.2125.122 YaBrowser/14.12.2125.9579 Safari/537.36\" \"fuid01=548f146728e6796d.lUpz02YDIHn3L8Qr4jq5FLG2Nc80R6TNijGlRuvb4qcHMhruEC51o-CT-UHthpFHBU0niFOExTYHCXjY7NIUGJ2zfRd_35etBqd4yCb-61jw4-KphK379RWr3UXE22fv; yandexuid=7386949011418662734; yp=1737284318.yb.14_12_2125_9142:1955450:1418663070:1421924318:47#1422183505.clh.1955452; ys=def_bro.0\" 1a3f6437b7fdb9cda4fc90274b07fc371421925167.136 \"-\" 0.000 298 \"-\"\n");
//        checker.check("[30/Jan/2015:21:13:17 +0300] market.yandex.ru 87.250.232.231 \"GET /market2/gate.hid/PersGrade/getExtractedFacts.xml?modelid=10667584 HTTP/1.1\" 200 \"-\" \"-\" \"-\" dba5d8e6422fc24a12a5ddcbb151e32a1422641597.290 \"0.024\" 0.024 648 \"-\"");

        checker.check(
            "[24/Mar/2015:05:35:24 +0300] m.market.yandex.ru 37.23.230.177 \"GET /category?hid=91491&CAT_ID=160043&CMD=-RR=9,0,0,0-VIS=8070-CAT_ID=160043-EXC=1-PG=10 HTTP/1.1\" " +
                "200 \"referer\" \"userAgent\" \"fuid01=548f146728e6796d.lUpz02YDIHn3L8Qr4jq5FLG2Nc80R6TNijGlRuvb4qcHMhruEC51o-CT-UHthpFHBU0niFOExTYHCXjY7NIUGJ2zfRd_35etBqd4yCb-61jw4-KphK379RWr3UXE22fv; yandexuid=7386949011418662734; yp=1737284318.yb.14_12_2125_9142:1955450:1418663070:1421924318:47#1422183505.clh.1955452; ys=def_bro.0\" bae3c113b026167968e39461979bba711421988971 \"1.068\" 1.202 64441 \"aaa\" http \"touch\"",
            new Date(1427164524000L),
            checker.getHost(),
            "m.market.yandex.ru",
            "/category?hid=91491&CAT_ID=160043&CMD=-RR=9,0,0,0-VIS=8070-CAT_ID=160043-EXC=1-PG=10",
            "GET",
            200,
            1068, // respTimeMillis
            true, // dynamic
            "", // pageId
            "", // pageType
            "touch", // service
            "37.23.230.177", // clientIp
            "userAgent", // userAgent
            new Object[]{}, // testId array
            "7386949011418662734",
            "",
            1202,
            0
        );
    }

    @Test
    public void testDynamic() throws Exception {
        dynamic(false, "/market2/i/_ymm-DEPOT.png");
        dynamic(false, "/_/QdTFnPWqjjfxoCVJyNa-EuiX8tU.svg");
        dynamic(true, "/model.xml?modelid=7940466&hid=396900");
        dynamic(false, "/touch/mvc/modules/params.js?_=0.941268760478124");
    }

    private void dynamic(boolean expected, String url) {
        Assert.assertEquals(expected, PepelacNginxLogParser.isDynamic(null, url));
    }
}