package ru.yandex.market.tsum.tms.tasks.multitesting;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.service.IssueService;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.IteratorF;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.market.tsum.clients.github.GitHubClient;
import ru.yandex.market.tsum.clients.github.model.Branch;
import ru.yandex.market.tsum.clients.startrek.IssueBuilder;
import ru.yandex.market.tsum.config.MultitestingCleanupConfiguration;
import ru.yandex.market.tsum.config.MultitestingConfiguration;
import ru.yandex.market.tsum.config.ReleaseConfiguration;
import ru.yandex.market.tsum.dao.PipelinesDao;
import ru.yandex.market.tsum.dao.ProjectsDao;
import ru.yandex.market.tsum.entity.pipeline.PipelineEntity;
import ru.yandex.market.tsum.entity.pipeline.StoredConfigurationEntity;
import ru.yandex.market.tsum.entity.project.JanitorSettingEntity;
import ru.yandex.market.tsum.entity.project.ProjectEntity;
import ru.yandex.market.tsum.multitesting.MultitestingCleanupService;
import ru.yandex.market.tsum.multitesting.MultitestingService;
import ru.yandex.market.tsum.multitesting.MultitestingTimeToLiveCalculationService;
import ru.yandex.market.tsum.multitesting.model.EffectiveMultitestingTimeToLiveSettingsWithSources.Source;
import ru.yandex.market.tsum.multitesting.model.JanitorEventType;
import ru.yandex.market.tsum.multitesting.model.MultitestingEnvironment;
import ru.yandex.market.tsum.multitesting.model.MultitestingTimeToLiveSettings;
import ru.yandex.market.tsum.pipe.engine.definition.DummyJob;
import ru.yandex.market.tsum.pipe.engine.definition.Pipeline;
import ru.yandex.market.tsum.pipe.engine.definition.builder.PipelineBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.resources.Resource;
import ru.yandex.market.tsum.pipe.engine.runtime.config.MockCuratorConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.PipeServicesConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.TestConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.di.model.ResourceContainer;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.PipeTester;
import ru.yandex.market.tsum.pipe.engine.runtime.state.PipeLaunchDao;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.JobLaunch;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.JobState;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.PipeLaunch;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.StatusChange;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.StatusChangeType;
import ru.yandex.market.tsum.pipelines.common.jobs.multitesting.MultitestingTags;
import ru.yandex.market.tsum.pipelines.common.resources.TicketsList;
import ru.yandex.market.tsum.pipelines.multitesting.MultitestingTestConfig;
import ru.yandex.market.tsum.pipelines.multitesting.MultitestingTestData;
import ru.yandex.startrek.client.Comments;
import ru.yandex.startrek.client.Events;
import ru.yandex.startrek.client.Issues;
import ru.yandex.startrek.client.model.Comment;
import ru.yandex.startrek.client.model.CommentCreate;
import ru.yandex.startrek.client.model.CommentRef;
import ru.yandex.startrek.client.model.CommentUpdate;
import ru.yandex.startrek.client.model.Event;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.startrek.client.model.IssueRef;

import static java.lang.Integer.max;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tsum.tms.tasks.multitesting.TestCommentBuilder.aComment;
import static ru.yandex.market.tsum.tms.tasks.multitesting.TestEventBuilder.anEvent;
import static ru.yandex.market.tsum.tms.tasks.multitesting.TestEventFieldChangeBuilder.anEventFieldChange;
import static ru.yandex.market.tsum.tms.tasks.multitesting.TestFieldRefBuilder.aFieldRef;
import static ru.yandex.market.tsum.tms.tasks.multitesting.TestStatusBuilder.aStatus;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 10.01.2018
 */
