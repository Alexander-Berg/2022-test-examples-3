package ru.yandex.market.tsum.clients.bitbucket;

import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.market.tsum.clients.bitbucket.models.BitbucketCommit;
import ru.yandex.market.tsum.clients.bitbucket.models.BitbucketDiffList;
import ru.yandex.market.tsum.clients.bitbucket.models.BitbucketPullRequest;
import ru.yandex.market.tsum.clients.bitbucket.models.BitbucketPullRequestActivity;

@Ignore
public class BitbucketClientTest extends TestCase {
    private final BitbucketClient client = new BitbucketClient(
        "**", null
    );

    @Test
    public void testGetIterator() {
        Iterator<BitbucketCommit> commitIterator = client.getCommitIterator("market", "b2b-crm_autotests", "master");

        int commitsCount = 0;

        while (commitIterator.hasNext()) {
            commitsCount++;
            System.out.println(commitIterator.next().getDisplayId());
        }

        System.out.println(commitsCount);
    }

    @Test
    public void testGetCommit() {
        BitbucketCommit commit = client.getCommit("TRACKER", "startrek", "923ee66d6d308e6ca0d537dd0f5462340b695e13");
        System.out.println(commit);
    }

    @Test
    public void testGetDiff() {
        BitbucketDiffList diffList = client.getDiff("TRACKER", "startrek", "923ee66d6d308e6ca0d537dd0f5462340b695e13");
        System.out.println(diffList);
    }

    @Test
    public void testGetPullRequests() {
        List<BitbucketPullRequest> pullRequests = client.getCommitPullRequests("TRACKER", "startrek",
            "923ee66d6d308e6ca0d537dd0f5462340b695e13");
        System.out.println(pullRequests);
    }

    @Test
    public void testGetCommitPullRequestActivities() {
        List<BitbucketPullRequestActivity> activities = client.getCommitPullRequestActivities("TRACKER", "startrek",
            1449);
        System.out.println(activities);
    }

    @Test
    public void testGetCommitPullRequestCommits() {
        List<BitbucketCommit> commits = client.getCommitPullRequestCommits("TRACKER", "startrek", 1449);
        System.out.println(commits);
    }
}
