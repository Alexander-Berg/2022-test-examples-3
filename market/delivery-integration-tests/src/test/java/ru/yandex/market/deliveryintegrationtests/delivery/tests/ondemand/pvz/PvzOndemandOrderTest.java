package ru.yandex.market.deliveryintegrationtests.delivery.tests.ondemand.pvz;

import java.util.EnumSet;

import dto.requests.checkouter.Address;
import dto.requests.checkouter.CreateOrderParameters;
import dto.requests.checkouter.RearrFactor;
import factory.OfferItems;
import io.qameta.allure.Epic;
import io.qameta.allure.Step;
import io.qameta.allure.TmsLink;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import ru.qatools.properties.Resource;

import ru.yandex.market.checkout.checkouter.delivery.DeliveryFeature;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.delivery.tracker.domain.enums.OrderDeliveryCheckpointStatus;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentDto;
import ru.yandex.market.logistics.lom.model.enums.OrderStatus;
import ru.yandex.market.logistics.lom.model.enums.SegmentStatus;

import static ru.yandex.market.logistic.api.model.common.OrderStatusType.ORDER_CREATED_DS;
import static ru.yandex.misc.thread.ThreadUtils.sleep;

@Resource.Classpath({"delivery/checkouter.properties", "delivery/delivery.properties"})
@DisplayName("Ondemand pvz order test")
@Epic("Ondemand pvz order")
@Slf4j
public class PvzOndemandOrderTest extends AbstractPvzOnDemandTest {

    @BeforeEach
    @Step("Подготовка данных")
    public void setUp() {
        params = CreateOrderParameters
            .newBuilder(regionId, OfferItems.FF_172_UNFAIR_STOCK.getItem(), DeliveryType.DELIVERY)
            .paymentType(PaymentType.PREPAID)
            .paymentMethod(PaymentMethod.YANDEX)
            .address(Address.PVZ_ON_DEMAND)
            .experiment(EnumSet.of(RearrFactor.LAVKA, RearrFactor.COMBINATORONDEMAND))
            .forceDeliveryId(ondemandDS)
            .build();
        order = ORDER_STEPS.createOrder(params);
        ORDER_STEPS.payOrder(order);

        Assertions.assertNotNull(order.getDelivery().getFeatures(), "Пустое поле features у заказа " + order.getId());
        Assertions.assertTrue(
            order.getDelivery().getFeatures().contains(DeliveryFeature.ON_DEMAND),
            "Заказ создался не в ON_DEMAND"
        );

        lomOrderId = LOM_ORDER_STEPS.getLomOrderId(order);
        LOM_ORDER_STEPS.verifyOrderStatus(lomOrderId, OrderStatus.PROCESSING);

        ORDER_STEPS.verifySDTracksCreated(order);

        middleSDWaybillSegment = LOM_ORDER_STEPS.getOnDemandPickupWaybillSegment(lomOrderId);
        lastSDWaybillSegment =
            LOM_ORDER_STEPS.getWaybillSegmentForPartner(lomOrderId, order.getDelivery().getDeliveryServiceId());
    }

