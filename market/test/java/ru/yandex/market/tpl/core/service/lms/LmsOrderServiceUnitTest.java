package ru.yandex.market.tpl.core.service.lms;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.common.util.exception.TplIllegalArgumentException;
import ru.yandex.market.tpl.core.domain.ds.DsZoneOffsetCachingService;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderDelivery;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.order.validator.TplOrderRescheduleValidator;
import ru.yandex.market.tpl.core.domain.partner.DeliveryService;
import ru.yandex.market.tpl.core.domain.reschedule.dbqueue.RescheduleOrderProducer;
import ru.yandex.market.tpl.core.service.lms.order.LmsOrderService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author sekulebyakin
 */
@ExtendWith(MockitoExtension.class)
public class LmsOrderServiceUnitTest {

    @InjectMocks
    private LmsOrderService lmsOrderService;

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private RescheduleOrderProducer rescheduleOrderProducer;
    @Mock
    private DsZoneOffsetCachingService dsZoneOffsetCachingService;
    @Mock
    private TplOrderRescheduleValidator tplOrderRescheduleValidator;
    @Spy
    private Clock clock = new TestableClock();

    private final ZoneOffset offset = ZoneOffset.of("+03:00");
    private final LocalDateTime now = LocalDateTime.of(2000, 5, 10, 15, 30);

    @BeforeEach
    void setup() {
        ((TestableClock) clock).setFixed(now.toInstant(offset), offset);
        lenient().doReturn(offset).when(dsZoneOffsetCachingService).getOffsetForDs(eq(DeliveryService.DEFAULT_DS_ID));
    }

    @Test
    void rescheduleOrdersSuccessTest() {
        var order1 = mockOrder("111", OrderFlowStatus.CREATED);
        var order2 = mockOrder("222", OrderFlowStatus.CREATED);
        var order1Date = now.toLocalDate().plusDays(2);
        var order2Date = now.toLocalDate().plusDays(3);

        var rescheduleMap = Map.of(
                order1.getExternalOrderId(), order1Date,
                order2.getExternalOrderId(), order2Date
        );

        doReturn(List.of(order1, order2)).when(orderRepository).findAllByExternalOrderIdOrThrow(rescheduleMap.keySet());

        lmsOrderService.rescheduleOrders(rescheduleMap);

        var nowInstant = Instant.now(clock);
        verify(rescheduleOrderProducer).produce(eq(order1), eq(order1Date), eq(nowInstant));
        verify(rescheduleOrderProducer).produce(eq(order2), eq(order2Date), eq(nowInstant));

        var order1NewInterval =  order1.getDelivery().getInterval().toLocalTimeInterval(offset).toInterval(order1Date, offset);
        var order2NewInterval =  order2.getDelivery().getInterval().toLocalTimeInterval(offset).toInterval(order2Date, offset);

        verify(tplOrderRescheduleValidator).validate(eq(order1), eq(order1NewInterval));
        verify(tplOrderRescheduleValidator).validate(eq(order2), eq(order2NewInterval));
    }

    @Test
    void rescheduleOrdersCancelledStatusFailTest() {
        var order1 = mockOrder("111", OrderFlowStatus.CREATED);
        var order2 = mockOrder("222", OrderFlowStatus.CANCELLED);
        var order1Date = now.toLocalDate().plusDays(2);
        var order2Date = now.toLocalDate().plusDays(3);

        var rescheduleMap = Map.of(
                order1.getExternalOrderId(), order1Date,
                order2.getExternalOrderId(), order2Date
        );

        doReturn(List.of(order1, order2)).when(orderRepository).findAllByExternalOrderIdOrThrow(rescheduleMap.keySet());

        var ex = assertThrows(TplIllegalArgumentException.class,
                () -> lmsOrderService.rescheduleOrders(rescheduleMap));

        assertThat(ex.getMessage()).contains(order2.getExternalOrderId());
    }

    private Order mockOrder(String externalId, OrderFlowStatus status) {
        var order = mock(Order.class);
        var delivery = mock(OrderDelivery.class);
        lenient().doReturn(externalId).when(order).getExternalOrderId();
        lenient().doReturn(delivery).when(order).getDelivery();
        var interval = LocalTimeInterval.valueOf("10:00-15:00").toInterval(now.toLocalDate(), offset);
        lenient().doReturn(interval).when(delivery).getInterval();
        lenient().doReturn(DeliveryService.DEFAULT_DS_ID).when(order).getDeliveryServiceId();
        lenient().doReturn(status).when(order).getOrderFlowStatus();
        return order;
    }

}
