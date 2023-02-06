package ru.yandex.market.delivery.transport_manager.converter.tracker;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.transport_manager.dto.tracker.NotificationResults;
import ru.yandex.market.delivery.transport_manager.dto.tracker.NotificationResults.NotificationResult;
import ru.yandex.market.delivery.transport_manager.dto.tracker.Track;
import ru.yandex.market.delivery.transport_manager.dto.tracker.TrackMeta;
import ru.yandex.market.delivery.transport_manager.dto.tracker.Tracks;

import static org.assertj.core.api.Assertions.assertThat;

class TrackerCallbackConverterTest {

    private final TrackerCallbackConverter converter = new TrackerCallbackConverter();

    @Test
    void testEmptyTracks() {
        assertThat(converter.convertAllOk(emptyTracks())).isEqualTo(emptyResults());
    }

    @Test
    void testAllOk() {
        assertThat(converter.convertAllOk(tracks())).isEqualTo(results());
    }

    private Tracks emptyTracks() {
        return new Tracks();
    }

    private Tracks tracks() {
        return new Tracks(
            List.of(
                new Track().setDeliveryTrackMeta(new TrackMeta().setId(1L)),
                new Track().setDeliveryTrackMeta(new TrackMeta().setId(2L))
            )
        );
    }

    private NotificationResults emptyResults() {
        return NotificationResults.of(Collections.emptyList());
    }

    private NotificationResults results() {
        return NotificationResults.of(
            List.of(
                new NotificationResult(1L, NotificationResults.NotificationResultStatus.OK),
                new NotificationResult(2L, NotificationResults.NotificationResultStatus.OK)
            )
        );
    }
}