    @Test
    @TmsLink("logistic-37")
    @DisplayName("Создание и успешная доставка заказа ондеманд ПВЗ")
    public void createAndSuccessDeliveryCourierPvzOrder() {
        log.debug("Trying to create ondemand pvz order");
        DELIVERY_TRACKER_STEPS.verifyCheckpoint(middleSDWaybillSegment.getTrackerId(), ORDER_CREATED_DS.getCode());
        DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(
            middleSDWaybillSegment.getTrackerId(),
            OrderDeliveryCheckpointStatus.DELIVERY_ARRIVED_PICKUP_POINT
        );

        LOM_ORDER_STEPS.verifyOrderSegmentStatus(
            lomOrderId,
            order.getDelivery().getDeliveryServiceId(),
            SegmentStatus.TRANSIT_PICKUP
        );

        lastSDWaybillSegment =
            LOM_ORDER_STEPS.getWaybillSegmentForPartner(lomOrderId, order.getDelivery().getDeliveryServiceId());

        //31
        DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(
            lastSDWaybillSegment.getTrackerId(),
            OrderDeliveryCheckpointStatus.DELIVERY_COURIER_SEARCH
        );
        LOM_ORDER_STEPS.verifyOrderSegmentStatus(
            lomOrderId,
            order.getDelivery().getDeliveryServiceId(),
            SegmentStatus.TRANSIT_COURIER_SEARCH
        );

        //32
        DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(
            lastSDWaybillSegment.getTrackerId(),
            OrderDeliveryCheckpointStatus.DELIVERY_COURIER_FOUND
        );
        LOM_ORDER_STEPS.verifyOrderSegmentStatus(
            lomOrderId,
            order.getDelivery().getDeliveryServiceId(),
            SegmentStatus.TRANSIT_COURIER_FOUND
        );

        //34
        DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(
            lastSDWaybillSegment.getTrackerId(),
            OrderDeliveryCheckpointStatus.DELIVERY_COURIER_ARRIVED_TO_SENDER
        );
        LOM_ORDER_STEPS.verifyOrderSegmentStatus(
            lomOrderId,
            order.getDelivery().getDeliveryServiceId(),
            SegmentStatus.TRANSIT_COURIER_ARRIVED_TO_SENDER
        );

        //35
        DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(
            lastSDWaybillSegment.getTrackerId(),
            OrderDeliveryCheckpointStatus.DELIVERY_COURIER_RECEIVED
        );
        LOM_ORDER_STEPS.verifyOrderSegmentStatus(
            lomOrderId,
            order.getDelivery().getDeliveryServiceId(),
            SegmentStatus.TRANSIT_COURIER_RECEIVED
        );

        //48
        DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(
            lastSDWaybillSegment.getTrackerId(),
            OrderDeliveryCheckpointStatus.DELIVERY_TRANSPORTATION_RECIPIENT
        );
        LOM_ORDER_STEPS.verifyOrderSegmentStatus(
            lomOrderId,
            order.getDelivery().getDeliveryServiceId(),
            SegmentStatus.TRANSIT_TRANSPORTATION_RECIPIENT
        );

        //49
        DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(
            lastSDWaybillSegment.getTrackerId(),
            OrderDeliveryCheckpointStatus.DELIVERY_TRANSMITTED_TO_RECIPIENT
        );
        LOM_ORDER_STEPS.verifyOrderSegmentStatus(
            lomOrderId,
            order.getDelivery().getDeliveryServiceId(),
            SegmentStatus.TRANSIT_TRANSMITTED_TO_RECIPIENT
        );

        ORDER_STEPS.verifyForOrderStatus(order, ru.yandex.market.checkout.checkouter.order.OrderStatus.DELIVERY);
    }

    @Test
    @TmsLink("logistic-36")
    @DisplayName("Создание и протухание заказа ондеманд ПВЗ и вызов курьера")
    public void createAndExpiredCourierPvzOrder() {
        log.debug("Trying to create ondemand pvz order");
        DELIVERY_TRACKER_STEPS.verifyCheckpoint(middleSDWaybillSegment.getTrackerId(), ORDER_CREATED_DS.getCode());
        DELIVERY_TRACKER_STEPS.verifyCheckpoint(lastSDWaybillSegment.getTrackerId(), ORDER_CREATED_DS.getCode());

        DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(
            middleSDWaybillSegment.getTrackerId(),
            OrderDeliveryCheckpointStatus.DELIVERY_ARRIVED_PICKUP_POINT
        );

        LOM_ORDER_STEPS.verifyOrderSegmentStatus(
            lomOrderId,
            order.getDelivery().getDeliveryServiceId(),
            SegmentStatus.TRANSIT_PICKUP
        );

        DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(
            middleSDWaybillSegment.getTrackerId(),
            OrderDeliveryCheckpointStatus.DELIVERY_STORAGE_PERIOD_EXPIRED
        );
        LOM_ORDER_STEPS.verifyOrderSegmentStatus(
            lomOrderId,
            order.getDelivery().getDeliveryServiceId(),
            SegmentStatus.RETURN_ARRIVED
        );

        WaybillSegmentDto external =
            LOM_ORDER_STEPS.getWaybillSegmentForPartner(lomOrderId, order.getDelivery().getDeliveryServiceId());
        Assertions.assertThrows(AssertionError.class,
            () -> BLUE_F_API_STEPS.resolveOnDemandLink(external.getExternalId()),
            "Возможно получить ссылку на протухший товар"
        );
    }

