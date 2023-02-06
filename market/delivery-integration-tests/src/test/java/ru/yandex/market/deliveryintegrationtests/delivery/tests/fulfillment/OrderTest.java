package ru.yandex.market.deliveryintegrationtests.delivery.tests.fulfillment;

import dto.requests.checkouter.CreateOrderParameters;
import dto.requests.checkouter.RearrFactor;
import factory.OfferItems;
import io.qameta.allure.Epic;
import io.qameta.allure.TmsLink;
import io.qameta.allure.TmsLinks;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import ru.qatools.properties.Property;
import ru.qatools.properties.Resource;

import ru.yandex.market.checkout.checkouter.delivery.DeliveryServiceType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.delivery.tracker.domain.enums.OrderDeliveryCheckpointStatus;
import ru.yandex.market.logistics.lom.model.dto.CombinatorRoute;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentDto;
import ru.yandex.market.logistics.lom.model.enums.OrderStatus;
import ru.yandex.market.logistics.lom.model.enums.PointType;
import ru.yandex.market.logistics.lom.model.enums.SegmentStatus;
import ru.yandex.market.logistics.lom.model.enums.SegmentType;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Stream;

@Resource.Classpath({"delivery/checkouter.properties", "delivery/delivery.properties"})
@DisplayName("Blue FF Create/Cancel order Test")
@Epic("Blue FF")
@Slf4j
public class OrderTest extends AbstractFulfillmentTest {
    private final List<Long> outletId = Collections.singletonList(10000977915L);

    @Property("delivery.firstWaveDS")
    private long firstWaveDS;
    @Property("delivery.secondWaveDS")
    private long secondWaveDS;
    @Property("delivery.marketCourierMiddleDS")
    private long marketCourierId;
    @Property("delivery.sofino")
    private long sofino;

    private void createOrder(DeliveryType deliveryType, Long regionId) {
        params = CreateOrderParameters
            .newBuilder(regionId, OfferItems.FF_172_UNFAIR_STOCK.getItem(), deliveryType)
            .build();
        order = ORDER_STEPS.createOrder(params);
    }

    private static Stream<Arguments> deliveryParameters() {
        return Stream.of(DeliveryType.DELIVERY, DeliveryType.PICKUP)
            .flatMap(deliveryType -> Stream.of(213, 2, 54).map(regionId -> Arguments.of(deliveryType, regionId))
            );
    }

    @ParameterizedTest(name = "Создание заказа типа {0} в регионе {1}")
    @TmsLinks({@TmsLink("logistic-52"), @TmsLink("logistic-53"), @TmsLink("logistic-54"), @TmsLink("logistic-55"),
        @TmsLink("logistic-56"), @TmsLink("logistic-57")})
    @MethodSource("deliveryParameters")
    public void createPickupOrderTest(DeliveryType deliveryType, long regionId) {
        log.info("Trying to create checkouter order");

        createOrder(deliveryType, regionId);

        lomOrderId = LOM_ORDER_STEPS.getLomOrderId(order);
        LOM_ORDER_STEPS.verifyOrderStatus(lomOrderId, OrderStatus.PROCESSING);

        ORDER_STEPS.verifySDTracksCreated(order);
        ORDER_STEPS.verifyFFTrackCreated(order);
    }

    @Test
    @DisplayName("FF: Создание заказа с использование идентификатора заказа из системы партнера")
    @TmsLink("logistic-58")
    public void createDropshipWithPartnerExternalOrderId() {
        createOrder(DeliveryType.DELIVERY, 54L);

        lomOrderId = LOM_ORDER_STEPS.getLomOrderId(order);

        WaybillSegmentDto ffSegment = LOM_ORDER_STEPS.getWaybillSegments(lomOrderId).stream()
            .filter(s -> s.getSegmentType() == SegmentType.FULFILLMENT)
            .findAny()
            .orElseThrow();

        Long lomFfTrackerId = ffSegment.getTrackerId();
        String trackCodeFromTracker = DELIVERY_TRACKER_STEPS.getTrackById(lomFfTrackerId)
            .getDeliveryTrackMeta()
            .getTrackCode();

        Assertions.assertEquals(
            ffSegment.getExternalId(),
            trackCodeFromTracker,
            String.format(
                "ExternalId на сегменте ff (%s) не эквивалентен orderId из трекера (%s)",
                ffSegment.getExternalId(),
                trackCodeFromTracker
            )
        );
    }

