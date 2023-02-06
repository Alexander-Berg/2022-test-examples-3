package ru.yandex.market.pvz.core.domain.shipment;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pvz.core.domain.dispatch.model.DispatchType;
import ru.yandex.market.pvz.core.domain.order.OrderQueryService;
import ru.yandex.market.pvz.core.domain.order.model.Order;
import ru.yandex.market.pvz.core.domain.order.model.PvzOrderStatus;
import ru.yandex.market.pvz.core.domain.order.model.history.OrderHistoryRecord;
import ru.yandex.market.pvz.core.domain.order.model.history.OrderHistoryRepository;
import ru.yandex.market.pvz.core.domain.order.model.params.OrderSimpleParams;
import ru.yandex.market.pvz.core.domain.order_delivery_result.ItemDeliveryFlow;
import ru.yandex.market.pvz.core.domain.order_delivery_result.PartialDeliveryStatus;
import ru.yandex.market.pvz.core.domain.order_delivery_result.service.OrderDeliveryResultCommandService;
import ru.yandex.market.pvz.core.domain.order_delivery_result.service.OrderDeliveryResultQueryService;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPointRequestData;
import ru.yandex.market.pvz.core.domain.shipment.model.Shipment;
import ru.yandex.market.pvz.core.domain.shipment.model.ShipmentCreateItemParams;
import ru.yandex.market.pvz.core.domain.shipment.model.ShipmentCreateParams;
import ru.yandex.market.pvz.core.domain.shipment.model.ShipmentStatus;
import ru.yandex.market.pvz.core.domain.shipment.model.ShipmentType;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestOrderFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;
import ru.yandex.market.pvz.core.test.factory.TestShipmentsFactory;
import ru.yandex.market.tpl.common.web.exception.TplInvalidActionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.market.pvz.core.domain.order.model.PvzOrderStatus.READY_FOR_RETURN;
import static ru.yandex.market.pvz.core.domain.order.model.PvzOrderStatus.RETURNED_ORDER_WAS_DISPATCHED;
import static ru.yandex.market.pvz.core.domain.order.model.PvzOrderStatus.STORAGE_PERIOD_EXPIRED;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.UIT_1_1;

