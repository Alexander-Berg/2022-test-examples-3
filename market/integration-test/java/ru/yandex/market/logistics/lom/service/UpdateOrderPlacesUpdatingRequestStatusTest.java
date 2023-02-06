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
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.jobs.processor.UpdateOrderPlacesUpdatingRequestProcessor;

@DatabaseSetup("/service/update_order_places_status_update/before/prepare.xml")
public class UpdateOrderPlacesUpdatingRequestStatusTest extends AbstractContextualTest {

    @Autowired
    private UpdateOrderPlacesUpdatingRequestProcessor processor;

    @Test
    @DisplayName("Перевод сегмента в PROCESSING (статус заявки не меняется)")
    @DatabaseSetup("/service/update_order_places_status_update/before/segment_requests_not_finished.xml")
    @ExpectedDatabase(
        value = "/service/update_order_places_status_update/after/still_processing.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void segmentProcessing() {
        softly.assertThat(processor.processPayload(new ChangeOrderSegmentRequestPayload(REQUEST_ID, 1001L)))
            .isEqualTo(ProcessingResult.success());
    }

    @Test
    @DisplayName("Успешное обновление сегмента, есть еще сегменты в обработке")
    @DatabaseSetup("/service/update_order_places_status_update/before/segment_requests_not_finished.xml")
    @ExpectedDatabase(
        value = "/service/update_order_places_status_update/after/still_processing.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void segmentSuccess() {
        softly.assertThat(processor.processPayload(new ChangeOrderSegmentRequestPayload(REQUEST_ID, 1002L)))
            .isEqualTo(ProcessingResult.success());
    }

    @Test
    @DisplayName("Ошибка при обновлении сегмента")
    @DatabaseSetup("/service/update_order_places_status_update/before/segment_requests_fail.xml")
    @ExpectedDatabase(
        value = "/service/update_order_places_status_update/after/error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void segmentError() {
        softly.assertThat(processor.processPayload(new ChangeOrderSegmentRequestPayload(REQUEST_ID, 1001L)))
            .isEqualTo(ProcessingResult.success());
    }

    @Test
    @DisplayName("Успешное обновление последнего сегмента, нет ожидающих обработки заявок на обновление коробок")
    @DatabaseSetup("/service/update_order_places_status_update/before/segment_requests_finished.xml")
    @ExpectedDatabase(
        value = "/service/update_order_places_status_update/after/success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void lastSegmentSuccessNoMoreRequests() {
        softly.assertThat(processor.processPayload(new ChangeOrderSegmentRequestPayload(REQUEST_ID, 1002L)))
            .isEqualTo(ProcessingResult.success());
    }

    @Test
    @DisplayName("Успешное обновление последнего сегмента, есть ожидающая обработки заявка на обновление коробок")
    @DatabaseSetup("/service/update_order_places_status_update/before/segment_requests_finished.xml")
    @DatabaseSetup(
        value = "/service/update_order_places_status_update/before/awaiting_request.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/service/update_order_places_status_update/after/success_multiple_requests.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void lastSegmentSuccessRequestAwaiting() {
        softly.assertThat(processor.processPayload(new ChangeOrderSegmentRequestPayload(REQUEST_ID, 1002L)))
            .isEqualTo(ProcessingResult.success());

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.CHANGE_ORDER_REQUEST,
            new ChangeOrderRequestPayload(REQUEST_ID + "/1", 101L)
        );
    }
}
