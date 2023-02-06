package ru.yandex.market.deliveryintegrationtests.delivery.tests.dropship;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import javax.validation.Valid;

import dto.requests.checkouter.CreateOrderParameters;
import dto.requests.checkouter.RearrFactor;
import dto.responses.lgw.LgwTaskFlow;
import factory.OfferItems;
import io.qameta.allure.Epic;
import io.qameta.allure.TmsLink;
import io.qameta.allure.TmsLinks;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import ru.qatools.properties.Property;
import ru.qatools.properties.Resource;
import step.PartnerApiSteps;

import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.delivery.tracker.domain.enums.OrderDeliveryCheckpointStatus;
import ru.yandex.market.logistics.lom.model.dto.CombinatorRoute;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentDto;
import ru.yandex.market.logistics.lom.model.enums.OrderStatus;
import ru.yandex.market.logistics.lom.model.enums.PartnerType;
import ru.yandex.market.logistics.lom.model.enums.PointType;
import ru.yandex.market.logistics.lom.model.enums.SegmentStatus;
import ru.yandex.market.logistics.lom.model.enums.SegmentType;


@Resource.Classpath({"delivery/checkouter.properties", "delivery/report.properties"})
@DisplayName("Blue Dropship DR order Test")
@Epic("Blue Dropship")
@Slf4j

public class DropshipDRPvzTest extends AbstractDropshipTest {

    public static final List<OrderDeliveryCheckpointStatus> PICKUP_CHECKPOINTS = List.of(
            OrderDeliveryCheckpointStatus.DELIVERY_AT_START,
            OrderDeliveryCheckpointStatus.DELIVERY_AT_START_SORT,
            OrderDeliveryCheckpointStatus.DELIVERY_ARRIVED_PICKUP_POINT
    );
    public static final List<OrderDeliveryCheckpointStatus> DELIVERY_CHECKPOINTS = List.of(
            OrderDeliveryCheckpointStatus.DELIVERY_AT_START,
            OrderDeliveryCheckpointStatus.DELIVERY_AT_START_SORT
    );

    @Property("reportblue.dropshipDRPvzCampaignId")
    private long dropshipDRPvzCampaignId;

    @Property("reportblue.dropshipDRPvzUID")
    private long dropshipDRPvzUID;

    private final List<Long> outletId = Collections.singletonList(10000904964L);

    @BeforeEach
    public void init() {
        partnerApiSteps = new PartnerApiSteps(dropshipDRPvzUID, dropshipDRPvzCampaignId);

    }

    @ParameterizedTest(name = "Dropship Dropoff: Создание дропофф как ПВЗ заказа типа {0}")
    @EnumSource(value = DeliveryType.class, names = {"DELIVERY", "PICKUP"})
    @TmsLinks({@TmsLink(value = "logistic-69"), @TmsLink(value = "logistic-71")})
    // DELIVERY – https://testpalm.yandex-team.ru/testcase/logistic-69
    // PICKUP – https://testpalm.yandex-team.ru/testcase/logistic-71
    public void createDropshipDRPvzOrderTest(DeliveryType deliveryType) {
        log.info("Starting createDropshipDropoffOrderTest");

        params = CreateOrderParameters
                .newBuilder(regionId, OfferItems.DROPSHIP_DR_PVZ.getItem(), deliveryType)
                .paymentType(PaymentType.POSTPAID)
                .paymentMethod(PaymentMethod.CARD_ON_DELIVERY)
                .experiment(EnumSet.noneOf(RearrFactor.class))
                .build();
        order = ORDER_STEPS.createOrder(params);

        partnerApiSteps.packOrder(order);
        lomOrderId = LOM_ORDER_STEPS.getLomOrderId(order);

        ORDER_STEPS.shipDropshipOrder(order);
        LOM_ORDER_STEPS.verifyOrderStatus(lomOrderId, OrderStatus.PROCESSING);

        CombinatorRoute orderCombinatorRoute = LOM_ORDER_STEPS.getOrderRoute(lomOrderId).getText();
        List<PartnerType> partnerTypes = orderCombinatorRoute.getRoute()
                .getPoints()
                .stream()
                .filter(point -> point.getSegmentType().equals(PointType.WAREHOUSE))
                .map(CombinatorRoute.Point::getPartnerType)
                .collect(Collectors.toList());
        PartnerType[] expectedPartnerTypes = new PartnerType[]{
            PartnerType.DROPSHIP,
            PartnerType.DELIVERY,
            PartnerType.SORTING_CENTER,
            PartnerType.SORTING_CENTER
        };
        Assertions.assertThat(partnerTypes)
                .as("Некорректный порядок партнеров в руте у заказа " + lomOrderId)
                .containsExactly(expectedPartnerTypes);
        if (deliveryType == DeliveryType.PICKUP) {
            List<CombinatorRoute.@Valid Point> pickupPoints = orderCombinatorRoute.getRoute()
                    .getPoints()
                    .stream()
                    .filter(point -> point.getSegmentType().equals(PointType.PICKUP))
                    .collect(Collectors.toList());
            Assertions.assertThat(pickupPoints).as("Точка вручения не ПВЗ у заказа " + lomOrderId).isNotEmpty();
        }
    }

