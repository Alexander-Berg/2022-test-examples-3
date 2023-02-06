package ru.yandex.market.delivery.tracker;

import java.time.Clock;
import java.time.Period;
import java.util.Arrays;
import java.util.stream.Collectors;

import javax.jms.JMSException;

import com.amazon.sqs.javamessaging.SQSQueueDestination;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.tracker.configuration.properties.OtherFeaturesProperties;
import ru.yandex.market.delivery.tracker.exception.EmptyPayloadException;
import ru.yandex.market.delivery.tracker.exception.SqsEventProcessingException;
import ru.yandex.market.delivery.tracker.service.pushing.PushCheckpointLesQueueProducer;
import ru.yandex.market.delivery.tracker.service.pushing.PushTrackQueueProducer;
import ru.yandex.market.delivery.tracker.service.sqs.les.LesEventConsumer;
import ru.yandex.market.delivery.tracker.service.tracking.CheckpointsProcessingService;
import ru.yandex.market.logistics.les.OrderNewCheckpointEvent;
import ru.yandex.market.logistics.les.base.Event;
import ru.yandex.market.logistics.les.base.crypto.EncryptedString;
import ru.yandex.market.logistics.les.tracker.enums.ApiVersion;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static ru.yandex.market.delivery.tracker.service.sqs.les.LesSaveNewCheckpointProcessor.PAYLOAD_IS_NOT_VALID;
import static ru.yandex.market.delivery.tracker.service.sqs.les.LesSaveNewCheckpointProcessor.SAVING_CHECKPOINTS_EXCEPTION;
import static ru.yandex.market.delivery.tracker.service.sqs.les.LesSaveNewCheckpointProcessor.SERVICES_NOT_FOUND;

public class LesEventConsumerTest extends AbstractContextualTest {

    public static final String MESSAGE_ID = "messageId";
    public static final String REQUEST_ID = "requestId";
    public static final String SOURCE = "source";
    public static final String EVENT_ID = "eventId";
    public static final String ORDER_NEW_CHECKPOINT_EVENT_TYPE = "ORDER_NEW_CHECKPOINT";
    public static final String DESCRIPTION = "description";

    public static final String TOKEN_1 = "token_1";
    public static final String TOKEN_2 = "token_2";
    public static final String ORDER_ID = "ORDER_1";
    public static final String TRACK_CODE_1 = "TRACK_CODE_1";
    public static final String TRACK_CODE_2 = "TRACK_CODE_2";
    public static final ApiVersion API_VERSION = ApiVersion.DS;
    public static final Integer NEW_STATUS = 49;
    public static final String MESSAGE = "msg";

    public static final String VALIDATION_ERROR_MESSAGE = "trackCode: must not be null,"
        + " checkpointDate: must not be null,"
        + " status: must be greater than 0,"
        + " apiVersion: must not be null";

    @Autowired
    private LesEventConsumer consumer;

    @Autowired
    private PushTrackQueueProducer pushTrackQueueProducer;

    @Autowired
    private PushCheckpointLesQueueProducer pushCheckpointLesQueueProducer;

    @Autowired
    private CheckpointsProcessingService checkpointsProcessingService;

    @Autowired
    private OtherFeaturesProperties otherFeaturesProperties;

    @Autowired
    private Clock clock;

    private final SQSQueueDestination queueDestination = mock(SQSQueueDestination.class);

    private Long timestamp;

    @BeforeEach
    void setUp() {
        doNothing().when(pushTrackQueueProducer).enqueue(anyLong());
        doNothing().when(pushCheckpointLesQueueProducer).enqueue(anyLong(), anyList());
        timestamp = clock.millis();
        otherFeaturesProperties.setShouldRaiseExceptionDuringCheckpointSaving(false);
    }

    @Test
    void emptyPayloadThrows() {
        assertions().assertThatThrownBy(() ->
            processEvent(buildEmptyEvent())
        ).isInstanceOf(EmptyPayloadException.class);
    }

    @Test
    void noDeliveryServicesThrows() {
        otherFeaturesProperties.setShouldRaiseExceptionDuringCheckpointSaving(true);
        assertions().assertThatThrownBy(() ->
            processEvent(buildRealEvent(TRACK_CODE_1, TOKEN_1))
        ).isInstanceOf(SqsEventProcessingException.class)
            .hasMessage(SERVICES_NOT_FOUND);
    }

    @Test
    void noDeliveryServicesThrowsWithEnabledFlag() {

        assertions().assertThatCode(() ->
            processEvent(buildRealEvent(TRACK_CODE_1, TOKEN_1))
        ).doesNotThrowAnyException();
    }

    @Test
    @DatabaseSetup("/database/states/les_integration/delivery_services.xml")
    void invalidPayloadThrows() {
        assertions().assertThatThrownBy(() ->
            processEvent(buildInvalidPayloadEvent())
        ).isInstanceOf(SqsEventProcessingException.class)
            .matches(ex -> checkExceptionValidationMessage(ex.getMessage()));
    }

