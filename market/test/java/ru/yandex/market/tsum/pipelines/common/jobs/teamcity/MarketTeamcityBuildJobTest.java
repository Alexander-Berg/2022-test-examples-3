package ru.yandex.market.tsum.pipelines.common.jobs.teamcity;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.fest.assertions.Assertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.tsum.clients.arcadia.ArcArcadiaClient;
import ru.yandex.market.tsum.clients.teamcity.TeamcityBuilder;
import ru.yandex.market.tsum.core.notify.common.NotificationCenter;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.definition.job.JobExecutor;
import ru.yandex.market.tsum.pipe.engine.runtime.config.JobTesterConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.JobTester;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.TestJobContext;
import ru.yandex.market.tsum.pipe.engine.runtime.notifications.Notificator;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.JobLaunch;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.JobState;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.StatusChange;
import ru.yandex.market.tsum.pipelines.common.resources.ConductorPackage;
import ru.yandex.market.tsum.pipelines.common.resources.DeliveryPipelineParams;
import ru.yandex.market.tsum.pipelines.common.resources.ModulesList;
import ru.yandex.market.tsum.pipelines.common.resources.SandboxResource;

import static org.mockito.ArgumentMatchers.eq;

/**
 * @author chmilevfa@yandex-team.ru
 * @since 23.11.17
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {MarketTeamcityBuildJobTest.Config.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class MarketTeamcityBuildJobTest {
    private static final String JOB_NAME = "SomeTeamcityJob";

    @Autowired
    private JobTester jobTester;

    @Autowired
    private TeamcityBuilder teamcityBuilder;

    @Before
    public void setUp() {
        Mockito.when(teamcityBuilder.getArtifact(Mockito.any(), Mockito.any())).thenReturn("{packages:[]}");
    }

    @Test
    public void simpleRun() throws Exception {
        JobExecutor sut = jobTester.jobInstanceBuilder(MarketTeamcityBuildJob.class)
            .withResource(
                MarketTeamcityBuildConfig.builder().withJobName(JOB_NAME).build()
            )
            .create();

        TestJobContext jobContext = getJobContext();
        sut.execute(jobContext);

        TeamcityBuildConfig teamcityBuildConfig = captureTeamcityBuildConfig(jobContext);
        Assert.assertEquals(JOB_NAME, teamcityBuildConfig.getJobName());
        Assert.assertNull(teamcityBuildConfig.toParametersMap().get("CHANGELOG_START_COMMIT_HASH"));
    }

    @Test
    public void changelogStartCommitHashPassing() throws Exception {
        String currentRevision = "current_revision";
        String stableRevision = "stable_revision";

        JobExecutor sut = jobTester.jobInstanceBuilder(MarketTeamcityBuildJob.class)
            .withResources(
                MarketTeamcityBuildConfig.builder().withJobName(JOB_NAME).build(),
                new DeliveryPipelineParams(currentRevision, stableRevision, stableRevision)
            )
            .create();

        TestJobContext jobContext = getJobContext();
        sut.execute(jobContext);

        TeamcityBuildConfig teamcityBuildConfig = captureTeamcityBuildConfig(jobContext);
        Assert.assertEquals(JOB_NAME, teamcityBuildConfig.getJobName());
        Assert.assertEquals(stableRevision, teamcityBuildConfig.toParametersMap().get("CHANGELOG_START_COMMIT_HASH"));
        Assert.assertEquals(currentRevision, teamcityBuildConfig.getVcsRevision());
    }

    @Test
    public void testEmptyPackagesCreated() throws Exception {
        Mockito.when(teamcityBuilder.getArtifact(Mockito.any(), Mockito.any())).thenReturn("{packages:[]}");

        JobExecutor sut = jobTester.jobInstanceBuilder(MarketTeamcityBuildJob.class)
            .withResource(MarketTeamcityBuildConfig.builder().withJobName(JOB_NAME).build())
            .create();

        TestJobContext jobContext = getJobContext();
        sut.execute(jobContext);

        List<ConductorPackage> conductorPackages = jobContext.getProducedResourcesList().stream()
            .filter(resource -> resource instanceof ConductorPackage)
            .map(resource -> (ConductorPackage) resource)
            .collect(Collectors.toList());
        List<SandboxResource> sandboxPackages = jobContext.getProducedResourcesList().stream()
            .filter(resource -> resource instanceof SandboxResource)
            .map(resource -> (SandboxResource) resource)
            .collect(Collectors.toList());

        Assertions.assertThat(conductorPackages).isEmpty();
        Assertions.assertThat(sandboxPackages).isEmpty();
    }

    @Test
    public void testComplexPackagesParsing() throws Exception {
        Mockito.when(teamcityBuilder.getArtifact(Mockito.any(), Mockito.any()))
            .thenReturn("{\n" +
                "  \"packages\": [\n" +
                "    {\n" +
                "      \"sandboxResourceType\": \"MARKET_FRONT_MBO_GURULITE_UI_VIEW\",\n" +
                "      \"moduleName\": \"gurulight-ui\",\n" +
                "      \"version\": \"2.241-2018.4.11+1\",\n" +
                "      \"changelog\": \"* MBO-14108 TEST\",\n" +
                "      \"changelogDetails\": [\n" +
                "        {\n" +
                "          \"revision\": \"ecb5d27974e427f1846fe435c3166574f439af2d\",\n" +
                "          \"author\": \"padme <padme@yandex-team.ru>\",\n" +
                "          \"change\": \"* MBO-14108 TEST\",\n" +
                "          \"timestampSeconds\": 1524060985\n" +
                "        }\n" +
                "      ],\n" +
                "      \"sandboxResourceId\": 543920950,\n" +
                "      \"sandboxTicket\": {\n" +
                "        \"url\": \"https://sandbox.yandex-team.ru/task/240091921/view\",\n" +
                "        \"id\": \"240091921\"\n" +
                "      }\n" +
                "    },\n" +
                "    {\n" +
                "      \"debPackageName\": \"yandex-mbo-lite\",\n" +
                "      \"changelogDetails\": [\n" +
                "        {\n" +
                "          \"revision\": \"ecb5d27974e427f1846fe435c3166574f439af2d\",\n" +
                "          \"author\": \"padme <padme@yandex-team.ru>\",\n" +
                "          \"change\": \"Merge remote-tracking branch 'origin/MBO-14108' into release/2018.4.11\",\n" +
                "          \"timestampSeconds\": 1524060985\n" +
                "        }\n" +
                "      ],\n" +
                "      \"moduleName\": \"mbo-lite\",\n" +
                "      \"version\": \"3.257-2018.4.11+1\",\n" +
                "      \"changelog\": \"* MBO-14108 Checkstyle\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"rpmPackages\": [\n" +
                "        {\n" +
                "          \"version\": \"1.2.2018.4.11.1-0\",\n" +
                "          \"name\": \"yandex-mbo-db\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"changelogDetails\": [\n" +
                "        {\n" +
                "          \"revision\": \"ecb5d27974e427f1846fe435c3166574f439af2d\",\n" +
                "          \"author\": \"padme <padme@yandex-team.ru>\",\n" +
                "          \"change\": \"* MBO-14108 Checkstyle\",\n" +
                "          \"timestampSeconds\": 1524060985\n" +
                "        }\n" +
                "      ],\n" +
                "      \"moduleName\": \"mbo-db\",\n" +
                "      \"changelog\": \"* MBO-14108 Remove GURU recipes from navigation tree\"\n" +
                "    }\n" +
                "  ]\n" +
                "}\n");

        JobExecutor sut = jobTester.jobInstanceBuilder(MarketTeamcityBuildJob.class)
            .withResource(MarketTeamcityBuildConfig.builder().withJobName(JOB_NAME).build())
            .create();

        TestJobContext jobContext = getJobContext();
        sut.execute(jobContext);

        List<ConductorPackage> conductorPackages = jobContext.getProducedResourcesList().stream()
            .filter(resource -> resource instanceof ConductorPackage)
            .map(resource -> (ConductorPackage) resource)
            .collect(Collectors.toList());
        List<SandboxResource> sandboxPackages = jobContext.getProducedResourcesList().stream()
            .filter(resource -> resource instanceof SandboxResource)
            .map(resource -> (SandboxResource) resource)
            .collect(Collectors.toList());

        ConductorPackage first = conductorPackages.stream().filter(x -> x.getPackageName().equals("yandex-mbo-lite"))
            .findFirst().orElseThrow(RuntimeException::new);

        ConductorPackage second = conductorPackages.stream().filter(x -> x.getPackageName().equals("yandex-mbo-db"))
            .findFirst().orElseThrow(RuntimeException::new);

        Assertions.assertThat(conductorPackages).hasSize(2);
        Assertions.assertThat(first.getVersion()).isEqualTo("3.257-2018.4.11+1");
        Assertions.assertThat(first.getPackageName()).isEqualTo("yandex-mbo-lite");
        Assertions.assertThat(second.getVersion()).isEqualTo("1.2.2018.4.11.1-0");
        Assertions.assertThat(second.getPackageName()).isEqualTo("yandex-mbo-db");
        Assertions.assertThat(sandboxPackages).hasSize(1);
        Assertions.assertThat(sandboxPackages.get(0).getResourceType()).isEqualTo("MARKET_FRONT_MBO_GURULITE_UI_VIEW");
    }

    @Test // MARKETINFRA-1896
    public void testMbiPackagesParsing() throws Exception {
        Mockito.when(teamcityBuilder.getArtifact(Mockito.any(), Mockito.any()))
            .thenReturn("{\n" +
                "  \"packages\": [\n" +
                "    {\n" +
                "      \"debPackageName\": \"yandex-mbi-db\",\n" +
                "      \"rpmPackages\": [\n" +
                "        {\n" +
                "          \"version\": \"1.608.2017.3.130MBI.23163.1-0\",\n" +
                "          \"name\": \"yandex-mbi-db\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"version\": \"1.608.2017.3.130MBI.23163.1-0\",\n" +
                "          \"name\": \"yandex-mbi-db-production\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"version\": \"1.608.2017.3.130MBI.23163.1-0\",\n" +
                "          \"name\": \"yandex-mbi-db-testing\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"moduleName\": \"mbi-db\",\n" +
                "      \"version\": \"1.608-2017.3.130MBI-23163+1\",\n" +
                "      \"changelog\": \"* MBI-22438 Тестовый ПР для отладки пайплайна\"\n" +
                "    }\n" +
                "  ]\n" +
                "}");

        JobExecutor sut = jobTester.jobInstanceBuilder(MarketTeamcityBuildJob.class)
            .withResource(MarketTeamcityBuildConfig.builder().withJobName(JOB_NAME).build())
            .create();

        TestJobContext jobContext = getJobContext();
        sut.execute(jobContext);

        List<ConductorPackage> conductorPackages = jobContext.getProducedResourcesList().stream()
            .filter(resource -> resource instanceof ConductorPackage)
            .map(resource -> (ConductorPackage) resource)
            .collect(Collectors.toList());
        List<SandboxResource> sandboxPackages = jobContext.getProducedResourcesList().stream()
            .filter(resource -> resource instanceof SandboxResource)
            .map(resource -> (SandboxResource) resource)
            .collect(Collectors.toList());

        ConductorPackage first = conductorPackages.stream()
            .filter(x -> x.getPackageName().equals("yandex-mbi-db"))
            .findFirst().orElseThrow(RuntimeException::new);

        ConductorPackage second = conductorPackages.stream()
            .filter(x -> x.getPackageName().equals("yandex-mbi-db-production"))
            .findFirst().orElseThrow(RuntimeException::new);

        ConductorPackage third = conductorPackages.stream()
            .filter(x -> x.getPackageName().equals("yandex-mbi-db-testing"))
            .findFirst().orElseThrow(RuntimeException::new);

        Assertions.assertThat(sandboxPackages).isEmpty();
        Assertions.assertThat(conductorPackages).hasSize(3);
        Assertions.assertThat(first.getVersion()).isEqualTo("1.608.2017.3.130MBI.23163.1-0");
        Assertions.assertThat(first.getPackageName()).isEqualTo("yandex-mbi-db");
        Assertions.assertThat(second.getVersion()).isEqualTo("1.608.2017.3.130MBI.23163.1-0");
        Assertions.assertThat(second.getPackageName()).isEqualTo("yandex-mbi-db-production");
        Assertions.assertThat(third.getVersion()).isEqualTo("1.608.2017.3.130MBI.23163.1-0");
        Assertions.assertThat(third.getPackageName()).isEqualTo("yandex-mbi-db-testing");
    }

    @Test // MARKETINFRA-3090
    public void testBothDebPackageAndSandboxResourceParsing() throws Exception {
        Mockito.when(teamcityBuilder.getArtifact(Mockito.any(), Mockito.any()))
            .thenReturn("{\n" +
                "  \"packages\": [\n" +
                "    {\n" +
                "      \"sandboxResourceType\": \"MARKET_MBI_PARTNER_APP\",\n" +
                "      \"moduleName\": \"mbi-partner\",\n" +
                "      \"version\": \"1.302-20180419-164243.master\",\n" +
                "      \"changelogDetails\": [],\n" +
                "      \"changelog\": \"* MBI-27648. Change SMTP server to outbound-relay.yandex.net\\n* MBI-27648. " +
                "Change SMTP server to yabacks.yandex.ru (#3792)\\n* MBI-25273. Remove old code to work with " +
                "organization info (#2962)\\n* MBI-27557 add a 'newbie' field to programs REST API and fix " +
                "some\\nstatuses. (#3779)\\n* MBI-27575 Учитывать в shops_web.v_shops_alive магазины, " +
                "которые\\nразмещаются только на красном маркете (#3773)\\n* MBI-23257: Add missing signatory fields " +
                "(#3762)\",\n" +
                "      \"sandboxTicket\": {\n" +
                "        \"url\": \"https://sandbox.yandex-team.ru/task/240457199/view\",\n" +
                "        \"id\": \"240457199\"\n" +
                "      },\n" +
                "      \"sandboxResourceId\": 544780959,\n" +
                "      \"debPackageName\": \"yandex-market-payment\"\n" +
                "    }\n" +
                "  ]\n" +
                "}");

        JobExecutor sut = jobTester.jobInstanceBuilder(MarketTeamcityBuildJob.class)
            .withResource(MarketTeamcityBuildConfig.builder().withJobName(JOB_NAME).build())
            .create();

        TestJobContext jobContext = getJobContext();
        sut.execute(jobContext);

        List<ConductorPackage> conductorPackages = jobContext.getProducedResourcesList().stream()
            .filter(resource -> resource instanceof ConductorPackage)
            .map(resource -> (ConductorPackage) resource)
            .collect(Collectors.toList());
        List<SandboxResource> sandboxPackages = jobContext.getProducedResourcesList().stream()
            .filter(resource -> resource instanceof SandboxResource)
            .map(resource -> (SandboxResource) resource)
            .collect(Collectors.toList());

        Assertions.assertThat(sandboxPackages).hasSize(1);
        Assertions.assertThat(sandboxPackages.get(0).getId()).isEqualTo(544780959);
        Assertions.assertThat(sandboxPackages.get(0).getResourceType()).isEqualTo("MARKET_MBI_PARTNER_APP");
        Assertions.assertThat(sandboxPackages.get(0).getTaskId()).isEqualTo(240457199);
        Assertions.assertThat(sandboxPackages.get(0).getTaskType()).isEqualTo(MarketTeamcityBuildJob.SANDBOX_TASK_TYPE);

        Assertions.assertThat(conductorPackages).hasSize(1);
        Assertions.assertThat(conductorPackages.get(0).getVersion()).isEqualTo("1.302-20180419-164243.master");
        Assertions.assertThat(conductorPackages.get(0).getPackageName()).isEqualTo("yandex-market-payment");
    }

    private TeamcityBuildConfig captureTeamcityBuildConfig(JobContext jobContext) throws InterruptedException,
        TimeoutException {
        ArgumentCaptor<TeamcityBuildConfig> captor = ArgumentCaptor.forClass(TeamcityBuildConfig.class);
        Mockito.verify(teamcityBuilder)
            .enqueueBuild(captor.capture(), eq(jobContext), Mockito.anyBoolean());
        return captor.getValue();
    }

    private TestJobContext getJobContext() {
        StatusChange statusChange = Mockito.mock(StatusChange.class);
        Mockito.when(statusChange.getDate()).thenReturn(new Date(1516039008000L));

        JobLaunch jobLaunch = Mockito.mock(JobLaunch.class);
        Mockito.when(jobLaunch.getStatusHistory()).thenReturn(Collections.singletonList(statusChange));

        JobState jobState = Mockito.mock(JobState.class);
        Mockito.when(jobState.getLaunches()).thenReturn(Collections.singletonList(jobLaunch));
        Mockito.when(jobState.getLastLaunch()).thenReturn(jobLaunch);

        TestJobContext jobContext = new TestJobContext();
        jobContext.setJobStateMock(jobState);
        return jobContext;
    }

    @Configuration
    @Import(JobTesterConfig.class)
    public static class Config {
        @Autowired
        private ApplicationContext ctx;

        @Bean
        public ArcArcadiaClient arcArcadiaClient() {
            return Mockito.mock(ArcArcadiaClient.class);
        }

        @Bean
        public TeamcityBuilder teamcityBuilder() {
            return Mockito.mock(TeamcityBuilder.class);
        }

        @Bean
        public NotificationCenter notificationCenter() {
            return Mockito.mock(NotificationCenter.class);
        }

        @Bean
        public Notificator notificator() {
            return Mockito.mock(Notificator.class);
        }
    }

    public static class UtilMethodTests {
        /**
         * Проверяем корректность получения актуального списка модулей в случае, если не задан опциональный список
         */
        @Test
        public void getActualModulesWithoutConfiguredModulesListTest() {
            ModulesList modulesList = new ModulesList(Arrays.asList("mbi-db", "mbi-bidding-vendor", "mbi-billing"));
            ModulesList actualModulesList = MarketTeamcityBuildJob.getActualModules(modulesList, null);

            Assert.assertEquals(modulesList, actualModulesList);

            modulesList = ModulesList.ALL;
            actualModulesList = MarketTeamcityBuildJob.getActualModules(modulesList, null);

            Assert.assertEquals(modulesList, actualModulesList);
        }

        /**
         * Проверяем корректность получения актуального списка модулей в случае, если задан опциональный список
         */
        @Test
        public void getActualModulesWithConfiguredModulesListTest() {
            ModulesList modulesList = new ModulesList(Arrays.asList("test-module-2", "mbi-db", "mbi-bidding-vendor",
                "mbi-billing"));
            ModulesList configModulesList = new ModulesList(Arrays.asList("mbi-db", "mbi-bidding-vendor"));
            ModulesList actualModulesList = MarketTeamcityBuildJob.getActualModules(modulesList, configModulesList);

            Assert.assertEquals(configModulesList, actualModulesList);

            modulesList = ModulesList.ALL;
            configModulesList = new ModulesList(Arrays.asList("mbi-billing", "mbi-bidding-vendor"));
            actualModulesList = MarketTeamcityBuildJob.getActualModules(modulesList, configModulesList);

            Assert.assertEquals(configModulesList, actualModulesList);
        }
    }
}
