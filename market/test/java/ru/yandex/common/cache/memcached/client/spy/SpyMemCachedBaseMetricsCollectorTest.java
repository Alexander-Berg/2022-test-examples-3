package ru.yandex.common.cache.memcached.client.spy;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SpyMemCachedBaseMetricsCollectorTest {
    SpyMemCachedBaseMetricsCollector collector = new SpyMemCachedBaseMetricsCollector() {
    };

    @Test
    public void counter() {
        String name = "x";
        collector.incrementCounter(name, 100); // noop, should not fail
        collector.decrementCounter(name, 200); // noop, should not fail
        assertThat(collector.registry.getCounters()).isEmpty();

        collector.addCounter(name);
        collector.incrementCounter(name, 3);
        collector.decrementCounter(name, 1);
        assertThat(collector.registry.getCounters()).hasSize(1);
        assertThat(collector.registry.counter(name).getCount()).isEqualTo(2);

        collector.removeCounter(name);
        assertThat(collector.registry.getCounters()).isEmpty();
    }

    @Test
    public void meter() {
        String name = "x";
        collector.markMeter(name); // noop, should not fail
        assertThat(collector.registry.getMeters()).isEmpty();

        collector.addMeter(name);
        collector.markMeter(name);
        collector.markMeter(name);
        assertThat(collector.registry.getMeters()).hasSize(1);
        assertThat(collector.registry.meter(name).getCount()).isEqualTo(2);

        collector.removeMeter(name);
        assertThat(collector.registry.getMeters()).isEmpty();
    }

    @Test
    public void histogram() {
        String name = "x";
        collector.updateHistogram(name, 100); // noop, should not fail
        assertThat(collector.registry.getHistograms()).isEmpty();

        collector.addHistogram(name);
        collector.updateHistogram(name, 3);
        collector.updateHistogram(name, 2);
        assertThat(collector.registry.getHistograms()).hasSize(1);
        assertThat(collector.registry.histogram(name).getCount()).isEqualTo(2);

        collector.removeHistogram(name);
        assertThat(collector.registry.getHistograms()).isEmpty();
    }
}
