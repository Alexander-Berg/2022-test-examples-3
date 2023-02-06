package ru.yandex.market.deliveryintegrationtests.delivery.tests.taxiintegration.pvzondemand;

import dto.responses.bluefapi.ResolveLink;
import dto.requests.checkouter.Address;
import dto.requests.checkouter.CreateOrderParameters;
import dto.requests.checkouter.OrderComment;
import dto.requests.checkouter.RearrFactor;
import factory.OfferItems;
import io.qameta.allure.Epic;
import io.qameta.allure.Step;
import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import ru.qatools.properties.Resource;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryServiceType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.delivery.tracker.domain.enums.OrderDeliveryCheckpointStatus;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentStatusHistoryDto;
import ru.yandex.market.logistics.lom.model.enums.OrderStatus;
import ru.yandex.market.logistics.lom.model.enums.SegmentStatus;
import toolkit.Delayer;

import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Resource.Classpath({"delivery/checkouter.properties", "delivery/delivery.properties"})
@DisplayName("Create ondemand pvz order with comment")
@Epic("Ondemand pvz order")
@Execution(ExecutionMode.SAME_THREAD)
@Slf4j
public class PvzOnDemandOrderWithCommentTest extends AbstractPvzOnDemandTest {

    private static final int FIRST_KOROBYTE = 2;
    private static final int SECOND_KOROBYTE = 3;
    private Integer mockId;

    public void createOrderWithComment(OrderComment comment) {
        params = CreateOrderParameters
                .newBuilder(regionId, OfferItems.FF_172_UNFAIR_STOCK.getItem(), DeliveryType.DELIVERY)
                .paymentType(PaymentType.PREPAID)
                .paymentMethod(PaymentMethod.YANDEX)
                .address(Address.PVZ_ON_DEMAND)
                .experiment(EnumSet.of(RearrFactor.LAVKA, RearrFactor.COMBINATORONDEMAND))
                .forceDeliveryId(ondemandDS)
                .comment(comment)
                .build();
        order = ORDER_STEPS.createOrder(params);
        ORDER_STEPS.payOrder(order);
        lomOrderId = LOM_ORDER_STEPS.getLomOrderId(order);
        LOM_ORDER_STEPS.verifyOrderStatus(lomOrderId, OrderStatus.PROCESSING);

        ORDER_STEPS.verifySDTracksCreated(order);
    }

    public void createOrderTwoBoxesWithComment(OrderComment comment) {
        params = CreateOrderParameters
                .newBuilder(regionId, OfferItems.FF_171_UNFAIR_STOCK.getItem(), DeliveryType.DELIVERY)
                .paymentType(PaymentType.PREPAID)
                .paymentMethod(PaymentMethod.YANDEX)
                .address(Address.PVZ_ON_DEMAND)
                .experiment(EnumSet.of(RearrFactor.LAVKA, RearrFactor.COMBINATORONDEMAND))
                .forceDeliveryId(ondemandDS)
                .comment(comment)
                .build();
        order = ORDER_STEPS.createOrder(params);

        ORDER_STEPS.payOrder(order);

        ORDER_STEPS.verifySDTracksCreated(order);
        ORDER_STEPS.verifyFFTrackCreated(order);

        Track trackNumber = ORDER_STEPS.getTrackNumbers(order, DeliveryServiceType.FULFILLMENT);

        mockId = MOCK_CLIENT.mockGetOrder(
                order.getId(),
                trackNumber.getTrackCode(),
                FIRST_KOROBYTE,
                SECOND_KOROBYTE
        );
        lomOrderId = LOM_ORDER_STEPS.getLomOrderId(order);
        List<WaybillSegmentStatusHistoryDto> historyStatuses = LOM_ORDER_STEPS.getSegmentStatusHistoryByPartner(
                lomOrderId, ondemandDS);

        DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(trackNumber.getTrackerId(), OrderDeliveryCheckpointStatus.SORTING_CENTER_PREPARED);
        ORDER_STEPS.verifyForCheckpointReceived(order, OrderDeliveryCheckpointStatus.SORTING_CENTER_PREPARED, DeliveryServiceType.FULFILLMENT);

        Delayer.delay(10, TimeUnit.SECONDS);
        List<WaybillSegmentStatusHistoryDto> historyStatusesAfterPrepared = LOM_ORDER_STEPS.getSegmentStatusHistoryByPartner(
                lomOrderId, ondemandDS);
        Assertions.assertEquals(historyStatuses, historyStatusesAfterPrepared, "Статусы изменились после 120 " + order.getId());

        LOM_ORDER_STEPS.verifyOrderHasTwoBoxes(lomOrderId);
    }

