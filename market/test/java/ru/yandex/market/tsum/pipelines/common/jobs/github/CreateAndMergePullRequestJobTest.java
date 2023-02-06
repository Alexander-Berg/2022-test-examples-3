package ru.yandex.market.tsum.pipelines.common.jobs.github;

import java.util.Optional;

import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.PullRequestMarker;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import ru.yandex.market.request.trace.Module;
import ru.yandex.market.tsum.clients.github.GitHubClient;
import ru.yandex.market.tsum.clients.github.model.Branch;
import ru.yandex.market.tsum.clients.github.model.MergeResult;
import ru.yandex.market.tsum.clients.notifications.telegram.TelegramNotification;
import ru.yandex.market.tsum.clients.notifications.telegram.TelegramNotificationTarget;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.TestJobContext;
import ru.yandex.market.tsum.pipe.engine.runtime.notifications.Notificator;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.TaskState;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.TaskState.TaskStatus;
import ru.yandex.market.tsum.pipelines.common.jobs.github.resources.CreateAndMergePullRequestConfig;
import ru.yandex.market.tsum.pipelines.common.resources.BranchRef;
import ru.yandex.market.tsum.pipelines.common.resources.FixVersion;
import ru.yandex.market.tsum.pipelines.common.resources.GithubRepo;
import ru.yandex.market.tsum.pipelines.common.resources.ReleaseInfo;

import static java.lang.String.format;
import static java.util.Optional.of;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.common.util.StringUtils.isEmpty;
import static ru.yandex.market.tsum.pipelines.common.resources.BranchRef.MASTER;

@RunWith(JUnit4.class)
public class CreateAndMergePullRequestJobTest {

    private static final String TEST_REPO_NAME = "githubrepo";
    private static final String TEST_SOURCE_BRANCH_NAME = "githubsourcebranch";
    private static final String TEST_TARGET_BRANCH_NAME = "githubtargetbranch";
    private static final Branch TEST_SOURCE_BRANCH = new Branch(TEST_SOURCE_BRANCH_NAME);
    private static final Branch TEST_TARGET_BRANCH = new Branch(TEST_TARGET_BRANCH_NAME);
    private static final Branch TEST_MASTER_BRANCH = new Branch(MASTER.getName());
    private static final String TEST_VERSION_NAME = "2022.22.22[test_version_name]";
    private static final String EXPECTED_PR_TITLE = format("Мёрж релиза %s", TEST_VERSION_NAME);
    private static final String TEST_TICKET_KEY = "TESTQUEUE-491";
    private static final long TEST_CHAT_ID = -100500L;
    private static final TelegramNotificationTarget TEST_TELEGRAM_TARGET = new TelegramNotificationTarget(TEST_CHAT_ID);

    private static final PullRequest TEST_PULL_REQUEST = new PullRequest().setHtmlUrl("github.com/test-pr");

    static {
        PullRequestMarker base = new PullRequestMarker();
        base.setSha("asdasd");

        TEST_PULL_REQUEST.setBase(base);
    }

    private final GitHubClient gitHubClientMock = createGitHubClientMock();
    private final TestJobContext jobContext = new TestJobContext();
    private final CreateAndMergePullRequestJob job = createJobWithInjectedParameters(
        gitHubClientMock, jobContext.notifications()
    );

    {
        job.setConfig(CreateAndMergePullRequestConfig.builder().build());
    }

    @Rule
    public ExpectedException expected = ExpectedException.none();

    @Test
    public void jobCallsGitHubToAcquireBothSourceAndTargetBranches() throws Exception {
        job.execute(jobContext);
        verifyGitHub().getBranch(TEST_REPO_NAME, TEST_SOURCE_BRANCH_NAME);
        verifyGitHub().getBranch(TEST_REPO_NAME, MASTER.getName());
    }

    @Test
    public void targetBranchIsAcquiredIfSpecified() throws Exception {
        job.setConfig(withTargetBranch(TEST_TARGET_BRANCH_NAME));
        job.execute(jobContext);
        verifyGitHub().getBranch(TEST_REPO_NAME, TEST_SOURCE_BRANCH_NAME);
        verifyGitHub().getBranch(TEST_REPO_NAME, TEST_TARGET_BRANCH_NAME);
    }

    @Test
    public void jobFailsIfSourceBranchIsNotFound() throws Exception {
        testFailOnMissingBranch(TEST_SOURCE_BRANCH_NAME);
    }

    @Test
    public void jobFailsIfTargetBranchIsNotFound() throws Exception {
        testFailOnMissingBranch(MASTER.getName());
    }

