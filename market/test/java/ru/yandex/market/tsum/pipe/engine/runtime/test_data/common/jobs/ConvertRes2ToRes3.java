package ru.yandex.market.tsum.pipe.engine.runtime.test_data.common.jobs;

import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.definition.resources.Produces;
import ru.yandex.market.tsum.pipe.engine.definition.job.JobExecutor;
import ru.yandex.market.tsum.pipe.engine.definition.resources.WiredResource;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.common.resources.Res2;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.common.resources.Res3;

import java.util.UUID;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 11.05.2017
 */
@Produces(single = Res3.class)
public class ConvertRes2ToRes3 implements JobExecutor {
    @WiredResource
    private Res2 res2;

    @Override
    public UUID getSourceCodeId() {
        return UUID.fromString("1e922fc5-dc27-477c-8d8f-a0b063047e75");
    }

    @Override
    public void execute(JobContext context) throws Exception {
        context.resources().produce(new Res3(res2.getS() + " " + getClass().getSimpleName()));
    }
}