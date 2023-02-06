package ru.yandex.executor.concurrent;

import org.hamcrest.MatcherAssert;
import org.hamcrest.number.OrderingComparison;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.test.util.TestBase;

public class ConcurrentExecutorTest extends TestBase {
    private static final long CHECK_INTERVAL = 100L;
    private static final long OVERHEAD = 2000L;

    public ConcurrentExecutorTest() {
        super(false, 0L);
    }

    private FakeTasksExecutor executeAll(
        final int concurrencyLevel,
        final int maxRetryTasksCount,
        final FakeTask... tasks)
        throws Exception
    {
        FakeTasksExecutor tasksExecutor = new FakeTasksExecutor(logger);
        try (FakeTasksProvider provider = new FakeTasksProvider(logger, tasks);
            ConcurrentExecutor<FakeTask> executor =
                new ConcurrentExecutor<>(
                    Thread.currentThread().getThreadGroup(),
                    "Executor",
                    provider,
                    tasksExecutor,
                    concurrencyLevel,
                    maxRetryTasksCount))
        {
            executor.start();
            long total = 0L;
            long maxAvailabilityDelay = 0;
            int completableTasks = 0;
            for (FakeTask task: tasks) {
                if (task.availabilityDelay() > maxAvailabilityDelay) {
                    maxAvailabilityDelay = task.availabilityDelay();
                }
                total += (task.retriesCount() + 1) * task.executionTime();
                if (task.retryInterval() >= 0L) {
                    ++completableTasks;
                    total += task.retriesCount() * task.retryInterval();
                }
            }
            total += maxAvailabilityDelay;
            total += OVERHEAD;
            long iterations = total / CHECK_INTERVAL;
            for (long i = 0; i < iterations; ++i) {
                Thread.sleep(CHECK_INTERVAL);
                if (tasksExecutor.completedTasksCount() == completableTasks) {
                    break;
                }
            }
            if (tasksExecutor.completedTasksCount() != completableTasks) {
                logger.warning(
                    "Not all tasks completed: "
                    + tasksExecutor.completedTasksCount()
                    + " ! = "
                    + completableTasks
                    + ", max availability delay: " + maxAvailabilityDelay
                    + ", total wait time: " + total);
            }
        }
        return tasksExecutor;
    }

    @Test
    public void test() throws Exception {
        FakeTasksExecutor tasksExecutor =
            executeAll(10, 10, new FakeTask(0, 0, 0L, 0L, 0L));
        FakeTask task = tasksExecutor.getCompletedTask(0).task();
        Assert.assertEquals(0, task.id());
        Assert.assertEquals(0, task.currentRetry());
    }

    @Test
    public void testRetries() throws Exception {
        // Check that failed tasks are:
        // 1. Retried
        // 2. Have higher priority than new tasks
        FakeTasksExecutor tasksExecutor =
            executeAll(
                1,
                1,
                new FakeTask(0, 1, 1000L, 250L, 0L),
                new FakeTask(1, 0, 0L, 0L, 0L));
        CompletedTask first = tasksExecutor.getCompletedTask(0);
        Assert.assertEquals(0, first.task().id());
        Assert.assertEquals(1, first.task().currentRetry());
        Assert.assertEquals(0, first.number());
        MatcherAssert.assertThat(
            first.timeTaken(),
            OrderingComparison.greaterThanOrEqualTo(1500L));

        CompletedTask second = tasksExecutor.getCompletedTask(1);
        Assert.assertEquals(1, second.task().id());
        Assert.assertEquals(0, second.task().currentRetry());
        Assert.assertEquals(1, second.number());
        MatcherAssert.assertThat(
            second.timeTaken(),
            OrderingComparison.greaterThanOrEqualTo(first.timeTaken()));
    }

    @Test
    public void testTasksAvailability() throws Exception {
        FakeTasksExecutor tasksExecutor =
            executeAll(10, 10, new FakeTask(0, 0, 0L, 0L, 1000L));
        CompletedTask task = tasksExecutor.getCompletedTask(0);
        Assert.assertEquals(0, task.task().id());
        Assert.assertEquals(0, task.task().currentRetry());
        MatcherAssert.assertThat(
            task.timeTaken(),
            OrderingComparison.greaterThanOrEqualTo(1000L));
    }

    @Test
    public void testTaskRetries() throws Exception {
        // Won't be completed, but will have currentRetry incremented once
        FakeTask badTask = new FakeTask(1, 2, -1L, 0L, 0L);
        FakeTasksExecutor tasksExecutor =
            executeAll(
                10,
                10,
                // Will be completed first
                new FakeTask(0, 1, 0L, 0L, 0L),
                // Won't be completed
                badTask,
                // Will be retried
                new FakeTask(2, 1, 2000L, 0L, 0L),
                // Will be retried twice, but faster
                new FakeTask(3, 2, 500L, 0L, 0L));
        Assert.assertEquals(1, badTask.currentRetry());
        Assert.assertEquals(3, tasksExecutor.completedTasksCount());
        Assert.assertNull(tasksExecutor.getCompletedTask(1));

        Assert.assertEquals(2, tasksExecutor.getCompletedTask(2).number());
        Assert.assertEquals(1, tasksExecutor.getCompletedTask(3).number());
    }
}

