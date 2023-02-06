package ru.yandex.market.pvz.internal.domain.lms;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import ru.yandex.market.logistics.front.library.dto.detail.DetailData;
import ru.yandex.market.logistics.front.library.dto.grid.GridData;
import ru.yandex.market.logistics.front.library.dto.grid.GridItem;
import ru.yandex.market.pvz.core.domain.configuration.global.ConfigurationGlobalCommandService;
import ru.yandex.market.pvz.core.domain.order.OrderRepository;
import ru.yandex.market.pvz.core.domain.order.model.Order;
import ru.yandex.market.pvz.core.domain.order.model.OrderDeliveryType;
import ru.yandex.market.pvz.core.domain.order.model.OrderPaymentType;
import ru.yandex.market.pvz.core.domain.order.model.OrderType;
import ru.yandex.market.pvz.core.domain.order.model.PvzOrderStatus;
import ru.yandex.market.pvz.core.domain.order.model.history.OrderHistoryRepository;
import ru.yandex.market.pvz.core.domain.order_delivery_result.service.OrderDeliveryResultCommandService;
import ru.yandex.market.pvz.core.test.EmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestOrderFactory;
import ru.yandex.market.pvz.internal.PvzIntTest;
import ru.yandex.market.pvz.internal.domain.lms.order.LmsOrderService;
import ru.yandex.market.pvz.internal.domain.lms.order.dto.LmsOrderFilterDto;
import ru.yandex.market.tpl.common.util.DateTimeUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.VERIFICATION_CODE_ON_DEMAND_ENABLED;
import static ru.yandex.market.pvz.core.domain.order.model.PvzOrderStatus.ARRIVED_TO_PICKUP_POINT;
import static ru.yandex.market.pvz.core.domain.order.model.PvzOrderStatus.CANCELLED;
import static ru.yandex.market.pvz.core.domain.order.model.PvzOrderStatus.CREATED;
import static ru.yandex.market.pvz.core.domain.order.model.PvzOrderStatus.ORDER_IS_LOST;

