package ru.yandex.market.tsum.release.merge.filters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import org.eclipse.egit.github.core.PullRequest;
import org.junit.Assert;
import org.junit.Test;

import static ru.yandex.market.tsum.pipelines.test_data.TestPullRequestFactory.pullRequest;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 08.06.17
 */
public class CompoundPullRequestValidatorTest {
    private static final String REPO_FULL_NAME = "market/market";

    @Test
    public void allMerge() throws Exception {
        CompoundPullRequestValidator sut =
            new CompoundPullRequestValidator(Arrays.asList(new AcceptAllPullRequestValidator()));
        PullRequest pullRequest = pullRequest();

        PullRequestValidatorResult result = sut.validate(REPO_FULL_NAME, Arrays.asList(pullRequest));

        Assert.assertEquals(1, result.getAcceptedPullRequests().size());
        Assert.assertEquals(0, result.getRejectedPullRequests().size());
    }

    @Test
    public void filterOutSamePullRequests() throws Exception {
        PullRequestValidator firstValidator = new LambdaPullRequestValidator(pr -> pr.getNumber() == 1);
        PullRequestValidator secondValidator = new LambdaPullRequestValidator(pr -> pr.getNumber() == 1);
        CompoundPullRequestValidator sut = new CompoundPullRequestValidator(Arrays.asList(firstValidator,
            secondValidator));

        PullRequestValidatorResult result = sut.validate(
            REPO_FULL_NAME,
            Arrays.asList(pullRequest(1), pullRequest(2))
        );

        Assert.assertEquals(1, result.getAcceptedPullRequests().size());
        Assert.assertEquals(1, result.getRejectedPullRequests().size());
    }

    @Test
    public void filterOutDifferentPullRequests() throws Exception {
        PullRequestValidator firstFilter = new LambdaPullRequestValidator(pr -> pr.getNumber() == 1);
        PullRequestValidator secondFilter = new LambdaPullRequestValidator(pr -> pr.getNumber() == 2);
        CompoundPullRequestValidator sut = new CompoundPullRequestValidator(Arrays.asList(firstFilter, secondFilter));

        PullRequestValidatorResult result = sut.validate(
            REPO_FULL_NAME,
            Arrays.asList(pullRequest(1), pullRequest(2))
        );

        Assert.assertEquals(0, result.getAcceptedPullRequests().size());
        Assert.assertEquals(2, result.getRejectedPullRequests().size());
    }

    static class LambdaPullRequestValidator implements PullRequestValidator {
        private final Predicate<PullRequest> predicate;

        LambdaPullRequestValidator(Predicate<PullRequest> predicate) {
            this.predicate = predicate;
        }

        @Override
        public PullRequestValidatorResult validate(String repositoryFullName, List<PullRequest> pullRequests) {
            List<PullRequest> acceptedPullRequests = new ArrayList<>();
            List<PullRequest> rejectedPullRequests = new ArrayList<>();

            for (PullRequest pullRequest : pullRequests) {
                if (predicate.test(pullRequest)) {
                    acceptedPullRequests.add(pullRequest);
                } else {
                    rejectedPullRequests.add(pullRequest);
                }
            }

            return new PullRequestValidatorResult(acceptedPullRequests, rejectedPullRequests);
        }
    }
}
