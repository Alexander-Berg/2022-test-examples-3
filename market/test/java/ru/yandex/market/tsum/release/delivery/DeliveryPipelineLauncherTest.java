package ru.yandex.market.tsum.release.delivery;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.tmatesoft.svn.core.SVNLogEntry;

import ru.yandex.market.tsum.arcadia.ArcadiaCache;
import ru.yandex.market.tsum.bitbucket.BitbucketCache;
import ru.yandex.market.tsum.clients.bitbucket.BitbucketClient;
import ru.yandex.market.tsum.clients.bitbucket.models.BitbucketCommit;
import ru.yandex.market.tsum.clients.bitbucket.models.BitbucketUser;
import ru.yandex.market.tsum.clients.calendar.CalendarClient;
import ru.yandex.market.tsum.clients.github.GitHubClient;
import ru.yandex.market.tsum.clients.staff.StaffApiClient;
import ru.yandex.market.tsum.clients.staff.StaffPerson;
import ru.yandex.market.tsum.core.TestMongo;
import ru.yandex.market.tsum.core.environment.Environment;
import ru.yandex.market.tsum.core.environment.EnvironmentProvider;
import ru.yandex.market.tsum.dao.ProjectsDao;
import ru.yandex.market.tsum.entity.project.DeliveryMachineEntity;
import ru.yandex.market.tsum.entity.project.ProjectEntity;
import ru.yandex.market.tsum.pipe.engine.definition.DummyJob;
import ru.yandex.market.tsum.pipe.engine.definition.Pipeline;
import ru.yandex.market.tsum.pipe.engine.definition.builder.PipelineBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.stage.Stage;
import ru.yandex.market.tsum.pipe.engine.definition.stage.StageGroup;
import ru.yandex.market.tsum.pipe.engine.runtime.PipeProvider;
import ru.yandex.market.tsum.pipe.engine.runtime.di.ResourceService;
import ru.yandex.market.tsum.pipe.engine.runtime.di.model.StoredResource;
import ru.yandex.market.tsum.pipe.engine.runtime.state.PipeLaunchDao;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.PipeLaunch;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.PipeLaunchRefImpl;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.StageGroupState;
import ru.yandex.market.tsum.pipelines.common.jobs.github.GithubHotfixJob;
import ru.yandex.market.tsum.pipelines.common.jobs.sandbox.ArcadiaHotfixJob;
import ru.yandex.market.tsum.pipelines.common.jobs.sandbox.bitbucket.BitbucketHotfixJob;
import ru.yandex.market.tsum.pipelines.common.resources.DeliveryPipelineParams;
import ru.yandex.market.tsum.release.dao.BitbucketSettings;
import ru.yandex.market.tsum.release.dao.CreateReleaseCommandBuilder;
import ru.yandex.market.tsum.release.dao.DeliveryMachineSettings;
import ru.yandex.market.tsum.release.dao.FinishCause;
import ru.yandex.market.tsum.release.dao.GitHubSettings;
import ru.yandex.market.tsum.release.dao.Release;
import ru.yandex.market.tsum.release.dao.ReleaseDao;
import ru.yandex.market.tsum.release.dao.ReleaseService;
import ru.yandex.market.tsum.release.dao.TestProjectDefinition;
import ru.yandex.market.tsum.release.dao.delivery.DeliveryMachineState;
import ru.yandex.market.tsum.release.dao.delivery.DeliveryMachineStateDao;
import ru.yandex.market.tsum.release.dao.delivery.HotfixParams;
import ru.yandex.market.tsum.release.dao.delivery.PipelineType;
import ru.yandex.market.tsum.release.dao.delivery.RollbackRequest;
import ru.yandex.market.tsum.release.dao.delivery.TriggerRequest;
import ru.yandex.market.tsum.release.dao.delivery.launch_rules.DepartmentRule;
import ru.yandex.market.tsum.release.dao.delivery.launch_rules.DirectoryRule;
import ru.yandex.market.tsum.release.dao.delivery.launch_rules.HasTicketRule;
import ru.yandex.market.tsum.release.dao.delivery.launch_rules.LaunchRuleChecker;
import ru.yandex.market.tsum.release.dao.delivery.launch_rules.PathPatternType;
import ru.yandex.market.tsum.release.dao.delivery.launch_rules.RuleGroup;
import ru.yandex.market.tsum.release.dao.title_providers.OrdinalTitleProvider;
import ru.yandex.market.tsum.release.delivery.rollback.RollbackPipelineBuilder;
import ru.yandex.market.tsum.service.DeliveryMachineDynamicConfigurationService;
import ru.yandex.market.tsum.test_data.TestStageGroupStateFactory;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tsum.test_data.TestRepositoryCommitFactory.commit;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 19.02.18
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = {TestMongo.class})
public class DeliveryPipelineLauncherTest {
    @Autowired
    private MongoTemplate mongoTemplate;

    private static final String STAGE_GROUP_ID = "stage-group-id";
    private static final String PROJECT_ID = "market-infra";
    private static final String PIPE_ID = "pipe-id";
    private static final String HOTFIX_PIPE_ID = "hotfix-pipe-id";

    private static final MongoConverter MONGO_CONVERTER = mock(MongoConverter.class);

    private static final DeliveryMachineEntity GITHUB_DELIVERY_MACHINE_SETTINGS =
        new DeliveryMachineEntity(
            DeliveryMachineSettings.builder()
                .withGithubSettings("market-infra/test-pipeline")
                .withStageGroupId(STAGE_GROUP_ID)
                .withPipeline(PIPE_ID, "test-pipe", mock(OrdinalTitleProvider.class))
                .build(), MONGO_CONVERTER);

    private static final DeliveryMachineEntity BITBUCKET_DELIVERY_MACHINE_SETTINGS =
        new DeliveryMachineEntity(
            DeliveryMachineSettings.builder()
                .withBitbucketSettings("market-infra", "test-pipeline", "master")
                .withStageGroupId(STAGE_GROUP_ID)
                .withPipeline(PIPE_ID, "test-pipe", mock(OrdinalTitleProvider.class))
                .build(), MONGO_CONVERTER);

    private static final String TRIGGERED_BY = "me";
    private static final String REVISION_1 = "e1d36cb89";
    private static final String REVISION_2 = "b8adf2208";
    private static final String COMMIT_AUTHOR = "user42";
    private static final String COMMIT_QUEUE = "MARKETINFRATEST";
    private static final String ANOTHER_COMMIT_QUEUE = "ANOTHERMARKETINFRATEST";
    private static final String DEPARTMENT_ID = "the_best_department";
    private static final String ARCADIA_HOTFIX_REVISION = "3";
    private static final long ARCADIA_REVISION_1 = 1;
    private static final long ARCADIA_REVISION_2 = 2;

