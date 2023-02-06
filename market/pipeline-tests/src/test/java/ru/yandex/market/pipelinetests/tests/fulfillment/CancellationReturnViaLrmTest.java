package ru.yandex.market.pipelinetests.tests.fulfillment;

import delivery.client.lrm.client.model.LogisticPointType;
import delivery.client.lrm.client.model.ReturnBoxStatus;
import delivery.client.lrm.client.model.ReturnSegmentStatus;
import delivery.client.lrm.client.model.ShipmentDestination;
import dto.requests.checkouter.CreateOrderParameters;
import factory.OfferItems;
import io.qameta.allure.Epic;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import ru.qatools.properties.Property;
import ru.qatools.properties.Resource;
import step.FfwfApiSteps;
import step.ScIntSteps;

import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentDto;
import ru.yandex.market.logistics.lom.model.enums.OrderStatus;
import ru.yandex.market.logistics.lom.model.enums.SegmentType;

@Resource.Classpath({"delivery/checkouter.properties", "delivery/delivery.properties"})
@DisplayName("Blue FF Cancellation Return Test")
@Epic("Blue FF")
@Slf4j
public class CancellationReturnViaLrmTest extends AbstractFulfillmentTest {

    @Property("delivery.sofino")
    private long lastMilePartnerId;

    @Property("delivery.scTarny")
    private long tarnyPartnerId;

    private static final ScIntSteps SC_INT_STEPS = new ScIntSteps();
    private static final FfwfApiSteps FFWF_API_STEPS = new FfwfApiSteps();

    @Tag("FbyCancellationReturnTest")
    @Test
    public void cancellationReturnTest() {
        params = CreateOrderParameters
            .newBuilder(regionId, OfferItems.FF_172_UNFAIR_STOCK.getItems(1, false), DeliveryType.PICKUP)
            .deliveryPredicate(Delivery::isMarketPartner)
            .build();
        order = ORDER_STEPS.createOrder(params);

        lomOrderId = LOM_ORDER_STEPS.getLomOrderId(order);
        orderExternalId = LOM_ORDER_STEPS.getLomOrderData(order).getExternalId();
        LOM_ORDER_STEPS.verifyOrderStatus(lomOrderId, OrderStatus.PROCESSING);

        ORDER_STEPS.verifySDTracksCreated(order);
        ORDER_STEPS.verifyFFTrackCreated(order);

        WaybillSegmentDto sortingCenterWaybillSegment = LOM_ORDER_STEPS.getWaybillSegments(lomOrderId).stream()
            .filter(segment -> segment.getSegmentType() == SegmentType.SORTING_CENTER)
            .findAny()
            .orElseThrow(() -> new RuntimeException("Cannot find SORTING_CENTER segment in waybill"));
        Long scPartnerId = sortingCenterWaybillSegment.getPartnerId();
        Long scLogisticPointId = sortingCenterWaybillSegment.getWarehouseLocation().getWarehouseId();

        ORDER_STEPS.cancelOrder(order);
        LOM_ORDER_STEPS.verifyOrderStatus(lomOrderId, OrderStatus.CANCELLED);

        ShipmentDestination scShipment = LRM_STEPS.verifyScSegmentCreation(
            orderExternalId,
            scPartnerId,
            scLogisticPointId,
            lastMilePartnerId
        );
        LRM_STEPS.verifyLastMileSegmentCreation(
            orderExternalId,
            scShipment.getPartnerId(),
            scShipment.getLogisticPointId(),
            LogisticPointType.FULFILLMENT
        );
        LRM_STEPS.verifySegmentStatus(orderExternalId, scPartnerId, ReturnSegmentStatus.CREATED, true);
        LRM_STEPS.verifyBoxStatus(orderExternalId, ReturnBoxStatus.CREATED);

        SC_INT_STEPS.acceptAndSortOrder(orderExternalId, orderExternalId, tarnyPartnerId);
        LRM_STEPS.verifySegmentStatus(orderExternalId, scPartnerId, ReturnSegmentStatus.TRANSIT_PREPARED);
        LRM_STEPS.verifyBoxStatus(orderExternalId, ReturnBoxStatus.IN_TRANSIT);

        SC_INT_STEPS.shipOrder(orderExternalId, orderExternalId, tarnyPartnerId);
        LRM_STEPS.verifySegmentStatus(orderExternalId, scPartnerId, ReturnSegmentStatus.OUT);
        LRM_STEPS.verifyBoxStatus(orderExternalId, ReturnBoxStatus.IN_TRANSIT);

        FFWF_API_STEPS.confirmBoxRecieved(orderExternalId, orderExternalId, lastMilePartnerId);
        LRM_STEPS.verifySegmentStatus(orderExternalId, lastMilePartnerId, ReturnSegmentStatus.IN);
        LRM_STEPS.verifyBoxStatus(orderExternalId, ReturnBoxStatus.FULFILMENT_RECEIVED);
    }
}
