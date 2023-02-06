package ru.yandex.market.clickphite.worker;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import com.google.common.collect.RangeSet;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.clickphite.ClickphiteService;
import ru.yandex.market.clickphite.config.ConfigurationService;
import ru.yandex.market.clickphite.config.TestConfiguration;
import ru.yandex.market.clickphite.metric.AsyncMetricGroupMonitoring;
import ru.yandex.market.clickphite.metric.MetricService;
import ru.yandex.market.clickphite.metric.QueryWeight;
import ru.yandex.market.clickphite.pool.WorkerPoolLogger;
import ru.yandex.market.clickphite.utils.ResourceUtils;
import ru.yandex.market.health.configs.clickphite.DateTimeUtils;
import ru.yandex.market.health.configs.clickphite.TimeRange;
import ru.yandex.market.health.configs.clickphite.meta.MetricData;
import ru.yandex.market.health.configs.clickphite.metric.MetricContext;
import ru.yandex.market.health.configs.clickphite.metric.MetricContextGroup;
import ru.yandex.market.health.configs.clickphite.metric.MetricContextGroupImpl;
import ru.yandex.market.health.configs.clickphite.metric.MetricQueue;

import static ru.yandex.market.clickphite.utils.TestMetricContextGroupUtils.getMetricContextGroupByMetric;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 20.09.17
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class MetricGroupWorkerTest {
    private static final int METRIC_DELAY_SECONDS = 30;
    private static final String TEST_POOL = "TEST_POOL";

    private ClickphiteService clickphiteService;
    private MetricService metricService;
    private AsyncMetricGroupMonitoring asyncMetricGroupMonitoring;
    @Autowired
    private Function<String, ConfigurationService> configurationServiceFactory;
    private ConfigurationService configurationService;

    private MetricThrottler throttler;

    @Before
    public void setUp() throws Exception {
        configurationService = configurationServiceFactory.apply(
            ResourceUtils.getResourcePath("metric_group_realtime_worker_test")
        );

        clickphiteService = Mockito.mock(ClickphiteService.class);
        Mockito.when(clickphiteService.isLagMode(TEST_POOL)).thenReturn(false);
        Mockito.when(clickphiteService.isMaster()).thenReturn(true);
        Mockito.when(clickphiteService.getMaxQueriesPerUpdate()).thenReturn(10);
        Mockito.when(clickphiteService.getMetricDelaySeconds()).thenReturn(METRIC_DELAY_SECONDS);
        Mockito.when(clickphiteService.getMaxMetricsAgeOnLagMinutes()).thenReturn(30);

        WorkerPoolLogger workerPoolLogger = Mockito.mock(WorkerPoolLogger.class);
        Mockito.doNothing().when(workerPoolLogger).decreaseNumberOfSubmittedWorkersInPool(Mockito.any());
        Mockito.doNothing().when(workerPoolLogger).increaseNumberOfSubmittedWorkersInPool(Mockito.any());
        Mockito.when(clickphiteService.getWorkerPoolLogger()).thenReturn(workerPoolLogger);

        metricService = Mockito.mock(MetricService.class);
        asyncMetricGroupMonitoring = Mockito.mock(AsyncMetricGroupMonitoring.class);

        throttler = new MetricThrottler(
            () -> 0L,
            0,
            0,
            0,
            0,
            0,
            1,
            1
        );
    }

    @Test
    public void noIntervals() throws Exception {
        MetricContextGroup metricContextGroup = getMetricContextGroupByMetric(
            "foo.${bar}.${baz}.foo",
            configurationService
        );

        MetricGroupWorker sut = createSut(metricContextGroup);
        sut.doWork();

        Mockito.verify(metricService, Mockito.never())
            .updateMetricGroup(Mockito.any(), Mockito.any(), Mockito.eq(QueryWeight.LIGHT), Mockito.any());
    }

    @Test
    public void buildOneIntervalForTwoMetricsInGroup() throws Exception {
        MetricContextGroup metricGroup = getMetricContextGroupByMetric("foo.${bar}.${baz}.foo", configurationService);
        Assert.assertEquals(2, metricGroup.getMetricContexts().size());

        Duration currentTime = Duration.ofMinutes(2).plusSeconds(35);
        Duration diffAddTime = Duration.ofMinutes(2);
        GroupQueueUpdater queueUpdater = new GroupQueueUpdater(metricGroup);
        queueUpdater.addRange(diffAddTime, Duration.ofMinutes(0), Duration.ofMinutes(1));

        MetricGroupWorker sut = createSut(metricGroup);
        sut.setCurrentTimeMillisSupplier(currentTime::toMillis);
        sut.doWork();

        Mockito.verify(metricService, Mockito.times(1))
            .updateMetricGroup(
                Mockito.eq(metricGroup),
                Mockito.eq(timeRange(Duration.ofMinutes(0), Duration.ofMinutes(1))),
                Mockito.eq(QueryWeight.LIGHT),
                Mockito.any());

        Assert.assertFalse(hasTasksInQueue(metricGroup));
    }

    @Test
    public void oldIntervalInLagModeIsIgnored() throws Exception {
        Mockito.when(clickphiteService.isLagMode(TEST_POOL)).thenReturn(true);
        MetricContextGroup metricGroup = getMetricContextGroupByMetric("foo.${bar}.${baz}.foo", configurationService);
        Assert.assertEquals(2, metricGroup.getMetricContexts().size());

        Duration currentTime = Duration.ofHours(1).plusSeconds(35);
        Duration diffAddTime = Duration.ofHours(1);
        GroupQueueUpdater queueUpdater = new GroupQueueUpdater(metricGroup);
        queueUpdater.addRange(diffAddTime, Duration.ofMinutes(0), Duration.ofMinutes(1));

        MetricGroupWorker sut = createSut(metricGroup);
        sut.setCurrentTimeMillisSupplier(currentTime::toMillis);
        sut.doWork();

        Mockito.verify(metricService, Mockito.never())
            .updateMetricGroup(Mockito.any(), Mockito.any(), Mockito.eq(QueryWeight.LIGHT), Mockito.any());
    }

    @Test
    public void buildsUpTo10IntervalsPerRun() throws Exception {
        MetricContextGroup metricGroup = getMetricContextGroupByMetric("foo.${bar}.${baz}.foo", configurationService);

        Duration currentTime = Duration.ofHours(101).plusSeconds(35);
        Duration diffAddTime = Duration.ofHours(101);
        GroupQueueUpdater queueUpdater = new GroupQueueUpdater(metricGroup);
        queueUpdater.addRange(diffAddTime, Duration.ofHours(0), Duration.ofHours(100));

        MetricGroupWorker sut = createSut(metricGroup);
        sut.setCurrentTimeMillisSupplier(currentTime::toMillis);
        sut.doWork();

        Mockito.verify(metricService, Mockito.times(10))
            .updateMetricGroup(Mockito.any(), Mockito.any(), Mockito.eq(QueryWeight.LIGHT), Mockito.any());
    }

    @Test
    public void twoBigOverlappingRanges() throws Exception {
        MetricContextGroup metricGroup = getMetricContextGroupByMetric("foo.${bar}.${baz}.foo", configurationService);
        Assert.assertEquals(2, metricGroup.getMetricContexts().size());

        QueueUpdater firstQueueUpdater = new QueueUpdater("foo.${bar}.${baz}.foo", metricGroup);
        QueueUpdater secondQueueUpdater = new QueueUpdater("quantile.${bar}.${baz}", metricGroup);

        Duration currentTime = Duration.ofHours(2).plusSeconds(35);
        Duration diffAddTime = Duration.ofHours(2);
        firstQueueUpdater.addRange(diffAddTime, Duration.ofHours(0), Duration.ofHours(2));
        secondQueueUpdater.addRange(diffAddTime, Duration.ofHours(1), Duration.ofHours(2));

        MetricGroupWorker sut = createSut(metricGroup);
        sut.setCurrentTimeMillisSupplier(currentTime::toMillis);
        sut.doWork();

        InOrder inOrder = Mockito.inOrder(metricService);
        inOrder.verify(metricService)
            .updateMetricGroup(
                Mockito.eq(metricGroup),
                Mockito.eq(timeRange(Duration.ofHours(1), Duration.ofHours(2))),
                Mockito.any(),
                Mockito.any());
        inOrder.verify(metricService)
            .updateMetricGroup(
                Mockito.eq(metricContextGroup(firstQueueUpdater.metricContext)),
                Mockito.eq(timeRange(Duration.ofHours(0), Duration.ofHours(1))),
                Mockito.any(),
                Mockito.any());
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void bigRangeAndTwoSmallOnes() throws Exception {
        MetricContextGroup metricGroup = getMetricContextGroupByMetric("foo.${bar}.${baz}.foo", configurationService);
        Assert.assertEquals(2, metricGroup.getMetricContexts().size());

        QueueUpdater firstQueueUpdater = new QueueUpdater("foo.${bar}.${baz}.foo", metricGroup);
        QueueUpdater secondQueueUpdater = new QueueUpdater("quantile.${bar}.${baz}", metricGroup);

        Duration currentTime = Duration.ofHours(2).plusMinutes(2).plusSeconds(35);
        Duration diffAddTime = Duration.ofHours(2).plusMinutes(2);
        firstQueueUpdater.addRange(diffAddTime, Duration.ofHours(0), Duration.ofHours(2));
        secondQueueUpdater.addRange(diffAddTime,
            Duration.ofHours(2).minusMinutes(1), Duration.ofHours(2).plusMinutes(1)
        );
        secondQueueUpdater.addRange(diffAddTime,
            Duration.ofHours(2).minusMinutes(4), Duration.ofHours(2).minusMinutes(3)
        );

        MetricGroupWorker sut = createSut(metricGroup);
        sut.setCurrentTimeMillisSupplier(currentTime::toMillis);
        sut.doWork();

        InOrder inOrder = Mockito.inOrder(metricService);
        inOrder.verify(metricService)
            .updateMetricGroup(
                Mockito.eq(metricContextGroup(secondQueueUpdater.metricContext)),
                Mockito.eq(timeRange(Duration.ofHours(2).minusMinutes(1), Duration.ofHours(2).plusMinutes(1))),
                Mockito.any(),
                Mockito.any());
        inOrder.verify(metricService)
            .updateMetricGroup(
                Mockito.eq(metricContextGroup(firstQueueUpdater.metricContext)),
                Mockito.eq(timeRange(Duration.ofHours(1), Duration.ofHours(2))),
                Mockito.any(),
                Mockito.any());
        inOrder.verify(metricService)
            .updateMetricGroup(
                Mockito.eq(metricContextGroup(secondQueueUpdater.metricContext)),
                Mockito.eq(timeRange(Duration.ofHours(2).minusMinutes(4), Duration.ofHours(2).minusMinutes(3))),
                Mockito.any(),
                Mockito.any());
        inOrder.verify(metricService)
            .updateMetricGroup(
                Mockito.eq(metricContextGroup(firstQueueUpdater.metricContext)),
                Mockito.eq(timeRange(Duration.ofHours(0), Duration.ofHours(1))),
                Mockito.any(),
                Mockito.any());
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void shouldChooseBiggerRangeFirstIfUpperBoundsEqual() throws Exception {
        MetricContextGroup metricGroup = getMetricContextGroupByMetric("foo.${bar}.${baz}.foo", configurationService);
        Assert.assertEquals(2, metricGroup.getMetricContexts().size());

        QueueUpdater firstQueueUpdater = new QueueUpdater("foo.${bar}.${baz}.foo", metricGroup);
        QueueUpdater secondQueueUpdater = new QueueUpdater("quantile.${bar}.${baz}", metricGroup);

        Duration currentTime = Duration.ofHours(2).plusMinutes(2).plusSeconds(35);
        Duration diffAddTime = Duration.ofHours(2).plusMinutes(2);
        firstQueueUpdater.addRange(diffAddTime, Duration.ofHours(1), Duration.ofHours(2));
        secondQueueUpdater.addRange(diffAddTime,
            Duration.ofHours(2).minusMinutes(1), Duration.ofHours(2)
        );

        MetricGroupWorker sut = createSut(metricGroup);
        sut.setCurrentTimeMillisSupplier(currentTime::toMillis);
        sut.doWork();

        InOrder inOrder = Mockito.inOrder(metricService);
        inOrder.verify(metricService)
            .updateMetricGroup(
                Mockito.eq(metricContextGroup(firstQueueUpdater.metricContext)),
                Mockito.eq(timeRange(Duration.ofHours(1), Duration.ofHours(2))),
                Mockito.any(),
                Mockito.any());
        inOrder.verify(metricService)
            .updateMetricGroup(
                Mockito.eq(metricContextGroup(secondQueueUpdater.metricContext)),
                Mockito.eq(timeRange(Duration.ofHours(2).minusMinutes(1), Duration.ofHours(2))),
                Mockito.any(),
                Mockito.any());
        inOrder.verifyNoMoreInteractions();
    }

    @Test  // MARKETINFRA-3342
    public void throttling() throws Exception {
        AtomicLong throttlerCurrentTimeMillis = new AtomicLong(1_000_000);
        throttler = new MetricThrottler(
            throttlerCurrentTimeMillis::get,
            120,
            60,
            120,
            60,
            30 * 60,
            100,
            300
        );

        MetricContextGroup metricGroup = getMetricContextGroupByMetric("foo.${bar}.${baz}.foo", configurationService);
        Assert.assertEquals(2, metricGroup.getMetricContexts().size());

        Duration currentTime = Duration.ofMinutes(2).plusSeconds(35);
        Duration diffAddTime = Duration.ofMinutes(2);
        GroupQueueUpdater queueUpdater = new GroupQueueUpdater(metricGroup);
        queueUpdater.addRange(diffAddTime, Duration.ofMinutes(0), Duration.ofMinutes(1));

        MetricGroupWorker sut = createSut(metricGroup);
        sut.setCurrentTimeMillisSupplier(currentTime::toMillis);
        sut.doWork();

        queueUpdater.addRange(diffAddTime, Duration.ofMinutes(0), Duration.ofMinutes(1));
        sut.doWork();

        Mockito.verify(metricService, Mockito.times(1))
            .updateMetricGroup(
                Mockito.eq(metricGroup),
                Mockito.eq(timeRange(Duration.ofMinutes(0), Duration.ofMinutes(1))),
                Mockito.eq(QueryWeight.LIGHT),
                Mockito.any());

        Assert.assertTrue(hasTasksInQueue(metricGroup));

        throttlerCurrentTimeMillis.set(2_000_000);

        sut.doWork();

        Mockito.verify(metricService, Mockito.times(2))
            .updateMetricGroup(
                Mockito.eq(metricGroup),
                Mockito.eq(timeRange(Duration.ofMinutes(0), Duration.ofMinutes(1))),
                Mockito.eq(QueryWeight.LIGHT),
                Mockito.any());

        Assert.assertFalse(hasTasksInQueue(metricGroup));
    }

    @Test
    public void buildTimeLimitForSingleTimeRange() throws Exception {
        MetricContextGroup metricGroup = getMetricContextGroupByMetric("foo.${bar}.${baz}.foo", configurationService);
        Mockito.doAnswer(
            invocation -> {
                TimeUnit.SECONDS.sleep(1);
                return null;
            }
        ).when(metricService).updateMetricGroup(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        Duration currentTime = Duration.ofHours(101).plusSeconds(35);
        Duration diffAddTime = Duration.ofHours(101);
        GroupQueueUpdater queueUpdater = new GroupQueueUpdater(metricGroup);
        queueUpdater.addRange(diffAddTime, Duration.ofHours(0), Duration.ofHours(100));

        MetricGroupWorker sut = createSut(metricGroup, 1, 0);
        sut.setCurrentTimeMillisSupplier(currentTime::toMillis);
        sut.doWork();

        Mockito.verify(metricService, Mockito.times(1))
            .updateMetricGroup(Mockito.any(), Mockito.any(), Mockito.eq(QueryWeight.LIGHT), Mockito.any());
    }

    @Test
    public void buildTimeTotalLimit() throws Exception {
        MetricContextGroup metricGroup = getMetricContextGroupByMetric("foo.${bar}.${baz}.foo", configurationService);
        Mockito.doAnswer(
            invocation -> {
                TimeUnit.SECONDS.sleep(1);
                return null;
            }
        ).when(metricService).updateMetricGroup(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        Duration currentTime = Duration.ofHours(101).plusSeconds(35);
        Duration diffAddTime = Duration.ofHours(101);
        GroupQueueUpdater queueUpdater = new GroupQueueUpdater(metricGroup);
        queueUpdater.addRange(diffAddTime, Duration.ofHours(0), Duration.ofHours(100));

        MetricGroupWorker sut = createSut(metricGroup, 0, 3);
        sut.setCurrentTimeMillisSupplier(currentTime::toMillis);
        sut.doWork();

        Mockito.verify(metricService, Mockito.times(3))
            .updateMetricGroup(Mockito.any(), Mockito.any(), Mockito.eq(QueryWeight.LIGHT), Mockito.any());
    }

    /**
     * Это не совсем тест, а скорее инструмент отладки. Можно скормить ему реальный json из монги и посмотреть,
     * в каком порядке {@link MetricGroupWorker} будет строить периоды.
     *
     * @throws Exception
     */
    @Test
    @Ignore
    public void realMetricQueueTest() throws Exception {
        MetricContextGroup metricContextGroup = getMetricContextGroupByMetric(
            "foo.${bar}.${baz}.foo",
            configurationService
        );
        Assert.assertEquals(2, metricContextGroup.getMetricContexts().size());

        setupMetric(metricContextGroup, 0, ResourceUtils.getResourcePath("metric_queues/queue_1.json"));
        setupMetric(metricContextGroup, 1, ResourceUtils.getResourcePath("metric_queues/queue_2.json"));

        MetricGroupWorker sut = createSut(metricContextGroup);
        AtomicLong currentTimeMillis = new AtomicLong(1508519430000L);
        sut.setCurrentTimeMillisSupplier(currentTimeMillis::get);
        for (int i = 0; i < 100; ++i) {
            sut.doWork();
            currentTimeMillis.addAndGet(Duration.ofMinutes(1).toMillis());
        }
    }

    private MetricContextGroup metricContextGroup(MetricContext... metricContexts) {
        return MetricContextGroupImpl.create(Arrays.asList(metricContexts));
    }

    private TimeRange timeRange(Duration from, Duration to) {
        return new TimeRange((int) from.getSeconds(), (int) to.getSeconds());
    }

    private void setupMetric(
        MetricContextGroup metricContextGroup, int metricIndex, String queueFilePath
    ) throws IOException {
        MetricQueue metricQueue = TestMetricQueueJsonReader.readMetricQueue(queueFilePath);
        MetricContext metricContext = metricContextGroup.getMetricContexts().get(metricIndex);
        metricContext.setMetricData(new MetricData(metricContext.getId(), metricQueue));
    }

    private boolean hasTasksInQueue(MetricContextGroup metricContextGroup) {
        return metricContextGroup.getMetricContexts().stream()
            .anyMatch(m -> m.getMetricQueue().hasActualTasks(Integer.MAX_VALUE));
    }

    private MetricGroupWorker createSut(MetricContextGroup metricContextGroup) {
        return createSut(metricContextGroup, 0, 0);
    }

    private MetricGroupWorker createSut(
        MetricContextGroup metricContextGroup,
        int groupUpdateLimitSeconds,
        int totalGroupUpdateLimitSeconds
    ) {
        return new MetricGroupWorker(
            clickphiteService, metricService, asyncMetricGroupMonitoring, metricContextGroup,
            throttler, QueryWeight.LIGHT, TEST_POOL, null, groupUpdateLimitSeconds, totalGroupUpdateLimitSeconds
        );
    }

    private MetricContext getMetricContext(MetricContextGroup metricContextGroup, String metricName) {
        return metricContextGroup.getMetricContexts().stream()
            .filter(m -> m.getId().endsWith(metricName))
            .findFirst()
            .orElseThrow(() -> new NoSuchElementException("No such metric " + metricName));
    }

    class QueueUpdater {
        private final MetricContext metricContext;

        QueueUpdater(String metricName, MetricContextGroup metricContextGroup) {
            this.metricContext = getMetricContext(metricContextGroup, metricName);
        }

        QueueUpdater(MetricContext metricContext) {
            this.metricContext = metricContext;
        }

        void addRange(Duration timestamp, Duration from, Duration to) {
            RangeSet<Integer> rangeSet = DateTimeUtils.toPeriodRangeSet(timeRange(from, to), metricContext.getPeriod());
            metricContext.addRangesToQueue(timestamp.toMillis(), rangeSet);
        }

    }

    class GroupQueueUpdater {
        private final MetricContextGroup metricContextGroup;

        GroupQueueUpdater(MetricContextGroup metricContextGroup) {
            this.metricContextGroup = metricContextGroup;
        }

        void addRange(Duration timestamp, Duration from, Duration to) {
            metricContextGroup.getMetricContexts().stream()
                .map(QueueUpdater::new)
                .forEach(u -> u.addRange(timestamp, from, to));
        }

    }
}