    private static final long FIRST_COMMIT_TIMESTAMP = new Date().getTime();
    private static final Date REVISION_1_DATE = new Date(FIRST_COMMIT_TIMESTAMP);
    private static final Date REVISION_2_DATE = new Date(FIRST_COMMIT_TIMESTAMP + 1);

    private static final ArcadiaVcsChange ARCADIA_COMMIT_1 = new ArcadiaVcsChange(
        ARCADIA_REVISION_1,
        REVISION_1_DATE.toInstant(),
        Collections.emptyList(),
        COMMIT_QUEUE + "-1 change 1",
        COMMIT_AUTHOR
    );

    private static final String STABLE_REVISION = "a150d6501";
    private static final String PIPE_LAUNCH_ID = "5a5ce2187c0e90d5bbc56f4a";

    private static final String RELEVANT_DIR = "some/file/in/relevant/dir";
    private static final String FILE_REGEXP_PATTERN = "some\\/file\\/in\\/relevant\\/dir(\\/.*)*";

    private static final String ARCADIA_COMMIT_2_CHANGED_PATHS = RELEVANT_DIR + "/someDir";

    private static final ArcadiaVcsChange ARCADIA_COMMIT_2 = new ArcadiaVcsChange(
        ARCADIA_REVISION_2,
        REVISION_2_DATE.toInstant(),
        Collections.singletonList(ARCADIA_COMMIT_2_CHANGED_PATHS),
        COMMIT_QUEUE + "-2 change 2",
        COMMIT_AUTHOR
    );

    private static final List<RuleGroup> PROJECT_RULE_GROUPS = Collections.singletonList(
        new RuleGroup(
            Collections.singletonList(
                new HasTicketRule(ANOTHER_COMMIT_QUEUE)
            ),
            MONGO_CONVERTER
        )
    );

    private ReleaseDao releaseDao;
    private ProjectsDao projectsDao;
    private PipeLaunchDao pipeLaunchDao;
    private DeliveryMachineStateDao deliveryMachineStateDao;
    private ReleaseService releaseService;
    private PipeProvider pipeProvider;
    private DeliveryPipelineLauncher sut;
    private ResourceService resourceService;

    private EnvironmentProvider environmentProvider;
    private GitHubClient gitHubClient;
    private BitbucketClient bitbucketClient;
    private BitbucketCache bitbucketCache;
    private ArcadiaCache arcadiaCache;
    private StaffApiClient staffApiClient;
    private CalendarClient calendarClient;
    private DeliveryMachineDynamicConfigurationService deliveryMachineDynConfigService;

    private static DeliveryMachineEntity getArcadiaDeliveryMachineEntity(List<RuleGroup> groupRules) {
        return new DeliveryMachineEntity(
            DeliveryMachineSettings.builder()
                .withArcadiaSettings()
                .withStageGroupId(STAGE_GROUP_ID)
                .withPipeline(PIPE_ID, "test-pipe", mock(OrdinalTitleProvider.class))
                .build(), MONGO_CONVERTER, groupRules);
    }

    @Before
    public void setUp() {
        releaseDao = mock(ReleaseDao.class);
        projectsDao = mock(ProjectsDao.class);
        pipeLaunchDao = mock(PipeLaunchDao.class);
        resourceService = mock(ResourceService.class);
        when(projectsDao.stream()).thenReturn(Stream.of(project(PROJECT_ID)));
        when(projectsDao.get(PROJECT_ID)).thenReturn(project(PROJECT_ID));

        when(projectsDao.stream()).thenReturn(
            Stream.of(project(PROJECT_ID))
        );

        deliveryMachineStateDao = Mockito.spy(
            new DeliveryMachineStateDao(
                mongoTemplate,
                mock(DeliveryMachineStateVersionService.class),
                projectsDao,
                5
            )
        );

        releaseService = mock(ReleaseService.class);
        environmentProvider = mock(EnvironmentProvider.class);
        when(environmentProvider.getCurrent()).thenReturn(Environment.PRODUCTION);

        StageGroup stageGroup = new StageGroup("testing");

        PipelineBuilder builder = PipelineBuilder.create();
        builder.withJob(DummyJob.class)
            .withPosition(0, 0)
            .beginStage(stageGroup.getStage("testing"));

        Pipeline pipeline = builder.build();
        pipeProvider = mock(PipeProvider.class);
        when(pipeProvider.get(any())).thenReturn(pipeline);

        gitHubClient = mock(GitHubClient.class);
        bitbucketClient = mock(BitbucketClient.class);
        bitbucketCache = mock(BitbucketCache.class);
        arcadiaCache = mock(ArcadiaCache.class);
        staffApiClient = mock(StaffApiClient.class);
        calendarClient = mock(CalendarClient.class);
        deliveryMachineDynConfigService = mock(DeliveryMachineDynamicConfigurationService.class);

        GitHubSettings gitHubSettings = (GitHubSettings) GITHUB_DELIVERY_MACHINE_SETTINGS.getVcsSettings().get();
        List<RepositoryCommit> gitHubCommits = Arrays.asList(
            commit(REVISION_2, COMMIT_AUTHOR, ANOTHER_COMMIT_QUEUE + "-1 change 1"),
            commit(REVISION_1, COMMIT_AUTHOR, COMMIT_QUEUE + "-2 change 2"),
            commit(STABLE_REVISION, COMMIT_AUTHOR, COMMIT_QUEUE + "-3 change 3")
        );
        when(gitHubClient.getCommitIterator(
            eq(gitHubSettings.getRepositoryFullName()),
            eq(gitHubSettings.getMainBranch())
        ))
            .thenReturn(gitHubCommits.iterator());

        when(gitHubClient.getCommit(any(), eq(STABLE_REVISION))).thenReturn(gitHubCommits.get(2));
        when(gitHubClient.getCommit(any(), eq(REVISION_1))).thenReturn(gitHubCommits.get(1));
        when(gitHubClient.getCommit(any(), eq(REVISION_2))).thenReturn(gitHubCommits.get(0));

        BitbucketSettings bitbucketSettings =
            (BitbucketSettings) BITBUCKET_DELIVERY_MACHINE_SETTINGS.getVcsSettings().get();
        List<BitbucketCommit> bitbucketCommits = Arrays.asList(
            bitbucketCommit(REVISION_2, COMMIT_AUTHOR, ANOTHER_COMMIT_QUEUE + "-1 change 1"),
            bitbucketCommit(REVISION_1, COMMIT_AUTHOR, COMMIT_QUEUE + "-2 change 2"),
            bitbucketCommit(STABLE_REVISION, COMMIT_AUTHOR, COMMIT_QUEUE + "-3 change 3")
        );

        when(bitbucketClient.getCommitIterator(
            eq(bitbucketSettings.getProject()),
            eq(bitbucketSettings.getRepositoryName()),
            eq(bitbucketSettings.getMainBranch())
        ))
            .thenReturn(bitbucketCommits.iterator());

        when(bitbucketClient.getCommit(any(), any(), eq(STABLE_REVISION))).thenReturn(bitbucketCommits.get(2));
        when(bitbucketClient.getCommit(any(), any(), eq(REVISION_1))).thenReturn(bitbucketCommits.get(1));
        when(bitbucketClient.getCommit(any(), any(), eq(REVISION_2))).thenReturn(bitbucketCommits.get(0));

        when(arcadiaCache.get(ARCADIA_REVISION_1)).thenReturn(ARCADIA_COMMIT_1);
        when(arcadiaCache.get(ARCADIA_REVISION_2)).thenReturn(ARCADIA_COMMIT_2);

        when(staffApiClient.getPerson(COMMIT_AUTHOR)).thenReturn(
            Optional.of(new StaffPerson(
                COMMIT_AUTHOR, -1, null, null, null,
                new StaffPerson.DepartmentGroup(DEPARTMENT_ID, "Группа разработки счастья пользователя")
            ))
        );

        when(releaseService.launchRelease(any()))
            .thenReturn(
                Release.builder()
                    .withProjectId(PROJECT_ID)
                    .withPipeId(PIPE_ID)
                    .withPipeLaunchId(PIPE_LAUNCH_ID)
                    .withCommit(REVISION_1, Instant.now())
                    .build()
            );

        when(pipeLaunchDao.getById(Mockito.anyString()))
            .thenReturn(
                PipeLaunch.builder()
                    .withLaunchRef(PipeLaunchRefImpl.create("pipeId"))
                    .withTriggeredBy("me")
                    .withJobs(new HashMap<>())
                    .withStagesGroupId("test")
                    .withStages(Collections.singletonList(new Stage(null, "tet")))
                    .withProjectId("prj")
                    .build()
            );

        when(deliveryMachineDynConfigService.canCreateRelease(PROJECT_ID)).thenReturn(true);
        when(deliveryMachineDynConfigService.canCreateRelease(TestProjectDefinition.TEST_PROJECT_ID)).thenReturn(true);

        sut = new DeliveryPipelineLauncher(
            pipeProvider,
            releaseDao, releaseService, deliveryMachineStateDao, environmentProvider, gitHubClient, bitbucketClient,
            bitbucketCache, arcadiaCache, pipeLaunchDao, TRIGGERED_BY, new RollbackPipelineBuilder(resourceService),
            new LaunchRuleChecker(staffApiClient, calendarClient, releaseDao, projectsDao),
            deliveryMachineDynConfigService
        );
    }

