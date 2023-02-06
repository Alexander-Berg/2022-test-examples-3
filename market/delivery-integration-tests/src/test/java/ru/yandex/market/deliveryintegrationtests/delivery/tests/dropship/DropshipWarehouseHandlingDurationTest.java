package ru.yandex.market.deliveryintegrationtests.delivery.tests.dropship;

import dto.requests.checkouter.CreateOrderParameters;
import factory.OfferItems;
import step.CheckouterSteps;
import step.LmsSteps;
import step.LomOrderSteps;
import step.PartnerApiSteps;
import io.qameta.allure.Epic;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import ru.qatools.properties.Property;
import ru.qatools.properties.Resource;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.logistics.lom.model.enums.OrderStatus;
import toolkit.Delayer;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Resource.Classpath({"delivery/checkouter.properties", "delivery/delivery.properties", "delivery/report.properties"})
@DisplayName("Warehouse Handling Duration Test")
@Epic("Blue Dropship")
@Slf4j
@Tag("SlowTest")
public class DropshipWarehouseHandlingDurationTest extends AbstractDropshipTest {

    @Property("reportblue.dropshipSCCampaignId")
    private long dropshipSCCampaignId;

    @Property("reportblue.dropshipSCUID")
    private long dropshipSCUID;

    private final CheckouterSteps ORDER_STEPS = new CheckouterSteps();
    private final LomOrderSteps LOM_ORDER_STEPS = new LomOrderSteps();
    private final LmsSteps LMS_STEPS = new LmsSteps();
    private PartnerApiSteps partnerApiSteps;

    private final List<Order> orders = new ArrayList<>();

    private static final String DURATION_1_H = "PT1H";
    private static final String DURATION_24_H = "PT24H";

    @BeforeEach
    @Step("Подготовка данных")
    public void setUp() {
        partnerApiSteps = new PartnerApiSteps(dropshipSCUID, dropshipSCCampaignId);
    }

    @Step("Создание заказа")
    public Order createOrder() {
        log.info("Creating order");

        CreateOrderParameters params = CreateOrderParameters
                .newBuilder(regionId, OfferItems.DROPSHIP_SC.getItem(), DeliveryType.DELIVERY)
                .build();

        Order order = ORDER_STEPS.createOrder(params);

        Long lomOrder = LOM_ORDER_STEPS.getLomOrderId(order);

        partnerApiSteps.packOrder(order);

        ORDER_STEPS.shipDropshipOrder(order);
        ORDER_STEPS.verifySDTracksCreated(order);

        LOM_ORDER_STEPS.verifyOrderStatus(lomOrder, OrderStatus.PROCESSING);

        orders.add(order);

        return order;
    }

    @Test
    @DisplayName("Dropship: Изменение срока обработки на сегменте склада")
    public void dropshipWarehouseHandlingDurationTest() {

        // Первый заказ нужен для получения id склада
        createOrder();

        // Получаем id склада
        Integer partnerId = OfferItems.DROPSHIP_SC.getItem().getWarehouseId();
        log.debug("WarehouseId is: {}", partnerId);

        // Выставляем время обработки склада в 1 час
        LMS_STEPS.changeWarehouseHandlingDuration(partnerId, DURATION_1_H);

        // Ждём 10 минут, чтобы настройка проросла
        Delayer.delay(10, TimeUnit.MINUTES);

        Order secondOrder = createOrder();

        // Получаем дату отгрузки второго заказа из Чекаутера
        LocalDate secondShipmentDate = ORDER_STEPS.getOrderShipmentDate(secondOrder);
        log.debug("Shipment date is: {}", secondShipmentDate);

        Assumptions.assumeTrue(secondShipmentDate.isAfter(LocalDate.now().plusDays(2)),
                "Дата отгрузки по каким-то причинам далеко в будущем. Скорее всего, тест не будет работать.");

        // Выставляем время обработки склада в 24 часа
        LMS_STEPS.changeWarehouseHandlingDuration(partnerId, DURATION_24_H);

/*
    Если нужно увеличить срок обработки на произвольное значение, то можно сделать так

        // Получаем срок обработки вида "PT1H", убираем кавычки, парсим в Duration
        Duration durationInitial = Duration.parse(lmsSteps.getWarehouseHandlingDuration(partnerId).replaceAll("\"", ""));
        // Добавляем 48 часов, пребразуем в String, добавляем кавычки
        String duration = "\""
                + String.valueOf(durationInitial.plusHours(48))
                + "\"";
        // Изменяем срок обработки
        lmsSteps.changeWarehouseHandlingDuration(partnerId, duration);
*/

        // Ждём 10 минут, чтобы настройка проросла
        Delayer.delay(10, TimeUnit.MINUTES);

        Order testOrder = createOrder();

        // Получаем дату отгрузки третьего заказа из Чекаутера
        LocalDate testShipmentDate = ORDER_STEPS.getOrderShipmentDate(testOrder);
        log.debug("New shipment date is: {}", testShipmentDate);

        // Проверяем, что дата отгрузки третьего заказа больше, чем у второго
        Assertions.assertTrue(testShipmentDate.isAfter(secondShipmentDate), "Дата отгрузки не увеличилась");

    }

    @AfterEach
    @Step("Чистка данных после теста")
    public void tearDown() {
        log.info("Сanceling all orders");
        ORDER_STEPS.tearDownOrders(orders);
    }
}