    @AfterEach
    @Step("Удаление мока mockGetOrder")
    public void tearDown() {
        if (mockId != null) {
            MOCK_CLIENT.deleteMockById(mockId);
            mockId = null;
        }
    }

    @Test
    @Tag("ExcludeRegress")
    @DisplayName("Создание и успешная доставка со 2й попытки заказа ондеманд ПВЗ")
    public void createAndSuccessDelivery2ndTryCourierPvzOrder() {
        log.debug("Trying to create ondemand pvz order");

        createOrderWithComment(OrderComment.FIND_COURIER_2_ATTEMPT);

        WaybillSegmentDto middleSDWaybillSegment = LOM_ORDER_STEPS.getWaybillSegmentForPartner(lomOrderId, pickupPartner);
        DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(middleSDWaybillSegment.getTrackerId(),
                OrderDeliveryCheckpointStatus.DELIVERY_ARRIVED_PICKUP_POINT);

        LOM_ORDER_STEPS.verifyOrderSegmentStatus(lomOrderId, order.getDelivery().getDeliveryServiceId(), SegmentStatus.TRANSIT_PICKUP);

        WaybillSegmentDto external = LOM_ORDER_STEPS.getWaybillSegmentForPartner(lomOrderId, order.getDelivery().getDeliveryServiceId());
        ResolveLink.Collection.OnDemandUrl onDemandUrl = BLUE_F_API_STEPS.resolveOnDemandLink(external.getExternalId());

        TAXI_STEPS.transferActivate(onDemandUrl.getTransferId(), external.getExternalId());
        LOM_ORDER_STEPS.verifyOrderSegmentStatus(lomOrderId, order.getDelivery().getDeliveryServiceId(), SegmentStatus.TRANSIT_COURIER_SEARCH);
        LOM_ORDER_STEPS.verifyOrderSegmentStatus(lomOrderId, order.getDelivery().getDeliveryServiceId(), SegmentStatus.TRANSIT_PICKUP);

        onDemandUrl = BLUE_F_API_STEPS.resolveOnDemandLink(external.getExternalId());
        TAXI_STEPS.transferActivate(onDemandUrl.getTransferId(), external.getExternalId());

        LOM_ORDER_STEPS.verifySegmentStatusCount(
                lomOrderId,
                order.getDelivery().getDeliveryServiceId(),
                SegmentStatus.TRANSIT_COURIER_SEARCH,
                2
        );
        LOM_ORDER_STEPS.verifyOrderSegmentStatus(lomOrderId, order.getDelivery().getDeliveryServiceId(), SegmentStatus.TRANSIT_TRANSMITTED_TO_RECIPIENT);

    }

