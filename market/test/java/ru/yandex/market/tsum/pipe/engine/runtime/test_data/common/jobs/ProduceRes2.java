package ru.yandex.market.tsum.pipe.engine.runtime.test_data.common.jobs;

import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.definition.resources.Produces;
import ru.yandex.market.tsum.pipe.engine.definition.job.JobExecutor;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.common.resources.Res2;

import java.util.UUID;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 11.05.2017
 */
@Produces(single = Res2.class)
public class ProduceRes2 implements JobExecutor {
    @Override
    public UUID getSourceCodeId() {
        return UUID.fromString("4e33ad71-ff16-419a-a3a8-dd2d07c0e7fb");
    }

    @Override
    public void execute(JobContext context) throws Exception {
        context.resources().produce(new Res2(getClass().getSimpleName()));
    }
}
