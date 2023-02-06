package ru.yandex.market.tsum.tms.tasks.automerge;

import ru.yandex.market.tsum.clients.github.GitHubClient;
import ru.yandex.market.tsum.dao.ProjectsDao;
import ru.yandex.market.tsum.release.dao.GithubMergeSettingsDao;
import ru.yandex.market.tsum.tms.tasks.automerge.AutomergeTaskTestData.PullRequestPart;
import ru.yandex.market.tsum.tms.tasks.automerge.AutomergeTaskTestData.TestRepository;

import java.time.temporal.TemporalAmount;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static ru.yandex.market.tsum.tms.tasks.automerge.AutomergeTaskTestData.pullRequest;
import static ru.yandex.market.tsum.tms.tasks.automerge.AutomergeTaskTestData.repository;
import static ru.yandex.market.tsum.tms.tasks.automerge.AutomergeTaskTestData.testData;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 20.08.2018
 */
class AutomergeTaskTester {
    public static final String MERGE_COMMENT = "Пулл-реквест успешно смёржен. Следить за выкладкой можно на странице релизной машины:";
    private static final String TSUM_URL = "tsum.yandex-team.ru";

    private final AutomergeTaskTestData testData;

    private AutomergeTaskTester(AutomergeTaskTestData testData) {
        this.testData = testData;
    }

    @SafeVarargs
    final AutomergeTaskTester runAutomergeTaskAndCheckThatIt(Consumer<GitHubClient>... expectedActions) {
        GitHubClient mockGitHubClient = testData.createMockGitHubClient();
        GithubMergeSettingsDao mockGithubMergeSettingsDao = testData.createMockGithubMergeSettingsDao();
        ProjectsDao mockProjectsDao = testData.createMockProjectsDao();

        new GithubPullRequestAutomergeTask(
            mockGitHubClient,
            testData::getCurrentInstant,
            true,
            TSUM_URL,
            mockGithubMergeSettingsDao,
            mockProjectsDao
        )
            .execute(null);

        verify(mockGitHubClient, atLeast(1)).getOpenPullRequests(anyString());
        verify(mockGitHubClient, atLeast(1)).getPullRequest(anyString(), anyInt());
        verify(mockGitHubClient, atLeast(0)).getIssueComments(anyString(), anyInt());
        verify(mockGitHubClient, atLeast(0)).getReviews(anyString(), anyInt());
        verify(mockGitHubClient, atLeast(0)).getBranch(anyString(), any(String.class));
        verify(mockGitHubClient, atLeast(0)).getStatusChecks(anyString(), any(String.class));

        for (Consumer<GitHubClient> expectedAction : expectedActions) {
            expectedAction.accept(mockGitHubClient);
        }

        verifyNoMoreInteractions(mockGitHubClient);

        return this;
    }

    AutomergeTaskTester changePullRequest(String repositoryId, int pullRequestNumber, PullRequestPart... pullRequestParts) {
        testData.changePullRequest(repositoryId, pullRequestNumber, pullRequestParts);
        return this;
    }

    AutomergeTaskTester timePassed(TemporalAmount amount) {
        testData.timePassed(amount);
        return this;
    }


    static AutomergeTaskTester given(TestRepository... repositories) {
        return new AutomergeTaskTester(testData(repositories));
    }


    static Consumer<GitHubClient> doesNothing() {
        return mockGitHubClient -> {
        };
    }

    static Consumer<GitHubClient> attemptsToMerge(String repositoryId, int pullRequestNumber) {
        return mockGitHubClient -> verify(mockGitHubClient).mergePullRequestWithSquash(eq(repositoryId), eq((long) pullRequestNumber), any());
    }

    static Consumer<GitHubClient> mergesAndWritesMergeComment(String repositoryId, int pullRequestNumber) {
        return attemptsToMerge(repositoryId, pullRequestNumber)
            .andThen(writesComment(repositoryId, pullRequestNumber, MERGE_COMMENT));
    }

    static Consumer<GitHubClient> writesMergePreconditionsComment(String repositoryId, int pullRequestNumber) {
        return writesComment(repositoryId, pullRequestNumber, "будет смёржен автоматически когда выполнятся все условия");
    }

    static Consumer<GitHubClient> writesRequiredChecksComment(String repositoryId, int pullRequestNumber) {
        return writesComment(repositoryId, pullRequestNumber, "не настроены обязательные покоммитные проверки");
    }

    static Consumer<GitHubClient> writesConflictsComment(String repositoryId, int pullRequestNumber) {
        return writesComment(repositoryId, pullRequestNumber, "Не получилось смёржить из-за конфликтов.");
    }

    private static Consumer<GitHubClient> writesComment(String repositoryId, int pullRequestNumber, String text) {
        return mockGitHubClient -> verify(mockGitHubClient).createComment(
            eq(repositoryId),
            eq(pullRequestNumber),
            contains(text)
        );
    }


    static void shouldDoNothingWithPr(PullRequestPart... repositoryParts) {
        given(repository("some/repo", pullRequest(1, repositoryParts)))
            .runAutomergeTaskAndCheckThatIt(doesNothing());
    }

    static void shouldMergePr(PullRequestPart... repositoryParts) {
        given(repository("some/repo", pullRequest(1, repositoryParts)))
            .runAutomergeTaskAndCheckThatIt(mergesAndWritesMergeComment("some/repo", 1));
    }

    static void shouldWriteMergePreconditionsCommentToPr(PullRequestPart... repositoryParts) {
        given(repository("some/repo", pullRequest(1, repositoryParts)))
            .runAutomergeTaskAndCheckThatIt(writesMergePreconditionsComment("some/repo", 1));
    }

    static void shouldWriteRequiredChecksCommentToPr(PullRequestPart... repositoryParts) {
        given(repository("some/repo", pullRequest(1, repositoryParts)))
            .runAutomergeTaskAndCheckThatIt(writesRequiredChecksComment("some/repo", 1));
    }
}
