package ru.yandex.market.logistics.lom.controller.order.processing;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.entity.InternalVariable;
import ru.yandex.market.logistics.lom.entity.enums.InternalVariableType;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.repository.InternalVariableRepository;
import ru.yandex.market.logistics.mqm.model.enums.EventType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory.createSegmentCancellationRequestIdPayload;
import static ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory.notifyOrderErrorToMqmPayload;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.noContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DatabaseSetup("/controller/order/cancel/before/cancellation_request_waiting_reply.xml")
@DatabaseSetup("/service/business_process_state/process_waybill_segment_cancel_async_request_sent.xml")
public class CancelOrderAsyncReplyControllerTest extends AbstractContextualTest {
    private static final Map<Long, Long> PARTNER_SEQUENCE_ID = Map.of(
        41L, 1001L,
        48L, 1002L,
        50L, 1003L,
        999L, 1999L
    );

    private static final Instant FIXED_TIME = Instant.parse("2021-01-01T15:00:00.00Z");

    private static final Duration TECH_RETRY_DELAY_DEFAULT_VALUE = Duration.ofHours(2);

    private static final int ERROR_CODE_AS_TECH_FAIL = 3999;
    private static final int ERROR_CODE_AS_NO_TECH_FAIL = 4000;

    @Autowired
    private InternalVariableRepository internalVariableRepository;

    @BeforeEach
    void setup() {
        orderCancellationProperties.setConfirmCancellationByApiWithoutCheckpoints(false);
        clock.setFixed(FIXED_TIME, DateTimeUtils.MOSCOW_ZONE);
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("apiSource")
    @DisplayName("Невалидный запрос")
    void badRequest(String url) throws Exception {
        mockMvc.perform(put(url).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonContent("controller/order/processing/cancel/bad_request.json"));
    }

    @Nonnull
    static Stream<String> apiSource() {
        return Stream.of(
            "/orders/ds/cancel/success",
            "/orders/ds/cancel/error",
            "/orders/ff/cancel/success",
            "/orders/ff/cancel/error"
        );
    }

    @Test
    @DisplayName("Успешная отмена ds-api (с ожиданием чекпоинта)")
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/cancellation_request_after_ds_success_with_checkpoint.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void successDsApiWithDeliveryProcessing() throws Exception {
        execCancelReply("/orders/ds/cancel/success", 48L);

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.UPDATE_ORDER_CANCELLATION_REQUEST,
            createSegmentCancellationRequestIdPayload(2L, "1", 1L)
        );
    }

