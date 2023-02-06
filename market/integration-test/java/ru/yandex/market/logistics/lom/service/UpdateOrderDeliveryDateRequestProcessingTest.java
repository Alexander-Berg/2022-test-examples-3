package ru.yandex.market.logistics.lom.service;

import java.time.Duration;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.configuration.properties.FeatureProperties;
import ru.yandex.market.logistics.lom.jobs.model.ChangeOrderSegmentRequestPayload;
import ru.yandex.market.logistics.lom.jobs.model.ProcessingResult;
import ru.yandex.market.logistics.lom.jobs.model.ProcessingResultStatus;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.jobs.processor.ChangeOrderRequestProcessingService;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;

@DisplayName("Создание заявок на обновление даты доставки на сегментах на основе DELIVERY_DATE заявки")
class UpdateOrderDeliveryDateRequestProcessingTest extends AbstractContextualTest {

    @Autowired
    private ChangeOrderRequestProcessingService processor;

    @Autowired
    private FeatureProperties featureProperties;

    @AfterEach
    public void tearDown() {
        featureProperties.setUpdateLavkaSegmentDD(false);
    }

    @Test
    @DisplayName("Обновление на одном сегменте по DELIVERY_DATE заявке не приводит к обновлению ДД на след. сегментах")
    @DatabaseSetup("/controller/order/deliverydate/before/prepare_data.xml")
    void oneDeliverySegmentProcessing() {
        process(1L);

        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @DisplayName("Обновление даты доставки от средней мили не приводит к обновлению даты доставки в последней ииле")
    @DatabaseSetup("/controller/order/deliverydate/before/middle_mile_ds_segments.xml")
    void middleMileDeliverySegmentsProcessing() {
        process(1L);

        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @DisplayName("Успешное создание заявок на обновление даты доставки на двух сегментах")
    @DatabaseSetup("/controller/order/deliverydate/before/multiple_ds_segments.xml")
    @ExpectedDatabase(
        value = "/controller/order/deliverydate/after/3_4_segments_requests_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void multipleDeliverySegmentsProcessing() {
        process(1L);

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_WAYBILL_SEGMENT_UPDATE_DELIVERY_DATE,
            requestPayload(1L)
        );
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_WAYBILL_SEGMENT_UPDATE_DELIVERY_DATE,
            requestPayload(2L)
        );
    }

    @Test
    @DisplayName("Запрос уже обработан")
    @DatabaseSetup("/controller/order/deliverydate/before/prepare_data.xml")
    @DatabaseSetup(
        value = "/controller/order/deliverydate/before/request_is_already_processing.xml",
        type = DatabaseOperation.UPDATE
    )
    void requestIsAlreadyProcessing() {
        process(1L);

        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @DisplayName(
        "Запрос на основе DELIVERY_DATE заявки по сегменту в статусе REQUIRED_SUCCESS - след. сегменты не обновляются"
    )
    @DatabaseSetup("/controller/order/deliverydate/before/prepare_data.xml")
    void requestIsRequiredSuccess() {
        process(2L);

        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @DisplayName("Успешное создание заявки на обновление даты доставки в TAXI_LAVKA")
    @DatabaseSetup("/controller/order/deliverydate/before/prepare_data_taxi_segment.xml")
    @ExpectedDatabase(
        value = "/controller/order/deliverydate/after/2_segments_requests_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateDeliveryDateInTaxiLavka() {
        featureProperties.setUpdateLavkaSegmentDD(true);
        process(1L);
        queueTaskChecker.assertExactlyOneQueueTaskCreated(QueueType.PROCESS_WAYBILL_SEGMENT_UPDATE_DELIVERY_DATE);
    }

    @Test
    @DisplayName("Не создавать заявку на обновление даты доставки в TAXI_LAVKA при выключенной настройке")
    @DatabaseSetup("/controller/order/deliverydate/before/prepare_data_taxi_segment.xml")
    void doNotUpdateDeliveryDateInTaxiLavkaIfPropertyDisabled() {
        featureProperties.setUpdateLavkaSegmentDD(false);
        process(1L);
        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @DisplayName("Заказ в неподходящем статусе")
    @DatabaseSetup("/controller/order/deliverydate/before/prepare_data.xml")
    @DatabaseSetup(
        value = "/controller/order/deliverydate/before/order_in_wrong_status.xml",
        type = DatabaseOperation.UPDATE
    )
    void orderInWrongStatus() {
        ProcessingResult processingResult = process(1);
        softly.assertThat(processingResult.getStatus()).isEqualTo(ProcessingResultStatus.UNPROCESSED);
        softly.assertThat(processingResult.getComment())
            .isEqualTo("Try to change order 1 with status RETURNING");
    }

    @Test
    @DisplayName("Заказ в процессе создания")
    @DatabaseSetup("/controller/order/deliverydate/before/prepare_data.xml")
    @DatabaseSetup(
        value = "/controller/order/deliverydate/before/order_in_enqueued_status.xml",
        type = DatabaseOperation.UPDATE
    )
    void orderInEnqueuedStatus() {
        ProcessingResult processingResult = process(1L);
        softly.assertThat(processingResult.getStatus()).isEqualTo(ProcessingResultStatus.UNPROCESSED);
        softly.assertThat(processingResult.getComment())
            .isEqualTo("Try to change order 1 with status ENQUEUED, task will be retried");
        queueTaskChecker.assertQueueTaskCreatedWithDelay(
            QueueType.CHANGE_ORDER_REQUEST,
            PayloadFactory.createChangeOrderRequestPayload(1L, "1", 1L),
            Duration.ofHours(1)
        );
    }

    @Test
    @DisplayName("Партнёр неподдерживаемого подтипа")
    @DatabaseSetup("/controller/order/deliverydate/before/prepare_data.xml")
    @DatabaseSetup(
        value = "/controller/order/deliverydate/before/unsupported_partner_subtype.xml",
        type = DatabaseOperation.UPDATE
    )
    void unsupportedPartnerSubtype() {
        ProcessingResult processingResult = process(1);
        softly.assertThat(processingResult.getStatus()).isEqualTo(ProcessingResultStatus.SUCCESS);

        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @DisplayName("Партнёр не поддерживает метод")
    @DatabaseSetup("/controller/order/deliverydate/before/prepare_data.xml")
    @DatabaseSetup(
        value = "/controller/order/deliverydate/before/unsupported_partner.xml",
        type = DatabaseOperation.UPDATE
    )
    void unsupportedPartner() {
        ProcessingResult processingResult = process(1);
        softly.assertThat(processingResult.getStatus()).isEqualTo(ProcessingResultStatus.SUCCESS);

        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Nonnull
    private ProcessingResult process(long requestId) {
        return processor.processPayload(
            PayloadFactory.createChangeOrderRequestPayload(requestId, "1", 1L)
        );
    }

    @Nonnull
    private ChangeOrderSegmentRequestPayload requestPayload(long requestId) {
        return PayloadFactory.createChangeOrderSegmentRequestPayload(requestId, String.valueOf(requestId), requestId);
    }
}
