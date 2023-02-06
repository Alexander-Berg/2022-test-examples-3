package ru.yandex.market.logistics.lom.jobs.processor.order.cancel;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.jobs.model.NotifyOrderErrorToMqmPayload;
import ru.yandex.market.logistics.lom.jobs.model.ProcessingResult;
import ru.yandex.market.logistics.lom.jobs.model.ProcessingResultStatus;
import ru.yandex.market.logistics.lom.jobs.processor.order.update.OrderErrorMonitoringService;
import ru.yandex.market.logistics.mqm.client.MqmClient;
import ru.yandex.market.logistics.mqm.model.enums.EventType;
import ru.yandex.market.logistics.mqm.model.request.monitoringevent.DynamicEventPayload;
import ru.yandex.market.logistics.mqm.model.request.monitoringevent.EventCreateRequest;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@DisplayName("Тесты на создание события об ошибках при отмене заказа в MQM")
public class CancelOrderErrorMonitoringServiceTest extends AbstractContextualTest {
    @Autowired
    private MqmClient mqmClient;

    @Autowired
    private OrderErrorMonitoringService monitoringService;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(mqmClient);
    }

    @Test
    @DisplayName("Успешный вызов MQM API для создания события обработки мониторингом при технической ошибке")
    @DatabaseSetup("/service/cancel_order_error_with_tech_fail/setup.xml")
    void successMqmApiCallWithTechFail() {
        ProcessingResult result = monitoringService.processPayload(payloadWithTechFail());
        softly.assertThat(result.getStatus()).isEqualTo(ProcessingResultStatus.SUCCESS);
        ArgumentCaptor<EventCreateRequest> captor = ArgumentCaptor.forClass(EventCreateRequest.class);
        verify(mqmClient).pushMonitoringEvent(captor.capture());

        Map<String, String> data = new HashMap<>();
        data.put("orderId", "1");
        EventCreateRequest value = captor.getValue();
        softly.assertThat(value.getEventType()).isEqualTo(EventType.DYNAMIC);
        softly.assertThat(value.getPayload())
            .usingRecursiveComparison()
            .isEqualTo(new DynamicEventPayload(
                    "LOM_ORDER_CANCELLATION_ERROR_WITH_TECH_FAIL",
                    data
                )
            );
    }

    @Nonnull
    private NotifyOrderErrorToMqmPayload payloadWithTechFail() {
        Map<String, String> data = new HashMap<>();
        data.put("orderId", "1");
        return new NotifyOrderErrorToMqmPayload(
            "1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd/1",
            1,
            1L,
            "1",
            EventType.DYNAMIC,
            "LOM_ORDER_CANCELLATION_ERROR_WITH_TECH_FAIL",
            data
        );
    }
}
