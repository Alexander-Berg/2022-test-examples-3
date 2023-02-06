package ru.yandex.market.tsum.pipelines.test;

import java.util.Arrays;
import java.util.UUID;

import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.definition.context.impl.SupportType;
import ru.yandex.market.tsum.pipe.engine.definition.job.JobExecutor;

public class FailingJob implements JobExecutor {
    @Override
    public void execute(JobContext context) throws Exception {
        Thread.sleep(3000);
        context.actions().failJob("I always fail", Arrays.asList(SupportType.NONE, SupportType.NANNY));
    }

    @Override
    public UUID getSourceCodeId() {
        return UUID.fromString("0461426d-5a5b-4a52-a97a-8e61a54e1880");
    }
}
