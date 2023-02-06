package ru.yandex.market.tsum.pipe.engine.runtime.test_data.resource_injection;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import ru.yandex.market.tsum.pipe.engine.definition.builder.JobBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.Pipeline;
import ru.yandex.market.tsum.pipe.engine.definition.builder.PipelineBuilder;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.resource_injection.jobs.Producer451Job;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.resource_injection.jobs.Resource451DoubleJob;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 22.03.17
 */
@Configuration
@Component
public class SimpleInjectionPipe {
    public static final String PIPE_NAME = "simple_injection";
    public static final String DOUBLER_JOB_ID = "downstream";

    @Bean(name = PIPE_NAME)
    public Pipeline getPipeline() {
        PipelineBuilder builder = PipelineBuilder.create();

        JobBuilder producer = builder.withJob(Producer451Job.class);

        builder.withJob(Resource451DoubleJob.class)
            .withId(DOUBLER_JOB_ID)
            .withUpstreams(producer);

        return builder.build();
    }
}
