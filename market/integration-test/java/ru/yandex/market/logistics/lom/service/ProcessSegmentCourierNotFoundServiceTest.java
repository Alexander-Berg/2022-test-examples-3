package ru.yandex.market.logistics.lom.service;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.jobs.processor.ProcessSegmentCourierNotFoundService;

import static ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory.createOrderCancellationRequestIdPayload;
import static ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory.createSegmentCancellationRequestIdPayload;
import static ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory.createWaybillSegmentPayload;

@DatabaseSetup("/service/process_segment_courier_not_found/before/setup.xml")
@DisplayName("Процессинг статуса от СД: курьер не найден")
public class ProcessSegmentCourierNotFoundServiceTest extends AbstractExternalServiceTest {

    @Autowired
    private ProcessSegmentCourierNotFoundService processSegmentCourierNotFoundService;

    @Test
    @DisplayName("Дропшип с заявкой на отмену")
    @ExpectedDatabase(
        value = "/service/process_segment_courier_not_found/after/dropship.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateOrderDropship() {
        processSegmentCourierNotFoundService.processPayload(createWaybillSegmentPayload(1, 2, "2", 2));

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.UPDATE_ORDER_CANCELLATION_REQUEST,
            createSegmentCancellationRequestIdPayload(2L, "1", 1L)
        );
    }

    @Test
    @DisplayName("DaaS с заявкой на отмену")
    @ExpectedDatabase(
        value = "/service/process_segment_courier_not_found/after/daas.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateOrderDaas() {
        processSegmentCourierNotFoundService.processPayload(createWaybillSegmentPayload(3, 5, "2", 2));

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.UPDATE_ORDER_CANCELLATION_REQUEST,
            createSegmentCancellationRequestIdPayload(3L, "1", 1L)
        );
    }

    @Test
    @DisplayName("Дропшип без заявки на отмену, заявка создается")
    @ExpectedDatabase(
        value = "/service/process_segment_courier_not_found/after/dropship_created_cancellation_request.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateOrderDropshipShipped() {
        processSegmentCourierNotFoundService.processPayload(createWaybillSegmentPayload(2, 4, "2", 2));

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.CREATE_SEGMENT_CANCELLATION_REQUESTS,
            createOrderCancellationRequestIdPayload(1L, "1", 1L)
        );
    }

    @Test
    @DisplayName("Дропшип без заявки на отмену, статус лежит в истории, заявка создается")
    @ExpectedDatabase(
        value = "/service/process_segment_courier_not_found/after/cancellation_request_from_status_history.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateOrderDropshipShippedStatusInHistory() {
        processSegmentCourierNotFoundService.processPayload(createWaybillSegmentPayload(4, 6, "2", 2));

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.CREATE_SEGMENT_CANCELLATION_REQUESTS,
            createOrderCancellationRequestIdPayload(1L, "1", 1L)
        );
    }
}