    @Test
    public void shouldNotLaunchReleaseWhenRevisionQueueIsEmpty() {
        StageGroupState stageGroupState = createStageGroupState();
        DeliveryMachineState deliveryMachineState = deliveryMachineState(null, null);

        maybeTriggerPipelines(stageGroupState, deliveryMachineState);

        Mockito.verifyZeroInteractions(releaseService);
    }

    @Test
    public void shouldNotLaunchReleaseWhenFirstStageIsLocked() {
        maybeTriggerPipelines(
            createStageGroupState(ImmutableMap.of(PIPE_LAUNCH_ID, "testing")),
            deliveryMachineState(null, REVISION_1)
        );

        Mockito.verifyZeroInteractions(releaseService);
    }

    @Test
    public void shouldNotLaunchReleaseWhenDisabled() {
        // arrange
        DeliveryMachineState deliveryMachineState = deliveryMachineState(null, REVISION_1);
        deliveryMachineState.getTriggerSettings().setTriggerNewPipelines(false);

        // act
        maybeTriggerPipelines(createStageGroupState(), deliveryMachineState);

        Mockito.verifyZeroInteractions(releaseService);
    }

    @Test
    public void shouldLaunchManualRelease_WhenCurrentEnvironmentIsNotProduction() {
        // arrange
        when(environmentProvider.getCurrent()).thenReturn(Environment.TESTING);
        DeliveryMachineState state = deliveryMachineState(REVISION_1, REVISION_2);

        state.setTriggerRequests(
            Collections.singletonList(new TriggerRequest(REVISION_1, true, "algebraic"))
        );

        Release runningRelease = release();
        when(releaseDao.getReleasesByPipeLaunchIds(any())).thenReturn(Collections.singletonList(runningRelease));


        // act
        maybeTriggerPipelines(
            project(PROJECT_ID, createGithubSettings()),
            createStageGroupState(),
            state
        );

        ArgumentCaptor<CreateReleaseCommandBuilder> captor = ArgumentCaptor.forClass(CreateReleaseCommandBuilder.class);
        Mockito.verify(releaseService, times(1)).launchRelease(captor.capture());
    }

    @Test
    public void shouldNotLaunchProductionRelease_WhenCurrentEnvironmentIsNotProduction() {
        // arrange
        when(environmentProvider.getCurrent()).thenReturn(Environment.TESTING);

        // act
        maybeTriggerPipelines(createStageGroupState(), deliveryMachineState(null, REVISION_1));

        Mockito.verifyZeroInteractions(releaseService);
    }

    @Test
    public void shouldLaunchTestRelease_WhenCurrentEnvironmentIsNotProduction() {
        // arrange
        when(
            projectsDao.stream()).thenReturn(Stream.of(project(TestProjectDefinition.TEST_PROJECT_ID))
        );

        when(environmentProvider.getCurrent()).thenReturn(Environment.TESTING);

        // act
        ProjectEntity project = project(TestProjectDefinition.TEST_PROJECT_ID);
        maybeTriggerPipelines(
            project, project.getDeliveryMachine(PIPE_ID), createStageGroupState(),
            deliveryMachineState(null, REVISION_2)
        );

        // assert
        ArgumentCaptor<CreateReleaseCommandBuilder> captor = ArgumentCaptor.forClass(CreateReleaseCommandBuilder.class);
        verify(releaseService, times(1)).launchRelease(captor.capture());

        Assert.assertEquals(TestProjectDefinition.TEST_PROJECT_ID, captor.getValue().getProjectId());
        Assert.assertEquals(PIPE_ID, captor.getValue().getPipeId());
        Assert.assertEquals(TRIGGERED_BY, captor.getValue().getTriggeredBy());
        Assert.assertEquals(PIPE_ID + ":" + REVISION_2, captor.getValue().getTag());
    }

