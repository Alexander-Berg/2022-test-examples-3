package ru.yandex.autotests.market.bidding.failover.tests;

import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.market.bidding.rules.BiddingFailoverTestsRule;
import ru.yandex.autotests.market.bidding.steps.BiddingNodeSteps;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Title;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * User: alkedr
 * Date: 14.10.2014
 */
@Aqua.Test(title = "База данных недоступна более 20 секунд")
@Features("Неполадки")
@Issue("TESTMARKET-1570")
public class CloseDatabasePortTest {
    @Rule
    public final BiddingFailoverTestsRule bidding = new BiddingFailoverTestsRule();

    @Test
    @Title("Лидер должен поменяться если база данных недоступна более 20 секунд")
    public void leaderShouldChangeIfDatabaseIsUnavailableForMoreThat20Seconds() {
        BiddingNodeSteps oldLeader = bidding.leader();
        oldLeader.admin.closeDatabasePort();
        bidding.waitForLeaderToChange(oldLeader);
    }

    @Test
    @Title("Лидер НЕ должен поменяться если база данных недоступна менее 20 секунд")
    public void leaderShouldNotChangeIfDatabaseIsUnavailableForLessThat20Seconds() {
        bidding.leader().admin.closeDatabasePort();
        bidding.leaderShouldRemainTheSameInTheNext(20, SECONDS);
    }
}
