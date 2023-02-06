package ru.yandex.autotests.market.bidding.failover.tests;

import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.market.bidding.rules.BiddingFailoverTestsRule;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.market.common.commonsteps.HtmlElementsCommonSteps.waitABit;

/**
 * User: alkedr
 * Date: 15.10.2014
 */
@Aqua.Test(title = "Удаление лока работающего лидера")
@Features("Неполадки")
@Stories("Удаление лока работающего лидера")
@Issue("TESTMARKET-1570")
public class LockProblemsTest {
    @Rule
    public final BiddingFailoverTestsRule bidding = new BiddingFailoverTestsRule();

    @Test
    @Title("Лидер должен перезахватить лок если лок удалили")
    public void leaderShouldReacquireLockIfLockWasDeleted() {
        bidding.database.failoverLock().clear();
        waitABit(20*1000);
        bidding.waitForSomeNodeToBecomeLeader();
    }

    @Test
    @Title("Лидер должен перезахватить лок если лок захватила неработающая нода")
    public void leaderShouldReacquireLockIfFakeNodeAcquiredIt() {
        bidding.database.failoverLock().set("fake.node");
        waitABit(20*1000);
        bidding.waitForSomeNodeToBecomeLeader();
    }
}
