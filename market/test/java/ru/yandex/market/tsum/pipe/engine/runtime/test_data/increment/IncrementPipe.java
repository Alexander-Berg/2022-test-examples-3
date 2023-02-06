package ru.yandex.market.tsum.pipe.engine.runtime.test_data.increment;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import ru.yandex.market.tsum.pipe.engine.definition.builder.JobBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.Pipeline;
import ru.yandex.market.tsum.pipe.engine.definition.builder.PipelineBuilder;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.increment.jobs.IncrementJob;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.increment.resources.IntegerResource;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 16.03.17
 */
@Component
@Configuration
public class IncrementPipe {
    public static final String PIPE_ID = "increment_pipe";
    public static final String JOB_ID = "increment";

    @Bean(name = PIPE_ID)
    public Pipeline pipeline() {
        PipelineBuilder builder = PipelineBuilder.create()
            .withManualResource(IntegerResource.class);

        JobBuilder increment = builder.withJob(IncrementJob.class)
            .withId(JOB_ID);

        return builder.build();
    }
}
