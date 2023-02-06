package ru.yandex.market.logistics.management.service.dbqueue;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logbroker.LogbrokerEventPublisher;
import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.entity.logbroker.LmsLogbrokerEvent;
import ru.yandex.market.logistics.management.entity.logbroker.EventDto;
import ru.yandex.market.logistics.management.queue.model.LogbrokerEventPayload;
import ru.yandex.market.logistics.management.queue.processor.LogbrokerEventProcessingService;
import ru.yandex.market.logistics.management.util.TestableClock;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.market.logistics.management.entity.logbroker.EntityType.BUSINESS_WAREHOUSE;

public class LogbrokerEventProcessingServiceTest extends AbstractContextualTest {

    @Autowired
    private LogbrokerEventProcessingService logbrokerEventProcessingService;
    @Autowired
    private LogbrokerEventPublisher<LmsLogbrokerEvent> logbrokerEventPublisher;
    @Autowired
    private TestableClock clock;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testTaskProcessedAndEventPublished() {
        EventDto eventDto = new EventDto()
            .setEventId(10L)
            .setEntityType(BUSINESS_WAREHOUSE)
            .setEntityId(20L)
            .setEventTimestamp(clock.instant())
            .setEntitySnapshot(objectMapper.valueToTree(Map.of("k1", "v1", "k2", List.of("v21", "v22"))))
            .setEntityDiff(objectMapper.valueToTree(Map.of("k4", List.of("v41", "v42", "v43"), "k5", "v5")));

        logbrokerEventProcessingService.processPayload(new LogbrokerEventPayload(REQUEST_ID, eventDto));

        ArgumentCaptor<LmsLogbrokerEvent> argumentCaptor = ArgumentCaptor.forClass(LmsLogbrokerEvent.class);
        Mockito.verify(logbrokerEventPublisher).publishEvent(argumentCaptor.capture());

        LmsLogbrokerEvent publishedLmsLogbrokerEvent = argumentCaptor.getValue();

        softly.assertThat(publishedLmsLogbrokerEvent.getBytes()).as("Bytes in event is not null").isNotNull();
        softly.assertThat(publishedLmsLogbrokerEvent.getPayload())
            .as("Event was published correctly")
            .usingRecursiveComparison()
            .isEqualTo(eventDto);
    }

    @Test
    void testExceptionFromPublisher() {
        Mockito.doThrow(new RuntimeException("Failed to publish event"))
            .when(logbrokerEventPublisher)
            .publishEvent(Mockito.any());

        assertThatThrownBy(() -> logbrokerEventProcessingService.processPayload(
            new LogbrokerEventPayload(REQUEST_ID, new EventDto())
        ))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Failed to publish event");
    }
}
