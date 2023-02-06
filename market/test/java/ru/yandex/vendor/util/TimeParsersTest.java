package ru.yandex.vendor.util;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.time.LocalDateTime.of;
import static org.junit.Assert.assertEquals;
import static ru.yandex.cs.billing.util.TimeUtil.UTC;
import static ru.yandex.vendor.util.Utils.entry;
import static ru.yandex.vendor.util.Utils.map;

@RunWith(JUnit4.class)
public class TimeParsersTest {

    @Test
    public void test_instant_parsing_gives_expected_result() throws Exception {

        int year = 2020;
        int month = 2;
        int dayOfMonth = 22;
        int hour = 22;
        int minute = 22;
        int second = 22;
        int nanoOfSecond = 20000000;

        Map<String, LocalDateTime> expectancyMap = map(LinkedHashMap::new,
            entry("2020-02-22T22:22:22.020Z", of(year, month, dayOfMonth, hour, minute, second, nanoOfSecond)),
            entry("2020-02-22T22:22:22.02Z", of(year, month, dayOfMonth, hour, minute, second, nanoOfSecond)),
            entry("2020-02-22T22:22:22Z", of(year, month, dayOfMonth, hour, minute, second))
        );

        for (Map.Entry<String, LocalDateTime> e : expectancyMap.entrySet()) {
            Instant parsedInstant = TimeParsers.parseInstant(e.getKey());
            Instant expectedInstant = e.getValue().atZone(UTC).toInstant();
            assertEquals(expectedInstant, parsedInstant);
        }
    }

    @Test
    public void test_instant_parsing_fails_on_unexpected_strings() throws Exception {

        List<String> badStrings = Arrays.asList(
            "2020-02-22T22:22:22.000",
            "2020-02-22 22:22:22.000Z",
            "2020-02-22T22:22:22",
            "2020-02-22 22:22:22Z",
            "2020-02-22T22:22Z",
            "2020-02-22T22:22",
            "2020-02-22 22:22Z",
            "2020-02-22 22:22",
            "2020-02-22Z",
            "2020-02-22T",
            "2020-02-22"
        );

        for (String badString : badStrings) {
            try {
                TimeParsers.parseInstant(badString);
                Assert.fail("Exception expected for string: " + badString);
            } catch (Exception e) {
                // expected
            }
        }
    }
}
