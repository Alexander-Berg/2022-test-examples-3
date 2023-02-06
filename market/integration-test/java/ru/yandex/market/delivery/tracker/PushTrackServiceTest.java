package ru.yandex.market.delivery.tracker;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.tracker.client.pushing.ConsumersNotificationClient;
import ru.yandex.market.delivery.tracker.client.pushing.PushTrackException;
import ru.yandex.market.delivery.tracker.domain.entity.DeliveryTrack;
import ru.yandex.market.delivery.tracker.domain.entity.NotificationResult;
import ru.yandex.market.delivery.tracker.domain.entity.NotificationResultStatus;
import ru.yandex.market.delivery.tracker.domain.entity.NotificationResults;
import ru.yandex.market.delivery.tracker.service.logger.data.track_events.TrackEvent;
import ru.yandex.market.delivery.tracker.service.logger.data.track_events.TrackEventLogData;
import ru.yandex.market.delivery.tracker.service.pushing.PushTrackService;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PushTrackServiceTest extends AbstractContextualTest {

    @Autowired
    private ConsumersNotificationClient consumersNotificationClient;

    @Autowired
    protected PushTrackService pushService;

    @BeforeEach
    public void setup() {
        reset(consumersNotificationClient);
        when(consumersNotificationClient.push(anyList()))
            .thenReturn(wrapResults(
                new NotificationResult().setStatus(NotificationResultStatus.OK).setTrackerId(1)
            ));
    }

    /**
     * Трек с незаполненой датой последнего успешного пуша будет запушен,
     * LAST_NOTIFY_SUCCESS_TS у трека и чекпоинта изменяется.
     */
    @Test
    @DatabaseSetup("/database/states/single_track_with_checkpoint.xml")
    @ExpectedDatabase(
        value = "/database/expected/push/single_track_with_checkpoint_pushed.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void pushTrackWithCheckpointsFirstTime() {
        push();
        verify(consumersNotificationClient, atLeastOnce()).push(anyList());

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verifyLogging(captor, 2);

        List<Object> captured = captor.getAllValues();

        TrackEventLogData actualTrackEventLogData = (TrackEventLogData) captured.get(0);

        assertEquals(
            "Check logged eventType",
            TrackEvent.CONSUMER_NOTIFIED.readableName(),
            actualTrackEventLogData.getEventType()
        );
    }

    /**
     * После сфейленной попытки пуша дата LAST_NOTIFY_SUCCESS_TS не изменится ни у трека, ни у чекпоинта.
     */
    @Test
    @DatabaseSetup("/database/states/single_track_with_checkpoint.xml")
    @ExpectedDatabase(
        value = "/database/expected/push/single_track_with_checkpoint_push_failed.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void lastNotifySuccessNotChangedAfterPushingFail() {
        when(consumersNotificationClient.push(anyList()))
            .thenThrow(new PushTrackException());

        try {
            push();
        } catch (PushTrackException exc) {
            // перехватываем исключение, чтобы проверить валидность ассертов после
        }

        verify(consumersNotificationClient, atLeastOnce()).push(anyList());
    }

    /**
     * Если чекаутер ответил ERROR статусом на пуш мы не обновляем LAST_NOTIFY_SUCCESS_TS ни у трека, ни у чекпонита,
     * но изымаем трек из очереди.
     */
    @Test
    @DatabaseSetup("/database/states/single_track_with_checkpoint.xml")
    @ExpectedDatabase(
        value = "/database/expected/push/single_track_with_checkpoint_push_failed.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void lastNotifySuccessNotChangedAfterCheckouterError() {
        when(consumersNotificationClient.push(anyList()))
            .thenReturn(wrapResults(
                new NotificationResult()
                    .setStatus(NotificationResultStatus.ERROR)
                    .setTrackerId(1)
                    .setMessage("Message text")
            ));

        try {
            push();
        } catch (PushTrackException exc) {
            // перехватываем исключение, чтобы проверить валидность ассертов после
        }

        verify(consumersNotificationClient, atLeastOnce()).push(anyList());
    }

    /**
     * Трек пушится если после последнего пуша у него появился новый чекпоинт.
     */
    @Test
    @DatabaseSetup("/database/states/single_track_with_new_checkpoint.xml")
    @ExpectedDatabase(
        value = "/database/expected/push/single_track_with_checkpoint_pushed.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void pushTrackWithNewCheckpointsAfterLastPush() {
        when(consumersNotificationClient.push(anyList()))
            .thenReturn(wrapResults(
                new NotificationResult().setStatus(NotificationResultStatus.IGNORED).setTrackerId(1)
            ));

        push();

        verify(consumersNotificationClient, atLeastOnce()).push(anyList());

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verifyLogging(captor, 2);

        List<Object> captured = captor.getAllValues();

        TrackEventLogData actualTrackEventLogData = (TrackEventLogData) captured.get(0);

        assertEquals(
            "Check logged eventType",
            TrackEvent.CONSUMER_NOTIFICATION_IGNORED.readableName(),
            actualTrackEventLogData.getEventType()
        );
    }

    /**
     * Трек пушится без UNKNOWN чекпоинтов, LAST_NOTIFY_SUCCESS_TS обновляется только у не UNKNOWN чекпоинтов.
     */
    @Test
    @DatabaseSetup("/database/states/single_track_with_new_unknown_checkpoint.xml")
    @ExpectedDatabase(
        value = "/database/expected/push/single_track_pushed_without_unknown_checkpoint.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @SuppressWarnings("unchecked")
    void pushTrackWithoutUnknownCheckpoint() {
        when(consumersNotificationClient.push(anyList()))
            .thenReturn(wrapResults(
                new NotificationResult().setStatus(NotificationResultStatus.IGNORED).setTrackerId(1)
            ));

        push();

        ArgumentCaptor<List> trackListCaptor = ArgumentCaptor.forClass(List.class);

        verify(consumersNotificationClient, atLeastOnce()).push(trackListCaptor.capture());

        List<DeliveryTrack> trackListCapture = trackListCaptor.getValue();
        assertions()
            .assertThat(trackListCapture.size())
            .isEqualTo(1);
        assertions()
            .assertThat(trackListCapture.get(0).getDeliveryTrackCheckpoints().size())
            .isEqualTo(1);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verifyLogging(captor, 2);

        List<Object> captured = captor.getAllValues();

        TrackEventLogData actualTrackEventLogData = (TrackEventLogData) captured.get(0);

        assertEquals(
            "Check logged eventType",
            TrackEvent.CONSUMER_NOTIFICATION_IGNORED.readableName(),
            actualTrackEventLogData.getEventType()
        );
    }

    /**
     * Трек с новым чекпоинтом должен запушиться, новому чекпоинту должны обновить LAST_NOTIFY_SUCCESS_TS,
     * а старому и UNKNOWN чекпоинту LAST_NOTIFY_SUCCESS_TS не перезаписывать.
     */
    @Test
    @DatabaseSetup("/database/states/single_track_with_old_checkpoint.xml")
    @ExpectedDatabase(
        value = "/database/expected/push/single_track_with_old_checkpoint_pushed.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void pushTrackWithOldAndUnknownCheckpoints() {
        push();

        verify(consumersNotificationClient, atLeastOnce()).push(anyList());

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verifyLogging(captor, 4);

        List<Object> captured = captor.getAllValues();

        TrackEventLogData actualTrackEventLogData = (TrackEventLogData) captured.get(0);

        assertEquals(
            "Check logged eventType",
            TrackEvent.CONSUMER_NOTIFIED.readableName(),
            actualTrackEventLogData.getEventType()
        );
    }

    @Test
    @DatabaseSetup("/database/states/single_track_with_pushed_checkpoint.xml")
    @ExpectedDatabase(
        value = "/database/expected/push/single_track_with_pushed_checkpoint.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void pushTrackWhenCheckpointsAlreadyPushed() {
        push();

        verify(consumersNotificationClient, atLeastOnce()).push(anyList());

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verifyLogging(captor, 2);

        List<Object> captured = captor.getAllValues();

        TrackEventLogData actualTrackEventLogData = (TrackEventLogData) captured.get(0);

        assertEquals(
            "Check logged eventType",
            TrackEvent.CONSUMER_NOTIFIED.readableName(),
            actualTrackEventLogData.getEventType()
        );
    }

    @Test
    @DatabaseSetup("/database/states/single_track_with_no_checkpoints.xml")
    @ExpectedDatabase(
        value = "/database/expected/push/single_track_with_no_checkpoints_pushed.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void pushTrackWithNoCheckpoints() {
        push();

        verify(consumersNotificationClient, atLeastOnce()).push(Collections.emptyList());
    }

    private void push() {
        pushService.pushTrack(10L, 1L);
    }

    private NotificationResults wrapResults(NotificationResult... notificationResult) {
        return NotificationResults.of(Arrays.asList(
            notificationResult
        ));
    }

}
