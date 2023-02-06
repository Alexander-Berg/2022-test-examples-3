package ru.yandex.market.mboc.common.masterdata.parsing;

import java.util.Arrays;
import java.util.Collection;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.market.mboc.common.masterdata.model.TimeInUnits;
import ru.yandex.market.mboc.common.masterdata.parsing.utils.TimeInUnitsConverter;

/**
 * @author dmserebr
 * @date 30/01/2019
 */
@SuppressWarnings({"checkstyle:magicNumber", "checkstyle:noWhitespaceAfter"})
@RunWith(Parameterized.class)
public class TimeInUnitsRenderingTest {
    private int timeInt;
    private TimeInUnits.TimeUnit timeUnit;
    private String result;

    public TimeInUnitsRenderingTest(int timeInt, TimeInUnits.TimeUnit timeUnit, String result) {
        this.timeInt = timeInt;
        this.timeUnit = timeUnit;
        this.result = result;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
            {0, TimeInUnits.TimeUnit.HOUR, "0 часов"},
            {1, TimeInUnits.TimeUnit.HOUR, "1 час"},
            {2, TimeInUnits.TimeUnit.HOUR, "2 часа"},
            {3, TimeInUnits.TimeUnit.HOUR, "3 часа"},
            {4, TimeInUnits.TimeUnit.HOUR, "4 часа"},
            {5, TimeInUnits.TimeUnit.HOUR, "5 часов"},
            {10, TimeInUnits.TimeUnit.HOUR, "10 часов"},
            {11, TimeInUnits.TimeUnit.HOUR, "11 часов"},
            {19, TimeInUnits.TimeUnit.HOUR, "19 часов"},
            {20, TimeInUnits.TimeUnit.HOUR, "20 часов"},
            {21, TimeInUnits.TimeUnit.HOUR, "21 час"},
            {22, TimeInUnits.TimeUnit.HOUR, "22 часа"},
            {25, TimeInUnits.TimeUnit.HOUR, "25 часов"},
            {100, TimeInUnits.TimeUnit.HOUR, "100 часов"},

            {0, TimeInUnits.TimeUnit.DAY, "0 дней"},
            {1, TimeInUnits.TimeUnit.DAY, "1 день"},
            {2, TimeInUnits.TimeUnit.DAY, "2 дня"},
            {3, TimeInUnits.TimeUnit.DAY, "3 дня"},
            {4, TimeInUnits.TimeUnit.DAY, "4 дня"},
            {5, TimeInUnits.TimeUnit.DAY, "5 дней"},
            {10, TimeInUnits.TimeUnit.DAY, "10 дней"},
            {11, TimeInUnits.TimeUnit.DAY, "11 дней"},
            {19, TimeInUnits.TimeUnit.DAY, "19 дней"},
            {20, TimeInUnits.TimeUnit.DAY, "20 дней"},
            {21, TimeInUnits.TimeUnit.DAY, "21 день"},
            {22, TimeInUnits.TimeUnit.DAY, "22 дня"},
            {25, TimeInUnits.TimeUnit.DAY, "25 дней"},
            {100, TimeInUnits.TimeUnit.DAY, "100 дней"},

            {0, TimeInUnits.TimeUnit.WEEK, "0 недель"},
            {1, TimeInUnits.TimeUnit.WEEK, "1 неделя"},
            {2, TimeInUnits.TimeUnit.WEEK, "2 недели"},
            {3, TimeInUnits.TimeUnit.WEEK, "3 недели"},
            {4, TimeInUnits.TimeUnit.WEEK, "4 недели"},
            {5, TimeInUnits.TimeUnit.WEEK, "5 недель"},
            {10, TimeInUnits.TimeUnit.WEEK, "10 недель"},
            {11, TimeInUnits.TimeUnit.WEEK, "11 недель"},
            {19, TimeInUnits.TimeUnit.WEEK, "19 недель"},
            {20, TimeInUnits.TimeUnit.WEEK, "20 недель"},
            {21, TimeInUnits.TimeUnit.WEEK, "21 неделя"},
            {22, TimeInUnits.TimeUnit.WEEK, "22 недели"},
            {25, TimeInUnits.TimeUnit.WEEK, "25 недель"},
            {100, TimeInUnits.TimeUnit.WEEK, "100 недель"},

            {0, TimeInUnits.TimeUnit.MONTH, "0 месяцев"},
            {1, TimeInUnits.TimeUnit.MONTH, "1 месяц"},
            {2, TimeInUnits.TimeUnit.MONTH, "2 месяца"},
            {3, TimeInUnits.TimeUnit.MONTH, "3 месяца"},
            {4, TimeInUnits.TimeUnit.MONTH, "4 месяца"},
            {5, TimeInUnits.TimeUnit.MONTH, "5 месяцев"},
            {10, TimeInUnits.TimeUnit.MONTH, "10 месяцев"},
            {11, TimeInUnits.TimeUnit.MONTH, "11 месяцев"},
            {19, TimeInUnits.TimeUnit.MONTH, "19 месяцев"},
            {20, TimeInUnits.TimeUnit.MONTH, "20 месяцев"},
            {21, TimeInUnits.TimeUnit.MONTH, "21 месяц"},
            {22, TimeInUnits.TimeUnit.MONTH, "22 месяца"},
            {25, TimeInUnits.TimeUnit.MONTH, "25 месяцев"},
            {100, TimeInUnits.TimeUnit.MONTH, "100 месяцев"},

            {0, TimeInUnits.TimeUnit.YEAR, "0 лет"},
            {1, TimeInUnits.TimeUnit.YEAR, "1 год"},
            {2, TimeInUnits.TimeUnit.YEAR, "2 года"},
            {3, TimeInUnits.TimeUnit.YEAR, "3 года"},
            {4, TimeInUnits.TimeUnit.YEAR, "4 года"},
            {5, TimeInUnits.TimeUnit.YEAR, "5 лет"},
            {10, TimeInUnits.TimeUnit.YEAR, "10 лет"},
            {11, TimeInUnits.TimeUnit.YEAR, "11 лет"},
            {19, TimeInUnits.TimeUnit.YEAR, "19 лет"},
            {20, TimeInUnits.TimeUnit.YEAR, "20 лет"},
            {21, TimeInUnits.TimeUnit.YEAR, "21 год"},
            {22, TimeInUnits.TimeUnit.YEAR, "22 года"},
            {25, TimeInUnits.TimeUnit.YEAR, "25 лет"},
            {100, TimeInUnits.TimeUnit.YEAR, "100 лет"},

            {0, TimeInUnits.TimeUnit.UNLIMITED, "не ограничен"},
            {1, TimeInUnits.TimeUnit.UNLIMITED, "не ограничен"},
            {9685, TimeInUnits.TimeUnit.UNLIMITED, "не ограничен"}
        });
    }

    @Test
    public void test() {
        Assertions.assertThat(TimeInUnitsConverter.convertToStringRussian(
            new TimeInUnits(timeInt, timeUnit)
        )).isEqualTo(result);
    }
}
