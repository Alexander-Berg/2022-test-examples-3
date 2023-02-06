package ru.yandex.market.clickphite.config.worker_pool;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.clickhouse.HttpResultRow;
import ru.yandex.market.clickphite.pool.WorkerPoolLogger;
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

public class WorkerPoolExecutorsTest {

    @Test
    public void simpleTest() throws InterruptedException {
        WorkerPoolExecutors executors = createWorkerPoolExecutors();

        Map<MetricStorage, Double> constraintsByStorage = new HashMap<>();
        constraintsByStorage.put(MetricStorage.GRAPHITE, 0.4);
        constraintsByStorage.put(MetricStorage.SOLOMON, 0.4);
        constraintsByStorage.put(MetricStorage.STATFACE, 0.2);

        executors.updateControllers(constraintsByStorage, 100, Collections.emptyList());
        Assert.assertEquals(
            40,
            executors.executorsByStorage.get(MetricStorage.GRAPHITE.name()).getThreadCount()
        );
        Assert.assertEquals(
            40,
            executors.executorsByStorage.get(MetricStorage.SOLOMON.name()).getThreadCount()
        );
        Assert.assertEquals(
            20,
            executors.executorsByStorage.get(MetricStorage.STATFACE.name()).getThreadCount()
        );
        Assert.assertFalse(
            executors.executorsByStorage.containsKey(WorkerPoolExecutors.DEFAULT_EXECUTOR)
        );
        Assert.assertTrue(executors.getWarningMessages().isEmpty());
    }

    @Test
    public void cleanTest() throws InterruptedException {
        WorkerPoolExecutors executors = createWorkerPoolExecutors();
        Map<MetricStorage, Double> constraintsByStorage = new HashMap<>();
        constraintsByStorage.put(MetricStorage.GRAPHITE, 0.4);
        constraintsByStorage.put(MetricStorage.SOLOMON, 0.4);
        constraintsByStorage.put(MetricStorage.STATFACE, 0.2);

        executors.updateControllers(constraintsByStorage, 100, Collections.emptyList());
        executors.clear(Collections.emptyList());

        Assert.assertFalse(executors.executorsByStorage.containsKey(MetricStorage.GRAPHITE.name()));
        Assert.assertFalse(executors.executorsByStorage.containsKey(MetricStorage.SOLOMON.name()));
        Assert.assertFalse(executors.executorsByStorage.containsKey(MetricStorage.STATFACE.name()));
        Assert.assertFalse(executors.executorsByStorage.containsKey(WorkerPoolExecutors.DEFAULT_EXECUTOR));
        Assert.assertTrue(executors.getWarningMessages().isEmpty());
    }

    @Test
    public void defaultExecutorTest() throws InterruptedException {
        WorkerPoolExecutors executors = createWorkerPoolExecutors();

        Map<MetricStorage, Double> constraintsByStorage = new HashMap<>();
        constraintsByStorage.put(MetricStorage.GRAPHITE, 0.4);

        executors.updateControllers(constraintsByStorage, 100, Collections.emptyList());
        Assert.assertEquals(
            40,
            executors.executorsByStorage.get(MetricStorage.GRAPHITE.name()).getThreadCount()
        );
        Assert.assertFalse(executors.executorsByStorage.containsKey(MetricStorage.SOLOMON.name()));
        Assert.assertFalse(executors.executorsByStorage.containsKey(MetricStorage.STATFACE.name()));
        Assert.assertEquals(
            60,
            executors.executorsByStorage.get(WorkerPoolExecutors.DEFAULT_EXECUTOR).getThreadCount()
        );
        Assert.assertTrue(executors.getWarningMessages().isEmpty());
    }

