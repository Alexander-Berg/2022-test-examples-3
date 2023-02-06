package ru.yandex.market.tpl.core.service.order.address.validation.dbqueue.batch;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderCommandService;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.service.order.validator.OrderAddressValidator;
import ru.yandex.market.tpl.core.test.ClockUtil;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RevalidateOrderAddressBatchServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderAddressValidator orderAddressValidator;
    @Mock
    private OrderCommandService orderCommandService;
    @Mock
    private Clock clock;
    @InjectMocks
    private RevalidateOrderAddressBatchService batchService;

    @Test
    void processPayload_when_Success() {
        //given
        Set<Long> dsIds = Set.of(1L, 2L, 3L);
        RevalidateOrderAddressBatchPayload batchPayload = new RevalidateOrderAddressBatchPayload("request", dsIds,
                Instant.now());

        Set<Long> expectedOrderIds = Set.of(4L, 5L, 6L);

        doReturn(expectedOrderIds
                        .stream()
                        .map(this::buildMockedOrder)
                        .collect(Collectors.toList()),
                List.of()).when(orderRepository).findAllForRevalidate(eq(batchPayload.getTriggeredAt()),
                eq(batchPayload.getDsIds()), any(), any(),
                any());
        ClockUtil.initFixed(clock);

        //when
        batchService.processPayload(batchPayload);

        //then
        verify(orderAddressValidator, times(1)).isGeoValidBulk(eq(expectedOrderIds));
    }

    private Order buildMockedOrder(Long id) {
        Order mockedOrder = mock(Order.class);
        doReturn(id).when(mockedOrder).getId();
        return mockedOrder;
    }
}
