package ru.yandex.market.tsum.pipe.engine.runtime.test_data.simple;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import ru.yandex.market.tsum.pipe.engine.definition.Pipeline;
import ru.yandex.market.tsum.pipe.engine.definition.builder.PipelineBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.DummyJob;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 07.03.17
 */
@Configuration
@Component
public class SimplePipe {
    public static final String JOB_ID = "dummy";
    public static final String PIPE_ID = "simple";

    @Bean(name = PIPE_ID)
    public Pipeline pipeline() {
        PipelineBuilder builder = PipelineBuilder.create();

        builder.withJob(DummyJob.class)
            .withId(JOB_ID);

        return builder.build();
    }
}