    @Test
    public void shouldLaunchReleaseWhenRevisionQueueIsNotEmpty() {
        // act
        maybeTriggerPipelines(
            createStageGroupState(),
            deliveryMachineState(null, REVISION_2)
        );

        // assert
        ArgumentCaptor<CreateReleaseCommandBuilder> captor = ArgumentCaptor.forClass(CreateReleaseCommandBuilder.class);
        verify(releaseService, times(1)).launchRelease(captor.capture());

        Assert.assertEquals(PROJECT_ID, captor.getValue().getProjectId());
        Assert.assertEquals(PIPE_ID, captor.getValue().getPipeId());
        Assert.assertEquals(TRIGGERED_BY, captor.getValue().getTriggeredBy());
        Assert.assertEquals(PIPE_ID + ":" + REVISION_2, captor.getValue().getTag());

        DeliveryPipelineParams pipelineParams = captor.getValue()
            .getResourceContainer()
            .getSingleOfType(DeliveryPipelineParams.class);

        Assert.assertEquals(REVISION_2, pipelineParams.getRevision());
        Assert.assertNull(pipelineParams.getStableRevision());

        verify(deliveryMachineStateDao, times(1))
            .setLastProcessedRevision(STAGE_GROUP_ID, REVISION_2);
    }

    @Test
    public void shouldPassStableRevisionOnReleaseLaunch() {
        // arrange
        Release runningRelease = releaseBuilder().build();
        when(releaseDao.getReleasesByPipeLaunchIds(any())).thenReturn(Collections.singletonList(runningRelease));

        // act
        maybeTriggerPipelines(
            createStageGroupState(ImmutableMap.of(PIPE_LAUNCH_ID, "stable")),
            deliveryMachineState(STABLE_REVISION, REVISION_2)
        );

        // assert
        ArgumentCaptor<CreateReleaseCommandBuilder> captor = ArgumentCaptor.forClass(CreateReleaseCommandBuilder.class);
        verify(releaseService, times(1)).launchRelease(captor.capture());

        Assert.assertEquals(PROJECT_ID, captor.getValue().getProjectId());
        Assert.assertEquals(PIPE_ID, captor.getValue().getPipeId());
        Assert.assertEquals(TRIGGERED_BY, captor.getValue().getTriggeredBy());
        Assert.assertEquals(PIPE_ID + ":" + REVISION_2, captor.getValue().getTag());

        DeliveryPipelineParams pipelineParams = captor.getValue()
            .getResourceContainer()
            .getSingleOfType(DeliveryPipelineParams.class);

        Assert.assertEquals(REVISION_2, pipelineParams.getRevision());
        Assert.assertEquals(STABLE_REVISION, pipelineParams.getStableRevision());

        verify(deliveryMachineStateDao, times(1))
            .setLastProcessedRevision(STAGE_GROUP_ID, REVISION_2);
    }

    @Test
    public void shouldNotLaunchReleaseIfItWasLaunchedOnPreviousAttempt() {
        // arrange
        when(releaseDao.findReleaseByTag(PIPE_ID + ":" + REVISION_2)).thenReturn(release());

        // act
        maybeTriggerPipelines(
            createStageGroupState(),
            deliveryMachineState(null, REVISION_2)
        );

        // assert
        verify(releaseService, Mockito.never()).launchRelease(any());

        verify(deliveryMachineStateDao, times(1))
            .setLastProcessedRevision(STAGE_GROUP_ID, REVISION_2);
    }

    @Test
    public void shouldLaunchMainPipelineAsAHotfix_WhenHotfixPipelineIsNotSet_AndNoOtherHotfixesRunning() {
        DeliveryMachineState state = deliveryMachineState(
            Long.toString(ARCADIA_REVISION_1), Long.toString(ARCADIA_REVISION_2)
        );

        setHotfixParams(state);

        DeliveryMachineEntity deliveryMachineSettings = createArcadiaSettings();
        ProjectEntity project = project(PROJECT_ID, deliveryMachineSettings);

        // act
        maybeTriggerPipelines(
            project,
            deliveryMachineSettings,
            createStageGroupState(),
            state
        );

        // assert
        ArgumentCaptor<CreateReleaseCommandBuilder> captor = ArgumentCaptor.forClass(CreateReleaseCommandBuilder.class);
        verify(releaseService, times(1)).launchRelease(captor.capture());

        Assert.assertEquals(PROJECT_ID, captor.getValue().getProjectId());
        Assert.assertEquals(PIPE_ID, captor.getValue().getPipeId());
        Assert.assertEquals(TRIGGERED_BY, captor.getValue().getTriggeredBy());

        verify(deliveryMachineStateDao, times(1))
            .setLastProcessedRevision(STAGE_GROUP_ID, ARCADIA_HOTFIX_REVISION);
    }

    @Test
    public void shouldLaunchHotfixPipeline_WhenHotfixPipelineIsSet_AndNoOtherHotfixesRunning() {
        DeliveryMachineState state = deliveryMachineState(
            Long.toString(ARCADIA_REVISION_1), Long.toString(ARCADIA_REVISION_2)
        );

        setHotfixParams(state);

        DeliveryMachineEntity deliveryMachineSettings = new DeliveryMachineEntity(
            createArcadiaSettingsBuilder()
                .withHotfixPipeline(HOTFIX_PIPE_ID, "hotfix pipeline", mock(OrdinalTitleProvider.class))
                .build(), MONGO_CONVERTER
        );

        ProjectEntity project = project(PROJECT_ID, deliveryMachineSettings);

        // act
        maybeTriggerPipelines(
            project,
            deliveryMachineSettings,
            createStageGroupState(),
            state
        );

        // assert
        ArgumentCaptor<CreateReleaseCommandBuilder> captor = ArgumentCaptor.forClass(CreateReleaseCommandBuilder.class);
        verify(releaseService, times(1)).launchRelease(captor.capture());

        Assert.assertEquals(PROJECT_ID, captor.getValue().getProjectId());
        Assert.assertEquals(HOTFIX_PIPE_ID, captor.getValue().getPipeId());
        Assert.assertEquals(TRIGGERED_BY, captor.getValue().getTriggeredBy());
    }

    @Test
    public void shouldInsertArcadiaJobForHotfix() {
        DeliveryMachineState state = deliveryMachineState(
            Long.toString(ARCADIA_REVISION_1), Long.toString(ARCADIA_REVISION_2)
        );

        setHotfixParams(state);

        DeliveryMachineEntity deliveryMachineSettings = createArcadiaSettings();

        ProjectEntity project = project(PROJECT_ID, deliveryMachineSettings);

        // act
        maybeTriggerPipelines(
            project,
            deliveryMachineSettings,
            createStageGroupState(),
            state
        );

        // assert
        ArgumentCaptor<CreateReleaseCommandBuilder> captor = ArgumentCaptor.forClass(CreateReleaseCommandBuilder.class);
        verify(releaseService, times(1)).launchRelease(captor.capture());

        Assert.assertEquals(PROJECT_ID, captor.getValue().getProjectId());
        Assert.assertEquals(PIPE_ID, captor.getValue().getPipeId());
        Assert.assertEquals(TRIGGERED_BY, captor.getValue().getTriggeredBy());

        Pipeline pipeline = captor.getValue().getCustomPipeline();
        Assert.assertEquals(
            ArcadiaHotfixJob.class,
            pipeline.getJobs().stream()
                .filter(job -> job.getUpstreams().isEmpty())
                .findFirst().orElseThrow(RuntimeException::new)
                .getExecutorClass()
        );
    }