    @Test
    public void onlyDefaultPoolExecutorTest() throws InterruptedException {
        WorkerPoolExecutors executors = createWorkerPoolExecutors();

        executors.updateControllers(Collections.emptyMap(), 100, Collections.emptyList());
        Assert.assertFalse(executors.executorsByStorage.containsKey(MetricStorage.GRAPHITE.name()));
        Assert.assertFalse(executors.executorsByStorage.containsKey(MetricStorage.SOLOMON.name()));
        Assert.assertFalse(executors.executorsByStorage.containsKey(MetricStorage.STATFACE.name()));
        Assert.assertEquals(
            100,
            executors.executorsByStorage.get(WorkerPoolExecutors.DEFAULT_EXECUTOR).getThreadCount()
        );
        Assert.assertTrue(executors.getWarningMessages().isEmpty());
    }

    @Test
    public void zeroExecutorTest() throws InterruptedException {
        WorkerPoolExecutors executors = createWorkerPoolExecutors();

        Map<MetricStorage, Double> constraintsByStorage = new HashMap<>();
        constraintsByStorage.put(MetricStorage.GRAPHITE, 0.4);
        constraintsByStorage.put(MetricStorage.SOLOMON, 0.6);
        constraintsByStorage.put(MetricStorage.STATFACE, 0.0);

        executors.updateControllers(constraintsByStorage, 100, Collections.emptyList());
        Assert.assertEquals(
            40,
            executors.executorsByStorage.get(MetricStorage.GRAPHITE.name()).getThreadCount()
        );
        Assert.assertEquals(
            60,
            executors.executorsByStorage.get(MetricStorage.SOLOMON.name()).getThreadCount()
        );
        Assert.assertFalse(
            executors.executorsByStorage.containsKey(MetricStorage.STATFACE.name())
        );
        Assert.assertFalse(
            executors.executorsByStorage.containsKey(WorkerPoolExecutors.DEFAULT_EXECUTOR)
        );
        Assert.assertTrue(executors.getWarningMessages().isEmpty());
    }

    @Test
    public void overflowConstraintsTest() throws InterruptedException {
        WorkerPoolExecutors executors = createWorkerPoolExecutors();

        Map<MetricStorage, Double> constraintsByStorage = new HashMap<>();
        constraintsByStorage.put(MetricStorage.GRAPHITE, 0.8);
        constraintsByStorage.put(MetricStorage.SOLOMON, 0.8);
        constraintsByStorage.put(MetricStorage.STATFACE, 0.4);

        executors.updateControllers(constraintsByStorage, 100, Collections.emptyList());
        Assert.assertEquals(
            40,
            executors.executorsByStorage.get(MetricStorage.GRAPHITE.name()).getThreadCount()
        );
        Assert.assertEquals(
            40,
            executors.executorsByStorage.get(MetricStorage.SOLOMON.name()).getThreadCount()
        );
        Assert.assertEquals(
            20,
            executors.executorsByStorage.get(MetricStorage.STATFACE.name()).getThreadCount()
        );
        Assert.assertFalse(
            executors.executorsByStorage.containsKey(WorkerPoolExecutors.DEFAULT_EXECUTOR)
        );
        Assert.assertFalse(executors.getWarningMessages().isEmpty());
    }

    @Test
    public void leakConstraintsTest() throws InterruptedException {
        WorkerPoolExecutors executors = createWorkerPoolExecutors();

        Map<MetricStorage, Double> constraintsByStorage = new HashMap<>();
        constraintsByStorage.put(MetricStorage.GRAPHITE, 0.2);
        constraintsByStorage.put(MetricStorage.SOLOMON, 0.2);
        constraintsByStorage.put(MetricStorage.STATFACE, 0.1);

        executors.updateControllers(constraintsByStorage, 100, Collections.emptyList());
        Assert.assertEquals(
            40,
            executors.executorsByStorage.get(MetricStorage.GRAPHITE.name()).getThreadCount()
        );
        Assert.assertEquals(
            40,
            executors.executorsByStorage.get(MetricStorage.SOLOMON.name()).getThreadCount()
        );
        Assert.assertEquals(
            20,
            executors.executorsByStorage.get(MetricStorage.STATFACE.name()).getThreadCount()
        );
        Assert.assertFalse(
            executors.executorsByStorage.containsKey(WorkerPoolExecutors.DEFAULT_EXECUTOR)
        );
        Assert.assertFalse(executors.getWarningMessages().isEmpty());
    }