    @Test
    @DatabaseSetup({
        "/database/states/les_integration/delivery_services.xml",
        "/database/states/les_integration/tracks.xml",
    })
    @ExpectedDatabase(
        value = "/database/expected/les_integration/single_checkpoint_saved.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void successfulSave() throws JMSException {
        processEvent(buildRealEvent(TRACK_CODE_1, TOKEN_1));
    }

    @Test
    @DatabaseSetup({
        "/database/states/les_integration/delivery_services.xml",
        "/database/states/les_integration/tracks.xml",
    })
    @ExpectedDatabase(
        value = "/database/expected/les_integration/two_checkpoints_saved.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void successfulSaveTwoTracks() throws JMSException {
        processEvent(buildRealEvent(TRACK_CODE_2, TOKEN_2));
    }

    @Test
    @DatabaseSetup({
        "/database/states/les_integration/delivery_services.xml",
        "/database/states/les_integration/tracks.xml",
    })
    @ExpectedDatabase(
        value = "/database/expected/les_integration/no_checkpoints_saved.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void saveNoTracksAfterException() {
        doThrow(new IllegalArgumentException("Illegal argument"))
            .when(checkpointsProcessingService)
            .createNewCheckPointsIfAchieved(argThat(i -> i.getDeliveryServiceId() == 303), any(), any(), any());

        assertions().assertThatThrownBy(() ->
            processEvent(buildRealEvent(TRACK_CODE_2, TOKEN_2))
        ).isInstanceOf(SqsEventProcessingException.class)
            .hasMessage(SAVING_CHECKPOINTS_EXCEPTION + "java.lang.IllegalArgumentException: Illegal argument");
    }

    @Test
    void testIncorrectEvent() {
        assertions()
            .assertThatThrownBy(() -> processEvent(buildIncorrectEvent()))
            .isInstanceOf(SqsEventProcessingException.class)
            .hasMessageContaining("Payload has to have either token or partner id. Neither was found");
    }

    @Test
    @DatabaseSetup({
        "/database/states/les_integration/delivery_services.xml",
        "/database/states/les_integration/tracks.xml",
    })
    @ExpectedDatabase(
        value = "/database/expected/les_integration/single_checkpoint_saved.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void successfulSaveUsingPartnerId() throws JMSException {
        processEvent(buildRealPartnerIdEvent(TRACK_CODE_1, 101L));
    }

    @Test
    @DatabaseSetup({
        "/database/states/les_integration/delivery_services.xml",
        "/database/states/les_integration/tracks.xml",
    })
    @ExpectedDatabase(
        value = "/database/expected/les_integration/two_checkpoints_saved.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void successfulSaveTwoTracksUsingPartnerId() throws JMSException {
        processEvent(buildRealPartnerIdEvent(TRACK_CODE_2, 202L));
        processEvent(buildRealPartnerIdEvent(TRACK_CODE_2, 303L));
    }

    private void processEvent(Event event) throws JMSException {
        consumer.processEvent(
            queueDestination,
            MESSAGE_ID,
            REQUEST_ID,
            timestamp,
            event
        );
    }

    private Event buildEmptyEvent() {
        return buildLesEvent(null);
    }

    private Event buildIncorrectEvent() {
        return buildLesEvent(
            new OrderNewCheckpointEvent(
                null,
                null,
                ORDER_ID,
                "",
                API_VERSION,
                NEW_STATUS,
                clock.instant().minus(Period.ofDays(2)),
                MESSAGE
            )
        );
    }

    private Event buildInvalidPayloadEvent() {
        return buildLesEvent(new OrderNewCheckpointEvent(
            new EncryptedString(TOKEN_1),
            ORDER_ID,
            null,
            null,
            -1,
            null,
            MESSAGE
        ));
    }

    private Event buildRealEvent(String trackCode, String token) {
        return buildLesEvent(new OrderNewCheckpointEvent(
            new EncryptedString(token),
            ORDER_ID,
            trackCode,
            API_VERSION,
            NEW_STATUS,
            clock.instant().minus(Period.ofDays(2)),
            MESSAGE
        ));
    }

    private Event buildRealPartnerIdEvent(String trackCode, Long partnerId) {
        return buildLesEvent(new OrderNewCheckpointEvent(
            null,
            partnerId,
            ORDER_ID,
            trackCode,
            API_VERSION,
            NEW_STATUS,
            clock.instant().minus(Period.ofDays(2)),
            MESSAGE
        ));
    }

    private Event buildLesEvent(OrderNewCheckpointEvent payload) {
        return new Event(
            SOURCE,
            EVENT_ID,
            timestamp,
            ORDER_NEW_CHECKPOINT_EVENT_TYPE,
            payload,
            DESCRIPTION
        );
    }

    private Boolean checkExceptionValidationMessage(String exMessage) {
        if (!exMessage.startsWith(PAYLOAD_IS_NOT_VALID)) {
            return false;
        }

        return Arrays.stream(VALIDATION_ERROR_MESSAGE.split(", "))
            .collect(Collectors.toSet())
            .containsAll(
                Arrays.stream(exMessage.substring(PAYLOAD_IS_NOT_VALID.length())
                    .split(", "))
                    .collect(Collectors.toSet())
            );
    }
}
