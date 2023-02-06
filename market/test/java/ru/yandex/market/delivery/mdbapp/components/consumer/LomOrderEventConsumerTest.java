package ru.yandex.market.delivery.mdbapp.components.consumer;

import java.util.List;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

import ru.yandex.market.delivery.mdbapp.configuration.queue.LomOrderEventFailoverConfiguration;
import ru.yandex.market.delivery.mdbapp.enums.PlatformClient;
import ru.yandex.market.delivery.mdbapp.integration.service.LomOrderEventService;
import ru.yandex.market.logistics.lom.model.dto.EventDto;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.enums.EntityType;
import ru.yandex.market.logistics.lom.model.enums.SegmentStatus;
import ru.yandex.money.common.dbqueue.api.EnqueueParams;
import ru.yandex.money.common.dbqueue.api.QueueProducer;

public class LomOrderEventConsumerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private LomOrderEventService lomOrderEventService;

    private QueueProducer<LomOrderEventFailoverConfiguration.EventDtoWithError> eventDtoQueueProducer;

    private LomOrderEventConsumer lomOrderEventConsumer;

    @Before
    public void beforeTest() {
        lomOrderEventService = Mockito.mock(LomOrderEventService.class);
        eventDtoQueueProducer = Mockito.mock(QueueProducer.class);
        lomOrderEventConsumer = new LomOrderEventConsumer(lomOrderEventService, eventDtoQueueProducer, objectMapper);
    }

    @Test
    @DisplayName("Постановка событий в очередь")
    public void testEnqueue() {
        EventDto e1 = createEventDto(1L, createStatusDiff(), createSnapshot());
        EventDto e2 = createEventDto(2L, createStatusDiff(), createSnapshot());
        EventDto e3 = createEventDto(3L, createStatusDiff(), createSnapshot());

        lomOrderEventConsumer.accept(List.of(e1, e2, e3));

        Mockito.verify(lomOrderEventService).processEvent(Mockito.eq(e1));
        Mockito.verify(lomOrderEventService).processEvent(Mockito.eq(e2));
        Mockito.verify(lomOrderEventService).processEvent(Mockito.eq(e3));

        Mockito.verifyNoMoreInteractions(eventDtoQueueProducer, lomOrderEventService);
    }

    @Test
    @DisplayName("Обработка ошибок")
    public void testError() {
        EventDto e1 = createEventDto(1L, createStatusDiff(), createSnapshot());
        EventDto e2 = createEventDto(2L, createStatusDiff(), createSnapshot());
        EventDto e3 = createEventDto(3L, createStatusDiff(), createSnapshot());

        Mockito
            .doThrow(new IllegalStateException())
            .when(lomOrderEventService)
            .processEvent(Mockito.any());

        lomOrderEventConsumer.accept(List.of(e1, e2, e3));

        Mockito.verify(lomOrderEventService).processEvent(Mockito.eq(e1));
        Mockito.verify(lomOrderEventService).processEvent(Mockito.eq(e2));
        Mockito.verify(lomOrderEventService).processEvent(Mockito.eq(e3));

        Mockito.verify(eventDtoQueueProducer).enqueue(Mockito.argThat(new EnqueueParamsArgumentMatcher(e1)));
        Mockito.verify(eventDtoQueueProducer).enqueue(Mockito.argThat(new EnqueueParamsArgumentMatcher(e2)));
        Mockito.verify(eventDtoQueueProducer).enqueue(Mockito.argThat(new EnqueueParamsArgumentMatcher(e3)));

        Mockito.verifyNoMoreInteractions(eventDtoQueueProducer, lomOrderEventService);
    }

    @Nonnull
    private ArrayNode createStatusDiff() {
        ObjectNode segmentStatusDiff = objectMapper.createObjectNode();
        segmentStatusDiff.put("op", "replace");
        segmentStatusDiff.put("path", "/waybill/1/segmentStatus");
        segmentStatusDiff.put("value", SegmentStatus.IN.name());

        return objectMapper.createArrayNode().add(segmentStatusDiff);
    }

    @Nonnull
    private EventDto createEventDto(Long id, JsonNode diff, JsonNode snapshot) {
        return new EventDto()
            .setEntityId(id)
            .setEntityType(EntityType.ORDER)
            .setDiff(diff)
            .setSnapshot(snapshot);
    }

    @SneakyThrows
    private JsonNode createSnapshot(OrderDto orderDto) {
        return objectMapper.valueToTree(orderDto);
    }

    @SneakyThrows
    private JsonNode createSnapshot() {
        var order = new OrderDto().setPlatformClientId(PlatformClient.BERU.getId());
        return createSnapshot(order);
    }

    @RequiredArgsConstructor
    private static class EnqueueParamsArgumentMatcher
        implements ArgumentMatcher<EnqueueParams<LomOrderEventFailoverConfiguration.EventDtoWithError>> {
        private final EventDto payload;

        @Override
        public boolean matches(EnqueueParams<LomOrderEventFailoverConfiguration.EventDtoWithError> argument) {
            return argument.getPayload().getEventDto().equals(payload);
        }
    }
}
