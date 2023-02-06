package ru.yandex.market.tsum.pipe.engine.runtime.test_data.common.jobs;

import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.definition.resources.Produces;
import ru.yandex.market.tsum.pipe.engine.definition.job.JobExecutor;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.common.resources.Res1;

import java.util.UUID;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 11.05.2017
 */
@Produces(single = Res1.class)
public class ProduceRes1 implements JobExecutor {
    @Override
    public UUID getSourceCodeId() {
        return UUID.fromString("580de241-8b23-4705-8960-c679f5c422d1");
    }

    @Override
    public void execute(JobContext context) throws Exception {
        context.resources().produce(new Res1(getClass().getSimpleName()));
    }
}
