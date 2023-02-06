package ru.yandex.market.pipelinetests.tests.fulfillment;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import dto.requests.checkouter.CreateOrderParameters;
import factory.OfferItems;
import io.qameta.allure.Epic;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import ru.qatools.properties.Property;
import ru.qatools.properties.Resource;

import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.logistics.lom.model.dto.CombinatorRoute;
import ru.yandex.market.logistics.lom.model.enums.OrderStatus;
import ru.yandex.market.logistics.lom.model.enums.PointType;

@Resource.Classpath({"delivery/checkouter.properties", "delivery/delivery.properties"})
@DisplayName("Blue FF Create/Cancel order Test")
@Epic("Blue FF")
@Slf4j
public class CreateOrderTest extends AbstractFulfillmentTest {
    private final List<Long> outletId = Collections.singletonList(10000981168L);

    @Property("delivery.firstWaveDS")
    private long firstWaveDS;
    @Property("delivery.secondWaveDS")
    private long secondWaveDS;
    @Property("delivery.marketCourierMiddleDS")
    private long marketCourierId;
    @Property("delivery.sofino")
    private long sofino;

    @Test
    @Tag("SmokeTest")
    @Tag("FulfillmentOrderCreationTest")
    @DisplayName("Создание заказа в партнерский ПВЗ")
    public void createPickupOrderWithMiddleMile() {
        log.info("Trying to create checkouter order");

        params = CreateOrderParameters
            .newBuilder(regionId, OfferItems.FF_172_UNFAIR_STOCK.getItem(), DeliveryType.PICKUP)
            .outletId(outletId)
            .build();
        order = ORDER_STEPS.createOrder(params);

        lomOrderId = LOM_ORDER_STEPS.getLomOrderId(order);
        LOM_ORDER_STEPS.verifyOrderStatus(lomOrderId, OrderStatus.PROCESSING);

        ORDER_STEPS.verifySDTracksCreated(order);
        ORDER_STEPS.verifyFFTrackCreated(order);

        CombinatorRoute combinatorRoute = LOM_ORDER_STEPS.getOrderRoute(lomOrderId).getText();
        CombinatorRoute.Point linehaul = combinatorRoute.getRoute().getPoints().stream()
            .filter(point -> point.getSegmentType() == PointType.LINEHAUL)
            .findFirst()
            .orElseThrow(() -> new AssertionError("Не найден сегмент с типом LINEHAUL"));

        Assertions.assertTrue(Arrays.asList(firstWaveDS, secondWaveDS).contains(linehaul.getIds().getPartnerId()));

        CombinatorRoute.Point pickup = combinatorRoute.getRoute().getPoints().stream()
            .filter(point -> point.getSegmentType() == PointType.PICKUP)
            .findFirst()
            .orElseThrow(() -> new AssertionError("Не найден сегмент с типом PICKUP"));

        Assertions.assertEquals(order.getDelivery().getDeliveryServiceId(), pickup.getIds().getPartnerId(),
            "Сегмент принадлежит другому партнеру");
    }
}
