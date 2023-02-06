package ru.yandex.market.tsum.pipe.engine.runtime.test_data.common.jobs;

import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.definition.resources.Produces;
import ru.yandex.market.tsum.pipe.engine.definition.job.JobExecutor;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.common.resources.Res1;

import java.util.UUID;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 11.04.2018
 */
@Produces(single = Res1.class)
public class ProduceRes1AndFail implements JobExecutor {
    @Override
    public UUID getSourceCodeId() {
        return UUID.fromString("661eb30b-de61-4823-8137-cf0e69535a0a");
    }

    @Override
    public void execute(JobContext context) {
        context.resources().produce(new Res1(""));
        throw new RuntimeException();
    }
}
