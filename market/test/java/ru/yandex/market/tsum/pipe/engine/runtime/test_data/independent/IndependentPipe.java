package ru.yandex.market.tsum.pipe.engine.runtime.test_data.independent;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import ru.yandex.market.tsum.pipe.engine.definition.DummyJob;
import ru.yandex.market.tsum.pipe.engine.definition.Pipeline;
import ru.yandex.market.tsum.pipe.engine.definition.builder.PipelineBuilder;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 21.03.17
 */
@Configuration
@Component
public class IndependentPipe {
    public static final String PIPE_NAME = "independent";
    public static final String JOB_PREFIX = "job";
    public static final int JOB_COUNT = 50;

    @Bean(name = PIPE_NAME)
    public Pipeline pipeline() {
        PipelineBuilder builder = PipelineBuilder.create();

        for (int i = 0; i < JOB_COUNT; ++i) {
            builder.withJob(DummyJob.class)
                .withId(JOB_PREFIX + i);
        }

        return builder.build();
    }
}
