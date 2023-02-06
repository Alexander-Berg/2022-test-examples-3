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
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.jobs.processor.ChangeOrderRequestProcessingService;

import static ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory.createChangeOrderRequestPayload;

@DatabaseSetup("/service/change_last_mile/to_pickup/request/before/setup.xml")
class ChangeLastMileToPickupRequestProcessingTest extends AbstractContextualTest {
    private static final ChangeOrderRequestPayload PAYLOAD = createChangeOrderRequestPayload(
        101L,
        "1",
        1L
    );

    @Autowired
    private ChangeOrderRequestProcessingService processor;

    @Test
    @DisplayName("Успешное создание заявок на двух сегментах MOVEMENT и PICKUP")
    @DatabaseSetup("/service/change_last_mile/to_pickup/request/before/change_request.xml")
    @ExpectedDatabase(
        value = "/service/change_last_mile/to_pickup/request/after/change_request_processing.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/service/change_last_mile/to_pickup/request/after/two_segment_requests_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void changeOrderSegmentRequestsCreated() {
        processor.processPayload(PAYLOAD);
        queueTaskChecker.assertQueueTasksCreated(QueueType.PROCESS_WAYBILL_SEGMENT_CHANGE_LAST_MILE, 1);
    }

    @Test
    @DisplayName("Заявка не в статусе PROCESSING")
    @DatabaseSetup("/service/change_last_mile/to_pickup/request/before/wrong_request_status.xml")
    void wrongChangeRequestStatus() {
        processor.processPayload(PAYLOAD);
        queueTaskChecker.assertQueueTaskNotCreated(QueueType.PROCESS_WAYBILL_SEGMENT_CHANGE_LAST_MILE);
    }

    @Test
    @DisplayName("Сегменты в заявке неверного типа")
    @DatabaseSetup("/service/change_last_mile/to_pickup/request/before/change_request.xml")
    @DatabaseSetup(
        value = "/service/change_last_mile/to_pickup/request/before/wrong_segment_types.xml",
        type = DatabaseOperation.UPDATE
    )
    void wrongSegmentTypes() {
        softly.assertThatThrownBy(() -> processor.processPayload(PAYLOAD));
        queueTaskChecker.assertQueueTaskNotCreated(QueueType.PROCESS_WAYBILL_SEGMENT_CHANGE_LAST_MILE);
    }
}
