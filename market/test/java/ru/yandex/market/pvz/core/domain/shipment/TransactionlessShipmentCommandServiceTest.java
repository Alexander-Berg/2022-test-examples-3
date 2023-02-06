package ru.yandex.market.pvz.core.domain.shipment;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.pvz.core.domain.dispatch.model.DispatchType;
import ru.yandex.market.pvz.core.domain.order.OrderCommandService;
import ru.yandex.market.pvz.core.domain.order.OrderParamsMapper;
import ru.yandex.market.pvz.core.domain.order.OrderRepository;
import ru.yandex.market.pvz.core.domain.order.model.Order;
import ru.yandex.market.pvz.core.domain.order.model.history.OrderHistoryRecord;
import ru.yandex.market.pvz.core.domain.order.model.history.OrderHistoryRepository;
import ru.yandex.market.pvz.core.domain.order.model.params.OrderUpdateParams;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPointRequestData;
import ru.yandex.market.pvz.core.domain.returns.ReturnRequestRepository;
import ru.yandex.market.pvz.core.domain.returns.model.ReturnRequest;
import ru.yandex.market.pvz.core.domain.returns.model.ReturnStatus;
import ru.yandex.market.pvz.core.domain.shipment.model.Shipment;
import ru.yandex.market.pvz.core.domain.shipment.model.ShipmentCreateItemParams;
import ru.yandex.market.pvz.core.domain.shipment.model.ShipmentCreateParams;
import ru.yandex.market.pvz.core.domain.shipment.model.ShipmentItem;
import ru.yandex.market.pvz.core.domain.shipment.model.ShipmentItemParams;
import ru.yandex.market.pvz.core.domain.shipment.model.ShipmentParams;
import ru.yandex.market.pvz.core.domain.shipment.model.ShipmentType;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestOrderFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;
import ru.yandex.market.pvz.core.test.factory.TestReturnRequestFactory;
import ru.yandex.market.tpl.common.util.configuration.ConfigurationProvider;
import ru.yandex.market.tpl.common.web.exception.TplInvalidActionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.market.pvz.core.domain.order.model.PvzOrderStatus.ARRIVED_TO_PICKUP_POINT;
import static ru.yandex.market.pvz.core.domain.order.model.PvzOrderStatus.CANCELLED;
import static ru.yandex.market.pvz.core.domain.order.model.PvzOrderStatus.CREATED;
import static ru.yandex.market.pvz.core.domain.order.model.PvzOrderStatus.READY_FOR_RETURN;
import static ru.yandex.market.pvz.core.domain.shipment.model.ShipmentStatus.FINISHED;
import static ru.yandex.market.pvz.core.domain.shipment.model.ShipmentType.RECEIVE;
import static ru.yandex.market.pvz.core.test.TestExternalConfiguration.DEFAULT_UID;

