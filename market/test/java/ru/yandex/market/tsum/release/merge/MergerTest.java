package ru.yandex.market.tsum.release.merge;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import com.google.common.eventbus.EventBus;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.Repository;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.tsum.clients.github.GitHubClient;
import ru.yandex.market.tsum.clients.github.exceptions.PullRequestAlreadyExistsException;
import ru.yandex.market.tsum.clients.github.model.Branch;
import ru.yandex.market.tsum.clients.github.model.MergeResult;
import ru.yandex.market.tsum.clients.startrek.IssueBuilder;
import ru.yandex.market.tsum.core.notify.common.NotificationCenter;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobProgressContext;
import ru.yandex.market.tsum.pipelines.test_data.TestVersionBuilder;
import ru.yandex.market.tsum.release.GitHubService;
import ru.yandex.market.tsum.release.ReleaseIssueService;
import ru.yandex.market.tsum.release.merge.exceptions.DivergedTargetMergeException;
import ru.yandex.market.tsum.release.merge.exceptions.ForkPullRequestsException;
import ru.yandex.market.tsum.release.merge.exceptions.MergeException;
import ru.yandex.market.tsum.release.merge.filters.AcceptAllPullRequestValidator;
import ru.yandex.market.tsum.release.merge.filters.PullRequestValidator;
import ru.yandex.market.tsum.release.merge.strategies.MergeStrategy;
import ru.yandex.market.tsum.release.merge.strategies.ToFirstFailedMergeStrategy;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.startrek.client.model.Version;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tsum.pipelines.test_data.TestPullRequestFactory.pullRequest;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 23.05.17
 */
@SuppressWarnings({"checkstyle:magicNumber", "checkstyle:lineLength"})
public class MergerTest {
    public static final Issue RELEASE_ISSUE = IssueBuilder.newBuilder("TEST-100500").build();

    public static final String VERSION = "2016.45 И здесь русскими буквами что-то";
    public static final String REPO_FULL_NAME = "algebraic/common";
    public static final String RELEASE_BRANCH_NAME = "release/2016.45";
    public static final String TARGET_BRANCH_NAME = "master";
    public static final String FEATURE_1_BRANCH_NAME = "feature/feature1";
    public static final String FEATURE_2_BRANCH_NAME = "feature/feature2";
    public static final String FEATURE_3_BRANCH_NAME = "feature/feature3";
    public static final String FEATURE_4_BRANCH_NAME = "feature/feature4";
    public static final String FEATURE_5_BRANCH_NAME = "feature/feature5";
    /**
     * Кастомное имя вида <релизная_версия>_<номер_релизного_тикета>.
     */
    public static final String CUSTOM_BRANCH_NAME = "2016.4.45_MBI-12345";

    public static final String RELEASE_BRANCH_NAME_KEY = "release_branch";
    public static final String TARGET_BRANCH_NAME_KEY = "target_branch";
    public static final String BACK_PULL_REQEST_KEY = "back_pull_request";

    private Version version;
    private GitHubClient gitHubClient;
    private Merger merger;
    // TODO: notifications
//    private EventBusHelper eventBusHelper;
//    private TextResourcesFactory textResourcesFactory;
    private ReleaseIssueService releaseIssueService;
    private Repository repository;
    private PullRequestService pullRequestService;
    private MergeOptions mergeOptions;
    private NotificationCenter notificationCenter;
    private JobContext jobContext;
    private GitHubService gitHubService;

