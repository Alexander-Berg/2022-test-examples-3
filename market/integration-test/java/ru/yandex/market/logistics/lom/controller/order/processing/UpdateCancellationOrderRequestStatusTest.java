package ru.yandex.market.logistics.lom.controller.order.processing;

import java.time.Instant;
import java.time.ZoneOffset;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.configuration.properties.FeatureProperties;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.jobs.processor.UpdateCancellationOrderRequestProcessor;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;

import static ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory.createSegmentCancellationRequestIdPayload;

@DatabaseSetup("/controller/order/cancel/before/update_status_all_processing.xml")
class UpdateCancellationOrderRequestStatusTest extends AbstractContextualTest {

    @Autowired
    private UpdateCancellationOrderRequestProcessor processor;

    @Autowired
    private FeatureProperties featureProperties;

    @BeforeEach
    void setup() {
        clock.setFixed(Instant.parse("2021-01-28T12:00:00.00Z"), ZoneOffset.UTC);
    }

    @AfterEach
    void close() {
        featureProperties.setCancellationWithLrmAllEnabled(false).setUseNewFlowForExpressCancellation(false);
    }

    @Test
    @DisplayName("Все сегменты в статусе PROCESSING")
    @ExpectedDatabase(
        value = "/controller/order/cancel/before/update_status_all_processing.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void allProcessing() {
        processor.processPayload(createSegmentCancellationRequestIdPayload(1L, 1L));
        tasksNotCreated();
    }

    @Test
    @DisplayName("Все сегменты в статусе SUCCESS")
    @DatabaseSetup(
        value = "/controller/order/cancel/before/update_status_all_success.xml",
        type = DatabaseOperation.UPDATE
    )
    @DatabaseSetup(
        value = "/controller/order/cancel/before/update_status_all_success_with_plan_facts.xml",
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(
        value = "/controller/order/cancel/before/update_status_all_processing_storage_units.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/update_status_all_success_with_plan_facts.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void allOk() {
        processor.processPayload(createSegmentCancellationRequestIdPayload(1L, 1L));
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.EXPORT_ORDER_CANCELLED,
            PayloadFactory.lesOrderEventPayload(1, 1, "1", 1)
        );
        queueTaskChecker.assertQueueTaskNotCreated(QueueType.SEND_CANCELLATION_TO_LRM);
    }

    @Test
    @DisplayName("Все сегменты в статусе SUCCESS, отмена с LRM")
    @DatabaseSetup(
        value = "/controller/order/cancel/before/update_status_all_success.xml",
        type = DatabaseOperation.UPDATE
    )
    @DatabaseSetup(
        value = "/controller/order/cancel/before/update_status_all_processing_storage_units.xml",
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(
        value = "/controller/order/cancel/before/update_status_all_success_with_lrm.xml",
        type = DatabaseOperation.UPDATE
    )
    void allOkWithLrm() {
        featureProperties.setCancellationWithLrmAllEnabled(true);
        processor.processPayload(createSegmentCancellationRequestIdPayload(1L, 1L));
        queueTaskChecker.assertQueueTaskNotCreated(QueueType.EXPORT_ORDER_CANCELLED);
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.SEND_CANCELLATION_TO_LRM,
            PayloadFactory.createOrderIdPayload(1, "1", 1)
        );
    }

