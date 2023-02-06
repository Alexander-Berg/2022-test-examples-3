package ru.yandex.market.logshatter.parser.direct;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

public class AccessLogParserTest {
    private LogParserChecker checker;
    private SimpleDateFormat dateFormat;

    @Test
    public void testParse() throws Exception {
        String line = "46.28.224.54\t-\t[19/Oct/2018:10:50:41 +0300]\t" +
            "\"GET https://avatars.mds.yandex.net/get-media-adv-screenshooter/41244/" +
            "414bfbd2-59ef-4d1e-8b99-6faabaee25a0/orig HTTP/1.1\"\t499\t0\t" +
            "\"https://yastatic.net/pcode/media/frame-api.html?id=uniq167&isDirect=true\"\t" +
            "\"Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/68.0.3440.106 YaBrowser/18.9.1.954 Yowser/2.5 Safari/537.36\"\t" +
            "\"direct.yandex.ru,443\"\t0.001 : -:0.028\treqid:8512270078406555338,cmd:Cmd/showCampStat\tTLSv1.2\tECDHE-RSA-AES128-GCM-SHA256";

        LogParserChecker checker = new LogParserChecker(new AccessLogParser());
        dateFormat = new SimpleDateFormat(AccessLogParser.DATE_PATTERN);
        checker.check(line,
            dateFormat.parse("[19/Oct/2018:10:50:41 +0300]"),
            "46.28.224.54", "", "GET",
            "https://avatars.mds.yandex.net/get-media-adv-screenshooter/41244/414bfbd2-59ef-4d1e-8b99-6faabaee25a0/orig",
            "HTTP/1.1", 499, 0, "https://yastatic.net/pcode/media/frame-api.html?id=uniq167&isDirect=true",
            "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/68.0.3440.106 YaBrowser/18.9.1.954 Yowser/2.5 Safari/537.36",
            "direct.yandex.ru", 443, new ArrayList<Float>(Arrays.asList(Float.valueOf("0.001"), Float.valueOf("0"))),
            Float.valueOf("0.028"),
            // upstream accel info
            8512270078406555338L, "Cmd", "showCampStat", emptyList(), emptyList(),
            "TLSv1.2", "ECDHE-RSA-AES128-GCM-SHA256", "", "hostname.test"
        );
    }

    @Test
    public void testParse2() throws Exception {
        String line = "213.180.204.26\t-\t[19/Oct/2018:06:25:16 +0300]\t" +
            "\"GET /admin?action=version HTTP/1.1\"\t404\t16276\t\"-\"\t\"deploy-consistency-monitor\"\t" +
            "\"direct.yandex.ru,443\"\t0.000 : 0.070:0.070\t-\tTLSv1.2\tECDHE-RSA-AES128-GCM-SHA256\tdirect.yandex.ru";
        LogParserChecker checker = new LogParserChecker(new AccessLogParser());
        dateFormat = new SimpleDateFormat(AccessLogParser.DATE_PATTERN);
        checker.check(line,
            dateFormat.parse("[19/Oct/2018:06:25:16 +0300]"),
            "213.180.204.26", "", "GET", "/admin?action=version", "HTTP/1.1",
            404, 16276, "", "deploy-consistency-monitor", "direct.yandex.ru", 443,
            new ArrayList<Float>(Arrays.asList(Float.valueOf("0.000"), Float.valueOf("0.070"))),
            Float.valueOf("0.070"),
            // upstream accel info
            0L, "", "", emptyList(), emptyList(),
            "TLSv1.2", "ECDHE-RSA-AES128-GCM-SHA256", "direct.yandex.ru", "hostname.test"
        );
    }

    @Test
    public void testParse3() throws Exception {
        String line = "::1\t-\t[19/Oct/2018:06:25:17 +0300]\t\"GET /alive HTTP/1.1\"\t200\t12\t\"-\"\t" +
            "\"Wget/1.15 (linux-gnu)\"\t\"localhost,80\"\t0.012:0.013\t"
            + "reqid:8512272755860171199,cmd:direct.json-api/changes.checkCampaigns,appcode:0\t-\t-";
        LogParserChecker checker = new LogParserChecker(new AccessLogParser());
        dateFormat = new SimpleDateFormat(AccessLogParser.DATE_PATTERN);
        checker.check(line,
            dateFormat.parse("[19/Oct/2018:06:25:17 +0300]"),
            "::1", "", "GET", "/alive", "HTTP/1.1", 200, 12, "", "Wget/1.15 (linux-gnu)",
            "localhost", 80, new ArrayList<Float>(Arrays.asList(Float.valueOf("0.012"))), Float.valueOf("0.013"),
            // upstream accel info
            8512272755860171199L, "direct.json-api", "changes.checkCampaigns", singletonList("appcode"),
            singletonList("0"),
            "", "", "", "hostname.test"
        );
    }

    @Test
    public void testParse4() throws Exception {
        String line = "\t-\t[17/Nov/2018:00:25:11 +0300]\t\"GET /tvm2_public_keys?lib_version=2.0.5 HTTP/1.0\"\t200" +
            "\t0\t\"-\"\t\"-\"\t\"-,-\"\t0.037:0.037\treqid:8512272755860171199,cmd:changes.checkCampaigns,appcode:0,data:rrr,emp:\t-\t-\tdirect.yandex.ru";
        LogParserChecker checker = new LogParserChecker(new AccessLogParser());
        dateFormat = new SimpleDateFormat(AccessLogParser.DATE_PATTERN);
        checker.check(line,
            dateFormat.parse("[17/Nov/2018:00:25:11 +0300]"),
            "", "", "GET", "/tvm2_public_keys?lib_version=2.0.5", "HTTP/1.0", 200, 0, "", "",
            "", 0, new ArrayList<Float>(Arrays.asList(Float.valueOf("0.037"))), Float.valueOf("0.037"),
            // upstream accel info
            8512272755860171199L, "", "changes.checkCampaigns",
            Arrays.asList("appcode", "data", "emp"), Arrays.asList("0", "rrr", ""),
            "", "", "direct.yandex.ru", "hostname.test"
        );
    }
}
