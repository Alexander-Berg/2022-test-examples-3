package ru.yandex.direct.core.entity.statistics.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.utils.TimeProvider;
import ru.yandex.direct.ytcomponents.service.OrderStatDayDynContextProvider;

import static org.assertj.core.api.Assertions.assertThat;

@Ignore("Ходит в yt, для ручного запуска")
@CoreTest
@RunWith(SpringRunner.class)
public class OrderStatDayRepositoryDateToSumSpentTest {
    @Autowired
    private OrderStatDayDynContextProvider dynContextProvider;

    private OrderStatDayRepository orderStatDayRepository;

    @Mock
    private TimeProvider timeProvider;

    @Before
    public void prepare() {
        MockitoAnnotations.initMocks(this);
        orderStatDayRepository = new OrderStatDayRepository(dynContextProvider, timeProvider);
    }

    @Test
    public void getOrderIdToMaxSum_withEmptyResult() {
        Long orderId = 0L;
        Map<LocalDate, BigDecimal> result = orderStatDayRepository.getDateToSumSpent(Collections.singleton(orderId),
                LocalDate.now().minusDays(4), LocalDate.now());
        assertThat(result).isEmpty();
    }

    @Test
    public void getOrderIdToMaxSum_nonEmptyResult() {
        Long orderId = 21446784L;
        Map<LocalDate, BigDecimal> result = orderStatDayRepository.getDateToSumSpent(
                Collections.singleton(orderId),
                LocalDate.of(2018, 7, 17),
                LocalDate.of(2018, 7, 20));
        assertThat(result)
                .containsEntry(LocalDate.of(2018, 7, 17), BigDecimal.valueOf(302264L))
                .containsEntry(LocalDate.of(2018, 7, 18), BigDecimal.valueOf(1523871L))
                .containsEntry(LocalDate.of(2018, 7, 19), BigDecimal.valueOf(1114605L))
                .containsEntry(LocalDate.of(2018, 7, 20), BigDecimal.valueOf(263148L));
    }

    @Test
    public void getOrderIdToMaxSum_bothPlatforms() {
        Long orderId = 10277264L;
        Map<LocalDate, BigDecimal> result = orderStatDayRepository.getDateToSumSpent(
                Collections.singleton(orderId),
                LocalDate.of(2016, 10, 8),
                LocalDate.of(2016, 10, 10));
        assertThat(result)
                .containsEntry(LocalDate.of(2016, 10, 8), BigDecimal.valueOf(173460L))
                .containsEntry(LocalDate.of(2016, 10, 9), BigDecimal.valueOf(160480L))
                .containsEntry(LocalDate.of(2016, 10, 10), BigDecimal.valueOf(215940L));
    }

    @Test
    public void getOrderIdToMaxSum_twoOrders() {
        Map<LocalDate, BigDecimal> result = orderStatDayRepository.getDateToSumSpent(
                Arrays.asList(7720839L, 8856790L),
                LocalDate.of(2016, 10, 18),
                LocalDate.of(2016, 10, 21));
        assertThat(result)
                .containsEntry(LocalDate.of(2016, 10, 18), BigDecimal.valueOf(58176360L))
                .containsEntry(LocalDate.of(2016, 10, 19), BigDecimal.valueOf(58399380L))
                .containsEntry(LocalDate.of(2016, 10, 20), BigDecimal.valueOf(56785140L))
                .containsEntry(LocalDate.of(2016, 10, 21), BigDecimal.valueOf(28358940L));
    }
}
