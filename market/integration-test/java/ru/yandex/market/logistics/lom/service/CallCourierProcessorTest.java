package ru.yandex.market.logistics.lom.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistic.gateway.client.DeliveryClient;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.common.ResourceId;
import ru.yandex.market.logistics.lom.jobs.consumer.CallCourierConsumer;
import ru.yandex.market.logistics.lom.jobs.model.NotifyOrderErrorToMqmPayload;
import ru.yandex.market.logistics.lom.jobs.model.OrderIdWaybillSegmentPayload;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.utils.jobs.TaskFactory;
import ru.yandex.market.logistics.mqm.client.MqmClient;
import ru.yandex.market.logistics.mqm.model.enums.EventType;
import ru.yandex.market.logistics.mqm.model.enums.RecallCourierReason;
import ru.yandex.market.logistics.util.client.exception.HttpTemplateException;
import ru.yandex.money.common.dbqueue.api.Task;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory.createWaybillSegmentPayload;
import static ru.yandex.market.logistics.lom.utils.lgw.CreateLgwCommonEntitiesUtils.createPartner;

@DatabaseSetup({
    "/service/call_courier/before/setup.xml",
    "/service/business_process_state/call_courier_enqueued.xml"
})
class CallCourierProcessorTest extends AbstractExternalServiceTest {
    private static final Partner PARTNER = createPartner(21);
    private static final ResourceId ORDER = ResourceId.builder().setYandexId("1001").setPartnerId("200").build();
    private static final Duration WAITING_TIME = Duration.ofMinutes(5);
    private static final OrderIdWaybillSegmentPayload PAYLOAD = createWaybillSegmentPayload(1L, 12L, "123", 1L);
    private static final Task<OrderIdWaybillSegmentPayload> TASK = TaskFactory.createTask(
        QueueType.CALL_COURIER,
        PAYLOAD
    );

    @Autowired
    private CallCourierConsumer callCourierConsumer;

    @Autowired
    private DeliveryClient deliveryClient;

    @Autowired
    private MqmClient mqmClient;

    @BeforeEach
    void setup() {
        clock.setFixed(Instant.parse("2021-04-10T00:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(deliveryClient, mqmClient);
    }

    @Test
    @DisplayName("Успешный вызов")
    void callSuccess() {
        callCourierConsumer.execute(TASK);
        verify(deliveryClient).callCourier(ORDER, WAITING_TIME, PARTNER);
    }

    @Test
    @DatabaseSetup(
        value = "/service/call_courier/before/active_cancellation_request.xml",
        type = DatabaseOperation.INSERT
    )
    @DisplayName("Ошибка: есть активная заявка на отмену")
    void callErrorHasActiveCancellationRequest() {
        callCourierConsumer.execute(TASK);
    }

    @Test
    @ExpectedDatabase(
        value = "/service/call_courier/after/error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Ошибка: ошибка при вызове")
    void callWithError() {
        Task<OrderIdWaybillSegmentPayload> task = TaskFactory.createTask(QueueType.CALL_COURIER, PAYLOAD, 100);

        when(deliveryClient.callCourier(ORDER, WAITING_TIME, PARTNER))
            .thenThrow(new HttpTemplateException(400, "error message"));

        callCourierConsumer.execute(task);

        queueTaskChecker.assertQueueTaskCreated(QueueType.NOTIFY_ORDER_ERROR_TO_MQM, new NotifyOrderErrorToMqmPayload(
            REQUEST_ID + "/1/1",
            1,
            123L,
            "1001",
            EventType.LOM_RECALL_COURIER,
            null,
            Map.of("recallReason", RecallCourierReason.UNKNOWN.name())
        ));
        verify(deliveryClient).callCourier(ORDER, WAITING_TIME, PARTNER);
    }
}
