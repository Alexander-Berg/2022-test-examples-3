package ru.yandex.market.logistics.lom.controller.order.processing;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.checker.QueueTaskChecker;
import ru.yandex.market.logistics.lom.jobs.model.ChangeOrderRequestPayload;
import ru.yandex.market.logistics.lom.jobs.model.ProcessingResult;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.jobs.processor.ChangeOrderRequestProcessingService;

import static ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory.createChangeOrderRequestPayload;
import static ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory.createChangeOrderSegmentRequestPayload;

@DatabaseSetup({
    "/controller/order/update_courier/before/order.xml",
    "/controller/order/update_courier/before/waybill_segment.xml",
    "/controller/order/update_courier/before/change_order_request_processing.xml",
})
@DisplayName("Обработчик задач очереди PROCESS_UPDATE_COURIER")
class UpdateCourierProcessorTest extends AbstractContextualTest {
    private static final ChangeOrderRequestPayload PAYLOAD = createChangeOrderRequestPayload(1100, "1001");

    @Autowired
    private QueueTaskChecker queueTaskChecker;

    @Autowired
    private ChangeOrderRequestProcessingService changeOrderRequestProcessingService;

    @Test
    @DisplayName("Успешная обработка задачи")
    @ExpectedDatabase(
        value = "/controller/order/update_courier/after/success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/order/update_courier/after/waybill_segments_updated.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successExecution() {
        softly.assertThat(changeOrderRequestProcessingService.processPayload(PAYLOAD))
            .isEqualTo(ProcessingResult.success());

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_WAYBILL_SEGMENT_UPDATE_COURIER,
            createChangeOrderSegmentRequestPayload(1, "1", 1)
        );
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_WAYBILL_SEGMENT_UPDATE_COURIER,
            createChangeOrderSegmentRequestPayload(2, "2", 2)
        );
    }

    @Test
    @DisplayName("Заявка в неподходящем статусе - бизнес-процесс переведется в статус UNPROCESSED")
    @DatabaseSetup(
        value = "/controller/order/update_courier/before/change_order_request_fail.xml",
        type = DatabaseOperation.UPDATE
    )
    void inappropriateChangeOrderRequestStatusTaskWillUnprocessed() {
        softly.assertThat(changeOrderRequestProcessingService.processPayload(PAYLOAD))
            .isEqualTo(ProcessingResult.unprocessed());

        queueTaskChecker.assertQueueTaskNotCreated(QueueType.PROCESS_WAYBILL_SEGMENT_UPDATE_COURIER);
    }

    @Test
    @DisplayName("Заказ в неподходящем статусе - бизнес-процесс переведется в статус UNPROCESSED")
    @DatabaseSetup(
        value = "/controller/order/update_courier/before/order_delivered.xml",
        type = DatabaseOperation.UPDATE
    )
    void inappropriateOrderStatusTaskWillUnprocessed() {
        softly.assertThat(changeOrderRequestProcessingService.processPayload(PAYLOAD))
            .isEqualTo(ProcessingResult.unprocessed("Try to change order 1110 with status DELIVERED"));

        queueTaskChecker.assertQueueTaskNotCreated(QueueType.PROCESS_WAYBILL_SEGMENT_UPDATE_COURIER);
    }

    @Test
    @DisplayName("Успешная обработка задачи для заказа, находящегося в процессе возврата")
    @DatabaseSetup(
        value = "/controller/order/update_courier/before/order_returning.xml",
        type = DatabaseOperation.UPDATE
    )
    @DatabaseSetup(
        value = "/controller/order/update_courier/before/cancellation_order_request.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/controller/order/update_courier/after/success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/order/update_courier/after/waybill_segments_updated.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successExecutionWhileReturning() {
        softly.assertThat(changeOrderRequestProcessingService.processPayload(PAYLOAD))
            .isEqualTo(ProcessingResult.success());

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_WAYBILL_SEGMENT_UPDATE_COURIER,
            createChangeOrderSegmentRequestPayload(1, "1", 1)
        );
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_WAYBILL_SEGMENT_UPDATE_COURIER,
            createChangeOrderSegmentRequestPayload(2, "2", 2)
        );
    }
}
