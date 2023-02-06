package ru.yandex.direct.utils;

import java.time.LocalDate;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static ru.yandex.direct.utils.DateTimeUtils.startOfDayInMoscowTime;

@RunWith(Parameterized.class)
public class StartOfDayInMoscowTimeTest {

    @Parameterized.Parameter
    public Long expectedTimestamp;

    @Parameterized.Parameter(1)
    public LocalDate date;

    @Parameterized.Parameters()
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {1577826000L, LocalDate.of(2020, 1, 1)},
                {1526850000L, LocalDate.of(2018, 5, 21)},
                {1553979600L, LocalDate.of(2019, 3, 31)},
                {1577739600L, LocalDate.of(2019, 12, 31)},
        });
    }

    @Test
    public void test() {
        assertEquals(expectedTimestamp, startOfDayInMoscowTime(date));
    }
}
