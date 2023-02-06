package ru.yandex.market.tsum.tms.tasks.automerge;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.Instant;
import java.time.Period;

import static ru.yandex.market.tsum.tms.tasks.automerge.AutomergeTaskTestData.approvedReview;
import static ru.yandex.market.tsum.tms.tasks.automerge.AutomergeTaskTestData.approvedReviewWithMergeCommand;
import static ru.yandex.market.tsum.tms.tasks.automerge.AutomergeTaskTestData.changesRequestedReview;
import static ru.yandex.market.tsum.tms.tasks.automerge.AutomergeTaskTestData.changesRequestedReviewWithMergeCommand;
import static ru.yandex.market.tsum.tms.tasks.automerge.AutomergeTaskTestData.comment;
import static ru.yandex.market.tsum.tms.tasks.automerge.AutomergeTaskTestData.commentWithMergeCommand;
import static ru.yandex.market.tsum.tms.tasks.automerge.AutomergeTaskTestData.commentedReview;
import static ru.yandex.market.tsum.tms.tasks.automerge.AutomergeTaskTestData.commentedReviewWithMergeCommand;
import static ru.yandex.market.tsum.tms.tasks.automerge.AutomergeTaskTestData.conflicts;
import static ru.yandex.market.tsum.tms.tasks.automerge.AutomergeTaskTestData.descriptionWithMergeCommand;
import static ru.yandex.market.tsum.tms.tasks.automerge.AutomergeTaskTestData.descriptionWithoutMergeCommand;
import static ru.yandex.market.tsum.tms.tasks.automerge.AutomergeTaskTestData.dismissedReview;
import static ru.yandex.market.tsum.tms.tasks.automerge.AutomergeTaskTestData.dismissedReviewWithMergeCommand;
import static ru.yandex.market.tsum.tms.tasks.automerge.AutomergeTaskTestData.failedOptionalCheck;
import static ru.yandex.market.tsum.tms.tasks.automerge.AutomergeTaskTestData.failedRequiredCheck;
import static ru.yandex.market.tsum.tms.tasks.automerge.AutomergeTaskTestData.missingRequiredCheck;
import static ru.yandex.market.tsum.tms.tasks.automerge.AutomergeTaskTestData.pullRequest;
import static ru.yandex.market.tsum.tms.tasks.automerge.AutomergeTaskTestData.repository;
import static ru.yandex.market.tsum.tms.tasks.automerge.AutomergeTaskTestData.successfulRequiredCheck;
import static ru.yandex.market.tsum.tms.tasks.automerge.AutomergeTaskTester.attemptsToMerge;
import static ru.yandex.market.tsum.tms.tasks.automerge.AutomergeTaskTester.doesNothing;
import static ru.yandex.market.tsum.tms.tasks.automerge.AutomergeTaskTester.given;
import static ru.yandex.market.tsum.tms.tasks.automerge.AutomergeTaskTester.mergesAndWritesMergeComment;
import static ru.yandex.market.tsum.tms.tasks.automerge.AutomergeTaskTester.shouldDoNothingWithPr;
import static ru.yandex.market.tsum.tms.tasks.automerge.AutomergeTaskTester.shouldMergePr;
import static ru.yandex.market.tsum.tms.tasks.automerge.AutomergeTaskTester.shouldWriteMergePreconditionsCommentToPr;
import static ru.yandex.market.tsum.tms.tasks.automerge.AutomergeTaskTester.shouldWriteRequiredChecksCommentToPr;
import static ru.yandex.market.tsum.tms.tasks.automerge.AutomergeTaskTester.writesConflictsComment;
import static ru.yandex.market.tsum.tms.tasks.automerge.AutomergeTaskTester.writesMergePreconditionsComment;
import static ru.yandex.market.tsum.tms.tasks.automerge.AutomergeTaskTester.writesRequiredChecksComment;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 20.08.2018
 */
public class GithubPullRequestAutomergeTaskTest {
    @Test
    public void shouldMergeWhenAllPreconditionsAreFulfilled() {
        shouldMergePr(successfulRequiredCheck(), approvedReviewWithMergeCommand());
    }

