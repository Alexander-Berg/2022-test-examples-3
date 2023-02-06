package ru.yandex.market.deliveryintegrationtests.delivery.tests.fulfillment;

import dto.requests.checkouter.CreateOrderParameters;
import dto.requests.report.OfferItem;
import factory.OfferItems;
import io.qameta.allure.Epic;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import ru.qatools.properties.Resource;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.logistics.lom.model.enums.OrderStatus;
import toolkit.Delayer;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Resource.Classpath({"delivery/checkouter.properties"})
@DisplayName("Blue PVZ in LMS Test")
@Epic("Blue FF")
@Slf4j
@Tag("SlowTest")
public class PvzVLmsTest extends AbstractFulfillmentTest {

    private static List<Long> logisticsPointId;


    @Test
    @Disabled("Время прохождения всего прогона с этим тестом увеличивается на 30-60 минут. Пока выключим этот тест.")
    @DisplayName("ПВЗ в LMS: Деактивация ПВЗ и проверка невозможности создать заказ на этот ПВЗ")
    public void deactivatePvzTest() {
        log.info("Trying to create order, receive outletId and cancel order");
        OfferItem ffItem = OfferItems.FF_172_UNFAIR_STOCK.getItem();
        params = CreateOrderParameters
                .newBuilder(regionId, ffItem, DeliveryType.PICKUP)
                .build();
        order = ORDER_STEPS.createOrder(params);

        lomOrderId = LOM_ORDER_STEPS.getLomOrderId(order);

        logisticsPointId = Collections.singletonList(Long.parseLong(ORDER_STEPS.getOutletId(order), 10));
        log.info("outletId is: {}", logisticsPointId);

        ORDER_STEPS.cancelOrder(order);
        ORDER_STEPS.verifyForOrderStatus(order, ru.yandex.market.checkout.checkouter.order.OrderStatus.CANCELLED);
        LOM_ORDER_STEPS.verifyOrderStatus(lomOrderId, OrderStatus.CANCELLED);

        log.info("Trying to deactivate and freeze logistics point");
        LMS_STEPS.deactivateLogisticsPoint(logisticsPointId);
        LMS_STEPS.freezeLogisticsPoint(logisticsPointId);

        Delayer.delay(30, TimeUnit.MINUTES);
        // ПВЗ должен исчезать с выдачи за 30 минут, разработчик советовал для автотеста ждать 40-45 минут.
        // С ожиданием в 15 минут тест иногда падает - деактивированный ПВЗ не успевает исчезнуть.
        // Выставлю 30 минут.
        log.info("Trying to create order for deactivated logistics point");
        params = CreateOrderParameters
                .newBuilder(regionId, ffItem, DeliveryType.PICKUP)
                .outletId(logisticsPointId)
                .build();

        ORDER_STEPS.createOrder(params);
    }


    @AfterEach
    @Step("Активация и анфриз ПВЗ")
    public void tearDown() {
        try {
            log.info("Trying to activate and unfreeze logistics point");
            LMS_STEPS.activateLogisticsPoint(logisticsPointId);
            LMS_STEPS.unfreezeLogisticsPoint(logisticsPointId);
        } catch (AssertionError ignored) {
        }
    }
}
