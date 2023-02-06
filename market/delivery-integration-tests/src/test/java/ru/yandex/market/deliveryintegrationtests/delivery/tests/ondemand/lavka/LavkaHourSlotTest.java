package ru.yandex.market.deliveryintegrationtests.delivery.tests.ondemand.lavka;

import dto.responses.lavka.LavkaParcelState;
import dto.responses.lavka.TimeSlot;
import dto.responses.lavka.TristeroOrderResponse;
import dto.requests.checkouter.Address;
import dto.requests.checkouter.CreateOrderParameters;
import dto.requests.checkouter.RearrFactor;
import factory.OfferItems;
import io.qameta.allure.Epic;
import io.qameta.allure.TmsLink;
import io.qameta.allure.TmsLinks;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import ru.qatools.properties.Property;
import ru.qatools.properties.Resource;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryFeature;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.RawDeliveryInterval;
import ru.yandex.market.checkout.checkouter.delivery.RawDeliveryIntervalAndDate;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.delivery.tracker.domain.enums.OrderDeliveryCheckpointStatus;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentDto;
import ru.yandex.market.logistics.lom.model.enums.OrderStatus;
import ru.yandex.market.logistics.lom.model.enums.SegmentStatus;
import toolkit.Pair;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;

@Resource.Classpath({"delivery/checkouter.properties", "delivery/delivery.properties"})
@DisplayName("Blue Lavka hour slot order Test")
@Epic("Blue Lavka")
@Slf4j
public class LavkaHourSlotTest extends AbstractLavkaTest {

    @Property("delivery.hourSlot")
    private long hourSlot;
    private static final String PERSONAL_PHONE_ID_LAVKA = "87745c883b8149138c50ef7e454b7857";

    @BeforeEach
    public void initEach() {
        params = CreateOrderParameters
                .newBuilder(regionId, OfferItems.FF_172_UNFAIR_STOCK.getItem(), DeliveryType.DELIVERY)
                .address(Address.LAVKA_HOUR_SLOT_SHARIK)
                .paymentType(PaymentType.PREPAID)
                .paymentMethod(PaymentMethod.YANDEX)
                .experiment(EnumSet.of(RearrFactor.DEFFERED_COURIER))
                .forceDeliveryId(hourSlot)
                .build();
        List<? extends Delivery> deliveries = ORDER_STEPS.cart(params);

        Delivery deliveryOption = null;
        for (Delivery delivery : deliveries) {
            for (RawDeliveryIntervalAndDate interval : delivery.getRawDeliveryIntervals().getForJson()) {
                for (RawDeliveryInterval rawDeliveryInterval : interval.getIntervals()) {
                    long count = interval.getIntervals().stream().filter(interv -> interv.equals(rawDeliveryInterval)).count();
                    Assertions.assertEquals(1, count, "Интервалы доставки повторяются " + rawDeliveryInterval);
                    String dateRawDeliveryInterval = new Date(rawDeliveryInterval.getDate().getTime()).toString();
                    if (dateRawDeliveryInterval.equals(Date.valueOf(LocalDate.now().plus(2,ChronoUnit.DAYS)).toString())
                        &&
                        rawDeliveryInterval.getFromTime().plus(1, ChronoUnit.HOURS).equals(rawDeliveryInterval.getToTime())
                                &&
                            params.getDeliveryInterval() == null) {
                        deliveryOption = delivery;
                        params.setDeliveryInterval(Pair.of(rawDeliveryInterval.getFromTime(), rawDeliveryInterval.getToTime()));
                    }
                }
            }
        }
        Assertions.assertNotNull(deliveryOption, "Отсутствует опция часовых слотов");
        order = ORDER_STEPS.checkout(params, deliveryOption).get(0);
        ORDER_STEPS.payOrder(order);
        DeliveryDates deliveryDates = order.getDelivery().getDeliveryDates();
        LocalTime fromTime = deliveryDates.getFromTime();
        LocalTime toTime = deliveryDates.getToTime();
        Assertions.assertNotNull(fromTime, "Отсутствует начало часового слота");
        Assertions.assertNotNull(toTime, "Отсутствует конец часового слота");
        Assertions.assertEquals(fromTime.plus(1, ChronoUnit.HOURS), toTime, "Некорректный часовой слот");
        lomOrderId = LOM_ORDER_STEPS.getLomOrderId(order);
    }

