package ru.yandex.market.logistics.lom.service;

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

@DisplayName("Тесты на обработку заявки CHANGE_LAST_MILE_TO_PICKUP в статусе PROCESSING")
@DatabaseSetup("/service/change_last_mile/to_courier/request/before/setup.xml")
public class ChangeLastMileToCourierRequestProcessingTest extends AbstractContextualTest {

    private static final ChangeOrderRequestPayload PAYLOAD = createChangeOrderRequestPayload(
        101L,
        "1",
        1L
    );

    @Autowired
    private ChangeOrderRequestProcessingService processor;

    @Test
    @DisplayName("Успешное создание заявок для сегментов COURIER и PICKUP")
    @DatabaseSetup("/service/change_last_mile/to_courier/request/before/change_request.xml")
    @ExpectedDatabase(
        value = "/service/change_last_mile/to_courier/request/after/change_request_processing.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/service/change_last_mile/to_courier/request/after/two_segment_requests_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void changeOrderSegmentRequestCreated() {
        processor.processPayload(PAYLOAD);
        queueTaskChecker.assertQueueTasksCreated(
            QueueType.PROCESS_WAYBILL_SEGMENT_CHANGE_LAST_MILE,
            1
        );
    }

    @Test
    @DisplayName("Заявка не в статусе PROCESSING")
    @DatabaseSetup("/service/change_last_mile/to_courier/request/before/wrong_request_status.xml")
    void wrongChangeRequestStatus() {
        processor.processPayload(PAYLOAD);
        queueTaskChecker.assertQueueTaskNotCreated(QueueType.PROCESS_WAYBILL_SEGMENT_CHANGE_LAST_MILE);
    }
}
