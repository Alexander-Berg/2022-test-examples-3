package ru.yandex.market.delivery.deliveryintegrationtests.wms.step.api;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.qameta.allure.Step;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.market.delivery.deliveryintegrationtests.tool.DateUtil;
import ru.yandex.market.delivery.deliveryintegrationtests.tool.FileUtil;
import ru.yandex.market.delivery.deliveryintegrationtests.tool.Retrier;
import ru.yandex.market.delivery.deliveryintegrationtests.tool.UniqueId;
import ru.yandex.market.delivery.deliveryintegrationtests.tool.XmlComparator;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.ApiClient;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.DatacreatorClient;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.ServiceBus;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Item;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Order;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.OrderStatus;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.SchedulerJob;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.ApiOrderStatus;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.datacreator.DatacreatorSteps;

public class OrderSteps {

    private static final ApiClient apiClient = new ApiClient();
    private static final ServiceBus serviceBus = new ServiceBus();
    private static final DatacreatorClient dataCreator = new DatacreatorClient();
    private static final Logger log = LoggerFactory.getLogger(OrderSteps.class);

    @Step("Создание заказа")
    private Order createOrder(long yandexId, String shipmentDate) {
        Item item = Item.builder()
                .sku("AUTO_FOR_ORDER")
                .vendorId(1559)
                .article("AUTO_FOR_ORDER")
                .quantity(1)
                .build();

        return createOrder(yandexId, List.of(item), shipmentDate);
    }

    @Step("Создание заказа")
    public Order createOrder(long yandexId, List<Item> items, String shipmentDate) {
            return serviceBus.createOrder(
                    yandexId,
                    items,
                    shipmentDate
            );
    }

    public Order createTodayOrder(Item item) {
        return createTodayOrder(List.of(item));
    }

    @Step("Создание заказа на сегодняшний операционный день")
    public Order createTodayOrder(List<Item> items) {
        return createOrder(
                UniqueId.get(),
                items,
                getCurrentOperationalDate()
        );
    }

    @Step("Получаем текущую операционнцю дату")
    public String getCurrentOperationalDate() {
        return DateUtil.fromDate(dataCreator.getCurrentOperationalDate());
    }

    @Step("Отмена заказа {order.yandexId}, {order.fulfillmentId}")
    public void cancelOrder(Order order) {
        serviceBus.cancelOrder(order);
    }

    @Step("Получаем статус заказа")
    public ApiOrderStatus getOrderStatus(Order order) {
        return ApiOrderStatus.get(apiClient.getOrderStatus(order)
                .extract()
                .xmlPath()
                .getInt("root.response.orderStatusHistories.orderStatusHistory.history.orderStatus.statusCode")
        );
    }

    @Step("Получаем детали заказа")
    public ValidatableResponse getOrder(Order order) {
            return serviceBus.getOrder(order.getYandexId(), order.getFulfillmentId());
    }

    @Step("Проверяем, что статус заказа {order.fulfillmentId} = {status}")
    public void verifyOrderStatus(Order order, ApiOrderStatus status) {
        log.info("Verifying order {} status is {}", order.getFulfillmentId(), status);

        Assertions.assertEquals(status, getOrderStatus(order), "Order status is wrong:");
    }

    @Step("Проверяем заказ")
    public void verifyOrder(Order order, String expectedResult) {
        log.info("Verifying order {} to expected {}", order.getFulfillmentId(), expectedResult);

        ValidatableResponse response = ApiSteps.Order().getOrder(order);
        new XmlComparator().assertXmlValuesAreEqual(response.extract().body().asString(),
                FileUtil.bodyStringFromFile(expectedResult, order.getYandexId(), order.getFulfillmentId()));

    }

    @Step("Проверяем fulfillmentId существующего заказа")
    public void verifyFulfillmentIdOfExistingOrder(Order order) {
        log.info("Verifying fulfillmentId of order {}", order.getFulfillmentId());

        Assertions.assertEquals(createOrder(order.getYandexId(), DateUtil.tomorrowDateTime())
                .getFulfillmentId(), order.getFulfillmentId());

    }

    @Step("Запускаем джобу разделения заказ по зданиям")
    public void startBuildingMarkingJob() { apiClient.executeSchedulerJob(SchedulerJob.BUILDING_MARKING_JOB); }

    @Step("Запускаем джобу CalculateOrdersStatus")
    public void startCalculateOrdersStatusJob() { apiClient.executeSchedulerJob(SchedulerJob.CALCULATE_ORDER_STATUS); }

    @Step("Запускаем джобу RemoveItemCancelOrder")
    public void startRemoveItemCancelOrderJob() { apiClient.executeSchedulerJob(SchedulerJob.REMOVE_ITEM_CANCEL_ORDER); }

    @Step("Проверяем историю статусов заказа {orderId}: last = {lastStatus}, secondLast = {secondLastStatus}")
    public void checkOrderStatusHistory(String orderId, OrderStatus lastStatus, OrderStatus secondLastStatus) {
        List<String> historyStatuses = DatacreatorSteps.Order().getOrderStatusHistory(orderId);
        String lastHistoryStatus = historyStatuses.get(historyStatuses.size() - 1);
        String secondLastHistoryStatus = historyStatuses.get(historyStatuses.size() - 2);

        Assertions.assertEquals(lastStatus.toString(), lastHistoryStatus,
                String.format("Order %s status history is wrong:", orderId));

        Assertions.assertEquals(secondLastStatus.toString(), secondLastHistoryStatus,
                String.format("Order %s status history is wrong:", orderId));
    }

    @Step("Проверяем, что заказ {orderId} был в статусе {status}")
    public void checkOrderWasInStatus(String orderId, OrderStatus status) {
        Retrier.retry(() -> {
            List<String> historyStatuses = DatacreatorSteps.Order().getOrderStatusHistory(orderId);
            for (String historyStatus : historyStatuses) {
                if (historyStatus.equals(status.toString())) {
                    return;
                }
            }
            Assertions.fail(String.format("Order %s was not in status %s", orderId, status.getState()));
        }, Retrier.RETRIES_TINY, Retrier.TIMEOUT_SMALL, TimeUnit.SECONDS);
    }
}
