package ru.yandex.market.tsum.pipelines.common.jobs.delivery.arcadia;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNLogEntryPath;
import org.tmatesoft.svn.core.wc.SVNRevision;

import ru.yandex.market.tsum.arcadia.ArcadiaCache;
import ru.yandex.market.tsum.clients.arcadia.ArcadiaEntryHandler;
import ru.yandex.market.tsum.clients.arcadia.ArcadiaGetChangeLogProgressListener;
import ru.yandex.market.tsum.clients.arcadia.RootArcadiaClient;
import ru.yandex.market.tsum.clients.sandbox.SandboxClient;
import ru.yandex.market.tsum.clients.sandbox.SandboxTask;
import ru.yandex.market.tsum.clients.sandbox.TaskInputDto;
import ru.yandex.market.tsum.clients.sandbox.TaskResource;
import ru.yandex.market.tsum.context.TestTsumJobContext;
import ru.yandex.market.tsum.dao.ProjectsDao;
import ru.yandex.market.tsum.entity.project.DeliveryMachineEntity;
import ru.yandex.market.tsum.entity.project.ProjectEntity;
import ru.yandex.market.tsum.pipe.engine.definition.resources.Resource;
import ru.yandex.market.tsum.pipe.engine.runtime.config.JobTesterConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.JobTester;
import ru.yandex.market.tsum.pipelines.common.resources.ChangelogEntry;
import ru.yandex.market.tsum.pipelines.common.resources.ChangelogInfo;
import ru.yandex.market.tsum.pipelines.common.resources.DeliveryPipelineParams;
import ru.yandex.market.tsum.release.RepositoryType;
import ru.yandex.market.tsum.release.dao.ArcadiaSettings;
import ru.yandex.market.tsum.release.dao.DeliveryMachineSettings;
import ru.yandex.market.tsum.release.dao.FinishCause;
import ru.yandex.market.tsum.release.dao.Release;
import ru.yandex.market.tsum.release.dao.ReleaseDao;
import ru.yandex.market.tsum.release.dao.ReleaseService;
import ru.yandex.market.tsum.release.dao.delivery.launch_rules.HasTicketRule;
import ru.yandex.market.tsum.release.dao.delivery.launch_rules.HaveNumberOfCommitsRule;
import ru.yandex.market.tsum.release.dao.delivery.launch_rules.LaunchRuleChecker;
import ru.yandex.market.tsum.release.dao.delivery.launch_rules.RuleGroup;
import ru.yandex.market.tsum.release.dao.title_providers.OrdinalTitleProvider;
import ru.yandex.market.tsum.release.delivery.ArcadiaVcsChange;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.internal.stubbing.answers.AnswerFunctionalInterfaces.toAnswer;
import static ru.yandex.market.tsum.test_utils.SandboxTestUtils.getParameterValue;
import static ru.yandex.market.tsum.test_utils.SandboxTestUtils.getParameterValueOrDefault;
import static ru.yandex.market.tsum.test_utils.SandboxTestUtils.successfulTaskDto;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {JobTesterConfig.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class GenerateArcadiaChangelogJobTest {
    private static final Long TASK_ID = 1L;

    private static final String MODULE_PATH = "market/svn-data";

    private static final String REVISION = "13";
    private static final String STABLE_REVISION = "2";
    private static final String MATCHING_QUEUE = "SOMEQUEUE";
    private static final String NON_MATCHING_QUEUE = "BADQUEUE";

    private static final int TOTAL_NUMBER_OF_COMMITS = 15;

    private static final List<String> MODULE_DEPENDENCIES = IntStream.range(1, TOTAL_NUMBER_OF_COMMITS + 1)
        .filter(num -> num % 2 == 1)
        .mapToObj(GenerateArcadiaChangelogJobTest::dependencyPath)
        .collect(Collectors.toList());

    // первые два коммита никогда не выбираются, у них ревизии STABLE_REVISION или раньше
    // последние два коммита тоже никогда не выбираются, у них ревизии больше REVISION
    // только у коммитов с чётными номерами изменения в каталогах, от которых зависит проект
    // только у первой половины коммитов в сообщениях указан тикет из подходящей очереди
    private static final List<SVNLogEntry> SOME_CHANGELOG = IntStream.range(1, TOTAL_NUMBER_OF_COMMITS + 1)
        .mapToObj(num -> {
            int ticketNum = 1000 + num;
            String details = "change " + num;
            String queue = num <= TOTAL_NUMBER_OF_COMMITS / 2 ? MATCHING_QUEUE : NON_MATCHING_QUEUE;
            return entry(num, commitMessage(queue, ticketNum, details), singletonChangedPath(num));
        })
        .collect(Collectors.toList());

    private static final MongoConverter MONGO_CONVERTER = mock(MongoConverter.class);

    @Autowired
    private JobTester jobTester;
    private SandboxClient sandboxClient;
    private ArgumentCaptor<TaskInputDto> taskInputCaptor;
    private ReleaseService releaseService;
    private final ArcadiaCache arcadiaCache = Mockito.mock(ArcadiaCache.class);
    private final RootArcadiaClient arcadiaClient = Mockito.mock(RootArcadiaClient.class);
    private List<SVNLogEntry> changelog;
    private LaunchRuleChecker launchRuleChecker = new LaunchRuleChecker(null, null, null, null);
    private final ArcadiaSettings arcadiaSettings = new ArcadiaSettings();
    private ReleaseDao releaseDao;

    @Value("${tsum.pipe-launcher.robot.login}")
    private String user;

    @Before
    public void setUp() {
        sandboxClient = mock(SandboxClient.class);

        Mockito.when(sandboxClient.newSandboxTaskRunner()).thenCallRealMethod();

        SandboxTask taskDto = successfulTaskDto(TASK_ID);

        taskInputCaptor = ArgumentCaptor.forClass(TaskInputDto.class);
        Mockito.when(sandboxClient.createTask(taskInputCaptor.capture())).thenReturn(taskDto);
        Mockito.when(sandboxClient.getTask(TASK_ID)).thenReturn(taskDto);

        TaskResource jsonResource = mock(TaskResource.class);
        Mockito.when(jsonResource.getType()).thenReturn(GenerateArcadiaChangelogJob.TSUM_JSON_RESOURCE);
        Mockito.when(jsonResource.getTask()).thenReturn(taskDto);
        Mockito.when(jsonResource.getId()).thenReturn(123L);

        Mockito.when(sandboxClient.getResources(TASK_ID)).thenReturn(Collections.singletonList(jsonResource));
        Mockito.when(sandboxClient.getResource(any(), Mockito.eq(ArcadiaDependenciesSandboxResult.class)))
            .thenReturn(new ArcadiaDependenciesSandboxResult(MODULE_DEPENDENCIES));

        releaseService = mock(ReleaseService.class);
        Mockito.when(releaseService.getVcsSettings(any(), any())).thenReturn(arcadiaSettings);

        DeliveryMachineEntity deliveryMachineEntity = new DeliveryMachineEntity(
            DeliveryMachineSettings.builder()
                .withArcadiaSettings()
                .withPipeline("pipeId", "pipe title", mock(OrdinalTitleProvider.class))
                .withStageGroupId("stage_group_id")
                .build(), MONGO_CONVERTER,
            Collections.singletonList(
                new RuleGroup(
                    Collections.singletonList(
                        new HasTicketRule(MATCHING_QUEUE)
                    ), MONGO_CONVERTER
                )
            )
        );

        Mockito.when(releaseService.getDeliveryMachineSettings(any())).thenReturn(deliveryMachineEntity);

        changelog = SOME_CHANGELOG;
        doAnswer(this::getMockChangelog)
            .when(arcadiaClient).getChangelog(
            any(SVNRevision.class), any(SVNRevision.class),
            anyLong(), anyBoolean(),
            anyList(),
            any(ArcadiaEntryHandler.class),
            anyBoolean(), any(ArcadiaGetChangeLogProgressListener.class));
        when(arcadiaClient.getHead()).thenReturn(new SVNLogEntry(Collections.emptyMap(), 30L, null, null, null));

        when(arcadiaCache.get(anyLong()))
            .thenAnswer(toAnswer((Long revision) -> ArcadiaVcsChange.from(entryByRevision(revision))));

        ProjectsDao projectsDao = mock(ProjectsDao.class);
        when(projectsDao.get(any())).thenReturn(new ProjectEntity());

        launchRuleChecker = new LaunchRuleChecker(null, null, null, projectsDao);

        releaseDao = mock(ReleaseDao.class);
        Mockito.when(releaseDao.getReleaseByPipeLaunchId(any(ObjectId.class))).thenReturn(
            Release.builder()
                .withProjectId("projectId")
                .withPipeId("pipeId")
                .withPipeLaunchId("pipeLaunchId")
                .withCommit(REVISION, Instant.ofEpochMilli(5))
                .build()
        );

        Mockito.when(releaseDao.getReleasesByPipeLaunchIds(any())).thenReturn(Collections.emptyList());
    }

    @Test
    public void producesChangelog() throws Exception {
        GenerateArcadiaChangelogJob sut = createSut();

        TestTsumJobContext jobContext = new TestTsumJobContext(releaseService, releaseDao, user, launchRuleChecker);
        sut.execute(jobContext);

        TaskInputDto taskInputDto = taskInputCaptor.getValue();
        assertEquals(MODULE_PATH, getParameterValue(taskInputDto, "module_path"));

        List<ChangelogEntry> filteredChangelog = changelogEntriesByRevisions(3, 5, 7);

        ChangelogInfo changelogInfo = jobContext.getResource(ChangelogInfo.class);
        assertEquals(changelogEntriesByRevisions(3, 5, 7, 9, 11, 13), changelogInfo.getChangelogEntries());
        assertEquals(filteredChangelog, changelogInfo.getFilteredChangelogEntries());

        Mockito.verify(releaseService, Mockito.times(1))
            .saveChangelog(jobContext.getPipeLaunch().getId(), filteredChangelog, RepositoryType.ARCADIA);
    }

    @Test
    public void cancelReleaseIfNotMatchingCommonFilters() throws Exception {
        DeliveryMachineEntity deliveryMachineEntity = new DeliveryMachineEntity(
            DeliveryMachineSettings.builder()
                .withArcadiaSettings()
                .withPipeline("pipeId", "pipe title", mock(OrdinalTitleProvider.class))
                .withStageGroupId("stage_group_id")
                .build(), MONGO_CONVERTER,
            Collections.singletonList(
                new RuleGroup(
                    Arrays.asList(
                        new HasTicketRule(MATCHING_QUEUE),
                        new HaveNumberOfCommitsRule(700)
                    ), MONGO_CONVERTER
                )
            )
        );

        Mockito.when(releaseService.getDeliveryMachineSettings(any())).thenReturn(deliveryMachineEntity);

        GenerateArcadiaChangelogJob sut = createSut();

        TestTsumJobContext jobContext = new TestTsumJobContext(releaseService, releaseDao, user, launchRuleChecker);
        sut.execute(jobContext);

        Mockito.verify(releaseService, Mockito.times(1))
            .cancelRelease(
                any(ObjectId.class), Mockito.eq(FinishCause.nothingToRelease()), any()
            );
    }

    @Test
    public void producesChangelogForHotfix() throws Exception {
        GenerateArcadiaChangelogJob sut = createSut(
            new DeliveryPipelineParams(REVISION, STABLE_REVISION, STABLE_REVISION, "branch")
        );

        TestTsumJobContext jobContext = new TestTsumJobContext(releaseService, releaseDao, user, launchRuleChecker);
        sut.execute(jobContext);

        TaskInputDto taskInputDto = taskInputCaptor.getValue();
        assertEquals(MODULE_PATH, getParameterValueOrDefault(taskInputDto, "module_path"));

        List<ChangelogEntry> filteredChangelog = changelogEntriesByRevisions(3, 5, 7, 13);
        ChangelogInfo changelogInfo = jobContext.getResource(ChangelogInfo.class);
        assertEquals(changelogEntriesByRevisions(3, 5, 7, 9, 11, 13, 15), changelogInfo.getChangelogEntries());
        assertEquals(filteredChangelog, changelogInfo.getFilteredChangelogEntries());

        /* It's ok not to filter changelog of hotfix, because there are only cherry-picks of revisions we picked */
        Mockito.verify(releaseService, Mockito.times(1))
            .saveChangelog(
                jobContext.getPipeLaunch().getId(), changelogInfo.getChangelogEntries(), RepositoryType.ARCADIA
            );
    }

    @Test
    public void producesReleaseRevisionChangelogIfNoStableRevision() throws Exception {
        ArcadiaVcsChange logEntry = ArcadiaVcsChange.from(entryByRevision(Long.parseLong(REVISION)));

        GenerateArcadiaChangelogJob sut = createSut(
            new DeliveryPipelineParams(String.valueOf(logEntry.getRevision()), null, null)
        );

        TestTsumJobContext jobContext = new TestTsumJobContext(releaseService, releaseDao, user, launchRuleChecker);
        sut.execute(jobContext);

        Mockito.verifyZeroInteractions(sandboxClient);

        ChangelogInfo changelogInfo = jobContext.getResource(ChangelogInfo.class);

        ChangelogEntry changelogEntry = changelogEntry(logEntry);

        assertEquals(1, changelogInfo.getChangelogEntries().size());
        assertEquals(1, changelogInfo.getFilteredChangelogEntries().size());

        assertEquals(changelogEntry, changelogInfo.getChangelogEntries().get(0));
        assertEquals(changelogEntry, changelogInfo.getFilteredChangelogEntries().get(0));

        Mockito.verify(releaseService, Mockito.times(1))
            .saveChangelog(jobContext.getPipeLaunch().getId(), Collections.singletonList(changelogEntry),
                RepositoryType.ARCADIA);
    }

    @Test
    public void noChangelog_ShouldFinishRelease() throws Exception {
        changelog = Collections.emptyList();

        GenerateArcadiaChangelogJob sut = createSut();

        sut.execute(new TestTsumJobContext(releaseService, mock(ReleaseDao.class), user, launchRuleChecker));

        Mockito.verify(releaseService, Mockito.times(1))
            .cancelRelease(
                any(ObjectId.class), Mockito.eq(FinishCause.nothingToRelease()), any()
            );
    }

    @Test
    public void noStableRevision_ShouldFinishImmediately() throws Exception {
        GenerateArcadiaChangelogJob sut = createSut(
            new DeliveryPipelineParams("100500", null, null)
        );

        sut.execute(new TestTsumJobContext(releaseService, releaseDao, user, launchRuleChecker));

        Mockito.verifyZeroInteractions(sandboxClient);
    }

    private Void getMockChangelog(InvocationOnMock invocation) throws SVNException {
        SVNRevision startRevision = invocation.getArgument(0);
        SVNRevision endRevision = invocation.getArgument(1);
        ArcadiaEntryHandler entryHandler = invocation.getArgument(5);
        for (SVNLogEntry entry : changelog) {
            long revision = entry.getRevision();
            if (revision < startRevision.getNumber() ||
                (endRevision != SVNRevision.HEAD && revision > endRevision.getNumber())) {
                continue;
            }

            entryHandler.handleLogEntry(entry);
        }
        return null;
    }

    private GenerateArcadiaChangelogJob createSut() {
        return createSut(new DeliveryPipelineParams(REVISION, STABLE_REVISION, STABLE_REVISION));
    }

    private GenerateArcadiaChangelogJob createSut(DeliveryPipelineParams deliveryPipelineParams) {
        return jobTester.jobInstanceBuilder(GenerateArcadiaChangelogJob.class)
            .withResources(
                GenerateArcadiaChangelogJob.configBuilder()
                    .withModulePath(MODULE_PATH)
                    .withFinishReleaseOnEmptyChangelog(true)
                    .build()
                    .toArray(new Resource[0])
            )
            .withResource(deliveryPipelineParams)
            .withBean(sandboxClient)
            .withBean(releaseService)
            .withBean(arcadiaCache)
            .withBean(arcadiaClient)
            .create();
    }

    private SVNLogEntry entryByRevision(long revision) {
        return changelog.stream()
            .filter(entry -> entry.getRevision() == revision)
            .findAny()
            .orElse(null);
    }

    public ChangelogEntry changelogEntry(ArcadiaVcsChange logEntry) {
        return new ChangelogEntry(
            String.valueOf(logEntry.getRevision()),
            logEntry.getCreatedDate().toEpochMilli() / 1000,
            logEntry.getMessage(),
            logEntry.getAuthor()
        );
    }

    public List<ChangelogEntry> changelogEntriesByRevisions(long... revisions) {
        return LongStream.of(revisions)
            .mapToObj(this::entryByRevision)
            .map(ArcadiaVcsChange::from)
            .map(this::changelogEntry)
            .collect(Collectors.toList());
    }

    private static String dependencyPath(int num) {
        return String.format("market/dep%d/", num);
    }

    private static String fullDependencyPath(int num) {
        return "trunk/arcadia/" + dependencyPath(num);
    }

    private static String commitMessage(String queue, int ticketNum, String details) {
        return String.format("%s-%d %s", queue, ticketNum, details);
    }

    private static SVNLogEntryPath path(String path) {
        return new SVNLogEntryPath(path, SVNLogEntryPath.TYPE_ADDED, null, -1);
    }

    private static Map<String, SVNLogEntryPath> pathMap(SVNLogEntryPath... paths) {
        return Stream.of(paths)
            .collect(Collectors.toMap(SVNLogEntryPath::getPath, Function.identity()));
    }

    private static Map<String, SVNLogEntryPath> singletonChangedPath(int num) {
        return pathMap(path(fullDependencyPath(num)));
    }

    private static SVNLogEntry entry(long revision, String message, Map<String, SVNLogEntryPath> changedPaths) {
        return new SVNLogEntry(changedPaths, revision, "author" + revision,
            new Date(revision * 1337), message);
    }
}
