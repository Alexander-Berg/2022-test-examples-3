package ru.yandex.market.tsum.context.impl;

import java.util.Collections;
import java.util.HashMap;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.tsum.context.TsumJobContext;
import ru.yandex.market.tsum.core.notify.common.NotificationCenter;
import ru.yandex.market.tsum.dao.PipelinesDao;
import ru.yandex.market.tsum.dao.ProjectsDao;
import ru.yandex.market.tsum.experiment.ExperimentService;
import ru.yandex.market.tsum.multitesting.MultitestingDao;
import ru.yandex.market.tsum.notifications.PipeLaunchNotificationSettingsDao;
import ru.yandex.market.tsum.per_commit.dao.PerCommitBranchStateDao;
import ru.yandex.market.tsum.per_commit.dao.PerCommitBranchStateHistoryDao;
import ru.yandex.market.tsum.pipe.engine.definition.job.JobExecutor;
import ru.yandex.market.tsum.pipe.engine.definition.stage.Stage;
import ru.yandex.market.tsum.pipe.engine.runtime.JobContextFactory;
import ru.yandex.market.tsum.pipe.engine.runtime.LaunchUrlProvider;
import ru.yandex.market.tsum.pipe.engine.runtime.di.LaunchEntitiesFactory;
import ru.yandex.market.tsum.pipe.engine.runtime.di.ResourceDao;
import ru.yandex.market.tsum.pipe.engine.runtime.di.ResourceService;
import ru.yandex.market.tsum.pipe.engine.runtime.notifications.Notificator;
import ru.yandex.market.tsum.pipe.engine.runtime.notifications.settings.ProjectNotificationSettingsDao;
import ru.yandex.market.tsum.pipe.engine.runtime.state.JobProgressService;
import ru.yandex.market.tsum.pipe.engine.runtime.state.PipeLaunchDao;
import ru.yandex.market.tsum.pipe.engine.runtime.state.PipeStateService;
import ru.yandex.market.tsum.pipe.engine.runtime.state.StageGroupDao;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.JobState;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.PipeLaunch;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.PipeLaunchRefImpl;
import ru.yandex.market.tsum.pipe.engine.source_code.SourceCodeService;
import ru.yandex.market.tsum.pipelines.common.jobs.delivery.ChangelogService;
import ru.yandex.market.tsum.pipelines.sre.helpers.ApproverHelper;
import ru.yandex.market.tsum.release.dao.GitHubSettings;
import ru.yandex.market.tsum.release.dao.ReleaseDao;
import ru.yandex.market.tsum.release.dao.ReleaseService;
import ru.yandex.market.tsum.release.dao.delivery.DeliveryMachineStateDao;
import ru.yandex.market.tsum.release.dao.delivery.launch_rules.LaunchRuleChecker;
import ru.yandex.market.tsum.release.dao.title_providers.ReleaseTitleProviderFactory;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Nikolay Firov <a href="mailto:firov@yandex-team.ru"></a>
 * @date 26/11/2018
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {JobContextFactoryImplTest.Config.class})
public class JobContextFactoryImplTest {
    @Autowired
    private JobContextFactory jobContextFactory;

    @Autowired
    private ReleaseService releaseService;

    @Test
    public void createsCdReleaseContext() {
        JobState jobState = mock(JobState.class);
        when(jobState.getLaunches()).thenReturn(Collections.singletonList(null));

        PipeLaunch launch = PipeLaunch.builder()
            .withLaunchRef(PipeLaunchRefImpl.create("pipeId"))
            .withTriggeredBy("me")
            .withJobs(new HashMap<>())
            .withStagesGroupId("test")
            .withStages(Collections.singletonList(new Stage(null, "tet")))
            .withProjectId("prj")
            .build();

        TsumJobContext context = (TsumJobContext) jobContextFactory.createJobContext(
            launch, jobState, JobExecutor.class
        );

        context.cd().getVcsSettings(GitHubSettings.class);

        verify(releaseService).getVcsSettings(any(), any());
    }

    @PropertySource("classpath:test.properties")
    public static class Config {
        @Bean
        public LaunchUrlProvider launchUrlProvider() {
            return mock(LaunchUrlProvider.class);
        }

        @Bean
        public MongoTemplate mongoTemplate() {
            MongoTemplate template = mock(MongoTemplate.class);
            when(template.getConverter()).thenReturn(mock(MongoConverter.class));
            return template;
        }

        @Bean
        public JobProgressService jobProgressService() {
            return mock(JobProgressService.class);
        }

        @Bean
        public ResourceDao resourceDao() {
            return mock(ResourceDao.class);
        }

        @Bean
        public ReleaseDao releaseDao() {
            return mock(ReleaseDao.class);
        }

        @Bean
        public ReleaseService releaseService() {
            return mock(ReleaseService.class);
        }

        @Bean
        public StageGroupDao stageService() {
            return mock(StageGroupDao.class);
        }

        @Bean
        public PipeLaunchNotificationSettingsDao pipeLaunchNotificationSettingsDao() {
            return mock(PipeLaunchNotificationSettingsDao.class);
        }

        @Bean
        public ProjectNotificationSettingsDao projectNotificationSettingsDao() {
            return mock(ProjectNotificationSettingsDao.class);
        }

        @Bean
        public PipeLaunchDao pipeLaunchDao() {
            return mock(PipeLaunchDao.class);
        }

        @Bean
        public ProjectsDao projectsDao() {
            return mock(ProjectsDao.class);
        }

        @Bean
        public PipelinesDao pipelinesDao() {
            return mock(PipelinesDao.class);
        }

        @Bean
        public ReleaseTitleProviderFactory releaseTitleProviderFactory() {
            return mock(ReleaseTitleProviderFactory.class);
        }

        @Bean
        public PipeStateService pipeStateService() {
            return mock(PipeStateService.class);
        }

        @Bean
        public MultitestingDao multitestingDao() {
            return mock(MultitestingDao.class);
        }

        @Bean
        public DeliveryMachineStateDao deliveryMachineStateDao() {
            return mock(DeliveryMachineStateDao.class);
        }

        @Bean
        public JobContextFactory jobContextFactory() {
            return new JobContextFactoryImpl();
        }

        @Bean
        public LaunchEntitiesFactory launchEntitiesFactory() {
            return mock(LaunchEntitiesFactory.class);
        }

        @Bean
        public ResourceService resourceService() {
            return mock(ResourceService.class);
        }

        @Bean
        public ChangelogService changelogService() {
            return mock(ChangelogService.class);
        }

        @Bean
        public ApproverHelper approverHelper() {
            return mock(ApproverHelper.class);
        }

        @Bean
        public ExperimentService experimentService() {
            return mock(ExperimentService.class);
        }

        @Bean
        public Notificator notificator() {
            return mock(Notificator.class);
        }

        @Bean
        public NotificationCenter notificationCenter() {
            return mock(NotificationCenter.class);
        }

        @Bean
        public SourceCodeService sourceCodeEntityService() {
            return mock(SourceCodeService.class);
        }

        @Bean
        public LaunchRuleChecker launchRuleChecker() {
            return mock(LaunchRuleChecker.class);
        }

        @Bean
        public PerCommitBranchStateDao perCommitBranchStateDao() {
            return mock(PerCommitBranchStateDao.class);
        }

        @Bean
        public PerCommitBranchStateHistoryDao perCommitBranchStateHistoryDao() {
            return mock(PerCommitBranchStateHistoryDao.class);
        }
    }
}
