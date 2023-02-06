package ru.yandex.market.tpl.core.domain.order.listener;

import java.time.Instant;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.order.producer.RevertOrderDamagedProducer;
import ru.yandex.market.tpl.core.domain.usershift.LockerSubtask;
import ru.yandex.market.tpl.core.domain.usershift.OrderDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.commands.OrderDeliveryFailReason;
import ru.yandex.market.tpl.core.domain.usershift.events.OrderDeliveryTaskReopenedEvent;
import ru.yandex.market.tpl.core.domain.usershift.events.locker.LockerDeliverySubtaskReopenedEvent;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

@RequiredArgsConstructor
public class OnRevertOrderDamagedSendToSqsTest extends TplAbstractTest {

    private static final String EXTERNAL_ORDER_ID = "12324";
    private static final Long ORDER_ID = 432L;

    private final ConfigurationProviderAdapter configurationProviderAdapter;

    private final OrderRepository orderRepository = Mockito.mock(OrderRepository.class);
    private final RevertOrderDamagedProducer revertOrderDamagedProducer =
        Mockito.mock(RevertOrderDamagedProducer.class);
    private final OrderDeliveryTask orderDeliveryTask = Mockito.mock(OrderDeliveryTask.class);
    private final RoutePoint routePoint = Mockito.mock(RoutePoint.class);
    private final OrderDeliveryFailReason orderDeliveryFailReason = Mockito.mock(OrderDeliveryFailReason.class);
    private final UserShift userShift = Mockito.mock(UserShift.class);
    private final LockerSubtask lockerSubtask = Mockito.mock(LockerSubtask.class);

    private OnRevertOrderDamagedSendToSqs onRevertOrderDamagedSendToSqsListener;

    private static final Instant INSTANT = Instant.ofEpochMilli(1632126835);

    @BeforeEach
    void setUp() {
        Mockito.when(orderRepository.findExternalOrderIdByOrderId(ORDER_ID))
            .thenReturn(Optional.of(EXTERNAL_ORDER_ID));
        Mockito.when(configurationProviderAdapter.isBooleanEnabled(ConfigurationProperties.SEND_SQS_EVENTS))
            .thenReturn(true);
        Mockito.when(orderDeliveryTask.getRoutePoint())
            .thenReturn(routePoint);
        Mockito.when(orderDeliveryTask.getOrderId())
            .thenReturn(ORDER_ID);

        Mockito.when(lockerSubtask.getDeliveryTask())
            .thenReturn(orderDeliveryTask);
        Mockito.when(orderDeliveryTask.getRoutePoint())
            .thenReturn(routePoint);
        Mockito.when(routePoint.getUserShift())
            .thenReturn(userShift);
        Mockito.when(lockerSubtask.getOrderId())
            .thenReturn(ORDER_ID);


        onRevertOrderDamagedSendToSqsListener =
            new OnRevertOrderDamagedSendToSqs(configurationProviderAdapter, revertOrderDamagedProducer, orderRepository);
    }

    @Test
    void produceOnRecipientRevertOrderDamagedEvent() {
        Mockito.doReturn(OrderDeliveryTaskFailReasonType.ORDER_IS_DAMAGED)
            .when(orderDeliveryFailReason)
            .getType();
        OrderDeliveryTaskReopenedEvent event = new OrderDeliveryTaskReopenedEvent(
            orderDeliveryTask,
            orderDeliveryFailReason,
            Source.COURIER,
            INSTANT
        );
        onRevertOrderDamagedSendToSqsListener.processRecipientRevertOrderDamagedEvent(event);

        Mockito.verify(revertOrderDamagedProducer).produce(
            Mockito.eq(event.getTimestamp()),
            Mockito.eq(EXTERNAL_ORDER_ID),
            Mockito.eq(event.getUuid().toString())
        );
    }

    @Test
    void doNotProduceOnRecipientRevertOtherEvent() {
        Mockito.doReturn(OrderDeliveryTaskFailReasonType.CANCEL_ORDER)
            .when(orderDeliveryFailReason)
            .getType();
        OrderDeliveryTaskReopenedEvent event = new OrderDeliveryTaskReopenedEvent(
            orderDeliveryTask,
            orderDeliveryFailReason,
            Source.COURIER,
            INSTANT
        );
        onRevertOrderDamagedSendToSqsListener.processRecipientRevertOrderDamagedEvent(event);

        Mockito.verifyNoInteractions(revertOrderDamagedProducer);
    }

    @Test
    void produceOnPickupRevertOrderDamagedEvent() {
        Mockito.doReturn(OrderDeliveryTaskFailReasonType.ORDER_IS_DAMAGED)
            .when(orderDeliveryFailReason)
            .getType();
        LockerDeliverySubtaskReopenedEvent event = new LockerDeliverySubtaskReopenedEvent(
            lockerSubtask,
            orderDeliveryFailReason,
            Source.COURIER,
            INSTANT
        );
        onRevertOrderDamagedSendToSqsListener.processPickupRevertOrderDamagedEvent(event);

        Mockito.verify(revertOrderDamagedProducer).produce(
            Mockito.eq(event.getTimestamp()),
            Mockito.eq(EXTERNAL_ORDER_ID),
            Mockito.eq(event.getUuid().toString())
        );
    }

    @Test
    void doNotProduceOnPickupRevertOtherEvent() {
        Mockito.doReturn(OrderDeliveryTaskFailReasonType.CANCEL_ORDER)
            .when(orderDeliveryFailReason)
            .getType();
        LockerDeliverySubtaskReopenedEvent event = new LockerDeliverySubtaskReopenedEvent(
            lockerSubtask,
            orderDeliveryFailReason,
            Source.COURIER,
            INSTANT
        );
        onRevertOrderDamagedSendToSqsListener.processPickupRevertOrderDamagedEvent(event);

        Mockito.verifyNoInteractions(revertOrderDamagedProducer);
    }
}
