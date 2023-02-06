package ru.yandex.market.tsum.pipe.engine.runtime.test_data.common;

import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.definition.job.JobExecutor;

import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class BadInterruptJob implements JobExecutor {
    @Autowired
    private Semaphore semaphore;

    @Override
    public UUID getSourceCodeId() {
        return UUID.fromString("10c60e63-7a6c-402a-911d-ebeb40063a82");
    }

    @Override
    public void execute(JobContext context) throws Exception {
        semaphore.release();

        context.progress().update(progress -> progress.setText("Sleeping"));
        Thread.sleep(TimeUnit.SECONDS.toMillis(2));
        context.progress().update(progress -> progress.setText("Woke up"));

        throw new RuntimeException("I woke up and looking for broken tests");
    }

    @Override
    public boolean interrupt(JobContext context, Thread executorThread) {
        throw new RuntimeException("I am stuck!");
    }
}
