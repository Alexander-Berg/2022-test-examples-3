package ru.yandex.market.clickphite.metric.graphite;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.clickhouse.HttpResultRow;
import ru.yandex.market.clickphite.config.storage.json.ClickphiteConfigFileJsonLoader;
import ru.yandex.market.clickphite.utils.ResourceUtils;
import ru.yandex.market.health.configs.clickphite.dashboard.DashboardContext;
import ru.yandex.market.health.configs.clickphite.defaults.ClickphiteDefaultValueResolver;
import ru.yandex.market.health.configs.clickphite.graphite.Metric;
import ru.yandex.market.health.configs.clickphite.metric.MetricResultRow;
import ru.yandex.market.health.configs.clickphite.metric.MetricServiceContext;
import ru.yandex.market.health.configs.clickphite.metric.graphite.GraphiteMetricContext;
import ru.yandex.market.health.configs.clickphite.metric.graphite.SplitNotFoundException;
import ru.yandex.market.health.configs.clickphite.mongo.ClickphiteConfigEntity;
import ru.yandex.market.health.configs.clickphite.mongo.ClickphiteConfigGroupVersionEntity;
import ru.yandex.market.health.configs.clickphite.mongo.GraphiteMetricEntity;
import ru.yandex.market.solomon.SolomonClient;
import ru.yandex.market.statface.StatfaceClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class GraphiteMetricContextMongoTest {
    private MetricResultRow resultRow;
    private TestMetricServiceContext context;

    @Before
    public void setUp() throws Exception {
        resultRow = Mockito.mock(MetricResultRow.class);
        Mockito.when(resultRow.getSplitValue("bar")).thenReturn("bar_value");
        Mockito.when(resultRow.getSplitValue("baz")).thenReturn("baz_value");
        Mockito.when(resultRow.getSplitValue("splitWithEmptyValue")).thenReturn("");
        Mockito.when(resultRow.getSplitValue("splitWithValueThatNeedsEscaping")).thenReturn("qwe.rty");
        context = new TestMetricServiceContext();
    }

    @Test
    public void dashboardNotifyMetricsIsCalled() throws Exception {
        DashboardContext dashboardContext = Mockito.mock(DashboardContext.class);

        GraphiteMetricContext sut = createSut(
            "foo.${bar}.${splitWithValueThatNeedsEscaping}",
            Collections.singletonList(dashboardContext)
        );
        sut.sendMetrics(Collections.singletonList(resultRow), context);

        Mockito.verify(dashboardContext).notifyMetric("bar_value", "qwe_rty");
    }

    @Test
    public void processSimpleMetricRow() throws Exception {
        createSut("foo.${bar}.${baz}.foo")
            .sendMetrics(Collections.singletonList(resultRow), context);

        assertThat(context.getMetrics())
            .extracting(Metric::getName)
            .containsExactly("one_min.foo.bar_value.baz_value.foo");
    }

    @Test
    public void splitWithEmptyValue() throws Exception {
        createSut("foo.${bar}.${splitWithEmptyValue}")
            .sendMetrics(Collections.singletonList(resultRow), context);

        assertThat(context.getMetrics()).isEmpty();
    }

    @Test
    public void splitWithValueThatNeedsEscaping() throws Exception {
        createSut("foo.${bar}.${splitWithValueThatNeedsEscaping}")
            .sendMetrics(Collections.singletonList(resultRow), context);

        assertThat(context.getMetrics())
            .extracting(Metric::getName)
            .containsExactly("one_min.foo.bar_value.qwe_rty");
    }

    @Test
    public void processSimpleMetricRowWithoutSplits() throws Exception {
        createSut("foo.TOTAL")
            .sendMetrics(Collections.singletonList(resultRow), context);

        assertThat(context.getMetrics())
            .extracting(Metric::getName)
            .containsExactly("one_min.foo.TOTAL");
    }

    @Test
    public void processQuantileMetricRow() throws Exception {
        Mockito.when(resultRow.getQuantileValueArray()).thenReturn(new double[]{12.5, 13.1, 60.1});

        createSut("quantile.${bar}.${baz}")
            .sendMetrics(Collections.singletonList(resultRow), context);

        assertThat(context.getMetrics())
            .extracting(Metric::getName, Metric::getValue)
            .containsExactly(
                tuple("one_min.quantile.bar_value.baz_value.0_5", 12.5),
                tuple("one_min.quantile.bar_value.baz_value.0_6", 13.1),
                tuple("one_min.quantile.bar_value.baz_value.0_7", 60.1)
            );
    }

    @Test
    public void processMetricRowNoDashboard() throws Exception {
        List<DashboardContext> dashboardContexts = Collections.emptyList();

        createSut("foo.${bar}.${baz}.foo", dashboardContexts)
            .sendMetrics(Collections.singletonList(resultRow), context);

        assertThat(context.getMetrics())
            .extracting(Metric::getName)
            .containsExactly("one_min.foo.bar_value.baz_value.foo");
    }

    private GraphiteMetricContext createSut(String metricName) throws SplitNotFoundException {
        List<DashboardContext> dashboardContexts = Collections.singletonList(Mockito.mock(DashboardContext.class));
        return createSut(metricName, dashboardContexts);
    }

    private GraphiteMetricContext createSut(
        String metricName,
        List<DashboardContext> dashboardContexts
    ) throws SplitNotFoundException {
        ClickphiteConfigFileJsonLoader loader = new ClickphiteConfigFileJsonLoader("");

        ClickphiteConfigGroupVersionEntity configGroupVersion = loader.loadOne(
            ResourceUtils.getResourceFile("graphite-metric-context-test.json")
        );

        configGroupVersion = new ClickphiteDefaultValueResolver("db1", false).resolveDefaults(configGroupVersion);

        ClickphiteConfigEntity config = configGroupVersion.getConfigs().stream()
            .filter(c -> c.getGraphiteSolomon().getGraphiteMetrics().stream()
                .anyMatch(metric -> metric.getName().equals(metricName)))
            .findFirst().orElseThrow(() -> new RuntimeException("Metric " + metricName + " doesn't exist"));

        GraphiteMetricEntity graphiteMetric = config.getGraphiteSolomon().getGraphiteMetrics().stream()
            .filter(metric -> metric.getName().equals(metricName))
            .findFirst().orElseThrow(() -> new RuntimeException("Metric " + metricName + " doesn't exist"));

        return new GraphiteMetricContext(
            configGroupVersion,
            config,
            null,
            config.getPeriods().get(0),
            graphiteMetric,
            "",
            "",
            dashboardContexts,
            Collections.emptyList(),
            false,
            false,
            null,
            null
        );
    }

    static class TestMetricServiceContext implements MetricServiceContext {
        private List<Metric> metrics = new ArrayList<>();

        @Override
        public StatfaceClient getStatfaceClient() {
            return null;
        }

        @Override
        public SolomonClient getSolomonClient() {
            return null;
        }

        @Override
        public void sendGraphiteMetrics(List<Metric> metrics) throws IOException {
            this.metrics.addAll(metrics);
        }

        @Override
        public int getRowTimestampSeconds(HttpResultRow row) {
            return 0;
        }

        List<Metric> getMetrics() {
            return metrics;
        }
    }
}