    @Test
    public void leakAndZeroConstraintsTest() throws InterruptedException {
        WorkerPoolExecutors executors = createWorkerPoolExecutors();

        Map<MetricStorage, Double> constraintsByStorage = new HashMap<>();
        constraintsByStorage.put(MetricStorage.GRAPHITE, 0.4);
        constraintsByStorage.put(MetricStorage.SOLOMON, 0.4);
        constraintsByStorage.put(MetricStorage.STATFACE, 0.0);

        executors.updateControllers(constraintsByStorage, 100, Collections.emptyList());
        Assert.assertEquals(
            50,
            executors.executorsByStorage.get(MetricStorage.GRAPHITE.name()).getThreadCount()
        );
        Assert.assertEquals(
            50,
            executors.executorsByStorage.get(MetricStorage.SOLOMON.name()).getThreadCount()
        );
        Assert.assertFalse(
            executors.executorsByStorage.containsKey(MetricStorage.STATFACE.name())
        );
        Assert.assertFalse(
            executors.executorsByStorage.containsKey(WorkerPoolExecutors.DEFAULT_EXECUTOR)
        );
        Assert.assertFalse(executors.getWarningMessages().isEmpty());
    }

    @Test
    public void testCloseMetricContextGroupOnSimpleUpdate() throws InterruptedException {
        WorkerPoolExecutors executors = createWorkerPoolExecutors();

        Map<MetricStorage, Double> constraintsByStorage = new HashMap<>();
        constraintsByStorage.put(MetricStorage.GRAPHITE, 0.4);
        constraintsByStorage.put(MetricStorage.SOLOMON, 0.4);
        constraintsByStorage.put(MetricStorage.STATFACE, 0.2);
        executors.updateControllers(constraintsByStorage, 100, Collections.emptyList());

        MetricContextGroup graphiteContext = new TestMetricContextGroup(MetricStorage.GRAPHITE);
        List<MetricContextGroup> metricContexts = Collections.singletonList(graphiteContext);
        //update GRAPHITE, SOLOMON and STATFACE pools
        executors.updateControllers(constraintsByStorage, 200, metricContexts);

        Assert.assertFalse(graphiteContext.getProcessStatus().isOnAir());
        Assert.assertTrue(executors.getWarningMessages().isEmpty());
    }

    @Test
    public void testCloseMetricContextGroupOnUpdateDefaultPool() throws InterruptedException {
        WorkerPoolExecutors executors = createWorkerPoolExecutors();

        Map<MetricStorage, Double> constraintsByStorage = new HashMap<>();
        constraintsByStorage.put(MetricStorage.GRAPHITE, 0.4);
        constraintsByStorage.put(MetricStorage.SOLOMON, 0.4);
        executors.updateControllers(constraintsByStorage, 100, Collections.emptyList());

        MetricContextGroup statfaceContext = new TestMetricContextGroup(MetricStorage.STATFACE);
        List<MetricContextGroup> metricContexts = Collections.singletonList(statfaceContext);
        constraintsByStorage.put(MetricStorage.GRAPHITE, 0.5);
        //update GRAPHITE and DEFAULT pools
        executors.updateControllers(constraintsByStorage, 100, metricContexts);

        Assert.assertFalse(statfaceContext.getProcessStatus().isOnAir());
        Assert.assertTrue(executors.getWarningMessages().isEmpty());
    }