    @Test
    @DisplayName("Успешная отмена ds-api (без ожидания чекпоинта)")
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/cancellation_request_after_ds_success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void successDsApiWithDeliveryProcessingWithoutWaitingCheckpoints() throws Exception {
        orderCancellationProperties.setConfirmCancellationByApiWithoutCheckpoints(true);

        execCancelReply("/orders/ds/cancel/success", 48L);

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.UPDATE_ORDER_CANCELLATION_REQUEST,
            createSegmentCancellationRequestIdPayload(2L, "1", 1L)
        );
    }

    @Test
    @DisplayName("Успешная отмена ff-api")
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/cancellation_request_after_ff_success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void successFfApi() throws Exception {
        execCancelReply("/orders/ff/cancel/success", 41L);

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.UPDATE_ORDER_CANCELLATION_REQUEST,
            createSegmentCancellationRequestIdPayload(1L, "1", 1L)
        );
    }

    @Test
    @DisplayName("Успешная отмена ff-api (ДШ)")
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/cancellation_request_after_dropship_success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void successDropshipApi() throws Exception {
        execCancelReply("/orders/ff/cancel/success", 50L);

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.UPDATE_ORDER_CANCELLATION_REQUEST,
            createSegmentCancellationRequestIdPayload(3L, "1", 1L)
        );
    }

    @Test
    @DisplayName("Успешная отмена стрельбового заказа ds-api")
    @DatabaseSetup(
        value = "/controller/order/cancel/before/shooting_order.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/cancellation_request_after_ds_success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void successShootingOrderDs() throws Exception {
        execCancelReply("/orders/ds/cancel/success", 48L);

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.UPDATE_ORDER_CANCELLATION_REQUEST,
            createSegmentCancellationRequestIdPayload(2L, "1", 1L)
        );
    }

    @Test
    @DisplayName("Успешная отмена стрельбового заказа ff-api")
    @DatabaseSetup(
        value = "/controller/order/cancel/before/shooting_order.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/cancellation_request_after_ff_success_by_api.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void successShootingOrderFf() throws Exception {
        execCancelReply("/orders/ff/cancel/success", 41L);

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.UPDATE_ORDER_CANCELLATION_REQUEST,
            createSegmentCancellationRequestIdPayload(1L, "1", 1L)
        );
    }

    @Test
    @DisplayName("Неудачная отмена стрельбового заказа ds-api")
    @DatabaseSetup(
        value = "/controller/order/cancel/before/shooting_order.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/cancellation_request_after_ds_error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void errorShootingDs() throws Exception {
        execCancelReply("/orders/ds/cancel/error", 48L);

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.UPDATE_ORDER_CANCELLATION_REQUEST,
            createSegmentCancellationRequestIdPayload(2L, "1", 1L)
        );
    }

    @Test
    @DisplayName("Неудачная отмена стрельбового заказа ff-api")
    @DatabaseSetup(
        value = "/controller/order/cancel/before/shooting_order.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/cancellation_request_after_ff_error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void errorShootingFf() throws Exception {
        execCancelReply("/orders/ff/cancel/error", 41L);

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.UPDATE_ORDER_CANCELLATION_REQUEST,
            createSegmentCancellationRequestIdPayload(1L, "1", 1L)
        );
    }


    @Test
    @DisplayName("Успешная отмена стрельбового заказа ds-api с пользователем из выделенного диапазона")
    @DatabaseSetup(
        value = "/controller/order/cancel/before/ranged_shooting_order.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/cancellation_request_after_ds_success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void successRangedShootingOrderDs() throws Exception {
        execCancelReply("/orders/ds/cancel/success", 48L);

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.UPDATE_ORDER_CANCELLATION_REQUEST,
            createSegmentCancellationRequestIdPayload(2L, "1", 1L)
        );
    }

    @Test
    @DisplayName("Успешная отмена стрельбового заказа ff-api с пользователем из выделенного диапазона")
    @DatabaseSetup(
        value = "/controller/order/cancel/before/ranged_shooting_order.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/cancellation_request_after_ff_success_by_api.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void successRangedShootingOrderFf() throws Exception {
        execCancelReply("/orders/ff/cancel/success", 41L);

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.UPDATE_ORDER_CANCELLATION_REQUEST,
            createSegmentCancellationRequestIdPayload(1L, "1", 1L)
        );
    }

    @Test
    @DisplayName("Неудачная отмена стрельбового заказа ds-api с пользователем из выделенного диапазона")
    @DatabaseSetup(
        value = "/controller/order/cancel/before/ranged_shooting_order.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/cancellation_request_after_ds_error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void errorRangedShootingDs() throws Exception {
        execCancelReply("/orders/ds/cancel/error", 48L);

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.UPDATE_ORDER_CANCELLATION_REQUEST,
            createSegmentCancellationRequestIdPayload(2L, "1", 1L)
        );
    }

    @Test
    @DisplayName("Неудачная отмена стрельбового заказа ff-api с пользователем из выделенного диапазона")
    @DatabaseSetup(
        value = "/controller/order/cancel/before/ranged_shooting_order.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/cancellation_request_after_ff_error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void errorRangedShootingFf() throws Exception {
        execCancelReply("/orders/ff/cancel/error", 41L);

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.UPDATE_ORDER_CANCELLATION_REQUEST,
            createSegmentCancellationRequestIdPayload(1L, "1", 1L)
        );
    }

    @Test
    @DisplayName("Ретрай успешной отмены ff-api")
    @DatabaseSetup(
        value = "/controller/order/cancel/before/cancellation_request_waiting_reply_with_success.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/cancellation_request_after_ff_success_retry.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void successFfApiWithRetry() throws Exception {
        execCancelReply("/orders/ff/cancel/success", 41L);

        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @DisplayName("Неудачная отмена ds-api")
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/cancellation_request_after_ds_error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void errorDsApi() throws Exception {
        execCancelReply("/orders/ds/cancel/error", 48L);

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.UPDATE_ORDER_CANCELLATION_REQUEST,
            createSegmentCancellationRequestIdPayload(2L, "1", 1L)
        );
    }

    @Test
    @DisplayName("Неудачная отмена ds-api по технической причине")
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/cancellation_request_after_ds_tech_error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void errorDsApiTech() throws Exception {
        execCancelReplyWithTechError("/orders/ds/cancel/error", 48L, true, null);

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.UPDATE_ORDER_CANCELLATION_REQUEST,
            createSegmentCancellationRequestIdPayload(2L, "1", 1L)
        );

        queueTaskChecker.assertQueueTaskCreatedWithDelay(
            QueueType.PROCESS_WAYBILL_SEGMENT_CANCEL,
            TECH_RETRY_DELAY_DEFAULT_VALUE
        );

        Map<String, String> data = new HashMap<>();
        data.put("orderId", "1");
        data.put("orderBarcode", "LO123");
        data.put("orderExternalId", "1001");
        data.put("orderType", "Fulfillment");
        data.put("segmentId", "2");
        data.put("segmentPartnerId", "48");
        data.put("segmentPartnerName", "");
        data.put("segmentApiType", "DS");
        data.put("cancellationSegmentRequestId", "2");
        data.put("cancellationSegmentRequestCreated", "2022-07-07");
        data.put("cancellationOrderRequestId", "1");
        data.put("businessProcessStateId", "2");
        data.put("businessProcessStateSequenceId", "1002");
        data.put("businessProcessStateComment", null);
        data.put("cancelOrderErrorMessage", "error message");
        data.put("cancelOrderErrorCode", "null");
        data.put("cancelOrderSequenceId", "1002");
        data.put("retryAttempt", "0");
        data.put("maxRetryCount", "9223372036854775807");
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.NOTIFY_ORDER_ERROR_TO_MQM,
            notifyOrderErrorToMqmPayload(
                1L,
                1002L,
                "1001",
                EventType.DYNAMIC,
                "LOM_ORDER_CANCELLATION_ERROR_WITH_TECH_FAIL",
                data,
                "2",
                2
            )
        );
    }

    @Test
    @DisplayName("Неудачная отмена ds-api по технической причине без нотификации")
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/cancellation_request_after_ds_tech_error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void errorDsApiTechWithoutNotifyBecauseDisabled() throws Exception {
        internalVariableRepository.save(
            new InternalVariable()
                .setType(InternalVariableType.LOM_ORDER_CANCELLATION_ERROR_WITH_TECH_FAIL_DISABLED)
                .setValue("true")
        );

        execCancelReplyWithTechError("/orders/ds/cancel/error", 48L, true, null);

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.UPDATE_ORDER_CANCELLATION_REQUEST,
            createSegmentCancellationRequestIdPayload(2L, "1", 1L)
        );

        queueTaskChecker.assertQueueTaskNotCreated(QueueType.NOTIFY_ORDER_ERROR_TO_MQM);
    }

    @Test
    @DisplayName("Неудачная отмена ds-api по технической причине без нотификации так как есть партнер")
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/cancellation_request_after_ds_tech_error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void errorDsApiTechWithoutNotifyBecauseExcludePartner() throws Exception {
        internalVariableRepository.save(
            new InternalVariable()
                .setType(InternalVariableType.LOM_ORDER_CANCELLATION_ERROR_WITH_TECH_FAIL_EXCLUDE_PARTNERS)
                .setValue("2,48")
        );

        execCancelReplyWithTechError("/orders/ds/cancel/error", 48L, true, null);

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.UPDATE_ORDER_CANCELLATION_REQUEST,
            createSegmentCancellationRequestIdPayload(2L, "1", 1L)
        );

        queueTaskChecker.assertQueueTaskNotCreated(QueueType.NOTIFY_ORDER_ERROR_TO_MQM);
    }

    @Test
    @DisplayName("Неудачная отмена ds-api по технической причине без нотификации так как есть OUT на сегменте")
    @DatabaseSetup(
        value = "/controller/order/cancel/before/cancellation_request_with_out_status.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/cancellation_request_after_ds_tech_error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void errorDsApiTechWithoutNotifyBecauseOut() throws Exception {
        execCancelReplyWithTechError("/orders/ds/cancel/error", 48L, true, null);

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.UPDATE_ORDER_CANCELLATION_REQUEST,
            createSegmentCancellationRequestIdPayload(2L, "1", 1L)
        );

        queueTaskChecker.assertQueueTaskNotCreated(QueueType.NOTIFY_ORDER_ERROR_TO_MQM);
    }

    @Test
    @DisplayName("Неудачная отмена ds-api по технической причине, если есть код ошибки")
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/cancellation_request_after_ds_tech_error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void errorDsApiTechWithErrorCode() throws Exception {
        execCancelReplyWithTechError(
            "/orders/ds/cancel/error", 48L, false, ERROR_CODE_AS_TECH_FAIL);

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.UPDATE_ORDER_CANCELLATION_REQUEST,
            createSegmentCancellationRequestIdPayload(2L, "1", 1L)
        );

        queueTaskChecker.assertQueueTaskCreatedWithDelay(
            QueueType.PROCESS_WAYBILL_SEGMENT_CANCEL,
            TECH_RETRY_DELAY_DEFAULT_VALUE
        );
    }

    @Test
    @DisplayName(
        "Неудачная отмена ds-api по не технической причине, и есть код ошибки, который не является техническим")
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/cancellation_request_after_ds_no_tech_error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void errorDsApiNoTechWithErrorCodeNoTechFail() throws Exception {
        execCancelReplyWithTechError(
            "/orders/ds/cancel/error", 48L, false, ERROR_CODE_AS_NO_TECH_FAIL);

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.UPDATE_ORDER_CANCELLATION_REQUEST,
            createSegmentCancellationRequestIdPayload(2L, "1", 1L)
        );

        queueTaskChecker.assertQueueTaskNotCreated(QueueType.PROCESS_WAYBILL_SEGMENT_CANCEL);
    }

    @Test
    @DisplayName("Неудачная отмена ff-api")
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/cancellation_request_after_ff_error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void errorFfApi() throws Exception {
        execCancelReply("/orders/ff/cancel/error", 41L);

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.UPDATE_ORDER_CANCELLATION_REQUEST,
                createSegmentCancellationRequestIdPayload(1L, "1", 1L)
        );
    }

    @Test
    @DisplayName("Неудачный ответ после успешной отмены ff-api")
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/cancellation_request_after_ff_success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void errorAfterSuccessFfApi() throws Exception {
        execCancelReply("/orders/ff/cancel/success", 41L);
        execCancelReply("/orders/ff/cancel/error", 41L);

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.UPDATE_ORDER_CANCELLATION_REQUEST,
            createSegmentCancellationRequestIdPayload(1L, "1", 1L)
        );
    }

    @Test
    @DisplayName("Неудачный ответ после успешной отмены ff-api (техническая ошибка)")
    @ExpectedDatabase(
        value = "/controller/order/cancel/after/cancellation_request_after_ff_success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void errorAfterSuccessFfApiTech() throws Exception {
        execCancelReply("/orders/ff/cancel/success", 41L);
        execCancelReplyWithTechError("/orders/ff/cancel/error", 41L, true, null);

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.UPDATE_ORDER_CANCELLATION_REQUEST,
            createSegmentCancellationRequestIdPayload(1L, "1", 1L)
        );

        queueTaskChecker.assertQueueTaskNotCreated(QueueType.PROCESS_WAYBILL_SEGMENT_CANCEL);
    }

    @Test
    @DisplayName("Несуществующий сегмент")
    @ExpectedDatabase(
        value = "/controller/order/cancel/before/cancellation_request_waiting_reply.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void segmentNotFound() throws Exception {
        execCancelReplyWithError("/orders/ff/cancel/success");
        execCancelReplyWithError("/orders/ds/cancel/success");
        execCancelReplyWithError("/orders/ff/cancel/error");
        execCancelReplyWithError("/orders/ds/cancel/error");
    }

    private void execCancelReply(String url, long partnerId) throws Exception {
        mockMvc.perform(
            put(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format(
                    "{\"sequenceId\": %s,\"barcode\": \"LO123\",\"partnerId\": %s}",
                    PARTNER_SEQUENCE_ID.get(partnerId),
                    partnerId
                ))
        )
            .andExpect(status().isOk())
            .andExpect(noContent());
    }

    private void execCancelReplyWithTechError(
        String url,
        long partnerId,
        Boolean techError,
        Integer errorCode
    ) throws Exception {
        mockMvc.perform(
            put(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format(
                    "{" +
                        "\"sequenceId\": %s," +
                        "\"barcode\": \"LO123\"," +
                        "\"partnerId\": %s," +
                        "\"message\": \"error message\"," +
                        "\"techError\": %s," +
                        "\"errorCode\": %s" +
                        "}",
                    PARTNER_SEQUENCE_ID.get(partnerId),
                    partnerId,
                    techError,
                    errorCode
                ))
        )
            .andExpect(status().isOk())
            .andExpect(noContent());
    }

    private void execCancelReplyWithError(String url) throws Exception {
        long partnerId = 999L;
        mockMvc.perform(
            put(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format(
                    "{\"sequenceId\": %s,\"barcode\": \"LO123\",\"partnerId\": %s}",
                    PARTNER_SEQUENCE_ID.get(partnerId),
                    partnerId
                ))
        )
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [CANCELLATION_SEGMENT_REQUEST] with id [4]"));
    }
}