    @Test
    @TmsLink("logistic-49")
    @DisplayName("Dropship Dropoff: Создание дропофф как ПВЗ заказа в тот самый ПВЗ")
    public void createDropshipDRPvzToPvzOrderTest() {
        log.info("Starting createDropshipDropoffOrderTest");

        params = CreateOrderParameters
                .newBuilder(regionId, OfferItems.DROPSHIP_DR_PVZ.getItem(), DeliveryType.PICKUP)
                .paymentType(PaymentType.POSTPAID)
                .paymentMethod(PaymentMethod.CARD_ON_DELIVERY)
                .experiment(EnumSet.noneOf(RearrFactor.class))
                .outletId(outletId)
                .build();
        order = ORDER_STEPS.createOrder(params);

        partnerApiSteps.packOrder(order);
        lomOrderId = LOM_ORDER_STEPS.getLomOrderId(order);

        ORDER_STEPS.shipDropshipOrder(order);
        LOM_ORDER_STEPS.verifyOrderStatus(lomOrderId, OrderStatus.PROCESSING);

        CombinatorRoute orderCombinatorRoute = LOM_ORDER_STEPS.getOrderRoute(lomOrderId).getText();
        List<PartnerType> partnerTypes = orderCombinatorRoute.getRoute()
                .getPoints()
                .stream()
                .filter(point -> point.getSegmentType().equals(PointType.WAREHOUSE))
                .map(CombinatorRoute.Point::getPartnerType)
                .collect(Collectors.toList());
        Assertions.assertThat(partnerTypes)
            .as("Некорректный порядок партнеров в роуте у заказа " + lomOrderId)
                .containsExactly(
                        PartnerType.DROPSHIP,
                        PartnerType.DELIVERY,
                        PartnerType.SORTING_CENTER,
                        PartnerType.SORTING_CENTER
                );
        List<CombinatorRoute.@Valid Point> pickupPoints = orderCombinatorRoute.getRoute()
                .getPoints()
                .stream()
                .filter(point -> point.getSegmentType().equals(PointType.PICKUP))
                .collect(Collectors.toList());
        Assertions.assertThat(pickupPoints).as("Точка вручения не ПВЗ у заказа " + lomOrderId).isNotEmpty();
    }

