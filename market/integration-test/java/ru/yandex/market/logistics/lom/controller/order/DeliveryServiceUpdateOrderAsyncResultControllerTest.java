package ru.yandex.market.logistics.lom.controller.order;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistic.gateway.client.DeliveryClient;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.common.ResourceId;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.entity.BusinessProcessState;
import ru.yandex.market.logistics.lom.entity.enums.BusinessProcessStatus;
import ru.yandex.market.logistics.lom.jobs.model.OrderIdWaybillSegmentPayload;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.jobs.processor.CallCourierProcessor;
import ru.yandex.market.logistics.lom.model.async.UpdateOrderErrorDto;
import ru.yandex.market.logistics.lom.model.async.UpdateOrderSuccessDto;
import ru.yandex.market.logistics.lom.repository.BusinessProcessStateRepository;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;
import ru.yandex.market.logistics.mqm.client.MqmClient;
import ru.yandex.market.logistics.mqm.model.enums.EventType;
import ru.yandex.market.logistics.mqm.model.request.monitoringevent.EventCreateRequest;
import ru.yandex.market.logistics.mqm.model.request.monitoringevent.LomCreateOrderErrorPayload;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.noContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

public class DeliveryServiceUpdateOrderAsyncResultControllerTest extends AbstractContextualTest {
    private static final String BARCODE = "2-LOinttest-1";
    private static final String PARTNER_ORDER_ID = "TAXI-2-LOinttest-1";
    private static final Long TAXI_PARTNER = 1006360L;

    @Autowired
    private DeliveryClient deliveryClient;

    @Autowired
    private CallCourierProcessor callCourierProcessor;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private BusinessProcessStateRepository businessProcessStateRepository;

    @Autowired
    private MqmClient mqmClient;

    private static final OrderIdWaybillSegmentPayload PAYLOAD =
        PayloadFactory.createWaybillSegmentPayload(1, 2, "1", 1);

    @BeforeEach
    void setUp() {
        clock.setFixed(Instant.parse("2021-07-12T02:30:00.00Z"), ZoneId.systemDefault());
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(deliveryClient, mqmClient);
    }

    @Test
    @DisplayName("Обработка успешного ответа от такси: не было попытки вызова курьера раньше")
    @DatabaseSetup("/controller/order/update/async/before/taxi_setup.xml")
    @ExpectedDatabase(
        value = "/controller/order/update/async/after/update_order_status_to_processing.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void asyncResultSuccessProcessingForTaxiNoCallCourierBefore() throws Exception {
        sendAsyncResultSuccess(BARCODE, TAXI_PARTNER)
            .andExpect(status().isOk())
            .andExpect(noContent());

        queueTaskChecker.assertQueueTaskCreatedWithDelay(
            QueueType.CALL_COURIER,
            PAYLOAD,
            Duration.ofSeconds(41400L)
        );

        callCourierProcessor.processPayload(PAYLOAD);

        verify(deliveryClient).callCourier(
            ResourceId.builder()
                .setYandexId(BARCODE)
                .setPartnerId(PARTNER_ORDER_ID)
                .build(),
            Duration.ofSeconds(300),
            new Partner(TAXI_PARTNER)
        );

        softly.assertThat(backLogCaptor.getResults().toString()).contains(
            "level=INFO\t" +
                "format=plain\t" +
                "code=UPDATE_ORDER_EXTERNAL_SUCCESS\t" +
                "payload=Order updated\t" +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                "tags=BUSINESS_ORDER_EVENT\t" +
                "entity_types=barcode,partner\t" +
                "entity_values=barcode:2-LOinttest-1,partner:1006360"
        );
    }

