package ru.yandex.market.tsum.pipe.engine.runtime.test_data.resource_injection.jobs;

import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.definition.job.JobExecutor;
import ru.yandex.market.tsum.pipe.engine.definition.resources.Produces;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.resource_injection.resources.DerivedResource451;

import java.util.UUID;

@Produces(single = {DerivedResource451.class})
public class ProducerDerived451Job implements JobExecutor {
    @Override
    public UUID getSourceCodeId() {
        return UUID.fromString("ab66ec32-1f68-4092-b812-bd37cf305bca");
    }

    @Override
    public void execute(JobContext context) throws Exception {
        context.resources().produce(new DerivedResource451(451));
    }
}
