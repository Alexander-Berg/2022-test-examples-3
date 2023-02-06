package ru.yandex.market.logshatter.parser.checkout;

import com.google.gson.internal.LinkedTreeMap;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.logshatter.parser.LogParserChecker;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * @author mmetlov
 */
public class NotifierIncomingKvLogParserTest {
    LogParserChecker checker;

    @Before
    public void before() {
        checker = new LogParserChecker(new NotifierIncomingKvLogParser());
    }

    @Test
    public void testParse() throws Exception {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2019, 0, 24, 12, 22, 43);
        calendar.set(Calendar.MILLISECOND, 0);

        LinkedTreeMap<Object, Object> result = new LinkedTreeMap<>();
        result.put("checkouterEventId", "172130722");
        result.put("firstBucketId", "48");

        checker.check("24/Jan/2019:12:22:43 +0300\t{\"checkouterEventId\":\"172130722\",\"firstBucketId\":\"48\"}",
            calendar.getTime(), result.keySet(), new ArrayList<>(result.values()), "hostname.test");
    }
}
