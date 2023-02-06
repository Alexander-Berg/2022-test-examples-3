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

public class UpdateLastMileRequestProcessingTest extends AbstractContextualTest {
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
        "Успешное создание чендж реквеста на обновление последней мили на одном сегменте"
    )
    @DatabaseSetup("/controller/order/lastmile/before/single_ds_segment.xml")
    @ExpectedDatabase(
        value = "/controller/order/lastmile/after/requests_created_for_single_segment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void changeOrderSegmentRequestCreated() {
        processor.processPayload(PAYLOAD);

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_WAYBILL_SEGMENT_UPDATE_LAST_MILE,
            SEGMENT_PAYLOAD
        );
    }

    @Test
    @DisplayName(
        "Успешное создание ченж реквеста на обновление последней мили только на последнем сегменте"
    )
    @DatabaseSetup("/controller/order/lastmile/before/multiple_ds_segments.xml")
    @ExpectedDatabase(
        value = "/controller/order/lastmile/after/requests_created_for_last_segment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void changeOrderSegmentRequestCreatedOnlyForLastDeliverySegment() {
        processor.processPayload(PAYLOAD);

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_WAYBILL_SEGMENT_UPDATE_LAST_MILE,
            SEGMENT_PAYLOAD
        );
    }

    @Test
    @DisplayName("Чендж реквест уже обработан")
    @DatabaseSetup("/controller/order/lastmile/before/single_ds_segment.xml")
    @DatabaseSetup(
        value = "/controller/order/lastmile/before/request_has_already_been_processed.xml",
        type = DatabaseOperation.UPDATE
    )
    void requestHasAlreadyBeenProcessed() {
        processor.processPayload(PAYLOAD);

        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @DisplayName("Чендж реквест не создается если заказ в неподходящем статусе")
    @DatabaseSetup("/controller/order/lastmile/before/single_ds_segment.xml")
    @DatabaseSetup(
        value = "/controller/order/lastmile/before/order_is_in_wrong_status.xml",
        type = DatabaseOperation.UPDATE
    )
    void doNotProcessOrderInWrongStatus() {
        ProcessingResult processingResult = processor.processPayload(PAYLOAD);
        softly.assertThat(processingResult.getStatus()).isEqualTo(ProcessingResultStatus.UNPROCESSED);
        softly.assertThat(processingResult.getComment())
            .isEqualTo("Try to change order 1 with status VALIDATION_ERROR");
    }
}
