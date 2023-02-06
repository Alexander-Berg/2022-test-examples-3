package ru.yandex.market.clickphite.config;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.clickphite.utils.ResourceUtils;
import ru.yandex.market.health.configs.clickphite.Queriable;
import ru.yandex.market.health.configs.clickphite.metric.MetricContextGroup;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 19.04.18
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ClickphiteConfigurationTest {
    @Autowired
    private Function<String, ConfigurationService> configurationServiceFactory;
    private ConfigurationService configurationService;

    @Before
    public void setUp() throws Exception {
        configurationService = configurationServiceFactory.apply(
            ResourceUtils.getResourcePath("array_join_metric_groupping")
        );
    }

    @Test
    public void arrayJoinMetricGroupping() {
        List<MetricContextGroup> metricContextGroups = configurationService.getConfiguration().getMetricContextGroups();

        Assert.assertEquals(
            new HashSet<>(Arrays.asList(
                "graphite.one_min.foo.foo",
                "graphite.one_min.foo.TOTAL",
                "graphite.one_min.foo.${bar}.foo,graphite.one_min.foo.${bar}.foo2"
            )),
            metricContextGroups.stream().map(Queriable::getId).collect(Collectors.toSet())
        );
    }

}