    @Test
    @DisplayName("Все сегменты в статусе SUCCESS, отмена с LRM, существует активный возврат, не создаем новый")
    @DatabaseSetup(
        value = "/controller/order/cancel/before/update_status_all_success.xml",
        type = DatabaseOperation.UPDATE
    )
    @DatabaseSetup("/controller/order/cancel/before/order_return.xml")
    @DatabaseSetup(
        value = "/controller/order/cancel/before/update_status_all_processing_storage_units.xml",
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(
        value = "/controller/order/cancel/before/update_status_all_success_with_lrm.xml",
        type = DatabaseOperation.UPDATE
    )
    void allOkWithLrmActiveReturnExists() {
        featureProperties.setCancellationWithLrmAllEnabled(true);
        processor.processPayload(createSegmentCancellationRequestIdPayload(1L, 1L));
        tasksNotCreated();
    }

    @Test
    @DisplayName("Все сегменты в статусе SUCCESS, отмена с LRM, все существующие возвраты неактивны, создаем новый")
    @DatabaseSetup(
        value = "/controller/order/cancel/before/update_status_all_success.xml",
        type = DatabaseOperation.UPDATE
    )
    @DatabaseSetup("/controller/order/cancel/before/order_return_cancelled.xml")
    @DatabaseSetup(
        value = "/controller/order/cancel/before/update_status_all_processing_storage_units.xml",
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(
        value = "/controller/order/cancel/before/update_status_all_success_with_lrm.xml",
        type = DatabaseOperation.UPDATE
    )
    void allOkWithLrmActiveReturnNotExists() {
        featureProperties.setCancellationWithLrmAllEnabled(true);
        processor.processPayload(createSegmentCancellationRequestIdPayload(1L, 1L));
        queueTaskChecker.assertQueueTaskNotCreated(QueueType.EXPORT_ORDER_CANCELLED);
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.SEND_CANCELLATION_TO_LRM,
            PayloadFactory.createOrderIdPayload(1, "1", 1)
        );
    }

    @Test
    @DisplayName("Все сегменты в статусе SUCCESS, отмена с LRM без СЦ")
    @DatabaseSetup(
        value = "/controller/order/cancel/before/update_status_all_success.xml",
        type = DatabaseOperation.UPDATE
    )
    @DatabaseSetup(
        value = "/controller/order/cancel/before/update_status_all_processing_storage_units.xml",
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(
        value = "/controller/order/cancel/before/update_status_all_success_without_sc.xml",
        type = DatabaseOperation.UPDATE
    )
    void allOkWithLrmWithoutSc() {
        featureProperties.setCancellationWithLrmAllEnabled(true);
        processor.processPayload(createSegmentCancellationRequestIdPayload(1L, 1L));
        tasksNotCreated();
    }

    @Test
    @DisplayName("Все сегменты в статусе SUCCESS, заявка в статусе REJECTED")
    @DatabaseSetup(
        value = "/controller/order/cancel/before/update_status_all_success_rejected.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/update_status_all_success_rejected.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void allSuccessRejected() {
        processor.processPayload(createSegmentCancellationRequestIdPayload(1L, 1L));
        tasksNotCreated();
    }

    @Test
    @DisplayName("Все сегменты в статусе FAIL")
    @DatabaseSetup(
        value = "/controller/order/cancel/before/update_status_all_fail.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/update_status_all_fail.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void allFail() {
        orderCancellationProperties.setSkipWaitingCheckpointsFallbackOnApiCancellationFail(true);
        processor.processPayload(createSegmentCancellationRequestIdPayload(1L, 1L));
        tasksNotCreated();
    }

    @Test
    @DisplayName("Все сегменты в статусе FAIL (ожидание чекпоинтов)")
    @DatabaseSetup(
        value = "/controller/order/cancel/before/update_status_all_fail.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/update_status_fail_sync.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void allFailSync() {
        orderCancellationProperties.setSkipWaitingCheckpointsFallbackOnApiCancellationFail(false);
        processor.processPayload(createSegmentCancellationRequestIdPayload(1L, 1L));
        tasksNotCreated();
    }

    @Test
    @DisplayName("Обязательные сегменты в статусе SUCCESS")
    @DatabaseSetup(
        value = "/controller/order/cancel/before/update_status_required_success.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/update_status_required_success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void requiredOk() {
        processor.processPayload(createSegmentCancellationRequestIdPayload(1L, 1L));
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.EXPORT_ORDER_CANCELLED,
            PayloadFactory.lesOrderEventPayload(1, 1, "1", 1)
        );
        queueTaskChecker.assertQueueTaskNotCreated(QueueType.SEND_CANCELLATION_TO_LRM);
    }

    @Test
    @DisplayName("Один из обязательных сегментов в статусе SUCCESS")
    @DatabaseSetup(
        value = "/controller/order/cancel/before/update_status_one_required_success.xml",
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(
        value = "/controller/order/cancel/before/update_status_required_success.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/update_status_one_required_success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void bothRequiredOk() {
        processor.processPayload(createSegmentCancellationRequestIdPayload(2L, 1L));
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.EXPORT_ORDER_CANCELLED,
            PayloadFactory.lesOrderEventPayload(1, 1, "1", 1)
        );
        queueTaskChecker.assertQueueTaskNotCreated(QueueType.SEND_CANCELLATION_TO_LRM);
    }

    @Test
    @DisplayName("Один из обязательных сегментов в статусе SUCCESS")
    @DatabaseSetup(
        value = "/controller/order/cancel/before/update_status_one_required_success.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/update_status_not_changed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void oneRequiredOk() {
        processor.processPayload(createSegmentCancellationRequestIdPayload(2L, 1L));
        tasksNotCreated();
    }

    @Test
    @DisplayName("Один из достаточных, но необязательных сегментов в статусе SUCCESS")
    @DatabaseSetup(
        value = "/controller/order/cancel/before/update_status_sufficient_not_required_success.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/update_status_not_changed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void oneSufficientNotRequiredOk() {
        processor.processPayload(createSegmentCancellationRequestIdPayload(2L, 1L));
        tasksNotCreated();
    }

    @Test
    @DisplayName("Обязательные сегменты в статусе FAIL")
    @DatabaseSetup(
        value = "/controller/order/cancel/before/update_status_required_fail.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/update_status_required_fail.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void requiredFail() {
        orderCancellationProperties.setSkipWaitingCheckpointsFallbackOnApiCancellationFail(true);
        processor.processPayload(createSegmentCancellationRequestIdPayload(1L, 1L));
        tasksNotCreated();
    }

    @Test
    @DisplayName("Обязательные сегменты в статусе FAIL (ожидание чекпоинтов)")
    @DatabaseSetup(
        value = "/controller/order/cancel/before/update_status_required_fail.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/update_status_fail_sync.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void requiredFailSync() {
        orderCancellationProperties.setSkipWaitingCheckpointsFallbackOnApiCancellationFail(false);
        processor.processPayload(createSegmentCancellationRequestIdPayload(1L, 1L));
        tasksNotCreated();
    }

    @Test
    @DisplayName("Не обязательные сегменты в статусе SUCCESS")
    @DatabaseSetup(
        value = "/controller/order/cancel/before/update_status_not_required_success.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/update_status_not_changed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void notRequiredOk() {
        processor.processPayload(createSegmentCancellationRequestIdPayload(1L, 1L));
        tasksNotCreated();
    }

    @Test
    @DisplayName("Не обязательные сегменты в статусе FAIL")
    @DatabaseSetup(
        value = "/controller/order/cancel/before/update_status_not_required_fail.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/update_status_not_changed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void notRequiredFail() {
        processor.processPayload(createSegmentCancellationRequestIdPayload(1L, 1L));
        tasksNotCreated();
    }

    @Test
    @DisplayName("Успешный статус SEGMENT_NOT_STARTED")
    @DatabaseSetup(
        value = "/controller/order/cancel/before/update_status_all_not_started.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/update_status_all_success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void segmentNotStated() {
        processor.processPayload(createSegmentCancellationRequestIdPayload(1L, 1L));
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.EXPORT_ORDER_CANCELLED,
            PayloadFactory.lesOrderEventPayload(1, 1, "1", 1)
        );
        queueTaskChecker.assertQueueTaskNotCreated(QueueType.SEND_CANCELLATION_TO_LRM);
    }

