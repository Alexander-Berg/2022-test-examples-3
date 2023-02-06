package ru.yandex.direct.core.entity.statistics.repository;


import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.statistics.model.StatisticsDateRange;
import ru.yandex.direct.core.testing.configuration.CoreTest;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@Ignore("Ходит в YT, для ручного запуска")
@CoreTest
@RunWith(Parameterized.class)
public class OrderStatDayRepositoryOrdersFirstLastDayTest {
    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();
    @Parameterized.Parameter
    public Long orderId;
    @Parameterized.Parameter(1)
    public LocalDate expectedMaxUpdateDate;
    @Parameterized.Parameter(2)
    public LocalDate expectedMinUpdateDate;

    @Autowired
    private OrderStatDayRepository orderStatDayRepository;

    @Parameterized.Parameters
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {0L, null, null},   // нет статистики
                {4L, LocalDate.of(2001, 2, 9), LocalDate.of(2001, 1, 25)},   // есть статистика
                {20890424L, LocalDate.of(2018, 9, 21), LocalDate.of(2018, 6, 6)}, // есть статистика
                {24103827L, LocalDate.of(2019, 6, 12), LocalDate.of(2019, 1, 18)} // есть статистика
        });
    }

    @Test
    public void test() {
        Map<Long, StatisticsDateRange> result = orderStatDayRepository.getOrdersFirstLastDay(
                Collections.singleton(orderId));
        if (expectedMaxUpdateDate == null) {
            assertThat(result).isEmpty();
        } else {
            assertThat(result).containsOnlyKeys(orderId);
            StatisticsDateRange expected = new StatisticsDateRange(expectedMinUpdateDate, expectedMaxUpdateDate);
            assertThat(result).containsEntry(orderId, expected);
        }
    }
}
