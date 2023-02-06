package ru.yandex.market.tpl.core.domain.order.listener;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.base.fsm.StatusTransition;
import ru.yandex.market.tpl.core.domain.base.fsm.StatusTransitionType;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.events.OrderFlowStatusChangedEvent;
import ru.yandex.market.tpl.core.domain.order.producer.PickupOrderDeliveredProducer;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

@RequiredArgsConstructor
public class OnOrderFlowStatusUpdatedSendToSqsTest extends TplAbstractTest {

    private final ConfigurationProviderAdapter configurationProviderAdapter;

    private final PickupOrderDeliveredProducer pickupOrderDeliveredProducer =
        Mockito.mock(PickupOrderDeliveredProducer.class);

    private OnOrderFlowStatusUpdatedSendToSqs onOrderFlowStatusUpdatedSendToSqs;

    private final Order order = Mockito.mock(ru.yandex.market.tpl.core.domain.order.Order.class);

    @BeforeEach
    void setUp() {
        Mockito.when(configurationProviderAdapter.isBooleanEnabled(ConfigurationProperties.SEND_SQS_EVENTS))
            .thenReturn(true);
        onOrderFlowStatusUpdatedSendToSqs =
            new OnOrderFlowStatusUpdatedSendToSqs(pickupOrderDeliveredProducer, configurationProviderAdapter);
    }

    @Test
    void produceOnDeliveredEvent() {
        Mockito.doReturn(OrderFlowStatus.DELIVERED_TO_PICKUP_POINT)
            .when(order)
            .getOrderFlowStatus();
        OrderFlowStatusChangedEvent event = new OrderFlowStatusChangedEvent(
            order,
            new StatusTransition<>(
                null,
                OrderFlowStatus.DELIVERED_TO_PICKUP_POINT,
                StatusTransitionType.NORMAL,
                Source.COURIER
            )
        );
        onOrderFlowStatusUpdatedSendToSqs.processEvent(event);

        Mockito.verify(pickupOrderDeliveredProducer).produce(Mockito.eq(event));
    }

    @Test
    void doNotProduceOnOtherEvent() {
        Mockito.doReturn(OrderFlowStatus.CREATED)
            .when(order)
            .getOrderFlowStatus();
        OrderFlowStatusChangedEvent event = new OrderFlowStatusChangedEvent(
            order,
            new StatusTransition<>(
                null,
                OrderFlowStatus.CREATED,
                StatusTransitionType.NORMAL,
                Source.COURIER
            )
        );

        onOrderFlowStatusUpdatedSendToSqs.processEvent(event);

        Mockito.verifyNoInteractions(pickupOrderDeliveredProducer);
    }
}
