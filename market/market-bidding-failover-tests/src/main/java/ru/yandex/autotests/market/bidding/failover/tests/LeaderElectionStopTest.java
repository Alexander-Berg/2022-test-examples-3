package ru.yandex.autotests.market.bidding.failover.tests;

import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.market.bidding.rules.BiddingFailoverTestsRule;
import ru.yandex.autotests.market.bidding.steps.BiddingNodeSteps;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * User: alkedr
 * Date: 14.10.2014
 *
 * https://wiki.yandex-team.ru/mbi/bids/failover#prinuditelnajaostanovkaborbyzaliderstvo
 */
@Aqua.Test(title = "Принудительная остановка борьбы за лидерство")
@Features("Неполадки")
@Stories("Принудительная остановка борьбы за лидерство")
@Issue("TESTMARKET-1570")
public class LeaderElectionStopTest {
    @Rule
    public final BiddingFailoverTestsRule bidding = new BiddingFailoverTestsRule();

    @Test
    @Title("Лидер должен поменяться после дёргания ручки /leader-election-stop")
    public void leaderShouldChangeAfterStoppingLeaderElectionOnCurrentLeader() {
        BiddingNodeSteps oldLeader = bidding.leader();
        oldLeader.failover.stopLeaderElection();
        bidding.waitForLeaderToChange(oldLeader);
        bidding.database.failoverLock().leaderNodeShouldHaveAcquiredLock(bidding.leader());
    }

    @Test
    @Title("GET /leader-election-stop должна возвращать время последнего вызова POST /leader-election-stop + 5 минут")
    public void getLeaderElectionShouldReturnCorrectValue() {
        BiddingNodeSteps oldLeader = bidding.leader();
        long stopLeaderElectionRequestTimestamp = System.currentTimeMillis();
        oldLeader.failover.stopLeaderElection();
        oldLeader.failover.getLeaderElectionStopShouldReturnValueCloseTo(stopLeaderElectionRequestTimestamp + MINUTES.toMillis(5), SECONDS.toMillis(30));
    }
}
