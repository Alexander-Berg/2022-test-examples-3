package ru.yandex.market.clickphite.config;

import java.util.Collections;
import java.util.HashSet;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.clickphite.metric.QueryWeight;
import ru.yandex.market.health.configs.clickphite.MetricPeriod;
import ru.yandex.market.health.configs.clickphite.metric.MetricContextGroup;

public class WorkerPoolConfigurationServiceTest {
    private static final HashSet<String> MEDIUM_TABLES = new HashSet<>(
        Collections.singletonList("market.medium_table")
    );
    private static final HashSet<String> HEAVY_TABLES = new HashSet<>(
        Collections.singletonList("market.heavy_table")
    );

    @Test
    public void getLightQueryWeight() {
        testGetQueryWeight(QueryWeight.LIGHT, MetricPeriod.ONE_MIN, "market.just_table");
    }

    @Test
    public void getMediumQueryWeight() {
        testGetQueryWeight(QueryWeight.MEDIUM, MetricPeriod.HOUR, "market.just_table");
        testGetQueryWeight(QueryWeight.MEDIUM, MetricPeriod.ONE_MIN, "market.medium_table");
    }

    @Test
    public void getHeavyQueryWeight() {
        testGetQueryWeight(QueryWeight.HEAVY, MetricPeriod.DAY, "market.just_table");
        testGetQueryWeight(QueryWeight.HEAVY, MetricPeriod.DAY, "market.medium_table");
        testGetQueryWeight(QueryWeight.HEAVY, MetricPeriod.ONE_MIN, "market.heavy_table");
    }

    private void testGetQueryWeight(QueryWeight expectedWeight, MetricPeriod metricPeriod, String tableName) {
        MetricContextGroup group = Mockito.mock(MetricContextGroup.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(group.getPeriod()).thenReturn(metricPeriod);
        Mockito.when(group.getTable().getFullName()).thenReturn(tableName);
        Assert.assertEquals(
            expectedWeight,
            WorkerPoolConfigurationService.getQueryWeight(metricPeriod, tableName, MEDIUM_TABLES, HEAVY_TABLES)
        );
    }
}
