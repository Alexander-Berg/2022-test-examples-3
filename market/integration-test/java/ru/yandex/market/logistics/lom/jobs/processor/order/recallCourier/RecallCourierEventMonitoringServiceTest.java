package ru.yandex.market.logistics.lom.jobs.processor.order.recallCourier;

import java.time.Instant;
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
import ru.yandex.market.logistics.mqm.model.enums.RecallCourierReason;
import ru.yandex.market.logistics.mqm.model.request.monitoringevent.EventCreateRequest;
import ru.yandex.market.logistics.mqm.model.request.monitoringevent.LomRecallCourierPayload;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@DisplayName("Создание события LOM_RECALL_COURIER в MQM")
public class RecallCourierEventMonitoringServiceTest extends AbstractContextualTest {
    @Autowired
    private MqmClient mqmClient;

    @Autowired
    private OrderErrorMonitoringService monitoringService;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(mqmClient);
    }

    @Test
    @DisplayName("Успех")
    @DatabaseSetup("/service/recall_courier_notification/before.xml")
    void success() {
        ProcessingResult result =
            monitoringService.processPayload(payload(Map.of("recallReason", RecallCourierReason.UNKNOWN.name())));

        softly.assertThat(result.getStatus()).isEqualTo(ProcessingResultStatus.SUCCESS);
        ArgumentCaptor<EventCreateRequest> captor = ArgumentCaptor.forClass(EventCreateRequest.class);
        verify(mqmClient).pushMonitoringEvent(captor.capture());

        EventCreateRequest value = captor.getValue();
        softly.assertThat(value.getEventType()).isEqualTo(EventType.LOM_RECALL_COURIER);
        softly.assertThat(value.getPayload())
            .usingRecursiveComparison()
            .isEqualTo(new LomRecallCourierPayload(
                "1001",
                1L,
                Instant.parse("2018-01-01T12:00:00Z"),
                RecallCourierReason.UNKNOWN
            ));
    }

    @Test
    @DisplayName("Не указана причина")
    @DatabaseSetup("/service/recall_courier_notification/before.xml")
    void noReason() {
        softly.assertThatCode(() -> monitoringService.processPayload(payload(Map.of())))
            .hasMessage("Error during error dispatch to MQM")
            .hasCauseInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Заказ не существует")
    void noOrder() {
        softly.assertThatCode(() -> monitoringService.processPayload(payload(Map.of())))
            .hasMessage("Error during error dispatch to MQM")
            .hasCauseInstanceOf(RuntimeException.class);
    }

    @Nonnull
    private NotifyOrderErrorToMqmPayload payload(Map<String, String> params) {
        return new NotifyOrderErrorToMqmPayload(
            REQUEST_ID,
            1,
            123L,
            "1001",
            EventType.LOM_RECALL_COURIER,
            null,
            params
        );
    }
}
