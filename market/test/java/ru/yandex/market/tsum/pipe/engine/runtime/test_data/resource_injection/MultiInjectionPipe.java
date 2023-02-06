package ru.yandex.market.tsum.pipe.engine.runtime.test_data.resource_injection;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import ru.yandex.market.tsum.pipe.engine.definition.builder.JobBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.Pipeline;
import ru.yandex.market.tsum.pipe.engine.definition.builder.PipelineBuilder;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.resource_injection.jobs.MultiResource451SumJob;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.resource_injection.jobs.Producer451Job;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.resource_injection.jobs.ProducerDerived451Job;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 22.03.17
 */
@Configuration
@Component
public class MultiInjectionPipe {
    public static final String PIPE_NAME = "multi_injection";
    public static final String SUMMATOR_JOB_ID = "summator";

    @Bean(name = PIPE_NAME)
    public Pipeline getPipeline() {
        PipelineBuilder builder = PipelineBuilder.create();

        JobBuilder producer1 = builder.withJob(Producer451Job.class);

        JobBuilder producer2 = builder.withJob(ProducerDerived451Job.class)
            .withUpstreams(producer1);

        JobBuilder producer3 = builder.withJob(Producer451Job.class)
            .withUpstreams(producer1);

        builder.withJob(MultiResource451SumJob.class)
            .withId(SUMMATOR_JOB_ID)
            .withUpstreams(producer2, producer3);

        return builder.build();
    }
}
