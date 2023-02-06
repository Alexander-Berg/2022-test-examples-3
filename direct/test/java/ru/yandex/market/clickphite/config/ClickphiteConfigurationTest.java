package ru.yandex.market.clickphite.config;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.clickphite.metric.MetricContextGroup;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import static ru.yandex.market.clickphite.config.ClickphiteConfiguration.extractExpressionsUnderArrayJoin;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 19.04.18
 */
public class ClickphiteConfigurationTest {
    @Test
    public void testExtractExpressionsUnderArrayJoin() {
        Assert.assertEquals(
            Collections.emptyList(),
            extractExpressionsUnderArrayJoin("sumIf(parse_time_ms, status = 'SUCCESS') / sumIf(line_count, status = 'SUCCESS') * 1000")
        );

        Assert.assertEquals(
            Collections.singletonList("source_names"),
            extractExpressionsUnderArrayJoin("arrayJoin(source_names)")
        );

        Assert.assertEquals(
            Collections.singletonList("dictGetHierarchy('category', toUInt64(category_id))"),
            extractExpressionsUnderArrayJoin("dictGetString('category', 'full_name', arrayJoin(dictGetHierarchy('category', toUInt64(category_id))))")
        );

        Assert.assertEquals(
            Collections.singletonList("arrayEnumerate(send_time_millis_per_id)"),
            extractExpressionsUnderArrayJoin("send_time_millis_per_id[arrayJoin(arrayEnumerate(send_time_millis_per_id)) AS i]")
        );

        Assert.assertEquals(
            Arrays.asList("source_names", "category_ids"),
            extractExpressionsUnderArrayJoin("arrayJoin(source_names) / arrayJoin(category_ids)")
        );
    }

    @Test
    public void arrayJoinMetricGroupping() {
        File file = new File("src/test/resources/array_join_metric_groupping");
        ConfigurationService configurationService = TestConfiguration.createConfigurationService(file.getAbsolutePath());

        List<MetricContextGroup> metricContextGroups = configurationService.getConfiguration().getMetricContextGroups();

        Assert.assertEquals(
            new HashSet<>(Arrays.asList(
                "graphite.one_min.foo.foo",
                "graphite.one_min.foo.TOTAL",
                "graphite.one_min.foo.${bar}.foo,graphite.one_min.foo.${bar}.foo2"
            )),
            metricContextGroups.stream().map(g -> g.getId()).collect(Collectors.toSet())
        );
    }
}