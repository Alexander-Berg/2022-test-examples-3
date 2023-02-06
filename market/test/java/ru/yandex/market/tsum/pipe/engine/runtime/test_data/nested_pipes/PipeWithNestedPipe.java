package ru.yandex.market.tsum.pipe.engine.runtime.test_data.nested_pipes;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import ru.yandex.market.tsum.pipe.engine.definition.builder.NestedPipelineBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.Pipeline;
import ru.yandex.market.tsum.pipe.engine.definition.builder.PipelineBuilder;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.common.jobs.ConvertRes1ToRes2;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.common.jobs.ConvertRes2ToRes3;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.common.resources.Res1;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 10.05.2017
 */
@Configuration
@Component
public class PipeWithNestedPipe {
    public static final String JOB_ID = "nested_pipe_job";
    public static final String PARENT_PIPE_NAME = "pipe_with_nested_pipe";

    @Bean(name = PARENT_PIPE_NAME)
    public Pipeline pipeline() {
        PipelineBuilder builder = PipelineBuilder.create()
            .withManualResource(Res1.class);

        NestedPipelineBuilder nestedPipeline = builder.withNestedPipeline(nestedPipeline());

        builder.withJob(ConvertRes2ToRes3.class)
            .withUpstreams(nestedPipeline)
            .withId(JOB_ID);

        return builder.build();
    }

    public Pipeline nestedPipeline() {
        PipelineBuilder builder = PipelineBuilder.create();
        builder.withJob(ConvertRes1ToRes2.class);
        return builder.build();
    }
}
