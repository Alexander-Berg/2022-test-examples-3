package ru.yandex.market.clickphite.metric.mocks;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.mockito.Mockito;

import ru.yandex.market.clickhouse.HttpResultRow;
import ru.yandex.market.health.configs.clickphite.ClickHouseTable;
import ru.yandex.market.health.configs.clickphite.MetricPeriod;
import ru.yandex.market.health.configs.clickphite.ProcessStatus;
import ru.yandex.market.health.configs.clickphite.config.metric.MetricField;
import ru.yandex.market.health.configs.clickphite.metric.MetricContext;
import ru.yandex.market.health.configs.clickphite.metric.MetricContextGroup;
import ru.yandex.market.health.configs.clickphite.metric.MetricQueries;
import ru.yandex.market.health.configs.clickphite.metric.MetricServiceContext;
import ru.yandex.market.health.configs.clickphite.metric.MetricStorage;
import ru.yandex.market.health.configs.clickphite.metric.SentMetricsStat;
import ru.yandex.market.health.configs.clickphite.mongo.SubAggregateEntity;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 01.12.16
 */
public class MetricContextGroupMock implements MetricContextGroup {
    private final String id;
    private final ProcessStatus processStatus;

    private MetricContextGroupMock(String id, ProcessStatus processStatus) {
        this.id = id;
        this.processStatus = processStatus;
    }

    public static MetricContextGroup metric(String id) {
        return new MetricContextGroupMock(id, status());
    }

    public static MetricContextGroup failingMetric(String id, int howLongAgoFailedMinutes) {
        ProcessStatus processStatus = failingStatus(howLongAgoFailedMinutes);
        return new MetricContextGroupMock(id, processStatus);
    }

    private static ProcessStatus failingStatus(int howLongAgoFailedMinutes) {
        ProcessStatus processStatus = Mockito.mock(ProcessStatus.class);
        long firstFailureInARowMillis = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(howLongAgoFailedMinutes);
        Mockito.when(processStatus.getFirstErrorTimeInARowMillis()).thenReturn(firstFailureInARowMillis);
        return processStatus;
    }

    private static ProcessStatus status() {
        ProcessStatus processStatus = Mockito.mock(ProcessStatus.class);
        Mockito.when(processStatus.getFirstErrorTimeInARowMillis()).thenReturn((long) -1);
        return processStatus;
    }

    @Override
    public ProcessStatus getProcessStatus() {
        return processStatus;
    }

    @Override
    public List<MetricContext> getMetricContexts() {
        throw new UnsupportedOperationException();
    }

    @Override
    public MetricQueries getQueries() {
        throw new UnsupportedOperationException();
    }

    @Override
    public MetricQueries getMetricQueries() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public SentMetricsStat.Builder sendMetrics(
        List<HttpResultRow> httpResultRows,
        MetricServiceContext metricServiceContext,
        SentMetricsStat.Builder statBuilder
    ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<MetricContextGroup> getOrigin() {
        return Optional.empty();
    }

    @Override
    public List<String> getConfigGroupIds() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MetricContextGroupMock that = (MetricContextGroupMock) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public MetricPeriod getPeriod() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ClickHouseTable getTable() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getFilter() {
        throw new UnsupportedOperationException();
    }

    @Override
    public SubAggregateEntity getSubAggregate() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<? extends MetricField> getSplits() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getMovingWindowPeriods() {
        throw new UnsupportedOperationException();
    }

    @Override
    public MetricStorage getStorage() {
        throw new UnsupportedOperationException();
    }
}