    @Test
    @Tag("ExcludeRegress")
    @DisplayName("Создание и успешная доставка со 2й попытки заказа многокоробок ондеманд ПВЗ")
    public void createAndSuccessDelivery2ndTryCourierMultiBoxPvzOrder() {
        log.debug("Trying to create ondemand pvz order");

        createOrderTwoBoxesWithComment(OrderComment.FIND_COURIER_2_ATTEMPT);

        WaybillSegmentDto middleSDWaybillSegment = LOM_ORDER_STEPS.getWaybillSegmentForPartner(lomOrderId, pickupPartner);
        DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(middleSDWaybillSegment.getTrackerId(), OrderDeliveryCheckpointStatus.DELIVERY_ARRIVED_PICKUP_POINT);

        LOM_ORDER_STEPS.verifyOrderSegmentStatus(lomOrderId, order.getDelivery().getDeliveryServiceId(), SegmentStatus.TRANSIT_PICKUP);

        WaybillSegmentDto external = LOM_ORDER_STEPS.getWaybillSegmentForPartner(lomOrderId, order.getDelivery().getDeliveryServiceId());
        ResolveLink.Collection.OnDemandUrl onDemandUrl = BLUE_F_API_STEPS.resolveOnDemandLink(external.getExternalId());

        TAXI_STEPS.transferActivate(onDemandUrl.getTransferId(), external.getExternalId());
        LOM_ORDER_STEPS.verifyOrderSegmentStatus(lomOrderId, order.getDelivery().getDeliveryServiceId(), SegmentStatus.TRANSIT_COURIER_SEARCH);
        LOM_ORDER_STEPS.verifyOrderSegmentStatus(lomOrderId, order.getDelivery().getDeliveryServiceId(), SegmentStatus.TRANSIT_PICKUP);

        onDemandUrl = BLUE_F_API_STEPS.resolveOnDemandLink(external.getExternalId());
        TAXI_STEPS.transferActivate(onDemandUrl.getTransferId(), external.getExternalId());
        LOM_ORDER_STEPS.verifySegmentStatusCount(
                lomOrderId,
                order.getDelivery().getDeliveryServiceId(),
                SegmentStatus.TRANSIT_COURIER_SEARCH,
                2
        );
        LOM_ORDER_STEPS.verifyOrderSegmentStatus(lomOrderId, order.getDelivery().getDeliveryServiceId(), SegmentStatus.TRANSIT_TRANSMITTED_TO_RECIPIENT);
    }


    @Test
    @Tag("ExcludeRegress")
    @DisplayName("Создание и успешная доставка с 4й попытки 2й статус водитель не мог доставить заказа ондеманд ПВЗ")
    public void createAndSuccessDelivery4thTry2ndCargoCourierPvzOrder() {
        log.debug("Trying to create ondemand pvz order");

        createOrderWithComment(OrderComment.FIND_COURIER_4_ATTEMPT_2_CARGO);

        WaybillSegmentDto middleSDWaybillSegment = LOM_ORDER_STEPS.getWaybillSegmentForPartner(lomOrderId, pickupPartner);
        DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(middleSDWaybillSegment.getTrackerId(), OrderDeliveryCheckpointStatus.DELIVERY_ARRIVED_PICKUP_POINT);

        LOM_ORDER_STEPS.verifyOrderSegmentStatus(lomOrderId, order.getDelivery().getDeliveryServiceId(), SegmentStatus.TRANSIT_PICKUP);

        WaybillSegmentDto external = LOM_ORDER_STEPS.getWaybillSegmentForPartner(lomOrderId, order.getDelivery().getDeliveryServiceId());
        ResolveLink.Collection.OnDemandUrl onDemandUrl = BLUE_F_API_STEPS.resolveOnDemandLink(external.getExternalId());

        TAXI_STEPS.transferActivate(onDemandUrl.getTransferId(), external.getExternalId());
        LOM_ORDER_STEPS.verifyOrderSegmentStatus(lomOrderId, order.getDelivery().getDeliveryServiceId(), SegmentStatus.TRANSIT_COURIER_SEARCH);
        LOM_ORDER_STEPS.verifyOrderSegmentStatus(lomOrderId, order.getDelivery().getDeliveryServiceId(), SegmentStatus.TRANSIT_PICKUP);

        onDemandUrl = BLUE_F_API_STEPS.resolveOnDemandLink(external.getExternalId());
        TAXI_STEPS.transferActivate(onDemandUrl.getTransferId(), external.getExternalId());
        LOM_ORDER_STEPS.verifyOrderSegmentStatus(lomOrderId, order.getDelivery().getDeliveryServiceId(), SegmentStatus.TRANSIT_COURIER_SEARCH);
        LOM_ORDER_STEPS.verifyOrderSegmentStatus(lomOrderId, order.getDelivery().getDeliveryServiceId(), SegmentStatus.TRANSIT_PICKUP);

        onDemandUrl = BLUE_F_API_STEPS.resolveOnDemandLink(external.getExternalId());
        TAXI_STEPS.transferActivate(onDemandUrl.getTransferId(), external.getExternalId());
        LOM_ORDER_STEPS.verifyOrderSegmentStatus(lomOrderId, order.getDelivery().getDeliveryServiceId(), SegmentStatus.TRANSIT_COURIER_SEARCH);
        LOM_ORDER_STEPS.verifyOrderSegmentStatus(lomOrderId, order.getDelivery().getDeliveryServiceId(), SegmentStatus.TRANSIT_PICKUP);

        onDemandUrl = BLUE_F_API_STEPS.resolveOnDemandLink(external.getExternalId());
        TAXI_STEPS.transferActivate(onDemandUrl.getTransferId(), external.getExternalId());
        LOM_ORDER_STEPS.verifySegmentStatusCount(
                lomOrderId,
                order.getDelivery().getDeliveryServiceId(),
                SegmentStatus.TRANSIT_COURIER_SEARCH,
                4
        );
        LOM_ORDER_STEPS.verifyOrderSegmentStatus(lomOrderId, order.getDelivery().getDeliveryServiceId(), SegmentStatus.TRANSIT_TRANSMITTED_TO_RECIPIENT);
    }

