package ru.yandex.market.clickphite.metric;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.clickhouse.HttpResultRow;
import ru.yandex.market.clickphite.ClickHouseTable;
import ru.yandex.market.clickphite.config.ConfigFile;
import ru.yandex.market.clickphite.config.ConfigurationService;
import ru.yandex.market.clickphite.config.TestConfiguration;
import ru.yandex.market.clickphite.config.metric.GraphiteMetricConfig;
import ru.yandex.market.clickphite.dashboard.DashboardContext;
import ru.yandex.market.clickphite.graphite.GraphiteClient;
import ru.yandex.market.clickphite.graphite.Metric;
import ru.yandex.market.clickphite.solomon.SolomonClient;
import ru.yandex.market.statface.StatfaceClient;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 03.08.17
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestConfiguration.class})
public class GraphiteMetricContextTest {
    private MetricResultRow resultRow;
    private TestMetricServiceContext context;

    @Autowired
    private ConfigurationService configurationService;
    private ConfigFile configFile;

    @Before
    public void setUp() throws Exception {
        configFile = new ConfigFile(new File("src/test/resources/graphite-metric-context-test.json"));
        configurationService.parseAndCheck(configFile);

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

    private GraphiteMetricContext createSut(String metricName) {
        List<DashboardContext> dashboardContexts = Collections.singletonList(Mockito.mock(DashboardContext.class));
        return createSut(metricName, dashboardContexts);
    }

    private GraphiteMetricContext createSut(String metricName, List<DashboardContext> dashboardContexts) {
        return new GraphiteMetricContext(
            getMetricConfig(metricName), ClickHouseTable.create("table", "database"),
            dashboardContexts, Collections.emptyList()
        );
    }

    private GraphiteMetricConfig getMetricConfig(String metricName) {
        return configFile.getGraphiteMetricConfigs().stream()
            .filter(c -> c.getMetricName().equals(metricName))
            .findFirst()
            .orElseThrow(() -> new NoSuchElementException("No such metric " + metricName));
    }

    static class TestMetricServiceContext implements MetricServiceContext {
        private List<Metric> metrics = new ArrayList<>();

        @Override
        public GraphiteClient getGraphiteClient() {
            return null;
        }

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