    @Test
    @TmsLink("logistic-34")
    @DisplayName("Создание заказа в лавку с часовыми слотами")
    public void createHourSlotOrderInLavka() {
        LOM_ORDER_STEPS.verifyOrderStatus(lomOrderId, OrderStatus.PROCESSING);
        TristeroOrderResponse orderInfo = TRISTERO_ORDER_STEPS.getOrderInfo(order.getId());
        TimeSlot timeslot = orderInfo.getTimeslot();

        Assertions.assertEquals(timeslot.getStart(), timeslot.getEnd().minus(1, ChronoUnit.HOURS),
                "Некорректный часовой слот в лавке");

        Assertions.assertEquals(
                order.getDelivery().getDeliveryDates().getFromDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                timeslot.getStart().toLocalDate(),
                "Часовой слот в заказе и в лавке не совпадает"
        );

        TRISTERO_ORDER_STEPS.setStatus(order.getId(), LavkaParcelState.IN_DEPOT);

        LOM_ORDER_STEPS.verifySegmentStatusHistory(lomOrderId, order.getDelivery().getDeliveryServiceId(), List.of(
                SegmentStatus.STARTED,
                SegmentStatus.TRACK_RECEIVED,
                SegmentStatus.INFO_RECEIVED,
                SegmentStatus.IN,
                SegmentStatus.TRANSIT_PICKUP
        ));

        LAVKA_STEPS.makeOrder(
                order.getBuyer().getUid(),
                PERSONAL_PHONE_ID_LAVKA,
                orderInfo,
                order.getDelivery().getBuyerAddress().getGps()
        );
    }

    @Test
    @TmsLink("logistic-47")
    @DisplayName("Создание заказа в лавку с часовыми слотами: отмена из статуса Delivery")
    public void cancelFromDeliveringHourSlotOrderTest() {
        LOM_ORDER_STEPS.verifyOrderStatus(lomOrderId, OrderStatus.PROCESSING);
        TRISTERO_ORDER_STEPS.setStatus(order.getId(), LavkaParcelState.IN_DEPOT);

        LOM_ORDER_STEPS.verifySegmentStatusHistory(lomOrderId, order.getDelivery().getDeliveryServiceId(), List.of(
                SegmentStatus.STARTED,
                SegmentStatus.TRACK_RECEIVED,
                SegmentStatus.INFO_RECEIVED,
                SegmentStatus.IN,
                SegmentStatus.TRANSIT_PICKUP
        ));

        ORDER_STEPS.cancelOrder(order);
        ORDER_STEPS.verifyForOrderStatus(order, ru.yandex.market.checkout.checkouter.order.OrderStatus.CANCELLED);

        LOM_ORDER_STEPS.verifyOrderSegmentStatus(lomOrderId, order.getDelivery().getDeliveryServiceId(), SegmentStatus.RETURN_ARRIVED);
    }


    @ParameterizedTest
    @EnumSource(value = OrderDeliveryCheckpointStatus.class, names = {"DELIVERY_ATTEMPT_FAILED", "DELIVERY_UPDATED_BY_RECIPIENT", "DELIVERY_UPDATED_BY_DELIVERY"})
    @TmsLinks({@TmsLink(value = "logistic-74"), @TmsLink(value = "logistic-75"), @TmsLink(value = "logistic-76")})
//    DELIVERY_ATTEMPT_FAILED – logistic-74
//    DELIVERY_UPDATED_BY_RECIPIENT – logistic-75
//    DELIVERY_UPDATED_BY_DELIVERY – logistic-76
    @DisplayName("Создание заказа в лавку с часовыми слотами: перевод заказа в ондеманд при опоздании")
    public void changeToOndemandHourSlotOrderTest(OrderDeliveryCheckpointStatus checkpointStatus) {
        LOM_ORDER_STEPS.verifyOrderStatus(lomOrderId, OrderStatus.PROCESSING);
        TRISTERO_ORDER_STEPS.setStatus(order.getId(), LavkaParcelState.IN_DEPOT);

        LOM_ORDER_STEPS.verifySegmentStatusHistory(lomOrderId, order.getDelivery().getDeliveryServiceId(), List.of(
                SegmentStatus.STARTED,
                SegmentStatus.TRACK_RECEIVED,
                SegmentStatus.INFO_RECEIVED,
                SegmentStatus.IN,
                SegmentStatus.TRANSIT_PICKUP
        ));
        WaybillSegmentDto waybillSegmentForDS = LOM_ORDER_STEPS.getWaybillSegmentForPartner(lomOrderId, order.getDelivery().getDeliveryServiceId());
        DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(waybillSegmentForDS.getTrackerId(), checkpointStatus);

        ORDER_STEPS.verifyOrderHasDeliveryFeature(order, DeliveryFeature.ON_DEMAND);
    }

    @TmsLink(value = "logistic-77")
    @DisplayName("Создание заказа в лавку с часовыми слотами: перевод заказа в ондеманд при 45")
    @Test
    public void changeToOndemandHourSlotOrderTest() {
        LOM_ORDER_STEPS.verifyOrderStatus(lomOrderId, OrderStatus.PROCESSING);

        WaybillSegmentDto waybillSegmentForDS = LOM_ORDER_STEPS.getWaybillSegmentForPartner(lomOrderId, order.getDelivery().getDeliveryServiceId());
        DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(
                waybillSegmentForDS.getTrackerId(),
                LocalDate.now().plus(2,ChronoUnit.DAYS),
                OrderDeliveryCheckpointStatus.DELIVERY_ARRIVED_PICKUP_POINT);

        ORDER_STEPS.verifyOrderHasDeliveryFeature(order, DeliveryFeature.ON_DEMAND);
    }
}
