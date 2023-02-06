package ru.yandex.market.pipelinetests.tests.faas;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import step.LomOrderSteps;
import step.OrderServiceSteps;
import step.TrackerSteps;

import ru.yandex.market.delivery.tracker.domain.enums.OrderDeliveryCheckpointStatus;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentDto;
import ru.yandex.market.logistics.lom.model.enums.CancellationOrderReason;
import ru.yandex.market.logistics.lom.model.enums.PartnerType;
import ru.yandex.market.logistics.lom.model.enums.PlatformClient;
import ru.yandex.market.logistics.lom.model.enums.SegmentType;
import ru.yandex.market.logistics.lom.model.filter.OrderSearchFilter;
import ru.yandex.market.order_service.client.model.DeliveryOptionsDto;
import ru.yandex.market.order_service.client.model.OrderStatus;
import ru.yandex.market.order_service.client.model.OrderSubStatus;
import ru.yandex.market.order_service.client.model.OrderSubStatus2;

@DisplayName("Создание/отмена Faas заказа")
public class FaasOrderCreationTest {
    private static final long SHOP_ID = 10427354L;

    private static final OrderServiceSteps ORDER_SERVICE_STEPS = new OrderServiceSteps();
    private static final LomOrderSteps LOM_ORDER_STEPS = new LomOrderSteps();
    private static final TrackerSteps TRACKER_STEPS = new TrackerSteps();

    private Long lomOrderId;
    private Long mbiOsOrderId;

    @BeforeEach
    public void init() {
        DeliveryOptionsDto deliveryOptions = ORDER_SERVICE_STEPS.getDeliveryOptions(SHOP_ID);
        mbiOsOrderId = ORDER_SERVICE_STEPS.createOrder(
            SHOP_ID,
            deliveryOptions.getOptions().get(0)
        )
            .getOrderId();

        lomOrderId = LOM_ORDER_STEPS.getLomOrderData(
            OrderSearchFilter.builder()
                .externalIds(Set.of(mbiOsOrderId.toString()))
                .platformClientIds(Set.of(PlatformClient.FAAS.getId()))
                .build()
        )
            .getId();

        LOM_ORDER_STEPS.verifyAllSegmentsAreCreated(lomOrderId);
        ORDER_SERVICE_STEPS.assertOrderStatus(SHOP_ID, mbiOsOrderId, OrderStatus.PROCESSING, OrderSubStatus.PACKAGING);
    }

    @Test
    @DisplayName("Заказ доставлен")
    void orderDelivered() {
        List<WaybillSegmentDto> waybillSegments = LOM_ORDER_STEPS.getWaybillSegments(lomOrderId);
        WaybillSegmentDto ffSegment = waybillSegments
            .stream()
            .filter(segment -> segment.getPartnerType() == PartnerType.FULFILLMENT)
            .findFirst()
            .orElseThrow();
        TRACKER_STEPS.addOrderCheckpointToTracker(
            ffSegment.getTrackerId(),
            OrderDeliveryCheckpointStatus.SORTING_CENTER_TRANSMITTED
        );
        ORDER_SERVICE_STEPS.assertOrderStatus(
            SHOP_ID,
            mbiOsOrderId,
            OrderStatus.DELIVERY,
            OrderSubStatus.DELIVERY_SERVICE_RECEIVED
        );
        WaybillSegmentDto lastMileSegment = waybillSegments.stream()
            .filter(segment -> SegmentType.LAST_MILE_SEGMENT_TYPES.contains(segment.getSegmentType()))
            .findFirst()
            .orElseThrow();
        TRACKER_STEPS.addOrderCheckpointToTracker(
            lastMileSegment.getTrackerId(),
            OrderDeliveryCheckpointStatus.DELIVERY_DELIVERED
        );
        ORDER_SERVICE_STEPS.assertOrderStatus(
            SHOP_ID,
            mbiOsOrderId,
            OrderStatus.DELIVERED,
            OrderSubStatus.DELIVERED_USER_RECEIVED
        );
    }

    @Test
    @DisplayName("Заказ отменён")
    void orderCancelled() {
        ORDER_SERVICE_STEPS.cancelOrder(SHOP_ID, mbiOsOrderId, OrderSubStatus2.SERVICE_FAULT);
        LOM_ORDER_STEPS.verifyOrderCancelled(lomOrderId, CancellationOrderReason.SHOP_CANCELLED);
        ORDER_SERVICE_STEPS.assertOrderStatus(
            SHOP_ID,
            mbiOsOrderId,
            OrderStatus.CANCELLED_IN_PROCESSING,
            OrderSubStatus.USER_CHANGED_MIND
        );
    }
}
