package ru.yandex.market.tsum.pipe.engine.runtime.test_data.common.jobs;

import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.definition.job.JobExecutor;
import ru.yandex.market.tsum.pipe.engine.definition.resources.Produces;
import ru.yandex.market.tsum.pipe.engine.definition.resources.WiredResource;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.common.resources.Res1;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.common.resources.Res2;

import java.util.UUID;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 11.05.2017
 */
@Produces(single = Res2.class)
public class ConvertRes1ToRes2 implements JobExecutor {
    @WiredResource
    private Res1 res1;

    @Override
    public UUID getSourceCodeId() {
        return UUID.fromString("cde10ba4-8bc3-4be4-8d89-60771a23031a");
    }

    @Override
    public void execute(JobContext context) throws Exception {
        context.resources().produce(new Res2(res1.getS() + " " + getClass().getSimpleName()));
    }
}