    @Test
    public void shouldInsertGithubJobForHotfix() {
        DeliveryMachineState state = deliveryMachineState(REVISION_1, REVISION_2);

        setHotfixParams(state);

        ProjectEntity project = project(PROJECT_ID, GITHUB_DELIVERY_MACHINE_SETTINGS);

        // act
        maybeTriggerPipelines(
            project,
            GITHUB_DELIVERY_MACHINE_SETTINGS,
            createStageGroupState(),
            state
        );

        // assert
        ArgumentCaptor<CreateReleaseCommandBuilder> captor = ArgumentCaptor.forClass(CreateReleaseCommandBuilder.class);
        verify(releaseService, times(1)).launchRelease(captor.capture());

        Assert.assertEquals(PROJECT_ID, captor.getValue().getProjectId());
        Assert.assertEquals(PIPE_ID, captor.getValue().getPipeId());
        Assert.assertEquals(TRIGGERED_BY, captor.getValue().getTriggeredBy());

        Pipeline pipeline = captor.getValue().getCustomPipeline();

        Assert.assertEquals(
            GithubHotfixJob.class,
            pipeline.getJobs().stream()
                .filter(job -> job.getUpstreams().isEmpty())
                .findFirst().orElseThrow(RuntimeException::new)
                .getExecutorClass()
        );
    }

    @Test
    public void shouldInsertBitbucketJobForHotfix() {
        DeliveryMachineState state = deliveryMachineState(REVISION_1, REVISION_2);

        setHotfixParams(state);

        DeliveryMachineEntity bitbucketSettings = new DeliveryMachineEntity(
            DeliveryMachineSettings.builder()
                .withBitbucketSettings("market-infra", "test-pipeline", "master")
                .withStageGroupId(STAGE_GROUP_ID)
                .withPipeline(PIPE_ID, "test-pipe", mock(OrdinalTitleProvider.class))
                .build(), MONGO_CONVERTER);

        ProjectEntity project = project(PROJECT_ID, bitbucketSettings);

        // act
        maybeTriggerPipelines(
            project,
            bitbucketSettings,
            createStageGroupState(),
            state
        );

        // assert
        ArgumentCaptor<CreateReleaseCommandBuilder> captor = ArgumentCaptor.forClass(CreateReleaseCommandBuilder.class);
        verify(releaseService, times(1)).launchRelease(captor.capture());

        Assert.assertEquals(PROJECT_ID, captor.getValue().getProjectId());
        Assert.assertEquals(PIPE_ID, captor.getValue().getPipeId());
        Assert.assertEquals(TRIGGERED_BY, captor.getValue().getTriggeredBy());

        Pipeline pipeline = captor.getValue().getCustomPipeline();

        Assert.assertEquals(
            BitbucketHotfixJob.class,
            pipeline.getJobs().stream()
                .filter(job -> job.getUpstreams().isEmpty())
                .findFirst().orElseThrow(RuntimeException::new)
                .getExecutorClass()
        );
    }

    @Test
    public void shouldGracefullyStopRunningReleases_WhenStartHotfix() {
        DeliveryMachineState state = deliveryMachineState(
            Long.toString(ARCADIA_REVISION_1), Long.toString(ARCADIA_REVISION_2)
        );

        setHotfixParams(state);

        DeliveryMachineEntity deliveryMachineSettings = createArcadiaSettings();

        ProjectEntity project = project(PROJECT_ID, deliveryMachineSettings);


        Release release = release();
        StageGroupState stageGroupState = createStageGroupState();
        stageGroupState.enqueue(new StageGroupState.QueueItem(release.getPipeLaunchId()));

        when(releaseDao.getReleasesByPipeLaunchIds(any())).thenReturn(Collections.singletonList(release));

        // act
        maybeTriggerPipelines(
            project,
            deliveryMachineSettings,
            stageGroupState,
            state
        );

        // assert
        ArgumentCaptor<CreateReleaseCommandBuilder> captor = ArgumentCaptor.forClass(CreateReleaseCommandBuilder.class);
        verify(releaseService, times(1)).launchRelease(captor.capture());

        Assert.assertEquals(PROJECT_ID, captor.getValue().getProjectId());
        Assert.assertEquals(PIPE_ID, captor.getValue().getPipeId());
        Assert.assertEquals(TRIGGERED_BY, captor.getValue().getTriggeredBy());

        verify(releaseService, times(1)).cancelRelease(
            eq(release), eq(FinishCause.cancelledByFix(ARCADIA_HOTFIX_REVISION)), eq(true), any(), eq(false)
        );
    }

    @Test
    public void shouldTriggerDeployOnManualRequest() {
        // arrange
        DeliveryMachineState state = deliveryMachineState(
            Long.toString(ARCADIA_REVISION_1), null
        );

        state.setTriggerRequests(
            Collections.singletonList(new TriggerRequest(Long.toString(ARCADIA_REVISION_2), false, "algebraic"))
        );

        // act
        maybeTriggerPipelines(
            project(PROJECT_ID, createArcadiaSettings()),
            createStageGroupState(),
            state
        );

        // assert
        ArgumentCaptor<CreateReleaseCommandBuilder> captor = ArgumentCaptor.forClass(CreateReleaseCommandBuilder.class);
        verify(releaseService, times(1)).launchRelease(captor.capture());
        Assert.assertEquals(PROJECT_ID, captor.getValue().getProjectId());
        Assert.assertEquals(PIPE_ID, captor.getValue().getPipeId());
        Assert.assertEquals("algebraic", captor.getValue().getTriggeredBy());

        verify(releaseService, Mockito.never()).cancelRelease(any(Release.class), any(), any());
    }

