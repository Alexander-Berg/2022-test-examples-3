package ru.yandex.market.deliveryintegrationtests.delivery.tests.capacity;

import dto.responses.lms.PartnerCapacityDto;
import dto.requests.checkouter.CreateOrderParameters;
import factory.OfferItems;
import io.qameta.allure.Epic;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import ru.qatools.properties.Resource;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.logistics.management.entity.type.CapacityService;
import ru.yandex.market.logistics.management.entity.type.CountingType;
import toolkit.Delayer;
import toolkit.OperationsHelper;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.chrono.ChronoLocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Resource.Classpath({"delivery/checkouter.properties"})
@DisplayName("Capacity Test")
@Epic("Capacity")
@Slf4j
@Execution(ExecutionMode.SAME_THREAD)
@Disabled
@Tag("CapacityTest")
public class CapacityTest extends AbstractCapacityTest {


    private PartnerCapacityDto capacityInLms;

    private static final Long CAPACITY_VALUE = 2L;

    @BeforeEach
    @Step("Подготовка данных")
    public void setUp() {
        CAPACITY_STORAGE_STEPS.snapshot();
    }

    @Step("Создание заказа {offerItem}")
    private Order createOrder(OfferItems offerItem) {
        params = CreateOrderParameters
                .newBuilder(offerItem.equals(OfferItems.FF_300_UNFAIR_STOCK_EXPRESS) ? 54L : regionId, offerItem.getItem(), DeliveryType.DELIVERY)
                .paymentMethod(PaymentMethod.YANDEX)
                .paymentType(PaymentType.PREPAID)
                .build();
        Delivery minDeliveryOption = ORDER_STEPS.getEarliestDeliveryOption(params);
        Order order = ORDER_STEPS.checkout(params, minDeliveryOption).get(0);
        ORDER_STEPS.payOrder(order);
        orders.add(order);
        return order;
    }


    @ParameterizedTest(name = "Проверка капасити у партнера {0} OfferItems={1} CapacityService={2} CountingType={3}")
    @MethodSource("getParams")
    public void capacityPartnerTest(Long partnerId, OfferItems offerItem, CapacityService capacityService, CountingType countingType) {
        capacityInLms = LMS_STEPS.createCapacityInLmsIfItIsNotAvailable(
                capacityService,
                countingType,
                225,
                225,
                partnerId,
                null,
                CAPACITY_VALUE,
                null
        );


        createOrder(offerItem);

        order = createOrder(offerItem);
        Long lomOrderId = LOM_ORDER_STEPS.getLomOrderId(order);
        Map<String, List<LocalDateTime>> pointAndServicesTime = LOM_ORDER_STEPS.getOrderRoute(lomOrderId).getText()
                .getRoute().getPoints().stream()
                .collect(Collectors.toMap(
                        point -> "Партнер = " + point.getIds().getPartnerId() + " Логистическая точка = " +
                                point.getIds().getLogisticPointId() + " " + point.getSegmentType(),
                        point -> point.getServices().stream()
                                .map(s ->
                                        LocalDateTime.ofEpochSecond(
                                                s.getStartTime().getSeconds(),
                                                s.getStartTime().getNanos(), ZoneOffset.UTC))
                                .collect(Collectors.toList()))
                );

        DeliveryDates deliveryShipmentDate = ORDER_STEPS.getEarliestDeliveryOption(params).getDeliveryDates();

        CAPACITY_STORAGE_STEPS.snapshot();

        ORDER_STEPS.verifyDeliveryShipmentDateChanged(deliveryShipmentDate, params, true);

        order = createOrder(offerItem);
        lomOrderId = LOM_ORDER_STEPS.getLomOrderId(order);
        Map<String, List<LocalDateTime>> afterPointAndServicesTime = LOM_ORDER_STEPS.getOrderRoute(lomOrderId)
                .getText().getRoute().getPoints().stream()
                .collect(Collectors.toMap(
                        point -> "Партнер = " + point.getIds().getPartnerId() + " Логистическая точка = " +
                                point.getIds().getLogisticPointId() + " " + point.getSegmentType(),
                        point -> point.getServices().stream()
                                .map(s ->
                                        LocalDateTime.ofEpochSecond(
                                                s.getStartTime().getSeconds(),
                                                s.getStartTime().getNanos(), ZoneOffset.UTC))
                                .collect(Collectors.toList()))
                );

        for (String pointIds : pointAndServicesTime.keySet()) {
            List<LocalDateTime> localDateTimes = pointAndServicesTime.get(pointIds);
            List<LocalDateTime> afterLocalDateTimes = afterPointAndServicesTime.get(pointIds);
            for (int i = 0; i < localDateTimes.size(); i++) {
                LocalDateTime beforeLocalDateTime = localDateTimes.get(i);
                LocalDateTime afterLocalDateTime = afterLocalDateTimes.get(i);
                Assertions.assertTrue(beforeLocalDateTime.toLocalDate().isBefore(ChronoLocalDate.from(afterLocalDateTime)),
                        "Дата не сдвинулась у партнера " + pointIds);
            }
        }
    }