@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class TransactionlessShipmentCommandServiceTest {

    private final TestableClock clock;

    private final TestPickupPointFactory pickupPointFactory;
    private final TestOrderFactory orderFactory;
    private final TestReturnRequestFactory returnFactory;

    private final OrderHistoryRepository orderHistoryRepository;
    private final ReturnRequestRepository returnRequestRepository;
    private final OrderRepository orderRepository;
    private final ShipmentRepository shipmentRepository;
    private final ShipmentItemRepository shipmentItemRepository;

    private final OrderCommandService orderCommandService;
    private final OrderParamsMapper orderParamsMapper;
    private final ConfigurationProvider configurationProvider;

    private final ShipmentCommandService shipmentCommandService;

    @Test
    void shouldReceiveOrders() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint();
        Order order1 = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .build());

        PickupPointRequestData pickupPointAuthInfo = new PickupPointRequestData(
                order1.getPickupPoint().getId(),
                order1.getPickupPoint().getPvzMarketId(),
                order1.getPickupPoint().getName(),
                1L,
                order1.getPickupPoint().getTimeOffset(),
                order1.getPickupPoint().getStoragePeriod()
        );
        Shipment shipment = shipmentCommandService.createShipment(pickupPointAuthInfo, new ShipmentCreateParams(
                RECEIVE, FINISHED,
                List.of(
                        new ShipmentCreateItemParams(order1.getExternalId())
                )));
        order1 = orderRepository.findByIdOrThrow(order1.getId());

        assertThat(shipment).isNotNull();
        assertThat(order1.getStatus()).isEqualTo(ARRIVED_TO_PICKUP_POINT);
        assertThat(
                orderHistoryRepository.getOrderHistory(order1.getId()).stream()
                        .map(OrderHistoryRecord::getStatus)
                        .collect(Collectors.toList()))
                .containsExactly(CREATED, ARRIVED_TO_PICKUP_POINT);
    }

    @Test
    void shouldReceiveOrdersThatBeenCancelled() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint();
        var order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .build());
        OrderUpdateParams orderUpdateParams = orderParamsMapper.mapUpdate(order, configurationProvider);
        orderCommandService.cancel(orderUpdateParams);
        var pickupPointAuthInfo = new PickupPointRequestData(order.getPickupPoint().getId(),
                order.getPickupPoint().getPvzMarketId(), order.getPickupPoint().getName(), 1L,
                order.getPickupPoint().getTimeOffset(), order.getPickupPoint().getStoragePeriod()
        );
        var shipment = shipmentCommandService.createShipment(pickupPointAuthInfo, new ShipmentCreateParams(
                RECEIVE, FINISHED, List.of(new ShipmentCreateItemParams(order.getExternalId()))));
        order = orderRepository.findByIdOrThrow(order.getId());

        assertThat(shipment).isNotNull();
        assertThat(order.getStatus()).isEqualTo(READY_FOR_RETURN);
        assertThat(
                orderHistoryRepository.getOrderHistory(order.getId()).stream()
                        .map(OrderHistoryRecord::getStatus)
                        .collect(Collectors.toList()))
                .containsExactly(CREATED, CANCELLED, ARRIVED_TO_PICKUP_POINT, READY_FOR_RETURN);
    }

    @Test
    void shouldDispatchReturns() {
        var pickupPoint = pickupPointFactory.createPickupPoint();

        var returnRequest = returnFactory.createReturnRequest(
                TestReturnRequestFactory.CreateReturnRequestBuilder.builder()
                        .pickupPoint(pickupPoint)
                        .build()
        );

        returnRequest = returnFactory.receiveReturnRequest(returnRequest.getReturnId());

        PickupPointRequestData pickupPointAuthInfo = new PickupPointRequestData(
                pickupPoint.getId(),
                pickupPoint.getPvzMarketId(),
                pickupPoint.getName(),
                1L,
                pickupPoint.getTimeOffset(),
                pickupPoint.getStoragePeriod()
        );
        Shipment shipment = shipmentCommandService.createShipment(pickupPointAuthInfo, new ShipmentCreateParams(
                ShipmentType.DISPATCH, FINISHED,
                List.of(
                        getReturnShipmentItem(returnRequest.getReturnId())
                )));

        assertThat(shipment).isNotNull();

        ReturnRequest returnRequestUpdated = returnRequestRepository.findById(returnRequest.getId()).orElseThrow();

        assertThat(returnRequestUpdated.getStatus()).isEqualTo(ReturnStatus.DISPATCHED);
        assertThat(returnRequestUpdated.getDispatchedAt()).isEqualTo(OffsetDateTime.now(clock));

    }

    private ShipmentCreateItemParams getReturnShipmentItem(String returnId) {
        ShipmentCreateItemParams shipmentCreateItemDto = new ShipmentCreateItemParams(returnId);
        shipmentCreateItemDto.setType(DispatchType.RETURN);
        return shipmentCreateItemDto;
    }

    @Test
    void createReceive() {
        var pickupPoint = pickupPointFactory.createPickupPoint();
        LocalDateTime createdDateTime = LocalDateTime.of(2022, 3, 11, 10, 40);
        ZoneOffset offset = ZoneOffset.ofHours(pickupPoint.getTimeOffset());
        Instant createdInstant = createdDateTime.toInstant(offset);
        clock.setFixed(createdInstant, offset);

        var createdOrder = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .params(TestOrderFactory.OrderParams.builder()
                        .deliveryDate(createdDateTime.toLocalDate().plusDays(1))
                        .build())
                .build());
        var cancelledOrder = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .params(TestOrderFactory.OrderParams.builder()
                        .deliveryDate(createdDateTime.toLocalDate().plusDays(1))
                        .build())
                .build());
        orderFactory.cancelOrder(cancelledOrder.getId());

        PickupPointRequestData pickupPointRequestData = new PickupPointRequestData(
                pickupPoint.getId(), pickupPoint.getPvzMarketId(), pickupPoint.getName(), DEFAULT_UID,
                pickupPoint.getTimeOffset(), pickupPoint.getStoragePeriod());
        ShipmentParams actual = shipmentCommandService.createReceive(
                pickupPointRequestData, List.of(createdOrder.getExternalId(), cancelledOrder.getExternalId()));

        ShipmentParams expectedShipment = ShipmentParams.builder()
                .type(RECEIVE)
                .status(FINISHED)
                .responsibleUserId(DEFAULT_UID)
                .shipmentDate(createdDateTime.toLocalDate())
                .createdAt(createdInstant)
                .updatedAt(createdInstant)
                .pickupPointId(pickupPoint.getId())
                .items(List.of(
                        ShipmentItemParams.builder()
                                .orderId(cancelledOrder.getId())
                                .createdAt(createdInstant)
                                .build(),
                        ShipmentItemParams.builder()
                                .orderId(createdOrder.getId())
                                .createdAt(createdInstant)
                                .build())).build();
        assertThat(actual).usingRecursiveComparison()
                .ignoringOverriddenEqualsForFields("items")
                .ignoringFields("id", "items.id", "items.shipmentId")
                .isEqualTo(expectedShipment);

        List<ShipmentItemParams> expectedShipmentItems = List.of(
                ShipmentItemParams.builder()
                        .orderId(createdOrder.getId())
                        .build(),
                ShipmentItemParams.builder()
                        .orderId(cancelledOrder.getId())
                        .build()
        );
        assertThat(StreamEx.of(expectedShipmentItems).map(ShipmentItemParams::getOrderId).toList())
                .containsExactlyInAnyOrder(createdOrder.getId(), cancelledOrder.getId());

        var shipments = shipmentRepository.findAll();
        assertThat(shipments).hasSize(1);
        var shipment = shipments.get(0);
        assertThat(shipment.getShipmentDate()).isEqualTo(createdDateTime.toLocalDate());
        assertThat(shipment.getType()).isEqualTo(RECEIVE);
        assertThat(shipment.getStatus()).isEqualTo(FINISHED);

        List<ShipmentItem> shipmentItems = shipmentItemRepository.findAllByShipmentId(shipment.getId());
        assertThat(shipmentItems).hasSize(2);
        assertThat(StreamEx.of(shipmentItems).map(si -> si.getOrder().getId()).toList())
                .containsExactlyInAnyOrder(createdOrder.getId(), cancelledOrder.getId());
    }

    @Test
    void tryToCreateReceiveWithInvalidStatusOrder() {
        var pickupPoint = pickupPointFactory.createPickupPoint();
        LocalDateTime createdDateTime = LocalDateTime.of(2022, 3, 11, 10, 40);
        ZoneOffset offset = ZoneOffset.ofHours(pickupPoint.getTimeOffset());

        clock.setFixed(createdDateTime.toInstant(offset), offset);

        var order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .params(TestOrderFactory.OrderParams.builder()
                        .deliveryDate(createdDateTime.toLocalDate().plusDays(1))
                        .build())
                .build());
        orderFactory.receiveOrder(order.getId());

        PickupPointRequestData pickupPointRequestData = new PickupPointRequestData(
                pickupPoint.getId(), pickupPoint.getPvzMarketId(), pickupPoint.getName(), DEFAULT_UID,
                pickupPoint.getTimeOffset(), pickupPoint.getStoragePeriod());
        assertThatThrownBy(() -> shipmentCommandService.createReceive(
                pickupPointRequestData, List.of(order.getExternalId())))
                .isExactlyInstanceOf(TplInvalidActionException.class);
    }

    @Test
    void tryToCreateReceiveWithVeryOldDeliveryDateOrder() {
        var pickupPoint = pickupPointFactory.createPickupPoint();
        LocalDateTime createdDateTime = LocalDateTime.of(2022, 3, 11, 10, 40);
        ZoneOffset offset = ZoneOffset.ofHours(pickupPoint.getTimeOffset());

        clock.setFixed(createdDateTime.toInstant(offset), offset);

        var order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .params(TestOrderFactory.OrderParams.builder()
                        .deliveryDate(createdDateTime.toLocalDate().minusYears(2))
                        .build())
                .build());

        PickupPointRequestData pickupPointRequestData = new PickupPointRequestData(
                pickupPoint.getId(), pickupPoint.getPvzMarketId(), pickupPoint.getName(), DEFAULT_UID,
                pickupPoint.getTimeOffset(), pickupPoint.getStoragePeriod());
        assertThatThrownBy(() -> shipmentCommandService.createReceive(
                pickupPointRequestData, List.of(order.getExternalId())))
                .isExactlyInstanceOf(TplInvalidActionException.class);
    }
}