    @Test
    @Tag("ExcludeRegress")
    @DisplayName("Создание и успешная доставка с 4й попытки 3й статус водитель не мог доставить заказа ондеманд ПВЗ")
    public void createAndSuccessDelivery4thTry3dCargoCourierPvzOrder() {
        log.debug("Trying to create ondemand pvz order");

        createOrderWithComment(OrderComment.FIND_COURIER_4_ATTEMPT_3_CARGO);

        WaybillSegmentDto middleSDWaybillSegment = LOM_ORDER_STEPS.getWaybillSegmentForPartner(lomOrderId, pickupPartner);
        DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(middleSDWaybillSegment.getTrackerId(), OrderDeliveryCheckpointStatus.DELIVERY_ARRIVED_PICKUP_POINT);

        LOM_ORDER_STEPS.verifyOrderSegmentStatus(lomOrderId, order.getDelivery().getDeliveryServiceId(), SegmentStatus.TRANSIT_PICKUP);

        WaybillSegmentDto external = LOM_ORDER_STEPS.getWaybillSegmentForPartner(lomOrderId, order.getDelivery().getDeliveryServiceId());
        ResolveLink.Collection.OnDemandUrl onDemandUrl = BLUE_F_API_STEPS.resolveOnDemandLink(external.getExternalId());

        TAXI_STEPS.transferActivate(onDemandUrl.getTransferId(), external.getExternalId());
        LOM_ORDER_STEPS.verifyOrderSegmentStatus(lomOrderId, order.getDelivery().getDeliveryServiceId(), SegmentStatus.TRANSIT_COURIER_SEARCH);
        LOM_ORDER_STEPS.verifyOrderSegmentStatus(lomOrderId, order.getDelivery().getDeliveryServiceId(), SegmentStatus.TRANSIT_PICKUP);

        onDemandUrl = BLUE_F_API_STEPS.resolveOnDemandLink(external.getExternalId());
        TAXI_STEPS.transferActivate(onDemandUrl.getTransferId(), external.getExternalId());

        LOM_ORDER_STEPS.verifyOrderSegmentStatus(lomOrderId, order.getDelivery().getDeliveryServiceId(), SegmentStatus.TRANSIT_COURIER_SEARCH);
        LOM_ORDER_STEPS.verifyOrderSegmentStatus(lomOrderId, order.getDelivery().getDeliveryServiceId(), SegmentStatus.TRANSIT_PICKUP);

        onDemandUrl = BLUE_F_API_STEPS.resolveOnDemandLink(external.getExternalId());
        TAXI_STEPS.transferActivate(onDemandUrl.getTransferId(), external.getExternalId());

        LOM_ORDER_STEPS.verifyOrderSegmentStatus(lomOrderId, order.getDelivery().getDeliveryServiceId(), SegmentStatus.TRANSIT_COURIER_SEARCH);
        LOM_ORDER_STEPS.verifyOrderSegmentStatus(lomOrderId, order.getDelivery().getDeliveryServiceId(), SegmentStatus.TRANSIT_PICKUP);

        onDemandUrl = BLUE_F_API_STEPS.resolveOnDemandLink(external.getExternalId());
        TAXI_STEPS.transferActivate(onDemandUrl.getTransferId(), external.getExternalId());

        LOM_ORDER_STEPS.verifySegmentStatusCount(
                lomOrderId,
                order.getDelivery().getDeliveryServiceId(),
                SegmentStatus.TRANSIT_COURIER_SEARCH,
                4
        );
        LOM_ORDER_STEPS.verifyOrderSegmentStatus(lomOrderId, order.getDelivery().getDeliveryServiceId(), SegmentStatus.TRANSIT_TRANSMITTED_TO_RECIPIENT);
    }


