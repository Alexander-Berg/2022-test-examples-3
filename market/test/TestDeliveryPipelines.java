package ru.yandex.market.tsum.pipelines.test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.tsum.clients.conductor.ConductorBranch;
import ru.yandex.market.tsum.pipe.engine.definition.DummyJob;
import ru.yandex.market.tsum.pipe.engine.definition.Pipeline;
import ru.yandex.market.tsum.pipe.engine.definition.builder.JobBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.builder.PipelineBuilder;
import ru.yandex.market.tsum.pipelines.common.jobs.conductor_deploy.ConductorDeployJob;
import ru.yandex.market.tsum.pipelines.common.jobs.conductor_deploy.ConductorDeployJobConfig;
import ru.yandex.market.tsum.pipelines.common.jobs.delivery.arcadia.GenerateArcadiaChangelogJob;
import ru.yandex.market.tsum.pipelines.common.jobs.finish_release.FinishReleaseJob;
import ru.yandex.market.tsum.pipelines.common.jobs.finish_release.FinishReleaseJobConfig;
import ru.yandex.market.tsum.pipelines.common.jobs.issues.BindIssuesToVersionJob;
import ru.yandex.market.tsum.pipelines.common.jobs.issues.ParseTicketsListFromChangelog;
import ru.yandex.market.tsum.pipelines.common.jobs.release.CreateReleaseJob;
import ru.yandex.market.tsum.pipelines.common.jobs.rollback.ConductorRollbackJob;
import ru.yandex.market.tsum.pipelines.common.jobs.rollback.RollbackJobConfig;
import ru.yandex.market.tsum.pipelines.common.jobs.teamcity.MarketTeamcityBuildConfig;
import ru.yandex.market.tsum.pipelines.common.jobs.teamcity.MarketTeamcityBuildJob;
import ru.yandex.market.tsum.pipelines.common.resources.DeliveryPipelineParams;
import ru.yandex.market.tsum.pipelines.common.resources.GithubRepo;
import ru.yandex.market.tsum.pipelines.common.resources.ModulesList;
import ru.yandex.market.tsum.pipelines.common.resources.StartrekSettings;
import ru.yandex.market.tsum.pipelines.idx.IdxUtils;
import ru.yandex.market.tsum.release.ReleaseStages;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 02.04.18
 */
@Configuration
public class TestDeliveryPipelines {
    public static final String ARCADIA_PIPE_ID = "test-arcadia-delivery-pipeline";
    public static final String GITHUB_PIPE_ID = "test-github-delivery-pipeline";
    public static final String DEPLOY_STABLE_JOB_ID = "deploy-stable";

    @Bean(name = GITHUB_PIPE_ID)
    public Pipeline githubPipeline() {
        ReleaseStages stages = ReleaseStages.INSTANCE;

        PipelineBuilder builder = PipelineBuilder.create();
        builder.withManualResource(DeliveryPipelineParams.class);

        JobBuilder start = builder.withJob(DummyJob.class)
            .withTitle("Старт")
            .beginStage(stages.build());

        JobBuilder buildPackages = builder.withJob(MarketTeamcityBuildJob.class)
            .withTitle("Сборка пакетов")
            .withResources(
                MarketTeamcityBuildConfig.builder()
                    .withJobName("MarketInfra_Sandbox_MarketInfraSandboxTestPipelineBuildV3")
                    .setModulesToBuild(new ModulesList("test-pipeline-foo"))
                    .build()
            )
            .withManualTrigger()
            .withUpstreams(start);

        JobBuilder deployJob = builder.withJob(ConductorDeployJob.class)
            .withId(DEPLOY_STABLE_JOB_ID)
            .withTitle("Деплой в прод")
            .withUpstreams(buildPackages)
            .withResources(
                ConductorDeployJobConfig.newBuilder(ConductorBranch.UNSTABLE)
                    .build()
            );

        builder.withJob(FinishReleaseJob.class)
            .withTitle("Завершение релиза и подсчет метрик")
            .withResources(
                FinishReleaseJobConfig.DO_NOTHING_CONFIG,
                new GithubRepo("market-infra/test-pipeline")
            )
            .withUpstreams(deployJob);

        return builder.build();
    }

    @Bean(name = GITHUB_PIPE_ID + "-rollback")
    public Pipeline githubRollbackPipeline() {
        ReleaseStages stages = ReleaseStages.INSTANCE;

        PipelineBuilder builder = PipelineBuilder.create();
        builder.withManualResource(DeliveryPipelineParams.class);

        JobBuilder start = builder.withJob(DummyJob.class, "start")
            .beginStage(stages.build())
            .withTitle("Начало ролбека гитхабовой сборки");

        JobBuilder deploy = builder.withJob(ConductorRollbackJob.class, DEPLOY_STABLE_JOB_ID)
            .withUpstreams(start)
            .withResources(RollbackJobConfig.builder().withJobId(DEPLOY_STABLE_JOB_ID).build())
            .withTitle("Деплой в стейбл");

        builder.withJob(FinishReleaseJob.class, "finish-release")
            .withUpstreams(deploy)
            .withResources(FinishReleaseJobConfig.DO_NOTHING_CONFIG)
            .withResources(new GithubRepo("market-infra/test-pipeline"))
            .withTitle("Завершение релиза");

        return builder.build();
    }

    @Bean(name = ARCADIA_PIPE_ID)
    public Pipeline arcadiaPipeline() {
        ReleaseStages stages = ReleaseStages.INSTANCE;

        PipelineBuilder builder = PipelineBuilder.create();
        builder.withManualResource(DeliveryPipelineParams.class);

        JobBuilder generateChangelog = builder.withJob(GenerateArcadiaChangelogJob.class)
            .beginStage(stages.precheck())
            .withTitle("Генерация ченджлога")
            .withResources(
                GenerateArcadiaChangelogJob.configBuilder()
                    .withModulePath("market/report")
                    .build()
            );

        JobBuilder createRelease = builder.withJob(CreateReleaseJob.class)
            .beginStage(stages.build())
            .withUpstreams(generateChangelog)
            .withTitle("Создание релиза")
            .withResources(new StartrekSettings("MARKETINFRATEST"));

        JobBuilder parseTicketsList = builder.withJob(ParseTicketsListFromChangelog.class)
            .withTitle("Парсинг тикетов из ченджлога")
            .withUpstreams(createRelease);

        JobBuilder setFixVersionJob = builder.withJob(BindIssuesToVersionJob.class)
            .withTitle("Прикрепляем тикеты к релизу")
            .withResources(IdxUtils.IGNORED_ISSUES)
            .withUpstreams(parseTicketsList);


        builder.withJob(FinishReleaseJob.class)
            .beginStage(stages.stable())
            .withTitle("Завершение релиза и подсчет метрик")
            .withResources(
                FinishReleaseJobConfig.builder()
                    .withCloseFeatureIssues(true)
                    .withCloseResolvedIssues(true)
                    .withMergeGitHubPullRequests(false)
                    .build()
            )
            .withUpstreams(setFixVersionJob);

        return builder.build();
    }
}
