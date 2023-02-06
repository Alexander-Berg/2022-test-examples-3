package ru.yandex.market.tsum.pipe.engine.runtime.test_data.increment.jobs;

import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.definition.job.JobExecutor;
import ru.yandex.market.tsum.pipe.engine.definition.resources.Produces;
import ru.yandex.market.tsum.pipe.engine.definition.resources.WiredResource;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.increment.resources.IntegerResource;

import java.util.UUID;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 16.03.17
 */
@Produces(single = IntegerResource.class)
public class IncrementJob implements JobExecutor {
    @WiredResource
    private IntegerResource integer;

    @Override
    public UUID getSourceCodeId() {
        return UUID.fromString("0f7407bc-fdda-4bc4-b6b3-eb207fa92b84");
    }

    @Override
    public void execute(JobContext context) throws Exception {
        context.resources().produce(new IntegerResource(integer.getValue() + 1));
    }
}
