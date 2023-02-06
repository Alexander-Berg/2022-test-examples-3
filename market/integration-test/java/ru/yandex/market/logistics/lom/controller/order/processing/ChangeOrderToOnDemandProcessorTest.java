package ru.yandex.market.logistics.lom.controller.order.processing;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.checker.QueueTaskChecker;
import ru.yandex.market.logistics.lom.jobs.model.ChangeOrderRequestPayload;
import ru.yandex.market.logistics.lom.jobs.model.ChangeOrderSegmentRequestPayload;
import ru.yandex.market.logistics.lom.jobs.model.ProcessingResult;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.jobs.processor.ChangeOrderRequestProcessingService;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;

@DisplayName("Создание заявки на изменение сегмента Яндекс.Go, для преобразования заказа в заказ с доставкой по клику")
@DatabaseSetup("/controller/order/change_order_to_on_demand/setup.xml")
class ChangeOrderToOnDemandProcessorTest extends AbstractContextualTest {

    private static final ChangeOrderRequestPayload ORDER_REQUEST_PAYLOAD =
        PayloadFactory.createChangeOrderRequestPayload(1, "1", 1);

    private static final ChangeOrderSegmentRequestPayload SEGMENT_REQUEST_PAYLOAD =
        PayloadFactory.createChangeOrderSegmentRequestPayload(1L, "1", 1L);

    @Autowired
    private ChangeOrderRequestProcessingService processor;

    @Autowired
    private QueueTaskChecker queueTaskChecker;

    @Test
    @DisplayName(
        "Успешное создание заявки для сегмента Яндекс.Go, в котором есть оба тега - DEFERRED_COURIER и ON_DEMAND"
    )
    @DatabaseSetup("/controller/order/change_order_to_on_demand/order/before/segment_with_both_tags.xml")
    @ExpectedDatabase(
        value = "/controller/order/change_order_to_on_demand/order/after/success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testProcessPayloadForOrderWithSegmentWithBothTagsSuccess() {
        processSuccess();
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_WAYBILL_SEGMENT_CHANGE_ORDER_TO_ON_DEMAND,
            SEGMENT_REQUEST_PAYLOAD
        );
    }

    @Test
    @DisplayName(
        "Заявка на изменение сегмента не должна создаваться, если заявка на изменение заказа не в статусе PROCESSING"
    )
    @DatabaseSetup(
        "/controller/order/change_order_to_on_demand/order/before/change_order_request_not_in_processing.xml"
    )
    @ExpectedDatabase(
        value = "/controller/order/change_order_to_on_demand/order/after/unprocessed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testProcessPayloadForChangeOrderRequestNotInProcessingStatusUnprocessed() {
        ProcessingResult processingResult = processor.processPayload(ORDER_REQUEST_PAYLOAD);

        softly.assertThat(processingResult)
            .as("Asserting that the processing result is unprocessed")
            .isEqualTo(ProcessingResult.unprocessed());

        queueTaskChecker.assertNoQueueTasksCreated();
    }

    private void processSuccess() {
        ProcessingResult processingResult = processor.processPayload(ORDER_REQUEST_PAYLOAD);

        softly.assertThat(processingResult)
            .as("Asserting that the processing result is success")
            .isEqualTo(ProcessingResult.success());
    }
}