    @Test
    @TmsLink("logistic-38")
    @DisplayName("Отмена заказа ондеманд ПВЗ после приемки в ПВЗ")
    public void cancelPvzOrderAfterPvzArrived() {
        log.debug("Trying to create ondemand pvz order");

        DELIVERY_TRACKER_STEPS.verifyCheckpoint(middleSDWaybillSegment.getTrackerId(), ORDER_CREATED_DS.getCode());
        DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(
            middleSDWaybillSegment.getTrackerId(),
            OrderDeliveryCheckpointStatus.DELIVERY_ARRIVED_PICKUP_POINT
        );

        LOM_ORDER_STEPS.verifyOrderSegmentStatus(
            lomOrderId,
            order.getDelivery().getDeliveryServiceId(),
            SegmentStatus.TRANSIT_PICKUP
        );

        ORDER_STEPS.cancelOrder(order);

        ORDER_STEPS.verifyForOrderStatus(order, ru.yandex.market.checkout.checkouter.order.OrderStatus.CANCELLED);
        LOM_ORDER_STEPS.verifyOrderStatus(lomOrderId, OrderStatus.RETURNING);
    }

    @Test
    @TmsLink("logistic-39")
    @DisplayName("Отмена заказа ондеманд ПВЗ до приемки в ПВЗ")
    public void cancelPvzOrderBeforePvzArrived() {
        log.debug("Trying to create ondemand pvz order");

        ORDER_STEPS.cancelOrder(order);

        ORDER_STEPS.verifyForOrderStatus(order, ru.yandex.market.checkout.checkouter.order.OrderStatus.CANCELLED);
        LOM_ORDER_STEPS.verifyOrderStatus(lomOrderId, OrderStatus.CANCELLED);
        LOM_ORDER_STEPS.verifyOrderSegmentStatus(
            lomOrderId,
            order.getDelivery().getDeliveryServiceId(),
            SegmentStatus.CANCELLED
        );
    }