@PvzIntTest
@EmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class LmsOrderServiceTest {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(DateTimeUtil.DEFAULT_ZONE_ID);

    private final TestOrderFactory orderFactory;
    private final LmsOrderService lmsOrderService;
    private final OrderRepository orderRepository;
    private final OrderHistoryRepository orderHistoryRepository;
    private final OrderDeliveryResultCommandService orderDeliveryResultCommandService;
    private final ConfigurationGlobalCommandService configurationGlobalCommandService;

    @Test
    void testGetOrdersWithFilter() {
        Order order = orderFactory.createOrder();

        GridData data = lmsOrderService.getOrders(LmsOrderFilterDto.builder()
                .externalId(order.getExternalId())
                .build(), PageRequest.of(0, 10));

        List<GridItem> orders = data.getItems();
        assertThat(orders.size()).isEqualTo(1);
        assertThat(orders.get(0).getValues()).containsExactlyInAnyOrderEntriesOf(Map.of(
                "externalId", order.getExternalId(),
                "pvzMarketId", order.getPickupPoint().getPvzMarketId(),
                "paymentStatus", order.getPaymentStatus().getDescription(),
                "paymentType", order.getPaymentType().getDescription(),
                "status", order.getStatus().getDescription()
        ));

    }

    @Test
    void testGetOrder() {
        Order order = orderFactory.createOrder();
        DetailData data = lmsOrderService.getOrderById(order.getId());
        var history = orderHistoryRepository.findFirstByExternalIdOrderByUpdatedAtDesc(order.getExternalId());
        assertThat(data.getItem().getValues()).containsExactlyInAnyOrderEntriesOf(Map.of(
                "externalId", order.getExternalId(),
                "pvzMarketId", order.getPickupPoint().getPvzMarketId(),
                "paymentStatus", order.getPaymentStatus().getDescription(),
                "paymentType", order.getPaymentType().getDescription(),
                "status", order.getStatus().getDescription(),
                "deliveryServiceId", order.safeGetDeliveryService().getId(),
                "type", order.getType().getDescription(),
                "lastUpdatedAt", DATE_TIME_FORMATTER.format(history.get().getUpdatedAt())
        ));
    }

    @ParameterizedTest
    @EnumSource(PvzOrderStatus.class)
    void testRevertShipmentSuccess(PvzOrderStatus status) {
        var order = orderFactory.createOrder();
        var orderId = order.getId();

        order.setStatusAndCheckpoint(status);
        if (PvzOrderStatus.ON_PVZ_STATUSES_CAN_BE_REVERTED.contains(status)) {
            revertAndAssert(CREATED, orderId);
        } else if (status == PvzOrderStatus.READY_FOR_RETURN) {
            revertAndAssert(CANCELLED, orderId);
        } else {
            assertThatThrownBy(() -> lmsOrderService.revertShipment(orderId));
        }
    }

    private void revertAndAssert(PvzOrderStatus status, Long orderId) {
        lmsOrderService.revertShipment(orderId);
        assertStatus(status, orderId);
    }

    @ParameterizedTest
    @EnumSource(PvzOrderStatus.class)
    void testConvertOrderToLost(PvzOrderStatus status) {
        var order = orderFactory.createOrder();
        var orderId = order.getId();

        order.setStatusAndCheckpoint(status);
        if (status == PvzOrderStatus.READY_FOR_RETURN) {
            lmsOrderService.convertOrderToLost(orderId);
            assertStatus(ORDER_IS_LOST, orderId);
        } else {
            assertThatThrownBy(() -> lmsOrderService.convertOrderToLost(orderId));
        }
    }

    private void assertStatus(PvzOrderStatus status, Long orderId) {
        var order = orderRepository.findByIdOrThrow(orderId);
        assertThat(order.getStatus()).isEqualTo(status);
        assertThat(order.getArrivedAt()).isNull();
    }

    @Test
    void testAcceptCancelledSuccess() {
        Order order = orderFactory.createOrder();
        order.setStatusAndCheckpoint(CANCELLED);
        lmsOrderService.acceptCancelled(order.getId());
        order = orderRepository.findByIdOrThrow(order.getId());
        assertThat(order.getStatus()).isEqualTo(PvzOrderStatus.READY_FOR_RETURN);
    }

    @Test
    void testAcceptCancelledFromInvalidStatus() {
        Order order = orderFactory.createOrder();
        order.setStatusAndCheckpoint(ARRIVED_TO_PICKUP_POINT);
        assertThatThrownBy(() -> lmsOrderService.acceptCancelled(order.getId()));
    }

    @Test
    void testCommitDeliverySuccess() {
        Order order = orderFactory.createOrder();
        order.setStatusAndCheckpoint(PvzOrderStatus.TRANSMITTED_TO_RECIPIENT);
        lmsOrderService.commitDelivery(order.getId());
        order = orderRepository.findByIdOrThrow(order.getId());
        assertThat(order.getStatus()).isEqualTo(PvzOrderStatus.DELIVERED_TO_RECIPIENT);
    }

    @Test
    void testCommitDeliveryFromInvalidStatus() {
        Order order = orderFactory.createOrder();
        order.setStatusAndCheckpoint(ARRIVED_TO_PICKUP_POINT);
        assertThatThrownBy(() -> lmsOrderService.commitDelivery(order.getId()));
    }

    @Test
    void testCommitDeliveryForFittingStartedOrder() {
        Order order = orderFactory.createSimpleFashionOrder();
        orderFactory.receiveOrder(order.getId());

        orderDeliveryResultCommandService.startFitting(order.getId());
        orderDeliveryResultCommandService.finishFitting(order.getId());
        orderDeliveryResultCommandService.pay(order.getId());
        orderFactory.verifyOrder(order.getId());
        orderFactory.deliverOrder(order.getId(), OrderDeliveryType.BARCODE, OrderPaymentType.CASH);

        assertThatThrownBy(() -> lmsOrderService.commitDelivery(order.getId()));
    }

    @Test
    void testCancelReturnSuccess() {
        Order order = orderFactory.createOrder();
        order.setStatusAndCheckpoint(PvzOrderStatus.READY_FOR_RETURN);
        lmsOrderService.cancelReturn(order.getId());
        order = orderRepository.findByIdOrThrow(order.getId());
        assertThat(order.getStatus()).isEqualTo(ARRIVED_TO_PICKUP_POINT);
    }

    @Test
    void testCancelReturnFromInvalidStatus() {
        Order order = orderFactory.createOrder();
        order.setStatusAndCheckpoint(PvzOrderStatus.RETURNED_ORDER_WAS_DISPATCHED);
        assertThatThrownBy(() -> lmsOrderService.cancelReturn(order.getId()));
    }


    @Test
    void testTransmitSuccessForClientOrder() {
        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .params(TestOrderFactory.OrderParams.builder()
                        .type(OrderType.CLIENT)
                        .build())
                .build());
        order.setStatusAndCheckpoint(ARRIVED_TO_PICKUP_POINT);
        lmsOrderService.transmit(order.getId());
        assertThat(order.getStatus()).isEqualTo(PvzOrderStatus.TRANSMITTED_TO_RECIPIENT);
    }

    @Test
    void testTransmitSuccessForOnDemandOrder() {
        configurationGlobalCommandService.setValue(VERIFICATION_CODE_ON_DEMAND_ENABLED, true);

        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .params(TestOrderFactory.OrderParams.builder()
                        .type(OrderType.ON_DEMAND)
                        .verificationCode("1337")
                        .build())
                .build());
        order.setStatusAndCheckpoint(ARRIVED_TO_PICKUP_POINT);
        lmsOrderService.transmit(order.getId());
        assertThat(order.getStatus()).isEqualTo(PvzOrderStatus.TRANSPORTATION_RECIPIENT);
    }

    @Test
    void testTransmitFromInvalidStatus() {
        Order order = orderFactory.createOrder();
        order.setStatusAndCheckpoint(PvzOrderStatus.RETURNED_ORDER_WAS_DISPATCHED);
        assertThatThrownBy(() -> lmsOrderService.transmit(order.getId()));
    }

    @Test
    void testGetOrderWithUpdatedStatus() {
        var order = orderFactory.createOrder();
        var orderId = order.getId();

        order.setStatusAndCheckpoint(ARRIVED_TO_PICKUP_POINT);
        lmsOrderService.revertShipment(orderId);

        var data = lmsOrderService.getOrderById(orderId);
        var history = orderHistoryRepository.findFirstByExternalIdOrderByUpdatedAtDesc(order.getExternalId());
        assertThat(data.getItem().getValues().get("lastUpdatedAt"))
                .isEqualTo(DATE_TIME_FORMATTER.format(history.get().getUpdatedAt()));
    }

    @Test
    void whenCancelCreatedOrderThenSuccess() {
        Order order = orderFactory.createOrder();
        lmsOrderService.cancelOrder(order.getId());
        order = orderRepository.findByIdOrThrow(order.getId());
        assertThat(order.getStatus()).isEqualTo(CANCELLED);
    }

    @Test
    void whenCancelArrivedToPickupPointOrderThenError() {
        Order order = orderFactory.createOrder();
        orderFactory.receiveOrder(order.getId());
        assertThatThrownBy(() -> lmsOrderService.cancelOrder(order.getId()));
    }

}
