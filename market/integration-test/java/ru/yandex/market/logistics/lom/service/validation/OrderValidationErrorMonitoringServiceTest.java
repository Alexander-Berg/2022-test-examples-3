package ru.yandex.market.logistics.lom.service.validation;

import java.time.Instant;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.configuration.properties.OrderValidationErrorMonitoringProperties;
import ru.yandex.market.logistics.lom.jobs.model.OrderValidationErrorPayload;
import ru.yandex.market.logistics.lom.jobs.model.ProcessingResult;
import ru.yandex.market.logistics.lom.jobs.model.ProcessingResultStatus;
import ru.yandex.market.logistics.lom.jobs.processor.validation.OrderValidationErrorMonitoringService;
import ru.yandex.market.logistics.mqm.client.MqmClient;
import ru.yandex.market.logistics.mqm.model.enums.EventType;
import ru.yandex.market.logistics.mqm.model.request.monitoringevent.EventCreateRequest;
import ru.yandex.market.logistics.mqm.model.request.monitoringevent.LomOrderValidationErrorPayload;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class OrderValidationErrorMonitoringServiceTest extends AbstractContextualTest {
    private static final Instant FIXED_TIME = Instant.parse("2021-01-01T00:00:00.00Z");

    @Autowired
    private MqmClient mqmClient;

    @Autowired
    private OrderValidationErrorMonitoringService orderValidationErrorMonitoringService;

    @Autowired
    private OrderValidationErrorMonitoringProperties properties;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(mqmClient);
    }

    @Test
    @DisplayName("Создание задачи")
    @DatabaseSetup("/service/validation_error_notification/before.xml")
    void createTask() {
        properties.setEnabled(true);
        ProcessingResult result = orderValidationErrorMonitoringService.processPayload(payload());
        softly.assertThat(result.getStatus()).isEqualTo(ProcessingResultStatus.SUCCESS);
        verify(mqmClient).pushMonitoringEvent(new EventCreateRequest(
                EventType.LOM_ORDER_VALIDATION_ERROR,
                new LomOrderValidationErrorPayload(
                    "12",
                    1L,
                    Instant.parse("2021-01-01T00:00:00Z"),
                    1L
                )
            )
        );
    }

    @Test
    @DisplayName("Создание задачи отключено")
    @DatabaseSetup("/service/validation_error_notification/before.xml")
    void createTaskDisabled() {
        properties.setEnabled(false);
        ProcessingResult result = orderValidationErrorMonitoringService.processPayload(payload());
        softly.assertThat(result.getStatus()).isEqualTo(ProcessingResultStatus.SUCCESS);
        verify(mqmClient, never()).pushMonitoringEvent(any(EventCreateRequest.class));
    }

    @Nonnull
    private OrderValidationErrorPayload payload() {
        return new OrderValidationErrorPayload(
            "1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd/1",
            1,
            1,
            "12",
            FIXED_TIME
        );
    }
}
