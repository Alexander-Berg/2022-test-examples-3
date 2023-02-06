package ru.yandex.market.tsum.pipelines.test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.tsum.pipe.engine.definition.Pipeline;
import ru.yandex.market.tsum.pipe.engine.definition.builder.JobBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.builder.PipelineBuilder;
import ru.yandex.market.tsum.pipelines.common.jobs.finish_release.FinishReleaseJob;
import ru.yandex.market.tsum.pipelines.common.jobs.release.CleanupReleaseJob;
import ru.yandex.market.tsum.pipelines.common.jobs.release.CreateReleaseJob;
import ru.yandex.market.tsum.pipelines.common.resources.StartrekSettings;

/**
 * @author Ilya Sapachev <a href="mailto:sid-hugo@yandex-team.ru"></a>
 * @date 27.06.18
 */
@Configuration
public class TestCleanupReleasePipeline {
    public static final String PIPE_ID = "test-cleanup-release";

    @Bean(name = PIPE_ID)
    public Pipeline pipeline() {
        PipelineBuilder builder = PipelineBuilder.create();

        JobBuilder createRelease = builder.withJob(CreateReleaseJob.class)
            .withTitle("Создание релиза")
            .withResources(new StartrekSettings("MARKETINFRATEST"));

        JobBuilder closeIssueJob = builder.withJob(FinishReleaseJob.class)
            .withTitle("Закрыть релиз")
            .withManualTrigger()
            .withUpstreams(createRelease);

        JobBuilder canelIssueJob = builder.withJob(CleanupReleaseJob.class)
            .withTitle("Отменить релиз")
            .withManualTrigger()
            .withUpstreams(createRelease);

        return builder.build();
    }
}
