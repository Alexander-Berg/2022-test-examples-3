package ru.yandex.executor.concurrent;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import org.apache.http.concurrent.FutureCallback;

import ru.yandex.util.timesource.TimeSource;

public class FakeTasksExecutor
    extends Timer
    implements TasksExecutor<FakeTask>
{
    private final long start = TimeSource.INSTANCE.currentTimeMillis();
    private final List<CompletedTask> completedTasks = new ArrayList<>();
    private final Logger logger;

    public FakeTasksExecutor(final Logger logger) {
        super("FakeExecutor", true);
        this.logger = logger;
    }

    public synchronized CompletedTask getCompletedTask(final int id) {
        CompletedTask task = null;
        for (CompletedTask nextTask: completedTasks) {
            if (nextTask.task().id() == id) {
                if (task == null) {
                    task = nextTask;
                } else {
                    throw new IllegalStateException();
                }
            }
        }
        return task;
    }

    public synchronized int completedTasksCount() {
        return completedTasks.size();
    }

    private long timeTaken() {
        return TimeSource.INSTANCE.currentTimeMillis() - start;
    }

    @Override
    public void execute(
        final FakeTask task,
        final FutureCallback<Void> callback)
    {
        logger.info("Executing task " + task);
        long executionTime = task.executionTime();
        if (task.currentRetry() == task.retriesCount()) {
            if (executionTime > 0L) {
                logger.info(
                    "Done with retries, task " + task
                    + " will be completed in " + executionTime + " ms");
                schedule(
                    new TimerTask() {
                        @Override
                        public void run() {
                            synchronized (FakeTasksExecutor.this) {
                                logger.info("Completing task " + task);
                                completedTasks.add(
                                    new CompletedTask(
                                        task,
                                        timeTaken(),
                                        completedTasks.size()));
                                callback.completed(null);
                            }
                        }
                    },
                    executionTime);
            } else {
                synchronized (this) {
                    logger.info(
                        "Done with retries, task " + task
                        + " will be completed immediately");
                    completedTasks.add(
                        new CompletedTask(
                            task,
                            timeTaken(),
                            completedTasks.size()));
                    callback.completed(null);
                }
            }
        } else {
            task.incrementRetry();
            if (executionTime > 0L) {
                logger.info(
                    "Task " + task
                    + " failure will take " + executionTime + " ms");
                schedule(
                    new TimerTask() {
                        @Override
                        public void run() {
                            logger.info(
                                "Failing task " + task
                                + ", next retry in " + task.retryInterval()
                                + " ms");
                            callback.failed(
                                new FakeTaskException(task.retryInterval()));
                        }
                    },
                    executionTime);
            } else {
                logger.info(
                    "Task " + task + " will fail immediately, next retry in "
                    + task.retryInterval() + " ms");
                callback.failed(new FakeTaskException(task.retryInterval()));
            }
        }
    }

    @Override
    public long retryIntervalFor(final Exception e) {
        if (e instanceof FakeTaskException) {
            return ((FakeTaskException) e).retryInterval();
        } else {
            return -1L;
        }
    }
}

