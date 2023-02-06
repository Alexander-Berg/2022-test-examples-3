package ru.yandex.market.delivery.tracker.service.logger;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import com.amazon.sqs.javamessaging.SQSQueueDestination;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.delivery.tracker.domain.dto.LesNewCheckpointEvent;
import ru.yandex.market.delivery.tracker.domain.dto.LesSqsMessageMetaInfo;
import ru.yandex.market.delivery.tracker.domain.entity.DeliveryService;
import ru.yandex.market.delivery.tracker.domain.enums.ApiVersion;
import ru.yandex.market.delivery.tracker.domain.enums.DeliveryServiceRole;
import ru.yandex.market.delivery.tracker.domain.enums.DeliveryServiceType;
import ru.yandex.market.delivery.tracker.domain.enums.EntityType;
import ru.yandex.market.delivery.tracker.util.DateTimeUtils;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.delivery.tracker.service.logger.TskvLogger.DEFAULT_DATE_TIME_PATTERN;
import static ru.yandex.market.delivery.tracker.service.logger.TskvLogger.MOSCOW_TIME_ZONE;

public class PullSqsEventsTskvLoggerTest {
    @Mock
    private TskvLogger tskvLogger;

    @InjectMocks
    private PullSqsEventsTskvLogger pullSqsEventsTskvLogger;

    private final Clock clock = Clock.fixed(Instant.parse("2021-01-01T12:00:00Z"), ZoneId.of("UTC"));

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        pullSqsEventsTskvLogger = new PullSqsEventsTskvLogger(tskvLogger, clock);
    }

    @Test
    void log() {
        SQSQueueDestination destination = null;
        String messageId = "1111";
        String requestId = "222/123";
        Long eventProducedToSqsTimestamp = clock.instant().minus(1, ChronoUnit.MINUTES).toEpochMilli();
        Long eventCreatedTimestamp = clock.instant().minus(2, ChronoUnit.MINUTES).toEpochMilli();
        String eventDescription = "some description";

        String source = "sourceName";
        String eventId = "10";
        String eventType = "newOrderCheckpointEvent";
        String yandexEntityId = "1";
        String partnerEntityId = "900";
        Integer apiVersion = 2;
        Integer checkpointStatus = 40;
        LocalDateTime checkpointDate = LocalDateTime.ofInstant(
            clock.instant().minus(3, ChronoUnit.MINUTES),
            clock.getZone()
        );
        String message = "Checkpoint 40";

        LesSqsMessageMetaInfo metaInfo = LesSqsMessageMetaInfo.builder()
            .queueDestination(destination)
            .messageId(messageId)
            .requestId(requestId)
            .lesSentTimestamp(eventProducedToSqsTimestamp)
            .eventId(eventId)
            .eventSource(source)
            .eventType(eventType)
            .sourceSentTimestamp(eventCreatedTimestamp)
            .build();

        LesNewCheckpointEvent checkpointEvent = LesNewCheckpointEvent.builder()
            .token(null)
            .entityId(yandexEntityId)
            .entityType(EntityType.ORDER)
            .trackCode(partnerEntityId)
            .apiVersion(ApiVersion.byVersionSafe(apiVersion))
            .status(checkpointStatus)
            .checkpointDate(checkpointDate)
            .message(message)
            .build();

        List<DeliveryService> services = List.of(createDeliveryService(1L), createDeliveryService(512L));

        LocalDateTime time1 = DateTimeUtils.convertToLocalDateTime(clock.instant(), clock.getZone());
        LocalDateTime time2 = DateTimeUtils.convertToLocalDateTime(
            clock.instant().minus(1, ChronoUnit.MINUTES),
            clock.getZone()
        );
        LocalDateTime time3 = DateTimeUtils.convertToLocalDateTime(
            clock.instant().minus(2, ChronoUnit.MINUTES),
            clock.getZone()
        );
        LocalDateTime time4 = DateTimeUtils.convertToLocalDateTime(
            clock.instant().minus(3, ChronoUnit.MINUTES),
            clock.getZone()
        );

        when(tskvLogger.formatDate(time1, clock.getZone()))
            .thenReturn(formatLocalDateTime(time1, clock.getZone()));
        when(tskvLogger.formatDate(time2, clock.getZone()))
            .thenReturn(formatLocalDateTime(time2, clock.getZone()));
        when(tskvLogger.formatDate(time3, clock.getZone()))
            .thenReturn(formatLocalDateTime(time3, clock.getZone()));
        when(tskvLogger.formatDate(time4, clock.getZone()))
            .thenReturn(formatLocalDateTime(time4, clock.getZone()));

        pullSqsEventsTskvLogger.logNewCheckpointFromSqs(
            metaInfo,
            checkpointEvent,
            services,
            DeliveryServiceRole.SC,
            EntityType.ORDER
        );

        verify(tskvLogger).log(
            eq(
                ImmutableMap.<String, String>builder()
                    .put("queueDestination", "")
                    .put("messageId", messageId)
                    .put("requestId", requestId)
                    .put("eventProducedDate", "2021-01-01 14:59:00.000+0300")
                    .put("eventConsumedDate", "2021-01-01 15:00:00.000+0300")
                    .put("eventCreatedDate", "2021-01-01 14:58:00.000+0300")
                    .put("eventId", eventId)
                    .put("eventType", eventType)
                    .put("entityType", "ORDER")
                    .put("eventSource", source)
                    .put("yandexEntityId", yandexEntityId)
                    .put("partnerEntityId", partnerEntityId)
                    .put("deliveryServices", "1,512")
                    .put("deliveryServiceRole", "SC")
                    .put("partnerApiVersion", "DS")
                    .put("checkpointStatus", "40")
                    .put("checkpointDate", "2021-01-01 14:57:00.000+0300")
                    .build()
            )
        );
    }

    private DeliveryService createDeliveryService(Long id) {
        return new DeliveryService(
            id,
            "ds",
            DeliveryServiceType.DELIVERY,
            0
        );
    }

    private String formatLocalDateTime(LocalDateTime date, ZoneId zoneId) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DEFAULT_DATE_TIME_PATTERN).withZone(MOSCOW_TIME_ZONE);
        return Optional.ofNullable(date)
            .map(dt -> formatter.format(dt.atZone(zoneId)))
            .orElse("");
    }
}