    @Test
    public void testSkipMetricContextGroupIfNotChangesInPool() throws InterruptedException {
        WorkerPoolExecutors executors = createWorkerPoolExecutors();

        Map<MetricStorage, Double> constraintsByStorage = new HashMap<>();
        constraintsByStorage.put(MetricStorage.GRAPHITE, 0.4);
        constraintsByStorage.put(MetricStorage.SOLOMON, 0.4);
        constraintsByStorage.put(MetricStorage.STATFACE, 0.2);
        executors.updateControllers(constraintsByStorage, 100, Collections.emptyList());

        MetricContextGroup solomonContext = new TestMetricContextGroup(MetricStorage.SOLOMON);
        List<MetricContextGroup> metricContexts = Collections.singletonList(solomonContext);
        constraintsByStorage.put(MetricStorage.GRAPHITE, 0.5);
        constraintsByStorage.put(MetricStorage.STATFACE, 0.1);
        //update GRAPHITE and STATFACE pools
        executors.updateControllers(constraintsByStorage, 100, metricContexts);

        Assert.assertTrue(solomonContext.getProcessStatus().isOnAir());
        Assert.assertTrue(executors.getWarningMessages().isEmpty());
    }

    @Test
    public void testCloseMetricContextGroupOnClose() throws InterruptedException {
        WorkerPoolExecutors executors = createWorkerPoolExecutors();

        Map<MetricStorage, Double> constraintsByStorage = new HashMap<>();
        constraintsByStorage.put(MetricStorage.GRAPHITE, 0.4);
        constraintsByStorage.put(MetricStorage.SOLOMON, 0.4);
        constraintsByStorage.put(MetricStorage.STATFACE, 0.2);
        executors.updateControllers(constraintsByStorage, 100, Collections.emptyList());

        List<MetricContextGroup> metricContexts = new ArrayList<>();
        metricContexts.add(new TestMetricContextGroup(MetricStorage.GRAPHITE));
        metricContexts.add(new TestMetricContextGroup(MetricStorage.SOLOMON));
        metricContexts.add(new TestMetricContextGroup(MetricStorage.STATFACE));
        //update GRAPHITE, SOLOMON and STATFACE pools
        executors.clear(metricContexts);

        metricContexts.stream()
            .map(MetricContextGroup::getProcessStatus)
            .map(ProcessStatus::isOnAir)
            .forEach(Assert::assertFalse);
        Assert.assertTrue(executors.getWarningMessages().isEmpty());
    }

    private WorkerPoolExecutors createWorkerPoolExecutors() {
        return new WorkerPoolExecutors(Mockito.mock(WorkerPoolLogger.class), "TEST", 0);
    }

    private static class TestMetricContextGroup implements MetricContextGroup {
        private final MetricStorage storage;
        private final ProcessStatus processStatus;

        TestMetricContextGroup(MetricStorage storage) {
            this.storage = storage;
            this.processStatus = new ProcessStatus();
            processStatus.setOnAir(true);
        }

        @Override
        public String getId() {
            return null;
        }

        @Override
        public MetricPeriod getPeriod() {
            return null;
        }

        @Override
        public ClickHouseTable getTable() {
            return new ClickHouseTable("market", "test");
        }

        @Override
        public String getFilter() {
            return null;
        }

        @Override
        public SubAggregateEntity getSubAggregate() {
            return null;
        }

        @Override
        public List<? extends MetricField> getSplits() {
            return null;
        }

        @Override
        public List<? extends MetricContext> getMetricContexts() {
            return null;
        }

        @Override
        public int getMovingWindowPeriods() {
            return 0;
        }

        @Override
        public ProcessStatus getProcessStatus() {
            return processStatus;
        }

        @Override
        public MetricQueries getQueries() {
            return null;
        }

        @Override
        public MetricQueries getMetricQueries() {
            return null;
        }

        @Override
        public SentMetricsStat.Builder sendMetrics(
            List<HttpResultRow> httpResultRows,
            MetricServiceContext metricServiceContext,
            SentMetricsStat.Builder statBuilder
        ) {
            return null;
        }

        @Override
        public Optional<MetricContextGroup> getOrigin() {
            return Optional.empty();
        }

        @Override
        public List<String> getConfigGroupIds() {
            return null;
        }

        @Override
        public MetricStorage getStorage() {
            return storage;
        }
    }

}
