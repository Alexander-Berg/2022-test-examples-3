package ru.yandex.market.tsum.pipe.engine.runtime.test_data.common;

import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.definition.job.JobExecutor;

import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class StuckJob implements JobExecutor {
    @Autowired
    private Semaphore semaphore;

    @Override
    public UUID getSourceCodeId() {
        return UUID.fromString("8c9ed3b1-5885-4489-80de-a70a199b406c");
    }

    @Override
    public void execute(JobContext context) throws Exception {
        semaphore.release();

        context.progress().update(progress -> progress.setText("Sleeping"));
        Thread.sleep(TimeUnit.SECONDS.toMillis(10));

        throw new RuntimeException("I woke up and looking for broken tests");
    }

    @Override
    public boolean interrupt(JobContext context, Thread executorThread) {
        semaphore.release();

        context.progress().update(progress -> progress.setText("Interrupting"));
        return true;
    }
}