    @Test
    @Tag("SmokeTest")
    @Tag("FulfillmentOrderCreationTest")
    @TmsLink("logistic-59")
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

        Assertions.assertTrue(
            Arrays.asList(firstWaveDS, secondWaveDS).contains(linehaul.getIds().getPartnerId()),
            "В созданном заказе в качестве linehaul'а выбрался не Тарный 1/2"
        );

        CombinatorRoute.Point pickup = combinatorRoute.getRoute().getPoints().stream()
            .filter(point -> point.getSegmentType() == PointType.PICKUP)
            .findFirst()
            .orElseThrow(() -> new AssertionError("Не найден сегмент с типом PICKUP"));

        Assertions.assertEquals(order.getDelivery().getDeliveryServiceId(), pickup.getIds().getPartnerId(),
            "Сегмент принадлежит другому партнеру"
        );
    }

    @ParameterizedTest(name = "Создание и отмена заказа типа {0} ")
    @TmsLinks({@TmsLink("logistic-60"), @TmsLink("logistic-61")})
    @EnumSource(value = DeliveryType.class, names = {"DELIVERY", "PICKUP"})
    public void cancelOrderTest(DeliveryType deliveryType) {

        log.info("Trying to create order and then cancel it");

        createOrder(deliveryType, regionId);

        lomOrderId = LOM_ORDER_STEPS.getLomOrderId(order);

        ORDER_STEPS.verifySDTracksCreated(order);
        ORDER_STEPS.verifyFFTrackCreated(order);

        ORDER_STEPS.cancelOrder(order);

        ORDER_STEPS.verifyForOrderStatus(order, ru.yandex.market.checkout.checkouter.order.OrderStatus.CANCELLED);
        LOM_ORDER_STEPS.verifyOrderStatus(lomOrderId, OrderStatus.CANCELLED);
    }

    @ParameterizedTest(name = "Отмена заказа типа {0} до того, как он создастся в службе")
    @TmsLinks({@TmsLink("logistic-62"),@TmsLink("logistic-63")})
    @EnumSource(value = DeliveryType.class, names = {"DELIVERY", "PICKUP"})
    public void cancelOrderBeforeCreatedTest(DeliveryType deliveryType) {
        log.info("Trying to create order and cancel it before order created in delivery");

        createOrder(deliveryType, regionId);

        lomOrderId = LOM_ORDER_STEPS.getLomOrderId(order);

        ORDER_STEPS.cancelOrder(order);

        ORDER_STEPS.verifyForOrderStatus(order, ru.yandex.market.checkout.checkouter.order.OrderStatus.CANCELLED);
        LOM_ORDER_STEPS.verifyOrderStatus(lomOrderId, OrderStatus.CANCELLED);
    }

    @Test
    @TmsLink("logistic-65")
    @Tag("SmokeTest")
    @DisplayName("Проверяем эквивалентность trackerId в двух системах: LOM и CPA")
    public void checkEqualsTrackerIdLomAndCpaTest() {
        params = CreateOrderParameters
            .newBuilder(regionId, OfferItems.FF_172_UNFAIR_STOCK.getItem(), DeliveryType.DELIVERY)
            .build();
        order = ORDER_STEPS.createOrder(params);

        lomOrderId = LOM_ORDER_STEPS.getLomOrderId(order);

        ORDER_STEPS.verifySDTracksCreated(order);
        ORDER_STEPS.verifyFFTrackCreated(order);

        Long lomFfTrackerId = LOM_ORDER_STEPS.getWaybillSegments(lomOrderId).stream()
            .filter(s -> s.getSegmentType() == SegmentType.FULFILLMENT)
            .map(WaybillSegmentDto::getTrackerId)
            .findAny().orElseThrow();

        Long cpaFfTrackerId = ORDER_STEPS.getTrackNumbers(order, DeliveryServiceType.FULFILLMENT).getTrackerId();

        Long lomDsTrackerId = LOM_ORDER_STEPS.getWaybillSegments(lomOrderId).stream()
            .filter(s -> s.getSegmentType() == SegmentType.COURIER)
            .map(WaybillSegmentDto::getTrackerId)
            .findAny().orElseThrow();

        Long cpaDsTrackerId = ORDER_STEPS.getTrackNumbers(order, DeliveryServiceType.CARRIER).getTrackerId();

        Assertions.assertEquals(lomFfTrackerId, cpaFfTrackerId, "TrackerId для FF разные в CPA  и LOM");
        Assertions.assertEquals(lomDsTrackerId, cpaDsTrackerId, "TrackerId для DS разные в CPA  и LOM");

    }

    @Test
    @TmsLink("logistic-66")
    @DisplayName("Возвратный флоу: отмена заказ пользователем")
    public void returningFlowCancelBeforeUserReceivedTest() {

        log.info("Returning flow test");

        params = CreateOrderParameters
            .newBuilder(regionId, OfferItems.FF_172_UNFAIR_STOCK.getItem(), DeliveryType.DELIVERY)
            .experiment(EnumSet.of(RearrFactor.FORCE_DELIVERY_ID))
            .forceDeliveryId(marketCourierId)
            .build();
        order = ORDER_STEPS.createOrder(params);

        lomOrderId = LOM_ORDER_STEPS.getLomOrderId(order);

        ORDER_STEPS.verifySDTracksCreated(order);
        ORDER_STEPS.verifyFFTrackCreated(order);

        ORDER_STEPS.cancelOrder(order);

        WaybillSegmentDto lastMileWaybillSegment =
            LOM_ORDER_STEPS.getWaybillSegmentForPartner(lomOrderId, order.getDelivery().getDeliveryServiceId());
        WaybillSegmentDto ffMileWaybillSegment = LOM_ORDER_STEPS.getWaybillSegments(lomOrderId).stream()
            .filter(s -> s.getSegmentType() == SegmentType.FULFILLMENT)
            .findAny().orElseThrow();

        //проверяем, что при отмене автоматически пришел 60 чп
        DELIVERY_TRACKER_STEPS.instantRequest(order.getId());
        LOM_ORDER_STEPS.verifyOrderSegmentStatus(
            lomOrderId,
            order.getDelivery().getDeliveryServiceId(),
            SegmentStatus.RETURN_PREPARING
        );

        //шлем в трекер 80, 170, 180 чп, проверяем что статус в ЛОМ изменился
        DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(
            lastMileWaybillSegment.getTrackerId(),
            OrderDeliveryCheckpointStatus.RETURN_TRANSMITTED_FULFILMENT
        );
        LOM_ORDER_STEPS.verifyOrderSegmentStatus(
            lomOrderId,
            order.getDelivery().getDeliveryServiceId(),
            SegmentStatus.RETURNED
        ); //80

        // ждем 101 чп перед тем, как отправлять 170
        DELIVERY_TRACKER_STEPS.verifyCheckpoint(ffMileWaybillSegment.getTrackerId(), 101);

        DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(
            ffMileWaybillSegment.getTrackerId(),
            OrderDeliveryCheckpointStatus.SORTING_CENTER_RETURN_ARRIVED
        );
        LOM_ORDER_STEPS.verifyOrderAnyMileSegmentStatus(
            lomOrderId,
            ffMileWaybillSegment.getPartnerId(),
            SegmentStatus.RETURN_ARRIVED
        ); //170

        DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(
            ffMileWaybillSegment.getTrackerId(),
            OrderDeliveryCheckpointStatus.SORTING_CENTER_RETURN_RETURNED
        );
        LOM_ORDER_STEPS.verifyOrderAnyMileSegmentStatus(
            lomOrderId,
            ffMileWaybillSegment.getPartnerId(),
            SegmentStatus.RETURNED
        ); //180
    }
}
