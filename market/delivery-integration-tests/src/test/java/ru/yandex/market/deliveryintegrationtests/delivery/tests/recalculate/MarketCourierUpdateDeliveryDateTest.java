package ru.yandex.market.deliveryintegrationtests.delivery.tests.recalculate;

import dto.requests.checkouter.CreateOrderParameters;
import dto.requests.checkouter.RearrFactor;
import factory.OfferItems;
import io.qameta.allure.Epic;
import io.qameta.allure.TmsLink;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import ru.qatools.properties.Property;
import ru.qatools.properties.Resource;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.enums.OrderStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.EnumSet;

@Slf4j
@DisplayName("Recalculate of delivery date")
@Epic("Recalculate of delivery date")
@Resource.Classpath({"delivery/checkouter.properties", "delivery/checkouter.properties"})
public class MarketCourierUpdateDeliveryDateTest extends AbstractRecalculateTest {

    @Property("delivery.marketCourier")
    protected static Long marketCourier;
    private LocalDate cpaDeliveryDateBefore;
    private LocalDate lomDeliveryDateMaxBefore;
    private LocalDate lomDeliveryDateMinBefore;
    private LocalTime lomDeliveryStartTimeBefore;
    private LocalTime lomDeliveryEndTimeBefore;

    @BeforeEach
    void setUp() {
        log.info("Trying to create checkouter order");

         params = CreateOrderParameters
                 .newBuilder(regionId, OfferItems.FF_172_UNFAIR_STOCK.getItem(), DeliveryType.DELIVERY)
                 .experiment(EnumSet.of(RearrFactor.RDD_BY_USER))
                 .forceDeliveryId(marketCourier)
                 .build();
        order = ORDER_STEPS.createOrder(params);
        //сохраняем дату доставки до в чекаутере
        cpaDeliveryDateBefore = order.getDelivery().getDeliveryDates().getToDate()
                .toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
        //проверяем статус заказа в ломе
        lomOrderId = LOM_ORDER_STEPS.getLomOrderId(order);
        LOM_ORDER_STEPS.verifyOrderStatus(lomOrderId, OrderStatus.PROCESSING);
        //проверяем треки в чекаутере
        ORDER_STEPS.verifySDTracksCreated(order);
        ORDER_STEPS.verifyFFTrackCreated(order);
        //проверяем треки в ломе
        LOM_ORDER_STEPS.verifyTrackerIds(lomOrderId);

        OrderDto lomOrder = LOM_ORDER_STEPS.getLomOrderData(order);
        lomDeliveryDateMaxBefore = lomOrder.getDeliveryInterval().getDeliveryDateMax();
        lomDeliveryDateMinBefore = lomOrder.getDeliveryInterval().getDeliveryDateMin();
        lomDeliveryStartTimeBefore = lomOrder.getDeliveryInterval().getFromTime();
        lomDeliveryEndTimeBefore = lomOrder.getDeliveryInterval().getToTime();
    }

    @Test
    @TmsLink("logistic-33")
    @Tag("SmokeTest")
    @DisplayName("ПДД юзером. Изменение даты в макрет курьерке сразу после создания заказа")
    public void updateDeliveryDateByUser() {
        LOM_ORDER_STEPS.verifyOrderStatus(lomOrderId, OrderStatus.PROCESSING);

        CHECKOUTER_STEPS.changeDeliveryDate(order);

        LOM_ORDER_STEPS.verifyDontChangeLomDeliveryTime(order, lomDeliveryStartTimeBefore, lomDeliveryEndTimeBefore);
        LOM_ORDER_STEPS.verifyChangeLomDeliveryDate(order, lomDeliveryDateMinBefore, lomDeliveryDateMaxBefore);
        ORDER_STEPS.verifyChangeDeliveryDate(order, cpaDeliveryDateBefore);
    }
}
