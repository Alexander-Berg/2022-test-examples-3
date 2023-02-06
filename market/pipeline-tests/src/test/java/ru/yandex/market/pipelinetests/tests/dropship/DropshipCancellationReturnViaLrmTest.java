package ru.yandex.market.pipelinetests.tests.dropship;

import java.util.List;

import delivery.client.lrm.client.model.ReturnBoxStatus;
import delivery.client.lrm.client.model.ReturnSegmentStatus;
import dto.requests.checkouter.CreateOrderParameters;
import factory.OfferItems;
import io.qameta.allure.Epic;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import ru.qatools.properties.Property;
import ru.qatools.properties.Resource;
import step.LrmSteps;
import step.PartnerApiSteps;
import step.ScIntSteps;

import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentDto;
import ru.yandex.market.logistics.lom.model.enums.OrderStatus;
import ru.yandex.market.logistics.lom.model.enums.SegmentType;

@Resource.Classpath({"delivery/checkouter.properties", "delivery/delivery.properties"})
@DisplayName("Blue Dropship Cancellation Return Test")
@Epic("Blue Dropship")
@Slf4j
public class DropshipCancellationReturnViaLrmTest extends AbstractDropshipTest {

    @Property("reportblue.dropshipSCCampaignId")
    private long dropshipSCCampaignId;

    @Property("reportblue.dropshipSCUID")
    private long dropshipSCUID;

    @Property("delivery.scHamovniki")
    private long firstScPartnerId;

    @Property("delivery.dropshipSc")
    private long lastMilePartnerId;

    private static final LrmSteps LRM_STEPS = new LrmSteps();
    private static final ScIntSteps SC_INT_STEPS = new ScIntSteps();

    @Test
    @Tag("DropshipCancellationReturnTest")
    void cancellationReturn() {
        partnerApiSteps = new PartnerApiSteps(dropshipSCUID, dropshipSCCampaignId);
        params = CreateOrderParameters
            .newBuilder(regionId, OfferItems.DROPSHIP_SC.getItems(1, false), DeliveryType.PICKUP)
            .deliveryPredicate(Delivery::isMarketPartner)
            .build();
        order = ORDER_STEPS.createOrder(params);

        lomOrderId = LOM_ORDER_STEPS.getLomOrderId(order);
        orderExternalId = LOM_ORDER_STEPS.getLomOrderData(order).getExternalId();
        LOM_ORDER_STEPS.verifyOrderStatus(lomOrderId, OrderStatus.PROCESSING);
        ORDER_STEPS.verifySDTracksCreated(order);

        ORDER_STEPS.cancelOrder(order);
        LOM_ORDER_STEPS.verifyOrderStatus(lomOrderId, OrderStatus.CANCELLED);

        List<WaybillSegmentDto> waybillSegments = LOM_ORDER_STEPS.getWaybillSegments(lomOrderId);
        WaybillSegmentDto firstScSegment = waybillSegments.stream()
            .filter(segment -> segment.getPartnerId().equals(firstScPartnerId))
            .findAny()
            .orElseThrow(
                () -> new RuntimeException("Cannot find waybill segment for partner %s".formatted(firstScPartnerId))
            );
        long firstScLogisticPointId = firstScSegment.getWarehouseLocation().getWarehouseId();
        WaybillSegmentDto secondScSegment = waybillSegments.stream()
            .filter(segment -> segment.getSegmentType() == SegmentType.SORTING_CENTER)
            .filter(segment -> !segment.getPartnerId().equals(firstScPartnerId))
            .findAny()
            .orElseThrow(() -> new RuntimeException("Cannot find second SORTING_CENTER in waybill segments"));
        long secondScPartnerId = secondScSegment.getPartnerId();
        long secondScLogisticPointId = secondScSegment.getWarehouseLocation().getWarehouseId();

        LRM_STEPS.verifyScSegmentCreation(
            orderExternalId,
            firstScPartnerId,
            firstScLogisticPointId,
            lastMilePartnerId
        );
        LRM_STEPS.verifyScSegmentCreation(
            orderExternalId,
            secondScPartnerId,
            secondScLogisticPointId,
            firstScPartnerId
        );
        LRM_STEPS.verifySegmentStatus(orderExternalId, firstScPartnerId, ReturnSegmentStatus.CREATED, true);
        LRM_STEPS.verifySegmentStatus(orderExternalId, secondScPartnerId, ReturnSegmentStatus.CREATED, true);
        LRM_STEPS.verifyBoxStatus(orderExternalId, ReturnBoxStatus.CREATED);

        SC_INT_STEPS.acceptPlace(orderExternalId, orderExternalId, firstScPartnerId);
        LRM_STEPS.verifySegmentStatus(orderExternalId, firstScPartnerId, ReturnSegmentStatus.IN, true);
        LRM_STEPS.verifySegmentStatus(orderExternalId, secondScPartnerId, ReturnSegmentStatus.CANCELLED);
        LRM_STEPS.verifyBoxStatus(orderExternalId, ReturnBoxStatus.DESTINATION_POINT_RECEIVED);

        SC_INT_STEPS.sortPlace(orderExternalId, firstScPartnerId);
        SC_INT_STEPS.sortPlace(orderExternalId, firstScPartnerId);
        LRM_STEPS.verifySegmentStatus(orderExternalId, firstScPartnerId, ReturnSegmentStatus.TRANSIT_PREPARED);
        LRM_STEPS.verifyBoxStatus(orderExternalId, ReturnBoxStatus.READY_FOR_RETURN);

        SC_INT_STEPS.shipOrder(orderExternalId, orderExternalId, firstScPartnerId);
        LRM_STEPS.verifySegmentStatus(orderExternalId, firstScPartnerId, ReturnSegmentStatus.OUT);
        LRM_STEPS.verifyBoxStatus(orderExternalId, ReturnBoxStatus.DELIVERED);
    }
}
