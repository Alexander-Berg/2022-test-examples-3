package ru.yandex.direct.core.entity.statistics.repository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import one.util.streamex.StreamEx;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import ru.yandex.direct.core.entity.statistics.model.SpentInfo;
import ru.yandex.direct.ytcomponents.service.OrderStatDayDynContextProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;


/**
 * Тест, проверяющий корректную обработку ответа YTя, в котором ключи идут не подряд
 */
@ParametersAreNonnullByDefault
public class OrderStatDayRepositoryOrdersRoughForecastMixedResponseTest {

    @Mock
    private OrderStatDayDynContextProvider dynContextProvider;

    private OrderStatDayRepository repository;

    @Before
    public void prepare() {
        initMocks(this);
        repository = spy(new OrderStatDayRepository(dynContextProvider));
    }

    @Test
    public void test() {
        StreamEx<SpentInfo> spend = StreamEx.of(fakeSpent(1, 0, 10),
                fakeSpent(2, 0, 5),
                fakeSpent(1, 1, 20)
        );
        when(repository.getOrdersSpentInfo(anyList(), any(), any())).thenReturn(spend);

        Map<Long, Long> orderIdToMaxSum = new HashMap<>();
        assertThatCode(() -> orderIdToMaxSum.putAll(repository.getOrderIdToMaxSum(List.of(1L, 2L), 2)))
                .doesNotThrowAnyException();
        assertThat(orderIdToMaxSum).containsExactly(Map.entry(1L, 20L), Map.entry(2L, 5L));
    }

    private static SpentInfo fakeSpent(long orderId, long daysAgo, long cost) {
        return new SpentInfo(orderId, Instant.now().minus(daysAgo, ChronoUnit.DAYS), cost, 0L, 0L);
    }
}
