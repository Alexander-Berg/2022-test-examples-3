package ru.yandex.market.delivery.tracker;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.tracker.service.logger.data.track_events.TrackEventLogData;
import ru.yandex.market.delivery.tracker.service.tracking.TrackStatusSyncService;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.delivery.tracker.service.logger.data.track_events.TrackEvent.TRACK_DELETED;

public class TrackStatusSyncServiceTest extends AbstractContextualTest {

    @Autowired
    private Clock clock;

    @Autowired
    private TrackStatusSyncService service;

    @Test
    @DatabaseSetup("/database/states/tracks_with_last_stop_tracking_ts.xml")
    @ExpectedDatabase(value = "/database/expected/deleted_track.xml", assertionMode = NON_STRICT_UNORDERED)
    public void updateAndLogTracksStatuses() {
        service.updateAndLogTracksStatuses();

        ArgumentCaptor<TrackEventLogData> trackEventCaptor = ArgumentCaptor.forClass(TrackEventLogData.class);
        verifyLogging(trackEventCaptor, 2);

        List<TrackEventLogData> actualTrackEventLogDataList = trackEventCaptor.getAllValues();

        assertions().assertThat(
            actualTrackEventLogDataList.stream()
                .map(TrackEventLogData::getTrackCode)
                .collect(Collectors.toSet()))
            .containsExactly("TRACK_CODE_1", "TRACK_CODE_2");

        assertions().assertThat(
            actualTrackEventLogDataList.stream()
                .map(TrackEventLogData::getEventType)
                .collect(Collectors.toSet()))
            .containsExactly(TRACK_DELETED.readableName());
    }

    @Test
    void startTracking_InvalidDuration() {
        List<Long> ids = List.of();
        Instant oneYear = clock.instant().plus(365, ChronoUnit.DAYS);

        assertThrows(IllegalArgumentException.class, () -> service.startTracking(ids, oneYear));
    }
}
