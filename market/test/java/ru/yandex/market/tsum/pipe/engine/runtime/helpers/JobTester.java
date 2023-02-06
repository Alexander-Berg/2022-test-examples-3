package ru.yandex.market.tsum.pipe.engine.runtime.helpers;

import org.springframework.context.support.GenericApplicationContext;
import ru.yandex.market.tsum.pipe.engine.definition.job.JobExecutor;
import ru.yandex.market.tsum.pipe.engine.runtime.di.ResourceInjector;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 16.01.18
 */
public class JobTester {
    private final ResourceInjector resourceInjector;
    private final GenericApplicationContext applicationContext;

    public JobTester(ResourceInjector resourceInjector, GenericApplicationContext applicationContext) {
        this.resourceInjector = resourceInjector;
        this.applicationContext = applicationContext;
    }

    public <T extends JobExecutor> JobInstanceBuilder<T> jobInstanceBuilder(Class<T> jobType) {
        return new JobInstanceBuilder<>(jobType, applicationContext, resourceInjector);
    }

}