    private void testFailOnMissingBranch(String missingBranchName) throws Exception {
        expected.expect(NullPointerException.class);
        expected.expectMessage(format("Branch '%s#%s' is not found!", TEST_REPO_NAME, missingBranchName));
        when(gitHubClientMock.getBranch(TEST_REPO_NAME, missingBranchName)).thenReturn(null);
        job.execute(jobContext);
    }

    @Test
    public void jobChecksIfBranchesAlreadyMerged() throws Exception {
        job.execute(jobContext);
        verifyGitHub()
            .isBranchMergedTo(TEST_REPO_NAME, TEST_SOURCE_BRANCH_NAME, MASTER.getName());
    }

    @Test
    public void targetBranchIsCheckedIfSpecified() throws Exception {
        job.setConfig(withTargetBranch(TEST_TARGET_BRANCH_NAME));
        job.execute(jobContext);
        verifyGitHub()
            .isBranchMergedTo(TEST_REPO_NAME, TEST_SOURCE_BRANCH_NAME, TEST_TARGET_BRANCH_NAME);
    }

    @Test
    public void ifBranchesAreMergedJobFinishesWithNoLink() throws Exception {
        when(gitHubClientMock.isBranchMergedTo(any(), any(), any())).thenReturn(true);
        job.execute(jobContext);
        TestJobContext.Progress lastProgress = jobContext.getLastProgress();
        assertEquals(Float.valueOf(1), lastProgress.getProgressRatio());
        assertEquals(format("Не найдено различий между ветками '%s' и '%s'",
            TEST_SOURCE_BRANCH_NAME,
            MASTER.getName()
        ), lastProgress.getStatusText());
        assertThat(lastProgress.getTaskStates(), is(empty()));
        // pull req is NOT created
        verify(gitHubClientMock, never())
            .createPullRequest(any(), any(Branch.class), any(Branch.class), any(), any());
    }

    @Test
    public void jobChecksIfOpenPrAlreadyExists() throws Exception {
        job.execute(jobContext);
        verifyGitHub()
            .getOptionalOpenPullRequest(TEST_REPO_NAME, TEST_SOURCE_BRANCH, TEST_MASTER_BRANCH);
    }

    @Test
    public void ifTargetBranchSpecifiedItUsedToSearchPr() throws Exception {
        job.setConfig(withTargetBranch(TEST_TARGET_BRANCH_NAME));
        job.execute(jobContext);
        verifyGitHub()
            .getOptionalOpenPullRequest(TEST_REPO_NAME, TEST_SOURCE_BRANCH, TEST_TARGET_BRANCH);
    }

    @Test
    public void jobCallsGitHubToCreatePr() throws Exception {
        job.execute(jobContext);
        verifyGitHub()
            .createPullRequest(TEST_REPO_NAME, TEST_SOURCE_BRANCH, TEST_MASTER_BRANCH,
                EXPECTED_PR_TITLE, createExpectedPrMessage(""));
    }

    @Test
    public void ifTargetSpecifiedItUsedToCreatePr() throws Exception {
        job.setConfig(withTargetBranch(TEST_TARGET_BRANCH_NAME));
        job.execute(jobContext);
        verifyGitHub()
            .createPullRequest(TEST_REPO_NAME, TEST_SOURCE_BRANCH, TEST_TARGET_BRANCH,
                EXPECTED_PR_TITLE, createExpectedPrMessage(""));
    }

    @Test
    public void ifTagUserSpecifiedItAddedToPrMessage() throws Exception {
        job.setConfig(CreateAndMergePullRequestConfig.builder().addTagUser("user").build());
        job.execute(jobContext);
        verifyGitHub()
            .createPullRequest(TEST_REPO_NAME, TEST_SOURCE_BRANCH, TEST_MASTER_BRANCH,
                EXPECTED_PR_TITLE, createExpectedPrMessage("@user"));
    }

    @Test
    public void ifMultipleTagUsersSpecifiedAllAddedToPrMessageInLine() throws Exception {
        job.setConfig(
            CreateAndMergePullRequestConfig.builder()
                .addTagUser("user")
                .addTagUser("cake")
                .addTagUser("spam")
                .addTagUser("eggs")
                .build()
        );
        job.execute(jobContext);
        verifyGitHub()
            .createPullRequest(TEST_REPO_NAME, TEST_SOURCE_BRANCH, TEST_MASTER_BRANCH,
                EXPECTED_PR_TITLE, createExpectedPrMessage("@user @cake @spam @eggs"));
    }