    @ParameterizedTest(name = "Проверка капасити у партнера {0} при параллельном создании заказов  OfferItems={1} CapacityService={2} CountingType={3}")
    @MethodSource("getParams")
    public void capacityParallelOrderTest(Long partnerId, OfferItems offerItem, CapacityService capacityService, CountingType countingType) {
        capacityInLms = LMS_STEPS.createCapacityInLmsIfItIsNotAvailable(
                capacityService,
                countingType,
                225,
                225,
                partnerId,
                null,
                CAPACITY_VALUE,
                null
        );

        boolean result = OperationsHelper.checkMultiplyTimes(() -> createOrder(offerItem), 2);
        Assertions.assertTrue(result, "Не удалось создать 2 параллельных заказа");
        DeliveryDates deliveryShipmentDate = ORDER_STEPS.getEarliestDeliveryOption(params).getDeliveryDates();

        CAPACITY_STORAGE_STEPS.snapshot();

        ORDER_STEPS.verifyDeliveryShipmentDateChanged(deliveryShipmentDate, params, true);
    }

    @ParameterizedTest(name = "Проверка сбрасывания дейоффа после отмены заказов у партнера {0}  CapacityService={2} CountingType={3}")
    @MethodSource("getParams")
    public void capacityCancelDayOffTest(Long partnerId, OfferItems offerItem, CapacityService capacityService, CountingType countingType) {
        capacityInLms = LMS_STEPS.createCapacityInLmsIfItIsNotAvailable(
                capacityService,
                countingType,
                225,
                225,
                partnerId,
                null,
                CAPACITY_VALUE,
                null
        );
        createOrder(offerItem);
        Order order = ORDER_STEPS.createOrder(params);

        DeliveryDates deliveryShipmentDate = ORDER_STEPS.getEarliestDeliveryOption(params).getDeliveryDates();

        CAPACITY_STORAGE_STEPS.snapshot();
        DeliveryDates actualDates = ORDER_STEPS.verifyDeliveryShipmentDateChanged(deliveryShipmentDate, params, true);

        ORDER_STEPS.cancelOrder(order);

        CAPACITY_STORAGE_STEPS.snapshot();

        ORDER_STEPS.verifyDeliveryShipmentDateChanged(actualDates, params, false);
    }


    @ParameterizedTest(name = "Проверка проставления дейоффа при выставлении капасити после его переполнения партнера {0} OfferItems={1}  CapacityService={2} CountingType={3}")
    @MethodSource("getParams")
    public void createCapacityAfterOverflow(Long partnerId, OfferItems offerItem, CapacityService capacityService, CountingType countingType) {

        createOrder(offerItem);
        createOrder(offerItem);
        order = ORDER_STEPS.createOrder(params);

        DeliveryDates deliveryShipmentDate = ORDER_STEPS.getEarliestDeliveryOption(params).getDeliveryDates();

        capacityInLms = LMS_STEPS.createCapacityInLmsIfItIsNotAvailable(
                capacityService,
                countingType,
                225,
                225,
                partnerId,
                null,
                CAPACITY_VALUE,
                null
        );
        CAPACITY_STORAGE_STEPS.snapshot();
        ORDER_STEPS.verifyDeliveryShipmentDateChanged(deliveryShipmentDate, params, true);
    }


    @AfterEach
    public void tearDown() {
        orders.forEach(order -> log.info("Created order id = " + order.getId()));
        log.info("Capacity id = " + capacityInLms.getId());
        ORDER_STEPS.tearDownOrders(orders);
        LMS_STEPS.deleteCapacityInLms(capacityInLms.getId());
        CAPACITY_STORAGE_STEPS.snapshot();
        Delayer.delay(3, TimeUnit.MINUTES);
    }


}
