package ru.yandex.market.logistics.lom.jobs.processor.order.update;

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
import ru.yandex.market.logistics.mqm.client.MqmClient;
import ru.yandex.market.logistics.mqm.model.enums.EventType;
import ru.yandex.market.logistics.mqm.model.request.monitoringevent.EventCreateRequest;
import ru.yandex.market.logistics.mqm.model.request.monitoringevent.LomOrderUpdateCourierErrorPayload;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@DisplayName("Тесты на создание события LOM_ORDER_UPDATE_COURIER_ERROR в MQM")
class UpdateCourierErrorMonitoringServiceTest extends AbstractContextualTest {
    @Autowired
    private MqmClient mqmClient;

    @Autowired
    private OrderErrorMonitoringService monitoringService;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(mqmClient);
    }

    @Test
    @DisplayName("Успешный вызов MQM API для создания события обработки мониторингом")
    @DatabaseSetup("/service/update_recipient_error_notification/before.xml")
    void successfullMqmApiCall() {
        ProcessingResult result = monitoringService.processPayload(payload());
        softly.assertThat(result.getStatus()).isEqualTo(ProcessingResultStatus.SUCCESS);
        ArgumentCaptor<EventCreateRequest> captor = ArgumentCaptor.forClass(EventCreateRequest.class);
        verify(mqmClient).pushMonitoringEvent(captor.capture());

        EventCreateRequest value = captor.getValue();
        softly.assertThat(value.getEventType()).isEqualTo(EventType.LOM_ORDER_UPDATE_COURIER_ERROR);
        softly.assertThat(value.getPayload())
            .usingRecursiveComparison()
            .isEqualTo(new LomOrderUpdateCourierErrorPayload(
                    "1",
                    1L,
                    1L,
                    123L,
                    "Партнер"
                )
            );
    }

    @Nonnull
    private NotifyOrderErrorToMqmPayload payload() {
        return new NotifyOrderErrorToMqmPayload(
            "1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd/1",
            1,
            1L,
            "1",
            EventType.LOM_ORDER_UPDATE_COURIER_ERROR,
            null,
            Map.of(
                "partnerId", "123",
                "partnerName", "Партнер"
            )
        );
    }
}
