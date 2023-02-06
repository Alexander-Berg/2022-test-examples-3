package ru.yandex.market.tsum.pipelines.common.jobs.sox;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.egit.github.core.User;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.impl.DefaultListF;
import ru.yandex.market.tsum.clients.arcadia.RootArcadiaClient;
import ru.yandex.market.tsum.clients.arcadia.review.ArcadiaReviewsClient;
import ru.yandex.market.tsum.clients.arcadia.review.model.ReviewRequest;
import ru.yandex.market.tsum.clients.arcadia.review.model.Reviewer;
import ru.yandex.market.tsum.clients.bitbucket.BitbucketClient;
import ru.yandex.market.tsum.clients.bitbucket.models.BitbucketPullRequest;
import ru.yandex.market.tsum.clients.bitbucket.models.BitbucketUser;
import ru.yandex.market.tsum.clients.bitbucket.models.BitbucketUserWithRole;
import ru.yandex.market.tsum.clients.github.GitHubClient;
import ru.yandex.market.tsum.clients.github.model.Review;
import ru.yandex.market.tsum.pipelines.common.resources.BitbucketRepo;
import ru.yandex.startrek.client.model.Comment;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.startrek.client.model.UserRef;

@RunWith(MockitoJUnitRunner.class)
public class AutoSoxTest {

    @Mock
    private BitbucketClient bitbucketClient;

    @Mock
    private GitHubClient gitHubClient;

    @Mock
    private RootArcadiaClient arcadiaClient;

    @Mock
    private ArcadiaReviewsClient reviewClient;

    @Mock
    private Issue issue;

    @InjectMocks
    private AutoSoxFeature autoSoxJob = new AutoSoxFeature();

    @Before
    public void setup() throws Exception {

        Mockito.when(arcadiaClient.getReviewsClient()).thenReturn(reviewClient);

        issue = Mockito.mock(Issue.class);

        ReflectionTestUtils.setField(autoSoxJob, "approveString", "!SOXOK");
    }

    @Test
    public void findArcReviewersTest() throws Exception {

        int reviewId = 1111755;

        Reviewer reviewer1 = new Reviewer();
        Reviewer reviewer2 = new Reviewer();
        reviewer1.setLogin("user");
        reviewer2.setLogin("login");
        reviewer1.setShipIt(true);
        reviewer2.setShipIt(true);

        ReviewRequest reviewRequest = new ReviewRequest();
        List<Reviewer> listReviewers = Arrays.asList(reviewer1, reviewer2);
        reviewRequest.setReviewers(listReviewers);
        Mockito.when(reviewClient.getReview(reviewId)).thenReturn(reviewRequest);

        AutoSoxFeature.ReviewInfo reviewInfo = new AutoSoxFeature.ReviewInfo();
        String change = "test messageREVIEW: " + reviewId;

        autoSoxJob.findArcReviewers(reviewInfo, change);
        Assert.assertTrue(reviewInfo.getReviewersSet().contains(listReviewers.get(0).getLogin()));
        Assert.assertTrue(reviewInfo.getReviewersSet().contains(listReviewers.get(1).getLogin()));
        Assert.assertEquals(reviewInfo.getReviewId(), Integer.toString(reviewId));
    }

    @Test
    public void findGitReviewersTest() throws Exception {
        String repositoryFullName = "market-infra/market-health";
        int prId = 4444;
        String login = "user";
        User user = new User();
        user.setLogin(login);
        Review review = new Review();
        review.setState(Review.State.APPROVED);
        review.setUser(user);
        Mockito.when(gitHubClient.getReviews(repositoryFullName, prId)).thenReturn(Collections.singletonList(review));

        Method findGitReviewersMethod = autoSoxJob.getClass()
            .getDeclaredMethod("findGitReviewers", AutoSoxFeature.ReviewInfo.class, String.class, String.class);
        findGitReviewersMethod.setAccessible(true);

        AutoSoxFeature.ReviewInfo reviewInfo = new AutoSoxFeature.ReviewInfo();
        String change = "MARKETTEST-11111: test pr (#" + prId + ")";

        autoSoxJob.findGitReviewers(reviewInfo, change, repositoryFullName);
        Assert.assertTrue(reviewInfo.getReviewersSet().contains(login));
        Assert.assertTrue(reviewInfo.getReviewersSet().contains(login));
        Assert.assertEquals(reviewInfo.getReviewId(), Integer.toString(prId));

    }

    @Test
    public void findBitbucketReviewersTest() {
        String project = "market-infra";
        String repo = "market-health";

        String revision = "40e38f709cb840dc95feb0fea36587fffc2d26d58";
        int prId = 4444;
        String login = "user";
        String change = "MARKETTEST-11111: test pr";

        BitbucketPullRequest openPR = new BitbucketPullRequest();
        openPR.setState(BitbucketPullRequest.State.OPEN);

        BitbucketPullRequest mergedPR = new BitbucketPullRequest();
        mergedPR.setId(prId);
        mergedPR.setState(BitbucketPullRequest.State.MERGED);
        mergedPR.setTitle(change);

        BitbucketPullRequest anotherMergedPR = new BitbucketPullRequest();
        anotherMergedPR.setState(BitbucketPullRequest.State.MERGED);
        anotherMergedPR.setTitle("MARKETTEST-12312: another test pr");

        mergedPR.setReviewers(List.of(makeBBUser(login, true), makeBBUser("user2", false)));

        Mockito.when(bitbucketClient.getCommitPullRequests(project, repo, revision))
            .thenReturn(List.of(openPR, mergedPR, anotherMergedPR));

        AutoSoxFeature.ReviewInfo reviewInfo = new AutoSoxFeature.ReviewInfo();

        autoSoxJob.findBitbucketReviewers(reviewInfo, revision, change, new BitbucketRepo(project, repo));

        Assert.assertEquals(1, reviewInfo.getReviewersSet().size());
        Assert.assertTrue(reviewInfo.getReviewersSet().contains(login));
        Assert.assertTrue(reviewInfo.getReviewersSet().contains(login));
        Assert.assertEquals(reviewInfo.getReviewId(), Integer.toString(prId));
    }

    private BitbucketUserWithRole makeBBUser(String login, boolean approved) {
        BitbucketUser user = new BitbucketUser();
        user.setName(login);

        BitbucketUserWithRole userWithRole = new BitbucketUserWithRole();
        userWithRole.setApproved(approved);
        userWithRole.setUser(user);

        return userWithRole;
    }

    @Test
    public void getExistingApprovesTest() throws Exception {

        String approveString = "!SOXOK";

        String login = "user";
        UserRef userRef = Mockito.mock(UserRef.class);
        Mockito.when(userRef.getLogin()).thenReturn(login);

        Comment comment = Mockito.mock(Comment.class);
        Mockito.when(comment.getCreatedBy()).thenReturn(userRef);
        Mockito.when(comment.getText()).thenReturn(Option.of(approveString));

        ListF<Comment> comments = new DefaultListF(Collections.singletonList(comment));
        Mockito.when(issue.getComments()).thenReturn(comments.iterator());
        Method getExistingApproves = autoSoxJob.getClass()
            .getDeclaredMethod("getExistingApproves", Issue.class);
        getExistingApproves.setAccessible(true);

        Set<String> approvesSet = (Set<String>) getExistingApproves.invoke(autoSoxJob, issue);
        Assert.assertTrue(approvesSet.contains(login));
    }

}

