package ru.yandex.market.pipelinetests.tests.fulfillment;

import java.util.List;

import delivery.client.lrm.client.model.LogisticPointType;
import delivery.client.lrm.client.model.ReturnBoxStatus;
import delivery.client.lrm.client.model.ReturnSegmentStatus;
import delivery.client.lrm.client.model.ShipmentDestination;
import dto.requests.checkouter.CreateOrderParameters;
import dto.responses.checkouter.ReturnResponse;
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
import step.TplSteps;

import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.returns.ReturnItem;
import ru.yandex.market.delivery.tracker.domain.enums.OrderDeliveryCheckpointStatus;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentDto;
import ru.yandex.market.logistics.lom.model.enums.OrderStatus;

@Resource.Classpath({"delivery/checkouter.properties", "delivery/delivery.properties"})
@DisplayName("Blue FF Return order Test")
@Epic("Blue FF")
@Slf4j
public class ReturnViaLrmTest extends AbstractFulfillmentTest {

    private static final String BARCODE_PREFIX = "VOZ_FF_";

    @Property("delivery.fashionOutletId")
    private long outletId;
    @Property("delivery.fashionPvzMarketId")
    private long pvzMarketId;

    @Property("delivery.mkScPiterId")
    private long mkSortingCenterId;
    @Property("delivery.secondWaveDS")
    private long mkDeliveryServiceId;

    @Property("delivery.sofinoRet")
    private long lastMilePartnerId;

    @Property("delivery.partnerPVZKurakin")
    private long dsForReturn;

    private static final TplSteps TPL_STEPS = new TplSteps();
    private static final ScIntSteps SC_INT_STEPS = new ScIntSteps();
    private static final FfwfApiSteps FFWF_API_STEPS = new FfwfApiSteps();

    @Test
    @Tag("FashionFbyFullReturnTest")
    @DisplayName("Полный возврат заказа через LRM")
    public void fullReturnTest() {
        params = CreateOrderParameters
                .newBuilder(regionId, OfferItems.FF_172_UNFAIR_STOCK.getItems(1, false), DeliveryType.PICKUP)
                .deliveryPredicate(Delivery::isMarketPartner)
                .build();

        order = ORDER_STEPS.createOrder(params);

        lomOrderId = LOM_ORDER_STEPS.getLomOrderId(order);
        LOM_ORDER_STEPS.verifyOrderStatus(lomOrderId, OrderStatus.PROCESSING);

        ORDER_STEPS.verifySDTracksCreated(order);
        ORDER_STEPS.verifyFFTrackCreated(order);

        WaybillSegmentDto lastMileSegment = LOM_ORDER_STEPS.getWaybillSegmentForPartner(
            lomOrderId,
            order.getDelivery().getDeliveryServiceId()
        );
        DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(
            lastMileSegment.getTrackerId(),
            OrderDeliveryCheckpointStatus.DELIVERY_DELIVERED
        );
        LOM_ORDER_STEPS.verifyOrderStatus(lomOrderId, OrderStatus.DELIVERED);

        List<ReturnItem> returnItems = ORDER_STEPS.initReturnItems(order, true);
        ReturnResponse returnResponse = ORDER_STEPS.initReturn(order, dsForReturn, outletId, returnItems);

        String barcode = BARCODE_PREFIX + returnResponse.getId();

        LRM_STEPS.verifyReturnCommit(barcode);
        LRM_STEPS.verifyPvzSegmentCreation(barcode);

        PVZ_STEPS.receiveReturn(pvzMarketId, returnResponse.getId());

        LRM_STEPS.verifyBoxStatus(barcode, ReturnBoxStatus.RECEIVED);

        TPL_STEPS.receiveReturnFromPvzToSc(barcode, mkSortingCenterId, mkDeliveryServiceId);

        ShipmentDestination pvzShipmentDestination = LRM_STEPS.verifyPvzSegmentShipment(barcode, mkSortingCenterId);

        // Проверка создания сегмента для МК СЦ и получение данных об отгрузке с него на следующий СЦ
        ShipmentDestination mkScShipmentDestination = LRM_STEPS.verifyScSegmentCreation(
            barcode,
            pvzShipmentDestination.getPartnerId(),
            pvzShipmentDestination.getLogisticPointId(),
            lastMilePartnerId
        );

        SC_INT_STEPS.acceptAndSortOrder(barcode, barcode, mkSortingCenterId);

        LRM_STEPS.verifySegmentStatus(barcode, mkSortingCenterId, ReturnSegmentStatus.TRANSIT_PREPARED);
        LRM_STEPS.verifyBoxStatus(barcode, ReturnBoxStatus.IN_TRANSIT);

        LRM_STEPS.verifyLastMileSegmentCreation(
            barcode,
            mkScShipmentDestination.getPartnerId(),
            mkScShipmentDestination.getLogisticPointId(),
            LogisticPointType.FULFILLMENT
        );

        SC_INT_STEPS.shipOrder(barcode, barcode, mkSortingCenterId);

        LRM_STEPS.verifySegmentStatus(barcode, mkSortingCenterId, ReturnSegmentStatus.OUT);
        LRM_STEPS.verifyBoxStatus(barcode, ReturnBoxStatus.IN_TRANSIT);

        FFWF_API_STEPS.confirmBoxRecieved(orderExternalId, barcode, lastMilePartnerId);
        LRM_STEPS.verifySegmentStatus(barcode, lastMilePartnerId, ReturnSegmentStatus.IN);
        LRM_STEPS.verifyBoxStatus(barcode, ReturnBoxStatus.FULFILMENT_RECEIVED);
    }
}
