package ru.yandex.market.tsum.pipelines.test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.tsum.pipe.engine.definition.Pipeline;
import ru.yandex.market.tsum.pipe.engine.definition.builder.JobBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.builder.PipelineBuilder;
import ru.yandex.market.tsum.pipelines.common.jobs.finish_release.FinishReleaseJob;
import ru.yandex.market.tsum.pipelines.common.jobs.finish_release.FinishReleaseJobConfig;
import ru.yandex.market.tsum.pipelines.common.jobs.issues.BindIssuesToVersionJob;
import ru.yandex.market.tsum.pipelines.common.jobs.merge.resources.MergeJobConfig;
import ru.yandex.market.tsum.pipelines.common.jobs.start.StartJob;
import ru.yandex.market.tsum.pipelines.common.resources.DeliveryPipelineParams;
import ru.yandex.market.tsum.pipelines.common.resources.FixVersionName;
import ru.yandex.market.tsum.pipelines.common.resources.GithubRepo;
import ru.yandex.market.tsum.pipelines.common.resources.StartrekSettings;
import ru.yandex.market.tsum.release.ReleaseJobTags;
import ru.yandex.market.tsum.release.ReleaseStages;

@Configuration
public class TestBindVersionPipeline {
    @Bean(name = "test-bind")
    public Pipeline testBindPipeline() {
        PipelineBuilder builder = PipelineBuilder.create();
        StartrekSettings startrekSettings = new StartrekSettings("MARKETINFRATEST");

        builder.withManualResource(
            DeliveryPipelineParams.class
        );

        JobBuilder startJob = builder.withJob(StartJob.class)
            .beginStage(ReleaseStages.INSTANCE.build())
            .withResources(startrekSettings, new FixVersionName("2018.2.10"))
            .withTitle("Начало релиза");

        JobBuilder bindJob = builder.withJob(BindIssuesToVersionJob.class, "bind")
            .withUpstreams(startJob)
            .withManualTrigger()
            .withResources(
                MergeJobConfig.builder("market-infra/test-pipeline", "master")
                    .build()
            );

        builder.withJob(FinishReleaseJob.class)
            .withUpstreams(bindJob)
            .withTitle("Завершение релиза и подсчет метрик")
            .withTags(ReleaseJobTags.FINAL_JOB)
            .withManualTrigger()
            .withResources(
                new GithubRepo("market-infra/test-pipeline"),
                FinishReleaseJobConfig.builder()
                    .build()
            );

        return builder.build();
    }
}
