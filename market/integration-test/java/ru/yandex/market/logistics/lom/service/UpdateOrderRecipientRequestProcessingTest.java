package ru.yandex.market.logistics.lom.service;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.jobs.model.ChangeOrderRequestPayload;
import ru.yandex.market.logistics.lom.jobs.model.ChangeOrderSegmentRequestPayload;
import ru.yandex.market.logistics.lom.jobs.model.ProcessingResult;
import ru.yandex.market.logistics.lom.jobs.model.ProcessingResultStatus;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.jobs.processor.ChangeOrderRequestProcessingService;

import static ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory.createChangeOrderRequestPayload;
import static ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory.createChangeOrderSegmentRequestPayload;

class UpdateOrderRecipientRequestProcessingTest extends AbstractContextualTest {
    private static final ChangeOrderRequestPayload PAYLOAD = createChangeOrderRequestPayload(
        1L,
        "1",
        1L
    );

    private static final ChangeOrderSegmentRequestPayload SEGMENT_PAYLOAD = createChangeOrderSegmentRequestPayload(
        1L,
        "1",
        1L
    );

    @Autowired
    private ChangeOrderRequestProcessingService processor;

    @Test
    @DisplayName(
        "Успешное создание заявки на обновление данных получателя на одном сегменте на основе RECIPIENT заявки"
    )
    @DatabaseSetup("/controller/order/recipient/before/single_ds_segment.xml")
    @ExpectedDatabase(
        value = "/controller/order/recipient/after/2_segments_requests_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void oneDeliverySegmentProcessing() {
        processor.processPayload(PAYLOAD);

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_WAYBILL_SEGMENT_UPDATE_RECIPIENT,
            SEGMENT_PAYLOAD
        );
    }

    @Test
    @DisplayName("Успешное создание заявки на обновление данных получателя только на последнем DS сегменте")
    @DatabaseSetup("/controller/order/recipient/before/multiple_ds_segments.xml")
    @ExpectedDatabase(
        value = "/controller/order/recipient/after/4_segments_requests_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void multipleDeliverySegmentsProcessing() {
        processor.processPayload(PAYLOAD);

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_WAYBILL_SEGMENT_UPDATE_RECIPIENT,
            SEGMENT_PAYLOAD
        );
    }

    @Test
    @DisplayName("Запрос уже обработан")
    @DatabaseSetup("/controller/order/recipient/before/single_ds_segment.xml")
    @DatabaseSetup(
        value = "/controller/order/recipient/before/request_is_already_processing.xml",
        type = DatabaseOperation.UPDATE
    )
    void requestIsAlreadyProcessing() {
        processor.processPayload(PAYLOAD);

        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @DisplayName("Заказ в неподходящем статусе")
    @DatabaseSetup("/controller/order/recipient/before/single_ds_segment.xml")
    @DatabaseSetup(
        value = "/controller/order/recipient/before/order_in_wrong_status.xml",
        type = DatabaseOperation.UPDATE
    )
    void orderInWrongStatus() {
        ProcessingResult processingResult = processor.processPayload(PAYLOAD);
        softly.assertThat(processingResult.getStatus()).isEqualTo(ProcessingResultStatus.UNPROCESSED);
        softly.assertThat(processingResult.getComment())
            .isEqualTo("Try to change order 1 with status VALIDATION_ERROR");
    }
}
