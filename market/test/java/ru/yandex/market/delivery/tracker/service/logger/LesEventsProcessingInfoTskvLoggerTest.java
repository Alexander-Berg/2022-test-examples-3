package ru.yandex.market.delivery.tracker.service.logger;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import com.amazon.sqs.javamessaging.SQSQueueDestination;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.JUnitJupiterSoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import ru.yandex.market.delivery.tracker.configuration.properties.OtherFeaturesProperties;
import ru.yandex.market.delivery.tracker.dao.repository.DeliveryServiceRepository;
import ru.yandex.market.delivery.tracker.domain.dto.LesNewCheckpointEvent;
import ru.yandex.market.delivery.tracker.domain.dto.LesSqsMessageMetaInfo;
import ru.yandex.market.delivery.tracker.domain.entity.DeliveryService;
import ru.yandex.market.delivery.tracker.domain.entity.DeliveryTrackMeta;
import ru.yandex.market.delivery.tracker.domain.enums.ApiVersion;
import ru.yandex.market.delivery.tracker.domain.enums.DeliveryServiceType;
import ru.yandex.market.delivery.tracker.domain.enums.EntityType;
import ru.yandex.market.delivery.tracker.service.sqs.les.LesSaveNewCheckpointProcessor;
import ru.yandex.market.delivery.tracker.service.tracking.CheckpointsProcessingService;
import ru.yandex.market.delivery.tracker.service.tracking.DeliveryTrackService;
import ru.yandex.market.delivery.tracker.util.ValidationUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.delivery.tracker.service.sqs.les.LesSaveNewCheckpointProcessor.PAYLOAD_IS_NOT_VALID;
import static ru.yandex.market.delivery.tracker.service.sqs.les.LesSaveNewCheckpointProcessor.SAVING_CHECKPOINTS_EXCEPTION;
import static ru.yandex.market.delivery.tracker.service.sqs.les.LesSaveNewCheckpointProcessor.SERVICES_NOT_FOUND;

public class LesEventsProcessingInfoTskvLoggerTest {

    @RegisterExtension
    final JUnitJupiterSoftAssertions softly = new JUnitJupiterSoftAssertions();

    @Mock
    private TskvLogger tskvLogger;

    @Mock
    private DeliveryServiceRepository deliveryServiceRepository;

    @Mock
    private DeliveryTrackService deliveryTrackService;

    @Mock
    private OtherFeaturesProperties otherProperties;

    @Mock
    private CheckpointsProcessingService checkpointsProcessingService;

    @Mock
    private PullSqsEventsTskvLogger pullSqsEventsTskvLogger;

    private LesSaveNewCheckpointProcessor lesSaveNewCheckpointProcessor;

    private final Clock clock = Clock.systemUTC();

    @Captor
    ArgumentCaptor<Map<String, String>> mapCaptor;

    private static final String QUEUE_DESTINATION = "queueDestination";
    private static final String MESSAGE_ID = "messageId";
    private static final String LOG_TYPE = "logType";
    private static final String PROCESSING_MESSAGE = "processingMessage";
    private static final String PAYLOAD = "payload";
    private static final String EVENT_TYPE = "eventType";
    private static final String EVENT_SOURCE = "eventSource";
    private static final String SERVICE_TOKEN = "serviceToken";