    private String createExpectedPrMessage(String tagUsersStr) {
        String tagUserFormat = isEmpty(tagUsersStr) ? "" : "%n%n%s";
        return format(
            "Тикет в Startrek: https://st.yandex-team.ru/%s%n" +
                "Автоматически создано из пайплайна: %s" + tagUserFormat,
            TEST_TICKET_KEY, jobContext.getPipeLaunchUrl(), tagUsersStr);
    }

    @Test
    public void whenPrIsCreatedTelegramNotificationIsSent() throws Exception {
        // given
        String pipeId = "test-pipe-id";
        when(jobContext.getPipeLaunch().getPipeId()).thenReturn(pipeId);
        // when
        job.execute(jobContext);
        // then
        verifyGitHub()
            .createPullRequest(any(), any(Branch.class), any(Branch.class), any(), any());
        // and also
        verify(jobContext.notifications(), times(1))
            .notifyAboutEvent(any(), anyMap());
    }

    @Test
    public void whenExistingPrIsFoundTelegramNotificationIsAlsoSent() throws Exception {
        // given
        String pipeId = "test-pipe-id";
        when(jobContext.getPipeLaunch().getPipeId()).thenReturn(pipeId);
        // and
        when(gitHubClientMock.getOptionalOpenPullRequest(any(), any(), any())).thenReturn(of(TEST_PULL_REQUEST));
        // when
        job.execute(jobContext);
        // then pull request is NOT created
        verify(gitHubClientMock, never())
            .createPullRequest(any(), any(Branch.class), any(Branch.class), any(), any());
        // and notification is sent
        verify(jobContext.notifications(), times(1))
            .notifyAboutEvent(any(), anyMap());
    }

    @Test
    public void ifMultipleTelegramTargetsSpecifiedAllAreNotified() throws Exception {
        // when
        job.execute(jobContext);
        // then
        verify(jobContext.notifications(), times(1))
            .notifyAboutEvent(any(), anyMap());
    }

    private String createExpectedTelegramNotificationMessage(String pipeId, String sourceBranchName,
                                                             String targetBranchName) {
        return format(
            "\uD83D\uDEE0️ *Пайплайн %s мёржит бранч `%s` в `%s`*\n" +
                "Релиз: `%s`, [Тикет в Startrek](https://st.yandex-team.ru/%s)\n" +
                "\n" +
                "[Пулл реквест](%s)\n" +
                "[Перейти к пайплайну](%s)\n" +
                "[Перейти к пайплайн задаче](%s)\n" +
                "\n" +
                "#%s #%s \n" +
                "--------\n",
            pipeId, sourceBranchName, targetBranchName, TEST_VERSION_NAME, TEST_TICKET_KEY, TEST_PULL_REQUEST
                .getHtmlUrl(),
            jobContext.getPipeLaunchUrl(), jobContext.getJobLaunchDetailsUrl(), pipeId, targetBranchName);
    }

    @Test
    public void jobCallsGitHubToMergeCreatedPrByDefault() throws Exception {
        job.execute(jobContext);
        String expectedMergeCommitMessage = createExpectedMergeMessage(TEST_SOURCE_BRANCH_NAME, MASTER.getName());
        verifyGitHub().mergePullRequest(TEST_REPO_NAME, TEST_PULL_REQUEST, expectedMergeCommitMessage);
    }

    @Test
    public void ifTargetBranchSpecifiedItUsedInMergeMessage() throws Exception {
        job.setConfig(withTargetBranch(TEST_TARGET_BRANCH_NAME));
        job.execute(jobContext);
        String expectedMergeCommitMessage = createExpectedMergeMessage(
            TEST_SOURCE_BRANCH_NAME, TEST_TARGET_BRANCH_NAME
        );
        verifyGitHub().mergePullRequest(TEST_REPO_NAME, TEST_PULL_REQUEST, expectedMergeCommitMessage);
    }

    private CreateAndMergePullRequestConfig withTargetBranch(String testTargetBranchName) {
        return CreateAndMergePullRequestConfig.builder()
            .withTargetBranch(new BranchRef(testTargetBranchName))
            .build();
    }

    @Test
    public void ifSourceBranchSpecifiedItUsedInMergeMessage() throws Exception {
        job.setSourceBranch(new BranchRef(TEST_TARGET_BRANCH_NAME));
        job.execute(jobContext);
        String expectedMergeCommitMessage = createExpectedMergeMessage(TEST_TARGET_BRANCH_NAME, MASTER.getName());
        verifyGitHub().mergePullRequest(TEST_REPO_NAME, TEST_PULL_REQUEST, expectedMergeCommitMessage);
    }

