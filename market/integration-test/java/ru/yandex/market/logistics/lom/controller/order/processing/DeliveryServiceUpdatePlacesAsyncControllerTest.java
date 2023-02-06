package ru.yandex.market.logistics.lom.controller.order.processing;

import java.time.Instant;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import one.util.streamex.EntryStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.jobs.consumer.NotifyOrderErrorToMqmConsumer;
import ru.yandex.market.logistics.lom.jobs.model.NotifyOrderErrorToMqmPayload;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.model.async.UpdateOrderErrorDto;
import ru.yandex.market.logistics.lom.model.async.UpdateOrderSuccessDto;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;
import ru.yandex.market.logistics.lom.utils.jobs.TaskFactory;
import ru.yandex.market.logistics.mqm.client.MqmClient;
import ru.yandex.market.logistics.mqm.model.enums.EventType;
import ru.yandex.market.logistics.mqm.model.request.monitoringevent.EventCreateRequest;
import ru.yandex.market.logistics.mqm.model.request.monitoringevent.LomCreateOrderErrorPayload;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory.createWaybillSegmentPayload;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@ParametersAreNonnullByDefault
@DatabaseSetup("/controller/order/processing/update_places_async/ds/before/prepare.xml")
class DeliveryServiceUpdatePlacesAsyncControllerTest extends AbstractContextualTest {

    @Autowired
    private NotifyOrderErrorToMqmConsumer notifyOrderErrorToMqmConsumer;

    @Autowired
    private MqmClient mqmClient;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(mqmClient);
    }

    @Test
    @DisplayName("Обработка успешного ответа")
    @ExpectedDatabase(
        value = "/controller/order/processing/update_places_async/ds/after/success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void successGeneral() throws Exception {
        performSuccessRequest(new UpdateOrderSuccessDto("1001", 48L, 10L))
            .andExpect(status().isOk());

        queueTaskChecker.assertQueueTaskNotCreated(QueueType.CALL_COURIER);
    }

    @Test
    @DisplayName("Обработка успешного ответа - CALL_COURIER сегмент")
    @DatabaseSetup(
        value = "/controller/order/processing/update_places_async/ds/before/call_courier_tag.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/controller/order/processing/update_places_async/ds/after/success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void successCallCourier() throws Exception {
        performSuccessRequest(new UpdateOrderSuccessDto("1001", 48L, 10L))
            .andExpect(status().isOk());

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.CALL_COURIER,
            createWaybillSegmentPayload(1L, 2L, "1", 1L)
        );
    }

    @Test
    @DisplayName("Обработка ответа об ошибке")
    @ExpectedDatabase(
        value = "/controller/order/processing/update_places_async/ds/after/error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void error() throws Exception {
        performErrorRequest(new UpdateOrderErrorDto(
            "1001",
            48L,
            10L,
            "9999 - update at partner error",
            9999,
            false
        ))
            .andExpect(status().isOk());

        NotifyOrderErrorToMqmPayload payload = PayloadFactory.notifyOrderErrorToMqmPayload(
            1L,
            10L,
            "1001",
            EventType.LOM_CREATE_ORDER_ERROR,
            null,
            EntryStream.of(
                    "errorCode", "9999",
                    "errorMessage", "9999 - update at partner error",
                    "waybillSegmentId", "2"
                )
                .toSortedMap(),
            "2",
            2
        );
        queueTaskChecker.assertQueueTaskCreated(QueueType.NOTIFY_ORDER_ERROR_TO_MQM, payload);

        notifyOrderErrorToMqmConsumer.execute(TaskFactory.createTask(QueueType.NOTIFY_ORDER_ERROR_TO_MQM, payload, 0));

        verify(mqmClient).pushMonitoringEvent(new EventCreateRequest(
            EventType.LOM_CREATE_ORDER_ERROR,
            new LomCreateOrderErrorPayload(
                "1001",
                48L,
                "Название службы доставки",
                9999,
                "9999 - update at partner error",
                "DS",
                2L,
                false,
                1L,
                Instant.parse("2022-06-28T09:00:00Z"),
                "VALIDATION_ERROR",
                10L
            )
        ));
    }

    @Test
    @DisplayName("Обработка ответа об ошибке: заказ в широкие слоты экспресса, в MQM не отправляем")
    @DatabaseSetup(
        value = "/controller/order/processing/update_places_async/ds/before/express_batch_tag.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/controller/order/processing/update_places_async/ds/after/error_no_notify.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void errorWithExpressBatch() throws Exception {
        performErrorRequest(new UpdateOrderErrorDto(
            "1001",
            48L,
            10L,
            "9999 - update at partner error",
            9999,
            false
        ))
            .andExpect(status().isOk());

        queueTaskChecker.assertQueueTaskNotCreated(QueueType.NOTIFY_ORDER_ERROR_TO_MQM);
    }

    @Test
    @DisplayName("Успешный ответ - у задачи нет сущности заявки изменения сегмента заказа")
    @DatabaseSetup(
        value = "/controller/order/processing/update_places_async/ds/before/no_segment_request_in_process.xml",
        type = DatabaseOperation.INSERT
    )
    void successNoSegmentRequestInBusinessProcess() throws Exception {
        performSuccessRequest(new UpdateOrderSuccessDto("1001", 48L, 3L))
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage(
                "BusinessProcessState with id 2 has no entity with type CHANGE_ORDER_SEGMENT_REQUEST"
            ));
    }

    @Test
    @DisplayName("Успешный ответ - невалидный sequenceId")
    @ExpectedDatabase(
        value = "/controller/order/processing/update_places_async/ds/before/prepare.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void successInvalidSequenceId() throws Exception {
        performSuccessRequest(new UpdateOrderSuccessDto("1001", 48L, 1L))
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [BUSINESS_PROCESS] with id [1]"));
    }

    @Nonnull
    private ResultActions performSuccessRequest(UpdateOrderSuccessDto request) throws Exception {
        return mockMvc.perform(request(HttpMethod.PUT, "/orders/processing/ds/updateSuccess", request));
    }

    @Nonnull
    private ResultActions performErrorRequest(UpdateOrderErrorDto request) throws Exception {
        return mockMvc.perform(request(HttpMethod.PUT, "/orders/processing/ds/updateError", request));
    }
}