    @Before
    public void setUp() throws Throwable {
        EventBus eventBus = new EventBus();
//        eventBusHelper = new EventBusHelper(eventBus);
        version = TestVersionBuilder.aVersion().withId(48471L).withName(VERSION).build();

        gitHubClient = mock(GitHubClient.class);
        when(gitHubClient.mergePullRequest(any(), any(), any(), any())).thenReturn(MergeResult.MERGED);
        when(gitHubClient.createPullRequest(any(), any(Branch.class), any(Branch.class), any(), any()))
            .thenReturn(pullRequest(RELEASE_BRANCH_NAME, TARGET_BRANCH_NAME));
        when(gitHubClient.getBranch(any(), any())).thenReturn(null);

        repository = new Repository();
        repository.setHtmlUrl("https://github.yandex-team.ru/" + REPO_FULL_NAME);
        when(gitHubClient.getRepository(REPO_FULL_NAME)).thenReturn(repository);

//        textResourcesFactory = mock(TextResourcesFactory.class);
        releaseIssueService = mock(ReleaseIssueService.class);
        when(releaseIssueService.getReleaseIssue(version)).thenReturn(RELEASE_ISSUE);

        MergeStrategy mergeStrategy = new ToFirstFailedMergeStrategy(gitHubClient);
        PullRequestValidator pullRequestValidator = new AcceptAllPullRequestValidator();
        mergeOptions = new MergeOptions(mergeStrategy, pullRequestValidator);

        TargetBranchProvider targetBranchProvider = new TargetBranchProvider(gitHubClient);
        notificationCenter = mock(NotificationCenter.class);
        jobContext = mock(JobContext.class);
        JobProgressContext jobProgressContext = mock(JobProgressContext.class);
        when(jobContext.progress()).thenReturn(jobProgressContext);
        ReleasePullRequestCreator releasePullRequestCreator = new ReleasePullRequestCreator(
            gitHubClient, releaseIssueService
        );

        gitHubService = new GitHubService(gitHubClient, pullRequestService);

        pullRequestService = mock(PullRequestService.class);

        merger = new Merger(
            gitHubClient,
            releaseIssueService,
            releasePullRequestCreator,
            targetBranchProvider,
            pullRequestService,
            gitHubService
        );
    }

    @Test
    public void createBranch() throws Throwable {
        when(pullRequestService.getPullRequests(REPO_FULL_NAME, version)).thenReturn(Arrays.asList(
            pullRequest(FEATURE_1_BRANCH_NAME, TARGET_BRANCH_NAME),
            pullRequest(FEATURE_2_BRANCH_NAME, TARGET_BRANCH_NAME)
        ));
        when(gitHubClient.getBranch(REPO_FULL_NAME, TARGET_BRANCH_NAME)).thenReturn(branch(TARGET_BRANCH_NAME));
        when(gitHubClient.createBranch(REPO_FULL_NAME, TARGET_BRANCH_NAME, RELEASE_BRANCH_NAME))
            .thenReturn(branch(RELEASE_BRANCH_NAME));

        Merger.MergeResult result = runMerge();

        assertEquals(result.getStatus(), Merger.Status.SUCCESSFUL);
        assertEquals(result.getReleaseBranch().getName(), RELEASE_BRANCH_NAME);
        verify(gitHubClient).createBranch(REPO_FULL_NAME, TARGET_BRANCH_NAME, RELEASE_BRANCH_NAME);
    }

    @Test
    public void createBranchAlreadyExists() throws Exception {
        when(gitHubClient.getBranch(REPO_FULL_NAME, RELEASE_BRANCH_NAME)).thenReturn(branch(RELEASE_BRANCH_NAME));
        when(gitHubClient.getBranch(REPO_FULL_NAME, TARGET_BRANCH_NAME)).thenReturn(branch(TARGET_BRANCH_NAME));
        when(pullRequestService.getPullRequests(REPO_FULL_NAME, version)).thenReturn(Arrays.asList(
            pullRequest(FEATURE_1_BRANCH_NAME, TARGET_BRANCH_NAME),
            pullRequest(FEATURE_2_BRANCH_NAME, TARGET_BRANCH_NAME)
        ));

        Merger.MergeResult result = runMerge();

        assertEquals(result.getStatus(), Merger.Status.SUCCESSFUL);
        assertEquals(result.getReleaseBranch().getName(), RELEASE_BRANCH_NAME);
        assertEquals(result.getTargetBranch().getName(), TARGET_BRANCH_NAME);
        verify(gitHubClient, never()).createBranch(any(), any(), any());
    }

    @Test(expected = DivergedTargetMergeException.class)
    public void createBranchDivergedBranches() throws IOException, MergeException, URISyntaxException,
        InterruptedException {
        String repositoryFullName = "algebraic/common";

        when(pullRequestService.getPullRequests(repositoryFullName, version)).thenReturn(Arrays.asList(
            pullRequest(FEATURE_1_BRANCH_NAME, TARGET_BRANCH_NAME),
            pullRequest(FEATURE_2_BRANCH_NAME, "flomaster")
        ));

        runMerge();
    }

