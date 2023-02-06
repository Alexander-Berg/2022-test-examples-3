package ru.yandex.direct.core.entity.statistics.repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.utils.DateTimeUtils;
import ru.yandex.direct.utils.TimeProvider;
import ru.yandex.direct.ytcomponents.service.OrderStatDayDynContextProvider;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Ignore("Ходит в YT, для ручного запуска")
@CoreTest
@RunWith(Parameterized.class)
public class OrderStatDayRepositoryOrderSessionsNumSumTest {

    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();
    @Parameterized.Parameter
    public Long orderId;
    @Parameterized.Parameter(1)
    public String nowStr;
    @Parameterized.Parameter(2)
    public Long expectedSessionsNumSum;
    @Parameterized.Parameter(3)
    public int minusDays;
    @Autowired
    private OrderStatDayDynContextProvider dynContextProvider;
    private OrderStatDayRepository orderStatDayRepository;

    @Parameterized.Parameters
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {0L, "2019-05-20T00:00:00", null, 7},        // вообще нет статистики
                {4L, "2001-02-09T00:00:00", 0L, 7},          // есть статистика, нулевая
                {22138303L, "2019-05-29T00:00:00", null, 7}, // есть статистика, все записи null
                {20890424L, "2018-06-13T00:00:00", 3L, 7},   // есть ненулевая статистика за семь дней
                {20890424L, "2018-06-13T00:00:00", 1L, 3},   // есть ненулевая статистика за три дня
                {24103827L, "2019-02-20T23:00:00", 624L, 7}, // считаем статистику с 13 февраля 2019 г., 23:00:00
                {24103827L, "2019-02-20T23:00:00", 590L, 2}, // считаем статистику с 18 февраля 2019 г., 23:00:00
                {24103827L, "2019-02-21T01:00:00", 587L, 2}, // считаем статистику с 19 февраля 2019 г., 1:00:00
        });
    }

    @Before
    public void setUp() {
        TimeProvider timeProvider = mock(TimeProvider.class);
        Instant instantNow = LocalDateTime.parse(nowStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                .atZone(DateTimeUtils.MSK).toInstant();
        when(timeProvider.instantNow()).thenReturn(instantNow);
        orderStatDayRepository = new OrderStatDayRepository(dynContextProvider, timeProvider);
    }

    @Test
    public void test() {
        Map<Long, Long> result = orderStatDayRepository.getOrdersSessionsNumSum(
                Collections.singleton(orderId), minusDays);
        if (expectedSessionsNumSum == null) {
            assertThat(result).isEmpty();
        } else {
            assertThat(result).containsOnlyKeys(orderId);
            assertThat(result).containsEntry(orderId, expectedSessionsNumSum);
        }
    }
}
