package ru.yandex.hadoop.woodsman.loaders.logbroker.parser.service;

import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckFilter;
import com.codahale.metrics.health.HealthCheckRegistry;
import org.junit.Test;

import java.time.Instant;
import java.util.SortedMap;
import java.util.concurrent.Executors;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class ReloadStateServiceTest {
    @Test
    public void test() {
        HealthCheckRegistry healthCheckRegistry = healthCheckRegistry();
        ReloadStateService<Object, Object> service = new ReloadStateService("name-it", Executors.newSingleThreadScheduledExecutor(),
                1000L, 1000L, healthCheckRegistry) {
            private int count = 0;
            @Override
            protected boolean needReload(Object key) {
                return true;
            }

            @Override
            protected void reloadIntl() {
                int current = count++;
                if (current == 0 ||
                        current == 1 ||
                        current > 2) {
                    throw new RuntimeException("");
                }
            }

            @Override
            protected Object getStateIntl(Object key) {
                return null;
            }
        };
        HealthCheckFilter healthCheckFilter = (name, hc) -> ReloadStateHealthCheck.class.isInstance(hc);

        HealthCheck healthCheck = healthCheckRegistry.getHealthCheck("name-it");
        assertThat(healthCheck, instanceOf(ReloadStateHealthCheck.class));

        Instant start = Instant.now();
        service.getState(new Object());

        Instant mid = Instant.now();
        Instant failAt = service.reloadFailed.get();
        assertThat(failAt, notNullValue());
        assertFalse(failAt.isBefore(start));
        assertFalse(failAt.isAfter(mid));

        service.getState(new Object());
        assertThat(service.reloadFailed.get(), notNullValue());
        assertFalse(service.reloadFailed.get().isBefore(start));
        assertFalse(service.reloadFailed.get().isAfter(mid));
        assertThat(service.reloadFailed.get(), is(failAt));

        SortedMap<String, HealthCheck.Result> healthChecks = healthCheckRegistry.runHealthChecks(healthCheckFilter);
        assertThat(healthChecks.values(), hasSize(1));
        assertThat(healthChecks, hasKey("name-it"));
        HealthCheck.Result actual = healthChecks.get("name-it");
        assertThat(actual.isHealthy(), is(false));
        assertThat(actual.getMessage(), is(failAt.toString()));

        service.getState(new Object());
        assertThat(service.reloadFailed.get(), nullValue());

        healthChecks = healthCheckRegistry.runHealthChecks(healthCheckFilter);
        assertThat(healthChecks.values(), hasSize(1));
        assertThat(healthChecks, hasKey("name-it"));
        actual = healthChecks.get("name-it");
        assertThat(actual.isHealthy(), is(true));
        assertThat(actual.getMessage(), nullValue());

        Instant end = Instant.now();
        service.getState(new Object());
        assertThat(service.reloadFailed.get(), notNullValue());
        assertTrue(service.reloadFailed.get().isAfter(failAt));
        assertFalse(service.reloadFailed.get().isBefore(end));
    }

    private HealthCheckRegistry healthCheckRegistry() {
        return new HealthCheckRegistry();
    }
}
