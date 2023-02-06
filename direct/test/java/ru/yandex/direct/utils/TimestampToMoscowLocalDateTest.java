package ru.yandex.direct.utils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static ru.yandex.direct.utils.DateTimeUtils.instantToMoscowDate;

@RunWith(Parameterized.class)
public class TimestampToMoscowLocalDateTest {

    @Parameterized.Parameter
    public Long timestamp;

    @Parameterized.Parameter(1)
    public LocalDate expectedLocalDate;

    @Parameterized.Parameters()
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {1556139600L, LocalDate.of(2019, 4, 25)},
                {1526850000L, LocalDate.of(2018, 5, 21)},
                {1526850001L, LocalDate.of(2018, 5, 21)},
                {1526849999L, LocalDate.of(2018, 5, 20)},
                {1557964800L, LocalDate.of(2019, 5, 16)},
                {1557964799L, LocalDate.of(2019, 5, 16)},
                {1557964801L, LocalDate.of(2019, 5, 16)},
        });
    }

    @Test
    public void test() {
        Instant instant = Instant.ofEpochSecond(timestamp);
        assertEquals(expectedLocalDate, instantToMoscowDate(instant));
    }
}
