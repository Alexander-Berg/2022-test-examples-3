package ru.yandex.market.tsum.pipe.engine.runtime.test_data.common.jobs;

import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.definition.resources.Produces;
import ru.yandex.market.tsum.pipe.engine.definition.job.JobExecutor;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.common.resources.Res1;

import java.util.UUID;

/**
 * @author Nikolay FIrov
 * @date 15.12.17
 */
@Produces(single = Res1.class)
public class JobThatShouldProduceRes1ButFailsInstead implements JobExecutor {
    @Override
    public UUID getSourceCodeId() {
        return UUID.fromString("3138788c-703d-409f-962b-69fa91d83b67");
    }

    @Override
    public void execute(JobContext context) throws Exception {
        throw new RuntimeException("test");
    }
}
