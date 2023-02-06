package ru.yandex.market.tsum.tms.tasks.automerge;

import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.CommitStatus;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.PullRequestMarker;
import ru.yandex.market.tsum.clients.github.GitHubClient;
import ru.yandex.market.tsum.clients.github.model.Branch;
import ru.yandex.market.tsum.clients.github.model.MergeResult;
import ru.yandex.market.tsum.clients.github.model.Review;
import ru.yandex.market.tsum.core.StoredObject;
import ru.yandex.market.tsum.dao.ProjectsDao;
import ru.yandex.market.tsum.entity.project.DeliveryMachineEntity;
import ru.yandex.market.tsum.entity.project.ProjectEntity;
import ru.yandex.market.tsum.release.dao.GitHubSettings;
import ru.yandex.market.tsum.release.dao.GithubMergeSettings;
import ru.yandex.market.tsum.release.dao.GithubMergeSettingsDao;
import ru.yandex.market.tsum.release.dao.VcsSettings;

import java.time.Instant;
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tsum.tms.tasks.automerge.AutomergeTaskTester.MERGE_COMMENT;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 22.08.2018
 */
class AutomergeTaskTestData {
    private static final String TARGET_BRANCH = "targetBranch";
    private static final String LAST_COMMIT_HASH = "lastCommitHash";

    private final TestRepository[] repositories;
    private Instant currentInstant = Instant.now();

    private AutomergeTaskTestData(TestRepository... repositories) {
        this.repositories = repositories;
    }

    static AutomergeTaskTestData testData(TestRepository... repositories) {
        return new AutomergeTaskTestData(repositories);
    }

    void changePullRequest(String repositoryId, int pullRequestNumber, PullRequestPart... pullRequestParts) {
        for (TestRepository repository : repositories) {
            if (repository.repositoryId.equals(repositoryId)) {
                for (TestPullRequest pullRequest : repository.pullRequests.values()) {
                    if (pullRequest.pullRequestNumber == pullRequestNumber) {
                        for (PullRequestPart pullRequestPart : pullRequestParts) {
                            pullRequestPart.addTo(repository, pullRequest);
                        }
                    }
                }
            }
        }
    }

    void timePassed(TemporalAmount amount) {
        currentInstant = currentInstant.plus(amount);
    }

    GitHubClient createMockGitHubClient() {
        GitHubClient mockGitHubClient = mock(GitHubClient.class);
        for (TestRepository repository : repositories) {
            when(mockGitHubClient.getOpenPullRequests(repository.repositoryId)).
                thenAnswer(invocation ->
                    repository.pullRequests.values().stream()
                        .map(pullRequest -> pullRequestBean(pullRequest.pullRequestNumber, pullRequest.description))
                        .collect(Collectors.toList())
                );
            for (TestPullRequest pullRequest : repository.pullRequests.values()) {
                when(mockGitHubClient.getPullRequest(repository.repositoryId, pullRequest.pullRequestNumber))
                    .thenReturn(pullRequestBean(pullRequest.pullRequestNumber, pullRequest.description));
                when(mockGitHubClient.getBranch(repository.repositoryId, TARGET_BRANCH)).thenReturn(branchBean(repository.requiredChecks));
                when(mockGitHubClient.getReviews(repository.repositoryId, pullRequest.pullRequestNumber)).thenReturn(pullRequest.reviews);
                when(mockGitHubClient.getIssueComments(repository.repositoryId, pullRequest.pullRequestNumber)).thenReturn(pullRequest.comments);
                when(mockGitHubClient.getStatusChecks(repository.repositoryId, LAST_COMMIT_HASH)).thenReturn(pullRequest.checks);
                doAnswer(invocation -> {
                    Comment comment = new Comment();
                    comment.setCreatedAt(Date.from(currentInstant));
                    comment.setBody(invocation.getArgument(2).toString());
                    pullRequest.comments.add(comment);
                    return null;
                })
                    .when(mockGitHubClient).createComment(
                    eq(repository.repositoryId),
                    eq(pullRequest.pullRequestNumber),
                    anyString()
                );
                when(
                    mockGitHubClient.mergePullRequestWithSquash(
                        eq(repository.repositoryId),
                        eq((long) pullRequest.pullRequestNumber),
                        anyString()
                    )
                )
                    .thenReturn(pullRequest.mergeResult);
            }
        }
        return mockGitHubClient;
    }

