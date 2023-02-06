package ru.yandex.direct.core.entity.statistics;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.statistics.service.OrderStatService;
import ru.yandex.direct.utils.TimeProvider;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class RestrictToBsStartDateTest {

    @Parameterized.Parameter
    public String dateStr;

    @Parameterized.Parameter(1)
    public String expectedResultStr;

    @Parameterized.Parameters
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {"2019-05-19", "2019-05-19"},
                {"2016-05-19", "2016-05-19"},
                {"2016-04-30", "2016-05-01"},
                {"2016-05-01", "2016-05-01"}
        });
    }

    private static OrderStatService orderStatService;

    @BeforeClass
    public static void setUp() {
        TimeProvider timeProvider = mock(TimeProvider.class);
        when(timeProvider.now()).thenReturn(LocalDateTime.of(2019, 5, 23, 12, 20, 32));
        orderStatService = new OrderStatService(null, null, null, null, null, null, timeProvider);
    }

    @Test
    public void test() {
        LocalDate date = LocalDate.parse(dateStr);
        LocalDate expectedResult = LocalDate.parse(expectedResultStr);
        assertEquals(expectedResult, orderStatService.restrictToBsStatStartDate(date));
    }
}
