package ru.yandex.market.tsum.pipelines.multitesting;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.tsum.arcadia.ArcadiaCache;
import ru.yandex.market.tsum.clients.startrek.StartrekClient;
import ru.yandex.market.tsum.dao.PipelinesDao;
import ru.yandex.market.tsum.dao.ProjectsDao;
import ru.yandex.market.tsum.entity.pipeline.PipelineEntity;
import ru.yandex.market.tsum.entity.pipeline.StoredConfigurationEntity;
import ru.yandex.market.tsum.multitesting.MultitestingCleanupService;
import ru.yandex.market.tsum.notifications.PipeLaunchNotificationSettingsDao;
import ru.yandex.market.tsum.per_commit.PerCommitService;
import ru.yandex.market.tsum.pipe.engine.definition.DummyJob;
import ru.yandex.market.tsum.pipe.engine.definition.Pipeline;
import ru.yandex.market.tsum.pipe.engine.definition.builder.JobBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.builder.PipelineBuilder;
import ru.yandex.market.tsum.pipe.engine.runtime.notifications.settings.ProjectNotificationSettingsDao;
import ru.yandex.market.tsum.pipelines.common.jobs.multitesting.MultitestingTags;
import ru.yandex.market.tsum.release.dao.title_providers.ReleaseTitleProvider;
import ru.yandex.market.tsum.release.dao.title_providers.ReleaseTitleProviderFactory;
import ru.yandex.startrek.client.Issues;

import static org.mockito.Mockito.mock;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 27.12.2017
 */
@Configuration
public class MultitestingTestConfig {
    public static final String PIPE_WITH_CUSTOM_CLEANUP_NAME = "test-custom-cleanup";
    public static final String PIPE_WITH_CUSTOM_CLEANUP_ID = "mt-" + PIPE_WITH_CUSTOM_CLEANUP_NAME;

    @Bean(name = PIPE_WITH_CUSTOM_CLEANUP_ID)
    public Pipeline mtTestCustomCleanupPipeline() {
        PipelineBuilder builder = PipelineBuilder.create();
        JobBuilder buildJob = builder.withJob(DummyJob.class)
            .withId("build")
            .withTags(MultitestingTags.BUILD);
        JobBuilder deployJob = builder.withJob(DummyJob.class).withId("deploy");
        JobBuilder cleanupJob = builder.withJob(DummyJob.class)
            .withId("cleanup")
            .withManualTrigger()
            .withTags(MultitestingTags.CLEANUP);
        return builder.build();
    }

    @Bean(name = "regular-pipeline")
    public Pipeline regularPipeline() {
        PipelineBuilder builder = PipelineBuilder.create();
        builder.withJob(DummyJob.class);
        return builder.build();
    }

    @Bean
    public ProjectNotificationSettingsDao projectNotificationSettingsDao() {
        return Mockito.mock(ProjectNotificationSettingsDao.class);
    }

    @Bean
    public PipeLaunchNotificationSettingsDao pipeLaunchNotificationSettingsDao() {
        return Mockito.mock(PipeLaunchNotificationSettingsDao.class);
    }

    @Bean
    public ProjectsDao projectsDao() {
        return mock(ProjectsDao.class);
    }

    @Bean
    public PipelinesDao pipelinesDao() {
        PipelinesDao mock = mock(PipelinesDao.class);

        Mockito.when(mock.exists(Mockito.anyString())).thenReturn(true);

        PipelineEntity entity = new PipelineEntity();
        entity.setCurrentConfiguration(new StoredConfigurationEntity());
        Mockito.when(mock.get(Mockito.anyString())).thenReturn(entity);

        return mock;
    }

    @Bean
    public ReleaseTitleProviderFactory releaseTitleProviderProvider() {
        ReleaseTitleProvider mock = mock(ReleaseTitleProvider.class);
        Mockito.when(mock.getTitle(Mockito.anyString(), Mockito.anyString(), Mockito.any())).thenReturn("");

        return clazz -> mock;
    }

    @Bean
    public PerCommitService perCommitService() {
        return mock(PerCommitService.class);
    }

    @Bean
    public Issues issues() {
        return Mockito.mock(Issues.class);
    }

    @Bean
    public StartrekClient startrekClient() {
        return Mockito.mock(StartrekClient.class);
    }

    @Bean
    public ArcadiaCache arcadiaCache() {
        return Mockito.mock(ArcadiaCache.class);
    }

    @Bean
    public MultitestingCleanupService multitestingCleanupService() {
        return mock(MultitestingCleanupService.class);
    }
}