    @Test
    public void merge() throws IOException, URISyntaxException, MergeException, InterruptedException {
        PullRequest pr1 = pullRequest(FEATURE_1_BRANCH_NAME, TARGET_BRANCH_NAME);
        PullRequest pr2 = pullRequest(FEATURE_2_BRANCH_NAME, TARGET_BRANCH_NAME);
        when(pullRequestService.getPullRequests(REPO_FULL_NAME, version)).thenReturn(Arrays.asList(pr1, pr2));

        Branch releaseBranch = branch(RELEASE_BRANCH_NAME);
        when(gitHubClient.getBranch(any(), eq(TARGET_BRANCH_NAME))).thenReturn(branch(TARGET_BRANCH_NAME));
        when(gitHubClient.createBranch(any(), any(), any())).thenReturn(releaseBranch);

        Merger.MergeResult result = runMerge();

        assertEquals(result.getStatus(), Merger.Status.SUCCESSFUL);

//        eventBusHelper.nextValidate(MergeFinishedInfo.class);
//        eventBusHelper.nextValidate(BackPullRequestCreatedEvent.class);
//        eventBusHelper.validateNoMoreEvents();
        verify(gitHubClient).mergePullRequest(eq(REPO_FULL_NAME), eq(FEATURE_1_BRANCH_NAME),
            eq(releaseBranch.getName()), any());
        verify(gitHubClient).mergePullRequest(eq(REPO_FULL_NAME), eq(FEATURE_2_BRANCH_NAME),
            eq(releaseBranch.getName()), any());
    }

    @Test
    public void mergeOneAlreadyMerged() throws URISyntaxException, MergeException, InterruptedException {
        Branch releaseBranch = branch(RELEASE_BRANCH_NAME);

        PullRequest pr1 = pullRequest(FEATURE_1_BRANCH_NAME, TARGET_BRANCH_NAME);
        PullRequest pr2 = pullRequest(FEATURE_2_BRANCH_NAME, TARGET_BRANCH_NAME);
        when(pullRequestService.getPullRequests(REPO_FULL_NAME, version)).thenReturn(Arrays.asList(pr1, pr2));
        when(gitHubClient.mergePullRequest(eq(REPO_FULL_NAME), eq(FEATURE_1_BRANCH_NAME),
            eq(releaseBranch.getName()), any())
        )
            .thenReturn(MergeResult.NOTHING_TO_MERGE);

        when(gitHubClient.getBranch(any(), eq(TARGET_BRANCH_NAME))).thenReturn(branch(TARGET_BRANCH_NAME));
        when(gitHubClient.createBranch(any(), any(), any())).thenReturn(releaseBranch);

        Merger.MergeResult result = runMerge();

        assertEquals(result.getStatus(), Merger.Status.SUCCESSFUL);

//        eventBusHelper.nextValidate(MergeFinishedInfo.class);
//        eventBusHelper.nextValidate(BackPullRequestCreatedEvent.class);
//        eventBusHelper.validateNoMoreEvents();
        verify(gitHubClient).mergePullRequest(eq(REPO_FULL_NAME), eq(FEATURE_1_BRANCH_NAME),
            eq(releaseBranch.getName()), any());
        verify(gitHubClient).mergePullRequest(eq(REPO_FULL_NAME), eq(FEATURE_2_BRANCH_NAME),
            eq(releaseBranch.getName()), any());
    }

    @Test
    public void mergeAllAlreadyMerged() throws IOException, URISyntaxException, MergeException, InterruptedException {
        Branch releaseBranch = branch(RELEASE_BRANCH_NAME);
        Branch targetBranch = branch(TARGET_BRANCH_NAME);

        PullRequest pr1 = pullRequest(FEATURE_1_BRANCH_NAME, TARGET_BRANCH_NAME);
        PullRequest pr2 = pullRequest(FEATURE_2_BRANCH_NAME, TARGET_BRANCH_NAME);
        when(pullRequestService.getPullRequests(REPO_FULL_NAME, version)).thenReturn(Arrays.asList(pr1, pr2));
        when(gitHubClient.mergePullRequest(eq(REPO_FULL_NAME),
            eq(FEATURE_1_BRANCH_NAME), eq(releaseBranch.getName()), any())
        )
            .thenReturn(MergeResult.NOTHING_TO_MERGE);
        when(gitHubClient.mergePullRequest(eq(REPO_FULL_NAME),
            eq(FEATURE_2_BRANCH_NAME), eq(releaseBranch.getName()), any())
        )
            .thenReturn(MergeResult.NOTHING_TO_MERGE);

        when(gitHubClient.getBranch(any(), eq(TARGET_BRANCH_NAME))).thenReturn(targetBranch);
        when(gitHubClient.createBranch(any(), eq(TARGET_BRANCH_NAME), any())).thenReturn(releaseBranch);

        Merger.MergeResult result = runMerge();

        assertEquals(result.getStatus(), Merger.Status.NO_CHANGES);

        verify(gitHubClient).mergePullRequest(eq(REPO_FULL_NAME), eq(FEATURE_1_BRANCH_NAME),
            eq(releaseBranch.getName()), any());
        verify(gitHubClient).mergePullRequest(eq(REPO_FULL_NAME), eq(FEATURE_2_BRANCH_NAME),
            eq(releaseBranch.getName()), any());
    }