    @Test
    public void shouldTriggerRollbackOnRollbackRequest() {
        // arrange
        DeliveryMachineState state = deliveryMachineState(
            Long.toString(ARCADIA_REVISION_1), null
        );

        state.setRollbackRequests(
            Collections.singletonList(
                new RollbackRequest("releaseId", "braicalge", false, true)
            )
        );

        Release mockRelease = Release.builder()
            .withProjectId("projectId")
            .withPipeId("pipelineId")
            .withPipeLaunchId("launchId")
            .withCommit("1", Instant.now())
            .build();

        when(releaseDao.getRelease(Mockito.eq("releaseId"))).thenReturn(mockRelease);

        // act
        maybeTriggerPipelines(
            project(PROJECT_ID, createArcadiaSettings()),
            createStageGroupState(),
            state
        );

        // assert
        ArgumentCaptor<CreateReleaseCommandBuilder> captor = ArgumentCaptor.forClass(CreateReleaseCommandBuilder.class);
        verify(releaseService, times(1)).launchRelease(captor.capture());
        Assert.assertEquals(PROJECT_ID, captor.getValue().getProjectId());
        Assert.assertEquals(PIPE_ID, captor.getValue().getPipeId());
        Assert.assertEquals("braicalge", captor.getValue().getTriggeredBy());

        verify(releaseService, Mockito.never()).cancelRelease(any(Release.class), any(), any());
    }

    @Test
    public void shouldNotTriggerDuplicateRelease_WhenUnprocessedRevisionIsEqualToTriggeredOne() {
        // arrange
        DeliveryMachineState state = deliveryMachineState(
            null, REVISION_2
        );

        state.setTriggerRequests(
            Collections.singletonList(new TriggerRequest(REVISION_2, false, "algebraic"))
        );

        // act
        maybeTriggerPipelines(
            project(PROJECT_ID, createGithubSettings()),
            createStageGroupState(),
            state
        );

        // assert
        ArgumentCaptor<CreateReleaseCommandBuilder> captor = ArgumentCaptor.forClass(CreateReleaseCommandBuilder.class);
        verify(releaseService, times(1)).launchRelease(captor.capture());
        Assert.assertEquals(PROJECT_ID, captor.getValue().getProjectId());
        Assert.assertEquals(PIPE_ID, captor.getValue().getPipeId());
        Assert.assertEquals("algebraic", captor.getValue().getTriggeredBy());

        verify(deliveryMachineStateDao, times(1))
            .setLastProcessedRevision(any(), eq(REVISION_2));
    }

    @Test
    public void triggerRequestShouldNotLaunchNewRelease_IfThereIsRunningReleaseOnGreaterRevision() {
        // arrange
        DeliveryMachineState state = deliveryMachineState(
            null, null
        );

        TriggerRequest triggerRequest = new TriggerRequest(Long.toString(ARCADIA_REVISION_1), false, "algebraic");
        state.setTriggerRequests(
            Collections.singletonList(triggerRequest)
        );

        Release runningRelease = releaseBuilder()
            .withCommit(Long.toString(ARCADIA_REVISION_2), ARCADIA_COMMIT_2.getCreatedDate())
            .build();

        when(releaseDao.getReleasesByPipeLaunchIds(any())).thenReturn(Collections.singletonList(runningRelease));

        // act
        maybeTriggerPipelines(
            project(PROJECT_ID, createArcadiaSettings()),
            createStageGroupState(),
            state
        );

        // assert
        verify(releaseService, never()).launchRelease(any());

        verify(deliveryMachineStateDao, times(1))
            .dequeueTriggerRequest(STAGE_GROUP_ID, triggerRequest);
    }

    @Test
    public void fixRequestShouldNotLaunchNewRelease_IfItIsLessThanStableRevision() {
        // arrange
        DeliveryMachineState state = deliveryMachineState(
            Long.toString(ARCADIA_REVISION_2), null
        );

        TriggerRequest triggerRequest = new TriggerRequest(Long.toString(ARCADIA_REVISION_1), true, "algebraic");
        state.setTriggerRequests(
            Collections.singletonList(triggerRequest)
        );

        // act
        maybeTriggerPipelines(
            project(PROJECT_ID, createArcadiaSettings()),
            createStageGroupState(),
            state
        );

        // assert
        verify(releaseService, never()).launchRelease(any());

        verify(deliveryMachineStateDao, times(1))
            .dequeueTriggerRequest(STAGE_GROUP_ID, triggerRequest);
    }

    @Test
    public void shouldLaunchRelease_IfRulesNotTrueButHotfix() {
        DeliveryMachineState state = deliveryMachineState(
            Long.toString(ARCADIA_REVISION_1), Long.toString(ARCADIA_REVISION_2)
        );

        setHotfixParams(state);

        DeliveryMachineEntity deliveryMachineSettings = new DeliveryMachineEntity(
            createArcadiaSettingsBuilder()
                .withHotfixPipeline(HOTFIX_PIPE_ID, "hotfix pipeline", mock(OrdinalTitleProvider.class))
                .build(), MONGO_CONVERTER,
            Collections.singletonList(
                new RuleGroup(
                    Collections.singletonList(
                        new HasTicketRule("SOME_QUEUE")
                    ), MONGO_CONVERTER
                )
            )
        );

        ProjectEntity project = project(PROJECT_ID, deliveryMachineSettings);

        // act
        maybeTriggerPipelines(
            project,
            deliveryMachineSettings,
            createStageGroupState(),
            state
        );

        // assert
        ArgumentCaptor<CreateReleaseCommandBuilder> captor = ArgumentCaptor.forClass(CreateReleaseCommandBuilder.class);
        verify(releaseService, times(1)).launchRelease(captor.capture());

        Assert.assertEquals(PROJECT_ID, captor.getValue().getProjectId());
        Assert.assertEquals(HOTFIX_PIPE_ID, captor.getValue().getPipeId());
        Assert.assertEquals(TRIGGERED_BY, captor.getValue().getTriggeredBy());
    }

    @Test
    public void shouldLaunchRelease_IfRulesNotTrueButRollback() {
        // arrange
        DeliveryMachineState state = deliveryMachineState(
            Long.toString(ARCADIA_REVISION_1), null
        );

        state.setRollbackRequests(
            Collections.singletonList(
                new RollbackRequest("releaseId", "braicalge", false, true)
            )
        );

        Release mockRelease = Release.builder()
            .withProjectId("projectId")
            .withPipeId("pipelineId")
            .withPipeLaunchId("launchId")
            .withCommit("1", Instant.now())
            .build();

        when(releaseDao.getRelease(Mockito.eq("releaseId"))).thenReturn(mockRelease);

        // act
        maybeTriggerPipelines(
            project(PROJECT_ID, createArcadiaSettings()),
            getArcadiaDeliveryMachineEntity(
                Collections.singletonList(
                    new RuleGroup(
                        Collections.singletonList(
                            new HasTicketRule("SOME_QUEUE")
                        ), MONGO_CONVERTER
                    )
                )
            ),
            createStageGroupState(),
            state
        );

        // assert
        ArgumentCaptor<CreateReleaseCommandBuilder> captor = ArgumentCaptor.forClass(CreateReleaseCommandBuilder.class);
        verify(releaseService, times(1)).launchRelease(captor.capture());
        Assert.assertEquals(PROJECT_ID, captor.getValue().getProjectId());
        Assert.assertEquals(PIPE_ID, captor.getValue().getPipeId());
        Assert.assertEquals("braicalge", captor.getValue().getTriggeredBy());

        verify(releaseService, Mockito.never()).cancelRelease(any(Release.class), any(), any());
    }

