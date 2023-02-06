package ru.yandex.direct.core.entity.statistics.repository;

import java.time.LocalDate;
import java.util.Collection;

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

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Ignore("Ходит в YT, для ручного запуска")
@CoreTest
@RunWith(Parameterized.class)
public class OrderStatDayRepositoryOrdersDaysNumTest {

    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();
    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();
    @Parameterized.Parameter
    public Long orderId;
    @Parameterized.Parameter(1)
    public LocalDate startDate;
    @Parameterized.Parameter(2)
    public LocalDate endDate;
    @Parameterized.Parameter(3)
    public Long expectedDays;

    @Autowired
    private OrderStatDayRepository orderStatDayRepository;

    @Parameterized.Parameters
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                // нет дат
                {0L, null, null, null},
                // нет начальной даты
                {0L, LocalDate.of(2001, 2, 9), null, null},
                // нет конечной даты
                {0L, null, LocalDate.of(2001, 2, 9), null},
                // даты совпадают, но статистики нет
                {0L, LocalDate.of(2001, 2, 9), LocalDate.of(2001, 2, 9), 0L},
                // конечная меньше начальной, но статистики нет
                {0L, LocalDate.of(2001, 1, 25), LocalDate.of(2001, 2, 9), 0L},
                // нулевая статистика
                {4L, LocalDate.of(2001, 2, 9), LocalDate.of(2001, 1, 25), 0L},
                // даты совпадают, статистика есть
                {24103827L, LocalDate.of(2019, 2, 20), LocalDate.of(2019, 2, 20), 1L},
                // есть ненулевая статистика
                {24103827L, LocalDate.of(2019, 2, 20), LocalDate.of(2019, 3, 20), 22L}
        });
    }

    @Test
    public void test() {
        if (startDate == null || endDate == null) {
            assertThatThrownBy(() -> orderStatDayRepository.getOrdersDaysNum(singleton(orderId), startDate, endDate))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("startDate and endDate must be not null");
        } else {
            Long result = orderStatDayRepository.getOrdersDaysNum(singleton(orderId), startDate, endDate);
            assertThat(result).isEqualTo(expectedDays);
        }

    }
}
