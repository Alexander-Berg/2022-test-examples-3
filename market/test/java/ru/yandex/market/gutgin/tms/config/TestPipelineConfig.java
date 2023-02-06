package ru.yandex.market.gutgin.tms.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;

import ru.yandex.market.gutgin.tms.engine.pipeline.PipelineProcessor;
import ru.yandex.market.gutgin.tms.engine.pipeline.StepProcessorHolder;
import ru.yandex.market.gutgin.tms.engine.pipeline.template.PipelineTemplate;
import ru.yandex.market.gutgin.tms.mocks.MockProcessor;
import ru.yandex.market.gutgin.tms.mocks.TestPipelineType;

@Configuration
@Import(TestTaskConfig.class)
public class TestPipelineConfig {
    private static final Logger log = LogManager.getLogger();


    @Bean
    public Map<TestPipelineType, PipelineTemplate> pipelines(
    ) {
        Map<TestPipelineType, PipelineTemplate> result = new HashMap<>();
        return result;
    }

    @Bean
    public StepProcessorHolder stepProcessors(
        @Lazy PipelineProcessor pipelineProcessor
    ) {
        log.info("Init StepProcessorHolder");
        StepProcessorHolder stepProcessorHolder = new StepProcessorHolder();
        stepProcessorHolder.addStepProcessors(
            ru.yandex.market.gutgin.tms.engine.pipeline.template.steps.wait.Template.class,
            new ru.yandex.market.gutgin.tms.engine.pipeline.template.steps.wait.Processor<>()
        );
        stepProcessorHolder.addStepProcessors(
            ru.yandex.market.gutgin.tms.engine.pipeline.template.steps.dowhile.Template.class,
            new ru.yandex.market.gutgin.tms.engine.pipeline.template.steps.dowhile.Processor<>(
                pipelineProcessor
            )
        );
        stepProcessorHolder.addStepProcessors(
            ru.yandex.market.gutgin.tms.engine.pipeline.template.steps.parallelprocess.Template.class,
            new ru.yandex.market.gutgin.tms.engine.pipeline.template.steps.parallelprocess.Processor<>(
                pipelineProcessor
            )
        );
        stepProcessorHolder.addStepProcessors(
            ru.yandex.market.gutgin.tms.engine.pipeline.template.steps.paralleldata.Template.class,
            new ru.yandex.market.gutgin.tms.engine.pipeline.template.steps.paralleldata.Processor<>(
                pipelineProcessor
            )
        );
        stepProcessorHolder.addStepProcessors(
            ru.yandex.market.gutgin.tms.engine.pipeline.template.steps.process.Template.class,
            new MockProcessor()
        );
        stepProcessorHolder.addStepProcessors(
            ru.yandex.market.gutgin.tms.engine.pipeline.template.steps.start.Template.class,
            new ru.yandex.market.gutgin.tms.engine.pipeline.template.steps.start.Processor<>()
        );
        return stepProcessorHolder;
    }

    @Bean
    public PipelineProcessor pipelineProcessor(@Lazy StepProcessorHolder stepProcessorHolder) {
        log.info("Init PipelineProcessor");
        final PipelineProcessor pipelineProcessor = new PipelineProcessor();
        pipelineProcessor.setStepProcessorHolder(stepProcessorHolder);
        return pipelineProcessor;
    }


}
