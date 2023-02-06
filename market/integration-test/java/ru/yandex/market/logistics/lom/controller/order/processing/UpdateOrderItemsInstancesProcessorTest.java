package ru.yandex.market.logistics.lom.controller.order.processing;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.jobs.consumer.ChangeOrderRequestConsumer;
import ru.yandex.market.logistics.lom.jobs.model.ChangeOrderRequestPayload;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;
import ru.yandex.market.logistics.lom.utils.jobs.TaskFactory;
import ru.yandex.money.common.dbqueue.api.Task;

@DisplayName("Обработчик задач очереди PROCESS_UPDATE_ORDER_ITEMS_INSTANCES")
class UpdateOrderItemsInstancesProcessorTest extends AbstractContextualTest {
    private static final ChangeOrderRequestPayload PAYLOAD = PayloadFactory.createChangeOrderRequestPayload(
        1100,
        "1001"
    );

    private static final Task<ChangeOrderRequestPayload> TASK =
        TaskFactory.createTask(QueueType.CHANGE_ORDER_REQUEST, PAYLOAD);

    @Autowired
    private ChangeOrderRequestConsumer changeOrderRequestConsumer;

    @Test
    @DisplayName("Успешная обработка задачи")
    @DatabaseSetup({
        "/controller/order/update_items_instances/before/order.xml",
        "/controller/order/update_items_instances/before/waybill_segment.xml",
        "/controller/order/update_items_instances/before/order_item_with_instances.xml",
        "/controller/order/update_items_instances/before/change_order_request_processing.xml",
        "/controller/order/update_items_instances/before/process_update_order_items_instances.xml"
    })
    @ExpectedDatabase(
        value = "/controller/order/update_items_instances/after/success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successProcessing() {
        changeOrderRequestConsumer.execute(TASK);
    }

    @Test
    @DisplayName("Заявка в неподходящем статусе - бизнес-процесс переведется в статус UNPROCESSED")
    @DatabaseSetup({
        "/controller/order/update_items_instances/before/order.xml",
        "/controller/order/update_items_instances/before/waybill_segment.xml",
        "/controller/order/update_items_instances/before/order_item_with_instances.xml",
        "/controller/order/update_items_instances/before/change_order_request_fail.xml",
        "/controller/order/update_items_instances/before/process_update_order_items_instances.xml"
    })
    @ExpectedDatabase(
        value = "/controller/order/update_items_instances/after/unprocessed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void inappropriateChangeOrderRequestStatusTaskWillUnprocessed() {
        changeOrderRequestConsumer.execute(TASK);
    }

    @Test
    @DisplayName("Заказ в неподходящем статусе - бизнес-процесс переведется в статус UNPROCESSED")
    @DatabaseSetup({
        "/controller/order/update_items_instances/before/order_delivered.xml",
        "/controller/order/update_items_instances/before/waybill_segment.xml",
        "/controller/order/update_items_instances/before/change_order_request_fail.xml",
        "/controller/order/update_items_instances/before/process_update_order_items_instances.xml"
    })
    @ExpectedDatabase(
        value = "/controller/order/update_items_instances/after/unprocessed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void inappropriateOrderStatusTaskWillUnprocessed() {
        changeOrderRequestConsumer.execute(TASK);
    }

    @Test
    @DisplayName(
        "Смена последней мили на курьера - бизнес-процесс переведется в статус WAITING_FOR_PROCESSING_AVAILABILITY"
    )
    @DatabaseSetup({
        "/controller/order/update_items_instances/before/order.xml",
        "/controller/order/update_items_instances/before/waybill_segment_last_mile_changed_to_courier.xml",
        "/controller/order/update_items_instances/before/order_item_with_instances.xml",
        "/controller/order/update_items_instances/before/change_order_request_change_last_mile_to_courier.xml",
        "/controller/order/update_items_instances/before/process_update_order_items_instances.xml"
    })
    @ExpectedDatabase(
        value = "/controller/order/update_items_instances/after/processing_change_last_mile_to_courier.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void lastMileChangeToCourierProcessing() {
        changeOrderRequestConsumer.execute(TASK);
    }

    @Test
    @DisplayName(
        "Смена последней мили на ПВЗ - бизнес-процесс переведется в статус WAITING_FOR_PROCESSING_AVAILABILITY"
    )
    @DatabaseSetup({
        "/controller/order/update_items_instances/before/order.xml",
        "/controller/order/update_items_instances/before/waybill_segment_last_mile_changed_to_pickup.xml",
        "/controller/order/update_items_instances/before/order_item_with_instances.xml",
        "/controller/order/update_items_instances/before/change_order_request_change_last_mile_to_pickup.xml",
        "/controller/order/update_items_instances/before/process_update_order_items_instances.xml"
    })
    @ExpectedDatabase(
        value = "/controller/order/update_items_instances/after/waiting_change_last_mile_to_pickup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void lastMileChangeToPickupWaitingForProcessAvailability() {
        changeOrderRequestConsumer.execute(TASK);
    }
}
