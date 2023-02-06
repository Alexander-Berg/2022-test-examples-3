package ru.yandex.market.tsum.pipe.engine.runtime.test_data.common;

import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.definition.job.JobExecutor;

import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class SleepyJob implements JobExecutor {
    @Autowired
    private Semaphore semaphore;

    @Override
    public UUID getSourceCodeId() {
        return UUID.fromString("d2188991-b094-4aec-98bf-051fe7bcc44a");
    }

    @Override
    public void execute(JobContext context) throws Exception {
        semaphore.release();

        context.progress().update(progress -> progress.setText("Sleeping"));
        Thread.sleep(TimeUnit.SECONDS.toMillis(3));

        throw new RuntimeException("I woke up and looking for broken tests");
    }

    @Override
    public boolean interrupt(JobContext context, Thread executorThread) {
        context.progress().update(progress -> progress.setText("Interrupting"));

        executorThread.interrupt();

        try {
            executorThread.join();
        } catch (InterruptedException ignored) {
            return false;
        }

        return true;
    }
}
