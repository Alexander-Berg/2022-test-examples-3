package ru.yandex.direct.core.entity.statistics.repository;

import java.time.LocalDateTime;
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
import static org.mockito.Mockito.when;

@Ignore("Ходит в yt, для ручного запуска")
@CoreTest
@RunWith(SpringRunner.class)
public class OrderStatDayRepositoryOrdersRoughForecastSmokeTest {
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
        setTimeProvider(LocalDateTime.now());
        Map<Long, Long> result = orderStatDayRepository.getOrderIdToMaxSum(Collections.singleton(orderId), 5);
        assertThat(result).isEmpty();
    }

    @Test
    public void getOrderIdToMaxSum_withNonEmptyResult() {
        Long orderId = 16281173L;
        setTimeProvider(LocalDateTime.of(2018, 7, 23, 13, 20, 18));
        Map<Long, Long> result = orderStatDayRepository.getOrderIdToMaxSum(Collections.singleton(orderId), 2);
        assertThat(result).containsEntry(orderId, 271150L);
    }

    private void setTimeProvider(LocalDateTime time) {
        when(timeProvider.now()).thenReturn(time);
    }
}