@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class ShipmentCommandServiceTest {

    private final TestPickupPointFactory pickupPointFactory;
    private final TestOrderFactory orderFactory;
    private final OrderHistoryRepository orderHistoryRepository;
    private final ShipmentCommandService shipmentCommandService;
    private final TestShipmentsFactory shipmentsFactory;
    private final ShipmentRepository shipmentRepository;
    private final OrderQueryService orderQueryService;

    private final OrderDeliveryResultCommandService orderDeliveryResultCommandService;
    private final OrderDeliveryResultQueryService orderDeliveryResultQueryService;

    @Test
    void shouldDispatchOrders() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointFromCrm();
        Order order1 = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .build());
        Order order2 = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .build());

        orderFactory.setStatusAndCheckpoint(order1.getId(), READY_FOR_RETURN);
        orderFactory.setStatusAndCheckpoint(order2.getId(), STORAGE_PERIOD_EXPIRED);

        PickupPointRequestData pickupPointAuthInfo = new PickupPointRequestData(
                pickupPoint.getId(), pickupPoint.getPvzMarketId(), pickupPoint.getName(), 1L,
                pickupPoint.getTimeOffset(), pickupPoint.getStoragePeriod());
        Shipment shipment = shipmentCommandService.createShipment(pickupPointAuthInfo, new ShipmentCreateParams(
                ShipmentType.DISPATCH, ShipmentStatus.FINISHED,
                List.of(
                        new ShipmentCreateItemParams(order1.getExternalId()),
                        new ShipmentCreateItemParams(order2.getExternalId())
                )));
        assertThat(shipment).isNotNull();

        OrderSimpleParams orderParams1 = orderQueryService.getSimple(order1.getId());
        OrderSimpleParams orderParams2 = orderQueryService.getSimple(order2.getId());

        assertThat(orderParams1.getStatus()).isEqualTo(RETURNED_ORDER_WAS_DISPATCHED);
        assertThat(orderParams2.getStatus()).isEqualTo(RETURNED_ORDER_WAS_DISPATCHED);

        assertThat(
                orderHistoryRepository.getOrderHistory(order1.getId()).stream()
                        .map(OrderHistoryRecord::getStatus)
                        .collect(Collectors.toList()))
                .endsWith(READY_FOR_RETURN, RETURNED_ORDER_WAS_DISPATCHED);

        assertThat(
                orderHistoryRepository.getOrderHistory(order2.getId()).stream()
                        .map(OrderHistoryRecord::getStatus)
                        .collect(Collectors.toList()))
                .endsWith(STORAGE_PERIOD_EXPIRED, RETURNED_ORDER_WAS_DISPATCHED);
    }

    @Test
    void shouldNotReceiveOrder() {
        List<PvzOrderStatus> statusesNotToReceiveWith = Stream.of(PvzOrderStatus.values())
                .filter(s -> !PvzOrderStatus.TO_RECEIVE_STATUSES.contains(s))
                .collect(Collectors.toList());

        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder().build());
        PickupPointRequestData pickupPoint = new PickupPointRequestData(1L, RandomUtils.nextLong(),
                "Подвал дома Колотушкина на улице Пушкина", 1L, 3, 7);


        for (PvzOrderStatus status : statusesNotToReceiveWith) {
            orderFactory.setStatusAndCheckpoint(order.getId(), status);

            assertThatThrownBy(() -> shipmentCommandService.createShipment(pickupPoint, new ShipmentCreateParams(
                    ShipmentType.RECEIVE, ShipmentStatus.FINISHED,
                    List.of(new ShipmentCreateItemParams(order.getExternalId())))));
        }
    }

    @Test
    void shouldNotDispatchOrder() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointFromCrm();
        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .build());

        Set<PvzOrderStatus> statusesNotToReceiveWith = EnumSet.allOf(PvzOrderStatus.class);
        statusesNotToReceiveWith.removeAll(PvzOrderStatus.TO_DISPATCH_STATUSES);

        PickupPointRequestData pickupPointRequestData = new PickupPointRequestData(
                pickupPoint.getId(), pickupPoint.getPvzMarketId(), pickupPoint.getName(), 1L,
                pickupPoint.getTimeOffset(), pickupPoint.getStoragePeriod());

        for (PvzOrderStatus status : statusesNotToReceiveWith) {
            orderFactory.setStatusAndCheckpoint(order.getId(), status);

            ShipmentCreateParams shipmentCreateParams = new ShipmentCreateParams(ShipmentType.DISPATCH,
                    ShipmentStatus.FINISHED, List.of(new ShipmentCreateItemParams(order.getExternalId())));
            assertThatThrownBy(() ->
                    shipmentCommandService.createShipment(pickupPointRequestData, shipmentCreateParams))
                    .isExactlyInstanceOf(TplInvalidActionException.class);
        }
    }

    @ParameterizedTest(name = "Transfer in state {0} set order to state {1}")
    @CsvSource({"FINISHED,ARRIVED_TO_PICKUP_POINT", "CANCELLED,CREATED", "PENDING,CREATED"})
    void updateTransferStateUpdateShouldFinishReceiveIfNeeded(
            ShipmentStatus shipmentStatus, PvzOrderStatus orderStatus) {
        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder().build());
        Shipment shipment = shipmentsFactory.createPendingShipment(order);
        shipmentCommandService.closeReceive(shipment.getTransferId(), shipmentStatus);

        Shipment finishedShipment = shipmentRepository.findByIdOrThrow(shipment.getId());
        assertThat(finishedShipment.getStatus()).isEqualTo(shipmentStatus);
        OrderSimpleParams orderParams = orderQueryService.getSimple(order.getId());
        assertThat(orderParams.getStatus()).isEqualTo(orderStatus);
    }

    @Test
    void shouldDispatchSafePackages() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointFromCrm();
        Order order = orderFactory.createSimpleFashionOrder(false, pickupPoint);
        order = orderFactory.receiveOrder(order.getId());

        orderDeliveryResultCommandService.startFitting(order.getId());
        orderDeliveryResultCommandService.updateItemFlow(order.getId(), UIT_1_1, ItemDeliveryFlow.RETURN);
        orderDeliveryResultCommandService.finishFitting(order.getId());
        orderDeliveryResultCommandService.pay(order.getId());
        orderDeliveryResultCommandService.packageReturn(order.getId(), List.of("Package1", "Package2"));

        PickupPointRequestData pickupPointAuthInfo = new PickupPointRequestData(
                pickupPoint.getId(), pickupPoint.getPvzMarketId(), pickupPoint.getName(), 1L,
                pickupPoint.getTimeOffset(), pickupPoint.getStoragePeriod());
        Shipment shipment = shipmentCommandService.createShipment(pickupPointAuthInfo, new ShipmentCreateParams(
                ShipmentType.DISPATCH, ShipmentStatus.FINISHED,
                List.of(
                        getSafePackageShipmentItem(order.getExternalId())
                )));

        assertThat(shipment).isNotNull();
        assertThat(orderDeliveryResultQueryService.get(order.getId()).getStatus())
                .isEqualTo(PartialDeliveryStatus.DISPATCHED);

    }

    private ShipmentCreateItemParams getSafePackageShipmentItem(String orderId) {
        ShipmentCreateItemParams shipmentCreateItemDto = new ShipmentCreateItemParams(orderId);
        shipmentCreateItemDto.setType(DispatchType.SAFE_PACKAGE);
        return shipmentCreateItemDto;
    }

}
