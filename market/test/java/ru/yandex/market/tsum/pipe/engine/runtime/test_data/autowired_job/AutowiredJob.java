package ru.yandex.market.tsum.pipe.engine.runtime.test_data.autowired_job;

import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.definition.resources.Produces;
import ru.yandex.market.tsum.pipe.engine.definition.job.JobExecutor;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.resource_injection.resources.Resource451;

import java.util.UUID;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 13.04.17
 */
@Produces(single = Resource451.class)
public class AutowiredJob implements JobExecutor {
    @Autowired
    private Bean451 bean451;

    @Override
    public UUID getSourceCodeId() {
        return UUID.fromString("611e0f8f-fe41-40ac-bddd-4c3b7991fd98");
    }

    @Override
    public void execute(JobContext context) throws Exception {
        context.resources().produce(new Resource451(bean451.getValue()));
    }
}
