package ru.yandex.market.pipelinetests.tests.dbs;

import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import dto.requests.checkouter.CreateOrderParameters;
import dto.requests.checkouter.RearrFactor;
import dto.requests.report.OfferItem;
import dto.responses.lom.admin.order.Route;
import dto.responses.tpl.pvz.PvzOrderDto;
import factory.OfferItems;
import io.qameta.allure.Allure;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import toolkit.Delayer;

import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.delivery.tracker.domain.enums.OrderDeliveryCheckpointStatus;
import ru.yandex.market.logistic.api.model.common.OrderStatusType;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentDto;
import ru.yandex.market.logistics.lom.model.enums.PartnerType;
import ru.yandex.market.logistics.lom.model.enums.PointType;
import ru.yandex.market.logistics.lom.model.enums.SegmentStatus;
import ru.yandex.market.logistics.lom.model.enums.SegmentType;

@DisplayName("Заказы от DBS мерча в ПВЗ Маркета")
public class DbsToMarketPickupOrderTest extends AbstractDbsTest {

    private static final Long DELIVERY_ID = 48629L;
    //Если меняем ПВЗ id, то надо менять сразу оба параметра. TPL id можно узнать в БД ТПЛ или у ребят из ТПЛ.
    private static final Long LMS_PVZ_ID = 10001016752L;
    private static final Long TPL_PVZ_ID = 1001543020L;
    private Long pickupTrackerId;

    @BeforeEach
    void setUp() {
        // Создаем заказ с несколькими товарами от одного мерча в чекаутере
        List<OfferItem> items = OfferItems.DBS_TO_MARKET_PICKUP_ITEM.getItems(2, true);
        params = CreateOrderParameters.newBuilder(213L, items, DeliveryType.PICKUP)
            .paymentType(PaymentType.PREPAID)
            .paymentMethod(PaymentMethod.YANDEX)
            .rgb(Color.WHITE)
            .outletId(List.of(LMS_PVZ_ID))
            .experiment(EnumSet.of(RearrFactor.DBS_TO_MARKET_PICKUP))
            .build();
        order = ORDER_STEPS.createOrder(params);
        Allure.step("Создан заказ checkouterOrderId = " + order.getId());

        // Оплачиваем заказ
        ORDER_STEPS.payOrder(order, OrderStatus.PROCESSING);

        // Передаем в доставку в ПИ DBS-мерча
        ORDER_STEPS.changeOrderStatusAndSubStatusByShopUser(
            order,
            OrderStatus.DELIVERY,
            OrderSubstatus.DELIVERY_SERVICE_RECEIVED
        );

        // Достаем заказ в LOM, дожидаемся статуса PROCESSING
        lomOrderId = LOM_ORDER_STEPS.getLomOrderId(order);
        lomOrder = LOM_ORDER_STEPS.verifyOrderStatus(
            lomOrderId,
            ru.yandex.market.logistics.lom.model.enums.OrderStatus.PROCESSING
        );

        // Валидируем сегменты вейбилла
        List<WaybillSegmentDto> waybillSegments = lomOrder.getWaybill();
        Assertions.assertEquals(2, waybillSegments.size());
        WaybillSegmentDto dbsSegment = waybillSegments.get(0);
        Assertions.assertEquals(PartnerType.DROPSHIP_BY_SELLER, dbsSegment.getPartnerType());
        Assertions.assertEquals(SegmentType.NO_OPERATION, dbsSegment.getSegmentType());
        WaybillSegmentDto pickupSegment = waybillSegments.get(1);
        Assertions.assertEquals(PartnerType.DELIVERY, pickupSegment.getPartnerType());
        Assertions.assertEquals(SegmentType.PICKUP, pickupSegment.getSegmentType());

        // Валидируем маршрут
        validateRoute();

        // Ожидаем, пока заказ создается в ПВЗ, трек регистрируется в трекере и прорастает в чекаутер
        lomOrder = LOM_ORDER_STEPS.verifyOrderCreatedAtPartnerOnWaybillSegment(order, pickupSegment.getId());
        DELIVERY_TRACKER_STEPS.instantRequest(order.getId());
        ORDER_STEPS.verifySDTracksCreated(order);

        // Ожидаем чекпоинт от ПВЗ
        pickupTrackerId = lomOrder.getWaybill().get(1).getTrackerId();
        DELIVERY_TRACKER_STEPS.verifyCheckpoint(
            pickupTrackerId,
            OrderStatusType.ORDER_CREATED_DS.getCode()
        );
    }

    private void validateRoute() {
        Route route = LOM_ORDER_STEPS.getOrderRoute(lomOrderId);
        Assertions.assertIterableEquals(
            route.getText().getRoute().getPoints().stream()
                .map(point -> Pair.of(point.getSegmentType(), point.getPartnerType()))
                .collect(Collectors.toList()),
            List.of(
                Pair.of(PointType.WAREHOUSE, PartnerType.DROPSHIP_BY_SELLER),
                Pair.of(PointType.MOVEMENT, PartnerType.DROPSHIP_BY_SELLER),
                Pair.of(PointType.LINEHAUL, PartnerType.DROPSHIP_BY_SELLER),
                Pair.of(PointType.PICKUP, PartnerType.DELIVERY)
            )
        );
    }

    private void checkVerificationCode() {
        ORDER_STEPS.verifyCode(order.getId(), lomOrder.getRecipientVerificationCode());
        PvzOrderDto tplOrder = TPL_PVZ_STEPS.getOrder(TPL_PVZ_ID, order.getId().toString());
        TPL_PVZ_STEPS.verifyCodeForPvzOrder(
            TPL_PVZ_ID.toString(),
            tplOrder.getId().toString(),
            lomOrder.getRecipientVerificationCode()
        );
    }

    @Test
    @Tag("DbsOrderCreationTest")
    @DisplayName("Заказ успешно доставлен получателю")
    void orderSuccessfullyDeliveredToRecipient() {
        TPL_PVZ_STEPS.receiveOrderInPvz(TPL_PVZ_ID, order.getId().toString());
        ORDER_STEPS.verifyOrderStatusAndSubstatus(order, OrderStatus.PICKUP, OrderSubstatus.PICKUP_SERVICE_RECEIVED);
        checkVerificationCode();
        Allure.step("Ждём, пока в чекаутер гарантированно попадёт PICKUP_USER_RECEIVED от MDB");
        LOM_ORDER_STEPS.verifyOrderSegmentStatus(lomOrderId, DELIVERY_ID, SegmentStatus.TRANSIT_PICKUP);
        Delayer.delay(2, TimeUnit.MINUTES);

        DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(
            pickupTrackerId,
            OrderDeliveryCheckpointStatus.DELIVERY_TRANSMITTED_TO_RECIPIENT
        );
        ORDER_STEPS.verifyOrderStatusAndSubstatus(order, OrderStatus.PICKUP, OrderSubstatus.PICKUP_USER_RECEIVED);

        DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(
            pickupTrackerId,
            OrderDeliveryCheckpointStatus.DELIVERY_DELIVERED
        );
        ORDER_STEPS.verifyOrderStatusAndSubstatus(
            order,
            OrderStatus.DELIVERED,
            OrderSubstatus.DELIVERY_SERVICE_DELIVERED
        );
    }
}
