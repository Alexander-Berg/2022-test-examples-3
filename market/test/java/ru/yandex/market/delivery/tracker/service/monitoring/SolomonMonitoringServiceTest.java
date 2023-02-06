package ru.yandex.market.delivery.tracker.service.monitoring;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONAssert;

import ru.yandex.market.delivery.tracker.configuration.dbqueue.QueueStatistics;
import ru.yandex.market.delivery.tracker.dao.repository.BatchDao;
import ru.yandex.market.delivery.tracker.dao.repository.DatabaseStatisticsDao;
import ru.yandex.market.delivery.tracker.dao.repository.DatabaseStatisticsDao.NamedValue;
import ru.yandex.market.delivery.tracker.dao.repository.DeliveryTrackDao;
import ru.yandex.market.delivery.tracker.domain.entity.UnprocessedBatchesCount;

import static java.util.Collections.singletonList;
import static ru.yandex.market.delivery.tracker.configuration.dbqueue.PushTrackQueueConfiguration.PUSH_QUEUE_NAME;

class SolomonMonitoringServiceTest {

    private final QueueStatistics queueStatistics = Mockito.mock(QueueStatistics.class);
    private final DeliveryTrackDao deliveryTrackDao = Mockito.mock(DeliveryTrackDao.class);
    private final DatabaseStatisticsDao statisticsDao = Mockito.mock(DatabaseStatisticsDao.class);
    private final BatchDao batchDao = Mockito.mock(BatchDao.class);
    private Clock clock = Clock.fixed(Instant.ofEpochMilli(1544460562), ZoneId.of("UTC"));
    private final SolomonMonitoringService service =
        new SolomonMonitoringService(clock, deliveryTrackDao, queueStatistics, statisticsDao, batchDao);

    @Test
    void loadNotRequestedTracksPercentile() throws JSONException {
        Mockito.when(deliveryTrackDao.getNotRequestedTracksMaxDelaySeconds())
            .thenReturn(200L);
        String actual = service.getTracksMonitorings();
        JSONAssert.assertEquals(readFile("data/solomon/not_requested_tracks_max_delay.json"), actual, false);
        Mockito.verify(deliveryTrackDao).getNotRequestedTracksMaxDelaySeconds();
    }

    @Test
    void loadNotPushedTracksPercentile() throws JSONException {
        Mockito.when(deliveryTrackDao.getNotPushedTracksCount())
            .thenReturn(500L);
        String actual = service.getTracksMonitorings();
        JSONAssert.assertEquals(readFile("data/solomon/not_pushed_tracks_count.json"), actual, false);
        Mockito.verify(deliveryTrackDao).getNotPushedTracksCount();
    }

    @Test
    void loadAssignedTracksCount() throws JSONException {
        Mockito.when(deliveryTrackDao.getAssignedTracksCount())
            .thenReturn(700L);
        String actual = service.getTracksMonitorings();
        JSONAssert.assertEquals(readFile("data/solomon/assigned_tracks_count.json"), actual, false);
        Mockito.verify(deliveryTrackDao).getAssignedTracksCount();
    }

    @Test
    void testQueueFilled() throws JSONException {
        Mockito.when(queueStatistics.getTasksCount()).thenReturn(ImmutableMap.of(PUSH_QUEUE_NAME, 3L));
        String s = service.getQueueSizeMonitoring();
        JSONAssert.assertEquals(readFile("data/solomon/queue_filled.json"), s, true);
        Mockito.verify(queueStatistics).getTasksCount();
    }

    @Test
    void testDatabaseStatisticsInvocation() throws JSONException {
        Mockito.when(statisticsDao.getAutovacuumTime()).thenReturn(
            singletonList(new NamedValue("schema.table", 1L))
        );
        Mockito.when(statisticsDao.getRelationSizes()).thenReturn(
            singletonList(new NamedValue("schema.table", 2L))
        );
        Mockito.when(statisticsDao.getDeadTupleSizes()).thenReturn(
            singletonList(new NamedValue("schema.table", 4L))
        );
        Mockito.when(statisticsDao.getInvalidIndexesCount()).thenReturn(5L);
        Mockito.when(statisticsDao.getXminLag()).thenReturn(6L);
        String s = service.getDatabaseStatistics();
        JSONAssert.assertEquals(readFile("data/solomon/database_stats.json"), s, true);
        Mockito.verify(statisticsDao).getAutovacuumTime();
        Mockito.verify(statisticsDao).getRelationSizes();
        Mockito.verify(statisticsDao).getDeadTupleSizes();
        Mockito.verify(statisticsDao).getInvalidIndexesCount();
    }

    @Test
    void testUnprocessedBatchesCountGroupedStatistics() throws JSONException {
        Mockito.when(batchDao.getUnprocessedBatchesStatistics()).thenReturn(
            List.of(
                new UnprocessedBatchesCount(0, 0, 10L),
                new UnprocessedBatchesCount(0, 10, 20L),
                new UnprocessedBatchesCount(1, 0, 60L),
                new UnprocessedBatchesCount(1, 10, 70L)
            )
        );
        Mockito.when(batchDao.getServicesWithUnprocessedBatchesStatistics(true, true)).thenReturn(
            List.of(
                new NamedValue("50", 10L),
                new NamedValue("100", 20L),
                new NamedValue("200", 50L),
                new NamedValue("10K", 100L)
            )
        );
        Mockito.when(batchDao.getServicesWithUnprocessedBatchesStatistics(true, false)).thenReturn(
            List.of(
                new NamedValue("100", 20L),
                new NamedValue("200", 60L),
                new NamedValue("10K", 100L)
            )
        );
        Mockito.when(batchDao.getServicesWithUnprocessedBatchesStatistics(false, true)).thenReturn(
            List.of(
                new NamedValue("500", 80L),
                new NamedValue("10K", 100L)
            )
        );
        String s = service.getUnprocessedBatchesCountMonitoring();
        JSONAssert.assertEquals(readFile("data/solomon/unprocessed_batches_count.json"), s, false);
        Mockito.verify(batchDao).getUnprocessedBatchesStatistics();
        Mockito.verify(batchDao).getServicesWithUnprocessedBatchesStatistics(true, true);
        Mockito.verify(batchDao).getServicesWithUnprocessedBatchesStatistics(true, false);
        Mockito.verify(batchDao).getServicesWithUnprocessedBatchesStatistics(false, true);
    }

    private String readFile(String relativePath) {
        try {
            Path path = Paths.get(this.getClass().getClassLoader().getResource(relativePath).toURI());

            try (Stream<String> lines = Files.lines(path)) {
                return lines.collect(Collectors.joining("\n"));
            }
        } catch (Exception e) {
            throw new RuntimeException("Error during reading from file " + relativePath, e);
        }
    }
}
