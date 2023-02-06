package ru.yandex.market.logistics.lom.controller.order.processing;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.logistic.gateway.common.model.fulfillment.CargoType;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Item;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Order;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.UnitId;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.jobs.consumer.UpdateOrderUpdatingRequestConsumer;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.jobs.processor.ProcessOrderReadyToShipService;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;
import ru.yandex.market.logistics.lom.utils.jobs.TaskFactory;
import ru.yandex.market.request.trace.RequestContext;
import ru.yandex.market.request.trace.RequestContextHolder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Производитель задач очереди PROCESS_UPDATE_ORDER_ITEMS_INSTANCES")
class UpdateOrderItemsInstancesProducerTest extends AbstractContextualTest {
    @Autowired
    private ProcessOrderReadyToShipService processOrderReadyToShipService;

    @Autowired
    private UpdateOrderUpdatingRequestConsumer updateOrderUpdatingRequestConsumer;

    @Test
    @DisplayName("Заявка создается, если заказ предоплаченный")
    @DatabaseSetup({
        "/controller/order/update_items_instances/before/prepaid_order.xml",
        "/controller/order/update_items_instances/before/waybill_segment.xml",
        "/controller/order/update_items_instances/before/order_item_without_instances.xml",
    })
    @ExpectedDatabase(
        value = "/controller/order/update_items_instances/after/cors_instances_1_places_1.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void changeOrderRequestWillBeProducedForPrepaidOrder() {
        processOrderReadyToShipService.processOrderFromPartner(1110, 1, order());
        queueTaskChecker.assertQueueTasksCreated(QueueType.CHANGE_ORDER_REQUEST, 2);
    }

    @Test
    @DisplayName("Заявка не создается, если партнёр не поддерживает фичу")
    @DatabaseSetup({
        "/controller/order/update_items_instances/before/order.xml",
        "/controller/order/update_items_instances/before/waybill_segment.xml",
        "/controller/order/update_items_instances/before/order_item_without_instances.xml",
    })
    @DatabaseSetup(
        value = "/controller/order/update_items_instances/before/waybill_segment_update.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/order/update_items_instances/after/cors_instances_0_places_1.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void changeOrderRequestWillNotBeProducedForIncapablePartner() {
        processOrderReadyToShipService.processOrderFromPartner(1110, 1, order());
        queueTaskChecker.assertExactlyOneQueueTaskCreated(QueueType.CHANGE_ORDER_REQUEST);
    }

    @Test
    @DisplayName(
        "Заявка создается и задача производится по 120 чекпоинту, " +
            "если нет заявки на обновление товаров в заказе"
    )
    @DatabaseSetup({
        "/controller/order/update_items_instances/before/order.xml",
        "/controller/order/update_items_instances/before/waybill_segment.xml",
        "/controller/order/update_items_instances/before/order_item_without_instances.xml",
    })
    @ExpectedDatabase(
        value = "/controller/order/update_items_instances/after/cors_instances_1_places_1.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void queueTaskProducedOn120Checkpoint() {
        processOrderReadyToShipService.processOrderFromPartner(1110, 1, order());
        queueTaskChecker.assertQueueTasksCreated(QueueType.CHANGE_ORDER_REQUEST, 2);
    }

    @Test
    @DisplayName("Вторая заявка не создается, т.к. экземпляры не изменились")
    @DatabaseSetup({
        "/controller/order/update_items_instances/before/order.xml",
        "/controller/order/update_items_instances/before/waybill_segment.xml",
        "/controller/order/update_items_instances/before/order_item_without_instances.xml",
    })
    @ExpectedDatabase(
        value = "/controller/order/update_items_instances/after/cors_instances_1_places_2.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void queueTasksProducedOnlyWhenInstancesChange() {
        processOrderReadyToShipService.processOrderFromPartner(1110, 1, order());
        processOrderReadyToShipService.processOrderFromPartner(1110, 1, order());
        queueTaskChecker.assertQueueTasksCreated(QueueType.CHANGE_ORDER_REQUEST, 2);
    }

    @Test
    @DisplayName("Заявка не создается, т.к. указан пустой список экземпляров в ответе от партнера FF-API")
    @DatabaseSetup({
        "/controller/order/update_items_instances/before/order.xml",
        "/controller/order/update_items_instances/before/waybill_segment.xml",
        "/controller/order/update_items_instances/before/order_item_without_instances.xml",
    })
    @ExpectedDatabase(
        value = "/controller/order/update_items_instances/after/cors_instances_0_places_1.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void queueTasksWillNotBeProducedWhenInstancesIsEmptyList() {
        processOrderReadyToShipService.processOrderFromPartner(1110, 1, order(item(List.of())));
        queueTaskChecker.assertExactlyOneQueueTaskCreated(QueueType.CHANGE_ORDER_REQUEST);
    }