    @Test
    @DisplayName("Обработка успешного ответа от такси: заказ в широкий интервал, курьер не вызывается")
    @DatabaseSetup(value = {
        "/controller/order/update/async/before/taxi_setup.xml",
        "/controller/order/update/async/before/taxi_setup_wide_interval.xml"
    })
    @ExpectedDatabase(
        value = "/controller/order/update/async/after/call_courier_not_called.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void asyncResultSuccessProcessingForTaxiWideInterval() throws Exception {
        sendAsyncResultSuccess(BARCODE, TAXI_PARTNER)
            .andExpect(status().isOk())
            .andExpect(noContent());

        queueTaskChecker.assertQueueTaskNotCreated(QueueType.CALL_COURIER);

        softly.assertThat(backLogCaptor.getResults().toString()).contains(
            "level=INFO\t" +
                "format=plain\t" +
                "code=UPDATE_ORDER_EXTERNAL_SUCCESS\t" +
                "payload=Order updated\t" +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                "tags=BUSINESS_ORDER_EVENT\t" +
                "entity_types=barcode,partner\t" +
                "entity_values=barcode:2-LOinttest-1,partner:1006360"
        );
    }

    @Test
    @DisplayName("Обработка успешного ответа от такси: вызов курьера раньше завершился с ошибкой QUEUE_TASK_ERROR")
    @DatabaseSetup("/controller/order/update/async/before/taxi_setup_call_courier_before.xml")
    @ExpectedDatabase(
        value = "/controller/order/update/async/after/update_order_status_to_processing.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void asyncResultSuccessProcessingForTaxiWasCallCourierBeforeCallAgain() throws Exception {
        sendAsyncResultSuccess(BARCODE, TAXI_PARTNER)
            .andExpect(status().isOk())
            .andExpect(noContent());

        queueTaskChecker.assertQueueTaskCreatedWithDelay(
            QueueType.CALL_COURIER,
            PAYLOAD,
            Duration.ZERO
        );

        callCourierProcessor.processPayload(PAYLOAD);

        verify(deliveryClient).callCourier(
            ResourceId.builder()
                .setYandexId(BARCODE)
                .setPartnerId(PARTNER_ORDER_ID)
                .build(),
            Duration.ofSeconds(300),
            new Partner(TAXI_PARTNER)
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @EnumSource(value = BusinessProcessStatus.class, names = { "ENQUEUED", "SYNC_PROCESS_SUCCEEDED"})
    @DisplayName("Обработка успешного ответа от такси: был вызов курьера ранее")
    @DatabaseSetup("/controller/order/update/async/before/taxi_setup_call_courier_before.xml")
    @ExpectedDatabase(
        value = "/controller/order/update/async/after/call_courier_not_called.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void asyncResultSuccessProcessingForTaxiWasCallCourierBeforeNoCall(
        BusinessProcessStatus businessProcessStatus
    ) throws Exception {
        transactionTemplate.execute(status -> {
            BusinessProcessState businessProcessState = businessProcessStateRepository.getOne(999L);
            businessProcessState.setStatus(businessProcessStatus);
            businessProcessStateRepository.save(businessProcessState);
            return null;
        });

        sendAsyncResultSuccess(BARCODE, TAXI_PARTNER)
            .andExpect(status().isOk())
            .andExpect(noContent());

        queueTaskChecker.assertQueueTaskNotCreated(QueueType.CALL_COURIER);
    }

    @Test
    @DisplayName("Обработка успешного ответа не от такси")
    @DatabaseSetup("/controller/order/update/async/before/not_taxi_setup.xml")
    void asyncResultSuccessProcessingNotForTaxi() throws Exception {
        sendAsyncResultSuccess(BARCODE, TAXI_PARTNER + 1)
            .andExpect(status().isOk())
            .andExpect(noContent());

        queueTaskChecker.assertQueueTaskNotCreated(QueueType.CALL_COURIER);

        softly.assertThat(backLogCaptor.getResults().toString()).contains(
            "level=INFO\t" +
                "format=plain\t" +
                "code=UPDATE_ORDER_EXTERNAL_SUCCESS\t" +
                "payload=Order updated\t" +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                "tags=BUSINESS_ORDER_EVENT\t" +
                "entity_types=barcode,partner\t" +
                "entity_values=barcode:2-LOinttest-1,partner:1006361"
        );
    }

    @Test
    @DisplayName("Обработка ошибки от такси")
    @DatabaseSetup("/controller/order/update/async/before/taxi_setup.xml")
    @ExpectedDatabase(
        value = "/controller/order/update/async/after/update_order_status_to_processing_error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void asyncResultErrorProcessingForTaxi() throws Exception {
        clock.setFixed(Instant.parse("2021-04-10T00:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE);

        sendAsyncResultError(BARCODE, TAXI_PARTNER)
            .andExpect(status().isOk())
            .andExpect(noContent());

        queueTaskChecker.assertQueueTaskNotCreated(QueueType.CALL_COURIER);

        softly.assertThat(backLogCaptor.getResults().toString()).contains(
            "level=ERROR\t" +
            "format=plain\t" +
            "code=UPDATE_ORDER_EXTERNAL_ERROR\t" +
            "payload=Code: 1234, message: 'error message'\t" +
            "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
            "tags=BUSINESS_ORDER_EVENT\t" +
            "entity_types=barcode,partner\t" +
            "entity_values=barcode:2-LOinttest-1,partner:1006360"
        );

        verify(mqmClient).pushMonitoringEvent(new EventCreateRequest(
                EventType.LOM_CREATE_ORDER_ERROR,
                new LomCreateOrderErrorPayload(
                    "2-LOinttest-1",
                    1006360L,
                    "",
                    1234,
                    "error message",
                    "DS",
                    2L,
                    true,
                    1L,
                    Instant.parse("2018-01-01T12:00:00.00Z"),
                    "VALIDATION_ERROR",
                    1000L
                )
            )
        );
    }

    @Test
    @DisplayName("Обработка ошибки от такси — заказ в широкий слот")
    @DatabaseSetup({
        "/controller/order/update/async/before/taxi_setup.xml",
        "/controller/order/update/async/before/taxi_setup_wide_interval.xml",
    })
    @ExpectedDatabase(
        value = "/controller/order/update/async/after/update_order_status_to_processing_error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void asyncResultErrorProcessingForTaxiWideSlots() throws Exception {
        clock.setFixed(Instant.parse("2021-04-10T00:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE);

        sendAsyncResultError(BARCODE, TAXI_PARTNER)
            .andExpect(status().isOk())
            .andExpect(noContent());

        queueTaskChecker.assertQueueTaskNotCreated(QueueType.CALL_COURIER);

        softly.assertThat(backLogCaptor.getResults().toString()).contains(
            "level=ERROR\t" +
            "format=plain\t" +
            "code=UPDATE_ORDER_EXTERNAL_ERROR\t" +
            "payload=Code: 1234, message: 'error message'\t" +
            "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
            "tags=BUSINESS_ORDER_EVENT\t" +
            "entity_types=barcode,partner\t" +
            "entity_values=barcode:2-LOinttest-1,partner:1006360"
        );
    }

    @Test
    @DisplayName("Обработка ошибки не отправляется т.к. есть 30 ЧП")
    @DatabaseSetup("/controller/order/update/async/before/delivery_with_transportation_setup.xml")
    void errorWithoutOrderErrorNotifyBecause30() throws Exception {
        clock.setFixed(Instant.parse("2021-04-10T00:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE);

        sendAsyncResultError(BARCODE, TAXI_PARTNER)
            .andExpect(status().isOk())
            .andExpect(noContent());
    }

    @Nonnull
    private ResultActions sendAsyncResultSuccess(String barcode, Long partnerId) throws Exception {
        return mockMvc.perform(request(
            HttpMethod.PUT,
            "/orders/processing/ds/updateSuccess",
            new UpdateOrderSuccessDto(barcode, partnerId, 1000L)
        ));
    }

    @Nonnull
    private ResultActions sendAsyncResultError(String barcode, Long partnerId) throws Exception {
        return mockMvc.perform(request(
            HttpMethod.PUT,
            "/orders/processing/ds/updateError",
            new UpdateOrderErrorDto(barcode, partnerId, 1000L, "error message", 1234, false)
        ));
    }
}
