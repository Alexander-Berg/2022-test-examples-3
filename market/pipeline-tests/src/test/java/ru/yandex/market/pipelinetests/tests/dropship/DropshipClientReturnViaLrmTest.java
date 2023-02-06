package ru.yandex.market.pipelinetests.tests.dropship;

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
import step.LrmSteps;
import step.PartnerApiSteps;
import step.ScIntSteps;
import step.TplPvzSteps;
import step.TplSteps;

import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.returns.ReturnItem;
import ru.yandex.market.delivery.tracker.domain.enums.OrderDeliveryCheckpointStatus;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentDto;
import ru.yandex.market.logistics.lom.model.enums.OrderStatus;

@Resource.Classpath({"delivery/checkouter.properties", "delivery/delivery.properties"})
@DisplayName("Blue Dropship Return order Test")
@Epic("Blue Dropship")
@Slf4j
public class DropshipClientReturnViaLrmTest extends AbstractDropshipTest {

    private static final String BARCODE_PREFIX = "VOZ_FBS_";

    @Property("delivery.fashionOutletId")
    private long outletId;
    @Property("delivery.fashionPvzMarketId")
    private long pvzMarketId;

    @Property("delivery.mkScPiterId")
    private long mkSortingCenterId;
    @Property("delivery.secondWaveDS")
    private long mkDeliveryServiceId;

    @Property("delivery.scHamovniki")
    private long middleMileSortingCenterId;

    @Property("delivery.dropshipSc")
    private long lastMilePartnerId;

    @Property("reportblue.dropshipSCCampaignId")
    private long dropshipSCCampaignId;

    @Property("reportblue.dropshipSCUID")
    private long dropshipSCUID;

    @Property("delivery.partnerPVZKurakin")
    private long dsForReturn;

    private static final TplSteps TPL_STEPS = new TplSteps();
    private static final LrmSteps LRM_STEPS = new LrmSteps();
    private static final TplPvzSteps PVZ_STEPS = new TplPvzSteps();
    private static final ScIntSteps SC_INT_STEPS = new ScIntSteps();

    @Test
    @Tag("DropshipFullClientReturnTest")
    @DisplayName("Полный возврат ДШ заказа через LRM")
    public void fullReturnTest() {
        partnerApiSteps = new PartnerApiSteps(dropshipSCUID, dropshipSCCampaignId);

        params = CreateOrderParameters
            .newBuilder(regionId, OfferItems.DROPSHIP_SC.getItems(1, false), DeliveryType.PICKUP)
            .deliveryPredicate(Delivery::isMarketPartner)
            .build();
        order = ORDER_STEPS.createOrder(params);

        lomOrderId = LOM_ORDER_STEPS.getLomOrderId(order);
        LOM_ORDER_STEPS.verifyOrderStatus(lomOrderId, OrderStatus.PROCESSING);

        ORDER_STEPS.verifySDTracksCreated(order);

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
            middleMileSortingCenterId
        );

        SC_INT_STEPS.acceptAndSortOrder(barcode, barcode, mkSortingCenterId);

        LRM_STEPS.verifySegmentStatus(barcode, mkSortingCenterId, ReturnSegmentStatus.TRANSIT_PREPARED);
        LRM_STEPS.verifyBoxStatus(barcode, ReturnBoxStatus.IN_TRANSIT);

        // Проверка создания сегмента для СЦ средней мили и получение данных об отгрузке с него на последнюю милю
        ShipmentDestination middleMileScShipmentDestination = LRM_STEPS.verifyScSegmentCreation(
            barcode,
            mkScShipmentDestination.getPartnerId(),
            mkScShipmentDestination.getLogisticPointId(),
            lastMilePartnerId
        );

        SC_INT_STEPS.shipOrder(barcode, barcode, mkSortingCenterId);
        SC_INT_STEPS.acceptAndSortOrder(barcode, barcode, middleMileSortingCenterId);

        LRM_STEPS.verifySegmentStatus(barcode, mkSortingCenterId, ReturnSegmentStatus.OUT);
        LRM_STEPS.verifySegmentStatus(barcode, middleMileSortingCenterId, ReturnSegmentStatus.TRANSIT_PREPARED);
        LRM_STEPS.verifyBoxStatus(barcode, ReturnBoxStatus.READY_FOR_RETURN);

        LRM_STEPS.verifyLastMileSegmentCreation(
            barcode,
            middleMileScShipmentDestination.getPartnerId(),
            middleMileScShipmentDestination.getLogisticPointId(),
            LogisticPointType.SHOP
        );

        SC_INT_STEPS.shipOrder(barcode, barcode, middleMileSortingCenterId);

        LRM_STEPS.verifySegmentStatus(barcode, middleMileSortingCenterId, ReturnSegmentStatus.OUT);
        LRM_STEPS.verifyBoxStatus(barcode, ReturnBoxStatus.DELIVERED);
    }
}
