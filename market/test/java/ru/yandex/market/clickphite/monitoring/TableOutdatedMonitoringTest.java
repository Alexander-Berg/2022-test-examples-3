package ru.yandex.market.clickphite.monitoring;

import java.util.ArrayList;
import java.util.List;

import de.bwaldvogel.mongo.backend.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.clickhouse.ClickhouseException;
import ru.yandex.market.clickhouse.ClickhouseMissingTableException;
import ru.yandex.market.clickhouse.ClickhouseTemplate;
import ru.yandex.market.clickphite.ClickphiteService;
import ru.yandex.market.clickphite.config.ClickphiteConfiguration;
import ru.yandex.market.clickphite.config.ConfigurationService;
import ru.yandex.market.clickphite.metric.mocks.ComplicatedMonitoringMock;
import ru.yandex.market.health.configs.clickphite.ClickHouseTable;
import ru.yandex.market.health.configs.clickphite.metric.MetricContext;
import ru.yandex.market.health.configs.clickphite.metric.graphite.GraphiteMetricContext;
import ru.yandex.market.monitoring.MonitoringStatus;


public class TableOutdatedMonitoringTest {
    private static final String TABLE_NAME = "test_table";
    private static final String DB_NAME = "market";

    private ComplicatedMonitoringMock complicatedMonitoringMock;
    private ClickhouseTemplate clickhouseTemplate;
    private ClickphiteService clickphiteService;

    @Before
    public void init() {
        complicatedMonitoringMock = new ComplicatedMonitoringMock();
        clickhouseTemplate = Mockito.mock(ClickhouseTemplate.class);
        clickphiteService = mockClickphiteService();
    }

    private ClickphiteService mockClickphiteService() {
        ClickphiteService clickphiteServiceMock = Mockito.mock(ClickphiteService.class);
        ConfigurationService configurationServiceMock = mockConfigurationService();
        Mockito.when(clickphiteServiceMock.getConfigurationService()).thenReturn(configurationServiceMock);
        Mockito.when(clickphiteServiceMock.isMaster()).thenReturn(true);
        return clickphiteServiceMock;
    }

    private ConfigurationService mockConfigurationService() {
        ConfigurationService configurationService = Mockito.mock(ConfigurationService.class);
        ClickphiteConfiguration clickphiteConfigurationMock = mockClickphiteConfiguration();
        Mockito.when(configurationService.getConfiguration()).thenReturn(clickphiteConfigurationMock);
        return configurationService;
    }

    private ClickphiteConfiguration mockClickphiteConfiguration() {
        ClickphiteConfiguration clickphiteConfiguration = Mockito.mock(ClickphiteConfiguration.class);
        List<MetricContext> metricContexts = mockMetricContexts();
        Mockito.when(clickphiteConfiguration.getMetricContexts()).thenReturn(metricContexts);
        return clickphiteConfiguration;
    }

    private List<MetricContext> mockMetricContexts() {
        List<MetricContext> metricContexts = new ArrayList<>();

        MetricContext metricContext = Mockito.mock(GraphiteMetricContext.class);
        Mockito.when(metricContext.getClickHouseTable()).thenReturn(new ClickHouseTable(DB_NAME, TABLE_NAME));
        metricContexts.add(metricContext);

        return metricContexts;
    }

    @Test
    public void testOkState() {
        Mockito.when(clickhouseTemplate.queryForInt(Mockito.anyString())).thenReturn(1);
        TableOutdatedMonitoring tableOutdatedMonitoring = createTableOutdatedMonitoring(false);
        tableOutdatedMonitoring.runOneIteration();
        Assert.equals(MonitoringStatus.OK, complicatedMonitoringMock.getUnit().getStatus());
    }

    @Test
    public void testTableOutdatedWarnState() {
        Mockito.when(clickhouseTemplate.queryForInt(Mockito.anyString())).thenReturn(0);
        TableOutdatedMonitoring tableOutdatedMonitoring = createTableOutdatedMonitoring(false);
        tableOutdatedMonitoring.runOneIteration();
        Assert.equals(MonitoringStatus.WARNING, complicatedMonitoringMock.getUnit().getStatus());
    }

    @Test
    public void testTableDoesNotExists() {
        Mockito.when(clickhouseTemplate.queryForInt(Mockito.anyString()))
            .thenThrow(new ClickhouseMissingTableException(new ClickhouseException("", "", 0)));
        TableOutdatedMonitoring tableOutdatedMonitoring = createTableOutdatedMonitoring(false);
        tableOutdatedMonitoring.runOneIteration();
        Assert.equals(MonitoringStatus.WARNING, complicatedMonitoringMock.getUnit().getStatus());
    }

    @Test
    public void testUndefinedClickhouseException() {
        Mockito.when(clickhouseTemplate.queryForInt(Mockito.anyString()))
            .thenThrow(new ClickhouseException("", "", 0));
        TableOutdatedMonitoring tableOutdatedMonitoring = createTableOutdatedMonitoring(false);
        tableOutdatedMonitoring.runOneIteration();
        Assert.equals(MonitoringStatus.OK, complicatedMonitoringMock.getUnit().getStatus());
    }

    @Test
    public void testRequestAttemptOnException() {
        Mockito.when(clickhouseTemplate.queryForInt(Mockito.anyString()))
            .thenThrow(new ClickhouseException("", "", 0))
            .thenReturn(0);
        TableOutdatedMonitoring tableOutdatedMonitoring = createTableOutdatedMonitoring(3, false);
        tableOutdatedMonitoring.runOneIteration();
        Mockito.verify(clickhouseTemplate, Mockito.times(2)).queryForInt(Mockito.anyString());
        Assert.equals(MonitoringStatus.WARNING, complicatedMonitoringMock.getUnit().getStatus());
    }

    @Test
    public void testMonitoringForceOk() {
        Mockito.when(clickhouseTemplate.queryForInt(Mockito.anyString())).thenReturn(0);
        TableOutdatedMonitoring tableOutdatedMonitoring = createTableOutdatedMonitoring(true);
        tableOutdatedMonitoring.runOneIteration();
        Assert.equals(MonitoringStatus.OK, complicatedMonitoringMock.getUnit().getStatus());
    }

    @Test
    public void testIgnoreIfNotMaster() {
        Mockito.when(clickphiteService.isMaster()).thenReturn(false);
        TableOutdatedMonitoring tableOutdatedMonitoring = createTableOutdatedMonitoring(false);
        tableOutdatedMonitoring.runOneIteration();
        Mockito.verifyZeroInteractions(clickhouseTemplate);
        Assert.equals(MonitoringStatus.OK, complicatedMonitoringMock.getUnit().getStatus());
    }

    private TableOutdatedMonitoring createTableOutdatedMonitoring(boolean monitoringForceOk) {
        return createTableOutdatedMonitoring(1, monitoringForceOk);
    }

    private TableOutdatedMonitoring createTableOutdatedMonitoring(
        int requestAttemptCount, boolean monitoringForceOk
    ) {
        return new TableOutdatedMonitoring(
            complicatedMonitoringMock,
            clickhouseTemplate,
            clickphiteService,
            0,
            0,
            requestAttemptCount,
            0,
            monitoringForceOk
        );
    }

}
