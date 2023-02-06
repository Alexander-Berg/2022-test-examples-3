package ru.yandex.market.clickphite.pool;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.clickphite.worker.Worker;
import ru.yandex.market.health.configs.clickphite.metric.MetricStorage;

@Ignore
public class WorkerPoolLoggerTest {
    @Mock
    Worker submittedWorker;
    @Mock
    Worker successfulWorker;
    @Mock
    Worker failedWorker;

    @Before
    public void prepare() {
        MockitoAnnotations.initMocks(this);

        Mockito.when(submittedWorker.getPoolName()).thenReturn("FRONT");
        Mockito.when(successfulWorker.getPoolName()).thenReturn("TRACE");
        Mockito.when(failedWorker.getPoolName()).thenReturn("BUSINESS");

        Mockito.when(submittedWorker.getMetricStorage()).thenReturn(MetricStorage.GRAPHITE, MetricStorage.SOLOMON);
        Mockito.when(successfulWorker.getMetricStorage()).thenReturn(MetricStorage.GRAPHITE);
        Mockito.when(failedWorker.getMetricStorage()).thenReturn(MetricStorage.SOLOMON);

        Mockito.when(submittedWorker.getFullTableName()).thenReturn("market.nginx2");
        Mockito.when(successfulWorker.getFullTableName()).thenReturn("market.nginx2");
        Mockito.when(failedWorker.getFullTableName()).thenReturn("market.trace");
    }

    @Test
    public void flushTest() throws InterruptedException {
        WorkerPoolLogger workerPoolLogger = new WorkerPoolLogger();

        ExecutorService executorService = Executors.newFixedThreadPool(10);
        int i = 100;
        while (i-- > 0) {
            executorService.submit(
                () -> workerPoolLogger.increaseNumberOfSubmittedWorkersInPool(submittedWorker));
            executorService.submit(
                () -> workerPoolLogger.increaseNumberOfSuccessFinishedWorkersInPool(successfulWorker));
            executorService.submit(
                () -> workerPoolLogger.increaseNumberOfFailedFinishedWorkersInPool(failedWorker));
        }
        executorService.awaitTermination(5, TimeUnit.SECONDS);
        executorService.shutdownNow();

        Assert.assertEquals("Should not be 0 before flush", 100,
            workerPoolLogger.getWorkerMetric(successfulWorker)
                .updateSuccessFinishedNumberOfWorkers(NumberOfPoolWorkersAction.READ));
        Assert.assertEquals("Should not be 0 before flush", 100,
            workerPoolLogger.getWorkerMetric(failedWorker)
                .updateFailedFinishedNumberOfWorkers(NumberOfPoolWorkersAction.READ));

        workerPoolLogger.flushLogLine();
        Assert.assertEquals("Should be 0 after flush", 0,
            workerPoolLogger.getWorkerMetric(failedWorker)
                .updateFailedFinishedNumberOfWorkers(NumberOfPoolWorkersAction.READ));
        Assert.assertNotEquals("Should not be 0 after flush", 0,
            workerPoolLogger.getWorkerMetric(submittedWorker)
                .updateSubmittedNumberOfWorkers(NumberOfPoolWorkersAction.READ));

        Assertions.assertThatCode(() -> {
            WorkerMetric workerMetric = workerPoolLogger.getWorkerMetric(successfulWorker);
            Assert.assertEquals(workerMetric.getFullTableName(), "market.nginx2");
            Assert.assertEquals(workerMetric.updateSuccessFinishedNumberOfWorkers(NumberOfPoolWorkersAction.READ), 0);
        }).doesNotThrowAnyException();

    }
}