    @Test
    @DisplayName("Обработка ошибки обновления статуса")
    @DatabaseSetup(
        value = "/controller/order/cancel/before/update_status_all_not_started.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/update_status_final_fail.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void finalFailure() {
        processor.processFinalFailure(
            createSegmentCancellationRequestIdPayload(1L, 1L),
            new RuntimeException("Process final failure")
        );
    }

    @Test
    @DisplayName("Все сегменты в статусе SUCCESS, заказ в статусе RETURNING")
    @DatabaseSetup("/controller/order/cancel/before/update_status_returning.xml")
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/update_status_returning.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void cancelReturning() {
        processor.processPayload(createSegmentCancellationRequestIdPayload(1L, 1L));
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.EXPORT_ORDER_CANCELLED,
            PayloadFactory.lesOrderEventPayload(1, 1, "1", 1)
        );
        queueTaskChecker.assertQueueTaskNotCreated(QueueType.SEND_CANCELLATION_TO_LRM);
    }

    @Test
    @DisplayName("Все сегменты в статусе SUCCESS, заказ в статусе LOST")
    @DatabaseSetup("/controller/order/cancel/before/update_status_lost.xml")
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/update_status_lost.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void cancelLost() {
        processor.processPayload(createSegmentCancellationRequestIdPayload(1L, 1L));
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.EXPORT_ORDER_CANCELLED,
            PayloadFactory.lesOrderEventPayload(1, 1, "1", 1)
        );
        queueTaskChecker.assertQueueTaskNotCreated(QueueType.SEND_CANCELLATION_TO_LRM);
    }

    @Test
    @DisplayName("Без обязательных сегментов")
    @DatabaseSetup("/controller/order/cancel/before/update_status_without_required.xml")
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/update_status_without_required.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void withoutRequired() {
        processor.processPayload(createSegmentCancellationRequestIdPayload(1L, 1L));
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.EXPORT_ORDER_CANCELLED,
            PayloadFactory.lesOrderEventPayload(1, 1, "1", 1)
        );
        queueTaskChecker.assertQueueTaskNotCreated(QueueType.SEND_CANCELLATION_TO_LRM);
    }

    @Test
    @DisplayName("Ручное подтверждение для заказа")
    @DatabaseSetup("/controller/order/cancel/before/update_status_order_manually_confirmed.xml")
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/update_status_order_manually_confirmed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void manuallyConfirmedOrder() {
        processor.processPayload(createSegmentCancellationRequestIdPayload(1L, 1L));
    }

    @Test
    @DisplayName("Ручное подтверждение для сегмента")
    @DatabaseSetup("/controller/order/cancel/before/update_status_segment_manually_confirmed.xml")
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/update_status_segment_manually_confirmed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void manuallyConfirmedSegment() {
        processor.processPayload(createSegmentCancellationRequestIdPayload(1L, 1L));
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.EXPORT_ORDER_CANCELLED,
            PayloadFactory.lesOrderEventPayload(1, 1, "1", 1)
        );
        queueTaskChecker.assertQueueTaskNotCreated(QueueType.SEND_CANCELLATION_TO_LRM);
    }

    @Test
    @DisplayName("Все сегменты в статусе SUCCESS, дата отгрузки для первого сегмента - вчера")
    @DatabaseSetup(
        value = "/controller/order/cancel/before/update_status_all_success.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/update_status_all_success_shipment_not_updated.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void shipmentDateLessThanToday() {
        clock.setFixed(Instant.parse("2021-01-29T09:00:00.00Z"), ZoneOffset.UTC);
        processor.processPayload(createSegmentCancellationRequestIdPayload(1L, 1L));
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.EXPORT_ORDER_CANCELLED,
            PayloadFactory.lesOrderEventPayload(1, 1, "1", 1)
        );
        queueTaskChecker.assertQueueTaskNotCreated(QueueType.SEND_CANCELLATION_TO_LRM);
    }

    @Test
    @DisplayName("Все сегменты в статусе SUCCESS, заявка на отгрузку в статусе DELIVERY_SERVICE_PROCESSING")
    @DatabaseSetup(
        value = "/controller/order/cancel/before/update_status_shipment_application_ds_processing.xml",
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(
        value = "/controller/order/cancel/before/update_status_all_success.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/update_status_all_success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void shipmentApplicationIsDeliveryServiceProcessing() {
        processor.processPayload(createSegmentCancellationRequestIdPayload(1L, 1L));
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.EXPORT_ORDER_CANCELLED,
            PayloadFactory.lesOrderEventPayload(1, 1, "1", 1)
        );
        queueTaskChecker.assertQueueTaskNotCreated(QueueType.SEND_CANCELLATION_TO_LRM);
    }

    @Test
    @DisplayName("Все сегменты в статусе SUCCESS, заявка на отгрузку в статусе REGISTRY_SENT")
    @DatabaseSetup(
        value = "/controller/order/cancel/before/update_status_shipment_application_registry_sent.xml",
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(
        value = "/controller/order/cancel/before/update_status_all_success.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/update_status_all_success_shipment_not_updated.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void shipmentApplicationRegistrySent() {
        processor.processPayload(createSegmentCancellationRequestIdPayload(1L, 1L));
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.EXPORT_ORDER_CANCELLED,
            PayloadFactory.lesOrderEventPayload(1, 1, "1", 1)
        );
        queueTaskChecker.assertQueueTaskNotCreated(QueueType.SEND_CANCELLATION_TO_LRM);
    }

    @Test
    @DisplayName("Сегменты в статусах SUCCESS, NON_CANCELLABLE_SEGMENT, WAITING_CHECKPOINTS")
    @DatabaseSetup(
        value = "/controller/order/cancel/before/update_status_not_cancellable.xml",
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(
        value = "/controller/order/cancel/before/update_status_success_waiting_for_checkpoint.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/update_status_success_waiting_for_checkpoint_not_cancellable.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void cancellationRequestStatusesAllowsUntieFromShipment() {
        processor.processPayload(createSegmentCancellationRequestIdPayload(1L, 1L));
        tasksNotCreated();
    }

    @Test
    @DisplayName("Все сегменты в статусе SUCCESS, заказ не принадлежит платформе Яндекс.Доставка")
    @DatabaseSetup(
        value = "/controller/order/cancel/before/update_status_order_platform_1.xml",
        type = DatabaseOperation.UPDATE
    )
    @DatabaseSetup(
        value = "/controller/order/cancel/before/update_status_all_success.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/update_status_all_success_shipment_not_updated.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void orderNotForDaasPlatform() {
        processor.processPayload(createSegmentCancellationRequestIdPayload(1L, 1L));
        tasksNotCreated();
    }

    private void tasksNotCreated() {
        queueTaskChecker.assertQueueTaskNotCreated(QueueType.EXPORT_ORDER_CANCELLED);
        queueTaskChecker.assertQueueTaskNotCreated(QueueType.SEND_CANCELLATION_TO_LRM);
    }
}
