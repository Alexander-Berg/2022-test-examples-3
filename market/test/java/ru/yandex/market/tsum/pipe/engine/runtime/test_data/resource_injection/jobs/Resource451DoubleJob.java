package ru.yandex.market.tsum.pipe.engine.runtime.test_data.resource_injection.jobs;

import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.definition.job.JobExecutor;
import ru.yandex.market.tsum.pipe.engine.definition.resources.Produces;
import ru.yandex.market.tsum.pipe.engine.definition.resources.WiredResource;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.resource_injection.resources.Pipe451Result;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.resource_injection.resources.Resource451;

import java.util.UUID;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 22.03.17
 */
@Produces(single = Pipe451Result.class)
public class Resource451DoubleJob implements JobExecutor {
    @WiredResource
    private Resource451 resource451;

    @Override
    public UUID getSourceCodeId() {
        return UUID.fromString("8925a387-a35f-4e5e-93d4-3f15b51aa0ad");
    }

    @Override
    public void execute(JobContext context) throws Exception {
        context.resources().produce(new Pipe451Result(resource451.getValue() * 2));
    }
}