    @Test
    @Tag("ExcludeRegress")
    @TmsLink("logistic-40")
    @DisplayName("Создание и успешная доставка со 2й попытки заказа ондеманд ПВЗ")
    public void createAndSuccessDelivery2ndTryCourierPvzOrder() {
        log.debug("Trying to create ondemand pvz order");
        DELIVERY_TRACKER_STEPS.verifyCheckpoint(middleSDWaybillSegment.getTrackerId(), ORDER_CREATED_DS.getCode());
        DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(
            middleSDWaybillSegment.getTrackerId(),
            OrderDeliveryCheckpointStatus.DELIVERY_ARRIVED_PICKUP_POINT
        );
        LOM_ORDER_STEPS.verifyOrderSegmentStatus(
            lomOrderId,
            order.getDelivery().getDeliveryServiceId(),
            SegmentStatus.TRANSIT_PICKUP
        );

        DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(
            lastSDWaybillSegment.getTrackerId(),
            OrderDeliveryCheckpointStatus.DELIVERY_COURIER_SEARCH
        );
        LOM_ORDER_STEPS.verifyOrderSegmentStatus(
            lomOrderId,
            order.getDelivery().getDeliveryServiceId(),
            SegmentStatus.TRANSIT_COURIER_SEARCH
        );

        DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(
            lastSDWaybillSegment.getTrackerId(),
            OrderDeliveryCheckpointStatus.DELIVERY_ARRIVED_PICKUP_POINT
        );
        LOM_ORDER_STEPS.verifyOrderSegmentStatus(
            lomOrderId,
            order.getDelivery().getDeliveryServiceId(),
            SegmentStatus.TRANSIT_PICKUP
        );

        DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(
            lastSDWaybillSegment.getTrackerId(),
            OrderDeliveryCheckpointStatus.DELIVERY_COURIER_SEARCH
        );
        LOM_ORDER_STEPS.verifyOrderSegmentStatus(
            lomOrderId,
            order.getDelivery().getDeliveryServiceId(),
            SegmentStatus.TRANSIT_COURIER_SEARCH
        );

        DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(
            lastSDWaybillSegment.getTrackerId(),
            OrderDeliveryCheckpointStatus.DELIVERY_TRANSPORTATION_RECIPIENT
        );
        LOM_ORDER_STEPS.verifyOrderSegmentStatus(
            lomOrderId,
            order.getDelivery().getDeliveryServiceId(),
            SegmentStatus.TRANSIT_TRANSPORTATION_RECIPIENT
        );

        DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(
            lastSDWaybillSegment.getTrackerId(),
            OrderDeliveryCheckpointStatus.DELIVERY_TRANSMITTED_TO_RECIPIENT
        );
        LOM_ORDER_STEPS.verifyOrderSegmentStatus(
            lomOrderId,
            order.getDelivery().getDeliveryServiceId(),
            SegmentStatus.TRANSIT_TRANSMITTED_TO_RECIPIENT
        );

    }

    @Test
    @TmsLink("logistic-46")
    @DisplayName("Создание и протухание после неудачной доставки заказа ондеманд ПВЗ")
    public void createAndCourierReturnExpiredPvzOrder() {
        log.debug("Trying to create ondemand pvz order");

        WaybillSegmentDto middleSDWaybillSegment = LOM_ORDER_STEPS.getOnDemandPickupWaybillSegment(lomOrderId);
        WaybillSegmentDto lastSDWaybillSegment =
            LOM_ORDER_STEPS.getWaybillSegmentForPartner(lomOrderId, order.getDelivery().getDeliveryServiceId());

        DELIVERY_TRACKER_STEPS.verifyCheckpoint(middleSDWaybillSegment.getTrackerId(), ORDER_CREATED_DS.getCode());
        DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(
            middleSDWaybillSegment.getTrackerId(),
            OrderDeliveryCheckpointStatus.DELIVERY_ARRIVED_PICKUP_POINT
        );

        LOM_ORDER_STEPS.verifyOrderSegmentStatus(
            lomOrderId,
            order.getDelivery().getDeliveryServiceId(),
            SegmentStatus.TRANSIT_PICKUP
        );

        DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(
            lastSDWaybillSegment.getTrackerId(),
            OrderDeliveryCheckpointStatus.DELIVERY_COURIER_SEARCH
        );
        LOM_ORDER_STEPS.verifyOrderSegmentStatus(
            lomOrderId,
            order.getDelivery().getDeliveryServiceId(),
            SegmentStatus.TRANSIT_COURIER_SEARCH
        );

        DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(
            lastSDWaybillSegment.getTrackerId(),
            OrderDeliveryCheckpointStatus.DELIVERY_ATTEMPT_FAILED
        );
        LOM_ORDER_STEPS.verifyOrderSegmentStatus(
            lomOrderId,
            order.getDelivery().getDeliveryServiceId(),
            SegmentStatus.TRANSIT_DELIVERY_ATTEMPT_FAILED
        );

        DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(
            middleSDWaybillSegment.getTrackerId(),
            OrderDeliveryCheckpointStatus.DELIVERY_STORAGE_PERIOD_EXPIRED
        );

        LOM_ORDER_STEPS.verifyOrderSegmentStatus(
            lomOrderId,
            order.getDelivery().getDeliveryServiceId(),
            SegmentStatus.TRANSIT_DELIVERY_ATTEMPT_FAILED
        );
    }
}
