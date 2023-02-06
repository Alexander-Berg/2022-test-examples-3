package ru.yandex.market.reporting.common.service;

import java.time.Instant;
import java.util.Collections;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.junit.Before;
import org.junit.Test;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class MetricsServiceTest {

    private MetricsService service;

    @Before
    public void setup() {
        service = new MetricsService(new MetricRegistry(), "test");
    }

    @Test
    public void testMeterCounters() {
        service.countOk(Collections.singletonList("A"));
        service.countOk(Collections.singletonList("A"));
        service.countFail(Collections.singletonList("B"));
        service.countFail(Collections.singletonList("B"));
        service.countOk(Collections.singletonList("B"));
        service.countOk(Collections.singletonList("C"));

        assertThat(service.getMetricRegistry().meter("test.meter.A.success").getCount(), is(2L));
        assertThat(service.getMetricRegistry().meter("test.meter.B.success").getCount(), is(1L));
        assertThat(service.getMetricRegistry().meter("test.meter.B.fail").getCount(), is(2L));
        assertThat(service.getMetricRegistry().meter("test.meter.C.success").getCount(), is(1L));
        assertThat(service.getMetricRegistry().meter("test.meter.C.fail").getCount(), is(0L));

    }

    @Test
    public void testRate() {
        service.countOk(Collections.singletonList("A"));
        service.countOk(Collections.singletonList("A"));
        service.countFail(Collections.singletonList("A"));
        service.countOk(Collections.singletonList("A"));
        service.countOk(Collections.singletonList("A"));

        Meter meter = service.getMetricRegistry().meter("test.meter.A.success");
        assertThat(meter.getCount(), is(4L));
        assertThat(meter.getMeanRate(), greaterThan(0.0));
    }

    @Test
    public void testMeterTime() {
        long nanosec = 1000000000;
        service.meterDuration(Collections.singletonList("A"), Instant.now().minusSeconds(10));
        service.meterDuration(Collections.singletonList("A"), Instant.now().minusSeconds(5));
        service.meterDuration(Collections.singletonList("A"), Instant.now().minusSeconds(15));

        Timer timer = service.getMetricRegistry().timer("test.meter.A.duration");
        assertThat(timer.getCount(), is(3L));
        assertThat(timer.getSnapshot().getMax() / nanosec, is(15L));
        assertThat(timer.getSnapshot().getMin() / nanosec, is(5L));
        assertThat(timer.getSnapshot().get99thPercentile() / nanosec, is(15.0));


    }

}