    ProjectsDao createMockProjectsDao() {
        ProjectsDao mockProjectsDao = mock(ProjectsDao.class);

        for (TestRepository repository : repositories) {
            when(mockProjectsDao.findProjectsByRepositoryId(repository.repositoryId, true))
                .thenAnswer((invocation) -> repository.projects);
        }

        return mockProjectsDao;
    }

    GithubMergeSettingsDao createMockGithubMergeSettingsDao() {
        GithubMergeSettingsDao githubMergeSettingsDao = mock(GithubMergeSettingsDao.class);

        when(githubMergeSettingsDao.list())
            .thenAnswer((invocation -> {
                List<GithubMergeSettings> result = new ArrayList<>();
                for(TestRepository repository : repositories) {
                    if (repository.mergeSettings != null) {
                        result.add(repository.mergeSettings);
                    }
                }
                return result;
            }));

        return githubMergeSettingsDao;
    }

    public Stream<TestRepository> getRepositoriesStream() {
        return Arrays.stream(repositories);
    }

    Instant getCurrentInstant() {
        return currentInstant;
    }

    private static Branch branchBean(List<String> contexts) {
        Branch.Protection.RequiredStatusChecks requiredStatusChecks = new Branch.Protection.RequiredStatusChecks();
        requiredStatusChecks.setContexts(contexts);

        Branch.Protection protection = new Branch.Protection();
        protection.setRequiredStatusChecks(requiredStatusChecks);

        Branch branch = new Branch();
        branch.setProtection(protection);

        return branch;
    }

    static TestRepository repository(String repositoryId, RepositoryPart... repositoryParts) {
        TestRepository repository = new TestRepository(repositoryId);

        repository.mergeSettings = new GithubMergeSettings(
            repositoryId,
            true,
            false
        );

        List<DeliveryMachineEntity> deliveryMachines = new ArrayList<>();

        DeliveryMachineEntity deliveryMachine = new DeliveryMachineEntity();
        deliveryMachine.setStageGroupId("some stage group id");
        deliveryMachine.setVcsSettings(new StoredObject<>(
            GitHubSettings.class.toString(),
            new GitHubSettings(repositoryId),
            null
        ));

        deliveryMachines.add(deliveryMachine);

        repository.projects.add(new ProjectEntity(
            "some project id",
            "some project title",
            deliveryMachines
        ));

        for (RepositoryPart repositoryPart : repositoryParts) {
            repositoryPart.addTo(repository);
        }

        return repository;
    }

    static RepositoryPart pullRequest(int pullRequestNumber, PullRequestPart... pullRequestParts) {
        return repository -> {
            TestPullRequest pullRequest = new TestPullRequest(pullRequestNumber);
            repository.pullRequests.put(pullRequestNumber, pullRequest);
            for (PullRequestPart pullRequestPart : pullRequestParts) {
                pullRequestPart.addTo(repository, pullRequest);
            }
        };
    }


    static PullRequestPart descriptionWithMergeCommand() {
        return description("/merge");
    }

    static PullRequestPart descriptionWithoutMergeCommand() {
        return description("descriptionWithoutMergeCommand");
    }

    static PullRequestPart description(String text) {
        return (repository, pullRequest) -> pullRequest.description = text;
    }


    static PullRequestPart missingRequiredCheck() {
        return missingRequiredCheck("missingRequiredCheck");
    }

    static PullRequestPart missingRequiredCheck(String context) {
        return (repository, pullRequest) -> repository.requiredChecks.add(context);
    }


    static PullRequestPart successfulRequiredCheck() {
        return successfulRequiredCheck("successfulRequiredCheck", Instant.now());
    }

    static PullRequestPart successfulRequiredCheck(String context, Instant instant) {
        return (repository, pullRequest) -> {
            missingRequiredCheck(context).addTo(repository, pullRequest);
            successfulOptionalCheck(context, instant).addTo(repository, pullRequest);
        };
    }

    static PullRequestPart failedRequiredCheck() {
        return failedRequiredCheck("failedRequiredCheck", Instant.now());
    }

    static PullRequestPart failedRequiredCheck(String context, Instant instant) {
        return (repository, pullRequest) -> {
            missingRequiredCheck(context).addTo(repository, pullRequest);
            failedOptionalCheck(context, instant).addTo(repository, pullRequest);
        };
    }


    static PullRequestPart successfulOptionalCheck(String context, Instant instant) {
        return optionalCheck(context, CommitStatus.STATE_SUCCESS, instant);
    }

