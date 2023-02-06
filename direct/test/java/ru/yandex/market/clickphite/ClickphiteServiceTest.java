package ru.yandex.market.clickphite;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.clickphite.config.metric.MetricPeriod;
import ru.yandex.market.clickphite.metric.MetricContextGroup;
import ru.yandex.market.clickphite.metric.QueryWeight;

import java.util.Collections;
import java.util.HashSet;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 19.10.17
 */
public class ClickphiteServiceTest {

    private HashSet<String> mediumTables = new HashSet<>(Collections.singletonList("market.medium_table"));
    private HashSet<String> heavyTables = new HashSet<>(Collections.singletonList("market.heavy_table"));

    @Test
    public void getLightQueryWeight() throws Exception {
        testGetQueryWeight(QueryWeight.LIGHT, MetricPeriod.ONE_MIN, "market.just_table");
    }

    @Test
    public void getMediumQueryWeight() throws Exception {
        testGetQueryWeight(QueryWeight.MEDIUM, MetricPeriod.HOUR, "market.just_table");
        testGetQueryWeight(QueryWeight.MEDIUM, MetricPeriod.ONE_MIN, "market.medium_table");
    }

    @Test
    public void getHeavyQueryWeight() throws Exception {
        testGetQueryWeight(QueryWeight.HEAVY, MetricPeriod.DAY, "market.just_table");
        testGetQueryWeight(QueryWeight.HEAVY, MetricPeriod.DAY, "market.medium_table");
        testGetQueryWeight(QueryWeight.HEAVY, MetricPeriod.ONE_MIN, "market.heavy_table");
    }

    private void testGetQueryWeight(QueryWeight expectedWeight, MetricPeriod metricPeriod, String tableName) {
        MetricContextGroup group = Mockito.mock(MetricContextGroup.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(group.getPeriod()).thenReturn(metricPeriod);
        Mockito.when(group.getTable().getFullName()).thenReturn(tableName);
        Assert.assertEquals(expectedWeight, ClickphiteService.getQueryWeight(group, mediumTables, heavyTables));
    }

}