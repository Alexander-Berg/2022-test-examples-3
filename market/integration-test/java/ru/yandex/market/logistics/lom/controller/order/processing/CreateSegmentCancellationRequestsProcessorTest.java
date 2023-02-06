package ru.yandex.market.logistics.lom.controller.order.processing;

import java.time.Instant;
import java.time.ZoneOffset;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.entity.enums.ApiType;
import ru.yandex.market.logistics.lom.jobs.model.CreateOrderErrorPayload;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.jobs.processor.CreateSegmentCancellationRequestsProcessor;
import ru.yandex.market.logistics.lom.jobs.processor.order.processing.ProcessCreateOrderAsyncErrorResultService;
import ru.yandex.market.logistics.lom.model.async.CreateOrderErrorDto;
import ru.yandex.market.logistics.lom.model.async.CreateOrderSuccessDto;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;

import static ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory.createSegmentCancellationRequestIdPayload;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Создание заявок на отмену на сегментах вейбилла")
class CreateSegmentCancellationRequestsProcessorTest extends AbstractContextualTest {

    @Autowired
    private ProcessCreateOrderAsyncErrorResultService processCreateOrderAsyncErrorResultService;
    @Autowired
    private CreateSegmentCancellationRequestsProcessor processor;

    @Test
    @DisplayName("Успешное создание заявок на отмену одного сегмента")
    @DatabaseSetup("/controller/order/cancel/before/cancellation_request_processing.xml")
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/cancellation_request_processing.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void oneSegmentProcessing() {
        processor.processPayload(PayloadFactory.createOrderCancellationRequestIdPayload(1L, 1L));

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_WAYBILL_SEGMENT_CANCEL,
            createSegmentCancellationRequestIdPayload(1L, "1", 1L)
        );
    }

    @Test
    @DisplayName("Заявка на отмену сегмента без ApiType не создается")
    @DatabaseSetup("/controller/order/cancel/before/dbs_to_pickup_point.xml")
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/cancellation_request_processing.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void cancellationSegmentRequestForSegmentWithNoApiTypeWillNotBeCreated() {
        processor.processPayload(PayloadFactory.createOrderCancellationRequestIdPayload(1L, 1L));

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_WAYBILL_SEGMENT_CANCEL,
            createSegmentCancellationRequestIdPayload(1L, "1", 1L)
        );
    }

    @Test
    @DisplayName("Дублирование создания заявок на отмену сегментов")
    @DatabaseSetup("/controller/order/cancel/before/cancellation_request_twice_start.xml")
    void oneSegmentProcessingCreationTwice() {
        processor.processPayload(PayloadFactory.createOrderCancellationRequestIdPayload(1L, 1L));

        queueTaskChecker.assertQueueTaskNotCreated(
            QueueType.PROCESS_WAYBILL_SEGMENT_CANCEL,
            createSegmentCancellationRequestIdPayload(1L, "1", 1L)
        );
    }

    @Test
    @DisplayName("Создание сегментов при наличии отмен других заказов у партнера")
    @DatabaseSetup("/controller/order/cancel/before/cancellation_request_with_segments_with_another_cancel.xml")
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/cancellation_request_processing_with_another_cancel.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void processCreationWithSuccessAnotherCancel() {
        processor.processPayload(PayloadFactory.createOrderCancellationRequestIdPayload(1L, 1L));

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_WAYBILL_SEGMENT_CANCEL,
            createSegmentCancellationRequestIdPayload(1L, "1", 1L)
        );
    }

    @Test
    @DisplayName("Создание заявок на отмену сегментов при наличии возвратного")
    @DatabaseSetup("/controller/order/cancel/before/cancellation_request_with_return_segment.xml")
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/cancellation_request_processing_return_segment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void processCreationWithReturnSegment() {
        processor.processPayload(PayloadFactory.createOrderCancellationRequestIdPayload(1L, 1L));

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_WAYBILL_SEGMENT_CANCEL,
            createSegmentCancellationRequestIdPayload(1L, "1", 1L)
        );
    }

    @Test
    @DisplayName("Успешное создание заявок на отмену сегментов с чекпоинтами отмены")
    @DatabaseSetup("/controller/order/cancel/before/cancellation_request_processing_with_cancellation_checkpoint.xml")
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/cancellation_request_processing_with_cancellation_checkpoint.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void segmentsWithCancellation() {
        processor.processPayload(PayloadFactory.createOrderCancellationRequestIdPayload(1L, 1L));
    }

    @Test
    @DisplayName("Успешное создание заявок на отмену двух сегментов")
    @DatabaseSetup("/controller/order/cancel/before/cancellation_request_processing_two_segments.xml")
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/cancellation_request_processing_two_segments.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void twoSegmentProcessing() {
        processor.processPayload(PayloadFactory.createOrderCancellationRequestIdPayload(1L, 1L));
    }

    @Test
    @DisplayName("Успешное создание заявок на отмену трех сегментов, из них два сегмента СД")
    @DatabaseSetup("/controller/order/cancel/before/cancellation_request_processing_three_segments.xml")
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/cancellation_request_processing_three_segments.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void threeSegmentProcessing() {
        processor.processPayload(PayloadFactory.createOrderCancellationRequestIdPayload(1L, 1L));
    }

    @Test
    @DisplayName("Успешное создание заявок на отмену заказа с четырьмя сегментами")
    @DatabaseSetup("/controller/order/cancel/before/cancellation_request_processing_four_segments.xml")
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/cancellation_request_processing_four_segments.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void fiveSegmentProcessing() {
        processor.processPayload(PayloadFactory.createOrderCancellationRequestIdPayload(1L, 1L));
    }

    @Test
    @DisplayName("Успешное создание заявок на отмену заказа через с двумя сегментами с одним и тем же партнёром")
    @DatabaseSetup("/controller/order/cancel/before/cancellation_request_processing_two_segments_with_same_partner.xml")
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/cancellation_request_processing_two_segments_with_same_partner.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void orderTwoSegmentsWithSamePartnerProcessing() {
        processor.processPayload(PayloadFactory.createOrderCancellationRequestIdPayload(1L, 1L));
    }

    @Test
    @DisplayName("Успешное создание заявок на отмену двух сегментов, когда один уже отменен")
    @DatabaseSetup("/controller/order/cancel/before/cancellation_request_processing_two_segments.xml")
    @DatabaseSetup(
        value = "/controller/order/cancel/before/cancellation_request_one_segment_success.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/cancellation_request_processing_two_segments_one_success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void twoSegmentProcessingWithOneCancelled() {
        processor.processPayload(PayloadFactory.createOrderCancellationRequestIdPayload(1L, 1L));
    }

    @Test
    @DisplayName("Создание заявки, когда сегмент еще не создан у партнера")
    @DatabaseSetup("/controller/order/cancel/before/cancellation_request_processing_not_created.xml")
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/cancellation_request_processing_not_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void segmentNotCreated() {
        processor.processPayload(PayloadFactory.createOrderCancellationRequestIdPayload(1L, 1L));
    }

    @Test
    @DisplayName("Создание заявки, когда сегмент не создан у партнера, ошибка в заказе")
    @DatabaseSetup("/controller/order/cancel/before/cancellation_request_processing_error.xml")
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/cancellation_request_processing_error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void segmentNotCreatedErrorProcessing() {
        clock.setFixed(Instant.parse("2021-10-01T12:00:00Z"), ZoneOffset.UTC);
        processor.processPayload(PayloadFactory.createOrderCancellationRequestIdPayload(1L, 1L));
    }

    @Test
    @DisplayName("Дублирование запуска задачи, проверка отсутствия задач по сегментам")
    @DatabaseSetup("/controller/order/cancel/before/cancellation_request_with_segments.xml")
    @ExpectedDatabase(
        value = "/controller/order/cancel/before/cancellation_request_with_segments.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void cancellationSegmentRequestExists() {
        processor.processPayload(PayloadFactory.createOrderCancellationRequestIdPayload(1L, 1L));
    }

    @Test
    @DisplayName("Финальная обработка ошибки")
    @DatabaseSetup("/controller/order/cancel/before/cancellation_request_processing.xml")
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/cancellation_request_tech_fail.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void processFinalFailure() {
        processor.processFinalFailure(
            PayloadFactory.createOrderCancellationRequestIdPayload(1L, 1L),
            new Exception("cancellation exception")
        );
    }

    @Test
    @DisplayName("Финальная обработка ошибки, если заказа в статусе FINISHED")
    @DatabaseSetup("/controller/order/cancel/before/cancellation_request_processing.xml")
    @DatabaseSetup(
        value = "/controller/order/cancel/before/cancellation_request_order_finished.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/cancellation_request_fail.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void processFinalFailureFinishedOrder() {
        processor.processFinalFailure(
            PayloadFactory.createOrderCancellationRequestIdPayload(1L, 1L),
            new Exception("cancellation exception")
        );
    }

    @Test
    @DisplayName("Отмена заказа в СД, заказ в PROCESSING_ERROR")
    @DatabaseSetup("/controller/order/cancel/before/cancellation_request_processing.xml")
    @DatabaseSetup(
        value = "/controller/order/cancel/before/order_processing_error.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/cancellation_request_processing_not_started.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deliveryProcessingError() {
        processor.processPayload(PayloadFactory.createOrderCancellationRequestIdPayload(1L, 1L));
    }

    @Test
    @DisplayName("Успешное создание заказа при ожидании отмены")
    @DatabaseSetup("/controller/order/cancel/before/cancellation_request_with_segments.xml")
    @DatabaseSetup("/service/business_process_state/create_order_external_async_request_sent.xml")
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/cancellation_request_processing_after_waiting.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successCreateOrderWithCancellationWaiting() throws Exception {
        createOrderPartnerResponse(
            "/orders/processing/ds/createSuccess",
            new CreateOrderSuccessDto("1", 48L, "LO-1", 1009L)
        );

        queueTaskChecker.assertQueueTaskNotCreated(
            QueueType.UPDATE_ORDER_CANCELLATION_REQUEST,
            createSegmentCancellationRequestIdPayload(1L, 1L)
        );
    }

    @Test
    @DisplayName("Ошибочное создание заказа при ожидании отмены")
    @DatabaseSetup("/controller/order/cancel/before/cancellation_request_with_segments.xml")
    @DatabaseSetup("/service/business_process_state/create_order_external_async_request_sent_segment_2.xml")
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/cancellation_request_processing_after_waiting_fail.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void errorCreateOrderWithCancellationWaiting() throws Exception {
        createOrderPartnerResponse(
            "/orders/processing/ds/createError",
            new CreateOrderErrorDto(48L, 1, null, "LO-1", 1009L, false)
        );

        CreateOrderErrorPayload createOrderErrorPayload = PayloadFactory.createOrderErrorPayload(
            ApiType.DELIVERY,
            48L,
            2L,
            1L,
            new CreateOrderErrorDto(48L, 1, null, "LO-1", 1009L, false),
            "1",
            1L
        );

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_CREATE_ORDER_ASYNC_ERROR_RESULT,
            createOrderErrorPayload
        );

        processCreateOrderAsyncErrorResultService.processPayload(createOrderErrorPayload);

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.UPDATE_ORDER_CANCELLATION_REQUEST,
            createSegmentCancellationRequestIdPayload(2L, "2", 2L)
        );
    }

    private void createOrderPartnerResponse(String api, Object request) throws Exception {
        mockMvc.perform(request(HttpMethod.PUT, api, request)).andExpect(status().isOk());
    }
}
