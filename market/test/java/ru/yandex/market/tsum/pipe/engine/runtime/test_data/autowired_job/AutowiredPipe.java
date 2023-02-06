package ru.yandex.market.tsum.pipe.engine.runtime.test_data.autowired_job;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import ru.yandex.market.tsum.pipe.engine.definition.Pipeline;
import ru.yandex.market.tsum.pipe.engine.definition.builder.PipelineBuilder;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 13.04.17
 */
@Configuration
@Component
public class AutowiredPipe {
    public static final String JOB_ID = "job";
    public static final String PIPE_ID = "autowired";

    @Bean(name = PIPE_ID)
    public Pipeline pipeline() {
        PipelineBuilder builder = PipelineBuilder.create();

        builder.withJob(AutowiredJob.class)
            .withId(JOB_ID);

        return builder.build();
    }
}