    private String createExpectedMergeMessage(String sourceBranchName, String targetBranchName) {
        return format("Merge branch '%s' into '%s'", sourceBranchName, targetBranchName);
    }

    @Test
    public void ifExistingPrFoundMergeIsStillPerformedByDefault() throws Exception {
        PullRequest pr = new PullRequest().setHtmlUrl("qweqwe").setBase(new PullRequestMarker().setSha("1"));
        when(gitHubClientMock.getOptionalOpenPullRequest(any(), any(), any())).thenReturn(of(pr));
        job.execute(jobContext);
        verifyGitHub().mergePullRequest(eq(TEST_REPO_NAME), same(pr), anyString());
    }

    private GitHubClient verifyGitHub() {
        return verify(gitHubClientMock, times(1));
    }

    @Test
    public void jobNeverCallsGitHubToMergeCreatedPrIfNotRequired() throws Exception {
        job.setConfig(CreateAndMergePullRequestConfig.builder().withAutoMergeRequest(false).build());
        job.execute(jobContext);
        verify(gitHubClientMock, never()).mergePullRequest(any(), any(), any());
    }

    @Test
    public void ifExistingPrFoundJobAlsoNeverCallsGitHubToMergeIfNotRequired() throws Exception {
        when(gitHubClientMock.getOptionalOpenPullRequest(any(), any(), any())).thenReturn(of(TEST_PULL_REQUEST));
        job.setConfig(CreateAndMergePullRequestConfig.builder().withAutoMergeRequest(false).build());
        job.execute(jobContext);
        verify(gitHubClientMock, never()).mergePullRequest(any(), any(), any());
    }

    @Test
    public void jobFinishesWithLinkToCreatedPr() throws Exception {
        job.execute(jobContext);
        TestJobContext.Progress lastProgress = jobContext.getLastProgress();
        assertEquals(Float.valueOf(1), lastProgress.getProgressRatio());
        assertEquals("Смёржено", lastProgress.getStatusText());
        assertSingleTaskState(lastProgress, TEST_PULL_REQUEST);
    }

    @Test
    public void ifExistingPrFoundJobFinishesWithLinkToIt() throws Exception {
        PullRequest pr = new PullRequest().setHtmlUrl("zzz").setBase(new PullRequestMarker().setSha("1"));
        when(gitHubClientMock.getOptionalOpenPullRequest(any(), any(), any())).thenReturn(of(pr));
        job.execute(jobContext);
        TestJobContext.Progress lastProgress = jobContext.getLastProgress();
        assertEquals(Float.valueOf(1), lastProgress.getProgressRatio());
        assertEquals("Смёржено", lastProgress.getStatusText());
        assertSingleTaskState(lastProgress, pr);
    }

    @Test
    public void ifPrIsEmptyJobFinishesWithLink() throws Exception {
        when(gitHubClientMock.mergePullRequest(any(), any(), any())).thenReturn(MergeResult.NOTHING_TO_MERGE);
        job.execute(jobContext);
        TestJobContext.Progress lastProgress = jobContext.getLastProgress();
        assertEquals(Float.valueOf(1), lastProgress.getProgressRatio());
        assertEquals("Нечего мёржить", lastProgress.getStatusText());
        assertSingleTaskState(lastProgress, TEST_PULL_REQUEST);
    }

    @Test
    public void ifPrHasConflictsJobFailsWithLink() throws Exception {
        when(gitHubClientMock.mergePullRequest(any(), any(), any())).thenReturn(MergeResult.CONFLICT);
        job.execute(jobContext);
        TestJobContext.Progress lastProgress = jobContext.getLastProgress();
        assertEquals(Float.valueOf(1), lastProgress.getProgressRatio());
        assertEquals("Конфликты", lastProgress.getStatusText());
        assertSingleTaskState(lastProgress, TEST_PULL_REQUEST, TaskStatus.FAILED);
    }

    @Test
    public void ifMergeDisabledJobFinishesWithLinkToCreatedPr() throws Exception {
        job.setConfig(CreateAndMergePullRequestConfig.builder().withAutoMergeRequest(false).build());
        job.execute(jobContext);
        TestJobContext.Progress lastProgress = jobContext.getLastProgress();
        assertEquals(Float.valueOf(1), lastProgress.getProgressRatio());
        assertEquals("Создан новый пулл-реквест", lastProgress.getStatusText());
        assertSingleTaskState(lastProgress, TEST_PULL_REQUEST);
    }

