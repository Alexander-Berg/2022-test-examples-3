package ru.yandex.market.pvz.internal.domain.order;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pvz.core.domain.order.OrderQueryService;
import ru.yandex.market.pvz.core.domain.order.model.history.OrderHistoryParams;
import ru.yandex.market.pvz.core.domain.order.model.history.OrderHistoryQueryService;
import ru.yandex.market.pvz.core.domain.order_delivery_result.service.OrderDeliveryResultCommandService;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestOrderFactory;
import ru.yandex.market.pvz.internal.PvzIntTest;
import ru.yandex.market.pvz.internal.controller.manual.dto.ManualChangeOrderHistoryDto;
import ru.yandex.market.tpl.common.util.exception.TplIllegalStateException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.market.pvz.core.domain.order.model.PvzOrderStatus.READY_FOR_RETURN;
import static ru.yandex.market.pvz.core.domain.order.model.PvzOrderStatus.RETURNED_ORDER_WAS_DISPATCHED;

@PvzIntTest
@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class ManualOrderServiceTest {

    private final TestOrderFactory orderFactory;
    private final ManualOrderService manualOrderService;
    private final OrderHistoryQueryService orderHistoryQueryService;
    private final OrderQueryService orderQueryService;
    private final OrderDeliveryResultCommandService orderDeliveryResultCommandService;

    @Test
    void changeOrderHistory() {
        orderFactory.createOrder();
        var order = orderFactory.createOrder();
        orderFactory.receiveOrder(order.getId());

        manualOrderService.changeOrderHistory(List.of(
                ManualChangeOrderHistoryDto.builder()
                        .id(order.getId())
                        .externalId(order.getExternalId())
                        .status(READY_FOR_RETURN)
                        .build()
        ));

        List<OrderHistoryParams> orderHistoryParams = orderHistoryQueryService.getOrderHistory(order.getId());
        assertThat(orderHistoryParams).contains(
                new OrderHistoryParams(order.getId(), order.getExternalId(), READY_FOR_RETURN)
        );
    }

    @Test
    void shipReturnOrder() {
        var order = orderFactory.createOrder();
        orderFactory.receiveOrder(order.getId());
        orderFactory.cancelOrder(order.getId());

        manualOrderService.shipReturnOrder(order.getId());

        var orderParams = orderQueryService.getSimple(order.getId());
        assertThat(orderParams.getStatus()).isEqualTo(RETURNED_ORDER_WAS_DISPATCHED);
    }

    @Test
    void cancelFashionOrderWithFitting() {
        var order = orderFactory.createSimpleFashionOrder();
        orderFactory.receiveOrder(order.getId());
        orderDeliveryResultCommandService.startFitting(order.getId());

        manualOrderService.cancelFashionOrderWithFitting(order.getId());

        var orderParams = orderQueryService.getSimple(order.getId());
        assertThat(orderParams.getStatus()).isEqualTo(READY_FOR_RETURN);
    }

    @Test
    void cancelFashionOrderWithoutFitting() {
        var order = orderFactory.createSimpleFashionOrder();
        orderFactory.receiveOrder(order.getId());

        assertThatThrownBy(() -> manualOrderService.cancelFashionOrderWithFitting(order.getId()))
                .isExactlyInstanceOf(TplIllegalStateException.class);
    }

}
