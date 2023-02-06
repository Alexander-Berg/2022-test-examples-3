package ru.yandex.market.tpl.core.domain.order.listener;

import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.order.producer.OrderDamagedProducer;
import ru.yandex.market.tpl.core.domain.usershift.LockerSubtask;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.commands.OrderDeliveryFailReason;
import ru.yandex.market.tpl.core.domain.usershift.events.locker.LockerDeliverySubtaskFailedEvent;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

@RequiredArgsConstructor
public class OnPickupOrderDamagedSendToSqsTest extends TplAbstractTest {
    private static final String EXTERNAL_ORDER_ID = "12324";
    private static final Long ORDER_ID = 432L;

    private final ConfigurationProviderAdapter configurationProviderAdapter;

    private final OrderRepository orderRepository = Mockito.mock(OrderRepository.class);
    private final OrderDamagedProducer orderDamagedProducer = Mockito.mock(OrderDamagedProducer.class);
    private final OrderDeliveryFailReason orderDeliveryFailReason = Mockito.mock(OrderDeliveryFailReason.class);
    private final UserShift userShift = Mockito.mock(UserShift.class);
    private final LockerSubtask lockerSubtask = Mockito.mock(LockerSubtask.class);

    private OnOrderDamagedSendToSqs onOrderDamagedSendToSqsListener;

    @BeforeEach
    void setUp() {
        Mockito.when(orderRepository.findExternalOrderIdByOrderId(ORDER_ID))
            .thenReturn(Optional.of(EXTERNAL_ORDER_ID));
        Mockito.when(configurationProviderAdapter.isBooleanEnabled(ConfigurationProperties.SEND_SQS_EVENTS))
            .thenReturn(true);
        Mockito.when(lockerSubtask.getOrderId())
            .thenReturn(ORDER_ID);

        onOrderDamagedSendToSqsListener =
            new OnOrderDamagedSendToSqs(configurationProviderAdapter, orderDamagedProducer, orderRepository);
    }

    @Test
    void produceOnPickupOrderDamagedEvent() {
        Mockito.doReturn(OrderDeliveryTaskFailReasonType.ORDER_IS_DAMAGED)
            .when(orderDeliveryFailReason)
            .getType();
        LockerDeliverySubtaskFailedEvent event = new LockerDeliverySubtaskFailedEvent(
            userShift,
            orderDeliveryFailReason,
            lockerSubtask
        );
        onOrderDamagedSendToSqsListener.processPickupOrderDamagedEvent(event);

        Mockito.verify(orderDamagedProducer).produce(
            Mockito.eq(event.getTimestamp()),
            Mockito.eq(EXTERNAL_ORDER_ID),
            Mockito.eq(event.getUuid().toString())
        );
    }

    @Test
    void doNotProduceOnPickupOrderOtherEvent() {
        Mockito.doReturn(OrderDeliveryTaskFailReasonType.CANCEL_ORDER)
            .when(orderDeliveryFailReason)
            .getType();
        LockerDeliverySubtaskFailedEvent event = new LockerDeliverySubtaskFailedEvent(
            userShift,
            orderDeliveryFailReason,
            lockerSubtask
        );
        onOrderDamagedSendToSqsListener.processPickupOrderDamagedEvent(event);

        Mockito.verifyNoInteractions(orderDamagedProducer);
    }
}