    @Test
    public void mergeNoChanges() throws IOException, URISyntaxException, MergeException, InterruptedException {
        when(pullRequestService.getPullRequests(REPO_FULL_NAME, version)).thenReturn(new ArrayList<>());

        Merger.MergeResult result = runMerge();

        assertEquals(result.getStatus(), Merger.Status.NO_PULL_REQUESTS);
        assertNull(result.getReleaseBranch());
        assertNull(result.getTargetBranch());
        assertNull(result.getReleasePullRequest());

        verify(gitHubClient, never()).mergePullRequest(any(), any(), any(), any());
        verifyNoMoreInteractions(releaseIssueService);
    }

    @Test
    public void mergeConflict() throws Exception {
        Branch releaseBranch = branch(RELEASE_BRANCH_NAME);

        PullRequest pr1 = pullRequest(FEATURE_1_BRANCH_NAME, TARGET_BRANCH_NAME);
        PullRequest pr2 = pullRequest(FEATURE_2_BRANCH_NAME, TARGET_BRANCH_NAME);
        when(pullRequestService.getPullRequests(REPO_FULL_NAME, version)).thenReturn(Arrays.asList(pr1, pr2));
        when(gitHubClient.mergePullRequest(eq(REPO_FULL_NAME), eq(FEATURE_2_BRANCH_NAME),
            eq(releaseBranch.getName()), any())
        )
            .thenReturn(MergeResult.CONFLICT);
        when(gitHubClient.createBranch(any(), any(), any())).thenReturn(releaseBranch);

        when(gitHubClient.getBranch(REPO_FULL_NAME, TARGET_BRANCH_NAME)).thenReturn(branch(TARGET_BRANCH_NAME));
        when(gitHubClient.getBranch(REPO_FULL_NAME, FEATURE_2_BRANCH_NAME)).thenReturn(branch(FEATURE_2_BRANCH_NAME));

        Merger.MergeResult result = runMerge();

        assertEquals(result.getStatus(), Merger.Status.FAILED);

//        eventBusHelper.nextValidate(MergeFinishedInfo.class);
//        eventBusHelper.nextValidate(BackPullRequestCreatedEvent.class);
//        eventBusHelper.validateNoMoreEvents();
    }

    @Test
    // https://st.yandex-team.ru/MARKETINFRA-186
    public void createPullRequestToTargetBranchEventIfNothingToMerge() throws Exception {
        Branch targetBranch = branch(TARGET_BRANCH_NAME);
        Branch releaseBranch = branch(RELEASE_BRANCH_NAME);

        PullRequest pullRequest = pullRequest(FEATURE_1_BRANCH_NAME, TARGET_BRANCH_NAME);
        when(pullRequestService.getPullRequests(REPO_FULL_NAME, version)).thenReturn(Arrays.asList(pullRequest));
        when(gitHubClient.mergePullRequest(eq(REPO_FULL_NAME), eq(FEATURE_1_BRANCH_NAME), eq(RELEASE_BRANCH_NAME),
            any()))
            .thenReturn(MergeResult.NOTHING_TO_MERGE);

        when(gitHubClient.getBranch(REPO_FULL_NAME, TARGET_BRANCH_NAME)).thenReturn(targetBranch);
        when(gitHubClient.createBranch(REPO_FULL_NAME, TARGET_BRANCH_NAME, RELEASE_BRANCH_NAME)).thenReturn(releaseBranch);

        Merger.MergeResult mergerResult = runMerge();

        Assert.assertNotNull(mergerResult.getReleasePullRequest());

        verify(gitHubClient).createPullRequest(
            eq(REPO_FULL_NAME),
            eq(releaseBranch),
            eq(targetBranch),
            any(),
            any()
        );
    }

