package ru.yandex.market.logistics.lom.jobs.processor.order.processing;

import java.time.Instant;
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
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistic.api.model.common.ErrorCode;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.checker.QueueTaskChecker;
import ru.yandex.market.logistics.lom.configuration.properties.FeatureProperties;
import ru.yandex.market.logistics.lom.entity.enums.ApiType;
import ru.yandex.market.logistics.lom.entity.enums.BusinessProcessStatus;
import ru.yandex.market.logistics.lom.entity.ydb.BusinessProcessStateStatusHistoryYdb;
import ru.yandex.market.logistics.lom.exception.http.InappropriateOrderStateException;
import ru.yandex.market.logistics.lom.exception.http.ResourceNotFoundException;
import ru.yandex.market.logistics.lom.jobs.model.CreateOrderErrorPayload;
import ru.yandex.market.logistics.lom.jobs.model.OrderIdWaybillSegmentPayload;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.model.async.CreateOrderErrorDto;
import ru.yandex.market.logistics.lom.repository.ydb.BusinessProcessStateStatusHistoryYdbRepository;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;
import ru.yandex.market.logistics.mqm.model.enums.ProcessType;

import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessCreateOrderAsyncErrorResultServiceTest extends AbstractContextualTest {

    @Autowired
    private ProcessCreateOrderAsyncErrorResultService processCreateOrderAsyncErrorResultService;
    @Autowired
    private FeatureProperties featureProperties;
    @Autowired
    protected QueueTaskChecker queueTaskChecker;

    @Autowired
    private BusinessProcessStateStatusHistoryYdbRepository ydbHistoryRepository;

    @BeforeEach
    void setup() {
        clock.setFixed(Instant.parse("2019-06-12T00:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE);
    }

    /**
     * Заказ с указанным id существует.
     * В заказе существует сегмент путевого листа, указанный в BusinessProcessState.
     * В результате статус заказа меняется на PROCESSING_ERROR.
     * История изменений сохраняется.
     */
    @Test
    @DisplayName("Успешно проставлен статус ошибки для СД")
    @DatabaseSetup("/controller/order/processing/create/error/before/order_create_error.xml")
    @DatabaseSetup("/service/business_process_state/fulfillment_create_order_external_async_request_sent.xml")
    @ExpectedDatabase(
        value = "/controller/order/processing/create/error/after/order_create_error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void createOrderError() {
        processCreateOrderAsyncErrorResultService.processPayload(
            new CreateOrderErrorPayload(
                "1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd",
                ApiType.FULFILLMENT,
                1L,
                1L,
                1L,
                new CreateOrderErrorDto(
                    1L,
                    123,
                    "Bad Gateway",
                    "LOinttest-1",
                    1010L,
                    false
                )
            )
        );

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.SEND_PROCESSING_ERROR_TO_MQM,
            PayloadFactory.processingErrorPayload(
                ProcessType.LOM_ORDER_CREATE,
                1L,
                -2L,
                123,
                "Bad Gateway",
                "1",
                1L
            )
        );

        softly.assertThat(backLogCaptor.getResults().toString()).contains(
            "level=INFO"
                + "\tformat=plain"
                + "\tcode=FULFILLMENT_CREATE_ORDER_EXTERNAL"
                + "\tpayload=Business process state is changed with comment: Bad Gateway"
                + "\trequest_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd"
                + "\ttags=BUSINESS_PROCESS_STATE_UPDATE"
                + "\textra_keys=sequenceId,entityIds,status"
                + "\textra_values=1010,[BusinessProcessStateEntityId(id=1, entityType=WAYBILL_SEGMENT, entityId=1)]"
                + ",ERROR_RESPONSE_PROCESSING_SUCCEEDED"
        );
    }

    @Test
    @DisplayName("Не отправляем ошибку в MQM для тестовых заказов")
    @DatabaseSetup("/controller/order/processing/create/error/before/order_create_error_test_order.xml")
    @DatabaseSetup("/service/business_process_state/fulfillment_create_order_external_async_request_sent.xml")
    void createOrderErrorTestOrder() {
        processCreateOrderAsyncErrorResultService.processPayload(
            new CreateOrderErrorPayload(
                "1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd",
                ApiType.FULFILLMENT,
                1L,
                1L,
                1L,
                new CreateOrderErrorDto(
                    1L,
                    123,
                    "Bad Gateway",
                    "LOinttest-1",
                    1010L,
                    false
                )
            )
        );

        queueTaskChecker.assertQueueTaskNotCreated(QueueType.SEND_PROCESSING_ERROR_TO_MQM);
    }

    @Test
    @DisplayName("Не отправляем ошибку в MQM для DAAS")
    @DatabaseSetup("/controller/order/processing/create/error/before/order_create_error_daas.xml")
    @DatabaseSetup("/service/business_process_state/fulfillment_create_order_external_async_request_sent.xml")
    void createOrderErrorDaas() {
        processCreateOrderAsyncErrorResultService.processPayload(
            new CreateOrderErrorPayload(
                "1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd",
                ApiType.FULFILLMENT,
                1L,
                1L,
                1L,
                new CreateOrderErrorDto(
                    1L,
                    123,
                    "Bad Gateway",
                    "LOinttest-1",
                    1010L,
                    false
                )
            )
        );

        queueTaskChecker.assertQueueTaskNotCreated(QueueType.SEND_PROCESSING_ERROR_TO_MQM);
    }

    @Test
    @DisplayName("Отправляем ошибку в MQM для DBS")
    @DatabaseSetup("/controller/order/processing/create/error/before/order_create_error_dbs.xml")
    @DatabaseSetup("/service/business_process_state/fulfillment_create_order_external_async_request_sent.xml")
    void createOrderErrorDbs() {
        processCreateOrderAsyncErrorResultService.processPayload(
            new CreateOrderErrorPayload(
                "1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd",
                ApiType.FULFILLMENT,
                1L,
                1L,
                1L,
                new CreateOrderErrorDto(
                    1L,
                    123,
                    "Bad Gateway",
                    "LOinttest-1",
                    1010L,
                    false
                )
            )
        );

        queueTaskChecker.assertExactlyOneQueueTaskCreated(QueueType.SEND_PROCESSING_ERROR_TO_MQM);
    }

    @Test
    @DisplayName("Успешно проставлен статус ошибки для СЦ")
    @DatabaseSetup("/controller/order/processing/create/error/before/order_create_error_sc_and_ds.xml")
    @DatabaseSetup("/service/business_process_state/fulfillment_create_order_external_async_request_sent.xml")
    @ExpectedDatabase(
        value = "/controller/order/processing/create/error/after/order_create_error_sc_and_ds.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void createOrderErrorScAndDs() {
        processCreateOrderAsyncErrorResultService.processPayload(
            new CreateOrderErrorPayload(
                "1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd",
                ApiType.FULFILLMENT,
                1L,
                1L,
                1L,
                new CreateOrderErrorDto(
                    1L,
                    400,
                    "lgw SC error",
                    "LOinttest-1",
                    1010L,
                    false
                )
            )
        );

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.SEND_PROCESSING_ERROR_TO_MQM,
            PayloadFactory.processingErrorPayload(
                ProcessType.LOM_ORDER_CREATE,
                1L,
                -2L,
                400,
                "lgw SC error",
                "1",
                1L
            )
        );

        softly.assertThat(backLogCaptor.getResults().toString()).contains(
            "level=INFO"
                + "\tformat=plain"
                + "\tcode=FULFILLMENT_CREATE_ORDER_EXTERNAL"
                + "\tpayload=Business process state is changed with comment: lgw SC error"
                + "\trequest_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd"
                + "\ttags=BUSINESS_PROCESS_STATE_UPDATE"
                + "\textra_keys=sequenceId,entityIds,status"
                + "\textra_values=1010,[BusinessProcessStateEntityId(id=1, entityType=WAYBILL_SEGMENT, entityId=1)]"
                + ",ERROR_RESPONSE_PROCESSING_SUCCEEDED"
        );
    }

    @Test
    @DisplayName("Успешно проставлен статус ошибки для СЦ")
    @DatabaseSetup("/service/business_process_state/fulfillment_create_order_external_async_request_sent.xml")
    @DatabaseSetup("/controller/order/processing/create/error/before/order_create_error_sc_and_ds_cancelled.xml")
    @ExpectedDatabase(
        value = "/controller/order/processing/create/error/after/order_create_error_sc_and_ds_cancelled.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void createOrderErrorScAndDsCancelled() {
        processCreateOrderAsyncErrorResultService.processPayload(
            new CreateOrderErrorPayload(
                "1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd",
                ApiType.FULFILLMENT,
                1L,
                1L,
                1L,
                new CreateOrderErrorDto(
                    1L,
                    400,
                    "lgw SC error",
                    "LOinttest-1",
                    1010L,
                    false
                )
            )
        );

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.SEND_PROCESSING_ERROR_TO_MQM,
            PayloadFactory.processingErrorPayload(
                ProcessType.LOM_ORDER_CREATE,
                1L,
                -2L,
                400,
                "lgw SC error",
                "1",
                1L
            )
        );

        softly.assertThat(backLogCaptor.getResults().toString()).contains(
            "level=INFO"
                + "\tformat=plain"
                + "\tcode=FULFILLMENT_CREATE_ORDER_EXTERNAL"
                + "\tpayload=Business process state is changed with comment: lgw SC error"
                + "\trequest_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd"
                + "\ttags=BUSINESS_PROCESS_STATE_UPDATE"
                + "\textra_keys=sequenceId,entityIds,status"
                + "\textra_values=1010,[BusinessProcessStateEntityId(id=1, entityType=WAYBILL_SEGMENT, entityId=1)]"
                + ",ERROR_RESPONSE_PROCESSING_SUCCEEDED"
        );
    }

    @Test
    @DisplayName("Неподходящий статус заказа")
    @DatabaseSetup("/controller/order/processing/create/error/before/order_in_invalid_status.xml")
    @DatabaseSetup("/service/business_process_state/fulfillment_create_order_external_async_request_sent.xml")
    @ExpectedDatabase(
        value = "/controller/order/processing/create/error/before/order_in_invalid_status.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void orderInInvalidStatus() {
        softly.assertThatThrownBy(() -> processCreateOrderAsyncErrorResultService.processPayload(
                new CreateOrderErrorPayload(
                    "1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd",
                    ApiType.FULFILLMENT,
                    1L,
                    1L,
                    1L,
                    new CreateOrderErrorDto(
                        1L,
                        400,
                        "lgw SC error",
                        "LOinttest-1",
                        1010L,
                        false
                    )
                )
            ))
            .isInstanceOf(InappropriateOrderStateException.class);
    }

    @Test
    @DisplayName("Заказ не найден")
    @DatabaseSetup("/service/business_process_state/fulfillment_create_order_external_async_request_sent.xml")
    void orderNotFound() {
        softly.assertThatThrownBy(() -> processCreateOrderAsyncErrorResultService.processPayload(
                new CreateOrderErrorPayload(
                    "1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd",
                    ApiType.FULFILLMENT,
                    1L,
                    1L,
                    1L,
                    new CreateOrderErrorDto(
                        1L,
                        400,
                        "lgw SC error",
                        "LOinttest-1",
                        1010L,
                        false
                    )
                )
            ))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Невалидный запрос")
    @DatabaseSetup("/service/business_process_state/fulfillment_create_order_external_async_request_sent.xml")
    void badRequest() {
        softly.assertThatThrownBy(() -> processCreateOrderAsyncErrorResultService.processPayload(
                new CreateOrderErrorPayload(
                    "1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd",
                    ApiType.FULFILLMENT,
                    1L,
                    1L,
                    1L,
                    new CreateOrderErrorDto(
                        1L,
                        400,
                        "lgw SC error",
                        "LOinttest-1",
                        1010L,
                        false
                    )
                )
            ))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("Отменить заказ из-за технической ошибки создания заказа в магазине "
        + "и создание заявки на отмену заказа. Прошло время ретраев")
    @DatabaseSetup({
        "/service/business_process_state/fulfillment_create_order_external_async_request_sent.xml",
        "/controller/order/processing/create/error/before/order_create_error_dropship.xml"
    })
    @ExpectedDatabase(
        value = "/controller/order/processing/create/error/after/order_create_error_dropship.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @ExpectedDatabase(
        value = "/controller/order/processing/create/error/after/"
            + "order_create_error_dropship_cancel_request_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @ExpectedDatabase(
        value = "/controller/order/processing/create/error/after/tags/dropship_partner_error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void dropshipCancelOrderWithCancellationOrderRequestCreation(boolean retryUntilCutoff) {
        when(featureProperties.isRetryDropshipUntilCutoff()).thenReturn(retryUntilCutoff);
        dropshipCancelOrder(ErrorCode.SERVICE_UNAVAILABLE.getCode());
    }

    @Test
    @DisplayName("Не отменять заказ, если у него есть externalId на сегменте")
    @DatabaseSetup({
        "/service/business_process_state/fulfillment_create_order_external_async_request_sent.xml",
        "/controller/order/processing/create/error/before/order_create_error_dropship.xml"
    })
    @DatabaseSetup(
        value = "/controller/order/processing/create/error/before/waybill_segment_created.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/controller/order/processing/create/error/after/" +
            "order_create_error_dropship_cancel_request_not_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void checkOrderNotCancelledExternalIdExists() {
        dropshipCancelOrder(ErrorCode.SERVICE_UNAVAILABLE.getCode());
        softly.assertThat(backLogCaptor.getResults().toString()).contains(
            "level=WARN" +
                "\tformat=plain" +
                "\tcode=DROPSHIP_CREATE_ORDER_NOT_IDEMPOTENT_ERROR\t" +
                "payload=Order already has external id\t" +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                "entity_types=order,lom_order,partner\t" +
                "entity_values=order:LOinttest-1,lom_order:1,partner:1"
        );
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("Отменить заказ из-за технической ошибки создания экспресс заказа в магазине "
        + "и создание заявки на отмену заказа. Прошло время ретраев")
    @DatabaseSetup({
        "/service/business_process_state/fulfillment_create_order_external_async_request_sent.xml",
        "/controller/order/processing/create/error/before/order_create_error_express.xml"
    })
    @ExpectedDatabase(
        value = "/controller/order/processing/create/error/after/order_create_error_dropship.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @ExpectedDatabase(
        value = "/controller/order/processing/create/error/after/"
            + "order_create_error_dropship_cancel_request_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @ExpectedDatabase(
        value = "/controller/order/processing/create/error/after/tags/express_partner_error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void expressCancelOrderWithCancellationOrderRequestCreation(boolean retryUntilCutoff) {
        when(featureProperties.isRetryDropshipUntilCutoff()).thenReturn(retryUntilCutoff);
        dropshipCancelOrder(ErrorCode.SERVICE_UNAVAILABLE.getCode());
    }

    @Test
    @DisplayName("Отменить заказ из-за технической ошибки создания заказа в маркете "
        + "и создание заявки на отмену заказа. Прошло время ретраев")
    @DatabaseSetup({
        "/service/business_process_state/fulfillment_create_order_external_async_request_sent.xml",
        "/controller/order/processing/create/error/before/order_create_error_dropship.xml"
    })
    @ExpectedDatabase(
        value = "/controller/order/processing/create/error/after/order_create_error_dropship.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @ExpectedDatabase(
        value = "/controller/order/processing/create/error/after/"
            + "order_create_error_dropship_cancel_request_service_fault_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @ExpectedDatabase(
        value = "/controller/order/processing/create/error/after/tags/dropship_market_error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void dropshipMarketErrorCancelOrderWithCancellationOrderRequestCreation() {
        dropshipCancelOrder(ErrorCode.UNKNOWN_ERROR.getCode());
    }

    @Test
    @DisplayName("Отменить заказ из-за технической ошибки создания экспресс заказа в маркете "
        + "и создание заявки на отмену заказа. Прошло время ретраев")
    @DatabaseSetup({
        "/service/business_process_state/fulfillment_create_order_external_async_request_sent.xml",
        "/controller/order/processing/create/error/before/order_create_error_express.xml"
    })
    @ExpectedDatabase(
        value = "/controller/order/processing/create/error/after/order_create_error_dropship.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @ExpectedDatabase(
        value = "/controller/order/processing/create/error/after/"
            + "order_create_error_dropship_cancel_request_service_fault_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @ExpectedDatabase(
        value = "/controller/order/processing/create/error/after/tags/express_market_error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void expressMarketErrorCancelOrderWithCancellationOrderRequestCreation() {
        dropshipCancelOrder(ErrorCode.UNKNOWN_ERROR.getCode());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("Создать задачу для ретрая создания по технической ошибке создания заказа от магазина. "
        + "Не прошло время ретраев")
    @DatabaseSetup({
        "/service/business_process_state/fulfillment_create_order_external_async_request_sent.xml",
        "/controller/order/processing/create/error/before/order_create_error_dropship_retry.xml"
    })
    @ExpectedDatabase(
        value = "/controller/order/processing/create/error/after/dropship_task_retried.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @ExpectedDatabase(
        value = "/controller/order/processing/create/error/after/tags/dropship_partner_error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void dropshipRetryOrderShop(boolean retryUntilCutoff) {
        when(featureProperties.isRetryDropshipUntilCutoff()).thenReturn(retryUntilCutoff);
        dropshipCancelOrder(ErrorCode.SERVICE_UNAVAILABLE.getCode(), false);
        checkTaskRetried();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("Создать задачу для ретрая создания по технической ошибке создания заказа от маркета. "
        + "Не прошло время ретраев")
    @DatabaseSetup({
        "/service/business_process_state/fulfillment_create_order_external_async_request_sent.xml",
        "/controller/order/processing/create/error/before/order_create_error_dropship_retry.xml"
    })
    @ExpectedDatabase(
        value = "/controller/order/processing/create/error/after/dropship_task_retried.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @ExpectedDatabase(
        value = "/controller/order/processing/create/error/after/tags/dropship_market_error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void dropshipRetryOrderMarket(boolean retryUntilCutoff) {
        when(featureProperties.isRetryDropshipUntilCutoff()).thenReturn(retryUntilCutoff);
        dropshipCancelOrder(ErrorCode.UNKNOWN_ERROR.getCode(), false);
        checkTaskRetried();

        verify(ydbHistoryRepository).save(refEq(
            new BusinessProcessStateStatusHistoryYdb()
                .setId(-2L)
                .setSequenceId(1010L)
                .setStatus(BusinessProcessStatus.ERROR_RESPONSE_PROCESSING_SUCCEEDED)
                .setMessage("lgw dropship error\nTask will be restarted.")
                .setCreated(Instant.parse("2022-07-01T00:00:00.00Z"))
        ));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("Создать задачу для ретрая создания по технической ошибке создания экспресс заказа от магазина. "
        + "Не прошло время ретраев")
    @DatabaseSetup({
        "/service/business_process_state/fulfillment_create_order_external_async_request_sent.xml",
        "/controller/order/processing/create/error/before/order_create_error_express_retry.xml"
    })
    @ExpectedDatabase(
        value = "/controller/order/processing/create/error/after/express_task_retried.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @ExpectedDatabase(
        value = "/controller/order/processing/create/error/after/tags/express_partner_error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void dropshipRetryExpressOrderPartner(boolean retryUntilCutoff) {
        when(featureProperties.isRetryDropshipUntilCutoff()).thenReturn(retryUntilCutoff);
        dropshipCancelOrder(ErrorCode.SERVICE_UNAVAILABLE.getCode(), false);
        checkTaskRetried();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("Создать задачу для ретрая создания по технической ошибке создания экспресс заказа от маркета. "
        + "Не прошло время ретраев")
    @DatabaseSetup({
        "/service/business_process_state/fulfillment_create_order_external_async_request_sent.xml",
        "/controller/order/processing/create/error/before/order_create_error_express_retry.xml"
    })
    @ExpectedDatabase(
        value = "/controller/order/processing/create/error/after/express_task_retried.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @ExpectedDatabase(
        value = "/controller/order/processing/create/error/after/tags/express_market_error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void dropshipRetryExpressOrderMarket(boolean retryUntilCutoff) {
        when(featureProperties.isRetryDropshipUntilCutoff()).thenReturn(retryUntilCutoff);
        dropshipCancelOrder(ErrorCode.UNKNOWN_ERROR.getCode(), false);
        checkTaskRetried();
    }

    @Test
    @DisplayName("Не отменять дропшиповый заказ из-за технической ошибки создания заказа в маркете,"
        + " если выключена проперти")
    @DatabaseSetup({
        "/service/business_process_state/fulfillment_create_order_external_async_request_sent.xml",
        "/controller/order/processing/create/error/before/order_create_error_dropship.xml"
    })
    @ExpectedDatabase(
        value = "/controller/order/processing/create/error/after/no_cancellation_request_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/order/processing/create/error/after/tags/dropship_market_error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void dontCancelDropshipOrderAfterFF4ShopsError() {
        when(featureProperties.isRetryDropshipUntilCutoff()).thenReturn(false);
        dropshipCancelOrder(ErrorCode.UNKNOWN_ERROR.getCode());
    }

    @ParameterizedTest
    @MethodSource
    @DisplayName("Не отменять заказ из-за технической ошибки создания заказа в магазине "
        + "если установлен соответствующий флаг")
    @DatabaseSetup({
        "/service/business_process_state/fulfillment_create_order_external_async_request_sent.xml",
        "/controller/order/processing/create/error/before/order_create_error_dropship.xml",
        "/controller/order/processing/create/error/before/order_auto_cancellation_on_fbs_creation_fail_disabled.xml"
    })
    @ExpectedDatabase(
        value = "/controller/order/processing/create/error/after/no_cancellation_request_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void dropshipOrderCreationFailedOrderIsNotCancelled(int errorCode, boolean retryUntilCutoff) {
        when(featureProperties.isRetryDropshipUntilCutoff()).thenReturn(retryUntilCutoff);
        dropshipCancelOrder(errorCode);
    }

    @Nonnull
    private static Stream<Arguments> dropshipOrderCreationFailedOrderIsNotCancelled() {
        return Stream.of(
            Arguments.of(9999, true),
            Arguments.of(9999, false),
            Arguments.of(1000, true),
            Arguments.of(1000, false)
        );
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("Не отменять заказ из-за нетехнической ошибки")
    @DatabaseSetup({
        "/service/business_process_state/fulfillment_create_order_external_async_request_sent.xml",
        "/controller/order/processing/create/error/before/order_create_error_dropship.xml"
    })
    @ExpectedDatabase(
        value = "/controller/order/processing/create/error/after/no_cancellation_request_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/order/processing/create/error/after/tags/empty.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void dropshipOrderCreationFailedOrderIsNotCancelledNonTechnical(boolean retryUntilCutoff) {
        when(featureProperties.isRetryDropshipUntilCutoff()).thenReturn(retryUntilCutoff);
        dropshipCancelOrder(ErrorCode.ENTITY_NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("Отменить заказ из-за технической ошибки создания заказа в магазине, "
        + "заявка на отмену заказа существует")
    @DatabaseSetup({
        "/service/business_process_state/fulfillment_create_order_external_async_request_sent.xml",
        "/controller/order/processing/create/error/before/order_create_error_dropship.xml",
        "/controller/order/processing/create/error/before/order_dropship_already_has_cancellation_request.xml"
    })
    @ExpectedDatabase(
        value = "/controller/order/processing/create/error/after/order_create_error_dropship.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void dropshipCancelOrderCancellationOrderRequestExists() {
        dropshipCancelOrder(ErrorCode.SERVICE_UNAVAILABLE.getCode());
    }

    private void dropshipCancelOrder(int errorCode) {
        dropshipCancelOrder(errorCode, true);
    }

    private void dropshipCancelOrder(int errorCode, boolean checkLog) {
        processCreateOrderAsyncErrorResultService.processPayload(
            new CreateOrderErrorPayload(
                REQUEST_ID,
                ApiType.FULFILLMENT,
                1L,
                1L,
                1L,
                new CreateOrderErrorDto(
                    1L,
                    errorCode,
                    "lgw dropship error",
                    "LOinttest-1",
                    1010L,
                    false
                )
            )
        );

        if (checkLog) {
            softly.assertThat(backLogCaptor.getResults().toString()).contains(
                "level=INFO"
                    + "\tformat=plain"
                    + "\tcode=FULFILLMENT_CREATE_ORDER_EXTERNAL"
                    + "\tpayload=Business process state is changed with comment: lgw dropship error"
                    + "\trequest_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd"
                    + "\ttags=BUSINESS_PROCESS_STATE_UPDATE"
                    + "\textra_keys=sequenceId,entityIds,status"
                    + "\textra_values=1010,[BusinessProcessStateEntityId(id=1, entityType=WAYBILL_SEGMENT, entityId=1)]"
                    + ",ERROR_RESPONSE_PROCESSING_SUCCEEDED"
            );
        }
    }

    private void checkTaskRetried() {
        var payload = queueTaskChecker.getProducedTaskPayload(
            QueueType.FULFILLMENT_CREATE_ORDER_EXTERNAL,
            OrderIdWaybillSegmentPayload.class
        );
        var expectedPayload = new OrderIdWaybillSegmentPayload("ignored", 1L, 1L);
        expectedPayload.setSequenceId(1010L);
        softly.assertThat(payload)
            .usingRecursiveComparison()
            .ignoringFields("requestId")
            .isEqualTo(expectedPayload);
        softly.assertThat(payload.getRequestId())
            .isNotBlank()
            .doesNotStartWith(REQUEST_ID)
            .containsPattern("^\\d+/.+$");
    }
}
