package ru.yandex.market.deliveryintegrationtests.delivery.tests.recalculate;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import dto.requests.checkouter.CreateOrderParameters;
import factory.OfferItems;
import io.qameta.allure.Epic;
import io.qameta.allure.Step;
import io.qameta.allure.TmsLink;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.qatools.properties.Resource;

import ru.yandex.market.checkout.checkouter.delivery.DeliveryServiceType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.delivery.tracker.domain.enums.OrderDeliveryCheckpointStatus;
import ru.yandex.market.logistics.lom.model.enums.OrderStatus;
import ru.yandex.market.logistics.lom.model.enums.SegmentType;

@Slf4j
@Resource.Classpath({"delivery/checkouter.properties", "delivery/delivery.properties"})
@DisplayName("Recalculate of delivery date")
@Epic("Recalculate of delivery date")
public class ChangeDeliveryDateTest extends AbstractRecalculateTest {

    private LocalDate cpaDeliveryDateBefore;

    private List<Integer> mocksId = new ArrayList<>();

    @BeforeEach
    @Step("Подготовка данных: Создаем заказ")
    public void setUp() {

        log.info("Creating order for changeOrdersDeliveryDate test...");

        params = CreateOrderParameters
            .newBuilder(regionId, OfferItems.FF_171_UNFAIR_STOCK.getItem(), DeliveryType.PICKUP)
            .forceDeliveryId(pickpointServiceId)
            .build();
        order = ORDER_STEPS.createOrder(params);

        cpaDeliveryDateBefore = order.getDelivery().getDeliveryDates().getToDate()
            .toInstant()
            .atZone(ZoneId.systemDefault())
            .toLocalDate();

        lomOrderId = LOM_ORDER_STEPS.getLomOrderId(order);
        LOM_ORDER_STEPS.verifyOrderStatus(lomOrderId, OrderStatus.PROCESSING);

        ORDER_STEPS.verifySDTracksCreated(order);
        ORDER_STEPS.verifyFFTrackCreated(order);

        LOM_ORDER_STEPS.verifyTrackerIds(lomOrderId);

        LOM_ORDER_STEPS.getLomOrderData(order).getWaybill().stream()
            .filter(ws -> ws.getSegmentType() == SegmentType.PICKUP)
            .findAny()
            .orElseGet(() -> Assertions.fail("Не найден сегмент с типом PICKUP"));
    }

    @AfterEach
    @Step("Чистка моков после теста")
    public void tearDown() {
        for (int mockId : mocksId) {
            MOCK_CLIENT.deleteMockById(mockId);
        }
        mocksId.clear();
    }

    @Test
    @TmsLink("logistic-83")
    @DisplayName("Проверка изменения даты доставки после получения 44 чекпоинта")
    public void changeDeliveryDateTest() {
        log.info("Starting Сhange Delivery Date test...");

        LocalDate localDate = order.getDelivery()
            .getDeliveryDates()
            .getFromDate()
            .toInstant()
            .atZone(ZoneId.systemDefault())
            .toLocalDate();

        LocalDate newDeliveryDate = localDate.plusDays(7);
        mocksId.add(MOCK_CLIENT.mockGetOrdersDeliveryDate(
            order.getId(),
            newDeliveryDate.toString()
        ));

        long trackerId = ORDER_STEPS.getTrackNumbers(order, DeliveryServiceType.CARRIER).getTrackerId();

        DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(
            trackerId,
            OrderDeliveryCheckpointStatus.DELIVERY_UPDATED_BY_SHOP
        );
        ORDER_STEPS.verifyForCheckpointReceived(
            order,
            OrderDeliveryCheckpointStatus.DELIVERY_UPDATED_BY_SHOP,
            DeliveryServiceType.CARRIER
        );

        ORDER_STEPS.verifyForOrderHasNewDates(order.getId(), newDeliveryDate);

        LocalDate nextDeliveryDate = localDate.plusDays(14);
        mocksId.add(MOCK_CLIENT.mockGetOrdersDeliveryDate(
            order.getId(),
            nextDeliveryDate.toString()
        ));

        DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(
            trackerId,
            OrderDeliveryCheckpointStatus.DELIVERY_UPDATED_BY_SHOP
        );
        ORDER_STEPS.verifyForCheckpointReceived(
            order,
            OrderDeliveryCheckpointStatus.DELIVERY_UPDATED_BY_SHOP,
            DeliveryServiceType.CARRIER
        );

        ORDER_STEPS.verifyForOrderHasNewDates(order.getId(), nextDeliveryDate);
//        в кейсах с 4х чп не меняется роут и дата отгрузки в последней миле
//        LOM_ORDER_STEPS.verifyChangeDeliveryDate(order, lomDeliveryDateBefore);
//        LOM_ORDER_STEPS.verifyChangeOrderRoute(lomOrderId, routeBefore);
        ORDER_STEPS.verifyChangeDeliveryDate(order, cpaDeliveryDateBefore);
    }

}