    @Test
    @DisplayName("Вторая заявка создается, т.к. экземпляры изменились и было два 120 чп")
    @DatabaseSetup({
        "/controller/order/update_items_instances/before/order.xml",
        "/controller/order/update_items_instances/before/waybill_segment.xml",
        "/controller/order/update_items_instances/before/order_item_without_instances.xml",
    })
    @ExpectedDatabase(
        value = "/controller/order/update_items_instances/after/cors_instances_2_places_2.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void queueTasksProducedOnEach120WhenInstancesChange() {
        processOrderReadyToShipService.processOrderFromPartner(1110, 1, order());
        processOrderReadyToShipService.processOrderFromPartner(1110, 1, order(item(List.of())));
        queueTaskChecker.assertQueueTasksCreated(QueueType.CHANGE_ORDER_REQUEST, 3);
    }

    @Test
    @DisplayName("Задача не производится по 120 чекпоинту, если была заявка на обновление товаров в заказе")
    @DatabaseSetup({
        "/controller/order/update_items_instances/before/order.xml",
        "/controller/order/update_items_instances/before/waybill_segment.xml",
        "/controller/order/update_items_instances/before/order_item_without_instances.xml",
        "/controller/order/update_items_instances/before/order_changed_by_partner_request.xml",
    })
    @ExpectedDatabase(
        value = "/controller/order/update_items_instances/after/cors_instances_1_places_1_changed_by_partner_1.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void queueTaskWillNotBeProducedForOrderWithChangedByPartnerRequestImmediately() {
        processOrderReadyToShipService.processOrderFromPartner(1110, 1, order());
        queueTaskChecker.assertExactlyOneQueueTaskCreated(QueueType.CHANGE_ORDER_REQUEST);
    }

    @Test
    @DisplayName(
        "Задача производится по успешному асинхронному ответу об обновлении товаров в заказе, " +
            "если есть активная заявка на изменение экземпляров товаров"
    )
    @DatabaseSetup({
        "/controller/order/update_items_instances/before/order.xml",
        "/controller/order/update_items_instances/before/waybill_segment.xml",
        "/controller/order/update_items_instances/before/order_item_without_instances.xml",
        "/controller/order/update_items_instances/before/order_changed_by_partner_request.xml",
        "/controller/order/update_items_instances/before/change_order_request_processing.xml",
        "/controller/order/update_items_instances/before/process_waybill_segment_update_order_items.xml",
    })
    void queueTaskProducedOnUpdateOrderItemsAsyncSuccessResponse() {
        callAsyncResponseMethod("/orders/ds/update-items/success");
        processOrderItemsRequestStatusUpdateTask();
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.CHANGE_ORDER_REQUEST,
            PayloadFactory.createChangeOrderRequestPayload(1100, "2", 1)
        );
    }

    @Test
    @DisplayName("Задача не производится по неуспешному асинхронному ответу об обновлении товаров в заказе")
    @DatabaseSetup({
        "/controller/order/update_items_instances/before/order.xml",
        "/controller/order/update_items_instances/before/waybill_segment.xml",
        "/controller/order/update_items_instances/before/order_item_without_instances.xml",
        "/controller/order/update_items_instances/before/order_changed_by_partner_request.xml",
        "/controller/order/update_items_instances/before/change_order_request_processing.xml",
        "/controller/order/update_items_instances/before/process_waybill_segment_update_order_items.xml",
    })
    void queueTaskWillNotBeProducedOnUpdateOrderItemsAsyncErrorResponse() {
        callAsyncResponseMethod("/orders/ds/update-items/error");
        processOrderItemsRequestStatusUpdateTask();
        queueTaskChecker.assertQueueTaskNotCreated(QueueType.CHANGE_ORDER_REQUEST);
    }

    @SneakyThrows
    private void callAsyncResponseMethod(String path) {
        RequestContextHolder.setContext(new RequestContext(REQUEST_ID));
        mockMvc.perform(
            put(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent(
                    "controller/order/update_items_instances/request/update_items.json"
                ))
        )
            .andExpect(status().isOk());
    }

    private void processOrderItemsRequestStatusUpdateTask() {
        updateOrderUpdatingRequestConsumer.execute(
            TaskFactory.createTask(
                QueueType.UPDATE_ORDER_ITEMS_REQUEST_STATUS_UPDATE,
                PayloadFactory.createChangeOrderSegmentRequestPayload(
                    1300,
                    "1"
                )
            )
        );
    }

    @Nonnull
    private Order order() {
        return order(item());
    }

    @Nonnull
    private Order order(Item item) {
        return new Order.OrderBuilder(
            null,
            null,
            List.of(item),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        )
            .build();
    }

    @Nonnull
    private Item item() {
        return item(List.of(Map.of("SN", "SC02DX3V9Q6LD")));
    }

    @Nonnull
    private Item item(List<Map<String, String>> instances) {
        return new Item.ItemBuilder(
            null,
            1,
            BigDecimal.valueOf(200.0),
            null,
            List.of(CargoType.CIS_REQUIRED)
        )
            .setUnitId(new UnitId(null, 1L, "test-item-article"))
            .setInstances(instances)
            .build();
    }
}