@RunWith(Parameterized.class)
@ContextConfiguration(classes = {TestConfig.class, PipeServicesConfig.class, MultitestingConfiguration.class,
    ReleaseConfiguration.class, MultitestingTestConfig.class, MockCuratorConfig.class,
    MultitestingEnvironmentsJanitorCronTask.class, MultitestingEnvironmentsJanitorCronTaskTest.Config.class,
    MultitestingCleanupConfiguration.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class MultitestingEnvironmentsJanitorCronTaskTest {
    private static final String PROJECT = "test";
    private static final String NAME = "n1";
    private static final String PIPELINE_ID = MultitestingTestConfig.PIPE_WITH_CUSTOM_CLEANUP_ID;
    private static final String REPOSITORY_ID = "market-infra/test";
    private static final String BRANCH_NAME = "develop";

    private static final List<MultitestingTimeToLiveSettings> TTL_OPTIONS = List.of(
        ttlSettings(3, 14, 7), // "старые" настройки
        ttlSettings(1, 4, 4),  // "новые" настройки
        ttlSettings(2, 5, 8)   // настройки, которые ловили баги в этом юнит-тесте
    );

    private static final List<Function<MultitestingTimeToLiveSettings, Object>> TTL_DESCRIPTION_PROPERTY_ORDER =
        List.of(
            MultitestingTimeToLiveSettings::getDaysBetweenClosingTicketsAndCleanup,
            MultitestingTimeToLiveSettings::getDaysBetweenLastJobExecutionAndComment,
            MultitestingTimeToLiveSettings::getDaysBetweenCommentAndCleanup);

    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private MultitestingService multitestingService;

    @Autowired
    private PipeLaunchDao pipeLaunchDao;

    @Autowired
    private MultitestingEnvironmentsJanitorCronTask sut;

    @Autowired
    private MultitestingTimeToLiveCalculationService timeToLiveCalculationService;

    @Autowired
    private MultitestingCleanupService multitestingCleanupService;

    @Autowired
    private Issues startrekIssues;

    @Autowired
    private MockStartrekComments startrekComments;

    @Autowired
    private Events startrekEvents;

    @Autowired
    private PipeTester pipeTester;

    @Autowired
    private ProjectsDao projectsDao;

    @Autowired
    private PipelinesDao pipelinesDao;

    @Autowired
    private GitHubClient gitHubClient;

    private final MultitestingTimeToLiveSettings effectiveTimeToLiveSettings;
    private final Source settingSource;

    private final int daysBetweenClosingTicketsAndCleanup;
    private final int daysBetweenLastJobExecutionAndComment;
    private final int daysBetweenCommentAndCleanup;

    @Parameterized.Parameters(name = "source={0}, ttl={2}")
    public static Collection<Object[]> parameters() {
        return TTL_OPTIONS.stream()
            .flatMap(ttlSettings -> Stream.of(Source.values())
                .map(source -> new Object[]{source, ttlSettings, describeTtlSettings(ttlSettings)}))
            .collect(Collectors.toList());
    }

    public MultitestingEnvironmentsJanitorCronTaskTest(Source settingSource,
                                                       MultitestingTimeToLiveSettings effectiveTimeToLiveSettings,
                                                       String ttlDescription) {
        this.effectiveTimeToLiveSettings = effectiveTimeToLiveSettings;
        this.settingSource = settingSource;

        daysBetweenClosingTicketsAndCleanup = effectiveTimeToLiveSettings.getDaysBetweenClosingTicketsAndCleanup();
        daysBetweenLastJobExecutionAndComment = effectiveTimeToLiveSettings.getDaysBetweenLastJobExecutionAndComment();
        daysBetweenCommentAndCleanup = effectiveTimeToLiveSettings.getDaysBetweenCommentAndCleanup();
    }

    @Before
    public void setup() {
        timeToLiveCalculationService.setGlobalTimeToLiveSettings(
            settingSource == Source.GLOBAL ? effectiveTimeToLiveSettings : null);

        ProjectEntity project = new ProjectEntity();
        project.setMultitestingTimeToLiveSettings(
            settingSource == Source.PROJECT ? effectiveTimeToLiveSettings : null);

        Mockito.when(projectsDao.get(Mockito.anyString())).thenReturn(project);

        StoredConfigurationEntity pipelineConfiguration = new StoredConfigurationEntity();
        pipelineConfiguration.setMultitestingTimeToLiveSettings(
            settingSource == Source.PIPELINE ? effectiveTimeToLiveSettings : null);
        PipelineEntity pipeline = new PipelineEntity();
        pipeline.setCurrentConfiguration(pipelineConfiguration);
        when(pipelinesDao.get(Mockito.anyString())).thenReturn(pipeline);
    }

    @Test
    public void shouldDoNothing_whenThereAreNoLaunches() {
        createStaticEnvironment();
        assertDoesNothingOnDay(1);
    }

    @Test
    public void shouldClean_whenThereAreNoTickets() throws InterruptedException {
        createStaticLaunchedEnvironment();
        assertDoesNothingOnDay(daysBetweenLastJobExecutionAndComment + daysBetweenCommentAndCleanup - 1);
        assertStartsCleanupOnDay(daysBetweenLastJobExecutionAndComment + daysBetweenCommentAndCleanup);
    }

    @Test
    public void shouldIgnoreTicketsThatCanNotBeRetrieved() throws InterruptedException {
        when(startrekIssues.get(Mockito.anyString())).thenThrow(new RuntimeException());
        createStaticLaunchedEnvironment("FORBIDDEN-1");
        assertDoesNothingOnDay(daysBetweenLastJobExecutionAndComment + daysBetweenCommentAndCleanup - 1);
        assertStartsCleanupOnDay(daysBetweenLastJobExecutionAndComment + daysBetweenCommentAndCleanup);
    }

    @Test
    public void shouldClean_whenAllTicketsAreClosed() throws InterruptedException {
        createStaticLaunchedEnvironment(createClosedTicket(3));
        assertDoesNothingOnDay(3 + daysBetweenClosingTicketsAndCleanup - 1);
        assertStartsCleanupOnDay(3 + daysBetweenClosingTicketsAndCleanup);
    }

    @Test  // st/MARKETINFRA-2649
    public void shouldClean_whenAllTicketsAreClosedBeforeLastLaunch() throws InterruptedException {
        createStaticLaunchedEnvironment(createClosedTicket(-12));
        assertDoesNothingOnDay(daysBetweenClosingTicketsAndCleanup - 1);
        assertStartsCleanupOnDay(daysBetweenClosingTicketsAndCleanup);
    }

    @Test
    public void shouldWriteCommentAndClean_whenThereAreOpenTickets() throws InterruptedException {
        createStaticLaunchedEnvironment(createOpenTicket());
        assertDoesNothingOnDay(daysBetweenLastJobExecutionAndComment - 1);
        assertWritesStartrekCommentOnDay(daysBetweenLastJobExecutionAndComment);
        assertDoesNothingOnDay(daysBetweenLastJobExecutionAndComment + daysBetweenCommentAndCleanup - 1);
        assertStartsCleanupOnDay(daysBetweenLastJobExecutionAndComment + daysBetweenCommentAndCleanup);
    }

    @Test
    public void shouldWriteCommentAndClean_whenTicketWasClosedAndReopened() throws InterruptedException {
        createStaticLaunchedEnvironment(createReopenedTicket(1, 2));
        assertDoesNothingOnDay(daysBetweenLastJobExecutionAndComment - 1);
        assertWritesStartrekCommentOnDay(daysBetweenLastJobExecutionAndComment);
        assertDoesNothingOnDay(daysBetweenLastJobExecutionAndComment + daysBetweenCommentAndCleanup - 1);
        assertStartsCleanupOnDay(daysBetweenLastJobExecutionAndComment + daysBetweenCommentAndCleanup);
    }

    @Test  // Стартрек иногда возвращает историю в неправильном порядке
    public void shouldWriteCommentAndClean_whenTicketWasReopenedAndEventsAreInWrongOrder() throws InterruptedException {
        createStaticLaunchedEnvironment(createReopenedTicketWithEventsInWrongOrder(1, 2));
        assertDoesNothingOnDay(daysBetweenLastJobExecutionAndComment - 1);
        assertWritesStartrekCommentOnDay(daysBetweenLastJobExecutionAndComment);
        assertDoesNothingOnDay(daysBetweenLastJobExecutionAndComment + daysBetweenCommentAndCleanup - 1);
        assertStartsCleanupOnDay(daysBetweenLastJobExecutionAndComment + daysBetweenCommentAndCleanup);
    }

    @Test
    public void shouldClean_onlyWhenCommentWasWrittenLongEnoughAgo() throws InterruptedException {
        createStaticLaunchedEnvironment(createOpenTicket());
        assertWritesStartrekCommentOnDay(daysBetweenLastJobExecutionAndComment + 1);
        assertDoesNothingOnDay(daysBetweenLastJobExecutionAndComment + daysBetweenCommentAndCleanup);
        assertStartsCleanupOnDay(daysBetweenLastJobExecutionAndComment + daysBetweenCommentAndCleanup + 1);
    }

    @Test
    public void shouldWriteCommentOnlyOnce() throws InterruptedException {
        createStaticLaunchedEnvironment(createOpenTicket());
        executeJanitorTaskOnDay(daysBetweenLastJobExecutionAndComment);
        assertCommentCountIs(1);
        executeJanitorTaskOnDay(daysBetweenLastJobExecutionAndComment);
        assertCommentCountIs(1);
    }

    @Test
    public void shouldIgnoreCommentsAboutPreviousLaunches() throws InterruptedException {
        String ticketKey = createOpenTicket();
        createStaticLaunchedEnvironment(ticketKey);
        assertWritesStartrekCommentOnDay(daysBetweenLastJobExecutionAndComment);

        relaunchEnvironment(daysBetweenLastJobExecutionAndComment + 1, ticketKey);
        assertOneMoreComment(() -> {
            // в оригинальное время чистки не получается (но может написать комментарий, если к этому моменту
            // опять прошло daysBetweenLastJobExecutionAndComment дней)
            assertDoesNotCleanupOnDay(daysBetweenLastJobExecutionAndComment + daysBetweenCommentAndCleanup);

            // а если джоба не написала комментарий, то сейчас-то точно напишет
            executeJanitorTaskOnDay(daysBetweenLastJobExecutionAndComment * 2 + 1);
        });

        assertStartsCleanupOnDay(max(daysBetweenLastJobExecutionAndComment * 2 + daysBetweenCommentAndCleanup + 1,
            daysBetweenLastJobExecutionAndComment + daysBetweenCommentAndCleanup * 2));
    }

    @Test
    public void shouldDelayCleanupIfThereWasAnyActivityInLaunchPipeline() throws InterruptedException {
        createStaticLaunchedEnvironment(createOpenTicket());
        assertWritesStartrekCommentOnDay(daysBetweenLastJobExecutionAndComment);

        restartAnyJobInLastEnvironmentLaunchOnDay(daysBetweenLastJobExecutionAndComment + 1);
        assertOneMoreComment(() -> {
            // в оригинальное время чистки не получается (но может написать комментарий, если к этому моменту
            // опять прошло daysBetweenLastJobExecutionAndComment дней)
            assertDoesNotCleanupOnDay(daysBetweenLastJobExecutionAndComment + daysBetweenCommentAndCleanup);

            // а если джоба не написала комментарий, то сейчас-то точно напишет
            executeJanitorTaskOnDay(daysBetweenLastJobExecutionAndComment * 2 + 1);
        });

        assertStartsCleanupOnDay(max(daysBetweenLastJobExecutionAndComment * 2 + daysBetweenCommentAndCleanup + 1,
            daysBetweenLastJobExecutionAndComment + daysBetweenCommentAndCleanup * 2));
    }

    @Test
    public void shouldCleanAndArchive_whenThereAreNoTicketsAndEnvironmentIsDynamic() throws InterruptedException {
        createDynamicLaunchedEnvironment();
        assertDoesNothingOnDay(daysBetweenLastJobExecutionAndComment + daysBetweenCommentAndCleanup - 1);
        assertStartsCleanupAndArchivingOnDay(daysBetweenLastJobExecutionAndComment + daysBetweenCommentAndCleanup);
    }

    @Test
    public void shouldClean_whenStatusIsCleanupFailed() throws InterruptedException {
        createStaticEnvironmentWithStatusCleanupFailed();
        assertStartsCleanupOnDay(0);
    }

    @Test
    public void shouldClean_whenEnvironmentStateIsReady() throws InterruptedException {
        createDynamicLaunchedEnvironment(JanitorEventType.AFTER_READY,
            (int) TimeUnit.DAYS.toSeconds(5) - 1);
        assertDoesNothingOnDay(4);
        assertStartsCleanupAndArchivingOnDay(5);
    }

    @Test
    public void shouldClean_afterStart() throws InterruptedException {
        createDynamicLaunchedEnvironment(JanitorEventType.AFTER_START,
            (int) TimeUnit.DAYS.toSeconds(5) - 1);
        assertDoesNothingOnDay(4);
        assertStartsCleanupAndArchivingOnDay(5);
    }

    @Test
    public void shouldClean_whenBranchRemoved() throws InterruptedException {
        Mockito.when(gitHubClient.getBranch(REPOSITORY_ID, BRANCH_NAME)).thenReturn(new Branch());

        createDynamicLaunchedEnvironment(JanitorEventType.BRANCH_REMOVED, -1);
        executeJanitorTaskOnDay(0);
        assertNotCleaningAndArchiving();

        Mockito.when(gitHubClient.getBranch(REPOSITORY_ID, BRANCH_NAME)).thenReturn(null);
        executeJanitorTaskOnDay(0);
        assertCleaningAndArchiving();
    }

    @Test
    public void shouldClean_whenEnvironmentFailed() {
        createDynamicLaunchedEnvironment(JanitorEventType.ON_FAILED, -1);
        multitestingService.setEnvironmentStatus(
            MultitestingEnvironment.toId(PROJECT, NAME),
            MultitestingEnvironment.Status.DEPLOY_FAILED
        );
        assertStartsCleanupAndArchivingOnDay(0);
    }

    @Test
    public void shouldClean_whenPullRequestsFinished() throws InterruptedException {
        Mockito.when(gitHubClient.getPullRequestsWithHeadAndState(REPOSITORY_ID, BRANCH_NAME, IssueService.STATE_OPEN))
            .thenReturn(Collections.singletonList(new PullRequest()));

        createDynamicLaunchedEnvironment(JanitorEventType.PULL_REQUEST_FINISHED, -1);
        executeJanitorTaskOnDay(0);
        assertNotCleaningAndArchiving();

        Mockito.when(gitHubClient.getPullRequestsWithHeadAndState(REPOSITORY_ID, BRANCH_NAME, IssueService.STATE_OPEN))
            .thenReturn(Collections.emptyList());
        executeJanitorTaskOnDay(0);
        assertCleaningAndArchiving();
    }

    @Test
    public void shouldClean_whenAllTicketsClosed() throws InterruptedException {
        String testIssueKey = "TEST-1";
        PullRequest pullRequest = new PullRequest();
        pullRequest.setTitle(testIssueKey + " test request");
        Mockito.when(gitHubClient.getPullRequestsWithHead(REPOSITORY_ID, BRANCH_NAME))
            .thenReturn(Collections.singletonList(pullRequest));

        Mockito.when(startrekIssues.get(testIssueKey)).thenReturn(IssueBuilder.newBuilder("TEST-1")
            .setDisplay("Тестовый тикет #1")
            .setAssignee("mishunin", "Mishunin Andrei")
            .setStatus("Open")
            .build());

        createDynamicLaunchedEnvironment(JanitorEventType.TICKETS_CLOSED, -1);
        executeJanitorTaskOnDay(0);
        assertNotCleaningAndArchiving();

        Mockito.when(startrekIssues.get(testIssueKey)).thenReturn(IssueBuilder.newBuilder("TEST-1")
            .setDisplay("Тестовый тикет #1")
            .setAssignee("mishunin", "Mishunin Andrei")
            .setStatus("closed")
            .build());
        executeJanitorTaskOnDay(0);
        assertCleaningAndArchiving();
    }

    private void createStaticLaunchedEnvironment(String... tickets) throws InterruptedException {
        createStaticEnvironment();
        launchEnvironment(tickets);
        pipeTester.runScheduledJobsToCompletion();
    }

    private void createDynamicLaunchedEnvironment(String... tickets) throws InterruptedException {
        createDynamicEnvironment();
        launchEnvironment(tickets);
        pipeTester.runScheduledJobsToCompletion();
    }

    private void createStaticEnvironmentWithStatusCleanupFailed() throws InterruptedException {
        createStaticLaunchedEnvironment();
        multitestingService.setEnvironmentStatus(
            MultitestingEnvironment.toId(PROJECT, NAME),
            MultitestingEnvironment.Status.CLEANUP_FAILED
        );
    }

    private void createStaticEnvironment() {
        multitestingService.createEnvironment(
            MultitestingTestData.defaultEnvironment(PROJECT, NAME)
                .withIsStatic(true)
                .withTimeToLiveSettings(
                    settingSource == Source.MULTITESTING ? effectiveTimeToLiveSettings : null)
                .build()
        );
    }

    private void createDynamicEnvironment() {
        multitestingService.createEnvironment(
            MultitestingTestData.defaultEnvironment(PROJECT, NAME)
                .withIsStatic(false)
                .withTimeToLiveSettings(
                    settingSource == Source.MULTITESTING ? effectiveTimeToLiveSettings : null)
                .build()
        );
    }

    private void createDynamicLaunchedEnvironment(JanitorEventType janitorEventType,
                                                  Integer janitorBetweenEventAndCleanupSeconds) {
        multitestingService.createEnvironment(
            MultitestingTestData.defaultEnvironment(PROJECT, NAME)
                .withIsStatic(false)
                .withJanitorSettings(Collections.singletonList(
                    new JanitorSettingEntity(janitorEventType, janitorBetweenEventAndCleanupSeconds)
                ))
                .withRepositoryName(REPOSITORY_ID)
                .withBranchName(BRANCH_NAME)
                .withTimeToLiveSettings(
                    settingSource == Source.MULTITESTING ? effectiveTimeToLiveSettings : null)
                .build()
        );
        launchEnvironment();
        pipeTester.runScheduledJobsToCompletion();
    }

    private MultitestingEnvironment launchEnvironment(String... tickets) {
        Multimap<String, Resource> resourceMap = HashMultimap.create();
        resourceMap.put(TicketsList.class.getName(), new TicketsList(tickets));
        return multitestingService.launchMultitestingEnvironment(
            multitestingService.getEnvironment(PROJECT, NAME),
            new ResourceContainer(resourceMap),
            "some user"
        );
    }

    private String createOpenTicket() {
        return createTicket();
    }

    private String createClosedTicket(int closeDay) {
        return createTicket(createCloseEvent(closeDay));
    }

    private String createReopenedTicket(int closeDay, int reopenDay) {
        return createTicket(createCloseEvent(closeDay), createOpenEvent(reopenDay));
    }

    private String createReopenedTicketWithEventsInWrongOrder(int closeDay, int reopenDay) {
        return createTicket(createOpenEvent(reopenDay), createCloseEvent(closeDay));
    }

    private static Event createCloseEvent(int day) {
        return anEvent()
            .updatedAt(Instant.now().plus(java.time.Duration.ofDays(day)))
            .fieldChanges(
                anEventFieldChange()
                    .field(aFieldRef().id("status").build())
                    .to(aStatus().key("closed").buildMap())
                    .build()
            )
            .build();
    }

    private static Event createOpenEvent(int day) {
        return anEvent()
            .updatedAt(Instant.now().plus(java.time.Duration.ofDays(day)))
            .fieldChanges(
                anEventFieldChange()
                    .field(aFieldRef().id("status").build())
                    .to(aStatus().key("anything but closed").buildMap())
                    .build()
            )
            .build();
    }

    private String createTicket(Event... events) {
        String key = "TICKET-1";
        when(startrekIssues.get(key)).thenReturn(
            new Issue(
                null,
                null,
                key,
                null,
                0,
                Cf.<String, Object>map(),
                null
            )
        );
        when(startrekEvents.getAll(key)).thenAnswer(invocation -> Cf.arrayList(events).iterator());
        return key;
    }

    private void assertDoesNothingOnDay(int day) {
        assertSameCommentCount(() -> assertDoesNotCleanupOnDay(day));
    }

    private void assertDoesNotCleanupOnDay(int day) {
        executeJanitorTaskOnDay(day);
        assertNotCleaning();
    }

    private void assertStartsCleanupOnDay(int day) {
        assertSameCommentCount(() -> {
            executeJanitorTaskOnDay(day);
            assertCleaning();
        });
    }

    private void assertStartsCleanupAndArchivingOnDay(int day) {
        assertSameCommentCount(() -> {
            executeJanitorTaskOnDay(day);
            assertCleaningAndArchiving();
        });
    }

    private void assertWritesStartrekCommentOnDay(int day) {
        assertOneMoreComment(() -> {
            executeJanitorTaskOnDay(day);
            assertNotCleaning();
        });
    }

    private void assertSameCommentCount(Runnable runnable) {
        int oldCommentCount = startrekComments.comments.size();
        runnable.run();
        assertCommentCountIs(oldCommentCount);
    }

    private void assertOneMoreComment(Runnable runnable) {
        int oldCommentCount = startrekComments.comments.size();
        runnable.run();
        assertCommentCountIs(oldCommentCount + 1);
    }

    private void relaunchEnvironment(int day, String... tickets) throws InterruptedException {
        multitestingService.cleanup(MultitestingEnvironment.toId(PROJECT, NAME), "user");
        pipeTester.runScheduledJobsToCompletion();
        launchEnvironment(tickets);
        pipeTester.runScheduledJobsToCompletion();
        restartAnyJobInLastEnvironmentLaunchOnDay(day);  // костыль чтобы подвинуть дату последнего события
    }

    private void restartAnyJobInLastEnvironmentLaunchOnDay(int day) {
        String pipeLaunchId = multitestingService.getEnvironment(PROJECT, NAME).getLastLaunch().getPipeLaunchId();

        PipeLaunch pipeLaunch = pipeLaunchDao.getById(pipeLaunchId);
        JobState jobState = pipeLaunch.getJobs().values().iterator().next();
        JobLaunch jobLaunch = jobState.getLaunches().get(jobState.getLaunches().size() - 1);
        Date date = Date.from(Instant.now().plus(day, ChronoUnit.DAYS));
        jobLaunch.recordStatusChange(new StatusChange(StatusChangeType.SUCCESSFUL, date));

        pipeLaunchDao.save(pipeLaunch);
    }


    private void executeJanitorTaskOnDay(int day) {
        setTimeSinceFirstLaunch(day, ChronoUnit.DAYS);
        sut.execute(null);
    }

    private void setTimeSinceFirstLaunch(long n, TemporalUnit temporalUnit) {
        Clock clock = Clock.fixed(
            Instant.now().plus(n, temporalUnit),
            Clock.systemUTC().getZone()
        );
        multitestingCleanupService.setClock(clock);
        startrekComments.setClock(clock);
    }


    private void assertCleaning() {
        assertEquals(
            MultitestingEnvironment.Status.CLEANUP_TO_IDLE,
            multitestingService.getEnvironment(PROJECT, NAME).getStatus()
        );
    }

    private void assertCleaningAndArchiving() {
        assertEquals(
            MultitestingEnvironment.Status.CLEANUP_TO_ARCHIVED,
            multitestingService.getEnvironment(PROJECT, NAME).getStatus()
        );
    }

    private void assertNotCleaning() {
        assertNotEquals(
            MultitestingEnvironment.Status.CLEANUP_TO_IDLE,
            multitestingService.getEnvironment(PROJECT, NAME).getStatus()
        );
    }

    private void assertNotCleaningAndArchiving() {
        assertNotEquals(
            MultitestingEnvironment.Status.CLEANUP_TO_ARCHIVED,
            multitestingService.getEnvironment(PROJECT, NAME).getStatus()
        );
    }

    private void assertCommentCountIs(int commentCountIs) {
        assertEquals(commentCountIs, startrekComments.comments.size());
    }

    private static MultitestingTimeToLiveSettings ttlSettings(
        int daysBetweenClosingTicketsAndCleanup,
        int daysBetweenLastJobExecutionAndComment,
        int daysBetweenCommentAndCleanup) {

        return new MultitestingTimeToLiveSettings()
            .withDaysBetweenClosingTicketsAndCleanup(daysBetweenClosingTicketsAndCleanup)
            .withDaysBetweenLastJobExecutionAndComment(daysBetweenLastJobExecutionAndComment)
            .withDaysBetweenCommentAndCleanup(daysBetweenCommentAndCleanup);
    }

    private static String describeTtlSettings(MultitestingTimeToLiveSettings settings) {
        return TTL_DESCRIPTION_PROPERTY_ORDER.stream()
            .map(property -> property.apply(settings).toString())
            .collect(Collectors.joining("/"));
    }

    @Configuration
    public static class Config {
        @Bean
        public Issues startrekIssues() {
            return mock(Issues.class);
        }

        @Bean
        public MockStartrekComments startrekComments() {
            return new MockStartrekComments();
        }

        @Bean
        public Events startrekEvents() {
            return mock(Events.class);
        }

        @Bean(PIPELINE_ID)
        public Pipeline pipeline() {
            PipelineBuilder builder = PipelineBuilder.create();
            builder.withJob(DummyJob.class);
            builder.withJob(DummyJob.class).withTags(MultitestingTags.CLEANUP).withManualTrigger();
            return builder.build();
        }

        @Bean
        public ProjectsDao projectsDao() {
            return mock(ProjectsDao.class);
        }

        @Bean
        public PipelinesDao pipelinesDao() {
            PipelinesDao pipelinesDao = mock(PipelinesDao.class);
            when(pipelinesDao.exists(ArgumentMatchers.anyString())).thenReturn(true);
            return pipelinesDao;
        }
    }


    private static class MockStartrekComments implements Comments {
        private final Multimap<String, Comment> comments = HashMultimap.create();
        private Clock clock = Clock.systemUTC();

        void setClock(Clock clock) {
            this.clock = clock;
        }

        @Override
        public Comment get(String issue, long id) {
            return null;
        }

        @Override
        public Comment get(IssueRef issue, long id) {
            return null;
        }

        @Override
        public Comment get(String issue, long id, ListF<Expand> expand) {
            return null;
        }

        @Override
        public Comment get(IssueRef issue, long id, ListF<Expand> expand) {
            return null;
        }

        @Override
        public Option<Comment> getO(String issue, long id) {
            return null;
        }

        @Override
        public Option<Comment> getO(IssueRef issue, long id) {
            return null;
        }

        @Override
        public Option<Comment> getO(String issue, long id, ListF<Expand> expand) {
            return null;
        }

        @Override
        public Option<Comment> getO(IssueRef issue, long id, ListF<Expand> expand) {
            return null;
        }

        @Override
        public IteratorF<Comment> getAll(String issue) {
            return Cf.toArrayList(comments.get(issue)).iterator();
        }

        @Override
        public IteratorF<Comment> getAll(IssueRef issue) {
            return null;
        }

        @Override
        public IteratorF<Comment> getAll(String issue, ListF<Expand> expand) {
            return null;
        }

        @Override
        public IteratorF<Comment> getAll(IssueRef issue, ListF<Expand> expand) {
            return null;
        }

        @Override
        public Comment create(String issue, CommentCreate commentCreate) {
            Comment comment = aComment()
                .createdAt(Instant.now(clock))
                .withText(commentCreate.getComment().get())
                .build();
            comments.put(issue, comment);
            return comment;
        }

        @Override
        public Comment create(IssueRef issue, CommentCreate comment) {
            return null;
        }

        @Override
        public Comment create(String issue, CommentCreate comment, boolean notifyAuthor) {
            return null;
        }

        @Override
        public Comment create(String issue, CommentCreate comment, boolean notifyAuthor, boolean notify) {
            return null;
        }

        @Override
        public Comment create(IssueRef issue, CommentCreate comment, boolean notifyAuthor) {
            return null;
        }

        @Override
        public Comment create(IssueRef issue, CommentCreate comment, boolean notifyAuthor, boolean notify) {
            return null;
        }

        @Override
        public Comment update(String issue, long comment, CommentUpdate commentUpdate, boolean notifyAuthor) {
            return null;
        }

        @Override
        public Comment update(String issue, long comment, CommentUpdate commentUpdate, boolean notifyAuthor,
                              boolean notify) {
            return null;
        }

        @Override
        public Comment update(IssueRef issue, CommentRef comment, CommentUpdate commentUpdate, boolean notifyAuthor) {
            return null;
        }

        @Override
        public Comment update(IssueRef issue, CommentRef comment, CommentUpdate commentUpdate, boolean notifyAuthor,
                              boolean notify) {
            return null;
        }

        @Override
        public void delete(String issueKey, Long commentId, boolean notifyAuthor) {
        }

        @Override
        public void delete(String issueKey, Long commentId, boolean notifyAuthor, boolean notify) {
        }

        @Override
        public void delete(IssueRef issue, Comment comment, boolean notifyAuthor) {
        }

        @Override
        public void delete(IssueRef issue, Comment comment, boolean notifyAuthor, boolean notify) {
        }
    }
}
