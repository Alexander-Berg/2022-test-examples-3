package ru.yandex.market.tsum.pipe.engine.runtime.test_data.diamond;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import ru.yandex.market.tsum.pipe.engine.definition.builder.JobBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.Pipeline;
import ru.yandex.market.tsum.pipe.engine.definition.builder.PipelineBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.DummyJob;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.common.jobs.ConvertRes1ToRes2;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.common.jobs.ProduceRes1;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 20.03.17
 */
@Configuration
@Component
public class DiamondPipeWithResources {
    public static final String PIPE_NAME = "diamond";
    public static final String START_JOB = "start";
    public static final String TOP_JOB = "top";
    public static final String BOTTOM_JOB = "bottom";
    public static final String END_JOB = "end";

    @Bean(name = PIPE_NAME)
    public Pipeline pipeline() {
        PipelineBuilder builder = PipelineBuilder.create();

        JobBuilder start = builder.withJob(ProduceRes1.class)
            .withId(START_JOB);

        JobBuilder top = builder.withJob(DummyJob.class)
            .withId(TOP_JOB)
            .withUpstreams(start);

        JobBuilder bottom = builder.withJob(DummyJob.class)
            .withId(BOTTOM_JOB)
            .withUpstreams(start);

        JobBuilder end = builder.withJob(ConvertRes1ToRes2.class)
            .withId(END_JOB)
            .withUpstreams(top, bottom);

        return builder.build();
    }
}
