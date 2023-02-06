package ru.yandex.market.tsum.core.clients.arcadia;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.tmatesoft.svn.core.SVNLogEntry;
import ru.yandex.market.tsum.clients.arcadia.TrunkArcadiaClient;
import ru.yandex.market.tsum.clients.arcadia.ArcadiaDiffStats;
import ru.yandex.market.tsum.clients.arcadia.review.ArcadiaReviewsClient;
import ru.yandex.market.tsum.clients.arcadia.review.model.ReviewRequest;
import ru.yandex.market.tsum.clients.arcadia.review.model.activity.ReviewActivity;
import ru.yandex.market.tsum.core.TsumDebugRuntimeConfig;

/**
 * @author Nikolay Firov <a href="mailto:firov@yandex-team.ru"></a>
 * @date 25.01.18
 */
@Ignore
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TsumDebugRuntimeConfig.class,})
public class ArcadiaClientIntegrationTest {
    @Autowired
    private TrunkArcadiaClient arcadiaClient;

    @Test
    public void fetchesReviewAndActivity() {
        int reviewId = 270915;

        ArcadiaReviewsClient client = arcadiaClient.getReviewsClient();

        ReviewRequest review = client.getReview(reviewId);
        Assert.assertEquals("vladvin", review.getSubmitter());

        ReviewActivity activity = client.getActivity(reviewId);
        Assert.assertEquals("commented", activity.getActivities().get(0).getAction());
    }

    @Test
    public void fetchesHeadRevision() {
        SVNLogEntry commit = arcadiaClient.getHead();
        Assert.assertTrue("Arcadia revision should be greater than 3398621L", commit.getRevision() >= 3398621L);
    }

    @Test(timeout = 120000)
    public void getDiffStat() {
        ArcadiaDiffStats diffStats = arcadiaClient.getDiffStats(3820932);
        Assert.assertEquals(new ArcadiaDiffStats(4, 0, 1), diffStats);
    }

    @Test(timeout = 120000)
    public void getDiffStat2() {
        ArcadiaDiffStats diffStats = arcadiaClient.getDiffStats(3820948);
    }
}
