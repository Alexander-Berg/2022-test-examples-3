package ru.yandex.market.tsum.pipe.engine.runtime.test_data.resource_injection.jobs;

import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.definition.job.JobExecutor;
import ru.yandex.market.tsum.pipe.engine.definition.resources.Produces;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.resource_injection.resources.Resource451;

import java.util.UUID;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 22.03.17
 */
@Produces(single = {Resource451.class})
public class Producer451Job implements JobExecutor {
    @Override
    public UUID getSourceCodeId() {
        return UUID.fromString("b779b990-15db-415f-8080-15117972a0af");
    }

    @Override
    public void execute(JobContext context) throws Exception {
        context.resources().produce(new Resource451(451));
    }
}
