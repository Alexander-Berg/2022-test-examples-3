package ru.yandex.market.deliveryintegrationtests.delivery.tests.taxiintegration.pvzondemand;

import dto.responses.bluefapi.ResolveLink;
import dto.requests.checkouter.Address;
import dto.requests.checkouter.CreateOrderParameters;
import dto.requests.checkouter.OrderComment;
import dto.requests.checkouter.RearrFactor;
import factory.OfferItems;
import io.qameta.allure.Epic;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

import java.util.EnumSet;

@Resource.Classpath({"delivery/checkouter.properties", "delivery/delivery.properties"})
@DisplayName("Create ondemand pvz order")
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
                .comment(OrderComment.FIND_COURIER_FASTER)
                .build();
        order = ORDER_STEPS.createOrder(params);
        ORDER_STEPS.payOrder(order);

        Assertions.assertNotNull(order.getDelivery().getFeatures(), "Пустое поле features у заказа " + order.getId());
        Assertions.assertTrue(order.getDelivery().getFeatures().contains(DeliveryFeature.ON_DEMAND),
                "Заказ создался не в ON_DEMAND");

        lomOrderId = LOM_ORDER_STEPS.getLomOrderId(order);
        LOM_ORDER_STEPS.verifyOrderStatus(lomOrderId, OrderStatus.PROCESSING);

        ORDER_STEPS.verifySDTracksCreated(order);
    }


    @Test
    @DisplayName("Создание и успешная доставка заказа ондеманд ПВЗ")
    public void createAndSuccessDeliveryCourierPvzOrder() {
        log.debug("Trying to create ondemand pvz order");

        WaybillSegmentDto middleSDWaybillSegment = LOM_ORDER_STEPS.getWaybillSegmentForPartner(lomOrderId, pickupPartner);
        DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(middleSDWaybillSegment.getTrackerId(), OrderDeliveryCheckpointStatus.DELIVERY_ARRIVED_PICKUP_POINT);

        LOM_ORDER_STEPS.verifyOrderSegmentStatus(lomOrderId, order.getDelivery().getDeliveryServiceId(), SegmentStatus.TRANSIT_PICKUP);

        WaybillSegmentDto external = LOM_ORDER_STEPS.getWaybillSegmentForPartner(lomOrderId, order.getDelivery().getDeliveryServiceId());
        ResolveLink.Collection.OnDemandUrl onDemandUrl = BLUE_F_API_STEPS.resolveOnDemandLink(external.getExternalId());

        TAXI_STEPS.transferActivate(onDemandUrl.getTransferId(), external.getExternalId());

        LOM_ORDER_STEPS.verifyOrderSegmentStatus(lomOrderId, order.getDelivery().getDeliveryServiceId(), SegmentStatus.TRANSIT_TRANSMITTED_TO_RECIPIENT);
    }


    @Test
    @DisplayName("Создание и протухание заказа ондеманд ПВЗ и вызов курьера")
    public void createAndExpiredCourierPvzOrder() {
        log.debug("Trying to create ondemand pvz order");

        WaybillSegmentDto middleSDWaybillSegment = LOM_ORDER_STEPS.getWaybillSegmentForPartner(lomOrderId, pickupPartner);
        DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(middleSDWaybillSegment.getTrackerId(), OrderDeliveryCheckpointStatus.DELIVERY_ARRIVED_PICKUP_POINT);
        DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(middleSDWaybillSegment.getTrackerId(), OrderDeliveryCheckpointStatus.DELIVERY_STORAGE_PERIOD_EXPIRED);
        LOM_ORDER_STEPS.verifyOrderSegmentStatus(lomOrderId, order.getDelivery().getDeliveryServiceId(), SegmentStatus.RETURN_ARRIVED);


        WaybillSegmentDto external = LOM_ORDER_STEPS.getWaybillSegmentForPartner(lomOrderId, order.getDelivery().getDeliveryServiceId());
        Assertions.assertThrows(AssertionError.class, () -> BLUE_F_API_STEPS.resolveOnDemandLink(external.getExternalId()),
                "Возможно получить ссылку на протухший товар");
    }


    @Test
    @DisplayName("Отмена заказа ондеманд ПВЗ после приемки в ПВЗ")
    public void cancelPvzOrderAfterPvzArrived() {
        log.debug("Trying to create ondemand pvz order");

        WaybillSegmentDto middleSDWaybillSegment = LOM_ORDER_STEPS.getWaybillSegmentForPartner(lomOrderId, pickupPartner);
        DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(middleSDWaybillSegment.getTrackerId(), OrderDeliveryCheckpointStatus.DELIVERY_ARRIVED_PICKUP_POINT);

        LOM_ORDER_STEPS.verifyOrderSegmentStatus(lomOrderId, order.getDelivery().getDeliveryServiceId(), SegmentStatus.TRANSIT_PICKUP);

        ORDER_STEPS.cancelOrder(order);

        ORDER_STEPS.verifyForOrderStatus(order, ru.yandex.market.checkout.checkouter.order.OrderStatus.CANCELLED);
        LOM_ORDER_STEPS.verifyOrderStatus(lomOrderId, OrderStatus.RETURNING);

        LOM_ORDER_STEPS.verifyOrderSegmentStatus(lomOrderId, order.getDelivery().getDeliveryServiceId(), SegmentStatus.RETURN_ARRIVED);
    }


    @Test
    @DisplayName("Отмена заказа ондеманд ПВЗ до приемки в ПВЗ")
    public void cancelPvzOrderBeforePvzArrived() {
        log.debug("Trying to create ondemand pvz order");

        ORDER_STEPS.cancelOrder(order);

        ORDER_STEPS.verifyForOrderStatus(order, ru.yandex.market.checkout.checkouter.order.OrderStatus.CANCELLED);
        LOM_ORDER_STEPS.verifyOrderStatus(lomOrderId, OrderStatus.CANCELLED);
        LOM_ORDER_STEPS.verifyOrderSegmentStatus(lomOrderId, order.getDelivery().getDeliveryServiceId(), SegmentStatus.CANCELLED);
    }
}
