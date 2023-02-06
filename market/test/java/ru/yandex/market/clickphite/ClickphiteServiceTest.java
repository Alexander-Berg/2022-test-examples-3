package ru.yandex.market.clickphite;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.health.configs.clickphite.MetricPeriod;
import ru.yandex.market.health.configs.clickphite.metric.MetricContextGroup;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 19.10.17
 */
public class ClickphiteServiceTest {

    @Test
    public void testCustomUpdateLimit() {
        Map<String, Integer> updateLimits = new HashMap<>();
        updateLimits.put("ONE_MIN", 1);

        MetricContextGroup group = Mockito.mock(MetricContextGroup.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(group.getPeriod()).thenReturn(MetricPeriod.ONE_MIN);

        Assert.assertEquals(1, ClickphiteService.getUpdateLimit(group, updateLimits));
    }

    @Test
    public void testDefaultUpdateLimit() {
        Map<String, Integer> updateLimits = new HashMap<>();
        updateLimits.put("default", 1);

        MetricContextGroup group = Mockito.mock(MetricContextGroup.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(group.getPeriod()).thenReturn(MetricPeriod.FIVE_MIN);

        Assert.assertEquals(1, ClickphiteService.getUpdateLimit(group, updateLimits));
    }

    @Test
    public void testUndefinedUpdateLimit() {
        Map<String, Integer> updateLimits = new HashMap<>();
        updateLimits.put("ONE_MIN", 1);

        MetricContextGroup group = Mockito.mock(MetricContextGroup.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(group.getPeriod()).thenReturn(MetricPeriod.FIVE_MIN);

        Assert.assertEquals(0, ClickphiteService.getUpdateLimit(group, updateLimits));
    }
}