    @Test
    public void createAlreadyExistingPullRequestToTargetBranch() throws Exception {
        when(pullRequestService.getPullRequests(REPO_FULL_NAME, version)).thenReturn(Arrays.asList(
            pullRequest(FEATURE_1_BRANCH_NAME, TARGET_BRANCH_NAME)
        ));
        when(gitHubClient.getBranch(REPO_FULL_NAME, TARGET_BRANCH_NAME)).thenReturn(branch(TARGET_BRANCH_NAME));
        when(gitHubClient.createBranch(REPO_FULL_NAME, TARGET_BRANCH_NAME, RELEASE_BRANCH_NAME))
            .thenReturn(branch(RELEASE_BRANCH_NAME));

        when(gitHubClient.createPullRequest(any(), any(Branch.class), any(Branch.class), any(), any()))
            .thenThrow(new PullRequestAlreadyExistsException());

        when(gitHubClient.getOpenPullRequest(any(), any(), any()))
            .thenReturn(pullRequest(TARGET_BRANCH_NAME, RELEASE_BRANCH_NAME));

        Merger.MergeResult result = runMerge();

        assertEquals(result.getStatus(), Merger.Status.SUCCESSFUL);

//        eventBusHelper.nextValidate(MergeFinishedInfo.class);
//        eventBusHelper.validateNoMoreEvents();
    }

    @Test
    public void reportAboutCreatedPullRequestToReleaseBranch() throws Exception {
        String targetBranchName = TARGET_BRANCH_NAME;
        PullRequest backPullRequest = pullRequest(RELEASE_BRANCH_NAME, targetBranchName);

        when(pullRequestService.getPullRequests(REPO_FULL_NAME, version)).thenReturn(Arrays.asList(
            pullRequest(FEATURE_1_BRANCH_NAME, targetBranchName)
        ));
        when(gitHubClient.createBranch(REPO_FULL_NAME, targetBranchName, RELEASE_BRANCH_NAME)).thenReturn(branch(RELEASE_BRANCH_NAME));
        when(gitHubClient.createPullRequest(any(), any(Branch.class), any(Branch.class), any(), any()))
            .thenReturn(backPullRequest);

        when(gitHubClient.getBranch(REPO_FULL_NAME, RELEASE_BRANCH_NAME)).thenReturn(branch(RELEASE_BRANCH_NAME));
        when(gitHubClient.getBranch(REPO_FULL_NAME, targetBranchName)).thenReturn(branch(targetBranchName));

        Merger.MergeResult result = runMerge();

        assertEquals(result.getStatus(), Merger.Status.SUCCESSFUL);
//        eventBusHelper.nextValidate(MergeFinishedInfo.class);
//        eventBusHelper.nextValidate(BackPullRequestCreatedEvent.class);
//        eventBusHelper.validateNoMoreEvents();
    }

    /**
     * Проверяет корректность создания ПРа с кастомной веткой вида release/<релизная_версия>_<номер_релизного_тикета>.
     * https://st.yandex-team.ru/MARKETINFRA-1630
     */
    @Test
    public void createPullRequestWithCustomBranchName() throws InterruptedException {

        String targetBranchName = TARGET_BRANCH_NAME;

        when(pullRequestService.getPullRequests(REPO_FULL_NAME, version)).thenReturn(Collections.singletonList(
            pullRequest(FEATURE_1_BRANCH_NAME, targetBranchName)
        ));

        String releaseBranchName = "release/" + CUSTOM_BRANCH_NAME;
        when(gitHubClient.createBranch(REPO_FULL_NAME, targetBranchName, releaseBranchName))
            .thenReturn(branch(releaseBranchName));

        when(gitHubClient.getBranch(REPO_FULL_NAME, targetBranchName)).thenReturn(branch(targetBranchName));

        Merger.MergeResult result = merger.merge(
            jobContext, REPO_FULL_NAME, version, mergeOptions,
            event -> {
            },
            event -> {
            },
            CUSTOM_BRANCH_NAME
        );

        assertEquals(result.getStatus(), Merger.Status.SUCCESSFUL);
        assertEquals(result.getReleaseBranch().getName(), releaseBranchName);
    }

    @Test(expected = ForkPullRequestsException.class)
    public void forkPullRequests() throws InterruptedException {
        // different repos
        int sourceRepoId = 2;
        int targetRepoId = 1;

        PullRequest forkPR = pullRequest(
            sourceRepoId,
            TARGET_BRANCH_NAME,
            targetRepoId,
            TARGET_BRANCH_NAME
        );

        when(pullRequestService.getPullRequests(REPO_FULL_NAME, version)).thenReturn(Arrays.asList(forkPR));

        runMerge();
    }

    private Merger.MergeResult runMerge() throws InterruptedException {
        return merger.merge(
            jobContext, REPO_FULL_NAME, version, mergeOptions,
            event -> {
            },
            event -> {
            },
            null
        );
    }

    public static Branch branch(String name) {
        Branch branch = new Branch();
        branch.setName(name);
        branch.setHtmlLink("https://github.yandex-team.ru/market-infra/market-ci/tree/MARKETINFRA-19");
        return branch;
    }
}
