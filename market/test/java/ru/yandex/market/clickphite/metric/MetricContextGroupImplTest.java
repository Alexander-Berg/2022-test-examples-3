package ru.yandex.market.clickphite.metric;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.clickhouse.HttpResultRow;
import ru.yandex.market.clickphite.config.ConfigurationService;
import ru.yandex.market.clickphite.config.TestConfiguration;
import ru.yandex.market.clickphite.utils.ResourceUtils;
import ru.yandex.market.health.configs.clickphite.QueryBuilder;
import ru.yandex.market.health.configs.clickphite.graphite.Metric;
import ru.yandex.market.health.configs.clickphite.metric.MetricContext;
import ru.yandex.market.health.configs.clickphite.metric.MetricContextGroup;
import ru.yandex.market.health.configs.clickphite.metric.MetricServiceContext;
import ru.yandex.market.health.configs.clickphite.metric.SentMetricsStat;
import ru.yandex.market.statface.StatfaceClient;
import ru.yandex.market.statface.StatfaceData;

import static ru.yandex.market.clickphite.utils.TestMetricContextGroupUtils.getMetricContextGroupByMetric;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class MetricContextGroupImplTest {
    private static final int FIRST_METRIC_VALUE_INDEX = 2;
    private static final int SECOND_METRIC_VALUE_INDEX = 3;
    private static final double SINGLE_VALUE = 1.0;
    private static final double[] QUANTILE_VALUES = {12.5, 13.1, 60.1};
    private static final Gson GSON = new Gson();
    @Autowired
    private Function<String, ConfigurationService> configurationServiceFactory;
    private ConfigurationService configurationService;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

    @Before
    public void setUp() throws Exception {
        configurationService = configurationServiceFactory.apply(
            ResourceUtils.getResourcePath("metric_context_group_impl_test")
        );
    }

    @Test
    public void sendGraphiteMetrics() throws Exception {
        MetricContextGroup sut = getMetricContextGroupByMetric("foo.${bar}.${baz}.foo", configurationService);

        HttpResultRow httpResultRow = Mockito.mock(HttpResultRow.class);
        long timestamp = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
        Mockito.doReturn((int) timestamp).when(httpResultRow).getInt(QueryBuilder.TIMESTAMP_INDEX);
        Mockito.doReturn(SINGLE_VALUE).when(httpResultRow).getDouble(FIRST_METRIC_VALUE_INDEX);
        Mockito.doReturn(QUANTILE_VALUES).when(httpResultRow).getDoubleArray(SECOND_METRIC_VALUE_INDEX);
        Mockito.doReturn("bar").when(httpResultRow).getString("bar");
        Mockito.doReturn("baz").when(httpResultRow).getString("baz");

        MetricServiceContext context = Mockito.mock(MetricServiceContext.class);

        Map<String, Double> sentMetrics = new HashMap<>();
        Mockito.doAnswer(invocation -> {
                List<Metric> metrics = invocation.getArgument(0);
                for (Metric metric : metrics) {
                    sentMetrics.put(metric.getName(), metric.getValue());
                }
                return null;
            }
        ).when(context).sendGraphiteMetrics(Mockito.any());

        SentMetricsStat.Builder builder = SentMetricsStat.builder();

        sut.sendMetrics(Collections.singletonList(httpResultRow), context, builder);

        SentMetricsStat sentMetricsStat = builder.build();

        Assert.assertEquals(
            1,
            sentMetricsStat
                .getSendStats(getMetricContext(sut, "foo.${bar}.${baz}.foo"))
                .getSentMetricsCount()
        );
        Assert.assertEquals(
            3,
            sentMetricsStat
                .getSendStats(getMetricContext(sut, "quantile.${another_bar}.${another_baz}"))
                .getSentMetricsCount()
        );


        Mockito.verify(context, Mockito.times(2)).sendGraphiteMetrics(Mockito.anyList());
        Assert.assertEquals(4, sentMetrics.size());

        Assert.assertEquals(SINGLE_VALUE, sentMetrics.get("one_min.foo.bar.baz.foo"), 0.0);
        Assert.assertEquals(QUANTILE_VALUES[0], sentMetrics.get("one_min.quantile.bar.baz.0_5"), 0.0);
        Assert.assertEquals(QUANTILE_VALUES[1], sentMetrics.get("one_min.quantile.bar.baz.0_6"), 0.0);
        Assert.assertEquals(QUANTILE_VALUES[2], sentMetrics.get("one_min.quantile.bar.baz.0_7"), 0.0);
    }

    @Test
    public void sendStatfaceMetrics() throws Exception {
        MetricContextGroup sut = getMetricContextGroupByMetric("statface/Some/Metric/daily", configurationService);

        HttpResultRow httpResultRow = Mockito.mock(HttpResultRow.class);
        int timestampSeconds = 1507108378;
        Mockito.doReturn(timestampSeconds).when(httpResultRow).getInt(QueryBuilder.TIMESTAMP_INDEX);
        Mockito.doReturn(1.0).when(httpResultRow).getDouble("tp_cnt_0");
        Mockito.doReturn(2.0).when(httpResultRow).getDouble("fp_cnt_0");
        Mockito.doReturn(3.0).when(httpResultRow).getDouble("tn_cnt_1");
        Mockito.doReturn(4.0).when(httpResultRow).getDouble("fn_cnt_1");

        StatfaceClient statfaceClient = Mockito.mock(StatfaceClient.class);
        MetricServiceContext context = Mockito.mock(MetricServiceContext.class);
        Mockito.when(context.getStatfaceClient()).thenReturn(statfaceClient);

        SentMetricsStat.Builder builder = SentMetricsStat.builder();

        sut.sendMetrics(Collections.singletonList(httpResultRow), context, builder);

        SentMetricsStat sentMetricsStat = builder.build();

        for (MetricContext metricContext : sut.getMetricContexts()) {
            Assert.assertEquals(1, sentMetricsStat.getSendStats(metricContext).getSentMetricsCount());
        }

        ArgumentCaptor<StatfaceData> metricsCaptor = ArgumentCaptor.forClass(StatfaceData.class);
        Mockito.verify(statfaceClient, Mockito.times(2)).sendData(metricsCaptor.capture());

        List<StatfaceData> sentData = metricsCaptor.getAllValues();
        verifySentJson(sentData,
            "Some/Metric",
            "{\"values\":[{\"fielddate\":\"2017-10-04 12:12:58\",\"tp_cnt\":1.0,\"fp_cnt\":2.0}]}"
        );

        verifySentJson(sentData,
            "Some/Metric2",
            "{\"values\":[{\"fielddate\":\"2017-10-04 12:12:58\",\"tn_cnt\":3.0,\"fn_cnt\":4.0}]}"
        );
    }

    private void verifySentJson(List<StatfaceData> sentData, String reportName, String expectedJsonString) {
        StatfaceData firstMetricData = sentData.stream()
            .filter(d -> d.getReportName().equals(reportName))
            .findFirst()
            .get();
        String actualJsonString = firstMetricData.toJson();
        JsonObject actualJson = GSON.fromJson(actualJsonString, JsonObject.class);
        JsonObject expectedJson = GSON.fromJson(expectedJsonString, JsonObject.class);
        Assert.assertEquals(expectedJson, actualJson);
    }

    private MetricContext getMetricContext(MetricContextGroup sut, String metricId) {
        return sut.getMetricContexts().stream().filter(m -> m.getId().endsWith(metricId)).findFirst().get();
    }
}