    static PullRequestPart failedOptionalCheck() {
        return failedOptionalCheck("failedOptionalCheck", Instant.now());
    }

    static PullRequestPart failedOptionalCheck(String context, Instant instant) {
        return optionalCheck(context, CommitStatus.STATE_FAILURE, instant);
    }

    static PullRequestPart optionalCheck(String context, String state, Instant instant) {
        return (repository, pullRequest) -> {
            CommitStatus check = new CommitStatus();
            check.setContext(context);
            check.setState(state);
            check.setUpdatedAt(Date.from(instant));
            pullRequest.checks.add(check);
        };
    }


    static PullRequestPart approvedReview() {
        return approvedReview(null);
    }

    static PullRequestPart approvedReviewWithMergeCommand() {
        return approvedReview("/merge");
    }

    static PullRequestPart approvedReview(String text) {
        return review(Review.State.APPROVED, text);
    }

    static PullRequestPart changesRequestedReview() {
        return changesRequestedReview(null);
    }

    static PullRequestPart changesRequestedReviewWithMergeCommand() {
        return changesRequestedReview("/merge");
    }

    static PullRequestPart changesRequestedReview(String text) {
        return review(Review.State.CHANGES_REQUESTED, text);
    }

    static PullRequestPart commentedReview() {
        return commentedReview(null);
    }

    static PullRequestPart commentedReviewWithMergeCommand() {
        return commentedReview("/merge");
    }

    static PullRequestPart commentedReview(String text) {
        return review(Review.State.COMMENTED, text);
    }

    static PullRequestPart dismissedReview() {
        return dismissedReview(null);
    }

    static PullRequestPart dismissedReviewWithMergeCommand() {
        return dismissedReview("/merge");
    }

    static PullRequestPart dismissedReview(String text) {
        return review(Review.State.DISMISSED, text);
    }

    static PullRequestPart review(Review.State state, String text) {
        return (repository, pullRequest) -> {
            Review review = new Review();
            review.setState(state);
            review.setMessage(text);
            pullRequest.reviews.add(review);
        };
    }


    static PullRequestPart commentWithMergeCommand() {
        return comment("/merge");
    }

    static PullRequestPart comment(String text) {
        return (repository, pullRequest) -> {
            Comment comment = new Comment();
            comment.setBody(text);
            pullRequest.comments.add(comment);
        };
    }


    static PullRequestPart conflicts() {
        return (repository, pullRequest) -> pullRequest.mergeResult = MergeResult.CONFLICT;
    }


    private static PullRequest pullRequestBean(int number, String description) {
        PullRequestMarker base = new PullRequestMarker();
        base.setRef(TARGET_BRANCH);

        PullRequestMarker head = new PullRequestMarker();
        head.setSha(LAST_COMMIT_HASH);

        PullRequest pullRequest = new PullRequest();
        pullRequest.setNumber(number);
        pullRequest.setBody(description);
        pullRequest.setBase(base);
        pullRequest.setHead(head);
        return pullRequest;
    }


    static class TestRepository {
        final String repositoryId;
        final List<String> requiredChecks = new ArrayList<>();
        final Map<Integer, TestPullRequest> pullRequests = new HashMap<>();
        final List<ProjectEntity> projects = new ArrayList<>();
        GithubMergeSettings mergeSettings;
        boolean silentMode = false;

        TestRepository(String repositoryId) {
            this.repositoryId = repositoryId;
        }

        public TestRepository setSilentMode(boolean silentMode) {
            this.silentMode = silentMode;

            if (mergeSettings != null) {
                mergeSettings.setSilentMode(silentMode);
            }

            return this;
        }

        public TestRepository setMergeSettings(GithubMergeSettings settings) {
            this.mergeSettings = settings;
            return this;
        }
    }

    static class TestPullRequest {
        final int pullRequestNumber;
        String description;
        final String targetBranch = "defaultBranch";
        final String lastCommitHash = "defaultCommitHash";
        final List<CommitStatus> checks = new ArrayList<>();
        final List<Comment> comments = new ArrayList<>();
        final List<Review> reviews = new ArrayList<>();
        MergeResult mergeResult = MergeResult.MERGED;

        TestPullRequest(int pullRequestNumber) {
            this.pullRequestNumber = pullRequestNumber;
        }
    }

    @FunctionalInterface
    interface RepositoryPart {
        void addTo(TestRepository repository);
    }

    @FunctionalInterface
    interface PullRequestPart {
        void addTo(TestRepository repository, TestPullRequest pullRequest);
    }
}