    @Test
    public void mergeCommandSearching() {
        shouldWriteRequiredChecksCommentToPr(descriptionWithMergeCommand());

        shouldWriteRequiredChecksCommentToPr(commentWithMergeCommand());

        shouldWriteRequiredChecksCommentToPr(approvedReviewWithMergeCommand());

        shouldDoNothingWithPr(changesRequestedReviewWithMergeCommand());
        shouldDoNothingWithPr(commentedReviewWithMergeCommand());
        shouldDoNothingWithPr(dismissedReviewWithMergeCommand());
    }

    @Test
    public void mergeCommandParsing() {
        shouldWriteRequiredChecksCommentToPr(approvedReview("какой-то текст /merge больше текста"));
        shouldWriteRequiredChecksCommentToPr(approvedReview("норм ваще\n/merge\nкакой-то текст"));
        shouldDoNothingWithPr(approvedReview("a/merge"));
        shouldDoNothingWithPr(approvedReview("/mergea"));
    }

    @Test
    public void shouldOnlyMergeIfThereIsMergeCommand() {
        shouldDoNothingWithPr(successfulRequiredCheck(), approvedReview());

        shouldDoNothingWithPr(
            successfulRequiredCheck(),
            comment("some comment without merge command"),
            approvedReview("some review comment without merge command")
        );
    }

    @Test
    public void shouldOnlyWriteComentIfThereIsMergeCommand() {
        shouldDoNothingWithPr(successfulRequiredCheck(), changesRequestedReview("fix your code"));

        shouldDoNothingWithPr(failedRequiredCheck(), approvedReview());

        shouldDoNothingWithPr(approvedReview());
    }

    @Test
    public void shouldOnlyMergeIfThereIsApprovedReview() {
        shouldMergePr(successfulRequiredCheck(), commentWithMergeCommand(), approvedReview());
        shouldMergePr(successfulRequiredCheck(), commentWithMergeCommand(), approvedReview(), commentedReview(), dismissedReview());

        shouldWriteMergePreconditionsCommentToPr(successfulRequiredCheck(), commentWithMergeCommand());
        shouldWriteMergePreconditionsCommentToPr(successfulRequiredCheck(), commentWithMergeCommand(), changesRequestedReview());
        shouldWriteMergePreconditionsCommentToPr(successfulRequiredCheck(), commentWithMergeCommand(), commentedReview());
        shouldWriteMergePreconditionsCommentToPr(successfulRequiredCheck(), commentWithMergeCommand(), dismissedReview());
    }

    @Test
    public void shouldOnlyMergeIfRequiredChecksPassed() {
        shouldMergePr(successfulRequiredCheck(), approvedReviewWithMergeCommand());
        shouldMergePr(successfulRequiredCheck(), failedOptionalCheck(), approvedReviewWithMergeCommand());

        shouldWriteMergePreconditionsCommentToPr(failedRequiredCheck(), approvedReviewWithMergeCommand());
        shouldWriteMergePreconditionsCommentToPr(missingRequiredCheck(), approvedReviewWithMergeCommand());
        shouldWriteRequiredChecksCommentToPr(approvedReviewWithMergeCommand());
    }

    @Test
    public void shouldLookAtTheLastCheckForEachContext() {
        Instant recent = Instant.now();
        Instant obsolete = Instant.now().minusSeconds(1);

        shouldMergePr(
            successfulRequiredCheck("context1", recent),
            failedRequiredCheck("context1", obsolete),
            approvedReviewWithMergeCommand()
        );

        shouldWriteMergePreconditionsCommentToPr(
            failedRequiredCheck("context1", recent),
            successfulRequiredCheck("context1", obsolete),
            approvedReviewWithMergeCommand()
        );
    }

    @Test
    public void multipleRepositoriesAndPullRequests() {
        given(
            repository(
                "repo1",
                pullRequest(1, successfulRequiredCheck(), approvedReviewWithMergeCommand()),
                pullRequest(2, successfulRequiredCheck(), approvedReviewWithMergeCommand())
            ),
            repository(
                "repo2",
                pullRequest(1, successfulRequiredCheck(), approvedReviewWithMergeCommand())
            )
        )
            .runAutomergeTaskAndCheckThatIt(
                mergesAndWritesMergeComment("repo1", 1),
                mergesAndWritesMergeComment("repo1", 2),
                mergesAndWritesMergeComment("repo2", 1)
            );
    }