    private final SQSQueueDestination destination = null;
    private final String messageId = "1000";
    private final String trackCode = "111";
    private final String entityId = "1";
    private final List<DeliveryService> services = List.of(
        new DeliveryService(1, "ds1", DeliveryServiceType.FULFILLMENT)
    );

    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json()
        .simpleDateFormat("yyyy-MM-dd HH:mm:ss")
        .serializationInclusion(JsonInclude.Include.ALWAYS)
        .propertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
        .build();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        LesEventsProcessingInfoTskvLogger lesEventsProcessingInfoTskvLogger =
            new LesEventsProcessingInfoTskvLogger(tskvLogger);
        lesSaveNewCheckpointProcessor = new LesSaveNewCheckpointProcessor(
            otherProperties,
            deliveryTrackService,
            deliveryServiceRepository,
            checkpointsProcessingService,
            lesEventsProcessingInfoTskvLogger,
            pullSqsEventsTskvLogger,
            clock
        );
    }

    @Test
    public void testNoDeliveryServices() throws JsonProcessingException {
        LesNewCheckpointEvent checkpointEvent = createCheckpointEvent();

        when(deliveryServiceRepository.getByToken(checkpointEvent.getToken())).thenReturn(List.of());

        softly.assertThatCode(
            () -> lesSaveNewCheckpointProcessor.saveNewCheckpoint(
                checkpointEvent,
                LesSqsMessageMetaInfo.builder()
                    .messageId(messageId)
                    .eventType("ORDER_NEW_CHECKPOINT")
                    .eventSource("sc")
                    .build()
            )
        ).doesNotThrowAnyException();

        Map<String, String> expectedMap = ImmutableMap.<String, String>builder()
            .put(QUEUE_DESTINATION, "")
            .put(MESSAGE_ID, messageId)
            .put(LOG_TYPE, "INFO")
            .put(PROCESSING_MESSAGE, SERVICES_NOT_FOUND)
            .put(PAYLOAD, objectMapper.writeValueAsString(checkpointEvent))
            .put(EVENT_TYPE, "ORDER_NEW_CHECKPOINT")
            .put(EVENT_SOURCE, "sc")
            .put(SERVICE_TOKEN, "token")
            .build();

        verify(tskvLogger).log(mapCaptor.capture());
        verifyMapsAreEqual(mapCaptor.getValue(), expectedMap);
    }

    @Test
    public void testNoDeliveryServicesWithEnabledFlag() throws JsonProcessingException {
        LesNewCheckpointEvent checkpointEvent = createCheckpointEvent();

        when(deliveryServiceRepository.getByToken(checkpointEvent.getToken())).thenReturn(List.of());
        when(otherProperties.shouldRaiseExceptionDuringCheckpointSaving()).thenReturn(true);

        softly.assertThatThrownBy(
            () -> lesSaveNewCheckpointProcessor.saveNewCheckpoint(
                checkpointEvent,
                LesSqsMessageMetaInfo.builder()
                    .messageId(messageId)
                    .eventType("ORDER_NEW_CHECKPOINT")
                    .eventSource("sc")
                    .build()
            )
        );

        Map<String, String> expectedMap = ImmutableMap.<String, String>builder()
            .put(QUEUE_DESTINATION, "")
            .put(MESSAGE_ID, messageId)
            .put(LOG_TYPE, "ERROR")
            .put(PROCESSING_MESSAGE, SERVICES_NOT_FOUND)
            .put(PAYLOAD, objectMapper.writeValueAsString(checkpointEvent))
            .put(EVENT_TYPE, "ORDER_NEW_CHECKPOINT")
            .put(EVENT_SOURCE, "sc")
            .put(SERVICE_TOKEN, "token")
            .build();

        verify(tskvLogger).log(mapCaptor.capture());
        verifyMapsAreEqual(mapCaptor.getValue(), expectedMap);
    }

    @Test
    public void testPayloadIsNotValid() throws JsonProcessingException {
        LesNewCheckpointEvent invalidEvent = createInvalidCheckpointEvent();

        when(deliveryServiceRepository.getByToken(invalidEvent.getToken())).thenReturn(services);

        softly.assertThatThrownBy(
            () -> lesSaveNewCheckpointProcessor.saveNewCheckpoint(
                invalidEvent,
                LesSqsMessageMetaInfo.builder()
                    .messageId(messageId)
                    .eventType("ORDER_NEW_CHECKPOINT")
                    .eventSource("sc")
                    .build()
            )
        );

        Map<String, String> expectedMap = ImmutableMap.<String, String>builder()
            .put(QUEUE_DESTINATION, "")
            .put(MESSAGE_ID, messageId)
            .put(LOG_TYPE, "ERROR")
            .put(PROCESSING_MESSAGE, PAYLOAD_IS_NOT_VALID + ValidationUtils.validateObject(invalidEvent))
            .put(PAYLOAD, objectMapper.writeValueAsString(invalidEvent))
            .put(EVENT_TYPE, "ORDER_NEW_CHECKPOINT")
            .put(EVENT_SOURCE, "sc")
            .build();

        verify(tskvLogger).log(mapCaptor.capture());
        verifyMapsAreEqual(mapCaptor.getValue(), expectedMap);
    }

    @Test
    public void testCheckpointProcessingError() throws JsonProcessingException {
        LesNewCheckpointEvent checkpointEvent = createCheckpointEvent();

        when(deliveryServiceRepository.getByToken(checkpointEvent.getToken())).thenReturn(services);

        when(
            deliveryTrackService.getDeliveryTrackMetas(
                services,
                checkpointEvent.getTrackCode(),
                checkpointEvent.getEntityType(),
                checkpointEvent.getApiVersion()
            )
        )
            .thenReturn(List.of(createDeliveryTrackMeta()));

        IllegalArgumentException e = new IllegalArgumentException("Some exception");
        Mockito.doThrow(e)
            .when(checkpointsProcessingService).createNewCheckPointsIfAchieved(any(), any(), any(), any());

        softly.assertThatThrownBy(
            () -> lesSaveNewCheckpointProcessor.saveNewCheckpoint(
                checkpointEvent,
                LesSqsMessageMetaInfo.builder()
                    .messageId(messageId)
                    .eventType("ORDER_NEW_CHECKPOINT")
                    .eventSource("sc")
                    .build()
            )
        );

        Map<String, String> expectedMap = ImmutableMap.<String, String>builder()
            .put(QUEUE_DESTINATION, "")
            .put(MESSAGE_ID, messageId)
            .put(LOG_TYPE, "ERROR")
            .put(PROCESSING_MESSAGE, SAVING_CHECKPOINTS_EXCEPTION + e)
            .put(PAYLOAD, objectMapper.writeValueAsString(checkpointEvent))
            .put(EVENT_TYPE, "ORDER_NEW_CHECKPOINT")
            .put(EVENT_SOURCE, "sc")
            .build();

        verify(tskvLogger).log(mapCaptor.capture());
        verifyMapsAreEqual(mapCaptor.getValue(), expectedMap);
    }

    @Test
    public void testNoTracksLogToTskv() throws JsonProcessingException {
        LesNewCheckpointEvent checkpointEvent = createCheckpointEvent();

        when(deliveryServiceRepository.getByToken(checkpointEvent.getToken())).thenReturn(services);

        when(
            deliveryTrackService.getDeliveryTrackMetas(
                services,
                checkpointEvent.getTrackCode(),
                checkpointEvent.getEntityType(),
                checkpointEvent.getApiVersion()
            )
        )
            .thenReturn(List.of());

        lesSaveNewCheckpointProcessor.saveNewCheckpoint(
            checkpointEvent,
            LesSqsMessageMetaInfo.builder()
                .messageId(messageId)
                .eventType("ORDER_NEW_CHECKPOINT")
                .eventSource("sc")
                .build()
        );

        String noTracksMessage = String.format(
            "No tracks found: services=%s, trackCode=%s, entityType=%s, apiVersion=%s, partnerId=%s",
            services,
            checkpointEvent.getTrackCode(),
            checkpointEvent.getEntityType(),
            checkpointEvent.getApiVersion(),
            checkpointEvent.getPartnerId()
        );

        Map<String, String> expectedMap = ImmutableMap.<String, String>builder()
            .put(QUEUE_DESTINATION, "")
            .put(MESSAGE_ID, messageId)
            .put(LOG_TYPE, "INFO")
            .put(PROCESSING_MESSAGE, noTracksMessage)
            .put(PAYLOAD, objectMapper.writeValueAsString(checkpointEvent))
            .put(EVENT_TYPE, "ORDER_NEW_CHECKPOINT")
            .put(EVENT_SOURCE, "sc")
            .build();

        verify(tskvLogger).log(mapCaptor.capture());
        verifyMapsAreEqual(mapCaptor.getValue(), expectedMap);
    }

    private LesNewCheckpointEvent createCheckpointEvent() {
        return LesNewCheckpointEvent.builder()
            .token("token")
            .entityId(entityId)
            .entityType(EntityType.ORDER)
            .trackCode(trackCode)
            .apiVersion(ApiVersion.DS)
            .status(10)
            .checkpointDate(LocalDateTime.of(2020, 1, 1, 13, 5, 36))
            .build();
    }

    private LesNewCheckpointEvent createInvalidCheckpointEvent() {
        return LesNewCheckpointEvent.builder()
            .status(-10)
            .build();
    }

    private DeliveryTrackMeta createDeliveryTrackMeta() {
        return new DeliveryTrackMeta(
            trackCode,
            services.get(0),
            entityId,
            EntityType.ORDER,
            null
        );
    }

    private void verifyMapsAreEqual(Map<String, String> actualMap, Map<String, String> expectedMap) {
        softly.assertThat(actualMap).hasSameSizeAs(expectedMap);
        actualMap.forEach(checkActualMapEntriesFunction(expectedMap));
    }

    private BiConsumer<String, String> checkActualMapEntriesFunction(Map<String, String> expectedMap) {
        return (k, v) -> softly.assertThat(v)
            .withFailMessage("Expecting key %s value: %s, but was: %s", k, expectedMap.get(k), v)
            .isEqualTo(expectedMap.get(k));
    }
}
