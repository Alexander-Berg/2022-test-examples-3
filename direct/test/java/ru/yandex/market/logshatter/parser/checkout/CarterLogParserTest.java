package ru.yandex.market.logshatter.parser.checkout;

import com.google.gson.internal.LinkedTreeMap;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.logshatter.parser.LogParserChecker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

public class CarterLogParserTest {
    private LogParserChecker checker;

    @Before
    public void setUp() {
        checker = new LogParserChecker(new CarterLogParser());
    }

    @Test
    public void testOldLogsCompatible() throws Exception {
        checker.check("[18/Apr/2016:09:35:38 +0300]\t1460961338\t93.158.141.25\tPATCH\t/cart/UUID/5a7ae178fcde47e8cb071415db925901/list\t200\t10\t0\t1",
                new Date(1460961338000L), checker.getHost(), "/cart/UUID/5a7ae178fcde47e8cb071415db925901/list", "PATCH", 200, 10,
                /* pageId cannot be tested in tests */ "", 0L, true, "",
                Collections.emptyList(), Collections.emptyList());
    }

    @Test
    public void testMarketRequestIdIsParsedIfPresent() throws Exception {
        checker.check("[18/Apr/2016:09:35:38 +0300]\t1460961338\t93.158.141.32\tPOST\t/cart/UUID/5a7ae178fcde47e8cb071415db925901/list\t200\t185\t0\t1\t135135/deadbeef",
                new Date(1460961338000L), checker.getHost(), "/cart/UUID/5a7ae178fcde47e8cb071415db925901/list", "POST", 200, 185,
                /* pageId cannot be tested in tests*/ "", 0L, true, "135135/deadbeef",
                Collections.emptyList(), Collections.emptyList());
    }

    @Test
    public void shouldParseKeyValueEntry() throws Exception {
        LinkedTreeMap<Object, Object> result = new LinkedTreeMap<>();
        result.put("getOwnerIdYandexUid.miss", "1");
        result.put("total.miss", "1");
        result.put("param.yandexuid", "3287481931526332359");
        result.put("total.hit", "0");

        checker.check("[15/May/2018:00:12:39 +0300]\t" +
                "1526332359\t" +
                "2a02:6b8:c08:a304:10b:5cd2:0:5b86\t" +
                "GET\t" +
                "/cart/YANDEXUID/3287481931526332359/list\t" +
                "200\t" +
                "15\t" +
                "0\t" +
                "1\t" +
                "1526332359336/f79bbd632e2b2254e00964768ae72c02/3\t" +
                "0\t" +
                "15\t" +
                "{" +
                "\"getOwnerIdYandexUid.miss\":1," +
                "\"total.miss\":1," +
                "\"param.yandexuid\":\"3287481931526332359\"," +
                "\"total.hit\":0" +
                "}\n",
                new Date(1526332359000L),
            checker.getHost(),
            "/cart/YANDEXUID/3287481931526332359/list",
            "GET",
            200,
            15,
            "",
            0L,
            true,
            "1526332359336/f79bbd632e2b2254e00964768ae72c02/3",
            result.keySet(),
            new ArrayList<>(result.values())
        );
    }

}
