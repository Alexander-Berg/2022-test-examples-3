package ru.yandex.market.tsum.pipelines.test;

import java.util.UUID;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.tsum.pipe.engine.definition.DummyJob;
import ru.yandex.market.tsum.pipe.engine.definition.Pipeline;
import ru.yandex.market.tsum.pipe.engine.definition.builder.JobBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.builder.PipelineBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.definition.job.JobExecutor;
import ru.yandex.market.tsum.pipe.engine.definition.resources.WiredResource;
import ru.yandex.market.tsum.pipelines.common.resources.FixVersionName;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 14.03.2018
 */
@Configuration
public class UiTestsPipelines {
    public static final String SIMPLE = "ui-tests-simple";

    /**
     * У этого пайплайна FixVersionName в ручных ресурсах и FixVersionTitleProvider, это позволяет UI-тестам называть
     * релизы так, как им хочется.
     */
    @Bean(name = SIMPLE)
    public Pipeline simplePipeline() {
        PipelineBuilder builder = PipelineBuilder.create();
        builder.withManualResource(FixVersionName.class);

        JobBuilder job1 = builder.withJob(FixVersionNameConsumer.class).withId("job1");

        builder.withJob(DummyJob.class)
            .withUpstreams(job1)
            .withManualTrigger();

        return builder.build();
    }

    public static class FixVersionNameConsumer implements JobExecutor {
        @WiredResource
        private FixVersionName fixVersionName;

        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("ab44f669-edbc-498e-91d3-2bbb9dde365f");
        }

        @Override
        public void execute(JobContext context) throws Exception {
        }
    }

}
