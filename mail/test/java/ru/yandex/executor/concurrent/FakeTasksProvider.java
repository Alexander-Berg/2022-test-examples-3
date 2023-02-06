package ru.yandex.executor.concurrent;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import ru.yandex.function.GenericAutoCloseable;
import ru.yandex.util.timesource.TimeSource;

public class FakeTasksProvider
    implements GenericAutoCloseable<RuntimeException>, TasksProvider<FakeTask>
{
    private final Lock lock = new ReentrantLock();
    private final Condition cond = lock.newCondition();
    private final Logger logger;
    private final FakeTask[] tasks;
    private boolean closed = false;
    private int pos = 0;

    public FakeTasksProvider(final Logger logger, final FakeTask... tasks) {
        this.logger = logger;
        this.tasks = new FakeTask[tasks.length];
        // Copy tasks, so availableAt() will be recalculated and test
        // initialization time won't be accounted in checks
        for (int i = 0; i < tasks.length; ++i) {
            FakeTask task = tasks[i];
            this.tasks[i] = new FakeTask(task);
        }
    }

    @Override
    public void close() {
        lock.lock();
        try {
            closed = true;
            cond.signalAll();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public FakeTask getTask() throws InterruptedException {
        lock.lock();
        try {
            while (!closed) {
                if (pos < tasks.length) {
                    FakeTask task = tasks[pos];
                    long availableAt = task.availableAt();
                    long now = TimeSource.INSTANCE.currentTimeMillis();
                    long delay = availableAt - now;
                    if (delay > 0L) {
                        logger.info(
                            "Task " + task + " is not ready yet, waiting for "
                            + delay + " ms");
                        cond.await(delay, TimeUnit.MILLISECONDS);
                    } else {
                        logger.info("Returning task " + task);
                        ++pos;
                        return task;
                    }
                } else {
                    cond.await();
                }
            }
        } finally {
            lock.unlock();
        }
        return null;
    }

    @Override
    public FakeTask tryGetTask() {
        lock.lock();
        try {
            if (pos < tasks.length) {
                FakeTask task = tasks[pos];
                long availableAt = task.availableAt();
                if (availableAt <= TimeSource.INSTANCE.currentTimeMillis()) {
                    logger.info("Task " + task + " is ready, returning");
                    ++pos;
                    return task;
                }
            }
        } finally {
            lock.unlock();
        }
        logger.info("No tasks available right now");
        return null;
    }

    @Override
    public Lock lock() {
        return lock;
    }
}

