package ru.yandex.market.delivery.mdbapp.components.queue.order.delivery.lastmile.changerequest;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.delivery.mdbapp.components.logging.json.LomEventsSuccessLogger;
import ru.yandex.market.delivery.mdbapp.configuration.FeatureProperties;
import ru.yandex.market.delivery.mdbapp.integration.service.LomOrderEventService;
import ru.yandex.market.delivery.mdbapp.integration.service.OrderErrorService;
import ru.yandex.market.logistics.lom.model.dto.EventDto;
import ru.yandex.market.logistics.lom.model.enums.EntityType;
import ru.yandex.money.common.dbqueue.api.EnqueueParams;
import ru.yandex.money.common.dbqueue.api.QueueProducer;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class LastMileChangeRequestQueueProducerTest {

    private static final long LOM_ORDER_ID = 12345L;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private QueueProducer<LastMileChangeRequestDto> lastMileChangeRequestDtoQueueProducer;

    @Mock
    private QueueProducer<ChangeLastMileToCourierRequestDto> changeLastMileToCourierRequestDtoQueueProducer;

    @Mock
    private QueueProducer<ChangeLastMileToPickupRequestDto> changeLastMileToPickupRequestDtoQueueProducer;

    @Mock
    private QueueProducer<ChangeLastMileFromPickupToPickupRequestDto>
        changeLastMileFromPickupToPickupRequestDtoQueueProducer;

    @Mock
    private LomEventsSuccessLogger lomEventsSuccessLogger;

    @Mock
    private OrderErrorService orderErrorService;

    private LomOrderEventService lomOrderEventService;

    @BeforeEach
    void setUp() {
        lomOrderEventService = new LomOrderEventService(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            new LastMileChangeRequestEnqueueService(lastMileChangeRequestDtoQueueProducer),
            new ChangeLastMileToCourierRequestEnqueueService(changeLastMileToCourierRequestDtoQueueProducer),
            new ChangeLastMileToPickupRequestEnqueueService(changeLastMileToPickupRequestDtoQueueProducer),
            new ChangeLastMileFromPickupToPickupRequestEnqueueService(
                changeLastMileFromPickupToPickupRequestDtoQueueProducer
            ),
            null,
            null,
            objectMapper,
            orderErrorService,
            null,
            null,
            lomEventsSuccessLogger,
            null,
            null,
            new FeatureProperties(),
            null
        );
    }

    @Test
    public void lastMileRequestQueueProcessing() {
        EventDto eventDto = createEventDto("/last_mile_diff.json", "/last_mile_snapshot.json");
        lomOrderEventService.processEvent(eventDto);
        verify(lomEventsSuccessLogger).logEvent(eventDto);

        EnqueueParams<LastMileChangeRequestDto> enqueueParams = EnqueueParams.create(
            LastMileChangeRequestDto.builder()
                .changeRequestId(1L)
                .lomOrderId(LOM_ORDER_ID)
                .build()
        );

        verify(lastMileChangeRequestDtoQueueProducer).enqueue(enqueueParams);
    }

    @Test
    public void changeLastMileToPickupRequestQueueProcessing() {
        EventDto eventDto = createEventDto(
            "/change_last_mile_to_pickup_diff.json",
            "/change_last_mile_to_pickup_snapshot.json");
        lomOrderEventService.processEvent(eventDto);
        verify(lomEventsSuccessLogger).logEvent(eventDto);

        EnqueueParams<ChangeLastMileToPickupRequestDto> enqueueParams = EnqueueParams.create(
            ChangeLastMileToPickupRequestDto.builder()
                .changeRequestId(1L)
                .lomOrderId(LOM_ORDER_ID)
                .build()
        );

        verify(changeLastMileToPickupRequestDtoQueueProducer).enqueue(enqueueParams);
    }

    @Nonnull
    @SneakyThrows
    private EventDto createEventDto(String diffPath, String snapshotPath) {
        String diffJson = IOUtils.toString(
            Objects.requireNonNull(this.getClass().getResourceAsStream(diffPath)),
            StandardCharsets.UTF_8
        );
        ArrayNode diff = objectMapper.readValue(
            diffJson,
            ArrayNode.class
        );

        String snapshotJson = IOUtils.toString(
            Objects.requireNonNull(this.getClass().getResourceAsStream(snapshotPath)),
            StandardCharsets.UTF_8
        );
        JsonNode snapshot = objectMapper.readValue(
            snapshotJson,
            JsonNode.class
        );
        return new EventDto()
            .setEntityId(LOM_ORDER_ID)
            .setEntityType(EntityType.ORDER)
            .setDiff(diff)
            .setSnapshot(snapshot);
    }
}
