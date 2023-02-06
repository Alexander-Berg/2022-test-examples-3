package ru.yandex.market.logistics.lom.service.order;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.configuration.properties.OrderUpdateRecipientErrorMonitoringProperties;
import ru.yandex.market.logistics.lom.jobs.model.OrderUpdateRecipientErrorPayload;
import ru.yandex.market.logistics.lom.jobs.model.ProcessingResult;
import ru.yandex.market.logistics.lom.jobs.model.ProcessingResultStatus;
import ru.yandex.market.logistics.lom.jobs.processor.order.update.UpdateRecipientErrorMonitoringService;
import ru.yandex.market.logistics.mqm.client.MqmClient;
import ru.yandex.market.logistics.mqm.model.enums.EventType;
import ru.yandex.market.logistics.mqm.model.request.monitoringevent.EventCreateRequest;
import ru.yandex.market.logistics.mqm.model.request.monitoringevent.LomOrderUpdateRecipientErrorPayload;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class UpdateRecipientErrorMonitoringServiceTest extends AbstractContextualTest {

    @Autowired
    private MqmClient mqmClient;

    @Autowired
    private UpdateRecipientErrorMonitoringService updateRecipientErrorMonitoringService;

    @Autowired
    private OrderUpdateRecipientErrorMonitoringProperties properties;

    @AfterEach
    void tearDown() {
        properties.setEnabled(true);
        verifyNoMoreInteractions(mqmClient);
    }

    @Test
    @DisplayName("Создание задачи")
    @DatabaseSetup("/service/update_recipient_error_notification/before.xml")
    void createTask() {
        properties.setEnabled(true);
        ProcessingResult result = updateRecipientErrorMonitoringService.processPayload(payload());
        softly.assertThat(result.getStatus()).isEqualTo(ProcessingResultStatus.SUCCESS);
        verify(mqmClient).pushMonitoringEvent(new EventCreateRequest(
                EventType.LOM_ORDER_UPDATE_RECIPIENT_ERROR,
                new LomOrderUpdateRecipientErrorPayload(
                    "1",
                    1L,
                    1L
                )
            )
        );
    }

    @Test
    @DisplayName("Создание задачи отключено")
    @DatabaseSetup("/service/update_recipient_error_notification/before.xml")
    void createTaskDisabled() {
        properties.setEnabled(false);
        ProcessingResult result = updateRecipientErrorMonitoringService.processPayload(payload());
        softly.assertThat(result.getStatus()).isEqualTo(ProcessingResultStatus.SUCCESS);
        verify(mqmClient, never()).pushMonitoringEvent(any(EventCreateRequest.class));
    }

    @Nonnull
    private OrderUpdateRecipientErrorPayload payload() {
        return new OrderUpdateRecipientErrorPayload(
            "1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd/1",
            1,
            1,
            "1"
        );
    }
}
