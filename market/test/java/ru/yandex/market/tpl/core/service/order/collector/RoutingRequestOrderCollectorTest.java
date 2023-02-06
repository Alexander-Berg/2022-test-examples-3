package ru.yandex.market.tpl.core.service.order.collector;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.tpl.api.model.order.OrderDeliveryStatus;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.external.routing.api.RoutingMockType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.common.util.DateTimeUtils.MOSCOW_ZONE;

@ExtendWith(MockitoExtension.class)
class RoutingRequestOrderCollectorTest {

    public static final LocalDate SHIFT_DATE = LocalDate.of(2021, 5, 19);
    public static final Set<Long> DS_IDS = Set.of(1L);
    public static final long SHIFT_ID = 100L;

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private UserShiftRepository userShiftRepository;
    @Mock
    private Clock clock;
    @Mock
    private ConfigurationProviderAdapter configurationProviderAdapter;

    @InjectMocks
    private RoutingRequestOrderCollector routingRequestOrderCollector;

    private Shift mockedShift;

    @BeforeEach
    void init() {
        initShift();
        reset(orderRepository);
    }

    @Test
    void shouldCollectNotArrived() {
        //given
        initClock(SHIFT_DATE);

        Order notArrivedOrder = mock(Order.class);
        when(orderRepository.findOrdersToDeliverBetweenDatesWithRelatedEntities(any(), eq(SHIFT_DATE), eq(DS_IDS)))
                .thenReturn(Collections.singletonList(notArrivedOrder));

        //when
        List<Order> collectedOrders = routingRequestOrderCollector.collect(mockedShift, DS_IDS,
                RoutingMockType.PREROUTING,
                Instant.now());

        //then
        assertThat(collectedOrders).hasSize(1).contains(notArrivedOrder);
    }

    @Test
    void shouldCollectForPreviousDays_withNotFinished() {
        //given
        initClock(SHIFT_DATE.plusDays(1L));
        when(mockedShift.getId()).thenReturn(SHIFT_ID);

        Order mockedOrder = mock(Order.class);
        when(orderRepository.findAllByExternalOrderIdWithRelatedEntities(any()))
                .thenReturn(Collections.singletonList(mockedOrder));

        Order mockedNotFinishedOrder = mock(Order.class);
        when(mockedNotFinishedOrder.getDeliveryStatus()).thenReturn(OrderDeliveryStatus.NOT_DELIVERED);
        when(orderRepository.findAllBetweenDeliveryDateInStatusesWithRelatedEntities(
                any(), eq(SHIFT_DATE), eq(DS_IDS), any()
        )).thenReturn(Collections.singletonList(mockedNotFinishedOrder));

        //when
        List<Order> collectedOrders = routingRequestOrderCollector.collect(mockedShift, DS_IDS,
                RoutingMockType.PREROUTING,
                Instant.now());

        //then
        verify(userShiftRepository, times(1)).findAllByShiftId(eq(SHIFT_ID));
        assertThat(collectedOrders).contains(mockedOrder, mockedNotFinishedOrder);
    }

    private void initClock(LocalDate localDate) {
        when(clock.instant()).thenReturn(DateTimeUtil.atStartOfDay(localDate));
        when(clock.getZone()).thenReturn(MOSCOW_ZONE);
    }

    private void initShift() {
        mockedShift = mock(Shift.class);
        when(mockedShift.getShiftDate()).thenReturn(SHIFT_DATE);

    }
}
