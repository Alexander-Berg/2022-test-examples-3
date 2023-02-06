package ru.yandex.market.delivery.tracker;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.apache.commons.lang.time.DateFormatUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.tracker.dao.repository.DeliveryTrackDao;
import ru.yandex.market.delivery.tracker.domain.entity.DeliveryTrackMeta;
import ru.yandex.market.delivery.tracker.domain.enums.RequestType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class UpdateTrackNextRequestTsTest extends AbstractContextualTest {

    public static final int DEFAULT_INTERVAL_VALUE = 15;
    @Autowired
    DeliveryTrackDao deliveryTrackDao;

    @Test
    @DatabaseSetup("/database/states/tracks_to_refresh_last_updated_and_new_request_ts.xml")
    public void refreshTracksLastUpdatedAndNextRequestTS() {
        deliveryTrackDao.refreshTracksLastUpdatedAndNextRequestTs(
            getTrackIds(),
            List.of(1, 12),
            RequestType.ORDER_HISTORY,
            DEFAULT_INTERVAL_VALUE
        );

        final DeliveryTrackMeta deliveryTrackMeta = deliveryTrackDao.getDeliveryTrackMeta(1L);
        final DeliveryTrackMeta deliveryTrackMeta2 = deliveryTrackDao.getDeliveryTrackMeta(2L);

        //factor = 1, interval = 15, next_request = 1 x 15 (15)
        assertEquals(15, getDiffBetweenUpdateAndNextRequestTime(deliveryTrackMeta));

        //factor = 12, interval = 3, next_request = 12 x 3 (36)
        assertEquals(36, getDiffBetweenUpdateAndNextRequestTime(deliveryTrackMeta2));

    }

    @Test
    @DatabaseSetup("/database/states/tracks_to_refresh_ts_without_intervals.xml")
    public void refreshTracksWithoutIntervals() {
        deliveryTrackDao.refreshTracksLastUpdatedAndNextRequestTs(
            getTrackIds(),
            List.of(1, 12),
            RequestType.ORDER_HISTORY,
            DEFAULT_INTERVAL_VALUE
        );

        final DeliveryTrackMeta deliveryTrackMeta = deliveryTrackDao.getDeliveryTrackMeta(1L);
        final DeliveryTrackMeta deliveryTrackMeta2 = deliveryTrackDao.getDeliveryTrackMeta(2L);

        //factor = 1, interval = 15(default), next_request = 1 x 15 (15)
        assertEquals(15, getDiffBetweenUpdateAndNextRequestTime(deliveryTrackMeta));

        //factor = 12 interval = 15(default), next_request = 12 x 15 (180)
        assertEquals(180, getDiffBetweenUpdateAndNextRequestTime(deliveryTrackMeta2));

    }

    @Test
    @DatabaseSetup("/database/states/tracks_to_refresh_with_overlimit_interval.xml")
    public void refreshTracksWithOverlimitInterval() {
        deliveryTrackDao.refreshTracksLastUpdatedAndNextRequestTs(
            getTrackIds(),
            List.of(1, 100),
            RequestType.ORDER_HISTORY,
            DEFAULT_INTERVAL_VALUE
        );

        final DeliveryTrackMeta deliveryTrackMeta = deliveryTrackDao.getDeliveryTrackMeta(1L);
        final DeliveryTrackMeta deliveryTrackMeta2 = deliveryTrackDao.getDeliveryTrackMeta(2L);

        //factor = 1, interval = 300, next_request = 1 x 300 (300)
        assertEquals(300, getDiffBetweenUpdateAndNextRequestTime(deliveryTrackMeta));

        //factor = 100, interval = 3, next_request = 1 x 300 (300)
        assertEquals(300, getDiffBetweenUpdateAndNextRequestTime(deliveryTrackMeta2));

    }

    @Test
    @DatabaseSetup("/database/states/tracks_to_refresh_with_same_checkpoints.xml")
    public void refreshTracksWithTwoCheckpointsWithSameTS() {
        deliveryTrackDao.refreshTracksLastUpdatedAndNextRequestTs(
            getTrackIds(),
            List.of(12),
            RequestType.ORDER_HISTORY,
            DEFAULT_INTERVAL_VALUE
        );

        final DeliveryTrackMeta deliveryTrackMeta = deliveryTrackDao.getDeliveryTrackMeta(1L);

        //factor = 12, interval = 15, next_request = 15 x 12 (180)
        assertEquals(180, getDiffBetweenUpdateAndNextRequestTime(deliveryTrackMeta));

    }

    @Test
    @DatabaseSetup("/database/states/refresh_only_two_tracks.xml")
    public void refreshOnlyTracksInList() {
        deliveryTrackDao.refreshTracksLastUpdatedAndNextRequestTs(
            getTrackIds(),
            List.of(1, 12),
            RequestType.ORDER_HISTORY,
            DEFAULT_INTERVAL_VALUE
        );

        final DeliveryTrackMeta deliveryTrackMeta = deliveryTrackDao.getDeliveryTrackMeta(1L);
        final DeliveryTrackMeta deliveryTrackMeta2 = deliveryTrackDao.getDeliveryTrackMeta(2L);
        final DeliveryTrackMeta deliveryTrackMeta3 = deliveryTrackDao.getDeliveryTrackMeta(3L);

        //factor = 1, interval = 15, next_request = 1 x 15 (15)
        assertEquals(15, getDiffBetweenUpdateAndNextRequestTime(deliveryTrackMeta));

        //factor = 12, interval = 3, next_request = 12 x 3 (36)
        assertEquals(36, getDiffBetweenUpdateAndNextRequestTime(deliveryTrackMeta2));

        //Not changed
        assertEquals(
            "2018-01-01",
            DateFormatUtils.format(deliveryTrackMeta3.getLastUpdatedDate(), "yyyy-MM-dd")
        );
        //Not changed
        assertNull(deliveryTrackMeta3.getNextRequestDate());

    }

    @Test
    @DatabaseSetup("/database/states/tracks_to_refresh_same_acquired_dates.xml")
    public void refreshTracksWithSameAcquiredDatesMaxCheckpointTsAndMaxFactor() {
        deliveryTrackDao.refreshTracksLastUpdatedAndNextRequestTs(
            getTrackIds(),
            List.of(2),
            RequestType.ORDER_HISTORY,
            DEFAULT_INTERVAL_VALUE
        );

        final DeliveryTrackMeta deliveryTrackMeta = deliveryTrackDao.getDeliveryTrackMeta(1L);

        //factor = 2, interval = 15, next_request = 2 x 15 (30)
        assertEquals(30, getDiffBetweenUpdateAndNextRequestTime(deliveryTrackMeta));

    }

    @Test
    @DatabaseSetup("/database/states/tracks_to_refresh_with_order_track_request_meta.xml")
    public void fallbackToServiceIntervalWhenRequestMetaIntervalNull() {
        deliveryTrackDao.refreshTracksLastUpdatedAndNextRequestTs(
            getTrackIds(),
            List.of(1),
            RequestType.ORDER_HISTORY,
            15
        );

        final DeliveryTrackMeta deliveryTrackMeta = deliveryTrackDao.getDeliveryTrackMeta(1L);

        //factor = 1, ds interval = 15, request meta interval = null, next_request = 1 x 15
        assertEquals(15, getDiffBetweenUpdateAndNextRequestTime(deliveryTrackMeta));
    }

    @Test
    @DatabaseSetup("/database/states/tracks_to_refresh_with_outbound_track_request_meta.xml")
    public void getOutboundRequestInterval() {
        deliveryTrackDao.refreshTracksLastUpdatedAndNextRequestTs(
            getTrackIds(),
            List.of(1),
            RequestType.OUTBOUND_STATUS,
            DEFAULT_INTERVAL_VALUE
        );

        final DeliveryTrackMeta deliveryTrackMeta = deliveryTrackDao.getDeliveryTrackMeta(1L);

        //factor = 1, ds interval = 15, request meta interval = 30, next_request = 1 x 30
        assertEquals(30, getDiffBetweenUpdateAndNextRequestTime(deliveryTrackMeta));
    }

    private long getDiffBetweenUpdateAndNextRequestTime(DeliveryTrackMeta deliveryTrackMeta) {
        return TimeUnit.MILLISECONDS.toMinutes(
            deliveryTrackMeta.getNextRequestDate().getTime()
                - deliveryTrackMeta.getLastUpdatedDate().getTime());
    }

    private List<Long> getTrackIds() {
        return List.of(1L, 2L);
    }
}
