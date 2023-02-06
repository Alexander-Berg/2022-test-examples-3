package ru.yandex.market.clickphite.worker;

import com.google.common.collect.RangeSet;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import ru.yandex.market.clickphite.ClickphiteService;
import ru.yandex.market.clickphite.DateTimeUtils;
import ru.yandex.market.clickphite.TimeRange;
import ru.yandex.market.clickphite.config.ConfigurationService;
import ru.yandex.market.clickphite.config.TestConfiguration;
import ru.yandex.market.clickphite.meta.MetricData;
import ru.yandex.market.clickphite.metric.AsyncMetricGroupMonitoring;
import ru.yandex.market.clickphite.metric.MetricContext;
import ru.yandex.market.clickphite.metric.MetricContextGroup;
import ru.yandex.market.clickphite.metric.MetricContextGroupImpl;
import ru.yandex.market.clickphite.metric.MetricQueue;
import ru.yandex.market.clickphite.metric.MetricService;
import ru.yandex.market.clickphite.metric.QueryWeight;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicLong;

import static ru.yandex.market.clickphite.utils.TestMetricContextGroupUtils.getMetricContextGroupByMetric;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 20.09.17
 */
public class MetricGroupWorkerTest {
    private static final int METRIC_DELAY_SECONDS = 30;

    private ClickphiteService clickphiteService;
    private MetricService metricService;
    private AsyncMetricGroupMonitoring asyncMetricGroupMonitoring;
    private ConfigurationService configurationService;

    @Before
    public void setUp() throws Exception {
        File file = new File("src/test/resources/metric_group_realtime_worker_test");
        configurationService = TestConfiguration.createConfigurationService(file.getAbsolutePath());

        clickphiteService = Mockito.mock(ClickphiteService.class);
        Mockito.when(clickphiteService.isLagMode()).thenReturn(false);
        Mockito.when(clickphiteService.isMaster()).thenReturn(true);
        Mockito.when(clickphiteService.getMaxQueriesPerUpdate()).thenReturn(10);
        Mockito.when(clickphiteService.getMetricDelaySeconds()).thenReturn(METRIC_DELAY_SECONDS);
        Mockito.when(clickphiteService.getMaxMetricsAgeOnLagMinutes()).thenReturn(30);

        metricService = Mockito.mock(MetricService.class);
        asyncMetricGroupMonitoring = Mockito.mock(AsyncMetricGroupMonitoring.class);
    }

    @Test
    public void noIntervals() throws Exception {
        MetricContextGroup metricContextGroup = getMetricContextGroupByMetric("foo.${bar}.${baz}.foo", configurationService);

        MetricGroupWorker sut = createSut(metricContextGroup);
        sut.doWork();

        Mockito.verify(metricService, Mockito.never())
            .updateMetricGroup(Mockito.any(), Mockito.any(), Mockito.eq(QueryWeight.LIGHT));
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
                Mockito.eq(QueryWeight.LIGHT)
            );

        Assert.assertFalse(hasTasksInQueue(metricGroup));
    }

    @Test
    public void oldIntervalInLagModeIsIgnored() throws Exception {
        Mockito.when(clickphiteService.isLagMode()).thenReturn(true);
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
            .updateMetricGroup(Mockito.any(), Mockito.any(), Mockito.eq(QueryWeight.LIGHT));
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
            .updateMetricGroup(Mockito.any(), Mockito.any(), Mockito.eq(QueryWeight.LIGHT));
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
                Mockito.any()
            );
        inOrder.verify(metricService)
            .updateMetricGroup(
                Mockito.eq(metricContextGroup(firstQueueUpdater.metricContext)),
                Mockito.eq(timeRange(Duration.ofHours(0), Duration.ofHours(1))),
                Mockito.any()
            );
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
                Mockito.any()
            );
        inOrder.verify(metricService)
            .updateMetricGroup(
                Mockito.eq(metricContextGroup(firstQueueUpdater.metricContext)),
                Mockito.eq(timeRange(Duration.ofHours(1), Duration.ofHours(2))),
                Mockito.any()
            );
        inOrder.verify(metricService)
            .updateMetricGroup(
                Mockito.eq(metricContextGroup(secondQueueUpdater.metricContext)),
                Mockito.eq(timeRange(Duration.ofHours(2).minusMinutes(4), Duration.ofHours(2).minusMinutes(3))),
                Mockito.any()
            );
        inOrder.verify(metricService)
            .updateMetricGroup(
                Mockito.eq(metricContextGroup(firstQueueUpdater.metricContext)),
                Mockito.eq(timeRange(Duration.ofHours(0), Duration.ofHours(1))),
                Mockito.any()
            );
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
                Mockito.any()
            );
        inOrder.verify(metricService)
            .updateMetricGroup(
                Mockito.eq(metricContextGroup(secondQueueUpdater.metricContext)),
                Mockito.eq(timeRange(Duration.ofHours(2).minusMinutes(1), Duration.ofHours(2))),
                Mockito.any()
            );
        inOrder.verifyNoMoreInteractions();
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
        MetricContextGroup metricContextGroup = getMetricContextGroupByMetric("foo.${bar}.${baz}.foo", configurationService);
        Assert.assertEquals(2, metricContextGroup.getMetricContexts().size());

        setupMetric(metricContextGroup, 0, "src/test/resources/metric_queues/queue_1.json");
        setupMetric(metricContextGroup, 1, "src/test/resources/metric_queues/queue_2.json");

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

    private void setupMetric(MetricContextGroup metricContextGroup, int metricIndex, String queueFilePath) throws IOException {
        MetricQueue metricQueue = TestMetricQueueJsonReader.readMetricQueue(queueFilePath);
        MetricContext metricContext = metricContextGroup.getMetricContexts().get(metricIndex);
        metricContext.setMetricData(new MetricData(metricContext.getId(), metricQueue));
    }

    private boolean hasTasksInQueue(MetricContextGroup metricContextGroup) {
        return metricContextGroup.getMetricContexts().stream()
            .anyMatch(m -> m.getMetricQueue().hasActualTasks(Integer.MAX_VALUE));
    }

    private MetricGroupWorker createSut(MetricContextGroup metricContextGroup) {
        return new MetricGroupWorker(
            clickphiteService, metricService, asyncMetricGroupMonitoring, metricContextGroup,
            QueryWeight.LIGHT
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
            metricContext.getMetricQueue().add(timestamp.toMillis(), rangeSet);
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