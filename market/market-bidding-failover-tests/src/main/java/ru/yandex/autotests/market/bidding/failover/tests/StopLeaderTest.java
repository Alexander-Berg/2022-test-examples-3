package ru.yandex.autotests.market.bidding.failover.tests;

import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.market.bidding.rules.BiddingFailoverTestsRule;
import ru.yandex.autotests.market.bidding.steps.BiddingNodeSteps;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Title;

/**
 * User: alkedr
 * Date: 14.10.2014
 */
@Aqua.Test(title = "Остановка лидера")
@Features("Остановка лидера")
@Issue("TESTMARKET-1570")
public class StopLeaderTest {
    @Rule
    public final BiddingFailoverTestsRule bidding = new BiddingFailoverTestsRule();

    @Test
    @Title("После остановки лидера другая нода должна стать лидером")
    public void otherNodeShouldBecomeLeaderWhenLeaderIsStopped() {
        BiddingNodeSteps oldLeader = bidding.leader();
        oldLeader.admin.stop();
        bidding.waitForSomeNodeToBecomeLeader();
        bidding.currentLeaderShouldBeDifferentFrom(oldLeader);
    }
}
