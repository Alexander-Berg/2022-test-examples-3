package ru.yandex.market.logistics.lom.controller.order.processing;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.jobs.consumer.ChangeOrderRequestConsumer;
import ru.yandex.market.logistics.lom.jobs.model.ChangeOrderRequestPayload;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.service.waybill.TransferCodesService;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;
import ru.yandex.market.logistics.lom.utils.jobs.TaskFactory;
import ru.yandex.money.common.dbqueue.api.Task;

import static org.mockito.Mockito.when;

@DisplayName("Обработчик задач очереди PROCESS_UPDATE_TRANSFER_CODES")
class UpdateTransferCodesProcessorTest extends AbstractContextualTest {
    private static final ChangeOrderRequestPayload PAYLOAD = PayloadFactory.createChangeOrderRequestPayload(
        1100,
        "1001"
    );

    private static final Task<ChangeOrderRequestPayload> TASK =
        TaskFactory.createTask(QueueType.CHANGE_ORDER_REQUEST, PAYLOAD);

    private static final ChangeOrderRequestPayload PAYLOAD_CONSECUTIVE = PayloadFactory.createChangeOrderRequestPayload(
        1101,
        "1004"
    );

    private static final Task<ChangeOrderRequestPayload> TASK_CONSECUTIVE =
        TaskFactory.createTask(QueueType.CHANGE_ORDER_REQUEST, PAYLOAD_CONSECUTIVE);

    @Autowired
    private ChangeOrderRequestConsumer changeOrderRequestConsumer;

    @Autowired
    private TransferCodesService transferCodesService;

    @BeforeEach
    void setUp() {
        when(transferCodesService.generateCode()).thenReturn("54321");
    }

    @Test
    @DisplayName("Успешная обработка задачи последнего сегмента")
    @DatabaseSetup({
        "/controller/order/update_transfer_codes/before/order.xml",
        "/controller/order/update_transfer_codes/before/waybill_segment.xml",
        "/controller/order/update_transfer_codes/before/order_item_with_instances.xml",
        "/controller/order/update_transfer_codes/before/change_order_request_processing.xml",
        "/controller/order/update_transfer_codes/before/process_update_order_items_instances.xml"
    })
    @ExpectedDatabase(
        value = "/controller/order/update_transfer_codes/after/success_last.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successProcessingLast() {
        changeOrderRequestConsumer.execute(TASK);
    }

    @Test
    @DisplayName("Успешная обработка задачи сегманта со следующим сегментом")
    @DatabaseSetup({
        "/controller/order/update_transfer_codes/before/order.xml",
        "/controller/order/update_transfer_codes/before/waybill_segment.xml",
        "/controller/order/update_transfer_codes/before/order_item_with_instances.xml",
        "/controller/order/update_transfer_codes/before/change_order_request_processing.xml",
        "/controller/order/update_transfer_codes/before/process_update_order_items_instances.xml"
    })
    @ExpectedDatabase(
        value = "/controller/order/update_transfer_codes/after/success_consecutive.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successProcessingConsecutive() {
        changeOrderRequestConsumer.execute(TASK_CONSECUTIVE);
    }

    @Test
    @DisplayName("Заявка в неподходящем статусе - бизнес-процесс переведется в статус UNPROCESSED")
    @DatabaseSetup({
        "/controller/order/update_transfer_codes/before/order.xml",
        "/controller/order/update_transfer_codes/before/waybill_segment.xml",
        "/controller/order/update_transfer_codes/before/order_item_with_instances.xml",
        "/controller/order/update_transfer_codes/before/change_order_request_fail.xml",
        "/controller/order/update_transfer_codes/before/process_update_order_items_instances.xml"
    })
    @ExpectedDatabase(
        value = "/controller/order/update_transfer_codes/after/unprocessed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void inappropriateChangeOrderRequestStatusTaskWillUnprocessed() {
        changeOrderRequestConsumer.execute(TASK);
    }

    @Test
    @DisplayName("Заказ в неподходящем статусе - бизнес-процесс переведется в статус UNPROCESSED")
    @DatabaseSetup({
        "/controller/order/update_transfer_codes/before/order_delivered.xml",
        "/controller/order/update_transfer_codes/before/waybill_segment.xml",
        "/controller/order/update_transfer_codes/before/change_order_request_fail.xml",
        "/controller/order/update_transfer_codes/before/process_update_order_items_instances.xml"
    })
    @ExpectedDatabase(
        value = "/controller/order/update_transfer_codes/after/unprocessed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void inappropriateOrderStatusTaskWillUnprocessed() {
        changeOrderRequestConsumer.execute(TASK);
    }
}
