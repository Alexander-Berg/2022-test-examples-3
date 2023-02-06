package ru.yandex.market.deliveryintegrationtests.delivery.tests.recalculate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;

import dto.requests.checkouter.CreateOrderParameters;
import dto.responses.lgw.LgwTaskFlow;
import factory.OfferItems;
import io.qameta.allure.Epic;
import io.qameta.allure.TmsLink;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import ru.qatools.properties.Resource;

import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.delivery.tracker.domain.enums.OrderDeliveryCheckpointStatus;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentDto;
import ru.yandex.market.logistics.lom.model.enums.OrderStatus;
import ru.yandex.market.logistics.lom.model.enums.SegmentType;

@Resource.Classpath({"delivery/checkouter.properties", "delivery/delivery.properties"})
@DisplayName("Recalculate of delivery date")
@Epic("Recalculate of delivery date")
@Slf4j
public class UpdateDeliveryDateTest extends AbstractRecalculateTest {

    private LocalDate cpaDeliveryDateBefore;
    private LocalDate lomDeliveryDateMaxBefore;
    private LocalDate lomDeliveryDateMinBefore;
    private LocalTime lomDeliveryStartTimeBefore;
    private LocalTime lomDeliveryEndTimeBefore;

    private WaybillSegmentDto movementSegment;
    private WaybillSegmentDto pickupSegment;
    private WaybillSegmentDto ffSegment;
    private OrderDto lomOrder;

    private void createOrderWithOutletId(List<Long> outletId) {
        log.info("Trying to create checkouter order");

        params = CreateOrderParameters
            .newBuilder(regionId, OfferItems.FF_172_UNFAIR_STOCK.getItem(), DeliveryType.PICKUP)
            .outletId(outletId)
            .build();
        order = ORDER_STEPS.createOrder(params);
        cpaDeliveryDateBefore = order.getDelivery().getDeliveryDates().getToDate()
            .toInstant()
            .atZone(ZoneId.systemDefault())
            .toLocalDate();

        lomOrderId = LOM_ORDER_STEPS.getLomOrderId(order);
        LOM_ORDER_STEPS.verifyOrderStatus(lomOrderId, OrderStatus.PROCESSING);

        ORDER_STEPS.verifySDTracksCreated(order);
        ORDER_STEPS.verifyFFTrackCreated(order);

        LOM_ORDER_STEPS.verifyTrackerIds(lomOrderId);

        lomOrder = LOM_ORDER_STEPS.getLomOrderData(order);
        lomDeliveryDateMaxBefore = lomOrder.getDeliveryInterval().getDeliveryDateMax();
        lomDeliveryDateMinBefore = lomOrder.getDeliveryInterval().getDeliveryDateMin();
        lomDeliveryStartTimeBefore = lomOrder.getDeliveryInterval().getFromTime();
        lomDeliveryEndTimeBefore = lomOrder.getDeliveryInterval().getToTime();

        pickupSegment = lomOrder.getWaybill().stream()
            .filter(ws -> ws.getSegmentType() == SegmentType.PICKUP)
            .findAny()
            .orElseGet(() -> Assertions.fail("Не найден сегмент с типом PICKUP"));

        ffSegment = lomOrder.getWaybill().stream()
            .filter(ws -> ws.getSegmentType() == SegmentType.FULFILLMENT)
            .findAny()
            .orElseGet(() -> Assertions.fail("Не найден сегмент с типом FULFILLMENT"));

        movementSegment = lomOrder.getWaybill().stream()
            .filter(ws -> ws.getSegmentType() == SegmentType.MOVEMENT)
            .findAny()
            .orElseGet(() -> Assertions.fail("Не найден сегмент с типом MOVEMENT"));
    }

    @ParameterizedTest(name = "Распрямленный ПВЗ. Изменение даты доставки в средней миле и {2}")
    @TmsLink("logistic-44")
    @MethodSource("getParams")
    @Tag("SmokeTest")
    @Tag("RddTest")
    public void updateDeliveryDateInMiddleMile(long outletId, LgwTaskFlow lgwTaskFlow, String caseName) {
        //создаем заказ с нужной последней милей и запоминаем состояние до
        createOrderWithOutletId(Collections.singletonList(outletId));

        prepareSegmentsForUpdate(
            ffSegment.getTrackerId(),
            movementSegment.getTrackerId(),
            pickupSegment.getTrackerId()
        );

        ORDER_STEPS.verifyForOrderStatus(order, ru.yandex.market.checkout.checkouter.order.OrderStatus.DELIVERY);

        LGW_STEPS.updateOrderDeliveryDate(lomOrder, movementSegment);

        DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(
            movementSegment.getTrackerId(),
            OrderDeliveryCheckpointStatus.DELIVERY_UPDATED_BY_SHOP
        );

        LGW_STEPS.getReadyTaskFromListWithEntityIdAndRequestFlow(
            String.valueOf(order.getId()),
            LgwTaskFlow.DS_GET_ORDERS_DELIVERY_DATE
        );
        LGW_STEPS.getReadyTaskFromListWithEntityIdAndRequestFlow(
            String.valueOf(order.getId()),
            lgwTaskFlow
        );

        LOM_ORDER_STEPS.verifyDontChangeLomDeliveryTime(order, lomDeliveryStartTimeBefore, lomDeliveryEndTimeBefore);
        LOM_ORDER_STEPS.verifyChangeLomDeliveryDate(order, lomDeliveryDateMinBefore, lomDeliveryDateMaxBefore);
        ORDER_STEPS.verifyChangeDeliveryDate(order, cpaDeliveryDateBefore);
    }

    @Test
    @TmsLink("logistic-45")
    @Tag("SmokeTest")
    @DisplayName("Распрямленный ПВЗ. Изменение даты доставки в последней миле")
    public void updateDeliveryDateInLastMile() {
        //создаем заказ с нужной последней милей и запоминаем состояние до
        createOrderWithOutletId(Collections.singletonList(successDeliveryDateUpdatePVZLogisticPoint));

        prepareSegmentsForUpdate(
            ffSegment.getTrackerId(),
            movementSegment.getTrackerId(),
            pickupSegment.getTrackerId()
        );

        ORDER_STEPS.verifyForOrderStatus(order, ru.yandex.market.checkout.checkouter.order.OrderStatus.DELIVERY);

        LGW_STEPS.updateOrderDeliveryDate(lomOrder, pickupSegment);

        DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(
            pickupSegment.getTrackerId(),
            OrderDeliveryCheckpointStatus.DELIVERY_UPDATED_BY_SHOP
        );

        LGW_STEPS.getReadyTaskFromListWithEntityIdAndRequestFlow(
            String.valueOf(order.getId()),
            LgwTaskFlow.DS_GET_ORDERS_DELIVERY_DATE
        );

        LOM_ORDER_STEPS.verifyDontChangeLomDeliveryTime(order, lomDeliveryStartTimeBefore, lomDeliveryEndTimeBefore);
        LOM_ORDER_STEPS.verifyChangeLomDeliveryDate(order, lomDeliveryDateMinBefore, lomDeliveryDateMaxBefore);
        ORDER_STEPS.verifyChangeDeliveryDate(order, cpaDeliveryDateBefore);
    }

}
