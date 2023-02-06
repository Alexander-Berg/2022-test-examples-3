package ru.yandex.market.tsum.pipelines;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.tsum.clients.conductor.ConductorBranch;
import ru.yandex.market.tsum.clients.sandbox.SandboxReleaseType;
import ru.yandex.market.tsum.pipe.engine.definition.Pipeline;
import ru.yandex.market.tsum.pipe.engine.definition.job.Job;
import ru.yandex.market.tsum.pipe.engine.definition.job.JobExecutor;
import ru.yandex.market.tsum.pipelines.common.jobs.conductor_deploy.ConductorDeployJob;
import ru.yandex.market.tsum.pipelines.common.jobs.conductor_deploy.ConductorDeployJobConfig;
import ru.yandex.market.tsum.pipelines.common.jobs.finish_release.FinishReleaseJob;
import ru.yandex.market.tsum.pipelines.common.jobs.merge.MergeJob;
import ru.yandex.market.tsum.pipelines.common.jobs.nanny.NannyReleaseJob;
import ru.yandex.market.tsum.pipelines.common.jobs.start.StartJob;
import ru.yandex.market.tsum.pipelines.common.jobs.teamcity.MarketTeamcityBuildConfig;
import ru.yandex.market.tsum.pipelines.common.jobs.teamcity.MarketTeamcityBuildJob;

/**
 * @author Dmitry Kotelnikov <a href="mailto:kotelnikov@yandex-team.ru"></a>
 * @date 03.08.17
 */
public class CommonJavaPipelinesTest extends Assert {
    @Test
    public void testCreateWithConductorDeploy() throws Exception {
        Pipeline pipeline = CommonJavaPipelineBuilder.create("QUEUE", "/rep/", "job")
            .withTestingStableConductorDeploy()
            .build();
        assertEquals(countJob(pipeline, StartJob.class), 1);
        assertEquals(countJob(pipeline, MergeJob.class), 1);
        assertEquals(countJob(pipeline, MarketTeamcityBuildJob.class), 1);
        assertEquals(countJob(pipeline, ConductorDeployJob.class), 2);
        assertEquals(countJob(pipeline, FinishReleaseJob.class), 1);

        pipeline = CommonJavaPipelineBuilder.create("QUEUE", "/rep/", "job")
            .addConductorDeploy(ConductorBranch.TESTING, TriggerType.AUTO, "package")
            .build();
        assertEquals(countJob(pipeline, ConductorDeployJob.class), 1);
    }

    private int countJob(Pipeline pipeline, Class<? extends JobExecutor> clazz) {
        int count = 0;
        for (Job job : pipeline.getJobs()) {
            if (job.getExecutorClass() == clazz) {
                count++;
            }
        }
        return count;
    }

    @Test
    public void testCreateWithNannyDeploy() throws Exception {
        Pipeline pipeline = CommonJavaPipelineBuilder.create("QUEUE", "/rep/", "job")
            .withNannyDeploy("dashboard")
            .addNannyDeploy(SandboxReleaseType.TESTING, "seq")
            .addNannyDeploy(SandboxReleaseType.STABLE, "seq")
            .build();
        assertEquals(countJob(pipeline, StartJob.class), 1);
        assertEquals(countJob(pipeline, MergeJob.class), 1);
        assertEquals(countJob(pipeline, MarketTeamcityBuildJob.class), 1);
        assertEquals(countJob(pipeline, NannyReleaseJob.class), 2);
        assertEquals(countJob(pipeline, FinishReleaseJob.class), 1);
    }

    @Test
    public void testWithAfterDeployJob() {
        /*
            0. Начало релиза
            1. Создание релизной ветки /rep/
            2. Сборка пакетов
            3. Деплой в unstable
            4. AUTO TEST
            5. Деплой в testing
            6. Деплой в stable
            7. Завершение релиза
        */
        Pipeline pipeline = CommonJavaPipelineBuilder.create("QUEUE", "/rep/", "job")
            .addConductorDeploy(ConductorBranch.UNSTABLE, "PACKAGE")
            .withAfterDeployJob(DeployStageType.UNSTABLE, MarketTeamcityBuildJob.class, builder -> builder
                .withTitle("AUTO TEST")
                .withResources(
                    MarketTeamcityBuildConfig.builder()
                        .withJobName("AUTO_TEST_JOB_NAME")
                        .build()
                )
            )
            .withTestingStableConductorDeploy()
            .build();
        List<Job> jobs = pipeline.getJobs();

        Job unstableDeployJob = jobs.get(3);
        assertEquals(ConductorDeployJob.class,
            unstableDeployJob.getExecutorClass());
        assertEquals(ConductorBranch.UNSTABLE,
            ((ConductorDeployJobConfig) unstableDeployJob.getStaticResources().get(0)).getBranch());

        Job autoTestsJob = jobs.get(4);
        assertEquals(MarketTeamcityBuildJob.class,
            autoTestsJob.getExecutorClass());
        assertEquals("AUTO_TEST_JOB_NAME",
            ((MarketTeamcityBuildConfig) autoTestsJob.getStaticResources().get(0)).getJobName());

        Job testingDeployJob = jobs.get(5);
        assertEquals(ConductorDeployJob.class,
            testingDeployJob.getExecutorClass());
        assertEquals(ConductorBranch.TESTING,
            ((ConductorDeployJobConfig) testingDeployJob.getStaticResources().get(0)).getBranch());

        assertEquals(unstableDeployJob,
            autoTestsJob.getUpstreams().iterator().next().getEntity());
        assertEquals(autoTestsJob,
            testingDeployJob.getUpstreams().iterator().next().getEntity());
    }

    @Test
    public void testDeployingToNannyAndToConductor() {
        Pipeline pipeline = CommonJavaPipelineBuilder.create("STARTREK_QUEUE", "REPOSITORY_FULL_NAME",
                "DEPLOY_JOB_NAME")
            .setModules(Arrays.asList("MODULE_1", "MODULE_2"))
            .withNannyDeploy("NANNY_DASHBOARD_NAME")
            .addNannyDeploy(SandboxReleaseType.TESTING, "TESTING_DEPLOY_SEQ", "MODULE_2_SANDBOX_RESOURCE_TYPE")
            .addConductorDeploy(ConductorBranch.TESTING, "MODULE_1_PACKAGE_NAME")
            .addNannyDeploy(SandboxReleaseType.STABLE, "STABLE_DEPLOY_SEQ", "MODULE_2_SANDBOX_RESOURCE_TYPE")
            .addConductorDeploy(ConductorBranch.STABLE, "MODULE_1_PACKAGE_NAME")
            .build();
        System.out.println();
        assertEquals(
            "Начало релиза\n" +
                "Создание релизной ветки REPOSITORY_FULL_NAME\n" +
                "Сборка пакетов\n" +
                "Деплой в testing: MODULE_1_PACKAGE_NAME\n" +
                "Деплой в testing: MODULE_2_SANDBOX_RESOURCE_TYPE\n" +
                "Тикеты в 'Можно тестировать'\n" +
                "Деплой в stable: MODULE_1_PACKAGE_NAME\n" +
                "Деплой в stable: MODULE_2_SANDBOX_RESOURCE_TYPE\n" +
                "Завершение релиза и подсчет метрик",
            pipeline.getJobs().stream().map(Job::getTitle).collect(Collectors.joining("\n"))
        );
    }
}
