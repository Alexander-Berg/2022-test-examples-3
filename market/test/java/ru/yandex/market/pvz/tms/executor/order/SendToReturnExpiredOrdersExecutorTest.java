package ru.yandex.market.pvz.tms.executor.order;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.pvz.core.domain.order.OrderRepository;
import ru.yandex.market.pvz.core.domain.order.model.Order;
import ru.yandex.market.pvz.core.domain.order.model.PvzOrderStatus;
import ru.yandex.market.pvz.core.domain.order_delivery_result.service.OrderDeliveryResultCommandService;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPointRequestData;
import ru.yandex.market.pvz.core.domain.shipment.ShipmentCommandService;
import ru.yandex.market.pvz.core.domain.shipment.model.Shipment;
import ru.yandex.market.pvz.core.domain.shipment.model.ShipmentCreateItemParams;
import ru.yandex.market.pvz.core.domain.shipment.model.ShipmentCreateParams;
import ru.yandex.market.pvz.core.domain.shipment.model.ShipmentStatus;
import ru.yandex.market.pvz.core.domain.shipment.model.ShipmentType;
import ru.yandex.market.pvz.core.test.EmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestOrderFactory;

import static org.assertj.core.api.Assertions.assertThat;

@EmbeddedDbTest
@Import({SendToReturnExpiredOrdersExecutor.class})
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class SendToReturnExpiredOrdersExecutorTest {

    private final TestableClock clock;
    private final TestOrderFactory orderFactory;

    private final OrderRepository orderRepository;

    private final SendToReturnExpiredOrdersExecutor executor;

    private final ShipmentCommandService shipmentCommandService;

    private final OrderDeliveryResultCommandService orderDeliveryResultCommandService;

    @Test
    void testSendToReturnExpiredOrders() {
        Instant arrivalDate = LocalDate.of(2020, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant checkDate = LocalDate.of(2020, 1, 9).atStartOfDay().toInstant(ZoneOffset.UTC);

        clock.setFixed(arrivalDate, clock.getZone());

        Order notReceivedOrder = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .params(TestOrderFactory.OrderParams.builder()
                        .deliveryDate(LocalDate.now(clock))
                        .build())
                .build());

        Order awaitingRecipientOrder = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .params(TestOrderFactory.OrderParams.builder()
                        .deliveryDate(LocalDate.now(clock))
                        .build())
                .build());

        Order fashionFittingStartedOrder = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .params(TestOrderFactory.OrderParams.builder()
                        .deliveryDate(LocalDate.now(clock))
                        .partialDeliveryAllowed(true)
                        .partialDeliveryAvailable(true)
                        .items(List.of(TestOrderFactory.OrderItemParams.builder()
                                .uitValues(List.of("123", "456"))
                                .count(2)
                                .build()))
                        .build())
                .build());

        Order fashionFittingNotStartedOrder = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .params(TestOrderFactory.OrderParams.builder()
                        .deliveryDate(LocalDate.now(clock))
                        .partialDeliveryAllowed(true)
                        .partialDeliveryAvailable(true)
                        .build())
                .build());

        Order dispatchedOrder = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .params(TestOrderFactory.OrderParams.builder()
                        .deliveryDate(LocalDate.now(clock))
                        .build())
                .build());


        createShipmentDispatch(ShipmentType.RECEIVE, awaitingRecipientOrder);
        createShipmentDispatch(ShipmentType.RECEIVE, fashionFittingStartedOrder);
        createShipmentDispatch(ShipmentType.RECEIVE, fashionFittingNotStartedOrder);
        createShipmentDispatch(ShipmentType.RECEIVE, dispatchedOrder);

        dispatchedOrder = orderRepository.findByIdOrThrow(dispatchedOrder.getId());
        dispatchedOrder.setStatusAndCheckpoint(PvzOrderStatus.STORAGE_PERIOD_EXPIRED);
        orderFactory.updateOrder(dispatchedOrder);

        orderDeliveryResultCommandService.startFitting(fashionFittingStartedOrder.getId());

        createShipmentDispatch(ShipmentType.DISPATCH, dispatchedOrder);

        clock.setFixed(checkDate, clock.getZone());
        executor.doRealJob(null);

        notReceivedOrder = orderRepository.findByIdOrThrow(notReceivedOrder.getId());
        awaitingRecipientOrder = orderRepository.findByIdOrThrow(awaitingRecipientOrder.getId());
        fashionFittingStartedOrder = orderRepository.findByIdOrThrow(fashionFittingStartedOrder.getId());
        fashionFittingNotStartedOrder = orderRepository.findByIdOrThrow(fashionFittingNotStartedOrder.getId());
        dispatchedOrder = orderRepository.findByIdOrThrow(dispatchedOrder.getId());

        assertThat(notReceivedOrder.getStatus()).isEqualTo(PvzOrderStatus.CREATED);
        assertThat(awaitingRecipientOrder.getStatus()).isEqualTo(PvzOrderStatus.STORAGE_PERIOD_EXPIRED);
        assertThat(fashionFittingStartedOrder.getStatus()).isEqualTo(PvzOrderStatus.ARRIVED_TO_PICKUP_POINT);
        assertThat(fashionFittingNotStartedOrder.getStatus()).isEqualTo(PvzOrderStatus.STORAGE_PERIOD_EXPIRED);
        assertThat(dispatchedOrder.getStatus()).isEqualTo(PvzOrderStatus.RETURNED_ORDER_WAS_DISPATCHED);
    }

    private Shipment createShipmentDispatch(ShipmentType type, Order order) {
        return shipmentCommandService.createShipment(
                new PickupPointRequestData(
                        order.getPickupPoint().getId(),
                        order.getPickupPoint().getPvzMarketId(),
                        order.getPickupPoint().getName(),
                        1L,
                        order.getPickupPoint().getTimeOffset(),
                        order.getPickupPoint().getStoragePeriod()),
                new ShipmentCreateParams(type, ShipmentStatus.FINISHED, List.of(
                        new ShipmentCreateItemParams(order.getExternalId())
                ))
        );
    }

}