    @Test
    public void shouldNotLaunchRelease_IfRulesNotTrue() {
        // act
        maybeTriggerPipelines(
            createStageGroupState(),
            deliveryMachineState(null, Long.toString(ARCADIA_REVISION_2)),
            getArcadiaDeliveryMachineEntity(
                Collections.singletonList(
                    new RuleGroup(
                        Arrays.asList(
                            new HasTicketRule("SOME_QUEUE"),
                            new DirectoryRule("**")
                        ), MONGO_CONVERTER
                    )
                )
            )
        );

        // assert
        verify(releaseService, Mockito.never()).launchRelease(any());

        verify(deliveryMachineStateDao, never())
            .setLastProcessedRevision(STAGE_GROUP_ID, Long.toString(ARCADIA_REVISION_2));

    }

    @Test
    public void shouldNotLaunchRelease_IfRulesNotValid() {
        // act
        maybeTriggerPipelines(
            createStageGroupState(),
            deliveryMachineState(null, Long.toString(ARCADIA_REVISION_2)),
            getArcadiaDeliveryMachineEntity(
                Collections.singletonList(
                    new RuleGroup(
                        Arrays.asList(
                            new DepartmentRule(DEPARTMENT_ID)
                        ), MONGO_CONVERTER
                    )
                )
            )
        );

        // assert
        verify(releaseService, Mockito.never()).launchRelease(any());

        verify(deliveryMachineStateDao, times(0))
            .setLastProcessedRevision(STAGE_GROUP_ID, Long.toString(ARCADIA_REVISION_2));

    }

    @Test
    public void shouldNotLaunchRelease_IfBlockedByDynamicConfig() {
        final String blockedByDynConfigProjectId = "blocked-by-dyn-config-project-id";
        when(deliveryMachineDynConfigService.canCreateRelease(blockedByDynConfigProjectId)).thenReturn(false);

        ProjectEntity project = project(blockedByDynConfigProjectId);

        maybeTriggerPipelines(
            project,
            createStageGroupState(),
            deliveryMachineState(null, Long.toString(ARCADIA_REVISION_2))
        );

        verify(releaseService, Mockito.never()).launchRelease(any());

        verify(deliveryMachineStateDao, times(0))
            .setLastProcessedRevision(STAGE_GROUP_ID, Long.toString(ARCADIA_REVISION_2));
    }

    @Test
    public void shouldLaunchRelease_IfBlockedByDynamicConfigButHotfix() {
        final String blockedByDynConfigProjectId = "blocked-by-dyn-config-project-id";
        when(deliveryMachineDynConfigService.canCreateRelease(blockedByDynConfigProjectId)).thenReturn(false);

        DeliveryMachineState state = deliveryMachineState(
            Long.toString(ARCADIA_REVISION_1), Long.toString(ARCADIA_REVISION_2)
        );

        setHotfixParams(state);

        DeliveryMachineEntity deliveryMachineSettings = new DeliveryMachineEntity(
            createArcadiaSettingsBuilder()
                .withHotfixPipeline(HOTFIX_PIPE_ID, "hotfix pipeline", mock(OrdinalTitleProvider.class))
                .build(), MONGO_CONVERTER
        );

        ProjectEntity project = project(blockedByDynConfigProjectId, deliveryMachineSettings);

        // act
        maybeTriggerPipelines(
            project,
            deliveryMachineSettings,
            createStageGroupState(),
            state
        );

        // assert
        ArgumentCaptor<CreateReleaseCommandBuilder> captor = ArgumentCaptor.forClass(CreateReleaseCommandBuilder.class);
        verify(releaseService, times(1)).launchRelease(captor.capture());

        Assert.assertEquals(blockedByDynConfigProjectId, captor.getValue().getProjectId());
        Assert.assertEquals(HOTFIX_PIPE_ID, captor.getValue().getPipeId());
        Assert.assertEquals(TRIGGERED_BY, captor.getValue().getTriggeredBy());
    }

    @Test
    public void testDeliveryMachineEntitySerializes() {
        String title = "testDeliveryMachineEntitySerializes";
        RuleGroup ruleGroup = new RuleGroup(
            Arrays.asList(
                new HasTicketRule(COMMIT_QUEUE),
                new DepartmentRule(DEPARTMENT_ID),
                new DirectoryRule(FILE_REGEXP_PATTERN, PathPatternType.REGEX)
            ), mongoTemplate.getConverter()
        );

        DeliveryMachineEntity originalEntity = new DeliveryMachineEntity(
            DeliveryMachineSettings.builder()
                .withTitle(title)
                .withGithubSettings("market-infra/test-pipeline")
                .withStageGroupId(STAGE_GROUP_ID)
                .withPipeline(PIPE_ID, "test-pipe", mock(OrdinalTitleProvider.class))
                .build(), mongoTemplate.getConverter(), Collections.singletonList(
            ruleGroup
        ));

        mongoTemplate.insert(originalEntity);

        DeliveryMachineEntity deserializedEntity = mongoTemplate.findOne(
            Query.query(Criteria.where("title").is(title)), DeliveryMachineEntity.class
        );

        deserializedEntity.getVcsSettings().instantiate(mongoTemplate.getConverter());
        deserializedEntity.getRuleGroups().forEach(
            group -> group.getLaunchRules().forEach(
                rule -> rule.instantiate(mongoTemplate.getConverter())));

        Assert.assertEquals(originalEntity, deserializedEntity);

        Assert.assertEquals(originalEntity.getVcsSettings().get(), deserializedEntity.getVcsSettings().get());

        HasTicketRule hasTicketRule =
            (HasTicketRule) originalEntity.getRuleGroups().get(0).getLaunchRules().get(0).get();
        DepartmentRule departmentRule =
            (DepartmentRule) originalEntity.getRuleGroups().get(0).getLaunchRules().get(1).get();
        DirectoryRule directoryRule =
            (DirectoryRule) originalEntity.getRuleGroups().get(0).getLaunchRules().get(2).get();

        Assert.assertEquals(hasTicketRule.getQueue(), COMMIT_QUEUE);
        Assert.assertEquals(departmentRule.getDepartmentId(), DEPARTMENT_ID);
        Assert.assertEquals(directoryRule.getFilePattern(), FILE_REGEXP_PATTERN);
    }

