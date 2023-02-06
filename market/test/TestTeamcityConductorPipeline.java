package ru.yandex.market.tsum.pipelines.test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.tsum.clients.conductor.ConductorBranch;
import ru.yandex.market.tsum.pipe.engine.definition.Pipeline;
import ru.yandex.market.tsum.pipe.engine.definition.builder.JobBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.builder.PipelineBuilder;
import ru.yandex.market.tsum.pipelines.common.jobs.aqua.AquaJob;
import ru.yandex.market.tsum.pipelines.common.jobs.aqua.AquaJobConfig;
import ru.yandex.market.tsum.pipelines.common.jobs.conductor_deploy.ConductorDeployJob;
import ru.yandex.market.tsum.pipelines.common.jobs.conductor_deploy.ConductorDeployJobConfig;
import ru.yandex.market.tsum.pipelines.common.jobs.finish_release.FinishReleaseJob;
import ru.yandex.market.tsum.pipelines.common.jobs.finish_release.FinishReleaseJobConfig;
import ru.yandex.market.tsum.pipelines.common.jobs.merge.MergeJob;
import ru.yandex.market.tsum.pipelines.common.jobs.merge.resources.MergeJobConfig;
import ru.yandex.market.tsum.pipelines.common.jobs.start.StartJob;
import ru.yandex.market.tsum.pipelines.common.jobs.teamcity.MarketTeamcityBuildConfig;
import ru.yandex.market.tsum.pipelines.common.jobs.teamcity.MarketTeamcityBuildJob;
import ru.yandex.market.tsum.pipelines.common.resources.DeliveryPipelineParams;
import ru.yandex.market.tsum.pipelines.common.resources.FixVersionName;
import ru.yandex.market.tsum.pipelines.common.resources.GithubRepo;
import ru.yandex.market.tsum.pipelines.common.resources.StartrekSettings;
import ru.yandex.market.tsum.release.ReleaseJobTags;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 13.04.17
 */
@Configuration
public class TestTeamcityConductorPipeline {
    public static final String TEST_DELIVERY_TEAMCITY_CONDUCTOR_ID = "test-delivery-teamcity-conductor";

    @Bean(name = "test-teamcity-conductor")
    public Pipeline testTeamcityConductor() {
        PipelineBuilder builder = PipelineBuilder.create();

        JobBuilder build = builder.withJob(MarketTeamcityBuildJob.class)
            .withResources(
                MarketTeamcityBuildConfig.builder()
                    .withJobName("MarketInfra_Sandbox_TestPipelineBuild_v2")
                    .build()
            );

        JobBuilder deployUnstable = builder.withJob(ConductorDeployJob.class)
            .withUpstreams(build)
            .withManualTrigger()
            .withResources(new ConductorDeployJobConfig(ConductorBranch.UNSTABLE));

        return builder.build();
    }

    @Bean(name = TEST_DELIVERY_TEAMCITY_CONDUCTOR_ID)
    public Pipeline testDeliveryTeamcityConductor() {
        PipelineBuilder builder = PipelineBuilder.create();

        builder.withManualResource(
            DeliveryPipelineParams.class
        );

        JobBuilder build = builder.withJob(MarketTeamcityBuildJob.class)
            .withResources(
                MarketTeamcityBuildConfig.builder()
                    .withJobName("MarketInfra_Sandbox_TestPipelineBuild_v2")
                    .build()
            );

        JobBuilder deployUnstable = builder.withJob(ConductorDeployJob.class)
            .withUpstreams(build)
            .withManualTrigger()
            .withResources(new ConductorDeployJobConfig(ConductorBranch.UNSTABLE));

        return builder.build();
    }

    @Bean(name = "start-merge-end")
    @SuppressWarnings("checkstyle:magicnumber")
    public Pipeline startMergeEnd() {
        PipelineBuilder builder = PipelineBuilder.create();
        StartrekSettings startrekSettings = new StartrekSettings("MARKETINFRATEST");

        builder.withManualResource(
            FixVersionName.class
        );

        JobBuilder startJob = builder.withJob(StartJob.class)
            .withResources(startrekSettings)
            .withTitle("Начало релиза");

        JobBuilder mergeJob = builder.withJob(MergeJob.class)
            .withUpstreams(startJob)
            .withManualTrigger()
            .withResources(
                MergeJobConfig.builder("market-infra/test-pipeline", "master")
                    .build()
            );

        builder.withJob(AquaJob.class)
            .withUpstreams(startJob)
            .withManualTrigger()
            .withResources(
                AquaJobConfig.builder("589053a1e4b04506a0ad7b65")
                    .withTimeoutMinutes(5)
                    .build()
            );

        builder.withJob(FinishReleaseJob.class)
            .withUpstreams(mergeJob)
            .withTitle("Завершение релиза и подсчет метрик")
            .withTags(ReleaseJobTags.FINAL_JOB)
            .withManualTrigger()
            .withResources(
                new GithubRepo("market-infra/test-pipeline"),
                FinishReleaseJobConfig.builder()
                    .withCreateGithubReleaseTags(true)
                    .build()
            );

        return builder.build();
    }
}
