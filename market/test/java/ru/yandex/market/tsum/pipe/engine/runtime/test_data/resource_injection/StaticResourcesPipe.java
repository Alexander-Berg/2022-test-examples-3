package ru.yandex.market.tsum.pipe.engine.runtime.test_data.resource_injection;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import ru.yandex.market.tsum.pipe.engine.definition.Pipeline;
import ru.yandex.market.tsum.pipe.engine.definition.builder.PipelineBuilder;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.resource_injection.jobs.Resource451DoubleJob;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.resource_injection.resources.Resource451;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 29.03.17
 */
@Configuration
@Component
public class StaticResourcesPipe {
    public static final String PIPE_NAME = "static_resources";
    public static final String DOUBLER_JOB_ID = "doubler";

    @Bean(name = PIPE_NAME)
    public Pipeline getPipeline() {
        PipelineBuilder builder = PipelineBuilder.create();

        builder.withJob(Resource451DoubleJob.class)
            .withId(DOUBLER_JOB_ID)
            .withResources(new Resource451(451));

        return builder.build();
    }
}