    private void setHotfixParams(DeliveryMachineState state) {
        state.setHotfixesParams(
            Collections.singletonList(
                new HotfixParams(
                    ARCADIA_HOTFIX_REVISION,
                    new HashSet<>(Collections.singletonList(Long.toString(ARCADIA_REVISION_1))),
                    "me",
                    PipelineType.HOTFIX,
                    true,
                    false,
                    false
                )
            )
        );
    }

    private DeliveryMachineEntity createArcadiaSettings() {
        return new DeliveryMachineEntity(
            createArcadiaSettingsBuilder().build(),
            MONGO_CONVERTER,
            Collections.singletonList(
                new RuleGroup(
                    Collections.singletonList(
                        new DirectoryRule(FILE_REGEXP_PATTERN, PathPatternType.REGEX)
                    ), MONGO_CONVERTER
                )
            )
        );
    }

    private DeliveryMachineEntity createGithubSettings() {
        return new DeliveryMachineEntity(
            createGithubSettingsBuilder().build(),
            MONGO_CONVERTER,
            Collections.emptyList()
        );
    }

    private DeliveryMachineSettings.Builder createArcadiaSettingsBuilder() {
        return DeliveryMachineSettings.builder()
            .withStageGroupId(STAGE_GROUP_ID)
            .withArcadiaSettings()
            .withPipeline(PIPE_ID, "test-pipe", mock(OrdinalTitleProvider.class));
    }

    private DeliveryMachineSettings.Builder createGithubSettingsBuilder() {
        return DeliveryMachineSettings.builder()
            .withStageGroupId(STAGE_GROUP_ID)
            .withGithubSettings(new GitHubSettings("repo", "master"))
            .withPipeline(PIPE_ID, "test-pipe", mock(OrdinalTitleProvider.class));
    }

    private void maybeTriggerPipelines(StageGroupState stageGroupState, DeliveryMachineState deliveryMachineState) {
        ProjectEntity project = project(PROJECT_ID);

        maybeTriggerPipelines(
            project,
            project.getDeliveryMachine(PIPE_ID),
            stageGroupState,
            deliveryMachineState
        );
    }

    private void maybeTriggerPipelines(StageGroupState stageGroupState, DeliveryMachineState deliveryMachineState,
                                       DeliveryMachineEntity machineSettings) {
        ProjectEntity project = project(PROJECT_ID);

        maybeTriggerPipelines(
            project,
            machineSettings,
            stageGroupState,
            deliveryMachineState
        );
    }

    private void maybeTriggerPipelines(ProjectEntity project,
                                       DeliveryMachineEntity machineSettings,
                                       StageGroupState stageGroupState,
                                       DeliveryMachineState machineState) {
        Optional<DeliveryMachineState> existingState = deliveryMachineStateDao.getById(machineState.getId());
        if (existingState.isPresent()) {
            deliveryMachineStateDao.delete(existingState.get());
            existingState = deliveryMachineStateDao.getById(machineState.getId());
            Assert.assertFalse(existingState.isPresent());
        }
        deliveryMachineStateDao.insert(machineState);

        sut.maybeTriggerPipelines(
            project,
            machineSettings,
            stageGroupState,
            machineState
        );
    }

    private void maybeTriggerPipelines(ProjectEntity project,
                                       StageGroupState stageGroupState,
                                       DeliveryMachineState deliveryMachineState) {
        maybeTriggerPipelines(
            project,
            project.getDeliveryMachine(PIPE_ID),
            stageGroupState,
            deliveryMachineState
        );
    }


    private Release release() {
        return releaseBuilder().build();
    }

    private Release.Builder releaseBuilder() {
        return Release.builder()
            .withProjectId(PROJECT_ID)
            .withPipeId(PIPE_ID)
            .withPipeLaunchId(PIPE_LAUNCH_ID)
            .withResources(Collections.singletonList(mock(StoredResource.class)))
            .withTriggeredBy(TRIGGERED_BY)
            .withDeliveryMachineId(STAGE_GROUP_ID)
            .withCommit(REVISION_1, REVISION_1_DATE.toInstant());
    }

    private DeliveryMachineState deliveryMachineState(String stableRevision, String lastUnprocessedRevision) {
        DeliveryMachineState machineState = new DeliveryMachineState(STAGE_GROUP_ID, stableRevision);
        machineState.setLastUnprocessedRevision(lastUnprocessedRevision);
        machineState.setLastProcessedRevision(stableRevision);
        machineState.getTriggerSettings().setTriggerNewPipelines(true);
        machineState.setVersion(1L);
        return machineState;
    }

    private StageGroupState createStageGroupState() {
        return TestStageGroupStateFactory.create(STAGE_GROUP_ID);
    }

    private StageGroupState createStageGroupState(Map<String, String> pipeLaunchIdToAcquiredStageMap) {
        StageGroupState stageGroupState = createStageGroupState();
        for (Map.Entry<String, String> entry : pipeLaunchIdToAcquiredStageMap.entrySet()) {
            String pipeLaunchId = entry.getKey();
            String acquiredStage = entry.getValue();
            StageGroupState.QueueItem queueItem = new StageGroupState.QueueItem(pipeLaunchId);
            queueItem.addStageId(acquiredStage);
            stageGroupState.enqueue(queueItem);
        }
        return stageGroupState;
    }

    private static ProjectEntity project(String projectId) {
        return project(projectId, GITHUB_DELIVERY_MACHINE_SETTINGS);
    }

    private static ProjectEntity project(String projectId, DeliveryMachineEntity deliveryMachineSettings) {
        ProjectEntity project = new ProjectEntity();
        project.setId(projectId);
        project.setTitle("test title");
        project.setRuleGroups(PROJECT_RULE_GROUPS);

        project.setDeliveryMachines(Collections.singletonList(deliveryMachineSettings));

        return project;
    }

    private SVNLogEntry svnLogEntry(long revision, List<String> changedPaths) {
        Map<String, String> changedPathsMap = changedPaths.stream()
            .collect(Collectors.toMap(Function.identity(), Function.identity()));

        return new SVNLogEntry(changedPathsMap, revision, "", new Date(), ANOTHER_COMMIT_QUEUE + "-123 change");
    }

    private BitbucketCommit bitbucketCommit(String revision, String author, String message) {
        BitbucketCommit commit = Mockito.mock(BitbucketCommit.class);

        when(commit.getId()).thenReturn(revision);
        when(commit.getMessage()).thenReturn(message);

        BitbucketUser user = Mockito.mock(BitbucketUser.class);
        when(user.getName()).thenReturn(author);

        when(commit.getCommitter()).thenReturn(user);

        return commit;
    }
}