    @Test
    public void shouldWriteRequiredChecksCommentEveryDay() {
        given(repository("repo1", pullRequest(1, approvedReviewWithMergeCommand())))
            .runAutomergeTaskAndCheckThatIt(writesRequiredChecksComment("repo1", 1))
            .runAutomergeTaskAndCheckThatIt(doesNothing())
            .timePassed(Period.ofDays(1))
            .runAutomergeTaskAndCheckThatIt(writesRequiredChecksComment("repo1", 1))
            .runAutomergeTaskAndCheckThatIt(doesNothing());
    }

    @Test
    public void shouldWriteMergePreconditionsCommentEveryDay() {
        given(repository("repo1", pullRequest(1, failedRequiredCheck(), approvedReviewWithMergeCommand())))
            .runAutomergeTaskAndCheckThatIt(writesMergePreconditionsComment("repo1", 1))
            .runAutomergeTaskAndCheckThatIt(doesNothing())
            .timePassed(Period.ofDays(1))
            .runAutomergeTaskAndCheckThatIt(writesMergePreconditionsComment("repo1", 1))
            .runAutomergeTaskAndCheckThatIt(doesNothing());
    }

    @Test
    public void shouldWriteMergeConflictsCommentEveryDay() {
        given(repository("repo1", pullRequest(1, successfulRequiredCheck(), approvedReviewWithMergeCommand(), conflicts())))
            .runAutomergeTaskAndCheckThatIt(
                attemptsToMerge("repo1", 1),
                writesConflictsComment("repo1", 1)
            )
            .runAutomergeTaskAndCheckThatIt(
                attemptsToMerge("repo1", 1)
            )
            .timePassed(Period.ofDays(1))
            .runAutomergeTaskAndCheckThatIt(
                attemptsToMerge("repo1", 1),
                writesConflictsComment("repo1", 1)
            )
            .runAutomergeTaskAndCheckThatIt(
                attemptsToMerge("repo1", 1)
            );
    }

    @Test
    public void shouldNotWriteRequiredChecksWithSilentMode() {
        given(repository("repo1", pullRequest(1, approvedReviewWithMergeCommand())).setSilentMode(true))
            .runAutomergeTaskAndCheckThatIt(doesNothing())
            .timePassed(Period.ofDays(1))
            .runAutomergeTaskAndCheckThatIt(doesNothing());
    }

    @Test
    public void shouldNotWriteMergePreconditionsWithSilentMode() {
        given(repository("repo1", pullRequest(1, failedRequiredCheck(), approvedReviewWithMergeCommand())).setSilentMode(true))
            .runAutomergeTaskAndCheckThatIt(doesNothing())
            .timePassed(Period.ofDays(1))
            .runAutomergeTaskAndCheckThatIt(doesNothing());
    }

    @Test
    public void shouldNotWriteMergeConflictsCommentWithSilentMode() {
        given(repository("repo1", pullRequest(1, successfulRequiredCheck(), approvedReviewWithMergeCommand(), conflicts())).setSilentMode(true))
            .runAutomergeTaskAndCheckThatIt(attemptsToMerge("repo1", 1))
            .timePassed(Period.ofDays(1))
            .runAutomergeTaskAndCheckThatIt(attemptsToMerge("repo1", 1));
    }

    @Test
    public void shouldNotFindMergeCommandInCommentsThatAreWrittenByAutomergeTask() {
        given(repository("repo1", pullRequest(1, descriptionWithMergeCommand())))
            .runAutomergeTaskAndCheckThatIt(writesRequiredChecksComment("repo1", 1))
            .changePullRequest("repo1", 1, successfulRequiredCheck())
            .runAutomergeTaskAndCheckThatIt(writesMergePreconditionsComment("repo1", 1))
            .changePullRequest("repo1", 1, approvedReview(), descriptionWithoutMergeCommand())
            .runAutomergeTaskAndCheckThatIt(doesNothing())
            .changePullRequest("repo1", 1, descriptionWithMergeCommand())
            .runAutomergeTaskAndCheckThatIt(mergesAndWritesMergeComment("repo1", 1));
    }
}
