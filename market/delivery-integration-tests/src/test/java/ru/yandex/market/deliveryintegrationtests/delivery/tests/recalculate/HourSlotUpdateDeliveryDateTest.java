package ru.yandex.market.deliveryintegrationtests.delivery.tests.recalculate;

import dto.requests.checkouter.Address;
import dto.requests.checkouter.CreateOrderParameters;
import dto.requests.checkouter.RearrFactor;
import factory.OfferItems;
import io.qameta.allure.Epic;
import io.qameta.allure.TmsLink;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.qatools.properties.Property;
import ru.qatools.properties.Resource;
import ru.yandex.market.checkout.checkouter.delivery.*;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.enums.OrderStatus;
import toolkit.Pair;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;

@Resource.Classpath({"delivery/checkouter.properties", "delivery/delivery.properties"})
@DisplayName("Recalculate of delivery date")
@Epic("Recalculate of delivery date")
@Slf4j
public class HourSlotUpdateDeliveryDateTest extends AbstractRecalculateTest {

    @Property("delivery.hourSlot")
    private long hourSlot;

    private LocalDate cpaDeliveryDateBefore;
    private LocalTime lomDeliveryStartTimeBefore;
    private LocalTime lomDeliveryEndTimeBefore;
    private LocalDate lomDeliveryDateMaxBefore;
    private LocalDate lomDeliveryDateMinBefore;

    @BeforeEach
    public void initEach() {
        params = CreateOrderParameters
                .newBuilder(regionId, OfferItems.FF_172_UNFAIR_STOCK.getItem(), DeliveryType.DELIVERY)
                .address(Address.LAVKA_HOUR_SLOT_CAMEN)
                .paymentType(PaymentType.PREPAID)
                .paymentMethod(PaymentMethod.YANDEX)
                .experiment(EnumSet.of(RearrFactor.DEFFERED_COURIER))
                .forceDeliveryId(hourSlot)
                .build();
        List<? extends Delivery> deliveries = ORDER_STEPS.cart(params);

        Delivery deliveryOption = null;
        loops:
        for (Delivery delivery : deliveries) {
            for (RawDeliveryIntervalAndDate interval : delivery.getRawDeliveryIntervals().getForJson()) {
                for (RawDeliveryInterval rawDeliveryInterval : interval.getIntervals()) {
                    long count = interval.getIntervals().stream().filter(interv -> interv.equals(rawDeliveryInterval)).count();
                    Assertions.assertEquals(1, count, "Интервалы доставки повторяются " + rawDeliveryInterval);
                    if (rawDeliveryInterval.getDate().equals(Date.valueOf(LocalDate.now().plus(2, ChronoUnit.DAYS)))
                            &&
                            rawDeliveryInterval.getFromTime().plus(1, ChronoUnit.HOURS).equals(rawDeliveryInterval.getToTime())
                            &&
                            params.getDeliveryInterval() == null) {
                        deliveryOption = delivery;
                        params.setDeliveryInterval(Pair.of(rawDeliveryInterval.getFromTime(), rawDeliveryInterval.getToTime()));
                        break loops;
                    }
                }
            }
        }
        Assertions.assertNotNull(deliveryOption, "Отсутствует опция часовых слотов");
        order = ORDER_STEPS.checkout(params, deliveryOption).get(0);
        ORDER_STEPS.payOrder(order);
        cpaDeliveryDateBefore = order.getDelivery().getDeliveryDates().getToDate()
                .toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
        DeliveryDates deliveryDates = order.getDelivery().getDeliveryDates();
        LocalTime fromTime = deliveryDates.getFromTime();
        LocalTime toTime = deliveryDates.getToTime();
        Assertions.assertNotNull(fromTime, "Отсутствует начало часового слота");
        Assertions.assertNotNull(toTime, "Отсутствует конец часового слота");
        Assertions.assertEquals(fromTime.plus(1, ChronoUnit.HOURS), toTime, "Некорректный часовой слот");

        lomOrderId = LOM_ORDER_STEPS.getLomOrderId(order);
        LOM_ORDER_STEPS.verifyOrderStatus(lomOrderId, OrderStatus.PROCESSING);

        ORDER_STEPS.verifySDTracksCreated(order);
        ORDER_STEPS.verifyFFTrackCreated(order);
        LOM_ORDER_STEPS.verifyTrackerIds(lomOrderId);
        OrderDto lomOrder = LOM_ORDER_STEPS.getLomOrderData(order);

        lomDeliveryStartTimeBefore = lomOrder.getDeliveryInterval().getFromTime();
        lomDeliveryEndTimeBefore = lomOrder.getDeliveryInterval().getToTime();
        lomDeliveryDateMaxBefore = lomOrder.getDeliveryInterval().getDeliveryDateMax();
        lomDeliveryDateMinBefore = lomOrder.getDeliveryInterval().getDeliveryDateMin();
    }

    @Test
    @TmsLink("logistic-24")
    @DisplayName("ПДД юзером. Изменение в часовых слотах сразу после создания заказа")
    public void updateDeliveryDateByUSerHourSlotOrderInLavka() {
        LOM_ORDER_STEPS.verifyOrderStatus(lomOrderId, OrderStatus.PROCESSING);

        CHECKOUTER_STEPS.changeDeliveryDateAndTime(order, 1);

        LOM_ORDER_STEPS.verifyChangeLomDeliveryTime(order, lomDeliveryStartTimeBefore, lomDeliveryEndTimeBefore);
        LOM_ORDER_STEPS.verifyChangeLomDeliveryDate(order, lomDeliveryDateMinBefore, lomDeliveryDateMaxBefore);
        ORDER_STEPS.verifyChangeDeliveryDate(order, cpaDeliveryDateBefore);

        CAPACITY_STORAGE_STEPS.verifyOrderRouteWasChanged(order.getId());
    }
}
