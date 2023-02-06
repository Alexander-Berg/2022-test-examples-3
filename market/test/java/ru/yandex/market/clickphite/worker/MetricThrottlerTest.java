package ru.yandex.market.clickphite.worker;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

import ru.yandex.market.health.configs.clickphite.ClickHouseTable;
import ru.yandex.market.health.configs.clickphite.MetricPeriod;
import ru.yandex.market.health.configs.clickphite.TimeRange;
import ru.yandex.market.health.configs.clickphite.metric.MetricContextGroup;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 06.08.2019
 */
public class MetricThrottlerTest {
    private static final String GROUP1 = "GROUP1";
    private static final String GROUP2 = "GROUP2";

    private final AtomicLong timeMillis = new AtomicLong(0);
    private final MetricThrottler throttler = new MetricThrottler(
        timeMillis::get,
        120,
        60,
        1200,
        600,
        2_000,
        100,
        1_000_000
    );

    @Test
    public void throttlingDisabled() {
        MetricThrottler noOpThrottler = new MetricThrottler(
            timeMillis::get,
            0,
            0,
            0,
            0,
            0,
            1,
            1
        );
        noOpThrottler.onMetricGroupBuild(group(GROUP1, MetricPeriod.ONE_MIN), new TimeRange(0, 60));
        assertFalse(noOpThrottler.shouldDelay(group(GROUP1, MetricPeriod.ONE_MIN), new TimeRange(0, 60)));
    }

    @Test
    public void onePeriod_realTime() {
        // строим какую-то метрику
        at(1_000).metricGroup1.isBuilt(0, 60);

        // hard throttling начинается сразу после построения
        at(1_000).metricGroup1.shouldBeThrottled(0, 60);
        at(1_030).metricGroup1.shouldBeThrottled(0, 60);

        // soft throttling начинается сразу после hard throttling
        at(1_060).metricGroup1.shouldBeThrottled(0, 60);
        at(1_090).metricGroup1.shouldBeThrottled(0, 60);

        // soft throttling заканчивается
        at(1_120).metricGroup1.shouldNotBeThrottled(0, 60);
    }

    @Test
    public void multiplePeriods_realTime() {
        // строим какую-то метрику
        at(1_000).metricGroup1.isBuilt(60, 120);

        // hard throttling начинается сразу после построения. Откладываем любые перестроения, которые содержат недавно
        // перестроенную метрику за недавно перестроенный период.
        at(1_000).metricGroup1.shouldBeThrottled(60, 120);
        at(1_000).metricGroup1.shouldBeThrottled(0, 120);
        at(1_030).metricGroup1.shouldBeThrottled(60, 120);
        at(1_030).metricGroup1.shouldBeThrottled(0, 120);

        // soft throttling начинается сразу после hard throttling. Откладываем перестроение только если все периоды в
        // нём soft throttled.
        at(1_060).metricGroup1.shouldBeThrottled(60, 120);
        at(1_060).metricGroup1.shouldNotBeThrottled(0, 120);
        at(1_090).metricGroup1.shouldBeThrottled(60, 120);
        at(1_090).metricGroup1.shouldNotBeThrottled(0, 120);

        // soft throttling заканчивается
        at(1_120).metricGroup1.shouldNotBeThrottled(0, 60);
        at(1_120).metricGroup1.shouldNotBeThrottled(60, 120);
        at(1_120).metricGroup1.shouldNotBeThrottled(0, 120);
    }

    @Test
    public void multiplePeriods_nonRealTime() {
        // строим какую-то метрику
        at(3_000).metricGroup1.isBuilt(60, 120);

        // hard throttling начинается сразу после построения. Откладываем любые перестроения, которые содержат недавно
        // перестроенную метрику за недавно перестроенный период.
        at(3_000).metricGroup1.shouldBeThrottled(60, 120);
        at(3_000).metricGroup1.shouldBeThrottled(0, 120);
        at(3_300).metricGroup1.shouldBeThrottled(60, 120);
        at(3_300).metricGroup1.shouldBeThrottled(0, 120);

        // soft throttling начинается сразу после hard throttling. Откладываем перестроение только если все периоды в
        // нём soft throttled.
        at(3_600).metricGroup1.shouldBeThrottled(60, 120);
        at(3_600).metricGroup1.shouldNotBeThrottled(0, 120);
        at(3_900).metricGroup1.shouldBeThrottled(60, 120);
        at(3_900).metricGroup1.shouldNotBeThrottled(0, 120);

        // soft throttling заканчивается
        at(4_200).metricGroup1.shouldNotBeThrottled(0, 60);
        at(4_200).metricGroup1.shouldNotBeThrottled(60, 120);
        at(4_200).metricGroup1.shouldNotBeThrottled(0, 120);
    }

    @Test
    public void differentMetricsAndPeriodsAreThrottledIndependently_realTime() {
        // строим какую-то метрику
        at(1_000).metricGroup1.isBuilt(0, 60);

        // она троттлится
        at(1_000).metricGroup1.shouldBeThrottled(0, 60);

        // другие периоды троттлятся отдельно
        at(1_000).metricGroup1.shouldNotBeThrottled(60, 120);

        // другие метрики троттлятся отдельно
        at(1_000).metricGroup2.shouldNotBeThrottled(0, 60);

        // другие периоды троттлятся отдельно
        at(1_000).metricGroup1DifferentPeriod.shouldNotBeThrottled(0, 60);
    }


    private At at(long seconds) {
        timeMillis.set(TimeUnit.SECONDS.toMillis(seconds));
        return new At();
    }

    private MetricContextGroup group(String id, MetricPeriod metricPeriod) {
        MetricContextGroup result = mock(MetricContextGroup.class);
        when(result.getId()).thenReturn(id);
        when(result.getPeriod()).thenReturn(metricPeriod);
        when(result.getTable()).thenReturn(ClickHouseTable.create("table", "db"));
        return result;
    }

    private class At {
        final MetricGroup metricGroup1 = new MetricGroup(group(GROUP1, MetricPeriod.ONE_MIN));
        final MetricGroup metricGroup1DifferentPeriod = new MetricGroup(group(GROUP1, MetricPeriod.HOUR));
        final MetricGroup metricGroup2 = new MetricGroup(group(GROUP2, MetricPeriod.ONE_MIN));

        private class MetricGroup {
            final MetricContextGroup metricGroup;

            MetricGroup(MetricContextGroup metricGroup) {
                this.metricGroup = metricGroup;
            }

            void isBuilt(int periodStartSeconds, int periodEndSeconds) {
                throttler.onMetricGroupBuild(metricGroup, new TimeRange(periodStartSeconds, periodEndSeconds));
            }

            void shouldBeThrottled(int periodStartSeconds, int periodEndSeconds) {
                assertTrue(throttler.shouldDelay(metricGroup, new TimeRange(periodStartSeconds, periodEndSeconds)));
            }

            void shouldNotBeThrottled(int periodStartSeconds, int periodEndSeconds) {
                assertFalse(throttler.shouldDelay(metricGroup, new TimeRange(periodStartSeconds, periodEndSeconds)));
            }
        }
    }
}