    @Test
    public void ifMergeDisabledAndExistingPrFoundJobFinishesWithLinkToIt() throws Exception {
        // given
        job.setConfig(CreateAndMergePullRequestConfig.builder().withAutoMergeRequest(false).build());
        // and
        PullRequest pr = new PullRequest().setHtmlUrl("asdfrewq");
        when(gitHubClientMock.getOptionalOpenPullRequest(any(), any(), any())).thenReturn(of(pr));
        // when
        job.execute(jobContext);
        // then
        TestJobContext.Progress lastProgress = jobContext.getLastProgress();
        assertEquals(Float.valueOf(1), lastProgress.getProgressRatio());
        assertEquals("Найден существующий пулл-реквест", lastProgress.getStatusText());
        assertSingleTaskState(lastProgress, pr);
    }

    @Test
    public void shouldCreateNotificationWithBaseBranchHash() throws Exception {
        String sha = "deadbeaf";

        job.setConfig(CreateAndMergePullRequestConfig.builder().withPrintHashBeforeMerge(true).build());

        PullRequestMarker marker = new PullRequestMarker();
        marker.setSha(sha);

        PullRequest pr = new PullRequest().setBase(marker);
        // when
        TelegramNotification notification = job.createTelegramNotification(jobContext, pr);
        //

        String message = notification.getTelegramMessage();

        System.out.println(message);

        Assert.assertThat(message, containsString(sha));
    }

    @Test
    public void shouldWriteCustomTags() throws Exception {
        String customTag = "hotfix";

        job.setConfig(CreateAndMergePullRequestConfig.builder().addNotificationCustomTag(customTag).build());

        PullRequestMarker marker = new PullRequestMarker();

        PullRequest pr = new PullRequest().setHtmlUrl("http://example.org");
        pr.setBase(marker);

        TelegramNotification notification = job.createTelegramNotification(jobContext, pr);

        String message = notification.getTelegramMessage();

        System.out.println(message);

        Assert.assertThat(message, containsString("#" + customTag));
    }

    private void assertSingleTaskState(TestJobContext.Progress lastProgress, PullRequest pr) {
        assertSingleTaskState(lastProgress, pr, TaskStatus.SUCCESSFUL);
    }

    private void assertSingleTaskState(TestJobContext.Progress lastProgress, PullRequest pr, TaskStatus status) {
        assertThat(lastProgress.getTaskStates(), hasSize(1));
        TaskState state = lastProgress.getTaskStates().get(0);
        assertEquals(Module.GITHUB, state.getModule());
        assertEquals(status, state.getStatus());
        assertEquals(pr.getHtmlUrl(), state.getUrl());
    }

    private CreateAndMergePullRequestJob createJobWithInjectedParameters(GitHubClient gitHubClient,
                                                                         Notificator notificator) {
        CreateAndMergePullRequestJob createdJob = new CreateAndMergePullRequestJob();
        createdJob.setGitHubClient(gitHubClient);
        createdJob.setReleaseInfo(new ReleaseInfo(new FixVersion(42, TEST_VERSION_NAME), TEST_TICKET_KEY));
        createdJob.setRepo(new GithubRepo(TEST_REPO_NAME));
        createdJob.setSourceBranch(new BranchRef(TEST_SOURCE_BRANCH_NAME));
        return createdJob;
    }

    private GitHubClient createGitHubClientMock() {
        GitHubClient mock = mock(GitHubClient.class);
        when(mock.getBranch(any(), eq(TEST_SOURCE_BRANCH_NAME))).thenReturn(TEST_SOURCE_BRANCH);
        when(mock.getBranch(any(), eq(TEST_TARGET_BRANCH_NAME))).thenReturn(TEST_TARGET_BRANCH);
        when(mock.getBranch(any(), eq(MASTER.getName()))).thenReturn(TEST_MASTER_BRANCH);
        when(mock.isBranchMergedTo(anyString(), anyString(), anyString())).thenReturn(false);
        when(mock.getOptionalOpenPullRequest(anyString(), any(), any())).thenReturn(Optional.empty());
        when(mock.createPullRequest(anyString(), any(Branch.class), any(Branch.class), anyString(), anyString()))
            .thenReturn(TEST_PULL_REQUEST);
        when(mock.mergePullRequest(anyString(), any(), anyString())).thenReturn(MergeResult.MERGED);
        return mock;
    }

}