    @ParameterizedTest(name = "Dropship Dropoff: Создание дропофф как ПВЗ заказа и переход его в статус в {0}")
    @EnumSource(value = DeliveryType.class, names = {"DELIVERY", "PICKUP"})
    @TmsLinks({@TmsLink(value = "logistic-70"), @TmsLink(value = "logistic-72")})
    // DELIVERY – https://testpalm.yandex-team.ru/testcase/logistic-70
    // PICKUP – https://testpalm.yandex-team.ru/testcase/logistic-72
    public void createDropshipDRPvzToCourierOrderTest(DeliveryType deliveryType) {
        log.info("Starting createDropshipDRPvzToCourierOrderTest");

        SegmentType segmentType = deliveryType == DeliveryType.PICKUP
                ? SegmentType.PICKUP
                : SegmentType.COURIER;

        params = CreateOrderParameters
                .newBuilder(regionId, OfferItems.DROPSHIP_DR_PVZ.getItem(), deliveryType)
                .paymentType(PaymentType.POSTPAID)
                .paymentMethod(PaymentMethod.CARD_ON_DELIVERY)
                .experiment(EnumSet.noneOf(RearrFactor.class))
                .build();
        order = ORDER_STEPS.createOrder(params);

        partnerApiSteps.packOrder(order);
        lomOrderId = LOM_ORDER_STEPS.getLomOrderId(order);

        ORDER_STEPS.shipDropshipOrder(order);
        LOM_ORDER_STEPS.verifyOrderStatus(lomOrderId, OrderStatus.PROCESSING);

        List<WaybillSegmentDto> waybillSegments = LOM_ORDER_STEPS.getWaybillSegments(lomOrderId);
        waybillSegments.stream()
                .filter(s -> s.getSegmentType() == segmentType)
                .map(WaybillSegmentDto::getPartnerId)
                .forEach(partnerId -> {
                    LOM_ORDER_STEPS.verifyOrderSegmentStatus(lomOrderId, partnerId, SegmentStatus.INFO_RECEIVED);
                });

        List<Long> ffTrackerIds = waybillSegments.stream()
                .filter(s -> s.getSegmentType() == SegmentType.FULFILLMENT || s.getSegmentType() == SegmentType.SORTING_CENTER)
                .map(WaybillSegmentDto::getTrackerId)
                .collect(Collectors.toList());
        ffTrackerIds.forEach(trackerId ->{
            List.of(
                    OrderDeliveryCheckpointStatus.SORTING_CENTER_AT_START,
                    OrderDeliveryCheckpointStatus.SORTING_CENTER_PREPARED,
                    OrderDeliveryCheckpointStatus.SORTING_CENTER_TRANSMITTED
            ).forEach(checkpoint -> {
                DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(trackerId, checkpoint);
            });
        });

        LOM_ORDER_STEPS.verifyOrderStatus(lomOrderId, OrderStatus.PROCESSING);


        List<Long> dsTrackerIds = waybillSegments.stream()
                .filter(s -> s.getSegmentType() == segmentType)
                .map(WaybillSegmentDto::getTrackerId)
                .collect(Collectors.toList());
        dsTrackerIds.forEach(trackerId ->{
            List<OrderDeliveryCheckpointStatus> checkpoints = deliveryType == DeliveryType.PICKUP
                    ? PICKUP_CHECKPOINTS
                    : DELIVERY_CHECKPOINTS;
            checkpoints
                    .forEach(checkpoint -> {
                        DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(trackerId, checkpoint);
                    });
        });

        SegmentStatus segmentStatus = deliveryType == DeliveryType.PICKUP
                ? SegmentStatus.TRANSIT_PICKUP
                : SegmentStatus.TRANSIT_DELIVERY_AT_START_SORT;

        ru.yandex.market.checkout.checkouter.order.OrderStatus orderStatus = deliveryType == DeliveryType.PICKUP
                ? ru.yandex.market.checkout.checkouter.order.OrderStatus.PICKUP
                : ru.yandex.market.checkout.checkouter.order.OrderStatus.DELIVERY;

        Long lastMilePartnerId = waybillSegments.stream()
                .filter(s -> s.getSegmentType() == segmentType)
                .map(WaybillSegmentDto::getPartnerId)
                .findFirst()
                .orElseThrow();

        LOM_ORDER_STEPS.verifyOrderSegmentStatus(lomOrderId, lastMilePartnerId, segmentStatus);
        ORDER_STEPS.verifyForOrderStatus(order, orderStatus);

    }

    @Test
    @TmsLink("logistic-50")
    @DisplayName("Dropship Dropoff: Отмена дропофф как ПВЗ заказа до передачи в СД")
    public void cancelDropshipDRPvzTest() {
        log.info("Starting cancelDropshipDRPvzTest");

        params = CreateOrderParameters
                .newBuilder(regionId, OfferItems.DROPSHIP_DR_PVZ.getItem(), DeliveryType.PICKUP)
                .paymentType(PaymentType.POSTPAID)
                .paymentMethod(PaymentMethod.CARD_ON_DELIVERY)
                .experiment(EnumSet.noneOf(RearrFactor.class))
                .build();
        order = ORDER_STEPS.createOrder(params);

        partnerApiSteps.packOrder(order);
        lomOrderId = LOM_ORDER_STEPS.getLomOrderId(order);

        ORDER_STEPS.shipDropshipOrder(order);
        LOM_ORDER_STEPS.verifyOrderStatus(lomOrderId, OrderStatus.PROCESSING);

        List<WaybillSegmentDto> waybillSegments = LOM_ORDER_STEPS.getWaybillSegments(lomOrderId);
        waybillSegments.stream()
                .filter(s -> s.getSegmentType() == SegmentType.PICKUP)
                .map(WaybillSegmentDto::getPartnerId)
                .forEach(partnerId -> {
                    LOM_ORDER_STEPS.verifyOrderSegmentStatus(lomOrderId, partnerId, SegmentStatus.INFO_RECEIVED);
                });

        ORDER_STEPS.cancelOrder(order);

        LGW_STEPS.getTasksFromListWithEntityIdAndRequestFlow(
            String.valueOf(order.getId()),
            LgwTaskFlow.FF_CANCEL_ORDER_SUCCESS,
            4,
            "READY"
        );
        LGW_STEPS.getTasksFromListWithEntityIdAndRequestFlow(
            String.valueOf(order.getId()),
            LgwTaskFlow.DS_CANCEL_ORDER_SUCCESS,
            1,
            "READY"
        );

        LOM_ORDER_STEPS.verifyOrderStatus(lomOrderId, OrderStatus.CANCELLED);
    }
}


