package ru.yandex.autotests.market.bidding.failover.tests;

import org.junit.Ignore;
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
@Aqua.Test(title = "Остановка nginx'а")
@Features("Остановка nginx'а")
@Issue("TESTMARKET-1570")
public class StopNginxTest {
    @Rule
    public final BiddingFailoverTestsRule bidding = new BiddingFailoverTestsRule();

    @Test
    @Title("После остановки nginx'а на лидере другая нода должна стать лидером")
    @Ignore("Отключено из-за https://st.yandex-team.ru/MBI-12151")
    public void oldLeaderNodeShouldNotBeLeader() {
        BiddingNodeSteps oldLeader = bidding.leader();
        oldLeader.admin.stopNginx();
        bidding.waitForSomeNodeToBecomeLeader();
        bidding.currentLeaderShouldBeDifferentFrom(oldLeader);
    }
}