    @Test
    @DisplayName("Создание и не смогли найти курьера для доставки ондеманд ПВЗ")
    public void createAndRejectCourierPvzOrder() {
        log.debug("Trying to create ondemand pvz order");

        createOrderWithComment(OrderComment.CANT_FIND_COURIER);

        WaybillSegmentDto middleSDWaybillSegment = LOM_ORDER_STEPS.getWaybillSegmentForPartner(lomOrderId, pickupPartner);
        DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(middleSDWaybillSegment.getTrackerId(), OrderDeliveryCheckpointStatus.DELIVERY_ARRIVED_PICKUP_POINT);

        LOM_ORDER_STEPS.verifyOrderSegmentStatus(lomOrderId, order.getDelivery().getDeliveryServiceId(), SegmentStatus.TRANSIT_PICKUP);

        WaybillSegmentDto external = LOM_ORDER_STEPS.getWaybillSegmentForPartner(lomOrderId, order.getDelivery().getDeliveryServiceId());
        ResolveLink.Collection.OnDemandUrl onDemandUrl = BLUE_F_API_STEPS.resolveOnDemandLink(external.getExternalId());

        TAXI_STEPS.transferActivate(onDemandUrl.getTransferId(), external.getExternalId());

        LOM_ORDER_STEPS.verifyOrderSegmentStatus(lomOrderId, order.getDelivery().getDeliveryServiceId(), SegmentStatus.TRANSIT_COURIER_SEARCH);
    }


    @Test
    @DisplayName("Создание и протухание после неудачной доставки заказа ондеманд ПВЗ")
    public void createAndCourierReturnExpiredPvzOrder() {
        log.debug("Trying to create ondemand pvz order");

        createOrderWithComment(OrderComment.COURIER_RETURN);

        WaybillSegmentDto middleSDWaybillSegment = LOM_ORDER_STEPS.getWaybillSegmentForPartner(lomOrderId, pickupPartner);
        DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(middleSDWaybillSegment.getTrackerId(), OrderDeliveryCheckpointStatus.DELIVERY_ARRIVED_PICKUP_POINT);

        LOM_ORDER_STEPS.verifyOrderSegmentStatus(lomOrderId, order.getDelivery().getDeliveryServiceId(), SegmentStatus.TRANSIT_PICKUP);

        WaybillSegmentDto external = LOM_ORDER_STEPS.getWaybillSegmentForPartner(lomOrderId, order.getDelivery().getDeliveryServiceId());
        ResolveLink.Collection.OnDemandUrl onDemandUrl = BLUE_F_API_STEPS.resolveOnDemandLink(external.getExternalId());

        TAXI_STEPS.transferActivate(onDemandUrl.getTransferId(), external.getExternalId());

        LOM_ORDER_STEPS.verifyOrderSegmentStatus(lomOrderId, order.getDelivery().getDeliveryServiceId(), SegmentStatus.TRANSIT_DELIVERY_ATTEMPT_FAILED);

        DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(middleSDWaybillSegment.getTrackerId(), OrderDeliveryCheckpointStatus.DELIVERY_STORAGE_PERIOD_EXPIRED);

        LOM_ORDER_STEPS.verifyOrderSegmentStatus(lomOrderId, order.getDelivery().getDeliveryServiceId(), SegmentStatus.RETURN_ARRIVED);
    }
}
