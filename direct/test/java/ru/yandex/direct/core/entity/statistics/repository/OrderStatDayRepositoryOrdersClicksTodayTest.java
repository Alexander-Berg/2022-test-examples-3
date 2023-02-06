package ru.yandex.direct.core.entity.statistics.repository;

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
public class OrderStatDayRepositoryOrdersClicksTodayTest {
    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();
    @Parameterized.Parameter
    public Long orderId;
    @Parameterized.Parameter(1)
    public String nowStr;
    @Parameterized.Parameter(2)
    public Long expectedClicksSum;
    @Autowired
    private OrderStatDayDynContextProvider dynContextProvider;
    private OrderStatDayRepository orderStatDayRepository;

    @Parameterized.Parameters
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {0L, "2019-05-20T00:00:00", null},      // вообще нет статистики
                {20890424L, "2018-06-06T00:00:00", 0L}, // есть статистика, нулевая
                {20890424L, "2018-06-07T00:00:00", 1L}, // есть ненулевая статистика
                {20890424L, "2018-06-08T00:00:00", 1L}, // есть ненулевая статистика
                {20890424L, "2018-06-09T00:00:00", 0L}, // есть статистика, нулевая
                {24103827L, "2019-02-20T00:00:00", 5L}, // считаем статистику с 20 февраля 2019 г., 00:00:00
                {24103827L, "2019-02-20T23:59:59", 5L}, // считаем статистику с 20 февраля 2019 г., 23:59:59
        });
    }

    @Before
    public void setUp() {
        TimeProvider timeProvider = mock(TimeProvider.class);
        LocalDateTime now = LocalDateTime.parse(nowStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                .atZone(DateTimeUtils.MSK).toLocalDateTime();
        when(timeProvider.now()).thenReturn(now);
        orderStatDayRepository = new OrderStatDayRepository(dynContextProvider, timeProvider);
    }

    @Test
    public void test() {
        Map<Long, Long> result = orderStatDayRepository.getOrdersClicksToday(
                Collections.singleton(orderId));
        if (expectedClicksSum == null) {
            assertThat(result).isEmpty();
        } else {
            assertThat(result).containsOnlyKeys(orderId);
            assertThat(result).containsEntry(orderId, expectedClicksSum);
        }
    }
}
