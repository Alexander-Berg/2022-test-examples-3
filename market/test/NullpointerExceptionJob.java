package ru.yandex.market.tsum.pipelines.test;

import java.util.UUID;

import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.definition.job.JobExecutor;

public class NullpointerExceptionJob implements JobExecutor {
    @Override
    public void execute(JobContext context) throws Exception {
        Thread.sleep(3000);
        throw new NullPointerException("I always throw npe");
    }

    @Override
    public UUID getSourceCodeId() {
        return UUID.fromString("9be6f195-2549-475e-b37e-3b516646c4e4");
    }
}
