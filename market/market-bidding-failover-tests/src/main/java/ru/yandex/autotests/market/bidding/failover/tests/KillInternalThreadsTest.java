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

import static java.util.concurrent.TimeUnit.SECONDS;
import static ru.yandex.autotests.market.bidding.client.failoveradmin.beans.InternalThread.*;

/**
 * User: alkedr
 * Date: 15.10.2014
 */
@Aqua.Test(title = "Остановка потока LeaderElection")
@Features("Неполадки")
@Stories("Остановка потока LeaderElection")
@Issue("TESTMARKET-1570")
public class KillInternalThreadsTest {
    @Rule
    public final BiddingFailoverTestsRule bidding = new BiddingFailoverTestsRule();

    @Test
    @Title("Лидер должен поменяться если поток LeaderElection остановлен")
    public void leaderShouldChangeWhenLeaderElectionThreadCrashes() {
        BiddingNodeSteps oldLeader = bidding.leader();
        oldLeader.killThread(LEADER_ELECTION);
        bidding.waitForLeaderToChange(oldLeader);
    }

    @Test
    @Title("Лидер должен поменяться если поток RunStateChecker остановлен")
    public void leaderShouldChangeWhenRunStateCheckerThreadCrashes() {
        BiddingNodeSteps oldLeader = bidding.leader();
        oldLeader.killThread(RUN_STATE_CHECKER);
        bidding.waitForLeaderToChange(oldLeader);
    }

    @Test
    @Title("Лидер НЕ должен поменяться если поток Starter остановлен")
    public void leaderShouldNotChangeWhenStarterThreadCrashes() {
        BiddingNodeSteps oldLeader = bidding.leader();
        oldLeader.killThread(STARTER);
        oldLeader.pingShouldNotReturnOk();
        bidding.leaderShouldRemainTheSameInTheNext(30, SECONDS);
    }
}
