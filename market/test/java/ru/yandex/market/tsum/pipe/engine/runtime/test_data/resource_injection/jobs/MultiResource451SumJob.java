package ru.yandex.market.tsum.pipe.engine.runtime.test_data.resource_injection.jobs;

import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.definition.job.JobExecutor;
import ru.yandex.market.tsum.pipe.engine.definition.resources.Produces;
import ru.yandex.market.tsum.pipe.engine.definition.resources.WiredResourceList;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.resource_injection.resources.Pipe451Result;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.resource_injection.resources.Resource451;

import java.util.List;
import java.util.UUID;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 22.03.17
 */
@Produces(single = Pipe451Result.class)
public class MultiResource451SumJob implements JobExecutor {
    @WiredResourceList(Resource451.class)
    private List<Resource451> resources;

    @Override
    public UUID getSourceCodeId() {
        return UUID.fromString("3c119195-56ee-4894-81b6-1f0ac95f05c2");
    }

    @Override
    public void execute(JobContext context) throws Exception {
        int sum = resources.stream().mapToInt(Resource451::getValue).sum();
        context.resources().produce(new Pipe451Result(sum));
    }
}
