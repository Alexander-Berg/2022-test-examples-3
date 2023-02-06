package ru.yandex.market.pvz.core.domain.logbroker.lrm;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lrm.event_model.ReturnEvent;
import ru.yandex.market.logistics.lrm.event_model.ReturnEventType;
import ru.yandex.market.pvz.core.domain.order.OrderQueryService;
import ru.yandex.market.pvz.core.domain.order.model.Order;
import ru.yandex.market.pvz.core.domain.order.model.params.OrderParams;
import ru.yandex.market.pvz.core.domain.order_delivery_result.ItemDeliveryFlow;
import ru.yandex.market.pvz.core.domain.order_delivery_result.service.OrderDeliveryResultCommandService;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestOrderFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.pvz.core.domain.order.model.PvzOrderStatus.ARRIVED_TO_PICKUP_POINT;
import static ru.yandex.market.pvz.core.domain.order.model.PvzOrderStatus.TRANSMITTED_TO_RECIPIENT;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.UIT_2_1;

@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class LrmLogbrokerEventHandlerTest {

    private static final long LRM_RETURN_ID = 42;

    private final OrderDeliveryResultCommandService orderDeliveryResultCommandService;
    private final LrmLogbrokerEventHandler eventHandler;
    private final OrderQueryService orderQueryService;

    private final TestOrderFactory orderFactory;

    @Test
    void testHandleOrderChangeEvent() {
        Order createdOrder = createOrder();

        orderDeliveryResultCommandService.startFitting(createdOrder.getId());
        orderDeliveryResultCommandService.updateItemFlow(createdOrder.getId(), UIT_2_1, ItemDeliveryFlow.RETURN);
        orderDeliveryResultCommandService.finishFitting(createdOrder.getId());
        orderDeliveryResultCommandService.pay(createdOrder.getId());
        orderDeliveryResultCommandService.packageReturn(createdOrder.getId(), List.of("123"));
        orderDeliveryResultCommandService.bindLrmReturnId(createdOrder.getId(), LRM_RETURN_ID);

        orderFactory.setStatusAndCheckpoint(createdOrder.getId(), ARRIVED_TO_PICKUP_POINT);
        orderFactory.setStatusOnly(createdOrder.getId(), TRANSMITTED_TO_RECIPIENT);

        eventHandler.handle(ReturnEvent.builder()
                .eventType(ReturnEventType.CUSTOMER_ORDER_ITEMS_CHANGED)
                .orderExternalId(createdOrder.getExternalId())
                .returnId(LRM_RETURN_ID)
                .build());

        OrderParams order = orderQueryService.get(createdOrder.getId());
        assertThat(order.getDsApiCheckpoint()).isEqualTo(TRANSMITTED_TO_RECIPIENT.getCode());
        assertThat(order.getStatus()).isEqualTo(TRANSMITTED_TO_RECIPIENT);
    }

    private Order createOrder() {
        var order = orderFactory.createSimpleFashionOrder();
        return orderFactory.receiveOrder(order.getId());
    }

}
