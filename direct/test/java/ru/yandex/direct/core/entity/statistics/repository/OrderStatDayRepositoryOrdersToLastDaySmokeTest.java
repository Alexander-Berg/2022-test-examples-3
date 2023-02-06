package ru.yandex.direct.core.entity.statistics.repository;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.testing.configuration.CoreTest;

import static org.assertj.core.api.Assertions.assertThat;

@Ignore("Ходит в yt, для ручного запуска")
@CoreTest
@RunWith(SpringRunner.class)
public class OrderStatDayRepositoryOrdersToLastDaySmokeTest {
    @Autowired
    private OrderStatDayRepository orderStatDayRepository;

    @Test
    public void getOrdersToLastDay_withEmptyResult() {
        Long orderId = 0L;
        Map<Long, LocalDate> result = orderStatDayRepository.getOrdersToLastDay(Collections.singleton(orderId));
        assertThat(result).isEmpty();
    }

    @Test
    public void getOrdersToLastDay_withNonEmptyResult() {
        Long orderId = 4L;
        Map<Long, LocalDate> result = orderStatDayRepository.getOrdersToLastDay(Collections.singleton(orderId));
        assertThat(result).containsEntry(orderId, LocalDate.of(2001, 2, 9));
    }
}
